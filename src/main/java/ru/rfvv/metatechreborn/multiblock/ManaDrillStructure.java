package ru.rfvv.metatechreborn.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import ru.rfvv.metatechreborn.registry.ModBlocks;

/**
 * 3x3x3 Mana Drill structure. The controller is the centre block of one outside wall.
 * Older test builds described the extension direction inconsistently, so both the
 * direction behind the controller face and the mirrored legacy direction are accepted.
 */
public final class ManaDrillStructure {
    private ManaDrillStructure() {}

    public static boolean isFormed(Level level, BlockPos controller, Direction facing) {
        if (!facing.getAxis().isHorizontal()) return false;
        Direction right = facing.getClockWise();
        return isFormedInDirection(level, controller, right, facing.getOpposite())
                || isFormedInDirection(level, controller, right, facing);
    }

    private static boolean isFormedInDirection(Level level, BlockPos controller,
                                               Direction right, Direction depth) {
        for (int y = -1; y <= 1; y++) {
            for (int z = 0; z <= 2; z++) {
                for (int x = -1; x <= 1; x++) {
                    BlockPos pos = local(controller, right, depth, x, y, z);
                    if (!level.hasChunkAt(pos)) return false;

                    if (x == 0 && y == 0 && z == 0) {
                        if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL.get())) return false;
                    } else if (x == 0 && y == 0 && z == 1) {
                        if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL_CORE.get())) return false;
                    } else if (x == 0 && y == 1 && z == 1) {
                        if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL_NOZZLE.get())) return false;
                    } else if (x == 0 && y == 1 && z == 0) {
                        if (!level.getBlockState(pos).isAir()) return false;
                    } else if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL_CASING.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static BlockPos local(BlockPos controller, Direction right, Direction depth,
                                 int x, int y, int z) {
        return controller.relative(right, x).relative(depth, z).above(y);
    }

    public static int requiredCasingCount() {
        return 23;
    }
}
