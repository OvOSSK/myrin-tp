package com.myrin.tp;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 放开目标选择器权限。
 * 原版 @e/@a/@p/@r 这类选择器要求 GAMEMASTERS 级（commands/entity_selectors 权限），
 * 本类做两处调整：服务端把该权限常量降为 ALL（普通玩家带选择器执行 /tp 等也能通过），
 * 客户端把恒返回 false 的 NO_PERMISSIONS 换成 ALL_PERMISSIONS（客户端解析与补全不拦截）。
 * 服务端其余命令权限不受影响；失败则保持原版限制。
 *
 * 注意：Fabric 重映射后字段名是中间名，反射 getDeclaredField("字段名") 的字符串不会被重映射，
 * 因此这里按字段值识别目标字段，而不是按名字查找。
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

    private static void writeStatic(Field f, Object value) throws Exception {
        f.setAccessible(true);
        UNSAFE.putObject(UNSAFE.staticFieldBase(f), UNSAFE.staticFieldOffset(f), value);
    }

    static {
        try {
            if (UNSAFE == null) {
                throw new IllegalStateException("Unsafe 不可用");
            }
            // 1) 服务端：找到命令选择器权限字段（值形如 Atom(id=...commands/entity_selectors)），
            //    把常量换成无等级要求的 ALL，让 LevelBasedPermissionSet 对普通玩家放行选择器。
            boolean foundSelector = false;
            for (Field f : Permissions.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) || !Permission.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                f.setAccessible(true);
                Object value = f.get(null);
                if (value instanceof Permission.Atom atom) {
                    String id = atom.id().toString();
                    if (id.contains("entity_selectors")) {
                        writeStatic(f, new Permission.HasCommandLevel(PermissionLevel.ALL));
                        foundSelector = true;
                        break;
                    }
                }
            }
            // 2) 客户端：把恒返回 false 的 NO_PERMISSIONS 换成 ALL_PERMISSIONS。
            //    按行为识别：对任意权限都返回 false 的静态集合就是 NO_PERMISSIONS。
            boolean foundNoPerms = false;
            for (Field f : PermissionSet.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) || !PermissionSet.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                f.setAccessible(true);
                Object value = f.get(null);
                if (value instanceof PermissionSet set) {
                    try {
                        if (!set.hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)
                                && !set.hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                            writeStatic(f, PermissionSet.ALL_PERMISSIONS);
                            foundNoPerms = true;
                            break;
                        }
                    } catch (Exception ignored) {
                        // 静态字段可能尚未完成初始化，跳过该字段
                    }
                }
            }
            applied = foundSelector || foundNoPerms;
            if (!foundSelector) {
                System.err.println("[myrintp] 未找到选择器权限字段，服务端选择器保持原版限制");
            }
        } catch (Exception e) {
            System.err.println("[myrintp] 选择器权限放开失败，保持原版限制：" + e);
        }
    }
}
