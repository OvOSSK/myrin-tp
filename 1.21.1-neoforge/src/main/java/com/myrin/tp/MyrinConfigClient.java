package com.myrin.tp;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端侧扩展点注册（配置界面）。仅客户端加载，服务端不引用此类。
 */
public final class MyrinConfigClient {

    private MyrinConfigClient() {
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (container, parent) -> new MyrinConfigScreen(parent));
    }
}
