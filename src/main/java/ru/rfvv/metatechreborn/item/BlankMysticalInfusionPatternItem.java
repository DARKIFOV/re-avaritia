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

public final class BlankMysticalInfusionPatternItem extends Item {
    public BlankMysticalInfusionPatternItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Шаблон рецепта Mystical Agriculture Infusion Altar")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Кодируется в Кодировщике мистического алтаря")
                .withStyle(ChatFormatting.GRAY));
    }
}
