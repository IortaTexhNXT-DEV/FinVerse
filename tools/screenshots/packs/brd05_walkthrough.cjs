// End-to-end walkthroughs of BRD-05 Accounting, Disbursement and ACSL, performed live on the seed profile by
// capture_pack.cjs (see walkthroughs.yaml of the pack). Each step signs in as the persona of the step, does what the
// step says on the screen and returns the page to capture. The walkthroughs run in order on a fresh seed database
// (A, B, C, D, then E): E works on the refund of B and the statement of C. The records created carry fictitious seed
// values; every step first checks whether its record exists, so a walkthrough can be run again.
const fs = require('fs');
const path = require('path');
const os = require('os');
const { act, tab, go, button, settle } = require('./brd01_walkthrough.cjs');
const { fill, dateText } = require('./brd02_walkthrough.cjs');

// ------------------------------------------------------------------ the data of the walkthroughs

const ACCRUAL = 'Accrual of the October rent and utilities of the head office';
const EXPENSE = '5603';
const ACCRUED = '2502';
const REFUND_AR = 'AR-HO-000002';
const REFUND_CLIENT = 'CL-2026-000001';
const REFUND_REF = '2026_118 Refund';
const SOA_FILE = path.join(os.tmpdir(), 'bibs-brd05-soa', 'Mabuhay_General_SOA_September_2026.csv');
const CASE_SUBJECT = 'Premium per the Mabuhay General statement differs from the books';
const NO_PAYEE = 'S-0417';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const MANILA_OFFSET_MS = 8 * 3600000;
const manila = () => new Date(Date.now() + MANILA_OFFSET_MS);

/** The first day of the next month as dd-MMM-yyyy (the reversal date of an accrual). */
function firstOfNextMonth() {
  const d = manila();
  const n = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 1));
  return `01-${MONTHS[n.getUTCMonth()]}-${n.getUTCFullYear()}`;
}

/** The previous month as MMM-yyyy and its first and last day as dd-MMM-yyyy. */
function previousMonth() {
  const d = manila();
  const first = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() - 1, 1));
  const last = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), 0));
  const text = (x) => `${String(x.getUTCDate()).padStart(2, '0')}-${MONTHS[x.getUTCMonth()]}-${x.getUTCFullYear()}`;
  return { month: `${MONTHS[first.getUTCMonth()]}-${first.getUTCFullYear()}`, from: text(first), to: text(last),
    iso: first.toISOString().slice(0, 7) };
}

// ------------------------------------------------------------------ records

function has(ctx, query) {
  return ctx.sql(query).length > 0;
}

const q = (s) => s.replace(/'/g, "''");
const accrualId = (ctx) => ctx.one(`select id from jnl_batch where narration = '${q(ACCRUAL)}' order by id desc limit 1`);
const accrualNo = (ctx) => ctx.one(`select batch_no from jnl_batch where narration = '${q(ACCRUAL)}' order by id desc limit 1`);
const refundId = (ctx) => ctx.one(`select id from prq_request where reference_text = '${REFUND_REF}' order by id limit 1`);
const refundNo = (ctx) => ctx.one(`select request_no from prq_request where reference_text = '${REFUND_REF}' order by id limit 1`);
const refundStage = (ctx) => ctx.one(`select stage from prq_request where reference_text = '${REFUND_REF}' order by id limit 1`);
const voucherOfRefund = (ctx) => ctx.sql(`select v.id, v.stage from dsb_voucher v join dsb_request r on r.id = v.request_id where r.source_ref = '${refundNo(ctx)}' order by v.id desc limit 1`)[0];
const soaUpload = (ctx) => ctx.one("select id from acsl_soa_upload where file_name like 'Mabuhay_General_SOA_%' order by id limit 1");
const caseId = (ctx) => ctx.one(`select id from acsl_case where subject = '${q(CASE_SUBJECT)}' order by id desc limit 1`);
const correctionOfCase = (ctx) => ctx.one(`select id from acsl_correction where case_id = ${caseId(ctx)} order by id desc limit 1`);

/** The open invoices of Mabuhay General as the insurer states them on its statement of account. */
function writeSoaFile(ctx) {
  if (fs.existsSync(SOA_FILE)) {
    return SOA_FILE;
  }
  const rows = ctx.sql("select invoice_no, coalesce(policy_no, ''), assured_name, inception_date, expiry_date, gross_premium from ops_invoice where insurer_code = 'INS-MGIC' and kind = 'BOOKING' and not cancelled order by invoice_no limit 6");
  const lines = ['Invoice No,Policy No,Assured,Inception Date,Expiry Date,Gross Premium,Balance,Payments'];
  rows.forEach(([inv, pol, assured, from, to, gross], i) => {
    // The insurer states the second invoice with a premium 1,250.00 higher than the books (the variance of the case).
    const premium = (Number(gross) + (i === 1 ? 1250 : 0)).toFixed(2);
    lines.push([inv, pol, `"${assured}"`, from, to, premium, premium, '0.00'].join(','));
  });
  fs.mkdirSync(path.dirname(SOA_FILE), { recursive: true });
  fs.writeFileSync(SOA_FILE, `${lines.join('\n')}\n`);
  return SOA_FILE;
}

/** The invoice with the premium variance on the statement. */
function varianceInvoice(ctx) {
  return ctx.one("select invoice_no from ops_invoice where insurer_code = 'INS-MGIC' and kind = 'BOOKING' and not cancelled order by invoice_no offset 1 limit 1");
}

/** The current accounting period is opened by the finance manager, the way Finance does at the start of a month. */
async function prepare(ctx) {
  const future = ctx.sql("select id from per_period where status = 'FUTURE' and start_date <= current_date order by start_date, id");
  for (const [id] of future) {
    await ctx.api('fmanager', 'POST', `/periods/${id}/open`);
  }
  writeSoaFile(ctx);
}

// ------------------------------------------------------------------ page helpers

const dialogOf = (page) => page.locator('dialog.modal[open]').last();

/** Runs an action of a table row: opens the row's action menu, then chooses the action. */
async function rowAction(row, name) {
  const actions = row.getByRole('button', { name: /^Actions for/ });
  await actions.evaluate((el) => el.scrollIntoView({ block: 'center', inline: 'nearest' }));
  await actions.click();
  await row.page().waitForTimeout(300);
  await row.page().getByRole('menuitem', { name }).first().click();
  await settle(row.page(), 600);
}

/** Clicks the confirming button of the open dialog and waits for the dialog to close (or to show its message). */
async function confirmDialog(page, name, keepResult = false) {
  const dialog = dialogOf(page);
  await dialog.locator('footer.modal-footer').getByRole('button', { name }).last().click();
  if (!keepResult) {
    await dialog.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => {});
  }
  await settle(page, 1200);
}

/** Opens a dialog with a page button and waits for it. */
async function openDialog(page, name) {
  await button(page, name).click();
  await dialogOf(page).waitFor({ timeout: 10000 });
  await page.waitForTimeout(400);
  return dialogOf(page);
}

/** Selects the option of a select whose text matches (a select found by a locator, not by its label). */
async function choose(select, value) {
  const options = await select.locator('option').allTextContents();
  const hit = options.find((o) => value.test(o));
  if (hit === undefined) {
    throw new Error(`option ${value} not in ${options.slice(0, 12).join(', ')}`);
  }
  await select.selectOption({ label: hit });
}

/** Ticks the check box of a table row by its aria label (Select <reference>). */
async function tick(page, reference) {
  const box = page.getByRole('checkbox', { name: `Select ${reference}` }).first();
  await box.waitFor({ timeout: 15000 });
  await box.check();
  await page.waitForTimeout(400);
}

/** The lines of a journal voucher: account, side, amount and cost centre of each line. */
async function journalLines(page, lines) {
  for (let i = 0; i < lines.length; i += 1) {
    if (i >= 2) {
      await button(page, lines[i].side === 'CREDIT' ? /^add credit$/i : /^add debit$/i).click();
      await page.waitForTimeout(300);
    }
    const n = i + 1;
    const account = page.getByLabel(`Account for line ${n}`);
    await account.fill(lines[i].account);
    await account.press('Tab');
    await page.getByLabel(`Debit or credit for line ${n}`).selectOption(lines[i].side);
    await page.getByLabel(`Amount for line ${n}`).fill(String(lines[i].amount));
    if (lines[i].costCentre) {
      await choose(page.getByLabel(`Cost centre for line ${n}`), lines[i].costCentre);
    }
    if (lines[i].narration) {
      await page.getByLabel(`Narration for line ${n}`).fill(lines[i].narration);
    }
  }
  await page.waitForTimeout(400);
}

/** The accrual of walkthrough A on New Journal (header and lines), before it is saved. */
async function accrualForm(page, amount = 185000) {
  await fill(page, 'Journal type', /^accrual$/i);
  await fill(page, 'Value date', dateText(0));
  await fill(page, 'Reverse on', firstOfNextMonth());
  await fill(page, 'Reference', 'Lease contract 2026-HO-01, October billing');
  await page.getByLabel(/^Narration/).first().fill(ACCRUAL);
  await journalLines(page, [
    { account: EXPENSE, side: 'DEBIT', amount, costCentre: /^FIN/, narration: 'October rent and utilities, head office' },
    { account: ACCRUED, side: 'CREDIT', amount: 185000, narration: 'Accrued rent and utilities, October' },
  ]);
}

/** Brings the tab strip of a record to the top of the window, so the tab's content is captured whole. */
async function tabsToTop(page) {
  await page.evaluate(() => {
    const t = document.querySelector('main div.tabs[role=tablist]');
    if (!t) return;
    let el = t;
    while (el.parentElement && el.parentElement.tagName !== 'MAIN' && !el.parentElement.classList.contains('stack')) {
      el = el.parentElement;
    }
    for (const sibling of el.parentElement ? el.parentElement.children : []) {
      if (sibling === el) break;
      sibling.style.display = 'none';
    }
  });
  await page.waitForTimeout(400);
}

/** The refund form of walkthrough B (and of the duplicate AR of walkthrough E). */
async function refundForm(page, reference) {
  await fill(page, 'Segment', 'Retail');
  await fill(page, 'Reference', reference);
  await fill(page, 'Requesting Unit', 'Marketing - Retail Sales');
  await fill(page, 'Purpose / Remarks', 'Refund of the overpayment of the premium of the motor policy');
  await page.getByLabel('AR No. 1').fill(REFUND_AR);
  await page.getByLabel('Client No. 1').fill(REFUND_CLIENT);
  await page.getByLabel('Client No. 1').press('Tab');
  await page.getByLabel('Assured 1').fill('Santos, Maria Clara Reyes');
  await page.getByLabel('Amount 1').fill('690.00');
  await choose(page.locator('#reason-0'), /overpayment/i);
  await page.getByLabel('Branch / Unit 1').fill('Head Office');
  await settle(page, 600);
  await fill(page, 'Mode of Payment', /credit to CA/i);
  const account = page.locator('main input[inputmode=numeric][maxlength="16"]').first();
  if ((await account.inputValue()) === '') {
    await account.fill('001122334455');
  }
  const name = page.getByLabel(/^Account Name/).first();
  if ((await name.inputValue()) === '') {
    await name.fill('Maria Clara R. Santos');
  }
}

// ------------------------------------------------------------------ the steps

const steps = {
  // ---------------------------------------------------------------- walkthrough A
  // 1. The GL Officer enters the accrual and submits it.
  'wt-a-01': async (ctx) => {
    if (has(ctx, `select 1 from jnl_batch where narration = '${q(ACCRUAL)}'`)) {
      return go(ctx, 'glofficer', `/gl/journals/${accrualId(ctx)}`);
    }
    const page = await go(ctx, 'glofficer', '/gl/journals/new');
    await accrualForm(page);
    await button(page, /^save & submit$/i).click();
    await page.waitForTimeout(700);
    if (await dialogOf(page).isVisible().catch(() => false)) {
      await confirmDialog(page, /^save & submit$/i);
    }
    await page.waitForURL(/\/gl\/journals\/\d+/, { timeout: 20000 }).catch(() => {});
    await settle(page, 1200);
    return page;
  },
  // 2. The GL Team Lead assigns it to the GL Section Head.
  'wt-a-02': async (ctx) => {
    const page = await go(ctx, 'gltl', '/gl/journals');
    const no = accrualNo(ctx);
    if (!has(ctx, `select 1 from jnl_batch where batch_no = '${no}' and assigned_to = 'glhead'`)) {
      await fill(page, 'Batch no.', no);
      await settle(page, 900);
      await tick(page, no);
      const d = await openDialog(page, /^assign \(/i);
      await choose(d.getByLabel('Assign to'), /Graciela/);
      await confirmDialog(page, /^assign$/i);
    }
    return page;
  },
  // 3. The GL Section Head posts it from the journals assigned to her.
  'wt-a-03': async (ctx) => {
    const page = await go(ctx, 'glhead', '/gl/journals');
    const no = accrualNo(ctx);
    await fill(page, 'Assignment', /assigned to me/i);
    await settle(page, 900);
    if (ctx.one(`select status from jnl_batch where batch_no = '${no}'`) === 'PENDING_APPROVAL') {
      await tick(page, no);
      await openDialog(page, /^post selected/i);
      await confirmDialog(page, /post/i);
    }
    return page;
  },
  // 4. The posted voucher.
  'wt-a-04': async (ctx) => go(ctx, 'glhead', `/gl/journals/${accrualId(ctx)}`),
  // 5. Account Inquiry of the expense account.
  'wt-a-05': async (ctx) => {
    const page = await go(ctx, 'glofficer', '/gl/inquiry');
    const account = page.getByLabel(/^Account/).first();
    await account.fill(EXPENSE);
    await account.press('Tab');
    await settle(page, 1500);
    return page;
  },

  // ---------------------------------------------------------------- walkthrough B
  // 1. The Marketing Processor raises the Refund Request Form.
  'wt-b-01': async (ctx) => {
    if (has(ctx, `select 1 from prq_request where reference_text = '${REFUND_REF}'`)) {
      return go(ctx, 'mktao', `/payment-requests/requests/${refundId(ctx)}`);
    }
    const page = await go(ctx, 'mktao', '/payment-requests/new-refund');
    await refundForm(page, REFUND_REF);
    await button(page, /^save request$/i).click();
    await page.waitForURL(/\/payment-requests\/requests\/\d+/, { timeout: 20000 }).catch(() => {});
    await settle(page, 1200);
    return page;
  },
  // 2. Submitted for review.
  'wt-b-02': async (ctx) => {
    const page = await go(ctx, 'mktao', `/payment-requests/requests/${refundId(ctx)}`);
    if (refundStage(ctx) === 'DRAFT') {
      await act(page, /^submit for review$/i, { reason: false, confirm: /^submit for review$/i });
      await settle(page, 1200);
    }
    return page;
  },
  // 3. Endorsed by the reviewer.
  'wt-b-03': async (ctx) => {
    const page = await go(ctx, 'mktrev', `/payment-requests/requests/${refundId(ctx)}`);
    if (refundStage(ctx) === 'FOR_REVIEW') {
      await act(page, /^endorse for approval$/i, { reason: false, confirm: /^endorse for approval$/i });
      await settle(page, 1200);
    }
    return page;
  },
  // 4. Approved and sent to Disbursement.
  'wt-b-04': async (ctx) => {
    const page = await go(ctx, 'mktappr', `/payment-requests/requests/${refundId(ctx)}`);
    if (refundStage(ctx) === 'FOR_APPROVAL') {
      await act(page, /^approve and send$/i, { reason: false, confirm: /^approve and send$/i });
      await settle(page, 2000);
    }
    return page;
  },
  // 5. The voucher of the refund on the Disbursement Workbench, created from the request and routed to the approver.
  'wt-b-05': async (ctx) => {
    const page = await go(ctx, 'disb', '/disbursement');
    const stage = (voucherOfRefund(ctx) || [])[1];
    await tab(page, stage === 'APPROVED' ? 'Approved' : 'For Approval');
    return page;
  },
  // 6. The voucher with its payee, mode, paying account and entry.
  'wt-b-06': async (ctx) => go(ctx, 'disb', `/disbursement/vouchers/${voucherOfRefund(ctx)[0]}`),
  // 7. Approved and posted by the Disbursement Approver.
  'wt-b-07': async (ctx) => {
    const [id, stage] = voucherOfRefund(ctx);
    const page = await go(ctx, 'disbappr', `/disbursement/vouchers/${id}`);
    if (stage === 'FOR_APPROVAL') {
      await act(page, /^approve and post$/i, { reason: false, confirm: /^approve and post$/i });
      await settle(page, 2500);
    }
    return page;
  },
  // 8. The Disbursement tab of the request.
  'wt-b-08': async (ctx) => {
    const page = await go(ctx, 'mktao', `/payment-requests/requests/${refundId(ctx)}`);
    await tab(page, 'Disbursement');
    await tabsToTop(page);
    return page;
  },

  // ---------------------------------------------------------------- walkthrough C
  // 1. The statement of account uploaded and reconciled.
  'wt-c-01': async (ctx) => {
    const page = await go(ctx, 'acsl', '/acsl/soa');
    if (!has(ctx, "select 1 from acsl_soa_upload where file_name like 'Mabuhay_General_SOA_%'")) {
      const month = previousMonth();
      const d = await openDialog(page, /^upload soa$/i);
      await choose(d.getByLabel(/^Insurer/), /Mabuhay General/i);
      await fill(page, 'Period From', month.from, d);
      await fill(page, 'Period To', month.to, d);
      await d.locator('input[type=file]').setInputFiles(writeSoaFile(ctx));
      await page.waitForTimeout(500);
      await confirmDialog(page, /^upload and reconcile$/i);
    }
    return page;
  },
  // 2. The upload with its results.
  'wt-c-02': async (ctx) => go(ctx, 'acsl', `/acsl/soa/${soaUpload(ctx)}`),
  // 3. The team leader opens a case on the invoice with the variance.
  'wt-c-03': async (ctx) => {
    const page = await go(ctx, 'acsltl', '/acsl');
    if (!has(ctx, `select 1 from acsl_case where subject = '${q(CASE_SUBJECT)}'`)) {
      const d = await openDialog(page, /^open a case$/i);
      await choose(d.getByLabel(/^Case Type/), /investigat/i);
      await fill(page, 'Invoice No.', varianceInvoice(ctx), d);
      await fill(page, 'Subject', CASE_SUBJECT, d);
      await d.getByLabel(/^Details/).fill('The statement of September shows a premium 1,250.00 higher than the booked premium. Check the booking against the policy schedule.');
      await confirmDialog(page, /^open case$/i);
    }
    return page;
  },
  // 4. Assigned to the ACSL Processor.
  'wt-c-04': async (ctx) => {
    const page = await go(ctx, 'acsltl', `/acsl/cases/${caseId(ctx)}`);
    if (ctx.one(`select stage from acsl_case where id = ${caseId(ctx)}`) === 'RECEIVED') {
      const d = await openDialog(page, /^assign$/i);
      await choose(d.getByLabel(/^Processor/), /Arturo/i);
      await confirmDialog(page, /^assign$/i);
    }
    return page;
  },
  // 5. The findings recorded and the correction entry raised.
  'wt-c-05': async (ctx) => {
    let page = await go(ctx, 'acsl', `/acsl/cases/${caseId(ctx)}`);
    if (!has(ctx, `select 1 from acsl_correction where case_id = ${caseId(ctx)}`)) {
      if (ctx.one(`select stage from acsl_case where id = ${caseId(ctx)}`) === 'ASSIGNED') {
        await act(page, /^start investigation$/i, { reason: false, confirm: /^start investigation$/i });
        await settle(page, 1200);
      }
      if (!has(ctx, `select 1 from acsl_case where id = ${caseId(ctx)} and findings is not null`)) {
        const d = await openDialog(page, /^record findings$/i);
        await d.getByLabel(/^Findings/).fill('The insurer states the premium of the policy schedule; the booking used the premium of the quotation and the insurer corrects it on its next statement. The review also found the commission of the invoice still in Unrealized Commission although the premium is fully paid, so the commission is earned.');
        await confirmDialog(page, /^save findings$/i);
      }
      const d = await openDialog(page, /^raise correction$/i);
      await choose(d.getByLabel(/^Correction Kind/), /wrong GL account/i);
      await d.getByLabel(/^Description/).fill('Re-post the commission of the invoice to Commission Income');
      await confirmDialog(page, /^raise correction$/i);
      page = await go(ctx, 'acsl', `/acsl/cases/${caseId(ctx)}`);
    }
    return page;
  },
  // 6. The team leader assigns the correction to the processor as preparer.
  'wt-c-06': async (ctx) => {
    const id = correctionOfCase(ctx);
    const page = await go(ctx, 'acsltl', `/acsl/corrections/${id}`);
    if (ctx.one(`select stage from acsl_correction where id = ${id}`) === 'ASSIGNED') {
      const d = await openDialog(page, /^assign preparer$/i);
      await choose(d.getByLabel(/^Preparer/), /Arturo/i);
      await confirmDialog(page, /^assign preparer$/i);
    }
    return page;
  },
  // 7. The processor corrects the wrong line and submits the correction.
  'wt-c-07': async (ctx) => {
    const id = correctionOfCase(ctx);
    const page = await go(ctx, 'acsl', `/acsl/corrections/${id}`);
    if (ctx.one(`select stage from acsl_correction where id = ${id}`) === 'DRAFT') {
      if (!has(ctx, `select 1 from acsl_correction_line where correction_id = ${id}`)) {
        const card = page.locator('.card').filter({ hasText: 'Posted Lines of the Invoice Family' }).first();
        const rows = card.locator('table tbody tr');
        const unrealized = rows.filter({ hasText: 'Unrealized Commission' });
        await rowAction((await unrealized.count()) > 0 ? unrealized.first() : rows.last(), /^correct$/i);
        await dialogOf(page).getByLabel(/^Right GL Account/).fill('4400');
        await confirmDialog(page, /^add correction lines$/i);
      }
      await act(page, /^submit for review$/i, { reason: false, confirm: /^submit for review$/i });
      await settle(page, 1500);
    }
    return page;
  },
  // 8. Endorsed by the team leader.
  'wt-c-08': async (ctx) => {
    const id = correctionOfCase(ctx);
    const page = await go(ctx, 'acsltl', `/acsl/corrections/${id}`);
    if (ctx.one(`select stage from acsl_correction where id = ${id}`) === 'FOR_REVIEW') {
      await act(page, /^endorse for approval$/i, { reason: false, confirm: /^endorse for approval$/i });
      await settle(page, 1500);
    }
    return page;
  },
  // 9. Approved and posted by the ACSL Head.
  'wt-c-09': async (ctx) => {
    const id = correctionOfCase(ctx);
    const page = await go(ctx, 'acslhead', `/acsl/corrections/${id}`);
    if (ctx.one(`select stage from acsl_correction where id = ${id}`) === 'FOR_APPROVAL') {
      await act(page, /^approve and post$/i, { reason: false, confirm: /^approve and post$/i });
      await settle(page, 2500);
    }
    return page;
  },

  // ---------------------------------------------------------------- walkthrough D
  // 1. The month-end rate of the US dollar.
  'wt-d-01': async (ctx) => {
    const page = await go(ctx, 'glhead', '/setup/currencies');
    const month = previousMonth();
    if (!has(ctx, `select 1 from cur_exchange_rate where rate_type = 'CLOSING' and currency_code = 'USD' and to_char(effective_date, 'YYYY-MM') = '${month.iso}' and rate = 58.105`)) {
      await fill(page, 'Month', month.month);
      await fill(page, 'Currency', /^USD/);
      await fill(page, 'Month-end rate', '58.105000');
      await button(page, /^save rate$/i).click();
      await settle(page, 1200);
    }
    return page;
  },
  // 2. The revaluation of the previous month posted.
  'wt-d-02': async (ctx) => {
    const page = await go(ctx, 'gltl', '/planning/fx-revaluation');
    const month = previousMonth();
    await fill(page, 'Period', new RegExp(`^${month.iso}`));
    await settle(page, 1500);
    if (!has(ctx, `select 1 from fx_revaluation_run r join per_period p on p.id = r.period_id where to_char(p.start_date, 'YYYY-MM') = '${month.iso}' and r.status <> 'REVERSED'`)) {
      await act(page, /^post revaluation$/i, { reason: false, confirm: /post/i });
      await settle(page, 2000);
    }
    return page;
  },
  // 3. The close of the previous month scheduled.
  'wt-d-03': async (ctx) => {
    const page = await go(ctx, 'gltl', '/planning/gl-close');
    if (!has(ctx, "select 1 from acc_period_close_schedule where status = 'SCHEDULED'")) {
      const d = await openDialog(page, /^schedule close$/i);
      await fill(page, 'Period', new RegExp(`^${previousMonth().iso}`), d);
      await settle(page, 600);
      await fill(page, 'Close on', dateText(1), d);
      await d.getByLabel('Close time').fill('17:00');
      await settle(page, 400);
      await confirmDialog(page, /^schedule close$/i);
    }
    return page;
  },
  // 4. The cut-off of the broking books.
  'wt-d-04': async (ctx) => {
    const page = await go(ctx, 'gltl', '/planning/gl-close');
    await tab(page, 'Broking Books Cut-Off');
    return page;
  },
  // 5. A line of the service fee run tagged Released.
  'wt-d-05': async (ctx) => {
    const run = ctx.one("select id from frbs_service_fee_run where stage = 'APPROVED' order by id limit 1");
    const page = await go(ctx, 'glofficer', `/frbs/service-fee/runs/${run}`);
    if (!has(ctx, `select 1 from frbs_service_fee_line where run_id = ${run} and status = 'RELEASED'`)) {
      const row = page.locator('main table tbody tr').filter({ has: page.getByRole('button', { name: /^Actions for/ }) }).first();
      await rowAction(row, /^tag released$/i);
      await fill(page, 'Released On', dateText(0), dialogOf(page)).catch(() => {});
      await confirmDialog(page, /released/i);
    }
    return page;
  },

  // ---------------------------------------------------------------- walkthrough E
  // 1. A voucher whose debit and credit differ.
  'wt-e-01': async (ctx) => {
    const page = await go(ctx, 'glofficer', '/gl/journals/new');
    await accrualForm(page, 158000);
    return page;
  },
  // 2. A refund of the AR of walkthrough B.
  'wt-e-02': async (ctx) => {
    const page = await go(ctx, 'mktao', '/payment-requests/new-refund');
    await refundForm(page, '2026_131 Refund');
    await button(page, /^save request$/i).click();
    await settle(page, 1500);
    return page;
  },
  // 3. The statement of walkthrough C uploaded again.
  'wt-e-03': async (ctx) => {
    const page = await go(ctx, 'acsl', '/acsl/soa');
    const month = previousMonth();
    const d = await openDialog(page, /^upload soa$/i);
    await choose(d.getByLabel(/^Insurer/), /Mabuhay General/i);
    await fill(page, 'Period From', month.from, d);
    await fill(page, 'Period To', month.to, d);
    await d.locator('input[type=file]').setInputFiles(writeSoaFile(ctx));
    await page.waitForTimeout(500);
    await confirmDialog(page, /^upload and reconcile$/i, true);
    await d.locator('[role=alert]').first().waitFor({ timeout: 10000 }).catch(() => {});
    return page;
  },
  // 4. A payment request for a payee that is not maintained, waiting on No Payee.
  'wt-e-04': async (ctx) => {
    if (!has(ctx, `select 1 from dsb_request where payee_code = '${NO_PAYEE}'`)) {
      const page = await go(ctx, 'disb', '/disbursement/requests/new');
      await fill(page, 'Disbursement Type', /supplier/i);
      await fill(page, 'Payee Code', NO_PAYEE);
      await fill(page, 'Payee Name', 'Northpoint Courier Services, Inc.');
      await fill(page, 'Amount', '18450.00');
      await fill(page, 'RFP No.', 'RFP-2026-0417');
      await fill(page, 'Purpose', 'Courier services for the September policy deliveries');
      await button(page, /^save request$/i).click();
      await settle(page, 1500);
    }
    const page = await go(ctx, 'disb', '/disbursement?tab=NO_PAYEE');
    await tab(page, 'No Payee');
    return page;
  },
};

module.exports = { steps, prepare, rowAction, tick, dialogOf, confirmDialog, openDialog, choose, accrualForm,
  refundForm, tabsToTop, writeSoaFile, previousMonth, firstOfNextMonth, accrualId, refundId, caseId,
  correctionOfCase, soaUpload, varianceInvoice, ACCRUAL, REFUND_REF, NO_PAYEE };
