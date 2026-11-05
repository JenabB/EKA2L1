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
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

/**
 * Palette and helpers for the code-built launcher screens, taken from the N-Gage QD:
 * charcoal body, silver keys, black centre keys and orange legends.
 */
public final class Ui {
    public static final int BG = 0xFF1A1B1D;
    public static final int SURFACE = 0xFF25272A;
    public static final int SURFACE_HI = 0xFF303236;
    public static final int BORDER = 0xFF3A3D41;
    public static final int ORANGE = 0xFFFF6A1A;
    public static final int ORANGE_DARK = 0xFFD9530C;
    public static final int SILVER = 0xFFC9CBCE;
    public static final int SILVER_DARK = 0xFFA9ACB0;
    public static final int KEY_BLACK = 0xFF121314;
    public static final int CHARCOAL = 0xFF2E2F32;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int MUTED = 0xFF9A9DA2;

    private Ui() {
    }

    static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    /** Charcoal action bar with a thin orange rule, charcoal system bars. */
    static void applyChrome(Activity activity, ActionBar actionBar, CharSequence title) {
        if (actionBar != null) {
            GradientDrawable base = new GradientDrawable();
            base.setColor(BG);
            GradientDrawable rule = new GradientDrawable();
            rule.setColor(ORANGE);
            LayerDrawable bg = new LayerDrawable(new Drawable[]{rule, base});
            bg.setLayerInset(1, 0, 0, 0, dp(activity, 2));
            actionBar.setBackgroundDrawable(bg);
            actionBar.setTitle(title);
            actionBar.setElevation(0);
        }
        activity.getWindow().setStatusBarColor(BG);
        activity.getWindow().setNavigationBarColor(BG);
        activity.getWindow().getDecorView().setBackgroundColor(BG);
    }

    /** "NGSTATION" with the first two letters in orange. */
    static CharSequence brand() {
        SpannableString s = new SpannableString("NGSTATION");
        s.setSpan(new ForegroundColorSpan(ORANGE), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    static LinearLayout row(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    static GradientDrawable rounded(Context context, int fill, int stroke, float radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, radiusDp));
        bg.setColor(fill);
        if (stroke != 0) {
            bg.setStroke(dp(context, 1), stroke);
        }
        return bg;
    }

    static GradientDrawable circle(int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        return d;
    }

    static LinearLayout card(Context context) {
        LinearLayout card = column(context);
        int pad = dp(context, 16);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(rounded(context, SURFACE, BORDER, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(context, 12);
        card.setLayoutParams(lp);
        return card;
    }

    /** Round badge like the QD's black centre keys: orange digit on black, or a tick on orange when done. */
    static TextView badge(Context context, String number) {
        TextView badge = text(context, number, 15, true);
        badge.setGravity(Gravity.CENTER);
        setBadgeDone(badge, number, false);
        return badge;
    }

    static void setBadgeDone(TextView badge, String number, boolean done) {
        badge.setText(done ? "✓" : number);
        badge.setTextColor(done ? WHITE : ORANGE);
        badge.setBackground(circle(done ? ORANGE : KEY_BLACK));
    }

    static TextView text(Context context, CharSequence value, float sp, boolean bold) {
        TextView tv = new TextView(context);
        tv.setText(value);
        tv.setTextColor(WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return tv;
    }

    static TextView muted(Context context, CharSequence value, float sp) {
        TextView tv = text(context, value, sp, false);
        tv.setTextColor(MUTED);
        tv.setLineSpacing(0, 1.15f);
        return tv;
    }

    /** Orange pill: the main action on a screen. */
    static Button button(Context context, CharSequence label) {
        return makeButton(context, label, ORANGE, ORANGE_DARK, WHITE);
    }

    /** Silver pill with charcoal text, like the QD's silver keys: secondary actions. */
    static Button secondaryButton(Context context, CharSequence label) {
        return makeButton(context, label, SILVER, SILVER_DARK, CHARCOAL);
    }

    /** Borderless orange text: minor actions. */
    static Button textButton(Context context, CharSequence label) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(ORANGE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setStateListAnimator(null);
        button.setBackground(new ColorDrawable(0));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        int pad = dp(context, 8);
        button.setPadding(pad, pad, pad, pad);
        return button;
    }

    private static Button makeButton(Context context, CharSequence label, int fill, int pressed, int textColor) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setStateListAnimator(null);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled}, rounded(context, SURFACE_HI, 0, 26));
        states.addState(new int[]{android.R.attr.state_pressed}, rounded(context, pressed, 0, 26));
        states.addState(new int[]{}, rounded(context, fill, 0, 26));
        button.setBackground(states);
        button.setTextColor(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{0xFF6A6D72, textColor}));

        int padV = dp(context, 13);
        button.setPadding(dp(context, 18), padV, dp(context, 18), padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(context, 10);
        button.setLayoutParams(lp);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    static void setBackground(View view) {
        view.setBackground(new ColorDrawable(BG));
    }
}
