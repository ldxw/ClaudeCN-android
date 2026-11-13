package com.host1syan.claudecn.cache;

import java.util.Locale;

public class CachePolicy {
    private static final String[] NEVER_CACHE = {"/api/", "/ws/", "/proxy/", "/health", "/webdav/"};
    private static final String[] DYNAMIC_EXT = {".php", ".asp", ".jsp", ".cgi"};
    private static final String[] STATIC_EXT = {".js", ".mjs", ".css", ".woff", ".woff2", ".ttf", ".otf", ".eot", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico", ".bmp", ".wasm", ".map", ".json"};

    public enum Verdict {
        PASS,
        CACHEABLE_STATIC,
        NEEDS_AUTH
    }

    public static CachePolicy.Verdict judge(String str) {
        if (str == null || str.isEmpty()) {
            return CachePolicy.Verdict.PASS;
        }
        String lowerCase = stripQuery(str).toLowerCase(Locale.ROOT);
        for (String str2 : NEVER_CACHE) {
            if (lowerCase.contains(str2) || str.toLowerCase(Locale.ROOT).contains(str2)) {
                return lowerCase.contains("/api/") ? CachePolicy.Verdict.NEEDS_AUTH : CachePolicy.Verdict.PASS;
            }
        }
        for (String str3 : DYNAMIC_EXT) {
            if (lowerCase.endsWith(str3)) {
                return CachePolicy.Verdict.PASS;
            }
        }
        if (lowerCase.endsWith("/") || lowerCase.endsWith(".html") || lowerCase.endsWith(".htm")) {
            return CachePolicy.Verdict.PASS;
        }
        for (String str4 : STATIC_EXT) {
            if (lowerCase.endsWith(str4)) {
                return CachePolicy.Verdict.CACHEABLE_STATIC;
            }
        }
        return CachePolicy.Verdict.PASS;
    }

    public static boolean isEntryHtml(String str, String str2) {
        if (str == null || str2 == null) {
            return false;
        }
        String stripQuery = stripQuery(str);
        String trimTrailingSlash = trimTrailingSlash(str2);
        if (!trimTrailingSlash(stripQuery).equals(trimTrailingSlash)) {
            if (!stripQuery.equals(trimTrailingSlash + "/index.html")) {
                return false;
            }
        }
        return true;
    }

    private static String trimTrailingSlash(String str) {
        int length = str.length();
        while (length > 0 && str.charAt(length - 1) == '/') {
            length--;
        }
        return str.substring(0, length);
    }

    public static String mimeFor(String str) {
        String lowerCase = stripQuery(str).toLowerCase(Locale.ROOT);
        return (lowerCase.endsWith(".js") || lowerCase.endsWith(".mjs")) ? "application/javascript" : lowerCase.endsWith(".css") ? "text/css" : (lowerCase.endsWith(".json") || lowerCase.endsWith(".map")) ? "application/json" : lowerCase.endsWith(".wasm") ? "application/wasm" : lowerCase.endsWith(".woff") ? "font/woff" : lowerCase.endsWith(".woff2") ? "font/woff2" : lowerCase.endsWith(".ttf") ? "font/ttf" : lowerCase.endsWith(".otf") ? "font/otf" : lowerCase.endsWith(".eot") ? "application/vnd.ms-fontobject" : lowerCase.endsWith(".svg") ? "image/svg+xml" : lowerCase.endsWith(".png") ? "image/png" : (lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg")) ? "image/jpeg" : lowerCase.endsWith(".gif") ? "image/gif" : lowerCase.endsWith(".webp") ? "image/webp" : lowerCase.endsWith(".ico") ? "image/x-icon" : lowerCase.endsWith(".bmp") ? "image/bmp" : (lowerCase.endsWith(".html") || lowerCase.endsWith(".htm")) ? "text/html" : "application/octet-stream";
    }

    private static String stripQuery(String str) {
        int indexOf = str.indexOf(63);
        int indexOf2 = str.indexOf(35);
        int length = str.length();
        if (indexOf >= 0) {
            length = Math.min(length, indexOf);
        }
        if (indexOf2 >= 0) {
            length = Math.min(length, indexOf2);
        }
        return str.substring(0, length);
    }
}
