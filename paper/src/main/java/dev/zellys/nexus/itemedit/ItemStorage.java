/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.itemedit;

import dev.zellys.nexus.common.store.YamlStore;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

public final class ItemStorage {
    private final YamlStore store;

    public ItemStorage(Path pluginDir) {
        this.store = new YamlStore(pluginDir.resolve("items.yml"));
    }

    public void save(String key, ItemStack item) {
        Map<String, Object> data = store.load();
        byte[] bytes = item.serializeAsBytes();
        data.put(key, Base64.getEncoder().encodeToString(bytes));
        store.save(data);
    }

    public ItemStack get(String key) {
        Map<String, Object> data = store.load();
        Object val = data.get(key);
        if (val instanceof String str) {
            try {
                return ItemStack.deserializeBytes(Base64.getDecoder().decode(str));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public boolean delete(String key) {
        Map<String, Object> data = store.load();
        if (data.containsKey(key)) {
            data.remove(key);
            store.save(data);
            return true;
        }
        return false;
    }

    public Set<String> list() {
        return store.load().keySet();
    }
}
