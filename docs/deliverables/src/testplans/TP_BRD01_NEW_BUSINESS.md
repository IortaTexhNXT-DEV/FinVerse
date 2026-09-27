---
# Word summary of the BRD-1 New Business test plan. The tables marked <!-- tp:... --> are filled
# from brd01_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd01_cases.yaml
title: New Business Test Plan
subtitle: BRD-1 New Business (Fire, Motor, Other Lines and Non-Package) and Workshop Addendum - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-01
name: New Business Summary
doc_id: BIBS-TP-BRD-01
version: "2.0"
date: 26 September 2026
status: Issued for BDOI business sign-off
header_title: Test Plan BRD-1 New Business
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-1 v1.0 and the as-built code
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
  - version: "2.0"
    date: 26 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Release set v2.0: cases re-traced to FRS v2.0 and its screen specifications (Screen ID on every case, Screens sheet); one screen case per screen and one message case per screen or dialog of the messages catalogue added. Status as of 26-Sep-2026"
distribution:
  - {name: "Product Owner, BDOI", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Retail, Commercial, Corporate and Institutional Marketing", role: Business tester, organisation: BDOI, purpose: "Clients, quotations, PRFs, accounts"}
  - {name: Technical Support Unit (TSU), role: Business tester, organisation: BDOI, purpose: "Non-package placement, TSU routing and clearance"}
  - {name: Processing, role: Business tester, organisation: BDOI, purpose: "Validation, payment, placement, issuance, booking"}
  - {name: Comptrollership, role: Business tester, organisation: BDOI, purpose: "Booking entries, service invoices"}
  - {name: Marketing Business Services and System Support (MBS), role: Business tester, organisation: BDOI, purpose: "Product rules, lists of values, templates"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-1 New Business in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-01_New_Business_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-1 v1.0 and to the BRD requirement IDs (BRNB.nnn) and BRD sections that the FR meets. New Business is built, so the expected results quote the messages and codes that BIBS returns, and each case names the automated test that already covers it, where one exists.

## Scope

In scope are all 93 FRs of FRS BRD-1 v1.0, grouped as in the FRS:

- product lines, product-specific fields and validations, intake templates and TSU routing (FR-NB-001 to 005);
- common controls: stages and history, work queues, void, document protection, approval before sending, notifications, audit, documents, bulk uploads and the authoritative source of data (FR-NB-010 to 020);
- client onboarding and KYC: search, create, update, duplicates, KYC verification and confirmation, bulk clients, tags and instructions, client 360 and the KYC review list (FR-NB-030 to 038);
- package quotation: requests, quotations and versions, approval, sending, acceptance, bulk quotations and the ARN (FR-NB-040 to 047);
- non-package placement: PRF, Marketing approval, TSU queue, quotation slip, insurer terms, comparative table, proposal slip and acceptance (FR-NB-050 to 057);
- accounts: search, creation, premium of Appendix A, duplicates, submission and validation, bulk accounts, contacts, returns, FFY and direct payment (FR-NB-060 to 069);
- placement and hold cover, CLPC billing, payment reports and the payment gate (FR-NB-080 to 092);
- issuance: e-policy receipt, extraction, document triggers, Insurance Advice and e-policy dispatch (FR-NB-100 to 106);
- booking: individual, batch, automatic and direct booking, multi-year, endorsements, cancellation, service invoices, incentive indicator and cost center (FR-NB-110 to 119);
- the NB dashboard and reports (FR-NB-120 to 126);
- administration: log-in and session, browser tabs, lists of values, approvals, profiles, access requests, audit log and retention (FR-NB-130 to 137).

The roles-and-access sheet checks each New Business action against the roles that may and may not perform it (the matrix of FRS section 3.3).

## Out of scope

- Override requests (BRD 1.1.12, 1.2.9, 2.1.14; OOS-1): Renewal only, tested in the BRD-6 plan.
- QPS-specific references (OOS-2). The ARN and the Placement Summary replace them and are tested.
- Maintenance of package products, versions and incentive criteria: BRD-3 test plan. This plan uses the catalogue as a given.
- Renewal of accounts (BRD-6), post-issuance adjustments beyond the endorsements of BRNB.061 / 081 (BRD-2) and collection of premium (BRD-4).
- Computation and payout of incentives (Q33). The cases stop at the incentive indicator on the invoice.
- External feeds to BDOI systems (Q08), the CIF interface (Q16), SFTP and API placement channels (Q06) and BIR CAS transmission: the cases check only that the parked seams behave as the FRS states.
- Performance and volume testing (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-1 New Business (`BIBS_FRS_BRD-01_New_Business_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | New Business BRD pack (`docs/source-documents/New Business (NB) BRD.pdf`) | Addendum signed Apr-2026; Other Lines Dec-2025; Fire and Motor V06162025 |
| R3 | Test plan workbook BRD-1 (`BIBS_TestPlan_BRD-01_New_Business_v1.0.xlsx`) | 1.0 |
| R4 | New Business requirements traceability (`docs/requirements/BDOI_NB_TRACEABILITY.md`) | current |
| R5 | Test plan BRD-3 Product Maintenance (catalogue, versions, incentive criteria) | 1.0 |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Rules, validations, workflow transitions, rating, booking entries, permissions and documents of each FR, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Every change, in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the seed data | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The ten scenarios run from start to finish by the persona that owns each step, including the e-mails in the test mailboxes and the booking journal | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data, including bulk files in BDOI's own volumes | BDOI Marketing, TSU, Processing, Comptrollership and MBS testers | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, documents, e-mails and journal entries are what the user expects. A case that names an automated test is still run on the screen at least once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions).
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes it with its code, as returned by the build; the parts that BIBS fills in (an ARN, a count, a list of fields) are written as they will appear for the named data or in angle brackets.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses it). Boundary cases test values at and next to a limit: minimum age 18, 50 files per upload, 10 MB per file, multi-year term of 2 years, DST rounding, fleet of 5 vehicles.
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, from a request to a booked invoice.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission; four-eyes rules |
| Workflow | A stage transition, return or closure of NB_CLIENT, NB_QUOTATION, NB_PROPOSAL or NB_ACCOUNT |
| Report-output | Reports, registers, comparative tables and exports; content checked against the screen |
| Upload-download | Bulk uploads, documents, e-policies, billing and payment files, downloads and protected e-mails |
| Screen | The screen matches its specification in FRS v2.0 chapter 13: fields, order, labels, mandatory markers, defaults, lists and buttons (TC-NB-SCR-nn, one per screen) |
| Message | Every error, validation and warning message of one screen or dialog, word for word with its code (TC-NB-MSG-nn, from the messages catalogue of FRS v2.0 chapter 15) |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Screens, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-NB-061.3-01 is the first case of condition 3 of FR-NB-061. The Screen ID column of Test Cases links each case to the screen specification of FRS v2.0 (SCR-NB-01 to SCR-NB-46); the Screens sheet lists the cases of each screen. Screen and message cases are numbered TC-NB-SCR-nn and TC-NB-MSG-nn and trace to the first FR of their screen. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The build is deployed on SIT with the seed profile (seeds V980 to V989); CI is green on the deployed commit; the test mailboxes for clients and the four seed insurers receive mail; the booking accounting rule of the seed (V988) is active; this plan is reviewed by the iorta TechNXT project manager. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; e-mail relay, scheduled jobs (MAIL_DISPATCH, BOOKING_BATCH, HOLD_COVER_EXPIRY, KYC_REVIEW_DUE, PAYMENT_CONFIRMATION_SWEEP) run on SIT. |
| UAT | FRS BRD-1 v1.0 is signed off or its open comments are agreed; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); BDOI testers have user IDs with the roles of section 5; Comptrollership has confirmed the GL accounts of the booking rule for UAT (OQ07). |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All ten scenarios passed end to end, including the protected e-mails and a balanced booking journal. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment or the e-mail relay is down for more than half a day, or when the seed or UAT data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed profile migrations (V900-V989); test mailboxes for clients, insurers and users |
| UAT | Acceptance by BDOI testers | Masked copy of production-like data plus the seed records; no real client names, TINs, addresses, loan or PN numbers, plate or engine numbers, or e-mail addresses |

Non-production data is always masked. Client names, TINs, IDs, addresses, e-mail addresses, phone numbers, PN and loan numbers and vehicle identifiers are replaced before data is loaded into SIT or UAT, and client and insurer e-mail addresses point to test mailboxes, so no quotation, slip, e-policy or service invoice can reach a real client or insurer. CLPC billing files produced in UAT are not sent to BDO Unibank.

## Named data sets

The cases refer to named data sets. Sets TD-NB-01 to TD-NB-10 are provided by the seed of the build; TD-NB-11 to TD-NB-14 are prepared by the test lead as the Test Data sheet describes.

<!-- tp:data -->

Many cases move a seed record to its next stage (for example the validation of ARN-2026-900002). The test lead reloads the seed between cycles, and each tester works on the records assigned to them in the test schedule so that two testers do not move the same record.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, data and bulk files; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Keep the automated tests green; fix defects; support triage |
| BDOI department testers | BDOI Marketing, TSU, Processing, Comptrollership, MBS | Run the UAT scenarios of their department; confirm the expected results match the business rules; raise defects |
| BDOI process owner | BDOI Product Owner | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Each BDOI department tests the steps it owns: Marketing the clients, quotations, PRFs and accounts (SC-NB-03, 04, 06); TSU the non-package placement and TSU clearance (SC-NB-05); Processing the payment, placement, issuance and booking (SC-NB-07, 08, 09); Comptrollership the booking entries and service invoices (SC-NB-09); MBS and the Business Administrator the product rules, lists of values and templates (SC-NB-01, 02). The personas and SIT/UAT users are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in New Business |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client, insurer or the ledger, or a security rule is broken | A quotation is e-mailed unprotected; an account is booked twice; the booking journal does not balance; a user approves his own quotation |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A duplicate account is accepted; the payment gate opens without payment for CBG Motor; the premium differs from Appendix A |
| Medium | A function works with a workaround, or a message, label, document or report is wrong but the data is right | A report column is missing; a message names the wrong field |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the ARN, QT, PRF or client code.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the build is wrong. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (section 9).
- Target fix times on the test environments: Critical within 1 working day, High within 3 working days, Medium within the cycle, Low by agreement.
- A fixed defect is retested with its case and the cases of the same condition; the automated tests of the FR must also pass.

# Coverage summary

## Totals

<!-- tp:counts -->

Every FR has at least one positive and one negative case, and every BRD ID is covered. The builder of the workbook checks this each time the plan is built and refuses to produce a plan with a gap. The BRD ID count includes the BRD sections that some FRs cite instead of a BRNB ID (for example "Appendix A" and "BRD 2.1.16"); see finding TF-NB-05.

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

<!-- tp:brd-coverage -->

## Scenarios

<!-- tp:scenarios -->

## Coverage by screen

Every screen of the FRS v2.0 screen specifications has its screen case and the cases that start on it. A case is linked to the screen where the tester starts; the steps may go on to other screens.

<!-- tp:screens -->

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
| BDOI answers to open questions change expected results (Q04 TSU thresholds, Q05 approval chain, Q07 password convention, Q14 bulk sanitisation, Q23 file types, Q26 placement prerequisites, Q27 hold cover expiry, Q30 advice trigger, Q35 multi-year basis) | Medium | The values are parameters, lists or rules; the affected cases name them and are re-run after the change without a new build |
| Production GL accounts of the booking entry are not yet given (OQ07) | High | SIT uses the seed booking rule; UAT waits for the Comptrollership accounts (entry criterion) |
| Seed records are moved by earlier cases | Medium | Reload the seed between cycles; assign records to testers in the test schedule |
| Test mailboxes or the e-mail relay not reachable from SIT or UAT | High | Check the relay before the cycle; e-mail cases read the Outbound Messages log when the mailbox is down and are re-run later |
| Time-based cases (idle session, SLA breach, stalled 5 days, hold cover expiry, multi-year year 2, monthly KYC job) need the clock to pass | Medium | The test lead moves stage-entry times and dates in the test database or runs the jobs by hand, as the preconditions describe |
| Bulk files in BDOI's volumes (thousands of rows) are not tested before UAT | Medium | System test uses the files of TD-NB-11; UAT includes one file per upload type in the expected monthly volume |
| Four-eyes cases need a user holding two roles | Low | The test lead creates the combined test users listed in the preconditions and removes them after the cycle |
| BDOI testers are not available in the UAT window | High | Agree named testers per department and dates in the UAT plan (deliverable 30) before UAT starts |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from the build. The cases use the built behaviour; the FRS owner decides the correction in the FRS.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the New Business requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, BDOI", organisation: BDOI}
  - {name: "", role: "Head, Marketing", organisation: BDOI}
  - {name: "", role: "Head, Technical Support Unit", organisation: BDOI}
  - {name: "", role: "Head, Processing", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
