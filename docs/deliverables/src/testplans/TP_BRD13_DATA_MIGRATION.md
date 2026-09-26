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
version: "1.2"
date: 26 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-13 Data Migration
h1_page_break: false
control:
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: First issue for BDOI review, from FRS BRD-13 v1.0, with the Excel workbook of the same version
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "From FRS BRD-13 v1.1 - cases for FR-DM-034 (package remapping) and FR-DM-124 (carried renewals of the January-May 2028 expiries), the trial-migration load order (FR-DM-120), the sign of a Migration Clearing difference (FR-DM-021), the excluded rows in the error rate (FR-DM-013), MIG_UPP_ISSUE_AR (FR-DM-051), QPS invoice numbers before DMQ11 (FR-DM-061) and the quoted messages of FR-DM-091, 110 and 123; test levels on the BDOI timeline; the seven findings of v1.0 are resolved in the FRS"
  - version: "1.2"
    date: 26 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "From FRS BRD-13 v1.2 (BDOI answers DMQ36-DMQ39) - new FR-DM-022 to 024 (provisional GL opening, FY2027 true-ups, true-up reconciliation and closure) and FR-DM-125 (renewal advices already sent, maker-checker resubmission); FR-DM-034 (PACKAGE map loaded for the Renewal sanitation), FR-DM-040, FR-DM-121, FR-DM-122, FR-DM-123 and FR-DM-124 (go-live renewal extraction) revised; scenario SC-DM-09 and data set TD-DM-11; the two findings of v1.1 closed"
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: Review and sign-off}
  - {name: "Head, Comptrollership; Product Owner FRBS / ACSL", role: Approver, organisation: BDOI, purpose: "Reconciliation, legacy sub-ledgers, reversals"}
  - {name: "Head, Operations; Operations - Financial Transactions", role: Business tester, organisation: BDOI, purpose: "Legacy invoices, UPP, remittance, endorsements"}
  - {name: "Product Owner, MBS; Marketing", role: Business tester, organisation: BDOI, purpose: "Client master, reference data, renewal transition"}
  - {name: "Audit / Compliance", role: Business tester, organisation: BDOI, purpose: "Legacy Inquiry and access log"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, trial migrations, defect fixing"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-13 Data Migration in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the Data Migration Lead, the data owners and stewards, Comptrollership, Operations, Marketing and Compliance what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-13_Data_Migration_v1.2.xlsx`.

Every case traces to a functional requirement (FR) of FRS BRD-13 v1.2 and to the BRD requirement IDs (BRID 1.1a to 12.1) that the FR meets.

Data Migration is designed and not yet built. The cases are written from the FRS and the build design, so:

- expected results quote the message text of the FRS without a message code; the codes are added in the version issued after the module is built;
- the automation reference column is blank; the automated tests written during the build are referenced in that version;
- screen paths and button labels are those of the build design and are checked against the screens before the system test starts.

## Scope

In scope are all 44 FRs of FRS BRD-13 v1.2:

- governance: data object register, decisions and sign-off gates (FR-DM-001 to 003);
- extract intake with control totals, code maps, unmapped codes, validation, load, rerun and rollback (FR-DM-010 to 015);
- reconciliation L1 to L5 and Migration Clearing; the provisional GL opening at the year-end boundary and the FY2027 true-ups with their reconciliation and closure (FR-DM-020 to 024);
- reference data, client matching and the migrated client master, search by legacy reference, the PACKAGE map loaded for the Renewal sanitation (FR-DM-030 to 034);
- in-force policy headers (FR-DM-040, 041);
- legacy invoices and UPP and their processing: automatch, dispositions, refund, reclassification to income, OTC and autopay, DPPR and PR2307 reversals, remittance, endorsements, Prod Recon change report (FR-DM-050 to 100);
- legacy archive inquiry and access log (FR-DM-110, 111);
- cutover plan with the trial-migration load order, go / no-go, RMEL transition, decommissioning, the go-live renewal extraction of the January-May 2028 expiries and the renewal advices already sent, with their maker-checker review (FR-DM-120 to 125).

The early renewal release of the concept paper of 6 September 2026 is superseded by the single January 2028 go-live (register DCR-240); no case tests a production use of BIBS before the cut-over. The cases apply BDOI's answers of 26 September 2026 to DMQ36-DMQ38 and the recommended year-end option A of DMQ39; if Comptrollership chooses another option, the cases of FR-DM-022 to 024 are rewritten.

## Relation to the migration cycles

This plan tests the **functions** of the migration and of legacy item processing. The **data** of each cycle (Trial migration 1 and Trial migration 2 in SIT in April and July 2027, Trial migration 3 and Trial migration 4 in UAT in August and October 2027, the dress rehearsal in November 2027, production in January 2028) is proven by the reconciliation and sign-off of every object, as set out in the Migration Reconciliation Approach and Sign-off. The system test runs before Trial migration 1 on the test extracts of section 4; Trial migration 2 is the "data migration dry run with reconciliation of migrated balances" of the UAT readiness programme (item 11); Trial migration 3 is the UAT load, and UAT runs on migrated data.

## Out of scope

- Loaders owned by other modules and decided under DMQ30: open claims (Claims), EB programmes, submitted-policy masterlists; they are tested in those modules' plans when BDOI brings them into scope.
- Performance at full volume (1,000,000 client rows, 500,000 open items): tested in the dress rehearsal and the performance plan (deliverable 28).
- The SFTP drop and the legacy read-only links (parked seams); the cases use console uploads.

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-13 Data Migration (`BIBS_FRS_BRD-13_Data_Migration_v1.2.docx`) | 1.2, 26 Sep 2026 |
| R2 | BDOI Data Migration BRD (`docs/source-documents/BRD - Data Migration - draft V0.01.pdf`) | draft v0.01, 14-Apr-2026 |
| R3 | Data Migration build design (`docs/architecture/DATA_MIGRATION_DESIGN.md`) | current |
| R4 | Data Migration Strategy and Approach; Reconciliation Approach and Sign-off; Cutover Runbook | 1.2 |
| R5 | BDOI Data Requirements Workbook and extract templates | 1.2 |
| R6 | BRD discrepancy and clarification register | 1.2 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Intake checks, masking, rule engine, matching, loaders through the services, reconciliation, gates, legacy postings per module, against a PostgreSQL database | iorta TechNXT developers | During the build waves DM0 to DM3; in CI on every change |
| System test | Every case of the workbook, screen by screen, with the test extracts | iorta TechNXT QA | After each build wave is deployed on SIT (DM0 and DM1-C by 9 Apr 2027, the rest by 18 Jun 2027), before the trial migration that needs it |
| Persona end-to-end | The nine scenarios by the persona that owns each step | iorta TechNXT QA with BDOI testers | Before Trial migration 2 |
| Trial migrations and dress rehearsal | Full cutover with masked full extracts, reference data and clients first; reconciliation and sign-off per object | Migration team with BDOI owners | Trial migration 1 19-30 Apr 2027, Trial migration 2 5-16 Jul 2027 (SIT); Trial migration 3 2-13 Aug 2027, Trial migration 4 4-15 Oct 2027 (UAT); dress rehearsal 15-26 Nov 2027 |
| User acceptance test (UAT) | Scenarios SC-DM-05 to SC-DM-09 and the High-priority cases on the Trial migration 3 and Trial migration 4 data | BDOI testers | UAT migration window (August-October 2027) |

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations and alternate flows became **test conditions** (sheet Test Conditions).
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen, the preconditions and data set, the steps and the expected result; where BIBS shows a message, the expected result quotes the FRS text.
- Every FR has at least one **positive** and one **negative** case. Boundary cases test the error-rate threshold (0.5 percent of master rows) and the archive export limit (1,000 rows).
- Amounts in the expected results come from the made-up data sets (for example invoice I00123456: basic 20,000.00, paid 10,000.00 before cutover) so testers can check the figures on the screen and in the journals. Accounts are named, not numbered: the legacy control accounts and Migration Clearing are assigned by Comptrollership (DMQ18).

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
| System test | The build wave under test is deployed on SIT with its roles, parameters, lists and seed data (V1980-V1982); legacy control accounts, Migration Clearing and the LG_ rule lines are on the test chart; the test extracts of section 4 are prepared in the workbook layouts; CI is green. |
| Persona end-to-end | All High-priority system test cases run; no open Critical defect; Renewal (for FR-DM-122) and the Operations modules deployed on SIT. |
| UAT | FRS BRD-13 v1.2 signed or its comments agreed; the M1 decisions of the Strategy applied; Trial migration 2 exit criteria met; UAT holds the Trial migration 3 data; BDOI testers have their users. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect. |
| Persona end-to-end | All nine scenarios passed end to end with the expected postings, reconciliation results and notifications. |
| UAT | All scenarios and High-priority cases passed or accepted by the process owner; no open Critical or High defect; the sign-off of section 10 is signed. |

**Suspension.** Testing stops when a Critical defect blocks a scenario, when a test extract leaves the environment unusable (staging or ledger corrupted), or when unmasked data is found outside production. It resumes after the fix or after the environment is restored from its snapshot.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated tests on every change | Created by each test |
| SIT | System test, persona runs, Trial migrations 1 and 2 | Test extracts of section 4.2; masked full extracts for the trial migrations |
| UAT | Trial migrations 3 and 4 and UAT | Masked full extracts |
| Production-sized | Dress rehearsal | Masked full-volume extracts |

Non-production data is always masked at intake (names, addresses, TIN, ID, account and phone numbers, e-mail, birth dates). Staging and files are purged within 5 days of sign-off in every environment.

## Named data sets

<!-- tp:data -->

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares the test extracts with the stewards; runs the defect triage |
| System testers | iorta TechNXT QA | Run the system test and persona scenarios |
| Developers | iorta TechNXT | Automated tests; defect fixes |
| BDOI testers | Data Migration Lead, stewards, owners, Comptrollership, Cashiering, Remittance, Adjustment, Marketing, Compliance | Run the UAT scenarios of their role; confirm expected results and figures |
| BDOI process owner | Program Manager with the Head of Comptrollership | Decides disputed expected results and accepted defects; signs off UAT |

The personas are:

<!-- tp:personas -->

# Defect management

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Data Migration |
|---|---|---|
| Critical | A load, posting or reconciliation gives a wrong financial result, data is lost, or a control (gate, maker-checker, masking) is broken | Migration Clearing not zero for a correct extract; unmasked data in SIT; operator signs own reconciliation |
| High | A function does not work as the FRS states and there is no workaround | Rerun loads rows already loaded; automatch ignores migrated UPP |
| Medium | Works with a workaround, or a message, label or report column is wrong | A report column missing; wrong break reason list |
| Low | Cosmetic | Alignment, spelling |

Defects are triaged daily; Critical within 1 working day, High within 3. A fixed defect is retested with its case and the cases of the same condition.

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

## Automated tests referenced

<!-- tp:automation -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| Open decisions change expected results (DMQ09 headers, DMQ12 components, DMQ14 UPP receipt, DMQ18 accounts, DMQ19 / DMQ20 reversal entries; DMQ39 until Comptrollership confirms option A) | High | Values are configuration; affected cases are re-run after the answer; the next FRS issue updates the texts |
| Renewal FRS does not yet describe the go-live extraction and the check PACKAGE_REMAP (findings) | Medium | Cases check the worklist and the Exception bucket; messages added when the Renewal FRS is issued |
| True-up cases need a legacy GL extract with adjustment journals and a journal listing | Medium | TD-DM-11 prepared with Comptrollership; a true-up is rehearsed in Trial migration 4 |
| Test extracts do not reflect real legacy data quality | High | Trial migrations use masked full extracts; profiling after Trial migration 1 adds cases for the issues found |
| Screens and messages differ from the design when built | Medium | Compare before the system test; workbook re-issued with the built texts and codes |
| Renewal not deployed for FR-DM-122, 124 and 125 | Medium | Test the port with the stub; re-run when Renewal is deployed |
| Real personal data reaches a test environment | High | Masking at intake; test lead checks staged rows after each upload |

# FRS findings

The seven findings of version 1.0 are resolved in FRS BRD-13 v1.1. The two findings of version 1.1 are closed by the BDOI answers: the STATUS:RENEWAL map of carried candidates is no longer needed (no candidate is carried, DMQ37), and the Renewal check PACKAGE_REMAP is named in FRS BRD-13 v1.2 (DMQ36). The findings below were raised while writing the cases of version 1.2; both are for the Renewal FRS.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Data Migration requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
