---
# Source of the short Word guide that goes with the Master Data and Configuration Upload Templates workbook.
# Build: python docs/deliverables/src/programme/bdoi_inputs/build_bdoi_inputs.py (writes both workbooks and this
# guide). The tables are ```pack blocks rendered by build_bdoi_inputs.py from the templates of the workbook.
title: Master Data Upload Guide
subtitle: "How to fill in, load, validate and sign off the master data and configuration of BIBS"
doc_type: Guide to the master data and configuration upload templates
doc_code: Guide
brd: BRD-00
name: Master Data Upload Guide
doc_id: BIBS-GUIDE-BRD-00-MD
version: "1.1"
date: 8 October 2026
status: Issued for BDOI review
header_title: Master Data Upload Guide
h1_page_break: false
control:
  - version: "1.0"
    date: 8 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the Master Data and Configuration Upload Templates workbook v1.0
  - version: "1.1"
    date: 8 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: "Taxes and charges made complete: premium charges checklist (PM-04, LGT in R04B), BDOI's own taxes checklist (D0-10), party tax profiles (TX-01), tax forms (TX-02), currencies and exchange rates (MD-02, MD-03); coverage check; data BIBS does not hold listed as gaps with their questions; insurer-company items removed"
distribution:
  - {name: "Program Manager, Business Project Services", role: Owner of the master data plan, organisation: BDO Unibank ESG, purpose: Review and approval}
  - {name: "BIBS Product Owner", role: Product owner, organisation: BDOI, purpose: Review}
  - {name: "Head, Comptrollership", role: "Owner of the organisation, finance, tax and bank data", organisation: BDOI, purpose: Input and sign-off}
  - {name: "Product Owner, Marketing Business System", role: "Owner of the product, package and list data", organisation: BDOI, purpose: Input and sign-off}
  - {name: "Head, Operations", role: "Owner of the insurer, commission and receipt data", organisation: BDOI, purpose: Input and sign-off}
  - {name: "Business Administration (User Access)", role: "Owner of the users, profiles and approvals", organisation: BDOI, purpose: Input and sign-off}
  - {name: "Chief Compliance Officer", role: "Owner of the watchlists and screening set-up", organisation: BDOI, purpose: Input and sign-off}
  - {name: "BDOI IT", role: "Extracts of the legacy data", organisation: BDOI, purpose: Input}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Templates, loading, reconciliation"}
---

# Purpose

BIBS needs its master, reference and configuration data before it can be used: the company and its branches, the users and their group profiles, the chart of accounts and the accounting rules, the bank accounts, the products, packages and rates, the insurers and other business partners, the clients, the lists of values, the numbering, the documents, the notifications and the service levels. This guide explains how BDOI prepares that data with the workbook **BIBS_Templates_BRD-00_Master_Data_and_Configuration_Upload_Templates_v1.1.xlsx**, in which order it is loaded, how it is checked and who signs it off.

The workbook answers the question of the BDOI product owner of 8 October 2026 on the templates to upload the master data: every dataset the platform needs to work has a template sheet in the column layout that BIBS loads. The companion workbook **BIBS_Inputs_BRD-00_BDOI_Requirements_and_Inputs_by_BRD_v1.1.xlsx** lists, BRD by BRD, every decision, clarification and input the project needs from the business users; its rows of type Master data and Configuration value point to the templates of this workbook.

# What the workbook contains

The sheet **Start here** is the index: one row per dataset, in load order, with the group, the BRD, the drop, who provides it, the load route, the screen or console where it is entered or loaded, the templates it depends on, the due date, the rows entered so far, the mandatory cells still empty, the status and a link to the sheet. The datasets are grouped as follows.

```pack
plugin: build_bdoi_inputs.py
render: groups
```

Each template sheet is self-contained. Its header block gives the purpose, who provides the data, the due date, how it is loaded, the templates it depends on (with links) and the progress. Above each column a guide band gives the Mandatory flag, the Format, the Allowed values and What to enter; a mandatory column is marked * in the header row. One grey example row shows a filled-in record; it is overwritten or deleted.

Seven review sheets list what the platform delivers and ask for the value BDOI wants: the lists of values, the system parameters, the SLA and turnaround parameters, the numbering prefixes, the notification events with their recipients, the document and letter templates, and the accounting events that need an accounting rule. The sheet **Configured on screens** lists the set-up that is decided in working sessions and entered directly on the screens (rules, matrices, schedules). The sheet **Gaps** lists the datasets that have a template but no upload, and the data BDOI may need that BIBS does not hold, each with its question in the requirements workbook. The sheet **Coverage check** shows, for each dataset the product owner asked about, where it is in the workbook or why BIBS does not hold it. The workbook holds no item of the insurer modules (underwriting, insurer claims, reinsurance, reserves, consolidation), which BDOI as a broker does not use. The sheet **Reference lists** holds every list of allowed values used by the drop-downs, and **Questions and comments** is where BDOI raises a question on a template or a column.

# Who does what

| Role | Responsibility |
|---|---|
| BDOI owner of the dataset (named in Provided by) | Fills in the template or has it filled in, answers the questions, confirms the template (Confirmed by and Date on the sheet) and signs the data off after the load |
| BDOI IT | Extracts the legacy data in the layouts of the Migration Console, with the control file, for the datasets loaded from the legacy systems |
| iorta TechNXT project team | Explains the templates, checks the filled-in sheets, loads the data or keys it on the screens, runs the reconciliation and records the answers |
| Second user (BDOI or project team, as the screen requires) | Authorises every record entered or uploaded (maker-checker); no record is used before it is authorised |
| Program Manager, Business Project Services | Follows the status at the weekly programme meeting and escalates late or incomplete datasets to the steering committee |

# How to fill in a template

1. Open the sheet from the **Start here** index (Open →) and read the header block and the guide band.
2. Fill in the templates in the order of the index: a template whose codes others use comes first (for example the branches before the holidays, the chart of accounts before the accounting rules, the group profiles before the users).
3. Overwrite or delete the grey example row, then enter one row per record below the header row. Use the drop-downs: they offer the allowed values of the sheet Reference lists and the codes entered in the templates filled in before.
4. Keep codes exactly as given in the other templates (branch codes, account codes, cost centres, bank account codes, group profiles, user IDs). Dates are entered as dates (for example 15-Jan-2028); amounts and rates as numbers without a currency sign.
5. A mandatory cell left empty turns red; the index counts the mandatory cells still missing. Correct them before setting the status to Ready for review.
6. For a large legacy dataset (clients, client contacts, opening trial balance), BDOI IT sends the extract file in the layout of the template instead of typing the rows; the template then serves as the agreed layout and a sample.
7. Raise questions on the sheet **Questions and comments** with the template and the column; the project team answers there.
8. When the template is complete, write the name of the confirming owner and the date in **Confirmed by** and **Date** on the sheet, set the status in the index to Confirmed and return the workbook to the project team by the due date.

# Taxes and charges

Two checklists make sure no tax or charge is missed. The template **PM-04 Rate tables** carries one row per premium charge billed to clients (documentary stamp tax, premium tax, VAT on premium, fire service tax, VAT on BDOI's commission and the motor factors); BDOI gives the rate per line and the dates, and adds a row where a line has another rate. The local government tax is the rate of each insurer branch in **R04B**. The sheet **PM-04 Charges checklist** states the rule BIBS applies to each charge, for BDOI to confirm: every charge is a rate of the net premium after the minimum premium, computed independently of the others; DST is rounded up to the next half peso and the other charges to centavos. Charges other than these taxes (CTPL COCAF or LTO authentication, notarial or documentation fees) have no field in BIBS and are asked as questions.

The template **D0-10 Tax codes** carries one row per kind of BDOI's own tax: output VAT on commission and service fees, zero-rated and exempt sales, input VAT, the withholding tax insurers and clients withhold on BDOI's income (2307 received) and the withholding taxes BDOI deducts from suppliers, one row per ATC. BDOI gives the codes, ATCs, rates and accounts as its tax adviser confirms them; the workbook proposes none. The sheet **D0-10 Tax checklist** says what BIBS does with each and what it does not hold (final taxes, government and top withholding agent flags, exemption certificates). The tax details of each party (TIN, branch code, payee class, VAT treatment, default ATC) are given in **TX-01 Party tax profiles**, and the returns BDOI files with their due dates in **TX-02 Tax forms**.

# Load routes

Every dataset has one route into BIBS. The route is shown in the index and in the header block of the template.

```pack
plugin: build_bdoi_inputs.py
render: routes
```

Upload screens accept the Excel template itself as well as a CSV or TXT file with the same header row. Files are never sent by e-mail: they are uploaded on the screen or received on the Extracts screen of the Data Migration Console.

# Load order and due dates

The data is loaded in the order of the index, which follows the dependencies between the templates. The due dates follow the milestones of the Drop 0 configuration register and of the data migration: drafts with the Drop 0 sign-off, the platform set-up for Drop 1 SIT, the reference data for trial migration 1, the approved code maps, the values for UAT, the production values before the dress rehearsal and the freeze of the code maps and reference values.

```pack
plugin: build_bdoi_inputs.py
render: dues
```

```pack
plugin: build_bdoi_inputs.py
render: order
```

# Validation of the master data

The data is checked at four points.

| Check | Where | What is checked |
|---|---|---|
| On the sheet | The template | Mandatory cells, allowed values from the drop-downs, dates and numbers, codes of the templates filled in before; the index counts the rows and the missing mandatory cells |
| Review by the project team | The returned workbook | Completeness against the index, consistency between the templates (every code used is defined), duplicates, and the answers to the questions |
| When loaded | Upload screen or Data Migration Console | Every row is checked as on the screen of the record (format, allowed values, existing codes, duplicates); rows with an error are refused with the reason and are corrected and loaded again. The Data Migration Console also checks the control file (row count and totals) and maps the legacy codes with the approved code maps |
| After the load | BIBS screens and reports | Every record is authorised by a second user; the owner compares the records on the screens and in the lists and reports of BIBS with the template, and the data migration reconciliation compares the loaded totals with the legacy totals |

A record that is refused or found wrong is corrected in the template and loaded again; a correction after the load is made on the screen of the record, with the same maker-checker authorisation.

# Sign-off of the master data

The owner of each dataset signs it off twice. The template is confirmed before it is loaded (Confirmed by and Date on the sheet, status Confirmed in the index). The loaded data is signed off after the load and the check against the template: for the SIT set-up with the platform set-up for Drop 1 SIT, for UAT with the values for UAT, and for production with the production values before the dress rehearsal. The legacy datasets loaded by the Data Migration Console are signed with the reconciliation of each trial migration, of the dress rehearsal and of the go-live migration, on the Sign-off screen of the console.

From the freeze of the code maps and reference values (T-7, 27 December 2027) a change of a master or reference value needs a change request approved by the change control board. After go-live the data is maintained on the screens of BIBS by the owners named in the index, with maker-checker.

# Gaps and set-up on screens

The following datasets have a template but no upload or Migration Console layout yet. Their route is: configuration screen – upload on the screen (being added; until then BDOI fills this template and it is loaded on the screen with maker-checker). The owner checks the result against the template before the dress rehearsal.

```pack
plugin: build_bdoi_inputs.py
render: gaps
```

BIBS does not hold the following data today; each is asked as a question in the BDOI Requirements and Inputs workbook, and BDOI decides whether it is needed.

```pack
plugin: build_bdoi_inputs.py
render: data_gaps
```

The rules, matrices, schedules and other set-up listed on the sheet **Configured on screens** are decided by BDOI in working sessions and entered directly on the screens named there; there is no template to fill in, and the decisions are recorded in BDOI comments on that sheet.

# Questions and contacts

Questions on a template or a column go to the sheet **Questions and comments** of the workbook; questions on a decision or a clarification go to the BDOI Requirements and Inputs workbook against the Ref of the row. The status of every dataset is reviewed at the weekly programme meeting with the Program Manager of Business Project Services and the iorta TechNXT Project Manager.
