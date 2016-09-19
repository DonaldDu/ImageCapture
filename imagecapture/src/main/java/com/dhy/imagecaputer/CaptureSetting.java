package com.dhy.imagecaputer;

public class CaptureSetting {
    /**
     * use to compress image when upload
     */
    public long maxWidth = 600, maxHeight = 800, maxSizeInBytes = 1024 * 1024;
    public int REQUEST_TAKE_PHOTO = 101, REQUEST_PICK_IMAGE = 102;
}
