package ru.rfvv.metatechreborn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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

/** Draws the assembled drill as one shaped machine instead of a flat 3x3 wall. */
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

        // The controller is inside the formed multiblock and can receive almost no
        // light, which made the custom 3x3 renderer look nearly black. Use the
        // environmental light from the air immediately in front of the machine.
        // This keeps the original white textures white without making the drill
        // full-bright or self-illuminated at night.
        int renderLight = exteriorLight(drill, visualFront, packedLight);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(visualFront)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Sprites sprites = loadSprites();
        FaceSprites casing = FaceSprites.all(sprites.casing());
        FaceSprites core = FaceSprites.all(sprites.core());
        FaceSprites controller = new FaceSprites(
                sprites.controllerFront(), sprites.controllerSide(),
                sprites.controllerSide(), sprites.controllerSide(),
                sprites.controllerTop(), sprites.casing());
        FaceSprites nozzle = new FaceSprites(
                sprites.nozzleFront(), sprites.nozzleSide(),
                sprites.nozzleSide(), sprites.nozzleSide(),
                sprites.nozzleTop(), sprites.nozzleSide());

        // Deep side housings and rear body. The gaps and different depths keep the
        // assembled drill from looking like a single flat wall.
        emitBox(poseStack, consumer, -1.00F, -1.00F, 0.12F,
                -0.12F, 2.00F, 3.00F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, 1.12F, -1.00F, 0.12F,
                2.00F, 2.00F, 3.00F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, -0.12F, -1.00F, 0.12F,
                1.12F, -0.12F, 3.00F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, -0.12F, 1.12F, 0.55F,
                1.12F, 2.00F, 3.00F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, -0.12F, -0.12F, 0.72F,
                1.12F, 1.12F, 3.00F, casing, renderLight, packedOverlay);

        // Raised front frame.
        emitBox(poseStack, consumer, -1.00F, -1.00F, -0.08F,
                -0.68F, 2.00F, 0.28F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, 1.68F, -1.00F, -0.08F,
                2.00F, 2.00F, 0.28F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, -0.68F, -1.00F, -0.08F,
                1.68F, -0.68F, 0.28F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, -0.68F, 1.68F, 0.18F,
                -0.08F, 2.00F, 0.55F, casing, renderLight, packedOverlay);
        emitBox(poseStack, consumer, 1.08F, 1.68F, 0.18F,
                1.68F, 2.00F, 0.55F, casing, renderLight, packedOverlay);

        // Recessed controller and visible mana core.
        emitBox(poseStack, consumer, 0.04F, 0.04F, -0.28F,
                0.96F, 0.96F, 0.48F, controller, renderLight, packedOverlay);
        emitBox(poseStack, consumer, 0.16F, 1.08F, 0.18F,
                0.84F, 1.76F, 0.72F, core, renderLight, packedOverlay);

        // The nozzle protrudes through the upper opening and gives the machine a
        // recognisable drill silhouette from the front and sides.
        emitBox(poseStack, consumer, 0.25F, 1.20F, -1.10F,
                0.75F, 1.64F, 0.28F, nozzle, renderLight, packedOverlay);
        emitBox(poseStack, consumer, 0.15F, 1.12F, -0.10F,
                0.85F, 1.72F, 0.30F, casing, renderLight, packedOverlay);

        poseStack.popPose();
    }

    private static int exteriorLight(ManaDrillBlockEntity drill, Direction visualFront, int fallback) {
        if (drill.getLevel() == null) {
            return fallback;
        }
        return LevelRenderer.getLightColor(
                drill.getLevel(), drill.getBlockPos().relative(visualFront));
    }

    @Override
    public boolean shouldRenderOffScreen(ManaDrillBlockEntity drill) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static void emitBox(PoseStack poseStack, VertexConsumer consumer,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                FaceSprites sprites, int packedLight, int packedOverlay) {
        emitFace(poseStack, consumer, Direction.NORTH, x0, y0, z0, x1, y1, z1,
                sprites.north(), packedLight, packedOverlay);
        emitFace(poseStack, consumer, Direction.SOUTH, x0, y0, z0, x1, y1, z1,
                sprites.south(), packedLight, packedOverlay);
        emitFace(poseStack, consumer, Direction.WEST, x0, y0, z0, x1, y1, z1,
                sprites.west(), packedLight, packedOverlay);
        emitFace(poseStack, consumer, Direction.EAST, x0, y0, z0, x1, y1, z1,
                sprites.east(), packedLight, packedOverlay);
        emitFace(poseStack, consumer, Direction.UP, x0, y0, z0, x1, y1, z1,
                sprites.up(), packedLight, packedOverlay);
        emitFace(poseStack, consumer, Direction.DOWN, x0, y0, z0, x1, y1, z1,
                sprites.down(), packedLight, packedOverlay);
    }

    private static void emitFace(PoseStack poseStack, VertexConsumer consumer,
                                 Direction face, float x0, float y0, float z0,
                                 float x1, float y1, float z1, TextureAtlasSprite sprite,
                                 int packedLight, int packedOverlay) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        switch (face) {
            case NORTH -> quad(poseStack, consumer, packedLight, packedOverlay, 0, 0, -1,
                    x1, y0, z0, u0, v1, x0, y0, z0, u1, v1,
                    x0, y1, z0, u1, v0, x1, y1, z0, u0, v0);
            case SOUTH -> quad(poseStack, consumer, packedLight, packedOverlay, 0, 0, 1,
                    x0, y0, z1, u0, v1, x1, y0, z1, u1, v1,
                    x1, y1, z1, u1, v0, x0, y1, z1, u0, v0);
            case WEST -> quad(poseStack, consumer, packedLight, packedOverlay, -1, 0, 0,
                    x0, y0, z0, u0, v1, x0, y0, z1, u1, v1,
                    x0, y1, z1, u1, v0, x0, y1, z0, u0, v0);
            case EAST -> quad(poseStack, consumer, packedLight, packedOverlay, 1, 0, 0,
                    x1, y0, z1, u0, v1, x1, y0, z0, u1, v1,
                    x1, y1, z0, u1, v0, x1, y1, z1, u0, v0);
            case UP -> quad(poseStack, consumer, packedLight, packedOverlay, 0, 1, 0,
                    x0, y1, z1, u0, v1, x1, y1, z1, u1, v1,
                    x1, y1, z0, u1, v0, x0, y1, z0, u0, v0);
            case DOWN -> quad(poseStack, consumer, packedLight, packedOverlay, 0, -1, 0,
                    x0, y0, z0, u0, v1, x1, y0, z0, u1, v1,
                    x1, y0, z1, u1, v0, x0, y0, z1, u0, v0);
        }
    }

    private static Sprites loadSprites() {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        return new Sprites(
                atlas.apply(CASING), atlas.apply(CORE),
                atlas.apply(CONTROLLER_FRONT), atlas.apply(CONTROLLER_SIDE),
                atlas.apply(CONTROLLER_TOP), atlas.apply(NOZZLE_FRONT),
                atlas.apply(NOZZLE_SIDE), atlas.apply(NOZZLE_TOP));
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

    private record FaceSprites(TextureAtlasSprite north, TextureAtlasSprite south,
                               TextureAtlasSprite west, TextureAtlasSprite east,
                               TextureAtlasSprite up, TextureAtlasSprite down) {
        private static FaceSprites all(TextureAtlasSprite sprite) {
            return new FaceSprites(sprite, sprite, sprite, sprite, sprite, sprite);
        }
    }

    private record Sprites(TextureAtlasSprite casing, TextureAtlasSprite core,
                           TextureAtlasSprite controllerFront,
                           TextureAtlasSprite controllerSide,
                           TextureAtlasSprite controllerTop,
                           TextureAtlasSprite nozzleFront,
                           TextureAtlasSprite nozzleSide,
                           TextureAtlasSprite nozzleTop) {}
}
