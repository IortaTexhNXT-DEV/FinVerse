---
# Source of the Jira import guide of the BIBS user story backlog (BRD-00). Produced by build_backlog.py: lines
# <!-- bl:... --> become tables and {{...}} tokens become the counts of the same run that writes the workbooks and the
# import files, so the three always agree.
title: User Story Backlog - Jira Import Guide
subtitle: Setting up the BIBS Jira project and importing the epics and stories of Drop 0, Drop 1 and Drop 2
doc_type: Import Guide
doc_code: Backlog
brd: BRD-00
name: Jira Import Guide
doc_id: BIBS-BKL-BRD-00-G
version: "1.0"
date: 9 October 2026
status: Issued for BDOI review
header_title: BIBS User Story Backlog - Jira Import Guide
h1_page_break: false
control:
  - version: "1.0"
    date: 09 Oct 2026
    author: iorta TechNXT Business Analyst lead
    reviewer: iorta TechNXT Project Manager; iorta TechNXT QA lead
    approver: BIBS Product Owner (pending)
    change: First issue, after the decision of the BIBS Product Owner of 9 October 2026 to keep the working backlog in Jira
distribution:
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Backlog content, priorities and fix versions"}
  - {name: "Jira site administrator", role: Implementer, organisation: BDOI, purpose: "Project set-up and import"}
  - {name: "Business owners of BRD-01 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "Stories and acceptance criteria of their BRD"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Refinement, estimation and iteration planning"}
---

# Purpose and decision

On 9 October 2026 the BIBS Product Owner decided that the FRS of each BRD keeps the user-story view and the storyboard index as signed appendices, and that the working backlog of the programme lives in Jira. This guide tells the Jira site administrator how to set up the BIBS project and import the backlog, and tells the Product Owner how to check the import and keep the backlog and the FRS aligned.

The backlog is generated from the same sources as the FRS appendices: the functional requirements (FR) of each FRS with their numbered acceptance criteria, the user-story view, the storyboard index, the clarifications for confirmation and the test plan of each BRD. Nothing in it is typed twice, so the backlog cannot drift from the signed documents. It holds **{{epics}} epics** and **{{stories}} stories** with **{{points}} story points** (first estimate), per BDOI drop in one workbook, BIBS_Backlog_BRD-00_User_Story_Backlog_Drop-n_v{{version}}.xlsx, and one import file, BIBS_Jira_Import_Drop-n.csv in the folder jira (n = 0, 1, 2). Each workbook holds the Summary, one sheet per BRD (each epic row followed by its stories, every field of the import file in readable columns), Coverage, BRD ID trace, Ready and Done, and Field mapping; the import file of a drop holds exactly the issues of its workbook. There is no status column: every issue imports with the initial status of the project workflow.

# How the backlog is cut

## Epics and stories

- **Epic** = one business capability of a BRD: one section of the chapter Functional requirements of its FRS, named "<BRD short name> - <capability>", for example "Renewal - Letters, acceptance and follow-up". Each BRD has {{epics_min}} to {{epics_max}} epics.
- **Story** = one user-facing increment, normally one FR. {{splits}} FRs whose acceptance criteria fall in clearly separate groups are split into {{split_stories}} stories (for example FR-OP-030: the scheduled and the manual extraction); {{merged}} pairs of small FRs of the same screen, each with one acceptance criterion, make one story each. No sub-tasks are imported.
- **Summary**: the short form of the story (the FR title, or the part of a split FR). **Description**: the user story (the BRD's own words where the BRD writes the requirement as a story, otherwise derived from the FR), Background (BRD references and pages, FRS section, persona), Business rules, Acceptance criteria as Given / When / Then with the AC number of the FRS in brackets, for example "(AC2)", Out of scope / notes (open clarifications CLR with their status) and Links (FRS section, screens, walkthrough steps, figures, UX deck slides, test cases). The text reads the same in Jira wiki markup and in Markdown.

<!-- bl:brds -->

## Drops and fix versions

Every epic takes the primary drop of its BRD (Drop 0: BRD-03, 11, 13; Drop 1: BRD-01, 02, 04, 05, 06, 09, 10, 12; Drop 2: BRD-07, 08), except the capabilities that the BDOI drop plan of 26 September 2026 places elsewhere: {{drop_text}}. All stories of an epic share its drop, so each import file holds its epics with all their stories. The {{phase2}} stories with the fix version Phase 2 stay in the file of their BRD: the Employee Benefits partner portal (drop plan item 2.4 is Employee Benefits without the portal feature) and the document extraction of Submitted Policies (FR-SP-002, BRD Release 2). The description of each epic gives the reason of its drop.

## Priority and story points

Priority comes from the MoSCoW priority of the FR ({{priority_text}}). The BRDs mark almost every requirement Must have, so nearly every story imports as High and the Product Owner ranks the stories on the board. Highest is not set by the import; the Product Owner sets it on a story that blocks a test window or the go-live. A Won't have FR is not imported.

Story points are a **first estimate**, set by one rule so that every team starts from the same basis; the team re-estimates each story at the refinement before it enters an iteration. The number of acceptance criteria gives the base on the scale 1, 2, 3, 5, 8, 13 (one criterion 1 point, two 2, three 3, four or five 5, six or more 8); the story goes one step up when it names three screens or more, and one step up when it exchanges data with a BDO or external system of the drop plan (EIAM, UIDM-ISC, CCM, M365, LMS, HL-LOAS, LFS, PMS, CMS / New BOB, Old BOB, OBPCS, AFTS, TFS, EDP, EGL, CARMS, Bridger Insight, NLDS) or with insurer, LAMD or bank files.

# Setting up the Jira project

The steps are for a **company-managed** project on Jira Cloud, which the programme recommends because the custom fields, components and the Story Points field are shared by every board of the project; the last section of this chapter gives the team-managed variant. The administrator needs the Jira administrator global permission.

## Project, issue types and fields

1. Create the project **BIBS** (template Scrum, company-managed). The issue type scheme holds **Epic** and **Story**; sub-tasks are not used. Keep the default priority scheme (Highest, High, Medium, Low, Lowest).
2. Create five custom fields of type **Paragraph (multi-line text)** with these exact names: **BRD IDs**, **FR IDs**, **Test Cases**, **FRS Reference**, **Acceptance Criteria** (Settings > Issues > Custom fields > Create custom field), with the global context or the context of the BIBS project.
3. Put Story Points, Components, Fix versions, Labels, Parent and the five custom fields on the screens of Story, and Components, Fix versions, Labels and the five custom fields on the screens of Epic.

## Components and fix versions

Create one component per module (Project settings > Components): {{components}}. The import would also create a missing component, but creating them first lets the Product Owner name a component lead. Then create the four versions (Project settings > Releases): **Drop 0**, **Drop 1** and **Drop 2**, each from the start of its requirements to the end of its last stream on the BDOI drop plan of 26 September 2026 (Drop 0 September 2026 to January 2028 with the cut-over, Drop 1 September 2026 to December 2027, Drop 2 December 2026 to November 2027; go-live of all modules together in January 2028), and **Phase 2** without dates.

## Team-managed variant

In a **team-managed** project the importer is the same, with these differences: create the five custom fields in Project settings > Issue types, on Epic and on Story (type Paragraph); map Story Points to **Story point estimate**; a team-managed project has no components, so map the column Component to **Labels** (the module becomes a label); do not map Epic Name; versions are created on the Releases page of the project. Linking by Parent works as in a company-managed project.

# Importing the files

## Importer steps

Import the files in the order Drop 0, Drop 1, Drop 2. Each file links its stories only to the epics of the same file.

1. Open **Settings > System > External system import > CSV** (Jira administrator).
2. Choose the file, for example BIBS_Jira_Import_Drop-0.csv (first time without a configuration file).
3. Settings: import to the project **BIBS**; CSV delimiter comma; file encoding **UTF-8**. The files hold no dates.
4. Map each column as in the table below. Tick "Map field value" for Issue Type and Priority only if the project uses other names than Epic, Story, Highest, High, Medium and Low.
5. Click **Validate**: the importer reads the file without creating anything and lists any warning. Then click **Begin Import**.
6. At the end, download the configuration file and use it for Drop 1 and Drop 2, so that the mapping stays the same.

<!-- bl:mapping -->

The file has {{labels}} columns named Labels: the importer reads repeated columns of a multi-value field as one value each. A label has no spaces (BRD-06, Drop-1, Marketing_AO).

## Linking the epics

Each row has a temporary **Issue ID**: 10001 onwards in Drop 0, 20001 onwards in Drop 1 and 30001 onwards in Drop 2; the epics come first in each file. The column **Parent** of a story holds the Issue ID of its epic in the same file. Mapped to Issue Id and Parent, the importer creates the epics, gives them their Jira keys and sets the parent of each story to the new epic. If the project still shows the Epic Name field on epics, map the column Epic Name to it; it holds the same text as the summary.

# Checking the import

After each file, compare the counts in Jira with the tables below (the Summary sheets of the workbooks). The filter project = BIBS AND fixVersion = "Drop 1" AND issuetype = Story counts the stories of a fix version; export it to Excel with the column Story Points to total the points.

<!-- bl:counts -->

<!-- bl:checks -->

Then open three stories per file at random: the description shows the story, the acceptance criteria with their AC numbers and the links, the custom fields are filled and the story sits under its epic.

**Coverage.** Every BRD ID of the user-story views and every FR of the FRS is in at least one story (Coverage and BRD ID trace sheets), except {{no_story}} BRD IDs that have no FR by the BRD's own text: ReInsurance (phase 2), requirements removed, moved or out of scope by an addendum, and two umbrella clarifications (CLR-CR-02, CLR-CR-09). Gaps: {{gaps}}.

# Keeping the FRS and the backlog aligned

- **A change of an acceptance criterion, a rule or a screen** goes through a change request in the Change Management Register; once the Change Control Board approves it, the project team updates the FR in the FRS, and the FRS appendices, the test plan and the backlog files are regenerated from it in the same release. The backlog never changes an acceptance criterion on its own.
- **In Jira**, the Product Owner updates the changed stories: by hand for a few, or by importing the changed rows with an added column Issue Key mapped to the key of the existing issue, which updates instead of creating. A new FR comes as a new story under its epic.
- **A clarification decided by BDOI** (CLR) changes the FR or confirms it; the note of the story records the decision and the story becomes ready.
- **Re-estimates, ranking and iteration planning** are made in Jira only and are not written back to the FRS; a filter on the field FR IDs finds every story of an FR when the FRS changes.

# Definition of Ready and Definition of Done

Both lists are on the sheet Ready and Done of each workbook and follow the Test Strategy. A story is **ready** when it is traced, its acceptance criteria are agreed with the BDOI owner of the BRD, its clarifications are decided or accepted as working assumptions, its screens, test cases, permissions, configuration values and dependencies are known and its estimate is reviewed (13 points at most). A story is **done** when every acceptance criterion passes in SIT with the test cases of its Test Cases field, no Severity 1 or 2 test incident is open on it, its main flow is in the regression set, the FRS, test plan and backlog still agree, and the BDOI owner of the BRD accepts it in UAT.
