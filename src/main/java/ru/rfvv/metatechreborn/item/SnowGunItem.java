package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Reimplementation of the old Snow Gun with three switchable firing modes. */
public final class SnowGunItem extends Item {
    private static final String MODE_TAG = "SnowGunMode";

    public SnowGunItem() {
        super(new Properties().stacksTo(1).durability(4096));
    }

    public static int getMode(ItemStack stack) {
        return Math.floorMod(stack.getOrCreateTag().getInt(MODE_TAG), 3);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int next = (getMode(gun) + 1) % 3;
                gun.getOrCreateTag().putInt(MODE_TAG, next);
                player.displayClientMessage(Component.translatable(
                        "message.metatech_reborn.snow_gun.mode." + next), true);
            }
            return InteractionResultHolder.sidedSuccess(gun, level.isClientSide);
        }

        int mode = getMode(gun);
        int shots = mode == 0 ? 1 : mode == 1 ? 5 : 3;
        int ammoCost = mode == 0 ? 1 : mode == 1 ? 3 : 2;
        if (!player.getAbilities().instabuild && countAmmo(player) < ammoCost) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(
                    "message.metatech_reborn.snow_gun.no_ammo"), true);
            return InteractionResultHolder.fail(gun);
        }

        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) consumeAmmo(player, ammoCost);
            for (int i = 0; i < shots; i++) {
                Snowball snowball = new Snowball(level, player);
                snowball.setItem(new ItemStack(Items.SNOWBALL));
                float yawOffset = shots == 1 ? 0.0F : (i - (shots - 1) / 2.0F) * (mode == 1 ? 4.0F : 1.5F);
                snowball.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset,
                        0.0F, mode == 2 ? 2.2F : 1.6F, mode == 2 ? 0.2F : 0.8F);
                level.addFreshEntity(snowball);
            }
            gun.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this, mode == 1 ? 14 : 7);
        }
        level.playSound(player, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.8F, mode == 2 ? 1.45F : 1.0F);
        return InteractionResultHolder.sidedSuccess(gun, level.isClientSide);
    }

    private static int countAmmo(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.SNOWBALL)) count += stack.getCount();
        }
        return count;
    }

    private static void consumeAmmo(Player player, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(Items.SNOWBALL)) continue;
            int removed = Math.min(amount, stack.getCount());
            stack.shrink(removed);
            amount -= removed;
            if (amount <= 0) return;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.snow_gun.mode." + getMode(stack))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.snow_gun.toggle")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
