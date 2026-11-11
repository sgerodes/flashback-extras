package io.serge.flashbackextras.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moulberry.flashback.combo_options.AudioCodec;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.exporting.PixelFormatHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = VideoContainer.class, remap = false)
public abstract class VideoContainerMixin {
    @WrapMethod(method = "getSupportedVideoCodecs")
    private VideoCodec[] flashbackExtras$lockSupportedVideoCodecs(boolean transparent, Operation<VideoCodec[]> original) {
        synchronized (PixelFormatHelper.class) {
            return original.call(transparent);
        }
    }

    @WrapMethod(method = "getSupportedAudioCodecs")
    private AudioCodec[] flashbackExtras$lockSupportedAudioCodecs(Operation<AudioCodec[]> original) {
        synchronized (PixelFormatHelper.class) {
            return original.call();
        }
    }
}
