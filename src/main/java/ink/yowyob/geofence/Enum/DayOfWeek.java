package ink.yowyob.geofence.Enum;

public enum DayOfWeek {
    MONDAY("Lundi"),
    TUESDAY("Mardi"),
    WEDNESDAY("Mercredi"),
    THURSDAY("Jeudi"),
    FRIDAY("Vendredi"),
    SATURDAY("Samedi"),
    SUNDAY("Dimanche");

    private final String displayName;

    DayOfWeek(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convertit un java.time.DayOfWeek vers notre enum personnalisé
     */
    public static DayOfWeek from(java.time.DayOfWeek javaDay) {
        return switch (javaDay) {
            case MONDAY -> MONDAY;
            case TUESDAY -> TUESDAY;
            case WEDNESDAY -> WEDNESDAY;
            case THURSDAY -> THURSDAY;
            case FRIDAY -> FRIDAY;
            case SATURDAY -> SATURDAY;
            case SUNDAY -> SUNDAY;
        };
    }

    /**
     * Convertit vers un java.time.DayOfWeek
     */
    public java.time.DayOfWeek toJavaDayOfWeek() {
        return switch (this) {
            case MONDAY -> java.time.DayOfWeek.MONDAY;
            case TUESDAY -> java.time.DayOfWeek.TUESDAY;
            case WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY;
            case THURSDAY -> java.time.DayOfWeek.THURSDAY;
            case FRIDAY -> java.time.DayOfWeek.FRIDAY;
            case SATURDAY -> java.time.DayOfWeek.SATURDAY;
            case SUNDAY -> java.time.DayOfWeek.SUNDAY;
        };
    }
}