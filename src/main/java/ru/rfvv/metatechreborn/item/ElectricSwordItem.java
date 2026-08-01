package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Restores the configurable MetaAdvanced electric swords with active/off modes. */
public final class ElectricSwordItem extends SwordItem {
    private static final String ACTIVE_TAG = "Active";

    private final int capacity;
    private final int transferLimit;
    private final float activeDamage;
    private final int hitCost;
    private final int passiveCost;

    public ElectricSwordItem(int capacity, int transferLimit, float activeDamage, int hitCost, int passiveCost) {
        super(Tiers.NETHERITE, 3, -2.2F, new Properties().stacksTo(1).fireResistant());
        this.capacity = capacity;
        this.transferLimit = transferLimit;
        this.activeDamage = activeDamage;
        this.hitCost = hitCost;
        this.passiveCost = passiveCost;
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().getBoolean(ACTIVE_TAG);
    }

    private static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(ACTIVE_TAG, active);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);

        if (!level.isClientSide) {
            boolean next = !isActive(stack);
            if (next && StackEnergyStorage.getStored(stack) < hitCost) next = false;
            setActive(stack, next);
            player.displayClientMessage(Component.translatable(next
                    ? "message.metatech_reborn.electric_sword.enabled"
                    : "message.metatech_reborn.electric_sword.disabled"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.35F, next ? 1.6F : 0.7F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isActive(stack) && StackEnergyStorage.consume(stack, hitCost)) {
            float vanillaDamage = 8.0F;
            float bonus = Math.max(0.0F, activeDamage - vanillaDamage);
            if (bonus > 0.0F) target.hurt(target.damageSources().magic(), bonus);
        } else if (isActive(stack)) {
            setActive(stack, false);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !selected || !isActive(stack) || level.getGameTime() % 20L != 0L) return;
        if (!StackEnergyStorage.consume(stack, passiveCost)) {
            setActive(stack, false);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable(
                        "message.metatech_reborn.electric_sword.empty"), true);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack) || super.isFoil(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return StackEnergyStorage.getCapacity(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Math.max(1, StackEnergyStorage.getCapacity(stack));
        return Math.round(13.0F * StackEnergyStorage.getStored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x35D9FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.energy",
                StackEnergyStorage.getStored(stack), capacity).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(isActive(stack)
                ? "tooltip.metatech_reborn.electric_sword.active"
                : "tooltip.metatech_reborn.electric_sword.inactive").withStyle(
                isActive(stack) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.electric_sword.toggle")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.electric_sword.damage", (int) activeDamage)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new StackEnergyStorage(stack, capacity, transferLimit, Math.max(hitCost, passiveCost));
    }
}
