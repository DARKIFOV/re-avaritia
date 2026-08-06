package ru.rfvv.metatechreborn.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.rfvv.metatechreborn.block.ManaDrillBlock;
import ru.rfvv.metatechreborn.block.ManaDrillNozzleBlock;
import ru.rfvv.metatechreborn.block.ManaDrillPartBlock;
import ru.rfvv.metatechreborn.blockentity.ManaDrillBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;

import java.util.Optional;

/**
 * 3x3x3 Mana Drill structure. The controller is the centre block of one outside wall.
 * Older test builds described the extension direction inconsistently, so both the
 * direction behind the controller face and the mirrored legacy direction are accepted.
 */
public final class ManaDrillStructure {
    private static final int CONTROLLER_SCAN_RADIUS = 2;

    private ManaDrillStructure() {}

    public record Match(Direction depth, boolean reversed) {}

    public static Optional<Match> findMatch(Level level, BlockPos controller, Direction facing) {
        if (!facing.getAxis().isHorizontal()) return Optional.empty();
        Direction right = facing.getClockWise();
        Direction normalDepth = facing.getOpposite();
        if (isFormedInDirection(level, controller, right, normalDepth)) {
            return Optional.of(new Match(normalDepth, false));
        }
        if (isFormedInDirection(level, controller, right, facing)) {
            return Optional.of(new Match(facing, true));
        }
        return Optional.empty();
    }

    public static boolean isFormed(Level level, BlockPos controller, Direction facing) {
        return findMatch(level, controller, facing).isPresent();
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

    /**
     * Applies the visual assembled state after the controller has completed its normal
     * gameplay structure check. All physical blocks stay in the world and retain their
     * collision; only their baked models are hidden while the controller renderer draws
     * the complete machine.
     */
    public static void syncVisualState(Level level, BlockPos controller, Direction facing,
                                       boolean structureFormed) {
        if (level.isClientSide) return;
        BlockState controllerState = level.getBlockState(controller);
        if (!controllerState.is(ModBlocks.MANA_DRILL.get())) return;

        Direction previousDepth = null;
        if (controllerState.hasProperty(ManaDrillBlock.FORMED)
                && controllerState.getValue(ManaDrillBlock.FORMED)) {
            boolean reversed = controllerState.getValue(ManaDrillBlock.REVERSED);
            previousDepth = reversed ? facing : facing.getOpposite();
        }

        Optional<Match> match = structureFormed
                ? findMatch(level, controller, facing)
                : Optional.empty();

        if (previousDepth != null
                && (match.isEmpty() || match.get().depth() != previousDepth)) {
            applyVisualState(level, controller, facing, previousDepth, false, false);
        }

        if (match.isPresent()) {
            Match active = match.get();
            applyVisualState(level, controller, facing, active.depth(), true, active.reversed());
        } else {
            BlockState current = level.getBlockState(controller);
            if (current.is(ModBlocks.MANA_DRILL.get())) {
                BlockState updated = current
                        .setValue(ManaDrillBlock.FORMED, false)
                        .setValue(ManaDrillBlock.REVERSED, false);
                if (!updated.equals(current)) {
                    level.setBlock(controller, updated, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    public static void clearVisualState(Level level, BlockPos controller,
                                        Direction facing, boolean reversed) {
        if (level.isClientSide) return;
        Direction depth = reversed ? facing : facing.getOpposite();
        applyVisualState(level, controller, facing, depth, false, false);
    }

    private static void applyVisualState(Level level, BlockPos controller, Direction facing,
                                         Direction depth, boolean formed, boolean reversed) {
        Direction right = facing.getClockWise();
        Direction visualFront = depth.getOpposite();

        for (int y = -1; y <= 1; y++) {
            for (int z = 0; z <= 2; z++) {
                for (int x = -1; x <= 1; x++) {
                    if (x == 0 && y == 1 && z == 0) continue;
                    BlockPos pos = local(controller, right, depth, x, y, z);
                    if (!level.hasChunkAt(pos)) continue;

                    BlockState state = level.getBlockState(pos);
                    BlockState updated = state;
                    if (state.is(ModBlocks.MANA_DRILL.get())) {
                        updated = state
                                .setValue(ManaDrillBlock.FORMED, formed)
                                .setValue(ManaDrillBlock.REVERSED, formed && reversed);
                    } else if (state.is(ModBlocks.MANA_DRILL_CASING.get())
                            || state.is(ModBlocks.MANA_DRILL_CORE.get())) {
                        if (state.hasProperty(ManaDrillPartBlock.FORMED)) {
                            updated = state.setValue(ManaDrillPartBlock.FORMED, formed);
                        }
                    } else if (state.is(ModBlocks.MANA_DRILL_NOZZLE.get())) {
                        updated = state.setValue(ManaDrillNozzleBlock.FORMED, formed);
                        if (formed) {
                            updated = updated.setValue(ManaDrillNozzleBlock.FACING, visualFront);
                        }
                    }

                    if (!updated.equals(state)) {
                        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Requests an immediate controller re-check after a casing/core/nozzle change. */
    public static void notifyControllers(Level level, BlockPos changedPos) {
        if (level.isClientSide) return;
        BlockPos immutable = changedPos.immutable();
        if (level.getServer() != null) {
            level.getServer().execute(() -> notifyControllersNow(level, immutable));
        } else {
            notifyControllersNow(level, immutable);
        }
    }

    private static void notifyControllersNow(Level level, BlockPos changedPos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -CONTROLLER_SCAN_RADIUS; x <= CONTROLLER_SCAN_RADIUS; x++) {
            for (int y = -CONTROLLER_SCAN_RADIUS; y <= CONTROLLER_SCAN_RADIUS; y++) {
                for (int z = -CONTROLLER_SCAN_RADIUS; z <= CONTROLLER_SCAN_RADIUS; z++) {
                    cursor.set(changedPos.getX() + x, changedPos.getY() + y, changedPos.getZ() + z);
                    if (!level.hasChunkAt(cursor)) continue;
                    BlockEntity candidate = level.getBlockEntity(cursor);
                    if (!(candidate instanceof ManaDrillBlockEntity drill)) continue;

                    drill.forceStructureCheck();
                    BlockState controllerState = level.getBlockState(cursor);
                    Direction facing = controllerState.hasProperty(ManaDrillBlock.FACING)
                            ? controllerState.getValue(ManaDrillBlock.FACING)
                            : Direction.NORTH;
                    syncVisualState(level, cursor.immutable(), facing, drill.isStructureFormed());
                }
            }
        }
    }

    public static BlockPos local(BlockPos controller, Direction right, Direction depth,
                                 int x, int y, int z) {
        return controller.relative(right, x).relative(depth, z).above(y);
    }

    public static int requiredCasingCount() {
        return 23;
    }
}
