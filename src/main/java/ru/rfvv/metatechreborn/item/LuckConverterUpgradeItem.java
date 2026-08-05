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

/** Upgrades accepted by the six upgrade slots of both luck converters. */
public final class LuckConverterUpgradeItem extends Item {
    public enum Type {
        SPEED_30("speed_30", 1, 30),
        SPEED_70("speed_70", 1, 70),
        SPEED_INSTANT("speed_instant", 1, 100),
        EFFICIENCY("efficiency", 8, 0),
        OPERATIONS("operations", 4, 0),
        DOUBLE("double", 1, 0),
        SMELT("smelt", 1, 0),
        AUTO_EJECT("auto_eject", 1, 0);

        private final String key;
        private final int maximum;
        private final int speedBonusPercent;

        Type(String key, int maximum, int speedBonusPercent) {
            this.key = key;
            this.maximum = maximum;
            this.speedBonusPercent = speedBonusPercent;
        }

        public String key() { return key; }
        public int maximum() { return maximum; }
        public int speedBonusPercent() { return speedBonusPercent; }
        public boolean isSpeedUpgrade() { return speedBonusPercent > 0; }
        public boolean isInstant() { return this == SPEED_INSTANT; }
    }

    private final Type type;

    public LuckConverterUpgradeItem(Type type) {
        super(new Item.Properties().stacksTo(type.maximum())
                .rarity(type.isInstant() ? Rarity.EPIC : type.maximum() == 1 ? Rarity.RARE : Rarity.UNCOMMON));
        this.type = type;
    }

    public Type type() { return type; }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type.maximum() == 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.luck_upgrade." + type.key())
                .withStyle(type.isInstant() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
        if (type.isSpeedUpgrade()) {
            tooltip.add(Component.translatable("tooltip.metatech_reborn.luck_upgrade.speed_priority")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.metatech_reborn.luck_upgrade.count",
                            stack.getCount(), type.maximum())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
