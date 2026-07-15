package io.serge.flashbackextras.segment;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.AudioCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.editor.ui.windows.ExportDoneWindow;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportJobQueue;
import com.moulberry.flashback.exporting.ExportSettings;
import com.moulberry.flashback.exporting.PixelFormatHelper;
import io.serge.flashbackextras.FlashbackExtras;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class RenderSegmentExportCoordinator {
    private static ActiveStitch activeStitch = null;

    private RenderSegmentExportCoordinator() {
    }

    public static boolean isStitchingBusy() {
        return activeStitch != null;
    }

    public static List<ExportSettings> createSeparateClipSettings(ExportSettings base, List<RenderSegment> segments) {
        List<ExportSettings> settings = new ArrayList<>();
        int count = segments.size();
        for (int i = 0; i < count; i++) {
            RenderSegment segment = segments.get(i);
            Path output = buildPartPath(base.output(), i + 1, count, base.container());
            String name = buildPartName(base.name(), output, i + 1, count);
            settings.add(copyWithRangeAndOutput(base, segment.startTick(), segment.endTick(), name, output));
        }
        return settings;
    }

    public static boolean startOrQueueSeparateClips(ExportSettings base, List<RenderSegment> segments, boolean startNow) {
        List<ExportSettings> jobs = createSeparateClipSettings(base, segments);
        if (jobs.isEmpty()) {
            return false;
        }
        startOrQueue(jobs, startNow);
        return true;
    }

    public static boolean startOrQueueStitchedVideo(ExportSettings base, List<RenderSegment> segments, boolean startNow) {
        if (base.container() == VideoContainer.PNG_SEQUENCE || activeStitch != null || segments.isEmpty()) {
            return false;
        }

        ActiveStitch stitch = ActiveStitch.create(base, segments);
        activeStitch = stitch;
        startOrQueue(stitch.partSettings, startNow);
        return true;
    }

    public static void tick() {
        ActiveStitch stitch = activeStitch;
        if (stitch == null) {
            return;
        }

        if (stitch.state == StitchState.RENDERING && Flashback.EXPORT_JOB == null && !stitch.hasPendingQueuedJobs()) {
            if (!stitch.allPartsExist()) {
                stitch.state = StitchState.FAILED;
                stitch.message = "Render segment stitch failed: one or more temporary clips are missing.";
                FlashbackExtras.LOGGER.error(stitch.message);
                return;
            }

            stitch.state = StitchState.STITCHING;
            Thread thread = new Thread(stitch::stitch, "Flashback Extras Segment Stitch");
            thread.setDaemon(true);
            thread.start();
        }

        if (stitch.state == StitchState.FINISHED) {
            if (stitch.finishedEntry != null) {
                ExportDoneWindow.addFinishedExportEntry(stitch.finishedEntry);
            }
            if (stitch.message != null) {
                ReplayUI.setInfoOverlayShort(stitch.message);
            }
            activeStitch = null;
        } else if (stitch.state == StitchState.FAILED) {
            if (stitch.message != null) {
                ReplayUI.setInfoOverlayShort(stitch.message);
            }
            activeStitch = null;
        }
    }

    public static Path buildPartPath(Path baseOutput, int partIndex, int partCount, VideoContainer container) {
        int digits = Math.max(2, String.valueOf(partCount).length());
        String suffix = "-part" + String.format("%0" + digits + "d", partIndex);

        if (container == VideoContainer.PNG_SEQUENCE) {
            Path absolute = baseOutput.toAbsolutePath();
            Path parent = absolute.getParent();
            String folderName = absolute.getFileName().toString() + suffix;
            return parent == null ? Path.of(folderName) : parent.resolve(folderName);
        }

        Path absolute = baseOutput.toAbsolutePath();
        Path parent = absolute.getParent();
        String fileName = absolute.getFileName().toString();
        String extension = "." + container.extension();
        String stem = fileName.endsWith(extension) ? fileName.substring(0, fileName.length() - extension.length()) : fileName;
        Path path = Path.of(stem + suffix + extension);
        return parent == null ? path : parent.resolve(path);
    }

    public static String buildPartName(String baseName, Path output, int partIndex, int partCount) {
        String name = baseName;
        if (name == null || name.isBlank()) {
            Path fileName = output.getFileName();
            name = fileName == null ? "Segment" : fileName.toString();
        }
        int digits = Math.max(2, String.valueOf(partCount).length());
        return name + " Part " + String.format("%0" + digits + "d", partIndex);
    }

    public static ExportSettings copyWithRangeAndOutput(ExportSettings base, int startTick, int endTick, String name, Path output) {
        return new ExportSettings(name, base.editorState(),
            base.initialCameraPosition(), base.initialCameraYaw(), base.initialCameraPitch(),
            base.resolutionX(), base.resolutionY(), startTick, endTick, base.framerate(), base.resetRng(),
            base.container(), base.codec(), base.encoder(), base.bitrate(), base.transparent(), base.ssaa(), base.noGui(),
            base.recordAudio(), base.stereoAudio(), base.audioCodec(),
            output, base.pngSequenceFormat());
    }

    private static void startOrQueue(List<ExportSettings> jobs, boolean startNow) {
        if (startNow && Flashback.EXPORT_JOB == null) {
            Flashback.EXPORT_JOB = new ExportJob(jobs.getFirst());
            if (jobs.size() > 1) {
                ExportJobQueue.queuedJobs.addAll(jobs.subList(1, jobs.size()));
                ExportJobQueue.drainingQueue = true;
            }
            return;
        }

        ExportJobQueue.queuedJobs.addAll(jobs);
        if (startNow) {
            ExportJobQueue.drainingQueue = true;
        }
    }

    private static final class ActiveStitch {
        private final ExportSettings finalSettings;
        private final List<ExportSettings> partSettings;
        private final List<Path> partPaths;
        private final Path tempDir;
        private final int durationTicks;

        private volatile StitchState state = StitchState.RENDERING;
        private volatile String message = null;
        private volatile ExportDoneWindow.FinishedExportEntry finishedEntry = null;

        private ActiveStitch(ExportSettings finalSettings, List<ExportSettings> partSettings, Path tempDir, int durationTicks) {
            this.finalSettings = finalSettings;
            this.partSettings = partSettings;
            this.partPaths = partSettings.stream().map(ExportSettings::output).toList();
            this.tempDir = tempDir;
            this.durationTicks = durationTicks;
        }

        private static ActiveStitch create(ExportSettings base, List<RenderSegment> segments) {
            Path output = base.output().toAbsolutePath();
            Path parent = output.getParent();
            if (parent == null) {
                parent = Path.of(".").toAbsolutePath();
            }
            Path tempDir = parent.resolve(".flashback-extras-segments").resolve(UUID.randomUUID().toString());

            List<ExportSettings> partSettings = new ArrayList<>();
            int durationTicks = 0;
            for (int i = 0; i < segments.size(); i++) {
                RenderSegment segment = segments.get(i);
                durationTicks += segment.durationTicks();
                Path tempOutput = tempDir.resolve("part" + String.format("%02d", i + 1) + "." + base.container().extension());
                String name = buildPartName(base.name(), tempOutput, i + 1, segments.size());
                partSettings.add(copyWithRangeAndOutput(base, segment.startTick(), segment.endTick(), name, tempOutput));
            }

            return new ActiveStitch(base, partSettings, tempDir, durationTicks);
        }

        private boolean hasPendingQueuedJobs() {
            for (ExportSettings queuedJob : ExportJobQueue.queuedJobs) {
                if (this.partPaths.contains(queuedJob.output())) {
                    return true;
                }
            }
            return false;
        }

        private boolean allPartsExist() {
            for (Path partPath : this.partPaths) {
                if (!Files.exists(partPath)) {
                    return false;
                }
            }
            return true;
        }

        private void stitch() {
            try {
                Files.createDirectories(this.finalSettings.output().toAbsolutePath().getParent());
                FFmpegLogCallback.set();

                int audioChannels = 0;
                if (this.finalSettings.recordAudio()) {
                    AudioCodec audioCodec = this.finalSettings.audioCodec();
                    audioChannels = audioCodec == AudioCodec.VORBIS || this.finalSettings.stereoAudio() ? 2 : 1;
                }

                try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(this.finalSettings.output().toString(),
                    this.finalSettings.resolutionX(), this.finalSettings.resolutionY(), audioChannels)) {
                    recorder.setVideoBitrate(this.finalSettings.bitrate());
                    recorder.setVideoCodec(this.finalSettings.codec().codecId());
                    recorder.setVideoCodecName(this.finalSettings.encoder());
                    recorder.setFormat(this.finalSettings.container().extension());
                    recorder.setFrameRate(this.finalSettings.framerate());
                    recorder.setPixelFormat(PixelFormatHelper.getBestPixelFormat(this.finalSettings.encoder(), this.finalSettings.transparent()));
                    recorder.setGopSize((int) Math.max(20, Math.min(240, Math.ceil(this.finalSettings.framerate() * 2))));

                    if (this.finalSettings.recordAudio()) {
                        recorder.setAudioCodec(this.finalSettings.audioCodec().codecId());
                        recorder.setSampleRate(48000);
                        recorder.setAudioBitrate(256000);
                    }

                    recorder.start();
                    for (Path partPath : this.partPaths) {
                        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(partPath.toFile())) {
                            grabber.start();
                            Frame frame;
                            while ((frame = grabber.grab()) != null) {
                                recorder.record(frame);
                            }
                            grabber.stop();
                        }
                    }
                    recorder.stop();
                }

                long fileSize = Files.exists(this.finalSettings.output()) ? Files.size(this.finalSettings.output()) : 0L;
                this.finishedEntry = new ExportDoneWindow.FinishedExportEntry(this.finalSettings, null, this.durationTicks / 20.0, fileSize);
                this.deleteTempDir();
                this.message = "Finished stitched render segments export.";
                this.state = StitchState.FINISHED;
            } catch (Throwable t) {
                FlashbackExtras.LOGGER.error("Failed to stitch render segment exports. Temporary clips were left at {}", this.tempDir, t);
                this.message = "Render segment stitching failed. Temporary clips were left in the export folder.";
                this.state = StitchState.FAILED;
            }
        }

        private void deleteTempDir() {
            try (var stream = Files.walk(this.tempDir)) {
                List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            } catch (IOException e) {
                FlashbackExtras.LOGGER.warn("Failed to delete temporary segment directory {}", this.tempDir, e);
            }
        }
    }

    private enum StitchState {
        RENDERING,
        STITCHING,
        FINISHED,
        FAILED
    }
}
