package com.myrin.tp;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务器：0=查询配置与权限，1=提交新配置。 */
public record ConfigSyncPacket(int type, String json) implements CustomPacketPayload {

    public static final Type<ConfigSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("myrintp", "config_sync"));

    public static final StreamCodec<ByteBuf, ConfigSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConfigSyncPacket::type,
            ByteBufCodecs.STRING_UTF8, ConfigSyncPacket::json,
            ConfigSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
