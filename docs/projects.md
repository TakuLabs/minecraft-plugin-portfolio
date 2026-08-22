# Project Notes

## Selected Plugins

### BirthdayEvent

Purpose: one-time event plugin with scheduling, message configuration and reward claiming.

Shows time-based event logic, YAML settings, Paper commands/listeners, persistent state and unit tests.

### EmpireCrates

Purpose: configurable crate system with rewards, admin tooling, visual opening behavior and player progress.

Shows the largest standalone showcase in this portfolio: YAML-backed crate definitions, item and command rewards, rarity/chance handling, admin GUIs, chat prompts, placed crate storage, player milestone progress, PlaceholderAPI support and optional ModelEngine/custom-item integration points.

### OtakuAuctionHouse

Purpose: inventory-based player auction house.

Shows economy integration, menu flow, serialized item storage and permission-based limits.

### NovaGuard

Purpose: server security plugin for IP limits, access lists and packet monitoring.

Shows service separation, rate limiting, ProtocolLib integration, privacy-aware IP handling and unit tests.

### OtakuScoreboard

Purpose: configurable scoreboard and tablist plugin.

Shows repeated UI updates, PlaceholderAPI usage, player toggles and configuration-driven rendering.

## Shop / Sell Review

Additional shop and sell candidates were reviewed from older local project archives. The strongest variants had real shop functionality, economy hooks and inventory menus, but also bundled server-specific report, Discord, AntiDupe and private-branding concerns.

No shop project was added in this pass. `OtakuAuctionHouse` already covers a strong economy/listing use case, while `OtakuScoreboard` keeps the selected project set more technically varied.

## Core Examples

The complete core project remains private. Only small standalone examples are included here:

- Message helper
- Offline player resolver
- Trade session state

## Rejected Candidate Themes

Several projects were not included because they were duplicate versions, too small to show much structure, too server-specific, bundled unrelated private server features, or were part of a larger private core that should not be published completely.
