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
        this(id, inventory,
                (ExtremeDragonAssemblerBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(7));
    }

    public ExtremeDragonAssemblerMenu(int id, Inventory inventory,
                                      ExtremeDragonAssemblerBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.EXTREME_DRAGON_ASSEMBLER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        // 12 injectors: a compact 2 x 6 column on the left.
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.INJECTOR_COUNT; i++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.INJECTOR_START + i,
                    12 + (i % 2) * 20, 34 + (i / 2) * 20));
        }

        // Fusion ingredients: 4 x 3, exactly mirroring the JEI category.
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.INGREDIENT_COUNT; i++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.INGREDIENT_START + i,
                    82 + (i % 4) * 20, 40 + (i / 4) * 20));
        }
        addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.CATALYST_SLOT, 174, 60));
        addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.OUTPUT_SLOT, 222, 60) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        // 36-pattern bank, 9 x 4.
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.PATTERN_COUNT; i++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.PATTERN_START + i,
                    258 + (i % 9) * 18, 34 + (i / 9) * 18));
        }

        // Player inventory centred below the machine, no huge dead area.
        int inventoryX = 82;
        int inventoryY = 178;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9,
                        inventoryX + col * 18, inventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, inventoryX + col * 18, inventoryY + 58));
        }
        addDataSlots(data);
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos()), player,
                ModBlocks.EXTREME_DRAGON_ASSEMBLER.get());
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (DragonFusionSupport.isInjector(stack)) {
            if (!moveItemStackTo(stack, ExtremeDragonAssemblerBlockEntity.INJECTOR_START,
                    ExtremeDragonAssemblerBlockEntity.INJECTOR_START + ExtremeDragonAssemblerBlockEntity.INJECTOR_COUNT,
                    false)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.ENCODED_DRAGON_PATTERN.get())) {
            if (!moveItemStackTo(stack, ExtremeDragonAssemblerBlockEntity.PATTERN_START,
                    ExtremeDragonAssemblerBlockEntity.PATTERN_START + ExtremeDragonAssemblerBlockEntity.PATTERN_COUNT,
                    false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, ExtremeDragonAssemblerBlockEntity.INGREDIENT_START,
                ExtremeDragonAssemblerBlockEntity.CATALYST_SLOT + 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
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

write("src/main/java/ru/rfvv/metatechreborn/client/screen/ExtremeDragonAssemblerScreen.java", r'''
package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.menu.ExtremeDragonAssemblerMenu;

public final class ExtremeDragonAssemblerScreen extends AbstractContainerScreen<ExtremeDragonAssemblerMenu> {
    private static final int BG = 0xFFE9E9E9;
    private static final int PANEL = 0xFF261010;
    private static final int PANEL_2 = 0xFF351515;
    private static final int RED = 0xFFE33A3A;
    private static final int RED_DARK = 0xFF781B1B;
    private static final int TEXT = 0xFFF5DADA;
    private static final int SUBTEXT = 0xFFFFA2A2;

    public ExtremeDragonAssemblerScreen(ExtremeDragonAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 430;
        imageHeight = 276;
        titleLabelX = 10;
        titleLabelY = 7;
        inventoryLabelX = 82;
        inventoryLabelY = 166;
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        machineTooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);

        // Three compact zones: injectors / recipe / pattern bank.
        panel(g, 6, 22, 50, 136, PANEL);
        panel(g, 64, 22, 184, 136, PANEL);
        panel(g, 252, 22, 172, 92, PANEL);
        panel(g, 64, 162, 184, 108, PANEL_2);

        for (int i = 0; i < 12; i++) slot(g, 10 + (i % 2) * 20, 32 + (i / 2) * 20, RED_DARK);
        for (int i = 0; i < 12; i++) slot(g, 80 + (i % 4) * 20, 38 + (i / 4) * 20, RED);
        slot(g, 172, 58, 0xFFFF7D52);
        slot(g, 220, 58, RED);
        for (int i = 0; i < 36; i++) slot(g, 256 + (i % 9) * 18, 32 + (i / 9) * 18, 0xFFAA2828);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            slot(g, 80 + col * 18, 176 + row * 18, 0xFF616161);
        for (int col = 0; col < 9; col++) slot(g, 80 + col * 18, 234, 0xFF616161);

        // Progress and FE are visually parallel and do not overlap labels.
        int progressPx = menu.maxProgress() <= 0 ? 0 : (int) (152L * menu.progress() / menu.maxProgress());
        g.fill(leftPos + 80, topPos + 112, leftPos + 234, topPos + 120, 0xFF120505);
        if (progressPx > 0) g.fill(leftPos + 81, topPos + 113, leftPos + 81 + progressPx, topPos + 119, RED);

        long cap = Math.max(1L, Integer.toUnsignedLong(menu.energyCapacity()));
        long stored = Math.max(0L, Integer.toUnsignedLong(menu.energy()));
        int energyPx = (int) Math.min(152L, 152L * stored / cap);
        g.fill(leftPos + 80, topPos + 126, leftPos + 234, topPos + 134, 0xFF120505);
        if (energyPx > 0) g.fill(leftPos + 81, topPos + 127, leftPos + 81 + energyPx, topPos + 133, 0xFFFF4A32);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFF8E0C0C, false);
        g.drawString(font, Component.literal("12 инжекторов"), 7, 148, 0xFF7A1111, false);
        g.drawString(font, Component.literal("Fusion-рецепт"), 72, 27, TEXT, false);
        g.drawString(font, Component.literal("Кат."), 168, 82, SUBTEXT, false);
        g.drawString(font, Component.literal("→"), 202, 63, SUBTEXT, false);
        g.drawString(font, Component.literal("Банк шаблонов"), 260, 27, TEXT, false);

        int tier = menu.machineTier();
        Component tierText = tier < 0
                ? Component.literal("Нет полного набора инжекторов")
                : Component.translatable("gui.metatech_reborn.dragon.tier." + DragonFusionSupport.tierKey(tier));
        g.drawString(font, Component.literal("Уровень: ").append(tierText), 80, 96,
                tier < 0 ? 0xFFFF6868 : 0xFFFFB0A6, false);

        Component status = statusText();
        g.drawString(font, status, 80, 140, statusColor(), false);
        g.drawString(font, inventory.getDisplayName(), inventoryLabelX, inventoryLabelY, 0xFF6D1414, false);
    }

    private Component statusText() {
        return switch (menu.status()) {
            case ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING -> Component.literal("Крафт выполняется");
            case ExtremeDragonAssemblerBlockEntity.STATUS_TIER_LOW -> Component.literal("Недостаточный уровень инжекторов");
            case ExtremeDragonAssemblerBlockEntity.STATUS_MISSING_INPUT -> Component.literal("Ожидание ингредиентов");
            case ExtremeDragonAssemblerBlockEntity.STATUS_NO_ENERGY -> Component.literal("Недостаточно энергии");
            case ExtremeDragonAssemblerBlockEntity.STATUS_OUTPUT_FULL -> Component.literal("Выход занят");
            default -> Component.literal("Готов к заданию");
        };
    }

    private int statusColor() {
        return menu.status() == ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING ? 0xFFFFC0B4 : 0xFFFF6A5A;
    }

    private void machineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 80, 126, 154, 8)) {
            g.renderTooltip(font, Component.literal("Энергия: "
                    + Integer.toUnsignedLong(menu.energy()) + " / "
                    + Integer.toUnsignedLong(menu.energyCapacity()) + " FE"), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 80, 112, 154, 8)) {
            g.renderTooltip(font, Component.literal("Прогресс: " + menu.progress() + " / " + menu.maxProgress()),
                    mouseX, mouseY);
        }
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w
                && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, 0xFF7D1B1B);
        g.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + w - 2, topPos + y + h - 2, color);
    }

    private void slot(GuiGraphics g, int x, int y, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, color);
        g.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + 16, topPos + y + 16, 0xFFBFBFBF);
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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
        this.background = helper.createBlankDrawable(184, 104);
        this.icon = helper.createDrawableItemLike(ModItems.EXTREME_DRAGON_ASSEMBLER.get());
    }

    @Override public @NotNull RecipeType<DragonFusionSupport.View> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() {
        return Component.translatable("jei.metatech_reborn.dragon_fusion");
    }
    @Override public @NotNull IDrawable getBackground() { return background; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull DragonFusionSupport.View recipe,
                          @NotNull IFocusGroup focuses) {
        // 12 injector ingredients in the same 4x3 order as the machine.
        for (int i = 0; i < recipe.ingredients().size() && i < 12; i++) {
            Ingredient ingredient = recipe.ingredients().get(i);
            int x = 4 + (i % 4) * 18;
            int y = 5 + (i / 4) * 18;
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .setStandardSlotBackground()
                    .addIngredients(ingredient);
            int count = DragonFusionSupport.requiredCount(ingredient);
            if (count > 1) {
                ItemStack display = DragonFusionSupport.displayStack(ingredient);
                if (!display.isEmpty()) slot.addItemStack(display);
            }
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 91, 23)
                .setStandardSlotBackground()
                .addIngredients(recipe.catalyst());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 154, 23)
                .setOutputSlotBackground()
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(@NotNull DragonFusionSupport.View recipe, @NotNull IRecipeSlotsView slots,
                     @NotNull GuiGraphics g, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.literal("+"), 79, 28, 0xFFB62020, false);
        g.drawString(font, Component.literal("→"), 126, 28, 0xFFB62020, false);
        g.drawString(font,
                Component.literal("Тир: ").append(Component.translatable(
                        "gui.metatech_reborn.dragon.tier." + DragonFusionSupport.tierKey(recipe.tier()))),
                4, 66, 0xFF8D1111, false);
        g.drawString(font, Component.literal("Энергия: " + recipe.energy() + " FE"),
                4, 80, 0xFF8D1111, false);
        g.drawString(font, Component.literal("Ингредиентов: " + recipe.ingredients().size() + " / 12"),
                4, 94, 0xFF555555, false);
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/integration/ae2/ExtremeDragonAssemblerAe2Provider.java", r'''
package ru.rfvv.metatechreborn.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.capabilities.Capabilities;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;
import ru.rfvv.metatechreborn.integration.dragon.DragonFusionSupport;
import ru.rfvv.metatechreborn.item.EncodedDragonPatternItem;
import ru.rfvv.metatechreborn.pattern.DragonFusionPatternData;
import ru.rfvv.metatechreborn.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Publishes the dragon pattern bank to AE2 and accepts crafting jobs from the ME network. */
public final class ExtremeDragonAssemblerAe2Provider implements
        ICapabilitySerializable<CompoundTag>, IInWorldGridNodeHost, ICraftingProvider, IActionHost {

    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(MetaTechReborn.MOD_ID, "extreme_dragon_assembler_ae2");
    private static final Set<ExtremeDragonAssemblerAe2Provider> PROVIDERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean registered;

    private final ExtremeDragonAssemblerBlockEntity host;
    private final LazyOptional<IInWorldGridNodeHost> nodeHostCapability = LazyOptional.of(() -> this);
    private final IGridNodeListener<ExtremeDragonAssemblerAe2Provider> listener = new IGridNodeListener<>() {
        @Override public void onSaveChanges(ExtremeDragonAssemblerAe2Provider owner, IGridNode node) {
            owner.host.setChanged();
        }
    };

    private IManagedGridNode managedNode;
    private CompoundTag pendingNodeTag;
    private List<IPatternDetails> cachedPatterns = List.of();
    private List<AEItemKey> cachedDefinitions = List.of();
    private long lastRefresh = Long.MIN_VALUE;
    private boolean invalid;

    private ExtremeDragonAssemblerAe2Provider(ExtremeDragonAssemblerBlockEntity host) {
        this.host = host;
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    private static final class Events {
        @SubscribeEvent
        public void attachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
            if (!(event.getObject() instanceof ExtremeDragonAssemblerBlockEntity assembler)) return;
            ExtremeDragonAssemblerAe2Provider provider = new ExtremeDragonAssemblerAe2Provider(assembler);
            synchronized (PROVIDERS) { PROVIDERS.add(provider); }
            event.addCapability(CAPABILITY_ID, provider);
            event.addListener(provider::invalidate);
        }

        @SubscribeEvent
        public void levelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
            List<ExtremeDragonAssemblerAe2Provider> copy;
            synchronized (PROVIDERS) { copy = new ArrayList<>(PROVIDERS); }
            for (ExtremeDragonAssemblerAe2Provider provider : copy) provider.tick(event.level);
        }
    }

    private void tick(Level level) {
        if (invalid || host.isRemoved() || host.getLevel() != level) return;
        ensureNode();
        if (managedNode == null) return;
        long time = level.getGameTime();
        if (time - lastRefresh >= 20L) {
            lastRefresh = time;
            refreshPatterns(true);
        }
        returnOutputToNetwork();
    }

    private void ensureNode() {
        if (invalid || managedNode != null) return;
        Level level = host.getLevel();
        if (level == null || level.isClientSide) return;
        managedNode = GridHelper.createManagedNode(this, listener)
                .setInWorldNode(true)
                .setVisualRepresentation(ModItems.EXTREME_DRAGON_ASSEMBLER.get())
                .setIdlePowerUsage(2.0D)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .addService(ICraftingProvider.class, this);
        if (pendingNodeTag != null && !pendingNodeTag.isEmpty()) {
            managedNode.loadFromNBT(pendingNodeTag);
            pendingNodeTag = null;
        }
        managedNode.create(level, host.getBlockPos());
        refreshPatterns(false);
    }

    private void refreshPatterns(boolean notify) {
        Level level = host.getLevel();
        if (level == null) return;
        List<IPatternDetails> patterns = new ArrayList<>();
        List<AEItemKey> definitions = new ArrayList<>();
        for (int slot = ExtremeDragonAssemblerBlockEntity.PATTERN_START;
             slot < ExtremeDragonAssemblerBlockEntity.PATTERN_START + ExtremeDragonAssemblerBlockEntity.PATTERN_COUNT;
             slot++) {
            ItemStack patternStack = host.getItems().getStackInSlot(slot);
            DragonFusionPatternData data = EncodedDragonPatternItem.read(patternStack).orElse(null);
            if (data == null) continue;
            DragonFusionSupport.View view = DragonFusionSupport.find(level, data.recipeId()).orElse(null);
            if (view == null) continue;
            ItemStack definitionStack = patternStack.copy();
            definitionStack.setCount(1);
            AEItemKey definition = AEItemKey.of(definitionStack);
            if (definition == null) continue;
            patterns.add(new DragonPatternDetails(definition, view));
            definitions.add(definition);
        }
        if (definitions.equals(cachedDefinitions)) return;
        cachedDefinitions = List.copyOf(definitions);
        cachedPatterns = List.copyOf(patterns);
        if (notify && managedNode != null && managedNode.isReady()) ICraftingProvider.requestUpdate(managedNode);
    }

    @Override public List<IPatternDetails> getAvailablePatterns() {
        refreshPatterns(false);
        return cachedPatterns;
    }

    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputHolder) {
        if (pattern == null || isBusy()) return false;
        refreshPatterns(false);
        DragonPatternDetails selected = null;
        for (IPatternDetails candidate : cachedPatterns) {
            if (candidate instanceof DragonPatternDetails details
                    && details.getDefinition().equals(pattern.getDefinition())) {
                selected = details;
                break;
            }
        }
        if (selected == null) return false;

        int machineTier = machineTier();
        if (machineTier < selected.view.tier()) return false;

        Map<AEItemKey, Long> supplied = collectInputs(inputHolder);
        if (supplied == null) return false;

        ItemStack catalyst = take(selected.view.catalyst(), supplied);
        if (catalyst == null) return false;
        List<ItemStack> ingredientStacks = new ArrayList<>();
        for (Ingredient ingredient : selected.view.ingredients()) {
            ItemStack taken = take(ingredient, supplied);
            if (taken == null) return false;
            ingredientStacks.add(taken);
        }

        var items = host.getItems();
        items.setStackInSlot(ExtremeDragonAssemblerBlockEntity.CATALYST_SLOT, catalyst);
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.INGREDIENT_COUNT; i++) {
            items.setStackInSlot(ExtremeDragonAssemblerBlockEntity.INGREDIENT_START + i,
                    i < ingredientStacks.size() ? ingredientStacks.get(i) : ItemStack.EMPTY);
        }
        host.setChanged();
        for (KeyCounter counter : inputHolder) counter.clear();
        return true;
    }

    private int machineTier() {
        int tier = Integer.MAX_VALUE;
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.INJECTOR_COUNT; i++) {
            int current = DragonFusionSupport.injectorTier(
                    host.getItems().getStackInSlot(ExtremeDragonAssemblerBlockEntity.INJECTOR_START + i));
            if (current < 0) return -1;
            tier = Math.min(tier, current);
        }
        return tier == Integer.MAX_VALUE ? -1 : tier;
    }

    private static @Nullable Map<AEItemKey, Long> collectInputs(KeyCounter[] holders) {
        Map<AEItemKey, Long> result = new LinkedHashMap<>();
        for (KeyCounter counter : holders) {
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey)) return null;
                result.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        return result;
    }

    private static @Nullable ItemStack take(Ingredient ingredient, Map<AEItemKey, Long> supplied) {
        int count = DragonFusionSupport.requiredCount(ingredient);
        for (Map.Entry<AEItemKey, Long> entry : supplied.entrySet()) {
            if (entry.getValue() < count) continue;
            ItemStack test = entry.getKey().toStack(1);
            if (!ingredient.test(test)) continue;
            entry.setValue(entry.getValue() - count);
            return entry.getKey().toStack(count);
        }
        return null;
    }

    @Override public boolean isBusy() {
        var items = host.getItems();
        if (!items.getStackInSlot(ExtremeDragonAssemblerBlockEntity.OUTPUT_SLOT).isEmpty()) return true;
        if (!items.getStackInSlot(ExtremeDragonAssemblerBlockEntity.CATALYST_SLOT).isEmpty()) return true;
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.INGREDIENT_COUNT; i++) {
            if (!items.getStackInSlot(ExtremeDragonAssemblerBlockEntity.INGREDIENT_START + i).isEmpty()) return true;
        }
        return false;
    }

    private void returnOutputToNetwork() {
        if (managedNode == null || !managedNode.isActive()) return;
        ItemStack output = host.getItems().getStackInSlot(ExtremeDragonAssemblerBlockEntity.OUTPUT_SLOT);
        if (output.isEmpty()) return;
        AEItemKey key = AEItemKey.of(output);
        if (key == null || managedNode.getGrid() == null) return;
        long inserted = managedNode.getGrid().getStorageService().getInventory().insert(
                key, output.getCount(), Actionable.MODULATE, IActionSource.ofMachine(this));
        if (inserted > 0) {
            host.getItems().extractItem(ExtremeDragonAssemblerBlockEntity.OUTPUT_SLOT,
                    (int) Math.min(Integer.MAX_VALUE, inserted), false);
            host.setChanged();
        }
    }

    @Override public @Nullable IGridNode getGridNode(Direction direction) {
        ensureNode();
        return managedNode == null ? null : managedNode.getNode();
    }
    @Override public @Nullable IGridNode getActionableNode() {
        ensureNode();
        return managedNode == null ? null : managedNode.getNode();
    }
    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                                @Nullable Direction side) {
        if (capability == Capabilities.IN_WORLD_GRID_NODE_HOST) return nodeHostCapability.cast();
        return LazyOptional.empty();
    }
    @Override public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (managedNode != null) managedNode.saveToNBT(tag);
        else if (pendingNodeTag != null) tag.merge(pendingNodeTag.copy());
        return tag;
    }
    @Override public void deserializeNBT(CompoundTag tag) { pendingNodeTag = tag.copy(); }

    private void invalidate() {
        if (invalid) return;
        invalid = true;
        synchronized (PROVIDERS) { PROVIDERS.remove(this); }
        if (managedNode != null) { managedNode.destroy(); managedNode = null; }
        nodeHostCapability.invalidate();
    }

    private static final class DragonPatternDetails implements IPatternDetails {
        private final AEItemKey definition;
        private final DragonFusionSupport.View view;
        private final IInput[] inputs;
        private final GenericStack[] outputs;

        private DragonPatternDetails(AEItemKey definition, DragonFusionSupport.View view) {
            this.definition = definition;
            this.view = view;
            List<IInput> in = new ArrayList<>();
            in.add(new IngredientInput(view.catalyst()));
            for (Ingredient ingredient : view.ingredients()) in.add(new IngredientInput(ingredient));
            this.inputs = in.toArray(IInput[]::new);
            AEItemKey output = AEItemKey.of(view.output());
            this.outputs = output == null ? new GenericStack[0]
                    : new GenericStack[]{new GenericStack(output, view.output().getCount())};
        }
        @Override public AEItemKey getDefinition() { return definition; }
        @Override public IInput[] getInputs() { return inputs; }
        @Override public GenericStack[] getOutputs() { return outputs; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof DragonPatternDetails details
                    && definition.equals(details.definition);
        }
        @Override public int hashCode() { return definition.hashCode(); }
    }

    private static final class IngredientInput implements IPatternDetails.IInput {
        private final Ingredient ingredient;
        private final long multiplier;
        private final GenericStack[] possible;

        private IngredientInput(Ingredient ingredient) {
            this.ingredient = ingredient;
            this.multiplier = DragonFusionSupport.requiredCount(ingredient);
            List<GenericStack> values = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems()) {
                AEItemKey key = AEItemKey.of(stack);
                if (key != null && values.stream().noneMatch(v -> v.what().equals(key)))
                    values.add(new GenericStack(key, 1));
            }
            this.possible = values.toArray(GenericStack[]::new);
        }
        @Override public GenericStack[] getPossibleInputs() { return possible; }
        @Override public long getMultiplier() { return multiplier; }
        @Override public boolean isValid(AEKey candidate, Level level) {
            return candidate instanceof AEItemKey itemKey && ingredient.test(itemKey.toStack(1));
        }
        @Override public @Nullable AEKey getRemainingKey(AEKey template) { return null; }
    }
}
''')

patch("src/main/java/ru/rfvv/metatechreborn/MetaTechReborn.java",
'''import ru.rfvv.metatechreborn.integration.ae2.MolecularAssemblerAe2Provider;\n''',
'''import ru.rfvv.metatechreborn.integration.ae2.MolecularAssemblerAe2Provider;\nimport ru.rfvv.metatechreborn.integration.ae2.ExtremeDragonAssemblerAe2Provider;\n''',
"dragon AE2 provider import")

patch("src/main/java/ru/rfvv/metatechreborn/MetaTechReborn.java",
'''            MolecularAssemblerAe2Provider.register();\n            MolecularAssemblerAe2ConnectionFix.register();\n''',
'''            MolecularAssemblerAe2Provider.register();\n            MolecularAssemblerAe2ConnectionFix.register();\n            ExtremeDragonAssemblerAe2Provider.register();\n''',
"dragon AE2 provider registration")

print("Applied 0.6.95 dragon GUI, JEI layout and AE2 crafting-provider fixes")
