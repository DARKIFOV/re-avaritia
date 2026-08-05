package ru.rfvv.metatechreborn.network;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.menu.ExtremePatternEncoderMenu;

import java.util.function.Supplier;

public final class EncoderGhostRecipePacket {
    private final NonNullList<ItemStack> grid;

    public EncoderGhostRecipePacket(NonNullList<ItemStack> source) {
        grid = NonNullList.withSize(ExtremePatternEncoderBlockEntity.GRID_SLOTS, ItemStack.EMPTY);
        int limit = Math.min(source.size(), grid.size());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = source.get(slot);
            if (stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            copy.setCount(1);
            grid.set(slot, copy);
        }
    }

    public static void encode(EncoderGhostRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(ExtremePatternEncoderBlockEntity.GRID_SLOTS);
        for (ItemStack stack : packet.grid) buffer.writeItem(stack);
    }

    public static EncoderGhostRecipePacket decode(FriendlyByteBuf buffer) {
        int slots = buffer.readVarInt();
        if (slots != ExtremePatternEncoderBlockEntity.GRID_SLOTS) {
            throw new IllegalArgumentException("Invalid extreme pattern grid size: " + slots);
        }
        NonNullList<ItemStack> grid = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int slot = 0; slot < slots; slot++) grid.set(slot, buffer.readItem());
        return new EncoderGhostRecipePacket(grid);
    }

    public static void handle(EncoderGhostRecipePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.containerMenu instanceof ExtremePatternEncoderMenu menu) {
                menu.applyGhostRecipe(player, packet.grid);
            }
        });
        context.setPacketHandled(true);
    }
}
