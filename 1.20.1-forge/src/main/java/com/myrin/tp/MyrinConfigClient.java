package com.myrin.tp;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * 客户端侧扩展点注册（配置界面）。仅客户端加载，服务端不引用此类。
 */
public final class MyrinConfigClient {

    private MyrinConfigClient() {
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new MyrinConfigScreen(parent)));
    }
}
