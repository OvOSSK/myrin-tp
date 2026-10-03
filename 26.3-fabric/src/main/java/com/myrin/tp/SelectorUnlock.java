package com.myrin.tp;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 放开目标选择器权限。
 * 服务端把 commands/entity_selectors 权限常量降为 ALL（普通玩家带选择器执行 /tp 等也能通过），
 * 客户端把恒返回 false 的 NO_PERMISSIONS 换成 ALL_PERMISSIONS（客户端解析与补全不拦截）。
 * 其余权限不受影响；失败则保持原版限制。
 * 注意：Fabric 重映射后字段名是中间名，反射按名字查找会失败，这里按字段值识别。
 */
public final class SelectorUnlock {

    private SelectorUnlock() {
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

    /** 服务端与客户端各调整一处选择器权限，调用一次即可。 */
    public static void init() {
        try {
            if (UNSAFE == null) {
                throw new IllegalStateException("Unsafe 不可用");
            }
            // 服务端：把 commands/entity_selectors 权限常量换成无等级要求的 ALL
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
            // 客户端：把恒返回 false 的 NO_PERMISSIONS 换成 ALL_PERMISSIONS
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
                            break;
                        }
                    } catch (Exception ignored) {
                        // 静态字段可能尚未完成初始化，跳过该字段
                    }
                }
            }
            if (!foundSelector) {
                System.err.println("[myrintp] 未找到选择器权限字段，服务端选择器保持原版限制");
            }
        } catch (Exception e) {
            System.err.println("[myrintp] 选择器权限放开失败，保持原版限制：" + e);
        }
    }
}
