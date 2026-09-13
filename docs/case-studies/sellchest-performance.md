# SellChest Performance

This case study summarizes a performance-focused subsystem from a private economy core. The full private core is not included in this portfolio.

## Problem

Sell chests are convenient for players, but a naive implementation can scan too many inventories in the same tick. On active SMP servers this can turn a background economy feature into steady tick pressure.

## Observed Constraint

Local profiling around the private subsystem showed that increasing processed sell chests per tick raised server tick cost quickly:

| Processed chests per tick | Observed tick cost |
| ---: | ---: |
| 4 | about 26 MSPT |
| 10 | about 39 MSPT |
| 13 | about 43 MSPT |

The useful fix was not to make every scan more complicated. The safer change was to bound the amount of work allowed to run in one tick and keep chunk-level limits explicit.

## Approach

- Cap sell-chest processing per tick
- Track per-chunk chest counts to avoid dense hotspots
- Keep selling logic in a manager service instead of event handlers
- Prefer predictable work slices over large one-tick batches
- Test manager behavior separately from the Paper runtime

## Simplified Flow

```text
Scheduled task
  |
  +-- take next bounded work slice
  |
  +-- check chunk limit
  |
  +-- inspect eligible container
  |
  +-- sell matching items through economy service
  |
  +-- defer remaining containers to later ticks
```

## Public Scope

The portfolio includes the architectural notes and measured tradeoff, not the full private economy implementation or server-specific configuration.
