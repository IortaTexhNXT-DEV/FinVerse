// How capture_pack.cjs reaches each screen state of the BRD-06 Renewal sign-off pack on the seed profile: the renewal
// to open for a stage (seed database), what is typed into the forms (fictitious seed values), the rows selected, the
// walkthrough steps and the generated documents. On a fresh seed database run '^wt-(a|b)-' first, then
// '^(scr|doc|ux)-', then '^wt-(c|d|e)-': the screens show the renewal that walkthroughs A and B take to the client
// before it is accepted.
const walkthrough = require('./brd06_walkthrough.cjs');
const { render } = require('./brd01_documents.cjs');
const { settle } = require('./brd01_walkthrough.cjs');
const { fill, dateText } = require('./brd02_walkthrough.cjs');
const { dialogOf, tickRow } = require('./brd04_walkthrough.cjs');

const { refA, renewalOf, record, company } = walkthrough;

// ------------------------------------------------------------------ records to open

/** A renewal in a stage, the first one by its number. */
function inStage(ctx, stage) {
  return ctx.one(`select renewal_ref from rnw_candidate where stage = '${stage}' order by id limit 1`);
}

const opens = {
  // The migrated corporate policy the Account Officer disposed in the seed data, waiting for the Team Leader.
  'open_renewal:review': (ctx) => record(renewalOf(ctx, 'EBIX-CG-0100004')),
  // A renewal assigned to the Account Officer and not yet disposed (assigned here when the walkthroughs have not).
  'open_renewal:for_disposition': async (ctx) => {
    const ref = renewalOf(ctx, 'QPS-FI-0100002');
    if (ctx.sql(`select 1 from rnw_candidate where renewal_ref = '${ref}' and stage = 'UNASSIGNED'`).length) {
      await ctx.api('mkttl', 'POST', '/renewal/candidates/assign', { companyId: company(ctx), renewalRefs: [ref], ao: 'ao' });
    }
    return record(ref);
  },
  // The renewal of walkthroughs A and B: its renewal account and its letters.
  'open_renewal:processing': (ctx) => record(refA(ctx)),
  'open_renewal:ra_sent': (ctx) => record(refA(ctx)),
  'open_renewal:with_insurer': (ctx) => record(inStage(ctx, 'WITH_INSURER')),
};

// ------------------------------------------------------------------ forms

/** Ticks the first rows of the list on screen. */
async function tickRows(page, n) {
  const rows = page.locator('main table tbody tr');
  await rows.first().waitFor({ timeout: 15000 });
  for (let i = 0; i < n; i += 1) {
    await rows.nth(i).locator('input[type=checkbox]').first().check();
    await page.waitForTimeout(300);
  }
}

const opening = (button, rows) => async (page, ctx) => {
  if (rows) {
    await selects[rows](page, ctx);
    await settle(page, 300);
  }
  await ctx.clickButton(page, button);
};
const inDialog = (label, value) => async (page) => fill(page, label, value, dialogOf(page));
const remarks = (text) => async (page) => dialogOf(page).getByLabel(/^Remarks/).fill(text);

const fills = {
  generate_range: [
    opening('^generate expiry list$'),
    inDialog('Expiry From', dateText(330)),
    inDialog('Expiry To', dateText(360)),
  ],
  assign_officer: [
    opening('^assign disposition$', 'first_renewal'),
    inDialog('Account Officer', /Aileen/),
  ],
  disposition_nfr: [
    opening('^set disposition$'),
    inDialog('Disposition', /^Not for Renewal$/),
    inDialog('Reason for Not for Renewal', /Unit Sold/),
    remarks('Client sold the vehicle in August'),
  ],
  override_balance: [
    opening('^override$', 'first_renewal'),
    inDialog('Override', /^Outstanding balance$/),
    inDialog('Reason', /Payment arranged/),
    remarks('Balance to be paid with the renewal premium'),
  ],
  return_ao: [
    opening('^return$', 'first_renewal'),
    inDialog('Reason', /Incomplete details/),
    remarks('Add the client\'s call and the new sum insured'),
  ],
  assign_po: [
    opening('^assign po$', 'first_renewal'),
    inDialog('Processing Officer', /Paolo/),
  ],
  return_marketing: [
    opening('^return to marketing$', 'first_renewal'),
    inDialog('Return to', /^Account Officer$/),
    inDialog('Reason', /For correction/),
    remarks('The sum insured of the expiring policy is not the one of the schedule'),
  ],
  new_batch: [
    opening('^new batch$'),
    inDialog('Insurance Company', /Mabuhay/),
    inDialog('Expiry From', dateText(330)),
    inDialog('Expiry To', dateText(420)),
  ],
  response_revise: [
    opening('^record insurer response$'),
    inDialog('Response', /revised terms/),
    inDialog('Insurer Reference', 'MGIC-REN-2027-0077'),
    inDialog('Revised Premium', '19850.00'),
    inDialog('Revised Sum Insured', '1600000.00'),
    remarks('Insurer renews with a higher premium after the claims review'),
  ],
  acceptance_payment: [
    opening('^record acceptance$'),
    inDialog('Method', /^Payment$/),
    inDialog('Payment Reference', 'OR-2027-001122'),
    remarks('Renewal premium paid at the branch'),
  ],
  followup_call: [
    opening('^add follow-up$'),
    inDialog('Channel', /^Call$/),
    inDialog('Outcome', /Call back requested/),
    inDialog('Next Action Date', dateText(3)),
    remarks('Client asked to call back on Monday'),
  ],
  new_risk_code: [
    opening('^new risk code$'),
    inDialog('Risk Code', 'PAR09'),
    inDialog('Line of Business', 'PROPERTY'),
    inDialog('Effective From', dateText(1)),
    inDialog('Reason', 'Property in a declared no-build zone, not renewed'),
  ],
};

const selects = {
  first_renewal: (page) => tickRows(page, 1),
};

// ------------------------------------------------------------------ states after opening

const after = {
  'ux-scr-rn-02-empty': async (page) => {
    await page.getByPlaceholder(/^search/i).first().fill('RNW-2099-999999');
    await page.getByRole('button', { name: /^search$/i }).first().click();
    await settle(page, 900);
  },
  'scr-rn-11-01-list': async (page, ctx) => {
    const batch = ctx.one('select batch_no from rnw_insurer_batch order by id desc limit 1');
    await page.locator('main table tbody tr').filter({ hasText: batch }).first().click();
    await settle(page, 1200);
  },
  'ux-scr-rn-09-actions': async (page) => {
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await settle(page, 300);
  },
  'ux-scr-rn-17-actions': async (page) => {
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await settle(page, 300);
  },
};

// A step or shot that shows one tab of a long record page: the tab strip and the tab's content, whole.
const TAB = 'main div.stack > div.tabs[role=tablist], main div.stack > div.tabs[role=tablist] ~ *';
const crops = { 'wt-a-03': TAB, 'wt-c-02': TAB, 'wt-c-04': TAB, 'wt-d-04': TAB };

// The record tabs: a field badge goes in the card that shows it, never on the flag chips of the header.
const card = (title) => ({ within: 'main section.card', title });
const callouts = {
  'SCR-RN-04': {
    1: card('Check results'),
    2: card('Classification history'),
    3: card('Endorsements linked'),
    5: card('Prior renewals'),
    6: card('Expiring'),
  },
};

// ------------------------------------------------------------------ documents

const year = () => new Date().getUTCFullYear();

const documents = {
  'doc-renewal-advice': async (ctx, out) => {
    const no = ctx.one(`select l.letter_no from rnw_letter l join rnw_candidate c on c.id = l.candidate_id where c.renewal_ref = '${refA(ctx)}' and l.letter_type = 'RA' order by l.id desc limit 1`);
    render(await ctx.api('ao2', 'GET', `/renewal/letters/${no}/file.pdf?companyId=${company(ctx)}`), 'pdf', out);
  },
  'doc-closing-letter': async (ctx, out) => {
    const query = "select l.letter_no from rnw_letter l where l.letter_type in ('NFR', 'NAL') order by l.id desc limit 1";
    if (ctx.sql(query).length === 0) {
      const ref = ctx.one("select renewal_ref from rnw_candidate where stage = 'LETTER_PENDING' order by id limit 1");
      await ctx.api('mkttl', 'POST', '/renewal/letters/closing', { companyId: company(ctx), renewalRefs: [ref] });
    }
    render(await ctx.api('mkttl', 'GET', `/renewal/letters/${ctx.one(query)}/file.pdf?companyId=${company(ctx)}`), 'pdf', out);
  },
  'doc-insurer-extract': async (ctx, out) => {
    const batch = ctx.one('select batch_no from rnw_insurer_batch order by id desc limit 1');
    render(await ctx.api('proc', 'GET', `/renewal/insurer-batches/${batch}/file.xlsx?companyId=${company(ctx)}`), 'xlsx', out, 200,
      ['Insurance Company', 'Invoice No', 'Assured Name', 'Risk Code', 'Inception Date', 'Expiry Date', 'Sum Insured',
        'Premium Amount', 'Commission Rate', 'Is Mortgaged', 'Policy No']);
  },
  'doc-expiry-list': async (ctx, out) => {
    render(await ctx.api('mkttl', 'GET', `/renewal/candidates/export.xlsx?companyId=${company(ctx)}&tab=ALL`), 'xlsx', out, 200,
      ['Renewal reference', 'Status', 'Classification', 'Disposition', 'Policy number', 'Client', 'Insurer', 'Unit',
        'Account officer', 'Expiry date', 'Gross premium']);
  },
  'doc-account-details': async (ctx, out) => {
    render(await ctx.api('ao2', 'GET', `/renewal/candidates/${refA(ctx)}/details.pdf?companyId=${company(ctx)}`), 'pdf', out);
  },
};

module.exports = { opens, fills, selects, after, crops, callouts, custom: {}, walkthrough: walkthrough.steps, documents,
  prepare: walkthrough.prepare, year, tickRow };
