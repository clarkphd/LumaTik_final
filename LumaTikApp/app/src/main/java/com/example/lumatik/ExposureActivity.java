package com.example.lumatik;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;

public class ExposureActivity extends AppCompatActivity {

    private static final String TAG = "com.example.lumatik.ExposureActivity";

    private static final int NOTIFICATION_START = 3;
    private static final int BACK_START = 2;
    private static final int SETUP_START = 1;
    private static final int SETTINGS_START = 0;
    private int enterState = SETUP_START;

    CheckBox hasHat;
    CheckBox hasFaceCovered;
    CheckBox hasGloves;
    CheckBox hasShoes;
    Spinner upperLimbSpinner;
    Spinner upperBodySpinner;
    Spinner lowerLimbSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exposure);
        enterState = getIntent().getIntExtra("StartFrom", -1);
        if (enterState == -1) {
            Log.e(TAG, "Illegal enterState, ending app");
            finishAndRemoveTask();
        }
        hasHat = findViewById(R.id.hasHat);
        hasFaceCovered = findViewById(R.id.faceCovered);
        hasGloves = findViewById(R.id.hasGloves);
        hasShoes = findViewById(R.id.hasShoes);
        upperLimbSpinner = findViewById(R.id.upperLimbSpinner);
        upperBodySpinner = findViewById(R.id.upperBodySpinner);
        lowerLimbSpinner = findViewById(R.id.lowerLimbSpinner);

        if (enterState == SETTINGS_START | enterState == BACK_START | enterState == NOTIFICATION_START) {
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            hasHat.setChecked(sharedPreferences.getBoolean("hasHat", false));
            hasGloves.setChecked(sharedPreferences.getBoolean("hasGloves", false));
            hasShoes.setChecked(sharedPreferences.getBoolean("hasShoes", false));
            hasFaceCovered.setChecked(sharedPreferences.getBoolean("hasFaceCovered", false));
            upperLimbSpinner.setSelection(sharedPreferences.getInt("upperLimbCoverage", 2));
            upperBodySpinner.setSelection(sharedPreferences.getInt("upperBodyCoverage", 2));
            lowerLimbSpinner.setSelection(sharedPreferences.getInt("lowerLimbCoverage", 2));
        }
    }

    public void onTemplateClicked(View view) {
        Log.i(TAG, "Template pressed.");
        ImageButton iB = (ImageButton) view;
        String type = iB.getTag().toString();
        Log.i(TAG, "Template is: " + type);
        switch (type.substring(type.length()-1)) {
            case "1":
                hasHat.setChecked(false);
                hasGloves.setChecked(false);
                hasShoes.setChecked(false);
                hasFaceCovered.setChecked(false);
                upperLimbSpinner.setSelection(0);
                upperBodySpinner.setSelection(0);
                lowerLimbSpinner.setSelection(1);
                break;

            case "2":
                hasHat.setChecked(false);
                hasGloves.setChecked(false);
                hasShoes.setChecked(false);
                hasFaceCovered.setChecked(false);
                upperLimbSpinner.setSelection(1);
                upperBodySpinner.setSelection(2);
                lowerLimbSpinner.setSelection(1);
                break;

            case "3":
                hasHat.setChecked(false);
                hasGloves.setChecked(false);
                hasShoes.setChecked(false);
                hasFaceCovered.setChecked(false);
                upperLimbSpinner.setSelection(2);
                upperBodySpinner.setSelection(2);
                lowerLimbSpinner.setSelection(1);
                break;

            case "4":
                hasHat.setChecked(false);
                hasGloves.setChecked(false);
                hasShoes.setChecked(false);
                hasFaceCovered.setChecked(false);
                upperLimbSpinner.setSelection(1);
                upperBodySpinner.setSelection(2);
                lowerLimbSpinner.setSelection(2);
                break;

            case "5":
                hasHat.setChecked(true);
                hasGloves.setChecked(false);
                hasShoes.setChecked(false);
                hasFaceCovered.setChecked(false);
                upperLimbSpinner.setSelection(2);
                upperBodySpinner.setSelection(2);
                lowerLimbSpinner.setSelection(2);
                break;

        }
    }

    boolean hat;
    boolean faceCovered;
    boolean gloves;
    boolean shoes;
    int upperLimbCoverage;
    int upperBodyCoverage;
    int lowerLimbCoverage;

    public void onApplyClicked(View view) {
        if (getAndCheckValues()) {
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("hasHat", hat);
            editor.putBoolean("hasFaceCovered", faceCovered);
            editor.putBoolean("hasGloves", gloves);
            editor.putBoolean("hasShoes", shoes);
            editor.putInt("upperLimbCoverage", upperLimbCoverage);
            editor.putInt("upperBodyCoverage", upperBodyCoverage);
            editor.putInt("lowerLimbCoverage", lowerLimbCoverage);
            editor.putFloat("exposureFactor", calculateExposureFactor());
            editor.apply();
            if (enterState == SETUP_START | enterState == BACK_START) {
                Intent intent = new Intent(this, DeviceListActivity.class);
                intent.putExtra("StartFrom", SETUP_START);
                startActivity(intent);
            } else if (enterState == NOTIFICATION_START) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
            new DataToServer().execute();
            finish();
        }
    }

    private boolean getAndCheckValues() {
        hat = hasHat.isChecked();
        faceCovered = hasFaceCovered.isChecked();
        gloves = hasGloves.isChecked();
        shoes = hasShoes.isChecked();
        upperLimbCoverage = getResources().getIntArray(R.array.coverage_values)[upperLimbSpinner.getSelectedItemPosition()];
        upperBodyCoverage = getResources().getIntArray(R.array.coverage_values)[upperBodySpinner.getSelectedItemPosition()];
        lowerLimbCoverage = getResources().getIntArray(R.array.coverage_values)[lowerLimbSpinner.getSelectedItemPosition()];
        return true;
    }

    //This exposure is very approximate, more work needed to get better exposure accuracies
    private float calculateExposureFactor() {
        float exposure = 0.01f;
        if (!hat) {
            exposure += 0.055f;
        }
        if (!faceCovered) {
            exposure += 0.035f;
        }
        if (!gloves) {
            exposure += 0.02f;
        }
        if (!shoes) {
            exposure += 0.03;
        }
        switch (upperLimbCoverage) {
            case 0:
                // uncovered
                exposure += 0.07f;
                break;
            case 1:
                // half covered
                exposure += 0.035f;
                break;
            case 2:
                // full covered
                break;
            default:
                Log.e(TAG, "Upper Limb Coverage default case, calculateExposureFactor");
                break; // this should never happen
        }

        switch (upperBodyCoverage) {
            case 0:
                // uncovered
                exposure += 0.18f;
                break;
            case 1:
                // half covered
                exposure += 0.09f;
                break;
            case 2:
                // full covered
                break;
            default:
                Log.e(TAG, "Upper Body Coverage default case, calculateExposureFactor");
                break; // this should never happen
        }

        switch (lowerLimbCoverage) {
            case 0:
                // uncovered
                exposure += 0.11f;
                break;
            case 1:
                // half covered
                exposure += 0.07f;
                break;
            case 2:
                // full covered
                break;
            default:
                Log.e(TAG, "Lower Limb Coverage default case, calculateExposureFactor");
                break; // this should never happen
        }
        Log.i(TAG, "Exposure recorded: " + exposure);
        return exposure;
    }

    /*
        {
        "UserDataID": "264ca2db-7e0a-4cd2-ab71-9c2451d81d5f",
        "UserID": "http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/User/1/",
        "DeviceID": "http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/Device/1/",
        "Writetime": 234,
        "Age": 2342,
        "SkinPigment": 34,
        "Bedtime": 234,
        "WakeUp": 234,
        "Coverage": 234.0,
        "Location": 234
    },
     */
    private class DataToServer extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            // put info into dictionary, into jsonobject
            SharedPreferences sP = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            HashMap<String, Object> userData = new HashMap<>();
            DecimalFormat df = new DecimalFormat("0.000");
            userData.put("UserID", "http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/User/51/");
            userData.put("DeviceID", "http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/Device/51/");
            userData.put("Writetime", (long) java.util.Calendar.getInstance().getTimeInMillis()/1000);
            userData.put("Age", sP.getInt("age", 0));
            userData.put("SkinPigment", ExposureActivity.typeToInt(sP.getString("pigment", "Type V")));
            userData.put("Bedtime", sP.getInt("bedtimeHour", 0)*60 + sP.getInt("bedtimeMin", 0));
            userData.put("WakeUp", sP.getInt("waketimeHour", 0)*60 + sP.getInt("waketimeMin", 0));
            userData.put("Coverage", df.format(1.0f - sP.getFloat("exposureFactor", 0f)));
            userData.put("Location", 0); //Hard code 0, not enough time to implement location settings
            JSONObject jsonUserData = new JSONObject(userData);

            // Post json object as string to server
            try {
                URL url = new URL("http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/UserData/");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
                urlConnection.setRequestProperty("Accept", "application/json");
                BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                Log.i(TAG, "Data: " + jsonUserData.toString());
                String s = jsonUserData.toString().replace("\\", "");
                Log.i(TAG, "String: " + s);
                byte[] b = s.getBytes();
                out.write(b);
                out.flush();
                String res = String.valueOf(urlConnection.getResponseCode());
                if (!res.equals("200")) {
                    Log.e(TAG, "URL Error: " + res);
                } else {
                    Log.i(TAG, "URL Response Ok");
                }
                out.close();
                urlConnection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Exception caught: " + e.getMessage());
            }
            return null;
        }
    }

    public static int typeToInt(String type) {
        switch (type) {
            case "Type I":
                return 1;
            case "Type II":
                return 2;
            case "Type III":
                return 3;
            case "Type IV":
                return 4;
            case "Type V":
                return 5;
            default:
                return -1; //THIS SHOULD NEVER HAPPEN
        }
    }

    @Override
    public void onBackPressed() {
        if (enterState == SETTINGS_START) {
            super.onBackPressed();
        } else if (enterState == NOTIFICATION_START) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("StartFrom", BACK_START);
            startActivity(intent);
        }
    }
}