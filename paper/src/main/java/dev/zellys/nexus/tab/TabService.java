/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.tab;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.dialog.DialogPalette;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public final class TabService {
    private final ZellysNexus plugin;
    private final Translator translator;
    private final String version;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public TabService(ZellysNexus plugin, Translator translator, String version) {
        this.plugin = plugin;
        this.translator = translator;
        this.version = version;
    }

    public void apply(Player player) {
        player.sendPlayerListHeaderAndFooter(
                Component.text(translator.get(TranslationKey.TAB_HEADER),
                        TextColor.color(DialogPalette.PASTEL_BLUE)),
                Component.text(translator.get(TranslationKey.TAB_FOOTER, version),
                        TextColor.color(DialogPalette.TEXT_GRAY)));
        player.playerListName(Component.text(player.getName(), NamedTextColor.GRAY));
        Team team = boardTeam(player);
        team.color(NamedTextColor.GRAY);
        team.addEntry(player.getName());
        BossBar bar = BossBar.bossBar(
                Component.text(translator.get(TranslationKey.TAB_HEADER),
                        TextColor.color(DialogPalette.PASTEL_BLUE)),
                1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        bars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.hideBossBar(bar);
            bars.remove(player.getUniqueId());
        }, 200L);
    }

    public void remove(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        player.playerListName(null);
        Team team = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam(teamName(player));
        if (team != null) {
            team.unregister();
        }
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void clear() {
        for (Team team : plugin.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team.getName().startsWith("zn")) {
                team.unregister();
            }
        }
        bars.clear();
    }

    private Team boardTeam(Player player) {
        Team team = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam(teamName(player));
        if (team == null) {
            team = plugin.getServer().getScoreboardManager().getMainScoreboard()
                    .registerNewTeam(teamName(player));
        }
        return team;
    }

    private static String teamName(Player player) {
        return "zn" + player.getUniqueId().toString().replace("-", "").substring(0, 8);
    }
}
