# TakuLabs
## Minecraft Plugin Portfolio

Java-based Minecraft plugin development focused on Paper systems, backend integrations, gameplay features and maintainable server tooling.

This repository is a public portfolio. It includes selected source showcases, documentation-only project pages and small code excerpts from real plugin projects. Full private server codebases and deployment files are not published.

## Highlights

- [EmpireCrates](plugins/EmpireCrates) - crate rewards, keys, admin GUI and player progress.
- [MythicSpawner](plugins/MythicSpawner) - custom spawner tooling with PDC state and GUI pagination.
- [NovaGuard](plugins/NovaGuard) - connection limits and ProtocolLib-backed packet safeguards.
- [Full Plugin Catalog](docs/plugin-catalog.md) - 51 included Minecraft plugins with real source snippets.

<p align="center">
  <img src="assets/empirecrates/crate-showcase.gif" alt="EmpireCrates in-game crate showcase" width="880">
</p>

<p align="center"><sub>EmpireCrates in-game crate showcase and opening system</sub></p>

## Plugin Coverage

| Category | Plugins |
| --- | ---: |
| Core / Infrastructure | 8 |
| Economy | 14 |
| Gameplay | 10 |
| Moderation / Security | 6 |
| Other | 3 |
| Utilities | 10 |

## Engineering Case Studies

- [SellChest Performance](docs/case-studies/sellchest-performance.md) - bounded per-tick economy work.
- [Discord / Minecraft Platform](docs/case-studies/discord-minecraft-platform.md) - Paper, Discord, database and web module boundaries.
- [Container Optimization](docs/case-studies/container-optimization.md) - container/hopper hotspot profiling.
- [Bounty Abuse Prevention](docs/case-studies/bounty-abuse-prevention.md) - repeated reward-growth guard design.

## Technologies

**Core:** `Java 21` · `Paper API` · `Bukkit API` · `Maven` · `YAML`

**Selected integrations:** `Vault` · `PlayerPoints` · `PlaceholderAPI` · `ProtocolLib` · `ModelEngine` · `JDA` · `JDBC`

## Selected Core Examples

The complete private core is not included. Only small standalone examples are published.

- [Message Helper](core-examples/message-helper) - formatted MiniMessage help output and legacy RGB gradient helpers.
- [Offline Player Resolver](core-examples/offline-player-resolver) - UUID/name lookup helper for Bukkit `OfflinePlayer` resolution.
- [Trade Session State](core-examples/trade-session-state) - small state model for expiring trade requests and ready-state handling.

## Documentation

- [Plugin Catalog](docs/plugin-catalog.md)
- [Project Overview](docs/projects.md)
- [Architecture](docs/architecture.md)
- [Project Selection](docs/evaluation.md)

## Repository Structure

```text
minecraft-plugin-portfolio/
├─ plugins/
├─ docs/
│  ├─ plugins/
│  └─ case-studies/
├─ core-examples/
└─ assets/
```

## About

I focus on Java-based Minecraft server development, primarily building smaller plugins and focused gameplay or server systems. The portfolio keeps examples reviewable without publishing private infrastructure or complete private server codebases.
