// End-to-end walkthroughs of BRD-11 User Access Maintenance, performed live on the seed profile by capture_pack.cjs
// (see walkthroughs.yaml of the pack). Each step signs in as the persona of the step, does what the step says on the
// screen and returns the page to capture. The walkthroughs run in order: B changes and deactivates the user enrolled
// in A. The records created carry fictitious seed values only; the SIT/UAT password comes from SEED_PASSWORD.
const { act, press, tab, go, button, settle } = require('./brd01_walkthrough.cjs');

const REQUESTS = '/user-access/requests';
const NEW_USER = 'a013000196';
const PROFILE = 'RENEWAL_ENQUIRY';

// ------------------------------------------------------------------ helpers

const requestOf = (ctx, where) => ctx.sql(`select id from nba_access_request where ${where} order by id desc limit 1`)[0]?.[0];
const seedRequest = (ctx, no) => ctx.one(`select id from nba_access_request where request_no = '${no}'`);

async function fill(page, label, value) {
  // A text value goes to the drop-down or text field of the label, not to a check box of the same name (the
  // group profile "Approver" next to the Approver drop-down).
  const matches = page.getByLabel(new RegExp(`^${label}`));
  let field = matches.first();
  if (typeof value !== 'boolean' && (await matches.count()) > 1) {
    field = matches.and(page.locator('select, input:not([type=checkbox]), textarea')).first();
  }
  const tag = await field.evaluate((e) => e.tagName.toLowerCase());
  if (tag === 'select') {
    const options = await field.locator('option').allTextContents();
    const hit = options.find((o) => new RegExp(value, 'i').test(o));
    if (hit === undefined) {
      throw new Error(`option ${value} not in ${label}: ${options.slice(0, 10).join(', ')}`);
    }
    await field.selectOption({ label: hit });
  } else if ((await field.getAttribute('type')) === 'checkbox') {
    await field.check();
  } else {
    await field.fill(String(value));
  }
  await page.waitForTimeout(200);
}

async function openRequest(ctx, user, id, tabName) {
  const page = await go(ctx, user, `${REQUESTS}/${id}`);
  if (tabName) {
    await tab(page, tabName);
  }
  return page;
}

/** A page of its own for a user that is not a seed persona (the new user of walkthrough A). */
async function ownPage(ctx, user, password) {
  const context = await ctx.newContext();
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  await page.goto(`${ctx.BASE}/login`);
  await settle(page, 300);
  await page.getByLabel('User ID').fill(user);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: /^login$/i }).click();
  await settle(page, 1500);
  return page;
}

async function runReport(ctx, user, code, params) {
  const page = await go(ctx, user, `/reports/${code}`);
  for (const [label, value] of params) {
    await fill(page, label, value);
  }
  await button(page, /^run report$/i).click();
  await settle(page, 2000);
  return page;
}

// ------------------------------------------------------------------ walkthrough A

const steps = {
  'wt-a-01': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new`);
    if (requestOf(ctx, `username = '${NEW_USER}'`)) {
      return page;
    }
    await fill(page, 'User ID', NEW_USER);
    await fill(page, 'Full Name', 'Isabel Navarro');
    await fill(page, 'E-mail', 'isabel.navarro@brokerverse-seed.ph');
    await fill(page, 'Windows ID', 'INAVARRO');
    await fill(page, 'Home Branch', '^HO');
    await page.getByLabel('Marketing Account Officer', { exact: true }).check();
    await fill(page, 'Approver', 'Ulysses');
    await fill(page, 'Remarks \\(Justification\\)', 'Joined Combank Marketing as account officer (seed data)');
    await button(page, /^save draft$/i).click();
    await settle(page, 1500);
    return page;
  },
  'wt-a-02': async (ctx) => {
    const id = requestOf(ctx, `username = '${NEW_USER}' and request_type = 'CREATE_USER'`);
    const page = await openRequest(ctx, 'requestor', id);
    if (ctx.one(`select status from nba_access_request where id = ${id}`) === 'DRAFT') {
      await button(page, /^edit request$/i).click();
      await settle(page);
      // The approver is chosen when the request is submitted (a draft does not keep it).
      await fill(page, 'Approver', 'Ulysses');
      await press(page, /^submit$/i);
      await settle(page, 1500);
    }
    return page;
  },
  'wt-a-03': async (ctx) => go(ctx, 'uamapprover', REQUESTS),
  'wt-a-04': async (ctx) => {
    const id = requestOf(ctx, `username = '${NEW_USER}' and request_type = 'CREATE_USER'`);
    const page = await openRequest(ctx, 'uamapprover', id);
    if (ctx.one(`select status from nba_access_request where id = ${id}`) === 'PENDING') {
      await act(page, /^approve and apply$/i, { comment: 'Welcome to BIBS' });
      ctx.state.temporary = (await page.locator('code.secret-value').first().innerText()).trim();
    }
    return page;
  },
  'wt-a-05': async (ctx) => {
    if (!ctx.state.temporary) {
      throw new Error('run walkthrough A from step 4: the temporary password is shown only once');
    }
    ctx.state.newUserPage = await ownPage(ctx, NEW_USER, ctx.state.temporary);
    return ctx.state.newUserPage;
  },
  'wt-a-06': async (ctx) => {
    const page = ctx.state.newUserPage;
    ctx.state.newPassword = `Seed#${Date.now().toString(36)}Aa7`;
    await page.getByLabel(/^Current password/).fill(ctx.state.temporary);
    await page.getByLabel(/^New password/).fill(ctx.state.newPassword);
    await page.getByLabel(/^Confirm new password/).fill(ctx.state.newPassword);
    await button(page, /^change password$/i).click();
    await settle(page, 1500);
    return page;
  },
  'wt-a-07': async (ctx) => {
    const page = ctx.state.newUserPage;
    await page.goto(`${ctx.BASE}/profile`);
    await settle(page);
    return page;
  },
  'wt-a-08': async (ctx) => runReport(ctx, 'auditor', 'UAM-AUDIT-LOG', [['Include Log-ins and Log-outs', true]]),

  // ---------------------------------------------------------------- walkthrough B
  'wt-b-01': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new?type=MODIFY_USER&user=${NEW_USER}`);
    await page.getByLabel(/^Marketing Team Leader/).check();
    await fill(page, 'Approver', 'Ulysses');
    await fill(page, 'Remarks \\(Justification\\)', 'Covers as team leader during the leave of the unit head (seed data)');
    await press(page, /^submit$/i);
    await settle(page, 1500);
    return page;
  },
  'wt-b-02': async (ctx) => {
    const id = requestOf(ctx, `username = '${NEW_USER}' and request_type = 'MODIFY_USER'`);
    const page = await openRequest(ctx, 'uamapprover', id);
    if (ctx.one(`select status from nba_access_request where id = ${id}`) === 'PENDING') {
      await act(page, /^approve and apply$/i, { comment: 'Approved for the leave period' });
    }
    return page;
  },
  'wt-b-03': async (ctx) => {
    const page = await openRequest(ctx, 'secapprover', seedRequest(ctx, 'AR-2026-900008'));
    if (ctx.one("select status from nba_access_request where request_no = 'AR-2026-900008'") === 'PENDING_SECOND') {
      await act(page, /^second approval$/i, { confirm: /^approve$/i, comment: 'Back-up administrator for the migration weekend' });
    }
    return page;
  },
  'wt-b-04': async (ctx) => {
    const page = await go(ctx, 'badmin', `${REQUESTS}/new?kind=group`);
    if (requestOf(ctx, `role_code = '${PROFILE}'`)) {
      return page;
    }
    await fill(page, 'Profile Code', PROFILE);
    await fill(page, 'Name', 'Renewal enquiry');
    await fill(page, 'Description', 'Read-only enquiry of clients and reports for the contact centre (seed data)');
    // Each permission shows its name; its check box is found by the permission code (id perm-CODE).
    await page.locator('#perm-CLIENT_VIEW').check();
    await page.locator('#perm-REPORT_VIEW').check();
    await fill(page, 'Approvers in Order', 'Ulysses');
    await fill(page, 'Approvers in Order', '\\(approver\\)');
    await fill(page, 'Remarks \\(Justification\\)', 'Enquiry profile for the renewal follow-up (seed data)');
    await press(page, /^submit$/i);
    await settle(page, 1500);
    await tab(page, 'Approvers');
    return page;
  },
  'wt-b-05': async (ctx) => {
    const id = requestOf(ctx, `role_code = '${PROFILE}'`);
    const page = await openRequest(ctx, 'uamapprover', id);
    if (ctx.one(`select assigned_approver from nba_access_request where id = ${id}`) === 'uamapprover') {
      await act(page, /^approve and apply$/i, { comment: 'First approval' });
    }
    return page;
  },
  'wt-b-06': async (ctx) => {
    const id = requestOf(ctx, `role_code = '${PROFILE}'`);
    const page = await openRequest(ctx, 'approver', id);
    if (ctx.one(`select status from nba_access_request where id = ${id}`) === 'PENDING') {
      await act(page, /^approve and apply$/i, { comment: 'Second approver in order' });
    }
    return page;
  },
  'wt-b-07': async (ctx) => {
    const page = await go(ctx, 'admin', '/admin/roles');
    const row = page.locator('table tbody tr').filter({ hasText: PROFILE }).first();
    if (await row.count()) {
      await row.getByRole('button', { name: /^implement request$/i }).click();
      await page.waitForTimeout(600);
      await page.locator('dialog.modal[open]').last().getByRole('button', { name: /^implement request$/i }).last().click();
      await settle(page, 1500);
    }
    return page;
  },
  'wt-b-08': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new?type=DISABLE_USER&user=${NEW_USER}`);
    if (requestOf(ctx, `username = '${NEW_USER}' and request_type = 'DISABLE_USER'`)) {
      return page;
    }
    await fill(page, 'Reason', 'Resigned');
    await fill(page, 'Approver', 'Ulysses');
    await fill(page, 'Remarks \\(Justification\\)', 'Resigned effective today (seed data)');
    await press(page, /^submit$/i);
    await settle(page, 1500);
    return page;
  },
  'wt-b-09': async (ctx) => {
    const id = requestOf(ctx, `username = '${NEW_USER}' and request_type = 'DISABLE_USER'`);
    const page = await openRequest(ctx, 'uamapprover', id);
    if (ctx.one(`select status from nba_access_request where id = ${id}`) === 'PENDING') {
      await act(page, /^approve and apply$/i, { comment: 'Clearance received' });
    }
    return page;
  },
  'wt-b-10': async (ctx) => {
    const page = await go(ctx, 'admin', '/admin/users');
    await page.getByLabel('Status').selectOption({ label: 'Disabled' });
    await settle(page, 600);
    return page;
  },

  // ---------------------------------------------------------------- walkthrough C
  'wt-c-01': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new`);
    await fill(page, 'User ID', 'ab');
    await press(page, /^submit$/i);
    await settle(page, 600);
    return page;
  },
  'wt-c-02': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new`);
    await fill(page, 'User ID', 'a01300019X');
    await fill(page, 'Full Name', 'Marco Salvador');
    await page.getByLabel('Marketing Account Officer', { exact: true }).check();
    await fill(page, 'Approver', 'Ulysses');
    await fill(page, 'Remarks \\(Justification\\)', 'New hire (seed data)');
    await press(page, /^submit$/i);
    await settle(page, 800);
    return page;
  },
  'wt-c-03': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new?type=MODIFY_USER&user=a013000101`);
    await fill(page, 'Full Name', 'SIT Enrolled User Reyes');
    await fill(page, 'Approver', 'Ulysses');
    await fill(page, 'Remarks \\(Justification\\)', 'Name corrected (seed data)');
    await press(page, /^submit$/i);
    await settle(page, 1200);
    return page;
  },
  'wt-c-04': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new?type=MODIFY_USER&user=requestor`);
    await page.getByLabel('Marketing Account Officer', { exact: true }).check();
    await fill(page, 'Approver', 'Ulysses');
    await fill(page, 'Remarks \\(Justification\\)', 'Also raises Marketing requests (seed data)');
    await press(page, /^submit$/i);
    await settle(page, 1200);
    return page;
  },
  'wt-c-12': async (ctx) => {
    const page = await go(ctx, 'requestor', `${REQUESTS}/new`);
    await fill(page, 'User ID', 'a013000197');
    await fill(page, 'Full Name', 'Paolo Mendoza');
    await page.getByLabel('User Access Requestor', { exact: true }).check();
    await page.getByLabel('User Access Approver', { exact: true }).check();
    await fill(page, 'Approver', 'Ulysses');
    await fill(page, 'Remarks \\(Justification\\)', 'Raises and approves access requests (seed data)');
    await press(page, /^submit$/i);
    await settle(page, 1200);
    return page;
  },
  'wt-c-05': async (ctx) => openRequest(ctx, 'approver', seedRequest(ctx, 'AR-2026-900002')),
  'wt-c-06': async (ctx) => {
    // A group-profile request raised by the System Administrator and approved, so that he may try to implement it.
    let id = requestOf(ctx, "created_by = 'admin' and request_type = 'MODIFY_ROLE_PERMISSIONS' and status = 'FOR_IMPLEMENTATION'");
    if (!id) {
      const added = ctx.one("select v.p from (values ('CLIENT_VIEW'), ('REPORT_VIEW'), ('ATTACHMENT_VIEW')) v(p) "
        + "where v.p not in (select rp.permission from sec_role_permission rp join sec_role r on r.id = rp.role_id "
        + "where r.code = 'CONTACT_CENTER') limit 1");
      const r = await ctx.api('admin', 'POST', '/nbadmin/access-requests', {
        type: 'MODIFY_ROLE_PERMISSIONS', roleCode: 'CONTACT_CENTER', permissionsAdded: [added], permissionsRemoved: [],
        justification: 'Enquiry right for the contact centre (seed data)', approvers: ['uamapprover'],
      });
      id = r.id;
      await ctx.api('uamapprover', 'POST', `/nbadmin/access-requests/${id}/approve`, { comment: 'Approved' });
    }
    const page = await go(ctx, 'admin', '/admin/roles');
    const row = page.locator('table tbody tr').filter({ hasText: ctx.one(`select request_no from nba_access_request where id = ${id}`) }).first();
    await row.getByRole('button', { name: /^implement request$/i }).click();
    await page.waitForTimeout(600);
    await page.locator('dialog.modal[open]').last().getByRole('button', { name: /^implement request$/i }).last().click();
    await settle(page, 1200);
    return page;
  },
  'wt-c-07': async (ctx) => {
    for (let i = 0; i < 3 && ctx.one("select locked from sec_user where username = 'a013000101'") !== 't'; i += 1) {
      await fetch(`${ctx.API}/api/v1/auth/login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Origin: ctx.BASE },
        body: JSON.stringify({ username: 'a013000101', password: `Wrong#Password${i}` }),
      });
    }
    return ownPage(ctx, 'a013000101', process.env.SEED_PASSWORD);
  },
  'wt-c-08': async (ctx) => {
    const page = await go(ctx, 'admin', '/admin/users');
    await page.getByLabel('Status').selectOption({ label: 'Locked' });
    await settle(page, 600);
    const row = page.locator('table tbody tr').filter({ hasText: 'a013000101' }).first();
    if (await row.getByRole('button', { name: /^unlock$/i }).count()) {
      await row.getByRole('button', { name: /^unlock$/i }).click();
      await settle(page, 800);
      // The unlock is confirmed in its dialog (confirmation standard).
      const dialog = page.locator('dialog[open]');
      if (await dialog.count()) {
        await dialog.getByRole('button', { name: /^unlock$/i }).last().click({ timeout: 10000 });
        await settle(page, 1200);
      }
    }
    await page.getByLabel('Status').selectOption({ label: 'All statuses' });
    await settle(page, 600);
    return page;
  },
  'wt-c-09': async (ctx) => ownPage(ctx, NEW_USER, ctx.state.newPassword || 'Unknown#Password1'),
  'wt-c-10': async (ctx) => {
    const page = await go(ctx, 'requestor', '/profile');
    await page.getByLabel(/^New password/).fill('short1');
    await page.getByLabel(/^Confirm new password/).fill('short1');
    await settle(page, 400);
    return page;
  },
  'wt-c-11': async (ctx) => {
    const page = await go(ctx, 'requestor', '/profile');
    await page.getByLabel(/^Current password/).fill(process.env.SEED_PASSWORD);
    await page.getByLabel(/^New password/).fill(process.env.SEED_PASSWORD);
    await page.getByLabel(/^Confirm new password/).fill(process.env.SEED_PASSWORD);
    await button(page, /^change password$/i).click();
    await settle(page, 1200);
    return page;
  },

  // ---------------------------------------------------------------- walkthrough D
  'wt-d-01': async (ctx) => {
    const page = await go(ctx, 'auditor', '/user-access/matrix');
    await tab(page, 'By Action');
    await page.getByLabel(/^Find an area or permission/).fill('USER ACCESS');
    await settle(page, 600);
    return page;
  },
  'wt-d-02': async (ctx) => runReport(ctx, 'auditor', 'UAM-GROUP-PROFILE', [['Group Profile \\(code\\)', 'UAM_APPROVER']]),
  'wt-d-03': async (ctx) => runReport(ctx, 'auditor', 'UAM-GROUP-MEMBERS', [['Group Profile \\(code\\)', 'MKT_AO']]),
  'wt-d-04': async (ctx) => {
    const installed = ctx.one("select to_char(min(occurred_at), 'YYYY-MM-DD') from sec_access_change_log where activity = 'DEACTIVATE_ROLE'");
    return runReport(ctx, 'auditor', 'UAM-AUDIT-LOG', [['Date From', installed], ['Activity', 'Group Profile Changes']]);
  },
  'wt-d-05': async (ctx) => go(ctx, 'badmin', `${REQUESTS}/new?kind=group`),
};

/** Nothing to prepare: the records of each step come from the seed data or from the earlier steps. */
async function prepare() {}

module.exports = { steps, prepare };
