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

import dev.zellys.nexus.common.dialog.ZDialog;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import org.bukkit.entity.Player;

public final class DialogManager {
    private final Translator translator;
    private boolean useDialogs = true;

    public DialogManager(Translator translator) {
        this.translator = translator;
    }

    public boolean useDialogs() {
        return useDialogs;
    }

    public void useDialogs(boolean useDialogs) {
        this.useDialogs = useDialogs;
    }

    public void show(Player player, ZDialog dialog) {
        if (useDialogs) {
            player.showDialog(PaperDialogs.notice(translator, dialog));
            return;
        }
        player.sendMessage(translator.get(TranslationKey.PREFIX) + " " + translator.get(dialog.title()));
        for (TranslationKey key : dialog.body()) {
            player.sendMessage(translator.get(key));
        }
    }
}
