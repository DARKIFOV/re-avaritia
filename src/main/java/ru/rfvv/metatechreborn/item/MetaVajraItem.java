package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** High-capacity MetaAdvanced Vajra with switchable 1x1 and 3x3 mining. */
public final class MetaVajraItem extends PickaxeItem {
    public static final int CAPACITY = 300_000_000;
    public static final int TRANSFER_LIMIT = 7_500_000;
    public static final int ENERGY_PER_BLOCK = 2_500;
    private static final String AREA_MODE_TAG = "AreaMode";

    public MetaVajraItem() {
        super(Tiers.NETHERITE, 6, -2.5F, new Properties().stacksTo(1).fireResistant());
    }

    public static boolean isAreaMode(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().getBoolean(AREA_MODE_TAG);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            boolean area = !isAreaMode(stack);
            stack.getOrCreateTag().putBoolean(AREA_MODE_TAG, area);
            player.displayClientMessage(Component.translatable(area
                    ? "message.metatech_reborn.vajra.area"
                    : "message.metatech_reborn.vajra.single"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return StackEnergyStorage.getStored(stack) >= ENERGY_PER_BLOCK ? 50.0F : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, net.minecraft.core.BlockPos pos,
                             LivingEntity miner) {
        if (!level.isClientSide) StackEnergyStorage.consume(stack, ENERGY_PER_BLOCK);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * StackEnergyStorage.getStored(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x56E7FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.energy",
                StackEnergyStorage.getStored(stack), CAPACITY).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(isAreaMode(stack)
                ? "tooltip.metatech_reborn.vajra.area"
                : "tooltip.metatech_reborn.vajra.single").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.vajra.toggle")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new StackEnergyStorage(stack, CAPACITY, TRANSFER_LIMIT, ENERGY_PER_BLOCK);
    }
}
