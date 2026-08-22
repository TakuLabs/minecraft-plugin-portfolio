# TakuLabs - Minecraft Plugin Development

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](#technologies)
[![Paper API](https://img.shields.io/badge/Paper%20API-1.21.x-blue.svg)](#technologies)
[![Maven](https://img.shields.io/badge/Build-Maven-red.svg)](#technologies)

This repository contains a curated selection of Minecraft Java projects and standalone development examples.
It shows how I structure plugins, work with the Paper/Bukkit API, handle YAML configuration and implement common server features.

The portfolio is intentionally focused. It is not a full archive of every private server system or experiment.

## Technologies

- Java 21
- Paper API
- Bukkit API
- Maven
- YAML configuration
- Vault API in selected projects
- PlaceholderAPI in selected projects
- ProtocolLib in NovaGuard

## Featured Projects

### [BirthdayEvent](plugins/BirthdayEvent)

Small event plugin for a configured birthday event window.

- Timezone-aware scheduling
- Configurable messages and reward command
- Restart-safe event and congratulation state
- Unit tests for event and reward logic

### [EmpireCrates](plugins/EmpireCrates) - Featured Showcase

Larger standalone crate system with admin tooling, configurable rewards, visual opening behavior and persistent crate state.

- YAML-backed crate, key and reward definitions
- Admin GUI flow with chat prompts for editing crate settings
- Item and command rewards with rarity, chance and broadcast controls
- Persistent placed crate and player milestone progress storage
- GUI and block-based opening animations with particles and sounds
- Optional PlaceholderAPI, ModelEngine and custom-item integration points

### [OtakuAuctionHouse](plugins/OtakuAuctionHouse)

Auction house plugin for listing, browsing and buying player items.

- Vault economy integration
- Inventory menus for listing, filtering and confirmations
- Item serialization for persisted listings
- Permission-based listing limits

### [NovaGuard](plugins/NovaGuard)

Server protection plugin focused on connection and packet safeguards.

- IP account limits
- UUID/IP whitelist and blacklist handling
- ProtocolLib packet monitoring
- Token-bucket rate limiting with tests

### [OtakuScoreboard](plugins/OtakuScoreboard)

Configurable scoreboard and tablist plugin.

- YAML-driven scoreboard rendering
- PlaceholderAPI integration
- Tablist and nametag services
- Player toggle and reload commands

## Selected Core Examples

The complete core project is private. This repository only contains selected standalone examples intended to demonstrate code structure, architecture and development practices.

- [Message helper](core-examples/message-helper)
- [Offline player resolver](core-examples/offline-player-resolver)
- [Trade session state](core-examples/trade-session-state)

## Documentation

- [Project notes](docs/projects.md)
- [Architecture notes](docs/architecture.md)
- [Project selection](docs/evaluation.md)

## About

I focus on Java-based Minecraft server development, primarily building smaller plugins and focused gameplay or server systems. This portfolio provides a practical overview of how I structure code, work with the Paper API and implement common server features.

No generated jars or private server deployment files are included.
