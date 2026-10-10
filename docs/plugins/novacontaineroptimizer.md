# NovaContainerOptimizer

Container/hopper profiling and throttling.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.takulabs.novacontaineroptimizer.NovaContainerOptimizer` |
| Source files | 70 |
| Tests | 19 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Core / Infrastructure
- Focus: Container/hopper profiling and throttling
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/takulabs/novacontaineroptimizer/config/PluginConfig.java

Configuration handling from the plugin source.

```java
public record PluginConfig(
        boolean profilerEnabled,
        int activityWindowSeconds,
        ClassificationConfig classificationConfig,
        boolean optimizationEnabled,
        int optimizationMonitoringSeconds,
        int optimizationCooldownSeconds,
        boolean optimizationLimitNewHoppers,
        boolean limitsEnabled,
        int hopperLimitPerChunk,
        boolean limitWarningsEnabled,
        int limitWarningCooldownSeconds,
        boolean reconcileEnabled,
        int reconcileIntervalTicks,
        int reconcileChunksPerRun,
        int reconcileMaxTimeMicros,
        boolean debugVisualizationEnabled,
        int debugDurationSeconds,
        int debugMaxMarkers,
        boolean alertsEnabled,
        int alertCooldownSeconds,
        boolean debugEnabled,
        boolean adaptiveOptimizationEnabled,
        int adaptiveEvaluationIntervalTicks,
        ServerLoadThresholds serverLoadThresholds,
        AdaptiveOptimizationConfig adaptiveOptimizationConfig,
        ThrottleSettings throttleSettings,
        boolean adaptiveRequireTransferHotspot,
        boolean adaptiveSkipExcludedChunks,
        boolean measurementsEnabled,
        int measurementsAfterDelaySeconds
) {

    public static PluginConfig load(FileConfiguration yaml, Logger logger) {
        return new PluginConfig(
                yaml.getBoolean("profiler.enabled", true),
                positive(yaml.getInt("profiler.activity-window-seconds", 300), 1),
                new ClassificationConfig(
                        positive(yaml.getInt("classification.hopper-dense-threshold", 80), 1),
                        positive(yaml.getInt("classification.container-dense-threshold", 180), 1),
                        positive(yaml.getLong("classification.transfer-heavy-per-minute", 3_000L), 1L),
                        positive(yaml.getInt("classification.extreme-risk-score", 90), 1)
                ),
                yaml.getBoolean("optimization.enabled", true),
                positive(yaml.getInt("optimization.monitoring-seconds", 60), 1),
                positive(yaml.getInt("optimization.cooldown-seconds", 300), 1),
```

### 2. src/main/java/de/takulabs/novacontaineroptimizer/limit/HopperLimitService.java

Service-level logic selected from the plugin source.

```java
    public HopperLimitService(boolean enabled, int perChunkLimit) {
        this.enabled = enabled;
        this.perChunkLimit = Math.max(1, perChunkLimit);
    }
```

### 3. src/main/java/de/takulabs/novacontaineroptimizer/service/ReconcileService.java

Service-level logic selected from the plugin source.

```java
    public ReconcileService(Plugin plugin, StatisticsService statisticsService, PluginConfig config) {
        this.plugin = plugin;
        this.statisticsService = statisticsService;
        this.config = config;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
