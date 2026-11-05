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

The starter count belongs in the world's server config:

```toml
# world/serverconfig/dualstarters-server.toml
starterCount = 2
```

## Development

```bash
cd mod
bash ./gradlew clean build --no-daemon --no-build-cache
```

The built JAR is written to `mod/build/libs/dualstarters-0.3.0.jar`.

## Acknowledgments

This project began with inspiration from [erichexter/dualstarters](https://github.com/erichexter/dualstarters). It is now maintained independently as **Cobblemon Flexible Starters**, with its own native-screen augmentation, server-authoritative selection protocol, configuration flow, and UI implementation.
