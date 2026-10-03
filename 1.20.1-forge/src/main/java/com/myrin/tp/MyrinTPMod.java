package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * Myrin TP（1.20.1 Forge）：传送指令 + 指令守卫。
 */
@Mod(MyrinTPMod.MODID)
public final class MyrinTPMod {

    public static final String MODID = "myrintp";

    public static Path CONFIG_DIR;
    public static Config CONFIG;
    public static DataStore DATA;
    public static TpManager TP;
    public static TpGuard GUARD;

    private static CommandDispatcher<CommandSourceStack> dispatcher;

    public MyrinTPMod() {
        CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("myrintp");
        CONFIG = Config.load(CONFIG_DIR.resolve("config.json"));
        DATA = DataStore.load(CONFIG_DIR.resolve("data.json"));
        GUARD = new TpGuard(CONFIG);
        if (net.minecraftforge.fml.loading.FMLLoader.getDist() == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new MyrinConfigScreen(parent)));
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static final class Events {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            dispatcher = event.getDispatcher();
            MyrinCommands.register(dispatcher);
        }

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            TP = new TpManager(event.getServer(), CONFIG, DATA);
            GuardNodes.apply(dispatcher, GUARD);
            SelectorPerm.inject();
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            if (TP != null) {
                TP.shutdown();
            }
            DATA.save(CONFIG_DIR.resolve("data.json"));
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && TP != null) {
                TP.onPlayerDeath(player);
            }
        }
    }
}
