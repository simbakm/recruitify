package hit400.cleo.recruiter.model.Enums;


import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum CompanySize {
    STARTUP("1-50", "Startup"),
    SMALL("51-200", "Small"),
    MEDIUM("201-1000", "Medium"),
    LARGE("1001-10000", "Large"),
    ENTERPRISE("10000+", "Enterprise");

    private final String headcountRange;
    private final String displayName;

    CompanySize(String headcountRange, String displayName) {
        this.headcountRange = headcountRange;
        this.displayName = displayName;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CompanySize fromJson(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;

        for (CompanySize size : values()) {
            if (size.name().equalsIgnoreCase(normalized)) {
                return size;
            }
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        for (CompanySize size : values()) {
            if (size.displayName != null && size.displayName.toLowerCase(Locale.ROOT).equals(lower)) {
                return size;
            }
            if (size.headcountRange != null && size.headcountRange.toLowerCase(Locale.ROOT).equals(lower)) {
                return size;
            }
        }

        // Frontend sends values like "1-10 Employees" etc. Accept those too.
        if (lower.contains("1-10") || lower.contains("11-50") || lower.contains("1-50")) {
            return STARTUP;
        }
        if (lower.contains("51-100") || lower.contains("51-200")) {
            return SMALL;
        }
        if (lower.contains("101-500") || lower.contains("201-1000")) {
            return MEDIUM;
        }
        if (lower.contains("500+") || lower.contains("1001-10000")) {
            return LARGE;
        }
        if (lower.contains("10000+")) {
            return ENTERPRISE;
        }

        throw new IllegalArgumentException("Unknown company size: " + value);
    }

    public String getHeadcountRange() {
        return headcountRange;
    }

    public String getDisplayName() {
        return displayName;
    }
}
