package com.example;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Utility to ensure photos captured by the camera or selected from storage are
 * saved vertically (portrait orientation) and decoded upright.
 * Includes modern in-memory LRU caching and explicit memory trimming as recommended
 * since Android Q instead of deprecated ashmem pinning.
 */
public final class ImageRotationHelper {

    private static final String TAG = "ImageRotationHelper";

    // Cache up to 1/8th of available app memory for sampled bitmaps
    private static final int MAX_MEMORY = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private static final int CACHE_SIZE = Math.max(1024, MAX_MEMORY / 8);

    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    private ImageRotationHelper() {}

    /**
     * Trims in-memory bitmap cache when system memory pressure is detected.
     */
    public static void trimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            BITMAP_CACHE.evictAll();
            Log.d(TAG, "Evicted all cached bitmaps on TRIM_MEMORY_MODERATE or higher");
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            BITMAP_CACHE.trimToSize(CACHE_SIZE / 2);
            Log.d(TAG, "Trimmed bitmap cache to 50% on moderate memory pressure");
        }
    }

    /**
     * Clears all cached bitmaps completely (e.g. on low memory warning).
     */
    public static void clearMemory() {
        BITMAP_CACHE.evictAll();
        Log.d(TAG, "Evicted all cached bitmaps on clearMemory()");
    }

    /**
     * Inspects the image file, extracts any EXIF rotation tag, and rewrites the JPEG
     * file so the raw pixel data is rotated vertically and stored upright.
     * Also resets the EXIF orientation tag to ORIENTATION_NORMAL.
     *
     * @param file The image file on disk to inspect and adjust.
     * @return True if rotated or already vertical, false if error occurred.
     */
    public static boolean fixPhotoVerticalOrientation(@Nullable File file) {
        if (file == null || !file.exists() || file.length() == 0) {
            return false;
        }

        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            int degrees = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                degrees = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                degrees = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                degrees = 270;
            }

            // Check dimensions: if image is wider than it is tall and degrees is 0,
            // check if phone camera recorded landscape dimensions for a vertical shot
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), boundsOptions);

            // If EXIF requires rotation:
            if (degrees != 0) {
                Bitmap source = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (source != null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(degrees);
                    Bitmap rotated = Bitmap.createBitmap(
                            source, 0, 0,
                            source.getWidth(), source.getHeight(),
                            matrix, true
                    );

                    FileOutputStream fos = new FileOutputStream(file);
                    rotated.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                    fos.flush();
                    fos.close();

                    if (rotated != source) {
                        source.recycle();
                    }
                    rotated.recycle();

                    // Reset EXIF orientation so subsequent decoders treat it as upright
                    try {
                        ExifInterface newExif = new ExifInterface(file.getAbsolutePath());
                        newExif.setAttribute(
                                ExifInterface.TAG_ORIENTATION,
                                String.valueOf(ExifInterface.ORIENTATION_NORMAL)
                        );
                        newExif.saveAttributes();
                    } catch (Exception ignored) {}

                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to fix photo vertical orientation: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Decodes an image file into a sampled Bitmap, applying any residual EXIF rotation
     * to guarantee that the returned Bitmap is displayed vertically / upright.
     */
    @Nullable
    public static Bitmap loadOrientedBitmap(@Nullable String path, int reqWidth, int reqHeight) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            return null;
        }

        String cacheKey = path + "@" + reqWidth + "x" + reqHeight + "_" + file.lastModified();
        Bitmap cached = BITMAP_CACHE.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null;
            }

            // Calculate inSampleSize
            int inSampleSize = 1;
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            options.inSampleSize = Math.max(1, inSampleSize);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null) {
                return null;
            }

            // Check if EXIF orientation is present and rotate if necessary
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            int degrees = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                degrees = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                degrees = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                degrees = 270;
            }

            Bitmap finalBitmap = bitmap;
            if (degrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(degrees);
                Bitmap rotated = Bitmap.createBitmap(
                        bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        matrix, true
                );
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                finalBitmap = rotated;
            }

            if (finalBitmap != null) {
                BITMAP_CACHE.put(cacheKey, finalBitmap);
            }
            return finalBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error loading oriented bitmap: " + e.getMessage());
            return null;
        }
    }
}
