package dev.justteam.justCrates.crate;

public final class RollDefinition {

    private final int size;
    private final String title;
    private final int durationTicks;
    private final int tickInterval;

    public RollDefinition(int size, String title, int durationTicks, int tickInterval) {
        this.size = size;
        this.title = title;
        this.durationTicks = durationTicks;
        this.tickInterval = tickInterval;
    }

    public int getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getTickInterval() {
        return tickInterval;
    }
}
