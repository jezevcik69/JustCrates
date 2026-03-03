package dev.justteam.justCrates.core;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class PreviewGuiSettings {
    private final File file;

    private int size;
    private String title;
    private String paneName;
    private Material darkMaterial;
    private Material accentMaterial;
    private Material cornerMaterial;
    private List<Integer> contentSlots = List.of();
    private List<Integer> accentSlots = List.of();
    private List<Integer> cornerSlots = List.of();

    public PreviewGuiSettings(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        this.size = normalizeSize(cfg.getInt("size", 54));
        this.title = cfg.getString("title", "&8Preview - %crate%");
        this.paneName = cfg.getString("pane-name", " ");

        this.darkMaterial = readMaterial(
                cfg.getString("border.dark-material"),
                Material.BLUE_STAINED_GLASS_PANE);
        this.accentMaterial = readMaterial(
                cfg.getString("border.accent-material"),
                Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        this.cornerMaterial = readMaterial(
                cfg.getString("border.corner-material"),
                Material.CYAN_STAINED_GLASS_PANE);

        List<Integer> defaultContent = defaultContentSlots(size);
        List<Integer> defaultAccent = defaultAccentSlots(size);
        List<Integer> defaultCorners = defaultCornerSlots(size);

        this.contentSlots = readSlots(cfg, "content-slots", size, defaultContent);
        this.accentSlots = readSlots(cfg, "border.accent-slots", size, defaultAccent);
        this.cornerSlots = readSlots(cfg, "border.corner-slots", size, defaultCorners);
    }

    public int getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }

    public String getPaneName() {
        return paneName;
    }

    public Material getDarkMaterial() {
        return darkMaterial;
    }

    public Material getAccentMaterial() {
        return accentMaterial;
    }

    public Material getCornerMaterial() {
        return cornerMaterial;
    }

    public List<Integer> getContentSlots() {
        return contentSlots;
    }

    public List<Integer> getAccentSlots() {
        return accentSlots;
    }

    public List<Integer> getCornerSlots() {
        return cornerSlots;
    }

    private static int normalizeSize(int input) {
        int size = Math.max(9, Math.min(54, input));
        int remainder = size % 9;
        if (remainder == 0) {
            return size;
        }
        size -= remainder;
        return Math.max(9, size);
    }

    private static Material readMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static List<Integer> readSlots(
            YamlConfiguration cfg,
            String path,
            int size,
            List<Integer> fallback
    ) {
        if (!cfg.contains(path)) {
            return fallback;
        }
        List<Integer> raw = cfg.getIntegerList(path);
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        LinkedHashSet<Integer> cleaned = new LinkedHashSet<>();
        for (int slot : raw) {
            if (slot >= 0 && slot < size) {
                cleaned.add(slot);
            }
        }
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return List.copyOf(cleaned);
    }

    private static List<Integer> defaultContentSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        int lastRowStart = size - 9;
        for (int slot = 0; slot < size; slot++) {
            if (slot < 9 || slot >= lastRowStart || slot % 9 == 0 || slot % 9 == 8) {
                continue;
            }
            slots.add(slot);
        }
        return List.copyOf(slots);
    }

    private static List<Integer> defaultCornerSlots(int size) {
        return List.of(0, 8, size - 9, size - 1);
    }

    private static List<Integer> defaultAccentSlots(int size) {
        LinkedHashSet<Integer> slots = new LinkedHashSet<>();
        int rows = size / 9;
        for (int row = 1; row < rows - 1; row++) {
            slots.add(row * 9);
            slots.add(row * 9 + 8);
        }
        if (size > 5) {
            slots.add(3);
            slots.add(5);
        }
        return List.copyOf(slots);
    }
}
