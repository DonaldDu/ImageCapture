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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
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
    private static final String[] permissionTake = new String[]{Manifest.permission.CAMERA};
    private static final String[] permissionPick = new String[]{Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * NOT: you need set 'file_provider_authority' in string file
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

    /**
     * NOT: you need set 'file_provider_authority' in string file
     */
    public <T extends View.OnCreateContextMenuListener> ImageCaptureUtil(T activityOrFragment, @NonNull ImageSetter imageSetter) {
        super(activityOrFragment, imageSetter);
        initSetting(new CaptureSetting());
    }

    public void initSetting(@NonNull CaptureSetting setting) {
        this.setting = setting;
        this.REQUEST_TAKE_PHOTO = setting.REQUEST_TAKE_PHOTO;
        this.REQUEST_PICK_IMAGE = setting.REQUEST_PICK_IMAGE;
    }

    public void showChooseDialog(@NonNull final View imageView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("请选择获取图片方式");
        builder.setNegativeButton("选择图片", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                pickImage(imageView);
            }
        });
        builder.setPositiveButton("拍照", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                takePhoto(imageView);
            }
        });
        builder.show();
    }

    //    http://www.jianshu.com/p/ba57444a7e69#
    //    http://www.cnblogs.com/jun-it/articles/2881826.html#commentform
    public void startPhotoZoom(Uri inputUri, File file) {// TODO: 2017/5/23/023
        if (inputUri == null) {
            Log.i("TAG", "The uri is not exist.");
            return;
        }

        Intent intent = new Intent("com.android.camera.action.CROP");
        //sdk>=24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri outPutUri = Uri.fromFile(file);
            intent.setDataAndType(inputUri, "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
            intent.putExtra("noFaceDetection", false);//去除默认的人脸识别，否则和剪裁匡重叠
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        } else {
            Uri outPutUri = Uri.fromFile(file);
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                String url = GetImagePath.getPath(context, inputUri);//这个方法是处理4.4以上图片返回的Uri对象不同的处理方法
                intent.setDataAndType(Uri.fromFile(new File(url)), "image/*");
            } else {
                intent.setDataAndType(inputUri, "image/*");
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
        }

        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());// 图片格式
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);//这里就将裁剪后的图片的Uri返回了
    }

    public void takePhoto(@NonNull View imageView) {
        setLastImageViewId(imageView.getId());
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = getImageHolder(imageView.getId()).getTempImageFile(context);
        if (file == null) {
            onCancelTakePhotoForCreateTempFileFailed();
            return;
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoUri(file));
        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 600);
        intent.putExtra("outputY", 600);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());// 图片格式
        if (hasPermission(permissionTake)) {
            try {
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } catch (Exception e) {
                onStartCaptureImageError(true, e);
            }
        } else {
            showRequestPermissionsDialog(true);
        }
    }

    protected void onCancelTakePhotoForCreateTempFileFailed() {
        Toast.makeText(context, "创建临时文件失败！", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private Uri getPhotoUri(File file) {
        if (Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(context, context.getPackageName(), file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public void pickImage(@NonNull View imageView) {
        setLastImageViewId(imageView.getId());
        Intent intent = new Intent(getPickImageAction());
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            onStartCaptureImageError(false, e);
        }
    }

    @SuppressLint("InlinedApi")
    private String getPickImageAction() {
        return Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT;
    }

    protected void onStartCaptureImageError(boolean takePhoto, Exception e) {
        e.printStackTrace();
        if (e instanceof SecurityException) {
            showRequestPermissionsDialog(takePhoto);
        } else {
            String msg;
            if (e instanceof ActivityNotFoundException) {
                msg = takePhoto ? "没有找到拍照相关应用" : "没有找到选择图片相关应用";
            } else msg = takePhoto ? "未知错误，相机拍照失败" : "未知错误，选择图片失败";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasPermission(String[] permission) {
        for (String p : permission) {
            if (ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_DENIED)
                return false;
        }
        return true;
    }

    protected void showRequestPermissionsDialog(final boolean takePhoto) {
        if (shouldShowRequestPermissionRationale(takePhoto)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("请开启【" + (takePhoto ? "拍照" : "文件读写") + "】权限，以便使用此功能");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton("开启", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    requestPermissions(takePhoto);
                }
            });
            builder.show();
        } else {
            requestPermissions(takePhoto);
        }
    }

    protected void requestPermissions(boolean takePhoto) {
        String[] permissions = takePhoto ? permissionTake : permissionPick;
        int requestCode = takePhoto ? REQUEST_TAKE_PHOTO : REQUEST_PICK_IMAGE;
        ActivityCompat.requestPermissions((Activity) context, permissions, requestCode);
    }

    protected boolean shouldShowRequestPermissionRationale(boolean takePhoto) {
        String[] permissions = takePhoto ? permissionTake : permissionPick;
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permission))
                return true;
        }
        return false;
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

    private void checkImageRotate(File photo) {
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
                File file = ImageHolder.getJpgImageFile(context, h.getViewId());
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
