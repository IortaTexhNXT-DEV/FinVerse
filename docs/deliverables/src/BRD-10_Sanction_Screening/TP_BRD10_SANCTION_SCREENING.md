---
# Word summary of the BRD-10 Sanction Screening test plan. The tables marked <!-- tp:... --> are filled
# from brd10_cases.yaml. Build: python docs/deliverables/src/testplans/build_test_plan.py brd10_cases.yaml
title: Sanction Screening Test Plan
subtitle: BRD-10 Sanction Screening and Risk Profiling - test conditions, scenarios and cases
doc_type: Test Plan
doc_code: TestPlan
brd: BRD-10
name: Sanction Screening Summary
doc_id: BIBS-TP-BRD-10
version: "1.1"
date: 08 October 2026
status: Issued for BDOI review
header_title: Test Plan BRD-10 Sanction Screening
h1_page_break: false
control:
  - version: "0.9"
    date: 24 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from FRS BRD-10 v1.0
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review, with the Excel workbook of the same version
  - version: "1.1"
    date: 08 Oct 2026
    author: iorta TechNXT QA
    reviewer: iorta TechNXT Business Analysis
    approver: BDOI Product Owner (pending)
    change: "Re-based on FRS BRD-10 v1.1 (Sanction Screening and Risk Profiling BRD v04172026, unchanged; Report List of 13-May-2026): case for the Name Matching Fall-out report added."
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: Chief Compliance Officer and Compliance unit, role: Business tester, organisation: BDOI, purpose: "Configuration, watchlist, compliance review, STR, reports"}
  - {name: "Unit Head, Claims and Risk Management", role: Business tester, organisation: BDOI, purpose: Risk profiling and case approval}
  - {name: "Combank, Corbank and Retail Marketing", role: Business tester, organisation: BDOI, purpose: Investigation and Unit Head approval}
  - {name: Business Project Services, role: UAT coordinator, organisation: BDO Unibank ESG, purpose: UAT planning and traceability}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "System test, issue resolution, UAT support"}
---

# Introduction

## Purpose

This document summarises the test plan for BRD-10 Sanction Screening and Risk Profiling in BIBS (BDOI Broker System, on iNXT BrokerVerse). It tells the Compliance unit and the other BDOI departments what will be tested, how, with which data and by whom, and when testing is complete. The test conditions, scenarios and cases themselves are in the Excel workbook of the same version, `BIBS_TestPlan_BRD-10_Sanction_Screening_v1.1.xlsx`, which the testers use during execution.

Every case traces to a functional requirement (FR) of FRS BRD-10 v1.1 and to the BRD requirement IDs (SNSRP-nnn, and the page references of the to-be process on p.6-8) that the FR meets.

The cases are written from the FRS: the expected results quote the message texts of the FRS, and the screen paths and button labels are those of the FRS. They are checked against the screens before the system test starts.

All list entries and client names in the test data are invented. No real sanctioned or politically exposed person is used on any test environment.

## Scope

In scope are all 44 FRs of FRS BRD-10 v1.1 and the BRD IDs they trace to:

- access, segregation of duties and the immutable audit log (FR-SS-001, 091, 092);
- versioned configuration under maker-checker: matching criteria, risk categories and rules, approval, assignment and SLA matrices, review and STR templates, dispositions (FR-SS-010 to 019);
- the watchlist: scheduled and uploaded list files, failed records, manual entries and their approval (FR-SS-020 to 023, 082);
- matching, risk tagging and automatic case creation on the four triggers and the batch window (FR-SS-030 to 035);
- case management: lifecycle, lists, search, re-assignment, SLA monitoring and the Screening Home (FR-SS-040 to 045);
- investigation: review template, disposition and documents (FR-SS-050 to 052);
- validation, Unit Head approval, Compliance review and the AML Committee (FR-SS-060 to 064);
- STR preparation, extraction and the AMLC filing reference (FR-SS-070 to 072);
- notifications, reminders and the compliance reports (FR-SS-080, 081, 090).

The roles-and-access sheet checks each screening action against the roles that may and may not perform it (the matrix of FRS section 3.3).

## Out of scope

- Filing the STR on the AMLC portal; it stays manual. The cases stop at the extraction file and the recorded AMLC reference.
- Real-time or system-to-system list sources (SQ01); only file and manual sources are tested.
- The AMLC prescribed STR layout and reason codes (SQ09). The cases test the placeholder layout and test reason codes; they are re-run when BDOI supplies the AMLC content.
- The production values of thresholds, risk categories, matrices and SLAs (SQ02-SQ08). The cases use the seed configuration; Compliance enters the production values before go-live.
- Blocking of business on an open match (SCR_BLOCK_ON_OPEN_MATCH), which is off until SQ07 is answered; the cases check that a tag warns and does not block.
- Performance and volume testing. The NFRs of FRS section 8 are tested in the BIBS-wide performance test plan (deliverable 28).

## References

<!-- table: widths=1.2,8.6,4.2 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | Functional Requirements Specification BRD-10 Sanction Screening (`BIBS_FRS_BRD-10_Sanction_Screening_v1.1.docx`) | 1.1, 08 Oct 2026 |
| R2 | Sanction Screening and Risk Profiling BRD | Prepared 10-Apr-2026; approved 16 to 17-Apr-2026 |
| R3 | Test plan workbook BRD-10 (`BIBS_TestPlan_BRD-10_Sanction_Screening_v1.1.xlsx`) | 1.1 |
| R5 | FRS BRD-11 User Access Maintenance (roles, sign-in, session policy) | Current version of the business sign-off pack |
| R6 | BRD discrepancy and clarification register (`BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.2.xlsx`) | 1.2 |

# Test approach

## Test levels

<!-- table: widths=3.2,6.2,3.6,3.6 caption="Test levels" -->
| Level | What is tested | Who | When |
|---|---|---|---|
| System test | Every case of the workbook, screen by screen, with the SIT data sets | iorta TechNXT QA | After Sanction Screening is deployed on SIT |
| Persona end-to-end | The eight scenarios from start to finish by the persona that owns each step, with notifications and e-mails checked | iorta TechNXT QA with the Compliance testers | After the system test passes |
| User acceptance test (UAT) | The scenarios and the High-priority cases, run by BDOI testers on masked production-like data and invented list entries | BDOI Compliance, investigators, Unit Heads and AML Committee members | After the entry criteria of section 3 are met |


## How the cases were derived

- Each FR's acceptance criteria, business rules, validations and alternate flows became one or more **test conditions** (sheet Test Conditions). A condition states what must be true, for example "The maker of a version cannot approve it, even with SCR_CONFIG_APPROVE".
- Each condition has at least one **test case** (sheet Test Cases) with the persona, the screen (menu path), the preconditions and data set, numbered steps and the expected result. Where BIBS shows a message, the expected result quotes the FRS text.
- Every FR has at least one **positive** case (the action succeeds) and one **negative** case (BIBS refuses the action). Boundary cases test the values at and next to a limit: thresholds 0 and 1, effective date today and yesterday, reminder lead equal to and below the SLA, the second and third committee vote, a date received tomorrow.
- **Scenarios** (sheet Scenarios) group the cases into business threads by persona, so a tester can follow a client from the list change to the filed STR in one sitting.
- The **roles-and-access** sheet lists, for each action, the roles that must be allowed and the roles that must be refused. It is run with one user per role; four-eyes cases use a combined test user that holds both rights.

<!-- table: widths=3.4,11 caption="Test case types" -->
| Type | Meaning |
|---|---|
| Positive | The normal flow succeeds with valid data |
| Negative | Invalid data, a missing item or a broken rule is refused with its message |
| Boundary | Values at, below and above a limit |
| Security-access | Screens, buttons and system functions are open only to the roles that hold the permission; four-eyes rules |
| Workflow | A stage transition of the case workflow SCR_CASE, or a state change of a configuration version, list change or STR |
| Report-output | Reports, case lists and extraction files; content checked against the screen |
| Upload-download | List files, case documents and client files uploaded, and files downloaded |

## Reading the workbook

The workbook has a README sheet that explains every column. The sheets are Document Control, Test Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access. Case IDs carry their condition: TC-SS-064.2-02 is the second case of condition 2 of FR-SS-064. Status starts as Not run; testers fill Status, Actual result, Tester, Date and Issue ID.

# Entry and exit criteria

## Entry criteria

<!-- table: widths=2.4,11 caption="Entry criteria" -->
| Level | Criteria |
|---|---|
| System test | Sanction Screening is deployed on SIT with its roles, parameters, lists and jobs; the seed data (invented names only) is loaded; screen labels and messages have been compared with this plan and differences recorded; the test mailboxes receive mail. |
| Persona end-to-end | All High-priority system test cases are run; no open Critical issue; five committee users exist; the SLA monitor and ingestion jobs run on SIT. |
| UAT | FRS BRD-10 v1.1 is signed off or its open comments are agreed; Compliance has entered the configuration to be used in UAT (thresholds, risk categories, matrices, SLAs) through maker-checker; the system test exit criteria are met; the UAT environment holds masked client data and invented list entries (section 4.1); BDOI testers have user IDs with the roles of section 5 and attended the walkthrough. |

## Exit criteria

<!-- table: widths=2.4,11 caption="Exit criteria" -->
| Level | Criteria |
|---|---|
| System test | 100 % of cases run (the AMLC layout cases may be Blocked until SQ09 is answered); 100 % of High-priority cases passed; no open Critical or High issue; open Medium and Low issues have an agreed fix date. |
| Persona end-to-end | All eight scenarios passed end to end with the expected notifications, timeline events and files. |
| UAT | All scenarios and High-priority cases passed or accepted by the BDOI process owner; no open Critical or High issue; open issues listed with an agreed plan in the UAT sign-off; the sign-off of section 9 is signed. |

**Suspension.** Testing of a scenario stops when a Critical issue blocks it, when the environment is down for more than half a day, or when the test data is corrupted. It resumes after the fix is deployed and the blocked cases are re-run from their first step.

# Environments and test data

## Environments

<!-- table: widths=2.6,6.4,5.4 caption="Test environments" -->
| Environment | Use | Data |
|---|---|---|
| SIT | System test and persona end-to-end runs by iorta TechNXT QA | Seed data (users, configuration, a watchlist with invented names and cases in every stage) and the data sets of section 4.2; test mailboxes |
| UAT | Acceptance by BDOI testers | Masked copy of production-like client data; invented list entries only; no real client names, TINs, IDs, addresses or e-mail addresses |

Non-production data is always masked. Client names, TINs, government IDs, birth dates, addresses and e-mail addresses are replaced before data is loaded into SIT or UAT. Watchlist data on test environments is invented; the official AML advisory and PEP files are never loaded outside production. Notification e-mails point to test mailboxes.

## Named data sets

The cases refer to named data sets. The seed data holds the users and configuration, a watchlist with invented names and cases in every stage; the test lead prepares the other sets on SIT as their source column says.

<!-- tp:data -->

Many cases move a case to its next stage. The test lead reloads the case seed between cycles, or testers create a fresh case with the matching client of TD-SS-05 before a case that needs a given stage.

# Roles and responsibilities

<!-- table: widths=4,5.4,5 caption="Test roles" -->
| Role | Organisation | Responsibilities |
|---|---|---|
| Test lead | iorta TechNXT QA | Owns this plan and the workbook; prepares environments, users and invented list data; runs the daily issue triage; reports progress |
| System testers | iorta TechNXT QA | Run the system test and the persona end-to-end scenarios; raise issues with evidence |
| Developers | iorta TechNXT | Resolve issues; support triage |
| Infrastructure administrator | iorta TechNXT with BDO Unibank ITIO | Runs the checks outside the screens of FR-SS-091 (the timeline cannot be altered) and hands the evidence to QA |
| BDOI department testers | Compliance unit; Combank, Corbank and Retail investigators and Unit Heads; AML Committee members | Run the UAT scenarios of their role; confirm the expected results match the compliance rules; raise issues |
| BDOI process owner | Chief Compliance Officer | Decides on disputed expected results and accepted issues; signs off UAT |
| UAT coordinator | Business Project Services, BDO Unibank ESG | Plans the UAT sessions; checks traceability to the BRD |

Compliance runs the configuration, watchlist, compliance review and STR scenarios (SC-SS-02, 03, 07); investigators and Unit Heads run the investigation and approval scenarios (SC-SS-05, 06); AML Committee members run the committee cases of SC-SS-06; the UCC and the Operations Lead run the monitoring scenario (SC-SS-08). The personas are:

<!-- tp:personas -->

# Issue management

## Severity

<!-- table: widths=2.2,8.4,5 caption="Issue severity" -->
| Severity | Definition | Example in Sanction Screening |
|---|---|---|
| Critical | A main flow cannot be completed, data is lost or wrong in a way that reaches a regulator or a client, or a security rule is broken | A listed name is not matched; a maker approves the own configuration; an STR without a committee decision is extracted; an audit row can be changed |
| High | A function does not work as the FRS states and there is no workaround acceptable to the business | A case is routed to the wrong approver; the SLA monitor does not escalate a breach |
| Medium | A function works with a workaround, or a message, label or report column is wrong but the data is right | A validation message names the wrong field; a report filter is missing |
| Low | Cosmetic issues that do not affect use | Alignment, spelling, colour |

## Triage and fixing

- Testers raise an issue for each failed case, with the case ID, steps, actual result, screenshot and the case, version or entry number.
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

## Roles and access

The table shows the actions checked per role. Each Y and N is one row of the Roles and Access sheet.

<!-- tp:access -->

# Risks

<!-- table: widths=5.4,2,7.2 caption="Test risks and mitigations" -->
| Risk | Impact | Mitigation |
|---|---|---|
| Screen names, labels or messages change after BDOI's review of the FRS | Medium | Compare the screens with this plan before the system test; re-issue the workbook with the confirmed texts |
| BDOI answers to open questions change expected results (SQ02 thresholds, SQ03 risk categories, SQ04 approval matrix, SQ06 dispositions, SQ08 SLAs, SQ15 committee rule) | Medium | The values are configuration versions, lists and parameters; the affected cases name them and are re-run after the change without a change to the system |
| The AMLC STR layout and reason codes are not supplied (SQ09) | High | Test the placeholder layout and test reason codes now; re-run FR-SS-017, 070 and 071 when the AMLC content is entered as a new version |
| Real list data reaches a test environment | High | Only invented names are loaded outside production; the test lead checks the list sources of SIT and UAT before each cycle |
| Time-based cases (SLA reminders and breaches, effective dates, the batch window) need the clock to pass | Medium | The test lead moves due times and dates in the test environment and runs the jobs on demand, as the preconditions describe |
| Four-eyes and committee cases need several users with specific role combinations | Medium | The test lead creates the combined users and the five committee users of TD-SS-01 and TD-SS-08 and removes them after the cycle |
| BDOI testers from several units (Compliance, Marketing, AML Committee) are not available at the same time | High | Plan the committee cases in one session with all five members; agree named testers per unit in the UAT plan (deliverable 30) |

<!-- pagebreak -->

# Sign-off

By signing, BDOI confirms that this test plan and its workbook cover the Sanction Screening and Risk Profiling requirements it expects to test, and accepts the entry and exit criteria of section 3.

```signoff
rows:
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: Chief Compliance Officer, organisation: BDOI}
  - {name: "", role: "Unit Head, Claims and Risk Management", organisation: BDOI}
  - {name: "", role: "UAT coordinator, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Test Lead, organisation: iorta TechNXT}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
