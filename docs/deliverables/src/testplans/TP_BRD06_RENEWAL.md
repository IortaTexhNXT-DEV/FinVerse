---
# Word summary of the BRD-6 Renewal test plan. The tables marked <!-- tp:... --> are filled
# from brd06_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd06_cases.yaml
title: Renewal Test Plan
subtitle: BRD-6 Renewal (RMEL Phase 2 Online Dispositioning, Addendum 1 and Workshop Addendum) - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-06
name: Renewal Summary
doc_id: BIBS-TP-BRD-06
version: "1.1"
date: 26 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-6 Renewal
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-6 v1.1 and the Renewal build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: BDOI Product Owner (pending)
    change: "FRS BRD-6 v1.1: cases for the go-live extraction and the RAs already sent (FR-RN-016) and the package remapping at sanitation (FR-RN-028); test data TD-RN-15, TD-RN-16"
distribution:
  - {name: "Product Owner, Renewal", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Unit Head, Combank and Corbank", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Retail and Corporate Marketing (Team Leaders, AOs)", role: Business tester, organisation: BDOI, purpose: "Extraction, assignment, disposition, review, letters"}
  - {name: "Processing (Team Leaders, Processing Officers)", role: Business tester, organisation: BDOI, purpose: "Processing, insurer round-trip, RAs, acceptance"}
  - {name: Marketing Business Services and System Support (MBS), role: Business tester, organisation: BDOI, purpose: "Lists of values, templates, rules and parameters"}
  - {name: "LAMD and Contact Center", role: Business tester, organisation: BDO Unibank / BDOI, purpose: "LAMD reports, follow-ups"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-6 Renewal in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-06_Renewal_v1.1.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-6 v1.1 and to the BRD requirement IDs (BRRN.nnn and the persona line IDs 1.001 to 6.002) that the FR meets. Renewal is **designed and not yet built**. The expected results therefore quote the message texts of the FRS without codes, the screen paths are those of the build design, and no case names an automated test yet. Where a Renewal check reuses a message that the platform already shows (log-in, bulk upload, account completeness), the case quotes the text BIBS shows today and a finding records the difference from the FRS (section 9).

## Scope

In scope are all 56 FRs of FRS BRD-6 v1.1 and every BRD ID they trace to:

- log-in, sessions, tabs, roles, data scope, profiles and the audit trail (FR-RN-001 to 004);
- extraction at the lead days and for any range, filters, the scrollable list, export, print and initiation (FR-RN-010 to 015);
- checks, the renewal reference, buckets, the decision matrix, non-renewable risk codes, LAMD reports, the KYC flag and endorsements on the mother policy (FR-RN-020 to 027);
- assignment, re-assignment and transfers between Marketing units (FR-RN-030 to 032);
- the AO's list, the record page, the account history, dispositions, remarks, returned accounts, re-open and the New Business path (FR-RN-040 to 048);
- TL review, return, post and the controlled overrides (FR-RN-050, 051);
- dispositioned-file upload, assignment to Processing Officers, the renewal account, computations and returns (FR-RN-060 to 065);
- the extract per insurer and the insurer responses (FR-RN-070, 071);
- Renewal Advices, NAL, NFR, NRNS reminders, non-acceptance letters, acceptance to booking and Contact Center follow-ups (FR-RN-080 to 085);
- the Submitted Policies hand-off (FR-RN-090);
- Renewal Home, the status report with its 34 counters, listings with escalation and the operational reports (FR-RN-100 to 103);
- lists of values, templates, rules and parameters (FR-RN-110 to 112).

The main BRD repeats most capabilities once per persona. The FRS traces its 1,032 line IDs in ranges (for example "1.009.2.1-40"); section 7.3 shows how every range is covered. The roles-and-access sheet checks each Renewal action against the roles that may and may not perform it (FRS section 3.3).

## Out of scope

- The nine BRD IDs that reference BDOIsys, EBIX or QPS (Addendum 1, p.34; FRS section 11.3). Negative cases confirm that no EBIX or QPS search or column is offered.
- Renewal of Employee Benefits programmes (BRD-8, decision D3). A case confirms that HMO lines are not extracted.
- Renewal of package products (BRD-3). Placement, issuance and booking of the renewal account are BRD-1 functions; the cases stop at the booked invoice of business type RENEWAL.
- Performance and volume testing beyond the two timing cases (20,000-row list, 30,000-row report). The NFRs of FRS section 8 are tested in the BIBS-wide performance test plan (deliverable 28).
- BDOI's own letter layouts, file layouts and password convention (RQ15, RQ20, RQ26, Q07). The cases use the draft layouts delivered with the build and are re-run when BDOI's layouts are loaded.

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-6 Renewal (`BIBS_FRS_BRD-06_Renewal_v1.1.docx`) | 1.1, 26 Sep 2026 |
| R2 | Renewal BRD pack: Workshop addendum, Addendum 1 and RMEL Phase 2 BRD (`docs/source-documents/Renewal (RN) BRD.pdf`) | BRD v1.0 9-May-2025; addenda 18-Nov-2025 and 8-Apr-2026 |
| R3 | Test plan workbook BRD-6 (`BIBS_TestPlan_BRD-06_Renewal_v1.1.xlsx`) | 1.1 |
| R4 | Renewal build design (`docs/architecture/RENEWAL_DESIGN.md`) | current |
| R5 | Cross-BRD decisions (`docs/requirements/BDOI_CROSS_BRD_DECISIONS.md`) | current |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.2.xlsx`) | 1.2 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Checks, bucket and matrix rules, workflow transitions, permissions, uploads and letters of each FR, against a PostgreSQL database | iorta TechNXT developers | Written with the build; every change in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the named data sets | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The twelve scenarios run from start to finish by the persona that owns each step, with notifications and e-mails checked in the test mailboxes | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data | BDOI Marketing, Processing, MBS, LAMD and Contact Center testers | After the entry criteria of section 3 are met |

The automation reference column is blank in this version. When the module is built, the developers name in the workbook the integration test that asserts each case, as was done for BRD-1 to BRD-3; a case with an automation reference is still run on the screen once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "A failed check never gives Clean, by rule or by override".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path of the design), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes the text; values filled in at run time (reference, row, check) are examples from the data sets.
- Every FR has at least one **positive** case and one **negative** case. Boundary cases test the values at and next to a limit: 200 and 201 characters of remarks, the third failed log-in, 30 and 20 days before expiry for the RA, 30 and 31 days after expiry for a re-open, 59 and 61 days for the at-risk alert, the financial-impact tolerance.
- Where the BRD asks for the same capability per persona (log-in, list, view, report, RA), the case is repeated for each persona and carries that persona's BRD IDs.
- **Scenarios** group the cases into business threads by persona, so a tester can follow a renewal from extraction to booking in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons, records and API calls are open only to the roles and data scope that allow them; maker-checker |
| Workflow | A stage transition of RNW_CASE, an insurer batch or a letter |
| Report-output | Lists, reports, exports, print previews and summary counters; content checked against the screen |
| Upload-download | Dispositioned files, insurer and LAMD files, letters and documents sent or downloaded |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-RN-043.2-01 is the first case of condition 2 of FR-RN-043. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The Renewal module is deployed on SIT with its jobs (RNW_EXTRACTION, RNW_REEVALUATE, RNW_NRNS_LETTERS, RNW_EXPIRY_SWEEP); the shared change BT0 (business type on the account) is deployed; CI is green on the deployed commit; the data sets of section 4.2 are loaded; the test mailboxes of clients and insurers receive mail; the developers have added the automation references to the workbook. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; notifications and the e-mail relay work on SIT; the Submitted Policies hand-off (BRD-12) is deployed for SC-RN-10. |
| UAT | FRS BRD-6 v1.1 is signed off or its open comments are agreed; the open questions that change expected results (RQ01, RQ08, RQ10, RQ15, RQ24) are answered or their test values agreed; the system test exit criteria are met; the UAT environment holds masked data; BDOI testers have user IDs with the roles of section 5. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All twelve scenarios passed end to end with the expected notifications, letters and e-mails. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment or the mail relay is down for more than half a day, or when the test data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Data sets of section 4.2, booked through BRD-1 on SIT; test mailboxes for clients and insurers |
| UAT | Acceptance by BDOI testers | Masked copy of production-like data plus the data sets; no real client names, TINs, PNs, addresses or e-mail addresses |

Non-production data is always masked. Client names, TINs, PN numbers, addresses, e-mail addresses and phone numbers are replaced before data is loaded into SIT or UAT, and insurer and client e-mail addresses point to test mailboxes, so no RA, NFR or insurer extract can reach a real client or insurer.

## Named data sets

Renewal is not built, so no seed provides the data yet. The test lead prepares each set on SIT as its source column says, mostly by booking policies through BRD-1 with expiry dates relative to the business date. When the module is built, the sets become a seed and this column names it.

<!-- tp:data -->

Many cases move a candidate to a later stage (post, send, accept). The test lead keeps a copy of TD-RN-02 per cycle, or re-books the invoices with the same values, so that each scenario starts from the stage its preconditions state. Extraction, the NRNS checkpoint, the RA notice days, the re-open limit, the escalation days and the expiry sweep depend on dates: the test lead sets the business date or books policies with expiry dates relative to it, and runs the jobs from Administration > Scheduled Jobs instead of waiting for the schedule.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments and data; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Write and keep the automated tests green; add the automation references; fix defects; support triage |
| BDOI department testers | BDOI Marketing, Processing, MBS; BDO LAMD; Contact Center | Run the UAT scenarios of their department; confirm the expected results match the business rules; raise defects |
| BDOI process owner | BDOI Product Owner, Renewal | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Each department tests the steps it owns: Marketing the extraction, assignment, transfer, disposition, review and letters (SC-RN-02, 04, 05, 06, 09); Processing the upload, processing, insurer round-trip and RAs (SC-RN-07, 08, 09); MBS the administration and rules (SC-RN-03, 12); LAMD and the Contact Center their uploads and follow-ups. The personas are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Renewal |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client or insurer, or a security rule is broken | An RA or insurer extract is e-mailed unprotected; a TL sees another unit's accounts; a failed check gives Clean |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | Extraction misses or duplicates an expiring policy; post is allowed with an open endorsement |
| Medium | A function works with a workaround, or a message, label or document is wrong but the data is right | A summary counter is wrong; a message names the wrong check |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the renewal reference.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the build is wrong. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (section 9).
- Target fix times on the test environments: Critical within 1 working day, High within 3 working days, Medium within the cycle, Low by agreement.
- A fixed defect is retested with its case and the cases of the same condition; the automated tests of the FR must also pass.

# Coverage summary

## Totals

<!-- tp:counts -->

Every FR has at least one positive and one negative case, and every BRD ID of the FRS is covered. The builder of the workbook checks this each time the plan is built and refuses to produce a plan with a gap.

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

The FRs trace 299 BRD references: the 40 BRRN IDs of the addenda, the persona line IDs and line-ID ranges of the main BRD as the FRS writes them (a range such as "1.009.2.1-40" stands for every line ID in it), and the cross-BRD references of FR-RN-090. The Coverage sheet of the workbook lists each of them with its positive and negative cases; every one has at least one case.

FRS section 11.2 maps each of the 1,032 line IDs of the main BRD to an FR, of which nine are out of scope. The table below groups them by BRD function and gives the number of cases of the FRs that meet them. Every in-scope line ID belongs to a reference that has at least one case; the report criteria (1.009.2.1-40 and its copies), the status columns (1.009.3.1.1-40), the 34 summary counters (1.009.3.2.1-34), the 28 insurer extract columns (3.009.1.4.1-28) and the ten non-renewal reasons (2.004.4.1-10) are each checked item by item in one case.

<!-- table: widths=6.6,1.8,1.4,4.2,1.6 caption="Main BRD line IDs by function (FRS section 11.2)" size=7.5 -->
| BRD function | Line IDs | Out of scope | FRs (FR-RN-) | Cases |
|---|---|---|---|---|
| 1.001 Log in | 4 | - | 001 | 13 |
| 1.002 Open in several tabs | 1 | - | 001 | 13 |
| 1.003 Generate the list of expiring accounts | 51 | 2 | 010, 011, 012, 013, 014, 024 | 41 |
| 1.004 View account | 8 | - | 041 | 8 |
| 1.005 Assign accounts | 10 | - | 030 | 7 |
| 1.006 Transfer account to other units | 5 | - | 031 | 7 |
| 1.007 Receive account transfer | 8 | - | 032 | 6 |
| 1.008 Review and post | 18 | - | 027, 041, 050 | 18 |
| 1.009 Generate report | 126 | - | 100, 101 | 12 |
| 1.010 Generate Renewal Advice | 16 | - | 080, 081 | 20 |
| 1.011 Override accounts | 4 | - | 051 | 8 |
| 2.001 Log in | 4 | - | 001 | 13 |
| 2.002 Open in several tabs | 1 | - | 001 | 13 |
| 2.003 View accounts for disposition | 10 | 1 | 040, 041 | 15 |
| 2.004 Provide disposition | 28 | - | 031, 043, 044, 047, 080 | 40 |
| 2.005 Transfer account to other units | 7 | - | 031 | 7 |
| 2.006 View the list of dispositioned accounts | 11 | 1 | 044, 045 | 8 |
| 2.007 Update the returned accounts | 11 | 1 | 046 | 4 |
| 2.008 Generate report | 125 | - | 101 | 8 |
| 2.009 Generate Renewal Advice | 17 | - | 080, 081 | 20 |
| 3.001 Log in | 4 | - | 001 | 13 |
| 3.002 Open in several tabs | 1 | - | 001 | 13 |
| 3.003 Generate list of accounts for processing | 46 | 3 | 010, 011, 012, 013, 014 | 35 |
| 3.004 Upload dispositioned file | 8 | - | 047, 060 | 14 |
| 3.005 Assign accounts | 11 | - | 061 | 5 |
| 3.006 View accounts for processing | 11 | - | 013, 041, 062 | 20 |
| 3.007 Update data on an account | 7 | - | 063 | 5 |
| 3.008 Review computations; return | 10 | - | 064, 065 | 7 |
| 3.009 Extract accounts per insurer | 43 | - | 070, 071, 103 | 26 |
| 3.010 Generate report | 129 | - | 100, 101 | 12 |
| 3.011 Generate Renewal Advice | 17 | - | 080, 081 | 20 |
| 4.001 Log in | 4 | - | 001 | 13 |
| 4.002 Open in several tabs | 1 | - | 001 | 13 |
| 4.003 View accounts for processing | 10 | 1 | 013, 041, 062 | 20 |
| 4.004 Upload dispositioned file | 6 | - | 060 | 8 |
| 4.005 Update data on an account | 7 | - | 063 | 5 |
| 4.006 Review computations; return | 10 | - | 064, 065 | 7 |
| 4.007 Extract accounts per insurer | 43 | - | 070, 071, 103 | 26 |
| 4.008 Generate report | 128 | - | 101 | 8 |
| 4.09 Generate Renewal Advice | 16 | - | 080, 081 | 20 |
| 5.001 Log in | 4 | - | 001 | 13 |
| 5.002 Open in several tabs | 1 | - | 001 | 13 |
| 5.003 Maintain lists of values | 5 | - | 110 | 4 |
| 5.004 Maintain users | 1 | - | 003 | 6 |
| 5.005 Update the RA template | 1 | - | 111 | 2 |
| 6.001 Access the application | 2 | - | 001 | 13 |
| 6.002 Manage users, profiles and functions | 32 | - | 003, 110, 111 | 12 |
| **Total** | **1,023** | **9** | | |

## Scenarios

<!-- tp:scenarios -->

## Roles and access

The table shows the actions checked per role. Each Y and N is one row of the Roles and Access sheet.

<!-- tp:access -->

## Automated tests

<!-- tp:automation -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| Renewal is not built; screen names, labels and messages may change at build | High | The cases name the design's screens and the FRS texts; the test lead updates the workbook from the build before the system test and records the changes in the document control |
| Open questions change expected results (RQ01 checks, RQ08 CBG, RQ10 counters, RQ15 insurer file, RQ24 matrix content) | High | The rules are configuration; the cases use the test values of TD-RN-08 and are re-run with BDOI's values without a new build |
| The Claims module is not connected (decision D4) | Medium | Claims cases expect "claims not connected"; the claim-driven cases (open claim gives Review) use a claims stub until Claims is built |
| Letter, LAMD and insurer file layouts are drafts (RQ15, RQ20, RQ26) | Medium | Upload-download and letter cases run on the draft layouts and are re-run when BDOI's layouts are loaded |
| Test mailboxes not reachable from SIT or UAT | High | Check the relay before the cycle (entry criterion); letter cases read the send log when the mailbox is down and are re-run later |
| Date-driven cases need the clock to pass | Medium | The test lead sets the business date and runs the jobs on demand (section 4.2) |
| Candidates are moved on by earlier cases | Medium | Keep one copy of the data per cycle; cases state the stage they start from |
| BDOI testers from Marketing, Processing, LAMD and the Contact Center are not available in the UAT window | High | Agree named testers per department and dates in the UAT plan (deliverable 30) before UAT starts |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from what the platform shows today. The cases follow the proposed resolution; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->



# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Renewal requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Renewal", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank and Corbank", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Head, Processing", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
