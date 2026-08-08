from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[1]


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(dedent(content).lstrip(), encoding="utf-8")


def replace_once(path: str, old: str, new: str, label: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


write("src/main/java/ru/rfvv/metatechreborn/client/screen/ManaDrillScreen.java", r'''
package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.menu.ManaDrillMenu;

import java.util.Locale;

public final class ManaDrillScreen extends AbstractContainerScreen<ManaDrillMenu> {
    private static final int TEXT = 0x404040;
    private static final int MANA = 0xFF3A86B8;
    private static final int PROGRESS = 0xFF7653A6;
    private static final int TRACK = 0xFF3D4147;

    public ManaDrillScreen(ManaDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 324;
        imageHeight = 324;
        inventoryLabelX = 81;
        inventoryLabelY = 228;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderMachineTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 112, 196);
        MetaTechGui.panel(g, leftPos + 124, topPos + 20, 194, 176);
        MetaTechGui.panel(g, leftPos + 77, topPos + 234, 170, 84);

        MetaTechGui.slot(g, leftPos + 20, topPos + 34, 0xFF3A9D72);
        MetaTechGui.slot(g, leftPos + 20, topPos + 64, MANA);
        MetaTechGui.slot(g, leftPos + 44, topPos + 64, PROGRESS);
        MetaTechGui.slot(g, leftPos + 68, topPos + 64, 0xFFD89B2B);
        MetaTechGui.grid(g, leftPos + 132, topPos + 30, 9, 9, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 81, topPos + 240, 9, 3, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 81, topPos + 298, 9, 1, 0xFF777777);

        drawHorizontalBar(g, 12, 108, 98, 12, menu.getManaPixels(94), MANA);
        drawHorizontalBar(g, 12, 140, 98, 12, menu.getProgressPixels(94), PROGRESS);
    }

    private void drawHorizontalBar(GuiGraphics g, int x, int y, int width, int height,
                                   int filled, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF202329);
        g.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + width - 1, topPos + y + height - 1, TRACK);
        int clamped = Math.max(0, Math.min(width - 4, filled));
        if (clamped > 0) {
            g.fill(leftPos + x + 2, topPos + y + 2,
                    leftPos + x + 2 + clamped, topPos + y + height - 2, color);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.mana",
                        compact(menu.getMana()), compact(menu.getManaCapacity())),
                12, 96, 0x255C88, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.tooltip.progress_ticks",
                        menu.getProgress(), menu.getMaxProgress()),
                12, 128, 0x5A3D82, false);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable(menu.isStructureFormed()
                        ? "gui.metatech_reborn.structure_formed"
                        : "gui.metatech_reborn.structure_missing"),
                12, 158, 98, menu.isStructureFormed() ? 0x176B45 : 0xA02020, 2);
    }

    private static String compact(int value) {
        long n = Math.max(0L, value);
        if (n >= 1_000_000_000L) return format(n / 1_000_000_000.0, "B");
        if (n >= 1_000_000L) return format(n / 1_000_000.0, "M");
        if (n >= 1_000L) return format(n / 1_000.0, "K");
        return Long.toString(n);
    }

    private static String format(double value, String suffix) {
        double rounded = Math.rint(value);
        return Math.abs(value - rounded) < 0.05
                ? String.format(Locale.ROOT, "%.0f%s", value, suffix)
                : String.format(Locale.ROOT, "%.1f%s", value, suffix);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, 12, 108, 98, 12)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.mana_drill.tooltip.mana",
                    menu.getMana(), menu.getManaCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 12, 140, 98, 12)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/menu/LuckConverterMenu.java", r'''
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
import ru.rfvv.metatechreborn.blockentity.LuckConverterBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class LuckConverterMenu extends AbstractContainerMenu {
    private final LuckConverterBlockEntity blockEntity;
    private final ContainerData data;
    private final boolean advanced;
    private final int machineMenuSlots;

    public LuckConverterMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (LuckConverterBlockEntity) inventory.player.level()
                .getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(10));
    }

    public LuckConverterMenu(int id, Inventory inventory,
                             LuckConverterBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.LUCK_CONVERTER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;
        this.advanced = blockEntity.isAdvanced();

        int inputColumns = advanced ? 12 : 10;
        int inputRows = advanced ? 6 : 3;
        for (int row = 0; row < inputRows; row++) {
            for (int column = 0; column < inputColumns; column++) {
                int handlerSlot = column + row * inputColumns;
                addSlot(new SlotItemHandler(blockEntity.getItems(), handlerSlot,
                        10 + column * 18, 30 + row * 18));
            }
        }

        int outputY = advanced ? 150 : 96;
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                int handlerSlot = LuckConverterBlockEntity.FIRST_OUTPUT + column + row * 9;
                addSlot(new SlotItemHandler(blockEntity.getItems(), handlerSlot,
                        10 + column * 18, outputY + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                });
            }
        }

        int upgradesX = 244;
        for (int index = 0; index < LuckConverterBlockEntity.UPGRADE_SLOTS; index++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(),
                    LuckConverterBlockEntity.FIRST_UPGRADE + index,
                    upgradesX + (index % 2) * 24, 42 + (index / 2) * 26));
        }
        addSlot(new SlotItemHandler(blockEntity.getItems(), LuckConverterBlockEntity.MODULE_SLOT, 316, 42));
        addSlot(new SlotItemHandler(blockEntity.getItems(), LuckConverterBlockEntity.ENERGY_SLOT, 316, 82));
        this.machineMenuSlots = slots.size();

        int playerX = 246;
        int playerY = 220;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        playerX + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, playerX + column * 18, playerY + 58));
        }
        addDataSlots(data);
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                        blockEntity.getLevel(), blockEntity.getBlockPos()), player,
                advanced ? ModBlocks.ADVANCED_LUCK_CONVERTER.get() : ModBlocks.LUCK_CONVERTER.get());
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < machineMenuSlots) {
            if (!moveItemStackTo(original, machineMenuSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, machineMenuSlots, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public boolean isAdvanced() { return advanced; }
    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getEnergy() { return data.get(2); }
    public int getEnergyCapacity() { return data.get(3); }
    public int getLuckLevel() { return data.get(4); }
    public int getStatus() { return data.get(5); }
    public int getOperations() { return data.get(7); }
    public int getEnergyPerTick() { return data.get(8); }
    public int getSpeedBonusPercent() { return data.get(9); }
    public boolean isInstantSpeed() { return getSpeedBonusPercent() >= 100; }
    public int progressPixels(int width) {
        return getMaxProgress() <= 0 ? 0 : Math.min(width, getProgress() * width / getMaxProgress());
    }
    public int energyPixels(int width) {
        return getEnergyCapacity() <= 0 ? 0 : Math.min(width, getEnergy() * width / getEnergyCapacity());
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/client/screen/LuckConverterScreen.java", r'''
package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.LuckConverterBlockEntity;
import ru.rfvv.metatechreborn.menu.LuckConverterMenu;

public final class LuckConverterScreen extends AbstractContainerScreen<LuckConverterMenu> {
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x606060;

    public LuckConverterScreen(LuckConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 430;
        imageHeight = 320;
        inventoryLabelX = 246;
        inventoryLabelY = 210;
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderMachineTooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        boolean advanced = menu.isAdvanced();
        int inputColumns = advanced ? 12 : 10;
        int inputRows = advanced ? 6 : 3;
        int outputY = advanced ? 150 : 96;

        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, inputColumns * 18 + 8, inputRows * 18 + 18);
        MetaTechGui.panel(g, leftPos + 6, topPos + outputY - 10, 170, 176);
        MetaTechGui.panel(g, leftPos + 232, topPos + 20, 192, 188);
        MetaTechGui.panel(g, leftPos + 240, topPos + 214, 170, 100);

        MetaTechGui.grid(g, leftPos + 10, topPos + 30, inputColumns, inputRows, 0xFF3A86B8);
        MetaTechGui.grid(g, leftPos + 10, topPos + outputY, 9, 9, 0xFF7653A6);
        for (int i = 0; i < LuckConverterBlockEntity.UPGRADE_SLOTS; i++) {
            MetaTechGui.slot(g, leftPos + 244 + (i % 2) * 24,
                    topPos + 42 + (i / 2) * 26, i < 3 ? 0xFF3A86B8 : 0xFFD89B2B);
        }
        MetaTechGui.slot(g, leftPos + 316, topPos + 42, 0xFF3A9D72);
        MetaTechGui.slot(g, leftPos + 316, topPos + 82, 0xFFD89B2B);
        MetaTechGui.grid(g, leftPos + 246, topPos + 220, 9, 3, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 246, topPos + 278, 9, 1, 0xFF777777);

        bar(g, 244, 140, 168, 7, menu.isInstantSpeed() ? 166
                : 166 * menu.getSpeedBonusPercent() / 100,
                menu.isInstantSpeed() ? 0xFF8E44AD : 0xFF3A86B8);
        bar(g, 244, 156, 168, 10, menu.progressPixels(166), MetaTechGui.CYAN);
        bar(g, 244, 190, 168, 10, menu.energyPixels(166), MetaTechGui.GOLD);
    }

    private void bar(GuiGraphics g, int x, int y, int w, int h, int pixels, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, 0xFF555555);
        int clamped = Math.max(0, Math.min(w - 2, pixels));
        if (clamped > 0) g.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + 1 + clamped, topPos + y + h - 1, color);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        boolean advanced = menu.isAdvanced();
        int outputY = advanced ? 150 : 96;
        g.drawString(font, title, 10, 8, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.inputs"), 10, 20, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.outputs"), 10, outputY - 10, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.upgrades"), 244, 24, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.module"), 312, 30, 0x176B45, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.energy_slot"), 312, 70, 0x8A6200, false);
        g.drawString(font, speedText(), 244, 128, menu.isInstantSpeed() ? 0x6B2E83 : 0x255C88, false);
        MetaTechGui.drawWrapped(g, font, Component.translatable(statusKey(menu.getStatus())),
                244, 172, 168, statusColor(menu.getStatus()), 2);
        g.drawString(font, Component.literal(menu.getEnergy() + " / " + menu.getEnergyCapacity() + " FE"),
                244, 202, 0x8A6200, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 244, 156, 168, 10)) {
            g.renderTooltip(font, Component.translatable("gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 244, 190, 168, 10)) {
            g.renderTooltip(font, Component.translatable("gui.metatech_reborn.tooltip.energy",
                    menu.getEnergy(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 244, 140, 168, 7)) {
            g.renderTooltip(font, Component.translatable("gui.metatech_reborn.luck_converter.tooltip.speed", speedText()), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 316, 42, 18, 18)) {
            g.renderTooltip(font, Component.translatable("gui.metatech_reborn.luck_converter.tooltip.module"), mouseX, mouseY);
        }
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private Component speedText() {
        if (menu.isInstantSpeed()) return Component.translatable("gui.metatech_reborn.luck_converter.speed.instant");
        if (menu.getSpeedBonusPercent() <= 0) return Component.translatable("gui.metatech_reborn.luck_converter.speed.none");
        return Component.translatable("gui.metatech_reborn.luck_converter.speed.percent", menu.getSpeedBonusPercent());
    }

    private static String statusKey(int status) {
        return switch (status) {
            case LuckConverterBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.luck_converter.status.running";
            case LuckConverterBlockEntity.STATUS_NO_MODULE -> "gui.metatech_reborn.luck_converter.status.no_module";
            case LuckConverterBlockEntity.STATUS_NO_ENERGY -> "gui.metatech_reborn.luck_converter.status.no_energy";
            case LuckConverterBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.luck_converter.status.output_full";
            case LuckConverterBlockEntity.STATUS_NO_VALID_INPUT -> "gui.metatech_reborn.luck_converter.status.no_input";
            default -> "gui.metatech_reborn.luck_converter.status.idle";
        };
    }
    private static int statusColor(int status) {
        return status == LuckConverterBlockEntity.STATUS_RUNNING ? 0x176B45
                : status == LuckConverterBlockEntity.STATUS_NO_ENERGY ? 0x8A6200 : 0xA02020;
    }
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
        addSlot(new SlotItemHandler(be.getItems(), DragonPatternEncoderBlockEntity.BLANK_SLOT, 212, 58));
        addSlot(new SlotItemHandler(be.getItems(), DragonPatternEncoderBlockEntity.OUTPUT_SLOT, 260, 58) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        addGhost(be, DragonPatternEncoderBlockEntity.CATALYST_GHOST_SLOT, 20, 58);
        for (int i = 0; i < 12; i++) addGhost(be, DragonPatternEncoderBlockEntity.INGREDIENT_GHOST_START + i,
                58 + (i % 4) * 20, 42 + (i / 4) * 20);
        addGhost(be, DragonPatternEncoderBlockEntity.PREVIEW_OUTPUT_SLOT, 158, 58);

        int invX = 78, invY = 150;
        for (int row=0; row<3; row++) for (int col=0; col<9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        for (int col=0; col<9; col++) addSlot(new Slot(inventory, col, invX + col * 18, invY + 58));
        addDataSlots(data);
    }

    private void addGhost(DragonPatternEncoderBlockEntity be, int slot, int x, int y) {
        addSlot(new SlotItemHandler(be.getItems(), slot, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            @Override public boolean mayPickup(@NotNull Player player) { return false; }
        });
    }
    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.DRAGON_PATTERN_ENCODER.get());
    }
    @Override public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == ENCODE_BUTTON) return blockEntity.encode();
        if (id == CLEAR_BUTTON) { blockEntity.clearRecipe(); return true; }
        return false;
    }
    public void applyRecipe(net.minecraft.resources.ResourceLocation id) { blockEntity.selectRecipe(id); }
    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot=slots.get(index); if(!slot.hasItem()||!slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(), copy=stack.copy();
        if(index<PLAYER_START) { if(!moveItemStackTo(stack,PLAYER_START,slots.size(),true)) return ItemStack.EMPTY; }
        else if(stack.is(ModItems.BLANK_DRAGON_PATTERN.get())) {
            if(!moveItemStackTo(stack,DragonPatternEncoderBlockEntity.BLANK_SLOT,DragonPatternEncoderBlockEntity.BLANK_SLOT+1,false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); return copy;
    }
    public int getStatus(){return data.get(0);} public boolean hasRecipe(){return data.get(1)!=0;}
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
    private static final int RED=0xFFE33434, DARK=0xFF151010, PANEL=0xFF2A1919;
    public DragonPatternEncoderScreen(DragonPatternEncoderMenu menu, Inventory inv, Component title) {
        super(menu,inv,title); imageWidth=318; imageHeight=232; inventoryLabelX=78; inventoryLabelY=138;
    }
    @Override protected void init(){ super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.dragon_encoder.encode"),
                b->click(DragonPatternEncoderMenu.ENCODE_BUTTON)).bounds(leftPos+208,topPos+88,88,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.dragon_encoder.clear"),
                b->click(DragonPatternEncoderMenu.CLEAR_BUTTON)).bounds(leftPos+208,topPos+112,88,20).build());
    }
    private void click(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override public void render(@NotNull GuiGraphics g,int mx,int my,float pt){renderBackground(g);super.render(g,mx,my,pt);renderTooltip(g,mx,my);}
    @Override protected void renderBg(@NotNull GuiGraphics g,float pt,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFF0F0F0);
        panel(g,8,20,188,112); panel(g,202,20,108,116); panel(g,74,144,170,84);
        for(int i=0;i<12;i++)slot(g,56+(i%4)*20,40+(i/4)*20,0xFF8A2424);
        slot(g,18,56,RED); slot(g,156,56,RED); slot(g,210,56,RED); slot(g,258,56,RED);
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)slot(g,76+c*18,148+r*18,0xFF666666);
        for(int c=0;c<9;c++)slot(g,76+c*18,206,0xFF666666);
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h){g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,DARK);g.fill(leftPos+x+1,topPos+y+1,leftPos+x+w-1,topPos+y+h-1,PANEL);g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+1,RED);}
    private void slot(GuiGraphics g,int x,int y,int a){g.fill(leftPos+x-1,topPos+y-1,leftPos+x+19,topPos+y+19,0xFF0B0B0B);g.fill(leftPos+x,topPos+y,leftPos+x+18,topPos+y+18,0xFFB8B8B8);g.fill(leftPos+x,topPos+y+17,leftPos+x+18,topPos+y+18,a);}
    @Override protected void renderLabels(@NotNull GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,7,0xFF7A0000,false);
        g.drawString(font,Component.literal("Катализатор"),10,112,0xFFE8B6B6,false);
        g.drawString(font,Component.literal("Ингредиенты (12)"),56,25,0xFFE8B6B6,false);
        g.drawString(font,Component.literal("Результат"),150,25,0xFFE8B6B6,false);
        g.drawString(font,statusText(),208,32,statusColor(),false);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF555555,false);
    }
    private Component statusText(){return Component.translatable("gui.metatech_reborn.dragon_encoder.status."+switch(menu.getStatus()){
        case DragonPatternEncoderBlockEntity.STATUS_READY->"ready";case DragonPatternEncoderBlockEntity.STATUS_ENCODED->"encoded";
        case DragonPatternEncoderBlockEntity.STATUS_NO_RECIPE->"recipe";case DragonPatternEncoderBlockEntity.STATUS_NO_BLANK->"blank";
        case DragonPatternEncoderBlockEntity.STATUS_OUTPUT_BLOCKED->"output";default->"idle";});}
    private int statusColor(){return menu.getStatus()==DragonPatternEncoderBlockEntity.STATUS_ENCODED?0xFF39C96B:RED;}
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
    public static final int PLAYER_START=ExtremeDragonAssemblerBlockEntity.TOTAL_SLOTS;
    private final ExtremeDragonAssemblerBlockEntity blockEntity; private final ContainerData data;
    public ExtremeDragonAssemblerMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,(ExtremeDragonAssemblerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()),new SimpleContainerData(7));}
    public ExtremeDragonAssemblerMenu(int id,Inventory inv,ExtremeDragonAssemblerBlockEntity be,ContainerData data){super(ModMenus.EXTREME_DRAGON_ASSEMBLER.get(),id);this.blockEntity=be;this.data=data;
        for(int i=0;i<12;i++)addSlot(new SlotItemHandler(be.getItems(),i,12+(i%2)*20,42+(i/2)*20));
        for(int i=0;i<12;i++)addSlot(new SlotItemHandler(be.getItems(),12+i,82+(i%4)*20,42+(i/4)*20));
        addSlot(new SlotItemHandler(be.getItems(),24,174,62)); addSlot(new SlotItemHandler(be.getItems(),25,222,62){@Override public boolean mayPlace(@NotNull ItemStack s){return false;}});
        for(int i=0;i<36;i++)addSlot(new SlotItemHandler(be.getItems(),26+i,258+(i%9)*18,34+(i/9)*18));
        int x=82,y=198;for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,x+c*18,y+r*18));for(int c=0;c<9;c++)addSlot(new Slot(inv,c,x+c*18,y+58));addDataSlots(data);}
    @Override public boolean stillValid(@NotNull Player p){return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(),blockEntity.getBlockPos()),p,ModBlocks.EXTREME_DRAGON_ASSEMBLER.get());}
    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player p,int i){Slot s=slots.get(i);if(!s.hasItem())return ItemStack.EMPTY;ItemStack st=s.getItem(),cp=st.copy();if(i<PLAYER_START){if(!moveItemStackTo(st,PLAYER_START,slots.size(),true))return ItemStack.EMPTY;}else if(DragonFusionSupport.isInjector(st)){if(!moveItemStackTo(st,0,12,false))return ItemStack.EMPTY;}else if(st.is(ModItems.ENCODED_DRAGON_PATTERN.get())){if(!moveItemStackTo(st,26,62,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(st,12,25,false))return ItemStack.EMPTY;if(st.isEmpty())s.set(ItemStack.EMPTY);else s.setChanged();s.onTake(p,st);return cp;}
    public int progress(){return data.get(0);}public int maxProgress(){return data.get(1);}public int machineTier(){return data.get(2)-1;}public int energy(){return data.get(3);}public int energyCapacity(){return data.get(4);}public int recipeTier(){return data.get(5)-1;}public int status(){return data.get(6);}
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

public final class ExtremeDragonAssemblerScreen extends AbstractContainerScreen<ExtremeDragonAssemblerMenu>{
    private static final int RED=0xFFE33A3A,PANEL=0xFF261010,TEXT=0xFFF5DADA;
    public ExtremeDragonAssemblerScreen(ExtremeDragonAssemblerMenu m,Inventory i,Component t){super(m,i,t);imageWidth=430;imageHeight=292;inventoryLabelX=82;inventoryLabelY=186;}
    @Override public void render(@NotNull GuiGraphics g,int mx,int my,float pt){renderBackground(g);super.render(g,mx,my,pt);tooltip(g,mx,my);renderTooltip(g,mx,my);}
    @Override protected void renderBg(@NotNull GuiGraphics g,float pt,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFE9E9E9);panel(g,6,20,50,144);panel(g,64,20,184,144);panel(g,252,20,172,96);panel(g,76,190,176,96);
        for(int i=0;i<12;i++)slot(g,10+(i%2)*20,40+(i/2)*20,0xFF781B1B);for(int i=0;i<12;i++)slot(g,80+(i%4)*20,40+(i/4)*20,RED);slot(g,172,60,0xFFFF7D52);slot(g,220,60,RED);
        for(int i=0;i<36;i++)slot(g,256+(i%9)*18,32+(i/9)*18,0xFFAA2828);for(int r=0;r<3;r++)for(int c=0;c<9;c++)slot(g,80+c*18,196+r*18,0xFF616161);for(int c=0;c<9;c++)slot(g,80+c*18,254,0xFF616161);
        int pp=menu.maxProgress()<=0?0:(int)(152L*menu.progress()/menu.maxProgress());bar(g,80,118,154,8,pp,RED);long cap=Math.max(1L,Integer.toUnsignedLong(menu.energyCapacity())),st=Math.max(0L,Integer.toUnsignedLong(menu.energy()));bar(g,80,134,154,8,(int)Math.min(152L,152L*st/cap),0xFFFF4A32);
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h){g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,0xFF7D1B1B);g.fill(leftPos+x+2,topPos+y+2,leftPos+x+w-2,topPos+y+h-2,PANEL);}
    private void slot(GuiGraphics g,int x,int y,int a){g.fill(leftPos+x,topPos+y,leftPos+x+18,topPos+y+18,a);g.fill(leftPos+x+2,topPos+y+2,leftPos+x+16,topPos+y+16,0xFFBFBFBF);}
    private void bar(GuiGraphics g,int x,int y,int w,int h,int p,int c){g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,0xFF120505);if(p>0)g.fill(leftPos+x+1,topPos+y+1,leftPos+x+1+Math.min(w-2,p),topPos+y+h-1,c);}
    @Override protected void renderLabels(@NotNull GuiGraphics g,int mx,int my){g.drawString(font,title,8,7,0xFF8E0C0C,false);g.drawString(font,Component.literal("Инжекторы"),9,25,TEXT,false);g.drawString(font,Component.literal("Fusion-рецепт"),72,25,TEXT,false);g.drawString(font,Component.literal("Банк шаблонов"),260,25,TEXT,false);g.drawString(font,Component.literal("Кат."),168,84,0xFFFFA2A2,false);g.drawString(font,Component.literal("→"),202,65,0xFFFFA2A2,false);
        int tier=menu.machineTier();Component tt=tier<0?Component.literal("нет"):Component.translatable("gui.metatech_reborn.dragon.tier."+DragonFusionSupport.tierKey(tier));g.drawString(font,Component.literal("Уровень: ").append(tt),80,102,tier<0?0xFFFF6868:0xFFFFB0A6,false);g.drawString(font,status(),80,150,statusColor(),false);g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF6D1414,false);}
    private Component status(){return switch(menu.status()){case ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING->Component.literal("Крафт выполняется");case ExtremeDragonAssemblerBlockEntity.STATUS_TIER_LOW->Component.literal("Низкий уровень");case ExtremeDragonAssemblerBlockEntity.STATUS_MISSING_INPUT->Component.literal("Ожидание ингредиентов");case ExtremeDragonAssemblerBlockEntity.STATUS_NO_ENERGY->Component.literal("Недостаточно энергии");case ExtremeDragonAssemblerBlockEntity.STATUS_OUTPUT_FULL->Component.literal("Выход занят");default->Component.literal("Готов к заданию");};}
    private int statusColor(){return menu.status()==ExtremeDragonAssemblerBlockEntity.STATUS_RUNNING?0xFFFFC0B4:0xFFFF6A5A;}
    private void tooltip(GuiGraphics g,int mx,int my){if(inside(mx,my,80,134,154,8))g.renderTooltip(font,Component.literal("Энергия: "+Integer.toUnsignedLong(menu.energy())+" / "+Integer.toUnsignedLong(menu.energyCapacity())+" FE"),mx,my);else if(inside(mx,my,80,118,154,8))g.renderTooltip(font,Component.literal("Прогресс: "+menu.progress()+" / "+menu.maxProgress()),mx,my);}
    private boolean inside(int mx,int my,int x,int y,int w,int h){return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h;}
}
''')

replace_once(
    "src/main/java/ru/rfvv/metatechreborn/blockentity/MolecularAssemblerBlockEntity.java",
    '''    public int getActivePatternSlots() {\n        return patternUpgradeItems.getStackInSlot(0).getItem() instanceof PatternCapacityUpgradeItem\n                ? MAX_PATTERN_SLOTS : BASE_PATTERN_SLOTS;\n    }\n''',
    '''    public int getActivePatternSlots() {\n        // The upgrade slot itself only accepts PatternCapacityUpgradeItem. Checking\n        // for a non-empty stack is more robust across world reloads and remapped classes.\n        return patternUpgradeItems.getStackInSlot(0).isEmpty()\n                ? BASE_PATTERN_SLOTS : MAX_PATTERN_SLOTS;\n    }\n''',
    "pattern bank capacity"
)

replace_once(
    "src/main/java/ru/rfvv/metatechreborn/client/renderer/ManaDrillRenderer.java",
    '''        VertexConsumer consumer = buffer.getBuffer(\n                RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));\n''',
    '''        // All approved drill textures are opaque. A solid render type prevents\n        // the assembled machine from looking like glass when the camera approaches it.\n        VertexConsumer consumer = buffer.getBuffer(\n                RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));\n''',
    "solid formed mana drill renderer"
)

# Inactive pattern-bank cells remain visibly disabled through their accent colour;
# remove the inner black patch that looked like a broken/missing texture.
replace_once(
    "src/main/java/ru/rfvv/metatechreborn/client/screen/MolecularAssemblerScreen.java",
    '''                MetaTechGui.slot(g, leftPos + 304 + column * 18, topPos + 28 + row * 18, accent);\n                if (slot >= menu.getActivePatternSlots()) {\n                    g.fill(leftPos + 308 + column * 18, topPos + 32 + row * 18,\n                            leftPos + 316 + column * 18, topPos + 40 + row * 18, 0xAA000000);\n                }\n''',
    '''                MetaTechGui.slot(g, leftPos + 304 + column * 18, topPos + 28 + row * 18, accent);\n''',
    "pattern bank disabled-cell visual"
)

print("Applied 0.6.107 GUI, mana drill render and pattern bank fixes")
