# Project Notes

## Selected Plugins

### EmpireCrates

Purpose: extracted crate-system module with rewards, admin tooling, visual opening behavior and player progress.

Shows the largest focused showcase in this portfolio: YAML-backed crate definitions, item and command rewards, physical key validation, rarity/chance handling, admin GUIs, chat prompts, placed crate storage, player milestone progress and optional integrations.

### MythicSpawner

Purpose: documentation-only showcase for a private custom spawner system.

Shows event-decision structure, persistent item and block tagging, custom tool behavior, paginated GUI flow, config reload handling and compatibility checks for virtual spawner plugins.

### SMPShop

Purpose: server-controlled SMP shop with configurable categories, buy/sell prices and inventory workflows.

Shows a focused economy-shop module with category menus, quantity selection, item purchase flow, item selling flow, YAML category definitions and optional economy integrations.

### OtakuAuctionHouse

Purpose: inventory-based player auction house.

Shows player-to-player economy integration, menu flow, serialized item storage and permission-based listing limits.

### NovaGuard

Purpose: server protection plugin for IP limits, access lists and packet monitoring.

Shows service separation, rate limiting, ProtocolLib integration, privacy-aware IP handling and unit tests.

### BirthdayEvent

Purpose: one-time event plugin with scheduling, message configuration and reward claiming.

Shows time-based event logic, YAML settings, Paper commands/listeners, persistent state and unit tests.

## Engineering Case Studies

- [SellChest Performance](case-studies/sellchest-performance.md): bounded per-tick economy work and chunk-level limits.
- [Discord / Minecraft Platform](case-studies/discord-minecraft-platform.md): module boundaries for a Paper, Discord, database and web platform.
- [Container Optimization](case-studies/container-optimization.md): profiling and throttling container or hopper hotspots.
- [Bounty Abuse Prevention](case-studies/bounty-abuse-prevention.md): pair-based cooldown handling for repeated bounty growth.

## Core Examples

The complete core project remains private. Only small standalone examples are included here:

- Message helper
- Offline player resolver
- Trade session state

## Scope

Duplicate versions, very small experiments, server-specific bundles and full private core projects were left out to keep the repository focused and understandable.
