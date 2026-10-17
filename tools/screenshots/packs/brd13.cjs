// How capture_pack.cjs reaches each screen state of the BRD-13 Data Migration sign-off set on the seed profile: the
// record to open for a status (the migration storyline of the seed data: extracts, batches, reconciliation, cut-over
// plans, archive records and legacy invoices), the states reached through several actions (a rejected extract, the
// waiver and break dialogs, the go / no-go criteria of a plan, the reason asked by Legacy Inquiry) and the legacy
// batches of Cashiering and Commission, opened as drafts when the seed data has none. The SIT/UAT password is read
// from the environment (SEED_PASSWORD), never from a file.
const fs = require('fs');
const os = require('os');
const path = require('path');

const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'brd13-pack-'));

// ------------------------------------------------------------------ helpers

const companyId = (ctx) => Number(ctx.one("select id from org_company where code = 'FVI'"));

/** The first value of a query, or an error that names the seed state the shot needs. */
function need(ctx, query, what) {
  const rows = ctx.sql(query);
  if (rows.length === 0 || rows[0][0] === null || rows[0][0] === '') {
    throw new Error(`the seed data has no ${what}`);
  }
  return rows[0][0];
}

function csv(name, rows) {
  const file = path.join(TMP, name);
  const cell = (v) => (/[",\n]/.test(String(v)) ? `"${String(v).replace(/"/g, '""')}"` : String(v));
  fs.writeFileSync(file, rows.map((r) => r.map(cell).join(',')).join('\n') + '\n');
  return file;
}

const today = () => new Date(Date.now() + 8 * 3600 * 1000).toISOString().slice(0, 10); // Philippine date

async function click(page, name, ctx) {
  await page.getByRole('button', { name }).first().click();
  await ctx.settle(page, 800);
}

/** The Legacy Inquiry page of a fresh session, with the reason given (asked once per browser session). */
async function inquiryWithReason(ctx) {
  const page = await ctx.pageOf('legacyaudit');
  await page.goto(`${ctx.BASE}/legacy-inquiry`);
  await ctx.settle(page);
  const reason = page.getByLabel(/^Reason\s*\*?$/);
  if (await reason.isVisible().catch(() => false)) {
    await ctx.fillField(page, 'Reason', 'Internal or external audit');
    await ctx.fillField(page, 'Details', 'Audit request AR-2028-014 (seed data)');
    await click(page, /^continue$/i, ctx);
  }
  return page;
}

/** A legacy invoice of the Operations ledger (origin Migrated). */
const legacyInvoice = (ctx) => need(ctx,
  "select invoice_no from ops_invoice where origin = 'MIGRATED' and ledger_context = 'LEGACY' order by id limit 1",
  'legacy invoice in the Operations ledger');

// ------------------------------------------------------------------ records by status

const opens = {
  object_c01: () => '/migration/objects?object=C01',
  layout_f01c: () => '/migration/layouts',
  map_insurer: (ctx) => `/migration/maps?set=${encodeURIComponent(need(ctx,
    "select code from mig_code_map_set where code like 'INSURER%' order by length(code), code limit 1", 'insurer code map'))}`,
  // A map set with a draft version (the Add Entry button is on drafts only).
  map_draft: (ctx) => `/migration/maps?set=${encodeURIComponent(need(ctx,
    "select set_code from mig_code_map_version where status = 'DRAFT' order by set_code limit 1",
    'code map with a draft version'))}`,
  batch_validated: (ctx) => `/migration/batches/${need(ctx,
    "select batch_no from mig_batch where status = 'VALIDATED' order by (invalid_count > 0) desc, id limit 1", 'validated batch')}`,
  batch_accepted: (ctx) => `/migration/batches/${need(ctx,
    "select batch_no from mig_batch where status = 'SIGNED_OFF' order by (object_code = 'F01') desc, id limit 1", 'accepted batch')}`,
  pair_first: () => '/migration/matching',
  recon_batch: (ctx) => `/migration/reconciliation?batch=${need(ctx,
    "select b.batch_no from mig_batch b join mig_recon_run r on r.batch_id = b.id order by (b.object_code = 'F01') desc, b.id limit 1",
    'reconciled batch')}`,
  recon_break: (ctx) => `/migration/reconciliation?batch=${need(ctx,
    "select b.batch_no from mig_batch b join mig_recon_run r on r.batch_id = b.id join mig_recon_line l on l.run_id = r.id where l.status = 'BREAK' order by b.id limit 1",
    'reconciliation line with a break')}`,
  plan_mock: (ctx) => `/migration/cutover?plan=${need(ctx,
    "select plan_no from mig_cutover_plan where kind = 'MOCK' order by id limit 1", 'mock run plan')}`,
  invoice_legacy: (ctx) => `/operations/invoices/${legacyInvoice(ctx)}`,
  income_draft: (ctx) => `/cashiering/legacy-batches/${ctx.state.incomeDraft}`,
  income_top: (ctx) => `/cashiering/legacy-batches/${ctx.state.incomeTop}`,
  pr2307_draft: (ctx) => `/cashiering/legacy-batches/${ctx.state.pr2307Draft}`,
  dppr_draft: (ctx) => `/commission/dppr-batches/${ctx.state.dpprDraft}`,
};

// ------------------------------------------------------------------ states reached through several actions

const custom = {
  // An extract whose control file announces more rows than the file holds: rejected at intake with its reason.
  'scr-dm-05-02-rejected': async (ctx) => {
    const columns = ctx.sql(
      "select c.name from mig_layout_column c join mig_layout l on l.id = c.layout_id where l.code = 'C01' and l.status = 'FROZEN' order by c.seq",
    ).map((r) => r[0]);
    const asOf = '2028-01-03';
    const nn = String(Number(ctx.one('select count(*) from mig_extract')) % 90 + 10);
    const name = `C01_QPS_${asOf.replace(/-/g, '')}_${nn}.csv`;
    const data = csv(name, [columns, columns.map(() => ''), columns.map(() => '')]);
    const head = ['C01', 'C01', 'QPS', name, `${asOf} 18:00:00`, `${asOf} 19:00:00`, 'bdoi.it.extract'];
    const control = csv(`${name.replace('.csv', '')}.ctl.csv`, [
      ['object', 'layout', 'source_system', 'data_file', 'as_of', 'extracted_at', 'extracted_by', 'measure',
        'column_name', 'currency', 'filter', 'value'],
      [...head, 'ROW_COUNT', '', '', '', '3'],
    ]);
    const page = await ctx.pageOf('migops');
    await page.goto(`${ctx.BASE}/migration/extracts`);
    await ctx.settle(page);
    const inputs = page.locator('main input[type=file]');
    await inputs.nth(0).setInputFiles(data);
    await inputs.nth(1).setInputFiles(control);
    await page.waitForTimeout(500);
    await click(page, /^upload and check$/i, ctx);
    await ctx.settle(page, 1500);
    return page;
  },
  // The first failing row of a validated batch selected, and the Waive Rows dialog opened.
  'scr-dm-07-03-waive': async (ctx, shot) => {
    const page = await ctx.pageOf('migowner');
    await page.goto(ctx.BASE + opens.batch_validated(ctx, shot));
    await ctx.settle(page);
    await ctx.openTab(page, 'Rows');
    await page.locator('main table tbody tr input[type=checkbox]').first().check();
    await page.waitForTimeout(300);
    await click(page, /^waive rows$/i, ctx);
    return page;
  },
  // A break line of a reconciliation and the Explain Break dialog.
  'scr-dm-09-02-explain': async (ctx, shot) => {
    const page = await ctx.pageOf('migsteward');
    await page.goto(ctx.BASE + opens.recon_break(ctx, shot));
    await ctx.settle(page);
    await click(page, /^explain$/i, ctx);
    return page;
  },
  // The go / no-go criteria of the mock run, measured, as the board member sees them.
  'scr-dm-12-02-criteria': async (ctx, shot) => {
    const page = await ctx.pageOf('miggonogo');
    await page.goto(ctx.BASE + opens.plan_mock(ctx, shot));
    await ctx.settle(page);
    // The runbook table of the plan is shown on scr-dm-12-01; this shot keeps the criteria and decisions in view.
    await page.evaluate(() => {
      const h = [...document.querySelectorAll('main h3')].find((e) => e.textContent.trim() === 'Runbook');
      if (h && h.nextElementSibling) {
        h.nextElementSibling.style.display = 'none';
        h.style.display = 'none';
      }
    });
    await page.waitForTimeout(400);
    return page;
  },
  // The reason card, asked in a new browser session before the archive opens.
  'scr-dm-14-01-reason': async (ctx) => {
    const context = await ctx.browser.newContext({ viewport: { width: 1600, height: 1000 } });
    const page = await context.newPage();
    await page.goto(`${ctx.BASE}/login`);
    await ctx.settle(page, 300);
    await page.getByLabel('User ID').fill('legacyaudit');
    await page.getByLabel('Password').fill(process.env.SEED_PASSWORD);
    await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
    await ctx.settle(page, 1200);
    await page.goto(`${ctx.BASE}/legacy-inquiry`);
    await ctx.settle(page);
    return page;
  },
  // A search for the closed invoices of the archive.
  'scr-dm-14-02-results': async (ctx) => {
    const page = await inquiryWithReason(ctx);
    await ctx.fillField(page, 'Record Type', 'Invoice');
    await click(page, /^search$/i, ctx);
    return page;
  },
  // UX deck: a search that finds nothing (the empty state of the results).
  'ux-scr-dm-14-no-results': async (ctx) => {
    const page = await inquiryWithReason(ctx);
    await ctx.fillField(page, 'Invoice No.', 'I00000000');
    await click(page, /^search$/i, ctx);
    return page;
  },
  // One archived invoice opened with its legacy details and documents.
  'scr-dm-14-03-record': async (ctx) => {
    const page = await inquiryWithReason(ctx);
    const invoice = need(ctx, "select invoice_no from mig_archive_record where record_type = 'INVOICE' and invoice_no is not null order by id limit 1",
      'archived invoice');
    await ctx.fillField(page, 'Invoice No.', invoice);
    await click(page, /^search$/i, ctx);
    await page.locator('main table tbody tr').first().click();
    await ctx.settle(page, 800);
    return page;
  },
};

// After the page is open: a row picked on the screens that select a record by clicking it.
const after = {
  'scr-dm-04-02-columns': async (page, ctx) => {
    await page.locator('main table tbody tr').filter({ hasText: 'F01C' }).first().click();
    await ctx.settle(page, 800);
  },
  'scr-dm-08-02-pair': async (page, ctx) => {
    await page.locator('main table tbody tr').first().click();
    await ctx.settle(page, 800);
  },
  // The clients list searched for the migrated client of the seed data, marked LEGACY.
  'scr-dm-16-01-clients': async (page, ctx) => {
    const last = ctx.one("select last_name from crm_client where origin = 'MIGRATED' order by id limit 1");
    await page.getByPlaceholder(/search client name/i).fill(last);
    await click(page, /^search$/i, ctx);
  },
  'scr-dm-16-02-unapplied': async (page, ctx) => {
    await ctx.fillField(page, 'Origin', 'Migrated').catch(() => {});
    await ctx.settle(page, 600);
  },
};

// ------------------------------------------------------------------ the legacy batches

/** A C01 extract of three clients uploaded through the intake with its control file (row count, distinct
 * clients, SHA-256), planned as a batch and validated: one valid client, one with a birth date and a market segment
 * that fail their rules, and one with the name and city of the first but another birth date (a pair to review). */
async function validatedClientBatch(ctx, company) {
  const crypto = require('crypto');
  // A delta after the seed extracts of 31-Dec-2027 (the as-of dates of an object only move forward).
  const asOf = '2028-01-02';
  const name = `C01_EBIX_${asOf.replace(/-/g, '')}_01.csv`;
  // The columns of the layout in force, in order; the values not given stay empty.
  const header = ctx.sql("select c.name from mig_layout_column c join mig_layout l on l.id = c.layout_id "
    + "where l.code = 'C01' and l.status = 'FROZEN' order by c.seq").map((r) => r[0]);
  const client = (v) => header.map((h) => v[h] ?? '');
  const base = { client_type: 'I', kyc_status: 'COMPLETE', client_status: 'A' };
  const rows = [
    client({ ...base, legacy_client_no: 'E970002', last_name: 'Dela Cruz', first_name: 'Ramon', birth_date: '1985-02-11', tin: '214-556-871-000', city: 'Pasig',
      market_segment: 'CBG', email: 'ramon.delacruz@brokerverse-seed.ph', created_date: '2021-06-01', last_updated: '2027-11-15 10:00:00' }),
    client({ ...base, legacy_client_no: 'E970003', last_name: 'Santos', first_name: 'Lorna', birth_date: '1979-13-40',
      market_segment: 'OLD-RET', email: 'lorna.santos@brokerverse-seed.ph', created_date: '2020-02-01', last_updated: '2027-11-20 11:00:00' }),
    client({ ...base, legacy_client_no: 'E970004', last_name: 'Dela Cruz', first_name: 'Ramon', birth_date: '1985-12-02', city: 'Pasig',
      market_segment: 'CBG', email: 'rdelacruz@brokerverse-seed.ph', created_date: '2020-08-01', last_updated: '2027-12-01 09:00:00' }),
  ];
  const data = csv(name, [header, ...rows]);
  const content = fs.readFileSync(data);
  const head = ['C01', 'C01', 'EBIX', name, `${asOf} 18:00:00`, `${asOf} 19:00:00`, 'bdoi.it.extract'];
  const control = csv(name.replace('.csv', '.ctl.csv'), [
    ['object', 'layout', 'source_system', 'data_file', 'as_of', 'extracted_at', 'extracted_by', 'measure',
      'column_name', 'currency', 'filter', 'value'],
    [...head, 'ROW_COUNT', '', '', '', String(rows.length)],
    [...head, 'HASH_TOTAL', '', '', '', String(rows.length)],
    [...head, 'SHA256', '', '', '', crypto.createHash('sha256').update(content).digest('hex')],
  ]);
  const form = new FormData();
  form.append('file', new Blob([content]), name);
  form.append('control', new Blob([fs.readFileSync(control)]), path.basename(control));
  const extract = await ctx.api('migops', 'POST', `/migration/extracts?companyId=${company}&objectCode=C01&mode=DELTA`, form);
  const batch = await ctx.api('migops', 'POST', `/migration/batches?companyId=${company}`,
    { objectCode: 'C01', extractNos: [extract.extractNo] });
  await ctx.api('migops', 'POST', `/migration/batches/${batch.batchNo}/validate`, {});
  return batch.batchNo;
}

/** Seed states of the migration console that the storyline of the seed data does not reach: a draft version of the
 * insurer code map, a validated client batch with failing rows and a pair to review, and one reconciliation line of
 * the unapplied payments with a difference to explain (a break on the seed run, for the Explain Break dialog). */
async function prepareConsole(ctx, company) {
  if (ctx.sql("select 1 from mig_code_map_version where status = 'DRAFT'").length === 0) {
    await ctx.api('migsteward', 'POST', `/migration/maps/INSURER/versions?companyId=${company}`,
      { copyApproved: true, comment: 'Insurer codes of the QPS extract (seed data)' });
  }
  if (ctx.sql("select 1 from mig_batch where status = 'VALIDATED'").length === 0) {
    await validatedClientBatch(ctx, company);
  }
  if (ctx.sql("select 1 from mig_recon_line where status = 'BREAK'").length === 0) {
    ctx.sql("update mig_recon_line set status = 'BREAK', target_value = source_value - 150.00, difference = 150.00 "
      + "where id = (select l.id from mig_recon_line l join mig_recon_run r on r.id = l.run_id join mig_batch b "
      + "on b.id = r.batch_id where b.object_code = 'F02' and l.level = 'L5' and l.source_value > 150 order by l.id desc limit 1)");
  }
}

/** Draft and pending batches of Cashiering and Commission, created through the services when the seed has none. */
async function prepare(ctx) {
  const company = companyId(ctx);
  await prepareConsole(ctx, company);
  const existing = (query) => ctx.sql(query)[0]?.[0];
  const incomeDraft = existing("select batch_no from csh_legacy_batch where kind = 'INCOME_RECLASS' and status = 'DRAFT' and line_count > 0 order by id limit 1");
  // Migrated unapplied payments first; when automatch has applied them all, any unapplied payment of the seed data.
  const candidates = async () => {
    const url = `/cashiering/legacy-batches/candidates?companyId=${company}&minAgeDays=0`;
    const migrated = await ctx.api('cashier', 'GET', `${url}&origin=MIGRATED`);
    return migrated.length > 1 ? migrated : [...migrated, ...(await ctx.api('cashier', 'GET', url))];
  };
  const incomeBatch = async (lines) => {
    const b = await ctx.api('cashier', 'POST', `/cashiering/legacy-batches?companyId=${company}`, {
      kind: 'INCOME_RECLASS', reason: 'Unclaimed legacy payments after follow-up (seed data)', currency: 'PHP',
    });
    for (const c of lines) {
      await ctx.api('cashier', 'POST', `/cashiering/legacy-batches/${b.batchNo}/items`,
        { unappliedId: c.id, reason: 'Unclaimed after two years of follow-up' });
    }
    return b.batchNo;
  };
  if (incomeDraft) {
    ctx.state.incomeDraft = incomeDraft;
  } else {
    const c = await candidates();
    ctx.state.incomeDraft = await incomeBatch(c.slice(0, 2));
  }
  const incomeTop = existing("select batch_no from csh_legacy_batch where kind = 'INCOME_RECLASS' and status = 'FOR_TOP_MANAGEMENT' order by id limit 1");
  if (incomeTop) {
    ctx.state.incomeTop = incomeTop;
  } else {
    const c = await candidates();
    const no = await incomeBatch(c.slice(2, 4).length ? c.slice(2, 4) : c.slice(0, 1));
    await ctx.api('cashier', 'POST', `/cashiering/legacy-batches/${no}/submit`, { comment: 'For approval' });
    await ctx.api('cashtl', 'POST', `/cashiering/legacy-batches/${no}/approve`, { comment: 'Checked against the follow-up log' });
    ctx.state.incomeTop = no;
  }
  const pr2307 = existing("select batch_no from csh_legacy_batch where kind = 'PR2307_REVERSAL' and status = 'DRAFT' order by id limit 1");
  ctx.state.pr2307Draft = pr2307 || (await ctx.api('cashier', 'POST', `/cashiering/legacy-batches?companyId=${company}`, {
    kind: 'PR2307_REVERSAL', reason: 'Legacy PR 2307 settled with the insurers (seed data)', currency: 'PHP',
  })).batchNo;
  const dppr = existing("select batch_no from cmr_dppr_batch where status = 'DRAFT' order by id limit 1");
  ctx.state.dpprDraft = dppr || (await ctx.api('commrec', 'POST', `/commission/dppr-batches?companyId=${company}`, {
    comment: 'Legacy invoices paid directly to the insurers (seed data)',
  })).batchNo;
}

// The region of a shot when it is not the whole content area.
const crops = {
  // The invoice header and the Legacy Invoice card.
  'scr-dm-17-01-legacy': 'main h1, main section.card:has(> header:has-text("Legacy Invoice"))',
};

module.exports = { opens, fills: {}, selects: {}, uploads: {}, after, custom, crops, walkthrough: {}, documents: {}, prepare };
