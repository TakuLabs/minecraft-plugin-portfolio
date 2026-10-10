# EmpireCrates

Crate rewards, keys and GUI management.

| Field | Value |
| --- | --- |
| Version | `1.14.5` |
| Build | Maven |
| Main class | `de.otaku.empirecrates.EmpireCratesPlugin` |
| Source files | 26 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Economy
- Focus: Crate rewards, keys and GUI management
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. work/ExcellentCratesConverter.java

Representative source excerpt.

```java
    public static void main(String[] args) throws Exception {
        File source = new File(args[0]);
        File output = new File(args[1]);
        File cratesFolder = new File(source, "crates");
        File keysFolder = new File(source, "keys");
        Map<String, Map<String, Object>> keyItems = keyItems(keysFolder);
        YamlConfiguration target = new YamlConfiguration();
        File[] crateFiles = cratesFolder.listFiles(file -> file.isFile() && file.getName().endsWith(".yml"));
        if (crateFiles == null) {
            throw new IllegalStateException("No crate yml files found in " + cratesFolder);
        }
        List.of(crateFiles).stream()
                .sorted(Comparator.comparing(File::getName))
                .forEach(file -> convertCrate(file, target, keyItems));
        output.getParentFile().mkdirs();
        target.save(output);
        System.out.println("Converted " + crateFiles.length + " crate files to " + output.getAbsolutePath());
    }
```

### 2. src/main/java/de/otaku/empirecrates/util/Text.java

Representative source excerpt.

```java
    public static Component mm(String input) {
        if (input == null) {
            return Component.empty();
        }
        input = legacyToMini(input);
        if (input.contains("<") && input.contains(">")) {
            try {
                return MINI.deserialize(input);
            } catch (RuntimeException ignored) {
                return Component.text(stripMiniTags(input));
            }
        }
        return LEGACY.deserialize(input);
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
