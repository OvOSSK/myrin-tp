package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * 指令树权限放开：
 * - 原版 tp/teleport 默认要求 OP 权限，这里放开给所有玩家，让生存无作弊也能看到补全并使用；
 * - 管控拦截不在这里做（会删补全），改由全局命令执行点 CommandBlocker 按模式判定。
 * Brigadier 没提供公开的 requirement 写入方法，用反射；反射失败降级成不限制，不让服务端崩。
 */
public final class GuardNodes {

    private GuardNodes() {
    }

    private static boolean enabled = true;

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
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
            }
        } catch (Exception e) {
            enabled = false;
            System.err.println("[myrintp] TP 指令权限放开失败（Brigadier 结构不兼容），已降级：" + e);
        }
    }
}
