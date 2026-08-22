# Project Selection

This portfolio contains a deliberately small selection of projects chosen to demonstrate different areas of Minecraft plugin development.

Included projects were selected based on:

- project completeness
- code structure
- technical variety
- independence from private server infrastructure
- suitability as public, focused examples

## Included Projects

- `BirthdayEvent`: demonstrates scheduled event logic, configuration loading, state persistence and unit-tested reward selection.
- `EmpireCrates`: serves as the flagship showcase for an extracted private-core module with crate definitions, physical key validation, reward handling, inventory GUIs, chat prompts, visual opening behavior, storage classes and optional integrations.
- `SMPShop`: demonstrates a server-controlled economy shop with inventory categories, buy/sell pricing, quantity selection, YAML configuration and optional economy integrations.
- `OtakuAuctionHouse`: demonstrates menu-driven item listings, Vault economy integration and serialized item storage.
- `NovaGuard`: shows service-oriented protection logic with IP account limits, access lists, ProtocolLib packet monitoring and tests.

## Shop / Sell Candidates

Shop and sell plugin variants from local archives were reviewed for possible inclusion. The selected shop module had meaningful shop/economy functionality and could be isolated from unrelated report handling, external bot tooling, AntiDupe inspection code and private branding.

## Scope

Duplicate versions, very small experiments, server-specific bundles and full private core projects were left out to keep the repository focused and understandable.

Build artifacts are excluded from the repository.
