package com.host1syan.claudecn.update;

import android.os.Handler;
import android.os.Looper;
import com.host1syan.claudecn.BuildConfig;
import com.host1syan.claudecn.NativeAbi;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/** 向 GitHub Releases 查询最新版本，挑出与本机 ABI 匹配的安装包。 */
public final class UpdateChecker {
    private static final String API =
            "https://api.github.com/repos/ldxw/ClaudeCN-android/releases/latest";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 20000;
    private static final int MAX_BODY_BYTES = 1024 * 1024;

    public interface Callback {
        void onResult(UpdateInfo info);
    }

    private UpdateChecker() {
    }

    /** 在后台线程查询，结果回主线程。 */
    public static void checkAsync(final Callback callback) {
        final Handler handler = new Handler(Looper.getMainLooper());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final UpdateInfo info = fetch();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(info);
                    }
                });
            }
        }, "update-check");
        thread.setDaemon(true);
        thread.start();
    }

    /** 同步查询，禁止在主线程调用。 */
    public static UpdateInfo fetch() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(API).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("User-Agent", "ClaudeCN-Android/" + BuildConfig.VERSION_NAME);

            int code = conn.getResponseCode();
            if (code == 403 || code == 429) {
                return UpdateInfo.failure("GitHub 接口访问过于频繁，请稍后再试");
            }
            if (code == 404) {
                return UpdateInfo.failure("仓库里还没有已发布的版本");
            }
            if (code != 200) {
                return UpdateInfo.failure("更新服务返回 " + code);
            }

            String body = readBody(conn.getInputStream());
            if (body == null) {
                return UpdateInfo.failure("更新信息过大或读取失败");
            }
            return parse(body);
        } catch (Exception e) {
            return UpdateInfo.failure("网络错误：" + message(e));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static UpdateInfo parse(String body) {
        try {
            JSONObject root = new JSONObject(body);
            String tag = root.optString("tag_name", "");
            String versionName = stripPrefix(tag);
            if (versionName.isEmpty()) {
                return UpdateInfo.failure("发布信息缺少版本号");
            }
            String notes = root.optString("body", "");
            boolean newer = isNewer(versionName, BuildConfig.VERSION_NAME);

            JSONObject asset = pickAsset(root.optJSONArray("assets"), versionName);
            if (asset == null) {
                return new UpdateInfo(versionName, tag, notes, null, null, null, 0L, newer, null);
            }
            String name = asset.optString("name", "");
            String url = asset.optString("browser_download_url", "");
            String digest = normalizeDigest(asset.optString("digest", ""));
            long size = asset.optLong("size", 0L);
            return new UpdateInfo(versionName, tag, notes, name, url, digest, size, newer, null);
        } catch (Exception e) {
            return UpdateInfo.failure("更新信息格式异常：" + message(e));
        }
    }

    /** 查找 ClaudeCN-<ver>-<abi>.apk，缺失时回落 ClaudeCN-<ver>-universal.apk。 */
    private static JSONObject pickAsset(JSONArray assets, String versionName) {
        if (assets == null || assets.length() == 0) {
            return null;
        }
        String abi = NativeAbi.abi();
        String ver = sanitizeVersion(versionName);

        JSONObject universal = null;
        JSONObject anyApk = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject item = assets.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String name = item.optString("name", "");
            if (!name.endsWith(".apk")) {
                continue;
            }
            if (anyApk == null) {
                anyApk = item;
            }
            if (name.equals("ClaudeCN-" + ver + "-" + abi + ".apk")) {
                return item;
            }
            if (name.equals("ClaudeCN-" + ver + "-universal.apk")) {
                universal = item;
            }
        }
        if (universal != null) {
            return universal;
        }
        return anyApk;
    }

    /** GitHub 的 digest 形如 sha256:abcdef…，只保留十六进制部分。 */
    private static String normalizeDigest(String digest) {
        if (digest == null) {
            return null;
        }
        String value = digest.trim();
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1);
        }
        value = value.trim().toLowerCase(java.util.Locale.US);
        return value.isEmpty() ? null : value;
    }

    private static String stripPrefix(String tag) {
        String value = tag == null ? "" : tag.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        return value;
    }

    /** 只保留版本名里的合法字符，防止拼资产名时出错。 */
    private static String sanitizeVersion(String versionName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < versionName.length(); i++) {
            char c = versionName.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') || c == '.' || c == '-' || c == '+';
            if (ok) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 按 x.y.z 逐段比较。当前版本带 -rc/-beta 等后缀时，同号正式版视为更新。
     * 无法解析的版本按「不比当前新」处理，避免误导用户。
     */
    static boolean isNewer(String remote, String current) {
        int[] a = parseVersion(stripPrefix(remote));
        int[] b = parseVersion(stripPrefix(current));
        if (a == null || b == null) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i];
            }
        }
        // 数字段相同：只有远端是正式版、当前是预发布时才算更新
        boolean remotePre = isPrerelease(stripPrefix(remote));
        boolean currentPre = isPrerelease(stripPrefix(current));
        return !remotePre && currentPre;
    }

    private static boolean isPrerelease(String version) {
        return version != null && version.indexOf('-') >= 0;
    }

    private static int[] parseVersion(String version) {
        if (version == null) {
            return null;
        }
        String value = version.trim();
        int cut = value.indexOf('-');
        if (cut < 0) {
            cut = value.indexOf('+');
        }
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        String[] parts = value.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            return null;
        }
        int[] result = new int[3];
        try {
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].isEmpty()) {
                    return null;
                }
                result[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }

    private static String readBody(InputStream in) throws Exception {
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(16384);
        try {
            byte[] buf = new byte[16384];
            long total = 0;
            while (true) {
                int read = in.read(buf);
                if (read <= 0) {
                    break;
                }
                total += read;
                if (total > MAX_BODY_BYTES) {
                    return null;
                }
                out.write(buf, 0, read);
            }
        } finally {
            in.close();
        }
        return out.toString("UTF-8");
    }

    private static String message(Exception e) {
        String m = e.getMessage();
        return m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
    }
}
