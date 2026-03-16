package io.serge.flashbackextras.mixin;

import com.moulberry.flashback.keyframe.change.KeyframeChangePlayAudio;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.sound.FlashbackAudioBuffer;
import com.moulberry.flashback.sound.FlashbackAudioManager;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KeyframeChangePlayAudio.class, remap = false)
public abstract class KeyframeChangePlayAudioMixin {
    @Shadow @Final private FlashbackAudioBuffer audioBuffer;
    @Shadow @Final private int startTick;
    @Shadow @Final private float seconds;

    /**
     * @reason Keep audio playback anchored to editor realtime instead of live replay tickrate.
     */
    @Overwrite
    public void apply(KeyframeHandler keyframeHandler) {
        Minecraft minecraft = keyframeHandler.getMinecraft();
        if (minecraft != null && minecraft.level != null) {
            FlashbackAudioManager.playAt(((SoundManagerAccessor) minecraft.getSoundManager()).flashbackExtras$getSoundEngine(), this.audioBuffer, this.startTick,
                this.seconds, 1.0f);
        }
    }
}
