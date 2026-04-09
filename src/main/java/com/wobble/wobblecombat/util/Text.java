package com.wobble.wobblecombat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class Text {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static String color(String input) {
        return SERIALIZER.serialize(SERIALIZER.deserialize(input == null ? "" : input));
    }

    public static Component component(String input) {
        return SERIALIZER.deserialize(input == null ? "" : input);
    }

    public static String componentToLegacy(String input) {
        return SERIALIZER.serialize(component(input));
    }

    public static String stripColor(String input) {
        return ChatColor.stripColor(input == null ? "" : input);
    }
}
