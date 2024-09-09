package com.emicasolutions.eventdetector;

public class TriggerItem {
    private final int index;
    private final String displayName;

    public TriggerItem(int index, String displayName) {
        this.index = index;
        this.displayName = displayName;
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayName() {
        return displayName;
    }
}

