package com.example.lumatik;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "com.example.lumatik.MainActivity";
    private static final int SETTINGS_START = 0;
    private static final int BT_REQUEST = 0;

    Intent dtsIntent;

    ProgressBar pb;
    TextView tv;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        pb = findViewById(R.id.progressBar2);
        tv = findViewById(R.id.VitDvalue);
        dtsIntent = new Intent(this, DataTransferForegroundService.class);
        if(getIntent().getBooleanExtra("startBT", false)) {
            startBluetoothService();
        }
        Context context = getApplicationContext();
        //preference listener on change function
        preferenceChangeListener = (sharedPreferences, key) -> {
            Log.i(TAG, "Prefs changed: " + key);
            if (key.equals("VitDval")) {
                Log.i(TAG, "Preference changed: "+ key);
                vitDChanged();
            }
        };
        SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        if (isMyServiceRunning(DataTransferForegroundService.class)) {
            if (!getIntent().getBooleanExtra("startBT", false)) {
                vitDChanged();
            }
        }
    }

    private void startBluetoothService() {
        ContextCompat.startForegroundService(this, dtsIntent);
    }

    @SuppressLint("SetTextI18n")
    private void vitDChanged() {
        Context context = getApplicationContext();
        SharedPreferences sp = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        float vitD = sp.getFloat("VitDval", 0.0f);
        // Round progress to nearest percent
        int vitD_percent = Math.min(Math.round((vitD/400)*100), 100);
        Log.i(TAG, "VitD: " + vitD + " "+ vitD_percent);
        pb.setProgress(vitD_percent, true);
        if (vitD_percent < 50) {
            tv.setText("Vitamin D goal: go get some sun!");
        } else if (vitD_percent < 80) {
            tv.setText("Vitamin D goal: half way!");
        } else if (vitD_percent < 100) {
            tv.setText("Vitamin D goal: nearly there!");
        } else if (vitD_percent == 100) {
            tv.setText("Vitamin D goal: complete!");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "Intent Returned: " + requestCode);
        if (requestCode == BT_REQUEST) {
            if (resultCode == RESULT_OK) {
                startBluetoothService();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId") // Note to self, R.id will be non-final in later gradle builds, ie cannot use in switch-case
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_bluetooth:
                if (!isMyServiceRunning(DataTransferForegroundService.class)) {
                    intent = new Intent(this, DeviceListActivity.class);
                    intent.putExtra("StartFrom", SETTINGS_START);
                    startActivityForResult(intent, BT_REQUEST);
                }
                return true;
            case R.id.action_profile:
                intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("StartFrom", SETTINGS_START);
                startActivity(intent);
                return true;
            case R.id.action_exposure:
                intent = new Intent(this, ExposureActivity.class);
                intent.putExtra("StartFrom", SETTINGS_START);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    @Override
    protected void onDestroy() {
        if (preferenceChangeListener != null){
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
            preferenceChangeListener = null;
        }
        super.onDestroy();
    }
}