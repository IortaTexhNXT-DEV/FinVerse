# Tax & Statutory Reporting

Package `com.iortatechnxt.brokerverse.tax`; UI section **Tax & Statutory** (`frontend/src/features/tax`).
The module computes Philippine BIR, LGU and BFP returns and the Insurance Commission (IC) statutory
schedules from **posted** data. It depends on underwriting, payables, party, coa, accounting,
journal and the platform modules; no module depends on it (ArchUnit enforces this).

Migrations: `V700__tax_and_statutory.sql` (schema, `TAX_REMITTANCE` event, exception codes,
parameters), `V701__tax_permissions.sql` (grants); seed `V975__seed_tax_remittance_rule.sql` and the
`TaxSeedData` runner (`seed` profile, order 95).

## 1. Forms covered

| Form | Authority | Frequency / due rule (seed) | Worksheet | Payable cleared (seed) |
|---|---|---|---|---|
| 2550Q Quarterly VAT return | BIR | quarterly, 25th of the month after the quarter | VAT | Dr 2504 output VAT, Cr 1603 input VAT |
| 0619-E Monthly remittance of EWT | BIR | months 1 and 2 of each quarter, 10th of the next month | EWT | 2508 |
| 1601-EQ Quarterly EWT return (+ QAP) | BIR | quarterly, last day of the month after the quarter | EWT | 2508 (net of 0619-E) |
| 2000 DST declaration | BIR | monthly, 5th of the next month | DST | 2503 |
| 2551Q Quarterly percentage (premium) tax | BIR | quarterly, 25th of the month after | PREMIUM_TAX | 2507 |
| LBT Local business tax on premiums | LGU | quarterly, 20th of the month after | LGT | 2505 |
| FST Fire service tax | BFP | monthly, 20th of the next month | FST | 2506 |
| 1601-C Withholding on compensation | BIR | monthly, 10th — **reminder only** (payroll) | — | — |
| BIR Form 2307 | BIR | per payee and quarter | EWT | — |

Forms are master data (`tax_form`): authority, frequency (`MONTHLY`, `QUARTERLY`,
`MONTHLY_EXCEPT_QUARTER_END`, `ANNUAL`), due rule (`dueDay` of the month `dueMonthsAfter` months after
the period-end month; day 31 = month end), worksheet, payable / credit account, `trackFiling` and the
first period tracked (`effectiveFrom`).

## 2. Sources of each figure

| Figure | Source | Date basis |
|---|---|---|
| VATable, zero-rated, exempt sales; output VAT | Approved policies and endorsements (`PolicyQueryService.approvedTransactions`), company's net premium and VAT of the `PremiumBreakdown` | approval (accounting) date |
| Purchases (services / capital goods / exempt / zero-rated); input VAT | Approved supplier invoices (`pay_supplier_invoice`, SQL read model) | invoice date |
| EWT on supplier invoices | Supplier invoice net amount and EWT withheld | invoice date |
| EWT on commissions | Commission and withholding of approved policies / endorsements | approval date |
| DST, premium tax, LGT, FST | Levies of the `PremiumBreakdown` of approved policies / endorsements | approval date |
| Ledger control of each worksheet | `gl_ledger_entry` movement of the GL accounts of the authorized tax codes of the type, excluding tax-module journals and year-end closing | value date |
| IC schedules | `gl_daily_balance` (balances) and `gl_ledger_entry` (movements by line of business) through the IC mapping | as of / period |

Every worksheet lists its source documents (drill-down to the policy or the invoice list) and a
**reconciliation**: documents total, ledger movement and difference per tax account. Amounts in
foreign currency are converted at the rate stored on the document (policy approval rate; invoice
base / payable ratio).

### Computations

- **VAT (2550Q)**: payable = output VAT − (input VAT + excess input VAT carried over from the previous
  quarter's FILED/PAID 2550Q). A negative result is carried over (`excessCredit`).
- **EWT**: per ATC (payee's default ATC), income payments and tax withheld. For a quarter, tax still
  due = total withheld − amounts of the monthly EWT returns (0619-E) FILED/PAID in the quarter.
- **Levies**: tax due = Σ levy per document, summarised by line of business.
- **BIR 2307**: `Certificate2307Aggregator` groups the quarter's EWT entries per payee and ATC and
  splits the income by month of the quarter (1st/2nd/3rd); negative entries (commission recovery)
  reduce the month in which they occur.

## 3. Returns lifecycle and remittance

`DRAFT` (prepared from the worksheet; *refresh* recomputes) → `FILED` (filing date and eFPS /
eBIRForms reference; the filer must differ from the preparer) → `PAID`. A draft can be `CANCELLED`;
one live return per form and period (unique index).

Paying posts `TAX_REMITTANCE` (components `TAX_PAYABLE`, `TAX_CREDIT`, `AMOUNT`; roles
`@TAX_PAYABLE`, `@TAX_CREDIT`, `@BANK`), source reference `TAXRET:<id>`:

- forms with a credit account (VAT): Dr payable (tax due), Cr credit account (credits applied up to the
  tax due; an excess stays on the account), Cr bank (amount payable);
- other forms: Dr payable, Cr bank for the amount payable.

Nothing is posted for a return without tax due. The remittance (`tax_remittance`) records payment
date, bank, reference, amounts and journal (report `TAX-REMIT`).

**Alerts** (`TaxFilingAlertCheck`, daily job `ALERT_DAILY_CHECKS`): `TAX_RETURN_DUE` (threshold days,
default 15) and `TAX_RETURN_OVERDUE` for tracked forms of the current and previous year, one open
alert per form and period.

## 4. BIR list exports (relief-style CSV)

`GET /api/v1/tax/exports/{SLS|SLP|QAP}?companyId&year&quarter`. Text fields quoted, names upper case,
amounts with 2 decimals and no separators, CRLF line ends, file `<TIN><LIST><YYYY>Q<n>.csv`. RDO
from parameter `TAX_RDO_CODE`.

**SLS** (one D line per customer):

```
H,S,"<owner TIN>","<owner name>","","","","<trade name>","<address>","",<exempt>,<zero-rated>,<taxable>,<output VAT>,<RDO>,<MM/DD/YYYY quarter end>,12
D,S,"<TIN>","<registered name>","<last>","<first>","<middle>","<address>","",<exempt>,<zero-rated>,<taxable>,<output VAT>,<owner TIN>,<MM/DD/YYYY>
```

**SLP** (one D line per supplier):

```
H,P,"<owner TIN>","<owner name>","","","","<trade name>","<address>","",<exempt>,<zero-rated>,<services>,<capital goods>,<other goods>,<input VAT>,<creditable input VAT>,<non-creditable>,<RDO>,<MM/DD/YYYY>,12
D,P,"<TIN>","<registered name>","<last>","<first>","<middle>","<address>","",<exempt>,<zero-rated>,<services>,<capital goods>,<other goods>,<input VAT>,<owner TIN>,<MM/DD/YYYY>
```

**QAP** (1601-EQ alphalist, one D1 line per payee and ATC, then a control record):

```
HQAP,H1601EQ,<owner TIN>,<branch>,"<owner name>",<MM/YYYY>,<RDO>
D1,1601EQ,<seq>,<TIN>,<branch>,"<registered name>","<last>","<first>","<middle>",<MM/YYYY>,<ATC>,<rate>,<income>,<tax>
C1,1601EQ,<owner TIN>,<branch>,<MM/YYYY>,<total income>,<total tax>
```

Individuals (tax profile `INDIVIDUAL`) are reported by last / first / middle name with an empty
registered name; others by registered name.

**BIR 2307 PDF** (`/api/v1/tax/2307/certificates/{id}/pdf`, batch `/batches/{id}/pdf`): period,
Part I payee (TIN, name, address, ZIP) and payor information, Part II income payments by ATC with
1st / 2nd / 3rd month, total and tax withheld for the quarter, declaration and signature blocks.
Payee and payor facts are copied at issue so reprints do not change.

## 5. IC schedules and the mapping

`tax_ic_line_item` (maker-checker) maps each line of a schedule (`PREMIUMS`, `LOSSES`, `COMMISSIONS`,
`NET_WORTH`, `RBC`, `RESERVES`, `INVESTMENTS`) to an account code range and/or a chart report group,
with the natural side (`DEBIT`/`CREDIT`), a sign (+1 adds, −1 deducts), the measure (`BALANCE` as of
the end date, or `MOVEMENT` of the period without year-end closing) and, for RBC, a factor in percent.
Premiums, losses and commissions are analysed by the line-of-business dimension of the ledger.

**RBC (simplified template)**: requirement = Σ line amount × factor (no covariance aggregation of
the full IC RBC2 framework); available capital = net worth schedule total; ratio = capital /
requirement × 100 against parameter `IC_RBC_HURDLE_PERCENT` (default 100). Seed factors: FVPL 30 %,
FVOCI 15 %, placements 1 %, insurance receivables 10 %, reinsurance assets 5 %, net premiums 15 %,
claims reserves 10 %, operational risk 2 % of gross premiums. Reserves and investments are read from
ledger balances only (no dependency on the reserves or investment modules).

## 6. Reports (category Tax & Statutory, permission `TAX_VIEW`)

`TAX-VAT-2550Q`, `TAX-SLS`, `TAX-SLP`, `TAX-EWT-1601EQ`, `TAX-QAP`, `TAX-2307-REG`, `TAX-DST-2000`,
`TAX-PREMTAX` (premium tax / LGT / FST), `TAX-REMIT`, `IC-PREM-LOB`, `IC-LOSS-LOB`, `IC-COMM-LOB`,
`IC-NETWORTH`, `IC-RBC`, `IC-RESERVES`, `IC-INVEST` — all exportable to PDF, Excel and CSV.

## 7. Permissions

`TAX_VIEW` (FIN_ADMIN, FIN_MANAGER, ACCOUNTANT, AUTHORIZER, AUDITOR) and `TAX_MANAGE` (FIN_ADMIN,
FIN_MANAGER, ACCOUNTANT, AUTHORIZER). Masters and IC mappings are authorized with `MASTER_AUTHORIZE`
and appear in the approval inbox.

## 8. Assumptions

1. Premium taxes and output VAT are reported in the period of the policy / endorsement approval
   (accounting) date; return premiums reduce the period in which they are approved.
2. VAT is charged on the company's net premium only (not on the policy fee), as the premium calculator
   does; without VAT a sale is zero-rated when the customer's profile says `ZERO_RATED`, else exempt.
3. All input VAT is creditable (no apportionment to exempt / non-VAT sales); purchases booked to an
   asset account are capital goods, others services; "other goods" is not distinguished.
4. Withholding is reported when the income is payable (invoice / commission accrual date), not when
   paid; the amount withheld is the one on the document, and a rate different from the ATC is noted.
5. LGT is computed on the quarter's premiums; the Local Government Code bases it on the previous
   year's gross receipts — enter the LGU assessment through a manual adjustment if it differs.
6. Due dates are not moved for weekends or holidays (e-filers file on or before the date).
7. 2307 certificates are computer-generated reproductions of the official layout.
8. The RDO code is one global parameter (single Philippine company).
9. Seed ATCs and rates follow RR 11-2018 as understood; commissions use WI515 / WC515 at 10 %.

## 9. Before go-live, the tax officer must

1. Confirm every tax code, ATC, rate and GL account (Tax Codes & Forms) against current BIR issuances
   and the company's chart; authorize them.
2. Configure the forms (due rules, payable / credit accounts, `effectiveFrom` = first period filed in
   BrokerVerse) and mark forms filed elsewhere as reminders.
3. Create party tax profiles (TIN, registered name, individual name parts, VAT treatment, default
   ATC) for every supplier, agent, broker and zero-rated / exempt customer; worksheets list payees
   still `UNMAPPED`.
4. Set `TAX_RDO_CODE` and the company TIN / registered address (company master).
5. Configure and authorize the `TAX_REMITTANCE` accounting rule for the company.
6. Map the IC schedules to the chart and confirm the RBC factors and hurdle.
7. Tune thresholds of `TAX_RETURN_DUE` / `TAX_RETURN_OVERDUE` (Administration → Exception Codes).
8. Reconcile the opening balances of the tax payable accounts (returns of periods before go-live are
   not cleared by BrokerVerse).
