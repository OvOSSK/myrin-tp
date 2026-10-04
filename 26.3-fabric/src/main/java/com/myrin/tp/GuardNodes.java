package com.myrin.tp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
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
 * Brigadier 没提供公开的字段写入方法，用反射；反射失败降级成不限制，不让服务端崩。
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
                    continue;
                }
                if (guard.isWhitelisted(name)) {
                    continue;
                }
                wrapExecution(node, guard, name);
            }
        } catch (Exception e) {
            enabled = false;
            System.err.println("[myrintp] TpGuard 指令限制启用失败（Brigadier 结构不兼容），已降级为不限制：" + e);
        }
    }

    /** 包装指令执行：管控不通过时提示并拒绝，保留原版补全。 */
    private static void wrapExecution(CommandNode<CommandSourceStack> node, TpGuard guard, String name) {
        try {
            Command<CommandSourceStack> original = node.getCommand();
            if (original == null) {
                return;
            }
            Field cmdField = CommandNode.class.getDeclaredField("command");
            cmdField.setAccessible(true);
            cmdField.set(node, (Command<CommandSourceStack>) ctx -> {
                CommandSourceStack src = ctx.getSource();
                if (!guard.allows(src, name)) {
                    src.sendFailure(Component.literal("该指令已被管理员禁止！仅允许使用 TP 类指令"));
                    return 0;
                }
                return original.run(ctx);
            });
        } catch (Exception ignored) {
            // 单个节点包装失败不影响其他节点
        }
    }
}
