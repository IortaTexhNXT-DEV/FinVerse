// Captures the screens of the UX Design of the Remittance (BRD-02) and Claims (BRD-07) packs in BDOI's format, and
// the Co-insurance / Risk Participation tab of the insurer page (BRD-03), from the running seed profile. One full page
// per screen, saved where the pack builders read them:
//   docs/deliverables/src/programme/bdoi_template_frs/figures/brd02r_v11_screens/<SCR-ID>.png
//   docs/deliverables/src/programme/bdoi_template_frs/figures/brd07_v11_screens/<SCR-ID>.png
//   docs/deliverables/src/programme/bdoi_template_frs/figures/brd03_v14_screens/ux-18-coinsurance.png
// Usage (seed profile running on a fresh database; see README.md):
//   SEED_PASSWORD=... BASE=http://localhost:5195 API=http://localhost:8095 \
//   PLAYWRIGHT_MODULE=/opt/node22/lib/node_modules/playwright CHROMIUM=/opt/pw-browsers/chromium-1194/chrome-linux/chrome \
//   node tools/screenshots/capture_sprint_screens.cjs [regex of screen ids]
const fs = require('fs');
const path = require('path');

const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const ROOT = path.resolve(__dirname, '../..');
const FIG = path.join(ROOT, 'docs/deliverables/src/programme/bdoi_template_frs/figures');
const BASE = process.env.BASE || 'http://localhost:5173';
const API = process.env.API || 'http://localhost:8080';
const PASSWORD = process.env.SEED_PASSWORD;
if (!PASSWORD) {
  console.error('Set SEED_PASSWORD to the password of the SIT/UAT users of the seed profile.');
  process.exit(2);
}
const FILTER = new RegExp(process.argv[2] || '.');
const VIEWPORT = { width: 1600, height: 1000 };

// id, folder, user, route, how to reach the state
const SCREENS = [
  ['SCR-RMT-01', 'brd02r_v11_screens', 'remittl', '/remittance'],
  ['SCR-RMT-02', 'brd02r_v11_screens', 'remit', '/remittance/extraction'],
  ['SCR-RMT-03', 'brd02r_v11_screens', 'remit', '/remittance/batches'],
  ['SCR-RMT-04', 'brd02r_v11_screens', 'remit', '/remittance/batches', { open: true }],
  ['SCR-RMT-04-files', 'brd02r_v11_screens', 'remit', '/remittance/batches', { open: true, tab: 'Documents and Receipts' }],
  ['SCR-RMT-05', 'brd02r_v11_screens', 'remit', '/remittance/insurer-or'],
  ['SCR-RMT-06', 'brd02r_v11_screens', 'remittl', '/remittance/holds'],
  ['SCR-RMT-07', 'brd02r_v11_screens', 'remittl', '/remittance/holds', { open: true }],
  ['SCR-RMT-08', 'brd02r_v11_screens', 'remittl', '/remittance/special'],
  ['SCR-RMT-09', 'brd02r_v11_screens', 'remittl', '/remittance/special', { open: true }],
  ['SCR-RMT-10', 'brd02r_v11_screens', 'remit', '/remittance/dtip'],
  ['SCR-RMT-11', 'brd02r_v11_screens', 'remittl', '/remittance/incentive-rules'],
  ['SCR-RMT-12', 'brd02r_v11_screens', 'remittl', '/reports'],
  ['SCR-RMT-13', 'brd02r_v11_screens', 'remittl', '/remittance/settings'],
  ['SCR-CLM-01', 'brd07_v11_screens', 'clmofficer', '/claims-handling'],
  ['SCR-CLM-02', 'brd07_v11_screens', 'clmofficer', '/claims-handling/worklist?tab=ALL'],
  ['SCR-CLM-03', 'brd07_v11_screens', 'clmofficer', '/claims-handling/new'],
  ['SCR-CLM-04', 'brd07_v11_screens', 'clmtl', '/claims-handling/worklist?tab=ALL', { open: true }],
  ['SCR-CLM-04-followups', 'brd07_v11_screens', 'clmtl', '/claims-handling/worklist?tab=ALL', { open: true, tab: 'Follow-ups' }],
  ['SCR-CLM-04-documents', 'brd07_v11_screens', 'clmtl', '/claims-handling/worklist?tab=ALL', { open: true, tab: 'Documents' }],
  ['SCR-CLM-05', 'brd07_v11_screens', 'clmofficer', '/claims-handling/covers'],
  ['SCR-CLM-06', 'brd07_v11_screens', 'clmofficer', '/claims-handling/diary'],
  ['SCR-CLM-07', 'brd07_v11_screens', 'clmuh', '/claims-handling/location-refs'],
  ['SCR-CLM-08', 'brd07_v11_screens', 'clmuh', '/claims-handling/reports'],
  ['SCR-CLM-09', 'brd07_v11_screens', 'clmuh', '/claims-handling/setup'],
  ['SCR-CLM-09-handlers', 'brd07_v11_screens', 'clmuh', '/claims-handling/setup', { tab: 'Claims Handler Register' }],
  ['SCR-CLM-09-checklist', 'brd07_v11_screens', 'clmuh', '/claims-handling/setup', { tab: 'Document Checklist' }],
  ['ux-18-coinsurance', 'brd03_v14_screens', 'badmin', '/catalog/insurers', { open: true, tab: 'Co-insurance / Risk Participation' }],
];

async function settle(page) {
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(1200);
}

async function waitUntilReady() {
  const deadline = Date.now() + 30 * 60 * 1000;
  while (Date.now() < deadline) {
    const status = await fetch(`${API}/actuator/health/readiness`)
      .then((r) => r.json())
      .then((b) => b.status)
      .catch(() => 'DOWN');
    if (status === 'UP') {
      return;
    }
    await new Promise((r) => setTimeout(r, 5000));
  }
  throw new Error('backend not ready');
}

async function signIn(browser, user) {
  const page = await (await browser.newContext({ viewport: VIEWPORT })).newPage();
  page.setDefaultTimeout(30000);
  await page.goto(`${BASE}/login`);
  await page.getByLabel('User ID').fill(user);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
  await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
  return page;
}

async function capture(page, [id, folder, , route, how = {}]) {
  await page.goto(BASE + route);
  await settle(page);
  if (how.open) {
    const link = page.locator('table tbody tr a').first();
    if ((await link.count()) > 0) {
      await link.click();
    } else {
      await page.locator('table tbody tr').first().click();
    }
    await settle(page);
  }
  if (how.tab) {
    await page.getByRole('tab', { name: how.tab, exact: true }).first().click();
    await settle(page);
  }
  const dir = path.join(FIG, folder);
  fs.mkdirSync(dir, { recursive: true });
  await page.screenshot({ path: path.join(dir, `${id}.png`), fullPage: true });
  return new URL(page.url()).pathname;
}

(async () => {
  await waitUntilReady();
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const pages = {};
  const failed = [];
  for (const screen of SCREENS.filter((s) => FILTER.test(s[0]))) {
    const user = screen[2];
    try {
      pages[user] = pages[user] || (await signIn(browser, user));
      const url = await capture(pages[user], screen);
      console.log(`${screen[0]}: ${url}`);
    } catch (e) {
      failed.push(`${screen[0]}: ${e.message.split('\n')[0]}`);
    }
  }
  await browser.close();
  if (failed.length > 0) {
    console.error(`Not captured:\n${failed.join('\n')}`);
    process.exit(1);
  }
})();
