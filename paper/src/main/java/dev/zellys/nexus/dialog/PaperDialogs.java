/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.dialog;

import dev.zellys.nexus.common.dialog.DialogPalette;
import dev.zellys.nexus.common.dialog.ZDialog;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class PaperDialogs {
    private PaperDialogs() {
    }

    public static Dialog notice(Translator translator, ZDialog dialog) {
        List<DialogBody> body = new ArrayList<>();
        for (TranslationKey key : dialog.body()) {
            body.add(DialogBody.plainMessage(
                    Component.text(translator.get(key), TextColor.color(DialogPalette.TEXT_GRAY))));
        }
        DialogBase base = DialogBase.builder(
                Component.text(translator.get(dialog.title()), TextColor.color(DialogPalette.PASTEL_BLUE)))
                .body(body)
                .build();
        return Dialog.create(factory -> factory.empty().base(base).type(DialogType.notice()));
    }
}
