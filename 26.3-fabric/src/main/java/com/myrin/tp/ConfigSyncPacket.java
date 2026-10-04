package com.myrin.tp;

import net.fabricmc.fabric.api.networking.v1.CustomPacketPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务器：0=查询配置与权限，1=提交新配置。 */
public record ConfigSyncPacket(int type, String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Id<ConfigSyncPacket> ID = new CustomPacketPayload.Id<>(ResourceLocation.fromNamespaceAndPath("myrintp", "config_sync"));

    public static final PacketCodec<RegistryByteBuf, ConfigSyncPacket> CODEC = PacketCodec.of(ConfigSyncPacket::write, ConfigSyncPacket::read);

    @Override
    public CustomPacketPayload.Id<? extends CustomPacketPayload> id() {
        return ID;
    }

    private static void write(ConfigSyncPacket pkt, RegistryByteBuf buf) {
        buf.writeVarInt(pkt.type);
        buf.writeUtf(pkt.json);
    }

    private static ConfigSyncPacket read(RegistryByteBuf buf) {
        return new ConfigSyncPacket(buf.readVarInt(), buf.readUtf(32767));
    }
}
