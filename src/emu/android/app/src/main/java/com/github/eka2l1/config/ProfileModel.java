/*
 * Copyright (c) 2021 EKA2L1 Team
 * Copyright (c) 2020 Yury Kharchenko
 *
 * This file is part of EKA2L1 project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.eka2l1.config;


import android.util.SparseIntArray;

import com.github.eka2l1.emu.overlay.VirtualKeyboard;
import com.github.eka2l1.util.SparseIntArrayAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.io.File;


public class ProfileModel {
    /**
     * True if this is a new profile (not yet saved to file)
     */
    public final transient boolean isNew;

    public transient File dir;

    @SerializedName("Version")
    public int version;

    @SerializedName("ScreenBackgroundColor")
    public int screenBackgroundColor;

    @SerializedName("ScreenBackgroundImageOpacity")
    public int screenBackgroundImageOpacity;

    @SerializedName("ScreenBackgroundImageKeepAspectRatio")
    public boolean screenBackgroundImageKeepAspectRatio;
    @SerializedName("ScreenScaleRatio")
    public int screenScaleRatio;

    @SerializedName("Orientation")
    public int orientation;

    @SerializedName("ScreenScaleType")
    public int screenScaleType;

    @SerializedName("ScreenGravity")
    public int screenGravity;

    @SerializedName("ScreenShowNotch")
    public boolean screenShowNotch;

    @SerializedName("TouchInput")
    public boolean touchInput;

    @SerializedName("ShowKeyboard")
    public boolean showKeyboard;

    @SerializedName("VirtualKeyboardType")
    public int vkType;

    @SerializedName("VirtualKeyboardShowEditKey")
    public boolean vkShowEditKey;

    @SerializedName("ButtonShape")
    public int vkButtonShape;

    @SerializedName("VirtualKeyboardAlpha")
    public int vkAlpha;

    @SerializedName("VirtualKeyboardFeedback")
    public boolean vkFeedback;

    @SerializedName("VirtualKeyboardDelay")
    public int vkHideDelay;

    @SerializedName("VirtualKeyboardColorBackground")
    public int vkBgColor;

    @SerializedName("VirtualKeyboardColorBackgroundSelected")
    public int vkBgColorSelected;

    @SerializedName("VirtualKeyboardColorForeground")
    public int vkFgColor;

    @SerializedName("VirtualKeyboardColorForegroundSelected")
    public int vkFgColorSelected;

    @SerializedName("VirtualKeyboardColorOutline")
    public int vkOutlineColor;

    @JsonAdapter(SparseIntArrayAdapter.class)
    @SerializedName("KeyMappings")
    public SparseIntArray keyMappings;

    @SuppressWarnings("unused") // Gson uses default constructor if present
    public ProfileModel() {
        isNew = false;
    }

    public ProfileModel(File dir) {
        this.dir = dir;
        this.isNew = true;
        version = 1;
        // NGStation defaults: grey N-Gage QD body round the screen, game centred, held sideways.
        screenBackgroundColor = 0x9C9EA1;
        screenBackgroundImageOpacity = 50;
        screenBackgroundImageKeepAspectRatio = true;
        screenScaleType = 1;
        screenGravity = 2;
        orientation = 3;
        screenScaleRatio = 100;
        screenShowNotch = false;

        showKeyboard = true;
        touchInput = true;

        vkButtonShape = VirtualKeyboard.ROUND_RECT_SHAPE;
        vkAlpha = 64;

        // Dark keys with white labels, red when pressed.
        vkBgColor = 0x262626;
        vkFgColor = 0xF2F2F2;
        vkBgColorSelected = 0xE01B22;
        vkFgColorSelected = 0xFFFFFF;
        vkOutlineColor = 0x4A4A4A;
    }
}
