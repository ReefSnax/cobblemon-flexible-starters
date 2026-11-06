# Installation

1. Download `cobblemon-flexible-starters-1.0.0.jar` from [Releases](https://github.com/ReefSnax/cobblemon-flexible-starters/releases).
2. Put the exact same JAR in the `mods/` folder on the dedicated server and every client.
3. Remove older `cobblemon-flexible-starters-*.jar` copies. Only one copy should exist on each side.
4. Fully restart the server and every client.

## Custom starter categories

On the server, set this in `config/cobblemon/main.json`:

```json
"exportStarterConfig": true
```

Then configure `config/cobblemon/starters.json` with:

```json
{
  "allowStarterOnJoin": true,
  "promptStarterOnceOnly": true,
  "useConfigStarters": true,
  "starters": []
}
```

Populate `starters` with normal Cobblemon categories and Pokémon properties. In multiplayer, the dedicated server's configuration is authoritative.

Next: [[Configuration]].
