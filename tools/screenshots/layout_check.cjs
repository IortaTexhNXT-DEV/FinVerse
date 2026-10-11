// Application shell layout check: opens screens across the modules at two window sizes and checks
// that the window itself never scrolls, that the content area is the only scroll area, that the
// sidebar and content area reach the bottom of the window, that no empty band shows below the
// content, and that no tab strip scrolls vertically. Uses the seed profile, like capture.cjs.
//
//   SEED_PASSWORD=... BASE=http://localhost:5173 PLAYWRIGHT_MODULE=... CHROMIUM=... \
//   node tools/screenshots/layout_check.cjs [shot-folder]
//
// Exits with code 1 when any screen fails. With a shot folder, saves one image per screen and size.
const path = require('path');

const PLAYWRIGHT = process.env.PLAYWRIGHT_MODULE || 'playwright';
const { chromium } = require(PLAYWRIGHT);

const BASE = process.env.BASE || 'http://localhost:5173';
const PASSWORD = process.env.SEED_PASSWORD;
if (!PASSWORD) {
  console.error('Set SEED_PASSWORD to the password of the SIT/UAT users of the seed profile.');
  process.exit(2);
}
const SHOTS = process.argv[2];
const SIZES = [
  { width: 1366, height: 768 },
  { width: 1920, height: 1080 },
];

// Dashboards, long lists, short forms, the User Access Matrix, reports and settings.
const SCREENS = [
  ['mkttl', '/nb/dashboard'],
  ['mkttl', '/nb/reports'],
  ['mkttl', '/reports'],
  ['ao', '/my-work'],
  ['ao', '/crm/clients'],
  ['ao', '/crm/clients/new'],
  ['ao', '/quotations'],
  ['ao', '/accounts/direct-payment'],
  ['rnwtl', '/renewal'],
  ['rnwtl', '/renewal/expiry?tab=ALL'],
  ['rnwtl', '/renewal/review'],
  ['badmin', '/renewal/setup?tab=matrix'],
  ['badmin', '/catalog/products'],
  ['badmin', '/nb/targets'],
  ['tsuhead', '/product-maintenance'],
  ['tsu', '/product-maintenance/expiry'],
  ['cashier', '/cashiering'],
  ['cashier', '/cashiering/receive'],
  ['cashier', '/cashiering/receipts'],
  ['cashtl', '/cashiering/series'],
  ['fmanager', '/'],
  ['fmanager', '/gl/journals'],
  ['glofficer', '/frbs'],
  ['proc', '/booking'],
  ['proc', '/booking/setup'],
  ['csfmgmt', '/csf/reports'],
  ['approver', '/broking-setup/access-matrix'],
  ['ebao', '/eb/programmes?tab=ALL'],
  ['ao', '/profile'],
];

/** Runs in the page: measures the shell. */
function measure() {
  const vh = window.innerHeight;
  const main = document.getElementById('main-content');
  const sidebar = document.querySelector('.app-sidebar');
  const doc = document.scrollingElement;
  const scrollAreas = [];
  for (let el = main?.firstElementChild ?? null; el && el !== document.documentElement; el = el.parentElement) {
    const oy = getComputedStyle(el).overflowY;
    if (oy === 'auto' || oy === 'scroll') {
      scrollAreas.push(el.id || el.className || el.tagName);
    }
  }
  if (doc.scrollHeight > vh + 1) {
    scrollAreas.push('document');
  }
  const mainRect = main?.getBoundingClientRect();
  const tabStrips = [...document.querySelectorAll('[role="tablist"]')].filter(
    (el) => el.scrollHeight > el.clientHeight + 1 && getComputedStyle(el).overflowY !== 'hidden',
  ).length;
  return {
    tabStrips,
    docScroll: doc.scrollHeight - vh,
    scrollAreas,
    sidebarBottom: sidebar ? Math.round(sidebar.getBoundingClientRect().bottom) : null,
    mainBottom: mainRect ? Math.round(mainRect.bottom) : null,
    vh,
  };
}

async function main() {
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM || undefined });
  const failures = [];
  let checked = 0;
  for (const size of SIZES) {
    let current = null;
    let page = null;
    for (const [user, route] of SCREENS) {
      if (user !== current) {
        await page?.context().close();
        page = await (await browser.newContext({ viewport: size })).newPage();
        await page.goto(`${BASE}/login`);
        await page.getByLabel('User ID').fill(user);
        await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
        await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
        await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
        current = user;
      }
      await page.goto(BASE + route);
      await page.waitForLoadState('networkidle').catch(() => {});
      await page.waitForTimeout(800);
      const m = await page.evaluate(measure);
      const problems = [];
      if (m.docScroll > 1) problems.push(`window scrolls by ${m.docScroll}px`);
      if (m.scrollAreas.length !== 1) problems.push(`scroll areas: ${m.scrollAreas.join(', ') || 'none'}`);
      if (m.tabStrips > 0) problems.push(`${m.tabStrips} tab strip(s) scroll vertically`);
      if (m.sidebarBottom !== null && Math.abs(m.sidebarBottom - m.vh) > 1) {
        problems.push(`sidebar ends at ${m.sidebarBottom} of ${m.vh}`);
      }
      if (m.mainBottom === null || Math.abs(m.mainBottom - m.vh) > 1) {
        problems.push(`content area ends at ${m.mainBottom} of ${m.vh}`);
      }
      checked += 1;
      const label = `${size.width}x${size.height} ${route} (${user})`;
      console.log(`${problems.length ? 'FAIL' : 'ok  '} ${label} ${problems.join('; ')}`);
      if (problems.length) failures.push(label);
      if (SHOTS) {
        const name = `${size.width}-${route.replace(/[^a-z0-9]+/gi, '_')}.png`;
        await page.screenshot({ path: path.join(SHOTS, name) });
      }
    }
    await page?.context().close();
  }
  await browser.close();
  console.log(`${checked} checks, ${failures.length} failed`);
  process.exit(failures.length ? 1 : 0);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
