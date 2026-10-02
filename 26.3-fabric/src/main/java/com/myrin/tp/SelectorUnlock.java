package com.myrin.tp;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

import java.lang.reflect.Field;

/**
 * 放开目标选择器权限。
 * 原版 @e/@a/@p/@r 这类选择器要求 GAMEMASTERS 级（COMMANDS_ENTITY_SELECTORS），
 * 本类做两处调整：服务端把该权限常量降到 ALL（普通玩家带选择器执行 /tp 等），
 * 客户端把恒返回的 NO_PERMISSIONS 换成 ALL_PERMISSIONS（客户端即时解析与补全不拦截）。
 * 服务端其余命令权限不受影响；失败则保持原版限制。
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
            Field f2 = PermissionSet.class.getDeclaredField("NO_PERMISSIONS");
            f2.setAccessible(true);
            UNSAFE.putObject(UNSAFE.staticFieldBase(f2), UNSAFE.staticFieldOffset(f2),
                    PermissionSet.ALL_PERMISSIONS);
            applied = true;
        } catch (Exception e) {
            System.err.println("[myrintp] 选择器权限放开失败，保持原版限制：" + e);
        }
    }
}
