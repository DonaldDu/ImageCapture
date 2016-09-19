package com.dhy.imagecaputer;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

abstract class ImageCapturePage {
    private Object activityOrFragment;
    protected Context context;
    private Activity activity;

    public <T extends View.OnCreateContextMenuListener> ImageCapturePage(T activityOrFragment) {
        this.activityOrFragment = activityOrFragment;
        context = activity = getActivity();
    }

    private Activity getActivity() {
        if (activityOrFragment instanceof Activity) {
            return (Activity) activityOrFragment;
        } else if (activityOrFragment instanceof Fragment) {//fragment
            return ((Fragment) activityOrFragment).getActivity();
        } else {
            throw new IllegalArgumentException("must be activityOrFragment");
        }
    }

    protected View findViewById(int id) {
        return activity.findViewById(id);
    }

    protected void startActivityForResult(Intent intent, int requestCode) throws SecurityException {
        if (activityOrFragment instanceof Activity) {
            ((Activity) activityOrFragment).startActivityForResult(intent, requestCode);
        } else {//fragment
            ((Fragment) activityOrFragment).startActivityForResult(intent, requestCode);
        }
    }

    /**
     * @return get image or not
     */
    public abstract boolean onActivityResult(int requestCode, int resultCode, Intent data);

    public abstract void onSaveInstanceState(Bundle outState);

    public abstract void onRestoreInstanceState(Bundle savedInstanceState);
}
