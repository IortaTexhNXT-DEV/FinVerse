// End-to-end walkthroughs of BRD-11 User Access Maintenance, performed live on the seed profile by capture_pack.cjs
// (see walkthroughs.yaml of the pack). The walkthroughs follow the UIDM-ISC / Microsoft Entra ID model of the BRD-11
// FRS v1.6: the joiner, mover, leaver and group-profile events of UIDM-ISC are sent from the Enterprise SSO simulator
// of Identity Sync (SIT and UAT), which feeds the same intake as the SCIM interface. Each step signs in as the persona
// of the step, does what the step says on the screen and returns the page to capture. The walkthroughs run in order:
// B, C and D use the user created in A. The records created carry fictitious seed values only.
const { act, tab, go, button, settle, chooseOption, rowAction } = require('./brd01_walkthrough.cjs');

// The permission picker of the group-profile requests: areas closed, check boxes found by the permission code at the end of
// their id. Opens every area, ticks the permissions, closes the areas again and opens the area of the first one, so the
// form stays short; the selected permissions show by area on the right.
async function pickPermissions(page, codes) {
  await page.getByRole('button', { name: /^expand all$/i }).click();
  const areas = new Set();
  for (const code of codes) {
    const box = page.locator(`.permission-picker input[type=checkbox][id$="-${code}"]`);
    await box.check();
    const group = box.locator('xpath=ancestor::div[contains(concat(" ", @class, " "), " profile-group ")][1]');
    areas.add((await group.locator('.profile-group-name').textContent()).trim());
  }
  await page.getByRole('button', { name: /^collapse all$/i }).click();
  for (const area of [...areas].slice(0, 1)) {
    const name = page.locator('.profile-group-name', { hasText: new RegExp(`^${area.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`) });
    await page.locator('.profile-group-head').filter({ has: name }).first().click();
  }
}

const SIM = '/admin/identity/simulator/accounts';
const SYNC = '/access/identity-sync';
const NEW_USER = 'a013000196';
const PROFILE = 'MKT_AO';
const ISABEL = {
  windowsId: 'BDO\\inavarro', userId: NEW_USER, email: 'isabel.navarro@uat.brokerverse.cloud', firstName: 'Isabel',
  lastName: 'Navarro', displayName: 'Isabel Navarro', adGroup: 'BIBS Users', status: 'ACTIVE',
  unitSegment: 'Retail Marketing', department: 'Marketing', location: 'Makati', uidmRequestNo: 'UIDM-2026-005196',
};

// ------------------------------------------------------------------ helpers

const userId = (ctx) => ctx.one(`select id from sec_user where username = '${NEW_USER}'`);
const hasUser = (ctx) => ctx.sql(`select 1 from sec_user where username = '${NEW_USER}'`).length > 0;
const simulated = async (ctx) => (await ctx.api('uamadmin', 'GET', SIM)).find((a) => a.windowsId === ISABEL.windowsId);
const event = (type) => `${SIM}/events/${type}?windowsId=${encodeURIComponent(ISABEL.windowsId)}`;
const accountRow = (page) => page.locator('main table tbody tr').filter({ hasText: ISABEL.windowsId }).first();

/** The Simulator tab of Identity Sync, as the User Access Administrator. */
async function simulator(ctx) {
  const page = await go(ctx, 'uamadmin', `${SYNC}?tab=simulator`);
  await tab(page, 'Simulator');
  return page;
}

/**
 * Chooses an action of the row menu of the account. The menu opens below the row and its last entries can fall
 * below the scrolling card, so the entry is clicked through the page rather than at its screen position.
 */
async function accountAction(page, name) {
  const row = accountRow(page);
  const menu = row.getByRole('button', { name: /^Actions for/ });
  await menu.evaluate((el) => el.scrollIntoView({ block: 'center' }));
  await menu.click();
  const item = page.getByRole('menuitem', { name }).first();
  await item.waitFor({ state: 'attached', timeout: 5000 });
  await item.evaluate((el) => el.click());
  await page.waitForTimeout(600);
}

/** Sends the group profiles of the account from its row menu (Send group profiles dialog). */
async function sendGroups(page, groups, requestNo) {
  await accountAction(page, /^send group profiles$/i);
  const dialog = page.getByRole('dialog').last();
  await dialog.getByLabel(/^Group profile codes/).fill(groups);
  await dialog.getByLabel(/^UIDM request no/).fill(requestNo);
  await dialog.getByRole('button', { name: /^send$/i }).click();
  await settle(page, 1500);
}

/** The pending change of the profile, or the last one. */
const changeOf = (ctx, where = "status not in ('IMPLEMENTED','REJECTED','CANCELLED')") =>
  ctx.sql(`select id, status from nba_access_request where role_code = '${PROFILE}' and ${where} order by id desc limit 1`)[0];

/** Sets the sign-in mode of the environment (security parameter: change, then second approval). */
async function authMode(ctx, mode) {
  if (ctx.one("select param_value from sys_parameter where param_key = 'AUTH_MODE'") === mode) {
    return;
  }
  await ctx.api('admin', 'PUT', '/system/parameters/AUTH_MODE', { value: mode });
  await ctx.api('infosec', 'POST', '/system/parameters/AUTH_MODE/approve');
}

/** Signs in through the Enterprise SSO simulator page, which stands in for Microsoft Entra ID in SIT and UAT. */
async function ssoSignIn(ctx, windowsId) {
  const context = await ctx.newContext();
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  await page.goto(`${ctx.BASE}/login`);
  await settle(page, 400);
  await page.getByRole('button', { name: /microsoft entra id/i }).click();
  await page.waitForURL(/eiam-simulator/);
  await page.selectOption('select[name=windowsId]', windowsId);
  // The simulator's page and the web client of the test environment run on different ports, so the browser's form
  // policy would stop the redirect back to the web client: the form is posted by the recipe and its redirect (the
  // callback with the code) is followed in the page, as the browser does on one address.
  const form = await page.locator('form').evaluate((f) => ({
    action: new URL(f.getAttribute('action'), location.href).href,
    data: Object.fromEntries(new FormData(f).entries()),
  }));
  const answer = await page.request.post(form.action, { form: form.data, maxRedirects: 0 });
  await page.goto(answer.headers().location);
  await page.waitForURL((u) => !u.pathname.startsWith('/sso') && !u.pathname.startsWith('/login'), { timeout: 30000 });
  await settle(page, 1500);
  return page;
}

// ------------------------------------------------------------------ walkthrough A: joiner to first sign-in

const steps = {
  // 1. The Enterprise SSO account of the joiner, then Send joiner.
  'wt-a-01': async (ctx) => {
    const page = await simulator(ctx);
    if (!(await simulated(ctx))) {
      await button(page, /^new account$/i).click();
      const d = page.getByRole('dialog').last();
      await d.getByLabel(/^Windows ID/).fill(ISABEL.windowsId);
      await d.getByLabel(/^User ID/).fill(ISABEL.userId);
      for (const [label, key] of [['First name', 'firstName'], ['Last name', 'lastName'], ['Display name', 'displayName'],
        ['AD e-mail', 'email'], ['Unit / segment', 'unitSegment'], ['Department', 'department'], ['Location', 'location'],
        ['UIDM request no.', 'uidmRequestNo']]) {
        await d.getByLabel(new RegExp(`^${label.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&')}`)).fill(ISABEL[key]);
      }
      await d.getByRole('button', { name: /^save$/i }).click();
      await settle(page, 1200);
    }
    if (!hasUser(ctx)) {
      await accountAction(page, /^send joiner$/i);
      await settle(page, 1500);
    }
    return page;
  },
  // 2. The group profile of the joiner.
  'wt-a-02': async (ctx) => {
    const page = await simulator(ctx);
    await sendGroups(page, PROFILE, ISABEL.uidmRequestNo);
    return page;
  },
  // 3. The events of the joiner.
  'wt-a-03': async (ctx) => {
    const page = await go(ctx, 'uamadmin', `${SYNC}?tab=events`);
    await tab(page, 'Events');
    return page;
  },
  // 4. The user with its group profile.
  'wt-a-04': async (ctx) => {
    const page = await go(ctx, 'uamadmin', `/access/users/${userId(ctx)}?tab=profiles`);
    return page;
  },
  // 5. First sign-in through the identity provider of the test environment. The sign-in mode of the environment is
  // switched to single sign-on (AUTH_MODE OIDC, a security parameter: System Administrator, then the Information
  // Security Officer approves) for the step and back to LOCAL after it, so the other personas keep their passwords.
  // The backend runs with the Enterprise SSO simulator as its OpenID Connect provider (BROKERVERSE_OIDC_ISSUER =
  // <API>/eiam-simulator, see tools/screenshots/README.md).
  'wt-a-05': async (ctx) => {
    await authMode(ctx, 'OIDC');
    try {
      return await ssoSignIn(ctx, ISABEL.windowsId);
    } finally {
      await authMode(ctx, 'LOCAL');
    }
  },
  // 6. The first session of the user.
  'wt-a-06': async (ctx) => go(ctx, 'uamadmin', `/access/users/${userId(ctx)}?tab=sessions`),

  // ---------------------------------------------------------------- walkthrough B: group-profile change
  // 1. The maker proposes one more permission for the Marketing Account Officer; the pending change offers the maker
  // no decision.
  'wt-b-01': async (ctx) => {
    const page = await go(ctx, 'requestor', `/access/group-profiles/${PROFILE}`);
    if (!changeOf(ctx)) {
      const roles = await ctx.api('requestor', 'GET', '/nbadmin/roles');
      const have = roles.find((r) => r.code === PROFILE).permissions;
      const add = roles.find((r) => r.code === 'MKT_TL').permissions.find((c) => !have.includes(c));
      await button(page, /^propose change$/i).click();
      await page.waitForTimeout(800);
      await pickPermissions(page, [add]);
      const d = page.getByRole('dialog').last();
      await d.getByLabel(/^Reason/).fill('Account officers need the permission for the monthly portfolio review');
      await d.getByRole('button', { name: /^submit for approval$/i }).click();
      await settle(page, 1500);
    }
    await go(ctx, 'requestor', `/access/group-profiles/${PROFILE}?tab=pending`);
    return page;
  },
  // 3. The checker approves with a remark.
  'wt-b-03': async (ctx) => {
    const page = await go(ctx, 'uamapprover', `/access/group-profiles/${PROFILE}?tab=pending`);
    const c = changeOf(ctx);
    if (c && c[1] === 'PENDING') {
      await act(page, /^approve$/i, { confirm: /^approve$/i, comment: 'Checked against the access matrix', reason: false });
    }
    return page;
  },
  // 4. The System Administrator implements the approved change.
  'wt-b-04': async (ctx) => {
    const page = await go(ctx, 'admin', `/access/group-profiles/${PROFILE}?tab=pending`);
    const c = changeOf(ctx);
    if (c && c[1] !== 'IMPLEMENTED') {
      await act(page, /^implement$/i, { confirm: /^implement$/i, reason: false });
    }
    return page;
  },
  // 5. The history of the profile, as the Auditor.
  'wt-b-05': async (ctx) => go(ctx, 'uamauditor', `/access/group-profiles/${PROFILE}?tab=history`),

  // ---------------------------------------------------------------- walkthrough C: refused event, reconciliation
  // 1. A group profile BIBS does not hold: the event is refused.
  'wt-c-01': async (ctx) => {
    const page = await simulator(ctx);
    await sendGroups(page, `${PROFILE}, MKT_SUPERVISOR`, 'UIDM-2026-005262');
    return page;
  },
  // 2. The refused event to review.
  'wt-c-02': async (ctx) => {
    const page = await go(ctx, 'uamadmin', `${SYNC}?tab=refused`);
    await tab(page, 'Refused');
    return page;
  },
  // 3. Dismissed with the remark once UIDM-ISC corrected the request.
  'wt-c-03': async (ctx) => {
    const page = await go(ctx, 'uamadmin', `${SYNC}?tab=refused`);
    await tab(page, 'Refused');
    const row = page.locator('main table tbody tr').filter({ hasText: /MKT_SUPERVISOR/ }).first();
    if (await row.isVisible().catch(() => false)) {
      await rowAction(row, /^dismiss$/i);
      const d = page.getByRole('dialog').last();
      await d.locator('textarea, input[type=text]').first()
        .fill('MKT_SUPERVISOR is not a group profile of BIBS; UIDM-ISC corrected request UIDM-2026-005262 to MKT_AO only');
      await d.getByRole('button', { name: /^dismiss$/i }).last().click();
      await settle(page, 1200);
    }
    return page;
  },
  // 4. The directory account made inactive without a leaver: the reconciliation finds the difference.
  'wt-c-04': async (ctx) => {
    const account = await simulated(ctx);
    if (account && account.status === 'ACTIVE') {
      await ctx.api('uamadmin', 'PUT', `${SIM}?windowsId=${encodeURIComponent(ISABEL.windowsId)}`,
        { ...account, status: 'INACTIVE', uidmRequestNo: 'UIDM-2026-005270' });
    }
    const page = await go(ctx, 'uamadmin', `${SYNC}?tab=reconciliation`);
    await tab(page, 'Reconciliation');
    await button(page, /^run now$/i).click();
    await settle(page, 2500);
    return page;
  },
  // 5. The leaver sent by UIDM-ISC, then the mismatch resolved with a remark.
  'wt-c-05': async (ctx) => {
    if (ctx.one(`select enabled from sec_user where username = '${NEW_USER}'`) === 't') {
      await ctx.api('uamadmin', 'POST', event('LEAVER'));
    }
    const page = await go(ctx, 'uamadmin', `${SYNC}?tab=reconciliation`);
    await tab(page, 'Reconciliation');
    const row = page.locator('main table tbody tr').filter({ hasText: NEW_USER }).first();
    if (await row.isVisible().catch(() => false)) {
      await rowAction(row, /^resolve$/i);
      const d = page.getByRole('dialog').last();
      await d.locator('textarea, input[type=text]').first()
        .fill('Leaver UIDM-2026-005270 sent by UIDM-ISC; the user is inactive in both systems');
      await d.getByRole('button', { name: /^resolve$/i }).last().click();
      await settle(page, 1200);
    }
    return page;
  },
  // 6. The connection with UIDM-ISC.
  'wt-c-06': async (ctx) => go(ctx, 'uamadmin', SYNC),

  // ---------------------------------------------------------------- walkthrough D: evidence and certification
  // 1. The certification extract (XLSX), logged with its SHA-256.
  'wt-d-01': async (ctx) => {
    const page = await go(ctx, 'uamauditor', '/access/reports');
    await button(page, /^extract xlsx$/i).click();
    await page.waitForTimeout(600);
    const download = page.waitForEvent('download', { timeout: 20000 }).catch(() => null);
    await page.getByRole('dialog').last().getByRole('button', { name: /^take extract$/i }).click();
    await download;
    await settle(page, 1200);
    return page;
  },
  // 2. The audit log of user access.
  'wt-d-02': async (ctx) => go(ctx, 'uamauditor', '/access/audit'),
  // 3. The access matrix.
  'wt-d-03': async (ctx) => go(ctx, 'uamauditor', '/access/matrix'),
};

/** Nothing to prepare: the records of each step come from the seed data or from the earlier steps. */
async function prepare() {}

module.exports = { steps, prepare, pickPermissions, chooseOption };
