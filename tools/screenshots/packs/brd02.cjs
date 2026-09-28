// How capture_pack.cjs reaches each screen state of the BRD-02 Operations sign-off pack on the seed profile: the
// record to open for a status (seed database), what is typed into the forms (fictitious seed values), the rows
// selected, the walkthrough steps and the generated documents. Run the walkthroughs first on a fresh seed database
// ('^wt-(a|b|d)-'), then the screens and documents ('^(scr|doc)-'), then walkthrough C ('^wt-c-'), which answers the
// DP billing the screens show awaiting the insurer.
const walkthrough = require('./brd02_walkthrough.cjs');
const { download, render } = require('./brd01_documents.cjs');
const { settle } = require('./brd01_walkthrough.cjs');

const { fill, firstOption, dateText, isoDate, invoiceA } = walkthrough;

// ------------------------------------------------------------------ records made on demand

/** A remittance batch in review: the batch waiting for approval is returned and its invoices extracted again. */
async function reviewBatch(ctx) {
  const open = "select id from rem_batch where stage = 'REVIEW_IN_PROCESS' order by id limit 1";
  if (ctx.sql(open).length === 0) {
    const id = ctx.one("select id from rem_batch where stage = 'FOR_APPROVAL' order by id limit 1");
    const invoices = ctx.sql(`select invoice_no from rem_batch_line where batch_id = ${id}`).map((r) => r[0] ?? r);
    await ctx.api('remittl', 'POST', `/remittance/batches/${id}/return`,
      { reasonCode: 'OTHERS', comment: 'Payment of the endorsement to be checked before remittance' });
    for (const no of invoices) {
      await ctx.api('remit', 'POST', '/remittance/runs', { companyId: 1, invoiceNo: String(no) });
    }
  }
  return ctx.one(open);
}

/** A special remittance request: the unpaid seed invoice is paid over the counter, then requested for immediate OR issuance. */
async function specialRequest(ctx) {
  const any = 'select id from rem_special_request order by id limit 1';
  if (ctx.sql(any).length === 0) {
    const inv = 'BI-HO-2026-000007';
    const row = ctx.sql(`select arn, gross_premium, payment_status from ops_invoice where invoice_no = '${inv}'`)[0];
    const [arn, gross, status] = Array.isArray(row) ? row : String(row).split('|');
    if (status !== 'PAID') {
      await ctx.api('cashier', 'POST', '/cashiering/payments', {
        companyId: 1, branchId: 1, references: [arn], payorName: 'Ramon C. Villanueva', currency: 'PHP',
        amount: Number(gross), paymentDate: isoDate(-7), mode: 'CASH',
      });
    }
    await ctx.api('mktcoll', 'POST', '/remittance/special', {
      companyId: 1, invoiceNo: inv, conditionCode: 'IMMEDIATE_OR',
      remarks: 'Client needs the insurer official receipt for its loan release this week',
    });
  }
  return ctx.one(any);
}

/** A BIR 2307 certificate of the insurer tagged to its commission official receipt. */
async function certificate(ctx) {
  const any = 'select id from cmr_certificate order by id limit 1';
  if (ctx.sql(any).length === 0) {
    const or = ctx.sql("select receipt_no, amount from csh_receipt where receipt_class = 'COMMISSION' and status = 'ISSUED' order by id limit 1")[0];
    const [orNo, amount] = Array.isArray(or) ? or : String(or).split('|');
    await ctx.api('commrec', 'POST', '/commission/certificates', {
      companyId: 1, insurerCode: 'INS-MGIC', form: '2307', number: 'MGIC-2307-2026-0917',
      periodFrom: '2026-07-01', periodTo: '2026-09-30', taxWithheld: Math.round(Number(amount) * 0.1 * 100) / 100,
      receipts: [{ orNo, amount: Number(amount) }],
    });
  }
  return ctx.one(any);
}

// ------------------------------------------------------------------ records to open

const opens = {
  'open_invoice:remitted': () => '/operations/invoices/BI-HO-2026-000001',
  'open_invoice:family': (ctx) => `/operations/invoices/${ctx.sql("select 1 from ops_invoice where arn = 'ARN-2026-940007'").length ? invoiceA(ctx) : 'BI-HO-2026-000001'}`,
  'open_receipt:applied': (ctx) => `/cashiering/receipts/${ctx.one("select id from csh_receipt where receipt_no = 'AR-HO-000001'")}`,
  'open_receipt:cancel_requested': (ctx) => `/cashiering/receipts/${ctx.one("select receipt_id from csh_receipt_action where stage = 'FOR_APPROVAL' order by id limit 1")}`,
  'open_unapplied:no_match': (ctx) => `/cashiering/unapplied/${ctx.one("select id from csh_unapplied where stage = 'UNAPPLIED' and invoice_no is null order by balance desc, id limit 1")}`,
  'open_unapplied:for_approval': (ctx) => `/cashiering/unapplied/${ctx.one("select id from csh_unapplied where stage = 'FOR_APPROVAL' order by id limit 1")}`,
  'open_batch:review': async (ctx) => `/remittance/batches/${await reviewBatch(ctx)}`,
  'open_batch:or_received': (ctx) => `/remittance/batches/${ctx.one("select id from rem_batch where stage = 'OR_RECEIVED' order by id limit 1")}`,
  'open_hold:for_approval': (ctx) => `/remittance/holds/${ctx.one("select id from rem_hold_request where stage = 'FOR_APPROVAL' order by id limit 1")}`,
  'open_special:first': async (ctx) => `/remittance/special/${await specialRequest(ctx)}`,
  'open_deduction:first': (ctx) => `/remittance/deductions/${ctx.one('select id from rem_deduction order by id limit 1')}`,
  'open_request:for_approval': (ctx) => `/adjustment/requests/${ctx.one("select id from adj_request where stage = 'FOR_APPROVAL' order by id limit 1")}`,
  'open_request:posted_cancellation': (ctx) => `/adjustment/requests/${ctx.one("select id from adj_request where stage = 'POSTED' order by id desc limit 1")}`,
  'open_cycle:reconciling': (ctx) => `/prodrecon/cycles/${ctx.one("select id from prc_cycle where stage = 'RECONCILING' order by id limit 1")}`,
  'open_billing:awaiting': (ctx) => `/commission/dp/billings/${ctx.one('select id from cmr_billing order by id limit 1')}`,
  'open_certificate:first': async (ctx) => `/commission/certificates/${await certificate(ctx)}`,
  'open_report:remittance_tracker': () => '/reports/REM-TRACKER',
  first_request_dv: () => '/operations/disbursements',
};

// ------------------------------------------------------------------ forms

function click(name) {
  return async (page) => {
    await page.getByRole('button', { name: new RegExp(name, 'i') }).first().click();
    await settle(page, 900);
  };
}

const dialog = (page) => page.locator('dialog.modal[open]').last();

/** New Endorsement Request: the policy of a paid, unlocked seed invoice chosen. */
async function choosePolicy(page) {
  await page.getByPlaceholder('Search policy, ARN, invoice or client').fill('BI-HO-2026-000001');
  await page.getByRole('button', { name: /^search$/i }).click();
  await settle(page, 1200);
  await page.getByLabel('Select BI-HO-2026-000001').check();
  await settle(page, 1200);
}

async function flatCancellation(page, ctx) {
  await choosePolicy(page);
  await click('^next$')(page);
  const inception = ctx.one("select to_char(inception_date, 'DD-Mon-YYYY') from ops_invoice where invoice_no = 'BI-HO-2026-000001'");
  await fill(page, 'Endorsement Type', /Change of Cover/);
  await fill(page, 'Request Type', /^Flat Cancellation$/);
  await firstOption(page.getByLabel(/^Reason for Cancellation/).first());
  await fill(page, 'Effective Date', inception);
  await fill(page, 'Description', 'Flat cancellation from inception: the vehicle was sold before the policy took effect');
}

const fills = {
  receive_payment: [
    ['ARN, Invoice, Policy or PN No.', 'ARN-2026-940001'],
    ['Payor Name', 'Maria Clara R. Santos'],
    ['Assured Name', 'Santos, Maria Clara Reyes'],
    ['Amount', '5000'],
    async (page) => { await fill(page, 'Payment Date', dateText(0)); await settle(page, 2000); },
  ],
  issue_or: [
    click('^issue official receipt$'),
    async (page) => { await firstOption(dialog(page).getByLabel(/^OR Type/).first()); },
    async (page) => fill(page, 'Payor Name', 'Mabuhay General Insurance Corp.', dialog(page)),
    async (page) => fill(page, 'Gross Amount', '12500', dialog(page)),
    async (page) => fill(page, 'Description', 'Service fee for the September 2026 renewal review', dialog(page)),
  ],
  disposition_refund: [
    async (page) => fill(page, 'Disposition Type', /Refund/),
    async (page) => fill(page, 'Refund Payee', 'Carmela R. Bautista'),
    async (page) => fill(page, 'Remarks', 'Client paid twice; refund of the second payment'),
  ],
  pdc_check: [
    click('^warehouse check$'),
    async (page) => fill(page, 'Payor Name', 'Pacific Harbor Logistics Inc.', dialog(page)),
    async (page) => fill(page, 'ARN, Invoice, Policy or PN No.', 'ARN-2026-940002', dialog(page)),
    async (page) => fill(page, 'Check No.', '0004127', dialog(page)),
    async (page) => fill(page, 'Bank', 'BDO Unibank', dialog(page)),
    async (page) => fill(page, 'Bank Branch', 'Ortigas Center', dialog(page)),
    async (page) => fill(page, 'Maturity Date', dateText(30), dialog(page)),
    async (page) => fill(page, 'Amount', '22268.75', dialog(page)),
  ],
  cwt_tag: [
    click('^tag 2307$'),
    async (page) => fill(page, 'Invoice No.', 'BI-HO-2026-000001', dialog(page)),
    async (page) => { await firstOption(dialog(page).getByLabel(/^Path/).first()); },
    async (page) => fill(page, 'Amount', '284.52', dialog(page)),
    async (page) => fill(page, 'Certificate No.', 'CWT-2026-0412', dialog(page)),
    async (page) => fill(page, 'Period From', dateText(-60), dialog(page)),
    async (page) => fill(page, 'Period To', dateText(-1), dialog(page)),
  ],
  new_series: [
    click('^new series$'),
    async (page) => { await firstOption(dialog(page).getByLabel(/^Kind/).first()); },
    async (page) => { await firstOption(dialog(page).getByLabel(/^Branch/).first()); },
    async (page) => fill(page, 'Prefix', 'AR-HO', dialog(page)),
    async (page) => fill(page, 'From No.', '200001', dialog(page)),
    async (page) => fill(page, 'To No.', '205000', dialog(page)),
    async (page) => fill(page, 'BIR ATP No.', 'OCN-4AU0001234567', dialog(page)),
    async (page) => fill(page, 'Warn When Remaining At', '250', dialog(page)),
  ],
  hold_request: [
    click('^new hold request$'),
    async (page) => fill(page, 'Invoice No.', 'I97000011', dialog(page)),
    async (page) => { await firstOption(dialog(page).getByLabel(/^Reason/).first()); },
    async (page) => fill(page, 'Hold Until', dateText(14), dialog(page)),
    async (page) => fill(page, 'Remarks', 'Client disputes the premium; hold until the account officer confirms', dialog(page)),
  ],
  incentive_rule: [
    click('^new rule$'),
    async (page) => fill(page, 'Insurer Code', 'INS-MGIC', dialog(page)),
    async (page) => fill(page, 'Rate (% of basic premium)', '2.5', dialog(page)),
    async (page) => fill(page, 'Window (days)', '30', dialog(page)),
    async (page) => fill(page, 'Effective From', dateText(3), dialog(page)),
    async (page) => fill(page, 'Description', 'Early remittance incentive of Mabuhay General, 2027 agreement', dialog(page)),
  ],
  flat_cancellation: [flatCancellation],
  flat_cancellation_recompute: [flatCancellation, click('^recompute$'), async (page) => settle(page, 2000)],
  request_incomplete: [choosePolicy, click('^next$'), click('^recompute$|^next$')],
};

// ------------------------------------------------------------------ rows selected, states after opening

async function tickFirst(page) {
  const row = page.locator('main table tbody tr').first();
  await row.waitFor({ timeout: 15000 });
  await row.locator('input[type=checkbox]').first().check();
  await page.waitForTimeout(400);
}

const selects = {
  first_request: tickFirst,
  first_policy: choosePolicy,
};

const after = {
  'scr-op-03-02-transactions': async (page) => {
    const toggles = page.locator('main button[aria-expanded="false"]');
    if ((await toggles.count()) > 0) {
      await toggles.last().click();
      await settle(page, 800);
    }
  },
  'scr-op-45-02-review': async (page) => {
    await page.getByRole('tab', { name: /^With Discrepancy/ }).first().click();
    await settle(page, 800);
    await page.getByRole('table', { name: 'Reconciliation items' }).locator('tbody tr').first().click();
    await settle(page, 800);
  },
  'scr-op-63-01-list': async (page) => {
    const heading = page.getByText(/^Operations$/).first();
    if ((await heading.count()) > 0) {
      await heading.scrollIntoViewIfNeeded();
    }
  },
  'scr-op-36-02-dv': async (page) => {
    const assign = page.getByRole('button', { name: /^assign dv$/i }).first();
    await assign.click();
    await settle(page, 600);
  },
};

// ------------------------------------------------------------------ documents

const receiptOf = (ctx, no) => ctx.one(`select id from csh_receipt where receipt_no = '${no}'`);
const batchOr = (ctx) => ctx.one("select id from rem_batch where stage = 'OR_RECEIVED' order by id limit 1");
const posted = (ctx) => ctx.one("select id from adj_request where stage = 'POSTED' order by id desc limit 1");

const documents = {
  'doc-ar': (ctx, out) => download(ctx, 'cashier', `/cashiering/receipts/${receiptOf(ctx, 'AR-HO-000001')}/pdf`, out),
  'doc-or': (ctx, out) => download(ctx, 'cashier', `/cashiering/receipts/${receiptOf(ctx, 'OR-HO-100002')}/pdf`, out),
  'doc-remittance-schedule': (ctx, out) => download(ctx, 'remittl', `/remittance/batches/${batchOr(ctx)}/documents/SCHEDULE_PDF`, out),
  'doc-payment-request': (ctx, out) => download(ctx, 'remittl', `/remittance/batches/${batchOr(ctx)}/documents/PAYMENT_REQUEST_PDF`, out),
  'doc-endorsement-slip': (ctx, out) => download(ctx, 'adjust', `/adjustment/requests/${posted(ctx)}/endorsement-slip`, out),
  'doc-validation-slip': (ctx, out) => download(ctx, 'adjust', `/adjustment/requests/${posted(ctx)}/validation-slip`, out),
  'doc-production-register': async (ctx, out) => render(await ctx.api('recon', 'GET', `/prodrecon/extracts/${ctx.one('select id from prc_extract order by id limit 1')}/file`),
    'xlsx', out, 200, ['Month of Production', 'Invoice Number', 'Booking Date', 'Policy No.', 'Assured Name', 'Gross Premium', 'Amount Paid', 'Remittance Status']),
  'doc-dp-billing': async (ctx, out) => render(await ctx.api('commrec', 'GET', `/commission/dp/billings/${ctx.one('select id from cmr_billing order by id limit 1')}/file`),
    'xlsx', out, 200, ['Billing No.', 'Invoice No.', 'Policy No.', 'Premium', 'Commission', 'VAT', 'Withholding Tax', 'Net Commission', 'Decision', 'Reason', 'Assured Name']),
};

module.exports = { opens, fills, selects, after, crops: {}, custom: {}, walkthrough: walkthrough.steps, documents,
  prepare: walkthrough.prepare };
