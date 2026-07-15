package io.serge.flashbackextras.segment;

import com.moulberry.flashback.combo_options.AudioCodec;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.exporting.ExportSettings;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderSegmentTest {
    @Test
    void normalizeClampsSortsAndMergesOverlaps() {
        List<RenderSegment> normalized = RenderSegment.normalize(List.of(
            new RenderSegment(80, 120),
            new RenderSegment(10, -5),
            new RenderSegment(50, 40),
            new RenderSegment(115, 150)
        ), 130);

        assertEquals(3, normalized.size());
        assertSegment(normalized.get(0), 0, 10);
        assertSegment(normalized.get(1), 40, 50);
        assertSegment(normalized.get(2), 80, 130);
    }

    @Test
    void storeMaintainsNormalizedSegmentsAndTotalDuration() {
        RenderSegmentStore store = new RenderSegmentStore();

        store.addSegment(100, 120, 200);
        store.addSegment(110, 150, 200);
        store.addSegment(10, 20, 200);

        assertEquals(2, store.segments().size());
        assertSegment(store.segments().get(0), 10, 20);
        assertSegment(store.segments().get(1), 100, 150);
        assertEquals(60, store.totalDurationTicks());
    }

    @Test
    void partPathsUseStableNumberedNames() {
        Path videoPart = RenderSegmentExportCoordinator.buildPartPath(Path.of("exports", "replay.mp4"), 2, 12, VideoContainer.MP4);
        Path pngPart = RenderSegmentExportCoordinator.buildPartPath(Path.of("exports", "frames"), 1, 4, VideoContainer.PNG_SEQUENCE);

        assertEquals(Path.of("exports").toAbsolutePath().resolve("replay-part02.mp4"), videoPart);
        assertEquals(Path.of("exports").toAbsolutePath().resolve("frames-part01"), pngPart);
    }

    @Test
    void copyWithRangeAndOutputOnlyChangesRangeNameAndOutput() {
        ExportSettings base = new ExportSettings("Base", null,
            new Vec3(1, 2, 3), 45f, 15f,
            1920, 1080, 0, 200, 60,
            true, VideoContainer.MP4, VideoCodec.H264, "libx264", 12_000_000,
            false, true, false,
            true, true, AudioCodec.AAC,
            Path.of("exports", "base.mp4"), "%04d");

        ExportSettings copied = RenderSegmentExportCoordinator.copyWithRangeAndOutput(base, 40, 90,
            "Part", Path.of("exports", "part.mp4"));

        assertEquals("Part", copied.name());
        assertEquals(40, copied.startTick());
        assertEquals(90, copied.endTick());
        assertEquals(Path.of("exports", "part.mp4"), copied.output());
        assertEquals(base.resolutionX(), copied.resolutionX());
        assertEquals(base.framerate(), copied.framerate());
        assertEquals(base.codec(), copied.codec());
        assertEquals(base.audioCodec(), copied.audioCodec());
    }

    private static void assertSegment(RenderSegment segment, int startTick, int endTick) {
        assertEquals(startTick, segment.startTick());
        assertEquals(endTick, segment.endTick());
    }
}
