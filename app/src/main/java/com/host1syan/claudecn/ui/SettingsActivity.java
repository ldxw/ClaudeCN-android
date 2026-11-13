package com.host1syan.claudecn.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import com.host1syan.claudecn.BuildConfig;
import com.host1syan.claudecn.NativeAbi;
import com.host1syan.claudecn.R;
import com.host1syan.claudecn.update.MirrorSelector;
import com.host1syan.claudecn.update.UpdateChecker;
import com.host1syan.claudecn.update.UpdateInfo;
import com.host1syan.claudecn.update.UpdateInstaller;
import com.host1syan.claudecn.update.UpdateSettings;
import java.io.File;
import java.util.List;

public class SettingsActivity extends Activity {
    /** 0 自动选路、1 官方直连、2 自定义。 */
    private static final String[] MIRROR_LABELS = {"自动选路", "官方直连", "自定义"};
    private static final int MODE_INDEX_AUTO = 0;
    private static final int MODE_INDEX_OFFICIAL = 1;
    private static final int MODE_INDEX_CUSTOM = 2;
    /** 主界面弹窗点「去更新」时带上，进页面即自动开始检查。 */
    public static final String EXTRA_CHECK_NOW = "check_now";

    private Palette palette;
    private LinearLayout themeRow;
    private TextView updateStatus;
    private Button updateButton;
    private boolean checking;
    private UpdateInfo pending;
    /** 线路分段选择状态：0 自动、1 官方、2 自定义。 */
    private int mirrorMode;
    private LinearLayout mirrorRow;
    private EditText mirrorInput;
    private TextView mirrorError;
    /** 自动选路相关控件。 */
    private LinearLayout mirrorAutoBox;
    private LinearLayout mirrorManualBox;
    /** 自定义线路的说明 + 输入框 + 保存按钮，仅选中「自定义」时显示。 */
    private LinearLayout mirrorCustomBox;
    private TextView mirrorSpeedText;
    private boolean probing;

    /** 允许用户从系统设置返回后自动继续安装。 */
    private boolean awaitingInstallPermission;
    /** 已下载完成、等待唤起安装器的安装包。 */
    private File pendingApk;

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
        Ui.addGap(column, this, 12);
        column.addView(buildUpdateCard(), Ui.matchWrap());
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
        if (getIntent().getBooleanExtra(EXTRA_CHECK_NOW, false)) {
            checkForUpdate();
        }
    }

    private LinearLayout buildUpdateCard() {
        LinearLayout card = Ui.card(this, this.palette);
        card.addView(Ui.sectionLabel(this, "更新", this.palette));
        Ui.addGap(card, this, 6);
        card.addView(Ui.body(this, "当前版本 " + BuildConfig.VERSION_NAME, this.palette));
        Ui.addGap(card, this, 6);
        TextView hint = Ui.body(this, "从 GitHub Releases 获取最新版本，下载后交给系统安装。", this.palette);
        hint.setTextSize(12.0f);
        hint.setTextColor(this.palette.textMuted);
        card.addView(hint);
        Ui.addGap(card, this, 12);

        card.addView(buildMirrorSection());
        Ui.addGap(card, this, 12);
        card.addView(buildAutoCheckRow());
        Ui.addGap(card, this, 12);

        Button button = Ui.secondaryButton(this, "检查更新", this.palette);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUpdateAction();
            }
        });
        card.addView(button, Ui.matchWrap());
        this.updateButton = button;

        TextView status = new TextView(this);
        status.setTextSize(12.0f);
        status.setTextColor(this.palette.textMuted);
        status.setIncludeFontPadding(false);
        status.setVisibility(View.GONE);
        status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReleaseNotes();
            }
        });
        LinearLayout.LayoutParams statusParams = Ui.matchWrap();
        statusParams.topMargin = Ui.dp(this, 10.0f);
        card.addView(status, statusParams);
        this.updateStatus = status;
        return card;
    }

    private LinearLayout buildMirrorSection() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(Ui.sectionLabel(this, "下载线路", this.palette));
        Ui.addGap(box, this, 8);

        LinearLayout row = Ui.row(this);
        this.mirrorRow = row;
        box.addView(row, Ui.matchWrap());

        // 自动选路：展示上次测速结果，可手动重新测速
        LinearLayout auto = new LinearLayout(this);
        auto.setOrientation(LinearLayout.VERTICAL);
        this.mirrorAutoBox = auto;
        Ui.addGap(auto, this, 10);

        TextView speed = new TextView(this);
        speed.setTextSize(12.0f);
        speed.setTextColor(this.palette.textMuted);
        speed.setIncludeFontPadding(false);
        this.mirrorSpeedText = speed;
        auto.addView(speed, Ui.matchWrap());
        Ui.addGap(auto, this, 8);

        Button probe = Ui.secondaryButton(this, "重新测速", this.palette);
        probe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMirrorProbe();
            }
        });
        auto.addView(probe, Ui.matchWrap());
        box.addView(auto, Ui.matchWrap());

        // 手动模式说明：文字说明只在非自动模式下露出
        LinearLayout manual = new LinearLayout(this);
        manual.setOrientation(LinearLayout.VERTICAL);
        manual.setVisibility(View.GONE);
        this.mirrorManualBox = manual;

        TextView hint = Ui.body(this, "GitHub 直连不稳时可换成加速镜像，只影响安装包下载。", this.palette);
        hint.setTextSize(12.0f);
        hint.setTextColor(this.palette.textMuted);
        hint.setPadding(0, Ui.dp(this, 10.0f), 0, 0);
        manual.addView(hint);
        box.addView(manual, Ui.matchWrap());

        // 自定义线路：默认隐藏，只有选中「自定义」时才露出输入框和保存按钮
        LinearLayout custom = new LinearLayout(this);
        custom.setOrientation(LinearLayout.VERTICAL);
        custom.setVisibility(View.GONE);
        this.mirrorCustomBox = custom;
        Ui.addGap(custom, this, 12);

        TextView customHint = Ui.body(this,
                "填入前缀式加速地址，安装包下载时会拼在原始 GitHub 地址前面；必须是 https:// 开头。",
                this.palette);
        customHint.setTextSize(12.0f);
        customHint.setTextColor(this.palette.textMuted);
        customHint.setPadding(0, 0, 0, Ui.dp(this, 8.0f));
        custom.addView(customHint);

        EditText input = Ui.input(this, this.palette, "https://example.com/", Ui.INPUT_URI);
        this.mirrorInput = input;
        custom.addView(input, Ui.matchWrap());
        Ui.addGap(custom, this, 8);

        Button save = Ui.secondaryButton(this, "保存自定义线路", this.palette);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCustomMirror();
            }
        });
        custom.addView(save, Ui.matchWrap());

        TextView error = new TextView(this);
        error.setTextSize(12.0f);
        error.setTextColor(this.palette.danger);
        error.setIncludeFontPadding(false);
        error.setVisibility(View.GONE);
        error.setPadding(0, Ui.dp(this, 8.0f), 0, 0);
        this.mirrorError = error;
        custom.addView(error);

        box.addView(custom, Ui.matchWrap());

        String current = UpdateSettings.mirrorBase(this);
        this.mirrorMode = currentMirrorIndex(current);
        if (!current.isEmpty()) {
            input.setText(current);
        }
        renderMirrorButtons();
        renderMirrorSpeed();
        return box;
    }

    /** 由已存的模式与前缀推出分段选择的下标。 */
    private int currentMirrorIndex(String base) {
        if (UpdateSettings.MODE_AUTO.equals(UpdateSettings.mirrorMode(this))) {
            return MODE_INDEX_AUTO;
        }
        if (base.isEmpty()) {
            return MODE_INDEX_OFFICIAL;
        }
        return MODE_INDEX_CUSTOM;
    }

    /** 用缓存结果渲染「自动」下的速度摘要；没有缓存就提示未测速。 */
    private void renderMirrorSpeed() {
        if (this.mirrorSpeedText == null) {
            return;
        }
        String extra = UpdateSettings.mirrorBase(this);
        if (this.probing) {
            this.mirrorSpeedText.setText("正在并发探测 "
                    + MirrorSelector.candidateCount(extra) + " 条线路…");
            return;
        }
        long now = System.currentTimeMillis();
        String cached = UpdateSettings.cachedRank(this, now);
        if (cached == null || !coversCustom(cached, extra)) {
            this.mirrorSpeedText.setText("尚未测速，首次下载时会自动挑选最快的可用线路。");
            return;
        }
        List<String> bases = MirrorSelector.decodeRank(cached);
        if (bases.isEmpty()) {
            this.mirrorSpeedText.setText("上次测速没有可用镜像，将使用官方直连。");
            return;
        }
        StringBuilder sb = new StringBuilder("上次测速顺序：");
        for (int i = 0; i < bases.size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(MirrorSelector.label(bases.get(i)));
        }
        this.mirrorSpeedText.setText(sb.toString());
    }

    /** 缓存里是否已经包含当前自定义前缀；不含说明缓存过期，摘要不该当真。 */
    private boolean coversCustom(String cached, String extra) {
        String custom = UpdateSettings.normalizeMirrorBase(extra);
        if (custom == null || custom.isEmpty()) {
            return true;
        }
        return MirrorSelector.decodeRank(cached).contains(custom);
    }

    /** 手动触发一次测速并写缓存。 */
    private void startMirrorProbe() {
        if (this.probing) {
            return;
        }
        UpdateInfo info = this.pending;
        final String url = info != null && info.downloadUrl != null && !info.downloadUrl.isEmpty()
                ? info.downloadUrl
                : null;
        if (url == null) {
            setUpdateStatus("请先检查更新，拿到安装包地址后再测速", this.palette.warning);
            return;
        }
        this.probing = true;
        renderMirrorSpeed();
        final String extra = UpdateSettings.mirrorBase(this);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 强制重新测速，不吃缓存；自定义前缀一并参与
                final List<String> result =
                        MirrorSelector.refreshRank(SettingsActivity.this, url, extra);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SettingsActivity.this.probing = false;
                        renderMirrorSpeed();
                        int ok = 0;
                        for (String base : result) {
                            if (!base.isEmpty()) {
                                ok++;
                            }
                        }
                        setUpdateStatus(ok > 0
                                ? "测速完成，可用镜像 " + ok + " 条"
                                : "没有镜像可用，将使用官方直连",
                                ok > 0 ? SettingsActivity.this.palette.success
                                        : SettingsActivity.this.palette.warning);
                    }
                });
            }
        }, "mirror-probe-ui");
        thread.setDaemon(true);
        thread.start();
    }

    private LinearLayout buildAutoCheckRow() {
        LinearLayout row = Ui.row(this);
        TextView label = Ui.body(this, "启动时自动检查更新", this.palette);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        row.addView(label, labelParams);

        Switch toggle = new Switch(this);
        toggle.setChecked(UpdateSettings.autoCheck(this));
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                UpdateSettings.setAutoCheck(SettingsActivity.this, isChecked);
            }
        });
        LinearLayout.LayoutParams toggleParams = Ui.wrapWrap();
        toggleParams.setMarginStart(Ui.dp(this, 8.0f));
        row.addView(toggle, toggleParams);
        return row;
    }

    private void renderMirrorButtons() {
        if (this.mirrorRow == null) {
            return;
        }
        this.mirrorRow.removeAllViews();
        for (int i = 0; i < MIRROR_LABELS.length; i++) {
            final int index = i;
            boolean selected = index == this.mirrorMode;
            TextView item = new TextView(this);
            item.setText(MIRROR_LABELS[i]);
            item.setTextSize(13.0f);
            item.setGravity(17);
            item.setIncludeFontPadding(false);
            item.setSingleLine(true);
            item.setPadding(0, Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f));
            item.setTextColor(selected ? this.palette.onAccent : this.palette.textSecondary);
            item.setBackground(selected
                    ? Ui.round(this, this.palette.accent, 10.0f)
                    : Ui.round(this, this.palette.surfaceAlt, 10.0f));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    applyMirrorMode(index);
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1.0f);
            if (i > 0) {
                params.setMarginStart(Ui.dp(this, 8.0f));
            }
            this.mirrorRow.addView(item, params);
        }
        if (this.mirrorAutoBox != null) {
            this.mirrorAutoBox.setVisibility(
                    this.mirrorMode == MODE_INDEX_AUTO ? View.VISIBLE : View.GONE);
        }
        if (this.mirrorManualBox != null) {
            this.mirrorManualBox.setVisibility(
                    this.mirrorMode == MODE_INDEX_AUTO ? View.GONE : View.VISIBLE);
        }
        if (this.mirrorCustomBox != null) {
            this.mirrorCustomBox.setVisibility(
                    this.mirrorMode == MODE_INDEX_CUSTOM ? View.VISIBLE : View.GONE);
        }
    }

    /** 切换线路：自动/直连立刻落盘，自定义项等用户填完再落盘。 */
    private void applyMirrorMode(int index) {
        this.mirrorMode = index;
        setMirrorError("");
        if (index == MODE_INDEX_AUTO) {
            UpdateSettings.setMirrorMode(this, UpdateSettings.MODE_AUTO);
        } else if (index == MODE_INDEX_OFFICIAL) {
            UpdateSettings.setMirrorMode(this, UpdateSettings.MODE_MANUAL);
            UpdateSettings.setMirrorBase(this, "");
        } else if (this.mirrorInput != null) {
            // 选中「自定义」即切到手动模式，前缀沿用已存值；用户再改完点保存
            UpdateSettings.setMirrorMode(this, UpdateSettings.MODE_MANUAL);
            this.mirrorInput.setText(UpdateSettings.mirrorBase(this));
        }
        renderMirrorButtons();
        renderMirrorSpeed();
    }

    private void saveCustomMirror() {
        String raw = this.mirrorInput == null ? "" : this.mirrorInput.getText().toString();
        String normalized = UpdateSettings.normalizeMirrorBase(raw);
        if (normalized == null) {
            setMirrorError("线路地址必须是 https:// 开头且不含空格");
            return;
        }
        if (normalized.isEmpty()) {
            setMirrorError("请填写完整的镜像地址，例如 https://example.com/");
            return;
        }
        setMirrorError("");
        UpdateSettings.setMirrorMode(this, UpdateSettings.MODE_MANUAL);
        UpdateSettings.setMirrorBase(this, normalized);
        this.mirrorMode = MODE_INDEX_CUSTOM;
        if (this.mirrorInput != null) {
            this.mirrorInput.setText(normalized);
        }
        // 缓存里没有这条新前缀，清掉让下次下载重新测速把它算进去
        UpdateSettings.clearCachedRank(this);
        renderMirrorButtons();
        renderMirrorSpeed();
        setUpdateStatus("已切换到 " + normalized, this.palette.success);
    }

    private void setMirrorError(String message) {
        if (this.mirrorError == null) {
            return;
        }
        this.mirrorError.setText(message == null ? "" : message);
        this.mirrorError.setVisibility(
                message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onUpdateAction() {
        if (this.checking) {
            return;
        }
        UpdateInfo info = this.pending;
        if (info != null && info.newer && info.hasAsset()) {
            startDownload(info);
        } else {
            checkForUpdate();
        }
    }

    private void checkForUpdate() {
        this.checking = true;
        this.pending = null;
        this.updateButton.setText("检查中…");
        setUpdateStatus("正在连接 GitHub…", this.palette.textMuted);
        UpdateChecker.checkAsync(new UpdateChecker.Callback() {
            @Override
            public void onResult(UpdateInfo info) {
                SettingsActivity.this.checking = false;
                SettingsActivity.this.updateButton.setText("检查更新");
                if (info.error != null) {
                    SettingsActivity.this.setUpdateStatus(info.error, SettingsActivity.this.palette.danger);
                    return;
                }
                if (!info.newer) {
                    SettingsActivity.this.setUpdateStatus("已是最新版本（"
                            + BuildConfig.VERSION_NAME + "）", SettingsActivity.this.palette.success);
                    return;
                }
                SettingsActivity.this.pending = info;
                if (!info.hasAsset()) {
                    SettingsActivity.this.setUpdateStatus("发现新版本 " + info.versionName
                            + "，但发布里没有安装包", SettingsActivity.this.palette.warning);
                    return;
                }
                SettingsActivity.this.updateButton.setText("下载并安装 " + info.versionName);
                SettingsActivity.this.setUpdateStatus("发现新版本 " + info.versionName
                        + "，点击上方按钮下载", SettingsActivity.this.palette.warning);
            }
        });
    }

    private void startDownload(final UpdateInfo info) {
        this.checking = true;
        this.updateButton.setEnabled(false);
        this.updateButton.setText("下载中…");
        UpdateInstaller.download(this, info, new UpdateInstaller.Progress() {
            @Override
            public void onProgress(final long downloaded, final long total) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SettingsActivity.this.updateButton.setText("下载中 " + percent(downloaded, total));
                        setUpdateStatus("正在下载 " + formatSize(downloaded)
                                + (total > 0 ? " / " + formatSize(total) : ""), SettingsActivity.this.palette.textMuted);
                    }
                });
            }
        }, new UpdateInstaller.Route() {
            @Override
            public void onRoute(final String label, final int index, final int total) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUpdateStatus("正在尝试线路 " + index + "/" + total + "：" + label,
                                SettingsActivity.this.palette.textMuted);
                    }
                });
            }

            @Override
            public void onRouteFailed(final String label, final String reason) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUpdateStatus(label + " 不可用（" + reason + "），换下一条线路",
                                SettingsActivity.this.palette.warning);
                    }
                });
            }
        }, new UpdateInstaller.Callback() {
            @Override
            public void onDone(File apk) {
                SettingsActivity.this.checking = false;
                SettingsActivity.this.probing = false;
                SettingsActivity.this.updateButton.setEnabled(true);
                SettingsActivity.this.updateButton.setText("下载并安装 " + info.versionName);
                SettingsActivity.this.pendingApk = apk;
                renderMirrorSpeed();
                SettingsActivity.this.tryInstall(apk);
            }

            @Override
            public void onError(String message) {
                SettingsActivity.this.checking = false;
                SettingsActivity.this.probing = false;
                SettingsActivity.this.updateButton.setEnabled(true);
                SettingsActivity.this.updateButton.setText("重新下载 " + info.versionName);
                renderMirrorSpeed();
                setUpdateStatus(message, SettingsActivity.this.palette.danger);
            }
        });
    }

    /** 有权限就唤起安装器；没有则记下待装包，跳系统设置等用户回来。 */
    private void tryInstall(File apk) {
        if (UpdateInstaller.needsUnknownSourcesPermission(this)) {
            this.awaitingInstallPermission = true;
            setUpdateStatus("请先允许本应用安装应用，返回后会自动继续", this.palette.warning);
            UpdateInstaller.openUnknownSourcesSettings(this);
            return;
        }
        this.awaitingInstallPermission = false;
        this.pendingApk = null;
        setUpdateStatus("下载完成，正在打开安装器…", this.palette.success);
        UpdateInstaller.install(this, apk);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!this.awaitingInstallPermission || this.pendingApk == null) {
            return;
        }
        if (UpdateInstaller.needsUnknownSourcesPermission(this)) {
            return;
        }
        File apk = this.pendingApk;
        this.pendingApk = null;
        tryInstall(apk);
    }

    private void showReleaseNotes() {
        UpdateInfo info = this.pending;
        if (info == null || info.notes == null || info.notes.trim().isEmpty()) {
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("更新说明 " + info.versionName)
                .setMessage(info.notes.trim())
                .setPositiveButton("关闭", null)
                .show();
    }

    private void setUpdateStatus(String text, int color) {
        if (this.updateStatus == null) {
            return;
        }
        this.updateStatus.setText(text);
        this.updateStatus.setTextColor(color);
        this.updateStatus.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static String percent(long downloaded, long total) {
        if (total <= 0) {
            return formatSize(downloaded);
        }
        int pct = (int) (downloaded * 100 / total);
        if (pct > 100) {
            pct = 100;
        }
        return pct + "%";
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(java.util.Locale.US, "%.0f KB", kb);
        }
        return String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0);
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
