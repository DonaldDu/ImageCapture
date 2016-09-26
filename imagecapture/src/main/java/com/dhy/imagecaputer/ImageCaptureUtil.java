package com.dhy.imagecaputer;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

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
        ImageHolder holder = getImageHolder(imageView.getId());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(holder.getTempImageFile(context)));
        try {
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        } catch (Exception e) {
            onStartCaptureImageError(true, e);
        }
    }

    public void pickImage(View imageView) {
        setLastImageViewId(imageView.getId());
        @SuppressLint("InlinedApi")
        Intent intent = new Intent(Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            onStartCaptureImageError(false, e);
        }
    }

    protected void onStartCaptureImageError(boolean takePhoto, Exception e) {
        e.printStackTrace();
        if (e instanceof SecurityException) {
            String[] permission;
            if (takePhoto) {
                permission = new String[]{Manifest.permission.CAMERA};
            } else {
                permission = new String[]{
                        Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                };
            }
            int requestCode = takePhoto ? REQUEST_TAKE_PHOTO : REQUEST_PICK_IMAGE;
            ActivityCompat.requestPermissions((Activity) context, permission, requestCode);
        } else {
            String msg;
            if (e instanceof ActivityNotFoundException) {
                msg = takePhoto ? "没有找到拍照相关应用" : "没有找到选择图片相关应用";
            } else msg = takePhoto ? "未知错误，相机拍照失败" : "未知错误，选择图片失败";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Boolean take = isTakePhotoRequestPermissionsResult(requestCode);
        if (take == null) return;
        if (grantResults != null && grantResults.length >= 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                View view = findViewById(getLastImageViewId());
                if (take) takePhoto(view);
                else pickImage(view);
            }
        }
    }

    /**
     * @return true take photo, fales pick image, null unkown
     */
    @Nullable
    private Boolean isTakePhotoRequestPermissionsResult(int requestCode) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            return true;
        } else if (requestCode == REQUEST_PICK_IMAGE) {
            return false;
        } else return null;
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
                return false;
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
        for (ImageHolder h : getImageHolders()) {
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
        for (ImageHolder h : getImageHolders()) {
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
        for (ImageHolder h : getImageHolders()) {
            if (h.isUploaded()) list.add(h);
        }
        return list;
    }
    //endregion
}
