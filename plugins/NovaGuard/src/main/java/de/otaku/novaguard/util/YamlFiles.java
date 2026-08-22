package de.otaku.novaguard.util;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YamlFiles {
    private YamlFiles() {
    }

    public static YamlConfiguration load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    public static void save(YamlConfiguration yaml, Path path) throws IOException {
        Files.writeString(path, yaml.saveToString(), StandardCharsets.UTF_8);
    }
}
