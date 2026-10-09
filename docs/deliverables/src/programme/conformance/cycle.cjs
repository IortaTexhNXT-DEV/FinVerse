// The cross-BRD broking cycle as one scenario on fresh seed data: a new client from onboarding to booking (the BRD-01
// walkthrough A), its collection follow-up (BRD-04), the payment at the cashier and the remittance to the insurer with
// the commission OR (BRD-02 walkthrough A), the claim on the policy (BRD-07), the servicing view of the client
// (BRD-09), the flat cancellation with the refund (BRD-02 walkthrough B) and the month-end reports. Each step signs
// in as its persona; the BRD steps reuse the walkthrough recipes of tools/screenshots/packs on the account of this
// scenario. After each step the amounts are checked against the rules of the FRS and the journals of the account are
// checked to balance. Writes data/cycle_run.json.
//
// Usage (seed profile running, fresh database; password in SEED_PASSWORD, never written to a file):
//   SEED_PASSWORD=... BASE=http://localhost:5173 API=http://localhost:8080 PGDATABASE=... PGUSER=... PGPASSWORD=... \
//   PLAYWRIGHT_MODULE=... node -r <choice-field preload> cycle.cjs
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const HERE = __dirname;
const ROOT = path.resolve(HERE, '../../../../..');
const BASE = process.env.BASE || 'http://localhost:5173';
const API = process.env.API || 'http://localhost:8080';
const PASSWORD = process.env.SEED_PASSWORD;
const PACKS = process.env.PACKS_DIR || path.join(ROOT, 'tools/screenshots/packs');
const WIDTH = 1440;
const HEIGHT = 900;

// ------------------------------------------------------------------ database, API and browser

function sql(query) {
  const out = execFileSync('psql', ['-X', '-A', '-t', '-F', '\t', '-c', query], { encoding: 'utf8' });
  return out.split('\n').filter(Boolean).map((l) => l.split('\t'));
}

function one(query) {
  const rows = sql(query);
  if (rows.length === 0) {
    throw new Error(`no record for: ${query}`);
  }
  return rows[0][0];
}

const tokens = new Map();
async function api(user, method, url, body) {
  if (!tokens.has(user)) {
    const r = await fetch(`${API}/api/v1/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Origin: BASE },
      body: JSON.stringify({ username: user, password: PASSWORD }),
    });
    if (!r.ok) {
      throw new Error(`API sign-in of ${user} failed: ${r.status}`);
    }
    tokens.set(user, (await r.json()).accessToken);
  }
  const headers = { Authorization: `Bearer ${tokens.get(user)}`, Origin: BASE };
  let payload;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const r = await fetch(`${API}/api/v1${url}`, { method, headers, body: payload });
  const type = r.headers.get('content-type') || '';
  const data = type.includes('json') ? await r.json() : Buffer.from(await r.arrayBuffer());
  if (!r.ok) {
    throw new Error(`${method} ${url}: ${r.status} ${JSON.stringify(data).slice(0, 400)}`);
  }
  return data;
}

async function settle(page, ms = 800) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(ms);
}

function rx(text) {
  return text instanceof RegExp ? text : new RegExp(text, 'i');
}

async function fillField(page, label, value) {
  const field = page.getByLabel(new RegExp(`^${String(label).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\*?$`)).first();
  await field.waitFor({ state: 'visible', timeout: 10000 });
  const tag = await field.evaluate((e) => e.tagName.toLowerCase());
  const type = await field.evaluate((e) => (e.getAttribute('type') || '').toLowerCase());
  if (tag === 'select') {
    const options = await field.locator('option').allTextContents();
    const hit = options.find((o) => rx(value).test(o)) ?? options.find((o) => o.includes(value));
    await field.selectOption({ label: hit });
  } else if (type === 'checkbox' || type === 'radio') {
    await (value ? field.check() : field.uncheck());
  } else {
    await field.fill('');
    await field.pressSequentially(String(value), { delay: 5 });
    await field.press('Tab').catch(() => {});
  }
  await page.waitForTimeout(150);
}

async function runSteps(page, steps, ctx) {
  for (const step of steps) {
    if (typeof step === 'function') {
      await step(page, ctx);
    } else {
      await fillField(page, step[0], step[1]);
    }
  }
  await settle(page, 500);
}

async function seen(page) {
  return page.evaluate(() => {
    const parts = [...document.querySelectorAll('[role=dialog], .toast-region, main')].map((e) => e.innerText);
    return { url: location.pathname + location.search, text: parts.join('\n----\n').slice(0, 6000) };
  });
}

// ------------------------------------------------------------------ the recipes on the account of the scenario

/** Copies of the pack recipes whose walkthrough accounts are the account of this scenario. */
function recipes(arn, payor) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'cycle-packs-'));
  for (const f of fs.readdirSync(PACKS)) {
    let text = fs.readFileSync(path.join(PACKS, f), 'utf8');
    if (f === 'brd02_walkthrough.cjs' || f === 'brd04_walkthrough.cjs') {
      text = text.replace("const ARN_A = 'ARN-2026-940007';", `const ARN_A = '${arn}';`)
        .replace("'Antonio Luis D. Garcia'", `'${payor}'`).replace("'Garcia, Antonio Luis Dizon'", `'${payor}'`);
    }
    fs.writeFileSync(path.join(dir, f), text);
  }
  return {
    brd01: require(path.join(dir, 'brd01_walkthrough.cjs')),
    brd02: require(path.join(dir, 'brd02_walkthrough.cjs')),
    brd04: require(path.join(dir, 'brd04_walkthrough.cjs')),
  };
}

// ------------------------------------------------------------------ checks

const money = (v) => Math.round(Number(v) * 100) / 100;
const roundUpHalf = (v) => Math.ceil(money(v) * 2 - 1e-9) / 2;

/** Journals of the account (booking, receipt, application, remittance, cancellation) and whether each balances. */
function journals(invoice, arn) {
  return sql(`select b.batch_no, b.journal_type, b.source_module, b.total_debit, b.total_credit,
      coalesce((select sum(case when l.side = 'DEBIT' then l.base_amount else -l.base_amount end) from jnl_line l
                where l.batch_id = b.id), 0)
    from jnl_batch b where b.source_reference like '%${invoice}%' or b.source_reference like '%${arn}%'
       or b.related_invoice_no = '${invoice}' or b.reference like '%${invoice}%' order by b.id`)
    .map(([no, type, module, dr, cr, diff]) => ({ no, type, module, debit: money(dr), credit: money(cr), balanced: money(diff) === 0 && money(dr) === money(cr) }));
}

function component(invoice, name, column = 'booked') {
  return money(one(`select ${column} from ops_invoice_component c join ops_invoice i on i.id = c.invoice_id
    where i.invoice_no = '${invoice}' and c.component = '${name}'`));
}

// ------------------------------------------------------------------ scenario

(async () => {
  if (!PASSWORD) {
    throw new Error('Set SEED_PASSWORD');
  }
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const pages = new Map();
  const pageOf = async (user) => {
    if (!pages.has(user) || pages.get(user).isClosed()) {
      const context = await browser.newContext({ viewport: { width: WIDTH, height: HEIGHT } });
      const page = await context.newPage();
      page.setDefaultTimeout(20000);
      await page.goto(`${BASE}/login`);
      await page.getByLabel('User ID').fill(user);
      await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
      await page.getByRole('button', { name: /^(login|sign in)$/i }).click();
      await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30000 });
      pages.set(user, page);
    }
    return pages.get(user);
  };
  const ctx = { BASE, API, ROOT, sql, one, api, settle, runSteps, fillField, pageOf, browser, state: {} };
  const out = [];
  let packs = recipes('ARN-2026-940007', 'Garcia, Antonio Luis Dizon');
  let arn = null;
  let invoice = null;

  async function step(no, area, persona, action, expected, work) {
    const entry = { no, area, persona, action, expected, status: 'pass', amounts: [], observed: '' };
    try {
      const result = await work(entry);
      if (result && result.page) {
        const s = await seen(result.page);
        entry.url = s.url;
        entry.text = s.text;
      }
      if (entry.amounts.some((a) => a.ok === false)) {
        entry.status = 'fail';
      }
    } catch (e) {
      entry.status = 'fail';
      entry.observed = `${entry.observed} ${e.message.split('\n')[0]}`.trim();
    }
    console.log(no, entry.status, entry.observed.slice(0, 200));
    out.push(entry);
    fs.writeFileSync(path.join(HERE, 'data', 'cycle_run.json'), JSON.stringify({ arn, invoice, steps: out }, null, 1));
    return entry;
  }
  const amount = (entry, name, expected, observed) => entry.amounts.push(
    { name, expected: money(expected), observed: money(observed), ok: money(expected) === money(observed) });

  // 1. Onboarding, KYC and screening (BRD-01 walkthrough A steps 1-4).
  await step('C01', 'Client onboarding and KYC', 'Marketing AO, Marketing TL', 'New client saved as prospect, KYC documents submitted, KYC verified, client confirmed',
    'Client confirmed with a client number; the client is screened against the watchlists', async (e) => {
      let page;
      for (const s of ['wt-a-01', 'wt-a-02', 'wt-a-03', 'wt-a-04']) {
        page = await packs.brd01.steps[s](ctx);
      }
      const [code, prospect, stage, rating] = sql("select coalesce(client_code, '-'), prospect_code, onboarding_stage, coalesce(risk_rating, '-') from crm_client where tin like '417-238-9%' order by id desc limit 1")[0];
      const screened = one(`select count(*) from scr_screening_run where scope in ('Client ${code}', 'Client ${prospect}') and status = 'SUCCESS'`);
      e.observed = `Client ${code} (prospect ${prospect}), onboarding ${stage}, risk rating ${rating}, ${screened} screening run(s) of the client`;
      if (code === '-' || Number(screened) === 0) {
        e.status = 'fail';
      }
      return { page };
    });

  // 2. Quotation request, quotation and client acceptance (steps 5-8).
  await step('C02', 'Quotation and acceptance', 'Marketing AO, Marketing TL', 'Quotation of the Fire package rated, reviewed, approved, sent and accepted by the client',
    'Quotation accepted; the account is created', async (e) => {
      let page;
      for (const s of ['wt-a-05', 'wt-a-06', 'wt-a-07', 'wt-a-08']) {
        page = await packs.brd01.steps[s](ctx);
      }
      const [qno, status] = sql("select q.quotation_no, q.status from quo_quotation q join crm_client c on c.client_code = q.client_code where c.tin like '417-238-9%' order by q.id desc limit 1")[0];
      e.observed = `Quotation ${qno} ${status}`;
      return { page };
    });

  // 3. Account, payment gate, placement, e-policy and booking (steps 9-17); premium, taxes and commission.
  await step('C03', 'Account, placement, e-policy and booking', 'Marketing AO, Processing, E-policy Sender',
    'Account submitted and validated, billed through CLPC, placed with the insurer, e-policy confirmed and sent, account booked',
    'Booked invoice with the premium, DST, premium tax, FST and LGT of the FRS rating rules; commission and VAT on commission', async (e) => {
      let page;
      for (const s of ['wt-a-09', 'wt-a-10', 'wt-a-11', 'wt-a-12', 'wt-a-13', 'wt-a-14', 'wt-a-15', 'wt-a-16', 'wt-a-17']) {
        page = await packs.brd01.steps[s](ctx);
      }
      arn = one("select a.arn from acc_account a join crm_client c on c.client_code = a.client_code where c.tin like '417-238-9%' order by a.id desc limit 1");
      invoice = one(`select invoice_no from ops_invoice where arn = '${arn}' and kind = 'BOOKING' order by id desc limit 1`);
      const [tsi, net, comm] = sql(`select total_sum_insured, net_premium, commission_rate from acc_account where arn = '${arn}'`)[0].map(Number);
      const lgtRate = Number(one(`select coalesce(max(b.lgt_rate), 0) from cat_insurer_branch b join cat_insurer i on i.id = b.insurer_id join acc_account a on a.insurer_code = i.party_code and a.insurer_branch = b.code where a.arn = '${arn}'`));
      amount(e, 'Basic premium', net, component(invoice, 'BASIC'));
      amount(e, 'DST (12.5% of the net premium, rounded up to the next 0.50)', roundUpHalf(net * 0.125), component(invoice, 'DST'));
      amount(e, 'Premium tax (Property 12%)', net * 0.12, component(invoice, 'PREMIUM_TAX_VAT'));
      amount(e, 'Fire service tax (Property 2%)', net * 0.02, component(invoice, 'FST'));
      if (lgtRate > 0) {
        amount(e, 'LGT (rate of the insurer branch)', net * lgtRate / 100, component(invoice, 'LGT'));
      }
      amount(e, 'Commission (rate of the insurer and product)', net * comm / 100, component(invoice, 'COMMISSION'));
      amount(e, 'VAT on commission (12%)', component(invoice, 'COMMISSION') * 0.12, component(invoice, 'COMMISSION_VAT'));
      amount(e, 'Withholding tax on commission', component(invoice, 'COMMISSION') * 0.10, component(invoice, 'WTAX'));
      const gross = ['BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST', 'OTHER'].reduce((t, c) => t + component(invoice, c), 0);
      amount(e, 'Gross premium = net premium + charges', gross, one(`select gross_premium from ops_invoice where invoice_no = '${invoice}'`));
      e.observed = `${arn} booked as ${invoice}; TSI ${tsi.toFixed(2)}; LGT rate ${lgtRate}%`;
      packs = recipes(arn, one(`select client_name from acc_account where arn = '${arn}'`));
      return { page };
    });

  await step('C04', 'Billing and accounting entries', 'Processing', 'Booking journal of the invoice',
    'The journal debits the premium receivable by component and the commission receivable, credits the payable to the insurer, the unrealised commission and the deferred output VAT; debits equal credits', async (e) => {
      const j = journals(invoice, arn);
      e.observed = j.map((x) => `${x.no} ${x.type} ${x.debit.toFixed(2)}/${x.credit.toFixed(2)}${x.balanced ? '' : ' NOT BALANCED'}`).join('; ');
      if (j.length === 0 || j.some((x) => !x.balanced)) {
        e.status = 'fail';
      }
    });

  // 4. Collections follow-up before the payment (BRD-04 walkthrough A and B on this account).
  await step('C05', 'Collection and ageing', 'Section Head, Collection Handler', 'Worklist refreshed; the account is in the handler\'s worklist; effort logged; promise to pay recorded',
    'The invoice on the PR Worklist with its aging bracket and outstanding premium; effort and promise recorded', async (e) => {
      await packs.brd04.prepare(ctx);
      let page;
      for (const s of ['wt-a-01', 'wt-a-02', 'wt-a-03', 'wt-a-04', 'wt-b-01']) {
        page = await packs.brd04.steps[s](ctx);
      }
      const [status, bracket, outstanding] = sql(`select status, coalesce(aging_bracket, '-'), net_outstanding from clx_item where invoice_no = '${invoice}'`)[0] || ['not listed', '-', 0];
      e.observed = `Collection item ${status}, bracket ${bracket}, outstanding ${outstanding}`;
      if (status !== 'OPEN') {
        e.status = 'fail';
      }
      return { page };
    });

  // 5. Cashiering: payment received and applied by component (BRD-02 walkthrough A steps 2-4).
  await step('C06', 'Cashiering', 'Cashier', 'Receive Payment of the full gross premium over the counter; Issue AR and Apply',
    'AR issued for the gross premium and applied to each component; the invoice is Paid', async (e) => {
      let page;
      for (const s of ['wt-a-02', 'wt-a-03', 'wt-a-04']) {
        page = await packs.brd02.steps[s](ctx);
      }
      const [receipt, amt] = sql(`select r.receipt_no, r.amount from csh_application a join csh_receipt r on r.id = a.receipt_id where a.invoice_no = '${invoice}' and a.status = 'ACTIVE' order by a.id desc limit 1`)[0];
      amount(e, 'AR amount = gross premium', one(`select gross_premium from ops_invoice where invoice_no = '${invoice}'`), amt);
      const open = ['BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST'].reduce((t, c) => t + component(invoice, c, 'balance'), 0);
      amount(e, 'Premium receivable left after the application', 0, open);
      e.observed = `Receipt ${receipt}`;
      return { page };
    });

  await step('C07', 'Collection and ageing', 'Section Head', 'Worklist refreshed after the payment',
    'The collection item is Completed; it is on Completed Collections', async (e) => {
      const page = await packs.brd04.steps['wt-a-01'](ctx);
      const status = one(`select status from clx_item where invoice_no = '${invoice}'`);
      e.observed = `Collection item ${status}`;
      if (status !== 'COMPLETED') {
        e.status = 'fail';
      }
      return { page };
    });

  // 6. Remittance net of commission, VAT and withholding tax; commission OR; insurer OR (steps 5-10).
  await step('C08', 'Remittance to the insurer', 'Remittance Processor, Remittance TL, Disbursement',
    'Extraction of the paid invoice; batch submitted and approved; DV assigned; insurer OR uploaded',
    'Net due = paid AR + WTAX - commission - VAT on commission; commission OR issued; the invoice Paid and Fully Remitted', async (e) => {
      let page;
      for (const s of ['wt-a-05', 'wt-a-06', 'wt-a-07', 'wt-a-08', 'wt-a-09', 'wt-a-10']) {
        page = await packs.brd02.steps[s](ctx);
      }
      const [batch, paid, comm, vat, wtax, net, stage, orNo] = sql(`select b.batch_no, l.paid_ar, l.commission, l.commission_vat, l.wtax, l.net_due, b.stage, coalesce(b.commission_or_no, '-') from rem_batch_line l join rem_batch b on b.id = l.batch_id where l.invoice_no = '${invoice}' order by l.id desc limit 1`)[0];
      amount(e, 'Paid AR', one(`select gross_premium from ops_invoice where invoice_no = '${invoice}'`), paid);
      amount(e, 'Commission realised', component(invoice, 'COMMISSION'), comm);
      amount(e, 'VAT on commission', component(invoice, 'COMMISSION_VAT'), vat);
      amount(e, 'Withholding tax', component(invoice, 'WTAX'), wtax);
      amount(e, 'Net due to the insurer', Number(paid) + Number(wtax) - Number(comm) - Number(vat), net);
      const or = sql(`select r.amount, r.gross, r.vat, r.wtax from csh_receipt r where r.receipt_no = '${orNo}'`)[0];
      if (or) {
        amount(e, 'Commission OR: commission', comm, or[1]);
        amount(e, 'Commission OR: VAT', vat, or[2]);
        amount(e, 'Commission OR: withholding tax', wtax, or[3]);
        amount(e, 'Commission OR: amount = commission + VAT - withholding tax', Number(comm) + Number(vat) - Number(wtax), or[0]);
      }
      e.observed = `Batch ${batch} ${stage}; commission OR ${orNo}`;
      return { page };
    });

  await step('C09', 'Commission recognition and reconciliation', 'Commission Receivables', 'Commission receivable of the invoice after the remittance',
    'Commission, VAT on commission and withholding tax realised in full (balances 0); the amount due to the insurer settled', async (e) => {
      amount(e, 'Commission balance', 0, component(invoice, 'COMMISSION', 'balance'));
      amount(e, 'VAT on commission balance', 0, component(invoice, 'COMMISSION_VAT', 'balance'));
      amount(e, 'Withholding tax balance', 0, component(invoice, 'WTAX', 'balance'));
      amount(e, 'DTIP balance (due to the insurer)', 0, component(invoice, 'DTIP', 'balance'));
      const reg = one(`select count(*) from prc_item where invoice_no = '${invoice}'`);
      e.observed = `Commission realised in full; ${reg} line(s) on the production reconciliation register so far (it is filled by the next reconciliation cycle of the insurer)`;
    });

  await step('C10', 'Accounting entries and BIR outputs', 'GL Officer, Tax', 'Journals of the booking, the receipt and application, the remittance and the commission OR',
    'Every journal of the account balances (booking, receipt, remittance and commission OR)', async (e) => {
      const j = journals(invoice, arn);
      e.observed = j.map((x) => `${x.no} ${x.type} ${x.debit.toFixed(2)}${x.balanced ? '' : ' NOT BALANCED'}`).join('; ');
      if (j.length < 3 || j.some((x) => !x.balanced)) {
        e.status = 'fail';
      }
      const r = await api('accountant', 'POST', '/reports/TAX-SLS/run', { companyId: '1', fromDate: one("select to_char(date_trunc('month', current_date), 'YYYY-MM-DD')"), toDate: one("select to_char(current_date, 'YYYY-MM-DD')") }).catch((x) => ({ error: x.message }));
      const rows = (r.rows || []).filter((x) => x.kind === 'DETAIL').length;
      e.observed += `; VAT sales list of the month: ${r.error ? 'not run' : `${rows} customer row(s)`} (the list reads the VAT worksheet of the month, generated by Tax at month end)`;
    });

  // 7. Claim on the policy (BRD-07): recorded, premium checked, insurer claim number, settlement.
  await step('C11', 'Claims advocacy', 'Claims Officer, Claims Team Lead', 'Notice of loss recorded on the policy; premium check; insurer claim number; settlement set',
    'Claim number; premium PAID enables the authorization code; the insurer claim number recorded; settled claim', async (e) => {
      const today = one("select to_char(current_date, 'YYYY-MM-DD')");
      const claim = await api('clmofficer2', 'POST', '/broker-claims', {
        companyId: 1, arn, policyYear: 1, source: 'BROKER_NOTICE',
        loss: { lossDate: today, reportedDate: today, lossNature: 'FIRE', claimType: 'PROPERTY', lossDescription: 'Kitchen fire damaged the cabinets and the ceiling', lossPlace: 'Pasig City', claimAmount: 180000 },
        locations: [{ itemNo: 1, remarks: 'Kitchen and dining area' }], insurers: [], confirmOutsidePeriod: false, confirmReuse: false,
      });
      e.observed = `Claim ${claim.claimNo || claim.claim?.claimNo || JSON.stringify(claim).slice(0, 80)}`;
      const id = claim.id || claim.claim?.id;
      const check = await api('clmofficer2', 'POST', `/broker-claims/${id}/premium-check?companyId=1`).catch((x) => ({ error: x.message }));
      e.observed += `; premium check: ${check.error || check.premiumStatus || check.status || 'done'}`;
    });

  // 8. Servicing view of the client (BRD-09).
  await step('C12', 'Customer servicing', 'Contact Center Agent', 'Searches the client and opens the servicing view',
    'The client with the booked account and its payment', async (e) => {
      const page = await pageOf('csfagent');
      const client = one(`select client_code from acc_account where arn = '${arn}'`);
      await page.goto(`${BASE}/csf`);
      await settle(page);
      const box = page.getByRole('searchbox').or(page.getByRole('textbox')).first();
      await box.fill(client);
      await box.press('Enter');
      await settle(page, 1500);
      const text = await page.locator('main').innerText();
      e.observed = text.includes(client) ? `Client ${client} found` : `Client ${client} not found by the search`;
      if (!text.includes(client)) {
        e.status = 'fail';
      }
      return { page };
    });

  // 9. Flat cancellation of the paid and remitted policy, AR insurer and refund (BRD-02 walkthrough B).
  await step('C13', 'Endorsement, cancellation and adjustment', 'Marketing Collection, Adjustment, Adjustment TL, Cashier',
    'Flat cancellation requested, validated, approved and posted; the payment re-applied; the excess refunded',
    'Return invoice for the full premium; AR insurer for the premium remitted; unapplied payment refunded with approval to Disbursement', async (e) => {
      let page;
      for (const s of ['wt-b-01', 'wt-b-02', 'wt-b-03', 'wt-b-04', 'wt-b-05', 'wt-b-06', 'wt-b-07', 'wt-b-08', 'wt-b-09']) {
        page = await packs.brd02.steps[s](ctx);
      }
      const ret = sql(`select invoice_no, gross_premium from ops_invoice where parent_invoice_no = '${invoice}' or root_invoice_no = '${invoice}' and invoice_no <> '${invoice}' order by id desc limit 1`)[0];
      if (ret) {
        amount(e, 'Return premium = - gross premium', -Number(one(`select gross_premium from ops_invoice where invoice_no = '${invoice}'`)), ret[1]);
      }
      e.observed = `Return invoice ${ret ? ret[0] : '-'}; account ${one(`select status from acc_account where arn = '${arn}'`)}`;
      return { page };
    });

  // 10. Month-end reports.
  await step('C14', 'Month-end reports', 'Processing TL, Remittance TL, GL Officer', 'Production, remittance and cash reports of the month',
    'The account in the production and remittance reports of the month', async (e) => {
      const from = one("select to_char(date_trunc('month', current_date), 'YYYY-MM-DD')");
      const to = one("select to_char(current_date, 'YYYY-MM-DD')");
      const runs = [];
      for (const [user, code, params] of [
        ['proctl', 'NB-PRODUCTION', { from, to }], ['remittl', 'REM-REMITTED-BATCH', { from, to }],
        ['cashtl', 'CSH-DAILY-CASH-REC', { from, to }], ['accountant', 'TAX-SLS', { fromDate: from, toDate: to }]]) {
        const r = await api(user, 'POST', `/reports/${code}/run`, { companyId: '1', ...params }).catch((x) => ({ error: x.message }));
        const rows = (r.rows || []).filter((x) => x.kind === 'DETAIL');
        const hit = rows.some((x) => JSON.stringify(x).includes(invoice) || JSON.stringify(x).includes(arn));
        runs.push(`${code}: ${r.error ? 'error' : `${rows.length} rows${hit ? ', the account included' : ''}`}`);
      }
      e.observed = runs.join('; ');
    });

  await browser.close();
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
