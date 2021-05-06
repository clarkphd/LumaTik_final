package com.example.lumatik;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

public class ExposureActivity extends AppCompatActivity {

    private static final String TAG = "com.example.lumatik.ExposureActivity";

    private static final int SETUP_START = 1;
    private static final int SETTINGS_START = 0;
    private int enterState = SETUP_START;

    CheckBox hasHat;
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
        hasGloves = findViewById(R.id.hasGloves);
        hasShoes = findViewById(R.id.hasShoes);
        upperLimbSpinner = findViewById(R.id.upperLimbSpinner);
        upperBodySpinner = findViewById(R.id.upperBodySpinner);
        lowerLimbSpinner = findViewById(R.id.lowerLimbSpinner);

        if (enterState == 0) {
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            hasHat.setChecked(sharedPreferences.getBoolean("hasHat", false));
            hasGloves.setChecked(sharedPreferences.getBoolean("hasGloves", false));
            hasShoes.setChecked(sharedPreferences.getBoolean("hasShoes", false));
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
        //TODO: set all fields to template values
    }

    boolean hat;
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
            editor.putBoolean("hasGloves", gloves);
            editor.putBoolean("hasShoes", shoes);
            editor.putInt("upperLimbCoverage", upperLimbCoverage);
            editor.putInt("upperBodyCoverage", upperBodyCoverage);
            editor.putInt("lowerLimbCoverage", lowerLimbCoverage);
            editor.apply();
            if (enterState == SETUP_START) {
                Intent intent = new Intent(this, DeviceListActivity.class);
                intent.putExtra("StartFrom", SETUP_START);
                startActivity(intent);
            }
            finish();
        }
    }

    private boolean getAndCheckValues() {
        hat = hasHat.isChecked();
        gloves = hasGloves.isChecked();
        shoes = hasShoes.isChecked();
        upperLimbCoverage = getResources().getIntArray(R.array.coverage_values)[upperLimbSpinner.getSelectedItemPosition()];
        upperBodyCoverage = getResources().getIntArray(R.array.coverage_values)[upperBodySpinner.getSelectedItemPosition()];
        lowerLimbCoverage = getResources().getIntArray(R.array.coverage_values)[lowerLimbSpinner.getSelectedItemPosition()];
        return true;
    }

    private void toastmsg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    //TODO: add back press code
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}