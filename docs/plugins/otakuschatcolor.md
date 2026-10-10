# OtakusChatColor

Chat color formatting and player preference handling.

| Field | Value |
| --- | --- |
| Version | `1.1.10` |
| Build | Maven |
| Main class | `otaku.chatcolor.OtakusChatcolorPlugin` |
| Source files | 13 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Chat color formatting and player preference handling

## Code Snippets

### 1. src/main/java/otaku/chatcolor/ColorCommand.java
```java
    public ColorCommand(OtakusChatcolorPlugin plugin, ColorManager colorManager, ColorTarget target) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.target = target;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
