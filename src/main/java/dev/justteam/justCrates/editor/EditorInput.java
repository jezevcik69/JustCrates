package dev.justteam.justCrates.editor;

public final class EditorInput {

    private final EditorInputType type;
    private final String crateId;
    private final Integer rewardIndex;

    public EditorInput(EditorInputType type, String crateId, Integer rewardIndex) {
        this.type = type;
        this.crateId = crateId;
        this.rewardIndex = rewardIndex;
    }

    public EditorInputType getType() {
        return type;
    }

    public String getCrateId() {
        return crateId;
    }

    public Integer getRewardIndex() {
        return rewardIndex;
    }
}
