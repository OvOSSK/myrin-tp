package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/**
 * Myrin TP（26.3 Fabric）：传送指令 + 指令管控。
 */
public final class MyrinTPMod implements ModInitializer {

    public static final String MODID = "myrintp";

    public static Path CONFIG_DIR;
    public static Path CONFIG_FILE;
    public static Path DATA_FILE;
    public static Config CONFIG;
    public static DataStore DATA;
    public static TpManager TP;
    public static TpGuard GUARD;

    private static CommandDispatcher<CommandSourceStack> dispatcher;

    @Override
    public void onInitialize() {
        CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("myrintp");
        CONFIG_FILE = CONFIG_DIR.resolve("config.json");
        DATA_FILE = CONFIG_DIR.resolve("data.json");
        CONFIG = Config.load(CONFIG_FILE);
        DATA = DataStore.load(DATA_FILE);
        GUARD = new TpGuard(CONFIG);
        Network.register();
        SelectorUnlock.init();

        CommandRegistrationCallback.EVENT.register((d, registryAccess, environment) -> {
            dispatcher = d;
            MyrinCommands.register(d);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TP = new TpManager(server, CONFIG, DATA);
            GuardNodes.apply(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (TP != null) {
                TP.shutdown();
            }
            DATA.save(DATA_FILE);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player && TP != null) {
                TP.onPlayerDeath(player);
            }
        });
    }

    /** 服务端：处理客户端配置同步请求（0=查询，1=提交）。OP 判定以服务端为准（26.3 权限集）。 */
    public static void onConfigSync(ServerPlayer player, int type, String json) {
        boolean op = player.level().getServer() != null && player.level().getServer().getPlayerList().isOp(player.nameAndId());
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
        for (ServerPlayer p : player.level().getServer().getPlayerList().getPlayers()) {
            sendConfigStatus(p, p.level().getServer() != null && p.level().getServer().getPlayerList().isOp(p.nameAndId()));
        }
    }

    private static void sendConfigStatus(ServerPlayer player, boolean op) {
        Network.sendToPlayer(player, new ConfigStatusPacket(op, CONFIG.toJsonString()));
    }
}

