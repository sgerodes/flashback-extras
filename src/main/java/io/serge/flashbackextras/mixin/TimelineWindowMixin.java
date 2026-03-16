package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.editor.ui.windows.TimelineWindow;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.moulberry90.ImGui;
import imgui.moulberry90.flag.ImGuiPopupFlags;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TimelineWindow.class, remap = false)
public abstract class TimelineWindowMixin {
    @Shadow private static float mouseX;
    @Shadow private static float mouseY;
    @Shadow private static float x;
    @Shadow private static float y;
    @Shadow private static float width;
    @Shadow private static float height;
    @Shadow private static int middleX;

    @Inject(
        method = "renderInner",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moulberry/flashback/editor/ui/windows/TimelineWindow;handleKeyPresses(Lcom/moulberry/flashback/playback/ReplayServer;II)V",
            shift = At.Shift.BEFORE
        )
    )
    private static void flashbackExtras$handleHorizontalScroll(ReplayServer replayServer, FlashbackMeta metadata, CallbackInfo ci) {
        if (!FlashbackExtrasConfig.isTimelineHorizontalScrollEnabled()) {
            return;
        }

        boolean shouldProcessInput = !ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup) && !ReplayUI.getIO().getWantTextInput();
        if (!shouldProcessInput) {
            return;
        }

        float horizontalScroll = ReplayUI.getIO().getMouseWheelH();
        if (horizontalScroll == 0) {
            return;
        }

        if (mouseX <= x + middleX || mouseX >= x + width || mouseY <= y || mouseY >= y + height) {
            return;
        }

        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {
            return;
        }

        double zoomSize = editorState.zoomMax - editorState.zoomMin;
        double panAmount = zoomSize * 0.05 * -horizontalScroll;
        editorState.zoomMin = Math.max(0, Math.min(1 - zoomSize, editorState.zoomMin + panAmount));
        editorState.zoomMax = editorState.zoomMin + zoomSize;
        editorState.markDirty();
    }
}
