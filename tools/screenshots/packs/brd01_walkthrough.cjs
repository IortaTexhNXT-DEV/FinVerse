// End-to-end walkthroughs of BRD-01 New Business, performed live on the seed profile by capture_pack.cjs (see
// walkthroughs.yaml of the pack). Each step signs in as the persona of the step, does what the step says on the
// screen and returns the page to capture. The records created carry fictitious seed values only.
const fs = require('fs');
const os = require('os');
const path = require('path');

const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'nb-pack-'));

// ------------------------------------------------------------------ files

/** A one-page PDF with a title and lines of text (Helvetica, real text so it can be read back). */
function pdf(name, title, lines) {
  const file = path.join(TMP, name);
  const latin = (t) => String(t).replace(/[\u2013\u2014]/g, '-').replace(/[^\x20-\xff]/g, '');
  const esc = (t) => latin(t).replace(/[\\()]/g, (c) => `\\${c}`);
  const text = ['BT', '/F1 16 Tf', '64 770 Td', `(${esc(title)}) Tj`, '/F1 11 Tf', '0 -32 Td'];
  lines.forEach((l) => text.push(`(${esc(l)}) Tj`, '0 -18 Td'));
  text.push('ET');
  const stream = text.join('\n');
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>',
    `<< /Length ${Buffer.byteLength(stream, 'latin1')} >>\nstream\n${stream}\nendstream`,
  ];
  let out = '%PDF-1.4\n';
  const offsets = [];
  objects.forEach((o, i) => {
    offsets.push(Buffer.byteLength(out, 'latin1'));
    out += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = Buffer.byteLength(out, 'latin1');
  out += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  out += offsets.map((o) => `${String(o).padStart(10, '0')} 00000 n \n`).join('');
  out += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  fs.writeFileSync(file, Buffer.from(out, 'latin1'));
  return file;
}

function csv(name, rows) {
  const file = path.join(TMP, name);
  const cell = (v) => (/[",\n]/.test(String(v)) ? `"${String(v).replace(/"/g, '""')}"` : String(v));
  fs.writeFileSync(file, rows.map((r) => r.map(cell).join(',')).join('\r\n') + '\r\n');
  return file;
}

const CLIENT_HEADERS = ['Client Code', 'Client Type', 'Last Name', 'First Name', 'Middle Name', 'Corporate Name',
  'Birth Date', 'TIN', 'ID Type', 'ID Number', 'E-mail', 'Mobile', 'Address', 'City', 'Province', 'Postal Code',
  'Market Segment', 'Bank Client', 'Bank CIF', 'Nationality', 'Source of Funds', 'Risk Rating'];

/** Fictitious clients for the bulk upload; `errors` makes rows 3 and 4 invalid (date format, missing type). */
function clientRows(ctx, errors, runNo) {
  const n = String(runNo).padStart(2, '0');
  const rows = [
    ['', 'INDIVIDUAL', 'Dela Paz', 'Rosario', 'Villareal', '', '1984-07-21', `512-3${n}-101-000`, 'PASSPORT', `P51231${n}01`, `rosario.delapaz${n}@seed-client.ph`, '09175551201', '8 Sampaguita Street, Barangay Malamig', 'Mandaluyong City', 'Metro Manila', '1550', 'CBG', 'Y', '', 'FILIPINO', 'SALARY', 'STANDARD'],
    ['', 'CORPORATE', '', '', '', `Tanglaw Printing Services ${n} Inc.`, '', `512-3${n}-102-000`, '', '', `accounts${n}@tanglawprinting.example`, '09175551202', 'Unit 5, 21 Shaw Boulevard', 'Pasig City', 'Metro Manila', '1603', 'COMBANK', 'N', '', '', '', 'STANDARD'],
    ['', 'INDIVIDUAL', 'Macaraeg', 'Leonora', 'Santos', '', errors ? '21/09/1979' : '1979-09-21', `512-3${n}-103-000`, 'PASSPORT', `P51231${n}03`, `leonora.macaraeg${n}@seed-client.ph`, '09175551203', '44 Kamagong Street, San Antonio Village', 'Makati City', 'Metro Manila', '1203', 'CBG', 'N', '', 'FILIPINO', 'BUSINESS', 'STANDARD'],
    ['', errors ? '' : 'INDIVIDUAL', 'Buenaventura', 'Ramil', 'Ocampo', '', '1990-02-11', `512-3${n}-104-000`, 'PASSPORT', `P51231${n}04`, `ramil.buenaventura${n}@seed-client.ph`, '09175551204', '17 Molave Road, Project 3', 'Quezon City', 'Metro Manila', '1102', 'CBG', 'Y', '', 'FILIPINO', 'SALARY', 'STANDARD'],
    ['', 'INDIVIDUAL', 'Galang', 'Theresa', 'Manalo', '', '1987-12-03', `512-3${n}-105-000`, 'PASSPORT', `P51231${n}05`, `theresa.galang${n}@seed-client.ph`, '09175551205', '9 Acacia Drive, Barangay Lahug', 'Cebu City', 'Cebu', '6000', 'RETAIL', 'N', '', 'FILIPINO', 'SALARY', 'STANDARD'],
  ];
  return rows;
}

/** The client bulk file: all five rows (two with errors), or only the two corrected rows. */
async function bulkClientFile(ctx, corrected) {
  const runNo = ctx.state.bulkRun ?? (ctx.state.bulkRun = Number(ctx.one('select count(*) from bulk_job')) + 1);
  const rows = clientRows(ctx, !corrected, runNo);
  const data = corrected ? rows.slice(2, 4) : rows;
  return csv(corrected ? `clients-corrected-${runNo}.csv` : `clients-${runNo}.csv`, [CLIENT_HEADERS, ...data]);
}

// ------------------------------------------------------------------ page helpers

async function settle(page, ms = 900) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(ms);
}

async function go(ctx, user, url) {
  const page = await ctx.pageOf(user);
  await page.goto(ctx.BASE + url);
  await settle(page);
  return page;
}

/** A button, or a link shown as a button, by its name. */
function button(scope, name) {
  const re = name instanceof RegExp ? name : new RegExp(`^${name}$`, 'i');
  return scope.getByRole('button', { name: re }).or(scope.getByRole('link', { name: re })).first();
}

/**
 * Clicks an action and completes its dialog when one opens: the reason (first value, or the one asked), the
 * comment, then the dialog's confirm button (same name, or the last button of the dialog).
 */
async function act(page, name, opts = {}) {
  const target = button(page, name);
  try {
    await target.waitFor({ state: 'visible', timeout: 10000 });
  } catch {
    const names = await page.locator('main button:visible').allInnerTexts();
    throw new Error(`no button ${name} on ${page.url()}; buttons: ${names.map((n) => n.trim()).filter(Boolean).join(' | ')}`);
  }
  await target.click();
  await page.waitForTimeout(700);
  const dialog = page.locator('dialog.modal[open]').last();
  if (await dialog.isVisible().catch(() => false)) {
    if (opts.fill) {
      await opts.fill(dialog);
    }
    const reason = dialog.locator('select').first();
    if (opts.reason !== false && (await reason.count()) > 0) {
      const options = await reason.locator('option').allTextContents();
      const pick = opts.reason ? options.find((o) => new RegExp(opts.reason, 'i').test(o)) : options.find((o, i) => i > 0);
      if (pick) {
        await reason.selectOption({ label: pick });
      }
    }
    if (opts.comment) {
      await dialog.locator('textarea').first().fill(opts.comment);
    }
    if (opts.keepOpen) {
      return page;
    }
    const confirm = dialog.getByRole('button', { name: opts.confirm ?? new RegExp(`^${name}$`, 'i') });
    if ((await confirm.count()) > 0) {
      await confirm.last().click();
    } else {
      await dialog.getByRole('button').last().click();
    }
  }
  await settle(page, 1200);
  return page;
}

async function tab(page, name) {
  await page.getByRole('tab', { name: new RegExp(`^${name}`) }).first().click();
  await settle(page, 600);
}

/** Uploads one typed document in a documents panel (document type drop-down, file, upload button). */
async function uploadDocument(page, type, file, uploadButton) {
  const scope = page.locator('main');
  const select = scope.getByLabel(/^Document type/).first();
  const options = await select.locator('option').allTextContents();
  await select.selectOption({ label: options.find((o) => new RegExp(type, 'i').test(o)) });
  await scope.locator('input[type=file]').first().setInputFiles(file);
  await page.waitForTimeout(500);
  await button(page, uploadButton).click();
  await settle(page, 1200);
}

// ------------------------------------------------------------------ walkthrough A

const PEOPLE = [
  { last: 'Bautista', first: 'Carmela', middle: 'Reyes', tin: '417-238-906-000', id: 'P4172389A', mobile: '09175550417' },
  { last: 'Villareal', first: 'Rosalind', middle: 'Cruz', tin: '417-238-907-000', id: 'P4172390A', mobile: '09175550418' },
  { last: 'Soriano', first: 'Mylene', middle: 'Ramos', tin: '417-238-908-000', id: 'P4172391A', mobile: '09175550419' },
  { last: 'Navarro', first: 'Imelda', middle: 'Lopez', tin: '417-238-909-000', id: 'P4172392A', mobile: '09175550420' },
  { last: 'Pascual', first: 'Leticia', middle: 'Mendoza', tin: '417-238-910-000', id: 'P4172393A', mobile: '09175550421' },
  { last: 'Aguilar', first: 'Rowena', middle: 'Castillo', tin: '417-238-911-000', id: 'P4172394A', mobile: '09175550422' },
  { last: 'Salazar', first: 'Evangeline', middle: 'Torres', tin: '417-238-912-000', id: 'P4172395A', mobile: '09175550423' },
  { last: 'Ocampo', first: 'Florencia', middle: 'Dizon', tin: '417-238-913-000', id: 'P4172396A', mobile: '09175550424' },
];

const exists = (ctx, p) => ctx.sql(`select 1 from crm_client where tin = '${p.tin}'`).length > 0;

/** The client of walkthrough A: a new person for step 1, afterwards the last one created. */
function person(ctx, fresh = false) {
  if (!ctx.state.person) {
    ctx.state.person = fresh ? PEOPLE.find((p) => !exists(ctx, p)) : [...PEOPLE].reverse().find((p) => exists(ctx, p));
    if (!ctx.state.person) {
      throw new Error(fresh ? 'every walkthrough person already exists; start from a fresh seed database'
        : 'run walkthrough A from step 1');
    }
  }
  return ctx.state.person;
}

function clientId(ctx) {
  return ctx.one(`select id from crm_client where tin = '${person(ctx).tin}'`);
}

const steps = {
  // 1. New client saved as prospect.
  'wt-a-01': async (ctx) => {
    const p = person(ctx, true);
    const page = await go(ctx, 'ao', '/crm/clients/new');
    await ctx.runSteps(page, [
      ['Client type', 'Individual'], ['Last name', p.last], ['First name', p.first], ['Middle name', p.middle],
      ['Birth date', '14-Mar-1988'], ['Nationality', 'Filipino'], ['Civil status', 'Married'], ['Occupation', 'Architect'],
      ['TIN', p.tin], ['ID type', 'Passport'], ['ID number', p.id],
      ['E-mail', `${p.first}.${p.last}@seed-client.ph`.toLowerCase()], ['Mobile', p.mobile],
      ['Street address', '12 Mabini Street, Barangay San Antonio'], ['City / municipality', 'Pasig City'],
      ['Province', 'Metro Manila'], ['Postal code', '1605'], ['Market segment', 'CBG'],
      ['The client banks with BDO', true], ['BDO CIF number', '0041723890'], ['Source of funds', 'Salary'],
    ]);
    await button(page, 'Save as Prospect').click();
    await settle(page, 1500);
    return page;
  },
  // 2. KYC documents uploaded and the KYC submitted.
  'wt-a-02': async (ctx) => {
    const page = await go(ctx, 'ao', `/crm/clients/${clientId(ctx)}?tab=kyc`);
    const p = person(ctx);
    const who = `${p.first} ${p.middle} ${p.last}`;
    await uploadDocument(page, 'KYC form', pdf('kyc-form.pdf', 'Client information sheet / KYC form',
      [`Client: ${who}`, 'Seed data for the SIT environment']), 'Upload KYC Document');
    await uploadDocument(page, '^Valid ID', pdf('valid-id.pdf', 'Valid ID (passport)',
      [`Holder: ${who}`, `Passport no. ${p.id}`, 'Seed data for the SIT environment']), 'Upload KYC Document');
    await act(page, 'Submit KYC');
    return page;
  },
  // 3. KYC verified by the Marketing Team Leader.
  'wt-a-03': async (ctx) => {
    const page = await go(ctx, 'mkttl', `/crm/clients/${clientId(ctx)}`);
    await act(page, 'Verify KYC');
    return page;
  },
  // 4. Client confirmed by the Account Officer.
  'wt-a-04': async (ctx) => {
    const page = await go(ctx, 'ao', `/crm/clients/${clientId(ctx)}`);
    await act(page, 'Confirm client');
    return page;
  },
  // 5. Quotation of the package product, submitted for review.
  'wt-a-05': async (ctx) => {
    const page = await go(ctx, 'ao', `/crm/clients/${clientId(ctx)}`);
    await act(page, 'Generate Quotation');
      // The wizard opens on step 2 with the client chosen; the market segment is not taken from the client.
    await tab(page, '1. Client');
    await ctx.runSteps(page, [['Market segment', 'CBG'], ['Source channel', 'Walk-in']]);
    await button(page, 'Next').click();
    await settle(page, 600);
    await ctx.runSteps(page, [['Product', '^PAR01'], ['Insurer', 'Mabuhay'], ['Insurer branch (LGT)', 'Makati']]);
    await button(page, 'Next').click();
    await settle(page, 600);
    await button(page, /add location/i).click();
    await ctx.runSteps(page, [
      ['Address', '12 Mabini Street, Barangay San Antonio'], ['City', 'Pasig City'], ['Province', 'Metro Manila'],
      ['Occupancy', 'Dwelling'], ['Construction class', 'Class 1'], ['Description', 'Two-storey concrete residence'],
      addInsuredItem('Building', '3800000'), addInsuredItem('Contents', '700000'),
    ]);
    await page.waitForTimeout(1500);
    await button(page, 'Next').click();
    await settle(page, 600);
    await button(page, 'Next').click();
    await settle(page, 600);
    await act(page, 'Submit for Review');
    return page;
  },
  // 6. Quotation approved by the Marketing Team Leader.
  'wt-a-06': async (ctx) => {
    const page = await go(ctx, 'mkttl', `/quotations/${quotationId(ctx)}`);
    await act(page, 'Approve');
    return page;
  },
  // 7. Quotation sent to the client by e-mail.
  'wt-a-07': async (ctx) => {
    const page = await go(ctx, 'ao', `/quotations/${quotationId(ctx)}`);
    await act(page, 'Send via Email', { confirm: /^send$/i });
    await tab(page, 'E-mails');
    return page;
  },
  // 8. Acceptance recorded and the account created.
  'wt-a-08': async (ctx) => {
    const page = await go(ctx, 'ao', `/quotations/${quotationId(ctx)}`);
    const reply = pdf('client-acceptance.pdf', 'Client acceptance e-mail', [
      `Re: Insurance quotation ${ctx.one(`select quotation_no from quo_quotation where id = ${quotationId(ctx)}`)}`,
      'I accept the quotation. Please proceed with the cover.', 'Seed data for the SIT environment']);
    await act(page, /^record acceptance$/i, {
      fill: async (dialog) => {
        await dialog.locator('input[type=file]').first().setInputFiles(reply);
        await page.waitForTimeout(400);
      },
      confirm: /record acceptance/i,
    });
    await act(page, /^create accounts?$/i);
    return page;
  },
  // 9. Draft account completed and submitted to Processing.
  'wt-a-09': async (ctx) => {
    const page = await go(ctx, 'ao', `/accounts/${accountId(ctx)}/edit`);
    await tab(page, '3. Period');
    await ctx.runSteps(page, [['Mortgagee bank', 'BDO Home Loans'], ['Loan application no.', loanNo(ctx)],
      ['PN numbers', pnNo(ctx)]]);
    for (let i = 0; i < 3; i += 1) {
      await button(page, 'Next').click();
      await settle(page, 700);
    }
    await page.waitForTimeout(1000);
    await act(page, 'Submit to Processing');
    return page;
  },
  // 10. Account validated by Processing.
  'wt-a-10': async (ctx) => {
    const page = await go(ctx, 'proc', `/accounts/${accountId(ctx)}`);
    await act(page, 'Validate');
    return page;
  },
  // 11. CLPC billing, payment report and confirmed matches.
  'wt-a-11': async (ctx) => {
    const arn = accountArn(ctx);
    const page = await go(ctx, 'proc', '/placement/billing');
    const batchOf = () => ctx.sql(`select b.batch_no from plc_billing_batch b join plc_billing_item i on i.batch_id = b.id where i.arn = '${arn}' order by b.id desc limit 1`);
    if (batchOf().length === 0) {
      await page.locator('table tbody tr').filter({ hasText: arn }).first().locator('input[type=checkbox]').check();
      await act(page, /^Bill Selected/);
    }
    const batch = batchOf()[0][0];
    const report = csv(`clpc-payment-report-${arn}.csv`, [
      ['PN No.', 'Loan Application No.', 'Borrower', 'Status', 'Amount', 'Payment Date'],
      [pnNo(ctx), loanNo(ctx), `${person(ctx).last}, ${person(ctx).first}`, 'PAID',
        ctx.one(`select gross_premium from acc_account where arn = '${arn}'`), new Date().toISOString().slice(0, 10)],
    ]);
    await button(page.locator('table tbody tr').filter({ hasText: batch }).first(), 'Upload Report').click();
    await page.waitForTimeout(700);
    const dialog = page.locator('dialog.modal[open]').last();
    await dialog.locator('input[type=file]').first().setInputFiles(report);
    await page.waitForTimeout(500);
    await button(dialog, 'Upload and Match').click();
    await settle(page, 2000);
    await act(page, 'Confirm Matches');
    return page;
  },
  // 12. Placement slip generated and sent to the insurer.
  'wt-a-12': async (ctx) => {
    const arn = accountArn(ctx);
    const page = await go(ctx, 'proc', '/placement');
    const tick = async () => {
      const box = page.locator('table tbody tr').filter({ hasText: arn }).first().locator('input[type=checkbox]');
      await box.check();
      await page.waitForTimeout(400);
    };
    if (ctx.sql(`select 1 from plc_slip_account where arn = '${arn}'`).length === 0) {
      await tick();
      await act(page, 'For Placement', { reason: false, confirm: /^Generate Slips$/ });
    }
    await tick();
    await act(page, 'Send Slips', { reason: false, confirm: /^Send/ });
    return page;
  },
  // 13. The insurer's e-policy uploaded against the account.
  'wt-a-13': async (ctx) => {
    const arn = accountArn(ctx);
    const p = person(ctx);
    const acc = ctx.sql(`select to_char(period_from, 'YYYY-MM-DD'), to_char(period_to, 'YYYY-MM-DD'), gross_premium from acc_account where arn = '${arn}'`)[0];
    const premium = Number(acc[2]).toLocaleString('en-US', { minimumFractionDigits: 2 });
    const file = pdf(`${arn}-epolicy.pdf`, 'Mabuhay General Insurance Corp. - Fire Insurance Policy', [
      `Policy No.: ${policyNo(ctx)}`,
      `Account Reference No.: ${arn}`,
      `Insured: ${p.first} ${p.middle} ${p.last}`,
      'Location of risk: 12 Mabini Street, Barangay San Antonio, Pasig City, Metro Manila',
      `Period of Insurance: from ${acc[0]} to ${acc[1]}`,
      `Gross Premium: PHP ${premium}`,
      'Mortgagee: BDO Unibank, Inc. (BDO Home Loans)',
      'Seed data for the SIT environment',
    ]);
    const page = await go(ctx, 'proc', '/issuance/upload');
    await page.getByLabel(/^E-policy PDF\s*\*?$/).setInputFiles(file);
    await ctx.runSteps(page, [['Account (ARN)', arn]]);
    await act(page, 'Upload and Review');
    await settle(page, 1500);
    return page;
  },
  // 14. Extracted policy number checked and the policy confirmed.
  'wt-a-14': async (ctx) => {
    const id = ctx.one(`select id from iss_epolicy where arn = '${accountArn(ctx)}' order by id desc limit 1`);
    const page = await go(ctx, 'proc', `/issuance/epolicies/${id}`);
    await act(page, 'Confirm Policy');
    return page;
  },
  // 15. E-policy sent to the client by the E-policy Sender.
  'wt-a-15': async (ctx) => {
    const arn = accountArn(ctx);
    const page = await go(ctx, 'epol', '/issuance/dispatch');
    const row = page.locator('table tbody tr').filter({ hasText: arn }).first();
    if (await row.isVisible().catch(() => false)) {
      await button(row, 'Send').click();
      await page.waitForTimeout(800);
      const dialog = page.locator('dialog.modal[open]').last();
      await dialog.getByRole('button', { name: /^Send/ }).last().click();
      await settle(page, 1500);
    }
    await tab(page, 'Dispatch Report');
    return page;
  },
  // 16. Account booked from the Booking Workbench.
  'wt-a-16': async (ctx) => {
    const page = await go(ctx, 'proc', `/booking/book/${accountArn(ctx)}`);
    await act(page, 'Book Account', { reason: false, confirm: /^Book/ });
    await settle(page, 1500);
    return page;
  },
  // 17. The booked invoice: journal and open items handed to Accounting.
  'wt-a-17': async (ctx) => {
    const id = ctx.one(`select id from bkg_invoice where arn = '${accountArn(ctx)}' and status = 'BOOKED' order by id desc limit 1`);
    const page = await go(ctx, 'proc', `/booking/invoices/${id}`);
    await tab(page, 'Journal');
    return page;
  },
};

function policyNo(ctx) {
  return `MGIC-FI-2026-${person(ctx).mobile.slice(-5)}`;
}

function pnNo(ctx) {
  return `PN-${person(ctx).mobile.slice(-4)}-2026`;
}

function loanNo(ctx) {
  return `HL-2026-${person(ctx).mobile.slice(-4)}`;
}

function accountArn(ctx) {
  return ctx.one(`select arn from acc_account where id = ${accountId(ctx)}`);
}

/** Adds an insured item (name and sum insured) to the first location of a risk items step. */
function addInsuredItem(name, sum) {
  return async (page) => {
    await button(page, /add insured item/i).click();
    await page.waitForTimeout(400);
    const items = page.getByLabel(/^Item \d+$/);
    const n = (await items.count()) - 1;
    await items.nth(n).fill(name);
    const box = items.nth(n).locator('xpath=ancestor::*[.//input[@type="number"]][1]');
    await box.locator('input[type=number]').first().fill(sum);
    await page.waitForTimeout(300);
  };
}

function accountId(ctx) {
  return ctx.one(`select a.id from acc_account a join crm_client c on c.client_code = a.client_code where c.tin = '${person(ctx).tin}' order by a.id desc limit 1`);
}

function quotationId(ctx) {
  return ctx.one(`select q.id from quo_quotation q join crm_client c on c.client_code = q.client_code or c.prospect_code = q.client_code where c.tin = '${person(ctx).tin}' order by q.id desc limit 1`);
}

async function prepare() {}

module.exports = { steps, prepare, bulkClientFile, pdf, csv, act, tab, go, button, settle, uploadDocument, TMP };
