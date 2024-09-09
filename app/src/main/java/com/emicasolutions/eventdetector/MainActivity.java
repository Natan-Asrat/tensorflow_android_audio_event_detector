package com.emicasolutions.eventdetector;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    static Map<Integer, String> labelsMap;

    private static final String ACTION_UPDATE_UI = "com.emicasolutions.eventdetector.UPDATE_UI";
    private TextView labelTextView;
    private SharedPreferences sharedPreferences;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Handle system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        labelTextView = findViewById(R.id.label);

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_UPDATE_UI);
        registerReceiver(updateUIReceiver, filter);
        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String settingsJson = sharedPreferences.getString("settings", null);
        if (settingsJson == null) {
            // If settings are not set, go to SettingsActivity
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish(); // Optionally finish MainActivity if you want to prevent going back
            return;
        }

        // Add button to go to SettingsActivity
        Button settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        labelsMap = loadLabels(this);
        Log.i("model", "label 0 is " + labelsMap.get(0));
        // Check and request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.CALL_PHONE
            });
        } else {
            // Permissions already granted, start the service
            saveSettingsAndStartService();
        }
    }

    private final BroadcastReceiver updateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String detectedSound = intent.getStringExtra("detected_sound");
                if (detectedSound != null) {
                    labelTextView.setText(detectedSound);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateUIReceiver);
    }

    private static Map<Integer, String> loadLabels(Context context) {
        Map<Integer, String> labelsMap = new HashMap<>();
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open("labels.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip header
                if (line.startsWith("index")) continue;

                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    try {
                        int index = Integer.parseInt(parts[0].trim());
                        String displayName = parts[2].trim().replace("\"", "");
                        labelsMap.put(index, displayName);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing CSV line: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading labels.csv", e);
        }
        return labelsMap;
    }
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean locationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean audioGranted = result.get(Manifest.permission.RECORD_AUDIO);
                Boolean smsGranted = result.get(Manifest.permission.SEND_SMS);
                Boolean callGranted = result.get(Manifest.permission.CALL_PHONE);

                if (Boolean.TRUE.equals(locationGranted) && Boolean.TRUE.equals(audioGranted) && Boolean.TRUE.equals(smsGranted) && Boolean.TRUE.equals(callGranted)) {
                    // Start the background service if all permissions are granted
                    saveSettingsAndStartService();
                } else {
                    Log.e(TAG, "Location, microphone, SMS, or CALL permissions not granted");
                    Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show();
                }
            });

    private void saveSettingsAndStartService() {
        // Save settings to SharedPreferences with SMS enabled

        // Start the background service
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        startService(serviceIntent);
        Toast.makeText(this, "Background service started", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Background service started");
    }

    private void saveSettings(Settings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("settings", settings.toJson());
        editor.apply();
        Log.d(TAG, "Settings saved in SharedPreferences");
    }
}
