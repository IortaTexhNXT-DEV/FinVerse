---
# Word summary of the BRD-7 Claims test plan. The tables marked <!-- tp:... --> are filled
# from brd07_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd07_cases.yaml
title: Claims Test Plan
subtitle: BRD-7 Motor and Non-Motor Claims Logging, Renumbering Addendum and Workshop Addendum - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-07
name: Claims Summary
doc_id: BIBS-TP-BRD-07
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-7 Claims
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-7 v1.0 and the Claims build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Product Owner, Claims", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Claims Unit Head", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Claims teams (Motor HO, Non-Motor HO, branches)", role: Business tester, organisation: BDOI, purpose: "Recording, handling, closure, follow-up"}
  - {name: "Retail and Corporate Marketing", role: Business tester, organisation: BDOI, purpose: Loss information and reports}
  - {name: "Operations, Remittance", role: Business tester, organisation: BDOI, purpose: Claims special remittance}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-7 Claims in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-07_Claims_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-7 v1.0 and to the BRD requirement IDs (BRCLM.001-043, the NFR items and the process and report pages) that the FR meets. The Claims module is **designed and not yet built**. The expected results therefore quote the message texts of the FRS without codes, the screen paths are those of the build design, and no case names an automated test yet. Where a Claims check reuses a message that the platform already shows (log-in, report parameters, e-mail address), the case quotes the text BIBS shows today and a finding records the difference from the FRS (section 9).

## Scope

In scope are all 39 FRs of FRS BRD-7 v1.0 and the 52 BRD references they trace to:

- log-in, role-based access and the claim history (FR-CL-001 to 003);
- the cover lookup, recording a claim, the reported date, policy number, Marketing data, cover version, premium check and authorization code (FR-CL-010 to 016);
- several locations and insurers per claim, insurer updates, insurer location references and the loss advice (FR-CL-020 to 024);
- claimant override, adjusters, the insurer reserve and catastrophe codes (FR-CL-030 to 033);
- statuses and the status matrix, settlement types, temporary and permanent closure, reopen and the claims special remittance (FR-CL-040 to 046);
- follow-up dates, the action plan, the diary, ageing, pending actions and Claims Home (FR-CL-050 to 055);
- the ageing, outstanding, settled, pending, loss, location, register and activity reports, the data extract and Marketing access (FR-CL-060 to 066).

The roles-and-access sheet checks each Claims action against the roles that may and may not perform it (FRS section 3.3).

## Out of scope

- The insurer-side claims function (reserving, claim payments, recoveries, posting). BDOI records the insurer's reserve and settlement as information; a case confirms that a reserve change produces no journal.
- Claim money through BDOI (CLQ10) and insurer portals or feeds for claims (CLQ17).
- Migration of EBIX / ISYS claims (CLQ14). Historical loss figures in the reports cover BIBS claims only.
- Performance and volume testing beyond the timing cases (premium check 5 seconds, Claims Home 5 seconds, status update 1 minute, report 5 minutes). The NFRs of FRS section 8 are tested in the BIBS-wide performance test plan (deliverable 28).
- BDOI's own loss advice layout and notification events (CLQ22). The cases use the draft layout.

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-7 Claims (`BIBS_FRS_BRD-07_Claims_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | Claims BRD pack: Workshop addendum, renumbering addendum and Motor and Non-Motor Claims Logging BRD (`docs/source-documents/Claims (CLM).PDF`) | BRD v1 22-Jan-2025; addenda 17-Dec-2025 and 8-Apr-2026 |
| R3 | Test plan workbook BRD-7 (`BIBS_TestPlan_BRD-07_Claims_v1.0.xlsx`) | 1.0 |
| R4 | Claims build design (`docs/architecture/CLAIMS_BROKING_DESIGN.md`) | proposal for review |
| R5 | BDOI Report List as of 27-Apr-2026 (Claims reports) | 27-Apr-2026 |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Premium check, status matrix, phases, ages, settlement rules, permissions, uploads and reports of each FR, against a PostgreSQL database | iorta TechNXT developers | Written with the build; every change in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the named data sets | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The eight scenarios run from start to finish by the persona that owns each step, with notifications and e-mails checked | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data | BDOI Claims, Marketing and Remittance testers | After the entry criteria of section 3 are met |

The automation reference column is blank in this version. When the module is built, the developers name the integration test that asserts each case; a case with an automation reference is still run on the screen once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions), for example "A status outside the matrix is refused even when called directly".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen, the preconditions and data set, numbered steps and the expected result with the message text where BIBS shows one. Values filled in at run time (claim number, dates) are examples from the data sets.
- Every FR has at least one **positive** and one **negative** case. Boundary cases test the limits: the third failed log-in, a reported date one day before the loss date, a zero reserve, 2,000 and 2,001 characters of action plan, follow-up days 365 and 366, age 30 on day 30, 80 and 95 days against the 90-day past due threshold, 2 and 3 claims for a claims-prone location, O/S at zero.
- **Scenarios** group the cases into business threads, from the notice of loss to closure and the reports.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and refused, from the matrix of FRS section 3.3.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit, and response times |
| Security-access | Screens, buttons, records and API calls are open only to the roles that hold the permission; the status matrix; maker-checker |
| Workflow | A phase or status change of the claim, closure, reopen, reassignment, a job run |
| Report-output | Reports and exports; content checked against the claims |
| Upload-download | Bulk uploads, the loss advice and report downloads |

## Reading the workbook

The workbook has a README sheet that explains every column. Case IDs carry their condition: TC-CL-045.2-01 is the first case of condition 2 of FR-CL-045. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The Claims module is deployed on SIT with its jobs (follow-up due, premium re-check, ageing alerts) and the feed CLAIMS_SPECIAL_REMIT; CI is green on the deployed commit; the data sets of section 4.2 are loaded; the insurer test mailboxes receive mail; the developers have added the automation references. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; notifications and the e-mail relay work on SIT; the Remittance screen of BRD-2 reads the feed. |
| UAT | FRS BRD-7 v1.0 is signed off or its open comments are agreed; the open questions that change expected results (CLQ01, CLQ04, CLQ05, CLQ06, CLQ15) are answered or their test values agreed; the system test exit criteria are met; the UAT environment holds masked data; BDOI testers have user IDs with the roles of section 5. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All eight scenarios passed end to end with the expected notifications and e-mails. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment is down for more than half a day, or when the test data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Data sets of section 4.2 on covers booked through BRD-1; insurer test mailboxes |
| UAT | Acceptance by BDOI testers | Masked copy of production-like data plus the data sets; no real client, claimant or third-party names, addresses or e-mail addresses |

Non-production data is always masked. Client, claimant and third-party names, addresses, e-mail addresses and phone numbers are replaced before data is loaded into SIT or UAT, and insurer claims e-mails point to test mailboxes, so no loss advice can reach a real insurer.

## Named data sets

Claims is not built, so no demo seed provides the data yet. The test lead prepares each set on SIT as its source column says; the ageing sets need reported dates in the past, which the test lead sets in the test database. When the module is built, the sets become a demo seed.

<!-- tp:data -->

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments and data; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Write and keep the automated tests green; add the automation references; fix defects |
| BDOI department testers | BDOI Claims, Marketing, Remittance | Run the UAT scenarios of their department; confirm the expected results; raise defects |
| BDOI process owner | BDOI Product Owner, Claims | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

The Claims teams test recording, handling, closure and follow-up (SC-CL-02 to 06); the Unit Head the set-up and reports (SC-CL-07, 08); Marketing the loss information (FR-CL-065); Remittance the special remittance (FR-CL-046). The personas are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Claims |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client or insurer, or a security rule is broken | The authorization code is issued on an unpaid cover; a Marketing user maintains a claim; a loss advice goes to the wrong insurer |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A status outside the matrix is accepted; ages are wrong; a closed claim can be edited |
| Medium | A function works with a workaround, or a message, label or document is wrong but the data is right | A report column is missing; a message names the wrong claim |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the claim number.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (section 9).
- Target fix times on the test environments: Critical within 1 working day, High within 3 working days, Medium within the cycle, Low by agreement.
- A fixed defect is retested with its case and the cases of the same condition; the automated tests of the FR must also pass.

# Coverage summary

## Totals

<!-- tp:counts -->

Every FR has at least one positive and one negative case, and every BRD ID is covered. The builder of the workbook checks this each time the plan is built and refuses to produce a plan with a gap.

## Coverage by FR

<!-- tp:coverage -->

## Coverage by BRD ID

<!-- tp:brd-coverage -->

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
| Claims is not built; screen names, labels and messages may change at build | High | The cases name the design's screens and the FRS texts; the test lead updates the workbook from the build before the system test |
| Open questions change expected results (CLQ01 code, CLQ04 matrix and units, CLQ05 closing types, CLQ06 closure rights and ageing, CLQ15 Marketing rights) | High | The matrix, lists and roles are configuration; the cases use the delivered defaults and are re-run with BDOI's values |
| Ageing and follow-up cases need past dates | Medium | The test lead sets reported and status dates in the test database and runs the jobs on demand |
| The Remittance feed and the BRD-1 endorsement events are needed by FR-CL-015, 016 and 046 | Medium | Run those cases after the BRD-2 remittance change is deployed; they are marked in their preconditions |
| Insurer test mailboxes not reachable from SIT or UAT | Medium | Check the relay before the cycle; loss advice cases read the send log when the mailbox is down and are re-run later |
| BDOI Claims testers from the branches are not available in the UAT window | Medium | Agree named testers and dates in the UAT plan (deliverable 30) |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from what the platform shows today. The cases follow the proposed resolution; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Claims requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Claims", organisation: BDOI}
  - {name: "", role: "Claims Unit Head", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
