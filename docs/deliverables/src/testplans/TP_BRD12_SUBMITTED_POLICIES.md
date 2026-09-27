---
# Word summary of the BRD-12 Submitted Policies test plan. The tables marked <!-- tp:... --> are filled
# from brd12_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd12_cases.yaml
title: Submitted Policies Test Plan
subtitle: BRD-12 Submitted Policies - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-12
name: Submitted Policies Summary
doc_id: BIBS-TP-BRD-12
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: Test Plan BRD-12 Submitted Policies
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-12 v1.0 and the Submitted Policies build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
distribution:
  - {name: "Product Owner, Submitted Policies", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Unit Head, Combank and Corbank", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "CBG Admin / Marketing (Submitted Handlers, Sanitation Handlers, Team Leads)", role: Business tester, organisation: BDOI, purpose: "Intake, processing, renewal hand-off"}
  - {name: "Non-CBG Corporate Policy Review Officers", role: Business tester, organisation: BDOI, purpose: "Policy review, IAAF and TOR"}
  - {name: "Admin Team (UPP handlers), NB Team Leads", role: Business tester, organisation: BDOI, purpose: "Handling fees, IAAF and rule approval"}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, defect fixing, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-12 Submitted Policies in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the CBG Admin and Marketing teams, the Non-CBG policy reviewers and the Admin Team what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-12_Submitted_Policies_v1.0.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-12 v1.0 and to the BRD requirement IDs (BRIDSP-nn) that the FR meets.

Submitted Policies is designed and not yet built. The cases are written from the FRS and the build design, so:

- expected results quote the message text of the FRS without a message code; the codes are added in version 1.1 when the module is built;
- the automation reference column is blank; the automated tests written during the build are referenced in version 1.1;
- screen paths and button labels are those of the build design and are checked against the screens before the system test starts.

## Scope

In scope are all 31 FRs of FRS BRD-12 v1.0 and the BRD IDs they trace to:

- intake from the approved sources, extraction with confirmation, manual entry and the migration of the Excel masterlists (FR-SP-001 to 004);
- the Submitted Masterlist, its role-based view and extract, handler, conversion status and remarks (FR-SP-010 to 012);
- rule sets under maker-checker, the processing run and the fallout (FR-SP-020 to 022);
- classification, qualification and non-renewal buckets, the RA template and inforced buckets (FR-SP-030 to 034);
- policy reviews and the IAAF with its approval matrix (FR-SP-040, 041);
- limit breaches and the TOR with its TSU approval and hand-over (FR-SP-050 to 053);
- the expiry scan and hand-off to Renewal, the renewal work list, letters, insurer re-assignment, placement and booking status (FR-SP-060 to 065);
- handling-fee tagging in the Unapplied Payment List (FR-SP-070);
- reports, export formats, notifications and alerts (FR-SP-080 to 082).

The roles-and-access sheet checks each Submitted Policies action against the roles that may and may not perform it (the matrix of FRS section 3.3).

## Out of scope

- The renewal itself after the hand-off - renewal account, hold cover request, RA, NRNS, NAL and SFU letters - which belongs to the Renewal module (decision D2) and its BRD-6 test plan. The cases here check the hand-off, the insurer re-assignment and the status that comes back.
- Placement and booking screens (BRD-1); the cases check only that the masterlist follows the account and invoice events.
- Automated feeds from LFS, HLS, CIU, SPI, LAMD and the mail house (COG), and OCR of scanned documents; they are parked. The cases use uploads and text PDFs.
- Qualified e-signature of the IAAF and TOR (parked); the cases check the stamped signature.
- The accounting entries of the handling fee (SP SQ13); the cases stop at the tag and the OR.
- Performance and volume testing, including the 2-second target of the NFRs. They are tested in the BIBS-wide performance test plan (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-12 Submitted Policies (`BIBS_FRS_BRD-12_Submitted_Policies_v1.0.docx`) | 1.0, 25 Sep 2026 |
| R2 | BDOI Submitted Policies BRD (`docs/source-documents/BRD - Submitted Policies (with e-sig MCM 4.24.2026).pdf`) | v1.3, 20-Apr-2026 |
| R3 | BDOI Report List, Submitted Policies rows #133-#164 (`docs/source-documents/Report List as of APR-27-2026.pdf`) | 27-Apr-2026 |
| R4 | Test plan workbook BRD-12 (`BIBS_TestPlan_BRD-12_Submitted_Policies_v1.0.xlsx`) | 1.0 |
| R5 | Submitted Policies build design (`docs/architecture/SUBMITTED_POLICIES_DESIGN.md`) | current |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.0.xlsx`) | 1.0 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| Unit and integration (automated) | Intake validation and natural key, extraction, rule engine and processing steps, buckets, IAAF and TOR routing, expiry scan and hand-off port, handling-fee matching, scope filter, permissions, against a PostgreSQL database; frontend form checks | iorta TechNXT developers | Written during the build; run on every change in CI (`mvn verify`, `npm run verify`) |
| System test | Every case of the workbook, screen by screen, with the SIT data sets | iorta TechNXT QA | After Submitted Policies is deployed on SIT |
| Persona end-to-end | The nine scenarios from start to finish by the persona that owns each step, with notifications, e-mails and letters checked | iorta TechNXT QA with the BDOI testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like masterlists | BDOI CBG Admin / Marketing, Non-CBG policy reviewers, Admin Team | After the entry criteria of section 3 are met |

Automated tests do not replace the system test. They show that a rule holds after every change; the system test shows that the screens, messages, documents, letters and reports are what the business expects. When the automated tests exist, version 1.1 of the workbook names them in the automation reference column, and each such case is still run on the screen once per cycle.

## How the cases were derived

- Each FR's acceptance criteria, business rules, validations and alternate flows became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "Loading the same file a second time is refused and names the earlier run".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes the FRS text.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit: the renewal lead of 150 and 151 days, the insurer acceptance window of 4 and 6 days, the unbooked hold cover at 5 days.
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow a policy from the upload to the booked renewal in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role and scope.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and API calls are open only to the roles that hold the permission and to the user's scope; preparer and maker rules |
| Workflow | A status change of a masterlist record (SBM_POLICY), an IAAF, a TOR, a rule set or a handling-fee record |
| Report-output | Reports, extracts, print batches; content checked against the screen |
| Upload-download | Source files, policy documents, legacy masterlists, IAAF and TOR PDFs |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access and FRS Findings. Case IDs carry their condition: TC-SP-060.1-02 is the second case of condition 1 of FR-SP-060. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Defect ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | Submitted Policies is built and deployed on SIT with its roles, user scopes, parameters, lists, jobs and seed rule sets; the planned seeds V1970 to V1972 are loaded; CI is green on the deployed commit; the automated tests of the module exist and pass; screen labels and messages have been compared with this plan and differences recorded; the test mailboxes receive mail. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical defect; the Renewal module (or its pending hand-off adapter) is deployed; Collections and Cashiering run on SIT for the handling-fee cases. |
| UAT | FRS BRD-12 v1.0 is signed off or its open comments are agreed; the answers to SP SQ04 to SQ08 are applied as rule sets, matrices and templates; the system test exit criteria are met; the UAT environment holds masked masterlists (section 4.1); BDOI testers have user IDs with the roles and scopes of section 5 and attended the walkthrough. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run; 100 % of High-priority cases passed; no open Critical or High defect; open Medium and Low defects have an agreed fix date. |
| Persona end-to-end | All nine scenarios passed end to end with the expected notifications, letters and status changes. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High defect; open defects listed with an agreed plan in the UAT sign-off; the sign-off of section 10 is signed. |

**Suspension.** Testing of a scenario stops when a Critical defect blocks it, when the environment is down for more than half a day, or when the masterlist data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| CI | Automated unit and integration tests on every change | Created by each test; PostgreSQL in a container |
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed profile migrations, the planned seeds V1970-V1972 and the data sets of section 4.2; test mailboxes |
| UAT | Acceptance by BDOI testers | Masked copies of the segment masterlists and of a LAMD snapshot; no real borrower names, PNs, CIFs, addresses or e-mail addresses |

Non-production data is always masked. Borrower and assured names, PN and loan numbers, CIFs, addresses, e-mail addresses and phone numbers are replaced before data is loaded into SIT or UAT. Bank counterpart, client and insurer e-mail addresses point to test mailboxes, so no letter, IAAF or hold cover request can reach a real party.

## Named data sets

The cases refer to named data sets. Submitted Policies has no seed yet; the build design plans V1970 (users and rules), V1971 (masterlist and LAMD snapshot) and V1972 (reviews, TOR, letters and handling fees). Until they exist the test lead prepares each set on SIT as its source column says.

<!-- tp:data -->

Processing runs and hand-offs change the status of many records. The test lead reloads the masterlist seed between cycles, or testers run a scenario on records uploaded fresh with the TD-SP-02 files.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, masked files and user scopes; runs the daily defect triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise defects with evidence |
| Developers | iorta TechNXT | Write and keep the automated tests green; fix defects; support triage |
| BDOI department testers | CBG Admin / Marketing (handlers, sanitation handlers, Team Leads); Non-CBG Corporate policy reviewers; Admin Team UPP handlers; NB Team Leads; TSU | Run the UAT scenarios of their role; confirm the expected results match the business rules; raise defects |
| BDOI process owner | Product Owner, Submitted Policies | Decides on disputed expected results and accepted defects; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Handlers run the intake and masterlist scenarios (SC-SP-01, 02); sanitation handlers the processing and renewal scenarios (SC-SP-03, 07); policy reviewers and NB Team Leads the IAAF scenario (SC-SP-05); AOs and TSU the TOR scenario (SC-SP-06); the Admin Team the handling-fee scenario (SC-SP-08); Team Leads the monitoring scenario (SC-SP-09). The personas are:

<!-- tp:personas -->

# Defect management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Defect severity" -->
| Severity | Definition | Example in Submitted Policies |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a client, bank counterpart or insurer, or a security rule is broken | An excluded account is handed to renewal; a user sees records outside the own scope; a preparer approves the own IAAF |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A file loaded twice creates duplicates; the expiry scan misses records within the lead days |
| Medium | A function works with a workaround, or a message, label or report column is wrong but the data is right | A fallout reason code is missing on the report; a filter is ignored |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise a defect for each failed case, with the case ID, steps, actual result, screenshot and the SBM, run, IAAF or TOR number.
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
| The screens, labels or messages differ from the build design when built | Medium | Compare the screens with this plan before the system test; update the workbook to version 1.1 with the built texts and message codes |
| BDOI answers to open questions change expected results (SP SQ04 classification, SQ05 bucket precedence, SQ06 RA template, SQ07 IAAF matrix, SQ08 TOR and Released, SQ12 acceptance window) | Medium | The values are rule sets, matrices and parameters; the affected cases name them and are re-run after the change without a new build |
| The Renewal module is not deployed when Submitted Policies is tested | High | Test the hand-off with the pending adapter (TC-SP-060.3-01); re-run the renewal cases of SC-SP-07 when the Renewal adapter is deployed |
| Source layouts and legacy masterlist layouts are not supplied (SP SQ01, SQ02) | High | Test with the draft templates; re-run the intake and migration cases on the BDOI layouts |
| Real borrower data reaches a test environment through an unmasked file | High | The test lead masks every source file before upload and checks the masterlist after each intake |
| Date-based cases (lead days, acceptance days, hold cover end) need the clock to pass | Medium | The test lead sets expiry and request dates in the test database and runs the jobs on demand |
| BDOI testers from several teams and segments are not available at the same time | High | Agree named testers per segment and role in the UAT plan (deliverable 30) |

# FRS findings

Writing the cases showed the points below, where the FRS is ambiguous or cannot be tested as written. The cases use the assumption stated in the proposal; the FRS owner decides the correction for FRS v1.1.

<!-- tp:findings -->

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Submitted Policies requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Submitted Policies", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank and Corbank", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
