package dev.justteam.justCrates.provider;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ProviderRegistry {

    private final JavaPlugin plugin;
    private final Map<String, ItemProvider> providers = new HashMap<>();

    public ProviderRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void detect() {
        register(new VanillaProvider());

        PluginManager pm = plugin.getServer().getPluginManager();
        if (isEnabled(pm, "Oraxen")) {
            register(new OraxenProvider());
        }
        if (isEnabled(pm, "ItemsAdder")) {
            register(new ItemsAdderProvider());
        }
        if (isEnabled(pm, "Nexo")) {
            register(new NexoProvider());
        }
        if (isEnabled(pm, "ModelEngine")) {
            register(new ModelEngineProvider());
        }
    }

    private boolean isEnabled(PluginManager pm, String name) {
        Plugin p = pm.getPlugin(name);
        return p != null && p.isEnabled();
    }

    public void register(ItemProvider provider) {
        providers.put(provider.id().toLowerCase(Locale.ROOT), provider);
    }

    public ItemProvider getProvider(String id) {
        if (id == null || id.isBlank()) {
            return providers.get("vanilla");
        }
        return providers.get(id.toLowerCase(Locale.ROOT));
    }
}
