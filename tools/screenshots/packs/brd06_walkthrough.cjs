// End-to-end walkthroughs of BRD-06 Renewal, performed live on the seed profile by capture_pack.cjs (see
// walkthroughs.yaml of the pack). Each step signs in as the user of the step, does what the step says on the screen
// and returns the page to capture. Run on a fresh seed database: walkthroughs A and B first ('^wt-(a|b)-'), then the
// screens, documents and UX states, then walkthroughs C, D and E ('^wt-(c|d|e)-'). B and C work on the renewal that A
// posts; the renewals are found by their expiring account or migrated policy, never by a number typed here.
const { act, go, button, settle } = require('./brd01_walkthrough.cjs');
const { fill, dateText } = require('./brd02_walkthrough.cjs');
const { search, tickRow, dialogOf, tabsToTop } = require('./brd04_walkthrough.cjs');

// Corporate Team 1: its Team Leader and Account Officer (seed users of the Renewal personas).
const CORP_TL = 'rnwtl';
const CORP_AO = 'ao2';

// ------------------------------------------------------------------ records

function has(ctx, query) {
  return ctx.sql(query).length > 0;
}

/** The renewal of a booked account by its expiring ARN, or of a migrated policy by its legacy reference. */
function renewalOf(ctx, key) {
  return ctx.one(`select renewal_ref from rnw_candidate where expiring_arn = '${key}' or source_ref = '${key}' order by id limit 1`);
}

const refA = (ctx) => renewalOf(ctx, 'ARN-2026-940004');
const refD = (ctx) => renewalOf(ctx, 'QPS-FI-0100007');
const refE = (ctx) => renewalOf(ctx, 'QPS-FI-0100002');

function stageOf(ctx, ref) {
  return ctx.one(`select stage from rnw_candidate where renewal_ref = '${ref}'`);
}

const company = (ctx) => Number(ctx.one("select id from org_company where code = 'FVI'"));

/** The record page of a renewal. */
const record = (ref) => `/renewal/candidates/${encodeURIComponent(ref)}`;

// ------------------------------------------------------------------ page helpers

/** Confirms the open dialog with its button of that name and waits until it closes (or shows its outcome). */
async function confirm(page, name) {
  await dialogOf(page).getByRole('button', { name }).last().click();
  await dialogOf(page).waitFor({ state: 'hidden', timeout: 20000 }).catch(() => {});
  await settle(page, 1200);
}

/** Opens a tab of a list or record by its name. */
async function tab(page, name) {
  await page.getByRole('tab', { name }).first().click();
  await settle(page, 900);
}

/** Ticks the row of a renewal on a list (searched first, so the row is in view). */
async function tickRenewal(page, ref) {
  await search(page, ref);
  await tickRow(page, ref);
}

/** Opens the record page of a renewal and the tab named. */
async function openRecord(ctx, user, ref, tabName) {
  const page = await go(ctx, user, record(ref));
  if (tabName) {
    await tab(page, tabName);
  }
  return page;
}

// ------------------------------------------------------------------ steps

const steps = {
  // ---------------------------------------------------------------- walkthrough A
  'wt-a-01': async (ctx) => {
    const ref = refA(ctx);
    const page = await go(ctx, CORP_TL, '/renewal/expiry?tab=UNASSIGNED');
    if (stageOf(ctx, ref) === 'UNASSIGNED') {
      await tickRenewal(page, ref);
      await button(page, /^assign disposition$/i).click();
      await settle(page, 600);
      await fill(page, 'Account Officer', /Arnel/, dialogOf(page));
      await confirm(page, /^assign and push$/i);
    }
    await search(page, '');
    return page;
  },
  'wt-a-02': async (ctx) => go(ctx, CORP_AO, '/renewal/mine'),
  'wt-a-03': async (ctx) => {
    const page = await openRecord(ctx, CORP_AO, refA(ctx), 'Account History');
    await button(page, /^view account history$/i).click();
    await settle(page, 1200);
    await tabsToTop(page);
    return page;
  },
  'wt-a-04': async (ctx) => {
    const ref = refA(ctx);
    const page = await openRecord(ctx, CORP_AO, ref);
    if (!has(ctx, `select 1 from rnw_candidate where renewal_ref = '${ref}' and disposition = 'FOR_RENEWAL'`)) {
      await act(page, 'Set Disposition', {
        reason: false,
        confirm: /^save$/i,
        fill: async (d) => {
          await fill(page, 'Disposition', /^For Renewal$/, d);
          await d.getByLabel(/^Remarks/).fill('Client confirmed by phone that it renews with Luzon Assurance');
        },
      });
    }
    return page;
  },
  'wt-a-05': async (ctx) => {
    const ref = refA(ctx);
    const page = await openRecord(ctx, CORP_AO, ref);
    if (stageOf(ctx, ref) === 'FOR_DISPOSITION') {
      await button(page, /^push$/i).click();
      await settle(page, 800);
      // Push asks for the approver (every Team Leader of the unit by default) and submits for posting.
      const d = dialogOf(page);
      if (await d.isVisible().catch(() => false)) {
        await d.getByRole('button', { name: /^submit for posting$/i }).click();
        await d.waitFor({ state: 'hidden', timeout: 15000 }).catch(() => {});
      }
      await settle(page, 1500);
    }
    return page;
  },
  'wt-a-06': async (ctx) => {
    const page = await go(ctx, CORP_TL, '/renewal/review');
    if (stageOf(ctx, refA(ctx)) === 'FOR_TL_REVIEW') {
      await tickRenewal(page, refA(ctx));
      await button(page, /^post$/i).click();
      await settle(page, 600);
      await dialogOf(page).getByRole('button', { name: /^post$/i }).last().click();
      await settle(page, 1500);
    }
    return page;
  },
  'wt-a-07': async (ctx) => {
    const ref = refA(ctx);
    // The open claim and the endorsement in progress are overridden through the same dialog, before the step.
    for (const target of ['CLAIMS', 'ENDORSEMENT_PENDING']) {
      if (!has(ctx, `select 1 from rnw_override o join rnw_candidate c on c.id = o.candidate_id where c.renewal_ref = '${ref}' and o.check_code = '${target}'`)) {
        await ctx.api(CORP_TL, 'POST', '/renewal/candidates/override', { companyId: company(ctx), renewalRefs: [ref], kind: 'CHECK',
          target, reasonCode: 'TL_APPROVED', remarks: 'Claim and endorsement reviewed with the unit head; renewal agreed' });
      }
    }
    const page = await go(ctx, CORP_TL, '/renewal/review');
    if (!has(ctx, `select 1 from rnw_override o join rnw_candidate c on c.id = o.candidate_id where c.renewal_ref = '${ref}' and o.kind = 'OUTSTANDING_BALANCE'`)) {
      await tickRenewal(page, ref);
      await button(page, /^override$/i).click();
      await settle(page, 600);
      const d = dialogOf(page);
      await fill(page, 'Override', /^Outstanding balance$/, d);
      await fill(page, 'Reason', /Approved by the Team Leader/, d);
      await d.getByLabel(/^Remarks/).fill('Client pays the balance with the renewal premium');
      await confirm(page, /^override$/i);
    }
    return page;
  },
  'wt-a-08': async (ctx) => {
    const page = await go(ctx, CORP_TL, '/renewal/review');
    if (stageOf(ctx, refA(ctx)) === 'FOR_TL_REVIEW') {
      await tickRenewal(page, refA(ctx));
      await button(page, /^post$/i).click();
      await settle(page, 600);
      await confirm(page, /^post$/i);
    }
    return page;
  },

  // ---------------------------------------------------------------- walkthrough B
  'wt-b-01': async (ctx) => {
    const page = await go(ctx, 'proctl', '/renewal/processing?tab=FOR_PROCESSING');
    if (stageOf(ctx, refA(ctx)) === 'FOR_PROCESSING') {
      await tickRenewal(page, refA(ctx));
      await button(page, /^assign po$/i).click();
      await settle(page, 600);
      await fill(page, 'Processing Officer', /Paolo/, dialogOf(page));
      await confirm(page, /^assign$/i);
    }
    return page;
  },
  'wt-b-02': async (ctx) => {
    const ref = refA(ctx);
    const [expiry, insurer] = ctx.sql(`select to_char(expiry_date, 'DD-Mon-YYYY'), insurer_code from rnw_candidate where renewal_ref = '${ref}'`)[0];
    const page = await go(ctx, 'proc', '/renewal/insurer');
    const exists = `select b.batch_no from rnw_insurer_batch b join rnw_insurer_batch_line l on l.batch_id = b.id join rnw_candidate c on c.id = l.candidate_id where c.renewal_ref = '${ref}' order by b.id desc limit 1`;
    if (!has(ctx, exists)) {
      await button(page, /^new batch$/i).click();
      await settle(page, 600);
      const d = dialogOf(page);
      const name = ctx.one(`select name from cat_insurer where party_code = '${insurer}' limit 1`);
      await fill(page, 'Insurance Company', new RegExp(name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), d);
      await fill(page, 'Expiry From', expiry, d);
      await fill(page, 'Expiry To', expiry, d);
      await confirm(page, /^create batch$/i);
    }
    ctx.state.batchB = ctx.one(exists);
    await page.locator('main table tbody tr').filter({ hasText: ctx.state.batchB }).first().click();
    await settle(page, 1200);
    return page;
  },
  'wt-b-03': async (ctx) => {
    const batch = ctx.state.batchB || ctx.one(`select b.batch_no from rnw_insurer_batch b join rnw_insurer_batch_line l on l.batch_id = b.id join rnw_candidate c on c.id = l.candidate_id where c.renewal_ref = '${refA(ctx)}' order by b.id desc limit 1`);
    const page = await go(ctx, 'proc', '/renewal/insurer');
    await page.locator('main table tbody tr').filter({ hasText: batch }).first().click();
    await settle(page, 1200);
    if (has(ctx, `select 1 from rnw_insurer_batch where batch_no = '${batch}' and status = 'DRAFT'`)) {
      await button(page, /^send to insurer$/i).click();
      await settle(page, 600);
      await confirm(page, /^send$/i);
    }
    return page;
  },
  'wt-b-04': async (ctx) => {
    const ref = refA(ctx);
    const page = await openRecord(ctx, 'proc', ref);
    if (stageOf(ctx, ref) === 'WITH_INSURER') {
      await act(page, 'Record Insurer Response', {
        reason: false,
        confirm: /^record$/i,
        fill: async (d) => {
          await fill(page, 'Response', /^Renew as is$/, d);
          await fill(page, 'Insurer Reference', 'LAC-REN-2027-0042', d);
          await d.getByLabel(/^Remarks/).fill('Insurer renews on the expiring terms');
        },
      });
    }
    return page;
  },
  'wt-b-05': async (ctx) => {
    const ref = refA(ctx);
    const page = await go(ctx, CORP_AO, '/renewal/letters?tab=RA_READY');
    if (stageOf(ctx, ref) === 'RA_READY') {
      await tickRenewal(page, ref);
      await button(page, /^generate ra$/i).click();
      await settle(page, 600);
      await fill(page, 'Notice', /^First notice$/, dialogOf(page));
      await confirm(page, /^generate$/i);
    }
    return page;
  },
  'wt-b-06': async (ctx) => {
    const ref = refA(ctx);
    const page = await go(ctx, CORP_AO, '/renewal/letters?tab=RA_GENERATED');
    if (stageOf(ctx, ref) === 'RA_GENERATED') {
      await tickRenewal(page, ref);
      await button(page, /^send$/i).click();
      await settle(page, 600);
      await confirm(page, /^send$/i);
    }
    return page;
  },

  // ---------------------------------------------------------------- walkthrough C
  'wt-c-01': async (ctx) => go(ctx, 'contactc', '/renewal/followups'),
  'wt-c-02': async (ctx) => {
    const ref = refA(ctx);
    const page = await openRecord(ctx, 'contactc', ref);
    if (!has(ctx, `select 1 from rnw_followup f join rnw_candidate c on c.id = f.candidate_id where c.renewal_ref = '${ref}'`)) {
      await act(page, 'Add Follow-up', {
        reason: false,
        confirm: /^save$/i,
        fill: async (d) => {
          await fill(page, 'Channel', /^Call$/, d);
          await fill(page, 'Outcome', /Client will renew/, d);
          await fill(page, 'Next Action Date', dateText(5), d);
          await d.getByLabel(/^Remarks/).fill('Treasurer confirmed; payment by check this week');
        },
      });
    }
    await tab(page, 'Remarks & Follow-ups');
    await tabsToTop(page);
    return page;
  },
  'wt-c-03': async (ctx) => {
    const ref = refA(ctx);
    const page = await openRecord(ctx, CORP_AO, ref);
    if (stageOf(ctx, ref) === 'RA_SENT') {
      await act(page, 'Record Acceptance', {
        reason: false,
        confirm: /^record$/i,
        fill: async (d) => {
          await fill(page, 'Method', /^Payment$/, d);
          await fill(page, 'Payment Reference', 'OR-2027-001122', d);
          await d.getByLabel(/^Remarks/).fill('Renewal premium paid at the branch');
        },
      });
    }
    return page;
  },
  'wt-c-04': async (ctx) => {
    const page = await openRecord(ctx, CORP_AO, refA(ctx), 'Letters');
    await tabsToTop(page);
    return page;
  },

  // ---------------------------------------------------------------- walkthrough D
  'wt-d-01': async (ctx) => {
    const ref = refD(ctx);
    if (stageOf(ctx, ref) === 'UNASSIGNED') {
      await ctx.api('mkttl', 'POST', '/renewal/candidates/assign', { companyId: company(ctx), renewalRefs: [ref], ao: 'ao' });
    }
    const page = await openRecord(ctx, 'ao', ref, 'Account History');
    if (!has(ctx, `select 1 from rnw_candidate where renewal_ref = '${ref}' and disposition is not null`)) {
      await button(page, /^view account history$/i).click();
      await settle(page, 1000);
      await act(page, 'Set Disposition', {
        reason: false,
        confirm: /^save$/i,
        fill: async (d) => {
          await fill(page, 'Disposition', /^Not for Renewal$/, d);
          await fill(page, 'Reason for Not for Renewal', /Loan fully paid/, d);
          await d.getByLabel(/^Remarks/).fill('Loan fully paid in May; client insures directly');
        },
      });
    }
    await tab(page, 'Details');
    return page;
  },
  'wt-d-02': async (ctx) => {
    const ref = refD(ctx);
    if (stageOf(ctx, ref) === 'FOR_DISPOSITION') {
      await ctx.api('ao', 'POST', '/renewal/candidates/push', { companyId: company(ctx), renewalRefs: [ref] });
    }
    const page = await go(ctx, 'mkttl', '/renewal/review');
    if (stageOf(ctx, ref) === 'FOR_TL_REVIEW') {
      await tickRenewal(page, ref);
      await button(page, /^post$/i).click();
      await settle(page, 600);
      await confirm(page, /^post$/i);
    }
    return page;
  },
  'wt-d-03': async (ctx) => {
    const ref = refD(ctx);
    const page = await go(ctx, 'ao', '/renewal/letters?tab=LETTER_PENDING');
    if (stageOf(ctx, ref) === 'LETTER_PENDING') {
      await tickRenewal(page, ref);
      await button(page, /^send letters$/i).click();
      await settle(page, 600);
      await confirm(page, /^send$/i);
    }
    return page;
  },
  'wt-d-04': async (ctx) => {
    const page = await openRecord(ctx, 'ao', refD(ctx), 'Letters');
    await tabsToTop(page);
    return page;
  },

  // ---------------------------------------------------------------- walkthrough E
  'wt-e-01': async (ctx) => {
    const page = await go(ctx, CORP_TL, '/renewal/transfers');
    const row = page.locator('main table tbody tr').filter({ hasText: 'Luzon Agri' }).first();
    await row.waitFor({ timeout: 15000 });
    if (has(ctx, "select 1 from rnw_transfer where status = 'REQUESTED'")) {
      await row.getByRole('button', { name: /^Actions for/ }).click();
      await page.waitForTimeout(300);
      await page.getByRole('menuitem', { name: /^accept$/i }).click();
      await settle(page, 600);
      await dialogOf(page).locator('textarea').first().fill('Corporate client of our unit; assigned to Arnel');
      await confirm(page, /^accept$/i);
    }
    return page;
  },
  'wt-e-02': async (ctx) => {
    const ref = refE(ctx);
    if (stageOf(ctx, ref) === 'UNASSIGNED') {
      await ctx.api('mkttl', 'POST', '/renewal/candidates/assign', { companyId: company(ctx), renewalRefs: [ref], ao: 'ao' });
    }
    const page = await openRecord(ctx, 'ao', ref);
    await act(page, 'Set Disposition', {
      reason: false,
      confirm: /^save$/i,
      keepResult: true,
      fill: async (d) => {
        await fill(page, 'Disposition', /^For Renewal$/, d);
      },
    });
    return page;
  },
  'wt-e-03': async (ctx) => {
    const page = await go(ctx, 'mkttl', '/renewal/expiry?tab=UNASSIGNED');
    await tickRenewal(page, refE(ctx)).catch(async () => {
      await page.goto(`${ctx.BASE}/renewal/expiry?tab=ALL`);
      await settle(page);
      await tickRenewal(page, refE(ctx));
    });
    await button(page, /^transfer$/i).click();
    await settle(page, 600);
    const d = dialogOf(page);
    await fill(page, 'Receiving Unit', /CBG Metro Team 1|CBG Team 1|T-CBG1/, d);
    await d.getByLabel(/^Remarks/).fill('Client asked for its own unit');
    await d.getByRole('button', { name: /^submit and push$/i }).click();
    await settle(page, 1500);
    return page;
  },
};

/** Nothing is prepared before the walkthroughs: every state comes from the seed data and the steps. */
async function prepare() {}

module.exports = { steps, prepare, refA, refD, refE, renewalOf, record, company, tickRenewal, confirm, tab };
