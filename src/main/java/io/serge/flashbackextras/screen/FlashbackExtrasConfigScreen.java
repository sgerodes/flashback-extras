package io.serge.flashbackextras.screen;

import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class FlashbackExtrasConfigScreen extends Screen {
    private final Screen parent;
    private Tab tab = Tab.FEATURES;

    public FlashbackExtrasConfigScreen(Screen parent) {
        super(Component.translatable("flashbackextras.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int panelWidth = 280;
        int left = centerX - panelWidth / 2;
        int y = this.height / 4 + 8;

        int tabWidth = 120;
        this.addRenderableWidget(Button.builder(this.tabLabel(Tab.FEATURES), button -> {
                this.tab = Tab.FEATURES;
                this.init();
            })
            .bounds(centerX - tabWidth - 4, y - 28, tabWidth, 20)
            .build());

        this.addRenderableWidget(Button.builder(this.tabLabel(Tab.FIXES), button -> {
                this.tab = Tab.FIXES;
                this.init();
            })
            .bounds(centerX + 4, y - 28, tabWidth, 20)
            .build());

        if (this.tab == Tab.FEATURES) {
            this.addToggleRow(left, y,
                "flashbackextras.option.timeline_horizontal_scroll",
                "flashbackextras.option.timeline_horizontal_scroll.description",
                FlashbackExtrasConfig.isTimelineHorizontalScrollEnabled(),
                FlashbackExtrasConfig::setTimelineHorizontalScrollEnabled);
            this.addToggleRow(left, y + 26,
                "flashbackextras.option.export_presets",
                "flashbackextras.option.export_presets.description",
                FlashbackExtrasConfig.isExportPresetsEnabled(),
                FlashbackExtrasConfig::setExportPresetsEnabled);
            this.addToggleRow(left, y + 52,
                "flashbackextras.option.export_crop_guide",
                "flashbackextras.option.export_crop_guide.description",
                FlashbackExtrasConfig.isExportCropGuideEnabled(),
                FlashbackExtrasConfig::setExportCropGuideEnabled);
        } else {
            this.addToggleRow(left, y,
                "flashbackextras.option.audio_timeline_fixes",
                "flashbackextras.option.audio_timeline_fixes.description",
                FlashbackExtrasConfig.isAudioTimelineFixesEnabled(),
                FlashbackExtrasConfig::setAudioTimelineFixesEnabled);
            this.addToggleRow(left, y + 26,
                "flashbackextras.option.export_warmup",
                "flashbackextras.option.export_warmup.description",
                FlashbackExtrasConfig.isExportWarmupEnabled(),
                FlashbackExtrasConfig::setExportWarmupEnabled);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> this.onClose())
            .bounds(centerX - 100, this.height - 28, 200, 20)
            .build());
    }

    private void addToggleRow(int x, int y, String labelKey, String descriptionKey, boolean value, Consumer<Boolean> setter) {
        CycleButton<Boolean> button = CycleButton.onOffBuilder(value)
            .create(x, y, 280, 20, Component.translatable(labelKey), (cycleButton, newValue) -> setter.accept(newValue));
        button.setTooltip(Tooltip.create(Component.translatable(descriptionKey)));
        this.addRenderableWidget(button);
    }

    private Component tabLabel(Tab tab) {
        String key = tab == Tab.FEATURES ? "flashbackextras.tab.features" : "flashbackextras.tab.fixes";
        if (this.tab == tab) {
            return Component.literal("[ ").append(Component.translatable(key)).append(" ]");
        }
        return Component.translatable(key);
    }

    @Override
    public void onClose() {
        FlashbackExtrasConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int panelWidth = 296;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = this.height / 4 - 8;
        int panelBottom = panelTop + (this.tab == Tab.FEATURES ? 124 : 98);

        guiGraphics.fill(0, 0, this.width, this.height, 0x55101010);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xCC101010);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0xFF6A6A6A);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelLeft + panelWidth, panelBottom, 0xFF6A6A6A);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF6A6A6A);
        guiGraphics.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelBottom, 0xFF6A6A6A);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, centerX, 20, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font,
            Component.translatable(this.tab == Tab.FEATURES ? "flashbackextras.tab.features" : "flashbackextras.tab.fixes"),
            centerX, panelTop + 8, 0xFFFFFFFF);
    }

    private enum Tab {
        FEATURES,
        FIXES
    }
}
