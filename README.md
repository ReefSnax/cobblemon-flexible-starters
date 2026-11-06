# Cobblemon Flexible Starters

[![License: Sharkwire Community Server License 1.0](https://img.shields.io/badge/license-Sharkwire%20Community%20Server%20License%201.0-6f42c1)](LICENSE)
[![Report an issue](https://img.shields.io/badge/report-an%20issue-d73a49)](https://github.com/ReefSnax/cobblemon-flexible-starters/issues)

A server-authoritative Cobblemon add-on for NeoForge that enhances the native starter picker with configurable multi-starter teams and selection limits.

> **Compatibility:** Minecraft Java 1.21.1 · NeoForge 21.1.x · Cobblemon 1.8.x · Java 21

## Features

- Let new trainers select **1–6** starters before confirming their team.
- Retains Cobblemon's native starter screen, category navigation, model preview, and world view.
- Adds a selected-team tray with **Choose**, **Undo**, and **Confirm** controls.
- Enforces starter counts, duplicate rules, group caps, malformed selections, and replay protection on the server.
- Supports `SPECIES_AND_VARIANT`, `SPECIES`, and `DISABLED` duplicate modes.
- Includes configurable selection groups; the default pseudo-legendary group caps Dratini, Larvitar, Bagon, Beldum, Gible, Deino, Goomy, Jangmo-o, Dreepy, and Frigibax at one selection.
- Shows configured ball and nature details while browsing starters.
- Uses native list states: blue for the current preview, green for selected Pokémon, red for group-limited choices, and gray for eligible choices.

## Install

1. Download `cobblemon-flexible-starters-1.0.0.jar` from [Releases](https://github.com/ReefSnax/cobblemon-flexible-starters/releases).
2. Put the exact same JAR in the `mods/` directory on the dedicated server and every client.
3. Remove older `cobblemon-flexible-starters-*.jar` files so each side has only one copy.
4. Restart the server and clients completely.

The server is authoritative. Clients need the mod for the picker UI, while only the dedicated server's starter configuration and selection rules control what is offered and granted.

For full setup, custom-starter configuration, and selection-rule examples, read the [Installation Guide](INSTALL.md) and the [Wiki](https://github.com/ReefSnax/cobblemon-flexible-starters/wiki).

## Configuration

After the server starts once, configure starter count, duplicate behavior, and active group limits in:

```text
<world>/serverconfig/cobblemon-flexible-starters-server.toml
```

Cobblemon's custom starter categories are configured separately in the server's `config/cobblemon/main.json` and `config/cobblemon/starters.json`. See [INSTALL.md](INSTALL.md) for the required settings.

## Support

- [Report a bug or request a feature](https://github.com/ReefSnax/cobblemon-flexible-starters/issues)
- [Read the license](LICENSE)
- [Browse releases](https://github.com/ReefSnax/cobblemon-flexible-starters/releases)

Please include your Minecraft, NeoForge, Cobblemon, and Cobblemon Flexible Starters versions, plus a relevant `latest.log` excerpt, when reporting a problem.

## License

Cobblemon Flexible Starters is distributed under the [Sharkwire Community Server License 1.0](LICENSE).

You may use unmodified official releases on free-to-access servers, including servers supported through optional donations and cosmetic-only perks. The license also permits unchanged binaries in no-cost qualifying server modpacks with attribution and the license included. It does not permit paid access, gameplay-affecting paid perks, forks, modified releases, or commercial redistribution. The mod is provided **AS IS**, without warranty.
