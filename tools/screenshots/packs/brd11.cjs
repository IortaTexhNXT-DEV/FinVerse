// How capture_pack.cjs reaches each screen state of the BRD-11 User Access Maintenance sign-off pack on the seed
// profile: the record to open for a status (seed V1960: AR-2026-900001 to 900008, BLK-2026-900001), what is typed
// into the forms (fictitious seed values), the states reached through several actions (a locked account, the forced
// password change, the inactivity warning, the temporary password of a new user, a bulk file), the walkthrough steps
// and the generated documents (the report exports and the matrix export). The SIT/UAT password is read from the
// environment (SEED_PASSWORD), never from a file.
const walkthrough = require('./brd11_walkthrough.cjs');
const { render, download } = require('./brd01_documents.cjs');
const { csv } = require('./brd01_walkthrough.cjs');

const REQUESTS = '/user-access/requests';

// ------------------------------------------------------------------ helpers

function click(name) {
  return async (page) => {
    await page.getByRole('button', { name: new RegExp(name, 'i') }).first().click();
    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(700);
  };
}

const requestId = (ctx, no) => ctx.one(`select id from nba_access_request where request_no = '${no}'`);
const today = () => new Date(Date.now() + 8 * 3600 * 1000).toISOString().slice(0, 10); // Philippine date

/** Sign-in attempts through the API (no browser), for example to lock a seed user before its screenshot. */
async function apiLogin(ctx, username, password) {
  return fetch(`${ctx.API}/api/v1/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', Origin: ctx.BASE },
    body: JSON.stringify({ username, password }),
  });
}

/** A browser page of its own, signed in as `user` with `password` (users that are not seed personas). */
async function freshPage(ctx, user, password, clock = false) {
  const context = await ctx.newContext();
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  if (clock) {
    await page.clock.install();
  }
  await page.goto(`${ctx.BASE}/login`);
  await ctx.settle(page, 300);
  if (user) {
    await page.getByLabel('User ID').fill(user);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
    await ctx.settle(page, 1200);
  }
  return page;
}

/** An enrolment request raised and submitted by the Requestor through the API, pending for uamapprover. */
async function pendingEnrolment(ctx, username, fullName) {
  const found = ctx.sql(`select id from nba_access_request where username = '${username}' and status = 'PENDING'`)[0];
  if (found) {
    return found[0];
  }
  const r = await ctx.api('requestor', 'POST', '/nbadmin/access-requests', {
    type: 'CREATE_USER', userType: 'INTERNAL', username, fullName, email: `${username}@brokerverse-seed.ph`,
    roleCodes: ['MKT_AO'], justification: 'Joined Marketing (seed data)', approvers: ['uamapprover'],
  });
  return r.id;
}

// ------------------------------------------------------------------ forms

const forgot = async (page) => {
  await page.getByRole('button', { name: /^forgot password\?$/i }).click();
  await page.waitForTimeout(400);
};

const fills = {
  login_wrong: [['User ID', 'a013000102'], ['Password', 'Wrong#Password1']],
  // a013000102 is locked by three wrong sign-ins through the API, then signs in with the right password.
  login_locked: async (ctx) => {
    const locked = ctx.one("select locked from sec_user where username = 'a013000102'");
    for (let i = 0; locked !== 't' && i < 3; i += 1) {
      await apiLogin(ctx, 'a013000102', `Wrong#Password${i}`);
    }
    return [['User ID', 'a013000102'], ['Password', process.env.SEED_PASSWORD]];
  },
  forgot_user: [forgot, ['User ID', 'requestor']],
  enrol_user: [
    ['User ID', 'a013000197'], ['Full Name', 'Andrea Mercado'], ['E-mail', 'andrea.mercado@brokerverse-seed.ph'],
    ['Windows ID', 'AMERCADO'], ['Home Branch', 'HO'], ['Marketing Account Officer', true],
    ['Approver', 'Ulysses'], ['Remarks (Justification)', 'Joined Combank Marketing as account officer (seed data)'],
  ],
  enrol_invalid: [['User ID', 'ab']],
  modify_user: [['Request Type', 'Modify user'], ['User ID', 'a013000101'], ['Full Name', 'SIT Enrolled User Santos']],
  new_profile: [
    ['Profile Code', 'UAT_ENQUIRY'], ['Name', 'Client enquiry (seed data)'],
    ['Description', 'Read-only enquiry of clients and reports'],
    // Each permission shows its name; its check box is found by the permission code (id perm-CODE).
    async (page) => page.locator('#perm-CLIENT_VIEW').check(),
    async (page) => page.locator('#perm-REPORT_VIEW').check(),
    ['Approvers in Order', 'Ulysses'], ['Approvers in Order', '\\(approver\\)'],
    ['Remarks (Justification)', 'Enquiry profile for the contact centre (seed data)'],
  ],
  modify_profile: [['Request Type', 'Modify group profile'], ['Group Profile', '^Marketing Account Officer'],
    async (page) => page.locator('#perm-UAM_VIEW').check()],
  deactivate_profile: [['Request Type', 'Deactivate group profile'], ['Group Profile', '^Processing Team Lead']],
  report_profile: [['Group Profile (code)', 'UAM_APPROVER']],
};

// ------------------------------------------------------------------ records by status

const opens = {
  pending_request: (ctx) => `${REQUESTS}/${requestId(ctx, 'AR-2026-900002')}`,
  second_request: (ctx) => `${REQUESTS}/${requestId(ctx, 'AR-2026-900008')}`,
  returned_request: (ctx) => `${REQUESTS}/${requestId(ctx, 'AR-2026-900003')}`,
  seed_batch: (ctx) => `/user-access/bulk/${ctx.one("select id from nba_access_request_batch where batch_no = 'BLK-2026-900001'")}`,
  // A reset link requested for requestor; the token is read from the e-mail queued for it.
  reset_link: async (ctx) => {
    const before = Number(ctx.one("select coalesce(max(id), 0) from msg_outbound where purpose = 'PASSWORD_RESET'"));
    await fetch(`${ctx.API}/api/v1/auth/password-reset/request`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Origin: ctx.BASE },
      body: JSON.stringify({ userId: 'requestor' }),
    });
    // The e-mail is queued after the answer: wait for it (the last one is used when a new one is not sent). The
    // token is cut out in the query, as psql prints one line per row.
    const tokenOf = (where) => ctx.sql(`select substring(body from 'token=([A-Za-z0-9_-]+)') from msg_outbound where purpose = 'PASSWORD_RESET' ${where} order by id desc limit 1`)[0]?.[0];
    let found;
    for (let i = 0; i < 20 && !found; i += 1) {
      await new Promise((r) => setTimeout(r, 750));
      found = tokenOf(`and id > ${before}`);
    }
    const token = found ? [null, found] : (tokenOf('') ? [null, tokenOf('')] : null);
    if (!token) {
      throw new Error('no reset link in the last PASSWORD_RESET e-mail');
    }
    return `/reset-password?token=${token[1]}`;
  },
};

// ------------------------------------------------------------------ states reached through several actions

const BULK_HEADERS = ['Action', 'User ID', 'Full Name', 'E-mail', 'Windows ID', 'Home Branch Code', 'Business Unit',
  'User Level', 'Group Profiles', 'Effective Date', 'Remarks'];

const custom = {
  // The forced change: the System Administrator sets a password for a013000104, who then signs in.
  'scr-ua-03-01-reset': async (ctx) => {
    const set = `Tmp#${Date.now().toString(36)}Aa9`;
    const id = ctx.one("select id from sec_user where username = 'a013000104'");
    await ctx.api('admin', 'POST', `/admin/users/${id}/reset-password`, { newPassword: set });
    return freshPage(ctx, 'a013000104', set);
  },
  // The inactivity warning: the browser clock is moved on 15 minutes after sign-in.
  'scr-ua-04-01-idle': async (ctx) => {
    const page = await freshPage(ctx, 'requestor', process.env.SEED_PASSWORD, true);
    await page.clock.fastForward('15:05');
    await page.waitForTimeout(1200);
    return page;
  },
  // The temporary password of a new user, shown to the approver once after the approval.
  'scr-ua-08-05-password': async (ctx) => {
    const id = await pendingEnrolment(ctx, 'a013000198', 'Paolo Villanueva');
    const page = await ctx.pageOf('uamapprover');
    await page.goto(`${ctx.BASE}${REQUESTS}/${id}`);
    await ctx.settle(page);
    await page.getByRole('button', { name: /^approve and apply$/i }).first().click();
    await page.waitForTimeout(600);
    const dialog = page.locator('dialog.modal[open]').last();
    await dialog.locator('textarea').first().fill('Welcome to BIBS');
    await dialog.getByRole('button', { name: /^approve and apply$/i }).last().click();
    await ctx.settle(page, 1500);
    return page;
  },
  // A bulk file of three rows, one with an action that does not exist, after Upload and Validate.
  'scr-ua-09-02-validated': async (ctx) => {
    const n = Number(ctx.one('select count(*) from bulk_job')) + 1;
    const file = csv(`uam-bulk-${n}.csv`, [BULK_HEADERS,
      ['ENROL', `a01300030${n % 10}`, 'Rowena Castillo', 'rowena.castillo@brokerverse-seed.ph', '', 'HO', '', '', 'MKT_AO', '', 'Joined Marketing (seed data)'],
      ['ENROL', `a01300031${n % 10}`, 'Dennis Aquino', 'dennis.aquino@brokerverse-seed.ph', '', 'HO', '', '', 'MKT_AO', '', 'Joined Marketing (seed data)'],
      ['PROMOTE', 'a013000101', '', '', '', '', '', '', '', '', 'Not an action of the template'],
    ]);
    const page = await ctx.pageOf('requestor');
    await page.goto(`${ctx.BASE}/user-access/bulk`);
    await ctx.settle(page);
    await page.locator('main input[type=file]').first().setInputFiles(file);
    await page.waitForTimeout(600);
    await page.getByRole('button', { name: /^upload and validate$/i }).first().click();
    await ctx.settle(page, 2500);
    return page;
  },
  // A change of a security setting asked by the System Administrator, as the Information Security Officer sees it.
  'scr-ua-18-02-approve': async (ctx) => {
    const pending = ctx.sql("select 1 from sys_parameter where param_key = 'PASSWORD_MIN_AGE_DAYS' and pending_value is not null");
    if (pending.length === 0) {
      await ctx.api('admin', 'PUT', '/system/parameters/PASSWORD_MIN_AGE_DAYS', { value: '2' });
    }
    const page = await ctx.pageOf('infosec');
    await page.goto(`${ctx.BASE}/admin/parameters`);
    await ctx.settle(page);
    await page.locator('table tbody tr').filter({ hasText: 'PASSWORD_MIN_AGE_DAYS' }).first().click();
    await ctx.settle(page, 800);
    return page;
  },
  // The sessions of the Requestor, opened from the Users screen.
  'scr-ua-15-02-sessions': async (ctx) => {
    const page = await ctx.pageOf('admin');
    await page.goto(`${ctx.BASE}/admin/users`);
    await ctx.settle(page);
    const row = page.locator('table tbody tr').filter({ hasText: 'requestor' }).first();
    // Sessions is in the row action menu of the user.
    const menu = row.getByRole('button', { name: /^Actions for/ });
    await menu.evaluate((el) => el.scrollIntoView({ block: 'center', inline: 'nearest' }));
    await menu.click();
    await page.getByRole('menuitem', { name: /^sessions$/i }).click();
    await ctx.settle(page, 800);
    return page;
  },
};

// ------------------------------------------------------------------ generated documents

async function exportReport(ctx, code, params, out) {
  const data = await ctx.api('auditor', 'POST', `/reports/${code}/export?format=PDF`, params);
  if (!Buffer.isBuffer(data)) {
    throw new Error(`${code} did not return a file`);
  }
  render(data, 'pdf', out);
}

const monthStart = () => `${today().slice(0, 8)}01`;

/** The group profiles of user access maintenance shown in the extract of the User Access Matrix (DO-06). */
const MATRIX_PROFILES = ['UAM_REQUESTOR', 'UAM_APPROVER', 'UAM_SECOND_APPROVER', 'INFOSEC_OFFICER', 'SYSADMIN', 'AUDITOR'];

const documents = {
  'doc-user-access': (ctx, out) => exportReport(ctx, 'UAM-USER-ACCESS', { asOf: today(), status: 'ALL' }, out),
  'doc-group-profile': (ctx, out) => exportReport(ctx, 'UAM-GROUP-PROFILE', { groupProfile: 'UAM_APPROVER', active: 'ALL' }, out),
  'doc-group-members': (ctx, out) => exportReport(ctx, 'UAM-GROUP-MEMBERS', { asOf: today(), groupProfile: 'MKT_AO' }, out),
  'doc-audit-log': (ctx, out) => exportReport(ctx, 'UAM-AUDIT-LOG', { from: monthStart(), to: today(), activity: 'ALL', includeSignIns: 'true' }, out),
  'doc-requests': (ctx, out) => exportReport(ctx, 'UAM-REQUESTS', { from: monthStart(), to: today(), status: 'ALL', type: 'ALL' }, out),
  // An extract of the matrix (it holds every permission against every group profile): the profiles of user access
  // maintenance and the first permissions of the User Access area, under the header of the workbook.
  'doc-access-matrix': (ctx, out) => download(ctx, 'auditor', '/nbadmin/access-matrix/export', out, 'xlsx',
    ['Permission', 'Area', 'Action class', ...MATRIX_PROFILES], { rows: { column: 'Area', equals: 'USER_ACCESS', max: 14 } }),
};

// Shots kept as the whole window (the sign-in pages have no menu and are always whole; the home page shows the
// menu of the persona); every other shot is cropped to its dialog or content area (capture_pack.cjs, cropOf).
// The password steps of walkthrough C show the Change password card (below the second factor card).
const PASSWORD_CARD = 'section.card:has(> header h2:text-is("Change password"))';
const crops = { 'scr-ua-05-01-view': 'full', 'wt-c-10': PASSWORD_CARD, 'wt-c-11': PASSWORD_CARD };

// UX deck: a search of the access requests that finds nothing (the empty state of the list).
const after = {
  'ux-scr-ua-06-empty': async (page) => {
    await page.getByPlaceholder(/^search request no/i).fill('AR-2099-999999');
    await page.getByRole('button', { name: /^search$/i }).first().click();
    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(800);
  },
};

module.exports = {
  crops, opens, fills, selects: {}, uploads: {}, after, custom, walkthrough: walkthrough.steps, documents,
  prepare: walkthrough.prepare, render,
};
