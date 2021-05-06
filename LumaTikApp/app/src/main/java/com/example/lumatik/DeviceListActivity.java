package com.example.lumatik;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private static final String TAG = "com.example.lumatik.DeviceListActivity";

    private static final int SETUP_START = 1;
    private static final int SETTINGS_START = 0;
    private int enterState = SETUP_START;

    public static final String EXTRA_MESSAGE = "com.example.lumatikbasic.BT_ADDRESS";
    private static final int BT_ENABLE = 1;

    private BluetoothAdapter bluetoothAdapter = null;
    private Set<BluetoothDevice> bDevices;

    ListView deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        enterState = getIntent().getIntExtra("StartFrom", -1);
        if (enterState == -1) {
            Log.e(TAG, "Illegal enterState, ending app");
            finishAndRemoveTask();
        }

        deviceList = (ListView) findViewById(R.id.deviceList);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth Adapter Available, Lumatik Cannot Be Used.", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetoothEnable, BT_ENABLE);
        } else {
            checkBoundAndShow();
        }
    }

    public void onSkipClicked(View view) {
        if (enterState == SETUP_START) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("startBT", false);
            startActivity(intent);
            finish();
        } else if (enterState == SETTINGS_START) {
            setResult(RESULT_CANCELED);
            finish();
        }
        //TODO: set notification to remind user to connect to Lumatik
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                checkBoundAndShow();
            } else {
                Toast.makeText(getApplicationContext(), "Lumatik cannot communicate to mobile without Bluetooth.", Toast.LENGTH_LONG).show();
                if (enterState == SETUP_START) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("startBT", false);
                    startActivity(intent);
                    finish();
                } else if (enterState == SETTINGS_START) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    private void checkBoundAndShow() {
        bDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> list = new ArrayList();

        if (bDevices.size() > 0) {
            for (BluetoothDevice bt : bDevices) {
                list.add(bt.getName() + '\n' + bt.getAddress());
                Log.i(TAG, "Bluetooth Device Added to list");
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Devices Found.", Toast.LENGTH_LONG).show();
            if (enterState == SETUP_START) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("startBT", false);
                startActivity(intent);
                finish();
            } else if (enterState == SETTINGS_START) {
                setResult(RESULT_CANCELED);
                finish();
            }
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener(listClickListener);
    }

    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length()-17);
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("bt_address", address);
            editor.apply();
            if (enterState == SETUP_START) {
                Intent intent = new Intent(DeviceListActivity.this, MainActivity.class);
                intent.putExtra("startBT", true);
                startActivity(intent);
                finish();
            } else if (enterState == SETTINGS_START) {
                setResult(RESULT_OK);
                finish();
            }
        }
    };
}