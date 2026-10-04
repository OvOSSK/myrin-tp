package com.myrin.tp;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 全局命令执行点拦截：在命令真正执行前按管控模式判定。
 * 指令补全不受影响（不改 requires），被禁指令执行时红字提示并取消。
 * 指令名优先取解析节点，解析结果异常时退回原始命令字符串，兼容各版本。
 */
public final class CommandBlocker {

    /** F3+F4 快捷切换模式走数据包/命令路径，按管控模式判定是否禁止（等价于 /gamemode）。 */
    public static boolean shouldBlockGameModeSwitch(ServerPlayer player) {
        try {
            return !MyrinTPMod.GUARD.allows(player.createCommandSourceStack(), "gamemode");
        } catch (Exception e) {
            System.err.println("[myrintp] 模式切换管控判定异常，按禁止处理：" + e);
            return true;
        }
    }

    private CommandBlocker() {
    }

    /** 返回 true 表示应取消本次执行（已发送红字提示）。判定异常时按 fail-closed 拦截。 */
    public static boolean shouldCancel(ParseResults<CommandSourceStack> parse, TpGuard guard) {
        try {
            CommandSourceStack src = parse.getContext().getSource();
            if (!(src.getEntity() instanceof ServerPlayer)) {
                return false;
            }
            String cmd = resolveName(parse);
            // 模式 0：恢复原版 /tp /teleport 权限（非 OP 不可用），其余指令不限制
            if (MyrinTPMod.CONFIG.mode == 0
                    && ("tp".equals(cmd) || "teleport".equals(cmd))
                    && !guard.isOp(src)) {
                src.sendFailure(Component.literal("你没有权限使用此命令"));
                return true;
            }
            if (TpGuard.MOD_COMMANDS.contains(cmd) || TpGuard.TP_COMMANDS.contains(cmd)) {
                return false;
            }
            if (guard.allows(src, cmd)) {
                return false;
            }
            src.sendFailure(Component.literal("你没有权限执行此指令！请联系服务器管理员处理！"));
            return true;
        } catch (Exception e) {
            System.err.println("[myrintp] 指令管控判定异常，按禁止处理：" + e);
            return true;
        }
    }

    private static String resolveName(ParseResults<CommandSourceStack> parse) {
        if (!parse.getContext().getNodes().isEmpty()) {
            return parse.getContext().getNodes().get(0).getNode().getName();
        }
        String raw = parse.getReader().getString();
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String first = raw.split(" ")[0].toLowerCase();
        if (first.startsWith("/")) {
            first = first.substring(1);
        }
        return first;
    }
}
