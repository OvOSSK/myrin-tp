package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * 指令树包装：
 * - 原版 tp/teleport 默认要求 OP 权限，这里放开给所有玩家，让生存无作弊也能用；
 * - 其余指令按 Only TP 规则收窄 requires。
 * Brigadier 没提供公开的 requires 写入方法，用反射改字段；反射失败降级成不限制，不让服务端崩。
 */
public final class OnlyTPNodeGuard {

    private OnlyTPNodeGuard() {
    }

    private static boolean enabled = true;

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher, OnlyTP onlyTP) {
        if (!enabled) {
            return;
        }
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            for (CommandNode<CommandSourceStack> node : new ArrayList<>(dispatcher.getRoot().getChildren())) {
                String name = node.getName();
                if (OnlyTP.MOD_COMMANDS.contains(name)) {
                    continue;
                }
                if (OnlyTP.TP_COMMANDS.contains(name)) {
                    field.set(node, (Predicate<CommandSourceStack>) s -> true);
                    continue;
                }
                if (onlyTP.isWhitelisted(name)) {
                    continue;
                }
                Predicate<CommandSourceStack> base = node.getRequirement();
                field.set(node, base.and(src -> onlyTP.allows(src, name)));
            }
        } catch (Exception e) {
            enabled = false;
            System.err.println("[myrintp] OnlyTP 指令限制启用失败（Brigadier 结构不兼容），已降级为不限制：" + e);
        }
    }
}
