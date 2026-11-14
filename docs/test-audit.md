# Test audit

Kept by this repo's owning session (Minecraft: Plugins Lead), per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

## 2026-11-10: the --json console

What's new: every `/mayor` command takes `--json <id>` (Signalbox format 1), and there's `/mayor describe`. Since the first audit, only CI changes had landed, so this covers the console alone.

| Promise | Covered by |
| --- | --- |
| One answer line per request, including unknown subcommands, missing or extra arguments and bad player names | unit `ConsoleTest.everyBadRequestStillGetsExactlyOneAnswer`; live "an unknown subcommand still gets one answer" |
| `describe` lists every command, format 1 | unit `describeListsEveryCommandWithFormatOne`; live |
| `check` gives both players with UUIDs, `reaches` and a stable reason code; someone who can't chat where they are reaches nobody | unit `checkGivesBothPlayersWithUuidsAndAStableReasonCode`, `someoneWhoCantSpeakThereReachesNobodyEvenInACircle`; live |
| While chat is paused, `check` refuses rather than guess | unit `whilePausedCheckRefusesInsteadOfGuessing` |
| An offline player is said plainly | unit `anOfflinePlayerIsSaidPlainly`; live |
| `status` and `reload` report circles, filter, problems and whether a reload applied | unit; live `status` |
| A player without `mayor.admin` gets nothing, `--json` or not | live "a player without mayor.admin gets no answer line" |
| Without `--json`, the commands read as before | the existing live checks (`check explains a shared circle`, the reload checks), unchanged |

Live suite: 67/67 on Paper 26.3 (2 skipped, as before). The test bots' two-letter names aren't valid Minecraft names, so the `--json` checks name them by UUID, which the format allows.

**Skipped:** tab completion for the new single argument. It's convenience; Brigadier draws it.

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
| BlueMapMini shares follow chat | live share section, with a stand-in BlueMapMini firing the share event; BlueMapMini's own guard keeps sharing off with a Mayor older than 0.5.0 |
| Blocked commands in every form, own message | unit `blockedCommandsInEveryForm`; live |
| Filter: whole words, disguises, allow list; Scunthorpe passes | unit `wholeWordsOnly`, `seesThroughDisguises`, `allowListWins`, `phrases`, `defaultList*`; live |
| **Blocked text is never logged or stored** | **new** live "filtered words never reach the server log" (chat and `/msg`) |
| Filter bypass, granted and removed without a relog; others still filtered | unit `filterBypass*`; live |
| `mayor.bypass` and `.filter.bypass` off for everyone, ops included | unit `bypassesAreOffForEveryoneOpsIncluded` |
| Nobody-heard notes: all before any repeat, cooldown | unit `everyNoteBeforeAnyRepeatAndNeverTwiceInARow`, `cooldown`, `emptyIsQuiet`; live |
| Permission changes take effect without a relog | live "Circles granted and removed with LuckPerms, no relog" |
| Broken config at startup pauses chat and messages; console says why | unit `pausedBlocksMessagesButNotOtherCommands`; live |
| **Admins are told about a broken config when they join** | **new** live "an admin joining is told…", and "a player who isn't an admin isn't" |
| A broken reload keeps the working rules | unit `brokenYamlIsAnErrorWithNoSettings`; live |
| `/circles`, names only with permission | live `/circles` section |
| `/mayor check` explains | live |
| 20 players chatting at once stay quick | live "Load" (TPS, 95th percentile under 500 ms) |

**Gaps added this audit:** the two in bold. Both passed on 26.2 and 26.3 (61/61, 2 real-client skips).

**Skipped, with reasons:**
- Sign text and anvil renames: bots can't place and edit signs or use an anvil. They're on the real-client list.
- `ReachService` "safe from any thread": it reads the rules from one snapshot that a reload swaps in whole (a `volatile` field with `final` contents), plus the two players' own permissions and world, the same reads async chat already makes. A dedicated concurrency test would be overkill; the live load test exercises the same path from the async chat thread with 20 bots.
