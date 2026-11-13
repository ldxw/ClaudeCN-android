package com.host1syan.claudecn.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import com.host1syan.claudecn.R;

public class Theme {
    public static final int DARK = 2;
    private static final String KEY_MODE = "theme_mode";
    public static final int LIGHT = 1;
    private static final String PREF = "claudecn_ui";
    public static final int SYSTEM = 0;

    public static int mode(Context context) {
        return context.getSharedPreferences(PREF, 0).getInt(KEY_MODE, 0);
    }

    public static void setMode(Context context, int i) {
        SharedPreferences.Editor edit = context.getSharedPreferences(PREF, 0).edit();
        edit.putInt(KEY_MODE, i);
        edit.apply();
    }

    public static boolean isDark(Context context) {
        int mode = mode(context);
        if (mode == 1) {
            return false;
        }
        return mode == 2 || (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static Palette palette(Context context) {
        return isDark(context) ? Palette.dark() : Palette.light();
    }

    public static void apply(Activity activity) {
        boolean isDark = isDark(activity);
        activity.setTheme(isDark ? R.style.AppTheme : R.style.AppThemeLight);
        Palette palette = palette(activity);
        Window window = activity.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(palette.bg));
        window.setStatusBarColor(palette.bg);
        window.setNavigationBarColor(palette.bg);
        if (Build.VERSION.SDK_INT >= 23) {
            View decorView = window.getDecorView();
            int systemUiVisibility = decorView.getSystemUiVisibility();
            decorView.setSystemUiVisibility(isDark ? systemUiVisibility & (-8193) : systemUiVisibility | 8192);
        }
    }
}
