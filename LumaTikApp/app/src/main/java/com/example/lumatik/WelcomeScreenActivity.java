package com.example.lumatik;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

//Activity contains a kill service button for debug usage
//Activity contains a new profile button for new user appearance
//Activity contains a use existing button for continued use

@SuppressWarnings("UnusedDeclaration")
public class WelcomeScreenActivity extends AppCompatActivity {


    private static final String TAG = "com.example.lumatik.WelcomeScreenActivity";

    private static final int SETUP_START = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);
    }

    public void onSetupClicked(View view) {
        killServiceClicked(view); // try to kill service as starting fresh
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("StartFrom", SETUP_START);
        startActivity(intent);
        finish();
    }

    public void onUseExistingClicked(View view) {
        //toastmsg("Not yet implemented."); // Yes it is?
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("startBT", false);
        startActivity(intent);
        finish();
    }

    public void killServiceClicked(View view) {
        if(isMyServiceRunning(DataTransferForegroundService.class)) { stopService(new Intent(getApplicationContext(), DataTransferForegroundService.class)); }
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

    private void toastmsg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}