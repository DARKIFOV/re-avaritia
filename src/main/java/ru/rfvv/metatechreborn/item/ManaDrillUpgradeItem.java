package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A single, non-stackable tiered upgrade for the Mana Drill. */
public final class ManaDrillUpgradeItem extends Item {
    public enum Type {
        SPEED("speed", 5),
        LOOTING("looting", 9),
        GENERATION("generation", 3);

        private final String translationKey;
        private final int maximum;

        Type(String translationKey, int maximum) {
            this.translationKey = translationKey;
            this.maximum = maximum;
        }

        public String translationKey() { return translationKey; }
        public int maximum() { return maximum; }
    }

    private final Type type;
    private final int level;

    public ManaDrillUpgradeItem(Type type, int level) {
        super(new Item.Properties().stacksTo(1).rarity(rarityFor(type, level)));
        this.type = type;
        this.level = Math.max(1, Math.min(type.maximum(), level));
    }

    private static Rarity rarityFor(Type type, int level) {
        int clamped = Math.max(1, Math.min(type.maximum(), level));
        if (clamped >= type.maximum()) return Rarity.EPIC;
        if (clamped >= Math.max(2, type.maximum() / 2)) return Rarity.RARE;
        return Rarity.UNCOMMON;
    }

    public Type type() { return type; }
    public int level() { return level; }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return level >= type.maximum();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.mana_drill_upgrade.level", this.level,
                        this.type.maximum())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "tooltip.metatech_reborn.mana_drill_upgrade." + this.type.translationKey(), this.level)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.metatech_reborn.mana_drill_upgrade.effect." + this.type.translationKey(),
                        effectValue())
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    private int effectValue() {
        return switch (type) {
            case SPEED -> Math.round((1.0F - 1.0F / (1.0F + level)) * 100.0F);
            case LOOTING -> Math.round(level * 3.5F);
            case GENERATION -> 1 + level;
        };
    }
}
