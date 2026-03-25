package ink.yowyob.geofence.Enum;

public enum AlertTypeEnum {
    ZONE_EXIT,
    ZONE_ENTER,
    SPEED_LIMIT,
    BATTERY_LOW,
    SYSTEM_ERROR,
    // Nouveaux types pour géofence intelligente
    ZONE_TEMPORAL_VIOLATION,  // Zone inactive à cette heure
    ZONE_SPEED_VIOLATION,     // Vitesse dépassée dans la zone
    ZONE_DWELL_TIME_EXCEEDED, // Temps de séjour dépassé
    ZONE_DWELL_TIME_INSUFFICIENT // Temps de séjour insuffisant
}
