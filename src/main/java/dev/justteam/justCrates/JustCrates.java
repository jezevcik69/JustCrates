package dev.justteam.justCrates;

import dev.justteam.justCrates.command.JustCratesCommand;
import dev.justteam.justCrates.core.Messages;
import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.PreviewGuiSettings;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.BlockCrateService;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.editor.EditorListener;
import dev.justteam.justCrates.editor.EditorService;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
import dev.justteam.justCrates.listener.CrateListener;
import dev.justteam.justCrates.listener.GuiListener;
import dev.justteam.justCrates.placeholder.ExcellentCratesCompatibilityExpansion;
import dev.justteam.justCrates.placeholder.JustCratesExpansion;
import dev.justteam.justCrates.provider.ProviderRegistry;
import dev.justteam.justCrates.utils.VersionChecker;
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
        this.pluginPaths = new PluginPaths(this);
        this.pluginPaths.ensure();
        reloadConfig();
        this.previewGuiSettings = new PreviewGuiSettings(pluginPaths.getPreviewGuiFile());
        reloadRuntimeAssets();

        ProviderRegistry providerRegistry = new ProviderRegistry(this);
        providerRegistry.detect();

        this.keyService = new KeyService(this, providerRegistry, pluginPaths);
        this.virtualKeyService = new VirtualKeyService(this, pluginPaths, keyService);
        this.crateService = new CrateService(this, providerRegistry, pluginPaths, keyService, virtualKeyService);
        this.blockCrateService = new BlockCrateService(this, pluginPaths, crateService);
        this.crateService.setBlockCrateService(blockCrateService);
        this.editorService = new EditorService(this, pluginPaths, crateService, keyService, blockCrateService);

        this.keyService.loadAll();
        this.crateService.loadAll();
        this.blockCrateService.load();

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
            getLogger().info("PlaceholderAPI expansion registered.");
            if (Bukkit.getPluginManager().getPlugin("ExcellentCrates") == null) {
                new ExcellentCratesCompatibilityExpansion(this, crateService).register();
                getLogger().info("ExcellentCrates compatibility expansion registered.");
            }
        }

        new VersionChecker(this, "Jezevcik69", "JustCrates").checkForUpdates();

        getLogger().info("JustCrates has been enabled!");
    }

    @Override
    public void onDisable() {
        this.blockCrateService.save();
        this.blockCrateService.shutdown();
        if (this.virtualKeyService != null) {
            this.virtualKeyService.save();
        }
        getLogger().info("JustCrates has been disabled!");
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
        this.keyService.loadAll();
        this.crateService.loadAll();
        this.blockCrateService.load();
        if (this.virtualKeyService != null) {
            this.virtualKeyService.reload();
        }
    }
}
