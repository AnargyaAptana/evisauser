package ai.seventhsense.faceidentificationdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Facing;

import ai.seventhsense.faceidentificationdemo.sdk.SdkBaseActivity;
import ai.seventhsense.faceidentificationdemo.ui.FaceRegionOverlay;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.faceidentificationdemo.utils.Util;
import ai.seventhsense.sensecryptsdk.FaceQuality;
import ai.seventhsense.sensecryptsdk.SenseCryptAsyncFunctionExecutor;
import ai.seventhsense.sensecryptsdk.SenseCryptDetectionResult;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * This activity is used to capture face of the user and pass the scanned
 * face to the next activity to generate a QR code.
 */
public class FaceCaptureActivity extends SdkBaseActivity {
    // The camera view
    private CameraView cvCamera;

    // The text view to show instructions to the user
    private TextView tvInstructions;

    // The button to switch between front and back camera
    private CircleImageView ivSwitchCamera;

    // The seek bar to control the zoom
    private SeekBar sbZoom;

    // The button to take a picture
    private AppCompatImageButton btnTakePicture;

    // The button to go back to the previous screen
    private ImageView ivBack;

    // The Face Region Overlay
    private FaceRegionOverlay faceRegionOverlay;

    private static final String TAG = "FaceCaptureActivity";

    // The root layout of this activity
    private RelativeLayout faceCaptureRootLayout;

    // Flag to indicate if the camera is facing back
    static boolean isCameraFacingBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        computeWindowSizeClasses();

        // Assign values to the components in this activity
        setUpComponents();

        // Set up handling of zom
        setupZoom();

        // Assign listeners to the components in this activity
        setUpListener();

        // Request for camera permission
        setUpCameraPermission();
    }


    /**
     * This method is used to execute the primary function of this activity.
     * <p>
     * This method is called after the user takes a picture.
     *
     * @param result The picture taken by the user
     */
    private void execute(PictureResult result) {
        // First get the bitmap from the PictureResult
        result.toBitmap(new BitmapCallback() {
            @SuppressLint("WrongThread")
            @Override
            public void onBitmapReady(@Nullable Bitmap bitmap) {
                // Need to scale the bitmap to specified long side
                Bitmap scaled = Util.rotateBitmap(bitmap, 0, cvCamera.getFacing() == Facing.FRONT, Constants.FRAME_LONG_SIDE);
                Log.d(TAG, "onBitmapReady: " + scaled.getWidth() + " " + scaled.getHeight());
                // Now we have the bitmap, we can use the SenseCrypt SDK to detect the face
                SenseCryptAsyncFunctionExecutor.Callback<SenseCryptDetectionResult> callback = new SenseCryptAsyncFunctionExecutor.Callback<SenseCryptDetectionResult>() {
                    @Override
                    public void onResult(SenseCryptDetectionResult detectionResult) {
                        //if detection result is null , no face is detected
                        if (detectionResult == null) {
                            hideLoadingDialog();
                            tvInstructions.setText(getString(R.string.no_faces_detected));

                        } else if (detectionResult.getDetections().length == 1) {
                            // Use Oval's long side to determine size of the face
                            int ovalLongSide = faceRegionOverlay.getOvalLongSide(Constants.FRAME_LONG_SIDE);

                            FaceQuality quality = SenseCryptSdk.getFaceQuality(scaled, cvCamera.getFacing() == Facing.FRONT, ovalLongSide);

                            Log.d(TAG, "onResult: " + quality);
                            if (quality != FaceQuality.QUALITY_GOOD) {
                                // If face quality is not good, we show the error message
                                hideLoadingDialog();
                                switch (quality) {
                                    case FACE_OCCULDED: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_occlusion, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case FACE_CLOSE_TO_EDGE: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_not_centered, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case TOO_CLOSE: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_too_close, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case TOO_FAR: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_too_far, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case PITCH_TOO_HIGH:
                                    case PITCH_TOO_LOW:
                                    case YAW_TOO_LEFT:
                                    case YAW_TOO_RIGHT: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_pitch_yaw, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case EYES_NOT_OPEN: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_eyes_not_open, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case QUALITY_TOO_LOW: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_quality_too_low, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case NO_FACE_DETECTED: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_no_face_detected, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                    case TOO_MANY_FACES_DETECTED: {
                                        showSnackBar(faceCaptureRootLayout, R.string.unsuitable_capture_too_many_faces_detected, R.color.colorErrorSnackbar, 0, 120);
                                        break;
                                    }
                                }
                                return;
                            }

                            hideLoadingDialog();

                            // If requested, we can generate the QR code
                            // To generate a QR code, we can use the thumbnail
                            byte[] imgData = Util.bitMap2ByteArray(detectionResult.getThumbnail());
                            Intent newIntent = new Intent(FaceCaptureActivity.this, GenerateQrMetaDataActivity.class);
                            newIntent.putExtra(Constants.EXTRA_DETECTED_IMAGE, imgData);
                            startActivity(newIntent);
                        } else if (detectionResult.getDetections().length > 1) {
                            //if more than one face is detected
                            hideLoadingDialog();
                            tvInstructions.setText(R.string.multiple_faces_included);
                        } else {
                            //if no face is detected
                            hideLoadingDialog();
                            tvInstructions.setText(getString(R.string.no_faces_detected));
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        // If error occurs, we don't expect any errors
                        // so we just log the error
                        hideLoadingDialog();
                        Log.e(TAG, "onError: ", e);
                        showUnexpectedErrorDialog();
                    }
                };

                // Start the detection process
                SenseCryptSdk.detect(FaceCaptureActivity.this, bitmap, callback);
            }
        });
    }

    /**
     * This method is used to set up the components in this activity.
     */
    private void setUpComponents() {
        cvCamera = findViewById(R.id.camera);

        ivSwitchCamera = findViewById(R.id.ivSwitchCamera);

        cvCamera.setZoom(0);

        sbZoom = findViewById(R.id.sbZoom);

        btnTakePicture = findViewById(R.id.ibTakePicture);

        ivBack = findViewById(R.id.back);

        faceCaptureRootLayout = findViewById(R.id.faceCaptureRootLayout);

        cvCamera.setLifecycleOwner(this);

        // We want the snapshot size to match the preview size
        cvCamera.setSnapshotMaxWidth(cvCamera.getWidth());
        cvCamera.setSnapshotMaxHeight(cvCamera.getHeight());

        tvInstructions = findViewById(R.id.tvInstruction);

        faceRegionOverlay = findViewById(R.id.faceRegionOverlay);

        if(isCameraFacingBack) {
            cvCamera.toggleFacing();
        }
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
            public void onClick(View view) {
                cvCamera.setZoom(0);
                sbZoom.setProgress(0);
                if (cvCamera.getFacing() == Facing.FRONT) {
                    cvCamera.setFacing(Facing.BACK);
                    isCameraFacingBack = true;
                } else {
                    cvCamera.setFacing(Facing.FRONT);
                    isCameraFacingBack = false;
                }
            }
        });
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog(R.string.detection_face_in_progress);
                cvCamera.takePictureSnapshot();
            }
        });
        cvCamera.addCameraListener(new CameraListener() {
            @Override
            public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
                super.onZoomChanged(newValue, bounds, fingers);
                int percentage = (int) (100f * newValue);
                sbZoom.setProgress(percentage);
            }

            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);
                execute(result);
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
     * This method is used to set up the zoom.
     */
    private void setupZoom() {
        final int min = 0, max = 100, current = 0;
        sbZoom.setProgress(max - min);
        sbZoom.setProgress(current - min);
        sbZoom.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        sbZoom.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        sbZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float fraction = (float) progress / (float) max;
                cvCamera.setZoom(fraction);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        cvCamera.setZoom((float) current / (float) max);
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
            setContentView(R.layout.activity_face_capture);
        } else if ( KeyValueStore.getInstance(FaceCaptureActivity.this).getPortraitMode()) {
            setContentView(R.layout.activity_sw_face_capture);
        }else{
            setContentView(R.layout.activity_expand_face_capture);
        }
    }
}