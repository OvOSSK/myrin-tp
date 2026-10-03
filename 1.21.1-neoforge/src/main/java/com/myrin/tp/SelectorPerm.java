package com.myrin.tp;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * 选择器权限点内置放行。
 * 原版与 NeoForge 要求 OP2（GAMEMASTERS）才能使用 @s/@p/@a 等目标选择器，
 * 而选择器权限的检查发生在命令解析阶段，普通玩家在生存无作弊下会被直接拦下。
 * 本类把 neoforge:use_entity_selectors 这个权限点的查询结果固定为放行，
 * 让解析阶段的选择器检查直接通过，其余权限节点全部保持原样，不产生任何越权。
 */
public final class SelectorPerm {

    private static final String SELECTOR_NODE = "use_entity_selectors";
    private static boolean injected = false;

    private SelectorPerm() {
    }

    /**
     * 服务端启动完成后调用一次：把当前权限处理器包一层，仅对选择器节点恒放行。
     * 通过反射替换 PermissionAPI 的活动处理器，不修改任何配置文件。
     */
    public static synchronized void inject() {
        if (injected) {
            return;
        }
        try {
            Field f = PermissionAPI.class.getDeclaredField("activeHandler");
            f.setAccessible(true);
            IPermissionHandler current = (IPermissionHandler) f.get(null);
            f.set(null, new SelectorDelegate(current));
            injected = true;
            System.out.println("[myrintp] 选择器权限已内置放行（neoforge:use_entity_selectors）");
        } catch (Exception e) {
            System.err.println("[myrintp] 选择器权限注入失败，保持原版限制：" + e);
        }
    }

    private static final class SelectorDelegate implements IPermissionHandler {

        private final IPermissionHandler base;

        SelectorDelegate(IPermissionHandler base) {
            this.base = base;
        }

        @Override
        public ResourceLocation getIdentifier() {
            return new ResourceLocation("myrintp", "selector_permit");
        }

        @Override
        public Set<PermissionNode<?>> getRegisteredNodes() {
            return base == null ? Collections.emptySet() : base.getRegisteredNodes();
        }

        @Override
        public <T> T getPermission(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
            if (isSelectorNode(node)) {
                return (T) Boolean.TRUE;
            }
            if (base != null) {
                return base.getPermission(player, node, context);
            }
            return node.getDefaultResolver().resolve(player, player.getUUID(), context);
        }

        @Override
        public <T> T getOfflinePermission(UUID player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
            if (isSelectorNode(node)) {
                return (T) Boolean.TRUE;
            }
            if (base != null) {
                return base.getOfflinePermission(player, node, context);
            }
            return node.getDefaultResolver().resolve(null, player, context);
        }

        private static boolean isSelectorNode(PermissionNode<?> node) {
            return node.getNodeName().endsWith(":" + SELECTOR_NODE);
        }
    }
}
