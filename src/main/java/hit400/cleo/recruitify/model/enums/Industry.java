package hit400.cleo.recruitify.model.enums;


import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum Industry {
    TECHNOLOGY("Technology"),
    FINANCE("Finance"),
    HEALTHCARE("Healthcare"),
    RETAIL("Retail"),
    MANUFACTURING("Manufacturing"),
    EDUCATION("Education"),
    MEDIA("Media"),
    TRANSPORTATION("Transportation"),
    ENERGY("Energy"),
    OTHER("Other");

    private final String displayName;

    Industry(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Industry fromJson(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;

        for (Industry industry : values()) {
            if (industry.name().equalsIgnoreCase(normalized)) {
                return industry;
            }
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        for (Industry industry : values()) {
            if (industry.displayName != null && industry.displayName.toLowerCase(Locale.ROOT).equals(lower)) {
                return industry;
            }
        }

        // Industry is currently a free-text field in the frontend; fall back instead of 400-ing.
        return OTHER;
    }

    public String getDisplayName() {
        return displayName;
    }
}
