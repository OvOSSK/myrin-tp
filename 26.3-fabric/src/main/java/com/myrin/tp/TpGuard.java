package com.myrin.tp;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Set;

/**
 * 指令管控决策逻辑（无 Mixin、无作弊）：
 * <p>
 * mode 0 = 不禁用任何指令；玩家获得 tpa 工具（tpa/tpahere/tpaccept 等传送请求指令）
 * mode 1 = 在模式0基础上，玩家开放 tp 权限（/tp 对玩家可用）
 * mode 2 = 管理（OP）禁用部分作弊功能：give、gamemode、F3+F4 快捷；其他指令不禁
 * mode 3 = 除作弊外全部指令开放：tpa 工具、原版 /tp 与原版非作弊指令人人可用，仅禁 give/gamemode/F3+F4
 */
public final class TpGuard {

    /** TP 类指令（含原版 /tp /teleport 与模组传送指令），用于放开指令树补全权限 */
    public static final Set<String> TP_COMMANDS = Set.of(
            "tp", "teleport",
            "tpa", "tpahere", "tpyes", "tpaccept", "tpno", "tpdeny",
            "tpcancel", "tplist",
            "sethome", "home", "homes", "delhome", "renamehome",
            "back", "tpr", "rtp"
    );

    /** 本模组管理指令，任何模式下都放行（由指令自身的 requires 控制权限） */
    public static final Set<String> MOD_COMMANDS = Set.of("mtp", "myrintp");

    /** 原版 /tp /teleport：模式3下所有人仅可用这条传送；模式0/2 对非 OP 不开放 */
    public static final Set<String> VANILLA_TP = Set.of("tp", "teleport");

    /** 模式2下仅对 OP 禁用的作弊指令（F3+F4 走 gamemode 路径，一并拦截） */
    public static final Set<String> CHEAT_COMMANDS = Set.of("give", "gamemode");

    private final Config config;

    public TpGuard(Config config) {
        this.config = config;
    }

    public boolean isWhitelisted(String command) {
        return config.whitelistMode2.contains(command);
    }

    /**
     * 判断某指令对当前命令源是否放行。requires 每次执行命令时都会重新求值，
     * 因此游戏内修改配置后立即生效。
     */
    public boolean allows(CommandSourceStack src, String command) {
        int mode = config.mode;
        boolean isOp = isOp(src);
        return switch (mode) {
            case 0, 1 -> true;                                     // 不禁用任何指令（/tp 对玩家是否开放由 CommandBlocker 单独判）
            case 2 -> !isOp || !CHEAT_COMMANDS.contains(command);  // 仅 OP 被禁 give/gamemode/F3+F4，其他指令不禁
            case 3 -> !CHEAT_COMMANDS.contains(command);             // 除作弊外全部指令开放（tpa、tp、原版非作弊指令人人可用）
            default -> true;
        };
    }

    /** OP 判定：先按命令源权限集，再按服务器 OP 列表兜底。 */
    public boolean isOp(CommandSourceStack src) {
        boolean op = src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))
                || src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS))
                || src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
        if (!op && src.getEntity() instanceof ServerPlayer p) {
            op = src.getServer().getPlayerList().isOp(p.nameAndId());
        }
        return op;
    }
}
