package com.dhy.imagecaputer;

import java.io.File;
import java.util.List;

public abstract class ImageUploader {
    public abstract void onPrepareUploadFile();

    public abstract void upload(ImageCaptureUtil util, int viewId, File file);

    public void onFinishOne(ImageCaptureUtil util, int viewId, String imageUrl) {
        util.setUploadedUrl(viewId, imageUrl);
        util.uploadImage(this);
    }

    public abstract void onFinishAll(List<String> urls);
}
