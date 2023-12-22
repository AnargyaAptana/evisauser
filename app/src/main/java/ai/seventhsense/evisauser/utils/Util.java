package ai.seventhsense.evisauser.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.frame.Frame;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Utility class
 */
public class Util {

    private static RenderScript rs = null;
    private static ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private static Type.Builder yuvType, rgbaType;
    private static Allocation in, out;
    private static Bitmap bmpout;

    /**
     * Convert a bitmap to a byte array
     *
     * @param bitmap The bitmap
     * @return The byte array
     */
    public static synchronized byte[] bitMap2ByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArrayQr = stream.toByteArray();
        return byteArrayQr;
    }

    /**
     * Convert a byte array to a bitmap
     *
     * @param byteArray The byte array
     * @return The bitmap
     */
    public static synchronized Bitmap byteArray2Bitmap(byte[] byteArray) {
        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return bitmap;
    }

    /**
     * Convert an image to a bitmap
     * @param image The image
     * @return The bitmap
     */
    private static Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21Data = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21Data, 0, ySize);
        uBuffer.get(nv21Data, ySize, uSize);
        vBuffer.get(nv21Data, ySize + uSize, vSize);

        YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, outputStream);
        byte[] jpegData = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
    }

    /**
     * Convert a frame to a bitmap
     *
     * @param frame The frame
     * @return The bitmap
     */
    public static synchronized Bitmap frameToBitmap(Context context, @NonNull Frame frame, boolean isFrontCamera, int maxSide) {
        Bitmap result = null;
        if (frame.getDataClass() == byte[].class) {
            // If the frame data is in byte[] format (NV21 or YUV_420_888 format)
            byte[] data = frame.getData();
            int width = frame.getSize().getWidth();
            int height = frame.getSize().getHeight();

            // Convert the byte[] data to a Bitmap

            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            result = yuvImageToBitmap(context, yuvImage, width, height);
        } else if (frame.getDataClass() == Image.class) {
            // If the frame data is in Image format (JPEG or YUV_420_888 format)
            result = imageToBitmap((Image) frame.getData());
        }
        // Handle other frame data formats if necessary
        if(result != null) {
            int userRotation = frame.getRotationToUser();
            if (userRotation != 0) {
                result = rotateBitmap(result, userRotation - 360, isFrontCamera, maxSide);
            } else {
                result = rotateBitmap(result, 0, isFrontCamera, maxSide);
            }
        }
        return result;
    }

    /**
     * Rotate a bitmap
     * @param bitmap The bitmap
     * @param degrees The degrees to rotate
     * @param isFrontCamera Is the camera front facing
     * @param maxSide The long side of the bitmap
     * @return The rotated bitmap
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees, boolean isFrontCamera, int maxSide) {
        Matrix matrix = new Matrix();
        if(degrees != 0) {
            matrix.postRotate(degrees);
        }

        // Scale the bitmap to the maxSide size, but only if maxSide of bitmap is > maxSide
        if(Math.max(bitmap.getHeight(), bitmap.getWidth()) > maxSide) {
            float scale = Math.min((float) maxSide / bitmap.getWidth(), (float) maxSide / bitmap.getHeight());
            matrix.postScale(scale, scale);
        }

        // Since we are showing a mirrored preview, we need to flip the image horizontally
        if(isFrontCamera) {
            if(degrees == 0 || degrees == -180)
                matrix.preScale(-1, 1);
            else
                matrix.preScale(1, -1);
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static synchronized Bitmap yuvImageToBitmap(Context context, YuvImage yuvImage, int width, int height) {
        if(rs != null) {
            // Check if the bitmap size has changed
            if(bmpout.getWidth() != width || bmpout.getHeight() != height) {
                rs = null;
            }
        }
        if(rs == null) {
            rs = RenderScript.create(context);
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

            int yuvDatalength = width * height * 3 / 2;  // this is 12 bit per pixel
            in = Allocation.createSized(rs, Element.U8(rs), yuvDatalength);

            // create bitmap for output
            bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // create output allocation from bitmap
            out = Allocation.createFromBitmap(rs, bmpout);  // this simple !

            // set the script´s in-allocation, this has to be done only once
            yuvToRgbIntrinsic.setInput(in);
        }

        in.copyFrom(yuvImage.getYuvData());

        yuvToRgbIntrinsic.forEach(out);

        out.copyTo(bmpout);  // and of course, show the bitmap or whatever

        return bmpout;
    }

}

