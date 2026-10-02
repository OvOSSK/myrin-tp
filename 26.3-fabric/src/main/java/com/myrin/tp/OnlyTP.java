package com.myrin.tp;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Set;

/**
 * Only TP 指令权限限制决策逻辑（无 Mixin、无作弊）：
 * <p>
 * mode 0 = 关闭
 * mode 1 = 非 OP 玩家仅可使用 TP 类指令（+ 模式一黑名单豁免），OP 不受限
 * mode 2 = OP 玩家仅可使用 TP 类指令（+ 模式二黑名单豁免 + 指令白名单）
 * mode 3 = 模式一、二同时生效
 */
public final class OnlyTP {

    /** TP 类指令（含原版 /tp /teleport 与模组传送指令），任何模式下都放行 */
    public static final Set<String> TP_COMMANDS = Set.of(
            "tp", "teleport",
            "tpa", "tpahere", "tpyes", "tpaccept", "tpno", "tpdeny",
            "tpcancel", "tplist",
            "sethome", "home", "homes", "delhome", "renamehome",
            "back", "tpr", "rtp"
    );

    /** 本模组管理指令，任何模式下都放行（由指令自身的 requires 控制权限） */
    public static final Set<String> MOD_COMMANDS = Set.of("otp", "myrintp");

    private final Config config;

    public OnlyTP(Config config) {
        this.config = config;
    }

    public boolean isWhitelisted(String command) {
        return config.whitelistMode2.contains(command);
    }

    /**
     * 判断某指令对当前命令源是否放行。
     */
    public boolean allows(CommandSourceStack src, String command) {
        int mode = config.mode;
        if (mode == 0) {
            return true;
        }
        if (TP_COMMANDS.contains(command) || MOD_COMMANDS.contains(command)) {
            return true;
        }
        // 指令白名单仅在模式二/三（OP 限制）生效
        if ((mode == 2 || mode == 3) && config.whitelistMode2.contains(command)) {
            return true;
        }
        int level = mode == 1 ? 2 : 4;
        if (src.getEntity() instanceof ServerPlayer p) {
            String name = p.getGameProfile().name();
            if ((mode == 1 || mode == 3) && config.blacklistMode1.contains(name)) {
                return true;
            }
            if ((mode == 2 || mode == 3) && config.blacklistMode2.contains(name)) {
                return true;
            }
        }
        Permission need = level >= 4 ? new Permission.HasCommandLevel(PermissionLevel.OWNERS) : new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);
        return src.permissions().hasPermission(need);
    }
}
