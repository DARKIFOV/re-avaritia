package ru.rfvv.metatechreborn.integration.ae2;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Compatibility pass for AE2 cables that were already placed before the assembler's
 * attached grid-node capability became available. Normal AE2 discovery still works;
 * this only creates the missing adjacent connection.
 */
public final class MolecularAssemblerAe2ConnectionFix {
    private static final Set<MolecularAssemblerBlockEntity> HOSTS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        // The event handler class name must be unique in this package. Forge/EventBus
        // generates dynamic invokers from the simple listener class name; using another
        // nested class named "Events" caused a ClassCastException with the provider's
        // own Events listener during LevelTickEvent dispatch.
        MinecraftForge.EVENT_BUS.register(new ConnectionEvents());
    }

    private static final class ConnectionEvents {
        @SubscribeEvent
        public void attachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
            if (!(event.getObject() instanceof MolecularAssemblerBlockEntity assembler)) return;
            synchronized (HOSTS) {
                HOSTS.add(assembler);
            }
            event.addListener(() -> {
                synchronized (HOSTS) {
                    HOSTS.remove(assembler);
                }
            });
        }

        @SubscribeEvent
        public void levelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.level.isClientSide
                    || event.level.getGameTime() % 20L != 0L) return;

            List<MolecularAssemblerBlockEntity> snapshot;
            synchronized (HOSTS) {
                snapshot = new ArrayList<>(HOSTS);
            }
            for (MolecularAssemblerBlockEntity assembler : snapshot) {
                if (assembler.isRemoved() || assembler.getLevel() != event.level) continue;
                connectAdjacent(event.level, assembler);
            }
        }
    }

    private static void connectAdjacent(Level level, MolecularAssemblerBlockEntity assembler) {
        for (Direction direction : Direction.values()) {
            assembler.getCapability(Capabilities.IN_WORLD_GRID_NODE_HOST, direction).ifPresent(nodeHost -> {
                IGridNode ownNode = nodeHost.getGridNode(direction);
                IGridNode adjacentNode = GridHelper.getExposedNode(
                        level,
                        assembler.getBlockPos().relative(direction),
                        direction.getOpposite());
                if (ownNode == null || adjacentNode == null || ownNode == adjacentNode) return;
                try {
                    GridHelper.createConnection(ownNode, adjacentNode);
                } catch (IllegalStateException ignored) {
                    // Already connected, or AE2 completed the connection during this tick.
                }
            });
        }
    }

    private MolecularAssemblerAe2ConnectionFix() {}
}
