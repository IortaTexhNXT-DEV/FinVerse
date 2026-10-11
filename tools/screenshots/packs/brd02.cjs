// How capture_pack.cjs reaches each screen state of the BRD-02 Operations sign-off pack on the seed profile: the
// record to open for a status (seed database), what is typed into the forms (fictitious seed values), the rows
// selected, the walkthrough steps and the generated documents. Run the walkthroughs first on a fresh seed database
// ('^wt-(a|b|d)-'), then the screens and documents ('^(scr|doc)-'), then walkthrough C ('^wt-c-'), which answers the
// DP billing the screens show awaiting the insurer.
const walkthrough = require('./brd02_walkthrough.cjs');
const { download, render } = require('./brd01_documents.cjs');
const { settle } = require('./brd01_walkthrough.cjs');

const { fill, firstOption, dateText, isoDate, invoiceA, postingStep } = walkthrough;

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
    ['ARN, Invoice, Policy or PN No.', 'ARN-2026-940004'],
    ['Payor Name', 'Pacific Harbor Logistics Inc.'],
    ['Assured Name', 'Pacific Harbor Logistics Inc.'],
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
    async (page) => fill(page, 'Kind', /^AR/, dialog(page)),
    async (page) => fill(page, 'Branch', /Davao/, dialog(page)),
    async (page) => fill(page, 'Prefix', 'AR-DVO', dialog(page)),
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
    async (page) => fill(page, 'Insurer', /Mabuhay/, dialog(page)),
    async (page) => fill(page, /^Rate \(% of basic premium\)/, '2.5', dialog(page)),
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
  // UX deck: a search that finds no invoice (the empty state of the list) and the row action menu of a payment request.
  'ux-scr-op-02-empty': async (page) => {
    await page.getByPlaceholder(/^search/i).first().fill('BI-HO-2099-999999');
    await page.getByRole('button', { name: /^search$/i }).first().click();
    await page.waitForLoadState('networkidle').catch(() => {});
    await settle(page, 800);
  },
  'ux-scr-op-36-actions': async (page) => {
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await settle(page, 300);
  },
  'scr-op-03-02-transactions': async (page) => {
    const toggles = page.locator('main section.card', { hasText: 'Policy Transactions' }).locator('button[aria-expanded="false"]');
    if ((await toggles.count()) > 0) {
      await toggles.first().click();
      await settle(page, 800);
    }
    await page.evaluate(() => document.querySelectorAll('main *').forEach((e) => { if (e.scrollLeft) e.scrollLeft = 0; }));
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
  'scr-op-52-02-account': async (page) => {
    await page.locator('main table tbody tr td').nth(1).click();
    await settle(page, 800);
  },
  'scr-op-36-02-dv': async (page) => {
    // Row actions sit in the row's action menu: open the menu, then choose the action.
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await settle(page, 300);
    await page.getByRole('menuitem', { name: /^assign dv/i }).first().click();
    await settle(page, 600);
  },
};

// ------------------------------------------------------------------ documents

const receiptOf = (ctx, no) => ctx.one(`select id from csh_receipt where receipt_no = '${no}'`);
const batchOr = (ctx) => ctx.one("select id from rem_batch where stage = 'OR_RECEIVED' order by id limit 1");
const posted = (ctx) => ctx.one("select id from adj_request where stage = 'POSTED' order by id desc limit 1");
// An internal adjustment has no endorsement slip: the latest posted adjustment that has one.
const postedWithSlip = (ctx) => ctx.one("select id from adj_request where stage = 'POSTED' and request_class <> 'INTERNAL' order by id desc limit 1");

const documents = {
  'doc-ar': (ctx, out) => download(ctx, 'cashier', `/cashiering/receipts/${receiptOf(ctx, 'AR-HO-000001')}/pdf`, out),
  'doc-or': (ctx, out) => download(ctx, 'cashier', `/cashiering/receipts/${receiptOf(ctx, 'OR-HO-100001')}/pdf`, out),
  'doc-remittance-schedule': (ctx, out) => download(ctx, 'remittl', `/remittance/batches/${batchOr(ctx)}/documents/SCHEDULE_PDF`, out),
  'doc-payment-request': (ctx, out) => download(ctx, 'remittl', `/remittance/batches/${batchOr(ctx)}/documents/PAYMENT_REQUEST_PDF`, out),
  'doc-endorsement-slip': (ctx, out) => download(ctx, 'adjust', `/adjustment/requests/${postedWithSlip(ctx)}/endorsement-slip`, out),
  'doc-validation-slip': (ctx, out) => download(ctx, 'adjust', `/adjustment/requests/${posted(ctx)}/validation-slip`, out),
  'doc-production-register': async (ctx, out) => render(await ctx.api('recon', 'GET', `/prodrecon/extracts/${ctx.one('select id from prc_extract order by id limit 1')}/file`),
    'xlsx', out, 200, ['Invoice Number', 'Policy No.', 'Gross Premium', 'Amount Paid', 'Remittance Status', 'Assured Name']),
  'doc-dp-billing': async (ctx, out) => render(await ctx.api('commrec', 'GET', `/commission/dp/billings/${ctx.one('select id from cmr_billing order by id limit 1')}/file`),
    'xlsx', out, 200, ['Invoice No.', 'Premium', 'Net Commission', 'Decision', 'Reason', 'Assured Name']),
};

// Walkthrough steps that show one tab of a record: the tab strip and the tab's content, below the record header.
const TAB = 'main div.stack > div.tabs[role=tablist], main div.stack > div.tabs[role=tablist] ~ *';
const crops = { 'wt-a-10': TAB, 'wt-b-07': TAB, 'wt-c-03': TAB, 'wt-c-05': TAB };
// Screen shots of one tab of a long record page: the tab strip and the tab's content.
['scr-op-03-02-transactions', 'scr-op-03-03-remittances', 'scr-op-03-04-movements', 'scr-op-26-04-documents',
  'scr-op-39-02-policy', 'scr-op-39-03-accounting', 'scr-op-39-04-transactions', 'scr-op-45-01-items',
  'scr-op-45-03-incentive'].forEach((slug) => { crops[slug] = TAB; });

// Lists wider than their card at 1440 pixels (invoice search, reconciliation cycles, the accounts of a batch beside
// its exclusions): taken in a wider window so that every column is in the image.
const widths = {};
['scr-op-02-01-list', 'scr-op-02-02-filters', 'wt-a-02', 'ux-scr-op-02-empty', 'scr-op-44-01-list',
  'scr-op-26-01-review', 'scr-op-26-03-closed'].forEach((slug) => { widths[slug] = 1760; });
['scr-op-25-01-list', 'scr-op-25-02-approval', 'scr-op-45-01-items'].forEach((slug) => {
  widths[slug] = 1600;
});

// ------------------------------------------------------------------ BDOI's AR / OR records (posting step)

const company = (ctx) => Number(ctx.one("select id from org_company where code = 'FVI'"));

/** A date some days before an ISO date (a check past its holding period). */
const daysBefore = (iso, n) => {
  const d = new Date(`${iso}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - n);
  return d.toISOString().slice(0, 10);
};

/**
 * The records of the posting lists, made once per run through the screens' own services: an AR creation record of
 * a premium payment for an account with an outstanding balance and an OR creation record of a service fee, both
 * submitted for posting, an AR record left Created, and a cancellation record of an issued AR.
 */
async function records(ctx) {
  if (ctx.state.records) {
    return ctx.state.records;
  }
  const companyId = company(ctx);
  const s = await ctx.api('cashier', 'GET', `/cashiering/records/settings?companyId=${companyId}`);
  const bank = (s.bankAccounts.find((b) => b.currency === 'PHP' && b.defaultForCurrency) || s.bankAccounts[0]).code;
  const head = s.arBranches.find((b) => /head office/i.test(b.name)) || s.arBranches[0];
  const rows = await ctx.api('cashier', 'GET', `/cashiering/records/accounts?companyId=${companyId}&q=ARN-2026-94`);
  const acc = rows.find((r) => Number(r.outstanding) > 1000 && !r.prebooked) || rows[0];
  const amount = Math.round(Number(acc.outstanding) * 100) / 100;
  const ar = await ctx.api('cashier', 'POST', '/cashiering/records', {
    companyId, receiptKind: 'AR', receiptType: 'PREMIUM', branchId: head.id, entryType: 'CLIENT',
    clientCode: acc.clientCode, clientName: acc.assuredName, payorName: acc.assuredName, tenderType: 'CASH',
    currency: 'PHP', bankAccount: bank, amount, remarks: 'Premium paid at the Head Office counter',
    accounts: [{ reference: acc.invoiceNo || acc.arn, amount }],
  });
  await ctx.api('cashier', 'POST', `/cashiering/records/${ar.id}/submit`);
  const orType = ctx.one("select code from lov_value where type_code = 'OR_TYPE' order by sort_order, id limit 1");
  const insurer = ctx.one("select party_code from cat_insurer where party_code = 'INS-MGIC' limit 1");
  const or = await ctx.api('cashier', 'POST', '/cashiering/records', {
    companyId, receiptKind: 'OR', receiptType: orType, branchId: (s.orBranches[0] || head).id, entryType: 'INSURER',
    insurerCode: insurer, insurerName: 'Mabuhay General Insurance Corp.', payorName: 'Mabuhay General Insurance Corp.',
    tenderType: 'CHECK', currency: 'PHP', bankAccount: bank, amount: 11200, vat: 1200, wtax: 0,
    checkNo: '0045871', checkDate: daysBefore(s.today, 14), checkBank: 'BDO Unibank', remarks: 'Risk management fee of September',
    accounts: [],
  });
  await ctx.api('cashier', 'POST', `/cashiering/records/${or.id}/submit`);
  const draft = await ctx.api('cashier', 'POST', '/cashiering/records', {
    companyId, receiptKind: 'AR', receiptType: 'PREMIUM', branchId: head.id, entryType: 'OTHER',
    payorName: 'Walk-in Payor', tenderType: 'CASH', currency: 'PHP', bankAccount: bank, amount: 3500,
    remarks: 'Payment without an account reference', accounts: [],
  });
  const reason = ctx.one("select code from lov_value where type_code = 'RECEIPT_CANCEL_REASON' order by sort_order, id limit 1");
  const receiptId = Number(ctx.one("select id from csh_receipt where receipt_no = 'AR-HO-000010'"));
  // The cancellation record of AR-HO-000010, made once (a receipt carries one open cancellation).
  let cancelId = ctx.sql(`select id from csh_receipt_record where record_kind = 'CANCELLATION' and receipt_id = ${receiptId} order by id desc limit 1`)[0]?.[0];
  if (!cancelId) {
    const cancels = await ctx.api('cashier', 'POST', '/cashiering/records/cancellations', {
      companyId, receiptIds: [receiptId], reasonCode: reason, reasonText: 'Double issuance of the AR',
    });
    if (cancels[0]) {
      await ctx.api('cashier', 'POST', `/cashiering/records/${cancels[0].id}/submit`).catch(() => {});
      cancelId = cancels[0].id;
    }
  }
  ctx.state.records = { ar: ar.id, or: or.id, draft: draft.id, cancel: cancelId };
  return ctx.state.records;
}

/** A Bills Payment file written by the channel simulator in BDOI's layout and uploaded on Payment Files. */
async function paymentFile(ctx) {
  if (!ctx.state.paymentFile) {
    const companyId = company(ctx);
    const content = await ctx.api('cashier', 'POST', `/cashiering/payment-files/simulator?companyId=${companyId}&fileType=BILLS_PAYMENT&rows=4`);
    const form = new FormData();
    form.append('companyId', String(companyId));
    form.append('types', 'BILLS_PAYMENT');
    form.append('files', new Blob([content]), 'BDOI20261009.txt');
    await ctx.api('cashier', 'POST', '/cashiering/payment-files', form);
    ctx.state.paymentFile = true;
  }
}

async function open(ctx, user, path) {
  const page = await ctx.pageOf(user);
  await page.goto(`${ctx.BASE}${path}`);
  await ctx.settle(page, 1000);
  return page;
}

const custom = {
  'scr-op-08-01-preview': async (ctx) => {
    const page = await receivePayment(ctx);
    for (const step of fills.receive_payment) {
      if (typeof step === 'function') {
        await step(page);
      } else {
        await fill(page, step[0], step[1]);
      }
    }
    return page;
  },
  'scr-op-08-02-error': async (ctx) => {
    const page = await receivePayment(ctx);
    await page.getByRole('button', { name: /^issue ar and apply$/i }).click();
    await ctx.settle(page, 800);
    return page;
  },
  // Receipts, with the posting step of the ARs back on (as delivered) for the screens that follow.
  'scr-op-09-01-list': async (ctx) => { await postingStep(ctx, 'AR,OR'); return open(ctx, 'cashier', '/cashiering/receipts'); },
  'scr-op-07-01-home': async (ctx) => { await postingStep(ctx, 'AR,OR'); await records(ctx); return open(ctx, 'cashtl', '/cashiering'); },
  'scr-op-65-02-filled': async (ctx) => open(ctx, 'cashier', `/cashiering/records/${(await records(ctx)).draft}/edit`),
  'scr-op-65-03-error': async (ctx) => {
    const page = await open(ctx, 'cashier', '/cashiering/records/new?kind=AR');
    await ctx.clickButton(page, '^save$');
    await ctx.settle(page, 600);
    return page;
  },
  'scr-op-66-01-record': async (ctx) => open(ctx, 'cashtl', `/cashiering/records/${(await records(ctx)).ar}`),
  'scr-op-66-02-post': async (ctx) => {
    const page = await open(ctx, 'cashtl', `/cashiering/records/${(await records(ctx)).or}`);
    await page.getByRole('button', { name: /^post$/i }).first().click();
    await ctx.settle(page, 600);
    return page;
  },
  'scr-op-66-03-cancellation': async (ctx) => open(ctx, 'cashtl', `/cashiering/records/${(await records(ctx)).cancel}`),
  'scr-op-67-01-list': async (ctx) => { await records(ctx); return open(ctx, 'cashtl', '/cashiering/posting'); },
  'scr-op-67-02-cancellations': async (ctx) => {
    await records(ctx);
    return open(ctx, 'cashtl', '/cashiering/posting?kind=CANCELLATION');
  },
  'scr-op-68-01-files': async (ctx) => { await paymentFile(ctx); return open(ctx, 'cashier', '/cashiering/payment-files'); },
  'scr-op-68-02-report': async (ctx) => {
    await paymentFile(ctx);
    const page = await open(ctx, 'cashier', '/cashiering/payment-files');
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await page.getByRole('menuitem', { name: /^run report$/i }).first().click();
    await ctx.settle(page, 1000);
    return page;
  },
  'scr-op-70-01-list': async (ctx) => { await records(ctx); return open(ctx, 'cashier', '/cashiering/day-end'); },
};

const recordCrops = {
  'scr-op-68-02-report': 'section.card:has(> header h2:text-matches("^Run Report"))',
};
Object.assign(crops, recordCrops);

module.exports = { opens, fills, selects, after, crops, widths, custom, walkthrough: walkthrough.steps, documents,
  prepare: walkthrough.prepare };
