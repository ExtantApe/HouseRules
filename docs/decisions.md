# Decisions

Owner decisions for HouseRules, newest first.

| Date | Decision | Why |
| --- | --- | --- |
| 2026-11-08 | `houserules.filter.bypass` lets a group skip the word filter everywhere it applies: chat, private messages, signs, books and item names. Off for everyone by default, ops included. Granted to adult groups only; children stay filtered everywhere. | Adults never share a world with children, so filtering adults protects nobody. Off by default so nobody is unfiltered by accident. |
| 2026-11-07 | Signs, books and item names follow the chat rule: where a player can't chat, they can't write either. A configurable word filter (`filter.yml`) checks chat, private messages, signs, books and item names. Blocked text is never logged or stored. | Covers everything players write, not just chat. Not logging matches the no-spying rule for moderators. |
| 2026-11-07 | `/circles` is on for everyone by default and shows the names of people who can hear you. Both parts are permissions (`houserules.circles`, `houserules.circles.names`), so they can be changed per group. | Different groups of players, such as children and adults, never share a world, so a child only sees other children who are meant to be there. Hidden (vanished) players are never shown. |
| 2026-11-07 | When someone chats and nobody can hear them, they get a funny, never mean, note from a large pool (`quips.yml`), shuffled so repeats are rare, at most once every 3 minutes by default. | Silence is confusing. A light note explains it without nagging. |
| 2026-11-07 | `houserules.bypass` lets a moderator reach, and be reached by, everyone. It shows nobody else's chat and logs nothing. Off for everyone by default, ops included. | Moderation without chat spying. Off by default so nobody can reach children by accident. |
| 2026-11-07 | `/r` and `/reply` check reach again at the moment of the reply. | The other player may have moved to a world out of reach since the conversation started. |
| 2026-11-07 | No `/sort` block in the defaults. | Portmanteau, the planned ChestSort replacement, keeps automatic sorting on by itself, and none of its commands turn it off. |
| 2026-11-07 | `/hud share` from BlueMapHUD follows the same reach as chat, with no narrower rule for kids. | One rule for everything players send each other. |
| 2026-11-10 | A viewer who isn't in game, such as someone on the web map, sees only players who share a HouseRules circle with them (owner, D54). | Chat's world rule needs a world, and an away viewer has none. |
| 2026-11-07 | Every list is in config, including circles, circle-only worlds, world groups, the dimension suffixes, message and reply commands, blocked commands and every message. With no circles, chat is vanilla. | Generic and publishable, with no server's names in the code. |
- **BlueMinimap (2026-11-08, owner rename of BlueMapHUD):** 0.2.0 listens for `com.extantape.blueminimap.api.CoordinateShareEvent` when BlueMinimap is installed, and still for the old `com.extantape.bluemaphud.api` event when only BlueMapHUD 0.1.x is, for one release. Released together with BlueMinimap 0.2.0, which turns sharing off when HouseRules is older than 0.2, so no order of upgrades lets a share skip the circles.

## 2026-11-14

- **Map plugin's new name (owner, D194, D196):** the share hook follows BlueMapMini only. No hooks or stand-ins for its earlier names are kept.
- **Name (owner, D198):** the plugin is renamed Mayor. The owner chose "Mayor (Recommended)". Released as 0.5.0 with no old names kept, installed together with BlueMapMini 0.3.0.
