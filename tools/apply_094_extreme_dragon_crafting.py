from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[1]


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(dedent(content).lstrip(), encoding="utf-8")


def patch(path: str, old: str, new: str, label: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


write("src/main/java/ru/rfvv/metatechreborn/pattern/DragonFusionPatternData.java", r'''
package ru.rfvv.metatechreborn.pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record DragonFusionPatternData(ResourceLocation recipeId, ItemStack output, int tier, long energy) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Recipe", recipeId.toString());
        CompoundTag outputTag = new CompoundTag();
        output.save(outputTag);
        tag.put("Output", outputTag);
        tag.putInt("Tier", tier);
        tag.putLong("Energy", energy);
        return tag;
    }

    public static Optional<DragonFusionPatternData> load(CompoundTag tag) {
        if (!tag.contains("Recipe") || !tag.contains("Output")) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (id == null) return Optional.empty();
        ItemStack output = ItemStack.of(tag.getCompound("Output"));
        if (output.isEmpty()) return Optional.empty();
        return Optional.of(new DragonFusionPatternData(id, output, tag.getInt("Tier"), tag.getLong("Energy")));
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/item/BlankDragonPatternItem.java", r'''
package ru.rfvv.metatechreborn.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class BlankDragonPatternItem extends Item {
    public BlankDragonPatternItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/item/EncodedDragonPatternItem.java", r'''
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
import ru.rfvv.metatechreborn.pattern.DragonFusionPatternData;
import ru.rfvv.metatechreborn.registry.ModItems;

import java.util.List;
import java.util.Optional;

public final class EncodedDragonPatternItem extends Item {
    private static final String TAG_PATTERN = "DragonFusionPattern";

    public EncodedDragonPatternItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static ItemStack create(DragonFusionPatternData data) {
        ItemStack stack = new ItemStack(ModItems.ENCODED_DRAGON_PATTERN.get());
        stack.getOrCreateTag().put(TAG_PATTERN, data.save());
        return stack;
    }

    public static Optional<DragonFusionPatternData> read(ItemStack stack) {
        if (!(stack.getItem() instanceof EncodedDragonPatternItem)) return Optional.empty();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_PATTERN)) return Optional.empty();
        return DragonFusionPatternData.load(tag.getCompound(TAG_PATTERN));
    }

    @Override public boolean isFoil(ItemStack stack) { return read(stack).isPresent(); }

    @Override public Component getName(ItemStack stack) {
        return read(stack)
                .map(data -> Component.translatable("item.metatech_reborn.encoded_dragon_pattern.named",
                        data.output().getHoverName()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                          TooltipFlag flag) {
        Optional<DragonFusionPatternData> data = read(stack);
        if (data.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.metatech_reborn.dragon_pattern.invalid")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        DragonFusionPatternData pattern = data.get();
        tooltip.add(Component.translatable("tooltip.metatech_reborn.dragon_pattern.output",
                pattern.output().getHoverName()).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.dragon_pattern.tier",
                tierKey(pattern.tier())).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.metatech_reborn.dragon_pattern.energy",
                pattern.energy()).withStyle(ChatFormatting.GRAY));
    }

    private static Component tierKey(int tier) {
        return Component.translatable("gui.metatech_reborn.dragon.tier." + switch (tier) {
            case 0 -> "basic";
            case 1 -> "wyvern";
            case 2 -> "draconic";
            case 3 -> "chaotic";
            default -> "none";
        });
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/integration/dragon/DragonFusionSupport.java", r'''
package ru.rfvv.metatechreborn.integration.dragon;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import ru.rfvv.metatechreborn.MetaTechReborn;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class DragonFusionSupport {
    public static final ResourceLocation FUSION_TYPE = new ResourceLocation("draconicevolution", "fusion_crafting");
    private static final ResourceLocation BASIC = new ResourceLocation("draconicevolution", "basic_crafting_injector");
    private static final ResourceLocation WYVERN = new ResourceLocation("draconicevolution", "wyvern_crafting_injector");
    private static final ResourceLocation DRACONIC = new ResourceLocation("draconicevolution", "awakened_crafting_injector");
    private static final ResourceLocation CHAOTIC = new ResourceLocation("draconicevolution", "chaotic_crafting_injector");

    public record View(Recipe<?> raw, ResourceLocation id, Ingredient catalyst, List<Ingredient> ingredients,
                       ItemStack output, int tier, String tierName, long energy) {}

    private DragonFusionSupport() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<View> all(Level level) {
        if (level == null) return List.of();
        RecipeType type = ForgeRegistries.RECIPE_TYPES.getValue(FUSION_TYPE);
        if (type == null) return List.of();
        List<Recipe<?>> recipes = (List<Recipe<?>>) (List<?>) level.getRecipeManager().getAllRecipesFor(type);
        List<View> result = new ArrayList<>();
        for (Recipe<?> recipe : recipes) fromRecipe(recipe, level.registryAccess()).ifPresent(result::add);
        return result;
    }

    public static Optional<View> find(Level level, ResourceLocation id) {
        return all(level).stream().filter(view -> view.id().equals(id)).findFirst();
    }

    public static Optional<View> fromRecipe(Recipe<?> recipe, RegistryAccess access) {
        try {
            Method catalystMethod = recipe.getClass().getMethod("getCatalyst");
            Method tierMethod = recipe.getClass().getMethod("getRecipeTier");
            Method energyMethod = recipe.getClass().getMethod("getEnergyCost");
            Object tierObject = tierMethod.invoke(recipe);
            String tierName = tierObject instanceof Enum<?> e ? e.name() : String.valueOf(tierObject);
            int tier = tierIndex(tierName);
            if (tier < 0) return Optional.empty();
            Ingredient catalyst = (Ingredient) catalystMethod.invoke(recipe);
            long energy = ((Number) energyMethod.invoke(recipe)).longValue();
            List<Ingredient> ingredients = List.copyOf(recipe.getIngredients());
            if (ingredients.size() > 12) return Optional.empty();
            ItemStack output = recipe.getResultItem(access).copy();
            if (output.isEmpty()) return Optional.empty();
            return Optional.of(new View(recipe, recipe.getId(), catalyst, ingredients,
                    output, tier, tierName.toUpperCase(Locale.ROOT), energy));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            return Optional.empty();
        }
    }

    public static int tierIndex(String tier) {
        if (tier == null) return -1;
        return switch (tier.toUpperCase(Locale.ROOT)) {
            case "DRACONIUM", "BASIC" -> 0;
            case "WYVERN" -> 1;
            case "DRACONIC", "AWAKENED" -> 2;
            case "CHAOTIC", "CHAOS" -> 3;
            default -> -1;
        };
    }

    public static String tierKey(int tier) {
        return switch (tier) {
            case 0 -> "basic";
            case 1 -> "wyvern";
            case 2 -> "draconic";
            case 3 -> "chaotic";
            default -> "none";
        };
    }

    public static int injectorTier(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (BASIC.equals(id)) return 0;
        if (WYVERN.equals(id)) return 1;
        if (DRACONIC.equals(id)) return 2;
        if (CHAOTIC.equals(id)) return 3;
        return -1;
    }

    public static boolean isInjector(ItemStack stack) { return injectorTier(stack) >= 0; }

    public static int requiredCount(Ingredient ingredient) {
        try {
            Method method = ingredient.getClass().getMethod("getCount");
            Object value = method.invoke(ingredient);
            if (value instanceof Number n) return Math.max(1, n.intValue());
        } catch (ReflectiveOperationException | RuntimeException ignored) {}
        return 1;
    }

    public static ItemStack displayStack(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return ItemStack.EMPTY;
        ItemStack copy = stacks[0].copy();
        copy.setCount(requiredCount(ingredient));
        return copy;
    }

    public static boolean inputMatches(View view, ItemStack catalyst, List<ItemStack> ingredientStacks) {
        if (!view.catalyst().test(catalyst)) return false;
        if (ingredientStacks.size() < view.ingredients().size()) return false;
        for (int i = 0; i < view.ingredients().size(); i++) {
            if (!view.ingredients().get(i).test(ingredientStacks.get(i))) return false;
        }
        return true;
    }

    public static ItemStack assembleResult(View view, RegistryAccess access, ItemStack catalyst,
                                           List<ItemStack> ingredients, int injectorTier) {
        try {
            Recipe<?> raw = view.raw();
            ClassLoader loader = raw.getClass().getClassLoader();
            Class<?> inventoryClass = Class.forName(
                    "com.brandon3055.draconicevolution.api.crafting.IFusionInventory", false, loader);
            Class<?> injectorClass = Class.forName(
                    "com.brandon3055.draconicevolution.api.crafting.IFusionInjector", false, loader);
            Class<?> techClass = Class.forName("com.brandon3055.brandonscore.api.TechLevel", false, loader);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object tech = Enum.valueOf((Class<? extends Enum>) techClass.asSubclass(Enum.class),
                    switch (Math.max(0, Math.min(3, injectorTier))) {
                        case 0 -> "DRACONIUM";
                        case 1 -> "WYVERN";
                        case 2 -> "DRACONIC";
                        default -> "CHAOTIC";
                    });

            List<Object> injectorProxies = new ArrayList<>();
            for (ItemStack stack : ingredients) {
                AtomicReference<ItemStack> ref = new AtomicReference<>(stack.copy());
                Object proxy = Proxy.newProxyInstance(loader, new Class<?>[]{injectorClass}, (obj, method, args) -> {
                    return switch (method.getName()) {
                        case "getInjectorTier" -> tech;
                        case "getInjectorStack" -> ref.get();
                        case "setInjectorStack" -> { ref.set(((ItemStack) args[0]).copy()); yield null; }
                        case "getInjectorEnergy", "getEnergyRequirement" -> 0L;
                        case "setInjectorEnergy", "setEnergyRequirement" -> null;
                        case "validate" -> true;
                        case "toString" -> "MetaTechFusionInjector";
                        case "hashCode" -> System.identityHashCode(obj);
                        case "equals" -> obj == args[0];
                        default -> defaultValue(method.getReturnType());
                    };
                });
                injectorProxies.add(proxy);
            }

            AtomicReference<ItemStack> catalystRef = new AtomicReference<>(catalyst.copy());
            AtomicReference<ItemStack> outputRef = new AtomicReference<>(ItemStack.EMPTY);
            Object inventoryProxy = Proxy.newProxyInstance(loader, new Class<?>[]{inventoryClass}, (obj, method, args) -> {
                return switch (method.getName()) {
                    case "getCatalystStack" -> catalystRef.get();
                    case "getOutputStack" -> outputRef.get();
                    case "setCatalystStack" -> { catalystRef.set(((ItemStack) args[0]).copy()); yield null; }
                    case "setOutputStack" -> { outputRef.set(((ItemStack) args[0]).copy()); yield null; }
                    case "getInjectors" -> injectorProxies;
                    case "getMinimumTier" -> tech;
                    case "getContainerSize" -> 2 + ingredients.size();
                    case "isEmpty" -> catalystRef.get().isEmpty() && ingredients.stream().allMatch(ItemStack::isEmpty);
                    case "getItem" -> {
                        int slot = (Integer) args[0];
                        if (slot == 0) yield catalystRef.get();
                        if (slot == 1) yield outputRef.get();
                        int index = slot - 2;
                        yield index >= 0 && index < ingredients.size() ? ingredients.get(index) : ItemStack.EMPTY;
                    }
                    case "setChanged", "clearContent" -> null;
                    case "stillValid" -> true;
                    case "toString" -> "MetaTechFusionInventory";
                    case "hashCode" -> System.identityHashCode(obj);
                    case "equals" -> obj == args[0];
                    default -> defaultValue(method.getReturnType());
                };
            });

            Method assemble = Arrays.stream(raw.getClass().getMethods())
                    .filter(method -> method.getName().equals("assemble") && method.getParameterCount() == 2
                            && method.getParameterTypes()[0].isAssignableFrom(inventoryClass))
                    .findFirst().orElse(null);
            if (assemble != null) {
                Object result = assemble.invoke(raw, inventoryProxy, access);
                if (result instanceof ItemStack stack && !stack.isEmpty()) return stack.copy();
            }
        } catch (Throwable error) {
            MetaTechReborn.LOGGER.debug("Falling back to static Draconic fusion output for {}", view.id(), error);
        }
        return view.output().copy();
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/block/DragonPatternEncoderBlock.java", r'''
package ru.rfvv.metatechreborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.blockentity.DragonPatternEncoderBlockEntity;

public final class DragonPatternEncoderBlock extends BaseEntityBlock {
    public DragonPatternEncoderBlock(Properties properties) { super(properties); }
    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.MODEL; }

    @Override public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
            @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DragonPatternEncoderBlockEntity encoder) NetworkHooks.openScreen(server, encoder, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DragonPatternEncoderBlockEntity encoder) {
                encoder.getDrops().forEach(stack -> Containers.dropItemStack(level,
                        pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DragonPatternEncoderBlockEntity(pos, state);
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/block/ExtremeDragonAssemblerBlock.java", r'''
package ru.rfvv.metatechreborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;

public final class ExtremeDragonAssemblerBlock extends BaseEntityBlock {
    public ExtremeDragonAssemblerBlock(Properties properties) { super(properties); }
    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.MODEL; }

    @Override public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
            @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExtremeDragonAssemblerBlockEntity assembler) NetworkHooks.openScreen(server, assembler, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExtremeDragonAssemblerBlockEntity assembler) {
                assembler.getDrops().forEach(stack -> Containers.dropItemStack(level,
                        pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ExtremeDragonAssemblerBlockEntity(pos, state);
    }

    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
            @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.EXTREME_DRAGON_ASSEMBLER.get(),
                ExtremeDragonAssemblerBlockEntity::serverTick);
    }

    @Override public boolean isPathfindable(@NotNull BlockState state, @NotNull BlockGetter level,
            @NotNull BlockPos pos, @NotNull net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/blockentity/DragonPatternEncoderBlockEntity.java", r'''
package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.item.EncodedDragonPatternItem;
import ru.rfvv.metatechreborn.menu.DragonPatternEncoderMenu;
import ru.rfvv.metatechreborn.pattern.DragonFusionPatternData;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModItems;

import java.util.Optional;

public final class DragonPatternEncoderBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BLANK_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int CATALYST_GHOST_SLOT = 2;
    public static final int INGREDIENT_GHOST_START = 3;
    public static final int INGREDIENT_GHOST_COUNT = 12;
    public static final int PREVIEW_OUTPUT_SLOT = 15;
    public static final int TOTAL_SLOTS = 16;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_ENCODED = 2;
    public static final int STATUS_NO_RECIPE = 3;
    public static final int STATUS_NO_BLANK = 4;
    public static final int STATUS_OUTPUT_BLOCKED = 5;

    private ResourceLocation selectedRecipe;
    private int status = STATUS_IDLE;
    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override public int getSlotLimit(int slot) { return slot == BLANK_SLOT ? 64 : 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == BLANK_SLOT && stack.is(ModItems.BLANK_DRAGON_PATTERN.get());
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final LazyOptional<IItemHandler> cap = LazyOptional.of(() -> items);
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> status;
                case 1 -> selectedRecipe == null ? 0 : 1;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { if (index == 0) status = value; }
        @Override public int getCount() { return 2; }
    };

    public DragonPatternEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAGON_PATTERN_ENCODER.get(), pos, state);
    }

    public boolean encode() {
        if (level == null || level.isClientSide || selectedRecipe == null) return false;
        if (!items.getStackInSlot(OUTPUT_SLOT).isEmpty()) { status = STATUS_OUTPUT_BLOCKED; setChanged(); return false; }
        ItemStack blank = items.getStackInSlot(BLANK_SLOT);
        if (blank.isEmpty()) { status = STATUS_NO_BLANK; setChanged(); return false; }
        Optional<DragonFusionSupport.View> view = DragonFusionSupport.find(level, selectedRecipe);
        if (view.isEmpty()) { status = STATUS_NO_RECIPE; setChanged(); return false; }
        DragonFusionSupport.View recipe = view.get();
        ItemStack encoded = EncodedDragonPatternItem.create(new DragonFusionPatternData(
                recipe.id(), recipe.output().copy(), recipe.tier(), recipe.energy()));
        blank.shrink(1);
        items.setStackInSlot(BLANK_SLOT, blank);
        items.setStackInSlot(OUTPUT_SLOT, encoded);
        status = STATUS_ENCODED;
        setChanged();
        return true;
    }

    public void selectRecipe(ResourceLocation recipeId) {
        if (level == null || level.isClientSide) return;
        Optional<DragonFusionSupport.View> found = DragonFusionSupport.find(level, recipeId);
        if (found.isEmpty()) { clearRecipe(); status = STATUS_NO_RECIPE; return; }
        selectedRecipe = recipeId;
        DragonFusionSupport.View view = found.get();
        items.setStackInSlot(CATALYST_GHOST_SLOT, DragonFusionSupport.displayStack(view.catalyst()));
        for (int i = 0; i < INGREDIENT_GHOST_COUNT; i++) {
            ItemStack stack = i < view.ingredients().size()
                    ? DragonFusionSupport.displayStack(view.ingredients().get(i)) : ItemStack.EMPTY;
            items.setStackInSlot(INGREDIENT_GHOST_START + i, stack);
        }
        items.setStackInSlot(PREVIEW_OUTPUT_SLOT, view.output().copy());
        status = STATUS_READY;
        setChanged();
    }

    public void clearRecipe() {
        selectedRecipe = null;
        for (int slot = CATALYST_GHOST_SLOT; slot < TOTAL_SLOTS; slot++) items.setStackInSlot(slot, ItemStack.EMPTY);
        status = STATUS_IDLE;
        setChanged();
    }

    public ItemStackHandler getItems() { return items; }
    public ContainerData getData() { return data; }
    public ResourceLocation getSelectedRecipe() { return selectedRecipe; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot : new int[]{BLANK_SLOT, OUTPUT_SLOT}) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.dragon_pattern_encoder");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
            @NotNull Player player) {
        return new DragonPatternEncoderMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        if (selectedRecipe != null) tag.putString("Recipe", selectedRecipe.toString());
        tag.putInt("Status", status);
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        selectedRecipe = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        status = tag.getInt("Status");
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> capability,
            @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return cap.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremeDragonAssemblerBlockEntity.java", r'''
package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.item.EncodedDragonPatternItem;
import ru.rfvv.metatechreborn.menu.ExtremeDragonAssemblerMenu;
import ru.rfvv.metatechreborn.pattern.DragonFusionPatternData;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ExtremeDragonAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INJECTOR_START = 0;
    public static final int INJECTOR_COUNT = 12;
    public static final int INGREDIENT_START = 12;
    public static final int INGREDIENT_COUNT = 12;
    public static final int CATALYST_SLOT = 24;
    public static final int OUTPUT_SLOT = 25;
    public static final int PATTERN_START = 26;
    public static final int PATTERN_COUNT = 36;
    public static final int TOTAL_SLOTS = 62;
    public static final int ENERGY_CAPACITY = 2_000_000_000;
    public static final int CRAFT_TICKS = 200;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_MISSING_INJECTORS = 1;
    public static final int STATUS_TIER_LOW = 2;
    public static final int STATUS_MISSING_INPUT = 3;
    public static final int STATUS_NO_ENERGY = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_RUNNING = 6;

    private int progress;
    private long energySpent;
    private ResourceLocation activeRecipe;
    private int status = STATUS_IDLE;
    private int activeRecipeTier = -1;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= INJECTOR_START && slot < INJECTOR_START + INJECTOR_COUNT)
                return DragonFusionSupport.isInjector(stack);
            if (slot == OUTPUT_SLOT) return false;
            if (slot >= PATTERN_START && slot < PATTERN_START + PATTERN_COUNT)
                return stack.is(ModItems.ENCODED_DRAGON_PATTERN.get());
            return slot >= INGREDIENT_START && slot <= CATALYST_SLOT;
        }
        @Override public int getSlotLimit(int slot) {
            if (slot >= INJECTOR_START && slot < INJECTOR_START + INJECTOR_COUNT) return 1;
            if (slot >= PATTERN_START && slot < PATTERN_START + PATTERN_COUNT) return 1;
            return 64;
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 100_000_000, 0) {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) setChanged();
            return received;
        }
    };
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
    private final LazyOptional<net.minecraftforge.energy.IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> CRAFT_TICKS;
                case 2 -> machineTier() + 1;
                case 3 -> energy.getEnergyStored();
                case 4 -> ENERGY_CAPACITY;
                case 5 -> activeRecipeTier + 1;
                case 6 -> status;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 5) activeRecipeTier = value - 1;
            else if (index == 6) status = value;
        }
        @Override public int getCount() { return 7; }
    };

    public ExtremeDragonAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTREME_DRAGON_ASSEMBLER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ExtremeDragonAssemblerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) return;
        int tier = machineTier();
        if (tier < 0) { resetCraft(STATUS_MISSING_INJECTORS); return; }

        Optional<DragonFusionSupport.View> selected = activeRecipe == null
                ? findCraftablePattern(tier) : DragonFusionSupport.find(level, activeRecipe);
        if (selected.isEmpty()) { resetCraft(STATUS_IDLE); return; }
        DragonFusionSupport.View view = selected.get();
        activeRecipeTier = view.tier();
        if (view.tier() > tier) { resetCraft(STATUS_TIER_LOW); return; }
        if (!inputsMatch(view)) { resetCraft(STATUS_MISSING_INPUT); return; }
        if (!canOutput(view.output())) { resetCraft(STATUS_OUTPUT_FULL); return; }

        activeRecipe = view.id();
        long targetSpent = (view.energy() * (progress + 1L)) / CRAFT_TICKS;
        long required = Math.max(0L, targetSpent - energySpent);
        if (required > 0L) {
            if (required > Integer.MAX_VALUE || energy.getEnergyStored() < required) {
                status = STATUS_NO_ENERGY; setChanged(); return;
            }
            int extracted = energy.extractEnergy((int) required, false);
            if (extracted < required) { status = STATUS_NO_ENERGY; setChanged(); return; }
            energySpent += extracted;
        }

        status = STATUS_RUNNING;
        progress++;
        if (progress >= CRAFT_TICKS) finishCraft(view, tier);
        setChanged();
    }

    private Optional<DragonFusionSupport.View> findCraftablePattern(int tier) {
        for (int slot = PATTERN_START; slot < PATTERN_START + PATTERN_COUNT; slot++) {
            ItemStack patternStack = items.getStackInSlot(slot);
            Optional<DragonFusionPatternData> pattern = EncodedDragonPatternItem.read(patternStack);
            if (pattern.isEmpty()) continue;
            Optional<DragonFusionSupport.View> view = DragonFusionSupport.find(level, pattern.get().recipeId());
            if (view.isPresent() && view.get().tier() <= tier && inputsMatch(view.get()) && canOutput(view.get().output()))
                return view;
        }
        return Optional.empty();
    }

    private boolean inputsMatch(DragonFusionSupport.View view) {
        List<ItemStack> ingredientStacks = new ArrayList<>(INGREDIENT_COUNT);
        for (int i = 0; i < INGREDIENT_COUNT; i++) ingredientStacks.add(items.getStackInSlot(INGREDIENT_START + i));
        return DragonFusionSupport.inputMatches(view, items.getStackInSlot(CATALYST_SLOT), ingredientStacks);
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void finishCraft(DragonFusionSupport.View view, int tier) {
        List<ItemStack> ingredientStacks = new ArrayList<>();
        for (int i = 0; i < view.ingredients().size(); i++)
            ingredientStacks.add(items.getStackInSlot(INGREDIENT_START + i).copy());
        ItemStack result = DragonFusionSupport.assembleResult(view, level.registryAccess(),
                items.getStackInSlot(CATALYST_SLOT).copy(), ingredientStacks, tier);
        if (result.isEmpty()) result = view.output().copy();

        shrinkSlot(CATALYST_SLOT, DragonFusionSupport.requiredCount(view.catalyst()));
        for (int i = 0; i < view.ingredients().size(); i++)
            shrinkSlot(INGREDIENT_START + i, DragonFusionSupport.requiredCount(view.ingredients().get(i)));

        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) items.setStackInSlot(OUTPUT_SLOT, result.copy());
        else { output.grow(result.getCount()); items.setStackInSlot(OUTPUT_SLOT, output); }
        resetCraft(STATUS_IDLE);
    }

    private void shrinkSlot(int slot, int count) {
        ItemStack stack = items.getStackInSlot(slot);
        stack.shrink(Math.max(1, count));
        items.setStackInSlot(slot, stack);
    }

    private void resetCraft(int newStatus) {
        progress = 0;
        energySpent = 0L;
        activeRecipe = null;
        activeRecipeTier = -1;
        status = newStatus;
        setChanged();
    }

    public int machineTier() {
        int min = 3;
        for (int i = 0; i < INJECTOR_COUNT; i++) {
            int tier = DragonFusionSupport.injectorTier(items.getStackInSlot(INJECTOR_START + i));
            if (tier < 0) return -1;
            min = Math.min(min, tier);
        }
        return min;
    }

    public ItemStackHandler getItems() { return items; }
    public ContainerData getData() { return data; }
    public EnergyStorage getEnergy() { return energy; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.extreme_dragon_assembler");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
            @NotNull Player player) {
        return new ExtremeDragonAssemblerMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putLong("EnergySpent", energySpent);
        if (activeRecipe != null) tag.putString("ActiveRecipe", activeRecipe.toString());
        tag.putInt("Status", status);
        tag.putInt("ActiveTier", activeRecipeTier);
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        energy.receiveEnergy(tag.getInt("Energy"), false);
        progress = tag.getInt("Progress");
        energySpent = tag.getLong("EnergySpent");
        activeRecipe = tag.contains("ActiveRecipe") ? ResourceLocation.tryParse(tag.getString("ActiveRecipe")) : null;
        status = tag.getInt("Status");
        activeRecipeTier = tag.getInt("ActiveTier");
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> capability,
            @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (capability == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); energyCap.invalidate(); }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/menu/DragonPatternEncoderMenu.java", r'''
package ru.rfvv.metatechreborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.DragonPatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class DragonPatternEncoderMenu extends AbstractContainerMenu {
    public static final int ENCODE_BUTTON = 0;
    public static final int CLEAR_BUTTON = 1;
    public static final int PLAYER_START = DragonPatternEncoderBlockEntity.TOTAL_SLOTS;
    private final DragonPatternEncoderBlockEntity blockEntity;
    private final ContainerData data;

    public DragonPatternEncoderMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (DragonPatternEncoderBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(2));
    }
    public DragonPatternEncoderMenu(int id, Inventory inventory, DragonPatternEncoderBlockEntity be, ContainerData data) {
        super(ModMenus.DRAGON_PATTERN_ENCODER.get(), id);
        this.blockEntity = be;
        this.data = data;

        addSlot(new SlotItemHandler(be.getItems(), DragonPatternEncoderBlockEntity.BLANK_SLOT, 202, 55));
        addSlot(new SlotItemHandler(be.getItems(), DragonPatternEncoderBlockEntity.OUTPUT_SLOT, 250, 55) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        addGhost(be, DragonPatternEncoderBlockEntity.CATALYST_GHOST_SLOT, 18, 55);
        for (int i = 0; i < 12; i++) addGhost(be, DragonPatternEncoderBlockEntity.INGREDIENT_GHOST_START + i,
                58 + (i % 4) * 20, 35 + (i / 4) * 20);
        addGhost(be, DragonPatternEncoderBlockEntity.PREVIEW_OUTPUT_SLOT, 150, 55);

        int invY = 136;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 58 + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 58 + col * 18, invY + 58));
        addDataSlots(data);
    }

    private void addGhost(DragonPatternEncoderBlockEntity be, int slot, int x, int y) {
        addSlot(new SlotItemHandler(be.getItems(), slot, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            @Override public boolean mayPickup(@NotNull Player player) { return false; }
        });
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(beLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.DRAGON_PATTERN_ENCODER.get());
    }
    private net.minecraft.world.level.Level beLevel() { return blockEntity.getLevel(); }

    @Override public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == ENCODE_BUTTON) return blockEntity.encode();
        if (id == CLEAR_BUTTON) { blockEntity.clearRecipe(); return true; }
        return false;
    }
    public void applyRecipe(net.minecraft.resources.ResourceLocation id) { blockEntity.selectRecipe(id); }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.BLANK_DRAGON_PATTERN.get())) {
            if (!moveItemStackTo(stack, DragonPatternEncoderBlockEntity.BLANK_SLOT,
                    DragonPatternEncoderBlockEntity.BLANK_SLOT + 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    public int getStatus() { return data.get(0); }
    public boolean hasRecipe() { return data.get(1) != 0; }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/menu/ExtremeDragonAssemblerMenu.java", r'''
package ru.rfvv.metatechreborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class ExtremeDragonAssemblerMenu extends AbstractContainerMenu {
    public static final int PLAYER_START = ExtremeDragonAssemblerBlockEntity.TOTAL_SLOTS;
    private final ExtremeDragonAssemblerBlockEntity blockEntity;
    private final ContainerData data;

    public ExtremeDragonAssemblerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (ExtremeDragonAssemblerBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(7));
    }
    public ExtremeDragonAssemblerMenu(int id, Inventory inventory, ExtremeDragonAssemblerBlockEntity be,
                                      ContainerData data) {
        super(ModMenus.EXTREME_DRAGON_ASSEMBLER.get(), id);
        this.blockEntity = be;
        this.data = data;

        for (int i = 0; i < 12; i++) addSlot(new SlotItemHandler(be.getItems(),
                ExtremeDragonAssemblerBlockEntity.INJECTOR_START + i, 12 + (i % 2) * 20, 32 + (i / 2) * 20));
        for (int i = 0; i < 12; i++) addSlot(new SlotItemHandler(be.getItems(),
                ExtremeDragonAssemblerBlockEntity.INGREDIENT_START + i, 76 + (i % 4) * 20, 40 + (i / 4) * 20));
        addSlot(new SlotItemHandler(be.getItems(), ExtremeDragonAssemblerBlockEntity.CATALYST_SLOT, 176, 60));
        addSlot(new SlotItemHandler(be.getItems(), ExtremeDragonAssemblerBlockEntity.OUTPUT_SLOT, 224, 60) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        for (int i = 0; i < 36; i++) addSlot(new SlotItemHandler(be.getItems(),
                ExtremeDragonAssemblerBlockEntity.PATTERN_START + i, 312 + (i % 9) * 18, 32 + (i / 9) * 18));

        int invY = 196;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 76 + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 76 + col * 18, invY + 58));
        addDataSlots(data);
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.EXTREME_DRAGON_ASSEMBLER.get());
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (DragonFusionSupport.isInjector(stack)) {
            if (!moveItemStackTo(stack, ExtremeDragonAssemblerBlockEntity.INJECTOR_START,
                    ExtremeDragonAssemblerBlockEntity.INJECTOR_START + 12, false)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.ENCODED_DRAGON_PATTERN.get())) {
            if (!moveItemStackTo(stack, ExtremeDragonAssemblerBlockEntity.PATTERN_START,
                    ExtremeDragonAssemblerBlockEntity.PATTERN_START + 36, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, ExtremeDragonAssemblerBlockEntity.INGREDIENT_START,
                ExtremeDragonAssemblerBlockEntity.CATALYST_SLOT + 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    public int progress() { return data.get(0); }
    public int maxProgress() { return data.get(1); }
    public int machineTier() { return data.get(2) - 1; }
    public int energy() { return data.get(3); }
    public int energyCapacity() { return data.get(4); }
    public int recipeTier() { return data.get(5) - 1; }
    public int status() { return data.get(6); }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/client/screen/DragonPatternEncoderScreen.java", r'''
package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.DragonPatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.menu.DragonPatternEncoderMenu;

public final class DragonPatternEncoderScreen extends AbstractContainerScreen<DragonPatternEncoderMenu> {
    private static final int RED = 0xFFE33434;
    private static final int DARK = 0xFF151010;
    private static final int PANEL = 0xFF2A1919;

    public DragonPatternEncoderScreen(DragonPatternEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 296; imageHeight = 216; inventoryLabelX = 58; inventoryLabelY = 124;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.dragon_encoder.encode"),
                b -> click(DragonPatternEncoderMenu.ENCODE_BUTTON)).bounds(leftPos + 194, topPos + 82, 82, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.dragon_encoder.clear"),
                b -> click(DragonPatternEncoderMenu.CLEAR_BUTTON)).bounds(leftPos + 194, topPos + 106, 82, 20).build());
    }
    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); super.render(g, mouseX, mouseY, partialTick); renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFF0F0F0);
        panel(g, 6, 20, 176, 104); panel(g, 188, 20, 102, 108); panel(g, 52, 130, 170, 82);
        for (int i = 0; i < 12; i++) slot(g, 56 + (i % 4) * 20, 33 + (i / 4) * 20, 0xFF8A2424);
        slot(g, 16, 53, RED); slot(g, 148, 53, RED); slot(g, 200, 53, RED); slot(g, 248, 53, RED);
        for (int row=0; row<3; row++) for (int col=0; col<9; col++) slot(g, 56+col*18, 134+row*18, 0xFF666666);
        for (int col=0; col<9; col++) slot(g, 56+col*18, 192, 0xFF666666);
    }
    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,DARK);
        g.fill(leftPos+x+1,topPos+y+1,leftPos+x+w-1,topPos+y+h-1,PANEL);
        g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+1,RED);
    }
    private void slot(GuiGraphics g,int x,int y,int accent) {
        g.fill(leftPos+x-1,topPos+y-1,leftPos+x+19,topPos+y+19,0xFF0B0B0B);
        g.fill(leftPos+x,topPos+y,leftPos+x+18,topPos+y+18,0xFFB8B8B8);
        g.fill(leftPos+x,topPos+y+17,leftPos+x+18,topPos+y+18,accent);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, 0xFF7A0000, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.dragon_encoder.catalyst"), 8, 112, 0xFFE8B6B6, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.dragon_encoder.ingredients"), 54, 23, 0xFFE8B6B6, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.dragon_encoder.result"), 145, 23, 0xFFE8B6B6, false);
        g.drawString(font, statusText(), 192, 32, statusColor(), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF555555, false);
    }
    private Component statusText() {
        return Component.translatable("gui.metatech_reborn.dragon_encoder.status." + switch (menu.getStatus()) {
            case DragonPatternEncoderBlockEntity.STATUS_READY -> "ready";
            case DragonPatternEncoderBlockEntity.STATUS_ENCODED -> "encoded";
            case DragonPatternEncoderBlockEntity.STATUS_NO_RECIPE -> "recipe";
            case DragonPatternEncoderBlockEntity.STATUS_NO_BLANK -> "blank";
            case DragonPatternEncoderBlockEntity.STATUS_OUTPUT_BLOCKED -> "output";
            default -> "idle";
        });
    }
    private int statusColor() { return menu.getStatus() == DragonPatternEncoderBlockEntity.STATUS_ENCODED ? 0xFF39C96B : RED; }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/client/screen/ExtremeDragonAssemblerScreen.java", r'''
package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.menu.ExtremeDragonAssemblerMenu;

public final class ExtremeDragonAssemblerScreen extends AbstractContainerScreen<ExtremeDragonAssemblerMenu> {
    private static final int RED = 0xFFE33434;
    private static final int DARK_RED = 0xFF711414;
    public ExtremeDragonAssemblerScreen(ExtremeDragonAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 492; imageHeight = 286; inventoryLabelX = 76; inventoryLabelY = 184;
    }
    @Override public void render(@NotNull GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        renderBackground(g); super.render(g,mouseX,mouseY,partialTick); machineTooltip(g,mouseX,mouseY); renderTooltip(g,mouseX,mouseY);
    }
    @Override protected void renderBg(@NotNull GuiGraphics g,float partialTick,int mouseX,int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFE8E8E8);
        panel(g,6,20,48,132); panel(g,62,20,212,132); panel(g,294,20,192,132); panel(g,62,186,176,94);
        for(int i=0;i<12;i++) slot(g,10+(i%2)*20,30+(i/2)*20,DARK_RED);
        for(int i=0;i<12;i++) slot(g,74+(i%4)*20,38+(i/4)*20,RED);
        slot(g,174,58,0xFFFF8A55); slot(g,222,58,RED);
        for(int i=0;i<36;i++) slot(g,310+(i%9)*18,30+(i/9)*18,0xFFB82929);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) slot(g,74+col*18,194+row*18,0xFF666666);
        for(int col=0;col<9;col++) slot(g,74+col*18,252,0xFF666666);

        int progress = menu.maxProgress() <= 0 ? 0 : (int)(98L * menu.progress() / menu.maxProgress());
        g.fill(leftPos+166,topPos+104,leftPos+266,topPos+114,0xFF160606);
        g.fill(leftPos+167,topPos+105,leftPos+167+progress,topPos+113,RED);
        int energy = menu.energyCapacity() <= 0 ? 0 : (int)(98L * menu.energy() / menu.energyCapacity());
        g.fill(leftPos+166,topPos+120,leftPos+266,topPos+130,0xFF160606);
        g.fill(leftPos+167,topPos+121,leftPos+167+energy,topPos+129,0xFFFF4A28);
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h) {
        g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,0xFF170C0C);
        g.fill(leftPos+x+1,topPos+y+1,leftPos+x+w-1,topPos+y+h-1,0xFF2A1717);
        g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+1,RED);
    }
    private void slot(GuiGraphics g,int x,int y,int accent) {
        g.fill(leftPos+x-1,topPos+y-1,leftPos+x+19,topPos+y+19,0xFF090909);
        g.fill(leftPos+x,topPos+y,leftPos+x+18,topPos+y+18,0xFFB8B8B8);
        g.fill(leftPos+x,topPos+y+17,leftPos+x+18,topPos+y+18,accent);
    }
    @Override protected void renderLabels(@NotNull GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,8,7,0xFF8A0000,false);
        g.drawString(font,Component.translatable("gui.metatech_reborn.dragon.injectors"),8,156,0xFF7A1515,false);
        g.drawString(font,Component.translatable("gui.metatech_reborn.dragon.inputs"),72,24,0xFFEABBBB,false);
        g.drawString(font,Component.translatable("gui.metatech_reborn.dragon.pattern_bank"),302,24,0xFFEABBBB,false);
        g.drawString(font,Component.translatable("gui.metatech_reborn.dragon.machine_tier",
                tier(menu.machineTier())),64,142,0xFF8A1515,false);
        if(menu.recipeTier()>=0) g.drawString(font,Component.translatable("gui.metatech_reborn.dragon.recipe_tier",
                tier(menu.recipeTier())),174,142,0xFF8A1515,false);
        g.drawString(font,status(),166,88,statusColor(),false);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF555555,false);
    }
    private Component tier(int tier) { return Component.translatable("gui.metatech_reborn.dragon.tier."+DragonFusionSupport.tierKey(tier)); }
    private Component status() { return Component.translatable("gui.metatech_reborn.dragon.status."+switch(menu.status()) {
        case ExtremeDragonAssemblerBlockEntity.STATUS_MISSING_INJECTORS -> "injectors";
        case ExtremeDragonAssemblerBlockEntity.STATUS_TIER_LOW -> "tier";
        case ExtremeDragonAssemblerBlockEntity.STATUS_MISSING_INPUT -> "input";
        case ExtremeDragonAssemblerBlockEntity.STATUS_NO_ENERGY -> "energy";
        case ExtremeDragonAssemblerBlockEntity.STATUS_OUTPUT_FULL -> "output";
        case ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING -> "running";
        default -> "idle";
    }); }
    private int statusColor() { return menu.status()==ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING ? 0xFF4DD97A : RED; }
    private void machineTooltip(GuiGraphics g,int mouseX,int mouseY) {
        if(inside(mouseX,mouseY,166,104,100,10)) g.renderTooltip(font,Component.translatable(
                "gui.metatech_reborn.tooltip.progress_ticks",menu.progress(),menu.maxProgress()),mouseX,mouseY);
        else if(inside(mouseX,mouseY,166,120,100,10)) g.renderTooltip(font,Component.translatable(
                "gui.metatech_reborn.tooltip.energy",menu.energy(),menu.energyCapacity()),mouseX,mouseY);
    }
    private boolean inside(int mx,int my,int x,int y,int w,int h) {
        return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h;
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/network/DragonEncoderRecipePacket.java", r'''
package ru.rfvv.metatechreborn.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.rfvv.metatechreborn.menu.DragonPatternEncoderMenu;

import java.util.function.Supplier;

public record DragonEncoderRecipePacket(ResourceLocation recipeId) {
    public static void encode(DragonEncoderRecipePacket packet, FriendlyByteBuf buffer) { buffer.writeResourceLocation(packet.recipeId); }
    public static DragonEncoderRecipePacket decode(FriendlyByteBuf buffer) { return new DragonEncoderRecipePacket(buffer.readResourceLocation()); }
    public static void handle(DragonEncoderRecipePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof DragonPatternEncoderMenu menu) menu.applyRecipe(packet.recipeId);
        });
        context.setPacketHandled(true);
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/jei/DragonFusionRecipeCategory.java", r'''
package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.registry.ModItems;

public final class DragonFusionRecipeCategory implements IRecipeCategory<DragonFusionSupport.View> {
    public static final RecipeType<DragonFusionSupport.View> TYPE = RecipeType.create(
            MetaTechReborn.MOD_ID, "extreme_dragon_fusion", DragonFusionSupport.View.class);
    private final IDrawable background;
    private final IDrawable icon;
    public DragonFusionRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(190, 104);
        icon = helper.createDrawableItemStack(new net.minecraft.world.item.ItemStack(ModItems.EXTREME_DRAGON_ASSEMBLER.get()));
    }
    @Override public @NotNull RecipeType<DragonFusionSupport.View> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("jei.metatech_reborn.extreme_dragon_fusion"); }
    @Override public @NotNull IDrawable getBackground() { return background; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull DragonFusionSupport.View recipe,
                                   @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 7, 39).addIngredients(recipe.catalyst());
        for (int i=0;i<recipe.ingredients().size();i++)
            builder.addSlot(RecipeIngredientRole.INPUT, 38+(i%4)*20, 19+(i/4)*20).addIngredients(recipe.ingredients().get(i));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 164, 39).addItemStack(recipe.output());
    }

    @Override public void draw(@NotNull DragonFusionSupport.View recipe, @NotNull IRecipeSlotsView slots,
                               @NotNull GuiGraphics g, double mouseX, double mouseY) {
        g.fill(0,0,190,104,0xFF241010);
        g.fill(1,1,189,103,0xFF361818);
        g.fill(0,0,190,2,0xFFE33434);
        g.drawString(net.minecraft.client.Minecraft.getInstance().font,
                Component.translatable("gui.metatech_reborn.dragon.recipe_tier",
                        Component.translatable("gui.metatech_reborn.dragon.tier."+DragonFusionSupport.tierKey(recipe.tier()))),
                6,82,0xFFFF7777,false);
        g.drawString(net.minecraft.client.Minecraft.getInstance().font,
                Component.translatable("jei.metatech_reborn.dragon.energy", recipe.energy()),6,93,0xFFFFB1A1,false);
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/jei/DragonPatternEncoderTransferHandler.java", r'''
package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.menu.DragonPatternEncoderMenu;
import ru.rfvv.metatechreborn.network.DragonEncoderRecipePacket;
import ru.rfvv.metatechreborn.network.ModNetwork;

import java.util.Optional;

public final class DragonPatternEncoderTransferHandler implements IRecipeTransferHandler<DragonPatternEncoderMenu, DragonFusionSupport.View> {
    private final MenuType<DragonPatternEncoderMenu> menuType;
    private final IRecipeTransferHandlerHelper helper;
    public DragonPatternEncoderTransferHandler(MenuType<DragonPatternEncoderMenu> menuType,
                                               IRecipeTransferHandlerHelper helper) {
        this.menuType=menuType; this.helper=helper;
    }
    @Override public @NotNull Class<? extends DragonPatternEncoderMenu> getContainerClass() { return DragonPatternEncoderMenu.class; }
    @Override public @NotNull Optional<MenuType<DragonPatternEncoderMenu>> getMenuType() { return Optional.of(menuType); }
    @Override public @NotNull mezz.jei.api.recipe.RecipeType<DragonFusionSupport.View> getRecipeType() { return DragonFusionRecipeCategory.TYPE; }
    @Override public @Nullable IRecipeTransferError transferRecipe(@NotNull DragonPatternEncoderMenu menu,
            @NotNull DragonFusionSupport.View recipe, @NotNull IRecipeSlotsView recipeSlots,
            @NotNull Player player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) ModNetwork.CHANNEL.sendToServer(new DragonEncoderRecipePacket(recipe.id()));
        return null;
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/jei/DragonJeiPlugin.java", r'''
package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModMenus;

@JeiPlugin
public final class DragonJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(MetaTechReborn.MOD_ID, "dragon_jei_plugin");
    @Override public @NotNull ResourceLocation getPluginUid() { return UID; }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new DragonFusionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded("draconicevolution") || Minecraft.getInstance().level == null) return;
        registration.addRecipes(DragonFusionRecipeCategory.TYPE, DragonFusionSupport.all(Minecraft.getInstance().level));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("draconicevolution")) return;
        registration.addRecipeCatalyst(ModItems.EXTREME_DRAGON_ASSEMBLER.get(), DragonFusionRecipeCategory.TYPE);
        registration.addRecipeCatalyst(ModItems.DRAGON_PATTERN_ENCODER.get(), DragonFusionRecipeCategory.TYPE);
    }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new DragonPatternEncoderTransferHandler(
                ModMenus.DRAGON_PATTERN_ENCODER.get(), registration.getTransferHelper()), DragonFusionRecipeCategory.TYPE);
    }
}
''')

# Registry and lifecycle patches.
patch("src/main/java/ru/rfvv/metatechreborn/registry/ModBlocks.java",
'''import ru.rfvv.metatechreborn.block.ExtremePatternEncoderBlock;\n''',
'''import ru.rfvv.metatechreborn.block.ExtremePatternEncoderBlock;\nimport ru.rfvv.metatechreborn.block.DragonPatternEncoderBlock;\nimport ru.rfvv.metatechreborn.block.ExtremeDragonAssemblerBlock;\n''', "ModBlocks imports")
patch("src/main/java/ru/rfvv/metatechreborn/registry/ModBlocks.java",
'''    public static final RegistryObject<Block> EXTREME_PATTERN_ENCODER = BLOCKS.register("extreme_pattern_encoder",\n            () -> new ExtremePatternEncoderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)\n                    .strength(5.0F, 12.0F).requiresCorrectToolForDrops()));\n''',
'''    public static final RegistryObject<Block> EXTREME_PATTERN_ENCODER = BLOCKS.register("extreme_pattern_encoder",\n            () -> new ExtremePatternEncoderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)\n                    .strength(5.0F, 12.0F).requiresCorrectToolForDrops()));\n    public static final RegistryObject<Block> DRAGON_PATTERN_ENCODER = BLOCKS.register("dragon_pattern_encoder",\n            () -> new DragonPatternEncoderBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)\n                    .strength(7.0F, 18.0F).requiresCorrectToolForDrops()));\n    public static final RegistryObject<Block> EXTREME_DRAGON_ASSEMBLER = BLOCKS.register("extreme_dragon_assembler",\n            () -> new ExtremeDragonAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)\n                    .strength(8.0F, 24.0F).requiresCorrectToolForDrops()));\n''', "ModBlocks registrations")

patch("src/main/java/ru/rfvv/metatechreborn/registry/ModItems.java",
'''import ru.rfvv.metatechreborn.item.BlankExtremePatternItem;\n''',
'''import ru.rfvv.metatechreborn.item.BlankExtremePatternItem;\nimport ru.rfvv.metatechreborn.item.BlankDragonPatternItem;\nimport ru.rfvv.metatechreborn.item.EncodedDragonPatternItem;\n''', "ModItems imports")
patch("src/main/java/ru/rfvv/metatechreborn/registry/ModItems.java",
'''    public static final RegistryObject<Item> MOLECULAR_ASSEMBLER_9X9 = blockItem(\n            "molecular_assembler_9x9", ModBlocks.MOLECULAR_ASSEMBLER_9X9);\n''',
'''    public static final RegistryObject<Item> MOLECULAR_ASSEMBLER_9X9 = blockItem(\n            "molecular_assembler_9x9", ModBlocks.MOLECULAR_ASSEMBLER_9X9);\n    public static final RegistryObject<Item> EXTREME_DRAGON_ASSEMBLER = blockItem(\n            "extreme_dragon_assembler", ModBlocks.EXTREME_DRAGON_ASSEMBLER);\n    public static final RegistryObject<Item> DRAGON_PATTERN_ENCODER = blockItem(\n            "dragon_pattern_encoder", ModBlocks.DRAGON_PATTERN_ENCODER);\n''', "ModItems dragon block items")
patch("src/main/java/ru/rfvv/metatechreborn/registry/ModItems.java",
'''    public static final RegistryObject<Item> ENCODED_EXTREME_PATTERN = ITEMS.register(\n            "encoded_extreme_pattern", EncodedExtremePatternItem::new);\n''',
'''    public static final RegistryObject<Item> ENCODED_EXTREME_PATTERN = ITEMS.register(\n            "encoded_extreme_pattern", EncodedExtremePatternItem::new);\n    public static final RegistryObject<Item> BLANK_DRAGON_PATTERN = ITEMS.register(\n            "blank_dragon_pattern", BlankDragonPatternItem::new);\n    public static final RegistryObject<Item> ENCODED_DRAGON_PATTERN = ITEMS.register(\n            "encoded_dragon_pattern", EncodedDragonPatternItem::new);\n''', "ModItems dragon patterns")

patch("src/main/java/ru/rfvv/metatechreborn/registry/ModBlockEntities.java",
'''import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;\n''',
'''import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;\nimport ru.rfvv.metatechreborn.blockentity.DragonPatternEncoderBlockEntity;\nimport ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;\n''', "ModBlockEntities imports")
patch("src/main/java/ru/rfvv/metatechreborn/registry/ModBlockEntities.java",
'''    public static final RegistryObject<BlockEntityType<ExtremePatternEncoderBlockEntity>> EXTREME_PATTERN_ENCODER =\n            BLOCK_ENTITIES.register("extreme_pattern_encoder", () -> BlockEntityType.Builder.of(\n                    ExtremePatternEncoderBlockEntity::new, ModBlocks.EXTREME_PATTERN_ENCODER.get()).build(null));\n''',
'''    public static final RegistryObject<BlockEntityType<ExtremePatternEncoderBlockEntity>> EXTREME_PATTERN_ENCODER =\n            BLOCK_ENTITIES.register("extreme_pattern_encoder", () -> BlockEntityType.Builder.of(\n                    ExtremePatternEncoderBlockEntity::new, ModBlocks.EXTREME_PATTERN_ENCODER.get()).build(null));\n    public static final RegistryObject<BlockEntityType<DragonPatternEncoderBlockEntity>> DRAGON_PATTERN_ENCODER =\n            BLOCK_ENTITIES.register("dragon_pattern_encoder", () -> BlockEntityType.Builder.of(\n                    DragonPatternEncoderBlockEntity::new, ModBlocks.DRAGON_PATTERN_ENCODER.get()).build(null));\n    public static final RegistryObject<BlockEntityType<ExtremeDragonAssemblerBlockEntity>> EXTREME_DRAGON_ASSEMBLER =\n            BLOCK_ENTITIES.register("extreme_dragon_assembler", () -> BlockEntityType.Builder.of(\n                    ExtremeDragonAssemblerBlockEntity::new, ModBlocks.EXTREME_DRAGON_ASSEMBLER.get()).build(null));\n''', "ModBlockEntities registrations")

patch("src/main/java/ru/rfvv/metatechreborn/registry/ModMenus.java",
'''import ru.rfvv.metatechreborn.menu.ExtremePatternEncoderMenu;\n''',
'''import ru.rfvv.metatechreborn.menu.ExtremePatternEncoderMenu;\nimport ru.rfvv.metatechreborn.menu.DragonPatternEncoderMenu;\nimport ru.rfvv.metatechreborn.menu.ExtremeDragonAssemblerMenu;\n''', "ModMenus imports")
patch("src/main/java/ru/rfvv/metatechreborn/registry/ModMenus.java",
'''    public static final RegistryObject<MenuType<ExtremePatternEncoderMenu>> EXTREME_PATTERN_ENCODER =\n            MENUS.register("extreme_pattern_encoder", () -> IForgeMenuType.create(ExtremePatternEncoderMenu::new));\n''',
'''    public static final RegistryObject<MenuType<ExtremePatternEncoderMenu>> EXTREME_PATTERN_ENCODER =\n            MENUS.register("extreme_pattern_encoder", () -> IForgeMenuType.create(ExtremePatternEncoderMenu::new));\n    public static final RegistryObject<MenuType<DragonPatternEncoderMenu>> DRAGON_PATTERN_ENCODER =\n            MENUS.register("dragon_pattern_encoder", () -> IForgeMenuType.create(DragonPatternEncoderMenu::new));\n    public static final RegistryObject<MenuType<ExtremeDragonAssemblerMenu>> EXTREME_DRAGON_ASSEMBLER =\n            MENUS.register("extreme_dragon_assembler", () -> IForgeMenuType.create(ExtremeDragonAssemblerMenu::new));\n''', "ModMenus registrations")

patch("src/main/java/ru/rfvv/metatechreborn/client/ClientModEvents.java",
'''import ru.rfvv.metatechreborn.client.screen.ExtremePatternEncoderScreen;\n''',
'''import ru.rfvv.metatechreborn.client.screen.ExtremePatternEncoderScreen;\nimport ru.rfvv.metatechreborn.client.screen.DragonPatternEncoderScreen;\nimport ru.rfvv.metatechreborn.client.screen.ExtremeDragonAssemblerScreen;\n''', "client imports")
patch("src/main/java/ru/rfvv/metatechreborn/client/ClientModEvents.java",
'''            MenuScreens.register(ModMenus.EXTREME_PATTERN_ENCODER.get(), ExtremePatternEncoderScreen::new);\n''',
'''            MenuScreens.register(ModMenus.EXTREME_PATTERN_ENCODER.get(), ExtremePatternEncoderScreen::new);\n            MenuScreens.register(ModMenus.DRAGON_PATTERN_ENCODER.get(), DragonPatternEncoderScreen::new);\n            MenuScreens.register(ModMenus.EXTREME_DRAGON_ASSEMBLER.get(), ExtremeDragonAssemblerScreen::new);\n''', "client screens")
patch("src/main/java/ru/rfvv/metatechreborn/client/ClientModEvents.java",
'''            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MOLECULAR_ASSEMBLER_9X9.get(), RenderType.translucent());\n''',
'''            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MOLECULAR_ASSEMBLER_9X9.get(), RenderType.translucent());\n            ItemBlockRenderTypes.setRenderLayer(ModBlocks.EXTREME_DRAGON_ASSEMBLER.get(), RenderType.cutout());\n            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DRAGON_PATTERN_ENCODER.get(), RenderType.cutout());\n''', "client render layers")

patch("src/main/java/ru/rfvv/metatechreborn/MetaTechReborn.java",
'''            event.accept(ModItems.MOLECULAR_ASSEMBLER_9X9.get());\n''',
'''            event.accept(ModItems.MOLECULAR_ASSEMBLER_9X9.get());\n            if (ModList.get().isLoaded("draconicevolution")) {\n                event.accept(ModItems.EXTREME_DRAGON_ASSEMBLER.get());\n                event.accept(ModItems.DRAGON_PATTERN_ENCODER.get());\n                event.accept(ModItems.BLANK_DRAGON_PATTERN.get());\n                event.accept(ModItems.ENCODED_DRAGON_PATTERN.get());\n            }\n''', "creative dragon items")

patch("src/main/java/ru/rfvv/metatechreborn/network/ModNetwork.java",
'''        CHANNEL.registerMessage(\n                0,\n                EncoderGhostRecipePacket.class,\n                EncoderGhostRecipePacket::encode,\n                EncoderGhostRecipePacket::decode,\n                EncoderGhostRecipePacket::handle);\n''',
'''        CHANNEL.registerMessage(\n                0,\n                EncoderGhostRecipePacket.class,\n                EncoderGhostRecipePacket::encode,\n                EncoderGhostRecipePacket::decode,\n                EncoderGhostRecipePacket::handle);\n        CHANNEL.registerMessage(\n                1,\n                DragonEncoderRecipePacket.class,\n                DragonEncoderRecipePacket::encode,\n                DragonEncoderRecipePacket::decode,\n                DragonEncoderRecipePacket::handle);\n''', "dragon network packet")

print("Applied 0.6.94 extreme Draconic fusion crafting source patch")
