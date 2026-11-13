package com.host1syan.claudecn.cache;

import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.host1syan.claudecn.model.ServerConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public class CnWebViewClient extends WebViewClient {
    private static final long MAX_HTML_BYTES = 4194304;
    private static final String TAG = "claudecn-web";
    private final AssetCache cache;
    private final File entryFile;
    private final ExecutorService io;
    private final Runnable onPageStarted;
    private final ServerConfig server;

    public CnWebViewClient(ServerConfig serverConfig, AssetCache assetCache, ExecutorService executorService, Runnable runnable) {
        this.server = serverConfig;
        this.cache = assetCache;
        this.io = executorService;
        this.onPageStarted = runnable;
        this.entryFile = new File(assetCache.dir(), "entry.html");
    }

    @Override
    public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
        Runnable runnable = this.onPageStarted;
        if (runnable != null) {
            runnable.run();
        }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
        if (!"GET".equalsIgnoreCase(webResourceRequest.getMethod())) {
            return null;
        }
        final String uri = webResourceRequest.getUrl().toString();
        if (CachePolicy.isEntryHtml(uri, this.server.url)) {
            return serveEntryHtml(uri);
        }
        if (CachePolicy.judge(uri) != CachePolicy.Verdict.CACHEABLE_STATIC) {
            return null;
        }
        File fileFor = this.cache.fileFor(uri);
        if (fileFor != null && fileFor.exists() && fileFor.length() > 0) {
            try {
                return response(CachePolicy.mimeFor(uri), new FileInputStream(fileFor));
            } catch (Exception unused) {
                return null;
            }
        }
        this.io.execute(new Runnable() {
            @Override
            public void run() {
                CnWebViewClient.this.cache.fetchInto(uri);
            }
        });
        return null;
    }

    private WebResourceResponse serveEntryHtml(final String str) {
        final byte[] readAll = this.cache.readAll(this.entryFile, MAX_HTML_BYTES);
        if (readAll != null && readAll.length > 0) {
            this.io.execute(new Runnable() {
                @Override
                public void run() {
                    CnWebViewClient.this.revalidateEntryHtml(str, readAll);
                }
            });
            return htmlResponse(readAll);
        }
        byte[] downloadHtml = downloadHtml(str);
        if (downloadHtml == null || downloadHtml.length <= 0) {
            return null;
        }
        writeFile(this.entryFile, downloadHtml);
        return htmlResponse(downloadHtml);
    }

    public void revalidateEntryHtml(String str, byte[] bArr) {
        byte[] downloadHtml = downloadHtml(str);
        if (downloadHtml == null || downloadHtml.length == 0 || Arrays.equals(downloadHtml, bArr)) {
            return;
        }
        Log.d(TAG, "入口 HTML 已更新，替换缓存");
        writeFile(this.entryFile, downloadHtml);
    }

    private WebResourceResponse htmlResponse(byte[] bArr) {
        String inject = Injector.inject(new String(bArr), this.server);
        HashMap hashMap = new HashMap();
        hashMap.put("Cache-Control", "no-store");
        return new WebResourceResponse("text/html", "utf-8", 200, "OK", hashMap, new ByteArrayInputStream(inject.getBytes()));
    }

    private WebResourceResponse response(String str, InputStream inputStream) {
        HashMap hashMap = new HashMap();
        hashMap.put("Cache-Control", "no-store");
        return new WebResourceResponse(str, null, 200, "OK", hashMap, inputStream);
    }

    private byte[] downloadHtml(String str) {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(20000);
            httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
            httpURLConnection.setRequestProperty("Accept", "text/html,*/*");
            if (this.server.token != null && !this.server.token.isEmpty()) {
                httpURLConnection.setRequestProperty("Authorization", "Bearer " + this.server.token);
            }
            if (httpURLConnection.getResponseCode() != 200) {
                return null;
            }
            inputStream = httpURLConnection.getInputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(98304);
            byte[] bArr = new byte[16384];
            long j = 0;
            while (true) {
                int read = inputStream.read(bArr);
                if (read <= 0) {
                    break;
                }
                j += read;
                if (j > MAX_HTML_BYTES) {
                    return null;
                }
                byteArrayOutputStream.write(bArr, 0, read);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            Log.d(TAG, "入口 HTML 抓取失败: " + e.getMessage());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception unused) {
                }
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    public void invalidateEntryHtml() {
        if (this.entryFile.exists()) {
            this.entryFile.delete();
        }
    }

    private static void writeFile(File file, byte[] bArr) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bArr);
        } catch (Exception e) {
            Log.d(TAG, "写盘失败 " + file.getName() + ": " + e.getMessage());
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception unused) {
                }
            }
        }
    }
}
