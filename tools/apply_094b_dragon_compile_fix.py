from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/item/EncodedDragonPatternItem.java"
text = path.read_text(encoding="utf-8")
old = '''    @Override public Component getName(ItemStack stack) {\n        return read(stack)\n                .map(data -> Component.translatable("item.metatech_reborn.encoded_dragon_pattern.named",\n                        data.output().getHoverName()))\n                .orElseGet(() -> super.getName(stack));\n    }\n'''
new = '''    @Override public Component getName(ItemStack stack) {\n        Optional<DragonFusionPatternData> decoded = read(stack);\n        if (decoded.isPresent()) {\n            return Component.translatable("item.metatech_reborn.encoded_dragon_pattern.named",\n                    decoded.get().output().getHoverName());\n        }\n        return super.getName(stack);\n    }\n'''
if new not in text:
    if text.count(old) != 1:
        raise RuntimeError("dragon pattern getName patch target not found")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("Applied 0.6.94 dragon pattern component compile fix")
