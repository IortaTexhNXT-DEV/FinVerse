---
# Word summary of the BRD-5 Volume 1 test plan (Accounting - FRBS, business and system administration).
# The tables marked <!-- tp:... --> are filled from brd05_vol1_cases.yaml.
# Build: python docs/deliverables/src/testplans/build_test_plan.py brd05_vol1_cases.yaml
title: Accounting (FRBS) Test Plan
subtitle: BRD-5 Volume 1 - FRBS accounting and business and system administration - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-05
name: Accounting Disbursement ACSL Vol1 Summary
doc_id: BIBS-TP-BRD-05-V1
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-5 Vol. 1 - FRBS and administration
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-5 Volume 1 v1.0 and the as-built code
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Comptrollership Head (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Head, Comptrollership", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Financial Reporting and Budget Section (GL Officers, Team Leads, Section Head)", role: Business tester, organisation: BDOI, purpose: "Chart, journals, closing, reports, service fee"}
  - {name: "Business Administrator, System Administrator", role: Business tester, organisation: BDOI, purpose: "Lists of values, users and role requests"}
  - {name: "Access approvers", role: Business tester, organisation: BDOI, purpose: Decisions on administration requests}
  - {name: "Disbursement, Remittance", role: Business tester, organisation: BDOI, purpose: "Service-fee payouts, broking cut-off"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for Volume 1 of BRD-5 in BIBS (BDOI Broker System, on iNXT BrokerVerse): the accounting of the Financial Reporting and Budget Section (FRBS) and the business and system administration (BASAU). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx`, which the testers use during execution.

BRD-5 is specified in two FRS volumes, and it has one test plan per volume. Volume 2 (Disbursement, Payment Requests and ACSL) has its own plan, `BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx`. Every case of this plan traces to a functional requirement (FR) of FRS BRD-5 Volume 1 v1.0 and to the BRD requirement IDs (FRBS n.n.n, BASAU n.n.n, Appendix A) that the FR meets. BRD-5 is built, so the expected results quote the messages and codes that BIBS returns, and each case names the automated test that already covers it, where one exists.

## Scope

In scope are all 36 FRs of FRS BRD-5 Volume 1 v1.0 and the BRD references they trace to:

- access and session warnings (FR-AC-001, 002);
- the monthly revaluation rate and the chart of accounts - maintenance, upload, numbering and short-code search (FR-AC-010 to 014);
- reports on demand - run, copy, column filters, batches and print options (FR-AC-020 to 023);
- posting through the accounting engine and manual entries - assignment, preparation, automatic reversal of accruals, validations, confirmation, posting and return, edit rules (FR-AC-030 to 037);
- the month-end GL close, the year-end close and its verification, the broking-books cut-off and the FX revaluation (FR-AC-040 to 043);
- bank file upload, automatic reconciliation and the unmatched reports (FR-AC-050, 051);
- the service fee, its liquidation, cost-centre derivation and early incentives as Other Income (FR-AC-052 to 055);
- the FRBS report pack, account schedules, Mancom and government outputs (FR-AC-060 to 063);
- lists of values and user and role management through approved requests (FR-AC-070 to 072).

The roles-and-access sheet checks each accounting and administration action against the roles that may and may not perform it (the matrix of FRS Volume 1 section 3.3).

## Out of scope

- Volume 2 of BRD-5 (Disbursement, Payment Requests, ACSL), which has its own test plan. Where a Volume 1 case needs a Disbursement step (the payout of a service-fee line), the step is run as the precondition says and tested in Volume 2.
- The real chart of accounts, posting rules, schedule layouts and GARD formats of BDOI (AQ01, AQ02, AQ05). The cases run on the seed chart and rules and are re-run when BDOI's data is loaded.
- eFPS, DAT and CAS files for the BIR, and the payroll returns (1601-C, 1604-C, HDMF, SSS, PhilHealth) (AQ06, AQ07).
- BDO single sign-on and Active Directory (Q42).
- Daily FX revaluation, unless BDOI confirms it (AQ03).
- Performance and volume testing; this is the BIBS-wide performance test plan (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-5 Volume 1 (`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx`) and its cover note | 1.0, 25 Sep 2026 |
| R2 | BRD-5 Accounting, Disbursement and ACSL with Addenda 1 and 2 (`docs/source-documents/`) | as signed |
| R3 | Test plan workbook BRD-5 Volume 1 (`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx`) | 1.0 |
| R4 | Test plan BRD-5 Volume 2 (`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx`) | 1.0 |
| R5 | Accounting and Disbursement build design (`docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md`) | current |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Journal controls, chart rules, closing, revaluation, reconciliation, service fee, schedules, BIR outputs and permissions of each FR, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Every change, in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the seed data | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The nine scenarios run from start to finish by the persona that owns each step | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data | BDOI FRBS and administration testers | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, reports and files are what the user expects. A case that names an automated test is still run on the screen at least once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "A journal is never posted by its submitter nor above the poster's limit".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes it exactly with its code, as returned by the build.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit (the back-dated window, the poster's authorisation limit, the numbering width, the reverse-on date).
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow the month end from the broking cut-off to the GL close in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission; maker-checker and four-eyes rules |
| Workflow | A status change of a journal, account, close schedule, service-fee run, reconciliation or request |
| Report-output | Reports, report batches, schedules, the report pack and BIR outputs; content checked against the ledger |
| Upload-download | Chart upload, journal upload, bank files and liquidation reports |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-AC-036.3-01 is the first case of condition 3 of FR-AC-036. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

Month-end and year-end cases depend on the date. The test lead sets the SIT clock or runs the jobs (GL_PERIOD_CLOSE, BROKING_BOOKS_CLOSE, JOURNAL_AUTO_REVERSAL, the alert job) on demand from Scheduled Jobs, as the preconditions describe.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The build is deployed on SIT with the seed profile (seeds V900 to V999 and the seed runners of Operations, Disbursement and FRBS); CI is green on the deployed commit; the periods of the current year are open; this plan is reviewed by the iorta TechNXT project manager. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; the Disbursement steps of the service-fee scenario pass in the Volume 2 plan. |
| UAT | FRS BRD-5 Volume 1 v1.0 is signed off or its open comments are agreed; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); BDOI testers have user IDs with the roles of section 5; BDOI's chart and rules are loaded, or BDOI accepts the seed chart for UAT (AQ01, AQ02). |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All nine scenarios passed end to end; the trial balance is balanced after the month-end and year-end scenarios. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when a period is closed by mistake and cannot be reopened, when the environment is down for more than half a day, or when the seed or UAT data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed profile migrations and runners (chart, rules, bookings, Operations, Disbursement, FRBS); SIT clock adjustable by the test lead |
| UAT | Acceptance by BDOI testers | Masked copy of production-like balances plus the seed data; no real client, supplier or employee names, TINs, bank account numbers or e-mail addresses |

Non-production data is always masked. Names, TINs, addresses, bank account and check numbers and e-mail addresses are replaced before data is loaded into SIT or UAT. Bank files used in the reconciliation cases are prepared from the masked data, never from real bank statements.

## Named data sets

The cases refer to named data sets. Most are provided by the seeds and runners; TD-AC-04 (the copy with an error), TD-AC-05 and TD-AC-07 (the bank file) are prepared by the tester or the test lead as the case describes.

<!-- tp:data -->

Month-end cases close periods of the SIT company. The test lead runs them on a copy of the seed company or reloads the seed profile between cycles, because a closed fiscal year cannot be reopened.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, data and the SIT clock; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Keep the automated tests green; fix defects; support triage |
| BDOI department testers | BDOI FRBS, Business and System Administration | Run the UAT scenarios of their department; confirm the expected results match the business and accounting rules; raise defects |
| BDOI process owner | BDOI Comptrollership Head | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

The GL Officers test the entries, reconciliation, service fee and reports (SC-AC-03, 04, 06, 07, 08); the GL Team Lead and Section Head the chart, rates, closing and approvals (SC-AC-02, 05); the Business and System Administrators and the access approvers the administration requests (SC-AC-01, 09). The personas and SIT/UAT users are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Accounting |
|---|---|---|
| Critical | A main flow cannot be completed, the ledger is wrong or unbalanced, or a security rule is broken | An unbalanced journal posts; a submitter posts the own journal; a closed period accepts a posting |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | The accrual is not reversed on its date; the service fee is computed on the gross commission |
| Medium | A function works with a workaround, or a message, label or report column is wrong but the figures are right | A report export misses the filter note; a message names the wrong period |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the journal, run, period or request number.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the build is wrong. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (see section 9).
- Target fix times on the test environments: Critical within 1 working day, High within 3 working days, Medium within the cycle, Low by agreement.
- A fixed defect is retested with its case and the cases of the same condition; the automated tests of the FR must also pass.

# Coverage summary

## Totals

<!-- tp:counts -->

Every FR has at least one positive and one negative case, and every BRD reference is covered. The builder of the workbook checks this each time the plan is built and refuses to produce a plan with a gap.

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

<!-- tp:brd-coverage -->

## Scenarios

<!-- tp:scenarios -->

## Roles and access

The table shows the actions checked per role. Each Y and N is one row of the Roles and Access sheet.

<!-- tp:access -->

<!-- pagebreak -->

## Automated tests referenced

Cases with an automation reference are covered by these existing tests, which run in CI on every change:

<!-- tp:automation -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| BDOI's chart, posting rules and report layouts are not loaded before UAT (AQ01, AQ02, AQ05) | High | The cases run on the seed chart; the report and posting cases are re-run on BDOI's data once loaded, without a new build |
| Closing a period or a fiscal year on SIT cannot be undone | High | Month-end and year-end cases run on a copy of the seed company or after a reload of the seed profile |
| Date-driven cases (close schedule, cut-off at 23:00, reverse-on date, year-end deadline) need the clock | Medium | The test lead sets the SIT clock and runs the jobs on demand |
| The service-fee payout depends on Disbursement (Volume 2) | Medium | The payout steps are run by the Disbursement testers as preconditions; a defect there is triaged with the Volume 2 plan |
| BDOI answers to open questions change expected results (role matrix AQ28, service-fee rates AQ20, cost-centre rules AQ26, negative-balance accounts AQ30) | Medium | The values are configuration; the affected cases name the rule or parameter and are re-run after the change |
| BDOI testers are not available in the UAT window | High | Agree named testers per department and dates in the UAT plan (deliverable 30) before UAT starts |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from the build. The cases use the built behaviour; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Volume 1 requirements of BRD-5 it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Section Head, Financial Reporting and Budget", organisation: BDOI}
  - {name: "", role: Business Administrator, organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
