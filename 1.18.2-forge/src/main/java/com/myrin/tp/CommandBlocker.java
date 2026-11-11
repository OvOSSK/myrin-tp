package com.myrin.tp;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 全局命令执行点拦截：在命令真正执行前按管控模式判定。
 * 指令补全不受影响（不改 requires），被禁指令执行时红字提示并取消。
 * 指令名优先取解析节点，解析结果异常时退回原始命令字符串，兼容各版本。
 * <p>
 * /execute 会把真正要执行的命令包在 run 之后（如 execute in minecraft:the_end run tp @s x y z），
 * 这里解析出 run 后面的实际命令名参与判定：传送类一律放行，其余按原模式规则判定，
 * 避免 execute 成为绕过 give / gamemode 管控的通道。
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
            // execute 链内真正执行的命令（非 execute 时为 null）
            String inner = resolveExecuteTarget(parse);
            String effective = inner != null ? inner : cmd;
            // 模组管理指令任何模式都放行（其自身 requires 控制 OP 权限）
            if (TpGuard.MOD_COMMANDS.contains(effective)) {
                return false;
            }
            // 通过 execute 执行的传送（跨维度定点传送）：所有模式一律放行
            if (inner != null && TpGuard.VANILLA_TP.contains(inner)) {
                return false;
            }
            // 非 OP 玩家直接执行的原版 /tp：模式1/3 对玩家开放；模式0/2 玩家改用 tpa 工具
            if ((MyrinTPMod.CONFIG.mode == 0 || MyrinTPMod.CONFIG.mode == 2)
                    && TpGuard.VANILLA_TP.contains(effective)
                    && !guard.isOp(src)) {
                src.sendFailure(new TextComponent("你没有权限使用此命令"));
                return true;
            }
            if (guard.allows(src, effective)) {
                return false;
            }
            src.sendFailure(new TextComponent("你没有权限执行此指令！请联系服务器管理员处理！"));
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

    /**
     * 解析 execute ... run &lt;命令&gt; 中真正执行的命令名：取最后一个 run 节点之后的节点。
     * 支持嵌套（execute run execute run tp 取到 tp）；不是 execute 或解析不到内层时返回 null。
     */
    private static String resolveExecuteTarget(ParseResults<CommandSourceStack> parse) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = parse.getContext().getNodes();
        if (nodes.isEmpty() || !TpGuard.EXECUTE_COMMAND.equals(nodes.get(0).getNode().getName())) {
            return null;
        }
        int lastRun = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if ("run".equals(nodes.get(i).getNode().getName())) {
                lastRun = i;
            }
        }
        if (lastRun >= 0 && lastRun + 1 < nodes.size()) {
            return nodes.get(lastRun + 1).getNode().getName();
        }
        return null;
    }
}
