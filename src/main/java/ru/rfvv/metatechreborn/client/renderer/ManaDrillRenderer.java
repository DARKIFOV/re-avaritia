package ru.rfvv.metatechreborn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.block.ManaDrillBlock;
import ru.rfvv.metatechreborn.blockentity.ManaDrillBlockEntity;

/** Draws the assembled 3x3x3 drill as one visual object from the controller. */
public final class ManaDrillRenderer implements BlockEntityRenderer<ManaDrillBlockEntity> {
    private static final ResourceLocation CASING = texture("mana_drill_casing");
    private static final ResourceLocation CORE = texture("mana_drill_core");
    private static final ResourceLocation CONTROLLER_FRONT = texture("mana_drill_controller_front");
    private static final ResourceLocation CONTROLLER_SIDE = texture("mana_drill_controller_side");
    private static final ResourceLocation CONTROLLER_TOP = texture("mana_drill_controller_top");
    private static final ResourceLocation NOZZLE_FRONT = texture("mana_drill_nozzle_front");
    private static final ResourceLocation NOZZLE_SIDE = texture("mana_drill_nozzle_side");
    private static final ResourceLocation NOZZLE_TOP = texture("mana_drill_nozzle_top");

    public ManaDrillRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ManaDrillBlockEntity drill, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = drill.getBlockState();
        if (!state.hasProperty(ManaDrillBlock.FORMED)
                || !state.getValue(ManaDrillBlock.FORMED)) {
            return;
        }

        Direction facing = state.getValue(ManaDrillBlock.FACING);
        Direction visualFront = state.getValue(ManaDrillBlock.REVERSED)
                ? facing.getOpposite()
                : facing;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(visualFront)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Sprites sprites = loadSprites();

        for (int y = -1; y <= 1; y++) {
            for (int z = 0; z <= 2; z++) {
                for (int x = -1; x <= 1; x++) {
                    Part part = partAt(x, y, z);
                    if (part == Part.AIR) continue;
                    for (Direction face : Direction.values()) {
                        if (partAt(x + face.getStepX(), y + face.getStepY(),
                                z + face.getStepZ()) != Part.AIR) {
                            continue;
                        }
                        emitFace(poseStack, consumer, x, y, z, face,
                                spriteFor(sprites, part, face), packedLight, packedOverlay);
                    }
                }
            }
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ManaDrillBlockEntity drill) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static Part partAt(int x, int y, int z) {
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < 0 || z > 2) {
            return Part.AIR;
        }
        if (x == 0 && y == 1 && z == 0) return Part.AIR;
        if (x == 0 && y == 0 && z == 0) return Part.CONTROLLER;
        if (x == 0 && y == 0 && z == 1) return Part.CORE;
        if (x == 0 && y == 1 && z == 1) return Part.NOZZLE;
        return Part.CASING;
    }

    private static TextureAtlasSprite spriteFor(Sprites sprites, Part part, Direction face) {
        return switch (part) {
            case CONTROLLER -> switch (face) {
                case NORTH -> sprites.controllerFront();
                case UP -> sprites.controllerTop();
                case DOWN -> sprites.casing();
                default -> sprites.controllerSide();
            };
            case CORE -> sprites.core();
            case NOZZLE -> switch (face) {
                case NORTH -> sprites.nozzleFront();
                case UP -> sprites.nozzleTop();
                default -> sprites.nozzleSide();
            };
            case CASING, AIR -> sprites.casing();
        };
    }

    private static Sprites loadSprites() {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        return new Sprites(
                atlas.apply(CASING),
                atlas.apply(CORE),
                atlas.apply(CONTROLLER_FRONT),
                atlas.apply(CONTROLLER_SIDE),
                atlas.apply(CONTROLLER_TOP),
                atlas.apply(NOZZLE_FRONT),
                atlas.apply(NOZZLE_SIDE),
                atlas.apply(NOZZLE_TOP));
    }

    private static float rotationFor(Direction front) {
        return switch (front) {
            case NORTH -> 0.0F;
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static void emitFace(PoseStack poseStack, VertexConsumer consumer,
                                 int blockX, int blockY, int blockZ, Direction face,
                                 TextureAtlasSprite sprite, int packedLight, int packedOverlay) {
        float x0 = blockX;
        float x1 = blockX + 1.0F;
        float y0 = blockY;
        float y1 = blockY + 1.0F;
        float z0 = blockZ;
        float z1 = blockZ + 1.0F;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        switch (face) {
            case NORTH -> quad(poseStack, consumer, packedLight, packedOverlay, 0, 0, -1,
                    x1, y0, z0, u0, v1,
                    x0, y0, z0, u1, v1,
                    x0, y1, z0, u1, v0,
                    x1, y1, z0, u0, v0);
            case SOUTH -> quad(poseStack, consumer, packedLight, packedOverlay, 0, 0, 1,
                    x0, y0, z1, u0, v1,
                    x1, y0, z1, u1, v1,
                    x1, y1, z1, u1, v0,
                    x0, y1, z1, u0, v0);
            case WEST -> quad(poseStack, consumer, packedLight, packedOverlay, -1, 0, 0,
                    x0, y0, z0, u0, v1,
                    x0, y0, z1, u1, v1,
                    x0, y1, z1, u1, v0,
                    x0, y1, z0, u0, v0);
            case EAST -> quad(poseStack, consumer, packedLight, packedOverlay, 1, 0, 0,
                    x1, y0, z1, u0, v1,
                    x1, y0, z0, u1, v1,
                    x1, y1, z0, u1, v0,
                    x1, y1, z1, u0, v0);
            case UP -> quad(poseStack, consumer, packedLight, packedOverlay, 0, 1, 0,
                    x0, y1, z1, u0, v1,
                    x1, y1, z1, u1, v1,
                    x1, y1, z0, u1, v0,
                    x0, y1, z0, u0, v0);
            case DOWN -> quad(poseStack, consumer, packedLight, packedOverlay, 0, -1, 0,
                    x0, y0, z0, u0, v1,
                    x1, y0, z0, u1, v1,
                    x1, y0, z1, u1, v0,
                    x0, y0, z1, u0, v0);
        }
    }

    private static void quad(PoseStack poseStack, VertexConsumer consumer,
                             int packedLight, int packedOverlay,
                             float normalX, float normalY, float normalZ,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float x4, float y4, float z4, float u4, float v4) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, packedLight, packedOverlay,
                x1, y1, z1, u1, v1, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay,
                x2, y2, z2, u2, v2, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay,
                x3, y3, z3, u3, v3, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay,
                x4, y4, z4, u4, v4, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               int packedLight, int packedOverlay,
                               float x, float y, float z, float u, float v,
                               float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(MetaTechReborn.MOD_ID, "block/" + name);
    }

    private enum Part {
        AIR,
        CASING,
        CONTROLLER,
        CORE,
        NOZZLE
    }

    private record Sprites(TextureAtlasSprite casing,
                           TextureAtlasSprite core,
                           TextureAtlasSprite controllerFront,
                           TextureAtlasSprite controllerSide,
                           TextureAtlasSprite controllerTop,
                           TextureAtlasSprite nozzleFront,
                           TextureAtlasSprite nozzleSide,
                           TextureAtlasSprite nozzleTop) {}
}
