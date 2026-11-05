# Dual Starters — build plan (Wolf, from Finn's PRD)

**What:** A Cobblemon add-on. A new player picks **two** starters on one screen (two slots, confirm step),
duplicates allowed (rolled independently), new-players-only, no config, single-player + multiplayer.
Source PRD: `PRD.md`.

## Tech path (fixed by the PRD — no Bedrock/Java ambiguity)
- Java Edition **1.21.1**, loader **NeoForge** (NeoForge `21.1.x` branch), base mod **Cobblemon 1.8.0**.
- Build on **JDK 21** (present on wolf-prime). NeoForge MDK ships the Gradle wrapper.

## Feasibility — Finn's Open Q1 ANSWERED (2026-10-22)
- Cobblemon **v1.8.0 for NeoForge on MC 1.21.1 exists** (released 2026-09-06; CurseForge + Modrinth). The
  stack Finn specified is real and current.
- Reference implementation proving the approach: **"Custom Starters Menu" (Cobblemon)** — replaces the default
  starter picker and grants via the Cobblemon API. We study its source for the exact hook; we do NOT ship it.

## The one real technical risk
Hooking Cobblemon's starter flow cleanly:
- **Trigger:** PRD wants the SAME keybind as the normal starter menu. So we intercept Cobblemon's
  "open starter screen" path (client keybind -> packet -> GUI) rather than add a new key.
- **"Has chosen a starter?" gate:** Cobblemon tracks starter-chosen in player data (party/starter store). We
  must read that to apply only to players who have NEVER chosen — and NOT re-trigger for existing players.
- **Grant:** on confirm, give both picks as regular starters via the Cobblemon starter-grant API (two
  independent rolls; same-species-twice = two separate Pokémon). The reference mod shows this path.
- Open question for the build: is the starter GUI cleanly overridable via Cobblemon's API/events, or does it
  need a **Mixin** into Cobblemon's client screen? Resolve by reading Cobblemon 1.8.0 source + the reference
  mod. This decides effort (API = small; Mixin = moderate).

## V1 scope (Finn: "that is all")
Two-starter one-screen picker + "Confirm your team" step. Dupes = separate independent Pokémon. New players
only. Always 2, no config. Match the normal Cobblemon starter screen's look. No custom names/colors/sounds.
**Explicitly NOT in V1:** duo battle effects (Finn hasn't designed them — later, needs its own pass).

## Where it gets built + tested (division of labor)
- **Build + server-side test:** wolf-prime (headless). `runServer` + a dedicated server can verify the
  new-player gate and the two-Pokémon grant without a display.
- **Client GUI playtest** (clicking through the 2-slot screen + confirm): needs a real MC client/display —
  route to the **Minecraft agent on Alienware** with a definition-of-done, or Finn/Hex playtests the built jar.
  Do NOT fire-and-forget: hand a spec + DoD and track progress.

## Steps
1. Scaffold NeoForge 1.21.1 MDK; set modid `cobblemon_flexible_starters`, JDK 21 toolchain.
2. Add Cobblemon 1.8.0 (NeoForge) as a `compileOnly`/`modImplementation` dep from Cobblemon's maven;
   confirm it resolves + the dev client launches with Cobblemon loaded.
3. Read Cobblemon 1.8.0 starter source + the reference mod -> decide API-hook vs Mixin.
4. Implement: intercept starter-open -> custom 2-slot screen -> confirm -> grant 2 (independent rolls) ->
   mark starter-chosen; gate on never-chosen; works SP + dedicated server.
5. Build jar; server-test the gate + grant; then client playtest (Alienware/Finn).

## Definition of done (V1)
A fresh player presses the starter keybind, sees a 2-slot picker styled like Cobblemon's, picks two (same
species allowed), hits "Confirm your team", and receives exactly two independent starters. A player who
already has a starter is untouched. Works in single-player and on a dedicated server. Closing without
confirming grants nothing and the keybind reopens it.
