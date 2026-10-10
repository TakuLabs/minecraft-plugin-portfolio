# OtakuAC

Security architecture and event pipeline.

| Field | Value |
| --- | --- |
| Version | `1.8.3` |
| Build | Maven |
| Main class | `de.otaku.anticheat.OtakuACPlugin` |
| Source files | 1890 |
| Tests | 0 |
| Dependencies | Maven · spigot-api |

## Technical Notes

- Category: Moderation / Security
- Focus: Security architecture and event pipeline
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. work/reference/ac/grim/grimac/manager/ParsedCommand.java

Service-level logic selected from the plugin source.

```java
   public ParsedCommand(int threshold, int interval, String command) {
      this.threshold = threshold;
      this.interval = interval;
      this.command = command;
   }
```

### 2. work/reference/ac/grim/grimac/shaded/configuralize/Source.java

Configuration handling from the plugin source.

```java
   public Source(DynamicConfig config, Class<?> clazz, String resource, File file) {
      this.config = config;
      this.clazz = clazz;
      this.resource = resource;
      this.file = file.getAbsoluteFile();
   }
```

### 3. work/reference/ac/grim/grimac/shaded/incendo/cloud/Command.java

Command handling from the plugin source.

```java
   public Command(final @NonNull List<@NonNull CommandComponent<C>> commandComponents, final @NonNull CommandExecutionHandler<@NonNull C> commandExecutionHandler, final @Nullable Type senderType, final @NonNull Permission permission, final @NonNull CommandMeta commandMeta, final @NonNull CommandDescription commandDescription) {
      this.components = (List)Objects.requireNonNull(commandComponents, "Command components may not be null");
      if (this.components.isEmpty()) {
         throw new IllegalArgumentException("At least one command component is required");
      } else {
         this.flagComponent = (CommandComponent)this.components.stream().filter((ca) -> ca.type() == CommandComponent.ComponentType.FLAG).findFirst().orElse((Object)null);
         boolean foundOptional = false;

         for(CommandComponent<C> component : this.components) {
            if (component.name().isEmpty()) {
               throw new IllegalArgumentException("Component names may not be empty");
            }

            if (foundOptional && component.required()) {
               throw new IllegalArgumentException(String.format("Command component '%s' cannot be placed after an optional argument", component.name()));
            }

            if (!component.required()) {
               foundOptional = true;
            }
         }

         this.commandExecutionHandler = commandExecutionHandler;
         this.senderType = senderType;
         this.permission = permission;
         this.commandMeta = commandMeta;
         this.commandDescription = commandDescription;
      }
   }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
