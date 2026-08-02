package no2.railplacementfix;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RailPlacementFix implements ModInitializer {
	public static final String MOD_ID = "rail-placement-fix";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static boolean EXPERIMENTAL_FEATURES = true;

    private static final Set<UUID> ENABLED_PLAYERS = new ObjectOpenHashSet<>();

    @Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        loadEnabled();

        CommandRegistrationCallback.EVENT.register((dispatcher, r, e) -> {
            dispatcher.register(Commands.literal("rpf").then(Commands.literal("toggle").executes((c) -> {
                Player player = c.getSource().getPlayer();
                if (player == null) {
                    c.getSource().sendFailure(Component.literal("Must be a player to run this command."));
                    return 0;
                }

                UUID uuid = player.getUUID();
                if (ENABLED_PLAYERS.remove(uuid)) {
                    c.getSource().sendSystemMessage(Component.literal("Disabled Rail Placement Fix."));
                } else {
                    ENABLED_PLAYERS.add(uuid);
                    c.getSource().sendSystemMessage(Component.literal("Enabled Rail Placement Fix."));
                }

                saveEnabled();

                return 1;
            })));
        });
    }

    private static void saveEnabled() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("railplacementfix/");
        try {
            Files.createDirectories(dir);

            ByteBuffer buf = ByteBuffer.allocate(ENABLED_PLAYERS.size() * 37);
            for (UUID uuid : ENABLED_PLAYERS) {
                buf.put(uuid.toString().getBytes(StandardCharsets.UTF_8));
                buf.put((byte) '\n');
            }

            Path path = dir.resolve("players_enabled.txt");
            Files.write(path, buf.array(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Could not save enabled players", e);
        }

        LOGGER.info("Wrote {} UUIDs of players with Rail Placement Fix enabled", ENABLED_PLAYERS.size());
    }

    private static void loadEnabled() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("railplacementfix/");
        try {
            Files.createDirectories(dir);

            Path path = dir.resolve("players_enabled.txt");
            List<String> lines = Files.readAllLines(path);

            for (int line = 0; line < lines.size(); line++) {
                var uuidStr = lines.get(line);
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ENABLED_PLAYERS.add(uuid);
                } catch (Exception e) {
                    LOGGER.error("Malformed UUID {} at line {} of {}: {}", uuidStr, line + 1, path, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not load enabled players", e);
        }

        LOGGER.info("Loaded {} UUIDs of players with Rail Placement Fix enabled", ENABLED_PLAYERS.size());
    }

    public static boolean isEnabledFor(Player player) {
        return ENABLED_PLAYERS.contains(player.getUUID());
    }
}