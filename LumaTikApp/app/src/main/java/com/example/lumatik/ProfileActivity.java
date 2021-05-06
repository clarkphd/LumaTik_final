package com.example.lumatik;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "com.example.lumatik.ProfileActivity";

    private static final int SETUP_START = 1;
    private static final int SETTINGS_START = 0;
    private int enterState = SETUP_START;

    EditText editAge;
    Spinner typeSelector;
    TimePicker bedtimePicker;
    TimePicker waketimePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        enterState = getIntent().getIntExtra("StartFrom", -1);
        if (enterState == -1) {
            Log.e(TAG, "Illegal enterState, ending app");
            finishAndRemoveTask();
        }
        editAge = findViewById(R.id.editAge);
        typeSelector = findViewById(R.id.typeSelector);
        bedtimePicker = findViewById(R.id.bedtimePicker);
        waketimePicker = findViewById(R.id.waketimePicker);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        bedtimePicker.setIs24HourView(true);
        bedtimePicker.setHour(hour);
        waketimePicker.setIs24HourView(true);
        waketimePicker.setHour(hour);

        if (enterState == 0) {
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            editAge.setText(sharedPreferences.getInt("age", 32)+"");
            typeSelector.setSelection(getIndex(typeSelector, sharedPreferences.getString("pigment", "Type I")));
            bedtimePicker.setHour(sharedPreferences.getInt("bedtimeHour", 0));
            bedtimePicker.setMinute(sharedPreferences.getInt("bedtimeMin", 0));
            waketimePicker.setHour(sharedPreferences.getInt("waketimeHour", 0));
            waketimePicker.setMinute(sharedPreferences.getInt("waketimeMin", 0));
        }
    }


    public void onApplyClicked(View view) {
        //TODO: save inputs to phone local files
        if (getAndCheckValues()) {
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("age", age);
            editor.putString("pigment", pigmentType);
            editor.putInt("bedtimeHour", bedtimeHour);
            editor.putInt("bedtimeMin", bedtimeMin);
            editor.putInt("waketimeHour", waketimeHour);
            editor.putInt("waketimeMin", waketimeMin);
            editor.apply();
            if (enterState == SETUP_START) {
                Intent intent = new Intent(this, ExposureActivity.class);
                intent.putExtra("StartFrom", SETUP_START);
                startActivity(intent);
            }
            finish();
        }
    }

    Integer age;
    String pigmentType;
    int bedtimeHour;
    int bedtimeMin;

    int waketimeHour;
    int waketimeMin;


    //private method of your class
    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }

        return 0;
    }

    //fact checking function that form is filled correctly, return false if not
    private boolean getAndCheckValues() {
        //age
        try {
            age = Integer.parseInt(editAge.getText().toString());
            if (age < 0) {
                Log.e(TAG, "Age is less than 0");
                toastmsg("Age cannot be less that 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Exception caught: " + e.getMessage());
            toastmsg("Age is not a number.");
            return false;
        }
        //pigmentType
        pigmentType = typeSelector.getSelectedItem().toString();
        //bedtime
        bedtimeHour = bedtimePicker.getHour();
        bedtimeMin = bedtimePicker.getMinute();
        //waketime
        waketimeHour = waketimePicker.getHour();
        waketimeMin = waketimePicker.getMinute();
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