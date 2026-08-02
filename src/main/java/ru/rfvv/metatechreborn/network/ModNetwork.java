package ru.rfvv.metatechreborn.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.rfvv.metatechreborn.MetaTechReborn;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MetaTechReborn.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static boolean registered;

    private ModNetwork() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(
                0,
                EncoderGhostRecipePacket.class,
                EncoderGhostRecipePacket::encode,
                EncoderGhostRecipePacket::decode,
                EncoderGhostRecipePacket::handle);
    }
}
