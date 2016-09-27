package com.dhy.imagecapturesample;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.dhy.imagecaputer.ImageCaptureUtil;
import com.dhy.imagecaputer.ImageHolder;
import com.dhy.imagecaputer.ImageUploader;
import com.facebook.drawee.backends.pipeline.Fresco;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ImageCaptureUtil imageCaptureUtil;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageCaptureUtil = new ImageCaptureUtil(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        imageCaptureUtil.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imageCaptureUtil.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageCaptureUtil.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imageCaptureUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void upload() {
        imageCaptureUtil.uploadImage(new ImageUploader() {
            @Override
            public void onPrepareUploadFile() {

            }

            @Override
            public void upload(ImageCaptureUtil util, int viewId, File file) {
                onFinishOne(util, viewId, file.getAbsolutePath());
            }

            @Override
            public void onFinishAll(List<ImageHolder> urls) {

            }
        });
    }

    public void choose(View view) {
        imageCaptureUtil.showChooseDialog(view);
    }
}
