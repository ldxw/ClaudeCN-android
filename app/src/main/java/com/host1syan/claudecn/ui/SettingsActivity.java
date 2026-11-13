package com.host1syan.claudecn.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.host1syan.claudecn.BuildConfig;
import com.host1syan.claudecn.NativeAbi;
import com.host1syan.claudecn.R;

public class SettingsActivity extends Activity {
    private Palette palette;
    private LinearLayout themeRow;

    @Override
    protected void onCreate(Bundle bundle) {
        Theme.apply(this);
        super.onCreate(bundle);
        this.palette = Theme.palette(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(this.palette.bg);
        scrollView.setFillViewport(true);
        LinearLayout column = Ui.column(this);
        int dp = Ui.dp(this, 20.0f);
        column.setPadding(dp, dp, dp, dp);
        scrollView.addView(column, Ui.matchWrap());
        column.addView(Ui.title(this, "设置", this.palette));
        Ui.addGap(column, this, 20);
        LinearLayout card = Ui.card(this, this.palette);
        column.addView(card, Ui.matchWrap());
        card.addView(Ui.sectionLabel(this, "外观", this.palette));
        Ui.addGap(card, this, 10);
        LinearLayout row = Ui.row(this);
        this.themeRow = row;
        card.addView(row, Ui.matchWrap());
        renderThemeButtons();
        Ui.addGap(card, this, 10);
        TextView body = Ui.body(this, "「跟随系统」会随手机的深色模式切换。改完回到主界面生效。", this.palette);
        body.setTextSize(12.0f);
        body.setTextColor(this.palette.textMuted);
        card.addView(body);
        Ui.addGap(column, this, 12);
        LinearLayout card2 = Ui.card(this, this.palette);
        column.addView(card2, Ui.matchWrap());
        card2.addView(Ui.sectionLabel(this, "存储", this.palette));
        Ui.addGap(card2, this, 6);
        card2.addView(Ui.body(this, "查看每台服务器占了多少空间，按台清理。", this.palette));
        Ui.addGap(card2, this, 12);
        Button secondaryButton = Ui.secondaryButton(this, "缓存管理", this.palette);
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, (Class<?>) CacheActivity.class));
            }
        });
        card2.addView(secondaryButton, Ui.matchWrap());
        Ui.addGap(column, this, 24);
        LinearLayout row2 = Ui.row(this);
        row2.setGravity(17);
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.ic_logo);
        int dp2 = Ui.dp(this, 28.0f);
        row2.addView(imageView, new LinearLayout.LayoutParams(dp2, dp2));
        Ui.addGap(row2, this, 0);
        TextView textView = new TextView(this);
        textView.setText("ClaudeCN 客户端 " + BuildConfig.VERSION_NAME);
        textView.setTextSize(12.0f);
        textView.setTextColor(this.palette.textMuted);
        textView.setIncludeFontPadding(false);
        LinearLayout.LayoutParams wrapWrap = Ui.wrapWrap();
        wrapWrap.leftMargin = Ui.dp(this, 8.0f);
        row2.addView(textView, wrapWrap);
        column.addView(row2, Ui.matchWrap());
        Ui.addGap(column, this, 6);
        TextView abiText = Ui.body(this, "架构 " + NativeAbi.abi(), this.palette);
        abiText.setTextSize(11.0f);
        abiText.setTextColor(this.palette.textMuted);
        abiText.setGravity(17);
        column.addView(abiText, Ui.matchWrap());
        setContentView(scrollView);
    }

    private void renderThemeButtons() {
        GradientDrawable round;
        this.themeRow.removeAllViews();
        int mode = Theme.mode(this);
        String[] strArr = {"跟随系统", "浅色", "深色"};
        int[] iArr = {0, 1, 2};
        for (int i = 0; i < 3; i++) {
            final int i2 = iArr[i];
            boolean z = i2 == mode;
            TextView textView = new TextView(this);
            textView.setText(strArr[i]);
            textView.setTextSize(14.0f);
            textView.setGravity(17);
            textView.setIncludeFontPadding(false);
            textView.setPadding(0, Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f));
            Palette palette = this.palette;
            textView.setTextColor(z ? palette.onAccent : palette.textSecondary);
            if (z) {
                round = Ui.round(this, this.palette.accent, 10.0f);
            } else {
                round = Ui.round(this, this.palette.surfaceAlt, 10.0f);
            }
            textView.setBackground(round);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Theme.setMode(SettingsActivity.this, i2);
                    SettingsActivity.this.recreate();
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
            if (i > 0) {
                layoutParams.setMarginStart(Ui.dp(this, 8.0f));
            }
            this.themeRow.addView(textView, layoutParams);
        }
    }
}
