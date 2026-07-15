package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.editor.ui.windows.TimelineWindow;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.moulberry90.ImDrawList;
import imgui.moulberry90.ImGui;
import imgui.moulberry90.flag.ImGuiMouseButton;
import imgui.moulberry90.flag.ImGuiMouseCursor;
import imgui.moulberry90.flag.ImGuiPopupFlags;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import io.serge.flashbackextras.segment.RenderSegment;
import io.serge.flashbackextras.segment.RenderSegmentStore;
import net.minecraft.client.resources.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = TimelineWindow.class, remap = false)
public abstract class TimelineWindowMixin {
    @Shadow private static float mouseX;
    @Shadow private static float mouseY;
    @Shadow private static float x;
    @Shadow private static float y;
    @Shadow private static float width;
    @Shadow private static float height;
    @Shadow private static int middleX;
    @Shadow private static int middleY;
    @Shadow private static int timestampHeight;

    @Shadow
    private static int replayTickToTimelineX(int tick) {
        throw new AssertionError();
    }

    @Shadow
    private static int timelineXToReplayTick(float x) {
        throw new AssertionError();
    }

    @Shadow
    private static void handleClick(ReplayServer replayServer, int totalTicks, float contentY) {
        throw new AssertionError();
    }

    @Unique private static SegmentDragMode flashbackExtras$segmentDragMode = SegmentDragMode.NONE;
    @Unique private static int flashbackExtras$segmentDragIndex = -1;
    @Unique private static int flashbackExtras$segmentDragAnchorTick = 0;
    @Unique private static int flashbackExtras$segmentDragStartTick = 0;
    @Unique private static int flashbackExtras$segmentDragEndTick = 0;
    @Unique private static int flashbackExtras$segmentPopupIndex = -1;

    @Inject(
        method = "renderInner",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moulberry/flashback/editor/ui/windows/TimelineWindow;handleKeyPresses(Lcom/moulberry/flashback/playback/ReplayServer;II)V",
            shift = At.Shift.BEFORE
        )
    )
    private static void flashbackExtras$handleHorizontalScroll(ReplayServer replayServer, FlashbackMeta metadata, CallbackInfo ci) {
        flashbackExtras$handleRenderSegmentInput(replayServer, metadata);

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

    @Inject(
        method = "renderInner",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moulberry/flashback/editor/ui/windows/TimelineWindow;renderPlaybackHead(IFIFIILimgui/moulberry90/ImDrawList;FIIFI)V",
            shift = At.Shift.BEFORE
        )
    )
    private static void flashbackExtras$renderRenderSegments(ReplayServer replayServer, FlashbackMeta metadata, CallbackInfo ci) {
        if (!FlashbackExtrasConfig.isRenderSegmentsEnabled()) {
            return;
        }

        RenderSegmentStore store = FlashbackExtrasConfig.getRenderSegmentStore(metadata.replayIdentifier);
        int totalTicks = replayServer.getTotalReplayTicks();
        store.normalize(totalTicks);

        ImDrawList drawList = ImGui.getWindowDrawList();
        float laneTop = flashbackExtras$laneTop();
        float laneBottom = flashbackExtras$laneBottom();
        float laneMidY = (laneTop + laneBottom) / 2f;

        drawList.addText(x + 8, laneTop - 1, 0xFFB0D8FF, I18n.get("flashbackextras.render_segments.lane"));
        drawList.addRectFilled(x + middleX + 1, laneTop, x + width, laneBottom, 0x40202020);

        List<RenderSegment> segments = store.segments();
        for (int i = 0; i < segments.size(); i++) {
            RenderSegment segment = segments.get(i);
            flashbackExtras$drawSegment(drawList, segment.startTick(), segment.endTick(), laneTop, laneBottom, 0x904FC3F7, 0xFF4FC3F7);

            if (flashbackExtras$isMouseInSegment(segment) && flashbackExtras$isMouseInSegmentLane()) {
                ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
                ImGui.setTooltip(I18n.get("flashbackextras.render_segments.drag_hint"));
            }
        }

        if (flashbackExtras$segmentDragMode != SegmentDragMode.NONE) {
            flashbackExtras$drawSegment(drawList, flashbackExtras$segmentDragStartTick, flashbackExtras$segmentDragEndTick,
                laneTop, laneBottom, 0xB0FFB74D, 0xFFFFB74D);
        }

        if (ImGui.beginPopup("##FlashbackExtrasRenderSegmentPopup")) {
            if (flashbackExtras$segmentPopupIndex >= 0 && flashbackExtras$segmentPopupIndex < segments.size()) {
                RenderSegment segment = segments.get(flashbackExtras$segmentPopupIndex);
                if (ImGui.menuItem(I18n.get("flashbackextras.render_segments.jump_start"))) {
                    replayServer.goToReplayTick(segment.startTick());
                    ImGui.closeCurrentPopup();
                }
                if (ImGui.menuItem(I18n.get("flashbackextras.render_segments.delete"))) {
                    store.removeSegment(flashbackExtras$segmentPopupIndex, totalTicks);
                    FlashbackExtrasConfig.saveRenderSegments();
                    flashbackExtras$segmentPopupIndex = -1;
                    ImGui.closeCurrentPopup();
                }
            } else {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        if (mouseY >= laneTop && mouseY <= laneBottom && mouseX >= x + middleX && mouseX <= x + width) {
            drawList.addLine(mouseX, laneTop, mouseX, laneBottom, 0x80FFFFFF);
            drawList.addCircleFilled(mouseX, laneMidY, ReplayUI.scaleUi(2), 0xFFFFFFFF);
        }
    }

    @Redirect(
        method = "renderInner",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moulberry/flashback/editor/ui/windows/TimelineWindow;handleClick(Lcom/moulberry/flashback/playback/ReplayServer;IF)V"
        )
    )
    private static void flashbackExtras$redirectHandleClick(ReplayServer replayServer, int totalTicks, float contentY) {
        if (FlashbackExtrasConfig.isRenderSegmentsEnabled() &&
            (flashbackExtras$isMouseInSegmentLane() || flashbackExtras$segmentDragMode != SegmentDragMode.NONE)) {
            return;
        }
        handleClick(replayServer, totalTicks, contentY);
    }

    @Unique
    private static void flashbackExtras$handleRenderSegmentInput(ReplayServer replayServer, FlashbackMeta metadata) {
        if (!FlashbackExtrasConfig.isRenderSegmentsEnabled()) {
            return;
        }

        boolean shouldProcessInput = !ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup) && !ReplayUI.getIO().getWantTextInput();
        if (!shouldProcessInput && flashbackExtras$segmentDragMode == SegmentDragMode.NONE) {
            return;
        }

        int totalTicks = replayServer.getTotalReplayTicks();
        RenderSegmentStore store = FlashbackExtrasConfig.getRenderSegmentStore(metadata.replayIdentifier);
        store.normalize(totalTicks);

        if (flashbackExtras$segmentDragMode != SegmentDragMode.NONE) {
            int targetTick = timelineXToReplayTick(mouseX - x);
            if (flashbackExtras$segmentDragMode == SegmentDragMode.RESIZE_LEFT ||
                flashbackExtras$segmentDragMode == SegmentDragMode.CREATE) {
                flashbackExtras$segmentDragStartTick = targetTick;
                flashbackExtras$segmentDragEndTick = flashbackExtras$segmentDragAnchorTick;
            } else if (flashbackExtras$segmentDragMode == SegmentDragMode.RESIZE_RIGHT) {
                flashbackExtras$segmentDragStartTick = flashbackExtras$segmentDragAnchorTick;
                flashbackExtras$segmentDragEndTick = targetTick;
            }

            if (!ImGui.isMouseDown(ImGuiMouseButton.Left)) {
                if (flashbackExtras$segmentDragMode == SegmentDragMode.CREATE) {
                    store.addSegment(flashbackExtras$segmentDragStartTick, flashbackExtras$segmentDragEndTick, totalTicks);
                } else {
                    store.replaceSegment(flashbackExtras$segmentDragIndex, flashbackExtras$segmentDragStartTick,
                        flashbackExtras$segmentDragEndTick, totalTicks);
                }
                FlashbackExtrasConfig.saveRenderSegments();
                flashbackExtras$segmentDragMode = SegmentDragMode.NONE;
                flashbackExtras$segmentDragIndex = -1;
            }
            return;
        }

        if (!flashbackExtras$isMouseInSegmentLane()) {
            return;
        }

        if (ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
            flashbackExtras$segmentPopupIndex = flashbackExtras$findSegmentAtMouse(store.segments());
            if (flashbackExtras$segmentPopupIndex >= 0) {
                ImGui.openPopup("##FlashbackExtrasRenderSegmentPopup");
            }
            return;
        }

        if (!ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            return;
        }

        List<RenderSegment> segments = store.segments();
        int handleIndex = flashbackExtras$findSegmentHandleAtMouse(segments);
        if (handleIndex >= 0) {
            RenderSegment segment = segments.get(handleIndex);
            flashbackExtras$segmentDragIndex = handleIndex;
            if (Math.abs(mouseX - (x + replayTickToTimelineX(segment.startTick()))) <= ReplayUI.scaleUi(6)) {
                flashbackExtras$segmentDragMode = SegmentDragMode.RESIZE_LEFT;
                flashbackExtras$segmentDragAnchorTick = segment.endTick();
            } else {
                flashbackExtras$segmentDragMode = SegmentDragMode.RESIZE_RIGHT;
                flashbackExtras$segmentDragAnchorTick = segment.startTick();
            }
            flashbackExtras$segmentDragStartTick = segment.startTick();
            flashbackExtras$segmentDragEndTick = segment.endTick();
            return;
        }

        int segmentIndex = flashbackExtras$findSegmentAtMouse(segments);
        if (segmentIndex >= 0) {
            replayServer.goToReplayTick(timelineXToReplayTick(mouseX - x));
            return;
        }

        int tick = timelineXToReplayTick(mouseX - x);
        flashbackExtras$segmentDragMode = SegmentDragMode.CREATE;
        flashbackExtras$segmentDragIndex = -1;
        flashbackExtras$segmentDragAnchorTick = tick;
        flashbackExtras$segmentDragStartTick = tick;
        flashbackExtras$segmentDragEndTick = tick;
    }

    @Unique
    private static void flashbackExtras$drawSegment(ImDrawList drawList, int rawStart, int rawEnd, float top, float bottom,
                                                    int fillColour, int lineColour) {
        int start = Math.min(rawStart, rawEnd);
        int end = Math.max(rawStart, rawEnd);
        float startX = x + replayTickToTimelineX(start);
        float endX = x + replayTickToTimelineX(end);
        if (endX <= x + middleX || startX >= x + width) {
            return;
        }
        startX = Math.max(startX, x + middleX + 1);
        endX = Math.min(endX, x + width);
        drawList.addRectFilled(startX, top, endX, bottom, fillColour);
        drawList.addRect(startX, top, endX, bottom, lineColour);
        drawList.addLine(startX, top, startX, bottom, lineColour, ReplayUI.scaleUi(2));
        drawList.addLine(endX, top, endX, bottom, lineColour, ReplayUI.scaleUi(2));
    }

    @Unique
    private static int flashbackExtras$findSegmentAtMouse(List<RenderSegment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            if (flashbackExtras$isMouseInSegment(segments.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private static int flashbackExtras$findSegmentHandleAtMouse(List<RenderSegment> segments) {
        float tolerance = ReplayUI.scaleUi(6);
        for (int i = 0; i < segments.size(); i++) {
            RenderSegment segment = segments.get(i);
            float startX = x + replayTickToTimelineX(segment.startTick());
            float endX = x + replayTickToTimelineX(segment.endTick());
            if (mouseX >= startX - tolerance && mouseX <= startX + tolerance ||
                mouseX >= endX - tolerance && mouseX <= endX + tolerance) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private static boolean flashbackExtras$isMouseInSegment(RenderSegment segment) {
        float startX = x + replayTickToTimelineX(segment.startTick());
        float endX = x + replayTickToTimelineX(segment.endTick());
        return mouseX >= Math.min(startX, endX) && mouseX <= Math.max(startX, endX);
    }

    @Unique
    private static boolean flashbackExtras$isMouseInSegmentLane() {
        return mouseX >= x + middleX && mouseX <= x + width && mouseY >= flashbackExtras$laneTop() && mouseY <= flashbackExtras$laneBottom();
    }

    @Unique
    private static float flashbackExtras$laneTop() {
        return y + timestampHeight + Math.max(2, (middleY - timestampHeight) / 2f);
    }

    @Unique
    private static float flashbackExtras$laneBottom() {
        return y + middleY - 2;
    }

    @Unique
    private enum SegmentDragMode {
        NONE,
        CREATE,
        RESIZE_LEFT,
        RESIZE_RIGHT
    }
}
