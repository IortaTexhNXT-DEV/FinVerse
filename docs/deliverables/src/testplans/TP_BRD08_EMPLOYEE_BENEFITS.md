---
# Word summary of the BRD-8 Employee Benefits test plan. The tables marked <!-- tp:... --> are filled
# from brd08_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd08_cases.yaml
title: Employee Benefits Test Plan
subtitle: BRD-8 Employee Benefits and Addendum, including the insurer and client portal - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-08
name: Employee Benefits Summary
doc_id: BIBS-TP-BRD-08
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-8 Employee Benefits
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-8 v1.0 and the Employee Benefits build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Product Owner, Employee Benefits", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Marketing, Employee Benefits teams", role: Business tester, organisation: BDOI, purpose: "Programmes, RA, BOR, franchise, TOR, proposals, comparative, placement trigger"}
  - {name: Processing, role: Business tester, organisation: BDOI, purpose: "Placement, booking, SOA, member changes"}
  - {name: "Collections and Marketing Support", role: Business tester, organisation: BDOI, purpose: Billing and payment tracking}
  - {name: BDO Information Security, role: Reviewer, organisation: BDO Unibank, purpose: Portal security cases}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-8 Employee Benefits (EB) in BIBS (BDOI Broker System, on iNXT BrokerVerse), including the partner portal for insurers and client HR users. It tells the BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-08_Employee_Benefits_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-8 v1.0 and to the BRD requirement IDs (BRID-001 to 030, 005.01-005.03, 022.01 and the TAT annex) that the FR meets. The EB module and the portal are **designed and not yet built**. The expected results therefore quote the message texts of the FRS without codes, the screen paths are those of the build design, and no case names an automated test yet. Where an EB check reuses a message that the platform already shows (attachment rules, client duplicate, workflow reason, report parameters), the case quotes the text BIBS shows today and a finding records the difference from the FRS (section 9).

## Scope

In scope are all 39 FRs of FRS BRD-8 v1.0 and the 34 BRD references they trace to:

- access by department, document links and access classes, audit and versions, protected outbound files (FR-EB-001 to 004);
- the partner portal: separate realm, provisioning through User Access, staged uploads, review, the insurer task inbox and proposal form, the client HR portal (FR-EB-010 to 015);
- client capture, programmes and cycles with the business type, the automatic RA, client feedback and the incumbent's indicative proposal (FR-EB-020 to 024);
- documents, BOR, franchise, the client advice, required documents and the TOR (FR-EB-030 to 035);
- proposals, the comparative, the value threshold, presentation, revisions and the client's confirmation with the placement trigger (FR-EB-040 to 046);
- work assignment, issuance tracking, booking with the billing number check, SOA, roster, member changes, direct billing and monitored items (FR-EB-050 to 057);
- the EB reports, the business type filter, Word export, TAT monitoring and EB Home (FR-EB-060 to 062).

The roles-and-access sheet checks each EB and portal action against the roles that may and may not perform it (FRS section 3.3).

## Out of scope

- BRID-028, documents for high-risk accounts (out of scope per the addendum, p.13).
- The insurer system-to-system API, e-signature verification of the BOR, HRIS master list feeds and reading insurer mailboxes (parked seams, FRS section 7).
- EB renewals in the general Renewal module (decision D3); the BRD-6 test plan confirms that HMO lines are not extracted.
- Penetration testing of the internet-facing portal. The portal security cases here check the functional rules; the penetration test is part of BDO Information Security's approval (EBQ13).
- Performance and volume testing (deliverable 28), and BDOI's own templates and report layouts (EBQ08, EBQ21).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-8 Employee Benefits (`BIBS_FRS_BRD-08_Employee_Benefits_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | Employee Benefits BRD and Addendum (`docs/source-documents/Employee Benefits.pdf`) | BRD v1.0 14-Nov-2025; addendum 23-Feb-2026 |
| R3 | Test plan workbook BRD-8 (`BIBS_TestPlan_BRD-08_Employee_Benefits_v1.0.xlsx`) | 1.0 |
| R4 | Employee Benefits build design (`docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md`) | proposal for review |
| R5 | User Access Maintenance build design (`docs/architecture/USER_ACCESS_DESIGN.md`) | proposal for review |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Cycle workflow, portal realm and scoping, staged uploads, threshold rules, billing number check, roster validation and reports of each FR, against a PostgreSQL database | iorta TechNXT developers | Written with the build; every change in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen and portal page by portal page, with the named data sets | iorta TechNXT QA | Before UAT, on the SIT environment |
| Persona end-to-end | The seven scenarios run from start to finish by the persona that owns each step, including the insurer and client HR users in the portal | iorta TechNXT QA with the BDOI department testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data; portal steps run by BDOI testers acting as insurer and client users | BDOI EB Marketing, Processing and Collection testers | After the entry criteria of section 3 are met |

The automation reference column is blank in this version. When the modules are built, the developers name the integration test that asserts each case; a case with an automation reference is still run on the screen once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations, alternate flows and field rules became one or more **test conditions** (sheet Test Conditions), for example "A portal user exists only after a User Access request of type External is approved by someone other than the requester".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen or portal page, the preconditions and data set, numbered steps and the expected result with the message text where BIBS shows one. Values filled in at run time (numbers, rows) are examples from the data sets.
- Every FR has at least one **positive** and one **negative** case. Boundary cases test the limits: the third failed portal sign-in, the 10 MB upload limit, a rule amount of zero, 2 and 3 working days for the franchise advice, 4 and 5 working days for follow-ups, 5 and 7 working days against the franchise TAT.
- Segregation of duties is tested for each maker-checker pair: the comparative maker and the signatory, the AO and the threshold approver, the portal user requester and approver.
- **Scenarios** follow one programme (EBP-2026-000010) from the RA to placement and servicing, and a new-business prospect through BOR and franchise.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, documents, portal records and API calls are open only to the roles and party that allow them; segregation of duties |
| Workflow | A stage transition of the EB cycle, franchise, member change, SOA or portal upload; a job run |
| Report-output | Reports, the comparative and exports; content checked against the data |
| Upload-download | Portal and internal uploads, protected e-mails and downloads |

## Reading the workbook

The workbook has a README sheet that explains every column. Case IDs carry their condition: TC-EB-046.2-01 is the first case of condition 2 of FR-EB-046. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The EB module and the portal are deployed on SIT, with the RA and follow-up jobs; the shared change BT0 and the User Access External request type are deployed; a virus scanner adapter is active for portal uploads; CI is green; the data sets of section 4.2 are loaded; test mailboxes receive mail; the developers have added the automation references. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; portal users of TD-EB-02 are provisioned; the e-mail relay and one-time codes work on SIT. |
| UAT | FRS BRD-8 v1.0 is signed off or its open comments are agreed; the open questions that change expected results (EBQ02, EBQ05, EBQ07, EBQ11, EBQ13) are answered or their test values agreed; BDO Information Security has approved the portal hosting for UAT; the UAT environment holds masked data; BDOI testers have user IDs with the roles of section 5. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All seven scenarios passed end to end with the expected notifications, portal tasks and e-mails. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment or the portal is down for more than half a day, or when the test data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA; the portal on its own address | Data sets of section 4.2; test mailboxes for HR contacts and insurers |
| UAT | Acceptance by BDOI testers, portal in the zone approved by BDO Information Security | Masked copy of production-like data plus the data sets; no real employee names, birth dates or health data |

Non-production data is always masked. Master lists and rosters hold personal data and utilization reports are health-related, so employee names, employee numbers and birth dates are replaced, utilization reports are synthetic, and HR and insurer e-mail addresses point to test mailboxes. No roster in a test environment holds health data (EBQ15).

## Named data sets

EB is not built, so no demo seed provides the data yet. The test lead prepares each set on SIT as its source column says; portal users are provisioned in scenario SC-EB-02 and reused by the later scenarios. When the modules are built, the sets become a demo seed.

<!-- tp:data -->

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, data and portal users; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios, including the portal steps; raise defects with evidence |
| Developers | iorta TechNXT | Write and keep the automated tests green; add the automation references; fix defects |
| BDOI department testers | BDOI EB Marketing, Processing, Collection | Run the UAT scenarios of their department; act as insurer and client HR users in the portal; raise defects |
| BDOI process owner | BDOI Product Owner, Employee Benefits | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

EB Marketing tests the programme, portal provisioning, BOR, franchise, TOR, proposals and placement trigger (SC-EB-02 to 05); Processing the placement, booking, SOA and member changes (SC-EB-06); Collection the billing and payment tracking (FR-EB-053, 056). The personas are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Employee Benefits |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client or insurer, or a security rule is broken | A portal user sees another insurer's request or another client's roster; a master list leaves BDOI unprotected; a staged upload changes the roster |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | Placement is triggered without the threshold approval; a duplicate billing number is booked |
| Medium | A function works with a workaround, or a message, label or document is wrong but the data is right | A report column is missing; a notification goes to the wrong AO |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the programme or cycle number.
- The test lead triages new defects daily with the development lead and, during UAT, the BDOI process owner. A disputed expected result goes to the BDOI process owner; an FRS change goes to the FRS owner (section 9). Portal security defects are also reported to BDO Information Security.
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
| EB and the portal are not built; screen names, labels and messages may change at build | High | The cases name the design's screens and the FRS texts; the test lead updates the workbook from the build before the system test |
| The portal hosting, multi-factor method and virus scanner are not approved (EBQ13) | High | Run the portal cases on SIT with the e-mail code and the scanner adapter; repeat the security cases in the approved zone before UAT |
| Open questions change expected results (EBQ02 RA lead time, EBQ05 BOR on renewal, EBQ07 franchise, EBQ11 thresholds) | High | The values are parameters and rules; the cases use the defaults and demo values and are re-run with BDOI's values |
| Personal and health-related data in master lists and utilization reports | High | Only masked and synthetic files are used (section 4.1); the test lead checks each file before upload |
| BDOI testers must act as insurer and client users | Medium | The test lead provisions one portal user per role and party and briefs the testers on the portal |
| Time-based cases (RA lead time, reminders, franchise TAT, follow-ups) need the clock to pass | Medium | The test lead sets the business date and runs the jobs on demand |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous, cannot be tested as written, or differs from what the platform shows today. The cases follow the proposed resolution; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Employee Benefits requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Employee Benefits", organisation: BDOI}
  - {name: "", role: "Head, Employee Benefits Marketing", organisation: BDOI}
  - {name: "", role: "Head, Processing", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
