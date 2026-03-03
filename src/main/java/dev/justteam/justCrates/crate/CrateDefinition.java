package dev.justteam.justCrates.crate;

import dev.justteam.justCrates.reward.RewardDefinition;

import java.util.List;

public final class CrateDefinition {
    private final String id;
    private final CrateType type;
    private final String name;
    private final List<String> lore;
    private final String keyId;
    private final RollDefinition rollDefinition;
    private final List<RewardDefinition> rewards;
    private final String particle;
    private final List<String> hologramLines;
    private final int cooldown;
    private final String permission;

    public CrateDefinition(String id, CrateType type, String name, List<String> lore, String keyId, RollDefinition rollDefinition, List<RewardDefinition> rewards, String particle, List<String> hologramLines, int cooldown, String permission) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.lore = lore;
        this.keyId = keyId;
        this.rollDefinition = rollDefinition;
        this.rewards = rewards;
        this.particle = particle;
        this.hologramLines = hologramLines;
        this.cooldown = cooldown;
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public CrateType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getKeyId() {
        return keyId;
    }

    public RollDefinition getRollDefinition() {
        return rollDefinition;
    }

    public List<RewardDefinition> getRewards() {
        return rewards;
    }

    public String getParticle() {
        return particle;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getPermission() {
        return permission;
    }
}
