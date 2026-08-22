# BirthdayEvent

`Java 21 · Paper · Maven · JUnit`

Small Paper plugin for running a configured one-day birthday event.

## Features

- Timezone-aware event window
- Configurable messages, titles and reward command
- Congratulation claim tracking
- Restart-safe event and congratulation storage
- Unit tests for scheduler and reward selection logic

## Requirements

- Java 21
- Paper 1.21.x
- Maven

## Building

```bash
mvn clean package
```

## Architecture

The project keeps date-window logic, reward selection and persistent claim state small enough to test directly. The example configuration uses a neutral placeholder player name.
