package ai.seventhsense.faceidentificationdemo.sdk;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import java.util.HashMap;

import ai.seventhsense.faceidentificationdemo.BaseActivity;
import ai.seventhsense.faceidentificationdemo.MainActivity;
import ai.seventhsense.faceidentificationdemo.R;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.sensecryptsdk.CheckKeyResult;
import ai.seventhsense.sensecryptsdk.ISenseCryptAccessKeyActivity;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;
import ai.seventhsense.sensecryptsdk.exceptions.SenseCryptAccessKeyNotUUIDException;

/**
 * This activity is used to start the SenseCrypt SDK activation process.
 * <p>
 * Importantly, it implements the {@link ISenseCryptAccessKeyActivity} interface, which is used by the
 * SenseCrypt SDK to communicate with the activity.
 * <p>
 * Activating the SDK requires an internet connection.
 */
public class AccessKeyActivity extends BaseActivity implements ISenseCryptAccessKeyActivity {

    // Field for the user to enter their Access Key
    private EditText etAccessKey;

    // TextView to show an error if any
    private TextView tvError;

    // Button to continue to the next screen
    private AppCompatButton btnContinue;

    // A handler for delayed tasks
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        computeWindowSizeClasses();

        // Assign values to the components in this activity
        setUpComponents();

        // Assign listeners to the components in this activity
        setUpListeners();
    }

    private void setUpComponents() {
        // Assign the field for the user to enter their Access Key
        etAccessKey = findViewById(R.id.etAccessKey);

        // Assign the button to continue to the next screen
        btnContinue = findViewById(R.id.btnContinue);

        // Assign the TextView to show an error if any
        tvError = findViewById(R.id.tvError);

        // Hide the error message initially
        tvError.setVisibility(View.INVISIBLE);
    }

    /**
     * Assign listeners to the components in this activity, to handle any user interaction
     */
    private void setUpListeners() {
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the access key entered by the user
                String accessKey = etAccessKey.getText().toString().trim();

                // Check if the access key is valid and show errors if any
                if (isAccessKeyValid(accessKey)) {
                    // Access key is valid, hide the error message
                    tvError.setVisibility(View.GONE);

                    // Activate the access key
                    try {
                        activate(AccessKeyActivity.this, accessKey);

                    } catch (SenseCryptAccessKeyNotUUIDException e) {
                        // This should not happen, as we have already validated the access key
                        showDialog(R.string.error, R.string.invalid_key);
                    }
                }
            }
        });
    }

    /**
     * Validate that the access key is a valid 36 character UUID
     * @param accessKey The access key to validate
     * @return true if the access key is valid, false otherwise
     */
    private boolean isAccessKeyValid(String accessKey) {
        if (accessKey.length() == 0 && !accessKey.equals(R.string.access_key)) {
            showError(R.string.access_key_is_re);
        } else if (accessKey.length() < 36) {
            showError(R.string.access_key_short);

        } else if (accessKey.length() > 36) {
            showError(R.string.access_key_long);
        } else if(!SenseCryptSdk.isAccessKeyValidUUID(accessKey)) {
            showError(R.string.access_key_invalid);
        } else {
            return true;
        }

        return false;
    }

    /**
     * Show an error to the user
     * @param message The error message to show
     */
    private void showError(int message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> {
            tvError.setVisibility(View.INVISIBLE);
        }, 5000);
    }


    /**
     * UI hook to show an error to the user if the Access Key check failed
     *
     * @param reason The reason why the Access Key check failed
     */
    @Override
    public void onCheckKeyWithServerFailed(Activity activity, CheckKeyResult reason) {
        switch (reason) {
            case KEY_INVALID:
                showError(R.string.access_key_invalid);
                break;
            case KEY_LINKED_TO_OTHER_DEVICE:
                showDialog(R.string.acess_key_in_use, R.string.access_key_previously_used_detail);
                break;
            case LICENSE_EXPIRED:
                showDialog(R.string.license_expired, R.string.license_expired_detail);
                break;
        }
    }

    /**
     * UI hook to show an indeterminate progress indicator to the user.
     * <p>
     * Called when we begin to check the access key provided with the server
     * Implementing classes should implement this method to show an indeterminate progress indicator to the user.
     * <p>
     * Here we override this method to show a loading dialog which blocks the UI. This is important
     * as users should not call SDK methods while the network call is in progress.
     */
    @Override
    public void onCheckAccessKeyWithServerStart() {
        showLoadingDialog(R.string.checking_access_key);
    }

    /**
     * UI hook to hide the indeterminate progress indicator.
     * <p>
     * Called when the access key check with the server is complete.
     * Implementing classes should implement this method to hide the indeterminate progress indicator.
     * <p>
     * Here we override this method to hide the blocking loading dialog that was shown via
     * {@link #onCheckAccessKeyWithServerStart()}.
     */
    @Override
    public void onCheckAccessKeyWithServerEnd() {
        hideLoadingDialog();
    }

    /**
     * Called when a network exception occurs while checking the access key
     * Implementing classes should implement this method to show an error message to the user.
     * <p>
     * Here we override this method to show an error dialog to the user.
     */
    @Override
    public void onCheckAccessKeyWithServerNetworkException(Throwable throwable) {
        showDialog(R.string.network_error, R.string.checking_access_key_network_error);
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
            setContentView(R.layout.activity_access_key);
        } else if (KeyValueStore.getInstance(AccessKeyActivity.this).getPortraitMode()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_sw_access_key);

        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_expand_key);
        }

    }
}