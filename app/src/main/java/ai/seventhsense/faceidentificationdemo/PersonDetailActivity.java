package ai.seventhsense.faceidentificationdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import ai.seventhsense.faceidentificationdemo.utils.Constants;
import ai.seventhsense.faceidentificationdemo.utils.KeyValueStore;
import ai.seventhsense.faceidentificationdemo.utils.Util;

/**
 * The activity to show the details of the person
 */
public class PersonDetailActivity extends BaseActivity {

    // UI Components
    private TextView tvRecord, tvMessage, tvTitle;

    private ImageView ivBack, ivImage;

    private AppCompatButton btnSeeMore, btnSeeLess;

    private LinearLayout identificationView, llPersonDetail;

    private boolean isTabletLandscapeMode = false;

    private LinearLayout layout2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        computeWindowSizeClasses();

        // Set up the UI components
        setUpComponents();

        // Set the data based on SensePrint metadata
        setMetaData();

        final Handler handler = new Handler(Looper.getMainLooper());


        // Show the success message
        handler.postDelayed(() -> {
            showSnackBar(llPersonDetail, R.string.verification_success, R.color.colorSuccess);
        }, 500);

        // Set up the listeners for the UI components
        setUpListener();
    }


    /**
     * Sets up the UI components
     */
    private void setUpComponents() {
        tvRecord = findViewById(R.id.tvRecordId);

        ivImage = findViewById(R.id.profile);

        ivBack = findViewById(R.id.ivBack);

        tvTitle = findViewById(R.id.tvTitle);

        tvMessage = findViewById(R.id.tvMessage);

        llPersonDetail = findViewById(R.id.llPersonDetail);

        btnSeeLess = findViewById(R.id.seeLess);

        btnSeeMore = findViewById(R.id.seeMore);

        layout2 = findViewById(R.id.layout2);

        identificationView = findViewById(R.id.layoutCard);
    }

    /**
     * Sort the metadata by key
     */
    private TreeMap<String, String> sortMap(Map<String, String> map) {
        TreeMap<String, String> sorted = new TreeMap<>();
        sorted.putAll(map);
        return sorted;
    }

    /**
     * Dynamically create textviews to show key and value of metadata
     */
    public void displayMap(Map<String, String> myMap) {

        Iterator iterator = sortMap(myMap).entrySet().iterator();

        // We want to show name first
        String isName = myMap.get("Name");
        String isname = myMap.get("name");
        String name = "";

        if (isName != null) {
            tvTitle.setText("NAME");
            tvMessage.setText(isName.toUpperCase());
            name = isName;

        }else if (isname != null) {

            tvTitle.setText(Constants.META_DATA_NAME.toUpperCase());
            tvMessage.setText(name.toUpperCase());

            name = isname;

        } else {

            tvTitle.setVisibility(View.GONE);
            tvMessage.setVisibility(View.GONE);
        }

        // Fields with underscore will be ignored in the display
        boolean isContainUnderscore;





        if (isTabletLandscapeMode) {
            layout2.setVisibility(View.VISIBLE);

            int columns = 3;
            int rows = (int) Math.ceil(myMap.size() / (float) columns);
            int index = 0;

            LinearLayout cardLinearLayout = new LinearLayout(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            cardLinearLayout.setLayoutParams(cardParams);
            cardLinearLayout.setOrientation(LinearLayout.VERTICAL);

            for (int i = 0; i < rows + 1; i++) {

                LinearLayout rowLayout = new LinearLayout(this);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                rowLayout.setLayoutParams(rowParams);
                rowLayout.setWeightSum(3);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);

                for (int j = 0; j < columns; j++) {

                    LinearLayout columnlLayout = new LinearLayout(this);
                    LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    columnlLayout.setLayoutParams(columnParams);
                    columnlLayout.setOrientation(LinearLayout.VERTICAL);

                    if (i == 0 && j == 0) {
                        //Add record id first
                        String record = getIntent().getStringExtra(Constants.EXTRA_RECORD_ID);
                        TextView valueIDTextView = createValueMetaDataTextView(record);
                        TextView keyIDTextView = createKeyMetaDataTextView(Constants.META_DATA_ID.toUpperCase());

                        columnlLayout.addView(keyIDTextView);
                        columnlLayout.addView(valueIDTextView);
                        rowLayout.addView(columnlLayout);

                        continue;

                    }
                    if (i == 0 && j == 1) {

                        if (name.length() >= 1) {
                            //Add name first
                            TextView keyNameTextView = createKeyMetaDataTextView(Constants.META_DATA_NAME.toUpperCase());
                            TextView valueNameTextView = createValueMetaDataTextView(name.toUpperCase());

                            columnlLayout.addView(keyNameTextView);
                            columnlLayout.addView(valueNameTextView);
                            rowLayout.addView(columnlLayout);
                            continue;
                        }

                    }


                    if (index < myMap.size()) {

                        Map.Entry<String, String> entry = (Map.Entry<String, String>) myMap.entrySet().toArray()[index];

                        if (entry.getKey().equals("Name") || entry.getKey().equals("name")) {
                            index++;
                            if (index >= myMap.size()) {
                                break;
                            }
                            entry = (Map.Entry<String, String>) myMap.entrySet().toArray()[index];
                        }
                        if (entry.getKey().equals(Constants.META_DATA_ID) || entry.getKey().equals(Constants.META_DATA_ID.toUpperCase())) {
                            index++;
                            if (index >= myMap.size()) {
                                break;
                            }
                            entry = (Map.Entry<String, String>) myMap.entrySet().toArray()[index];

                        }

                        String key = entry.getKey().toUpperCase();
                        String value = entry.getValue().toUpperCase();
                        TextView keyTextView = createKeyMetaDataTextView(key);
                        TextView valueTextView = createValueMetaDataTextView(value);
                        columnlLayout.addView(keyTextView);
                        columnlLayout.addView(valueTextView);
                        rowLayout.addView(columnlLayout);

                        index++;

                    }
                }

                if (i < 2) {
                    cardLinearLayout.addView(rowLayout); //first two rows is visible
                } else {
                    identificationView.setLayoutParams(cardParams);
                    identificationView.addView(rowLayout); // the rest will be hidden
                }


            }


            layout2.addView(cardLinearLayout);

        } else {
            // Iterate through rest of the fields
            while (iterator.hasNext()) {


                // Get the entry
                Map.Entry mentry = (Map.Entry) iterator.next();

                // We want to ignore the fields with underscore
                isContainUnderscore = mentry.getKey().toString().startsWith("_");
                if (isContainUnderscore) {
                    continue;
                }

                // We want to ignore the fields with name and id
                // as they are already shown
                if (mentry.getKey().equals(Constants.META_DATA_NAME) || mentry.getKey().equals(Constants.META_DATA_NAME.toLowerCase())) {
                    continue;
                }
                if (mentry.getKey().equals(Constants.META_DATA_ID) || mentry.getKey().equals(Constants.META_DATA_ID.toUpperCase())) {
                    continue;
                }


                TextView keyTextView = createKeyMetaDataTextView(mentry.getKey().toString().toUpperCase());
                TextView valueTextView = createValueMetaDataTextView(mentry.getValue().toString().toUpperCase());
                identificationView.addView(keyTextView);
                identificationView.addView(valueTextView);
            }


        }
    }


    /**
     * Create textview to show key of metadata
     */
    private TextView createKeyMetaDataTextView(String key) {
        TextView keyTextView = new TextView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        keyTextView.setText(key);
        keyTextView.setTextColor(getResources().getColor(R.color.buttonColor));
        keyTextView.setTextSize(18);
        //keyTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        keyTextView.setPadding(10, 10, 0, 0);

        return keyTextView;
    }

    /**
     * Create textview to show value of metadata
     */
    private TextView createValueMetaDataTextView(String value) {
        TextView textView2 = new TextView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textView2.setText(value);
        textView2.setTextColor(getResources().getColor(R.color.colorBackground));
        textView2.setTextSize(18);
        layoutParams.setMargins(0, 0, 0, 32);
        textView2.setLayoutParams(layoutParams);
        textView2.setPadding(10, 5, 10, 15);
        return textView2;
    }


    private void setMetaData() {
        Intent intent = getIntent();

        // Get the string metadata from the intent and convert it to a hashmap
        Map<String, String> metaDataMap = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.EXTRA_DATA);

        // Get the record id from the intent
        String record = intent.getStringExtra(Constants.EXTRA_RECORD_ID);
        tvRecord.setText(record);

        // Display the metadata
        displayMap(metaDataMap);

        // Get the image from the intent and set it to the imageview
        byte[] byteArray = getIntent().getByteArrayExtra(Constants.EXTRA_DETECTED_IMAGE);
        Bitmap bitmap = Util.byteArray2Bitmap(byteArray);
        ivImage.setImageBitmap(bitmap);
    }


    /**
     * Sets up the listeners for the UI components
     */
    private void setUpListener() {
        btnSeeMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSeeLessView();
            }
        });

        btnSeeLess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSeeMoreView();

            }
        });


        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                navigateToMain();

            }
        });
    }

    /**
     * Show the see more view (more fields are visible)
     */
    private void showSeeMoreView() {
        btnSeeMore.setVisibility(View.VISIBLE);
        identificationView.setVisibility(View.GONE);
        btnSeeLess.setVisibility(View.GONE);
    }

    /**
     * Show the see less view (less fields are visible)
     */
    private void showSeeLessView() {
        btnSeeLess.setVisibility(View.VISIBLE);
        identificationView.setVisibility(View.VISIBLE);
        btnSeeMore.setVisibility(View.GONE);
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
            setContentView(R.layout.activity_person_detail);

        } else if (KeyValueStore.getInstance(PersonDetailActivity.this).getPortraitMode()) {
            setContentView(R.layout.activity_sw_person_detail);

        } else {
            setContentView(R.layout.activity_expand_person_detail);
            isTabletLandscapeMode = true;
        }

    }
}


