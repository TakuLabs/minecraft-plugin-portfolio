# Project Notes

## Selected Plugins

### BirthdayEvent

Purpose: one-time event plugin with scheduling, message configuration and reward claiming.

Shows time-based event logic, YAML settings, Paper commands/listeners, persistent state and unit tests.

### EmpireCrates

Purpose: extracted crate-system module with rewards, admin tooling, visual opening behavior and player progress.

Shows the largest focused showcase in this portfolio. EmpireCrates originated inside a larger private core and has been prepared here to demonstrate YAML-backed crate definitions, item and command rewards, physical key validation, rarity/chance handling, admin GUIs, chat prompts, placed crate storage, player milestone progress, PlaceholderAPI support and optional ModelEngine/custom-item integration points.

### SMPShop

Purpose: server-controlled SMP shop with configurable categories, buy/sell prices and inventory workflows.

Shows a focused economy-shop module extracted from a larger private server system. The public copy keeps the shop GUI, quantity selection, item purchase flow, item selling flow, YAML category definitions and optional Vault, PlayerPoints and PlaceholderAPI integrations.

### OtakuAuctionHouse

Purpose: inventory-based player auction house.

Shows player-to-player economy integration, menu flow, serialized item storage and permission-based listing limits.

### NovaGuard

Purpose: server security plugin for IP limits, access lists and packet monitoring.

Shows service separation, rate limiting, ProtocolLib integration, privacy-aware IP handling and unit tests.

## Shop / Sell Review

Shop and sell candidates from local project archives were reviewed for public suitability. `SMPShop` was selected because the shop-specific subsystem could be isolated cleanly while leaving unrelated private server modules out of the portfolio.

## Core Examples

The complete core project remains private. Only small standalone examples are included here:

- Message helper
- Offline player resolver
- Trade session state

## Rejected Candidate Themes

Several projects were not included because they were duplicate versions, too small to show much structure, too server-specific, bundled unrelated private server features, or were part of a larger private core that should not be published completely.
