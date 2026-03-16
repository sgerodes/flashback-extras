package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.keyframe.change.KeyframeChangeTickrate;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.KeyframeTrack;
import com.moulberry.flashback.state.RealTimeMapping;
import io.serge.flashbackextras.access.FlashbackExtrasEditorStateAccess;
import io.serge.flashbackextras.audio.AudioTimelineContext;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EditorState.class, remap = false)
public abstract class EditorStateMixin implements FlashbackExtrasEditorStateAccess {
    @Shadow private volatile transient int lastRealTimeMappingModCount;
    @Shadow private volatile transient RealTimeMapping realTimeMapping;
    @Shadow public volatile transient int modCount;

    @Shadow
    private EditorScene currentScene() {
        throw new AssertionError();
    }

    @Invoker("updateRealtimeMappingsIfNeeded")
    protected abstract void flashbackExtras$invokeUpdateRealtimeMappingsIfNeeded();

    @Override
    public @Nullable RealTimeMapping flashbackExtras$getRealTimeMapping() {
        this.flashbackExtras$invokeUpdateRealtimeMappingsIfNeeded();
        return this.realTimeMapping;
    }

    @Inject(method = "getFirstAndLastTicksInTracks", at = @At("HEAD"))
    private void flashbackExtras$pushRealtimeMapping(CallbackInfoReturnable<EditorState.StartAndEnd> cir) {
        AudioTimelineContext.push(this.flashbackExtras$getRealTimeMapping());
    }

    @Inject(method = "getFirstAndLastTicksInTracks", at = @At("RETURN"))
    private void flashbackExtras$popRealtimeMapping(CallbackInfoReturnable<EditorState.StartAndEnd> cir) {
        AudioTimelineContext.pop();
    }

    /**
     * @reason Keep realtime mappings continuous between timelapse segments for audio timeline math.
     */
    @Overwrite
    private void calculateRealtimeMappings() {
        this.lastRealTimeMappingModCount = this.modCount;
        this.realTimeMapping = new RealTimeMapping();

        List<KeyframeTrack> applicableTracks = new ArrayList<>();
        int start = -1;
        int end = -1;
        int lastApplicableKeyframe = -1;

        for (KeyframeTrack keyframeTrack : this.currentScene().keyframeTracks) {
            if (!keyframeTrack.enabled || keyframeTrack.keyframesByTick.isEmpty()) {
                continue;
            }

            Class<? extends KeyframeChange> keyframeChangeType = keyframeTrack.keyframeType.keyframeChangeType();
            if (keyframeChangeType == null || !KeyframeChangeTickrate.class.isAssignableFrom(keyframeChangeType)) {
                continue;
            }

            applicableTracks.add(keyframeTrack);

            int trackStart = keyframeTrack.keyframesByTick.firstKey();
            int trackEnd = keyframeTrack.keyframesByTick.lastKey();

            if (start < 0 || trackStart < start) {
                start = trackStart;
            }
            if (end < 0 || trackEnd > end) {
                end = trackEnd;
            }

            if (!keyframeTrack.keyframeType.neverApplyLastKeyframe()) {
                if (lastApplicableKeyframe < 0 || trackEnd > lastApplicableKeyframe) {
                    lastApplicableKeyframe = trackEnd;
                }
            }
        }

        if (applicableTracks.isEmpty() || start < 0 || end < 0) {
            return;
        }

        float lastSpeed = 1.0f;

        for (int tick = start; tick <= end; tick++) {
            boolean foundTickrate = false;
            for (KeyframeTrack keyframeTrack : applicableTracks) {
                KeyframeChange change = keyframeTrack.createKeyframeChange(tick, this.realTimeMapping);
                if (!(change instanceof KeyframeChangeTickrate changeTickrate)) {
                    continue;
                }

                float newSpeed = changeTickrate.tickrate() / 20.0f;
                if (newSpeed != lastSpeed) {
                    lastSpeed = newSpeed;
                    this.realTimeMapping.addMapping(tick, newSpeed);
                }
                foundTickrate = true;
                break;
            }
            if (!foundTickrate && lastSpeed != 1.0f) {
                lastSpeed = 1.0f;
                this.realTimeMapping.addMapping(tick, 1.0f);
            }
        }

        if (!FlashbackExtrasConfig.isAudioTimelineFixesEnabled()) {
            for (KeyframeTrack keyframeTrack : applicableTracks) {
                KeyframeChange change = keyframeTrack.createKeyframeChange(end + 1, this.realTimeMapping);
                if (!(change instanceof KeyframeChangeTickrate changeTickrate)) {
                    continue;
                }

                float newSpeed = changeTickrate.tickrate() / 20.0f;
                if (newSpeed != lastSpeed) {
                    this.realTimeMapping.addMapping(end + 1, newSpeed);
                }
                return;
            }

            for (KeyframeTrack keyframeTrack : applicableTracks) {
                if (!keyframeTrack.keyframeType.neverApplyLastKeyframe() && keyframeTrack.keyframesByTick.lastKey() == lastApplicableKeyframe) {
                    KeyframeChange change = keyframeTrack.createKeyframeChange(lastApplicableKeyframe, this.realTimeMapping);
                    if (!(change instanceof KeyframeChangeTickrate changeTickrate)) {
                        break;
                    }

                    float newSpeed = changeTickrate.tickrate() / 20.0f;
                    if (newSpeed != lastSpeed) {
                        this.realTimeMapping.addMapping(end + 1, newSpeed);
                    }
                    return;
                }
            }

            this.realTimeMapping.addMapping(end + 1, 1.0f);
            return;
        }

        for (KeyframeTrack keyframeTrack : applicableTracks) {
            KeyframeChange change = keyframeTrack.createKeyframeChange(end + 1, this.realTimeMapping);
            if (!(change instanceof KeyframeChangeTickrate changeTickrate)) {
                continue;
            }

            float newSpeed = changeTickrate.tickrate() / 20.0f;
            if (newSpeed != lastSpeed) {
                this.realTimeMapping.addMapping(end + 1, newSpeed);
            }
            return;
        }

        for (KeyframeTrack keyframeTrack : applicableTracks) {
            if (!keyframeTrack.keyframeType.neverApplyLastKeyframe() && keyframeTrack.keyframesByTick.lastKey() == lastApplicableKeyframe) {
                KeyframeChange change = keyframeTrack.createKeyframeChange(lastApplicableKeyframe, this.realTimeMapping);
                if (!(change instanceof KeyframeChangeTickrate changeTickrate)) {
                    break;
                }

                float newSpeed = changeTickrate.tickrate() / 20.0f;
                if (newSpeed != lastSpeed) {
                    this.realTimeMapping.addMapping(end + 1, newSpeed);
                }
                return;
            }
        }

        this.realTimeMapping.addMapping(end + 1, 1.0f);
    }
}
