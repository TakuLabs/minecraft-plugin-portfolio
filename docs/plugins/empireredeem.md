# EmpireRedeem

Redeem codes, rewards and validation.

| Field | Value |
| --- | --- |
| Version | `1.4.3` |
| Build | Maven |
| Main class | `de.otaku.empireredeem.EmpireRedeemPlugin` |
| Source files | 164 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Economy
- Focus: Redeem codes, rewards and validation

## Code Snippets

### 1. src/main/java/de/otaku/empireredeem/model/Voucher.java
```java
package de.otaku.empireredeem.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Voucher {
    private final String name;
    private String displayName;
    private final List<ItemStack> itemRewards = new ArrayList<>();
    private final List<String> commandRewards = new ArrayList<>();
    private final List<RandomReward> randomRewards = new ArrayList<>();
    private boolean randomVoucher;
    private ItemStack voucherItem;
    private String rewardListTitle = "Mögliche Gewinne:";

    public Voucher(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return displayName == null || displayName.isBlank() ? name : displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? name : displayName;
    }

    public List<ItemStack> itemRewards() {
        return itemRewards;
```

### 2. src/main/java/de/otaku/empireredeem/model/RedeemCode.java
```java
    public RedeemCode(String code, String voucherName) {
        this.code = code;
        this.voucherName = voucherName;
        this.perPlayerLimit = 1;
    }
```

### 3. src/main/java/de/otaku/empireredeem/model/RandomReward.java
```java
package de.otaku.empireredeem.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RandomReward {
    private String name;
    private String displayName;
    private int weight = 1;
    private final List<ItemStack> itemRewards = new ArrayList<>();
    private final List<String> commandRewards = new ArrayList<>();

    public RandomReward(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public String displayName() {
        return displayName == null || displayName.isBlank() ? name : displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? name : displayName;
    }

    public int weight() {
```

[← Back to Plugin Catalog](../plugin-catalog.md)
