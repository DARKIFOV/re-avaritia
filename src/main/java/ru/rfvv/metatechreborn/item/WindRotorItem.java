package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Config-derived MetaAdvanced wind rotor. Machine integration can consume its durability later. */
public final class WindRotorItem extends Item {
    private final int radius;
    private final float efficiency;
    private final double minWindStrength;
    private final double maxWindStrength;

    public WindRotorItem(int radius, int durability, float efficiency,
                         double minWindStrength, double maxWindStrength) {
        super(new Properties().stacksTo(1).durability(Math.max(1, durability)));
        this.radius = Math.max(1, radius);
        this.efficiency = Math.max(0.0F, efficiency);
        this.minWindStrength = Math.max(0.0D, minWindStrength);
        this.maxWindStrength = Math.max(this.minWindStrength, maxWindStrength);
    }

    public int radius() {
        return radius;
    }

    public float efficiency() {
        return efficiency;
    }

    public double minWindStrength() {
        return minWindStrength;
    }

    public double maxWindStrength() {
        return maxWindStrength;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.wind_rotor.radius", radius)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.wind_rotor.min", minWindStrength)
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.wind_rotor.max", maxWindStrength)
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.wind_rotor.efficiency", efficiency)
                .withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
