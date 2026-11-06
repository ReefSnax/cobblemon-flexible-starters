# Troubleshooting

## The picker allows only one starter

Check `<world>/serverconfig/cobblemon-flexible-starters-server.toml`, set `starterCount` from 1 through 6, then restart the server.

## Custom starters do not appear

Verify both Cobblemon settings on the dedicated server:

```json
"exportStarterConfig": true
```

```json
"useConfigStarters": true
```

The server's `starters.json` controls multiplayer starter choices.

## Client cannot join after an update

Every client and the server need the exact same Cobblemon Flexible Starters version. Remove duplicate or stale JARs from `mods/`, then restart fully.

## Need help or found a bug?

[Open a GitHub issue](https://github.com/ReefSnax/cobblemon-flexible-starters/issues) with your Minecraft, NeoForge, Cobblemon, and mod versions plus a relevant `latest.log` excerpt.
