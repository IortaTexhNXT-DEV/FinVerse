// Captures the screenshots of a BRD business sign-off pack from the running seed profile: every screen state of the
// pack (list, empty form, filled form, validation error, record per status, approval, generated document), the
// steps of the end-to-end walkthroughs and the upload error-file flow, with numbered callout badges drawn on the
// fields before the capture. The shots come from the pack itself (signoff_pack.py --manifest), so the FRS, the
// sign-off workbook and the images always name the same screens and fields.
//
// Usage (from the project root, seed profile running, see tools/screenshots/README.md):
//   SEED_PASSWORD=... BASE=http://localhost:5173 API=http://localhost:8080 PGDATABASE=brokerverse \
//   node tools/screenshots/capture_pack.cjs brd01 [slug-regex]
//
// The per-BRD recipe (tools/screenshots/packs/<brd>.cjs) says how to reach each state: which record to open for a
// status, what to type into a form, which rows to select, which files to upload, how to perform the walkthrough
// steps and how to render the generated documents. The images are written as optimised PNG to the pack's
// screenshot folder (meta.screenshot_dir of pack.yaml).
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
const WIDTH = 1600;
const HEIGHT = 1000;
const MAX_HEIGHT = Number(process.env.MAX_HEIGHT || 2000);
const READY_TIMEOUT_MS = 20 * 60 * 1000;

if (!PASSWORD) {
  console.error('Set SEED_PASSWORD to the password of the SIT/UAT users of the seed profile.');
  process.exit(2);
}
const [brd, only] = process.argv.slice(2);
if (!brd) {
  console.error('Usage: node tools/screenshots/capture_pack.cjs <brd folder, e.g. brd01> [slug-regex]');
  process.exit(2);
}

// ------------------------------------------------------------------ manifest

function loadManifest() {
  const pack = path.join(ROOT, 'docs/deliverables/src/signoff', brd, 'pack.yaml');
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
    tokens.set(user, (await r.json()).token);
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

async function signIn(browser, user) {
  const context = await browser.newContext({ viewport: { width: WIDTH, height: HEIGHT } });
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
  await button.waitFor({ state: 'visible', timeout: 15000 });
  await button.click();
  await settle(page);
}

async function openTab(page, name) {
  const tab = page.getByRole('tab', { name: rx(`^${escapeRx(name)}`) }).first();
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
  const field = page.getByLabel(rx(`^${escapeRx(label)}\\s*\\*?$`)).first();
  await field.waitFor({ state: 'visible', timeout: 10000 });
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
    const inChrome = (el) => !!el.closest('nav, aside, header, [data-callout-layer]');
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
    const find = (label, type) => {
      const t = (type || '').toLowerCase();
      const pref = Object.entries(byType).find(([k]) => t.includes(k));
      // A column label goes on the table header; any other field never lands on a column header, so a filter and
      // a column of the same name get their own badges.
      const rest = generic.filter((g) => t.includes('column') || !['th', '[role=columnheader]'].includes(g));
      const selectors = pref ? [...pref[1], ...rest] : rest;
      for (const v of variants(label)) {
        for (const sel of selectors) {
          for (const el of document.querySelectorAll(sel)) {
            if (used.has(el) || inChrome(el) || !visible(el)) {
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
          if (!used.has(el) && visible(el) && norm(el.getAttribute('placeholder')) === v) {
            return el;
          }
        }
      }
      return null;
    };
    for (const [no, label, type] of items) {
      const el = find(label, type);
      if (!el) {
        missing.push(no);
        continue;
      }
      used.add(el);
      const r = el.getBoundingClientRect();
      // Beside a label (left of its first letter); inside the top-left corner of a column header or tab.
      const inside = el.matches('th, [role=columnheader], [role=tab], button, input, textarea');
      let x = r.left + window.scrollX + (inside ? 2 : -23);
      let y = r.top + window.scrollY + (inside ? -8 : Math.min(r.height, 24) / 2 - 10);
      // Keep badges of neighbouring fields apart.
      while (taken.some(([a, b]) => Math.abs(a - x) < 22 && Math.abs(b - y) < 22)) {
        x += 24;
      }
      taken.push([x, y]);
      const badge = document.createElement('div');
      badge.textContent = String(no);
      badge.style.cssText = `position:absolute;left:${Math.max(2, x)}px;top:${Math.max(2, y)}px;min-width:20px;` +
        'height:20px;padding:0 4px;box-sizing:border-box;border-radius:10px;background:#004EA8;color:#fff;' +
        'font:700 11px/16px Arial,sans-serif;text-align:center;border:2px solid #FDB913;' +
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

/** Grows the viewport so the whole scrolling content area is in the image (up to MAX_HEIGHT). */
async function fitViewport(page, tall) {
  const extra = await page.evaluate(() => {
    const main = document.querySelector('main.app-main, main');
    const doc = document.documentElement;
    const inner = main ? main.scrollHeight - main.clientHeight : 0;
    return Math.max(inner, doc.scrollHeight - doc.clientHeight, 0);
  });
  const height = Math.min(MAX_HEIGHT, HEIGHT + (tall === false ? 0 : extra));
  await page.setViewportSize({ width: WIDTH, height });
  await page.waitForTimeout(300);
}

function optimise(file) {
  const script = [
    'import sys',
    'from PIL import Image',
    'p = sys.argv[1]',
    'im = Image.open(p).convert("RGB")',
    'q = im.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)',
    'q.save(p, optimize=True)',
  ].join('\n');
  execFileSync(PYTHON, ['-c', script, file]);
}

// ------------------------------------------------------------------ main

(async () => {
  const manifest = loadManifest();
  const recipe = require(path.join(__dirname, 'packs', `${brd}.cjs`));
  const OUT = path.resolve(manifest.out);
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
    pageOf, browser, OUT, state: {} };
  // Callouts of a screen go on every image of that screen where the field is visible.
  const calloutsOf = {};
  manifest.shots.forEach((s) => {
    if (s.callouts && s.callouts.length) {
      calloutsOf[s.screen] = s.callouts;
    }
  });
  const shots = manifest.shots.filter((s) => !only || new RegExp(only).test(s.slug));
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
        optimise(file);
        report.push(`${shot.slug}: document`);
        console.log('captured', shot.slug);
        continue;
      }
      if (shot.state === 'walkthrough') {
        const step = recipe.walkthrough[shot.slug];
        if (!step) {
          throw new Error('no walkthrough step in the recipe');
        }
        page = await step(ctx, shot);
      } else {
        page = await pageOf(shot.user);
        await reachState(page, shot, recipe, ctx);
      }
      await page.evaluate(() => document.activeElement && document.activeElement.blur && document.activeElement.blur());
      await fitViewport(page, shot.tall);
      const missing = await drawCallouts(page, calloutsOf[shot.screen]);
      await page.screenshot({ path: file });
      await clearCallouts(page);
      optimise(file);
      const placed = (calloutsOf[shot.screen] || []).length - missing.length;
      report.push(`${shot.slug}: ${new URL(page.url()).pathname}, ${placed} callouts`);
      console.log('captured', shot.slug, new URL(page.url()).pathname, `${placed} callouts`);
    } catch (e) {
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
    await openTab(page, shot.tab);
  }
  if (shot.step) {
    await page.getByRole('button', { name: rx(escapeRx(shot.step)) }).first().click().catch(() => {});
    await page.getByText(shot.step, { exact: true }).first().click().catch(() => {});
    await settle(page);
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
