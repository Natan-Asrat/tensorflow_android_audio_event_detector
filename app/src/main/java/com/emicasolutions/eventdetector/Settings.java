package com.emicasolutions.eventdetector;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Settings {
    private boolean sendLocation;
    private boolean sendAndroidID;

    public long getINTERVAL_TIME_MS() {
        return INTERVAL_TIME_MS;
    }

    public void setINTERVAL_TIME_MS(long INTERVAL_TIME_MS) {
        this.INTERVAL_TIME_MS = INTERVAL_TIME_MS;
    }

    private long INTERVAL_TIME_MS = 60000;

    public boolean isSendSMS() {
        return sendSMS;
    }

    public void setSendSMS(boolean sendSMS) {
        this.sendSMS = sendSMS;
    }

    public boolean isMakeCall() {
        return makeCall;
    }

    public void setMakeCall(boolean makeCall) {
        this.makeCall = makeCall;
    }

    public boolean isSaveRecording() {
        return saveRecording;
    }

    public void setSaveRecording(boolean saveRecording) {
        this.saveRecording = saveRecording;
    }

    private boolean sendSMS;
    private boolean makeCall;
    private boolean saveRecording;
    private String phoneNumbers;

    public String getTriggerCodes() {
        return triggerCodes;
    }

    public void setTriggerCodes(String triggerCodes) {
        this.triggerCodes = triggerCodes;
    }

    private String triggerCodes;

    public Settings(boolean sendLocation, boolean sendAndroidID, String phoneNumbers, String triggerCodes, boolean sendSMS, boolean makeCall, long INTERVAL_TIME_MS) {
        this.sendLocation = sendLocation;
        this.sendAndroidID = sendAndroidID;
        this.phoneNumbers = phoneNumbers;
        this.INTERVAL_TIME_MS = INTERVAL_TIME_MS;
        this.triggerCodes = triggerCodes;
        this.sendSMS = sendSMS;
        this.makeCall = makeCall;
    }
    public List<Integer> getTriggerIndexes() {
        List<Integer> triggerIndexes = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(triggerCodes); // assuming triggerCodes is stored as JSON string
            for (int i = 0; i < jsonArray.length(); i++) {
                triggerIndexes.add(jsonArray.getInt(i)); // Extract each integer from the JSON array
            }
        } catch (JSONException e) {
            e.printStackTrace(); // Handle error if JSON is invalid
        }
        return triggerIndexes;
    }

    public boolean isSendLocation() {
        return sendLocation;
    }

    public void setSendLocation(boolean sendLocation) {
        this.sendLocation = sendLocation;
    }

    public boolean isSendAndroidID() {
        return sendAndroidID;
    }

    public void setSendAndroidID(boolean sendAndroidID) {
        this.sendAndroidID = sendAndroidID;
    }

    public String getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    // Convert Settings object to JSON
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Convert JSON to Settings object
    public static Settings fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Settings.class);
    }
}
