package no2.railplacementfix.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.world.entity.player.Player;
import no2.railplacementfix.RailPlacementFix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Function;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;setConsumer(Lcom/mojang/brigadier/ResultConsumer;)V"), method = "<init>")
    private void addCommands(Commands.CommandSelection selection, CommandBuildContext buildContext, CallbackInfo ci) {
        Function<String, LiteralArgumentBuilder<CommandSourceStack>> cmd = (s) -> Commands.literal(s).then(Commands.literal("toggle").then(Commands.literal("self").executes((c) -> {
            Player player = c.getSource().getPlayer();
            if (player == null) {
                c.getSource().sendFailure(Component.literal("Must be a player to run this command."));
                return 0;
            }

            UUID uuid = player.getUUID();
            if (RailPlacementFix.PLAYERS.computeBoolean(uuid, (ignored, prev) -> prev == null ? !RailPlacementFix.ENABLED_BY_DEFAULT : !prev)) {
                c.getSource().sendSystemMessage(Component.literal("Enabled Rail Placement Fix for yourself."));
            } else {
                c.getSource().sendSystemMessage(Component.literal("Disabled Rail Placement Fix for yourself."));
            }

            RailPlacementFix.saveConfig();

            return 1;
        })).then(Commands.literal("default").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes((c) -> {
            RailPlacementFix.ENABLED_BY_DEFAULT = !RailPlacementFix.ENABLED_BY_DEFAULT;
            if (RailPlacementFix.ENABLED_BY_DEFAULT) {
                c.getSource().sendSystemMessage(Component.literal("Enabled Rail Placement Fix by default."));
            } else {
                c.getSource().sendSystemMessage(Component.literal("Disabled Rail Placement Fix by default."));
            }

            RailPlacementFix.saveConfig();

            return 1;
        })));

        dispatcher.register(cmd.apply("railplacementfix"));
    }
}

