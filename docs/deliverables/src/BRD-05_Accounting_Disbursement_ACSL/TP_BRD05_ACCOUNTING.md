---
# Word summary of the BRD-5 Accounting, Disbursement and ACSL test plan (release set v2.1). The tables marked
# <!-- tp:... --> are filled from brd05_cases.yaml.
# Build: python docs/deliverables/src/testplans/build_test_plan.py brd05_cases.yaml
title: Accounting, Disbursement and ACSL Test Plan
subtitle: BRD-5 Accounting (FRBS), Disbursement, Marketing Refund and Cash-Advance Requests and ACSL - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-05
name: Accounting Disbursement ACSL Summary
doc_id: BIBS-TP-BRD-05
version: "2.1"
date: 8 October 2026
status: Issued for BDOI business sign-off
header_title: Test Plan BRD-5 Accounting, Disbursement and ACSL
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal drafts of the two plans, from FRS BRD-5 Volumes 1 and 2 v1.0
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Comptrollership Head (pending)
    change: First issue for BDOI review, one plan per FRS volume, with the Excel workbooks of the same version
  - version: "2.0"
    date: 2 Oct 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Business sign-off pack: the two plans in one, traced to FRS v2.0 (one document); one Screen case and one Message case per screen of the screen specifications (chapter 13 of the FRS), a Screen ID on every case and the coverage by screen; FR, BRD and test IDs kept"
  - version: "2.1"
    date: 08 Oct 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Re-based on the WS Addendum v04152026 (minutes of 4-May-2026) with FRS v2.1: cases added for the CPC2 incentive tag of the booked transaction (FR-DS-090, 092) and for the service invoice with one line per qualified transaction (FR-DS-091). Status as of 08-Oct-2026"
distribution:
  - {name: "Ronald Allan E. De Leon, VP, Head - Comptrollership", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Financial Reporting and Budget Section (GL Officers, Team Lead, Section Head)", role: Business tester, organisation: BDOI, purpose: "Chart, journals, closing, bank reconciliation, reports, service fee"}
  - {name: "Disbursement (Processors, Team Leaders, Approvers)", role: Business tester, organisation: BDOI, purpose: "Payees, vouchers, instruments, end of day, funding, reports"}
  - {name: "Marketing (Processors, Reviewers, Approvers) and Human Resources", role: Business tester, organisation: BDOI, purpose: "Refund, cash-advance and check-cancellation requests"}
  - {name: "ACSL (Processors, Team Leader, Head)", role: Business tester, organisation: BDOI, purpose: "Reconciliations, cases, corrections, remittance deductions"}
  - {name: "Business Administrator, System Administrator, access approvers", role: Business tester, organisation: BDOI, purpose: "Lists of values, users and role requests"}
  - {name: "Cashiering, Remittance", role: Business tester, organisation: BDOI, purpose: "Refund validations, payment reversals, remittance payments"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, issue resolution, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan of BRD-5 in BIBS (BDOI Broker System, on iNXT BrokerVerse): the accounting of the Financial Reporting and Budget Section (FRBS) and the business and system administration (BASAU), Disbursement, the Marketing refund and cash-advance requests (Payment Requests) and the Accounting Controls and Subsidiary Ledger (ACSL). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `04_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_v2.0.xlsx`, which the testers use during execution.

Version 1.0 had one plan per FRS volume; this version combines them, as the FRS v2.0 combined the volumes. Every case traces to a functional requirement (FR) of the FRS BRD-5 v2.1 and to the BRD requirement IDs (FRBS, BASAU, DIS, MKT and ACSL n.n.n, Appendix A) that the FR meets, and to the screen of chapter 13 of the FRS it runs on. The expected results quote the messages, with their codes, as BIBS shows them.

## Scope

In scope are all 119 FRs of the FRS BRD-5 v2.1 and the BRD references they trace to:

- access and session warnings (FR-AC-001, 002; FR-DS-001);
- the monthly revaluation rate and the chart of accounts - maintenance, upload, numbering and short-code search (FR-AC-010 to 014);
- reports on demand - run, copy, column filters, batches and print options (FR-AC-020 to 023);
- posting through the accounting engine and manual entries - assignment, preparation, automatic reversal of accruals, validations, confirmation, posting and return, edit rules (FR-AC-030 to 037);
- the month-end GL close, the year-end close and its verification, the broking-books cut-off and the FX revaluation (FR-AC-040 to 043);
- bank file upload, automatic reconciliation and the unmatched reports (FR-AC-050, 051);
- the service fee, its liquidation, cost-centre derivation and early incentives as Other Income (FR-AC-052 to 055);
- the FRBS report pack, account schedules, Mancom and government outputs (FR-AC-060 to 063);
- lists of values and user and role management through approved requests (FR-AC-070 to 072);
- Disbursement: payees and their migration, requests from the modules, by e-mail and by upload, the workbench, the voucher with its entry and allocation, the seven modes of payment and the employee master (FR-DS-010 to 037);
- Disbursement: review, approval, rejection, cancellation and regularisation of vouchers; instrument statuses and edits, bank uploads, stale checks, clearing entries, OR / AR and CWT tags, BIR Form 2307 (FR-DS-040 to 058);
- Disbursement: end of day, DCTF, checks, confirmations, account funding, check series, bank accounts and reports; CPC2 and early incentives and the invoice family (FR-DS-060 to 093);
- Payment Requests: refund and cash-advance requests from Requests Home to disbursement and liquidation, with validation, four eyes, duplicate control and check cancellation (FR-PQ-001 to 017);
- ACSL: reports, insurer SOA upload and reconciliation, GL-SL reconciliation, cases, payment reversals, correction entries, remittance deductions and the invoice family (FR-AS-001 to 026);
- the 51 screens of the screen specifications, each with a Screen case (the screen as specified) and a Message case (the messages of the screen, word for word).

The roles-and-access sheet checks each action against the roles that may and may not perform it (the matrices of FRS section 3.3).

## Out of scope

- The real chart of accounts, posting rules, schedule layouts and GARD formats of BDOI (AQ01, AQ02, AQ05). The cases run on the seed chart and rules and are re-run when BDOI's data is loaded.
- eFPS, DAT and CAS files for the BIR, and the payroll returns (1601-C, 1604-C, HDMF, SSS, PhilHealth) (AQ06, AQ07).
- BDO single sign-on and Active Directory (Q42); daily FX revaluation, unless BDOI confirms it (AQ03).
- The Operations steps before Disbursement (remittance extraction, cashiering application) and the Cashiering side of the validations and payment reversals, beyond what the ACSL and Payment Requests cases need; they are in the BRD-2 Operations test plan.
- The bank interfaces (BDO Business Online Banking, the TPD transport of the DCTF) and the bank file layouts, which wait for AQ09; the cases use the minimal CSV uploads.
- The final layouts of the checks, vouchers, forms and RRF / RFP (AQ14, AQ18), of the CPC2 report and of the ACSL ageing reports; the cases run on the draft templates and the proposed layouts and are re-run once the layouts are confirmed.
- Performance and volume testing, including SOA files at the 20,000-row limit under load; this is the BIBS-wide performance test plan (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-5 (`02_BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_v2.1.docx`) | 2.1, 8 Oct 2026 |
| R2 | BRD-5 Accounting, Disbursement and ACSL with Addenda 1 and 2 | as signed |
| R3 | Test plan workbook BRD-5 (`04_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx`) | 2.1 |
| R4 | Sign-off workbook BRD-5 (`03_BIBS_Signoff_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx`) | 2.1 |
| R5 | Test cases and traceability BRD-2 Operations Cashiering, review workbook of the FRS in BDOI's format (`BIBS_RTM_BRD-02_Test_Cases_and_Traceability_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| System test | Every case of the workbook, screen by screen, with the seed data | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The scenarios run from start to finish by the persona that owns each step | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data | BDOI FRBS, Disbursement, Marketing, HR, ACSL and administration testers | After the entry criteria of section 3 are met |

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "A journal is never posted by its submitter nor above the poster's limit" or "The approver of a voucher is never its processor or checker".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path and Screen ID), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes it exactly with its code, as the system shows it.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit (the back-dated window, the poster's authorisation limit, the numbering width, the reverse-on date, the withholding tax against the gross, 50 lines of an RRF, 60 fieldwork days, 180 days to a stale check, 20,000 SOA rows, 200 correction lines).
- Each screen of the screen specifications has a **Screen case** (the screen shows the fields, actions and rules as specified) and a **Message case** (each message of the screen is shown word for word).
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow the month end from the broking cut-off to the GL close, a refund from the RRF to the paid voucher, or a correction from the case to the posted journal, in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and system functions are open only to the roles that hold the permission; maker-checker and four-eyes rules |
| Workflow | A status or stage change of a journal, account, close schedule, service-fee run, payee, request, voucher, instrument, funding, validation, case, correction or deduction |
| Report-output | Reports, report batches, schedules, the report pack, BIR outputs, end-of-day outputs, the DCTF, BIR Form 2307 and the SOA reconciliation report |
| Upload-download | Chart, request, payee, bank and SOA uploads; documents; payment confirmations; liquidation reports |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Screens, Test Data, Roles and Access. Case IDs carry their condition: TC-AC-036.3-01 is the first case of condition 3 of FR-AC-036; the Disbursement cases start with TC-DS, the Payment Requests cases with TC-PQ and the ACSL cases with TC-AS; the Screen and Message cases are TC-AC-SCR-nn and TC-AC-MSG-nn. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Issue ID.

Month-end, year-end and date-driven cases (close schedule, cut-off at 23:00, reverse-on date, year-end deadline, stale checks, end of day) depend on the date. The test lead sets the SIT clock or runs the scheduled jobs on demand from Scheduled Jobs, as the preconditions describe.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | BIBS is deployed on SIT with the seed data; the periods of the current year are open; the test mailboxes of payees and branches receive mail; this plan is reviewed by the iorta TechNXT project manager. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical issue; the Cashiering and Remittance steps used by the scenarios pass in the BRD-2 plan. |
| UAT | The FRS BRD-5 v2.1 is signed off or its open comments are agreed; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); BDOI testers have user IDs with the roles of section 5, including two Disbursement team leaders and two approvers for the funding cases; BDOI's chart and rules are loaded, or BDOI accepts the seed chart for UAT (AQ01, AQ02). |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High issue; open Medium and Low issues have an agreed fix date. |
| Persona end-to-end | All scenarios passed end to end; the trial balance is balanced after the month-end and year-end scenarios. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High issue; open issues listed with an agreed plan in the UAT sign-off; the sign-off of section 9 is signed. |

**Suspension.** Testing of a scenario stops when a Critical issue blocks it, when a period is closed by mistake and cannot be reopened, when the environment is down for more than half a day, or when the seed or UAT data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed profile (chart, rules, bookings, Operations, payees, vouchers, end of day, funding, service fee, deductions); SIT clock adjustable by the test lead; test mailboxes for payees and branches |
| UAT | Acceptance by BDOI testers | Masked copy of production-like balances, payees and requests plus the seed data; no real client, supplier, payee or employee names, TINs, bank account or check numbers or e-mail addresses |

Non-production data is always masked. Names, TINs, addresses, bank account and check numbers and e-mail addresses are replaced before data is loaded into SIT or UAT, and branch and payee e-mails go to test mailboxes, so no ATD, payment advice or statement can reach a real bank branch or payee. Bank files used in the reconciliation cases are prepared from the masked data, never from real bank statements, and the DCTF produced in testing is never forwarded to TPD.

## Named data sets

The cases refer to named data sets. Most are provided by the seed data; the copies with an error, the bank files, the upload and SOA files are prepared by the tester or the test lead as the case describes, and the Payment Requests are keyed in by the tester.

<!-- tp:data -->

Month-end cases close periods of the SIT company, and many cases change the state of a seed voucher, payee or deduction. The test lead runs them on a copy of the seed company or reloads the seed profile between cycles, because a closed fiscal year cannot be reopened; cases that change a parameter, an account or a master restore it at the end, as their expected result says.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, data, upload files, combined test users and the SIT clock; runs the jobs on demand; runs the daily issue triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise issues with evidence |
| Developers | iorta TechNXT | Resolve issues; support triage |
| BDOI department testers | BDOI FRBS, Disbursement, Marketing, HR, ACSL, Cashiering, Remittance, Business and System Administration | Run the UAT scenarios of their department; confirm the expected results match the business and accounting rules; raise issues |
| BDOI process owner | BDOI Comptrollership Head | Decides on disputed expected results and accepted issues; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

The GL team tests the entries, reconciliation, closing, service fee and reports (SC-AC scenarios); Disbursement the payees, vouchers, instruments, end of day, funding and reports (SC-DPA-01 to 05); Remittance the incentives and the invoice family (SC-DPA-06); Marketing and HR the refund and cash-advance requests (SC-DPA-07, 08); ACSL the reconciliations, cases, corrections and deductions (SC-DPA-09, 10); the Business and System Administrators and the access approvers the administration requests. The personas and SIT/UAT users are:

<!-- tp:personas -->

# Issue management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Issue severity" -->
| Severity | Definition | Example |
|---|---|---|
| Critical | A main flow cannot be completed, money is paid wrongly or twice, the ledger is wrong or unbalanced, or a security rule is broken | An unbalanced journal posts; a voucher is approved by its processor; a refund is paid twice for the same AR; a closed period accepts a posting |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | The accrual is not reversed on its date; the DCTF line is not 89 characters; a correction posts unbalanced |
| Medium | A function works with a workaround, or a message, label or report column is wrong but the figures are right | A report misses a column; a message names the wrong voucher or period |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise an issue for each failed case, with the case ID, steps, actual result, screenshot and the journal, run, period, voucher, request, case, correction or deduction number.
- The test lead triages new issues daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the system needs the correction. An issue found in a Cashiering or Remittance step is triaged with the BRD-2 test lead. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner, who records it in the clarifications chapter of the FRS.
- Target fix times on the test environments: Critical within 1 working day, High within 3 working days, Medium within the cycle, Low by agreement.
- A resolved issue is retested with its case and the cases of the same condition.

# Coverage summary

## Totals

<!-- tp:counts -->

Every FR has at least one positive and one negative case, and every BRD reference is covered. The coverage is checked each time the workbook is issued, and a plan with a gap is not issued.

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

<!-- tp:brd-coverage -->

## Scenarios

<!-- tp:scenarios -->

## Coverage by screen

Each screen of the screen specifications (FRS chapter 13) with its Screen case, its Message case and the cases that run on it.

<!-- tp:screens -->

## Roles and access

The table shows the actions checked per role. Each Y and N is one row of the Roles and Access sheet.

<!-- tp:access -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| BDOI's chart, posting rules and report layouts are not loaded before UAT (AQ01, AQ02, AQ05) | High | The cases run on the seed chart; the report and posting cases are re-run on BDOI's data once loaded, without a change to the system |
| Closing a period or a fiscal year on SIT cannot be undone | High | Month-end and year-end cases run on a copy of the seed company or after a reload of the seed profile |
| Bank layouts and interfaces are not given (AQ09); the uploads take a minimal CSV | Medium | The cases use the minimal CSV; the upload cases are re-run when BDOI gives the layouts |
| Document layouts and signatories are drafts (AQ14, AQ18) | Medium | The Report-output and Upload-download cases are re-run when the layouts are loaded |
| Four-eyes cases need combined test users and two users per role | Medium | The test lead creates them before the cycle (entry criterion) and removes them after it |
| Date-driven cases (close schedule, cut-off at 23:00, reverse-on date, year-end deadline, stale checks, end of day) need the clock | Medium | The test lead sets the SIT clock and runs the jobs on demand |
| BDOI answers to open questions change expected results (role matrix AQ28, service-fee rates AQ20, cost-centre rules AQ26, negative-balance accounts AQ30, approvers AQ18, deduction sources AQ23, CPC2 base AQ24) | Medium | The values are configuration; the affected cases name the rule, list or parameter and are re-run after the change |
| BDOI testers are not available in the UAT window | High | Agree named testers per department and dates in the UAT plan (deliverable 30) before UAT starts |

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the requirements of BRD-5 it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "Ronald Allan E. De Leon", role: "VP, Head - Comptrollership", organisation: BDOI}
  - {name: "Iris S. Marquez", role: "Product Owner - Comptrollership - FRBS and ACSL", organisation: BDOI}
  - {name: "Rodrigo R. Dela Cruz", role: "Product Owner - Comptrollership - Disbursement", organisation: BDOI}
  - {name: "Shellah Marie C. Miranda", role: "AVP, Product Owner - Marketing, Processing and Collections", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
