# Bounty Abuse Prevention

This case study describes a focused anti-farming guard from a private bounty system. The implementation is not published here.

## Problem

If a bounty grows every time the same killer defeats the same target, two players can repeatedly trigger the same reward path. A useful guard has to block that repeated growth without changing unrelated kill handling.

## Approach

- Track the last counted kill-growth time per killer and victim pair
- Apply the cooldown only to the bounty-growth branch
- Persist cooldown state so restarts do not reset the guard
- Leave the original bounty amount unchanged when growth is denied
- Use an injectable time source for deterministic tests

## Simplified Decision

```text
Player kill
  |
  +-- active bounty exists?
        |
        +-- same killer/victim pair still cooling down -> no growth
        |
        +-- cooldown elapsed -> apply configured growth and record timestamp
```

## Public Scope

The public note avoids private server values and keeps the focus on the decision boundary: repeated pair detection, persistence and testable time handling.
