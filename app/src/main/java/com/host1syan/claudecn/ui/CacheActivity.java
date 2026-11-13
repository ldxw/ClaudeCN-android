package com.host1syan.claudecn.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.host1syan.claudecn.cache.AssetCache;
import com.host1syan.claudecn.model.ServerConfig;
import com.host1syan.claudecn.store.ServerStore;
import java.util.Iterator;
import java.util.List;

public class CacheActivity extends Activity {
    private LinearLayout list;
    private Palette palette;
    private ServerStore store;

    @Override
    protected void onCreate(Bundle bundle) {
        Theme.apply(this);
        super.onCreate(bundle);
        this.palette = Theme.palette(this);
        this.store = new ServerStore(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(this.palette.bg);
        scrollView.setFillViewport(true);
        LinearLayout column = Ui.column(this);
        int dp = Ui.dp(this, 20.0f);
        column.setPadding(dp, dp, dp, dp);
        scrollView.addView(column, Ui.matchWrap());
        column.addView(Ui.title(this, "缓存管理", this.palette));
        Ui.addGap(column, this, 4);
        column.addView(Ui.body(this, "静态资源按服务器分开存在本机，二次打开不用重新下载。", this.palette));
        Ui.addGap(column, this, 20);
        LinearLayout column2 = Ui.column(this);
        this.list = column2;
        column.addView(column2, Ui.matchWrap());
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    public void render() {
        this.list.removeAllViews();
        List<ServerConfig> list = this.store.list();
        if (list.isEmpty()) {
            TextView body = Ui.body(this, "还没有配置服务器", this.palette);
            body.setTextColor(this.palette.textMuted);
            this.list.addView(body);
            return;
        }
        long j = 0;
        for (ServerConfig serverConfig : list) {
            final AssetCache assetCache = new AssetCache(this, serverConfig.id);
            long sizeBytes = assetCache.sizeBytes();
            int count = assetCache.count();
            j += sizeBytes;
            LinearLayout card = Ui.card(this, this.palette);
            LinearLayout.LayoutParams matchWrap = Ui.matchWrap();
            matchWrap.bottomMargin = Ui.dp(this, 10.0f);
            this.list.addView(card, matchWrap);
            TextView textView = new TextView(this);
            textView.setText(serverConfig.displayName());
            textView.setTextSize(16.0f);
            textView.setTextColor(this.palette.textPrimary);
            textView.setIncludeFontPadding(false);
            card.addView(textView);
            Ui.addGap(card, this, 6);
            TextView textView2 = new TextView(this);
            textView2.setText(humanSize(sizeBytes) + "  ·  " + count + " 个文件");
            textView2.setTextSize(13.0f);
            textView2.setTextColor(this.palette.textSecondary);
            textView2.setIncludeFontPadding(false);
            card.addView(textView2);
            Ui.addGap(card, this, 2);
            TextView textView3 = new TextView(this);
            textView3.setText(serverConfig.url);
            textView3.setTextSize(11.0f);
            textView3.setTextColor(this.palette.textMuted);
            textView3.setSingleLine(true);
            textView3.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            card.addView(textView3);
            Ui.addGap(card, this, 14);
            Button secondaryButton = Ui.secondaryButton(this, "清空这台的缓存", this.palette);
            secondaryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    assetCache.clear();
                    Toast.makeText(CacheActivity.this, "已清空", Toast.LENGTH_SHORT).show();
                    CacheActivity.this.render();
                }
            });
            card.addView(secondaryButton, Ui.matchWrap());
        }
        Ui.addGap(this.list, this, 8);
        LinearLayout row = Ui.row(this);
        row.setGravity(16);
        this.list.addView(row, Ui.matchWrap());
        TextView textView4 = new TextView(this);
        textView4.setText("合计 " + humanSize(j));
        textView4.setTextSize(13.0f);
        textView4.setTextColor(this.palette.textMuted);
        textView4.setIncludeFontPadding(false);
        row.addView(textView4, Ui.weight(1.0f));
        Button dangerButton = Ui.dangerButton(this, "清空全部", this.palette);
        dangerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CacheActivity.this.confirmClearAll();
            }
        });
        row.addView(dangerButton);
    }

    public void confirmClearAll() {
        new AlertDialog.Builder(this).setTitle("清空全部缓存").setMessage("下次打开每台服务器都会重新下载静态文件，会慢一阵。确定吗？").setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("清空", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Iterator<ServerConfig> it = CacheActivity.this.store.list().iterator();
                while (it.hasNext()) {
                    new AssetCache(CacheActivity.this, it.next().id).clear();
                }
                CacheActivity.this.render();
            }
        }).show();
    }

    private static String humanSize(long j) {
        if (j >= 1024) {
            return j < 1048576 ? String.format("%.1f KB", Double.valueOf(j / 1024.0d)) : String.format("%.1f MB", Double.valueOf(j / 1048576.0d));
        }
        return j + " B";
    }
}
