# MythicSpawner

`Java 21` · `Paper 1.21.x` · `Maven` · `PersistentDataContainer` · `JUnit`

MythicSpawner documents a custom spawner system without publishing the private source or server configuration. The page keeps the useful parts visible: feature boundaries, event decisions and the GUI flow.

## Overview

The system centers on a custom spawner pickaxe and converted spawner items. It handles break/place behavior, item tagging, conversion outcomes and a paginated GUI for selecting mob types.

## Key Features

- Custom spawner pickaxe with configurable drop behavior
- Converted spawner items that preserve selected entity type
- Block and item state stored through Bukkit `PersistentDataContainer`
- Paginated mob selection menu with fixed page size
- Reload path for config changes and recipe updates
- Compatibility checks for virtual spawner plugins
- Focused tests for pagination and break-decision behavior

## Technical Structure

```text
command/
  Admin and player-facing spawner commands
gui/
  Mob selection and pagination
listener/
  Break and place event handling
service/
  Conversion, config and recipe services
item/
  Spawner pickaxe and converted spawner factories
```

## Break Flow

```text
BlockBreakEvent
  |
  +-- virtual spawner marker? -> ignore
  |
  +-- converted spawner?
  |     |
  |     +-- correct tool -> drop converted item
  |     +-- other tool   -> block unsafe break
  |
  +-- normal spawner with custom pickaxe -> roll configured drop chance
```

## Public Scope

Private gameplay configuration, economy values and deployment files stay out of the repository. The useful review points are the event decisions, persistent item state, GUI pagination and compatibility checks.
