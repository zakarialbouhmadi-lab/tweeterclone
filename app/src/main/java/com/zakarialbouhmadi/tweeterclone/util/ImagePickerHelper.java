package com.zakarialbouhmadi.tweeterclone.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImagePickerHelper {
    public static final int PICK_IMAGE_REQUEST = 1;
    
    // Max dimensions for different image types
    public static final int MAX_PROFILE_SIZE = 512;  // 512x512 for profile pics
    public static final int MAX_TWEET_IMAGE_WIDTH = 1080;  // Max width for tweet images
    public static final int MAX_TWEET_IMAGE_HEIGHT = 1350; // Max height for tweet images
    
    // Compression quality
    public static final int PROFILE_QUALITY = 80;
    public static final int TWEET_IMAGE_QUALITY = 75;

    public static void openImagePicker(Activity activity) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public static byte[] getBytes(ContentResolver contentResolver, Uri uri) throws IOException {
        InputStream inputStream = contentResolver.openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    /**
     * Compress and resize image for profile picture
     * @param contentResolver Content resolver to read the image
     * @param uri Uri of the selected image
     * @return Base64 encoded string of the compressed image
     */
    public static String compressProfileImage(ContentResolver contentResolver, Uri uri) throws IOException {
        Bitmap bitmap = getBitmapFromUri(contentResolver, uri);
        bitmap = rotateImageIfRequired(contentResolver, uri, bitmap);
        bitmap = resizeToSquare(bitmap, MAX_PROFILE_SIZE);
        return compressBitmapToBase64(bitmap, PROFILE_QUALITY);
    }

    /**
     * Compress and resize image for tweet
     * @param contentResolver Content resolver to read the image
     * @param uri Uri of the selected image
     * @return Base64 encoded string of the compressed image
     */
    public static String compressTweetImage(ContentResolver contentResolver, Uri uri) throws IOException {
        Bitmap bitmap = getBitmapFromUri(contentResolver, uri);
        bitmap = rotateImageIfRequired(contentResolver, uri, bitmap);
        bitmap = resizeToFit(bitmap, MAX_TWEET_IMAGE_WIDTH, MAX_TWEET_IMAGE_HEIGHT);
        return compressBitmapToBase64(bitmap, TWEET_IMAGE_QUALITY);
    }

    /**
     * Get Bitmap from Uri with sample size optimization
     */
    private static Bitmap getBitmapFromUri(ContentResolver contentResolver, Uri uri) throws IOException {
        // First, get the dimensions without loading the full image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        
        InputStream inputStream = contentResolver.openInputStream(uri);
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        // Calculate sample size
        int sampleSize = calculateInSampleSize(options, MAX_TWEET_IMAGE_WIDTH, MAX_TWEET_IMAGE_HEIGHT);
        
        // Decode with sample size
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        
        inputStream = contentResolver.openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        return bitmap;
    }

    /**
     * Calculate the optimal sample size for loading large images
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Rotate image based on EXIF data
     */
    private static Bitmap rotateImageIfRequired(ContentResolver contentResolver, Uri uri, Bitmap bitmap) throws IOException {
        InputStream inputStream = contentResolver.openInputStream(uri);
        ExifInterface exif = new ExifInterface(inputStream);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        inputStream.close();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateBitmap(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateBitmap(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateBitmap(bitmap, 270);
            default:
                return bitmap;
        }
    }

    /**
     * Rotate bitmap by specified degrees
     */
    private static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private static Bitmap resizeToFit(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        if (resizedBitmap != bitmap) {
            bitmap.recycle();
        }
        return resizedBitmap;
    }

    /**
     * Resize bitmap to square (for profile pictures)
     */
    private static Bitmap resizeToSquare(Bitmap bitmap, int size) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Crop to square first
        int cropSize = Math.min(width, height);
        int x = (width - cropSize) / 2;
        int y = (height - cropSize) / 2;
        
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x, y, cropSize, cropSize);
        if (croppedBitmap != bitmap) {
            bitmap.recycle();
        }

        // Then resize if needed
        if (cropSize > size) {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, size, size, true);
            if (resizedBitmap != croppedBitmap) {
                croppedBitmap.recycle();
            }
            return resizedBitmap;
        }

        return croppedBitmap;
    }

    /**
     * Compress bitmap to Base64 string
     */
    private static String compressBitmapToBase64(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Get compressed bitmap directly (for preview purposes)
     */
    public static Bitmap getCompressedBitmap(ContentResolver contentResolver, Uri uri, int maxWidth, int maxHeight) throws IOException {
        Bitmap bitmap = getBitmapFromUri(contentResolver, uri);
        bitmap = rotateImageIfRequired(contentResolver, uri, bitmap);
        return resizeToFit(bitmap, maxWidth, maxHeight);
    }
}
