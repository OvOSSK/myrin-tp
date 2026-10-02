package com.myrin.tp;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;

import java.lang.reflect.Field;

/**
 * 放开目标选择器权限。
 * 原版 @e/@a/@p/@r 这类选择器要求 OP 2（COMMANDS_ENTITY_SELECTORS），
 * 这里把该权限常量降到 ALL，让普通玩家也能带选择器使用指令（如 /tp @p 100 64 100）。
 * 只改这一个判断点，不影响其它指令权限；失败则保持原版限制。
 */
public final class SelectorUnlock {

    private static boolean applied = false;

    private SelectorUnlock() {
    }

    public static boolean applied() {
        return applied;
    }

    private static final sun.misc.Unsafe UNSAFE = unsafe();

    private static sun.misc.Unsafe unsafe() {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    static {
        try {
            if (UNSAFE == null) {
                throw new IllegalStateException("Unsafe 不可用");
            }
            Field f = Permissions.class.getDeclaredField("COMMANDS_ENTITY_SELECTORS");
            f.setAccessible(true);
            UNSAFE.putObject(UNSAFE.staticFieldBase(f), UNSAFE.staticFieldOffset(f),
                    new Permission.HasCommandLevel(PermissionLevel.ALL));
            applied = true;
        } catch (Exception e) {
            System.err.println("[myrintp] 选择器权限放开失败，保持原版限制：" + e);
        }
    }
}
