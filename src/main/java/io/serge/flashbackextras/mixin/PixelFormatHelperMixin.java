package io.serge.flashbackextras.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moulberry.flashback.exporting.PixelFormatHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PixelFormatHelper.class, remap = false)
public abstract class PixelFormatHelperMixin {
    // The warmup, export windows and video writer all use these caches.
    // Use the same monitor for nested codec and container lookups.
    @WrapMethod(method = "getBestPixelFormat")
    private static int flashbackExtras$lockBestPixelFormat(String encoder, int sourceFormat, boolean transparent,
                                                           Operation<Integer> original) {
        synchronized (PixelFormatHelper.class) {
            return original.call(encoder, sourceFormat, transparent);
        }
    }

    @WrapMethod(method = "doesPixelFormatSupportTransparency")
    private static boolean flashbackExtras$lockPixelFormatTransparency(int format, Operation<Boolean> original) {
        synchronized (PixelFormatHelper.class) {
            return original.call(format);
        }
    }

    @WrapMethod(method = "pixelFormatToString")
    private static String flashbackExtras$lockPixelFormatName(int format, Operation<String> original) {
        synchronized (PixelFormatHelper.class) {
            return original.call(format);
        }
    }
}
