package com.myrin.tp;

import net.fabricmc.fabric.api.networking.v1.CustomPacketPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.resources.ResourceLocation;

/** 服务器→客户端：配置与权限状态（canEdit + 服务器当前配置 JSON）。 */
public record ConfigStatusPacket(boolean canEdit, String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Id<ConfigStatusPacket> ID = new CustomPacketPayload.Id<>(ResourceLocation.fromNamespaceAndPath("myrintp", "config_status"));

    public static final PacketCodec<RegistryByteBuf, ConfigStatusPacket> CODEC = PacketCodec.of(ConfigStatusPacket::write, ConfigStatusPacket::read);

    @Override
    public CustomPacketPayload.Id<? extends CustomPacketPayload> id() {
        return ID;
    }

    private static void write(ConfigStatusPacket pkt, RegistryByteBuf buf) {
        buf.writeBoolean(pkt.canEdit);
        buf.writeUtf(pkt.json);
    }

    private static ConfigStatusPacket read(RegistryByteBuf buf) {
        return new ConfigStatusPacket(buf.readBoolean(), buf.readUtf(32767));
    }
}
