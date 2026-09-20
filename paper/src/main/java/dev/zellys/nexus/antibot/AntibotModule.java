/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.antibot;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.store.YamlStore;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.Nullable;

final class AntibotListener implements Listener {
    private final Translator translator;
    private final AntibotService service;

    AntibotListener(Translator translator, AntibotService service) {
        this.translator = translator;
        this.service = service;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress() != null ? event.getAddress().getHostAddress() : "";
        switch (service.check(event.getName(), ip)) {
            case PANIC -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text(translator.get(TranslationKey.ANTIBOT_KICKED_PANIC)));
            case BAD_NAME -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text(translator.get(TranslationKey.ANTIBOT_KICKED_NAME)));
            case THROTTLED -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text(translator.get(TranslationKey.ANTIBOT_KICKED_THROTTLE)));
            case BLACKLISTED -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text(translator.get(TranslationKey.ANTIBOT_KICKED_BLACKLIST)));
            default -> {
            }
        }
    }
}

final class AntibotCommand implements BasicCommand {
    private final Translator translator;
    private final AntibotService service;

    AntibotCommand(Translator translator, AntibotService service) {
        this.translator = translator;
        this.service = service;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_USAGE));
            return;
        }
        if (args[0].equalsIgnoreCase("list")) {
            if (service.blocked().isEmpty()) {
                source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_LIST_EMPTY));
                return;
            }
            for (String ip : service.blocked()) {
                source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_LIST_ENTRY, ip));
            }
            return;
        }
        if (args[0].equalsIgnoreCase("status")) {
            source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_STATUS,
                    service.connectionRate(), service.blockedCount(), service.isPanic()));
            return;
        }
        if (args[0].equalsIgnoreCase("panic")) {
            if (args.length == 2) {
                boolean on = args[1].equalsIgnoreCase("on");
                service.setPanic(on);
                source.getSender().sendMessage(translator.get(on ? TranslationKey.ANTIBOT_PANIC_ON : TranslationKey.ANTIBOT_PANIC_OFF));
            } else {
                source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_USAGE));
            }
            return;
        }
        if (args.length != 2) {
            source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_USAGE));
            return;
        }
        if (args[0].equalsIgnoreCase("add")) {
            source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_ADDED, args[1]));
            service.block(args[1]);
        } else if (args[0].equalsIgnoreCase("remove")) {
            source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_REMOVED, args[1]));
            service.unblock(args[1]);
        } else {
            source.getSender().sendMessage(translator.get(TranslationKey.ANTIBOT_USAGE));
        }
    }

    @Override
    public @Nullable String permission() {
        return "zn.antibot";
    }
}

public final class AntibotModule {
    private AntibotModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        AntibotService service = new AntibotService(
                new YamlStore(plugin.getDataFolder().toPath().resolve("antibot.yml")));
        plugin.getServer().getPluginManager().registerEvents(new AntibotListener(plugin.translator(), service), plugin);
        AntibotCommand cmd = new AntibotCommand(plugin.translator(), service);
        plugin.registerCommand("zantibot", cmd);
        if (znCommand != null) {
            znCommand.registerCommand("antibot", "ab", cmd);
        }
        plugin.permission("zn.antibot", org.bukkit.permissions.PermissionDefault.OP);
        return (System.nanoTime() - start) / 1_000_000;
    }
}
