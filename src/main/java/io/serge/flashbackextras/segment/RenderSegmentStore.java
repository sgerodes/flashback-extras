package io.serge.flashbackextras.segment;

import java.util.ArrayList;
import java.util.List;

public final class RenderSegmentStore {
    private List<RenderSegment> segments = new ArrayList<>();
    private RenderSegmentExportMode lastExportMode = RenderSegmentExportMode.NORMAL;

    public List<RenderSegment> segments() {
        return List.copyOf(this.segments);
    }

    public RenderSegmentExportMode lastExportMode() {
        return this.lastExportMode == null ? RenderSegmentExportMode.NORMAL : this.lastExportMode;
    }

    public void setLastExportMode(RenderSegmentExportMode lastExportMode) {
        this.lastExportMode = lastExportMode == null ? RenderSegmentExportMode.NORMAL : lastExportMode;
    }

    public void normalize(int totalTicks) {
        if (this.segments == null) {
            this.segments = new ArrayList<>();
        }
        this.segments = new ArrayList<>(RenderSegment.normalize(this.segments, totalTicks));
    }

    public void addSegment(int startTick, int endTick, int totalTicks) {
        this.ensureSegments();
        this.segments.add(new RenderSegment(startTick, endTick));
        this.normalize(totalTicks);
    }

    public void replaceSegment(int index, int startTick, int endTick, int totalTicks) {
        this.ensureSegments();
        if (index < 0 || index >= this.segments.size()) {
            return;
        }
        this.segments.set(index, new RenderSegment(startTick, endTick));
        this.normalize(totalTicks);
    }

    public void removeSegment(int index, int totalTicks) {
        this.ensureSegments();
        if (index < 0 || index >= this.segments.size()) {
            return;
        }
        this.segments.remove(index);
        this.normalize(totalTicks);
    }

    public int totalDurationTicks() {
        this.ensureSegments();
        int total = 0;
        for (RenderSegment segment : this.segments) {
            total += segment.durationTicks();
        }
        return total;
    }

    private void ensureSegments() {
        if (this.segments == null) {
            this.segments = new ArrayList<>();
        }
    }
}
