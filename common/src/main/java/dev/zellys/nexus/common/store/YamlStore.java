/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.store;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class YamlStore {
    private final Path file;
    private final Yaml yaml = new Yaml();

    public YamlStore(Path file) {
        this.file = file;
    }

    public synchronized Map<String, Object> load() {
        if (!Files.isRegularFile(file)) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Object> data = yaml.load(reader);
            return data == null ? new HashMap<>() : new HashMap<>(data);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public synchronized void save(Map<String, Object> data) {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            yaml.dump(new HashMap<>(data), writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
