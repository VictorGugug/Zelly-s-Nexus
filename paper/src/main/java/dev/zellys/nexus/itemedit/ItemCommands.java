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

import dev.zellys.nexus.common.dialog.DialogPalette;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import dev.zellys.nexus.thread.ThreadRouter;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.Nullable;

import java.util.*;

final class ItemEditSubcommand implements BasicCommand {
    private final Translator translator;
    private final ItemStorage storage;
    private final dev.zellys.nexus.dialog.DialogManager dialogs;

    ItemEditSubcommand(Translator translator, ItemStorage storage, dev.zellys.nexus.dialog.DialogManager dialogs) {
        this.translator = translator;
        this.storage = storage;
        this.dialogs = dialogs;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        
        if (sub.equals("menu")) {
            if (dialogs != null) {
                dialogs.show(player, new dev.zellys.nexus.common.dialog.ZDialog("itemedit-menu", TranslationKey.ITEMEDIT_USAGE, java.util.List.of(TranslationKey.ITEMEDIT_INFO)));
            } else {
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
            }
            return;
        }

        if (sub.equals("import")) {
            if (args.length != 2) {
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                return;
            }
            try {
                byte[] bytes = Base64.getDecoder().decode(args[1]);
                ItemStack imported = ItemStack.deserializeBytes(bytes);
                player.getInventory().addItem(imported);
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_IMPORTED));
            } catch (Exception e) {
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_IMPORT_FAIL));
            }
            return;
        }

        if (sub.equals("storage")) {
            if (args.length < 2) {
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                return;
            }
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("list")) {
                Set<String> keys = storage.list();
                if (keys.isEmpty()) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_EMPTY));
                } else {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_LIST, String.join(", ", keys)));
                }
                return;
            }
            if (args.length < 3) {
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                return;
            }
            String key = args[2];
            switch (action) {
                case "save":
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_NO_ITEM));
                        return;
                    }
                    storage.save(key, hand);
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_SAVED, key));
                    break;
                case "get":
                    ItemStack retrieved = storage.get(key);
                    if (retrieved == null) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_NOT_FOUND, key));
                    } else {
                        player.getInventory().addItem(retrieved);
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_LOADED, key));
                    }
                    break;
                case "delete":
                    if (storage.delete(key)) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_DELETED, key));
                    } else {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_NOT_FOUND, key));
                    }
                    break;
                default:
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    break;
            }
            return;
        }

        if (sub.equals("serveritem")) {
            if (args.length < 3) {
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                return;
            }
            String action = args[1].toLowerCase(Locale.ROOT);
            String key = args[2];
            if (action.equals("create")) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_NO_ITEM));
                    return;
                }
                storage.save(key, hand);
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_SERVER_CREATED, key));
            } else if (action.equals("give")) {
                ItemStack retrieved = storage.get(key);
                if (retrieved == null) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_STORAGE_NOT_FOUND, key));
                    return;
                }
                Player target = player;
                if (args.length >= 4) {
                    target = player.getServer().getPlayerExact(args[3]);
                    if (target == null) {
                        player.sendMessage(translator.get(TranslationKey.ESS_PLAYER_OFFLINE));
                        return;
                    }
                }
                int amount = 1;
                if (args.length >= 5) {
                    try {
                        amount = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                        return;
                    }
                }
                retrieved.setAmount(amount);
                Player receiver = target;
                ThreadRouter.owner(org.bukkit.plugin.java.JavaPlugin.getPlugin(dev.zellys.nexus.ZellysNexus.class), receiver,
                        () -> receiver.getInventory().addItem(retrieved));
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_SERVER_GIVEN, key, target.getName()));
            } else {
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
            }
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(translator.get(TranslationKey.ITEMEDIT_NO_ITEM));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        switch (sub) {
            case "rename":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    return;
                }
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                meta.displayName(Component.text(name, TextColor.color(DialogPalette.TEXT_WHITE)));
                item.setItemMeta(meta);
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_RENAMED));
                break;

            case "lore":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    return;
                }
                String loreAction = args[1].toLowerCase(Locale.ROOT);
                if (loreAction.equals("clear")) {
                    meta.lore(null);
                    item.setItemMeta(meta);
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_LORE_CLEARED));
                } else if (loreAction.equals("add")) {
                    if (args.length < 3) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                        return;
                    }
                    List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                    lore.add(Component.text(String.join(" ", Arrays.copyOfRange(args, 2, args.length)), TextColor.color(DialogPalette.TEXT_GRAY)));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_LORE_SET));
                } else if (loreAction.equals("set")) {
                    if (args.length < 4) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                        return;
                    }
                    try {
                        int line = Integer.parseInt(args[2]);
                        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                        while (lore.size() <= line) {
                            lore.add(Component.empty());
                        }
                        lore.set(line, Component.text(String.join(" ", Arrays.copyOfRange(args, 3, args.length)), TextColor.color(DialogPalette.TEXT_GRAY)));
                        meta.lore(lore);
                        item.setItemMeta(meta);
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_LORE_SET));
                    } catch (NumberFormatException e) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    }
                } else {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                }
                break;

            case "enchant":
                if (args.length < 3) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    return;
                }
                try {
                    String enchName = args[1].toLowerCase(Locale.ROOT);
                    Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchName));
                    if (ench == null) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ENCHANT_INVALID, args[1]));
                        return;
                    }
                    int level = Integer.parseInt(args[2]);
                    meta.addEnchant(ench, level, true);
                    item.setItemMeta(meta);
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ENCHANTED, ench.getKey().getKey(), level));
                } catch (Exception e) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ENCHANT_INVALID, args[1]));
                }
                break;

            case "unbreakable":
                boolean isUnbreakable = meta.isUnbreakable();
                if (args.length >= 2) {
                    isUnbreakable = Boolean.parseBoolean(args[1]);
                } else {
                    isUnbreakable = !isUnbreakable;
                }
                meta.setUnbreakable(isUnbreakable);
                item.setItemMeta(meta);
                if (isUnbreakable) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_UNBREAKABLE_ON));
                } else {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_UNBREAKABLE_OFF));
                }
                break;

            case "attribute":
                if (args.length < 3) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    return;
                }
                String attrAction = args[1].toLowerCase(Locale.ROOT);
                if (attrAction.equals("remove")) {
                    try {
                        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(args[2].toLowerCase(Locale.ROOT)));
                        if (attr == null) {
                            player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ATTRIBUTE_INVALID, args[2]));
                            return;
                        }
                        meta.removeAttributeModifier(attr);
                        item.setItemMeta(meta);
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ATTRIBUTE_REMOVED, args[2]));
                    } catch (Exception e) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ATTRIBUTE_INVALID, args[2]));
                    }
                } else if (attrAction.equals("add")) {
                    if (args.length < 4) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                        return;
                    }
                    try {
                        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(args[2].toLowerCase(Locale.ROOT)));
                        if (attr == null) {
                            player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ATTRIBUTE_INVALID, args[2]));
                            return;
                        }
                        double amount = Double.parseDouble(args[3]);
                        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.ANY;
                        if (args.length >= 5) {
                            slotGroup = EquipmentSlotGroup.getByName(args[4]);
                            if (slotGroup == null) slotGroup = EquipmentSlotGroup.ANY;
                        }
                        AttributeModifier.Operation op = AttributeModifier.Operation.ADD_NUMBER;
                        if (args.length >= 6) {
                            op = AttributeModifier.Operation.valueOf(args[5].toUpperCase(Locale.ROOT));
                        }
                        AttributeModifier modifier = new AttributeModifier(NamespacedKey.minecraft("itemedit_" + System.currentTimeMillis()), amount, op, slotGroup);
                        meta.addAttributeModifier(attr, modifier);
                        item.setItemMeta(meta);
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ATTRIBUTE_ADDED, args[2]));
                    } catch (Exception e) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_ATTRIBUTE_INVALID, args[2]));
                    }
                } else {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                }
                break;

            case "custommodeldata":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    return;
                }
                try {
                    int cmd = Integer.parseInt(args[1]);
                    meta.setCustomModelData(cmd);
                    item.setItemMeta(meta);
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_MODEL_SET, cmd));
                } catch (NumberFormatException e) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                }
                break;

            case "flags":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    return;
                }
                String flagAction = args[1].toLowerCase(Locale.ROOT);
                if (flagAction.equals("clear")) {
                    meta.removeItemFlags(meta.getItemFlags().toArray(new ItemFlag[0]));
                    item.setItemMeta(meta);
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_FLAGS_CLEARED));
                } else if (flagAction.equals("add") || flagAction.equals("remove")) {
                    if (args.length < 3) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                        return;
                    }
                    try {
                        ItemFlag flag = ItemFlag.valueOf(args[2].toUpperCase(Locale.ROOT));
                        if (flagAction.equals("add")) {
                            meta.addItemFlags(flag);
                            item.setItemMeta(meta);
                            player.sendMessage(translator.get(TranslationKey.ITEMEDIT_FLAGS_ADDED, flag.name()));
                        } else {
                            meta.removeItemFlags(flag);
                            item.setItemMeta(meta);
                            player.sendMessage(translator.get(TranslationKey.ITEMEDIT_FLAGS_REMOVED, flag.name()));
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                    }
                } else {
                    player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                }
                break;

            case "info":
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_INFO, item.getType().name()));
                player.sendMessage(Component.text("Type: " + item.getType()));
                if (meta.hasCustomModelData()) {
                    player.sendMessage(Component.text("CMD: " + meta.getCustomModelData()));
                }
                player.sendMessage(Component.text("Flags: " + meta.getItemFlags()));
                player.sendMessage(Component.text("Enchants: " + meta.getEnchants().keySet()));
                break;

            default:
                player.sendMessage(translator.get(TranslationKey.ITEMEDIT_USAGE));
                break;
        }
    }

    @Override
    public @Nullable String permission() {
        return "zellysnexus.itemedit";
    }
}
