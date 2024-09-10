package com.emicasolutions.eventdetector;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private EditText phoneNumbersEditText;
    private Switch sendSmsSwitch, sendLocationSwitch;
    private AutoCompleteTextView triggerCodesAutoComplete;
    private RecyclerView triggerRecyclerView;
    private TriggerAdapter triggerAdapter;
    private List<TriggerItem> triggerList;
    private Switch makeCallSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        phoneNumbersEditText = findViewById(R.id.phone_numbers);
        sendSmsSwitch = findViewById(R.id.send_sms_switch);
        makeCallSwitch = findViewById(R.id.make_call_switch);
        sendLocationSwitch = findViewById(R.id.send_location_switch);
        triggerCodesAutoComplete = findViewById(R.id.auto_complete_triggers);
        triggerRecyclerView = findViewById(R.id.trigger_recycler_view);
        Button saveButton = findViewById(R.id.save_button);

        // Set input type for phone numbers
        phoneNumbersEditText.setInputType(InputType.TYPE_CLASS_PHONE);

        // Load settings from SharedPreferences and initialize the adapter accordingly
        loadSettings();

        saveButton.setOnClickListener(v -> saveSettings());
        setupAutoCompleteTextView();
    }
    private void setupAutoCompleteTextView() {
        List<String> triggerNames = new ArrayList<>();
        for (TriggerItem item : triggerList) {
            triggerNames.add(item.getDisplayName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, triggerNames);

        triggerCodesAutoComplete.setAdapter(adapter);
        triggerCodesAutoComplete.setDropDownHeight(0);


        triggerCodesAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTriggerName = (String) parent.getItemAtPosition(position);

            // Find the corresponding TriggerItem and check it in the RecyclerView
            for (TriggerItem item : triggerList) {
                if (item.getDisplayName().equals(selectedTriggerName)) {
                    triggerAdapter.checkTrigger(item.getIndex());
                    break;
                }
            }

            // Optionally, clear the AutoCompleteTextView input after selection
            triggerCodesAutoComplete.setText("");
        });

        // Add listener for typing text to dynamically filter triggers in the RecyclerView
        triggerCodesAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecyclerViewTriggers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void filterRecyclerViewTriggers(String query) {
        List<TriggerItem> filteredList = new ArrayList<>();
        for (TriggerItem item : triggerList) {
            if (item.getDisplayName().toLowerCase().startsWith(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        triggerAdapter.updateTriggerList(filteredList);
    }

    private List<TriggerItem> loadTriggersFromCsv(Context context) {
        List<TriggerItem> triggers = new ArrayList<>();
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open("labels.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("index")) continue; // Skip header
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    try {
                        int index = Integer.parseInt(parts[0].trim());
                        String displayName = parts[2].trim().replace("\"", "");
                        triggers.add(new TriggerItem(index, displayName));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Error parsing CSV", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error loading triggers", Toast.LENGTH_SHORT).show();
        }
        return triggers;
    }

    private void loadSettings() {
        String settingsJson = sharedPreferences.getString("settings", null);

        // Load trigger codes from CSV file
        triggerList = loadTriggersFromCsv(this);

        if (settingsJson != null) {
            Settings settings = Settings.fromJson(settingsJson);
            phoneNumbersEditText.setText(settings.getPhoneNumbers());
            sendSmsSwitch.setChecked(settings.isSendSMS());
            sendLocationSwitch.setChecked(settings.isSendLocation());

            // Load selected triggers from settings
            List<Integer> selectedIndexes = settings.getTriggerIndexes();
            List<TriggerItem> reorderedTriggers = reorderTriggers(triggerList, selectedIndexes);

            // Initialize adapter with reordered list and preselected triggers
            triggerAdapter = new TriggerAdapter(this,reorderedTriggers, selectedIndexes);
        } else {
            // If no settings are found, load triggers normally
            triggerAdapter = new TriggerAdapter(this, triggerList);
        }

        triggerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        triggerRecyclerView.setAdapter(triggerAdapter);
    }

    private void saveSettings() {
        String phoneNumbers = phoneNumbersEditText.getText().toString();
        boolean sendSms = sendSmsSwitch.isChecked();
        boolean sendLocation = sendLocationSwitch.isChecked();
        boolean makeCall = makeCallSwitch.isChecked();
        List<Integer> selectedIndexes = triggerAdapter.getSelectedTriggerIndexes();
        String triggerCodes = new JSONArray(selectedIndexes).toString();

        // Create Settings object and save to SharedPreferences
        Settings settings = new Settings(sendLocation, true, phoneNumbers, triggerCodes, sendSms, makeCall, 60000);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("settings", settings.toJson());
        editor.apply();

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();

        // Go back to MainActivity
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private List<TriggerItem> reorderTriggers(List<TriggerItem> triggerList, List<Integer> selectedIndexes) {
        List<TriggerItem> reorderedList = new ArrayList<>();

        // Add selected triggers first
        for (TriggerItem item : triggerList) {
            if (selectedIndexes.contains(item.getIndex())) {
                reorderedList.add(item);
            }
        }

        // Add non-selected triggers
        for (TriggerItem item : triggerList) {
            if (!selectedIndexes.contains(item.getIndex())) {
                reorderedList.add(item);
            }
        }

        return reorderedList;
    }


}
