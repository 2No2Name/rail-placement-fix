package no2.railplacementfix.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RailState;
import no2.railplacementfix.RailPlacementFix;
import no2.railplacementfix.common.RailPlacementHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RailState.class)
public abstract class RailStateMixin {

    @Shadow protected abstract boolean connectsTo(RailState railState);

    @Shadow @Final private List<BlockPos> connections;

    @Shadow @Final private boolean isStraight;

    @Shadow public abstract List<BlockPos> getConnections();

    @Shadow @Final private BlockPos pos;


    @Inject(
            method = "canConnectTo",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I"
            ), cancellable = true
    )
    private void canConnectTo(RailState railState, CallbackInfoReturnable<Boolean> cir) {
        BlockPos neighborPos = ((RailStateMixin) (Object) railState).pos;
        if (neighborPos.equals(RailPlacementHelper.PLAYER_PLACED_POS.get()) && this.isStraight) {
            if (this.connections.size() == 1) {
                BlockPos possible_connection = this.pos.offset(this.pos.subtract(this.connections.get(0)));
                if (neighborPos.getX() != (possible_connection.getX()) ||
                        neighborPos.getZ() != (possible_connection.getZ())) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
