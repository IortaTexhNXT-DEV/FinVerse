---
# Word summary of the BRD-5 Volume 2 test plan (Disbursement, Payment Requests and ACSL).
# The tables marked <!-- tp:... --> are filled from brd05_vol2_cases.yaml.
# Build: python docs/deliverables/src/testplans/build_test_plan.py brd05_vol2_cases.yaml
title: Disbursement and ACSL Test Plan
subtitle: BRD-5 Volume 2 - Disbursement, Payment Requests and ACSL - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-05
name: Accounting Disbursement ACSL Vol2 Summary
doc_id: BIBS-TP-BRD-05-V2
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-5 Vol. 2 - Disbursement, Requests, ACSL
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-5 Volume 2 v1.0 and the as-built code
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Comptrollership Head (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Head, Comptrollership", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Disbursement Section (Processors, Team Leaders, Approvers)", role: Business tester, organisation: BDOI, purpose: "Payees, vouchers, instruments, end of day, funding, reports"}
  - {name: "Marketing (AOs, reviewers, approvers)", role: Business tester, organisation: BDOI, purpose: "Refund, cash-advance and check-cancellation requests"}
  - {name: Human Resources, role: Business tester, organisation: BDOI, purpose: Cash-advance approval}
  - {name: "Accounting Controls and Subsidiary Ledger (ACSL)", role: Business tester, organisation: BDOI, purpose: "SOA reconciliation, cases, corrections, deductions"}
  - {name: "Operations (Cashiering, Remittance)", role: Business tester, organisation: BDOI, purpose: "Refund validation, payment reversals, CPC2, incentives, deductions"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for Volume 2 of BRD-5 in BIBS (BDOI Broker System, on iNXT BrokerVerse): Disbursement, the Marketing refund and cash-advance requests (Payment Requests) and the Accounting Controls and Subsidiary Ledger (ACSL). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx`, which the testers use during execution.

BRD-5 is specified in two FRS volumes, and it has one test plan per volume. Volume 1 (FRBS accounting and administration) has its own plan, `BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx`. Every case of this plan traces to a functional requirement (FR) of FRS BRD-5 Volume 2 v1.0 and to the BRD requirement IDs (DIS, MKT and ACSL n.n.n) that the FR meets. BRD-5 is built, so the expected results quote the messages and codes that BIBS returns, and each case names the automated test that already covers it, where one exists.

## Scope

In scope are all 83 FRs of FRS BRD-5 Volume 2 v1.0 and the BRD references they trace to:

- Disbursement: access, payees and their migration, requests from the modules, by e-mail and by upload, the workbench, the voucher with its entry and allocation, the seven modes of payment and the employee master (FR-DS-001 to 037);
- Disbursement: review, approval, rejection, cancellation and regularisation of DVs; instrument statuses and edits, bank uploads, stale checks, clearing entries, OR / AR and CWT tags, BIR Form 2307 (FR-DS-040 to 058);
- Disbursement: end of day, DCTF, checks, confirmations, account funding, check series, bank accounts and reports; CPC2 and early incentives, the CPC2 report gap and the invoice family (FR-DS-060 to 093);
- Payment Requests: refund and cash-advance requests from Requests Home to disbursement and liquidation, with validation, four-eyes, duplicate control and check cancellation (FR-PQ-001 to 017);
- ACSL: reports, insurer SOA upload and reconciliation, GL-SL reconciliation, the aging gap, cases, payment reversals, correction entries, remittance deductions and the invoice family (FR-AS-001 to 026).

The roles-and-access sheet checks each action against the roles that may and may not perform it (the matrix of FRS Volume 2 section 3.3).

## Out of scope

- Volume 1 of BRD-5 (FRBS accounting and administration), which has its own test plan. The account schedules used in place of the ACSL aging reports (FR-AS-005) are tested there.
- The Operations steps before Disbursement (remittance extraction, cashiering application) and the Cashiering side of the validations and payment reversals, beyond what the ACSL and Payment Requests cases need; they are in the BRD-2 Operations test plan.
- The bank interfaces (BDO Business Online Banking, the TPD transport of the DCTF) and the bank file layouts, which wait for AQ09; the cases use the minimal CSV uploads delivered.
- The CPC2 report (gap G4) and the ACSL aging reports (gap G3) as built functions; their cases check the interim views and are re-run when the reports are delivered.
- The final layouts of the checks, vouchers, forms and RRF / RFP (AQ14, AQ18); the cases run on the draft templates.
- Performance and volume testing, including SOA files at the 20,000-row limit under load; this is the BIBS-wide performance test plan (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-5 Volume 2 (`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx`) and its cover note | 1.0, 25 Sep 2026 |
| R2 | BRD-5 Accounting, Disbursement and ACSL with Addenda 1 and 2 (`docs/source-documents/`) | as signed |
| R3 | Test plan workbook BRD-5 Volume 2 (`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx`) | 1.0 |
| R4 | Test plan BRD-5 Volume 1 (`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx`) | 1.0 |
| R5 | Test plan BRD-2 Operations (`BIBS_TestPlan_BRD-02_Operations_v1.0.xlsx`) | 1.0 |
| R6 | Accounting and Disbursement build design (`docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md`) | current |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Payees, vouchers, instruments, postings, end of day, funding, requests, validations, liquidations, SOA reconciliation, corrections, deductions and permissions of each FR, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Every change, in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the seed data | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The ten scenarios run from start to finish by the persona that owns each step | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data | BDOI Disbursement, Marketing, HR and ACSL testers | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, documents, files and e-mails are what the user expects. A case that names an automated test is still run on the screen at least once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "The approver is never the processor or checker; only a DV For Approval with an entry is approved".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes it exactly with its code, as returned by the build.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit (EWT against the gross, 50 lines of an RRF, 60 fieldwork days, 180 days to a stale check, 20,000 SOA rows, 200 correction lines).
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow a refund from the RRF to the released check, or a correction from the case to the posted journal, in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission; maker-checker and four-eyes rules |
| Workflow | A stage or status change of a payee, request, DV, instrument, funding, validation, case, correction or deduction |
| Report-output | Reports, end-of-day outputs, the DCTF, BIR Form 2307 and the SOA reconciliation report |
| Upload-download | Request, payee, bank and SOA uploads; documents; payment confirmations |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-DS-041.3-01 is the first case of condition 3 of FR-DS-041; the Payment Requests cases start with TC-PQ and the ACSL cases with TC-AS. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The build is deployed on SIT with the seed profile (seeds V900 to V999, V1900 and the seed runners of Operations, Disbursement, FRBS and remittance deductions); CI is green on the deployed commit; the test mailboxes of payees and branches receive mail; this plan is reviewed by the iorta TechNXT project manager. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; the Cashiering and Remittance steps used by the scenarios pass in the BRD-2 plan. |
| UAT | FRS BRD-5 Volume 2 v1.0 is signed off or its open comments are agreed; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); BDOI testers have user IDs with the roles of section 5, including two Disbursement team leaders and two approvers for the funding cases. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All ten scenarios passed end to end; every approved DV of the cycle is regularised or explained in DSB-UNREGULARIZED. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when a posting failure leaves DVs unregularised that cannot be re-processed, when the environment is down for more than half a day, or when the seed or UAT data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed profile migrations and runners (payees, vouchers, end of day, funding, service fee, deductions); test mailboxes for payees and branches |
| UAT | Acceptance by BDOI testers | Masked copy of production-like payees, requests and balances plus the seed data; no real names, TINs, bank account numbers, check numbers or e-mail addresses |

Non-production data is always masked. Payee, client and employee names, TINs, addresses, bank account and check numbers and e-mail addresses are replaced before data is loaded into SIT or UAT, and branch and payee e-mails go to test mailboxes, so no ATD, payment advice or statement can reach a real bank branch or payee. The DCTF produced in testing is never forwarded to TPD.

## Named data sets

The cases refer to named data sets. The Disbursement data is provided by the seeds and runners; Payment Requests have no seed storyline, so TD-DPA-05 is keyed in by the tester; the upload and SOA files are prepared by the test lead.

<!-- tp:data -->

Many cases change the state of a seed DV, payee or deduction. The test lead reloads the seed profile between cycles; cases that change a parameter, an account or a master restore it at the end, as their expected result says.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, data, upload files and combined test users; runs the jobs on demand; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Keep the automated tests green; fix defects; support triage |
| BDOI department testers | BDOI Disbursement, Marketing, HR, ACSL, Cashiering, Remittance | Run the UAT scenarios of their department; confirm the expected results match the business and accounting rules; raise defects |
| BDOI process owner | BDOI Comptrollership Head | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Disbursement tests the payees, vouchers, instruments, end of day, funding and reports (SC-DPA-01 to 05); Remittance the incentives and the invoice family (SC-DPA-06); Marketing and HR the refund and cash-advance requests (SC-DPA-07, 08); ACSL the reconciliations, cases, corrections and deductions (SC-DPA-09, 10). The personas and SIT/UAT users are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Volume 2 |
|---|---|---|
| Critical | A main flow cannot be completed, money is paid wrongly or twice, the ledger is wrong, or a security rule is broken | A DV is approved by its processor; a refund is paid twice for the same AR; a cancelled DV is not reversed |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | The DCTF line is not 89 characters; a deduction is not capped at the payable; a correction posts unbalanced |
| Medium | A function works with a workaround, or a message, label or report column is wrong but the data is right | A report misses a column; a message names the wrong DV |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the DV, request, case, correction or deduction number.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the build is wrong. A defect found in a Cashiering or Remittance step is triaged with the BRD-2 test lead. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (see section 9).
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

## Automated tests referenced

Cases with an automation reference are covered by these existing tests, which run in CI on every change:

<!-- tp:automation -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| Bank layouts and interfaces are not given (AQ09); the uploads take a minimal CSV | Medium | The cases use the delivered CSV; the upload cases are re-run when BDOI gives the layouts |
| Document layouts and signatories are drafts (AQ14, AQ18) | Medium | The Report-output and Upload-download cases are re-run when the layouts are loaded |
| Four-eyes cases need combined test users and two users per role | Medium | The test lead creates them before the cycle (entry criterion) and removes them after it |
| Stale-check and date-driven cases need the clock (180 days, end of day, SLA) | Medium | The test lead moves print dates and business dates in the test database and runs the jobs on demand |
| Seed DVs, payees and deductions are changed by earlier cases | Medium | Reload the seed profile between cycles; cases that change a master restore it |
| BDOI answers to open questions change expected results (approvers AQ18, role matrix AQ28, deduction sources AQ23, CPC2 base AQ24, stale-check accounting AQ02 / AQ14) | Medium | The values are configuration; the affected cases name the parameter, list or rule and are re-run after the change |
| BDOI testers are not available in the UAT window | High | Agree named testers per department and dates in the UAT plan (deliverable 30) before UAT starts |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from the build. The cases use the built behaviour; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Volume 2 requirements of BRD-5 it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Disbursement Section", organisation: BDOI}
  - {name: "", role: "Head, Accounting Controls and Subsidiary Ledger", organisation: BDOI}
  - {name: "", role: "Head, Marketing", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
