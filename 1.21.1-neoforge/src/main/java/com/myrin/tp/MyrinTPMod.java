package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;

/**
 * Myrin TP（1.21.1 NeoForge）：传送指令 + 指令管控。
 */
@Mod(MyrinTPMod.MODID)
public final class MyrinTPMod {

    public static final String MODID = "myrintp";

    public static Path CONFIG_DIR;
    public static Path CONFIG_FILE;
    public static Path DATA_FILE;
    public static Config CONFIG;
    public static DataStore DATA;
    public static TpManager TP;
    public static TpGuard GUARD;

    private static CommandDispatcher<CommandSourceStack> dispatcher;

    public MyrinTPMod() {
        CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("myrintp");
        CONFIG_FILE = CONFIG_DIR.resolve("config.json");
        DATA_FILE = CONFIG_DIR.resolve("data.json");
        CONFIG = Config.load(CONFIG_FILE);
        DATA = DataStore.load(DATA_FILE);
        GUARD = new TpGuard(CONFIG);
        if (FMLLoader.getDist() == Dist.CLIENT) {
            MyrinConfigClient.register();
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
    public static final class Events {

        @SubscribeEvent
        public static void onCommand(CommandEvent event) {
            if (CommandBlocker.shouldCancel(event.getParseResults(), GUARD)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            dispatcher = event.getDispatcher();
            MyrinCommands.register(dispatcher);
        }

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            TP = new TpManager(event.getServer(), CONFIG, DATA);
            GuardNodes.apply(dispatcher);
            SelectorPerm.inject();
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            if (TP != null) {
                TP.shutdown();
            }
            DATA.save(DATA_FILE);
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && TP != null) {
                TP.onPlayerDeath(player);
            }
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    onConfigSync(player, pkt.kind(), pkt.json());
                }
            });
        });
        registrar.playToClient(ConfigStatusPacket.TYPE, ConfigStatusPacket.STREAM_CODEC, (pkt, ctx) -> {
            ctx.enqueueWork(() -> {
                MyrinConfigScreen screen = MyrinConfigScreen.active;
                if (screen != null) {
                    screen.onConfigStatus(pkt.canEdit(), pkt.json());
                }
            });
        });
        }
    }

    /** 服务端：处理客户端配置同步请求（0=查询，1=提交）。OP 判定以服务端为准。 */
    public static void onConfigSync(ServerPlayer player, int type, String json) {
        boolean op = player.hasPermissions(2) || player.hasPermissions(3) || player.hasPermissions(4);
        if (type == 0) {
            sendConfigStatus(player, op);
            return;
        }
        if (type != 1) {
            return;
        }
        if (!op) {
            player.sendSystemMessage(Component.literal("你没有权限执行此指令！请联系服务器管理员处理！"));
            sendConfigStatus(player, false);
            return;
        }
        Config n = Config.fromJson(json);
        CONFIG.copyFrom(n);
        CONFIG.save(CONFIG_FILE);
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            sendConfigStatus(p, p.hasPermissions(2) || p.hasPermissions(3) || p.hasPermissions(4));
        }
    }

    private static void sendConfigStatus(ServerPlayer player, boolean op) {
        PacketDistributor.sendToPlayer(player, new ConfigStatusPacket(op, CONFIG.toJsonString()));
    }
}

