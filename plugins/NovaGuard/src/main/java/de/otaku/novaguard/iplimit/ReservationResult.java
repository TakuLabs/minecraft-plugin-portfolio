package de.otaku.novaguard.iplimit;

public record ReservationResult(boolean allowed, int active, int reserved, int limit) {
    public static ReservationResult allowed(int active, int reserved, int limit) {
        return new ReservationResult(true, active, reserved, limit);
    }

    public static ReservationResult denied(int active, int reserved, int limit) {
        return new ReservationResult(false, active, reserved, limit);
    }
}
