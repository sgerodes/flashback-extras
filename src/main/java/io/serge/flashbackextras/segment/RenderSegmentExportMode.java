package io.serge.flashbackextras.segment;

import net.minecraft.client.resources.language.I18n;

public enum RenderSegmentExportMode {
    NORMAL("flashbackextras.render_segments.mode.normal"),
    SEPARATE_CLIPS("flashbackextras.render_segments.mode.separate_clips"),
    STITCHED_VIDEO("flashbackextras.render_segments.mode.stitched_video");

    private final String translationKey;

    RenderSegmentExportMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String displayName() {
        return I18n.get(this.translationKey);
    }
}
