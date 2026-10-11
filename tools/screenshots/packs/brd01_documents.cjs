// Generated documents of BRD-01 New Business: each document is produced by the system on the seed profile,
// downloaded through the API and its first page rendered to PNG (see documents.yaml of the pack). Word and Excel
// files are converted to PDF with LibreOffice before rendering.
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'nb-pack-docs-'));
const SOFFICE = process.env.SOFFICE || 'soffice';

const HELPER = path.join(__dirname, '..', 'doc_render.py');
const PYTHON = process.env.PYTHON || 'python3';

/**
 * Renders the first page of a PDF, Word, Excel or OpenDocument file, or a text file, to a PNG at `out`.
 *
 * A workbook prints landscape, fitted to the page width, every column shown as wide as its content (doc_render.py).
 * `keep` names the columns to show (a wide file would print too small to read); `options.rows` keeps only the rows of
 * one value of a column (an extract) and `options.first` the first rows; `options.above` = 'drop' leaves out the rows
 * above the headings (the guide band of a template), with their images, while by default the report header (logo,
 * title, parameters) stays above the columns shown. A text file is drawn in a monospace font without wrapping; a file
 * of delimited records field by field.
 */
function render(buffer, ext, out, dpi = Number(process.env.DOC_DPI || 200), keep = [], options = {}) {
  const base = path.join(TMP, `doc-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`);
  let pdfFile = `${base}.${ext}`;
  fs.writeFileSync(pdfFile, buffer);
  if (ext === 'txt') {
    execFileSync(PYTHON, [HELPER, 'text', pdfFile, out, String(dpi)], { cwd: TMP });
    return;
  }
  if (ext === 'xlsx') {
    execFileSync(PYTHON, [HELPER, 'xlsx', pdfFile, JSON.stringify({ keep, ...options })], { cwd: TMP, stdio: 'ignore' });
  }
  if (ext !== 'pdf') {
    execFileSync(SOFFICE, ['--headless', '--convert-to', 'pdf', '--outdir', TMP, pdfFile], { stdio: 'ignore',
      timeout: 120000 });
    pdfFile = `${base}.pdf`;
  }
  execFileSync('pdftoppm', ['-png', '-r', String(dpi), '-f', '1', '-l', '1', '-singlefile', pdfFile, base]);
  if (ext === 'xlsx') {
    execFileSync(PYTHON, [HELPER, 'crop', `${base}.png`], { cwd: TMP });
  }
  fs.copyFileSync(`${base}.png`, out);
}

function extOf(buffer) {
  const head = buffer.subarray(0, 4).toString('latin1');
  if (head.startsWith('%PDF')) {
    return 'pdf';
  }
  return 'xlsx';
}

/** Downloads a file from the API as `user` and renders its first page (`keep` and `options` as for render). */
async function download(ctx, user, url, out, ext, keep = [], options = {}) {
  const data = await ctx.api(user, 'GET', url);
  if (!Buffer.isBuffer(data)) {
    throw new Error(`${url} did not return a file`);
  }
  render(data, ext || extOf(data), out, undefined, keep, options);
}

const one = (ctx, q) => ctx.one(q);

/**
 * The hold cover request is e-mailed, not kept on the account: its PDF is the attachment of the outbound message,
 * read from the local document store of the seed profile (STORAGE_ROOT = BROKERVERSE_STORAGE_LOCAL_ROOT).
 */
async function holdCover(ctx, out) {
  const sql = "select f.bucket || '/objects/' || f.object_key from msg_outbound_attachment a join stored_file f on f.id = a.stored_file_id where a.file_name like 'HOLD_COVER_%' order by a.id desc limit 1";
  if (ctx.sql(sql).length === 0) {
    const arn = ctx.one("select a.arn from acc_account a where a.status in ('READY_FOR_PLACEMENT', 'PLACED') and a.hold_cover_status is null order by a.id limit 1");
    await ctx.api('proc', 'POST', `/placement/accounts/${arn}/hold-cover`, { to: [] });
  }
  if (!process.env.STORAGE_ROOT) {
    throw new Error('set STORAGE_ROOT to the local document store of the seed profile');
  }
  render(fs.readFileSync(path.join(process.env.STORAGE_ROOT, ctx.one(sql))), 'pdf', out);
}

const shots = {
  'doc-quotation': (ctx, out) => download(ctx, 'ao',
    `/quotations/${one(ctx, "select id from quo_quotation where status in ('SENT_TO_CLIENT', 'ACCEPTED', 'CONVERTED') order by id desc limit 1")}/document.pdf`, out),
  'doc-quotation-slip': (ctx, out) => download(ctx, 'tsu',
    `/proposals/${one(ctx, 'select id from npk_proposal where qs_no is not null order by id desc limit 1')}/quotation-slip.pdf`, out),
  'doc-comparative': (ctx, out) => download(ctx, 'tsu',
    `/proposals/${one(ctx, 'select id from npk_proposal where chosen_insurer is not null order by id desc limit 1')}/comparative.pdf`, out),
  'doc-proposal-slip': (ctx, out) => download(ctx, 'ao',
    `/proposals/${one(ctx, 'select id from npk_proposal where ps_no is not null order by id desc limit 1')}/proposal-slip.pdf`, out),
  'doc-placement-slip': (ctx, out) => download(ctx, 'proc',
    `/placement/slips/${one(ctx, "select id from plc_slip where status = 'SENT' order by id desc limit 1")}/files/pdf`, out),
  'doc-hold-cover': holdCover,
  'doc-insurance-advice': (ctx, out) => download(ctx, 'proc',
    `/issuance/insurance-advice/${one(ctx, 'select id from iss_insurance_advice order by id desc limit 1')}/file`, out),
  'doc-service-invoice': (ctx, out) => download(ctx, 'proc',
    `/booking/service-invoices/${one(ctx, 'select id from bkg_service_invoice order by id desc limit 1')}/pdf`, out),
  'doc-clpc-billing': (ctx, out) => download(ctx, 'proc',
    `/placement/billing/batches/${one(ctx, 'select id from plc_billing_batch order by id desc limit 1')}/file?format=XLSX`, out, 'xlsx',
    ['PN No.', 'Loan Application No.', 'Booking Date', 'Borrower', 'Premium', 'Reference']),
  'doc-report': async (ctx, out) => {
    const company = ctx.one("select id from org_company where code = 'FVI'");
    const data = await ctx.api('mkttl', 'POST', '/reports/NB-ACC-STATUS/export?format=PDF', { companyId: company });
    render(data, 'pdf', out);
  },
};

module.exports = { shots, render, download };
