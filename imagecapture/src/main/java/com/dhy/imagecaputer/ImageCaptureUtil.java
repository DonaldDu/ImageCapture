package com.dhy.imagecaputer;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageCaptureUtil extends ImageCaptureData {
    private int REQUEST_TAKE_PHOTO, REQUEST_PICK_IMAGE;
    private CaptureSetting setting;

    /**
     * creat with default {@link CaptureSetting} and {@link ImageSetter} of imageView.setImageURI(uri);
     * <p>use {@link #initSetting(CaptureSetting)} to set custom setting</p>
     */
    public <T extends View.OnCreateContextMenuListener> ImageCaptureUtil(T activityOrFragment) {
        this(activityOrFragment, new ImageSetter() {
            @Override
            public void setImage(ImageView imageView, Uri uri) {
                imageView.setImageURI(uri);
            }
        });
    }

    public <T extends View.OnCreateContextMenuListener> ImageCaptureUtil(T activityOrFragment, @NonNull ImageSetter imageSetter) {
        super(activityOrFragment, imageSetter);
        initSetting(new CaptureSetting());
    }

    public void initSetting(@NonNull CaptureSetting setting) {
        this.setting = setting;
        this.REQUEST_TAKE_PHOTO = setting.REQUEST_TAKE_PHOTO;
        this.REQUEST_PICK_IMAGE = setting.REQUEST_PICK_IMAGE;
    }

    public void showChooseDialog(final View imageView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("You may override this to show custom dialog");
        builder.setNegativeButton("PickImage", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                pickImage(imageView);
            }
        });
        builder.setPositiveButton("TakePhoto", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                takePhoto(imageView);
            }
        });
        builder.show();
    }

    public void takePhoto(View imageView) {
        setLastImageViewId(imageView.getId());
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            ImageHolder holder = getImageHolder(imageView.getId());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(holder.getTempImageFile(context)));
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        } else {
            Toast.makeText(context, "没有找到相关应用，请直接选择图片", Toast.LENGTH_LONG).show();
        }
    }

    public void pickImage(View imageView) {
        setLastImageViewId(imageView.getId());
        @SuppressLint("InlinedApi")
        Intent intent = new Intent(Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            getImageHolder(imageView.getId());
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } else {
            Toast.makeText(context, "没有找到相关应用，请使用相机拍照", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * @return get image or not
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO || requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                onActivityResult(requestCode == REQUEST_TAKE_PHOTO, data);
                return true;
            } else if (requestCode == REQUEST_TAKE_PHOTO) {//cancel take photo
                getLastImageHolder().deleteTempImageFile();
                return true;
            }
        }
        return false;
    }

    private void onActivityResult(boolean takePhoto, Intent data) {
        if (takePhoto) {
            ImageHolder status = getLastImageHolder();
            File file = status.getTempImageFile(context);
            checkImageRotate(file);
            status.saveTempImageFile();
            onGetImageUri(Uri.fromFile(file));
        } else {//pick image
            onGetImageUri(data.getData());
        }
    }

    private void onGetImageUri(Uri uri) {
        try {
            ImageHolder holder = getLastImageHolder();
            holder.onGetNewRawImage(uri);
            ImageView view = (ImageView) findViewById(getLastImageViewId());
            updateView(view, holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void checkImageRotate(File photo) {
        try {
            int digree = ImageRotateUtil.getRotateDigree(photo);
            if (digree > 0) {
                ImageCompressUtil.compressJpegImage(context, Uri.fromFile(photo), photo,
                        (int) setting.maxWidth,
                        (int) setting.maxHeight,
                        setting.maxSizeInBytes);
                Bitmap bitmap = BitmapFactory.decodeFile(photo.getAbsolutePath());
                bitmap = ImageRotateUtil.rotate(bitmap, digree);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(photo));
                bitmap.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //region for upload image

    private void prepareUploadFile() throws FileNotFoundException {
        Map<Integer, ImageHolder> holders = getImageHolders();
        for (ImageHolder h : holders.values()) {
            if (h.needPrepared()) {
                File file = ImageHolder.getJpgImageFile(context);
                ImageCompressUtil.compressJpegImage(context, h.getRawImage(), file,
                        (int) setting.maxWidth,
                        (int) setting.maxHeight,
                        setting.maxSizeInBytes);
                h.setUploadFile(file);
            }
        }
    }

    public void setUploadedUrl(int viewId, String imageUrl) {
        getImageHolder(viewId).setUploadedImageUrl(imageUrl);
    }

    /**
     * @return null when no more to upload
     */
    @Nullable
    private ImageHolder getNextUploadTask() {
        for (Integer id : getImageHolders().keySet()) {
            ImageHolder h = getImageHolder(id);
            if (!h.isUploaded() && h.isReadyToUpload()) {
                return h;
            }
        }
        return null;
    }

    public void uploadImage(final ImageUploader imageUploader) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                imageUploader.onPrepareUploadFile();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    prepareUploadFile();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ImageHolder task = getNextUploadTask();
                if (task != null) {
                    imageUploader.upload(ImageCaptureUtil.this, task.getViewId(), task.getUploadFile());
                } else imageUploader.onFinishAll(getAllUploadImageResults());
            }
        }.execute();
    }

    public List<ImageHolder> getAllUploadImageResults() {
        List<ImageHolder> list = new ArrayList<>();
        for (ImageHolder h : getImageHolders().values()) {
            if (h.isUploaded()) list.add(h);
        }
        return list;
    }
    //endregion
}
