package ai.seventhsense.faceidentificationdemo;


import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import java.util.Calendar;
import java.util.HashMap;

import ai.seventhsense.faceidentificationdemo.sdk.SdkBaseActivity;
import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.faceidentificationdemo.utils.Util;
import ai.seventhsense.sensecryptsdk.SenseCryptAsyncFunctionExecutor;
import ai.seventhsense.sensecryptsdk.SenseCryptGenerateQRRequest;
import ai.seventhsense.sensecryptsdk.SenseCryptSdk;
import ai.seventhsense.sensecryptsdk.SensePrintRequestWithDetectionThumbnail;
import ai.seventhsense.sensecryptsdk.SensePrintResult;

/**
 * Activity to get the metadata information from the user to generate a SensePrint
 * <p>
 * The fields provided by the user are used to create a HashMap of the metadata
 * <p>
 * The SensePrint is then created using SenseCryptSdk.createSensePrint()
 * <p>
 * The SensePrint is then embedded into a QR code using SenseCryptSdk.generateQR()
 */
public class GenerateQrMetaDataActivity extends SdkBaseActivity {
    // UI Components
    AppCompatButton btnNext;
    EditText etAddress, etNationality, etId, etName, etPassword, etDob, etDateOfIssue;
    Spinner etGender, etMarital;
    TextView tvError;
    ScrollView scrollView;

    // Two compulsory fields
    String recordId, name;

    AppCompatButton btnLivenessYes, btnLivenessNo;


    ImageView ivBack;

    private boolean isLivenessIncluded = true;


    ImageView ivSelectGender, ivSelectDOB, ivSelectIssue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        computeWindowSizeClasses();

        // Set up the UI components
        setUpComponents();

        // Add listeners to the UI components
        setUpListeners();
        isLivenessIncluded = true;
    }

    public void setUpComponents() {
        etId = findViewById(R.id.etRecordId);

        etName = findViewById(R.id.etName);

        tvError = findViewById(R.id.tvError);

        etDob = findViewById(R.id.etDOB);

        etGender = findViewById(R.id.etGender);

        etMarital = findViewById(R.id.etMarital);

        etPassword = findViewById(R.id.etPassword);

        scrollView = findViewById(R.id.layoutMetaData);

        ivSelectDOB = findViewById(R.id.ivSelectDOB);

        ivSelectGender = findViewById(R.id.ivSelectGender);

        ivSelectIssue = findViewById(R.id.ivSelectIssue);

        ivBack = findViewById(R.id.ivBack);

        etDateOfIssue = findViewById(R.id.etIssueDate);

        etNationality = findViewById(R.id.etNationality);

        etAddress = findViewById(R.id.etAddress);

        btnNext = findViewById(R.id.btnNext);

        btnLivenessNo = findViewById(R.id.btnLivenessNo);

        btnLivenessYes = findViewById(R.id.btnLivenessYes);

        // More initialization for the spinner
        setUpGenderSpinner();

        setUpMaritalSpinner();
    }

    /**
     * Setup the gender spinner
     */
    private void setUpGenderSpinner() {
        String[] dataRegion = getResources().getStringArray(R.array.gender);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_layout, dataRegion);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        etGender.setAdapter(spinnerArrayAdapter);

        etGender.setSelection(0);
    }

    private void setUpMaritalSpinner(){
        String[] dataRegion = getResources().getStringArray(R.array.marital_status);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_layout, dataRegion);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        etMarital.setAdapter(spinnerArrayAdapter);

        etMarital.setSelection(0);
    }
    /**
     * Add listeners to the UI components
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setUpListeners() {
        etDob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showDatePicker("dob");

            }
        });

        ivBack.setOnClickListener(view -> {
            finish();
        });


        ivSelectDOB.setOnClickListener(view -> {
            showDatePicker("dob");
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isEmpty()) {
                    createSensePrint();
                }
            }
        });

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float endY = event.getY();
                        if (endY > startY) {
                            int scrollDistance = 1000; // Adjust the scroll distance as needed
                            scrollView.smoothScrollBy(0, scrollDistance);
                        }
                        break;
                }
                return false;
            }
        });

        etDateOfIssue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showDatePicker("dateOfIssue");

            }
        });

        ivSelectIssue.setOnClickListener(view -> {
            showDatePicker("dateOfIssue");
        });

        etDob.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideKeyboard(v);
                showDatePicker("dob");
            }
        });

        etDateOfIssue.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideKeyboard(v);
                showDatePicker("dateOfIssue");
            }
        });
    }

    /**
     * Generate the QR code from the SensePrint
     *
     * @param result the SensePrint result
     */
    private void generateQR(SensePrintResult result) {
        // The loading dialog is shown in startDetection()

        // Create the request
        SenseCryptGenerateQRRequest request = new SenseCryptGenerateQRRequest(result.getSensePrint());

        // Generate the QR code
        SenseCryptSdk.generateQR(GenerateQrMetaDataActivity.this, request, new SenseCryptAsyncFunctionExecutor.Callback<Bitmap>() {

            @Override
            public void onResult(Bitmap bitmap) {
                // The QR code is generated, hide the loading dialog
                hideLoadingDialog();

                // Check if the bitmap is not null
                if (bitmap != null) {
                    // Start the ShowQrActivity which shows the QR code
                    Intent newIntent = new Intent(GenerateQrMetaDataActivity
                            .this, ShowQrActivity.class);
                    newIntent.putExtra(Constants.EXTRA_DETECTED_IMAGE, Util.bitMap2ByteArray(bitmap));
                    startActivity(newIntent);
                    finish();
                } else {
                    showUnexpectedErrorDialog();
                }
            }

            @Override
            public void onError(Exception e) {
                hideLoadingDialog();
                showUnexpectedErrorDialog();
            }
        });
    }

    /**
     * Create the SensePrint from the metadata
     */
    private void createSensePrint() {
        // Show the loading dialog
        showLoadingDialog(R.string.generating_qr_in_progress);

        // Create the metadata from the fields
        HashMap<String, String> metaData = createPersonMetaData();

        // Get the detection image data from the intent
        byte[] byteArray = getIntent().getByteArrayExtra(Constants.EXTRA_DETECTED_IMAGE);
        // Convert the byte array to a bitmap
        Bitmap bitmap = Util.byteArray2Bitmap(byteArray);

        // Create the request, based on password (if provided)
        SensePrintRequestWithDetectionThumbnail request;

        if (etPassword.getText().length() == 0 || etPassword.getText().equals(getString(R.string.enter_password))) {
            request = new SensePrintRequestWithDetectionThumbnail(bitmap, recordId, metaData, null, isLivenessIncluded);
        } else {
            request = new SensePrintRequestWithDetectionThumbnail(bitmap, recordId, metaData, etPassword.getText().toString().trim(), isLivenessIncluded);
        }

        // Create the SensePrint
        SenseCryptSdk.createSensePrint(GenerateQrMetaDataActivity.this, request, new SenseCryptAsyncFunctionExecutor.Callback<SensePrintResult>() {

            @Override
            public void onResult(SensePrintResult sensePrintResult) {
                // The SensePrint is created, generate the QR code
                if (sensePrintResult != null) {
                    // Create a QR code containing the SensePrint byte array
                    // in the SensePrintResult
                    generateQR(sensePrintResult);
                } else {
                    hideLoadingDialog();
                    showUnexpectedErrorDialog();
                }
            }

            @Override
            public void onError(Exception e) {
                hideLoadingDialog();
                showUnexpectedErrorDialog();
            }
        });
    }

    /**
     * Check if required fields are empty
     *
     * @return true if the fields are empty, false otherwise
     */
    private boolean isEmpty() {
        tvError.setVisibility(View.GONE);
        if (etId.getText().toString().length() == 0) {

            tvError.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

    /**
     * Create the metadata from the fields
     *
     * @return a HashMap of the metadata
     */
    private HashMap<String, String> createPersonMetaData() {
        HashMap<String, String> metaData = new HashMap<>();
        recordId = etId.getText().toString();
        name = etName.getText().toString();
        String dob = etDob.getText().toString();
        String address = etAddress.getText().toString();
        String dateOfIssue = etDateOfIssue.getText().toString();
        String nationality = etNationality.getText().toString();
        String sex = etGender.getSelectedItem().toString();


        metaData.put(getString(R.string.id), recordId);


        if (address.length() != 0) {
            metaData.put(getString(R.string.address), address);

        }

        if (name.length() != 0 && !name.equals("Name")) {
            metaData.put(getString(R.string.name), name);

        }

        if (sex.length() != 0 && !sex.equals(getString(R.string.gender))) {
            metaData.put(getString(R.string.sex), sex);

        }

        if (dob.length() != 0 && !dob.equals(getString(R.string.select_birth_date))) {
            metaData.put(getString(R.string.date_of_birth), dob);

        }

        if (dateOfIssue.length() != 0 && !dateOfIssue.equals(getString(R.string.select_date_of_issue))) {
            metaData.put(getString(R.string.date_of_issue), dateOfIssue);

        }

        if (nationality.length() != 0 && !nationality.equals(getString(R.string.enter_nationality))) {
            metaData.put(getString(R.string.nationality), nationality);

        }

        return metaData;
    }

    /**
     * Show the date picker dialog and set the date to the field
     *
     * @param fieldName the field name whose value is to be set
     */
    private void showDatePicker(String fieldName) {

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(GenerateQrMetaDataActivity.this, R.style.CustomDatePickerDialogTheme, (view, year1, monthOfYear, dayOfMonth1) -> {
            // Month value is zero-based, so add 1 to match standard month numbering (January = 1)
            String selectedDate = dayOfMonth1 + "/" + (monthOfYear + 1) + "/" + year1;
            if (fieldName.equals(getString(R.string.dob))) {
                etDob.setText(selectedDate);
            } else {
                etDateOfIssue.setText(selectedDate);
            }
        }, year, month, dayOfMonth);

        datePickerDialog.show();

        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.colorButton));
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.colorButton));
    }

    /**
     * Hide the keyboard
     *
     * @param view the view
     */
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        isLivenessIncluded = true;
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
            setContentView(R.layout.activity_generate_qr_meta_data);
        } else if (KeyValueStore.getInstance(GenerateQrMetaDataActivity.this).getPortraitMode()) {
            setContentView(R.layout.activity_sw_generate_qr_meta_data);

        } else {
            setContentView(R.layout.activity_expand_generate_qr_meta_data);
        }
    }
}