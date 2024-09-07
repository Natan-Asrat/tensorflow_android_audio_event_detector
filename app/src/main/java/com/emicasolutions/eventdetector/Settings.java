package com.emicasolutions.eventdetector;
import com.google.gson.Gson;

public class Settings {
    private boolean sendLocation;
    private boolean sendAndroidID;

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

    public Settings(boolean sendLocation, boolean sendAndroidID, String phoneNumbers, String triggerCodes, boolean sendSMS, boolean makeCall) {
        this.sendLocation = sendLocation;
        this.sendAndroidID = sendAndroidID;
        this.phoneNumbers = phoneNumbers;
        this.triggerCodes = triggerCodes;
        this.sendSMS = sendSMS;
        this.makeCall = makeCall;
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
