# Architecture Notes

## Common Patterns

- Maven project layout with `src/main/java` and `src/main/resources`
- `plugin.yml` metadata for commands, permissions and soft dependencies
- Java 21 compilation
- YAML configuration for user-facing messages and feature settings
- Separate packages for commands, listeners, models, storage, services and utilities where the plugin size justifies it
- Optional integration code is kept behind soft dependencies or reflection in projects that can run without the extra plugin

## Plugin Structure

The selected plugins generally follow this shape:

```text
src/main/java/<package>/
  command/
  listener/
  model/
  storage/
  util/

src/main/resources/
  plugin.yml
  config.yml
```

Not every plugin has every package. Smaller plugins keep fewer classes; larger plugins split command, GUI, storage and model logic more clearly.

`EmpireCrates` is the broadest example in the repository. It originated as a module in a larger private core and is included here as a focused Crates showcase, combining command handling, GUI state, chat prompt editing, YAML persistence, reward models, physical key validation, optional PlaceholderAPI/ModelEngine integration and runtime visual behavior.

`SMPShop` is a focused shop/economy showcase extracted from a larger private server system. Its public copy centers on inventory categories, buy/sell logic, quantity selection, YAML-backed item definitions and optional economy bridges while leaving unrelated private server modules out.

## Core Examples

The core examples and the extracted EmpireCrates showcase are intentionally limited to public-safe code. They are included to show focused implementation patterns without exposing a complete private server core.

## Build Artifacts

Generated jars, `target/`, IDE files and logs are excluded from the repository.
