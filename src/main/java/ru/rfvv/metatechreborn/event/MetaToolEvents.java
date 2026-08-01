package ru.rfvv.metatechreborn.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.item.MetaVajraItem;
import ru.rfvv.metatechreborn.item.StackEnergyStorage;

@Mod.EventBusSubscriber(modid = MetaTechReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MetaToolEvents {
    private static final ThreadLocal<Boolean> BREAKING_AREA = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (BREAKING_AREA.get() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof MetaVajraItem) || !MetaVajraItem.isAreaMode(tool)) return;
        if (StackEnergyStorage.getStored(tool) < MetaVajraItem.ENERGY_PER_BLOCK) return;

        Direction hitDirection = Direction.getNearest(
                (float) player.getLookAngle().x,
                (float) player.getLookAngle().y,
                (float) player.getLookAngle().z);
        BlockPos origin = event.getPos();

        BREAKING_AREA.set(true);
        try {
            for (int first = -1; first <= 1; first++) {
                for (int second = -1; second <= 1; second++) {
                    if (first == 0 && second == 0) continue;
                    BlockPos target = switch (hitDirection.getAxis()) {
                        case Y -> origin.offset(first, 0, second);
                        case X -> origin.offset(0, first, second);
                        case Z -> origin.offset(first, second, 0);
                    };
                    BlockState state = player.level().getBlockState(target);
                    if (state.isAir() || state.getDestroySpeed(player.level(), target) < 0.0F) continue;
                    if (!tool.isCorrectToolForDrops(state)) continue;
                    if (StackEnergyStorage.getStored(tool) < MetaVajraItem.ENERGY_PER_BLOCK) return;
                    if (player.gameMode.destroyBlock(target)) {
                        StackEnergyStorage.consume(tool, MetaVajraItem.ENERGY_PER_BLOCK);
                    }
                }
            }
        } finally {
            BREAKING_AREA.set(false);
        }
    }

    private MetaToolEvents() {}
}
