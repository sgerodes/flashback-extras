package io.serge.flashbackextras.audio;

import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.state.KeyframeTrack;
import com.moulberry.flashback.state.RealTimeMapping;
import io.serge.flashbackextras.access.FlashbackExtrasEditorStateAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public final class AudioTimelineContext {
    private AudioTimelineContext() {
    }

    private record Scope(@Nullable RealTimeMapping mapping) {
    }

    private static final ThreadLocal<Deque<Scope>> CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    public static void push(@Nullable RealTimeMapping mapping) {
        CONTEXT.get().push(new Scope(mapping));
    }

    public static void pop() {
        Deque<Scope> stack = CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            CONTEXT.remove();
        }
    }

    public static @Nullable RealTimeMapping getCurrentOrFetch() {
        Deque<Scope> stack = CONTEXT.get();
        if (!stack.isEmpty()) {
            return stack.peek().mapping();
        }

        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {
            return null;
        }
        return ((FlashbackExtrasEditorStateAccess) editorState).flashbackExtras$getRealTimeMapping();
    }

    public static float getAudioDurationInTicks(float durationInTicks, int tick, @Nullable RealTimeMapping mapping) {
        if (mapping == null) {
            return durationInTicks;
        }

        float startRealTime = mapping.getRealTime(tick);
        float endRealTime = startRealTime + durationInTicks;
        float low = tick;
        float high = tick + Math.max(1.0f, durationInTicks);

        while (mapping.getRealTime(high) < endRealTime) {
            float delta = high - tick;
            if (delta >= 1_000_000f) {
                return delta;
            }
            high = tick + Math.max(delta * 2.0f, delta + 1.0f);
        }

        for (int i = 0; i < 24; i++) {
            float mid = (low + high) * 0.5f;
            if (mapping.getRealTime(mid) < endRealTime) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return Math.max(0.0f, high - tick);
    }

    public static int resolveKeyframeTick(Keyframe keyframe) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {
            return -1;
        }

        long stamp = editorState.acquireRead();
        try {
            EditorScene editorScene = editorState.getCurrentScene(stamp);
            for (KeyframeTrack keyframeTrack : editorScene.keyframeTracks) {
                for (Map.Entry<Integer, Keyframe> entry : keyframeTrack.keyframesByTick.entrySet()) {
                    if (entry.getValue() == keyframe) {
                        return entry.getKey();
                    }
                }
            }
        } finally {
            editorState.release(stamp);
        }

        return -1;
    }
}

