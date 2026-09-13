# Container Optimization

This case study is based on a separate private plugin for profiling and reducing container or hopper hotspots. It is included as an engineering note because it covers a different problem than shop or sell-chest logic.

## Problem

Large item networks can create uneven load: a few chunks with many active containers or hoppers may dominate the cost while the rest of the server is quiet. The useful signal is not just the total number of containers, but where activity concentrates over time.

## Approach

- Track container activity by chunk key
- Build short time-window snapshots for hotspots
- Classify hotspots before applying limits
- Use adaptive throttling instead of one static global rule
- Keep explicit exclusions for locations that should not be optimized automatically
- Expose inspection commands for operators
- Cover classification and throttling behavior with tests

## Simplified Flow

```text
Container activity
  |
  +-- registry update
  |
  +-- time-window counters
  |
  +-- hotspot classifier
  |
  +-- adaptive throttle decision
  |
  +-- operator inspection / debug output
```

## Public Scope

The public repository does not include the private optimizer source, server configuration or operational thresholds. The case study records the design shape and the reason it is separate from ordinary economy plugin work.
