package com.workstation.modules.weight;

public enum TrendGranularity {
    DAY,
    MONTH,
    YEAR;

    public static TrendGranularity parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DAY;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("granularity 只支持 day / month / year");
        }
    }
}
