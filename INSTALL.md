# Dual Starters — Install Guide

Pick **several** starter Pokémon at once (1-6, server-configurable, default 2), from a clean
Cobblemon-styled screen.

## What you need

| Thing | Version | Where |
|---|---|---|
| Minecraft: Java Edition | **1.21.1** | the Minecraft launcher |
| Java | **21** (Temurin) | https://adoptium.net/temurin/releases/?version=21 |
| NeoForge | **21.1.264** | https://neoforged.net/ |
| Cobblemon (NeoForge) | **1.8.0** | https://modrinth.com/mod/cobblemon/versions |
| Kotlin for Forge | **5.12.0** | https://modrinth.com/mod/kotlin-for-forge/versions |
| **Dual Starters** | this release | the `.jar` below ⬇ |

A launcher that makes this easy: **Prism Launcher** (https://prismlauncher.org/).

Dual Starters is a **client + server** add-on: every player and the server (dedicated or
single-player's integrated server) need the jar. Cobblemon itself is never modified or bundled —
install it normally alongside this add-on.

## Install

1. Make a **1.21.1 + NeoForge 21.1.264** instance (in Prism: Add Instance → pick 1.21.1 → NeoForge 21.1.264).
2. Open that instance's **mods** folder.
3. Put **all three** jars in it:
   - `Cobblemon-neoforge-1.8.0+1.21.1.jar`
   - `kotlinforforge-5.12.0-all.jar`
   - `dualstarters-0.3.0.jar`  ← from this release
4. Launch the game.
5. For a dedicated server, repeat steps 2-3 in the server's `mods` folder.

## Server config: `starterCount`

How many starters a new player picks is controlled by **one server-side setting**:

- File: `world/serverconfig/dualstarters-server.toml` (created the first time the world/server
  starts; for single-player this is inside the save folder; for a dedicated server it is relative
  to the server's working directory).
- Key: `starterCount` — an integer, **clamped to 1-6**, default **2**.

This value is **server-authoritative**: NeoForge syncs it to every connecting client
automatically, so a player's local client always sees (and can only submit) the server's count —
editing a local config, or using a mismatched client build, cannot grant extra starters. The
server also independently re-validates the submitted pick count before granting anything, so this
holds even against a client that skips the sync entirely.

To change it: stop the server/world, edit `starterCount` in that file, restart.

## How to play

1. Make a **new world** (or join a fresh server).
2. Press **M** to open the starter picker.
3. Use **◀ ▶** next to the Pokémon to browse within a category, and **◀ ▶** next to the category
   name to switch categories.
4. Click a slot to make it the current slot (highlighted), then **I choose you!** to fill it —
   repeated picks auto-advance to the next empty slot. **Remove** clears the current slot.
5. Once every slot is filled, **Confirm your team** — you get all of them!

You only get to pick once per world/server, so choose wisely.

---
Source: https://github.com/erichexter/dualstarters
