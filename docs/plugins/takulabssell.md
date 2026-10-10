# TakuLabsSell

Sell-worth economy module with plugin lifecycle bootstrap.

| Field | Value |
| --- | --- |
| Version | `1.1.2` |
| Build | Maven |
| Main class | `de.mythicempire.empirecore.OtakuSellWorthPlugin` |
| Source files | 7 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Economy
- Focus: Sell-worth economy module with plugin lifecycle bootstrap

## Code Snippets

### 1. src/main/java/de/mythicempire/empirecore/OtakuSellWorthPlugin.java
```java
public final class OtakuSellWorthPlugin extends JavaPlugin {
    private EmpireCore empireCore;

    @Override
    public void onEnable() {
        empireCore = new EmpireCore(this);
        empireCore.onEnable();
    }

    @Override
    public void onDisable() {
        if (empireCore != null) {
            empireCore.onDisable();
        }
    }
}
```

[← Back to Plugin Catalog](../plugin-catalog.md)
