// Screen checks of the walkthrough steps of step_checks.yaml on a running seed profile: for each step, signs in as
// the persona of the step, opens each screen (a record page from the first row of its list) and checks that the
// screen opens for the persona and shows the tabs, buttons or texts the step needs. Writes data/steps_run.json and a
// screenshot of every failed check (SHOTS folder).
//
// Usage (seed profile running; the password of the SIT/UAT users in SEED_PASSWORD, never written to a file):
//   SEED_PASSWORD=... BASE=http://localhost:5173 SHOTS=<folder> PLAYWRIGHT_MODULE=... node run_steps.cjs [step-regex]
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const HERE = __dirname;
const BASE = process.env.BASE || 'http://localhost:5173';
const SHOTS = process.env.SHOTS || path.join(HERE, 'shots');
const PASSWORD = process.env.SEED_PASSWORD;
const ONLY = process.argv[2] ? new RegExp(process.argv[2]) : null;

const plan = JSON.parse(fs.readFileSync(path.join(HERE, 'data', 'steps_plan.json'), 'utf8'));
const curated = JSON.parse(execFileSync('python3', ['-c',
  'import json,sys,yaml; print(json.dumps(yaml.safe_load(open(sys.argv[1]))))', path.join(HERE, 'step_checks.yaml')]));

const DENIED = /You do not have access|Page not found|not permitted|Something went wrong|could not be loaded/i;

async function signIn(browser, user) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  await page.goto(`${BASE}/login`);
  await page.getByLabel('User ID').fill(user);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
  await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
  return page;
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(800);
}

/** Opens the first record of a list: the first link of the first row, else the row. */
async function openFirst(page, list) {
  await page.goto(`${BASE}${list}`);
  await settle(page);
  const card = page.locator('main a.report-card').first();
  if ((await card.count()) > 0) {
    const before = page.url();
    await card.click();
    await page.waitForURL((u) => u.toString() !== before, { timeout: 10000 }).catch(() => {});
    await settle(page);
    return null;
  }
  let row = page.locator('main table tbody tr').filter({ hasNotText: /^\s*No .* (match|to display|recorded|found)/i }).first();
  if ((await row.count()) === 0 || /^\s*No \w/.test(await row.innerText().catch(() => ''))) {
    // A work list opens on the user's own tab: look in the tab of all records.
    const all = page.getByRole('tab', { name: /^All$/ }).or(page.getByRole('button', { name: /^All$/ })).first();
    if ((await all.count()) > 0) {
      await all.click();
      await settle(page);
      row = page.locator('main table tbody tr').first();
    }
  }
  if ((await row.count()) === 0 || /^\s*No \w/.test(await row.innerText().catch(() => ''))) {
    return 'the list has no record';
  }
  const link = row.locator('a').first();
  const before = page.url();
  if ((await link.count()) > 0) {
    await link.click();
  } else {
    await row.click();
  }
  await page.waitForURL((u) => u.toString() !== before, { timeout: 10000 }).catch(() => {});
  await settle(page);
  return page.url() === before ? 'no record page opened from the list' : null;
}

async function check(page, c, shotName) {
  const [target, list, items] = c;
  let problem = null;
  if (target.includes(':')) {
    problem = await openFirst(page, list);
  } else {
    await page.goto(`${BASE}${target}`);
    await settle(page);
  }
  const text = await page.locator('body').innerText().catch(() => '');
  if (!problem && DENIED.test(text)) {
    problem = `the screen shows: ${text.match(DENIED)[0]}`;
  }
  const missing = [];
  for (const item of items || []) {
    const re = new RegExp(item.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
    if (!re.test(text)) {
      // Tabs and actions may sit behind a "more" menu: open the row or page menus and look again.
      const more = page.getByRole('button', { name: /more|actions/i }).first();
      if ((await more.count()) > 0) {
        await more.click().catch(() => {});
        await page.waitForTimeout(300);
      }
      const again = await page.locator('body').innerText().catch(() => '');
      if (!re.test(again)) {
        missing.push(item);
      }
      await page.keyboard.press('Escape').catch(() => {});
    }
  }
  const url = new URL(page.url()).pathname;
  const ok = !problem && missing.length === 0;
  if (!ok) {
    fs.mkdirSync(SHOTS, { recursive: true });
    await page.screenshot({ path: path.join(SHOTS, `${shotName}.png`) }).catch(() => {});
  }
  return { screen: target, url, ok, problem, missing };
}

(async () => {
  if (!PASSWORD) {
    throw new Error('Set SEED_PASSWORD');
  }
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM || undefined });
  const results = [];
  for (const step of plan) {
    const spec = (curated[step.brd] || {})[step.step];
    if (!spec || (ONLY && !ONLY.test(`${step.brd} ${step.step}`))) {
      continue;
    }
    const entry = { brd: step.brd, step: step.step, user: spec.user || step.user, checks: [] };
    if (spec.blocked || (spec.gap && !spec.checks)) {
      entry.status = spec.blocked ? 'blocked' : 'fail';
      entry.note = spec.blocked || `gap ${spec.gap}`;
      results.push(entry);
      continue;
    }
    try {
      const page = await signIn(browser, entry.user);
      let n = 0;
      for (const c of spec.checks || []) {
        n += 1;
        entry.checks.push(await check(page, c, `${step.brd}_${step.step}_${n}`.replace(/[^\w.-]/g, '_')));
      }
      await page.context().close();
      entry.status = entry.checks.every((c) => c.ok) ? 'pass' : 'fail';
    } catch (e) {
      entry.status = 'fail';
      entry.note = e.message.split('\n')[0];
    }
    console.log(entry.brd, entry.step, entry.user, entry.status,
      entry.checks.filter((c) => !c.ok).map((c) => `${c.screen}: ${c.problem || ''} ${c.missing.join(', ')}`).join(' | '));
    results.push(entry);
  }
  await browser.close();
  const out = path.join(HERE, 'data', 'steps_run.json');
  fs.writeFileSync(out, JSON.stringify(results, null, 1));
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
