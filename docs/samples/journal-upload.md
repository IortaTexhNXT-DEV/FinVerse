# Journal bulk upload – file format

Screen: **General Ledger → Journal Upload** (`/gl/upload`), API `POST /api/v1/journals/upload`
(`multipart/form-data`, parameters `companyId`, `mode=VALIDATE|IMPORT`, part `file`).
Permission: `JOURNAL_CREATE`. Templates: `GET /api/v1/journals/upload/template?format=csv|xlsx`
(or the *CSV template* / *Excel template* buttons). A ready-made example is in
[`journal-upload-sample.csv`](journal-upload-sample.csv).

## File

- `.csv` (UTF-8, comma separated, RFC 4180 quoting; a BOM is accepted) or `.xlsx` (first sheet).
- Maximum 5 MB and 5,000 data rows.
- Row 1 is the header. Column names are case-insensitive; spaces and dashes are read as `_`.
  Column order does not matter; unknown columns are ignored.
- One row per journal **line**. Rows with the same `voucher_key` form one voucher (journal).

## Columns

| Column | Required | Level | Description |
|---|---|---|---|
| `voucher_key` | yes | voucher | Any text grouping the lines of one voucher (e.g. `V1`). Only used inside the file. |
| `branch_code` | yes* | voucher | Branch code of the voucher (e.g. `HO`). |
| `journal_type` | no | voucher | `MANUAL` (default), `ADJUSTMENT` or `ACCRUAL`. |
| `value_date` | yes* | voucher | Accounting date, ISO `YYYY-MM-DD` (Excel date cells are accepted). |
| `currency` | yes* | voucher | Header currency, ISO code (e.g. `PHP`). |
| `narration` | yes* | voucher | Voucher narration (max 500). |
| `reference` | no | voucher | External reference (max 60). |
| `account_code` | yes | line | GL account code (must exist and be postable). |
| `debit` | one of | line | Debit amount, max 2 decimals; thousands separators allowed. |
| `credit` | one of | line | Credit amount. Exactly one of `debit` / `credit` must be > 0. |
| `line_currency` | no | line | Line currency when it differs from the header currency. |
| `exchange_rate` | no | line | Explicit rate; default is the SPOT rate of the value date. |
| `cost_center` | no | line | Cost centre code (mandatory for accounts that require it). |
| `business_line` | no | line | Line of business code. |
| `party_code` | no | line | Sub-ledger party code. |
| `line_reference` | no | line | Line reference (max 60). |
| `line_narration` | no | line | Line narration (max 250). |

\* Voucher-level columns are read from the first row of the voucher that fills them. Later rows of
the same voucher may leave them blank; if they repeat a value it must be identical.

## Processing

1. **VALIDATE** (dry run): every row and voucher is checked – column formats, header consistency,
   branch, balance (debits = credits per currency), at least two lines, bean validation of the
   journal request and account resolution. Each voucher is tried inside a transaction that is
   rolled back, so **nothing is created** (and no document number is consumed).
2. **IMPORT**: the same checks; each valid voucher is created as a **DRAFT** journal in its own
   transaction (atomic per voucher). Invalid vouchers are reported and skipped. Drafts are then
   submitted and approved through the normal maker-checker flow. Each import gets an upload
   reference (`UPL-yyyy-nnnnnn`, returned as `uploadReference`); every journal it creates records
   source `JOURNAL_UPLOAD` with that reference, shown as "Journal upload" on the journal detail.

The response lists every voucher (`VALID`, `CREATED` with its batch number, or `ERROR` with
messages) and every row with row-level errors. An IMPORT is recorded in the audit trail
(`JournalUpload`, action `RUN`).
