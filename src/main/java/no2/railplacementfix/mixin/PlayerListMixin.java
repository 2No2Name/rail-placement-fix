package no2.railplacementfix.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import no2.railplacementfix.RailPlacementFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void initializePlayerConfig(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (!RailPlacementFix.PLAYERS.containsKey(player.getUUID())) {
            RailPlacementFix.PLAYERS.put(player.getUUID(), RailPlacementFix.ENABLED_BY_DEFAULT);
            RailPlacementFix.saveConfig();
        }
    }
}
