# MythicEmpireControl

Paper adapter for Discord/web platform services.

| Field | Value |
| --- | --- |
| Version | `1.0.7` |
| Build | Maven |
| Main class | `com.mythicempire.control.minecraft.MythicEmpireControlPlugin` |
| Source files | 38 |
| Tests | 13 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Core / Infrastructure
- Focus: Paper adapter for Discord/web platform services
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/com/mythicempire/control/minecraft/tips/PlayerTipConfig.java

Configuration handling from the plugin source.

```java
public record PlayerTipConfig(boolean enabled, long intervalMinutes, boolean avoidRepeat, List<PlayerTip> tips,
                              PlayerTipAppearance appearance) {
    public PlayerTipConfig(boolean enabled, long intervalMinutes, boolean avoidRepeat, List<PlayerTip> tips) {
        this(enabled, intervalMinutes, avoidRepeat, tips, PlayerTipAppearance.defaults());
    }

    public PlayerTipConfig {
        intervalMinutes = intervalMinutes > 0 ? intervalMinutes : 15;
        tips = tips == null ? List.of() : List.copyOf(tips);
        appearance = appearance == null ? PlayerTipAppearance.defaults() : appearance;
    }

    public static PlayerTipConfig defaults() {
        return new PlayerTipConfig(true, 15, true, List.of(
                tip("vote", "/vote", "Unterstütze den Server & sichere dir Belohnungen!", "Stimme jetzt mit /vote für uns ab."),
                tip("stats", "/stats", "Wie weit bist du eigentlich schon gekommen?", "Deine Statistiken findest du mit /stats."),
                tip("shop", "/shop", "Dir fehlen noch die richtigen Items?", "Öffne unseren Server-Shop mit /shop."),
                tip("sell", "/sell", "Dein Inventar ist voll mit wertvollen Items?", "Verkaufe sie schnell und einfach mit /sell."),
                tip("store", "/store", "Du willst Mythic Empire unterstützen?", "Unsere Store-Angebote findest du mit /store."),
                tip("milestones", "/milestones", "Schon gesehen, was du alles erreicht hast?", "Deine Meilensteine findest du mit /milestones."),
                tip("auction-house", "/ah", "Kaufen, verkaufen, handeln – direkt mit anderen Spielern.", "Öffne das Auktionshaus mit /ah."),
                tip("daily", "/daily", "Deine tägliche Belohnung wartet auf dich!", "Hol sie dir mit /daily."),
                tip("discord", "/dc", "Du willst nichts verpassen und Teil der Community sein?", "Unseren Discord findest du mit /dc."),
                new PlayerTip("linking-reminder", true, "/link",
                        "Dein Minecraft-Account ist noch nicht mit Discord verbunden!",
                        "Nutze /link und schalte die Synchronisierung frei.")
        ), PlayerTipAppearance.defaults());
    }

    private static PlayerTip tip(String key, String command, String message, String action) {
        return new PlayerTip(key, false, command, message, action);
    }
}
```

### 2. src/main/java/com/mythicempire/control/minecraft/tips/PlayerTipService.java

Service-level logic selected from the plugin source.

```java
    public PlayerTipService(PlayerTipConfig config, AccountLinkRepository links, PlayerTipSelector selector,
                            PlayerTipMessageFactory messages, PlayerTipPlatform platform, PlayerTipWorker worker,
                            Consumer<String> warnings) {
        this.config = config;
        this.links = links;
        this.selector = selector;
        this.messages = messages;
        this.platform = platform;
        this.worker = worker;
        this.warnings = warnings == null ? ignored -> { } : warnings;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
