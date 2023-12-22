package ai.seventhsense.evisauser.ui;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import ai.seventhsense.evisauser.utils.KeyValueStore;
import ai.seventhsense.evisauser.R;

/**
 * Helper class to save images to the gallery
 */
public class GalleryHelper {
    /**
     * Saves the SensePrint QR code to the gallery
     * @param context The context
     * @param bitmap The bitmap
     * @throws IOException If there is an error saving the image
     */
    public static Uri saveImageToGallery(Context context, Bitmap bitmap) throws IOException {
        OutputStream imageOutStream;

        Uri external = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        File file = new File(Environment.DIRECTORY_PICTURES, context.getString(R.string.app_name));

        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(System.currentTimeMillis()) + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,
                file.toString());

        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.WIDTH, bitmap.getWidth());
        values.put(MediaStore.MediaColumns.HEIGHT, bitmap.getHeight());
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri =
                context.getContentResolver().insert(external,
                        values);

        imageOutStream = context.getContentResolver().openOutputStream(uri);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream);
        imageOutStream.close();
        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);

        context.getContentResolver().update(uri, values, null, null);

        long id = ContentUris.parseId(uri);

        // If we don't have the bucket id, record the bucket id
        if (KeyValueStore.getInstance(context).getGalleryBucketId() == null) {
            String[] projection = new String[]{
                    MediaStore.Images.ImageColumns.BUCKET_ID,
            };

            final Cursor cursor = context.getContentResolver()
                    .query(external, projection, MediaStore.Images.Media._ID + "=?", new String[]{"" + id}, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
            // Put it in the image view
            if (cursor.moveToFirst()) {
                String bucketId = cursor.getString(0);
                KeyValueStore.getInstance(context).setGalleryBucketId(bucketId);
            }
        }


        return uri;
    }
}
