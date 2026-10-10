# Architecture Notes

## Common Patterns

- Maven project layout with `src/main/java` and `src/main/resources`
- `plugin.yml` metadata for commands, permissions and soft dependencies
- Java 21 compilation
- YAML configuration for user-facing messages and feature settings
- Separate packages for commands, listeners, models, storage, services and utilities where the plugin size justifies it
- Optional integration code kept behind soft dependencies or adapter classes

## Plugin Structure

The selected plugins generally follow this shape:

```text
src/main/java/<package>/
  command/
  listener/
  model/
  storage/
  service/
  util/

src/main/resources/
  plugin.yml
  config.yml
```

Not every plugin has every package. Smaller plugins keep fewer classes; larger plugins split command, GUI, storage and model logic more clearly.

## Included Source Projects

The portfolio now contains a broader plugin catalog with compact pages for small utilities and more detailed pages for larger systems. `EmpireCrates` remains the broadest source showcase, combining command handling, GUI state, chat prompt editing, YAML persistence, reward models, physical key validation, optional integrations and runtime visual behavior.

Economy plugins such as the shop, auction, crate, key and sell systems show different approaches to menu-driven workflows and persistence. Utility plugins cover smaller command and quality-of-life features. Security and moderation plugins are represented with selected public-safe excerpts, keeping private detection logic and infrastructure details out of the repository.

## Documentation-Only Systems

Some systems are represented as architecture notes instead of full source:

- `MythicSpawner`: custom spawner tool and converted-spawner flow
- `SellChest Performance`: bounded economy work and chunk-level limits
- `Discord / Minecraft Platform`: multi-module Paper, Discord, database and web architecture
- `Container Optimization`: hotspot profiling and adaptive throttling
- `Bounty Abuse Prevention`: pair-based cooldown logic for repeated reward growth

These notes show design boundaries without publishing private server infrastructure or complete private implementations.

## Core Examples

The core examples, catalog excerpts and extracted source showcases are intentionally limited to public-safe code. They are included to show focused implementation patterns without exposing a complete private server core.

## Build Artifacts

Generated jars, `target/`, IDE files and logs are excluded from the repository.
