# OtakuAuctionHouse

Paper auction house plugin for listing and buying player items.

## Features

- `/ah` command entry point
- Inventory menus for browsing, filtering, listing and cancellation confirmation
- Vault economy integration
- YAML-backed listing and notification storage
- Permission-based listing limits

## Requirements

- Java 21
- Paper 1.21.x
- Maven
- Vault-compatible economy plugin at runtime

## Building

```bash
mvn clean package
```

## Notes

This project is useful as a portfolio sample because it shows GUI flow, economy integration, model classes and persistent item serialization.
