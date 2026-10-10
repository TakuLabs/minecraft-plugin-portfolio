# OperatorItems

Operator-only item command utilities.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `otaku.operatoritems.OperatorItemsPlugin` |
| Source files | 2 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Operator-only item command utilities
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/otaku/operatoritems/OperatorItem.java

Representative source excerpt.

```java
package otaku.operatoritems;

import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

record OperatorItem(String name, Supplier<ItemStack> factory, boolean available) {
    ItemStack item() {
        return factory.get();
    }

    ItemStack displayItem() {
        return item();
    }
}

```

[← Back to Plugin Catalog](../plugin-catalog.md)
