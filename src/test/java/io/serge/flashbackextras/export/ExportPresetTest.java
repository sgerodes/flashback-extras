package io.serge.flashbackextras.export;

import com.moulberry.flashback.combo_options.AudioCodec;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportPresetTest {
    @Test
    void builtInPresetsExposeStableCoreOptions() {
        List<ExportPreset> presets = ExportPreset.builtIns();

        assertEquals(10, presets.size());

        ExportPreset youtube1080 = presets.getFirst();
        assertTrue(youtube1080.builtIn());
        assertEquals("[Built-in] YouTube 1080p60", youtube1080.displayName());
        assertEquals("12m", youtube1080.bitrate());

        ExportPreset gifPreview = presets.getLast();
        assertEquals("[Built-in] GIF Preview", gifPreview.displayName());

        FlashbackConfigV1.SubcategoryInternalExport config = new FlashbackConfigV1.SubcategoryInternalExport();
        gifPreview.applyTo(config);

        assertEquals(VideoContainer.GIF, config.container);
        assertEquals(VideoCodec.GIF, config.videoCodec);
        assertFalse(config.recordAudio);
        assertFalse(config.stereoAudio);
    }

    @Test
    void fromCopiesConfigValuesAndApplyToWritesIndependentArrays() {
        FlashbackConfigV1.SubcategoryInternalExport source = new FlashbackConfigV1.SubcategoryInternalExport();
        source.resolution = new int[]{2560, 1440};
        source.framerate = new float[]{59.94f};
        source.resetRng = true;
        source.ssaa = true;
        source.noGui = true;
        source.container = VideoContainer.MP4;
        source.videoCodec = VideoCodec.H264;
        source.selectedVideoEncoder = new int[]{3};
        source.useMaximumBitrate = true;
        source.recordAudio = true;
        source.transparentBackground = true;
        source.audioCodec = AudioCodec.AAC;
        source.stereoAudio = true;
        source.defaultExportPath = "exports/test.mp4";

        ExportPreset preset = ExportPreset.from("Round Trip", source, "24m", "%05d");

        source.resolution[0] = 1;
        source.framerate[0] = 1.0f;
        source.selectedVideoEncoder[0] = 9;

        FlashbackConfigV1.SubcategoryInternalExport applied = new FlashbackConfigV1.SubcategoryInternalExport();
        preset.applyTo(applied);

        assertEquals("Round Trip", preset.name());
        assertEquals("[Custom] Round Trip", preset.displayName());
        assertEquals("24m", preset.bitrate());
        assertEquals("%05d", preset.pngSequenceFormat());
        assertFalse(preset.builtIn());

        assertArrayEquals(new int[]{2560, 1440}, applied.resolution);
        assertArrayEquals(new float[]{59.94f}, applied.framerate, 0.0001f);
        assertArrayEquals(new int[]{3}, applied.selectedVideoEncoder);
        assertTrue(applied.resetRng);
        assertTrue(applied.ssaa);
        assertTrue(applied.noGui);
        assertEquals(VideoContainer.MP4, applied.container);
        assertEquals(VideoCodec.H264, applied.videoCodec);
        assertTrue(applied.useMaximumBitrate);
        assertTrue(applied.recordAudio);
        assertTrue(applied.transparentBackground);
        assertEquals(AudioCodec.AAC, applied.audioCodec);
        assertTrue(applied.stereoAudio);
        assertEquals("exports/test.mp4", applied.defaultExportPath);

        applied.resolution[1] = 999;
        applied.framerate[0] = 30.0f;
        applied.selectedVideoEncoder[0] = 7;

        FlashbackConfigV1.SubcategoryInternalExport appliedAgain = new FlashbackConfigV1.SubcategoryInternalExport();
        preset.applyTo(appliedAgain);

        assertArrayEquals(new int[]{2560, 1440}, appliedAgain.resolution);
        assertArrayEquals(new float[]{59.94f}, appliedAgain.framerate, 0.0001f);
        assertArrayEquals(new int[]{3}, appliedAgain.selectedVideoEncoder);
    }

    @Test
    void builtInPresetsAlwaysHaveNames() {
        for (ExportPreset preset : ExportPreset.builtIns()) {
            assertNotNull(preset.name());
            assertFalse(preset.name().isBlank());
        }
    }
}
