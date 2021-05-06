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
import android.icu.text.DecimalFormat;
import android.icu.text.NumberFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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


    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = null;
    private boolean dataToSend = false;

    private boolean isLEDOn = false;

    public DataTransferForegroundService() {
        Log.i(TAG, "Service constructor");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lumatik Connected!")
                .setContentText("Periodically obtaining data from your Lumatik Device.")
                .setSmallIcon(R.drawable.bluetoothbutton)
                .build();

        Context context = getApplicationContext();
        //preference listener on change function
        preferenceChangeListener = (sharedPreferences, key) -> {
            if (!key.equals("bt_address") & !key.equals("isBTConnected")) {
                Log.i(TAG, "Preference changed: "+ key);
                prefsChanged();
            }
        };
        SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        address = sharedPreferences.getString("bt_address", "");
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        startForeground(1, notification);
        startTimerTasks();

        return START_STICKY;
    }

    private void prefsChanged() {
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Lumatik Notification Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimerTask();
        if (preferenceChangeListener != null){
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
            preferenceChangeListener = null;
        }
        Log.i(TAG, "Service Destroyed.");
    }

    @SuppressWarnings("UnusedDeclaration")
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
    private TimerTask parseAndSendTimerTask;
    private TimerTask notificationTimerTask;

    private void startTimerTasks() {
        Log.i(TAG, "Service start timer task.");
        ConnectThread connectThread = new ConnectThread();
        connectThread.start();
        // Only need one timer, saves resources
        timer = new Timer();
        initialiseParseAndSendTimerTask();
        timer.schedule(parseAndSendTimerTask, 10000, 300000); //set period to 300000 (5 mins) delay to 10000
        initialiseNotificiationTimerTask();
        timer.schedule(notificationTimerTask, 300000, 1800000); //set period to 1800000 delay to 300000
    }

    private void initialiseParseAndSendTimerTask() {
        Log.i(TAG, "Parse and Send timer task initialising.");
        parseAndSendTimerTask = new TimerTask() {
            public void run() {
                Log.i(TAG, "Service is in state: " + state);
                //toggleLed(); //Removed once happy service works properly, kept for later debugging if needed

                // actual work here
                try {
                    readBluetoothAndParse();
                } catch (IOException e) {
                    Log.e(TAG, "Caught exception: " + e.getMessage() + ". Stopping service.");
                    Context context = getApplicationContext();
                    SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.bt_connected), false);
                    editor.apply();
                    stopSelf();
                }
                if(dataToSend) {
                    try {
                        Log.i(TAG, "Data to send!");
                        sendDataToServer();
                        dataToSend = false;
                    } catch (IOException e) {
                        // we dont stop the service if failing to send, the phone can buffer up data till internet connection is restored
                        Log.e(TAG, "Exception caught: " + e.getMessage());
                    }
                }
            }
        };
    }

    private void initialiseNotificiationTimerTask() {
        Log.i(TAG, "Notification timer task initialising.");
        notificationTimerTask = new TimerTask() {
            public void run() {
                Context context = getApplicationContext();
                SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                float vitD = sharedPreferences.getFloat("VitDval", 0.0f);
                int vitD_percent = Math.min(Math.round((vitD/400)*100), 100);
                String text = "";
                // These strings will work better with weather information integrated, can make better recommendations if we know if its sunny or not
                if (vitD_percent < 50) {
                    text = "Time to go get some sun and hit your vitamin D goal!";
                } else if (vitD_percent < 80) {
                    text = "You're over half way to your goal, keep on grabbing the sunlight when you can!";
                } else if (vitD_percent < 100) {
                    text = "You're nearly there, time to catch those last few rays!";
                }

                // make notification if vit D below 400
                if (!text.equals("")) {
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                    Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle("Vitamin D Progress Update!")
                            .setSmallIcon(R.mipmap.lumatik_logo)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                            .setOnlyAlertOnce(true)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build();

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    notificationManager.notify(4, notification);
                }
            }
        };
    }



	private final ArrayList<JSONObject> parsedData = new ArrayList<>();

    private char state = 'S'; //S start, R red, G green, B blue, A uva, V uvb, I uvi, U iu, T time, E end
    private byte[] rcs = new byte[10];
    private int count = 0;

    private HashMap<String, Object> currentData = new HashMap<>();

    public void readBluetoothAndParse() throws IOException{
        while (sspInputStream.available() > 0) {
			byte rc = (byte) sspInputStream.read();
			if (rc == -1) {
			    break;
            }
            //look for start marker
			switch (state) {
                case 'S':
                    if (rc == 'R') {
                        currentData = new HashMap<>();
                        currentData.put("UserID", "http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/User/51/");
                        currentData.put("DeviceID", "http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/Device/51/");
                        state = 'R';
                    }
                    if (rc == 'W') {
                        state = 'W';
                    }
                    break;

                case 'R':
                    if (rc != 'G') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        currentData.put("Rval", Integer.valueOf(new String(rcs, 0, count)));
                        rcs = new byte[10];
                        count = 0;
                        state = 'G';
                    }
                    break;

                case 'G':
                    if (rc != 'B') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        currentData.put("Gval", Integer.valueOf(new String(rcs, 0, count)));
                        rcs = new byte[10];
                        count = 0;
                        state = 'B';
                    }
                    break;

                case 'B':
                    if (rc != 'A') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        currentData.put("Bval", Integer.valueOf(new String(rcs, 0, count)));
                        rcs = new byte[10];
                        count = 0;
                        state = 'A';
                    }
                    break;

                case 'A':
                    if (rc != 'V') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        currentData.put("UVAval", Float.valueOf(new String(rcs, 0, count)));
                        rcs = new byte[10];
                        count = 0;
                        state = 'V';
                    }
                    break;

                case 'V':
                    if (rc != 'I') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        currentData.put("UVBval", Float.valueOf(new String(rcs, 0, count)));
                        rcs = new byte[10];
                        count = 0;
                        state = 'I';
                    }
                    break;

                case 'I':
                    if (rc != 'U') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        currentData.put("UVIndex", Float.valueOf(new String(rcs, 0, count)));
                        rcs = new byte[10];
                        count = 0;
                        state = 'U';
                    }
                    break;

                case 'U':
                    if (rc != 'T') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        float vitD = Float.parseFloat(new String(rcs, 0, count));
                        currentData.put("VitDval", vitD);

                        // Store so we can display goal in main screen.
                        Context context = getApplicationContext();
                        SharedPreferences sp = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putFloat("VitDval", vitD);
                        editor.apply();

                        rcs = new byte[10];
                        count = 0;
                        state = 'T';
                    }
                    break;

                case 'T':
                    count += 1;
                    if (count == 1) {
                        currentData.put("seconds", (int) rc);
                    } else if (count == 2) {
                        currentData.put("minutes", (int) rc);
                    } else if (count == 3) {
                        currentData.put("hours", (int) rc);
                    } else if (rc == '>') {
                        Log.i(TAG, "Timestamp: " + currentData.get("hours") + ":" + currentData.get("minutes") + ":" + currentData.get("seconds"));
                        Calendar calendar = Calendar.getInstance();
                        int phoneHour = calendar.get(Calendar.HOUR_OF_DAY);
                        calendar.set(Calendar.HOUR_OF_DAY, (int) currentData.get("hours"));
                        calendar.set(Calendar.MINUTE, (int) currentData.get("minutes"));
                        calendar.set(Calendar.SECOND, (int) currentData.get("seconds"));
                        calendar.set(Calendar.MILLISECOND, 0);
                        long secondsSince = calendar.getTimeInMillis() / 1000;
                        if ((int) currentData.get("hours") == 23 & phoneHour == 0) {
                            secondsSince -= 86400; // minus 1 day of seconds to equate for phone being ahead of arduino send
                        }
                        currentData.remove("seconds");
                        currentData.remove("minutes");
                        currentData.remove("hours");
                        currentData.put("Writetime", secondsSince);
                        Log.i(TAG, "Calendar: "  + calendar.toString());
                        JSONObject jo = new JSONObject(currentData);
                        parsedData.add(jo);
                        rcs = new byte[10];
                        dataToSend = true;
                        count = 0;
                        state = 'S';
                    }
                    break;

                case 'W':
                    if (rc != '>') {
                        rcs[count] = rc;
                        count += 1;
                    } else {
                        String wCode = new String(rcs, 0, count);
                        if (wCode.equals("uv")) {
                            // Execute notification for UV reccomended reached and you should cover up
                            Log.i(TAG, "UV warning received");
                            createNotification(wCode);
                        } else if (wCode.equals("lu")) {
                            // Execute notification for "your light was above sensible sleeping levels just now"
                            // Set a timeout system here so we don't spam this message!
                            Log.i(TAG, "LU warning received");
                            createNotification(wCode);
                        }
                        count = 0;
                        rcs= new byte[10];
                        state = 'S';
                    }
			        break;

                default:
                    if (rc == '>') {
                        state = 'S';
                    }
                    break;
            }
        }
    }

    private void createNotification(String type) {
        String title = "Default.";
        String text = "Default.";
        int id = 0;
        if (type.equals("uv")) {
            title = "UV Goal Achieved!";
            text = "Well done, you've reached your UV goal today!T ime to avoid those rays before they get harmful. Head indoors, cover up, or put on some suncream! Tap to change your exposure.";
            id = 3;
        } else if (type.equals("lu")) {
            title = "Light warning!";
            text = "Light was detected during your sleep window.";
            id = 2;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.lumatik_logo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setOnlyAlertOnce(true);

        if (type.equals("uv")) {
            Intent intent = new Intent(this, ExposureActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("StartFrom", 3);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            builder.setContentIntent(pendingIntent);
            builder.setAutoCancel(true);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(id, builder.build());
    }

    private void sendDataToServer() throws IOException  {
        for (JSONObject j : parsedData) {
            URL url = new URL("http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/Data/");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            Log.i(TAG, "Data: "+ j.toString());
            String s = j.toString().replace("\\", "");
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
        }
        parsedData.clear();
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

    // Is there a more efficient way to do this? 28-03 Who cares right now.
    private void setArduinoTime() throws IOException {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        ArrayList<Byte> bytes = new ArrayList<>();
        bytes.add("T".getBytes()[0]);
        bytes.add((byte) hour);
        bytes.add((byte) min);
        bytes.add((byte) second);
        sendBytesWithMarkers(bytes);
    }

    private void setArduinoFactors() throws IOException {
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        float exposure = sharedPreferences.getFloat("exposureFactor", 0.000f);
        String pigment = sharedPreferences.getString("pigment", "Type V");
        int age = sharedPreferences.getInt("age", 0);
        Log.i(TAG,"Sent factors, Age: " + age + " Exposure: " + exposure + " Pigment: " + pigment);
        ArrayList<Byte> bytes = new ArrayList<>();
        bytes.add("P".getBytes()[0]);
        byte[] pigmentBytes = typeToByteArray(pigment);
        for (byte b : pigmentBytes) {
            bytes.add(b);
        }
        sendBytesWithMarkers(bytes);
        bytes.clear();
        bytes.add("A".getBytes()[0]);
        byte[] ageBytes = ageToByteArray(age);
        for (byte b : ageBytes) {
            bytes.add(b);
        }
        sendBytesWithMarkers(bytes);
        bytes.clear();
        bytes.add("E".getBytes()[0]);
        byte[] exposureBytes = exposureToByteArray(exposure);
        for (byte b : exposureBytes) {
            bytes.add(b);
        }
        sendBytesWithMarkers(bytes);
        bytes.clear();
        bytes.add("W".getBytes()[0]);
        bytes.add((byte) sharedPreferences.getInt("waketimeHour", 0));
        bytes.add((byte) sharedPreferences.getInt("waketimeMin", 0));
        sendBytesWithMarkers(bytes);
        bytes.clear();
        bytes.add("B".getBytes()[0]);
        bytes.add((byte) sharedPreferences.getInt("bedtimeHour", 0));
        bytes.add((byte) sharedPreferences.getInt("bedtimeMin", 0));
        sendBytesWithMarkers(bytes);
        bytes.clear();
        // Send following to tell Arduino it has all the data it needs to operate
        bytes.add("I".getBytes()[0]);
        sendBytesWithMarkers(bytes);
    }

    private byte[] typeToByteArray(String type) {
        switch (type) {
            case "Type I":
                return "1.0667".getBytes();
            case "Type II":
                return "1.0000".getBytes();
            case "Type III":
                return "0.8000".getBytes();
            case "Type IV":
                return "0.6095".getBytes();
            case "Type V":
                return "0.4267".getBytes();
            default:
                return "FAIL!".getBytes(); //THIS SHOULD NEVER HAPPEN
        }
    }

    private byte[] ageToByteArray(int age) {
        if (age <= 21) {
            return "1.00".getBytes();
        } else if (age <= 40) {
            return "0.83".getBytes();
        } else if (age <= 59) {
            return "0.66".getBytes();
        } else {
            return "0.49".getBytes();
        }
    }

    // exposure should come in format .3f
    private byte[] exposureToByteArray(float exposure) {
        NumberFormat formatter = new DecimalFormat("0.000");
        return formatter.format(exposure).getBytes();
    }

    // Last step in send process, adds markers for detecting a command window
    // "<" and ">" Ascii 60 and 62 should not conflict with other data we send
    private void sendBytesWithMarkers(List<Byte> bytes) throws IOException {
        sspOutputStream.write("<".getBytes());
        for (Byte b : bytes) {
            sspOutputStream.write(b);
        }
        sspOutputStream.write(">".getBytes());
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