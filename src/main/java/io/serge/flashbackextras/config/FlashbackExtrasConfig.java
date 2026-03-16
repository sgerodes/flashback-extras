package io.serge.flashbackextras.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.serge.flashbackextras.FlashbackExtras;
import io.serge.flashbackextras.export.ExportCapabilityWarmup;
import io.serge.flashbackextras.export.ExportPreset;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FlashbackExtrasConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("flashback-extras.json");

    private static Data data = new Data();

    private FlashbackExtrasConfig() {
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(PATH)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            data = loaded != null ? loaded : new Data();
            if (data.exportPresets == null) {
                data.exportPresets = new ArrayList<>();
            }
        } catch (Exception e) {
            FlashbackExtras.LOGGER.error("Failed to load config from {}", PATH, e);
            data = new Data();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            FlashbackExtras.LOGGER.error("Failed to save config to {}", PATH, e);
        }
    }

    public static boolean isTimelineHorizontalScrollEnabled() {
        return data.timelineHorizontalScroll;
    }

    public static void setTimelineHorizontalScrollEnabled(boolean enabled) {
        data.timelineHorizontalScroll = enabled;
        save();
    }

    public static boolean isExportPresetsEnabled() {
        return data.exportPresetsEnabled;
    }

    public static void setExportPresetsEnabled(boolean enabled) {
        data.exportPresetsEnabled = enabled;
        save();
    }

    public static boolean isExportCropGuideEnabled() {
        return data.exportCropGuideEnabled;
    }

    public static void setExportCropGuideEnabled(boolean enabled) {
        data.exportCropGuideEnabled = enabled;
        save();
    }

    public static boolean isAudioTimelineFixesEnabled() {
        return data.audioTimelineFixesEnabled;
    }

    public static void setAudioTimelineFixesEnabled(boolean enabled) {
        data.audioTimelineFixesEnabled = enabled;
        save();
    }

    public static boolean isExportWarmupEnabled() {
        return data.exportWarmupEnabled;
    }

    public static void setExportWarmupEnabled(boolean enabled) {
        data.exportWarmupEnabled = enabled;
        save();
        ExportCapabilityWarmup.onConfigChanged();
    }

    public static List<ExportPreset> getCustomExportPresets() {
        return List.copyOf(data.exportPresets);
    }

    public static List<ExportPreset> getAllExportPresets() {
        List<ExportPreset> presets = new ArrayList<>(data.exportPresets);
        presets.addAll(ExportPreset.builtIns());
        return presets;
    }

    public static void saveExportPreset(ExportPreset preset) {
        deleteExportPreset(preset.name());
        data.exportPresets.add(preset);
        data.exportPresets.sort(Comparator.comparing(ExportPreset::name, String.CASE_INSENSITIVE_ORDER));
        save();
    }

    public static void deleteExportPreset(String name) {
        data.exportPresets.removeIf(preset -> preset.name() != null && preset.name().equalsIgnoreCase(name));
        save();
    }

    private static final class Data {
        private boolean timelineHorizontalScroll = true;
        private boolean exportPresetsEnabled = true;
        private boolean exportCropGuideEnabled = true;
        private boolean audioTimelineFixesEnabled = true;
        private boolean exportWarmupEnabled = true;
        private List<ExportPreset> exportPresets = new ArrayList<>();
    }
}
