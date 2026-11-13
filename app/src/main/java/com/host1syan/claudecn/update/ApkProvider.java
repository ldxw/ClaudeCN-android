package com.host1syan.claudecn.update;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * 把下载到私有目录的安装包暴露成 content:// Uri，供系统安装器读取。
 * 零依赖环境下代替 androidx 的 FileProvider：只放行 updates/ 下的 .apk，其他路径一律拒绝。
 */
public final class ApkProvider extends ContentProvider {
    public static final String AUTHORITY = "com.host1syan.claudecn.updates";
    private static final String MIME_APK = "application/vnd.android.package-archive";

    /** updates 目录，与 UpdateInstaller 共用。 */
    static File updatesDir(android.content.Context context) {
        File dir = new File(context.getCacheDir(), "updates");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** 生成给安装器用的 Uri。 */
    public static Uri uriFor(String fileName) {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(fileName)
                .build();
    }

    /** 把 Uri 还原成受控目录内的文件；越界或非 apk 返回 null。 */
    static File resolve(android.content.Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        if (!AUTHORITY.equals(uri.getAuthority())) {
            return null;
        }
        String name = uri.getLastPathSegment();
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        if (name.contains("/") || name.contains("\\") || name.startsWith(".")) {
            return null;
        }
        if (!name.endsWith(".apk")) {
            return null;
        }
        File dir = updatesDir(context);
        File file = new File(dir, name);
        try {
            String rootPath = dir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(rootPath + File.separator)) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        File file = resolve(getContext(), uri);
        if (file == null || !file.exists()) {
            return null;
        }
        String[] columns = projection != null && projection.length > 0
                ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) {
                row[i] = file.getName();
            } else if (OpenableColumns.SIZE.equals(columns[i])) {
                row[i] = file.length();
            } else {
                row[i] = null;
            }
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return MIME_APK;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = resolve(getContext(), uri);
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("不在 updates 目录内: " + uri);
        }
        // 安装器只需要读，写入模式一律拒绝
        if (mode != null && mode.contains("w")) {
            throw new FileNotFoundException("只读提供: " + uri);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("只读提供者");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("只读提供者");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("只读提供者");
    }
}
