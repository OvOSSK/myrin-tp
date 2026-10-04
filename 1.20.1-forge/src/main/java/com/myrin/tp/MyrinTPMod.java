package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * Myrin TP（1.20.1 Forge）：传送指令 + 指令管控。
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
        Network.register();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MyrinConfigClient::register);
    }

    @Mod.EventBusSubscriber(modid = MODID)
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
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ConfigStatusPacket(op, CONFIG.toJsonString()));
    }
}

