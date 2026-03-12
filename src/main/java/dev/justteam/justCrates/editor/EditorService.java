package dev.justteam.justCrates.editor;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.core.Messages;
import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.BlockCrateService;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.key.KeyDefinition;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
import dev.justteam.justCrates.reward.RewardDefinition;
import dev.justteam.justCrates.reward.RewardType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
import java.util.*;
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
    private final Map<UUID, Integer> hologramAwaitingLineInput = new ConcurrentHashMap<>();
    private final Map<UUID, String> keyIconMode = new ConcurrentHashMap<>();
    private final Map<UUID, String> rewardItemMode = new ConcurrentHashMap<>();

    public EditorService(JavaPlugin plugin, PluginPaths paths, CrateService crateService, KeyService keyService, BlockCrateService blockCrateService) {
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

    public boolean isInKeyIconMode(Player player) {
        return keyIconMode.containsKey(player.getUniqueId());
    }

    public boolean isInRewardItemMode(Player player) {
        return rewardItemMode.containsKey(player.getUniqueId());
    }

    public void handleRewardItemClick(Player player, ItemStack clicked) {
        String crateId = rewardItemMode.remove(player.getUniqueId());
        if (crateId == null || clicked == null || clicked.getType().isAir()) {
            return;
        }
        ItemStack reward = clicked.clone();
        reward.setAmount(clicked.getAmount());
        if (!addItemReward(crateId, reward)) {
            player.sendMessage(Text.chat("&cFailed to add item reward."));
            return;
        }
        crateService.loadAll();
        player.sendMessage(Text.chat("&aItem reward added."));
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openRewardsMenu(player, crateId), 1L);
    }

    public void handleKeyIconClick(Player player, ItemStack clicked) {
        String keyId = keyIconMode.remove(player.getUniqueId());
        if (keyId == null || clicked == null || clicked.getType().isAir()) {
            return;
        }
        ItemStack icon = clicked.clone();
        icon.setAmount(1);
        if (!keyService.updateKeyItemStack(keyId, icon)) {
            player.sendMessage(Text.chat("&cFailed to update key icon."));
            return;
        }
        keyService.loadAll();
        player.sendMessage(Messages.get("key-icon-updated", "%key%", keyId));
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openKeyEditor(player, keyId), 1L);
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
        inv.setItem(49, actionItem(Material.LIME_DYE, "&aCreate Crate", "create_crate",
                "&7Create new crate by ID"));
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Return to editor"));

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
        inv.setItem(49, actionItem(Material.LIME_DYE, "&aCreate Key", "create_key",
                "&7Create key by ID", "&7No item in hand required"));
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Return to editor"));

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
            lore.add(ui("&7Mode: &f" + (key.isVirtual() ? "Virtual" : "Physical")));
            lore.add(ui("&7Current key icon"));
            lore.add(ui("&eLeft click: rename key"));
            lore.add(ui("&aRight click: edit key lines"));
            previewMeta.setLore(lore);
            previewMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "rename_key");
            preview.setItemMeta(previewMeta);
        }

        inv.setItem(13, preview);

        inv.setItem(12, actionItem(Material.ENDER_EYE, "&dToggle Virtual", "toggle_virtual_key",
                "&7Current: &f" + (key.isVirtual() ? "Virtual" : "Physical"),
                "&7Virtual key is stored as balance",
                "&7and can be used in crate key checks"));
        inv.setItem(11, actionItem(Material.ITEM_FRAME, "&bSet Icon", "set_key_icon",
                "&7Click this, then click any",
                "&7item in your inventory to",
                "&7set it as the key icon"));

        inv.setItem(14, actionItem(Material.EMERALD, "&aGet 1x Key", "give_key",
                "&7Adds this key to your inventory"));
        inv.setItem(26, actionItem(Material.BARRIER, "&cDelete Key", "delete_key",
                "&7Permanently deletes this key",
                "&eClick to confirm in chat"));
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
            lore.add(ui("&7Open Sound: &f"
                    + (crate.getOpenSound() == null || crate.getOpenSound().isBlank() ? "&cNone" : crate.getOpenSound())));
            lore.add(ui("&7Cooldown: &f" + (crate.getCooldown() > 0 ? crate.getCooldown() + "s" : "None")));
            lore.add(ui("&7Permission: &f"
                    + (crate.getPermission() == null || crate.getPermission().isBlank() ? "&cNone" : crate.getPermission())));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(22, info);

        inv.setItem(10, actionItem(Material.TNT, "&cUnbind Block", "unbind_block",
                "&7Click a block", "&7to unbind"));
        inv.setItem(11, actionItem(Material.CHAIN, "&aBind Block", "bind_block",
                "&7Click a block", "&7to bind"));

        String currentKeyName = "&cNone";
        if (crate.getKeyId() != null && !crate.getKeyId().isBlank()) {
            KeyDefinition currentKey = keyService.getKey(crate.getKeyId());
            currentKeyName = currentKey != null ? currentKey.getName() : crate.getKeyId();
        }
        inv.setItem(12, actionItem(Material.TRIPWIRE_HOOK, "&eSelect Key", "select_key",
                "&7Current: &f" + currentKeyName,
                "&7Click to select a key"));

        inv.setItem(14, actionItem(Material.CHEST_MINECART, "&bRewards", "rewards",
                "&7Add and edit"));
        inv.setItem(15, actionItem(Material.HOPPER, "&dRoll Type", "roll_type",
                "&7Current: &f" + crate.getRollDefinition().getRollType().name(),
                "&7Click to change animation"));
        inv.setItem(16, actionItem(Material.BLAZE_POWDER, "&6Set Particle", "set_particle",
                "&7Current: &f"
                        + (crate.getParticle() == null || crate.getParticle().isBlank() ? "None" : crate.getParticle()),
                "&7Click to set particle"));
        inv.setItem(31, actionItem(Material.NOTE_BLOCK, "&dOpen Sound", "set_open_sound",
                "&7Current: &f"
                        + (crate.getOpenSound() == null || crate.getOpenSound().isBlank() ? "None" : crate.getOpenSound()),
                "&7Play sound when crate opens"));

        List<String> hologramLines = crate.getHologramLines() == null ? List.of() : crate.getHologramLines();
        String hologramPreview = hologramLines.isEmpty() ? "Disabled" : hologramLines.get(0);
        inv.setItem(28, actionItem(Material.NAME_TAG, "&bSet Hologram", "set_hologram",
                "&7Lines: &f" + hologramLines.size(),
                "&7Top: &f" + hologramPreview,
                "&7Click to edit lines"));

        inv.setItem(44, actionItem(Material.BARRIER, "&cDelete Crate", "delete_crate",
                "&7Permanently deletes this crate",
                "&7and unbinds all related blocks",
                "&eClick to confirm in chat"));

        if (crate.getKeyId() != null && !crate.getKeyId().isBlank()) {
            inv.setItem(13, actionItem(Material.SHEARS, "&cUnbind Key", "unbind_key",
                    "&7Remove key requirement",
                    "&7from this crate"));
        }

        inv.setItem(30, actionItem(Material.CLOCK, "&eCooldown", "set_cooldown",
                "&7Current: &f" + (crate.getCooldown() > 0 ? crate.getCooldown() + "s" : "None"),
                "&7Set open cooldown in seconds"));

        inv.setItem(32, actionItem(Material.IRON_BARS, "&6Permission", "set_permission",
                "&7Current: &f"
                        + (crate.getPermission() == null || crate.getPermission().isBlank() ? "None" : crate.getPermission()),
                "&7Set required permission"));

        inv.setItem(40, actionItem(Material.ARROW, "&7Back", "back", "&7Return to crates"));

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
        inv.setItem(48, actionItem(Material.CHEST, "&aAdd Item Reward", "add_item_reward",
                "&7Click this, then click any",
                "&7item in your inventory to",
                "&7add it as a reward"));
        inv.setItem(49, actionItem(Material.COMMAND_BLOCK, "&cAdd Command Reward", "add_command_reward",
                "&7Adds COMMAND reward via chat input",
                "&7Type command only",
                "&7Then edit name/material/lore by left click"));
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Return to crate"));

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
                if (reward.getType() == RewardType.COMMAND) {
                    lore.add(ui("&7Left: edit command reward"));
                } else {
                    lore.add(ui("&7Left: change weight"));
                }
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
        CrateDefinition crate = crateService.getCrate(crateId);
        String currentKeyId = crate != null ? crate.getKeyId() : "";

        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.KEY_SELECT, crateId), 54,
                ui("&8Select Key • " + crateId));
        fillBorder(inv);
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Return to crate"));

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
                if (key.getId().equalsIgnoreCase(currentKeyId)) {
                    lore.add(ui("&aCurrent key"));
                }
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
        inv.setItem(10, actionItem(Material.HOPPER, "&bCSGO", "set_roll_csgo",
                "&7Items scroll horizontally", "&7and slow down to reveal reward"));
        inv.setItem(12, actionItem(Material.COMPASS, "&dRoulette", "set_roll_roulette",
                "&7Items rotate around", "&7the perimeter of the GUI"));
        inv.setItem(14, actionItem(Material.REDSTONE, "&aInstant", "set_roll_instant",
                "&7Instantly reveals the reward", "&7No animation"));
        inv.setItem(15, actionItem(Material.EMERALD, "&eNo Gamble", "set_roll_no_gamble",
                "&7Player picks any reward", "&7from a selection GUI"));
        inv.setItem(16, actionItem(Material.ARMOR_STAND, "&6Hologram", "set_roll_hologram",
                "&7Rewards cycle above the", "&7crate block with names",
                "&7Hologram hides during animation"));
        inv.setItem(22, actionItem(Material.ARROW, "&7Back", "back", "&7Return to crate"));
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
            case KEY_EDIT -> handleKeyEditClick(player, holder.getCrateId(), action, cursor, rawSlot, rightClick);
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

    public void startSetKeyName(Player player, String keyId) {
        if (keyId == null) {
            return;
        }
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_KEY_NAME, keyId, null));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter new key name (supports & colors)."));
        player.sendMessage(Text.chat("&7Type &fnone &7to reset to default."));
    }

    public void startSetKeyLore(Player player, String keyId) {
        if (keyId == null) {
            return;
        }
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_KEY_LORE, keyId, null));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter key lines separated by &f|&e."));
        player.sendMessage(Text.chat("&7Example: &f&7Opens starter crate|&eRare rewards"));
        player.sendMessage(Text.chat("&7Type &fnone &7to clear all lines."));
    }

    public void startSetRewardWeight(Player player, String crateId, int rewardIndex) {
        pendingInput.put(player.getUniqueId(),
                new EditorInput(EditorInputType.SET_REWARD_WEIGHT, crateId, rewardIndex));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter new weight (number)."));
    }

    public void startAddCommandReward(Player player, String crateId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.ADD_COMMAND_REWARD, crateId, null));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter command to execute on reward."));
        player.sendMessage(Text.chat("&7Example: &fcrate key give %player% starter_key 1"));
        player.sendMessage(Text.chat("&7Then left click reward to edit name/material/lore/weight."));
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

        ComponentBuilder builder = new ComponentBuilder("");
        for (int i = 0; i < popularParticles.length; i++) {
            String p = popularParticles[i];
            TextComponent comp = new TextComponent(p);
            if (p.equals("NONE")) {
                comp.setColor(ChatColor.RED);
            } else {
                comp.setColor(ChatColor.AQUA);
            }
            comp.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/justcrates _editchat " + p));
            comp.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(Text.color("&aClick to set to &f" + p)).create()));
            builder.append(comp);
            if (i < popularParticles.length - 1) {
                builder.append(new TextComponent(" | "))
                        .color(ChatColor.GRAY);
            }
        }
        player.spigot().sendMessage(builder.create());
        player.sendMessage(Text.chat("&7Or type any other particle name manually. Type &cnone&7 to clear."));
    }

    public void startSetOpenSound(Player player, String crateId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_CRATE_OPEN_SOUND, crateId, null));
        player.closeInventory();
        player.sendMessage(Messages.get("enter-open-sound"));
        player.sendMessage(Messages.get("open-sound-click-info"));

        String[] popularSounds = {
                "BLOCK_CHEST_OPEN", "BLOCK_ENDER_CHEST_OPEN", "BLOCK_BARREL_OPEN", "ENTITY_PLAYER_LEVELUP",
                "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_FIREWORK_ROCKET_BLAST", "BLOCK_AMETHYST_BLOCK_CHIME",
                "BLOCK_NOTE_BLOCK_PLING", "BLOCK_BEACON_ACTIVATE", "ITEM_TOTEM_USE", "UI_TOAST_CHALLENGE_COMPLETE",
                "NONE"
        };

        ComponentBuilder builder = new ComponentBuilder("");
        for (int i = 0; i < popularSounds.length; i++) {
            String sound = popularSounds[i];
            TextComponent comp = new TextComponent(sound);
            comp.setColor("NONE".equals(sound) ? ChatColor.RED : ChatColor.LIGHT_PURPLE);
            comp.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/justcrates _editchat " + sound));
            comp.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(Text.color("&aClick to set to &f" + sound)).create()));
            builder.append(comp);
            if (i < popularSounds.length - 1) {
                builder.append(new TextComponent(" | "))
                        .color(ChatColor.GRAY);
            }
        }
        player.spigot().sendMessage(builder.create());
        player.sendMessage(Messages.get("open-sound-type-info"));
    }

    public void startSetCrateHologram(Player player, String crateId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_CRATE_HOLOGRAM, crateId, null));
        hologramAwaitingLineInput.remove(player.getUniqueId());
        player.closeInventory();
        CrateDefinition crate = crateService.getCrate(crateId);
        List<String> currentLines = crate == null || crate.getHologramLines() == null
                ? List.of()
                : crate.getHologramLines();

        player.sendMessage(Messages.get("hologram-editor-title", "%crate%", crateId));
        sendHologramEditorChat(player, currentLines);
        player.sendMessage(Text.chat("&7Replace all: &fline1|line2|line3"));
        player.sendMessage(Text.chat("&7Edit line: &fset <line> <text>"));
        player.sendMessage(Text.chat("&7Remove line: &fremove <line>"));
        player.sendMessage(Text.chat("&7Disable: &fnone"));
        player.sendMessage(Text.chat("&7Exit editor: &fdone"));
        player.sendMessage(Text.chat("&7RGB: &f&#FFAA00My Text"));
        player.sendMessage(Messages.get("hologram-max-lines", "%value%", String.valueOf(getHologramMaxLines())));
        player.sendMessage(Text.chat("&7Placeholders: &f%crate_name% &7and &f%crate_id%"));
    }

    public void startDeleteCrate(Player player, String crateId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.DELETE_CRATE_CONFIRM, crateId, null));
        player.closeInventory();
        player.sendMessage(Messages.get("crate-delete-confirm", "%crate%", crateId));
        player.sendMessage(Text.chat("&7This will also unbind all blocks linked to it."));
        player.sendMessage(Text.chat("&7Type anything else to cancel."));
    }

    public void startDeleteKey(Player player, String keyId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.DELETE_KEY_CONFIRM, keyId, null));
        player.closeInventory();
        player.sendMessage(Messages.get("key-delete-confirm", "%key%", keyId));
        player.sendMessage(Text.chat("&7Type anything else to cancel."));
    }

    public void startSetCooldown(Player player, String crateId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_CRATE_COOLDOWN, crateId, null));
        player.closeInventory();
        player.sendMessage(Text.chat("&eEnter cooldown in seconds (0 to disable)."));
    }

    public void startSetPermission(Player player, String crateId) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_CRATE_PERMISSION, crateId, null));
        player.closeInventory();
        player.sendMessage(Messages.get("enter-permission", "%crate%", crateId));
        player.sendMessage(Text.chat("&7Type &fnone &7to remove permission requirement."));
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
        EditorInput input = pendingInput.get(player.getUniqueId());
        if (input == null) {
            return;
        }

        if (input.getType() != EditorInputType.SET_CRATE_HOLOGRAM
                && input.getType() != EditorInputType.EDIT_COMMAND_REWARD_MENU) {
            pendingInput.remove(player.getUniqueId());
        }

        switch (input.getType()) {
            case CREATE_CRATE -> handleCreateCrateInput(player, message);
            case CREATE_KEY -> handleCreateKeyInput(player, message);
            case SET_KEY_NAME -> handleSetKeyNameInput(player, message, input.getCrateId());
            case SET_KEY_LORE -> handleSetKeyLoreInput(player, message, input.getCrateId());
            case ADD_COMMAND_REWARD -> handleAddCommandRewardInput(player, message, input.getCrateId());
            case EDIT_COMMAND_REWARD_MENU -> handleEditCommandRewardMenuInput(player, message, input.getCrateId(),
                    input.getRewardIndex());
            case SET_COMMAND_REWARD_NAME -> handleSetCommandRewardNameInput(player, message, input.getCrateId(),
                    input.getRewardIndex());
            case SET_COMMAND_REWARD_WEIGHT -> handleSetCommandRewardWeightInput(player, message, input.getCrateId(),
                    input.getRewardIndex());
            case SET_COMMAND_REWARD_MATERIAL -> handleSetCommandRewardMaterialInput(player, message, input.getCrateId(),
                    input.getRewardIndex());
            case SET_COMMAND_REWARD_LORE -> handleSetCommandRewardLoreInput(player, message, input.getCrateId(),
                    input.getRewardIndex());
            case SET_COMMAND_REWARD_COMMAND -> handleSetCommandRewardCommandInput(player, message, input.getCrateId(),
                    input.getRewardIndex());
            case SET_REWARD_WEIGHT ->
                handleSetRewardWeightInput(player, message, input.getCrateId(), input.getRewardIndex());
            case SET_CRATE_PARTICLE -> handleSetCrateParticleInput(player, message, input.getCrateId());
            case SET_CRATE_OPEN_SOUND -> handleSetCrateOpenSoundInput(player, message, input.getCrateId());
            case SET_CRATE_HOLOGRAM -> handleSetCrateHologramInput(player, message, input.getCrateId());
            case DELETE_CRATE_CONFIRM -> handleDeleteCrateConfirmInput(player, message, input.getCrateId());
            case DELETE_KEY_CONFIRM -> handleDeleteKeyConfirmInput(player, message, input.getCrateId());
            case SET_CRATE_COOLDOWN -> handleSetCrateCooldownInput(player, message, input.getCrateId());
            case SET_CRATE_PERMISSION -> handleSetCratePermissionInput(player, message, input.getCrateId());
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
            default -> {}
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

    private void handleKeyEditClick(Player player, String keyId, String action, ItemStack cursor, int rawSlot,
            boolean rightClick) {
        if (keyId == null) {
            return;
        }
        if ("rename_key".equals(action) && (cursor == null || cursor.getType().isAir())) {
            if (rightClick) {
                startSetKeyLore(player, keyId);
            } else {
                startSetKeyName(player, keyId);
            }
            return;
        }
        if ("back_keys".equals(action)) {
            keyIconMode.remove(player.getUniqueId());
            openKeysMenu(player);
            return;
        }
        if ("give_key".equals(action)) {
            giveKey(player, keyId, player, 1);
            openKeyEditor(player, keyId);
            return;
        }
        if ("toggle_virtual_key".equals(action)) {
            KeyDefinition key = keyService.getKey(keyId);
            if (key == null) {
                player.sendMessage(Text.chat("&cKey does not exist."));
                return;
            }
            boolean next = !key.isVirtual();
            if (!keyService.updateKeyVirtualMode(keyId, next)) {
                player.sendMessage(Text.chat("&cFailed to update key mode."));
                return;
            }
            keyService.loadAll();
            player.sendMessage(Messages.get("key-mode-updated", "%value%", next ? "Virtual" : "Physical"));
            openKeyEditor(player, keyId);
            return;
        }
        if ("set_key_icon".equals(action)) {
            keyIconMode.put(player.getUniqueId(), keyId);
            player.sendMessage(Text.chat("&aNow click any item in your inventory to set it as the key icon."));
            return;
        }
        if ("delete_key".equals(action)) {
            startDeleteKey(player, keyId);
            return;
        }
        if (rawSlot >= 27) {
            if (keyIconMode.containsKey(player.getUniqueId())) {
                return;
            }
        }
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
        if (action == null) {
            return;
        }
        switch (action) {
            case "bind_block" -> setBindMode(player, crateId);
            case "unbind_block" -> setUnbindMode(player);
            case "select_key" -> openKeySelectMenu(player, crateId);
            case "rewards" -> openRewardsMenu(player, crateId);
            case "set_particle" -> startSetCrateParticle(player, crateId);
            case "set_open_sound" -> startSetOpenSound(player, crateId);
            case "set_hologram" -> startSetCrateHologram(player, crateId);
            case "roll_type" -> openRollTypeMenu(player, crateId);
            case "delete_crate" -> startDeleteCrate(player, crateId);
            case "unbind_key" -> {
                setCrateKey(crateId, "");
                crateService.loadAll();
                player.sendMessage(Text.chat("&aKey unbound from crate."));
                openCrateEditor(player, crateId);
            }
            case "set_cooldown" -> startSetCooldown(player, crateId);
            case "set_permission" -> startSetPermission(player, crateId);
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
        if ("add_item_reward".equals(action)) {
            rewardItemMode.put(player.getUniqueId(), crateId);
            player.sendMessage(Text.chat("&aNow click any item in your inventory to add it as a reward."));
            return;
        }
        if ("add_command_reward".equals(action)) {
            startAddCommandReward(player, crateId);
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
            CrateDefinition crate = crateService.getCrate(crateId);
            if (crate != null && index >= 0 && index < crate.getRewards().size()
                    && crate.getRewards().get(index).getType() == RewardType.COMMAND) {
                openCommandRewardEditor(player, crateId, index);
            } else {
                startSetRewardWeight(player, crateId, index);
            }
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
            case "set_roll_no_gamble" -> "NO_GAMBLE";
            case "set_roll_hologram" -> "HOLOGRAM";
            default -> null;
        };
        if (rollType != null) {
            setCrateRollType(crateId, rollType);
            crateService.loadAll();
            player.sendMessage(Messages.get("roll-type-set", "%value%", rollType));
            openCrateEditor(player, crateId);
        }
    }

    private void setCrateRollType(String crateId, String rollType) {
        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
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
        if (resolveCrateFile(id) != null) {
            player.sendMessage(Text.chat("&cA crate with this ID already exists."));
            return;
        }
        File file = new File(paths.getCratesFolder(), id + ".yml");

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
        cfg.set("sounds.open", "");
        cfg.set("cooldown", 0);
        cfg.set("permission", "");
        cfg.set("rewards", new ArrayList<>());

        try {
            cfg.save(file);
            saveCrateHologramLines(id, getDefaultHologramLines());
            crateService.loadAll();
            player.sendMessage(Messages.get("crate-created", "%crate%", id));
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
            player.sendMessage(Messages.get("key-created", "%key%", id));
            openKeyEditor(player, id);
        } else {
            player.sendMessage(Text.chat("&cFailed to create key. ID may already exist."));
        }
    }

    private void handleSetKeyNameInput(Player player, String message, String keyId) {
        if (keyId == null) {
            return;
        }

        String keyName = message == null ? "" : message.trim();
        if (keyName.equalsIgnoreCase("none")) {
            keyName = "&e" + keyId + " key";
        }
        if (keyName.isBlank()) {
            player.sendMessage(Text.chat("&cKey name cannot be empty."));
            return;
        }

        if (!keyService.updateKeyDisplayName(keyId, keyName)) {
            player.sendMessage(Text.chat("&cFailed to update key name."));
            return;
        }

        keyService.loadAll();
        player.sendMessage(Text.chat("&aKey name updated."));
        openKeyEditor(player, keyId);
    }

    private void handleSetKeyLoreInput(Player player, String message, String keyId) {
        if (keyId == null) {
            return;
        }

        String raw = message == null ? "" : message.trim();
        List<String> lore = new ArrayList<>();

        if (!raw.equalsIgnoreCase("none") && !raw.isBlank()) {
            String[] split = raw.split("\\|");
            for (String line : split) {
                String trimmed = line.trim();
                if (!trimmed.isBlank()) {
                    lore.add(trimmed);
                }
            }
        }

        if (!keyService.updateKeyLore(keyId, lore)) {
            player.sendMessage(Text.chat("&cFailed to update key lines."));
            return;
        }

        keyService.loadAll();
        player.sendMessage(Text.chat(lore.isEmpty() ? "&aKey lines cleared." : "&aKey lines updated."));
        openKeyEditor(player, keyId);
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

        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
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

    private void handleSetCrateOpenSoundInput(Player player, String message, String crateId) {
        if (crateId == null) {
            return;
        }

        String openSound = message == null ? "" : message.trim();
        if (openSound.equalsIgnoreCase("none") || openSound.isEmpty()) {
            openSound = "";
        } else {
            try {
                org.bukkit.Sound.valueOf(openSound.toUpperCase(Locale.ROOT).replace('.', '_'));
                openSound = openSound.toUpperCase(Locale.ROOT).replace('.', '_');
            } catch (IllegalArgumentException e) {
                player.sendMessage(Messages.get("open-sound-invalid"));
                return;
            }
        }

        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            player.sendMessage(Text.chat("&cCrate file not found."));
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("sounds.open", openSound);
        try {
            cfg.save(file);
            crateService.loadAll();
            player.sendMessage(Messages.get("open-sound-saved"));
            openCrateEditor(player, crateId);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate open sound: " + e.getMessage());
            player.sendMessage(Messages.get("open-sound-save-failed"));
        }
    }

    private void handleSetCrateHologramInput(Player player, String message, String crateId) {
        if (crateId == null) {
            return;
        }
        int maxLines = getHologramMaxLines();

        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            player.sendMessage(Text.chat("&cCrate file not found."));
            return;
        }

        List<String> lines = loadCurrentHologramLines(crateId, file);
        String raw = message == null ? "" : message.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        if (lower.equals("holo_done") || lower.equals("done") || lower.equals("exit") || lower.equals("cancel")) {
            pendingInput.remove(player.getUniqueId());
            hologramAwaitingLineInput.remove(player.getUniqueId());
            openCrateEditor(player, crateId);
            return;
        }

        if (lower.equals("holo_add") || lower.equals("+")) {
            if (lines.size() >= maxLines) {
                player.sendMessage(Messages.get("hologram-max-reached", "%value%", String.valueOf(maxLines)));
                sendHologramEditorChat(player, lines);
                return;
            }
            hologramAwaitingLineInput.put(player.getUniqueId(), -1);
            player.sendMessage(Messages.get("hologram-enter-new-line", "%value%", String.valueOf(lines.size() + 1)));
            return;
        }

        if (lower.startsWith("holo_edit ")) {
            String[] parts = lower.split("\\s+", 2);
            int lineIndex;
            try {
                lineIndex = Integer.parseInt(parts[1]);
            } catch (Exception e) {
                player.sendMessage(Text.chat("&cInvalid line."));
                sendHologramEditorChat(player, lines);
                return;
            }
            if (lineIndex < 1 || lineIndex > lines.size()) {
                player.sendMessage(Text.chat("&cLine out of range."));
                sendHologramEditorChat(player, lines);
                return;
            }
            hologramAwaitingLineInput.put(player.getUniqueId(), lineIndex);
            player.sendMessage(Messages.get("hologram-enter-edit-line", "%value%", String.valueOf(lineIndex)));
            return;
        }

        if (lower.startsWith("holo_remove ")) {
            String[] parts = lower.split("\\s+", 2);
            int lineIndex;
            try {
                lineIndex = Integer.parseInt(parts[1]);
            } catch (Exception e) {
                player.sendMessage(Text.chat("&cInvalid line."));
                sendHologramEditorChat(player, lines);
                return;
            }
            if (lineIndex < 1 || lineIndex > lines.size()) {
                player.sendMessage(Text.chat("&cLine out of range."));
                sendHologramEditorChat(player, lines);
                return;
            }
            lines.remove(lineIndex - 1);
            if (!saveCrateHologramLines(crateId, lines)) {
                player.sendMessage(Text.chat("&cFailed to save hologram."));
                return;
            }
            player.sendMessage(Text.chat(lines.isEmpty() ? "&aHologram disabled." : "&aLine removed."));
            sendHologramEditorChat(player, lines);
            return;
        }

        Integer awaitingLine = hologramAwaitingLineInput.remove(player.getUniqueId());
        if (awaitingLine != null) {
            if (raw.isBlank()) {
                player.sendMessage(Text.chat("&cLine text cannot be empty."));
                sendHologramEditorChat(player, lines);
                return;
            }
            if (awaitingLine == -1) {
                if (lines.size() >= maxLines) {
                    player.sendMessage(Messages.get("hologram-max-reached", "%value%", String.valueOf(maxLines)));
                    sendHologramEditorChat(player, lines);
                    return;
                }
                lines.add(raw);
            } else {
                int idx = awaitingLine - 1;
                if (idx < 0 || idx >= lines.size()) {
                    player.sendMessage(Text.chat("&cLine out of range."));
                    sendHologramEditorChat(player, lines);
                    return;
                }
                lines.set(idx, raw);
            }
            if (!saveCrateHologramLines(crateId, lines)) {
                player.sendMessage(Text.chat("&cFailed to save hologram."));
                return;
            }
            player.sendMessage(Text.chat("&aHologram updated."));
            sendHologramEditorChat(player, lines);
            return;
        }

        if (lower.equals("none")) {
            lines.clear();
        } else if (lower.startsWith("set ")) {
            String[] parts = raw.split("\\s+", 3);
            if (parts.length < 3) {
                player.sendMessage(Text.chat("&cUsage: set <line> <text>"));
                sendHologramEditorChat(player, lines);
                return;
            }
            int lineIndex;
            try {
                lineIndex = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Text.chat("&cLine must be a number."));
                sendHologramEditorChat(player, lines);
                return;
            }
            if (lineIndex < 1 || lineIndex > maxLines) {
                player.sendMessage(Messages.get("hologram-line-range", "%value%", String.valueOf(maxLines)));
                sendHologramEditorChat(player, lines);
                return;
            }

            while (lines.size() < lineIndex) {
                lines.add("");
            }
            lines.set(lineIndex - 1, parts[2].trim());
            lines.removeIf(String::isBlank);
        } else if (lower.startsWith("remove ")) {
            String[] parts = raw.split("\\s+", 2);
            if (parts.length < 2) {
                player.sendMessage(Text.chat("&cUsage: remove <line>"));
                sendHologramEditorChat(player, lines);
                return;
            }
            int lineIndex;
            try {
                lineIndex = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Text.chat("&cLine must be a number."));
                sendHologramEditorChat(player, lines);
                return;
            }
            if (lineIndex < 1 || lineIndex > lines.size()) {
                player.sendMessage(Text.chat("&cLine out of range."));
                sendHologramEditorChat(player, lines);
                return;
            }
            lines.remove(lineIndex - 1);
        } else if (!raw.isBlank()) {
            lines.clear();
            String[] split = raw.split("\\|");
            for (String part : split) {
                String line = part.trim();
                if (!line.isBlank()) {
                    lines.add(line);
                }
                if (lines.size() >= maxLines) {
                    break;
                }
            }
        } else {
            player.sendMessage(Text.chat("&cInput is empty."));
            sendHologramEditorChat(player, lines);
            return;
        }

        if (!saveCrateHologramLines(crateId, lines)) {
            player.sendMessage(Text.chat("&cFailed to save hologram."));
            return;
        }
        player.sendMessage(Text.chat(lines.isEmpty() ? "&aHologram disabled." : "&aHologram updated."));
        sendHologramEditorChat(player, lines);
    }

    private void handleDeleteCrateConfirmInput(Player player, String message, String crateId) {
        if (crateId == null) {
            return;
        }

        String expected = "delete " + crateId.toLowerCase(Locale.ROOT);
        String input = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (!expected.equals(input)) {
            player.sendMessage(Text.chat("&7Crate deletion cancelled."));
            openCrateEditor(player, crateId);
            return;
        }

        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            player.sendMessage(Text.chat("&cCrate file not found."));
            openCratesMenu(player);
            return;
        }

        int unbound = blockCrateService.unbindAll(crateId);
        boolean deleted = file.delete();
        if (!deleted) {
            player.sendMessage(Text.chat("&cFailed to delete crate file."));
            openCrateEditor(player, crateId);
            return;
        }
        File hologramFile = resolveHologramFile(crateId);
        if (hologramFile.exists()) {
            hologramFile.delete();
        }

        blockCrateService.save();
        crateService.loadAll();
        blockCrateService.refreshHolograms();
        player.sendMessage(Messages.get(
                "crate-deleted",
                "%crate%", crateId,
                "%count%", String.valueOf(unbound)));
        openCratesMenu(player);
    }

    private void handleDeleteKeyConfirmInput(Player player, String message, String keyId) {
        if (keyId == null) {
            return;
        }

        String expected = "delete " + keyId.toLowerCase(Locale.ROOT);
        String input = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (!expected.equals(input)) {
            player.sendMessage(Text.chat("&7Key deletion cancelled."));
            openKeyEditor(player, keyId);
            return;
        }

        if (!keyService.deleteKey(keyId)) {
            player.sendMessage(Text.chat("&cFailed to delete key file."));
            openKeyEditor(player, keyId);
            return;
        }

        for (CrateDefinition crate : crateService.getCrates()) {
            if (keyId.equalsIgnoreCase(crate.getKeyId())) {
                setCrateKey(crate.getId(), "");
            }
        }

        keyService.loadAll();
        crateService.loadAll();
        player.sendMessage(Messages.get("key-deleted", "%key%", keyId));
        openKeysMenu(player);
    }

    private void handleSetCrateCooldownInput(Player player, String message, String crateId) {
        if (crateId == null) {
            return;
        }
        int cooldown;
        try {
            cooldown = Math.max(0, Integer.parseInt(message.trim()));
        } catch (NumberFormatException e) {
            player.sendMessage(Text.chat("&cInvalid number."));
            return;
        }

        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            player.sendMessage(Text.chat("&cCrate file not found."));
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("cooldown", cooldown);
        try {
            cfg.save(file);
            crateService.loadAll();
            player.sendMessage(Messages.get(
                    "cooldown-set",
                    "%value%", cooldown > 0 ? cooldown + "s" : "Disabled"));
            openCrateEditor(player, crateId);
        } catch (IOException e) {
            player.sendMessage(Text.chat("&cFailed to save cooldown."));
        }
    }

    private void handleSetCratePermissionInput(Player player, String message, String crateId) {
        if (crateId == null) {
            return;
        }
        String perm = message == null ? "" : message.trim();
        if (perm.equalsIgnoreCase("none")) {
            perm = "";
        }

        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            player.sendMessage(Text.chat("&cCrate file not found."));
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("permission", perm);
        try {
            cfg.save(file);
            crateService.loadAll();
            player.sendMessage(Messages.get(
                    "permission-set",
                    "%value%", perm.isBlank() ? "None" : perm));
            openCrateEditor(player, crateId);
        } catch (IOException e) {
            player.sendMessage(Text.chat("&cFailed to save permission."));
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
            player.sendMessage(Messages.get("weight-update-failed"));
            return;
        }
        crateService.loadAll();
        openRewardsMenu(player, crateId);
    }

    private void handleAddCommandRewardInput(Player player, String message, String crateId) {
        if (crateId == null) {
            return;
        }
        String raw = message == null ? "" : message.trim();
        if (raw.isBlank()) {
            player.sendMessage(Text.chat("&cCommand cannot be empty."));
            return;
        }

        if (!addCommandReward(crateId, raw)) {
            player.sendMessage(Text.chat("&cFailed to add command reward."));
            return;
        }

        crateService.loadAll();
        player.sendMessage(Text.chat("&aCommand reward added."));
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null || crate.getRewards().isEmpty()) {
            openRewardsMenu(player, crateId);
            return;
        }
        openCommandRewardEditor(player, crateId, crate.getRewards().size() - 1);
    }

    private void openCommandRewardEditor(Player player, String crateId, int rewardIndex) {
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null || rewardIndex < 0 || rewardIndex >= crate.getRewards().size()) {
            player.sendMessage(Text.chat("&cReward not found."));
            openRewardsMenu(player, crateId);
            return;
        }
        RewardDefinition reward = crate.getRewards().get(rewardIndex);
        if (reward.getType() != RewardType.COMMAND) {
            startSetRewardWeight(player, crateId, rewardIndex);
            return;
        }

        pendingInput.put(player.getUniqueId(),
                new EditorInput(EditorInputType.EDIT_COMMAND_REWARD_MENU, crateId, rewardIndex));
        player.closeInventory();
        player.sendMessage(Messages.get("cmd-reward-editor-title", "%value%", String.valueOf(rewardIndex + 1)));
        player.sendMessage(Messages.get(
                "cmd-reward-name",
                "%value%",
                reward.getPreviewName() == null || reward.getPreviewName().isBlank() ? "&cNone" : reward.getPreviewName()));
        player.sendMessage(Messages.get("cmd-reward-weight", "%value%", String.valueOf(reward.getWeight())));
        player.sendMessage(Messages.get(
                "cmd-reward-material",
                "%value%",
                reward.getPreviewMaterial() == null || reward.getPreviewMaterial().isBlank() ? "COMMAND_BLOCK"
                        : reward.getPreviewMaterial()));
        player.sendMessage(Messages.get(
                "cmd-reward-lore-lines",
                "%value%",
                String.valueOf(reward.getPreviewLore() == null ? 0 : reward.getPreviewLore().size())));
        player.sendMessage(Messages.get(
                "cmd-reward-command",
                "%value%",
                reward.getCommands() == null || reward.getCommands().isEmpty() ? "&cNone" : reward.getCommands().get(0)));

        sendCommandRewardAction(player, "&b[Set Name]", "cmdr_set_name " + rewardIndex, "&7Change preview display name");
        sendCommandRewardAction(player, "&e[Set Weight]", "cmdr_set_weight " + rewardIndex, "&7Change reward weight");
        sendCommandRewardAction(player, "&d[Set Material]", "cmdr_set_material " + rewardIndex, "&7Change preview material");
        sendCommandRewardAction(player, "&a[Set Lore]", "cmdr_set_lore " + rewardIndex,
                "&7Set lore lines with | separator, or none");
        sendCommandRewardAction(player, "&6[Set Command]", "cmdr_set_command " + rewardIndex, "&7Change executed command");
        sendCommandRewardAction(player, "&7[Done]", "cmdr_done", "&7Back to rewards menu");
    }

    private void handleEditCommandRewardMenuInput(Player player, String message, String crateId, Integer rewardIndex) {
        if (crateId == null || rewardIndex == null || message == null) {
            return;
        }
        String input = message.trim().toLowerCase(Locale.ROOT);
        if (input.equals("cmdr_done") || input.equals("done")) {
            pendingInput.remove(player.getUniqueId());
            openRewardsMenu(player, crateId);
            return;
        }

        if (input.startsWith("cmdr_set_name")) {
            pendingInput.put(player.getUniqueId(),
                    new EditorInput(EditorInputType.SET_COMMAND_REWARD_NAME, crateId, rewardIndex));
            player.sendMessage(Text.chat("&eType new preview name. Use colors (&). Type &fnone &eto clear."));
            return;
        }
        if (input.startsWith("cmdr_set_weight")) {
            pendingInput.put(player.getUniqueId(),
                    new EditorInput(EditorInputType.SET_COMMAND_REWARD_WEIGHT, crateId, rewardIndex));
            player.sendMessage(Text.chat("&eType new weight (number)."));
            return;
        }
        if (input.startsWith("cmdr_set_material")) {
            pendingInput.put(player.getUniqueId(),
                    new EditorInput(EditorInputType.SET_COMMAND_REWARD_MATERIAL, crateId, rewardIndex));
            player.sendMessage(Text.chat("&eType preview material (e.g. &fGOLD_INGOT&e)."));
            return;
        }
        if (input.startsWith("cmdr_set_lore")) {
            pendingInput.put(player.getUniqueId(),
                    new EditorInput(EditorInputType.SET_COMMAND_REWARD_LORE, crateId, rewardIndex));
            player.sendMessage(Text.chat("&eType lore lines separated by &f|&e. Type &fnone &eto clear lore."));
            return;
        }
        if (input.startsWith("cmdr_set_command")) {
            pendingInput.put(player.getUniqueId(),
                    new EditorInput(EditorInputType.SET_COMMAND_REWARD_COMMAND, crateId, rewardIndex));
            player.sendMessage(Text.chat("&eType command to run on reward."));
            return;
        }
        player.sendMessage(Text.chat("&cUnknown action."));
        openCommandRewardEditor(player, crateId, rewardIndex);
    }

    private void handleSetCommandRewardNameInput(Player player, String message, String crateId, Integer rewardIndex) {
        if (crateId == null || rewardIndex == null) {
            return;
        }
        String value = message == null ? "" : message.trim();
        if (value.equalsIgnoreCase("none")) {
            value = "";
        }
        final String previewNameValue = value;
        if (!updateCommandReward(crateId, rewardIndex, map -> map.put("preview-name", previewNameValue))) {
            player.sendMessage(Text.chat("&cFailed to update name."));
            return;
        }
        crateService.loadAll();
        player.sendMessage(Text.chat("&aPreview name updated."));
        openCommandRewardEditor(player, crateId, rewardIndex);
    }

    private void handleSetCommandRewardWeightInput(Player player, String message, String crateId, Integer rewardIndex) {
        if (crateId == null || rewardIndex == null) {
            return;
        }
        int weight;
        try {
            weight = Math.max(1, Integer.parseInt(message.trim()));
        } catch (Exception e) {
            player.sendMessage(Text.chat("&cInvalid number."));
            return;
        }
        if (!updateCommandReward(crateId, rewardIndex, map -> map.put("weight", weight))) {
            player.sendMessage(Messages.get("weight-update-failed"));
            return;
        }
        crateService.loadAll();
        player.sendMessage(Text.chat("&aWeight updated."));
        openCommandRewardEditor(player, crateId, rewardIndex);
    }

    private void handleSetCommandRewardMaterialInput(Player player, String message, String crateId,
            Integer rewardIndex) {
        if (crateId == null || rewardIndex == null || message == null) {
            return;
        }
        String material = message.trim().toUpperCase(Locale.ROOT);
        try {
            Material.valueOf(material);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.chat("&cInvalid material."));
            return;
        }
        if (!updateCommandReward(crateId, rewardIndex, map -> map.put("preview-material", material))) {
            player.sendMessage(Text.chat("&cFailed to update material."));
            return;
        }
        crateService.loadAll();
        player.sendMessage(Text.chat("&aPreview material updated."));
        openCommandRewardEditor(player, crateId, rewardIndex);
    }

    private void handleSetCommandRewardLoreInput(Player player, String message, String crateId, Integer rewardIndex) {
        if (crateId == null || rewardIndex == null || message == null) {
            return;
        }
        String raw = message.trim();
        List<String> lore = new ArrayList<>();
        if (!raw.equalsIgnoreCase("none") && !raw.isBlank()) {
            String[] split = raw.split("\\|");
            for (String line : split) {
                String trimmed = line.trim();
                if (!trimmed.isBlank()) {
                    lore.add(trimmed);
                }
            }
        }

        if (!updateCommandReward(crateId, rewardIndex, map -> map.put("preview-lore", lore))) {
            player.sendMessage(Text.chat("&cFailed to update lore."));
            return;
        }
        crateService.loadAll();
        player.sendMessage(Text.chat("&aPreview lore updated."));
        openCommandRewardEditor(player, crateId, rewardIndex);
    }

    private void handleSetCommandRewardCommandInput(Player player, String message, String crateId, Integer rewardIndex) {
        if (crateId == null || rewardIndex == null || message == null) {
            return;
        }
        String cmd = message.trim();
        if (cmd.isBlank()) {
            player.sendMessage(Text.chat("&cCommand cannot be empty."));
            return;
        }

        if (!updateCommandReward(crateId, rewardIndex, map -> map.put("commands", List.of(cmd)))) {
            player.sendMessage(Text.chat("&cFailed to update command."));
            return;
        }
        crateService.loadAll();
        player.sendMessage(Text.chat("&aCommand updated."));
        openCommandRewardEditor(player, crateId, rewardIndex);
    }

    private void setCrateKey(String crateId, String keyId) {
        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
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
        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
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
        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
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

    private boolean addCommandReward(String crateId, String command) {
        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            return false;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rewards = cfg.getMapList("rewards");

        List<Map<String, Object>> updated = new ArrayList<>();
        for (Map<?, ?> reward : rewards) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : reward.entrySet()) {
                if (entry.getKey() != null) {
                    map.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            updated.add(map);
        }

        Map<String, Object> commandReward = new HashMap<>();
        commandReward.put("type", "COMMAND");
        commandReward.put("weight", 1);
        commandReward.put("commands", List.of(command));
        commandReward.put("preview-material", "COMMAND_BLOCK");
        commandReward.put("preview-name", "&cCommand Reward");
        commandReward.put("preview-lore", new ArrayList<>());
        updated.add(commandReward);

        cfg.set("rewards", updated);
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to add command reward: " + e.getMessage());
            return false;
        }
    }

    private boolean addItemReward(String crateId, ItemStack item) {
        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            return false;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rewards = cfg.getMapList("rewards");

        List<Map<String, Object>> updated = new ArrayList<>();
        for (Map<?, ?> reward : rewards) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : reward.entrySet()) {
                if (entry.getKey() != null) {
                    map.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            updated.add(map);
        }

        YamlConfiguration temp = new YamlConfiguration();
        temp.set("itemstack", item);
        Object serialized = temp.get("itemstack");

        Map<String, Object> itemReward = new HashMap<>();
        itemReward.put("type", "ITEM");
        itemReward.put("weight", 1);
        itemReward.put("itemstack", serialized);
        updated.add(itemReward);

        cfg.set("rewards", updated);
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to add item reward: " + e.getMessage());
            return false;
        }
    }

    private boolean updateCommandReward(String crateId, int index, java.util.function.Consumer<Map<String, Object>> updater) {
        File file = resolveCrateFile(crateId);
        if (file == null || !file.exists()) {
            return false;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rewards = cfg.getMapList("rewards");
        if (index < 0 || index >= rewards.size()) {
            return false;
        }

        List<Map<String, Object>> updated = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            Map<?, ?> reward = rewards.get(i);
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : reward.entrySet()) {
                if (entry.getKey() != null) {
                    map.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            if (i == index) {
                Object type = map.get("type");
                if (type == null || !"COMMAND".equalsIgnoreCase(String.valueOf(type))) {
                    return false;
                }
                updater.accept(map);
            }
            updated.add(map);
        }
        cfg.set("rewards", updated);
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update command reward: " + e.getMessage());
            return false;
        }
    }

    private void sendCommandRewardAction(Player player, String label, String action, String hover) {
        TextComponent button = new TextComponent(Text.color(label));
        button.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/justcrates _editchat " + action));
        button.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Text.color(hover)).create()));
        player.spigot().sendMessage(button);
    }

    private void giveKey(Player sender, String keyId, Player target, int amount) {
        KeyDefinition key = keyService.getKey(keyId);
        if (key == null) {
            sender.sendMessage(Text.chat("&cKey does not exist."));
            return;
        }
        if (key.isVirtual()) {
            VirtualKeyService virtualKeyService = getVirtualKeyService();
            if (virtualKeyService == null) {
                sender.sendMessage(Text.chat("&cVirtual key service is not available."));
                return;
            }
            virtualKeyService.addKeys(target.getUniqueId(), key.getId(), amount);
            if (sender.getUniqueId().equals(target.getUniqueId())) {
                sender.sendMessage(Messages.get(
                        "given-virtual-key-self",
                        "%key%", keyId,
                        "%amount%", String.valueOf(amount)));
            } else {
                sender.sendMessage(Messages.get(
                        "given-virtual-key-other",
                        "%player%", target.getName(),
                        "%key%", keyId,
                        "%amount%", String.valueOf(amount)));
            }
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

    private VirtualKeyService getVirtualKeyService() {
        if (plugin instanceof JustCrates justCrates) {
            return justCrates.getVirtualKeyService();
        }
        return null;
    }

    private boolean saveKeyItemFromCursor(Player player, String keyId, ItemStack cursor) {
        ItemStack icon = cursor.clone();
        icon.setAmount(1);
        boolean saved = keyService.updateKeyItemStack(keyId, icon);
        if (!saved) {
            return false;
        }
        keyService.loadAll();
        player.sendMessage(Messages.get("key-icon-updated", "%key%", keyId));
        openKeyEditor(player, keyId);
        return true;
    }

    private boolean saveCrateHologramLines(String crateId, List<String> lines) {
        File file = resolveHologramFile(crateId);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("crate-id", crateId.toLowerCase(Locale.ROOT));
        cfg.set("lines", lines);
        try {
            cfg.save(file);
            crateService.loadAll();
            blockCrateService.refreshHolograms();
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate hologram: " + e.getMessage());
            return false;
        }
    }

    private List<String> loadCurrentHologramLines(String crateId, File crateFile) {
        File hologramFile = resolveHologramFile(crateId);
        if (hologramFile.exists()) {
            YamlConfiguration hCfg = YamlConfiguration.loadConfiguration(hologramFile);
            return new ArrayList<>(hCfg.getStringList("lines"));
        }

        if (crateFile != null && crateFile.exists()) {
            YamlConfiguration cCfg = YamlConfiguration.loadConfiguration(crateFile);
            if (cCfg.contains("display.hologram-lines")) {
                return new ArrayList<>(cCfg.getStringList("display.hologram-lines"));
            }
        }

        return new ArrayList<>(getDefaultHologramLines());
    }

    private int getHologramMaxLines() {
        return Math.max(1, plugin.getConfig().getInt("hologram.max-lines", 6));
    }

    private List<String> getDefaultHologramLines() {
        List<String> defaults = plugin.getConfig().getStringList("hologram.default-lines");
        if (defaults.isEmpty()) {
            defaults = List.of("&e%crate_name%", "&7store: &fyour.store.com");
        }
        int maxLines = getHologramMaxLines();
        if (defaults.size() <= maxLines) {
            return defaults;
        }
        return new ArrayList<>(defaults.subList(0, maxLines));
    }

    private void sendHologramEditorChat(Player player, List<String> lines) {
        if (lines.isEmpty()) {
            player.sendMessage(Text.chat("&7Current lines: &cnone"));
        } else {
            for (int i = 0; i < lines.size(); i++) {
                int lineIndex = i + 1;

                TextComponent line = new TextComponent(
                        Text.color("&7line " + lineIndex + ": &f" + lines.get(i)));
                line.setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/justcrates _editchat holo_edit " + lineIndex));
                line.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(Text.color("&aClick to edit this line")).create()));

                TextComponent spacer = new TextComponent(" ");

                TextComponent remove = new TextComponent(
                        Text.color("&c[X]"));
                remove.setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/justcrates _editchat holo_remove " + lineIndex));
                remove.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(Text.color("&cRemove line " + lineIndex))
                                .create()));

                player.spigot().sendMessage(line, spacer, remove);
            }
        }

        TextComponent add = new TextComponent(
                Text.color("&a[+ Add line]"));
        add.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/justcrates _editchat holo_add"));
        add.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Text.color("&aClick and then type line text")).create()));

        TextComponent spacer = new TextComponent(" ");

        TextComponent done = new TextComponent(Text.color("&7[Done]"));
        done.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/justcrates _editchat holo_done"));
        done.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Text.color("&7Back to crate editor")).create()));

        player.spigot().sendMessage(add, spacer, done);
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
        inv.setItem(0, corner);
        inv.setItem(8, corner);
        inv.setItem(size - 9, corner);
        inv.setItem(size - 1, corner);
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
            Material material = Material.COMMAND_BLOCK;
            if (reward.getPreviewMaterial() != null && !reward.getPreviewMaterial().isBlank()) {
                try {
                    material = Material.valueOf(reward.getPreviewMaterial().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    material = Material.COMMAND_BLOCK;
                }
            }
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                String name = reward.getPreviewName() == null || reward.getPreviewName().isBlank()
                        ? "&cCommand Reward"
                        : reward.getPreviewName();
                meta.setDisplayName(ui(name));
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

    private File resolveCrateFile(String crateId) {
        if (crateId == null || crateId.isBlank()) {
            return null;
        }
        String normalizedId = crateId.toLowerCase(Locale.ROOT);

        File direct = new File(paths.getCratesFolder(), normalizedId + ".yml");
        if (direct.exists()) {
            return direct;
        }

        File[] files = paths.getCratesFolder().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return null;
        }

        for (File file : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                String id = cfg.getString("id", file.getName().replace(".yml", "")).toLowerCase(Locale.ROOT);
                if (normalizedId.equals(id)) {
                    return file;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private File resolveHologramFile(String crateId) {
        String normalizedId = crateId == null ? "" : crateId.toLowerCase(Locale.ROOT);
        return new File(paths.getHologramsFolder(), normalizedId + ".yml");
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
