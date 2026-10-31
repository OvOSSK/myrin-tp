package com.myrin.tp;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端侧扩展点注册（配置界面 + 打开按键）。
 */
public final class MyrinConfigClient {

    public static final String KEY_CATEGORY = "key.categories.myrintp";
    public static final String KEY_OPEN = "key.myrintp.open_config";

    public static KeyMapping OPEN_KEY;

    private MyrinConfigClient() {
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new MyrinConfigScreen(parent)));

        OPEN_KEY = new KeyMapping(KEY_OPEN, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY);
        ClientRegistry.registerKeyBinding(OPEN_KEY);
        MinecraftForge.EVENT_BUS.addListener(MyrinConfigClient::onKey);
    }

    private static void onKey(InputEvent.KeyInputEvent event) {
        if (OPEN_KEY != null && OPEN_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.execute(() -> mc.setScreen(new MyrinConfigScreen(null)));
            }
        }
    }
}
