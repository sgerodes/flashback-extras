package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.moulberry90.ImDrawList;
import imgui.moulberry90.ImGui;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReplayUI.class, remap = false)
public abstract class ReplayUIMixin {
    @Shadow public static int frameX;
    @Shadow public static int frameY;
    @Shadow public static int frameWidth;
    @Shadow public static int frameHeight;

    @Inject(method = "drawOverlayInternal", at = @At(value = "INVOKE", target = "Limgui/moulberry90/ImGui;render()V", shift = At.Shift.BEFORE))
    private static void flashbackExtras$drawExportCropGuide(CallbackInfo ci) {
        if (!FlashbackExtrasConfig.isExportCropGuideEnabled()) {
            return;
        }
        if (Minecraft.getInstance().screen != null || Minecraft.getInstance().getOverlay() != null) {
            return;
        }
        if (!Flashback.isInReplay() || frameWidth <= 1 || frameHeight <= 1) {
            return;
        }
        if (EditorStateManager.getCurrent() == null) {
            return;
        }

        FlashbackConfigV1.SubcategoryInternalExport export = Flashback.getConfig().internalExport;
        if (export.resolution == null || export.resolution.length < 2 || export.resolution[0] <= 0 || export.resolution[1] <= 0) {
            return;
        }

        float targetAspectRatio = export.resolution[0] / (float) export.resolution[1];
        float currentAspectRatio = frameWidth / (float) frameHeight;
        if (Math.abs(currentAspectRatio - targetAspectRatio) < 0.001f) {
            return;
        }

        float guideX = frameX;
        float guideY = frameY;
        float guideWidth = frameWidth;
        float guideHeight = frameHeight;

        if (currentAspectRatio > targetAspectRatio) {
            guideWidth = frameHeight * targetAspectRatio;
            guideX += (frameWidth - guideWidth) * 0.5f;
        } else {
            guideHeight = frameWidth / targetAspectRatio;
            guideY += (frameHeight - guideHeight) * 0.5f;
        }

        ImDrawList drawList = ImGui.getBackgroundDrawList();
        int deadZoneColour = 0x66303030;
        int frameColour = 0xFFD7E36D;
        int innerFrameColour = 0x99D7E36D;

        drawList.addRectFilled(frameX, frameY, guideX, frameY + frameHeight, deadZoneColour);
        drawList.addRectFilled(guideX + guideWidth, frameY, frameX + frameWidth, frameY + frameHeight, deadZoneColour);
        drawList.addRectFilled(guideX, frameY, guideX + guideWidth, guideY, deadZoneColour);
        drawList.addRectFilled(guideX, guideY + guideHeight, guideX + guideWidth, frameY + frameHeight, deadZoneColour);

        drawList.addRect(guideX, guideY, guideX + guideWidth, guideY + guideHeight, frameColour, 0.0f, 0, 2.0f);
        drawList.addRect(guideX + 1.0f, guideY + 1.0f, guideX + guideWidth - 1.0f, guideY + guideHeight - 1.0f, innerFrameColour, 0.0f, 0, 1.0f);
    }
}
