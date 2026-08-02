package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Upgrades accepted by the six original upgrade slots of both luck converters. */
public final class LuckConverterUpgradeItem extends Item {
    public enum Type {
        SPEED("speed", 8),
        EFFICIENCY("efficiency", 8),
        OPERATIONS("operations", 4),
        DOUBLE("double", 1),
        SMELT("smelt", 1),
        AUTO_EJECT("auto_eject", 1);

        private final String key;
        private final int maximum;

        Type(String key, int maximum) {
            this.key = key;
            this.maximum = maximum;
        }

        public String key() { return key; }
        public int maximum() { return maximum; }
    }

    private final Type type;

    public LuckConverterUpgradeItem(Type type) {
        super(new Item.Properties().stacksTo(type.maximum())
                .rarity(type.maximum() == 1 ? Rarity.RARE : Rarity.UNCOMMON));
        this.type = type;
    }

    public Type type() { return type; }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type.maximum() == 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.luck_upgrade." + type.key())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.luck_upgrade.count",
                        stack.getCount(), type.maximum())
                .withStyle(ChatFormatting.GRAY));
    }
}
