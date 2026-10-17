// Full-page screenshots of the Renewal screens (BRD-06 v1.3): one per screen state of the UX design and one per step
// of the process storyboards. Started by shots_v13.sh, which sets the browser and its flags and writes the list of
// screen states (brd06_v13_screens.py --manifest). Storyboards SB-01 and SB-02 run first, then the screens, then
// SB-03 to SB-05 (they change the renewal the screens show). The SIT/UAT password comes from the environment
// (SEED_PASSWORD), never from a file. The steps are those of tools/screenshots/packs/brd06.cjs and
// brd06_walkthrough.cjs.
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
function one(query) {
  const rows = sql(query);
  if (rows.length === 0) throw new Error(`no record for: ${query}`);
  return rows[0][0];
}

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
  const context = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1.5 });
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  await page.goto(`${BASE}/login`);
  await settle(page, 300);
  const test = page.getByRole('button', { name: /test account sign-in/i });
  if (await test.count()) await test.click();
  await page.getByLabel('User ID').fill(user);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
  await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
  await settle(page);
  pages.set(user, page);
  return page;
}

/** Grows the window to the height of the page content (the content area scrolls inside the window), so that the
 * whole page is on one image; the menu is left out of the measure. An open dialog keeps the window. */
async function fitHeight(page) {
  const need = await page.evaluate(() => {
    if (document.querySelector('dialog[open]')) return window.innerHeight;
    let h = document.documentElement.scrollHeight;
    for (const el of document.querySelectorAll('main, main *, body > div, body > div > div')) {
      if (el.closest('nav, aside')) continue;
      const s = getComputedStyle(el);
      if ((s.overflowY === 'auto' || s.overflowY === 'scroll') && el.scrollHeight > el.clientHeight + 4) {
        h = Math.max(h, el.scrollHeight - el.clientHeight + window.innerHeight);
      }
    }
    return h;
  });
  const height = Math.min(Math.max(900, Math.ceil(need)), 2800);
  if (height !== page.viewportSize().height) {
    await page.setViewportSize({ width: 1440, height });
    await page.waitForTimeout(500);
  }
}

async function shot(page, file) {
  await fitHeight(page);
  await page.mouse.move(0, 0);
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(OUT, `${file}.png`), fullPage: true });
}

// ------------------------------------------------------------------ form helpers (as tools/screenshots/capture_pack.cjs)
const escapeRx = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const rx = (t) => (t instanceof RegExp ? t : new RegExp(t, 'i'));

async function pickField(matches, value) {
  await matches.first().waitFor({ state: 'visible', timeout: 10000 });
  const n = await matches.count();
  if (n > 1) {
    const wantBox = typeof value === 'boolean';
    for (let i = 0; i < n; i += 1) {
      const el = matches.nth(i);
      const type = await el.evaluate((e) => (e.getAttribute('type') || e.tagName).toLowerCase());
      if (wantBox === (type === 'checkbox' || type === 'radio') && (await el.isVisible())) return el;
    }
  }
  return matches.first();
}

async function fillField(page, label, value) {
  const field = await pickField(page.getByLabel(rx(`^${escapeRx(label)}\\s*\\*?$`)), value);
  const tag = await field.evaluate((e) => e.tagName.toLowerCase());
  const type = await field.evaluate((e) => (e.getAttribute('type') || '').toLowerCase());
  if (tag === 'select') {
    for (let i = 0; i < 25 && (await field.locator('option').count()) < 2; i += 1) await page.waitForTimeout(200);
    const options = await field.locator('option').allTextContents();
    const hit = options.find((o) => rx(value).test(o)) ?? options.find((o) => o.includes(value));
    if (hit === undefined) throw new Error(`option ${value} not in ${label}: ${options.slice(0, 12).join(', ')}`);
    await field.selectOption({ label: hit });
  } else if ((await field.getAttribute('role')) === 'combobox') {
    await field.click();
    const listId = await field.getAttribute('aria-controls');
    const list = listId ? page.locator(`[id="${listId}"]`) : page.getByRole('listbox').last();
    await list.waitFor({ state: 'visible', timeout: 5000 });
    for (let i = 0; i < 25 && (await list.getByRole('option').count()) === 0; i += 1) await page.waitForTimeout(200);
    const entries = list.getByRole('option');
    const texts = await entries.allTextContents();
    let index = texts.findIndex((o) => rx(value).test(o));
    if (index < 0) index = texts.findIndex((o) => o.includes(String(value)));
    if (index < 0) throw new Error(`entry ${value} not in ${label}: ${texts.slice(0, 12).join(', ')}`);
    await entries.nth(index).click();
  } else if (type === 'checkbox' || type === 'radio') {
    if (value) await field.check(); else await field.uncheck();
  } else {
    await field.fill('');
    await field.pressSequentially(String(value), { delay: 5 });
    await field.press('Tab').catch(() => {});
  }
  await page.waitForTimeout(150);
}

async function runSteps(page, steps, ctx) {
  for (const step of steps) {
    if (typeof step === 'function') await step(page, ctx);
    else await fillField(page, step[0], step[1]);
  }
  await settle(page, 500);
}

async function openTab(page, name) {
  const re = new RegExp(`^${name}`, 'i');
  await page.getByRole('tab', { name: re }).or(page.locator('main').getByRole('button', { name: re })).first().click();
  await settle(page, 600);
}

async function clickButton(page, name) {
  const button = page.getByRole('button', { name: rx(name) }).or(page.getByRole('link', { name: rx(name) })).first();
  if (await button.waitFor({ state: 'visible', timeout: 5000 }).then(() => true).catch(() => false)) {
    await button.click();
    await settle(page, 700);
    return;
  }
  // An action of a row is in the row action menu: the first row whose menu offers it.
  const menus = page.locator('main table tbody tr').getByRole('button', { name: /^Actions for/ });
  for (let i = 0; i < Math.min(await menus.count(), 20); i += 1) {
    await menus.nth(i).click();
    const item = page.getByRole('menuitem', { name: rx(name) }).first();
    if (await item.waitFor({ state: 'visible', timeout: 1500 }).then(() => true).catch(() => false)) {
      await item.click();
      await settle(page, 700);
      return;
    }
    await page.keyboard.press('Escape');
  }
  throw new Error(`no button or row action ${name}`);
}

// ------------------------------------------------------------------ steps
const ctx = { BASE, API, sql, one, api, settle, pageOf, runSteps, fillField, openTab, clickButton, escapeRx, state: {} };
const STEPS = require('./shots_v13_steps.cjs')(ctx);

(async () => {
  if (!PASSWORD) throw new Error('SEED_PASSWORD is not set');
  fs.mkdirSync(OUT, { recursive: true });
  const args = (process.env.CHROME_ARGS || '').split(' ').filter(Boolean);
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM, args });
  const failed = [];
  for (const s of STEPS) {
    if (!FILTER.test(s.file)) continue;
    try {
      const page = await s.run(ctx);
      await page.setViewportSize({ width: 1440, height: s.h || 900 });
      if (s.after) await s.after(page, ctx);
      await shot(page, s.file);
      console.log('captured', s.file, page.url().replace(BASE, ''));
    } catch (e) {
      failed.push(`${s.file}: ${e.message.split('\n')[0]}`);
      console.error('FAILED', s.file, e.message.split('\n')[0]);
    }
  }
  await browser.close();
  if (failed.length) { console.error(failed.join('\n')); process.exit(1); }
})().catch((e) => { console.error(e); process.exit(1); });
