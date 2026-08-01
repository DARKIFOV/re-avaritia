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
import ru.rfvv.metatechreborn.pattern.ExtremePatternData;

import java.util.List;
import java.util.Optional;

/** One encoded complete 9x9 recipe. */
public final class EncodedExtremePatternItem extends Item {
    private static final String TAG_PATTERN = "ExtremePattern";

    public EncodedExtremePatternItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    public static void write(ItemStack stack, ExtremePatternData pattern) {
        stack.getOrCreateTag().put(TAG_PATTERN, pattern.save());
    }

    public static Optional<ExtremePatternData> read(ItemStack stack) {
        if (!(stack.getItem() instanceof EncodedExtremePatternItem)) return Optional.empty();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_PATTERN)) return Optional.empty();
        return ExtremePatternData.load(tag.getCompound(TAG_PATTERN));
    }

    public static ItemStack create(ExtremePatternData pattern) {
        ItemStack stack = new ItemStack(ru.rfvv.metatechreborn.registry.ModItems.ENCODED_EXTREME_PATTERN.get());
        write(stack, pattern);
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return read(stack).isPresent();
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<ExtremePatternData> decoded = read(stack);
        if (decoded.isPresent()) {
            return Component.translatable("item.metatech_reborn.encoded_extreme_pattern.named",
                    decoded.get().output().getHoverName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        Optional<ExtremePatternData> decoded = read(stack);
        if (decoded.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.metatech_reborn.extreme_pattern.invalid")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ExtremePatternData pattern = decoded.get();
        tooltip.add(Component.translatable("tooltip.metatech_reborn.extreme_pattern.output",
                        pattern.output().getHoverName(), pattern.output().getCount())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.extreme_pattern.ingredients",
                        pattern.ingredientCount())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.extreme_pattern.full_grid")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
