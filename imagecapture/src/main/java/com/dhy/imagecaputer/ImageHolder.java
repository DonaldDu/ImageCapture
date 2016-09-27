package com.dhy.imagecaputer;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageHolder implements Serializable {
    private File uploadFile, temp;
    private String rawImageUri;
    private String uploadedImageUrl;
    private int viewId;

    public ImageHolder(@IdRes int viewId) {
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

    @Nullable
    File getTempImageFile(Context context) {
        if (temp == null) temp = getJpgImageFile(context, viewId);
        return temp;
    }

    void deleteTempImageFile() {
        delete(temp);
        temp = null;
    }

    void saveTempImageFile() {
        temp = null;
    }

    @Nullable
    static File getJpgImageFile(Context context, @IdRes int viewId) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + Math.abs(viewId);
            File storageDir = getTempFileDir(context);
            try {
                return File.createTempFile(imageFileName, ".jpg", storageDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static File getTempFileDir(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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

    @IdRes
    public int getViewId() {
        return viewId;
    }
}
