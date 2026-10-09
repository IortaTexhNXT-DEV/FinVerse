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
  await page.locator('nav.app-sidebar').waitFor({ timeout: 15000 });
  // Opens every closed group of the sidebar (a group renders its screens only when open).
  for (let i = 0; i < 5; i += 1) {
    const opened = await page.locator('nav.app-sidebar button[aria-expanded="false"]').evaluateAll((bs) => {
      bs.forEach((b) => b.click());
      return bs.length;
    });
    if (opened === 0) break;
    await page.waitForTimeout(250);
  }
  const hrefs = await page.locator('nav.app-sidebar a.nav-link[href]').evaluateAll((as) => as.map((a) => a.getAttribute('href')));
  return [...new Set(hrefs.filter((h) => h && h.startsWith('/')))].sort();
}

(async () => {
  if (!PASSWORD) throw new Error('Set SEED_PASSWORD');
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM || undefined });
  const seen = {}; // per sign-in: the sidebar, the screen probed by its address and the sign-in error
  const results = [];
  for (const p of personas) {
    let error = null;
    let page = null;
    let context = null;
    if (!seen[p.user]) {
      context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
      page = await context.newPage();
      page.setDefaultTimeout(20000);
      const entry = { shown: [], denial: null, error: null };
      try {
        await page.goto(`${BASE}/login`);
        await page.getByLabel('User ID').fill(p.user);
        await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
        await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
        await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
        await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
        entry.shown = await sidebar(page);
        const probe = PROBES.find((x) => !p.expected.includes(x));
        if (probe) {
          await page.goto(`${BASE}${probe}`);
          await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
          await page.waitForTimeout(500);
          entry.denial = { screen: probe, denied: DENIED.test(await page.locator('body').innerText()) };
        }
      } catch (e) {
        entry.error = e.message.split('\n')[0];
      }
      seen[p.user] = entry;
    }
    const { shown, denial } = seen[p.user];
    error = seen[p.user].error;
    const observed = shown || [];
    const missing = p.expected.filter((x) => !observed.includes(x));
    const extra = observed.filter((x) => !p.expected.includes(x));
    const frsMissing = p.frs_menu.filter((x) => !observed.includes(x));
    const ok = !error && missing.length === 0 && extra.length === 0 && (!denial || denial.denied);
    if (!ok && page) {
      fs.mkdirSync(SHOTS, { recursive: true });
      await page.screenshot({ path: path.join(SHOTS, `persona_${p.brd}_${p.user}.png`) }).catch(() => {});
    }
    if (context) {
      await context.close();
    }
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
