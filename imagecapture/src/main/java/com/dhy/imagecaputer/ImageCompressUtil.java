package com.dhy.imagecaputer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


public class ImageCompressUtil {
    /**
     * compressJpegImage(path, 480, 800, 1024 * 1024);//1M
     */
    public static boolean compressJpegImage(Context context, Uri rawImageUri, File fileToStore) throws FileNotFoundException {
        return compressJpegImage(context, rawImageUri, fileToStore, 480, 800, 1024 * 1024);//1M
    }

    /**
     * @param maxSize in bytes
     * @return success or not
     */
    public static boolean compressJpegImage(Context context, Uri rawImageUri, File fileToStore, int maxW, int maxH, long maxSize) throws FileNotFoundException {
        BitmapFactory.Options options = getCompressImageOptions(context, rawImageUri, maxW, maxH);
        Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(rawImageUri), null, options);
        int quality = getCompressJpegImageQuality(bitmap, maxSize);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileToStore);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream);
            bitmap.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * @param maxSize in bytes
     */
    private static int getCompressJpegImageQuality(Bitmap image, long maxSize) {
        if (image.getByteCount() > maxSize) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int quality = (int) (100 * maxSize / image.getByteCount());
            image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            while (outputStream.toByteArray().length > maxSize) {
                outputStream.reset();
                image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                quality -= 10;
            }
            outputStream.reset();
            return quality;
        }
        return 100;
    }

    private static BitmapFactory.Options getCompressImageOptions(Context context, Uri uri, int maxW, int maxH) throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        options.inJustDecodeBounds = false;
        options.inSampleSize = computeSampleSize(options, maxW, maxH);
        return options;
    }

    private static int computeSampleSize(BitmapFactory.Options options, int width, int height) {
        int initialSize = computeInitialSampleSize(options, width, height);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int width, int height) {
        int minSideLength = Math.min(width, height);
        long maxNumOfPixels = width * height;
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
}
