package com.host1syan.claudecn.update;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** 下载更新包并在下载完成后唤起系统安装器。 */
public final class UpdateInstaller {
    private static final String TAG = "claudecn-update";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    /** 兜底上限：100 MB，避免异常地址把缓存写满。 */
    private static final long MAX_BYTES = 100L * 1024 * 1024;

    public interface Progress {
        /** 已下载字节；total 为 -1 表示服务端没给长度。 */
        void onProgress(long downloaded, long total);
    }

    /** 线路切换通知；回调发生在下载线程，调用方自行切主线程。 */
    public interface Route {
        /** 开始尝试某条线路；index 从 1 开始，total 是候选总数。 */
        void onRoute(String label, int index, int total);

        /** 这条线路不可用，正准备换下一条。 */
        void onRouteFailed(String label, String reason);
    }

    public interface Callback {
        void onDone(File apk);

        void onError(String message);
    }

    private UpdateInstaller() {
    }

    /** 下载更新包；结果回主线程，安装由调用方决定。 */
    public static void download(Context context, final UpdateInfo info,
                                final Progress progress, final Callback callback) {
        download(context, info, progress, null, callback);
    }

    /** 下载更新包，按候选线路依次重试；全部失败才回调 onError。 */
    public static void download(Context context, final UpdateInfo info,
                                final Progress progress, final Route route,
                                final Callback callback) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final File dir = ApkProvider.updatesDir(context);
        final String assetName = info.assetName == null ? "update.apk" : info.assetName;
        final File target = new File(dir, safeFileName(assetName));
        final File part = new File(dir, target.getName() + ".part");
        final String downloadUrl = info.downloadUrl;
        final String digest = info.digest;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 候选线路要现测速，必须留在这条线程里，否则会卡住调用方主线程
                List<String> bases = candidateBases(context, downloadUrl);
                String error = null;
                File result = null;
                String lastError = null;
                for (int i = 0; i < bases.size(); i++) {
                    String base = bases.get(i);
                    String label = MirrorSelector.label(base);
                    if (route != null) {
                        route.onRoute(label, i + 1, bases.size());
                    }
                    try {
                        result = download(resolveDownloadUrl(base, downloadUrl),
                                digest, part, target, progress);
                    } catch (Exception e) {
                        result = null;
                        lastError = message(e);
                    }
                    if (result != null) {
                        break;
                    }
                    if (lastError == null) {
                        lastError = "下载未完成";
                    }
                    if (route != null) {
                        route.onRouteFailed(label, lastError);
                    }
                }
                if (result == null) {
                    error = lastError == null ? "下载未完成" : lastError;
                }
                final String finalError = error;
                final File finalResult = result;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalResult != null) {
                            callback.onDone(finalResult);
                        } else {
                            callback.onError(finalError);
                        }
                    }
                });
            }
        }, "update-download");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 候选前缀顺序：手动模式只认用户选定的前缀（末尾补官方直连兜底）；
     * 自动模式按上次测速的缓存顺序（末尾已含官方直连），全部失败也会落到官方直连。
     * 自动模式下用户自填的前缀同样参与测速，不要求切成手动模式才能用。
     */
    static List<String> candidateBases(Context context, String url) {
        List<String> bases = new ArrayList<String>();
        if (UpdateSettings.MODE_MANUAL.equals(UpdateSettings.mirrorMode(context))) {
            String custom = UpdateSettings.mirrorBase(context);
            bases.add(custom);
            if (!custom.isEmpty()) {
                bases.add("");
            }
            return bases;
        }
        List<String> ranked = MirrorSelector.cachedRank(
                context, url, UpdateSettings.mirrorBase(context));
        for (String base : ranked) {
            if (!bases.contains(base)) {
                bases.add(base);
            }
        }
        if (!bases.contains("")) {
            bases.add("");
        }
        return bases;
    }

    /**
     * 把原始 GitHub 下载地址套到加速前缀上：base 为空表示官方直连，否则按
     * 「前缀 + 原始地址」拼装。拼装结果必须仍是 https，否则视为线路非法。
     */
    static String resolveDownloadUrl(String base, String url) throws Exception {
        if (url == null || url.isEmpty()) {
            throw new Exception("缺少下载地址");
        }
        String prefix = base == null ? "" : base.trim();
        if (prefix.isEmpty()) {
            return url;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String resolved = prefix + url;
        if (!resolved.startsWith("https://")) {
            throw new Exception("加速地址无效，请检查是否以 https:// 开头");
        }
        return resolved;
    }

    /** 下载到 part，校验后改名为 target；失败抛异常，返回 null 表示大小不符。 */
    private static File download(String url, String digest, File part, File target,
                                 Progress progress) throws Exception {
        if (url == null || url.isEmpty()) {
            throw new Exception("缺少下载地址");
        }
        if (!url.startsWith("https://")) {
            throw new Exception("下载地址不是 https");
        }
        // 复用手上已有的完整文件，避免重复下载
        if (target.exists() && target.length() > 0 && matches(target, digest)) {
            if (progress != null) {
                progress.onProgress(target.length(), target.length());
            }
            return target;
        }
        deleteQuietly(part);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("User-Agent", "ClaudeCN-Android");
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new Exception("下载失败，HTTP " + code);
            }
            long total = Build.VERSION.SDK_INT >= 24
                    ? conn.getContentLengthLong()
                    : conn.getContentLength();
            if (total > MAX_BYTES) {
                throw new Exception("安装包过大：" + total + " 字节");
            }

            MessageDigest md = digest == null ? null : MessageDigest.getInstance("SHA-256");
            long written = 0;
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(part);
            try {
                byte[] buf = new byte[32768];
                while (true) {
                    int read = in.read(buf);
                    if (read <= 0) {
                        break;
                    }
                    written += read;
                    if (written > MAX_BYTES) {
                        throw new Exception("安装包超出大小上限");
                    }
                    out.write(buf, 0, read);
                    if (md != null) {
                        md.update(buf, 0, read);
                    }
                    if (progress != null) {
                        progress.onProgress(written, total);
                    }
                }
                out.flush();
            } finally {
                out.close();
                in.close();
            }

            if (total > 0 && written != total) {
                throw new Exception("下载不完整：期望 " + total + " 字节，实际 " + written);
            }
            if (md != null) {
                String actual = hex(md.digest());
                if (!actual.equalsIgnoreCase(digest)) {
                    throw new Exception("安装包校验失败，文件可能被篡改");
                }
            }
            if (!part.renameTo(target)) {
                // 目标已存在时 renameTo 在部分文件系统上会失败，先删后重试
                deleteQuietly(target);
                if (!part.renameTo(target)) {
                    throw new Exception("无法保存安装包");
                }
            }
            return target;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            deleteQuietly(part);
        }
    }

    private static boolean matches(File file, String digest) {
        if (digest == null || digest.isEmpty()) {
            return true;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            InputStream in = new java.io.FileInputStream(file);
            try {
                byte[] buf = new byte[32768];
                int read;
                while ((read = in.read(buf)) > 0) {
                    md.update(buf, 0, read);
                }
            } finally {
                in.close();
            }
            return hex(md.digest()).equalsIgnoreCase(digest);
        } catch (Exception e) {
            return false;
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static String safeFileName(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') || c == '.' || c == '-' || c == '_' || c == '+';
            if (ok) {
                sb.append(c);
            }
        }
        String value = sb.toString();
        if (value.isEmpty()) {
            return "update.apk";
        }
        return value.endsWith(".apk") ? value : value + ".apk";
    }

    /** 打开系统安装界面；API 26+ 还需用户先允许本应用安装未知来源。 */
    public static void install(Activity activity, File apk) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            openUnknownSourcesSettings(activity);
            return;
        }
        Uri uri = ApkProvider.uriFor(apk.getName());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.d(TAG, "唤起安装器失败: " + message(e));
            openUnknownSourcesSettings(activity);
        }
    }

    /** canRequestPackageInstalls 需要 API 26，低版本直接跳过。 */
    public static boolean needsUnknownSourcesPermission(Activity activity) {
        return Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls();
    }

    public static void openUnknownSourcesSettings(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.d(TAG, "打开未知来源设置失败: " + message(e));
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static String message(Exception e) {
        String m = e.getMessage();
        return m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
    }
}
