/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.dialog;

import dev.zellys.nexus.common.translation.TranslationKey;
import java.util.List;

public final class ZDialog {
    private final String id;
    private final TranslationKey title;
    private final List<TranslationKey> body;

    public ZDialog(String id, TranslationKey title, List<TranslationKey> body) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id");
        }
        if (title == null) {
            throw new IllegalArgumentException("title");
        }
        this.id = id;
        this.title = title;
        this.body = body == null ? List.of() : List.copyOf(body);
    }

    public String id() {
        return id;
    }

    public TranslationKey title() {
        return title;
    }

    public List<TranslationKey> body() {
        return body;
    }
}
