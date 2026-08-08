from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Keep the existing screen compile fix.
screen_path = ROOT / "src/main/java/ru/rfvv/metatechreborn/client/screen/ExtremeDragonAssemblerScreen.java"
screen_text = screen_path.read_text(encoding="utf-8")
old = 'g.drawString(font, inventory.getDisplayName(), inventoryLabelX, inventoryLabelY, 0xFF6D1414, false);'
new = 'g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF6D1414, false);'
if old in screen_text:
    screen_path.write_text(screen_text.replace(old, new, 1), encoding="utf-8")
elif new not in screen_text:
    raise RuntimeError("dragon screen inventory-label compile fix: target line not found")


def rename_event_listener(path: Path, old_register: str, new_register: str,
                          old_class: str, new_class: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    changed = False
    if old_register in text:
        text = text.replace(old_register, new_register, 1)
        changed = True
    elif new_register not in text:
        raise RuntimeError(f"{label}: listener registration target not found")

    if old_class in text:
        text = text.replace(old_class, new_class, 1)
        changed = True
    elif new_class not in text:
        raise RuntimeError(f"{label}: listener class target not found")

    if changed:
        path.write_text(text, encoding="utf-8")


# Forge generates ASM event-handler classes from the listener class simple name.
# Both providers previously used a nested class literally named `Events`, so Forge generated
# the same __Events_levelTick_LevelTickEvent handler and later tried to cast the dragon
# listener instance to the molecular listener class. Give each listener a unique class name.
rename_event_listener(
    ROOT / "src/main/java/ru/rfvv/metatechreborn/integration/ae2/MolecularAssemblerAe2Provider.java",
    "MinecraftForge.EVENT_BUS.register(new Events());",
    "MinecraftForge.EVENT_BUS.register(new MolecularAssemblerEvents());",
    "private static final class Events {",
    "private static final class MolecularAssemblerEvents {",
    "molecular AE2 listener rename",
)

rename_event_listener(
    ROOT / "src/main/java/ru/rfvv/metatechreborn/integration/ae2/ExtremeDragonAssemblerAe2Provider.java",
    "MinecraftForge.EVENT_BUS.register(new Events());",
    "MinecraftForge.EVENT_BUS.register(new DragonAssemblerEvents());",
    "private static final class Events {",
    "private static final class DragonAssemblerEvents {",
    "dragon AE2 listener rename",
)

print("Applied dragon screen compile fix and unique AE2 event listener names")
