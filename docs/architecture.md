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

The portfolio now has compact pages for small utilities and more detailed pages for larger plugins. `EmpireCrates` is still the largest source showcase, with command handling, GUI state, chat prompt editing, YAML persistence, reward models, physical key validation, optional integrations and runtime visual behavior.

The economy plugins show different menu-driven workflows and persistence choices: shops, auctions, crates, keys and sell systems. Utility plugins cover smaller command and quality-of-life features. Security and moderation plugins only show small code sections that do not expose private detection logic or infrastructure.

## Documentation-Only Systems

Some systems are described through architecture notes instead of full source:

- `MythicSpawner`: custom spawner tool and converted-spawner flow
- `SellChest Performance`: bounded economy work and chunk-level limits
- `Discord / Minecraft Platform`: multi-module Paper, Discord, database and web architecture
- `Container Optimization`: hotspot profiling and adaptive throttling
- `Bounty Abuse Prevention`: pair-based cooldown logic for repeated reward growth

These notes show the boundaries and tradeoffs without publishing private server infrastructure or complete private implementations.

## Core Examples

The core examples and plugin snippets are intentionally small. They show useful code structure without exposing the full private server core.

## Build Artifacts

Generated jars, `target/`, IDE files and logs are excluded from the repository.
