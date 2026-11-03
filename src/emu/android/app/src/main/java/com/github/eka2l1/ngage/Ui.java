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

/** Palette and helpers for the code-built launcher screens: black, red and white like the N-Gage. */
public final class Ui {
    public static final int BLACK = 0xFF000000;
    public static final int SURFACE = 0xFF141414;
    public static final int SURFACE_HI = 0xFF1E1E1E;
    public static final int BORDER = 0xFF2E2E2E;
    public static final int RED = 0xFFE01B22;
    public static final int RED_DARK = 0xFF9E1016;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int SILVER = 0xFFB8B8B8;
    public static final int MUTED = 0xFF8A8A8A;
    public static final int OK = 0xFF5BD16A;

    // Kept for older call sites.
    static final int ACCENT = RED;

    private Ui() {
    }

    static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    /** Black action bar with a thin red rule, black status bar. */
    static void applyChrome(Activity activity, ActionBar actionBar, CharSequence title) {
        if (actionBar != null) {
            GradientDrawable base = new GradientDrawable();
            base.setColor(BLACK);
            GradientDrawable rule = new GradientDrawable();
            rule.setColor(RED);
            LayerDrawable bg = new LayerDrawable(new android.graphics.drawable.Drawable[]{rule, base});
            bg.setLayerInset(1, 0, 0, 0, dp(activity, 2));
            actionBar.setBackgroundDrawable(bg);
            actionBar.setTitle(title);
        }
        activity.getWindow().setStatusBarColor(BLACK);
        activity.getWindow().setNavigationBarColor(BLACK);
        activity.getWindow().getDecorView().setBackgroundColor(BLACK);
    }

    /** "NGSTATION" with the first two letters in red. */
    static CharSequence brand() {
        SpannableString s = new SpannableString("NGSTATION");
        s.setSpan(new ForegroundColorSpan(RED), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
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

    static LinearLayout card(Context context) {
        LinearLayout card = column(context);
        int pad = dp(context, 16);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(rounded(context, SURFACE, BORDER, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(context, 12);
        card.setLayoutParams(lp);
        return card;
    }

    /** Small red step badge ("1", "2", …) next to a card title. */
    static LinearLayout stepHeader(Context context, String number, String title) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = text(context, number, 14, true);
        badge.setTextColor(WHITE);
        badge.setGravity(Gravity.CENTER);
        int size = dp(context, 26);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(RED);
        badge.setBackground(circle);
        row.addView(badge, new LinearLayout.LayoutParams(size, size));
        TextView label = text(context, title, 17, true);
        label.setPadding(dp(context, 10), 0, 0, 0);
        row.addView(label);
        row.setPadding(0, 0, 0, dp(context, 6));
        return row;
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
        tv.setTextColor(SILVER);
        return tv;
    }

    /** Filled red button: the main action on a screen. */
    static Button button(Context context, CharSequence label) {
        return makeButton(context, label, RED, RED_DARK, 0, WHITE);
    }

    /** Dark outlined button: secondary actions. */
    static Button secondaryButton(Context context, CharSequence label) {
        return makeButton(context, label, SURFACE_HI, BORDER, BORDER, WHITE);
    }

    private static Button makeButton(Context context, CharSequence label, int fill, int pressed,
                                     int stroke, int textColor) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setStateListAnimator(null);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled}, rounded(context, 0xFF2A2A2A, 0, 10));
        states.addState(new int[]{android.R.attr.state_pressed}, rounded(context, pressed, stroke, 10));
        states.addState(new int[]{}, rounded(context, fill, stroke, 10));
        button.setBackground(states);
        button.setTextColor(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{0xFF6A6A6A, textColor}));

        int padV = dp(context, 12);
        button.setPadding(dp(context, 16), padV, dp(context, 16), padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(context, 10);
        button.setLayoutParams(lp);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    static void setBlackBackground(View view) {
        view.setBackground(new ColorDrawable(BLACK));
    }
}
