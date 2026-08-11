package no2.railplacementfix;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class RailPlacementFix implements ModInitializer {
	public static final String MOD_ID = "rail-placement-fix";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static boolean EXPERIMENTAL_FEATURES = true;

    public static boolean ENABLED_BY_DEFAULT = true;
    public static final Object2BooleanMap<UUID> PLAYERS = new Object2BooleanOpenHashMap<>();

    @Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        loadConfig();
    }

    public static void saveConfig() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("railplacementfix/");
        try {
            Files.createDirectories(dir);

            StringBuilder buf = new StringBuilder(10 + PLAYERS.size() * 39);
            buf.append(ENABLED_BY_DEFAULT ? "default y\n" : "default n\n");

            var iter = Object2BooleanMaps.fastIterator(PLAYERS);
            while (iter.hasNext()) {
                var entry = iter.next();
                buf.append(entry.getKey());
                buf.append(entry.getBooleanValue() ? " y\n" : " n\n");
            }

            Path path = dir.resolve("config.txt");
            Files.writeString(path, buf.toString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Could not save enabled players", e);
        }

        LOGGER.info("Wrote config with states of {} players", PLAYERS.size());
    }

    private static void loadConfig() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("railplacementfix/");
        try {
            Files.createDirectories(dir);

            Path path = dir.resolve("config.txt");
            List<String> lines = Files.readAllLines(path);

            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                String line = lines.get(lineIdx);
                String[] split = line.split(" ");
                if (split.length != 2) {
                    LOGGER.error("Expected format `<key> [y|n]` at line {} of {}:  but got {}", lineIdx + 1, path, line);
                    continue;
                }

                String enabledStr = split[1];
                boolean enabled;
                if (enabledStr.equalsIgnoreCase("y") || enabledStr.equalsIgnoreCase("yes") || enabledStr.equalsIgnoreCase("true")) {
                    enabled = true;
                } else if (enabledStr.equalsIgnoreCase("n") || enabledStr.equalsIgnoreCase("no") || enabledStr.equalsIgnoreCase("false")) {
                    enabled = false;
                } else {
                    LOGGER.error("Expected boolean at line {} of {} but got {}", lineIdx + 1, path, enabledStr);
                    continue;
                }

                String key = split[0];
                if (key.equals("default")) {
                    ENABLED_BY_DEFAULT = enabled;
                    continue;
                }

                try {
                    UUID uuid = UUID.fromString(key);
                    PLAYERS.put(uuid, enabled);
                } catch (Exception e) {
                    LOGGER.error("Malformed UUID {} at line {} of {}: {}", key, lineIdx + 1, path, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not load enabled players", e);
        }

        LOGGER.info("Loaded {} UUIDs of players with Rail Placement Fix enabled", PLAYERS.size());
    }

    public static boolean isEnabledFor(Player player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), ignored -> ENABLED_BY_DEFAULT);
    }
}