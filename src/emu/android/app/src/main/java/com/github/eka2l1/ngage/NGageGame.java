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

    public long uid = -1;
    public Bitmap icon;

    public NGageGame(String title, String innerName, String path, boolean archive) {
        this.title = title;
        this.innerName = innerName;
        this.path = path;
        this.archive = archive;
    }

    public boolean isInstalled() {
        return uid != -1;
    }
}
