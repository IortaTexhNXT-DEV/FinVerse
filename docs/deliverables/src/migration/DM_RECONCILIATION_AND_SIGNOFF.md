---
# Reconciliation Approach and Sign-off of the BRD-13 Data Migration.
# Build: python docs/deliverables/src/migration/build_migration_pack.py
title: Migration Reconciliation Approach and Sign-off
subtitle: BRD-13 Data Migration - how every data object is proven from source to target
doc_type: Reconciliation Approach
doc_code: Migration
brd: BRD-13
name: Reconciliation Approach and Signoff
doc_id: BIBS-DMR-BRD-13
version: "1.2"
date: 26 September 2026
status: Issued for BDOI review
header_title: Migration Reconciliation Approach and Sign-off
output: Migration/BIBS_Migration_BRD-13_Reconciliation_Approach_and_Signoff_v1.2.docx
h1_page_break: false
control:
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: "Head, Comptrollership (pending)"
    change: First issue for BDOI review, with the Data Migration Strategy and Approach v1.0
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: "Head, Comptrollership (pending)"
    change: "With the Strategy v1.1 (BDOI timeline, go-live January 2028; four trial migrations). L5 compares each legacy control account with the opening detail; sign of a Migration Clearing difference corrected in the worked example; cohort completeness of the carried RMEL cohorts (P03); package remapping samples"
  - version: "1.2"
    date: 26 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: "Head, Comptrollership (pending)"
    change: "BDOI answers of 26-Sep-2026. Provisional GL opening and FY2027 true-ups (DMQ39, recommended) - true-up reconciliation with cut-off, movement, balance and clearing checks, and its sign-off form. RA-sent file (P03) and the go-live renewal extraction check replace the carried cohorts (DMQ37, DMQ38). The PACKAGE map is reconciled as reference data loaded for the Renewal sanitation (DMQ36)"
distribution:
  - {name: "Head, Comptrollership; Product Owner FRBS / ACSL", role: Approver, organisation: BDOI, purpose: "Reconciliation approver for financial objects"}
  - {name: "Data owners of each object", role: Approver, organisation: BDOI, purpose: "Business verification and acceptance (G6)"}
  - {name: "BDOI Data Migration Lead; Program Manager", role: Reviewer, organisation: BDOI, purpose: "Go / no-go evidence"}
  - {name: "Audit / Compliance", role: Reviewer, organisation: BDOI, purpose: "Evidence retention; archive reconciliation"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Reconciliation runs and reports"}
---

# Purpose and scope

BRID 1.1b asks that, after cutover, "all data objects and items are reconcilable from source to target" from the generated data migration reports (BRD p.7). This document states how each object is reconciled, what BDOI verifies on the BIBS screens, how breaks are handled, which evidence is kept, and the forms that BDOI signs. It applies to every trial migration, the dress rehearsal and the production cutover, to the FY2027 true-ups after go-live, and to the archive loads before decommissioning.

It completes the Data Migration Strategy and Approach (section 6.10) and FRS BRD-13 (FR-DM-020 to FR-DM-024, FR-DM-003). Account codes are not named: the legacy control accounts and the Migration Clearing account are assigned by Comptrollership (DMQ18, register DCR-197).

# Reconciliation levels

Five levels are run by BIBS after each load (job MIG_RECONCILE) and on demand. Levels L1 to L4 apply to every object; L5 to the financial objects.

<!-- table: widths=1.2,3,6.2,6.2 caption="Reconciliation levels" size=8.5 -->
| Level | Measure | How it is computed | Pass rule |
|---|---|---|---|
| L1 | Record counts | Control file row count; rows received, staged, valid, warning, invalid, loaded, skipped, rejected, excluded (per batch and its reruns) | Received = control count; loaded + skipped + rejected + excluded = staged; rejected rows are 0 for financial objects or excluded by the owner |
| L2 | Amounts | Per amount column and currency: control total, staged sum, and the value read from BIBS (ledger components of origin LEGACY, unapplied balances, journal lines) | Difference 0.00 (tolerance MIG_AMOUNT_TOLERANCE, default 0.00) at each stage |
| L3 | Hash totals | Hash total of the key column as the layout defines it; SHA-256 per staged row; count of distinct keys in the cross-reference | Control = staged = cross-reference |
| L4 | Fields | Every mapped field of every loaded row read back through the owning service and compared with the staged, mapped value | Equal; a difference explained by the code map (mapped value) is not a break |
| L5 | GL | Migration Clearing balance per branch and currency, analysed per legacy control account (trial balance line against the opening detail posted to that account); each legacy control account against its legacy sub-ledger (ACSL GL-SL reconciliation with ledger context LEGACY) | Migration Clearing 0.00; per control account TB line = opening detail; control account = sub-ledger |

**Breaks.** Any difference is a break (status BREAK). A break is fixed by a rerun (new extract, new map version or a corrected row) or explained. An explanation gives a reason from the list MIG_BREAK_REASON (rounding at source, record excluded by the owner, mapping difference, legacy data error, late legacy transaction, other) and a text, and is approved by the reconciliation approver. A reconciliation cannot be signed (gate G5) while a break is open.

**Reports.** MIG-RECON-SUMMARY (object by level with status), MIG-RECON-DETAIL (lines, breaks, explanations, approvals), MIG-REJECTS (rejected and invalid rows with their messages), MIG-GL-CLEARING (Migration Clearing per branch and currency; legacy control accounts against sub-ledgers). All export to Excel and PDF.

# Control totals BDOI sends

Every data file comes with a control file (workbook sheet "Control File" and template `CONTROL_template.ctl.csv`). It always carries the row count and the SHA-256 of the data file, plus the hash total of the key and the amount totals of the layout:

<!-- dm:controls -->

The workbook sheet "Control Totals" lists the same measures one per row, with columns for the value from BDOI, the value in BIBS, the difference and the status. It is the tie-out sheet of L1 to L3 for each cycle.

# Reconciliation per data object

<!-- table: widths=1.2,2.6,3.4,2.6,2.6,4.2 caption="What is reconciled per object" size=8 -->
| Object | L1 counts | L2 amounts | L3 hash | L4 fields | L5 GL / business check |
|---|---|---|---|---|---|
| R01-R08, R11 | Values received, mapped, created, rejected; PACKAGE map entries | - | Distinct keys | Created values (name, status, dates); PACKAGE entries in the Renewal package map | Unmapped-code report empty; every CREATE value authorised in its master; PACKAGE entries loaded into the Renewal package map equal the approved version |
| R09 | Payees received and loaded | - | Distinct payee codes | All loaded fields | Payee list in Disbursement |
| C01, C02 | Legacy clients received, clusters, merged, new, loaded | - | Distinct legacy client numbers = cross-reference entries | Identity, contact, segment, KYC fields of the survivor | Client review queue empty; each legacy number resolves to one BIBS client |
| C03 | Accounts received and loaded | - | Distinct (client, account) | All fields | - |
| P01, P01S | Headers received and loaded; shares | Sum insured, gross and net premium per currency | Distinct legacy policy references | All header fields and shares | Headers linked to their clients and legacy invoices |
| P03 | RA-sent rows received, loaded, rejected; resubmitted rows | Proposed premium per currency | Distinct legacy policy references | RA date, reference, channel, sender, tracker | At go-live, the extraction check MIG-RENEWAL-GOLIVE per expiry month January-May 2028: P01 headers expiring from T to 31 May 2028 = candidates + renewals booked in legacy; loaded P03 rows = candidates with the RA already sent; every January candidate URGENT |
| F01, F01S, F01C | Invoices, share rows, component rows | Booked, adjusted, paid, remitted, written off and open per component and currency | Distinct invoice numbers | Header fields, shares, every component and bucket | L5 - Premium Receivable, PR2307, DTIP, Commission Receivable (legacy) against the legacy invoices; Migration Clearing 0.00 |
| F02 | UPP items | Amount and balance per currency | Distinct UPP references | All fields, stage and disposition | L5 - Unapplied Collections (legacy) against the migrated UPP balances |
| F03 | Rows per record type | Promise and installment amounts | Distinct (invoice, type, seq) | All fields | Worklist items show the carried promises and assignments |
| F04, F06 | Rows per record type | Amount per currency | Distinct keys | All fields | Holds and PDCs visible in Remittance and Cashiering |
| G01 | Trial balance lines | Debit and credit per branch and currency, in currency and PHP | Lines per branch | Account, branch, currency and amounts per line | Provisional opening - balance sheet in BIBS = preliminary December TB per branch and currency; FY2027 result on retained earnings; P&L accounts 0.00; Migration Clearing 0.00 |
| G03 (G03, G03D) | Journal lines and detail lines per true-up | Debit and credit per branch and currency; detail per account | Distinct legacy journals | Account, branch, currency, amounts; item and component of each detail line | True-up reconciliation (section 5.1) |
| H01, H02 | Records per record type; documents | Amount per record type and currency | Distinct keys per record type; SHA-256 per document | Labelled legacy columns | Samples found in Legacy Inquiry with their documents |

# GL reconciliation: Migration Clearing

The open-item objects and the trial balance are loaded from two independent sources in legacy: the invoice and UPP detail (F01, F02) and the GL (G01). Migration Clearing proves that they agree (Figure 1):

- each legacy invoice and each UPP item is loaded with an opening entry whose balancing line goes to Migration Clearing;
- the trial balance is loaded as OPENING journals; its lines on the legacy control accounts (premium receivable, PR2307, DTIP, commission receivable, unrealised commission, deferred VAT, unapplied collections) are mapped to Migration Clearing instead of the legacy control accounts, because the opening entries build those balances in detail;
- when the detail equals the GL, Migration Clearing is 0.00 per branch and currency.

![Migration Clearing nets to zero when the detail equals the legacy GL](figures/dm_clearing.dot){width=15}

**Worked example** (one branch, PHP, one invoice and one UPP item; made-up figures; the unrealised commission follows DMQ13 and is taken here as fully unrealised):

<!-- table: widths=6.4,3.4,3.4,3.4 caption="Migration Clearing for one branch" size=8.5 -->
| Line | Debit | Credit | Effect on Migration Clearing |
|---|---|---|---|
| F01 opening of I00123456 - Premium Receivable - Legacy | 10,000.00 | | |
| F01 opening - Commission Receivable - Legacy | 4,480.00 | | |
| F01 opening - DTIP - Legacy | | 23,050.00 | |
| F01 opening - Unrealised Commission and Deferred VAT - Legacy | | 4,480.00 | |
| F01 opening - balancing line to Migration Clearing | 13,050.00 | | Debit 13,050.00 |
| F02 opening of UPP-2026-004321 - Unapplied Collections - Legacy | | 5,000.00 | |
| F02 opening - balancing line to Migration Clearing | 5,000.00 | | Debit 5,000.00 |
| G01 trial balance - legacy control-account lines (PR 10,000.00 Dr, commission 4,480.00 Dr, DTIP 23,050.00 Cr, unrealised and deferred 4,480.00 Cr, UPP 5,000.00 Cr) mapped to Migration Clearing | | 18,050.00 (net) | Credit 18,050.00 |
| G01 trial balance - cash in bank 18,050.00 Dr, mapped to the BIBS bank account | 18,050.00 | | - |
| **Migration Clearing** | | | **0.00** |

**A missing invoice.** A difference on Migration Clearing has the sign of the trial balance, because the TB puts the legacy control-account lines on Migration Clearing with their own sign and the opening entries put the detail there with the opposite sign. So an invoice missing from F01 leaves a **debit** equal to its net receivable position (open premium receivable, PR2307 and commission receivable, less open DTIP, unrealised commission and deferred VAT) when that is positive, and a **credit** when it is negative (FR-DM-021 acceptance 3). Two cases, made-up figures:

- F01 missed an invoice paid in full and not remitted, with DTIP 5,000.00 open and commission receivable 800.00 (realised on collection): Migration Clearing shows a **credit of 4,200.00** in that branch.
- F01 missed an unpaid invoice with premium receivable 3,000.00, DTIP 3,000.00, commission receivable 480.00 and the same 480.00 unrealised: Migration Clearing nets to 0.00. The per-account comparison of MIG-GL-CLEARING finds it: the TB is 3,000.00 debit higher than the detail on Premium Receivable - Legacy, 3,000.00 credit higher on DTIP - Legacy, and 480.00 higher on each commission account.

Either break is investigated and fixed or explained before go / no-go.

**Legacy control accounts against sub-ledgers.** After the loads, the ACSL GL to Sub-ledger Reconciliation is run with ledger context LEGACY: each legacy control account must equal the open positions of the legacy invoices (or legacy UPP) behind it. This is criterion 6 of the go / no-go and is repeated daily in hypercare and at every month-end until the legacy context closes.

## True-up reconciliation (FY2027 closing and audit adjustments)

With the year-end cut-over (DMQ39, option A, recommended), the GL opening is provisional. After the freeze the legacy GL accepts only the FY2027 closing and audit adjustments of the Comptrollership users on the signed access list, and each adjustment reaches BIBS in a true-up (FR-DM-023): true-up 1 after the legacy year-end close (about 18-21 January 2028), interim true-ups only when needed, and the final true-up after the audited financial statements (about March-April 2028). Each true-up comes with the legacy trial balance after the adjustments (G01, version TU1, TU2 or FINAL), the adjustment journal lines (G03), their open-item detail on the legacy control accounts (G03D) and the legacy journal listing since the freeze. BIBS runs MIG-TRUEUP-RECON after the posting; the Head of Comptrollership signs the true-up only when every check is met (FR-DM-024).

<!-- table: widths=2.6,7.4,6.6 caption="True-up checks (MIG-TRUEUP-RECON)" size=8.5 -->
| Check | Rule | What a break usually means |
|---|---|---|
| Cut-off | Legacy journal listing since the freeze = FY2027 adjustment register = journals of the true-ups so far; every journal dated in FY2027, posted after the freeze by a user on the access list; no posting by a legacy business module | A journal posted outside the register, by the wrong user, or in FY2028 |
| Movement | Per BIBS account, branch and currency: true-up n = legacy TB of true-up n - legacy TB of the previous true-up (or of the provisional opening), both mapped, P&L netted into retained earnings | A legacy journal missing from G03, or a TB extracted at a different time |
| Balance | Per BIBS account, branch and currency: provisional opening + true-ups 1 to n = legacy TB of true-up n, mapped (P&L netted into retained earnings; legacy control accounts replaced by the open-item detail) | An earlier true-up incomplete, or a mapping change between true-ups |
| Clearing and sub-ledgers | Migration Clearing 0.00 per branch and currency; each legacy control account = its legacy sub-ledger (ACSL, context LEGACY) | A control-account adjustment whose G03D detail is missing or on the wrong item |
| Opening period | When January 2028 was reopened for the posting: no journal other than the true-up posted into it during the window; the period closed again the same day | The reopen window used for other postings |

**Worked example** (one branch, PHP, made-up figures). True-up 1 brings two FY2027 adjustments: a closing accrual of professional fees of 80,000.00 (Dr expense, Cr accrued expenses) and an audit write-off of the 3,000.00 premium receivable of legacy invoice I00300002 (Dr bad debts, Cr Premium Receivable - Legacy), sent with its detail line.

<!-- table: widths=6.8,3.2,3.2,3.4 caption="True-up 1 in BIBS (journal MIG-TU-1, value date 1 January 2028)" size=8.5 -->
| Line | Debit | Credit | Note |
|---|---|---|---|
| Retained earnings (FY2027 expense of the accrual) | 80,000.00 | | P&L of FY2027, not FY2028 |
| Accrued expenses | | 80,000.00 | Balance-sheet account |
| Retained earnings (FY2027 bad debts) | 3,000.00 | | P&L of FY2027 |
| Migration Clearing (legacy control-account line of the write-off) | | 3,000.00 | Replaced by the detail |
| Detail - Migration Clearing / Premium Receivable - Legacy, invoice I00300002 basic | 3,000.00 | 3,000.00 | Legacy written off on the invoice; open balance 0.00 |
| **Migration Clearing after true-up 1** | | | **0.00** |

Movement check: the legacy TB after the close differs from the preliminary TB by exactly these amounts on the mapped accounts; balance check: the BIBS opening plus true-up 1 equals the legacy post-close TB; the FY2028 P&L is unchanged.

# Business verification on screen (gate G6)

L1 to L5 prove that what BDOI sent is what BIBS holds. Business verification proves that what BIBS holds means what the business expects: the right client, the right policy, the right balance, on the screens users will work with. The data owner (or the steward on the owner's behalf) checks a sample per object in the environment of the cycle and signs the object acceptance.

## Sample plan

The Migration Lead draws the samples from the loaded batch (random with a fixed seed recorded on the form, plus the targeted records). Samples are checked against the legacy screen or a legacy report of the same as-of date.

<!-- table: widths=1.6,7.8,7.2 caption="Business verification samples per object" size=8 -->
| Object | Sample | What is checked on the BIBS screen |
|---|---|---|
| R01-R08, R11 | Every CREATE value; 10 mapped values per map set; every legacy package with more than one BIBS target (conditional PACKAGE entries); every receipt series | Value, description and status in the master; the PACKAGE entries of each split in the Renewal package map; next AR / OR number = legacy last used + 1 |
| C01-C03 | 30 random clients per source system; 20 merged clusters; 10 decisions of the review queue; 10 corporate clients | Client search by legacy number; name, identity, contacts, segment, AO, KYC status and review date; cluster shows every legacy number |
| P01 | 30 random headers; 10 with several insurers; 10 expiring within 140 days | Account search by policy and legacy reference; dates, insurer and shares, sum insured, premium, client link, legacy invoices listed |
| P03 | 10 rows per tracker; every resubmitted row; at go-live 20 candidates per expiry month January-May 2028 | RA date, reference and channel against the tracker; at go-live the candidate shows the RA already sent, January candidates URGENT and in expiry-date order |
| F01 | 20 largest open balances per currency; 30 random; 10 endorsement or cancellation invoices; 10 direct-payment; 10 with 2307; up to 20 foreign-currency | Invoice 360: legacy badge, source, legacy number, original values, booked / paid / remitted / open per component, shares; Collections item for open premium |
| F02 | 20 largest balances; 20 random; every item with a disposition in progress (up to 30) | Unapplied Payments workbench: legacy AR, amount, balance, tab, references, disposition |
| F03, F04, F06 | 20 random per object | Promise and assignment on the Collections item; hold or special request in Remittance; PDC in the warehouse list |
| G01 | Every branch and currency total; 20 largest account lines; retained earnings per branch | Trial balance report in BIBS against the signed preliminary trial balance; P&L accounts at 0.00 |
| G03 | Every journal of the true-up; every detail line on a legacy control account | Journal inquiry (MIG-TU-n) against the legacy journal and the register; Invoice 360 or the Unapplied workbench for the item adjusted |
| H01, H02 | 10 records per record type; 10 documents | Legacy Inquiry search by the legacy key; labelled columns; document opens and matches its checksum |

## Acceptance rule

- **Financial objects (F01, F02, G01, G03) and identity fields of clients:** no error is accepted. One error stops the sign-off: the cause is found, fixed at source, in the map or in the loader, the batch is rerun, and a new sample is drawn.
- **Other fields:** an error that is isolated and cosmetic (for example capitalisation of an address) is listed on the form with its fix (legacy correction before the next extract, or correction in BIBS after go-live by the owner). Two or more errors of the same kind in a sample are treated as systematic: the sample is doubled and the cause is fixed before sign-off.
- The result is recorded on the object sign-off form (section 9.1) with the sample seed, the records checked and the errors found.

# Breaks, waivers and exclusions

<!-- table: widths=4.2,6.6,5.8 caption="How differences are closed" size=8.5 -->
| Situation | Handling | Approved by |
|---|---|---|
| Row rejected by a data-quality rule | Fixed at source and re-extracted, fixed by a new map version, or waived (master data only) | Data steward; waiver by the data owner |
| Financial row that cannot be fixed before go-live | Excluded from the batch with a manual-entry plan (who keys it in BIBS after go-live, when, and the amount) | Data owner and Comptrollership |
| Amount or count difference between control file and staging | The extract is rejected at intake; BDOI IT re-sends | - |
| Difference between staging and BIBS (L2, L4) | Loader or mapping defect: fixed and rerun; never explained away | iorta Migration Lead |
| Migration Clearing not 0.00 | Traced to the invoice, UPP or TB line; fixed by rerun or by a legacy correction and a new extract; a residual rounding difference is explained with its amount | Reconciliation approver (Comptrollership) |
| Late legacy transaction after the freeze | Recorded as a break; the transaction is re-keyed in BIBS after go-live and the legacy system owner explains the access. A FY2027 GL adjustment by a named Comptrollership user is not a late transaction: it goes through the next true-up | Data owner; BDOI IT |
| True-up check not met | The true-up is not signed; the cause is corrected by a new extract before sign-off or by the next true-up; a posted journal is never edited | Head, Comptrollership |

# Evidence and retention

For each object and cycle the evidence pack holds: the extract numbers with file names, SHA-256 and control totals; the map versions used; MIG-DQ-ISSUES with waivers; MIG-REJECTS; MIG-RECON-SUMMARY and MIG-RECON-DETAIL; MIG-GL-CLEARING for financial objects; the sample list with the check results; and the signed forms of section 9. The pack is exported from the Migration Console and filed in the project records. Staging rows and files are purged within 5 days of sign-off, but counts, hashes, totals, reports and sign-offs are kept (proposed 10 years, DMQ29).

# Sign-off forms

The forms below are signed in the Migration Console (gates G5 and G6) and printed for the project file. One form per object and cycle.

<!-- pagebreak -->

## Object reconciliation and acceptance (G5 and G6)

```keyvalues
Object and name: "..........  ...................................................."
Cycle and environment: "Trial migration 1 / Trial migration 2 / Trial migration 3 / Trial migration 4 / Dress rehearsal / Production     Environment: ................"
Batch numbers (MGB-): "........................................................................"
Extract numbers (MGX-) and as-of: "........................................................................"
Code map versions used: "........................................................................"
Sample seed and size: "Seed ............     Records checked ............"
```

<!-- table: widths=1.2,4.8,2.4,2.4,2.2,3.6 caption="Reconciliation results (from MIG-RECON-SUMMARY)" size=8.5 -->
| Level | Measure | Source (control) | BIBS | Difference | Status (MATCHED / EXPLAINED) |
|---|---|---|---|---|---|
| L1 | Rows received / staged / loaded / skipped / rejected / excluded | | | | |
| L2 | Amount totals per currency (list) | | | | |
| L3 | Hash total of the key | | | | |
| L4 | Field differences (count) | - | | | |
| L5 | Migration Clearing; control account vs sub-ledger | - | | | |
| G6 | Business verification sample - errors found | - | | | |

<!-- table: widths=1.2,6.4,5,4 caption="Breaks explained, waivers and exclusions" size=8.5 -->
| # | Break, waiver or exclusion | Reason and explanation | Approved by / manual-entry plan |
|---|---|---|---|
| 1 | | | |
| 2 | | | |

```signoff
rows:
  - {name: "", role: "Reconciliation approver - G5 (Comptrollership for financial objects)", organisation: BDOI}
  - {name: "", role: "Data owner - G6 object accepted", organisation: BDOI}
  - {name: "", role: "Data Migration Lead - G6", organisation: BDOI}
  - {name: "", role: "Migration Lead (reconciliation run)", organisation: iorta TechNXT}
```

<!-- pagebreak -->

## Cutover reconciliation summary (go / no-go GNG-3)

```keyvalues
Cutover: "Production     Go-live date (T): ................     Freeze: T-3 22:00"
Rollback point (snapshot ID and time): "........................................................................"
```

<!-- table: widths=3,4.2,3.2,2.6,3.6 caption="Day-1 objects at go / no-go" size=8.5 -->
| Object | Batch | Loaded / staged | G5 signed | G6 signed |
|---|---|---|---|---|
| R01-R08, R11 | | | | |
| C01-C03 | | | | |
| P01, P03 | | | | |
| F01 | | | | |
| F02 | | | | |
| F03, F04, F06 | | | | |
| G01 (provisional) | | | | |

<!-- table: widths=5.4,3.4,3.4,4.4 caption="Migration Clearing and legacy control accounts per branch and currency" size=8.5 -->
| Branch / currency | Migration Clearing | Control accounts vs sub-ledgers | Status |
|---|---|---|---|
| | | | |
| | | | |
| | | | |

<!-- table: widths=3,3.4,3.4,3.4,3.4 caption="Go-live renewal extraction check (MIG-RENEWAL-GOLIVE; completed on the morning of T)" size=8.5 -->
| Expiry month | P01 headers expiring | Renewed in legacy | Candidates created | RA already sent (P03) |
|---|---|---|---|---|
| January 2028 (URGENT) | | | | |
| February 2028 | | | | |
| March 2028 | | | | |
| April 2028 | | | | |
| May 2028 | | | | |

```signoff
rows:
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: "Program Manager (for the go / no-go board)", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

<!-- pagebreak -->

## True-up reconciliation and sign-off (FY2027)

```keyvalues
True-up: "1 / 2 / 3 / F (final)     Legacy trial balance version: TU1 / TU2 / TU3 / FINAL (audited)"
Legacy extracts (MGX-) and as-of: "........................................................................"
Journals posted (MIG-TU-): "........................................................................"
Prepared by (Comptrollership GL lead) and date: "........................................................................"
Opening period reopened: "No / Yes - reopened at ........ closed at ........ reason ................................"
```

<!-- table: widths=3.2,6.2,3.4,3.8 caption="True-up checks (from MIG-TRUEUP-RECON)" size=8.5 -->
| Check | Measure | Result | Status (MET / BREAK) |
|---|---|---|---|
| Cut-off | Legacy journals since the freeze / in the register / in the true-ups | | |
| Movement | Accounts with a difference between the true-up and the legacy TB change | | |
| Balance | Accounts with a difference between the BIBS opening and the legacy TB | | |
| Clearing | Migration Clearing per branch and currency | | |
| Sub-ledgers | Legacy control accounts vs legacy sub-ledgers | | |
| Opening period | Other journals posted in the reopen window | | |

```signoff
rows:
  - {name: "", role: "Head, Comptrollership - true-up approved and reconciliation signed", organisation: BDOI}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: "Program Manager - closure of the true-ups (final true-up only)", organisation: BDO Unibank ESG}
  - {name: "", role: "Migration Lead (reconciliation run)", organisation: iorta TechNXT}
```

The final true-up's reconciliation against the audited FY2027 trial balance is shared with the external auditor.

<!-- pagebreak -->

## Archive reconciliation (before decommissioning)

```keyvalues
Legacy system: "EBIX / QPS / ISYS / CMS / File shares"
Record types archived: "........................................................................"
Archive batches (MGB-): "........................................................................"
```

<!-- table: widths=3.4,2.6,2.6,2.6,2.6,2.8 caption="Archive counts and totals per record type" size=8.5 -->
| Record type | Legacy count | Archived count | Amount total legacy | Amount total archive | Samples checked in Legacy Inquiry |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

```signoff
rows:
  - {name: "", role: "Legacy system owner", organisation: BDOI}
  - {name: "", role: "Audit / Compliance", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
```
