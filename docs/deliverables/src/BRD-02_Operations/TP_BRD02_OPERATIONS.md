---
# Word summary of the BRD-2 Operations test plan. The tables marked <!-- tp:... --> are filled
# from brd02_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd02_cases.yaml
title: Operations Test Plan
subtitle: BRD-2 Operations v1.01 (Cashiering, Remittance, Production Reconciliation, Adjustment / Cancellation, Commission Receivables) and the May 2026 annexes - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-02
name: Operations Summary
doc_id: BIBS-TP-BRD-02
version: "2.1"
date: 08 October 2026
status: Issued for BDOI business sign-off
header_title: Test Plan BRD-2 Operations
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-2 v1.0
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Operations Head (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
  - version: "2.0"
    date: 28 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Release set v2.0: cases re-traced to FRS v2.0 and its screen specifications (Screen ID on every case, Screens sheet); one screen case per screen and one message case per screen or dialog of the messages catalogue added. Status as of 28-Sep-2026"
  - version: "2.1"
    date: 08 Oct 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Re-based on FRS v2.1 (Operations BRD v1.01 and the May 2026 annexes): the cases of FR-OP-017 (check pick-up) and FR-OP-026 (Cashiering BIR 2307) retired with their FRs and the Check Pick-up screen case; cases rewritten for minimal balances at processing (FR-OP-023), the PR 2307 reversal of Marketing Collection (FR-OP-113, 121) and Commission Receivables (FR-OP-090 to 095); cases added for the payment history, audit trail, extraction source and pending endorsements, draft schedule, early incentive, return to Marketing, recompute inputs, justification limits, ageing, peso tolerance, annotations, classification and the new FR-OP-099 (ageing); new IDs BRQID.007, RMTID.041 and MKTID.014-019 traced"
distribution:
  - {name: "Head, Operations Services", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: Cashiering, role: Business tester, organisation: BDOI, purpose: "Receipts, payment files, application, unapplied items, minimal balances"}
  - {name: Remittance, role: Business tester, organisation: BDOI, purpose: "Extraction, batches, approval, insurer ORs, incentives"}
  - {name: Production Reconciliation, role: Business tester, organisation: BDOI, purpose: "Registers, insurer feedback, matching, closure"}
  - {name: Adjustment / Cancellation, role: Business tester, organisation: BDOI, purpose: "Endorsement requests, recompute, posting"}
  - {name: Commission Receivables, role: Business tester, organisation: BDOI, purpose: "Direct payment, incentives, BIR certificates"}
  - {name: "Marketing, Comptrollership and Disbursement", role: Business tester, organisation: BDOI, purpose: "Holds, special remittances, the PR 2307 reversal from tag to release, returned endorsement requests, certificates, payment requests"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, issue resolution, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-2 Operations in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the BDOI Operations teams what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases are in the Excel workbook of the same version, `04_BIBS_TestPlan_BRD-02_Operations_v2.1.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-2 v2.1 and to the BRD requirement IDs (BRQID, CSHID, RMTID, ADJID, PRCID, CMRID, MKTID, DBMID) that the FR meets. The expected results quote the messages, with their codes, as BIBS shows them.

## Scope

In scope are all 82 FRs of FRS BRD-2 v2.1, grouped as in the FRS. FR-OP-017 (check pick-up, CSHID.009) and FR-OP-026 (Cashiering processing of BIR 2307, CSHID.026 and 027) are removed from scope by the new BRD version, and their test conditions TC-OP-017.1-017.2 and TC-OP-026.1-026.3 are retired; FR-OP-099 (ageing of commission receivables) is new:

- access and invoice ledger: log-in, role-based access, the Operations home, the invoice ledger, Invoice 360, invoice locks, co-insurance shares, batch run reports and report access (FR-OP-001 to 009);
- Cashiering: receipt series, ARs and ORs, cancellation and reinstatement, payment files, PDC warehouse, matching and application by component, pre-booked payments, AR Insurance, commission ORs, unapplied dispositions, minimal balances cleared at processing, search and the audit trail, batch printing, accounting entries and reports (FR-OP-010 to 028);
- Remittance: extraction with its source and duplicate prevention, eligibility including pending financial endorsements, batch review and exclusion, processing, draft and issued schedule and payment request, approval, Disbursement tracking, insurer OR upload, early remittance incentive (RMTID.041), search, special remittances and reports (FR-OP-030 to 041);
- Adjustment / Cancellation: requests, duplicates, documents, validation, approval and return to Marketing, recompute per insurer, TSI above the package limit, posting, excess and AR Insurer, justification and over-adjustment limits, minimal balance file, slips, search, ageing to the posting date and reports (FR-OP-050 to 062);
- Production Reconciliation: schedules, manual extracts, register file, sending, feedback upload, matching within the peso-equivalent tolerance, unbooked production, annotations, feedback and pairing, the check of the early incentive applied by insurers, closure and reports (FR-OP-070 to 080);
- Commission Receivables: DP entries from the tagging, read-only review, rule-based exclusion and eligibility from the fully paid status, billing and Statement of Account, insurer answers and account status, collection and PR reversal, SLA incentives, BIR certificates, estimated items, classification, reports and invoice-level ageing (FR-OP-090 to 099);
- Marketing collection items: schedule sending, holds, special remittance requests, the PR 2307 reversal from tag to routing, and endorsement slips (FR-OP-110 to 114);
- the Disbursement queue and the release of the 2307 certificates without posting (FR-OP-120, 121) and the interfaces, flow-in feeds and New Business payment gate (FR-OP-130 to 132).

The roles-and-access sheet checks each Operations action against the roles that may and may not perform it (the grants of FRS section 3.3).

## Out of scope

- The Collection system, the Disbursement system and the GL mapping specification (OQ01, OQ02, OQ07). BRD-4 and BRD-5 test plans cover their replacements; this plan tests the hand-offs, extracts and the in-app Disbursement queue that stand in until then.
- Electronic transfers (file transfer, system-to-system interfaces, shared drive folders) to insurers, BDO bank channels and the Marketing and Claims systems (OQ17, OQ22, OQ29). Files are uploaded by hand and sent by e-mail in this phase, and the cases test that route.
- Payout of incentives to branches (CLR-OP-58, OQ39). The cases stop at the posted incentive.
- Maintenance of products and incentive criteria (BRD-3), booking of new accounts (BRD-1) and renewal (BRD-6). This plan uses booked invoices as given.
- Performance and volume testing (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-2 Operations (`02_BIBS_FRS_BRD-02_Operations_v2.1.docx`) | 2.1, 08 Oct 2026 |
| R2 | Operations BRD v1.01 and the Operations Central Addendum 1 annexes of May 2026 (file Operations_WS Addendum) | BRD v1.01 15-Apr-2026; annexes 8 to 21-May-2026 |
| R3 | Test plan workbook BRD-2 (`04_BIBS_TestPlan_BRD-02_Operations_v2.1.xlsx`) | 2.1 |
| R6 | Test plan BRD-1 New Business (booking, payment gate) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| System test | Every case of the workbook, screen by screen, with the seed data | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The nine scenarios run from start to finish by the persona that owns each step, including the e-mails in the test mailboxes, the Disbursement queue and the journal | iorta TechNXT QA with the BDOI team testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data, including payment files and insurer feedback in BDOI's own volumes | BDOI Operations, Marketing, Comptrollership and Disbursement testers | After the entry criteria of section 3 are met |


## How the cases were derived

- Each FR's acceptance criteria, business rules, validations and alternate flows became one or more **test conditions** (sheet Test Conditions).
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, the steps and the expected result. Where BIBS shows a message, the expected result quotes it with its code, as the system shows it; the parts that BIBS fills in (an invoice number, a batch, a status) are written in angle brackets.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses it). Boundary cases test values at and next to a limit: the reconciliation tolerance of 1.00, the 98 % application limit of 2 % CWT clients, the minimal balance range, 50 invoices per request, due dates in working days.
- **Scenarios** (sheet Scenarios) group the cases into business threads by team, from a payment to a remitted and reconciled invoice.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and system functions are open only to the roles that hold the permission; four-eyes rules |
| Workflow | A stage transition, approval, return or closure of a receipt, batch, request, cycle, billing or payment request |
| Report-output | Reports, registers, schedules and exports; content checked against the screen |
| Upload-download | Payment files, insurer files, DP lists, registers, slips and protected e-mails |
| Screen | The screen matches its specification in FRS v2.1 chapter 13: fields, order, labels, mandatory markers, defaults, lists and buttons (TC-OP-SCR-nn, one per screen) |
| Message | Every error, validation and warning message of one screen or dialog, word for word with its code (TC-OP-MSG-nn, from the messages catalogue of FRS v2.1 chapter 15) |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Screens, Test Data, Roles and Access. Case IDs carry their condition: TC-OP-075.1-01 is the first case of condition 1 of FR-OP-075. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Issue ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | BIBS is deployed on SIT with the seed data; the test mailboxes of the four seed insurers and the Operations users receive mail; the seed accounting rules of the Operations events are active; this plan is reviewed by the iorta TechNXT project manager. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical issue; the e-mail relay and the scheduled jobs (remittance extraction, production register extraction, hold expiry, DP feedback follow-up, minimal balance sweep, flow-in feeds) run on SIT. |
| UAT | FRS BRD-2 v2.1 is signed off or its open comments are agreed; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); BDOI testers have user IDs with the roles of section 5; Comptrollership has confirmed the GL accounts of the Operations events for UAT (OQ07); receipt series match BDOI's BIR registration (OQ05). |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High issue; open Medium and Low issues have an agreed fix date. |
| Persona end-to-end | All nine scenarios passed end to end, including balanced journals for receipts, remittances, adjustments and DP collections, and the protected e-mails to insurers. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High issue; open issues listed with an agreed plan in the UAT sign-off; the sign-off of section 9 is signed. |

**Suspension.** Testing of a scenario stops when a Critical issue blocks it, when the environment or the e-mail relay is down for more than half a day, or when the seed or UAT data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed data; test mailboxes for insurers and users |
| UAT | Acceptance by BDOI testers | Masked copy of production-like data plus the seed records; no real client names, TINs, addresses, account or check numbers, or e-mail addresses |

Non-production data is always masked. Client and payor names, TINs, addresses, bank account and check numbers, e-mail addresses and phone numbers are replaced before data is loaded into SIT or UAT, and insurer e-mail addresses point to test mailboxes, so no register, schedule, billing or certificate can reach a real insurer. Payment files used in UAT are masked copies; no file produced in UAT is sent to a bank.

## Named data sets

The cases refer to named data sets. Sets TD-OP-01 to TD-OP-07, 09 and 10 come from the seed data; TD-OP-08 (the files) is prepared by the test lead as the Test Data sheet describes.

<!-- tp:data -->

Many cases move a seed record to its next stage (for example the approval of the batch in review). The test lead reloads the seed between cycles, and each tester works on the records assigned to them in the test schedule so that two testers do not move the same record.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, data and files; runs the daily issue triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise issues with evidence |
| Developers | iorta TechNXT | Resolve issues; support triage |
| BDOI team testers | BDOI Cashiering, Remittance, Production Reconciliation, Adjustment, Commission Receivables, Marketing, Comptrollership, Disbursement | Run the UAT scenarios of their team; confirm the expected results match the business rules; raise issues |
| BDOI process owner | BDOI Head, Operations Services | Decides on disputed expected results and accepted issues; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Each team tests the steps it owns: Cashiering SC-OP-02 and 03; Remittance SC-OP-04; Marketing SC-OP-05; Adjustment SC-OP-06; Production Reconciliation SC-OP-07; Commission Receivables SC-OP-08; Disbursement and the System Administrator SC-OP-09; the Auditor SC-OP-01. The personas and SIT/UAT users are:

<!-- tp:personas -->

# Issue management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Issue severity" -->
| Severity | Definition | Example in Operations |
|---|---|---|
| Critical | A main flow cannot be completed, money or data is wrong in a way that reaches a client, an insurer or the ledger, or a security rule is broken | A receipt number is issued twice; a remittance is paid twice; a journal does not balance; a user approves his own batch or hold |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A payment is applied outside the component hierarchy; an ineligible invoice is extracted; the tolerance is not applied |
| Medium | A function works with a workaround, or a message, label, document or report is wrong but the data is right | A report column is missing; a message names the wrong field |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise an issue for each failed case, with the case ID, steps, actual result, screenshot and the invoice, receipt, batch, request or cycle number.
- The test lead triages new issues daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the system needs the correction. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner, who records it in the clarifications chapter of the FRS.
- Target fix times on the test environments: Critical within 1 working day, High within 3 working days, Medium within the cycle, Low by agreement.
- A resolved issue is retested with its case and the cases of the same condition.

# Coverage summary

## Totals

<!-- tp:counts -->

Every FR has at least one positive and one negative case, and every BRD ID is covered. The coverage is checked each time the workbook is issued, and a plan with a gap is not issued.

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

<!-- tp:brd-coverage -->

## Scenarios

<!-- tp:scenarios -->

## Coverage by screen

Every screen of the FRS v2.1 screen specifications has its screen case and the cases that start on it. A case is linked to the screen where the tester starts; the steps may go on to other screens.

<!-- tp:screens -->

## Roles and access

The table shows the actions checked per role. Each Y and N is one row of the Roles and Access sheet.

<!-- tp:access -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| BDOI answers to open questions change expected results (OQ15 disposition approvals, OQ17 remittance types, OQ22 OR tolerance, OQ23 incentive rates, OQ29 schedules, OQ30 match keys, OQ39 incentive tiers, OQ48 permission matrix) | Medium | The values are parameters, lists or rules; the affected cases name them and are re-run after the change without a change to the system |
| Production GL accounts of the Operations events are not yet given (OQ07) | High | SIT uses the seed accounting rules; UAT waits for the Comptrollership accounts (entry criterion) |
| The Collection and Disbursement systems are not known (OQ01, OQ02) | Medium | The cases test the in-app queue and hand-off extracts; interface cases are added when the systems are named |
| Test mailboxes or the e-mail relay not reachable from SIT or UAT | High | Check the relay before the cycle; e-mail cases read the Outbound Messages log when the mailbox is down and are re-run later |
| Time-based cases (hold expiry, 10 working-day SLA, monthly schedules, holidays, check clearing) need the clock to pass | Medium | The test lead moves dates in the test environment or runs the jobs by hand, as the preconditions describe |
| Payment files and insurer feedback in BDOI's volumes are not tested before UAT | Medium | System test uses the files of TD-OP-08; UAT includes one file per type in the expected daily or monthly volume |
| BDOI testers are not available in the UAT window | High | Agree named testers per team and dates in the UAT plan (deliverable 30) before UAT starts |

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Operations requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Head, Operations Services", organisation: BDOI}
  - {name: "", role: "Team Head, Cashiering", organisation: BDOI}
  - {name: "", role: "Team Head, Remittance", organisation: BDOI}
  - {name: "", role: "Team Head, Production Reconciliation", organisation: BDOI}
  - {name: "", role: "Team Head, Adjustment", organisation: BDOI}
  - {name: "", role: "Team Head, Commission Receivables", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
