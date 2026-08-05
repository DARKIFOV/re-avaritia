package ru.rfvv.metatechreborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;

public final class ExtremePatternEncoderBlock extends BaseEntityBlock {
    public ExtremePatternEncoderBlock(Properties properties) {
        super(properties);
    }

    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                          @NotNull BlockPos pos, @NotNull Player player,
                                          @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ExtremePatternEncoderBlockEntity encoder) {
                NetworkHooks.openScreen(serverPlayer, encoder, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ExtremePatternEncoderBlockEntity encoder) {
                for (var stack : encoder.getDrops()) {
                    Containers.dropItemStack(level,
                            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ExtremePatternEncoderBlockEntity(pos, state);
    }

    @Override
    public boolean isPathfindable(@NotNull BlockState state, @NotNull BlockGetter level,
                                  @NotNull BlockPos pos,
                                  @NotNull net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
