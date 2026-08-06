package ru.rfvv.metatechreborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.multiblock.ManaDrillStructure;

/**
 * Casing/core block used by the Mana Drill multiblock.
 *
 * <p>The block remains physically present while the structure is assembled, but its
 * normal baked model is hidden through the {@link #FORMED} block-state property. The
 * controller's block-entity renderer then draws the complete 3x3x3 machine as one
 * visual object.</p>
 */
public final class ManaDrillPartBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ManaDrillPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                        @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!oldState.is(state.getBlock())) {
            ManaDrillStructure.notifyControllers(level, pos);
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            ManaDrillStructure.notifyControllers(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
