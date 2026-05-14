package dev.meyba.justCrates;

import dev.meyba.justCrates.command.JustCratesCommand;
import dev.meyba.justCrates.core.Messages;
import dev.meyba.justCrates.core.PluginPaths;
import dev.meyba.justCrates.core.PreviewGuiSettings;
import dev.meyba.justCrates.core.Text;
import dev.meyba.justCrates.crate.BlockCrateService;
import dev.meyba.justCrates.crate.CrateService;
import dev.meyba.justCrates.editor.EditorListener;
import dev.meyba.justCrates.editor.EditorService;
import dev.meyba.justCrates.key.KeyService;
import dev.meyba.justCrates.key.VirtualKeyService;
import dev.meyba.justCrates.listener.CrateListener;
import dev.meyba.justCrates.listener.GuiListener;
import dev.meyba.justCrates.placeholder.ExcellentCratesCompatibilityExpansion;
import dev.meyba.justCrates.placeholder.JustCratesExpansion;
import dev.meyba.justCrates.provider.ProviderRegistry;
import dev.meyba.justCrates.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class JustCrates extends JavaPlugin {
    private PluginPaths pluginPaths;
    private PreviewGuiSettings previewGuiSettings;
    private KeyService keyService;
    private VirtualKeyService virtualKeyService;
    private CrateService crateService;
    private BlockCrateService blockCrateService;
    private EditorService editorService;

    @Override
    public void onEnable() {
pluginPaths = new PluginPaths(this);
pluginPaths.ensure();
        reloadConfig();
previewGuiSettings = new PreviewGuiSettings(pluginPaths.getPreviewGuiFile());
        reloadRuntimeAssets();

        ProviderRegistry providerRegistry = new ProviderRegistry(this);
        providerRegistry.detect();

keyService = new KeyService(this, providerRegistry, pluginPaths);
virtualKeyService = new VirtualKeyService(this, pluginPaths, keyService);
crateService = new CrateService(this, providerRegistry, pluginPaths, keyService, virtualKeyService);
blockCrateService = new BlockCrateService(this, pluginPaths, crateService);
crateService.setBlockCrateService(blockCrateService);
editorService = new EditorService(this, pluginPaths, crateService, keyService, blockCrateService);

keyService.loadAll();
crateService.loadAll();
blockCrateService.load();

        JustCratesCommand command = new JustCratesCommand(this);
        getCommand("justcrates").setExecutor(command);
        getCommand("justcrates").setTabCompleter(command);
        if (getCommand("key") != null) {
            getCommand("key").setExecutor(command);
            getCommand("key").setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, virtualKeyService), this);
        getServer().getPluginManager().registerEvents(new EditorListener(editorService), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new JustCratesExpansion(this, crateService, virtualKeyService).register();
            if (Bukkit.getPluginManager().getPlugin("ExcellentCrates") == null) {
                new ExcellentCratesCompatibilityExpansion(this, crateService).register();
            }
        }

        new VersionUtil(this, "Jezevcik69", "JustCrates").checkForUpdates();
    }

    @Override
    public void onDisable() {
        if (blockCrateService != null) {
            blockCrateService.save();
            blockCrateService.shutdown();
        }
        if (virtualKeyService != null) virtualKeyService.save();
    }

    public KeyService getKeyService() {
        return keyService;
    }

    public VirtualKeyService getVirtualKeyService() {
        return virtualKeyService;
    }

    public CrateService getCrateService() {
        return crateService;
    }

    public BlockCrateService getBlockCrateService() {
        return blockCrateService;
    }

    public EditorService getEditorService() {
        return editorService;
    }

    public PreviewGuiSettings getPreviewGuiSettings() {
        return previewGuiSettings;
    }

    public void reloadRuntimeAssets() {
        String configuredPrefix = getConfig().getString("prefix");
        if (configuredPrefix == null || configuredPrefix.isBlank()) {
            configuredPrefix = getConfig().getString("settings.prefix");
        }
        Text.setPrefix(configuredPrefix);
        Messages.load(pluginPaths.getMessagesFile());
        if (previewGuiSettings != null) {
            previewGuiSettings.reload();
        }
    }

    public void reloadAllData() {
        reloadConfig();
        reloadRuntimeAssets();
keyService.loadAll();
crateService.loadAll();
blockCrateService.load();
        if (this.virtualKeyService != null) {
    virtualKeyService.reload();
        }
    }
}
