package ai.seventhsense.faceidentificationdemo.sdk;

import android.content.pm.ActivityInfo;

import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import ai.seventhsense.faceidentificationdemo.BaseActivity;
import ai.seventhsense.faceidentificationdemo.MainActivity;
import ai.seventhsense.faceidentificationdemo.R;
import ai.seventhsense.faceidentificationdemo.SplashActivity;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.sensecryptsdk.SenseCryptActivityStateManager;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;

/**
 * This class is used to initialize the SenseCrypt SDK.
 * <p>
 * Developers must also set the android:name attribute of the application tag in the AndroidManifest.xml
 * file to this class.
 */
public class Application extends android.app.Application {

    /**
     * This method is called when the application is created.
     * <p>
     * Here we set the activities that are used by the SenseCrypt SDK.
     */
    @Override
    public void onCreate() {
        super.onCreate();


        /**
         * It is also possible to preset a license key for the SDK.
         * In such a case, the customer key should be used
         */

        SenseCryptSdk.setAccessKeyForActivation(this,
                "8040aaea-28ef-4283-af40-f31dd8318f87");

        /**
         * Sets the activities that will be used by the SDK.
         * This method must be called before creating an instance of {@link SenseCryptActivityStateManager}.
         */
        // The Access Key Activity is used to start the SDK activation process
        // After the access key is verified by the server, the SDK will need activation
        // and models must be downloaded. This is done in the DownloadingModelActivity.
        SenseCryptActivityStateManager.setActivities(
                // Activity which will show model download status
                // and link the device to the access key
                DownloadingModelActivity.class,
                // Activity which will be used to check access key validity
                // and start the activation process handled in the DownloadingModelActivity
                AccessKeyActivity.class,
                // Activity which will be shown after the SDK is fully
                // downloaded and ready to use
                MainActivity.class
        );
    }



}
