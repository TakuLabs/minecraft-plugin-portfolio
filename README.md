# TakuLabs - Minecraft Plugin Development

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](#technologies)
[![Paper API](https://img.shields.io/badge/Paper%20API-1.21.x-blue.svg)](#technologies)
[![Maven](https://img.shields.io/badge/Build-Maven-red.svg)](#technologies)

This repository contains a curated selection of my Minecraft Java projects and standalone development examples.
It provides an overview of how I structure plugins, implement gameplay features, work with configuration files and use the Paper/Bukkit API.

The repository is intended as an honest portfolio for Minecraft server development applications. It is not a full archive of every project or every private server system I have worked on.

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

- Timezone-aware event scheduling
- Configurable event state and messages
- Congratulation reward tracking
- JUnit tests for event and reward logic

### [EmpireCrates](plugins/EmpireCrates)

Crate system with configurable rewards, GUI management and visual opening flow.

- Configurable crates, rewards and rarities
- Inventory GUI flow with chat prompts
- Persistent crate and player progress storage
- PlaceholderAPI support

### [OtakuAuctionHouse](plugins/OtakuAuctionHouse)

Auction house plugin for listing, browsing and buying player items.

- Vault economy integration
- Multiple inventory menus for listing, filtering and confirmations
- Item serialization for persisted listings
- Permission-based active listing limits

### [NovaGuard](plugins/NovaGuard)

Server protection plugin focused on connection and packet safeguards.

- IP account limits
- UUID/IP whitelist and blacklist stores
- ProtocolLib packet monitoring and rate limiting
- Unit tests for core security logic

### [OtakuScoreboard](plugins/OtakuScoreboard)

Configurable scoreboard and tablist plugin.

- Scoreboard rendering from YAML configuration
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
- [Evaluation notes](docs/evaluation.md)

## About

TakuLabs is my GitHub developer name. I work on Java-based Minecraft server features, mostly smaller plugins and focused server systems. This portfolio gives server teams a practical look at how I structure code, handle commands and events, work with YAML configuration and build Paper plugins.

No generated jars or private server deployment files are included.
