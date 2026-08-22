# Architecture Notes

## Common Patterns

- Maven project layout with `src/main/java` and `src/main/resources`
- `plugin.yml` metadata for commands, permissions and soft dependencies
- Java 21 compilation
- YAML configuration for user-facing messages and feature settings
- Separate packages for commands, listeners, models, storage, services and utilities where the plugin size justifies it

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

## Core Examples

The core examples are intentionally incomplete as applications. They are included to show focused implementation patterns without exposing a complete private server core.

## Build Artifacts

Generated jars, `target/`, IDE files and logs are excluded from the repository.
