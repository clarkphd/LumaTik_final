package com.example.lumatik;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class DataTransferForegroundService extends Service {

    private static final String TAG = "DataTransferForegroundService";
    private static final String CHANNEL_ID = "DataTransferForeground";

    String address = null;
    BluetoothSocket sspSocket=null;
    BluetoothDevice sspDevice;
    OutputStream sspOutputStream;
    InputStream sspInputStream;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    byte[] pigmentFactor;
    byte[] exposureFactor;

    private boolean dataToSend = false;

    private boolean isLEDOn = false;
    private PendingIntent resultIntent;

    public DataTransferForegroundService() {
        Log.i(TAG, "Service constructor");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lumatik Connected!")
                .setContentText("Periodically obtaining data from your Lumatik Device.")
                .setSmallIcon(R.drawable.bluetoothbutton)
                .build();

        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        address = sharedPreferences.getString("bt_address", "");
        startForeground(1, notification);
        startTimerTask();

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth Connected Alert Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimerTask();
        Log.i(TAG, "Service Destroyed.");
    }

    //TODO: remove test function
    private void toggleLed() {
        try {
            String s = (isLEDOn) ? "<OF>" : "<ON>";
            Log.i(TAG, "Sent to Lumatik: " + s);
            sspOutputStream.write(s.getBytes());
            isLEDOn = !isLEDOn;
        } catch (IOException e) {
            Log.e(TAG, "Caught exception: " + e.getMessage());
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.bt_connected), false);
            editor.apply();
            stopSelf();
        }
    }

    private Timer timer;
    private TimerTask timerTask;

    public void startTimerTask() {
        Log.i(TAG, "Service start timer task.");
        ConnectThread connectThread = new ConnectThread();
        connectThread.start();
        timer = new Timer();
        initialiseTimerTask();
        timer.schedule(timerTask, 120000, 120000);
    }

    private int checkFileCounter = 0;
    //TODO within run run our transfer code (in thread? no this is a thread)
    //TODO every x counts of the timer task we should check the file to see if pig/exp updates needed
    public void initialiseTimerTask() {
        Log.i(TAG, "Service initialise timer task.");
        timerTask = new TimerTask() {
            public void run() {
                Log.i(TAG, "Service is actively processing!");
                toggleLed(); //TODO: Remove when happy

                // actual work here
                readBluetoothAndParse();
                if(dataToSend) {
                    try {
                        //sendToServer();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception caught: " + e.getMessage());
                    }
                }
                if (checkFileCounter < 5) {
                    checkFileCounter += 1;
                } else {
                    boolean ret = checkFile();
                    if (ret) {
                        try {
                            setArduinoFactors();
                        } catch (IOException e) {
                            Log.e(TAG, "Caught exception: " + e.getMessage() + ". Stopping service.");
                            Context context = getApplicationContext();
                            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(getString(R.string.bt_connected), false);
                            editor.apply();
                            stopSelf();
                        }

                    }
                }
            }
        };
    }

    public void readBluetoothAndParse() {
        //TODO
    }

    public void sendToServer() throws IOException  {
        URL url = new URL("http://sturdy-apricot-305816.ew.r.appspot.com/Data/");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        //TODO: finish up
    }

    public void stopTimerTask() {
        Log.i(TAG, "Service stop timer task.");
        try {
            if (sspSocket != null) {
                sspSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Close socket failed, exception: " + e.getMessage());
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean checkFile() {
        // compare current agefactor, pigmentfactor, exposurefactor with file saved ones
        // in case changed
        return false;
    }

    // Is there a more efficient way to do this?
    private void setArduinoTime() throws IOException {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        ArrayList<Byte> bytes = new ArrayList<>();
        bytes.add("T".getBytes(StandardCharsets.UTF_8)[0]);
        bytes.add((byte) hour);
        bytes.add((byte) min);
        bytes.add((byte) second);
        sendBytesWithMarkers(bytes);
    }

    //
    private void setArduinoFactors() throws IOException {
        //send agefactor
        //send exposurefactor - likely only this one changes
        //send skinfactor
    }

    // Last step in send process, adds markers for detecting a command window
    // "<" and ">" Ascii 60 and 62 should not conflict with other data we send
    private void sendBytesWithMarkers(List<Byte> bytes) throws IOException {
        sspOutputStream.write("<".getBytes(StandardCharsets.UTF_8));
        for (Byte b : bytes) {
            sspOutputStream.write(b);
        }
        sspOutputStream.write(">".getBytes(StandardCharsets.UTF_8));
    }

    //ConnectionThread
    private class ConnectThread extends Thread {

        public ConnectThread() {
            Log.i(TAG, "Starting Connect Thread");
        }

        @Override
        public void run() {
            Context context = getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                sspDevice = adapter.getRemoteDevice(address);
                sspSocket = sspDevice.createRfcommSocketToServiceRecord(uuid);
                sspSocket.connect();
                sspInputStream = sspSocket.getInputStream();
                sspOutputStream = sspSocket.getOutputStream();
                setArduinoTime();
                setArduinoFactors();
                editor.putBoolean(getString(R.string.bt_connected), true);
                toastMsg("BT is connected!");
                Log.i(TAG, "BT is connected!");
            } catch (IOException | IllegalArgumentException e) {
                Log.e(TAG, "Caught exception connecting BT: " + e.getMessage() + ". Stopping service.");
                toastMsg("BT Failed to connect :(");
                editor.putBoolean(getString(R.string.bt_connected), false);
                stopSelf();
            }
            editor.apply();
        }
    }

    private void toastMsg(String s) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show());
    }
}