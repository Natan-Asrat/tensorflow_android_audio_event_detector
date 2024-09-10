package com.emicasolutions.eventdetector;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.provider.Settings.Secure;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import org.tensorflow.lite.DataType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class BackgroundService extends Service {

    private static final String TAG = "BackgroundService";
    private static final int RECORDING_PERIOD_MS = 5000; // 5 seconds
    private static final int SAMPLE_SIZE = 15600; // Number of samples the model expects
    private FusedLocationProviderClient fusedLocationClient;
    private long lastSmsTime = 0;
    private long lastCallTime = 0;
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 62400; // Set to the size expected by the model
    private static final int NUM_CLASSES = 521; // Number of classes in YAMNet
    private static final String MODEL_PATH = "model.tflite";
    private static final String SECRET_KEY = "your-secret-key"; // Replace with your secret key
    private String[] triggerCodeArray ;
    private Handler handler;
    private Runnable runnable;
    private Interpreter interpreter;
    private AudioRecord audioRecord;
    private SharedPreferences sharedPreferences;
    private Settings settings;
    private String latitude = "";
    private String longitude = "";

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getCurrentLocation();

        // Load settings
        String settingsJson = sharedPreferences.getString("settings", "{}");
        settings = Settings.fromJson(settingsJson);
        try {
            String triggerCodesJson = settings.getTriggerCodes(); // Assuming this returns a JSON string
            triggerCodeArray = new Gson().fromJson(triggerCodesJson, String[].class); // Parsing the JSON array into a String array
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse trigger codes", e);
            triggerCodeArray = new String[0]; // Handle parse failure by setting an empty array
        }        // Initialize YAMNet
        Log.i(TAG, "Loaded settings: " + settingsJson);

        // Initialize and log trigger codes
        Log.i(TAG, "Trigger codes: " + Arrays.toString(triggerCodeArray));

        // Log phone numbers
        List<String> phoneNumberList = Arrays.asList(settings.getPhoneNumbers().split("/"));
        Log.i(TAG, "Registered phone numbers: " + phoneNumberList.toString());

        try {
            MappedByteBuffer tfliteModel = loadModelFile();
            interpreter = new Interpreter(tfliteModel);
        } catch (IOException e) {
            Log.e("model", "couldnt load into tflie: " + e);
            e.printStackTrace();
        }
        Log.i("model", "initialized ");
        int[] inputShape = interpreter.getInputTensor(0).shape();
        Log.i("model", "Input shape: " + Arrays.toString(inputShape));

        // Initialize AudioRecord
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
        );

        runnable = new Runnable() {
            @Override
            public void run() {
                // Record audio and classify it
                recordAndClassifyAudio();

                handler.postDelayed(this, RECORDING_PERIOD_MS);
            }
        };
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY; // Ensure the service restarts if it's killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        if (interpreter != null) {
            interpreter.close();
        }
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetManager assetManager = this.getAssets();
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd("model.tflite")) {
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            // Initialize TensorFlow Lite interpreter
        } catch (IOException e) {
            Log.e("model", "couldnt load model: " + e);
            e.printStackTrace();
        }
        Log.e("model", "model is none");
        return null;

    }

    private void recordAndClassifyAudio() {
        // Allocate buffer for audio data
        short[] audioData = new short[SAMPLE_SIZE];

        // Start recording
        audioRecord.startRecording();

        // Read audio data
        int readSize = audioRecord.read(audioData, 0, SAMPLE_SIZE);

        // Stop recording
        audioRecord.stop();

        if (readSize != SAMPLE_SIZE) {
            Log.e(TAG, "Failed to read the expected amount of audio data");
            return;
        }

        // Convert audio data to a float array
        float[] inputArray = new float[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            inputArray[i] = audioData[i] / 32768.0f; // Normalize to [-1, 1]
        }

        // Create TensorBuffer for input data
        TensorBuffer inputBuffer = TensorBuffer.createFixedSize(new int[]{1, SAMPLE_SIZE}, DataType.FLOAT32);
        inputBuffer.loadArray(inputArray);

        // Perform classification
        float[][] output = new float[1][521]; // Adjust output size to 521 classes
        interpreter.run(inputBuffer.getBuffer(), output);

        // Determine the result
        int predictedClass = getMaxIndex(output[0]);
        String detectedSound = MainActivity.labelsMap.getOrDefault(predictedClass, "Unknown");

        // Send broadcast with detected sound
        sendBroadcast(String.valueOf(predictedClass),detectedSound);
        Log.i("model", "Alert: Detected event with code " + predictedClass + " sound " + detectedSound);
        // Save and handle the recorded audio based on settings
        String predictedClassString = String.valueOf(predictedClass);

        if (Arrays.asList(triggerCodeArray).contains(predictedClassString)) {
            if (settings.isSaveRecording()) {
                saveAudioToFile(audioData, predictedClass);
            }
            if (settings.isSendSMS()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSmsTime >= settings.getINTERVAL_TIME_MS()) {
                    sendSms(predictedClass);
                    lastSmsTime = currentTime; // Update last SMS time
                } else {
                    Log.d(TAG, "SMS skipped, waiting for interval to pass");
                }
            }
            if (settings.isMakeCall()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCallTime >= settings.getINTERVAL_TIME_MS()) {
                    makeCall();
                    lastCallTime = currentTime; // Update last call time
                } else {
                    Log.d(TAG, "Call skipped, waiting for interval to pass");
                }
            }
        }
        getCurrentLocation();
    }


    private int getMaxIndex(float[] output) {
        int maxIndex = 0;
        for (int i = 1; i < output.length; i++) {
            if (output[i] > output[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private void saveAudioToFile(short[] audioData, int detectedCode) {
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String androidID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        String locationString = "";

        if (settings.isSendLocation()) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    locationString = location.getLatitude() + "_" + location.getLongitude();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Location permissions not granted", e);
            }
        }

        String hash = convertToHex(generateHash(detectedCode));
        String filename = String.format("%s_%d_%s_%s_%s.pcm", date, detectedCode, androidID, locationString, hash);
        File file = new File(getFilesDir(), filename);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (short sample : audioData) {
                fos.write((sample & 0xFF)); // Write low byte
                fos.write((sample >> 8) & 0xFF); // Write high byte
            }
            Log.d(TAG, "Audio saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendSms(int detectedCode) {
        List<String> phoneNumberList = Arrays.asList(settings.getPhoneNumbers().split("/"));
        String detectedSound = MainActivity.labelsMap.getOrDefault(detectedCode, "Unknown");

        // Create YAML message
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        Map<String, Object> alertMap = new HashMap<>();
        alertMap.put("detected_code", detectedCode);
        alertMap.put("detected_sound", detectedSound);
        Map<String, Object> locationMap = new HashMap<>();
        if (settings.isSendLocation()) {

                    locationMap.put("latitude", latitude);
                    locationMap.put("longitude", longitude);
        }
        alertMap.put("location", locationMap);

        String androidIDString = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        alertMap.put("android_id", androidIDString);

        String hash = generateHash(detectedCode);
        String hex = convertToHex(hash);
        alertMap.put("hash", hex);

        String yamlMessage = yaml.dump(alertMap);

        SmsManager smsManager = SmsManager.getDefault();
        for (String phoneNumber : phoneNumberList) {
            Log.i(TAG, yamlMessage);
            smsManager.sendTextMessage(phoneNumber, null, yamlMessage, null, null);
            Log.d(TAG, "SMS sent to " + phoneNumber);
        }
    }

    private void makeCall() {
        List<String> phoneNumberList = Arrays.asList(settings.getPhoneNumbers().split("/"));
        if (!phoneNumberList.isEmpty()) {
            String phoneNumber = phoneNumberList.get(0);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));

            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            } else {
                Log.e(TAG, "Call permission not granted");
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location location = task.getResult();
                                latitude = String.valueOf(location.getLatitude());
                                longitude = String.valueOf(location.getLongitude());
                            } else {
                                Log.d("CurrentLocation", "Failed to get current location.");
                            }
                        }
                    });
        }
    }

    private String generateHash(int detectedCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String text = detectedCode + SECRET_KEY;
            byte[] hash = digest.digest(text.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Hash generation error", e);
            return "";
        }
    }

    private String convertToHex(String hash) {
        StringBuilder sb = new StringBuilder();

        for (char b : hash.toCharArray()) {
            sb.append(String.format("%02x", (int) b));
        }
        String hexString =  sb.toString();
        return hexString.substring(0, Math.min(hexString.length(), 8));
    }
    private void sendBroadcast(String detectedCode, String detectedSound) {
        Intent intent = new Intent("com.emicasolutions.eventdetector.UPDATE_UI");
        intent.putExtra("detected_sound", detectedSound);
        intent.putExtra("detected_code", detectedCode);
        sendBroadcast(intent);
    }

}
