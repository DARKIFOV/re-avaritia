package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Rechargeable food from MetaAdvanced. The item is not consumed: finishing the
 * eating animation spends FE stored in the stack and restores hunger instead.
 */
public final class EnergyFoodItem extends Item {
    private final int nutrition;
    private final float saturation;
    private final int useDuration;
    private final int energyCost;
    private final int capacity;
    private final int transferLimit;
    private final int tier;

    public EnergyFoodItem(int nutrition, float saturation, int useDuration, int energyCost,
                          int capacity, int transferLimit, int tier) {
        super(new Properties().stacksTo(1));
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.useDuration = Math.max(0, useDuration);
        this.energyCost = Math.max(0, energyCost);
        this.capacity = Math.max(1, capacity);
        this.transferLimit = Math.max(1, transferLimit);
        this.tier = Math.max(1, tier);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.canEat(false)) return InteractionResultHolder.pass(stack);

        if (StackEnergyStorage.getStored(stack) < energyCost) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.metatech_reborn.energy_food.empty"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (useDuration <= 1) {
            if (!level.isClientSide) feed(player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof Player player && player.canEat(false)) {
            feed(player, stack);
        }
        return stack;
    }

    private void feed(Player player, ItemStack stack) {
        if (!StackEnergyStorage.consume(stack, energyCost)) return;
        player.getFoodData().eat(nutrition, saturation);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Math.max(1, useDuration);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * StackEnergyStorage.getStored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x67E8FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.energy",
                StackEnergyStorage.getStored(stack), capacity).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.energy_food.tier", tier)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.energy_food.cost", energyCost)
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.energy_food.restore", nutrition, saturation)
                .withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new StackEnergyStorage(stack, capacity, transferLimit, 0);
    }
}
