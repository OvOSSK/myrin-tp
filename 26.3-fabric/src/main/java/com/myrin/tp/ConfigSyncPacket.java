package com.myrin.tp;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 客户端→服务器：0=查询配置与权限，1=提交新配置。 */
public record ConfigSyncPacket(int kind, String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("myrintp", "config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConfigSyncPacket::kind,
            ByteBufCodecs.STRING_UTF8, ConfigSyncPacket::json,
            ConfigSyncPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
