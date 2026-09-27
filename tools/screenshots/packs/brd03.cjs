// How capture_pack.cjs reaches each screen state of the BRD-03 Product Maintenance sign-off pack on the seed profile:
// the record to open for a stage (seed database, V996 / V997, and the requests prepared below through the API), what
// is typed into the forms (fictitious seed values), the walkthrough steps and the generated documents.
const walkthrough = require('./brd03_walkthrough.cjs');
const { render, download } = require('./brd01_documents.cjs');

// ------------------------------------------------------------------ helpers

function click(name) {
  return async (page) => {
    await page.getByRole('button', { name: new RegExp(name, 'i') }).first().click();
    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(900);
  };
}

const companyId = (ctx) => Number(ctx.one("select id from org_company where code = 'FVI'"));
const requestId = (ctx, no) => ctx.one(`select id from pm_request where request_no = '${no}'`);
const requestByTitle = (ctx, title) => ctx.sql(`select id from pm_request where title = '${title}' order by id desc limit 1`)[0]?.[0];

/** Terms of the requests the recipe prepares (fictitious values). */
function terms(insurers) {
  const start = new Date();
  start.setDate(start.getDate() + 45);
  const end = new Date(start);
  end.setFullYear(end.getFullYear() + 1);
  const iso = (d) => d.toISOString().slice(0, 10);
  return {
    sections: [{ heading: 'Target market / client', text: 'Owners of residential condominium units in Metro Manila (seed data).' }],
    coverages: [{ coverageCode: 'FIRE_LIGHTNING', included: true, optional: false, clauseCodes: [], limitAmount: 5000000 }],
    scheme: { defaultRate: 0.25, minimumPremium: 1500, commissionRate: 20, maxSumInsured: 10000000 },
    dates: { effectiveFrom: iso(start), packageStartDate: iso(start), packageEndDate: iso(end) },
    insurers: insurers.map((insurerCode) => ({ insurerCode, terms: [] })),
  };
}

/**
 * Creates a generic property request through the API and moves it to `target` (FOR_MKT_APPROVAL, FOR_TSU_APPROVAL
 * or REQUIREMENTS_PREP), unless a request of that title exists. The users are the seed users of each stage, so
 * the four-eyes rules hold.
 */
async function prepared(ctx, title, target) {
  const found = requestByTitle(ctx, title);
  if (found) {
    return found;
  }
  const base = '/product-maintenance/requests';
  const maker = target === 'REQUIREMENTS_PREP' ? 'tsu' : 'ao';
  const p = await ctx.api(maker, 'POST', base, {
    companyId: companyId(ctx), type: 'NEW', scope: 'GENERIC', title, lineCode: 'PROPERTY',
    coverTypeCode: 'FIRE_LIGHTNING', marketSegments: ['CBG'], reason: 'NEW_PROGRAMME', negotiationRequired: true,
    terms: terms(['INS-MGIC', 'INS-LAC']),
  });
  await ctx.api(maker, 'POST', `${base}/${p.id}/submit`, {});
  if (target === 'FOR_MKT_APPROVAL') {
    return p.id;
  }
  await ctx.api('mkttl', 'POST', `${base}/${p.id}/approve`, { comment: 'Approved for TSU review' });
  await ctx.api('tsulead', 'POST', `${base}/${p.id}/recommend`, { text: 'Approach the two panel insurers; the programme fits the CBG segment.' });
  if (target === 'FOR_TSU_APPROVAL') {
    return p.id;
  }
  await ctx.api('tsuhead', 'POST', `${base}/${p.id}/tsu-approve`, {});
  await ctx.api('tsu', 'PUT', `${base}/${p.id}/rounds/1`, { insurers: ['INS-MGIC', 'INS-LAC'], notes: 'Terms as requested.' });
  await ctx.api('tsu', 'POST', `${base}/${p.id}/rounds/1/quotation-slip/submit`, {});
  await ctx.api('tsulead', 'POST', `${base}/${p.id}/rounds/1/quotation-slip/approve`);
  const [round] = await ctx.api('tsu', 'GET', `${base}/${p.id}/rounds`);
  for (const r of round.responses) {
    const offer = r.insurerCode === 'INS-MGIC';
    await ctx.api('tsu', 'PUT', `${base}/${p.id}/responses/${r.id}`, {
      outcome: offer ? 'ACCEPTED_AS_REQUESTED' : 'DECLINED', rate: offer ? 0.25 : undefined,
      minimumPremium: offer ? 1500 : undefined, coverages: [], remarks: offer ? 'As requested' : 'No capacity',
    });
  }
  await ctx.api('tsu', 'POST', `${base}/${p.id}/terms-final`, { insurers: [{ insurerCode: 'INS-MGIC', role: 'PANEL' }] });
  await ctx.api('tsuhead', 'POST', `${base}/${p.id}/skip-marketing-review`, {});
  return p.id;
}

// ------------------------------------------------------------------ forms

const newRequest = [
  ['Request type', 'New package'],
  ['Scope', 'Generic programme'],
  ['Package / programme name', 'Motor Fleet Plus'],
  ['Product line', 'Motor'],
  ['Cover type / subtype', 'Comprehensive'],
  ['Reason', 'New programme'],
  ['Comment on the reason', 'SME fleets of 5 to 50 vehicles (seed data)'],
  ['Requested rate %', '1.2'],
  ['Minimum premium', '5000'],
  ['Commission %', '15'],
  ['Package TSI limit', '5000000'],
  click('^add coverage$'),
  ['Coverage 1 code', 'OD_THEFT'],
  ['Coverage 1 limit', '2000000'],
  ['Coverage 1 deductible', 'PHP 2,000 per claim'],
  ['Mabuhay General Insurance Corp.', true],
  ['Luzon Assurance Co.', true],
];

const fills = {
  new_request: newRequest,
  new_request_invalid: [['Comment on the reason', 'Name, line and reason left out']],
  version_invalid: [['Minimum Premium', ''], ['Effective From', '01-Jan-2020']],
  calculator_package: [['Product', '^MTR12'], ['Insurer', 'Mabuhay'], ['Sum insured', '1500000']],
};

// ------------------------------------------------------------------ records by stage

const opens = {
  for_mkt_approval: async (ctx) => `/product-maintenance/requests/${await prepared(ctx, 'Home Contents Programme', 'FOR_MKT_APPROVAL')}`,
  for_tsu_approval: async (ctx) => `/product-maintenance/requests/${await prepared(ctx, 'Condominium Unit Owners Programme', 'FOR_TSU_APPROVAL')}`,
  requirements_prep: async (ctx) => `/product-maintenance/requests/${await prepared(ctx, 'SME Office Property Programme', 'REQUIREMENTS_PREP')}`,
  negotiation: (ctx) => `/product-maintenance/requests/${requestId(ctx, 'PKR-2026-900003')}`,
  // The same request with a current comparative master, so that a client view can be generated.
  negotiation_master: async (ctx) => {
    const id = requestId(ctx, 'PKR-2026-900003');
    if (ctx.sql(`select 1 from pm_comparative_output where request_id = ${id} and kind = 'MASTER' and is_current`).length === 0) {
      await ctx.api('tsu', 'POST', `/product-maintenance/requests/${id}/comparatives/master`);
    }
    return `/product-maintenance/requests/${id}`;
  },
  for_mancom: (ctx) => `/product-maintenance/requests/${requestId(ctx, 'PKR-2026-900004')}`,
  with_mbs: (ctx) => `/product-maintenance/requests/${requestId(ctx, 'PKR-2026-900005')}`,
  released: (ctx) => `/product-maintenance/requests/${requestId(ctx, 'PKR-2026-900006')}`,
  packaged_product: () => '/catalog/products/MTR12',
  released_product: () => '/catalog/products/PAR25',
  draft_version: () => '/catalog/products/MTR12/versions/2',
  version_for_validation: async (ctx) => {
    const row = ctx.sql("select product_code, version_no from cat_product_version where status = 'FOR_VALIDATION' order by id limit 1")[0];
    if (row) {
      return `/catalog/products/${row[0]}/versions/${row[1]}`;
    }
    // The seed draft of MTR12 version 2 is submitted by its maker, so the TSU Head can validate it.
    await ctx.api('mbs', 'POST', '/catalog/products/MTR12/versions/2/submit', {});
    return '/catalog/products/MTR12/versions/2';
  },
  released_version: () => '/catalog/products/MTR12/versions/1',
  first_insurer: (ctx) => `/catalog/insurers/${ctx.one("select id from cat_insurer where party_code = 'INS-MGIC' order by id limit 1")}`,
  // A rate exception of the walkthrough A quotation waiting for approval: the one of step 22 while it is pending,
  // otherwise a second request of the Account Officer on the same quotation (seed values).
  rate_exception_pending: async (ctx) => {
    const pending = ctx.sql("select reference_no from cat_rate_scheme_exception where product_code = 'MTR30' and record_status = 'PENDING_AUTHORIZATION' order by id desc limit 1")[0];
    if (pending) {
      return `/catalog/rate-exceptions/${pending[0]}`;
    }
    const q = ctx.sql("select quotation_no from quo_quotation where product_code = 'MTR30' order by id desc limit 1")[0];
    if (!q) {
      throw new Error('run walkthrough A up to step 22 first: no quotation of MTR30');
    }
    const created = await ctx.api('ao', 'POST', '/catalog/rate-scheme-exceptions', {
      productCode: 'MTR30', requestedRate: 1.05, transactionRef: q[0],
      reason: 'Fleet grows to 40 vehicles at renewal of the client programme (seed data).',
    });
    return `/catalog/rate-exceptions/${created.referenceNo}`;
  },
  quotation_deviation: (ctx) => {
    // The quotation of walkthrough A (package MTR30 with an item rate other than the scheme rate).
    const row = ctx.sql("select id from quo_quotation where product_code = 'MTR30' and status = 'DRAFT' order by id desc limit 1")[0];
    if (!row) {
      throw new Error('run walkthrough A up to step 22 first: no draft quotation of MTR30');
    }
    return `/quotations/${row[0]}`;
  },
};

// ------------------------------------------------------------------ generated documents

const docs = {
  'doc-request-form': (ctx, out) => download(ctx, 'ao', `/product-maintenance/requests/${requestId(ctx, 'PKR-2026-900003')}/form.pdf`, out),
  'doc-quotation-slip': (ctx, out) => download(ctx, 'tsu', `/product-maintenance/requests/${requestId(ctx, 'PKR-2026-900003')}/rounds/2/quotation-slip.pdf`, out),
  'doc-comparative': async (ctx, out) => {
    const id = requestId(ctx, 'PKR-2026-900003');
    let master = ctx.sql(`select id from pm_comparative_output where request_id = ${id} and kind = 'MASTER' and is_current order by id desc limit 1`)[0]?.[0];
    if (!master) {
      master = (await ctx.api('tsu', 'POST', `/product-maintenance/requests/${id}/comparatives/master`)).id;
    }
    await download(ctx, 'tsu', `/product-maintenance/requests/${id}/comparatives/${master}.pdf`, out);
  },
  'doc-package-slip': (ctx, out) => download(ctx, 'tsu', `/product-maintenance/requests/${requestId(ctx, 'PKR-2026-900004')}/package-slip.pdf`, out),
  'doc-mancom-signoff': (ctx, out) => {
    const id = ctx.one("select id from doc_attachment where entity_type = 'PackageRequest' and document_type = 'MANCOM_SIGNOFF' and not deleted order by id desc limit 1");
    return download(ctx, 'tsu', `/attachments/${id}/content`, out, 'pdf');
  },
  'doc-advisory': (ctx, out) => {
    const id = ctx.one("select id from doc_attachment where entity_type = 'PackageRequest' and document_type = 'PKG_ADVISORY' and not deleted order by id desc limit 1");
    return download(ctx, 'tsu', `/attachments/${id}/content`, out, 'pdf');
  },
};

// ------------------------------------------------------------------ callout scopes

// Where the callout of a field goes when the same text is on several parts of the page (capture_pack.cjs): the rate
// scheme panel of the quotation, the Request Rate Exception dialog (its "Valid until", not the quotation's), the
// exception record and its Approve and Reject confirmations. A field outside its scope gets no badge in that state.
const panel = { within: '.alert[role=status]', title: 'Priced on package version' };
const requestDialog = { within: 'dialog[open]', title: 'Request Rate Exception' };
const record = { within: 'main', title: 'Product Maintenance · Rate Exception' };
const callouts = {
  'SCR-PM-22': {
    1: { ...panel, target: 'span' },
    2: { ...panel, target: 'a[href*="/catalog/rate-exceptions/"]' },
    3: requestDialog,
    4: requestDialog,
    5: requestDialog,
    6: record,
    7: record,
    8: record,
    9: record,
    10: record,
    11: record,
    12: record,
    13: { within: 'dialog[open]', title: 'Approve Rate Exception' },
    14: { within: 'dialog[open]', title: 'Reject Rate Exception' },
  },
};

module.exports = {
  opens, fills, selects: {}, uploads: {}, after: {}, custom: {}, walkthrough: walkthrough.steps, documents: docs, callouts,
  prepare: walkthrough.prepare, render,
};
