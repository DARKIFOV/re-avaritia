from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[1]


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(dedent(content).lstrip(), encoding="utf-8")


write("src/main/java/ru/rfvv/metatechreborn/menu/DragonPatternEncoderMenu.java", r'''
package ru.rfvv.metatechreborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
        this(id, inventory,
                (DragonPatternEncoderBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(2));
    }

    public DragonPatternEncoderMenu(int id, Inventory inventory,
                                    DragonPatternEncoderBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.DRAGON_PATTERN_ENCODER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getItems(), DragonPatternEncoderBlockEntity.BLANK_SLOT,
                204, 54));
        addSlot(new SlotItemHandler(blockEntity.getItems(), DragonPatternEncoderBlockEntity.OUTPUT_SLOT,
                260, 54) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        addGhost(blockEntity, DragonPatternEncoderBlockEntity.CATALYST_GHOST_SLOT, 16, 58);
        for (int i = 0; i < DragonPatternEncoderBlockEntity.INGREDIENT_GHOST_COUNT; i++) {
            addGhost(blockEntity, DragonPatternEncoderBlockEntity.INGREDIENT_GHOST_START + i,
                    60 + (i % 4) * 20, 38 + (i / 4) * 20);
        }
        addGhost(blockEntity, DragonPatternEncoderBlockEntity.PREVIEW_OUTPUT_SLOT, 158, 58);

        int inventoryX = 72;
        int inventoryY = 158;
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

    private void addGhost(DragonPatternEncoderBlockEntity blockEntity, int slot, int x, int y) {
        addSlot(new SlotItemHandler(blockEntity.getItems(), slot, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            @Override public boolean mayPickup(@NotNull Player player) { return false; }
        });
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                        blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.DRAGON_PATTERN_ENCODER.get());
    }

    @Override public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == ENCODE_BUTTON) return blockEntity.encode();
        if (id == CLEAR_BUTTON) {
            blockEntity.clearRecipe();
            return true;
        }
        return false;
    }

    public void applyRecipe(ResourceLocation id) { blockEntity.selectRecipe(id); }

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
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }

    public int getStatus() { return data.get(0); }
    public boolean hasRecipe() { return data.get(1) != 0; }
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
    private static final int BG = 0xFFE9E9E9;
    private static final int PANEL = 0xFF2A1111;
    private static final int RED = 0xFFE33A3A;
    private static final int TEXT = 0xFFF3D9D9;

    public DragonPatternEncoderScreen(DragonPatternEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 306;
        imageHeight = 246;
        inventoryLabelX = 72;
        inventoryLabelY = 146;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.dragon_encoder.encode"),
                        b -> click(DragonPatternEncoderMenu.ENCODE_BUTTON))
                .bounds(leftPos + 198, topPos + 88, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.dragon_encoder.clear"),
                        b -> click(DragonPatternEncoderMenu.CLEAR_BUTTON))
                .bounds(leftPos + 198, topPos + 112, 96, 20).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        panel(g, 6, 20, 180, 118);
        panel(g, 190, 20, 110, 118);
        panel(g, 68, 142, 170, 98);

        slot(g, 14, 56, 0xFFB13535);
        for (int i = 0; i < 12; i++) slot(g, 58 + (i % 4) * 20, 36 + (i / 4) * 20, RED);
        slot(g, 156, 56, 0xFFB13535);
        slot(g, 202, 52, RED);
        slot(g, 258, 52, RED);

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            slot(g, 70 + col * 18, 156 + row * 18, 0xFF666666);
        for (int col = 0; col < 9; col++) slot(g, 70 + col * 18, 214, 0xFF666666);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 7, 0xFF8E0C0C, false);
        g.drawString(font, Component.literal("Рецепт"), 62, 24, TEXT, false);
        MetaTechGui.drawWrapped(g, font, statusText(), 198, 30, 94, statusColor(), 3);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF6D1414, false);
    }

    private Component statusText() {
        if (!menu.hasRecipe()) return Component.literal("Выберите рецепт");
        return switch (menu.getStatus()) {
            case DragonPatternEncoderBlockEntity.STATUS_READY -> Component.literal("Готов к кодированию");
            case DragonPatternEncoderBlockEntity.STATUS_ENCODED -> Component.literal("Шаблон закодирован");
            case DragonPatternEncoderBlockEntity.STATUS_NO_BLANK -> Component.literal("Нужен пустой шаблон");
            case DragonPatternEncoderBlockEntity.STATUS_OUTPUT_BLOCKED -> Component.literal("Выход занят");
            case DragonPatternEncoderBlockEntity.STATUS_NO_RECIPE -> Component.literal("Рецепт не выбран");
            default -> Component.literal("Ожидание");
        };
    }

    private int statusColor() {
        return switch (menu.getStatus()) {
            case DragonPatternEncoderBlockEntity.STATUS_READY -> 0xFFFFB0A6;
            case DragonPatternEncoderBlockEntity.STATUS_ENCODED -> 0xFFFF7070;
            case DragonPatternEncoderBlockEntity.STATUS_NO_BLANK,
                 DragonPatternEncoderBlockEntity.STATUS_OUTPUT_BLOCKED,
                 DragonPatternEncoderBlockEntity.STATUS_NO_RECIPE -> 0xFFFF5C5C;
            default -> TEXT;
        };
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, 0xFF7D1B1B);
        g.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + w - 2, topPos + y + h - 2, PANEL);
    }

    private void slot(GuiGraphics g, int x, int y, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, color);
        g.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + 16, topPos + y + 16, 0xFFBFBFBF);
    }
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
        this(id, inventory,
                (ExtremeDragonAssemblerBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(7));
    }

    public ExtremeDragonAssemblerMenu(int id, Inventory inventory,
                                      ExtremeDragonAssemblerBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.EXTREME_DRAGON_ASSEMBLER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.INJECTOR_COUNT; i++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.INJECTOR_START + i,
                    12 + (i % 2) * 20, 36 + (i / 2) * 20));
        }
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.INGREDIENT_COUNT; i++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.INGREDIENT_START + i,
                    68 + (i % 4) * 20, 40 + (i / 4) * 20));
        }
        addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.CATALYST_SLOT, 158, 60));
        addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.OUTPUT_SLOT, 190, 60) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.PATTERN_COUNT; i++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.PATTERN_START + i,
                    224 + (i % 9) * 18, 36 + (i / 9) * 18));
        }

        int inventoryX = 114;
        int inventoryY = 180;
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
    private static final int RED = 0xFFE33A3A;
    private static final int TEXT = 0xFFF5DADA;

    public ExtremeDragonAssemblerScreen(ExtremeDragonAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 390;
        imageHeight = 260;
        inventoryLabelX = 114;
        inventoryLabelY = 168;
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        machineTooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        panel(g, 6, 20, 48, 136);
        panel(g, 58, 20, 158, 136);
        panel(g, 220, 20, 164, 94);
        panel(g, 110, 164, 170, 90);

        for (int i = 0; i < 12; i++) slot(g, 10 + (i % 2) * 20, 34 + (i / 2) * 20, 0xFF781B1B);
        for (int i = 0; i < 12; i++) slot(g, 66 + (i % 4) * 20, 38 + (i / 4) * 20, RED);
        slot(g, 156, 58, 0xFFFF7D52);
        slot(g, 188, 58, RED);
        for (int i = 0; i < 36; i++) slot(g, 222 + (i % 9) * 18, 34 + (i / 9) * 18, 0xFFAA2828);

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            slot(g, 112 + col * 18, 178 + row * 18, 0xFF616161);
        for (int col = 0; col < 9; col++) slot(g, 112 + col * 18, 236, 0xFF616161);

        int progressPx = menu.maxProgress() <= 0 ? 0 : (int) (138L * menu.progress() / menu.maxProgress());
        g.fill(leftPos + 66, topPos + 112, leftPos + 206, topPos + 120, 0xFF120505);
        if (progressPx > 0) g.fill(leftPos + 67, topPos + 113, leftPos + 67 + progressPx, topPos + 119, RED);

        long cap = Math.max(1L, Integer.toUnsignedLong(menu.energyCapacity()));
        long stored = Math.max(0L, Integer.toUnsignedLong(menu.energy()));
        int energyPx = (int) Math.min(138L, 138L * stored / cap);
        g.fill(leftPos + 66, topPos + 126, leftPos + 206, topPos + 134, 0xFF120505);
        if (energyPx > 0) g.fill(leftPos + 67, topPos + 127, leftPos + 67 + energyPx, topPos + 133, 0xFFFF4A32);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 7, 0xFF8E0C0C, false);
        g.drawString(font, Component.literal("Fusion"), 66, 24, TEXT, false);
        g.drawString(font, Component.literal("Шаблоны"), 228, 24, TEXT, false);
        g.drawString(font, Component.literal("→"), 178, 63, 0xFFFFA2A2, false);

        int tier = menu.machineTier();
        Component tierText = tier < 0
                ? Component.literal("Инжекторы не готовы")
                : Component.translatable("gui.metatech_reborn.dragon.tier." + DragonFusionSupport.tierKey(tier));
        MetaTechGui.drawWrapped(g, font,
                tier < 0 ? tierText : Component.literal("Уровень: ").append(tierText),
                66, 94, 140, tier < 0 ? 0xFFFF6868 : 0xFFFFB0A6, 2);
        g.drawString(font, statusText(), 66, 142, statusColor(), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF6D1414, false);
    }

    private Component statusText() {
        return switch (menu.status()) {
            case ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING -> Component.literal("Крафт выполняется");
            case ExtremeDragonAssemblerBlockEntity.STATUS_TIER_LOW -> Component.literal("Низкий уровень");
            case ExtremeDragonAssemblerBlockEntity.STATUS_MISSING_INPUT -> Component.literal("Нужны ингредиенты");
            case ExtremeDragonAssemblerBlockEntity.STATUS_NO_ENERGY -> Component.literal("Нет энергии");
            case ExtremeDragonAssemblerBlockEntity.STATUS_OUTPUT_FULL -> Component.literal("Выход занят");
            default -> Component.literal("Готов");
        };
    }

    private int statusColor() {
        return menu.status() == ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING ? 0xFFFFC0B4 : 0xFFFF6A5A;
    }

    private void machineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 66, 126, 140, 8)) {
            g.renderTooltip(font, Component.literal("Энергия: "
                    + Integer.toUnsignedLong(menu.energy()) + " / "
                    + Integer.toUnsignedLong(menu.energyCapacity()) + " FE"), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 66, 112, 140, 8)) {
            g.renderTooltip(font, Component.literal("Прогресс: " + menu.progress() + " / " + menu.maxProgress()),
                    mouseX, mouseY);
        }
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w
                && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, 0xFF7D1B1B);
        g.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + w - 2, topPos + y + h - 2, PANEL);
    }

    private void slot(GuiGraphics g, int x, int y, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, color);
        g.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + 16, topPos + y + 16, 0xFFBFBFBF);
    }
}
''')
