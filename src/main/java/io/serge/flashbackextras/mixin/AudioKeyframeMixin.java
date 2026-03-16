package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.impl.AudioKeyframe;
import com.moulberry.flashback.sound.FlashbackAudioBuffer;
import com.moulberry.flashback.state.RealTimeMapping;
import imgui.moulberry90.ImDrawList;
import io.serge.flashbackextras.audio.AudioTimelineContext;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.TreeMap;

@Mixin(value = AudioKeyframe.class, remap = false)
public abstract class AudioKeyframeMixin {
    @Shadow private FlashbackAudioBuffer audioBuffer;

    @Shadow
    private void ensureAudioBufferLoaded() {
        throw new AssertionError();
    }

    private float flashbackExtras$getDurationInTicks(int tick) {
        this.ensureAudioBufferLoaded();
        if (this.audioBuffer == FlashbackAudioBuffer.EMPTY) {
            return -1;
        }

        float durationInTicks = this.audioBuffer.durationInSeconds() * 20.0f;
        RealTimeMapping mapping = AudioTimelineContext.getCurrentOrFetch();
        return AudioTimelineContext.getAudioDurationInTicks(durationInTicks, tick, mapping);
    }

    /**
     * @reason Reflect realtime-mapped audio duration in timeline width calculations.
     */
    @Overwrite
    public float getCustomWidthInTicks() {
        this.ensureAudioBufferLoaded();
        if (this.audioBuffer == FlashbackAudioBuffer.EMPTY) {
            return -1;
        }

        if (!FlashbackExtrasConfig.isAudioTimelineFixesEnabled()) {
            return this.audioBuffer.durationInSeconds() * 20.0f;
        }

        int tick = AudioTimelineContext.resolveKeyframeTick((Keyframe) (Object) this);
        if (tick < 0) {
            return this.audioBuffer.durationInSeconds() * 20.0f;
        }
        return this.flashbackExtras$getDurationInTicks(tick);
    }

    /**
     * @reason Draw audio waveform lengths using realtime mapping so the visual block matches playback behavior.
     */
    @Overwrite
    public void drawOnTimeline(ImDrawList drawList, int keyframeSize, float x, float y, int colour,
                               float timelineScale, float minTimelineX, float maxTimelineX, int tick, TreeMap<Integer, Keyframe> keyframeTimes) {
        this.ensureAudioBufferLoaded();

        int alpha = colour & 0xFF000000;

        if (this.audioBuffer == FlashbackAudioBuffer.EMPTY) {
            drawList.addRectFilled(x - keyframeSize, y - keyframeSize, x + keyframeSize, y + keyframeSize, alpha | 0x155FFF);
            drawList.addText(x + keyframeSize + 4, y - keyframeSize, 0xFF155FFF, "Error loading audio");
            return;
        }

        float durationInTicks = FlashbackExtrasConfig.isAudioTimelineFixesEnabled()
            ? this.flashbackExtras$getDurationInTicks(tick)
            : this.audioBuffer.durationInSeconds() * 20.0f;
        int waveformLength = (int) (durationInTicks / timelineScale);
        int drawLength = waveformLength;

        var next = keyframeTimes.ceilingEntry(tick + 1);
        if (next != null) {
            int nextTick = next.getKey();
            drawLength = Math.min(drawLength, (int) ((nextTick - tick) / timelineScale));
        }

        int minSample = Math.max(0, (int) (minTimelineX - x));
        int maxSample = Math.min(drawLength, (int) (maxTimelineX - x));

        byte[] waveform = this.audioBuffer.getAveragedWaveform(waveformLength);
        drawList.addRectFilled(x, y - keyframeSize, x + drawLength, y + keyframeSize, alpha);
        drawList.addRect(x, y - keyframeSize - 1, x + drawLength, y + keyframeSize + 1, colour);
        for (int i = minSample; i < maxSample; i++) {
            float max = y + keyframeSize;
            float min = max - 2 * keyframeSize * ((int) waveform[i] - (int) Byte.MIN_VALUE) / 255f;
            drawList.addRectFilled(x + i, min, x + i + 1, max, alpha | 0xE37D77);
        }
    }
}
