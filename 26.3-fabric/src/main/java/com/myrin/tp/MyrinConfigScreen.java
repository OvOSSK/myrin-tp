package com.myrin.tp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    /** 当前打开的配置界面（客户端），供网络回包刷新。 */
    public static MyrinConfigScreen active;

    private final List<Button> modeButtons = new ArrayList<>();
    private final List<AbstractSliderButton> sliders = new ArrayList<>();
    private Button toggleMove;
    private Button toggleBack;
    private Button saveButton;
    private boolean canEdit;
    private boolean localMode;
    private boolean queried;

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
        MyrinConfigScreen.active = this;
    }

    @Override
    protected void init() {
        this.localMode = this.minecraft.hasSingleplayerServer() || this.minecraft.player == null;
        this.canEdit = localMode;
        if (!localMode && !queried) {
            queried = true;
            sendSync(0, "");
        }
        int x = this.width / 2 - 170;
        int rows = 10;
        int bottomY = this.height - 28;
        int step = Math.max(12, Math.min(24, (bottomY - 66) / rows));
        int ctrlH = Math.min(20, step);
        int topY = Math.max(66, (bottomY - 20 - rows * step) / 2);

        modeButtons.clear();
        for (int i = 0; i < 4; i++) {
            final int m = i;
            Button b = Button.builder(modeLabel(i),
                    btn -> { mode = m; cfg.mode = m; if (localMode) { cfg.save(MyrinTPMod.CONFIG_FILE); } else { submit(); } refreshModeButtons(); })
                    .bounds(x + i * 100, 30, 90, 18).build();
            modeButtons.add(b);
            addRenderableWidget(b);
        }

        int y = topY;
        addSlider("tpa 请求冷却(秒)", tpaCooldown, 0, 3600, v -> tpaCooldown = v, x, y, ctrlH); y += step;
        addSlider("tpa 请求超时(秒)", tprTimeout, 5, 300, v -> tprTimeout = v, x, y, ctrlH); y += step;
        addSlider("回家冷却(秒)", homeCooldown, 0, 3600, v -> homeCooldown = v, x, y, ctrlH); y += step;
        addSlider("返回冷却(秒)", backCooldown, 0, 3600, v -> backCooldown = v, x, y, ctrlH); y += step;
        addSlider("随机传送冷却(秒)", tprCooldown, 0, 3600, v -> tprCooldown = v, x, y, ctrlH); y += step;
        addSlider("传送倒计时(刻)", delay, 0, 200, v -> delay = v, x, y, ctrlH); y += step;
        addSlider("随机传送范围", tprRange, 100, 200000, v -> tprRange = v, x, y, ctrlH); y += step;
        addSlider("家点数量上限", maxHomes, 1, 100, v -> maxHomes = v, x, y, ctrlH); y += step;

        toggleMove = Button.builder(Component.literal("倒计时期间移动取消: " + (cancelOnMove ? "开" : "关")),
                b -> { cancelOnMove = !cancelOnMove; b.setMessage(Component.literal("倒计时期间移动取消: " + (cancelOnMove ? "开" : "关"))); })
                .bounds(x, y, 340, ctrlH).build();
        addRenderableWidget(toggleMove); y += step;
        toggleBack = Button.builder(Component.literal("/back 优先回死亡点: " + (deathBack ? "开" : "关")),
                b -> { deathBack = !deathBack; b.setMessage(Component.literal("/back 优先回死亡点: " + (deathBack ? "开" : "关"))); })
                .bounds(x, y, 340, ctrlH).build();
        addRenderableWidget(toggleBack);

        int by = this.height - 28;
        saveButton = Button.builder(Component.literal("保存"), b -> save()).bounds(x, by, 120, 20).build();
        addRenderableWidget(saveButton);
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose()).bounds(x + 132, by, 120, 20).build());
        if (!canEdit) {
            applyReadOnly();
        }
    }

    private Component modeLabel(int i) {
        return Component.literal("模式 " + i + (mode == i ? "(当前)" : ""));
    }

    private void refreshModeButtons() {
        for (int i = 0; i < modeButtons.size(); i++) {
            modeButtons.get(i).setMessage(modeLabel(i));
        }
    }

    private void addSlider(String label, int value, int min, int max, Consumer<Integer> setter, int x, int y, int h) {
        double v = max <= min ? 0.0 : (double) (value - min) / (double) (max - min);
        AbstractSliderButton s = new AbstractSliderButton(x, y, 340, h, Component.literal(label + ": " + value), v) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(label + ": " + (min + (int) Math.round((max - min) * value))));
            }

            @Override
            protected void applyValue() {
                setter.accept(min + (int) Math.round((max - min) * value));
            }
        };
        sliders.add(s);
        addRenderableWidget(s);
    }

    private void save() {
        if (!canEdit) {
            return;
        }
        if (localMode) {
            this.cfg = buildConfig();
            this.cfg.save(MyrinTPMod.CONFIG_FILE);
        } else {
            submit();
        }
        onClose();
    }

    /** 按当前界面临时值构造配置对象。 */
    private Config buildConfig() {
        Config c = new Config();
        c.mode = mode;
        c.tpaRequestCooldownSeconds = tpaCooldown;
        c.tpaRequestTimeoutSeconds = tprTimeout;
        c.homeCooldownSeconds = homeCooldown;
        c.backCooldownSeconds = backCooldown;
        c.tprCooldownSeconds = tprCooldown;
        c.teleportDelayTicks = delay;
        c.tprRange = tprRange;
        c.maxHomes = maxHomes;
        c.cancelOnMove = cancelOnMove;
        c.deathBack = deathBack;
        return c;
    }

    /** 网络模式：提交当前配置给服务器（服务端校验 OP 并广播）。 */
    private void submit() {
        sendSync(1, buildConfig().toJsonString());
    }

    private void sendSync(int type, String json) {
        Network.sendToServer(new ConfigSyncPacket(type, json));
    }

    /** 服务器回包：更新权限状态与服务器配置，并重建界面。 */
    public void onConfigStatus(boolean edit, String json) {
        if (!localMode && json != null && !json.isEmpty()) {
            Config n = Config.fromJson(json);
            this.cfg = n;
            this.mode = n.mode;
            this.tpaCooldown = n.tpaRequestCooldownSeconds;
            this.tprTimeout = n.tpaRequestTimeoutSeconds;
            this.homeCooldown = n.homeCooldownSeconds;
            this.backCooldown = n.backCooldownSeconds;
            this.tprCooldown = n.tprCooldownSeconds;
            this.delay = n.teleportDelayTicks;
            this.tprRange = n.tprRange;
            this.maxHomes = n.maxHomes;
            this.cancelOnMove = n.cancelOnMove;
            this.deathBack = n.deathBack;
        }
        this.canEdit = edit;
        this.init();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ex, int mx, int my, float pt) {
        ex.fill(0, 0, this.width, this.height, 0xFF111111);
        int x = this.width / 2 - 170;
        ex.text(font, Component.literal("指令管控：0关闭 / 1非OP仅TP / 2OP禁非TP / 3同时启用"), x, 12, 0xFFFFFFFF);
        if (this.height >= 260) {
            if (canEdit) {
                ex.text(font, Component.literal("模式点击立即生效；参数拖动调节，保存后生效（多人请由服主设置）。"), x, 52, 0xFFAAAAAA);
            } else {
                ex.text(font, Component.literal("非 OP 玩家只能查看当前配置，无法修改"), x, 52, 0xFFFF5555);
            }
        }
        super.extractRenderState(ex, mx, my, pt);
    }

    /** 非 OP 只读：禁用全部修改控件。 */
    private void applyReadOnly() {
        for (Button b : modeButtons) {
            b.active = false;
        }
        for (AbstractSliderButton s : sliders) {
            s.active = false;
        }
        if (toggleMove != null) {
            toggleMove.active = false;
        }
        if (toggleBack != null) {
            toggleBack.active = false;
        }
        if (saveButton != null) {
            saveButton.active = false;
        }
    }

    /** 是否有权修改：本地模式（主机/主界面）直接放行，网络模式以服务器回包为准。 */
    private boolean isOp() {
        return localMode;
    }

    @Override
    public void onClose() {
        MyrinConfigScreen.active = null;
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
