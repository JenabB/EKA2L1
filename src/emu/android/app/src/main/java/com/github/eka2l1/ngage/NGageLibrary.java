/*
 * Copyright (c) 2026 NGStation contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.github.eka2l1.ngage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.github.eka2l1.applist.AppItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Storage and matching logic for the game library: which folder holds the games,
 * which installed app UID belongs to which game folder, and the launch that is
 * pending across the process restart the core needs after installing a game.
 */
public final class NGageLibrary {
    private static final String PREFS = "ngstation_library";
    private static final String KEY_GAMES_DIR = "games_dir";
    private static final String KEY_UID_PREFIX = "uid:";
    private static final String KEY_PENDING_PATH = "pending_path";
    private static final String KEY_PENDING_BEFORE = "pending_before";
    private static final String KEY_SETUP_DONE = "setup_done";

    private NGageLibrary() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- settings

    public static String getGamesDir(Context context) {
        return prefs(context).getString(KEY_GAMES_DIR, null);
    }

    public static void setGamesDir(Context context, String dir) {
        prefs(context).edit().putString(KEY_GAMES_DIR, dir).apply();
    }

    public static boolean isSetupDone(Context context) {
        return prefs(context).getBoolean(KEY_SETUP_DONE, false);
    }

    public static void setSetupDone(Context context, boolean done) {
        prefs(context).edit().putBoolean(KEY_SETUP_DONE, done).commit();
    }

    public static long getMappedUid(Context context, String gamePath) {
        return prefs(context).getLong(KEY_UID_PREFIX + gamePath, -1);
    }

    public static void setMappedUid(Context context, String gamePath, long uid) {
        prefs(context).edit().putLong(KEY_UID_PREFIX + gamePath, uid).commit();
    }

    public static void clearMappedUid(Context context, String gamePath) {
        prefs(context).edit().remove(KEY_UID_PREFIX + gamePath).commit();
    }

    /**
     * Creates Games/N-Gage in shared storage, with a short note on how to lay games out.
     * Returns null when the folder cannot be created (no direct storage access).
     */
    public static File createDefaultGamesDir() {
        File dir = new File(android.os.Environment.getExternalStorageDirectory(), "Games/N-Gage");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return null;
        }
        new File(dir, "Firmware").mkdirs();
        File note = new File(dir, "README.txt");
        if (!note.exists()) {
            String text = "NGStation - N-Gage games folder\n\n"
                    + "Put each game here as its own folder (containing System/Apps/...)\n"
                    + "or as a .zip file. For example:\n\n"
                    + "  Games/N-Gage/Asphalt Urban GT/System/Apps/Asphalt/...\n"
                    + "  Games/N-Gage/Rayman 3.zip\n\n"
                    + "Then open NGStation; the games show up in the library.\n\n"
                    + "Firmware: put the firmware pack .zip in the Firmware/ folder\n"
                    + "before setup and it installs automatically.\n";
            try (java.io.Writer w = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(note), java.nio.charset.StandardCharsets.UTF_8)) {
                w.write(text);
            } catch (java.io.IOException ignored) {
            }
        }
        return dir;
    }

    private static final String KEY_PLAYED_PREFIX = "played:";

    public static long getLastPlayed(Context context, String gamePath) {
        return prefs(context).getLong(KEY_PLAYED_PREFIX + gamePath, 0);
    }

    public static void setLastPlayed(Context context, String gamePath, long when) {
        prefs(context).edit().putLong(KEY_PLAYED_PREFIX + gamePath, when).apply();
    }

    // ------------------------------------------------------------ pending launch

    /** Remembers the game to launch once the process has restarted after install. */
    public static void setPendingLaunch(Context context, String gamePath, Set<Long> uidsBefore) {
        StringBuilder sb = new StringBuilder();
        for (Long uid : uidsBefore) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(uid);
        }
        // commit(), not apply(): the process is killed right after this.
        prefs(context).edit()
                .putString(KEY_PENDING_PATH, gamePath)
                .putString(KEY_PENDING_BEFORE, sb.toString())
                .commit();
    }

    public static String getPendingPath(Context context) {
        return prefs(context).getString(KEY_PENDING_PATH, null);
    }

    public static Set<Long> getPendingBefore(Context context) {
        Set<Long> result = new HashSet<>();
        String raw = prefs(context).getString(KEY_PENDING_BEFORE, "");
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String part : raw.split(",")) {
            try {
                result.add(Long.parseLong(part));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public static void clearPendingLaunch(Context context) {
        prefs(context).edit().remove(KEY_PENDING_PATH).remove(KEY_PENDING_BEFORE).commit();
    }

    // ------------------------------------------------------------------ scanning

    public static boolean isContentUri(String path) {
        return path != null && path.startsWith("content://");
    }

    /**
     * Lists the games in {@code dir}. A game is a folder holding System/Apps (directly, or one
     * level deeper, as many dumps wrap it in an extra folder) or a .zip archive of one. The
     * chosen folder may itself be a single game.
     */
    public static List<NGageGame> scan(Context context, String dir) {
        if (dir == null) {
            return new ArrayList<>();
        }
        if (isContentUri(dir)) {
            DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(dir));
            return root == null ? new ArrayList<>() : scanDocument(root);
        }
        return scanFile(new File(dir));
    }

    private static List<NGageGame> scanFile(File root) {
        List<NGageGame> games = new ArrayList<>();
        String rootInner = findInnerName(root);
        if (rootInner != null) {
            games.add(new NGageGame(root.getName(), rootInner, root.getAbsolutePath(), false));
            return games;
        }

        File[] children = root.listFiles();
        if (children == null) {
            return games;
        }
        Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        for (File child : children) {
            if (child.isDirectory()) {
                String inner = findInnerName(child);
                if (inner != null) {
                    games.add(new NGageGame(child.getName(), inner, child.getAbsolutePath(), false));
                    continue;
                }
                File[] grand = child.listFiles();
                if (grand == null) {
                    continue;
                }
                for (File g : grand) {
                    if (!g.isDirectory()) {
                        continue;
                    }
                    String innerG = findInnerName(g);
                    if (innerG != null) {
                        games.add(new NGageGame(child.getName(), innerG, g.getAbsolutePath(), false));
                        break;
                    }
                }
            } else if (isArchiveName(child.getName()) && !FirmwarePack.isPackZip(child)) {
                games.add(new NGageGame(stripExtension(child.getName()), null, child.getAbsolutePath(), true));
            }
        }
        return games;
    }

    /** Returns the folder name under System/Apps, or null if this is not a game card folder. */
    private static String findInnerName(File dir) {
        File system = findChildIgnoreCase(dir, "system");
        if (system == null) {
            return null;
        }
        File apps = findChildIgnoreCase(system, "apps");
        if (apps == null) {
            return null;
        }
        File[] entries = apps.listFiles();
        if (entries == null) {
            return null;
        }
        for (File e : entries) {
            if (e.isDirectory()) {
                return e.getName();
            }
        }
        return null;
    }

    private static File findChildIgnoreCase(File dir, String name) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return null;
        }
        for (File e : entries) {
            if (e.isDirectory() && e.getName().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    private static List<NGageGame> scanDocument(DocumentFile root) {
        List<NGageGame> games = new ArrayList<>();
        String rootInner = findInnerName(root);
        if (rootInner != null) {
            games.add(new NGageGame(nameOf(root), rootInner, root.getUri().toString(), false));
            return games;
        }

        List<DocumentFile> children = new ArrayList<>(Arrays.asList(root.listFiles()));
        Collections.sort(children, (a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));

        for (DocumentFile child : children) {
            if (child.isDirectory()) {
                String inner = findInnerName(child);
                if (inner != null) {
                    games.add(new NGageGame(nameOf(child), inner, child.getUri().toString(), false));
                    continue;
                }
                for (DocumentFile g : child.listFiles()) {
                    if (!g.isDirectory()) {
                        continue;
                    }
                    String innerG = findInnerName(g);
                    if (innerG != null) {
                        games.add(new NGageGame(nameOf(child), innerG, g.getUri().toString(), false));
                        break;
                    }
                }
            } else if (isArchiveName(nameOf(child))) {
                games.add(new NGageGame(stripExtension(nameOf(child)), null, child.getUri().toString(), true));
            }
        }
        return games;
    }

    private static String findInnerName(DocumentFile dir) {
        DocumentFile system = findChildIgnoreCase(dir, "system");
        if (system == null) {
            return null;
        }
        DocumentFile apps = findChildIgnoreCase(system, "apps");
        if (apps == null) {
            return null;
        }
        for (DocumentFile e : apps.listFiles()) {
            if (e.isDirectory()) {
                return nameOf(e);
            }
        }
        return null;
    }

    private static DocumentFile findChildIgnoreCase(DocumentFile dir, String name) {
        for (DocumentFile e : dir.listFiles()) {
            if (e.isDirectory() && nameOf(e).equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    private static String nameOf(DocumentFile file) {
        String name = file.getName();
        return name == null ? "" : name;
    }

    private static boolean isArchiveName(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    // ------------------------------------------------------------------ matching

    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Picks the installed app that belongs to {@code game}. Apps that appeared since the
     * install started are preferred; among several candidates the name decides. Returns
     * null when nothing can be told apart with confidence.
     */
    public static AppItem matchInstalledApp(NGageGame game, List<AppItem> apps, Set<Long> uidsBefore) {
        List<AppItem> fresh = new ArrayList<>();
        if (uidsBefore != null) {
            for (AppItem app : apps) {
                if (!uidsBefore.contains(app.getUid())) {
                    fresh.add(app);
                }
            }
        }
        if (fresh.size() == 1) {
            return fresh.get(0);
        }
        if (!fresh.isEmpty()) {
            return matchByName(game, fresh, 1);
        }
        // Nothing new appeared (the game was installed before): only trust a clear name match.
        return matchByName(game, apps, 3);
    }

    /**
     * Name match between a game folder and the installed apps. Scores: 4 exact System/Apps
     * folder name, 3 exact folder title, 2 partial System/Apps name, 1 partial title.
     * Returns null on a tie or when the best score is below {@code minScore}.
     */
    public static AppItem matchByName(NGageGame game, List<AppItem> apps, int minScore) {
        String inner = normalize(game.innerName);
        String title = normalize(game.title);
        AppItem best = null;
        int bestScore = 0;
        int ties = 0;
        for (AppItem app : apps) {
            String caption = normalize(app.getTitle());
            if (caption.isEmpty()) {
                continue;
            }
            int score = 0;
            if (!inner.isEmpty() && caption.equals(inner)) {
                score = 4;
            } else if (!title.isEmpty() && caption.equals(title)) {
                score = 3;
            } else if (!inner.isEmpty() && (caption.contains(inner) || inner.contains(caption))) {
                score = 2;
            } else if (!title.isEmpty() && (caption.contains(title) || title.contains(caption))) {
                score = 1;
            }
            if (score > bestScore) {
                best = app;
                bestScore = score;
                ties = 0;
            } else if (score == bestScore && score > 0) {
                ties++;
            }
        }
        return (ties == 0 && bestScore >= minScore) ? best : null;
    }

    // ------------------------------------------------------------------- devices

    private static final String[] NGAGE_CODES = {"nem4", "rh29", "rh47"};

    public static boolean looksLikeNGage(String deviceName, String firmwareCode) {
        String name = normalize(deviceName);
        String code = normalize(firmwareCode);
        if (name.contains("ngage") || code.contains("ngage")) {
            return true;
        }
        for (String c : NGAGE_CODES) {
            if (name.contains(c) || code.equals(c) || code.startsWith(c)) {
                return true;
            }
        }
        return false;
    }
}
