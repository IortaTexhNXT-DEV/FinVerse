// End-to-end walkthroughs of BRD-02 Operations, performed live on the seed profile by capture_pack.cjs (see
// walkthroughs.yaml of the pack). Each step signs in as the persona of the step, does what the step says on the
// screen and returns the page to capture. The walkthroughs run in order on a fresh seed database: B cancels the invoice
// that A books, pays and remits. The records created carry fictitious seed values only.
const fs = require('fs');
const path = require('path');
const { act, tab, go, button, settle, csv, TMP } = require('./brd01_walkthrough.cjs');

const ARN_A = 'ARN-2026-940007';

// ------------------------------------------------------------------ helpers

const sqlText = (s) => s.replace(/'/g, "''");

/** The invoice of walkthrough A (booked in step A-01). */
function invoiceA(ctx) {
  return ctx.one(`select invoice_no from ops_invoice where arn = '${ARN_A}' and kind = 'BOOKING' order by id desc limit 1`);
}

function batchOf(ctx, invoiceNo) {
  return ctx.one(`select b.id from rem_batch b join rem_batch_line l on l.batch_id = b.id where l.invoice_no = '${invoiceNo}' order by b.id desc limit 1`);
}

function requestB(ctx) {
  return ctx.one(`select id from adj_request where invoice_no = '${invoiceA(ctx)}' order by id desc limit 1`);
}

/** Types into the field of the label (select: option matching the text; date and text: typed). */
async function fill(page, label, value, scope) {
  const root = scope || page;
  const field = root.getByLabel(label instanceof RegExp ? label : new RegExp(`^${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\*?$`)).first();
  await field.waitFor({ state: 'visible', timeout: 15000 });
  const tag = await field.evaluate((e) => e.tagName.toLowerCase());
  if (tag === 'select') {
    const options = await field.locator('option').allTextContents();
    const hit = options.find((o) => (value instanceof RegExp ? value : new RegExp(value, 'i')).test(o));
    if (hit === undefined) {
      throw new Error(`option ${value} not in ${label}: ${options.slice(0, 12).join(', ')}`);
    }
    await field.selectOption({ label: hit });
  } else {
    await field.fill('');
    await field.pressSequentially(String(value), { delay: 5 });
    await field.press('Tab').catch(() => {});
  }
  await page.waitForTimeout(200);
}

/** Opens the first option other than the empty one of a select in a dialog. */
async function firstOption(select) {
  const options = await select.locator('option').allTextContents();
  const pick = options.find((o, i) => i > 0 && o.trim() !== '');
  if (pick) {
    await select.selectOption({ label: pick });
  }
}

/** Ticks the check box of the table row whose text matches. */
async function tickRow(page, text) {
  const row = page.locator('table tbody tr').filter({ hasText: text }).first();
  await row.waitFor({ timeout: 15000 });
  await row.locator('input[type=checkbox]').first().check();
  await page.waitForTimeout(400);
}

async function openRow(page, text) {
  const row = page.locator('table tbody tr').filter({ hasText: text }).first();
  await row.waitFor({ timeout: 15000 });
  await row.click();
  await settle(page, 1200);
}

/** The date `days` from today as dd-MMM-yyyy (Philippine business dates of the seed stack). */
const MANILA_OFFSET_MS = 8 * 3600000;

function dateText(days) {
  // The business date is the Philippine date (UTC+8), also in the evening UTC.
  const d = new Date(Date.now() + MANILA_OFFSET_MS + days * 86400000);
  const m = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][d.getUTCMonth()];
  return `${String(d.getUTCDate()).padStart(2, '0')}-${m}-${d.getUTCFullYear()}`;
}

function isoDate(days) {
  return new Date(Date.now() + MANILA_OFFSET_MS + days * 86400000).toISOString().slice(0, 10);
}


/** Brings the tab strip of a record to the top of the window, so the tab's content is captured whole. */
/** Runs an action of a table row: opens the row's action menu, then chooses the action. */
async function rowAction(row, name) {
  await row.getByRole('button', { name: /^Actions for/ }).click();
  await row.page().waitForTimeout(300);
  await row.page().getByRole('menuitem', { name }).first().click();
}

async function tabsToTop(page) {
  // The capture shows the window from the top: the record header and the stepper above the tabs are set aside
  // (they are in the other steps of the walkthrough) so that the tab's rows fit in the window.
  await page.evaluate(() => {
    const t = document.querySelector('main div.tabs[role=tablist]');
    if (!t) return;
    for (const el of t.parentElement.children) {
      if (el === t) break;
      el.style.display = 'none';
    }
  });
  await page.waitForTimeout(400);
}

// ------------------------------------------------------------------ walkthrough A

const steps = {
  // 1. Processing books the account; the booked invoice reaches the Operations ledger.
  'wt-a-01': async (ctx) => {
    const booked = ctx.sql(`select 1 from ops_invoice where arn = '${ARN_A}'`).length > 0;
    if (booked) {
      const id = ctx.one(`select id from bkg_invoice where arn = '${ARN_A}' and status = 'BOOKED' order by id desc limit 1`);
      return go(ctx, 'proc', `/booking/invoices/${id}`);
    }
    const page = await go(ctx, 'proc', `/booking/book/${ARN_A}`);
    await act(page, 'Book Account', { reason: false, confirm: /^Book/ });
    await settle(page, 2500);
    return page;
  },
  // 2. The invoice on Invoice Search.
  'wt-a-02': async (ctx) => {
    const page = await go(ctx, 'cashier', `/operations/invoices?q=${ARN_A}`);
    await settle(page, 1200);
    return page;
  },
  // 3. Receive Payment with the live preview (the payment dated a week back so that remittance may extract it).
  'wt-a-03': async (ctx) => {
    const invoice = invoiceA(ctx);
    const gross = ctx.one(`select gross_premium from ops_invoice where invoice_no = '${invoice}'`);
    const page = await go(ctx, 'cashier', '/cashiering/receive');
    await fill(page, 'ARN, Invoice, Policy or PN No.', ARN_A);
    await fill(page, 'Payor Name', 'Antonio Luis D. Garcia');
    await fill(page, 'Assured Name', 'Garcia, Antonio Luis Dizon');
    await fill(page, 'Amount', gross);
    await fill(page, 'Payment Date', dateText(-7));
    await settle(page, 2000);
    ctx.state.paymentPage = page;
    return page;
  },
  // 4. Issue AR and Apply: the receipt with its application.
  'wt-a-04': async (ctx) => {
    const page = ctx.state.paymentPage || (await steps['wt-a-03'](ctx));
    await button(page, /^issue ar and apply$/i).click();
    await page.waitForURL(/\/cashiering\/receipts\/\d+/, { timeout: 20000 });
    await settle(page, 1500);
    return page;
  },
  // 5. Extraction of the invoice into a batch; the run with its tag.
  'wt-a-05': async (ctx) => {
    const invoice = invoiceA(ctx);
    const page = await go(ctx, 'remit', '/remittance/extraction');
    if (ctx.sql(`select 1 from rem_batch_line where invoice_no = '${invoice}'`).length === 0) {
      await fill(page, 'Invoice No.', invoice);
      await button(page, /^run extraction$/i).last().click();
      await settle(page, 2500);
    }
    await page.locator('table tbody tr').first().click();
    await settle(page, 1200);
    return page;
  },
  // 6. The batch submitted for approval.
  'wt-a-06': async (ctx) => {
    const id = batchOf(ctx, invoiceA(ctx));
    const page = await go(ctx, 'remit', `/remittance/batches/${id}`);
    if (ctx.one(`select stage from rem_batch where id = ${id}`) === 'REVIEW_IN_PROCESS') {
      await act(page, 'Preview and Submit', { confirm: /^submit for approval$/i, comment: 'Amounts checked against the ledger' });
      await settle(page, 1500);
    }
    return page;
  },
  // 7. Approved by the Remittance Team Leader and pushed to Disbursement.
  'wt-a-07': async (ctx) => {
    const id = batchOf(ctx, invoiceA(ctx));
    const page = await go(ctx, 'remittl', `/remittance/batches/${id}`);
    if (ctx.one(`select stage from rem_batch where id = ${id}`) === 'FOR_APPROVAL') {
      await act(page, 'Approve and Push', { confirm: /^approve and push$/i, comment: 'Approved' });
      await settle(page, 2000);
      if (ctx.one(`select stage from rem_batch where id = ${id}`) === 'FOR_APPROVAL') {
        const message = await page.locator('dialog.modal[open] [role=alert]').allInnerTexts().catch(() => []);
        throw new Error(`batch ${id} not approved ${message.join(' ')}`);
      }
    }
    return page;
  },
  // 8. Disbursement acknowledges the payment request and assigns the DV number.
  'wt-a-08': async (ctx) => {
    const batchNo = ctx.one(`select batch_no from rem_batch where id = ${batchOf(ctx, invoiceA(ctx))}`);
    const requestNo = ctx.one(`select request_no from ops_disbursement_request where source_ref like '%${batchNo}%' order by id desc limit 1`);
    const page = await go(ctx, 'disb', '/operations/disbursements');
    const status = ctx.one(`select status from ops_disbursement_request where request_no = '${requestNo}'`);
    if (status === 'SENT') {
      const row = page.locator('table tbody tr').filter({ hasText: requestNo }).first();
      await rowAction(row, /^acknowledge$/i);
      await settle(page, 1500);
    }
    if (ctx.one(`select status from ops_disbursement_request where request_no = '${requestNo}'`) === 'ACKNOWLEDGED') {
      await tab(page, 'Acknowledged');
      const row = page.locator('table tbody tr').filter({ hasText: requestNo }).first();
      await rowAction(row, /^assign dv/i);
      await page.waitForTimeout(500);
      const dialog = page.locator('dialog.modal[open]').last();
      await dialog.getByLabel(/^Disbursement Voucher No\./).fill('DV-2026-900101');
      await dialog.getByRole('button', { name: /^assign dv number$/i }).click();
      await settle(page, 1500);
    }
    await tab(page, 'DV Assigned');
    return page;
  },
  // 9. The insurer's OR schedule uploaded: the line is matched and the batch closed.
  'wt-a-09': async (ctx) => {
    const invoice = invoiceA(ctx);
    const id = batchOf(ctx, invoice);
    const batchNo = ctx.one(`select batch_no from rem_batch where id = ${id}`);
    const paid = ctx.one(`select paid_ar from rem_batch_line where batch_id = ${id} and invoice_no = '${invoice}'`);
    const page = await go(ctx, 'remit', '/remittance/insurer-or');
    if (ctx.sql(`select 1 from rem_batch_line where batch_id = ${id} and insurer_or_no is not null`).length === 0) {
      const file = csv(`MGIC_OR_${batchNo}.csv`, [['Batch No.', 'Invoice No.', 'OR No.', 'OR Date', 'OR Amount'],
        [batchNo, invoice, 'MGIC-OR-2026-900101', dateText(0), paid]]);
      await page.locator('input[type=file]').first().setInputFiles(file);
      await page.waitForTimeout(500);
      await button(page, /^upload file$/i).click();
      await settle(page, 2500);
    }
    await page.waitForTimeout(6000);
    return page;
  },
  // 10. Invoice 360: paid and fully remitted, with the batch on the Remittances tab.
  'wt-a-10': async (ctx) => {
    const page = await go(ctx, 'remit', `/operations/invoices/${invoiceA(ctx)}`);
    await tab(page, 'Remittances');
    await page.locator('main section.card table tbody tr').first().waitFor({ timeout: 15000 });
    await settle(page, 800);
    await tabsToTop(page);
    return page;
  },

  // ---------------------------------------------------------------- walkthrough B
  // 1. The policy chosen on the New Endorsement Request.
  'wt-b-01': async (ctx) => {
    const invoice = invoiceA(ctx);
    const policy = ctx.one(`select policy_no from ops_invoice where invoice_no = '${invoice}'`);
    const page = await go(ctx, 'mktcoll', '/adjustment/new');
    await page.getByPlaceholder('Search policy, ARN, invoice or client').fill(policy);
    await button(page, /^search$/i).click();
    await settle(page, 1200);
    const pick = page.getByLabel(`Select ${invoice}`);
    if (await pick.isDisabled()) {
      // Walkthrough B starts from the remitted invoice of walkthrough A: an invoice still in a batch is locked.
      const lock = await page.locator('table tbody tr').filter({ hasText: invoice }).first().innerText();
      throw new Error(`invoice ${invoice} cannot be chosen (run walkthrough A to the end first): ${lock.replace(/\s+/g, ' ')}`);
    }
    await pick.check();
    await settle(page, 1500);
    ctx.state.requestPage = page;
    return page;
  },
  // 2. The request with its recompute.
  'wt-b-02': async (ctx) => {
    const page = ctx.state.requestPage || (await steps['wt-b-01'](ctx));
    const inception = ctx.one(`select to_char(inception_date, 'DD-Mon-YYYY') from ops_invoice where invoice_no = '${invoiceA(ctx)}'`);
    await button(page, /^next$/i).click();
    await settle(page, 800);
    await fill(page, 'Endorsement Type', /Change of Cover/);
    await fill(page, 'Request Type', /^Flat Cancellation$/);
    await firstOption(page.getByLabel(/^Reason for Cancellation/).first());
    await fill(page, 'Effective Date', inception);
    await fill(page, 'Description', 'Flat cancellation from inception: the client sold the insured property before the policy took effect');
    await button(page, /^recompute$/i).click();
    await settle(page, 2500);
    return page;
  },
  // 3. Submitted for validation: the request page.
  'wt-b-03': async (ctx) => {
    const page = ctx.state.requestPage || (await steps['wt-b-02'](ctx));
    await button(page, /^submit for validation$/i).click();
    await page.waitForURL(/\/adjustment\/requests\/\d+/, { timeout: 20000 });
    await settle(page, 1500);
    return page;
  },
  // 4. Validated by Adjustment.
  'wt-b-04': async (ctx) => {
    const id = requestB(ctx);
    const page = await go(ctx, 'adjust', `/adjustment/requests/${id}`);
    if (ctx.one(`select stage from adj_request where id = ${id}`) === 'FOR_VALIDATION') {
      await act(page, 'Validate', { reason: false });
      await settle(page, 1500);
    }
    return page;
  },
  // 5. Approved by the Adjustment Team Leader.
  'wt-b-05': async (ctx) => {
    const id = requestB(ctx);
    const page = await go(ctx, 'adjtl', `/adjustment/requests/${id}`);
    if (ctx.one(`select stage from adj_request where id = ${id}`) === 'FOR_APPROVAL') {
      await act(page, 'Approve', { reason: false });
      await settle(page, 1500);
    }
    return page;
  },
  // 6. Posted in a posting batch.
  'wt-b-06': async (ctx) => {
    const requestNo = ctx.one(`select request_no from adj_request where id = ${requestB(ctx)}`);
    const page = await go(ctx, 'adjust', '/adjustment/batches');
    if (ctx.one(`select stage from adj_request where request_no = '${requestNo}'`) === 'FOR_POSTING') {
      await tickRow(page, requestNo);
      await act(page, 'Post Selected', { reason: false, confirm: /^post batch$/i, keepResult: true });
      await settle(page, 2500);
    } else {
      await tab(page, 'Posted Batches');
    }
    return page;
  },
  // 7. The Policy Transactions of the request with the journals of the cancellation.
  'wt-b-07': async (ctx) => {
    const page = await go(ctx, 'adjust', `/adjustment/requests/${requestB(ctx)}`);
    await tab(page, 'Policy Transactions');
    const toggles = page.locator('main button[aria-expanded]');
    const n = await toggles.count();
    if (n > 0) {
      await toggles.nth(n - 1).click();
      await settle(page, 800);
    }
    // Opening a journal scrolls the wide table: show it from its first column.
    await page.evaluate(() => document.querySelectorAll('main *').forEach((e) => { if (e.scrollLeft) e.scrollLeft = 0; }));
    await tabsToTop(page);
    return page;
  },
  // 8. The unapplied payment of the re-application: refund assigned and submitted.
  'wt-b-08': async (ctx) => {
    const id = ctx.one(`select id from csh_unapplied where invoice_no = '${invoiceA(ctx)}' or source_ref like 'ADJ:%${ctx.one(`select request_no from adj_request where id = ${requestB(ctx)}`)}%' order by id desc limit 1`);
    ctx.state.refundItem = id;
    const page = await go(ctx, 'cashier', `/cashiering/unapplied/${id}`);
    const stage = ctx.one(`select stage from csh_unapplied where id = ${id}`);
    if (stage === 'UNAPPLIED') {
      await fill(page, 'Disposition Type', /refund/i);
      await settle(page, 400);
      await button(page, /^save disposition$/i).click();
      await settle(page, 1500);
    }
    if (ctx.one(`select stage from csh_unapplied where id = ${id}`) === 'MONITORING') {
      await act(page, 'Submit for Approval', { reason: false });
      await settle(page, 1500);
    }
    return page;
  },
  // 9. The refund approved by the Cashiering Team Leader.
  'wt-b-09': async (ctx) => {
    const id = ctx.state.refundItem || ctx.one(`select id from csh_unapplied where invoice_no = '${invoiceA(ctx)}' order by id desc limit 1`);
    const page = await go(ctx, 'cashtl', `/cashiering/unapplied/${id}`);
    if (ctx.one(`select stage from csh_unapplied where id = ${id}`) === 'FOR_APPROVAL') {
      await act(page, 'Approve', { reason: false });
      await settle(page, 1800);
    }
    return page;
  },
  // 10. The refund in the Disbursement queue.
  'wt-b-10': async (ctx) => go(ctx, 'disb', '/operations/disbursements'),

  // ---------------------------------------------------------------- walkthrough C
  // 1. The insurer's approval recorded on the DP billing.
  'wt-c-01': async (ctx) => {
    const id = ctx.one("select id from cmr_billing where billing_no = 'CRB-2026-000001'");
    const page = await go(ctx, 'commrec', `/commission/dp/billings/${id}`);
    if (ctx.one(`select stage from cmr_billing where id = ${id}`) === 'AWAITING_INSURER') {
      await button(page, /^record answers$/i).click();
      await page.waitForTimeout(600);
      const dialog = page.locator('dialog.modal[open]').last();
      await dialog.getByRole('button', { name: /^approve all$/i }).click();
      await page.waitForTimeout(300);
      await dialog.getByRole('button', { name: /^save answers$/i }).click();
      await settle(page, 1800);
    }
    return page;
  },
  // 2. The commission collected and the premium receivable reversed.
  'wt-c-02': async (ctx) => {
    const id = ctx.one("select id from cmr_billing where billing_no = 'CRB-2026-000001'");
    const page = await go(ctx, 'commrec', `/commission/dp/billings/${id}`);
    if (ctx.one(`select stage from cmr_billing where id = ${id}`) === 'APPROVED') {
      await button(page, /^record collection$/i).click();
      await page.waitForTimeout(600);
      const dialog = page.locator('dialog.modal[open]').last();
      const bank = dialog.getByLabel(/^Bank Account/).first();
      if ((await bank.evaluate((e) => e.tagName.toLowerCase())) === 'select' && !(await bank.inputValue())) {
        await firstOption(bank);
      }
      await dialog.locator('.modal-footer button, footer button').last().click();
      await settle(page, 2000);
    }
    return page;
  },
  // 3. Invoice 360 of the direct payment invoice.
  'wt-c-03': async (ctx) => {
    const page = await go(ctx, 'commrec', '/operations/invoices/BI-HO-2026-000003');
    await tab(page, 'Movements');
    await settle(page, 800);
    await tabsToTop(page);
    return page;
  },
  // 4. Feedback and disposition of the item with a premium difference.
  'wt-c-04': async (ctx) => {
    const id = ctx.one("select id from prc_cycle order by id limit 1");
    const page = await go(ctx, 'recon', `/prodrecon/cycles/${id}`);
    await tab(page, 'With Discrepancy');
    await page.getByRole('table', { name: 'Reconciliation items' }).locator('tbody tr').first().click();
    await page.waitForTimeout(800);
    const dialog = page.locator('dialog.modal[open]').last();
    const selects = dialog.locator('select');
    for (let i = 0; i < Math.min(2, await selects.count()); i += 1) {
      await firstOption(selects.nth(i));
    }
    const feedback = dialog.getByLabel(/^Insurer Feedback/).first();
    if ((await feedback.count()) > 0) {
      await feedback.fill('Insurer billed the premium before the rate correction; revised invoice to follow');
    }
    ctx.state.reviewDialog = true;
    return page;
  },
  // 5. The disposition of the insurer-only item.
  'wt-c-05': async (ctx) => {
    const id = ctx.one("select id from prc_cycle order by id limit 1");
    const page = await go(ctx, 'recon', `/prodrecon/cycles/${id}`);
    await tab(page, 'With Discrepancy');
    await page.getByRole('table', { name: 'Reconciliation items' }).locator('tbody tr').first().click();
    await page.waitForTimeout(800);
    let dialog = page.locator('dialog.modal[open]').last();
    const selects = dialog.locator('select');
    for (let i = 0; i < Math.min(2, await selects.count()); i += 1) {
      await firstOption(selects.nth(i));
    }
    await dialog.getByRole('button', { name: /^save feedback$/i }).click();
    await settle(page, 1200);
    await tab(page, 'Insurer Only');
    const row = page.getByRole('table', { name: 'Reconciliation items' }).locator('tbody tr').first();
    await row.locator('input[type=checkbox]').first().check();
    await button(page, /^set disposition$/i).click();
    await page.waitForTimeout(600);
    dialog = page.locator('dialog.modal[open]').last();
    const sel = dialog.locator('select');
    for (let i = 0; i < await sel.count(); i += 1) {
      await firstOption(sel.nth(i));
    }
    await dialog.getByRole('button', { name: /^apply to selected$/i }).click();
    await settle(page, 1500);
    await tab(page, 'Insurer Only');
    await tabsToTop(page);
    return page;
  },
  // 6. Unbooked Accounts.
  'wt-c-06': async (ctx) => go(ctx, 'recon', '/prodrecon/unbooked'),

  // ---------------------------------------------------------------- walkthrough D
  // 1. A hold raised by Marketing Collection and approved by the Marketing Team Leader.
  'wt-d-01': async (ctx) => {
    const invoice = 'I97000011';
    const live = `select id from rem_hold_request where invoice_no = '${invoice}' order by id desc limit 1`;
    if (ctx.sql(live).length === 0) {
      const page = await go(ctx, 'mktcoll', '/remittance/holds');
      await button(page, /^new hold request$/i).click();
      await settle(page, 600);
      const dialog = page.locator('dialog.modal[open]').last();
      await fill(page, 'Invoice No.', invoice, dialog);
      await firstOption(dialog.getByLabel(/^Reason/).first());
      await fill(page, 'Hold Until', dateText(14), dialog);
      await fill(page, 'Remarks', 'Client disputes the premium; hold until the account officer confirms', dialog);
      await dialog.getByRole('button', { name: /^submit hold request$/i }).click();
      await settle(page, 1500);
    }
    const id = ctx.one(live);
    const page = await go(ctx, 'mkttl', `/remittance/holds/${id}`);
    if (ctx.one(`select stage from rem_hold_request where id = ${id}`) === 'FOR_APPROVAL') {
      await act(page, 'Approve hold', { reason: false, comment: 'Confirmed with the account officer' });
      await settle(page, 1500);
    }
    return page;
  },
  // 2. A request refused on an invoice locked by Remittance.
  'wt-d-02': async (ctx) => {
    const invoice = ctx.one("select l.invoice_no from rem_batch_line l join rem_batch b on b.id = l.batch_id where b.stage in ('REVIEW_IN_PROCESS', 'FOR_APPROVAL') order by b.id limit 1");
    const page = await go(ctx, 'mktcoll', '/adjustment/new');
    await page.getByPlaceholder('Search policy, ARN, invoice or client').fill(invoice);
    await button(page, /^search$/i).click();
    await settle(page, 1200);
    return page;
  },
  // 3. The cashier asks for the cancellation of a receipt.
  'wt-d-03': async (ctx) => {
    const id = ctx.one("select id from csh_receipt where receipt_no = 'AR-HO-000009'");
    const page = await go(ctx, 'cashier', `/cashiering/receipts/${id}`);
    if (ctx.sql(`select 1 from csh_receipt_action where receipt_id = ${id}`).length === 0) {
      await act(page, 'Cancel Receipt', { confirm: /^submit cancellation$/i, comment: 'Payment received twice; the payor keeps the first receipt' });
      await settle(page, 1500);
    }
    return page;
  },
  // 4. The Cashiering Team Leader approves it.
  'wt-d-04': async (ctx) => {
    const id = ctx.one("select id from csh_receipt where receipt_no = 'AR-HO-000009'");
    const page = await go(ctx, 'cashtl', `/cashiering/receipts/${id}`);
    const stage = ctx.sql(`select stage from csh_receipt_action where receipt_id = ${id} order by id desc limit 1`)[0]?.[0];
    if (stage === 'FOR_APPROVAL') {
      await act(page, 'Approve and Post', { reason: false });
      await settle(page, 1800);
    }
    return page;
  },
  // 5. The submitter of a batch tries to approve it.
  'wt-d-05': async (ctx) => {
    const id = ctx.one("select id from rem_batch where batch_no = 'RMB-INS-MGIC-2026-000002'");
    const page = await go(ctx, 'remittl', `/remittance/batches/${id}`);
    if (ctx.one(`select stage from rem_batch where id = ${id}`) === 'REVIEW_IN_PROCESS') {
      await act(page, 'Preview and Submit', { confirm: /^submit for approval$/i });
      await settle(page, 1500);
    }
    await act(page, 'Approve and Push', { confirm: /^approve and push$/i });
    await settle(page, 1500);
    return page;
  },
};

async function prepare() {
  fs.mkdirSync(TMP, { recursive: true });
}

module.exports = { steps, prepare, fill, tickRow, openRow, firstOption, dateText, isoDate, invoiceA, sqlText, path };
