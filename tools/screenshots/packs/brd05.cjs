// How capture_pack.cjs reaches each screen state of the BRD-05 Accounting, Disbursement and ACSL sign-off pack on the
// seed profile: the record to open for a status (seed database), what is typed into the forms (fictitious seed
// values), the walkthrough steps and the generated documents. Run the walkthroughs first on a fresh seed database
// ('^wt-'), then the screens, documents and UX states ('^(scr|doc|ux)-'): the screens show the accrual, the refund,
// the statement, the case and the correction that the walkthroughs create.
const walkthrough = require('./brd05_walkthrough.cjs');
const { render } = require('./brd01_documents.cjs');
const { settle, button } = require('./brd01_walkthrough.cjs');
const { fill, dateText } = require('./brd02_walkthrough.cjs');

const { rowAction, tick, dialogOf, openDialog, choose, accrualForm, refundForm, previousMonth } = walkthrough;

// ------------------------------------------------------------------ records prepared through the services

const COMPANY = (ctx) => Number(ctx.one("select id from org_company where code = 'FVI'"));
const HEAD_OFFICE = (ctx) => Number(ctx.one("select id from org_branch where code = 'HO' order by id limit 1"));

const EMPLOYEES = [
  ['E-2019-0142', 'Teresa M. Villanueva', 'FIN', 'Accounting Supervisor', '2019-06-17'],
  ['E-2021-0388', 'Ramon B. Castillo', 'MKT', 'Account Officer', '2021-02-01'],
  ['E-2023-0517', 'Joanna P. Lim', 'MKT', 'Marketing Associate', '2023-09-04'],
];

/** The employees of the head office, maintained by the Business Administrator (the seed loads none). */
async function employees(ctx) {
  for (const [no, name, cc, position, hired] of EMPLOYEES) {
    if (ctx.sql(`select 1 from org_employee where employee_no = '${no}'`).length === 0) {
      await ctx.api('badmin', 'POST', '/organization/employees', { companyId: COMPANY(ctx), employeeNo: no,
        fullName: name, branchId: HEAD_OFFICE(ctx), costCenter: cc, position, hiredOn: hired });
    }
  }
}

/** A manual journal waiting for approval, entered and submitted by the GL Officer. */
async function pendingJournal(ctx) {
  const query = "select id, batch_no from jnl_batch where status = 'PENDING_APPROVAL' and narration = 'Telephone and internet bills of September' order by id desc limit 1";
  if (ctx.sql(query).length === 0) {
    const j = await ctx.api('glofficer', 'POST', '/journals', { companyId: COMPANY(ctx), branchId: HEAD_OFFICE(ctx),
      journalType: 'MANUAL', valueDate: new Date(Date.now() + 8 * 3600000).toISOString().slice(0, 10), currency: 'PHP',
      narration: 'Telephone and internet bills of September', reference: 'PLDT SOA 0926',
      lines: [
        { accountCode: '5604', side: 'DEBIT', amount: 24860.5, costCenter: 'FIN' },
        { accountCode: '2502', side: 'CREDIT', amount: 24860.5 },
      ] });
    await ctx.api('glofficer', 'POST', `/journals/${j.id}/submit`);
  }
  return ctx.sql(query)[0];
}

/** A draft refund of the Marketing Processor, waiting for its preparer. */
async function draftRefund(ctx) {
  const query = "select id from prq_request where reference_text = '2026_124 Refund' order by id limit 1";
  if (ctx.sql(query).length === 0) {
    await ctx.api('mktao', 'POST', `/payment-requests/requests/refunds?companyId=${COMPANY(ctx)}`, {
      segment: 'Retail', referenceText: '2026_124 Refund', requestingUnit: 'Marketing - Retail Sales',
      purpose: 'Refund of the double payment of the first installment', currency: 'PHP', paymentMode: 'CHECK',
      accountName: 'Maria Clara R. Santos',
      lines: [{ arNo: 'AR-HO-000005', clientCode: 'CL-2026-000001', assuredName: 'Santos, Maria Clara Reyes',
        amount: 1500, reasonCode: 'DOUBLE_PAYMENT', branchUnit: 'Head Office' }],
    });
  }
  return ctx.one(query);
}

/** A second case of the invoice family, assigned to the processor and under investigation. */
async function investigatingCase(ctx) {
  const query = "select id from acsl_case where subject = 'Payment of the assured not applied to the endorsement' order by id limit 1";
  if (ctx.sql(query).length === 0) {
    const c = await ctx.api('acsltl', 'POST', `/acsl/cases?companyId=${COMPANY(ctx)}`, { type: 'ANALYSIS_REQUEST',
      invoiceNo: walkthrough.varianceInvoice(ctx), subject: 'Payment of the assured not applied to the endorsement',
      details: 'Collections asks whether the payment of 30-Sep is for the endorsement or for the original invoice.' });
    await ctx.api('acsltl', 'POST', `/acsl/cases/${c.id}/assign`, { username: 'acsl' });
    await ctx.api('acsl', 'PUT', `/acsl/cases/${c.id}/findings`, { findings: 'The payment matches the premium of the endorsement; the application to the original invoice is being checked with Cashiering.' });
  }
  return ctx.one(query);
}

async function prepare(ctx) {
  await walkthrough.prepare(ctx);
  await employees(ctx);
}

// ------------------------------------------------------------------ records to open

const opens = {
  'open_journal:posted': (ctx) => `/gl/journals/${walkthrough.accrualId(ctx)}`,
  'open_journal:pending': async (ctx) => `/gl/journals/${(await pendingJournal(ctx))[0]}`,
  'open_report:trial_balance': async (ctx) => {
    const list = await ctx.api('glofficer', 'GET', '/reports');
    const tb = list.find((r) => /^trial balance$/i.test(r.name || r.title || ''));
    return `/reports/${tb ? tb.code : 'FIN-TB-MAIN'}`;
  },
  'open_run:approved': (ctx) => `/frbs/service-fee/runs/${ctx.one("select id from frbs_service_fee_run order by id limit 1")}`,
  'open_request:received': () => '/disbursement',
  'open_voucher:in_process': (ctx) => `/disbursement/vouchers/${ctx.one("select id from dsb_voucher where stage = 'IN_PROCESS' order by id limit 1")}`,
  'open_voucher:for_approval': (ctx) => `/disbursement/vouchers/${ctx.one("select id from dsb_voucher where stage = 'FOR_APPROVAL' order by id limit 1")}`,
  'open_voucher:check': (ctx) => `/disbursement/vouchers/${ctx.one("select v.id from dsb_voucher v join dsb_instrument i on i.voucher_id = v.id where v.mode = 'CHECK' order by v.id limit 1")}`,
  'open_payee:active': (ctx) => `/disbursement/payees/${ctx.one("select id from dsb_payee where payee_code = 'INS-MGIC'")}`,
  'open_payee:new': () => '/disbursement/payees/new',
  'open_funding:approved': (ctx) => `/disbursement/funding/${ctx.one("select id from dsb_funding_request where stage = 'APPROVED' order by id limit 1")}`,
  'open_funding:new': () => '/disbursement/funding/new',
  'open_request:draft_refund': async (ctx) => `/payment-requests/requests/${await draftRefund(ctx)}`,
  'open_request:preparing': async (ctx) => `/payment-requests/requests/${await draftRefund(ctx)}`,
  'open_request:disbursed': (ctx) => `/payment-requests/requests/${walkthrough.refundId(ctx)}`,
  'open_case:investigating': async (ctx) => `/acsl/cases/${await investigatingCase(ctx)}`,
  'open_correction:draft': async (ctx) => {
    const query = "select id from acsl_correction where description = 'Reclassify the DST of the endorsement' order by id limit 1";
    if (ctx.sql(query).length === 0) {
      const c = await ctx.api('acsl', 'POST', `/acsl/cases/${await investigatingCase(ctx)}/correction`,
        { kind: 'RECLASS', description: 'Reclassify the DST of the endorsement' });
      await ctx.api('acsltl', 'POST', `/acsl/corrections/${c.id}/assign`, { username: 'acsl' });
    }
    return `/acsl/corrections/${ctx.one(query)}`;
  },
  'open_correction:posted': (ctx) => `/acsl/corrections/${walkthrough.correctionOfCase(ctx)}`,
  'open_soa:latest': (ctx) => `/acsl/soa/${walkthrough.soaUpload(ctx)}`,
};

// ------------------------------------------------------------------ forms

const inDialog = (label, value) => async (page) => fill(page, label, value, dialogOf(page));

const fills = {
  assign_journal: [
    async (page, ctx) => {
      const [, no] = await pendingJournal(ctx);
      await fill(page, 'Batch no.', no);
      await settle(page, 900);
      await tick(page, no);
      const d = await openDialog(page, /^assign \(/i);
      await choose(d.getByLabel('Assign to'), /Graciela/);
    },
  ],
  post_selected: [
    async (page, ctx) => {
      const [, no] = await pendingJournal(ctx);
      await fill(page, 'Batch no.', no);
      await settle(page, 900);
      await tick(page, no);
      await openDialog(page, /^post selected/i);
    },
  ],
  accrual_journal: [async (page) => accrualForm(page)],
  unbalanced_journal: [async (page) => accrualForm(page, 158000)],
  journals_none: [async (page) => { await fill(page, 'Batch no.', 'JV-HO-2026-999999'); await settle(page, 900); }],
  inquiry_cash: [
    async (page) => {
      const account = page.getByLabel(/^Account/).first();
      await account.fill('1111');
      await account.press('Tab');
      await settle(page, 1500);
    },
  ],
  party_insurer: [
    async (page) => {
      await fill(page, 'Party code', 'INS-MGIC');
      await settle(page, 1500);
    },
  ],
  recon_account: [async (page) => { await fill(page, 'Bank account', /BDO-CA|0012/); await settle(page, 1500); }],
  run_schedule: [
    async (page) => {
      const select = page.getByLabel(/^Schedule/).first();
      const options = await select.locator('option').allTextContents();
      const pick = options.find((o) => /receivable|premium/i.test(o)) || options.find((o, i) => i > 0 && o.trim());
      await select.selectOption({ label: pick });
      await page.getByRole('button', { name: /^run schedule$/i }).last().click();
      await settle(page, 2000);
    },
  ],
  encode_supplier: [
    async (page) => {
      await fill(page, 'Disbursement Type', /supplier/i);
      await fill(page, 'Payee Code', 'S-0001');
      await fill(page, 'Payee Name', 'Metro Office Supplies Co.');
      await fill(page, 'Amount', '36520.00');
      await fill(page, 'RFP No.', 'RFP-2026-0398');
      await fill(page, 'Purpose', 'Office supplies of the head office for October');
    },
  ],
  new_payee: [
    async (page) => {
      await fill(page, 'Payee Code', 'S-0388');
      await fill(page, 'Payee Class', /supplier/i);
      await fill(page, 'Name', 'Northpoint Courier Services, Inc.');
      await fill(page, 'TIN', '008-412-339-000');
      await fill(page, 'E-mail', 'billing@northpoint-courier.example');
      await fill(page, 'Address', '2/F Pacific Building, 1045 Quirino Avenue, Paco, Manila');
      await fill(page, 'Default Mode', /check/i);
    },
  ],
  new_funding: [
    async (page) => {
      await fill(page, 'From Account', /BPI/);
      await fill(page, 'To Account', /BDO-CA|0012/);
      await fill(page, 'Amount', '2500000.00');
      await fill(page, 'Value Date', dateText(1));
      await fill(page, 'Purpose', 'Funding of the checks to be released on the next business day');
    },
  ],
  add_series: [
    async (page) => {
      await rowAction(page.locator('main table tbody tr').filter({ hasText: 'BDO-CA' }).first(), /^add series$/i);
      await fill(page, 'First Check No.', '1000501', dialogOf(page));
      await fill(page, 'Last Check No.', '1001000', dialogOf(page));
    },
  ],
  refund_form: [async (page) => refundForm(page, '2026_140 Refund')],
  refund_duplicate: [async (page) => refundForm(page, '2026_141 Refund')],
  cash_advance: [
    async (page) => {
      await fill(page, 'Employee No.', 'E-2021-0388');
      await fill(page, 'Employee Name', 'Ramon B. Castillo');
      await fill(page, 'Type', /./);
      await fill(page, 'Amount (PHP)', '15000.00');
      await fill(page, 'Purpose', 'Travel to the Cebu branch for the client visits of 12 to 14 October');
      await fill(page, 'Segment', 'Retail');
      await fill(page, 'Requesting Unit', 'Marketing - Retail Sales');
      await fill(page, 'Mode of Payment', /^check$/i);
      await fill(page, 'Check / Payee Name', 'Ramon B. Castillo').catch(() => {});
    },
  ],
  open_case: [
    async (page, ctx) => {
      const d = await openDialog(page, /^open a case$/i);
      await choose(d.getByLabel(/^Case Type/), /analysis/i);
      await fill(page, 'Invoice No.', walkthrough.varianceInvoice(ctx), d);
      await fill(page, 'Subject', 'Balance of the invoice per Collections differs from the statement', d);
      await d.getByLabel(/^Details/).fill('Collections shows the invoice fully paid; the insurer still states a balance.');
    },
  ],
};

// ------------------------------------------------------------------ states after opening

const after = {
  'scr-ac-25-02-request': async (page) => {
    await page.locator('main table tbody tr').filter({ hasText: 'DSR-2026-000001' }).first().click();
    await dialogOf(page).waitFor({ timeout: 10000 }).catch(() => {});
    await settle(page, 600);
  },
  'scr-ac-11-02-schedule': async (page) => settle(page, 800),
  'ux-scr-ac-28-actions': async (page) => {
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await settle(page, 300);
  },
};

// ------------------------------------------------------------------ documents

const documents = {
  'doc-dv': async (ctx, out) => {
    const id = ctx.one("select id from dsb_voucher where stage = 'APPROVED' and mode = 'CHECK' order by id limit 1");
    render(await ctx.api('disb', 'GET', `/disbursement/vouchers/${id}/document`), 'pdf', out);
  },
  'doc-atd': async (ctx, out) => {
    const id = ctx.one("select v.id from dsb_voucher v join dsb_instrument i on i.voucher_id = v.id where v.mode <> 'CHECK' order by v.id limit 1");
    render(await ctx.api('disb', 'GET', `/disbursement/vouchers/${id}/instrument/document`), 'pdf', out);
  },
  'doc-dctf': async (ctx, out) => {
    const id = ctx.one("select id from dsb_eod_output where kind = 'DCTF' order by id limit 1");
    render(await ctx.api('disbtl', 'GET', `/disbursement/eod/outputs/${id}`), 'txt', out);
  },
  'doc-rrf': async (ctx, out) => {
    render(await ctx.api('mktao', 'GET', `/payment-requests/requests/${walkthrough.refundId(ctx)}/form`), 'pdf', out);
  },
  'doc-2307': async (ctx, out) => {
    const id = ctx.one("select id from tax_2307_certificate where status = 'ISSUED' order by id limit 1");
    render(await ctx.api('accountant', 'GET', `/tax/2307/certificates/${id}/pdf`), 'pdf', out);
  },
  'doc-soa-report': async (ctx, out) => {
    render(await ctx.api('acsl', 'GET', `/acsl/soa-uploads/${walkthrough.soaUpload(ctx)}/report`), 'xlsx', out, 200,
      ['Invoice No', 'Assured', 'Result', 'Premium per SOA', 'Premium per Books', 'Variance']);
  },
};

// A walkthrough step or screen shot that shows one tab of a long record page: the tab strip and the tab's content.
const TAB = 'main div.stack > div.tabs[role=tablist], main div.stack > div.tabs[role=tablist] ~ *';
const crops = {
  'scr-ac-27-02-entry': TAB, 'scr-ac-27-03-instrument': TAB, 'scr-ac-27-04-tags': TAB,
  'scr-ac-43-02-disbursement': TAB, 'scr-ac-46-02-family': TAB, 'scr-ac-50-02-log': TAB,
};

module.exports = { opens, fills, selects: {}, after, crops, custom: {}, walkthrough: walkthrough.steps, documents,
  prepare, previousMonth, button };
