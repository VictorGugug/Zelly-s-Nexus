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
import dev.zellys.nexus.thread.ThreadRouter;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

final class DebugSubcommand implements BasicCommand {
    private static final List<String> AREAS = List.of("translation", "auth", "clans", "files", "essentials", "threads", "world");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final ZellysNexus plugin;
    private final Translator translator;

    DebugSubcommand(ZellysNexus plugin) {
        this.plugin = plugin;
        this.translator = plugin.translator();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String area = args.length == 0 ? "all" : args[0].toLowerCase(Locale.ROOT);
        if (args.length > 1 || (!area.equals("all") && !AREAS.contains(area))) {
            sender.sendMessage(translator.get(TranslationKey.DEBUG_USAGE));
            return;
        }
        Map<String, CompletableFuture<List<String>>> runs = new LinkedHashMap<>();
        for (String name : area.equals("all") ? AREAS : List.of(area)) {
            runs.put(name, runArea(name, sender).orTimeout(10, TimeUnit.SECONDS).exceptionally(e -> List.of(e.toString())));
        }
        CompletableFuture.allOf(runs.values().toArray(CompletableFuture[]::new)).thenRun(() -> report(sender, runs));
    }

    private void report(CommandSender sender, Map<String, CompletableFuture<List<String>>> runs) {
        List<String> lines = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        for (Map.Entry<String, CompletableFuture<List<String>>> run : runs.entrySet()) {
            String name = run.getKey();
            List<String> failures = run.getValue().join();
            lines.add(translator.get(TranslationKey.DEBUG_HEADER, name));
            if (failures.isEmpty()) {
                passed++;
                lines.add(translator.get(TranslationKey.DEBUG_PASS, name));
            } else {
                failed++;
                for (String failure : failures) {
                    lines.add(translator.get(TranslationKey.DEBUG_FAIL, name, failure));
                }
            }
        }
        lines.add(translator.get(TranslationKey.DEBUG_SUMMARY, passed, failed, passed + failed));
        lines.forEach(sender::sendMessage);
        ThreadRouter.async(plugin, () -> {
            Path file = plugin.getDataFolder().toPath().resolve("debug").resolve("debug-" + STAMP.format(LocalDateTime.now()) + ".log");
            try {
                Files.createDirectories(file.getParent());
                Files.write(file, lines);
                sender.sendMessage(translator.get(TranslationKey.DEBUG_REPORT_SAVED, file));
            } catch (IOException e) {
                sender.sendMessage(translator.get(TranslationKey.DEBUG_REPORT_FAILED, e.getMessage()));
            }
        });
    }

    private CompletableFuture<List<String>> runArea(String area, CommandSender sender) {
        return switch (area) {
            case "translation" -> CompletableFuture.completedFuture(checkTranslation());
            case "auth" -> CompletableFuture.completedFuture(checkAuth());
            case "clans" -> CompletableFuture.completedFuture(checkClans());
            case "files" -> CompletableFuture.completedFuture(checkFiles());
            case "essentials" -> CompletableFuture.completedFuture(checkEssentials());
            case "threads" -> checkThreads();
            case "world" -> checkWorld(sender);
            default -> CompletableFuture.completedFuture(List.of(area));
        };
    }

    private CompletableFuture<List<String>> checkThreads() {
        Server server = plugin.getServer();
        Location spawn = server.getWorlds().get(0).getSpawnLocation();
        CompletableFuture<List<String>> result = new CompletableFuture<>();
        ThreadRouter.async(plugin, () -> {
            List<String> failures = new ArrayList<>();
            check(failures, "async-off-tick", !server.isGlobalTickThread() && !server.isOwnedByCurrentRegion(spawn));
            ThreadRouter.global(plugin, () -> {
                check(failures, "global", server.isGlobalTickThread());
                result.complete(failures);
            });
        });
        return result;
    }

    private CompletableFuture<List<String>> checkWorld(CommandSender sender) {
        Location base = sender instanceof Player player ? player.getLocation() : plugin.getServer().getWorlds().get(0).getSpawnLocation();
        World world = base.getWorld();
        Location probe = new Location(world, base.getBlockX(), world.getMaxHeight() - 1, base.getBlockZ());
        CompletableFuture<List<String>> result = new CompletableFuture<>();
        world.getChunkAtAsync(probe).thenRun(() -> ThreadRouter.region(plugin, probe, () -> {
            try {
                List<String> failures = new ArrayList<>();
                check(failures, "region-owner", plugin.getServer().isOwnedByCurrentRegion(probe));
                Block block = probe.getBlock();
                BlockData original = block.getBlockData();
                Material marker = original.getMaterial() == Material.STONE ? Material.GLASS : Material.STONE;
                try {
                    block.setType(marker, false);
                    check(failures, "block-set", block.getType() == marker);
                } finally {
                    block.setBlockData(original, false);
                }
                check(failures, "block-restored", block.getBlockData().equals(original));
                result.complete(failures);
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        }));
        return result;
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
            deleteTemp(temp, failures);
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
            deleteTemp(temp, failures);
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
            deleteTemp(temp, failures);
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
            deleteTemp(temp, failures);
        }
        return failures;
    }

    private static void check(List<String> failures, String name, boolean ok) {
        if (!ok) {
            failures.add(name);
        }
    }

    private static void deleteTemp(Path temp, List<String> failures) {
        if (temp == null) {
            return;
        }
        try (var stream = Files.walk(temp)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            failures.add("cleanup " + e);
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String partial = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> out = new ArrayList<>();
            for (String area : Stream.concat(AREAS.stream(), Stream.of("all")).toList()) {
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
        DebugSubcommand cmd = new DebugSubcommand(plugin);
        if (znCommand != null) {
            znCommand.registerCommand("debug", "dbg", "zn.debug", null, cmd);
        }
        plugin.permission("zn.debug", org.bukkit.permissions.PermissionDefault.OP);
        return (System.nanoTime() - start) / 1_000_000;
    }
}
