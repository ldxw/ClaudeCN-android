package com.host1syan.claudecn.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.host1syan.claudecn.R;
import com.host1syan.claudecn.cache.AssetCache;
import com.host1syan.claudecn.cache.CnWebViewClient;
import com.host1syan.claudecn.model.ServerConfig;
import com.host1syan.claudecn.store.ServerStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements StatusBridge.Listener {
    private static final int MAX_LIVE_TABS = 6;
    private static final int TAB_BAR_HEIGHT_DP = 56;
    private FrameLayout content;
    private String currentId;
    private boolean darkAtCreate;
    private View emptyState;
    private ExecutorService io;
    private ConnectivityManager.NetworkCallback networkCallback;
    private Palette palette;
    private ServerStore store;
    private LinearLayout tabBar;
    private final List<ServerConfig> servers = new ArrayList();
    private final Map<String, WebView> webViews = new HashMap();
    private final Map<String, CnWebViewClient> clients = new HashMap();
    private final Map<String, StatusBridge> bridges = new HashMap();
    private final Map<String, Integer> states = new HashMap();

    @Override
    protected void onCreate(Bundle bundle) {
        Theme.apply(this);
        super.onCreate(bundle);
        this.palette = Theme.palette(this);
        this.darkAtCreate = Theme.isDark(this);
        this.store = new ServerStore(this);
        this.io = Executors.newFixedThreadPool(3);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(this.palette.bg);
        FrameLayout frameLayout = new FrameLayout(this);
        this.content = frameLayout;
        linearLayout.addView(frameLayout, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        View buildEmptyState = buildEmptyState();
        this.emptyState = buildEmptyState;
        this.content.addView(buildEmptyState, new FrameLayout.LayoutParams(-1, -1));
        linearLayout.addView(buildTabBar(), new LinearLayout.LayoutParams(-1, Ui.dp(this, 56.0f)));
        setContentView(linearLayout);
        registerNetworkCallback();
    }

    private View buildEmptyState() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(17);
        int dp = Ui.dp(this, 32.0f);
        linearLayout.setPadding(dp, 0, dp, Ui.dp(this, 48.0f));
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.ic_logo);
        int dp2 = Ui.dp(this, 96.0f);
        linearLayout.addView(imageView, new LinearLayout.LayoutParams(dp2, dp2));
        Ui.addGap(linearLayout, this, 20);
        TextView title = Ui.title(this, "还没有服务器", this.palette);
        title.setGravity(17);
        linearLayout.addView(title);
        Ui.addGap(linearLayout, this, 8);
        TextView body = Ui.body(this, "添加一台 claudecn 服务器，\n之后可以在底部标签之间随时切换。", this.palette);
        body.setGravity(17);
        body.setTextColor(this.palette.textMuted);
        linearLayout.addView(body);
        Ui.addGap(linearLayout, this, 24);
        Button primaryButton = Ui.primaryButton(this, "添加服务器", this.palette);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.openEditor(null);
            }
        });
        LinearLayout.LayoutParams wrapWrap = Ui.wrapWrap();
        wrapWrap.gravity = 1;
        linearLayout.addView(primaryButton, wrapWrap);
        return linearLayout;
    }

    private View buildTabBar() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(this.palette.tabBarBg);
        View view = new View(this);
        view.setBackgroundColor(this.palette.divider);
        linearLayout.addView(view, new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dp(this, 0.5f))));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout2.setGravity(16);
        int dp = Ui.dp(this, 8.0f);
        linearLayout2.setPadding(dp, 0, dp, 0);
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        horizontalScrollView.setFillViewport(false);
        horizontalScrollView.setClipToPadding(false);
        horizontalScrollView.setPadding(dp, 0, dp, 0);
        LinearLayout linearLayout3 = new LinearLayout(this);
        this.tabBar = linearLayout3;
        linearLayout3.setOrientation(LinearLayout.HORIZONTAL);
        this.tabBar.setGravity(16);
        horizontalScrollView.addView(this.tabBar, new FrameLayout.LayoutParams(-2, Ui.dp(this, 55.0f)));
        linearLayout2.addView(horizontalScrollView, new LinearLayout.LayoutParams(0, -1, 1.0f));
        Palette palette = this.palette;
        TextView refresh = Ui.iconButton(this, "⟳", palette, palette.textSecondary);
        refresh.setContentDescription("刷新当前页面");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                view2.animate().rotationBy(360.0f).setDuration(400L).start();
                MainActivity.this.refreshCurrent(false);
            }
        });
        refresh.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view2) {
                view2.animate().rotationBy(360.0f).setDuration(400L).start();
                MainActivity.this.refreshCurrent(true);
                return true;
            }
        });
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(Ui.dp(this, 42.0f), Ui.dp(this, 42.0f));
        refreshParams.setMarginEnd(Ui.dp(this, 4.0f));
        linearLayout2.addView(refresh, refreshParams);
        TextView add = Ui.iconButton(this, "＋", palette, palette.accent);
        add.setContentDescription("添加服务器");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                MainActivity.this.openEditor(null);
            }
        });
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(Ui.dp(this, 42.0f), Ui.dp(this, 42.0f));
        addParams.setMarginEnd(Ui.dp(this, 4.0f));
        linearLayout2.addView(add, addParams);
        TextView settings = Ui.iconButton(this, "⚙", palette, palette.textSecondary);
        settings.setContentDescription("设置");
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                MainActivity.this.startActivity(new Intent(MainActivity.this, (Class<?>) SettingsActivity.class));
            }
        });
        linearLayout2.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 42.0f), Ui.dp(this, 42.0f)));
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        return linearLayout;
    }

    private void refreshCurrent(boolean z) {
        String str = this.currentId;
        if (str == null) {
            return;
        }
        WebView webView = this.webViews.get(str);
        if (webView == null) {
            showTab(str);
            return;
        }
        CnWebViewClient cnWebViewClient = this.clients.get(str);
        if (cnWebViewClient != null) {
            cnWebViewClient.invalidateEntryHtml();
        }
        if (z) {
            new AssetCache(this, str).clear();
            Toast.makeText(this, "已清空该服务器缓存，正在重新加载", Toast.LENGTH_SHORT).show();
        }
        StatusBridge statusBridge = this.bridges.get(str);
        if (statusBridge != null) {
            statusBridge.reset();
        }
        onStatusChanged(str, 0);
        webView.reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Theme.isDark(this) != this.darkAtCreate) {
            recreate();
            return;
        }
        reloadServers();
        Iterator<WebView> it = this.webViews.values().iterator();
        while (it.hasNext()) {
            it.next().onResume();
        }
        healConnections();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void healConnections() {
        Iterator<WebView> it = this.webViews.values().iterator();
        while (it.hasNext()) {
            it.next().evaluateJavascript("(function(){try{if(window.__cnForceReconnect)window.__cnForceReconnect();}catch(e){}})()", null);
        }
    }

    private void registerNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }
        NetworkRequest build = new NetworkRequest.Builder().build();
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.healConnections();
                    }
                });
            }
        };
        this.networkCallback = networkCallback;
        try {
            connectivityManager.registerNetworkCallback(build, networkCallback);
        } catch (Exception unused) {
        }
    }

    private void reloadServers() {
        List<ServerConfig> list = this.store.list();
        ArrayList<String> arrayList = new ArrayList();
        for (String str : this.webViews.keySet()) {
            Iterator<ServerConfig> it = list.iterator();
            while (true) {
                if (it.hasNext()) {
                    if (it.next().id.equals(str)) {
                        break;
                    }
                } else {
                    arrayList.add(str);
                    break;
                }
            }
        }
        for (String str2 : arrayList) {
            destroyWebView(str2);
            this.states.remove(str2);
        }
        Iterator<ServerConfig> it2 = list.iterator();
        while (true) {
            ServerConfig serverConfig = null;
            if (!it2.hasNext()) {
                break;
            }
            ServerConfig next = it2.next();
            if (this.clients.get(next.id) != null) {
                Iterator<ServerConfig> it3 = this.servers.iterator();
                while (true) {
                    if (!it3.hasNext()) {
                        break;
                    }
                    ServerConfig next2 = it3.next();
                    if (next2.id.equals(next.id)) {
                        serverConfig = next2;
                        break;
                    }
                }
                if (serverConfig != null && (!serverConfig.url.equals(next.url) || !eq(serverConfig.token, next.token))) {
                    destroyWebView(next.id);
                    this.states.remove(next.id);
                }
            }
        }
        this.servers.clear();
        this.servers.addAll(list);
        buildTabs();
        if (this.servers.isEmpty()) {
            this.currentId = null;
            this.emptyState.setVisibility(View.VISIBLE);
            return;
        }
        this.emptyState.setVisibility(View.GONE);
        Iterator<ServerConfig> it4 = this.servers.iterator();
        while (true) {
            if (it4.hasNext()) {
                if (it4.next().id.equals(this.currentId)) {
                    break;
                }
            } else {
                this.currentId = this.servers.get(0).id;
                break;
            }
        }
        showTab(this.currentId);
    }

    private void destroyWebView(String str) {
        WebView remove = this.webViews.remove(str);
        if (remove != null) {
            this.content.removeView(remove);
            remove.destroy();
        }
        this.clients.remove(str);
        this.bridges.remove(str);
    }

    private void buildTabs() {
        this.tabBar.removeAllViews();
        for (final ServerConfig serverConfig : this.servers) {
            TabView tabView = new TabView(this, serverConfig.id, serverConfig.displayName(), this.palette);
            Integer num = this.states.get(serverConfig.id);
            tabView.setState(num == null ? 0 : num.intValue());
            tabView.setActive(serverConfig.id.equals(this.currentId));
            tabView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.showTab(serverConfig.id);
                }
            });
            tabView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    MainActivity.this.openEditor(serverConfig);
                    return true;
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, Ui.dp(this, 40.0f));
            layoutParams.setMarginEnd(Ui.dp(this, 4.0f));
            this.tabBar.addView(tabView, layoutParams);
        }
        tintTabs();
    }

    private void tintTabs() {
        int min = Math.min(this.servers.size(), this.tabBar.getChildCount());
        for (int i = 0; i < min; i++) {
            View childAt = this.tabBar.getChildAt(i);
            if (childAt instanceof TabView) {
                ((TabView) childAt).setActive(this.servers.get(i).id.equals(this.currentId));
            }
        }
    }

    public void showTab(String str) {
        String str2;
        this.currentId = str;
        Iterator<Map.Entry<String, WebView>> it = this.webViews.entrySet().iterator();
        while (true) {
            int i = View.VISIBLE;
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, WebView> next = it.next();
            WebView value = next.getValue();
            if (!next.getKey().equals(str)) {
                i = View.GONE;
            }
            value.setVisibility(i);
        }
        WebView webView = this.webViews.get(str);
        if (webView == null) {
            ServerConfig findById = findById(str);
            if (findById == null) {
                return;
            }
            if (this.webViews.size() >= MAX_LIVE_TABS) {
                Iterator<String> it2 = this.webViews.keySet().iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        str2 = null;
                        break;
                    } else {
                        str2 = it2.next();
                        if (!str2.equals(str)) {
                            break;
                        }
                    }
                }
                if (str2 != null) {
                    destroyWebView(str2);
                }
            }
            webView = createWebView(findById);
            this.webViews.put(str, webView);
            this.content.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        }
        webView.setVisibility(View.VISIBLE);
        tintTabs();
    }

    private WebView createWebView(final ServerConfig serverConfig) {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 26) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, false);
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().setAcceptCookie(true);
        final StatusBridge statusBridge = new StatusBridge(serverConfig.id, this);
        this.bridges.put(serverConfig.id, statusBridge);
        webView.addJavascriptInterface(statusBridge, "ClaudecnHost");
        CnWebViewClient cnWebViewClient = new CnWebViewClient(serverConfig, new AssetCache(this, serverConfig.id), this.io, new Runnable() {
            @Override
            public void run() {
                statusBridge.reset();
                MainActivity.this.onStatusChanged(serverConfig.id, 0);
            }
        });
        this.clients.put(serverConfig.id, cnWebViewClient);
        webView.setWebViewClient(cnWebViewClient);
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(serverConfig.url);
        return webView;
    }

    @Override
    public void onStatusChanged(String str, int i) {
        Integer num = this.states.get(str);
        if (num == null || num.intValue() != i) {
            this.states.put(str, Integer.valueOf(i));
            for (int i2 = 0; i2 < this.tabBar.getChildCount(); i2++) {
                View childAt = this.tabBar.getChildAt(i2);
                if (childAt instanceof TabView) {
                    TabView tabView = (TabView) childAt;
                    if (tabView.serverId.equals(str)) {
                        tabView.setState(i);
                        return;
                    }
                }
            }
        }
    }

    public void openEditor(ServerConfig serverConfig) {
        Intent intent = new Intent(this, (Class<?>) ServerEditActivity.class);
        if (serverConfig != null) {
            intent.putExtra("id", serverConfig.id);
        }
        startActivity(intent);
    }

    private ServerConfig findById(String str) {
        for (ServerConfig serverConfig : this.servers) {
            if (serverConfig.id.equals(str)) {
                return serverConfig;
            }
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        String str = this.currentId;
        WebView webView = str == null ? null : this.webViews.get(str);
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (this.networkCallback != null) {
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    connectivityManager.unregisterNetworkCallback(this.networkCallback);
                }
            } catch (Exception unused) {
            }
            this.networkCallback = null;
        }
        for (WebView webView : this.webViews.values()) {
            this.content.removeView(webView);
            webView.destroy();
        }
        this.webViews.clear();
        this.clients.clear();
        this.bridges.clear();
        this.io.shutdown();
        super.onDestroy();
    }

    private static boolean eq(String str, String str2) {
        if (str == null) {
            return str2 == null;
        }
        return str.equals(str2);
    }
}
