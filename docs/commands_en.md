# Commands

Commands are registered in Carpet's default command tree. Except for `/hao`, every command is **gated by its rule**: while the rule is off, the whole command tree (including `help`) is invisible and unusable.

## Example rules (`/hao`)

### Syntax

`/hao`

### Effect

- Prints the current values of the two example rules (`exampleBoolean` / `exampleString`)

## Zone Guard (`/haoZoneguard`)

Requires `/carpet haoZoneguard true`; while the rule is off the whole command tree (including `help`) is invisible.

### Syntax

- `/haoZoneguard set <id> <from> <to>` — define a cubic disabled region
- `/haoZoneguard view` — list the configured regions
- `/haoZoneguard clear <id>` — remove a region
- `/haoZoneguard op add|remove <player>` — add/remove a whitelisted player
- `/haoZoneguard op list` — show the whitelist
- `/haoZoneguard help` — show usage

### Effect

- `set` turns the cuboid between the two opposite corners `<from>` and `<to>` into a disabled region (the corners are normalised, so order does not matter). Regions persist with the world save.
- `clear` deletes the region and then schedules one tick for every "face-to-face observer pair" stuck in the loaded chunks of that region, so they resume working (the feedback reports how many pairs were revived; `0` means none were loaded).
- See the output of `/haoZoneguard help` for the exact shorthand forms.

## No End Portal Teleport (`/playerNoEndPortalTeleport`)

Visible and usable while the rule `/carpet haoNoEndPortalTeleport true` is on.

### Syntax

- `/playerNoEndPortalTeleport globalMode <true|false>` — switch the global mode
- `/playerNoEndPortalTeleport add <player>` — add a player to the list
- `/playerNoEndPortalTeleport remove <player>` — remove a player from the list
- `/playerNoEndPortalTeleport clear` — clear the list
- `/playerNoEndPortalTeleport list` — show the list
- `/playerNoEndPortalTeleport help` — show usage

### Effect

- `globalMode true`: **no** player is teleported by end portals; `globalMode false`: only players on the **list** are not teleported.
- The list and the global mode live in **memory** and are cleared when the server restarts.

## Rocket Shulker refill slot (`/haoRocketShulker`)

Sets **your own** refill slot for the rocket shulker (one setting per player, independent of each other). Used together with the rule `/carpet haoRocketShulker true`.

### Syntax

- `/haoRocketShulker` — show your current setting
- `/haoRocketShulker offhand` — refill into your off hand
- `/haoRocketShulker mainhand <1-9>` — refill into hotbar slot 1–9 of your main hand

### Effect

- Open to everyone (you can only change your own setting), no OP required.
- The master switch is the Carpet rule; the command only chooses where the items land.

## Easy Place Entities (`/easyPlaceEntityCount` `/easyPlaceEntityUi`)

Used together with the rule `/carpet haoProjectionEntity true`.

### Syntax

- `/easyPlaceEntityCount` — show how many entities you place at a time
- `/easyPlaceEntityCount <1-64>` — set how many entities you place at a time
- `/easyPlaceEntityUi` — show/toggle your selector-UI switch
- `/easyPlaceEntityUi <true|false>` — set the UI switch directly

### Effect

- The count lives in **memory** only and resets to the default on restart.
- Open to everyone (you can only change your own setting), no OP required.
