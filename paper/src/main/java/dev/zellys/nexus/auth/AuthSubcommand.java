/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.auth;

import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class AuthSubcommand implements BasicCommand {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "register", "login", "logout", "unregister", "delacc", "changepassword", "email",
            "totp", "otp", "pin", "remember", "captcha", "verification", "premium", "cracked",
            "freemium", "getlogs", "admin", "form"
    );

    private final Translator translator;
    private final AuthService service;
    private final Server server;
    private final dev.zellys.nexus.dialog.DialogManager dialogs;

    public AuthSubcommand(Translator translator, AuthService service, Server server) {
        this(translator, service, server, null);
    }

    public AuthSubcommand(Translator translator, AuthService service, Server server, dev.zellys.nexus.dialog.DialogManager dialogs) {
        this.translator = translator;
        this.service = service;
        this.server = server;
        this.dialogs = dialogs;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            return;
        }
        if (args.length == 0) {
            player.sendMessage(translator.get(TranslationKey.AUTH_USAGE));
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "register":
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_REGISTER_USAGE));
                    return;
                }
                switch (service.register(player.getUniqueId(), AuthModule.address(player), args[1])) {
                    case OK -> player.sendMessage(translator.get(TranslationKey.AUTH_REGISTERED));
                    case ALREADY_REGISTERED -> player.sendMessage(translator.get(TranslationKey.AUTH_ALREADY_REGISTERED));
                    case TOO_MANY_ACCOUNTS -> player.sendMessage(translator.get(TranslationKey.AUTH_TOO_MANY_ACCOUNTS));
                    default -> player.sendMessage(translator.get(TranslationKey.AUTH_SHORT_PASSWORD));
                }
                break;
            case "login":
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_LOGIN_USAGE));
                    return;
                }
                switch (service.login(player.getUniqueId(), AuthModule.address(player), args[1])) {
                    case OK -> player.sendMessage(translator.get(TranslationKey.AUTH_LOGGED_IN));
                    case NOT_REGISTERED -> player.sendMessage(translator.get(TranslationKey.AUTH_NOT_REGISTERED));
                    default -> player.sendMessage(translator.get(TranslationKey.AUTH_WRONG_PASSWORD));
                }
                break;
            case "logout":
                service.logout(player.getUniqueId());
                player.sendMessage(translator.get(TranslationKey.AUTH_LOGGED_OUT));
                break;
            case "unregister":
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_USAGE));
                    return;
                }
                switch (service.unregister(player.getUniqueId(), args[1])) {
                    case OK -> player.sendMessage(translator.get(TranslationKey.AUTH_UNREGISTERED));
                    case NOT_REGISTERED -> player.sendMessage(translator.get(TranslationKey.AUTH_NOT_REGISTERED));
                    default -> player.sendMessage(translator.get(TranslationKey.AUTH_WRONG_PASSWORD));
                }
                break;
            case "delacc":
                if (!player.hasPermission("zn.auth.admin")) return;
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_ADMIN_USAGE));
                    return;
                }
                Player targetDel = server.getPlayer(args[1]);
                if (targetDel != null) {
                    if (service.deleteAccount(targetDel.getUniqueId()) == AuthService.AuthResult.OK) {
                        player.sendMessage(translator.get(TranslationKey.AUTH_ACCOUNT_DELETED, args[1]));
                    }
                }
                break;
            case "changepassword":
                if (args.length != 3) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_USAGE));
                    return;
                }
                switch (service.changePassword(player.getUniqueId(), args[1], args[2])) {
                    case OK -> player.sendMessage(translator.get(TranslationKey.AUTH_PASSWORD_CHANGED));
                    case NOT_REGISTERED -> player.sendMessage(translator.get(TranslationKey.AUTH_NOT_REGISTERED));
                    case WRONG_PASSWORD -> player.sendMessage(translator.get(TranslationKey.AUTH_PASSWORD_MISMATCH));
                    default -> player.sendMessage(translator.get(TranslationKey.AUTH_SHORT_PASSWORD));
                }
                break;
            case "email":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_EMAIL_USAGE));
                    return;
                }
                if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("change")) {
                    if (args.length == 3) {
                        service.setEmail(player.getUniqueId(), args[2]);
                        player.sendMessage(translator.get(args[1].equalsIgnoreCase("add") ? TranslationKey.AUTH_EMAIL_ADDED : TranslationKey.AUTH_EMAIL_CHANGED));
                    } else {
                        player.sendMessage(translator.get(TranslationKey.AUTH_EMAIL_USAGE));
                    }
                } else if (args[1].equalsIgnoreCase("recover")) {
                    if (service.getEmail(player.getUniqueId()) == null) {
                        player.sendMessage(translator.get(TranslationKey.AUTH_NO_EMAIL));
                    } else if (args.length == 2) {
                        String code = service.issueVerification(player.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.AUTH_VERIFICATION_SENT, code));
                    } else if (args.length == 4 && service.confirmVerification(player.getUniqueId(), args[2])) {
                        switch (service.resetPassword(player.getUniqueId(), args[3])) {
                            case OK -> player.sendMessage(translator.get(TranslationKey.AUTH_PASSWORD_CHANGED));
                            default -> player.sendMessage(translator.get(TranslationKey.AUTH_SHORT_PASSWORD));
                        }
                    } else {
                        player.sendMessage(translator.get(TranslationKey.AUTH_VERIFICATION_FAIL));
                    }
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_EMAIL_USAGE));
                }
                break;
            case "totp":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_TOTP_USAGE));
                    return;
                }
                if (args[1].equalsIgnoreCase("setup")) {
                    String secret = service.setupTotp(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.AUTH_TOTP_SETUP, secret));
                } else if (args[1].equalsIgnoreCase("confirm")) {
                    if (args.length == 3 && service.confirmTotp(player.getUniqueId(), args[2])) {
                        player.sendMessage(translator.get(TranslationKey.AUTH_TOTP_ENABLED));
                    } else {
                        player.sendMessage(translator.get(TranslationKey.AUTH_TOTP_INVALID));
                    }
                } else if (args[1].equalsIgnoreCase("disable")) {
                    service.disableTotp(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.AUTH_TOTP_DISABLED));
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_TOTP_USAGE));
                }
                break;
            case "otp":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_OTP_USAGE));
                    return;
                }
                if (args[1].equalsIgnoreCase("generate")) {
                    String code = service.generateOtp(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.AUTH_OTP_GENERATED, code));
                } else if (args[1].equalsIgnoreCase("verify")) {
                    if (args.length == 3 && service.verifyOtp(player.getUniqueId(), args[2])) {
                        player.sendMessage(translator.get(TranslationKey.AUTH_OTP_VERIFIED));
                    } else {
                        player.sendMessage(translator.get(TranslationKey.AUTH_OTP_INVALID));
                    }
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_OTP_USAGE));
                }
                break;
            case "pin":
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_PIN_USAGE));
                    return;
                }
                if (args[1].equalsIgnoreCase("set")) {
                    if (args.length == 3) {
                        service.setPin(player.getUniqueId(), args[2]);
                        player.sendMessage(translator.get(TranslationKey.AUTH_PIN_SET));
                    } else {
                        player.sendMessage(translator.get(TranslationKey.AUTH_PIN_USAGE));
                    }
                } else if (args[1].equalsIgnoreCase("verify")) {
                    if (args.length == 3 && service.verifyPin(player.getUniqueId(), args[2])) {
                        player.sendMessage(translator.get(TranslationKey.AUTH_PIN_VERIFIED));
                    } else {
                        player.sendMessage(translator.get(TranslationKey.AUTH_PIN_INVALID));
                    }
                } else if (args[1].equalsIgnoreCase("reset")) {
                    service.resetPin(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.AUTH_PIN_RESET));
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_PIN_USAGE));
                }
                break;
            case "remember":
                if (args.length == 2) {
                    boolean on = args[1].equalsIgnoreCase("on");
                    service.setRemember(player.getUniqueId(), on);
                    player.sendMessage(translator.get(on ? TranslationKey.AUTH_REMEMBER_ON : TranslationKey.AUTH_REMEMBER_OFF));
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_USAGE));
                }
                break;
            case "captcha":
                if (args.length == 1) {
                    String challenge = service.issueCaptcha(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.AUTH_CAPTCHA_CHALLENGE, challenge));
                } else if (args.length == 2 && service.answerCaptcha(player.getUniqueId(), args[1])) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_CAPTCHA_OK));
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_CAPTCHA_FAIL));
                }
                break;
            case "verification":
                if (args.length == 1) {
                    String code = service.issueVerification(player.getUniqueId());
                    player.sendMessage(translator.get(TranslationKey.AUTH_VERIFICATION_SENT, code));
                } else if (args.length == 2 && service.confirmVerification(player.getUniqueId(), args[1])) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_VERIFICATION_OK));
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_VERIFICATION_FAIL));
                }
                break;
            case "premium":
                if (!player.hasPermission("zn.auth.admin")) return;
                if (args.length == 2) {
                    Player target = server.getPlayer(args[1]);
                    if (target != null) {
                        service.setPremium(target.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.AUTH_PREMIUM_SET, args[1]));
                    }
                }
                break;
            case "cracked":
                if (!player.hasPermission("zn.auth.admin")) return;
                if (args.length == 2) {
                    Player target = server.getPlayer(args[1]);
                    if (target != null) {
                        service.setCracked(target.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.AUTH_CRACKED_SET, args[1]));
                    }
                }
                break;
            case "freemium":
                if (!player.hasPermission("zn.auth.admin")) return;
                if (args.length == 2) {
                    Player target = server.getPlayer(args[1]);
                    if (target != null) {
                        service.setFreemium(target.getUniqueId());
                        player.sendMessage(translator.get(TranslationKey.AUTH_FREEMIUM_SET, args[1]));
                    }
                }
                break;
            case "getlogs":
                if (!player.hasPermission("zn.auth.admin")) return;
                if (args.length == 2) {
                    Player target = server.getPlayer(args[1]);
                    if (target != null) {
                        List<String> logs = service.getLogs(target.getUniqueId());
                        if (logs.isEmpty()) {
                            player.sendMessage(translator.get(TranslationKey.AUTH_LOGS_EMPTY));
                        } else {
                            player.sendMessage(translator.get(TranslationKey.AUTH_LOGS_HEADER, args[1]));
                            for (String log : logs) {
                                player.sendMessage(translator.get(TranslationKey.AUTH_LOGS_ENTRY, log));
                            }
                        }
                    }
                }
                break;
            case "admin":
                if (!player.hasPermission("zn.auth.admin")) return;
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.AUTH_ADMIN_USAGE));
                    return;
                }
                if (args[1].equalsIgnoreCase("reload")) {
                    service.reload();
                    player.sendMessage(translator.get(TranslationKey.AUTH_ADMIN_RELOADED));
                } else if (args[1].equalsIgnoreCase("purge")) {
                    int days = args.length == 3 ? Integer.parseInt(args[2]) : 30;
                    int count = service.purgeInactive(days);
                    player.sendMessage(translator.get(TranslationKey.AUTH_ADMIN_PURGED, count));
                } else if (args[1].equalsIgnoreCase("forcelogin")) {
                    if (args.length == 3) {
                        Player target = server.getPlayer(args[2]);
                        if (target != null) {
                            service.forceLogin(target.getUniqueId(), AuthModule.address(target));
                            player.sendMessage(translator.get(TranslationKey.AUTH_ADMIN_FORCELOGIN, args[2]));
                        }
                    }
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_ADMIN_USAGE));
                }
                break;
            case "form":
                if (dialogs != null) {
                    dialogs.show(player, new dev.zellys.nexus.common.dialog.ZDialog("auth-form", TranslationKey.AUTH_USAGE, java.util.List.of(TranslationKey.AUTH_REGISTER_USAGE, TranslationKey.AUTH_LOGIN_USAGE)));
                } else {
                    player.sendMessage(translator.get(TranslationKey.AUTH_USAGE));
                }
                break;
            default:
                player.sendMessage(translator.get(TranslationKey.AUTH_USAGE));
                break;
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length <= 1) {
            String partial = args.length == 1 ? args[0].toLowerCase() : "";
            List<String> list = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    list.add(sub);
                }
            }
            return list;
        }
        return List.of();
    }
}
