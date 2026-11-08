// Live test kit for HouseRules: server control and protocol bots. See README.md in this folder.
const path = require('path'), fs = require('fs'), cp = require('child_process');
const mineflayer = require('mineflayer');
const ROOT = path.resolve(process.env.HR_LIVE_DIR || path.join(__dirname, 'work'));
const JAVA = process.env.JAVA || (process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : 'java');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// ---- Reporting ----
const results = [], skipped = [];
function check(name, ok, detail = '') {
  results.push({ name, ok: !!ok, detail: String(detail) });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== '' ? '  [' + detail + ']' : ''}`);
  return !!ok;
}
function skip(name, reason) { skipped.push({ name, reason }); console.log(`SKIP  ${name}  [${reason}]`); }
const section = (t) => console.log(`\n=== ${t}`);
function summary() {
  const failed = results.filter((r) => !r.ok);
  console.log(`\nSUMMARY ${results.length - failed.length}/${results.length} passed${skipped.length ? `, ${skipped.length} skipped` : ''}`);
  failed.forEach((f) => console.log(`  FAILED ${f.name} [${f.detail}]`));
  skipped.forEach((f) => console.log(`  SKIPPED ${f.name} [${f.reason}]`));
  return failed.length;
}

// ---- Server ----
class Server {
  constructor(dir, jar) { this.dir = path.join(ROOT, dir); this.jar = path.join(ROOT, jar); this.log = ''; }
  async start(timeoutMs = 240000) {
    this.log = '';
    this.proc = cp.spawn(JAVA, ['-Xmx2G', '-jar', this.jar, 'nogui'], { cwd: this.dir, stdio: ['pipe', 'pipe', 'pipe'] });
    // Colour codes are stripped so tests can match plain text.
    const add = (d) => { this.log += d.toString().replace(/\x1b\[[0-9;]*m/g, ''); };
    this.proc.stdout.on('data', add); this.proc.stderr.on('data', add);
    this.exited = new Promise((r) => this.proc.on('exit', r));
    await this.waitLog(/Done \(|Failed to start/, timeoutMs);
    if (!/Done \(/.test(this.log)) throw new Error('Server failed to start:\n' + this.log.slice(-2000));
    return this;
  }
  cmd(c) { this.proc.stdin.write(c + '\n'); }
  mark() { return this.log.length; }
  since(m) { return this.log.slice(m); }
  async waitLog(re, timeoutMs = 20000, from = 0) {
    const end = Date.now() + timeoutMs;
    while (Date.now() < end) { const m = this.log.slice(from).match(re); if (m) return m; await sleep(100); }
    return null;
  }
  /** Runs a console command and waits for a log line matching re. */
  async run(c, re, timeoutMs = 8000) { const m = this.mark(); this.cmd(c); return re ? this.waitLog(re, timeoutMs, m) : (await sleep(300), null); }
  async stop() {
    if (!this.proc) return;
    // Keep the console output next to the suite log, since the server folder is deleted afterwards.
    fs.appendFileSync(this.dir + '-console.log', this.log);
    this.cmd('stop');
    await Promise.race([this.exited, sleep(60000)]);
    if (this.proc.exitCode === null) { this.proc.kill('SIGKILL'); await this.exited; }
    this.proc = null;
    await sleep(500);
  }
  errors(from = 0) {
    return this.log.slice(from).split('\n').filter((l) => /\bERROR\b|Exception|\bat com\.extantape|WARN\]: \[HouseRules\]/.test(l) && !/Advanced terminal/.test(l));
  }
  path(...p) { return path.join(this.dir, ...p); }
  writeConfig(name, text) { fs.writeFileSync(this.path('plugins/HouseRules', name), text); }
}

// ---- Bot ----
class Bot {
  constructor(name, port) {
    this.name = name; this.chat = []; this.kicked = null; this.ended = null;
    this.bot = mineflayer.createBot({ host: '127.0.0.1', port, username: name, version: '1.21.11', auth: 'offline', physicsEnabled: false, hideErrors: true });
    const c = this.bot._client;
    // Tests move players from the server. Position replies only add noise, and through ViaBackwards they can get
    // the bot kicked, so they're dropped. Teleport confirmations still go out.
    const write = c.write.bind(c);
    c.write = (n, params) => { if (!['position', 'position_look', 'look', 'flying'].includes(n)) write(n, params); };
    this.spawned = new Promise((res, rej) => {
      this.bot.once('spawn', () => { this.bot.physicsEnabled = false; res(); });
      this.bot.once('end', (r) => rej(new Error(`${name} disconnected before spawning: ${r}`)));
    });
    this.bot.on('messagestr', (m) => this.chat.push({ t: Date.now(), m }));
    this.bot.on('kicked', (r) => { this.kicked = JSON.stringify(r); });
    this.bot.on('end', (r) => { this.ended = r; });
    this.bot.on('error', () => {});
  }
  say(text) { this.bot.chat(text); }
  mark() { return this.chat.length; }
  heard(re, from = 0) { return this.chat.slice(from).some((c) => (typeof re === 'string' ? c.m.includes(re) : re.test(c.m))); }
  lines(from = 0) { return this.chat.slice(from).map((c) => c.m); }
  async waitFor(re, timeoutMs = 4000, from = 0) {
    const end = Date.now() + timeoutMs;
    while (Date.now() < end) { if (this.heard(re, from)) return true; await sleep(50); }
    return false;
  }
  quit() { try { this.bot.quit(); } catch { } }
}

module.exports = { ROOT, sleep, check, skip, section, summary, Server, Bot };
