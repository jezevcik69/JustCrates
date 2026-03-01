package dev.justteam.justCrates;

import dev.justteam.justCrates.command.JustCratesCommand;
import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.BlockCrateService;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.editor.EditorListener;
import dev.justteam.justCrates.editor.EditorService;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
import dev.justteam.justCrates.listener.CrateListener;
import dev.justteam.justCrates.listener.GuiListener;
import dev.justteam.justCrates.provider.ProviderRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public final class JustCrates extends JavaPlugin {
    private KeyService keyService;
    private VirtualKeyService virtualKeyService;
    private CrateService crateService;
    private BlockCrateService blockCrateService;
    private EditorService editorService;

    @Override
    public void onEnable() {
        PluginPaths paths = new PluginPaths(this);
        paths.ensure();
        reloadConfig();

        ProviderRegistry providerRegistry = new ProviderRegistry(this);
        providerRegistry.detect();

        this.keyService = new KeyService(this, providerRegistry, paths);
        this.virtualKeyService = new VirtualKeyService(this, paths, keyService);
        this.crateService = new CrateService(this, providerRegistry, paths, keyService, virtualKeyService);
        this.blockCrateService = new BlockCrateService(this, paths, crateService);
        this.editorService = new EditorService(this, paths, crateService, keyService, blockCrateService);

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
}