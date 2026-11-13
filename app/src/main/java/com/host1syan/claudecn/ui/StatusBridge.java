package com.host1syan.claudecn.ui;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

public class StatusBridge {
    private final StatusBridge.Listener listener;
    private final String serverId;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int last = 0;

    public interface Listener {
        void onStatusChanged(String str, int i);
    }

    public void reset() {
        this.last = 0;
    }

    public StatusBridge(String str, StatusBridge.Listener listener) {
        this.serverId = str;
        this.listener = listener;
    }

    @JavascriptInterface
    public void status(int i, int i2) {
        final int i3 = i2 != 0 ? 2 : i != 0 ? 1 : 0;
        if (i3 == this.last) {
            return;
        }
        this.last = i3;
        this.main.post(new Runnable() {
            @Override
            public void run() {
                StatusBridge.this.listener.onStatusChanged(StatusBridge.this.serverId, i3);
            }
        });
    }
}
