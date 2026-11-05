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
import android.content.res.Configuration;

import java.util.Locale;

/**
 * NGStation is English-only: every activity runs with an English configuration, so the
 * stock EKA2L1 screens and the system's own button labels ("OK", "Cancel") stay English
 * whatever language the phone uses.
 */
public final class AppLocale {
    private AppLocale() {
    }

    public static Context wrap(Context base) {
        Locale.setDefault(Locale.ENGLISH);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(Locale.ENGLISH);
        return base.createConfigurationContext(config);
    }
}
