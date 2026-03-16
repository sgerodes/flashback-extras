package io.serge.flashbackextras.export;

import com.moulberry.flashback.combo_options.AudioCodec;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.configuration.FlashbackConfigV1;

import java.util.Arrays;
import java.util.List;

public final class ExportPreset {
    private boolean builtIn;
    private String name;
    private int[] resolution = new int[]{1920, 1080};
    private float[] framerate = new float[]{60};
    private boolean resetRng;
    private boolean ssaa;
    private boolean noGui;
    private VideoContainer container;
    private VideoCodec videoCodec;
    private int selectedVideoEncoder;
    private boolean useMaximumBitrate;
    private boolean recordAudio;
    private boolean transparentBackground;
    private AudioCodec audioCodec = AudioCodec.AAC;
    private boolean stereoAudio;
    private String defaultExportPath;
    private String bitrate = "20m";
    private String pngSequenceFormat = "%04d";

    public ExportPreset() {
    }

    public ExportPreset(String name) {
        this.name = name;
    }

    public static List<ExportPreset> builtIns() {
        return List.of(
            builtIn("YouTube 1080p60", 1920, 1080, 60, VideoContainer.MP4, VideoCodec.H264, true, true, false, "12m"),
            builtIn("YouTube 1440p60", 2560, 1440, 60, VideoContainer.MP4, VideoCodec.H264, true, true, false, "24m"),
            builtIn("YouTube 4K30", 3840, 2160, 30, VideoContainer.MP4, VideoCodec.H264, true, true, false, "40m"),
            builtIn("YouTube 4K60", 3840, 2160, 60, VideoContainer.MP4, VideoCodec.H264, true, true, false, "60m"),
            builtIn("YouTube Shorts 1080x1920", 1080, 1920, 60, VideoContainer.MP4, VideoCodec.H264, true, true, false, "12m"),
            builtIn("TikTok 1080x1920", 1080, 1920, 60, VideoContainer.MP4, VideoCodec.H264, true, true, false, "16m"),
            builtIn("Instagram Reels 1080x1920", 1080, 1920, 30, VideoContainer.MP4, VideoCodec.H264, true, true, false, "10m"),
            builtIn("Discord 720p30", 1280, 720, 30, VideoContainer.MP4, VideoCodec.H264, true, true, false, "8m"),
            builtIn("Discord 1080p30", 1920, 1080, 30, VideoContainer.MP4, VideoCodec.H264, true, true, false, "10m"),
            builtIn("GIF Preview", 960, 540, 20, VideoContainer.GIF, VideoCodec.GIF, false, false, false, "0")
        );
    }

    private static ExportPreset builtIn(String name, int width, int height, int fps, VideoContainer container,
                                        VideoCodec videoCodec, boolean recordAudio, boolean stereoAudio,
                                        boolean useMaximumBitrate, String bitrate) {
        ExportPreset preset = new ExportPreset(name);
        preset.builtIn = true;
        preset.resolution = new int[]{width, height};
        preset.framerate = new float[]{fps};
        preset.container = container;
        preset.videoCodec = videoCodec;
        preset.recordAudio = recordAudio;
        preset.stereoAudio = stereoAudio;
        preset.useMaximumBitrate = useMaximumBitrate;
        preset.audioCodec = AudioCodec.AAC;
        preset.bitrate = bitrate;
        if (container == VideoContainer.GIF) {
            preset.recordAudio = false;
            preset.stereoAudio = false;
        }
        return preset;
    }

    public static ExportPreset from(String name, FlashbackConfigV1.SubcategoryInternalExport config, String bitrate, String pngSequenceFormat) {
        ExportPreset preset = new ExportPreset(name);
        preset.resolution = Arrays.copyOf(config.resolution, 2);
        preset.framerate = Arrays.copyOf(config.framerate, 1);
        preset.resetRng = config.resetRng;
        preset.ssaa = config.ssaa;
        preset.noGui = config.noGui;
        preset.container = config.container;
        preset.videoCodec = config.videoCodec;
        preset.selectedVideoEncoder = config.selectedVideoEncoder != null && config.selectedVideoEncoder.length > 0 ? config.selectedVideoEncoder[0] : 0;
        preset.useMaximumBitrate = config.useMaximumBitrate;
        preset.recordAudio = config.recordAudio;
        preset.transparentBackground = config.transparentBackground;
        preset.audioCodec = config.audioCodec;
        preset.stereoAudio = config.stereoAudio;
        preset.defaultExportPath = config.defaultExportPath;
        preset.bitrate = bitrate;
        preset.pngSequenceFormat = pngSequenceFormat;
        return preset;
    }

    public void applyTo(FlashbackConfigV1.SubcategoryInternalExport config) {
        config.resolution = Arrays.copyOf(this.resolution, 2);
        config.framerate = Arrays.copyOf(this.framerate, 1);
        config.resetRng = this.resetRng;
        config.ssaa = this.ssaa;
        config.noGui = this.noGui;
        config.container = this.container;
        config.videoCodec = this.videoCodec;
        config.selectedVideoEncoder = new int[]{this.selectedVideoEncoder};
        config.useMaximumBitrate = this.useMaximumBitrate;
        config.recordAudio = this.recordAudio;
        config.transparentBackground = this.transparentBackground;
        config.audioCodec = this.audioCodec;
        config.stereoAudio = this.stereoAudio;
        config.defaultExportPath = this.defaultExportPath;
    }

    public boolean builtIn() {
        return this.builtIn;
    }

    public String name() {
        return this.name;
    }

    public String displayName() {
        return this.builtIn ? "[Built-in] " + this.name : "[Custom] " + this.name;
    }

    public String bitrate() {
        return this.bitrate;
    }

    public String pngSequenceFormat() {
        return this.pngSequenceFormat;
    }
}
