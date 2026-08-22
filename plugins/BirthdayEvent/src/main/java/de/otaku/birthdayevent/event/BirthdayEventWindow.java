package de.otaku.birthdayevent.event;

import java.time.Duration;
import java.time.ZonedDateTime;

public final class BirthdayEventWindow {
    private final EventSettings settings;
    private final ZonedDateTime start;
    private final ZonedDateTime end;

    public BirthdayEventWindow(EventSettings settings) {
        this.settings = settings;
        this.start = ZonedDateTime.of(settings.date(), settings.startTime(), settings.zone());
        this.end = start.plusHours(settings.durationHours());
    }

    public EventSettings settings() {
        return settings;
    }

    public ZonedDateTime start() {
        return start;
    }

    public ZonedDateTime end() {
        return end;
    }

    public ZonedDateTime now() {
        return ZonedDateTime.now(settings.zone());
    }

    public boolean hasStarted(ZonedDateTime now) {
        return !now.isBefore(start);
    }

    public boolean isActive(ZonedDateTime now) {
        return !now.isBefore(start) && now.isBefore(end);
    }

    public boolean hasEnded(ZonedDateTime now) {
        return !now.isBefore(end);
    }

    public Duration untilStart(ZonedDateTime now) {
        return Duration.between(now, start);
    }
}
