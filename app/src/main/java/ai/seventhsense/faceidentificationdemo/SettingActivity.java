package ai.seventhsense.faceidentificationdemo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import ai.seventhsense.faceidentificationdemo.sdk.SdkBaseActivity;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.sensecryptsdk.SenseCryptAsyncFunctionExecutor;
import ai.seventhsense.sensecryptsdk.SenseCryptLicenseInformation;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;

/**
 * This activity shows the settings screen which includes the access key and the expiration date
 * of the license and a button to deactivate the license.
 * <p>
 * The access key can be copied to the clipboard by clicking on the copy button.
 * <p>
 * The license can be deactivated by clicking on the deactivate button.
 * <p>
 * This activity requires an internet connection to deactivate the license.
 */
public class SettingActivity extends SdkBaseActivity {
    // UI Components

    AppCompatImageButton btnCopy, btnDeactivate;

    AppCompatImageView ivBack;
    TextView tvAccessKeyMessage, tvExpiration;

    CardView llDeactivation;

    TextView btnPortrait, btnLandscape;

    boolean isTabletMode = false;

    // Callback to get the license information
    SenseCryptAsyncFunctionExecutor.Callback<SenseCryptLicenseInformation> licenseInformationCallback = new SenseCryptAsyncFunctionExecutor.Callback<SenseCryptLicenseInformation>() {

        @Override
        public void onResult(SenseCryptLicenseInformation senseCryptLicenseInformation) {


            SpannableString content = new SpannableString(senseCryptLicenseInformation.getAccessKey());
            content = new SpannableString(senseCryptLicenseInformation.getAccessKey());
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            tvAccessKeyMessage.setText(content);

            // Set the desired locale for the date-time format
            Locale defaultLocale = Locale.getDefault();

            // Create a DateTimeFormatter for the default locale, using the default date and time format styles
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(defaultLocale);

            // Format the LocalDateTime object to a string using the default locale
            String formattedDateTime = senseCryptLicenseInformation.getExpiryDate().format(formatter);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvExpiration.setText(formattedDateTime);
                }
            });


        }

        @Override
        public void onError(Exception e) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //computeWindowSizeClasses();
        super.onCreate(savedInstanceState);
        //setContentView(deviceLayoutId);

        computeWindowSizeClasses();

        // Set up the UI components
        setUpUIComponents();

        // Get the license information
        SenseCryptSdk.getLicenseInformation(SettingActivity.this, licenseInformationCallback); //get license information

        // Set up the listeners
        setUpListener();
    }


    /**
     * Sets up the UI components
     */
    private void setUpUIComponents() {
        btnCopy = findViewById(R.id.btnCopy);

        btnDeactivate = findViewById(R.id.btnSetting);

        ivBack = findViewById(R.id.ivBack);

        tvAccessKeyMessage = findViewById(R.id.tvAccessKeyMessage);

        tvExpiration = findViewById(R.id.tvExpiration);

        btnPortrait = findViewById(R.id.btnPortrait);

        btnLandscape = findViewById(R.id.btnLandscape);

        llDeactivation = findViewById(R.id.llDeactivation);

        if (!SenseCryptSdk.isAccessKeySetForActivation(SettingActivity.this)) {
            showDeactivation();
        }


        if (KeyValueStore.getInstance(SettingActivity.this).getPortraitMode()) {
            setUpButtonForPortraitMode();
        } else {
            setUpButtonForLandscapeMode();
        }


    }

    /**
     * Sets up the listeners for the UI components
     */
    private void setUpListener() {
        btnCopy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            // Create a ClipData object to hold the copied text
            ClipData clip = ClipData.newPlainText("Copied Text", tvAccessKeyMessage.getText());

            // Set access key to the clipboard
            clipboard.setPrimaryClip(clip);
        });


        btnDeactivate.setOnClickListener(view -> {
            showConfirmationDialog(R.string.deactivate, R.string.deactivate_detail, R.string.yes, R.string.no, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    KeyValueStore.getInstance(SettingActivity.this).resetPortraitMode();
                    deactivate(SettingActivity.this);

                }
            });
        });


        ivBack.setOnClickListener(view -> {

            backToMainActivity();


        });

        btnLandscape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setUpLandscapeMode();

            }
        });

        btnPortrait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setUpPortraitMode();

            }
        });
    }

    // Deactivation option is available only if the access key is not preset in Application.java
    private void showDeactivation() {
        llDeactivation.setVisibility(View.VISIBLE);
    }

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
            setContentView(R.layout.activity_setting);
        } else if (KeyValueStore.getInstance(SettingActivity.this).getPortraitMode()) {
            isTabletMode = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


            setContentView(R.layout.activity_sw_setting);

        }else{
            isTabletMode = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            setContentView(R.layout.activity_expand_setting);
        }
    }

    // Lock to portrait mode
    private void setUpPortraitMode() {


        setUpButtonForPortraitMode();


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        KeyValueStore.getInstance(SettingActivity.this).setScreenMode(true);


    }

    // The design for the portrait mode button
    private void setUpButtonForPortraitMode() {
        btnPortrait.setBackground(getDrawable(R.drawable.portrait_screen_background));
        btnPortrait.setTextColor(getColor(R.color.colorWhite));
        btnLandscape.setBackground(getDrawable(R.drawable.screen_rotate_background));
        btnLandscape.setTextColor(getColor(R.color.screen_rotate_text));
    }

    // The design for the landscape mode button
    private void setUpButtonForLandscapeMode() {
        btnLandscape.setBackground(getDrawable(R.drawable.portrait_screen_background));
        btnLandscape.setTextColor(getColor(R.color.colorWhite));
        btnPortrait.setBackground(getDrawable(R.drawable.screen_rotate_background));
        btnPortrait.setTextColor(getColor(R.color.screen_rotate_text));
    }

    // Lock to landscape mode
    private void setUpLandscapeMode() {

        setUpButtonForLandscapeMode();


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        KeyValueStore.getInstance(SettingActivity.this).setScreenMode(false);

    }

    private void backToMainActivity(){
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        backToMainActivity();
    }

    @Override
    protected void onResume() {
//        computeWindowSizeCzlasses();
        super.onResume();

    }
}