"""Content of the Product Maintenance document set v1.6 (BRD-03) in BDOI's template: the FRS, the process and
storyboards (BPS), the UX design (UXD), the reports, notifications and outputs (RNO) and the interface specification
(ICD). Read by build_brd03_v16.py, brd03_pack.py and build_brd03_v16_workbooks.py. Business wording only.

Colour of a point: "k" black (in the BRD, BRD-00 or a programme NFR), "g" green (not in the BRD, offered by the
iNXT BrokerVerse OOTB platform), "o" orange (change request), "b" blue (best practice, separate table). A label
"int" or "dm" adds the grey [Integration scope] or [Data Migration scope] label.
"""

from __future__ import annotations

VERSION = "1.7"
WB_VERSION = "1.4"
DATE = "17 Oct 2026"
DATE_US = "10/17/2026"
AUTHOR = "iorta TechNXT Project Team (drafting support); for review by the BDOI Product Owner"
OWNER = "BDOI Product Owner, Product Maintenance"
ICD_VERSION = "1.0"
# The UX Design alone moved to 1.7 (screenshots of the insurer record tabs and the Master Data Change Log).
UXD_VERSION = "1.8"

DOCS = {
    "FRS": {"file": f"BIBS_FRS-BDOI_BRD-03_Product_Maintenance_v{VERSION}.docx",
            "cover": "Functional Requirements Specification: Product Maintenance",
            "header_left": "Functional Requirements Specification", "doc_no": "PRJ0012475-PM"},
    "BPS": {"file": f"BIBS_BPS-BDOI_BRD-03_Product_Maintenance_Process_and_Storyboards_v{VERSION}.docx",
            "cover": "Product Maintenance: To-Be Process and Storyboards",
            "header_left": "To-Be Process and Storyboards", "doc_no": "PRJ0012475-PM-BPS"},
    "UXD": {"file": f"BIBS_UXD-BDOI_BRD-03_Product_Maintenance_UX_Design_v{UXD_VERSION}.docx",
            "cover": "Product Maintenance: Screens and UX Design",
            "header_left": "Screens and UX Design", "doc_no": "PRJ0012475-PM-UXD"},
    "RNO": {"file": f"BIBS_RNO-BDOI_BRD-03_Product_Maintenance_Reports_Notifications_Outputs_v{VERSION}.docx",
            "cover": "Product Maintenance: Reports, Notifications and Outputs",
            "header_left": "Reports, Notifications and Outputs", "doc_no": "PRJ0012475-PM-RNO"},
    "ICD": {"file": f"BIBS_ICD-BDOI_BRD-03_Product_Maintenance_Interface_Specification_v{ICD_VERSION}.docx",
            "cover": "Product Maintenance: Interface Specification",
            "header_left": "Interface Specification", "doc_no": "PRJ0012475-PM-ICD"},
}
HEADER_RIGHT = "BDO Insure Brokering System (BIBS) - Product Maintenance"

# Revision log of the FRS. BDOI's version 1.0 is kept as BDOI's issue (author by role).
REVISIONS = [
    ("19 Aug 2026", "1.0", "BDOI's FRS for Product Maintenance (FRPM.001 to FRPM.021).", "BDOI System Analyst"),
    ("09 Oct 2026", "1.1", "Draft for BDOI review: full coverage of the BRD, new items FRPM.022 to FRPM.030, "
     "observations for decision.", AUTHOR),
    ("10 Oct 2026", "1.2", "Business Unit review edition: process flows, screens, notices, documents, reports, "
     "integrations, open questions.", AUTHOR),
    ("11 Oct 2026", "1.3", "Swimlane flows, colour code of the points not in the BRD, change requests with effort.",
     AUTHOR),
    (DATE, "1.4", "Rewritten as a slim FRS that follows the system delivered: one table of requirements per item, business "
     "rules, validations, personas, permissions, parameters and lists. Screens, process flows, storyboards, notices, "
     "reports and documents move to the UX Design, the Process and Storyboards and the Reports, Notifications and "
     "Outputs v1.4; the product master transfer to the Interface Specification v1.0; non-functional requirements, "
     "glossary and decisions to the programme documents. Status names follow the stages of the system. Names of "
     "persons removed.", AUTHOR),
]
REVISIONS.append(
    ("16 Oct 2026", "1.5", "Insurer page completed: contacts, payment terms and remittance details, bank accounts and "
     "linked products under maker-checker (FRPM.023.02, Conformance Register gap G08). Field-level change history of "
     "the master records with maker and checker, read by the Auditor (FRPM.021.03, gap G05).", AUTHOR))
REVISIONS.append(
    ("17 Oct 2026", "1.6", "Insurer page: read-only Ledger Accounts tab with the general ledger accounts posted for "
     "the insurer and its sub-ledger open items (FRPM.023.02, gap G08). Master Data Change Log screen under "
     "Administration with the changes of every master record, filtered by record type, dates, maker and checker, "
     "within the reader's data access, and exported to Excel; the change history of a record outside the reader's "
     "data access is not shown (FRPM.021.03, gap G05).", AUTHOR))
REVISIONS.append(
    ("17 Oct 2026", "1.7", "Insurer page: Co-insurance / Risk Participation tab (BRD-00 CRQ08, client decision of "
     "17-Oct-2026: co-insurance is in scope, only reinsurance is deferred): the participating insurers of the "
     "insurer's default co-insurance arrangement with share %, lead insurer flag and effective dates, maker-checker "
     "with the change log; the authorized shares in force, when they total 100 %, are the default co-insurance split "
     "of an invoice booked without one (FRPM.023.03).", AUTHOR))
REVISION_COMPANION = [
    (DATE, "1.4", "First issue as a separate document of the Product Maintenance set (taken out of the annexes of the "
     "FRS v1.3 and aligned with the system delivered). Numbered 1.4 to match the FRS.", AUTHOR),
    ("16 Oct 2026", "1.5", "Insurer Record (SCR-PM-18) with its tabs: details, contacts, payment terms and "
     "remittance, bank accounts, products, branches and commissions, change history; Change History tab on the "
     "product record. Numbered 1.5 to match the FRS.", AUTHOR),
    ("17 Oct 2026", "1.6", "Ledger Accounts tab on the Insurer Record (SCR-PM-18); the Master Data Change Log "
     "screen. Numbered 1.6 to match the FRS.", AUTHOR),
    ("17 Oct 2026", "1.7", "Co-insurance / Risk Participation tab on the Insurer Record (SCR-PM-18). Numbered 1.7 to "
     "match the FRS.", AUTHOR),
]

# ================================================================================================ shared facts
PERSONAS = [
    # name, what the person does, where, walkthrough account (fictitious test data)
    ("Marketing Account Officer", "Raises and submits package requests; reviews the terms of a client-specific "
     "package; prices new business on a package; requests rate exceptions.",
     "Home, Package Requests, Products, Premium Calculator", "Aileen Torres"),
    ("Marketing Approver", "Marketing Team Leader, Team Head or Unit Head: approves or returns package requests "
     "of Marketing.", "Package Requests, Dashboard", "Marites Herrera"),
    ("TSU Officer", "Prepares quotation slips, keys in insurer terms, compiles the comparative table, prepares the "
     "product requirements, sends advisories, generates renewal requests.",
     "TSU Workbench, Package Requests, Package Expiry, Placement Update Reports", "Teodoro Vergara"),
    ("TSU Team Lead", "Reviews and recommends package requests, approves quotation slips of others, assigns work.",
     "TSU Workbench, Package Requests, Dashboard", "Teresa Yap"),
    ("TSU Head", "Approves package requests for negotiation, reviews negotiated terms, validates and releases "
     "package versions.", "TSU Workbench, Validation Queue, Dashboard", "Tomas Villareal"),
    ("ManCom Member", "Approves the product requirements; the President approves last.", "Package Requests "
     "(Routing & Approvals)", "Manuel Valdez; Patricia Vergara (President)"),
    ("MBS", "Sets up package versions, maintains coverages, clauses, field rules, insurers, rates and incentive "
     "criteria; requests deactivations; follows the product master transfer.",
     "Products, Package Version, Coverages & Clauses, Incentive Criteria, Insurers, Rates & Taxes, Deactivation "
     "Requests, Product Master Transfers", "Monica Villareal"),
    ("Business Administrator", "Authorises product masters, clauses, incentive criteria and the sales "
     "organisation; may validate package versions.", "Products, Coverages & Clauses, Sales Organisation, "
     "Validation Queue", "Bea Garcia"),
    ("Auditor", "Reads every Product Maintenance screen, runs the reports and the audit logs. Changes nothing.",
     "Dashboard, Package Requests, Products, Audit Logs, reports", "Audrey Lim"),
]

MENU = [
    # screen, purpose, personas (A AO, P Marketing approver, T TSU officer, L TSU lead, H TSU head, C ManCom, M MBS,
    # B Business Administrator, U Auditor)
    ("Product Maintenance Home", "Tiles by stage with overdue counts; Product Matrix tabs", "A P T L H C M B U"),
    ("Product Maintenance Dashboard", "KPIs, drill-down, export", "P T L H M B U"),
    ("Package Requests", "Work list by stage; new request", "A P T L H C M B U"),
    ("TSU Workbench", "TSU queues by stage; assign and claim", "T L H"),
    ("Package Expiry", "Expiring, in renewal, expired; renewal requests", "T L H M U"),
    ("Deactivation Requests", "Deactivation requests and decisions", "T L H M B U"),
    ("Product Master Transfers", "Changes for other BDOI systems; transfer status", "M B U"),
    ("Placement Update Reports", "Weekly and on-request reports", "P T L H M U"),
    ("Audit Logs", "Audit trail of Product Maintenance", "H M B U"),
    ("Products", "Product Matrix: packages and non-package products", "A P T L H M B U"),
    ("Validation Queue", "Package versions waiting for validation", "H B"),
    ("Coverages & Clauses", "Lines, cover types, coverages, clause library, field rules", "T L H M B U"),
    ("Incentive Criteria", "Incentive eligibility rules", "M B U"),
    ("Insurers", "Insurer profiles, branches, placement e-mails, commission", "T L H M B U"),
    ("Rates & Taxes", "Rate tables, short-period rates, statutory charges", "M B U"),
    ("Sales Organisation", "Sales units and officers", "B U"),
    ("Premium Calculator", "Premium on the package rate scheme", "A P T M"),
]

LEGEND_NOTE = (
    "Each requirement and rule carries a colour. Black: asked by the BRD, BRD-00 or a programme-wide non-functional "
    "requirement. The points not in the BRD form one group with three colours: green when the iNXT BrokerVerse OOTB "
    "platform offers them at no cost, orange when they need a change request, and blue for best-practice "
    "recommendations, kept in a separate table. A grey label marks a point delivered by the Integration workstream "
    "or by Data Migration (BRD-13).")

# ================================================================================================ FRS
FRS_INTRO = {
    "purpose": (
        "This document states the functional requirements of Product Maintenance in BIBS, the BDO Insure Brokering "
        "System. It replaces version 1.3. BDOI's requirement items FRPM.001 to FRPM.021 keep their numbers; items "
        "FRPM.022 to FRPM.030 complete the BRD. Each requirement, rule and validation is stated once with its ID."),
    "audience": (
        "The BIBS project team; the BDOI Business Unit owners of Product Maintenance (Technical Support Unit, "
        "Marketing Business Services and System Support, Retail Marketing); ManCom; BDO ITG; Internal Audit."),
    "background": [
        "Product set-up, insurer negotiation, approvals, comparative tables and renewal monitoring were done by "
        "e-mail and spreadsheets. BIBS moves them into one controlled workflow with an audit trail.",
        "A package request runs from Marketing or TSU through Marketing approval, TSU review, insurer negotiation, "
        "review of the terms, ManCom sign-off, MBS set-up and validation, to the release of a package version.",
        "The commercial terms of a package are held in numbered versions. New business is priced on the version in "
        "force on the transaction date; another rate needs an approved rate exception.",
        "Non-package quotation requests (BDOI's FRPM.005 to FRPM.010) run in New Business › Proposal Requests and "
        "use the same comparative table and proposal slips.",
    ],
    "in_scope": [
        "Product Matrix: product hierarchy, packages and non-package products, field rules, clause library.",
        "Package requests: new, amendment, update, renewal, retirement, reactivation.",
        "Quotation slips, negotiation rounds, insurer terms, comparative tables, proposal slips.",
        "Review of terms, product requirements, ManCom sign-off, MBS set-up, post-setup validation, release.",
        "Package versions, latest approved rate scheme, rate exceptions, Premium Calculator.",
        "Advisories, expiry and anniversary alerts, renewal requests, deactivation.",
        "Incentive criteria; insurers, rates, sales organisation as used by the packages.",
        "Dashboard, reports, Placement Update Reports, audit logs, notices, product master transfer.",
    ],
    "out_scope": [
        "Sign-in, users, roles and the role-to-action matrix: User Access Maintenance (BRD-11).",
        "Quotations, accounts and booking on a package: New Business (BRD-01).",
        "Endorsement re-rating and commission: Operations (BRD-02). Renewal of client policies: Renewal (BRD-06).",
        "Computation and payout of incentives.",
        "Go-live load of products, insurers and the package map: Data Migration (BRD-13).",
    ],
}

DOC_SET = [
    ("FRS (this document)", "Requirements, rules, validations, personas, permissions, parameters, statuses, change "
     "control."),
    ("Process and Storyboards (BPS) v1.4", "To-be process flows and four storyboards with screenshots."),
    ("UX Design (UXD) v1.4", "Menu by persona and every Product Maintenance screen: fields, validations, actions, "
     "states."),
    ("Reports, Notifications and Outputs (RNO) v1.4", "Reports, documents, exports, notices, e-mails, jobs."),
    ("Interface Specification (ICD) v1.0", "Product master transfer file to other BDOI systems; e-mail channel."),
    ("Developer Specification (DSP) v1.0", "Screens, service operations, field checks, rule enforcement, states, "
     "jobs, audit records, permissions."),
    ("Data Dictionary (DDI) v1.0", "Tables, columns, lists of values, parameters, status models, indexes."),
    ("User Stories (USR) v1.0", "Epics and stories with acceptance criteria, ready for the backlog."),
    ("Workbooks v1.1", "RTM and test cases (trace BRD, FRPM, user story, screen, test case), Fit-Gap, CR Register "
     "and Effort Estimate, Requirements Collection, Walkthrough Users."),
    ("Programme documents", "Non-Functional and Security Requirements, Glossary, Decision and Observation Log, "
     "Document Register (Programme/Common). Non-functional requirements are referenced by NFR ID."),
]

REFERENCES = [
    "Business Requirements Document - Product Maintenance (BRD-03), final version with the addendum PMADD01 to "
    "PMADD08.",
    "BRD-00 and the programme-wide non-functional requirements of the Collections, Claims, ReInsurance and "
    "Operations addenda.",
    "FRS - BDOI BROKERSYS Product Maintenance v1.0 (BDOI), template D051, with Annexes A to F (fields and "
    "quotation slip templates).",
    "BIBS Interface Specification: Product Maintenance v1.0.",
    "BIBS BDOI Feature List vs OOTB and Best Practice v1.0 (baseline of the colour code).",
]

# Business Requirements Mapping: BRD -> FRPM items, where, change from v1.3
MAPPING = [
    ("PMADD01", "Standard reusable product structure hierarchy", "FRPM.003, FRPM.022.01", "BIBS", "Kept"),
    ("PMADD02", "Insurer-specific coverage, deductibles and warranties", "FRPM.003, FRPM.022.02, FRPM.023.01", "BIBS",
     "Kept"),
    ("PMADD03", "Configurable comparative tables with an auditable master", "FRPM.006, FRPM.012.04", "BIBS", "Kept"),
    ("PMADD04", "Exception states and iterative negotiation", "FRPM.006, FRPM.012.03", "BIBS", "Kept"),
    ("PMADD05", "Role-to-action access matrix", "User Access Maintenance FRS; FRPM.028.01", "BIBS (BRD-11)", "Kept"),
    ("PMADD06", "Post-setup validation checkpoint", "FRPM.003.03, FRPM.016.02", "BIBS", "Kept"),
    ("PMADD07, PMADD08", "Incentive criteria on maintained products", "FRPM.003.02, FRPM.018", "BIBS", "Kept"),
    ("BRPM.001, BRPM.002", "Access and restriction to authorised users", "User Access Maintenance FRS; FRPM.002.01",
     "BIBS (BRD-11)", "Kept"),
    ("BRPM.003, BRPM.004", "Mandatory and product-specific fields; rules by product type", "FRPM.003, FRPM.003.08",
     "BIBS", "Kept"),
    ("BRPM.005", "Shared templates with dynamic fields", "FRPM.020, FRPM.030.01", "BIBS", "Kept"),
    ("BRPM.006", "Archive of expired packages; reactivation", "FRPM.002.02, FRPM.003.05, FRPM.019.03", "BIBS", "Kept"),
    ("BRPM.007", "New business on the latest approved rate scheme", "FRPM.003.03, FRPM.024.01, FRPM.024.02", "BIBS",
     "Kept"),
    ("BRPM.008, BRPM.009", "Submit and review package requests", "FRPM.011", "BIBS", "Changed: stages of the system"),
    ("BRPM.010, BRPM.012, BRPM.013", "Negotiation, quotation slips, insurer terms",
     "FRPM.006, FRPM.008 to FRPM.010, FRPM.012, FRPM.013", "BIBS", "Kept"),
    ("BRPM.011", "Receive requests for creation, amendment, update or deletion", "FRPM.005, FRPM.011, FRPM.017, "
     "FRPM.003.04", "BIBS", "Kept"),
    ("BRPM.014", "Consolidated comparative table per package", "FRPM.006.02, FRPM.012.04", "BIBS", "Kept"),
    ("BRPM.015", "Product requirements, ManCom sign-off, MBS processing", "FRPM.014, FRPM.015, FRPM.016", "BIBS",
     "Changed: forwarding to MBS by setting"),
    ("BRPM.016", "Supporting documents for advisories", "FRPM.025.01", "BIBS", "Kept"),
    ("BRPM.017", "Monitor expiry and anniversary dates", "FRPM.004, FRPM.019, FRPM.026.01", "BIBS", "Kept"),
    ("BRPM.018", "Placement Update Reports", "FRPM.007", "BIBS", "Kept"),
    ("BRPM.019", "Dashboard for real-time tracking", "FRPM.001", "BIBS", "Kept"),
    ("BRPM.020", "Document security (password protection)", "FRPM.027.01", "BIBS", "Kept"),
    ("BRPM.021", "Validation and approval workflow enforcement", "FRPM.028.01", "BIBS", "Kept"),
    ("BRPM.022", "Notification and integration", "FRPM.029.01", "BIBS; other BDOI systems", "Changed: transfer file "
     "specified (ICD)"),
    ("BRPM.024", "Audit trail for all actions and changes", "FRPM.021", "BIBS", "Kept"),
]


def _r(rid, text, rule="-", ex="-", colour="k", label=""):
    return (rid, text, colour, label, rule, ex)


# Functional requirements. status against v1.3: Kept, Changed, New
FRUM = [
    {"id": "FRPM.001", "component": "Dashboard", "capability": "Dashboard", "status": "Kept", "brd": "BRPM.019",
     "was": "Same", "reqs": [
         _r("FRPM.001.01", "The dashboard shows, for the user's role and data access, the KPIs Incoming Requests, "
            "In-Progress Requests, For Approval, Expiring Packages, Issued Proposals and Deactivation Requests, with "
            "the filters Period From and To, TSU Officer, Product Line and Package Type.",
            "Counts follow the user's data access. Each KPI drills down to its requests.",
            "Clicking Expiring Packages lists the packages ending in the period."),
         _r("FRPM.001.02", "Each pending request shows its days in the current stage, amber when due soon and red "
            "when the service level of the stage has passed.", "BR-PM-10",
            "A request 30 hours in Marketing approval (24 hours) is red."),
         _r("FRPM.001.03", "The dashboard data is exported to Excel, PDF and CSV with the rows the user sees.", "-",
            "-"),
     ]},
    {"id": "FRPM.002", "component": "Module", "capability": "Product Maintenance module and landing page",
     "status": "Kept", "brd": "BRPM.001, BRPM.002, BRPM.006", "was": "Same", "reqs": [
         _r("FRPM.002.01", "The menu shows only the Product Maintenance screens the user's group profiles allow; "
            "actions the user may not perform are hidden. A direct request without the permission is refused and "
            "logged.", "BR-PM-01", "A Marketing Account Officer does not see the Validation Queue."),
         _r("FRPM.002.02", "The landing page shows tiles by stage with the overdue and due-soon counts, and the "
            "Product Matrix with the tabs Active, Expiring (within 90 days) and Expired. Expired packages are shown "
            "only to users allowed to view archived packages.", "BR-PM-06", "-"),
     ]},
    {"id": "FRPM.003", "component": "Product Matrix", "capability": "Product Matrix maintenance and deactivation",
     "status": "Kept", "brd": "PMADD01, PMADD06, BRPM.003, BRPM.004, BRPM.006, BRPM.011", "was": "Same", "reqs": [
         _r("FRPM.003.01", "Products lists every package and non-package product with line, sub-line, risk code, "
            "cover type, insurer, status, version in force, effective and expiry dates, last update; search, filter "
            "by line and status, sort, page and export.", "-", "The export holds the rows of the filtered list."),
         _r("FRPM.003.02", "Authorised users create and update a Product Matrix record (package or non-package) "
            "with the fields of its line, an Incentive Eligible indicator with an incentive amount or commission rate, "
            "documents, and an approver other than themselves.", "BR-PM-01; setting PM_DIRECT_MAINTENANCE",
            "Incentive Eligible = Yes without amount or rate is refused."),
         _r("FRPM.003.03", "The approver approves, returns or rejects the record; remarks are mandatory to return "
            "or reject. An approved record is Active and selectable in new business. Rate, minimum premium, "
            "commission and TSI limit of an active package change only through a new version.",
            "BR-PM-01, BR-PM-03", "-"),
         _r("FRPM.003.04", "Deactivate Package on an active package asks for the effective date (today or later), "
            "the reason, remarks, documents and the approver; the request number follows PKD-yyyy-n.",
            "Setting PM_DEACTIVATION_ROUTE", "-"),
         _r("FRPM.003.05", "A package whose expiry date passed without a newer released version becomes Expired, is "
            "no longer sold and stays readable with its versions and documents; TSU and MBS are told.",
            "BR-PM-05, BR-PM-06", "-"),
         _r("FRPM.003.06", "Deactivation Requests lists every request with number, package, effective date, status, "
            "requester, request date and package expiry date; the record shows the decision and its history.", "-",
            "-"),
         _r("FRPM.003.07", "The approver, not the requester, approves or rejects a deactivation; a rejection needs "
            "remarks. On approval the package expiry date becomes the later of the effective date and the approval "
            "date; the daily deactivation step then retires the package.", "BR-PM-01", "-"),
         _r("FRPM.003.08", "MBS maintains field rules per line or product: presence, list, range and pattern. Each "
            "rule is authorised by another user. A quotation or account is checked against all rules in scope.",
            "BR-PM-01", "A Motor rule 'plate number required' stops a Motor account without a plate number."),
     ]},
    {"id": "FRPM.004", "component": "Package Expiry", "capability": "Package expiry and anniversary alerts",
     "status": "Kept", "brd": "BRPM.017", "was": "Same", "reqs": [
         _r("FRPM.004.01", "The daily check raises an alert 90 days before the package expiry date and before the "
            "anniversary date, and reminders at 30 and 7 days, once per package, version and alert day; TSU "
            "Officers and TSU Team Leads are told (MBS as well when set).",
            "Settings PACKAGE_EXPIRY_NOTICE_DAYS, PACKAGE_EXPIRY_REMINDER_DAYS, PACKAGE_ANNIVERSARY_NOTICE_DAYS, "
            "PACKAGE_EXPIRY_NOTIFY_MBS", "A package ending in 45 days shows 45 days left."),
         _r("FRPM.004.02", "Package Expiry lists the packages Expiring (days left, anniversary), Renewal in Progress "
            "(open renewal request and stage) and Expired, with the filter Ending within (days) and an export.", "-",
            "-"),
     ]},
    {"id": "FRPM.005", "component": "Quotation Request", "capability": "Non-package quotation requests",
     "status": "Kept", "brd": "BRPM.011", "was": "Same; delivered in New Business › Proposal Requests", "reqs": [
         _r("FRPM.005.01", "Non-package quotation requests are listed with number, date, line, status, dates, TSU "
            "user, Account Officer and age; the number comes from a yearly sequence and cannot be edited. Submission "
            "checks the client, product, risk description, period and mandatory documents.", "-",
            "A request without its mandatory documents cannot be submitted."),
         _r("FRPM.005.02", "TSU approves, returns (reason from the list) or rejects a request; the approver is not "
            "its maker.", "BR-PM-01", "-"),
     ]},
    {"id": "FRPM.006", "component": "Quotation Slip", "capability": "Non-package quotation slips and insurer terms",
     "status": "Kept", "brd": "PMADD03, PMADD04, BRPM.012 to BRPM.014", "was": "Same", "reqs": [
         _r("FRPM.006.01", "A quotation slip per insurer is generated from BDOI's template, approved by another TSU "
            "officer and sent as a password-protected PDF; each send is listed with its status and can be resent.",
            "BR-PM-01, BR-PM-11", "-"),
         _r("FRPM.006.02", "The comparative table shows the QS value and one column per insurer, with several "
            "options per insurer, the response Approved, Not Covered or Others, and the history of every change; "
            "export to Excel and PDF.", "-", "-"),
     ]},
    {"id": "FRPM.007", "component": "Placement Update", "capability": "Placement Update and package status reports",
     "status": "Kept", "brd": "BRPM.018", "was": "Same", "reqs": [
         _r("FRPM.007.01", "The Consolidated Placement Update Report is generated every Thursday at 08:00 for the "
            "last 7 days and on request, stored as PlacementUpdate_MMDDYYYY.xlsx with its run details, and "
            "previewed and downloaded from Placement Update Reports.",
            "Settings PLACEMENT_UPDATE_DAY, PLACEMENT_UPDATE_TIME, PLACEMENT_UPDATE_DAYS, PLACEMENT_UPDATE_SCOPE",
            "-"),
         _r("FRPM.007.02", "The Package Status Update Report lists one or all package requests with type, client or "
            "programme, package, version, stage, days in stage, expiry, rate, insurers and last action; PDF, Excel "
            "and CSV.", "-", "-"),
     ]},
    {"id": "FRPM.008", "component": "Proposal", "capability": "Insurer selection for the proposal",
     "status": "Kept", "brd": "BRPM.010, BRPM.013", "was": "Same", "reqs": [
         _r("FRPM.008.01", "The requestor selects one or more insurers with a response and proceeds to the proposal; "
            "an insurer that declined cannot be selected; the TSU officer is told.", "-", "-"),
     ]},
    {"id": "FRPM.009", "component": "Proposal", "capability": "Final terms and proposal slips",
     "status": "Kept", "brd": "BRPM.010", "was": "Same", "reqs": [
         _r("FRPM.009.01", "The comparative table has a Final Terms for Proposal column, filled from the selected "
            "insurer and editable, with the history kept.", "-", "-"),
         _r("FRPM.009.02", "Proposal slips are generated per insurer, versioned, approved by a TSU officer other "
            "than the preparer and then sent to Marketing.", "BR-PM-01", "-"),
     ]},
    {"id": "FRPM.010", "component": "Proposal", "capability": "Client response", "status": "Kept",
     "brd": "BRPM.010", "was": "Same", "reqs": [
         _r("FRPM.010.01", "The client's response (Accepted, Rejected, Return for Revision) is recorded with date and "
            "remarks; acceptance needs the client's e-mail attached and allows a draft account per accepted risk "
            "group.", "-", "-"),
     ]},
    {"id": "FRPM.011", "component": "Package Request", "capability": "Package requests", "status": "Changed",
     "brd": "BRPM.008, BRPM.009, BRPM.011", "was": "Status list of BDOI's FRS; the screens use the stages of "
     "section 9", "reqs": [
         _r("FRPM.011.01", "Package Requests lists the requests by stage tab with number (PKR-yyyy-n), type, scope, "
            "client or programme, line, stage, assignee, days in stage and overdue marking; search by part of the "
            "number, filter by type, Only My Requests.", "-", "-"),
         _r("FRPM.011.02", "A request has a type (new, amendment, update, renewal, retirement, reactivation), scope "
            "(generic programme or client-specific with the client), name, line, cover type or package, reason, "
            "source (Marketing, TSU, Insurer), the package details of BDOI's Annex E, the requested terms and the "
            "target insurers. A request on an existing package loads the current terms.",
            "BR-PM-12; settings PKG_MIN_POLICIES, PKG_MIN_PREMIUM",
            "Estimated policies 15 is refused: at least 20."),
         _r("FRPM.011.03", "Marketing requests are approved by a Marketing approver other than the maker; the TSU "
            "Team Lead recommends and the TSU Head approves before negotiation. A return needs a reason and sends "
            "the request back to its maker; a draft can be voided.", "BR-PM-01; setting PM_MARKETING_CHAIN", "-"),
     ]},
    {"id": "FRPM.012", "component": "Package Request", "capability": "Package quotation slips, insurer terms and "
     "comparative tables", "status": "Kept", "brd": "PMADD03, PMADD04, BRPM.010, BRPM.012 to BRPM.014",
     "was": "Same", "reqs": [
         _r("FRPM.012.01", "Each negotiation round has a quotation slip numbered PQS-yyyy-n with a reply date (send "
            "date plus 5 days), prepared by a TSU officer, approved by the TSU Team Lead or a co-officer and sent to "
            "the placement e-mail of each insurer as a protected PDF; it can be resent to one insurer. Slips lock at "
            "the ManCom approval.", "BR-PM-01, BR-PM-07, BR-PM-11; setting PKG_QS_REPLY_DAYS", "-"),
         _r("FRPM.012.02", "TSU keys in each insurer's response: outcome, rate, minimum premium, terms per coverage, "
            "conditions, validity, remarks and the reply document. An offer needs a rate; every change keeps the "
            "earlier values. Marketing reads the responses only.", "-", "An offer without a rate is refused."),
         _r("FRPM.012.03", "TSU opens a further round with the insurers that did not decline; all rounds stay on "
            "the request. TSU or the TSU Head closes a request as Not Proceeded with a reason.", "-", "-"),
         _r("FRPM.012.04", "One current master comparative table per request is compiled from the latest round; a "
            "new compilation supersedes the previous one, which stays downloadable. Client views choose the title, "
            "fields and insurers and never change values.", "-", "-"),
     ]},
    {"id": "FRPM.013", "component": "Package Request", "capability": "Terms final and review of the terms",
     "status": "Kept", "brd": "BRPM.010, BRPM.013", "was": "Same", "reqs": [
         _r("FRPM.013.01", "Terms Final selects the insurers of the package with their role (Lead, Participant, "
            "Panel) and share; only insurers that offered terms are selectable and shares total 100%.", "BR-PM-04",
            "-"),
         _r("FRPM.013.02", "The TSU Head reviews the negotiated terms. A client-specific package goes to the "
            "Marketing requestor, who accepts or asks for changes (back to negotiation); a generic programme goes "
            "straight to the requirements.", "-", "-"),
     ]},
    {"id": "FRPM.014", "component": "Package Request", "capability": "Product requirements and ManCom sign-off",
     "status": "Kept", "brd": "BRPM.015", "was": "Same", "reqs": [
         _r("FRPM.014.01", "TSU completes the product requirements: rate or rate per insurer, minimum premium, "
            "commission, TSI limit, computation basis, dates, insurers with role and share, and the signed package "
            "slip. Submission names every missing item.", "-",
            "Submission without the signed package slip is refused."),
         _r("FRPM.014.02", "TSU selects the ManCom approvers; they approve in parallel and the President last. An "
            "approver is not the submitter; a return or rejection needs remarks. The sign-off reference is "
            "MC-<request number> and a sign-off record is attached when no signed sheet is uploaded.",
            "BR-PM-01, BR-PM-07; settings PM_MANCOM_ROUTING, PM_MANCOM_PRESIDENT", "-"),
     ]},
    {"id": "FRPM.015", "component": "Package Request", "capability": "Deployment request to MBS",
     "status": "Changed", "brd": "BRPM.015", "was": "Request for Deployment button only", "reqs": [
         _r("FRPM.015.01", "After the ManCom approval the request goes to MBS with a deployment request numbered "
            "PDR-yyyy-n: at once (setting AUTO, the BRD) or by Request for Deployment (setting MANUAL, BDOI's FRS). "
            "MBS is told. No deployment is possible without the ManCom approval.",
            "BR-PM-08; setting PM_DEPLOYMENT_REQUEST", "-"),
     ]},
    {"id": "FRPM.016", "component": "Package Version", "capability": "MBS set-up and post-setup validation",
     "status": "Kept", "brd": "PMADD06, BRPM.015", "was": "Same", "reqs": [
         _r("FRPM.016.01", "MBS sees the requests With MBS with the ManCom reference and days in stage, and can "
            "return incomplete requirements to TSU with a reason.", "-", "-"),
         _r("FRPM.016.02", "Set Up Package Version creates a draft version from the approved terms (new package: "
            "risk code and name given by MBS). MBS completes and submits it; the TSU Head or the Business "
            "Administrator, neither maker nor submitter, confirms the checklist and validates and releases it, or "
            "returns it with a reason.", "BR-PM-01, BR-PM-09",
            "The MBS user who set up the version cannot validate it."),
     ]},
    {"id": "FRPM.017", "component": "Workflow", "capability": "Assignment and reassignment", "status": "Kept",
     "brd": "BRPM.011", "was": "Same", "reqs": [
         _r("FRPM.017.01", "A TSU Team Lead or Head assigns selected requests of one stage to a team member or "
            "back to the queue; an officer claims an unassigned request. The new assignee is told.", "-", "-"),
     ]},
    {"id": "FRPM.018", "component": "Incentive Criteria", "capability": "Incentive eligibility rules",
     "status": "Kept", "brd": "PMADD07, PMADD08", "was": "Same", "reqs": [
         _r("FRPM.018.01", "MBS maintains incentive criteria: code, name, type, value basis and value, parameters, "
            "products of the matrix (optionally cover type, segment, channel, insurer), effective dates. Each "
            "criterion is authorised by another user; an active one changes through a successor; two criteria with "
            "the same code do not overlap.", "BR-PM-01, BR-PM-13", "-"),
         _r("FRPM.018.02", "When a product expires or is retired, its active criteria are flagged for review and "
            "TSU and MBS are told; criteria are never deleted.", "-", "-"),
     ]},
    {"id": "FRPM.019", "component": "Reports", "capability": "Expiry, renewal and version history reports",
     "status": "Kept", "brd": "BRPM.006, BRPM.017", "was": "Same", "reqs": [
         _r("FRPM.019.01", "The Renewal Request Report lists the expiring packages of the period with any open "
            "renewal request and its stage.", "-", "-"),
         _r("FRPM.019.02", "The Package Expiry Report lists active packages by expiry date (Ending Within, default "
            "90 days) with days left, anniversary and renewal request.", "-", "-"),
         _r("FRPM.019.03", "The Package Version History lists every version with status, dates, submitter, "
            "validator, result, source request and change summary.", "-", "-"),
     ]},
    {"id": "FRPM.020", "component": "Reports", "capability": "Reports builder", "status": "Kept",
     "brd": "BRPM.005, BRPM.018", "was": "Same", "reqs": [
         _r("FRPM.020.01", "Authorised users design, save, share and run reports on the Product Maintenance data "
            "sources, with filters and dates, and export them to Excel, PDF and CSV; every run is logged.", "-", "-"),
     ]},
    {"id": "FRPM.021", "component": "Audit Trail", "capability": "Audit logs", "status": "Kept",
     "brd": "BRPM.024", "was": "Same", "reqs": [
         _r("FRPM.021.01", "Audit Logs lists every Product Maintenance action with time, module, reference, client, "
            "action, description, old and new value, user and remarks, filtered by date, action, reference and user, "
            "and exported as Audit Logs_MMDDYYYY. No one can change or delete an entry.", "BR-PM-05", "-"),
         _r("FRPM.021.02", "Each record shows its own audit and workflow history in time order.", "-", "-"),
         _r("FRPM.021.03", "Every change of a master record (product, insurer, insurer branch, contact and bank "
            "account, commission rate, coverage, clause, rule, rate, list of values) is kept field by field: field, "
            "old value, new value, maker and time of the change, checker and time of the authorization. The insurer "
            "and product pages show it in a Change History tab; the Auditor reads it and nobody changes it. The "
            "Master Data Change Log screen lists the changes of every master record, filtered by record type, date "
            "range, maker and checker, and exports them to Excel. A reader sees only the records of the companies "
            "and branches of his data access; the history of another record answers as not found.",
            "BR-PM-01, BR-PM-05", "A change waiting for authorization shows Pending authorization.", "g"),
     ]},
    {"id": "FRPM.022", "component": "Product Matrix", "capability": "Product hierarchy and clause library",
     "status": "Kept", "brd": "PMADD01, PMADD02", "was": "Same", "reqs": [
         _r("FRPM.022.01", "One hierarchy holds line, cover type (with at most one level of subtype), product and "
            "coverages (Section, Coverage, Peril, Extension). Each record is authorised by another user.", "BR-PM-01",
            "A subtype under a subtype is refused."),
         _r("FRPM.022.02", "The clause library holds warranties, clauses, exclusions and deductible wordings with "
            "line, title, wording and dates; each is authorised by another user and never deleted.", "BR-PM-01, "
            "BR-PM-05", "-"),
     ]},
    {"id": "FRPM.023", "component": "Package Version", "capability": "Insurer-specific package terms",
     "status": "Kept", "brd": "PMADD02", "was": "Same", "reqs": [
         _r("FRPM.023.01", "A version lists its insurers with role, share, rate and minimum premium and, per insurer "
            "and coverage, inclusion, limit, deductible and clauses. Shares total 100% and every insurer has terms "
            "for each included coverage before submission. New business with an insurer outside the panel is "
            "refused.", "BR-PM-04", "-"),
         _r("FRPM.023.02", "The insurer page shows the profile, the contact persons by role, the payment terms "
            "and remittance details (cycle, mode, advice e-mail, currency), the bank accounts by purpose, the "
            "products linked through package versions and commission rates, the branches with LGT and the "
            "commission rates, and, read-only, the general ledger accounts posted for the insurer (debit, credit, "
            "balance) and its sub-ledger open items (amount, settled, outstanding). Each addition or change of the "
            "profile, a contact or a bank account is saved for authorization by another user.", "BR-PM-01",
            "An account number already recorded for the insurer is refused.", "g"),
         _r("FRPM.023.03", "The Co-insurance / Risk Participation tab of the insurer page holds the insurer's "
            "default co-insurance arrangement: each participating insurer with its share %, the lead insurer flag "
            "and the effective dates. Each addition or change is saved for authorization by another user and is "
            "recorded in the change log. Booking uses the authorized shares in force on the booking date as the "
            "co-insurance split of an invoice when the user enters none (FR-OP-007).", "BR-PM-01 (BRD-00 CRQ08)",
            "The shares of one period cannot exceed 100 %; a participating insurer appears once per period; only "
            "one participant leads; an arrangement whose authorized shares in force do not total 100 % or do not "
            "include the insurer itself is not used.", "k"),
     ]},
    {"id": "FRPM.024", "component": "Package Version", "capability": "Package versions and latest approved rate "
     "scheme", "status": "Kept", "brd": "BRPM.007", "was": "Same", "reqs": [
         _r("FRPM.024.01", "The terms of a package are held in numbered versions: Draft, For Validation, Released, "
            "Superseded, Expired. One released version is in force per date; the previous one ends the day before.",
            "BR-PM-02", "-"),
         _r("FRPM.024.02", "New business is priced on the version in force on the transaction date and stores the "
            "version number. A different rate needs a rate exception approved by another user, valid 30 days. "
            "Renewals and endorsements keep the version of the original account.",
            "BR-PM-03; setting RATE_EXCEPTION_VALIDITY_DAYS", "A quotation at 1.10% on a 1.20% scheme needs an "
            "approved exception."),
         _r("FRPM.024.03", "The Premium Calculator computes the premium of a product and insurer on the rate scheme "
            "in force.", "-", "-", "g"),
     ]},
    {"id": "FRPM.025", "component": "Package Request", "capability": "Package advisories", "status": "Kept",
     "brd": "BRPM.016", "was": "Same", "reqs": [
         _r("FRPM.025.01", "On release, retirement or deactivation the advisory is drafted; TSU edits recipients, "
            "subject and text and sends it once the signed package slip and the ManCom sign-off are attached. Groups "
            "are told in the system; e-mail addresses receive a protected PDF.",
            "BR-PM-11, BR-PM-14; setting PKG_ADVISORY_GROUPS", "-"),
     ]},
    {"id": "FRPM.026", "component": "Package Expiry", "capability": "Renewal requests", "status": "Kept",
     "brd": "BRPM.017", "was": "Same", "reqs": [
         _r("FRPM.026.01", "TSU generates a renewal request per selected expiring package, pre-filled from the "
            "version in force with the next term's dates. A package has at most one open renewal request. When set, "
            "the first expiry alert drafts it.", "BR-PM-12; setting PACKAGE_RENEWAL_AUTODRAFT", "-"),
     ]},
    {"id": "FRPM.027", "component": "Documents", "capability": "Document protection", "status": "Kept",
     "brd": "BRPM.020", "was": "Same", "reqs": [
         _r("FRPM.027.01", "Every document e-mailed outside BIBS is a password-protected PDF or Excel file with the "
            "password in a separate e-mail. Package documents open only for users with access to the request.",
            "BR-PM-11", "-"),
     ]},
    {"id": "FRPM.028", "component": "Workflow", "capability": "Workflow enforcement and service levels",
     "status": "Kept", "brd": "BRPM.021, PMADD05", "was": "Same", "reqs": [
         _r("FRPM.028.01", "Each action checks the permission, the segregation of duties and the data before the "
            "request moves; only the transitions of the workflow are possible. Each stage has a service level in "
            "hours; overdue requests raise an alert.", "BR-PM-01, BR-PM-10; settings PKG_SLA_*",
            "'Approve' on a request in negotiation is refused."),
     ]},
    {"id": "FRPM.029", "component": "Notifications", "capability": "Notices and product master transfer",
     "status": "Changed", "brd": "BRPM.022", "was": "Product master changes recorded; transfer to be decided",
     "reqs": [
         _r("FRPM.029.01", "The users concerned are told in the system of each stage entry, assignment, release, "
            "expiry and advisory; e-mail where shown in the RNO.", "-", "-"),
         _r("FRPM.029.02", "Each release, retirement and expiry of a package is recorded as a product master change "
            "and sent every 30 minutes in the file ProductMaster_yyyyMMddHHmmss.csv to the folder of the receiving "
            "systems; a failed transfer is alerted and can be processed again.", "Interface Specification IF-PM-01",
            "-", "k", "int"),
     ]},
    {"id": "FRPM.030", "component": "Documents", "capability": "Shared document templates", "status": "Kept",
     "brd": "BRPM.005", "was": "Same", "reqs": [
         _r("FRPM.030.01", "Package documents (request form, quotation slip, comparative table, package slip, "
            "advisory) come from versioned templates with line-specific sections; BDOI's Annex D and F templates are "
            "loaded; each document records the template version.", "-", "-"),
     ]},
]

BUSINESS_RULES = [
    ("BR-PM-01", "A request, quotation slip, proposal, ManCom approval, package version, master record, incentive "
     "criterion, deactivation or rate exception is decided by a user other than its maker or submitter.", "k"),
    ("BR-PM-02", "One released version of a package is in force at any date; the previous one ends the day before.",
     "k"),
    ("BR-PM-03", "New business is priced on the version in force on the transaction date; another rate needs an "
     "approved rate exception.", "k"),
    ("BR-PM-04", "Co-insurance shares of a package total 100%; every insurer has terms for each included coverage.",
     "k"),
    ("BR-PM-05", "Packages, versions, clauses, documents and audit entries are never deleted; they are end-dated, "
     "retired or expired.", "k"),
    ("BR-PM-06", "Expired and retired packages are visible only to users allowed to view archived packages.", "k"),
    ("BR-PM-07", "Quotation slips lock at the ManCom approval.", "k"),
    ("BR-PM-08", "A request reaches MBS only after the ManCom approval.", "k"),
    ("BR-PM-09", "A package version is sold only after its post-setup validation.", "k"),
    ("BR-PM-10", "Each workflow stage has a service level; a request past it is overdue and alerted.", "k"),
    ("BR-PM-11", "Every document e-mailed outside BIBS is password protected; the password is sent separately.", "k"),
    ("BR-PM-12", "A package has at most one open renewal request; a product has at most one version in set-up.", "k"),
    ("BR-PM-13", "Incentive criteria apply only to products active on their dates and do not overlap for one code.",
     "k"),
    ("BR-PM-14", "An advisory is sent only with its supporting documents attached.", "k"),
]

MESSAGES = [
    # screen or step, condition, message (as shown)
    ("New Package Request", "Name missing", "Enter the package or programme name"),
    ("New Package Request", "Line missing", "Select the product line"),
    ("New Package Request", "Reason missing", "Select the reason"),
    ("New Package Request", "Cover type missing on a new package", "Select the cover type"),
    ("New Package Request", "Package missing on a change", "Select the package product"),
    ("New Package Request", "Client missing on a client-specific package", "Select the client"),
    ("New Package Request", "End before start", "The package end date must be after its start date"),
    ("New Package Request", "Estimated policies below the minimum", "Enter at least 20 policies"),
    ("New Package Request", "Estimated premium below the minimum", "Enter at least 5,000,000.00"),
    ("New Package Request", "Incentive eligible without value", "Enter the incentive amount or the incentive "
     "commission rate"),
    ("Package Request", "Action not allowed in the stage", "'<action>' is not allowed while <reference> is in stage "
     "<stage name>"),
    ("Package Request", "Maker approves own request", "A package request is approved by someone other than its maker"),
    ("Negotiation", "Preparer approves own slip", "The quotation slip is approved by a TL or a co-officer"),
    ("Negotiation", "Insurer without placement e-mail", "<insurer> has no placement e-mail address"),
    ("Negotiation", "Change after ManCom", "The slips are locked since the ManCom sign-off"),
    ("Insurer response", "Offer without rate", "Enter the rate offered by <insurer> (<outcome>)"),
    ("Terms Final", "Shares not 100%", "The co-insurance shares must add up to 100%"),
    ("Requirements", "Item missing", "Complete the requirements: <missing items>"),
    ("ManCom", "Decision by a user not selected", "No ManCom approval of this request waits for you"),
    ("ManCom", "Return or rejection without remarks", "Enter the remarks of the rejection or the return"),
    ("Package Request", "TSU Head approves own recommendation", "The TSU Head approval is given by someone other than "
     "the recommender"),
    ("Package Version", "Rate missing", "Enter the package rate or a rate for every insurer"),
    ("Package Version", "Effective date in the past", "The effective date cannot be before today"),
    ("Package Version", "No coverage", "Include at least one coverage"),
    ("Package Version", "Insurer without terms", "Insurer <code> has no terms for coverage <coverage>"),
    ("Validation", "Maker validates", "A package version is validated by someone other than its maker and submitter"),
    ("Coverages & Clauses", "Wording missing", "Enter the wording"),
    ("Coverages & Clauses", "Code format", "Use A-Z, 0-9 and _"),
    ("Incentive Criteria", "No product", "Select at least one product of the matrix"),
    ("Advisory", "Documents missing", "Attach the supporting documents before sending: <documents>"),
    ("Product Matrix", "Approver is the maker", "Select an approver, other than you, who may approve products"),
    ("Product Master Transfers", "Retry of a change that did not fail",
     "Only a failed product master change is processed again"),
]

PERMISSIONS = [
    # function, AO, Mkt approver, TSU, TSU TL, TSU Head, ManCom, MBS, Bus. Admin, Auditor
    ("View Product Maintenance screens", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y"),
    ("Create and submit package requests", "Y", "Y", "Y", "Y", "-", "-", "-", "-", "-"),
    ("Approve package requests (Marketing)", "-", "Y", "-", "-", "-", "-", "-", "-", "-"),
    ("Recommend (TSU Team Lead) and approve (TSU Head)", "-", "-", "-", "Y", "Y", "-", "-", "-", "-"),
    ("Prepare slips, key in terms, compile comparatives", "-", "-", "Y", "Y", "-", "-", "-", "-", "-"),
    ("Approve quotation slips of others", "-", "-", "-", "Y", "-", "-", "-", "-", "-"),
    ("Approve product requirements (ManCom)", "-", "-", "-", "-", "-", "Y", "-", "-", "-"),
    ("Set up package versions; maintain masters", "-", "-", "-", "-", "-", "-", "Y", "-", "-"),
    ("Validate and release package versions", "-", "-", "-", "-", "Y", "-", "-", "Y", "-"),
    ("Authorise masters, clauses, incentive criteria", "-", "-", "-", "-", "-", "-", "-", "Y", "-"),
    ("Send advisories; generate renewal requests", "-", "-", "Y", "Y", "Y", "-", "-", "-", "-"),
    ("Reports and audit logs", "-", "Y", "Y", "Y", "Y", "-", "Y", "Y", "Y"),
]
PERMISSION_HEADS = ["Function", "AO", "Mkt", "TSU", "TL", "Head", "ManCom", "MBS", "BA", "Aud."]

PARAMETERS = [
    # setting, default, range, area, colour
    ("PKG_REQUEST_PREFIX: package request number prefix", "PKR-", "-", "Numbering", "k"),
    ("PKG_QS_PREFIX: quotation slip number prefix", "PQS-", "-", "Numbering", "k"),
    ("PKG_DEACTIVATION_PREFIX / PKG_DEPLOYMENT_PREFIX", "PKD- / PDR-", "-", "Numbering", "k"),
    ("PKG_QS_REPLY_DAYS: days insurers have to reply", "5", "1 to 60", "Negotiation", "k"),
    ("PKG_MIN_POLICIES: least estimated policies of a request", "20", "0 to 100,000", "Request", "k"),
    ("PKG_MIN_PREMIUM: least estimated basic premium", "5,000,000.00", "0 or more", "Request", "k"),
    ("PKG_SLA_MKT_APPROVAL / TSU_REVIEW / TSU_APPROVAL (hours)", "24 / 24 / 24", "1 to 720", "Service levels", "k"),
    ("PKG_SLA_NEGOTIATION (hours)", "120", "1 to 2,160", "Service levels", "k"),
    ("PKG_SLA_MANCOM / MBS_SETUP (hours)", "72 / 48", "1 to 720", "Service levels", "k"),
    ("PACKAGE_EXPIRY_NOTICE_DAYS / PACKAGE_ANNIVERSARY_NOTICE_DAYS", "90 / 90", "1 to 365", "Expiry", "k"),
    ("PACKAGE_EXPIRY_REMINDER_DAYS", "30, 7", "List of days", "Expiry", "k"),
    ("PACKAGE_EXPIRY_NOTIFY_MBS", "No", "Yes / No", "Expiry", "k"),
    ("PACKAGE_RENEWAL_AUTODRAFT", "No", "Yes / No", "Expiry", "k"),
    ("PKG_ADVISORY_GROUPS: default advisory recipients", "Marketing, TSU, MBS, Operations", "List", "Advisory", "k"),
    ("PLACEMENT_UPDATE_DAY / TIME / DAYS / SCOPE", "Thursday / 08:00 / 7 / quotation requests", "-", "Reports",
     "k"),
    ("PM_MARKETING_CHAIN", "ANY_ONE (one Marketing approval)", "ANY_ONE, TL_TH_UH", "Workflow", "k"),
    ("PM_MANCOM_ROUTING / PM_MANCOM_PRESIDENT", "Selected in parallel, then the President / to be named", "-",
     "Workflow", "k"),
    ("PM_DEPLOYMENT_REQUEST", "AUTO", "AUTO, MANUAL", "Workflow", "k"),
    ("PM_DEACTIVATION_ROUTE", "EFFECTIVE_DATED", "EFFECTIVE_DATED, RETIRE_REQUEST", "Workflow", "k"),
    ("PM_DIRECT_MAINTENANCE", "Yes", "Yes / No", "Product Matrix", "k"),
    ("RATE_EXCEPTION_VALIDITY_DAYS", "30", "Days", "Rate scheme", "k"),
    ("OTHER_CHARGES_ENABLED: bill other charges with the premium", "No", "Yes / No", "Rates", "g"),
]

LOVS = [
    ("Request type", "New package; Amendment; Update; Renewal; Retirement; Reactivation"),
    ("Request scope", "Generic programme; Client-specific package"),
    ("Request source", "Marketing; TSU; Insurer"),
    ("Request reason (PKG_REQUEST_REASON)", "Eight values maintained in the lists of values (Data Dictionary)"),
    ("Response outcome (PKG_RESPONSE_OUTCOME)", "Six values, from accepted as requested to declined and no response"),
    ("Not proceeded reason (PKG_NOT_PROCEEDED_REASON)", "Five values"),
    ("Deactivation reason (PKG_DEACTIVATION_REASON)", "Six values"),
    ("Advisory recipient group (PKG_ADVISORY_GROUP)", "Marketing; TSU; MBS; Processing; Operations"),
    ("Insurer role", "Lead; Participant; Panel"),
    ("Coverage kind / Clause kind", "Section, Coverage, Peril, Extension / Warranty, Clause, Exclusion, Deductible "
     "wording"),
    ("Incentive type (INCENTIVE_TYPE)", "Values supplied by BDOI (for example CPC2)"),
]

STAGE_ROWS = [
    ("Draft request", "Being prepared by Marketing or TSU.", "For Marketing approval; For TSU review; Voided"),
    ("For Marketing approval", "With the Marketing approver.", "For TSU review; Draft (returned)"),
    ("For TSU review", "TSU Team Lead reviews and recommends.", "For TSU Head approval; Draft (returned)"),
    ("For TSU Head approval", "TSU Head approves.", "Insurer negotiation; Requirements preparation (no "
     "negotiation); Not proceeded"),
    ("Insurer negotiation", "Rounds, slips, responses, comparative tables.", "Negotiated terms review; Not "
     "proceeded"),
    ("Negotiated terms review", "TSU Head reviews the terms.", "Marketing review of terms; Requirements preparation"),
    ("Marketing review of terms", "Client-specific packages only.", "Requirements preparation; Insurer negotiation"),
    ("Requirements preparation", "TSU completes the product requirements.", "For ManCom sign-off"),
    ("For ManCom sign-off", "Selected approvers, then the President.", "With MBS for set-up; ManCom approved; "
     "Requirements preparation (returned)"),
    ("ManCom approved, deployment to request", "Setting MANUAL only.", "With MBS for set-up"),
    ("With MBS for set-up", "MBS sets up the version.", "Package version for validation; Requirements preparation "
     "(returned)"),
    ("Package version for validation", "Validator checks and releases.", "Released; With MBS (returned)"),
    ("Released / Package retired / Not proceeded / Voided", "Closed.", "-"),
]
STATUSES = {
    "Package request": STAGE_ROWS,
    "Package version": [("Draft", "Being set up by MBS.", "For Validation"),
                        ("For Validation", "Waiting for the validator.", "Released; Draft (returned)"),
                        ("Released", "Sells from its effective date.", "Superseded; Expired"),
                        ("Superseded", "Ended by a newer released version.", "-"),
                        ("Expired", "End date passed without a newer version.", "-")],
    "Deactivation request": [("Pending Approval", "With the approver.", "Approved; Rejected; Cancelled"),
                             ("Approved", "Expiry date set; retired by the daily step.", "-"),
                             ("Rejected", "Package stays active.", "-"),
                             ("Cancelled", "Withdrawn by the requester.", "-")],
    "Product": [("Active", "Sellable.", "Expired; Retired"), ("Expired", "Not sold; readable.", "Active "
                "(reactivation request)"), ("Retired", "Not sold; readable.", "-")],
}
BDOI_STATUS_MAP = [
    ("Draft; Returned for Revision", "Draft request"),
    ("Pending Marketing TL / TH / UH Approval", "For Marketing approval"),
    ("For TSU Review", "For TSU review; For TSU Head approval"),
    ("For Quotation Slip Creation; QS for Approval; QS Returned; QS Rejected; Under Negotiation",
     "Insurer negotiation (slip status shown on the round)"),
    ("Terms Agreed", "Negotiated terms review; Marketing review of terms"),
    ("Proposal Ready; Proposal Rejected", "Requirements preparation"),
    ("Awaiting ManCom Approval", "For ManCom sign-off"),
    ("For Deployment; Draft Setup; Package Deployment Returned", "With MBS for set-up"),
    ("Pending Approval; Package Deployment for Review", "Package version for validation"),
    ("Approved; Deployed", "Released"),
    ("Rejected", "Voided; Not proceeded"),
]

ASSUMPTIONS = [
    ("A-PM-01", "The final BRD and its addendum are the baseline."),
    ("A-PM-02", "Package request volume is low (tens a month)."),
    ("A-PM-03", "Insurers are reached by e-mail; their replies are keyed in by TSU."),
]
DEPENDENCIES = [
    ("D-PM-01", "BDOI IT gives the receiving systems, folder and timing of the product master file (ICD)."),
    ("D-PM-02", "BDOI IT gives the e-mail channel and sender mailbox for insurer and advisory e-mails."),
    ("D-PM-03", "BDOI gives the layouts of the request form, package slip, advisory and client comparative view."),
    ("D-PM-04", "BDOI defines CPC2 and the other incentive types."),
    ("D-PM-05", "Renewal (BRD-06) passes the version of the expiring account."),
]
OPEN_QUESTIONS = [
    ("Q-PM-01", "Name the President for the ManCom routing (setting PM_MANCOM_PRESIDENT) and confirm the routing.",
     "ManCom; Product Owner", "30 Nov 2026"),
    ("Q-PM-02", "Marketing approval: one approval (BRD) or Team Leader, Team Head and Unit Head (BDOI's FRS)?",
     "Head, Retail Marketing", "30 Nov 2026"),
    ("Q-PM-03", "Forwarding to MBS after ManCom: automatic (BRD) or Request for Deployment (BDOI's FRS)?",
     "Head, MBS; Head, TSU", "30 Nov 2026"),
    ("Q-PM-04", "Deactivation: effective-dated request (BDOI's FRS) or retirement package request?",
     "Head, TSU", "30 Nov 2026"),
    ("Q-PM-05", "Confirm the post-setup validation checklist and whether the Business Administrator validates.",
     "Head, TSU; Head, MBS", "30 Nov 2026"),
    ("Q-PM-06", "Confirm the service levels of each stage and who is alerted.", "Head, TSU", "30 Nov 2026"),
    ("Q-PM-07", "Confirm the password convention of protected documents.", "Product Owner", "30 Nov 2026"),
    ("Q-PM-08", "Name the systems that receive the product master file, its folder and timing.",
     "Product Owner; BDO ITG", "30 Nov 2026"),
    ("Q-PM-09", "Agree one list of status names (BDOI's list or the stages, section 9).", "Product Owner",
     "30 Nov 2026"),
    ("Q-PM-10", "Non-package quotation requests: keep in this FRS or move to the New Business FRS.",
     "Product Owner", "30 Nov 2026"),
]

CRS = [
    # id, title, status, FRS, effort (analysis, backend, frontend, test, doc), note
    ("CR-PM-101", "Accessibility of the Product Maintenance screens (WCAG 2.2 level AA)", "Open",
     "NFR-ACC-01", (2.0, 0.0, 5.5, 3.0, 1.0), "Not in the BRD; OOTB partial."),
]
OOTB_POINTS = [
    ("G-02", "Premium Calculator on the package rate scheme", "FRPM.024.03"),
    ("G-06", "Other charges (documentation, notarial fees) billed with the premium, switched off by default",
     "Parameters"),
]
INTEGRATION_POINTS = [
    ("IS-PM-01", "Product master transfer file to other BDOI systems", "FRPM.029.02; ICD IF-PM-01"),
    ("IS-PM-02", "E-mail channel for slips, advisories and passwords", "FRPM.012.01, FRPM.027.01; ICD IF-PM-02"),
    ("DM-PM-01", "Go-live load of lines, products, coverages, clauses, rates, insurers, commission, package map",
     "Data Migration (BRD-13)"),
    ("DM-PM-02", "Packages in force at go-live set up through package requests", "FRPM.024.01"),
]
BEST_PRACTICE = [
    ("BP-PM-01", "Periodic package performance review (take-up, premium, loss ratio) with a keep, redesign or "
     "retire decision", "Medium", 6),
    ("BP-PM-02", "Product disclosure sheet per package for the bank channel, produced from the released version",
     "High", 5),
    ("BP-PM-03", "Record of the data shared with each insurer of a client-specific package (Data Privacy Act)",
     "Medium", 3),
]

CHANGES_V13 = [
    ("Annexes G to Y of v1.3", "Moved to the UXD, BPS, RNO, ICD, DSP, DDI and the programme documents; the FRS keeps "
     "requirements, rules, validations, personas, permissions, parameters and statuses."),
    ("Acceptance criteria", "Kept as the Example column and in the User Stories v1.0."),
    ("FRPM.011", "Status list replaced by the stages of the system with a mapping to BDOI's names (section 9)."),
    ("FRPM.015", "Forwarding to MBS by setting PM_DEPLOYMENT_REQUEST (AUTO by default, the BRD)."),
    ("FRPM.024.03", "Premium Calculator stated as its own requirement (green)."),
    ("FRPM.029", "Split: notices (029.01) and the product master transfer file (029.02, Interface Specification)."),
    ("Business rules", "Thirteen rules of Annex J.1 renumbered BR-PM-01 to BR-PM-14."),
    ("Cover page and revision log", "Names of persons replaced by roles."),
]
