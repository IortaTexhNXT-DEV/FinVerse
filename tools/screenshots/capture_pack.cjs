// Captures the screenshots of a BRD business sign-off pack from the running seed profile: every screen state of the
// pack (list, empty form, filled form, validation error, record per status, approval, generated document), the
// steps of the end-to-end walkthroughs and the upload error-file flow, with numbered callout badges drawn on the
// fields before the capture. The shots come from the pack itself (signoff_pack.py --manifest), so the FRS, the
// sign-off workbook and the images always name the same screens and fields.
//
// Usage (from the project root, seed profile running, see tools/screenshots/README.md):
//   SEED_PASSWORD=... BASE=http://localhost:5173 API=http://localhost:8080 PGDATABASE=brokerverse \
//   node tools/screenshots/capture_pack.cjs brd01 [slug-regex]      (brd01 or BRD-01)
//
// The per-BRD recipe (tools/screenshots/packs/<brd>.cjs) says how to reach each state: which record to open for a
// status, what to type into a form, which rows to select, which files to upload, how to perform the walkthrough
// steps and how to render the generated documents. The images are written as optimised PNG to the pack's
// screenshot folder (meta.screenshot_dir of pack.yaml).
//
// Sharpness: every page is rendered at device scale factor 2 (SCALE), so the PNG holds two image pixels per
// screen pixel and prints sharp. Each shot is cropped to the region that matters (crop rules below), so it can be
// printed large: an open dialog, the content area without the menu, or the element the recipe names
// (recipe.crops[slug]: a Playwright selector, 'main', 'dialog' or 'full' for the whole window when the menu and
// header give the reader the navigation context). The crop kind is written into the PNG ("bibs-crop"), so the
// Word builder can put full-window shots on landscape pages.
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const PLAYWRIGHT = process.env.PLAYWRIGHT_MODULE || 'playwright';
const { chromium } = require(PLAYWRIGHT);

const ROOT = path.resolve(__dirname, '../..');
const BASE = process.env.BASE || 'http://localhost:5173';
const API = process.env.API || 'http://localhost:8080';
const PASSWORD = process.env.SEED_PASSWORD;
const PYTHON = process.env.PYTHON || 'python3';
const WIDTH = Number(process.env.WIDTH || 1440);
const HEIGHT = Number(process.env.HEIGHT || 900);
const SCALE = Number(process.env.SCALE || 2);
const MAX_HEIGHT = Number(process.env.MAX_HEIGHT || 2000);
const MAX_WIDTH = Number(process.env.MAX_WIDTH || 1920);  // a list wider than its card widens the window up to this
const MENU_MAX_HEIGHT = Number(process.env.MENU_MAX_HEIGHT || 12000);  // the full menu of a persona (UX deck)
const MARGIN = 12;  // CSS pixels of page kept around a cropped region
const READY_TIMEOUT_MS = 20 * 60 * 1000;

if (!PASSWORD) {
  console.error('Set SEED_PASSWORD to the password of the SIT/UAT users of the seed profile.');
  process.exit(2);
}
const [brd, only] = process.argv.slice(2);  // brd01 or BRD-01; the recipe is packs/brdNN.cjs
if (!brd) {
  console.error('Usage: node tools/screenshots/capture_pack.cjs <brd folder, e.g. brd01> [slug-regex]');
  process.exit(2);
}

// ------------------------------------------------------------------ manifest

function loadManifest() {
  // The pack of the BRD lives in its source folder: docs/deliverables/src/<BRD-nn_Name>/pack/pack.yaml.
  const code = `BRD-${brd.replace(/\D/g, '').slice(0, 2)}`;
  const src = path.join(ROOT, 'docs/deliverables/src');
  const folder = fs.readdirSync(src).find((d) => d.startsWith(`${code}_`));
  if (!folder) {
    throw new Error(`no source folder for ${code} in ${src}`);
  }
  const pack = path.join(src, folder, 'pack', 'pack.yaml');
  const file = path.join(os.tmpdir(), `capture-pack-${brd}-${process.pid}.json`);
  try {
    execFileSync(PYTHON, [path.join(ROOT, 'docs/deliverables/src/signoff/signoff_pack.py'), pack, '--manifest', file],
      { stdio: ['ignore', 'ignore', 'inherit'] });
  } catch {
    // The manifest is written even when the pack check reports problems; they are printed by the check.
  }
  const manifest = JSON.parse(fs.readFileSync(file, 'utf8'));
  fs.unlinkSync(file);
  return manifest;
}

// ------------------------------------------------------------------ seed database and API

/** Rows of a read-only query on the seed database (psql, PG* environment), as arrays of strings. */
function sql(query) {
  const out = execFileSync('psql', ['-X', '-A', '-t', '-F', '\t', '-c', query], { encoding: 'utf8' });
  return out.split('\n').filter(Boolean).map((l) => l.split('\t'));
}

/** First value of a query, or throws with the query when the seed data has no such record. */
function one(query) {
  const rows = sql(query);
  if (rows.length === 0) {
    throw new Error(`no seed record for: ${query}`);
  }
  return rows[0][0];
}

const tokens = new Map();

/** Calls the backend as a seed user (JSON in, JSON or Buffer out). */
async function api(user, method, url, body) {
  if (!tokens.has(user)) {
    const r = await fetch(`${API}/api/v1/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Origin: BASE },
      body: JSON.stringify({ username: user, password: PASSWORD }),
    });
    if (!r.ok) {
      throw new Error(`API sign-in of ${user} failed: ${r.status}`);
    }
    tokens.set(user, (await r.json()).accessToken);
  }
  const headers = { Authorization: `Bearer ${tokens.get(user)}`, Origin: BASE };
  let payload;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const r = await fetch(`${API}/api/v1${url}`, { method, headers, body: payload });
  const type = r.headers.get('content-type') || '';
  const data = type.includes('json') ? await r.json() : Buffer.from(await r.arrayBuffer());
  if (!r.ok) {
    const e = new Error(`${method} ${url}: ${r.status} ${JSON.stringify(data).slice(0, 300)}`);
    e.status = r.status;
    e.data = data;
    throw e;
  }
  return data;
}

async function waitUntilReady() {
  const deadline = Date.now() + READY_TIMEOUT_MS;
  while (Date.now() < deadline) {
    const status = await fetch(`${API}/actuator/health/readiness`)
      .then((r) => r.json()).then((b) => b.status).catch(() => 'DOWN');
    if (status === 'UP') {
      return;
    }
    await new Promise((r) => setTimeout(r, 5000));
  }
  throw new Error(`backend at ${API} not ready after ${READY_TIMEOUT_MS / 60000} minutes`);
}

// ------------------------------------------------------------------ browser helpers

async function settle(page, ms = 800) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(ms);
}

/** A browser context at the capture size and scale (recipes use it for users that are not seed personas). */
function newContext(browser, options = {}) {
  return browser.newContext({ viewport: { width: WIDTH, height: HEIGHT }, deviceScaleFactor: SCALE, ...options });
}

async function signIn(browser, user) {
  const context = await newContext(browser);
  if (process.env.TRACE_DIR) {
    // A trace per persona for the person running the capture (TRACE_DIR set): kept when a shot fails.
    await context.tracing.start({ screenshots: true, snapshots: true });
  }
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  await page.goto(`${BASE}/login`);
  await settle(page, 300);
  if (user) {
    await page.getByLabel('User ID').fill(user);
    await page.getByLabel('Password').fill(PASSWORD);
    await page.getByRole('button', { name: /^login$/i }).click();
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 }).catch(() => {
      throw new Error(`sign-in of ${user} failed (check BROKERVERSE_ALLOWED_ORIGINS includes ${BASE})`);
    });
    await settle(page, 300);
  }
  return page;
}

/** Opens the first record of the list on screen (a link in the first row, otherwise the row itself). */
async function openFirstRecord(page) {
  await page.locator('table tbody tr').first().waitFor({ timeout: 15000 }).catch(() => {});
  const link = page.locator('table tbody tr a').first();
  if ((await link.count()) > 0) {
    await link.click();
  } else {
    await page.locator('table tbody tr').first().click();
  }
  await settle(page);
}

function rx(text) {
  return text instanceof RegExp ? text : new RegExp(text, 'i');
}

async function clickButton(page, name) {
  const button = page.getByRole('button', { name: rx(name) }).first();
  const shown = await button.waitFor({ state: 'visible', timeout: 5000 }).then(() => true).catch(() => false);
  if (shown) {
    await button.click();
    await settle(page);
    return;
  }
  // An action of a row is in the row action menu: the first row whose menu offers it.
  const menus = page.locator('main table tbody tr').getByRole('button', { name: /^Actions for/ });
  for (let i = 0; i < Math.min(await menus.count(), 20); i += 1) {
    await menus.nth(i).evaluate((el) => el.scrollIntoView({ block: 'center' }));
    await menus.nth(i).click();
    const item = page.getByRole('menuitem', { name: rx(name) }).first();
    if (await item.waitFor({ state: 'visible', timeout: 1500 }).then(() => true).catch(() => false)) {
      await item.click();
      await settle(page);
      return;
    }
    await page.keyboard.press('Escape');
  }
  throw new Error('no button or row action ' + name);
}

/** Opens a tab (or a tab-like filter button) whose name starts with `name` (a regular expression text). */
async function openTab(page, name) {
  const re = new RegExp(`^${name}`, 'i');
  const tab = page.getByRole('tab', { name: re }).or(page.locator('main').getByRole('button', { name: re })).first();
  await tab.waitFor({ state: 'visible', timeout: 15000 });
  await tab.click();
  await settle(page);
}

function escapeRx(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Types a value into the field with this label: a select gets the option whose text matches, a check box is
 * ticked, a date field gets the value typed as shown (dd-MMM-yyyy), every other input gets the text.
 */
async function fillField(page, label, value) {
  const field = await pickField(page.getByLabel(rx(`^${escapeRx(label)}\\s*\\*?$`)), value);
  const tag = await field.evaluate((e) => e.tagName.toLowerCase());
  const type = await field.evaluate((e) => (e.getAttribute('type') || '').toLowerCase());
  if (tag === 'select') {
    const options = await field.locator('option').allTextContents();
    const hit = options.find((o) => rx(value).test(o)) ?? options.find((o) => o.includes(value));
    if (hit === undefined) {
      throw new Error(`option ${value} not in ${label}: ${options.slice(0, 12).join(', ')}`);
    }
    await field.selectOption({ label: hit });
  } else if (type === 'checkbox' || type === 'radio') {
    if (value) {
      await field.check();
    } else {
      await field.uncheck();
    }
  } else {
    await field.fill('');
    await field.pressSequentially(String(value), { delay: 5 });
    await field.press('Tab').catch(() => {});
  }
  await page.waitForTimeout(150);
}

/**
 * The field of a label for a value: when several fields carry the label (a group profile check box named
 * "Approver" next to the Approver drop-down), a text value goes to the drop-down or text field and a true / false
 * value to the check box.
 */
async function pickField(matches, value) {
  await matches.first().waitFor({ state: 'visible', timeout: 10000 });
  const n = await matches.count();
  if (n > 1) {
    const wantBox = typeof value === 'boolean';
    for (let i = 0; i < n; i += 1) {
      const el = matches.nth(i);
      const type = await el.evaluate((e) => (e.getAttribute('type') || e.tagName).toLowerCase());
      if (wantBox === (type === 'checkbox' || type === 'radio') && (await el.isVisible())) {
        return el;
      }
    }
  }
  return matches.first();
}

/** Runs a list of steps: [label, value] fills a field; functions get the page. */
async function runSteps(page, steps, ctx) {
  for (const step of steps) {
    if (typeof step === 'function') {
      await step(page, ctx);
    } else {
      await fillField(page, step[0], step[1]);
    }
  }
  await settle(page, 500);
}

// ------------------------------------------------------------------ callouts

/**
 * Draws a numbered badge next to each field, column, tab or button of the screen whose visible text is the
 * callout label. Badges sit in an overlay layer on top of the page, so the application is not changed.
 * Returns the numbers that could not be placed (fields not visible in this state).
 */
async function drawCallouts(page, callouts) {
  if (!callouts || callouts.length === 0) {
    return [];
  }
  return page.evaluate((items) => {
    const norm = (s) => (s || '').replace(/\s+/g, ' ').replace(/[\s*:]+$/, '').trim().toLowerCase();
    const visible = (el) => {
      const r = el.getBoundingClientRect();
      if (r.width < 2 || r.height < 2) {
        return false;
      }
      const st = getComputedStyle(el);
      return st.visibility !== 'hidden' && st.display !== 'none' && Number(st.opacity) > 0.05;
    };
    const ownText = (el) => norm([...el.childNodes].filter((n) => n.nodeType === 3).map((n) => n.textContent).join(' '));
    // With a dialog open, only the dialog is described.
    const dialogs = [...document.querySelectorAll('dialog[open], [role=dialog]')].filter((d) => d.getBoundingClientRect().width > 0);
    const scope = dialogs.length ? dialogs[dialogs.length - 1] : null;
    // The application menu and top bar are not described; a page or card header inside the content area is.
    // The title bar of a dialog is not a field: a dialog named after its button ("Book Now") keeps its title clear.
    const inChrome = (el) => !!el.closest('nav, aside, [data-callout-layer]') || (!!el.closest('header') && !el.closest('main'))
      || (scope !== null && !scope.contains(el)) || (scope !== null && !!el.closest('dialog > header'));
    const byType = {
      column: ['th', '[role=columnheader]'],
      tab: ['[role=tab]'],
      button: ['button', 'a'],
    };
    const generic = ['label', 'legend', 'th', '[role=columnheader]', '[role=tab]', 'dt', 'h2', 'h3', 'h4', 'button',
      'span', 'div', 'p', 'td', 'a', 'strong', 'dd'];
    const used = new Set();
    const layer = document.createElement('div');
    layer.setAttribute('data-callout-layer', '');
    layer.style.cssText = 'position:absolute;left:0;top:0;width:0;height:0;z-index:2147483647;pointer-events:none;';
    document.body.appendChild(layer);
    const taken = [];
    const missing = [];
    const variants = (label) => {
      const clean = label.replace(/^\[|\]$/g, '');
      const out = [clean];
      clean.split(/\s+\/\s+|,\s+|\s+and\s+/).forEach((p) => out.push(p));
      clean.split(' (')[0] && out.push(clean.split(' (')[0]);
      return [...new Set(out.map(norm).filter((v) => v.length > 1))];
    };
    // A callout the recipe scopes (packs/brdNN.cjs `callouts`) is looked for only inside the visible elements that
    // match `within` (and contain the `title` text); `target` names the element itself (a panel line, a link). When
    // no such element is on the page the field is not in this state and gets no badge.
    const scoped = (scope) => [...document.querySelectorAll(scope.within)]
      .filter((el) => visible(el) && !el.closest('[data-callout-layer]')
        && (!scope.title || (el.innerText || '').includes(scope.title)));
    const findScoped = (label, type, scope) => {
      const roots = scoped(scope);
      if (roots.length === 0) {
        return null;
      }
      if (scope.target) {
        return roots.map((r) => [...r.querySelectorAll(scope.target)].find((el) => visible(el) && !used.has(el)))
          .find(Boolean) ?? null;
      }
      return find(label, type, (el) => roots.some((r) => r.contains(el)));
    };
    const find = (label, type, inside = () => true) => {
      const t = (type || '').toLowerCase();
      const pref = Object.entries(byType).find(([k]) => t.includes(k));
      // A column label goes on the table header; any other field never lands on a column header, so a filter and
      // a column of the same name get their own badges.
      const rest = generic.filter((g) => t.includes('column') || !['th', '[role=columnheader]'].includes(g));
      const selectors = pref ? [...pref[1], ...rest] : rest;
      for (const v of variants(label)) {
        for (const sel of selectors) {
          for (const el of document.querySelectorAll(sel)) {
            if (used.has(el) || inChrome(el) || !visible(el) || !inside(el)) {
              continue;
            }
            const text = ['span', 'div', 'p', 'td', 'a', 'strong', 'dd'].includes(sel) ? ownText(el) : norm(el.innerText);
            if (text === v) {
              return el;
            }
          }
        }
        // A search box or field known by its placeholder.
        for (const el of document.querySelectorAll('input[placeholder], textarea[placeholder]')) {
          if (!used.has(el) && visible(el) && inside(el) && norm(el.getAttribute('placeholder')) === v) {
            return el;
          }
        }
      }
      return null;
    };
    for (const [no, label, type, scope] of items) {
      const el = scope ? findScoped(label, type, scope) : find(label, type);
      if (!el) {
        missing.push(no);
        continue;
      }
      used.add(el);
      const r = el.getBoundingClientRect();
      // Inside the corner of a column header, tab or input; left of a label, link or button, so its text stays whole.
      const inside = el.matches('th, [role=columnheader], [role=tab], input, textarea');
      let x = r.left + window.scrollX + (inside ? 2 : -29);
      let y = r.top + window.scrollY + (inside ? -10 : Math.min(r.height, 26) / 2 - 13);
      // Keep badges of neighbouring fields apart.
      while (taken.some(([a, b]) => Math.abs(a - x) < 28 && Math.abs(b - y) < 28)) {
        x += 30;
      }
      taken.push([x, y]);
      const badge = document.createElement('div');
      badge.textContent = String(no);
      // Large enough to read on a printed page: 26 px, bold 15 px digits, Header Blue with the Yellow ring.
      badge.setAttribute('data-callout-badge', '');
      badge.style.cssText = `position:absolute;left:${Math.max(2, x)}px;top:${Math.max(2, y)}px;min-width:26px;` +
        'height:26px;padding:0 5px;box-sizing:border-box;border-radius:13px;background:#004EA8;color:#fff;' +
        'font:700 15px/22px Arial,sans-serif;text-align:center;border:2px solid #FDB913;' +
        'box-shadow:0 1px 3px rgba(0,0,0,.35);';
      layer.appendChild(badge);
    }
    return missing;
  }, callouts);
}

async function clearCallouts(page) {
  await page.evaluate(() => document.querySelectorAll('[data-callout-layer]').forEach((e) => e.remove())).catch(() => {});
}

// ------------------------------------------------------------------ capture and optimise

/**
 * Grows the viewport so the whole scrolling content area is in the image (up to MAX_HEIGHT). A list that scrolls
 * inside its card (capped to the room left in the window) counts with the rows hidden in its card: in the taller
 * window the list lifts its cap and shows every row, so the image holds the whole card. A walkthrough step (`tall`
 * false) keeps the top of its page and grows only by the hidden rows of such a list.
 */
async function fitViewport(page, tall, width = WIDTH) {
  // A list sets its cap again on a window resize: after a recipe set content aside (the record header above the
  // tabs of a walkthrough step), the list takes the room freed.
  // An open row action menu closes on any scroll, so a shot of the menu leaves the page as it is.
  const menuOpen = (await page.locator('[role=menu]').count()) > 0;
  if (!menuOpen) {
    await page.evaluate(() => window.dispatchEvent(new Event('resize')));
    await page.waitForTimeout(200);
  }
  const extra = await page.evaluate(() => {
    const main = document.querySelector('main.app-main, main');
    const doc = document.documentElement;
    const inner = main ? main.scrollHeight - main.clientHeight : 0;
    const cards = [...document.querySelectorAll('main .table-wrap[data-fit]')]
      .reduce((sum, w) => sum + Math.max(0, w.scrollHeight - w.clientHeight), 0);
    // A little more than the hidden rows (a sideways scroll bar, rounding), so the last row is never cut; the crop
    // trims the window to what is drawn.
    const room = cards > 0 ? cards + 48 : 0;
    // An open dialog taller than the window: the window grows so the whole dialog, title to buttons, is in view.
    const dialogs = [...document.querySelectorAll('dialog[open], [role=dialog]')]
      .filter((d) => d.getBoundingClientRect().width > 0);
    const dialog = dialogs[dialogs.length - 1];
    // (a dialog is at most 90% of the window high and scrolls inside beyond that)
    const tallDialog = dialog && dialog.scrollHeight > dialog.clientHeight + 1
      ? Math.max(0, Math.ceil(dialog.scrollHeight / 0.9) + 16 - window.innerHeight) : 0;
    return { all: Math.max(inner + room, doc.scrollHeight - doc.clientHeight, tallDialog, 0), cards: Math.max(room, tallDialog) };
  });
  // A walkthrough step keeps the top of its page, but a list capped in its card is shown with all its rows.
  const grow = tall === false ? extra.cards : extra.all;
  if (extra.cards > 0 && HEIGHT + grow > MAX_HEIGHT && !menuOpen) {
    // The window cannot grow enough (a long page beside the list): the page scrolls as a whole, as the screen does
    // for a page with several lists, so the list's rows run down the page instead of being held in a short card.
    await page.evaluate(() => {
      const main = document.querySelector('main.app-main, main');
      if (main && !main.querySelector('[data-capture-whole]')) {
        const marker = document.createElement('div');
        marker.setAttribute('data-fit', '');
        marker.setAttribute('data-capture-whole', '');
        marker.hidden = true;
        main.appendChild(marker);
      }
      window.dispatchEvent(new Event('resize'));
    });
    await page.waitForTimeout(300);
  }
  const height = Math.min(MAX_HEIGHT, HEIGHT + grow);
  await page.setViewportSize({ width, height });
  await page.waitForTimeout(grow > 0 ? 600 : 300);
  if (!menuOpen) {
    await page.evaluate(() => document.querySelectorAll('main .table-wrap[data-fit]').forEach((w) => w.scrollTo(0, 0)));
  }
}

/**
 * Stores the PNG compactly without visible loss: a screen of flat colours keeps a 256-colour palette (no dither);
 * a photo-like image (the sign-in background, a rendered page with pictures) stays full colour. The crop kind is
 * written as the PNG text "bibs-crop".
 */
function optimise(file, crop = '') {
  const script = [
    'import sys',
    'from PIL import Image, PngImagePlugin',
    'p, crop = sys.argv[1], sys.argv[2]',
    'im = Image.open(p).convert("RGB")',
    'info = PngImagePlugin.PngInfo()',
    'if crop:',
    '    info.add_text("bibs-crop", crop)',
    'photo = im.getcolors(maxcolors=60000) is None',
    'out = im if photo else im.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)',
    'out.save(p, optimize=True, pnginfo=info)',
  ].join('\n');
  execFileSync(PYTHON, ['-c', script, file, crop]);
}

// ------------------------------------------------------------------ crop

/**
 * The region of the page to keep, in CSS pixels, and its kind. Rules, first match wins:
 * recipe.crops[slug] (or `crop` of the shot in the pack), then an open dialog, then the content area (main).
 * A named region is widened to the callout badges and the last message (toast) that belong to it.
 */
async function cropOf(page, shot, recipe) {
  const rule = (recipe.crops && recipe.crops[shot.slug]) || shot.crop || 'auto';
  if (rule === 'full') {
    return { kind: 'full', clip: null };
  }
  let kind = rule;
  let boxes = [];
  const visibleBoxes = async (locator) => {
    const out = [];
    for (const el of await locator.all()) {
      if (await el.isVisible().catch(() => false)) {
        const b = await el.boundingBox();
        if (b && b.width > 4 && b.height > 4) {
          out.push(b);
        }
      }
    }
    return out;
  };
  if (rule === 'auto' || rule === 'dialog') {
    const dialogs = await visibleBoxes(page.locator('dialog[open], [role=dialog]'));
    if (dialogs.length) {
      kind = 'dialog';
      boxes = [dialogs[dialogs.length - 1]];
    } else if (rule === 'dialog') {
      throw new Error('crop dialog: no dialog open');
    }
  }
  if (boxes.length === 0 && rule === 'auto') {
    // The sign-in pages: the form card, without the picture beside it.
    boxes = await visibleBoxes(page.locator('.login-card'));
    kind = boxes.length ? 'region' : kind;
  }
  if (boxes.length === 0 && rule !== 'auto' && rule !== 'main' && rule !== 'dialog') {
    boxes = await visibleBoxes(page.locator(rule));
    if (boxes.length === 0) {
      throw new Error(`crop ${rule}: no such element on the page`);
    }
    kind = 'region';
  }
  if (boxes.length === 0) {
    // The content area without the menu and the header, trimmed to what is drawn in it.
    kind = 'main';
    const box = await page.evaluate(() => {
      const main = document.querySelector('main.app-main, main');
      if (!main) {
        return null;
      }
      const m = main.getBoundingClientRect();
      let right = m.left + 1;
      let bottom = m.top + 1;
      for (const el of main.querySelectorAll('*')) {
        const r = el.getBoundingClientRect();
        if (r.width < 1 || r.height < 1 || getComputedStyle(el).visibility === 'hidden') {
          continue;
        }
        // Rows scrolled out of a list that scrolls inside its card are not drawn below the card.
        const box = el.parentElement && el.parentElement.closest('.table-wrap');
        const limit = box ? box.getBoundingClientRect().bottom : Infinity;
        right = Math.max(right, Math.min(r.right, m.right));
        bottom = Math.max(bottom, Math.min(r.bottom, limit));
      }
      return { x: m.left, y: m.top, width: right - m.left, height: bottom - m.top };
    });
    boxes = box ? [box] : [];
    if (box) {
      // The message of the step sits at the bottom right of the window; under a short page it is moved up to just
      // below the content, so the image has no empty band between the content and the message.
      await page.evaluate((contentBottom) => {
        const region = document.querySelector('.toast-region');
        const shown = region && [...region.querySelectorAll('.toast')].filter((t) => t.style.display !== 'none');
        if (!shown || shown.length === 0) {
          return;
        }
        const r = region.getBoundingClientRect();
        // Below a short page it moves up; over the last rows of a page it moves down when the window has room.
        const below = r.top > contentBottom + 24;
        const over = r.top < contentBottom && contentBottom + 8 + r.height <= window.innerHeight;
        if (below || over) {
          region.style.top = `${Math.round(contentBottom + 8)}px`;
          region.style.bottom = 'auto';
        }
      }, box.y + box.height);
      await page.waitForTimeout(100);
    }
  }
  if (boxes.length === 0) {
    return { kind: 'full', clip: null };
  }
  let [x0, y0, x1, y1] = [Infinity, Infinity, -Infinity, -Infinity];
  const add = (b) => {
    x0 = Math.min(x0, b.x); y0 = Math.min(y0, b.y);
    x1 = Math.max(x1, b.x + b.width); y1 = Math.max(y1, b.y + b.height);
  };
  boxes.forEach(add);
  // Badges drawn for this region (a badge sits just left of its label) and the message just shown.
  const near = (b) => b.x + b.width >= x0 - 60 && b.x <= x1 + 60 && b.y + b.height >= y0 - 60 && b.y <= y1 + 60;
  for (const b of await visibleBoxes(page.locator('[data-callout-badge]'))) {
    if (kind === 'main' || near(b)) {
      add(b);
    }
  }
  for (const b of await visibleBoxes(page.locator('.toast-region .toast'))) {
    if (kind === 'main') {
      add(b);
    }
  }
  const view = page.viewportSize();
  const x = Math.max(0, Math.floor(x0 - MARGIN));
  const y = Math.max(0, Math.floor(y0 - MARGIN));
  const clip = {
    x, y,
    width: Math.min(view.width, Math.ceil(x1 + MARGIN)) - x,
    height: Math.min(view.height, Math.ceil(y1 + MARGIN)) - y,
  };
  return { kind, clip };
}

// ------------------------------------------------------------------ main

(async () => {
  const manifest = loadManifest();
  const recipe = require(path.join(__dirname, 'packs', `brd${brd.replace(/\D/g, '').slice(0, 2)}.cjs`));
  const OUT = path.resolve(manifest.out);
  // Images are written and old ones removed only inside the checkout that holds this script.
  if (path.relative(ROOT, OUT).startsWith('..') || path.isAbsolute(path.relative(ROOT, OUT))) {
    throw new Error(`screenshot folder ${OUT} is outside ${ROOT}`);
  }
  fs.mkdirSync(OUT, { recursive: true });
  await waitUntilReady();
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const pages = new Map();
  const pageOf = async (user) => {
    const key = user ?? '-';
    if (!pages.has(key) || pages.get(key).isClosed()) {
      pages.set(key, await signIn(browser, user));
    }
    const page = pages.get(key);
    await page.setViewportSize({ width: WIDTH, height: HEIGHT });
    return page;
  };
  const ctx = { BASE, API, ROOT, sql, one, api, settle, openFirstRecord, clickButton, openTab, fillField, runSteps,
    pageOf, browser, OUT, state: {}, WIDTH, HEIGHT, SCALE, newContext: (options) => newContext(browser, options) };
  // Callouts of a screen go on every image of that screen where the field is visible.
  const calloutsOf = {};
  manifest.shots.forEach((s) => {
    if (s.callouts && s.callouts.length) {
      // The recipe may scope a field to the dialog, panel or page where it is (recipe.callouts[screen][no]).
      const scopes = (recipe.callouts && recipe.callouts[s.screen]) || {};
      calloutsOf[s.screen] = s.callouts.map(([no, label, type]) => [no, label, type, scopes[no]]);
    }
  });
  const shots = manifest.shots.filter((s) => !only || new RegExp(only).test(s.slug));
  if (!only) {
    // A full run keeps only the images of the current pack.
    const wanted = new Set(manifest.shots.map((s) => `${s.slug}.png`));
    fs.readdirSync(OUT).filter((f) => f.endsWith('.png') && !wanted.has(f)).forEach((f) => fs.unlinkSync(path.join(OUT, f)));
  }
  if (recipe.prepare) {
    await recipe.prepare(ctx);
  }
  const failed = [];
  const report = [];
  for (const shot of shots) {
    const file = path.join(OUT, `${shot.slug}.png`);
    try {
      let page;
      if (shot.state === 'document') {
        await recipe.documents[shot.slug](ctx, file);
        optimise(file, 'document');
        report.push(`${shot.slug}: document`);
        console.log('captured', shot.slug);
        continue;
      }
      if (recipe.custom && recipe.custom[shot.slug]) {
        // A state reached by several actions (an upload flow); returns the page, or null when it wrote the image.
        page = await recipe.custom[shot.slug](ctx, shot, file);
        if (page === null) {
          optimise(file, 'document');
          report.push(`${shot.slug}: rendered file`);
          console.log('captured', shot.slug);
          continue;
        }
      } else if (shot.state === 'landing' || shot.state === 'menu') {
        // The UX deck: the page a persona lands on after sign-in (whole window), and the persona's full menu.
        page = await navigationState(await pageOf(shot.user), shot);
      } else if (shot.state === 'walkthrough') {
        const step = recipe.walkthrough[shot.slug];
        if (!step) {
          throw new Error('no walkthrough step in the recipe');
        }
        page = await step(ctx, shot);
      } else {
        page = await pageOf(shot.user);
        await reachState(page, shot, recipe, ctx);
      }
      await page.evaluate(() => {
        if (document.activeElement && document.activeElement.blur) {
          document.activeElement.blur();
        }
        document.querySelectorAll('main').forEach((m) => m.scrollTo(0, 0));
        // Only the latest message stays on screen, so earlier confirmations do not hide the content.
        const toasts = [...document.querySelectorAll('.toast-region .toast')];
        toasts.slice(0, -1).forEach((t) => { t.style.display = 'none'; });
        window.scrollTo(0, 0);
      });
      // A walkthrough step shows the top of the page (record header, stepper, the message of the step) at a
      // size that stays readable next to its text; the recipe sets `tall` for a step that needs the whole page.
      // A step cropped to a named region (recipe.crops) gets the whole page, so the region is in view.
      const named = recipe.crops && recipe.crops[shot.slug] && !['main', 'dialog', 'full'].includes(recipe.crops[shot.slug]);
      if (shot.state === 'menu') {
        await fitSidebar(page);
      } else if (shot.state !== 'landing') {
        // A list wider than its card at the standard window (recipe.widths[slug], CSS pixels) is taken in a wider
        // window, so every column is in the image.
        const width = (recipe.widths && recipe.widths[shot.slug]) || WIDTH;
        if (width !== WIDTH) {
          await page.setViewportSize({ width, height: HEIGHT });
          await page.waitForTimeout(500);
        }
        // A list that still scrolls sideways inside its card is taken in a window wide enough for all its columns.
        const over = await page.evaluate(() => Math.max(0, ...[...document.querySelectorAll('main .table-wrap')]
          .filter((w) => w.getBoundingClientRect().width > 0)
          .map((w) => w.scrollWidth - w.clientWidth)));
        const wide = over > 1 ? Math.min(MAX_WIDTH, width + over + 24) : width;
        if (wide !== width) {
          await page.setViewportSize({ width: wide, height: HEIGHT });
          await page.waitForTimeout(500);
        }
        await fitViewport(page, shot.state === 'walkthrough' ? (shot.tall ?? Boolean(named)) : shot.tall, wide);
      }
      if (named) {
        // A message of the step would cover the rows of the region.
        await page.evaluate(() => document.querySelectorAll('.toast-region .toast').forEach((t) => { t.style.display = 'none'; }));
      }
      const bare = shot.state === 'walkthrough' || shot.nocallouts;
      const missing = bare ? [] : await drawCallouts(page, calloutsOf[shot.screen]);
      const { kind, clip } = await cropOf(page, shot, recipe);
      await page.screenshot(clip ? { path: file, clip } : { path: file });
      await clearCallouts(page);
      optimise(file, kind);
      const placed = bare ? 0 : (calloutsOf[shot.screen] || []).length - missing.length;
      const size = clip ? `${Math.round(clip.width)}x${Math.round(clip.height)}` : 'window';
      report.push(`${shot.slug}: ${new URL(page.url()).pathname}, ${kind} ${size}, ${placed} callouts`);
      console.log('captured', shot.slug, new URL(page.url()).pathname, kind, size, `${placed} callouts`);
    } catch (e) {
      // The page as it was when the shot failed, for the person running the capture (not kept in the pack).
      for (const [user, p] of pages.entries()) {
        await p.screenshot({ path: path.join(os.tmpdir(), `capture-pack-failed-${shot.slug}-${user}.png`) })
          .catch(() => {});
        if (process.env.TRACE_DIR) {
          await p.context().tracing.stop({ path: path.join(process.env.TRACE_DIR, `trace-${shot.slug}-${user}.zip`) }).catch(() => {});
          await p.context().tracing.start({ screenshots: true, snapshots: true }).catch(() => {});
        }
      }
      failed.push(`${shot.slug}: ${String(e.message).split('\n')[0]}`);
      console.log('FAILED', shot.slug, String(e.message).split('\n')[0]);
    }
  }
  await browser.close();
  fs.writeFileSync(path.join(os.tmpdir(), `capture-pack-${brd}.log`), [...report, '', ...failed].join('\n'));
  if (failed.length > 0) {
    console.error(`Failed shots:\n${failed.join('\n')}`);
    process.exit(1);
  }
})().catch((e) => {
  console.error(e);
  process.exit(1);
});

/**
 * The UX deck shots of a persona: `landing` is the page after sign-in, as the persona first sees it (the whole
 * window, menu groups as they open); `menu` opens every group of the sidebar so the whole menu is in the image.
 */
async function navigationState(page, shot) {
  await page.goto(`${BASE}/`);
  await settle(page, 1200);
  if (shot.state === 'menu') {
    for (let i = 0; i < 20; i += 1) {
      const closed = page.locator('nav.app-sidebar .nav-group-toggle[aria-expanded="false"]');
      if ((await closed.count()) === 0) {
        break;
      }
      await closed.first().click();
      await page.waitForTimeout(150);
    }
    await settle(page, 400);
  }
  return page;
}

/** Grows the viewport so the whole sidebar is in the image (up to MENU_MAX_HEIGHT). */
async function fitSidebar(page) {
  const height = await page.evaluate(() => {
    const nav = document.querySelector('nav.app-sidebar');
    return nav ? nav.scrollHeight : 0;
  });
  await page.setViewportSize({ width: WIDTH, height: Math.min(MENU_MAX_HEIGHT, Math.max(HEIGHT, height + 8)) });
  await page.waitForTimeout(300);
}

/** Brings the page to the state of a screen shot: open the record, tab, fill, select, click, submit, upload. */
async function reachState(page, shot, recipe, ctx) {
  const openKey = Object.keys(shot).find((k) => k === 'open' || k.startsWith('open_'));
  let target = shot.path || shot.route;
  if (openKey && !(openKey === 'open' && shot[openKey] === 'first')) {
    const resolver = recipe.opens[`${openKey}:${shot[openKey]}`] || recipe.opens[shot[openKey]];
    if (!resolver) {
      throw new Error(`no resolver for ${openKey}: ${shot[openKey]}`);
    }
    target = await resolver(ctx, shot);
  } else if (openKey && target.includes(':')) {
    target = target.split('/:')[0];
  }
  if (target.includes(':')) {
    throw new Error(`route ${target} needs a record`);
  }
  await page.goto(BASE + target);
  await settle(page);
  if (openKey === 'open' && shot[openKey] === 'first') {
    await openFirstRecord(page);
  }
  if (shot.tab) {
    await openTab(page, escapeRx(shot.tab));
  }
  if (shot.step) {
    // Wizard steps ahead of the furthest step reached are not clickable: go there with Next.
    const step = page.getByRole('tab', { name: new RegExp(`^\\d+\\. ${escapeRx(shot.step)}`, 'i') }).first();
    for (let i = 0; i < 8 && (await step.getAttribute('aria-selected')) !== 'true'; i += 1) {
      await page.getByRole('button', { name: /^next$/i }).first().click();
      await settle(page, 500);
    }
  }
  if (shot.fill) {
    const steps = recipe.fills[shot.fill];
    if (!steps) {
      throw new Error(`no fill ${shot.fill} in the recipe`);
    }
    await runSteps(page, typeof steps === 'function' ? await steps(ctx) : steps, ctx);
  }
  if (shot.select) {
    await recipe.selects[shot.select](page, ctx);
    await settle(page, 300);
  }
  if (shot.upload) {
    await recipe.uploads[shot.upload](page, ctx);
    await settle(page);
  }
  if (shot.click) {
    await clickButton(page, shot.click);
  }
  if (shot.submit) {
    await clickButton(page, shot.submit);
    await page.waitForTimeout(800);
  }
  if (recipe.after && recipe.after[shot.slug]) {
    await recipe.after[shot.slug](page, ctx);
  }
}
