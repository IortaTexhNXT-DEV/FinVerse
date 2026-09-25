---
# Word summary of the BRD-9 Customer Servicing Facility test plan. The tables marked <!-- tp:... --> are filled
# from brd09_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd09_cases.yaml
title: Customer Servicing Facility Test Plan
subtitle: BRD-9 Customer Servicing Facility (CSF) - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-09
name: Customer Servicing Facility Summary
doc_id: BIBS-TP-BRD-09
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-9 Customer Servicing Facility
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-9 v1.0 and the Customer Servicing build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Product Owner, Customer Servicing Facility", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: Alternative Distribution Head, role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: BDO Insure Contact Center Management, role: Business tester, organisation: BDOI, purpose: "Search, servicing view, contact changes, resends, reports"}
  - {name: Marketing Business Services and System Support (MBS), role: Business tester, organisation: BDOI, purpose: Status mapping and contact rules}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-9 Customer Servicing Facility (CSF) in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the BDO Insure Contact Center and the other BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-09_Customer_Servicing_Facility_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-9 v1.0 and to the BRD requirement IDs (BRCSF-nnn, the process steps nnn.nnn, and the e-mail items CSF-EM07 and CSF-EM09) that the FR meets.

The CSF is designed and not yet built. The cases are written from the FRS and the build design, so:

- expected results quote the message text of the FRS without a message code; the codes are added in version 1.1 when the module is built;
- the automation reference column is blank; the automated tests written during the build are referenced in version 1.1;
- screen paths and button labels are those of the build design and are checked against the screens before the system test starts.

## Scope

In scope are all 17 FRs of FRS BRD-9 v1.0 and the BRD IDs they trace to:

- log-in and role-based access for agents, supervisors and management (FR-CSF-001, 002);
- the Customer Search by name, client ID, account number, PN number and application number (FR-CSF-010);
- the Servicing View with client information, accounts, CSF status and payment history (FR-CSF-011 to 013);
- verification of the caller, contact-only update and the legacy sync outbox for QPS and EBIX (FR-CSF-020 to 022);
- renewal advice and e-policy resend, document upload and retrieval (FR-CSF-030 to 033);
- the audit trail, the CSF reports, the agent activity log and the 15-minute backup (FR-CSF-040 to 043).

The roles-and-access sheet checks each CSF action against the roles that may and may not perform it (the matrix of FRS section 3.3).

## Out of scope

- Case management and inquiry logging, deferred by the e-mail of 13-Feb-2026 (CSF-EM10).
- Changes other than contact details (name, civil status, ID); the cases check only that BIBS refuses them and names the fulfilment unit.
- The transport of contact changes to QPS and EBIX and the lookup of accounts that exist only there (CSQ01, CSQ02). The cases check the outbox with sync disabled; the sending cases run only when a test interface exists.
- The production of renewal advices, e-policies and claims reports; they belong to BRD-6, BRD-8, BRD-1 and BRD-7. The CSF cases read and resend them.
- Performance and volume testing beyond the response times named in the FRs. The NFRs of FRS section 8 are tested in the BIBS-wide performance test plan (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-9 Customer Servicing Facility (`BIBS_FRS_BRD-09_Customer_Servicing_Facility_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | Customer Servicing Facility BRD and e-mail thread (`docs/source-documents/Customer Servicing Facility.PDF`) | BRD v1.0 09-Jun-2025; e-mail 13-Feb-2026 |
| R3 | Test plan workbook BRD-9 (`BIBS_TestPlan_BRD-09_Customer_Servicing_Facility_v1.0.xlsx`) | 1.0 |
| R4 | Customer Servicing Facility build design (`docs/architecture/CUSTOMER_SERVICING_DESIGN.md`) | proposal for review |
| R5 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Search keys, status mapping, verification rules, contact-only update, outbox, resend and upload checks, permissions, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Written during the build; run on every change in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the SIT data sets | iorta TechNXT QA | After the CSF is deployed on SIT |
| Persona end-to-end | The eight scenarios from start to finish by the persona that owns each step, with e-mails checked in the test mailboxes | iorta TechNXT QA with the Contact Center testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by Contact Center testers on masked production-like data | BDO Insure Contact Center agents, supervisors and management | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, e-mails and reports are what the agent expects. When the automated tests exist, version 1.1 of the workbook names them in the automation reference column, and each such case is still run on the screen once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations and alternate flows became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "A verification with at least 2 of the 4 checks matched passes".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes the FRS text.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit: 2 and 3 search characters, 1 and 2 verification matches, 29 and 31 minutes after a verification, 12 months and 1 day of payment history, 2 and 3 failed log-ins.
- **Scenarios** (sheet Scenarios) group the cases into the threads of a servicing contact, so a tester can follow a call from the search to the resend in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission |
| Workflow | A contact change, a resend or an unlock that moves a record to its next state |
| Report-output | Reports and exports; content checked against the screen |
| Upload-download | Documents uploaded to the client or an account and files downloaded or previewed |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-CSF-020.2-01 is the first case of condition 2 of FR-CSF-020. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | The CSF is built and deployed on SIT with its roles, parameters and lists; CI is green on the deployed commit; the automated tests of the CSF exist and pass; screen labels and messages have been compared with this plan and differences recorded; the data sets of section 4.2 are loaded; the test mailboxes receive mail. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; renewal advices of at least one source module (or loaded RENEWAL_ADVICE documents) are available. |
| UAT | FRS BRD-9 v1.0 is signed off or its open comments are agreed; the answers to CSQ03, CSQ04 and CSQ10 are applied as configuration; the system test exit criteria are met; the UAT environment holds masked data (section 4.1); Contact Center testers have user IDs with the roles of section 5 and attended the walkthrough of the CSF screens. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run (the legacy sync cases may be Blocked until CSQ01 is answered); 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All eight scenarios passed end to end with the expected e-mails and activity log rows. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment is down for more than half a day, or when the test data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Demo profile seeds, the planned CSF demo seed V1940, and the data sets of section 4.2; test mailboxes |
| UAT | Acceptance by Contact Center testers | Masked copy of production-like data plus the test clients; no real client names, TINs, addresses, e-mail addresses or phone numbers |

Non-production data is always masked. Client names, government IDs, addresses, e-mail addresses and phone numbers are replaced before data is loaded into SIT or UAT. The registered e-mail of every test client points to a test mailbox, so no renewal advice or e-policy resent during testing can reach a real client.

## Named data sets

The cases refer to named data sets. The CSF has no demo seed yet; the build design plans V1940 with the demo users and a status map. Until then the test lead prepares each set on SIT as its source column says.

<!-- tp:data -->

Some cases change the state of a test client (for example the contact changes on UAT CSF Client One). The test lead restores the client's contact details between cycles, or testers run those cases on a second masked client.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments and data; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Write and keep the automated tests green; fix defects; support triage |
| Infrastructure team | BDO Unibank ITIO with iorta TechNXT | Run the backup and restore cases of FR-CSF-043 and provide the restore log |
| BDOI department testers | BDO Insure Contact Center (agents, supervisors, management) | Run the UAT scenarios; confirm the expected results match the way they serve clients; raise defects |
| BDOI process owner | Product Owner, Customer Servicing Facility | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Agents run the search, servicing, verification, resend and upload scenarios (SC-CSF-02 to 06); supervisors run the resends to another address and the activity report; management runs the audit and report scenario (SC-CSF-07). The personas are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in the CSF |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client, or a security rule is broken | A contact change is saved without a passed verification; an RA is sent unprotected or to the wrong address; an agent sees an action of another role |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A PN search finds nothing for an account in BIBS; the payment history misses applied invoices |
| Medium | A function works with a workaround, or a message, label or report column is wrong but the data is right | The no-result message omits the criteria; a report column is missing |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the client code or change number.
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

<!-- tp:automation -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| The CSF screens, labels or messages differ from the build design when built | Medium | Compare the screens with this plan before the system test; update the workbook to version 1.1 with the built texts and message codes |
| BDOI answers to open questions change expected results (CSQ03 verification checks, CSQ04 status definitions, CSQ10 role matrix, CSQ14 and CSQ15 contacts) | Medium | The values are parameters and lists; the affected cases name them and are re-run after the change without a new build |
| No renewal advices exist until the Renewal and EB modules store them (D-CSF-01) | High | The test lead loads RENEWAL_ADVICE documents on the test clients; the cases are re-run when the modules produce RAs |
| The QPS / EBIX interface is not specified (CSQ01) | Medium | The sync-enabled cases stay Blocked; the disabled sync and the outbox are tested now |
| Test mailboxes not reachable from SIT or UAT | High | Check the relay before the cycle (entry criterion); resend cases read the outbox when the mailbox is down and are re-run later |
| Time-based cases (verification validity, payment window, failed verifications per day) need the clock to pass | Medium | The test lead sets verification and receipt times in the test database, as the preconditions describe |
| The restore test needs an infrastructure environment | Medium | The infrastructure team runs FR-CSF-043 on its restore environment and hands the log to QA |
| Contact Center testers are not available in the UAT window (24 agents share the hotline) | High | Agree named testers and dates outside the peak hours 10:00-12:00 and 14:00-16:00 in the UAT plan (deliverable 30) |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous or cannot be tested as written. The cases use the assumption stated in the proposal; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Customer Servicing Facility requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Customer Servicing Facility", organisation: BDOI}
  - {name: "", role: Alternative Distribution Head, organisation: BDOI}
  - {name: "", role: "Head, Contact Center Management", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
