# BDOI Accounting, Disbursement and ACSL (BRD-5) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse (BIBS).

Status: **for BDOI concurrence.** Build design: [`ACCOUNTING_DISBURSEMENT_DESIGN.md`](../architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md). The review workbook is consolidated later with the other BRDs. File references in the "Current" column are relative to `backend/src/main/java/com/iortatechnxt/brokerverse/` unless they start with `frontend/` or `docs/`.

## 1. Source documents

Source: `docs/source-documents/Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger BRD.zip`, one PDF
of 267 pages (29 MB). Every page was read: the digital pages as text, the scanned pages as images (OCR plus visual check,
the landscape tables of the Workshop Addendum rotated first).

| PDF pages | Document | Content |
|---|---|---|
| 1-15 (scanned, landscape tables) | **Addendum 2 "Financial Reporting, Disbursement and ACSL - Addendum (Workshop)"**, v1.0 10-Apr-2026, workshop 23-27 Mar 2026, signed 8-15 Apr 2026 | Envisioned processes (FRBS, Disbursement, ACSL); **new** requirements FRBS 3.1.1, 3.1.2; DIS 3.29.0, 3.29.1, 3.29.2, 3.30.0, 3.30.1, 3.30.1 (second, read as 3.30.2); ACSL 2.9.1, 2.9.2; **updated** DIS 2.2.8 |
| 16-27 (scanned) | Signed copy of Addendum 1 (same content as pp.28-39) | Signatures dated 13-Jan-2026 |
| 28-39 | **Addendum 1 "Accounting, Disbursement and ACSL - Addendum"**, v1.0 18-Dec-2025 | Clarifies DIS 3.27.0, DIS 2.2.8, DIS 2.17.4 (was 2.17.3), DIS 2.24.2 (was 2.24.1), ACSL 2.4.0, ACSL 2.5.5 (was 2.5.4), ACSL 2.16.0, MKT 2.25.0, FRBS 2.6.0; NFR update (availability, retention) |
| 40-49 | Main BRD v1.0 23-Jul-2025: executive summary, objectives, current and envisioned processes (with flow diagrams), key capabilities | Accounting (FRBS), Disbursement, Marketing refund / cash advance, ACSL |
| 50-67 | Accounting (FRBS) requirements and role matrix | BRD 1.1.x, FRBS 2.2.0-3.6.0 |
| 67-103 | Disbursement requirements and role matrix | BRD 1.1.x, DIS 2.2.0-3.28.4 |
| 104-113 | Marketing refund and cash-advance request requirements and role matrix | MKT 1.1.0-2.26.0 |
| 114-126 | ACSL requirements and role matrix | ACSL 1.1.0-2.16.0 |
| 127-132 | Business and system administration | BASAU 1.1.0-2.6.3 |
| 133-139 | Usage requirements: users, volumes, response times, peaks, availability, retention | NFR |
| 140-141 | Approval sheet (signed 24-Jul-2025 and later) | |
| 142-144 | **Appendix A** - Comptrollership FRBS list of reports (142 reports in 7 groups) | Section 5 |
| 145-148 | **Appendix B** - Disbursement reports (real-time, payee, fall-out, DCTF layout, end-of-day) | Section 5 |
| 149-151 | **Appendix C** - ACSL reports (schedules, aging, list of booked accounts, SOA reconciliation) | Section 5 |
| 152-153 (images) | **Appendix D** - Request for Payment form (RFP), Refund Request Form (RRF), Cash Advance Liquidation Form | Section 6 |
| 154-267 (scanned) | Signed scanned copy of the main BRD and its appendices (same content as pp.40-153; approval dated Sep-Oct 2025) | |

## 2. Business summary

BDOI is a broker. Its money is of two kinds: **trust money** (premium collected for insurers, remitted net of commission)
and **its own income** (commission, service fee, profit share, incentives). BRD-2 Operations covers the front of the
cycle (receipts, application, remittance batches, production reconciliation, adjustments, commission receivables). This
BRD covers the back office of Comptrollership:

- **Accounting (FRBS, the GL team)**: sets up and uploads the chart of accounts, inputs the monthly revaluation rate,
  posts and reviews manual entries (with automatic reversal of accruals), revalues USD balances, reconciles cash in bank
  against an uploaded bank file (check number + amount), closes the broking books automatically at month end and the GL
  books manually (Addendum 1: on a preferred date), closes the year by 15 April, monitors the service fee (referrers'
  share, released / liquidated), and produces 138 reports (Appendix A): end-of-day statements, BDO Unibank (GARD) bank-format
  statements and schedules, subsidiaries accounting, schedules and aging, Mancom, service fee, and government returns (BIR,
  HDMF, SSS, PhilHealth, IC). Addendum 2 adds cost-centre rules and early incentives as Other Income.
- **Disbursement**: maintains payees (all payee classes and seven modes of payment), receives system-triggered, uploaded
  and e-mailed requests, creates the disbursement voucher (DV) and an editable proforma entry, processes Credit to Account,
  ATD, MC / DD, Credit Ticket, TT, Online Banking and Check, tags instrument statuses (manually, automatically and by bank
  files), runs end of day (DCTF file, check printing, forms, e-mail confirmations), funds the main account through BDO
  Business Online Banking with a verifier and two approvers, tags OR / AR and CWT, generates BIR 2307, and regularises the
  accounting of every approval, cancellation, stale and negotiated check. Addendum 2 adds CPC2 and early-incentive
  handling (service invoice with 2% withholding) and cost-centre allocation with an employee / cost-centre master.
- **Marketing refund and cash-advance requests**: AOs raise Refund Request Forms (per AR number) and employees raise
  cash-advance RFPs; ACSL and Cashiering validate refunds of cancelled policies; Marketing review / approval (plus HR for
  cash advances) routes them automatically to Disbursement; CA/SA details are written to the client record.
- **ACSL**: generates input files, uploads insurer statements of account and reconciles them per invoice, reconciles GL
  against SL per GL code, investigates accounts (analysis requests such as refunds of cancelled accounts), applies AR
  refunds and reverses SL payments, creates correction entries through assign / create / review / approve / post, links
  all transactions of an invoice (booking, adjustment, cancellation) under one invoice number, produces aging and schedule
  reports per insurer and currency. Addendum 2 adds controlled correction of wrong GL postings and remittance deductions
  upon insurer confirmation.
- **Business and system administration**: LOV maintenance and user management with approval.

Most of the accounting core already exists in BrokerVerse (chart, journals, engine, sub-ledger, periods, FX revaluation,
year-end, bank reconciliation, payables masters, BIR returns and 2307). The **big new build is Disbursement**, followed by
**ACSL reconciliation and correction**, the **Marketing request flow** and the **BDOI report pack**.


## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 83 |
| CONFIGURE | Set-up only | 12 |
| CHANGE | Extends an existing capability | 75 |
| NEW | New build | 106 |
| OUT | Out of scope per the BRD | 1 |
| **Total** | | **277** |

### Rows per section and fit

| Section | Name | Rows | FIT | CONFIGURE | CHANGE | NEW | OUT | Effort S/M/L/XL |
|---|---|---|---|---|---|---|---|---|
| A | Access and session (all five units) | 20 | 10 | 10 | 0 | 0 | 0 | 20/0/0/0 |
| B | Accounting (FRBS): revaluation rate and chart of accounts | 8 | 5 | 0 | 3 | 0 | 0 | 7/1/0/0 |
| C | Accounting (FRBS): reports on demand | 12 | 7 | 0 | 5 | 0 | 0 | 11/0/0/0 |
| D | Accounting (FRBS): posting, manual entries and validation | 20 | 12 | 0 | 8 | 0 | 0 | 19/1/0/0 |
| E | Accounting (FRBS): month-end, year-end, broking books, revaluation | 7 | 1 | 1 | 5 | 0 | 0 | 6/1/0/0 |
| F | Accounting (FRBS): bank reconciliation, service fee, cost centres, incentive income | 9 | 2 | 1 | 3 | 3 | 0 | 7/2/0/0 |
| G | Disbursement: payee maintenance | 9 | 0 | 0 | 1 | 8 | 0 | 7/2/0/0 |
| H | Disbursement: reports | 17 | 8 | 0 | 2 | 7 | 0 | 14/3/0/0 |
| I | Disbursement: request intake | 13 | 2 | 0 | 3 | 8 | 0 | 10/3/0/0 |
| J | Disbursement: processing, vouchers, modes and cost centres | 15 | 0 | 0 | 4 | 11 | 0 | 9/5/1/0 |
| K | Disbursement: status, OR / AR and CWT tagging | 23 | 1 | 0 | 0 | 22 | 0 | 20/3/0/0 |
| L | Disbursement: review, approval, cancellation, end of day, account funding | 19 | 0 | 0 | 3 | 15 | 1 | 14/4/1/0 |
| M | Disbursement: masters, accounting regularisation, incentives | 11 | 4 | 0 | 5 | 2 | 0 | 6/5/0/0 |
| N | Marketing refund and cash-advance requests | 32 | 5 | 0 | 6 | 21 | 0 | 29/3/0/0 |
| O | ACSL: input files, reconciliation and reports | 18 | 3 | 0 | 8 | 7 | 0 | 11/7/0/0 |
| P | ACSL: investigation, AR refund application, correction entries, remittance deduction | 23 | 7 | 0 | 14 | 2 | 0 | 18/5/0/0 |
| Q | Business and system administration | 21 | 16 | 0 | 5 | 0 | 0 | 21/0/0/0 |

### Rows per ID family

| Family | Area | Rows | FIT | CONFIGURE | CHANGE | NEW | OUT |
|---|---|---|---|---|---|---|---|
| FRBS | Accounting (Financial Reporting and Budget Section, GL) | 60 | 29 | 4 | 24 | 3 | 0 |
| DIS | Disbursement | 111 | 17 | 2 | 18 | 73 | 1 |
| MKT | Marketing refund and cash-advance requests | 36 | 7 | 2 | 6 | 21 | 0 |
| ACSL | Accounting Controls and Subsidiary Ledger | 45 | 12 | 2 | 22 | 9 | 0 |
| BASAU | Business and system administration | 25 | 18 | 2 | 5 | 0 | 0 |
| **Total** | | **277** | 83 | 12 | 75 | 106 | 1 |

Rows from the addenda: Addendum 1 (Dec-2025) updates DIS 2.2.8, DIS 2.17.4, DIS 2.24.2, DIS 3.27.0, ACSL 2.4.0, ACSL 2.5.5, ACSL 2.16.0, MKT 2.25.0 and FRBS 2.6.0 (recorded on the updated row). Addendum 2 (Workshop, Apr-2026) adds FRBS 3.1.1, FRBS 3.1.2, DIS 3.29.0-3.29.2, DIS 3.30.0-3.30.2, ACSL 2.9.1 and ACSL 2.9.2, and updates DIS 2.2.8.

### Target modules

| Module | Rows | Note |
|---|---|---|
| `disbursement` | 84 | New module; implements the Operations `DisbursementGateway`. Built in wave A1-DSB (design section 17, "A1-DSB as built"; parked content AQ09-AQ17) |
| `platform (report)` | 30 | Extension of a built module |
| `acsl` | 26 | New module; SOA and GL-SL reconciliation, investigation cases, correction entries, ACSL reports |
| `payrequest` | 25 | New module; Marketing refund and cash-advance requests |
| `platform (security)` | 22 | Extension of a built module |
| `platform (journal)` | 16 | Extension of a built module |
| `platform (nbadmin)` | 9 | Extension of a built module |
| `platform (coa)` | 6 | Extension of a built module |
| `platform (closing)` | 6 | Extension of a built module |
| `platform (payables)` | 6 | Extension of a built module |
| `platform (lov)` | 6 | Extension of a built module |
| `platform (receivables)` | 4 | Extension of a built module |
| `remittance` | 4 | Extension of a built module. CPC2, early-incentive SI, deductions and the cancelled-DV restore built in wave A1-OPSX (design section 17, "A1-OPSX as built"); the CPC2 report (DIS 3.29.0) is not built yet |
| `platform (approval)` | 4 | Extension of a built module |
| `frbs` | 3 | New module; BDOI report pack and service fee |
| `cashiering` | 3 | Extension of a built module |
| `opsledger` | 3 | Extension of a built module |
| `platform (frontend)` | 2 | Extension of a built module |
| `platform (accounting)` | 2 | Extension of a built module |
| `platform (organization)` | 2 | Extension of a built module |
| `booking, opsledger` | 2 | Extension of a built module |
| `crm` | 2 | Extension of a built module |
| `platform (currency)` | 1 | Extension of a built module |
| `platform (coa, currency)` | 1 | Extension of a built module |
| `80 schedules, broker reports in the new `frbs` module, new BIR books and forms in `tax`` | 1 | Extension of a built module |
| `platform (journal, coa)` | 1 | Extension of a built module |
| `platform (journal, approval)` | 1 | Extension of a built module |
| `platform (period, closing)` | 1 | Extension of a built module |
| `remittance (rules)` | 1 | Extension of a built module |
| `platform (bulk)` | 1 | Extension of a built module |
| `platform (tax)` | 1 | Extension of a built module |
| `platform (attachment)` | 1 | Extension of a built module |

### Big-ticket items

1. **Disbursement module** (DIS 2.2-3.28, about 110 rows, L/XL overall): payee master, request intake (gateway, upload,
   encoding), DV with editable proforma entry, seven payment modes with instrument life cycles, automatic and file-based
   status tagging, end-of-day outputs (DCTF, checks, ATD, CT / TT, MC / DD), account funding with dual approval, OR / AR and
   CWT tagging, approval posting and full regularisation on cancellation, stale / negotiated check entries, 14 reports. It
   replaces the in-app Disbursement queue of Operations (OQ02) behind the same `DisbursementGateway` port.
2. **ACSL reconciliation and correction** (ACSL 2.4-2.16, 2.9.1-2.9.2): insurer SOA upload and per-invoice reconciliation,
   GL-SL reconciliation, investigation cases, correction entries that post to sub-ledger control accounts and to the
   Operations invoice ledger, aging and schedule reports with Appendix C buckets, remittance deduction on insurer
   confirmation.
3. **BDOI report pack** (FRBS 3.2.0, Appendices A-C, about 170 outputs): mostly configuration of existing statements plus
   a configurable account-schedule engine; new BIR books of accounts and forms; Mancom and service-fee reports.
4. **Marketing refund and cash-advance requests** (MKT, 42 rows): RRF / RFP forms, validation by ACSL and Cashiering,
   Marketing / HR approvals, automatic routing to Disbursement, CA/SA on the client record.
5. **One invoice family** (DIS 3.27.2, ACSL 2.5.3, ACSL 2.16.0): a root invoice number carried by booking, the invoice
   ledger and every downstream document.
6. **Incentive accounting** (DIS 3.29.1, 3.29.2, FRBS 3.1.2): CPC2 per remittance and early incentive with an automatic
   service invoice and 2% withholding, both as separate income.
7. **GL platform changes** (FRBS): chart upload and numbering, short-code entry, accrual auto-reversal, negative-balance
   policy, bulk posting, scheduled GL close, broking-books cut-off, bank-file XLSX import and check-number matching,
   cost-centre derivation rules.


## 4. Requirements and fit/gap

Page references are PDF pages of the source file. "Add.1" is the Addendum of Dec-2025 (pp.28-39), "Add.2" the Workshop Addendum of Apr-2026 (pp.1-15). Question references: `AQ##` are new (section 9); `OQ##` Operations, `Q##` BRD-1, `PQ##` BRD-3.

### A. Access and session (all five units)

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| FRBS 1.1.0<br><sub>p.50</sub> | All (Accounting) | Access the module using any BDO-issued device (printed "BRD 1.1.0" in the Accounting table) | Module reachable from any BDO device | **FIT** | Web SPA, responsive layout (`frontend/src/components/layout`) | Re-use; BDO SSO / AD stays parked (BRD-1 Q42) | `platform (security)` | S |  |
| FRBS 1.1.1<br><sub>p.50</sub> | All (Accounting) | Log in with a specific user profile | Login with own profile | **FIT** | JWT login with roles (`security/service`) | New FRBS roles (design section 6) | `platform (security)` | S | AQ28 |
| FRBS 1.1.2<br><sub>p.50</sub> | All (Accounting) | Inactivity warning after 15 minutes | Warning shown after 15 min idle | **CONFIGURE** | Idle timeout and warning dialog delivered for BRNB.040 (`frontend/src/components/layout/HeaderTools.tsx`) | Same parameters as BRD-1 | `platform (security)` | S |  |
| FRBS 1.1.3<br><sub>p.50</sub> | All (Accounting) | Warning 30 minutes before system-triggered logout | Warning 30 min before forced logout | **CONFIGURE** | Absolute session-end warning delivered for BRNB.040 | Same parameters as BRD-1 | `platform (security)` | S |  |
| DIS 1.1.0<br><sub>p.68</sub> | All (Disbursement) | Access the module using any BDO-issued device (printed "BRD 1.1.0") | As FRBS 1.1.0 | **FIT** | As FRBS 1.1.0 | Re-use | `platform (security)` | S |  |
| DIS 1.1.1<br><sub>p.69</sub> | All (Disbursement) | Log in with a specific user profile | As FRBS 1.1.1 | **FIT** | As FRBS 1.1.1 | Disbursement roles (design section 6) | `platform (security)` | S | AQ28 |
| DIS 1.1.2<br><sub>p.69</sub> | All (Disbursement) | Inactivity warning after 15 minutes | As FRBS 1.1.2 | **CONFIGURE** | As FRBS 1.1.2 | As BRD-1 | `platform (security)` | S |  |
| DIS 1.1.3<br><sub>p.69</sub> | All (Disbursement) | Warning 30 minutes before system-triggered logout | As FRBS 1.1.3 | **CONFIGURE** | As FRBS 1.1.3 | As BRD-1 | `platform (security)` | S |  |
| MKT 1.1.0<br><sub>p.105</sub> | All (Marketing refund / CA) | Access the module using any BDO-issued device | As FRBS 1.1.0 | **FIT** | As FRBS 1.1.0 | Re-use | `platform (security)` | S |  |
| MKT 1.1.1<br><sub>p.105</sub> | All (Marketing refund / CA) | Log in with a specific user profile | As FRBS 1.1.1 | **FIT** | As FRBS 1.1.1 | Marketing roles exist (BRD-1); HR approver role added | `platform (security)` | S | AQ28 |
| MKT 1.1.2<br><sub>p.105</sub> | All (Marketing refund / CA) | Inactivity warning after 15 minutes | As FRBS 1.1.2 | **CONFIGURE** | As FRBS 1.1.2 | As BRD-1 | `platform (security)` | S |  |
| MKT 1.1.3<br><sub>p.105</sub> | All (Marketing refund / CA) | Warning 30 minutes before system-triggered logout | As FRBS 1.1.3 | **CONFIGURE** | As FRBS 1.1.3 | As BRD-1 | `platform (security)` | S |  |
| ACSL 1.1.0<br><sub>p.114</sub> | All (ACSL) | Access the module using any BDO-issued device | As FRBS 1.1.0 | **FIT** | As FRBS 1.1.0 | Re-use | `platform (security)` | S |  |
| ACSL 1.1.1<br><sub>p.115</sub> | All (ACSL) | Log in with a specific user profile | As FRBS 1.1.1 | **FIT** | As FRBS 1.1.1 | ACSL roles (design section 6) | `platform (security)` | S | AQ28 |
| ACSL 1.1.2<br><sub>p.115</sub> | All (ACSL) | Inactivity warning after 15 minutes | As FRBS 1.1.2 | **CONFIGURE** | As FRBS 1.1.2 | As BRD-1 | `platform (security)` | S |  |
| ACSL 1.1.3<br><sub>p.115</sub> | All (ACSL) | Warning 30 minutes before system-triggered logout | As FRBS 1.1.3 | **CONFIGURE** | As FRBS 1.1.3 | As BRD-1 | `platform (security)` | S |  |
| BASAU 1.1.0<br><sub>p.127</sub> | All (Business / System Admin) | Access the module using any BDO-issued device | As FRBS 1.1.0 | **FIT** | As FRBS 1.1.0 | Re-use | `platform (security)` | S |  |
| BASAU 1.1.1<br><sub>p.127</sub> | All (Business / System Admin) | Log in with a specific user profile | As FRBS 1.1.1 | **FIT** | As FRBS 1.1.1 | Re-use | `platform (security)` | S |  |
| BASAU 1.1.2<br><sub>p.128</sub> | All (Business / System Admin) | Inactivity warning after 15 minutes | As FRBS 1.1.2 | **CONFIGURE** | As FRBS 1.1.2 | As BRD-1 | `platform (security)` | S |  |
| BASAU 1.1.3<br><sub>p.128</sub> | All (Business / System Admin) | Warning 30 minutes before system-triggered logout | As FRBS 1.1.3 | **CONFIGURE** | As FRBS 1.1.3 | As BRD-1 | `platform (security)` | S |  |

### B. Accounting (FRBS): revaluation rate and chart of accounts

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| FRBS 2.2.0<br><sub>p.51</sub> | FRBS User (GL TL / TH) | Input the revaluation rate monthly; the rate is maintained in the system | Rate entered monthly and kept | **FIT** (built A1-GL, design §17) | Exchange-rate master with rate types SPOT / CLOSING / AVERAGE / BUDGET / BOOK, maker-checker, effective dates (`currency/domain/RateType.java`, `currency/service/CurrencyService.java`, screen *Currency Rates*) | The monthly revaluation rate is a CLOSING rate at month end. Proposal for OQ08: the same rate becomes the BOOK rate of the next month (parameter `OPS_BOOK_RATE_SOURCE=CLOSING_PREV_MONTH`, a copy job in `currency`) | `platform (currency)` | S | AQ03, OQ08 |
| FRBS 2.3.0<br><sub>p.51</sub> | FRBS User | Set up the chart of accounts | Chart can be set up | **FIT** | Chart master with levels GROUP / MAIN / SUB, classes, categories, control accounts, sub-ledger type, currencies, branch / role restrictions, freeze / close, maker-checker (`coa/domain/GlAccount.java`, `coa/service/ChartOfAccountsService.java`) | BDOI's own broker chart is loaded at go-live (see design section 3 on the demo chart) | `platform (coa)` | S | AQ01 |
| FRBS 2.3.1<br><sub>p.51</sub> | FRBS User | Upload a file with the parent and child account details (chart of accounts) | File upload creates parent and child accounts | **CHANGE** (built A1-GL, design §17) | No chart upload; bulk framework reads XLSX / CSV / ODS with per-row outcomes (`bulk/service/BulkFileReader.java`) | Bulk handler `COA_ACCOUNTS` (parent code, name, short code, class, level, category, postable, control, sub-ledger type, currency, flags); rows created PENDING_AUTHORIZATION, parents before children; error report | `platform (coa)` | M | AQ01 |
| FRBS 2.3.2<br><sub>p.52</sub> | FRBS User | View the system-generated unique account number of each parent or child account; child linked to its parent; child transactions roll up to the parent | Unique number generated; child linked; roll-up | **CHANGE** (built A1-GL, design §17) | Code is keyed by the user (unique per company); parent link and roll-up exist (`coa/domain/GlAccount.java` parent, `finreport/service/AccountHierarchy.java`) | Optional numbering scheme per parent (`coa_numbering`: parent code + child sequence and width); code proposed by the system when blank; roll-up unchanged | `platform (coa)` | S | AQ01 |
| FRBS 2.3.3<br><sub>p.52</sub> | FRBS User | Search the chart by name, short code, account number | Search by the three keys | **CHANGE** (built A1-GL, design §17) | Search by code prefix or name (`coa/api/ChartOfAccountsController.java` `q`) | Add the short code (`shortName`, made unique) to the search and to journal-line entry | `platform (coa)` | S |  |
| FRBS 2.3.4<br><sub>p.53</sub> | FRBS User | View details of an account | Account details shown | **FIT** | Account page (`/gl/accounts`) | Re-use | `platform (coa)` | S |  |
| FRBS 2.3.5<br><sub>p.53</sub> | FRBS User | Edit account details | Account edited | **FIT** | Edit with maker-checker and audit | Re-use | `platform (coa)` | S |  |
| FRBS 3.6.0<br><sub>p.65</sub> | System | Maintain values: revaluation rate and chart of accounts | Both kept in the system | **FIT** | Currency rates and chart masters | Re-use | `platform (coa, currency)` | S |  |

### C. Accounting (FRBS): reports on demand

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| FRBS 2.4.0<br><sub>p.53</sub> | FRBS User | Access system-generated reports anytime (user triggered) | Reports run on demand, no IT | **FIT** | Report framework: catalogue, parameter form, on-screen run, PDF / XLSX / CSV / ODS export, generated-report archive (`report/core/ReportService.java`, `report/core/ReportArchiveService.java`) | Re-use; the BDOI report pack is section R | `platform (report)` | S |  |
| FRBS 2.4.1<br><sub>p.53</sub> | FRBS User | Select a report | Report can be selected | **FIT** | Report catalogue by category | Category "Accounting (FRBS)" | `platform (report)` | S |  |
| FRBS 2.4.2<br><sub>p.53</sub> | FRBS User | Select a date range | Date range selectable | **FIT** | `ParameterSpec` date parameters with defaults | Re-use | `platform (report)` | S |  |
| FRBS 2.4.3<br><sub>p.54</sub> | FRBS User | View the details of the selected report | Details viewable | **FIT** | On-screen result table | Re-use | `platform (report)` | S |  |
| FRBS 2.4.4<br><sub>p.54</sub> | FRBS User | Filter each column of the report | Every column filterable | **CHANGE** (built A1-GL, design §17) | Report viewer shows the result; filters are report parameters, not column filters | Column filter row in the report viewer (client side on the loaded page, server side for exports) | `platform (report)` | S |  |
| FRBS 2.4.5<br><sub>p.54</sub> | FRBS User | Download single or multiple reports | One or several reports downloaded | **CHANGE** (built A1-GL, design §17) | One report per export | "Report batch": select several reports with shared parameters, run asynchronously, download a ZIP; archived in `report_run` | `platform (report)` | S |  |
| FRBS 2.4.6<br><sub>p.54</sub> | FRBS User | Download in .xlsx or .ods | Both formats | **FIT** | `report/render/ExportFormat.java` (XLSX, ODS) | Re-use | `platform (report)` | S |  |
| FRBS 2.4.7<br><sub>p.54</sub> | FRBS User | Print single or multiple reports in .pdf | PDF of one or several reports | **CHANGE** (built A1-GL, design §17) | PDF per report | Report batch (FRBS 2.4.5) with a merged PDF option | `platform (report)` | S |  |
| FRBS 2.4.8<br><sub>p.55</sub> | FRBS User | Preview the report | Preview before printing | **FIT** | On-screen run and PDF preview | Re-use | `platform (report)` | S |  |
| FRBS 2.4.9<br><sub>p.55</sub> | FRBS User | Set printing options | Print options settable | **CHANGE** (built A1-GL, design §17) | Fixed PDF layout | PDF options on export: paper size, orientation, fit-to-width, header / footer on every page | `platform (report)` | S |  |
| FRBS 2.4.10<br><sub>p.55</sub> | FRBS User | Copy report data into another application | Copy / paste into Excel, Word, etc. | **FIT** | Selectable HTML table; CSV / XLSX export | Re-use | `platform (report)` | S |  |
| FRBS 3.2.0<br><sub>p.63</sub> | System | Generate the report families: end-of-day, GARD, subsidiaries accounting, schedules and aging, performance / Mancom, service fee, government returns and remittances, real-time reports (Appendix A, 138 reports) | All listed reports generated | **CHANGE** (built A1-FRBS, design §17) | Statements (MIS BS / IE with configurable formats), trial balances, GL / SL, day books, cash / bank position, budget vs actual, BIR returns (2550Q, 0619-E, 1601-EQ, SLS / SLP / QAP, 2307), IC schedules (`finreport/**`, `tax/**`, `budget/report`) | Section R: configure existing reports, a configurable **account schedule** report engine in `finreport` for the | `80 schedules, broker reports in the new `frbs` module, new BIR books and forms in `tax`` | frbs | XL |

### D. Accounting (FRBS): posting, manual entries and validation

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| FRBS 2.5.0<br><sub>p.55</sub> | FRBS User (TL / TH) | Post transactions to the GL and the sub-ledger | Posting to GL and SL | **FIT** | Posting engine (`journal/service/PostingService.java`), open-item sub-ledger (`subledger/service/OpenItemService.java`) | Re-use | `platform (journal)` | S |  |
| FRBS 2.5.1<br><sub>p.56</sub> | FRBS User | View the list of manual entries for posting assigned to me | List of assigned entries | **CHANGE** (built A1-GL, design §17) | Approval inbox lists pending journals the checker may approve (`journal/service/JournalApprovalSource.java`); no assignee | `assigned_to` on `jnl_batch` (TL assigns / re-assigns, FRBS / ACSL); inbox filter "assigned to me" | `platform (journal)` | S |  |
| FRBS 2.5.2<br><sub>p.56</sub> | FRBS User | Select a manual entry | Entry selectable | **FIT** | Journal list and page | Re-use | `platform (journal)` | S |  |
| FRBS 2.5.3<br><sub>p.56</sub> | FRBS User | View the transaction details for posting | Details shown | **FIT** | Journal page with lines, dimensions, audit | Re-use | `platform (journal)` | S |  |
| FRBS 2.5.4<br><sub>p.56</sub> | FRBS User | Prevent posting transactions with errors (negative balance, debit not equal to credit, etc.) | Posting refused on errors | **CHANGE** (built A1-GL, design §17) | Balanced lines, value-date window, postable / frozen / manual-allowed / currency / branch checks enforced (`journal/service/JournalValidator.java`); no negative-balance check | Account flag `negative_balance_policy` (ALLOW / WARN / BLOCK; natural side from the account class); validator computes the balance after posting | `platform (journal, coa)` | S | AQ30 |
| FRBS 2.5.5<br><sub>p.57</sub> | FRBS User | Receive warning and information messages for data-entry errors | Messages shown | **FIT** | `JOURNAL_INVALID` lists every error; UI shows `ErrorAlert` | Warnings (non-blocking) added with FRBS 2.5.4 | `platform (journal)` | S |  |
| FRBS 2.5.6<br><sub>p.57</sub> | FRBS User | Post single or multiple transactions | Bulk posting | **CHANGE** (built A1-GL, design §17) | Approve one journal at a time (`journal/service/JournalAuthorizationService.java`) | Bulk approve from the inbox (each journal still validated and audited; partial success report) | `platform (journal, approval)` | S |  |
| FRBS 2.5.7<br><sub>p.57</sub> | FRBS User | Return a posting request | Request returned to maker | **FIT** (built A1-GL, design §17) | Reject with reason; REJECTED journals are editable and re-submittable (`journal/domain/JournalBatch.java`) | Label the action "Return" in the BDOI UI | `platform (journal)` | S |  |
| FRBS 2.5.8<br><sub>p.57</sub> | FRBS User | Add remarks | Remarks added | **FIT** | Reason on reject / reverse; narration | Re-use | `platform (journal)` | S |  |
| FRBS 2.5.9<br><sub>p.57</sub> | FRBS User | Save remarks | Remarks saved | **FIT** | As FRBS 2.5.8 | Re-use | `platform (journal)` | S |  |
| FRBS 2.5.10<br><sub>p.58</sub> | FRBS User | Confirmation of the transaction details before pushing to the next workflow | Confirmation prompt | **CHANGE** (built A1-GL, design §17) | Submit / approve act at once | Confirmation dialog with totals, line count and warnings (shared `ConfirmSubmit` component, reused by every BDOI workflow in this BRD) | `platform (frontend)` | S |  |
| FRBS 2.8.0<br><sub>p.59</sub> | FRBS User | Perform manual entries | Manual entries possible | **FIT** | MANUAL / ADJUSTMENT / ACCRUAL journals with maker-checker, upload and recurring templates (`journal/service/JournalEntryService.java`, `JournalUploadService.java`, `RecurringJournalService.java`) | Re-use | `platform (journal)` | S |  |
| FRBS 2.8.1<br><sub>p.60</sub> | FRBS User | Input description, short code (GL account auto-populated from the short code), debit, credit and a reversal date for accruals; the reversal posts automatically on that date in GL and SL | Fields captured; reversal auto-posted on the date | **CHANGE** (built A1-GL, design §17) | Lines by account code; manual reversal with a date (`JournalAuthorizationService.reverse`); auto-reverse only on recurring templates (first day of next period) | Short-code entry (FRBS 2.3.3); `reverse_on` on MANUAL / ACCRUAL journals; managed job `JOURNAL_AUTO_REVERSAL` (daily) posts the reversal as a system journal type REVERSAL linked by `reversal_of_id`, also reversing SL open items | `platform (journal)` | M |  |
| FRBS 2.8.2<br><sub>p.60</sub> | FRBS User | Save draft of manual entries | Draft saved | **FIT** | DRAFT status | Re-use | `platform (journal)` | S |  |
| FRBS 2.8.3<br><sub>p.60</sub> | FRBS User | Confirmation prompt before pushing to the next workflow | Prompt shown | **CHANGE** (built A1-GL, design §17) | As FRBS 2.5.10 | As FRBS 2.5.10 | `platform (frontend)` | S |  |
| FRBS 2.8.4<br><sub>p.61</sub> | FRBS User | Warning and information messages for data-entry errors (inappropriate negative balance, negative balance, unbalanced) | Messages shown | **CHANGE** (built A1-GL, design §17) | As FRBS 2.5.4 | As FRBS 2.5.4 | `platform (journal)` | S | AQ30 |
| FRBS 2.8.5<br><sub>p.61</sub> | FRBS User | Submit for review and posting | Submitted | **FIT** | Submit to PENDING_APPROVAL | Re-use | `platform (journal)` | S |  |
| FRBS 2.9.0<br><sub>p.61</sub> | FRBS User | Edit accounting entries | Entries editable | **FIT** | Draft / returned journals editable; posted journals corrected by reversal (audit rule) | Re-use; ACSL corrections section P | `platform (journal)` | S |  |
| FRBS 3.1.0<br><sub>p.62</sub> | System | Record every transaction with financial impact and post it to the ledger | All financial transactions posted | **FIT** | Accounting engine (`accounting/service/AccountingEngine.java`) with event log; every module publishes `BusinessEvent` | New events of this BRD (design section 5) | `platform (accounting)` | S | AQ02 |
| FRBS 3.6.0b<br><sub>p.66</sub> | System | Generate a warning and information message for data-entry errors (second row numbered 3.6.0, item "q") | Messages generated | **CHANGE** (built A1-GL, design §17) | As FRBS 2.5.4 | As FRBS 2.5.4 | `platform (journal)` | S | AQ30 |

### E. Accounting (FRBS): month-end, year-end, broking books, revaluation

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| FRBS 2.6.0<br><sub>p.58; Add.1 p.35</sub> | FRBS User | Perform the month-end closing of the previous month's GL books on the 2nd banking day of the current month. **Addendum 1:** manual closing on a preferred date and time in the current month | Close of the previous month on a chosen date / time | **CHANGE** (built A1-GL, design §17) | Period close with checklist (period status, pending journals, reconciliations, FX revaluation, TB) (`closing/service/ClosingChecklistService.java`, `period/service/PeriodService.java`) | Scheduled close: FRBS picks date and time; job `GL_PERIOD_CLOSE` runs the checklist and closes, or notifies failures; default proposal = 2nd banking day (holiday calendar `org_holiday`) | `platform (closing)` | S | AQ04 |
| FRBS 2.6.1<br><sub>p.58</sub> | FRBS User | Select the closing date; refuse closing if the date is not the previous month and year | Guard on the period | **CHANGE** (built A1-GL, design §17) | Any open period can be closed | Guard `CLOSE_ONLY_PREVIOUS_MONTH` (parameter, on for BDOI) | `platform (closing)` | S | AQ04 |
| FRBS 2.7.0<br><sub>p.59</sub> | FRBS User | Perform the year-end closing on or before April 15 of the current year | Year-end close | **FIT** (built A1-GL, design §17) | Year-end close zeroing income / expense to retained earnings, next year created (`closing/service/YearEndService.java`) | Alert `YEAR_END_CLOSE_DUE` (deadline parameter 15 April) | `platform (closing)` | S | AQ04 |
| FRBS 2.7.1<br><sub>p.59</sub> | FRBS User | Confirmation that all nominal accounts are zero and the GL book balance is 0 | Confirmation shown | **CHANGE** (built A1-GL, design §17) | Close posts the CLOSING journal; checks before close only | Post-close verification (nominal balances as of year end = 0, TB difference = 0) shown and stored on `YearEndClose` | `platform (closing)` | S |  |
| FRBS 3.4.0<br><sub>p.64</sub> | System | Automatically close the broking books | Broking books closed by the system | **CHANGE** (built A1-GL, design §17) | Period status governs all postings; no separate cut-off for the broking sub-ledger | Module cut-off `acc_period_module_lock` (module BROKING); booking, opsledger, cashiering, remittance, adjustment, disbursement call `PeriodService.requirePostingPeriod(date, module)`; GL stays open for FRBS adjustments until FRBS 2.6.0 | `platform (period, closing)` | M | AQ04 |
| FRBS 3.4.1<br><sub>p.65</sub> | System | Close the current month's (and year's) broking books at month end at an agreed time | Automatic close at the agreed time | **CHANGE** (built A1-GL, design §17) | No scheduled close | Job `BROKING_BOOKS_CLOSE` (cron parameter, month end); pending broking items listed (unposted DVs, unapplied batches) | `platform (closing)` | S | AQ04 |
| FRBS 3.5.0<br><sub>p.65</sub> | System | Revalue USD transactions with outstanding balances (receivables, cash, payables) and create entries for the difference of the previous date versus the current date | Revaluation entries created | **CONFIGURE** | FX revaluation per account / branch / currency at the CLOSING rate, idempotent, auto-reversal, register report `GL-FXREV`; open items revalued for information (`closing/service/FxRevaluationService.java`, `OpenItemRevaluation.java`) | Flag the USD receivable, payable and bank accounts `revaluationRequired`; enable auto-reversal; add the Operations invoice detail (per invoice / insurer) to the register. Daily revaluation only if BDOI confirms (AQ03) | `platform (closing)` | S | AQ03 |

### F. Accounting (FRBS): bank reconciliation, service fee, cost centres, incentive income

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| FRBS 3.3.0<br><sub>p.63</sub> | System | Perform automatic reconciliation | Auto reconciliation | **FIT** | Bank reconciliation auto-match, BRS, finalize (`receivables/service/BankMatchingService.java`, `AutoMatcher.java`) | Re-use | `platform (receivables)` | S |  |
| FRBS 3.3.1<br><sub>p.63</sub> | System | Accept an .xlsx upload of cash-in-bank transactions that triggers auto reconciliation | XLSX upload triggers matching | **CHANGE** (built A1-GL, design §17) | CSV statement import only (`receivables/service/BankStatementParser.java`) | XLSX / ODS statement import through `BulkFileReader` with a column mapping per bank account (`brs_statement_layout`); auto-match after import | `platform (receivables)` | S | AQ08 |
| FRBS 3.3.2<br><sub>p.64</sub> | System | Reconcile cash per books with cash per bank, matching on check number and amount | Matched on check no. + amount | **CHANGE** (built A1-GL, design §17) | One-to-one on amount within a date window, preferring a reference / cheque number in the text | Matching rule set per bank account: rule `CHECK_NO_AND_AMOUNT` first (exact), then the existing rules | `platform (receivables)` | S | AQ08 |
| FRBS 3.3.3<br><sub>p.64</sub> | System | Report of unmatched transactions | Unmatched report | **FIT** | `FIN-BRS-UNREC-BOOK`, `FIN-BRS-UNREC-BANK`, `FIN-BRS-STMT` | Re-use | `platform (receivables)` | S |  |
| FRBS 2.10.0<br><sub>p.61</sub> | FRBS User | Monitor the service fee | Service fee monitored | **NEW** (built A1-FRBS, design §17) | None (Appendix A VI: service fee = referrers' share, 2.5% / 1% of fully paid commission, net of taxes, p.144) | `frbs` service-fee runs: base = commission fully collected in the period (opsledger), rate by segment, taxes, lines per unit / referrer, approval, payout request through `DisbursementGateway` (type SERVICE_FEE), status RELEASED / LIQUIDATED | `frbs` | M | AQ20 |
| FRBS 2.10.1<br><sub>p.62</sub> | FRBS User | Receive the liquidation report from the units | Liquidation report received | **NEW** (built A1-FRBS, design §17) | None | Upload of the unit's liquidation report on the service-fee line (attachment + liquidation date) | `frbs` | S | AQ20 |
| FRBS 2.10.2<br><sub>p.62</sub> | FRBS User | Manually tag a transaction "liquidated" (with liquidation date) or "released" (with released date, after credit to the recipient) | Status and dates tagged | **NEW** (built A1-FRBS, design §17) | None | Tagging on the service-fee line; RELEASED also set automatically when the disbursement is credited | `frbs` | S | AQ20 |
| FRBS 3.1.1<br><sub>Add.2 p.5-6</sub> | FRBS User | Standardised rules for capturing cost centre / responsibility centre so that entries and reports are consistent and reconcilable | Rules enforced; mandatory cost centre validated before posting; applied consistently; exceptions flagged and logged | **CHANGE** (built A1-GL, design §17) | `costCenterRequired` per account validated on journals; `BusinessEvent.costCenter` passed by modules; cost-centre dimension (`dimension/domain/DimensionType.java`) | Cost-centre derivation rules (`acc_cost_center_rule`: source module / event type / unit / AO -> cost centre); the engine fills a missing cost centre from the rules; still missing -> event parked FAILED with `COST_CENTER_MISSING` alert and exception list | `platform (accounting)` | M | AQ26 |
| FRBS 3.1.2<br><sub>Add.2 p.6</sub> | System | Account for early incentives as Other Income, separate from commission and premium | Separate GL mapping; commission GL untouched; audit trail to the source account | **CONFIGURE** | Event `OPS_REMIT_INCENTIVE` with its own component `INCENTIVE_INCOME` (demo 4130 under Commission Income) (`remittance/service/BatchPosting.java`, V992) | Comptrollership maps `INCENTIVE_INCOME` to an Other Income account; demo 4130 re-parented under 4700 Other Income (V998) | `remittance (rules)` | S | OQ23 |

### G. Disbursement: payee maintenance

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| DIS 2.2.0<br><sub>p.69</sub> | Disbursement User | Maintain payees | Payees maintained | **NEW** | Party master with one bank name / account no. (`party/domain/Party.java`); tax profile per party (TIN, ATC, VAT) (`tax/domain/PartyTaxProfile.java`) | `dsb_payee` profile per party code: classification, modes of payment, bank accounts (`dsb_payee_account`), disbursement types, currency, source; maker-checker | `disbursement` | M | AQ11 |
| DIS 2.2.1<br><sub>p.69</sub> | Disbursement User | Receive payee maintenance requests (refund request form, request from Disbursement) | Requests received | **NEW** | None | Payee request queue fed by `payrequest` (RRF / RFP payee data, MKT 2.25.0) and by the fall-out of DIS 3.25.2 | `disbursement` | S | AQ11 |
| DIS 2.2.2<br><sub>p.70</sub> | Disbursement User | Add payee details and classify: Supplier, Insurer, Employee, Client, Others (government agencies, etc.) | Payee added with class | **CHANGE** | Party types include SUPPLIER, INSURER, individual / corporate client; no EMPLOYEE or GOVERNMENT (`party/domain/PartyType.java`) | Add party types EMPLOYEE, GOVERNMENT, OTHER_PAYEE (sub-ledger VENDOR); payee class LOV `PAYEE_CLASS` | `disbursement (+ party)` | S | AQ11 |
| DIS 2.2.3<br><sub>p.70</sub> | Disbursement User | Update payee details | Updated | **NEW** | As DIS 2.2.0 | Edit with maker-checker and audit | `disbursement` | S |  |
| DIS 2.2.4<br><sub>p.70</sub> | Disbursement User | Delete payee details | Deleted | **NEW** | Parties are deactivated, never deleted | Delete allowed only for a payee never used; otherwise deactivate (audit rule) | `disbursement` | S | AQ11 |
| DIS 2.2.5<br><sub>p.70</sub> | Disbursement User | Select a payment method: Credit to Account, Debit BDOIR Main Account (ATD), Manager's Check / Demand Draft, Credit Ticket, Telegraphic Transfer, Online Banking, Check | Mode selectable | **NEW** | Payables modes CHEQUE / BANK_TRANSFER / PDC (`payables/domain/PaymentMode.java`) | Enum `DisbursementMode` (CTA, ATD, MC_DD, CREDIT_TICKET, TT, ONLINE_BANKING, CHECK) with per-mode instrument life cycles | `disbursement` | S | AQ09 |
| DIS 2.2.6<br><sub>p.71</sub> | Disbursement User | Save payee details | Saved | **NEW** | As DIS 2.2.0 | Part of the payee master | `disbursement` | S |  |
| DIS 2.2.7<br><sub>p.71</sub> | Disbursement User | Save a draft of payee details | Draft kept | **NEW** | None | DRAFT status on the payee request | `disbursement` | S |  |
| DIS 2.2.8<br><sub>p.71; Add.1 p.31-32; Add.2 p.11-12</sub> | Disbursement User | View all maintained payees (name, address, account no., mode of payment, disbursement type); existing payees migrated from the current system; details flow to processing. **Addendum 2:** consolidated list, active and inactive by access, consistent with the master, secure downstream use, audit | Role-restricted view; migrated payees complete and correct; payee data flows to processing; maintenance logged | **NEW** | None | Payee list and page (active / inactive filter, masked account no. without `DISB_PAYEE_VIEW_FULL`), migration handler `DISB_PAYEE_MIGRATION` with reconciliation report | `disbursement` | M | AQ11 |

### H. Disbursement: reports

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| DIS 2.3.0<br><sub>p.71</sub> | Disbursement User | Access the end-of-day, real-time and payee reports | Reports available | **NEW** | Report framework only | Report definitions in `disbursement.report` (category "Disbursement") | `disbursement` | M |  |
| DIS 2.3.1<br><sub>p.72</sub> | Disbursement User | Select a date range / covered period | Range selectable | **FIT** | Report parameters | Re-use | `platform (report)` | S |  |
| DIS 2.3.2<br><sub>p.72</sub> | Disbursement User | Select the needed report | Selectable | **FIT** | Catalogue | Re-use | `platform (report)` | S |  |
| DIS 2.3.3<br><sub>p.72</sub> | Disbursement User | View the details of the report | Viewable | **FIT** | On-screen run | Re-use | `platform (report)` | S |  |
| DIS 2.3.4<br><sub>p.72</sub> | Disbursement User | Copy details into another application | Copy / paste | **FIT** | Selectable table, CSV / XLSX | Re-use | `platform (report)` | S |  |
| DIS 2.3.5<br><sub>p.72</sub> | Disbursement User | Download in .xlsx, .ods, .pdf | Formats available | **FIT** | `ExportFormat` PDF / XLSX / ODS / CSV | Re-use | `platform (report)` | S |  |
| DIS 2.3.6<br><sub>p.73</sub> | Disbursement User | Save the report in a desired location | Saved | **FIT** | Browser download | Re-use | `platform (report)` | S |  |
| DIS 2.3.7<br><sub>p.73</sub> | Disbursement User | Print reports | Printed | **FIT** | PDF | Re-use | `platform (report)` | S |  |
| DIS 2.3.8<br><sub>p.73</sub> | Disbursement User | Preview before printing | Preview | **FIT** | On-screen / PDF preview | Re-use | `platform (report)` | S |  |
| DIS 2.3.9<br><sub>p.73</sub> | Disbursement User | Set the print option | Options set | **CHANGE** | As FRBS 2.4.9 | As FRBS 2.4.9 | `platform (report)` | S |  |
| DIS 3.28.0<br><sub>p.100</sub> | System | Automatically generate reports | Reports generated | **NEW** | Report framework, scheduled jobs | Scheduled EOD report run (job `DISB_EOD_REPORTS`) archived in `report_run` | `disbursement` | S |  |
| DIS 3.28.1<br><sub>p.100</sub> | System | Generate the payee report (Appendix B: name, address, account no., mode of payment, disbursement type, source) | Payee report | **NEW** | None | `DSB-PAYEE` | `disbursement` | S |  |
| DIS 3.28.2<br><sub>p.101</sub> | System | Generate the end-of-day reports for the selected date: Remittance, Refund, Summary, Payment to supplier, Employee-related, Other disbursement EOD reports, and the Direct Credit Transaction text file | Seven outputs per day | **NEW** | Payables payment notification file (fixed width / delimited) (`payables/service/PaymentNotificationFormatter.java`) | `DSB-EOD-REMIT`, `DSB-EOD-REFUND`, `DSB-EOD-SUMMARY`, `DSB-EOD-SUPPLIER`, `DSB-EOD-EMPLOYEE`, `DSB-EOD-OTHER` (Appendix B fields) and the DCTF file (new `NotificationFormat.DCTF`) | `disbursement` | M | AQ09 |
| DIS 3.28.3<br><sub>p.101</sub> | System | Real-time reports for any date range: masterlist of all disbursements, unreleased checks, CWT / BIR 2307 on commission, Authority to Debit, Miscellaneous Liability stale checks (all unnegotiated), cash flow | Six reports | **NEW** | None | `DSB-MASTERLIST`, `DSB-UNRELEASED-CHECKS` (ageing current-30 ... 151-180), `DSB-CWT-COMMISSION` (AR-BIR on commission and on incentives vs certificates, per payee / insurer), `DSB-ATD`, `DSB-ML-STALE` (ageing to 181+), `DSB-CASH-FLOW` | `disbursement` | M | AQ16 |
| DIS 3.28.4<br><sub>p.102</sub> | System | Generate the disbursement-request upload fall-out report (reasons, period selected) | Fall-out report | **CHANGE** | Bulk error report per upload (`bulk/**`) | `DSB-UPLOAD-FALLOUT` across uploads for a period, with reason codes | `disbursement` | S |  |
| DIS 3.29.0<br><sub>Add.2 p.7</sub> | Disbursement User | Generate CPC2 incentive reports (period, user, product, role), with calculation breakdown, consistent with accounting, exportable, logged | CPC2 report | **NEW** | Early-remittance incentive only in remittance batches (`remittance/domain/EarlyIncentiveRule.java`) | `DSB-CPC2-INCENTIVE` from the CPC2 lines of remittance batches (DIS 3.29.2) | `remittance` | S | AQ24 |
| DIS 3.30.2<br><sub>Add.2 p.10-11</sub> | Disbursement User | Headcount report per cost centre (printed as a second "BRD DIS 3.30.1") | Headcount per cost centre, filters, export, role-restricted | **NEW** | None | `ORG-HEADCOUNT-CC` from the employee master (DIS 3.30.1) | `platform (organization)` | S | AQ26 |

### I. Disbursement: request intake

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| DIS 2.4.0<br><sub>p.73</sub> | Disbursement User | View the list of transactions for disbursement processing | Work list | **NEW** | In-app Disbursement queue (SENT / ACKNOWLEDGED / DV_ASSIGNED / PAID / RETURNED) (`opsledger/service/DisbursementQueueService.java`, screen `/operations/disbursements`) | Disbursement workbench (tabs by stage) replaces the queue screen | `disbursement` | M | OQ02 |
| DIS 2.4.1<br><sub>p.73</sub> | Disbursement User | Select a date range based on the date the request was received | Filter by received date | **NEW** | Queue list filters | Workbench filter | `disbursement` | S |  |
| DIS 2.4.2<br><sub>p.74</sub> | Disbursement User | View and download attached documents | Attachments viewable | **CHANGE** | Attachments with multi-link (`attachment/**`); queue carries one `attachment_ref` | Request attachments linked to the DV | `disbursement` | S |  |
| DIS 2.4.3<br><sub>p.74</sub> | Disbursement User | View payee details from the payee master during processing (name, currency, mode of payment, taxes for suppliers) | Payee data shown | **NEW** | None | Payee panel on the DV (tax from `PartyTaxProfile`) | `disbursement` | S |  |
| DIS 2.4.4<br><sub>p.74</sub> | Disbursement User | Filter and sort every column | All columns filter / sort | **FIT** | `DataTable` filters and sort | Re-use | `disbursement` | S |  |
| DIS 2.5.0<br><sub>p.75</sub> | Disbursement User | Upload an .xlsx or .ods file with a list of disbursement requests; successful rows join the processing list | Upload creates requests | **CHANGE** | Bulk framework (XLSX / CSV / ODS, per-row outcome) | Handler `DISB_REQUESTS` | `disbursement` | S | AQ12 |
| DIS 2.5.1<br><sub>p.75</sub> | Disbursement User | View the fall-out with the reason of each failed row | Fall-out with reasons | **FIT** | Bulk error report | Re-use (DIS 3.28.4 for the period view) | `platform (bulk)` | S |  |
| DIS 2.6.0<br><sub>p.75</sub> | Disbursement User | Receive requests from other units | Requests received | **NEW** | Queue receives remittance, refund, 2307 and pass-on requests (`DisbursementGateway`) | `dsb_request` from gateway, upload, manual encoding and `payrequest` | `disbursement` | M | OQ02 |
| DIS 2.6.1<br><sub>p.76</sub> | Disbursement User | Manually encode individual requests received by e-mail; payee types auto-populate from the payee master; select the disbursement type (Remittance, Refund, Payment to supplier, Employee-related, Other); flow to the checker | Encoded request goes to checker | **NEW** | None | Encode form (payee lookup, type, amount, currency, purpose, attachments) -> DV template (DIS 2.7.5) | `disbursement` | S |  |
| DIS 2.6.2<br><sub>p.76</sub> | Disbursement User | View system-triggered requests; they flow to the checker | Visible and routed | **NEW** | Queue list | Workbench tab "System requests" | `disbursement` | S | OQ02 |
| DIS 3.25.0<br><sub>p.95</sub> | System | Receive system-triggered requests with the RFP number; auto-reject requests without maintained payee and notify the requestor; for refund to client and remittance to insurer create the DV automatically and route straight to the approver | As stated | **CHANGE** | `DisbursementGateway` port with the queue adapter (`opsledger/service/adapter/QueueDisbursementGateway.java`); remittance batches push REMITTANCE requests (`remittance/service/BatchPosting.java`) | `disbursement` implements the port (`@Primary` adapter); `Spec` gains RFP no., payee class, disbursement type, attachments, accounting references; no payee -> RETURNED `PAYEE_NOT_MAINTAINED` + notification; REFUND / REMITTANCE -> DV auto-built, stage FOR_APPROVAL | `disbursement (+ opsledger)` | M | OQ02, AQ12 |
| DIS 3.25.1<br><sub>p.96</sub> | System | Classify requests: Remittance, Refund, Payment to supplier, Payment to government agencies, Payment to other bank units | Auto classification | **NEW** | Queue type REMITTANCE / REFUND / CWT2307 / PASS_ON | Type from source and payee class (LOV `DISBURSEMENT_TYPE`) | `disbursement` | S |  |
| DIS 3.25.2<br><sub>p.96</sub> | System | Match the payee to the payee master; report of unmatched payees | Match and no-match report | **NEW** | None | Matching by party code, then name + account no.; `DSB-PAYEE-NOMATCH` | `disbursement` | S | AQ11 |

### J. Disbursement: processing, vouchers, modes and cost centres

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| DIS 2.7.0<br><sub>p.77</sub> | Disbursement User | Process a disbursement via Credit to Account, ATD, MC / DD, Credit Ticket, Telegraphic Transfer, Online Banking, Check | All seven modes | **NEW** | Payables payment voucher with cheque / bank transfer / PDC, cheque books, payment notification file (`payables/**`) | `dsb_voucher` with a mode-specific instrument (`dsb_instrument`); cheques reuse payables cheque books | `disbursement` | L | AQ09 |
| DIS 2.7.1<br><sub>p.77</sub> | Disbursement User | View payee details from the payee master (name, currency, mode, taxes for suppliers) | Payee data shown | **NEW** | As DIS 2.4.3 | As DIS 2.4.3 | `disbursement` | S |  |
| DIS 2.7.2<br><sub>p.78</sub> | Disbursement User | Select a transaction; transactions taken by another user are disabled | One user per item | **CHANGE** | Work-case claim / assign (`workflow/service/WorkAssignmentService.java`) | DV cases claimed on selection | `disbursement` | S |  |
| DIS 2.7.3<br><sub>p.78</sub> | Disbursement User | View the selected transaction | Details viewable | **NEW** | None | DV page (header, WorkflowPanel, tabs Details / Entry / Instrument / Documents / History) | `disbursement` | S |  |
| DIS 2.7.4<br><sub>p.78</sub> | Disbursement User | Update details; flags on mandatory or wrong fields; cannot push with missing or wrong data | Validation blocks submit | **NEW** | None | Field validation and submit guard `DV_INCOMPLETE` | `disbursement` | S |  |
| DIS 2.7.5<br><sub>p.79</sub> | Disbursement User | Create the disbursement voucher: automatic on selection (DV no. generated and attached) or manual template for encoded requests | DV auto-numbered | **NEW** | Payment voucher numbering `PV-...` (payables) | `DV-<yyyy>-nnnnnn` via `DocumentNumberService`; template form for encoded requests | `disbursement` | M | AQ13 |
| DIS 2.7.6<br><sub>p.80</sub> | Disbursement User | Create the proforma entry automatically (also after the manual template); it is editable | Editable proforma | **NEW** | Accounting engine builds lines from rules (`accounting/service/JournalLineBuilder.java`) | Engine preview of the DV event stored as `dsb_voucher_line`; processor may edit lines (account eligibility checked by `coa/service/PostingEligibilityService.java`); edited flag shown to approver; posted at approval | `disbursement` | M | AQ13 |
| DIS 2.7.7<br><sub>p.81</sub> | Disbursement User | Process ATD: generate the ATD (auto or template), send it to the processing branch with the debit instruction looping in the requestor, receive the branch confirmation | ATD cycle | **NEW** | Document templates and e-mail (`docgen/**`, `messaging/**`) | ATD document (template `DSB_ATD`), e-mail to the branch (LOV `BRANCH_EMAIL`) with requestor in copy; statuses DIS 2.8.2 | `disbursement` | M | AQ09 |
| DIS 2.7.8<br><sub>p.81</sub> | Disbursement User | Process a Demand Draft / Manager's Check: generate the form, route for sign-off, transact at the branch, receive the issued MC / DD | MC / DD cycle | **NEW** | None | Form `DSB_MC_DD`; statuses DIS 2.8.4 | `disbursement` | S |  |
| DIS 2.7.9<br><sub>p.82</sub> | Disbursement User | Process a Credit Ticket / Telegraphic Transfer: generate the form, route for sign-off, transact at the branch of account, receive the validated ticket | CT / TT cycle | **NEW** | None | Forms `DSB_CREDIT_TICKET`, `DSB_TT`; statuses DIS 2.8.3 | `disbursement` | S |  |
| DIS 2.7.10<br><sub>p.82</sub> | Disbursement User | Attach a file for expense allocation to the DV | Allocation file attached | **CHANGE** | Attachments; supplier invoice lines with cost centre (`payables/domain/SupplierInvoiceLine.java`) | Allocation upload (account, cost centre, amount) that builds the expense lines of the proforma | `disbursement` | S |  |
| DIS 2.7.11<br><sub>p.82</sub> | Disbursement User | Submit for review / checking | Submitted | **NEW** | Workflow engine (`workflow/**`) | Workflow `DISB_VOUCHER` IN_PROCESS -> FOR_REVIEW | `disbursement` | S |  |
| DIS 2.7.12<br><sub>p.83</sub> | Disbursement User | Automatically e-mail the confirmation and the remittance schedule to the intended recipient for the day's processed accounts | E-mail at end of day | **CHANGE** | Remittance schedule documents and dispatch (`remittance/service/ScheduleDispatch.java`), outbound e-mail with protection | Job `DISB_EOD_CONFIRMATION` sends per payee the payment advice and, for remittances, the batch schedule | `disbursement` | S |  |
| DIS 3.30.0<br><sub>Add.2 p.9</sub> | Disbursement User | Allocate expenses by cost centre; entries carry the right cost centre per supplier; visible in review; logged | Cost centre on every expense line | **CHANGE** | Cost-centre dimension on journal lines; `costCenterRequired` | Default cost centre per supplier / employee (payee master), per-line override, allocation file (DIS 2.7.10), rules of FRBS 3.1.1 | `disbursement` | M | AQ26 |
| DIS 3.30.1<br><sub>Add.2 p.9-10</sub> | User | Maintain cost-centre details (employee number, name, position, unit, related information); no duplicate employee number; history | Master with validation and audit | **NEW** | Cost-centre dimension values (code, name) only (`dimension/domain/DimensionValue.java`) | `org_employee` master (employee no. unique, name, position, unit, cost centre, active, effective dates) in `organization`, maker-checker | `platform (organization)` | M | AQ26 |

### K. Disbursement: status, OR / AR and CWT tagging

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| DIS 2.8.0<br><sub>p.83</sub> | Disbursement User | Perform disbursement status tagging | Status tagging | **NEW** | Queue statuses only | Instrument status machine per mode (design section 7) | `disbursement` | M |  |
| DIS 2.8.1<br><sub>p.83</sub> | Disbursement User | Tag check status "Released" once the recipient receives the check | Released | **NEW** | Payables cheque statuses for vouchers (APPROVED / VOIDED) and PDC issued | Check PRINTED -> RELEASED (release date, picked-up by) | `disbursement` | S |  |
| DIS 2.8.2<br><sub>p.84</sub> | Disbursement User | ATD "Printed" -> "Emailed" -> "Debited" after branch confirmation | ATD statuses | **NEW** | None | ATD statuses | `disbursement` | S |  |
| DIS 2.8.3<br><sub>p.84</sub> | Disbursement User | Credit Ticket / TT "Printed" -> "Debited" after branch confirmation | CT / TT statuses | **NEW** | None | CT / TT statuses | `disbursement` | S |  |
| DIS 2.8.4<br><sub>p.85</sub> | Disbursement User | MC / DD "Printed" -> "Received" (from branch) -> "Released" (to client) | MC / DD statuses | **NEW** | None | MC / DD statuses | `disbursement` | S |  |
| DIS 2.8.5<br><sub>p.85</sub> | Disbursement User | Edit the status tagging and submit it for approval | Edited tag approved | **NEW** | None | Status-correction request (workflow `DISB_STATUS_EDIT`, approver TL) | `disbursement` | S | AQ15 |
| DIS 2.9.0<br><sub>p.85</sub> | Disbursement User | Cancel an "In Process" transaction on request; search, select, remarks; status "Cancelled" | Cancelled with remarks | **NEW** | Queue RETURNED | Cancel from IN_PROCESS (reason) -> CANCELLED; source notified (`DisbursementStatusChanged`) | `disbursement` | S |  |
| DIS 2.22.0<br><sub>p.93</sub> | Disbursement User | Upload files (text, .csv, .xlsx) to update statuses: deposited checks, credited payments | Uploads update statuses | **NEW** | Bulk framework; no TXT fixed-width reader for this layout yet | Handlers `DISB_CHECKS_NEGOTIATED` (bank deposited-checks file) and `DISB_CTA_CREDITED` (ACA credited .txt) with configurable layouts | `disbursement` | M | AQ09 |
| DIS 3.26.0<br><sub>p.96</sub> | System | Perform automatic disbursement status tagging | Auto tagging | **NEW** | None | Status transitions fired by print, extraction, uploads and jobs | `disbursement` | M |  |
| DIS 3.26.1<br><sub>p.97</sub> | System | Tag a check "Negotiated" once deposited (uploaded deposited-checks file) | Negotiated | **NEW** | Bank reconciliation matches cheque payments (`receivables/service/BankMatchingService.java`) | From the upload of DIS 2.22.0, or from a bank-reconciliation match of the cheque (hook) | `disbursement` | S |  |
| DIS 3.26.2<br><sub>p.97</sub> | System | Tag a check "Staled" 180 days after the printed date when still Printed or Released | Staled at 180 days | **NEW** | None | Job `DISB_CHECK_STALE` (daily; parameter `DISB_STALE_DAYS=180`) | `disbursement` | S | AQ14 |
| DIS 3.26.3<br><sub>p.97</sub> | System | Tag ATD "Printed" after the PDF is generated | Printed | **NEW** | None | On ATD document generation | `disbursement` | S |  |
| DIS 3.26.4<br><sub>p.98</sub> | System | Credit to Account "Extracted" after processing, then "Credited" from the uploaded .txt of credited accounts | Extracted -> Credited | **NEW** | None | Extracted when included in the DCTF; Credited by `DISB_CTA_CREDITED` | `disbursement` | S | AQ09 |
| DIS 3.26.5<br><sub>p.98</sub> | System | Tag Credit Ticket / TT "Printed" after processing | Printed | **NEW** | None | On form generation | `disbursement` | S |  |
| DIS 3.26.6<br><sub>p.98</sub> | System | Tag Demand Draft / Manager's Check "Printed" after processing | Printed | **NEW** | None | On form generation | `disbursement` | S |  |
| DIS 3.26.7<br><sub>p.99</sub> | System | Online Banking "Approved" after processing, then "Debited" when the BOB transaction is fully approved, using the voucher reference | Approved -> Debited | **NEW** | None | Manual confirmation or upload of the BOB approval report by voucher reference until a BOB interface exists (seam `BankChannelPort`) | `disbursement` | S | AQ09 |
| DIS 2.10.0<br><sub>p.86</sub> | Disbursement User | Perform Official Receipt / Acknowledgement Receipt tagging | OR / AR tagging | **NEW** | Remittance stores the insurer OR per batch line (`remittance/service/InsurerOrService.java`) | OR / AR tag on the DV; for remittance DVs the insurer OR uploaded in remittance is shown and not re-keyed | `disbursement` | S | AQ17 |
| DIS 2.10.1<br><sub>p.86</sub> | Disbursement User | Receive the OR / AR | Received | **NEW** | As DIS 2.10.0 | As DIS 2.10.0 | `disbursement` | S | AQ17 |
| DIS 2.10.2<br><sub>p.86</sub> | Disbursement User | Tag OR / AR: search the DV, select it, add OR / AR no., date and date received | Tag saved | **NEW** | None | `dsb_voucher_receipt_tag` | `disbursement` | S | AQ17 |
| DIS 2.11.0<br><sub>p.86</sub> | Disbursement User | Perform CWT tagging | CWT tagging | **NEW** (register built A1-FRBS, design §17) | Planned: 2307 intake on premium (cashiering `csh_cwt_tag`), BIR certificate submission of commissions (commission `cmr_certificate_submission`) | One received-certificate register in `tax` (`tax_certificate_received`), used by Disbursement tagging, Commission (CMRID.015) and SAWT | `disbursement (+ tax)` | S | AQ16, OQ41 |
| DIS 2.11.1<br><sub>p.86</sub> | Disbursement User | Receive the actual CWT: received (from insurers) / released (to suppliers) | Received / released | **NEW** (register built A1-FRBS, design §17) | BDOI-issued 2307 certificates exist (`tax/domain/Certificate2307.java`) | RECEIVED for insurer certificates on commission / incentives; RELEASED for BDOI 2307 to suppliers | `disbursement (+ tax)` | S | AQ16 |
| DIS 2.11.2<br><sub>p.87</sub> | Disbursement User | Tag CWT: search the DV, select it, add period covered, date received / released and amount | Tag saved | **NEW** | None | Tag on the DV linked to the certificate register | `disbursement` | S | AQ16 |
| DIS 2.12.0<br><sub>p.87</sub> | Disbursement User | Generate CWT by filling the BIR Form 2307 template in the system and saving it in .pdf | 2307 PDF | **FIT** | 2307 per payee and quarter with PDF and batch PDF (`tax/service/Certificate2307Service.java`, `Certificate2307Pdf.java`) | Re-use; action "Generate 2307" on supplier DVs | `platform (tax)` | S |  |

### L. Disbursement: review, approval, cancellation, end of day, account funding

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| DIS 2.13.0<br><sub>p.87</sub> | Disbursement User (TL) | View assigned transactions and review / check the selected one | Review queue | **NEW** | Work queues (`workflow/service/WorkQueueService.java`) | Stage FOR_REVIEW queue | `disbursement` | S |  |
| DIS 2.14.0<br><sub>p.88</sub> | Disbursement User (TL / Approver) | Return transactions with remarks (reason, special instruction) | Returned with remarks | **NEW** | Workflow return transitions | `return` transitions to IN_PROCESS with reason | `disbursement` | S |  |
| DIS 2.15.0<br><sub>p.88</sub> | Disbursement User (TL) | Submit the transaction for approval | Submitted | **NEW** | Workflow | FOR_REVIEW -> FOR_APPROVAL | `disbursement` | S |  |
| DIS 2.16.0<br><sub>p.88</sub> | Disbursement User | Perform end-of-day processing | EOD processed | **NEW** | None | EOD run per date: freezes the day's approved DVs, builds DCTF, cheque print batch, ATD / CT / TT / MC-DD forms, reports | `disbursement` | M |  |
| DIS 2.16.1<br><sub>p.88</sub> | Disbursement User | Access the autogenerated Direct Credit Transaction File and forward it to TPD for ACA processing | DCTF available | **CHANGE** | Payment notification file per bank account in fixed width / delimited (`payables/domain/NotificationFormat.java`) | New format DCTF (Appendix B: 12-digit account, 30-char payee, 12 spaces, reference, amount 000000000000.00, header with date and file name); e-mail to TPD | `disbursement (+ payables)` | S | AQ09 |
| DIS 2.16.2<br><sub>p.88</sub> | Disbursement User | Access the autogenerated checks for printing | Check print batch | **CHANGE** | Cheque numbers from cheque books (`payables/service/BankAccountService.java` `nextChequeNo`); voucher report | Check print layout per bank (docgen template), batch print with number verification | `disbursement` | M | AQ14 |
| DIS 2.16.3<br><sub>p.89</sub> | Disbursement User | Access the autogenerated ATDs | ATDs available | **NEW** | None | EOD output list | `disbursement` | S |  |
| DIS 2.16.4<br><sub>p.89</sub> | Disbursement User | Access the autogenerated Credit Ticket / TT forms | Forms available | **NEW** | None | EOD output list | `disbursement` | S |  |
| DIS 2.16.5<br><sub>p.89</sub> | Disbursement User | Access the autogenerated MC / DD forms | Forms available | **NEW** | None | EOD output list | `disbursement` | S |  |
| DIS 2.16.6<br><sub>p.89</sub> | Disbursement User | Print single or multiple vouchers with print options | Voucher print | **CHANGE** | Payment voucher PDF (`payables/report/PaymentVoucherReport.java`) | DV PDF (docgen) with batch print and options | `disbursement` | S |  |
| DIS 2.17.0<br><sub>p.90</sub> | Disbursement User | Fund the Main BDOIR China bank account via Business Online Banking (BOB) | Funding recorded | **NEW** | None | Funding request (`dsb_funding_request`: source and target bank accounts, amount, purpose) and the transfer event `DISB_FUND_TRANSFER` | `disbursement` | M | AQ10 |
| DIS 2.17.1<br><sub>p.90</sub> | Disbursement User | Log in to BOB | BOB login | **OUT** | BOB is BDO's external banking channel | External link only (LOV `OPS_EXTERNAL_LINK`) | `disbursement` | S | AQ10 |
| DIS 2.17.2<br><sub>p.90</sub> | Disbursement User | Route the request to a verifier (another Team Leader) and two approvers | Routing | **NEW** | Workflow | Workflow `DISB_FUNDING`: CREATED -> FOR_VERIFICATION (TL other than maker) -> FOR_APPROVAL_1 -> FOR_APPROVAL_2 -> APPROVED | `disbursement` | S | AQ10 |
| DIS 2.17.3<br><sub>p.90</sub> | Disbursement User (TL) | Verify the received funding request, then route to the two approvers | Verified | **NEW** | Workflow | Verify transition | `disbursement` | S | AQ10 |
| DIS 2.17.4<br><sub>p.90; Add.1 p.33</sub> | Disbursement User (Approver) | Approve / decline the funding request, add and save remarks (printed as a second "2.17.3"; renumbered 2.17.4 by Addendum 1) | Approve / decline with remarks | **NEW** | Workflow | Approve / decline / return transitions | `disbursement` | S | AQ10 |
| DIS 2.18.0<br><sub>p.91</sub> | Disbursement User | Cancel a "For review" transaction on request; search by payee, client, amount, reference; remarks | Cancelled | **NEW** | None | Cancel from FOR_REVIEW; search on the workbench | `disbursement` | S |  |
| DIS 2.19.0<br><sub>p.91</sub> | Disbursement User (Approver) | Approve / decline single or multiple transactions (for posting, BOB transactions) and post to the GL; approval triggers creation / regularisation of the accounting entries; remarks | Bulk approve and posting | **NEW** | Journal posting through `SystemJournalService` / accounting engine | Approval posts the DV journal (proforma lines) and settles the source (open items, ops ledger); bulk approval with per-item result | `disbursement` | M | AQ13 |
| DIS 2.20.0<br><sub>p.92</sub> | Disbursement User (Approver) | Cancel an "Approved" transaction on request; status "Cancelled"; regularise accounting and data (refund ARs, remittance to insurer DTIP, ORs) | Cancelled with regularisation | **NEW** | Payables void pattern: negative event, unmatch open items (`payables/service/PaymentVoucherService.java`) | Cancel = reversal journal (`DV:<no>:CANCEL`), unmatch, and `DisbursementStatusChanged(CANCELLED)` so sources restore (remittance lines back to extractable, refund unapplied re-opened through cashiering, OR tags cleared) | `disbursement` | L | AQ15 |
| DIS 2.21.0<br><sub>p.92</sub> | Disbursement User (Approver) | Reject a transaction with remarks | Rejected | **NEW** | Workflow | Reject -> REJECTED (terminal); source notified | `disbursement` | S |  |

### M. Disbursement: masters, accounting regularisation, incentives

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| DIS 2.23.0<br><sub>p.93</sub> | Disbursement User (TL) | Maintain the check series | Series maintained | **FIT** | Cheque books per bank account with first / last / next no. and status (`payables/domain/ChequeBook.java`) | Re-use | `platform (payables)` | S |  |
| DIS 2.23.1<br><sub>p.93</sub> | Disbursement User (TL) | Add the beginning check series | Series added | **FIT** | `BankAccountService.addChequeBook` | Re-use | `platform (payables)` | S |  |
| DIS 2.23.2<br><sub>p.93</sub> | Disbursement User (TL) | Edit the beginning check series | Series edited | **CHANGE** | Cheque books can be cancelled, not edited | Edit first / last no. while no leaf is used (maker-checker) | `platform (payables)` | S |  |
| DIS 2.24.0<br><sub>p.93</sub> | Disbursement User (TL) | Maintain BDOIR bank accounts | Accounts maintained | **FIT** | Bank account master with GL account, currency, branch, notification format, maker-checker (`payables/domain/BankAccount.java`) | Re-use | `platform (payables)` | S |  |
| DIS 2.24.1<br><sub>p.93</sub> | Disbursement User | Add a BDOIR bank account | Added | **FIT** | As DIS 2.24.0 | Re-use | `platform (payables)` | S |  |
| DIS 2.24.2<br><sub>p.94; Add.1 p.33</sub> | Disbursement User | Tag the status of a BDOIR bank account active / inactive (printed as a second "2.24.1"; renumbered 2.24.2 by Addendum 1) | Active / inactive | **CHANGE** | No deactivate action on bank accounts (`payables/service/BankAccountService.java`) | Activate / deactivate with maker-checker; inactive accounts refused for new DVs | `platform (payables)` | S |  |
| DIS 3.27.0<br><sub>p.99; Add.1 p.31</sub> | System | Regularise accounting entries and the database automatically for every disbursement transaction (approved, cancelled, etc.); entries editable. **Addendum 1:** only unposted transactions are editable; list of unregularised transactions | Regularised; unposted editable; list of unregularised | **NEW** | Event log with FAILED events and re-processing (`accounting/domain/AccountingEventLog.java`) | Every DV state change with accounting impact posts through the engine in the same transaction; failures parked; `DSB-UNREGULARIZED` lists DVs with parked or pending postings; lines editable until posting | `disbursement` | M | AQ13 |
| DIS 3.27.1<br><sub>p.100</sub> | System | Create the reversal entry in the GL (flowing to FRBS) for stale checks (on "Stale") and negotiated checks (on "Negotiated"); amounts editable | Reversal entries on stale / negotiated | **NEW** | None | Check clearing model: approval credits "Checks outstanding" (`@CHECK_CLEARING`); Negotiated: Dr checks outstanding / Cr bank; Stale: Dr checks outstanding / Cr Miscellaneous Liability - stale checks; proforma editable before posting | `disbursement` | M | AQ02, AQ14 |
| DIS 3.27.2<br><sub>p.100</sub> | System | Link related transactions to a single invoice number | One invoice reference across related transactions | **CHANGE** (built A1-OPSX, design §17) | Endorsements and cancellations create new invoices linked by `parent_invoice_no` (`booking/domain/BookedInvoice.java`); ops ledger keeps one row per invoice (`opsledger/domain/OpsInvoice.java`) | `root_invoice_no` on booking and `ops_invoice`; every DV, receipt, remittance line, adjustment and ACSL correction carries it; Invoice 360 "family" view | `booking, opsledger` | M | AQ29, Q32 |
| DIS 3.29.1<br><sub>Add.2 p.7-8</sub> | System | Generate a Service Invoice automatically after the early-incentive computation, applying 2% withholding tax; supports OR issuance and posting; no manual SI for early incentives | SI per qualified early incentive, 2% WTAX, linked, logged | **CHANGE** (built A1-OPSX, design §17) | Remittance computes the early incentive and issues an incentive OR through `ReceiptIssuer` (`remittance/service/BatchPosting.java`); service invoices exist in booking (`booking/service/ServiceInvoiceService.java`) | Remittance calls `ServiceInvoiceService.issue` (new trigger ON_INCENTIVE, type EARLY_INCENTIVE, WTAX 2%) per batch; OR references the SI; manual SI of that type refused | `remittance (+ booking)` | M | OQ23, AQ25 |
| DIS 3.29.2<br><sub>Add.2 p.8-9</sub> | System | Account for CPC2 incentives (package Fire and Motor incentives agreed with insurers on top of regular commission) separately from basic commission; computed per remittance cycle from the TSU product table; recognised as incentive income; audit trail | CPC2 only for packaged Fire / Motor; % from TSU table; per remittance, not cumulative; distinct entries | **CHANGE** (built A1-OPSX, design §17) | Early incentive only (`remittance/domain/EarlyIncentiveRule.java`); incentive criteria "CPC2" planned in catalog (PMADD07/08) | Remittance computes CPC2 per batch line from `cat_incentive_criteria` (CPC2, packaged Fire / Motor), component `CPC2_INCENTIVE`, event `OPS_REMIT_CPC2`; batch totals show CPC2 separately | `remittance (+ catalog)` | M | OQ39, PQ04, Q33, AQ24 |

### N. Marketing refund and cash-advance requests

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| MKT 1.2.0<br><sub>p.105</sub> | Marketing User | Receive refund-to-client requests from Marketing AOs and cash-advance requests from employees | Requests received | **NEW** | Cashiering plans unapplied dispositions with REFUND (OPERATIONS_DESIGN 4.2) | `payrequest` module: `prq_request` (REFUND / CASH_ADVANCE), from AO / employee | `payrequest` | M | AQ18 |
| MKT 1.3.0<br><sub>p.106</sub> | Marketing User | View the list of received requests | List | **NEW** | None | Request workbench | `payrequest` | S |  |
| MKT 1.4.0<br><sub>p.106</sub> | Marketing User | Select single or multiple requests | Multi-select | **NEW** | None | Bulk actions | `payrequest` | S |  |
| MKT 1.5.0<br><sub>p.106</sub> | Marketing User | View the details of the request | Details | **NEW** | None | Request page | `payrequest` | S |  |
| MKT 1.6.0<br><sub>p.106</sub> | Marketing User | Filter requests | Filters | **FIT** | `DataTable` filters | Re-use | `payrequest` | S |  |
| MKT 1.7.0<br><sub>p.106</sub> | Marketing User | Access the unapplied payment report with a date range | Report | **CHANGE** | Planned cashiering unapplied workbench and reports (OPERATIONS_DESIGN section 11) | Grant `CSH-UNAPPLIED` style report to Marketing processors (segment-restricted) | `cashiering` | S | AQ18 |
| MKT 1.7.1<br><sub>p.106</sub> | Marketing User | Download the report in .xlsx | XLSX | **FIT** | Export | Re-use | `platform (report)` | S |  |
| MKT 1.7.2<br><sub>p.107</sub> | Marketing User | Save the report in a folder in .xlsx | Saved | **FIT** | Browser download | Re-use | `platform (report)` | S |  |
| MKT 1.7.3<br><sub>p.107</sub> | Marketing User | Print the report | Printed | **FIT** | PDF | Re-use | `platform (report)` | S |  |
| MKT 1.8.0<br><sub>p.107</sub> | Marketing User | Return a request to the previous handler | Returned | **NEW** | Workflow | Return transitions | `payrequest` | S |  |
| MKT 1.9.0<br><sub>p.107</sub> | Marketing User (Reviewer) | Assign / re-assign refund requests to a user | Assignment | **CHANGE** | `WorkAssignmentService` | Re-use on `PRQ_REFUND` cases | `payrequest` | S |  |
| MKT 1.10.0<br><sub>p.107</sub> | Marketing User | Accomplish the forms: Refund Request Form (single or multiple accounts) or Request for Payment (cash advance) | Forms completed | **NEW** | None (Appendix D layouts p.152) | RRF lines (AR no., client no., assured, amount, reason, branch / unit, category A / B, account / check name) and RFP (type, payee, amount, purpose, mode); PDF forms | `payrequest` | M | AQ18 |
| MKT 1.11.0<br><sub>p.108</sub> | Marketing User | Send for validation (refund of a cancelled policy): ACSL checks the cancelled premium and the insurer's return of remitted premium; Cashiering confirms reinstatement to the unapplied (UPP) list with a new AR no. | ACSL and Cashiering validation | **NEW** | Adjustment sets AR Insurer after a remitted cancellation (`adjustment/service/LedgerEffects.java`); cashiering reinstatement planned | Parallel validation tasks: ACSL analysis request (ACSL 2.5.5) and Cashiering confirmation (via `UnappliedSink` result / AR no.); both required before review | `payrequest (+ acsl, cashiering)` | M | OQ15 |
| MKT 1.12.0<br><sub>p.108</sub> | Marketing User | Upload supporting documents (.xlsx, .pdf, .ods, .txt) | Upload | **FIT** | Attachments with document types (`attachment/**`) | Re-use | `payrequest` | S |  |
| MKT 1.13.0<br><sub>p.108</sub> | Marketing User | Access uploaded documents: select one or several, view, download several, save, preview side by side | Multi-document handling | **CHANGE** | One-at-a-time download / preview | Multi-select ZIP download and split preview (platform attachment panel) | `platform (attachment)` | S |  |
| MKT 1.14.0<br><sub>p.109</sub> | Marketing User | Submit the request for review | Routed | **NEW** | Workflow | `PRQ_REFUND` / `PRQ_CASH_ADVANCE` | `payrequest` | S |  |
| MKT 1.15.0<br><sub>p.109</sub> | Marketing User (Reviewer) | Review and route to the approver | Routed | **NEW** | Workflow | Stage FOR_REVIEW | `payrequest` | S |  |
| MKT 1.16.0<br><sub>p.109</sub> | Marketing User (Approver) | Approve / decline with remarks | Decision with remarks | **NEW** | Workflow | Stage FOR_APPROVAL | `payrequest` | S |  |
| MKT 1.16.1<br><sub>p.109</sub> | Marketing User (Approver) | Approve / decline refund requests | Decision | **NEW** | Workflow | As MKT 1.16.0 | `payrequest` | S |  |
| MKT 1.16.2<br><sub>p.109</sub> | Human Resource User | Approve / decline cash-advance requests with remarks | HR decision | **NEW** | Workflow | Stage HR_APPROVAL (role `HR_APPROVER`) | `payrequest` | S | AQ18 |
| MKT 1.16.3<br><sub>p.109</sub> | Marketing User (Approver) | Approve / decline a disbursed-check cancellation request with remarks | Decision | **NEW** | Workflow | `PRQ_CHECK_CANCEL` approval | `payrequest` | S |  |
| MKT 1.17.0<br><sub>p.110</sub> | Marketing User | Cancel a request before approval | Cancelled | **NEW** | Workflow | Cancel transition before FOR_APPROVAL decision | `payrequest` | S |  |
| MKT 1.18.0<br><sub>p.110</sub> | All Marketing | View the status of each request for a date range | Status view | **NEW** | None | Status column and tracker | `payrequest` | S |  |
| MKT 1.18.1<br><sub>p.110</sub> | All Marketing | Extract the requests with status for a date range; .xlsx / .ods; print | Extract | **NEW** | Report framework | Report `PRQ-STATUS` | `payrequest` | S |  |
| MKT 1.19.0<br><sub>p.110</sub> | Marketing User | Request cancellation of a disbursed check, route for review and approval, remarks | Request routed | **NEW** | None | `PRQ_CHECK_CANCEL` -> on approval, Disbursement cancellation of the approved DV (DIS 2.20.0) | `payrequest` | S | AQ15 |
| MKT 1.20.0<br><sub>p.111</sub> | Marketing User | Receive confirmation of the disbursed refund / cash advance from Disbursement | Confirmation | **NEW** | `DisbursementStatusChanged` event (`opsledger/service/DisbursementQueueService.java`) | Notification on RELEASED / CREDITED | `payrequest` | S |  |
| MKT 2.22.0<br><sub>p.111</sub> | System | Attach uploaded documents to the account and the request | Linked documents | **CHANGE** | Attachment multi-link (V27) | Link to request, invoice (ARN / invoice) and client | `payrequest` | S |  |
| MKT 2.23.0<br><sub>p.111</sub> | System | Prevent duplicate refund processing based on the AR number on the RRF | Duplicate blocked | **NEW** | None | Unique active refund per AR no. (`PRQ_DUPLICATE_AR`) | `payrequest` | S |  |
| MKT 2.24.0<br><sub>p.111</sub> | System | Send refunds to Disbursement after approval; cash advances to HR after Marketing approval, then to Disbursement after HR approval | Automatic routing | **NEW** | `DisbursementGateway` | On final approval `DisbursementGateway.send` (type REFUND / CASH_ADVANCE) | `payrequest` | S |  |
| MKT 2.25.0<br><sub>p.112; Add.1 p.34-35</sub> | System | Capture CA/SA information from the form and add it to the client record upon approval. **Addendum 1:** Credit to account: payee name, BDO account no.; via check: payee name | CA / SA on the client record | **CHANGE** | Client has bank-client flag and CIF only (`crm/domain/Client.java`) | `crm_client_payout_account` (mode CTA / CHECK, payee name, BDO account no., source request, active); written on approval | `crm` | S | AQ19 |
| MKT 2.25.1<br><sub>p.112</sub> | System | Prevent duplicate CA/SA information | Duplicate blocked | **CHANGE** | None | Unique (client, account no.) | `crm` | S | AQ19 |
| MKT 2.26.0<br><sub>p.112</sub> | System | Track the status of each request | Status tracked | **NEW** | Workflow history | Case history and status tracker | `payrequest` | S |  |

### O. ACSL: input files, reconciliation and reports

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| ACSL 2.2.0<br><sub>p.115</sub> | ACSL User | Generate input files | Input files generated | **CHANGE** | Extract repository and invoice ledger queries (`opsledger/service/ExtractRepositoryService.java`, `InvoiceLedgerQueryService.java`) | On-demand extracts of booked accounts per insurer / period (ACSL 2.14.2) | `acsl` | S |  |
| ACSL 2.2.1<br><sub>p.115</sub> | ACSL User | Receive input files from insurers (statement of account) | SOA received | **CHANGE** | `InsurerFileInbox` manual upload (`opsledger/service/adapter/ManualInsurerFileInbox.java`) | SOA upload per insurer and period (ACSL 2.4.0) | `acsl` | S | AQ21 |
| ACSL 2.3.0<br><sub>p.115</sub> | ACSL User | Access system-generated reports anytime | On demand | **FIT** | Report framework | Category "ACSL" | `platform (report)` | S |  |
| ACSL 2.3.1<br><sub>p.116</sub> | ACSL User | View the list of reports | List | **FIT** | Catalogue | Re-use | `platform (report)` | S |  |
| ACSL 2.3.2<br><sub>p.116</sub> | ACSL User | Select single or multiple reports | Multi-select | **CHANGE** | One at a time | Report batch (FRBS 2.4.5) | `platform (report)` | S |  |
| ACSL 2.3.3<br><sub>p.116</sub> | ACSL User | View the details of a report | Viewable | **FIT** | On-screen | Re-use | `platform (report)` | S |  |
| ACSL 2.3.4<br><sub>p.116</sub> | ACSL User | Download single and multiple reports | Multi download | **CHANGE** | One at a time | Report batch | `platform (report)` | S |  |
| ACSL 2.3.5<br><sub>p.116</sub> | ACSL User | Print single and multiple reports | Multi print | **CHANGE** | One at a time | Report batch (merged PDF) | `platform (report)` | S |  |
| ACSL 2.3.6<br><sub>p.116</sub> | ACSL User | Save single and multiple reports | Multi save | **CHANGE** | One at a time | Report batch (ZIP) | `platform (report)` | S |  |
| ACSL 2.4.0<br><sub>p.117; Add.1 p.33-34</sub> | ACSL User | Upload files (statement of account) that trigger reconciliation. **Addendum 1:** SOA reconciliation report generated; log of uploaded transactions proving every row was loaded | Upload triggers recon; upload log | **NEW** | Bulk framework with per-row outcome and job audit (`bulk/**`) | Handler `ACSL_INSURER_SOA` (layout per insurer), `acsl_soa_upload` + lines, upload log report `ACSL-SOA-UPLOAD-LOG`, recon run after load | `acsl` | M | AQ21 |
| ACSL 2.13.0<br><sub>p.120</sub> | System | Perform automatic reconciliation | Auto recon | **NEW** | Production reconciliation (prodrecon, planned) matches production registers, not SOA balances | `acsl` reconciliation engine (SOA and GL-SL) | `acsl` | M |  |
| ACSL 2.13.1<br><sub>p.121</sub> | System | Reconcile the uploaded insurer SOA against booked transactions using the invoice number | Matched per invoice | **NEW** | Invoice ledger with components, remitted, cancelled, direct-billed flags (`opsledger/domain/OpsInvoice.java`) | Match SOA line to `ops_invoice` (and family, ACSL 2.16.0) by invoice no.; classify Outstanding / For remittance / Remitted / Cancelled / Direct billed / Not found; variance | `acsl` | M | AQ21 |
| ACSL 2.13.2<br><sub>p.121</sub> | System | Reconcile GL and SL balances by GL code | GL vs SL per account | **CHANGE** | GL balances (`ledger/service/LedgerQueryService.java`), open-item balances per control account (`subledger/service/OpenItemService.java`) | Job `ACSL_GL_SL_RECON` and report `ACSL-GL-SL-RECON` (per control account: GL, SL, difference, drill-down); also a period-end check through `PeriodEndCheckProvider` | `acsl` | S |  |
| ACSL 2.14.0<br><sub>p.121</sub> | System | Generate a reconciliation report | Report generated | **NEW** | None | Report run at the end of each recon | `acsl` | S |  |
| ACSL 2.14.1<br><sub>p.122</sub> | System | SOA reconciliation report per invoice: Outstanding, For remittance, Remitted, Cancelled, Direct billed (indicator), SOA balance, Not found; file name "Insurer_Covered period" | Appendix C layout | **NEW** | None | `ACSL-SOA-RECON` (Appendix C "SOA reconciliation" fields incl. 2307 batch and remittance dates, cancellation reference tied to the original invoice) | `acsl` | M | AQ21 |
| ACSL 2.14.2<br><sub>p.123</sub> | System | List of all booked accounts with financial details for the chosen period; file name "List of all booked accounts_GL account name_period" | Appendix C layout | **NEW** | Invoice ledger search (`opsledger/service/LedgerSearch.java`) | `ACSL-BOOKED-FIN-DETAILS` with system references (version, cover, invoice), all payments and payment dates per invoice, direct-billed rows | `acsl` | M |  |
| ACSL 2.14.3<br><sub>p.124</sub> | System | Aging reports by posting date for the selected period: AR insurer's refund, AP refund from insurer, commission receivable (PHP / USD), payable to insurance company (PHP / USD), premium receivable (PHP / USD); GL balance and SL-GL difference | Separate files; buckets per Appendix C | **CHANGE** | Ageing service with up to five slots (`subledger/service/AgeingSlots.java` MAX_SLOTS = 5); debtors / creditors ageing reports | Raise `MAX_SLOTS` to 8; ACSL ageing reports on ops ledger / open items with the Appendix C buckets (0-30 ... over 730; premium receivable USD 8 buckets to over 5 years) and GL vs SL difference | `acsl (+ subledger)` | M | OQ43 |
| ACSL 2.14.4<br><sub>p.125</sub> | System | Schedule reports for the covered period (same five account families, PHP / USD) | Separate files | **NEW** | None | `ACSL-SCHED-*` reports (Appendix C "Schedule reports" fields: insurer, invoice, policy, assured, dates, co-insurance, share, segment, risk, account status, AO, unit head, client payment status, outstanding, ages) | `acsl` | M |  |

### P. ACSL: investigation, AR refund application, correction entries, remittance deduction

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| ACSL 2.5.0<br><sub>p.117</sub> | ACSL User | Perform account investigation: identify the status of the account and assess whether a correction is needed | Investigation | **CHANGE** | Invoice 360 with components, statuses, movements, receipts, batches, adjustments (`opsledger/service/Invoice360Service.java`) | Investigation case (`acsl_case`) opened from Invoice 360 with findings and decision (correction / no action) | `acsl` | S |  |
| ACSL 2.5.5<br><sub>p.117; Add.1 p.34</sub> | ACSL User | Receive account analysis requests (e.g. refund requests for cancelled accounts) (printed as a first "2.5.4"; renumbered 2.5.5 by Addendum 1) | Requests received | **NEW** | None | Case type ANALYSIS_REQUEST created by `payrequest` (MKT 1.11.0) or manually | `acsl` | S |  |
| ACSL 2.5.1<br><sub>p.117</sub> | ACSL User | Search by invoice no., policy no., assured name, inception date, account officer | Five search keys | **CHANGE** | Ledger search by invoice, ARN, policy, client, insurer, status (`opsledger/api/dto/LedgerSearchParams.java`) | Add assured name (trigram), inception date and AO | `opsledger` | S |  |
| ACSL 2.5.2<br><sub>p.118</sub> | ACSL User | Select a transaction | Selectable | **FIT** | Ledger list | Re-use | `opsledger` | S |  |
| ACSL 2.5.3<br><sub>p.118</sub> | ACSL User | View the transaction with all related transactions (common invoice number: adjustment, cancellation, regular booking) | Related transactions visible | **CHANGE** (built A1-OPSX, design §17) | Invoice 360 per invoice; related endorsements via parent link | Family view on `root_invoice_no` (DIS 3.27.2) | `opsledger` | S | AQ29 |
| ACSL 2.5.4<br><sub>p.118</sub> | ACSL User | Provide the result of the investigation to the requestor | Result sent | **NEW** | Notifications / messaging | Case result + notification to the requestor; `payrequest` validation task closed | `acsl` | S |  |
| ACSL 2.6.0<br><sub>p.118</sub> | ACSL User | Perform AR (receivable) refund payment application | Application | **CHANGE** | Planned cashiering application / re-application and `PaymentReapplier` (OPERATIONS_DESIGN 4.2; default adapter `opsledger/service/adapter/LedgerPaymentReapplier.java`) | ACSL action on the case calling cashiering application services (permission `ACSL_APPLY`) | `cashiering (+ acsl)` | M |  |
| ACSL 2.6.1<br><sub>p.118</sub> | ACSL User | Initiate the automatic SL payment reversal entry and route it for review and approval | Reversal routed | **CHANGE** | Planned cashiering payment reversal (CSH-PAYMENT-REVERSAL) | Reversal request on the case -> approval -> `PaymentReapplier.unapply` (entries by the cashiering events) | `cashiering (+ acsl)` | M |  |
| ACSL 2.6.2<br><sub>p.119</sub> | ACSL User | Coordinate short or over payment with the Account Officer | AO informed | **CHANGE** | Messaging and notifications | Case message to the AO of the invoice with reply thread | `acsl` | S |  |
| ACSL 2.7.0<br><sub>p.119</sub> | ACSL User (TL) | Assign journal-entry creation to a user | Assigned | **CHANGE** | `WorkAssignmentService` | Correction case assignment | `acsl` | S |  |
| ACSL 2.8.0<br><sub>p.119</sub> | ACSL User (TL) | Re-assign journal-entry creation | Re-assigned | **CHANGE** | `WorkAssignmentService` | As ACSL 2.7.0 | `acsl` | S |  |
| ACSL 2.9.0<br><sub>p.119</sub> | ACSL User | Create a correction journal entry assigned to me and route it for review and approval | Correction routed | **CHANGE** | Manual journals cannot post to control accounts with sub-ledger (`allowManualPosting`), no link to invoices | Correction entry (`acsl_correction` + lines with party and invoice) built as an ADJUSTMENT journal that records / matches open items and ops-ledger CORRECTION movements on posting | `acsl (+ journal, opsledger)` | M | AQ22 |
| ACSL 2.9.1<br><sub>Add.2 p.12-13</sub> | System | Controlled correction of postings to a wrong GL account: authorised users create a correcting entry linked to the original invoice and transaction; both visible; action logged | No overwrite; linked; audit | **CHANGE** | Posted journals are immutable and reversed, not edited (`journal/domain/JournalBatch.java`) | Correction "wrong account": system proposes reversal of the original line + re-post to the right account, linked (`corrects_batch_id`, `root_invoice_no`) | `acsl (+ journal)` | M | AQ22 |
| ACSL 2.9.2<br><sub>Add.2 p.13</sub> | ACSL User | Perform a remittance deduction upon insurer confirmation: only after the insurer confirmed with supporting documents; amount applied to the outstanding remittance balance with journal entries; blocked above the outstanding balance; logged | As stated | **CHANGE** (built A1-OPSX, design §17) | Remittance batches with net due and statuses (`remittance/domain/RemittanceBatch.java`); AR Insurer set-up after remitted decreases (`adjustment`, event `OPS_AR_INSURER_SETUP`) | `rem_deduction` (insurer, source AR Insurer / refund item, amount, documents, CONFIRMED by ACSL) consumed by the next batch; event `OPS_REMIT_DEDUCTION` (Dr due to insurer / Cr AR insurer's refund); cap at net due | `remittance` | M | AQ23 |
| ACSL 2.10.0<br><sub>p.119</sub> | ACSL User (TL) | Review the journal entry and endorse it for approval | Reviewed | **CHANGE** | Journal maker-checker is one step | Workflow `ACSL_CORRECTION` DRAFT -> FOR_REVIEW (TL) -> FOR_APPROVAL (Head) -> POSTED | `acsl` | S |  |
| ACSL 2.11.0<br><sub>p.120</sub> | ACSL User (Approver) | Approve / decline journal entries | Decision | **FIT** | Journal authorization with limits and segregation (`journal/service/JournalAuthorizationService.java`) | Final step of `ACSL_CORRECTION` | `acsl` | S |  |
| ACSL 2.11.1<br><sub>p.120</sub> | ACSL User (Approver) | Add a comment | Comment | **FIT** | Reasons / case notes | Re-use | `acsl` | S |  |
| ACSL 2.11.2<br><sub>p.120</sub> | ACSL User (Approver) | Save the comment | Saved | **FIT** | As ACSL 2.11.1 | Re-use | `acsl` | S |  |
| ACSL 2.12.0<br><sub>p.120</sub> | ACSL User (Approver) | Return the journal entry to the requestor | Returned | **CHANGE** | Reject / return on journals | Return transitions of `ACSL_CORRECTION` | `acsl` | S |  |
| ACSL 2.12.1<br><sub>p.120</sub> | ACSL User (Approver) | Add a comment | Comment | **FIT** | Reasons | Re-use | `acsl` | S |  |
| ACSL 2.12.2<br><sub>p.120</sub> | ACSL User (Approver) | Save the comment | Saved | **FIT** | Reasons | Re-use | `acsl` | S |  |
| ACSL 2.15.0<br><sub>p.125</sub> | System | Post journal entries automatically upon approval | Auto posting | **FIT** | Approval posts in the same transaction | Re-use | `platform (journal)` | S |  |
| ACSL 2.16.0<br><sub>p.126; Add.1 p.34</sub> | System | Link related transactions to a single invoice number and track them per invoice per insurer | Viewable per invoice and / or per insurer | **CHANGE** (built A1-OPSX, design §17) | As DIS 3.27.2 | As DIS 3.27.2 plus insurer share rows (`ops_invoice_share`) | `booking, opsledger` | S | AQ29 |

### Q. Business and system administration

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BASAU 2.2.0<br><sub>p.128</sub> | Business Admin | Maintain LOVs after approval | Maker-checker LOVs | **FIT** | LOV types / values with effectivity and approval (`lov/service/LovService.java`, `LovApprovalSource.java`) | New LOV types of this BRD (design section 10) | `platform (lov)` | S |  |
| BASAU 2.2.1<br><sub>p.128</sub> | Business Admin | View defined LOVs | View | **FIT** | LOV screen | Re-use | `platform (lov)` | S |  |
| BASAU 2.2.2<br><sub>p.128</sub> | Business Admin | Add new LOVs | Add | **FIT** | LOV screen | Re-use | `platform (lov)` | S |  |
| BASAU 2.2.3<br><sub>p.128</sub> | Business Admin | Edit defined LOVs | Edit | **FIT** | LOV screen | Re-use | `platform (lov)` | S |  |
| BASAU 2.2.4<br><sub>p.128</sub> | Business Admin | Deactivate defined LOVs | Deactivate | **FIT** | Effective-to date | Re-use | `platform (lov)` | S |  |
| BASAU 2.2.5<br><sub>p.129</sub> | Business Admin | Select an effectivity date | Effectivity | **FIT** | `effectiveFrom` / `effectiveTo` (`lov/domain/LovValue.java`) | Re-use | `platform (lov)` | S |  |
| BASAU 2.3.0<br><sub>p.129</sub> | System Admin | Manage users per the access matrix; requests go through approval | Maker-checker user admin | **FIT** | Access requests CREATE_USER / MODIFY_ROLES / DISABLE_USER with approval (`nbadmin/service/AccessRequestService.java`) | Re-use | `platform (nbadmin)` | S | AQ28 |
| BASAU 2.3.1<br><sub>p.129</sub> | System Admin | Define user profile / role | Profiles | **FIT** | Users and roles | Re-use | `platform (security)` | S |  |
| BASAU 2.3.2<br><sub>p.129</sub> | System Admin | Assign specific functionality to a profile / role | Role permissions | **CHANGE** | Role permissions changed directly (`security/service/UserAdminService.updateRole`), no checker | Access-request type MODIFY_ROLE_PERMISSIONS (already proposed for PMADD05) | `platform (nbadmin)` | S | PQ17 |
| BASAU 2.3.3<br><sub>p.129</sub> | System Admin | Define group profile membership | Groups | **FIT** | Roles act as groups; users hold several roles | Re-use | `platform (security)` | S |  |
| BASAU 2.4.0<br><sub>p.130</sub> | System Admin | Request approval of user management | Request | **FIT** | Access requests | Re-use | `platform (nbadmin)` | S |  |
| BASAU 2.4.1<br><sub>p.130</sub> | System Admin | Notification on approved / declined / returned requests | Notifications | **CHANGE** (built A1-GL, design §17) | Approved / rejected notified; no RETURNED status (`nbadmin/domain/AccessRequestStatus.java`) | Add RETURNED (editable, re-submittable) and its notification | `platform (nbadmin)` | S |  |
| BASAU 2.4.2<br><sub>p.130</sub> | System Admin | View the list of requests with status | List | **FIT** | Access request list | Re-use | `platform (nbadmin)` | S |  |
| BASAU 2.5.0<br><sub>p.130</sub> | Approver | Approve LOV maintenance and manage-user requests with remarks | Decision with remarks | **FIT** | Approval inbox | Re-use | `platform (approval)` | S |  |
| BASAU 2.5.1<br><sub>p.131</sub> | Approver | Notification for approval requests | Notified | **FIT** | Approval inbox counts and notifications | Re-use | `platform (approval)` | S |  |
| BASAU 2.5.2<br><sub>p.131</sub> | Approver | View the list of pending requests | List | **FIT** | Approval inbox | Re-use | `platform (approval)` | S |  |
| BASAU 2.5.3<br><sub>p.131</sub> | Approver | Select single or multiple requests | Multi-select | **CHANGE** (built A1-GL, design §17) | One at a time | Bulk approve in the inbox (FRBS 2.5.6) | `platform (approval)` | S |  |
| BASAU 2.6.0<br><sub>p.131</sub> | Approver | Return approval requests (manage users) | Return | **CHANGE** (built A1-GL, design §17) | As BASAU 2.4.1 | As BASAU 2.4.1 | `platform (nbadmin)` | S |  |
| BASAU 2.6.1<br><sub>p.131</sub> | Approver | Return requests | Return | **CHANGE** (built A1-GL, design §17) | As BASAU 2.4.1 | As BASAU 2.4.1 | `platform (nbadmin)` | S |  |
| BASAU 2.6.2<br><sub>p.131</sub> | Approver | Add remarks | Remarks | **FIT** | Reason payloads | Re-use | `platform (nbadmin)` | S |  |
| BASAU 2.6.3<br><sub>p.132</sub> | Approver | Save remarks | Saved | **FIT** | Reason payloads | Re-use | `platform (nbadmin)` | S |  |

## 5. Report catalogue (Appendices A-C)

The appendices name reports without IDs; they are covered by FRBS 3.2.0, DIS 3.28.x and ACSL 2.14.x and are listed here
by group. "Schedule engine" is the configurable account-schedule report proposed for `finreport` (design section 11):
one definition per schedule (accounts or report group, grouping by party / document / cost centre, optional ageing with up
to 8 buckets, optional lapsing columns, comparative periods), so that most schedules are configuration, not code.

### Appendix A - Comptrollership FRBS list of reports (pp.142-144, 138 reports)

| Group | Reports | Fit | Approach | Q |
|---|---|---|---|---|
| I. End-of-day (7) | Statement of Condition, Income Statement, Trial Balance, Journal Entries, Subsidiary Ledger, Cash Disbursements, Cash Receipts | FIT / CONFIGURE | `FIN-MIS-BS`, `FIN-MIS-IE`, `FIN-TB-MAIN`, `FIN-GL-DAYBOOK`, `FIN-GL-SUBLEDGER-LC`; cash books = day book filtered by journal types PAYMENT / RECEIPT (saved variants) | |
| II. GARD - BDO Unibank bank format (45) | 1-2 SOC / SIE bank format | CONFIGURE | Statement formats (`finreport/service/StatementFormatService.java`) with the BDO Unibank line structure | AQ05 |
| | 3-44 schedules (capital, RATA, operating expenses, cost-to-income, fees and commission expense, fines, FX gain / loss, other income, rent, reciprocal accounts, due from local banks, misc. assets / liabilities / income / expenses, accrued expenses and taxes, intangibles, R&M IT, cash, AP, IT capex / opex, training, lapsing of receivables / liabilities / resources, aging of loans and receivables) | CHANGE | Schedule engine definitions (one per report) | AQ05 |
| | DTA components, IFRS 16 reconciliation / entries / schedule, monthly rentals, OCI-NUGL ECL, NUG, provision and allowance movement | CHANGE | Schedule engine where the data is in the GL; lease (IFRS 16), DTA and ECL computations are not in BIBS: manual inputs or out of scope | AQ05 |
| | Land, building, LRI and FFE schedule; lapsing schedule of FFE and leasehold improvements | CONFIGURE | Fixed-asset module registers (`fixedasset/**`) | AQ05 |
| | 45 Variance analysis SIE month-on-month with commentary | CHANGE | Comparative statement format (current vs previous month) with a commentary column stored per account and period | |
| III. Subsidiaries accounting (14) | FS analysis SIE / SOC month-on-month, year-on-year, actual vs budget; NUG; RATA and depreciation; DFLB and off-book placements; PNA / NFS notes templates; loans and advances to associates; fines; reciprocal deposits | CHANGE | Comparative formats and budget vs actual (`budget/report/BudgetVsActualReport.java`); schedule engine; investment module for placements; notes templates as XLSX exports of schedules | AQ05 |
| IV. Schedules and aging (40) | AR overhead / officers / SSS / others / retirement / BIR tax credit / BIR on hand / BIR per insurer (with aging); FFE; leasehold improvements; prepaid insurance / other / taxes; AP officers / BDO Unibank / BDO loans / Mall Assurance / others current PHP and USD / NUBE / others non-current / trust (with aging); commission income; expense grouping; GAP report; SA and CA reconciliation; cash flow; checks and other cash items; returned checks; premiums receivable 2307; money placement; total deposits; DFLB savings; interest receivable and income (PHP / FX / TD); revaluation for the year | CHANGE | Schedule engine with ageing (AR / AP with party open items); 2307 schedules from the certificate register (`tax`) and cashiering 2307 tags; placements and interest from `investment`; bank reconciliations (FIT, `FIN-BRS-STMT`); revaluation (`GL-FXREV`, FIT); expense grouping = allocation by unit / cost centre (NEW in `frbs`); GAP and cash-flow reports NEW in `frbs` | AQ05 |
| V. Performance / Mancom (2) | Market performance summary (premium, commission vs budget per segment and location; month, YTD, YoY); branch production per BDO branch (detailed / summary) | NEW | `frbs` reports on the booked production (`booking`, `opsledger`) and a budget by segment (budget lines tagged with the segment dimension) | AQ05 |
| VI. Service fee (2) | Service fee report per market segment (summary, details); referrers' share 2.5% / 1% of fully paid commission net of taxes | NEW | `frbs` service-fee runs (FRBS 2.10.0) | AQ20 |
| VII. Government (28) | 2550-Q with RELIEF (SLS / SLP), 0619-E, 1601-EQ / QAP | FIT | `tax` returns and exports (`tax/service/TaxReturnService.java`, `BirExportService.java`) | |
| | 1604-E annual alphalist, MAP (monthly alphalist of payees), 2550-M | CHANGE | Annual / monthly variants of the QAP builder; 2550-M only if still required | AQ07 |
| | 0619-F / final withholding with alphalist, 1603 fringe benefit tax, 1702-Q / 1702 income tax, SAWT, Broker's Annual Statement of Business Operations (IC) | NEW | New tax worksheets and forms; SAWT from the received-certificate register; IC statement per line from the production register | AQ07 |
| | BIR books of accounts: General Journal, Purchase Journal, Sales Journal, Cash Receipts, Cash Disbursements, Subsidiary Ledgers (assets, liabilities, capital, contingent, income, expenses) | NEW | `tax` "books of accounts" pack generated from the ledger by journal type and account class (loose-leaf PDF / CAS export format) | AQ07 |
| | HDMF contributions and loans, SSS contributions and loans, PhilHealth, 1601-C, 1604-C | OUT | Payroll reports; BIBS has no payroll. 1601-C stays a reminder in the tax calendar | AQ06 |

### Appendix B - Disbursement reports (pp.145-148)

| Report | Code | Fit |
|---|---|---|
| Masterlist of all disbursement transactions | `DSB-MASTERLIST` | NEW |
| List of unreleased checks (ageing current-30 to 151-180, subtotals per category) | `DSB-UNRELEASED-CHECKS` | NEW |
| CWT / BIR 2307 on commission report (AR-BIR on commission and incentives vs certificate amounts, variances, per payee / insurer) | `DSB-CWT-COMMISSION` | NEW |
| Authority to Debit report | `DSB-ATD` | NEW |
| Miscellaneous Liability stale check report (ageing to 181 days and over) | `DSB-ML-STALE` | NEW |
| Cash flow report (amount per savings account, checks / ATD / CTA in process and for crediting, inter-office) | `DSB-CASH-FLOW` | NEW |
| Payee report | `DSB-PAYEE` | NEW |
| Disbursement request upload fall-out report | `DSB-UPLOAD-FALLOUT` | CHANGE (bulk error report) |
| Direct Credit Transaction text file | DCTF (`NotificationFormat.DCTF`) | CHANGE (payables notification file) |
| Remittance / Refund / Summary end-of-day (plus supplier, employee, other EOD per DIS 3.28.2) | `DSB-EOD-*` | NEW |

### Appendix C - ACSL reports (pp.149-151)

| Report | Code | Fit |
|---|---|---|
| Schedules: payable to insurance companies PHP / USD, AP refund from insurer, AR insurer's refund, commission receivables PHP / USD | `ACSL-SCHED-*` (6) | NEW |
| Aging: payable to insurance company PHP / USD, premium receivables PHP / USD, AP refund from insurer, AR insurer's refund, commission receivables PHP / USD (GL amount and SL-GL difference) | `ACSL-AGING-*` (8) | CHANGE |
| List of transactions financial details | `ACSL-BOOKED-FIN-DETAILS` | NEW |
| SOA reconciliation | `ACSL-SOA-RECON` | NEW |

## 6. Lists and formats from the BRD

### 6.1 Disbursement

| List | Values (as written) | Source |
|---|---|---|
| Payee classification | Supplier, Insurer, Employee, Client, Others (i.e. government agencies, etc.) | DIS 2.2.2 |
| Modes of payment | Credit to Account; Debit BDOIR Main Account (ATD); Manager's Check or Demand Draft; Credit Ticket; Telegraphic Transfer; Online Banking; Check | DIS 2.2.5, 2.7.0 |
| Disbursement type (manual encoding) | Remittance; Refund; Payment to Supplier; Employee-related request; Other disbursement requests | DIS 2.6.1 |
| Automatic classification | Remittance; Refund; Payment to supplier (other service provider); Payment to government agencies; Payment to other bank units | DIS 3.25.1 |
| Check statuses | Printed, Released, Negotiated (deposited-checks file), Staled (180 days from printed date if Printed or Released), Cancelled | DIS 2.8.1, 3.26.1-3.26.2 |
| ATD statuses | Printed (PDF generated), Emailed (sent to branch, requestor in copy), Debited (branch confirmation) | DIS 2.8.2, 3.26.3 |
| Credit to Account | Extracted (after processing), Credited (uploaded .txt of credited accounts) | DIS 3.26.4 |
| Credit Ticket / TT | Printed, Debited (branch confirmation) | DIS 2.8.3, 3.26.5 |
| MC / DD | Printed, Received (from branch), Released (to client) | DIS 2.8.4, 3.26.6 |
| Online Banking | Approved (after processing), Debited (BOB transaction fully approved, voucher reference) | DIS 3.26.7 |
| Transaction statuses | In Process, For review, Approved, Cancelled, Rejected, Returned | DIS 2.9.0, 2.18.0-2.21.0 |
| DCTF layout | Header: current date and file name; bank account number 12 numeric; payee 30 alphanumeric; 12 blank spaces; system reference number; amount `000000000000.00` | Appendix B p.147 |
| Stale check ageing (ML report) | current-30, 31-60, 61-90, 91-120, 121-150, 151-180, 181 and over | Appendix B p.145-146 |
| Withholding on remittance EOD | 2% and / or 15% | Appendix B p.148 |

### 6.2 Marketing forms (Appendix D, pp.152-153)

| Form | Fields |
|---|---|
| Request for Payment (RFP) | Type: Payroll, Cash Advance, Petty Cash, Others (specify), Remittance to insurance company, Remittance to government agencies; payee; amount (PHP / USD); purpose; mode of payment: Credit to CA/SA no., Check, ATD, Inter-Office, Manager's Check, Demand Draft; prepared / checked / approved by; Disbursement section: processed / checked / approved by, payment received by |
| Refund Request Form (RRF) | Segment, request date, reference (e.g. "2025_331 Refund"); lines: item, AR no., client no., assured / client name, amount, reason of refund, branch / unit, category A, category B, account name / check name; total; requesting unit and Disbursement processed / checked / approved by |
| Cash Advance Liquidation Form | Name, job level, date, from, purpose, cash advanced; lines: fieldwork date, particulars, per diem, representation, transportation, lodging, others, total; attachments (OR photocopies); over / (short); Finance: to Disbursement (checked and validated), to Cashier (excess money received, acknowledgement no.), to GL section (received for booking) |

### 6.3 ACSL

| List | Values | Source |
|---|---|---|
| Investigation search keys | Invoice number, policy number, assured's name, inception date, account officer | ACSL 2.5.1 |
| SOA reconciliation buckets per invoice | Outstanding; For remittance; Remitted; Cancelled; Direct billed (indicator); SOA balance; Not found | ACSL 2.14.1 |
| Aging buckets (most reports) | 0-30, 31-90, 91-180, 181-365, 366-730, over 730 days | Appendix C p.150 |
| Aging buckets (premium receivable USD) | below 90, 91-180, 181 days to 1 year, over 1 to 2 years, 2-3, 3-4, 4-5, over 5 years | Appendix C p.150 |
| Aging buckets (AR insurer's refund) | 0-30, 31-90, 91-180, 181-365, over 365 | Appendix C p.150 |
| Account status | New policy, Renewal, Direct billed, Adjustment, Cancellation | Appendix C p.149 |
| Client payment status | Full payment, Partial payment, No payment, Direct billed | Appendix C p.149 |
| File naming | "Name of Insurer_Covered Period (MM/DD/YYYY)"; "List of all booked accounts_GL account Name_covered period"; "Aging Report_Name of Account_Covered Period"; "Schedule Report_Name of Account_Covered Period" | ACSL 2.14.1-2.14.4 |

## 7. Non-functional requirements

| Topic | BRD | Approach | Fit |
|---|---|---|---|
| Users (p.133-137) | Accounting 5 (GL officer / TL / TH); Disbursement 8; ACSL 6; Marketing 394 named, 40 concurrent; Business / System Admin 7 | Within the sizing of BRD-1 (145 concurrent) | FIT |
| Volumes | Annual: remittance DVs 198 (+20% per year), refunds 246, supplier payments 293, government 9, other bank units 31, employee-related 56, CWT tagging 50; correcting entries 400; Marketing RFP / refund 302; manual adjustments 100; GL closings 6 | Small; no special tuning | FIT |
| Response time | Screen load 5-10 s, refresh 5 s, field display 2 s, save 5 s (2 s for some Disbursement and ACSL saves); login 5-10 s; reports 10-20 s first load, 10 s next; document upload / download 3 s per file | p95 < 3 s online; heavy reports asynchronous (report batch) | CONFIGURE |
| Peaks | Month end and year end; 10:00-15:00 (Accounting, ACSL, Marketing, admin), 08:00-12:00 (Disbursement) | Schedule EOD and recon jobs outside the peaks | CONFIGURE |
| Devices | Mobile and desktop expect the same performance | Responsive UI | FIT |
| Availability (main BRD) | "Follow existing QPS/EBIX setup" | - | - |
| Availability (**Addendum 1**, p.37) | 100%; use 07:00-18:00 Mon-Sat; downtime less than 24 h per month / year; maintenance 19:00-07:00; BCP threshold less than 3 days | HA deployment; 100% is not a measurable SLA (AQ27) | CONFIGURE |
| Retention (**Addendum 1**) | Reports / vouchers: 5 years online from creation, 5 years archive, daily backup, daily accessibility, backup retention 5 years; no anonymisation | `nbadmin` retention rules (BRNB.106) with a document class for vouchers and generated reports (`report_run`) | CONFIGURE |

## 8. Questions answered by this BRD

| Q# | Source | Question (short) | Answer from this BRD | Design impact |
|---|---|---|---|---|
| **OQ02** | Operations | What is "Disbursement"; payload of the push; how DV numbers and statuses return; may payables act as Disbursement? | **Answered.** Disbursement is a Comptrollership unit working **in the target system** (BIBS): it receives system-triggered requests with the RFP number (DIS 3.25.0), rejects requests without a maintained payee and notifies the requestor, creates the DV (number auto-generated, DIS 2.7.5) and an editable proforma entry (DIS 2.7.6); for refunds to clients and remittances to insurers from other BDOI units the DV is created automatically and goes straight to the approver. Statuses come back per instrument (Printed, Emailed, Extracted, Credited, Debited, Received, Released, Negotiated, Staled, Cancelled) and an e-mail confirmation with the remittance schedule goes to the recipient at end of day (DIS 2.7.12). Cancellation of an approved DV regularises remittance DTIP, refund ARs and ORs (DIS 2.20.0) | New module `disbursement` implements `DisbursementGateway` and replaces the in-app queue (design section 2). The queue's statuses map onto the new ones; `DisbursementStatusChanged` gains the DV statuses; remittance `DisbursementFeedback` treats CANCELLED like RETURNED. Payables masters (bank accounts, cheque books, notification files) are re-used; the payables payment voucher is not the BDOI payment document |
| **OQ01** | Operations | Which BRQID.004 systems are external (Collection, Accounting, Disbursement, Marketing, Claims)? | **Partially.** Accounting and Disbursement are **internal**: the GL is kept in the target system (FRBS 2.5.0, 3.1.0; "flow in to FRBS", DIS 3.27.1) and Disbursement runs in it. Collection, Marketing systems and Claims are not addressed; bank channels stay external (BOB, TPD / ACA, branches) | No `AccountingExport` adapter. Bank channels get a `BankChannelPort` seam (file / e-mail now) |
| **OQ07** | Operations | Real GL accounts and default entries (Operations section 5) | **Partially.** No chart and no entries are given; FRBS sets up / uploads the chart (FRBS 2.3.0-2.3.2). The BRD names BDOI's accounts through its reports: premium receivable PHP / USD, payable to insurance companies PHP / USD, commission receivables PHP / USD, AR insurer's refund, AP refund from insurer, AR-BIR on commission / on incentives / on hand / tax credit, premiums receivable 2307, AP others (unidentified or excess payments) PHP / USD, Miscellaneous Liability (stale checks), incentive income as Other Income (FRBS 3.1.2, DIS 3.29.2), commission income, service fee. Stale and negotiated checks need reversal entries (DIS 3.27.1) | Demo chart realigned to these names (design section 3); new events for Disbursement, ACSL and FRBS with demo rules; the real rules stay with Comptrollership (AQ02). The Operations accounts 1211, 1225, 2211, 2215, 2216 map to BRD accounts as in design section 3 |
| **OQ08** | Operations | Which exchange rate is the Comptrollership "BOOK" rate? | **Partially.** FRBS inputs a **revaluation rate monthly** and it is maintained in the system (FRBS 2.2.0, 3.6.0); USD items are revalued with it (FRBS 3.5.0). The BRD does not say that receipts use it | Proposal: BOOK rate of month M = revaluation (CLOSING) rate entered for the end of M-1, copied by a job (`OPS_BOOK_RATE_SOURCE`). Confirm (AQ03) |
| **OQ14** | Operations | AR Insurance definition and accounting | **Partially.** The ACSL schedules separate **AR insurer's refund** (return premium already remitted, receivable from the insurer) from **A/P refund from insurer** (refund received from the insurer, payable to the client) (Appendix C) | Operations 1225 AR Insurer = AR insurer's refund; add AP refund from insurer (demo 2217) used when the insurer pays the refund |
| **OQ15** | Operations | Disposition types, approvals and the refund hand-off | **Partially (refunds).** Refund to client: AO raises an RRF per AR number; for cancelled policies ACSL validates the cancelled premium and the insurer's return, Cashiering confirms the premium is back in the unapplied list with a new AR (MKT 1.11.0); Marketing reviewer and approver; automatic send to Disbursement (MKT 2.24.0); duplicate refunds blocked by AR number (MKT 2.23.0) | `payrequest` owns the refund request; cashiering's REFUND disposition creates or links the RRF instead of sending directly to Disbursement |
| **OQ16** | Operations | BIR 2307 flow (statuses, when Dr DTIP / Cr PR2307, who tags) | **Partially.** The SOA reconciliation shows, per remitted invoice, the "BIR 2307 amount", its "batch number" and "date remitted" (Appendix C p.151): client 2307s are remitted to insurers in batches with the remittance. Disbursement tags CWT received / released (DIS 2.11) | The Dr DTIP / Cr PR2307 offset posts when the 2307 batch is released with the remittance DV; batch number kept on the certificate tag |
| **OQ23** | Operations | Early remittance incentive: rates, deduction vs separate OR | **Partially.** After the early-incentive computation a **service invoice** is generated automatically with **2% withholding**, supporting OR issuance (DIS 3.29.1); early incentives are Other Income (FRBS 3.1.2). Rates are not given | Remittance issues the SI through booking's `ServiceInvoiceService`, then the OR; rule maps the incentive to Other Income |
| **OQ39**, **Q33**, **PQ04** | Operations / BRD-1 / BRD-3 | Incentive schemes; CPC2 definition | **Partially.** CPC2 = incentives earned by BDOI on **package Fire and Motor** products agreed with insurers on top of regular commission; percentage from the TSU-maintained product table; computed **per remittance** (not cumulative); recognised as incentive income separate from basic commission (DIS 3.29.2); CPC2 reports (DIS 3.29.0). No tiers or values | CPC2 computed in remittance from `cat_incentive_criteria` (BRD-3 design); No Touch / Top Up / Motor Mania still open |
| **OQ41** | Operations | CMRID.015 certificates are insurers' EWT certificates on commissions? | **Answered.** Disbursement receives the actual CWT "Received (for insurer)" and tags period, date and amount (DIS 2.11); the "CWT / BIR 2307 on commission" report compares AR-BIR on commission and on incentives with the certificate amounts per insurer (Appendix B) | One received-certificate register in `tax` shared by commission (CMRID.015) and disbursement; SAWT built from it |
| **OQ42** | Operations | Report layouts not given | **Partially** for Disbursement and ACSL reports (Appendices B, C). Operations report layouts are still open | - |
| **OQ43** | Operations | Ageing buckets | **Partially.** ACSL buckets 0-30 / 31-90 / 91-180 / 181-365 / 366-730 / over 730 (Appendix C); ML stale checks 30-day steps to 181+ | `AgeingSlots.MAX_SLOTS` raised to 8; propose the ACSL buckets as the BDOI default `AGEING_BUCKETS` |
| **OQ44** | Operations | NFR alignment | **Not resolved; a new variant.** Addendum 1: 100% availability, 07:00-18:00 Mon-Sat, maintenance 19:00-07:00, retention 5 + 5 years, backup retention 5 years | Recorded in AQ27 |
| **OQ48**, **PQ17** | Operations / BRD-3 | Access matrices | **Partially.** Role matrices for Accounting (Section Head / TL / Processor), Disbursement (Approver / TL / Processor), Marketing (Processor / Reviewer / Approver / HR) and ACSL (Approver / TL / Processor), read from the scanned copies (pp.181, 216-217, 227, 240) | Roles and grants in design section 6; final matrix to confirm (AQ28) |
| **Q08** | BRD-1 | Which BDOI systems receive NB data (core / accounting)? | **Partially.** Accounting is internal (see OQ01) | Booking posts to the BIBS GL only |
| **Q32** | BRD-1 | Booking accounting entries and service invoice rules | **Partially.** Related transactions (booking, adjustment, cancellation) must be linked to a **single invoice number** (ACSL 2.16.0, DIS 3.27.2; today "a different invoice number is generated for cancellations", p.45). No entries given | `root_invoice_no` on booking and the invoice ledger |

Not answered: OQ03-OQ06, OQ09-OQ13, OQ17-OQ22, OQ24-OQ38, OQ40, OQ45-OQ47, OQ49, OQ50 and the BRD-1 / BRD-3 questions not listed.

## 9. Open questions for BDOI (Accounting, Disbursement, ACSL)

| Q# | Topic | Question | Related |
|---|---|---|---|
| AQ01 | Chart of accounts | BDOI's current chart (EBIX / QPS) and its mapping; numbering convention of generated account numbers; meaning of "short code"; are PHP and USD kept as separate accounts (the schedules are per currency) or one account with currency sub-ledgers? | FRBS 2.3.x, OQ07 |
| AQ02 | Default entries | Entries per transaction: DV by disbursement type, check clearing (outstanding checks account?), stale checks to Miscellaneous Liability and re-issue, negotiated checks, cancellation of an approved DV, account funding, service fee accrual and payout, cash advance and liquidation, CPC2 and early incentive (with VAT / 2% WTAX), remittance deduction, ACSL corrections. Remainder of OQ07 | DIS 2.7.6, 3.27.x, OQ07 |
| AQ03 | Rates | Which rate is the monthly revaluation rate (BDO closing, BAP / BSP reference)? Is it also the BOOK rate of Operations receipts (OQ08)? FRBS 3.5.0 says "difference of the previous date versus the current date": daily or monthly revaluation? | FRBS 2.2.0, 3.5.0, OQ08 |
| AQ04 | Closing | Difference between "broking books" (closed automatically at month end at an agreed time) and "GL books" (closed by FRBS on the 2nd banking day, or on a preferred date per Addendum 1); the agreed time; may a closed month be reopened; why the year-end deadline is 15 April (audited FS / ITR) and how audit adjustments after that are posted | FRBS 2.6.x, 2.7.x, 3.4.x |
| AQ05 | Appendix A reports | Layouts of the GARD bank-format statements and schedules, subsidiaries reports and Mancom reports (only titles given); sources of data not in BIBS (IFRS 16 leases, DTA, ECL / NUGL, placements, payroll-related schedules); segment budgets for the market performance report | FRBS 3.2.0 |
| AQ06 | Payroll | HDMF, SSS, PhilHealth, 1601-C, 1604-C come from payroll: out of scope for BIBS, or imported? | Appendix A VII |
| AQ07 | BIR forms and books | Is 2550-M still required? Layout / channel of MAP, SAWT, 0619-F, 1603, 1702-Q / 1702, 1604-E; BIR books of accounts format (loose-leaf PDF, CAS registration, DAT files); IC Broker's Annual Statement layout | Appendix A VII |
| AQ08 | Bank reconciliation | Bank file layouts (.xlsx per bank); which accounts; matching of lines without check numbers; meaning of "reconcile cash balance per books with broking books" (FRBS 3.3.2) | FRBS 3.3.x |
| AQ09 | Bank channels | For each mode: manual at the branch, BDO BOB, or file (DCTF to TPD for ACA). Full DCTF specification (header / trailer / totals), credited-accounts .txt layout, deposited-checks file layout, BOB approval report; will any channel become an API? | DIS 2.7.x, 2.16.1, 2.22.0, 3.26.4, 3.26.7 |
| AQ10 | Account funding | "Main BDOIR China bank account": a China Banking Corp. account funded from BDO through BOB? Which accounts are main / source; are both approvers required and in order; limits | DIS 2.17.x |
| AQ11 | Payee master | Migration file from the current system; payee classes vs party types (employees, government); several bank accounts per payee; maker-checker on payees; "delete" vs deactivate | DIS 2.2.x |
| AQ12 | Requests | List of system-triggered request sources and the RFP number format; upload template columns | DIS 2.5.0, 3.25.0 |
| AQ13 | DV and proforma | DV number format; which proforma lines may be edited and whether an edited entry needs extra approval; is posting at approval or at release; EWT on supplier payments computed at the DV or at the invoice | DIS 2.7.5-2.7.6, 2.19.0, 3.27.0 |
| AQ14 | Checks | Check layouts per bank, signatories, voucher and form layouts (ATD, MC / DD, CT / TT); stale-check accounting and re-issue | DIS 2.16.2, 3.26.2, 3.27.1 |
| AQ15 | Status edits and cancellations | Who approves an edited status; can an approved DV be cancelled after release (check void vs stale); what "regularise the database" includes for each source | DIS 2.8.5, 2.20.0, MKT 1.19.0 |
| AQ16 | CWT | Confirm "Received (for insurer)" = insurer certificates on commission / incentives and "Released (for supplier)" = BDOI's 2307 to suppliers; relation to Cashiering's client 2307 flow and CMRID.015; period covered (quarter / month) | DIS 2.11.x, 3.28.3, OQ16, OQ41 |
| AQ17 | OR / AR tagging | Which DVs need an OR / AR back (insurers for remittances, suppliers, others)? Relation to the insurer OR upload of RMTID.012 | DIS 2.10.x |
| AQ18 | Marketing requests | Mandatory fields of RRF / RFP (MKT 1.10.0 list is empty); approval chain per segment; HR approver; is the cash-advance liquidation (Appendix D form) in scope and who posts it; "unapplied payment report" = Cashiering unapplied list? | MKT 1.x |
| AQ19 | CA/SA | Definition (BDO current / savings account of the client for refunds); several per client; validation against BDO | MKT 2.25.x |
| AQ20 | Service fee | Definition and computation (2.5% / 1% of fully paid commission, net of taxes, per segment); recipients (BDO branches / referrers); liquidation report content; who releases | FRBS 2.10.x, Appendix A VI |
| AQ21 | Insurer SOA | SOA layouts per insurer; do insurer SOAs carry BDOI's invoice number (matching key)? Direct-billed indicator source | ACSL 2.4.0, 2.13.1, 2.14.1 |
| AQ22 | Corrections | Which accounts ACSL may correct; effect on the sub-ledger and on invoice components; reviewer / approver; the "separate unit" that posts today | ACSL 2.9.x |
| AQ23 | Remittance deduction | What is deducted (AR insurer's refund, over-remittance, other) and against which batch; entries; can a deduction span batches | ACSL 2.9.2 |
| AQ24 | CPC2 | Percentage per product / insurer; base (premium or commission); deducted from the remittance or billed; VAT / WTAX; service invoice as for early incentives? | DIS 3.29.x, OQ39, PQ04 |
| AQ25 | Early incentive SI | SI series (BIR-registered), recipient (insurer), timing vs remittance; relation to the "incentive OR" already issued by remittance | DIS 3.29.1, OQ23 |
| AQ26 | Cost centres | Rules "when and how" per transaction type; is a cost centre an employee-level record (DIS 3.30.1 lists employee number, name, position) or a unit; headcount basis date | FRBS 3.1.1, DIS 3.30.x |
| AQ27 | NFR | Main BRD "follow QPS / EBIX" vs Addendum 1 (100%, 07:00-18:00, 5 + 5 years) vs BRD-1 and Operations values | NFR, OQ44 |
| AQ28 | Access | Confirm the role matrices per unit (a few ticks ambiguous in the scans); segregation (the verifier TL must differ from the maker) | all, OQ48 |
| AQ29 | Invoice number | Keep new numbers for endorsements / cancellations linked to a root invoice (our proposal) or reuse the same invoice number (BIR invoice uniqueness)? | DIS 3.27.2, ACSL 2.16.0 |
| AQ30 | Negative balances | Which accounts may never be negative; warning or block | FRBS 2.5.4, 2.8.4 |
| AQ31 | BDO Unibank reporting | Are GARD reports submitted as files to BDO Unibank (format) or as printed reports; reciprocal accounts with BDO | Appendix A II |

## 10. Observations on the BRD pack

- The PDF holds **four documents**: the Workshop Addendum (Apr-2026, pp.1-15, scanned landscape tables with one page upside
  down, p.13), a scanned signed copy of Addendum 1 (pp.16-27), Addendum 1 itself (pp.28-39) and the main BRD (pp.40-153)
  followed by its scanned signed copy (pp.154-267). The scans duplicate the digital text; no handwritten change was found
  on the sampled pages.
- The Workshop Addendum's title is "**Financial Reporting**, Disbursement and ACSL", and its purpose paragraph says
  "Business Requirements for **New Business**"; Addendum 1's approval text says "Business Requirements Document for the New
  Business Fire and Motor". Both look copied from other BRDs.
- **Duplicate IDs**: FRBS 3.6.0 (items p and q), DIS 2.17.3 (verify / approve; Addendum 1 renames the second 2.17.4),
  DIS 2.24.1 (add / tag status; renamed 2.24.2), ACSL 2.5.4 (receive requests / provide result; the first renamed 2.5.5),
  DIS 3.30.1 (maintain cost centre / headcount report; recorded here as 3.30.2). The generic access rows are all numbered
  "BRD 1.1.0-1.1.3" in the Accounting and Disbursement tables; they are recorded as FRBS 1.1.x and DIS 1.1.x.
- **Numbering gaps**: MKT jumps from 1.20.0 to 2.22.0 (no 2.21.0); the Workshop row read as "BRD DIS 3.29" is 3.29.1.
- The Business / System Admin table is headed "Accounting Controls and Subsidiary Ledger (ACSL) Unit Business
  Requirements" (p.127), a copy error.
- **MKT 1.10.0** ends with "populate the below details:" and no list; the RRF / RFP fields are only in Appendix D.
- Appendix C headers name the company "BDO Insurance and Brokerage, Inc."; Appendix D forms say "BDO Insurance Brokers,
  Inc."; elsewhere "BDO Insurance and Reinsurance Brokers, Inc." (BDOIR).
- **DIS 2.7.9** says "process Demand Draft / Manager's check as follows" but describes Credit Ticket / TT steps.
- **DIS 2.23.2** says "add edit series" for editing the beginning series.
- The key capabilities of Accounting (p.49) list 16 items; the role matrix covers only a-j. FRBS 3.x rows (k-q) are
  system requirements.
- **FRBS 2.6.0** (2nd banking day) is superseded by Addendum 1 (manual close on a preferred date and time in the current
  month); FRBS 2.6.1 still requires the date to be in the previous month.
- Appendix A section VII lists **2550-M** (monthly VAT declaration), which appears no longer required since the TRAIN law
  change to quarterly VAT filing from 2023; to confirm (AQ07). Payroll reports (HDMF, SSS, PhilHealth, 1601-C, 1604-C) are
  listed although BIBS has no payroll (AQ06).
- Appendix A's last group ends with an unlabelled line "Referrers share in commission earned by BDOI. Computed at 2.5% /
  1% of fully paid commission for a specified period and net of corresponding taxes" (p.144), which is taken as the
  definition of the **service fee** (FRBS 2.10.0).
- The "Main BDOIR **China** bank account" (DIS 2.17.x) is unusual for a BDO subsidiary; see AQ10.
- Main-BRD availability and retention say "Follow existing QPS / EBIX setup"; Addendum 1 then gives 100% availability,
  which is not measurable as an SLA (AQ27).
- The current process (p.45) confirms that "a different invoice number is generated for transaction cancellations" and
  that "posting of correction and reversal entries is handled by a separate unit"; both are target-state changes
  (ACSL 2.16.0, ACSL 2.9.x).
- Role matrices (pp.66-67, 102-103, 113, 126) lost their tick marks in the text layer; they were read from the scanned
  copies (pp.181, 216-217, 227, 240) and are reflected in the design roles. Examples: only the Section Head inputs the
  revaluation rate; only the Team Leader closes the month and the year; Processors make manual entries and monitor the
  service fee; in Disbursement the Approver maintains check series and bank accounts, approves, cancels approved items and
  approves funding, while the Team Leader creates and verifies funding requests. A few ticks are ambiguous (AQ28).
