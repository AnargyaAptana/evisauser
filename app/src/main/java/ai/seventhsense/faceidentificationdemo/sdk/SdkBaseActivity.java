package ai.seventhsense.faceidentificationdemo.sdk;


import android.app.Activity;
import android.content.Context;
import android.view.View;

import ai.seventhsense.faceidentificationdemo.BaseActivity;
import ai.seventhsense.faceidentificationdemo.R;
import ai.seventhsense.sensecryptsdk.ISenseCryptSdkActivity;
import ai.seventhsense.sensecryptsdk.SenseCryptActivityStateManager;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;

/**
 * The base activity for all activities that use the SDK.
 */
public class SdkBaseActivity extends BaseActivity implements ISenseCryptSdkActivity {

    // The state manager is used to manage the state of the SDK, every SDK activity
    // should have its own state manager. Therefore we declare it in this activity
    // as all activities that use the SDK will be extending this activity.
    private SenseCryptActivityStateManager stateManager = new SenseCryptActivityStateManager(this);


    /**
     * UI hook to show an indicator that the SDK is loading.
     * <p>
     * Model loading to memory can take some time, so it is recommended to show an indicator
     * to the user that the SDK is loading.
     * <p>
     * Developers should override this method to show an indeterminate progress indicator.
     * Once the SDK is initialized, the {@link #onSdkLoadingEnd(boolean)} method will be called, and
     * the indicator should be hidden.
     *
     * Here we override this method to show a loading dialog which blocks the UI. This is important
     * as users should not call SDK methods while the SDK loading has started.
     */
    @Override
    public void onSdkLoadingStart() {
        showLoadingDialog(R.string.loading_detail);
    }

    /**
     * UI hook to hide the indicator once the SDK is loaded.
     * <p>
     * Developers should override this method to hide the indeterminate progress indicator.
     * This method is called by {@link #initialize(Activity)}.
     * <p>
     * Here we override this method to hide the blocking loading dialog that was shown via
     * {@link #onSdkLoadingStart()}.
     */
    @Override
    public void onSdkLoadingEnd(boolean b) {
        hideLoadingDialog();
    }


    /**
     * UI hook to show an error message to the user when the SDK has expired, and communication
     * with the server fails due to a network error.
     * <p>
     * When the SDK license expires on the device, the SDK will try to check with the server to
     * see if the license has been renewed. If the license has been renewed, then the SDK will
     * be reinitialized.
     * <p>
     * During communicating with the server, a network error may occur. Override this method to
     * show an error message to the user. To retry, developers can call {@link #initialize(Activity)}.
     * <p>
     * Here we override this method to show a user a dialog to retry the license check.
     */
    @Override
    public void onSdkExpiredAndNetworkException(Throwable throwable) {
        showDialog(R.string.license_check,
                R.string.license_check_detail, new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                initialize(
                        SdkBaseActivity.this);
            }
        }, R.color.colorError);
    }

    /**
     * UI hook to show an error message to the user when user requested deactivation, and communication
     * with the server fails due to a network error.
     * <p>
     * During communicating with the server, a network error may occur. Override this method to
     * show an error message to the user. To retry, developers can call {@link #deactivate(Activity)}.
     * <p>
     * Here we override this method to show a user a dialog to retry the deactivation , or cancel it.
     */
    @Override
    public void onDeactivateNetworkException(Throwable throwable) {
        showConfirmationDialog(R.string.network_error, R.string.deactivate_network_err, R.string.retry, R.string.cancel, new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //call deactivate again when user tap on retry
                deactivate(SdkBaseActivity.this);
            }
        });
    }

    /**
     * A helper method to call onRequiresReactivation() of the interface.
     * @param activity the current activity
     */
    private void superOnRequiresReactivation(Activity activity) {
        ISenseCryptSdkActivity.super.onRequiresReactivation(activity);
    }

    /**
     * Called when the SDK requires reactivation. Override this method to handle any UI logic.
     * The default implementation starts the access key activity specified via the
     * {@link SenseCryptActivityStateManager#setActivities(Class, Class, Class)} method by
     * calling {@link #startAccessKeyActivity(Activity)}. If the access key is programatically
     * set via {@link SenseCryptSdk#setAccessKeyForActivation(Context, String)}, then the
     * activation activity specified via the {@link SenseCryptActivityStateManager#setActivities(Class, Class, Class)}
     * method is started by calling {@link ISenseCryptSdkActivity#startActivationActivity(Activity)}.
     */
    @Override
    public void onRequiresReactivation(Activity activity) {
        // Check if the SDK was previously activated
        if(SenseCryptSdk.isPreviouslyActivated(activity)){
            // If so, show the dialog that the license has expired
            showDialog(R.string.license_expired, R.string.license_expired_detail, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Upon acknowledgement, clear the previously activated status, and start the access key activity
                    SenseCryptSdk.resetPreviouslyActivatedStatus(activity);
                    superOnRequiresReactivation(activity);
                }
            }, R.color.colorError);
        } else {
            // Directly go to the access key activity, if previously activated status is not set
            superOnRequiresReactivation(activity);
        }
    }

    /**
     * All activities that use the SDK need to override onResume() and call the
     * stateManager.onResume() method.
     * <p>
     * This ensures that the SDK automatically manages its state, and shows the correct
     * UI to the user.
     * <p>
     * Here we override this method to call the stateManager.onResume() method, and since
     * all activities that use the SDK will extend this activity, we do not need to override
     * onResume() in those activities.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // call onResume of stateManager
        stateManager.onResume();
    }
}
