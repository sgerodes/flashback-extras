package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.editor.ui.windows.TimelineWindow;
import com.moulberry.flashback.state.EditorState;
import io.serge.flashbackextras.access.FlashbackExtrasEditorStateAccess;
import io.serge.flashbackextras.audio.AudioTimelineContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TimelineWindow.class, remap = false)
public abstract class TimelineWindowAudioMixin {
    @Shadow private static EditorState editorState;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/moulberry/flashback/state/EditorState;acquireRead()J", shift = At.Shift.BEFORE))
    private static void flashbackExtras$pushRealtimeMapping(CallbackInfo ci) {
        AudioTimelineContext.push(editorState == null ? null : ((FlashbackExtrasEditorStateAccess) editorState).flashbackExtras$getRealTimeMapping());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private static void flashbackExtras$popRealtimeMapping(CallbackInfo ci) {
        AudioTimelineContext.pop();
    }
}

