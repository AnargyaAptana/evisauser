package ai.seventhsense.faceidentificationdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.zxingcpp.BarcodeReader;

import ai.seventhsense.faceidentificationdemo.sdk.SdkBaseActivity;
import ai.seventhsense.faceidentificationdemo.ui.QrScannerView;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.faceidentificationdemo.utils.Util;
import ai.seventhsense.sensecryptsdk.SenseCryptAsyncFunctionExecutor;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;
import ai.seventhsense.sensecryptsdk.SensePrintTypeResult;
import de.hdodenhof.circleimageview.CircleImageView;

public class QrScannerActivity extends SdkBaseActivity {

    // UI Components
    private BarcodeReader barcodeReader;
    TextView tvQrInfo, tvQrInstruction;

    String qrPassword;

    CameraView cvCamera;

    ImageView back;

    RelativeLayout llQrScanner;

    // Flag to indicate if a function is pending and is presently being executed
    boolean isFunctionPending;

    CircleImageView ivSwitchCamera;

    QrScannerView qrScannerView;

    // Initially the camera is facing the back
    static boolean isCameraFacingBack = false;


    /**
     * Callback to get the SensePrint type
     */
    SenseCryptAsyncFunctionExecutor.Callback<SensePrintTypeResult> callback = new SenseCryptAsyncFunctionExecutor.Callback<SensePrintTypeResult>() {

        @Override
        public void onResult(SensePrintTypeResult sensePrintTypeResult) {

            showLoadingDialog(R.string.scanning_qr);

            // The SensePrint for which the type has been determined
            byte[] sensePrint = sensePrintTypeResult.getSensePrint();

            SenseCryptSdk.SensePrintType type = sensePrintTypeResult.getSensePrintType();

            if (type == SenseCryptSdk.SensePrintType.NONE) {
                // Could not determine the SensePrint type
                // this could be because the SensePrint is invalid
                // or because the SensePrint is for another
                // customer's license. SensePrint's for one
                // customer's license cannot be used with another
                // customer's license.

                // Hide the loading dialog
                hideLoadingDialog();

                // Show a snackbar to indicate that the QR code is invalid
                //showQrErrorSnackBar();
                showSnackBar(llQrScanner, R.string.invalid_qr, R.color.colorErrorSnackbar, 2000);

                isFunctionPending = false;

                return;
            } else if (type == SenseCryptSdk.SensePrintType.SENSE_PRINT_WITH_PASSWORD) {
                // Hide the loading dialog
                hideLoadingDialog();

                // Stop the camera


                // Ask the user to enter the password
                showQrPasswordDialog(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tvQrInfo.setText(R.string.scanning_qr);
                        Intent intent = new Intent(QrScannerActivity.this, FaceScanActivity.class);
                        intent.putExtra(Constants.EXTRA_QR_CODE_DATA, sensePrint);
                        intent.putExtra(Constants.EXTRA_PASSWORD, qrPassword);
                        startActivity(intent);
                        finish();
                    }
                });
                return;
            }

            // If the SensePrint is of type SENSE_PRINT_WITHOUT_PASSWORD, then
            if (sensePrint != null) {
                // Hide the loading dialog
                hideLoadingDialog();

                // SensePrint is structurally valid, so navigate to the FaceScanActivity to
                // acquire the face image and then verify the SensePrint
                tvQrInfo.setText(R.string.scanning_qr);
                cvCamera.stopVideo();
                Intent intent = new Intent(QrScannerActivity.this, FaceScanActivity.class);
                intent.putExtra(Constants.EXTRA_QR_CODE_DATA, sensePrint);
                startActivity(intent);
                finish();


            }
        }

        @Override
        public void onError(Exception e) {
            // We set this flag to false to indicate that the function is no longer pending
            isFunctionPending = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        barcodeReader = new BarcodeReader();


        setContentView(R.layout.activity_qr_scanner);

        // Set up the UI components
        setUpUIComponents();

        // Set up listeners for the UI components
        setUpListener();

        // Ask for camera permission
        askCameraPermission();
    }


    /**
     * Sets up the UI components
     */
    private void setUpUIComponents() {
        tvQrInfo = findViewById(R.id.tvQrInfo2);

        cvCamera = findViewById(R.id.qrScanner);

        llQrScanner = findViewById(R.id.llQrScanner);

        tvQrInfo.setText(R.string.place_qr_insdie_frame);

        tvQrInstruction = findViewById(R.id.tvQrInstruction);

        back = findViewById(R.id.back);

        ivSwitchCamera = findViewById(R.id.ivSwitchCamera);

        cvCamera.setLifecycleOwner(QrScannerActivity.this);

        qrScannerView = findViewById(R.id.qrScannerView);

        if (isCameraFacingBack) {
            cvCamera.toggleFacing();
        }

    }

    /**
     * Sets up the listeners for the UI components
     */
    private void setUpListener() {
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });

        cvCamera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                Bitmap img = Util.frameToBitmap(QrScannerActivity.this, frame, cvCamera.getFacing() == Facing.FRONT, Constants.FRAME_LONG_SIDE);

                try {
                    // Scan in the whole image
                    Rect roi = new Rect(0, 0, img.getWidth(), img.getHeight());
                    BarcodeReader.Result result = barcodeReader.read(img, roi, 0);
                    if (result != null) {
                        if (isFunctionPending) {
                            return;
                        }

                        // If the QR code is not of length 200, then return
                        // SensePrints are of length 200 or more
                        if (result.getBytes().length < 200) {
                            hideLoadingDialog();
                            showSnackBar(llQrScanner, R.string.invalid_qr, R.color.colorErrorSnackbar, 2000);
                            return;  //sense-print length should be equal to 200 bytes, otherwise invalid qr
                        }


                        isFunctionPending = true; //set true to qr scan function pending

                        SenseCryptSdk.getSensePrintType(QrScannerActivity.this, result.getBytes(), callback); // sense print type callback will be returned by get senseprint type

                    }

                } catch (Exception ex) {
                    Log.d("Camera failure in qr scanner", ex.getMessage());
                }

            }
        });

        ivSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Switch the camera
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
     * Ask for camera permission
     */
    private void askCameraPermission() {
        MultiplePermissionsListener snackbarMultiplePermissionsListener = SnackbarOnAnyDeniedMultiplePermissionsListener.Builder.with(tvQrInfo, R.string.camera_permission).withOpenSettingsButton(R.string.settings).withCallback(new Snackbar.Callback() {
            @Override
            public void onShown(Snackbar snackbar) {
                // Event handler for when the given Snackbar is visible
            }

            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                // Event handler for when the given Snackbar has been dismissed
            }
        }).build();

        Dexter.withContext(this)
                .withPermissions(android.Manifest.permission.CAMERA, android.Manifest.permission.INTERNET)
                .withListener(snackbarMultiplePermissionsListener).check();
    }


    /**
     * When a user navigates away from the app and then comes back, the camera is started again.
     */
    @Override
    protected void onResume() {
        super.onResume();

    }

    /**
     * When the back button is pressed, the user is navigated to the main activity.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToMain();

    }


    /**
     * Ask the user to enter password if it is required to scan qr
     */
    public void showQrPasswordDialog(View.OnClickListener continueListener) {

        Dialog dialog = new Dialog(this);
        AppCompatButton btnYes;
        EditText password;
        TextView tvError;
        WindowMetrics metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(this);

        float widthDp = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            widthDp = metrics.getBounds().width() /
                    getResources().getDisplayMetrics().density;
        }




        if (widthDp < Constants.PHONE_WIDTH_DP) {
            dialog.setContentView(R.layout.layout_dialog_with_password);
        } else if (widthDp < 840f && KeyValueStore.getInstance(QrScannerActivity.this).getPortraitMode()) {
            dialog.setContentView(R.layout.layout_sw_dialog_with_password);

        } else {
            dialog.setContentView(R.layout.layout_expand_dialog_with_password);
        }

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); //change mathch
        window.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.dimAmount = 0.7f;
        lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;
        btnYes = dialog.findViewById(R.id.btnConfirm);
        password = dialog.findViewById(R.id.etPassword);
        tvError = dialog.findViewById(R.id.tvError);


        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (password.length() != 0 && !password.getText().toString().equals(R.string.key_password)) {
                    dialog.dismiss();
                    qrPassword = password.getText().toString();
                    continueListener.onClick(v);
                } else {
                    tvError.setVisibility(View.VISIBLE); //show error field if password is null
                }
            }
        });
        dialog.show();

    }


}