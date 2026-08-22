package de.otaku.birthdayevent.event;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventSchedulerModeTest {
    private final BirthdayEventWindow window = new BirthdayEventWindow(EventSettings.defaults());

    @Test
    void serverStartsBeforeEventSchedulesStartTask() {
        assertEquals(EventSchedulerMode.DELAYED_START,
                EventSchedulerMode.forState(window, false, berlin(2026, 8, 21, 23, 0, 0)));
    }

    @Test
    void serverRestartDuringEventKeepsEventActiveWithoutDelayedStartWhenTriggered() {
        assertTrue(window.isActive(berlin(2026, 8, 22, 13, 3, 0)));
        assertEquals(EventSchedulerMode.NONE,
                EventSchedulerMode.forState(window, true, berlin(2026, 8, 22, 13, 3, 0)));
    }

    @Test
    void lateStartDuringEventUsesSafetyCheckUntilGlobalStartIsCaughtUp() {
        assertTrue(window.isActive(berlin(2026, 8, 22, 0, 5, 0)));
        assertEquals(EventSchedulerMode.SAFETY_ONLY,
                EventSchedulerMode.forState(window, false, berlin(2026, 8, 22, 0, 5, 0)));
    }

    @Test
    void serverStartsAfterEventEndSchedulesNothing() {
        assertFalse(window.isActive(berlin(2026, 8, 23, 0, 1, 0)));
        assertEquals(EventSchedulerMode.NONE,
                EventSchedulerMode.forState(window, false, berlin(2026, 8, 23, 0, 1, 0)));
    }

    @Test
    void repeatedReloadsDuringTriggeredEventDoNotNeedAnotherScheduler() {
        ZonedDateTime reloadTime = berlin(2026, 8, 22, 18, 0, 0);
        for (int i = 0; i < 5; i++) {
            assertEquals(EventSchedulerMode.NONE, EventSchedulerMode.forState(window, true, reloadTime));
        }
    }

    private static ZonedDateTime berlin(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("Europe/Berlin"));
    }
}
