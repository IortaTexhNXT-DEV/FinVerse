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
  const query = "select id, stage, findings is not null from acsl_case where subject = 'Payment of the assured not applied to the endorsement' order by id limit 1";
  if (ctx.sql(query).length === 0) {
    const c = await ctx.api('acsltl', 'POST', `/acsl/cases?companyId=${COMPANY(ctx)}`, { type: 'ANALYSIS_REQUEST',
      invoiceNo: walkthrough.varianceInvoice(ctx), subject: 'Payment of the assured not applied to the endorsement',
      details: 'Collections asks whether the payment of 30-Sep is for the endorsement or for the original invoice.' });
    await ctx.api('acsltl', 'POST', `/acsl/cases/${c.id}/assign`, { username: 'acsl' });
  }
  let [id, stage, findings] = ctx.sql(query)[0];
  if (stage === 'ASSIGNED') {
    await workflowAction(ctx, 'acsl', 'AcslCase', id, /start/i);
    [id, stage, findings] = ctx.sql(query)[0];
  }
  if (findings !== 't') {
    await ctx.api('acsl', 'PUT', `/acsl/cases/${id}/findings`, { findings: 'The payment matches the premium of the endorsement; the application to the original invoice is being checked with Cashiering.' });
  }
  return id;
}

/** Runs a workflow action of a record as a user, the way the record page does (Start Investigation...). */
async function workflowAction(ctx, user, entityType, entityId, action) {
  const detail = await ctx.api(user, 'GET', `/workflow/cases/by-record?entityType=${entityType}&entityId=${entityId}`);
  const pick = detail.actions.find((a) => action.test(a.action) || action.test(a.label));
  if (!pick) {
    throw new Error(`no action ${action} on ${entityType} ${entityId}: ${detail.actions.map((a) => a.action).join(', ')}`);
  }
  await ctx.api(user, 'POST', `/workflow/cases/${detail.item.caseId ?? detail.item.id}/actions/${pick.action}`, {});
}

/** A supplier payment by authority to debit, approved, so its bank form can be printed. */
async function atdVoucher(ctx) {
  const query = "select id from dsb_voucher where mode = 'ATD' and stage = 'APPROVED' order by id limit 1";
  if (ctx.sql(query).length === 0) {
    // The building administrator is paid by authority to debit; the team leader keeps the payee, the approver
    // authorises it.
    if (ctx.sql("select 1 from dsb_payee where payee_code = 'S-0271' and stage = 'ACTIVE'").length === 0) {
      if (ctx.sql("select 1 from dsb_payee where payee_code = 'S-0271'").length === 0) {
        await ctx.api('disbtl', 'POST', '/disbursement/payees', { companyId: COMPANY(ctx), payeeCode: 'S-0271',
          payeeClass: 'SUPPLIER', name: 'Pacific Facilities Management Corp.', tin: '009-221-845-000',
          address: '18/F Ayala Tower One, Ayala Avenue, Makati City', defaultMode: 'ATD', allowedModes: ['ATD', 'CHECK'],
          disbursementTypes: ['SUPPLIER'], currency: 'PHP' });
      }
      const payee = ctx.one("select id from dsb_payee where payee_code = 'S-0271'");
      if (ctx.one(`select stage from dsb_payee where id = ${payee}`) === 'DRAFT') {
        await ctx.api('disbtl', 'POST', `/disbursement/payees/${payee}/submit`);
      }
      await ctx.api('disbappr', 'POST', `/disbursement/payees/${payee}/authorize`);
    }
    const request = "select id, status, voucher_id from dsb_request where rfp_no = 'RFP-2026-0413' order by id limit 1";
    if (ctx.sql(request).length === 0) {
      await ctx.api('disb', 'POST', '/disbursement/requests', { companyId: COMPANY(ctx), disbursementType: 'SUPPLIER',
        payeeCode: 'S-0271', currency: 'PHP', amount: 48250, purpose: 'Building maintenance dues of the head office for October',
        rfpNo: 'RFP-2026-0413' });
    }
    const [requestId, status] = ctx.sql(request)[0];
    if (status !== 'IN_VOUCHER') {
      await ctx.api('disb', 'POST', `/disbursement/requests/${requestId}/voucher`);
    }
    const voucherId = ctx.one(`select voucher_id from dsb_request where id = ${requestId}`);
    const bank = Number(ctx.one("select id from pay_bank_account where code = 'BDO-CA'"));
    await ctx.api('disb', 'PUT', `/disbursement/vouchers/${voucherId}/terms`, { mode: 'ATD', bankAccountId: bank, ewt: 0,
      purpose: 'Building maintenance dues of the head office for October', valueDate: new Date(Date.now() + 8 * 3600000).toISOString().slice(0, 10) });
    await ctx.api('disb', 'POST', `/disbursement/vouchers/${voucherId}/submit`, {});
    await ctx.api('disbtl', 'POST', `/disbursement/vouchers/${voucherId}/submit-for-approval`, {});
    await ctx.api('disbappr', 'POST', `/disbursement/vouchers/${voucherId}/approve`, {});
  }
  return ctx.one(query);
}

/** A refund endorsed by the reviewer and waiting for the Marketing approver. */
async function refundForApproval(ctx) {
  const query = "select id, stage from prq_request where reference_text = '2026_127 Refund' order by id limit 1";
  if (ctx.sql(query).length === 0) {
    await ctx.api('mktao', 'POST', `/payment-requests/requests/refunds?companyId=${COMPANY(ctx)}`, {
      segment: 'Retail', referenceText: '2026_127 Refund', requestingUnit: 'Marketing - Retail Sales',
      purpose: 'Refund of the premium decrease of the endorsement of the motor policy', currency: 'PHP',
      paymentMode: 'CHECK', accountName: 'Jose Miguel L. Reyes',
      lines: [{ arNo: 'AR-HO-000004', clientCode: 'CL-2026-000002', assuredName: 'Reyes, Jose Miguel Lopez',
        amount: 1500, reasonCode: 'PREMIUM_DECREASE', branchUnit: 'Head Office' }],
    });
  }
  let [id, stage] = ctx.sql(query)[0];
  if (stage === 'DRAFT') {
    await ctx.api('mktao', 'POST', `/payment-requests/requests/${id}/submit`, { comment: 'For review' });
    [id, stage] = ctx.sql(query)[0];
  }
  if (stage === 'FOR_REVIEW') {
    await ctx.api('mktrev', 'POST', `/payment-requests/requests/${id}/endorse`, { comment: 'Checked against the endorsement' });
  }
  return id;
}

/** The GL accounts of the cash-advance liquidations, kept by the Comptrollership administrator. */
async function liquidationAccounts(ctx) {
  const accounts = { PER_DIEM: '5606', REPRESENTATION: '5609', TRANSPORT: '5606', LODGING: '5606', OTHER: '5613', CASH: '1101' };
  for (const [role, accountCode] of Object.entries(accounts)) {
    await ctx.api('fmanager', 'PUT', `/payment-requests/liquidation-accounts?companyId=${COMPANY(ctx)}`, { role, accountCode });
  }
}

async function prepare(ctx) {
  await walkthrough.prepare(ctx);
  await employees(ctx);
  await refundForApproval(ctx);
  await liquidationAccounts(ctx);
  // The GL-SL reconciliation of the day, run by the ACSL Team Leader.
  if (ctx.sql('select 1 from acsl_glsl_run').length === 0) {
    const asOf = new Date(Date.now() + 8 * 3600000).toISOString().slice(0, 10);
    await ctx.api('acsltl', 'POST', `/acsl/gl-sl/runs?companyId=${COMPANY(ctx)}&asOf=${asOf}`);
  }
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
  recon_account: [async (page) => { await fill(page, 'Bank account', /^1111 /); await settle(page, 1500); }],
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
      await fill(page, 'Default Mode', /^check$/i);
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
      await fill(page, 'Type', /^(?!Select)\S/);
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
  'scr-ac-11-02-schedule': async (page) => {
    await fill(page, 'Period', new RegExp(`^${previousMonth().iso}`), dialogOf(page));
    await settle(page, 800);
  },
  'scr-ac-43-03-assign': async (page) => {
    await choose(dialogOf(page).getByLabel(/^Preparer/), /Marco/);
    await settle(page, 300);
  },
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
    const id = await atdVoucher(ctx);
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
    render(await ctx.api('acsl', 'GET', `/acsl/soa-uploads/${walkthrough.soaUpload(ctx)}/report`), 'xlsx', out);
  },
};

// A walkthrough step or screen shot that shows one tab of a long record page: the tab strip and the tab's content.
const TAB = 'main div.stack > div.tabs[role=tablist], main div.stack > div.tabs[role=tablist] ~ *';
const crops = {
  'scr-ac-27-02-entry': TAB, 'scr-ac-27-03-instrument': TAB, 'scr-ac-27-04-tags': TAB,
  'scr-ac-43-02-disbursement': TAB, 'scr-ac-46-02-family': TAB,
};

// Lists wider than their card at the standard window are taken in a wider window (CSS pixels).
const widths = { 'scr-ac-16-01-match': 1900 };

module.exports = { opens, fills, selects: {}, after, crops, widths, custom: {}, walkthrough: walkthrough.steps, documents,
  prepare, previousMonth, button };
