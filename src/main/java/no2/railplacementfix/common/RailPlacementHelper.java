package no2.railplacementfix.common;

import net.minecraft.core.BlockPos;

public class RailPlacementHelper {
    public static final ThreadLocal<BlockPos> NO_CONNECT_POS = new ThreadLocal<>();
    public static final ThreadLocal<BlockPos> PLAYER_PLACED_POS = new ThreadLocal<>();
}
