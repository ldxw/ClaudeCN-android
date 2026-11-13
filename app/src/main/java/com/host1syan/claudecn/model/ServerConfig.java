package com.host1syan.claudecn.model;

public class ServerConfig {
    public final String id;
    public String name;
    public String token;
    public String url;

    public ServerConfig(String str, String str2, String str3, String str4) {
        this.id = str;
        this.name = str2;
        this.url = str3;
        this.token = str4;
    }

    public static String normalizeUrl(String str) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        if (trim.isEmpty()) {
            return "";
        }
        if (!trim.startsWith("http://") && !trim.startsWith("https://")) {
            trim = "https://" + trim;
        }
        while (trim.endsWith("/")) {
            trim = trim.substring(0, trim.length() - 1);
        }
        return trim;
    }

    public String displayName() {
        String str = this.name;
        if (str != null && !str.trim().isEmpty()) {
            return this.name.trim();
        }
        String str2 = this.url;
        String replaceFirst = str2 != null ? str2.replaceFirst("^https?://", "") : "";
        return replaceFirst.isEmpty() ? "未命名" : replaceFirst;
    }
}
