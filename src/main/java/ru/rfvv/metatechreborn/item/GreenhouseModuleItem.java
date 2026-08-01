package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Functional module installed into one of the three greenhouse module slots. */
public final class GreenhouseModuleItem extends Item {
    public enum Type {
        ECONOMY("economy"),
        EFFICIENCY("efficiency"),
        SPEED("speed"),
        INFINITE_DAY("infinite_day"),
        INFINITE_NIGHT("infinite_night"),
        INFINITE_LAVA("infinite_lava");

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

    public GreenhouseModuleItem(Type type, int level) {
        super(new Properties().stacksTo(1));
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.greenhouse_module." + type.translationKey(), this.level)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.greenhouse_module.slot_limit")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
