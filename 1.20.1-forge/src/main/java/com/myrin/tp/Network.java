package com.myrin.tp;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** 网络通道注册（Forge 1.20.1 SimpleChannel）。 */
public final class Network {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("myrintp", "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private Network() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ConfigSyncPacket.class,
                ConfigSyncPacket::encode, ConfigSyncPacket::decode, ConfigSyncPacket::handle,
                NetworkDirection.PLAY_TO_SERVER);
        CHANNEL.registerMessage(1, ConfigStatusPacket.class,
                ConfigStatusPacket::encode, ConfigStatusPacket::decode, ConfigStatusPacket::handle,
                NetworkDirection.PLAY_TO_CLIENT);
    }
}
