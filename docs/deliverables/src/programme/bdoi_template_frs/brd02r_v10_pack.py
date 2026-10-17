"""BRD-02 Operations Remittance v1.0: screens of the UX Design, user stories, the facts of the developer
specification and data dictionary, and the "How to read this pack" note. Read by build_bdoi_v10_packs.py (FRS, BPS,
UXD, RNO, ICD, workbooks) and by the shared writers pack_dsp, pack_ddi, pack_usr and pack_readme.

The services, screens and tables are read from the sources by tools/deliverables/api_facts.py and schema_facts.py;
this module says which belong to Remittance and gives them their business wording. The insurer-confirmed deductions
of the same screens belong to Accounting (BRD-05) and are left out.
"""

from __future__ import annotations

import api_facts as af
import bdoi_v10_common as common
import brd02r_v10_content as C
from pack_common import Column

BRD = C.BRD
NAME = C.NAME
SHORT = C.SHORT
FOLDER = "BRD-02_Operations_Remittance"
FIX_VERSION = "Drop 1"
DATE = C.DATE
DATE_US = C.DATE_US
PREPARED = "iorta TechNXT Project Team"
HEADER_RIGHT = C.HEADER_RIGHT
DOC_NO = C.DOC_NO
CLASS_FIRST_CONSTANTS = True

SET = common.make_set(C, "Remittance", {
    "FRS": "BDOI Business Unit owners (Remittance, Marketing Collection, Comptrollership)",
    "ICD": "Remittance extract files in Excel and text, insurer OR return, shared folder, e-mail to the insurer, "
           "module hand-offs.",
    "DDI": ", and the fields of the extract files."})


def file_name(key: str) -> str:
    return common.file_name(SET, BRD, key)


DSP_REVISIONS = [
    (C.DATE, "1.0", "First issue. Describes the screens, service operations, validations, rules, states, jobs, "
     "audit records and permissions of the release.", PREPARED),
    (C.DATE, "1.1", "Second issue with the release of 17 Oct 2026: Remittance Settings screen and service, extract files and deliveries, settlement numbering, the REMITTANCE_SCHEDULE job and the shared folder copy.", PREPARED),
]


def version(key: str) -> str:
    return next(v for k, _, _, v, *_ in SET if k == key)


for _k in C.DOCS:
    assert C.DOCS[_k]["file"] == file_name(_k), (C.DOCS[_k]["file"], file_name(_k))

DOCS = {
    "DSP": {"cover": "Operations Remittance: Developer Specification", "header_left": "Developer Specification",
            "doc_no": f"{DOC_NO}-DSP"},
    "README": {"cover": "Operations Remittance: How to Read this Pack", "header_left": "How to Read this Pack",
               "doc_no": f"{DOC_NO}-README"},
}

# ================================================================================================ UX screens
AV = "Available"
CH = "Available; changes of the FRS v1.0 to deliver ({})"
NEW = "Proposed screen ({})"
DEL = "Available; delivered in the release of 17 Oct 2026 ({})"
UX_SCREENS = [
    {"id": "SCR-RMT-01", "name": "Remittance Workbench", "route": "remittance", "page": "RemittanceHomePage.tsx",
     "purpose": "The work waiting by stage: batches to review, to approve, holds and special requests for decision, "
                "the last extraction run.", "access": "Remittance and Marketing personas (any remittance permission)",
     "frrn": "FRID.RMT.013", "outcome": "The user opens the list of the work he chose.", "state": AV,
     "rules": ["Counts and amounts follow the user's permissions and company.",
               "Each tile opens its list filtered on the tile."],
     "fields": [], "actions": [["Open a tile", "All", "Always", "Opens the filtered list", "List"]]},
    {"id": "SCR-RMT-02", "name": "Extraction", "route": "remittance/extraction", "page": "ExtractionPage.tsx",
     "purpose": "Extraction runs, manual extraction per insurer and type or per invoice, the end-of-day queue and the "
                "tags of each run.", "access": "Remittance Processor, Remittance Team Leader",
     "frrn": "FRID.RMT.001, 003, 004, 005", "outcome": "Batches created; tags and reasons per account.",
     "state": DEL.format("FRID.RMT.001.03, 001.04, 001.08, 001.09"),
     "rules": ["A manual extraction applies the rules of the scheduled run (BR-RMT-02).",
               "An invoice queued twice for the end of day is kept once.",
               "Each run lists its tags: Extracted, Unextracted - not yet due, Unextracted - due (reason), Returned."],
     "fields": [["Insurer", "List", "N", "All", "Active insurers", "-"],
                ["Remittance type", "List", "Y", "-", "With Incentives, Normal - Peso, Normal - Dollar", "-"],
                ["Invoice number", "Text", "Cond.", "-", "Manual extraction of one invoice", "Invoice <invoice> cannot "
                 "be extracted: <reason>"]],
     "actions": [["Extract", "Processor, Team Leader", "Type chosen", "Runs the extraction", "Run with its batches"],
                 ["Queue for end of day", "Processor", "Invoice given", "Queues the invoice", "Queued"],
                 ["Open tags", "Processor, Team Leader", "A run selected", "Lists the accounts checked", "Tags"]]},
    {"id": "SCR-RMT-03", "name": "Remittance Batches", "route": "remittance/batches", "page": "BatchesPage.tsx",
     "purpose": "Batches by stage with insurer, type, processor, accounts and amount due.",
     "access": "Remittance personas, Auditor (read)", "frrn": "FRID.RMT.006, 009", "outcome": "A batch opened.",
     "state": AV, "rules": ["Tabs by stage; filters by insurer, type, processor and batch number."],
     "fields": [["Search", "Text", "N", "-", "Batch number, insurer, invoice", "-"]],
     "actions": [["Reassign", "Team Leader", "Batch open", "Gives the batch to another processor", "Assigned"]]},
    {"id": "SCR-RMT-04", "name": "Remittance Batch", "route": "remittance/batches/:id", "page": "BatchDetailPage.tsx",
     "purpose": "One batch: its lines, exclusions, preview, documents, settlement and history.",
     "access": "Remittance personas", "frrn": "FRID.RMT.002, 006, 007, 015",
     "outcome": "Batch submitted, approved, returned or sent.", "state": DEL.format("FRID.RMT.002.02, 007.01: extract files, settlement reference, shared folder copy and resend"),
     "rules": ["Amounts are read-only; exclusion and restore while Review in Process (BR-RMT-06).",
               "The submitter cannot approve (BR-RMT-01).",
               "The schedule is sent after approval; a second sending asks for a confirmation."],
     "fields": [["Exclusion reason", "List", "Y", "-", "REMIT_EXCLUSION_REASON", "Select a reason"],
                ["Comment", "Text 500", "Cond.", "-", "Required for Others", "Enter a comment"],
                ["Return reason", "List", "Y", "-", "REMIT_RETURN_REASON", "Select a reason"]],
     "actions": [["Exclude", "Processor", "Review in Process", "Excludes the selected lines", "Totals updated"],
                 ["Restore", "Processor", "Review in Process", "Restores a line", "Totals updated"],
                 ["Preview", "Processor", "Review in Process", "Shows the batch after exclusions", "Preview"],
                 ["Submit", "Processor", "Review in Process", "Checks the account statuses", "For Approval"],
                 ["Approve", "Team Leader (not the submitter)", "For Approval", "Posts; payment request",
                  "Approved"],
                 ["Return", "Processor, Team Leader", "Open", "Frees the lines", "Returned"],
                 ["Send schedule", "Processor", "Approved", "E-mails the schedule", "Sent"],
                 ["Documents", "Remittance personas", "Always", "Opens the Excel, text, schedule and payment "
                  "request", "File"]]},
    {"id": "SCR-RMT-05", "name": "Insurer OR Upload", "route": "remittance/insurer-or", "page": "InsurerOrPage.tsx",
     "purpose": "Upload of the insurer's returned schedule, results and exception report.",
     "access": "Remittance Processor, Remittance Team Leader", "frrn": "FRID.RMT.008",
     "outcome": "Invoices updated with the insurer OR.", "state": DEL.format("FRID.RMT.008.01: Excel and text OR return with control totals"),
     "rules": ["Each account is processed on its own; refused accounts are listed with the reason.",
               "The same OR number is not recorded twice for a client."],
     "fields": [["File", "File", "Y", "-", "Excel or text of IF-RMT-03; at most 10 MB", "The file must have the "
                 "columns <columns>"]],
     "actions": [["Upload", "Processor", "File chosen", "Reads and checks the file", "Run with results"],
                 ["Exception report", "Processor", "Run open", "Downloads the report", "Excel, PDF"],
                 ["Template", "Processor", "Always", "Downloads the upload template", "Excel"]]},
    {"id": "SCR-RMT-06", "name": "Remittance Holds", "route": "remittance/holds", "page": "HoldsPage.tsx",
     "purpose": "Hold requests by stage; new request, bulk upload.", "access": "Marketing Collection, Marketing "
     "Team Leader, Remittance personas (read)", "frrn": "FRID.RMT.010", "outcome": "Hold requested.", "state": AV,
     "rules": ["One active hold per invoice."],
     "fields": [["Invoice number", "Text", "Y", "-", "Invoice with premium due", "Invoice <invoice> already has an "
                 "active hold"], ["Reason", "List", "Y", "-", "HOLD_REASON", "-"],
                ["Hold until", "Date", "Y", "-", "Later than today", "The hold-until date must be a later date"]],
     "actions": [["New hold", "Marketing Collection", "Always", "Saves the request", "Draft"],
                 ["Upload", "Marketing Collection", "Always", "Loads several requests", "Drafts"]]},
    {"id": "SCR-RMT-07", "name": "Remittance Hold", "route": "remittance/holds/:id", "page": "HoldDetailPage.tsx",
     "purpose": "One hold request with its decisions, extension, cancellation and release.",
     "access": "As Remittance Holds", "frrn": "FRID.RMT.010", "outcome": "Hold approved, extended, cancelled or "
     "released.", "state": AV, "rules": ["The requester cannot approve (BR-RMT-01)."], "fields": [],
     "actions": [["Submit", "Requester", "Draft", "Sends for approval", "For Approval"],
                 ["Approve / Reject", "Marketing Team Leader", "For Approval", "Decides", "Active / Rejected"],
                 ["Extend", "Requester", "Active", "New hold-until date", "Extension For Approval"],
                 ["Cancel", "Requester", "Active", "Asks to cancel", "Cancel For Approval"],
                 ["Release", "Requester", "Active", "Removes the hold", "Released"],
                 ["Assign", "Marketing Team Leader", "Active", "Gives the hold to a processor", "Assigned"]]},
    {"id": "SCR-RMT-08", "name": "Special Remittance", "route": "remittance/special", "page": "SpecialPage.tsx",
     "purpose": "Special remittance requests; new request and upload.", "access": "Marketing Collection, "
     "Remittance personas", "frrn": "FRID.RMT.011, 009.06", "outcome": "Request saved.", "state": AV,
     "rules": ["The condition is checked when the request is saved."],
     "fields": [["Invoice number", "Text", "Y", "-", "Invoice with premium due", "-"],
                ["Condition", "List", "Y", "-", "Claims, Renewal, Installment due, Immediate OR", "Invoice <invoice> "
                 "does not meet the condition <condition>"]],
     "actions": [["New request", "Marketing Collection", "Always", "Saves the request", "Requested"]]},
    {"id": "SCR-RMT-09", "name": "Special Remittance Request", "route": "remittance/special/:id",
     "page": "SpecialDetailPage.tsx", "purpose": "One request with its decision and batch.", "access": "As "
     "Special Remittance", "frrn": "FRID.RMT.011", "outcome": "Request approved or rejected.", "state": AV,
     "rules": ["The approver is a Remittance Team Leader other than the requester."], "fields": [],
     "actions": [["Approve", "Remittance Team Leader", "For Approval", "Creates the Special batch",
                  "In Process Remittance"], ["Reject", "Remittance Team Leader", "For Approval", "With a reason",
                                             "Rejected"]]},
    {"id": "SCR-RMT-10", "name": "DTIP Status", "route": "remittance/dtip", "page": "DtipStatusPage.tsx",
     "purpose": "Premium due to insurers per invoice with its remittance status; search.", "access": "Remittance "
     "personas, Comptrollership", "frrn": "FRID.RMT.009, 016", "outcome": "Invoice found.", "state": AV,
     "rules": ["Search by invoice, batch, endorsement reference, policy or assured, partial, whatever the case."],
     "fields": [["Search", "Text", "N", "-", "At least 3 characters", "-"],
                ["Insurer", "List", "N", "All", "-", "-"], ["Remittance status", "List", "N", "All", "-", "-"]],
     "actions": [["Open invoice", "All", "Row chosen", "Opens the invoice view", "Invoice view"]]},
    {"id": "SCR-RMT-11", "name": "Incentive Rules", "route": "remittance/incentive-rules",
     "page": "IncentiveRulesPage.tsx", "purpose": "Early remittance incentive rules.", "access": "Remittance Team "
     "Leader", "frrn": "FRID.RMT.012", "outcome": "Rule saved.", "state": AV,
     "rules": ["Rules of the same insurer and line may not overlap."],
     "fields": [["Insurer", "List", "Y", "-", "-", "-"], ["Line", "List", "N", "All", "-", "-"],
                ["Rate", "Percent", "Y", "-", "0 to 100", "-"], ["Window (days)", "Number", "Y", "30", "1 to 365",
                                                                 "-"],
                ["Effective from / to", "Date", "Y", "-", "From before to", "The rule overlaps another rule of the "
                 "same insurer and line"]],
     "actions": [["Save", "Remittance Team Leader", "Always", "Saves the rule", "Rule"]]},
    {"id": "SCR-RMT-12", "name": "Report Centre (Remittance)", "route": "reports", "page": None,
     "purpose": "The remittance reports of Annex III and the control reports.", "access": "Report permission",
     "frrn": "FRID.RMT.014", "outcome": "Report run and exported.", "state": DEL.format("FRID.RMT.014.01, 014.02: Mall Assurance columns, remitted batch by settlement reference"),
     "rules": ["Each run is archived; names follow BR-RMT-05 and BR-RMT-15."],
     "fields": [["Report", "List", "Y", "-", "Remittance category", "-"], ["Period", "Dates", "Cond.", "-",
                                                                         "From not after to", "-"]],
     "actions": [["Run", "Report users", "Parameters given", "Runs on screen", "Report"],
                 ["Export", "Report users", "Report run", "Excel or PDF", "File"]]},
    {"id": "SCR-RMT-13", "name": "Remittance Settings", "route": "remittance/settings", "page": "RemittanceSettingsPage.tsx",
     "purpose": "Extraction schedule (days and time), output folders per type and file name pattern.",
     "access": "Remittance Team Leader; approved by another user", "frrn": "FRID.RMT.003.01, 001.04, 001.05",
     "outcome": "Settings applied after approval.", "state": DEL.format("FRID.RMT.003.01"),
     "rules": ["A change applies after another user approves it.", "The schedule keeps the run off-peak."],
     "fields": [["Run days", "Days", "Y", "Working days", "-", "-"], ["Run time", "Time", "Y", "20:00",
                                                                     "18:00 to 05:00", "-"],
                ["Folder per type", "Text", "Y", "-", "Shared drive path", "-"],
                ["File name pattern", "Text", "Y", "{date}{batchNo}_{type}", "Tokens", "-"]],
     "actions": [["Save", "Remittance Team Leader", "Always", "Saves for approval", "For Approval"],
                 ["Approve", "Another Team Leader", "For Approval", "Applies the settings", "Applied"]]},
]
SCREENS_BY_ID = {s["id"]: s for s in UX_SCREENS}


def screens_by_id() -> dict[str, dict]:
    return SCREENS_BY_ID


# ================================================================================================ developer spec
PACKAGES = ["remittance"]
MESSAGE_PACKAGES = ["remittance"]
SETTING_CLASSES = ("SystemParameter",)
CONTROLLERS = ["remittance/api/" + c for c in (
    "HoldController.java", "RemittanceBatchController.java", "RemittanceController.java",
    "RemittanceTemplateController.java", "SpecialRemittanceController.java", "RemittanceSettingsController.java")]
SHARED_CONTROLLERS = ["report/api/ReportController.java"]  # read only when a screen calls it
OMIT_OPS: set[str] = set()
DSP_TEXT = {
    "requirement_ids": "FRID.RMT, BR-RMT",
    "screen_ids": "SCR-RMT",
    "interfaces": "remittance extract files, insurer OR return, shared folder, e-mail, module hand-offs",
    "integration_prefix": "",
    "service_ops": "The layouts of the extract files are detailed in the Interface Specification.",
    "status_note": "Batches, holds and special remittance requests follow the status models of the data dictionary "
                   "(sheet Status models).",
    "jobs_note": "The extraction can also be started from Extraction.",
    "audit_note": "Each batch also shows its own history (action, user, time) on its History tab.",
    "security_note": "The insurer-confirmed deductions shown on the batch belong to Accounting (BRD-05).",
    "omit_classes": ("Seed",),
    "replace": {},
}
AREAS = [
    ("Extraction", ("remittance/runs", "remittance/eod-requests"), "Runs, manual extraction, end-of-day queue, tags."),
    ("Batches", ("remittance/batches",), "Batches, exclusion, submission, approval, documents, sending."),
    ("Insurer OR and templates", ("remittance/insurer-or", "remittance/templates"), "Insurer OR upload, runs, "
     "upload templates."),
    ("Holds", ("remittance/holds",), "Hold requests and decisions."),
    ("Special remittance", ("remittance/special",), "Special remittance requests and decisions."),
    ("Inquiry and incentives", ("remittance/accounts", "remittance/dtip", "remittance/incentive-rules"),
     "Account search, DTIP status, incentive rules."),
]
COMPONENTS = [
    ("Remittance screens", f"{sum(1 for s in UX_SCREENS if s['page'])} screens under Operations › Remittance and "
     "the Report Centre.", "Section 3"),
    ("Remittance service", "Extraction, eligibility, batches, documents, insurer OR, holds, special remittance, "
     "incentives.", "Section 4"),
    ("Extract files", "Excel and text files per batch, shared folder per type.", "Interface Specification v1.0"),
    ("Audit trail", "Insert-only record of every action.", "Section 8"),
    ("Scheduled jobs", "Remittance extraction, hold expiry.", "Section 7"),
]

common.clean_operations()
_OPS = af.operations(files=[af.JAVA / c for c in CONTROLLERS])


def _screens() -> list[dict]:
    import code_facts as cf  # noqa: PLC0415
    menu = {sc.path.lstrip("/"): sc for _, _, sc in cf.all_screens()}
    out = []
    for s in UX_SCREENS:
        if not s["page"]:
            continue
        out.append({"id": s["id"], "name": s["name"], "route": s["route"], "uxd": s["id"], "frum": s["frrn"],
                    "page": ("remittance", s["page"]), "loads": s["purpose"], "states": s["rules"][:4] or ["-"],
                    "fields": [], "codes": [], "calls": common.screen_calls("remittance", s["page"], _OPS)})
        if s["route"] not in menu:
            out[-1]["permission"] = "REMIT_PROCESS (or another remittance permission)"
    return out


SCREENS = _screens()
UX_INPUTS: dict[str, str] = {}
ACTION_LABELS = {"extract": "Extract", "queueEod": "Queue for end of day", "exclude": "Exclude", "restore": "Restore",
                 "submit": "Submit", "approve": "Approve", "returnBatch": "Return", "assign": "Reassign",
                 "sendSchedule": "Send schedule", "uploadInsurerOr": "Upload", "template": "Template",
                 "reject": "Reject", "release": "Release", "extend": "Extend", "cancel": "Cancel",
                 "document": "Documents"}
DATA_LABELS = {"runs": "Runs", "tags": "Tags of a run", "batches": "Batches", "batch": "The batch",
               "preview": "Preview", "dtip": "DTIP status", "accounts": "Accounts"}
PERMISSION_PREFIXES = ("REMIT_", "HOLD_", "SPECIAL_REMIT")
PERSONA_ROLES = [("Remittance Processor", "REMIT_PROCESSOR"), ("Remittance Team Leader", "REMIT_TL"),
                 ("Marketing Collection", "MKT_COLLECTION"), ("Marketing Team Leader", "MKT_TL"),
                 ("Auditor", "AUDITOR")]
RULE_ENFORCEMENT = [
    ("BR-RMT-01", "Approve hidden from the submitter.", "REMIT_FOUR_EYES; HOLD_FOUR_EYES; SPECIAL_REMIT_FOUR_EYES.",
     "Submitter and approver recorded on the batch."),
    ("BR-RMT-02", "Tags with reasons on Extraction.", "REMIT_INVOICE_NOT_EXTRACTABLE.", "Eligibility of the run; "
     "list of exclusion reasons."),
    ("BR-RMT-03", "Reason Paid AR greater than DTIP.", "-", "Setting REMIT_PAIDAR_OVER_DTIP_MODE."),
    ("BR-RMT-04", "Reason Check within the holding period.", "-", "Setting REMIT_CHECK_HOLD_DAYS; holiday calendar."),
    ("BR-RMT-05", "Batch number on every screen.", "-", "Number series of the batches; setting REMIT_FILE_PATTERN."),
    ("BR-RMT-06", "Amounts read-only; Exclude and Restore while under review.", "REMIT_LINE_ALREADY_EXCLUDED; "
     "REMIT_RESTORE_NOT_ELIGIBLE; REMIT_BATCH_STAGE.", "Exclusion state of each line."),
    ("BR-RMT-07", "Lock shown on the invoice view.", "REMIT_BATCH_STAGE.", "Invoice locks of the invoice ledger."),
    ("BR-RMT-08", "Hold stage shown.", "HOLD_DUPLICATE; HOLD_STAGE.", "Job HOLD_EXPIRY."),
    ("BR-RMT-09", "Totals of the extract.", "-", "Amounts of each batch line."),
    ("BR-RMT-10", "Upload result.", "FEED_FILE_COLUMNS.", "Header totals of the text file (to deliver)."),
    ("BR-RMT-11", "History tab.", "-", "Audit trail insert-only."),
    ("BR-RMT-12", "Menu and actions by permission.", "-", "Data access per company."),
    ("BR-RMT-13", "Extraction screen.", "-", "Job REMITTANCE_EXTRACTION."),
    ("BR-RMT-14", "Incentive columns.", "INCENTIVE_RULE_PERIOD.", "Incentive rules."),
    ("BR-RMT-15", "-", "-", "Report file names."),
]
WORKFLOWS = {
    "Remittance batch": ("remittance/domain/RemittanceEnums.BatchStage", [
        ("REVIEW_IN_PROCESS", "Review in Process", "Extraction", "FOR_APPROVAL, ON_HOLD, RETURNED"),
        ("ON_HOLD", "On Hold", "Hold by the processor", "REVIEW_IN_PROCESS"),
        ("FOR_APPROVAL", "For Approval", "Submit", "APPROVED, REVIEW_IN_PROCESS, RETURNED"),
        ("APPROVED", "Approved", "Approve by another user", "PARTIALLY_REMITTED, FULLY_REMITTED"),
        ("PARTIALLY_REMITTED", "Partially Remitted", "Payment voucher, DTIP left", "FULLY_REMITTED"),
        ("FULLY_REMITTED", "Fully Remitted", "Payment voucher, no DTIP left", "OR_RECEIVED"),
        ("OR_RECEIVED", "OR Received", "Insurer ORs uploaded", "-"),
        ("RETURNED", "Returned", "Return", "-")]),
    "Hold request": ("remittance/domain/RemittanceEnums.HoldStage", [
        ("DRAFT", "Draft", "New hold", "FOR_APPROVAL"), ("FOR_APPROVAL", "For Approval", "Submit", "ACTIVE, REJECTED"),
        ("ACTIVE", "Active", "Approve", "EXTENSION_FOR_APPROVAL, CANCEL_FOR_APPROVAL, RELEASED"),
        ("EXTENSION_FOR_APPROVAL", "Extension For Approval", "Extend", "ACTIVE"),
        ("CANCEL_FOR_APPROVAL", "Cancel For Approval", "Request cancel", "CANCELLED, ACTIVE"),
        ("RELEASED", "Released", "Release or expiry", "-"), ("REJECTED", "Rejected", "Reject", "-"),
        ("CANCELLED", "Cancelled", "Cancel approved", "-")]),
    "Special remittance request": ("remittance/domain/RemittanceEnums.SpecialStage", [
        ("REQUESTED", "Requested", "New request", "FOR_APPROVAL"),
        ("FOR_APPROVAL", "For Approval", "Submit", "IN_PROCESS_REMITTANCE, REJECTED, RETURNED"),
        ("IN_PROCESS_REMITTANCE", "In Process Remittance", "Approve", "PUSHED_TO_DISBURSEMENT"),
        ("PUSHED_TO_DISBURSEMENT", "Pushed to Disbursement", "Batch approved", "-"),
        ("REJECTED", "Rejected", "Reject", "-"), ("RETURNED", "Returned", "Return", "-")]),
}
STATUSES = C.STATUSES
AUDIT_LABELS = {"Package version": "Template version", "RemittanceSettings": "Remittance settings change", "RemittanceBatch": "Remittance batch",
                "RemittanceDeduction": "Remittance deduction (Accounting)", "RemittanceEodRequest": "End-of-day request",
                "RemittanceExtractionRun": "Extraction run", "RemittanceHold": "Hold request",
                "RemittanceIncentiveRule": "Incentive rule", "SpecialRemittance": "Special remittance request"}
AUDIT_OMIT: set[str] = set()
ACTION_WORDS = {"CREATE": "Created", "UPDATE": "Changed", "SUBMIT": "Submitted", "AUTHORIZE": "Approved",
                "REJECT": "Rejected or returned", "RUN": "Run", "CLOSE": "Closed"}
JOBS = {j[0]: j[2] for j in C.JOBS}
JOB_OMIT: set[str] = set()

# ================================================================================================ data dictionary
TABLE_PATTERN = r"^rem_"
TABLES = {t: ("Remittance", p, "-", "-") for t, p in {
    "rem_batch": "Remittance batches: insurer, type, currency, stage, processor, totals, documents.",
    "rem_batch_document": "Documents stored on a batch (schedule, payment request).",
    "rem_batch_line": "Accounts of a batch with their amounts, exclusion state and insurer OR.",
    "rem_file_delivery": "Each copy of an extract file to the shared folder: file, folder, result, reason, when and by whom.",
    "rem_setting_change": "Proposed changes of the remittance settings with their maker-checker decision.",
    "rem_deduction": "Insurer-confirmed deductions from a later remittance (Accounting, BRD-05).",
    "rem_deduction_application": "Deductions consumed by approved batches (Accounting, BRD-05).",
    "rem_eod_request": "Invoices queued for the end-of-day extraction.",
    "rem_extraction_run": "Extraction runs with trigger, status and counts.",
    "rem_extraction_tag": "Tag and reason of each invoice checked by a run.",
    "rem_hold_request": "Hold requests with reason, hold-until date, stage and decisions.",
    "rem_incentive_rule": "Early remittance incentive rules.",
    "rem_special_request": "Special remittance requests with condition and stage.",
}.items()}
VOLUME_NOTE = "Every table of the module; volumes follow the invoices with premium due (about 600 extracted a day)."
PARAMETER_FILTER = "param_key like 'REMIT%' or param_key = 'EARLY_INCENTIVE_WTAX_RATE'"
PARAMETER_LABELS: dict[str, str] = {}
SETTINGS_SCREEN_KEYS: list[str] = []
PARAMETER_MAKER_CHECKER = ("-", "Yes: System Parameters, approved by another user")
LOV_TYPES = ("REMITTANCE_TYPE", "REMIT_EXCLUSION_REASON", "REMIT_RETURN_REASON", "HOLD_REASON",
             "SPECIAL_REMIT_CONDITION")
ENUM_LISTS = [("Remittance type", "remittance/domain/RemittanceEnums.RemittanceType"),
              ("Extraction tag", "remittance/domain/RemittanceEnums.ExtractionTag"),
              ("Batch stage", "remittance/domain/RemittanceEnums.BatchStage"),
              ("Hold stage", "remittance/domain/RemittanceEnums.HoldStage"),
              ("Special remittance stage", "remittance/domain/RemittanceEnums.SpecialStage"),
              ("Insurer OR status", "remittance/domain/RemittanceEnums.OrStatus")]
ENUM_LABELS = {"NORMAL_PHP": "Normal - Peso", "NORMAL_USD": "Normal - Dollar", "WITH_INCENTIVES": "With Incentives",
               "UNEXTRACTED_NOT_DUE": "Unextracted - not yet due", "UNEXTRACTED_DUE": "Unextracted - due",
               "OR_RECEIVED": "OR Received", "IN_PROCESS_REMITTANCE": "In Process Remittance"}
INACTIVE_CODES: set[str] = set()
DDI_TEXT: dict[str, str] = {}


def extra_ddi_sheets(wb) -> None:
    """The fields of the extract files of the Interface Specification, read from BDOI's specimen files."""
    common.layout_sheet(wb, Column, "File Excel Extract", "Excel extract", C.XLS_COLUMNS,
                        ("Column", "Header", "Type", "Content"),
                        "IF-RMT-01: the 35 fields of the Excel remittance extract (header row 9; columns 7 to 9 and 11 to 12 "
                        "are merged cells).")
    common.layout_sheet(wb, Column, "File Text Header", "Text header", C.TXT_HEADER,
                        ("No.", "Field", "Format", "Content"),
                        "IF-RMT-02: header record H of the pipe-delimited text extract (16 fields, control totals).")
    common.layout_sheet(wb, Column, "File Text Detail", "Text detail", C.TXT_DETAIL,
                        ("No.", "Field", "Format", "Content"),
                        "IF-RMT-02: detail record D of the text extract, one per account (27 fields).")
    common.layout_sheet(wb, Column, "File Naming", "Naming", C.NAMING, ("File", "Name", "How the name is made"),
                        "Names of the specimen files and the naming rule of BDOI's FRS (FRID.RMT.001.04).")


# ================================================================================================ user stories
EPICS = [("EP-RMT-01", "Extraction and extract files"), ("EP-RMT-02", "Process Remittance"),
         ("EP-RMT-03", "Insurer schedule and OR"), ("EP-RMT-04", "Holds and special remittance"),
         ("EP-RMT-05", "Inquiry, notices and reports")]
_EPIC_OF = {"FRID.RMT.001": 1, "FRID.RMT.002": 2, "FRID.RMT.003": 1, "FRID.RMT.004": 1, "FRID.RMT.005": 1,
            "FRID.RMT.006": 2, "FRID.RMT.007": 3, "FRID.RMT.008": 3, "FRID.RMT.009": 5, "FRID.RMT.010": 4,
            "FRID.RMT.011": 4, "FRID.RMT.012": 1, "FRID.RMT.013": 5, "FRID.RMT.014": 5, "FRID.RMT.015": 2,
            "FRID.RMT.016": 5}
_P = {"P": "Remittance Processor", "T": "Remittance Team Leader", "M": "Marketing Collection user",
      "C": "Comptrollership user"}
STORIES = common.stories(C, _P, _EPIC_OF)
REPORT_AC: list = []

README = {
    "intro": ("This pack holds every document of Operations Remittance (BRD-02, Remittance section of the Operations "
              "BRD), from BDOI's FRS for Remittance. Each document has one job. A requirement or rule is stated once, "
              "in the FRS, with its ID; the other documents refer to that ID. The Operations Cashiering pack of the "
              "same BRD is in its own folder."),
    "order": ["FRS", "BPS", "UXD", "RNO", "ICD", "DSP", "DDI", "USR", "RTM"],
    "programme": [
        ("Non-Functional and Security Requirements", "Performance, availability, security, audit, retention, backup, "
         "accessibility and browsers for every BRD."),
        ("Glossary", "Terms and abbreviations used in every pack."),
        ("Decision and Observation Log", "Decisions taken and observations raised across the BRDs."),
        ("Document Register", "Every current document of the BRD packs with version, date, owner and status."),
    ],
    "ids": [("FRID.RMT.nnn.nn", "Functional requirement (FRS; BDOI's numbering)"), ("BR-RMT-nn", "Business rule (FRS)"),
            ("Q-RM-nn", "Open question to BDOI (FRS, Requirements Collection)"),
            ("SCR-RMT-nn", "Screen (UX Design)"), ("SB-RMT-nn, P-RMT-nn", "Storyboard, process (Process and "
                                                                          "Storyboards)"),
            ("RPT-RMT-nn, DO-nn, NT-nn, AL-nn", "Report, document, notice, alert (Reports, Notifications and "
             "Outputs)"), ("IF-RMT-nn", "Interface (Interface Specification)"),
            ("US-RMT-nnn", "User story (User Stories)"), ("TC-FRID-RMT-…", "Test case (Test Cases and Traceability)"),
            ("DW-RMT-nn", "Delivery work (Effort Estimate)"), ("NFR-…", "Non-functional requirement (programme NFR)")],
    "colour": "The colour code of the FRS applies to every document: black in the BRD, green OOTB, orange change "
              "request, blue best practice, grey label for Integration or Data Migration scope.",
}


def check() -> list[str]:
    problems = []
    rules = {r[0] for r in C.BUSINESS_RULES}
    frum = {r["id"] for f in C.FRUM for r in f["reqs"]}
    for s in STORIES:
        for ref in [x.strip() for x in s[6].split(",")]:
            if ref not in frum:
                problems.append(f"{s[0]}: unknown requirement {ref}")
        for ref in [x.strip() for x in s[7].split(",") if x.strip() != "-"]:
            if ref not in rules:
                problems.append(f"{s[0]}: unknown rule {ref}")
        if s[8] not in SCREENS_BY_ID:
            problems.append(f"{s[0]}: unknown screen {s[8]}")
    missing = rules - {r[0] for r in RULE_ENFORCEMENT}
    if missing:
        problems.append(f"rules without enforcement: {sorted(missing)}")
    return problems
