/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.debug;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.auth.AuthService;
import dev.zellys.nexus.clans.ClanService;
import dev.zellys.nexus.common.store.YamlStore;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

final class DebugSubcommand implements BasicCommand {
    private final Translator translator;

    DebugSubcommand(Translator translator) {
        this.translator = translator;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length != 1) {
            sender.sendMessage(translator.get(TranslationKey.DEBUG_USAGE));
            return;
        }
        String area = args[0].toLowerCase();
        List<String> areas = new ArrayList<>();
        if (area.equals("all")) {
            areas.add("translation");
            areas.add("auth");
            areas.add("clans");
            areas.add("files");
            areas.add("essentials");
        } else if (area.equals("translation") || area.equals("auth") || area.equals("clans") || area.equals("files") || area.equals("essentials")) {
            areas.add(area);
        } else {
            sender.sendMessage(translator.get(TranslationKey.DEBUG_USAGE));
            return;
        }
        int passed = 0;
        int failed = 0;
        for (String name : areas) {
            sender.sendMessage(translator.get(TranslationKey.DEBUG_HEADER, name));
            List<String> failures = runArea(name);
            if (failures.isEmpty()) {
                passed++;
                sender.sendMessage(translator.get(TranslationKey.DEBUG_PASS, name));
            } else {
                failed++;
                for (String failure : failures) {
                    sender.sendMessage(translator.get(TranslationKey.DEBUG_FAIL, name, failure));
                }
            }
        }
        sender.sendMessage(translator.get(TranslationKey.DEBUG_SUMMARY, passed, failed));
    }

    private List<String> runArea(String area) {
        return switch (area) {
            case "translation" -> checkTranslation();
            case "auth" -> checkAuth();
            case "clans" -> checkClans();
            case "files" -> checkFiles();
            case "essentials" -> checkEssentials();
            default -> List.of(area);
        };
    }

    private List<String> checkTranslation() {
        List<String> failures = new ArrayList<>();
        if (translator.completion("en") != 100) {
            failures.add("en=" + translator.completion("en"));
        }
        if (translator.completion("es") != 100) {
            failures.add("es=" + translator.completion("es"));
        }
        return failures;
    }

    private List<String> checkAuth() {
        List<String> failures = new ArrayList<>();
        Path temp = null;
        try {
            temp = Files.createTempDirectory("zn-debug-auth");
            AuthService service = new AuthService(new YamlStore(temp.resolve("auth.yml")));
            UUID premium = UUID.randomUUID();
            check(failures, "register", service.register(premium, "10.9.9.1", "secret") == AuthService.AuthResult.OK);
            service.setPremium(premium);
            service.logout(premium);
            check(failures, "premium-autologin", service.autoLoginIfPremium(premium, "Zell", "10.9.9.1"));
            UUID cracked = UUID.randomUUID();
            service.register(cracked, "10.9.9.2", "secret");
            service.setCracked(cracked);
            service.logout(cracked);
            check(failures, "cracked-blocked", !service.autoLoginIfPremium(cracked, "Zell", "10.9.9.2"));
            check(failures, "login-wrong", service.login(premium, "10.9.9.1", "wrong") == AuthService.AuthResult.WRONG_PASSWORD);
            String captcha = service.issueCaptcha(premium);
            check(failures, "captcha", service.answerCaptcha(premium, captcha));
            String code = service.issueVerification(premium);
            check(failures, "verification", service.confirmVerification(premium, code));
            String otp = service.generateOtp(premium);
            check(failures, "otp", service.verifyOtp(premium, otp));
        } catch (Exception e) {
            failures.add(e.toString());
        } finally {
            deleteTemp(temp);
        }
        return failures;
    }

    private List<String> checkClans() {
        List<String> failures = new ArrayList<>();
        Path temp = null;
        try {
            temp = Files.createTempDirectory("zn-debug-clans");
            ClanService service = new ClanService(new YamlStore(temp.resolve("clans.yml")));
            UUID owner = UUID.randomUUID();
            UUID member = UUID.randomUUID();
            check(failures, "create", service.create(owner, "Debug") == ClanService.ClanResult.OK);
            check(failures, "invite", service.invite(owner, member) == ClanService.ClanResult.OK);
            check(failures, "accept", service.accept(member) == ClanService.ClanResult.OK);
            check(failures, "friendly", service.areFriendly(owner, member));
            service.setGlobalFf(true);
            check(failures, "globalff", !service.areFriendly(owner, member));
            service.setGlobalFf(false);
            check(failures, "globalff-off", service.areFriendly(owner, member));
        } catch (Exception e) {
            failures.add(e.toString());
        } finally {
            deleteTemp(temp);
        }
        return failures;
    }

    private List<String> checkFiles() {
        List<String> failures = new ArrayList<>();
        Path temp = null;
        try {
            temp = Files.createTempDirectory("zn-debug-files");
            YamlStore store = new YamlStore(temp.resolve("roundtrip.yml"));
            Map<String, Object> data = new HashMap<>();
            data.put("name", "Zelly");
            data.put("numbers", List.of(1, 2, 3));
            Map<String, Object> nested = new HashMap<>();
            nested.put("on", true);
            data.put("nested", nested);
            store.save(data);
            Map<String, Object> back = store.load();
            check(failures, "roundtrip", "Zelly".equals(back.get("name")) && back.get("nested") instanceof Map);
        } catch (Exception e) {
            failures.add(e.toString());
        } finally {
            deleteTemp(temp);
        }
        return failures;
    }

    private List<String> checkEssentials() {
        List<String> failures = new ArrayList<>();
        Path temp = null;
        try {
            temp = Files.createTempDirectory("zn-debug-ess");
            YamlStore store = new YamlStore(temp.resolve("kits.yml"));
            org.bukkit.inventory.ItemStack original = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE, 2);
            Map<String, Object> node = new HashMap<>();
            node.put("cooldown", 60L);
            node.put("items", List.of(original.serialize()));
            Map<String, Object> root = new HashMap<>();
            root.put("starter", node);
            store.save(root);
            Map<String, Object> back = store.load();
            Object kitRaw = back.get("starter");
            boolean roundtrip = false;
            if (kitRaw instanceof Map) {
                Object itemsRaw = ((Map<?, ?>) kitRaw).get("items");
                if (itemsRaw instanceof List && !((List<?>) itemsRaw).isEmpty() && ((List<?>) itemsRaw).get(0) instanceof Map) {
                    org.bukkit.inventory.ItemStack restored = org.bukkit.inventory.ItemStack.deserialize((Map<String, Object>) ((List<?>) itemsRaw).get(0));
                    roundtrip = restored.getType() == org.bukkit.Material.STONE && restored.getAmount() == 2;
                }
            }
            check(failures, "kit-roundtrip", roundtrip);
            check(failures, "kit-cooldown", dev.zellys.nexus.essentials.EssentialsData.kitCooldownLeft(60L, 1000L, 1030L) == 30L
                    && dev.zellys.nexus.essentials.EssentialsData.kitCooldownLeft(60L, 1000L, 2000L) == 0L
                    && dev.zellys.nexus.essentials.EssentialsData.kitCooldownLeft(0L, 1000L, 1001L) == 0L);
        } catch (Exception e) {
            failures.add(e.toString());
        } finally {
            deleteTemp(temp);
        }
        return failures;
    }

    private static void check(List<String> failures, String name, boolean ok) {
        if (!ok) {
            failures.add(name);
        }
    }

    private static void deleteTemp(Path temp) {
        if (temp == null) {
            return;
        }
        try (var stream = Files.walk(temp)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String partial = args.length == 1 ? args[0].toLowerCase() : "";
            List<String> out = new ArrayList<>();
            for (String area : List.of("translation", "auth", "clans", "files", "essentials", "all")) {
                if (area.startsWith(partial)) {
                    out.add(area);
                }
            }
            return out;
        }
        return List.of();
    }

    @Override
    public @Nullable String permission() {
        return "zn.debug";
    }
}

public final class DebugModule {
    private DebugModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        DebugSubcommand cmd = new DebugSubcommand(plugin.translator());
        if (znCommand != null) {
            znCommand.registerCommand("debug", "dbg", "zn.debug", null, cmd);
        }
        plugin.permission("zn.debug", org.bukkit.permissions.PermissionDefault.OP);
        return (System.nanoTime() - start) / 1_000_000;
    }
}
