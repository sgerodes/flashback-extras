package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.editor.ui.windows.StartExportWindow;
import imgui.moulberry90.ImGui;
import imgui.moulberry90.type.ImString;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import io.serge.flashbackextras.export.ExportCapabilityWarmup;
import io.serge.flashbackextras.export.ExportCapabilityWarmup.Status;
import io.serge.flashbackextras.export.ExportPreset;
import net.minecraft.client.resources.language.I18n;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = StartExportWindow.class, remap = false)
public abstract class StartExportWindowMixin {
    @Shadow @Final private static ImString bitrate;
    @Shadow @Final private static ImString pngSequenceFormat;

    @Unique private static final int[] flashbackExtras$selectedPreset = new int[]{-1};
    @Unique private static final ImString flashbackExtras$presetName = ImGuiHelper.createResizableImString("");
    @Unique private static final String FLASHBACK_EXTRAS_SAVE_PRESET_POPUP = "###FlashbackExtrasSavePreset";
    @Unique private static final String FLASHBACK_EXTRAS_OVERWRITE_PRESET_POPUP = "Overwrite Preset###FlashbackExtrasOverwritePreset";
    @Unique private static final String FLASHBACK_EXTRAS_DELETE_PRESET_POPUP = "Delete Preset###FlashbackExtrasDeletePreset";

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moulberry/flashback/editor/ui/ImGuiHelper;separatorWithText(Ljava/lang/String;)V",
            ordinal = 0,
            shift = At.Shift.BEFORE
        )
    )
    private static void flashbackExtras$renderExportPresets(CallbackInfo ci) {
        if (!FlashbackExtrasConfig.isExportPresetsEnabled()) {
            return;
        }

        FlashbackConfigV1 config = Flashback.getConfig();
        List<ExportPreset> presets = FlashbackExtrasConfig.getAllExportPresets();
        if (flashbackExtras$selectedPreset[0] >= presets.size()) {
            flashbackExtras$selectedPreset[0] = presets.size() - 1;
        }

        ImGuiHelper.separatorWithText(I18n.get("flashbackextras.export_presets.title"));

        float availableWidth = ImGui.getContentRegionAvailX();
        float iconButtonWidth = 28f;
        float spacing = ImGui.getStyle().getItemSpacingX();

        boolean hasSelection = flashbackExtras$selectedPreset[0] >= 0 && flashbackExtras$selectedPreset[0] < presets.size();
        ExportPreset selectedPreset = hasSelection ? presets.get(flashbackExtras$selectedPreset[0]) : null;
        boolean canModifySelected = selectedPreset != null && !selectedPreset.builtIn();

        if (presets.isEmpty()) {
            ImGui.textDisabled(I18n.get("flashbackextras.export_presets.none"));
        } else {
            String[] presetNames = presets.stream().map(ExportPreset::displayName).toArray(String[]::new);
            float comboWidth = Math.max(120f, availableWidth - iconButtonWidth * 3f - spacing * 3f);
            ImGui.setNextItemWidth(comboWidth);
            ImGuiHelper.combo("##FlashbackExtrasPreset", flashbackExtras$selectedPreset, presetNames);
            ImGui.sameLine();
        }

        if (ImGui.button("+", iconButtonWidth, 0)) {
            flashbackExtras$presetName.set("");
            ImGui.openPopup(FLASHBACK_EXTRAS_SAVE_PRESET_POPUP);
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.get("flashbackextras.export_presets.save_current_tooltip"));
        }

        ImGui.sameLine();
        if (!canModifySelected) {
            ImGui.beginDisabled();
        }
        if (ImGui.button("*", iconButtonWidth, 0) && canModifySelected) {
            ImGui.openPopup(FLASHBACK_EXTRAS_OVERWRITE_PRESET_POPUP);
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.get("flashbackextras.export_presets.overwrite_tooltip"));
        }
        if (!canModifySelected) {
            ImGui.endDisabled();
        }

        ImGui.sameLine();
        if (!canModifySelected) {
            ImGui.beginDisabled();
        }
        if (ImGui.button("-", iconButtonWidth, 0) && canModifySelected) {
            ImGui.openPopup(FLASHBACK_EXTRAS_DELETE_PRESET_POPUP);
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.get("flashbackextras.export_presets.delete_tooltip"));
        }
        if (!canModifySelected) {
            ImGui.endDisabled();
        }

        if (!hasSelection) {
            ImGui.beginDisabled();
        }
        if (ImGui.button(I18n.get("flashbackextras.export_presets.apply"), ImGui.getContentRegionAvailX(), 0) && hasSelection) {
            flashbackExtras$applyPreset(config, selectedPreset);
        }
        if (!hasSelection) {
            ImGui.endDisabled();
        }

        Status warmupStatus = ExportCapabilityWarmup.getStatus();
        if (warmupStatus == Status.SCHEDULED || warmupStatus == Status.RUNNING) {
            ImGui.textDisabled(I18n.get("flashbackextras.export_warmup.preparing"));
        }

        if (ImGui.beginPopup(FLASHBACK_EXTRAS_SAVE_PRESET_POPUP)) {
            ImGui.setNextItemWidth(220);
            ImGui.inputText(I18n.get("flashbackextras.export_presets.name"), flashbackExtras$presetName);

            if (ImGui.button(I18n.get("flashbackextras.export_presets.save"))) {
                String name = ImGuiHelper.getString(flashbackExtras$presetName).trim();
                if (!name.isEmpty()) {
                    FlashbackExtrasConfig.saveExportPreset(ExportPreset.from(name, config.internalExport,
                        ImGuiHelper.getString(bitrate), ImGuiHelper.getString(pngSequenceFormat)));
                    flashbackExtras$selectPresetByName(name);
                    ImGui.closeCurrentPopup();
                }
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.get("gui.cancel"))) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        flashbackExtras$renderConfirmPopup(
            FLASHBACK_EXTRAS_OVERWRITE_PRESET_POPUP,
            I18n.get("flashbackextras.export_presets.overwrite_confirm", selectedPreset != null ? selectedPreset.name() : ""),
            I18n.get("flashbackextras.export_presets.overwrite_yes"),
            () -> {
                if (selectedPreset != null && !selectedPreset.builtIn()) {
                    FlashbackExtrasConfig.saveExportPreset(ExportPreset.from(selectedPreset.name(), config.internalExport,
                        ImGuiHelper.getString(bitrate), ImGuiHelper.getString(pngSequenceFormat)));
                }
            }
        );

        flashbackExtras$renderConfirmPopup(
            FLASHBACK_EXTRAS_DELETE_PRESET_POPUP,
            I18n.get("flashbackextras.export_presets.delete_confirm", selectedPreset != null ? selectedPreset.name() : ""),
            I18n.get("flashbackextras.export_presets.delete_yes"),
            () -> {
                if (selectedPreset != null && !selectedPreset.builtIn()) {
                    FlashbackExtrasConfig.deleteExportPreset(selectedPreset.name());
                    flashbackExtras$selectedPreset[0] = -1;
                }
            }
        );
    }

    @Unique
    private static void flashbackExtras$renderConfirmPopup(String popupId, String message, String confirmLabel, Runnable action) {
        float popupWidth = 360f;
        float buttonWidth = 110f;
        ImGui.setNextWindowSize(popupWidth, 0f);
        if (ImGui.beginPopupModal(popupId)) {
            ImGui.pushTextWrapPos(ImGui.getCursorPosX() + popupWidth - 24f);
            ImGui.textWrapped(message);
            ImGui.popTextWrapPos();
            ImGui.dummy(0, 10);

            float buttonsWidth = buttonWidth * 2f + ImGui.getStyle().getItemSpacingX();
            float offset = Math.max(0f, (ImGui.getContentRegionAvailX() - buttonsWidth) / 2f);
            ImGui.setCursorPosX(ImGui.getCursorPosX() + offset);
            if (ImGui.button(confirmLabel, buttonWidth, 0)) {
                action.run();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.get("gui.cancel"), buttonWidth, 0)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
    }

    @Unique
    private static void flashbackExtras$applyPreset(FlashbackConfigV1 config, ExportPreset preset) {
        preset.applyTo(config.internalExport);
        bitrate.set(preset.bitrate());
        pngSequenceFormat.set(preset.pngSequenceFormat());
    }

    @Unique
    private static void flashbackExtras$selectPresetByName(String name) {
        List<ExportPreset> presets = FlashbackExtrasConfig.getAllExportPresets();
        for (int i = 0; i < presets.size(); i++) {
            if (name.equals(presets.get(i).name())) {
                flashbackExtras$selectedPreset[0] = i;
                return;
            }
        }
        flashbackExtras$selectedPreset[0] = -1;
    }
}
