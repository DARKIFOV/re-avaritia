package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A single, non-stackable tiered upgrade for the Mana Drill. */
public final class ManaDrillUpgradeItem extends Item {
    public enum Type {
        SPEED("speed"),
        LOOTING("looting"),
        GENERATION("generation");

        private final String translationKey;

        Type(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    private final Type type;
    private final int level;

    public ManaDrillUpgradeItem(Type type, int level) {
        super(new Item.Properties().stacksTo(1));
        this.type = type;
        this.level = Math.max(1, level);
    }

    public Type type() {
        return type;
    }

    public int level() {
        return level;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.mana_drill_upgrade.level", this.level)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "tooltip.metatech_reborn.mana_drill_upgrade." + this.type.translationKey(), this.level)
                .withStyle(ChatFormatting.GRAY));
    }
}
