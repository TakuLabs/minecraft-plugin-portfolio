# OtakuFakeChat

Fake chat and player-message command utility.

| Field | Value |
| --- | --- |
| Version | `1.0.1` |
| Build | Maven |
| Main class | `de.otaku.fakechat.OtakuFakeChatPlugin` |
| Source files | 2 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Fake chat and player-message command utility
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/fakechat/FakeChatCommand.java

Command handling from the plugin source.

```java
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!isAuthorized(sender)) {
            sender.sendMessage(ChatColor.RED + "Unbekannter Befehl. Gib \"/help\" ein, um Hilfe zu erhalten.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /" + label + " <Spieler> <Nachricht>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Dieser Spieler ist nicht online.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String commandCheck = message.stripLeading();
        if (commandCheck.startsWith("/") || commandCheck.startsWith("\uFF0F")) {
            sender.sendMessage(ChatColor.RED + "Commands sind nicht erlaubt. Es können nur Chatnachrichten gesendet werden.");
            return true;
        }

        if (message.isBlank()) {
            sender.sendMessage(ChatColor.RED + "Die Nachricht darf nicht leer sein.");
            return true;
        }

        target.chat(message);
        sender.sendMessage(ChatColor.GREEN + "Nachricht als " + target.getName() + " gesendet.");
        return true;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
