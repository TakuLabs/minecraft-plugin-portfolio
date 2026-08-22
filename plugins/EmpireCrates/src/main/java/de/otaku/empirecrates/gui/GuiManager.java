package de.otaku.empirecrates.gui;

import de.otaku.empirecrates.EmpireCratesPlugin;
import de.otaku.empirecrates.model.CrateAnimation;
import de.otaku.empirecrates.model.CrateAura;
import de.otaku.empirecrates.model.CrateDefinition;
import de.otaku.empirecrates.model.CrateReward;
import de.otaku.empirecrates.model.RewardRarity;
import de.otaku.empirecrates.model.RewardType;
import de.otaku.empirecrates.util.ItemBuilder;
import de.otaku.empirecrates.util.RewardChanceCalculator;
import de.otaku.empirecrates.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GuiManager implements Listener {
    private static final int ITEM_INPUT_SLOT = 13;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 52;
    private static final String INPUT_CRATE_ITEM = "crate_item";
    private static final String INPUT_KEY_ITEM = "key_item";
    private static final String INPUT_REWARD_ITEM = "reward_item";

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final EmpireCratesPlugin plugin;
    private final Map<UUID, ChatPrompt> prompts = new HashMap<>();

    public GuiManager(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        openMain(player, 0);
    }

    public void openMain(Player player, int page) {
        List<CrateDefinition> crates = new ArrayList<>(plugin.crates().all());
        crates.sort(Comparator.comparing(CrateDefinition::getId));
        int maxPage = maxPage(crates.size());
        page = clampPage(page, maxPage);
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.MAIN, null, null, null, page), 54, Text.mm("<bold><gradient:#F5B642:#9B4DFF>EmpireCrates Admin</gradient></bold>"));
        fillFrame(inventory);
        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, crates.size());
        for (int itemIndex = start; itemIndex < end; itemIndex++) {
            CrateDefinition crate = crates.get(itemIndex);
            inventory.setItem(CONTENT_SLOTS[itemIndex - start], new ItemBuilder(crate.getCrateItem())
                    .name(crate.getDisplayName())
                    .lore("&8ID: &7" + crate.getId(), "&7Links: Preview", "&7Rechts: Bearbeiten", "&cShift-Rechts: L\u00f6schen")
                    .glowless()
                    .build());
        }
        addPageControls(inventory, page, maxPage);
        inventory.setItem(47, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name("<gradient:#F5B642:#9B4DFF>Keyall Timer</gradient>")
                .lore("<gray>Crate-Key für automatisches Keyall wählen.</gray>",
                        "<gray>Countdown:</gray> <gradient:#7CFFB2:#F5B642>" + plugin.keyall().remainingFormatted() + "</gradient>",
                        "<dark_gray>Placeholder: %empirecrates_keyall_time%</dark_gray>")
                .build());
        inventory.setItem(PAGE_INFO_SLOT, new ItemBuilder(Material.EMERALD).name("&aCrate erstellen").lore("&7Erstellt eine neue Crate per Chat-Eingabe.", "&8Seite " + (page + 1) + "/" + (maxPage + 1)).build());
        player.openInventory(inventory);
    }

    public void openEdit(Player player, CrateDefinition crate) {
        openEditCategories(player, crate);
        if (player.isOnline()) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.EDIT_CRATE, crate.getId(), null), 54, Text.mm("<gradient:#F5B642:#9B4DFF>Crate bearbeiten</gradient> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        inventory.setItem(4, new ItemBuilder(crate.getCrateItem())
                .name(crate.getDisplayName())
                .lore("&8ID: &7" + crate.getId(), "&7Aktuelles Crate-Item und Hologramm-Name.")
                .glowless()
                .build());

        paintSection(inventory, 9, Material.ORANGE_STAINED_GLASS_PANE, "<gradient:#F5B642:#9B4DFF>Crate</gradient>", "Item, Name und platzierbares Crate-Item.");
        inventory.setItem(11, new ItemBuilder(crate.getCrateItem()).name("<gradient:#F5B642:#9B4DFF>Crate Item setzen</gradient>").lore("&7Item ins Eingabe-Menü legen.", "&7ItemsAdder-Items bleiben erhalten.").build());
        inventory.setItem(13, new ItemBuilder(Material.NAME_TAG).name("<gradient:#F5B642:#9B4DFF>Name ändern</gradient>").lore("&7Per Chat mit MiniMessage oder &-Farben.", "&7Der Hologramm-Name wird direkt aktualisiert.").build());
        inventory.setItem(15, new ItemBuilder(Material.SHULKER_BOX).name("<gradient:#7CFFB2:#F5B642>Crate Item erhalten</gradient>").lore("&7Gibt dir das platzierbare Crate Item.").build());
        inventory.setItem(17, new ItemBuilder(Material.ARMOR_STAND).name("<gradient:#F5B642:#9B4DFF>ModelEngine Model</gradient>").lore(
                "<gray>Model:</gray> <gradient:#7CFFB2:#F5B642>" + (crate.getModelEngineModelId().isBlank() ? "Keins" : crate.getModelEngineModelId()) + "</gradient>",
                "<gray>Open:</gray> <gradient:#F5B642:#9B4DFF>" + emptyModelValue(crate.getModelEngineOpenAnimation()) + "</gradient>",
                "<gray>Idle:</gray> <gradient:#7CFFB2:#F5B642>" + emptyModelValue(crate.getModelEngineIdleAnimation()) + "</gradient>",
                "<gray>Close:</gray> <gradient:#F5B642:#9B4DFF>" + emptyModelValue(crate.getModelEngineCloseAnimation()) + "</gradient>",
                "<gray>Klicken, um die Model-ID per Chat zu setzen.</gray>",
                "<dark_gray>none entfernt das Model.</dark_gray>").build());

        paintSection(inventory, 18, Material.PURPLE_STAINED_GLASS_PANE, "<gradient:#9B4DFF:#7CFFB2>Keys</gradient>", "Key-Pflicht, Key-Item und Test-Key.");
        inventory.setItem(20, new ItemBuilder(crate.isKeyRequired() ? Material.LIME_DYE : Material.GRAY_DYE).name("<gradient:#F5B642:#9B4DFF>Key benötigt: " + (crate.isKeyRequired() ? "Ja" : "Nein") + "</gradient>").lore("&7Klicken zum Umschalten.").build());
        inventory.setItem(22, new ItemBuilder(crate.getKeyItem()).name("<gradient:#F5B642:#9B4DFF>Crate Key setzen</gradient>").lore("&7Item ins Eingabe-Menü legen.", "&8Key-ID: &7" + crate.getKeyId()).build());
        inventory.setItem(24, new ItemBuilder(Material.TRIPWIRE_HOOK).name("<gradient:#7CFFB2:#F5B642>Key erhalten</gradient>").lore("&7Gibt dir den markierten Key.").build());

        paintSection(inventory, 27, Material.LIME_STAINED_GLASS_PANE, "<gradient:#7CFFB2:#F5B642>Rewards</gradient>", "Gewinne, Preview und Animation.");
        inventory.setItem(29, new ItemBuilder(Material.CHEST).name("<gradient:#F5B642:#9B4DFF>Rewards bearbeiten</gradient>").lore("&7Item- und Command-Rewards einstellen.", "&7Seltenheiten und Broadcasts verwalten.").build());
        inventory.setItem(31, animationIcon(crate.getAnimation()));
        inventory.setItem(33, new ItemBuilder(Material.ENDER_EYE).name("<gradient:#7CFFB2:#F5B642>Preview öffnen</gradient>").lore("&7Zeigt die Spieler-Preview dieser Crate.", "&7Mit Zurück kommst du wieder hierher.").build());

        paintSection(inventory, 36, Material.CYAN_STAINED_GLASS_PANE, "<gradient:#45CAFF:#F5B642>Milestone</gradient>", "Fortschritt und Milestone-Key.");
        inventory.setItem(38, new ItemBuilder(crate.isMilestoneEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<gradient:#F5B642:#9B4DFF>Milestone: " + (crate.isMilestoneEnabled() ? "An" : "Aus") + "</gradient>")
                .lore("<gray>Klicken, um Milestones für diese Crate umzuschalten.</gray>")
                .build());
        inventory.setItem(40, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("<gradient:#F5B642:#9B4DFF>Milestone Ziel: " + crate.getMilestoneTarget() + "</gradient>").lore("<gray>Klicken, um das Ziel per Chat zu setzen.</gray>", "<dark_gray>Beispiel: 50 für 0/50.</dark_gray>").build());
        inventory.setItem(42, new ItemBuilder(Material.TRIPWIRE_HOOK).name("<gradient:#F5B642:#9B4DFF>Milestone Key</gradient>").lore("<gray>Aktuell: " + crate.getMilestoneRewardCrateId() + "</gray>", "<gray>Klicken, um durch Crates zu wechseln.</gray>").build());

        inventory.setItem(49, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    private void openEditCategories(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.EDIT_CRATE, crate.getId(), null), 27, Text.mm("<gradient:#F5B642:#9B4DFF>Crate Admin</gradient> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        inventory.setItem(10, categorySign("<gradient:#F5B642:#9B4DFF>Crate</gradient>", "Item", "Name", "Crate-Item", "Lootbox", "ModelEngine-ID"));
        inventory.setItem(12, categorySign("<gradient:#9B4DFF:#7CFFB2>Keys</gradient>", "Keys Pflicht", "Key-Item", "Test-Key"));
        inventory.setItem(14, categorySign("<gradient:#7CFFB2:#F5B642>Rewards</gradient>", "Gewinne", "Preview", "Animation"));
        inventory.setItem(16, categorySign("<gradient:#45CAFF:#F5B642>Milestone</gradient>", "Fortschritt", "Milestone-Key Reward"));
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openCrateSettings(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.CRATE_SETTINGS, crate.getId(), null), 36, Text.mm("<gradient:#F5B642:#9B4DFF>Crate Einstellungen</gradient> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        inventory.setItem(4, new ItemBuilder(crate.getCrateItem()).name(crate.getDisplayName()).lore("&8ID: &7" + crate.getId(), "&7Aktuelles Crate-Item und Hologramm-Name.").glowless().build());
        inventory.setItem(10, new ItemBuilder(crate.getCrateItem()).name("<gradient:#F5B642:#9B4DFF>Crate Item setzen</gradient>").lore("&7Item ins Eingabe-Menü legen.", "&7ItemsAdder-Items bleiben erhalten.").build());
        inventory.setItem(12, new ItemBuilder(Material.NAME_TAG).name("<gradient:#F5B642:#9B4DFF>Name ändern</gradient>").lore("&7Per Chat mit MiniMessage oder &-Farben.", "&7Der Hologramm-Name wird direkt aktualisiert.").build());
        inventory.setItem(14, new ItemBuilder(Material.SHULKER_BOX).name("<gradient:#7CFFB2:#F5B642>Crate Item erhalten</gradient>").lore("&7Gibt dir das markierte Crate Item.").build());
        inventory.setItem(16, new ItemBuilder(crate.isLootboxEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<gradient:#F5B642:#9B4DFF>Lootbox: " + (crate.isLootboxEnabled() ? "An" : "Aus") + "</gradient>")
                .lore("&7Wenn aktiv, kann das Crate-Item", "&7per Rechtsklick aus der Hand geöffnet werden.", "&7Das Item wird beim Öffnen verbraucht.")
                .build());
        inventory.setItem(20, auraIcon(crate.getAura()));
        inventory.setItem(28, auraParticleIcon(crate));
        inventory.setItem(22, new ItemBuilder(Material.ARMOR_STAND).name("<gradient:#F5B642:#9B4DFF>ModelEngine Model</gradient>").lore(
                "<gray>Model:</gray> <gradient:#7CFFB2:#F5B642>" + (crate.getModelEngineModelId().isBlank() ? "Keins" : crate.getModelEngineModelId()) + "</gradient>",
                "<gray>Open:</gray> <gradient:#F5B642:#9B4DFF>" + emptyModelValue(crate.getModelEngineOpenAnimation()) + "</gradient>",
                "<gray>Idle:</gray> <gradient:#7CFFB2:#F5B642>" + emptyModelValue(crate.getModelEngineIdleAnimation()) + "</gradient>",
                "<gray>Close:</gray> <gradient:#F5B642:#9B4DFF>" + emptyModelValue(crate.getModelEngineCloseAnimation()) + "</gradient>",
                "<gray>Klicken, um die Model-ID per Chat zu setzen.</gray>",
                "<dark_gray>none entfernt das Model.</dark_gray>").build());
        inventory.setItem(24, new ItemBuilder(Material.OAK_SIGN).name("<gradient:#F5B642:#9B4DFF>Hologramm Höhe</gradient>").lore("<gray>Aktuell:</gray> <gradient:#7CFFB2:#F5B642>" + cleanNumber(crate.getHologramHeight()) + "</gradient>", "<gray>Klicken, um die Höhe per Chat zu setzen.</gray>", "<dark_gray>Standard: 2.25 | Bereich: 0.25 bis 8.0</dark_gray>").build());
        inventory.setItem(31, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openKeySettings(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.KEY_SETTINGS, crate.getId(), null), 36, Text.mm("<gradient:#9B4DFF:#7CFFB2>Key Einstellungen</gradient> <dark_gray>-</dark_gray> <gradient:#F5B642:#9B4DFF>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        inventory.setItem(11, new ItemBuilder(crate.isKeyRequired() ? Material.LIME_DYE : Material.GRAY_DYE).name("<gradient:#F5B642:#9B4DFF>Keys Pflicht: " + (crate.isKeyRequired() ? "Ja" : "Nein") + "</gradient>").lore("&7Klicken zum Umschalten.").build());
        inventory.setItem(13, new ItemBuilder(crate.getKeyItem()).name("<gradient:#F5B642:#9B4DFF>Key Item setzen</gradient>").lore("&7Item ins Eingabe-Menü legen.", "&8Key-ID: &7" + crate.getKeyId()).build());
        inventory.setItem(15, new ItemBuilder(Material.TRIPWIRE_HOOK).name("<gradient:#7CFFB2:#F5B642>Test-Key erhalten</gradient>").lore("&7Gibt dir den markierten Key.").build());
        inventory.setItem(31, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openRewardSettings(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.REWARD_SETTINGS, crate.getId(), null), 36, Text.mm("<gradient:#7CFFB2:#F5B642>Reward Einstellungen</gradient> <dark_gray>-</dark_gray> <gradient:#F5B642:#9B4DFF>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        inventory.setItem(11, new ItemBuilder(Material.CHEST).name("<gradient:#F5B642:#9B4DFF>Gewinne bearbeiten</gradient>").lore("&7Item- und Command-Rewards einstellen.", "&7Seltenheiten und Broadcasts verwalten.").build());
        inventory.setItem(13, new ItemBuilder(Material.ENDER_EYE).name("<gradient:#7CFFB2:#F5B642>Preview öffnen</gradient>").lore("&7Zeigt die Spieler-Preview dieser Crate.", "&7Mit Zurück kommst du wieder hierher.").build());
        inventory.setItem(15, animationIcon(crate.getAnimation()));
        inventory.setItem(31, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openMilestoneSettings(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.MILESTONE_SETTINGS, crate.getId(), null), 36, Text.mm("<gradient:#45CAFF:#F5B642>Milestone Einstellungen</gradient> <dark_gray>-</dark_gray> <gradient:#F5B642:#9B4DFF>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        inventory.setItem(11, new ItemBuilder(crate.isMilestoneEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<gradient:#F5B642:#9B4DFF>Milestone: " + (crate.isMilestoneEnabled() ? "An" : "Aus") + "</gradient>")
                .lore("<gray>Klicken, um Milestones für diese Crate umzuschalten.</gray>")
                .build());
        inventory.setItem(13, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("<gradient:#F5B642:#9B4DFF>Fortschritt Ziel: " + crate.getMilestoneTarget() + "</gradient>").lore("<gray>Klicken, um das Ziel per Chat zu setzen.</gray>", "<dark_gray>Beispiel: 50 für 0/50.</dark_gray>").build());
        inventory.setItem(15, new ItemBuilder(Material.TRIPWIRE_HOOK).name("<gradient:#F5B642:#9B4DFF>Milestone-Key Reward</gradient>").lore("<gray>Aktuell: " + crate.getMilestoneRewardCrateId() + "</gray>", "<gray>Klicken, um durch Crates zu wechseln.</gray>").build());
        inventory.setItem(31, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }
    public void openMilestoneKeySelect(Player player, CrateDefinition crate, int page) {
        List<CrateDefinition> crates = sortedCrates();
        int maxPage = maxPage(crates.size());
        page = clampPage(page, maxPage);
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.MILESTONE_KEY_SELECT, crate.getId(), null, null, page), 54, Text.mm("<gradient:#F5B642:#9B4DFF>Milestone Key</gradient> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, crates.size());
        for (int itemIndex = start; itemIndex < end; itemIndex++) {
            CrateDefinition keyCrate = crates.get(itemIndex);
            inventory.setItem(CONTENT_SLOTS[itemIndex - start], keyCrateIcon(keyCrate, keyCrate.getId().equalsIgnoreCase(crate.getMilestoneRewardCrateId())));
        }
        addPageControls(inventory, page, maxPage);
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openKeyallSettings(Player player) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.KEYALL_SETTINGS, null, null), 27, Text.mm("<gradient:#F5B642:#9B4DFF>Keyall Timer</gradient>"));
        fillFrame(inventory);
        String crateId = plugin.keyall().configuredCrateId();
        inventory.setItem(10, new ItemBuilder(plugin.keyall().isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<gradient:#F5B642:#9B4DFF>Timer: " + (plugin.keyall().isEnabled() ? "An" : "Aus") + "</gradient>")
                .lore("<gray>Klicken, um den automatischen Keyall zu toggeln.</gray>")
                .build());
        inventory.setItem(12, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name("<gradient:#7CFFB2:#F5B642>Key-Crate auswählen</gradient>")
                .lore("<gray>Aktuell:</gray> <gradient:#F5B642:#9B4DFF>" + (crateId.isBlank() ? "Keine" : crateId) + "</gradient>",
                        "<gray>Klicken, um die Crate per GUI zu wählen.</gray>")
                .build());
        inventory.setItem(14, new ItemBuilder(Material.CLOCK)
                .name("<gradient:#F5B642:#9B4DFF>Countdown</gradient>")
                .lore("<gray>Restzeit:</gray> <gradient:#7CFFB2:#F5B642>" + plugin.keyall().remainingFormatted() + "</gradient>",
                        "<gray>Intervall:</gray> <gradient:#F5B642:#9B4DFF>" + plugin.keyall().intervalMinutes() + " Minuten</gradient>",
                        "<dark_gray>Config: keyall-timer.interval-minutes</dark_gray>")
                .build());
        inventory.setItem(16, new ItemBuilder(Material.CHEST)
                .name("<gradient:#7CFFB2:#F5B642>Anzahl: " + plugin.keyall().configuredAmount() + "</gradient>")
                .lore("<gray>Klicken: +1</gray>", "<gray>Rechtsklick: -1</gray>")
                .build());
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openKeyallCrateSelect(Player player, int page) {
        List<CrateDefinition> crates = sortedCrates();
        int maxPage = maxPage(crates.size());
        page = clampPage(page, maxPage);
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.KEYALL_CRATE_SELECT, null, null, null, page), 54, Text.mm("<gradient:#F5B642:#9B4DFF>Keyall Crate</gradient>"));
        fillFrame(inventory);
        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, crates.size());
        String selected = plugin.keyall().configuredCrateId();
        for (int itemIndex = start; itemIndex < end; itemIndex++) {
            CrateDefinition keyCrate = crates.get(itemIndex);
            inventory.setItem(CONTENT_SLOTS[itemIndex - start], keyCrateIcon(keyCrate, keyCrate.getId().equalsIgnoreCase(selected)));
        }
        addPageControls(inventory, page, maxPage);
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openRewards(Player player, CrateDefinition crate) {
        openRewards(player, crate, 0);
    }

    public void openRewards(Player player, CrateDefinition crate, int page) {
        int maxPage = maxPage(crate.getRewards().size());
        page = clampPage(page, maxPage);
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.REWARDS, crate.getId(), null, null, page), 54, Text.mm("<gradient:#F5B642:#9B4DFF>Rewards</gradient> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, crate.getRewards().size());
        for (int itemIndex = start; itemIndex < end; itemIndex++) {
            inventory.setItem(CONTENT_SLOTS[itemIndex - start], rewardIcon(crate.getRewards().get(itemIndex), true));
        }
        addPageControls(inventory, page, maxPage);
        inventory.setItem(48, new ItemBuilder(Material.DIAMOND).name("&aItem Reward erstellen").lore("&7Danach Item per Cursor setzen.").build());
        inventory.setItem(50, new ItemBuilder(Material.COMMAND_BLOCK).name("&aCommand Reward erstellen").lore("&7Command danach per Chat setzen.").build());
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&7Zur\u00fcck").build());
        player.openInventory(inventory);
    }

    public void openRewardEdit(Player player, CrateDefinition crate, CrateReward reward) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.EDIT_REWARD, crate.getId(), reward.getId()), 36, Text.mm("<gradient:#F5B642:#9B4DFF>Reward bearbeiten</gradient>"));
        fillFrame(inventory);
        inventory.setItem(10, new ItemBuilder(reward.getType() == RewardType.ITEM ? Material.DIAMOND : Material.COMMAND_BLOCK).name("&dTyp: &f" + reward.getType()).lore("&7Klicken zum Wechseln.").build());
        inventory.setItem(12, reward.getItem() != null
                ? new ItemBuilder(reward.getItem()).name(reward.getType() == RewardType.ITEM ? "&6Item Reward setzen" : "&6Platzhalter Item setzen").lore("&7Klicken, dann Item ins Eingabe-Men\u00fc legen.", reward.getType() == RewardType.COMMAND ? "&8Wird im Chat als Hover angezeigt." : "&8Stack-Anzahl wird als Gewinn gespeichert.").build()
                : new ItemBuilder(reward.getType() == RewardType.ITEM ? Material.DIAMOND : Material.NAME_TAG).name(reward.getType() == RewardType.ITEM ? "&6Item Reward setzen" : "&6Platzhalter Item setzen").lore("&7Klicken, dann Item ins Eingabe-Men\u00fc legen.", reward.getType() == RewardType.COMMAND ? "&8Versteckt den echten Command im Chat." : "&8Stack-Anzahl wird als Gewinn gespeichert.").build());
        inventory.setItem(14, new ItemBuilder(Material.PAPER).name("&eCommand").lore("&7Aktuell: &f/" + nullToEmpty(reward.getCommand()), "&7Klicken zum Bearbeiten.", "&8Spieler: %spieler% oder %player%").build());
        inventory.setItem(16, new ItemBuilder(reward.getRarity().icon()).name(reward.getRarity().miniMessage()).lore(
                "<gray>Chance:</gray> <gradient:#F5B642:#9B4DFF>" + reward.getChancePercent() + "%</gradient>",
                "<gray>Klicken zum Wechseln.</gray>",
                "<dark_gray>Gew\u00f6hnlich -> Ungew\u00f6hnlich -> Selten -> Episch -> Legend\u00e4r -> Mystisch -> JACKPOT</dark_gray>"
        ).build());
        inventory.setItem(20, new ItemBuilder(reward.isBroadcast() ? Material.LIME_DYE : Material.GRAY_DYE).name("<gradient:#F5B642:#9B4DFF>Broadcast: " + (reward.isBroadcast() ? "An" : "Aus") + "</gradient>").lore("<gray>Wenn aktiv, wird dieser Gewinn im Chat angek\u00fcndigt.</gray>").build());
        inventory.setItem(22, new ItemBuilder(Material.REDSTONE_BLOCK).name("<gradient:#ff5f6d:#ffc371>Reward l\u00f6schen</gradient>").build());
        inventory.setItem(24, new ItemBuilder(Material.OAK_SIGN).name("<gradient:#F5B642:#9B4DFF>Anzeigename</gradient>").lore("<gray>Aktuell:</gray> " + reward.getDisplayName(), "<gray>Klicken, um den Chat-Namen zu setzen.</gray>", "<dark_gray>MiniMessage und Hex-Gradient gehen.</dark_gray>").build());
        inventory.setItem(28, new ItemBuilder(reward.getItem() != null ? reward.getItem() : new ItemStack(Material.CHEST))
                .name("<gradient:#7CFFB2:#F5B642>Item-Kopie erhalten</gradient>")
                .lore("<gray>Gibt dir eine Kopie des gespeicherten Items.</gray>",
                        reward.getType() == RewardType.COMMAND ? "<dark_gray>Bei Command-Rewards ist das das Platzhalter-Item.</dark_gray>" : "<dark_gray>Bei Item-Rewards ist das der echte Drop.</dark_gray>")
                .build());
        inventory.setItem(31, new ItemBuilder(Material.ARROW).name("&7Zur\u00fcck").build());
        player.openInventory(inventory);
    }

    public void openRaritySelect(Player player, CrateDefinition crate, CrateReward reward) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.RARITY_SELECT, crate.getId(), reward.getId()), 27, Text.mm("<gradient:#F5B642:#9B4DFF>Seltenheit auswählen</gradient>"));
        fillFrame(inventory);
        int slot = 10;
        for (RewardRarity rarity : RewardRarity.values()) {
            inventory.setItem(slot++, new ItemBuilder(rarity.icon())
                    .name(rarity.miniMessage())
                    .lore("<gray>Chance:</gray> <gradient:#7CFFB2:#F5B642>" + rarity.chancePercent() + "%</gradient>",
                            reward.getRarity() == rarity ? "<green>Aktuell ausgewählt.</green>" : "<gray>Klicken, um diese Seltenheit zu setzen.</gray>")
                    .build());
        }
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openAnimationType(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.ANIMATION_TYPE, crate.getId(), null), 27, Text.mm("<gradient:#F5B642:#9B4DFF>Animation Typ</gradient>"));
        fillFrame(inventory);
        inventory.setItem(11, new ItemBuilder(Material.END_CRYSTAL)
                .name("<gradient:#F5B642:#9B4DFF>Offene Animation</gradient>")
                .lore("<gray>Items werden sichtbar über der Crate animiert.</gray>", "<gray>Gut für klassische Crate-Openings.</gray>")
                .build());
        inventory.setItem(15, new ItemBuilder(Material.CHEST)
                .name("<gradient:#7CFFB2:#F5B642>GUI Animation</gradient>")
                .lore("<gray>Beim Öffnen erscheint ein animiertes GUI.</gray>", "<gray>Der Gewinn wird im Menü enthüllt.</gray>")
                .build());
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openAnimationSelect(Player player, CrateDefinition crate, boolean guiAnimations) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.ANIMATION_SELECT, crate.getId(), null, guiAnimations ? "gui" : "open"), 45, Text.mm(guiAnimations ? "<gradient:#7CFFB2:#F5B642>GUI Animationen</gradient>" : "<gradient:#F5B642:#9B4DFF>Offene Animationen</gradient>"));
        fillFrame(inventory);
        int index = 0;
        for (CrateAnimation animation : CrateAnimation.values()) {
            if (animation.isGuiAnimation() != guiAnimations || index >= CONTENT_SLOTS.length) {
                continue;
            }
            inventory.setItem(CONTENT_SLOTS[index++], new ItemBuilder(animation.icon())
                    .name("<gradient:#F5B642:#9B4DFF>" + animation.displayName() + "</gradient>")
                    .lore("<gray>Typ:</gray> <gradient:#7CFFB2:#F5B642>" + animation.category() + "</gradient>",
                            "<gray>" + animation.description() + "</gray>",
                            crate.getAnimation() == animation ? "<green>Aktuell ausgewählt.</green>" : "<gray>Klicken, um diese Animation zu setzen.</gray>")
                    .build());
        }
        inventory.setItem(40, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openAuraParticleSelect(Player player, CrateDefinition crate, int page) {
        Particle[] particles = Particle.values();
        int maxPage = maxPage(particles.length);
        page = clampPage(page, maxPage);
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.AURA_PARTICLE_SELECT, crate.getId(), null, null, page), 54, Text.mm("<gradient:#F5B642:#9B4DFF>Aura Partikel</gradient> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, particles.length);
        for (int itemIndex = start; itemIndex < end; itemIndex++) {
            Particle particle = particles[itemIndex];
            inventory.setItem(CONTENT_SLOTS[itemIndex - start], particleIcon(crate, particle));
        }
        addPageControls(inventory, page, maxPage);
        inventory.setItem(47, new ItemBuilder(Material.BARRIER)
                .name("<gradient:#ff5f6d:#ffc371>Standard Partikel</gradient>")
                .lore("<gray>Nutzt den Standard-Partikel der Aura-Form.</gray>")
                .build());
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&7Zurück").build());
        player.openInventory(inventory);
    }

    public void openItemInput(Player player, CrateDefinition crate, String action, String rewardId, ItemStack currentItem) {
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.ITEM_INPUT, crate.getId(), rewardId, action), 27, Text.mm("<gradient:#F5B642:#9B4DFF>Item einsetzen</gradient> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        if (usable(currentItem)) {
            inventory.setItem(4, new ItemBuilder(currentItem).name("<gradient:#F5B642:#9B4DFF>Aktuelles Item</gradient>").lore("<gray>Das ist nur eine Vorschau.</gray>", "<gray>Lege das neue Item in den freien Slot.</gray>").build());
        }
        inventory.setItem(11, new ItemBuilder(Material.HOPPER).name("<gradient:#F5B642:#9B4DFF>Item hier einsetzen</gradient>").lore("<gray>Lege dein Item in den freien Slot in der Mitte.</gray>").build());
        inventory.setItem(15, new ItemBuilder(Material.LIME_DYE).name("<green>Speichern</green>").lore("<gray>Schlie\u00dfe das Men\u00fc oder klicke hier.</gray>").build());
        player.openInventory(inventory);
    }

    public void openPreview(Player player, CrateDefinition crate) {
        openPreview(player, crate, null, 0);
    }

    public void openPreview(Player player, CrateDefinition crate, String backAction) {
        openPreview(player, crate, backAction, 0);
    }

    public void openPreview(Player player, CrateDefinition crate, String backAction, int page) {
        int maxPage = maxPage(crate.getRewards().size());
        page = clampPage(page, maxPage);
        Inventory inventory = Bukkit.createInventory(new CrateGuiHolder(GuiType.PREVIEW, crate.getId(), null, backAction, page), 54, Text.mm("<bold><gradient:#F5B642:#9B4DFF>Crate Preview</gradient></bold> <dark_gray>-</dark_gray> <gradient:#7CFFB2:#F5B642>" + crate.getId() + "</gradient>"));
        fillFrame(inventory);
        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, crate.getRewards().size());
        for (int itemIndex = start; itemIndex < end; itemIndex++) {
            inventory.setItem(CONTENT_SLOTS[itemIndex - start], rewardIcon(crate.getRewards().get(itemIndex), player.isOp()));
        }
        addPageControls(inventory, page, maxPage);
        if (player.isOp()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&7Zurück").lore("&7Zurück zum Crate-Editor.").build());
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof CrateGuiHolder holder)) {
            return;
        }
        if (holder.type() == GuiType.ITEM_INPUT) {
            handleItemInputClick(event);
            return;
        }
        if (holder.type() == GuiType.PREVIEW) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            plugin.crates().find(holder.crateId()).ifPresent(crate -> {
                if (slot == PREVIOUS_PAGE_SLOT && holder.page() > 0) {
                    openPreview(player, crate, holder.action(), holder.page() - 1);
                    return;
                }
                if (slot == NEXT_PAGE_SLOT && holder.page() < maxPage(crate.getRewards().size())) {
                    openPreview(player, crate, holder.action(), holder.page() + 1);
                    return;
                }
                if (slot == 53 && player.isOp()) {
                    if ("reward_settings".equalsIgnoreCase(holder.action())) {
                        openRewardSettings(player, crate);
                    } else {
                        openEdit(player, crate);
                    }
                }
            });
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        switch (holder.type()) {
            case MAIN -> clickMain(player, event, holder, slot);
            case EDIT_CRATE -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickEdit(player, event, crate, slot));
            case CRATE_SETTINGS -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickCrateSettings(player, crate, slot));
            case KEY_SETTINGS -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickKeySettings(player, crate, slot));
            case REWARD_SETTINGS -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickRewardSettings(player, crate, slot));
            case MILESTONE_SETTINGS -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickMilestoneSettings(player, crate, slot));
            case MILESTONE_KEY_SELECT -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickMilestoneKeySelect(player, holder, crate, slot));
            case KEYALL_SETTINGS -> clickKeyallSettings(player, event, slot);
            case KEYALL_CRATE_SELECT -> clickKeyallCrateSelect(player, holder, slot);
            case REWARDS -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickRewards(player, event, holder, crate, slot));
            case EDIT_REWARD -> plugin.crates().find(holder.crateId()).ifPresent(crate -> findReward(crate, holder.rewardId()).ifPresent(reward -> clickRewardEdit(player, event, crate, reward, slot)));
            case RARITY_SELECT -> plugin.crates().find(holder.crateId()).ifPresent(crate -> findReward(crate, holder.rewardId()).ifPresent(reward -> clickRaritySelect(player, crate, reward, slot)));
            case ANIMATION_TYPE -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickAnimationType(player, crate, slot));
            case ANIMATION_SELECT -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickAnimationSelect(player, crate, holder, slot));
            case AURA_PARTICLE_SELECT -> plugin.crates().find(holder.crateId()).ifPresent(crate -> clickAuraParticleSelect(player, crate, holder, slot));
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CrateGuiHolder holder && holder.type() == GuiType.ITEM_INPUT) {
            int topSize = event.getInventory().getSize();
            boolean allowed = event.getRawSlots().stream().allMatch(slot -> slot >= topSize || slot == ITEM_INPUT_SLOT);
            event.setCancelled(!allowed);
        } else if (event.getInventory().getHolder() instanceof CrateGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !(event.getInventory().getHolder() instanceof CrateGuiHolder holder) || holder.type() != GuiType.ITEM_INPUT) {
            return;
        }
        ItemStack input = event.getInventory().getItem(ITEM_INPUT_SLOT);
        if (usable(input)) {
            saveInputItem(player, holder, input);
            player.getInventory().addItem(input.clone());
            event.getInventory().setItem(ITEM_INPUT_SLOT, null);
        }
        Bukkit.getScheduler().runTask(plugin, () -> reopenAfterItemInput(player, holder));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = prompts.remove(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> handlePrompt(player, prompt, input));
    }

    private void clickMain(Player player, InventoryClickEvent event, CrateGuiHolder holder, int slot) {
        List<CrateDefinition> crates = sortedCrates();
        int maxPage = maxPage(crates.size());
        if (slot == PREVIOUS_PAGE_SLOT && holder.page() > 0) {
            openMain(player, holder.page() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT && holder.page() < maxPage) {
            openMain(player, holder.page() + 1);
            return;
        }
        if (slot == PAGE_INFO_SLOT) {
            prompt(player, new ChatPrompt(PromptType.CREATE_CRATE, null, null), "&aSchreibe die neue Crate-ID in den Chat.");
            return;
        }
        if (slot == 47) {
            openKeyallSettings(player);
            return;
        }
        CrateDefinition crate = crateByDisplayedSlot(slot, holder.page());
        if (crate == null) {
            return;
        }
        if (event.getClick() == ClickType.LEFT) {
            openPreview(player, crate);
        } else if (event.getClick().isShiftClick() && event.getClick().isRightClick()) {
            plugin.crates().remove(crate.getId());
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.6F, 1.2F);
            openMain(player);
        } else if (event.getClick().isRightClick()) {
            openEdit(player, crate);
        }
    }

    private void clickEdit(Player player, InventoryClickEvent event, CrateDefinition crate, int slot) {
        if (slot == 10) {
            openCrateSettings(player, crate);
        } else if (slot == 12) {
            openKeySettings(player, crate);
        } else if (slot == 14) {
            openRewardSettings(player, crate);
        } else if (slot == 16) {
            openMilestoneSettings(player, crate);
        } else if (slot == 22) {
            openMain(player);
        }
    }

    private void clickCrateSettings(Player player, CrateDefinition crate, int slot) {
        if (slot == 16) {
            crate.setLootboxEnabled(!crate.isLootboxEnabled());
            plugin.crates().save();
            openCrateSettings(player, crate);
            return;
        }
        if (slot == 20) {
            crate.setAura(crate.getAura().next());
            plugin.crates().save();
            plugin.crateListener().refreshLookedAtCrate(player);
            openCrateSettings(player, crate);
            return;
        }
        if (slot == 28) {
            openAuraParticleSelect(player, crate, 0);
            return;
        }
        if (slot == 22) {
            prompt(player, new ChatPrompt(PromptType.MODEL_ENGINE_MODEL, crate.getId(), null), "&aSchreibe die ModelEngine-ID in den Chat oder &enone &azum Entfernen.");
            return;
        }
        if (slot == 24) {
            prompt(player, new ChatPrompt(PromptType.HOLOGRAM_HEIGHT, crate.getId(), null), "&aSchreibe die neue Hologramm-Höhe in den Chat, z.B. &e2.25");
            return;
        }
        if (slot == 10) {
            openItemInput(player, crate, INPUT_CRATE_ITEM, null, crate.getCrateItem());
        } else if (slot == 12) {
            prompt(player, new ChatPrompt(PromptType.RENAME_CRATE, crate.getId(), null), "&aSchreibe den neuen Anzeigenamen in den Chat.");
        } else if (slot == 14) {
            player.getInventory().addItem(plugin.markedItems().markCrate(crate));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7F, 1.2F);
        } else if (slot == 16) {
            prompt(player, new ChatPrompt(PromptType.MODEL_ENGINE_MODEL, crate.getId(), null), "&aSchreibe die ModelEngine-ID in den Chat oder &enone &azum Entfernen.");
        } else if (slot == 22) {
            prompt(player, new ChatPrompt(PromptType.HOLOGRAM_HEIGHT, crate.getId(), null), "&aSchreibe die neue Hologramm-Höhe in den Chat, z.B. &e2.25");
        } else if (slot == 31) {
            openEdit(player, crate);
        }
    }

    private void clickKeySettings(Player player, CrateDefinition crate, int slot) {
        if (slot == 11) {
            crate.setKeyRequired(!crate.isKeyRequired());
            plugin.crates().save();
            openKeySettings(player, crate);
        } else if (slot == 13) {
            openItemInput(player, crate, INPUT_KEY_ITEM, null, crate.getKeyItem());
        } else if (slot == 15) {
            player.getInventory().addItem(plugin.markedItems().markKey(crate));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7F, 1.4F);
        } else if (slot == 31) {
            openEdit(player, crate);
        }
    }

    private void clickRewardSettings(Player player, CrateDefinition crate, int slot) {
        if (slot == 11) {
            openRewards(player, crate);
        } else if (slot == 13) {
            openPreview(player, crate, "reward_settings");
        } else if (slot == 15) {
            openAnimationType(player, crate);
        } else if (slot == 31) {
            openEdit(player, crate);
        }
    }

    private void clickMilestoneSettings(Player player, CrateDefinition crate, int slot) {
        if (slot == 11) {
            crate.setMilestoneEnabled(!crate.isMilestoneEnabled());
            plugin.crates().save();
            openMilestoneSettings(player, crate);
        } else if (slot == 13) {
            prompt(player, new ChatPrompt(PromptType.MILESTONE_TARGET, crate.getId(), null), "&aSchreibe das neue Milestone-Ziel in den Chat, z.B. &e50");
        } else if (slot == 15) {
            openMilestoneKeySelect(player, crate, 0);
        } else if (slot == 31) {
            openEdit(player, crate);
        }
    }

    private void clickMilestoneKeySelect(Player player, CrateGuiHolder holder, CrateDefinition crate, int slot) {
        List<CrateDefinition> crates = sortedCrates();
        int maxPage = maxPage(crates.size());
        if (slot == PREVIOUS_PAGE_SLOT && holder.page() > 0) {
            openMilestoneKeySelect(player, crate, holder.page() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT && holder.page() < maxPage) {
            openMilestoneKeySelect(player, crate, holder.page() + 1);
            return;
        }
        if (slot == 53) {
            openMilestoneSettings(player, crate);
            return;
        }
        CrateDefinition selected = crateByDisplayedSlot(slot, holder.page());
        if (selected == null) {
            return;
        }
        crate.setMilestoneRewardCrateId(selected.getId());
        plugin.crates().save();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.55F, 1.3F);
        openMilestoneSettings(player, crate);
    }

    private void clickKeyallSettings(Player player, InventoryClickEvent event, int slot) {
        if (slot == 10) {
            plugin.keyall().setEnabled(!plugin.keyall().isEnabled());
            openKeyallSettings(player);
        } else if (slot == 12) {
            openKeyallCrateSelect(player, 0);
        } else if (slot == 16) {
            int amount = plugin.keyall().configuredAmount() + (event.getClick().isRightClick() ? -1 : 1);
            plugin.keyall().setAmount(amount);
            openKeyallSettings(player);
        } else if (slot == 22) {
            openMain(player);
        }
    }

    private void clickKeyallCrateSelect(Player player, CrateGuiHolder holder, int slot) {
        List<CrateDefinition> crates = sortedCrates();
        int maxPage = maxPage(crates.size());
        if (slot == PREVIOUS_PAGE_SLOT && holder.page() > 0) {
            openKeyallCrateSelect(player, holder.page() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT && holder.page() < maxPage) {
            openKeyallCrateSelect(player, holder.page() + 1);
            return;
        }
        if (slot == 53) {
            openKeyallSettings(player);
            return;
        }
        CrateDefinition selected = crateByDisplayedSlot(slot, holder.page());
        if (selected == null) {
            return;
        }
        plugin.keyall().setCrateId(selected.getId());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.55F, 1.3F);
        openKeyallSettings(player);
    }

    private void clickRewards(Player player, InventoryClickEvent event, CrateGuiHolder holder, CrateDefinition crate, int slot) {
        int maxPage = maxPage(crate.getRewards().size());
        if (slot == PREVIOUS_PAGE_SLOT && holder.page() > 0) {
            openRewards(player, crate, holder.page() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT && holder.page() < maxPage) {
            openRewards(player, crate, holder.page() + 1);
            return;
        }
        if (slot == 48 || slot == 50) {
            RewardType type = slot == 48 ? RewardType.ITEM : RewardType.COMMAND;
            CrateReward reward = new CrateReward(null, type == RewardType.ITEM ? "&bItem Reward" : "&dCommand Reward", type);
            reward.setRarity(RewardRarity.GEWOEHNLICH);
            if (type == RewardType.ITEM) {
                reward.setItem(new ItemStack(Material.DIAMOND));
            } else {
                reward.setCommand("say %player% won a reward");
            }
            crate.getRewards().add(reward);
            plugin.crates().save();
            openRewardEdit(player, crate, reward);
            return;
        }
        if (slot == 53) {
            openRewardSettings(player, crate);
            return;
        }
        int rewardIndex = rewardIndexByDisplayedSlot(slot, holder.page());
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            moveReward(player, crate, rewardIndex, event.getClick() == ClickType.SHIFT_LEFT ? -1 : 1);
            return;
        }
        CrateReward reward = rewardByDisplayedSlot(crate, slot, holder.page());
        if (reward != null) {
            openRewardEdit(player, crate, reward);
        }
    }

    private void moveReward(Player player, CrateDefinition crate, int rewardIndex, int direction) {
        int targetIndex = rewardIndex + direction;
        if (rewardIndex < 0 || targetIndex < 0 || targetIndex >= crate.getRewards().size()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.45F, 0.7F);
            return;
        }
        Collections.swap(crate.getRewards(), rewardIndex, targetIndex);
        plugin.crates().save();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.55F, direction < 0 ? 0.9F : 1.25F);
        openRewards(player, crate, targetIndex / CONTENT_SLOTS.length);
    }

    private void clickRewardEdit(Player player, InventoryClickEvent event, CrateDefinition crate, CrateReward reward, int slot) {
        if (slot == 10) {
            reward.setType(reward.getType() == RewardType.ITEM ? RewardType.COMMAND : RewardType.ITEM);
            plugin.crates().save();
            openRewardEdit(player, crate, reward);
        } else if (slot == 12) {
            openItemInput(player, crate, INPUT_REWARD_ITEM, reward.getId(), reward.getItem());
        } else if (slot == 14) {
            prompt(player, new ChatPrompt(PromptType.COMMAND, crate.getId(), reward.getId()), "&aSchreibe den Command ohne Slash. Nutze &e%spieler% &aoder &e%player% &af\u00fcr den Spieler.");
        } else if (slot == 16) {
            openRaritySelect(player, crate, reward);
        } else if (slot == 20) {
            reward.setBroadcast(!reward.isBroadcast());
            plugin.crates().save();
            openRewardEdit(player, crate, reward);
        } else if (slot == 22) {
            crate.getRewards().removeIf(existing -> existing.getId().equals(reward.getId()));
            plugin.crates().save();
            openRewards(player, crate);
        } else if (slot == 24) {
            prompt(player, new ChatPrompt(PromptType.REWARD_DISPLAY_NAME, crate.getId(), reward.getId()), "&aSchreibe den Anzeigenamen f\u00fcr Chat, Preview und Animation.");
        } else if (slot == 28) {
            giveRewardItemCopy(player, reward);
        } else if (slot == 31) {
            openRewards(player, crate);
        }
    }

    private void giveRewardItemCopy(Player player, CrateReward reward) {
        ItemStack item = reward.getItem();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(Text.chat("<red>Dieser Reward hat noch kein gespeichertes Item.</red>"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.7F);
            return;
        }
        player.getInventory().addItem(item.clone()).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(Text.chat("<green>Item-Kopie erhalten.</green>"));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.65F, 1.35F);
    }

    private void clickRaritySelect(Player player, CrateDefinition crate, CrateReward reward, int slot) {
        if (slot == 22) {
            openRewardEdit(player, crate, reward);
            return;
        }
        int index = slot - 10;
        RewardRarity[] rarities = RewardRarity.values();
        if (index < 0 || index >= rarities.length) {
            return;
        }
        reward.setRarity(rarities[index]);
        if (reward.getRarity().isBroadcastDefault()) {
            reward.setBroadcast(true);
        }
        plugin.crates().save();
        openRewardEdit(player, crate, reward);
    }

    private void clickAnimationType(Player player, CrateDefinition crate, int slot) {
        if (slot == 11) {
            openAnimationSelect(player, crate, false);
        } else if (slot == 15) {
            openAnimationSelect(player, crate, true);
        } else if (slot == 22) {
            openRewardSettings(player, crate);
        }
    }

    private void clickAnimationSelect(Player player, CrateDefinition crate, CrateGuiHolder holder, int slot) {
        if (slot == 40) {
            openAnimationType(player, crate);
            return;
        }
        int index = contentIndex(slot);
        if (index < 0) {
            return;
        }
        boolean guiAnimations = "gui".equalsIgnoreCase(holder.action());
        List<CrateAnimation> animations = new ArrayList<>();
        for (CrateAnimation animation : CrateAnimation.values()) {
            if (animation.isGuiAnimation() == guiAnimations) {
                animations.add(animation);
            }
        }
        if (index >= animations.size()) {
            return;
        }
        crate.setAnimation(animations.get(index));
        plugin.crates().save();
        openRewardSettings(player, crate);
    }

    private void clickAuraParticleSelect(Player player, CrateDefinition crate, CrateGuiHolder holder, int slot) {
        Particle[] particles = Particle.values();
        int maxPage = maxPage(particles.length);
        if (slot == PREVIOUS_PAGE_SLOT && holder.page() > 0) {
            openAuraParticleSelect(player, crate, holder.page() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT && holder.page() < maxPage) {
            openAuraParticleSelect(player, crate, holder.page() + 1);
            return;
        }
        if (slot == 47) {
            crate.setAuraParticle("standard");
            plugin.crates().save();
            plugin.crateListener().refreshLookedAtCrate(player);
            openCrateSettings(player, crate);
            return;
        }
        if (slot == 53) {
            openCrateSettings(player, crate);
            return;
        }
        int index = contentIndex(slot);
        int absoluteIndex = holder.page() * CONTENT_SLOTS.length + index;
        if (index < 0 || absoluteIndex >= particles.length) {
            return;
        }
        crate.setAuraParticle(particles[absoluteIndex].name());
        plugin.crates().save();
        plugin.crateListener().refreshLookedAtCrate(player);
        openCrateSettings(player, crate);
    }

    private void handleItemInputClick(InventoryClickEvent event) {
        int topSize = event.getInventory().getSize();
        int slot = event.getRawSlot();
        if (slot == 15) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            return;
        }
        if (slot >= topSize) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (usable(clicked) && !usable(event.getInventory().getItem(ITEM_INPUT_SLOT))) {
                    event.getInventory().setItem(ITEM_INPUT_SLOT, clicked.clone());
                    event.setCurrentItem(null);
                }
            } else {
                event.setCancelled(false);
            }
            return;
        }
        event.setCancelled(slot != ITEM_INPUT_SLOT);
    }

    private void saveInputItem(Player player, CrateGuiHolder holder, ItemStack input) {
        plugin.crates().find(holder.crateId()).ifPresent(crate -> {
            if (INPUT_CRATE_ITEM.equals(holder.action())) {
                ItemStack saved = single(input);
                crate.setCrateItem(saved);
                crate.setDisplayName(itemDisplayName(saved));
                plugin.crates().save();
                player.sendMessage(Text.chat("<green>Crate Item gespeichert.</green>"));
            } else if (INPUT_KEY_ITEM.equals(holder.action())) {
                ItemStack saved = single(input);
                crate.setKeyItem(saved);
                plugin.crates().save();
                player.sendMessage(Text.chat("<green>Crate Key gespeichert.</green>"));
            } else if (INPUT_REWARD_ITEM.equals(holder.action())) {
                ItemStack saved = input.clone();
                findReward(crate, holder.rewardId()).ifPresent(reward -> {
                    reward.setItem(saved);
                    reward.setExcellentItemTag("");
                    reward.setDisplayName(itemDisplayName(saved));
                    plugin.crates().save();
                    player.sendMessage(Text.chat(reward.getType() == RewardType.ITEM ? "<green>Item Reward gespeichert.</green>" : "<green>Platzhalter Item gespeichert.</green>"));
                });
            }
        });
    }

    private void reopenAfterItemInput(Player player, CrateGuiHolder holder) {
        plugin.crates().find(holder.crateId()).ifPresent(crate -> {
            if (INPUT_REWARD_ITEM.equals(holder.action())) {
                findReward(crate, holder.rewardId()).ifPresent(reward -> openRewardEdit(player, crate, reward));
            } else if (INPUT_CRATE_ITEM.equals(holder.action())) {
                openCrateSettings(player, crate);
            } else if (INPUT_KEY_ITEM.equals(holder.action())) {
                openKeySettings(player, crate);
            } else {
                openEdit(player, crate);
            }
        });
    }

    private void handlePrompt(Player player, ChatPrompt prompt, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            openMain(player);
            return;
        }
        switch (prompt.type()) {
            case CREATE_CRATE -> {
                String id = input.toLowerCase().replaceAll("[^a-z0-9_-]", "");
                if (id.isBlank()) {
                    player.sendMessage(Text.chat("<red>Ung\u00fcltige ID.</red>"));
                    openMain(player);
                    return;
                }
                CrateDefinition crate = plugin.crates().find(id).orElseGet(() -> plugin.crates().create(id));
                openEdit(player, crate);
            }
            case RENAME_CRATE -> plugin.crates().find(prompt.crateId()).ifPresent(crate -> {
                crate.setDisplayName(input);
                plugin.crates().save();
                openCrateSettings(player, crate);
            });
            case COMMAND -> plugin.crates().find(prompt.crateId()).flatMap(crate -> findReward(crate, prompt.rewardId())).ifPresent(reward -> {
                reward.setCommand(input.startsWith("/") ? input.substring(1) : input);
                plugin.crates().save();
                plugin.crates().find(prompt.crateId()).ifPresent(crate -> openRewardEdit(player, crate, reward));
            });
            case MODEL_ENGINE_MODEL -> plugin.crates().find(prompt.crateId()).ifPresent(crate -> {
                crate.setModelEngineModelId(input);
                plugin.crates().save();
                plugin.crateListener().refreshLookedAtCrate(player);
                openCrateSettings(player, crate);
            });
            case REWARD_DISPLAY_NAME -> plugin.crates().find(prompt.crateId()).flatMap(crate -> findReward(crate, prompt.rewardId())).ifPresent(reward -> {
                reward.setDisplayName(input);
                plugin.crates().save();
                plugin.crates().find(prompt.crateId()).ifPresent(crate -> openRewardEdit(player, crate, reward));
            });
            case MILESTONE_TARGET -> plugin.crates().find(prompt.crateId()).ifPresent(crate -> {
                try {
                    crate.setMilestoneTarget(Integer.parseInt(input));
                    plugin.crates().save();
                } catch (NumberFormatException ignored) {
                    player.sendMessage(Text.chat("<red>Das war keine g\u00fcltige Zahl.</red>"));
                }
                openMilestoneSettings(player, crate);
            });
            case HOLOGRAM_HEIGHT -> plugin.crates().find(prompt.crateId()).ifPresent(crate -> {
                try {
                    crate.setHologramHeight(Double.parseDouble(input.replace(',', '.')));
                    plugin.crates().save();
                    plugin.crateListener().refreshLookedAtCrate(player);
                } catch (NumberFormatException ignored) {
                    player.sendMessage(Text.chat("<red>Das war keine gültige Höhe.</red>"));
                }
                openCrateSettings(player, crate);
            });
            case AURA_PARTICLE -> plugin.crates().find(prompt.crateId()).ifPresent(crate -> {
                try {
                    if (!input.equalsIgnoreCase("standard") && !input.equalsIgnoreCase("default") && !input.equalsIgnoreCase("none")) {
                        Particle.valueOf(input.trim().toUpperCase());
                    }
                    crate.setAuraParticle(input);
                    plugin.crates().save();
                    plugin.crateListener().refreshLookedAtCrate(player);
                } catch (IllegalArgumentException ignored) {
                    player.sendMessage(Text.chat("<red>Dieser Partikel wurde nicht gefunden. Nutze z.B. END_ROD, FLAME, PORTAL oder standard.</red>"));
                }
                openCrateSettings(player, crate);
            });
        }
    }

    private void prompt(Player player, ChatPrompt prompt, String message) {
        prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        player.sendMessage(Text.chat("<bold><gradient:#F5B642:#9B4DFF>EmpireCrates</gradient></bold> <dark_gray>></dark_gray> " + message));
        player.sendMessage(Text.chat("<dark_gray>Schreibe <red>cancel</red> zum Abbrechen.</dark_gray>"));
    }

    private ItemStack categorySign(String name, String... lore) {
        List<String> lines = new ArrayList<>();
        for (String entry : lore) {
            lines.add("<gray>" + entry + "</gray>");
        }
        lines.add("<dark_gray>Klicken zum Öffnen.</dark_gray>");
        return new ItemBuilder(Material.OAK_SIGN).name(name).lore(lines).build();
    }

    private void fillFrame(Inventory inventory) {
        ItemStack glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("&8").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == inventory.getSize() / 9 - 1 || col == 0 || col == 8) {
                inventory.setItem(i, glass);
            }
        }
    }

    private void paintSection(Inventory inventory, int firstSlot, Material material, String title, String description) {
        ItemStack filler = new ItemBuilder(material).name("&8").build();
        for (int slot = firstSlot; slot < firstSlot + 9 && slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(firstSlot, new ItemBuilder(Material.OAK_SIGN)
                .name(title)
                .lore("<gray>" + description + "</gray>")
                .build());
    }

    private ItemStack rewardIcon(CrateReward reward, boolean adminView) {
        ItemStack icon = reward.getItem() != null ? reward.getItem() : new ItemStack(reward.getType() == RewardType.COMMAND ? Material.NAME_TAG : Material.DIAMOND);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            if (!meta.hasDisplayName()) {
                meta.displayName(Text.mm(reward.getDisplayName()));
            }
            List<Component> mergedLore = new ArrayList<>();
            if (meta.lore() != null) {
                mergedLore.addAll(meta.lore());
                mergedLore.add(Component.empty());
            }
            mergedLore.addAll(Text.mm(rewardInfoLore(reward, adminView)));
            meta.lore(mergedLore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack keyCrateIcon(CrateDefinition crate, boolean selected) {
        return new ItemBuilder(crate.getKeyItem())
                .name(crate.getDisplayName())
                .lore("<dark_gray>ID:</dark_gray> <gray>" + crate.getId() + "</gray>",
                        selected ? "<green>Aktuell ausgewählt.</green>" : "<gray>Klicken, um diesen Key zu wählen.</gray>")
                .glowless()
                .build();
    }

    private List<String> rewardInfoLore(CrateReward reward, boolean adminView) {
        return adminView
                ? List.of("<dark_gray>Typ:</dark_gray> <gray>" + reward.getType() + "</gray>",
                "<dark_gray>Anzeigename:</dark_gray> " + reward.getDisplayName(),
                "<dark_gray>Seltenheit:</dark_gray> " + reward.getRarity().miniMessage(),
                "<dark_gray>Seltenheits-Chance:</dark_gray> <gradient:#F5B642:#9B4DFF>" + reward.getChancePercent() + "%</gradient>",
                "<dark_gray>Effektive Item-Chance:</dark_gray> <gradient:#7CFFB2:#F5B642>" + effectiveRewardChance(reward) + "%</gradient>",
                "<dark_gray>Broadcast:</dark_gray> <gray>" + (reward.isBroadcast() ? "An" : "Aus") + "</gray>",
                "<gray>Klicken zum Bearbeiten.</gray>",
                "<dark_gray>Shift-Links: nach links</dark_gray>",
                "<dark_gray>Shift-Rechts: nach rechts</dark_gray>")
                : List.of("<dark_gray>Seltenheit:</dark_gray> " + reward.getRarity().miniMessage(),
                "<dark_gray>Chance:</dark_gray> <gradient:#7CFFB2:#F5B642>" + effectiveRewardChance(reward) + "%</gradient>");
    }

    private double effectiveRewardChance(CrateReward reward) {
        CrateDefinition crate = plugin.crates().all().stream()
                .filter(candidate -> candidate.getRewards().stream().anyMatch(existing -> existing.getId().equals(reward.getId())))
                .findFirst()
                .orElse(null);
        if (crate == null) {
            return 0.0D;
        }
        return RewardChanceCalculator.effectivePercent(crate.getRewards(), reward);
    }

    private ItemStack animationIcon(CrateAnimation animation) {
        return new ItemBuilder(animation.icon())
                .name("<gradient:#F5B642:#9B4DFF>Open Animation: " + animation.displayName() + "</gradient>")
                .lore("<gray>Typ:</gray> <gradient:#7CFFB2:#F5B642>" + animation.category() + "</gradient>",
                        "<gray>" + animation.description() + "</gray>",
                        "<gray>Klicken, um Typ und Animation auszuwählen.</gray>")
                .build();
    }

    private ItemStack auraIcon(CrateAura aura) {
        return new ItemBuilder(aura.icon())
                .name("<gradient:#F5B642:#9B4DFF>Aura: " + aura.displayName() + "</gradient>")
                .lore("<gray>" + aura.description() + "</gray>",
                        "<gray>Klicken, um die Aura-Form zu wechseln.</gray>",
                        "<dark_gray>Keine Aura -> Ring -> Helix -> Pulse -> Flame -> Galaxy</dark_gray>")
                .build();
    }

    private ItemStack auraParticleIcon(CrateDefinition crate) {
        return new ItemBuilder(Material.GLOWSTONE_DUST)
                .name("<gradient:#F5B642:#9B4DFF>Aura Partikel</gradient>")
                .lore("<gray>Aktuell:</gray> <gradient:#7CFFB2:#F5B642>" + crate.getAuraParticleName() + "</gradient>",
                        "<gray>Klicken, um alle Minecraft-Partikel zu sehen.</gray>",
                        "<dark_gray>standard nutzt den Partikel der Aura-Form.</dark_gray>")
                .build();
    }

    private ItemStack particleIcon(CrateDefinition crate, Particle particle) {
        boolean selected = crate.getAuraParticleName().equalsIgnoreCase(particle.name());
        List<String> lore = new ArrayList<>(Arrays.asList(
                "<gray>Partikel:</gray> <gradient:#7CFFB2:#F5B642>" + particle.name() + "</gradient>",
                selected ? "<green>Aktuell ausgewählt.</green>" : "<gray>Klicken, um diesen Partikel zu nutzen.</gray>"
        ));
        if (particle.getDataType() != Void.class) {
            lore.add("<dark_gray>Braucht Minecraft-Daten und wird bei der Aura automatisch geschützt.</dark_gray>");
        }
        return new ItemBuilder(selected ? Material.LIME_DYE : Material.GLOWSTONE_DUST)
                .name("<gradient:#F5B642:#9B4DFF>" + particle.name() + "</gradient>")
                .lore(lore)
                .build();
    }

    private CrateDefinition crateByDisplayedSlot(int slot, int page) {
        int index = contentIndex(slot);
        if (index < 0) {
            return null;
        }
        int absoluteIndex = page * CONTENT_SLOTS.length + index;
        List<CrateDefinition> crates = sortedCrates();
        return absoluteIndex < crates.size() ? crates.get(absoluteIndex) : null;
    }

    private CrateReward rewardByDisplayedSlot(CrateDefinition crate, int slot, int page) {
        int absoluteIndex = rewardIndexByDisplayedSlot(slot, page);
        return absoluteIndex >= 0 && absoluteIndex < crate.getRewards().size() ? crate.getRewards().get(absoluteIndex) : null;
    }

    private int rewardIndexByDisplayedSlot(int slot, int page) {
        int index = contentIndex(slot);
        return index < 0 ? -1 : page * CONTENT_SLOTS.length + index;
    }

    private List<CrateDefinition> sortedCrates() {
        List<CrateDefinition> crates = new ArrayList<>(plugin.crates().all());
        crates.sort(Comparator.comparing(CrateDefinition::getId));
        return crates;
    }

    private void addPageControls(Inventory inventory, int page, int maxPage) {
        if (page > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, new ItemBuilder(Material.ARROW).name("<gradient:#F5B642:#9B4DFF>Vorherige Seite</gradient>").lore("<gray>Seite " + page + " öffnen.</gray>").build());
        }
        inventory.setItem(PAGE_INFO_SLOT, new ItemBuilder(Material.BOOK).name("<gradient:#F5B642:#9B4DFF>Seite " + (page + 1) + "/" + (maxPage + 1) + "</gradient>").lore("<gray>Weitere Rewards werden auf extra Seiten angezeigt.</gray>").build());
        if (page < maxPage) {
            inventory.setItem(NEXT_PAGE_SLOT, new ItemBuilder(Material.ARROW).name("<gradient:#7CFFB2:#F5B642>Nächste Seite</gradient>").lore("<gray>Seite " + (page + 2) + " öffnen.</gray>").build());
        }
    }

    private int maxPage(int itemCount) {
        return Math.max(0, (Math.max(0, itemCount) - 1) / CONTENT_SLOTS.length);
    }

    private int clampPage(int page, int maxPage) {
        return Math.max(0, Math.min(page, maxPage));
    }

    private Optional<CrateReward> findReward(CrateDefinition crate, String rewardId) {
        return crate.getRewards().stream().filter(reward -> reward.getId().equals(rewardId)).findFirst();
    }

    private int contentIndex(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private boolean usable(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private ItemStack single(ItemStack item) {
        ItemStack clone = item.clone();
        clone.setAmount(1);
        return clone;
    }

    private String itemDisplayName(ItemStack item) {
        if (item == null) {
            return "<gradient:#F5B642:#9B4DFF>Item</gradient>";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            Component displayName = item.getItemMeta().displayName();
            if (displayName != null) {
                return MiniMessage.miniMessage().serialize(displayName);
            }
        }
        return "<gradient:#F5B642:#9B4DFF>" + readableMaterialName(item.getType()) + "</gradient>";
    }

    private String readableMaterialName(Material material) {
        String lower = material.name().toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder(lower.length());
        boolean upper = true;
        for (char character : lower.toCharArray()) {
            if (character == ' ') {
                upper = true;
                builder.append(character);
                continue;
            }
            builder.append(upper ? Character.toUpperCase(character) : character);
            upper = false;
        }
        return builder.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyModelValue(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private String cleanNumber(double value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String nextCrateId(String current) {
        List<CrateDefinition> crates = new ArrayList<>(plugin.crates().all());
        crates.sort(Comparator.comparing(CrateDefinition::getId));
        if (crates.isEmpty()) {
            return current;
        }
        for (int index = 0; index < crates.size(); index++) {
            if (crates.get(index).getId().equalsIgnoreCase(current)) {
                return crates.get((index + 1) % crates.size()).getId();
            }
        }
        return crates.get(0).getId();
    }
}



