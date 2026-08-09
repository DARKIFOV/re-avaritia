package ru.rfvv.metatechreborn.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.rfvv.metatechreborn.menu.MysticalInfusionEncoderMenu;

import java.util.function.Supplier;

public record MysticalInfusionRecipePacket(ResourceLocation recipeId) {
    public static void encode(MysticalInfusionRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.recipeId);
    }
    public static MysticalInfusionRecipePacket decode(FriendlyByteBuf buffer) {
        return new MysticalInfusionRecipePacket(buffer.readResourceLocation());
    }
    public static void handle(MysticalInfusionRecipePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof MysticalInfusionEncoderMenu menu) {
                menu.applyRecipe(packet.recipeId);
            }
        });
        context.setPacketHandled(true);
    }
}
