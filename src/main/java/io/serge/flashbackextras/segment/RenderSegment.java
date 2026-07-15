package io.serge.flashbackextras.segment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RenderSegment {
    private int startTick;
    private int endTick;

    public RenderSegment() {
    }

    public RenderSegment(int startTick, int endTick) {
        this.startTick = startTick;
        this.endTick = endTick;
    }

    public int startTick() {
        return this.startTick;
    }

    public int endTick() {
        return this.endTick;
    }

    public int durationTicks() {
        return Math.max(0, this.endTick - this.startTick);
    }

    public RenderSegment withTicks(int startTick, int endTick) {
        return new RenderSegment(startTick, endTick);
    }

    public RenderSegment clamped(int totalTicks) {
        int min = Math.min(this.startTick, this.endTick);
        int max = Math.max(this.startTick, this.endTick);
        min = clamp(min, 0, totalTicks);
        max = clamp(max, 0, totalTicks);
        return new RenderSegment(min, max);
    }

    public static List<RenderSegment> normalize(List<RenderSegment> segments, int totalTicks) {
        List<RenderSegment> sorted = new ArrayList<>();
        for (RenderSegment segment : segments) {
            if (segment == null) {
                continue;
            }
            RenderSegment clamped = segment.clamped(totalTicks);
            if (clamped.endTick > clamped.startTick) {
                sorted.add(clamped);
            }
        }

        sorted.sort(Comparator.comparingInt(RenderSegment::startTick)
            .thenComparingInt(RenderSegment::endTick));

        List<RenderSegment> merged = new ArrayList<>();
        for (RenderSegment segment : sorted) {
            if (merged.isEmpty()) {
                merged.add(segment);
                continue;
            }

            RenderSegment previous = merged.getLast();
            if (segment.startTick <= previous.endTick) {
                merged.set(merged.size() - 1, new RenderSegment(previous.startTick,
                    Math.max(previous.endTick, segment.endTick)));
            } else {
                merged.add(segment);
            }
        }
        return merged;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
