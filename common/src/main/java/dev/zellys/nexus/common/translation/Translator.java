/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.translation;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class Translator {
    public static final String FALLBACK_LANGUAGE = "en";

    private final String activeLanguage;
    private final Map<String, String> english;
    private final Map<String, String> active;

    public Translator(String language) {
        this.english = load(FALLBACK_LANGUAGE);
        Map<String, String> loaded = FALLBACK_LANGUAGE.equals(language) ? this.english : load(language);
        if (loaded.isEmpty()) {
            loaded = this.english;
            language = FALLBACK_LANGUAGE;
        }
        this.active = loaded;
        this.activeLanguage = language;
    }

    public String get(TranslationKey key) {
        String text = active.get(key.key());
        if (text == null) {
            text = english.get(key.key());
        }
        return text != null ? text : key.key();
    }

    public String get(TranslationKey key, Object... args) {
        return String.format(get(key), args);
    }

    public int completion(String language) {
        if (english.isEmpty()) {
            return 0;
        }
        Map<String, String> map = FALLBACK_LANGUAGE.equals(language) ? english : load(language);
        int hit = 0;
        for (TranslationKey key : TranslationKey.values()) {
            if (map.containsKey(key.key())) {
                hit++;
            }
        }
        return (int) Math.round(hit * 100.0 / TranslationKey.values().length);
    }

    public String activeLanguage() {
        return activeLanguage;
    }

    private static Map<String, String> load(String language) {
        String path = "/lang/translations_" + language + ".yml";
        try (InputStream in = Translator.class.getResourceAsStream(path)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            Map<String, Object> raw = new Yaml().load(in);
            if (raw == null) {
                return Collections.emptyMap();
            }
            Map<String, String> out = new HashMap<>();
            flatten("", raw, out);
            return out;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> node, Map<String, String> out) {
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatten(key, (Map<String, Object>) value, out);
            } else if (value != null) {
                out.put(key, String.valueOf(value));
            }
        }
    }
}
