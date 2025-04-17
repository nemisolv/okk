package com.vht.kafkamonitoring.dto;

public enum MonitoredState {
    RUNNING("RUNNING"),
    FAILED("FAILED"),
    UNASSIGNED("UNASSIGNED"),
    PAUSED("PAUSED"),
    OVERLOADED("OVERLOADED"),  // system overload
    ;

    private final String state;

    MonitoredState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public static MonitoredState fromString(String state) {
        for (MonitoredState monitoredState : MonitoredState.values()) {
            if (monitoredState.state.equalsIgnoreCase(state)) {
                return monitoredState;
            }
        }
        throw new IllegalArgumentException("No constant with text " + state + " found");
    }
}
