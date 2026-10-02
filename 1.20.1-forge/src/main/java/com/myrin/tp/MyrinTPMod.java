package com.myrin.tp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * Myrin TP（1.20.1 Forge）：传送指令 + Only TP 指令限制。
 */
@Mod(MyrinTPMod.MODID)
public final class MyrinTPMod {

    public static final String MODID = "myrintp";

    public static Path CONFIG_DIR;
    public static Config CONFIG;
    public static DataStore DATA;
    public static TpManager TP;
    public static OnlyTP ONLY_TP;

    private static CommandDispatcher<CommandSourceStack> dispatcher;

    public MyrinTPMod() {
        CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("myrintp");
        CONFIG = Config.load(CONFIG_DIR.resolve("config.json"));
        DATA = DataStore.load(CONFIG_DIR.resolve("data.json"));
        ONLY_TP = new OnlyTP(CONFIG);
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
            OnlyTPNodeGuard.apply(dispatcher, ONLY_TP);
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
            if (event.isCanceled() || ONLY_TP == null) {
                return;
            }
            ParseResults<CommandSourceStack> results = event.getParseResults();
            if (results == null || results.getContext() == null) {
                return;
            }
            java.util.List<ParsedCommandNode<CommandSourceStack>> nodes = results.getContext().getNodes();
            if (nodes.isEmpty()) {
                return;
            }
            String name = nodes.get(0).getNode().getName();
            CommandSourceStack src = results.getContext().getSource();
            if (!ONLY_TP.allows(src, name)) {
                event.setCanceled(true);
                if (src.getEntity() instanceof ServerPlayer p) {
                    p.displayClientMessage(Component.literal("指令 /" + name + " 已被 Only TP 限制，仅可使用 TP 类指令。")
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
