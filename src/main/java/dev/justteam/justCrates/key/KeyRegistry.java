package dev.justteam.justCrates.key;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class KeyRegistry {

    private final Map<String, KeyDefinition> keys = new HashMap<>();

    public void register(KeyDefinition key) {
        keys.put(key.getId().toLowerCase(Locale.ROOT), key);
    }

    public KeyDefinition get(String id) {
        if (id == null) {
            return null;
        }
        return keys.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<KeyDefinition> all() {
        return keys.values();
    }

    public void clear() {
        keys.clear();
    }
}

