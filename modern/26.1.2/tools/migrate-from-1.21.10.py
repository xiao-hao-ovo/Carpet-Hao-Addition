"""26.2 迁移助手:把 versions/1.21.10 的版本层源码复制到 modern/26.2 并迁移到 Mojang 官方名。

用法: python tools/migrate-from-1.21.10.py
(一次性工具,迁移完成后可删除)
"""
import pathlib
import re
import shutil

SRC = pathlib.Path(__file__).resolve().parents[3] / "versions" / "1.21.10" / "src" / "main" / "java" / "carpet_hao_addition"
DST = pathlib.Path(__file__).resolve().parents[1] / "src" / "main" / "java" / "carpet_hao_addition"

# 共享层文件不覆盖(它们已在 26.2 侧迁移好)
def should_skip(rel: pathlib.Path) -> bool:
    return rel.name == "CarpetHaoAdditionExtension.java" \
        or rel.name.endswith("Settings.java") \
        or rel.as_posix() == "portal/PlayerNoEndPortalTeleportList.java"

FQ = {
    "net.minecraft.block.AbstractBlock": "net.minecraft.world.level.block.state.BlockBehaviour",
    "net.minecraft.block.Block": "net.minecraft.world.level.block.Block",
    "net.minecraft.block.BlockState": "net.minecraft.world.level.block.state.BlockState",
    "net.minecraft.block.Blocks": "net.minecraft.world.level.block.Blocks",
    "net.minecraft.block.ComposterBlock": "net.minecraft.world.level.block.ComposterBlock",
    "net.minecraft.block.EndPortalBlock": "net.minecraft.world.level.block.EndPortalBlock",
    "net.minecraft.block.FacingBlock": "net.minecraft.world.level.block.DirectionalBlock",
    "net.minecraft.block.FluidBlock": "net.minecraft.world.level.block.LiquidBlock",
    "net.minecraft.block.ObserverBlock": "net.minecraft.world.level.block.ObserverBlock",
    "net.minecraft.block.entity.BlockEntity": "net.minecraft.world.level.block.entity.BlockEntity",
    "net.minecraft.command.argument.BlockPosArgumentType": "net.minecraft.commands.arguments.coordinates.BlockPosArgument",
    "net.minecraft.command.argument.EntityArgumentType": "net.minecraft.commands.arguments.EntityArgument",
    "net.minecraft.command.argument.GameProfileArgumentType": "net.minecraft.commands.arguments.GameProfileArgument",
    "net.minecraft.datafixer.DataFixTypes": "net.minecraft.util.datafix.DataFixTypes",
    "net.minecraft.entity.Entity": "net.minecraft.world.entity.Entity",
    "net.minecraft.entity.EntityCollisionHandler": "net.minecraft.world.entity.InsideBlockEffectApplier",
    "net.minecraft.entity.EquipmentSlot": "net.minecraft.world.entity.EquipmentSlot",
    "net.minecraft.entity.LivingEntity": "net.minecraft.world.entity.LivingEntity",
    "net.minecraft.entity.mob.MobEntity": "net.minecraft.world.entity.Mob",
    "net.minecraft.entity.mob.WitherSkeletonEntity": "net.minecraft.world.entity.monster.skeleton.WitherSkeleton",
    "net.minecraft.entity.player.PlayerEntity": "net.minecraft.world.entity.player.Player",
    "net.minecraft.fluid.FluidState": "net.minecraft.world.level.material.FluidState",
    "net.minecraft.fluid.LavaFluid": "net.minecraft.world.level.material.LavaFluid",
    "net.minecraft.item.Item": "net.minecraft.world.item.Item",
    "net.minecraft.item.ItemStack": "net.minecraft.world.item.ItemStack",
    "net.minecraft.item.Items": "net.minecraft.world.item.Items",
    "net.minecraft.registry.RegistryKey": "net.minecraft.resources.ResourceKey",
    "net.minecraft.registry.RegistryKeys": "net.minecraft.core.registries.Registries",
    "net.minecraft.registry.entry.RegistryEntry": "net.minecraft.core.Holder",
    "net.minecraft.resource.ResourcePackManager": "net.minecraft.server.packs.repository.PackRepository",
    "net.minecraft.screen.StonecutterScreenHandler": "net.minecraft.world.inventory.StonecutterMenu",
    "net.minecraft.server.PlayerConfigEntry": "net.minecraft.server.players.NameAndId",
    "net.minecraft.server.command.CommandManager": "net.minecraft.commands.Commands",
    "net.minecraft.server.command.ServerCommandSource": "net.minecraft.commands.CommandSourceStack",
    "net.minecraft.server.network.ServerPlayerEntity": "net.minecraft.server.level.ServerPlayer",
    "net.minecraft.server.network.ServerPlayerInteractionManager": "net.minecraft.server.level.ServerPlayerGameMode",
    "net.minecraft.server.world.ServerWorld": "net.minecraft.server.level.ServerLevel",
    "net.minecraft.text.Text": "net.minecraft.network.chat.Component",
    "net.minecraft.util.ActionResult": "net.minecraft.world.InteractionResult",
    "net.minecraft.util.Hand": "net.minecraft.world.InteractionHand",
    "net.minecraft.util.WorldSavePath": "net.minecraft.world.level.storage.LevelResource",
    "net.minecraft.util.hit.BlockHitResult": "net.minecraft.world.phys.BlockHitResult",
    "net.minecraft.util.math.BlockPos": "net.minecraft.core.BlockPos",
    "net.minecraft.util.math.ChunkSectionPos": "net.minecraft.core.SectionPos",
    "net.minecraft.util.math.Direction": "net.minecraft.core.Direction",
    "net.minecraft.util.math.random.Random": "net.minecraft.util.RandomSource",
    "net.minecraft.world.BlockView": "net.minecraft.world.level.BlockGetter",
    "net.minecraft.world.PersistentState": "net.minecraft.world.level.saveddata.SavedData",
    "net.minecraft.world.PersistentStateType": "net.minecraft.world.level.saveddata.SavedDataType",
    "net.minecraft.world.World": "net.minecraft.world.level.Level",
    "net.minecraft.world.WorldAccess": "net.minecraft.world.level.LevelAccessor",
    "net.minecraft.world.WorldEvents": "net.minecraft.world.level.block.LevelEvent",
    "net.minecraft.world.WorldView": "net.minecraft.world.level.LevelReader",
    "net.minecraft.world.biome.Biome": "net.minecraft.world.level.biome.Biome",
    "net.minecraft.world.chunk.ChunkSection": "net.minecraft.world.level.chunk.LevelChunkSection",
    "net.minecraft.world.chunk.WorldChunk": "net.minecraft.world.level.chunk.LevelChunk",
    "net.minecraft.world.tick.OrderedTick": "net.minecraft.world.ticks.ScheduledTick",
    "net.minecraft.world.tick.ScheduledTickView": "net.minecraft.world.ticks.LevelTickAccess",
}

SIMPLE = {
    "AbstractBlock": "BlockBehaviour", "FacingBlock": "DirectionalBlock", "FluidBlock": "LiquidBlock",
    "BlockPosArgumentType": "BlockPosArgument", "EntityArgumentType": "EntityArgument",
    "GameProfileArgumentType": "GameProfileArgument", "EntityCollisionHandler": "InsideBlockEffectApplier",
    "MobEntity": "Mob", "WitherSkeletonEntity": "WitherSkeleton", "PlayerEntity": "Player",
    "RegistryKey": "ResourceKey", "RegistryKeys": "Registries", "RegistryEntry": "Holder",
    "ResourcePackManager": "PackRepository", "StonecutterScreenHandler": "StonecutterMenu",
    "PlayerConfigEntry": "NameAndId", "CommandManager": "Commands", "ServerCommandSource": "CommandSourceStack",
    "ServerPlayerEntity": "ServerPlayer", "ServerPlayerInteractionManager": "ServerPlayerGameMode",
    "ServerWorld": "ServerLevel", "Text": "Component", "ActionResult": "InteractionResult",
    "Hand": "InteractionHand", "BlockView": "BlockGetter", "WorldSavePath": "LevelResource",
    "PersistentStateType": "SavedDataType", "PersistentState": "SavedData",
    "WorldAccess": "LevelAccessor", "WorldEvents": "LevelEvent", "WorldView": "LevelReader",
    "ChunkSectionPos": "SectionPos", "ChunkSection": "LevelChunkSection", "WorldChunk": "LevelChunk",
    "OrderedTick": "ScheduledTick", "ScheduledTickView": "LevelTickAccess", "World": "Level",
}


def main() -> None:
    copied = 0
    for p in SRC.rglob("*.java"):
        rel = p.relative_to(SRC)
        if should_skip(rel):
            continue
        dst = DST / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(p, dst)
        copied += 1

    changed = 0
    for p in DST.rglob("*.java"):
        text0 = text = p.read_text(encoding="utf-8")
        for a in sorted(FQ, key=len, reverse=True):
            pattern = r"(?<![\w.])" + re.escape(a) + r"(?![\w])"
            text = re.sub(pattern, lambda _m, rep=FQ[a]: rep, text)
        for a in sorted(SIMPLE, key=len, reverse=True):
            text = re.sub(r"\b" + a + r"\b", SIMPLE[a], text)
        if text != text0:
            p.write_text(text, encoding="utf-8")
            changed += 1

    print(f"复制版本层文件: {copied};迁移改写文件: {changed}")

    leftovers = []
    yarn_prefixes = (
        "net.minecraft.block", "net.minecraft.entity.mob", "net.minecraft.item.", "net.minecraft.server.world",
        "net.minecraft.server.network", "net.minecraft.server.command", "net.minecraft.util.math",
        "net.minecraft.util.hit", "net.minecraft.util.ActionResult", "net.minecraft.util.Hand",
        "net.minecraft.text.", "net.minecraft.world.World", "net.minecraft.world.BlockView",
        "net.minecraft.registry.", "net.minecraft.resource.", "net.minecraft.screen.",
    )
    for p in DST.rglob("*.java"):
        for line in p.read_text(encoding="utf-8").splitlines():
            s = line.strip()
            if s.startswith("import net.minecraft.") and s.split()[1].startswith(yarn_prefixes):
                leftovers.append(f"{p.name}: {s}")
    print(f"残留待迁移 import: {len(leftovers)}")
    for item in leftovers[:10]:
        print("  " + item)


if __name__ == "__main__":
    main()
