package com.wobble.wobblecombat.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;

public final class Placeholders {

    private Placeholders() {
    }

    public static String apply(Player player, String input, Map<String, String> replacements, boolean usePlaceholderApi) {
        String output = input == null ? "" : input;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            output = output.replace("{" + entry.getKey() + "}", value);
            output = output.replace("%wobblecombat_" + entry.getKey() + "%", value);
        }

        if (player != null) {
            output = output.replace("%wobblecombat_player%", player.getName());
            output = output.replace("%wobblecombat_world%", player.getWorld().getName());
        }

        if (!usePlaceholderApi || player == null) {
            return output;
        }
        return applyPlaceholderApi(player, output);
    }

    private static String applyPlaceholderApi(Player player, String input) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (plugin == null || !plugin.isEnabled()) {
            return input;
        }

        try {
            Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = placeholderApiClass.getMethod("setPlaceholders", Player.class, String.class);
            Object result = method.invoke(null, player, input);
            return result instanceof String string ? string : input;
        } catch (ReflectiveOperationException ignored) {
            return input;
        }
    }
}
