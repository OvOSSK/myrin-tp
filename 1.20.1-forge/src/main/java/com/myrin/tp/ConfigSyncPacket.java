package com.myrin.tp;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端→服务器：0=查询配置与权限，1=提交新配置。 */
public class ConfigSyncPacket {

    private final int type;
    private final String json;

    public ConfigSyncPacket(int type, String json) {
        this.type = type;
        this.json = json == null ? "" : json;
    }

    public static void encode(ConfigSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.type);
        buf.writeUtf(pkt.json);
    }

    public static ConfigSyncPacket decode(FriendlyByteBuf buf) {
        return new ConfigSyncPacket(buf.readVarInt(), buf.readUtf(32767));
    }

    public static void handle(ConfigSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player != null) {
                MyrinTPMod.onConfigSync(player, pkt.type, pkt.json);
            }
        });
        c.setPacketHandled(true);
    }
}
