"""Content of the Operations Cashiering document set v3.4 (BRD-02) in BDOI's template: the FRS, the process and
storyboards (BPS), the UX design (UXD), the reports, notifications and outputs (RNO) and the interface specification
(ICD). Read by build_brd02_v34.py, brd02_pack.py and build_brd02_v34_workbooks.py. Business wording only.

The requirement rows and their acceptance criteria are in brd02_v34_requirements.yaml; the BRD IDs and the
non-functional lines of the BRD in brd02_v34_brd.yaml. Colour of a point: "k" black (in the BRD, BRD-00 or a programme
NFR), "g" green (not in the BRD, offered by the iNXT BrokerVerse OOTB platform), "o" orange (change request), "b" blue
(best practice, separate table). A label "int" or "dm" adds the grey [Integration scope] or [Data Migration scope]
label.
"""

from __future__ import annotations

from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
VERSION = "3.4"
WB_VERSION = "1.1"
DATE = "16 Oct 2026"
DATE_US = "10/16/2026"
AUTHOR = "iorta TechNXT Project Team (drafting support); for review by the BDOI Product Owner"
OWNER = "BDOI Product Owner, Operations"
ICD_VERSION = "1.0"
NAME = "Operations Cashiering"

DOCS = {
    "FRS": {"file": f"BIBS_FRS-BDOI_BRD-02_Operations_Cashiering_v{VERSION}.docx",
            "cover": "Functional Requirements Specification: Operations Cashiering",
            "header_left": "Functional Requirements Specification", "doc_no": "PRJ0012475-CSH"},
    "BPS": {"file": f"BIBS_BPS-BDOI_BRD-02_Operations_Cashiering_Process_and_Storyboards_v{VERSION}.docx",
            "cover": "Operations Cashiering: To-Be Process and Storyboards",
            "header_left": "To-Be Process and Storyboards", "doc_no": "PRJ0012475-CSH-BPS"},
    "UXD": {"file": f"BIBS_UXD-BDOI_BRD-02_Operations_Cashiering_UX_Design_v{VERSION}.docx",
            "cover": "Operations Cashiering: Screens and UX Design",
            "header_left": "Screens and UX Design", "doc_no": "PRJ0012475-CSH-UXD"},
    "RNO": {"file": f"BIBS_RNO-BDOI_BRD-02_Operations_Cashiering_Reports_Notifications_Outputs_v{VERSION}.docx",
            "cover": "Operations Cashiering: Reports, Notifications and Outputs",
            "header_left": "Reports, Notifications and Outputs", "doc_no": "PRJ0012475-CSH-RNO"},
    "ICD": {"file": f"BIBS_ICD-BDOI_BRD-02_Operations_Cashiering_Interface_Specification_v{ICD_VERSION}.docx",
            "cover": "Operations Cashiering: Interface Specification",
            "header_left": "Interface Specification", "doc_no": "PRJ0012475-CSH-ICD"},
}
HEADER_RIGHT = "BDO Insure Brokering System (BIBS) - Operations Cashiering"

# Revision log of the FRS. BDOI's versions are kept as BDOI's issues (author by role).
REVISIONS = [
    ("26 Aug 2026", "1.0", "BDOI's FRS for Operations Cashiering (FRS.CSH.01 to FRS.CSH.09).", "BDOI System Analyst"),
    ("09 Oct 2026", "1.1", "Automatic generation of the OR from Disbursement updated (issued by BDOI as file v3.1).",
     "BDOI System Analyst"),
    ("10 Oct 2026", "3.2", "Draft for BDOI review: full coverage of the Cashiering scope of the Operations BRD, "
     "acceptance criteria, items FRS.CSH.05.02 to FRS.CSH.10.04, review annexes.", AUTHOR),
    ("11 Oct 2026", "3.3", "Process flows, colour code of the points not in the BRD, change requests with effort.",
     AUTHOR),
    (DATE, VERSION, "Rewritten as a slim FRS that follows the system delivered: one table of requirements per item, "
     "business rules, validations, personas, permissions, parameters, lists and statuses. Numbering slips of BDOI's "
     "FRS corrected. Screens, flows, walkthroughs, documents, notices and reports move to the UX Design, the Process "
     "and Storyboards and the Reports, Notifications and Outputs v3.4; the interfaces and file layouts to the Interface "
     "Specification v1.0; acceptance criteria to the User Stories and the test cases; non-functional requirements, "
     "glossary and decisions to the programme documents. Names of persons replaced by roles.", AUTHOR),
]
REVISION_COMPANION = [
    (DATE, VERSION, "First issue as a separate document of the Operations Cashiering set (taken out of the appendices "
     "of the FRS v3.3 and aligned with the system delivered). Numbered 3.4 to match the FRS.", AUTHOR),
]

# ================================================================================================ shared facts
PERSONAS = [
    # name, what the person does, where, walkthrough account (fictitious test data)
    ("Cashier", "Receives payments at Head Office or a branch, creates AR and OR records and submits them, raises "
     "cancellations and reinstatements, uploads payment files, warehouses post-dated checks, gives dispositions, "
     "prints receipts.", "Cashiering Dashboard, Create AR/OR, Receipts, Unapplied Payments, Payment Uploads, Batch "
     "Print, Day-End List", "Carmela Cashier (Head Office); Benjie Branch Cashier (Cebu)"),
    ("Cashiering Team Leader", "Posts or returns AR, OR, cancellation and reinstatement records, approves "
     "dispositions and requests, maintains receipt series and the Cashiering settings, follows payment files.",
     "Posting, Unapplied Payments, Incoming Requests, Receipt Series, AR and OR Forms, Cashiering Setup, Search Log",
     "Cecilia Cashiering Lead"),
    ("Marketing Collection", "Tags BIR 2307 certificates of 2% CWT clients, follows the unapplied payments of the "
     "marketing unit and raises collector requests.", "BIR 2307, Unapplied Payment List",
     "Miguel Marketing Collection"),
    ("Disbursement", "Receives the refund requests of approved dispositions and approves insurer settlements that "
     "trigger the automatic OR.", "Disbursement Queue (BRD-05)", "Dante Disbursement"),
    ("Top Management Approver", "Gives the final approval of unapplied payments reclassified to income.",
     "Unapplied to Income, My Approvals", "Teresa Top Management"),
    ("Comptrollership", "Reads the Cashiering reports and journals.", "Report Centre", "Consuelo Comptroller"),
    ("Auditor", "Reads receipts, their history, the search log and the reports. Changes nothing.",
     "Receipts, Search Log, Report Centre", "Audrey Auditor"),
    ("System Administrator", "Keeps the group profiles and the lists of values.", "User Access screens (BRD-11)",
     "SIT System Administrator"),
]

MENU = [
    # screen, purpose, personas (C Cashier, T Team Leader, M Marketing Collection, X Auditor, P Top Management)
    ("Cashiering Dashboard", "Counts and amounts of the work waiting, search, main navigation", "C T M"),
    ("Create AR/OR", "AR and OR creation records", "C T"),
    ("Posting", "Creation, cancellation and reinstatement records for posting", "T"),
    ("Receive Payment", "Payment over the counter when the posting step is off", "C T"),
    ("Receipts", "Issued ARs and ORs; cancellation and reinstatement", "C T X"),
    ("Unapplied Payments", "Dispositions of unapplied payments", "C T"),
    ("Unapplied Payment List", "Inquiry of unapplied payments", "C T M"),
    ("Day-End List", "Records of the day with totals", "C T"),
    ("Search Log", "Searches made on Cashiering", "T X"),
    ("Incoming Requests", "Collector, refund validation and payment reversal requests", "C T"),
    ("Pre-booked Payments", "Payments waiting for the booking", "C T"),
    ("Payment Uploads", "Upload of payment files", "C T"),
    ("Payment Files", "Payment files and their runs", "C T"),
    ("Settlement ORs", "ORs of insurer settlements", "C T"),
    ("PDC Warehouse", "Post-dated checks", "C T"),
    ("Batch Print", "Printing and re-printing", "C T"),
    ("BIR 2307", "BIR 2307 tags and batches", "C T M"),
    ("Commission ORs", "ORs of Commission Schedules", "C T"),
    ("Unapplied to Income", "Old unapplied payments to other income", "C T P"),
    ("Legacy PR 2307 Reversal", "PR 2307 of legacy invoices", "C T"),
    ("AR and OR Forms", "Layout of the receipts", "T"),
    ("Receipt Series", "AR and OR number series", "T"),
    ("Cashiering Setup", "File layouts, minimal balance rules, settings", "T"),
    ("Check Pick-up", "Checks for pick-up by Collection (CSHID.009 removed from the BRD; Q-CSH-07)", "C T"),
]
PERSONA_CODES = {"C": "Cashier", "T": "Team Leader", "M": "Mkt Collection", "X": "Auditor", "P": "Top Mgmt"}

LEGEND_NOTE = (
    "Each requirement and rule carries a colour. Black: asked by the BRD, BRD-00 or a programme-wide non-functional "
    "requirement. The points not in the BRD form one group with three colours: green when the iNXT BrokerVerse OOTB "
    "platform offers them at no cost, orange when they need a change request, and blue for best-practice "
    "recommendations, kept in a separate table. A grey label marks a point delivered by the Integration workstream "
    "or by Data Migration (BRD-13).")

# ================================================================================================ FRS
FRS_INTRO = {
    "purpose": (
        "This document states the functional requirements of Operations Cashiering in BIBS, the BDO Insure Brokering "
        "System. It replaces version 3.3. BDOI's items FRS.CSH.01 to FRS.CSH.09 keep their numbers; the slips of "
        "numbering are corrected (section 11); items FRS.CSH.05.02 to FRS.CSH.10.04 complete the BRD. Each "
        "requirement, rule and validation is stated once with its ID."),
    "audience": (
        "The BIBS project team; the BDOI Business Unit owners of Cashiering (Head Office and branches), "
        "Comptrollership, Marketing Collection, Commission Receivables; BDO ITG; Internal Audit."),
    "background": [
        "Payments were receipted, matched and applied by hand across several systems. BIBS receives every payment, "
        "issues the AR or OR, applies it by component and keeps every step in one audit trail.",
        "A cashier saves an AR or OR record; a second user posts it, which issues the number, applies the payment and "
        "posts the journal. Excess, unmatched and returned money is worked in the Unapplied list.",
        "Payment files of the bank channels (Bills Payment, CLPC, Trade, Direct Credit, post-dated checks) are matched "
        "and applied row by row; the insurer settlements and Commission Schedules give their ORs.",
        "Paid premium flows to Remittance; refunds to Disbursement; the journals to the general ledger.",
    ],
    "in_scope": [
        "Dashboard, AR and OR records, posting, number series, forms, batch printing.",
        "Cancellation and reinstatement of ARs and ORs with their accounting.",
        "Payment files, MFT intake, matching, post-dated check warehouse, pre-booked payments.",
        "Unapplied payments, dispositions, requests of other modules.",
        "Automatic and Commission Schedule ORs, inquiry, payment history, audit trail.",
        "Cashiering reports, interfaces, batch runs, notices and alerts.",
    ],
    "out_scope": [
        "Sign-in, users, group profiles and permissions: User Access Maintenance (BRD-11).",
        "Remittance, Adjustment, Production Reconciliation, Commission Receivables: other FRS of the Operations BRD.",
        "Refund payment, deposit slips and bank reconciliation: Accounting and Disbursement (BRD-05).",
        "Check pick-up from Collection (CSHID.009) and the Cashiering BIR 2307 processing (CSHID.026, 027): removed "
        "from the BRD.",
        "Load of open accounts, unapplied payments and receipt series at go-live: Data Migration (BRD-13).",
    ],
}

DOC_SET = [
    ("FRS (this document)", "Requirements, rules, validations, personas, permissions, parameters, statuses, change "
     "control."),
    (f"Process and Storyboards (BPS) v{VERSION}", "End-to-end journey, to-be processes and five storyboards with "
     "screenshots."),
    (f"UX Design (UXD) v{VERSION}", "Menu by persona and every Cashiering screen: fields, validations, actions, "
     "states."),
    (f"Reports, Notifications and Outputs (RNO) v{VERSION}", "Reports, receipts and documents, exports, notices, "
     "alerts, jobs."),
    (f"Interface Specification (ICD) v{ICD_VERSION}", "Payment channel files, MFT, automatic matching, collector "
     "requests, module hand-offs."),
    ("Developer Specification (DSP) v1.0", "Screens, service operations, field checks, rule enforcement, states, "
     "jobs, audit records, permissions."),
    ("Data Dictionary (DDI) v1.0", "Tables, columns, lists of values, parameters, status models, indexes."),
    ("User Stories (USR) v1.0", "Epics and stories with acceptance criteria, ready for the backlog."),
    (f"Workbooks v{WB_VERSION}", "RTM and test cases (trace BRD, requirement, user story, screen, test case; every "
     "acceptance criterion), Fit-Gap, CR Register and Effort Estimate, Requirements Collection, Walkthrough Users."),
    ("Programme documents", "Non-Functional and Security Requirements, Glossary, Decision and Observation Log, "
     "Document Register (Programme/Common). Non-functional requirements are referred to by NFR ID."),
]

REFERENCES = [
    "Business Requirements Document - Operations, v1.01 of 15-Apr-2026 with the Cashiering annex of May 2026 "
    "(version of 8-Oct-2026).",
    "BRD-00 and the programme-wide non-functional requirements of the Collections, Claims, ReInsurance and Operations "
    "addenda.",
    "FRS - BDOI Core Replacement Operations Cashiering v3.1 (BDOI), template D051, with its Appendix A (sample AR and "
    "payment files).",
    f"BIBS Interface Specification: Operations Cashiering v{ICD_VERSION}.",
    "BIBS BDOI Feature List vs OOTB and Best Practice v1.0 (baseline of the colour code).",
]

# ------------------------------------------------------------------------------------------------ requirements
_REQ = yaml.safe_load((HERE / "brd02_v34_requirements.yaml").read_text(encoding="utf-8"))
_BRD = yaml.safe_load((HERE / "brd02_v34_brd.yaml").read_text(encoding="utf-8"))
BRD_ROWS = _BRD["brd"]
BRD_NFR = _BRD["nfr"]

# Numbering slips of BDOI's FRS v3.1 corrected in this version (old number: new number).
RENUMBERED = [
    ("FRS.OPS.001", "FRS.CSH.01.01.01", "Item of entry FRS.CSH.01.01 numbered in another series."),
    ("FRS.OPS.002.01 to FRS.OPS.002.05", "FRS.CSH.01.02.01 to FRS.CSH.01.02.05", "Items of entry FRS.CSH.01.02 "
     "numbered in another series."),
    ("FRS.CSH.01.03 .01 to .04, 02.05.08 .01 and .02, 02.05.09 .02 to .04, 03.01.08 .08, 04.01.09 .06, 04.01.10 .02",
     "Same numbers without the space", "Spaces inside the numbers."),
    ("FRS.CSH.02.02.09.10 to FRS.CSH.02.02.09.19", "FRS.CSH.02.02.10 to FRS.CSH.02.02.19", "The form, save, submit, "
     "edit and audit items of Create OR were numbered under Matching (02.02.09); 02.02.05 and 02.02.09.06 to .09 "
     "were never used."),
    ("FRS.CSH.02.02.10 (Taxes on official receipts, v3.2)", "FRS.CSH.02.02.20", "Freed for BDOI's item 02.02.10."),
    ("FRS.CSH.02.02.09.12.1", "FRS.CSH.02.02.12.01", "One-digit sub-item."),
    ("FRS.CSH.03.01.05 (first: selection of the receipts)", "FRS.CSH.03.01.04.03", "Number used twice; the selection "
     "belongs to the list of 03.01.04."),
    ("FRS.CSH.04.01.03 (second: list of issued receipts)", "FRS.CSH.04.01.04", "Number used twice; 04.01.04.01 and "
     ".02 had no parent."),
    ("FRS.CSH.05.01.10 \"see FRS.CSH.02.03\"", "FRS.CSH.02.04", "Cross-reference to batch printing."),
    ("FRS.CSH.09.02 (lead text of the report requirements)", "BR-CSH-21", "Common layout of the reports stated once "
     "as a business rule."),
]


def _r(row):
    return (row["id"], " ".join(row["text"].split()), row["colour"], row["label"], row["rule"],
            row["ac"][0] if row["ac"] else "-")


FRUM = []
for _e in _REQ:
    FRUM.append({"id": _e["id"], "component": _e["component"], "capability": _e["capability"],
                 "brd": ", ".join(_e["brd"]), "brd_ids": _e["brd"],
                 "status": "Renumbered" if _e["id"] in ("FRS.CSH.01.01", "FRS.CSH.01.02", "FRS.CSH.02.02",
                                                       "FRS.CSH.03.01", "FRS.CSH.04.01") else
                 ("New in v3.2" if _e["id"] >= "FRS.CSH.05.02" else "Kept"),
                 "reqs": [_r(r) for r in _e["reqs"]]})
ACCEPTANCE = {r["id"]: r["ac"] for _e in _REQ for r in _e["reqs"]}


def _mapping():
    rows = []
    for b in BRD_ROWS:
        if b.get("note"):
            rows.append((b["id"], b["text"], "-", b["note"]))
            continue
        entries = [f["id"] for f in FRUM if b["id"] in f["brd_ids"]] + b.get("frs", [])
        if not entries:
            raise SystemExit(f"BRD {b['id']} maps to no entry")
        rows.append((b["id"], b["text"], ", ".join(dict.fromkeys(entries)), "-"))
    return rows


MAPPING = _mapping()

BUSINESS_RULES = [
    ("BR-CSH-01", "The user who creates a record cannot post, approve or authorise it: AR and OR records, "
     "cancellation and reinstatement records, dispositions, requests, receipt series, AR and OR forms, legacy "
     "batches.", "k"),
    ("BR-CSH-02", "An AR or OR number is issued only when a second user posts the creation record (setting "
     "CASH_POSTING_STEP_RECEIPTS = AR,OR).", "k"),
    ("BR-CSH-03", "ORs are issued by Head Office only, from the Head Office series; ARs from the series of the "
     "receipting branch.", "k"),
    ("BR-CSH-04", "Numbers of a series are given in sequence, without duplicate or gap; a depleted series stops the "
     "posting and the record stays For Posting.", "k"),
    ("BR-CSH-05", "A payment is matched when it is accepted, except a post-dated check before maturity: a booked "
     "account is applied; an account not yet booked waits as pre-booked; an unknown or cancelled reference becomes "
     "unapplied.", "k"),
    ("BR-CSH-06", "Payments are applied check before cash, and per account in the order DST, Premium Tax (VAT), LGT, "
     "Fire Service Tax, Other Charges, Basic Premium.", "k"),
    ("BR-CSH-07", "An account with the BIR 2307 tag is applied up to its outstanding less the PR 2307, 2% of the "
     "premium (setting CASH_PR2307_BASE = PREMIUM).", "k"),
    ("BR-CSH-08", "A premium balance of 10.00 or less is reversed in the same posting unless it equals the 2% CWT, "
     "the DST or the whole premium; an excess of 10.00 or less goes to AP overages. Neither reaches the Unapplied "
     "list.", "k"),
    ("BR-CSH-09", "An excess above the minimal balance becomes an Excess payment record in the Unapplied list, with "
     "its source and the marketing unit of the account.", "k"),
    ("BR-CSH-10", "A check is accepted only when its date is at least 4 working days before the receipting date "
     "(settings CASH_CHECK_DATE_RULE = ON, CASH_CHECK_HOLDING_DAYS = 4).", "k"),
    ("BR-CSH-11", "The currency of a record is that of the accounts it pays; a USD amount is converted at the rate of "
     "the day.", "k"),
    ("BR-CSH-12", "A cancellation or reinstatement needs a reason from its list; Others needs a text; a check payment "
     "accepts only the check reasons (setting CASH_CHECK_CANCEL_REASONS).", "k"),
    ("BR-CSH-13", "A receipt has at most one cancellation or reinstatement record waiting. An account locked by "
     "Remittance or Adjustment accepts a payment and no other Cashiering action.", "k"),
    ("BR-CSH-14", "The cancellation of an AR already remitted turns the remitted premium into an AR Insurer Refund "
     "(setting CASH_REMITTED_CANCELLATION).", "k"),
    ("BR-CSH-15", "Every posting, cancellation and reinstatement posts its journal at once, with the receipt or record "
     "number as reference.", "k"),
    ("BR-CSH-16", "Refund, Reclass, Transfer to Other Marketing Unit and Others wait for the Team Leader's approval; "
     "Apply to Other Invoice, DST Payment Application and Handling Fee are processed at once. A reclass or transfer "
     "moves the whole balance.", "k"),
    ("BR-CSH-17", "Each row of a payment file is processed on its own and accepted once; a file already received is "
     "refused; a failed row is kept with its reason.", "k"),
    ("BR-CSH-18", "Every action is logged with user, time, source address, record and the values before and after; "
     "the audit trail is insert-only and no receipt or record is deleted.", "k"),
    ("BR-CSH-19", "The first print of a receipt carries no mark; every later print carries REPRINT.", "k"),
    ("BR-CSH-20", "A user sees and acts only within the permissions of the group profiles and the company and branch "
     "of the user; Marketing users see the unapplied payments of their unit (setting "
     "CASH_UNAPPLIED_MARKETING_SCOPE).", "k"),
    ("BR-CSH-21", "Each report is named <Report Name>_<date of extraction MMDDYYYY>, lists amounts with two decimals "
     "and dates as dd-MMM-yyyy, has a total line and a footer with the user, date and time.", "k"),
]

MESSAGES = [
    # screen or step, condition, message (as shown)
    ("Create AR/OR", "Required field missing", "<Field> is required"),
    ("Create AR/OR", "Paid amount out of range", "Paid Amount must be above zero and not more than 1,000,000,000.00"),
    ("Create AR/OR", "Account amounts differ from the paid amount", "The amounts of the accounts must add up to the "
     "Paid Amount"),
    ("Create AR/OR", "Currency of the account differs", "The currency of account <account> is <currency>; the receipt "
     "is in <currency>"),
    ("Create AR/OR", "Check within the holding period", "The check date must be at least 4 working days before today"),
    ("Create AR/OR", "OR of a branch", "Official receipts are issued by Head Office only"),
    ("Create AR/OR", "Non-premium AR without insurer", "A non-premium AR needs the paying insurer's code"),
    ("Create AR/OR", "Cancel a record not Created", "Only a record in status Created can be cancelled; <record> is "
     "<status>"),
    ("Create AR/OR", "Edit a record For Posting or posted", "<record> is <status> and cannot be changed"),
    ("Posting", "Poster is the creator", "The requester cannot approve <record>"),
    ("Posting", "Return without reason", "Enter the reason for the return"),
    ("Posting", "Series depleted", "Receipt Number series for this Branch is depleted. Contact your Administrator."),
    ("Posting", "Record no longer For Posting", "<record> is <status>, not For Posting"),
    ("Receive Payment", "AR issued without the posting step", "An <kind> is issued when its creation record is "
     "posted: save it with Create <kind> and submit it for posting"),
    ("Receipts", "Mixed selection", "Select AR numbers only or OR numbers only"),
    ("Receipts", "Second record waiting", "Receipt <number> already has a cancellation or reinstatement record "
     "waiting"),
    ("Cancellation", "No reason", "Select a reason"),
    ("Cancellation", "Others without text", "Specify the reason when 'Others' is selected"),
    ("Cancellation", "Reason not allowed for a check", "The reason <reason> is not allowed for a check payment"),
    ("Reinstatement", "Partial reinstatement of an OR", "Partial reinstatement is not available for an OR"),
    ("Reinstatement", "Partial without account", "Select at least one account for a partial reinstatement"),
    ("Reinstatement", "Type missing", "Select the type of reinstatement"),
    ("Reinstatement", "Unit Head missing", "Enter the Unit Head"),
    ("Reinstatement", "Remarks too long", "Remarks can have at most 100 characters"),
    ("Unapplied Payments", "Amount above the balance", "The amount must be above zero and at most the balance "
     "<balance>"),
    ("Unapplied Payments", "Partial reclass or transfer", "A reclass or transfer moves the whole balance <balance>"),
    ("Unapplied Payments", "Target invoice paid", "Invoice <invoice> has nothing outstanding to apply to"),
    ("Unapplied Payments", "Reversal of a refund not returned", "A refund is reversed only after Disbursement "
     "returned the request"),
    ("Payment Uploads", "File type not chosen", "Select the payment file type of every file"),
    ("Payment Uploads", "File not processed", "<file> was not processed: <reason>"),
    ("PDC Warehouse", "Check already warehoused", "Check <bank> <check number> is already warehoused"),
    ("PDC Warehouse", "Release with another outcome", "A check leaves the warehouse as returned, replaced or pulled "
     "out"),
    ("Batch Print", "Selection out of range", "Select between 1 and 500 receipts to print"),
    ("Receipt Series", "Last number below the first", "The last number must not be below the first number"),
    ("Receipt Series", "Format without sequence", "The number format must hold the sequence {SEQ}"),
    ("AR and OR Forms", "Approver is the maker", "The user who changed the form cannot approve it"),
    ("AR and OR Forms", "Effective date in the past", "The effective date must be today or a later date"),
    ("Incoming Requests", "Request already decided", "<request> is already <status>"),
    ("Settlement ORs", "Issue again an issued OR", "Only an OR that could not be issued is issued again"),
    ("BIR 2307", "Invoice already tagged", "Invoice <invoice> already has a 2307 tag in process"),
]

PERSONA_HEADS = ["Function", "Cashier", "Cash. TL", "Mkt Coll.", "Top Mgmt", "Compt.", "Auditor"]
_ROLES = ["CASHIER", "CASHIER_TL", "MKT_COLLECTION", "TOP_MANAGEMENT_APPROVER", "COMPTROLLERSHIP", "AUDITOR"]
_GRANTS = {  # permission: roles holding it (seed group profiles of the system)
    "CASH_RECEIPT": "CASHIER CASHIER_TL", "CASH_APPROVE": "CASHIER_TL", "CASH_CANCEL": "CASHIER CASHIER_TL",
    "CASH_REINSTATE": "CASHIER CASHIER_TL", "CASH_APPLY": "CASHIER CASHIER_TL",
    "CASH_DISPOSITION": "CASHIER CASHIER_TL", "CASH_DISPOSITION_APPROVE": "CASHIER_TL",
    "CASH_UPLOAD": "CASHIER CASHIER_TL", "CASH_PRINT": "CASHIER CASHIER_TL", "CASH_SERIES_MANAGE": "CASHIER_TL",
    "CWT_TAG": "MKT_COLLECTION", "CWT_PROCESS": "CASHIER CASHIER_TL",
    "CASH_UPP_INCOME_REQUEST": "CASHIER CASHIER_TL", "CASH_UPP_INCOME_APPROVE": "TOP_MANAGEMENT_APPROVER",
    "LEGACY_REVERSAL_REQUEST": "CASHIER", "LEGACY_REVERSAL_APPROVE": "CASHIER_TL",
    "OPS_REPORT_VIEW": "CASHIER CASHIER_TL MKT_COLLECTION COMPTROLLERSHIP AUDITOR", "AUDIT_VIEW": "AUDITOR",
}
_FUNCTIONS = [
    ("Create AR and OR records; receive payments", "CASH_RECEIPT"), ("Post and return records", "CASH_APPROVE"),
    ("Cancellation records", "CASH_CANCEL"), ("Reinstatement records", "CASH_REINSTATE"),
    ("Apply payments; pre-booked payments", "CASH_APPLY"), ("Give dispositions", "CASH_DISPOSITION"),
    ("Approve dispositions", "CASH_DISPOSITION_APPROVE"), ("Upload payment files; PDC warehouse", "CASH_UPLOAD"),
    ("Print and re-print receipts", "CASH_PRINT"), ("Receipt series; AR and OR forms; setup", "CASH_SERIES_MANAGE"),
    ("Tag BIR 2307", "CWT_TAG"), ("Process BIR 2307 batches", "CWT_PROCESS"),
    ("Request reclass to income", "CASH_UPP_INCOME_REQUEST"), ("Final approval to income", "CASH_UPP_INCOME_APPROVE"),
    ("Request legacy PR 2307 reversal", "LEGACY_REVERSAL_REQUEST"),
    ("Approve legacy PR 2307 reversal", "LEGACY_REVERSAL_APPROVE"), ("Operations reports", "OPS_REPORT_VIEW"),
    ("Audit trail and search log", "AUDIT_VIEW"),
]
PERMISSIONS = [(f"{name} ({perm})", *("Y" if r in _GRANTS[perm].split() else "-" for r in _ROLES))
               for name, perm in _FUNCTIONS]

PARAMETERS = [
    # setting, default, range, area, colour
    ("CASH_POSTING_STEP_RECEIPTS: receipt kinds issued at posting", "AR,OR", "AR, OR, blank", "Posting", "k"),
    ("CASH_RECEIPT_NO_FORMAT: format of a new series", "{PREFIX}-{YEAR}-{BRANCH}-{SEQ}", "Tokens", "Numbering",
     "k"),
    ("CASH_RECORD_NO_FORMAT: format of the record numbers", "{PREFIX}-{TYPE}-{SEQ}", "Tokens", "Numbering", "k"),
    ("CASH_CERTIFICATE_NO_FORMAT: acknowledgement certificate number", "AC_{BRANCH}_{MMYYYY}_{SEQ}", "Tokens",
     "Printing", "k"),
    ("CASH_MAX_PAID_AMOUNT: highest paid amount of a record", "1,000,000,000.00", "Amount", "Records", "k"),
    ("CASH_AR_REMARKS_REQUIRED: remarks required", "Y", "Y, N", "Records", "k"),
    ("CASH_CHECK_DATE_RULE / CASH_CHECK_HOLDING_DAYS", "ON / 4", "ON, OFF / 0 to 30", "Checks", "k"),
    ("CASH_CHECK_BEFORE_CASH: checks applied before cash", "Y", "Y, N", "Application", "k"),
    ("CASH_PR2307_BASE: base of the PR 2307", "PREMIUM", "PREMIUM, BASIC", "Application", "k"),
    ("Minimal balance rules: premium, excess, commission", "10.00 / 10.00 / 10.00 (commission inactive)", "Amount",
     "Application", "k"),
    ("CASH_CHECK_CANCEL_REASONS: reasons allowed for a check", "Four check reasons", "List", "Cancellation", "k"),
    ("CASH_REMITTED_CANCELLATION: cancellation of a remitted AR", "AR_INSURER_REFUND", "AR_INSURER_REFUND, "
     "REVERSE_ALL", "Cancellation", "k"),
    ("CASH_REINSTATEMENT_SCOPE: what Reinstate does", "BOTH", "ISSUED, CANCELLED, BOTH", "Reinstatement", "k"),
    ("CASH_DIRECT_CREDIT_MODE: Direct Credit file", "IDENTIFY", "IDENTIFY, NEW_PAYMENT", "Payment files", "k"),
    ("CASH_COMMISSION_SCHEDULE_MODE", "LINE_LEVEL", "LINE_LEVEL, WHOLE_FILE", "Payment files", "k"),
    ("CASH_MFT_COMPANY_CODE: company of the MFT files", "(first company)", "Company code", "Payment files", "k"),
    ("CASH_SETTLEMENT_OR_TRIGGER: when the settlement OR is issued", "DISBURSEMENT_APPROVAL", "DISBURSEMENT_APPROVAL, "
     "REMITTANCE_APPROVAL", "Automatic OR", "k"),
    ("CASH_UNAPPLIED_MARKETING_SCOPE", "OWN_UNIT", "OWN_UNIT, ALL", "Unapplied", "k"),
    ("CASH_BANK_ACCOUNT / CASH_ON_HAND_ACCOUNT: GL accounts", "(set by Comptrollership)", "GL account",
     "Accounting", "k"),
    ("CASH_AMOUNT_WORDS_UNITS: words of the amount", "PHP Pesos/Centavos; USD US Dollars/Cents", "List", "Printing",
     "k"),
    ("Ageing threshold of pre-booked payments (alert PREBOOKED_AGEING)", "5 days", "Days", "Alerts", "k"),
]

LOVS = [
    ("AR class (AR_CLASS)", "Premium payments (Bills Payment, OTC, Trade, CLPC, PDC, Direct Credit); Non-premium "
     "payments (Refund, Other expenses, AR Insurance)"),
    ("OR type (OR_TYPE)", "Service Fee Income; Insurance Profit Share; Commissions; Incentives; Handling Fee; Others"),
    ("Cancellation reason (RECEIPT_CANCEL_REASON)", "Premium, commission and post-dated check reasons, each with "
     "Others (specify)"),
    ("Reinstatement reason (REINSTATEMENT_REASON)", "Premium: due to cancellation, mis-application, withholding tax "
     "adjustment, others; direct payment: double reversal, wrong OR details, withholding tax adjustment, "
     "cancellation, others"),
    ("Disposition type (DISPOSITION_TYPE)", "Apply to other invoice; DST payment application; Refund; Reclass; "
     "Transfer to other marketing unit; Handling fee; Others"),
    ("Payment type", "Cash; Check; Direct Credit (OR)"),
    ("Entry type", "Client; Insurer; Other"),
    ("Payment file type", "Bills Payment; CLPC; Trade; Direct Credit; PDC; Commission Schedule"),
    ("Receipting branch", "Head Office; Cebu; Angeles; Davao; General Santos; CDO"),
]

STATUSES = {
    "AR / OR record": [("Created", "Saved by its creator.", "For Posting; Record Cancelled"),
                       ("For Posting", "With the posters.", "Posted; Returned"),
                       ("Returned", "Back with the creator with a reason.", "For Posting"),
                       ("Posted", "AR/OR Issued, Cancelled or Reinstated.", "-"),
                       ("Record Cancelled", "Withdrawn by the creator.", "-")],
    "Receipt": [("Issued", "Number given, payment applied.", "Cancelled; Reinstated"),
                ("Cancelled", "Payments reversed.", "Reinstated"),
                ("Reinstated", "Cancelled receipt restored.", "Cancelled")],
    "Unapplied payment": [("Unapplied", "No disposition.", "Monitoring; For Approval; Applied"),
                          ("Monitoring", "Disposition given.", "Applied; Unapplied"),
                          ("For Approval", "Waiting for the Team Leader.", "In Process; Unapplied"),
                          ("For Reversal", "Reversal requested.", "Unapplied"),
                          ("Applied / Closed", "Balance 0.00.", "-")],
    "Disposition": [("Monitoring", "Given, not processed.", "For Approval; Completed"),
                    ("For Approval", "Waiting for the Team Leader.", "In Process; Completed; Withdrawn"),
                    ("In Process", "With Disbursement (refund).", "Completed; For Reversal"),
                    ("Completed", "Processed.", "For Reversal"), ("For Reversal", "Reversal requested.", "Reversed"),
                    ("Reversed / Withdrawn", "Undone.", "-")],
    "Post-dated check": [("Warehoused", "In the warehouse.", "Matured; Returned; Replaced; Pulled Out"),
                         ("Matured", "Payment made at maturity.", "Applied"), ("Applied", "Applied.", "-"),
                         ("Returned / Replaced / Pulled Out", "Left before maturity.", "-")],
    "Collector request": [("Queued", "Waiting for a cashier.", "Accepted; Rejected"),
                          ("Accepted", "Disposition created.", "Applied; Rejected"),
                          ("Rejected", "Refused with a reason.", "-"), ("Applied", "Disposition processed.", "-")],
}

ASSUMPTIONS = [
    ("A-CSH-01", "The Operations BRD of 8-Oct-2026 (v1.01 with the Cashiering annex of May 2026) is the baseline."),
    ("A-CSH-02", "All modules go live together (BDOI decision of 26-Sep-2026)."),
    ("A-CSH-03", "Deposit slips, bank statements and bank reconciliation are done in Finance › Receivables "
     "(Accounting, BRD-05) on the bank accounts the Cashiering journals post to."),
]
DEPENDENCIES = [
    ("D-CSH-01", "BDO IT gives the MFT folders and the OBPCS and PMS file schedules (ICD IF-CSH-01, IF-CSH-02)."),
    ("D-CSH-02", "BDOI gives one real file of each payment channel to confirm the layouts (ICD IF-CSH-03)."),
    ("D-CSH-03", "Comptrollership gives the GL accounts of the bank accounts and the rate of the day."),
    ("D-CSH-04", "Data Migration loads the open accounts, unapplied payments and receipt series in use before "
     "go-live."),
    ("D-CSH-05", "BDOI gives the BIR-registered formats of the AR and OR numbers and of the certificate number."),
]
OPEN_QUESTIONS = [
    ("Q-CSH-01", "Minimal balance: 10.00 (BRD CSHID.016, setting) or -1.00 to 1.00 (BDOI's FRS)?",
     "Comptrollership", "30 Nov 2026"),
    ("Q-CSH-02", "Reinstatement: restore a cancelled receipt, reverse an issued one, or both (setting BOTH)?",
     "Cashiering Process Owner", "30 Nov 2026"),
    ("Q-CSH-03", "Direct Credit file: identify payments already received (setting IDENTIFY) or new payments?",
     "Cashiering Process Owner; BDO IT", "30 Nov 2026"),
    ("Q-CSH-04", "Settlement OR: at Disbursement approval (setting) or at Remittance approval?", "Comptrollership",
     "30 Nov 2026"),
    ("Q-CSH-05", "AR and OR number formats and the acknowledgement certificate format registered with the BIR.",
     "Comptrollership", "30 Nov 2026"),
    ("Q-CSH-06", "List and addresses of the applications outside BIBS to link from the dashboard (BRQID.003).",
     "BDO IT", "30 Nov 2026"),
    ("Q-CSH-07", "Check Pick-up (CSHID.009 removed): remove the screen from the Cashier profiles at go-live?",
     "Cashiering Process Owner", "30 Nov 2026"),
    ("Q-CSH-08", "AR and OR Forms: MBS holds no group profile with the form permission; who maintains and approves "
     "the forms?", "Product Owner, Operations", "30 Nov 2026"),
    ("Q-CSH-09", "Approve or remove CR-CSH-102 (e-mail copy of the notices), which is delivered; approve or reject "
     "CR-CSH-101 (time rules).", "Product Owner, Operations", "30 Nov 2026"),
    ("Q-CSH-10", "Note printed on the AR: \"Official receipt to be issued by the insurance company\" or \"Invoice to "
     "be issued by the insurance company\"?", "Comptrollership", "30 Nov 2026"),
]

CRS = [
    # id, title, status, FRS, effort (analysis, backend, frontend, test, doc), note
    ("CR-CSH-101", "Time rules of the Cashiering stages: overdue marker and reminder", "Open", "BR-CSH-02",
     (1.0, 2.5, 1.0, 1.5, 0.5), "Not in the BRD; not delivered; BDOI approves or rejects."),
    ("CR-CSH-102", "Each user chooses an e-mail copy of the Cashiering notices", "Open", "FRS.CSH.10.04.03",
     (0.5, 1.0, 1.0, 0.5, 0.5), "Not in the BRD; delivered; BDOI approves or asks for removal."),
]
OOTB_POINTS = [
    ("G-01b", "Scheduled weekly run of the Unapplied Premium Payment Transaction report", "FRS.CSH.09.01.05"),
]
INTEGRATION_POINTS = [
    ("IS-CSH-01", "Bills Payment files of OBPCS through MFT", "FRS.CSH.05.01.04; ICD IF-CSH-01"),
    ("IS-CSH-02", "Matured post-dated checks of PMS through MFT", "FRS.CSH.05.01.04; ICD IF-CSH-02"),
    ("IS-CSH-03", "MFT intake of the payment files", "FRS.CSH.05.01.02; ICD IF-CSH-01"),
    ("IS-CSH-04", "Interfaces with the systems outside BIBS", "FRS.CSH.10.01.01; ICD"),
    ("DM-CSH-01", "Receipt series in use and their next numbers at go-live", "FRS.CSH.02.03.06; Data Migration "
     "(BRD-13)"),
    ("DM-CSH-02", "Legacy unapplied payments and PR 2307 balances (Unapplied to Income, Legacy PR 2307 Reversal)",
     "FRS.CSH.06.01.02; Data Migration (BRD-13)"),
]
BEST_PRACTICE = [
    ("BP-CSH-01", "End-of-day cash count by denomination for each cashier, compared with the receipts of the day, "
     "variance approved by the Team Leader", "High", 6),
    ("BP-CSH-02", "Cash acceptance limits per cashier and per payment, with a recorded supervisor override", "Medium",
     4),
    ("BP-CSH-03", "Dormant unapplied money flagged for client contact and reported under the Unclaimed Balances Law "
     "(Act No. 3936)", "Medium", 5),
]

CHANGES_V33 = [
    ("Appendices A to W of v3.3", "Moved to the UXD, BPS, RNO, ICD, DSP, DDI, the workbooks and the programme "
     "documents; the FRS keeps requirements, rules, validations, personas, permissions, parameters and statuses."),
    ("Acceptance criteria", "Kept with each requirement: the first in the Example column, all of them in the User "
     "Stories v1.0 and the test cases of the RTM v1.1."),
    ("Numbering", "Slips of BDOI's FRS corrected; listed in section 11 and in the Decision and Observation Log "
     "(D-014)."),
    ("Minimal balance", "10.00 of the BRD (CSHID.016), as the system applies it; BDOI's -1.00 to 1.00 is Q-CSH-01."),
    ("Business rules", "Stated once as BR-CSH-01 to BR-CSH-21."),
    ("Change requests", "Two open change requests: CR-CSH-101 (not delivered) and CR-CSH-102 (delivered)."),
    ("Cover page and revision log", "Names of persons replaced by roles."),
]
