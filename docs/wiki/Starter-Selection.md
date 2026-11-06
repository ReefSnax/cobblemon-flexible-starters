# Starter Selection

## Player flow

1. Open Cobblemon's normal starter selector as a new player.
2. Browse categories and preview a Pokémon.
3. Press **Choose** to add it to the team tray.
4. Press **Undo** to remove the latest selection.
5. Press **Confirm** to submit the complete team.

The server validates every submitted selection atomically. Invalid categories, excessive selections, duplicates, group-limit violations, replayed requests, and invalid Random outcomes are rejected.

## Native category-list states

1. **Blue** — current preview.
2. **Green** — already in the current tray.
3. **Red** — known Pokémon blocked by an active group limit.
4. **Gray** — eligible and unselected.

Random slots are not colored green or red because their species is not known until the server resolves the choice.
