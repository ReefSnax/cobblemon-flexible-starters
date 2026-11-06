# Configuration

After the server has started once, edit:

```text
<world>/serverconfig/cobblemon-flexible-starters-server.toml
```

Restart the server after changes. Values synchronize to joining clients.

## Starter count

```toml
starterCount = 2
```

Valid values are clamped from 1 through 6.

## Duplicate behavior

```toml
duplicateIdentity = "SPECIES_AND_VARIANT"
```

- `SPECIES_AND_VARIANT` blocks the same species, aspects/forms, and regional bias.
- `SPECIES` blocks any repeat of a species.
- `DISABLED` permits repeats.

## Selection groups

Each group has `enabled`, `max`, and `species` settings. Only enabled groups impose limits. The default pseudo-legendary group is enabled with `max = 1` and contains Dratini, Larvitar, Bagon, Beldum, Gible, Deino, Goomy, Jangmo-o, Dreepy, and Frigibax.

Random choices remain server-authoritative and cannot bypass duplicate or group limits.
