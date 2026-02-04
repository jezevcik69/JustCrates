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

    public EditorInput getPendingInput(Player player) {
        return pendingInput.get(player.getUniqueId());
    }

    public void clearPendingInput(Player player) {
        pendingInput.remove(player.getUniqueId());
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.MAIN, null), 27, Text.color("&8JustCrates"));
        fillBorder(inv);
        inv.setItem(11, actionItem(Material.CHEST, "&aCrates", "crates",
            "&7Crate management", "&7Create, rewards, binding"));
        inv.setItem(15, actionItem(Material.TRIPWIRE_HOOK, "&eKeys", "keys",
            "&7Key management", "&7Create and give"));
        inv.setItem(22, actionItem(Material.BARRIER, "&cClose", "close", "&7Close editor"));
        player.openInventory(inv);
    }

    public void openCratesMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.CRATES, null), 54, Text.color("&8Crates"));
        fillBorder(inv);
        inv.setItem(49, actionItem(Material.EMERALD_BLOCK, "&aCreate Crate", "create_crate",
            "&7Enter new ID"));
        inv.setItem(53, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));

        int slot = 10;
        for (CrateDefinition crate : crateService.getCrates()) {
            if (slot >= 45) {
                break;
            }
            ItemStack icon = new ItemStack(Material.CHEST);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Text.color(crate.getName()));
                List<String> lore = new ArrayList<>();
                lore.add(Text.color("&7ID: &f" + crate.getId()));
                lore.add(Text.color("&7Type: &f" + crate.getType().name()));
                if (crate.getKeyId() != null && !crate.getKeyId().isBlank()) {
                    lore.add(Text.color("&7Key: &f" + crate.getKeyId()));
                } else {
                    lore.add(Text.color("&7Key: &cNone"));
                }
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
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.KEYS, null), 54, Text.color("&8Keys"));
        fillBorder(inv);
        inv.setItem(49, actionItem(Material.EMERALD_BLOCK, "&aCreate Key", "create_key",
            "&7Hold an item in hand", "&7and enter an ID"));
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
                lore.add(Text.color("&7Click for 1x key"));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot = nextSlot(slot);
        }

        player.openInventory(inv);
    }

    public void openCrateEditor(Player player, String crateId) {
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null) {
            player.sendMessage(Text.color("&cCrate does not exist."));
            return;
        }

        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.CRATE_EDIT, crateId), 45, Text.color("&8Crate: " + crateId));
        fillBorder(inv);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Text.color("&e" + crate.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(Text.color("&7ID: &f" + crate.getId()));
            lore.add(Text.color("&7Type: &f" + crate.getType().name()));
            lore.add(Text.color("&7Key: &f" + (crate.getKeyId() == null || crate.getKeyId().isBlank() ? "&cNone" : crate.getKeyId())));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        inv.setItem(11, actionItem(Material.ANVIL, "&aBind Block", "bind_block",
            "&7Click a block", "&7to bind"));
        inv.setItem(13, actionItem(Material.TRIPWIRE_HOOK, "&eSelect Key", "select_key",
            "&7Select an existing key"));
        inv.setItem(15, actionItem(Material.CHEST, "&bRewards", "rewards",
            "&7Add and edit"));
        inv.setItem(29, actionItem(Material.DIAMOND, "&aAdd Reward From Hand", "add_reward_hand",
            "&7Takes item from hand"));
        inv.setItem(40, actionItem(Material.ARROW, "&7Back", "back", "&7Back"));

        player.openInventory(inv);
    }

    public void openRewardsMenu(Player player, String crateId) {
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null) {
            player.sendMessage(Text.color("&cCrate does not exist."));
            return;
        }

        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.REWARDS, crateId), 54, Text.color("&8Rewards: " + crateId));
        fillBorder(inv);
        inv.setItem(49, actionItem(Material.DIAMOND, "&aAdd Reward From Hand", "add_reward_hand",
            "&7Add item reward"));
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
                lore.add(Text.color("&7Weight: &f" + reward.getWeight()));
                lore.add(Text.color("&7Left: change weight"));
                lore.add(Text.color("&7Right: remove"));
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
        Inventory inv = Bukkit.createInventory(new EditorMenuHolder(EditorMenuType.KEY_SELECT, crateId), 54, Text.color("&8Select Key"));
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
                lore.add(Text.color("&7Click to select"));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slot = nextSlot(slot);
        }

        player.openInventory(inv);
    }

    public void handleMenuClick(Player player, EditorMenuHolder holder, ItemStack clicked, boolean rightClick) {
        if (holder == null) {
            return;
        }
        EditorMenuType type = holder.getType();
        PersistentDataContainer data = clicked != null && clicked.hasItemMeta() ? clicked.getItemMeta().getPersistentDataContainer() : null;
        String action = data != null ? data.get(actionKey, PersistentDataType.STRING) : null;

        switch (type) {
            case MAIN -> handleMainClick(player, action);
            case CRATES -> handleCratesClick(player, action, data);
            case KEYS -> handleKeysClick(player, action, data);
            case CRATE_EDIT -> handleCrateEditClick(player, holder.getCrateId(), action);
            case REWARDS -> handleRewardsClick(player, holder.getCrateId(), action, data, rightClick);
            case KEY_SELECT -> handleKeySelectClick(player, holder.getCrateId(), action, data);
        }
    }

    public void startCreateCrate(Player player) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.CREATE_CRATE, null, null));
        player.closeInventory();
        player.sendMessage(Text.color("&eEnter crate ID (e.g. &fstarter& e)."));
    }

    public void startCreateKey(Player player) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.CREATE_KEY, null, null));
        player.closeInventory();
        player.sendMessage(Text.color("&eHold an item in hand and enter a key ID."));
    }

    public void startSetRewardWeight(Player player, String crateId, int rewardIndex) {
        pendingInput.put(player.getUniqueId(), new EditorInput(EditorInputType.SET_REWARD_WEIGHT, crateId, rewardIndex));
        player.closeInventory();
        player.sendMessage(Text.color("&eEnter new weight (number)."));
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
        player.sendMessage(Text.color("&aClick a block to bind the crate."));
    }

    public void clearBindMode(Player player) {
        bindMode.remove(player.getUniqueId());
    }

    public void handleChatInput(Player player, String message) {
        EditorInput input = pendingInput.remove(player.getUniqueId());
        if (input == null) {
            return;
        }

        switch (input.getType()) {
            case CREATE_CRATE -> handleCreateCrateInput(player, message);
            case CREATE_KEY -> handleCreateKeyInput(player, message);
            case SET_REWARD_WEIGHT -> handleSetRewardWeightInput(player, message, input.getCrateId(), input.getRewardIndex());
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

    private void handleKeysClick(Player player, String action, PersistentDataContainer data) {
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
            giveKey(player, keyId, player, 1);
        }
    }

    private void handleCrateEditClick(Player player, String crateId, String action) {
        if (crateId == null) {
            return;
        }
        switch (action) {
            case "bind_block" -> setBindMode(player, crateId);
            case "select_key" -> openKeySelectMenu(player, crateId);
            case "rewards" -> openRewardsMenu(player, crateId);
            case "add_reward_hand" -> addRewardFromHand(player, crateId);
            case "back" -> openCratesMenu(player);
            default -> {
            }
        }
    }

    private void handleRewardsClick(Player player, String crateId, String action, PersistentDataContainer data, boolean rightClick) {
        if ("add_reward_hand".equals(action)) {
            addRewardFromHand(player, crateId);
            return;
        }
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

    private void handleCreateCrateInput(Player player, String message) {
        String id = normalizeId(message);
        if (id == null) {
            player.sendMessage(Text.color("&cInvalid ID. Use a-z, 0-9, - or _."));
            return;
        }
        File file = new File(paths.getCratesFolder(), id + ".yml");
        if (file.exists()) {
            player.sendMessage(Text.color("&cA crate with this ID already exists."));
            return;
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", id);
        cfg.set("type", "GUI");
        cfg.set("key", "");
        cfg.set("display.name", "&aCrate " + id);
        cfg.set("display.lore", List.of("&7Edit me"));
        cfg.set("roll.size", 27);
        cfg.set("roll.title", "&aCrate " + id);
        cfg.set("roll.duration-ticks", 60);
        cfg.set("roll.tick-interval", 2);
        cfg.set("rewards", new ArrayList<>());

        try {
            cfg.save(file);
            crateService.loadAll();
            player.sendMessage(Text.color("&aCrate created: " + id));
            openCrateEditor(player, id);
        } catch (IOException e) {
            player.sendMessage(Text.color("&cFailed to save crate."));
        }
    }

    private void handleCreateKeyInput(Player player, String message) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(Text.color("&cHold an item in hand."));
            return;
        }
        String id = normalizeId(message);
        if (id == null) {
            player.sendMessage(Text.color("&cInvalid ID. Use a-z, 0-9, - or _."));
            return;
        }
        File file = new File(paths.getKeysFolder(), id + ".yml");
        if (file.exists()) {
            player.sendMessage(Text.color("&cA key with this ID already exists."));
            return;
        }
        boolean saved = keyService.saveKeyFromItem(id, hand);
        if (saved) {
            keyService.loadAll();
            player.sendMessage(Text.color("&aKey created: " + id));
            openKeysMenu(player);
        } else {
            player.sendMessage(Text.color("&cFailed to save key."));
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
            player.sendMessage(Text.color("&cInvalid number."));
            return;
        }
        if (!setRewardWeight(crateId, index, weight)) {
            player.sendMessage(Text.color("&cFailed to update reward."));
            return;
        }
        crateService.loadAll();
        openRewardsMenu(player, crateId);
    }

    private void addRewardFromHand(Player player, String crateId) {
        if (crateId == null) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(Text.color("&cHold an item in hand."));
            return;
        }
        if (!addItemReward(crateId, hand)) {
            player.sendMessage(Text.color("&cFailed to add reward."));
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

    private boolean addItemReward(String crateId, ItemStack itemStack) {
        File file = new File(paths.getCratesFolder(), crateId.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
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
        Map<String, Object> newReward = new HashMap<>();
        newReward.put("type", "ITEM");
        newReward.put("weight", 1);
        newReward.put("itemstack", itemStack.clone());
        updated.add(newReward);
        cfg.set("rewards", updated);
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save reward: " + e.getMessage());
            return false;
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
            sender.sendMessage(Text.color("&cKey does not exist."));
            return;
        }
        ItemStack item = keyService.createKeyItem(key);
        if (item == null) {
            sender.sendMessage(Text.color("&cFailed to build key item."));
            return;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        if (!sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(Text.color("&aKey given."));
        }
    }

    private ItemStack actionItem(Material material, String name, String action, String... loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(Text.color(line));
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
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, pane);
            }
        }
    }

    private ItemStack rewardIcon(RewardDefinition reward) {
        if (reward.getType() == RewardType.COMMAND) {
            ItemStack stack = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Text.color("&cCommand Reward"));
                stack.setItemMeta(meta);
            }
            return stack;
        }
        if (reward.getItemStack() != null) {
            return reward.getItemStack().clone();
        }
        if (reward.getItemDefinition() != null) {
            Material mat = reward.getItemDefinition().getMaterial() != null ? reward.getItemDefinition().getMaterial() : Material.STONE;
            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Text.color(reward.getItemDefinition().getDisplayNameOrDefault()));
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
}
