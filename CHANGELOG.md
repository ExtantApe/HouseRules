# Changelog

## 0.5.0

- `/hud share` filtering follows BlueMapMini, the map plugin's new name (D194). HouseRules hooks BlueMapMini's share event and nothing else. The owner asked that no old names be kept (D196), so the hooks for the plugin's earlier names are gone.
- Install it together with BlueMapMini 0.3.0. BlueMapMini keeps sharing off until HouseRules is 0.5.0 or newer, so shares can never skip chat circles.
- Tested: unit tests pass, and the live share check now uses a stand-in named BlueMapMini. The real BlueMapMini 0.3.0 and HouseRules 0.5.0 haven't been run together yet.

## 0.4.0

- `ReachService.sharesCircle(viewerHas, to)`: for a viewer who isn't in game, such as someone on Mapkeeper's web map, only a shared circle counts (owner decision D54, 2026-11-10). World, world group and bypass don't, and with no circles set up nobody is reached.
- Tested: 42 unit tests, 3 of them new, plus Mapkeeper's bot test with this jar on throwaway Paper 26.1.2 (25/25): an away kid sees only their circle, and in game chat's reach applies. Chat itself is unchanged and wasn't re-run live. Not seen in a real game client.

## 0.3.0

- Every `/houserules` command takes `--json <id>` and answers with one line in the shared Signalbox format, for scripts and the Leaf Juice app. `/houserules describe` lists the commands. The command is still hidden from players without `houserules.admin`.
- `/houserules check` takes player names or UUIDs. Selectors such as `@p` aren't accepted any more.
- Tested on throwaway Paper 26.3 with bots: 67/67 live checks, 7 of them new for `--json`, and 8 new unit tests. Not seen in a real game client.

## 0.2.0

- Follows BlueMapHUD's rename to BlueMinimap (2026-11-08): coordinate shares from BlueMinimap 0.2 are filtered like chat. Shares from BlueMapHUD 0.1.x are still filtered for this one release, so the two can be updated in either order. Update both together: BlueMinimap 0.2 turns `/hud share` off when HouseRules is older than 0.2.
- No change to chat, private messages, the word filter or circles.

## 0.1.1

- `houserules.filter.bypass` lets a group skip the word filter in chat, private messages, signs, books and item names. It's off for everyone by default, ops included, and changes take effect without a relog.
- `/houserules check` says when a player skips the word filter.

## 0.1.0

First release.

- Chat circles: chat and private messages reach players in the same world group, or anyone sharing a circle. Circle-only worlds, such as a lobby, count circles only.
- `/msg`, `/tell`, `/w` and replies are checked against the same rules, and replies are checked again when sent.
- Commands that would broadcast past the circles (`/me`, `/teammsg`) are off while circles are in use.
- Blocked commands, each with its own message, covering namespaced and mixed-case forms.
- A word filter for chat, private messages, signs, books and item names, with a starting list in `filter.yml`.
- A light-hearted note when nobody could hear you, from a pool of 75 in `quips.yml`.
- `/circles`, and `/houserules reload`, `status` and `check`.
- `houserules.bypass` for moderators.
- An optional hook for BlueMapHUD's `/hud share`, and a `ReachService` for other plugins.
