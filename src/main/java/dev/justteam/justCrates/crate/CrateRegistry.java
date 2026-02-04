package dev.justteam.justCrates.crate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CrateRegistry {

    private final Map<String, CrateDefinition> crates = new HashMap<>();

    public void register(CrateDefinition crate) {
        crates.put(crate.getId().toLowerCase(Locale.ROOT), crate);
    }

    public CrateDefinition get(String id) {
        if (id == null) {
            return null;
        }
        return crates.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<CrateDefinition> all() {
        return crates.values();
    }

    public void clear() {
        crates.clear();
    }
}
