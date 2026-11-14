# Mayor

Chat rules for family and community Paper servers. Players hear people in the same world, plus anyone who shares a "circle" with them, so groups such as families or age groups keep their own conversations across every world.

**Status:** pre-release. It passes its acceptance tests on throwaway Paper 26.2 and 26.3 servers, using protocol bots with LuckPerms: chat and private messages for every pair of players, replies, blocked commands, the word filter and its bypass, books, `/circles`, reloads, a broken config, and 20 bots chatting at once. BlueMapMini's share was tested with a stand-in plugin here; the real BlueMapMini 0.3.0 and Mayor 0.5.0 haven't been run together yet. Signs and anvil renames weren't tested. It hasn't been seen in a real game client yet, so the wording and colours haven't been checked by a person. See [the live tests](test/live/README.md) and the [test audit](docs/test-audit.md).

- Requires Paper 26.1 or newer and Java 25. Paper 1.21.11 and older aren't supported.
- With no circles set up it changes nothing about who hears whom, so it's safe to install first and configure after.

## What it does

**Circles.** A circle is a permission, `mayor.circle.<name>`, and the circle names are listed in `config.yml`. A player can be in several. Two players can hear each other if:

1. they're in the same world group, unless it's a circle-only world, or
2. they share a circle.

Otherwise they can't. A world group is a world with its Nether and End (`survival`, `survival_nether`, `survival_the_end`), or any set of worlds you list under `world-groups`.

**Circle-only worlds.** In these worlds, such as a lobby where new players wait to be approved, being in the same world doesn't count. A player with no circle can't chat, message or write there, and is told why.

**What follows the rules:**

- Public chat goes only to the players who can hear you.
- Private messages (`/msg`, `/tell`, `/w` and the others in `private-message-commands`) to someone out of reach aren't sent, and you're told who you can talk to instead.
- `/r` and `/reply` are checked again when you reply, because the other player may have moved.
- Commands that would reach the whole server, `/me` and `/teammsg` by default, are off while circles are in use.
- BlueMapMini's `/hud share`, if BlueMapMini is installed, goes only to players who could hear you in chat. BlueMapMini keeps sharing off until Mayor is 0.5.0 or newer.

**Blocked commands.** Any command can be turned off, each with its own message. `sort` also blocks `anyplugin:sort`, in any letter case.

**Word filter.** `filter.yml` lists words and phrases to keep out of chat, private messages, signs, books and item names. It matches whole words, so "class" and "Scunthorpe" are fine, and it sees through `sh1t`, `$` for s and stretched letters. Spaced-out spellings get through. Blocked text is never logged or stored: the player is told kindly and can try again. Players with `mayor.filter.bypass`, such as an adults-only group, aren't filtered anywhere.

**Nobody heard you.** If you chat and nobody online can hear you, you get a light-hearted note, such as "A squid nods politely. It didn't understand a word." There are 75 in `quips.yml`, and you see them all before any comes round again. At most one every 3 minutes.

Every message players read is in `config.yml`, in [MiniMessage](https://docs.advntr.dev/minimessage/format.html). The defaults are short and kind, say what to do instead, and never rely on colour alone.

## Install

1. Put `Mayor-<version>.jar` in `plugins/` and start the server.
2. List your circles in `plugins/Mayor/config.yml`, and any circle-only worlds.
3. Give each player their circle permission with your permission plugin, for example with LuckPerms: `/lp group family permission set mayor.circle.family`.
4. Run `/mayor reload`.

Permission changes take effect straight away, with no relog.

If the config can't be read when the server starts, there are no rules to follow, so chat and private messages are paused, and admins are told when they join. Fix the file and run `/mayor reload`. A reload with mistakes keeps the rules that were working, and says what's wrong.

## Commands

| Command | What it does |
| --- | --- |
| `/circles` | Shows your circles and who online can hear you right now. |
| `/mayor reload` | Reads `config.yml`, `filter.yml` and `quips.yml` again. |
| `/mayor status` | Shows the circles, whether the filter is on, and any config warnings. |
| `/mayor check <player> <player>` | Says whether the first can reach the second, and why. |
| `/mayor describe` | Lists these admin commands. |

Every `/mayor` command also takes `--json <id>` at the end, for scripts and the Leaf Juice app. It then answers with one `ea-result <id> {...}` line in the shared [Signalbox](https://github.com/ExtantApe/Signalbox) format: `status` gives the circles, the filter and any problems; `reload` whether the new rules were applied; `check` both players with their UUIDs, `reaches` and a reason code such as `shared-circle`; `describe` the commands. Players are named or given by UUID; selectors like `@p` aren't accepted.

## Permissions

| Permission | Default | Allows |
| --- | --- | --- |
| `mayor.circle.<name>` | nobody | Being in that circle. |
| `mayor.circles` | everyone | `/circles`. |
| `mayor.circles.names` | everyone | Names in `/circles`. Without it, only a count. |
| `mayor.bypass` | nobody, ops included | Reaching and being reached by everyone, for moderators. It doesn't show anyone else's chat. |
| `mayor.filter.bypass` | nobody, ops included | Skipping the word filter everywhere it applies, for adult groups. It doesn't change who hears whom. |
| `mayor.admin` | ops | `/mayor`, and config warnings on joining. |

## For other plugins

Mayor registers a `ReachService` with Bukkit's services manager:

```java
ReachService rules = Bukkit.getServicesManager().load(ReachService.class);
if (rules == null || rules.mayReach(from, to)) { /* send it */ }
```

`mayReach(from, to)` and `maySpeak(player)` follow exactly the same rules as chat, and are safe from any thread.

`sharesCircle(viewerHas, to)` is for a viewer who isn't in game, such as someone on a web map. It's true only when they share a circle with `to`. World, world group and bypass don't count, and with no circles set up it's always false. The caller passes the viewer's permissions, read while they're offline, and anything it can't check should answer false.

## Limits

- Signs are readable by anyone who walks past, and a book or named item can be carried to another world. Mayor only checks them when they're written.
- Chat plugins that send messages their own way, rather than through Paper's chat event, aren't covered. Add their message commands to `private-message-commands`.
- Only the commands listed in `private-message-commands` and `reply-commands` are checked. `/r` is checked against the last conversation Mayor saw.

## Building

```bash
./gradlew build
```

The jar is `build/libs/Mayor-<version>.jar`. Live tests with bots are in `test/live`.

## License

MIT. See [LICENSE](LICENSE).
