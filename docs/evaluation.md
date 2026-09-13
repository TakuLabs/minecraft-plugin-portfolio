# Project Selection

This portfolio contains a deliberately small selection of projects chosen to demonstrate different areas of Minecraft plugin development.

Included projects were selected based on:

- project completeness
- code structure
- technical variety
- independence from private server infrastructure
- suitability as public, focused examples

## Included Projects

- `EmpireCrates`: flagship source showcase for crate definitions, physical key validation, reward handling, inventory GUIs, chat prompts, visual opening behavior, storage classes and optional integrations.
- `MythicSpawner`: documentation-only showcase for custom spawner behavior, persistent item/block state, GUI pagination and compatibility decisions.
- `SMPShop`: economy-shop source showcase with inventory categories, buy/sell pricing, quantity selection, YAML configuration and optional economy integrations.
- `OtakuAuctionHouse`: menu-driven item listings, Vault economy integration and serialized item storage.
- `NovaGuard`: service-oriented protection logic with IP account limits, access lists, ProtocolLib packet monitoring and tests.
- `BirthdayEvent`: scheduled event logic, configuration loading, state persistence and unit-tested reward selection.

## Included Case Studies

- `SellChest Performance`: selected because it shows a practical tick-budget tradeoff in economy processing.
- `Discord / Minecraft Platform`: selected because it shows module boundaries across Paper, Discord, persistence and web layers.
- `Container Optimization`: selected because it demonstrates profiling and adaptive throttling around a different performance problem.
- `Bounty Abuse Prevention`: selected because it shows a small, testable rule that limits repeated reward growth without changing unrelated kill handling.

## Scope

Duplicate versions, very small experiments, server-specific bundles and full private core projects were left out to keep the repository focused and understandable.

Build artifacts are excluded from the repository.
