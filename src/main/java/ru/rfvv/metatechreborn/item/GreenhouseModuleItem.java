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

/** Functional module installed into one of the three greenhouse module slots. */
public final class GreenhouseModuleItem extends Item {
    public enum Type {
        ECONOMY("economy", 3),
        EFFICIENCY("efficiency", 3),
        SPEED("speed", 3),
        INFINITE_DAY("infinite_day", 1),
        INFINITE_NIGHT("infinite_night", 1),
        INFINITE_LAVA("infinite_lava", 1);

        private final String translationKey;
        private final int maximum;

        Type(String translationKey, int maximum) {
            this.translationKey = translationKey;
            this.maximum = maximum;
        }

        public String translationKey() { return translationKey; }
        public int maximum() { return maximum; }
        public boolean isSpecial() { return maximum == 1; }
    }

    private final Type type;
    private final int level;

    public GreenhouseModuleItem(Type type, int level) {
        super(new Properties().stacksTo(1).rarity(type.isSpecial() ? Rarity.EPIC
                : level >= type.maximum() ? Rarity.RARE : Rarity.UNCOMMON));
        this.type = type;
        this.level = Math.max(1, Math.min(type.maximum(), level));
    }

    public Type type() { return type; }
    public int level() { return level; }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type.isSpecial() || level >= type.maximum();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!type.isSpecial()) {
            tooltip.add(Component.translatable("tooltip.metatech_reborn.greenhouse_module.level",
                            this.level, this.type.maximum())
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.metatech_reborn.greenhouse_module." + type.translationKey(),
                        this.level)
                .withStyle(type.isSpecial() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.greenhouse_module.slot_limit")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
