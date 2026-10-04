package com.myrin.tp;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Set;

/**
 * 指令管控决策逻辑（无 Mixin、无作弊）：
 * <p>
 * mode 0 = 不限制
 * mode 1 = 玩家仅可使用 TP 类指令（模式一黑名单豁免），管理员不受限
 * mode 2 = 管理员仅可使用 TP 类指令（模式二黑名单豁免 + 白名单），普通玩家不受限
 * mode 3 = 任何人仅可使用 TP 类指令（黑名单豁免）
 */
public final class TpGuard {

    /** TP 类指令（含原版 /tp /teleport 与模组传送指令），任何模式下都放行 */
    public static final Set<String> TP_COMMANDS = Set.of(
            "tp", "teleport",
            "tpa", "tpahere", "tpyes", "tpaccept", "tpno", "tpdeny",
            "tpcancel", "tplist",
            "sethome", "home", "homes", "delhome", "renamehome",
            "back", "tpr", "rtp"
    );

    /** 本模组管理指令，任何模式下都放行（由指令自身的 requires 控制权限） */
    public static final Set<String> MOD_COMMANDS = Set.of("mtp", "myrintp");

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
        if (mode == 0) {
            return true;
        }
        if (TP_COMMANDS.contains(command) || MOD_COMMANDS.contains(command)) {
            return true;
        }
        // 白名单仅在模式二/三（管理员受限）生效
        if ((mode == 2 || mode == 3) && config.whitelistMode2.contains(command)) {
            return true;
        }
        // 黑名单豁免：命中名单的玩家不受对应模式限制
        boolean isPlayer = src.getEntity() instanceof ServerPlayer;
        if (isPlayer) {
            String name = ((ServerPlayer) src.getEntity()).getGameProfile().name();
            if ((mode == 1 || mode == 3) && config.blacklistMode1.contains(name)) {
                return true;
            }
            if ((mode == 2 || mode == 3) && config.blacklistMode2.contains(name)) {
                return true;
            }
        }
        boolean isOp = isOp(src);
        return switch (mode) {
            case 1 -> isOp;    // 玩家仅TP：非管理员只能 TP
            case 2 -> !isOp;   // 管理员仅TP：管理员只能 TP
            case 3 -> false;   // 全部仅TP：任何人只能 TP
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
