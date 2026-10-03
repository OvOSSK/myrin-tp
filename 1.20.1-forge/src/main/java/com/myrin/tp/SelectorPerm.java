package com.myrin.tp;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * 选择器权限点内置放行：对选择器权限点恒返回 true，其余权限节点保持原样。
 */
public final class SelectorPerm {

    private static final String SELECTOR_NODE = "use_entity_selectors";
    private static boolean injected = false;

    private SelectorPerm() {
    }

    /** 包一层权限处理器，仅对选择器节点恒放行。 */
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
            System.out.println("[myrintp] 选择器权限已内置放行（forge:use_entity_selectors）");
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
