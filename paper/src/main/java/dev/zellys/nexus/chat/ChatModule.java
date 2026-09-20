/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.chat;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class ChatListener implements Listener {
    private final Set<UUID> shoutEnabled;
    private final Set<UUID> chatHidden;

    ChatListener(Set<UUID> shoutEnabled, Set<UUID> chatHidden) {
        this.shoutEnabled = shoutEnabled;
        this.chatHidden = chatHidden;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        event.viewers().removeIf(audience -> audience instanceof Player && chatHidden.contains(((Player) audience).getUniqueId()));

        if (!shoutEnabled.contains(player.getUniqueId())) {
            event.viewers().removeIf(audience -> {
                if (audience instanceof Player p) {
                    if (p.getWorld().equals(player.getWorld())) {
                        return p.getLocation().distanceSquared(player.getLocation()) > 10000;
                    }
                    return true;
                }
                return false;
            });
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component msg = event.message();

        if (plain.contains("[item]")) {
            Material material = player.getInventory().getItemInMainHand().getType();
            if (!material.isAir()) {
                msg = msg.replaceText(builder -> builder.matchLiteral("[item]").replacement(Component.translatable(material.translationKey())));
            }
        }

        if (plain.contains("[inv]")) {
            msg = msg.replaceText(builder -> builder.matchLiteral("[inv]").replacement(
                    Component.text("[Inventory]").color(NamedTextColor.GOLD)
                            .clickEvent(ClickEvent.runCommand("/zn inv see " + player.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to view inventory")))
            ));
        }

        if (plain.contains("[ender]")) {
            msg = msg.replaceText(builder -> builder.matchLiteral("[ender]").replacement(
                    Component.text("[Ender Chest]").color(NamedTextColor.LIGHT_PURPLE)
                            .clickEvent(ClickEvent.runCommand("/zn inv ender " + player.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to view ender chest")))
            ));
        }

        event.message(msg);
    }
}

final class ChatSubcommand implements BasicCommand {
    private final Translator translator;
    private final Set<UUID> shoutEnabled;
    private final Set<UUID> chatHidden;
    private final dev.zellys.nexus.dialog.DialogManager dialogs;

    ChatSubcommand(Translator translator, Set<UUID> shoutEnabled, Set<UUID> chatHidden) {
        this(translator, shoutEnabled, chatHidden, null);
    }

    ChatSubcommand(Translator translator, Set<UUID> shoutEnabled, Set<UUID> chatHidden, dev.zellys.nexus.dialog.DialogManager dialogs) {
        this.translator = translator;
        this.shoutEnabled = shoutEnabled;
        this.chatHidden = chatHidden;
        this.dialogs = dialogs;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            return;
        }
        if (args.length == 0) {
            player.sendMessage(translator.get(TranslationKey.CHAT_USAGE));
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "menu":
                if (dialogs != null) {
                    dialogs.show(player, new dev.zellys.nexus.common.dialog.ZDialog("chat-menu", TranslationKey.CHAT_MENU, java.util.List.of(TranslationKey.CHAT_USAGE)));
                } else {
                    player.sendMessage(translator.get(TranslationKey.CHAT_MENU));
                }
                break;
            case "shout":
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("on")) {
                        shoutEnabled.add(player.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.CHAT_SHOUT_ON));
                    } else if (args[1].equalsIgnoreCase("off")) {
                        shoutEnabled.remove(player.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.CHAT_SHOUT_OFF));
                    }
                } else {
                    if (shoutEnabled.contains(player.getUniqueId())) {
                        shoutEnabled.remove(player.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.CHAT_SHOUT_OFF));
                    } else {
                        shoutEnabled.add(player.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.CHAT_SHOUT_ON));
                    }
                }
                break;
            case "toggle":
                if (chatHidden.contains(player.getUniqueId())) {
                    chatHidden.remove(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.CHAT_TOGGLE_OFF));
                } else {
                    chatHidden.add(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.CHAT_TOGGLE_ON));
                }
                break;
            default:
                player.sendMessage(translator.get(TranslationKey.CHAT_USAGE));
                break;
        }
    }
}

public final class ChatModule {
    private ChatModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        Set<UUID> shoutEnabled = new HashSet<>();
        Set<UUID> chatHidden = new HashSet<>();
        plugin.getServer().getPluginManager().registerEvents(new ChatListener(shoutEnabled, chatHidden), plugin);
        if (znCommand != null) {
            znCommand.registerCommand("chat", "c", new ChatSubcommand(plugin.translator(), shoutEnabled, chatHidden, plugin.dialogs()));
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
