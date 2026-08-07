from pathlib import Path
import json
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
GEN = ROOT / "build/generated/metatech_assets"
ASSETS = GEN / "assets/metatech_reborn"
DATA = GEN / "data/metatech_reborn"
SOURCE_ASSETS = ROOT / "src/main/resources/assets/metatech_reborn"


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def write_png(path: Path, pixels: list[list[tuple[int,int,int,int]]]) -> None:
    h = len(pixels); w = len(pixels[0])
    rows = []
    for row_pixels in pixels:
        row = bytearray([0])
        for rgba in row_pixels: row.extend(rgba)
        rows.append(bytes(row))
    raw = b"".join(rows)
    png = b"\x89PNG\r\n\x1a\n"
    png += png_chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += png_chunk(b"IDAT", zlib.compress(raw, 9))
    png += png_chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def machine_texture(path: Path, encoder: bool) -> None:
    n=128
    p=[[(0,0,0,255) for _ in range(n)] for _ in range(n)]
    def rect(x0,y0,x1,y1,c):
        for y in range(max(0,y0),min(n,y1+1)):
            for x in range(max(0,x0),min(n,x1+1)): p[y][x]=c
    def px(x,y,c):
        if 0<=x<n and 0<=y<n:p[y][x]=c
    black=(10,10,12,255); steel=(34,35,40,255); steel2=(55,57,65,255)
    darkred=(65,7,10,255); red=(181,18,24,255); bright=(255,52,52,255); glow=(255,105,70,255)
    rect(0,0,127,127,black); rect(4,4,123,123,steel); rect(8,8,119,119,(17,17,20,255))
    for k in range(0,5):
        c=steel2 if k%2==0 else darkred
        rect(8+k*3,8+k*3,119-k*3,10+k*3,c); rect(8+k*3,117-k*3,119-k*3,119-k*3,c)
        rect(8+k*3,8+k*3,10+k*3,119-k*3,c); rect(117-k*3,8+k*3,119-k*3,119-k*3,c)
    # corner clamps
    for ox,oy in ((12,12),(92,12),(12,92),(92,92)):
        rect(ox,oy,ox+23,oy+23,steel2); rect(ox+4,oy+4,ox+19,oy+19,darkred)
        rect(ox+7,oy+7,ox+16,oy+16,red); rect(ox+10,oy+10,ox+13,oy+13,bright)
    # central red draconic core
    cx=64
    if encoder:
        rect(39,38,89,90,(24,10,12,255)); rect(43,42,85,86,darkred)
        for y in range(48,81):
            half=max(2,18-abs(64-y))
            for x in range(cx-half,cx+half+1):
                if abs(x-cx)+abs(y-64)<26: px(x,y,red)
        rect(58,54,70,74,bright); rect(61,57,67,71,(255,155,115,255))
    else:
        # ring + core, visually close to extreme assembler but Draconic red
        for y in range(28,100):
            for x in range(28,100):
                d2=(x-cx)*(x-cx)+(y-cx)*(y-cx)
                if 31*31<=d2<=36*36: px(x,y,red if (x+y)%3 else bright)
                if d2<=19*19: px(x,y,darkred)
                if d2<=12*12: px(x,y,red)
                if d2<=6*6: px(x,y,glow)
        for x,y in ((64,24),(104,64),(64,104),(24,64)):
            rect(x-2,y-6,x+2,y+6,bright)
    # micro red traces
    for i in range(15,114,10):
        px(i,15,bright); px(i,112,red); px(15,i,red); px(112,i,bright)
    write_png(path,p)


def pattern_texture(path: Path, encoded: bool) -> None:
    n=128; p=[[(0,0,0,0) for _ in range(n)] for _ in range(n)]
    def rect(x0,y0,x1,y1,c):
        for y in range(y0,y1+1):
            for x in range(x0,x1+1): p[y][x]=c
    dark=(18,12,14,255); steel=(67,65,72,255); red=(190,20,30,255); bright=(255,70,70,255)
    rect(12,12,115,115,dark); rect(14,14,113,17,steel); rect(14,110,113,113,steel)
    rect(14,18,17,109,steel); rect(110,18,113,109,steel)
    rect(22,22,105,105,(32,17,20,255)); rect(25,25,102,28,red); rect(25,99,102,102,red)
    if encoded:
        for y in range(38,91):
            half=max(3,26-abs(64-y))
            for x in range(64-half,65+half):
                if abs(x-64)+abs(y-64)<38: p[y][x]=red
        rect(58,48,70,80,bright); rect(61,52,67,76,(255,170,130,255))
    else:
        for gy in range(4):
            for gx in range(4):
                x=33+gx*16;y=33+gy*16
                rect(x,y,x+11,y+11,(47,42,47,255));rect(x,y+10,x+11,y+11,red)
    write_png(path,p)


def patch_lang(locale: str, additions: dict[str,str]) -> None:
    data={}
    source=SOURCE_ASSETS/"lang"/f"{locale}.json"
    generated=ASSETS/"lang"/f"{locale}.json"
    if source.exists(): data.update(json.loads(source.read_text(encoding="utf-8")))
    if generated.exists(): data.update(json.loads(generated.read_text(encoding="utf-8")))
    data.update(additions); write_json(generated,data)


machine_texture(ASSETS/"textures/block/extreme_dragon_assembler.png",False)
machine_texture(ASSETS/"textures/block/dragon_pattern_encoder.png",True)
pattern_texture(ASSETS/"textures/item/blank_dragon_pattern.png",False)
pattern_texture(ASSETS/"textures/item/encoded_dragon_pattern.png",True)

for name in ("extreme_dragon_assembler","dragon_pattern_encoder"):
    write_json(ASSETS/f"blockstates/{name}.json", {"variants":{"":{"model":f"metatech_reborn:block/{name}"}}})
    write_json(ASSETS/f"models/block/{name}.json", {"parent":"minecraft:block/cube_all","textures":{"all":f"metatech_reborn:block/{name}"}})
    write_json(ASSETS/f"models/item/{name}.json", {"parent":f"metatech_reborn:block/{name}","display":{"gui":{"rotation":[30,225,0],"scale":[0.82,0.82,0.82]}}})
    write_json(DATA/f"loot_tables/blocks/{name}.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"metatech_reborn:{name}"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})

for name in ("blank_dragon_pattern","encoded_dragon_pattern"):
    write_json(ASSETS/f"models/item/{name}.json", {"parent":"minecraft:item/generated","textures":{"layer0":f"metatech_reborn:item/{name}"}})

cond=[{"type":"forge:mod_loaded","modid":"draconicevolution"}]
write_json(DATA/"recipes/blank_dragon_pattern.json", {
    "type":"minecraft:crafting_shaped","conditions":cond,"pattern":["DRD","RPR","DRD"],
    "key":{"D":{"item":"draconicevolution:draconium_ingot"},"R":{"item":"minecraft:redstone"},"P":{"item":"metatech_reborn:blank_extreme_pattern"}},
    "result":{"item":"metatech_reborn:blank_dragon_pattern","count":4}})
write_json(DATA/"recipes/dragon_pattern_encoder.json", {
    "type":"minecraft:crafting_shaped","conditions":cond,"pattern":["DCD","RER","DCD"],
    "key":{"D":{"item":"draconicevolution:draconium_ingot"},"C":{"item":"draconicevolution:draconium_core"},"R":{"item":"minecraft:redstone"},"E":{"item":"metatech_reborn:extreme_pattern_encoder"}},
    "result":{"item":"metatech_reborn:dragon_pattern_encoder"}})
write_json(DATA/"recipes/extreme_dragon_assembler.json", {
    "type":"minecraft:crafting_shaped","conditions":cond,"pattern":["ACA","CMC","ACA"],
    "key":{"A":{"item":"draconicevolution:awakened_draconium_ingot"},"C":{"item":"draconicevolution:awakened_core"},"M":{"item":"metatech_reborn:molecular_assembler_9x9"}},
    "result":{"item":"metatech_reborn:extreme_dragon_assembler"}})

patch_lang("ru_ru", {
    "block.metatech_reborn.extreme_dragon_assembler":"Экстремальный сборщик дракона",
    "item.metatech_reborn.extreme_dragon_assembler":"Экстремальный сборщик дракона",
    "container.metatech_reborn.extreme_dragon_assembler":"Экстремальный сборщик дракона",
    "block.metatech_reborn.dragon_pattern_encoder":"Кодировщик драконьих шаблонов",
    "item.metatech_reborn.dragon_pattern_encoder":"Кодировщик драконьих шаблонов",
    "container.metatech_reborn.dragon_pattern_encoder":"Кодировщик драконьих шаблонов",
    "item.metatech_reborn.blank_dragon_pattern":"Пустой драконий шаблон",
    "item.metatech_reborn.encoded_dragon_pattern":"Закодированный драконий шаблон",
    "item.metatech_reborn.encoded_dragon_pattern.named":"Драконий шаблон: %s",
    "tooltip.metatech_reborn.dragon_pattern.invalid":"Шаблон повреждён или не закодирован",
    "tooltip.metatech_reborn.dragon_pattern.output":"Результат: %s",
    "tooltip.metatech_reborn.dragon_pattern.tier":"Тир крафта: %s",
    "tooltip.metatech_reborn.dragon_pattern.energy":"Энергия рецепта: %s FE",
    "gui.metatech_reborn.dragon.tier.none":"Нет",
    "gui.metatech_reborn.dragon.tier.basic":"Обычный",
    "gui.metatech_reborn.dragon.tier.wyvern":"Виверна",
    "gui.metatech_reborn.dragon.tier.draconic":"Драконий",
    "gui.metatech_reborn.dragon.tier.chaotic":"Хаос",
    "gui.metatech_reborn.dragon.injectors":"Инжекторы 12/12",
    "gui.metatech_reborn.dragon.inputs":"Ингредиенты fusion-рецепта",
    "gui.metatech_reborn.dragon.pattern_bank":"Банк драконьих шаблонов",
    "gui.metatech_reborn.dragon.machine_tier":"Уровень сборщика: %s",
    "gui.metatech_reborn.dragon.recipe_tier":"Уровень рецепта: %s",
    "gui.metatech_reborn.dragon.status.idle":"Ожидание шаблона и ресурсов",
    "gui.metatech_reborn.dragon.status.injectors":"Установите все 12 инжекторов",
    "gui.metatech_reborn.dragon.status.tier":"Уровень инжекторов слишком низкий",
    "gui.metatech_reborn.dragon.status.input":"Не хватает ингредиентов",
    "gui.metatech_reborn.dragon.status.energy":"Недостаточно энергии",
    "gui.metatech_reborn.dragon.status.output":"Выход занят",
    "gui.metatech_reborn.dragon.status.running":"Fusion-крафт выполняется",
    "gui.metatech_reborn.dragon_encoder.encode":"Кодировать",
    "gui.metatech_reborn.dragon_encoder.clear":"Очистить",
    "gui.metatech_reborn.dragon_encoder.catalyst":"Катализатор",
    "gui.metatech_reborn.dragon_encoder.ingredients":"12 позиций инжекторов",
    "gui.metatech_reborn.dragon_encoder.result":"Результат",
    "gui.metatech_reborn.dragon_encoder.status.idle":"Выберите рецепт в JEI",
    "gui.metatech_reborn.dragon_encoder.status.ready":"Рецепт загружен",
    "gui.metatech_reborn.dragon_encoder.status.encoded":"Шаблон закодирован",
    "gui.metatech_reborn.dragon_encoder.status.recipe":"Рецепт недоступен",
    "gui.metatech_reborn.dragon_encoder.status.blank":"Нужен пустой шаблон",
    "gui.metatech_reborn.dragon_encoder.status.output":"Заберите готовый шаблон",
    "jei.metatech_reborn.extreme_dragon_fusion":"Экстремальный Fusion-крафт дракона",
    "jei.metatech_reborn.dragon.energy":"Энергия: %s FE"
})
patch_lang("en_us", {
    "block.metatech_reborn.extreme_dragon_assembler":"Extreme Dragon Assembler",
    "item.metatech_reborn.extreme_dragon_assembler":"Extreme Dragon Assembler",
    "container.metatech_reborn.extreme_dragon_assembler":"Extreme Dragon Assembler",
    "block.metatech_reborn.dragon_pattern_encoder":"Dragon Pattern Encoder",
    "item.metatech_reborn.dragon_pattern_encoder":"Dragon Pattern Encoder",
    "container.metatech_reborn.dragon_pattern_encoder":"Dragon Pattern Encoder",
    "item.metatech_reborn.blank_dragon_pattern":"Blank Dragon Pattern",
    "item.metatech_reborn.encoded_dragon_pattern":"Encoded Dragon Pattern",
    "item.metatech_reborn.encoded_dragon_pattern.named":"Dragon Pattern: %s",
    "tooltip.metatech_reborn.dragon_pattern.invalid":"Pattern is invalid or not encoded",
    "tooltip.metatech_reborn.dragon_pattern.output":"Output: %s",
    "tooltip.metatech_reborn.dragon_pattern.tier":"Craft tier: %s",
    "tooltip.metatech_reborn.dragon_pattern.energy":"Recipe energy: %s FE",
    "gui.metatech_reborn.dragon.tier.none":"None",
    "gui.metatech_reborn.dragon.tier.basic":"Basic",
    "gui.metatech_reborn.dragon.tier.wyvern":"Wyvern",
    "gui.metatech_reborn.dragon.tier.draconic":"Draconic",
    "gui.metatech_reborn.dragon.tier.chaotic":"Chaotic",
    "gui.metatech_reborn.dragon.injectors":"Injectors 12/12",
    "gui.metatech_reborn.dragon.inputs":"Fusion recipe inputs",
    "gui.metatech_reborn.dragon.pattern_bank":"Dragon pattern bank",
    "gui.metatech_reborn.dragon.machine_tier":"Assembler tier: %s",
    "gui.metatech_reborn.dragon.recipe_tier":"Recipe tier: %s",
    "gui.metatech_reborn.dragon.status.idle":"Waiting for pattern and resources",
    "gui.metatech_reborn.dragon.status.injectors":"Install all 12 injectors",
    "gui.metatech_reborn.dragon.status.tier":"Injector tier is too low",
    "gui.metatech_reborn.dragon.status.input":"Missing ingredients",
    "gui.metatech_reborn.dragon.status.energy":"Not enough energy",
    "gui.metatech_reborn.dragon.status.output":"Output is blocked",
    "gui.metatech_reborn.dragon.status.running":"Fusion craft running",
    "gui.metatech_reborn.dragon_encoder.encode":"Encode",
    "gui.metatech_reborn.dragon_encoder.clear":"Clear",
    "gui.metatech_reborn.dragon_encoder.catalyst":"Catalyst",
    "gui.metatech_reborn.dragon_encoder.ingredients":"12 injector positions",
    "gui.metatech_reborn.dragon_encoder.result":"Result",
    "gui.metatech_reborn.dragon_encoder.status.idle":"Select a JEI recipe",
    "gui.metatech_reborn.dragon_encoder.status.ready":"Recipe loaded",
    "gui.metatech_reborn.dragon_encoder.status.encoded":"Pattern encoded",
    "gui.metatech_reborn.dragon_encoder.status.recipe":"Recipe unavailable",
    "gui.metatech_reborn.dragon_encoder.status.blank":"Blank pattern required",
    "gui.metatech_reborn.dragon_encoder.status.output":"Take encoded pattern first",
    "jei.metatech_reborn.extreme_dragon_fusion":"Extreme Dragon Fusion",
    "jei.metatech_reborn.dragon.energy":"Energy: %s FE"
})
print("Installed 0.6.94 red Draconic assembler/encoder assets")
