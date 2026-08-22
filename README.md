# TakuLabs
## Minecraft Plugin Development

Java-based Minecraft plugin development focused on clean server systems, gameplay features and maintainable Paper plugins.

[![Java 21](https://img.shields.io/badge/Java-21-2f3437?style=flat-square&logo=openjdk&logoColor=white)](#technologies)
[![Paper 1.21.x](https://img.shields.io/badge/Paper-1.21.x-2f3437?style=flat-square)](#technologies)
[![Maven](https://img.shields.io/badge/Maven-Build-2f3437?style=flat-square&logo=apachemaven&logoColor=white)](#technologies)
[![Minecraft Plugins](https://img.shields.io/badge/Minecraft-Plugins-2f3437?style=flat-square)](#featured-project)

This repository contains a curated selection of standalone Minecraft Java projects and small core examples. It is intentionally focused: not a full private server archive, but a practical look at how I structure Paper plugins, configuration-heavy systems and common server features.

---

## Featured Project

### [EmpireCrates](plugins/EmpireCrates)

Flagship standalone crate system with admin tooling, configurable rewards, visual opening behavior and persistent crate state.

`Java 21 · Paper · Maven · PlaceholderAPI · YAML`

- YAML-backed crate, key and reward definitions
- Admin GUI flow with chat prompts for editing crate settings
- Item and command rewards with rarity, chance and broadcast controls
- Persistent placed crate and player milestone progress storage
- GUI and block-based opening animations with particles and sounds
- Optional ModelEngine and custom-item integration points

Screenshots are not included yet. Good future captures would be the admin GUI, reward editor, crate preview and opening animation.

---

## Projects

<table>
  <tr>
    <td width="50%" valign="top">
      <h3><a href="plugins/BirthdayEvent">BirthdayEvent</a></h3>
      <p>Small event plugin for a configured birthday event window.</p>
      <ul>
        <li>Timezone-aware scheduling</li>
        <li>Configurable messages and reward command</li>
        <li>Restart-safe state with unit tests</li>
      </ul>
      <p><code>Java 21 · Paper · Maven · JUnit</code></p>
    </td>
    <td width="50%" valign="top">
      <h3><a href="plugins/OtakuAuctionHouse">OtakuAuctionHouse</a></h3>
      <p>Auction house plugin for listing, browsing and buying player items.</p>
      <ul>
        <li>Vault economy integration</li>
        <li>Inventory menus for listings and confirmations</li>
        <li>Serialized item storage and listing limits</li>
      </ul>
      <p><code>Java 21 · Paper · Maven · Vault · YAML</code></p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3><a href="plugins/NovaGuard">NovaGuard</a></h3>
      <p>Server protection plugin focused on connection and packet safeguards.</p>
      <ul>
        <li>IP account limits</li>
        <li>Whitelist and blacklist handling</li>
        <li>ProtocolLib packet-rate monitoring with tests</li>
      </ul>
      <p><code>Java 21 · Paper · Maven · ProtocolLib · JUnit</code></p>
    </td>
    <td width="50%" valign="top">
      <h3><a href="plugins/OtakuScoreboard">OtakuScoreboard</a></h3>
      <p>Configurable scoreboard and tablist plugin.</p>
      <ul>
        <li>YAML-driven scoreboard rendering</li>
        <li>PlaceholderAPI integration</li>
        <li>Tablist, nametag and player toggle services</li>
      </ul>
      <p><code>Java 21 · Paper · Maven · PlaceholderAPI · YAML</code></p>
    </td>
  </tr>
</table>

---

## Development Focus

- Modular plugin structure
- Configuration-driven systems
- Inventory GUI workflows
- Persistent data handling
- Paper/Bukkit event architecture
- External API integrations through soft dependencies where possible

## Technologies

**Core:** `Java 21` · `Paper API` · `Bukkit API` · `Maven` · `YAML`

**Selected integrations:** `Vault` · `PlaceholderAPI` · `ProtocolLib` · `ModelEngine`

## Selected Core Examples

The complete private core is not included. Only small standalone examples are published.

- [Message Helper](core-examples/message-helper) — formatted MiniMessage help output and legacy RGB gradient helpers.
- [Offline Player Resolver](core-examples/offline-player-resolver) — UUID/name lookup helper for Bukkit `OfflinePlayer` resolution.
- [Trade Session State](core-examples/trade-session-state) — small state model for expiring trade requests and ready-state handling.

## Documentation

- [Project Overview](docs/projects.md)
- [Architecture](docs/architecture.md)
- [Project Selection](docs/evaluation.md)

## Repository Structure

```text
minecraft-plugin-portfolio/
├─ plugins/
├─ core-examples/
└─ docs/
```

## About

I focus on Java-based Minecraft server development, building smaller plugins and focused server systems. This repository provides a practical look at how I structure and implement those projects.

No generated jars or private server deployment files are included.
