package com.dhy.imagecaputer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

abstract class ImageCaptureData extends ImageCapturePage {
    private int lastImageViewId;
    private ImageSetter imageSetter;
    private Map<Integer, ImageHolder> buffer = new HashMap<>();
    private static final String KEY_BUFFER = ImageCaptureData.class.getName();

    public <T extends View.OnCreateContextMenuListener> ImageCaptureData(T activityOrFragment, @NonNull ImageSetter imageSetter) {
        super(activityOrFragment);
        this.imageSetter = imageSetter;
    }

    public int getImageCount() {
        int count = 0;
        for (ImageHolder i : buffer.values()) {
            if (i.hasImage()) count++;
        }
        return count;
    }

    protected boolean hasImage(View... imageViews) {
        for (View imageView : imageViews) {
            if (!getImageHolder(imageView.getId()).hasImage()) {
                return false;
            }
        }
        return true;
    }

    protected boolean hasImage(int viewId) {
        return getImageHolder(viewId).hasImage();
    }

    @NonNull
    protected ImageHolder getImageHolder(int viewId) {
        ImageHolder status = buffer.get(viewId);
        if (status == null) {
            status = new ImageHolder();
            buffer.put(viewId, status);
        }
        return status;
    }

    @NonNull
    protected ImageHolder getLastImageHolder() {
        return getImageHolder(lastImageViewId);
    }

    protected Map<Integer, ImageHolder> getImageHolders() {
        return buffer;
    }

    protected void setLastImageViewId(int viewId) {
        lastImageViewId = viewId;
    }

    protected int getLastImageViewId() {
        return lastImageViewId;
    }

    //region restore data
    public void onSaveInstanceState(Bundle outState) {
        Map<String, Serializable> map = new HashMap<>();
        map.put(Map.class.getSimpleName(), (Serializable) buffer);
        map.put(Integer.class.getSimpleName(), lastImageViewId);
        outState.putSerializable(KEY_BUFFER, (Serializable) map);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        Serializable serializable = savedInstanceState.getSerializable(KEY_BUFFER);
        if (serializable != null) {
            Map<String, Serializable> map = (Map<String, Serializable>) serializable;
            this.buffer = (Map<Integer, ImageHolder>) map.get(Map.class.getSimpleName());
            lastImageViewId = (int) map.get(Integer.class.getSimpleName());
            updateView();
        }
    }

    protected void updateView(ImageView imageView, ImageHolder holder) {
        if (holder.hasImage()) {
            if (imageView.getVisibility() != View.VISIBLE) imageView.setVisibility(View.VISIBLE);
            imageSetter.setImage(imageView, holder.getRawImageUri());
        }
    }

    private void updateView() {
        for (Integer id : buffer.keySet()) {
            updateView((ImageView) findViewById(id), buffer.get(id));
        }
    }
    //endregion
}
