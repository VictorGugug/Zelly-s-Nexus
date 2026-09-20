/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.integration;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class IntegrationsModule {
    private IntegrationsModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        plugin.permission("zn.admin.integrations", org.bukkit.permissions.PermissionDefault.OP);
        if (znCommand != null) {
            znCommand.registerCommand("integrations", "int", "zn.admin.integrations", null, new IntegrationsSubcommand(plugin));
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}

final class IntegrationsSubcommand implements BasicCommand {
    private final ZellysNexus plugin;
    private final Translator translator;

    IntegrationsSubcommand(ZellysNexus plugin) {
        this.plugin = plugin;
        this.translator = plugin.translator();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("zn.admin.integrations")) {
            return;
        }
        if (args.length == 0) {
            player.sendMessage(translator.get(TranslationKey.INT_USAGE));
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "perms":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.INT_USAGE));
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    return;
                }
                try {
                    Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
                    RegisteredServiceProvider<?> provider = plugin.getServer().getServicesManager().getRegistration(lpClass);
                    if (provider != null) {
                        Object api = provider.getProvider();
                        Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
                        Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, target.getUniqueId());
                        if (user != null) {
                            String group = (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
                            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                            String prefix = (String) metaData.getClass().getMethod("getPrefix").invoke(metaData);
                            player.sendMessage(translator.get(TranslationKey.INT_PERMS_INFO, target.getName(), group, prefix != null ? prefix : "", ""));
                        } else {
                            player.sendMessage(translator.get(TranslationKey.INT_PERMS_UNAVAILABLE));
                        }
                    } else {
                        player.sendMessage(translator.get(TranslationKey.INT_PERMS_UNAVAILABLE));
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    player.sendMessage(translator.get(TranslationKey.INT_PERMS_UNAVAILABLE));
                }
                break;
            case "bedrock":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.INT_USAGE));
                    return;
                }
                Player targetBedrock = plugin.getServer().getPlayer(args[1]);
                if (targetBedrock == null) {
                    return;
                }
                try {
                    Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                    Object api = floodgateApiClass.getMethod("getInstance").invoke(null);
                    boolean isBedrock = (boolean) api.getClass().getMethod("isFloodgatePlayer", java.util.UUID.class).invoke(api, targetBedrock.getUniqueId());
                    if (isBedrock) {
                        player.sendMessage(translator.get(TranslationKey.INT_BEDROCK_YES, targetBedrock.getName()));
                    } else {
                        player.sendMessage(translator.get(TranslationKey.INT_BEDROCK_NO, targetBedrock.getName()));
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    player.sendMessage(translator.get(TranslationKey.INT_BEDROCK_UNAVAILABLE));
                }
                break;
            default:
                player.sendMessage(translator.get(TranslationKey.INT_USAGE));
                break;
        }
    }
}
