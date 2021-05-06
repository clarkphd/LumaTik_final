package com.example.lumatik;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Objects;

//TODO: info button

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "com.example.lumatik.ProfileActivity";

    private static final int BACK_START = 2;
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

        // set default pigment to 2
        typeSelector.setSelection(getIndex(typeSelector, "Type II"));

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        bedtimePicker.setIs24HourView(true);
        bedtimePicker.setHour(hour);
        waketimePicker.setIs24HourView(true);
        waketimePicker.setHour(hour);

        if (enterState == SETTINGS_START | enterState == BACK_START) {
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

    Integer age;
    String pigmentType;
    int bedtimeHour;
    int bedtimeMin;
    int waketimeHour;
    int waketimeMin;

    public void onApplyClicked(View view) {
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
            if (enterState == SETUP_START | enterState == BACK_START) {
                Intent intent = new Intent(this, ExposureActivity.class);
                intent.putExtra("StartFrom", SETUP_START);
                startActivity(intent);
            } else if (enterState == SETTINGS_START) {
                new DataToServer().execute();
            }
            finish();
        }
    }

    public void onInfoClicked(View view) {
        DialogFragment dialog = new FitzpatrickDialogFragment();
        dialog.show(getSupportFragmentManager(), "FitzpatrickDialogFragment");
    }

    public static class FitzpatrickDialogFragment extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);
            builder.setTitle(R.string.fitzpatrick)
                    .setMessage(R.string.fitzpatrick_info)
                    .setPositiveButton("Back", (dialog, id) -> Objects.requireNonNull(FitzpatrickDialogFragment.this.getDialog()).cancel());
            return builder.create();
        }
    }

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

    @Override
    public void onBackPressed() {
        if (enterState == SETTINGS_START) {
            super.onBackPressed();
        } else {
            Intent intent = new Intent(this, WelcomeScreenActivity.class);
            intent.putExtra("StartFrom", BACK_START);
            startActivity(intent);
        }
    }
}