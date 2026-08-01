package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** MetaThaumcraft Skull Axe: a sword-like axe that harvests mob heads. */
public final class SkullAxeItem extends SwordItem {
    public SkullAxeItem() {
        super(Tiers.NETHERITE, 8, -2.7F, new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!target.level().isClientSide && target.isDeadOrDying() && target.getRandom().nextFloat() < 0.22F) {
            ItemStack skull = skullFor(target);
            if (!skull.isEmpty()) target.spawnAtLocation(skull);
        }
        return result;
    }

    private static ItemStack skullFor(LivingEntity target) {
        if (target instanceof WitherSkeleton) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (target instanceof Skeleton) return new ItemStack(Items.SKELETON_SKULL);
        if (target instanceof Zombie) return new ItemStack(Items.ZOMBIE_HEAD);
        if (target instanceof Creeper) return new ItemStack(Items.CREEPER_HEAD);
        if (target instanceof Piglin) return new ItemStack(Items.PIGLIN_HEAD);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.skull_axe")
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
