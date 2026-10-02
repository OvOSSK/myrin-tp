package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/**
 * Myrin TP（26.3 Fabric）：传送指令 + 指令守卫。
 */
public final class MyrinTPMod implements ModInitializer {

    public static final String MODID = "myrintp";

    public static Path CONFIG_DIR;
    public static Config CONFIG;
    public static DataStore DATA;
    public static TpManager TP;
    public static TpGuard GUARD;

    private static CommandDispatcher<CommandSourceStack> dispatcher;

    @Override
    public void onInitialize() {
        CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("myrintp");
        CONFIG = Config.load(CONFIG_DIR.resolve("config.json"));
        DATA = DataStore.load(CONFIG_DIR.resolve("data.json"));
        GUARD = new TpGuard(CONFIG);
        SelectorUnlock.applied();

        CommandRegistrationCallback.EVENT.register((d, registryAccess, environment) -> {
            dispatcher = d;
            MyrinCommands.register(d);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TP = new TpManager(server, CONFIG, DATA);
            GuardNodes.apply(dispatcher, GUARD);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (TP != null) {
                TP.shutdown();
            }
            DATA.save(CONFIG_DIR.resolve("data.json"));
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player && TP != null) {
                TP.onPlayerDeath(player);
            }
        });
    }
}
