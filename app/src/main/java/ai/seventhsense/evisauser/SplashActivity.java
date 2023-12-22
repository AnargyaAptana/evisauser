package ai.seventhsense.evisauser;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import ai.seventhsense.evisauser.utils.Constants;
import ai.seventhsense.evisauser.utils.KeyValueStore;

/**
 * A simple activity to show the splash screen
 */
public class SplashActivity extends AppCompatActivity {
    static boolean isMainActivityLaunched = false;

    private TextView tvVersion;

    public static String getVersionName(Context context) {
        try {
            // Get the package info for the current app
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    0
            );

            // Return the version name
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        
        computeWindowSizeClasses();


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        final Handler handler = new Handler();


        // Navigate to the main activity after 2 seconds
        handler.postDelayed(() -> {
            if(isFinishing()) {
                return;
            }
            if(!isMainActivityLaunched) {
                isMainActivityLaunched = true;

                finish();
                Intent newIntent = new Intent(this, MainActivity.class);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(newIntent);
            }

        }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isMainActivityLaunched = false;

        //computeWindowSizeClasses();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

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
            setContentView(R.layout.activity_splash);
        } else if ( KeyValueStore.getInstance(SplashActivity.this).getPortraitMode()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_sw_splash);

        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_sw_splash);
        }
    }


}