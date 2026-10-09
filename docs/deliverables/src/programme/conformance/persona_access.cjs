// Persona access check on a running seed profile: signs in as the SIT/UAT user of every persona of
// data/persona_expected.json, opens every group of the sidebar and lists the screens it shows, then compares them with
// the sidebar the FRS gives the persona (missing and extra screens), and opens a screen outside the persona's menu by
// its address to check the no-access message. Writes data/persona_run.json and a screenshot of every mismatch.
//
// Usage (seed profile running; the password of the SIT/UAT users in SEED_PASSWORD, never written to a file):
//   SEED_PASSWORD=... BASE=http://localhost:5173 SHOTS=<folder> PLAYWRIGHT_MODULE=... node persona_access.cjs
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const HERE = __dirname;
const BASE = process.env.BASE || 'http://localhost:5173';
const SHOTS = process.env.SHOTS || path.join(HERE, 'shots');
const PASSWORD = process.env.SEED_PASSWORD;
const personas = JSON.parse(fs.readFileSync(path.join(HERE, 'data', 'persona_expected.json'), 'utf8'));
// Screens tried by address when they are outside the persona's menu (the first one outside is used).
const PROBES = ['/admin/users', '/gl/journals', '/cashiering/receive', '/screening/cases', '/catalog/insurers'];
const DENIED = /You do not have access|not permitted|no access/i;

async function sidebar(page) {
  const nav = page.locator('nav.app-sidebar');
  for (let i = 0; i < 40; i += 1) {
    const closed = nav.locator('button[aria-expanded="false"]');
    if ((await closed.count()) === 0) break;
    await closed.first().click();
    await page.waitForTimeout(120);
  }
  const hrefs = await page.locator('nav.app-sidebar a[href]').evaluateAll((as) => as.map((a) => a.getAttribute('href')));
  return [...new Set(hrefs.filter((h) => h && h.startsWith('/')))].sort();
}

(async () => {
  if (!PASSWORD) throw new Error('Set SEED_PASSWORD');
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM || undefined });
  const seen = {};
  const results = [];
  for (const p of personas) {
    let shown = seen[p.user];
    let denial = null;
    let error = null;
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();
    page.setDefaultTimeout(20000);
    try {
      await page.goto(`${BASE}/login`);
      await page.getByLabel('User ID').fill(p.user);
      await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
      await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
      await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
      await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
      if (!shown) {
        shown = await sidebar(page);
        seen[p.user] = shown;
      }
      const probe = PROBES.find((x) => !p.expected.includes(x));
      if (probe) {
        await page.goto(`${BASE}${probe}`);
        await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
        await page.waitForTimeout(600);
        const text = await page.locator('body').innerText();
        denial = { screen: probe, denied: DENIED.test(text) };
      }
    } catch (e) {
      error = e.message.split('\n')[0];
    }
    const observed = shown || [];
    const missing = p.expected.filter((x) => !observed.includes(x));
    const extra = observed.filter((x) => !p.expected.includes(x));
    const frsMissing = p.frs_menu.filter((x) => !observed.includes(x));
    const ok = !error && missing.length === 0 && extra.length === 0 && (!denial || denial.denied);
    if (!ok) {
      fs.mkdirSync(SHOTS, { recursive: true });
      await page.screenshot({ path: path.join(SHOTS, `persona_${p.brd}_${p.user}.png`) }).catch(() => {});
    }
    await context.close();
    results.push({ ...p, observed, missing, extra, frs_missing: frsMissing, denial, error, ok });
    console.log(p.brd, p.user, p.role, ok ? 'ok' : 'MISMATCH', error || '', missing.join(' '), extra.length ? `extra ${extra.join(' ')}` : '',
      denial && !denial.denied ? `opens ${denial.screen}` : '');
  }
  await browser.close();
  fs.writeFileSync(path.join(HERE, 'data', 'persona_run.json'), JSON.stringify(results, null, 1));
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
