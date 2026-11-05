# Cobblemon Flexible Starters — Installation Guide

Choose a configurable team of starters from Cobblemon's native-style starter screen. The server decides how many starters each new player may pick; the default is **2** and the valid range is **1–6**.

## Compatibility

| Requirement | Tested version |
|---|---|
| Minecraft: Java Edition | 1.21.1 |
| Java | 21 |
| NeoForge | 21.1.265 |
| Cobblemon (NeoForge) | 1.8.4 |
| Cobblemon Flexible Starters | 0.3.0 |

The add-on is required on both the client and server. It is a separate mod; do not modify or bundle Cobblemon.

## Install

1. Ensure the client and dedicated server use matching Minecraft, NeoForge, and Cobblemon versions.
2. Put `cobblemon-flexible-starters-0.3.0.jar` in each side's `mods/` directory.
3. Do not retain older `cobblemon-flexible-starters-*.jar` builds alongside it; only one copy of the mod should be installed.
4. Fully restart the server and clients.

## Configure custom starter categories

On the **server**, edit `config/cobblemon/main.json` and ensure:

```json
"exportStarterConfig": true
```

Then set the desired starter configuration in:

```text
config/cobblemon/starters.json
```

That JSON must contain:

```json
{
  "allowStarterOnJoin": true,
  "promptStarterOnceOnly": true,
  "useConfigStarters": true,
  "starters": []
}
```

Populate `starters` with Cobblemon categories and Pokémon properties as normal. Ball and nature values such as `pokeball=safari_ball nature=modest` are displayed in the picker and granted server-side.

In single-player, the integrated server reads the instance's `config/cobblemon/` directory. In multiplayer, only the dedicated server's configuration is authoritative.

## Set the number of starters

After the server has started once, edit:

```text
world/serverconfig/cobblemon-flexible-starters-server.toml
```

Set:

```toml
starterCount = 2
```

The setting is clamped to 1–6 and synchronized to joining clients. Restart the server after changing it.

## Player flow

1. Open Cobblemon's starter selector as a new player.
2. Browse categories and Pokémon with Cobblemon's normal controls.
3. Press **Choose** to add the current starter to the team tray.
4. Use **Undo** to remove the most recent choice.
5. Press **Confirm** once at least one starter is chosen.

The server rejects malformed, over-limit, replayed, or no-longer-valid selections.

## Acknowledgments

Cobblemon Flexible Starters is independently maintained. [erichexter/dualstarters](https://github.com/erichexter/dualstarters) provided initial inspiration; this repository is not maintained as a fork.
