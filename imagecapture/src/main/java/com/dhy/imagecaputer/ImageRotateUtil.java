package com.dhy.imagecaputer;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;

/**
 * Created by donald on 2016/9/20.
 */
public class ImageRotateUtil {
    static int getRotateDigree(File file) throws IOException {
        if (file != null && file.exists() && file.length() > 0) {
            ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
            String tag = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            if (tag != null) {
                int digree;
                switch (Integer.parseInt(tag)) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        digree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        digree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        digree = 270;
                        break;
                    default:
                        digree = 0;
                        break;
                }
                return digree;
            }
        }
        return 0;
    }

    public static Bitmap rotate(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                bitmap.recycle();
                return b2;
            } catch (OutOfMemoryError ignored) {
            }
        }
        return bitmap;
    }
}
