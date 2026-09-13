# TakuLabs
## Java Backend and Minecraft Systems

Java-based Minecraft development focused on Paper plugins, backend integrations, economy systems and maintainable server tooling.

This repository contains selected public-safe showcase projects and engineering notes. It is not a full private server archive; the goal is to show code structure, system boundaries and the way larger features are split into smaller services.

## Featured Projects

### [EmpireCrates](plugins/EmpireCrates)

Flagship crate-system module extracted from a larger private server core and prepared as a focused public showcase.

`Java 21` · `Paper` · `Maven` · `PlaceholderAPI` · `YAML`

- YAML-backed crate, key and reward definitions
- Admin GUI flow with chat prompts for editing crate settings
- Item and command rewards with rarity, chance and broadcast controls
- Physical crate keys with `PersistentDataContainer` validation
- Persistent placed crate and player milestone progress storage

<p align="center">
  <img src="assets/empirecrates/crate-showcase.gif" alt="EmpireCrates in-game crate showcase" width="880">
</p>

<p align="center"><sub>In-game crate showcase and opening system</sub></p>

### [MythicSpawner](plugins/MythicSpawner)

Documentation-only showcase for a custom spawner system. The private source is not included.

`Java 21` · `Paper` · `Maven` · `PersistentDataContainer` · `JUnit`

- Custom spawner pickaxe flow with configurable drop and conversion rules
- Persistent item and block state using Bukkit `PersistentDataContainer`
- Paginated mob-selection GUI for converted spawners
- Compatibility guard for virtual spawner plugins

### [Discord / Minecraft Platform](docs/case-studies/discord-minecraft-platform.md)

Architecture case study for a multi-module platform connecting a Paper plugin, Discord bot, database layer and web backend.

`Java 21` · `Paper` · `JDA` · `JDBC` · `OAuth2` · `Maven`

## Plugin Projects

| Project | Focus | Highlights |
| --- | --- | --- |
| [SMPShop](plugins/SMPShop) | Economy shop | Category menus, quantity selector, buy/sell pricing, Vault and PlayerPoints hooks |
| [OtakuAuctionHouse](plugins/OtakuAuctionHouse) | Auction house | Player listings, inventory browsing, purchase flow, serialized item storage |
| [NovaGuard](plugins/NovaGuard) | Server protection | IP account limits, whitelist/blacklist handling, ProtocolLib packet-rate checks |
| [BirthdayEvent](plugins/BirthdayEvent) | Timed event | Timezone-aware scheduling, configurable rewards, restart-safe state tests |

## Engineering Case Studies

- [SellChest Performance](docs/case-studies/sellchest-performance.md) - reducing tick impact in a private economy subsystem.
- [Container Optimization](docs/case-studies/container-optimization.md) - profiling and throttling container/hopper hotspots.
- [Bounty Abuse Prevention](docs/case-studies/bounty-abuse-prevention.md) - limiting repeated bounty growth from the same player pair.

## Development Focus

- Modular Paper plugin structure
- Inventory GUI workflows
- Configuration-driven behavior
- Economy and reward systems
- Persistence boundaries around YAML, JDBC or plugin data stores
- Soft dependency handling for external APIs
- Service-level tests where they add confidence

## Technologies

**Core:** `Java 21` · `Paper API` · `Bukkit API` · `Maven` · `YAML`

**Selected integrations:** `Vault` · `PlayerPoints` · `PlaceholderAPI` · `ProtocolLib` · `ModelEngine` · `JDA` · `JDBC`

## Selected Core Examples

The complete private core is not included. Only small standalone examples are published.

- [Message Helper](core-examples/message-helper) - formatted MiniMessage help output and legacy RGB gradient helpers.
- [Offline Player Resolver](core-examples/offline-player-resolver) - UUID/name lookup helper for Bukkit `OfflinePlayer` resolution.
- [Trade Session State](core-examples/trade-session-state) - small state model for expiring trade requests and ready-state handling.

## Documentation

- [Project Overview](docs/projects.md)
- [Architecture](docs/architecture.md)
- [Project Selection](docs/evaluation.md)

## Repository Structure

```text
minecraft-plugin-portfolio/
├─ plugins/
├─ core-examples/
├─ docs/
└─ assets/
```

## About

I focus on Java-based Minecraft server development, primarily building smaller plugins and focused gameplay or server systems. This portfolio provides a practical overview of how I structure code, work with the Paper API and implement common server features.

No generated jars, private deployment files or full private core projects are included.
