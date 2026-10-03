package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;
import java.util.List;

/**
 * Myrin TP（1.21.1 NeoForge）：传送指令 + 指令守卫。
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
        if (FMLLoader.getDist() == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> new MyrinConfigScreen(parent));
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
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
        public static void onCommand(CommandEvent event) {
            if (event.isCanceled() || GUARD == null) {
                return;
            }
            ParseResults<CommandSourceStack> results = event.getParseResults();
            if (results == null || results.getContext() == null) {
                return;
            }
            List<ParsedCommandNode<CommandSourceStack>> nodes = results.getContext().getNodes();
            if (nodes.isEmpty()) {
                return;
            }
            String name = nodes.get(0).getNode().getName();
            CommandSourceStack src = results.getContext().getSource();
            if (!GUARD.allows(src, name)) {
                event.setCanceled(true);
                if (src.getEntity() instanceof ServerPlayer p) {
                    p.displayClientMessage(Component.literal("指令 /" + name + " 已被指令守卫拦截，仅可使用 TP 类传送指令。")
                            .withStyle(ChatFormatting.RED), false);
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && TP != null) {
                TP.onPlayerDeath(player);
            }
        }
    }
}
