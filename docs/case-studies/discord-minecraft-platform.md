# Discord / Minecraft Platform

This case study describes a private multi-module platform that connects Minecraft, Discord, persistence and web tooling. The source and deployment configuration are not published.

## Overview

The platform is structured as a Maven reactor with separate modules for shared domain logic, database access, Discord adapters, a web backend and a Paper plugin bootstrap.

```text
discord-bot      web-backend      minecraft-plugin
     |                |                  |
     +----------------+------------------+
                      |
                 common services
                      |
                  database layer
```

## Module Boundaries

| Module | Responsibility |
| --- | --- |
| `common` | Domain services, state transitions, reconciliation logic and shared contracts |
| `database` | JDBC repositories and database bootstrap |
| `discord-bot` | JDA command and listener adapters |
| `web-backend` | OAuth2 login flow, session handling and dashboard API foundation |
| `minecraft-plugin` | Paper lifecycle bootstrap, Bukkit commands and server integration |
| `web-frontend` | Dashboard frontend source and static build flow |

## Technical Notes

- Paper plugin starts the platform inside the Minecraft server process
- Discord and Minecraft identities are connected through an account-linking flow
- Database access is isolated behind repository interfaces
- Staff tools, moderation, tickets and rank synchronization share service-layer logic
- Web authentication uses short-lived browser flow state and secure session cookies
- Reconciliation jobs repair cross-platform state instead of relying only on live events

## Public Scope

The portfolio keeps this as an architecture note because publishing the complete platform would expose unrelated private server infrastructure. No runtime configuration, credentials or private deployment details are included.
