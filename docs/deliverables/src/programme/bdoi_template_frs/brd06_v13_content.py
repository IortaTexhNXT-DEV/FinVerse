"""Content of the Renewal document set v1.3 (BRD-06) in BDOI's template: the FRS, the process and storyboards (BPS),
the UX design (UXD), the reports, notifications and outputs (RNO) and the interface specification (ICD). Read by
build_brd06_v13.py, brd06_pack.py and build_brd06_v13_workbooks.py. Business wording only.

The requirement rows and their acceptance criteria are in brd06_v13_requirements.yaml; the mapping of the BRD lines in
brd06_v13_brd_lines.yaml. Colour of a point: "k" black (in the BRD, BRD-00 or a programme NFR), "g" green (not in the
BRD, offered by the iNXT BrokerVerse OOTB platform), "o" orange (change request), "b" blue (best practice, separate
table). A label "int" or "dm" adds the grey [Integration scope] or [Data Migration scope] label.
"""

from __future__ import annotations

from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
VERSION = "1.3"
WB_VERSION = "1.1"
DATE = "16 Oct 2026"
DATE_US = "10/16/2026"
AUTHOR = "iorta TechNXT Project Team (drafting support); for review by the BDOI Product Owner"
OWNER = "BDOI Product Owner, Renewal"
ICD_VERSION = "1.0"

DOCS = {
    "FRS": {"file": f"BIBS_FRS-BDOI_BRD-06_Renewal_v{VERSION}.docx",
            "cover": "Functional Requirements Specification: Renewal",
            "header_left": "Functional Requirements Specification", "doc_no": "PRJ0012475-RN"},
    "BPS": {"file": f"BIBS_BPS-BDOI_BRD-06_Renewal_Process_and_Storyboards_v{VERSION}.docx",
            "cover": "Renewal: To-Be Process and Storyboards",
            "header_left": "To-Be Process and Storyboards", "doc_no": "PRJ0012475-RN-BPS"},
    "UXD": {"file": f"BIBS_UXD-BDOI_BRD-06_Renewal_UX_Design_v{VERSION}.docx",
            "cover": "Renewal: Screens and UX Design",
            "header_left": "Screens and UX Design", "doc_no": "PRJ0012475-RN-UXD"},
    "RNO": {"file": f"BIBS_RNO-BDOI_BRD-06_Renewal_Reports_Notifications_Outputs_v{VERSION}.docx",
            "cover": "Renewal: Reports, Notifications and Outputs",
            "header_left": "Reports, Notifications and Outputs", "doc_no": "PRJ0012475-RN-RNO"},
    "ICD": {"file": f"BIBS_ICD-BDOI_BRD-06_Renewal_Interface_Specification_v{ICD_VERSION}.docx",
            "cover": "Renewal: Interface Specification",
            "header_left": "Interface Specification", "doc_no": "PRJ0012475-RN-ICD"},
}
HEADER_RIGHT = "BDO Insure Brokering System (BIBS) - Renewal"

# Revision log of the FRS. BDOI's version 1.0 is kept as BDOI's issue (author by role).
REVISIONS = [
    ("11 Jun 2026", "1.0", "BDOI's FRS for Renewal (FRRN.001 to FRRN.044).", "BDOI System Analyst"),
    ("09 Oct 2026", "1.1", "Draft for BDOI review: full coverage of the Renewal BRD of 8-Oct-2026, acceptance criteria, "
     "items FRRN.045 to FRRN.049, Business Unit review annexes.", AUTHOR),
    ("11 Oct 2026", "1.2", "Product Owner review: colour code of the points not in the BRD, change requests with "
     "effort.", AUTHOR),
    (DATE, "1.3", "Rewritten as a slim FRS that follows the system delivered: one table of requirements per item, "
     "business rules, validations, personas, permissions, parameters, lists and statuses. Screens, flows, storyboards, "
     "letters, notices and reports move to the UX Design, the Process and Storyboards and the Reports, Notifications "
     "and Outputs v1.3; the interfaces to the Interface Specification v1.0; acceptance criteria to the User Stories and "
     "the test cases; non-functional requirements, glossary and decisions to the programme documents. Names of "
     "persons removed.", AUTHOR),
]
REVISION_COMPANION = [
    (DATE, "1.3", "First issue as a separate document of the Renewal set (taken out of the annexes of the FRS v1.2 and "
     "aligned with the system delivered). Numbered 1.3 to match the FRS.", AUTHOR),
]

# ================================================================================================ shared facts
PERSONAS = [
    # name, what the person does, where, walkthrough account (fictitious test data)
    ("Marketing Team Lead", "Generates and initiates the expiry list, assigns and transfers renewals, reviews, "
     "returns, overrides and posts dispositions, decides transfers and TSU requests.",
     "Renewal Home, Dashboard, Expiry List, TL Review, Transfers, Letters, KYC Monitoring",
     "Marites Marketing Lead; Rosario Renewal Team Lead"),
    ("Marketing Account Officer", "Gives the disposition of assigned renewals, sends the Renewal Advice and letters, "
     "records the client's acceptance, raises transfers and TSU requests.",
     "My Dispositions, Renewal, Letters, Transfer Requests, KYC Monitoring",
     "Aileen Account Officer; Arnel Account Officer"),
    ("Processing Team Lead", "Assigns posted renewals to Processing Officers, uploads files, approves package "
     "choices, follows placement and billing.", "Processing Dashboard, Processing Worklist, Renewal Setup (package "
     "choices), Submitted for Placement", "Patricia Processing Lead"),
    ("Processing Officer", "Sends renewals to the insurers, records the insurer's answer, places, books, sends "
     "e-policies and billing files.", "Processing Worklist, Insurer Batches, Submitted for Placement, E-Policies, CLPC "
     "Billing Files", "Paolo Processor"),
    ("Contact Center", "Follows up clients who have not answered the Renewal Advice; sees no premium or commission.",
     "Follow-ups, Renewal", "Cecilia Contact Center"),
    ("LAMD", "Uploads the LAMD loan reports and reads the matching results.", "LAMD Reports, RMU Account "
     "Maintenance", "Leonardo Loan Monitoring"),
    ("TSU Officer", "Works the quotation requests raised from renewals.", "TSU Requests", "Teodoro TSU Analyst"),
    ("Business Administrator", "Keeps Renewal Setup, risk codes, lists of values, letter templates, RMU codes and "
     "the budget.", "Renewal Setup, Risk Code Maintenance, Renewal Annual Budget, Channel Monitor, Audit Logs",
     "Bea Business Admin"),
    ("System Administrator", "Keeps the group profiles and the channel connections.", "Channel Monitor; User Access "
     "screens (BRD-11)", "SIT System Administrator"),
    ("Auditor", "Reads renewals, their history, the reports and the audit logs. Changes nothing.",
     "Renewal Dashboard, Renewal Accounts, Audit Logs, reports", "Audrey Auditor"),
]

MENU = [
    # screen, purpose, personas (T Marketing TL, A AO, P Processing TL, O Processing Officer, C Contact Center,
    # L LAMD, U TSU, B Business Administrator, S System Administrator, X Auditor)
    ("Renewal Home", "Due dates, work queues, status, workload", "T A P O C L B X"),
    ("Renewal Dashboard", "Marketing KPIs and drill-downs", "T A P O B X"),
    ("Renewal Accounts", "Clean, Review, Non-Renewable and All panels", "T A P O B X"),
    ("Processing Dashboard", "Processing KPIs and drill-downs", "P O"),
    ("Expiry List", "Extraction, initiation, assignment, transfer", "T A P O C L B X"),
    ("My Dispositions", "The Account Officer's renewals", "T A"),
    ("TL Review", "Review, return, override, post", "T"),
    ("Transfers", "Transfers to and from the unit", "T A"),
    ("Transfer Requests", "Referrals to another unit", "T A"),
    ("Processing Worklist", "Processing tabs, assignment, uploads", "P O"),
    ("Submitted for Approval", "Accepted accounts to approve", "T"),
    ("Submitted for Placement", "Placement, responses, return", "P O"),
    ("TSU Requests", "Quotation requests to TSU", "T U"),
    ("KYC Monitoring", "KYC due, upcoming, completed", "T A P O"),
    ("Risk Code Maintenance", "Renewable indicator of risk codes", "B T P"),
    ("CLPC Billing Files", "Billing files to LMS", "P O"),
    ("E-Policies", "E-policy receipts and sending", "P O"),
    ("Insurer Batches", "Requests to the insurers", "P O"),
    ("Letters", "Renewal Advice, closing letters", "T A P O"),
    ("Follow-ups", "Contact Center follow-up", "C"),
    ("LAMD Reports", "LAMD and BDOFC/SOLD uploads", "L"),
    ("RMU Account Maintenance", "RMU Account Officer codes", "B L"),
    ("Channel Monitor", "CCM and MFT messages and connections", "T P O B S"),
    ("Audit Logs", "Audit trail of Renewal", "T P B X"),
    ("Renewal Annual Budget", "Budgets by year and hierarchy", "B"),
    ("Renewal Setup", "Checks, rules, decision matrix, package map, go-live", "B P O"),
]
PERSONA_CODES = {"T": "Mkt TL", "A": "AO", "P": "Proc. TL", "O": "Proc. Officer", "C": "Contact Center", "L": "LAMD",
                 "U": "TSU", "B": "Bus. Admin", "S": "Sys. Admin", "X": "Auditor"}

LEGEND_NOTE = (
    "Each requirement and rule carries a colour. Black: asked by the BRD, BRD-00 or a programme-wide non-functional "
    "requirement. The points not in the BRD form one group with three colours: green when the iNXT BrokerVerse OOTB "
    "platform offers them at no cost, orange when they need a change request, and blue for best-practice "
    "recommendations, kept in a separate table. A grey label marks a point delivered by the Integration workstream "
    "or by Data Migration (BRD-13).")

# ================================================================================================ FRS
FRS_INTRO = {
    "purpose": (
        "This document states the functional requirements of Renewal in BIBS, the BDO Insure Brokering System. It "
        "replaces version 1.2. BDOI's items FRRN.001 to FRRN.044 keep their numbers; items FRRN.045 to FRRN.049 "
        "complete the BRD. Each requirement, rule and validation is stated once with its ID."),
    "audience": (
        "The BIBS project team; the BDOI Business Unit owners of Renewal (Marketing, Processing, Contact Center, "
        "LAMD, MBS); BDO ITG; Internal Audit."),
    "background": [
        "Expiring accounts were tracked in spreadsheets and e-mail. BIBS extracts them, checks and classifies them, "
        "routes them to Marketing and Processing and keeps every step in one audit trail.",
        "A renewal runs from the expiry list through the disposition by Marketing, the Team Lead's posting, the "
        "insurer's answer, the Renewal Advice and the client's acceptance to placement and booking through the "
        "shared New Business flow.",
        "Straight-through CBG renewals follow the LAMD reports, the hold cover and the CLPC billing without an "
        "Account Officer.",
        "BDOI decided on 26-Sep-2026 that all modules go live together; the renewals expiring after go-live are "
        "taken over once from the migrated policies.",
    ],
    "in_scope": [
        "Expiry list: daily extraction, generation by range, initiation, duplicates, sanitation, classification.",
        "Assignment, transfer and referral, disposition, review, override and posting by Marketing.",
        "Processing worklist, insurer requests and dispositions, LAMD and BDOFC/SOLD reports, uploads.",
        "Renewal Advice, client letters, NRNS, file protection, delivery through CCM and MFT.",
        "Acceptance, approval, placement, placement responses, Insurance Advice, e-policies, billing files.",
        "Hold cover requests and responses; incentive code; KYC monitoring.",
        "Dashboards, Renewal Accounts panels, reports, audit logs; Renewal Setup, risk codes, RMU codes, budget.",
        "Go-live take-over, migrated packages, renewal of submitted policies, Contact Center follow-up.",
    ],
    "out_scope": [
        "Sign-in, users, group profiles and permissions: User Access Maintenance (BRD-11).",
        "Quotation, proposal, placement documents of new business and booking: New Business (BRD-01).",
        "Cashiering and payment application: Operations Cashiering (BRD-02).",
        "Package versions and the product master: Product Maintenance (BRD-03).",
        "Renewal of Employee Benefits lines (HMO, group life, group personal accident): BRD-08.",
        "Load of the policies in force at go-live: Data Migration (BRD-13).",
    ],
}

DOC_SET = [
    ("FRS (this document)", "Requirements, rules, validations, personas, permissions, parameters, statuses, change "
     "control."),
    (f"Process and Storyboards (BPS) v{VERSION}", "End-to-end journey, to-be processes and five storyboards with "
     "screenshots."),
    (f"UX Design (UXD) v{VERSION}", "Menu by persona and every Renewal screen: fields, validations, actions, states."),
    (f"Reports, Notifications and Outputs (RNO) v{VERSION}", "Reports, letters and documents, exports, notices, "
     "e-mails, jobs."),
    (f"Interface Specification (ICD) v{ICD_VERSION}", "CCM, MFT, insurer files, LAMD and BDOFC/SOLD reports, CLPC "
     "billing files, e-policies."),
    ("Developer Specification (DSP) v1.0", "Screens, service operations, field checks, rule enforcement, states, "
     "jobs, audit records, permissions."),
    ("Data Dictionary (DDI) v1.0", "Tables, columns, lists of values, parameters, status models, indexes."),
    ("User Stories (USR) v1.0", "Epics and stories with acceptance criteria, ready for the backlog."),
    (f"Workbooks v{WB_VERSION}", "RTM and test cases (trace BRD, FRRN, user story, screen, test case; every acceptance "
     "criterion), Fit-Gap, CR Register and Effort Estimate, Requirements Collection, Walkthrough Users."),
    ("Programme documents", "Non-Functional and Security Requirements, Glossary, Decision and Observation Log, "
     "Document Register (Programme/Common). Non-functional requirements are referred to by NFR ID."),
]

REFERENCES = [
    "Business Requirements Document - Renewal (RN), issued 8-Oct-2026, with the Walkthrough addendum, the Workshop "
    "addendum, Addendum 1 and RMEL Phase 2 (Online Dispositioning).",
    "BRD-00 and the programme-wide non-functional requirements of the Collections, Claims, ReInsurance and "
    "Operations addenda.",
    "FRS - BDOI BROKERSYS Renewal v1.0 (BDOI), template D051, with its annexes (sanitation rules, letter matrix, "
    "templates).",
    "Concept Paper - Advance Implementation of Renewal Processing v1.0 (signed); superseded by BDOI's decision of "
    "26-Sep-2026 (single go-live).",
    f"BIBS Interface Specification: Renewal v{ICD_VERSION}.",
    "BIBS BDOI Feature List vs OOTB and Best Practice v1.0 (baseline of the colour code).",
]

# ------------------------------------------------------------------------------------------------ requirements
_REQ = yaml.safe_load((HERE / "brd06_v13_requirements.yaml").read_text(encoding="utf-8"))
_LINES = yaml.safe_load((HERE / "brd06_v13_brd_lines.yaml").read_text(encoding="utf-8"))
BRD_LINES = _LINES["lines"]
BRD_SECTIONS = _LINES["sections"]
BRRN = _LINES["brrn"]

# Items renumbered from version 1.2 (slips of BDOI's numbering), and items split.
RENUMBERED = {"FRRN.004.06 (Endorsement Synchronization)": "FRRN.004.05",
              "FRRN.004.06 (Export, Save and Print)": "FRRN.004.06",
              "FRRN.012.03 and FRRN.012.04 under FRRN.013": "FRRN.013.03 and FRRN.013.04",
              "FRRN.038.03 (Update of Existing Risk Code Entries)": "FRRN.038.04",
              "FRRN.26.01, FRRN.27.01 to FRRN.27.03, FRRN.29.03 to FRRN.29.05": "FRRN.026.01, FRRN.027.01 to "
              "FRRN.027.03, FRRN.029.03 to FRRN.029.05",
              "FRRN.011, FRRN.021, FRRN.033 (entry text)": "FRRN.011.01, FRRN.021.01, FRRN.033.01"}
_CHANGED = {"FRRN.004.05", "FRRN.004.06", "FRRN.013.03", "FRRN.013.04", "FRRN.038.04", "FRRN.026.01",
            "FRRN.027.01", "FRRN.027.02", "FRRN.027.03", "FRRN.029.03", "FRRN.029.04", "FRRN.029.05",
            "FRRN.021.01"}
_NEW = {"FRRN.009.02"}

# BRRN short titles (the full text is in the BRD).
_BRRN_TITLE = {
    "BRRN.001": "No Advice Letter", "BRRN.002": "Date range of the expiry list", "BRRN.003": "Filters of the list",
    "BRRN.004": "Scrollable list with all columns", "BRRN.005": "Expired and unbooked accounts",
    "BRRN.006": "Column filter and search", "BRRN.007": "Download, save, print (.xlsx)",
    "BRRN.008": "Print preview and settings", "BRRN.009": "Non-renewable risk codes and NFR letters",
    "BRRN.010": "Renewal Advice: select, generate, notice, send protected",
    "BRRN.011": "Accounts assigned to the user; endorsements reflected", "BRRN.012": "Select an account",
    "BRRN.013": "All renewal fields of an account", "BRRN.014": "Filter on every column",
    "BRRN.015": "Sort on every column", "BRRN.016": "Search within the list", "BRRN.017": "Find by name or reference",
    "BRRN.018": "Upload dispositioned files", "BRRN.019": "Reports by role with filters and summary",
    "BRRN.020": "Sanitation in the system", "BRRN.021": "Extraction without premature start",
    "BRRN.022": "Unique reference as matching key", "BRRN.023": "Clean, Review, Exception buckets",
    "BRRN.024": "LAMD limited to early validation", "BRRN.025": "Reminder letters at the follow-up checkpoint",
    "BRRN.026": "Contact Center access and status updates", "BRRN.027": "Full account history",
    "BRRN.028": "KYC due for visibility", "BRRN.029": "LAMD reports matched to the RMEL",
    "BRRN.030": "Extraction at configurable lead days", "BRRN.031": "Rule-based disposition, exceptions isolated",
    "BRRN.032": "Endorsements on the mother policy", "BRRN.033": "New Business flow for repricing",
    "BRRN.034": "Configurable decision matrix", "BRRN.035": "Insurer responses applied to the workflow",
    "BRRN.036": "Renewal listings by unit, window and status", "BRRN.037": "NRNS accounts and letters",
    "BRRN.038": "Routing by financial impact", "BRRN.039": "LAMD-driven CBG Motor and Fire renewals",
    "BRRN.040": "Placement and booking after acceptance", "BRRN.041": "Incentive eligibility after full payment",
    "BRRN.042": "Hold cover request and confirmation",
}


def _entries_of(refs: list[str]) -> list[str]:
    out = []
    for r in refs:
        e = ".".join(r.split(".")[:2])
        if e not in out:
            out.append(e)
    return out


def _mapping():
    rows = []
    for b in BRRN:
        rows.append((b["id"], _BRRN_TITLE[b["id"]], ", ".join(b["frrn"]), "BIBS", "Kept"))
    for s in BRD_SECTIONS:
        sid = s["id"]
        refs = [r for ln in BRD_LINES if ln["id"].startswith(sid.replace("BRD ", "BRD ") + ".")
                for r in ln["frrn"]]
        rows.append((sid, s["text"], ", ".join(sorted(_entries_of(refs))) or "-", "BIBS",
                     "Line level in the RTM workbook"))
    return rows


MAPPING = _mapping()


def _r(row):
    return (row["id"], row["text"], row["colour"], row["label"], row["rule"],
            row["ac"][0] if row["ac"] else "-")


FRUM = []
for _e in _REQ:
    FRUM.append({"id": _e["id"], "component": _e["component"], "capability": _e["capability"],
                 "brd": ", ".join(_e["brrn"]) or "BRD main sections (RTM workbook)",
                 "status": "Changed" if any(r["id"] in _CHANGED for r in _e["reqs"]) else
                 ("New" if _e["id"] >= "FRRN.045" else "Kept"),
                 "was": "Same", "reqs": [_r(r) for r in _e["reqs"]]})
ACCEPTANCE = {r["id"]: r["ac"] for _e in _REQ for r in _e["reqs"]}
STATUS_OF = {r["id"]: ("Renumbered" if r["id"] in _CHANGED else "New" if r["id"] in _NEW else "Kept")
             for _e in _REQ for r in _e["reqs"]}

BUSINESS_RULES = [
    ("BR-RN-01", "A risk code, check setting, rule or decision-matrix version, package mapping or choice, correction "
     "of a legacy Renewal Advice, list value, letter template or Renewal setting applies only after a user other than "
     "its maker approves it.", "k"),
    ("BR-RN-02", "An expiring policy has one renewal account per term; a second extraction, take-over or hand-off "
     "creates none.", "k"),
    ("BR-RN-03", "Extraction never starts the renewal processing; an authorised user initiates it.", "k"),
    ("BR-RN-04", "A user sees and acts only on the renewals of the scope of the role: own records for officers, the "
     "hierarchy for leads; LAMD and Contact Center users never see premium or commission.", "k"),
    ("BR-RN-05", "A renewal account locks on the Marketing side when its Renewal Advice is generated; only the "
     "cancellation of the Renewal Advice by an override unlocks it.", "k"),
    ("BR-RN-06", "A failed blocking check (endorsement in progress, outstanding premium above the threshold, financial "
     "impact, missing payment for direct booking) stops submission, posting and acceptance until it is overridden.",
     "k"),
    ("BR-RN-07", "Classification follows the active versioned rules; an override never makes an account with a "
     "failed check Clean; accounts with claims, endorsements, PN or payment issues are handled by a user.", "k"),
    ("BR-RN-08", "Closing letters use the effective expiry date (end of a confirmed hold cover, else the policy "
     "expiry); none is sent before it; an account never receives both a No Advice Letter and a Non-Renewal Letter.",
     "k"),
    ("BR-RN-09", "Every document sent outside BIBS is password protected; the password follows separately unless the "
     "client can derive it.", "k"),
    ("BR-RN-10", "The KYC due flag is for visibility only; it never blocks or reroutes a renewal.", "k"),
    ("BR-RN-11", "BIBS never escalates an account to the Unit Head by itself.", "k"),
    ("BR-RN-12", "Placement and booking are user actions; a paid CBG account moves to Submitted for Placement by "
     "itself.", "k"),
    ("BR-RN-13", "The incentive code is set only by the system, after booking, full payment and confirmed "
     "acceptance.", "k"),
    ("BR-RN-14", "Renewals, risk codes, list values, budgets and audit entries are never deleted; they are closed, "
     "ended or deactivated; the audit trail is insert-only.", "k"),
    ("BR-RN-15", "A renewal has at most one open transfer, insurer request, New Business path and hold cover request "
     "at a time.", "k"),
    ("BR-RN-16", "A return, override, decline, rejection and re-open needs a reason from its list and remarks of up "
     "to 200 characters.", "k"),
]

MESSAGES = [
    # screen or step, condition, message (as shown)
    ("Expiry List", "End date before the start date", "The end date must be on or after the start date"),
    ("Expiry List", "Bulk initiation of a one-by-one segment", "Segment <segment> is initiated one account at a time"),
    ("Expiry List", "Renewal already initiated", "Renewal <reference> is already initiated"),
    ("Expiry List", "Transfer to the own unit", "The receiving unit must be another unit"),
    ("Expiry List", "Assignment without officer", "Select the account officer"),
    ("Renewal", "Disposition before the account history", "Open the Account History before you give the disposition"),
    ("Renewal", "Not for Renewal without reason", "Select the reason for Not for Renewal"),
    ("Renewal", "Change after the Renewal Advice", "Renewal <reference> is locked since the Renewal Advice was "
     "generated"),
    ("Renewal", "Re-open after the limit", "Renewal <reference> can no longer be re-opened (expired on <date>)"),
    ("Renewal", "Required fields missing on submission", "Complete the mandatory fields: <fields>"),
    ("Renewal", "Acceptance with a failed blocking check", "Renewal <reference> cannot be accepted: <check> failed"),
    ("Renewal", "Second New Business path", "Renewal <reference> already has quotation <number>"),
    ("Renewal", "Revision without revised values", "Enter the revised premium, sum insured or rate"),
    ("TL Review", "Return without reason", "Select a reason for 'Return'"),
    ("TL Review", "Override without reason or remarks", "Select the reason of the override; Enter the remarks of the "
     "override"),
    ("Transfers", "Decline without remarks", "Enter the remarks of the decline"),
    ("Transfers", "Decision on a decided transfer", "The transfer of <reference> is already decided"),
    ("Insurer allocation", "Shares not 100%", "The shares of the insurers must total 100%"),
    ("Direct booking", "Account not fully paid", "The account is not fully paid. Override approval is required before "
     "you can proceed."),
    ("Hold cover", "Second open request", "Account <ARN> already has a hold cover requested or confirmed"),
    ("Hold cover", "Approval without validity", "Enter the insurer reference and the validity period"),
    ("Follow-ups", "Follow-up without outcome", "Select the outcome"),
    ("Uploads", "File in another format", "Upload failed. Invalid file format. Please upload a supported file type "
     "(.xlsx or .csv)."),
    ("Uploads", "PN matching several renewals", "Row <n>: PN <pn> matches several renewals"),
    ("Uploads", "Legacy RA without renewal", "Row <n>: no renewal candidate for legacy policy <reference>"),
    ("Risk Code Maintenance", "Duplicate risk code", "Duplicate entry detected. The risk code already exists in the "
     "list."),
    ("Billing files", "Period over 31 days", "The period of the billing file cannot exceed 31 days"),
    ("Proposal", "Signatories missing", "Required signatories must be selected before the Full Quote can be "
     "generated."),
    ("Channels", "CCM unavailable", "Unable to send the file. CCM service is currently unavailable."),
    ("Channels", "Invalid recipient", "Unable to send the file. One or more recipient email addresses are invalid."),
    ("Lists of values", "End before start", "The effective-to date is before the effective-from date"),
]

PERMISSION_HEADS = ["Function", "Mkt TL", "AO", "Proc. TL", "Proc. Off.", "CC", "LAMD", "TSU", "BA", "SA", "Aud."]
_ROLES = ["MKT_TL", "MKT_AO", "PROCESSING_TL", "PROCESSOR", "CONTACT_CENTER", "LAMD", "TSU", "BUSINESS_ADMIN",
          "SYSADMIN", "AUDITOR"]
_GRANTS = {  # permission: roles holding it (seed group profiles of the system)
    "RNW_VIEW": "AUDITOR BUSINESS_ADMIN CONTACT_CENTER LAMD MKT_AO MKT_TL PROCESSING_TL PROCESSOR",
    "RNW_EXTRACT": "MKT_TL PROCESSING_TL", "RNW_ASSIGN": "MKT_AO MKT_TL", "RNW_DISPOSE": "MKT_AO MKT_TL",
    "RNW_REVIEW": "MKT_TL", "RNW_OVERRIDE": "MKT_TL", "RNW_PROCESS": "PROCESSING_TL PROCESSOR",
    "RNW_PROCESS_ASSIGN": "PROCESSING_TL", "RNW_INSURER": "PROCESSING_TL PROCESSOR",
    "RNW_UPLOAD": "PROCESSING_TL PROCESSOR", "RNW_RA_GENERATE": "MKT_AO MKT_TL PROCESSING_TL PROCESSOR",
    "RNW_RA_SEND": "MKT_AO MKT_TL PROCESSING_TL PROCESSOR", "RNW_ACCEPT": "MKT_AO MKT_TL PROCESSING_TL PROCESSOR",
    "RNW_FOLLOWUP": "CONTACT_CENTER", "RNW_LAMD_UPLOAD": "LAMD", "RNW_VALIDATE": "LAMD",
    "RNW_TSU_QUEUE": "TSU", "RNW_KYC": "MKT_AO MKT_TL", "RNW_BILLING": "PROCESSING_TL PROCESSOR",
    "RNW_PACKAGE_REMAP": "PROCESSING_TL PROCESSOR", "RNW_SETUP": "BUSINESS_ADMIN",
    "RNW_TEMPLATE_MAINTAIN": "BUSINESS_ADMIN", "RNW_RMU_MAINTAIN": "BUSINESS_ADMIN", "RNW_BUDGET": "BUSINESS_ADMIN",
    "RNW_CHANNEL_MONITOR": "BUSINESS_ADMIN MKT_TL PROCESSING_TL PROCESSOR SYSADMIN",
    "RNW_CHANNEL_SETUP": "BUSINESS_ADMIN SYSADMIN",
    "RNW_REPORT_VIEW": "AUDITOR BUSINESS_ADMIN MKT_AO MKT_TL PROCESSING_TL PROCESSOR",
    "RNW_EXPORT": "MKT_AO MKT_TL PROCESSING_TL PROCESSOR",
    "RNW_AUDIT_VIEW": "AUDITOR BUSINESS_ADMIN MKT_TL PROCESSING_TL",
}
_FUNCTIONS = [
    ("View Renewal screens", "RNW_VIEW"), ("Generate and initiate the expiry list", "RNW_EXTRACT"),
    ("Assign and transfer (Marketing)", "RNW_ASSIGN"), ("Give dispositions", "RNW_DISPOSE"),
    ("Review, return and post", "RNW_REVIEW"), ("Override", "RNW_OVERRIDE"),
    ("Process renewals", "RNW_PROCESS"), ("Assign Processing Officers", "RNW_PROCESS_ASSIGN"),
    ("Insurer batches and responses", "RNW_INSURER"), ("Uploads of Processing", "RNW_UPLOAD"),
    ("Generate the Renewal Advice", "RNW_RA_GENERATE"), ("Send letters", "RNW_RA_SEND"),
    ("Record acceptance", "RNW_ACCEPT"), ("Record follow-ups", "RNW_FOLLOWUP"),
    ("Upload LAMD reports", "RNW_LAMD_UPLOAD"), ("TSU request queue", "RNW_TSU_QUEUE"),
    ("KYC monitoring", "RNW_KYC"), ("CLPC billing files", "RNW_BILLING"),
    ("Choose packages of migrated policies", "RNW_PACKAGE_REMAP"), ("Renewal Setup and risk codes", "RNW_SETUP"),
    ("Letter templates", "RNW_TEMPLATE_MAINTAIN"), ("RMU codes", "RNW_RMU_MAINTAIN"), ("Budget", "RNW_BUDGET"),
    ("Channel Monitor", "RNW_CHANNEL_MONITOR"), ("Channel connections", "RNW_CHANNEL_SETUP"),
    ("Reports", "RNW_REPORT_VIEW"), ("Export lists", "RNW_EXPORT"), ("Audit logs", "RNW_AUDIT_VIEW"),
]
PERMISSIONS = [(f"{name} ({perm})", *("Y" if r in _GRANTS[perm].split() else "-" for r in _ROLES))
               for name, perm in _FUNCTIONS]

PARAMETERS = [
    # setting, default, range, area, colour
    ("RNW_EXTRACTION_LEAD_DAYS: days before expiry of the daily extraction", "140", "1 to 400", "Extraction", "k"),
    ("RNW_EXTRACTION_LEAD_DAYS_BY_SEGMENT: lead days per segment", "(none)", "List", "Extraction", "k"),
    ("RNW_EXCLUDED_LINES: lines never extracted", "HMO, GLI, GPA", "List", "Extraction", "k"),
    ("RNW_BULK_INITIATION_SEGMENTS: segments initiated in bulk", "CLG", "List", "Extraction", "k"),
    ("RNW_REFERENCE_PREFIX: prefix of the renewal reference", "RNW", "-", "Numbering", "k"),
    ("RNW_DUPLICATE_CRITERIA / RNW_DUPLICATE_MIN_MATCH", "Expiring policy and PN / 1", "List / 1 to 4", "Checks",
     "k"),
    ("RNW_OUTSTANDING_THRESHOLD / RNW_FIN_IMPACT_TOLERANCE", "0.00 / 0.00", "0 or more", "Checks", "k"),
    ("RNW_TSI_ABOVE_ROUTE: TSI above the threshold", "QUOTATION", "QUOTATION, REVIEW", "Checks", "k"),
    ("RNW_ENDORSEMENT_ROUTE: endorsements that route to Review", "ANY", "ANY, IN_PROGRESS", "Checks", "k"),
    ("RNW_THIRD_BUCKET: third panel", "NON_RENEWABLE", "NON_RENEWABLE, EXCEPTION", "Classification", "k"),
    ("RNW_CLEAN_AUTO_RENEWAL / RNW_STP_SKIP_TL_REVIEW", "Yes / Yes", "Yes / No", "Decision matrix", "k"),
    ("RNW_AUTO_ASSIGN_SEGMENTS / RNW_AUTO_ASSIGN_PROCESSING", "(none) / No", "List / Yes, No", "Assignment", "k"),
    ("RNW_TRANSFER_MODE", "BOTH", "TRANSFER, REFERRAL, BOTH", "Transfer", "k"),
    ("RNW_POSTING_APPROVER", "OPTIONAL", "REQUIRED, OPTIONAL", "Posting", "k"),
    ("RNW_LOCK_POINT", "RA_GENERATION", "RA_GENERATION, PLACEMENT", "Posting", "k"),
    ("RNW_REOPEN_DAYS: days after expiry to re-open", "30", "0 to 365", "Disposition", "k"),
    ("RNW_INSURER_REPLY_DAYS", "10", "1 to 90", "Insurer", "k"),
    ("RNW_RA_NOTICE_MODE / FIRST / LAST / SECOND / MIN notice days", "AUTOMATIC / 75 / 45 / 15 / 30", "Days",
     "Renewal Advice", "k"),
    ("RNW_NRNS_REMINDER_DAYS / RNW_NRNS_WAITING_DAYS", "90 / 0", "Days", "Letters", "k"),
    ("RNW_NAL_OFFSET_DAYS / RNW_NRL_OFFSET_DAYS_CBG / _NON_CBG / RNW_NON_ACCEPTANCE_DAYS", "1 / 1 / 3 / 0", "Days",
     "Letters", "k"),
    ("RNW_NAL_REASONS: reasons answered with a No Advice Letter", "Direct to insurer, with submitted policy, booked "
     "to new invoice, unit sold, cancelled policy", "List", "Letters", "k"),
    ("RNW_PASSWORD_CONVENTION / RNW_FILE_NAMES", "CLIENT / CLIENT", "CLIENT, GENERATED / CLIENT, SYSTEM", "Documents",
     "k"),
    ("RNW_DELIVERY_CHANNEL", "CCM", "CCM, EMAIL", "Channels", "k"),
    ("RNW_SECOND_APPROVAL: approval after acceptance", "No", "Yes / No", "Approval", "k"),
    ("RNW_AUTO_PLACEMENT / RNW_BOOK_ON_PLACEMENT_APPROVAL", "No / No", "Yes / No", "Placement", "k"),
    ("RNW_PLACEMENT_SLA_PACKAGED / _NON_PACKAGE / RNW_PLACEMENT_CUTOFF_HOUR", "3 / 10 / 15", "Business days / hour",
     "Placement", "o"),
    ("RNW_HOLD_COVER_DAYS_NON_CBG / _CBG / _CBG_HOME_TRANSFER / _CBG_MOTOR_TRANSFER", "30 / 90 / 30 / 60", "1 to 365",
     "Hold cover", "k"),
    ("RNW_HOLD_COVER_CBG_DAYS_BEFORE / EXTENSION_WINDOW / EXTENSION_DAYS", "75 / 7 / 15, 30", "Days", "Hold cover",
     "k"),
    ("RNW_CLPC_BILLING_DAYS_BEFORE / RNW_CLPC_FFY_DAYS", "10 / 10th and 25th", "Days", "Billing", "k"),
    ("RNW_KYC_UPCOMING_DAYS / RNW_KYC_CLIENT_TYPES", "60 / Bank, Non-bank", "Days / list", "KYC", "k"),
    ("RNW_ESCALATION_DAYS: days to expiry of the ageing flag", "IBG 60, Leasing 60, others 30", "Days per segment",
     "Reports", "k"),
    ("RNW_CBG_MOTOR_AUTO_UPDATE / BI and PD tiers / RNW_PROMO_TO_REGULAR", "Yes / tables / Yes", "-", "CBG Motor",
     "o"),
    ("RNW_MFT_ENROLLED_INSURERS / RNW_CCM_MODE / RNW_MFT_MODE / RNW_LMS_MODE", "(none) / LIVE at BDOI", "-",
     "Interfaces", "k"),
]

LOVS = [
    ("Disposition (RNW_DISPOSITION)", "For Renewal; Not for Renewal; For Quotation; For Proposal; Lost Business"),
    ("Reason for Not for Renewal (RNW_NONRENEWAL_REASON)", "24 values, from RMU and Loan fully paid to Non-Renewable "
     "Risk Code"),
    ("Return reason (RNW_RETURN_REASON)", "Disapproved; Incomplete details; Disposition to be reviewed; For correction "
     "of the account data; Others"),
    ("Transfer reason (RNW_TRANSFER_REASON)", "Account belongs to another unit; Client request; Workload balancing; "
     "Others"),
    ("Override reason (RNW_OVERRIDE_REASON)", "Five values"),
    ("Insurer response (RNW_INSURER_RESPONSE)", "Renew As Is; Revise; Reject"),
    ("Follow-up channel and outcome", "Call, E-mail, SMS; six outcomes"),
    ("LAMD status (RNW_LAMD_STATUS)", "Paid-off; RMU"),
    ("Placement rejection reason (RNW_PLACEMENT_REJECT_REASON)", "Seven values"),
    ("Placement return reason (RNW_PLACEMENT_RETURN_REASON)", "39 values"),
    ("Classification", "Clean; Review; Exception (shown as Non-Renewable by setting)"),
    ("Acceptance method", "E-mail; Signed Renewal Advice; Payment"),
]

STAGE_ROWS = [
    ("Extracted", "Generated, not initiated.", "Unassigned Disposition; For Processing; Letter Pending (by the rules)"),
    ("Unassigned Disposition", "Initiated, waiting for an Account Officer.", "For Disposition; Transfer Pending"),
    ("Transfer Pending", "Waiting for the receiving unit.", "Unassigned Disposition (new or old unit)"),
    ("For Disposition", "With the Account Officer.", "Review in Progress"),
    ("Review in Progress", "With the Marketing Team Lead.", "For Processing; For Proposal / New Business Path; Letter "
     "Pending; Closed; For Disposition (returned)"),
    ("For Proposal / New Business Path", "Quotation or proposal in New Business.", "Renewed; Closed"),
    ("For Processing / In Processing", "With Processing.", "With Insurer"),
    ("With Insurer", "In an insurer request.", "RA Ready; For Disposition (returned)"),
    ("RA Ready / RA Generated / RA Sent", "Renewal Advice stages; locked after generation.", "Accepted; Closed"),
    ("Accepted / For Placement and Booking", "Client accepted.", "Renewed"),
    ("Letter Pending", "Closing letter to send.", "Closed"),
    ("Renewed / Closed", "Final.", "-"),
]
STATUSES = {
    "Renewal account": STAGE_ROWS,
    "Transfer": [("Requested", "With the receiving unit.", "Accepted; Declined; Cancelled"),
                 ("Accepted", "Account joins the unit.", "-"), ("Declined", "Back to the sender.", "-"),
                 ("Cancelled", "Withdrawn by the sender.", "-")],
    "Referral": [("Pending Acceptance", "With the receiving unit.", "Accepted; Rejected; Returned for Clarification"),
                 ("Accepted", "New Business account created.", "-"), ("Rejected", "Closed.", "-"),
                 ("Returned for Clarification", "Back to the requester.", "Pending Acceptance; Cancelled")],
    "Insurer batch": [("Draft", "Created, not sent.", "Sent"), ("Sent", "With the insurer.", "Partially Responded; "
                      "Closed"), ("Partially Responded", "Some answers in.", "Closed"), ("Closed", "All answered.",
                                                                                      "-")],
    "Letter": [("Generated", "PDF created.", "Queued"), ("Queued", "Handed to the channel.", "Sent; Failed"),
               ("Sent", "Delivered to the channel.", "-"), ("Failed", "Refused by the channel.", "Queued (resend); "
                                                            "Cancelled"), ("Cancelled", "Withdrawn.", "-")],
    "Channel message": [("Pending Transmission", "Queued.", "Submitted to CCM; Failed"),
                        ("Submitted to CCM", "Reference received.", "Sent; Delivered; Failed"),
                        ("Sent / Delivered", "Delivered.", "-"), ("Failed", "Refused or bounced.", "Resent; "
                                                                 "Cancelled"), ("Cancelled", "Withdrawn.", "-")],
}
BDOI_STATUS_MAP = [
    ("In Process Renewal", "For Disposition; For Processing; In Processing; With Insurer"),
    ("Submitted for Posting", "Review in Progress"),
    ("Posted", "For Processing (For Renewal); Letter Pending (Not for Renewal)"),
    ("Returned - Renewal; Returned Account", "For Disposition with the Returned flag"),
    ("For RA Generation; RA Sent", "RA Ready; RA Generated; RA Sent / Awaiting Response"),
    ("Submitted for Approval", "Accepted (setting RNW_SECOND_APPROVAL)"),
    ("Submitted for Placement; For Booking; Rejected Placement", "For Placement and Booking (placement status shown on "
     "the account)"),
    ("Booked; For E-Policy Sending", "Renewed"),
    ("For Billing Generation", "RA Sent (CBG Home, FFY) until the billing file"),
    ("Not for Renewal; Lost Business; Expired", "Closed (with its closing reason)"),
]

ASSUMPTIONS = [
    ("A-RN-01", "The Renewal BRD of 8-Oct-2026 with its addenda is the baseline."),
    ("A-RN-02", "All modules go live together in January 2028 (BDOI decision of 26-Sep-2026)."),
    ("A-RN-03", "Booking, invoices and accounting entries of a renewal are those of the shared New Business flow."),
]
DEPENDENCIES = [
    ("D-RN-01", "BDOI IT gives the CCM address, client identifier and access key (ICD IF-RN-01)."),
    ("D-RN-02", "BDOI IT gives the MFT server, folders, key and the list of enrolled insurers (ICD IF-RN-02)."),
    ("D-RN-03", "BDOI IT gives the LMS directory and the billing file recipients (ICD IF-RN-05)."),
    ("D-RN-04", "LAMD gives the monthly loan reports in the agreed layout (ICD IF-RN-06)."),
    ("D-RN-05", "Data Migration loads the policies in force and the package code map before the go-live take-over."),
    ("D-RN-06", "BDOI gives the final wording of the Renewal Advice, letters and e-mails."),
]
OPEN_QUESTIONS = [
    ("Q-RN-01", "Third panel: Non-Renewable (BDOI's FRS, setting default) or Exception (BRD BRRN.023)?",
     "Product Owner, Renewal", "30 Nov 2026"),
    ("Q-RN-02", "Transfer: referral, transfer of the account, or both (setting RNW_TRANSFER_MODE = BOTH)?",
     "Marketing Heads", "30 Nov 2026"),
    ("Q-RN-03", "Second approval after the client's acceptance (RNW_SECOND_APPROVAL, off by default)?",
     "Product Owner, Renewal; Processing", "30 Nov 2026"),
    ("Q-RN-04", "Lock point: at the Renewal Advice (BRD 2.004.9) or at placement (RNW_LOCK_POINT)?",
     "Product Owner, Renewal", "30 Nov 2026"),
    ("Q-RN-05", "Status names on the lists: BDOI's names (default) or the stages of the system (RNW_STATUS_NAMES)?",
     "Product Owner, Renewal", "30 Nov 2026"),
    ("Q-RN-06", "Effect of a BDOFC or BDOSOLD match on the renewal (flag only, or route to Review)?",
     "Product Owner, Renewal; CBG", "30 Nov 2026"),
    ("Q-RN-07", "What makes a structural change, and is the routing to New Business automatic?", "Marketing; TSU",
     "30 Nov 2026"),
    ("Q-RN-08", "Confirm the CCM and MFT go-live dates; e-mail stays the fall-back (RNW_DELIVERY_CHANNEL).",
     "BDO ITG", "30 Nov 2026"),
    ("Q-RN-09", "Confirm the password convention of protected files for individual and corporate clients.",
     "Product Owner, Renewal; Compliance", "30 Nov 2026"),
    ("Q-RN-10", "Confirm the incentive criteria for renewals (BRRN.041).", "Product Owner, Renewal", "30 Nov 2026"),
    ("Q-RN-11", "Approve or remove the change requests CR-RN-005, CR-RN-008 and CR-RN-015, which are delivered.",
     "Product Owner, Renewal", "30 Nov 2026"),
]

CRS = [
    # id, title, status, FRS, effort (analysis, backend, frontend, test, doc), note
    ("CR-RN-005", "Annual renewal budget maintenance, upload, inquiry and history", "Open", "FRRN.042",
     (1.5, 4.0, 3.0, 2.0, 0.5), "Not in the BRD; delivered; BDOI approves or asks for removal."),
    ("CR-RN-008", "CBG Motor rules of Annexes D and E: depreciation, BI / PD tiers, Auto Promo to Regular", "Open",
     "FRRN.009.02", (1.0, 3.0, 0.5, 1.5, 0.5), "Not in the BRD; delivered; BDOI approves or asks for removal."),
    ("CR-RN-015", "Placement turnaround time in business days with With Issue and resolution date", "Open",
     "FRRN.029.05", (1.0, 3.0, 1.5, 2.0, 0.5), "Not in the BRD; delivered; BDOI approves or asks for removal."),
    ("CR-RN-024", "Accessibility of the Renewal screens (WCAG 2.2 level AA)", "Open", "NFR-ACC-01",
     (2.0, 0.0, 12.0, 4.0, 2.0), "Not in the BRD; OOTB partial; not delivered."),
]
OOTB_POINTS = [
    ("G-09", "Allocation of a renewal to several insurers by percentage or amount", "FRRN.014.03"),
]
INTEGRATION_POINTS = [
    ("IS-RN-01", "CCM: Renewal Advice, letters, proposals, Insurance Advice, e-policies, placement e-mails",
     "FRRN.017.02, 022.02, 023.02, 029.04, 032.02, 033.04; ICD IF-RN-01"),
    ("IS-RN-02", "MFT with the insurers: placement files, hold cover requests, responses, e-policies",
     "FRRN.029.03, 030.01, 033.01, 036.06, 037.01; ICD IF-RN-02"),
    ("IS-RN-03", "CLPC billing files to the LMS directory and recipients", "FRRN.027.01, 027.03; ICD IF-RN-05"),
    ("IS-RN-04", "BDOFC and BDOSOLD reports", "FRRN.012.06; ICD IF-RN-06"),
    ("DM-RN-01", "Policies in force at go-live and their legacy references", "FRRN.045.01; Data Migration (BRD-13)"),
    ("DM-RN-02", "Package code map of the legacy packages", "FRRN.046.01; Data Migration (BRD-13)"),
]
BEST_PRACTICE = [
    ("BP-RN-01", "Suitability review at renewal: the officer confirms the client's needs and covers and records the "
     "advice given", "High", 6),
    ("BP-RN-02", "Remarketing check: renewals above a premium threshold or with a large increase are quoted with "
     "another insurer", "Medium", 5),
    ("BP-RN-03", "Sum-insured adequacy reminder for property and motor values not reviewed for two years", "Medium", 4),
    ("BP-RN-04", "Key-account renewal plan 120 days before expiry for the largest accounts", "Low", 5),
    ("BP-RN-05", "Remuneration disclosure on request stated on the Renewal Advice", "Low", 2),
]

CHANGES_V12 = [
    ("Annexes AA to AQ of v1.2", "Moved to the UXD, BPS, RNO, ICD, DSP, DDI, the workbooks and the programme "
     "documents; the FRS keeps requirements, rules, validations, personas, permissions, parameters and statuses."),
    ("Acceptance criteria", "Kept with each requirement: the first in the Example column, all of them in the User "
     "Stories v1.0 and the test cases of the RTM v1.1."),
    ("Business Requirements Mapping", "BRRN rows and the BRD sections here; the 1,033 BRD lines in the RTM workbook, "
     "sheet BRD Lines."),
    ("Numbering", "; ".join(f"{a} is {b}" for a, b in RENUMBERED.items()) + "."),
    ("FRRN.009.02", "The CBG Motor product rules of BDOI's Annexes D and E stated apart (orange, CR-RN-008)."),
    ("Business rules", "Stated once as BR-RN-01 to BR-RN-16."),
    ("Statuses", "Stages of the system with the mapping to BDOI's status names (section 9)."),
    ("Change requests", "Four open change requests; the former green and BRD gaps of v1.2 are delivered and closed "
     "in the CR Register v1.1."),
    ("Cover page and revision log", "Names of persons replaced by roles."),
]
