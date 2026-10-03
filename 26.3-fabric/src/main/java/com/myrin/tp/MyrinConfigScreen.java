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
        for (int i = 0; i < 4; i++) {
            final int m = i;
            int bx = x + i * 122;
            addRenderableWidget(Button.builder(Component.literal("模式 " + i + (mode == i ? "(当前)" : "")),
                    b -> { mode = m; rebuild(); })
                    .bounds(bx, 34, 110, 20).build());
        }

        int y = 70;
        y = numRow("tpa 请求冷却(秒)", tpaCooldown, 5, y);
        y = numRow("tpa 请求超时(秒)", tprTimeout, 5, y);
        y = numRow("回家冷却(秒)", homeCooldown, 5, y);
        y = numRow("返回冷却(秒)", backCooldown, 5, y);
        y = numRow("随机传送冷却(秒)", tprCooldown, 5, y);
        y = numRow("传送倒计时(刻)", delay, 5, y);
        y = numRow("随机传送范围", tprRange, 500, y);
        y = numRow("家点数量上限", maxHomes, 1, y);

        addRenderableWidget(Button.builder(Component.literal("倒计时期间移动取消: " + onOff(cancelOnMove)),
                b -> { cancelOnMove = !cancelOnMove; rebuild(); })
                .bounds(x, y, 250, 20).build());
        y += 28;
        addRenderableWidget(Button.builder(Component.literal("/back 优先回死亡点: " + onOff(deathBack)),
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
            case "tpa 请求冷却(秒)" -> tpaCooldown = newVal;
            case "tpa 请求超时(秒)" -> tprTimeout = newVal;
            case "回家冷却(秒)" -> homeCooldown = newVal;
            case "返回冷却(秒)" -> backCooldown = newVal;
            case "随机传送冷却(秒)" -> tprCooldown = newVal;
            case "传送倒计时(刻)" -> delay = newVal;
            case "随机传送范围" -> tprRange = newVal;
            case "家点数量上限" -> maxHomes = newVal;
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
        ex.text(font, "指令管控：0 不限制 / 1 玩家仅TP / 2 管理员仅TP / 3 全部仅TP", x, 18, 0xFFFFFFFF);
        ex.text(font, "tpa 请求冷却(秒): " + tpaCooldown, x + 46, 74, 0xFFAAAAAA);
        ex.text(font, "tpa 请求超时(秒): " + tprTimeout, x + 46, 100, 0xFFAAAAAA);
        ex.text(font, "回家冷却(秒): " + homeCooldown, x + 46, 126, 0xFFAAAAAA);
        ex.text(font, "返回冷却(秒): " + backCooldown, x + 46, 152, 0xFFAAAAAA);
        ex.text(font, "随机传送冷却(秒): " + tprCooldown, x + 46, 178, 0xFFAAAAAA);
        ex.text(font, "传送倒计时(刻): " + delay, x + 46, 204, 0xFFAAAAAA);
        ex.text(font, "随机传送范围: " + tprRange, x + 46, 230, 0xFFAAAAAA);
        ex.text(font, "家点数量上限: " + maxHomes, x + 46, 256, 0xFFAAAAAA);
        ex.text(font, "修改立即生效；多人模式请由服主在服务端设置。", x, this.height - 58, 0xFF888888);
    }

    @Override
    public void onClose() {
        net.minecraft.client.Minecraft.getInstance().setScreenAndShow(parent);
    }
}
