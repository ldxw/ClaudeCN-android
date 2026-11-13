package com.host1syan.claudecn.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int INPUT_TEXT = 1;
    public static final int INPUT_URI = 16;
    public static final int SPACE_L = 16;
    public static final int SPACE_M = 12;
    public static final int SPACE_S = 8;
    public static final int SPACE_XL = 24;
    public static final int SPACE_XS = 4;

    private Ui() {
    }

    public static int dp(Context context, float f) {
        return Math.round(context.getResources().getDisplayMetrics().density * f);
    }

    public static GradientDrawable round(Context context, int i, float f) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(context, f));
        return gradientDrawable;
    }

    public static GradientDrawable roundStroke(Context context, int i, int i2, float f, float f2) {
        GradientDrawable round = round(context, i, f);
        round.setStroke(Math.max(1, dp(context, f2)), i2);
        return round;
    }

    public static RippleDrawable ripple(Context context, int i, float f) {
        return new RippleDrawable(ColorStateList.valueOf(872415231), round(context, i, f), null);
    }

    public static TextView title(Context context, String str, Palette palette) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(22.0f);
        textView.setTextColor(palette.textPrimary);
        textView.setIncludeFontPadding(false);
        return textView;
    }

    public static TextView sectionLabel(Context context, String str, Palette palette) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(12.0f);
        textView.setTextColor(palette.textMuted);
        textView.setIncludeFontPadding(false);
        return textView;
    }

    public static TextView body(Context context, String str, Palette palette) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(13.0f);
        textView.setTextColor(palette.textSecondary);
        textView.setLineSpacing(dp(context, 2.0f), 1.0f);
        return textView;
    }

    public static LinearLayout card(Context context, Palette palette) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackground(round(context, palette.surface, 16.0f));
        int dp = dp(context, 16.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        return linearLayout;
    }

    public static LinearLayout column(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        return linearLayout;
    }

    public static LinearLayout row(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(16);
        return linearLayout;
    }

    public static View gap(Context context, int i) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, i)));
        return view;
    }

    public static void addGap(LinearLayout linearLayout, Context context, int i) {
        linearLayout.addView(gap(context, i));
    }

    public static Button primaryButton(Context context, String str, Palette palette) {
        Button baseButton = baseButton(context, str, palette);
        baseButton.setTextColor(palette.onAccent);
        baseButton.setBackground(ripple(context, palette.accent, 12.0f));
        return baseButton;
    }

    public static Button secondaryButton(Context context, String str, Palette palette) {
        Button baseButton = baseButton(context, str, palette);
        baseButton.setTextColor(palette.textPrimary);
        baseButton.setBackground(ripple(context, palette.surfaceAlt, 12.0f));
        return baseButton;
    }

    public static Button dangerButton(Context context, String str, Palette palette) {
        Button baseButton = baseButton(context, str, palette);
        baseButton.setTextColor(palette.danger);
        baseButton.setBackground(ripple(context, palette.dangerSoft, 12.0f));
        return baseButton;
    }

    private static Button baseButton(Context context, String str, Palette palette) {
        Button button = new Button(context);
        button.setText(str);
        button.setAllCaps(false);
        button.setTextSize(15.0f);
        button.setGravity(17);
        button.setIncludeFontPadding(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 16.0f), dp(context, 13.0f), dp(context, 16.0f), dp(context, 13.0f));
        button.setStateListAnimator(null);
        button.setElevation(0.0f);
        return button;
    }

    public static EditText input(final Context context, final Palette palette, String str, int i) {
        final EditText editText = new EditText(context);
        editText.setHint(str);
        editText.setHintTextColor(palette.hint);
        editText.setTextColor(palette.textPrimary);
        editText.setTextSize(15.0f);
        editText.setInputType(i);
        editText.setSingleLine(true);
        editText.setIncludeFontPadding(false);
        editText.setBackground(roundStroke(context, palette.surfaceAlt, palette.border, 12.0f, 1.0f));
        int dp = dp(context, 14.0f);
        int dp2 = dp(context, 12.0f);
        editText.setPadding(dp, dp2, dp, dp2);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean z) {
                EditText editText2 = editText;
                Context context2 = context;
                int i2 = palette.surfaceAlt;
                Palette palette2 = palette;
                editText2.setBackground(Ui.roundStroke(context2, i2, z ? palette2.accent : palette2.border, 12.0f, z ? 1.5f : 1.0f));
            }
        });
        return editText;
    }

    public static TextView iconButton(Context context, String str, Palette palette, int i) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(19.0f);
        textView.setTextColor(i);
        textView.setGravity(17);
        textView.setBackground(ripple(context, 0, 12.0f));
        textView.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 42.0f), dp(context, 42.0f)));
        return textView;
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    public static LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    public static LinearLayout.LayoutParams weight(float f) {
        return new LinearLayout.LayoutParams(0, -2, f);
    }
}
