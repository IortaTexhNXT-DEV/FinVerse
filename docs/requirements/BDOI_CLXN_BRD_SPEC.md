# BDOI Collections (BRD-4, CLXN) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse (BIBS).

Status: **for BDOI concurrence.** Build design: [`COLLECTIONS_DESIGN.md`](../architecture/COLLECTIONS_DESIGN.md). The review workbook is consolidated later with the other BRDs. This baseline answers several items that the Operations baseline ([`BDOI_OPS_BRD_SPEC.md`](BDOI_OPS_BRD_SPEC.md)) parked; see section 9.

## 1. Source documents

Source: `docs/source-documents/Collections (CLXN) BRD.pdf` (94 pages, about 14 MB). The file is a pack of five documents. Pages 13-22 and 62-93 are images (vector or scanned) and were read as rendered pages.

| Pages | Document | Content |
|---|---|---|
| 1-12 | Collections Addendum (Workshop), **draft**, v1.0 05-Apr-2026 / 10-Apr-2026 ("x of 14"; pages 13-14 of that draft are not in the pack) | Purpose, BRCLXN.049-**064** |
| 13-22 | Collections Addendum (Workshop), **signed** version ("x of 10"), prepared 10-13 Apr 2026, input 14-16 Apr 2026, reviewed 10-12 Apr, approved 16-17 Apr 2026 | Purpose, BRCLXN.049-**060** only; approval pages (p.21-22) |
| 23-34 | Renumbering addendum, 17-Dec-2025, approved 19-22 Dec 2025 (page header reads "CLAIMS") | Maps FRID-001..048 to BRCLXN.001..048 (p.26-33); approval (p.34) |
| 35-61 | Collection Management System (CMS) BRD v1, 17-Jan-2025 to 10-Mar-2025 | Introduction and terms (p.37-38), overview and objectives (p.39), current and target process (p.40-42), stakeholders and functions (p.43-46), FRID-001-048 (p.47-50), NFRs (p.51-58), reports (p.59-61) |
| 62-65 (scanned) | CMS BRD sign-off sheet, Feb 2025, and e-mail sign-offs, 3-4 Mar 2025 | Prepared, reviewed and validated signatures |
| 66-93 (scanned) | Signed printout of the CMS BRD | Same content as p.35-61, with ticks and initials; one handwritten edit to FRID-015 (p.78) and a handwritten **caveat** by the BDOI Operations Head (p.93) |
| 94 | Blank | - |

What each document governs:
- **BRCLXN.001-048** are the CMS FRIDs renumbered (p.26-33). The renumbering addendum is the governing wording. Where it differs from FRID text, the difference is noted on the row.
- **BRCLXN.049-060** come from the signed workshop addendum (p.13-20). They are baselined.
- **BRCLXN.061-064** appear **only in the unsigned draft** (p.8-12). They are analysed and designed, but they are marked *Draft* and their scope is confirmed through CQ01.
- The handwritten caveat on the CMS approval sheet (p.93), from the VP and Head of BDOI Operations: *"Providing the export & download list feature via the UI may lead to slowdown & performance issues down the line. These were not part of the original requirement as the primary intent is to provide a UI for users to provide & record their dispositions on PR + UPP."* The design keeps exports asynchronous and permission-gated (design section 11).
- The handwritten edit on FRID-015 (p.78) changes "update existing records from the EBIX to the system" to "update existing records **in the system from data in EBIX**".

## 2. Business context

BDOI Marketing collects premium receivables (PR) from clients through Account Officers (AO), Marketing Handlers and dedicated Collection Handlers, with Team Leads, Unit Heads (UH) and Section Heads (Corporate, Retail). Today the work runs on the legacy EBIX (booking and accounting) and QPS systems, on a "Marketing Diary" (the client's instruction on when, where, how and what to pay, p.38/40), on e-mail and on Excel lists.

Current process (p.40-41), step by step:
1. The AO records the client's payment instruction in the Marketing Diary.
2. The Collection Handler takes the outstanding-PR list for the assigned market segment and follows up each account as the diary says.
3. For newly booked accounts that are not yet paid, the handler collects within 10-15 days from booking.
4. The handler secures a payment commitment within 60 days.
5. If the commitment is outside the credit term, the handler informs the AO, who asks the insurer for a Credit Term Extension (CTE). If the CTE is refused, the AO escalates to the Unit Head.
6. On the 60th day from inception without a commitment, the handler sets a disposition: cancel the account, coordinate further, or ask the bank account officer for help.
7. The handler keeps the account's tagging category (lists A, B, C) and remarks up to date.
8. The handler follows the payment by channel:
   - deposit: check the payment status with Cashiering;
   - bills payment: use the Unapplied screen;
   - direct payment to the insurer: check with the insurer.
9. If the client pays net of 2% CWT, the tagging stays a *Marketing action* until the BIR 2307 is collected. The tagging becomes an *Operations action* once the BIR 2307 is received, the account is cancelled or the balance is minimal.
10. Operations extracts the Direct Payment Report every month as the basis for PR reversal.

Other handler tasks:
- facilitate cheque pick-up requested by the client;
- print BIR 2307 certificates received from the client by e-mail.

Target process (p.42):
- **System, premium receivable**:
  - generated PR list by market segment;
  - check the Marketing Diary through a web form;
  - PR disposition;
  - tagging category;
  - remarks;
  - Direct Payment report and PR2307 report as the basis for reversal;
  - Outstanding PR list.
- **System, unapplied payment**:
  - list by segment and disposition status;
  - disposition from a list of values (LOV);
  - remarks;
  - a file to the Automated Payment System.
- **Manual**: collection effort, commitment, CTE with the insurer, and escalation to the UH.
- **ISYS**: the AO updates the Marketing Diary.

The March 2026 workshop (addendum, p.13-20) adds the following:
- configurable escalation, and manual escalation;
- bulk update;
- reassignment;
- installment monitoring and promise-to-pay per installment;
- payment and transaction history, and the policy / invoice / co-insurance view;
- billing statements (SOA) per billing cycle of a multi-year policy;
- the rule that commission-receivable (CR) billing needs Premium Receivable confirmation that the premium is fully paid.

The unsigned draft adds four items: separate regular commission and incentive receivables, campaign-based incentive billing, refunds from negative adjustments, and the collectible commission on mixed payments.

**Position in BIBS.** The CMS BRD describes a separate application that is fed nightly from EBIX / QPS and that hands files to the legacy Automated Payment Processing program. In BIBS the booked invoice and its PR balance by component already live in BrokerVerse:
- BRD-1 `booking` books the invoice;
- the Operations `opsledger` keeps the balances;
- Operations `cashiering` receives payments, applies them and holds the unapplied items.

Collections is therefore a **BrokerVerse module** that reads the invoice ledger and hands work to Cashiering and Commission inside the application. The EBIX extraction becomes an in-app refresh, and the "file to the Automated Payment System" becomes an in-app application request, while the audit file is still produced.

## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 5 |
| CONFIGURE | Set-up only | 8 |
| CHANGE | Extend an existing capability | 21 |
| NEW | New build | 30 |
| OUT | Out of scope per BRD | 0 |
| **Total** | | **64** |

"Existing" means code that is already built. Items that the Operations design specifies but that are not built yet (Cashiering, Commission) count as NEW, with the dependency named in the row.

### Rows per section and fit

| Section | Name | Rows | FIT | CONFIGURE | CHANGE | NEW | Effort S/M/L |
|---|---|---|---|---|---|---|---|
| A | Outstanding PR list, balance and threshold | 13 | 2 | 2 | 8 | 1 | 11/2/0 |
| B | Daily refresh from the booking source (EBIX in the BRD) | 3 | 0 | 0 | 3 | 0 | 2/1/0 |
| C | Collector dispositions, user tracking and audit | 10 | 3 | 3 | 2 | 2 | 9/1/0 |
| D | Scheduled files and reports | 7 | 0 | 0 | 4 | 3 | 4/3/0 |
| E | Unapplied payment management | 15 | 0 | 3 | 0 | 12 | 11/4/0 |
| F | Collection management (workshop addendum, signed) | 11 | 0 | 0 | 3 | 8 | 2/6/3 |
| G | Commission receivable and incentives (059 signed; 061-064 draft) | 5 | 0 | 0 | 1 | 4 | 0/4/1 |
| **Total** | | **64** | **5** | **8** | **21** | **30** | **39/21/4** |

Signed and baselined: 60 rows (BRCLXN.001-060). Draft only: 4 rows (BRCLXN.061-064, CQ01).

### Target modules

| Module | Rows |
|---|---|
| `collections` (new) | 55 |
| `collections` + `cashiering` (Cashiering owns the unapplied item and executes the request) | 4 |
| `commission` (Operations, extended) | 5 |

### Platform impact

| Area | Treatment | Impact |
|---|---|---|
| Invoice ledger (`opsledger`) | Re-use (read) | The PR list, balances, movements, co-insurance shares and Invoice 360 come from the ledger. Collections posts nothing to it |
| Ports (`opsledger.service.port`) | Implement + extend | Collections implements `CollectionFeed` in-app. Two ports are added for Cashiering's unapplied items. `CollectionFeed` gets an `acknowledge` default method |
| Cashiering (being built) | Contract | Cashiering exposes its unapplied items read-only, accepts Collections disposition requests (apply to invoice, refund, check pick-up), and pulls 2307 tags and pick-up requests from Collections |
| Commission (being built) | Contract + scope | Commission pulls DP lists from Collections and sends returned DP accounts back. It adds the CR billing gate (059), and the collectible-portion, refund and incentive-campaign items (061-064, draft) |
| Workflow / alerts / messaging | Re-use | Escalation workflow, notifications, alert codes |
| Report / bulk / docgen | Re-use + small extension | About 14 reports, scheduled file generation with an availability time, a bulk update handler, and an SOA template |
| Audit | Extend | Field-level "from / to" change log for Collections records (BRCLXN.043) |
| Catalog (sales organisation) | Contract | Unit Head per sales unit (BRCLXN.011/012) |
| Security | Extend | Collections permissions and roles (p.43-46 functions) |

## 4. Collections flow (for concurrence)

| Step | Owner | What happens | BRD |
|---|---|---|---|
| Booking (BRD-1) | Processing | The invoice is booked; the PR by component, the DTIP, the commission and the co-insurance shares go to the invoice ledger | BRNB.027, opsledger |
| Nightly refresh | System, after the 22:00 EOD | New invoices with PR above the threshold become collection items. Existing items are refreshed; items at zero or below the threshold close with their history kept. Cancelled negatives are excluded | BRCLXN.001-015, 046 |
| Assignment | TL | Items are assigned to handlers by rule (segment, unit, client). Reassignment can be temporary or permanent | BRCLXN.052 |
| Follow-up | Collection Handler / AO | Collection efforts (code, remarks), promises per installment, collector disposition and category A/B/C, marketing-diary view | BRCLXN.016-023, 053, 055 |
| Escalation | System / handler / TL | Rules by aging, commitments and broken promises; manual escalation, including in bulk; TL / UH queue | BRCLXN.049, 050, 055 |
| Dispositions for Operations | Handler, then Cashiering / Commission | "PR 2307 for reversal" goes to Cashiering's 2307 intake. "DP PR for reversal" goes to Commission's DP list. "For check pick-up" goes to Cashiering's pick-up queue. Monthly and weekly files | BRCLXN.024-029, MKTID.010/012/013, CSHID.009/026 |
| Unapplied payments | Collection Handler / Unapplied Payment Handler, then Cashiering | Collector disposition on Cashiering's unapplied item. "For application to invoice" (valid invoice) becomes a Cashiering disposition request; a daily text file is produced for audit | BRCLXN.030-042, 047-048, CSHID.024 |
| Billing statements | Handler | SOA per billing cycle due of multi-year and installment policies | BRCLXN.058, 060 |
| Commission receivable | CRU (Commission) | CR billing only after PR confirms full payment. Collectible portion only. Refunds kept separate. Incentive campaigns | BRCLXN.059, 061-064 |

## 5. Requirements and fit/gap

Abbreviations: `opsledger` = `backend/src/main/java/com/iortatechnxt/brokerverse/opsledger`; the other module paths follow the same pattern. Page numbers refer to the PDF pack. For BRCLXN.001-048 the first page is the renumbering addendum and the second is the CMS BRD FRID table. For 049-060 the first page is the draft and the second is the signed addendum.

### A. Outstanding PR list, balance and threshold

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCLXN.001 (FRID-001)<br><sub>p.26 / p.47</sub> | Collection user | A module with UI and database to generate the list of accounts with outstanding PR at **invoice number** level (FRID-001 said "Cover Number level") | List generated at invoice level; one row per invoice with PR | **CHANGE** | `opsledger/service/InvoiceLedgerQueryService.search` + `LedgerSearch`; screen `frontend/src/features/operations/InvoiceSearchPage.tsx` (search, not a worklist) | Collection worklist `clx_item`, one row per ledger invoice with outstanding PR, fed by the nightly refresh; the PR Worklist screen groups by client / ARN / invoice | `collections` | M | CQ02 |
| BRCLXN.002 (FRID-002)<br><sub>p.26 / p.47</sub> | System | Access to the data and calculations that give the outstanding PR per account | Outstanding PR per account available | **FIT** | `opsledger/domain/OpsInvoiceComponent` (booked + adjusted - applied + reversed - remitted - written off, DB check), `PaymentStatus` | Read through `InvoiceLedgerQueryService`; nothing new | `collections` | S | |
| BRCLXN.003 (FRID-003)<br><sub>p.26 / p.47</sub> | System | Check and add the PR balance across invoices to determine whether there is outstanding PR | Balances summed across invoices of the account | **CHANGE** | Balances per invoice only | Aggregation per ARN and per client in the worklist (`clx_item` grouped; the account and client totals are shown and filterable) | `collections` | S | CQ02 |
| BRCLXN.004 (FRID-004)<br><sub>p.26 / p.47</sub> | System | Access to invoice data and calculations for the PR balance | As 002 | **FIT** | As 002; `Invoice360Service` | Re-use | `collections` | S | |
| BRCLXN.005 (FRID-005)<br><sub>p.26 / p.47</sub> | System | Use the minimal balance threshold to decide which accounts enter the outstanding PR list | Threshold applied by the list generation | **CHANGE** | `system/service/SystemParameterService` | Refresh rule: include when the net outstanding PR is greater than `CLX_MIN_BALANCE_THRESHOLD` | `collections` | S | CQ03, OQ11 |
| BRCLXN.006 (FRID-006)<br><sub>p.26 / p.47</sub> | System | Store the minimal balance threshold | Stored | **CONFIGURE** | `sys_parameter` | Parameter `CLX_MIN_BALANCE_THRESHOLD` (value to be confirmed, CQ03) | `collections` | S | CQ03 |
| BRCLXN.007 (FRID-007)<br><sub>p.26 / p.47</sub> | Authorised user (Section Head, App Support, p.44-45) | Authorised users update and maintain the threshold | Maintained by authorised users only | **CONFIGURE** | System parameter screen with permission | Grant `CLX_SETUP` to Section Heads and App Support; the change is audited (043) | `collections` | S | |
| BRCLXN.008 (FRID-008)<br><sub>p.26 / p.47</sub> | System | Exclude invoices with a sum of zero or below the threshold (not outstanding) | Excluded from the list | **CHANGE** | - | Refresh rule; an item that falls to or below the threshold closes as COMPLETED and keeps its history (022) | `collections` | S | |
| BRCLXN.009 (FRID-009)<br><sub>p.26 / p.47</sub> | System | Include invoices with a sum above the threshold | Included | **CHANGE** | - | Refresh rule (as 005) | `collections` | S | |
| BRCLXN.010 (FRID-010)<br><sub>p.27 / p.47</sub> | System | Exclude invoices with a negative balance and transaction type C (Cancelled) | Excluded | **CHANGE** | `OpsInvoice.kind`, flag `CANCELLED` (`InvoiceFlag`) | Refresh rule: a negative balance on a cancellation / return invoice (kind or flag CANCELLED) is never listed; other negatives go to the credit view (CQ04) | `collections` | S | CQ04 |
| BRCLXN.011 (FRID-011)<br><sub>p.27 / p.47</sub> | Collection user | Filter the list by market segment and by Unit Head | Filters available | **CHANGE** | `ops_invoice.segment`, `sales_unit`, `ao_username`; `catalog/domain/SalesUnit` has **no Unit Head** | Filters on segment, unit, UH, handler, AO; UH resolved from the sales unit's head (contract ask to catalog, design section 9) | `collections` | M | CQ05 |
| BRCLXN.012 (FRID-012)<br><sub>p.27 / p.47</sub> | System | Show only accounts of the selected segment and UH | Data scoping | **CHANGE** | Filters only | Server-side filter; also a default data scope per user (own segment / unit), confirmed in CQ06 | `collections` | S | CQ05, CQ06 |
| BRCLXN.046 (FRID-046)<br><sub>p.33 / p.50</sub> | System | Show the net outstanding PR and its breakdown (booked premium less negative endorsements, adjustments and cancellations, and payments) for unpaid or partially paid premiums | Breakdown shown | **NEW** | `OpsInvoiceComponent` (booked / adjusted / applied / reversed / remitted / written off), `OpsInvoiceAdjustmentTotal`, `OpsInvoiceMovement` | "Net PR breakdown" panel per item and per account: booked, negative endorsements / adjustments / cancellations (from the child return invoices of the same original), payments applied, write-offs, net; per component (DST, VAT, LGT, other, basic, PR2307) | `collections` | S | |

### B. Daily refresh from the booking source (EBIX in the BRD)

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCLXN.013 (FRID-013)<br><sub>p.27 / p.47</sub> | System | Daily batch that extracts all data from EBIX after the EOD financial run at 22:00 | Runs daily after EOD | **CHANGE** | `system/service/ManagedJob`, `JobScheduler`; `opsledger` fed by `InvoiceLedgerFeed` (booking event) and replay | EBIX is replaced by BrokerVerse booking. Job `CLX_DAILY_REFRESH` runs after the EOD (22:15 PHT) and reads the invoice ledger. Legacy EBIX open items at go-live are a one-time migration (CQ07) | `collections` | M | CQ07, OQ01 |
| BRCLXN.014 (FRID-014)<br><sub>p.27 / p.47</sub> | System | Daily upload of new records | New records created | **CHANGE** | As 013 | Same job: new ledger invoices become items; run log with counts (the "EBIX/QPS synchronization view" of App Support / DCO, p.45-46, is the job run history) | `collections` | S | |
| BRCLXN.015 (FRID-015)<br><sub>p.27 / p.47, edit p.78</sub> | System | Daily update of existing records in the system from the source data (handwritten wording p.78) | Existing records updated | **CHANGE** | As 013 | Same job updates balances, statuses and flags; balances are also refreshed on `InvoiceMovementPosted` (after commit) for same-day accuracy | `collections` | S | |

### C. Collector dispositions, user tracking and audit

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCLXN.016 (FRID-016)<br><sub>p.27 / p.47</sub> | System | Store the LOV of Premium Receivable Collector Dispositions | LOV stored | **CONFIGURE** | `lov/service/LovService` (types, values, effectivity, maker-checker `LovApprovalSource`) | LOV type `CLX_PR_DISPOSITION` with attributes (category A/B/C, tagging owner MARKETING / OPERATIONS, Operations action NONE / CWT2307_REVERSAL / DP_REVERSAL / CHECK_PICKUP / CANCEL_REQUEST); values from BDOI (CQ08) | `collections` | S | CQ08 |
| BRCLXN.017 (FRID-017)<br><sub>p.27 / p.47</sub> | Authorised user | Add and deactivate values of the collector disposition LOV | Maintenance by authorised users | **CONFIGURE** | LOV maintenance screen + `LOV_MANAGE` | Grant to Section Heads / App Support / Admin | `collections` | S | |
| BRCLXN.018 (FRID-018)<br><sub>p.28 / p.48</sub> | System | Deactivated values cannot be selected | Blocked | **FIT** | `LovService.activeValues`; validation of active values | Use active values in the pickers and validate on save | `collections` | S | |
| BRCLXN.019 (FRID-019)<br><sub>p.28 / p.48</sub> | System | Field showing who encoded and updated information | Shown | **FIT** | `BaseEntity` audit columns (created_by / updated_by / at) | Show "Encoded by / Updated by" on dispositions, efforts and promises | `collections` | S | |
| BRCLXN.020 (FRID-020)<br><sub>p.28 / p.48</sub> | System | Capture, store and display the user ID | As 019 | **FIT** | As 019 | As 019 | `collections` | S | |
| BRCLXN.021 (FRID-021)<br><sub>p.28 / p.48</sub> | System | Track and store the history of records and their disposition | History kept | **NEW** | - | `clx_disposition` is append-only (the current one is a pointer); `clx_effort` log | `collections` | S | |
| BRCLXN.022 (FRID-022)<br><sub>p.28 / p.48</sub> | System | History maintained even when the PR becomes zero or falls below the threshold | Retained | **NEW** | - | Items are never deleted: status COMPLETED (the "completed collections" list, p.43) | `collections` | S | |
| BRCLXN.023 (FRID-023)<br><sub>p.28 / p.48</sub> | System | History maintained regardless of PR changes | Retained | **CHANGE** | Ledger movements are immutable | As 021 / 022; each balance change is noted on the item timeline | `collections` | S | |
| BRCLXN.043 (FRID-043)<br><sub>p.32 / p.49</sub> | System | Audit log of user changes with field, "from" and "to" values and user ID | Field-level before / after | **CHANGE** | `audit/domain/AuditLog` keeps a summary only; `AuditTrailService.record` | `clx_field_change` (entity, id, field, from, to, user, timestamp, source IP) written by a Collections change recorder; also bulk updates (051) | `collections` | M | |
| BRCLXN.044 (FRID-044)<br><sub>p.32 / p.49</sub> | Authorised user | Extract or view the audit log on demand | View and export | **CONFIGURE** | `audit/api/AuditController` (list) | Report `CLX-AUDIT-LOG` (view / export permissions) over `clx_field_change` + `AuditLog` | `collections` | S | |

### D. Scheduled files and reports

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCLXN.024 (FRID-024)<br><sub>p.28 / p.48</sub> | System | Monthly batch generating the file of invoices with disposition "DP PR for reversal" (after EOD at 22:00) | File generated | **NEW** | `opsledger/service/ExtractRepositoryService`, `report/core/ReportService` | Report `CLX-DP-FOR-REVERSAL` (fields p.59) generated by job `CLX_MONTHLY_FILES` into the extract repository (folder `COLLECTIONS/DP_FOR_REVERSAL`). The same dispositions are placed on the `COLLECTION_DP_LIST` outbox for Commission (MKTID.012, CMRID.001) | `collections` | M | OQ38 |
| BRCLXN.025 (FRID-025)<br><sub>p.28 / p.48</sub> | Authorised user | DP PR for reversal file available every first working day, covering the previous month | Available on day 1 (working) | **CHANGE** | `organization/domain/Holiday` calendar | The job computes the first working day (holiday calendar) and publishes the file with `available_from`; the Files screen hides it until then | `collections` | S | |
| BRCLXN.026 (FRID-026)<br><sub>p.29 / p.48</sub> | System | Monthly batch generating the file of invoices with disposition "PR 2307 for reversal" | File generated | **NEW** | As 024 | Report `CLX-PR2307-FOR-REVERSAL` (p.59). The dispositions are placed on the `COLLECTION_CWT2307` outbox for Cashiering (CSHID.026/027, MKTID.013) | `collections` | M | OQ16 |
| BRCLXN.027 (FRID-027)<br><sub>p.29 / p.48</sub> | Authorised user | PR 2307 file available every first working day, covering the previous month | As 025 | **CHANGE** | As 025 | As 025 | `collections` | S | |
| BRCLXN.028 (FRID-028)<br><sub>p.29 / p.48</sub> | System | Weekly reports per unit per branch in Excel, Saturday to Friday, generated Friday after EOD | Weekly Excel per unit and branch | **NEW** | Report framework (Excel export) | Job `CLX_WEEKLY_FILES` (Friday 22:30 PHT): weekly "DP PR for reversal" and "PR 2307 for reversal" (p.59) split per unit and invoicing branch | `collections` | M | CQ09 |
| BRCLXN.029 (FRID-029)<br><sub>p.29 / p.48</sub> | Authorised user | View the weekly reports at 08:00 on the following Monday | Available Monday 08:00 | **CHANGE** | As 025 | `available_from` = next Monday 08:00 PHT | `collections` | S | |
| BRCLXN.045 (FRID-045)<br><sub>p.33 / p.50</sub> | System | Daily batch generating the PR report after EOD at 22:00 | Daily | **CHANGE** | `report/core/ReportArchiveService` (archive of runs) | Job `CLX_DAILY_FILES`: "Outstanding PR List" and "Full Production Report" (fields p.60-61) into the repository | `collections` | S | CQ09 |

### E. Unapplied payment management

Cashiering owns the unapplied item and its disposition workflow (CSHID.024/025, `OPS_DISPOSITION`, being built). Collections adds the collector-side disposition and the "request application" action. It reads the items through a port, so the two modules stay decoupled (design section 4.4).

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCLXN.030 (FRID-030)<br><sub>p.29 / p.48</sub> | Collection Handler | Request application of payment | Request recorded and routed | **NEW** | `opsledger/service/port/UnappliedSink` (create only); `csh_disposition` designed, not built | Action "Request application" -> port `UnappliedDispositionRequests.request(APPLY_TO_INVOICE, target invoice)` implemented by Cashiering (creates / updates a `csh_disposition` in MONITORING, source COLLECTIONS) | `collections` + `cashiering` | M | OQ15 |
| BRCLXN.031 (FRID-031)<br><sub>p.29 / p.48</sub> | Collection Handler | Document the disposition of the unapplied payment | Disposition stored with history | **NEW** | - | `clx_unapplied_disposition` (append-only) keyed by Cashiering's unapplied reference | `collections` | S | |
| BRCLXN.032 (FRID-032)<br><sub>p.29 / p.48</sub> | Unapplied Payment Handler | Request application of payment | As 030 | **NEW** | As 030 | As 030; role `UNAPPLIED_HANDLER` (CQ10: who this persona is) | `collections` + `cashiering` | S | CQ10 |
| BRCLXN.033 (FRID-033)<br><sub>p.29 / p.48</sub> | Unapplied Payment Handler | Document the disposition | As 031 | **NEW** | As 031 | As 031 | `collections` | S | CQ10 |
| BRCLXN.034 (FRID-034)<br><sub>p.30 / p.48</sub> | Collection user | UI and database for the list of unapplied payments as of the current system date | List as of today | **NEW** | - | Unapplied Payments (collector view): reads `UnappliedDirectory.open(companyId, filter)` (Cashiering) and joins the collector dispositions; age = today - payment date | `collections` + `cashiering` | M | |
| BRCLXN.035 (FRID-035)<br><sub>p.30 / p.48</sub> | Collection user | Filter by market segment and disposition status | Filters | **NEW** | - | Filters on segment (payment's matched client / invoice), collector disposition, Cashiering tab, age | `collections` | S | |
| BRCLXN.036 (FRID-036)<br><sub>p.31 / p.49</sub> | Collection user | List fields from the Payments Master List (date, payment file name, transaction no., amount, type, payor, bank code, check no., reference, client code match, assured, PR balance, inception, segment, business origin, UH, AO name, insurer name, processing stage, PN / loan application no., age, system remarks; UH / AO / bank officer as names; invoice number format; invoice category Regular / Direct Bill; **booker** name - added in the renumbering addendum) | All fields shown | **NEW** | Payment fields designed in `csh_payment` (OPERATIONS_DESIGN 4.2), not built | The `UnappliedDirectory` item carries the payment fields; the match fields (client, invoice, PR balance, segment, UH, AO, insurer, booker, invoice category = DP flag) are resolved from the ledger / account. "Processing stage" and "business origin" need definitions (CQ11) | `collections` + `cashiering` | M | CQ11 |
| BRCLXN.037 (FRID-037)<br><sub>p.32 / p.49</sub> | Authorised user | Add and deactivate values of the Unapplied Payment Disposition LOV | Maintained | **CONFIGURE** | LOV module | LOV type `CLX_UPP_DISPOSITION` with attributes: requires invoice (047/048), Cashiering action (APPLY_TO_INVOICE / REFUND / RECLASS / TRANSFER / NONE) mapped to Cashiering's `DISPOSITION_TYPE` | `collections` | S | CQ08, OQ15 |
| BRCLXN.038 (FRID-038)<br><sub>p.32 / p.49</sub> | System | Deactivated values not selectable | Blocked | **CONFIGURE** | As 018 | As 018 | `collections` | S | |
| BRCLXN.039 (FRID-039)<br><sub>p.32 / p.49</sub> | System | Store the LOV of Unapplied Payment Dispositions | Stored | **CONFIGURE** | As 016 | As 037 | `collections` | S | |
| BRCLXN.040 (FRID-040)<br><sub>p.32 / p.49</sub> | System | History of records and dispositions kept even after the payment is applied or refunded | Retained | **NEW** | - | Collector dispositions are never deleted. Cashiering's closure (applied / refunded) is shown from `UnappliedDirectory.history` | `collections` | S | |
| BRCLXN.041 (FRID-041)<br><sub>p.32 / p.49</sub> | System | Daily batch producing a **text** file of records with disposition "for application to invoice", loaded to the BDOI file server (FS04) before 06:00 the next day | Daily text file by 06:00 | **NEW** | `opsledger` `FileDropPort` (default: in-system repository; shared drive parked OQ17) | In BIBS the application happens in-app (030). The file is still produced for audit and legacy hand-off: job `CLX_APPLICATION_FILE` (05:00 PHT) writes "For Application To Invoice" (fields p.60, incl. user ID) through `FileDropPort` to location FS04 | `collections` | M | OQ17, CQ12 |
| BRCLXN.042 (FRID-042)<br><sub>p.32 / p.49</sub> | System | Load the auto-generated file to the BDOI file server | File on FS04 | **NEW** | `FileDropPort` / `RepositoryFileDrop` | As 041; the real FS04 transport stays parked (OQ17) | `collections` | S | OQ17 |
| BRCLXN.047 (FRID-047)<br><sub>p.33 / p.50</sub> | System | When "for application to invoice" is selected, validate that the invoice number is "I" followed by 8 digits | Format validated | **NEW** | BrokerVerse invoice numbers are `BI-<branch>-<yyyy>-...` (`booking/service/InvoiceBooker.java`) | Pattern parameter `CLX_INVOICE_NO_PATTERN` (default accepts both the EBIX `I\d{8}` and the BrokerVerse format) **plus** an existence check in the invoice ledger | `collections` | S | CQ13 |
| BRCLXN.048 (FRID-048)<br><sub>p.33 / p.50</sub> | System | No submission without a valid invoice number; the field is mandatory for "for application to invoice" | Mandatory | **NEW** | - | Front- and back-end validation driven by the LOV attribute "requires invoice" | `collections` | S | |

### F. Collection management (workshop addendum, signed)

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCLXN.049<br><sub>p.4 / p.16</sub> | Collection Team Lead | Configure escalation rules on aging, commitments and broken promises; accounts meeting the criteria are flagged automatically; actions auditable | Rules by days, conditions and thresholds; system flags and escalates; logged and reportable | **NEW** | `alert` checks (`AlertCheck`, `AlertDailyJob`), workflow SLA (`WorkSlaAlertCheck`) - fixed rules, not user-configurable | `clx_escalation_rule` (basis AGING_FROM_BOOKING / AGING_FROM_INCEPTION / NO_COMMITMENT_BY_DAY / BROKEN_PROMISES_COUNT / AMOUNT_OVER, filters segment / unit / product, target level TL / UH / SECTION_HEAD, notify); job `CLX_ESCALATION`; an escalation case in workflow `CLX_ESCALATION`; seeded examples 45 / 60 days and 60th day from inception (p.16, p.40) | `collections` | L | CQ14 |
| BRCLXN.050<br><sub>p.4 / p.16</sub> | Collection User | Escalate an account manually to the TL or a designated authority, including several invoices at once, regardless of the automated status | Multi-select; history per account; authorised users only; workflow / status transitions and notifications | **NEW** | `workflow/service/WorkflowService`, `messaging` `NotificationService` | "Escalate" bulk action -> one `CLX_ESCALATION` case per account with the selected invoices; target from the rule table or chosen; permission `CLX_ESCALATE`. The BRD gives no priority for this row | `collections` | M | |
| BRCLXN.051<br><sub>p.5 / p.17</sub> | Collection User | Bulk updates with audit traceability | Multi-invoice selection; required fields validated per record; audit of before / after values and affected records | **NEW** | `bulk.service` (upload handlers, row validation, outcomes) | In-grid bulk action (disposition, category, effort, promise date, remarks) with per-row validation and outcome list; bulk upload handler `CLX_BULK_UPDATE`; `clx_field_change` per record with a bulk reference | `collections` | M | |
| BRCLXN.052<br><sub>p.5 / p.17</sub> | Collection Team Lead | Reassign collection accounts to balance workload without losing accountability; temporary and permanent | Criteria (client, unit, aging, amount); temporary vs permanent; history logged and visible | **NEW** | `workflow/service/WorkAssignmentService` (per work case only) | `clx_assignment` (handler, PERMANENT / TEMPORARY with from-to, reason, by); `clx_assignment_rule` (default handler by segment / unit / client); "Reassign" by criteria or selection; automatic revert at the end of a temporary assignment (daily job) | `collections` | M | |
| BRCLXN.053<br><sub>p.5-6 / p.17-18</sub> | Collection User | Monitor installment-based premiums per due date | Installment schedule viewable per account; overdue installments flagged; available for follow-up and escalation | **NEW** | Multi-year: one invoice per policy year (`booking/domain/BookedInvoice.policyYear`, status `SCHEDULED`); **no installment schedule** in booking or the ledger | `clx_installment_plan` / `clx_installment` per invoice (frequency, due dates, amounts), created from the billing frequency (058) or entered; paid amounts allocated from the ledger's APPLIED movements by due date; overdue flag in the daily refresh | `collections` | L | CQ15, Q35 |
| BRCLXN.054<br><sub>p.6 / p.18</sub> | Collection User | Complete payment history and financial summary for installment and non-installment accounts | Chronological payments; outstanding and paid amounts | **CHANGE** | `InvoiceLedgerQueryService.movements`, `Invoice360Service` (RECEIPTS tab filled by Cashiering `InvoiceRelatedItems`) | "Payments" tab on the collection account: ledger APPLIED / reversal movements + receipt refs (AR no., date), per installment where a plan exists; summary booked / paid / outstanding | `collections` | M | |
| BRCLXN.055<br><sub>p.6 / p.18</sub> | Collection Team Lead | Detect broken payment promises per installment | Promised vs actual dates compared automatically; flags per installment; reportable and usable for escalation | **NEW** | - | `clx_promise` (item or installment, promised date, amount); job `CLX_PROMISE_CHECK` marks KEPT / PARTIALLY_KEPT / BROKEN against the applied payments; the broken count feeds 049; report `CLX-BROKEN-PROMISES` | `collections` | M | CQ16 |
| BRCLXN.056<br><sub>p.6-7 / p.18-19</sub> | Collection User | View policy, account, invoice and co-insurance information | Policy status, booking, delivery and receipt dates; client / account / invoice / co-insurance levels; read-only unless authorised | **CHANGE** | `Invoice360Service` (header, components, shares `ops_invoice_share`), `account` and `issuance` query services (policy, e-policy dispatch) | "Policy & Co-insurance" tab: read-only composition of Invoice 360 + account + issuance dispatch date; "receipt date" = first AR date from Cashiering (CQ17) | `collections` | M | CQ17 |
| BRCLXN.057<br><sub>p.7 / p.19</sub> | Collection User | Complete transaction history per account (payments, adjustments, collection actions) | Chronological; retained for audit | **CHANGE** | Ledger movements and status changes | Timeline merging ledger movements (APPLIED / ADJUSTED / WRITE_OFF / DP_REVERSAL / CWT_RECLASS) with Collections actions (efforts, dispositions, promises, escalations, assignments) | `collections` | S | |
| BRCLXN.058<br><sub>p.7 / p.19</sub> | Collection User | Generate billing statements per billing cycle due of a multi-year policy | Configurable billing frequencies; SOA per cycle with correct amounts and coverage period; amounts aligned to the policy period; multi-year and installment policies | **NEW** | `docgen` templates; multi-year SCHEDULED invoices (booking) | `clx_billing_statement` (SOA-<yyyy>) per client / account and cycle, from the installment plan or the policy-year invoices; LOV `CLX_BILLING_FREQUENCY` (annual, semi-annual, quarterly, monthly); SOA PDF / Excel through docgen; generating an SOA never creates a receivable (060) | `collections` | L | CQ15, CQ18 |
| BRCLXN.060<br><sub>p.8 / p.20</sub> | Collection User / Team Lead | See billing statements, invoice dates and policy periods for accurate aging, monitoring and escalation; billing supports monitoring only | Invoice refs, dates and amounts visible; supports aging and escalation; billing does not auto-initiate collection or CR | **NEW** | - | SOA list per account; aging basis parameter `CLX_AGING_BASIS` (BOOKING / INVOICE / DUE_DATE); rule: an SOA creates no collection item and no CR billing | `collections` | S | CQ14 |

### G. Commission receivable and incentives

BRCLXN.059 is in the signed addendum (persona "Collections / Commission User"). BRCLXN.061-064 are **draft only** (CQ01). The commission receivable unit (CRU) is the Operations Commission Receivables team, so these rows extend the Operations `commission` module (CMRID, being built) rather than Collections.

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCLXN.059<br><sub>p.7-8 / p.19-20</sub> | Collections / Commission User | Block CR billing unless Premium Receivable confirms the premium is fully paid | CR SOA blocked without PR confirmation; billing alone does not trigger CR; partial / adjusted balances remain visible (criteria numbered 1, 2, 5 in the source) | **CHANGE** | Designed in `commission` (`cmr_dp_item`, `OPS_DP_BILLING`), not built; the ledger has `PaymentStatus` and DP reversal movements | Gate in Commission: a CR billing line needs a `PremiumConfirmation` = ledger PR fully settled (paid to BDOI and applied, or DP reversal confirmed for direct-to-insurer). The confirmation is recorded with source and date; partial lines stay visible as "awaiting PR confirmation" | `commission` | M | OQ38 |
| BRCLXN.061 (draft)<br><sub>p.8-9</sub> | System | Regular commission and incentives as distinct, related receivable types, each with its own eligibility, billing and accounting | Regular commission collectible only when the premium is fully paid or confirmed; incentives are **Other Income**, generated only when eligibility is met; traceable at policy, invoice, insurer and transaction level; negative endorsements override original values | **NEW** | `OPS_INCENTIVE_ACCRUE` (Dr 1230 / Cr 4130) designed; booking commission (Dr 1220 / Cr 2220) | Receivable type on Commission items (REGULAR_COMMISSION / INCENTIVE); incentive receivables on 1230 Incentive Receivable with Cr 4130 as Other Income (rule by Comptrollership, OQ07); negative adjustment deltas (adjustment `CommissionAdjuster`) recompute both | `commission` | M | CQ01, OQ07 |
| BRCLXN.062 (draft)<br><sub>p.9-10</sub> | User | Incentives configured as campaigns with criteria; the system identifies qualified invoices and generates incentive billing | Campaign criteria: product / policy type, payment timing (X days from booking / inception), payment status, exclusions (pending negative adjustments); auto-exclusion of unresolved adjustments, cancellations and partial payments; system-generated service invoice with applicable taxes (WTAX); auditable link to campaign; no manual tagging | **NEW** | PM `cat_incentive_criteria` (designed, PMADD07/08); remittance `IncentiveRuleService` (early-remittance rule, built); booking `ServiceInvoiceService` (built) | `cmr_incentive_scheme` extended to campaigns (criteria reference `cat_incentive_criteria`, timing window and basis, payment-status and exclusion rules); campaign run over ledger flags (`PENDING_NEG_ADJ`, `CANCELLED`, `PaymentStatus`); billing through `ServiceInvoiceService` per insurer with WTAX; line keeps campaign + criteria snapshot | `commission` | L | CQ01, OQ23, OQ39, PQ04 |
| BRCLXN.063 (draft)<br><sub>p.10-11</sub> | User | Refunds from negative adjustments computed by the system and handled apart from collectible commission | Not CRU-collectible; commission recomputed on the final premium; reductions / reversals excluded from the CR SOA; refund transactions traceable to the original commission and adjustment; CRU dashboards and aging exclude refunds | **NEW** | `adjustment/service/CommissionAdjuster`, `Recompute` (built) post commission deltas per insurer | Commission subscribes to adjustment's commission deltas (`InvoiceMovementPosted` ADJUSTED on COMMISSION): a negative delta on a billed or collected line creates a `cmr_commission_refund` (reference to the original line and adjustment request), never a CR line; SOA and aging filter by type | `commission` | M | CQ01 |
| BRCLXN.064 (draft)<br><sub>p.11-12</sub> | User | Only the collectible commission portion for mixed payments (part to BDOI, part directly to the insurer) | Commission computed on the directly billed and paid portion after PR confirmation; excludes unpaid / unconfirmed; SOA includes only confirmed collectible portions; aging and escalation only on collectible amounts | **NEW** | Ledger `DP_REVERSAL` movement (per component, amount); `dp_flag` is per invoice only | Partial DP confirmation on a non-DP invoice (DP reversal movement for the portion paid directly to the insurer, confirmed by Collections disposition + Commission validation); collectible CR = commission x confirmed direct portion / gross premium; the rest is retained through remittance netting | `commission` | M | CQ01, CQ19 |

## 6. Capabilities shown in the process diagrams without an FR ID

These capabilities appear in the process diagrams and the stakeholder table only. They are **not** requirements, and they are listed only so that BDOI can confirm them. The proposed handling costs little because it reuses the entities above.

| Ref | Source | Capability | Proposed handling | Q |
|---|---|---|---|---|
| Marketing Diary | p.40-42 (target: "Check Web Form for Marketing Diary information"; ISYS box "Update Marketing Diary") | AO's record of the client's payment instruction: payment date and mode, commitment date, payment arrangement, method, contact, contact person, credit term | Read-only "Marketing Diary" panel on the collection account. The source is ISYS (link or feed, parked) or a Collections form if ISYS is not kept | CQ20 |
| Collection effort | p.43-46 ("manage collection effort transactions"), p.59 (Last Collection Effort Date / Code / Remarks) | Log of efforts with code and remarks | `clx_effort` with LOV `CLX_EFFORT_CODE` (needed by the reports anyway) | CQ08 |
| Tagging categories A / B / C and tagging owner | p.41, p.61 (CATEGORYA/B/C) | Category of the account and whether the next action is Marketing's or Operations' | Attributes of the collector disposition LOV (016) | CQ08 |
| Credit Term Extension | p.38, p.40-41 | CTE with the insurer, UH escalation | CTE status and date on the item (effort code "CTE requested / approved / refused") | CQ20 |
| Cheque pick-up | p.40 ("Facilitate cheque pick-up as requested by client") | Pick-up requests | Disposition action CHECK_PICKUP -> `COLLECTION_CHECK_PICKUP` outbox for Cashiering (CSHID.009) | OQ13 |
| Dashboard | p.43-46 (every role), p.61 ("Dashboard", no fields) | Role dashboard | Collections home tiles (design section 11) | CQ21 |
| Completed collections list | p.43-45 | Items closed as paid / below threshold | COMPLETED status view and export | |
| Processing Unit disposition on missing policy number | p.44 | Processing Unit role | Role `PROCESSOR` gets `CLX_WORK` limited to the disposition "No / missing policy number" | CQ08 |

## 7. Reports (p.59-61)

| Report | Frequency | Format | Fields (source) | Code |
|---|---|---|---|---|
| DP PR for Reversal | Monthly (first working day, prior month), weekly (Sat-Fri, Monday 08:00) | Excel | Booking category, client code, invoice no., currency, basic premium, PR2307 amount, total premium, basic commission, VAT and WTAX on commission, total commission, date tagged as DP or BIR submitted, aging, aging bracket, risk code, insurer, with / without co-insurance, co-insurance % share, UH, market segment, corporate department, last collection effort date / code / remarks, commission rate, premium and commission balances (basic, total, diff total premium balance less PR2307, VAT, WTAX, total), inception, expiry, invoice date, policy no., assured, AO, booker, collection handler, commission invoice no. / date / amount, CR team remarks, PR reversal date (p.59) | `CLX-DP-FOR-REVERSAL` |
| PR 2307 for Reversal | Monthly, weekly | Excel | Same field list (p.59) | `CLX-PR2307-FOR-REVERSAL` |
| For Application To Invoice | Daily (before 06:00) | Text | Payment date, payment file name, transaction no., paid amount, payment type, payor, reference no., assured, EBIX invoice no., user ID of the disposition (p.60) | `CLX-APPLICATION-TO-INVOICE` |
| Outstanding PR List | Daily | Excel | Client no. / name, cover no. / version, invoice no., booking date, age, booker, invoicing branch, QPS reference, BIR 2307 rate, assured, risk description, transaction type, insurer code, product code, inception / expiry, sum insured, currency, insurer proportion, basic premium / commission, booked premium, outstanding premium, policy no., marketing department code / description, business type, account type, customer segment, UH, AO, bank branch code / name, bank officer, payment arrangement, client payment status; per payment applied: amount, application date, AR no. / date; remittance no. / date / amount; refund amount / date (p.60) | `CLX-OUTSTANDING-PR` |
| Full Production Report | Daily after EOD 22:00 | Excel | Invoicing branch ... categories A / B / C, remarks (p.61, 51 columns) | `CLX-FULL-PRODUCTION` |
| Dashboard | - | - | Not specified (p.61) | Collections home (CQ21) |

Additional reports that the requirements imply: `CLX-UNAPPLIED-LIST` (034-036), `CLX-COMPLETED-COLLECTIONS` (p.43), `CLX-INVOICES-WITH-DISPOSITION` (p.43), `CLX-ESCALATIONS` (049), `CLX-BROKEN-PROMISES` (055), `CLX-INSTALLMENTS-DUE` (053), `CLX-REASSIGNMENTS` (052), `CLX-AUDIT-LOG` (044). Legacy-only fields (cover number / version, QPS reference, EBIX invoice number, BDOI SYS REF) are mapped in CQ22.

## 8. Non-functional requirements (p.51-58)

| Topic | BRD | Approach | Fit |
|---|---|---|---|
| Authentication | SSO with Windows credentials (employee ID); password rules for non-SSO (length 10 for user ID input, complexity, history 8, minimum age 1 day, 90-day change, minimum 8 / 12 for admins); lockout after **3** failed attempts; masked password; friendly errors ("Invalid User Name or Password", "User does not exist"); all valid and invalid attempts logged, visible to the system administrator only (NFR 1-2) | SSO stays parked with BRD-1 (Q42). The platform lockout is 5 attempts (`security/domain/AppUser.MAX_FAILED_ATTEMPTS`): make it a parameter and set it to 3 | CHANGE |
| Access control | RBAC, custom roles, user maintenance with lock / unlock; protection against IDOR; **a user may not hold several roles** (NFR 7) | RBAC and user admin exist. The platform allows several roles per user, so the single-role rule is a policy check (CQ23) | CHANGE |
| Audit logging | Login / logout, privileged use, role changes, admin actions, application start / stop / failures / configuration changes, audit-log access, record access and updates; timestamp, user, **source IP**, resource; exportable | Platform audit exists (`audit`); add source IP and resource to the Collections change log; audit export through `CLX-AUDIT-LOG` | CHANGE |
| Batch security | Restricted execution; audit of origin / time / trigger; encrypted transport; no hard-coded credentials | ManagedJob run history (trigger, user) and permission-gated manual runs | FIT |
| API / session | OAuth2 (internal financial APIs), mTLS public; session timeout 15 min, configurable in System Administration; one session per device (a new device logs off the old); HttpOnly / Secure cookies | JWT + `SESSION_TIMEOUT_MINUTES` parameter exists; single-device session is a platform change (CQ23) | CHANGE |
| UI / UX | Follow the BDOI application UI; copy / paste; smart search on all search fields; **record lock with the prompt "<Username> is editing"** | BDO UX guidelines (`docs/design/BDO_UX_GUIDELINES.md`); soft edit-lock on the collection account (`clx_item.editing_by / editing_since`, 15 min expiry) | CHANGE |
| Operating hours / locations | 06:00-22:00 Mon-Fri, and month-end weekends 06:00-22:00; HO Makati and Ortigas plus six provincial offices (Angeles, Cebu, CDO, Davao, General Santos) | Service hours are covered by the BRD-1 window; batch jobs after 22:00 | CONFIGURE |
| Performance | 130 users (56 concurrent); screens under 5 s; dispositions, applications and handler updates 1,200 / day each; daily report under 3 min; weekly under 10 min ("6,0000 tpw"); monthly 26,400 per month under 15 min; payment file input 1,200 / day; reversal of accounting transactions 12,000 / day under 20 min; daily sync 1,200 / day (NFR 15.01-15.14) | Worklist on indexed `clx_item`; exports and files asynchronous; the refresh is incremental | CONFIGURE |
| Scalability / availability | No downtime when the application scales; database scaling up to 120 min, manual; DR server yes; RTO 4 h, **RPO 4 h**; standard maintenance window | Same deployment as BRD-1 / BRD-2 | CONFIGURE |
| Retention | Logs, audit, historical data: 5 years online, 15 years archive, backup every 4 h, backup retention 5 years; hard-copy documents 5 / 10 years | Same framework as BRD-1 (BRNB.106); differs from Operations (7-year backup retention, OQ44) | CONFIGURE |
| Exports (caveat p.93) | The Operations Head warns that export and download from the UI may slow the system | Exports run as asynchronous jobs with row limits and a separate export permission | CONFIGURE |

## 9. Questions answered by this BRD

| Q# | Origin | Question (short) | Answer from the CLXN BRD | Design impact |
|---|---|---|---|---|
| OQ01 | Operations | Is "Collection" an external system or a later BrokerVerse module; direction, data, interface? | **Answered for Collection.** It is the Collection Management System, a module with its own UI and database for PR and unapplied-payment follow-up (p.37-39). It was fed from EBIX / QPS (FRID-013-015) and produces files for the Automated Payment System and FS04 (FRID-041/042). In BIBS, EBIX is replaced by booking / opsledger, so Collections is a **BrokerVerse module**. Accounting, Disbursement, Marketing (ISYS) and Claims remain open | `CollectionFeed` gets an in-app adapter in the new `collections` module (transport `IN_APP`). The `COLLECTION_*` feeds are fed from Collections dispositions instead of uploads (design 4.3) |
| OQ45 | Operations | Are the MKTID activities built in Operations or in a Marketing / Collection BRD? | **Partially answered.** The CLXN BRD covers PR follow-up and the dispositions "DP PR for reversal" and "PR 2307 for reversal" (MKTID.010/012/013 tagging) and unapplied-payment dispositions. It does **not** cover holds (MKTID.002-007), special remittance (009), the send-schedule request (001), the endorsement slip (008) or DP tagging at quotation (011) | MKTID.001-009 and 011 **stay in Operations as built** (remittance `HoldService`, `SpecialRemittanceService`, adjustment slip). MKTID.010/013 tagging moves to Collections dispositions; the Cashiering Marketing "BIR 2307 tagging" screen shrinks to a correction screen. MKTID.012 tag source = Collections |
| OQ13 | Operations | How does the "for check pick" status reach Operations? | **Partially.** Pick-up is a Collection Handler task (p.40) and there is no external system. The status mechanism is not specified | A disposition with action CHECK_PICKUP feeds `COLLECTION_CHECK_PICKUP` (in-app); whether the AR is printed at pick-up is still open |
| OQ16 | Operations | 2307 statuses, sorting, posting moment; does Marketing tagging stay in BrokerVerse? | **Partially.** Tagging stays in BIBS as the Collections disposition "PR 2307 for reversal", with monthly (first working day, prior month) and weekly files (FRID-026/027, p.59). The tag stays a Marketing action until the BIR 2307 is collected, then becomes an Operations action (p.41). Certificate statuses and the posting moment remain open | `COLLECTION_CWT2307` outbox items are created when the disposition's owner turns to OPERATIONS (certificate received) |
| OQ38 | Operations | DP list sources, naming, schedule; confirmation of full payment | **Partially.** The DP list is the Collections disposition "DP PR for reversal", monthly for the prior month on the first working day, and weekly per unit / branch (FRID-024/025/028). The fields include commission invoice data and CR remarks (p.59). Operations extracts the Direct Payment Report monthly as the basis for reversal (p.41). BRCLXN.059 requires PR confirmation before CR billing | `COLLECTION_DP_LIST` is fed in-app; branch file naming (CMRID.001) is no longer needed for Collections-sourced lists; the confirmation gate lives in Commission |
| OQ15 | Operations | Full disposition list, approvals, refund hand-off | **Partially.** The collector-side Unapplied Payment Disposition LOV is maintained by authorised users (FRID-037-039), and "for application to invoice" needs a valid invoice number (047/048). The values and the Cashiering-side approvals are still not given | `CLX_UPP_DISPOSITION` LOV with a mapping to Cashiering's `DISPOSITION_TYPE` |
| OQ17 | Operations | Shared drive: path, or is an in-system repository acceptable? | **Partially.** The BDOI file server **FS04** is the target for the daily application file, before 06:00 (FRID-041/042); reports go to a "Shared Drive" (NFR 15.06-15.08) | `FileDropPort` location FS04; transport still parked |
| OQ10 | Operations | 2% CWT: 98% application, PR2307 open until the certificate | **Partially confirmed.** "If payment is net of CWT payment, the tagging will remain as Marketing's Action until BIR2307 is collected" (p.41); reports carry "PR2307 Amount" and "Diff (Total Premium Balance less PR2307 Amount)" (p.59) | The PR2307 component stays open until the certificate; the Collections item shows net-of-CWT |
| OQ11 | Operations | Minimal balance rules | **Not answered for the GL rule.** Collections adds its own **list** threshold (FRID-005-009) and the tagging change on "Cancelled / Minimal Balance" (p.41) | `CLX_MIN_BALANCE_THRESHOLD` is separate from `MIN_BALANCE_AUTO_MAX` |
| OQ23 | Operations | Early remittance incentive: window basis, deduction or separate OR | **Partially (draft 062).** Payment timing is "within X days from booking / inception", configurable per campaign; incentive billing produces a service invoice with WTAX | Commission campaign type EARLY_PAYMENT; the relation to the remittance "With Incentives" batch (RMTID.023) is still open (CQ24) |
| OQ39 | Operations | Incentive schemes and payout | **Partially (draft 061/062).** Incentives are Other Income, campaign-based, with automatic qualification, a service invoice with WTAX and exclusions | `cmr_incentive_scheme` extended to campaigns |
| OQ07 | Operations | GL accounts and entries | **Partially (draft 061/063).** Incentives are Other Income; commission refunds are classified apart from CR | Seed rules only (design section 5) |
| OQ43 | Operations | Ageing buckets | **Not answered.** The reports carry "Aging" and "Aging Bracket" without buckets; the escalation examples are 45 / 60 days | Parameter `CLX_AGING_BRACKETS` (default 0-30, 31-45, 46-60, 61-90, 91-120, >120) |
| OQ48 | Operations | Access matrix | **Partially.** The CLXN stakeholder table gives the functions for AO / TL, Marketing Handler, Collection Handler, Section Heads, Processing Unit, Operations Cashiering, Disbursement, ACSL, Comptrollership, App Support, Admin and DCO (p.43-46). NFR: one role per user | Roles in design section 6 |
| OQ44 | Operations | NFR alignment | **Adds data:** CLXN gives 06:00-22:00 Mon-Fri, RPO 4 h, backup retention 5 years, response under 5 s | Kept in CQ25 |
| Q35 | New Business | Multi-year: premium upfront or yearly? | **Partially.** BRCLXN.058 bills per billing cycle due of a multi-year policy with configurable billing frequencies, which implies billing per cycle and not upfront | Installment plan / SOA per cycle; accounting per cycle stays open (CQ15) |
| PQ04 | Product Maintenance | Incentive criteria and their relation to the Operations schemes | **Partially (draft 062).** The campaign criteria include product / policy type, payment timing, payment status and exclusions | Campaigns reference `cat_incentive_criteria` |

Questions **not** answered by this BRD: OQ02 (Disbursement, a viewer-only stakeholder here), OQ24 / OQ25 (holds, special remittance), OQ46 (Claims), OQ03 (payment file layouts), OQ05-OQ09.

## 10. New open questions

| Q# | Topic | Question | Related |
|---|---|---|---|
| CQ01 | Draft scope | BRCLXN.061-064 are in the unsigned draft (p.8-12) and missing from the signed addendum (p.13-20), which ends at 060. Are they in scope? Who owns them (CRU / Commission vs Collections)? | BRCLXN.061-064 |
| CQ02 | List level | BRCLXN.001 says "invoice number level"; FRID-001 said "cover number level". Confirm the invoice level, and whether totals per ARN (account) or per client decide the threshold (003) | BRCLXN.001/003 |
| CQ03 | Threshold | Value of the minimal balance threshold; per currency? Compared with the total of the invoice or the account; net of PR2307? | BRCLXN.005-009 |
| CQ04 | Negatives | Treatment of negative balances that are not type C (credits from overpayment or endorsement): excluded from the list, or shown as a credit view? | BRCLXN.010 |
| CQ05 | Unit Head | Source of the Unit Head: the head of the sales unit in the sales organisation (proposal), or a separate master? Also bank branch / bank officer and "booker" as names | BRCLXN.011/012/036 |
| CQ06 | Data scope | Do handlers, AOs and UHs see only their segment / unit / assigned accounts, or everything with filters? | BRCLXN.012 |
| CQ07 | Go-live migration | Migration of open EBIX PRs, disposition history and unapplied items at go-live: scope, cut-off and format | BRCLXN.013-015, 022 |
| CQ08 | LOVs | Values of the PR collector dispositions (incl. "DP PR for reversal", "PR 2307 for reversal", category A / B / C, owner), unapplied-payment dispositions, collection effort codes | BRCLXN.016/037, p.41 |
| CQ09 | Report scope | "Weekly reports per unit per branch": which reports (p.59 lists DP PR and PR 2307 for reversal)? "PR Report" (045) = Outstanding PR List and / or Full Production Report? | BRCLXN.028/045 |
| CQ10 | Personas | Who is the "Unapplied Payment Handler" (Marketing, Cashiering or a separate team)? | BRCLXN.032/033 |
| CQ11 | Fields | Definitions of "Processing Stage", "Business Origin", "Client Code Match", "System Generated Remarks" in the unapplied list | BRCLXN.036 |
| CQ12 | Application file | With application done in-app, is the daily "For Application To Invoice" text file still needed (audit / legacy APS), and which layout? | BRCLXN.041/042 |
| CQ13 | Invoice number | BIBS invoice numbers differ from EBIX `I########`. Should the validation accept only BIBS numbers after go-live, or both during migration? | BRCLXN.047 |
| CQ14 | Escalation | Default rules and targets (45 / 60 days, broken-promise count), levels (TL, UH, Section Head), notification channel; aging basis (booking, invoice, due date) | BRCLXN.049/050/060 |
| CQ15 | Installments | Where do installment terms come from (quotation, account payment arrangement, Collections entry)? Is the PR booked in full at inception (current booking) with installments as a collection schedule only, or booked per installment? | BRCLXN.053/058, Q35 |
| CQ16 | Promises | Is a promise kept when paid on or before the date in full; grace days; partial payment | BRCLXN.055 |
| CQ17 | Dates | "Delivery date" = e-policy dispatch or physical delivery? "Receipt date" = client's receipt of the policy or the AR date? | BRCLXN.056 |
| CQ18 | SOA | SOA layout, recipient (client, bank), numbering, sending by e-mail; frequencies to seed | BRCLXN.058 |
| CQ19 | Mixed payments | How is a partial direct-to-insurer payment on a non-DP invoice evidenced (insurer confirmation, production report)? | BRCLXN.064 |
| CQ20 | Marketing Diary / CTE | Is the Marketing Diary kept in ISYS (then interface or link) or moved into BIBS? Are CTE requests tracked in BIBS? | p.38-42 |
| CQ21 | Dashboard | Dashboard content per role (p.61 blank) | p.43-46, 61 |
| CQ22 | Legacy fields | Mapping of cover no. / version, QPS reference, EBIX invoice no., "BDOI SYS REF", "booking category", "Corp Dep" to BIBS fields | p.59-61 |
| CQ23 | Security NFRs | Single role per user, single session per device, lockout at 3 attempts: apply to all BIBS users or to Collections only? | NFR 1-10 |
| CQ24 | Incentive campaigns | Relation between the draft campaigns (062) and the early-remittance incentive in remittance batches (RMTID.023 / PRCID.028): the same programme billed by service invoice, or two programmes? | BRCLXN.062, OQ23 |
| CQ25 | NFR alignment | CLXN hours 06:00-22:00 Mon-Fri and RPO 4 h vs Operations 07:00-18:30 and RPO 24 h vs BRD-1 07:00-22:00 Mon-Sat | NFR, OQ44 |

**Answered in part by later BRDs** (single record with the design impact: [`BDOI_CROSS_BRD_DECISIONS.md`](BDOI_CROSS_BRD_DECISIONS.md) section 3):
- **CQ13 answered** by BRD-13 Data Migration (BRID 6.1, 6.2): legacy invoices keep being processed in BIBS after
  cutover, so both BIBS and EBIX invoice numbers are valid until the legacy invoices run off
  (`CLX_INVOICE_NO_PATTERN` stays; QPS format per DMQ11).
- **CQ07 partial** (BRD-13 p.3, BRID 5.1, 6.1-6.4, 11.1): outstanding receivables and UPP are carried forward and
  processed in BIBS; history stays read-only in legacy or in the archive. Open dispositions, promises and collector
  assignments are not mentioned (DMQ34). The Collections seam `CLX_LEGACY_ITEMS` is narrowed to that open state (V1007).
- **CQ22 partial** (BRD-13 BRID 6.1): legacy invoices keep their legacy identifiers (`ops_invoice.legacy_invoice_no`,
  `legacy_ref`, `source_system`); the field mapping is not given.

## 11. Observations on the BRD pack

- The pack holds **two versions of the workshop addendum**. The signed one (p.13-22) stops at BRCLXN.060. The draft (p.1-12, "of 14", last two pages missing) adds 061-064. The revision logs are identical. Rows 061-064 are therefore unapproved (CQ01).
- The renumbering addendum (p.23-34) has the page header "CLAIMS" and the heading "Addendum1: Claims", a copy error. It is signed by the Collections reviewers.
- BRCLXN.001 changes "Cover Number level" (FRID-001) to "Invoice Number level" without comment (CQ02).
- BRCLXN.036 adds "BOOKER - should be name of the booker", which FRID-036 does not have.
- FRID-045-048 have no RQ ID. The RQ IDs jump from RQID-031 to RQID-033 (FRID-039 -> FRID-040).
- BRCLXN.059 numbers its acceptance criteria 1, 2, 5. BRCLXN.056 and 059 have their acceptance criteria on the following page. BRCLXN.050 has no priority.
- The handwritten edit on FRID-015 (p.78) and the caveat of the Operations Head on exports (p.93) are recorded in section 1. The caveat conflicts with the stakeholder functions "export the disposition list" etc. (p.43-46).
- Approval of the signed addendum: J. M. Jarin "for regularization - on leave", R. A. De Leon (Comptrollership) "not required", two approvers "with signing" (p.22). The CMS BRD sign-off (p.62-63) has the approver block (S. M. Miranda) unsigned; approval comes by e-mail (p.64-65). One input provider is marked "resigned 2/24/2025".
- The BRD is written for the legacy landscape: EBIX, QPS, Automated Payment Processing and ISYS. In BIBS these become booking / opsledger, cashiering, and a parked ISYS link. EBIX-specific wording (invoice format `I########`, cover number, QPS reference) needs mapping (CQ13, CQ22).
- The NFR tables contain typos: "6,0000 tpw" (weekly volume) and "minuntes". The retention table is headed "23b" and the performance table "15a", which are template numbers.
- The Stakeholders table lists "BDO Insure" groups Disbursement, ACSL, Comptrollership and ITG as viewers. These link Collections to the pending Accounting, Disbursement and ACSL BRD pack (`docs/source-documents/Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger BRD.zip`).
- There is **no requirement on holds, special remittance or the insurer schedule** (MKTID.001-009). The Operations build of those items stands (section 9, OQ45).
