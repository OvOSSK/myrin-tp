package com.myrin.tp;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/** 网络注册（Fabric 26.3 networking v1）。 */
public final class Network {

    private Network() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ConfigSyncPacket.ID, ConfigSyncPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigStatusPacket.ID, ConfigStatusPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player != null) {
                context.server().execute(() -> MyrinTPMod.onConfigSync(player, payload.type(), payload.json()));
            }
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(ConfigStatusPacket.ID, (payload, context) -> {
                context.client().execute(() -> {
                    MyrinConfigScreen screen = MyrinConfigScreen.active;
                    if (screen != null) {
                        screen.onConfigStatus(payload.canEdit(), payload.json());
                    }
                });
            });
        }
    }

    /** 客户端→服务器发送（需客户端环境）。 */
    public static void sendToServer(ConfigSyncPacket pkt) {
        ClientPlayNetworking.send(pkt);
    }

    /** 服务器→指定玩家发送。 */
    public static void sendToPlayer(ServerPlayer player, ConfigStatusPacket pkt) {
        ServerPlayNetworking.send(player, pkt);
    }
}
