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

    private CommandBlocker() {
    }

    /** 返回 true 表示应取消本次执行（已发送红字提示）。 */
    public static boolean shouldCancel(ParseResults<CommandSourceStack> parse, TpGuard guard) {
        CommandSourceStack src = parse.getContext().getSource();
        if (!(src.getEntity() instanceof ServerPlayer)) {
            return false;
        }
        String cmd = resolveName(parse);
        if (TpGuard.MOD_COMMANDS.contains(cmd) || TpGuard.TP_COMMANDS.contains(cmd)) {
            return false;
        }
        if (guard.allows(src, cmd)) {
            return false;
        }
        src.sendFailure(Component.literal("该指令已被管理员禁止！仅允许使用 TP 类指令"));
        return true;
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
