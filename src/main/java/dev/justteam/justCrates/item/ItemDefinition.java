package dev.justteam.justCrates.item;

import dev.justteam.justCrates.core.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class ItemDefinition {

    private final Material material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final Integer customModelData;
    private final String provider;
    private final String providerItem;

    public ItemDefinition(Material material, int amount, String name, List<String> lore, Integer customModelData, String provider, String providerItem) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
        this.provider = provider;
        this.providerItem = providerItem;
    }

    public static ItemDefinition fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        int amount = Math.max(1, section.getInt("amount", 1));
        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }
        Integer cmd = section.contains("custom-model-data") ? section.getInt("custom-model-data") : null;
        String provider = section.getString("provider", "").trim();
        String providerItem = section.getString("provider-item", "").trim();

        return new ItemDefinition(material, amount, name, lore, cmd, provider, providerItem);
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderItem() {
        return providerItem;
    }

    public String getDisplayNameOrDefault() {
        return Text.color(name != null ? name : material.name());
    }
}
