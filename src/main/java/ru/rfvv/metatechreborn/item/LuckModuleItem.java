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

/** Original MetaAdvanced fortune-module progression: 1,2,3,5,10,15,20,25,30,35,40,50. */
public final class LuckModuleItem extends Item {
    public static final int[] LEVELS = {1, 2, 3, 5, 10, 15, 20, 25, 30, 35, 40, 50};

    private final int fortuneLevel;

    public LuckModuleItem(int fortuneLevel) {
        super(new Item.Properties().stacksTo(1).rarity(rarity(fortuneLevel)));
        this.fortuneLevel = Math.max(1, fortuneLevel);
    }

    private static Rarity rarity(int level) {
        if (level >= 40) return Rarity.EPIC;
        if (level >= 15) return Rarity.RARE;
        return Rarity.UNCOMMON;
    }

    public int fortuneLevel() {
        return fortuneLevel;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return fortuneLevel >= 40;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.metatech_reborn.luck_module.level", fortuneLevel)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.luck_module.install")
                .withStyle(ChatFormatting.GRAY));
    }
}
