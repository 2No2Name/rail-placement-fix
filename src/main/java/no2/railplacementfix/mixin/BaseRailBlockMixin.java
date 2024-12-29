package no2.railplacementfix.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static no2.railplacementfix.common.RailPlacementHelper.NO_CONNECT_POS;

@Mixin(BaseRailBlock.class)
public abstract class BaseRailBlockMixin extends Block implements SimpleWaterloggedBlock {
    @Shadow @Final public static BooleanProperty WATERLOGGED;

    @Shadow @Final private boolean isStraight;

    public BaseRailBlockMixin(Properties properties) {
        super(properties);
    }

    @Shadow protected static native boolean shouldBeRemoved(BlockPos blockPos, Level level, RailShape railShape);

    @Shadow public abstract Property<RailShape> getShapeProperty();

    @Shadow protected abstract BlockState updateDir(Level level, BlockPos blockPos, BlockState blockState, boolean bl);

    @Inject(method = "getStateForPlacement", at = @At(
            value = "INVOKE_ASSIGN", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/item/context/BlockPlaceContext;getHorizontalDirection()Lnet/minecraft/core/Direction;"
    ), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void getSmartPlacementState(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<BlockState> cir, boolean shouldWaterlog, BlockState defaultRailState, Direction placementDirection) {
        if (blockPlaceContext.getPlayer() != null && blockPlaceContext.getPlayer().isShiftKeyDown()) {
            BlockPos blockPos = blockPlaceContext.getClickedPos();
            Direction clickSide = blockPlaceContext.getClickedFace();

            Vec3 clickLocation = blockPlaceContext.getClickLocation();
            //Ascend rail if placed like a top slab: Clicked on the ceiling or clicked the top half of a block
            boolean shouldAscend = clickSide == Direction.DOWN ||
                    (clickSide != Direction.UP && clickLocation.y - (double) blockPos.getY() > 0.5D);
            //Rails placed on walls are never placed curved, but rails placed on the ceiling and the floor must check for curving
            boolean checkCurve = (clickSide == Direction.UP || clickSide == Direction.DOWN);

            RailShape betterRailShape = null;
            if (shouldAscend) {
                betterRailShape = getAscendingRailShape(blockPlaceContext, placementDirection, clickSide, blockPos);
            } else if (checkCurve && !this.isStraight) {
                betterRailShape = getCurvedRailShape(clickLocation, blockPos);
            }
            if (betterRailShape != null) {
                cir.setReturnValue(defaultRailState.setValue(WATERLOGGED, shouldWaterlog).setValue(this.getShapeProperty(), betterRailShape));
            }
            NO_CONNECT_POS.set(blockPos);
        }
    }

    @Unique
    private static @Nullable RailShape getCurvedRailShape(Vec3 clickLocation, BlockPos blockPos) {
        boolean positiveX = (clickLocation.x - (double) blockPos.getX() > 11D/16D);
        boolean positiveZ = (clickLocation.z - (double) blockPos.getZ() > 11D/16D);
        boolean negativeX = (clickLocation.x - (double) blockPos.getX() < 5D/16D);
        boolean negativeZ = (clickLocation.z - (double) blockPos.getZ() < 5D/16D);
        //SOUTH AND EAST are positive. SOUTH is Z axis, EAST is X
        if (positiveX && positiveZ) {
            return RailShape.SOUTH_EAST;
        } else if (positiveX && negativeZ) {
            return RailShape.NORTH_EAST;
        } else if (negativeX && positiveZ) {
            return RailShape.SOUTH_WEST;
        } else if (negativeX && negativeZ) {
            return RailShape.NORTH_WEST;
        }
        return null;
    }

    @Unique
    private @Nullable RailShape getAscendingRailShape(BlockPlaceContext blockPlaceContext, Direction placementDirection, Direction clickSide, BlockPos blockPos) {
        if (placementDirection.getAxis().isHorizontal() || clickSide.getAxis().isHorizontal()) {
            if (clickSide.getAxis().isHorizontal()) {
                placementDirection = clickSide.getOpposite();
            }
            RailShape slopedRailShape = getSlopedRailShape(placementDirection);
            if (slopedRailShape != null && !shouldBeRemoved(blockPos, blockPlaceContext.getLevel(), slopedRailShape)) {
                return slopedRailShape;
            }
        }
        return null;
    }

    @Unique
    private static @Nullable RailShape getSlopedRailShape(Direction uphillDirection) {
        if (uphillDirection == Direction.NORTH) {
            return RailShape.ASCENDING_NORTH;
        } else if (uphillDirection == Direction.SOUTH) {
            return RailShape.ASCENDING_SOUTH;
        } else if (uphillDirection == Direction.WEST) {
            return RailShape.ASCENDING_WEST;
        } else if (uphillDirection == Direction.EAST) {
            return RailShape.ASCENDING_EAST;
        }
        return null;
    }

    @Redirect(
            method = "updateState(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BaseRailBlock;updateDir(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private BlockState cancelUpdates(BaseRailBlock instance, Level level, BlockPos blockPos, BlockState blockState, boolean bl) {
        BlockPos noUpdatePos = NO_CONNECT_POS.get();

        if (noUpdatePos != null) {
            NO_CONNECT_POS.set(null);
        }

        if (!blockPos.equals(noUpdatePos)) {
            return this.updateDir(level, blockPos, blockState, bl);
        }
        return blockState;
    }
}
