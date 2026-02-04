package dev.justteam.justCrates.core;

import net.md_5.bungee.api.ChatColor;

public final class Text {

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
