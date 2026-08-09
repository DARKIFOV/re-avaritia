package ru.rfvv.metatechreborn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.pattern.MysticalInfusionPatternData;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;

import java.util.List;
import java.util.Optional;

public final class EncodedMysticalInfusionPatternItem extends Item {
    private static final String TAG_PATTERN = "MysticalInfusionPattern";

    public EncodedMysticalInfusionPatternItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    public static Optional<MysticalInfusionPatternData> read(ItemStack stack) {
        if (!(stack.getItem() instanceof EncodedMysticalInfusionPatternItem)) return Optional.empty();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_PATTERN)) return Optional.empty();
        return MysticalInfusionPatternData.load(tag.getCompound(TAG_PATTERN));
    }

    public static ItemStack create(MysticalInfusionPatternData data) {
        ItemStack stack = new ItemStack(ModMysticalInfusion.ENCODED_PATTERN.get());
        stack.getOrCreateTag().put(TAG_PATTERN, data.save());
        return stack;
    }

    @Override public boolean isFoil(ItemStack stack) { return read(stack).isPresent(); }

    @Override
    public Component getName(ItemStack stack) {
        return read(stack)
                .<Component>map(data -> Component.literal("Мистический шаблон: ").append(data.output().getHoverName()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Optional<MysticalInfusionPatternData> decoded = read(stack);
        if (decoded.isEmpty()) {
            tooltip.add(Component.literal("Повреждённый шаблон").withStyle(ChatFormatting.RED));
            return;
        }
        var data = decoded.get();
        tooltip.add(Component.literal("Результат: ").append(data.output().getHoverName())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Рецепт: " + data.recipeId()).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("1 предмет алтаря + 8 пьедесталов").withStyle(ChatFormatting.GRAY));
    }
}
