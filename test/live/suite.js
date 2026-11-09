// The HouseRules acceptance tests, on one throwaway server. See README.md in this folder.
// Usage: node suite.js <dir> <jar> <port>, relative to the work folder.
const fs = require('fs'), path = require('path');
const { sleep, check, skip, section, summary, Server, Bot } = require('./lib.js');
const [dir, jar, port] = process.argv.slice(2);
const REPO = path.resolve(__dirname, '..', '..');

// Made-up circles and worlds. "farm" is an explicit world group; the rest join by suffix.
const CONFIG = `
circles: [red, blue]
circle-only-worlds: [hall]
world-groups:
  farm: [meadow, orchard]
blocked-commands:
  - commands: [sort, other:only]
    message: "<gray>BLOCKTEST sorting stays on"
nobody-heard:
  enabled: true
  cooldown-seconds: 2
`;
const PEOPLE = {
  R1: { world: 'world', circles: ['red'] },
  R2: { world: 'orchard_nether', circles: ['red'] },
  B1: { world: 'world_nether', circles: ['blue'] },
  B2: { world: 'hall', circles: ['blue'] },
  N1: { world: 'world', circles: [] },
  N2: { world: 'hall', circles: [] },
  N3: { world: 'meadow', circles: [] },
  M1: { world: 'world_the_end', circles: [], bypass: true },
};
const NAMES = Object.keys(PEOPLE);
const PM_COMMANDS = ['msg', 'tell', 'w', 'minecraft:msg', 'minecraft:tell', 'minecraft:w'];

// An independent model of the rule in the brief, to compare against.
const group = (w) => { const base = w.replace(/_(nether|the_end)$/, ''); return ['meadow', 'orchard'].includes(base) ? 'farm' : base; };
const circleOnly = (w) => group(w) === 'hall';
const canSpeak = (a) => PEOPLE[a].bypass || !circleOnly(PEOPLE[a].world) || PEOPLE[a].circles.length > 0;
const shares = (a, b) => PEOPLE[a].circles.some((c) => PEOPLE[b].circles.includes(c));
function reaches(a, b) {
  if (a === b) return true;
  if (PEOPLE[a].bypass || PEOPLE[b].bypass) return true;
  if (group(PEOPLE[a].world) === group(PEOPLE[b].world) && !circleOnly(PEOPLE[a].world)) return true;
  return shares(a, b);
}

const NOTES = fs.readFileSync(path.join(REPO, 'src/main/resources/quips.yml'), 'utf8').split('\n')
  .filter((l) => l.startsWith('  - "')).map((l) => l.slice(5, -1).replace(/^<gray>/, '').replace(/\\"/g, '"'));

let s, bots = {};
const all = () => Object.values(bots);

async function join(names) {
  for (const n of names) { bots[n] = new Bot(n, +port); await bots[n].spawned; await sleep(250); }
}
async function place() {
  for (const [n, p] of Object.entries(PEOPLE)) await s.run(`hrt tp ${n} ${p.world}`, new RegExp(`HRT tp ${n} ${p.world} true`), 10000);
  await sleep(1500);
}
async function worlds() {
  for (const w of ['meadow', 'orchard', 'orchard_nether', 'hall', 'island']) await s.run(`hrt world ${w}`, new RegExp(`HRT world (${w}|FAILED)`), 60000);
  const m = await s.run('hrt worlds', /HRT worlds (.*)/);
  return m ? m[1].trim().split(',') : [];
}
async function reload() { return s.run('houserules reload', /\[HouseRules\] (Reloaded|The config has problems, so nothing changed|The config still has problems)[^\n]*/, 10000); }
async function lp(cmd) { await s.run(`lp ${cmd}`, null); await sleep(400); }

/** Each sender says one line; returns who heard it. */
async function chatRound(senders, tag) {
  const marks = Object.fromEntries(all().map((b) => [b.name, b.mark()]));
  for (const a of senders) { bots[a].say(`${tag}-${a}`); await sleep(200); }
  await sleep(1500);
  const heard = {};
  for (const a of senders) heard[a] = all().filter((b) => b.name !== a && b.heard(`${tag}-${a}`, marks[b.name])).map((b) => b.name).sort();
  return { heard, marks };
}

async function vanilla() {
  section('No circles: vanilla chat');
  const { heard } = await chatRound(['N1', 'N3'], 'v');
  check('N1 (world) is heard by N3 (meadow)', heard.N1.includes('N3'), heard.N1);
  check('N3 is heard by everyone', heard.N3.length === NAMES.length - 1, heard.N3);
  const m = bots.N2.mark(); bots.N2.say('/circles');
  check('/circles says everyone can hear you', await bots.N2.waitFor('Everyone on the server can hear you', 3000, m));
}

async function matrixChat() {
  section('Reach matrix: public chat, every pair');
  const { heard, marks } = await chatRound(NAMES, 'c');
  let bad = [];
  for (const a of NAMES) {
    const want = NAMES.filter((b) => b !== a && canSpeak(a) && reaches(a, b)).sort();
    if (JSON.stringify(want) !== JSON.stringify(heard[a])) bad.push(`${a}: want ${want} got ${heard[a]}`);
  }
  check(`chat from each of ${NAMES.length} players reaches exactly the expected players`, bad.length === 0, bad.join('; '));
  check('N2 (hall, no circle) is told chat opens once they join a circle', bots.N2.heard("Chat opens here once you've joined a circle", marks.N2));
}

async function matrixMessages() {
  section('Reach matrix: private messages, every pair, every command');
  let bad = [], count = 0;
  for (const cmd of PM_COMMANDS) {
    const marks = Object.fromEntries(all().map((b) => [b.name, b.mark()]));
    // Round robin, so no sender trips the vanilla spam limit.
    for (const b of NAMES) for (const a of NAMES) {
      if (a === b) continue;
      bots[a].say(`/${cmd} ${b} p-${cmd}-${a}-${b}`); count++;
      await sleep(160);
    }
    await sleep(1500);
    for (const a of NAMES) for (const b of NAMES) {
      if (a === b) continue;
      const want = canSpeak(a) && reaches(a, b);
      const got = bots[b].heard(`p-${cmd}-${a}-${b}`, marks[b]);
      if (want !== got) bad.push(`${cmd} ${a}->${b} want ${want}`);
    }
    // The blocked sender is told who they couldn't reach, and pointed to /circles.
    const blockedPair = NAMES.flatMap((a) => NAMES.map((b) => [a, b])).find(([a, b]) => a !== b && canSpeak(a) && !reaches(a, b));
    if (blockedPair && !bots[blockedPair[0]].heard(`${blockedPair[1]} can't hear you from where you are`, marks[blockedPair[0]])) bad.push(`${cmd}: no explanation to ${blockedPair[0]}`);
  }
  check(`${count} private messages delivered or blocked exactly as expected`, bad.length === 0, bad.slice(0, 12).join('; '));
  const m = bots.N1.mark(); bots.N1.say('/msg @a hello');
  check('/msg with a selector is refused with an example', await bots.N1.waitFor('Private messages go to one person by name', 3000, m));
}

async function circlesCommand() {
  section('/circles');
  const m = bots.R1.mark(); bots.R1.say('/circles');
  await sleep(1200);
  const lines = bots.R1.lines(m).join(' | ');
  const want = NAMES.filter((b) => b !== 'R1' && reaches('R1', b)).sort((x, y) => x.localeCompare(y));
  check('/circles lists your circles', /Your circles: red/.test(lines), lines);
  check('/circles names exactly who can hear you', lines.includes(`(${want.length}): ${want.join(', ')}`), `${want} in ${lines}`);
  const n = bots.N2.mark(); bots.N2.say('/circles');
  await sleep(1000);
  check('/circles in a circle-only world with no circle explains why', bots.N2.heard("You're not in any circles", n) && bots.N2.heard('Chat opens here', n), bots.N2.lines(n));
  await lp('user R1 permission set houserules.circles.names false');
  const k = bots.R1.mark(); bots.R1.say('/circles');
  check('without houserules.circles.names, only a count', await bots.R1.waitFor(/People online who can hear you: \d+$/, 3000, k), bots.R1.lines(k));
  await lp('user R1 permission unset houserules.circles.names');
}

async function blockedCommands() {
  section('Blocked and broadcast commands');
  const forms = ['/sort', '/SORT', '/SoRt chest', '/chestsort:sort', '/Any:Sort', '/other:only'];
  let bad = [];
  for (const f of forms) { const m = bots.N1.mark(); bots.N1.say(f); if (!(await bots.N1.waitFor('BLOCKTEST sorting stays on', 3000, m))) bad.push(f); await sleep(300); }
  check('plain, namespaced and mixed-case forms are blocked with their own message', bad.length === 0, bad);
  bad = [];
  for (const f of ['/only', '/isort', '/help']) { const m = bots.N1.mark(); bots.N1.say(f); await sleep(1200); if (bots.N1.heard('BLOCKTEST', m)) bad.push(f); }
  check('unlisted commands pass', bad.length === 0, bad);
  let m = bots.N1.mark(); bots.N1.say('/me waves');
  check('/me is off while circles are on, with a reason', await bots.N1.waitFor('would reach everyone on the server', 3000, m));
  m = bots.N3.mark(); bots.N1.say('/minecraft:me waves again'); await sleep(1200);
  check('nobody out of reach sees /minecraft:me', !bots.N3.heard('waves again', m));
  m = bots.M1.mark(); const n = bots.N3.mark(); bots.M1.say('/me moderator wave');
  await sleep(1200);
  check('a moderator with bypass can still use /me', bots.N3.heard('moderator wave', n) && !bots.M1.heard('would reach everyone', m));
}

async function replies() {
  section('Replies');
  // Paper has no /r of its own, so "passes" means HouseRules let it through to the server.
  bots.R1.say('/msg N1 start'); await sleep(800);
  let m = bots.N1.mark(); bots.N1.say('/r still here');
  await sleep(1200);
  check('/r to someone still in reach passes', !bots.N1.heard(/reply wasn't sent/, m), bots.N1.lines(m));
  await s.run('hrt tp R1 meadow', /HRT tp R1 meadow true/); await sleep(800);
  m = bots.N1.mark(); bots.N1.say('/reply are you there');
  check('/reply after the other player moves out of reach is blocked and explained', await bots.N1.waitFor("R1 has moved somewhere they can't hear you", 3000, m), bots.N1.lines(m));
  await s.run(`hrt tp R1 ${PEOPLE.R1.world}`, /HRT tp R1 world true/); await sleep(800);
}

async function filter() {
  section('Word filter');
  const marks = Object.fromEntries(all().map((b) => [b.name, b.mark()]));
  bots.R1.say('this is sh1t'); await sleep(1200);
  check('filtered chat is not delivered', !bots.R2.heard('sh1t', marks.R2) && !bots.N1.heard('sh1t', marks.N1));
  check('the speaker is told kindly', bots.R1.heard("a word we don't use here", marks.R1));
  bots.R1.say('Scunthorpe has a classic assassin'); await sleep(1200);
  check('ordinary words that contain a blocked one pass (Scunthorpe)', bots.R2.heard('Scunthorpe has a classic assassin', marks.R2));
  const m = bots.R1.mark(); bots.R1.say('/msg R2 you are a b1tch');
  await sleep(1200);
  check('filtered private message is not delivered', !bots.R2.heard('b1tch', marks.R2) && bots.R1.heard("a word we don't use here", m));

  section('Filter bypass for adult groups, granted and removed with LuckPerms, no relog');
  await lp('user R1 permission set houserules.filter.bypass true');
  let k = bots.R2.mark(); bots.R1.say('this is sh1t'); await sleep(1200);
  check('with houserules.filter.bypass, chat is not filtered', bots.R2.heard('this is sh1t', k), bots.R2.lines(k));
  k = bots.R2.mark(); bots.R1.say('/msg R2 that was sh1t'); await sleep(1200);
  check('with houserules.filter.bypass, private messages are not filtered', bots.R2.heard('that was sh1t', k), bots.R2.lines(k));
  k = bots.R1.mark(); const own = bots.R2.mark(); bots.R2.say('this is sh1t too'); await sleep(1200);
  check('players without it are still filtered', !bots.R1.heard('sh1t too', k) && bots.R2.heard("a word we don't use here", own), bots.R2.lines(own));
  await lp('user R1 permission unset houserules.filter.bypass');
  k = bots.R2.mark(); const told = bots.R1.mark(); bots.R1.say('this is sh1t again'); await sleep(1200);
  check('removing it filters again straight away', !bots.R2.heard('sh1t again', k) && bots.R1.heard("a word we don't use here", told));
}

async function writing() {
  section('Signs, books and item names');
  // Book: give a writable book and edit it, as the client does.
  for (const [who, text, wantBlocked, why, bypass] of [['N1', 'a lovely story', false, 'clean text'], ['R2', 'what the fuck', true, 'filtered word'],
    ['R1', 'what the fuck', false, 'filtered word with houserules.filter.bypass', true], ['N2', 'hello from the hall', true, 'circle-only world, no circle']]) {
    if (bypass) await lp(`user ${who} permission set houserules.filter.bypass true`);
    await s.run(`hrt give ${who} writable_book`, /HRT give/);
    for (let i = 0; i < 40 && !bots[who].bot.inventory.slots[36]; i++) await sleep(100);
    const m = bots[who].mark();
    try {
      // A refused edit sends no slot update, so don't wait for one.
      await Promise.race([bots[who].bot.writeBook(36, [text]).catch(() => {}), sleep(2500)]);
      // Paper kicks a player who edits books too quickly, so leave a gap.
      await sleep(3000);
      const told = bots[who].heard(/book has a word|Writing on signs, books and item names opens/, m);
      check(`book edit, ${why}: ${wantBlocked ? 'refused and explained' : 'allowed'}`, told === wantBlocked, bots[who].lines(m));
    } catch (e) { skip(`book edit, ${why}`, 'the bot could not edit a book: ' + e.message); }
    if (bypass) await lp(`user ${who} permission unset houserules.filter.bypass`);
  }
  // Signs and anvils need the client's own block placing and window handling, which the bot can't do reliably
  // through ViaBackwards. The rules for them are the same code path as books.
  skip('sign text', 'needs a real client to place and edit a sign');
  skip('anvil rename', 'needs a real client to use an anvil');
}

async function nobodyHeard() {
  section('Nobody heard you');
  // The moderator's bypass would let them hear N3, which is right, so it's off for this part.
  await lp('user M1 permission unset houserules.bypass');
  await s.run('hrt tp N3 island', /HRT tp N3 island true/, 10000); await sleep(800);
  const m = bots.N3.mark(); bots.N3.say('anyone out there');
  await sleep(1500);
  const note = bots.N3.lines(m).find((l) => NOTES.includes(l));
  check('a player nobody can hear gets one of the notes', !!note, bots.N3.lines(m));
  await sleep(2200);
  const k = bots.N3.mark(); bots.N3.say('anyone out there now');
  await sleep(1500);
  const second = bots.N3.lines(k).find((l) => NOTES.includes(l));
  check('the next note is a different one', !!second && second !== note, `${note} / ${second}`);
  const r = bots.R1.mark(); bots.R1.say('someone hears this'); await sleep(1200);
  check('no note when someone heard you', !bots.R1.lines(r).some((l) => NOTES.includes(l)));
  await s.run(`hrt tp N3 ${PEOPLE.N3.world}`, /HRT tp N3 meadow true/, 10000); await sleep(800);
  await lp('user M1 permission set houserules.bypass true');
}

async function share() {
  const old = process.env.HR_SHARE_PLUGIN === 'old';
  section(`${old ? 'BlueMapHUD (old name)' : 'BlueMinimap'} share (stand-in plugin, mocked)`);
  let bad = [];
  for (const a of NAMES) {
    const m = await s.run(`fakeshare ${a}`, new RegExp(`FAKESHARE ${a} cancelled=(\\w+) listeners=(\\w+) to=(.*)`));
    if (!m) { bad.push(`${a}: no reply`); continue; }
    const cancelled = m[1] === 'true', to = m[3].trim() ? m[3].trim().split(',').sort() : [];
    const want = canSpeak(a) ? NAMES.filter((b) => b !== a && reaches(a, b)).sort() : null;
    if (want === null ? !cancelled : cancelled || JSON.stringify(to) !== JSON.stringify(want)) bad.push(`${a}: cancelled=${cancelled} to=${to} want ${want ?? 'cancelled'}`);
  }
  check('share recipients are filtered exactly as chat, and a no-circle sender in a circle-only world is cancelled', bad.length === 0, bad.join('; '));
  check(`HouseRules registered for ${old ? 'the old' : 'the new'} share event`, (old ? /BlueMapHUD found/ : /BlueMinimap found/).test(s.log));
}

async function liveCircles() {
  section('Circles granted and removed with LuckPerms, no relog');
  await lp('user N3 permission set houserules.circle.red true');
  let { heard } = await chatRound(['N3'], 'lp1');
  check('N3 reaches the red circle as soon as it is granted', heard.N3.includes('R1') && heard.N3.includes('R2'), heard.N3);
  await lp('user N3 permission unset houserules.circle.red');
  ({ heard } = await chatRound(['N3'], 'lp2'));
  check('and stops as soon as it is removed', !heard.N3.includes('R1'), heard.N3);
}

async function adminCheck() {
  section('/houserules check');
  let m = await s.run('houserules check R1 R2', /\w+ can(?:'t)? reach R2: [^\n]*/);
  check('check explains a shared circle', m && /R1 can reach R2: they share the circle red/.test(m[0]), m && m[0]);
  m = await s.run('houserules check N1 N3', /\w+ can(?:'t)? reach N3: [^\n]*/);
  check('check explains different worlds', m && /can't reach N3: they're in different world groups/.test(m[0]), m && m[0]);
}

async function brokenReload() {
  section('Config: a broken reload keeps the old rules');
  const good = fs.readFileSync(s.path('plugins/HouseRules/config.yml'), 'utf8');
  s.writeConfig('config.yml', 'circles: [red\nnope: : :');
  const m = await reload();
  check('a broken reload is reported and nothing changes', m && /The config has problems, so nothing changed/.test(m[0]), m && m[0]);
  const { heard } = await chatRound(['R1', 'N1'], 'br');
  check('old rules still apply after the broken reload', heard.R1.includes('R2') && !heard.N1.includes('R2'), JSON.stringify(heard));
  s.writeConfig('config.yml', good);
  check('a fixed reload applies', /Reloaded/.test((await reload() || [''])[0]));
}

async function load() {
  section('Load: twenty bots chatting at once');
  const extra = [];
  for (let i = 0; i < 20; i++) { const b = new Bot(`L${String(i).padStart(2, '0')}`, +port); extra.push(b); }
  await Promise.all(extra.map((b) => b.spawned));
  for (let i = 0; i < extra.length; i++) await lp(`user ${extra[i].name} permission set houserules.circle.${i % 2 ? 'red' : 'blue'} true`);
  await sleep(1000);
  const start = Date.now(), sent = [];
  for (let round = 0; round < 15; round++) {
    for (const b of extra) { const t = `ld-${b.name}-${round}`; b.say(t); sent.push({ t, at: Date.now(), from: b.name }); }
    await sleep(1000);
  }
  await sleep(2000);
  const tps = await s.run('hrt tps', /HRT tps ([\d.]+) mspt ([\d.]+)/);
  const latencies = [];
  for (const m of sent) {
    const i = +m.from.slice(1), mate = extra.find((b) => b.name !== m.from && (+b.name.slice(1)) % 2 === i % 2);
    const got = mate.chat.find((c) => c.m.includes(m.t));
    if (got) latencies.push(got.t - m.at);
  }
  latencies.sort((a, b) => a - b);
  const p95 = latencies[Math.floor(latencies.length * 0.95)];
  check(`${sent.length} lines in ${Math.round((Date.now() - start) / 1000)}s all delivered within their circle`, latencies.length === sent.length, `${latencies.length}/${sent.length}`);
  check('TPS stays steady', tps && +tps[1] >= 19.5, tps && `tps ${tps[1]} mspt ${tps[2]}`);
  check('chat stays quick (95th percentile under 500 ms)', p95 < 500, `p95 ${p95} ms, median ${latencies[latencies.length >> 1]} ms`);
  check('no bot was kicked', extra.every((b) => !b.kicked), extra.filter((b) => b.kicked).map((b) => b.name + ' ' + b.kicked));
  check('no blocked-thread warnings', !/Thread .* blocked|Can't keep up/.test(s.log), (s.log.match(/.*(blocked|Can't keep up).*/) || [''])[0]);
  const r = extra[0].mark(); extra[0].say('/r hello');
  check('/r with nobody to reply to says so', await extra[0].waitFor("There's nobody to reply to yet", 3000, r));
  extra.forEach((b) => b.quit());
  await sleep(1500);
}

async function pausedOnBrokenStart() {
  section('Config: broken at startup pauses chat');
  all().forEach((b) => b.quit()); bots = {};
  await s.stop();
  const good = fs.readFileSync(s.path('plugins/HouseRules/config.yml'), 'utf8');
  s.writeConfig('config.yml', 'circles: [red\nnope: : :');
  await s.start();
  check('the console explains the pause', /chat and private messages are paused until it's fixed/.test(s.log));
  s.cmd('op R2'); await sleep(300);
  await join(['R1', 'R2']);
  await sleep(800);
  check('an admin joining is told chat is paused, and how to fix it', bots.R2.lines(0).some((l) => /paused because the config couldn't be read/.test(l)), bots.R2.lines(0).slice(-3).join(' | '));
  check('a player who isn\'t an admin isn\'t', !bots.R1.lines(0).some((l) => /paused because the config couldn't be read/.test(l)));
  const m1 = bots.R1.mark(), m2 = bots.R2.mark();
  bots.R1.say('paused hello'); await sleep(1200);
  check('chat is paused, and the player is told', bots.R1.heard('Chat is paused for a moment', m1) && !bots.R2.heard('paused hello', m2));
  bots.R1.say('/msg R2 paused pm'); await sleep(1200);
  check('private messages are paused too', !bots.R2.heard('paused pm', m2));
  s.writeConfig('config.yml', good);
  await reload();
  const k = bots.R2.mark(); bots.R1.say('back again'); await sleep(1200);
  check('fixing the config and reloading brings chat back', bots.R2.heard('back again', k));
  s.cmd('deop R2');
}

(async () => {
  s = new Server(dir, jar);
  await s.start();
  const have = await worlds();
  check('test worlds exist', ['world', 'world_nether', 'world_the_end', 'meadow', 'orchard_nether', 'hall'].every((w) => have.includes(w)), have);
  check('HouseRules starts with no config as vanilla chat', /No circles are set up, so chat reaches everyone/.test(s.log));
  await join(NAMES);
  await place();
  await vanilla();

  s.writeConfig('config.yml', CONFIG);
  check('reload applies the new config', /Reloaded/.test((await reload() || [''])[0]));
  for (const [n, p] of Object.entries(PEOPLE)) {
    for (const c of p.circles) await lp(`user ${n} permission set houserules.circle.${c} true`);
    if (p.bypass) await lp(`user ${n} permission set houserules.bypass true`);
  }
  await sleep(1000);

  await matrixChat();
  await matrixMessages();
  await circlesCommand();
  await blockedCommands();
  await replies();
  await filter();
  await writing();
  await nobodyHeard();
  await share();
  await liveCircles();
  await adminCheck();
  await brokenReload();
  await load();
  await pausedOnBrokenStart();

  section('Server log');
  // README: "Blocked text is never logged or stored." The words the filter test used must not be in the server log.
  const leaked = ['sh1t', 'b1tch'].filter((w) => s.log.toLowerCase().includes(w));
  check('filtered words never reach the server log', leaked.length === 0, leaked.join(', '));
  const errors = s.errors().filter((l) => !/config.yml: isn't valid YAML|couldn't read its config|The config has problems|SEVERE.*HouseRules.*(YAML|paused|kept)/.test(l));
  check('no errors or exceptions in the log', errors.length === 0, errors.slice(0, 8).join(' || '));
  check('no test player died and respawned elsewhere', !/HRT death /.test(s.log), (s.log.match(/.*HRT death .*/) || [''])[0]);
  const kicked = all().filter((b) => b.kicked);
  check('no test bot was kicked', kicked.length === 0, kicked.map((b) => b.name + ' ' + b.kicked));
  all().forEach((b) => b.quit());
  await s.stop();
  process.exit(summary() ? 1 : 0);
})().catch(async (e) => {
  console.error('CRASHED', e);
  try { all().forEach((b) => b.quit()); await s.stop(); } catch { }
  summary();
  process.exit(1);
});
