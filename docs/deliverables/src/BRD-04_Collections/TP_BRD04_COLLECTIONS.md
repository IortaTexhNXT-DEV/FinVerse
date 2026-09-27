---
# Word summary of the BRD-4 Collections test plan. The tables marked <!-- tp:... --> are filled
# from brd04_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd04_cases.yaml
title: Collections Test Plan
subtitle: BRD-4 Collection Management System, renumbering addendum and Collections Addendum (Workshop) - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-04
name: Collections Summary
doc_id: BIBS-TP-BRD-04
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-4 Collections
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-4 v1.0 and the as-built code
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Marketing Head (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Head, Marketing (Corporate and Retail Section Heads)", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Collection Handlers and Collection Team Leads", role: Business tester, organisation: BDOI, purpose: "Worklist, dispositions, plans, promises, escalations, billing statements"}
  - {name: "Marketing AOs, Handlers and Team Leads", role: Business tester, organisation: BDOI, purpose: "Own accounts, bulk update, assignment, escalations"}
  - {name: Unapplied Payment Handlers, role: Business tester, organisation: BDOI, purpose: Unapplied payments and requests to Cashiering}
  - {name: "Operations (Cashiering, Commission Receivables)", role: Business tester, organisation: BDOI, purpose: "Hand-offs, collector requests, DP billing"}
  - {name: "Application Support, Business Administrator, DCO", role: Business tester, organisation: BDOI, purpose: "Set-up, audit log, job runs"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-4 Collections in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-04_Collections_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-4 v1.0 and to the BRD requirement IDs (BRCLXN.001-064, NFRs and the stakeholder functions of the CMS BRD) that the FR meets. Collections is built, so the expected results quote the messages and codes that BIBS returns, and each case names the automated test that already covers it, where one exists.

## Scope

In scope are all 52 FRs of FRS BRD-4 v1.0 and the BRD references they trace to:

- access, record keeping, the field-level audit log and its extraction, and the record lock (FR-CL-001 to 005);
- the outstanding PR worklist at invoice level - threshold, exclusions, totals, filters, net PR breakdown, daily refresh and history (FR-CL-010 to 018);
- assignment by rule and reassignment (FR-CL-020, 021);
- PR collector dispositions, hand-offs to Cashiering and Commission, efforts and bulk update (FR-CL-030 to 034);
- installment plans, payment allocation, promises to pay, policy and transaction views (FR-CL-040 to 044);
- escalation rules, automatic and manual escalation and the escalation workflow (FR-CL-050 to 053);
- billing statements per billing cycle, their sending and billing as monitoring only (FR-CL-060 to 062);
- unapplied payments - list, filters, disposition list, collector disposition, requests to Cashiering, invoice validation, history and the daily application file (FR-CL-070 to 077);
- scheduled reversal files, daily PR reports, exports and the Collections home (FR-CL-080 to 085);
- the commission receivable billing gate (FR-CL-090) and the four draft rows on commission and incentives (FR-CL-091 to 094).

The roles-and-access sheet checks each Collections action against the roles that may and may not perform it (the matrix of FRS section 3.3).

## Out of scope

- Posting of money. Collections posts no journal; payments, 2307 reversals, DP reversals and cancellations are tested in the BRD-2 Operations test plan. The Collections cases stop at the hand-off and at what the Operations screens show.
- Holds, special remittances, the send-schedule request and the endorsement slip (MKTID.001-009); they stay in Operations (BRD-2).
- The Marketing Diary kept in ISYS (CQ20) and the transport of the daily application file to the BDOI file server FS04 (OQ17, CQ12). The cases check the file in the in-system repository.
- FR-CL-091 to 094 as delivered functions. Their cases are written from the FRS and are marked "run when built"; they are run once BDOI confirms the draft addendum (CQ01) and the functions are delivered.
- The one-time migration of legacy EBIX open items (CQ07).
- Performance and volume testing, including the export cap under load; this is the BIBS-wide performance test plan (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-4 Collections (`BIBS_FRS_BRD-04_Collections_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | Collections (CLXN) BRD pack: CMS BRD, renumbering addendum, signed and draft Collections Addendum (`docs/source-documents/Collections (CLXN) BRD.pdf`) | CMS BRD v1 Jan-Mar 2025; addendum v1.0 10-Apr-2026 |
| R3 | Test plan workbook BRD-4 (`BIBS_TestPlan_BRD-04_Collections_v1.0.xlsx`) | 1.0 |
| R4 | Collections build design (`docs/architecture/COLLECTIONS_DESIGN.md`) | current |
| R5 | Test plan BRD-2 Operations (Cashiering, Commission) (`BIBS_TestPlan_BRD-02_Operations_v1.0.xlsx`) | 1.0 |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Refresh, threshold, dispositions, hand-offs, plans, promises, escalation rules, files and permissions of each FR, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Every change, in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the seed data | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The nine scenarios run from start to finish by the persona that owns each step, with the Cashiering and Commission steps run by their users | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data | BDOI Marketing, Collection and Operations testers | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, files and e-mails are what the user expects. A case that names an automated test is still run on the screen at least once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "An invoice is listed only when its total to collect is above CLX_MIN_BALANCE_THRESHOLD".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes it exactly with its code, as returned by the build.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit (threshold 10.00 and 10.01, 500 accounts per action, lock time 15 minutes, the promised amount against the outstanding).
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow an account from the refresh to the escalation or the hand-off in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission; four-eyes and reserved dispositions |
| Workflow | A status change of an account, hand-off, plan, promise, escalation, statement or collector request |
| Report-output | Scheduled files, reports, statements of account and exports; content checked against the screen |
| Upload-download | The Collections Bulk Update upload, file downloads and e-mailed statements |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-CL-031.2-01 is the first case of condition 2 of FR-CL-031. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

Several cases depend on time: the nightly refresh (22:15), the promise check (22:45), the escalation job (23:00), the daily application file (05:00), the weekly files (Friday 22:30) and the monthly files (first working day). The test lead runs these jobs on demand on SIT, or moves the dates in the test database, as the preconditions describe.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The build is deployed on SIT with the seed profile (seeds V980 to V1901 and the Collections seed runners); CI is green on the deployed commit; the Operations seed has booked the seed invoices; the test mailboxes receive mail; this plan is reviewed by the iorta TechNXT project manager. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; the Cashiering and Commission screens of BRD-2 pass their own entry criteria. |
| UAT | FRS BRD-4 v1.0 is signed off or its open comments are agreed; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); BDOI testers have user IDs with the roles of section 5; BDOI has given the disposition values, categories and escalation defaults it wants tested (CQ08, CQ14) or accepts the delivered placeholders. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run, except the "run when built" cases of FR-CL-091 to 094; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All nine scenarios passed end to end, with the hand-offs taken by Cashiering and Commission and the notifications received. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when a scheduled job fails on SIT and cannot be re-run, when the environment is down for more than half a day, or when the seed or UAT data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed profile migrations and runners (bookings, Operations, Collections worklist, plans, unapplied payments); test mailboxes |
| UAT | Acceptance by BDOI testers | Masked copy of production-like receivables plus the seed accounts; no real client names, TINs, addresses, account numbers or e-mail addresses |

Non-production data is always masked. Client and payor names, TINs, addresses, bank account and check numbers, e-mail addresses and phone numbers are replaced before data is loaded into SIT or UAT, and statement e-mails go to test mailboxes, so no statement of account can reach a real client.

## Named data sets

The cases refer to named data sets. For BRD-4 most sets are provided by the seeds and the Collections seed runners, so testers do not key in master data before they start; TD-CL-09 and TD-CL-10 are prepared by the tester or the test lead as the case describes.

<!-- tp:data -->

Many cases change the state of a seed account (a disposition, a promise, a plan cancelled). The test lead reloads the seed profile between cycles, or testers run those cases on an invoice booked with the TD-CL-09 values. Cases that change a parameter or a list reset it at the end, as their expected result says.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments and data; runs the jobs on demand; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Keep the automated tests green; fix defects; support triage |
| BDOI department testers | BDOI Marketing, Collections, Cashiering, Commission | Run the UAT scenarios of their department; confirm the expected results match the business rules; raise defects |
| BDOI process owner | BDOI Marketing Head (Section Heads) | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Each BDOI department tests the steps it owns: Collection Handlers and Team Leads the worklist, dispositions, plans, promises, escalations and statements (SC-CL-02 to 07); Marketing AOs and Team Leads their own accounts, bulk update, assignment and escalation handling (SC-CL-03, 04, 06); Unapplied Payment Handlers and Cashiering the unapplied payments (SC-CL-08); the Section Heads, Commission and Application Support the set-up, files, reports and CR billing (SC-CL-01, 09). The personas and SIT/UAT users are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Collections |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client or another unit, or a security rule is broken | The refresh drops open accounts; a statement is e-mailed unprotected; a user without the permission records a disposition |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A DP disposition does not reach the Commission DP list; a broken promise is not escalated |
| Medium | A function works with a workaround, or a message, label or file column is wrong but the data is right | A file column is missing; a message shows the wrong invoice number |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the invoice, ARN or reference.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the build is wrong. A defect found in a Cashiering or Commission step is triaged with the BRD-2 test lead. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (see section 9).
- Target fix times on the test environments: Critical within 1 working day, High within 3 working days, Medium within the cycle, Low by agreement.
- A fixed defect is retested with its case and the cases of the same condition; the automated tests of the FR must also pass.

# Coverage summary

## Totals

<!-- tp:counts -->

Every FR has at least one positive and one negative case, and every BRD reference is covered. The builder of the workbook checks this each time the plan is built and refuses to produce a plan with a gap.

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

The BRD references are read from the FR headers of the FRS. Some references there contain commas and are split into several entries (for example "B", "C", "Code", "draft)"); findings TF-CL-05 and TF-CL-06 ask the FRS owner to correct them.

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
| BDOI answers to open questions change expected results (threshold CQ03, disposition values and categories CQ08, escalation defaults CQ14, kept / broken rule CQ16, SOA layout and recipients CQ18) | Medium | The values are configuration; the affected cases name the parameter, list or rule and are re-run after the change without a new build |
| Time-based cases (nightly jobs, promise dates, file availability, lock expiry, SLA) need the clock to pass | Medium | The test lead runs the jobs on demand and moves dates in the test database, as the preconditions describe |
| Cashiering does not yet pull the PR 2307 hand-offs (FRS 1.6) | Medium | The cases check the pending item and the CLX_OUTBOX_STALE alert; the 2307 reversal itself is tested through Cashiering's upload in BRD-2 |
| Seed accounts are changed by earlier cases | Medium | Reload the seed profile between cycles; cases that change a seed account or a parameter say so in their preconditions or expected result |
| The draft addendum rows (BRCLXN.061-064) are not built | Low | Their cases are marked "run when built" and excluded from the exit criteria until BDOI confirms the draft |
| Test mailboxes not reachable from SIT or UAT | Medium | Check the relay before the cycle; statement cases read the E-mails Sent tab when the mailbox is down and are re-run later |
| BDOI testers are not available in the UAT window | High | Agree named testers per department and dates in the UAT plan (deliverable 30) before UAT starts |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from the build. The cases use the built behaviour; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Collections requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Head, Marketing", organisation: BDOI}
  - {name: "", role: "Section Head, Corporate Marketing", organisation: BDOI}
  - {name: "", role: "Section Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Head, Operations Services", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
