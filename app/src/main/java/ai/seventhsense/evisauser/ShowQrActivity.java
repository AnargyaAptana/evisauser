package ai.seventhsense.evisauser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import java.io.IOException;

import ai.seventhsense.evisauser.ui.GalleryHelper;
import ai.seventhsense.evisauser.utils.Constants;
import ai.seventhsense.evisauser.utils.KeyValueStore;

/**
 * The activity to show the QR code and save it to the gallery
 */
public class ShowQrActivity extends BaseActivity {
    // UI Components
    AppCompatButton btnSaveQr;

    AppCompatImageView ivClose;
    AppCompatImageView ivQr;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        computeWindowSizeClasses();

        // Set up the UI components
        setUpUICompoments();

        // Set up the listeners
        setUpListener();
    }

    /**
     * Sets up the UI components
     */
    private void setUpUICompoments(){
        btnSaveQr = findViewById(R.id.btnSaveQr);

        ivQr = findViewById(R.id.ivQr);

        ivClose = findViewById(R.id.ivBack);

        btnSaveQr.setText(R.string.save_qr);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenHeight = displayMetrics.heightPixels;
        ViewGroup.LayoutParams layoutParams = ivQr.getLayoutParams();
        layoutParams.height = screenHeight / 2;
        ivQr.setLayoutParams(layoutParams);
    }

    /**
     * Sets up the listeners for the UI components
     */
    private void setUpListener(){

        byte[] byteArray = getIntent().getByteArrayExtra(Constants.EXTRA_DETECTED_IMAGE);
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        ivQr.setImageBitmap(bitmap);
        btnSaveQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    GalleryHelper.saveImageToGallery(ShowQrActivity.this, bitmap);
                    showSuccessDialog();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMain();
            }
        });
    }

    /**
     * Shows a dialog to indicate that the QR code is saved to the gallery
     */
    public void showSuccessDialog() {
        showDialogWithColor(R.string.success, R.string.qr_image_is_successfully_saved_into_gallery, R.color.colorSuccess);
    }

    /**
     * Different layout for different screen sizes
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
            setContentView(R.layout.activity_show_qr);
        } else if ( KeyValueStore.getInstance(ShowQrActivity.this).getPortraitMode()) {
            setContentView(R.layout.activity_sw_show_qr);
        }else{
            setContentView(R.layout.activity_expand_show_qr);
        }
    }
}