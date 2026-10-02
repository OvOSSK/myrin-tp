package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;

/**
 * 指令注册。
 */
public final class MyrinCommands {

    private MyrinCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tpa")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> tp().tpa(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), false))));

        d.register(Commands.literal("tpahere")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> tp().tpa(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), true))));

        d.register(Commands.literal("tpyes")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpAccept(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> tp().tpAccept(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")))));

        d.register(Commands.literal("tpaccept")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpAccept(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> tp().tpAccept(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")))));

        d.register(Commands.literal("tpno")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpDeny(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> tp().tpDeny(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")))));

        d.register(Commands.literal("tpdeny")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpDeny(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> tp().tpDeny(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")))));

        d.register(Commands.literal("tpcancel")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpCancel(c.getSource().getPlayerOrException())));

        d.register(Commands.literal("tplist")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpList(c.getSource().getPlayerOrException())));

        d.register(Commands.literal("sethome")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().setHome(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> tp().setHome(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")))));

        d.register(Commands.literal("home")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().goHome(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> tp().goHome(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")))));

        d.register(Commands.literal("homes")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().homes(c.getSource().getPlayerOrException())));

        d.register(Commands.literal("delhome")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> tp().delHome(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")))));

        d.register(Commands.literal("renamehome")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("old", StringArgumentType.word())
                        .then(Commands.argument("new", StringArgumentType.word())
                                .executes(c -> tp().renameHome(c.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(c, "old"), StringArgumentType.getString(c, "new"))))));

        d.register(Commands.literal("back")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().back(c.getSource().getPlayerOrException())));

        d.register(Commands.literal("tpr")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpr(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("range", IntegerArgumentType.integer(16, 1000000))
                        .executes(c -> tp().tpr(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "range")))));

        d.register(Commands.literal("rtp")
                .requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> tp().tpr(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("range", IntegerArgumentType.integer(16, 1000000))
                        .executes(c -> tp().tpr(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "range")))));

        d.register(Commands.literal("otp")
                .requires(s -> hasPerm(s, MyrinTPMod.CONFIG.mode >= 2 ? 4 : 2))
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", IntegerArgumentType.integer(0, 3))
                                .executes(c -> setMode(c.getSource(), IntegerArgumentType.getInteger(c, "mode")))))
                .then(Commands.literal("blacklist1")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(c -> blacklistAdd(c.getSource(), StringArgumentType.getString(c, "player"), true))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(c -> blacklistRemove(c.getSource(), StringArgumentType.getString(c, "player"), true))))
                        .then(Commands.literal("list")
                                .executes(c -> blacklistList1(c.getSource()))))
                .then(Commands.literal("blacklist2")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(c -> blacklistAdd(c.getSource(), StringArgumentType.getString(c, "player"), false))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(c -> blacklistRemove(c.getSource(), StringArgumentType.getString(c, "player"), false))))
                        .then(Commands.literal("list")
                                .executes(c -> blacklistList2(c.getSource()))))
                .then(Commands.literal("whitelist")
                        .then(Commands.literal("add")
                                .then(Commands.argument("command", StringArgumentType.word())
                                        .executes(c -> whitelistAdd(c.getSource(), StringArgumentType.getString(c, "command")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("command", StringArgumentType.word())
                                        .executes(c -> whitelistRemove(c.getSource(), StringArgumentType.getString(c, "command")))))
                        .then(Commands.literal("list")
                                .executes(c -> whitelistList(c.getSource()))))
                .then(Commands.literal("status")
                        .executes(c -> status(c.getSource())))
                .then(Commands.literal("reload")
                        .executes(c -> reload(c.getSource()))));

        d.register(Commands.literal("myrintp")
                .requires(s -> hasPerm(s, MyrinTPMod.CONFIG.mode >= 2 ? 4 : 2))
                .then(Commands.literal("reload")
                        .executes(c -> reload(c.getSource()))));
    }

    private static TpManager tp() {
        return MyrinTPMod.TP;
    }

    private static boolean hasPerm(CommandSourceStack s, int level) {
        Permission need = level >= 4 ? new Permission.HasCommandLevel(PermissionLevel.OWNERS) : new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);
        return s.permissions().hasPermission(need);
    }

    private static int setMode(CommandSourceStack src, int mode) {
        MyrinTPMod.CONFIG.mode = mode;
        MyrinTPMod.CONFIG.save(configFile());
        reply(src, "已设置 Only TP 模式为 " + mode + "（0=关闭 1=非OP仅TP 2=OP仅TP 3=全部）。");
        return 1;
    }

    private static int blacklistAdd(CommandSourceStack src, String name, boolean mode1) {
        List<String> list = mode1 ? MyrinTPMod.CONFIG.blacklistMode1 : MyrinTPMod.CONFIG.blacklistMode2;
        if (!list.contains(name)) {
            list.add(name);
            saveConfig();
            reply(src, "已将 " + name + " 加入" + (mode1 ? "模式一" : "模式二") + "黑名单（豁免对应模式的 TP 限制）。");
        } else {
            reply(src, name + " 已在" + (mode1 ? "模式一" : "模式二") + "黑名单中。");
        }
        return 1;
    }

    private static int blacklistRemove(CommandSourceStack src, String name, boolean mode1) {
        List<String> list = mode1 ? MyrinTPMod.CONFIG.blacklistMode1 : MyrinTPMod.CONFIG.blacklistMode2;
        if (list.remove(name)) {
            saveConfig();
            reply(src, "已将 " + name + " 移出" + (mode1 ? "模式一" : "模式二") + "黑名单。");
        } else {
            reply(src, name + " 不在" + (mode1 ? "模式一" : "模式二") + "黑名单中。");
        }
        return 1;
    }

    private static int blacklistList1(CommandSourceStack src) {
        reply(src, "模式一黑名单（豁免非OP仅TP限制）：" + (MyrinTPMod.CONFIG.blacklistMode1.isEmpty() ? "（空）" : String.join(", ", MyrinTPMod.CONFIG.blacklistMode1)));
        return 1;
    }

    private static int blacklistList2(CommandSourceStack src) {
        reply(src, "模式二黑名单（豁免OP仅TP限制）：" + (MyrinTPMod.CONFIG.blacklistMode2.isEmpty() ? "（空）" : String.join(", ", MyrinTPMod.CONFIG.blacklistMode2)));
        return 1;
    }

    private static int whitelistAdd(CommandSourceStack src, String cmd) {
        if (!MyrinTPMod.CONFIG.whitelistMode2.contains(cmd)) {
            MyrinTPMod.CONFIG.whitelistMode2.add(cmd);
            saveConfig();
            reply(src, "已将指令 " + cmd + " 加入模式二白名单。");
        } else {
            reply(src, "指令 " + cmd + " 已在白名单中。");
        }
        return 1;
    }

    private static int whitelistRemove(CommandSourceStack src, String cmd) {
        if (MyrinTPMod.CONFIG.whitelistMode2.remove(cmd)) {
            saveConfig();
            reply(src, "已将指令 " + cmd + " 移出模式二白名单。");
        } else {
            reply(src, "指令 " + cmd + " 不在白名单中。");
        }
        return 1;
    }

    private static int whitelistList(CommandSourceStack src) {
        reply(src, "模式二指令白名单：" + String.join(", ", MyrinTPMod.CONFIG.whitelistMode2));
        return 1;
    }

    private static int status(CommandSourceStack src) {
        reply(src, "Only TP 模式：" + MyrinTPMod.CONFIG.mode);
        reply(src, "传送套件：/tpa /tpahere /tpyes /tpno /tpcancel /tplist /sethome /home /homes /delhome /renamehome /back /tpr(/rtp)");
        reply(src, "配置目录：" + configFile().getParent().toString());
        return 1;
    }

    private static int reload(CommandSourceStack src) {
        MyrinTPMod.CONFIG = Config.load(configFile());
        reply(src, "配置已重载（mode=" + MyrinTPMod.CONFIG.mode + "）。");
        return 1;
    }

    private static java.nio.file.Path configFile() {
        return MyrinTPMod.CONFIG_DIR.resolve("config.json");
    }

    private static void saveConfig() {
        MyrinTPMod.CONFIG.save(configFile());
    }

    private static void reply(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.GREEN), true);
    }
}
