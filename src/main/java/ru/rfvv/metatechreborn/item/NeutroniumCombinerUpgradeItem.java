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

/** Stackable upgrades for the Neutronium Combiner. */
public final class NeutroniumCombinerUpgradeItem extends Item {
    public enum Type {
        SPEED("speed", 8),
        EFFICIENCY("efficiency", 8),
        OUTPUT("output", 3);

        private final String translationKey;
        private final int maximum;

        Type(String translationKey, int maximum) {
            this.translationKey = translationKey;
            this.maximum = maximum;
        }

        public String translationKey() {
            return translationKey;
        }

        public int maximum() {
            return maximum;
        }
    }

    private final Type type;

    public NeutroniumCombinerUpgradeItem(Type type) {
        super(new Item.Properties().stacksTo(type.maximum()));
        this.type = type;
    }

    public Type type() {
        return type;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        "tooltip.metatech_reborn.neutron_combiner_upgrade." + type.translationKey())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "tooltip.metatech_reborn.neutron_combiner_upgrade.installed", stack.getCount(), type.maximum())
                .withStyle(ChatFormatting.GRAY));
    }
}
