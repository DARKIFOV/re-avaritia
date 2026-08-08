from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/client/renderer/ManaDrillRenderer.java"
text = path.read_text(encoding="utf-8")

# The assembled drill uses a decorative one-piece BER that protrudes toward the camera.
# When the near clip plane intersects those raised/recessed parts, the hidden formed blocks
# behind it can become visible. Keep the decorative geometry, but add a fully closed opaque
# inner hull behind the front details. This gives the camera a solid backstop at point-blank
# range, so the multiblock can never visually turn transparent and reveal the mine/world.
marker = '''        FaceSprites nozzle = new FaceSprites(\n                sprites.nozzleFront(), sprites.nozzleSide(),\n                sprites.nozzleSide(), sprites.nozzleSide(),\n                sprites.nozzleTop(), sprites.nozzleSide());\n\n'''
insert = marker + '''        // Opaque inner safety hull. It sits behind the recessed controller/nozzle/front frame\n        // and in front of the hidden 3x3 formed blocks. At point-blank camera distances the\n        // near plane may clip the decorative front pieces, but this closed hull remains in\n        // view and prevents the world or mine shafts behind the drill from showing through.\n        emitBox(poseStack, consumer, -0.68F, -0.68F, 0.52F,\n                1.68F, 1.68F, 2.96F, casing, renderLight, packedOverlay);\n\n'''
if "Opaque inner safety hull" not in text:
    if marker not in text:
        raise RuntimeError("ManaDrillRenderer insertion point not found")
    text = text.replace(marker, insert, 1)

# Keep the renderer strictly non-blended and double-sided. All eight drill textures are
# fully opaque, so cutout-no-cull gives stable close-up rendering without translucent sorting.
if "RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)" not in text:
    raise RuntimeError("Expected opaque no-cull Mana Drill render type is missing")
if ".color(255, 255, 255, 255)" not in text:
    raise RuntimeError("Mana Drill vertices are not forced to full alpha")

path.write_text(text, encoding="utf-8")
print("Applied 0.6.113 Mana Drill point-blank opaque inner hull fix")
