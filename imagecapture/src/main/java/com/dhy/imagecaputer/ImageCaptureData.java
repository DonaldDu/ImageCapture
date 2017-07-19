package com.dhy.imagecaputer;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

abstract class ImageCaptureData extends ImageCapturePage {
    private int lastImageViewId;
    private ImageSetter imageSetter;
    private Map<Integer, ImageHolder> buffer = new HashMap<>();
    private static final String KEY_BUFFER = ImageCaptureData.class.getName();

    public <T extends View.OnCreateContextMenuListener> ImageCaptureData(T activityOrFragment, @Nullable ImageSetter imageSetter) {
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

    @NonNull
    public ImageHolder getImageHolder(View view) {
        return getImageHolder(view.getId());
    }

    @NonNull
    public ImageHolder getImageHolder(int viewId) {
        ImageHolder status = buffer.get(viewId);
        if (status == null) {
            status = new ImageHolder(viewId);
            buffer.put(viewId, status);
        }
        return status;
    }

    public Map<Integer, ImageHolder> getImages() {
        return buffer;
    }

    public void loadExtraImages(Map<Integer, ImageHolder> extra) {
        if (extra != null) for (Integer viewId : extra.keySet()) {
            buffer.put(viewId, extra.get(viewId));
        }
    }

    @NonNull
    protected ImageHolder getLastImageHolder() {
        return getImageHolder(lastImageViewId);
    }

    protected Collection<ImageHolder> getImageHolders() {
        return buffer.values();
    }

    protected void setLastImageViewId(int viewId) {
        lastImageViewId = viewId;
    }

    protected int getLastImageViewId() {
        return lastImageViewId;
    }

    public void reset() {
        for (ImageHolder holder : buffer.values()) {
            holder.deleteTempImageFile();
            File file = holder.getUploadFile();
            if (file != null && file.exists()) file.delete();
            imageSetter.setImage((ImageView) findViewById(holder.getViewId()), Uri.EMPTY);
        }
        buffer.clear();
        lastImageViewId = View.NO_ID;
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
        if (imageView != null && imageSetter != null && holder.hasImage()) {
            if (imageView.getVisibility() != View.VISIBLE) imageView.setVisibility(View.VISIBLE);
            imageSetter.setImage(imageView, holder.getRawImage());
        }
    }

    public void updateView() {
        final Iterator<Integer> iterator = buffer.keySet().iterator();
        while (iterator.hasNext()) {
            int id = iterator.next();
            View view = findViewById(id);
            if (view instanceof ImageView) {
                updateView((ImageView) view, buffer.get(id));
            } else {
                buffer.get(id).reset();
                iterator.remove();
            }
        }
    }
    //endregion
}
