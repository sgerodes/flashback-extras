package io.serge.flashbackextras.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.exporting.PixelFormatHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = VideoCodec.class, remap = false)
public abstract class VideoCodecMixin {
    @WrapMethod(method = "getEncoders")
    private String[] flashbackExtras$lockEncoderProbe(Operation<String[]> original) {
        synchronized (PixelFormatHelper.class) {
            return original.call();
        }
    }

    @WrapMethod(method = "supportsTransparency")
    private boolean flashbackExtras$lockCodecTransparency(Operation<Boolean> original) {
        synchronized (PixelFormatHelper.class) {
            return original.call();
        }
    }
}
