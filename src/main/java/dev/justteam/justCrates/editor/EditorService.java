package dev.justteam.justCrates.editor;

import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.BlockCrateService;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.key.KeyDefinition;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.reward.RewardDefinition;
import dev.justteam.justCrates.reward.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorService {

    private final JavaPlugin plugin;
    private final PluginPaths paths;
    private final CrateService crateService;
    private final KeyService keyService;
    private final BlockCrateService blockCrateService;

    private final NamespacedKey actionKey;
    private final NamespacedKey crateIdKey;
    private final NamespacedKey keyIdKey;
    private final NamespacedKey rewardIndexKey;

    private final Map<UUID, EditorInput> pendingInput = new ConcurrentHashMap<>();
    private final Map<UUID, String> bindMode = new ConcurrentHashMap<>();
    private final List<UUID> unbindMode = new ArrayList<>();

    public EditorService(JavaPlugin plugin, PluginPaths paths, CrateService crateService, KeyService keyService,
            BlockCrateService blockCrateService) {
        this.plugin = plugin;
        this.paths = paths;
        this.crateService = crateService;
        this.keyService = keyService;
        this.blockCrateService = blockCrateService;
        this.actionKey = new NamespacedKey(plugin, "jc_action");
        this.crateIdKey = new NamespacedKey(plugin, "jc_crate_id");
        this.keyIdKey = new NamespacedKey(plugin, "jc_key_id");
        this.rewardIndexKey = new NamespacedKey(plugin, "jc_reward_index");
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public BlockCrateService getBlockCrateService() {
        return blockCrateService;
    }

    public boolean hasPendingInput(Player player) {
        return pendingInput.containsKey(player.getUniqueId());
    }

    public EditorInput getPendingInput(Player player) {
        return pendingInput.get(player.getUniqueId());
    }

    public void clearPendingInput(Player player) {
        pendingInput.remove(player.getUniqueId());
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.MAIN, null), 27,
                ui("&8JustCrates • Editor"));
        fillBorder(inv);
        inv.setItem(11, actionItem(Material.CHEST, "&aCrates", "crates",
                "&7Create crates", "&7Setup rewards", "&7Bind crate blocks"));
        inv.setItem(15, actionItem(Material.TRIPWIRE_HOOK, "&eKeys", "keys",
                "&7Create keys", "&7Edit key icon in GUI", "&7Quick give keys"));
        inv.setItem(22, actionItem(Material.BARRIER, "&cClose", "close", "&7Close editor"));
        player.openInventory(inv);
    }

    public void openCratesMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.CRATES, null), 54,
                ui("&8Crates • Manager"));
        fillBorder(inv);
        inv.setItem(49, actionItem(Material.LIME_CONCRETE, "&aCreate Crate", "create_crate",
                "&7Create new crate by ID"));
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));

        int slot = 10;
        for (CrateDefinition crate : crateService.getCrates()) {
            if (slot >= 45) {
                break;
            }
            ItemStack icon = new ItemStack(Material.CHEST);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ui(crate.getName()));
                List<String> lore = new ArrayList<>();
                lore.add(ui("&7ID: &f" + crate.getId()));
                lore.add(ui("&7Type: &f" + crate.getType().name()));
                if (crate.getKeyId() != null && !crate.getKeyId().isBlank()) {
                    lore.add(ui("&7Key: &f" + crate.getKeyId()));
                } else {
                    lore.add(ui("&7Key: &cNone"));
                }
                lore.add(ui("&eClick to edit crate"));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(crateIdKey, PersistentDataType.STRING, crate.getId());
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot = nextSlot(slot);
        }

        player.openInventory(inv);
    }

    public void openKeysMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.KEYS, null), 54,
                ui("&8Keys • Manager"));
        fillBorder(inv);
        inv.setItem(49, actionItem(Material.LIME_CONCRETE, "&aCreate Key", "create_key",
                "&7Create key by ID", "&7No item in hand required"));
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));

        int slot = 10;
        for (KeyDefinition key : keyService.getKeys()) {
            if (slot >= 45) {
                break;
            }
            ItemStack icon = keyService.createKeyItem(key);
            if (icon == null) {
                icon = new ItemStack(Material.TRIPWIRE_HOOK);
            }
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(keyIdKey, PersistentDataType.STRING, key.getId());
                List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
                lore.add(ui("&eLeft click: edit key"));
                lore.add(ui("&aRight click: get 1x key"));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot = nextSlot(slot);
        }

        player.openInventory(inv);
    }

    public void openKeyEditor(Player player, String keyId) {
        KeyDefinition key = keyService.getKey(keyId);
        if (key == null) {
            player.sendMessage(Text.chat("&cKey does not exist."));
            return;
        }

        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.KEY_EDIT, keyId), 27,
                ui("&8Key: " + keyId));
        fillBorder(inv);

        ItemStack preview = keyService.createKeyItem(key);
        if (preview == null) {
            preview = new ItemStack(Material.TRIPWIRE_HOOK);
        }
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta != null) {
            List<String> lore = previewMeta.getLore() == null ? new ArrayList<>()
                    : new ArrayList<>(previewMeta.getLore());
            lore.add(ui("&7ID: &f" + keyId));
            lore.add(ui("&7Current key icon"));
            previewMeta.setLore(lore);
            preview.setItemMeta(previewMeta);
        }

        inv.setItem(13, preview);
        inv.setItem(11, actionItem(Material.ITEM_FRAME, "&bSet Icon From Cursor", "set_key_icon",
                "&7Pick any item from inventory", "&7then click this button"));
        inv.setItem(15, actionItem(Material.EMERALD, "&aGet 1x Key", "give_key",
                "&7Adds this key to your inventory"));
        inv.setItem(22, actionItem(Material.ARROW, "&7Back", "back_keys", "&7Back to keys"));

        player.openInventory(inv);
    }

    public void openCrateEditor(Player player, String crateId) {
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null) {
            player.sendMessage(Text.chat("&cCrate does not exist."));
            return;
        }

        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.CRATE_EDIT, crateId), 45,
                ui("&8Crate Editor • " + crateId));
        fillBorder(inv);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ui("&e" + crate.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ui("&7ID: &f" + crate.getId()));
            lore.add(ui("&7Type: &f" + crate.getType().name()));
            lore.add(ui("&7Key: &f"
                    + (crate.getKeyId() == null || crate.getKeyId().isBlank() ? "&cNone" : crate.getKeyId())));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        inv.setItem(11, actionItem(Material.CHAIN, "&aBind Block", "bind_block",
                "&7Click a block", "&7to bind"));
        inv.setItem(12, actionItem(Material.BARRIER, "&cUnbind Block", "unbind_block",
                "&7Click a block", "&7to unbind"));
        inv.setItem(13, actionItem(Material.TRIPWIRE_HOOK, "&eSelect Key", "select_key",
                "&7Select an existing key"));
        inv.setItem(15, actionItem(Material.CHEST_MINECART, "&bRewards", "rewards",
                "&7Add and edit"));
        inv.setItem(20, actionItem(Material.BLAZE_POWDER, "&6Set Particle", "set_particle",
                "&7Current: &f"
                        + (crate.getParticle() == null || crate.getParticle().isBlank() ? "None" : crate.getParticle()),
                "&7Click to set particle"));
        inv.setItem(29, actionItem(Material.HOPPER, "&dRoll Type", "roll_type",
                "&7Current: &f" + crate.getRollDefinition().getRollType().name(),
                "&7Click to change animation"));
        inv.setItem(40, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));

        player.openInventory(inv);
    }

    public void openRewardsMenu(Player player, String crateId) {
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null) {
            player.sendMessage(Text.chat("&cCrate does not exist."));
            return;
        }

        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.REWARDS, crateId), 54,
                ui("&8Rewards • " + crateId));
        fillBorder(inv);
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));

        int slot = 10;
        int index = 0;
        for (RewardDefinition reward : crate.getRewards()) {
            if (slot >= 45) {
                break;
            }
            ItemStack icon = rewardIcon(reward);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
                lore.add(ui("&7Weight: &f" + reward.getWeight()));
                lore.add(ui("&7Left: change weight"));
                lore.add(ui("&7Right: remove"));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(rewardIndexKey, PersistentDataType.INTEGER, index);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot = nextSlot(slot);
            index++;
        }

        player.openInventory(inv);
    }

    public void openKeySelectMenu(Player player, String crateId) {
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.KEY_SELECT, crateId), 54,
                ui("&8Select Key • " + crateId));
        fillBorder(inv);
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));

        int slot = 10;
        for (KeyDefinition key : keyService.getKeys()) {
            if (slot >= 45) {
                break;
            }
            ItemStack icon = keyService.createKeyItem(key);
            if (icon == null) {
                icon = new ItemStack(Material.TRIPWIRE_HOOK);
            }
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(keyIdKey, PersistentDataType.STRING, key.getId());
                List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
                lore.add(ui("&7Click to select"));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot = nextSlot(slot);
        }

        player.openInventory(inv);
    }

    public void openRollTypeMenu(Player player, String crateId) {
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.ROLL_TYPE_SELECT, crateId), 27,
                ui("&8Roll Type • " + crateId));
        fillBorder(inv);
        inv.setItem(11, actionItem(Material.HOPPER, "&bCSGO", "set_roll_csgo",
                "&7Items scroll horizontally", "&7and slow down to reveal reward"));
        inv.setItem(13, actionItem(Material.COMPASS, "&dRoulette", "set_roll_roulette",
                "&7Items rotate around", "&7the perimeter of the GUI"));
        inv.setItem(15, actionItem(Material.REDSTONE, "&aInstant", "set_roll_instant",
                "&7Instantly reveals the reward", "&7No animation"));
        inv.setItem(22, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));
        player.openInventory(inv);
    }

    public void handleMenuClick(Player player, EditorMenuHolder holder, ItemStack clicked, ItemStack cursor,
            int rawSlot, boolean rightClick) {
        if (holder == null) {
            return;
        }
        EditorMenuType type = holder.getType();
        PersistentDataContainer data = clicked != null && clicked.hasItemMeta()
                ? clicked.getItemMeta().getPersistentDataContainer()
                : null;
        String action = data != null ? data.get(actionKey, PersistentDataType.STRING) : null;

        switch (type) {
            case MAIN -> handleMainClick(player, action);
            case CRATES -> handleCratesClick(player, action, data);
            case KEYS -> handleKeysClick(player, action, data, rightClick);
            case KEY_EDIT -> handleKeyEditClick(player, holder.getCrateId(), action, cursor, rawSlot);
            case CRATE_EDIT -> handleCrateEditClick(player, holder.getCrateId(), action);
            case REWARDS -> handleRewardsClick(player, holder.getCrateId(), action, data, rightClick);
            case KEY_SELECT -> handleKeySelectClick(player, holder.getCrateId(), action, data);
            case ROLL_TYPE_SELECT -> handleRollTypeSelectClick(player, holder.getCrateId(), action);
        }
    }

    public void startCreateCrate(Player player) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.CREATE_CRATE, null, null));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter crate ID (e.g. &fstarter& e)."));
    }

    public void startCreateKey(Player player) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.CREATE_KEY, null, null));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter key ID (e.g. &fstarter_key& e)."));
        player.sendMessage(Text.chat("&7Icon can be set in GUI editor after creation."));
    }

    public void startSetRewardWeight(Player player, String crateId, int rewardIndex) {
        pendingInput.put(player.getUniqueId(),
                new EditorInput(EditorInputType.SET_REWARD_WEIGHT, crateId, rewardIndex));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter new weight (number)."));
    }

    public void startSetCrateParticle(Player player, String crateId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_CRATE_PARTICLE, crateId, null));
        player.closeInventory();
        player.sendMessage(Text.chat("&eClick a particle below to set it:"));

        String[] popularParticles = {
                "FLAME", "VILLAGER_HAPPY", "HEART", "CRIT_MAGIC", "ENCHANTMENT_TABLE",
                "PORTAL", "REDSTONE", "SLIME", "SNOWBALL", "SPELL_WITCH",
                "TOWNAURA", "VILLAGER_ANGRY", "WATER_DROP", "DRAGON_BREATH", "END_ROD",
                "TOTEM", "SMOKE_NORMAL", "CLOUD", "LAVA", "GLOW", "NONE"
        };

        net.md_5.bungee.api.chat.ComponentBuilder builder = new net.md_5.bungee.api.chat.ComponentBuilder("");
        for (int i = 0; i < popularParticles.length; i++) {
            String p = popularParticles[i];
            net.md_5.bungee.api.chat.TextComponent comp = new net.md_5.bungee.api.chat.TextComponent(p);
            if (p.equals("NONE")) {
                comp.setColor(net.md_5.bungee.api.ChatColor.RED);
            } else {
                comp.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            }
            comp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/justcrates _editchat " + p));
            comp.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                    new net.md_5.bungee.api.chat.ComponentBuilder(Text.color("&aClick to set to &f" + p)).create()));
            builder.append(comp);
            if (i < popularParticles.length - 1) {
                builder.append(new net.md_5.bungee.api.chat.TextComponent(" | "))
                        .color(net.md_5.bungee.api.ChatColor.GRAY);
            }
        }
        player.spigot().sendMessage(builder.create());
        player.sendMessage(Text.chat("&7Or type any other particle name manually. Type &cnone&7 to clear."));
    }

    public boolean isInBindMode(Player player) {
        return bindMode.containsKey(player.getUniqueId());
    }

    public String getBindCrateId(Player player) {
        return bindMode.get(player.getUniqueId());
    }

    public void setBindMode(Player player, String crateId) {
        bindMode.put(player.getUniqueId(), crateId);
        player.closeInventory();
        player.sendMessage(Text.chat("&aClick a block to bind the crate."));
    }

    public void clearBindMode(Player player) {
        bindMode.remove(player.getUniqueId());
    }

    public boolean isInUnbindMode(Player player) {
        return unbindMode.contains(player.getUniqueId());
    }

    public void setUnbindMode(Player player) {
        unbindMode.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(Text.chat("&cClick a bound block to unbind it."));
    }

    public void clearUnbindMode(Player player) {
        unbindMode.remove(player.getUniqueId());
    }

    public void handleChatInput(Player player, String message) {
        EditorInput input = pendingInput.remove(player.getUniqueId());
        if (input == null) {
            return;
        }

        switch (input.getType()) {
            case CREATE_CRATE -> handleCreateCrateInput(player, message);
            case CREATE_KEY -> handleCreateKeyInput(player, message);
            case SET_REWARD_WEIGHT ->
                handleSetRewardWeightInput(player, message, input.getCrateId(), input.getRewardIndex());
            case SET_CRATE_PARTICLE -> handleSetCrateParticleInput(player, message, input.getCrateId());
        }
    }

    private void handleMainClick(Player player, String action) {
        if (action == null) {
            return;
        }
        switch (action) {
            case "crates" -> openCratesMenu(player);
            case "keys" -> openKeysMenu(player);
            case "close" -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleCratesClick(Player player, String action, PersistentDataContainer data) {
        if ("create_crate".equals(action)) {
            startCreateCrate(player);
            return;
        }
        if ("back".equals(action)) {
            openMainMenu(player);
            return;
        }
        if (data == null) {
            return;
        }
        String crateId = data.get(crateIdKey, PersistentDataType.STRING);
        if (crateId != null) {
            openCrateEditor(player, crateId);
        }
    }

    private void handleKeysClick(Player player, String action, PersistentDataContainer data, boolean rightClick) {
        if ("create_key".equals(action)) {
            startCreateKey(player);
            return;
        }
        if ("back".equals(action)) {
            openMainMenu(player);
            return;
        }
        if (data == null) {
            return;
        }
        String keyId = data.get(keyIdKey, PersistentDataType.STRING);
        if (keyId != null) {
            if (rightClick) {
                giveKey(player, keyId, player, 1);
            } else {
                openKeyEditor(player, keyId);
            }
        }
    }

    private void handleKeyEditClick(Player player, String keyId, String action, ItemStack cursor, int rawSlot) {
        if (keyId == null) {
            return;
        }
        if ("back_keys".equals(action)) {
            openKeysMenu(player);
            return;
        }
        if ("give_key".equals(action)) {
            giveKey(player, keyId, player, 1);
            openKeyEditor(player, keyId);
            return;
        }
        if ("set_key_icon".equals(action)) {
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage(Text.chat("&cPick an item on cursor first."));
                return;
            }
            if (!saveKeyItemFromCursor(player, keyId, cursor)) {
                player.sendMessage(Text.chat("&cFailed to update key icon."));
                return;
            }
            return;
        }

        // Allow direct icon update by clicking center slot with cursor item.
        if (rawSlot == 13 && cursor != null && !cursor.getType().isAir()) {
            if (!saveKeyItemFromCursor(player, keyId, cursor)) {
                player.sendMessage(Text.chat("&cFailed to update key icon."));
                return;
            }
        }
    }

    private void handleCrateEditClick(Player player, String crateId, String action) {
        if (crateId == null) {
            return;
        }
        switch (action) {
            case "bind_block" -> setBindMode(player, crateId);
            case "unbind_block" -> setUnbindMode(player);
            case "select_key" -> openKeySelectMenu(player, crateId);
            case "rewards" -> openRewardsMenu(player, crateId);
            case "set_particle" -> startSetCrateParticle(player, crateId);
            case "roll_type" -> openRollTypeMenu(player, crateId);
            case "back" -> openCratesMenu(player);
            default -> {
            }
        }
    }

    private void handleRewardsClick(Player player, String crateId, String action, PersistentDataContainer data,
            boolean rightClick) {
        if ("back".equals(action)) {
            openCrateEditor(player, crateId);
            return;
        }
        if (data == null || crateId == null) {
            return;
        }
        Integer index = data.get(rewardIndexKey, PersistentDataType.INTEGER);
        if (index == null) {
            return;
        }
        if (rightClick) {
            removeReward(crateId, index);
            crateService.loadAll();
            openRewardsMenu(player, crateId);
        } else {
            startSetRewardWeight(player, crateId, index);
        }
    }

    private void handleKeySelectClick(Player player, String crateId, String action, PersistentDataContainer data) {
        if ("back".equals(action)) {
            openCrateEditor(player, crateId);
            return;
        }
        if (data == null || crateId == null) {
            return;
        }
        String keyId = data.get(keyIdKey, PersistentDataType.STRING);
        if (keyId != null) {
            setCrateKey(crateId, keyId);
            crateService.loadAll();
            openCrateEditor(player, crateId);
        }
    }

    private void handleRollTypeSelectClick(Player player, String crateId, String action) {
        if ("back".equals(action)) {
            openCrateEditor(player, crateId);
            return;
        }
        if (crateId == null || action == null) {
            return;
        }
        String rollType = switch (action) {
            case "set_roll_csgo" -> "CSGO";
            case "set_roll_roulette" -> "ROULETTE";
            case "set_roll_instant" -> "INSTANT";
            default -> null;
        };
        if (rollType != null) {
            setCrateRollType(crateId, rollType);
            crateService.loadAll();
            player.sendMessage(Text.chat("&aRoll type set to: " + rollType));
            openCrateEditor(player, crateId);
        }
    }

    private void setCrateRollType(String crateId, String rollType) {
        File file = new File(paths.getCratesFolder(), crateId.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("roll.type", rollType);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save roll type: " + e.getMessage());
        }
    }

    private void handleCreateCrateInput(Player player, String message) {
        String id = normalizeId(message);
        if (id == null) {
            player.sendMessage(Text.chat("&cInvalid ID. Use a-z, 0-9, - or _."));
            return;
        }
        File file = new File(paths.getCratesFolder(), id + ".yml");
        if (file.exists()) {
            player.sendMessage(Text.chat("&cA crate with this ID already exists."));
            return;
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", id);
        cfg.set("type", "GUI");
        cfg.set("key", "");
        cfg.set("display.name", "&aCrate " + id);
        cfg.set("display.lore", List.of("&7Edit me"));
        cfg.set("roll.type", "CSGO");
        cfg.set("roll.size", 27);
        cfg.set("roll.title", "&aCrate " + id);
        cfg.set("roll.duration-ticks", 60);
        cfg.set("roll.tick-interval", 2);
        cfg.set("rewards", new ArrayList<>());

        try {
            cfg.save(file);
            crateService.loadAll();
            player.sendMessage(Text.chat("&aCrate created: " + id));
            openCrateEditor(player, id);
        } catch (IOException e) {
            player.sendMessage(Text.chat("&cFailed to save crate."));
        }
    }

    private void handleCreateKeyInput(Player player, String message) {
        String id = normalizeId(message);
        if (id == null) {
            player.sendMessage(Text.chat("&cInvalid ID. Use a-z, 0-9, - or _."));
            return;
        }
        boolean saved = keyService.createDefaultKey(id);
        if (saved) {
            keyService.loadAll();
            player.sendMessage(Text.chat("&aKey created: " + id));
            openKeyEditor(player, id);
        } else {
            player.sendMessage(Text.chat("&cFailed to create key. ID may already exist."));
        }
    }

    private void handleSetCrateParticleInput(Player player, String message, String crateId) {
        if (crateId == null) {
            return;
        }

        String particle = message.trim().toUpperCase(Locale.ROOT);
        if (particle.equalsIgnoreCase("NONE") || particle.isEmpty()) {
            particle = "";
        } else {
            try {
                org.bukkit.Particle.valueOf(particle);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Text.chat("&cInvalid particle name."));
                return;
            }
        }

        File file = new File(paths.getCratesFolder(), crateId.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            player.sendMessage(Text.chat("&cCrate file not found."));
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("display.particle", particle);
        try {
            cfg.save(file);
            crateService.loadAll();
            player.sendMessage(Text.chat("&aParticle saved."));
            openCrateEditor(player, crateId);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate particle: " + e.getMessage());
            player.sendMessage(Text.chat("&cFailed to save particle."));
        }
    }

    private void handleSetRewardWeightInput(Player player, String message, String crateId, Integer index) {
        if (crateId == null || index == null) {
            return;
        }
        int weight;
        try {
            weight = Math.max(1, Integer.parseInt(message.trim()));
        } catch (NumberFormatException e) {
            player.sendMessage(Text.chat("&cInvalid number."));
            return;
        }
        if (!setRewardWeight(crateId, index, weight)) {
            player.sendMessage(Text.chat("&cFailed to update reward."));
            return;
        }
        crateService.loadAll();
        openRewardsMenu(player, crateId);
    }

    private void setCrateKey(String crateId, String keyId) {
        File file = new File(paths.getCratesFolder(), crateId.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("key", keyId.toLowerCase(Locale.ROOT));
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate key: " + e.getMessage());
        }
    }

    private boolean setRewardWeight(String crateId, int index, int weight) {
        File file = new File(paths.getCratesFolder(), crateId.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rewards = cfg.getMapList("rewards");
        if (index < 0 || index >= rewards.size()) {
            return false;
        }
        List<Map<String, Object>> updated = new ArrayList<>();
        int i = 0;
        for (Map<?, ?> reward : rewards) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : reward.entrySet()) {
                if (entry.getKey() != null) {
                    map.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            if (i == index) {
                map.put("weight", weight);
            }
            updated.add(map);
            i++;
        }
        cfg.set("rewards", updated);
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save reward weight: " + e.getMessage());
            return false;
        }
    }

    private void removeReward(String crateId, int index) {
        File file = new File(paths.getCratesFolder(), crateId.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rewards = cfg.getMapList("rewards");
        if (index < 0 || index >= rewards.size()) {
            return;
        }
        List<Map<String, Object>> updated = new ArrayList<>();
        int i = 0;
        for (Map<?, ?> reward : rewards) {
            if (i != index) {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<?, ?> entry : reward.entrySet()) {
                    if (entry.getKey() != null) {
                        map.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                updated.add(map);
            }
            i++;
        }
        cfg.set("rewards", updated);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to remove reward: " + e.getMessage());
        }
    }

    private void giveKey(Player sender, String keyId, Player target, int amount) {
        KeyDefinition key = keyService.getKey(keyId);
        if (key == null) {
            sender.sendMessage(Text.chat("&cKey does not exist."));
            return;
        }
        ItemStack item = keyService.createKeyItem(key);
        if (item == null) {
            sender.sendMessage(Text.chat("&cFailed to build key item."));
            return;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        if (!sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(Text.chat("&aKey given."));
        }
    }

    private boolean saveKeyItemFromCursor(Player player, String keyId, ItemStack cursor) {
        ItemStack icon = cursor.clone();
        icon.setAmount(1);
        boolean saved = keyService.updateKeyItemStack(keyId, icon);
        if (!saved) {
            return false;
        }
        keyService.loadAll();
        player.sendMessage(Text.chat("&aKey icon updated: " + keyId));
        openKeyEditor(player, keyId);
        return true;
    }

    private ItemStack actionItem(Material material, String name, String action, String... loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ui(name));
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ui(line));
            }
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fillBorder(Inventory inv) {
        ItemStack dark = decorativePane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack accent = decorativePane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack corner = decorativePane(Material.CYAN_STAINED_GLASS_PANE);
        int size = inv.getSize();
        int rows = size / 9;

        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, dark);
            }
        }

        // Corners
        inv.setItem(0, corner);
        inv.setItem(8, corner);
        inv.setItem(size - 9, corner);
        inv.setItem(size - 1, corner);

        // Side accents
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, accent);
            inv.setItem(row * 9 + 8, accent);
        }
    }

    private ItemStack decorativePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack rewardIcon(RewardDefinition reward) {
        if (reward.getType() == RewardType.COMMAND) {
            ItemStack stack = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ui("&cCommand Reward"));
                stack.setItemMeta(meta);
            }
            return stack;
        }
        if (reward.getItemStack() != null) {
            return reward.getItemStack().clone();
        }
        if (reward.getItemDefinition() != null) {
            Material mat = reward.getItemDefinition().getMaterial() != null ? reward.getItemDefinition().getMaterial()
                    : Material.STONE;
            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ui(reward.getItemDefinition().getDisplayNameOrDefault()));
                stack.setItemMeta(meta);
            }
            return stack;
        }
        return new ItemStack(Material.STONE);
    }

    private int nextSlot(int slot) {
        if ((slot + 1) % 9 == 0) {
            return slot + 2;
        }
        return slot + 1;
    }

    private String normalizeId(String input) {
        if (input == null) {
            return null;
        }
        String id = input.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            return null;
        }
        if (!id.matches("[a-z0-9_-]+")) {
            return null;
        }
        return id;
    }

    private String ui(String input) {
        return Text.color(Text.toSmallCaps(input));
    }
}
