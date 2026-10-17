"""Content of the Operations Remittance document set v1.0 (BRD-02, Operations BRD, Remittance section RMTID and
MKTID) in BDOI's template: the FRS, the process and storyboards (BPS), the UX design (UXD, screens in
brd02r_v10_pack.py), the reports, notifications and outputs (RNO) and the interface specification (ICD) with the
insurer remittance file layouts. Read by build_bdoi_v10_packs.py and brd02r_v10_pack.py. Business wording only.

Source: BDOI's "FRS - BDO Insure Core Modernization - Remittance" (template D051, Release 1, received 17-Oct-2026).
BDOI's FRS maps 48 BRD lines (RMTID.001-036, 039, 040; MKTID.001-007, 009-011) and writes FRID.RMT.001 to 003; this
version keeps those numbers, corrects one numbering slip and completes FRID.RMT.004 to 016 for the other BRD lines.
The file layouts of the ICD are read from BDOI's three specimen files (field layout only; every value shown in the
ICD is invented).

Colour of a point: "k" black (in the BRD, BRD-00 or a programme document), "g" green (not in the BRD, offered by the
iNXT BrokerVerse OOTB platform), "o" orange (change request), "b" blue (best practice, separate table). A label "int"
or "dm" adds the grey [Integration scope] or [Data Migration scope] label. Fit of a requirement against the BIBS
platform: Fit, Partial, Gap, Change request, Integration, Data Migration, In another FRS.
"""

from __future__ import annotations

VERSION = "1.0"
DROP = "Drop 1"
WB_VERSION = "1.0"
ICD_VERSION = "1.0"
# Documents issued again after the release of 17 Oct 2026 changed them; the others stay at VERSION.
DOC_VERSIONS = {"UXD": "1.1", "RNO": "1.1", "DSP": "1.1", "DDI": "1.1"}
SHOTS_DIR = "brd02r_v11_screens"
DATE = "17 Oct 2026"
DATE_LONG = "17 October 2026"
DATE_US = "10/17/2026"
CREATED_US: dict[str, str] = {}
AUTHOR = "iorta TechNXT Project Team (drafting support); for review by the BDOI Product Owner"
OWNER = "BDOI Product Owner, Operations"
ANSWER_BY = "30 Nov 2026"
BRD = "BRD-02"
NAME = "Operations Remittance"
SHORT = "Remittance"
CODE = "RMT"
DOC_NO = "PRJ0012475-RMT"
HEADER_RIGHT = "BDO Insure Brokering System (BIBS) - Operations Remittance"

DOCS = {
    "FRS": {"file": f"BIBS_FRS-BDOI_BRD-02_Operations_Remittance_v{VERSION}.docx",
            "cover": "Functional Requirements Specification: Operations Remittance",
            "header_left": "Functional Requirements Specification", "doc_no": DOC_NO},
    "BPS": {"file": f"BIBS_BPS-BDOI_BRD-02_Operations_Remittance_Process_and_Storyboards_v{VERSION}.docx",
            "cover": "Operations Remittance: To-Be Process and Storyboards",
            "header_left": "To-Be Process and Storyboards", "doc_no": f"{DOC_NO}-BPS"},
    "UXD": {"file": f"BIBS_UXD-BDOI_BRD-02_Operations_Remittance_UX_Design_v{DOC_VERSIONS['UXD']}.docx",
            "cover": "Operations Remittance: Screens and UX Design",
            "header_left": "Screens and UX Design", "doc_no": f"{DOC_NO}-UXD"},
    "RNO": {"file": f"BIBS_RNO-BDOI_BRD-02_Operations_Remittance_Reports_Notifications_Outputs_v{DOC_VERSIONS['RNO']}.docx",
            "cover": "Operations Remittance: Reports, Notifications and Outputs",
            "header_left": "Reports, Notifications and Outputs", "doc_no": f"{DOC_NO}-RNO"},
    "ICD": {"file": f"BIBS_ICD-BDOI_BRD-02_Operations_Remittance_Interface_Specification_v{ICD_VERSION}.docx",
            "cover": "Operations Remittance: Interface Specification",
            "header_left": "Interface Specification", "doc_no": f"{DOC_NO}-ICD"},
}

REVISIONS = [
    ("17 Oct 2026", "0.1", "BDOI's FRS for Remittance, Release 1: business requirements mapping of 48 BRD lines and "
     "items FRID.RMT.001 to FRID.RMT.003, with three specimen extract files.", "BDOI System Analyst"),
    (DATE, VERSION, "First issue in the standard document set: BDOI's items FRID.RMT.001 to 003 kept with their "
     "numbers (one numbering slip corrected); items FRID.RMT.004 to 016 written for the other BRD lines; acceptance "
     "criteria, business rules, validations, personas, permissions, settings, statuses, open questions and the colour "
     "code. The specimen files are specified in the Interface Specification v1.0. Names of persons replaced by roles.",
     AUTHOR),
]
REVISION_COMPANION = [(DATE, VERSION, "First issue, with the FRS Operations Remittance v1.0.", AUTHOR)]
REVISIONS_UXD = REVISION_COMPANION + [(DATE, "1.1", "Second issue with the release of 17 Oct 2026: SCR-RMT-02, 04, 05 and 12 updated for the delivered extract files, settlement numbering, shared folder copy and Mall Assurance columns; SCR-RMT-13 Remittance Settings now available; screenshots captured from the running application on seed data.", AUTHOR)]
REVISIONS_RNO = REVISION_COMPANION + [(DATE, "1.1", "Second issue with the release of 17 Oct 2026: extract file names (Excel and text), settlement reference, shared folder copy and its alert REMIT_SHARED_FOLDER_FAILED, the scheduled job REMITTANCE_SCHEDULE and the Mall Assurance columns.", AUTHOR)]
ICD_REVISIONS = [(DATE, ICD_VERSION, "First issue: remittance extract files (Excel and text) read from BDOI's three "
                  "specimen files, insurer OR return, shared folder, e-mail to the insurer and module hand-offs.",
                  AUTHOR)]

# ================================================================================================ shared facts
PERSONAS = [
    # name, what the person does, where, walkthrough account (fictitious)
    ("Remittance Processor", "Runs manual extractions, reviews the batches assigned to him, excludes and restores "
     "accounts, submits, holds or returns batches, sends the schedule to the insurer and uploads the insurer ORs.",
     "Remittance Workbench, Extraction, Remittance Batches, Insurer OR Upload, DTIP Status",
     "Rosa Remittance Processor"),
    ("Remittance Team Leader", "Approves or returns submitted batches, reassigns batches, approves special remittance "
     "requests, keeps the early remittance incentive rules and follows the extraction runs.",
     "Remittance Batches, Special Remittance, Incentive Rules, Report Centre", "Ramon Remittance Lead"),
    ("Marketing Collection", "Requests holds and special remittances for the accounts of the marketing unit and "
     "asks for a remittance schedule to be sent to the insurer.", "Remittance Holds, Special Remittance",
     "Miguel Marketing Collection"),
    ("Marketing Team Leader", "Approves hold requests, extensions and cancellations.", "Remittance Holds",
     "Marta Marketing Lead"),
    ("Disbursement", "Receives the payment request of each approved batch and pays the insurer; the payment status "
     "comes back to Remittance.", "Disbursement Queue (BRD-05)", "Dante Disbursement"),
    ("Comptrollership", "Reads the remittance reports and the DTIP status.", "Report Centre, DTIP Status",
     "Consuelo Comptroller"),
    ("Auditor", "Reads batches, holds, special remittances, their history and the reports. Changes nothing.",
     "Remittance Batches, Report Centre", "Audrey Auditor"),
    ("System Administrator", "Keeps the group profiles, the lists of values and the extraction schedule.",
     "User Access screens (BRD-11), Job Monitor", "SIT System Administrator"),
]

MENU = [
    # screen, purpose, personas (P processor, T team leader, M marketing collection, L marketing team leader,
    # C comptrollership, X auditor)
    ("Remittance Workbench", "Work waiting by stage, counts and amounts, links to each list", "P T M L"),
    ("Extraction", "Runs, manual extraction per insurer and type or per invoice, end-of-day queue", "P T"),
    ("Remittance Batches", "Batches by stage; review, exclusion, submission, approval, documents", "P T X"),
    ("Insurer OR Upload", "Upload of the insurer ORs and the exception report", "P T"),
    ("Remittance Holds", "Hold requests, approvals, extensions, cancellations, releases", "P T M L"),
    ("Special Remittance", "Special remittance requests and their approval", "P T M"),
    ("DTIP Status", "Premium due to insurers per invoice and its remittance status", "P T C"),
    ("Incentive Rules", "Early remittance incentive rules", "T"),
    ("Remittance Settings", "Extraction schedule, file names and output folders (proposed)", "T"),
    ("Report Centre", "The remittance reports", "P T C X"),
]
PERSONA_CODES = {"P": "Processor", "T": "Team Leader", "M": "Mkt Collection", "L": "Mkt Team Leader",
                 "C": "Comptrollership", "X": "Auditor"}
MENU_NOTE = ("The screens are under Operations › Remittance; the reports under Reports. A persona sees only its "
             "entries.")

LEGEND_NOTE = (
    "Each requirement and rule carries a colour. Black: asked by the BRD, BRD-00 or a programme document such as the "
    "BDOI Report List. The points not in the BRD form one group with three colours: green when the iNXT BrokerVerse "
    "OOTB platform offers them at no cost, orange when they need a change request, and blue for best-practice "
    "recommendations, kept in a separate table. A grey label marks a point delivered by the Integration workstream "
    "or by Data Migration (BRD-13).")

# ================================================================================================ FRS
FRS_INTRO = {
    "purpose": (
        "This document states the functional requirements of Operations Remittance in BIBS, the BDO Insure Brokering "
        "System. It completes BDOI's FRS for Remittance (Release 1): BDOI's items FRID.RMT.001 to FRID.RMT.003 keep "
        "their numbers and items FRID.RMT.004 to FRID.RMT.016 state the other Remittance lines of the Operations "
        "BRD. Each requirement, rule and validation is stated once with its ID."),
    "audience": (
        "The BIBS project team; the BDOI Business Unit owners of Remittance and Marketing Collection; Comptrollership; "
        "Disbursement; BDO ITG; Internal Audit."),
    "background": [
        "BDOI collects premium from clients on behalf of the insurers and remits it to them net of its commission. "
        "Today the remittance extract is produced by the core system as an Excel report and a text file per insurer "
        "and remittance type, reviewed by hand and sent to the insurer, who returns its official receipts (OR).",
        "BIBS extracts the applied, posted and cleared premium of each invoice per insurer and remittance type, groups "
        "it in batches, lets the processor review and exclude accounts without changing any amount, and sends the "
        "approved batch to Disbursement for payment.",
        "The remittance schedule goes to the insurer, who returns it with the OR date and number per client; the "
        "upload updates the invoices and produces the exception report.",
        "Marketing asks for holds and special remittances; the early remittance incentive is computed on the With "
        "Incentives batches; the Remittance team follows every account to its OR.",
    ],
    "in_scope": [
        "Scheduled and manual extraction per insurer and remittance type; eligibility rules.",
        "Extract files in Excel and text, file names, output folders, run notices.",
        "Batches, review with exclusion only, submission, hold, return, approval, payment request.",
        "Remittance schedule and payment request; sending to the insurer.",
        "Insurer OR upload and exception report.",
        "Holds, special remittances, early remittance incentive.",
        "Search, inquiry, tracking, notices and the remittance reports.",
    ],
    "out_scope": [
        "Sign-in, users, group profiles and permissions: User Access Maintenance (BRD-11).",
        "Receipt and application of payments: Operations Cashiering FRS (BRD-02).",
        "Payment of the insurer, payment voucher and bank files: Accounting and Disbursement (BRD-05).",
        "BIR 2307 tagging by Marketing (MKTID.010): Collections (BRD-04) and Operations Cashiering.",
        "Direct payment tagging at quotation and policy (MKTID.011): New Business (BRD-01) and Collections (BRD-04).",
        "Remittance of reinsurance transactions: Phase 2 with the reinsurance module.",
        "Legacy invoices and open batches at go-live: Data Migration (BRD-13).",
    ],
}

DOC_SET = [
    ("FRS (this document)", "Requirements, rules, validations, personas, permissions, settings, statuses, change "
     "control, review of BDOI's FRS."),
    (f"Process and Storyboards (BPS) v{VERSION}", "End-to-end journey, to-be processes and storyboards."),
    (f"UX Design (UXD) v{VERSION}", "Menu by persona and every Remittance screen: fields, validations, actions, "
     "states."),
    (f"Reports, Notifications and Outputs (RNO) v{VERSION}", "Reports, extract files, schedules, exports, notices, "
     "alerts, jobs."),
    (f"Interface Specification (ICD) v{ICD_VERSION}", "Remittance extract files in Excel and text, insurer OR return, "
     "shared folder, e-mail to the insurer, module hand-offs."),
    ("Developer Specification (DSP) v1.0", "Screens, service operations, field checks, rule enforcement, states, "
     "jobs, audit records, permissions."),
    ("Data Dictionary (DDI) v1.0", "Tables, columns, lists of values, parameters, status models, indexes and the "
     "fields of the extract files."),
    ("User Stories (USR) v1.0", "Epics and stories with acceptance criteria, ready for the backlog."),
    (f"Workbooks v{WB_VERSION}", "RTM and test cases, Fit-Gap, CR Register and Effort Estimate, Requirements "
     "Collection, Walkthrough Users."),
    ("Programme documents", "Non-Functional and Security Requirements, Glossary, Decision and Observation Log, "
     "Document Register (Programme/Common)."),
]

REFERENCES = [
    "Business Requirements Document - Operations, v1.01 of 15-Apr-2026 (Remittance section, p.47-76; Annex III "
    "Remittance reports), with Addendum 1 (RMTID.002) (version of 8-Oct-2026).",
    "FRS - BDO Insure Core Modernization - Remittance, Release 1 (BDOI), template D051, with three specimen files: a "
    "Normal - Peso Excel extract, a Normal - Dollar Excel extract and a text extract.",
    "BDOI - Core Modernization Report List 05132026 (remittance file naming convention).",
    "BDOI drop plan of 26-Sep-2026: Remittance in Drop 1, downstream.",
    "BIBS FRS Operations Cashiering v3.4 (BRD-02) and the FRS of Collections (BRD-04) and Accounting and Disbursement "
    "(BRD-05).",
    f"BIBS Interface Specification: Operations Remittance v{ICD_VERSION}.",
    "BIBS BDOI Feature List vs OOTB and Best Practice v1.0 (baseline of the colour code).",
]


# ------------------------------------------------------------------------------------------------ requirements
def R(rid, text, colour, label, rule, fit, today, acs):
    """One requirement row: ID, text, colour, grey label, rule or setting, fit, what BIBS offers, criteria."""
    return {"id": rid, "text": " ".join(text.split()), "colour": colour, "label": label, "rule": rule, "fit": fit,
            "today": today, "ac": acs}


def E(eid, component, capability, brd, status, story, reqs):
    """One FRS entry (BDOI item) with its requirement rows and its user story."""
    return {"id": eid, "component": component, "capability": capability, "brd_ids": brd, "brd": ", ".join(brd),
            "status": status, "story": story, "reqs": reqs}


BDOI = "BDOI FRS"
NEW = "Completed v1.0"

FRUM = [
    E("FRID.RMT.001", "Extraction", "Extraction per insurer and per remittance type", ["RMTID.001"], BDOI,
      ("P", "to extract the premium due per insurer and remittance type in the current file formats",
       "the insurer receives the files it knows", "SCR-RMT-02", "Highest", 13,
       [("insurer SICI and type Normal - Peso", "the extraction runs", "a batch with its Excel and text files is "
         "saved in the Normal - Peso folder"),
        ("an account with a pending cancellation", "the extraction runs", "it is not extracted")]), [
        R("FRID.RMT.001.01", "Only users whose group profile holds the extraction permission open Extraction and the "
          "remittance reports; access follows the user matrix of User Access Maintenance (BRD-11).", "k", "",
          "BR-RMT-12", "Fit", "Extraction and the reports open with the extraction and report permissions of the "
          "group profiles.", ["A Remittance Processor opens Extraction; a Marketing Collection user does not see it "
                              "in the menu and a direct attempt is refused."]),
        R("FRID.RMT.001.02", "The extraction options are the insurer (one or all) and the remittance type: With "
          "Incentives, Normal - Peso and Normal - Dollar.", "k", "", "-", "Fit",
          "Insurer and remittance type are chosen on Extraction; the scheduled run covers every insurer and type.",
          ["An extraction for one insurer and Normal - Dollar gives only the USD accounts of that insurer."]),
        R("FRID.RMT.001.03", "Each batch gives two output files: an Excel file and a text file, in the formats of the "
          "current extract (Interface Specification, IF-RMT-01 and IF-RMT-02).", "k", "", "ICD IF-RMT-01, 02",
          "Partial", "One Excel schedule per batch in the BIBS layout. The text file and the column layout of the "
          "current extract are delivery work.",
          ["A batch gives one Excel file and one text file; both list the same accounts and the same totals."]),
        R("FRID.RMT.001.04", "File names follow YYYYMMDD + insurer code + settlement number + _NormalPeso, "
          "_NormalDollar or _WithIncentives, with the extension of the file, for example "
          "20240731SICI00003992_NormalPeso.xls and 20240731SICI00003992_NormalPeso.txt. The insurer code and "
          "settlement number form the batch number, which the insurer's OR also carries.", "k", "", "BR-RMT-05",
          "Partial", "The file name is set by the setting REMIT_FILE_PATTERN from the batch number, insurer, type and "
          "date. The date without separators, the type words and the settlement number of BR-RMT-05 are delivery "
          "work.", ["A Normal - Peso batch of insurer SICI with settlement number 00003992 extracted on 31-Jul-2024 "
                    "is saved as 20240731SICI00003992_NormalPeso.xls and .txt."]),
        R("FRID.RMT.001.05", "The extract files are saved in the remittance shared folder of BDOI, in one folder per "
          "remittance type (Normal - Peso, Normal - Dollar, With Incentives).", "k", "int", "-", "Partial",
          "The files are kept per type in the BIBS file store and downloadable from the batch; the copy to BDOI's "
          "shared drive is an integration item.",
          ["The files of a Normal - Dollar batch are in the Normal - Dollar folder of the shared drive."]),
        R("FRID.RMT.001.06", "After each run the Remittance team is told the result: batches created, accounts "
          "extracted, accounts not extracted with their reason. A failed run raises an alert.", "k", "", "-", "Fit",
          "Notice of the run with its counts; alert on a failed run.",
          ["After the evening run the processors see a notice with the batches and the counts of the run."]),
        R("FRID.RMT.001.07", "An account with a pending financial endorsement (cancellation, decrease of sum insured "
          "or of commission) is not extracted and is reported with the reason.", "k", "", "BR-RMT-02", "Fit",
          "The pending adjustment flag of the invoice excludes it with the reason Pending negative adjustment.",
          ["An invoice with a pending cancellation request is tagged Unextracted - due with that reason."]),
        R("FRID.RMT.001.08", "The Excel extract keeps the content of the current report: heading (company, title "
          "with the batch number, date extracted, run date, time, run by and page), one row per account with the 35 "
          "columns of IF-RMT-01, a total row, the number of accounts, Total Remittance, the Mall Assurance "
          "deductions, VAT and WTAX, the Grand Total After Mall Assurance Charges, and the Printed By and Checked By "
          "lines.", "k", "", "ICD IF-RMT-01; BR-RMT-09", "Partial",
          "The BIBS schedule has the account, policy, dates and amount columns; the Mall Assurance columns, the "
          "heading and closing lines of the current report are delivery work (layouts OQ42).",
          ["The Excel extract of a batch of 2 accounts shows 2 rows, a total row, Number of Accounts 2 and the Grand "
           "Total equal to the sum of Net Due less the Mall Assurance charges."]),
        R("FRID.RMT.001.09", "The text extract is a pipe-delimited file with one header record holding the control "
          "totals and one detail record per account (IF-RMT-02); the totals of the header equal the sums of the "
          "details.", "k", "", "ICD IF-RMT-02; BR-RMT-10", "Gap",
          "No text extract today; delivery work with the layout of IF-RMT-02.",
          ["The text file of a batch of 3 accounts has 1 H record and 3 D records; the record count of the header "
           "is 3 and its Paid AR equals the sum of the 3 details."]),
    ]),
    E("FRID.RMT.002", "Extraction", "Review of the extracted file", ["RMTID.002"], BDOI,
      ("P", "to review the extracted accounts and exclude some without changing any amount",
       "only the right accounts are remitted", "SCR-RMT-04", "Highest", 8,
       [("a batch in Review in Process", "I exclude an invoice with a reason", "it leaves the totals and the "
         "exclusion is in the history"), ("an excluded invoice", "I restore it before submission",
                                         "it is back in the totals")]), [
        R("FRID.RMT.002.01", "The extracted file is shown on screen as the lines of the batch.", "k", "", "-", "Fit",
          "Lines tab of the batch.", ["Opening a batch shows its accounts with their amounts."]),
        R("FRID.RMT.002.02", "The screen view follows the columns of the extract report.", "k", "", "ICD IF-RMT-01",
          "Partial", "The lines show the main columns; the full column set of IF-RMT-01 is delivery work with "
          "FRID.RMT.001.08.", ["The Lines tab shows the columns of the Excel extract in the same order."]),
        R("FRID.RMT.002.03", "Authorised users mark records to exclude from processing with a reason; financial data "
          "cannot be edited.", "k", "", "BR-RMT-06", "Fit",
          "Exclude with a reason from the list; amounts are read-only.",
          ["A processor excludes an invoice with the reason Others and a comment; no amount field can be changed."]),
        R("FRID.RMT.002.04", "Every exclusion is recorded with user, time and details, and can be reversed before "
          "submission.", "k", "", "BR-RMT-11", "Fit", "Exclusion and restore are in the batch history.",
          ["The history of the batch shows the exclusion with the user, time, reason and comment."]),
        R("FRID.RMT.002.05", "A preview of the resulting batch (accounts and totals after exclusions) is shown before "
          "submission.", "k", "", "-", "Fit", "Preview before submission.",
          ["The preview of a batch of 5 with 1 exclusion shows 4 accounts and their totals."]),
        R("FRID.RMT.002.06", "Excluded records can be restored before final submission.", "k", "", "BR-RMT-06", "Fit",
          "Restore on an excluded line while the batch is under review.",
          ["After submission the Restore action is no longer offered."]),
        R("FRID.RMT.002.07", "Only the records not excluded are sent to Disbursement, and the full trail of the "
          "exclusions is kept.", "k", "", "BR-RMT-06", "Fit",
          "The payment request carries the included lines only.",
          ["The payment request of the approved batch carries the amount of the 4 included accounts."]),
    ]),
    E("FRID.RMT.003", "Extraction", "Automatic extraction on schedule", ["RMTID.003", "RMTID.001"], BDOI,
      ("T", "the extraction to run on its schedule and tag every account", "nothing due is forgotten",
       "SCR-RMT-02", "High", 5,
       [("the schedule set to 20:00 on working days", "the time comes", "the run starts and its result is notified"),
        ("an account with a check of yesterday", "the run ends", "it is tagged Unextracted - due with the reason")]), [
        R("FRID.RMT.003.01", "An authorised administrator sets the schedule of the extraction (days and time) and "
          "can start a run at once.", "k", "", "BR-RMT-13", "Partial",
          "The run time is a system setting kept by BDO IT and a run can be started from the Job Monitor; a "
          "business screen to change the schedule (Remittance Settings) is delivery work.",
          ["The Remittance Team Leader changes the run time to 19:00; the next run starts at 19:00."]),
        R("FRID.RMT.003.02", "Each account checked by a run is tagged Extracted, Unextracted - not yet due, "
          "Unextracted - due (with the reason) or Returned.", "k", "", "-", "Fit",
          "Extraction tags per run with their reason.",
          ["After a run the tags of the run list each checked invoice with its tag and reason."]),
        R("FRID.RMT.003.03", "The scheduled run takes place off-peak; a failure on one account does not stop the run, "
          "and the failed account is listed for review.", "k", "", "-", "Fit",
          "Evening run; each account is processed on its own.",
          ["A run with one invoice in error ends with the others extracted and the error listed."]),
    ]),
    E("FRID.RMT.004", "Extraction", "Manual extraction and end-of-day requests", ["RMTID.004", "RMTID.005"], NEW,
      ("P", "to extract one invoice by hand or queue it for the end-of-day run", "urgent accounts are remitted",
       "SCR-RMT-02", "High", 3,
       [("a posted invoice of insurer PISC", "I extract it by invoice number", "a batch of one account is created"),
        ("an invoice not yet posted", "I extract it", "it is refused with the reason")]), [
        R("FRID.RMT.004.01", "A processor extracts a specific invoice number of an insurer, with the same checks as "
          "the scheduled run; an invoice not posted or not eligible is refused with the reason.", "k", "",
          "BR-RMT-02", "Fit", "Manual extraction by invoice.",
          ["Extracting an invoice whose check is 1 banking day old is refused: the check is within the holding "
           "period."]),
        R("FRID.RMT.004.02", "An invoice looked up for remittance during the day can be queued for the end-of-day "
          "run; it is queued once whatever the number of searches.", "k", "", "-", "Fit",
          "End-of-day queue on Extraction (meaning to confirm, Q-RM-11).",
          ["Queuing the same invoice twice keeps one request; the evening run extracts it."]),
    ]),
    E("FRID.RMT.005", "Validation and filtering", "Eligibility of an account",
      ["RMTID.006", "RMTID.014", "RMTID.015", "RMTID.017", "RMTID.018", "RMTID.020", "RMTID.021", "RMTID.022",
       "RMTID.031"], NEW,
      ("P", "only applied, posted and cleared premium to be extracted", "BDOI never remits money it has not received",
       "SCR-RMT-02", "Highest", 8,
       [("a cash payment applied and posted today", "the run starts", "the invoice is extracted"),
        ("an invoice on an active hold", "the run starts", "it is tagged Unextracted - due: Account on hold")]), [
        R("FRID.RMT.005.01", "Only applied and posted payments are extracted, cash or check, from every channel "
          "(credit to account, automatic debit arrangement, over the counter, bills payment).", "k", "",
          "BR-RMT-02", "Fit", "Paid AR from the applied and posted payments of the invoice ledger.",
          ["A payment received but not applied is not extracted; the reason is Payment not yet applied and posted."]),
        R("FRID.RMT.005.02", "The paid AR of an invoice may not exceed its outstanding DTIP. An invoice above it is "
          "excluded, or remitted up to the DTIP, as the setting says.", "k", "", "REMIT_PAIDAR_OVER_DTIP_MODE",
          "Fit", "Setting EXCLUDE (default) or CAP (Q-RM-12).",
          ["With EXCLUDE, an invoice with paid AR 12,000.00 and DTIP 10,000.00 is not extracted and is listed."]),
        R("FRID.RMT.005.03", "A list of the invoices whose paid AR is above the DTIP balance is available after each "
          "run.", "k", "", "-", "Fit", "Report Paid AR over DTIP.",
          ["The list shows the invoice, insurer, paid AR and DTIP of each invoice excluded for that reason."]),
        R("FRID.RMT.005.04", "A check payment is extracted only when the check is at least 3 banking days old and "
          "cleared (holding period).", "k", "", "BR-RMT-04", "Fit",
          "Holding period in banking days on the holiday calendar, setting REMIT_CHECK_HOLD_DAYS = 3 (clearing "
          "evidence Q-RM-13).", ["A check deposited on Friday is extracted on Wednesday when cleared, not before."]),
        R("FRID.RMT.005.05", "Accounts on hold or with a pending negative adjustment request are excluded.", "k", "",
          "BR-RMT-08", "Fit", "Hold and pending adjustment flags of the invoice.",
          ["An invoice with an active hold is tagged Unextracted - due with the reason Account on hold."]),
        R("FRID.RMT.005.06", "An account whose holding date has passed is processed by the next run; the hold "
          "information comes from Marketing.", "k", "", "BR-RMT-08", "Fit", "Daily hold expiry run.",
          ["A hold ending yesterday is released by the morning run and the invoice is extracted that evening."]),
        R("FRID.RMT.005.07", "Written-off accounts are excluded from every output but stay visible, marked, in the "
          "inquiry and audit views.", "k", "", "-", "Fit", "Written-off flag of the invoice.",
          ["A written-off invoice is not extracted and shows Written off on the invoice view."]),
    ]),
    E("FRID.RMT.006", "Process Remittance", "Batches, submission and approval",
      ["RMTID.007", "RMTID.008", "RMTID.009", "RMTID.010", "RMTID.019", "RMTID.029"], NEW,
      ("P", "to work the batches assigned to me and submit them for approval", "the insurers are paid on time",
       "SCR-RMT-03", "Highest", 13,
       [("a batch in Review in Process", "I submit it", "it moves to For Approval and the Team Leader is told"),
        ("a batch I submitted", "I try to approve it", "it is refused")]), [
        R("FRID.RMT.006.01", "Extracts are grouped by remittance type, processor, insurer and batch number.", "k", "",
          "-", "Fit", "One batch per insurer, type and currency; processor from the assignment.",
          ["A run for two insurers and two types gives four batches, each with its processor."]),
        R("FRID.RMT.006.02", "Each batch has a unique number made of the insurer code and an 8-digit settlement "
          "number (for example SICI00003992), never reused.", "k", "", "BR-RMT-05", "Partial",
          "Unique batch number RMB-<insurer>-<year>-<sequence>; the settlement number format of BR-RMT-05 and the "
          "continuation of the legacy numbers are delivery work (Q-RM-04).",
          ["Two batches of insurer SICI on the same day get 00003992 and 00003993."]),
        R("FRID.RMT.006.03", "Extracted accounts move to Process Remittance with status Review in Process; a Team "
          "Leader can reassign a batch to another processor.", "k", "", "-", "Fit", "Batch stages and reassignment.",
          ["A reassigned batch appears in the new processor's list and leaves the old one."]),
        R("FRID.RMT.006.04", "A batch is submitted for approval; the system checks the remittance status of each "
          "account (Approved, Review in Process, Requested for Hold, Unapplied Payment, Outstanding Balance, "
          "Unprocessed, Fully Remitted, Partially Remitted) and refuses a submission with an invalid one.", "k", "",
          "-", "Fit", "Submission check of the batch.",
          ["Submitting a batch with a line that became Requested for Hold is refused with the invoice named."]),
        R("FRID.RMT.006.05", "Invoices or whole batches can be submitted, put on hold or returned; a returned line "
          "can be extracted again.", "k", "", "BR-RMT-07", "Fit", "Hold, return and submit actions.",
          ["A returned batch frees its invoices for the next run with the tag Returned."]),
        R("FRID.RMT.006.06", "A Team Leader other than the submitter approves or returns the batch; approval posts the "
          "remittance and sends the payment request to Disbursement.", "k", "", "BR-RMT-01", "Fit",
          "Approval by another user, posting and payment request.",
          ["The approved batch appears in the Disbursement queue with its amount due."]),
    ]),
    E("FRID.RMT.007", "Sending and uploading of file", "Remittance schedule, payment request and sending",
      ["RMTID.011", "MKTID.001"], NEW,
      ("P", "to print, save and send the remittance schedule", "the insurer knows what it is paid for",
       "SCR-RMT-04", "High", 5,
       [("an approved batch", "Marketing asks to send the schedule and I send it", "the insurer contact gets the "
         "schedule and the sending is confirmed"), ("a schedule already sent", "I send it again",
                                                   "I am warned that it was already sent")]), [
        R("FRID.RMT.007.01", "After submission the remittance schedule and the payment request can be printed and "
          "saved, with the batch number, insurer, remittance type and payment details, in the layouts of Annex III.",
          "k", "", "-", "Partial", "Schedule (PDF and Excel) and payment request (PDF) per batch; the Annex III "
          "layouts with the Mall Assurance columns are delivery work (OQ42).",
          ["The schedule of an approved batch opens in PDF and Excel with its batch number and totals."]),
        R("FRID.RMT.007.02", "Marketing can ask for the schedule to be sent to the insurer, with the client details "
          "(client ID, amount, date); the sending is confirmed and a second sending needs a confirmation.", "k", "",
          "-", "Fit", "Send schedule with a send log.",
          ["After sending, the batch shows Schedule sent with the date, time and recipient."]),
        R("FRID.RMT.007.03", "The schedule is e-mailed to the insurer's remittance contact through BDO's e-mail "
          "service, protected as agreed with the insurer.", "k", "int", "-", "Integration",
          "E-mail through the BIBS messaging service; the BDO e-mail service (CCM) is an integration item.",
          ["The insurer contact receives the e-mail with the schedule attached."]),
    ]),
    E("FRID.RMT.008", "Sending and uploading of file", "Insurer OR schedule and exception report",
      ["RMTID.012", "RMTID.013", "RMTID.016"], NEW,
      ("P", "to upload the insurer's ORs and see the exceptions", "every account is closed with its OR",
       "SCR-RMT-05", "Highest", 8,
       [("the schedule returned by the insurer with OR number and date per client", "I upload it",
         "the invoices show the insurer OR and the exception report lists the differences"),
        ("an OR number already recorded for the client", "I upload it", "the line is refused as a duplicate")]), [
        R("FRID.RMT.008.01", "The populated schedule received from the insurer, with the OR date and number per "
          "client, is accepted in the layout of the extract it answers (Excel or text). Its integrity is checked: "
          "batch number, insurer, invoices of the batch, amounts and duplicate OR numbers.", "k", "", "ICD IF-RMT-03",
          "Partial", "Upload of the insurer OR file in the BIBS template (Excel or delimited text) with these "
          "checks; reading the returned extract layout and the insurer channels are delivery work.",
          ["A returned Excel extract with the OR columns filled for 3 accounts updates 3 invoices."]),
        R("FRID.RMT.008.02", "The upload updates each invoice and batch line with the insurer OR date and number, and "
          "confirms the number of records updated and refused.", "k", "", "-", "Fit", "OR update with the run result.",
          ["The invoice view shows the insurer OR number and date after the upload."]),
        R("FRID.RMT.008.03", "After each upload an exception report compares the OR amount with the paid PR of each "
          "account, with reference, OR amount, paid PR, status and reason, and a summary.", "k", "", "-", "Fit",
          "Exception report of the upload in Excel and PDF.",
          ["An OR of 9,900.00 against a paid PR of 10,000.00 is listed with status Amount mismatch."]),
    ]),
    E("FRID.RMT.009", "Search and view", "Search, inquiry and invoice view",
      ["RMTID.024", "RMTID.025", "RMTID.026", "RMTID.027", "RMTID.028", "RMTID.030"], NEW,
      ("P", "to search accounts and see their payment and remittance history", "I answer insurers and Marketing",
       "SCR-RMT-10", "High", 5,
       [("an assured name typed in part", "I search", "the matching invoices show with their status"),
        ("an invoice", "I open it", "its payments, batches and ORs show")]), [
        R("FRID.RMT.009.01", "Extracted accounts can be listed and filtered by insurer, remittance type and batch.",
          "k", "", "-", "Fit", "Batches and DTIP Status lists.",
          ["Filtering on insurer PISC and Normal - Dollar lists only those accounts."]),
        R("FRID.RMT.009.02", "Accounts and payment details can be searched by invoice number, remittance batch "
          "number, endorsement reference number, policy number or assured name, exact or partial, whatever the case.",
          "k", "", "-", "Fit", "Remittance search.", ["Searching 'trading' finds 'ALPHA TRADING CORPORATION'."]),
        R("FRID.RMT.009.03", "The account or invoice view shows the payment status, remittance type, insurer and the "
          "history of payments, batches, ORs, holds and adjustments.", "k", "", "-", "Fit", "Invoice view.",
          ["The invoice view of a remitted invoice shows its batch number and the insurer OR."]),
        R("FRID.RMT.009.04", "The accounts submitted from Process Remittance are listed with their status and "
          "batch.", "k", "", "-", "Fit", "Batches by stage.",
          ["The For Approval tab lists the submitted batches with their accounts."]),
        R("FRID.RMT.009.05", "The payment status of an account (posted, cleared) is shown before extraction or "
          "processing.", "k", "", "-", "Fit", "Payment status on the lines and the invoice view.",
          ["A line paid by an uncleared check shows Check not cleared."]),
        R("FRID.RMT.009.06", "Special remittance requests can be viewed with their condition, requester and status.",
          "k", "", "-", "Fit", "Special Remittance list.",
          ["The list shows each request with its condition and status."]),
    ]),
    E("FRID.RMT.010", "Hold remittance management", "Hold requests",
      ["MKTID.002", "MKTID.003", "MKTID.004", "MKTID.005", "MKTID.006", "MKTID.007", "RMTID.031"], NEW,
      ("M", "to hold the remittance of an account and release it later", "the insurer is not paid too early",
       "SCR-RMT-06", "High", 8,
       [("an invoice of my unit", "I request a hold until 30-Nov and the Marketing Team Leader approves",
         "the invoice is excluded from extraction until then"),
        ("an active hold", "I release it", "the invoice is extracted by the next run")]), [
        R("FRID.RMT.010.01", "Marketing tags an account On Hold with a reason and a hold-until date; it is excluded "
          "from extraction while the hold is active.", "k", "", "BR-RMT-08", "Fit", "Hold request with reason.",
          ["An active hold makes the invoice Unextracted - due with the reason Account on hold."]),
        R("FRID.RMT.010.02", "Hold requests can be created, cancelled and extended.", "k", "", "-", "Fit",
          "Request, extend and cancel actions.", ["An extension to a later date waits for approval."]),
        R("FRID.RMT.010.03", "Holds and their cancellations are approved by a Marketing Team Leader other than the "
          "requester.", "k", "", "BR-RMT-01", "Fit", "Approval by another user.",
          ["The requester cannot approve his own hold."]),
        R("FRID.RMT.010.04", "Each hold request has a unique reference number.", "k", "", "-", "Fit",
          "Hold reference number.", ["Two requests get two different reference numbers."]),
        R("FRID.RMT.010.05", "Approved holds are assigned to remittance processors.", "k", "", "-", "Fit",
          "Assignment of the approved hold.", ["The assigned processor sees the hold in his list."]),
        R("FRID.RMT.010.06", "The On Hold tag can be removed so that the account is extracted again.", "k", "",
          "BR-RMT-08", "Fit", "Release action.", ["A released hold makes the invoice eligible at the next run."]),
    ]),
    E("FRID.RMT.011", "Special remittance", "Special remittance requests", ["MKTID.009", "RMTID.030"], NEW,
      ("M", "to request a special remittance for an account that needs it now", "a claim or renewal is not blocked",
       "SCR-RMT-08", "High", 5,
       [("an invoice with an open claim", "I request a special remittance with condition Claims",
         "the Claims module confirms the claim and the request waits for approval"),
        ("no claim on the invoice", "I request with condition Claims", "the request is refused")]), [
        R("FRID.RMT.011.01", "Marketing requests a special remittance for accounts that meet a condition: claims, "
          "renewal, instalment due or immediate OR issuance; the claims condition is confirmed by the Claims module.",
          "k", "", "-", "Fit", "Special remittance request with its condition checks.",
          ["A request with condition Claims on an invoice without an open claim is refused."]),
        R("FRID.RMT.011.02", "An approved request flows to Process Remittance, grouped by remittance type, processor "
          "and insurer.", "k", "", "BR-RMT-01", "Fit", "Approved request creates its batch.",
          ["The approved request appears as a Special batch in Review in Process."]),
    ]),
    E("FRID.RMT.012", "Extraction", "Early remittance incentive", ["RMTID.023"], NEW,
      ("T", "the early remittance incentive computed by rule", "BDOI earns it without manual work", "SCR-RMT-11",
       "Medium", 5,
       [("a rule of 2% within 30 days from inception for insurer X", "an account is remitted 20 days after "
         "inception", "the incentive is computed on the With Incentives batch")]), [
        R("FRID.RMT.012.01", "The additional incentive for early remittance is computed when the remittance is made "
          "within the window of the rule (for example 30 days from inception), from the rule table per insurer and "
          "line with its rate and window.", "k", "", "BR-RMT-14", "Fit", "Incentive rules with rate and window; "
          "rates to confirm (Q-RM-14).", ["An account remitted 31 days after inception gets no incentive."]),
        R("FRID.RMT.012.02", "The incentive is shown on the With Incentives schedule with Basic Premium, Incentive, "
          "VAT on Incentive and Net Due After Incentive.", "k", "", "-", "Fit", "With Incentives layout.",
          ["The With Incentives schedule shows the incentive columns and totals."]),
    ]),
    E("FRID.RMT.013", "Notifications and tracking", "Notices and tracking",
      ["RMTID.032", "RMTID.033", "RMTID.034", "RMTID.035", "RMTID.036"], NEW,
      ("P", "to be told of every change of my batches and accounts", "I act on time", "SCR-RMT-01", "Medium", 3,
       [("a batch returned by the Team Leader", "I open the bell", "the notice opens the batch"),
        ("a pending negative adjustment on an extracted invoice", "it is raised", "the Remittance team is told")]), [
        R("FRID.RMT.013.01", "Users are told of status changes: returned, approved, on hold, and of the decisions on "
          "their hold and special remittance requests.", "k", "", "-", "Fit", "Notices of the batch, hold and "
          "special remittance stages.", ["The requester of a hold is told when it is approved."]),
        R("FRID.RMT.013.02", "Users are told of the remittance and disbursement status: payment voucher assigned, "
          "partially or fully remitted.", "k", "", "-", "Fit", "Disbursement feedback updates the batch stage.",
          ["When Disbursement pays the batch it becomes Fully Remitted and the processor is told."]),
        R("FRID.RMT.013.03", "The Remittance team is told of pending negative adjustments on its accounts.", "k", "",
          "-", "Fit", "Notice on a pending negative adjustment.",
          ["A decrease of premium requested on an extracted invoice notifies the processor of its batch."]),
        R("FRID.RMT.013.04", "The hold and remittance status of each account is tracked across every stage, from "
          "extraction to the insurer OR.", "k", "", "-", "Fit", "Remittance status per invoice.",
          ["The invoice view shows the stages of its remittance with dates."]),
    ]),
    E("FRID.RMT.014", "Reports", "Remittance reports", ["RMTID.039"], NEW,
      ("C", "the remittance reports of the BRD", "I follow the premium due to insurers", "SCR-RMT-12", "High", 8,
       [("a period", "I run the Remittance Tracker", "it lists the batches per insurer and type with net due")]), [
        R("FRID.RMT.014.01", "The eight Remittance reports of Annex III are available: Remittance Tracker, Special "
          "Remittance Register, Remittance Schedule Normal, Remittance Schedule Special, DTIP Status Summary, DTIP "
          "Status Detailed, Remittance Schedule with Incentives, List of Remitted Accounts with Batch Number.", "k",
          "", "BR-RMT-15", "Partial", "The eight reports run on screen and export to Excel and PDF; the Mall "
          "Assurance columns of the Normal schedule and the layout of the List of Remitted Accounts are delivery "
          "work (OQ42).", ["Each of the eight reports runs for a period and exports to Excel and PDF."]),
        R("FRID.RMT.014.02", "Report and extract files are named by the convention of the BDOI Report List "
          "(YYYYMMDD + insurer code + settlement number + _type).", "k", "", "BR-RMT-05", "Partial",
          "Report file names follow the report code and date; the Report List convention is delivery work with "
          "FRID.RMT.001.04.", ["The Normal schedule of batch SICI00003992 of 31-Jul-2024 downloads as "
                               "20240731SICI00003992_NormalPeso.xls."]),
    ]),
    E("FRID.RMT.015", "Controls", "Coordination of adjustments, remittance and accounting", ["RMTID.040"], NEW,
      ("T", "adjustments, remittance and accounting entries to follow one another under locks",
       "an invoice is never remitted while it is being changed", "SCR-RMT-04", "High", 3,
       [("an invoice in an open batch", "an adjustment is requested on it", "the invoice shows the lock and the "
         "adjustment waits until the batch is approved or the line excluded")]), [
        R("FRID.RMT.015.01", "Invoice adjustments, remittance exemptions and accounting entries follow one another: "
          "an invoice in an open batch is locked, its status indicator shows the lock and its reason, and the "
          "release follows the approvals.", "k", "", "BR-RMT-07", "Fit", "Invoice locks with reason.",
          ["The invoice view shows Locked by Remittance with the batch number."]),
    ]),
    E("FRID.RMT.016", "Marketing", "Marketing items specified in other FRS", ["MKTID.010", "MKTID.011"], NEW,
      ("M", "the BIR 2307 and direct payment tags I give to reach Remittance", "remittance uses the right amounts",
       "SCR-RMT-10", "Low", 1,
       [("an invoice tagged direct payment at booking", "the extraction runs", "it is not extracted")]), [
        R("FRID.RMT.016.01", "BIR 2307 tagging by Marketing, the withholding rate at AR or OR creation and the check "
          "whether the payment was already remitted are specified in the Collections FRS (BRD-04) and the Operations "
          "Cashiering FRS; Remittance reads their result.", "k", "", "-", "In another FRS",
          "Specified and delivered with Collections and Cashiering.",
          ["A reversal of a remitted payment goes to Unapplied Payments, not to Remittance."]),
        R("FRID.RMT.016.02", "The direct payment tag set at quotation or conversion to policy is specified in New "
          "Business (BRD-01) and Collections (BRD-04); a direct payment invoice is not extracted.", "k", "", "-",
          "In another FRS", "Tag copied from booking to the invoice.",
          ["A direct payment invoice never appears in a remittance batch."]),
    ]),
]
for _f in FRUM:
    for _r in _f["reqs"]:
        assert _r["id"].startswith(_f["id"] + "."), _r["id"]
ACCEPTANCE = {r["id"]: r["ac"] for f in FRUM for r in f["reqs"]}

# ------------------------------------------------------------------------------------------------ BRD mapping
BRD_ROWS = [
    ("RMTID.001", "Extraction per insurer and per remittance type (With Incentives, Normal - Dollar, Normal - Peso); "
     "off-peak; batch run to be defined."),
    ("RMTID.002", "View the extracted file online and mark records for exclusion only (Addendum 1)."),
    ("RMTID.003", "Automatic extraction on predefined schedules and criteria."),
    ("RMTID.004", "Manual extraction of a specific invoice number per insurer."),
    ("RMTID.005", "Search account and payment details per invoice or batch; trigger extraction at end of day."),
    ("RMTID.006", "Extract only from applied and posted payments, all channels."),
    ("RMTID.007", "Group extract by remittance type, processor, insurer and batch number."),
    ("RMTID.008", "Unique reference or batch number for each remittance extract."),
    ("RMTID.009", "Transfer extracted accounts to Process Remittance with status Review in process."),
    ("RMTID.010", "Submit accounts for remittance processing."),
    ("RMTID.011", "Print and save the remittance schedule and payment request."),
    ("RMTID.012", "Receive the populated remittance schedule from the insurer with the OR details."),
    ("RMTID.013", "Accept and upload the received schedule; update the OR details."),
    ("RMTID.014", "Paid AR not more than the outstanding DTIP per remittance."),
    ("RMTID.015", "List of paid AR higher than the DTIP balance."),
    ("RMTID.016", "Exception report of the OR amount against the paid PR after upload."),
    ("RMTID.017", "Checks at least 3 days old and cleared before extraction (holding period)."),
    ("RMTID.018", "Validate the check holding period and clearance."),
    ("RMTID.019", "Validate the remittance status on submission (eight statuses)."),
    ("RMTID.020", "Exclude accounts on hold or with pending negative adjustment requests."),
    ("RMTID.021", "Process an account when its holding date has expired."),
    ("RMTID.022", "Exclude written-off accounts."),
    ("RMTID.023", "Automate the additional incentive for early remittance to the insurer."),
    ("RMTID.024", "View extracted accounts."),
    ("RMTID.025", "Search by invoice, batch, endorsement reference, policy number or assured name."),
    ("RMTID.026", "View account and invoice details."),
    ("RMTID.027", "View accounts submitted from Process Remittance."),
    ("RMTID.028", "Check the payment status."),
    ("RMTID.029", "Submit, hold and return invoices or batch numbers."),
    ("RMTID.030", "View special remittance requests."),
    ("RMTID.031", "Exclude active hold accounts during extraction."),
    ("RMTID.032", "Track hold status and remittance status."),
    ("RMTID.033", "Notify users on status changes (returned, approved, on hold)."),
    ("RMTID.034", "Notify users on remittance and disbursement status."),
    ("RMTID.035", "Notify the Remittance team of pending negative adjustments."),
    ("RMTID.036", "Track remittance status across all stages."),
    ("RMTID.039", "Generate the remittance reports (Annex)."),
    ("RMTID.040", "Coordinate invoice adjustments, remittance exemptions and accounting entries with locks, status "
     "indicators and approvals."),
    ("MKTID.001", "Request to send a remittance schedule to the insurer, with client details, and confirm sending."),
    ("MKTID.002", "Remove the On Hold tagging of accounts to be included in the extraction."),
    ("MKTID.003", "Tag accounts On Hold, excluded during extraction."),
    ("MKTID.004", "Assign approved hold requests to remittance processors."),
    ("MKTID.005", "Create, cancel and extend hold remittance requests."),
    ("MKTID.006", "Approve hold requests and cancellations."),
    ("MKTID.007", "Unique reference numbers for hold requests."),
    ("MKTID.009", "Request and process special remittance for accounts meeting conditions (claims, renewal, "
     "instalment due, immediate OR)."),
    ("MKTID.010", "Marketing checks of BIR 2307 tagging, withholding rate, CWT details and remitted payments."),
    ("MKTID.011", "Tag direct payment during quotation creation and conversion to policy."),
]
MAPPING_NOTE = ("BDOI's FRS lists 48 Remittance lines of the Operations BRD. Each maps to entries of section 3; "
                "MKTID.010 and MKTID.011 are specified in other FRS and stated here for Remittance. RMTID.037 "
                "(estimated items) belongs to Production Reconciliation and RMTID.038 (payment history at invoice "
                "level) to the Operations Cashiering FRS (BRQID.007); BDOI's FRS does not list them either.")


def _mapping():
    rows = []
    for bid, text in BRD_ROWS:
        entries = [f["id"] for f in FRUM if bid in f["brd_ids"]]
        if not entries:
            raise SystemExit(f"BRD {bid} maps to no entry")
        note = "In another FRS" if bid in ("MKTID.010", "MKTID.011") else "-"
        rows.append((bid, text, ", ".join(entries), note))
    return rows


MAPPING = _mapping()
BRD_NFR = [
    ("Batch run", "Off-peak execution; batch run to be defined (RMTID.001).", "NFR-PER (batch window); BR-RMT-13"),
    ("Error handling", "A batch continues when a record fails; failed records logged and reported (BRQID.006).",
     "NFR-OPS (batch resilience)"),
    ("Security", "Access by user matrix, role based (FRID.RMT.001.01).", "NFR-SEC-02; BRD-11"),
    ("Audit", "Exclusions and actions recorded with user, time and details (FRID.RMT.002.04).", "NFR-AUD-01"),
    ("Retention", "Extract files kept in the shared folder.", "NFR-DATA (retention); IS-RMT-01"),
]
FRUM_NOTE = ("Each entry keeps BDOI's FRID.RMT number; FRID.RMT.001 to 003 are BDOI's items and FRID.RMT.004 to 016 "
             "complete the BRD lines. Source gives which. The Example column holds the first acceptance criterion; "
             "all the criteria are in the User Stories v1.0 and the test cases of the RTM workbook.")

BUSINESS_RULES = [
    ("BR-RMT-01", "The user who submits a batch, a hold, a special remittance or a deduction cannot approve it.", "k"),
    ("BR-RMT-02", "An account is extracted only when its payments are applied and posted, its checks are past the "
     "holding period and cleared, it is not on hold, has no pending financial endorsement or negative adjustment, is "
     "not written off and is not a direct payment account.", "k"),
    ("BR-RMT-03", "The paid AR of an invoice may not exceed its outstanding DTIP (setting REMIT_PAIDAR_OVER_DTIP_MODE: "
     "EXCLUDE the invoice or CAP the remittance at the DTIP).", "k"),
    ("BR-RMT-04", "A check is held 3 banking days on the holiday calendar and must be cleared before its payment is "
     "remitted (setting REMIT_CHECK_HOLD_DAYS = 3).", "k"),
    ("BR-RMT-05", "The batch number is the insurer code followed by an 8-digit settlement number, never reused. The "
     "extract and report files are named YYYYMMDD + batch number + _NormalPeso, _NormalDollar or _WithIncentives.",
     "k"),
    ("BR-RMT-06", "Amounts of an extracted account are read-only. An account is excluded or restored, with a reason, "
     "only while the batch is under review; only the included accounts are paid.", "k"),
    ("BR-RMT-07", "An invoice is in one open batch at a time and is locked while there; a returned or excluded line "
     "is free for a later run.", "k"),
    ("BR-RMT-08", "An active hold excludes the account from every extraction until it is released or its hold-until "
     "date passes.", "k"),
    ("BR-RMT-09", "Net Due = Paid AR - Realized Commission - Realized VAT + WTAX; Net Due After Mall Assurance = Net "
     "Due - Mall Assurance Total Deductions; Grand Total = Total Remittance - Mall Assurance deductions, VAT and WTAX "
     "(as read from the specimen extracts; Q-RM-07).", "k"),
    ("BR-RMT-10", "The header record of a text extract carries the number of detail records and the totals of their "
     "amount fields; a file whose totals differ is refused.", "k"),
    ("BR-RMT-11", "Every action is recorded with user, time, record and the values before and after; the record is "
     "insert-only.", "k"),
    ("BR-RMT-12", "A user sees and acts only within the permissions of his group profiles and his company.", "k"),
    ("BR-RMT-13", "The scheduled extraction runs off-peak on the days and time set by the administrator; a failure "
     "on one account does not stop the run.", "k"),
    ("BR-RMT-14", "The early remittance incentive applies only within the window of its rule, counted from the "
     "inception date to the remittance date.", "k"),
    ("BR-RMT-15", "Reports list amounts with two decimals and dates as dd-MMM-yyyy, have a total line and a footer "
     "with the user, date and time.", "k"),
]

MESSAGES = [
    ("Extraction", "Invoice not posted or not eligible", "Invoice <invoice> cannot be extracted: <reason>"),
    ("Extraction", "Invoice of another insurer", "Invoice <invoice> is not an invoice of <insurer>"),
    ("Batch", "Exclude a line already excluded", "Invoice <invoice> is already excluded"),
    ("Batch", "Restore a line not excluded or after submission", "Invoice <invoice> cannot be restored"),
    ("Batch", "Submit an empty batch", "The batch has no included account"),
    ("Batch", "Submit with an invalid account status", "Invoice <invoice> is <status>: the batch cannot be submitted"),
    ("Batch", "Approver is the submitter", "The submitter cannot approve batch <batch>"),
    ("Batch", "Action not allowed in the stage", "Batch <batch> is <stage>"),
    ("Batch", "Send a schedule not approved", "The schedule of <batch> is sent after approval"),
    ("Batch", "Send again", "The schedule of <batch> was already sent on <date>; confirm to send it again"),
    ("Insurer OR Upload", "Batch not approved", "Batch <batch> is <stage>: not yet approved"),
    ("Insurer OR Upload", "Invoice excluded from the batch", "Invoice <invoice> was excluded from <batch>"),
    ("Insurer OR Upload", "OR already recorded", "OR <number> was already recorded for client <client>"),
    ("Insurer OR Upload", "Missing columns", "The file must have the columns <columns>"),
    ("Insurer OR Upload", "Totals differ from the header", "The totals of the file do not match its header record"),
    ("Remittance Holds", "Hold-until in the past", "The hold-until date must be a later date"),
    ("Remittance Holds", "Invoice already on hold", "Invoice <invoice> already has an active hold"),
    ("Remittance Holds", "Approver is the requester", "The requester cannot approve hold <reference>"),
    ("Special Remittance", "Condition not met", "Invoice <invoice> does not meet the condition <condition>"),
    ("Special Remittance", "Request already open", "Invoice <invoice> already has a special remittance request"),
    ("Incentive Rules", "Overlapping period", "The rule overlaps another rule of the same insurer and line"),
]

PERSONA_HEADS = ["Function", "Processor", "Rem. TL", "Mkt Coll.", "Mkt TL", "Auditor"]
_ROLES = ["REMIT_PROCESSOR", "REMIT_TL", "MKT_COLLECTION", "MKT_TL", "AUDITOR"]
_GRANTS = {
    "REMIT_EXTRACT": "REMIT_PROCESSOR REMIT_TL", "REMIT_PROCESS": "REMIT_PROCESSOR REMIT_TL",
    "REMIT_EXCLUDE": "REMIT_PROCESSOR REMIT_TL", "REMIT_APPROVE": "REMIT_TL",
    "REMIT_OR_UPLOAD": "REMIT_PROCESSOR REMIT_TL", "HOLD_REQUEST": "MKT_COLLECTION MKT_TL",
    "HOLD_APPROVE": "MKT_TL", "SPECIAL_REMIT_REQUEST": "MKT_COLLECTION MKT_TL", "SPECIAL_REMIT_APPROVE": "REMIT_TL",
    "OPS_REPORT_VIEW": "REMIT_PROCESSOR REMIT_TL MKT_COLLECTION AUDITOR",
}
_FUNCTIONS = [
    ("Run extractions; end-of-day queue", "REMIT_EXTRACT"), ("Work batches; send schedules", "REMIT_PROCESS"),
    ("Exclude and restore accounts", "REMIT_EXCLUDE"), ("Approve or return batches; incentive rules",
                                                        "REMIT_APPROVE"),
    ("Upload insurer ORs", "REMIT_OR_UPLOAD"), ("Request holds", "HOLD_REQUEST"), ("Approve holds", "HOLD_APPROVE"),
    ("Request special remittance", "SPECIAL_REMIT_REQUEST"), ("Approve special remittance", "SPECIAL_REMIT_APPROVE"),
    ("Operations reports", "OPS_REPORT_VIEW"),
]
PERMISSIONS = [(f"{name} ({perm})", *("Y" if r in _GRANTS[perm].split() else "-" for r in _ROLES))
               for name, perm in _FUNCTIONS]
ACCESS_NOTE = ("Users and their group profiles come from UIDM-ISC and every user signs in with Microsoft Entra ID "
               "(User Access Maintenance, BRD-11); there is no break-glass account. The permissions of each group "
               "profile are kept in BIBS under maker-checker. The reinsurance functions are Phase 2 and hidden from "
               "the BDOI profiles.")

PARAMETER_NOTE = ("Settings are changed on System Parameters and applied after another user approves the change; "
                  "the extraction schedule and output folders on Remittance Settings (proposed). Keys, types and "
                  "ranges are in the Data Dictionary, sheet Parameters.")
PARAMETERS = [
    ("REMIT_CHECK_HOLD_DAYS: banking days a check is held", "3", "0 to 10", "Eligibility", "k"),
    ("REMIT_PAIDAR_OVER_DTIP_MODE: paid AR above the DTIP", "EXCLUDE", "EXCLUDE, CAP", "Eligibility", "k"),
    ("REMIT_FILE_PATTERN: name of the extract files", "{date}{batchNo}_{type} (BR-RMT-05)", "Tokens", "Files", "k"),
    ("Extraction schedule: days and time of the scheduled run", "Working days 20:00", "Days, time", "Extraction",
     "k"),
    ("Output folders per remittance type", "Normal - Peso, Normal - Dollar, With Incentives", "Folder", "Files", "k"),
    ("EARLY_INCENTIVE_WTAX_RATE: withholding on the incentive invoice", "2", "Percent", "Incentive", "k"),
    ("Approval amount limit per Team Leader", "(none)", "Amount", "Approval", "g"),
]
LOVS = [
    ("Remittance type (REMITTANCE_TYPE)", "With Incentives; Normal - Peso; Normal - Dollar (Special for the special "
     "remittance batches)"),
    ("Exclusion reason (REMIT_EXCLUSION_REASON)", "Account on hold; Pending negative adjustment; Written off; Check "
     "within the holding period or not cleared; Paid AR greater than DTIP; Payment not yet applied and posted; Others"),
    ("Return reason (REMIT_RETURN_REASON)", "Others (see comment); values to give (Q-RM-16)"),
    ("Hold reason (HOLD_REASON)", "Others (see comment); values to give (Q-RM-16)"),
    ("Special remittance condition (SPECIAL_REMIT_CONDITION)", "Claims; Renewal; Installment due; Immediate OR "
     "issuance"),
    ("Remittance status of an account (RMTID.019)", "Approved; Review in Process; Requested for Hold; Unapplied "
     "Payment; Outstanding Balance; Unprocessed; Fully Remitted; Partially Remitted"),
]

LIFECYCLES = {
    "batch": ([("R", "Review in\\nProcess"), ("H", "On Hold"), ("F", "For Approval"), ("A", "Approved"),
               ("P", "Partially\\nRemitted"), ("U", "Fully\\nRemitted"), ("O", "OR Received"), ("T", "Returned")],
              [("R", "F", "Submit"), ("R", "H", "Hold"), ("H", "R", "Resume"), ("F", "A", "Approve (another user)"),
               ("F", "R", "Send back"), ("R", "T", "Return"), ("F", "T", "Return"), ("A", "P", "Paid in part"),
               ("A", "U", "Paid"), ("P", "U", "Paid"), ("U", "O", "Insurer ORs uploaded")],
              "Stages of a remittance batch"),
    "hold": ([("D", "Draft"), ("F", "For Approval"), ("A", "Active"), ("E", "Extension\\nFor Approval"),
              ("C", "Cancel\\nFor Approval"), ("L", "Released"), ("J", "Rejected"), ("X", "Cancelled")],
             [("D", "F", "Submit"), ("F", "A", "Approve"), ("F", "J", "Reject"), ("A", "E", "Extend"),
              ("E", "A", "Approve or reject"), ("A", "C", "Cancel"), ("C", "X", "Approve"), ("A", "L", "Release or "
                                                                                          "expiry")],
             "Stages of a hold request"),
}
STATUSES = {
    "Remittance batch": [("Review in Process", "With the processor; lines can be excluded and restored.",
                          "For Approval; On Hold; Returned"),
                         ("On Hold", "Put on hold by the processor.", "Review in Process"),
                         ("For Approval", "Waiting for the Team Leader.", "Approved; Review in Process; Returned"),
                         ("Approved", "Posted; payment request with Disbursement.", "Partially or Fully Remitted"),
                         ("Partially Remitted", "Payment voucher assigned; DTIP remains on some invoices.",
                          "Fully Remitted"),
                         ("Fully Remitted", "Payment voucher assigned; no DTIP outstanding.", "OR Received"),
                         ("OR Received", "Insurer ORs uploaded for every line.", "-"),
                         ("Returned", "Lines freed for a later run.", "-")],
    "Extraction tag of an account": [("Extracted", "Met the criteria; in a batch.", "-"),
                                     ("Unextracted - not yet due", "Not yet due for remittance.", "Extracted"),
                                     ("Unextracted - due", "Due but excluded, with the reason.", "Extracted"),
                                     ("Returned", "Extracted and returned by a user.", "Extracted")],
    "Hold request": [("Draft", "Being prepared.", "For Approval"), ("For Approval", "Waiting for approval.",
                                                                    "Active; Rejected"),
                     ("Active", "Account excluded from extraction.", "Extension or Cancel For Approval; Released"),
                     ("Released", "Hold removed or expired.", "-"), ("Rejected / Cancelled", "Closed.", "-")],
    "Special remittance request": [("Requested", "Saved by Marketing.", "For Approval"),
                                   ("For Approval", "Waiting for the Remittance Team Leader.",
                                    "In Process Remittance; Rejected; Returned"),
                                   ("In Process Remittance", "Batch created.", "Pushed to Disbursement"),
                                   ("Pushed to Disbursement", "Paid through the batch.", "-"),
                                   ("Rejected / Returned", "Closed or back to the requester.", "-")],
}

ASSUMPTIONS = [
    ("A-RMT-01", "The Operations BRD v1.01 with Addendum 1 (version of 8-Oct-2026) is the baseline; BDOI's FRS for "
     "Remittance (Release 1) adds the file formats and naming."),
    ("A-RMT-02", "All modules go live together in January 2028 (BDOI decision of 26-Sep-2026)."),
    ("A-RMT-03", "The three specimen files show the layouts in use; every insurer receives the same layout per "
     "remittance type."),
    ("A-RMT-04", "Remittance of reinsurance transactions starts with the reinsurance module in Phase 2."),
]
DEPENDENCIES = [
    ("D-RMT-01", "BDO IT gives the shared drive path, folders and access for the extract files (IS-RMT-01)."),
    ("D-RMT-02", "BDO IT confirms the e-mail service (CCM) for the schedules sent to insurers (IS-RMT-02)."),
    ("D-RMT-03", "Data Migration loads the legacy invoices with premium still due to insurers, open holds and the "
     "last settlement number per insurer before go-live (DM-RMT-01, DM-RMT-02)."),
    ("D-RMT-04", "Disbursement (BRD-05) pays the batches and returns the payment status."),
    ("D-RMT-05", "BDOI gives one returned insurer file per format with the OR columns filled, to confirm IF-RMT-03."),
]
OPEN_QUESTIONS = [
    ("Q-RM-01", "Text extract name: the specimen is AM01260317PGAI00005787.txt (AM01 + YYMMDD + insurer code + "
     "settlement number); FRID.RMT.001.04 says YYYYMMDD + insurer code + settlement number + _NormalPeso.txt. "
     "Which applies, and what is AM01?", "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-02", "Text extract header: meaning of the fields 2 (M), 4 (01) and 5 (1) and of the detail fields 5 "
     "(invoice number on 10 digits), 8, 17 to 20 and 22 to 27; is the text file sent to every insurer or only to "
     "some?", "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-03", "The Normal - Dollar specimen is titled REMITTANCE EXTRACT NORMAL PESO and its sheet is named "
     "NormalPeso. Confirm the title of the Dollar extract and that its amounts are in USD.",
     "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-04", "Settlement number: is the 8-digit number per insurer or across insurers, does it continue the "
     "legacy numbers at go-live, and how does the insurer's OR \"reflect\" it?", "Remittance Process Owner",
     ANSWER_BY),
    ("Q-RM-05", "Mall Assurance columns: definition and source of Mode of Payment, Service Charge, Credit / Debit "
     "Card Charge, Total Deductions, VAT and WTAX, and the accounts they apply to (the specimen shows Mode of Payment "
     "as an amount).", "Remittance Process Owner; Comptrollership", ANSWER_BY),
    ("Q-RM-06", "Extract columns: values of With COI, Record Status, OR Delivery and Payment Status; Annex III names "
     "Date Reversed where the specimen shows Date Applied; is Excel 97-2003 (.xls) required or is .xlsx accepted?",
     "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-07", "Formulas: the specimens give Net Due = Paid AR - Realized Commission - Realized VAT + WTAX; Amount "
     "Due for Remittance and OR Amount are 0.00 in both. When is Amount Due for Remittance filled and how is it "
     "computed?", "Comptrollership", ANSWER_BY),
    ("Q-RM-08", "Shared drive: path, folder per type, retention of the files and who may read them.", "BDO IT",
     ANSWER_BY),
    ("Q-RM-09", "FRID.RMT.003.02 is blank in BDOI's FRS: confirm the content proposed here (tags of the accounts "
     "checked by a run).", "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-10", "Schedule of the extraction (RMTID.001 'Batch Run: TBD'): days and time, and who may change it "
     "(proposed: the Remittance Team Leader on Remittance Settings).", "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-11", "End-of-day trigger (RMTID.005): extract at end of day every invoice searched for remittance, or "
     "only those the processor queues (proposed)?", "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-12", "Paid AR above DTIP: exclude the invoice (acceptance criteria of RMTID.014) or remit the DTIP "
     "balance (expected result of RMTID.014)?", "Comptrollership", ANSWER_BY),
    ("Q-RM-13", "Check clearing: which evidence marks a check as cleared (bank reconciliation, bank file) and from "
     "which date the 3 banking days count (deposit or receipt)?", "Comptrollership", ANSWER_BY),
    ("Q-RM-14", "Early remittance incentive: rates, window and basis date per insurer and line.",
     "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-15", "Insurer OR return: will insurers return the extract file with the OR columns filled (Excel or "
     "text), or a file of their own; by e-mail or another channel?", "Remittance Process Owner; BDO IT", ANSWER_BY),
    ("Q-RM-16", "Hold reasons and return reasons: the lists of values.", "Remittance Process Owner", ANSWER_BY),
    ("Q-RM-17", "Reinsurance: the drop plan places reinsurance transactions in Drop 1 Remittance; the programme "
     "decision defers reinsurance to Phase 2. Confirm that no reinsurance remittance type is needed at go-live.",
     "Product Owner, Operations", ANSWER_BY),
    ("Q-RM-18", "MKTID.010 and MKTID.011 are listed in this FRS and specified in the Collections, Cashiering and New "
     "Business FRS: confirm that they leave the Remittance FRS.", "Product Owner, Operations", ANSWER_BY),
]

CRS: list = []
CR_DECISION = ("Register v1.0: no change request. Every requirement of BDOI's Remittance FRS is asked by the "
               "Operations BRD or the BDOI Report List; the partial and gap requirements are delivery work, listed "
               "in the Delivery Work sheet of the Effort Estimate.")
OOTB_POINTS = [
    ("G-RMT-01", "Approval of a batch within an amount limit per Team Leader (authority matrix of the OOTB "
     "platform), above which a second approver is needed", "FRID.RMT.006.06"),
]
INTEGRATION_POINTS = [
    ("IS-RMT-01", "Copy of the extract files to BDOI's shared drive, one folder per remittance type",
     "FRID.RMT.001.05; ICD IF-RMT-04"),
    ("IS-RMT-02", "E-mail of the remittance schedule to the insurer through BDO's e-mail service (CCM)",
     "FRID.RMT.007.03; ICD IF-RMT-05"),
    ("IS-RMT-03", "Insurer OR returns received from the insurers' channels", "FRID.RMT.008.01; ICD IF-RMT-03"),
    ("DM-RMT-01", "Legacy invoices with premium due to insurers included in the extraction after go-live",
     "FRID.RMT.005.01; Data Migration (BRD-13)"),
    ("DM-RMT-02", "Open holds, open batches and the last settlement number per insurer at go-live",
     "FRID.RMT.006.02; Data Migration (BRD-13)"),
]
BEST_PRACTICE = [
    ("BP-RMT-01", "Ageing of premium collected and not yet remitted against each insurer's remittance terms, with an "
     "alert before the term ends", "High", 5),
    ("BP-RMT-02", "Monthly reconciliation of the premium trust bank account with the DTIP balance per insurer",
     "Medium", 6),
    ("BP-RMT-03", "A control total and record count on every file exchanged with an insurer, checked on receipt",
     "Medium", 2),
]
DELIVERY_WORK = [
    ("DW-RMT-01", "Excel extract in the layout of the current report: heading, 35 columns with the Mall Assurance "
     "columns, totals and closing lines (IF-RMT-01)", "FRID.RMT.001.03, 001.08, 002.02, 007.01", 6),
    ("DW-RMT-02", "Pipe-delimited text extract with header control totals and detail records (IF-RMT-02)",
     "FRID.RMT.001.03, 001.09", 5),
    ("DW-RMT-03", "Settlement number per insurer (insurer code + 8 digits) and the YYYYMMDD file and report naming",
     "FRID.RMT.001.04, 006.02, 014.02", 3),
    ("DW-RMT-04", "Remittance Settings screen: extraction schedule, output folders, file pattern", "FRID.RMT.003.01",
     4),
    ("DW-RMT-05", "Insurer OR return read from the returned extract layout (Excel and text) with the header totals "
     "check", "FRID.RMT.008.01", 5),
    ("DW-RMT-06", "Annex III layouts: Mall Assurance columns of the Normal schedule, List of Remitted Accounts",
     "FRID.RMT.014.01", 4),
]

REVIEW_NOTE = ("BDOI's FRS for Remittance was received on 17-Oct-2026. It is a first draft in template D051: the "
               "business requirements mapping lists 48 BRD lines without their FRS IDs, and the functional "
               "requirements cover FRID.RMT.001 to 003, FRID.RMT.003 being incomplete. This version keeps BDOI's "
               "text and numbers and completes the rest from the BRD.")
REVIEW = [
    ("Mapping column", "The column Functional Requirement Req IDs is empty for the 48 lines.",
     "Filled in section 2 with the FRID.RMT entries."),
    ("FRID.RMT.001", "Extraction, outputs, naming, shared drive and notice: complete; the outputs refer to three "
     "specimen files.", "Kept; the specimen layouts are specified in the ICD (IF-RMT-01, IF-RMT-02); 001.07 to 001.09 "
     "state the pending endorsement rule of RMTID.001 and the content of the two files."),
    ("FRID.RMT.002", "Review with exclusion only (Addendum 1); item 002.06 numbered twice.",
     "Kept; the second 002.06 becomes 002.07."),
    ("FRID.RMT.003", "Schedule set by the admin; item 003.02 has no text.", "003.01 kept; 003.02 and 003.03 proposed "
     "(Q-RM-09)."),
    ("FRID.RMT.004 to 016", "Not written in BDOI's FRS.", "Written from the BRD lines and the acceptance criteria of "
     "the BRD."),
    ("Specimen files", "The Dollar extract is titled Normal Peso; the text file name does not follow 001.04; the "
     "amount due and OR columns are empty.", "Questions Q-RM-01 to Q-RM-07."),
    ("Overview", "The project overview names tracking of ORs and Marketing dispositions for returned ORs.",
     "Returned ORs are covered by the exception report (FRID.RMT.008.03) and the return of lines "
     "(FRID.RMT.006.05)."),
    ("Cover and sign-off", "Names of persons on the cover and revision log.", "Replaced by roles."),
]
CONFLICTS = [
    ("Access by 'user matrix' (FRID.RMT.001.01)", "Users and group-profile membership come from UIDM-ISC; sign-in "
     "through Microsoft Entra ID; no break-glass account (FRS User Access Maintenance v1.6).",
     "Access is by the permissions of the group profiles; no local user maintenance in Remittance."),
    ("'Admin' sets the schedule (FRID.RMT.003.01)", "Business settings are changed by business owners under "
     "maker-checker; system jobs by BDO IT.", "Proposed: the Remittance Team Leader sets the schedule on Remittance "
     "Settings, approved by another user (Q-RM-10)."),
    ("Reinsurance transactions in Remittance", "Reinsurance is Phase 2; its functions are hidden from the BDOI "
     "profiles and shown only to the Brokerverse administrator.", "No reinsurance remittance type in Drop 1 "
     "(Q-RM-17)."),
    ("MKTID.010 and MKTID.011", "BIR 2307 tagging is a Collections disposition (BRD-04); the direct payment tag is "
     "set in New Business (BRD-01).", "Kept in the mapping, specified in the other FRS (FRID.RMT.016)."),
    ("Edit of the extract (RMTID.002 of the main BRD)", "Addendum 1 replaced online editing by exclusion only.",
     "FRID.RMT.002 follows the addendum."),
]
RENUMBERED = [("FRID.RMT.002.06 (second)", "FRID.RMT.002.07", "Number used twice in BDOI's FRS.")]

# ================================================================================================ UXD
UXD_NOTE = ("Most screens are offered by the BIBS platform today; the Availability row of each screen names the "
            "changes of this FRS still to deliver and marks the proposed new screen. Screenshots are added in the "
            "next version, after the walkthrough of the screens with the Business Unit on the test environment.")

# ================================================================================================ BPS
BPS_NOTE = ("The storyboards use the walkthrough accounts and fictitious clients, insurers and amounts; the "
            "screens they name are described in the UX Design.")
JOURNEY_SPAN = "the posting of the payment to the insurer's official receipt"
JOURNEY = [
    ("1", "Cashiering", "Payment applied and posted to the invoice"),
    ("2", "Marketing Collection", "Hold or special remittance requested when needed"),
    ("3", "Marketing Team Leader", "Hold approved or rejected"),
    ("4", "System", "Scheduled extraction checks eligibility and tags every account"),
    ("5", "System", "Batches per insurer and type with Excel and text files in the shared folder"),
    ("6", "Remittance Processor", "Batch reviewed, accounts excluded or restored, previewed"),
    ("7", "Remittance Processor", "Batch submitted for approval"),
    ("8", "Remittance Team Leader", "Batch approved or returned"),
    ("9", "Disbursement", "Payment request paid; status back to Remittance"),
    ("10", "Remittance Processor", "Schedule sent to the insurer"),
    ("11", "Insurer", "Schedule returned with OR date and number per client"),
    ("12", "Remittance Processor", "Insurer ORs uploaded; exception report reviewed"),
]
PROCESSES = [
    {"id": "P-RMT-01", "name": "Extraction", "steps": "4, 5", "trigger": "Schedule; manual extraction; end-of-day "
     "queue; approved special remittance", "actors": "System, Remittance Processor",
     "outcome": "Batches with their files; tags and reasons for the accounts not extracted",
     "controls": "BR-RMT-02 to BR-RMT-05, BR-RMT-13", "frs": "FRID.RMT.001, 003, 004, 005"},
    {"id": "P-RMT-02", "name": "Process Remittance", "steps": "6 to 9", "trigger": "Batch in Review in Process",
     "actors": "Remittance Processor, Remittance Team Leader, Disbursement",
     "outcome": "Approved batch paid to the insurer", "controls": "BR-RMT-01, BR-RMT-06, BR-RMT-07",
     "frs": "FRID.RMT.002, 006, 007, 013, 015"},
    {"id": "P-RMT-03", "name": "Insurer OR update", "steps": "10 to 12", "trigger": "Approved batch; insurer's "
     "returned schedule", "actors": "Remittance Processor, Insurer", "outcome": "Invoices with the insurer OR; "
     "exceptions listed", "controls": "BR-RMT-10, BR-RMT-11", "frs": "FRID.RMT.007, 008"},
    {"id": "P-RMT-04", "name": "Hold and special remittance", "steps": "2, 3", "trigger": "Marketing request",
     "actors": "Marketing Collection, Marketing Team Leader, Remittance Team Leader",
     "outcome": "Account held, released or remitted at once", "controls": "BR-RMT-01, BR-RMT-08",
     "frs": "FRID.RMT.010, 011"},
]
STORYBOARDS = [
    {"id": "SB-RMT-01", "title": "Evening extraction to approved batch", "data": "insurer Alpha Insurance (code "
     "ALPHA), three invoices paid in cash, one by a check deposited yesterday",
     "summary": "The scheduled run extracts the eligible accounts, the processor reviews the batch and excludes one, "
                "the Team Leader approves and Disbursement receives the payment request.",
     "steps": [("System", "Extraction", "The evening run checks the four invoices", "Three extracted; the check "
                "payment Unextracted - due: check within the holding period"),
               ("Remittance Processor", "Remittance Workbench", "Opens the notice of the run", "Batch ALPHA00000001 "
                "Normal - Peso with 3 accounts"),
               ("Remittance Processor", "Remittance Batch", "Excludes one account with a reason, previews",
                "2 accounts and their totals; the exclusion in the history"),
               ("Remittance Processor", "Remittance Batch", "Submits the batch", "For Approval; the Team Leader is "
                "told"),
               ("Remittance Team Leader", "Remittance Batch", "Approves", "Approved; payment request in the "
                "Disbursement queue")]},
    {"id": "SB-RMT-02", "title": "Schedule to the insurer and OR upload", "data": "the approved batch of SB-RMT-01",
     "summary": "The schedule is sent to the insurer, who returns it with its ORs; the upload updates the invoices "
                "and lists one exception.",
     "steps": [("Marketing Collection", "Remittance Batch", "Asks to send the schedule", "Request recorded"),
               ("Remittance Processor", "Remittance Batch", "Sends the schedule", "Schedule sent with date and "
                "recipient"),
               ("Remittance Processor", "Insurer OR Upload", "Uploads the returned file", "2 lines read; 1 updated; "
                "1 Amount mismatch"),
               ("Remittance Processor", "Insurer OR Upload", "Opens the exception report", "The mismatch with OR "
                "amount and paid PR")]},
    {"id": "SB-RMT-03", "title": "Hold requested and released", "data": "an invoice of the Cebu marketing unit",
     "summary": "Marketing holds an account; it is excluded from extraction until released.",
     "steps": [("Marketing Collection", "Remittance Holds", "Requests a hold until 30-Nov with a reason",
                "For Approval"),
               ("Marketing Team Leader", "Remittance Holds", "Approves", "Active; assigned to a processor"),
               ("System", "Extraction", "The evening run", "Invoice Unextracted - due: account on hold"),
               ("Marketing Collection", "Remittance Holds", "Releases the hold", "Released; extracted by the next "
                "run")]},
]

# ================================================================================================ RNO
REPORTS = [
    ("RPT-RMT-01", "Remittance Tracker", "FRID.RMT.014.01", "Period, insurer, type", "Insurer, type, batch, accounts "
     "extracted, net due, incentive, due date", "Excel, PDF"),
    ("RPT-RMT-02", "Special Remittance Register", "FRID.RMT.014.01", "Period", "Invoice, policy, insurer, assured, "
     "reason, processor, requester, segment, dates, ages", "Excel, PDF"),
    ("RPT-RMT-03", "Remittance Schedule Normal", "FRID.RMT.014.01", "Batch or period", "Columns of IF-RMT-01 with "
     "Mall Assurance", "Excel, PDF"),
    ("RPT-RMT-04", "Remittance Schedule Special", "FRID.RMT.014.01", "Batch or period", "As Normal without Mall "
     "Assurance", "Excel, PDF"),
    ("RPT-RMT-05", "DTIP Status Summary", "FRID.RMT.014.01", "As of date", "Insurer, accounts, amount, remittance "
     "status", "Excel, PDF"),
    ("RPT-RMT-06", "DTIP Status Detailed", "FRID.RMT.014.01", "As of date, insurer", "Invoice, assured, dates, "
     "premium, AR paid, for remittance, commission, ages, outstanding DTIP", "Excel, PDF"),
    ("RPT-RMT-07", "Remittance Schedule with Incentives", "FRID.RMT.014.01", "Batch or period", "Schedule columns "
     "with incentive expiry, basic premium, incentive", "Excel, PDF"),
    ("RPT-RMT-08", "List of Remitted Accounts with Batch Number", "FRID.RMT.014.01", "Period", "Layout to give "
     "(OQ42)", "Excel, PDF"),
    ("RPT-RMT-09", "Paid AR over DTIP", "FRID.RMT.005.03", "Run", "Invoice, insurer, paid AR, DTIP", "Excel"),
    ("RPT-RMT-10", "Insurer OR Exception Report", "FRID.RMT.008.03", "Upload", "Reference, OR amount, paid PR, "
     "status, reason, summary", "Excel, PDF"),
    ("RPT-RMT-11", "Excluded Accounts", "FRID.RMT.002.04", "Period, batch", "Invoice, reason, user, time", "Excel"),
    ("RPT-RMT-12", "Remittance Holds", "FRID.RMT.010", "Period, stage", "Reference, invoice, reason, hold-until, "
     "stage", "Excel"),
]
REPORT_NOTE = ("Reports run on screen from the Report Centre and export to Excel and PDF; each run is archived. "
               "Names follow BR-RMT-05 for the schedules and <Report Name>_<MMDDYYYY> for the others (BR-RMT-15).")
DOCUMENTS = [
    ("DO-01", "Remittance extract, Excel", "{date}{insurer}{settlement}_{type}.xls, Excel 97-2003 in the IF-RMT-01 "
     "layout; pattern set on Remittance Settings", "File repository and shared folder per type; batch",
     "FRID.RMT.001.03, 001.04"),
    ("DO-02", "Remittance extract, text", "AM01 + YYMMDD + insurer code + settlement number .txt, header (H) and "
     "detail (D) records with control totals (IF-RMT-02)", "File repository and shared folder per type; batch",
     "FRID.RMT.001.09"),
    ("DO-03", "Remittance schedule", "PDF and Excel on the batch", "Batch", "FRID.RMT.007.01"),
    ("DO-04", "Payment request", "PDF on the batch, to Disbursement", "Batch", "FRID.RMT.007.01"),
    ("DO-05", "Exception report of an OR upload", "Excel and PDF on the upload run", "Upload run",
     "FRID.RMT.008.03"),
]
EXPORTS = [
    ("Batch lines", "Excel", "Remittance Batch", "Lines with their amounts and exclusion state"),
    ("DTIP status", "Excel", "DTIP Status", "Invoices with premium due to insurers"),
    ("Extraction tags", "Excel", "Extraction", "Accounts checked by a run with tag and reason"),
    ("Extract files and deliveries", "Excel (.xls), text", "Remittance Batch, Extract Files",
     "Download of the extract files of the batch; each delivery to the shared folder with its result; resend"),
]
NOTICES = [
    ("NT-01", "Extraction run ended", "Remittance team", "Run <run>: <n> batches, <m> accounts extracted, <k> not "
     "extracted", "Bell"),
    ("NT-02", "Batch submitted", "Remittance Team Leaders", "Batch <batch> of <insurer> is for approval", "Bell"),
    ("NT-03", "Batch approved or returned", "Processor", "Batch <batch> is <stage>", "Bell"),
    ("NT-04", "Batch fully remitted", "Processor", "Batch <batch> is fully remitted", "Bell"),
    ("NT-05", "Hold decided", "Requester", "Hold <reference> is <stage>", "Bell"),
    ("NT-06", "Special remittance decided", "Requester", "Special remittance <reference> is <stage>", "Bell"),
    ("NT-07", "Pending negative adjustment", "Processor", "Invoice <invoice> of batch <batch> has a pending negative "
     "adjustment", "Bell"),
    ("AL-01", "Extraction run failed", "Remittance Team Leaders", "The extraction of <date> failed: <reason>",
     "Alert"),
    ("AL-02", "Shared folder copy failed (REMIT_SHARED_FOLDER_FAILED)", "Remittance Team Leaders", "File <file> of "
     "batch <batch> could not be copied to <folder>: <reason>. Resend from the batch.", "Alert"),
    ("NT-08", "Settings change to approve", "Remittance Team Leaders", "A change of the remittance settings waits "
     "for your approval", "Bell"),
]
JOBS = [
    ("REMITTANCE_EXTRACTION", "Working days 20:00 (schedule set on Remittance Settings)", "Checks every invoice with "
     "premium due, tags it and creates the batches with their files; processes the end-of-day queue.", "AL-01"),
    ("REMITTANCE_SCHEDULE", "Every 15 minutes, Manila time", "Starts the extraction at the run days and time of the "
     "approved Remittance Settings (off by default) and copies the files to the shared folder.", "AL-01, AL-02"),
    ("HOLD_EXPIRY", "Daily 08:15", "Releases the holds whose hold-until date has passed.", "-"),
]


# ================================================================================================ ICD
# Layout of the Excel extract (sheet RemittanceExtractNormalPeso_Rep of both specimens): column position (A = 1),
# header, type, content. Columns 7-9 (G to I) and 11-12 (K, L) are merged cells (one field each).
XLS_COLUMNS = [
    ("1", "Invoice Number", "Text, 8 digits", "Invoice number with leading zeros"),
    ("2", "Policy Number", "Text", "Insurer's policy number"),
    ("3", "Endorsement Number", "Text", "Endorsement number; empty for the policy"),
    ("4", "With COI", "Text", "Yes or No"),
    ("5", "Name of Assured", "Text", "Assured as on the invoice"),
    ("6", "Product Line", "Text, 5", "Product line code (for example NPFIR)"),
    ("7-9", "Record Status", "Text", "Status of the record (values Q-RM-06)"),
    ("10", "Date Last Paid", "Date M/D/YYYY", "Date of the last payment"),
    ("11-12", "Date Applied", "Date M/D/YYYY", "Date the payment was applied (Annex III: Date Reversed)"),
    ("13", "Date of Inception", "Date M/D/YYYY", "Inception of the cover"),
    ("14", "Date Booked", "Date M/D/YYYY", "Booking date"),
    ("15", "Date of Expiry", "Date M/D/YYYY", "Expiry of the cover"),
    ("16", "OR Delivery", "Text", "OR delivery instruction (values Q-RM-06)"),
    ("17", "Payment Status", "Text", "Payment status of the invoice"),
    ("18", "Paid AR", "Amount #,##0.00", "Premium collected and applied"),
    ("19", "Realized Commission", "Amount", "Commission realised on the paid AR"),
    ("20", "Realized VAT", "Amount", "VAT on the realised commission"),
    ("21", "WTAX", "Amount", "Withholding tax on the commission"),
    ("22", "DTIP", "Amount", "Due to insurance provider outstanding"),
    ("23", "Commission Receivable", "Amount", "Commission still receivable"),
    ("24", "Net Due", "Amount", "Paid AR - Realized Commission - Realized VAT + WTAX (BR-RMT-09)"),
    ("25", "Mall Assurance Mode of Payment", "Amount in the specimen", "Mode of payment (Q-RM-05)"),
    ("26", "Mall Assurance Service Charge", "Amount", "Mall Assurance service charge"),
    ("27", "Mall Assurance Credit / Debit Card Charge", "Amount", "Card charge"),
    ("28", "Mall Assurance Total Deductions", "Amount", "Total of the Mall Assurance deductions"),
    ("29", "Mall Assurance VAT", "Amount", "VAT on the deductions"),
    ("30", "Mall Assurance WTAX", "Amount", "Withholding on the deductions"),
    ("31", "Net Due After Mall Assurance", "Amount", "Net Due - Total Deductions"),
    ("32", "Amount Due for Remittance", "Amount", "Amount to remit (0.00 in both specimens; Q-RM-07)"),
    ("33", "Date of Official Receipt", "Date", "Insurer OR date, filled by the insurer"),
    ("34", "Official Receipt Number", "Text", "Insurer OR number, filled by the insurer"),
    ("35", "OR Amount", "Amount", "Insurer OR amount, filled by the insurer"),
    ("36", "Extracted Date", "Date M/D/YYYY", "Date of the extraction"),
    ("37", "Batch Number", "Text", "Insurer code + 8-digit settlement number"),
    ("38", "Processor", "Text", "User ID of the processor of the batch"),
]
XLS_BLOCKS = [
    ("1", "Columns 9 to 11 (merged, rows 1-4)", "Run Date : M/D/YYYY, Run Time : h:mm AM/PM, Run By, Page : n of n"),
    ("2", "Columns 1 to 7 (merged)", "Company name, subsidiary line, address and VAT registration TIN of BDOI"),
    ("4", "Columns 1 to 7 (merged, rows 4-5)", "REMITTANCE EXTRACT NORMAL PESO: <batch number>"),
    ("7", "Columns 1 to 7 (merged)", "DATE EXTRACTED: M/D/YYYY"),
    ("9", "Columns 1 to 38", "Column headers (35 fields, table above)"),
    ("10 to n", "Columns 1 to 38", "One row per account"),
    ("n+1", "Columns 18 to 35", "Total row: sums of the amount columns, no label"),
    ("n+3", "Columns 1, 2", "Number of Accounts: <count>"),
    ("n+5 to n+9", "Column 5 (label), 6 (amount)", "Total Remittance; Less: Mall Assurance Deductions; Less: Mall Assurance "
     "VAT; Less: Mall Assurance WTAX; Grand Total After Mall Assurance Charges"),
    ("n+11", "Columns 1, 4", "Printed By: and Checked By: (signature lines)"),
]
TXT_HEADER = [
    ("1", "Record type", "H", "Fixed"),
    ("2", "Mode", "M", "Meaning to confirm (Q-RM-02)"),
    ("3", "Extraction date", "YYYYMMDD", "Date of the extract; the file name carries it as YYMMDD"),
    ("4", "Sequence or currency", "01", "Meaning to confirm (Q-RM-02)"),
    ("5", "Number of detail records", "Integer", "Count of D records (control)"),
    ("6", "Total Paid AR", "Decimal, 2 places", "Sum of detail field 11 (control)"),
    ("7", "Total Realized Commission", "Decimal, 2 places", "Sum of detail field 12"),
    ("8", "Total Realized VAT", "Decimal, 2 places", "Sum of detail field 13"),
    ("9", "Total WTAX", "Decimal, 4 places", "Sum of detail field 14"),
    ("10", "Total DTIP", "Decimal", "Sum of detail field 15"),
    ("11", "Total Net Due", "Decimal, 4 places", "Sum of detail field 16"),
    ("12-14", "Further totals", "Decimal", "Sums of detail fields 17 to 19 (meaning Q-RM-02)"),
    ("15", "Total Mall Assurance deductions", "Decimal, 2 places", "Sum of detail field 20 (to confirm)"),
    ("16", "Total amount due", "Decimal, 2 places", "Sum of detail field 21"),
]
TXT_DETAIL = [
    ("1", "Record type", "D", "Fixed"),
    ("2", "Invoice Number", "Text, 8 digits", "As column 1 of the Excel extract"),
    ("3", "Policy Number", "Text", "Column 2"),
    ("4", "Endorsement Number", "Text", "Column 3; may be empty"),
    ("5", "Invoice reference", "Text, 10 digits", "Invoice number padded to 10 digits (meaning Q-RM-02)"),
    ("6", "Name of Assured", "Text", "Column 5"),
    ("7", "Product Line", "Text, 5", "Column 6"),
    ("8", "Record Status", "Text", "Empty in the specimen (to confirm)"),
    ("9", "Date of Inception", "MM/DD/YYYY", "Column 13"),
    ("10", "Date of Expiry", "MM/DD/YYYY", "Column 15"),
    ("11", "Paid AR", "Decimal, 2 places", "Column 18"),
    ("12", "Realized Commission", "Decimal, 2 places", "Column 19"),
    ("13", "Realized VAT", "Decimal, 2 places", "Column 20"),
    ("14", "WTAX", "Decimal, 4 places", "Column 21"),
    ("15", "DTIP", "Decimal", "Column 22"),
    ("16", "Net Due", "Decimal, 4 places", "Column 24"),
    ("17-19", "Amounts", "Decimal", "0 in the specimen (Commission Receivable and Mall Assurance; Q-RM-02)"),
    ("20", "Mall Assurance Total Deductions", "Decimal, 2 places", "0.00 in the specimen (to confirm)"),
    ("21", "Amount due", "Decimal, 2 places", "Net Due after deductions"),
    ("22", "With COI", "Y or N", "Column 4"),
    ("23-26", "OR fields", "Text", "Empty: OR date, OR number, OR amount filled on return (to confirm)"),
    ("27", "Flag", "Y or N", "Meaning to confirm (Q-RM-02)"),
]
NAMING = [
    ("Excel extract, Normal - Peso", "20240729FIRSTL00003974_NormalPeso.xls", "YYYYMMDD (extraction date) + insurer "
     "code (FIRSTL, 6 letters) + settlement number (8 digits) + _NormalPeso + .xls"),
    ("Excel extract, Normal - Dollar", "20250214PISC00004625_NormalDollar.xls", "YYYYMMDD + insurer code (PISC, 4 "
     "letters) + settlement number + _NormalDollar + .xls"),
    ("Text extract", "AM01260317PGAI00005787.txt", "AM01 (meaning Q-RM-01) + YYMMDD + insurer code (PGAI) + "
     "settlement number + .txt; no type suffix"),
    ("Rule of BDOI's FRS (FRID.RMT.001.04)", "20240731SICI00003992_NormalPeso.xls and .txt", "YYYYMMDD + insurer "
     "code + settlement number + _NormalDollar or _NormalPeso; the insurer code and settlement number also appear on "
     "the insurer's OR"),
]


def write_icd(d, tmp) -> None:
    """Body of the Interface Specification v1.0 of Operations Remittance."""
    d.h1("Introduction")
    d.para("This document specifies the files and messages Operations Remittance exchanges with the insurers, with "
           "BDOI's shared drive and with the other BIBS modules. The layouts of the extract files are read from the "
           "three specimen files BDOI gave with its FRS: a Normal - Peso Excel extract, a Normal - Dollar Excel "
           "extract and a text extract. Only their structure is used; every value shown in this document is "
           "invented.")
    d.table(["ID", "Interface", "Direction", "Means", "Frequency", "Content"], [
        ["IF-RMT-01", "Remittance extract, Excel", "BIBS to insurer and shared drive", "File", "Each batch",
         "Accounts of a batch with totals"],
        ["IF-RMT-02", "Remittance extract, text", "BIBS to insurer and shared drive", "Pipe-delimited file",
         "Each batch", "Header with control totals; one record per account"],
        ["IF-RMT-03", "Insurer OR return", "Insurer to BIBS", "Upload of the returned file", "Each batch",
         "OR date, number and amount per account"],
        ["IF-RMT-04", "Shared folder", "BIBS to BDOI shared drive", "File copy", "Each batch", "IF-RMT-01 and 02 per "
         "type folder"],
        ["IF-RMT-05", "Schedule e-mail", "BIBS to insurer", "E-mail (CCM)", "On request", "Schedule attached"],
        ["IF-RMT-06", "Module hand-offs", "BIBS modules", "In the system", "Continuous", "Paid premium, payment "
         "request, payment status, claims confirmation"]],
        [1.1, 2.0, 1.8, 1.5, 1.1, 1.7], caption="Interfaces")
    d.h1("File Naming")
    d.table(["File", "Name in the specimen or the rule", "How the name is made"], NAMING, [2.2, 3.0, 4.0],
            caption="Names of the specimen files and the rule of BDOI's FRS")
    d.para("Proposed rule for BIBS (BR-RMT-05, Q-RM-01): YYYYMMDD of the extraction + insurer code + 8-digit "
           "settlement number + _NormalPeso, _NormalDollar or _WithIncentives, with .xls for the Excel file and .txt "
           "for the text file. The insurer code has no fixed length (4 and 6 letters in the specimens), so the "
           "settlement number is read as the last 8 digits before the underscore.")
    d.h1("IF-RMT-01 Remittance Extract, Excel")
    d.kv([("Format", "Excel 97-2003 workbook (.xls), one sheet named RemittanceExtractNormalPeso_Rep in both "
                     "specimens (Q-RM-03, Q-RM-06)"),
          ("Structure", "Report layout: heading block, header row 9, one row per account, a total row and closing "
                        "lines. Columns 7 to 9 and 11 to 12 are merged and hold one field each."),
          ("Amounts", "Number format #,##0.00 with negatives in brackets; the currency of the batch (PHP for "
                      "Normal - Peso, USD for Normal - Dollar)"),
          ("Dates", "Text M/D/YYYY"),
          ("Control", "Total row = sums of the amount columns; Number of Accounts = count of the rows; Grand Total = "
                      "Total Remittance less the Mall Assurance lines")], caption="File characteristics")
    d.table(["Row", "Cells", "Content"], XLS_BLOCKS, [1.4, 2.4, 5.4], caption="Rows of the Excel extract")
    d.table(["Column", "Header", "Type", "Content"], XLS_COLUMNS, [1.0, 2.8, 1.9, 3.5],
            caption="Columns of the Excel extract (35 fields)")
    d.para("Invented example of one account row (columns 1 to 6, 18 to 24, 36, 37): 00012345, FI-HO-26-0000123-00, "
           "(blank), No, ALPHA TRADING CORPORATION, NPFIR; 50,000.00, 10,000.00, 1,200.00, 1,500.00, 0.00, 0.00, "
           "40,300.00; 10/17/2026, ALPHA00000001.")
    d.h1("IF-RMT-02 Remittance Extract, Text")
    d.kv([("Format", "Text, UTF-8 with byte order mark, lines ended by carriage return and line feed"),
          ("Delimiter", "Vertical bar (|); no quotes; empty fields kept as two bars"),
          ("Records", "One H record first, then one D record per account; no trailer record"),
          ("Amounts", "Decimal point, no thousands separator; 2 or 4 decimal places as below"),
          ("Dates", "Header YYYYMMDD; details MM/DD/YYYY"),
          ("Control", "Header field 5 = number of D records; header fields 6 to 16 = sums of detail fields 11 to 21 "
                      "(BR-RMT-10)")], caption="File characteristics")
    d.table(["No.", "Field", "Format", "Content"], TXT_HEADER, [0.8, 3.0, 2.0, 3.4],
            caption="Header record H (16 fields)")
    d.table(["No.", "Field", "Format", "Content"], TXT_DETAIL, [0.8, 3.0, 2.0, 3.4],
            caption="Detail record D (27 fields)")
    d.para("Invented example of a file with one account:")
    d.bullets(["H|M|20261017|01|1|50000.00|10000.00|1200.00|1500.0000|0|40300.0000|0|0|0|0.00|40300.00",
               "D|00012345|FI-HO-26-0000123-00||0000012345|ALPHA TRADING CORPORATION|NPFIR||10/01/2026|10/01/2027|"
               "50000.00|10000.00|1200.00|1500.0000|0|40300.0000|0|0|0|0.00|40300.00|N|||||N"])
    d.para("The specimen gives Net Due = Paid AR - Realized Commission - Realized VAT + WTAX in both formats "
           "(BR-RMT-09). The fields whose meaning is not certain are marked for confirmation (Q-RM-02).")
    d.h1("IF-RMT-03 Insurer OR Return")
    d.bullets([
        "The insurer returns the extract of a batch with the Date of Official Receipt, Official Receipt Number and OR "
        "Amount filled per account (columns 33 to 35 of IF-RMT-01, or fields 23 to 25 of IF-RMT-02, to confirm).",
        "The processor uploads it on Insurer OR Upload. The batch number is read from the file name or the heading; "
        "each account must belong to the batch and the batch must be approved.",
        "Checks: batch approved, invoice in the batch and not excluded, OR number and date present, the same OR "
        "number not recorded twice for a client, totals equal to the header (text file).",
        "Each account is processed on its own; the result lists the updated and refused accounts and the exception "
        "report compares the OR amount with the paid PR (RPT-RMT-10).",
        "Until the returned layouts are confirmed (Q-RM-15) the upload also accepts the BIBS template with the "
        "columns Batch No., Invoice No., OR No., OR Date and OR Amount."])
    d.h1("IF-RMT-04 Shared Folder and IF-RMT-05 Schedule E-mail")
    d.table(["Item", "Detail"], [
        ["Shared folder", "One folder per remittance type: Normal - Peso, Normal - Dollar, With Incentives; the two "
                          "files of each batch are copied there when the batch is created and again when it is "
                          "approved [Integration scope]."],
        ["Failure", "A failed copy is retried and raises an alert; the files stay downloadable from the batch."],
        ["E-mail", "The schedule is sent to the remittance contact of the insurer kept on the insurer record, "
                   "through BDO's e-mail service (CCM), with the send log on the batch [Integration scope]."]],
        [2.2, 7.0], caption="Shared folder and e-mail")
    d.h1("IF-RMT-06 Module Hand-offs")
    d.table(["Hand-off", "What passes", "When"], [
        ["Cashiering", "Applied and posted payments per invoice (paid AR) and check clearing", "Posting"],
        ["Adjustment", "Pending financial endorsements and negative adjustments; invoice locks", "Request"],
        ["Disbursement", "Payment request of the approved batch; payment voucher and payment status back", "Approval"],
        ["Claims", "Confirmation of an open claim for a special remittance with condition Claims", "Request"],
        ["Accounting", "Journal of the remittance posting", "Approval"]], [2.2, 5.4, 1.6], caption="Hand-offs")
    d.h1("Open Points")
    d.table(["ID", "Point", "Owner"], [[a, b, c] for a, b, c, _ in OPEN_QUESTIONS[:8]] + [
        [OPEN_QUESTIONS[14][0], OPEN_QUESTIONS[14][1], OPEN_QUESTIONS[14][2]]], [1.4, 5.8, 2.0], caption="Open points")


FRS_ANNEXES = [
    ("Annex A: Remittance File Formats", [
        "BDOI gave three specimen files with its FRS. Their layouts are specified in the Interface Specification "
        "v1.0 and summarised here; no value of the specimens is copied.",
        "The Excel extract (.xls) is a report export: a heading block, a header row, one row per account with 35 "
        "fields, a total row, the number of accounts, the remittance totals less the Mall Assurance charges and the "
        "Printed By and Checked By lines. The Normal - Peso and Normal - Dollar specimens have the same layout.",
        "The text extract (.txt) is pipe-delimited: one header record H with 16 fields holding the date, the number "
        "of records and the totals, and one detail record D with 27 fields per account. The header totals equal the "
        "sums of the detail amounts."], (["File", "Records or rows", "Fields", "Control"], [
            ["Excel extract", "Heading, header row, one row per account, totals", "35", "Total row; Number of "
             "Accounts; Grand Total"],
            ["Text extract", "1 header, n details", "16 and 27", "Record count and amount totals in the header"]],
            [2.0, 3.2, 1.4, 2.6], "Remittance file formats")),
]
