package com.myrin.tp;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MyrinConfigScreen extends Screen {
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

    public MyrinConfigScreen(Screen parent) {
        super(Component.literal("Myrin TP 配置"));
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
        for (int i = 0; i < 4; i++) {
            final int m = i;
            int bx = x + i * 86;
            addRenderableWidget(Button.builder(Component.literal("守卫模式 " + i + (mode == i ? " ✓" : "")),
                    b -> { mode = m; rebuild(); })
                    .bounds(bx, 34, 82, 20).build());
        }

        int y = 70;
        y = numRow("tpa 发送冷却(秒)", tpaCooldown, 5, y);
        y = numRow("tpa 请求超时(秒)", tprTimeout, 5, y);
        y = numRow("home 冷却(秒)", homeCooldown, 5, y);
        y = numRow("back 冷却(秒)", backCooldown, 5, y);
        y = numRow("tpr 冷却(秒)", tprCooldown, 5, y);
        y = numRow("传送倒计时(tick)", delay, 5, y);
        y = numRow("tpr 范围", tprRange, 500, y);
        y = numRow("最大家点数", maxHomes, 1, y);

        addRenderableWidget(Button.builder(Component.literal("倒计时移动取消: " + onOff(cancelOnMove)),
                b -> { cancelOnMove = !cancelOnMove; rebuild(); })
                .bounds(x, y, 250, 20).build());
        y += 28;
        addRenderableWidget(Button.builder(Component.literal("back 优先死亡点: " + onOff(deathBack)),
                b -> { deathBack = !deathBack; rebuild(); })
                .bounds(x, y, 250, 20).build());

        int by = this.height - 32;
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save()).bounds(x, by, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose()).bounds(x + 132, by, 120, 20).build());
    }

    private String onOff(boolean v) {
        return v ? "开" : "关";
    }

    private int numRow(String label, int value, int step, int y) {
        int x = this.width / 2 - 170;
        addRenderableWidget(Button.builder(Component.literal("-" + step), b -> { setNum(label, value - step, step); })
                .bounds(x, y, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+" + step), b -> { setNum(label, value + step, step); })
                .bounds(x + 210, y, 40, 20).build());
        return y + 26;
    }

    private void setNum(String label, int newVal, int step) {
        if (newVal < 0) newVal = 0;
        switch (label) {
            case "tpa 发送冷却(秒)" -> tpaCooldown = newVal;
            case "tpa 请求超时(秒)" -> tprTimeout = newVal;
            case "home 冷却(秒)" -> homeCooldown = newVal;
            case "back 冷却(秒)" -> backCooldown = newVal;
            case "tpr 冷却(秒)" -> tprCooldown = newVal;
            case "传送倒计时(tick)" -> delay = newVal;
            case "tpr 范围" -> tprRange = newVal;
            case "最大家点数" -> maxHomes = newVal;
        }
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        init();
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
    public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        int x = this.width / 2 - 170;
        g.drawString(this.font, "守卫模式（0 关 / 1 黑名单 / 2 白名单 / 3 全限制）", x, 18, 0xFFFFFFFF);
        g.drawString(this.font, "tpa 发送冷却(秒): " + tpaCooldown, x + 46, 74, 0xFFAAAAAA);
        g.drawString(this.font, "tpa 请求超时(秒): " + tprTimeout, x + 46, 100, 0xFFAAAAAA);
        g.drawString(this.font, "home 冷却(秒): " + homeCooldown, x + 46, 126, 0xFFAAAAAA);
        g.drawString(this.font, "back 冷却(秒): " + backCooldown, x + 46, 152, 0xFFAAAAAA);
        g.drawString(this.font, "tpr 冷却(秒): " + tprCooldown, x + 46, 178, 0xFFAAAAAA);
        g.drawString(this.font, "传送倒计时(tick): " + delay, x + 46, 204, 0xFFAAAAAA);
        g.drawString(this.font, "tpr 范围: " + tprRange, x + 46, 230, 0xFFAAAAAA);
        g.drawString(this.font, "最大家点数: " + maxHomes, x + 46, 256, 0xFFAAAAAA);
        g.drawString(this.font, "保存后需重启生效；多人模式请改服务端配置。", x, this.height - 58, 0xFF888888);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
