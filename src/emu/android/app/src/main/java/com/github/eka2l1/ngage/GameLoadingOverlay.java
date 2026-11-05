/*
 * Copyright (c) 2026 NGStation contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.github.eka2l1.ngage;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Black loading screen shown while the emulated phone boots and the game starts. The core
 * gives no "first frame" signal, so the emulator surface is sampled every half second and
 * the overlay goes away once it shows real picture content (more than a handful of colours).
 */
public final class GameLoadingOverlay {
    private static final long POLL_MS = 500;
    private static final long TIMEOUT_MS = 60_000;
    private static final long FALLBACK_MS = 8_000;
    private static final int SAMPLE_W = 48;
    private static final int SAMPLE_H = 27;
    private static final int COLOURS_FOR_CONTENT = 6;

    private static final String[] TIPS = {
            "Powering on the virtual handheld…",
            "Loading Symbian OS…",
            "Starting the game…",
            "Almost there…",
    };

    private final Activity activity;
    private final SurfaceView surface;
    private final View root;
    private final TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long startedAt = System.currentTimeMillis();
    private Bitmap sample;
    private boolean polling;
    private boolean dismissed;

    private GameLoadingOverlay(Activity activity, SurfaceView surface, String title) {
        this.activity = activity;
        this.surface = surface;

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Ui.BG);
        box.setClickable(true);

        TextView brand = Ui.text(activity, Ui.brand(), 26, true);
        brand.setLetterSpacing(0.12f);
        brand.setGravity(Gravity.CENTER);
        box.addView(brand);

        if (title != null) {
            TextView name = Ui.text(activity, title, 18, true);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, Ui.dp(activity, 18), 0, Ui.dp(activity, 18));
            box.addView(name);
        }

        ProgressBar spinner = new ProgressBar(activity);
        spinner.setIndeterminateTintList(ColorStateList.valueOf(Ui.ORANGE));
        int size = Ui.dp(activity, 48);
        box.addView(spinner, new LinearLayout.LayoutParams(size, size));

        status = Ui.muted(activity, TIPS[0], 14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, Ui.dp(activity, 14), 0, 0);
        box.addView(status);

        TextView hint = Ui.muted(activity, "Tap to dismiss", 11);
        hint.setTextColor(0xFF555555);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(activity, 28), 0, 0);
        box.addView(hint);
        box.setOnClickListener(v -> dismiss());

        root = box;
        activity.addContentView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Puts the overlay over the whole activity. Call {@link #start()} once the surface is live. */
    public static GameLoadingOverlay show(Activity activity, SurfaceView surface, String title) {
        return new GameLoadingOverlay(activity, surface, title);
    }

    /** Starts watching the emulator surface. Safe to call more than once. */
    public void start() {
        if (polling || dismissed) {
            return;
        }
        polling = true;
        handler.postDelayed(this::tick, POLL_MS);
    }

    private void tick() {
        if (dismissed) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        int tip = (int) Math.min(TIPS.length - 1, elapsed / 4000);
        status.setText(TIPS[tip]);

        if (elapsed > TIMEOUT_MS) {
            dismiss();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // No way to read the surface back before Android 7: fall back to a fixed delay.
            if (elapsed > FALLBACK_MS) {
                dismiss();
            } else {
                handler.postDelayed(this::tick, POLL_MS);
            }
            return;
        }
        if (surface.getHolder().getSurface() == null || !surface.getHolder().getSurface().isValid()) {
            handler.postDelayed(this::tick, POLL_MS);
            return;
        }
        if (sample == null) {
            sample = Bitmap.createBitmap(SAMPLE_W, SAMPLE_H, Bitmap.Config.ARGB_8888);
        }
        try {
            PixelCopy.request(surface, sample, result -> {
                if (result == PixelCopy.SUCCESS && hasContent(sample)) {
                    dismiss();
                } else {
                    handler.postDelayed(this::tick, POLL_MS);
                }
            }, handler);
        } catch (IllegalArgumentException e) {
            handler.postDelayed(this::tick, POLL_MS);
        }
    }

    private static boolean hasContent(Bitmap bitmap) {
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                // Quantise so that dithering noise on a flat colour does not count.
                colours.add(bitmap.getPixel(x, y) & 0xF0F0F0);
                if (colours.size() >= COLOURS_FOR_CONTENT) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dismiss() {
        if (dismissed) {
            return;
        }
        dismissed = true;
        handler.removeCallbacksAndMessages(null);
        root.animate().alpha(0f).setDuration(250).withEndAction(() -> {
            root.setVisibility(View.GONE);
            ViewGroup parent = (ViewGroup) root.getParent();
            if (parent != null) {
                parent.removeView(root);
            }
        }).start();
    }
}
