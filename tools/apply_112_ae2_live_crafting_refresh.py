from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# ---------------------------------------------------------------------------
# Molecular Assembler 9x9 AE2 provider
# ---------------------------------------------------------------------------
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/integration/ae2/MolecularAssemblerAe2Provider.java"
text = path.read_text(encoding="utf-8")

text = replace_once(
    text,
    "    private long lastPatternRefresh = Long.MIN_VALUE;\n    private boolean invalid;",
    "    private long lastPatternRefresh = -20L;\n"
    "    private long lastCraftingRefresh = -200L;\n"
    "    private boolean craftingRefreshPending = true;\n"
    "    private boolean invalid;",
    "molecular refresh timer fields"
)

text = replace_once(
    text,
    """                public void onSaveChanges(MolecularAssemblerAe2Provider owner, IGridNode node) {\n                    owner.host.setChanged();\n                }\n""",
    """                public void onSaveChanges(MolecularAssemblerAe2Provider owner, IGridNode node) {\n                    owner.host.setChanged();\n                }\n\n                @Override\n                public void onInWorldConnectionChanged(MolecularAssemblerAe2Provider owner, IGridNode node) {\n                    owner.craftingRefreshPending = true;\n                }\n\n                @Override\n                public void onGridChanged(MolecularAssemblerAe2Provider owner, IGridNode node) {\n                    owner.craftingRefreshPending = true;\n                }\n\n                @Override\n                public void onStateChanged(MolecularAssemblerAe2Provider owner, IGridNode node,\n                                           IGridNodeListener.State state) {\n                    owner.craftingRefreshPending = true;\n                }\n""",
    "molecular node listener refresh callbacks"
)

text = replace_once(
    text,
    """        if (gameTime - lastPatternRefresh >= 20L) {\n            lastPatternRefresh = gameTime;\n            refreshPatterns(true);\n        }\n        returnOutputToNetwork();\n""",
    """        if (gameTime - lastPatternRefresh >= 20L) {\n            lastPatternRefresh = gameTime;\n            refreshPatterns(true);\n        }\n        if (gameTime - lastCraftingRefresh >= 200L) {\n            craftingRefreshPending = true;\n        }\n        requestCraftingRefreshIfOnline();\n        returnOutputToNetwork();\n""",
    "molecular live crafting refresh tick"
)

text = replace_once(
    text,
    """        managedNode.create(level, host.getBlockPos());\n        refreshPatterns(false);\n    }\n\n    private void refreshPatterns(boolean notifyGrid) {\n""",
    """        managedNode.create(level, host.getBlockPos());\n        refreshPatterns(false);\n        craftingRefreshPending = true;\n    }\n\n    private void refreshPatterns(boolean notifyGrid) {\n""",
    "molecular initial refresh pending"
)

text = replace_once(
    text,
    """        if (definitions.equals(cachedDefinitions)) return;\n        cachedDefinitions = List.copyOf(definitions);\n        cachedPatterns = List.copyOf(patterns);\n        if (notifyGrid && managedNode != null && managedNode.isReady()) {\n            ICraftingProvider.requestUpdate(managedNode);\n        }\n    }\n\n    private void returnOutputToNetwork() {\n""",
    """        if (definitions.equals(cachedDefinitions)) return;\n        cachedDefinitions = List.copyOf(definitions);\n        cachedPatterns = List.copyOf(patterns);\n        craftingRefreshPending = true;\n        if (notifyGrid) {\n            requestCraftingRefreshIfOnline();\n        }\n    }\n\n    private void requestCraftingRefreshIfOnline() {\n        if (!craftingRefreshPending || managedNode == null || !managedNode.isReady()\n                || !managedNode.isActive() || !managedNode.hasGridBooted()) {\n            return;\n        }\n        ICraftingProvider.requestUpdate(managedNode);\n        craftingRefreshPending = false;\n        Level level = host.getLevel();\n        if (level != null) {\n            lastCraftingRefresh = level.getGameTime();\n        }\n    }\n\n    private void returnOutputToNetwork() {\n""",
    "molecular request update helper"
)

path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Extreme Dragon Assembler AE2 provider (generated earlier in the hotfix chain)
# ---------------------------------------------------------------------------
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/integration/ae2/ExtremeDragonAssemblerAe2Provider.java"
text = path.read_text(encoding="utf-8")

text = replace_once(
    text,
    "    private long lastRefresh = Long.MIN_VALUE;\n    private boolean invalid;",
    "    private long lastRefresh = -20L;\n"
    "    private long lastCraftingRefresh = -200L;\n"
    "    private boolean craftingRefreshPending = true;\n"
    "    private boolean invalid;",
    "dragon refresh timer fields"
)

text = replace_once(
    text,
    """        @Override public void onSaveChanges(ExtremeDragonAssemblerAe2Provider owner, IGridNode node) {\n            owner.host.setChanged();\n        }\n""",
    """        @Override public void onSaveChanges(ExtremeDragonAssemblerAe2Provider owner, IGridNode node) {\n            owner.host.setChanged();\n        }\n        @Override public void onInWorldConnectionChanged(ExtremeDragonAssemblerAe2Provider owner, IGridNode node) {\n            owner.craftingRefreshPending = true;\n        }\n        @Override public void onGridChanged(ExtremeDragonAssemblerAe2Provider owner, IGridNode node) {\n            owner.craftingRefreshPending = true;\n        }\n        @Override public void onStateChanged(ExtremeDragonAssemblerAe2Provider owner, IGridNode node,\n                                             IGridNodeListener.State state) {\n            owner.craftingRefreshPending = true;\n        }\n""",
    "dragon node listener refresh callbacks"
)

text = replace_once(
    text,
    """        long time = level.getGameTime();\n        if (time - lastRefresh >= 20L) {\n            lastRefresh = time;\n            refreshPatterns(true);\n        }\n        returnOutputToNetwork();\n""",
    """        long time = level.getGameTime();\n        if (time - lastRefresh >= 20L) {\n            lastRefresh = time;\n            connectAdjacent(level);\n            refreshPatterns(true);\n        }\n        if (time - lastCraftingRefresh >= 200L) {\n            craftingRefreshPending = true;\n        }\n        requestCraftingRefreshIfOnline();\n        returnOutputToNetwork();\n""",
    "dragon live crafting refresh tick"
)

text = replace_once(
    text,
    """        managedNode.create(level, host.getBlockPos());\n        refreshPatterns(false);\n    }\n\n    private void refreshPatterns(boolean notify) {\n""",
    """        managedNode.create(level, host.getBlockPos());\n        refreshPatterns(false);\n        craftingRefreshPending = true;\n    }\n\n    private void connectAdjacent(Level level) {\n        if (managedNode == null) return;\n        IGridNode ownNode = managedNode.getNode();\n        if (ownNode == null) return;\n        for (Direction direction : Direction.values()) {\n            IGridNode adjacentNode = GridHelper.getExposedNode(\n                    level, host.getBlockPos().relative(direction), direction.getOpposite());\n            if (adjacentNode == null || adjacentNode == ownNode) continue;\n            try {\n                GridHelper.createConnection(ownNode, adjacentNode);\n            } catch (IllegalStateException ignored) {\n                // Already connected, incompatible, or AE2 completed it during this tick.\n            }\n        }\n    }\n\n    private void refreshPatterns(boolean notify) {\n""",
    "dragon connection fallback"
)

text = replace_once(
    text,
    """        if (definitions.equals(cachedDefinitions)) return;\n        cachedDefinitions = List.copyOf(definitions);\n        cachedPatterns = List.copyOf(patterns);\n        if (notify && managedNode != null && managedNode.isReady()) ICraftingProvider.requestUpdate(managedNode);\n    }\n\n    @Override public List<IPatternDetails> getAvailablePatterns() {\n""",
    """        if (definitions.equals(cachedDefinitions)) return;\n        cachedDefinitions = List.copyOf(definitions);\n        cachedPatterns = List.copyOf(patterns);\n        craftingRefreshPending = true;\n        if (notify) requestCraftingRefreshIfOnline();\n    }\n\n    private void requestCraftingRefreshIfOnline() {\n        if (!craftingRefreshPending || managedNode == null || !managedNode.isReady()\n                || !managedNode.isActive() || !managedNode.hasGridBooted()) {\n            return;\n        }\n        ICraftingProvider.requestUpdate(managedNode);\n        craftingRefreshPending = false;\n        Level level = host.getLevel();\n        if (level != null) lastCraftingRefresh = level.getGameTime();\n    }\n\n    @Override public List<IPatternDetails> getAvailablePatterns() {\n""",
    "dragon request update helper"
)

path.write_text(text, encoding="utf-8")

print("Applied 0.6.112 live AE2 crafting-provider refresh fixes")
