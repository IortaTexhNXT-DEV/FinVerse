// How capture_pack.cjs reaches each screen state of the BRD-04 Collections sign-off pack on the seed profile: the
// record to open for a status (seed database), what is typed into the forms (fictitious seed values), the rows
// selected, the walkthrough steps and the generated documents. Run the walkthroughs first on a fresh seed database
// ('^wt-'), then the screens, documents and UX states ('^(scr|doc|ux)-'): the screens show the account, the plan, the
// statement and the request that the walkthroughs create.
const walkthrough = require('./brd04_walkthrough.cjs');
const { render } = require('./brd01_documents.cjs');
const { act, settle } = require('./brd01_walkthrough.cjs');
const { fill, dateText } = require('./brd02_walkthrough.cjs');

const { invoiceA, rowAction, tickRow, search, dialogOf, PAYMENT_C } = walkthrough;

// ------------------------------------------------------------------ records to open

/** The account of walkthrough A, or a seed account when the walkthroughs have not run. */
function accountA(ctx) {
  return ctx.sql("select 1 from ops_invoice where arn = 'ARN-2026-940007'").length ? invoiceA(ctx) : 'BI-HO-2026-000002';
}

/** The plan of walkthrough D. */
function planD(ctx) {
  return ctx.one(`select id from clx_installment_plan where invoice_no = '${accountA(ctx)}' and status = 'ACTIVE' order by id desc limit 1`);
}

/** A statement to send: cycle 2 of the plan of walkthrough D, generated when there is none. */
async function statementToSend(ctx) {
  const query = "select id from clx_billing_statement where status = 'GENERATED' order by id limit 1";
  if (ctx.sql(query).length === 0) {
    await ctx.api('clxhandler', 'POST', '/collections/billing/statements', { planId: Number(planD(ctx)), cycleSeq: 2 });
  }
  return ctx.one(query);
}

const opens = {
  'open_item:handled': (ctx) => `/collections/items/${accountA(ctx)}`,
  'open_item:locked': async (ctx) => {
    await ctx.api('clxhandler', 'POST', `/collections/items/${accountA(ctx)}/lock`);
    return `/collections/items/${accountA(ctx)}`;
  },
  'open_client:first': () => '/collections/clients/CL-2026-000005',
  'open_plan:walkthrough': (ctx) => `/collections/plans/${planD(ctx)}`,
  'open_plan:seed': (ctx) => `/collections/plans/${ctx.one("select id from clx_installment_plan where plan_no = 'IPL-2026-000001'")}`,
  'open_statement:first': (ctx) => `/collections/billing/${ctx.one("select id from clx_billing_statement where status = 'SENT' order by id limit 1")}`,
  'open_statement:to_send': async (ctx) => `/collections/billing/${await statementToSend(ctx)}`,
  'open_escalation:with_tl': (ctx) => `/collections/escalations/${ctx.one("select id from clx_escalation where status = 'WITH_TL' and kind = 'AUTO' order by id limit 1")}`,
  'open_escalation:second': (ctx) => `/collections/escalations/${ctx.one("select id from clx_escalation where status in ('WITH_TL', 'IN_ACTION') order by id desc limit 1")}`,
  'open_unapplied:matched': () => '/collections/unapplied/UNP-2026-000003',
  'open_unapplied:requested': () => `/collections/unapplied/${PAYMENT_C}`,
};

// ------------------------------------------------------------------ forms

/**
 * Opens the dialog of a form before its fields are filled: ticks the rows the dialog works on (a `selects` entry), then
 * presses the button that opens it. capture_pack.cjs fills before it selects or clicks, so the dialog forms open here.
 */
const opening = (button, rows) => async (page, ctx) => {
  if (rows) {
    await selects[rows](page, ctx);
    await settle(page, 300);
  }
  await ctx.clickButton(page, button);
};
/** Presses the button of the open dialog (not the button of the same name on the page behind it). */
const pressing = (button) => async (page) => {
  await dialogOf(page).getByRole('button', { name: new RegExp(button, 'i') }).last().click();
  await settle(page, 800);
};
const inDialog = (label, value) => async (page) => fill(page, label, value, dialogOf(page));
const remarks = (text) => async (page) => dialogOf(page).getByLabel(/^Remarks/).fill(text);

const fills = {
  pickup_disposition: [
    opening('^record disposition$', 'first_account'),
    inDialog('Disposition', /check pick-up/i),
    inDialog('Pick-up Date', dateText(3)),
    inDialog('Pick-up Address', '5th Floor, Harbor Point Building, Port Area, Manila'),
    inDialog('Contact Person', 'Ramon Uy, treasury'),
    inDialog('Check Amount', '28281.25'),
    remarks('Client will issue one check for the balance'),
  ],
  reassign_temporary: [
    opening('^reassign$', 'two_accounts'),
    inDialog('New Handler', /Miguel/),
    inDialog('Kind', /^Temporary$/),
    inDialog('Until', dateText(14)),
    inDialog('Reason', 'Marco Marketing Handler on leave for two weeks'),
  ],
  new_plan: [
    opening('^new installment plan$'),
    inDialog('Plan Basis', /one invoice/i),
    inDialog('Invoice No.', 'BI-HO-2026-000004'),
    inDialog('First Due Date', dateText(15)),
    inDialog('Number of Installments', '4'),
    inDialog('Billing Frequency', /^quarterly$/i),
  ],
  new_promise: [
    opening('^record promise$'),
    inDialog(/^Invoice Nos?\.?/, 'BI-HO-2026-000004'),
    inDialog('Promised Payment Date', dateText(5)),
    inDialog('Promised Amount', '10000'),
    remarks('Client promised a partial payment by check'),
  ],
  promise_over_balance: [
    opening('^record promise$'),
    inDialog(/^Invoice Nos?\.?/, 'BI-HO-2026-000004'),
    inDialog('Promised Payment Date', dateText(7)),
    inDialog('Promised Amount', '950000'),
    pressing('^record promise$'),
  ],
  escalate_manual: [
    opening('^escalate accounts$'),
    inDialog('Invoice Nos.', 'BI-HO-2026-000004'),
    inDialog('Reason', /no payment commitment/i),
    remarks('No commitment after three calls; client asks for the bank account officer'),
  ],
  new_escalation_rule: [
    opening('^new rule$'),
    inDialog('Code', 'CLX-NO-PROMISE-30'),
    inDialog('Name', 'No promise 30 days after booking - team lead'),
    inDialog('Basis', /without a promise/i),
    inDialog('Threshold', '30'),
    inDialog('Segment', 'RETAIL'),
    inDialog('Reason', /no payment commitment/i),
    inDialog('SLA (Hours)', '48'),
  ],
  apply_to_invoice: [
    opening('^request application$', 'unmatched_payment'),
    inDialog('Invoice No.', 'BI-HO-2026-000004'),
    inDialog('Amount', '1000'),
    remarks('Payor confirmed the invoice by phone'),
  ],
  apply_bad_invoice: [opening('^request application$', 'unmatched_payment'), inDialog('Invoice No.', 'INV 2026/0417'),
    pressing('^request application$')],
  reassign_criteria: [async (page) => fill(page, 'Current Handler', /Marco/)],
  new_assignment_rule: [
    opening('^new rule$'),
    inDialog('Priority', '30'),
    inDialog('Name', 'Retail branch accounts'),
    inDialog('Handler', /Miguel/),
    inDialog('Market Segment', 'RETAIL'),
  ],
};

// ------------------------------------------------------------------ rows selected, states after opening

async function tickRows(page, n) {
  const rows = page.locator('main table tbody tr');
  await rows.first().waitFor({ timeout: 15000 });
  for (let i = 0; i < n; i += 1) {
    await rows.nth(i).locator('input[type=checkbox]').first().check();
    await page.waitForTimeout(300);
  }
}

const selects = {
  first_account: (page) => tickRows(page, 1),
  two_accounts: (page) => tickRows(page, 2),
  unmatched_payment: async (page) => {
    await search(page, 'UNP-2026-000006');
    await tickRow(page, 'UNP-2026-000006');
  },
};

/**
 * A dialog taller than the window scrolls inside itself; the capture grows the window to the page, so the page
 * under the dialog is made as tall as the dialog and the whole form is in the image.
 */
async function roomForDialog(page) {
  await page.evaluate(() => {
    const dialog = document.querySelector('dialog.modal[open]');
    const main = document.querySelector('main.app-main, main');
    if (!dialog || !main) {
      return;
    }
    let hidden = 0;
    [dialog, ...dialog.querySelectorAll('*')].forEach((el) => {
      if (/auto|scroll/.test(getComputedStyle(el).overflowY)) {
        hidden = Math.max(hidden, el.scrollHeight - el.clientHeight);
      }
    });
    main.style.paddingBottom = `${hidden + 120}px`;
  });
  await settle(page, 300);
}

const after = {
  'scr-cl-13-02-new': roomForDialog,
  'ux-scr-cl-02-empty': async (page) => search(page, 'BI-HO-2099-999999'),
  'ux-scr-cl-13-actions': async (page) => {
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await settle(page, 300);
  },
  'scr-cl-06-02-actions': async (page) => {
    // The installment of cycle 2 of the plan of walkthrough D: not billed yet, so both actions are offered.
    await page.locator('main table tbody tr').nth(1).getByRole('button', { name: /^Actions for/ }).click();
    await settle(page, 300);
  },
  'scr-cl-12-02-resolve': async (page) => {
    if (await page.getByRole('button', { name: /^acknowledge$/i }).count()) {
      await act(page, 'Acknowledge', { reason: false, confirm: /^acknowledge$/i, comment: 'Called the client with the AO' });
    }
    await page.getByRole('button', { name: /^resolve$/i }).first().click();
    await settle(page, 600);
    await dialogOf(page).getByLabel(/^Resolution/).fill('Client paid the first half; the balance is on a promise to pay');
  },
  'scr-cl-19-02-change': async (page) => {
    await page.locator('main table tbody tr').filter({ hasText: 'Minimal Balance Threshold' }).first().click();
    await settle(page, 600);
  },
};

// ------------------------------------------------------------------ documents

/** A Collections report as the business receives it (Excel), the first page rendered with its first columns. */
async function report(ctx, code, out, params, columns) {
  const company = ctx.one("select id from org_company where code = 'FVI'");
  const data = await ctx.api('clxuh', 'POST', `/reports/${code}/export?format=XLSX`, { companyId: String(company), ...params });
  render(data, 'xlsx', out, 200, columns);
}

const ITEM_COLUMNS = ['Client Code', 'Name of Assured', 'Invoice No.', 'Booking Date', 'Aging Bracket', 'Outstanding Premium',
  'Collection Handler', 'Disposition', 'Status'];
const year = () => new Date().getUTCFullYear();

const documents = {
  'doc-soa': async (ctx, out) => {
    const id = ctx.one("select id from clx_billing_statement where status = 'SENT' order by id limit 1");
    render(await ctx.api('clxhandler', 'GET', `/collections/billing/statements/${id}/document`), 'pdf', out);
  },
  'doc-outstanding-pr': (ctx, out) => report(ctx, 'CLX-OUTSTANDING-PR', out, {}, ITEM_COLUMNS),
  'doc-full-production': (ctx, out) => report(ctx, 'CLX-FULL-PRODUCTION', out,
    { from: `${year()}-01-01`, to: `${year()}-12-31` }, ITEM_COLUMNS),
  'doc-dp-reversal': (ctx, out) => report(ctx, 'CLX-DP-FOR-REVERSAL', out,
    { from: `${year()}-01-01`, to: `${year()}-12-31` },
    ['Invoice No.', 'Name of Assured', 'Outstanding Premium', 'Date Tagged', 'Tagged By', 'Basic Commission', 'Tag Remarks']),
  'doc-application-file': async (ctx, out) => {
    const id = ctx.one("select id from ops_extract_file where source_module = 'COLLECTIONS' order by id desc limit 1");
    render(await ctx.api('cashier', 'GET', `/ops/extracts/${id}/file`), 'txt', out);
  },
};

// A walkthrough step or screen shot that shows one tab of a long record page: the tab strip and the tab's content.
const TAB = 'main div.stack > div.tabs[role=tablist], main div.stack > div.tabs[role=tablist] ~ *';
const crops = { 'scr-cl-03-02-dispositions': TAB, 'scr-cl-03-03-history': TAB };

module.exports = { opens, fills, selects, after, crops, custom: {}, walkthrough: walkthrough.steps, documents,
  prepare: walkthrough.prepare };
