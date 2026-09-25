---
# Word summary of the BRD-3 Product Maintenance test plan. The tables marked <!-- tp:... --> are filled
# from brd03_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd03_cases.yaml
title: Product Maintenance Test Plan
subtitle: BRD-3 Product Maintenance (Package) and Workshop Addendum - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-03
name: Product Maintenance Summary
doc_id: BIBS-TP-BRD-03
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-3 Product Maintenance
h1_page_break: false
control:
  - version: "0.9"
    date: 23 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-3 v1.0 and the as-built code
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: Marketing Business Services and System Support (MBS), role: Business tester, organisation: BDOI, purpose: "Set-up, versions, masters, incentive criteria"}
  - {name: Technical Support Unit (TSU), role: Business tester, organisation: BDOI, purpose: "Review, negotiation, requirements, advisories, expiry"}
  - {name: "Retail, Corporate and Commercial Marketing", role: Business tester, organisation: BDOI, purpose: Requests and approvals}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-3 Product Maintenance in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-03_Product_Maintenance_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-3 v1.0 and to the BRD requirement IDs (BRPM.nnn, PMADDnn) that the FR meets. BRD-3 is built, so the expected results quote the messages and codes that BIBS returns, and each case names the automated test that already covers it, where one exists.

## Scope

In scope are all 39 FRs of FRS BRD-3 v1.0 and the 31 BRD IDs they trace to:

- access, roles, protected documents and audit (FR-PM-001 to 005);
- the product hierarchy, clause library, field rules, document templates and insurer terms (FR-PM-010 to 014);
- the package request and its approvals by Marketing, the TSU TL and the TSU Head (FR-PM-020 to 024);
- negotiation rounds, insurer outcomes, comparative master and client views, and the review of terms (FR-PM-030 to 036);
- requirements pack, ManCom sign-off, MBS set-up, validation and release, advisories and retirement (FR-PM-040 to 045);
- package versions, pricing on the latest rate scheme with rate exceptions, and the archive (FR-PM-050 to 052);
- expiry monitoring and renewal requests (FR-PM-060, 061);
- the Product Maintenance home, the three package reports, notifications and SLA control (FR-PM-070 to 073);
- incentive criteria on the products matrix and their use at booking (FR-PM-080, 081).

The roles-and-access sheet checks each Product Maintenance action against the roles that may and may not perform it (the matrix of FRS section 3.3).

## Out of scope

- Maintenance of non-package products (BRD revision of 27-Oct-2025, PQ19).
- Renewal of client policies; this is BRD-6. The renewal of the *package* is in scope.
- Computation and payout of incentives (Q33). The cases stop at the criteria codes stamped on the invoice.
- The transport of product master changes to other BDOI systems (PQ16). The cases check only the log entry of the product master feed.
- Performance and volume testing. The NFRs of FRS section 8 are tested in the BIBS-wide performance test plan (deliverable 28).
- BDOI's own document layouts and password convention (Q03, Q07). The cases test the draft layouts delivered with the build; they are re-run when BDOI's layouts are loaded.

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-3 Product Maintenance (`BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | Product Maintenance (Package) BRD and Workshop Addendum (`docs/source-documents/Product Maintenance.pdf`) | BRD v1.0 24-Nov-2025; addendum v1.1 10-Apr-2026 |
| R3 | Test plan workbook BRD-3 (`BIBS_TestPlan_BRD-03_Product_Maintenance_v1.0.xlsx`) | 1.0 |
| R4 | Product Maintenance build design (`docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md`) | current |
| R5 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Rules, validations, workflow transitions, permissions and documents of each FR, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Every change, in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the demo data | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The eight scenarios run from start to finish by the persona that owns each step, with notifications and e-mails checked in the test mailboxes | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data | BDOI MBS, TSU and Marketing testers | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, documents and e-mails are what the user expects. A case that names an automated test is still run on the screen at least once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "The preparer cannot approve the own quotation slip".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes it exactly with its code, as returned by the build.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit (shares of 100, the 1-365 day filter, SLA at 80 %).
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow a package from the request to the advisory in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission; four-eyes rules |
| Workflow | A stage transition, return or closure of PM_PACKAGE_REQUEST or of a package version |
| Report-output | Reports, comparative outputs and exports; content checked against the screen |
| Upload-download | Documents uploaded to a request and files downloaded or e-mailed from it |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-PM-020.2-01 is the first case of condition 2 of FR-PM-020. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The build is deployed on SIT with the demo profile (seeds V980 to V998); CI is green on the deployed commit; the test mailboxes of the four demo insurers receive mail; this plan is reviewed by the iorta TechNXT project manager. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; the notification and e-mail relay work on SIT. |
| UAT | FRS BRD-3 v1.0 is signed off or its open comments are agreed; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); BDOI testers have user IDs with the roles of section 5; the BDOI testers attended the walkthrough of the Product Maintenance screens. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All eight scenarios passed end to end with the expected notifications and e-mails. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment is down for more than half a day, or when the demo or UAT data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Demo profile seeds (V900-V998); test mailboxes for insurers and users |
| UAT | Acceptance by BDOI testers | Masked copy of production-like data plus the demo package requests; no real client names, TINs, addresses or e-mail addresses |

Non-production data is always masked. Client names, TINs, addresses, e-mail addresses and phone numbers are replaced before data is loaded into SIT or UAT, and insurer e-mail addresses point to test mailboxes, so no quotation slip or advisory can reach a real insurer or client.

## Named data sets

The cases refer to named data sets. For BRD-3 every set is provided by the demo seed of the build, so testers do not key in master data before they start; TD-PM-12 and TD-PM-13 are prepared by the tester as the case describes.

<!-- tp:data -->

Some cases change the state of a demo request (for example the sign-off of PKR-2026-900004). The test lead reloads the demo seed between cycles, or testers run those cases on a copy made with the TD-PM-12 values.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments and data; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Keep the automated tests green; fix defects; support triage |
| BDOI department testers | BDOI MBS, TSU, Marketing | Run the UAT scenarios of their department; confirm the expected results match the business rules; raise defects |
| BDOI process owner | BDOI Product Owner, MBS | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Each BDOI department tests the steps it owns: Marketing the requests, approvals and review of terms (SC-PM-03, part of SC-PM-04); TSU the review, negotiation, requirements, advisories and expiry (SC-PM-04, 05, 07); MBS the set-up, versions, masters and incentive criteria (SC-PM-02, 05, 06, 08). The personas and demo users are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Product Maintenance |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client or insurer, or a security rule is broken | A QS is e-mailed unprotected; a user without the permission approves a request; a released version prices quotations wrongly |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | Terms Final accepts a pending insurer; the advisory is sent without the signed slip |
| Medium | A function works with a workaround, or a message, label or document is wrong but the data is right | A message shows the wrong insurer name; a report column is missing |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the request or product number.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. Triage confirms the severity, links duplicates and decides whether the FRS or the build is wrong. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (see section 9).
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

## Automated tests referenced

Cases with an automation reference are covered by these existing tests, which run in CI on every change:

<!-- tp:automation -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| BDOI answers to open questions change expected results (PQ05 outcomes, PQ07 ManCom quorum, PQ08 SLA and approval chain, PQ09 checklist) | Medium | The values are configuration; the affected cases name the parameter or list, and are re-run after the change without a new build |
| BDOI document layouts and password convention not yet given (Q03, Q07) | Medium | Documents are tested on the draft layouts; the Upload-download and Report-output cases are re-run when the layouts are loaded |
| Demo data is changed by earlier cases (for example PKR-2026-900004 signed off) | Medium | Reload the demo seed between cycles; cases that change a demo request are marked in their preconditions |
| Test mailboxes of insurers not reachable from SIT or UAT | High | Check the relay before the cycle (entry criterion); QS and advisory cases read the E-mails tab when the mailbox is down and are re-run later |
| Time-based cases (SLA at 80 %, expiry at 60, 30 and 7 days) need the clock to pass | Medium | The test lead moves stage-entry times and end dates in the test database, as the preconditions describe |
| Four-eyes cases need a user holding two roles | Low | The test lead creates the combined test users listed in the preconditions and removes them after the cycle |
| BDOI testers are not available in the UAT window | High | Agree named testers per department and dates in the UAT plan (deliverable 30) before UAT starts |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from the build. The cases use the built behaviour; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Product Maintenance requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: "Head, Marketing Business Services and System Support", organisation: BDOI}
  - {name: "", role: "Head, Technical Support Unit", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
