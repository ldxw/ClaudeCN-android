package com.host1syan.claudecn.cache;

import java.io.File;
import java.security.MessageDigest;

public class CacheKey {
    private final File root;

    public File dir() {
        return this.root;
    }

    public CacheKey(File file) {
        this.root = file;
    }

    public File fileFor(String str) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(40);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 15, 16));
                sb.append(Character.forDigit(b & 15, 16));
            }
            return new File(this.root, sb.toString());
        } catch (Exception unused) {
            return null;
        }
    }
}
