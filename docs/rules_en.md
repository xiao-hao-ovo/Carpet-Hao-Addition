# Rules

**Tip: use `Ctrl+F` to quickly find the rule you are looking for**

All rules are registered in Carpet's default settings manager. Set them with `/carpet <rule> <value>`.

## Auto mending (new) (haoAutoMending)

Ported from Carpet WuHu Addition. Once a second, automatically spend the XP already accumulated in the level bar to repair damaged gear enchanted with Mending, repairing as much as that XP allows and deducting the same amount of XP (no free durability). Off by default.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Copper stonecutting recipes (haoCopperStonecutting)

Lets the stonecutter convert copper variants by equal material value: any variant can be cut in one step into any shape (cut/chiseled/grate/slab/stairs/door/trapdoor/bulb), equal-value shapes convert 1:1, downgrades use the exact ratio (copper block -> cut x4, -> slab x8). Strictly value-preserving, no infinite resource loop. Off by default.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Direct Block Drops (haoDirectBlockDrops)

While enabled, drops produced by blocks the player breaks (including chain breaks, drops of blocks that lost support in the same tick, and items released from broken containers) go straight into that player's inventory; whatever does not fit still drops in the world as usual.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Direct Entity Drops (haoDirectEntityDrops)

While enabled, drops produced by killing an entity (mobs and armor stands, as well as minecarts, boats, paintings and item frames) go straight into that player's inventory; whatever does not fit still drops in the world as usual.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Easy Place Entities (haoProjectionEntity)

While following a schematic with easy place, also spawns the schematic's entities (minecarts, boats, armor stands, item frames, paintings...) and consumes the matching items. Only entities with a matching item are supported; count is set by easyPlaceEntityCount. Spawns nothing if items are short. Off by default.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Easy Place Fill (haoProjectionWaterlogged)

While following a schematic with easy place, fills in the schematic's water/lava/lava cauldron/fire: water consumes ice, lava consumes a magma block, igniting prefers a fire charge. Values: false = off; true = inventory only; shulker_direct = take from inside a shulker box; shulker_take = move the material into the inventory. If short on materials, only the block itself is placed. Requires Litematica.

- Type: `Mode`
- Default: `FALSE`
- Options: `FALSE`, `TRUE`, `SHULKER_DIRECT`, `SHULKER_TAKE`
- Categories: `Hao`

## Easy Place Fill Trigger (haoProjectionWaterloggedTrigger)

When the fill feature is allowed to run: always = both poses, standing or sneaking (default); standing = only while the player is not sneaking; sneaking = only while the player is sneaking (holding the sneak key).

- Type: `Trigger`
- Default: `ALWAYS`
- Options: `STANDING`, `SNEAKING`, `ALWAYS`
- Categories: `Hao`

## Example Boolean Rule (exampleBoolean)

Example boolean rule shipped with the Carpet extension template; no gameplay effect.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `haoaddition`

## Example String Rule (exampleString)

Example string rule shipped with the Carpet extension template (foo / bar / baz); no gameplay effect.

- Type: `String`
- Default: `foo`
- Options: `foo`, `bar`, `baz`
- Categories: `haoaddition`

## Golden Carrot Composting (haoGoldenCarrotCompost)

While enabled, right-clicking a composter while holding a golden carrot composts it with 100% chance, exactly like a regular compostable item.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Bedrock Can Be Mined (haoBedrockMines)

While enabled, bedrock can be mined (it mines like obsidian) and drops itself as an item when broken; while disabled, vanilla behavior is restored.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Projection Placement (haoProjectionPlacement)

When building from a schematic, places blocks with complex states (facing, toggle, color, block-entity data...) exactly as the schematic specifies. Requires Litematica. For waterlogged blocks use haoProjectionWaterlogged. Values: false = off (default); true = on, but a composter's fill level is not restored; with_composter_level = on, restores a composter's fill level too.

- Type: `Mode`
- Default: `FALSE`
- Options: `FALSE`, `TRUE`, `WITH_COMPOSTER_LEVEL`
- Categories: `Hao`

## Craftable Trim Templates (haoCraftableTrimTemplates)

When on, all 19 trim/upgrade smithing templates become craftable: 7 diamonds + 1 material taken from that template's original duplicating recipe (shipwreck -> cobblestone, desert temple -> sandstone, trail ruins -> terracotta, ...). Off by default.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Enhanced World Eater ProMax (haoEnhancedWorldEaterProMax)

Lets waterlogged blocks be destroyed by explosions. Vanilla takes max(block resistance, fluid resistance) and water has a fluid resistance of 100, so waterlogged slabs/coral fans etc. survive TNT; when on, water's fluid resistance counts as 0. This only covers waterlogged blocks - for high-resistance blocks (obsidian/iron block/...) use 其它扩展's enhancedWorldEater. Off by default.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Wacko Beacons (client) (haoWackoBeacons)

Client-side beacon unlock: Regeneration selectable as a primary effect, and effect buttons are no longer disabled by beacon tier. Ported from wacko-beacons.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Lava Depth Strider (haoLavaDepthStrider)

While enabled, moving through lava no longer slows you down if you have Depth Strider: the water movement formula (and its water_movement_efficiency attribute) is applied to lava as well, so lava feels exactly like wearing Depth Strider boots in water. Higher enchantment levels move faster.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## No End Portal Teleport (haoNoEndPortalTeleport)

Enables the /playerNoEndPortalTeleport list management. The rule itself does not change teleporting: only players added to the blacklist (or with globalMode enabled) are not teleported by end portals; non-player entities are unaffected.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Rocket Shulker Auto Refill (haoRocketShulker)

For a shulker box named exactly 'rocket': when the chosen slot runs out of fireworks, refills one stack (up to 64) from the box, repeating until the box is empty. Set the slot with /haoRocketShulker offhand (default) or /haoRocketShulker mainhand <1-9>; kept in memory only. Never touches a slot holding something else. Off by default.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Snowy Calcite (haoSnowyCalcite)

While enabled, stone/cobblestone produced by lava+water generators in snowy (snow-precipitating) biomes becomes calcite.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Terracotta Uncolor (haoTerracottaUncolor)

While enabled, the stonecutter can revert all 16 dyed terracotta to plain terracotta and all 16 glazed terracotta to their dyed terracotta; while disabled, these recipes do not appear in the stonecutter at all.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Use dye on shulker box (haoUseDyeOnShulkerBox)

Right-click a shulker box with dye to recolour it; sneak + right-click a coloured box while holding a cactus to wash it back to plain (the cactus is not consumed). Contents and custom name are preserved. Off by default.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`

## Wither Skeleton Drop Reduction (haoWitherSkeletonDrop)

Removes selected drops from wither skeletons. Options: false=vanilla; bone=no bones; coal=no coal; skull=no wither skeleton skull; sword=no dropped stone sword; all=remove all of the above.

- Type: `String`
- Default: `false`
- Options: `false`, `bone`, `coal`, `skull`, `sword`, `all`
- Categories: `Hao`

## haoZoneguard (haoZoneguard)

Disables observers inside the cubic regions configured with /haoZoneguard. Turning the rule off restores observers in those regions.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `Hao`
