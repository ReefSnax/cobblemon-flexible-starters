# Cobblemon Flexible Starters

A separate NeoForge add-on for **Cobblemon 1.8.x on Minecraft 1.21.1** that extends Cobblemon's native starter-selection screen with configurable multi-starter teams.

## What it does

- Keeps Cobblemon's native starter screen, category navigation, model preview, descriptions, lighting, and unblurred world view.
- Lets a new player choose **1 through 6** starters before confirming. The default server limit is **2**.
- Provides **Choose**, **Undo**, and **Confirm** controls plus a visible selected-team tray.
- Uses the active server-side Cobblemon starter configuration, including custom categories and Pokémon.
- Displays configured ball and nature values before confirmation, including the ball sprite beside the selected Pokémon's name.
- Validates every submitted selection on the server: maximum count, category/index validity, first-time-player gate, and replay protection.

Cobblemon itself is not modified, patched, bundled, or copied into this project.

## Compatibility

- Minecraft: 1.21.1
- NeoForge: 21.1.x
- Cobblemon: 1.8.x (built and tested against 1.8.4)
- Java: 21

Install this JAR on both clients and the dedicated server. For configuration and deployment details, see [`../INSTALL.md`](../INSTALL.md).

## Configuration

Cobblemon reads `config/cobblemon/starters.json` only when `config/cobblemon/main.json` contains:

```json
"exportStarterConfig": true
```

The supplied `starters.json` must also set `"useConfigStarters": true`. On multiplayer, the dedicated server is authoritative; client-side copies do not change the offered starters.

The starter count and selection rules belong in the world's server config:

```toml
# world/serverconfig/cobblemon-flexible-starters-server.toml
starterCount = 2

# Blocks repeats of the same species + form/aspects/region_bias, while allowing
# intentional regional alternatives such as standard Cyndaquil and
# `Cyndaquil region_bias=hisui`.
duplicateIdentity = "SPECIES_AND_VARIANT"
```

### Selection rules

`duplicateIdentity` accepts exactly three values:

- `SPECIES_AND_VARIANT` (default): blocks the same species, form/aspects, and regional bias from being selected twice. It intentionally ignores ball, nature, level, moves, IVs, and shiny settings, so those cannot bypass the rule.
- `SPECIES`: blocks any repeat of a species, including differently configured forms.
- `DISABLED`: permits repeated selections.

The `[exclusivityGroups]` section contains **three independently editable selection groups**. Each has an `enabled` flag, a `max` cap, and a `species` list. Only enabled groups impose a cap, and a species can belong to more than one active group.

- `pseudoLegendaries` is enabled by default with `max = 1`. Its list contains Dratini, Larvitar, Bagon, Beldum, Gible, Deino, Goomy, Jangmo-o, Dreepy, and Frigibax.
- `fanFavorites` is a disabled example using Pikachu and Eevee.
- `traditionalStarters` is a disabled example using Bulbasaur, Charmander, and Squirtle.

Edit any existing group’s `enabled`, `max`, and `species` values to fit your server. The three supplied groups are deliberately well-commented templates; additional named groups require a mod update, rather than silently accepting a TOML section the current release cannot enforce. Group matching is species-based, so regional variants still consume the same pool cap—for example, regular and Hisuian-flavoured Goomy both count against the pseudo-legendary limit. The server resolves and validates every selection atomically, including Random choices; a random choice is drawn only from entries that remain legal under the configured rules.


## Development

```bash
cd mod
bash ./gradlew clean build --no-daemon --no-build-cache
```

The built JAR is written to `mod/build/libs/cobblemon-flexible-starters-0.3.0.jar`.

## Acknowledgments

This project began with inspiration from [erichexter/dualstarters](https://github.com/erichexter/dualstarters). It is now maintained independently as **Cobblemon Flexible Starters**, with its own native-screen augmentation, server-authoritative selection protocol, configuration flow, and UI implementation.
