package com.example.callcenter.Entity;

import lombok.Getter;

@Getter
public enum ContactStatus {
    NOT_CONTACTED("Not Contacted"),
    CONTACTED_AVAILABLE("Contacted - Available"),
    CONTACTED_UNAVAILABLE("Contacted - Unavailable"),
    NO_ANSWER("No Answer"),
    WRONG_NUMBER("Wrong Number"),
    CALL_BACK_LATER("Call Back Later");

    private final String label;

    ContactStatus(String label) {
        this.label = label;
    }
} 