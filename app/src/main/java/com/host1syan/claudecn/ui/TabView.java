package com.host1syan.claudecn.ui;

import android.content.Context;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TabView extends LinearLayout {
    private final StatusDot dot;
    private final TextView label;
    private final Palette palette;
    public final String serverId;

    public TabView(Context context, String str, String str2, Palette palette) {
        super(context);
        this.serverId = str;
        this.palette = palette;
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(16);
        setPadding(Ui.dp(context, 14.0f), 0, Ui.dp(context, 6.0f), 0);
        TextView textView = new TextView(context);
        this.label = textView;
        textView.setText(str2);
        textView.setTextSize(14.0f);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setIncludeFontPadding(false);
        textView.setTextColor(palette.textSecondary);
        addView(textView);
        StatusDot statusDot = new StatusDot(context, palette);
        this.dot = statusDot;
        addView(statusDot);
    }

    public void setState(int i) {
        this.dot.setState(i);
    }

    public void setActive(boolean z) {
        TextView textView = this.label;
        Palette palette = this.palette;
        textView.setTextColor(z ? palette.textPrimary : palette.textSecondary);
        this.label.getPaint().setFakeBoldText(z);
        this.label.invalidate();
        setBackground(z ? Ui.ripple(getContext(), this.palette.tabActiveBg, 12.0f) : null);
    }
}
