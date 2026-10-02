package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * 指令树包装：
 * - 原版 tp/teleport 默认要求 OP 权限，这里放开给所有玩家，让生存无作弊也能用；
 * - tp/teleport 的执行器包一层临时提权：执行期间把源权限提到 2 级，让目标选择器
 *   不再被原版权限检查拦下，结束后恢复。不改原版执行逻辑，只对该命令生效；
 * - 其余指令按守卫规则收窄 requires。
 * Brigadier 没提供公开的 requires 写入方法，用反射改字段；反射失败降级成不限制，不让服务端崩。
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
            unlockSelectors(dispatcher);
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            for (CommandNode<CommandSourceStack> node : new ArrayList<>(dispatcher.getRoot().getChildren())) {
                String name = node.getName();
                if (TpGuard.MOD_COMMANDS.contains(name)) {
                    continue;
                }
                if (TpGuard.TP_COMMANDS.contains(name)) {
                    field.set(node, (Predicate<CommandSourceStack>) s -> true);
                    continue;
                }
                if (guard.isWhitelisted(name)) {
                    continue;
                }
                Predicate<CommandSourceStack> base = node.getRequirement();
                field.set(node, base.and(src -> guard.allows(src, name)));
            }
        } catch (Exception e) {
            enabled = false;
            System.err.println("[myrintp] TpGuard 指令限制启用失败（Brigadier 结构不兼容），已降级为不限制：" + e);
        }
    }

    private static void unlockSelectors(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (CommandNode<CommandSourceStack> node : new ArrayList<>(dispatcher.getRoot().getChildren())) {
            if (node.getName().equals("tp") || node.getName().equals("teleport")) {
                wrapExecutes(node);
            }
        }
    }

    private static void wrapExecutes(CommandNode<CommandSourceStack> node) {
        Command<CommandSourceStack> cmd = node.getCommand();
        if (cmd != null && !(cmd instanceof TempPermCommand)) {
            try {
                Field cf = CommandNode.class.getDeclaredField("command");
                cf.setAccessible(true);
                cf.set(node, new TempPermCommand(cmd));
            } catch (Exception e) {
                System.err.println("[myrintp] tp 选择器权限包装失败：" + e);
            }
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            wrapExecutes(child);
        }
    }

    /**
     * 临时把命令源的权限等级提到 2，执行原逻辑后恢复。
     */
    private static final class TempPermCommand implements Command<CommandSourceStack> {

        private static final Field PERM = permField();

        private final Command<CommandSourceStack> delegate;

        TempPermCommand(Command<CommandSourceStack> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            CommandSourceStack src = ctx.getSource();
            int old = readPerm(src);
            writePerm(src, 2);
            try {
                return delegate.run(ctx);
            } finally {
                writePerm(src, old);
            }
        }

        private static Field permField() {
            try {
                Field f = CommandSourceStack.class.getDeclaredField("permissionLevel");
                f.setAccessible(true);
                return f;
            } catch (Exception e) {
                System.err.println("[myrintp] 权限字段不可用，选择器保持原版限制：" + e);
                return null;
            }
        }

        private static int readPerm(CommandSourceStack src) {
            try {
                return PERM.getInt(src);
            } catch (Exception e) {
                return 0;
            }
        }

        private static void writePerm(CommandSourceStack src, int level) {
            try {
                PERM.setInt(src, level);
            } catch (Exception e) {
                // 静默：失败则保持原权限，不影响命令本身
            }
        }
    }
}
