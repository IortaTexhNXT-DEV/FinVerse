---
# Word summary of the BRD-13 Data Migration test plan. The tables marked <!-- tp:... --> are filled
# from brd13_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd13_cases.yaml
title: Data Migration Test Plan
subtitle: BRD-13 Data Migration - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-13
name: Data Migration Summary
doc_id: BIBS-TP-BRD-13
version: "2.0"
date: 5 October 2026
status: Issued for BDOI business sign-off
header_title: Test Plan BRD-13 Data Migration
h1_page_break: false
control:
  - version: "1.2"
    date: 26 Sep 2026
    author: iorta TechNXT test team
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "Previous issue, from the separate FRS BRD-13 v1.2 (BDOI answers DMQ36-DMQ39)"
  - version: "2.0"
    date: 5 Oct 2026
    author: iorta TechNXT test team
    reviewer: iorta TechNXT Project Manager
    approver: "Program Manager, Business Project Services (at sign-off)"
    change: "Files 04 and 05 of the BRD-13 business sign-off set: cases traced to the Data Migration Handbook and to the screens of the Migration Console (one screen case per screen, one message case per screen); scenarios SC-DM-10 trial migrations and dress rehearsal, SC-DM-11 rollback and rerun, SC-DM-12 cut-over rehearsal"
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: Review and sign-off}
  - {name: "Head, Comptrollership; Product Owner FRBS / ACSL", role: Approver, organisation: BDOI, purpose: "Reconciliation, legacy sub-ledgers, reversals"}
  - {name: "Head, Operations; Operations - Financial Transactions", role: Business tester, organisation: BDOI, purpose: "Legacy invoices, UPP, remittance, endorsements"}
  - {name: "Product Owner, MBS; Marketing", role: Business tester, organisation: BDOI, purpose: "Client master, reference data, renewal transition"}
  - {name: "Audit / Compliance", role: Business tester, organisation: BDOI, purpose: "Legacy Inquiry and access log"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, trial migrations, issue resolution"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-13 Data Migration in BIBS. It tells the Data Migration Lead, the data owners and stewards, Comptrollership, Operations, Marketing, the Renewal processing team, Compliance and BDOI IT what will be tested, how, with which data and by whom, and when testing is complete. It is file 05 of the BRD-13 business sign-off set; the test conditions, scenarios and cases are in the test plan workbook, file 04.

Every case traces to a functional requirement (FR) of the Data Migration Handbook v2.0 (Part B) and to the BRD requirement IDs (BRID 1.1a to 12.1) that the FR meets, and names the screen of the Migration Console where it runs.

The expected results quote the message texts of the handbook, and the screen paths and button labels are those of its screen specifications. Each screen also has a screen case (the fields, actions and states of its specification) and a message case (the messages of the screen).

## Scope

In scope are all 44 FRs of the Data Migration Handbook v2.0 and the 20 screens of Part B:

- governance: data object register, decisions and sign-off gates (FR-DM-001 to 003);
- extract intake with control totals, code maps, unmapped codes, validation, load, rerun and rollback (FR-DM-010 to 015);
- reconciliation L1 to L5 and Migration Clearing; the provisional GL opening at the year-end boundary and the FY2027 true-ups with their reconciliation and closure (FR-DM-020 to 024);
- reference data, client matching and the migrated client master, search by legacy reference, the package code map loaded for the Renewal sanitation (FR-DM-030 to 034);
- in-force policy headers (FR-DM-040, 041);
- legacy invoices and UPP and their processing: automatch, dispositions, refund, reclassification to income, OTC and autopay, the DP PR Legacy Reversal and Legacy PR 2307 Reversal batches, remittance, endorsements, the Changes to Legacy Invoices report (FR-DM-050 to 100);
- legacy archive inquiry and access log (FR-DM-110, 111);
- cutover plan with the trial-migration load order, go / no-go, RMEL transition, decommissioning, the go-live renewal extraction of the January-May 2028 expiries and the renewal advices already sent, with their maker-checker review (FR-DM-120 to 125).

The early renewal release of the concept paper of 6 September 2026 is superseded by the single January 2028 go-live (register DCR-240); no case tests a production use of BIBS before the cut-over. The cases apply BDOI's answers of 26 September 2026 to DMQ36-DMQ38 and the recommended year-end option A of DMQ39; if Comptrollership chooses another option, the cases of FR-DM-022 to 024 are rewritten.

## Trial migrations, reconciliation, rollback, rerun and cut-over rehearsal

This plan tests the **functions** of the migration and of legacy item processing, and the way the migration is rehearsed. The **data** of each cycle (Trial migrations 1 and 2 in SIT in April and July 2027, Trial migrations 3 and 4 in UAT in August and October 2027, the dress rehearsal in November 2027, production in January 2028) is proven by the reconciliation and sign-off of every object, as set out in Part C of the handbook. The rehearsal itself is tested by three scenarios:

- **SC-DM-10 Trial migrations and the dress rehearsal.** Each trial migration is a Mock run plan on fresh masked extracts, from intake to acceptance, with its timings, error rates and reconciliation kept for comparison with the next run; the dress rehearsal loads the production volume within the 48-hour window with at least 20 percent margin (FR-DM-120).
- **SC-DM-11 Rollback and rerun.** A batch loaded with a wrong code map is rolled back, the map is corrected and the batch is loaded again and reconciles; rejected rows are corrected at source and rerun; a rollback is refused once a record of the batch has changed (FR-DM-015).
- **SC-DM-12 Cut-over rehearsal.** The production plan is rehearsed with its go / no-go checkpoints; a NO-GO restores the snapshot and reopens legacy, and the rehearsal is run again to GO (FR-DM-121).

The reconciliation of every object at L1 to L5, Migration Clearing and the opening-balance adjustments are tested by SC-DM-03 and SC-DM-09. The system test runs before Trial migration 1 on the test extracts of chapter 4; Trial migration 2 is the "data migration dry run with reconciliation of migrated balances" of the UAT readiness programme (item 11); Trial migration 3 is the UAT load, and UAT runs on migrated data.

## Out of scope

- Objects decided under DMQ30 (open claims, Employee Benefits programmes, submitted-policy masterlists, payees): they are tested in the plans of those modules when BDOI brings them into scope.
- Performance at full volume (1,000,000 client rows, 500,000 open items): tested in the dress rehearsal and the performance plan (deliverable 28).
- The secure file drop and the read-only legacy addresses (on hold until BDOI IT names them); the cases use the uploads of the Extracts screen.

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Data Migration Handbook BRD-13 (file 02 of the set) | 2.0, 5 Oct 2026 |
| R2 | BDOI Data Migration BRD | draft v0.01, 14-Apr-2026 |
| R4 | Migration Workbook BRD-13 (file 03 of the set): load templates, code maps, validation rules, cut-over tasks | 2.0, 5 Oct 2026 |
| R5 | Start Here guide and Guide deck BRD-13 (files 00 and 01 of the set) | 2.0, 5 Oct 2026 |
| R6 | BRD discrepancy and clarification register | 1.2 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| System test | Every case of the workbook, screen by screen, with the test extracts | iorta TechNXT test team | On SIT before Trial migration 1 (from 9 Apr 2027) |
| Persona end-to-end | The twelve scenarios by the persona that owns each step | iorta TechNXT test team with BDOI testers | Before Trial migration 2 |
| Trial migrations and dress rehearsal | Full cut-over with masked full extracts, reference data and clients first; reconciliation and sign-off per object; scenarios SC-DM-10 to 12 | Migration team with BDOI owners | Trial migration 1 19-30 Apr 2027, Trial migration 2 5-16 Jul 2027 (SIT); Trial migration 3 2-13 Aug 2027, Trial migration 4 4-15 Oct 2027 (UAT); dress rehearsal 15-26 Nov 2027 |
| User acceptance test (UAT) | Scenarios SC-DM-05 to SC-DM-09 and the High-priority cases on the Trial migration 3 and Trial migration 4 data | BDOI testers | UAT migration window (August-October 2027) |

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations and alternate flows became **test conditions** (sheet Test Conditions).
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen, the preconditions and data set, the steps and the expected result; where BIBS shows a message, the expected result quotes the message text of the handbook.
- Every FR has at least one **positive** and one **negative** case. Boundary cases test the error-rate threshold (0.5 percent of master rows) and the archive export limit (1,000 rows).
- Amounts in the expected results come from the named data sets (for example invoice I00123456: basic 20,000.00, paid 10,000.00 before cutover) so testers can check the figures on the screen and in the journals. Accounts are named, not numbered: the legacy control accounts and Migration Clearing are assigned by Comptrollership (DMQ18).

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at and next to a limit |
| Security-access | Screens and actions open only to the roles that hold the permission; maker-checker and segregation of duties |
| Workflow | A status change of an object, map version, batch, reclassification or reversal batch, plan or checklist |
| Report-output | Reports and exports; content checked against the screen |
| Upload-download | Extract and control files, payment files, runbook export |

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | SIT holds the Migration Console with its roles, settings, lists and seed data; legacy control accounts, Migration Clearing and the legacy accounting rules are on the test chart; the test extracts of chapter 4 are prepared in the load templates. |
| Persona end-to-end | All High-priority system test cases run; no open Critical issue; Renewal (for FR-DM-122) and the Operations modules on SIT. |
| UAT | The BRD-13 set signed or its comments agreed; the M1 decisions of the proposed rules applied; Trial migration 2 exit criteria met; UAT holds the Trial migration 3 data; BDOI testers have their users. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High issue. |
| Persona end-to-end | All twelve scenarios passed end to end with the expected postings, reconciliation results and notifications. |
| UAT | All scenarios and High-priority cases passed or accepted by the process owner; no open Critical or High issue; the sign-off of this plan is signed. |

**Suspension.** Testing stops when a Critical issue blocks a scenario, when a test extract leaves the environment unusable (staging or ledger corrupted), or when unmasked data is found outside production. It resumes after the fix or after the environment is restored from its snapshot.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| SIT | System test, persona runs, Trial migrations 1 and 2 | Named test extracts (below); masked full extracts for the trial migrations |
| UAT | Trial migrations 3 and 4 and UAT | Masked full extracts |
| Production-sized | Dress rehearsal | Masked full-volume extracts |

Non-production data is always masked at intake (names, addresses, TIN, ID, account and phone numbers, e-mail, birth dates). Staging and files are purged within 5 days of sign-off in every environment.

## Named data sets

<!-- tp:data -->

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT test team | Owns this plan and the workbook; prepares the test extracts with the stewards; runs the issue triage |
| System testers | iorta TechNXT test team | Run the system test and persona scenarios |
| Project team | iorta TechNXT | Resolve issues; support triage |
| BDOI testers | Data Migration Lead, stewards, owners, Comptrollership, Cashiering, Remittance, Adjustment, Marketing, Compliance | Run the UAT scenarios of their role; confirm expected results and figures |
| BDOI process owner | Program Manager with the Head of Comptrollership | Decides disputed expected results and accepted issues; signs off UAT |

The personas are:

<!-- tp:personas -->

# Issue management

<!-- table: widths=2.2,8.4,5 caption="Issue severity" -->
| Severity | Definition | Example in Data Migration |
|---|---|---|
| Critical | A load, posting or reconciliation gives a wrong financial result, data is lost, or a control (gate, maker-checker, masking) is broken | Migration Clearing not zero for a correct extract; unmasked data in SIT; operator signs own reconciliation |
| High | A function does not work as the handbook states and there is no workaround | Rerun loads rows already loaded; automatch ignores migrated UPP |
| Medium | Works with a workaround, or a message, label or report column is wrong | A report column missing; wrong break reason list |
| Low | Cosmetic | Alignment, spelling |

Issues are triaged daily; Critical within 1 working day, High within 3. A resolved issue is retested with its case and the cases of the same condition.

# Coverage summary

## Totals

<!-- tp:counts -->

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

<!-- tp:brd-coverage -->

## Scenarios

<!-- tp:scenarios -->

## Roles and access

<!-- tp:access -->

## Screens

<!-- tp:screens -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| Open decisions change expected results (DMQ09 headers, DMQ12 components, DMQ14 UPP receipt, DMQ18 accounts, DMQ19 / DMQ20 reversal entries; DMQ39 until Comptrollership confirms option A) | High | Values are configuration; affected cases are re-run after the answer; the next issue of the set updates the texts |
| The Renewal sign-off set describes the go-live extraction and the package check after this set | Medium | Cases check the worklist and the Exception bucket; messages added when the Renewal set is issued |
| True-up cases need a legacy GL extract with adjustment journals and a journal listing | Medium | TD-DM-11 prepared with Comptrollership; a true-up is rehearsed in Trial migration 4 |
| Test extracts do not reflect real legacy data quality | High | Trial migrations use masked full extracts; profiling after Trial migration 1 adds cases for the issues found |
| Screen names, labels or messages change after BDOI's review of the handbook | Medium | Compare before the system test; re-issue the workbook with the confirmed texts |
| Renewal needed on SIT for FR-DM-122, 124 and 125 | Medium | Test the hand-over with a prepared file; re-run when Renewal is on SIT |
| Real personal data reaches a test environment | High | Masking at intake; test lead checks staged rows after each upload |

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Data Migration requirements it expects to test, and accepts the entry and exit criteria of chapter 3.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
