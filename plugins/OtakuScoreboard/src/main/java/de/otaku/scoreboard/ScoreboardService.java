package de.otaku.scoreboard;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class ScoreboardService {
    private static final String OBJECTIVE_NAME = "otaku_board";
    private static final String BELOW_NAME_OBJECTIVE = "ot_below";
    private static final String TEAM_PREFIX = "otaku_line_";
    private static final String[] ENTRIES = {
            ChatColor.BLACK.toString(),
            ChatColor.DARK_BLUE.toString(),
            ChatColor.DARK_GREEN.toString(),
            ChatColor.DARK_AQUA.toString(),
            ChatColor.DARK_RED.toString(),
            ChatColor.DARK_PURPLE.toString(),
            ChatColor.GOLD.toString(),
            ChatColor.GRAY.toString(),
            ChatColor.DARK_GRAY.toString(),
            ChatColor.BLUE.toString(),
            ChatColor.GREEN.toString(),
            ChatColor.AQUA.toString(),
            ChatColor.RED.toString(),
            ChatColor.LIGHT_PURPLE.toString(),
            ChatColor.YELLOW.toString()
    };

    private final OtakuScoreboardPlugin plugin;
    private final TextRenderer textRenderer;
    private final Set<UUID> disabledPlayers = new HashSet<>();

    private String title;
    private List<String> lines;
    private String viewPermission;
    private boolean placeholderApiAvailable;
    private boolean sortingEnabled;
    private boolean sortingCaseSensitive;
    private boolean sortingCollisionEnabled;
    private boolean sortingInvisibleNametags;
    private boolean sortingCanSeeFriendlyInvisibles;
    private String sortingDisableCondition;
    private List<String> sortingGroups;
    private List<String> sortingGroupPlaceholders;
    private boolean nametagEnabled;
    private String nametagPrefix;
    private String nametagSuffix;
    private boolean armorStandNametagsEnabled;
    private boolean armorStandHideVanillaNametag;
    private boolean armorStandDisableBelowName;
    private boolean belowNameEnabled;
    private String belowNameTitle;
    private String belowNameValue;

    public ScoreboardService(OtakuScoreboardPlugin plugin, TextRenderer textRenderer) {
        this.plugin = plugin;
        this.textRenderer = textRenderer;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        title = config.getString("scoreboard.title", "<aqua>Otaku</aqua>");
        lines = config.getStringList("scoreboard.lines").stream().limit(ENTRIES.length).toList();
        viewPermission = config.getString("view-permission", "");
        placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        sortingEnabled = config.getBoolean("scoreboard-teams.enabled", true);
        sortingCaseSensitive = config.getBoolean("scoreboard-teams.case-sensitive-sorting", true);
        sortingCollisionEnabled = config.getBoolean("scoreboard-teams.enable-collision", true);
        sortingInvisibleNametags = config.getBoolean("scoreboard-teams.invisible-nametags", false);
        sortingCanSeeFriendlyInvisibles = config.getBoolean("scoreboard-teams.can-see-friendly-invisibles", false);
        sortingDisableCondition = config.getString("scoreboard-teams.disable-condition", "");
        sortingGroups = readSortingGroups(config.getStringList("scoreboard-teams.sorting-types"));
        sortingGroupPlaceholders = config.getStringList("scoreboard-teams.group-placeholders");
        nametagEnabled = config.getBoolean("scoreboard-teams.nametag.enabled", true);
        nametagPrefix = config.getString("scoreboard-teams.nametag.prefix", "%luckperms_prefix% ");
        nametagSuffix = config.getString("scoreboard-teams.nametag.suffix", "");
        armorStandNametagsEnabled = config.getBoolean("scoreboard-teams.armorstand.enabled", false);
        armorStandHideVanillaNametag = config.getBoolean("scoreboard-teams.armorstand.hide-vanilla-nametag", true);
        armorStandDisableBelowName = config.getBoolean("scoreboard-teams.armorstand.disable-belowname-while-enabled", true);
        belowNameEnabled = config.getBoolean("scoreboard-teams.belowname.enabled", false);
        belowNameTitle = config.getString("scoreboard-teams.belowname.title", "<red>❤</red>");
        belowNameValue = config.getString("scoreboard-teams.belowname.value", "%health%");
        updateAll();
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            clearAll();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (canView(player)) {
                update(player);
            } else {
                clear(player);
            }
        }
    }

    public void update(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, render(player, title));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.displayName(render(player, title));
        }

        int score = lines.size();
        for (int index = 0; index < lines.size(); index++) {
            String entry = ENTRIES[index];
            Team team = getOrCreateTeam(scoreboard, index, entry);
            team.prefix(render(player, lines.get(index)));
            objective.getScore(entry).setScore(score--);
        }

        clearUnusedTeams(scoreboard);
        updateSortingTeams(player, scoreboard);
        updateBelowName(scoreboard);
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPlayers.remove(uuid)) {
            update(player);
            return true;
        }

        disabledPlayers.add(uuid);
        clear(player);
        return false;
    }

    public void clear(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
    }

    private boolean canView(Player player) {
        if (disabledPlayers.contains(player.getUniqueId())) {
            return false;
        }
        return viewPermission == null || viewPermission.isBlank() || player.hasPermission(viewPermission);
    }

    private Component render(Player player, String input) {
        return textRenderer.render(player, input, placeholderApiAvailable);
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, int index, String entry) {
        Team team = scoreboard.getTeam(TEAM_PREFIX + index);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_PREFIX + index);
        }
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
        return team;
    }

    private void clearUnusedTeams(Scoreboard scoreboard) {
        for (int index = lines.size(); index < ENTRIES.length; index++) {
            Team team = scoreboard.getTeam(TEAM_PREFIX + index);
            if (team != null) {
                team.unregister();
            }
            scoreboard.resetScores(ENTRIES[index]);
        }
    }

    private void updateSortingTeams(Player viewer, Scoreboard scoreboard) {
        if (!sortingEnabled || isSortingDisabled(viewer)) {
            clearSortingTeams(scoreboard);
            return;
        }

        Set<String> activeTeamNames = new HashSet<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamName = sortingTeamName(target);
            activeTeamNames.add(teamName);

            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            for (String entry : Set.copyOf(team.getEntries())) {
                if (!entry.equals(target.getName())) {
                    team.removeEntry(entry);
                }
            }
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }

            team.setOption(Team.Option.COLLISION_RULE, sortingCollisionEnabled ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                    sortingInvisibleNametags || armorStandNametagsEnabled && armorStandHideVanillaNametag ? Team.OptionStatus.NEVER : Team.OptionStatus.ALWAYS);
            team.setCanSeeFriendlyInvisibles(sortingCanSeeFriendlyInvisibles);
            if (nametagEnabled && (!armorStandNametagsEnabled || !armorStandHideVanillaNametag)) {
                team.prefix(render(target, nametagPrefix));
                team.suffix(render(target, nametagSuffix));
            } else {
                team.prefix(Component.empty());
                team.suffix(Component.empty());
            }
        }

        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("ot_sort_") && !activeTeamNames.contains(team.getName())) {
                team.unregister();
            }
        }
    }

    private void clearSortingTeams(Scoreboard scoreboard) {
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("ot_sort_")) {
                team.unregister();
            }
        }
    }

    private String sortingTeamName(Player player) {
        String playerName = sortingCaseSensitive ? player.getName() : player.getName().toLowerCase(Locale.ROOT);
        String group = primaryGroup(player);
        int groupIndex = sortingGroups.indexOf(group);
        if (groupIndex < 0) {
            groupIndex = sortingGroups.indexOf("default");
        }
        if (groupIndex < 0) {
            groupIndex = 99;
        }

        String safeName = playerName.replaceAll("[^A-Za-z0-9_]", "");
        if (safeName.length() > 8) {
            safeName = safeName.substring(0, 8);
        }
        return "ot_sort_" + String.format("%02d", Math.min(groupIndex, 99)) + "_" + safeName;
    }

    private String primaryGroup(Player player) {
        for (String placeholder : sortingGroupPlaceholders) {
            String value = textRenderer.parse(player, placeholder, placeholderApiAvailable).trim();
            if (!value.isBlank() && !value.equals(placeholder) && !value.contains("%")) {
                return value.toLowerCase(Locale.ROOT);
            }
        }
        return "default";
    }

    private List<String> readSortingGroups(List<String> sortingTypes) {
        return sortingTypes.stream()
                .filter(type -> type.toUpperCase(Locale.ROOT).startsWith("GROUPS:"))
                .findFirst()
                .map(type -> type.substring("GROUPS:".length()))
                .stream()
                .flatMap(groups -> List.of(groups.split(",")).stream())
                .map(group -> group.trim().toLowerCase(Locale.ROOT))
                .filter(group -> !group.isBlank())
                .toList();
    }

    private boolean isSortingDisabled(Player viewer) {
        if (sortingDisableCondition == null || sortingDisableCondition.isBlank() || !sortingDisableCondition.contains("=")) {
            return false;
        }

        String[] parts = sortingDisableCondition.split("=", 2);
        String left = textRenderer.parse(viewer, parts[0], placeholderApiAvailable);
        String right = textRenderer.parse(viewer, parts[1], placeholderApiAvailable);
        return left.equals(right);
    }

    private void updateBelowName(Scoreboard scoreboard) {
        Objective objective = scoreboard.getObjective(BELOW_NAME_OBJECTIVE);
        if (!belowNameEnabled || armorStandNametagsEnabled && armorStandDisableBelowName) {
            if (objective != null) {
                objective.unregister();
            }
            return;
        }

        if (objective == null) {
            objective = scoreboard.registerNewObjective(BELOW_NAME_OBJECTIVE, Criteria.DUMMY, renderForConsole(belowNameTitle));
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        } else {
            objective.displayName(renderForConsole(belowNameTitle));
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            objective.getScore(target.getName()).setScore(parseBelowNameValue(target));
        }
    }

    private int parseBelowNameValue(Player player) {
        String parsed = textRenderer.parse(player, belowNameValue, placeholderApiAvailable)
                .replaceAll("[^0-9-]", "");
        if (parsed.isBlank() || parsed.equals("-")) {
            return 0;
        }
        try {
            return Integer.parseInt(parsed);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private Component renderForConsole(String input) {
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) {
            return Component.text(input == null ? "" : input);
        }
        return render(player, input);
    }

}
