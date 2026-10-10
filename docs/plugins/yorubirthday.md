# YoruBirthday

Birthday event scheduling and reward selection.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.otaku.yorubirthday.YoruBirthday` |
| Source files | 28 |
| Tests | 7 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Gameplay
- Focus: Birthday event scheduling and reward selection

## Code Snippets

### 1. src/main/java/de/otaku/yorubirthday/event/EventSettings.java
```java
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
                "Yoru"
        );
    }
}
```

### 2. src/main/java/de/otaku/yorubirthday/config/PluginSettings.java
```java
public record PluginSettings(
        EventSettings event,
        boolean titleEnabled,
        int titleFadeInTicks,
        int titleStayTicks,
        int titleFadeOutTicks,
        boolean fireworkEnabled,
        String rewardCommand,
        List<RewardCrate> crates,
        boolean rewardLogging
) {
}
```

[← Back to Plugin Catalog](../plugin-catalog.md)
