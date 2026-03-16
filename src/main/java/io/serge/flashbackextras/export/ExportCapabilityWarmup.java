package io.serge.flashbackextras.export;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import io.serge.flashbackextras.FlashbackExtras;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class ExportCapabilityWarmup {
    private static final long DELAY_MILLIS = 3_000L;
    private static final Object LOCK = new Object();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(new WarmupThreadFactory());

    private static volatile Status status = Status.IDLE;
    private static volatile boolean replayWasActive = false;
    private static volatile long sessionId = 0L;
    private static ScheduledFuture<?> scheduledWarmup = null;

    private ExportCapabilityWarmup() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static Status getStatus() {
        return status;
    }

    public static void onConfigChanged() {
        boolean replayActive = Flashback.isInReplay();
        replayWasActive = replayActive;

        if (!FlashbackExtrasConfig.isExportWarmupEnabled()) {
            reset();
            return;
        }

        if (replayActive && status == Status.IDLE) {
            scheduleWarmup();
        }
    }

    private static void tick() {
        boolean replayActive = Flashback.isInReplay();
        if (!FlashbackExtrasConfig.isExportWarmupEnabled()) {
            replayWasActive = replayActive;
            if (status != Status.IDLE) {
                reset();
            }
            return;
        }

        if (replayActive != replayWasActive) {
            replayWasActive = replayActive;
            if (replayActive) {
                scheduleWarmup();
            } else {
                reset();
            }
        }
    }

    private static void scheduleWarmup() {
        synchronized (LOCK) {
            sessionId += 1;
            long scheduledSessionId = sessionId;
            cancelScheduledWarmupLocked();
            status = Status.SCHEDULED;
            scheduledWarmup = EXECUTOR.schedule(() -> runWarmup(scheduledSessionId), DELAY_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private static void reset() {
        synchronized (LOCK) {
            sessionId += 1;
            cancelScheduledWarmupLocked();
            status = Status.IDLE;
        }
    }

    private static void runWarmup(long scheduledSessionId) {
        synchronized (LOCK) {
            if (sessionId != scheduledSessionId || !Flashback.isInReplay() || !FlashbackExtrasConfig.isExportWarmupEnabled()) {
                if (sessionId == scheduledSessionId) {
                    status = Status.IDLE;
                }
                scheduledWarmup = null;
                return;
            }

            status = Status.RUNNING;
            scheduledWarmup = null;
        }

        try {
            warmCapabilities();

            synchronized (LOCK) {
                if (sessionId == scheduledSessionId && Flashback.isInReplay() && FlashbackExtrasConfig.isExportWarmupEnabled()) {
                    status = Status.READY;
                } else if (sessionId == scheduledSessionId) {
                    status = Status.IDLE;
                }
            }
        } catch (Throwable t) {
            FlashbackExtras.LOGGER.warn("Failed to warm Flashback export capabilities in the background", t);

            synchronized (LOCK) {
                if (sessionId == scheduledSessionId && Flashback.isInReplay() && FlashbackExtrasConfig.isExportWarmupEnabled()) {
                    status = Status.FAILED;
                } else if (sessionId == scheduledSessionId) {
                    status = Status.IDLE;
                }
            }
        }
    }

    private static void warmCapabilities() {
        Set<VideoCodec> warmedCodecs = new HashSet<>();

        warmContainers(VideoContainer.findSupportedContainers(false), false, warmedCodecs);
        warmContainers(VideoContainer.findSupportedContainers(true), true, warmedCodecs);
    }

    private static void warmContainers(VideoContainer[] containers, boolean transparency, Set<VideoCodec> warmedCodecs) {
        for (VideoContainer container : containers) {
            VideoCodec[] codecs = container.getSupportedVideoCodecs(transparency);
            if (container != VideoContainer.PNG_SEQUENCE) {
                container.getSupportedAudioCodecs();
            }

            for (VideoCodec codec : codecs) {
                if (warmedCodecs.add(codec)) {
                    codec.getEncoders();
                }
            }
        }
    }

    private static void cancelScheduledWarmupLocked() {
        if (scheduledWarmup != null) {
            scheduledWarmup.cancel(false);
            scheduledWarmup = null;
        }
    }

    public enum Status {
        IDLE,
        SCHEDULED,
        RUNNING,
        READY,
        FAILED
    }

    private static final class WarmupThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "flashback-extras-export-warmup");
            thread.setDaemon(true);
            return thread;
        }
    }
}
