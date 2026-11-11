package io.serge.flashbackextras.export;

import com.moulberry.flashback.combo_options.AudioCodec;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void fromCopiesConfigValuesAndApplyToWritesIndependentArrays() throws ReflectiveOperationException {
        var encoderField = FlashbackConfigV1.SubcategoryInternalExport.class.getField("selectedVideoEncoder");
        boolean namedEncoder = encoderField.getType() == String.class;
        int[] legacyEncoder = {3};
        FlashbackConfigV1.SubcategoryInternalExport source = new FlashbackConfigV1.SubcategoryInternalExport();
        source.resolution = new int[]{2560, 1440};
        source.framerate = new float[]{59.94f};
        source.resetRng = true;
        source.ssaa = true;
        source.noGui = true;
        source.container = VideoContainer.MP4;
        source.videoCodec = VideoCodec.H264;
        encoderField.set(source, namedEncoder ? "libx264" : legacyEncoder);
        source.useMaximumBitrate = true;
        source.recordAudio = true;
        source.transparentBackground = true;
        source.audioCodec = AudioCodec.AAC;
        source.stereoAudio = true;
        source.defaultExportPath = "exports/test.mp4";

        ExportPreset preset = ExportPreset.from("Round Trip", source, "24m", "%05d");

        source.resolution[0] = 1;
        source.framerate[0] = 1.0f;
        legacyEncoder[0] = 9;
        encoderField.set(source, namedEncoder ? "h264_qsv" : legacyEncoder);

        FlashbackConfigV1.SubcategoryInternalExport applied = new FlashbackConfigV1.SubcategoryInternalExport();
        preset.applyTo(applied);

        assertEquals("Round Trip", preset.name());
        assertEquals("[Custom] Round Trip", preset.displayName());
        assertEquals("24m", preset.bitrate());
        assertEquals("%05d", preset.pngSequenceFormat());
        assertFalse(preset.builtIn());

        assertArrayEquals(new int[]{2560, 1440}, applied.resolution);
        assertArrayEquals(new float[]{59.94f}, applied.framerate, 0.0001f);
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
        if (namedEncoder) {
            assertEquals("libx264", encoderField.get(applied));
            encoderField.set(applied, "h264_qsv");
        } else {
            int[] appliedEncoder = (int[]) encoderField.get(applied);
            assertArrayEquals(new int[]{3}, appliedEncoder);
            appliedEncoder[0] = 7;
        }

        FlashbackConfigV1.SubcategoryInternalExport appliedAgain = new FlashbackConfigV1.SubcategoryInternalExport();
        preset.applyTo(appliedAgain);

        assertArrayEquals(new int[]{2560, 1440}, appliedAgain.resolution);
        assertArrayEquals(new float[]{59.94f}, appliedAgain.framerate, 0.0001f);
        if (namedEncoder) {
            assertEquals("libx264", encoderField.get(appliedAgain));
        } else {
            assertArrayEquals(new int[]{3}, (int[]) encoderField.get(appliedAgain));
        }
    }

    @Test
    void legacyPresetUsesDefaultEncoderOnFlashbackWithNamedEncoders() throws ReflectiveOperationException {
        var encoderField = FlashbackConfigV1.SubcategoryInternalExport.class.getField("selectedVideoEncoder");
        ExportPreset preset = new Gson().fromJson("""
            {"name":"Legacy","videoCodec":"H264","selectedVideoEncoder":3}
            """, ExportPreset.class);
        var config = new FlashbackConfigV1.SubcategoryInternalExport();

        preset.applyTo(config);

        assertEquals(VideoCodec.H264, config.videoCodec);
        if (encoderField.getType() == String.class) {
            assertNull(encoderField.get(config));
        } else {
            assertArrayEquals(new int[]{3}, (int[]) encoderField.get(config));
        }
    }

    @Test
    void builtInPresetsAlwaysHaveNames() {
        for (ExportPreset preset : ExportPreset.builtIns()) {
            assertNotNull(preset.name());
            assertFalse(preset.name().isBlank());
        }
    }
}
