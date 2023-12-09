package ai.seventhsense.faceidentificationdemo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import ai.seventhsense.faceidentificationdemo.sdk.SdkBaseActivity;
import ai.seventhsense.faceidentificationdemo.ui.FaceRegionOverlay;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.faceidentificationdemo.utils.Util;
import ai.seventhsense.sensecryptsdk.FaceQuality;
import ai.seventhsense.sensecryptsdk.SenseCryptAsyncFunctionExecutor;
import ai.seventhsense.sensecryptsdk.SenseCryptDetectionResult;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;
import ai.seventhsense.sensecryptsdk.SensePrintMetadataRequest;
import ai.seventhsense.sensecryptsdk.SensePrintMetadataResult;
import ai.seventhsense.sensecryptsdk.exceptions.SenseCryptLivenessFailedException;
import ai.seventhsense.sensecryptsdk.exceptions.SenseCryptValidationFailedException;

import net.idrnd.facesdk.FaceException;

import java.util.ArrayList;

/**
 * This activity is used to scan the face of the user and
 * verify the scanned face against a SensePrint
 */
public class FaceScanActivity extends SdkBaseActivity {
    // The camera view
    private CameraView cvCamera;

    // The text view to show instructions to the user
    private TextView tvInstructions;

    // A flag to indicate if a face is detected. If a face is detected, we don't want to
    // suspend face detection
    private volatile boolean isDetected = false;

    // The button to go back to the previous screen
    private ImageView ivBack;

    // The button to switch between the front and back camera
    private ImageView ivSwitchCamera;

    // A lock to synchronize access to the face detection
    private final Object lock = new Object();

    // Initially the camera is facing the back
    static boolean isCameraFacingBack = false;

    // The unix timestamp when the face scan started and the first frame was captured
    private long faceScanStartTime = 0;

    // High quality face frames
    ArrayList<Bitmap> frames = new ArrayList<>();
    // Unix timestamps for the frames
    ArrayList<Long> timestamps = new ArrayList<>();
    // The Face Region Overlay
    FaceRegionOverlay overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view
        computeWindowSizeClasses();

        // Assign values to the components in this activity
        setUpComponents();

        // Assign listeners to the components in this activity
        setUpListener();

        // Assign a listener to the camera
        setUpCameraListener();

        // Request for camera permission
        setUpCameraPermission();
    }

    /**
     * This method is used to execute the primary function of this activity.
     * <p>
     * This method is called after the user takes a picture.
     *
     * @param frames The high quality face frames
     */
    private void execute(ArrayList<Bitmap> frames, ArrayList<Long> timestamps) {
        // We detect again, to get the thumbnail
        SenseCryptAsyncFunctionExecutor.Callback<SenseCryptDetectionResult> callback = new SenseCryptAsyncFunctionExecutor.Callback<SenseCryptDetectionResult>() {
            @Override
            public void onResult(SenseCryptDetectionResult detectionResult) {
                //if detection result is null , no face is detected
                if (detectionResult == null) {
                    // We could detect no faces, this should never be the case
                    // as we got called only after a face quality assessment
                    hideLoadingDialog();
                    isDetected = false;
                    tvInstructions.setText(getString(R.string.no_faces_detected));
                } else {
                    // We got called only after face quality assessment
                    // So, here we get the thumbnail for the largest face
                    // for the verification process
                    verification(frames, timestamps, Util.bitMap2ByteArray(detectionResult.getThumbnail()));
                }
            }

            @Override
            public void onError(Exception e) {
                isDetected = false;
                // If error occurs, we don't expect any errors
                // so we just log the error
                hideLoadingDialog();
                Log.e(TAG, "onError: ", e);
                showUnexpectedErrorDialog();
            }
        };

        // Start the detection process
        if(frames.size()>0){
            SenseCryptSdk.detect(FaceScanActivity.this, frames.get(frames.size()-1) , callback);
        }

    }

    /**
     * This method is used to set up the components in this activity.
     */
    private void setUpComponents() {
        cvCamera = findViewById(R.id.camera);

        cvCamera.setZoom(0);

        ivBack = findViewById(R.id.back);

        cvCamera.setLifecycleOwner(this);


        if(isCameraFacingBack) {
            cvCamera.toggleFacing();
        }

        tvInstructions = findViewById(R.id.tvInstruction);

        ivSwitchCamera = findViewById(R.id.ivSwitchCamera);

        overlay = findViewById(R.id.faceRegionOverlay);

// If a specific size is needed, then use the following code
//        cvCamera.setPictureSize(new SizeSelector() {
//            @Override
//            public List<Size> select(List<Size> source) {
//                // Receives a list of available sizes.
//                // Must return a list of acceptable sizes.
//                ArrayList<Size> toRet = new ArrayList<>();
//                int maxArea = 0;
//                Size biggestSize = null;
//                for(Size size: source){
//                    int area = size.getHeight() * size.getWidth();
//                    if(area > maxArea) {
//                        maxArea = area;
//                        biggestSize = size;
//                    }
//                }
//                toRet.add(source.get(3));
////                cvCamera.setMinimumWidth(biggestSize.getWidth());
////                cvCamera.setMinimumHeight(biggestSize.getHeight());
//                return toRet;
//            }
//        });

    }

    /**
     * This method is used to set up the camera listener.
     */
    private void setUpCameraListener() {
        cvCamera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                synchronized (lock) {
                    // Detect a face only if a currently detected face is not being processed
                    if (!isDetected) {
                        Bitmap img = Util.frameToBitmap(FaceScanActivity.this, frame, cvCamera.getFacing() == Facing.FRONT, Constants.FRAME_LONG_SIDE);
                        int ovalLongSide = overlay.getOvalLongSide(Constants.FRAME_LONG_SIDE);
                        FaceQuality faceQuality = SenseCryptSdk.getFaceQuality( img, cvCamera.getFacing() == Facing.FRONT, ovalLongSide);

                        // Show the user instructions based on the face quality

                        long unixTimestampSeconds = System.currentTimeMillis() / 1000;
                        if(faceScanStartTime == 0){
                            faceScanStartTime = unixTimestampSeconds;
                        } else if((unixTimestampSeconds - faceScanStartTime) > Constants.FACE_SCAN_TIMEOUT){

                            frames.clear();
                            timestamps.clear();

                            faceScanStartTime = 0;

                            return;
                        }

                        switch (faceQuality) {
                            case NO_FACE_DETECTED: {
                                tvInstructions.setText(R.string.face_quality_no_face_detected);
                                return;
                            }
                            case TOO_MANY_FACES_DETECTED: {
                                tvInstructions.setText(R.string.face_quality_too_many_faces);
                                return;
                            }
                            case YAW_TOO_LEFT: {
                                tvInstructions.setText(R.string.face_quality_yaw_too_left);
                                return;
                            }
                            case YAW_TOO_RIGHT: {
                                tvInstructions.setText(R.string.face_quality_yaw_too_right);
                                return;
                            }
                            case TOO_FAR: {
                                tvInstructions.setText(R.string.face_quality_too_far);
                                return;
                            }
                            case FACE_CLOSE_TO_EDGE: {
                                tvInstructions.setText(R.string.face_quality_center_face);
                                return;
                            }
                            case FACE_OCCULDED: {
                                tvInstructions.setText(R.string.face_quality_face_occluded);
                                return;
                            }
                            case PITCH_TOO_HIGH: {
                                tvInstructions.setText(R.string.face_quality_pitch_too_high);
                                return;
                            }
                            case PITCH_TOO_LOW: {
                                tvInstructions.setText(R.string.face_quality_pitch_too_low);
                                return;
                            }
                            case TOO_CLOSE: {
                                tvInstructions.setText(R.string.face_quality_too_close);
                                return;
                            }
                            case EYES_NOT_OPEN: {
                                tvInstructions.setText(R.string.face_quality_eyes_closed);
                                return;
                            }
                            case QUALITY_TOO_LOW: {
                                tvInstructions.setText(R.string.face_quality_quality_low);
                                return;
                            }
                            case QUALITY_GOOD: {
                                if(frames.size() <= Constants.NUMBER_OF_IMAGES_FOR_LIVENESS ){
                                    String message = getString(R.string.face_capture_message)+" "+(frames.size()+1)+" "+getString(R.string.face_capture_out_of_n, Constants.NUMBER_OF_IMAGES_FOR_LIVENESS);
                                    tvInstructions.setText(message);
                                } else if(frames.size() == Constants.NUMBER_OF_IMAGES_FOR_LIVENESS){
                                    tvInstructions.setText(R.string.face_scan_finished);
                                }
                                break;
                            }
                        }

                        // If the face quality is good, then we add the frame to the list of frames
                        frames.add(img);
                        timestamps.add(unixTimestampSeconds);
                        if(frames.size() == Constants.NUMBER_OF_IMAGES_FOR_LIVENESS){
                            // We have enough frames, so we can start the verification process
                            isDetected = true;

                            // Since we are not in the UI thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    tvInstructions.setText(R.string.face_scan_finished);

                                    showLoadingDialog(R.string.detection_face_in_progress);

                                }
                            });

                            // Start the verification process
                            execute(frames, timestamps );
                        }
                    }
                }
            }
        });
    }

    /**
     * This method is used to set up the listeners for the components in this activity.
     */
    private void setUpListener() {
        ivBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();

            }
        });

        ivSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cvCamera.getFacing() == Facing.FRONT) {
                    cvCamera.setFacing(Facing.BACK);
                    isCameraFacingBack = true;
                } else {
                    cvCamera.setFacing(Facing.FRONT);
                    isCameraFacingBack = false;
                }
            }
        });
    }

    /**
     * This method is used to request for camera permission.
     */
    private void setUpCameraPermission() {
        MultiplePermissionsListener snackbarMultiplePermissionsListener =
                SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
                        .with(cvCamera, R.string.permissions_needed)
                        .withOpenSettingsButton(R.string.settings)
                        .withCallback(new Snackbar.Callback() {
                            @Override
                            public void onShown(Snackbar snackbar) {
                                // Event handler for when the given Snackbar is visible
                            }

                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                // Event handler for when the given Snackbar has been dismissed
                            }
                        })
                        .build();

        Dexter.withContext(this)
                .withPermissions(android.Manifest.permission.CAMERA, Manifest.permission.INTERNET)
                .withListener(snackbarMultiplePermissionsListener).check();
    }


    /**
     * This method is used to verify the detected face against the SensePrint.
     */
    private void verification(ArrayList<Bitmap> frames, ArrayList<Long>  timestamps, byte[] thumbnail) {
        // At this point the busy dialog is already showing as it was shown
        // when face detection was started.

        // Get the intent to get the SensePrint
        Intent intent = getIntent();
        // Get the scanned data of the SensePrint from the intent
        byte[] sensePrint = intent.getByteArrayExtra(Constants.EXTRA_QR_CODE_DATA);

        // If there was a password, then get it from the intent
        String qrPass = getIntent().getStringExtra(Constants.EXTRA_PASSWORD);

        // Create a request to get the SensePrint metadata
        SensePrintMetadataRequest request;
        if (qrPass != null) {
            request = new SensePrintMetadataRequest(
                    frames, timestamps, sensePrint, qrPass);
        } else {
            request = new SensePrintMetadataRequest(
                    frames, timestamps, sensePrint, null);
        }

// If image needs to be saved to gallery, then use the following code
//        try {
//            Uri uri = GalleryHelper.saveImageToGallery(FaceScanActivity.this, cameraFrame);
//            Intent data = new Intent();
//            data.putExtra(Constants.EXTRA_IMAGE_URI, uri);
//            setResult(RESULT_OK, data);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }


        // Start the process to get metadata
        SenseCryptSdk.getSensePrintData(FaceScanActivity.this, request, new SenseCryptAsyncFunctionExecutor.Callback<SensePrintMetadataResult>() {
            @Override
            public void onResult(SensePrintMetadataResult result) {
                // With the result, we can hide the busy dialog
                hideLoadingDialog();

                // We have a valid result. Show the PersonDetailActivity
                Intent newIntent = new Intent(FaceScanActivity.this, PersonDetailActivity.class);
                Log.e("face scan verified ", "success");

                newIntent.putExtra(Constants.EXTRA_DATA, result.getMetadata());
                newIntent.putExtra(Constants.EXTRA_RECORD_ID, result.getRecordId());
                newIntent.putExtra(Constants.EXTRA_DETECTED_IMAGE, thumbnail);
                startActivity(newIntent);
                finish();
            }

            @Override
            public void onError(Exception error) {
                hideLoadingDialog();

                Log.e(TAG, "onError: ", error);

                if (error instanceof SenseCryptValidationFailedException) {
                    // Face or password did not match the SensePrint
                    if (qrPass != null) {
                        // We show a different error message if there was a password
                        // as either the face or the password could have been wrong
                        showErrorMessageDialog(R.string.senseprint_not_match_with_password);
                    } else {
                        // The face doesn't match the SensePrint
                        showErrorMessageDialog(R.string.senseprint_not_match_without_password);
                    }
                    return;
                } else if (error instanceof SenseCryptLivenessFailedException) {
                    // Save bitmap to gallery in error folder
                    // Liveness failed, this also means the face matched the SensePrint
                    // but the liveness check failed. So we want to let the user scan their face again.
                    showErrorMessageDialog(R.string.liveness_failed_title, R.string.liveness_failed, true);
                    return;
                } else if (error instanceof FaceException) {
                    FaceException.Status status = ((FaceException) error).getStatus();
                    switch (status) {
                        case FACE_CROPPED:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_cropped, true);
                            break;
                        case FACE_TOO_CLOSE:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_too_close, true);
                            break;
                        case FACE_CLOSE_TO_BORDER:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_too_close_border, true);
                            break;
                        case FACE_NOT_FOUND:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_not_found, true);
                            break;
                        case TOO_MANY_FACES:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_too_many_faces, true);
                            break;
                        case FACE_TOO_SMALL:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_too_small, true);
                            break;
                        case FACE_ANGLE_TOO_LARGE:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_angle_too_large, true);
                            break;
                        case FAILED_TO_READ_IMAGE:
                            showErrorMessageDialog(R.string.failed_to_read_image);
                            break;
                        case FAILED_TO_WRITE_IMAGE:
                            showErrorMessageDialog(R.string.failed_to_write_image);
                            break;
                        case FAILED_TO_READ_MODEL:
                            showErrorMessageDialog(R.string.failed_to_read_model);
                            break;
                        case FAILED_TO_BUILD_INTERPRETER:
                            showErrorMessageDialog(R.string.failed_to_build_inter);
                            break;
                        case FAILED_TO_INVOKE_INTERPRETER:
                            showErrorMessageDialog(R.string.failed_to_invoke_inter);
                            break;
                        case FAILED_TO_ALLOCATE:
                            showErrorMessageDialog(R.string.failed_to_allocate);
                            break;
                        case INVALID_CONFIG:
                            showErrorMessageDialog(R.string.invalid_config);
                            break;
                        case NO_SUCH_OBJECT_IN_BUILD:
                            showErrorMessageDialog(R.string.no_such_object_in_build);
                            break;
                        case FAILED_TO_PREPROCESS_IMAGE_WHILE_PREDICT:
                            showErrorMessageDialog(R.string.failed_to_preprocess_predict);
                            break;
                        case FAILED_TO_PREPROCESS_IMAGE_WHILE_DETECT:
                            showErrorMessageDialog(R.string.failed_to_preprocess_detect);
                            break;
                        case FAILED_TO_PREDICT_LANDMARKS:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.failed_to_predict_landmarks, true);
                            break;
                        case INVALID_FUSE_MODE:
                            showErrorMessageDialog(R.string.invalid_fuse_mode);
                            break;
                        case NULLPTR:
                            showErrorMessageDialog(R.string.nullptr);
                            break;
                        case LICENSE_ERROR:
                            showErrorMessageDialog(R.string.license_error);
                            break;
                        case INVALID_META:
                            showErrorMessageDialog(R.string.invalid_neta);
                            break;
                        case UNKNOWN:
                            showErrorMessageDialog(R.string.unknown);
                            break;
                        case OK:
                            showErrorMessageDialog(R.string.Ok);
                            break;
                        case FACE_IS_OCCLUDED:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.face_is_occluded, true);
                            break;
                        case FAILED_TO_FETCH_COREML_DECRYPTION_KEY:
                            showErrorMessageDialog(R.string.failed_to_fetch_decryption);
                            break;
                        case EYES_CLOSED:
                            showErrorMessageDialog(R.string.liveness_failed_title, R.string.eyes_closed, true);
                            break;
                    }
                    return;
                }

                // Otherwise we show an unexpected error message
                showUnexpectedErrorDialog();
                isDetected = false;
            }
        });
    }

    /**
     * This method is used to show an error message dialog.
     *
     * @param messageResourceId The resource id of the message to show
     */
    private void showErrorMessageDialog(int messageResourceId) {
        showErrorMessageDialog(R.string.verification_failed, messageResourceId, false);
    }

    /**
     * This method is used to show an error message dialog.
     *
     * @param titleResourceId   The resource id of the title to show
     * @param messageResourceId The resource id of the message to show
     * @param isRescanPossible  A flag to indicate if a rescan is possible
     */
    private void showErrorMessageDialog(int titleResourceId, int messageResourceId, boolean isRescanPossible) {
        showConfirmationDialog(titleResourceId, messageResourceId, R.string.retry,
                R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!isRescanPossible) {
                            // If rescan of face is not possible, then a retry means going back to the
                            // QR code scanning activity
                            Intent intent = new Intent(FaceScanActivity.this, QrScannerActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Otherwise, we set the state to not detected and show the instructions
                            isDetected = false;
                            frames.clear();
                            timestamps.clear();

                        }

                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigateToMain();
                    }
                });
    }

    /**
     * When the activity is resumed, we set the state to not detected
     */
    @Override
    protected void onResume() {
        super.onResume();
        isDetected = false;
    }

    /**
     * Show different layouts based on the screen size
     */
    private void computeWindowSizeClasses() {

        WindowMetrics metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(this);

        float widthDp = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            widthDp = metrics.getBounds().width() /
                    getResources().getDisplayMetrics().density;
        }


        if (widthDp < Constants.PHONE_WIDTH_DP) {
            setContentView(R.layout.activity_face_scan);
        } else if ( KeyValueStore.getInstance(FaceScanActivity.this).getPortraitMode()) {
            setContentView(R.layout.activity_sw_face_scan);
        }else{
            setContentView(R.layout.activity_expand_face_scan);
        }
    }
}