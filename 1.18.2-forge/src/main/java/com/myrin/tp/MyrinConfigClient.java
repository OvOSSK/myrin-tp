package com.myrin.tp;

import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * 客户端侧扩展点注册（配置界面）。仅客户端加载，服务端不引用此类。
 * 1.18.2 用 ConfigGuiHandler（1.19 起才改名 ConfigScreenHandler）。
 */
public final class MyrinConfigClient {

    private MyrinConfigClient() {
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new MyrinConfigScreen(parent)));
    }
}
