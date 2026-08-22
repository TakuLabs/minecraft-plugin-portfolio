# NovaGuard

Paper/Purpur security plugin focused on connection limits, access lists and packet-rate monitoring.

## Features

- IP account limit service
- UUID and IP blacklist/whitelist stores
- ProtocolLib packet listener
- Token-bucket packet rate limiting
- Conservative monitor/enforce configuration mode
- JUnit tests for key services

## Requirements

- Java 21
- Paper 1.21.x
- Maven
- ProtocolLib at runtime

## Building

```bash
mvn clean package
```

## Notes

The included tests use documentation-only IP addresses from `203.0.113.0/24`.
