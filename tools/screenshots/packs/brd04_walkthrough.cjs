// End-to-end walkthroughs of BRD-04 Collections, performed live on the seed profile by capture_pack.cjs (see
// walkthroughs.yaml of the pack). Each step signs in as the persona of the step, does what the step says on the
// screen and returns the page to capture. The walkthroughs run in order on a fresh seed database (A, B, C, D, then E):
// B, D and E work on the account that A brings into the worklist. The records created carry fictitious seed values.
const { act, tab, go, button, settle } = require('./brd01_walkthrough.cjs');
const { fill, firstOption, dateText } = require('./brd02_walkthrough.cjs');

const ARN_A = 'ARN-2026-940007';
const ARN_C = 'ARN-2026-940008';
const PAYMENT_C = 'UNP-2026-000008';
const PAYMENT_E = 'UNP-2026-000007';
const RULE_B = 'CLX-AMOUNT-1M';

// ------------------------------------------------------------------ records

/** The booking invoice of an account in the Operations ledger. */
function invoiceOf(ctx, arn) {
  return ctx.one(`select invoice_no from ops_invoice where arn = '${arn}' and kind = 'BOOKING' order by id desc limit 1`);
}

const invoiceA = (ctx) => invoiceOf(ctx, ARN_A);
const invoiceC = (ctx) => invoiceOf(ctx, ARN_C);

function has(ctx, query) {
  return ctx.sql(query).length > 0;
}

function escalationOf(ctx, invoiceNo) {
  return ctx.one(`select e.id from clx_escalation e join clx_escalation_item i on i.escalation_id = e.id where i.invoice_no = '${invoiceNo}' and e.kind = 'MANUAL' order by e.id desc limit 1`);
}

function planOf(ctx, invoiceNo) {
  return ctx.one(`select id from clx_installment_plan where invoice_no = '${invoiceNo}' and status = 'ACTIVE' order by id desc limit 1`);
}

function statementOf(ctx, planId) {
  return ctx.one(`select id from clx_billing_statement where plan_id = ${planId} and cycle_seq = 1 and status <> 'CANCELLED' order by id desc limit 1`);
}

/** The two accounts of walkthrough A are booked by Processing (the steps before Collections). */
async function prepare(ctx) {
  for (const arn of [ARN_A, ARN_C]) {
    if (!has(ctx, `select 1 from ops_invoice where arn = '${arn}'`)) {
      await ctx.api('proc', 'POST', '/booking/book', { arn });
    }
  }
}

// ------------------------------------------------------------------ page helpers

const dialogOf = (page) => page.locator('dialog.modal[open]').last();

/** Runs an action of a table row: opens the row's action menu, then chooses the action. */
async function rowAction(row, name) {
  await row.getByRole('button', { name: /^Actions for/ }).click();
  await row.page().waitForTimeout(300);
  await row.page().getByRole('menuitem', { name }).first().click();
  await settle(row.page(), 600);
}

/** Ticks the check box of the table row whose text matches. */
async function tickRow(page, text) {
  const row = page.locator('main table tbody tr').filter({ hasText: text }).first();
  await row.waitFor({ timeout: 15000 });
  await row.locator('input[type=checkbox]').first().check();
  await page.waitForTimeout(400);
}

/** Searches a list for a reference (the toolbar search of the list). */
async function search(page, text) {
  await page.getByPlaceholder(/^search/i).first().fill(text);
  await page.getByRole('button', { name: /^search$/i }).first().click();
  await settle(page, 900);
}

/** Brings the tab strip of a record to the top of the window, so the tab's content and the message are captured. */
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

/** Clicks the confirming button of the open dialog and waits for the dialog to close (or to show its message). */
async function confirmDialog(page, name, keepResult = false) {
  const dialog = dialogOf(page);
  await dialog.getByRole('button', { name }).last().click();
  if (!keepResult) {
    await dialog.waitFor({ state: 'hidden', timeout: 20000 }).catch(() => {});
  }
  await settle(page, 1200);
}

function outstanding(ctx, invoiceNo) {
  return Number(ctx.one(`select net_outstanding from clx_item where invoice_no = '${invoiceNo}'`));
}

// ------------------------------------------------------------------ walkthrough A

const steps = {
  // 1. The refresh of the worklist, run on demand by the Section Head.
  'wt-a-01': async (ctx) => {
    const page = await go(ctx, 'clxuh', '/collections/setup');
    await button(page, /^refresh worklist now$/i).click();
    await page.locator('.toast-region .toast').first().waitFor({ timeout: 30000 }).catch(() => {});
    await settle(page, 1200);
    return page;
  },
  // 2. The Collection Handler's home: her new account.
  'wt-a-02': async (ctx) => go(ctx, 'clxhandler', '/collections'),
  // 3. Her accounts on the PR Worklist.
  'wt-a-03': async (ctx) => {
    const page = await go(ctx, 'clxhandler', '/collections/worklist?mine=true');
    await settle(page, 800);
    return page;
  },
  // 4. A phone call logged on the account.
  'wt-a-04': async (ctx) => {
    const invoice = invoiceA(ctx);
    const page = await go(ctx, 'clxhandler', `/collections/items/${invoice}`);
    if (!has(ctx, `select 1 from clx_effort e join clx_item i on i.id = e.item_id where i.invoice_no = '${invoice}'`)) {
      await act(page, 'Log Effort', {
        reason: false,
        confirm: /^log effort$/i,
        fill: async (d) => {
          await fill(page, 'Effort', /phone call/i, d);
          await fill(page, 'Channel', 'Mobile', d);
          await fill(page, 'Contact Person', 'Lorna Dizon, finance officer', d);
          await d.getByLabel(/^Remarks/).fill('Client will pay by check; asked for a pick-up on Friday');
        },
      });
    }
    return page;
  },
  // 5. The disposition For check pick-up with its details, handed to Cashiering.
  'wt-a-05': async (ctx) => {
    const invoice = invoiceA(ctx);
    const page = await go(ctx, 'clxhandler', `/collections/items/${invoice}`);
    const done = has(ctx, `select 1 from clx_item where invoice_no = '${invoice}' and disposition_code = 'FOR_CHECK_PICKUP'`);
    if (!done) {
      const amount = outstanding(ctx, invoice).toFixed(2);
      await act(page, 'Record Disposition', {
        reason: false,
        confirm: /^record disposition$/i,
        fill: async (d) => {
          await fill(page, 'Disposition', /check pick-up/i, d);
          await fill(page, 'Pick-up Date', dateText(2), d);
          await fill(page, 'Pick-up Address', '12th Floor, Tower One, Ayala Avenue, Makati City', d);
          await fill(page, 'Contact Person', 'Lorna Dizon', d);
          await fill(page, 'Check Amount', amount, d);
          await d.getByLabel(/^Remarks/).fill('Check ready for pick-up on Friday morning');
        },
      });
    }
    await tab(page, 'Dispositions');
    await tabsToTop(page);
    return page;
  },
  // 6. The pick-up request in Cashiering.
  'wt-a-06': async (ctx) => go(ctx, 'cashier', '/cashiering/pickups'),

  // ---------------------------------------------------------------- walkthrough B
  // 1. A promise to pay of half of the outstanding in ten days.
  'wt-b-01': async (ctx) => {
    const invoice = invoiceA(ctx);
    const page = await go(ctx, 'clxhandler', '/collections/promises');
    if (!has(ctx, `select 1 from clx_promise where invoice_no = '${invoice}'`)) {
      await button(page, /^record promise$/i).click();
      await page.waitForTimeout(600);
      const d = dialogOf(page);
      await fill(page, /^Invoice Nos?\.?/, invoice, d);
      await fill(page, 'Promised Payment Date', dateText(10), d);
      await fill(page, 'Promised Amount', (outstanding(ctx, invoice) / 2).toFixed(2), d);
      await d.getByLabel(/^Remarks/).fill('Half now, the balance with the first installment');
      await confirmDialog(page, /^record promise$/i);
    }
    return page;
  },
  // 2. The account escalated to the team lead by hand.
  'wt-b-02': async (ctx) => {
    const invoice = invoiceA(ctx);
    const page = await go(ctx, 'clxhandler', '/collections/escalations');
    if (!has(ctx, `select 1 from clx_escalation e join clx_escalation_item i on i.escalation_id = e.id where i.invoice_no = '${invoice}' and e.kind = 'MANUAL'`)) {
      await button(page, /^escalate accounts$/i).click();
      await page.waitForTimeout(600);
      const d = dialogOf(page);
      await fill(page, 'Invoice Nos.', invoice, d);
      await fill(page, 'Reason', /credit term extension/i, d);
      await d.getByLabel(/^Remarks/).fill('The insurer refused the 30-day credit term extension; client asks for installments');
      await confirmDialog(page, /^escalate$/i);
    }
    return page;
  },
  // 3. The team lead acknowledges it.
  'wt-b-03': async (ctx) => {
    const id = escalationOf(ctx, invoiceA(ctx));
    const page = await go(ctx, 'clxtl', `/collections/escalations/${id}`);
    if (ctx.one(`select status from clx_escalation where id = ${id}`) === 'WITH_TL') {
      await act(page, 'Acknowledge', { reason: false, confirm: /^acknowledge$/i, comment: 'Taken up with the account officer' });
    }
    return page;
  },
  // 4. And resolves it.
  'wt-b-04': async (ctx) => {
    const id = escalationOf(ctx, invoiceA(ctx));
    const page = await go(ctx, 'clxtl', `/collections/escalations/${id}`);
    if (ctx.one(`select status from clx_escalation where id = ${id}`) === 'IN_ACTION') {
      await button(page, /^resolve$/i).click();
      await page.waitForTimeout(600);
      await dialogOf(page).getByLabel(/^Resolution/).fill('Agreed with the client: four quarterly installments on an installment plan; promise of half the premium recorded');
      await confirmDialog(page, /^resolve$/i);
    }
    return page;
  },
  // 5. The Section Head authorizes the rule set up by the Business Administrator.
  'wt-b-05': async (ctx) => {
    const page = await go(ctx, 'clxuh', '/collections/escalation-rules');
    if (ctx.one(`select record_status from clx_escalation_rule where code = '${RULE_B}'`) === 'PENDING_AUTHORIZATION') {
      await rowAction(page.locator('main table tbody tr').filter({ hasText: RULE_B }).first(), /^authorize$/i);
      await confirmDialog(page, /^authorize$/i);
    }
    return page;
  },

  // ---------------------------------------------------------------- walkthrough C
  // 1. The Unapplied Payment Handler requests the application of a payment not matched.
  'wt-c-01': async (ctx) => {
    const page = await go(ctx, 'upphandler', '/collections/unapplied');
    if (!has(ctx, `select 1 from clx_application_request where unapplied_ref = '${PAYMENT_C}'`)) {
      await search(page, PAYMENT_C);
      await tickRow(page, PAYMENT_C);
      await button(page, /^request application$/i).click();
      await page.waitForTimeout(600);
      const d = dialogOf(page);
      await fill(page, 'Invoice No.', invoiceC(ctx), d);
      await d.getByLabel(/^Remarks/).fill('Payor confirmed by e-mail that the payment is for this invoice');
      await confirmDialog(page, /^request application$/i);
    }
    return page;
  },
  // 2. The request on Requests to Cashiering.
  'wt-c-02': async (ctx) => go(ctx, 'upphandler', '/collections/unapplied/requests'),
  // 3. Cashiering accepts it and applies the payment at once.
  'wt-c-03': async (ctx) => {
    const page = await go(ctx, 'cashier', '/cashiering/requests');
    const status = ctx.one(`select status from clx_application_request where unapplied_ref = '${PAYMENT_C}' order by id desc limit 1`);
    if (['SENT', 'DEFERRED'].includes(status)) {
      const ref = ctx.one(`select cashiering_ref from clx_application_request where unapplied_ref = '${PAYMENT_C}' order by id desc limit 1`);
      await tickRow(page, ref);
      await button(page, /^accept request$/i).click();
      await page.waitForTimeout(600);
      const box = dialogOf(page).getByRole('checkbox');
      if ((await box.count()) > 0 && !(await box.first().isChecked())) {
        await box.first().check();
      }
      await confirmDialog(page, /^accept request$/i);
    }
    return page;
  },
  // 4. The request executed.
  'wt-c-04': async (ctx) => {
    const page = await go(ctx, 'upphandler', '/collections/unapplied/requests');
    await tab(page, 'Executed');
    return page;
  },
  // 5. The history of the payment.
  'wt-c-05': async (ctx) => {
    const page = await go(ctx, 'upphandler', `/collections/unapplied/${PAYMENT_C}`);
    await tab(page, 'History');
    return page;
  },

  // ---------------------------------------------------------------- walkthrough D
  // 1. An installment plan of four quarterly installments on the invoice of walkthrough A.
  'wt-d-01': async (ctx) => {
    const invoice = invoiceA(ctx);
    if (has(ctx, `select 1 from clx_installment_plan where invoice_no = '${invoice}' and status = 'ACTIVE'`)) {
      return go(ctx, 'clxhandler', `/collections/plans/${planOf(ctx, invoice)}`);
    }
    const page = await go(ctx, 'clxhandler', '/collections/plans');
    await button(page, /^new installment plan$/i).click();
    await page.waitForTimeout(600);
    const d = dialogOf(page);
    await fill(page, 'Plan Basis', /one invoice/i, d);
    await fill(page, 'Invoice No.', invoice, d);
    await fill(page, 'First Due Date', dateText(15), d);
    await fill(page, 'Number of Installments', '4', d);
    await fill(page, 'Billing Frequency', /^quarterly$/i, d);
    await d.getByLabel(/^Remarks/).fill('Quarterly installments agreed with the client after the escalation');
    await confirmDialog(page, /^create plan$/i);
    await page.waitForURL(/\/collections\/plans\/\d+/, { timeout: 20000 }).catch(() => {});
    await settle(page, 1200);
    return page;
  },
  // 2. The statement of account of cycle 1.
  'wt-d-02': async (ctx) => {
    const plan = planOf(ctx, invoiceA(ctx));
    const page = await go(ctx, 'clxhandler', `/collections/plans/${plan}`);
    if (!has(ctx, `select 1 from clx_billing_statement where plan_id = ${plan} and cycle_seq = 1 and status <> 'CANCELLED'`)) {
      await rowAction(page.locator('main table tbody tr').first(), /^generate soa$/i);
      await settle(page, 1200);
    }
    await tab(page, 'Statements of Account');
    await tabsToTop(page);
    return page;
  },
  // 3. Send via Email: the e-mail proposed.
  'wt-d-03': async (ctx) => {
    const id = statementOf(ctx, planOf(ctx, invoiceA(ctx)));
    const page = await go(ctx, 'clxhandler', `/collections/billing/${id}`);
    if (ctx.one(`select status from clx_billing_statement where id = ${id}`) !== 'SENT') {
      await button(page, /^send via email$/i).click();
      await page.waitForTimeout(1200);
      const to = dialogOf(page).getByLabel(/^To/).first();
      if ((await to.inputValue()) === '') {
        await to.fill('accounts.payable@garcia-family.example');
      }
    }
    return page;
  },
  // 4. Sent.
  'wt-d-04': async (ctx) => {
    const id = statementOf(ctx, planOf(ctx, invoiceA(ctx)));
    const page = await ctx.pageOf('clxhandler');
    if (await dialogOf(page).isVisible().catch(() => false)) {
      await confirmDialog(page, /^send$/i);
      return page;
    }
    return go(ctx, 'clxhandler', `/collections/billing/${id}`);
  },

  // ---------------------------------------------------------------- walkthrough E
  // 1. A disposition reserved to other roles, refused for the account officer.
  'wt-e-01': async (ctx) => {
    const page = await go(ctx, 'ao', '/collections/items/BI-HO-2026-000002');
    await button(page, /^record disposition$/i).click();
    await page.waitForTimeout(600);
    const d = dialogOf(page);
    await fill(page, 'Disposition', /policy number/i, d);
    await confirmDialog(page, /^record disposition$/i, true);
    await d.locator('[role=alert]').first().waitFor({ timeout: 10000 }).catch(() => {});
    return page;
  },
  // 2. The account another user is working on.
  'wt-e-02': async (ctx) => {
    const invoice = invoiceA(ctx);
    await ctx.api('clxhandler', 'POST', `/collections/items/${invoice}/lock`);
    return go(ctx, 'clxtl', `/collections/items/${invoice}`);
  },
  // 3. A promise above the outstanding.
  'wt-e-03': async (ctx) => {
    const page = await go(ctx, 'clxhandler', '/collections/promises');
    await button(page, /^record promise$/i).click();
    await page.waitForTimeout(600);
    const d = dialogOf(page);
    await fill(page, /^Invoice Nos?\.?/, 'BI-HO-2026-000004', d);
    await fill(page, 'Promised Payment Date', dateText(7), d);
    await fill(page, 'Promised Amount', '950000', d);
    await confirmDialog(page, /^record promise$/i, true);
    await settle(page, 800);
    return page;
  },
  // 4. An invoice number in the wrong format.
  'wt-e-04': async (ctx) => {
    const page = await go(ctx, 'upphandler', '/collections/unapplied');
    await search(page, PAYMENT_E);
    await tickRow(page, PAYMENT_E);
    await button(page, /^request application$/i).click();
    await page.waitForTimeout(600);
    const d = dialogOf(page);
    await fill(page, 'Invoice No.', 'INV 2026/0417', d);
    await confirmDialog(page, /^request application$/i, true);
    return page;
  },
};

module.exports = { steps, prepare, invoiceA, invoiceC, rowAction, tickRow, search, tabsToTop, dialogOf, confirmDialog,
  firstOption, PAYMENT_C };
