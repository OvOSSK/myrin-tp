package com.myrin.tp;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 服务器→客户端：配置与权限状态（canEdit + 服务器当前配置 JSON）。 */
public record ConfigStatusPacket(boolean canEdit, String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigStatusPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("myrintp", "config_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigStatusPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ConfigStatusPacket::canEdit,
            ByteBufCodecs.STRING_UTF8, ConfigStatusPacket::json,
            ConfigStatusPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
