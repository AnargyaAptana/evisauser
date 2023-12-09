package ai.seventhsense.faceidentificationdemo.sdk;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import ai.seventhsense.faceidentificationdemo.BaseActivity;
import ai.seventhsense.faceidentificationdemo.MainActivity;
import ai.seventhsense.faceidentificationdemo.R;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.sensecryptsdk.ISenseCryptActivationActivity;
import ai.seventhsense.sensecryptsdk.SenseCryptActivationFailureReason;
import ai.seventhsense.sensecryptsdk.SenseCryptActivityStateManager;

/**
 * This activity is used to start the download of the SenseCrypt SDK models.
 * <p>
 * Importantly, it implements the {@link ISenseCryptActivationActivity} interface, which is used by the
 * SenseCrypt SDK to communicate with the activity.
 * <p>
 * This activity requires an internet connection.
 */
public class DownloadingModelActivity extends BaseActivity implements ISenseCryptActivationActivity {
    // All activities that use the SDK should have their own state manager
    private SenseCryptActivityStateManager stateManager = new SenseCryptActivityStateManager(this);

    // The progress bar to show the progress of the model download
    private ProgressBar progressBar;

    // The layout that contains the progress bar
    private ConstraintLayout layoutProgress;

    // The text view to show the progress of the model download
    // For example, "Downloading Face Detection Model"
    // Also, if there is an error, it will show the error message
    // using tvErrorTitle, tvErrorMessage
    private TextView tvProgress, tvErrorTitle, tvErrorMessage, tvProgressTitle;

    // The retry button to retry the model download
    private AppCompatButton btnRetry;


    // The layout that contains the error message
    private LinearLayout errorView;

    // The image view to show the error image
    private ImageView errorImage, ivProgress;

    // The tag for logging
    private static final String TAG = "DownloadingModelActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout for this activity based on the screen size
        computeWindowSizeClasses();

        // Assign values to the components in this activity
        setUpComponents();

        // Assign listeners to the components in this activity
        setUpListeners();
    }

    /**
     * Assign values to the components in this activity
     */
    private void setUpComponents() {

        progressBar = findViewById(R.id.progressBar);

        progressBar.setMax(100);

        tvProgress = findViewById(R.id.tvProgress);

        btnRetry = findViewById(R.id.btnRetry);

        errorView = findViewById(R.id.layoutEmpty);

        layoutProgress = findViewById(R.id.layoutProgress);

        errorImage = findViewById(R.id.emptyImage);

        tvErrorTitle = findViewById(R.id.tvEmptyTitle);

        tvErrorMessage = findViewById(R.id.tvEmptyMessage);

        tvProgressTitle = findViewById(R.id.tvProgressTitle);

        ivProgress = findViewById(R.id.ivProgress);

    }

    /**
     * Assign listeners to the components in this activity
     */
    private void setUpListeners() {
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activate(DownloadingModelActivity.this);
            }
        });
    }

    /**
     * To allow the user to exit the app by pressing the back button, we override the onBackPressed()
     * this allows the downloading thread to continue in the background
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Finish the activity if back is pressed
        finishAffinity();
    }

    /**
     * Called when the application starts a network action. Derived classes should override this
     * method to show an indeterminate progress dialog or similar.
     * <p>
     * Here we override this method to show a loading dialog which blocks the UI. This is important
     * as users should not call SDK methods while the SDK is communicating with the server.
     *
     * @param senseCryptNetworkAction The network action that is starting
     */
    @Override
    public void onNetworkActionStart(SenseCryptNetworkAction senseCryptNetworkAction) {
        // Hide the view that shows the error message
        hideErrorView();

        // Show the layout with the progress bars
        showProgressLayout();

        // Determine which type of network action is starting
        switch (senseCryptNetworkAction) {
            case ACTIVATE:
                // Initial activation, show a blocking loading dialog
                progressBar.setProgress(0);
                ivProgress.setImageDrawable(getResources().getDrawable(R.drawable.activating));
                tvProgressTitle.setText(R.string.download_activity_activating);
                tvProgress.setText(R.string.download_activity_activating_detail);
                break;
            case FACE_DETECTION_MODEL_DOWNLOAD:
                // Show the progress and set the text to indicate we are downloading the face detection model
                progressBar.setProgress(0);
                ivProgress.setImageDrawable(getResources().getDrawable(R.drawable.progress));
                tvProgressTitle.setText(R.string.download_activity_download_in_progress);
                tvProgress.setText(R.string.sensecrypt_fd_model);
                break;
            case FACE_RECOGNITION_MODEL_DOWNLOAD:
                // Show the progress and set the text to indicate we are downloading the face recognition model
                progressBar.setProgress(0);
                ivProgress.setImageDrawable(getResources().getDrawable(R.drawable.progress));
                tvProgressTitle.setText(R.string.download_activity_download_in_progress);
                tvProgress.setText(R.string.sensecrypt_fr_model);
                break;
        }

        Log.d("ISenseCryptActivationActivity", "SDK Network start finish");
    }

    /**
     * Called when the application ends a network action. Derived classes should override this
     * method to dismiss the indeterminate progress dialog or similar.
     * <p>
     * Here we override this method to hide the blocking loading dialog that was shown via
     * {@link #onNetworkActionStart(SenseCryptNetworkAction)}.
     *
     * @param senseCryptNetworkAction The network action that is ending
     */
    @Override
    public void onNetworkActionEnd(SenseCryptNetworkAction senseCryptNetworkAction) {
        // Hide the blocking loading dialog
        Log.d("ISenseCryptActivationActivity", "SDK Network end");
        hideLoadingDialog();
    }

    /**
     * Called when the application starts downloading a model. Derived classes should override this
     * method to show a determinate progress dialog or similar.
     * <p>
     * Here we override the method to show the progress bar and set the text to indicate which model
     * is being downloaded.
     *
     * @param senseCryptModelType The model that is being downloaded
     * @param i                   The progress of the download (0-100)
     */
    @Override
    public void onDownloadProgress(SenseCryptModelType senseCryptModelType, int i) {
        switch (senseCryptModelType) {
            case FACE_DETECTION:
                tvProgress.setText(R.string.sensecrypt_fd_model);
                progressBar.setProgress(i);
                break;
            case FACE_RECOGNITION:
                tvProgress.setText(R.string.sensecrypt_fr_model);
                progressBar.setProgress(i);
                break;
        }
    }

    /**
     * Called when the application encounters a network exception. Derived classes should override
     * this method to show an error dialog or similar. After the user clicks on a button to retry
     * the network action, the derived class should call the {@link #activate(Activity)} method.
     * <p>
     * Here we override this method to show the error layout with message and graphic and hide the
     * progress layout.
     *
     * @param throwable The exception that was thrown
     */
    @Override
    public void onNetworkException(Throwable throwable) {

        errorImage.setImageDrawable(getDrawable(R.drawable.connection));

        // Set the error heading
        tvErrorTitle.setText(R.string.network_error);

        // Set the error message
        tvErrorMessage.setText(R.string.network_error_detail);

        // This is so that the progress layout is not hidden too soon when a network exception occurs
        // Otherwise, the progress layout will be hidden immediately and use will see frequent
        // flashes of the error layout
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideProgressLayout();

                showErrorView();
            }
        }, 1000);
    }

    /**
     * Hide the layout with the progress bar
     */
    private void hideProgressLayout() {
        layoutProgress.setVisibility(View.GONE);
    }

    /**
     * Show the layout with the progress bar
     */
    private void showProgressLayout() {
        layoutProgress.setVisibility(View.VISIBLE);
    }

    /**
     * Hide the layout with the error message and graphic
     */
    private void hideErrorView() {
        errorView.setVisibility(View.GONE);
    }

    /**
     * Show the layout with the error message and graphic
     */
    private void showErrorView() {
        errorView.setVisibility(View.VISIBLE);
    }

    /**
     * Called when the application encounters an exception during the saving of SDK components.
     * Derived classes should override this method to show an error dialog or similar.
     * After the user clicks on a button to retry the operation, the derived class should call
     * the {@link #activate(Activity)} method.
     * <p>
     * Here we override this method to show the error layout with message and graphic and hide the
     * progress layout.
     *
     * @param throwable The exception that was thrown
     */
    @Override
    public void onFileSaveException(Throwable throwable) {
        // Hide the progress layout
        hideProgressLayout();

        // Set the error graphic
        errorImage.setImageDrawable(getDrawable(R.drawable.storage));

        // Set the error heading
        tvErrorTitle.setText(R.string.storage_err);

        // Set the error message
        tvErrorMessage.setText(R.string.storage_err_detail);

        // Show the error layout
        showErrorView();
    }

    /**
     * Called when the activation fails. Derived classes should override this method to show an
     * error message to the user. After the user clicks on a button to retry the activation process,
     * the derived class should call the {@link #activate(Activity)} method.
     * <p>
     * The default implementation automatically calls the {@link #activate(Activity)} method.
     * <p>
     * Here we override this method to show an error dialog with a message, and on dismissing
     * the dialog, we go back to the access key activity.
     *
     * @param reason The reason for the failure
     */
    @Override
    public void onActivationFailed(Activity activity, SenseCryptActivationFailureReason reason) {
        Log.d(TAG, "onActivationFailed: " + reason);
        hideLoadingDialog();
        // The key used for activation is invalid (doesn't exist) or has expired (customer's
        // subscription has expired)

        if (reason == SenseCryptActivationFailureReason.KEY_INVALID) {
            showDialog(R.string.invalid_key, R.string.invalid_key_detail, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAccessKeyActivity(activity);
                }
            }, R.color.colorError);
        } else {
            if (reason == SenseCryptActivationFailureReason.LICENSE_EXPIRED) {
                // The SDK license has expired

                // Set the error graphic
                errorImage.setImageDrawable(getDrawable(R.drawable.license_expired));

                // Set the error heading
                tvErrorTitle.setText(R.string.license_expired);

                // Set the error message
                tvErrorMessage.setText(R.string.please_contact_administrator);
            } else if (reason == SenseCryptActivationFailureReason.USER_LIMIT_EXCEEDED) {
                // The user limit has been exceeded

                // Set the error graphic
                errorImage.setImageDrawable(getDrawable(R.drawable.limit_exceed));

                // Set the error heading
                tvErrorTitle.setText(R.string.user_limit_exceeded);

                // Set the error message
                tvErrorMessage.setText(R.string.please_contact_administrator);
            } else if (reason == SenseCryptActivationFailureReason.TRANSACTION_FAILED) {
                // The transaction failed

                // Set the error graphic
                errorImage.setImageDrawable(getDrawable(R.drawable.sever_busy));

                // Set the error heading
                tvErrorTitle.setText(R.string.server_busy);

                // Set the error message
                tvErrorMessage.setText(R.string.server_busy_detail);
            } else {
                // The key is linked to another device. It must be deactivated on the other device
                showDialog(R.string.acess_key_in_use, R.string.access_key_previously_used_detail, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startAccessKeyActivity(activity);
                    }
                }, R.color.colorError);
            }

            // Delay switching layouts so they don't
            // change too soon
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideProgressLayout();

                    showErrorView();
                }
            }, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoadingDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // onResume() must be called on the state manager
        // so that it can handle the SDK state
        stateManager.onResume();
    }

    /**
     * Sets the layout for this activity based on the screen size
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
            setContentView(R.layout.activity_downloading_model);
        } else if ( KeyValueStore.getInstance(DownloadingModelActivity.this).getPortraitMode()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_sw_downloading_model);

        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_expand_downloading_model);
        }

    }
}