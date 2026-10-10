# OtakusEnchants

Custom enchantment behavior and item handling.

| Field | Value |
| --- | --- |
| Version | `1.1.1` |
| Build | Maven |
| Main class | `de.otaku.enchants.OtakusEnchantsPlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Gameplay
- Focus: Custom enchantment behavior and item handling

## Code Snippets

### 1. src/main/java/de/otaku/enchants/OtakusEnchantsPlugin.java
```java
    public void onEnable() {
        loreCountKey = new NamespacedKey(this, "generated_lore_lines");
        registerVanillaEnchantments();
        getServer().getPluginManager().registerEvents(this, this);

        var command = getCommand("enchant");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
