package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Expands the assembler pattern bank from 9 to 36 slots. */
public final class PatternCapacityUpgradeItem extends Item {
    public static final int EXTRA_SLOTS = 27;

    public PatternCapacityUpgradeItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.pattern_capacity_upgrade",
                        EXTRA_SLOTS, 36)
                .withStyle(ChatFormatting.AQUA));
    }
}
