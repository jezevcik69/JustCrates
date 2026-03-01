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

    private PluginPaths paths;
    private ProviderRegistry providerRegistry;
    private KeyService keyService;
    private VirtualKeyService virtualKeyService;
    private CrateService crateService;
    private BlockCrateService blockCrateService;
    private EditorService editorService;

    @Override
    public void onEnable() {
        printStartupBanner();

        this.paths = new PluginPaths(this);
        this.paths.ensure();
        reloadConfig();

        this.providerRegistry = new ProviderRegistry(this);
        this.providerRegistry.detect();

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

        getLogger().info(Text.color("&aJustCrates enabled."));
    }

    @Override
    public void onDisable() {
        this.blockCrateService.save();
        if (this.virtualKeyService != null) {
            this.virtualKeyService.save();
        }
    }

    public PluginPaths getPaths() {
        return paths;
    }

    public ProviderRegistry getProviderRegistry() {
        return providerRegistry;
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

    private void printStartupBanner() {
        getLogger().info(" ");
        getLogger().info("     ____.               __   _________                __                 ");
        getLogger().info("    |    |__ __  _______/  |_ \\_   ___ \\____________ _/  |_  ____   ______");
        getLogger().info("    |    |  |  \\/  ___/\\   __\\/    \\  \\/\\_  __ \\__  \\\\   __\\/ __ \\ /  ___/");
        getLogger().info("/\\__|    |  |  /\\___ \\  |  |  \\     \\____|  | \\// __ \\|  | \\  ___/ \\___ \\ ");
        getLogger().info("\\________|____//____  > |__|   \\______  /|__|  (____  /__|  \\___  >____  >");
        getLogger().info("                    \\/                \\/            \\/          \\/     \\/ ");
        getLogger().info("                              Loading JustCrates...                         ");
        getLogger().info(" ");
    }
}
