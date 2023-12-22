package ai.seventhsense.evisauser;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import com.google.android.material.snackbar.Snackbar;

import ai.seventhsense.evisauser.utils.Constants;
import ai.seventhsense.evisauser.utils.KeyValueStore;

/**
 * This is the base activity for all the activities in the app.
 * <p>
 * All activities extend this activity. This activity provides some utility methods
 * that are used by all the activities.
 */
public  class BaseActivity extends AppCompatActivity {
    // A dialog that is shown when the app is busy
    // this could be when the SDK is loading, or when the SDK is processing an image
    // or when the SDK is communicating with the server
    private AlertDialog busyDialog = null;

    // A dialog that is shown on an error or when user needs to confirm something
    // or acknowledge and error
    private Dialog dialog = null;

    // A snackbar that is shown when there is an error r success
    private Snackbar snackbar = null;
    // Whether the device is a tablet or not
    boolean isTablet;

    // The smallest screen width in density-independent pixels (dp)
    int smallestScreenWidthDp;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       // setLayouts();
    }

    /**
     * A utility method to navigate to the main activity
     */
    public void navigateToMain() {
        finish();
        Intent newIntent = new Intent(this, MainActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(newIntent);
    }

    /**
     * A utility method that shows a loading dialog that blocks the UI.
     * <p>
     *
     * @param resIdTitle  The title of the dialog
     */
    public synchronized void showLoadingDialog(int resIdTitle) {
        // If the activity is finishing, we cannot show a dialog
        if(isFinishing()){
            return;
        }
        if(busyDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialogTheme);
            LayoutInflater inflater = getLayoutInflater();
            builder.setTitle(null);
            builder.setCancelable(false);
            // The TextView that is shown in the busy dialog
            View busyDialogTextView = inflater.inflate(R.layout.dialog_loading, null);
            builder.setView(busyDialogTextView);
            busyDialog = builder.create();
            busyDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            busyDialog.setCancelable(false);
            TextView title = busyDialogTextView.findViewById(R.id.tvBusyTitle);
            title.setText(resIdTitle);
            busyDialog.show();
        }else{
            busyDialog.show();
        }
    }

    /**
     * A utility method that hides the loading dialog and unblocks the UI.
     */
    public void hideLoadingDialog() {
        if (busyDialog != null) {
            busyDialog.dismiss();
        }
    }

    /**
     * A utility method that dismisses any error or confirmation dialog that is shown.
     * <p>
     * Note: This method does not dismiss the loading dialog. To dismiss the loading dialog
     * use {@link #hideLoadingDialog()}
     */
    public void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    /**
     * A utility method that shows a dialog with a title and a message.
     * A dialog is not shown if there is already a dialog shown. An already shown dialog
     * can be dismissed by calling {@link #dismissDialog()}
     *
     * @param resIdTitle  The title of the dialog
     * @param resIdDetail The detail of the dialog
     * @param listener    The listener to be called when the user clicks the OK button. If the listener is null, then the dialog will be dismissed when the user clicks the OK button.
     * @param colorCode   The color of the title
     */
    public synchronized void showDialog(int resIdTitle, int resIdDetail, View.OnClickListener listener, int colorCode) {
        // If the activity is finishing, we cannot show a dialog
        if(isFinishing()){
            return;
        }
        if (dialog == null) {
            dialog = new Dialog(BaseActivity.this);
            AppCompatButton btnOk;
            TextView tvTitle;
            TextView tvMessage;

            WindowMetrics metrics = WindowMetricsCalculator.getOrCreate()
                    .computeCurrentWindowMetrics(this);

            float widthDp = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                widthDp = metrics.getBounds().width() /
                        getResources().getDisplayMetrics().density;
            }

            // Use different layouts for different screen sizes
            if (widthDp < Constants.PHONE_WIDTH_DP) {
                dialog.setContentView(R.layout.layout_dialog_qr_fail);
            } else if ( KeyValueStore.getInstance(BaseActivity.this).getPortraitMode()) {
                dialog.setContentView(R.layout.layout_sw_dialog_qr_fail);

            }else{
                dialog.setContentView(R.layout.layout_expand_dialog_qr_fail);
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

            btnOk = dialog.findViewById(R.id.btnOk);
            tvTitle = dialog.findViewById(R.id.tvTitle);
            tvMessage = dialog.findViewById(R.id.tvMessage);

            tvTitle.setText(resIdTitle);
            tvTitle.setTextColor(getColor(colorCode));
            tvMessage.setText(resIdDetail);

            dialog.setOnCancelListener(dialog1 -> {
                dialog = null;
                Log.e("Dialog", "cancel");
            });

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogs) {
                    dialog = null;
                }
            });



            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    dialog = null;
                    if(listener != null) {
                        listener.onClick(view);
                    }
                }
            });

            dialog.show();
        }else{
            dialog.show();
        }
    }

    /**
     * A utility method that shows a confirmation dialog with a title and a message.
     *
     * @param resIdTitle  The title of the dialog
     * @param resIdDetail The detail of the dialog
     * @param btnYesTitle The text of the positive button
     * @param btnNoTitle  The text of the negative button
     * @param positiveListener  The listener to be called when the user clicks the positive button. If the listener is null, then the dialog will be dismissed when the user clicks the positive button.
     */
    public synchronized void showConfirmationDialog(int resIdTitle, int resIdDetail, int btnYesTitle, int btnNoTitle, View.OnClickListener positiveListener) {
        showConfirmationDialog(resIdTitle, resIdDetail, btnYesTitle, btnNoTitle, positiveListener, null);
    }

    /**
     * A utility method that shows a confirmation dialog with a title and a message.
     *
     * @param resIdTitle  The title of the dialog
     * @param resIdDetail The detail of the dialog
     * @param btnYesTitle The text of the positive button
     * @param btnNoTitle  The text of the negative button
     * @param positiveListener  The listener to be called when the user clicks the positive button. If the listener is null, then the dialog will be dismissed when the user clicks the positive button.
     * @param negativeListener  The listener to be called when the user clicks the negative button. If the listener is null, then the dialog will be dismissed when the user clicks the negative button.
     */
    public synchronized void showConfirmationDialog(int resIdTitle, int resIdDetail, int btnYesTitle, int btnNoTitle, View.OnClickListener positiveListener, View.OnClickListener negativeListener) {
        // If the activity is finishing, we cannot show a dialog
        if(isFinishing()){
            return;
        }
        if(dialog == null ) {
            dialog = new Dialog(this);
            AppCompatButton btnYes;
            AppCompatButton btnNo;
            TextView title;
            TextView message;

            WindowMetrics metrics = WindowMetricsCalculator.getOrCreate()
                    .computeCurrentWindowMetrics(this);

            float widthDp = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                widthDp = metrics.getBounds().width() /
                        getResources().getDisplayMetrics().density;
            }



            if (widthDp < Constants.PHONE_WIDTH_DP) {
                dialog.setContentView(R.layout.layout_dialog_confirm);
            } else if (widthDp < 840f && KeyValueStore.getInstance(BaseActivity.this).getPortraitMode()) {
                dialog.setContentView(R.layout.layout_sw_dialog_confirm);

            }else{
                dialog.setContentView(R.layout.layout_expand_dialog_confirm);
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

            btnYes = dialog.findViewById(R.id.btnYes);
            btnNo = dialog.findViewById(R.id.btnNo);
            btnYes.setText(btnYesTitle);
            btnNo.setText(btnNoTitle);
            title = dialog.findViewById(R.id.tvTitle);
            message = dialog.findViewById(R.id.tvMessage);

            title.setText(resIdTitle);
            message.setText(resIdDetail);
            btnYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    dialog = null;
                    if(positiveListener != null) {
                        positiveListener.onClick(v);
                    }
                }
            });

            btnNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    dialog = null;
                    if(negativeListener != null) {
                        negativeListener.onClick(v);
                    }
                }
            });

            dialog.show();
        }
    }

    /**
     * A utility method that shows an error dialog when there is an unexpected error.
     */
    public synchronized void showUnexpectedErrorDialog() {
        showDialog(R.string.unexpected_error, R.string.unexpected_detail, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMain();
            }
        }, R.color.colorError);
    }

    /**
     * A utility method that shows an error dialog with a title and a message.
     * A dialog is not shown if there is already a dialog shown. An already shown dialog
     * can be dismissed by calling {@link #dismissDialog()}
     *
     * @param title   The title of the dialog
     * @param message The detail of the dialog
     */
    public synchronized void showDialog(int title, int message) {
        showDialog(title, message, null, R.color.colorError);
    }

    /**
     * A utility method that shows an error dialog with a title and a message.
     * @param title  The title of the dialog
     * @param message The detail of the dialog
     * @param color The color of the title
     */
    public synchronized  void showDialogWithColor(int title, int message, int color){
        showDialog(title, message, null, color);
    }

    /**
     * When an activity on destroy is called, we should dismiss any dialogs that are shown.
     */
    @Override
    protected void onDestroy() {
        hideLoadingDialog();
        dismissDialog();
        super.onDestroy();
    }

    /**
     * A utility method that shows a snackbar with a message.
     * A snackbar is not shown if there is already a snackbar shown.
     *
     * @param view    The view to which the snackbar should be attached
     * @param textId  The message of the snackbar
     * @param colorId The color of the snackbar
     */
    public synchronized void showSnackBar(View view,  int textId, int colorId) {
        showSnackBar(view, textId, colorId, 0, 0);
    }

    /**
     * A utility method to convert dp to pixels
     * @param dp The dp value
     * @return The pixel value
     */
    public int dpToPx(float dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /**
     * A utility method that shows a snackbar with a message.
     * @param view The view to which the snackbar should be attached
     * @param textId The message of the snackbar
     * @param colorId The color of the snackbar
     * @param delayMs The delay in milliseconds after which the snackbar should be dismissed.
     *                Setting this value means that the no new snackbar will be shown until the
     *                current snackbar is dismissed.
     */
    public synchronized void showSnackBar(View view,  int textId, int colorId, int delayMs) {
        showSnackBar(view, textId, colorId, delayMs, 0);
    }

    /**
     * A utility method that shows a snackbar with a message.
     * @param view The view to which the snackbar should be attached
     * @param textId The message of the snackbar
     * @param colorId The color of the snackbar
     * @param delayMs The delay in milliseconds after which the snackbar should be dismissed.
     * @param marginDp The bottom margin of the snackbar in dp
     */
     public synchronized void showSnackBar(View view,  int textId, int colorId, int delayMs, int marginDp) {
         // If the activity is finishing, we cannot show a dialog
         if(isFinishing()){
             return;
         }

        if(snackbar == null ) {
            snackbar = Snackbar.make(view, textId, Snackbar.LENGTH_LONG);
            final View snackBarView = snackbar.getView();


            if(marginDp != 0) {
                // Convert the dp value to pixels
                int bottomMarginInPixels = dpToPx(marginDp);

                final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarView.getLayoutParams();

                params.setMargins(params.leftMargin,
                        params.topMargin,
                        params.rightMargin,
                        params.bottomMargin + bottomMarginInPixels);
            }

            ViewCompat.setElevation(snackBarView, 12f);

            // Set background color
            snackBarView.setBackgroundColor(ContextCompat.getColor(BaseActivity.this, colorId));

            final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarView.getLayoutParams();

            TextView tv = (TextView) snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (tv.getText().equals(getString(R.string.invalid_qr))) {
                params.setMargins(params.leftMargin,
                        params.topMargin,
                        params.rightMargin,
                        params.bottomMargin + 350);
            } else {
                params.setMargins(params.leftMargin,
                        params.topMargin,
                        params.rightMargin,
                        params.bottomMargin + 52);
            }


            //Replace with android.support.design.R.id.snackbar_text if you are not using androidX
            if (tv != null) {
                tv.setGravity(Gravity.CENTER);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            }

            snackBarView.setLayoutParams(params);

            if(delayMs != 0) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        // To avoid a flood of snackbar messages, wait for the snackbar to be dismissed
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                snackbar = null;
                            }
                        }, delayMs);
                    }
                });
            } else {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        snackbar = null;
                    }
                });
            }

            snackbar.show();
        }
    }
}
