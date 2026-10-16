// Full-page screenshots of the User Access screens (BRD-11 v1.5): one per screen state of the UX design and one per
// step of the process storyboards. Started by shots_v15.sh, which sets the browser and its flags. Each step names the
// test account that acts, the page and what is done before the capture; the steps run in order on a fresh seed
// database because the storyboards build on one another (joiner, mover, leaver, group-profile change, refused event).
// The SIT/UAT password comes from the environment (SEED_PASSWORD), never from a file.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const BASE = process.env.BASE || 'http://localhost:5173';
const API = process.env.API || 'http://localhost:8080';
const PASSWORD = process.env.SEED_PASSWORD;
const OUT = process.env.OUT_DIR;
const FILTER = new RegExp(process.argv[2] || '.');
const VIEWPORT = { width: 1440, height: 900 };

// ------------------------------------------------------------------ database and API
function sql(query) {
  const out = execFileSync('psql', ['-X', '-A', '-t', '-F', '\t', '-c', query], { encoding: 'utf8' });
  return out.split('\n').filter(Boolean).map((l) => l.split('\t'));
}
const one = (q) => (sql(q)[0] || [])[0];

const tokens = new Map();
async function api(user, method, url, body) {
  if (!tokens.has(user)) {
    const r = await fetch(`${API}/api/v1/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Origin: BASE },
      body: JSON.stringify({ username: user, password: PASSWORD }),
    });
    if (!r.ok) throw new Error(`API sign-in of ${user} failed: ${r.status}`);
    tokens.set(user, (await r.json()).accessToken);
  }
  const headers = { Authorization: `Bearer ${tokens.get(user)}`, Origin: BASE };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const r = await fetch(`${API}/api/v1${url}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await r.text();
  if (!r.ok) throw new Error(`${method} ${url}: ${r.status} ${text.slice(0, 300)}`);
  try { return JSON.parse(text); } catch { return text; }
}

// ------------------------------------------------------------------ browser
let browser;
const pages = new Map();

async function settle(page, ms = 900) {
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(ms);
}

/** A page signed in as a test account (the test-account sign-in of a non-production environment). */
async function pageOf(user) {
  if (pages.has(user)) return pages.get(user);
  const context = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 2 });
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  if (user !== '-' && !user.startsWith('sso-')) {
    await page.goto(`${BASE}/login`);
    await settle(page, 300);
    const test = page.getByRole('button', { name: /test account sign-in/i });
    if (await test.count()) await test.click();
    await page.getByLabel('User ID').fill(user);
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
    await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
    await settle(page);
  }
  pages.set(user, page);
  return page;
}

async function shot(page, file) {
  await page.mouse.move(0, 0);
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(OUT, `${file}.png`), fullPage: true });
}

const go = (p) => async (page) => { await page.goto(BASE + p); await settle(page); };
const click = (name, role = 'button') => async (page) => {
  await page.getByRole(role, { name: typeof name === 'string' ? new RegExp(`^${name}$`, 'i') : name }).first().click();
  await settle(page, 700);
};
const tab = (name) => click(name, 'tab');

// ------------------------------------------------------------------ steps
const STEPS = require('./shots_v15_steps.cjs')({ api, sql, one, settle, go, click, tab, pageOf, BASE, API });

(async () => {
  if (!PASSWORD) throw new Error('SEED_PASSWORD is not set');
  fs.mkdirSync(OUT, { recursive: true });
  const args = (process.env.CHROME_ARGS || '').split(' ').filter(Boolean);
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM, args });
  const failed = [];
  for (const s of STEPS) {
    if (!FILTER.test(s.file)) continue;
    try {
      if (s.setup) await s.setup();
      const page = await pageOf(s.user);
      await page.setViewportSize({ width: 1440, height: s.h || 900 });
      for (const act of [].concat(s.do || [])) await act(page);
      if (!s.noShot) {
        await shot(page, s.file);
        console.log('captured', s.file, page.url().replace(BASE, ''));
      }
      if (s.after) for (const act of [].concat(s.after)) await act(page);
    } catch (e) {
      failed.push(`${s.file}: ${e.message.split('\n')[0]}`);
      console.error('FAILED', s.file, e.message.split('\n')[0]);
    }
  }
  await browser.close();
  if (failed.length) { console.error(failed.join('\n')); process.exit(1); }
})().catch((e) => { console.error(e); process.exit(1); });
