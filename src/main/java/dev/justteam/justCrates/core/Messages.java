package dev.justteam.justCrates.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Messages {
    private static final Map<String, String> defaults = new LinkedHashMap<>();
    private static final Map<String, String> literalToKey = new HashMap<>();
    private static YamlConfiguration config;

    private Messages() {}

    public static void load(File file) {
        config = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String key = entry.getKey();
            if (!config.contains(key) && entry.getValue() != null) {
                config.set(key, entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            try {
                config.save(file);
            } catch (IOException ignored) {
            }
        }
    }

    public static String get(String key) {
        return Text.chat(resolveKey(key));
    }

    public static String get(String key, String... replacements) {
        return Text.chat(formatRaw(key, replacements));
    }

    public static String raw(String key) {
        return resolveKey(key);
    }

    public static String formatRaw(String key, String... replacements) {
        return applyReplacements(resolveKey(key), replacements);
    }

    public static String resolveLiteral(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String key = literalToKey.get(value);
        if (key == null) {
            return value;
        }
        return resolveKey(key);
    }

    private static String resolveKey(String key) {
        if (config != null) {
            String value = config.getString(key);
            if (value != null) {
                return value;
            }
        }
        String def = defaults.get(key);
        return def != null ? def : key;
    }

    private static String applyReplacements(String msg, String... replacements) {
        if (replacements == null || replacements.length < 2) {
            return msg;
        }
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    private static void loadDefaultsFromResource() {
        defaults.clear();
        try (InputStream stream = Messages.class.getClassLoader().getResourceAsStream("messages.yml")) {
            if (stream == null) {
                return;
            }
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            Set<String> keys = bundled.getKeys(true);
            for (String key : keys) {
                if (bundled.isString(key)) {
                    defaults.put(key, bundled.getString(key));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void rebuildLiteralLookup() {
        literalToKey.clear();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            literalToKey.putIfAbsent(value, key);
        }
    }

    static {
        loadDefaultsFromResource();
        if (defaults.isEmpty()) {
            defaults.put("no-permission", "&cNo permission.");
            defaults.put("player-only", "&cOnly players can use this command.");
            defaults.put("reloaded", "&aReloaded.");
        }
        rebuildLiteralLookup();
    }
}
