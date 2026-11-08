# Changelog

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
