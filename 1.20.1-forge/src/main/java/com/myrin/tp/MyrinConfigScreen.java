package com.myrin.tp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 配置界面：参数直接平铺（不使用滚动列表），滑块可鼠标拖动，
 * 所有行完整显示，纯色背景，适配原版与钠界面缩放。
 */
public class MyrinConfigScreen extends Screen {
    private static final String[] MODE_LABELS = {
            "关闭模组功能", "非OP玩家仅允许TP指令", "OP玩家禁止非TP指令", "同时启用"
    };

    private final Screen parent;
    private final Config cfg;
    private int mode;
    private int tpaCooldown;
    private int tprTimeout;
    private int homeCooldown;
    private int backCooldown;
    private int tprCooldown;
    private int delay;
    private int tprRange;
    private int maxHomes;
    private boolean cancelOnMove;
    private boolean deathBack;

    private final List<Button> modeButtons = new ArrayList<>();
    private Button toggleMove;
    private Button toggleBack;

    public MyrinConfigScreen(Screen parent) {
        super(Component.literal("Myrin TP 设置"));
        this.parent = parent;
        this.cfg = MyrinTPMod.CONFIG;
        this.mode = cfg.mode;
        this.tpaCooldown = cfg.tpaRequestCooldownSeconds;
        this.tprTimeout = cfg.tpaRequestTimeoutSeconds;
        this.homeCooldown = cfg.homeCooldownSeconds;
        this.backCooldown = cfg.backCooldownSeconds;
        this.tprCooldown = cfg.tprCooldownSeconds;
        this.delay = cfg.teleportDelayTicks;
        this.tprRange = cfg.tprRange;
        this.maxHomes = cfg.maxHomes;
        this.cancelOnMove = cfg.cancelOnMove;
        this.deathBack = cfg.deathBack;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 170;
        modeButtons.clear();
        for (int i = 0; i < 4; i++) {
            final int m = i;
            Button b = Button.builder(modeLabel(i),
                    btn -> { mode = m; cfg.mode = m; cfg.save(MyrinTPMod.CONFIG_DIR.resolve("config.json")); refreshModeButtons(); })
                    .bounds(x + i * 122, 34, 110, 20).build();
            modeButtons.add(b);
            addRenderableWidget(b);
        }

        int y = 62;
        addSlider("tpa 请求冷却(秒)", tpaCooldown, 0, 3600, v -> tpaCooldown = v, x, y); y += 24;
        addSlider("tpa 请求超时(秒)", tprTimeout, 5, 300, v -> tprTimeout = v, x, y); y += 24;
        addSlider("回家冷却(秒)", homeCooldown, 0, 3600, v -> homeCooldown = v, x, y); y += 24;
        addSlider("返回冷却(秒)", backCooldown, 0, 3600, v -> backCooldown = v, x, y); y += 24;
        addSlider("随机传送冷却(秒)", tprCooldown, 0, 3600, v -> tprCooldown = v, x, y); y += 24;
        addSlider("传送倒计时(刻)", delay, 0, 200, v -> delay = v, x, y); y += 24;
        addSlider("随机传送范围", tprRange, 100, 200000, v -> tprRange = v, x, y); y += 24;
        addSlider("家点数量上限", maxHomes, 1, 100, v -> maxHomes = v, x, y); y += 24;

        toggleMove = Button.builder(Component.literal("倒计时期间移动取消: " + (cancelOnMove ? "开" : "关")),
                b -> { cancelOnMove = !cancelOnMove; b.setMessage(Component.literal("倒计时期间移动取消: " + (cancelOnMove ? "开" : "关"))); })
                .bounds(x, y, 340, 20).build();
        addRenderableWidget(toggleMove); y += 24;
        toggleBack = Button.builder(Component.literal("/back 优先回死亡点: " + (deathBack ? "开" : "关")),
                b -> { deathBack = !deathBack; b.setMessage(Component.literal("/back 优先回死亡点: " + (deathBack ? "开" : "关"))); })
                .bounds(x, y, 340, 20).build();
        addRenderableWidget(toggleBack);

        int by = this.height - 32;
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save()).bounds(x, by, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose()).bounds(x + 132, by, 120, 20).build());
    }

    private Component modeLabel(int i) {
        return Component.literal("模式 " + i + (mode == i ? "(当前)" : "") + " " + MODE_LABELS[i]);
    }

    private void refreshModeButtons() {
        for (int i = 0; i < modeButtons.size(); i++) {
            modeButtons.get(i).setMessage(modeLabel(i));
        }
    }

    private void addSlider(String label, int value, int min, int max, Consumer<Integer> setter, int x, int y) {
        double v = max <= min ? 0.0 : (double) (value - min) / (double) (max - min);
        addRenderableWidget(new AbstractSliderButton(x, y, 340, 20, Component.literal(label + ": " + value), v) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(label + ": " + (min + (int) Math.round((max - min) * value))));
            }

            @Override
            protected void applyValue() {
                setter.accept(min + (int) Math.round((max - min) * value));
            }
        });
    }

    private void save() {
        cfg.mode = mode;
        cfg.tpaRequestCooldownSeconds = tpaCooldown;
        cfg.tpaRequestTimeoutSeconds = tprTimeout;
        cfg.homeCooldownSeconds = homeCooldown;
        cfg.backCooldownSeconds = backCooldown;
        cfg.tprCooldownSeconds = tprCooldown;
        cfg.teleportDelayTicks = delay;
        cfg.tprRange = tprRange;
        cfg.maxHomes = maxHomes;
        cfg.cancelOnMove = cancelOnMove;
        cfg.deathBack = deathBack;
        cfg.save(MyrinTPMod.CONFIG_DIR.resolve("config.json"));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0xFF111111);
        int x = this.width / 2 - 170;
        g.drawString(this.font, "指令管控：0 关闭 / 1 非OP仅TP / 2 OP禁非TP / 3 同时启用", x, 16, 0xFFFFFFFF);
        g.drawString(this.font, "模式点击立即生效；参数拖动调节，保存后生效（多人请由服主设置）。", x, this.height - 62, 0xFFAAAAAA);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
