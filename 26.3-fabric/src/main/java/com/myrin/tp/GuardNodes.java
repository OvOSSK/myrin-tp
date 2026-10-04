package com.myrin.tp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * 指令树包装：
 * - 原版 tp/teleport 默认要求 OP 权限，这里放开给所有玩家，让生存无作弊也能用；
 * - 其余指令保留原版补全，执行时按管控规则拦截并提示（不删指令补全）。
 * 递归包装整棵子树时把顶层指令名沿路径传入闭包，执行拦截不依赖运行时解析节点。
 */
public final class GuardNodes {

    private GuardNodes() {
    }

    private static boolean enabled = true;

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher, TpGuard guard) {
        if (!enabled) {
            return;
        }
        try {
            Field reqField = CommandNode.class.getDeclaredField("requirement");
            reqField.setAccessible(true);
            for (CommandNode<CommandSourceStack> node : new ArrayList<>(dispatcher.getRoot().getChildren())) {
                String name = node.getName();
                if (TpGuard.MOD_COMMANDS.contains(name)) {
                    continue;
                }
                if (TpGuard.TP_COMMANDS.contains(name)) {
                    reqField.set(node, (Predicate<CommandSourceStack>) s -> true);
                }
                wrapTree(node, guard, name);
            }
        } catch (Exception e) {
            enabled = false;
            System.err.println("[myrintp] TpGuard 指令限制启用失败（Brigadier 结构不兼容），已降级为不限制：" + e);
        }
    }

    /** 递归包装整棵子树：有执行逻辑的节点都包一层，按传入的顶层指令名判定。 */
    private static void wrapTree(CommandNode<CommandSourceStack> node, TpGuard guard, String top) {
        try {
            Command<CommandSourceStack> original = node.getCommand();
            if (original != null) {
                Field cmdField = CommandNode.class.getDeclaredField("command");
                cmdField.setAccessible(true);
                cmdField.set(node, (Command<CommandSourceStack>) ctx -> {
                    if (TpGuard.MOD_COMMANDS.contains(top) || TpGuard.TP_COMMANDS.contains(top) || guard.allows(ctx.getSource(), top)) {
                        return original.run(ctx);
                    }
                    ctx.getSource().sendFailure(Component.literal("该指令已被管理员禁止！仅允许使用 TP 类指令"));
                    return 0;
                });
            }
        } catch (Exception ignored) {
            // 单个节点包装失败不影响其他节点
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            wrapTree(child, guard, top);
        }
    }
}
