package hit400.cleo.recruitify.model.enums;



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

    public String getDisplayName() {
        return displayName;
    }
}
