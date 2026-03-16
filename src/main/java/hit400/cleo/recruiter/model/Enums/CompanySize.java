package hit400.cleo.recruiter.model.Enums;


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

    public String getHeadcountRange() {
        return headcountRange;
    }

    public String getDisplayName() {
        return displayName;
    }
}
