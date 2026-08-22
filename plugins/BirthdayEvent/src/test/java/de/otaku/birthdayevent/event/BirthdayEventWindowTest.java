package de.otaku.birthdayevent.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class BirthdayEventWindowTest {
    private final BirthdayEventWindow window = new BirthdayEventWindow(EventSettings.defaults());

    @Test
    void beforeMidnightIsNotActive() {
        assertFalse(window.isActive(berlin(2026, 8, 21, 23, 59, 59)));
    }

    @Test
    void midnightStartsEvent() {
        assertTrue(window.isActive(berlin(2026, 8, 22, 0, 0, 0)));
    }

    @Test
    void lastSecondOfBirthdayIsActive() {
        assertTrue(window.isActive(berlin(2026, 8, 22, 23, 59, 59)));
    }

    @Test
    void lastNanosecondBeforeEndExclusiveIsActive() {
        assertTrue(window.isActive(ZonedDateTime.of(2026, 8, 22, 23, 59, 59, 999_999_999, ZoneId.of("Europe/Berlin"))));
    }

    @Test
    void nextMidnightEndsEvent() {
        assertFalse(window.isActive(berlin(2026, 8, 23, 0, 0, 0)));
        assertTrue(window.hasEnded(berlin(2026, 8, 23, 0, 0, 0)));
    }

    @Test
    void systemDefaultTimezoneDoesNotChangeEventWindow() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            BirthdayEventWindow changedDefault = new BirthdayEventWindow(EventSettings.defaults());
            assertTrue(changedDefault.isActive(berlin(2026, 8, 22, 0, 0, 0)));
            assertFalse(changedDefault.isActive(berlin(2026, 8, 21, 23, 59, 59)));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void wrongServerDefaultZoneDoesNotAffectBerlinCalculation() {
        EventSettings settings = new EventSettings(
                ZoneId.of("Europe/Berlin"),
                LocalDate.of(2026, 8, 22),
                LocalTime.MIDNIGHT,
                24,
                15,
                "BirthdayPlayer"
        );
        BirthdayEventWindow berlinWindow = new BirthdayEventWindow(settings);
        ZonedDateTime sameInstantFromWrongZone = ZonedDateTime.of(2026, 8, 21, 22, 0, 0, 0, ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Europe/Berlin"));
        assertTrue(berlinWindow.isActive(sameInstantFromWrongZone));
    }

    private static ZonedDateTime berlin(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("Europe/Berlin"));
    }
}
