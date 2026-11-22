/*
 * Copyright (C) 2018 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.grarak.kerneladiutor.utils;

import android.content.Context;
import android.support.annotation.StyleRes;

import com.grarak.kerneladiutor.R;

/**
 * Created by willi on 04.03.18.
 */

public class Themes {

    public static class Theme {
        private final @StyleRes int mStyle;

        private Theme(@StyleRes int style) {
            mStyle = style;
        }

        public int getStyle() {
            return mStyle;
        }
    }

    private static final String DARK_THEME_PREF_KEY = "darktheme";
    private static final Theme LIGHT_THEME = new Theme(R.style.Theme_Default_Pink);
    private static final Theme DARK_THEME = new Theme(R.style.Theme_Default_Pink_Dark);

    public static boolean isDarkTheme(Context context) {
        return Prefs.getBoolean(DARK_THEME_PREF_KEY, true, context);
    }

    public static Theme getTheme(Context context, boolean darktheme) {
        return darktheme ? DARK_THEME : LIGHT_THEME;
    }

}
