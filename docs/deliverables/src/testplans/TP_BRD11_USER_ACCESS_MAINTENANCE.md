---
# Word summary of the BRD-11 User Access Maintenance test plan. The tables marked <!-- tp:... --> are filled
# from brd11_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd11_cases.yaml
title: User Access Maintenance Test Plan
subtitle: BRD-11 User Access Maintenance - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-11
name: User Access Maintenance Summary
doc_id: BIBS-TP-BRD-11
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-11 User Access Maintenance
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-11 v1.0, the User Access build design and the built access functions
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Business Administrators (process owner)", role: Business tester, organisation: BDOI, purpose: "Requests, group-profile requests, reports"}
  - {name: "Unit Heads, Combank and Corbank Marketing / Corporate Processing", role: Business tester, organisation: BDOI, purpose: Approval and second approval}
  - {name: "ITIO-SRE and ITSD-AMS", role: System administration, organisation: BDO Unibank, purpose: "System Administrator functions, directory sign-in, logs"}
  - {name: "ES-BPDS, Business Project Services", role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-11 User Access Maintenance in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the Business Administrators, the approvers and the system administrators what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-11_User_Access_Maintenance_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-11 v1.0 and to the BRD requirement IDs (the printed BRD numbers 1.001 to 4.003, and the non-functional rows UAM-NFR-nn) that the FR meets.

Part of User Access Maintenance exists today: sign-in and lock-out, the session policy, the Users and Roles screens, one-step access requests and the User Access Matrix (FRS section 2.3, "the baseline"). The BRD-11 changes are designed and not yet built. The cases test the behaviour the FRS asks for after the change, so:

- expected results quote the message text of the FRS without a message code, also where the baseline already has a code; the codes are added in version 1.1 when the change is built;
- the automation reference column is blank; the existing access tests and those written during the build are referenced in version 1.1;
- screen paths are those of the build design (the new section User Access) and are checked against the screens before the system test starts.

## Scope

In scope are all 37 FRs of FRS BRD-11 v1.0 and the BRD IDs they trace to:

- sign-in with a persona profile, the inactivity warning and sign-out, directory sign-in with the Windows ID, log-out and the session log, the password policy (FR-UA-001 to 005);
- user access requests: draft, edit, cancel and submit; enrol, modify, deactivate and reactivate; the chosen approver; correction, cancellation, tracking, bulk requests and effective dates (FR-UA-010 to 020);
- review and approval: approve, reject, return, and the second approval of privileged and out-of-hours changes (FR-UA-030 to 034);
- group-profile requests with ordered approvers and their implementation by the System Administrator (FR-UA-040 to 045);
- the delivered profiles, the functions assignable to a profile and the user data (FR-UA-050 to 052);
- the four access reports and the insert-only access-change log (FR-UA-060 to 064);
- notifications and the e-mail of failed batch runs (FR-UA-070, 071).

The roles-and-access sheet checks each access-maintenance function against the roles that may and may not perform it (the matrix of FRS section 3.3).

## Out of scope

- Access only from BDO-issued devices; it is enforced by the BDO network and endpoint policy, not by BIBS (finding TF-UA-01).
- The EUA protocol itself (UQ04). The directory cases run against a test stub once BDO supplies the interface.
- An external access-control list and single session per device (UQ09, UQ14), which are parked seams.
- Temporary access with an end date (UQ06), which is not built.
- Portal users of Employee Benefits (user type External); they belong to the BRD-8 Employee Benefits test plan.
- The infrastructure rows of the BRD NFRs answered "Follow existing QPS set up"; they are covered by the BIBS deployment and security controls (deliverables 25 and 26).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-11 User Access Maintenance (`BIBS_FRS_BRD-11_User_Access_Maintenance_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | QPS User Access Maintenance Module BRD (`docs/source-documents/User Access Maintenance.pdf`) | v1, 15-Apr-2025; signed April-May 2025 |
| R3 | Test plan workbook BRD-11 (`BIBS_TestPlan_BRD-11_User_Access_Maintenance_v1.0.xlsx`) | 1.0 |
| R4 | User Access Maintenance build design (`docs/architecture/USER_ACCESS_DESIGN.md`) | proposal for review |
| R5 | FRS BRD-3 Product Maintenance (role-permission change requests, PMADD05) | 1.0 |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Request lifecycle and types, approver eligibility, four-eyes and second-approval rules, effective-date job, bulk validation, group-profile implementation, password policy, change log, permissions, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Baseline tests run today; new tests written during the build; all run on every change in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the SIT data sets | iorta TechNXT QA | After the BRD-11 change is deployed on SIT |
| Persona end-to-end | The eight scenarios from start to finish by the persona that owns each step, with notifications and e-mails checked | iorta TechNXT QA with the BDOI testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like user data | BDOI Business Administrators, approvers and system administrators | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, notifications and reports are what the business expects. When the automated tests exist, version 1.1 of the workbook names them in the automation reference column, and each such case is still run on the screen once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations and alternate flows became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "The requester and the subject user cannot decide the request".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes the FRS text.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit: 2 and 3 failed log-ins, 14 and 15 minutes of inactivity, a 9-character password, a password 90 days old, a reset link after 30 minutes, 1001 characters of remarks, a 41-character profile code, 17:59 and 21:00 for the out-of-hours flag.
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow a request from the draft to the applied change and the report in one sitting.
- The **roles-and-access** sheet lists, for each function, the roles that must be allowed and the roles that must be refused. It is run with one user per role; four-eyes cases use a combined test user.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission; four-eyes rules |
| Workflow | A status change of a user access request or a group-profile request (section 5 of the FRS) |
| Report-output | The access reports and the User Access Matrix; content checked against the screen |
| Upload-download | The bulk request template and file |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-UA-034.2-01 is the first case of condition 2 of FR-UA-034. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The BRD-11 change is built and deployed on SIT with its roles, parameters, lists and jobs; the planned demo seed V1960 is loaded; CI is green on the deployed commit; the automated tests exist and pass; screen labels and messages have been compared with this plan and differences recorded; the test mailboxes receive mail. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; the effective-date and password-expiry jobs run on SIT. |
| UAT | FRS BRD-11 v1.0 is signed off or its open comments are agreed; the answers to UQ01, UQ02, UQ05 and UQ07 are applied as configuration; the system test exit criteria are met; the UAT environment holds masked user data (section 4.1); BDOI testers have user IDs with the roles of section 5 and attended the walkthrough of the User Access screens. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run (the directory sign-in cases may be Blocked until UQ04 is answered); 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All eight scenarios passed end to end with the expected notifications, e-mails and change-log rows. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment is down for more than half a day, or when the test users are corrupted (for example all administrators locked). It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Demo profile seeds, the planned seed V1960 and the data sets of section 4.2; directory stub when available; test mailboxes |
| UAT | Acceptance by BDOI testers | Masked copy of production-like user data plus the test users; no real names, Windows IDs or e-mail addresses of BDO staff |

Non-production data is always masked. Full names, Windows IDs, e-mail addresses and mobile numbers of users are replaced before data is loaded into SIT or UAT, and user e-mail addresses point to test mailboxes. Test passwords are issued by the test lead and are never real directory passwords.

## Named data sets

The cases refer to named data sets. The BRD-11 change has no demo seed yet; the build design plans V1960 with the new users and requests in every status. Until it exists the test lead prepares each set on SIT as its source column says.

<!-- tp:data -->

Many cases change users and profiles (enrolment, deactivation, profile changes). The test lead reloads the seed between cycles, or testers run those cases on fresh test users created with the TD-UA-04 pattern.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, test users and the clock settings; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Write and keep the automated tests green; fix defects; provide the directory stub; support triage |
| Database administrator | iorta TechNXT with BDO Unibank ITIO | Runs the database checks of FR-UA-003 and FR-UA-064 and hands the evidence to QA |
| BDOI department testers | Business Administrators; Unit Heads as approvers; ITIO-SRE and ITSD-AMS as system administrators | Run the UAT scenarios of their role; confirm the expected results match the access policy; raise defects |
| BDOI process owner | Process Owner, User Access Maintenance | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | ES-BPDS, Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Requestors run the request scenarios (SC-UA-02, 03); approvers and second approvers run SC-UA-04; the Business Administrators run the group-profile requests and reports (SC-UA-05, 07); the system administrators run SC-UA-06 and the sign-in cases of SC-UA-01. The personas are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in User Access Maintenance |
|---|---|---|
| Critical | Access is granted without the required approval, a user reaches functions of a role not held, data is lost, or a log can be changed | A requester approves the own request; a deactivated user signs in; a change-log row can be deleted |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A scheduled change is not applied on its date; a privileged change skips the second approval |
| Medium | A function works with a workaround, or a message, label or report column is wrong but the data is right | The history misses a remark; a report shows the wrong "created by" |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the request number or user ID.
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

The table shows the functions checked per role. Each Y and N is one row of the Roles and Access sheet.

<!-- tp:access -->

## Automated tests referenced

<!-- tp:automation -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| The screens, labels or messages differ from the build design when built | Medium | Compare the screens with this plan before the system test; update the workbook to version 1.1 with the built texts and message codes |
| BDOI answers to open questions change expected results (UQ02 approver choice, UQ05 user ID format and lists, UQ07 privilege levels and working hours, UQ10 bulk rules, UQ16 profiles with members) | Medium | The values are parameters and lists; the affected cases name them and are re-run after the change without a new build |
| The EUA interface is not supplied (UQ04) | Medium | Directory sign-in cases stay Blocked; LOCAL mode is tested now |
| A test locks out the administrators of the environment | High | Keep a second administrator (admin2) outside the lock-out cases; the test lead can unlock through the database |
| Time-based cases (inactivity, token expiry, password age, effective dates, out-of-hours) need the clock to pass | Medium | The test lead shortens timers and moves dates in the test environment, as the preconditions describe |
| Existing roles lose functions when the new permissions are introduced | High | FR-UA-050 cases check that ACCESS_REQUEST holders keep their request functions; the BRD-1 to BRD-5 access checks are re-run in the same cycle |
| BDOI testers (14 Requestors and 8 Approvers, UQ01) are not named in time | High | Agree named testers per role in the UAT plan (deliverable 30) |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous or cannot be tested as written. The cases use the assumption stated in the proposal; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the User Access Maintenance requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: "Process Owner, User Access Maintenance", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank / Corbank Marketing and Corporate Processing", organisation: BDOI}
  - {name: "", role: "UAT coordinator, ES-BPS", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
