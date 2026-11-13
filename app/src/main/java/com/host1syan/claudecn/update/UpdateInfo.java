package com.host1syan.claudecn.update;

/** 一次「检查更新」的结果。error 非空时其余字段不保证有意义。 */
public final class UpdateInfo {
    public final String versionName;
    public final String tagName;
    public final String notes;
    public final String assetName;
    public final String downloadUrl;
    public final String digest;
    public final long sizeBytes;
    public final boolean newer;
    public final String error;

    UpdateInfo(String versionName, String tagName, String notes, String assetName,
               String downloadUrl, String digest, long sizeBytes, boolean newer, String error) {
        this.versionName = versionName;
        this.tagName = tagName;
        this.notes = notes;
        this.assetName = assetName;
        this.downloadUrl = downloadUrl;
        this.digest = digest;
        this.sizeBytes = sizeBytes;
        this.newer = newer;
        this.error = error;
    }

    public static UpdateInfo failure(String message) {
        return new UpdateInfo(null, null, null, null, null, null, 0L, false, message);
    }

    public boolean hasAsset() {
        return this.downloadUrl != null && !this.downloadUrl.isEmpty()
                && this.assetName != null && !this.assetName.isEmpty();
    }
}
