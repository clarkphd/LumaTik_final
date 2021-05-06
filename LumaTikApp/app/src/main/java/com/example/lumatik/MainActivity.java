package com.example.lumatik;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "com.example.lumatik.MainActivity";
    private static final int SETTINGS_START = 0;
    private static final int BT_REQUEST = 0;

    Intent dtsIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        dtsIntent = new Intent(this, DataTransferForegroundService.class);
        if(getIntent().getBooleanExtra("startBT", false)) {
            startBluetoothService();
        }
    }

    private void startBluetoothService() {
        ContextCompat.startForegroundService(this, dtsIntent);
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
}