package com.feri.watchmyparent.mobile.domain.enums;

public enum CriticalityLevel {
    CRITICAL(30),      // 30 seconds
    IMPORTANT(120),    // 2 minutes
    REGULAR(300),      // 5 minutes
    LONG_TERM(900);    // 15 minutes

    private final int defaultFrequencySeconds;

    CriticalityLevel(int defaultFrequencySeconds) {
        this.defaultFrequencySeconds = defaultFrequencySeconds;
    }

    public int getDefaultFrequencySeconds() {
        return defaultFrequencySeconds;
    }
}