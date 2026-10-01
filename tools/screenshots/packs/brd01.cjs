// How capture_pack.cjs reaches each screen state of the BRD-01 New Business sign-off pack on the seed profile:
// the record to open for a status (seed database), what is typed into the forms (fictitious seed values), the
// rows selected, the files uploaded, the walkthrough steps and the generated documents.
const walkthrough = require('./brd01_walkthrough.cjs');
const documents = require('./brd01_documents.cjs');

// ------------------------------------------------------------------ helpers

/** Picks the first match of a client look-up (type part of the code or name, then click the match). */
function pickClient(label, text) {
  return async (page) => {
    const input = page.getByLabel(new RegExp(`^${label}`)).first();
    await input.fill(text);
    const option = page.locator('.client-option').filter({ hasText: text }).first();
    await option.waitFor({ timeout: 15000 });
    await option.click();
    await page.waitForTimeout(600);
  };
}

function click(name) {
  return async (page) => {
    await page.getByRole('button', { name: new RegExp(name, 'i') }).first().click();
    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(900);
  };
}

/** Ticks the check box of the first table row whose text matches. */
function tickRow(text) {
  return async (page) => {
    const row = page.locator('table tbody tr').filter({ hasText: new RegExp(text) }).first();
    await row.waitFor({ timeout: 15000 });
    await row.locator('input[type=checkbox]').first().check();
    await page.waitForTimeout(400);
  };
}

const NEXT = click('^next$');

// ------------------------------------------------------------------ forms

const newClient = [
  ['Client type', 'Individual'],
  // A person the walkthroughs do not create, so the form shows no duplicate warning.
  ['Last name', 'Mercado'],
  ['First name', 'Josefina'],
  ['Middle name', 'Alvarez'],
  ['Birth date', '14-Mar-1988'],
  ['Nationality', 'Filipino'],
  ['Civil status', 'Married'],
  ['Occupation', 'Architect'],
  ['TIN', '417-238-990-000'],
  ['ID type', 'Passport'],
  ['ID number', 'P4172399Z'],
  ['E-mail', 'josefina.mercado@seed-client.ph'],
  ['Mobile', '09175550499'],
  ['Street address', '12 Mabini Street, Barangay San Antonio'],
  ['City / municipality', 'Pasig City'],
  ['Province', 'Metro Manila'],
  ['Postal code', '1605'],
  ['Market segment', 'CBG'],
  ['The client banks with the group bank', true],
  ['BDO CIF number', '0041723890'],
  ['Source of funds', 'Salary'],
];

const quotationTerms = [
  pickClient('Client or prospect', 'Garcia'),
  NEXT,
  ['Product', '^PAR01'],
  ['Insurer', 'Mabuhay'],
];

const quotationItems = [
  ...quotationTerms,
  NEXT,
  click('add location'),
  ['Address', '45 Kalayaan Avenue, Barangay Pinyahan'],
  ['City', 'Quezon City'],
  ['Province', 'Metro Manila'],
  ['Occupancy', 'Dwelling'],
  ['Construction class', 'Class 1'],
  ['Description', 'Two-storey concrete residence'],
  ['Sum insured', '4500000'],
];

const accountClient = [pickClient('Client', 'Garcia'), NEXT];
const accountProduct = [
  ['Market segment', 'CBG'],
  ['Product', '^PAR01'],
  ['Source channel', 'Walk-in'],
  ['Insurer', 'Mabuhay'],
  ['Insurer branch', 'Makati'],
];

const fills = {
  new_client: newClient,
  new_client_invalid: [
    ['Last name', 'Bautista'],
    ['First name', 'Carmela'],
    ['TIN', '41723'],
    ['E-mail', 'carmela.bautista'],
    ['Mobile', '0917555'],
  ],
  new_client_duplicate: (ctx) => [
    ['Last name', 'Santos'],
    ['First name', 'Maria Clara'],
    ['TIN', ctx.one("select tin from crm_client where last_name = 'Santos' and tin is not null order by id limit 1")],
    async (page) => page.waitForTimeout(2500),
  ],
  quotation_terms: quotationTerms,
  quotation_items: [...quotationItems, async (page) => page.waitForTimeout(1500)],
  quotation_no_items: [...quotationTerms, NEXT],
  prf_form: [
    pickClient('Client or prospect', 'Pacific Harbor'),
    ['Product line and risk code', '^CAR00'],
    ['Market segment', 'Commercial'],
    ['Period from', '01-Nov-2026'],
    ['Period to', '01-Nov-2027'],
    async (page) => {
      const details = page.getByLabel(/^Details/);
      const texts = [
        'Construction of a four-storey warehouse and office building, Laguna Technopark, Biñan City.',
        'Warehouse and office; contractor Pacific Harbor Logistics Inc. with its sub-contractors.',
        'No claims in the last five years.',
        "Contractor's all risks for the contract works of PHP 85,000,000 and third party liability of PHP 5,000,000.",
      ];
      for (let i = 0; i < texts.length && i < (await details.count()); i += 1) {
        await details.nth(i).fill(texts[i]);
      }
    },
    walkthrough.addItem('Contract works: four-storey warehouse and office building', '85000000'),
    walkthrough.addItem('Construction plant and equipment', '6500000'),
    ['Luzon Assurance Co.', true],
    ['Mabuhay General Insurance Corp.', true],
    ['Visayas Mutual Insurance', true],
  ],
  account_period: [
    ...accountClient,
    ...accountProduct,
    NEXT,
    ['Mortgagee bank', 'BDO Home Loans'],
    ['Loan application no.', 'HL-2026-0417'],
    ['PN numbers', 'PN-0417-2026'],
  ],
  // The same client and location twice: the first draft is saved, the second is refused with the first ARN.
  account_duplicate: (ctx) => {
    const once = [
      pickClient('Client', 'CL-2026-900001'),
      NEXT,
      ...accountProduct,
      NEXT,
      NEXT,
      click('add location'),
      ['Address', '7 Sampaguita Street, Barangay Poblacion'],
      ['City', 'Makati City'],
      NEXT,
      NEXT,
      click('^save draft$'),
      async (page) => page.waitForTimeout(1500),
    ];
    return [
      ...once,
      async (page) => {
        await page.goto(`${ctx.BASE}/accounts/new`);
        await ctx.settle(page);
      },
      ...once,
    ];
  },
  // Two insurer e-policies named by their ARN, uploaded together and matched to the accounts.
  epolicy_bulk: (ctx) => {
    const rows = ctx.sql("select arn from acc_account where status = 'PLACED' order by id limit 2");
    const files = rows.map(([arn], i) => walkthrough.pdf(`${arn}.pdf`, 'Insurer e-policy', [
      `Policy No.: SEED-POL-2026-${String(i + 1).padStart(5, '0')}`, `Account Reference No.: ${arn}`,
      'Seed data for the SIT environment']));
    return [
      async (page) => {
        await page.getByLabel(/^E-policy PDFs/).setInputFiles(files);
        await page.waitForTimeout(600);
      },
      click('^upload and match$'),
      async (page) => page.waitForTimeout(1500),
    ];
  },
};

// ------------------------------------------------------------------ records by status

const opens = {
  // A client in KYC review. Walkthrough C (step 2) returns the seeded one to the Account Officer; when it ran first,
  // the Account Officer submits that client's KYC again (its documents are on file).
  kyc_review: async (ctx) => {
    const q = "select id from crm_client where onboarding_stage = 'KYC_REVIEW' order by id limit 1";
    if (ctx.sql(q).length === 0) {
      const id = ctx.one("select id from crm_client where prospect_code = 'PR-2026-000007' and onboarding_stage = 'PROSPECT'");
      await ctx.api('ao', 'POST', `/crm/clients/${id}/submit-kyc`, { comment: 'Current valid ID uploaded' });
    }
    return `/crm/clients/${ctx.one(q)}`;
  },
  // A quotation for review made by someone other than the approver (the one walkthrough C-03 makes is the approver's).
  // When walkthrough C returned the seeded one, the Account Officer submits the seeded draft QT-2026-900001 again.
  for_review_by_other: async (ctx) => {
    const q = "select id from quo_quotation where status = 'FOR_REVIEW' and created_by <> 'mkttl' order by id limit 1";
    if (ctx.sql(q).length === 0) {
      const id = ctx.one("select id from quo_quotation where quotation_no = 'QT-2026-900001' and status = 'DRAFT'");
      await ctx.api('ao', 'POST', `/quotations/${id}/submit`, { comment: 'Comprehensive cover with acts of nature' });
    }
    return `/quotations/${ctx.one(q)}`;
  },
  approved: (ctx) => `/quotations/${ctx.one("select id from quo_quotation where status = 'APPROVED' order by id limit 1")}`,
  with_tsu: (ctx) => `/proposals/${ctx.one("select id from npk_proposal where status = 'WITH_TSU' order by id limit 1")}`,
  terms_received: (ctx) => `/proposals/${ctx.one("select id from npk_proposal where status = 'TERMS_RECEIVED' order by id limit 1")}`,
  draft_account: (ctx) => `/accounts/${ctx.one("select id from acc_account where status = 'DRAFT' and product_code like 'PAR%' order by id limit 1")}/edit`,
  submitted_account: (ctx) => `/accounts/${ctx.one("select id from acc_account where status = 'SUBMITTED' and product_code like 'PAR%' order by id limit 1")}`,
  awaiting_payment_other_lines: (ctx) => `/placement/accounts/${ctx.one("select arn from acc_account where status = 'AWAITING_PAYMENT' and product_code not like 'PAR%' order by id limit 1")}`,
  placed_with_hold_cover: (ctx) => `/placement/accounts/${ctx.one("select arn from acc_account where hold_cover_status is not null order by id limit 1")}`,
  'open_report:review': (ctx) => `/placement/billing/reports/${ctx.one("select id from plc_payment_report where status = 'REVIEW' order by id limit 1")}`,
  'open_epolicy:review': (ctx) => `/issuance/epolicies/${ctx.one("select id from iss_epolicy where status = 'REVIEW' order by id limit 1")}`,
  'open_booking:first_ready': (ctx) => `/booking/book/${ctx.one("select arn from acc_account a where status = 'POLICY_ISSUED' and not exists (select 1 from bkg_queue q where q.arn = a.arn) order by id desc limit 1")}`,
  'open_invoice:first': (ctx) => `/booking/invoices/${ctx.one("select id from bkg_invoice where status = 'BOOKED' order by id limit 1")}`,
  'open_endorsement:booked_account': (ctx) => `/booking/endorsements/new?arn=${ctx.one("select arn from acc_account where status = 'BOOKED' order by id limit 1")}`,
};

// ------------------------------------------------------------------ selections and uploads

const selects = {
  ready_for_placement: tickRow('ARN-2026-9000(07|10)'),
  first_advice: tickRow('.'),
  first_ready: tickRow('ARN-'),
};

// Extra steps after the standard ones, by slug.
const after = {
  // UX deck: a search that finds nothing.
  'ux-scr-nb-01-empty': async (page) => {
    await page.getByPlaceholder(/^search/i).first().fill('Zamboanga Lighthouse Holdings');
    await page.keyboard.press('Enter');
    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(1200);
  },
  // The Assign dialog of a team leader, from the row action menu of the first item.
  'scr-nb-38-02-assign': async (page) => {
    await walkthrough.rowAction(page.locator('main table tbody tr').first(), /^assign$/i);
    await page.waitForTimeout(800);
  },
  // The terms dialog of the first insurer, from the row action menu.
  'scr-nb-11-05-terms': async (page) => {
    await walkthrough.rowAction(page.locator('main table tbody tr').first(), /^terms$/i);
    await page.waitForTimeout(800);
  },
  // The Close dialog of the first new request, from the row action menu.
  'scr-nb-08-03-close': async (page) => {
    await walkthrough.rowAction(page.locator('main table tbody tr').first(), /^close$/i);
    await page.waitForTimeout(800);
  },
  // The send dialog of the first sent slip (Resend), from the row action menu.
  'scr-nb-20-02-send': async (page) => {
    const row = page.locator('main table tbody tr').filter({ hasText: 'Sent' }).first();
    await walkthrough.rowAction(row, /^resend$/i);
    await page.waitForTimeout(1200);
  },
  // The Match Row dialog of the first unmatched line, from the row action menu.
  'scr-nb-22-02-match': async (page) => {
    await walkthrough.rowAction(page.locator('main table tbody tr').first(), /^match$/i);
    await page.waitForTimeout(800);
  },
  // UX deck: the row action menu of the first request, opened.
  'ux-scr-nb-08-actions': async (page) => {
    await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
    await page.waitForTimeout(400);
  },
};

// Shots kept as the whole window (menu and header give the navigation context); every other shot is cropped to
// its dialog or content area (capture_pack.cjs, cropOf).
const crops = { 'scr-nb-01-01-list': 'full' };

module.exports = { opens, fills, selects, after, crops, custom: walkthrough.bulk, walkthrough: walkthrough.steps, documents: documents.shots,
  prepare: walkthrough.prepare };
