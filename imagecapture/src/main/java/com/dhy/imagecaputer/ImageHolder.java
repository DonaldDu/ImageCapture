package com.dhy.imagecaputer;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

class ImageHolder implements Serializable {
    private File fileToUpload, temp;
    private String rawImageUri;
    private String uploadedImageUrl;

    public ImageHolder() {
    }

    public String getUploadedImageUrl() {
        return uploadedImageUrl;
    }

    public void setUploadedImageUrl(String uploadedImageUrl) {
        this.uploadedImageUrl = uploadedImageUrl;
    }

    public boolean isUploaded() {
        return !TextUtils.isEmpty(uploadedImageUrl);
    }

    public boolean hasImage() {
        return rawImageUri != null;
    }

    public File getTempImageFile(Context context) {
        if (temp == null) temp = getJpgImageFile(context);
        return temp;
    }

    public void deleteTempImageFile() {
        delete(temp);
        temp = null;
    }

    public void saveTempImageFile() {
        temp = null;
    }

    public static File getJpgImageFile(Context context) {
        File dir = context.getExternalCacheDir();
        if (dir == null) dir = context.getCacheDir();
        Log.i("getJpgImageFile", "CacheDir " + dir.getAbsolutePath());
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = context.getClass().getName() + timeStamp + ".jpg";
        return new File(dir, fileName);
    }

    public void reset() {
        delete(fileToUpload);
        fileToUpload = null;

        deleteTempImageFile();
        rawImageUri = null;
    }

    public boolean needPrepared() {
        return hasImage() && !isReadyToUpload() && !isUploaded();
    }

    public boolean isReadyToUpload() {
        return fileToUpload != null;
    }

    private void delete(File file) {
        if (file != null && file.exists()) file.delete();
    }

    public Uri getRawImageUri() {
        return Uri.parse(rawImageUri);
    }

    public void setRawImageUri(Uri rawImageUri) {
        this.rawImageUri = rawImageUri.toString();
    }

    public void setFileToUpload(File fileToUpload) {
        this.fileToUpload = fileToUpload;
    }

    public File getFileToUpload() {
        return fileToUpload;
    }
}
