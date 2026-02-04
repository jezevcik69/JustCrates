package dev.justteam.justCrates.crate;

import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.gui.RollGui;
import dev.justteam.justCrates.item.ItemDefinition;
import dev.justteam.justCrates.item.ItemFactory;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
import dev.justteam.justCrates.provider.ProviderRegistry;
import dev.justteam.justCrates.reward.RewardDefinition;
import dev.justteam.justCrates.reward.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CrateService {

    private final JavaPlugin plugin;
    private final ProviderRegistry providerRegistry;
    private final PluginPaths paths;
    private final KeyService keyService;
    private final VirtualKeyService virtualKeyService;
    private final CrateRegistry registry;
    private final ItemFactory itemFactory;
    private final Random random;

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
            String keyId = cfg.getString("key", "").trim();

            int size = cfg.getInt("roll.size", 27);
            String title = cfg.getString("roll.title", name);
            int durationTicks = cfg.getInt("roll.duration-ticks", 60);
            int tickInterval = cfg.getInt("roll.tick-interval", 2);
            RollDefinition roll = new RollDefinition(size, title, durationTicks, tickInterval);

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

                rewards.add(new RewardDefinition(rewardType, weight, commands, itemDef, itemStack));
            }

            CrateDefinition crate = new CrateDefinition(id, type, name, lore, keyId, roll, rewards);
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

    public void openCrate(Player player, CrateDefinition crate) {
        if (crate == null) {
            return;
        }
        if (!crate.getKeyId().isEmpty()) {
            boolean consumed = false;
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (keyService.isKey(hand, crate.getKeyId())) {
                hand.setAmount(hand.getAmount() - 1);
                consumed = true;
            } else if (virtualKeyService != null && virtualKeyService.takeKeys(player.getUniqueId(), crate.getKeyId(), 1)) {
                consumed = true;
            }

            if (!consumed) {
                String keyId = crate.getKeyId();
                player.sendMessage(Text.color("&cYou don't have the required key: &f" + keyId));
                player.sendTitle(
                    Text.color("&cYou don't have a key"),
                    Text.color("&7Required: &f" + keyId),
                    10,
                    40,
                    10
                );
                return;
            }
        }

        RollGui.open(plugin, player, crate, this);
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
        }
    }
}
