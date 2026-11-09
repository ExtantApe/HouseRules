# Test audit

Kept by this repo's owning session (Minecraft: Plugins Lead), per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

## 2026-11-09: first full audit (at 0.2.0)

Each promise in the README, and the test that holds it to it. "Live" is `test/live/suite.js` on throwaway Paper 26.2 and 26.3 with bots and LuckPerms; "unit" is `src/test`.

| Promise | Covered by |
| --- | --- |
| No circles: nothing changes | unit `noCirclesMeansVanilla`, `noConfigIsVanillaChat`; live "No circles: vanilla chat", "starts with no config as vanilla chat" |
| Same world group reaches, unless circle-only; shared circle reaches anywhere | unit `sameWorldGroupReaches`, `sharedCircleReachesAcrossWorlds`, `circleOnlyWorld`, `worldGroups`, `reachIsSymmetric`; live reach matrix, every pair |
| No circle in a circle-only world: can't chat, message or write, and is told why | live "N2 (hall, no circle) is told…", book edits, `/circles` explanation; signs need a real client (skipped) |
| Private messages out of reach aren't sent; told who instead; selectors refused | live private-message matrix (every command), "/msg with a selector is refused" |
| `/r` re-checked on reply | unit `replies`; live "Replies" |
| `/me` and `/teammsg` off while circles are in use; bypass keeps them | unit `broadcastCommandsOffWhileCirclesAreOn`; live "Blocked and broadcast commands" |
| BlueMinimap shares follow chat; old BlueMapHUD event for one release | live share section with `HR_SHARE_PLUGIN=new` and `old`; BlueMinimap's `test/live/houserules.js` with the real jars |
| Blocked commands in every form, own message | unit `blockedCommandsInEveryForm`; live |
| Filter: whole words, disguises, allow list; Scunthorpe passes | unit `wholeWordsOnly`, `seesThroughDisguises`, `allowListWins`, `phrases`, `defaultList*`; live |
| **Blocked text is never logged or stored** | **new** live "filtered words never reach the server log" (chat and `/msg`) |
| Filter bypass, granted and removed without a relog; others still filtered | unit `filterBypass*`; live |
| `houserules.bypass` and `.filter.bypass` off for everyone, ops included | unit `bypassesAreOffForEveryoneOpsIncluded` |
| Nobody-heard notes: all before any repeat, cooldown | unit `everyNoteBeforeAnyRepeatAndNeverTwiceInARow`, `cooldown`, `emptyIsQuiet`; live |
| Permission changes take effect without a relog | live "Circles granted and removed with LuckPerms, no relog" |
| Broken config at startup pauses chat and messages; console says why | unit `pausedBlocksMessagesButNotOtherCommands`; live |
| **Admins are told about a broken config when they join** | **new** live "an admin joining is told…", and "a player who isn't an admin isn't" |
| A broken reload keeps the working rules | unit `brokenYamlIsAnErrorWithNoSettings`; live |
| `/circles`, names only with permission | live `/circles` section |
| `/houserules check` explains | live |
| 20 players chatting at once stay quick | live "Load" (TPS, 95th percentile under 500 ms) |

**Gaps added this audit:** the two in bold. Both passed on 26.2 and 26.3 (61/61, 2 real-client skips).

**Skipped, with reasons:**
- Sign text and anvil renames: bots can't place and edit signs or use an anvil. They're on the real-client list.
- `ReachService` "safe from any thread": it reads the rules from one snapshot that a reload swaps in whole (a `volatile` field with `final` contents), plus the two players' own permissions and world, the same reads async chat already makes. A dedicated concurrency test would be overkill; the live load test exercises the same path from the async chat thread with 20 bots.
