// End-to-end walkthroughs of BRD-03 Product Maintenance, performed live on the seed profile by capture_pack.cjs (see
// walkthroughs.yaml of the pack). Each step signs in as the persona of the step, does what the step says on the
// screen and returns the page to capture. The records created carry fictitious seed values only.
const { pdf, act, tab, go, button, settle } = require('./brd01_walkthrough.cjs');

const TITLE = 'Motor Fleet Plus';
const RISK_CODE = 'MTR30';
const REQUESTS = '/product-maintenance/requests';

// ------------------------------------------------------------------ helpers

/** The request of walkthrough A (created at step 3). */
function requestA(ctx) {
  const row = ctx.sql(`select id from pm_request where title = '${TITLE}' order by id desc limit 1`)[0];
  if (!row) {
    throw new Error('run walkthrough A from step 3');
  }
  return row[0];
}

const seedRequest = (ctx, no) => ctx.one(`select id from pm_request where request_no = '${no}'`);

async function openRequest(ctx, user, id, tabName) {
  const page = await go(ctx, user, `${REQUESTS}/${id}`);
  if (tabName) {
    await tab(page, tabName);
  }
  return page;
}

async function fill(page, label, value) {
  const field = page.getByLabel(new RegExp(`^${label}`)).first();
  await field.fill(String(value));
  await page.waitForTimeout(150);
}

/** Attaches a file of a document type in the Documents panel (the upload starts when the file is chosen). */
async function attach(page, type, file) {
  const main = page.locator('main');
  const select = main.getByLabel(/^Document type/).first();
  const options = await select.locator('option').allTextContents();
  await select.selectOption({ label: options.find((o) => new RegExp(type, 'i').test(o)) });
  await main.getByLabel(/^Files to attach/).first().setInputFiles(file);
  await settle(page, 1800);
}

const dialog = (page) => page.locator('dialog.modal[open]').last();

// ------------------------------------------------------------------ walkthrough A

const steps = {
  // 1-2. A clause of the library, made by MBS and authorised by the Business Administrator.
  'wt-a-01': async (ctx) => {
    const page = await go(ctx, 'mbs', '/catalog/coverages');
    await tab(page, 'Clause Library');
    if (ctx.sql("select 1 from cat_clause where code = 'FLEET_REPAIR'").length > 0) {
      return page;
    }
    await button(page, /^new clause$/i).click();
    const d = dialog(page);
    await d.getByLabel(/^Code/).fill('FLEET_REPAIR');
    const kind = d.getByLabel(/^Kind/);
    await kind.selectOption({ label: (await kind.locator('option').allTextContents()).find((o) => /^Clause$/i.test(o.trim())) });
    await d.getByLabel(/^Product line/).selectOption({ label: 'Motor' });
    await d.getByLabel(/^Title/).fill('Accredited repair shops for fleets');
    await d.getByLabel(/^Effective from/).fill(new Date(Date.now() + 8 * 3600 * 1000).toISOString().slice(0, 10)); // Philippine date
    await d.getByLabel(/^Wording/).fill('Repairs of insured fleet vehicles are made at the accredited repair shops of the insurer (seed data).');
    await d.getByRole('button', { name: /save for authorization/i }).click();
    await settle(page, 1200);
    return page;
  },
  'wt-a-02': async (ctx) => {
    const page = await go(ctx, 'badmin', '/catalog/coverages');
    await tab(page, 'Clause Library');
    const row = page.locator('table tbody tr').filter({ hasText: 'FLEET_REPAIR' }).first();
    const authorize = row.getByRole('button', { name: /^authorize$/i });
    if (await authorize.count()) {
      await authorize.click();
      await settle(page, 1200);
    }
    return page;
  },
  // 3. Marketing creates and submits the request.
  'wt-a-03': async (ctx) => {
    const page = await go(ctx, 'ao', `${REQUESTS}/new`);
    await ctx.runSteps(page, [
      ['Request type', 'New package'], ['Scope', 'Generic programme'], ['Package / programme name', TITLE],
      ['Product line', 'Motor'], ['Cover type / subtype', 'Comprehensive'], ['Reason', 'New programme'],
      ['Requested rate %', '1.2'], ['Minimum premium', '5000'], ['Commission %', '15'], ['Package TSI limit', '5000000'],
    ]);
    // Effective today, so that New Business can quote the package on the day it is released (steps 21-24).
    const start = new Date(Date.now() + 8 * 3600 * 1000); // Philippine date: the system's today
    const end = new Date(start);
    end.setFullYear(end.getFullYear() + 1);
    const iso = (d) => d.toISOString().slice(0, 10);
    await fill(page, 'Effective from', iso(start));
    await fill(page, 'Package start', iso(start));
    await fill(page, 'Package end', iso(end));
    await button(page, /^add coverage$/i).click();
    await fill(page, 'Coverage 1 code', 'OD_THEFT');
    await fill(page, 'Coverage 1 limit', '2000000');
    await fill(page, 'Coverage 1 deductible', 'PHP 2,000 per claim');
    for (const insurer of ['Mabuhay General Insurance Corp.', 'Luzon Assurance Co.']) {
      await page.getByLabel(insurer).check();
    }
    await button(page, /^submit for approval$/i).click();
    await settle(page, 2000);
    return page;
  },
  'wt-a-04': async (ctx) => act(await openRequest(ctx, 'mkttl', requestA(ctx)), /^approve and send to tsu$/i),
  'wt-a-05': async (ctx) => {
    const page = await openRequest(ctx, 'tsulead', requestA(ctx));
    await act(page, /^recommend for approval$/i, {
      confirm: /^recommend$/i,
      fill: async (d) => d.locator('textarea').first().fill('Two panel insurers approached; fleet market of the Commercial segment.'),
    });
    return page;
  },
  'wt-a-06': async (ctx) => act(await openRequest(ctx, 'tsuhead', requestA(ctx)), /^approve for negotiation$/i),
  // 7-10. Negotiation.
  'wt-a-07': async (ctx) => {
    const page = await openRequest(ctx, 'tsu', requestA(ctx), 'Negotiation');
    await page.getByLabel(/^Notes to the insurers/).fill('Please quote the fleet programme as requested (seed data).');
    await button(page, /^submit for approval$/i).click();
    await settle(page, 1500);
    return page;
  },
  'wt-a-08': async (ctx) => {
    const page = await openRequest(ctx, 'tsulead', requestA(ctx), 'Negotiation');
    await button(page, /^approve and send$/i).click();
    await settle(page, 2500);
    return page;
  },
  'wt-a-09': async (ctx) => {
    const page = await openRequest(ctx, 'tsu', requestA(ctx), 'Negotiation');
    const replies = [
      ['Mabuhay', /approved with changes/i, '1.15', '5000'],
      ['Luzon', /counter-proposal/i, '1.25', '6000'],
    ];
    for (const [insurer, outcome, rate, minimum] of replies) {
      const row = page.locator('table tbody tr').filter({ hasText: insurer }).first();
      await row.getByRole('button', { name: /^key in$/i }).click();
      await page.waitForTimeout(600);
      const d = dialog(page);
      const select = d.getByLabel(/^Outcome/);
      await select.selectOption({ label: (await select.locator('option').allTextContents()).find((o) => outcome.test(o)) });
      await d.getByLabel(/^Rate %/).fill(rate);
      await d.getByLabel(/^Minimum premium/).fill(minimum);
      await d.locator('input[type=file]').first().setInputFiles(pdf(`${insurer}-reply.pdf`, `${insurer} reply to the quotation slip`,
        [`Rate ${rate}%, minimum premium PHP ${minimum}`, 'Seed data for the SIT environment']));
      await d.getByRole('button', { name: /^save terms$/i }).click();
      await settle(page, 1500);
    }
    return page;
  },
  'wt-a-10': async (ctx) => {
    const page = await openRequest(ctx, 'tsu', requestA(ctx), 'Negotiation');
    await act(page, /^terms final$/i, {
      confirm: /^make terms final$/i,
      fill: async (d) => d.getByLabel(/Mabuhay/).check(),
    });
    return page;
  },
  'wt-a-11': async (ctx) => openRequest(ctx, 'tsu', requestA(ctx), 'Comparative'),
  'wt-a-12': async (ctx) => act(await openRequest(ctx, 'tsuhead', requestA(ctx)), /^proceed to requirements/i),
  // 13-14. Requirements and ManCom.
  'wt-a-13': async (ctx) => {
    const id = requestA(ctx);
    let page = await openRequest(ctx, 'tsu', id, 'Requirements');
    await page.getByLabel(/^Computation basis/).fill('Comprehensive rate on the vehicle value; Acts of Nature included');
    await button(page, /^save requirements$/i).click();
    await settle(page, 1200);
    page = await openRequest(ctx, 'tsu', id, 'Documents');
    await attach(page, 'Package slip', pdf('package-slip-signed.pdf', 'Package slip (signed)',
      [`Package ${TITLE}`, 'Signed by the TSU Head and the insurer (seed data)']));
    page = await openRequest(ctx, 'tsu', id, 'Requirements');
    await act(page, /^submit requirements for mancom sign-off$/i);
    return page;
  },
  'wt-a-14': async (ctx) => {
    const page = await openRequest(ctx, 'mancom', requestA(ctx), 'Requirements');
    await act(page, /^sign off and send to mbs$/i);
    return page;
  },
  // 15-17. Set-up and validation.
  'wt-a-15': async (ctx) => {
    const page = await openRequest(ctx, 'mbs', requestA(ctx));
    await act(page, /^set up package version$/i, {
      confirm: /^set up version$/i,
      fill: async (d) => {
        await d.getByLabel(/^Risk code of the new package/).fill(RISK_CODE);
        await d.getByLabel(/^Change summary/).fill('First version of the fleet programme');
      },
    });
    await tab(page, 'Set-up');
    return page;
  },
  'wt-a-16': async (ctx) => {
    const page = await go(ctx, 'mbs', `/catalog/products/${RISK_CODE}/versions/1`);
    // The version starts on the Philippine today (a capture that runs across midnight UTC keeps it valid).
    const today = new Date(Date.now() + 8 * 3600 * 1000).toISOString().slice(0, 10);
    const from = page.getByLabel(/^Effective From/).first();
    if (await from.isEditable().catch(() => false)) {
      await from.fill(today);
      await page.getByLabel(/^Package Start/).first().fill(today).catch(() => {});
    }
    await tab(page, 'Insurer Terms');
    const clauses = page.getByLabel(/^Clauses of INS-MGIC on OD_THEFT/);
    if (await clauses.count()) {
      const options = await clauses.locator('option').allTextContents();
      const pick = options.find((o) => /accredited repair shops for fleets/i.test(o));
      if (pick) {
        await clauses.selectOption({ label: pick });
      }
    }
    await button(page, /^submit for validation$/i).click();
    await settle(page, 1500);
    return page;
  },
  'wt-a-17': async (ctx) => {
    const page = await go(ctx, 'tsuhead', `/catalog/products/${RISK_CODE}/versions/1`);
    for (const box of await page.locator('main input[type=checkbox]').all()) {
      await box.check().catch(() => {});
    }
    await button(page, /^validate and release$/i).click();
    await settle(page, 2000);
    return page;
  },
  // 18-19. Commission rate of the insurer for the new product.
  'wt-a-18': async (ctx) => {
    const id = ctx.one("select id from cat_insurer where party_code = 'INS-MGIC' order by id limit 1");
    const page = await go(ctx, 'badmin', `/catalog/insurers/${id}`);
    await button(page, /^add rate$/i).click();
    const d = dialog(page);
    const product = d.getByLabel(/^Product/);
    await product.selectOption({ label: (await product.locator('option').allTextContents()).find((o) => o.startsWith(RISK_CODE)) });
    await d.getByLabel(/^Commission %/).fill('15');
    await d.getByRole('button', { name: /save for authorization/i }).click();
    await settle(page, 1200);
    return page;
  },
  'wt-a-19': async (ctx) => {
    const id = ctx.one("select id from cat_insurer where party_code = 'INS-MGIC' order by id limit 1");
    const page = await go(ctx, 'approver', `/catalog/insurers/${id}`);
    const row = page.locator('table tbody tr').filter({ hasText: RISK_CODE }).first();
    const authorize = row.getByRole('button', { name: /^authorize$/i });
    if (await authorize.count()) {
      await authorize.click();
      await settle(page, 1200);
    }
    return page;
  },
  // 20. Advisory (the ManCom sign-off record was attached at step 14).
  'wt-a-20': async (ctx) => {
    const page = await openRequest(ctx, 'tsu', requestA(ctx), 'Advisories');
    const send = button(page, /^send advisory$/i);
    if (await send.isEnabled().catch(() => false)) {
      await send.click();
      await settle(page, 1800);
    }
    return page;
  },
  // 21-23. New business pricing and the rate exception.
  'wt-a-21': async (ctx) => {
    const page = await go(ctx, 'ao', '/catalog/calculator');
    await ctx.runSteps(page, [['Product', `^${RISK_CODE}`], ['Insurer', 'Mabuhay'], ['Sum insured', '1500000']]);
    await button(page, /^calculate$/i).click();
    await settle(page, 1500);
    return page;
  },
  'wt-a-22': async (ctx) => {
    const client = ctx.one("select id from crm_client where status = 'CONFIRMED' and last_name = 'Garcia' order by id limit 1");
    const company = Number(ctx.one("select id from org_company where code = 'FVI'"));
    let q = ctx.sql(`select id, quotation_no from quo_quotation where product_code = '${RISK_CODE}' order by id desc limit 1`)[0];
    if (!q) {
      const created = await ctx.api('ao', 'POST', '/quotations', {
        companyId: company, clientId: Number(client), productCode: RISK_CODE, insurerCode: 'INS-MGIC',
        marketSegment: 'CBG',
        items: [{ riskGroup: 1, item: { description: 'Fleet vehicle 1', sumInsured: 1500000, rate: 1.10 } }],
      });
      q = [created.id, created.quotationNo];
    }
    const page = await go(ctx, 'ao', `/quotations/${q[0]}`);
    await act(page, /^request rate exception$/i, {
      confirm: /^request exception$/i,
      fill: async (d) => d.getByLabel(/^Reason/).fill('Fleet of 30 vehicles with no claims in three years (seed data).'),
    });
    return page;
  },
  // 23. The approver opens the exception from My Approvals and approves it on its record.
  'wt-a-23': async (ctx) => {
    const page = await go(ctx, 'approver', '/approvals');
    const ex = ctx.sql(`select reference_no from cat_rate_scheme_exception where product_code = '${RISK_CODE}' and record_status = 'PENDING_AUTHORIZATION' order by id limit 1`)[0];
    if (!ex) {
      const done = ctx.one(`select reference_no from cat_rate_scheme_exception where product_code = '${RISK_CODE}' order by id limit 1`);
      return go(ctx, 'approver', `/catalog/rate-exceptions/${done}`);
    }
    await page.locator('table tbody tr', { hasText: ex[0] }).first().click();
    await settle(page, 1200);
    await act(page, /^approve$/i, {
      confirm: /^approve$/i,
      comment: 'Loss-free fleet of 30 vehicles; approved for this quotation only (seed data).',
    });
    return page;
  },
  'wt-a-24': async (ctx) => {
    const q = ctx.one(`select id from quo_quotation where product_code = '${RISK_CODE}' order by id desc limit 1`);
    return go(ctx, 'ao', `/quotations/${q}`);
  },

  // ---------------------------------------------------------------- walkthrough B
  'wt-b-01': async (ctx) => {
    const page = await go(ctx, 'ao', `${REQUESTS}/new`);
    await button(page, /^save draft$/i).click();
    await settle(page, 800);
    return page;
  },
  'wt-b-02': async (ctx) => {
    const page = await go(ctx, 'ao', `${REQUESTS}/new`);
    await ctx.runSteps(page, [['Package / programme name', 'Travel Assist Programme'], ['Product line', 'Motor'],
      ['Cover type / subtype', 'Comprehensive'], ['Reason', 'Market competitiveness']]);
    await button(page, /^submit for approval$/i).click();
    await settle(page, 1500);
    return page;
  },
  'wt-b-03': async (ctx) => {
    let id = ctx.sql("select id from pm_request where title = 'Bancassurance Motor Programme' order by id desc limit 1")[0]?.[0];
    if (!id) {
      const company = Number(ctx.one("select id from org_company where code = 'FVI'"));
      const end = new Date(Date.now() + 8 * 3600 * 1000);
      end.setFullYear(end.getFullYear() + 1);
      const p = await ctx.api('mkttl', 'POST', REQUESTS, {
        companyId: company, type: 'NEW', scope: 'GENERIC', title: 'Bancassurance Motor Programme', lineCode: 'MOTOR',
        coverTypeCode: 'COMPREHENSIVE', marketSegments: [], reason: 'NEW_PROGRAMME', negotiationRequired: true,
        terms: { sections: [{ heading: 'Requested cover and features', text: 'Comprehensive motor cover for bank clients (seed data).' }],
          coverages: [], scheme: {}, dates: { packageEndDate: end.toISOString().slice(0, 10) }, insurers: [{ insurerCode: 'INS-MGIC', terms: [] }] },
      });
      await ctx.api('mkttl', 'POST', `${REQUESTS}/${p.id}/submit`, {});
      id = p.id;
    }
    return act(await openRequest(ctx, 'mkttl', id), /^approve and send to tsu$/i);
  },
  'wt-b-04': async (ctx) => act(await openRequest(ctx, 'tsulead', seedRequest(ctx, 'PKR-2026-900002')), /^return to requester$/i, {
    comment: 'Add the target insurers and the requested deductible.',
  }),
  // 5. A slip prepared and submitted by the TSU Team Lead, who then tries to approve it himself.
  'wt-b-05': async (ctx) => {
    const title = 'Farm Equipment Programme';
    let id = ctx.sql(`select id from pm_request where title = '${title}' order by id desc limit 1`)[0]?.[0];
    if (!id) {
      const company = Number(ctx.one("select id from org_company where code = 'FVI'"));
      const end = new Date(Date.now() + 8 * 3600 * 1000);
      end.setFullYear(end.getFullYear() + 1);
      const p = await ctx.api('tsu', 'POST', REQUESTS, {
        companyId: company, type: 'NEW', scope: 'GENERIC', title, lineCode: 'PROPERTY', coverTypeCode: 'FIRE_LIGHTNING',
        marketSegments: [], reason: 'NEW_PROGRAMME', negotiationRequired: true,
        terms: { sections: [{ heading: 'Requested cover and features', text: 'Fire cover of farm equipment sheds (seed data).' }],
          coverages: [], scheme: {}, dates: { packageEndDate: end.toISOString().slice(0, 10) }, insurers: [{ insurerCode: 'INS-MGIC', terms: [] }] },
      });
      id = p.id;
      await ctx.api('tsu', 'POST', `${REQUESTS}/${id}/submit`, {});
      await ctx.api('mkttl', 'POST', `${REQUESTS}/${id}/approve`, {});
      await ctx.api('tsulead', 'POST', `${REQUESTS}/${id}/recommend`, { text: 'Approach the lead insurer only.' });
      await ctx.api('tsuhead', 'POST', `${REQUESTS}/${id}/tsu-approve`, {});
      await ctx.api('tsulead', 'PUT', `${REQUESTS}/${id}/rounds/1`, { insurers: ['INS-MGIC'], notes: 'Terms as requested.' });
      await ctx.api('tsulead', 'POST', `${REQUESTS}/${id}/rounds/1/quotation-slip/submit`, {});
    }
    const page = await openRequest(ctx, 'tsulead', id, 'Negotiation');
    const approve = button(page, /^approve and send$/i);
    if (await approve.count()) {
      await approve.click();
      await settle(page, 1200);
    }
    return page;
  },
  'wt-b-06': async (ctx) => act(await openRequest(ctx, 'tsu', seedRequest(ctx, 'PKR-2026-900003'), 'Negotiation'), /^terms final$/i, {
    confirm: /^make terms final$/i,
    fill: async (d) => d.locator('input[type=checkbox]').first().check(),
  }),
  'wt-b-07': async (ctx) => {
    const page = await openRequest(ctx, 'mancom', seedRequest(ctx, 'PKR-2026-900004'));
    await act(page, /^return to tsu$/i, { comment: 'Attach the ManCom sheet of the roadside assistance cost.' });
    await tab(page, 'Requirements');
    return page;
  },
  'wt-b-08': async (ctx) => {
    const page = await openRequest(ctx, 'tsu', seedRequest(ctx, 'PKR-2026-900004'), 'Requirements');
    await act(page, /^submit requirements for mancom sign-off$/i);
    return page;
  },
  'wt-b-09': async (ctx) => act(await openRequest(ctx, 'mbs', seedRequest(ctx, 'PKR-2026-900005')), /^return incomplete requirements/i, {
    confirm: /^return to tsu$/i, comment: 'The signed package slip of the renewal is missing.',
  }),
  'wt-b-10': async (ctx) => {
    // The screen shots of SCR-PM-13 submitted the seed draft for validation; it is returned to draft first.
    if (ctx.one("select status from cat_product_version where product_code = 'MTR12' and version_no = 2") !== 'DRAFT') {
      await ctx.api('tsuhead', 'POST', '/catalog/products/MTR12/versions/2/return', { reason: 'Returned for walkthrough B' });
    }
    const page = await go(ctx, 'mbs', '/catalog/products/MTR12/versions/2');
    await page.getByLabel(/^Minimum Premium/).fill('');
    await page.getByLabel(/^Effective From/).fill('2020-01-01');
    await button(page, /^submit for validation$/i).click();
    await settle(page, 800);
    return page;
  },
  'wt-b-11': async (ctx) => {
    const status = ctx.one("select status from cat_product_version where product_code = 'MTR12' and version_no = 2");
    if (status === 'DRAFT') {
      await ctx.api('mbs', 'POST', '/catalog/products/MTR12/versions/2/submit', {});
    }
    const page = await go(ctx, 'tsuhead', '/catalog/products/MTR12/versions/2');
    await act(page, /^return to mbs$/i, { confirm: /^return$/i, comment: 'The rate of Luzon Assurance differs from the signed-off terms.' });
    return page;
  },
  'wt-b-12': async (ctx) => go(ctx, 'mbs', '/catalog/products/MTR12/versions/2'),
};

/** Nothing to prepare: the records of each step come from the seed data or from the earlier steps. */
async function prepare() {}

module.exports = { steps, prepare };
