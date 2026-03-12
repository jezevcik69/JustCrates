package dev.justteam.justCrates.crate;

import dev.justteam.justCrates.core.Messages;
import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.gui.roll.RollGuiFactory;
import dev.justteam.justCrates.item.ItemDefinition;
import dev.justteam.justCrates.item.ItemFactory;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
import dev.justteam.justCrates.provider.ProviderRegistry;
import dev.justteam.justCrates.reward.RewardDefinition;
import dev.justteam.justCrates.reward.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CrateService {
    private final JavaPlugin plugin;
    private final ProviderRegistry providerRegistry;
    private final PluginPaths paths;
    private final KeyService keyService;
    private final VirtualKeyService virtualKeyService;
    private final CrateRegistry registry;
    private final ItemFactory itemFactory;
    private final Random random;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private BlockCrateService blockCrateService;

    public CrateService(JavaPlugin plugin, ProviderRegistry providerRegistry, PluginPaths paths, KeyService keyService, VirtualKeyService virtualKeyService) {
        this.plugin = plugin;
        this.providerRegistry = providerRegistry;
        this.paths = paths;
        this.keyService = keyService;
        this.virtualKeyService = virtualKeyService;
        this.registry = new CrateRegistry();
        this.itemFactory = new ItemFactory(plugin, providerRegistry);
        this.random = new Random();
    }

    public void setBlockCrateService(BlockCrateService blockCrateService) {
        this.blockCrateService = blockCrateService;
    }

    public void loadAll() {
        registry.clear();
        File[] files = paths.getCratesFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String id = cfg.getString("id", file.getName().replace(".yml", "")).toLowerCase();
            CrateType type = CrateType.valueOf(cfg.getString("type", "GUI").toUpperCase());
            String name = cfg.getString("display.name", "&aCrate " + id);
            List<String> lore = cfg.getStringList("display.lore");
            String particle = cfg.getString("display.particle", "");
            String openSound = cfg.getString("sounds.open", "").trim();
            List<String> hologramLines = loadHologramLines(id, cfg);
            int maxLines = Math.max(1, plugin.getConfig().getInt("hologram.max-lines", 6));
            if (hologramLines.size() > maxLines) {
                hologramLines = new ArrayList<>(hologramLines.subList(0, maxLines));
            }
            String keyId = cfg.getString("key", "").trim().toLowerCase(Locale.ROOT);
            int cooldown = cfg.getInt("cooldown", 0);
            String permission = cfg.getString("permission", "");

            RollType rollType;
            try {
                rollType = RollType.valueOf(cfg.getString("roll.type", "CSGO").toUpperCase());
            } catch (IllegalArgumentException e) {
                rollType = RollType.CSGO;
            }
            int size = cfg.getInt("roll.size", 27);
            String title = cfg.getString("roll.title", name);
            int durationTicks = cfg.getInt("roll.duration-ticks", 60);
            int tickInterval = cfg.getInt("roll.tick-interval", 2);
            RollDefinition roll = new RollDefinition(rollType, size, title, durationTicks, tickInterval);

            List<RewardDefinition> rewards = new ArrayList<>();
            List<Map<?, ?>> rewardMaps = cfg.getMapList("rewards");
            for (Map<?, ?> map : rewardMaps) {
                Object typeObj = map.containsKey("type") ? map.get("type") : "ITEM";
                RewardType rewardType = RewardType.valueOf(typeObj.toString().toUpperCase());
                Object weightObj = map.containsKey("weight") ? map.get("weight") : 1;
                int weight = weightObj instanceof Number n ? n.intValue() : 1;

                List<String> commands = new ArrayList<>();
                Object cmdObj = map.get("commands");
                if (cmdObj instanceof List<?>) {
                    for (Object c : (List<?>) cmdObj) {
                        commands.add(String.valueOf(c));
                    }
                }

                ItemDefinition itemDef = null;
                ItemStack itemStack = null;

                if (map.containsKey("item")) {
                    YamlConfiguration temp = new YamlConfiguration();
                    if (map.get("item") instanceof Map<?, ?> itemMap) {
                        temp.createSection("item", (Map<?, ?>) itemMap);
                        itemDef = ItemDefinition.fromSection(temp.getConfigurationSection("item"));
                    }
                }
                if (map.containsKey("itemstack")) {
                    YamlConfiguration temp = new YamlConfiguration();
                    temp.set("itemstack", map.get("itemstack"));
                    itemStack = temp.getItemStack("itemstack");
                }
                String previewMaterial = map.containsKey("preview-material")
                        ? String.valueOf(map.get("preview-material"))
                        : null;
                String previewName = map.containsKey("preview-name")
                        ? String.valueOf(map.get("preview-name"))
                        : null;
                List<String> previewLore = new ArrayList<>();
                if (map.containsKey("preview-lore") && map.get("preview-lore") instanceof List<?> loreList) {
                    for (Object line : loreList) {
                        previewLore.add(String.valueOf(line));
                    }
                }

                rewards.add(new RewardDefinition(rewardType, weight, commands, itemDef, itemStack, previewMaterial,
                        previewName, previewLore));
            }

            CrateDefinition crate = new CrateDefinition(id, type, name, lore, keyId, roll, rewards, particle,
                    openSound,
                    hologramLines, cooldown, permission);
            registry.register(crate);
        }

        plugin.getLogger().info("Loaded crates: " + registry.all().size());
    }

    public CrateDefinition getCrate(String id) {
        return registry.get(id);
    }

    public List<CrateDefinition> getCrates() {
        return new ArrayList<>(registry.all());
    }

    public long getRemainingCooldown(Player player, CrateDefinition crate) {
        if (player == null || crate == null || crate.getCooldown() <= 0) {
            return 0L;
        }

        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0L;
        }

        Long lastUse = playerCooldowns.get(crate.getId());
        if (lastUse == null) {
            return 0L;
        }

        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        long remaining = crate.getCooldown() - elapsed;
        return Math.max(remaining, 0L);
    }

    public int getAvailableOpens(Player player, CrateDefinition crate) {
        if (player == null || crate == null) {
            return 0;
        }

        String keyId = crate.getKeyId();
        if (keyId == null || keyId.isBlank() || virtualKeyService == null) {
            return 0;
        }

        return virtualKeyService.getTotalKeys(player, keyId);
    }

    public boolean canOpen(Player player, CrateDefinition crate) {
        if (player == null || crate == null || crate.getRewards().isEmpty()) {
            return false;
        }
        if (crate.getPermission() != null && !crate.getPermission().isBlank() && !player.hasPermission(crate.getPermission())) {
            return false;
        }
        if (getRemainingCooldown(player, crate) > 0) {
            return false;
        }
        String keyId = crate.getKeyId();
        return keyId == null || keyId.isBlank() || getAvailableOpens(player, crate) > 0;
    }

    public void openCrate(Player player, CrateDefinition crate, Block block) {
        if (crate == null) {
            return;
        }

        if (crate.getRewards().isEmpty()) {
            player.sendMessage(Messages.get("crate-no-rewards"));
            if (block != null) {
                knockback(player, block);
            }
            return;
        }

        if (crate.getPermission() != null && !crate.getPermission().isBlank()) {
            if (!player.hasPermission(crate.getPermission())) {
                player.sendMessage(Messages.get("crate-no-permission"));
                return;
            }
        }

        if (crate.getCooldown() > 0) {
            long remaining = getRemainingCooldown(player, crate);
            if (remaining > 0) {
                player.sendMessage(Messages.get("crate-cooldown", "%time%", String.valueOf(remaining)));
                return;
            }
        }

        boolean needsKey = crate.getKeyId() != null && !crate.getKeyId().isEmpty();
        if (needsKey) {
            boolean consumed = false;
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (keyService.isKey(hand, crate.getKeyId())) {
                hand.setAmount(hand.getAmount() - 1);
                consumed = true;
            } else {
                ItemStack[] contents = player.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item != null && keyService.isKey(item, crate.getKeyId()) && keyService.isVirtualKey(item)) {
                        item.setAmount(item.getAmount() - 1);
                        if (item.getAmount() <= 0) {
                            contents[i] = null;
                        }
                        player.getInventory().setContents(contents);
                        consumed = true;
                        break;
                    }
                }
                if (!consumed && virtualKeyService != null
                        && virtualKeyService.takeKeys(player.getUniqueId(), crate.getKeyId(), 1)) {
                    consumed = true;
                }
            }

            if (!consumed) {
                player.sendMessage(Messages.get("crate-no-key", "%key%", crate.getKeyId()));
                player.sendTitle(
                        Text.color(Text.toSmallCaps(Messages.formatRaw("crate-no-key-title"))),
                        Text.color(Text.toSmallCaps(Messages.formatRaw("crate-no-key-subtitle", "%key%", crate.getKeyId()))),
                        10,
                        40,
                        10);

                if (block != null) {
                    knockback(player, block);
                }
                return;
            }
        }

        if (crate.getCooldown() > 0) {
            cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(crate.getId(), System.currentTimeMillis());
        }

        playOpenSound(player, crate);
        RollGuiFactory.open(plugin, player, crate, this, block, blockCrateService);
    }

    public RewardDefinition rollReward(CrateDefinition crate) {
        int total = crate.getRewards().stream().mapToInt(RewardDefinition::getWeight).sum();
        int roll = random.nextInt(Math.max(1, total));
        int cursor = 0;
        for (RewardDefinition reward : crate.getRewards()) {
            cursor += reward.getWeight();
            if (roll < cursor) {
                return reward;
            }
        }
        return crate.getRewards().isEmpty() ? null : crate.getRewards().get(0);
    }

    public void giveReward(Player player, RewardDefinition reward) {
        if (reward == null) {
            return;
        }

        if (reward.getType() == RewardType.COMMAND) {
            for (String cmd : reward.getCommands()) {
                String command = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
            player.sendMessage(Messages.get("reward-received", "%value%", resolveRewardDisplayName(reward)));
            return;
        }

        ItemStack item = null;
        if (reward.getItemStack() != null) {
            item = reward.getItemStack().clone();
        } else if (reward.getItemDefinition() != null) {
            item = itemFactory.createItem(reward.getItemDefinition());
        }
        if (item != null) {
            player.getInventory().addItem(item);
            player.sendMessage(Messages.get("reward-received", "%value%", resolveRewardDisplayName(reward)));
        }
    }

    private String resolveRewardDisplayName(RewardDefinition reward) {
        if (reward == null) {
            return "Reward";
        }
        if (reward.getType() == RewardType.COMMAND) {
            if (reward.getPreviewName() != null && !reward.getPreviewName().isBlank()) {
                return ChatColor.stripColor(Text.color(reward.getPreviewName()));
            }
            return "Command Reward";
        }

        if (reward.getItemStack() != null && reward.getItemStack().hasItemMeta()) {
            if (reward.getItemStack().getItemMeta().hasDisplayName()) {
                return ChatColor.stripColor(reward.getItemStack().getItemMeta().getDisplayName());
            }
        }
        if (reward.getItemDefinition() != null) {
            if (reward.getItemDefinition().getName() != null && !reward.getItemDefinition().getName().isBlank()) {
                return ChatColor.stripColor(Text.color(reward.getItemDefinition().getName()));
            }
            Material material = reward.getItemDefinition().getMaterial();
            if (material != null) {
                return material.name().toLowerCase().replace('_', ' ');
            }
        }
        return "Reward";
    }

    private void knockback(Player player, Block block) {
        BoundingBox box = block.getBoundingBox();
        Vector crateCenter = box.getCenter();
        Vector knockback = player.getLocation().toVector().subtract(crateCenter);
        knockback.setY(0);
        if (knockback.lengthSquared() < 0.0001) {
            knockback = player.getLocation().getDirection().setY(0).multiply(-1);
        }
        if (knockback.lengthSquared() >= 0.0001) {
            knockback.normalize().multiply(1.2);
        } else {
            knockback = new Vector(0, 0, 0);
        }
        knockback.setY(0.65);
        player.setVelocity(knockback);
    }

    private void playOpenSound(Player player, CrateDefinition crate) {
        Sound sound = parseSound(crate.getOpenSound());
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private Sound parseSound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase(Locale.ROOT).replace('.', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<String> getDefaultHologramLines() {
        List<String> defaults = plugin.getConfig().getStringList("hologram.default-lines");
        if (defaults.isEmpty()) {
            return List.of("&e%crate_name%", "&7store: &fyour.store.com");
        }
        return defaults;
    }

    private List<String> loadHologramLines(String crateId, YamlConfiguration crateCfg) {
        File hologramFile = getHologramFile(crateId);
        List<String> lines;

        if (hologramFile.exists()) {
            YamlConfiguration hCfg = YamlConfiguration.loadConfiguration(hologramFile);
            lines = new ArrayList<>(hCfg.getStringList("lines"));
        } else {
            lines = crateCfg.contains("display.hologram-lines")
                    ? new ArrayList<>(crateCfg.getStringList("display.hologram-lines"))
                    : new ArrayList<>(getDefaultHologramLines());
            saveHologramFile(hologramFile, crateId, lines);
        }

        int maxLines = Math.max(1, plugin.getConfig().getInt("hologram.max-lines", 6));
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
            saveHologramFile(hologramFile, crateId, lines);
        }
        return lines;
    }

    private File getHologramFile(String crateId) {
        return new File(paths.getHologramsFolder(), crateId.toLowerCase(Locale.ROOT) + ".yml");
    }

    private void saveHologramFile(File file, String crateId, List<String> lines) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("crate-id", crateId.toLowerCase(Locale.ROOT));
        cfg.set("lines", lines);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save hologram file " + file.getName() + ": " + e.getMessage());
        }
    }
}
