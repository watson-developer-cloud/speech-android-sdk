package com.ibm.watson.developer_cloud.android.examples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by mihui on 7/4/16.
 */
public class TTSCustomizationDetailsActivity extends Activity {
    private static String TAG = "DetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String customizationId = intent.getStringExtra("customization_id");

        Log.w(TAG, "customizationId--->"+ customizationId);

        //TextView customizationIdTextView = (TextView)findViewById(R.id.customizationIdTextView);
        //customizationIdTextView.setText(customizationId);
    }
}
