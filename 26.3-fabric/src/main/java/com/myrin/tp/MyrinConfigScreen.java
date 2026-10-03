package com.myrin.tp;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        y = numRow("tpaCooldown", tpaCooldown, 5, y);
        y = numRow("tprTimeout", tprTimeout, 5, y);
        y = numRow("homeCooldown", homeCooldown, 5, y);
        y = numRow("backCooldown", backCooldown, 5, y);
        y = numRow("tprCooldown", tprCooldown, 5, y);
        y = numRow("delay", delay, 5, y);
        y = numRow("tprRange", tprRange, 500, y);
        y = numRow("maxHomes", maxHomes, 1, y);

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
            case "tpaCooldown" -> tpaCooldown = newVal;
            case "tprTimeout" -> tprTimeout = newVal;
            case "homeCooldown" -> homeCooldown = newVal;
            case "backCooldown" -> backCooldown = newVal;
            case "tprCooldown" -> tprCooldown = newVal;
            case "delay" -> delay = newVal;
            case "tprRange" -> tprRange = newVal;
            case "maxHomes" -> maxHomes = newVal;
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
    public void extractRenderState(GuiGraphicsExtractor ex, int mx, int my, float pt) {
        ex.fill(0, 0, this.width, this.height, 0xCC111111);
        super.extractRenderState(ex, mx, my, pt);
        int x = this.width / 2 - 170;
        ex.text(font, "守卫模式（0 关 / 1 黑名单 / 2 白名单 / 3 全限制）", x, 18, 0xFFFFFFFF);
        ex.text(font, "tpa 发送冷却(秒): " + tpaCooldown, x + 46, 74, 0xFFAAAAAA);
        ex.text(font, "tpa 请求超时(秒): " + tprTimeout, x + 46, 100, 0xFFAAAAAA);
        ex.text(font, "home 冷却(秒): " + homeCooldown, x + 46, 126, 0xFFAAAAAA);
        ex.text(font, "back 冷却(秒): " + backCooldown, x + 46, 152, 0xFFAAAAAA);
        ex.text(font, "tpr 冷却(秒): " + tprCooldown, x + 46, 178, 0xFFAAAAAA);
        ex.text(font, "传送倒计时(tick): " + delay, x + 46, 204, 0xFFAAAAAA);
        ex.text(font, "tpr 范围: " + tprRange, x + 46, 230, 0xFFAAAAAA);
        ex.text(font, "最大家点数: " + maxHomes, x + 46, 256, 0xFFAAAAAA);
        ex.text(font, "保存后需重启生效；多人模式请改服务端配置。", x, this.height - 58, 0xFF888888);
    }

    @Override
    public void onClose() {
        net.minecraft.client.Minecraft.getInstance().setScreenAndShow(parent);
    }
}
