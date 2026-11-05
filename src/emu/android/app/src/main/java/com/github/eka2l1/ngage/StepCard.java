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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One setup step: a badge, a title and a one-line status. While the step is open its
 * explanation and buttons show; once done it folds to the status line, with "Ubah"
 * to open it again.
 */
final class StepCard {
    final LinearLayout card;
    final LinearLayout body;
    private final TextView badge;
    private final TextView status;
    private final Button toggle;
    private final String number;
    private boolean done;
    private boolean userOpened;

    StepCard(Context ctx, String number, String title, String description) {
        this.number = number;
        card = Ui.card(ctx);

        LinearLayout header = Ui.row(ctx);
        badge = Ui.badge(ctx, number);
        int size = Ui.dp(ctx, 30);
        header.addView(badge, new LinearLayout.LayoutParams(size, size));

        LinearLayout titles = Ui.column(ctx);
        titles.setPadding(Ui.dp(ctx, 12), 0, 0, 0);
        titles.addView(Ui.text(ctx, title, 17, true));
        status = Ui.muted(ctx, "", 13);
        titles.addView(status);
        header.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toggle = Ui.textButton(ctx, "Ubah");
        toggle.setVisibility(View.GONE);
        toggle.setOnClickListener(v -> {
            userOpened = !userOpened;
            apply();
        });
        header.addView(toggle);
        card.addView(header);

        body = Ui.column(ctx);
        body.setPadding(0, Ui.dp(ctx, 12), 0, 0);
        if (description != null) {
            body.addView(Ui.muted(ctx, description, 14));
        }
        card.addView(body);
    }

    void addAction(View view) {
        body.addView(view);
    }

    /** Sets whether the step is done and the status line under its title. */
    void setState(boolean done, String statusText) {
        if (this.done && !done) {
            userOpened = false;
        }
        this.done = done;
        status.setText(statusText);
        status.setTextColor(done ? Ui.SILVER : Ui.MUTED);
        apply();
    }

    private void apply() {
        Ui.setBadgeDone(badge, number, done);
        boolean open = !done || userOpened;
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        toggle.setVisibility(done ? View.VISIBLE : View.GONE);
        toggle.setText(userOpened ? "Tutup" : "Ubah");
    }

    boolean isDone() {
        return done;
    }
}
