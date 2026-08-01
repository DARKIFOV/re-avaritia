package ru.rfvv.metatechreborn.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import ru.rfvv.metatechreborn.registry.ModBlocks;

/**
 * 3x3x3 Mana Drill structure. The controller is the front-centre block of the middle layer.
 * The structure extends two blocks behind the controller.
 */
public final class ManaDrillStructure {
    private ManaDrillStructure() {
    }

    public static boolean isFormed(Level level, BlockPos controller, Direction facing) {
        if (!facing.getAxis().isHorizontal()) return false;
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();

        for (int y = -1; y <= 1; y++) {
            for (int z = 0; z <= 2; z++) {
                for (int x = -1; x <= 1; x++) {
                    BlockPos pos = local(controller, right, back, x, y, z);
                    if (!level.hasChunkAt(pos)) return false;

                    if (x == 0 && y == 0 && z == 0) {
                        if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL.get())) return false;
                        continue;
                    }
                    if (x == 0 && y == 0 && z == 1) {
                        if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL_CORE.get())) return false;
                        continue;
                    }
                    if (x == 0 && y == 1 && z == 1) {
                        if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL_NOZZLE.get())) return false;
                        continue;
                    }
                    if (x == 0 && y == 1 && z == 0) {
                        if (!level.getBlockState(pos).isAir()) return false;
                        continue;
                    }
                    if (!level.getBlockState(pos).is(ModBlocks.MANA_DRILL_CASING.get())) return false;
                }
            }
        }
        return true;
    }

    public static BlockPos local(BlockPos controller, Direction right, Direction back,
                                 int x, int y, int z) {
        return controller.relative(right, x).relative(back, z).above(y);
    }

    public static int requiredCasingCount() {
        return 23;
    }
}
