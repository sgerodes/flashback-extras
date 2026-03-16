package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.impl.AudioKeyframe;
import com.moulberry.flashback.keyframe.types.AudioKeyframeType;
import com.moulberry.flashback.state.RealTimeMapping;
import io.serge.flashbackextras.audio.AudioTimelineContext;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Map;
import java.util.TreeMap;

@Mixin(value = AudioKeyframeType.class, remap = false)
public abstract class AudioKeyframeTypeMixin {
    /**
     * @reason Base audio seek position on realtime mapping when speed or timelapse keyframes remap ticks.
     */
    @Overwrite
    public KeyframeChange customKeyframeChange(TreeMap<Integer, Keyframe> keyframes, float tick) {
        Map.Entry<Integer, Keyframe> entry = keyframes.floorEntry((int) tick);
        if (entry == null) {
            return null;
        }

        float seconds;
        if (FlashbackExtrasConfig.isAudioTimelineFixesEnabled()) {
            RealTimeMapping mapping = AudioTimelineContext.getCurrentOrFetch();
            if (mapping != null) {
                seconds = (mapping.getRealTime(tick) - mapping.getRealTime(entry.getKey())) / 20.0f;
            } else {
                seconds = (tick - entry.getKey()) / 20.0f;
            }
        } else {
            seconds = (tick - entry.getKey()) / 20.0f;
        }

        AudioKeyframe audioKeyframe = (AudioKeyframe) entry.getValue();
        return audioKeyframe.createAudioChange(entry.getKey(), seconds);
    }
}
