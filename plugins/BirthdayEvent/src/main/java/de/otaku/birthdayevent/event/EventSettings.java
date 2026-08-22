package de.otaku.birthdayevent.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public record EventSettings(
        ZoneId zone,
        LocalDate date,
        LocalTime startTime,
        int durationHours,
        int age,
        String birthdayPlayerName
) {
    public static EventSettings defaults() {
        return new EventSettings(
                ZoneId.of("Europe/Berlin"),
                LocalDate.of(2026, 8, 22),
                LocalTime.MIDNIGHT,
                24,
                15,
                "BirthdayPlayer"
        );
    }
}
