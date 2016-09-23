package com.dhy.imagecaputer;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageHolder implements Serializable {
    private File uploadFile, temp;
    private String rawImageUri;
    private String uploadedImageUrl;
    private int viewId;

    public ImageHolder(int viewId) {
        this.viewId = viewId;
    }

    public String getUploadedImageUrl() {
        return uploadedImageUrl;
    }

    void setUploadedImageUrl(String uploadedImageUrl) {
        this.uploadedImageUrl = uploadedImageUrl;
    }

    boolean isUploaded() {
        return !TextUtils.isEmpty(uploadedImageUrl);
    }

    boolean hasImage() {
        return rawImageUri != null;
    }

    File getTempImageFile(Context context) {
        if (temp == null) temp = getJpgImageFile(context);
        return temp;
    }

    void deleteTempImageFile() {
        delete(temp);
        temp = null;
    }

    void saveTempImageFile() {
        temp = null;
    }

    static File getJpgImageFile(Context context) {
        File dir = context.getExternalCacheDir();
        if (dir == null) dir = context.getCacheDir();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = context.getClass().getName() + timeStamp + ".jpg";
        return new File(dir, fileName);
    }

    boolean needPrepared() {
        return hasImage() && !isReadyToUpload() && !isUploaded();
    }

    boolean isReadyToUpload() {
        return uploadFile != null;
    }

    private void delete(File file) {
        if (file != null && file.exists()) file.delete();
    }

    Uri getRawImage() {
        if (rawImageUri != null) {
            return Uri.parse(rawImageUri);
        } else if (uploadedImageUrl != null) {
            return Uri.parse(uploadedImageUrl);
        }
        return null;
    }

    void onGetNewRawImage(Uri rawImageUri) {
        if (!rawImageUri.equals(getRawImage())) {
            reset();
        }
        this.rawImageUri = rawImageUri.toString();
    }

    public void reset() {
        delete(uploadFile);
        delete(temp);

        uploadFile = null;
        temp = null;
        rawImageUri = null;
        uploadedImageUrl = null;
    }

    void setUploadFile(File uploadFile) {
        this.uploadFile = uploadFile;
    }

    public File getUploadFile() {
        return uploadFile;
    }

    public int getViewId() {
        return viewId;
    }
}
