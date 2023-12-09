package ai.seventhsense.faceidentificationdemo.utils;

/**
 * Constants class
 */
public class Constants {
    // Intent extra names
    public static final String EXTRA_QR_CODE_DATA = "EXTRA_QR_CODE_DATA";

    public static final String EXTRA_DETECTED_IMAGE = "EXTRA_DETECTED_IMAGE";
    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    public static final String EXTRA_RECORD_ID = "EXTRA_RECORD_ID";

    public static final String META_DATA_NAME = "Name";

    public static final String META_DATA_ID = "Id";

    // Images from the camera, will be resized to have a long side of 1920
    // while maintaining the aspect ratio
    public static final int FRAME_LONG_SIDE = 1920;

    // Number of images to be taken for livenFess detection
    public static final int NUMBER_OF_IMAGES_FOR_LIVENESS = 3;

    // Face Scan Timeout
    public static final int FACE_SCAN_TIMEOUT = 30;

    // Intent key for record id extra


    // The width of the phone in dp
    public static final float PHONE_WIDTH_DP = 600f;

}


