# BDOI Report List (as of 27-Apr-2026) - Cross-BRD Report Traceability

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI). Platform: iNXT BrokerVerse (BIBS).

Status: **for BDOI concurrence.** This document maps every report on BDOI's consolidated report list to the BRD
requirement that defines it, to the report code built or designed in BIBS, and to a status. It is consolidated later
with the review workbook; it has no Flyway range.

## 1. Source and method

- Source: `docs/source-documents/Report List as of APR-27-2026.pdf`, 58 landscape pages, one table with the columns BRD,
  Report Name, Description, Frequency, Format, Fields, PDE / AR, Remarks, New / Existing, Sample / Template link, Source,
  SME, Covered Period, File Naming Convention, Completed?. **185 reports.** The table was extracted page by page; rows
  that continue across a page break were merged, and garbled cells (overlapping text) were checked on the rendered page.
- Report codes built: every `ReportDefinition` under `backend/src/main/java/com/iortatechnxt/brokerverse/**/report/`
  (about 190 distinct codes: `GL-*`, `FIN-*`, `TAX-*`, `CTL-*`, `PGIBR*`, `NB-*`, `PM-*`, `CSH-*`, `REM-*`, `PRC-*`, `ADJ-*`,
  `CMR-*` and the insurer-side reports). `collections`, `disbursement`, `payrequest`, `acsl` and `frbs` hold only their
  foundation package today.
- Report codes designed: `OPERATIONS_DESIGN.md` section 11, `COLLECTIONS_DESIGN.md` section 11,
  `ACCOUNTING_DISBURSEMENT_DESIGN.md` section 10, `PRODUCT_MAINTENANCE_DESIGN.md`, `BROKING_ARCHITECTURE.md` section 16
  and `SUBMITTED_POLICIES_DESIGN.md` section 11. Requirement IDs come from the BRD specs in `docs/requirements/`.

Statuses:

| Status | Meaning |
|---|---|
| **BUILT** | A report with that content runs today in the Report Centre (PDF / XLSX / ODS / CSV / XML) |
| **BUILT (partial)** | A built report or screen covers most of it; the gap is named in the note (variant, column, filter) |
| **DESIGNED** | Specified with a code in a build design, not built yet |
| **MISSING** | No report and no design. The note proposes the owning module (and a code where the owner is known) |
| **DROPPED** | The list itself marks the row as deleted or no longer needed |

## 2. Summary

| Status | Reports |
|---|---|
| BUILT | 70 |
| BUILT (partial) | 23 |
| DESIGNED | 47 |
| MISSING | 42 |
| DROPPED | 3 |
| **Total** | **185** |

| Group (list column "BRD") | Reports | BUILT | partial | DESIGNED | MISSING | DROPPED |
|---|---|---|---|---|---|---|
| FRBS (BRD-5 Accounting) | 31 | 27 | 4 | 0 | 0 | 0 |
| Disbursement (BRD-5) | 12 | 0 | 0 | 12 | 0 | 0 |
| ACSL (BRD-5) | 15 | 0 | 2 | 13 | 0 | 0 |
| Collection Management (BRD-4) | 4 | 1 | 1 | 2 | 0 | 0 |
| Operations (BRD-2) | 35 | 32 | 1 | 0 | 2 | 0 |
| New Business / Renewal / other (BRD-1 and unassigned) | 33 | 7 | 10 | 4 | 9 | 3 |
| Renewal (Renewal BRD) | 3 | 0 | 0 | 0 | 3 | 0 |
| Product Maintenance / TSU (BRD-3) | 9 | 3 | 5 | 0 | 1 | 0 |
| Submitted Policies (BRD-12) | 16 | 0 | 0 | 16 | 0 | 0 |
| Reinsurance (no BRD received) | 4 | 0 | 0 | 0 | 4 | 0 |
| Customer Service Facility | 5 | 0 | 0 | 0 | 5 | 0 |
| Claims | 6 | 0 | 0 | 0 | 6 | 0 |
| Sanction Screening and RPR | 6 | 0 | 0 | 0 | 6 | 0 |
| Employee Benefits | 6 | 0 | 0 | 0 | 6 | 0 |

Reading: the reports of the BRDs already built (Operations, BRD-1, Product Maintenance) are mostly BUILT. BRD-4 and
BRD-5 reports are DESIGNED and wait for their build waves. The MISSING reports belong mainly to BRDs that have no build
design yet (Renewal, Claims, Customer Service Facility, Sanction Screening, Employee Benefits, a Reinsurance BRD that
is not in the pack) and to a small set of BRD-1 reports that no BRNB requirement names.

## 3. Gaps: missing reports and the proposed owner

| # | Report | Proposed owner | Proposed code | Note |
|---|---|---|---|---|
| 60 | Generated Proposal Report | `nbreport` | `NB-PROPOSALS` | Proposals / quotations with status (BRNB.043/075 read broadly) |
| 105 | Summary Per Disposition - Remittance (unapplied by disposition) | `cashiering` | `CSH-UNAPPLIED-DISPOSITION-SUM` | Disposition list of the row answers OQ15 in part |
| 127 | Webform Masterlist | `catalog` / `productmaint` | `PM-WEBFORM-FIELDS` | Field dictionary of the package forms |
| 129 | Reversed Cancellation and Error Count | `adjustment` | `ADJ-REVERSED-CANCEL` | Not in the Operations Annex; `ADJ-REGISTER` is partial |
| 131, 132 | Trade Accumulation / Trade Monitoring (TPC system) | `placement` + `nbreport` | `NB-TRADE-ACCUM`, `NB-TRADE-MONITOR` | Needs a TPC trade intake that no analysed BRD defines: new question |
| 139 | Conversion Report - Home (CARI / pre-selling to FIP) | `nbreport` (+ `account` history) | `NB-HOME-CONVERSION` | Insurance-type change history |
| 141 | Hold Cover Report | `nbreport` | `NB-HOLD-COVER` | Over `plc_hold_cover`; submitted renewals appear too |
| 143, 144 | Booked Non-Bank Client / Booked PEP Client (quarterly RCTC) | `nbreport` + `crm` | `NB-RCTC-NONBANK`, `NB-RCTC-PEP` | PEP flag and screening result from the Sanction Screening BRD |
| 167 | Insurer's scorecard | `nbreport` | `NB-INSURER-SCORECARD` | Policies received vs placed, TAT, quarterly |
| 169 | Sustainability Report - Risk Management | `frbs` | `FRBS-SUSTAINABILITY-PROD` | Built by A1-FRBS; yearly, first Monday of January |
| 178 | Sustainability Report - Claims | broker-claims module | - | Claims BRD |
| 76, 78, 79 | List of Expiring Accounts, Renewal Status Report, Generated Renewal Advice | future `renewal` module | - | Submitted part designed in BRD-12 (`SBM-RENEWABLE`, `SBM-LETTERS`) |
| 112-115 | Reinsurance (premium payment and RI commission, RI production, RI placement list, audit trails) | future reinsurance-broking module | - | The Reinsurance BRD is not in the pack; the platform `reinsurance` module is insurer-side |
| 116-119, 174 | Customer Service Facility reports | future `csf` module | - | CSF BRD |
| 120-125 | Claims reports | future broker-claims module | - | Claims BRD; insurer-side analogues exist (`PGIBR002/012/018/023/028`) |
| 145-150 | Sanction Screening and RPR reports | future screening module | - | Sanction Screening BRD |
| 170-173, 175, 176 | Employee Benefits reports | future employee-benefits module | - | EB BRD |

Partial rows that need a small change (variant, column or filter) are listed with their note in section 4; the
notable ones are the day-book cash reports (#6, #7), the outstanding-check aging (#13), the three CLPC billing variants
(#65-67), the account-without-policy age in working days (#166) and the policy-delivery TAT (#185).

## 4. Report-by-report mapping

Columns: # = row in the list; p. = page; Group = the list's "BRD" column as written; N/E = New / Existing as written.

| # | p. | Group | Report (as written) | N/E | Defining BRD / requirement | Code | Status | Owner / note |
|---|---|---|---|---|---|---|---|---|
| 1 | 1 | FRBS | Statement of Condition (Daily; Monthly; Year-to-date movement) | Existing | FRBS 3.2.0, App. A I | `FIN-MIS-BS` (also `GL-BS`) | **BUILT** | finreport; daily / monthly / YTD as parameters |
| 2 | 1 | FRBS | Income Statement (Daily; Monthly; Year-to-date movement) | Existing | FRBS 3.2.0, App. A I | `FIN-MIS-IE` (also `GL-PL`) | **BUILT** | finreport |
| 3 | 1 | FRBS | Trial Balance (Daily; Monthly; Year-to-date movement) | Existing | FRBS 3.2.0, App. A I | `FIN-TB-MAIN`, `FIN-TB-YTD` (also `GL-TB`) | **BUILT** | finreport |
| 4 | 1 | FRBS | Journal Entries | Existing | FRBS 3.2.0, App. A I | `FIN-GL-DAYBOOK`, `GL-JRNL` | **BUILT** | finreport |
| 5 | 1 | FRBS | Subsidiary Ledger | Existing | FRBS 3.2.0, App. A I | `FIN-GL-SUBLEDGER-LC` / `-FC` | **BUILT** | finreport |
| 6 | 1 | FRBS | Cash Disbursements | Existing | FRBS 3.2.0, App. A I | `FIN-GL-DAYBOOK` variant (journal type PAYMENT) | **BUILT (partial)** | finreport; saved variant; payee / TIN columns to confirm |
| 7 | 2 | FRBS | Cash Receipts | Existing | FRBS 3.2.0, App. A I | `FIN-GL-DAYBOOK` variant (RECEIPT); OR / AR detail in `CSH-DAILY-CASH-REC` | **BUILT (partial)** | finreport + cashiering; OR / AR columns (TIN, income type, VAT, WTax) are not in the day book |
| 8 | 2 | FRBS | Invoice Register | Existing | FRBS 3.2.0, App. A IV | `NB-BOOKED-REG` | **BUILT (partial)** | nbreport; 'same details as Production report': align with `CLX-FULL-PRODUCTION` |
| 9 | 2 | FRBS | Schedule of Commission Income | Existing | FRBS 3.2.0, App. A IV (commission income) | `GL-SCHEDULE` definition `SCH-COMMISSION-INCOME` | **BUILT** | finreport schedule engine (A1-FRBS); layout to confirm (AQ05) |
| 10 | 3 | FRBS | Expense Allocation per cost center | Existing | FRBS 3.2.0, App. A IV (expense grouping) | `FRBS-EXPENSE-GROUPING` | **BUILT** | frbs (A1-FRBS); also schedule `SCH-EXPENSE-CC` |
| 11 | 3 | FRBS | SA Reconciliation | Existing | FRBS 3.2.0, App. A IV | `FIN-BRS-STMT` | **BUILT** | receivables (bank reconciliation) |
| 12 | 3 | FRBS | CA Reconciliation | Existing | FRBS 3.2.0, App. A IV | `FIN-BRS-STMT` | **BUILT** | receivables |
| 13 | 3 | FRBS | Aging of Outstanding checks | Existing | FRBS 3.2.0, App. A IV (checks and other cash items) | `FIN-BRS-UNREC-BOOK` | **BUILT (partial)** | receivables; aging buckets (current-30 ... over 365) missing; check statuses come from `disbursement` |
| 14 | 3 | FRBS | Revaluation Report | Existing | FRBS 3.2.0, App. A IV (revaluation) | `GL-FXREV` | **BUILT** | closing |
| 15 | 3 | FRBS | Market Performance Summary – Premium Commission | Existing | FRBS 3.2.0, App. A V | `FRBS-MANCOM-MARKET` | **BUILT** | frbs (A1-FRBS); budget columns wait for segment budgets (AQ05) |
| 16 | 4 | FRBS | Branch Production report per BDO Branch | Existing | FRBS 3.2.0, App. A V | `FRBS-BRANCH-PRODUCTION`, `FRBS-BRANCH-PRODUCTION-SUM` | **BUILT** | frbs (A1-FRBS); BDO branch codes = sales units until AQ05 |
| 17 | 4 | FRBS | Service Fee report | Existing | FRBS 3.2.0, App. A VI (FRBS 2.10.0) | `FRBS-SERVICE-FEE`, `FRBS-SERVICE-FEE-DETAIL` | **BUILT** | frbs (A1-FRBS); add handling-fee and No Touch fee income (BRD-12) |
| 18 | 4 | FRBS | General Journal (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-GJ` | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 19 | 4 | FRBS | Purchase Journal (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-PJ` | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 20 | 5 | FRBS | Sales Revenue Journal (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-SJ` | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 21 | 5 | FRBS | Cash Receipts (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-CRB` | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 22 | 5 | FRBS | Cash Disbursements (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-CDB` | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 23 | 5 | FRBS | General Ledger – Assets (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-SL` (assets) | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 24 | 6 | FRBS | General Ledger – Liabilities (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-SL` (liabilities) | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 25 | 6 | FRBS | General Ledger – Capital (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-SL` (capital) | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 26 | 6 | FRBS | General Ledger – Income (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-SL` (income) | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 27 | 6 | FRBS | General Ledger – Expenses (Books of Accounts) | Existing | FRBS 3.2.0, App. A VII | `TAX-BOOK-SL` (expenses) | **BUILT** | tax (A1-FRBS); format to confirm (AQ07) |
| 28 | 6 | FRBS | Monthly Alphalist of Payees | Existing | FRBS 3.2.0, App. A VII | `TAX-MAP` | **BUILT** | tax (A1-FRBS); quarterly `TAX-QAP` built; DAT format AQ07 |
| 29 | 7 | FRBS | Value Added Tax Payable (Output VAT and Input VAT) | Existing | FRBS 3.2.0, App. A VII | `TAX-VAT-2550Q`, `TAX-SLS`, `TAX-SLP` | **BUILT** | tax |
| 30 | 7 | FRBS | Summary of Alphalist of Withholding Taxes | Existing | FRBS 3.2.0, App. A VII | `TAX-SAWT` | **BUILT** | tax (A1-FRBS), from the received-certificate register (V702) |
| 31 | 7 | FRBS | Broker's Annual Statement of Business Operations | Existing | FRBS 3.2.0, App. A VII | `IC-BROKER-ASBO` | **BUILT** | tax (A1-FRBS); IC layout to confirm (AQ07) |
| 32 | 8 | Disbursement | Masterlist of all Disbursement Transactions | Existing | DIS 3.28.3, App. B | `DSB-MASTERLIST` | **DESIGNED** | disbursement |
| 33 | 8 | Disbursement | List of Unreleased Check | Existing | DIS 3.28.3, App. B | `DSB-UNRELEASED-CHECKS` | **DESIGNED** | disbursement |
| 34 | 9 | Disbursement | CWT on Commission Report | Existing | DIS 3.28.3, App. B | `DSB-CWT-COMMISSION` | **DESIGNED** | disbursement |
| 35 | 9 | Disbursement | Authority to Debit Report | Existing | DIS 3.28.3, App. B | `DSB-ATD` | **DESIGNED** | disbursement |
| 36 | 10 | Disbursement | Stale Checks Report | Existing | DIS 3.28.3, App. B | `DSB-ML-STALE` | **DESIGNED** | disbursement |
| 37 | 10 | Disbursement | Cash Flow Report | Existing | DIS 3.28.3, App. B | `DSB-CASH-FLOW` | **DESIGNED** | disbursement |
| 38 | 11 | Disbursement | Disbursement request upload fall out report | New | DIS 3.28.x, App. B | `DSB-UPLOAD-FALLOUT` | **DESIGNED** | disbursement (bulk error report) |
| 39 | 11 | Disbursement | Generate Direct Credit Transaction text file | Existing | DIS 3.28.x, App. B | DCTF file (`NotificationFormat.DCTF`) | **DESIGNED** | disbursement / payables |
| 40 | 11 | Disbursement | Remittance End-of-day | Existing | DIS 3.28.2, App. B | `DSB-EOD-REMIT` | **DESIGNED** | disbursement |
| 41 | 11 | Disbursement | Refund End-of-day | - | DIS 3.28.2, App. B | `DSB-EOD-REFUND` | **DESIGNED** | disbursement |
| 42 | 12 | Disbursement | Summary End-of-day | - | DIS 3.28.2, App. B | `DSB-EOD-SUMMARY` | **DESIGNED** | disbursement |
| 43 | 12 | ACSL | SCHEDULE OF PAYABLE TO INSURANCE COMPANIES - Peso | Existing - Isys | ACSL 2.14.x, App. C | `ACSL-SCHED-PAY-INS-PHP` | **DESIGNED** | acsl |
| 44 | 13 | ACSL | SCHEDULE OF PAYABLE TO INSURANCE COMPANIES - USD | Existing - Isys | ACSL 2.14.x, App. C | `ACSL-SCHED-PAY-INS-USD` | **DESIGNED** | acsl |
| 45 | 13 | ACSL | SCHEDULE OF RECEIVABLE FROM INSURANCE COMPANIES CLIENTS - PESO | Existing - Isys | ACSL 2.14.x, App. C | `GL-SCHEDULE` definition `SCH-PR-PHP`; `ACSL-AGING-PR-PHP` (aging) | **BUILT (partial)** | finreport schedule engine (A1-FRBS): schedule per client with ageing 30/90/180/365/730, layout to confirm (AQ05); the ACSL ageing report stays with acsl |
| 46 | 14 | ACSL | SCHEDULE OF RECEIVABLE FROM INSURANCE COMPANIES CLIENTS - USD | Existing - Isys | ACSL 2.14.x, App. C | `GL-SCHEDULE` definition `SCH-PR-USD`; `ACSL-AGING-PR-USD` (aging) | **BUILT (partial)** | finreport schedule engine (A1-FRBS): schedule per client with ageing 30/90/180/365/730, layout to confirm (AQ05); the ACSL ageing report stays with acsl |
| 47 | 14 | ACSL | SCHEDULE OF A/P REFUND FROM INSURER - Peso | New | ACSL 2.14.x, App. C | `ACSL-SCHED-AP-REFUND-INS` | **DESIGNED** | acsl |
| 48 | 14 | ACSL | SCHEDULE OF AR INSURER'S REFUND - Peso | New | ACSL 2.14.x, App. C | `ACSL-SCHED-AR-INS-REFUND` | **DESIGNED** | acsl |
| 49 | 15 | ACSL | SCHEDULE OF COMMISSION RECEIVABLES - Peso | Existing - Isys | ACSL 2.14.x, App. C | `ACSL-SCHED-COMM-PHP` | **DESIGNED** | acsl |
| 50 | 15 | ACSL | SCHEDULE OF COMMISSION RECEIVABLES - USD | Existing - Isys | ACSL 2.14.x, App. C | `ACSL-SCHED-COMM-USD` | **DESIGNED** | acsl |
| 51 | 16 | ACSL | PAYABLE TO INSURANCE COMPANY AGING REPORT | New | ACSL 2.14.x, App. C | `ACSL-AGING-PAY-INS-PHP` / `-USD` | **DESIGNED** | acsl |
| 52 | 16 | ACSL | RECEIVABLE FROM INSURANCE COMPANIES CLIENTS AGING REPORT | New | ACSL 2.14.x, App. C | `ACSL-AGING-PR-PHP` / `-USD` | **DESIGNED** | acsl |
| 53 | 17 | ACSL | A/P REFUND FROM INSURER AGING REPORT | New | ACSL 2.14.x, App. C | `ACSL-AGING-AP-REFUND-INS` | **DESIGNED** | acsl |
| 54 | 17 | ACSL | AR INSURER'S REFUND AGING REPORT | New | ACSL 2.14.x, App. C | `ACSL-AGING-AR-INS-REFUND` | **DESIGNED** | acsl |
| 55 | 18 | ACSL | COMMISSION RECEIVABLES AGING REPORT | New | ACSL 2.14.x, App. C | `ACSL-AGING-COMM-PHP` / `-USD` | **DESIGNED** | acsl |
| 56 | 18 | ACSL | LIST OF TRANSACTIONS FINANCIAL DETAILS (Extract for SOA Recon) | Existing - Isys | ACSL 2.14.x, App. C | `ACSL-BOOKED-FIN-DETAILS` | **DESIGNED** | acsl |
| 57 | 19 | ACSL | SOA RECONCILIATION | New | ACSL 2.14.1 | `ACSL-SOA-RECON` | **DESIGNED** | acsl |
| 58 | 19 | Collection Management | DP PR for Reversal | Existing | BRCLXN.024/025 | `CLX-DP-FOR-REVERSAL` | **DESIGNED** | collections |
| 59 | 20 | Collection Management | Reports per Unit per Branch | Existing | BRCLXN.028/029 | job `CLX_WEEKLY_FILES` (DP PR / PR 2307 per unit and branch) | **DESIGNED** | collections; content of the weekly set open (CQ09) |
| 60 | 20 | New Business | GENERATED PROPOSAL REPORT | Existing | BRNB.043/075 (no explicit ID) | - | **MISSING** | nbreport: `NB-PROPOSALS` (proposals and quotations with status); lists exist only as screens |
| 61 | 21 | New Business/Renewal | PIPELINE Report | Existing | BRNB.075/115 | `NB-ACC-STATUS` | **BUILT (partial)** | nbreport; renewal accounts join once `account.business_type` exists (BRD-12 design section 9) |
| 62 | 21 | New Business/Renewal/FRBS | FULL PRODUCTION REPORT - New Business/Renewal | Existing | BRNB.075; BRCLXN.045 | `CLX-FULL-PRODUCTION` (designed); `NB-BOOKED-REG` (built) | **DESIGNED** | collections; business origin 'Submitted' comes from BRD-12 |
| 63 | 22 | New Business | Production Statistics Report/Dashboard | New | BRNB.012/075 | `NB-PRODUCTION` + NB Dashboard | **BUILT** | nbreport; 'dashboard format, not an extract' (remark 04/16/2026) |
| 64 | 22 | New Business/Renewal | Audit Log Report | Existing | BRNB.086/089 | `CTL-AUDIT` | **BUILT** | audit / report |
| 65 | 22 | New Business/Renewal | CLPC Billing Report (Non-Built in FIP) | Existing | BRNB.067 | `NB-CLPC-BILLING` | **BUILT (partial)** | placement / nbreport; variant non-built-in FIP (Q28) |
| 66 | 22 | New Business/Renewal | CLPC Billing Report (Built in HLS AMORTIZED and non- built in) | Existing | BRNB.067 | `NB-CLPC-BILLING` | **BUILT (partial)** | variant built-in HLS amortised and non-built-in |
| 67 | 23 | New Business/Renewal | CLPC Billing Report (HLS (AMORTIZED) LoanRelease) | Existing | BRNB.067 | `NB-CLPC-BILLING` | **BUILT (partial)** | variant HLS amortised loan release |
| 68 | 23 | New Business | Report Showing Matched and Unmatched Payments | Existing | BRNB.068/075 | `NB-PAY-MATCH` | **BUILT** | nbreport |
| 69 | 24 | New Business | Insurance Advice Reports | Existing | BRNB.070 | Insurance Advice register (screen) | **DROPPED** | BU: report no longer needed |
| 70 | 24 | New Business/Renewal | Successful and Fall Out Accounts Report per Stage | New | BRNB.075 | `NB-STAGE-OUTCOME` | **BUILT** | nbreport; check the filters listed in the remark |
| 71 | 25 | New Business/Renewal/FRBS | PLACEMENT SUMMARY REPORT | Existing | BRNB.075 | `NB-PLC-SUMMARY` | **BUILT** | nbreport |
| 72 | 25 | New Business/Renewal | Report on Successfully and Unsuccessfully Sent E-Policies | Existing | BRNB.078 | `NB-DISPATCH` | **BUILT** | nbreport; renamed 'Policy Transmittal List' |
| 73 | 25 | New Business | E-Policy Sender Reports | Existing | BRNB.078 | `NB-DISPATCH` | **DROPPED** | remark: DELETE, same as #72 |
| 74 | 25 | All BRD | Dynamic Reports | New | BRNB.057 (Q40) | saved variants (`nbr_report_variant`) | **BUILT (partial)** | report platform; ad-hoc builder parked; BRD-5 adds column filters |
| 75 | 26 | PRODUCT MAINTENANCE | PLACEMENT SUMMARY REPORT - PRODUCT MAINTENANCE | - | BRPM.018 | `PM-PKG-STATUS` | **BUILT** | productmaint (PQ15) |
| 76 | 26 | RENEWAL | List of Expiring Accounts | Existing | Renewal BRD | - | **MISSING** | future `renewal` module; submitted part = `SBM-RENEWABLE` (BRD-12, designed) |
| 77 | 26 | NEW/RENEWAL | List of Accounts for Processing | New | BRNB.075/115 | `NB-ACC-STATUS` variant (stage SUBMITTED) | **BUILT (partial)** | nbreport; saved variant |
| 78 | 27 | RENEWAL | RENEWAL STATUS REPORT | New | Renewal BRD | - | **MISSING** | future `renewal` module |
| 79 | 27 | RENEWAL | Generated Renewal Advice | New | Renewal BRD | - | **MISSING** | future `renewal`; letters register `SBM-LETTERS` covers submitted policies |
| 80 | 28 | Operations - Cashiering | Applied Premium Reports | Existing | CSHID.023, Annex II 1 | `CSH-APPLIED-PREM` | **BUILT** | cashiering |
| 81 | 28 | Operations - Cashiering | Applied Commission Reports | New | CSHID.023, Annex II 2 | `CSH-APPLIED-COMM` | **BUILT** | cashiering |
| 82 | 28 | Operations - Cashiering | Minimal Balance of Unapplied Payments | New | CSHID.023, Annex II 4 | `CSH-MINBAL-EXCESS` | **BUILT** | cashiering |
| 83 | 28 | Operations - Cashiering | Cancelled Official Receipts | New | CSHID.023, Annex II 5 | `CSH-CANCELLED-OR` | **BUILT** | cashiering |
| 84 | 28 | Operations - Cashiering | Cancelled Acknowledgment Receipts | New | CSHID.023, Annex II 6 | `CSH-CANCELLED-AR` | **BUILT** | cashiering |
| 85 | 29 | Operations - Cashiering | Unapplied Commission Receivable Extract for Mancom Reports | New | CSHID.023, Annex II 9 | `CSH-UNAPPLIED-COMM-MANCOM` | **BUILT** | cashiering |
| 86 | 29 | Operations - Cashiering | Unapplied Commission Receivable Payments YTD balance per Criteria | New | CSHID.023, Annex II 10 | `CSH-UNAPPLIED-COMM-YTD` | **BUILT** | cashiering |
| 87 | 29 | Operations - Cashiering | Unpplied Premium Minimal Balance | New | CSHID.023, Annex II 11 | `CSH-MINBAL-PREMIUM` | **BUILT** | cashiering |
| 88 | 29 | Operations - Cashiering | Unapplied Commission Minimal Balance | New | CSHID.023, Annex II 12 | `CSH-MINBAL-COMMISSION` | **BUILT** | cashiering |
| 89 | 29 | Operations - Cashiering | Daily Cash Reconciliation Reports | New | CSHID.023, Annex II 13 | `CSH-DAILY-CASH-REC` | **BUILT** | cashiering |
| 90 | 30 | Operations - Cashiering | Unapplied Premium Payment Transaction | New | CSHID.023, Annex II 14 | `CSH-ADVANCE-PAYMENT` | **BUILT** | cashiering |
| 91 | 30 | Operations - Cashiering | Direct Payment | New | CSHID.023, Annex II 16 | `CSH-DIRECT-PAYMENT` | **BUILT** | cashiering |
| 92 | 30 | Operations - Cashiering | Reinstatement Monitoring Report | New | CSHID.023, Annex II 17 | `CSH-REINSTATEMENT-MON` | **BUILT** | cashiering |
| 93 | 31 | Operations - Cashiering | Re-Application Report | New | CSHID.023, Annex II 18 | `CSH-REAPPLICATION` | **BUILT** | cashiering |
| 94 | 31 | Operations - Cashiering | Certification of Payment | New | CSHID.023, Annex II 19 | `CSH-CERT-OF-PAYMENT` | **BUILT** | cashiering |
| 95 | 31 | Operations - Cashiering | Reinstatement | New | CSHID.023, Annex II 20 | `CSH-REINSTATEMENT` | **BUILT** | cashiering |
| 96 | 31 | Operations - Cashiering | CWT Monitoring Report (Premium) | New | CSHID.023, Annex II 21 | `CSH-CWT` | **BUILT** | cashiering |
| 97 | 32 | Operations - Commission | CWT Monitoring Report (Commission) | New | CMRID.015; DIS 3.28.3 | `CMR-BIR-CERT` (built); `DSB-CWT-COMMISSION` (designed) | **BUILT (partial)** | commission; WTax base / amount columns to align |
| 98 | 32 | Operations - Remittance | Remittance Tracker | New | RMTID.039, Annex III 1 | `REM-TRACKER` | **BUILT** | remittance |
| 99 | 32 | Operations - Remittance | Special Remittance Register | New | RMTID.039, Annex III 2 | `REM-SPECIAL-REGISTER` | **BUILT** | remittance |
| 100 | 33 | Operations - Remittance | Remittance Schedule Normal/Special | Existing | RMTID.039, Annex III 3/4 | `REM-SCHEDULE-NORMAL`, `REM-SCHEDULE-SPECIAL` | **BUILT** | remittance |
| 101 | 33 | Operations - Remittance | DTIP Report For Remittance (Summary/Detailed) | New | RMTID.039, Annex III 5/6 | `REM-DTIP-SUMMARY`, `REM-DTIP-DETAIL` | **BUILT** | remittance |
| 102 | 34 | Operations - Remittance | Remittance Schedule with Incentives | New | RMTID.039, Annex III 7 | `REM-SCHEDULE-INCENTIVE` | **BUILT** | remittance |
| 103 | 34 | Operations - Remittance | List of Remitted Accounts with Batch Number | Existing | RMTID.039, Annex III 8 | `REM-REMITTED-BATCH` | **BUILT** | remittance |
| 104 | 35 | Operations - Remittance | Production Reconciliation Summary | New | PRCID, Annex IV 1 | `PRC-SUMMARY` | **BUILT** | prodrecon (the list files it under Remittance) |
| 105 | 35 | Operations - Cashiering | Summary Per Disposition - Remittance | New | CSHID.024 (OQ15) | - | **MISSING** | cashiering: `CSH-UNAPPLIED-DISPOSITION-SUM` (unapplied by disposition type); the list tags it 'Remittance' |
| 106 | 35 | Operations - Production Reconciliation | Production Register | New | PRCID, Annex IV 5 | `PRC-REGISTER` | **BUILT** | prodrecon |
| 107 | 36 | Operations - Production Reconciliation | Production Reconciliation | New | PRCID, Annex IV 1 | `PRC-SUMMARY` | **BUILT** | prodrecon |
| 108 | 36 | Operations - Production Reconciliation | Unbooked Accounts | New | PRCID.019 | `PRC-UNBOOKED` | **BUILT** | prodrecon; the list says 'to be added' |
| 109 | 36 | Operations - Production Reconciliation | Summary For UnmatchedAccounts Per Location | New | PRCID, Annex IV 2 | `PRC-UNMATCHED-LOC` | **BUILT** | prodrecon |
| 110 | 37 | Operations - Production Reconciliation | Summary For UnmatchedAccounts Per Marketing AO/AB | New | PRCID, Annex IV 3 | `PRC-UNMATCHED-AO` | **BUILT** | prodrecon |
| 111 | 37 | Operations - Production Reconciliation | Summary Per Disposition - Prod Recon | New | PRCID, Annex IV 4 | `PRC-DISPOSITION` | **BUILT** | prodrecon |
| 112 | 37 | Reinsurance | Premium Payment and Collection of RI Commission Report | Existing | Reinsurance BRD (not received) | - | **MISSING** | future reinsurance-broking module; the platform `reinsurance` module is insurer-side (RI-*) |
| 113 | 38 | Reinsurance | Reinsurance Production Report | Existing | Reinsurance BRD (not received) | - | **MISSING** | as #112 |
| 114 | 38 | Reinsurance | List of Accounts for Reinsurance Placement | Existing | Reinsurance BRD (not received) | - | **MISSING** | as #112 |
| 115 | 39 | Reinsurance | Audit trails and logs for placement booking remittance claims and communications | New | Reinsurance BRD (not received) | - | **MISSING** | as #112; `CTL-AUDIT` covers the platform audit trail |
| 116 | 39 | Customer Service Facility | Customer Update Information Audit Trail Report | Existing | Customer Servicing Facility BRD | - | **MISSING** | future `csf` module; crm client changes appear in `CTL-AUDIT` |
| 117 | 39 | Customer Service Facility | Case Logging System Report | Existing | Customer Servicing Facility BRD | - | **MISSING** | future `csf` |
| 118 | 40 | Customer Service Facility | Case Resolution Rating Report | Existing | Customer Servicing Facility BRD | - | **MISSING** | future `csf` |
| 119 | 40 | Customer Service Facility | Past TAT Cases Report | Existing | Customer Servicing Facility BRD | - | **MISSING** | future `csf` |
| 120 | 40 | Claims | Outstanding Claims | Existing | Claims BRD | - | **MISSING** | future broker-claims module; insurer analogue `PGIBR018` |
| 121 | 40 | Claims | Settled Claims | Existing | Claims BRD | - | **MISSING** | future broker-claims; insurer analogue `PGIBR002` |
| 122 | 41 | Claims | Outstanding Claims 90 Days Past Due | Existing | Claims BRD | - | **MISSING** | future broker-claims |
| 123 | 41 | Claims | Claims Aging Report | Existing | Claims BRD | - | **MISSING** | future broker-claims |
| 124 | 42 | Claims | Loss Experience Report | Existing | Claims BRD | - | **MISSING** | future broker-claims; insurer analogue `PGIBR023` |
| 125 | 42 | Claims | Loss Ratio Report | Existing | Claims BRD | - | **MISSING** | future broker-claims; insurer analogues `PGIBR012` / `PGIBR028` |
| 126 | 42 | Product Maintenance | Approved Packaged Program per Risk | Existing | BRPM.006/007/017/018 | `PM-PKG-STATUS`, `PM-PKG-EXPIRY`, `PM-VERSION-HISTORY` | **BUILT (partial)** | productmaint; one consolidated view per risk is missing |
| 127 | 42 | Product Maintenance | Webform Masterlist | - | BRPM (field dictionary, no ID) | - | **MISSING** | catalog / productmaint: `PM-WEBFORM-FIELDS` over the minimum-field matrix |
| 128 | 42 | Operations - Adjustment/Cancellation | Daily Adjustment | New | ADJID.016 | `ADJ-DAILY` | **BUILT** | adjustment |
| 129 | 43 | Operations - Adjustment/Cancellation | Reversed Cancellation and Error Count | New | ADJID (no ID) | - | **MISSING** | adjustment: `ADJ-REVERSED-CANCEL` (reversed cancellations and error count); `ADJ-REGISTER` is partial |
| 130 | 43 | Operations - Adjustment/Cancellation | Adjustment Transaction Validation List | New | ADJID.017 | `ADJ-VALIDATION-LIST` | **BUILT** | adjustment |
| 131 | 43 | New Business | Trade Accumulation Report | Existing | none (TPC trade) | - | **MISSING** | placement / nbreport: trade intake from the TPC system is in no BRD analysed so far |
| 132 | 44 | New Business | Trade Monitoring Report | Existing | none (TPC trade) | - | **MISSING** | as #131 |
| 133 | 44 | Submitted Policies | Policy Review Conversion Report | New | BRD-12 BRIDSP-20/29 | `SBM-PR-CONVERSION` | **DESIGNED** | submitted |
| 134 | 44 | Submitted Policies | Policy Review Monitoring Report | - | BRD-12 BRIDSP-06/20 | `SBM-PR-MONITORING` | **DESIGNED** | submitted |
| 135 | 44 | Renewal/Submitted Policy | Renewal Persistency Report | Existing | Renewal BRD; BRD-12 | `SBM-PERSISTENCY` (submitted part) | **DESIGNED** | submitted for submitted accounts; general persistency: future `renewal` |
| 136 | 45 | Collection Management | BIR 2307 | Existing | BRCLXN.026; CSHID.026/027 | `CLX-PR2307-FOR-REVERSAL` (designed); `CSH-2307-TXN`, `TAX-2307-REG` (built) | **BUILT (partial)** | collections + cashiering |
| 137 | 45 | Collection Management | Check pick-up | Existing | CSHID.009 | `CSH-CHECK-PICKUP` | **BUILT** | cashiering |
| 138 | 45 | New Business / Submitted Policies | Penetration Report | Existing | BRD-12 (Report List remark) | `SBM-PENETRATION`, `SBM-HOLD-COVER-GAP` | **DESIGNED** | submitted |
| 139 | 46 | New Business | Conversion Report - Home | Existing | none (NB: CARI / pre-selling to FIP) | - | **MISSING** | nbreport / account: history of insurance-type changes |
| 140 | 46 | New Business | Client and Account Maintenance Status Report | New | BRNB.024/025 | bulk job summary and error report | **BUILT (partial)** | bulk; not in the Report Centre; add `NB-BULK-STATUS` |
| 141 | 46 | New Business/Renewal | Hold Cover Report | - | BRNB.072/103 | - | **MISSING** | nbreport: `NB-HOLD-COVER` over `plc_hold_cover` (submitted renewals included) |
| 142 | 46 | New Business | Account Creation and Maintenance | - | BRNB.067 | `NB-CLPC-BILLING` / billing batch file | **BUILT (partial)** | placement: generation on upload of the daily insurance report (Home) is a change |
| 143 | 46 | New Business/Renewal | Booked Non-Bank Client | New | none (RCTC / compliance) | - | **MISSING** | nbreport + crm: booked non-bank clients per quarter; needs the sanction-screening result |
| 144 | 47 | New Business/Renewal | Booked PEP Client | New | none (RCTC / compliance) | - | **MISSING** | as #143 with the PEP flag |
| 145 | 47 | Sanction Screening and RPR | Extract list of High-risk Clients | New | Sanction Screening BRD | - | **MISSING** | future screening module |
| 146 | 48 | Sanction Screening and RPR | Case Status Monitoring reports | New | Sanction Screening BRD | - | **MISSING** | future screening module |
| 147 | 48 | Sanction Screening and RPR | Audit Reports - Sanction Screening and Risk Profiling | New | Sanction Screening BRD | - | **MISSING** | future screening module |
| 148 | 48 | Sanction Screening and RPR | Records of unsuccessfully ingested to System | New | Sanction Screening BRD | - | **MISSING** | future screening module |
| 149 | 49 | Sanction Screening and RPR | STR cases (in the defined AMLC Reporting format) | New | Sanction Screening BRD | - | **MISSING** | future screening module (AMLC STR format) |
| 150 | 49 | Sanction Screening and RPR | Name matching fall out reports | New | Sanction Screening BRD | - | **MISSING** | future screening module |
| 151 | 49 | Submitted Policies | Submitted Masterlist Report / Extract | New | BRD-12 BRIDSP-04/28 | `SBM-MASTERLIST` | **DESIGNED** | submitted |
| 152 | 50 | Submitted Policies | Document Processing Fallout Report | New | BRD-12 BRIDSP-10 | `SBM-DOC-FALLOUT` | **DESIGNED** | submitted |
| 153 | 50 | Submitted Policies | Fallout Reports for the ff: 1. Sanitation, 2. Matching 3. Disposition, 4. Account classification | New | BRD-12 BRIDSP-10/20 | `SBM-PROCESS-FALLOUT` | **DESIGNED** | submitted |
| 154 | 50 | Submitted Policies | Non-renewal Report | New | BRD-12 BRIDSP-14 | `SBM-NON-RENEWAL` | **DESIGNED** | submitted |
| 155 | 50 | Submitted Policies | Migration Error Log | New | BRD-12 BRIDSP-33 | `SBM-MIGRATION-ERRORS` | **DESIGNED** | submitted |
| 156 | 50 | Submitted Policies | Sanitation Report | New | BRD-12 BRIDSP-20 | `SBM-SANITATION` | **DESIGNED** | submitted |
| 157 | 51 | Submitted Policies | Disposition Report | New | BRD-12 BRIDSP-20 | `SBM-DISPOSITION` | **DESIGNED** | submitted |
| 158 | 51 | Submitted Policies | Account Classification Report | New | BRD-12 BRIDSP-20 | `SBM-CLASSIFICATION` | **DESIGNED** | submitted |
| 159 | 51 | Submitted Policies | Renewable Accounts / Policies Report | New | BRD-12 BRIDSP-25 | `SBM-RENEWABLE` | **DESIGNED** | submitted |
| 160 | 51 | Submitted Policies | IAAF Tracking / Review Report | New | BRD-12 BRIDSP-05-07 | `SBM-IAAF` | **DESIGNED** | submitted |
| 161 | 51 | Submitted Policies | TOR Status & Approval Report | New | BRD-12 BRIDSP-17-19 | `SBM-TOR` | **DESIGNED** | submitted |
| 162 | 51 | Submitted Policies | Handling Fee Payment Classification Report | New | BRD-12 BRIDSP-31 | `SBM-HANDLING-FEE` | **DESIGNED** | submitted |
| 163 | 52 | Submitted Policies | Submitted Policies Conversion Report | New | BRD-12 BRIDSP-20 | `SBM-CONVERSION` | **DESIGNED** | submitted |
| 164 | 52 | Submitted Policies | Submitted Policies No Touch Report | New | BRD-12 BRIDSP-14 (OQ39) | `SBM-NO-TOUCH` | **DESIGNED** | submitted |
| 165 | 53 | Disbursement | CPC2 Incentive Report | New | DIS (CPC2) | `DSB-CPC2-INCENTIVE` | **DESIGNED** | remittance (category Disbursement) |
| 166 | 53 | Renewal/New Business | List of Accounts without Policy number | Existing | BRNB.073/115 | issuance workbench tab 'Placed - awaiting policy' | **BUILT (partial)** | nbreport: `NB-NO-POLICY` with submission age in working days (holiday calendar) |
| 167 | 54 | Renewal/New Business | Insurer's scorecard | Existing | none | - | **MISSING** | nbreport: `NB-INSURER-SCORECARD` (policies received vs placed per insurer, branch, segment; TAT) |
| 168 | 54 | Renewal/New Business | Policy Transmittal | Existing | BRNB.078 | `NB-DISPATCH` | **DROPPED** | remark: DELETE, same as the Policy Transmittal List (#72) |
| 169 | 54 | N/A | Sustainability Report- Risk Management | Existing | report list #169 | `FRBS-SUSTAINABILITY-PROD` | **BUILT** | frbs (A1-FRBS): yearly production per client type, region and line of business |
| 170 | 55 | Employee Benefits | Production Report | Existing | Employee Benefits BRD | - | **MISSING** | future employee-benefits module |
| 171 | 55 | Employee Benefits | Renewal Report | New | Employee Benefits BRD | - | **MISSING** | future employee-benefits module |
| 172 | 55 | Employee Benefits | Placement report | New | Employee Benefits BRD | - | **MISSING** | future employee-benefits module |
| 173 | 55 | Employee Benefits | New Business Report | New | Employee Benefits BRD | - | **MISSING** | future employee-benefits module |
| 174 | 55 | Customer Service Facility | Cases Due for Resolution | New | Customer Servicing Facility BRD | - | **MISSING** | future `csf` |
| 175 | 55 | Employee Benefits | Ad-hoc Report (i.e Booking, Persistency, Unpaid, Outstanding balance ) | New | Employee Benefits BRD | - | **MISSING** | future employee-benefits module (ad-hoc report) |
| 176 | 55 | Employee Benefits | Claims Report | New | Employee Benefits BRD | - | **MISSING** | future employee-benefits module |
| 177 | 56 | New Business/Renewal | Unapplied payment report with disposition | Existing | BRCLXN.034-036; BRD-12 BRIDSP-31 | `CLX-UNAPPLIED-LIST`, `CLX-INVOICES-WITH-DISPOSITION` | **DESIGNED** | collections; handling-fee tag from BRD-12 |
| 178 | 56 | N/A | Sustainability Report- Claims | Existing | Claims BRD | - | **MISSING** | future broker-claims module |
| 179 | 56 | TSU | Comparative | Existing | BRPM.014 / PMADD03; BRNB non-package comparative | productmaint comparative outputs; nonpackage comparative table | **BUILT (partial)** | productmaint / nonpackage: documents, not a catalogued report |
| 180 | 57 | TSU | Placement Update | Existing | BRNB.011 | `NB-PLC-UPDATE` | **BUILT** | nbreport |
| 181 | 57 | TSU | Pending Placement | Existing | BRNB.075/115 | `NB-ACC-STATUS` / placement workbench | **BUILT (partial)** | nbreport: `NB-PENDING-PLACEMENT` variant |
| 182 | 57 | TSU | Late Renewal | Existing | BRNB.018 (Q09); BRPM.017 | `PM-PKG-EXPIRY` | **BUILT (partial)** | productmaint for packages; late renewal of accounts waits for the Renewal BRD |
| 183 | 58 | TSU | Marine Hull | Existing | BRPM.017/018 | `PM-PKG-STATUS` variant (line Marine Hull) | **BUILT (partial)** | productmaint; saved variant |
| 184 | 58 | TSU | Package Masterlist | New | BRPM.017 | `PM-PKG-EXPIRY` | **BUILT** | productmaint |
| 185 | 58 | New / Renewal | Policy Delivery Report | Existing | BRNB.078 | `NB-DISPATCH` | **BUILT (partial)** | nbreport: TAT adherence (3 / 5 / 10 / 15 working days) missing |

## 5. Observations on the Report List

- The list is dated 27-Apr-2026 and mixes delivered BRDs with BRDs not in the pack (Reinsurance) and rows with no BRD ("N/A": two sustainability reports).
- Rows #73 and #168 carry the remark "DELETE - same as Policy Transmittal List / Report on successfully and unsuccessfully sent e-Policies"; #69 "BU confirmed that they do not need this report anymore". They are kept above as DROPPED.
- #104 (Production Reconciliation Summary) is filed under Operations - Remittance and #105 (Summary per Disposition - Remittance) describes unapplied payments, a Cashiering subject.
- #62 and #71 are tagged "New Business/Renewal/FRBS"; #138 "New Business / Submitted Policies"; #135 "Renewal/Submitted Policy": these reports span two BRDs and are owned as noted.
- Many rows carry the standard remark "Assumption: the result is not native to the solution ... can be considered as PDE" and "Solution is still unknown"; they were written before the BIBS design. They do not change the mapping.
- Several rows have no fields or say "for editing" (#63 "refer to the template", #74, #108 "to be added", #134, #152-#162); layouts are to be confirmed with each SME (see BDOI_SP_BRD_SPEC SQ25 for the Submitted Policies rows).
- The TSU rows (#179-#184) ask for "a separate TSU module" for individual non-package placement; BIBS covers TSU work inside `nonpackage` and `productmaint`. This is a scoping question for BDOI.
- #131 / #132 refer to a "TPC System" trade feed that no analysed BRD describes.
