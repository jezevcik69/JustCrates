package dev.justteam.justCrates.core;

import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final String PREFIX = "&#4498DB&lᴊ&#52A0DD&lᴜ&#61A8E0&lꜱ&#6FAFE2&lᴛ&#7EB7E5&lᴄ&#8CBFE7&lʀ&#9BC7EA&lᴀ&#A9CEEC&lᴛ&#B8D6EF&lᴇ&#C6DEF1&lꜱ";
    private static final Map<Character, String> SMALL_CAPS = createSmallCapsMap();

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return null;
        }
        String withHex = applyHexColors(input);
        return ChatColor.translateAlternateColorCodes('&', withHex);
    }

    public static String chat(String input) {
        String content = input == null ? "" : toSmallCaps(input);
        return color(PREFIX + " &8» &f" + content);
    }

    public static String toSmallCaps(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '&') {
                if (i + 7 < input.length() && input.charAt(i + 1) == '#') {
                    boolean hex = true;
                    for (int j = i + 2; j <= i + 7; j++) {
                        if (!isHexChar(input.charAt(j))) {
                            hex = false;
                            break;
                        }
                    }
                    if (hex) {
                        out.append(input, i, i + 8);
                        i += 7;
                        continue;
                    }
                }

                if (i + 1 < input.length()) {
                    out.append(c).append(input.charAt(i + 1));
                    i++;
                    continue;
                }
            }

            char lower = Character.toLowerCase(c);
            String mapped = SMALL_CAPS.get(lower);
            out.append(mapped != null ? mapped : c);
        }
        return out.toString();
    }

    private static String applyHexColors(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = "#" + matcher.group(1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(ChatColor.of(hex).toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9')
            || (c >= 'a' && c <= 'f')
            || (c >= 'A' && c <= 'F');
    }

    private static Map<Character, String> createSmallCapsMap() {
        Map<Character, String> map = new HashMap<>();
        map.put('a', "ᴀ");
        map.put('b', "ʙ");
        map.put('c', "ᴄ");
        map.put('d', "ᴅ");
        map.put('e', "ᴇ");
        map.put('f', "ꜰ");
        map.put('g', "ɢ");
        map.put('h', "ʜ");
        map.put('i', "ɪ");
        map.put('j', "ᴊ");
        map.put('k', "ᴋ");
        map.put('l', "ʟ");
        map.put('m', "ᴍ");
        map.put('n', "ɴ");
        map.put('o', "ᴏ");
        map.put('p', "ᴘ");
        map.put('q', "ǫ");
        map.put('r', "ʀ");
        map.put('s', "ꜱ");
        map.put('t', "ᴛ");
        map.put('u', "ᴜ");
        map.put('v', "ᴠ");
        map.put('w', "ᴡ");
        map.put('x', "x");
        map.put('y', "ʏ");
        map.put('z', "ᴢ");
        return map;
    }
}

