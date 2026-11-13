package com.host1syan.claudecn.cache;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AssetCache {
    private static final long MAX_ENTRY_BYTES = 8388608;
    private static final String TAG = "claudecn-cache";
    private final CacheKey key;
    private final File root;

    public File dir() {
        return this.root;
    }

    public AssetCache(Context context, String str) {
        File file = new File(new File(context.getFilesDir(), "assets"), str);
        this.root = file;
        if (!file.exists()) {
            file.mkdirs();
        }
        this.key = new CacheKey(file);
    }

    public File fileFor(String str) {
        return this.key.fileFor(str);
    }

    public boolean has(String str) {
        File fileFor = this.key.fileFor(str);
        return fileFor != null && fileFor.exists() && fileFor.length() > 0;
    }

    public void fetchInto(String str) {
        File fileFor = this.key.fileFor(str);
        if (fileFor == null) {
            return;
        }
        if (fileFor.exists() && fileFor.length() > 0) {
            return;
        }
        File file = new File(this.root, fileFor.getName() + ".part");
        long j = 0;
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(20000);
            httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
            if (httpURLConnection.getResponseCode() != 200) {
                return;
            }
            // getContentLengthLong() 需要 API 24，低版本退回到 32 位的 getContentLength()
            long contentLength = Build.VERSION.SDK_INT >= 24
                    ? httpURLConnection.getContentLengthLong()
                    : httpURLConnection.getContentLength();
            if (contentLength > MAX_ENTRY_BYTES) {
                return;
            }
            InputStream inputStream = httpURLConnection.getInputStream();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                boolean z = false;
                try {
                    byte[] bArr = new byte[16384];
                    while (true) {
                        int read = inputStream.read(bArr);
                        if (read <= 0) {
                            z = true;
                            break;
                        }
                        j += read;
                        if (j > MAX_ENTRY_BYTES) {
                            break;
                        }
                        fileOutputStream.write(bArr, 0, read);
                    }
                } finally {
                    fileOutputStream.close();
                }
                if (!z) {
                    file.delete();
                    return;
                }
            } finally {
                inputStream.close();
            }
            if (!file.renameTo(fileFor)) {
                file.delete();
            }
        } catch (Exception e) {
            Log.d(TAG, "补抓失败 " + str + " : " + e.getMessage());
            file.delete();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    public byte[] readAll(File file, long j) {
        if (file == null || !file.exists() || file.length() > j) {
            return null;
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) file.length());
            byte[] bArr = new byte[16384];
            while (true) {
                int read = fileInputStream.read(bArr);
                if (read <= 0) {
                    break;
                }
                byteArrayOutputStream.write(bArr, 0, read);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException unused) {
            return null;
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException unused2) {
                }
            }
        }
    }

    public long sizeBytes() {
        return dirSize(this.root);
    }

    public void clear() {
        deleteRecursive(this.root);
        this.root.mkdirs();
    }

    public int count() {
        File[] listFiles = this.root.listFiles();
        if (listFiles == null) {
            return 0;
        }
        int i = 0;
        for (File file : listFiles) {
            if (file.isFile() && !file.getName().endsWith(".part")) {
                i++;
            }
        }
        return i;
    }

    private static long dirSize(File file) {
        long j = 0;
        if (file != null && file.exists()) {
            if (file.isFile()) {
                return file.length();
            }
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                return 0L;
            }
            for (File file2 : listFiles) {
                j += dirSize(file2);
            }
        }
        return j;
    }

    private static void deleteRecursive(File file) {
        File[] listFiles;
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory() && (listFiles = file.listFiles()) != null) {
            for (File file2 : listFiles) {
                deleteRecursive(file2);
            }
        }
        file.delete();
    }
}
