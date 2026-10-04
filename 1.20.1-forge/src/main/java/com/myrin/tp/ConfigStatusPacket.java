package com.myrin.tp;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务器→客户端：配置与权限状态（canEdit + 服务器当前配置 JSON）。 */
public class ConfigStatusPacket {

    private final boolean canEdit;
    private final String json;

    public ConfigStatusPacket(boolean canEdit, String json) {
        this.canEdit = canEdit;
        this.json = json == null ? "" : json;
    }

    public static void encode(ConfigStatusPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.canEdit);
        buf.writeUtf(pkt.json);
    }

    public static ConfigStatusPacket decode(FriendlyByteBuf buf) {
        return new ConfigStatusPacket(buf.readBoolean(), buf.readUtf(32767));
    }

    public static void handle(ConfigStatusPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MyrinConfigScreen screen = MyrinConfigScreen.active;
            if (screen != null) {
                screen.onConfigStatus(pkt.canEdit, pkt.json);
            }
        }));
        c.setPacketHandled(true);
    }
}
