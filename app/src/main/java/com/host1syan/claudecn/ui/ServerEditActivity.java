package com.host1syan.claudecn.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.host1syan.claudecn.cache.AssetCache;
import com.host1syan.claudecn.model.ServerConfig;
import com.host1syan.claudecn.store.ServerStore;

public class ServerEditActivity extends Activity {
    private String editingId;
    private String keptToken = "";
    private EditText nameField;
    private Palette palette;
    private ServerStore store;
    private EditText urlField;

    @Override
    protected void onCreate(Bundle bundle) {
        ServerConfig serverConfig;
        Theme.apply(this);
        super.onCreate(bundle);
        this.palette = Theme.palette(this);
        this.store = new ServerStore(this);
        this.editingId = getIntent().getStringExtra("id");
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(this.palette.bg);
        scrollView.setFillViewport(true);
        LinearLayout column = Ui.column(this);
        int dp = Ui.dp(this, 20.0f);
        column.setPadding(dp, dp, dp, dp);
        scrollView.addView(column, Ui.matchWrap());
        column.addView(Ui.title(this, this.editingId == null ? "添加服务器" : "编辑服务器", this.palette));
        Ui.addGap(column, this, 4);
        column.addView(Ui.body(this, this.editingId == null ? "填一个名字方便区分，再填服务器地址。" : "改完保存，这台服务器的页面会重新加载。", this.palette));
        Ui.addGap(column, this, 20);
        LinearLayout card = Ui.card(this, this.palette);
        column.addView(card, Ui.matchWrap());
        card.addView(Ui.sectionLabel(this, "名称", this.palette));
        Ui.addGap(card, this, 6);
        EditText input = Ui.input(this, this.palette, "例如：家里、公司", 1);
        this.nameField = input;
        card.addView(input, Ui.matchWrap());
        Ui.addGap(card, this, 16);
        card.addView(Ui.sectionLabel(this, "地址", this.palette));
        Ui.addGap(card, this, 6);
        EditText input2 = Ui.input(this, this.palette, "例如：https://your-host:3456", 16);
        this.urlField = input2;
        card.addView(input2, Ui.matchWrap());
        Ui.addGap(column, this, 12);
        TextView body = Ui.body(this, "地址不用写 http://，会自动补 https。\n如果这台服务器开了 H5 访问令牌，首次打开时在网页里输入一次即可。", this.palette);
        body.setTextSize(12.0f);
        body.setTextColor(this.palette.textMuted);
        column.addView(body);
        Ui.addGap(column, this, 24);
        Button primaryButton = Ui.primaryButton(this, "保存", this.palette);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerEditActivity.this.save();
            }
        });
        column.addView(primaryButton, Ui.matchWrap());
        if (this.editingId != null) {
            Ui.addGap(column, this, 10);
            Button dangerButton = Ui.dangerButton(this, "删除这台服务器", this.palette);
            dangerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ServerEditActivity.this.confirmDelete();
                }
            });
            column.addView(dangerButton, Ui.matchWrap());
        }
        String str = this.editingId;
        if (str != null && (serverConfig = this.store.get(str)) != null) {
            this.nameField.setText(serverConfig.name);
            this.urlField.setText(serverConfig.url);
            this.keptToken = serverConfig.token == null ? "" : serverConfig.token;
        }
        setContentView(scrollView);
    }

    public void save() {
        String normalizeUrl = ServerConfig.normalizeUrl(this.urlField.getText().toString());
        if (normalizeUrl.isEmpty()) {
            Toast.makeText(this, "地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!normalizeUrl.startsWith("https://") && !normalizeUrl.startsWith("http://")) {
            Toast.makeText(this, "地址格式不对", Toast.LENGTH_SHORT).show();
            return;
        }
        String str = this.editingId;
        if (str == null) {
            str = this.store.newId();
        }
        this.store.save(new ServerConfig(str, this.nameField.getText().toString().trim(), normalizeUrl, this.keptToken));
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void confirmDelete() {
        new AlertDialog.Builder(this).setTitle("删除服务器").setMessage("会同时清掉这台服务器的本地缓存，确定吗？").setNegativeButton("取消", (DialogInterface.OnClickListener) null).setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (ServerEditActivity.this.editingId != null) {
                    ServerEditActivity serverEditActivity = ServerEditActivity.this;
                    new AssetCache(serverEditActivity, serverEditActivity.editingId).clear();
                    ServerEditActivity.this.store.remove(ServerEditActivity.this.editingId);
                }
                ServerEditActivity.this.finish();
            }
        }).show();
    }
}
