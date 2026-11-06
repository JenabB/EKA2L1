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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * A game "card" in the shape of the MMC game cards the handheld used: a rounded card with
 * the top-right corner cut off. The colour comes from the title, so every game keeps its
 * own; the middle shows the game's icon once installed, its initials before that. A small
 * badge says whether it plays straight away or still needs installing.
 */
public class GameCoverView extends View {
    private static final int[] PALETTE = {
            0xFF3A2B4F, 0xFF1F3B4D, 0xFF4D2A22, 0xFF26412C,
            0xFF4A3A1C, 0xFF2A3556, 0xFF472236, 0xFF1E4442,
    };

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path card = new Path();
    private final RectF rect = new RectF();

    private String title = "";
    private Bitmap icon;
    private boolean installed;
    private int base = PALETTE[0];

    public GameCoverView(Context context) {
        super(context);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void bind(String title, Bitmap icon, boolean installed) {
        this.title = title == null ? "" : title;
        this.icon = icon;
        this.installed = installed;
        base = PALETTE[(this.title.hashCode() & 0x7fffffff) % PALETTE.length];
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        // Portrait card, like the real thing.
        setMeasuredDimension(w, Math.round(w * 1.18f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float r = w * 0.07f;
        float notch = w * 0.16f;

        card.reset();
        card.moveTo(r, 0);
        card.lineTo(w - notch, 0);
        card.lineTo(w, notch);
        card.lineTo(w, h - r);
        card.quadTo(w, h, w - r, h);
        card.lineTo(r, h);
        card.quadTo(0, h, 0, h - r);
        card.lineTo(0, r);
        card.quadTo(0, 0, r, 0);
        card.close();

        canvas.save();
        canvas.clipPath(card);

        // Body in the game's colour, lighter at the top.
        fill.setColor(base);
        canvas.drawRect(0, 0, w, h, fill);
        fill.setColor(0x1AFFFFFF);
        canvas.drawRect(0, 0, w, h * 0.45f, fill);

        // Diagonal stripes for texture.
        fill.setColor(0x0DFFFFFF);
        float step = w * 0.12f;
        for (float x = -h; x < w; x += step * 2) {
            Path stripe = new Path();
            stripe.moveTo(x, h);
            stripe.lineTo(x + step, h);
            stripe.lineTo(x + step + h, 0);
            stripe.lineTo(x + h, 0);
            stripe.close();
            canvas.drawPath(stripe, fill);
        }

        // Label strip at the bottom with the orange rule.
        float labelTop = h * 0.78f;
        fill.setColor(0xFF161718);
        canvas.drawRect(0, labelTop, w, h, fill);
        fill.setColor(Ui.ORANGE);
        canvas.drawRect(0, labelTop, w, labelTop + Math.max(2f, h * 0.012f), fill);
        text.setColor(0xFF8E9196);
        text.setTextSize(h * 0.075f);
        text.setLetterSpacing(0.25f);
        canvas.drawText("NGSTATION", w / 2f, labelTop + (h - labelTop) * 0.62f, text);
        text.setLetterSpacing(0f);

        // Centre: the game's icon, or its initials.
        float cx = w / 2f, cy = labelTop * 0.5f;
        if (icon != null) {
            float size = Math.min(w, labelTop) * 0.5f;
            fill.setColor(0x33000000);
            rect.set(cx - size * 0.62f, cy - size * 0.62f, cx + size * 0.62f, cy + size * 0.62f);
            canvas.drawRoundRect(rect, size * 0.22f, size * 0.22f, fill);
            rect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);
            canvas.drawBitmap(icon, null, rect, bitmapPaint);
        } else {
            text.setColor(0xFFFFFFFF);
            text.setTextSize(w * 0.3f);
            Paint.FontMetrics fm = text.getFontMetrics();
            canvas.drawText(initials(title), cx, cy - (fm.ascent + fm.descent) / 2f, text);
        }
        canvas.restore();

        // Status badge, bottom-right of the artwork.
        float br = w * 0.1f;
        float bx = w - br * 1.45f, by = labelTop - br * 1.25f;
        if (installed) {
            fill.setColor(Ui.ORANGE);
            canvas.drawCircle(bx, by, br, fill);
            Path play = new Path();
            play.moveTo(bx - br * 0.32f, by - br * 0.45f);
            play.lineTo(bx + br * 0.5f, by);
            play.lineTo(bx - br * 0.32f, by + br * 0.45f);
            play.close();
            fill.setColor(0xFFFFFFFF);
            canvas.drawPath(play, fill);
        } else {
            fill.setColor(0xCC121314);
            canvas.drawCircle(bx, by, br, fill);
            fill.setColor(Ui.SILVER);
            float a = br * 0.42f;
            canvas.drawRect(bx - br * 0.09f, by - a, bx + br * 0.09f, by + a * 0.25f, fill);
            Path arrow = new Path();
            arrow.moveTo(bx - a * 0.8f, by);
            arrow.lineTo(bx + a * 0.8f, by);
            arrow.lineTo(bx, by + a * 0.85f);
            arrow.close();
            canvas.drawPath(arrow, fill);
        }
    }

    /** First letters of up to two significant words: "Asphalt Urban GT" gives "AU". */
    static String initials(String title) {
        String[] words = title.trim().split("[\\s\\-:]+");
        // Sequels keep their number: "Asphalt Urban GT 2" gives "A2".
        if (words.length > 1 && words[words.length - 1].matches("\\d+|[IVX]+")
                && !words[0].isEmpty() && Character.isLetterOrDigit(words[0].charAt(0))) {
            String n = words[words.length - 1];
            return (Character.toUpperCase(words[0].charAt(0)) + n).substring(0, Math.min(3, n.length() + 1));
        }
        StringBuilder sb = new StringBuilder();
        for (String word : title.split("[\\s\\-:]+")) {
            if (word.isEmpty() || word.equalsIgnoreCase("the") || word.equalsIgnoreCase("of")) {
                continue;
            }
            char c = word.charAt(0);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
