/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.command;

import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ZnRootCommand implements BasicCommand {

    private final Translator translator;
    private final Map<String, ModuleInfo> modules = new HashMap<>();

    public ZnRootCommand(Translator translator) {
        this.translator = translator;
    }

    public void register(String module, String alias, ModuleHandler handler) {
        register(module, alias, null, null, handler);
    }

    public void register(String module, String alias, String permission, TranslationKey descriptionKey, ModuleHandler handler) {
        ModuleInfo info = new ModuleInfo(module, alias, permission, descriptionKey, handler);
        modules.put(module.toLowerCase(), info);
        if (alias != null && !alias.isEmpty()) {
            modules.put(alias.toLowerCase(), info);
        }
    }

    public void registerCommand(String module, String alias, BasicCommand command) {
        registerCommand(module, alias, command.permission(), null, command);
    }

    public void registerCommand(String module, String alias, String permission, TranslationKey descriptionKey, BasicCommand command) {
        register(module, alias, permission, descriptionKey, new ModuleHandler() {
            @Override
            public void execute(CommandSourceStack source, String[] args) {
                command.execute(source, args);
            }

            @Override
            public Collection<String> suggest(CommandSourceStack source, String[] args) {
                return command.suggest(source, args);
            }
        });
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(source);
            return;
        }

        String sub = args[0].toLowerCase();
        ModuleInfo info = modules.get(sub);

        if (info == null) {
            source.getSender().sendMessage(translator.get(TranslationKey.CMD_UNKNOWN_SUB, sub));
            return;
        }

        if (info.permission() != null && !source.getSender().hasPermission(info.permission())) {
            source.getSender().sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
            return;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        info.handler().execute(source, subArgs);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase() : "";
            return modules.values().stream()
                    .filter(info -> info.permission() == null || source.getSender().hasPermission(info.permission()))
                    .map(ModuleInfo::module)
                    .distinct()
                    .filter(name -> name.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();
        ModuleInfo info = modules.get(sub);

        if (info != null && (info.permission() == null || source.getSender().hasPermission(info.permission()))) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return info.handler().suggest(source, subArgs);
        }

        return List.of();
    }

    private void sendHelp(CommandSourceStack source) {
        source.getSender().sendMessage(translator.get(TranslationKey.CMD_HELP_HEADER));
        for (ModuleInfo info : modules.values().stream().distinct().toList()) {
            if (info.permission() == null || source.getSender().hasPermission(info.permission())) {
                String desc = info.descriptionKey() != null ? translator.get(info.descriptionKey()) : info.module();
                source.getSender().sendMessage(translator.get(TranslationKey.CMD_HELP_ENTRY, info.module(), desc));
            }
        }
    }

    @FunctionalInterface
    public interface ModuleHandler {
        void execute(CommandSourceStack source, String[] args);

        default Collection<String> suggest(CommandSourceStack source, String[] args) {
            return List.of();
        }
    }

    private record ModuleInfo(String module, String alias, String permission, TranslationKey descriptionKey, ModuleHandler handler) {
    }
}
