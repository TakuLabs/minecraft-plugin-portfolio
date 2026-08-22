# SMPShop

`Java 21 · Paper · Maven · Vault · PlayerPoints · PlaceholderAPI · YAML`

SMPShop is an extracted shop/economy module originally developed as part of a larger private server system and prepared here as a focused portfolio showcase.

## Overview

This project provides a server-controlled economy shop for SMP-style servers. Players browse configured item categories in inventory GUIs, buy items through a quantity selector and sell configured items back to the server economy.

The showcase keeps the shop-relevant code and removes unrelated private server suite features such as report handling, external bot tooling, auction-house code and AntiDupe inspection commands.

## Features

### Shop Navigation

- Main inventory menu with configured category buttons
- Category menus with configurable size and pagination
- Back, close, previous and next navigation buttons
- YAML-defined category names, icons, descriptions and item lists

### Buying

- Left-click item buying flow
- Quantity selector with `-64`, `-32`, `-1`, `+1`, `+32` and `+64` controls
- Configurable maximum buy quantity
- Inventory-space validation before taking currency
- Item rewards or configured command rewards per item

### Selling

- Right-click sell flow for configured shop items
- Per-item sell price support
- Disabled selling through negative prices
- Item availability checks before payout
- Refund path if currency payout fails after item removal

### Economy

- Vault economy support through a soft dependency and reflection
- PlayerPoints support through a soft dependency and reflection
- Per-category or per-item currency selection
- Configurable money and points display formats

### Configuration

- YAML-backed categories and item definitions
- Buy prices, sell prices, item amounts and display names
- Optional item commands
- Potion type, firework power and enchantment support
- Configurable user-facing messages and GUI text

### Validation

- Invalid materials are skipped when loading item definitions
- Buy flow checks currency availability and inventory capacity first
- Sell flow checks matching inventory items before payout
- Inventory menu clicks and drags are cancelled for controlled shop views

## Technical Structure

- `SMPShopPlugin`: plugin bootstrap, command registration, listener registration and component reloads
- `ShopCommand`: `/shop`, `/shop reload` and `/shop open <player>` handling
- `ShopManager`: category loading, GUI construction, pagination and quantity menu setup
- `ShopListener`: menu click handling, buy/sell execution and inventory validation
- `ShopItem` / `ShopCategory`: configuration-backed item and category models
- `EconomyHook`, `PlayerPointsHook`, `PlaceholderHook`: optional integration bridges
- `Text`: legacy color and PlaceholderAPI-aware text rendering

## Requirements

- Java 21
- Paper 1.21.x
- Maven
- Vault optional for money-based purchases and sales
- PlayerPoints optional for points-based shop items
- PlaceholderAPI optional for configured text placeholders

## Building

```bash
mvn clean package
```

The generated jar is intentionally not tracked in this portfolio repository.

## Why Included

SMPShop adds a server-controlled commerce example to the portfolio. It complements `OtakuAuctionHouse`, which focuses on player-to-player listings, by showing configurable server shop categories, buy/sell pricing, quantity selection and optional economy integrations.
