package ai.seventhsense.faceidentificationdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import ai.seventhsense.faceidentificationdemo.sdk.SdkBaseActivity;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;

/**
 * The main activity of the app
 * <p>
 * This activity shows the main screen of the app, with buttons to generate a QR code, or scan a QR code
 * or to go to the settings screen
 */
public class MainActivity extends SdkBaseActivity {
    // UI Components
    CardView btnSmartScan;
    CardView btnGenerateQr;
    AppCompatImageButton btnSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        // Set up the window layout
        computeWindowSizeClasses();

        // Set up the UI components
        setUpComponents();

        // Set up the listeners
        setUpListener();
    }

    /**
     * Sets up the listeners for the UI components
     */
    private void setUpListener() {
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSetting.setClickable(false);

                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        btnSmartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(MainActivity.this, QrScannerActivity.class);
                startActivity(intent);

                btnSmartScan.setClickable(false);
                btnSmartScan.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btnSmartScan.setClickable(true);

                    }
                }, 500);


            }
        });

        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                navigateToGenerateQr();

                btnGenerateQr.setClickable(false);

                // Delay the clickability of the button to prevent multiple clicks
                btnGenerateQr.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btnGenerateQr.setClickable(true);

                    }
                }, 500);


            }
        });
    }

    /**
     * Sets up the UI components
     */
    private void setUpComponents() {
        btnSmartScan = findViewById(R.id.btnSmartScan);

        btnGenerateQr = findViewById(R.id.btnGenerateQr);

        btnSetting = findViewById(R.id.btnSetting);
    }

    /**
     * Navigates to the Generate QR activity
     */
    private void navigateToGenerateQr() {
        Intent intent = new Intent(MainActivity.this, FaceCaptureActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();

        btnSmartScan.setClickable(true);

        btnGenerateQr.setClickable(true);

        btnSetting.setClickable(true);

        // Pre-load the SDK, if lazy loading is desired, this can be removed
        SenseCryptSdk.initialize(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);

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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_main);
        } else if (KeyValueStore.getInstance(MainActivity.this).getPortraitMode()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_sw_main);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_expand_main);
        }
    }
}