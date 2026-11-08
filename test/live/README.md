# Live tests

These run HouseRules on a real Paper server and drive it with protocol bots. The unit tests in `src/test` cover the rules on their own; these cover everything that needs a server: chat, commands, permissions, reloads and load.

`run.sh` builds one throwaway server on 127.0.0.1, runs the whole suite, and deletes the server afterwards, including when a test fails. Only one server runs at a time. The worlds are small, flat and made up, and so are the players and circles.

## What's real and what isn't

- **Real:** Paper, LuckPerms, the plugin jar, Paper's chat and command handling, and private messages through vanilla `/msg`, `/tell` and `/w`.
- **A bot, not the game:** the players are [mineflayer](https://github.com/PrismarineJS/mineflayer) bots, which speak the 1.21.11 protocol and join 26.x through ViaVersion and ViaBackwards. They read chat as plain text, so colours and how the messages look aren't checked.
- **Mocked:** BlueMapHUD's share. A stand-in plugin named BlueMapHUD (`fakehud/`) fires a copy of its `CoordinateShareEvent`. The copy matches the event in BlueMapHUD 0.1.0, compared with `javap`, but the real BlueMapHUD isn't run here. A separate check with the real BlueMapHUD 0.1.0 and HouseRules 0.1.0 jars passed on Paper 26.2 on 2026-11-08 (11 of 11, bots, permissions set without LuckPerms). That test isn't in this repo.
- **Not covered:** signs and anvil renames, which the bot can't do reliably. They go through the same check as books, which is covered. Paper has no `/r` of its own, so a reply that passes is seen as reaching the server, not as delivered.

## Setting up

You need Java 25, Node 22 or newer, and about 500 MB of disk while a server runs.

```bash
./gradlew build
cd test/live && npm ci && cd ../..
test/live/fetch.sh 26.2 26.3
```

`fetch.sh` downloads the latest Paper build for each version from PaperMC, ViaVersion and ViaBackwards from Hangar, and LuckPerms from Modrinth, into `test/live/work/`, which git ignores. Each file is checked against its published checksum, and a mismatch stops it.

## Running

Running a server means accepting the [Minecraft EULA](https://aka.ms/MinecraftEULA), so `run.sh` won't start until you set `HR_ACCEPT_EULA=1`.

```bash
HR_ACCEPT_EULA=1 test/live/run.sh 26.3
```

It takes about 4 minutes. The summary lists failures and skipped checks. The full output is in `test/live/work/server-<version>.log`, and the server console in `server-<version>-console.log`. The game port is 25619; change it with `HR_PORT`.

## What the suite checks

| Part | What it checks |
| --- | --- |
| No config | A fresh install changes nothing: everyone hears everyone. |
| Reach matrix | Eight players in two circles, with no circle, and with bypass, across two world groups (one by suffix, one listed), their Nether and End, and a circle-only world. Public chat from each, and 336 private messages over six commands, match an independent model of the rule. |
| `/circles` | Lists your circles and exactly who can hear you, a count without `houserules.circles.names`, and an explanation in a circle-only world. |
| Commands | Blocked commands in plain, namespaced and mixed-case forms, unlisted ones passing, `/me` off except with bypass. |
| Replies | `/r` with nobody to reply to, and after the other player moves out of reach. |
| Word filter | Chat and private messages with a blocked word aren't delivered; "Scunthorpe has a classic assassin" is. |
| Books | A clean edit is kept; a filtered one, or one in a circle-only world with no circle, is refused with an explanation. |
| Nobody heard you | A note from `quips.yml`, a different one next time, and none when someone heard. |
| Share (mocked) | Recipients filtered exactly as chat, and cancelled for a no-circle sender in a circle-only world. |
| LuckPerms | A circle granted or removed takes effect with no relog. |
| Config | A broken reload keeps the old rules; a broken config at startup pauses chat and private messages until it's fixed and reloaded. |
| Load | 20 more bots, 300 lines in about 15 seconds: all delivered within their circle, TPS, chat latency, no kicks. |

## The test-only plugins

`helper/` makes worlds, moves players, gives items and reports TPS, with one console command, `/hrt`. `fakehud/` is the BlueMapHUD stand-in. Both are built by `./gradlew build` and never shipped. Never install either on a real server.
