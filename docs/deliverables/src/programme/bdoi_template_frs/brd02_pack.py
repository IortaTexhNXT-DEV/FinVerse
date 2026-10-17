"""BRD-02 Operations Cashiering: content of the standard document set that is not already in brd02_v34_content.py,
brd02_v34_pages.py and brd02_v34_screens.py. Read by build_pack.py (developer specification DSP, data dictionary
DDI, user stories USR, the "How to read this pack" note) and by build_brd02_v34_workbooks.py (traceability).

The facts of the services, screens and tables are read from the sources by tools/deliverables/api_facts.py and
schema_facts.py; this module says which of them belong to Cashiering and gives them their business wording.
"""

from __future__ import annotations

import re

import brd02_v34_content as C
import brd02_v34_pages as PG
import brd02_v34_screens as UX

BRD = "BRD-02"
NAME = "Operations Cashiering"
SHORT = "Cashiering"
FOLDER = "BRD-02_Operations_Cashiering"
DATE = C.DATE
DATE_US = C.DATE_US
PREPARED = "iorta TechNXT Project Team"
HEADER_RIGHT = C.HEADER_RIGHT
DOC_NO = "PRJ0012475-CSH"
# Qualified permission constants (CashAccess.APPROVE) are read from their own class.
CLASS_FIRST_CONSTANTS = True

SET = [
    ("README", "Inputs", "How_to_Read_this_Pack", "1.0", "This note: the documents of the pack and who reviews them.",
     "All reviewers"),
    ("FRS", "FRS-BDOI", "Operations_Cashiering", C.VERSION, "Requirements, business rules, validations, personas and "
     "permissions, parameters and lists of values, statuses.", "BDOI Business Unit owners (Cashiering, "
     "Comptrollership)"),
    ("BPS", "BPS-BDOI", "Operations_Cashiering_Process_and_Storyboards", C.VERSION, "Journey, to-be processes and "
     "storyboards.", "BDOI Business Unit owners"),
    ("UXD", "UXD-BDOI", "Operations_Cashiering_UX_Design", C.VERSION, "Every screen: purpose, users, fields, "
     "actions, states, messages, one screenshot per page.", "BDOI Business Unit owners; UAT lead"),
    ("RNO", "RNO-BDOI", "Operations_Cashiering_Reports_Notifications_Outputs", C.VERSION, "Reports, receipts and "
     "documents, exports, notices, alerts and jobs.", "BDOI Business Unit owners; Internal Audit"),
    ("ICD", "ICD-BDOI", "Operations_Cashiering_Interface_Specification", C.ICD_VERSION, "Payment channel files, MFT, "
     "automatic matching, collector requests, module hand-offs.", "BDOI IT (integration)"),
    ("DSP", "DSP-BDOI", "Operations_Cashiering_Developer_Specification", "1.0", "Screens, service operations, field "
     "validations, rules, states, jobs, audit events and permissions, as the system works.", "BDOI IT; iorta "
     "TechNXT developers and testers"),
    ("DDI", "DDI-BDOI", "Operations_Cashiering_Data_Dictionary", "1.0", "Tables, columns, lists of values, "
     "parameters, status models and indexes.", "BDOI IT (database administration); Data Migration"),
    ("USR", "USR-BDOI", "Operations_Cashiering_User_Stories", "1.0", "User stories with acceptance criteria, ready to "
     "import into the backlog tool.", "BDOI Product Owner; UAT lead"),
    ("RTM", "RTM", "Test_Cases_and_Traceability", C.WB_VERSION, "Test cases (every acceptance criterion) and the "
     "trace BRD, requirement, user story, screen, test case.", "UAT lead; Internal Audit"),
    ("FIT", "FitGap", "BRD_vs_OOTB_vs_Best_Practice", C.WB_VERSION, "Each requirement against the BRD and the OOTB "
     "platform.", "BDOI Product Owner"),
    ("CRR", "CR", "Change_Request_Register", C.WB_VERSION, "Change requests and their status.", "BDOI Product Owner"),
    ("CRE", "CR", "Effort_Estimate", C.WB_VERSION, "Effort of the open change requests.", "BDOI Product Owner"),
    ("INP", "Inputs", "Business_Unit_Requirements_Collection", C.WB_VERSION, "Values the Business Unit is asked to "
     "give.", "BDOI Business Unit owners"),
    ("WTU", "UAT", "Business_Unit_Walkthrough_Users", C.WB_VERSION, "Accounts and script of the walkthroughs.",
     "UAT lead"),
]
EXT = {"README": "docx", "FRS": "docx", "BPS": "docx", "UXD": "docx", "RNO": "docx", "ICD": "docx", "DSP": "docx",
       "DDI": "xlsx", "USR": "xlsx", "RTM": "xlsx", "FIT": "xlsx", "CRR": "xlsx", "CRE": "xlsx", "INP": "xlsx",
       "WTU": "xlsx"}
SET.append(("ACX", "Access", "Cashiering_Access_Matrix", "1.0", "Group profile x screen x action, from the permissions, group profiles, menus and server checks of the release.", "BDOI Information Security; UAT lead"))
EXT["ACX"] = "xlsx"
SET.append(("SRX", "SRS", "Cashiering_System_Requirements_Extract", "1.2", "The chapter of the BIBS System Requirements Specification v1.2 for this module: system requirements traced to the FRS, common requirements, access and acceptance.", "BDOI Product Owner; BDOI IT"))
EXT["SRX"] = "docx"


def file_name(key: str) -> str:
    kind, title, ver = next((k2, t, v) for k, k2, t, v, *_ in SET if k == key)
    return f"BIBS_{kind}_{BRD}_{title}_v{ver}.{EXT[key]}"


def version(key: str) -> str:
    return next(v for k, _, _, v, *_ in SET if k == key)


DOCS = {
    "DSP": {"cover": "Operations Cashiering: Developer Specification", "header_left": "Developer Specification",
            "doc_no": f"{DOC_NO}-DSP"},
    "README": {"cover": "Operations Cashiering: How to Read this Pack", "header_left": "How to Read this Pack",
               "doc_no": f"{DOC_NO}-README"},
}

# ================================================================================================ developer spec
PACKAGES = ["cashiering"]
MESSAGE_PACKAGES = ["cashiering"]
SETTING_CLASSES = ("SystemParameter",)
_A = "cashiering/api/"
CONTROLLERS = [_A + c for c in (
    "CashBankAccountController.java", "CashReceiptController.java", "CashieringDashboardController.java",
    "CashieringRequestController.java", "CashieringSetupController.java", "ChannelFileController.java",
    "CheckController.java", "CwtController.java", "LegacyBatchController.java", "OtherIncomeController.java",
    "PaymentController.java", "ReceiptDocumentController.java", "ReceiptFormController.java",
    "ReceiptRecordController.java", "SearchLogController.java", "SettlementOrController.java",
    "UnappliedController.java", "UnappliedInquiryController.java")]
SHARED_CONTROLLERS = ["report/api/ReportController.java", "attachment/api/AttachmentController.java",
                      "bulk/api/BulkController.java"]
OMIT_OPS = {"cashiering/payment-files/simulator"}

DSP_TEXT = {
    "requirement_ids": "FRS.CSH, BR-CSH",
    "screen_ids": "SCR-CSH",
    "interfaces": "payment channel files, MFT intake, automatic matching, collector requests, module hand-offs",
    "integration_prefix": "",
    "service_ops": "The payment file layouts and the matching rules are detailed in the Interface Specification.",
    "status_note": "Receipt series, AR and OR forms and legacy batches follow the maker-checker statuses of the data "
                   "dictionary (sheet Status models).",
    "jobs_note": "Receive MFT Files Now on Payment Files starts CASH_MFT_INTAKE.",
    "audit_note": "Each receipt also shows its own history (action, user, time) on its History tab.",
    "security_note": "Marketing users see the unapplied payments of their marketing unit only "
                     "(CASH_UNAPPLIED_MARKETING_SCOPE).",
    "omit_classes": ("Simulator", "Seed"),
    "replace": {", or the bytes of a batch printed before ST1": ""},
}

AREAS = [
    ("Records, posting and receipts", ("cashiering/records", "cashiering/receipts", "cashiering/receipt-actions",
                                       "cashiering/dashboard", "cashiering/search-log"),
     "AR and OR records, posting, receipts, cancellation, reinstatement, dashboard, search log."),
    ("Payments and payment files", ("cashiering/payments", "cashiering/payment-files", "cashiering/pdc",
                                    "cashiering/prebooked", "cashiering/matching", "cashiering/commission-payments",
                                    "cashiering/pickups", "bulk/"),
     "Over-the-counter payments, payment files, MFT, post-dated checks, pre-booked payments, matching."),
    ("Unapplied payments and requests", ("cashiering/unapplied", "cashiering/disposition-types",
                                         "cashiering/collector-requests", "cashiering/refund-validations",
                                         "cashiering/payment-reversals", "cashiering/requests",
                                         "cashiering/legacy-batches", "cashiering/other-income-items"),
     "Unapplied payments, dispositions, collector, refund validation and reversal requests, legacy batches."),
    ("ORs, printing and BIR 2307", ("cashiering/settlement-ors", "cashiering/print-batches", "cashiering/cwt"),
     "Settlement ORs, batch printing, BIR 2307 tags and batches."),
    ("Set-up and reports", ("cashiering/series", "cashiering/receipt-forms", "cashiering/layouts",
                            "cashiering/minimal-balance", "cashiering/bank-accounts", "reports/", "attachments/"),
     "Receipt series, AR and OR forms, layouts, minimal balance, bank accounts, reports."),
]

COMPONENTS = [
    ("Cashiering screens", f"{len(UX.SCREENS)} screens under Finance › Cashiering and the Report Centre.", "Section 3"),
    ("Cashiering service", "Records, posting, numbering, application, unapplied payments, payment files, matching, "
     "printing, ORs, set-up.", "Section 4"),
    ("Payment channel intake", "Uploads and MFT folders with the channel simulator for SIT and UAT.",
     "Interface Specification v1.0"),
    ("Audit trail", "Insert-only record of every action.", "Section 8"),
    ("Scheduled jobs", "Pre-booked re-match, automatic matching, post-dated check maturity, minimal balance sweep, "
     "MFT intake.", "Section 7"),
]

_PAGES = {
    "SCR-OP-07": "CashieringHomePage.tsx", "SCR-OP-65": "RecordFormPage.tsx", "SCR-OP-66": "RecordDetailPage.tsx",
    "SCR-OP-67": "PostingListPage.tsx", "SCR-OP-08": "ReceivePaymentPage.tsx", "SCR-OP-09": "ReceiptsPage.tsx",
    "SCR-OP-10": "ReceiptDetailPage.tsx", "SCR-OP-11": "UnappliedPage.tsx", "SCR-OP-12": "UnappliedDetailPage.tsx",
    "SCR-OP-72": "UnappliedInquiryPage.tsx", "SCR-OP-70": "DayEndListPage.tsx", "SCR-OP-71": "SearchLogPage.tsx",
    "SCR-OP-13": "CashieringRequestsPage.tsx", "SCR-OP-14": "PrebookedPage.tsx", "SCR-OP-15": "PaymentUploadsPage.tsx",
    "SCR-OP-68": "PaymentFilesPage.tsx", "SCR-OP-73": "SettlementOrsPage.tsx", "SCR-OP-16": "PdcWarehousePage.tsx",
    "SCR-OP-18": "BatchPrintPage.tsx", "SCR-OP-19": "Cwt2307Page.tsx", "SCR-OP-20": "CommissionOrsPage.tsx",
    "SCR-OP-69": "ReceiptFormsPage.tsx", "SCR-OP-21": "ReceiptSeriesPage.tsx", "SCR-OP-22": "CashieringSetupPage.tsx",
    "SCR-X-INCOME": "UnappliedIncomePage.tsx", "SCR-X-PR2307": "LegacyPr2307Page.tsx",
    "SCR-X-PICKUP": "CheckPickupPage.tsx",
}
_SHARED_PAGES = {"SCR-OP-63": ("reports", "ReportCentrePage.tsx"), "SCR-OP-64": ("reports", "ReportPage.tsx")}


def _screens() -> list[dict]:
    out = []
    for s in UX.SCREENS:
        page = _SHARED_PAGES.get(s["pack_id"]) or ("cashiering", _PAGES[s["pack_id"]])
        out.append({"id": s["id"], "name": s["name"], "route": s["route"].lstrip("/"), "uxd": s["id"],
                    "frum": s["frrn"], "page": page, "loads": s["purpose"],
                    "states": s["rules"][:4] or ["-"], "fields": [], "codes": []})
    return out


SCREENS = _screens()
UX_INPUTS: dict[str, str] = {}
_DROP = {("lovApi", "options"), ("systemApi", "settings")}


def fix_call(c):
    """Calls the reader cannot resolve are dropped; the query-string suffix is not part of the path."""
    if (c.obj, c.fn) in _DROP or not c.path.lstrip("/").startswith(("cashiering", "reports", "bulk", "attachments")):
        return None  # services of other modules (catalogue, parties, system) are described in their own packs
    return c.__class__(c.obj, c.fn, c.verb, re.sub(r"(?<!/)\{x\}", "", c.path))


ACTION_LABELS = {"post": "Post", "submit": "Submit for Posting", "returnRecord": "Return", "approve": "Approve",
                 "reject": "Reject", "upload": "Upload", "export": "Download"}
DATA_LABELS = {"list": "The list", "get": "The record", "dashboard": "Dashboard figures"}
PERMISSION_PREFIXES = ("CASH_", "CWT_", "LEGACY_")

PERSONA_ROLES = [
    ("Cashier", "CASHIER"), ("Cashiering Team Leader", "CASHIER_TL"), ("Marketing Collection", "MKT_COLLECTION"),
    ("Top Management Approver", "TOP_MANAGEMENT_APPROVER"), ("Comptrollership", "COMPTROLLERSHIP"),
    ("Auditor", "AUDITOR"), ("System Administrator", "SYSADMIN"),
]

RULE_ENFORCEMENT = [
    ("BR-CSH-01", "Post, Approve and Authorise hidden from the maker.", "MAKER_CHECKER_VIOLATION; "
     "CASH_BATCH_MAKER_CHECKER.", "Maker and checker recorded on each record."),
    ("BR-CSH-02", "Receive Payment refers to Create AR/OR.", "RECEIPT_POSTING_STEP.", "CASH_POSTING_STEP_RECEIPTS."),
    ("BR-CSH-03", "Receipting Branch Head Office for an OR.", "OR_HEAD_OFFICE_ONLY; NO_HEAD_OFFICE.",
     "Series per kind and branch."),
    ("BR-CSH-04", "-", "RECEIPT_SERIES_DEPLETED.", "Next number taken under a lock at posting."),
    ("BR-CSH-05", "Application preview.", "-", "Jobs PREBOOKED_REMATCH, PAYMENT_AUTOMATCH, PDC_MATURITY."),
    ("BR-CSH-06", "Preview by component.", "-", "CASH_CHECK_BEFORE_CASH."),
    ("BR-CSH-07", "PR 2307 column for tagged accounts.", "CWT_AMOUNT_MISMATCH.", "CASH_PR2307_BASE."),
    ("BR-CSH-08", "Minimal Balance tab of Cashiering Setup.", "-", "Minimal balance rules; job "
     "MINIMAL_BALANCE_SWEEP."),
    ("BR-CSH-09", "Excess shown on the record.", "-", "Unapplied payment with origin EXCESS."),
    ("BR-CSH-10", "Field message on the check date.", "RECORD_INVALID.", "CASH_CHECK_DATE_RULE, "
     "CASH_CHECK_HOLDING_DAYS."),
    ("BR-CSH-11", "Field message on the currency.", "RECORD_INVALID.", "Rate of the day."),
    ("BR-CSH-12", "Reason lists in the dialogs.", "RECEIPT_REASON_REQUIRED; RECEIPT_REASON_TEXT_REQUIRED; "
     "RECORD_CHECK_REASON.", "CASH_CHECK_CANCEL_REASONS."),
    ("BR-CSH-13", "Actions hidden while a record waits.", "RECORD_WAITING; RECEIPT_ACTION_PENDING.", "Locks of the "
     "invoice ledger."),
    ("BR-CSH-14", "-", "-", "CASH_REMITTED_CANCELLATION."),
    ("BR-CSH-15", "Journal on the receipt.", "-", "Accounting events of each posting."),
    ("BR-CSH-16", "Approve on For Approval.", "DISPOSITION_WHOLE_BALANCE; DISPOSITION_NEEDS_APPROVAL.",
     "Disposition type rules."),
    ("BR-CSH-17", "Run report per file.", "CHANNEL_FILE_NOT_PROCESSED.", "Content hash of each file; rows staged."),
    ("BR-CSH-18", "History tab.", "-", "Audit trail insert-only; search log."),
    ("BR-CSH-19", "REPRINT watermark.", "-", "Print count of the receipt."),
    ("BR-CSH-20", "Menu and actions by permission.", "-", "Data access; CASH_UNAPPLIED_MARKETING_SCOPE."),
    ("BR-CSH-21", "-", "-", "Report file names."),
]

WORKFLOWS = {
    "AR / OR record": ("cashiering/domain/RecordCodes", [
        ("CREATED", "Created", "Save", "FOR_POSTING, RECORD_CANCELLED"),
        ("FOR_POSTING", "For Posting", "Submit for Posting", "POSTED, RETURNED"),
        ("RETURNED", "Returned", "Return with a reason", "FOR_POSTING"),
        ("POSTED", "AR/OR Issued, Cancelled or Reinstated", "Post", "-"),
        ("RECORD_CANCELLED", "Record Cancelled", "Cancel Record", "-")]),
}
STATUSES = C.STATUSES

AUDIT_LABELS = {
    "CashApplication": "Application of a payment", "CashBankAccount": "Bank account", "CashLegacyBatch": "Legacy batch",
    "CashRefundValidation": "Refund validation request", "ChannelProfile": "Payment file type",
    "CollectorRequest": "Collector request", "CwtTag": "BIR 2307 tag", "Package version": "Package version",
    "PaymentFile": "Payment file", "PaymentFileLayout": "Payment file layout", "PdcItem": "Post-dated check",
    "PickupRequest": "Check pick-up request", "Prebooked": "Pre-booked payment", "Receipt": "AR or OR",
    "ReceiptForm": "AR or OR form", "ReceiptRecord": "AR / OR, cancellation or reinstatement record",
    "ReceiptSeries": "Receipt series", "SettlementOr": "Settlement OR", "Unapplied": "Unapplied payment"}
AUDIT_OMIT: set[str] = set()
ACTION_WORDS = {"CREATE": "Created", "UPDATE": "Changed", "DEACTIVATE": "Deactivated or cancelled",
                "SUBMIT": "Submitted", "AUTHORIZE": "Authorised", "REJECT": "Rejected or returned",
                "EXPORT": "Exported or printed", "RUN": "Run", "POST": "Posted", "REVERSE": "Reversed",
                "CLOSE": "Closed"}
JOBS = {j[0]: j[2] for j in PG.JOBS}
JOB_OMIT: set[str] = set()


def job_rows():
    """The Cashiering jobs (their names are constants of one class, which the job reader does not see)."""
    cron = {"PREBOOKED_REMATCH": "0 0 */2 * * *", "PAYMENT_AUTOMATCH": "0 30 * * * *",
            "PDC_MATURITY": "0 30 0 * * *", "MINIMAL_BALANCE_SWEEP": "0 0 20 * * *", "CASH_MFT_INTAKE": "-"}
    return [(j, cron[j], when, what) for j, when, what, _ in PG.JOBS]


# ================================================================================================ data dictionary
TABLE_PATTERN = r"^csh_"
_T = {
    "csh_application": "Applications of a payment to an invoice, with source and reversal.",
    "csh_application_line": "Amount applied per component of the invoice.",
    "csh_bank_account": "Bank accounts of Post to Bank Account with their GL account.",
    "csh_certificate_of_payment": "Certifications of Payment issued with their series number.",
    "csh_channel_file": "Payment files received by upload or MFT, with status and counts.",
    "csh_channel_profile": "Payment file types: name convention, layout, size, MFT folder.",
    "csh_collector_request": "Requests of collectors on unapplied payments.",
    "csh_commission_line": "Lines of the Commission Schedules and their match.",
    "csh_cwt_batch": "BIR 2307 batches per insurer.",
    "csh_cwt_tag": "BIR 2307 tags of invoices with path and checklist.",
    "csh_disposition": "Dispositions of unapplied payments with status and approvals.",
    "csh_disposition_type_rule": "Disposition types: action, approval, income event.",
    "csh_legacy_batch": "Legacy batches: reclass to income, PR 2307 reversal.",
    "csh_legacy_batch_line": "Lines of a legacy batch with their result.",
    "csh_legacy_reference": "Legacy references of migrated unapplied payments.",
    "csh_minimal_balance": "Minimal balances reversed or moved to AP overages.",
    "csh_minimal_balance_rule": "Minimal balance rules per kind.",
    "csh_payment": "Payments received (counter, files, checks) with their match.",
    "csh_payment_file_layout": "Layouts of the payment files.",
    "csh_payment_reversal": "Payment reversal requests of Accounting Controls.",
    "csh_pdc_item": "Post-dated checks of the warehouse.",
    "csh_pickup_request": "Check pick-up requests of Collection.",
    "csh_prebooked": "Payments waiting for the booking of their account.",
    "csh_print_batch": "Print batches with their file.",
    "csh_print_line": "Receipts of a print batch with their outcome.",
    "csh_reapplication": "Re-applications after a reinstatement or correction.",
    "csh_receipt": "ARs and ORs with number, status, print count and certificate number.",
    "csh_receipt_action": "Cancellations and reinstatements without the posting step.",
    "csh_receipt_form": "Versions of the AR and OR forms.",
    "csh_receipt_line": "Lines of an OR (income, VAT, withholding tax).",
    "csh_receipt_record": "AR / OR creation, cancellation and reinstatement records.",
    "csh_receipt_record_account": "Accounts paid by a record.",
    "csh_receipt_series": "AR and OR number series per branch and year.",
    "csh_refund_validation": "Refund validation requests of Payment Requests.",
    "csh_search_log": "Searches made on Cashiering.",
    "csh_settlement_or": "ORs of insurer settlements.",
    "csh_unapplied": "Unapplied payments with origin, stage and balance.",
}
TABLES = {t: ("Cashiering", p, "-", "-") for t, p in _T.items()}
VOLUME_NOTE = "Every table of the module; volumes follow the payments received (about 1,000 receipts a day)."
PARAMETER_FILTER = "param_key like 'CASH%'"
PARAMETER_LABELS: dict[str, str] = {}
SETTINGS_SCREEN_KEYS: list[str] = []
PARAMETER_MAKER_CHECKER = ("-", "Yes: System Parameters, approved by another user")
LOV_TYPES = ("AR_CLASS", "OR_TYPE", "RECEIPT_CANCEL_REASON", "REINSTATEMENT_REASON", "DISPOSITION_TYPE")
ENUM_LISTS = [("Receipt kind", "cashiering/domain/CashCodes.ReceiptKind"),
              ("Receipt status", "cashiering/domain/CashCodes.ReceiptStatus"),
              ("Payment mode", "cashiering/domain/CashCodes.PaymentMode"),
              ("Match category", "cashiering/domain/CashCodes.MatchCategory"),
              ("Unapplied origin", "cashiering/domain/CashCodes.UnappliedOrigin"),
              ("Disposition status", "cashiering/domain/CashCodes.DispositionStatus"),
              ("Post-dated check status", "cashiering/domain/CashCodes.PdcStatus"),
              ("Record stage", "cashiering/domain/RecordCodes.RecordStage")]
ENUM_LABELS: dict[str, str] = {}
INACTIVE_CODES: set[str] = set()

# ================================================================================================ user stories
EPICS = [
    ("EP-CSH-01", "Access and dashboard"), ("EP-CSH-02", "AR and OR records and posting"),
    ("EP-CSH-03", "Series, forms and printing"), ("EP-CSH-04", "Cancellation and reinstatement"),
    ("EP-CSH-05", "Payment files and matching"), ("EP-CSH-06", "Unapplied payments and requests"),
    ("EP-CSH-07", "ORs, inquiry, reports and interfaces"),
]
_P = {"C": "Cashier", "T": "Cashiering Team Leader", "M": "Marketing Collection user", "X": "Auditor",
      "B": "Comptrollership user", "I": "BDOI IT user"}
# entry, epic, persona, want, so that, criteria (given, when, then), screen, priority, points
_S = [
    ("01.01", 1, "C", "to use Cashiering from a BDO-issued device", "access stays controlled",
     [("a BDO-issued laptop", "I open BIBS", "I reach the Login page")], "SCR-CSH-01", "High", 1),
    ("01.02", 1, "C", "to see only the Cashiering screens of my role", "access stays role-appropriate",
     [("the Cashier profile", "I open the menu", "I do not see Receipt Series"),
      ("I created a record", "I try to post it", "it is refused")], "SCR-CSH-01", "High", 3),
    ("01.03", 1, "T", "a dashboard of the work waiting", "I follow the team's work",
     [("records For Posting", "I open the dashboard", "their count and amount show per receipt type"),
      ("an AR number", "I search it", "the receipt opens")], "SCR-CSH-01", "High", 5),
    ("02.01", 2, "C", "to create an AR record for a premium payment", "the payment is receipted",
     [("an account with an outstanding balance", "I save the record", "it is Created with a CR-AR number"),
      ("a check dated yesterday", "I save", "the holding-period message shows")], "SCR-CSH-02", "High", 8),
    ("02.02", 2, "C", "to create an OR record at Head Office", "other income is receipted",
     [("a Service Fee with VAT", "I save the OR record", "it is Created with a CR-OR number"),
      ("I am a Cebu cashier", "I save an OR record", "it is refused")], "SCR-CSH-02", "High", 5),
    ("02.03", 3, "T", "to maintain the AR and OR number series", "numbers follow the BIR permits",
     [("a new series", "I authorise my own series", "it is refused")], "SCR-CSH-23", "High", 3),
    ("02.04", 3, "C", "to print and re-print receipts in batches", "clients get their receipts",
     [("501 receipts selected", "I print", "it is refused"),
      ("an AR printed once", "I print it again", "it shows REPRINT")], "SCR-CSH-19", "High", 5),
    ("02.05", 2, "T", "to post or return the records", "receipts are issued under control",
     [("a record For Posting", "I post it", "the AR is issued and the payment applied"),
      ("a depleted series", "I post", "the record stays For Posting with the message")], "SCR-CSH-04", "High", 8),
    ("02.06", 3, "T", "to maintain the AR and OR forms", "receipts carry BDOI's wording",
     [("a form change", "another user approves it", "receipts print with it from its date")], "SCR-CSH-22",
     "Medium", 3),
    ("03.01", 4, "C", "to cancel an issued AR or OR", "errors are corrected",
     [("an applied AR", "the cancellation is posted", "the AR is Cancelled and the invoice outstanding again")],
     "SCR-CSH-07", "High", 5),
    ("04.01", 4, "C", "to reinstate an AR or OR", "payments are re-applied",
     [("an applied AR", "the full reinstatement is posted", "the money returns to the Unapplied list")],
     "SCR-CSH-07", "High", 5),
    ("05.01", 5, "C", "to upload the payment files", "channel payments are applied",
     [("a Bills Payment file", "I upload it", "each row gets an AR and a result")], "SCR-CSH-16", "High", 8),
    ("05.02", 5, "C", "to warehouse post-dated checks", "checks are applied at maturity",
     [("a check maturing today", "the daily run ends", "it is Applied with its AR")], "SCR-CSH-18", "Medium", 3),
    ("05.03", 5, "C", "pre-booked payments applied at booking", "nothing waits by hand",
     [("an account booked", "I click Re-match Now", "the payment is applied")], "SCR-CSH-14", "Medium", 2),
    ("05.04", 5, "C", "the payment confirmation passed to New Business", "placement does not wait",
     [("a payment by ARN", "it is recorded", "New Business reads the account as paid")], "SCR-CSH-14", "Medium", 1),
    ("06.01", 6, "C", "to follow the unapplied payments", "money is not left idle",
     [("an excess payment", "I filter Excess", "it is listed with its source")], "SCR-CSH-08", "High", 3),
    ("06.02", 6, "C", "to give dispositions with approval", "money is applied or refunded under control",
     [("a Refund", "the Team Leader approves", "the request goes to Disbursement")], "SCR-CSH-09", "High", 5),
    ("06.03", 6, "C", "to decide the requests of other modules", "collectors get an answer",
     [("a collector apply request", "I accept it", "its disposition is created")], "SCR-CSH-13", "Medium", 3),
    ("07.01", 7, "C", "ORs issued for insurer settlements", "commissions are receipted",
     [("a settlement approved by Disbursement", "the OR is issued", "it is in the print queue")], "SCR-CSH-17",
     "High", 3),
    ("08.01", 7, "X", "to search receipts and read the search log", "I can trace inquiries",
     [("searches of a user", "I open the Search Log", "they are listed")], "SCR-CSH-12", "Medium", 2),
    ("08.02", 7, "C", "the payment history of each invoice", "I answer client questions",
     [("a payment applied", "I open the receipt", "the invoice and amounts per component show")], "SCR-CSH-07",
     "Medium", 1),
    ("08.03", 7, "X", "the audit trail of each receipt", "every action is traceable",
     [("a re-printed AR", "I open its History", "both prints show")], "SCR-CSH-07", "High", 2),
    ("09.01", 7, "B", "to run the Cashiering reports", "I reconcile collections",
     [("a period", "I run a report", "it downloads in Excel")], "SCR-CSH-28", "High", 3),
    ("09.02", 7, "B", "the report list of the BRD", "each report has its content",
     [("the Daily Cash Reconciliation report", "I run it for a day", "it lists the ORs of each day with encoder and deposit bank")],
     "SCR-CSH-29", "High", 8),
    ("10.01", 7, "I", "the interfaces of Cashiering", "data flows without re-keying",
     [("a payment applied", "Remittance extracts", "the paid premium is read")], "SCR-CSH-16", "High", 3),
    ("10.02", 7, "I", "flow-in runs recorded", "nothing is received twice",
     [("a file received twice", "it is processed", "no duplicate payment")], "SCR-CSH-16", "Medium", 2),
    ("10.03", 7, "I", "batch runs past failed records", "one error stops nothing",
     [("a run with a failed record", "it ends", "the others are done and the failure is listed")], "SCR-CSH-16",
     "High", 2),
    ("10.04", 7, "T", "notices and alerts", "I act on time",
     [("a record submitted", "I open the bell", "the notice opens the record")], "SCR-CSH-01", "Medium", 2),
]


def _items(entry: str) -> str:
    return ", ".join(r[0] for f in C.FRUM if f["id"] == entry for r in f["reqs"])


_RULES = {}
for _f in C.FRUM:
    _rs = sorted({x.strip() for r in _f["reqs"] for x in r[4].replace(";", ",").split(",")
                  if x.strip().startswith("BR-")})
    _RULES[_f["id"]] = ", ".join(_rs) or "-"

STORIES = [(f"US-CSH-{n.replace('.', '')}", f"EP-CSH-{e:02d}", _P[p], want, so, acs, _items(f"FRS.CSH.{n}"),
            _RULES[f"FRS.CSH.{n}"], scr, prio, pts) for n, e, p, want, so, acs, scr, prio, pts in _S]
REPORT_AC = []

README = {
    "intro": ("This pack holds every document of Operations Cashiering (BRD-02). Each document has one job. A "
              "requirement or rule is stated once, in the FRS, with its ID; the other documents refer to that ID."),
    "order": ["FRS", "BPS", "UXD", "RNO", "ICD", "DSP", "DDI", "USR", "RTM"],
    "programme": [
        ("Non-Functional and Security Requirements", "Performance, availability, security, audit, retention, backup, "
         "accessibility and browsers for every BRD."),
        ("Glossary", "Terms and abbreviations used in every pack."),
        ("Decision and Observation Log", "Decisions taken and observations raised across the BRDs."),
        ("Document Register", "Every current document of the BRD packs with version, date, owner and status."),
    ],
    "ids": [("FRS.CSH.nn.nn.nn", "Functional requirement (FRS)"), ("BR-CSH-nn", "Business rule (FRS)"),
            ("SCR-CSH-nn", "Screen (UX Design)"), ("SB-nn, P-CSH-nn", "Storyboard, process (Process and Storyboards)"),
            ("RPT-CSH-nn, DO-nn, NT-nn, AL-nn", "Report, document, notice, alert (Reports, Notifications and "
             "Outputs)"), ("IF-CSH-nn", "Interface (Interface Specification)"),
            ("US-CSH-nnnn", "User story (User Stories)"), ("TC-CSH-…", "Test case (Test Cases and Traceability)"),
            ("CR-CSH-nnn", "Change request (CR Register)"), ("NFR-…", "Non-functional requirement (programme NFR)"),
            ("D-nnn / O-nnn", "Decision or observation (programme log)")],
    "colour": "The colour code of the FRS applies to every document: black in the BRD, green OOTB, orange change "
              "request, blue best practice, grey label for Integration or Data Migration scope.",
}


def screens_by_id() -> dict[str, dict]:
    return {s["id"]: s for s in UX.SCREENS}


def check() -> list[str]:
    problems = []
    rules = {r[0] for r in C.BUSINESS_RULES}
    frum = {r[0] for f in C.FRUM for r in f["reqs"]}
    scr = set(screens_by_id())
    for s in STORIES:
        for ref in [x.strip() for x in s[6].split(",")]:
            if ref not in frum:
                problems.append(f"{s[0]}: unknown requirement {ref}")
        for ref in [x.strip() for x in s[7].split(",") if x.strip() != "-"]:
            if ref not in rules:
                problems.append(f"{s[0]}: unknown rule {ref}")
        if s[8] not in scr:
            problems.append(f"{s[0]}: unknown screen {s[8]}")
    missing = rules - {r[0] for r in RULE_ENFORCEMENT}
    if missing:
        problems.append(f"rules without enforcement: {sorted(missing)}")
    covered = {x.strip() for s in STORIES for x in s[6].split(",")}
    if frum - covered:
        problems.append(f"requirements without a user story: {sorted(frum - covered)}")
    return problems


DDI_TEXT: dict[str, str] = {}
