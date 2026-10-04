package com.myrin.tp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 配置界面：参数行放在可滚动列表内，适配小分辨率与原版/钠界面缩放；
 * 提示文字与保存/取消固定在底部，互不遮挡。
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

    private ConfigList list;
    private final List<Button> modeButtons = new ArrayList<>();

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
            int bx = x + i * 122;
            Button b = Button.builder(Component.literal("模式 " + i + (mode == i ? "(当前)" : "")),
                    btn -> { mode = m; cfg.mode = m; cfg.save(MyrinTPMod.CONFIG_DIR.resolve("config.json")); refreshModeButtons(); rebuild(); })
                    .bounds(bx, 34, 110, 20).build();
            modeButtons.add(b);
            addRenderableWidget(b);
        }

        int listTop = 62;
        int listBottom = this.height - 70;
        if (listBottom < listTop + 60) {
            listBottom = listTop + 60;
        }
        list = new ConfigList(Minecraft.getInstance(), 340, listBottom - listTop, listTop, listBottom);
        list.setX(x);
        rebuildList();
        addRenderableWidget(list);

        int by = this.height - 32;
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save()).bounds(x, by, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose()).bounds(x + 132, by, 120, 20).build());
    }

    private void rebuildList() {
        if (list == null) {
            return;
        }
        list.clearAll();
        list.add(new SliderEntry("tpa 请求冷却(秒)", tpaCooldown, 0, 3600, v -> tpaCooldown = v));
        list.add(new SliderEntry("tpa 请求超时(秒)", tprTimeout, 5, 300, v -> tprTimeout = v));
        list.add(new SliderEntry("回家冷却(秒)", homeCooldown, 0, 3600, v -> homeCooldown = v));
        list.add(new SliderEntry("返回冷却(秒)", backCooldown, 0, 3600, v -> backCooldown = v));
        list.add(new SliderEntry("随机传送冷却(秒)", tprCooldown, 0, 3600, v -> tprCooldown = v));
        list.add(new SliderEntry("传送倒计时(刻)", delay, 0, 200, v -> delay = v));
        list.add(new SliderEntry("随机传送范围", tprRange, 100, 200000, v -> tprRange = v));
        list.add(new SliderEntry("家点数量上限", maxHomes, 1, 100, v -> maxHomes = v));
        list.add(new ToggleEntry("倒计时期间移动取消", cancelOnMove, v -> cancelOnMove = v));
        list.add(new ToggleEntry("/back 优先回死亡点", deathBack, v -> deathBack = v));
    }

    private void rebuild() {
        if (list != null) {
            rebuildList();
        }
    }

    private void refreshModeButtons() {
        for (int i = 0; i < modeButtons.size(); i++) {
            modeButtons.get(i).setMessage(Component.literal("模式 " + i + (mode == i ? "(当前)" : "")));
        }
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
        ex.fill(0, 0, this.width, this.height, 0xFF111111);
        super.extractRenderState(ex, mx, my, pt);
        int x = this.width / 2 - 170;
        ex.text(font, "指令管控：0 不限制 / 1 玩家仅TP / 2 管理员仅TP / 3 全部仅TP", x, 18, 0xFFFFFFFF);
        ex.text(font, "模式点击立即生效；参数拖动调节，保存后生效（多人请由服主设置）。", x, this.height - 62, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    /** 参数行：可拖动滑块调节数值 */
    private final class SliderEntry extends ContainerObjectSelectionList.Entry<SliderEntry> {
        private final Slider slider;

        SliderEntry(String label, int value, int min, int max, Consumer<Integer> setter) {
            double v = max <= min ? 0.0 : (double) (value - min) / (double) (max - min);
            this.slider = new Slider(label, min, max, v, setter);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(slider);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return children().stream().filter(c -> c instanceof NarratableEntry).map(c -> (NarratableEntry) c).toList();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor ex, int mx, int my, boolean hovered, float pt) {
            slider.setX(this.getX());
            slider.setY(this.getY());
            slider.setWidth(340);
            slider.extractRenderState(ex, mx, my, pt);
        }

        /** 拖动滑块即时更新数值变量，保存时统一落盘 */
        private final class Slider extends AbstractSliderButton {
            private final String label;
            private final int min;
            private final int max;
            private final Consumer<Integer> setter;

            Slider(String label, int min, int max, double value, Consumer<Integer> setter) {
                super(0, 0, 340, 20, Component.literal(label), value);
                this.label = label;
                this.min = min;
                this.max = max;
                this.setter = setter;
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(label + ": " + current()));
            }

            @Override
            protected void applyValue() {
                setter.accept(current());
            }

            private int current() {
                return min + (int) Math.round((max - min) * value);
            }
        }
    }

    /** 开关行：点击切换 */
    private final class ToggleEntry extends ContainerObjectSelectionList.Entry<ToggleEntry> {
        private final Button toggle;
        private final String label;
        private boolean value;
        private final Consumer<Boolean> setter;

        ToggleEntry(String label, boolean value, Consumer<Boolean> setter) {
            this.label = label;
            this.value = value;
            this.setter = setter;
            this.toggle = Button.builder(Component.literal(label + ": " + (value ? "开" : "关")), b -> change()).bounds(0, 0, 250, 20).build();
        }

        private void change() {
            value = !value;
            setter.accept(value);
            toggle.setMessage(Component.literal(label + ": " + (value ? "开" : "关")));
            rebuildList();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return children().stream().filter(c -> c instanceof NarratableEntry).map(c -> (NarratableEntry) c).toList();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor ex, int mx, int my, boolean hovered, float pt) {
            toggle.setX(this.getX());
            toggle.setY(this.getY());
            toggle.extractRenderState(ex, mx, my, pt);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class ConfigList extends ContainerObjectSelectionList {
        ConfigList(Minecraft mc, int width, int height, int y0, int y1) {
            super(mc, width, height, y0, y1);
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        public void clearAll() {
            clearEntries();
        }

        @SuppressWarnings("unchecked")
        public void add(ContainerObjectSelectionList.Entry e) {
            addEntry(e);
        }
    }
}
