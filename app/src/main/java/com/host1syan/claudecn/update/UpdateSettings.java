package com.host1syan.claudecn.update;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/** 更新相关的用户偏好：自动检查、检查节流、已提醒版本、下载加速地址。 */
public final class UpdateSettings {
    /** 与 Theme 共用同一个偏好文件，不再单开一个。 */
    private static final String PREF = "claudecn_ui";
    private static final String KEY_AUTO_CHECK = "update_auto_check";
    private static final String KEY_LAST_CHECK = "update_last_check";
    private static final String KEY_NOTIFIED = "update_notified";
    private static final String KEY_MIRROR = "update_mirror_base";
    private static final String KEY_MODE = "update_mirror_mode";
    private static final String KEY_RANK = "update_rank_cache";

    /** 两次自动检查之间的最小间隔。 */
    public static final long INTERVAL_MS = 24L * 60L * 60L * 1000L;

    /** 测速结果的缓存时长：6 小时内复用，避免每次下载都重新探测。 */
    public static final long RANK_TTL = 6L * 60L * 60L * 1000L;

    /** 自动在候选镜像里选最快的可用线路，失败时回退官方直连。 */
    public static final String MODE_AUTO = "auto";
    /** 固定使用用户选定的前缀。 */
    public static final String MODE_MANUAL = "manual";

    private UpdateSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF, 0);
    }

    public static boolean autoCheck(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_CHECK, true);
    }

    public static void setAutoCheck(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_AUTO_CHECK, value).apply();
    }

    public static long lastCheckAt(Context context) {
        return prefs(context).getLong(KEY_LAST_CHECK, 0L);
    }

    /** 只在查询成功时调用，离线失败下次启动会重试。 */
    public static void markChecked(Context context, long now) {
        prefs(context).edit().putLong(KEY_LAST_CHECK, now).apply();
    }

    /** 是否到了该自动检查的时机；系统时间被回拨时也放行一次。 */
    public static boolean shouldCheck(Context context, long now) {
        if (!autoCheck(context)) {
            return false;
        }
        long last = lastCheckAt(context);
        if (last <= 0L || now < last) {
            return true;
        }
        return now - last >= INTERVAL_MS;
    }

    /** 已经为哪个版本弹过提示，避免同一版本反复打扰。 */
    public static String notifiedVersion(Context context) {
        return prefs(context).getString(KEY_NOTIFIED, "");
    }

    public static boolean isNotified(Context context, String versionName) {
        return versionName != null && !versionName.isEmpty()
                && versionName.equals(notifiedVersion(context));
    }

    public static void markNotified(Context context, String versionName) {
        prefs(context).edit().putString(KEY_NOTIFIED, versionName == null ? "" : versionName).apply();
    }

    /** 当前加速前缀；空串表示官方直连。 */
    public static String mirrorBase(Context context) {
        String value = prefs(context).getString(KEY_MIRROR, "");
        return value == null ? "" : value.trim();
    }

    public static void setMirrorBase(Context context, String base) {
        prefs(context).edit().putString(KEY_MIRROR, base == null ? "" : base.trim()).apply();
    }

    /** 默认走自动选路。 */
    public static String mirrorMode(Context context) {
        String value = prefs(context).getString(KEY_MODE, MODE_AUTO);
        return MODE_MANUAL.equals(value) ? MODE_MANUAL : MODE_AUTO;
    }

    public static void setMirrorMode(Context context, String mode) {
        prefs(context).edit()
                .putString(KEY_MODE, MODE_MANUAL.equals(mode) ? MODE_MANUAL : MODE_AUTO)
                .apply();
    }

    /**
     * 上次测速得到的可用前缀列表，序列化为「时间戳\nbase1\nbase2…」。
     * 超过 TTL 返回 null；时间戳异常（回拨）也返回 null 重新测。
     */
    public static String cachedRank(Context context, long now) {
        String raw = prefs(context).getString(KEY_RANK, "");
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        int sep = raw.indexOf('\n');
        String head = sep < 0 ? raw : raw.substring(0, sep);
        long stamp;
        try {
            stamp = Long.parseLong(head);
        } catch (NumberFormatException e) {
            return null;
        }
        if (stamp <= 0L || now < stamp || now - stamp >= RANK_TTL) {
            return null;
        }
        return sep < 0 ? "" : raw.substring(sep + 1);
    }

    /** 存测速结果；value 为 join 后的前缀列表，可含空串项表示官方直连。 */
    public static void setCachedRank(Context context, String value, long now) {
        String body = value == null ? "" : value;
        prefs(context).edit().putString(KEY_RANK, now + "\n" + body).apply();
    }

    /** 丢掉缓存的测速结果，下次下载重新探测。 */
    public static void clearCachedRank(Context context) {
        prefs(context).edit().remove(KEY_RANK).apply();
    }

    /**
     * 规范化加速前缀：空串表示官方直连；非空时必须是 https 开头，自动补结尾的 /。
     * 非法输入返回 null，调用方负责提示用户。
     */
    public static String normalizeMirrorBase(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return null;
            }
        }
        if (!value.toLowerCase(Locale.US).startsWith("https://")) {
            return null;
        }
        if (value.length() <= "https://".length()) {
            return null;
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }
}
