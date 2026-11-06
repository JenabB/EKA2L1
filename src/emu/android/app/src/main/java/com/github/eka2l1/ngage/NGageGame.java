/*
 * Copyright (c) 2026 NGStation contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.github.eka2l1.ngage;

import android.graphics.Bitmap;

/**
 * One game found in the user's games folder. {@link #path} is what gets handed to the
 * emulator core: a filesystem path, or a content URI when only SAF access is available.
 */
public class NGageGame {
    /** Display name, taken from the folder (or archive) name. */
    public final String title;
    /** Name of the single folder under System/Apps, used to match the installed app. */
    public final String innerName;
    public final String path;
    public final boolean archive;

    /** Title without region and version tags, e.g. "The King of Fighters - Extreme". */
    public final String displayTitle;
    /** Short region label from the name, e.g. "USA/EU", or null. */
    public final String region;

    public long uid = -1;
    public Bitmap icon;
    public long lastPlayed;

    public NGageGame(String title, String innerName, String path, boolean archive) {
        this.title = title;
        this.innerName = innerName;
        this.path = path;
        this.archive = archive;
        this.displayTitle = cleanTitle(title);
        this.region = regionOf(title);
    }

    /**
     * Strips the "(USA, Europe) (En,Fr,De)" and "[!]" tags that dump names carry, tidies
     * underscores and moves a trailing ", The" to the front.
     */
    static String cleanTitle(String raw) {
        String t = raw.replace('_', ' ');
        t = t.replaceAll("\\s*[\\(\\[][^\\)\\]]*[\\)\\]]", "");
        t = t.replaceAll("\\s+", " ").trim();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(.*?),\\s*The(\\b.*)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.matches()) {
            t = "The " + m.group(1).trim() + m.group(2);
        } else {
            m = java.util.regex.Pattern.compile("^(.*?)\\s+The(\\s+-.*)$").matcher(t);
            if (m.matches()) {
                t = "The " + m.group(1).trim() + m.group(2);
            }
        }
        t = t.replaceAll("\\s*-\\s*$", "").trim();
        return t.isEmpty() ? raw : t;
    }

    static String regionOf(String raw) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^)]*)\\)").matcher(raw);
        java.util.List<String> parts = new java.util.ArrayList<>();
        while (m.find()) {
            for (String token : m.group(1).split("[,\\s]+")) {
                String code;
                switch (token.toLowerCase(java.util.Locale.ROOT)) {
                    case "usa": code = "USA"; break;
                    case "europe": case "eu": code = "EU"; break;
                    case "japan": case "jp": code = "JP"; break;
                    case "asia": code = "Asia"; break;
                    case "world": code = "World"; break;
                    case "korea": code = "KR"; break;
                    case "china": code = "CN"; break;
                    default: code = null;
                }
                if (code != null && !parts.contains(code)) {
                    parts.add(code);
                }
            }
        }
        return parts.isEmpty() ? null : android.text.TextUtils.join("/", parts);
    }

    public boolean isInstalled() {
        return uid != -1;
    }
}
