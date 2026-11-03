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
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.github.eka2l1.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installs a ready-made EKA2L1 data pack: a "Data" folder holding devices.yml,
 * drives/z/&lt;firmware code&gt;/ and roms/&lt;firmware code&gt;/SYM.ROM. It is merged into the
 * emulator's own data folder, so devices already installed stay.
 */
final class FirmwarePack {
    static final String DEVICES_FILE = "devices.yml";

    private FirmwarePack() {
    }

    // --------------------------------------------------------------- detection

    /**
     * Finds the pack root (the folder holding devices.yml) from whatever folder the user
     * picked: the Data folder itself, its parent, or a folder inside it such as roms/RH-29.
     */
    static File findPackRoot(File picked) {
        if (picked == null) {
            return null;
        }
        File data = childIgnoreCase(picked, "data");
        if (data != null && isPackRoot(data)) {
            return data;
        }
        File current = picked;
        for (int i = 0; i < 4 && current != null; i++) {
            if (isPackRoot(current)) {
                return current;
            }
            current = current.getParentFile();
        }
        return null;
    }

    private static boolean isPackRoot(File dir) {
        return dir.isDirectory() && childFileIgnoreCase(dir, DEVICES_FILE) != null
                && childIgnoreCase(dir, "drives") != null;
    }

    static DocumentFile findPackRoot(DocumentFile picked) {
        if (picked == null) {
            return null;
        }
        if (isPackRoot(picked)) {
            return picked;
        }
        for (DocumentFile child : picked.listFiles()) {
            if (child.isDirectory() && "data".equalsIgnoreCase(child.getName()) && isPackRoot(child)) {
                return child;
            }
        }
        return null;
    }

    private static boolean isPackRoot(DocumentFile dir) {
        boolean devices = false, drives = false;
        for (DocumentFile child : dir.listFiles()) {
            String name = child.getName();
            if (name == null) {
                continue;
            }
            if (child.isFile() && name.equalsIgnoreCase(DEVICES_FILE)) {
                devices = true;
            } else if (child.isDirectory() && name.equalsIgnoreCase("drives")) {
                drives = true;
            }
        }
        return devices && drives;
    }

    // ------------------------------------------------------------ installation

    /** Merges the pack at {@code packRoot} into {@code dataDir}. */
    static void installFromFolder(File packRoot, File dataDir) throws IOException {
        if (!dataDir.isDirectory() && !dataDir.mkdirs()) {
            throw new IOException("Tidak bisa membuat folder " + dataDir);
        }
        File[] children = packRoot.listFiles();
        if (children == null) {
            throw new IOException("Folder firmware tidak bisa dibaca");
        }
        for (File child : children) {
            if (child.isFile() && child.getName().equalsIgnoreCase(DEVICES_FILE)) {
                mergeDevices(child, new File(dataDir, DEVICES_FILE));
            } else {
                copyRecursive(child, new File(dataDir, normalizeTopName(child.getName())));
            }
        }
    }

    /** Copies a pack reachable only through SAF into {@code dataDir}. */
    static void installFromDocument(Context context, DocumentFile packRoot, File dataDir) throws IOException {
        File staging = new File(dataDir.getParentFile(), "ngstation_pack_tmp");
        FileUtils.deleteDirectory(staging);
        try {
            copyDocumentRecursive(context, packRoot, staging);
            installFromFolder(staging, dataDir);
        } finally {
            FileUtils.deleteDirectory(staging);
        }
    }

    /** Unpacks a zipped pack (as shared around for EKA2L1) and merges it into {@code dataDir}. */
    static void installFromZip(Context context, Uri zipUri, File dataDir) throws IOException {
        File staging = new File(dataDir.getParentFile(), "ngstation_pack_tmp");
        FileUtils.deleteDirectory(staging);
        try {
            InputStream input = context.getContentResolver().openInputStream(zipUri);
            if (input == null) {
                throw new IOException("File ZIP tidak bisa dibuka");
            }
            unzip(input, staging);
            File root = findPackRootBelow(staging);
            if (root == null) {
                throw new IOException("ZIP ini bukan paket firmware EKA2L1 (tidak ada devices.yml + drives).");
            }
            installFromFolder(root, dataDir);
        } finally {
            FileUtils.deleteDirectory(staging);
        }
    }

    private static File findPackRootBelow(File dir) {
        if (isPackRoot(dir)) {
            return dir;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                if (isPackRoot(child)) {
                    return child;
                }
                File[] grand = child.listFiles();
                if (grand == null) {
                    continue;
                }
                for (File g : grand) {
                    if (g.isDirectory() && isPackRoot(g)) {
                        return g;
                    }
                }
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- helpers

    /** The core expects lower-case "drives" and "roms"; packs made on Windows vary. */
    private static String normalizeTopName(String name) {
        if (name.equalsIgnoreCase("drives") || name.equalsIgnoreCase("roms")) {
            return name.toLowerCase();
        }
        return name;
    }

    private static void copyRecursive(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.isDirectory() && !dst.mkdirs()) {
                throw new IOException("Tidak bisa membuat folder " + dst);
            }
            File[] children = src.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                copyRecursive(child, new File(dst, child.getName()));
            }
        } else {
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                copy(in, out);
            }
        }
    }

    private static void copyDocumentRecursive(Context context, DocumentFile src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.isDirectory() && !dst.mkdirs()) {
                throw new IOException("Tidak bisa membuat folder " + dst);
            }
            for (DocumentFile child : src.listFiles()) {
                String name = child.getName();
                if (name == null) {
                    continue;
                }
                copyDocumentRecursive(context, child, new File(dst, name));
            }
        } else {
            InputStream in = context.getContentResolver().openInputStream(src.getUri());
            if (in == null) {
                throw new IOException("Tidak bisa membaca " + src.getName());
            }
            try (InputStream input = in; OutputStream out = new FileOutputStream(dst)) {
                copy(input, out);
            }
        }
    }

    private static void unzip(InputStream input, File target) throws IOException {
        String targetPath = target.getCanonicalPath() + File.separator;
        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(target, entry.getName());
                // Refuse entries that would land outside the staging folder.
                if (!file.getCanonicalPath().startsWith(targetPath)) {
                    throw new IOException("Isi ZIP tidak valid: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) {
                        throw new IOException("Tidak bisa membuat folder " + file);
                    }
                    continue;
                }
                File parent = file.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Tidak bisa membuat folder " + parent);
                }
                try (OutputStream out = new FileOutputStream(file)) {
                    copy(zis, out);
                }
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * devices.yml is a map keyed by firmware code. Entries from the pack are added; an
     * entry with the same code is replaced by the pack's.
     */
    static void mergeDevices(File incoming, File existing) throws IOException {
        Map<String, List<String>> merged = new LinkedHashMap<>();
        if (existing.isFile()) {
            readDeviceBlocks(existing, merged);
        }
        readDeviceBlocks(incoming, merged);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(existing), StandardCharsets.UTF_8)) {
            for (List<String> block : merged.values()) {
                for (String line : block) {
                    writer.write(line);
                    writer.write('\n');
                }
            }
        }
    }

    private static void readDeviceBlocks(File file, Map<String, List<String>> out) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            List<String> current = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                boolean topLevel = !Character.isWhitespace(line.charAt(0)) && line.trim().endsWith(":");
                if (topLevel) {
                    String key = line.trim();
                    current = new ArrayList<>();
                    current.add(line);
                    out.put(key, current);
                } else if (current != null) {
                    current.add(line);
                }
            }
        }
    }

    private static File childIgnoreCase(File dir, String name) {
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

    private static File childFileIgnoreCase(File dir, String name) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return null;
        }
        for (File e : entries) {
            if (e.isFile() && e.getName().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }
}
