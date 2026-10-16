"""BRD-06 Renewal: content of the standard document set that is not already in brd06_v13_content.py,
brd06_v13_pages.py and brd06_v13_screens.py. Read by build_pack.py (developer specification DSP, data dictionary DDI,
user stories USR, the "How to read this pack" note) and by build_brd06_v13_workbooks.py (traceability).

The facts of the services, screens and tables are read from the sources by tools/deliverables/api_facts.py and
schema_facts.py; this module says which of them belong to Renewal and gives them their business wording.
"""

from __future__ import annotations

import re

import brd06_v13_content as C
import brd06_v13_screens as UX

BRD = "BRD-06"
NAME = "Renewal"
SHORT = "Renewal"
FOLDER = "BRD-06_Renewal"
DATE = C.DATE
DATE_US = C.DATE_US
PREPARED = "iorta TechNXT Project Team"
HEADER_RIGHT = C.HEADER_RIGHT
DOC_NO = "PRJ0012475-RN"

SET = [
    ("README", "Inputs", "How_to_Read_this_Pack", "1.0", "This note: the documents of the pack and who reviews them.",
     "All reviewers"),
    ("FRS", "FRS-BDOI", "Renewal", C.VERSION, "Requirements, business rules, validations, personas and permissions, "
     "parameters and lists of values, statuses.", "BDOI Business Unit owners (Marketing, Processing, LAMD, MBS)"),
    ("BPS", "BPS-BDOI", "Renewal_Process_and_Storyboards", C.VERSION, "Journey, to-be processes and storyboards.",
     "BDOI Business Unit owners"),
    ("UXD", "UXD-BDOI", "Renewal_UX_Design", C.VERSION, "Every screen: purpose, users, fields, actions, states, "
     "messages, one screenshot per page.", "BDOI Business Unit owners; UAT lead"),
    ("RNO", "RNO-BDOI", "Renewal_Reports_Notifications_Outputs", C.VERSION, "Reports, letters and documents, "
     "exports, notices, e-mails and jobs.", "BDOI Business Unit owners; Internal Audit"),
    ("ICD", "ICD-BDOI", "Renewal_Interface_Specification", C.ICD_VERSION, "CCM, MFT, insurer files, LAMD and "
     "BDOFC/SOLD reports, CLPC billing files.", "BDOI IT (integration)"),
    ("DSP", "DSP-BDOI", "Renewal_Developer_Specification", "1.0", "Screens, service operations, field validations, "
     "rules, states, jobs, audit events and permissions, as the system works.", "BDOI IT; iorta TechNXT developers "
     "and testers"),
    ("DDI", "DDI-BDOI", "Renewal_Data_Dictionary", "1.0", "Tables, columns, lists of values, parameters, status "
     "models and indexes.", "BDOI IT (database administration); Data Migration"),
    ("USR", "USR-BDOI", "Renewal_User_Stories", "1.0", "User stories with acceptance criteria, ready to import into "
     "the backlog tool.", "BDOI Product Owner; UAT lead"),
    ("RTM", "RTM", "Test_Cases_and_Traceability", C.WB_VERSION, "Test cases (every acceptance criterion) and the "
     "trace BRD, FRRN, user story, screen, test case.", "UAT lead; Internal Audit"),
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


def file_name(key: str) -> str:
    kind, title, ver = next((k2, t, v) for k, k2, t, v, *_ in SET if k == key)
    return f"BIBS_{kind}_{BRD}_{title}_v{ver}.{EXT[key]}"


def version(key: str) -> str:
    return next(v for k, _, _, v, *_ in SET if k == key)


DOCS = {
    "DSP": {"cover": "Renewal: Developer Specification", "header_left": "Developer Specification",
            "doc_no": f"{DOC_NO}-DSP"},
    "README": {"cover": "Renewal: How to Read this Pack", "header_left": "How to Read this Pack",
               "doc_no": f"{DOC_NO}-README"},
}

# ================================================================================================ developer spec
PACKAGES = ["renewal"]
MESSAGE_PACKAGES = ["renewal"]
SETTING_CLASSES = ("SystemParameter",)
_R = "renewal/"
CONTROLLERS = [_R + c for c in (
    "acceptance/api/AcceptanceController.java", "allocation/api/InsurerAllocationController.java",
    "approval/api/DispositionSetController.java", "approval/api/RenewalApprovalController.java",
    "audit/api/RenewalAuditLogController.java", "billing/api/BillingFileController.java",
    "budget/api/RenewalBudgetController.java", "candidate/api/CandidateController.java",
    "channel/api/ChannelController.java", "channel/api/RenewalDeliveriesController.java",
    "dashboard/api/ProcessingDashboardController.java", "dashboard/api/RenewalDashboardController.java",
    "epolicy/api/RenewalEpolicyController.java", "extraction/api/ExtractionController.java",
    "extraction/api/ManualCreationController.java", "holdcover/api/HoldCoverRequestController.java",
    "holdcover/api/RenewalHoldCoverController.java", "home/api/RenewalHomeController.java",
    "insurer/api/RenewalInsurerController.java", "kyc/api/KycMonitoringController.java",
    "lamd/api/LamdController.java", "lamd/api/RmuOfficerController.java", "letter/api/LetterController.java",
    "marketing/api/MarketingController.java", "marketing/api/PostingApproverController.java",
    "marketing/api/ReferralController.java", "marketing/api/TransferController.java",
    "mft/api/MftInboxController.java", "placement/api/RenewalPlacementController.java",
    "processing/api/ProcessingController.java", "proposal/api/RenewalProposalController.java",
    "rules/api/RenewalUpdatesController.java", "setup/api/InsurerRenewableListController.java",
    "setup/api/PackageChoiceController.java", "setup/api/RenewalSetupController.java",
    "setup/api/RiskCodeMaintenanceController.java", "upload/api/RenewalUploadController.java")]
SHARED_CONTROLLERS = ["report/api/ReportController.java", "attachment/api/AttachmentController.java",
                      "bulk/api/BulkController.java"]
OMIT_OPS: set[str] = set()

DSP_TEXT = {
    "requirement_ids": "FRRN, BR-RN",
    "screen_ids": "SCR-RN",
    "interfaces": "CCM, MFT, insurer files, LAMD and BDOFC/SOLD reports, CLPC billing files",
    "integration_prefix": "",
    "service_ops": "The CCM and MFT exchanges and the file layouts are detailed in the Interface Specification.",
    "status_note": "Set-up records (risk codes, checks, rule versions, package map and choices) follow the "
                   "maker-checker statuses of the data dictionary (sheet Status models).",
    "jobs_note": "The jobs can be started at once from Operations › Jobs.",
    "audit_note": "Each renewal also keeps its own history (stage, action, user, time, reason).",
    "security_note": "LAMD and Contact Center users receive the renewal lists without premium and commission.",
    "omit_classes": ("Simulator", "Retention", "Seed"),
}

AREAS = [
    ("Expiry list, renewals and checks", ("renewal/candidates", "renewal/extraction", "renewal/manual",
                                          "renewal/updates", "renewal/home"),
     "Extraction, initiation, the renewal record, checks, classification, updates."),
    ("Marketing", ("renewal/marketing", "renewal/transfers", "renewal/referrals", "renewal/posting",
                   "renewal/disposition"), "Assignment, disposition, review, posting, transfers and referrals."),
    ("Processing and insurers", ("renewal/processing", "renewal/insurer", "renewal/allocation", "renewal/uploads",
                                 "renewal/lamd", "renewal/rmu", "bulk/"),
     "Worklist, insurer batches and responses, uploads, LAMD reports."),
    ("Letters, acceptance and approval", ("renewal/letters", "renewal/acceptance", "renewal/approval",
                                          "renewal/proposal", "renewal/hold-cover"),
     "Renewal Advice, letters, proposals, acceptance, approval, hold cover."),
    ("Placement, billing and e-policies", ("renewal/placement", "renewal/billing", "renewal/epolicies",
                                           "renewal/mft"), "Placement, responses, CLPC billing, e-policies, MFT."),
    ("Dashboards, channels, set-up and audit", ("renewal/dashboard", "renewal/processing-dashboard", "renewal/kyc",
                                                "renewal/channels", "renewal/deliveries", "renewal/setup",
                                                "renewal/risk-codes", "renewal/budget", "renewal/audit",
                                                "reports/", "attachments/"),
     "Dashboards, KYC, channel monitor, Renewal Setup, risk codes, budget, audit logs, reports."),
]

COMPONENTS = [
    ("Renewal screens", f"{len(UX.SCREENS)} screens and record tabs under Client & Policy › Renewal.", "Section 3"),
    ("Renewal service", "Extraction, checks, classification, decision matrix, marketing, processing, letters, "
     "acceptance, placement, hold cover, billing, e-policies, set-up.", "Section 4"),
    ("Delivery channels", "CCM and MFT connections with their simulators for SIT and UAT.",
     "Interface Specification v1.0"),
    ("Audit trail", "Insert-only record of every action.", "Section 8"),
    ("Scheduled jobs", "Extraction, re-evaluation, letters, expiry sweep, hold cover, billing, KYC, MFT inbox, "
     "channel status.", "Section 7"),
]

_PAGES = {
    "SCR-RN-01": "home/RenewalHomePage.tsx", "SCR-RN-02": "expiry/ExpiryListPage.tsx",
    "SCR-RN-03": "record/CandidatePage.tsx", "SCR-RN-04": "record/HistoryTabs.tsx",
    "SCR-RN-05": "record/RecordTabs.tsx", "SCR-RN-06": "mine/MyDispositionsPage.tsx",
    "SCR-RN-07": "record/RecordDialogs.tsx", "SCR-RN-08": "review/ReviewPage.tsx",
    "SCR-RN-09": "transfers/TransfersPage.tsx", "SCR-RN-10": "processing/ProcessingPage.tsx",
    "SCR-RN-11": "insurer/InsurerBatchesPage.tsx", "SCR-RN-12": "record/RecordDialogs.tsx",
    "SCR-RN-13": "letters/LettersPage.tsx", "SCR-RN-14": "record/ClientDialogs.tsx",
    "SCR-RN-15": "followups/FollowupsPage.tsx", "SCR-RN-16": "lamd/LamdPage.tsx",
    "SCR-RN-17": "setup/ChecksTab.tsx", "SCR-RN-18": "setup/RuleVersionsTab.tsx",
    "SCR-RN-19": "setup/PackageChoicesTab.tsx", "SCR-RN-20": "setup/GoLiveTab.tsx",
    "SCR-RN-21": "dashboard/RenewalDashboardPage.tsx", "SCR-RN-22": "accounts/RenewalAccountsPage.tsx",
    "SCR-RN-23": "dashboard/ProcessingDashboardPage.tsx", "SCR-RN-24": "referrals/ReferralsPage.tsx",
    "SCR-RN-25": "approval/ApprovalsPage.tsx", "SCR-RN-26": "placement/PlacementPage.tsx",
    "SCR-RN-27": "proposal/TsuQueuePage.tsx", "SCR-RN-28": "kyc/KycDashboardPage.tsx",
    "SCR-RN-29": "riskcodes/RiskCodesPage.tsx", "SCR-RN-30": "billing/BillingFilesPage.tsx",
    "SCR-RN-31": "epolicy/EpolicyPage.tsx", "SCR-RN-32": "uploads/RmuOfficersPage.tsx",
    "SCR-RN-33": "channels/ChannelMonitorPage.tsx", "SCR-RN-34": "audit/AuditLogsPage.tsx",
    "SCR-RN-35": "budget/BudgetPage.tsx", "SCR-RN-36": "record/HoldCoverTab.tsx",
}


def _screens() -> list[dict]:
    out = []
    for s in UX.SCREENS:
        out.append({"id": s["id"], "name": s["name"], "route": s["route"].lstrip("/"), "uxd": s["id"],
                    "frum": s["frrn"], "page": ("renewal", _PAGES[s["id"]]), "loads": s["purpose"],
                    "states": s["rules"][:4] or ["-"], "fields": [], "codes": []})
    return out


SCREENS = _screens()
UX_INPUTS: dict[str, str] = {}
_DROP = {("lovApi", "options"), ("systemApi", "settings")}


def fix_call(c):
    """The Renewal clients append the query string as ${query(...)}: that suffix is not part of the path."""
    if (c.obj, c.fn) in _DROP:
        return None
    if c.obj == "renewalHoldCoverApi" and c.fn in ("request", "confirm", "decline", "cancel"):
        return c.__class__(c.obj, c.fn, c.verb, f"/renewal/candidates/{{x}}/hold-cover/{c.fn}")
    return c.__class__(c.obj, c.fn, c.verb, re.sub(r"(?<!/)\{x\}", "", c.path))


ACTION_LABELS = {
    "assign": "Assign", "push": "Push", "initiate": "Initiate", "transfer": "Transfer", "post": "Post",
    "returnToOfficer": "Return", "override": "Override", "setDisposition": "Set Disposition",
    "recordResponse": "Record Insurer Response", "recordAcceptance": "Record Acceptance", "generate": "Generate",
    "send": "Send", "resend": "Resend", "export": "Download",
}
DATA_LABELS = {"search": "Renewals found", "get": "The renewal", "list": "The list", "home": "Renewal Home figures"}
PERMISSION_PREFIXES = ("RNW_",)

PERSONA_ROLES = [
    ("Marketing Team Lead", "MKT_TL"), ("Marketing Account Officer", "MKT_AO"),
    ("Processing Team Lead", "PROCESSING_TL"), ("Processing Officer", "PROCESSOR"),
    ("Contact Center", "CONTACT_CENTER"), ("LAMD", "LAMD"), ("TSU Officer", "TSU"),
    ("Business Administrator", "BUSINESS_ADMIN"), ("System Administrator", "SYSADMIN"), ("Auditor", "AUDITOR"),
]

RULE_ENFORCEMENT = [
    ("BR-RN-01", "Approve and Activate hidden from the maker.", "RNW_RULES_REJECT_REMARKS; RNW_RA_SENT_DECIDED.",
     "Maker and approver recorded on set-up records and versions."),
    ("BR-RN-02", "-", "-", "One renewal per expiring term; extraction runs count read, new and skipped."),
    ("BR-RN-03", "Initiate on the Extracted tab.", "-", "Stage EXTRACTED until initiation."),
    ("BR-RN-04", "Menu and actions by permission; premium hidden for LAMD and Contact Center.", "-",
     "Lists, reports and exports filtered by the user's scope."),
    ("BR-RN-05", "Locked flag; edit actions hidden.", "-", "Setting RNW_LOCK_POINT."),
    ("BR-RN-06", "Blocking checks in an amber notice.", "RNW_BOOKING_ONLY_OVERRIDE.", "Check results with severity."),
    ("BR-RN-07", "Classification pill and history.", "RNW_CHECK_SEVERITY.", "Active rule set; job RNW_REEVALUATE."),
    ("BR-RN-08", "-", "-", "Jobs RNW_NRNS_LETTERS and RNW_EXPIRY_SWEEP on the effective expiry date."),
    ("BR-RN-09", "-", "-", "Password convention RNW_PASSWORD_CONVENTION; channel messages."),
    ("BR-RN-10", "KYC due flag only.", "-", "Job RNW_KYC_MONITOR."),
    ("BR-RN-11", "Attention flags on the listing.", "-", "No escalation job."),
    ("BR-RN-12", "Place and book are user actions.", "-", "Settings RNW_AUTO_PLACEMENT, RNW_BOOK_ON_PLACEMENT_APPROVAL."),
    ("BR-RN-13", "Incentive code read only.", "-", "Set at booking from the incentive criteria."),
    ("BR-RN-14", "No delete action.", "-", "Records ended or deactivated; audit trail insert-only."),
    ("BR-RN-15", "Actions hidden while one is open.", "RNW_BATCH_SENT; RNW_TRANSFER_DECIDED.", "-"),
    ("BR-RN-16", "Reason and remarks required in the dialogs.", "RNW_REMARKS_REQUIRED; RNW_OVERRIDE_REASON.", "-"),
]

WORKFLOWS = {
    "Renewal account": ("renewal/domain/RenewalStage", [
        ("EXTRACTED", "Extracted", "Generation or daily extraction", "EVALUATING, UNASSIGNED, FOR_PROCESSING, "
         "LETTER_PENDING"),
        ("EVALUATING", "Evaluating", "Initiate", "UNASSIGNED, FOR_PROCESSING, LETTER_PENDING"),
        ("UNASSIGNED", "Unassigned Disposition", "Checks and decision matrix", "FOR_DISPOSITION, TRANSFER_PENDING"),
        ("TRANSFER_PENDING", "Transfer Pending", "Transfer", "UNASSIGNED"),
        ("FOR_DISPOSITION", "For Disposition", "Assign; Return; Re-open", "FOR_TL_REVIEW"),
        ("FOR_TL_REVIEW", "Review in Progress", "Push / Submit for Posting", "FOR_PROCESSING, NB_PATH, "
         "LETTER_PENDING, CLOSED, FOR_DISPOSITION"),
        ("NB_PATH", "For Proposal / New Business Path", "Post For Quotation or For Proposal", "RENEWED, CLOSED"),
        ("FOR_PROCESSING", "For Processing", "Post For Renewal", "IN_PROCESSING"),
        ("IN_PROCESSING", "In Processing", "Assign PO", "WITH_INSURER"),
        ("WITH_INSURER", "With Insurer", "Insurer batch sent", "RA_READY, FOR_DISPOSITION"),
        ("RA_READY", "RA Ready", "Insurer response Renew As Is", "RA_GENERATED"),
        ("RA_GENERATED", "RA Generated", "Generate RA", "RA_SENT"),
        ("RA_SENT", "RA Sent / Awaiting Response", "Send", "ACCEPTED, CLOSED"),
        ("ACCEPTED", "Accepted", "Record Acceptance", "FOR_PLACEMENT_BOOKING"),
        ("FOR_PLACEMENT_BOOKING", "For Placement and Booking", "Approval or payment", "RENEWED"),
        ("LETTER_PENDING", "Letter Pending", "Post Not for Renewal", "CLOSED"),
        ("RENEWED", "Renewed", "Booking", "-"),
        ("CLOSED", "Closed", "Letter sent, Lost Business, expiry sweep", "-")]),
    "Transfer": ("renewal/domain/TransferStatus", [
        ("REQUESTED", "Requested", "Transfer", "ACCEPTED, DECLINED, CANCELLED"),
        ("ACCEPTED", "Accepted", "Accept", "-"), ("DECLINED", "Declined", "Decline", "-"),
        ("CANCELLED", "Cancelled", "Cancel", "-")]),
    "Insurer batch": ("renewal/domain/InsurerBatchStatus", [
        ("DRAFT", "Draft", "New Batch", "SENT"), ("SENT", "Sent", "Send to Insurer", "PARTIALLY_RESPONDED, CLOSED"),
        ("PARTIALLY_RESPONDED", "Partially Responded", "First responses", "CLOSED"),
        ("CLOSED", "Closed", "Last response", "-")]),
    "Letter": ("renewal/domain/LetterStatus", [
        ("GENERATED", "Generated", "Generate", "QUEUED"), ("QUEUED", "Queued", "Send", "SENT, FAILED"),
        ("SENT", "Sent", "Channel delivery", "-"), ("FAILED", "Failed", "Channel refusal", "QUEUED, CANCELLED"),
        ("CANCELLED", "Cancelled", "Cancel", "-")]),
}
STATUSES = C.STATUSES

AUDIT_LABELS = {
    "Account": "Renewal account (New Business account)", "BillingFile": "CLPC billing file",
    "EpolicyReceipt": "E-policy receipt", "Package version": "Package version of a migrated policy",
    "RenewalCheckSetting": "Check setting", "RenewalInsurerBatch": "Insurer batch",
    "RenewalInsurerRenewableList": "Insurer renewable list", "RenewalPackageMap": "Package map entry",
    "RenewalRiskCode": "Risk code", "RenewalUpload": "Renewal upload", "RmuOfficer": "RMU Account Officer code"}
AUDIT_OMIT: set[str] = set()
ACTION_WORDS = {"CREATE": "Created", "UPDATE": "Changed", "DEACTIVATE": "Deactivated", "SUBMIT": "Submitted",
                "AUTHORIZE": "Authorised", "REJECT": "Rejected", "EXPORT": "Exported", "RUN": "Run",
                "APPROVE": "Approved", "DELETE": "Removed", "SEND": "Sent"}
JOBS = {j[0]: j[2] for j in __import__("brd06_v13_pages").JOBS}
JOB_OMIT: set[str] = set()

# ================================================================================================ data dictionary
TABLE_PATTERN = r"^rnw_"
_T = {
    "rnw_acceptance": "Client acceptances of the Renewal Advice: method, evidence, date.",
    "rnw_assignment": "Assignments of a renewal to Account Officers, with reason.",
    "rnw_bdofc_line": "Rows of the BDOFC and BDOSOLD reports and their match.",
    "rnw_billing_file": "CLPC billing files: run, line, kind, LMS upload status.",
    "rnw_bucket_history": "Classification changes of a renewal with rule and version.",
    "rnw_bucket_rule": "Classification rules of a rule set.",
    "rnw_bucket_rule_set": "Versions of the classification rules (draft, active, retired).",
    "rnw_budget": "Annual renewal budgets by year and hierarchy.",
    "rnw_budget_history": "Changes of the budgets.",
    "rnw_budget_month": "Monthly amounts of a budget.",
    "rnw_candidate": "Renewal accounts: expiring account, stage, classification, disposition, flags.",
    "rnw_candidate_endorsement": "Endorsements linked to a renewal.",
    "rnw_channel_event": "Delivery history of the channel messages.",
    "rnw_channel_message": "CCM and MFT messages: document, recipients, status, reference.",
    "rnw_check_result": "Results of each check of a check run.",
    "rnw_check_run": "Check runs of a renewal (trigger, time).",
    "rnw_check_setting": "Severity and settings of each check.",
    "rnw_decision_matrix": "Versions of the decision matrix.",
    "rnw_decision_rule": "Rules of a decision matrix version.",
    "rnw_disposition": "Dispositions of a renewal with source, reason and remarks.",
    "rnw_epolicy_line": "Records of an e-policy receipt and their match.",
    "rnw_epolicy_receipt": "E-policy receipts (upload or MFT) with their outcome.",
    "rnw_extraction_run": "Extraction runs with range and counts.",
    "rnw_followup": "Follow-ups of the Contact Center.",
    "rnw_history_view": "Openings of the account history before a disposition.",
    "rnw_hold_cover_request": "Hold cover requests, responses and extensions.",
    "rnw_insurance_advice": "Insurance Advices of mortgaged accounts and their versions.",
    "rnw_insurer_allocation": "Insurers of a renewal by percentage or amount.",
    "rnw_insurer_batch": "Insurer requests (batches) with reply date and status.",
    "rnw_insurer_batch_line": "Renewals of a batch with the 28 frozen fields.",
    "rnw_insurer_disposition_line": "Rows of the insurer disposition uploads.",
    "rnw_insurer_disposition_map": "Mapping of insurer dispositions to classification and disposition.",
    "rnw_insurer_renewable_risk": "Risks each insurer renews.",
    "rnw_insurer_response": "Insurer responses of a renewal.",
    "rnw_kyc_activity": "KYC monitoring statuses and activities.",
    "rnw_lamd_line": "Rows of the LAMD reports and their match.",
    "rnw_lamd_loan": "Loans of the LAMD CBG reports.",
    "rnw_lamd_report": "LAMD report uploads.",
    "rnw_letter": "Letters of a renewal: type, template version, status.",
    "rnw_letter_batch": "Letter generation and sending batches.",
    "rnw_non_renewable_risk_code": "Risk codes with the renewable indicator and its approval.",
    "rnw_override": "Overrides with kind, old and new value, reason and remarks.",
    "rnw_package_choice": "Package choices of migrated policies and their approval.",
    "rnw_package_map": "Package code map of the legacy packages.",
    "rnw_paid_off_account": "Repository of paid-off accounts.",
    "rnw_placement": "Placement of a renewal: status, dates, turnaround, issue.",
    "rnw_placement_file": "Placement slips and files with their sending status.",
    "rnw_placement_tracking": "Placement responses and turnaround tracking.",
    "rnw_processing_assignment": "Assignments of a renewal to Processing Officers.",
    "rnw_proposal": "Quick and full proposals of a renewal.",
    "rnw_ra_sent_request": "Renewal Advices sent before go-live and their corrections.",
    "rnw_referral": "Referrals to another Marketing unit.",
    "rnw_remark": "Remarks of a renewal.",
    "rnw_rmu_officer": "RMU Account Officer codes.",
    "rnw_submitted_handoff": "Hand-offs of submitted policies.",
    "rnw_transfer": "Transfers to another Marketing unit.",
    "rnw_tsu_quote": "Quotes of a TSU request.",
    "rnw_tsu_request": "TSU requests raised from renewals.",
    "rnw_upload_scope": "Complete-file declarations of the dispositioned uploads.",
}
TABLES = {t: ("Renewal", p, "-", "-") for t, p in _T.items()}
VOLUME_NOTE = "Every table of the module; volumes follow the expiring policies (about 20,000 renewals a year)."
PARAMETER_FILTER = "param_key like 'RNW%'"
PARAMETER_LABELS: dict[str, str] = {}
SETTINGS_SCREEN_KEYS: list[str] = []
PARAMETER_MAKER_CHECKER = ("-", "Yes: System Parameters, approved by another user")
LOV_TYPES = ("RNW_DISPOSITION", "RNW_FOLLOWUP_CHANNEL", "RNW_FOLLOWUP_OUTCOME", "RNW_INSURER_RESPONSE",
             "RNW_LAMD_STATUS", "RNW_NONRENEWAL_REASON", "RNW_OVERRIDE_REASON", "RNW_PLACEMENT_REJECT_REASON",
             "RNW_PLACEMENT_RETURN_REASON", "RNW_RETURN_REASON", "RNW_TRANSFER_REASON")
ENUM_LISTS = [("Renewal stage", "renewal/domain/RenewalStage"), ("Classification", "renewal/domain/Bucket"),
              ("Letter type", "renewal/domain/LetterType"), ("Letter status", "renewal/domain/LetterStatus"),
              ("Insurer response", "renewal/domain/InsurerResponseCode"),
              ("Acceptance method", "renewal/domain/AcceptanceMethod"), ("Override kind", "renewal/domain/OverrideKind"),
              ("Transfer status", "renewal/domain/TransferStatus"), ("Closed as", "renewal/domain/ClosedAs")]
ENUM_LABELS = {code: label for code, label, *_ in WORKFLOWS["Renewal account"][1]} | {
    "CLEAN": "Clean", "REVIEW": "Review", "EXCEPTION": "Exception", "RA": "Renewal Advice", "NAL": "No Advice Letter",
    "NFR": "Not for Renewal Letter", "NRNS_REMINDER": "NRNS reminder", "NON_ACCEPTANCE": "Non-acceptance letter",
    "RENEW_AS_IS": "Renew As Is", "REVISE": "Revise", "REJECT": "Reject", "EMAIL": "E-mail",
    "SIGNED_RA": "Signed Renewal Advice", "PAYMENT": "Payment", "RA_UNLOCK": "Renewal Advice cancelled (unlock)",
    "OUTSTANDING_BALANCE": "Outstanding balance", "INSURER_MISMATCH": "Insurer response mismatch",
    "BOOKED_OTHER_INVOICE": "Booked to another invoice", "EXPIRED_UNRENEWED": "Expired without renewal"}
INACTIVE_CODES: set[str] = set()

# ================================================================================================ user stories
EPICS = [
    ("EP-RN-01", "Access, dashboards and lists"), ("EP-RN-02", "Extraction, checks and classification"),
    ("EP-RN-03", "Marketing: assignment, disposition, transfer and posting"),
    ("EP-RN-04", "Processing, insurers and uploads"), ("EP-RN-05", "Renewal Advice, letters and acceptance"),
    ("EP-RN-06", "Placement, booking, billing and e-policies"), ("EP-RN-07", "Hold cover"),
    ("EP-RN-08", "Set-up, reports, audit and go-live"),
]
_P = {"T": "Marketing Team Lead", "A": "Marketing Account Officer", "P": "Processing Team Lead",
      "O": "Processing Officer", "C": "Contact Center user", "L": "LAMD user", "B": "Business Administrator",
      "X": "Auditor", "U": "TSU Officer"}
# id, epic, persona, want, so that, criteria (given, when, then), screen, priority, points
_S = [
    ("001", 1, "T", "to see only the Renewal screens and records of my role", "access stays role-appropriate",
     [("I hold the Marketing Team Lead profile", "I open the menu", "I see no Processing Worklist or Renewal Setup"),
      ("I am idle for 15 minutes", "the warning shows", "I choose Stay Logged In or Log Out")], "SCR-RN-01", "High", 3),
    ("002", 1, "T", "a Renewal Home and a dashboard of my scope", "I follow the renewal work",
     [("my unit has renewals due in 30 days", "I open Renewal Home", "the tile counts them and opens the list"),
      ("the budget is 10,000,000.00 and the actual 8,500,000.00", "I open the dashboard", "% vs budget shows 85.00")],
     "SCR-RN-21", "High", 8),
    ("003", 1, "P", "a Processing Dashboard and worklist", "Processing sees its workload",
     [("an account is Submitted for Placement", "I open the dashboard", "it counts in For Placement"),
      ("I am a Processing Officer", "I open In Processing", "I see only my accounts")], "SCR-RN-23", "High", 5),
    ("004", 2, "T", "to generate and initiate the expiry list", "renewals start under control",
     [("a range of March 2027", "I generate", "each account expiring in March is listed once"),
      ("an account is generated", "nobody initiates it", "it stays out of every work list")], "SCR-RN-02", "High", 5),
    ("005", 2, "O", "to create a renewal account by hand", "a missed account still renews",
     [("an expiring Motor account", "I create its renewal", "it carries the client, insurer and PN and is checked"),
      ("a mandatory field is empty", "I save", "every missing field is named")], "SCR-RN-10", "Medium", 3),
    ("006", 2, "O", "duplicates to be detected", "no account renews twice",
     [("the same expiring policy and PN", "a second account is generated", "it is classified Review as potential "
       "duplicate")], "SCR-RN-04", "High", 2),
    ("007", 2, "T", "the checks to run on every event", "eligibility is consistent",
     [("an endorsement is open", "the checks run", "the endorsement check fails"),
      ("the endorsement is posted", "the next event runs the checks", "the check passes")], "SCR-RN-04", "High", 5),
    ("008", 2, "T", "accounts classified Clean, Review or Exception", "work is prioritised",
     [("an account has an open claim", "it is classified", "it is Review, never Clean")], "SCR-RN-22", "High", 3),
    ("009", 2, "O", "the renewal values set by the system", "nothing is keyed twice",
     [("a CBG Motor renewal", "the account is created", "the dates, premiums and taxes are set"),
      ("the premium differs from the expiring one", "I open Computations", "the financial impact shows the "
       "difference")], "SCR-RN-04", "High", 5),
    ("010", 3, "T", "to assign and reassign accounts", "each account has an officer",
     [("accounts of my unit", "I assign them to an officer", "they move to his My Dispositions and he is notified"),
      ("a locked account", "I reassign it", "it is refused")], "SCR-RN-02", "High", 3),
    ("011", 3, "T", "to transfer or refer an account to another unit", "the right unit serves the client",
     [("I choose my own unit", "I transfer", "I see \"The receiving unit must be another unit\""),
      ("a transfer to my unit", "I accept it", "it joins my unassigned list flagged Transferred")], "SCR-RN-09",
     "Medium", 5),
    ("012", 4, "L", "to upload the LAMD and BDOFC/SOLD reports", "loans drive the CBG renewals",
     [("a paid-off PN matches a renewal", "I upload the report", "the renewal is updated by the checks"),
      ("a PN matches two renewals", "I upload", "neither is updated and the row is listed")], "SCR-RN-16", "High", 5),
    ("013", 4, "O", "to record the insurer's disposition by file or by hand", "the workflow follows the insurer",
     [("Renew As Is is uploaded", "the row matches", "the account is Clean and For Renewal"),
      ("a revision without revised values", "I record it", "it is refused")], "SCR-RN-12", "High", 5),
    ("014", 3, "A", "to view and update the account and give the disposition", "I decide on full information",
     [("I have not opened the Account History", "I give the disposition", "I see \"Open the Account History before "
       "you give the disposition\""), ("Not for Renewal without reason", "I save", "it is refused")], "SCR-RN-07",
     "High", 8),
    ("015", 4, "O", "to update accounts in bulk", "large files are processed at once",
     [("500 rows with 3 unknown references", "I upload", "497 accounts are updated and 3 rows refused"),
      ("the file is declared complete", "an account of the range is missing", "it is tagged Not for Renewal")],
     "SCR-RN-10", "Medium", 5),
    ("016", 3, "A", "to submit the account for posting", "my Team Lead reviews it",
     [("a required field is missing", "I submit", "each missing field is named"),
      ("the account is complete", "I submit", "it is Review in Progress for the approver")], "SCR-RN-07", "High", 2),
    ("017", 5, "A", "quick and full proposals sent through CCM", "the client receives the offer",
     [("the disposition is not For Proposal", "I open the proposal", "Generate is disabled"),
      ("an invalid recipient", "I send", "it is refused and nothing is sent")], "SCR-RN-27", "Medium", 5),
    ("018", 3, "A", "to raise a TSU request and take the New Business path", "repriced renewals are quoted",
     [("an account For Quotation", "I submit the TSU request", "it waits for the Team Lead"),
      ("a second request", "I submit", "it is refused")], "SCR-RN-27", "Medium", 5),
    ("019", 5, "A", "to record the client's response to a proposal", "the decision is kept",
     [("the client rejects", "I record it", "the disposition becomes Not for Renewal")], "SCR-RN-27", "Medium", 2),
    ("020", 3, "T", "to review, return, override and post", "only correct renewals move on",
     [("an outstanding premium", "I post", "it is refused until I override with remarks"),
      ("I return without a reason", "I confirm", "it is refused")], "SCR-RN-08", "High", 5),
    ("021", 4, "O", "to send the insurer request per insurer", "insurers get one protected file",
     [("For Renewal accounts of an insurer", "I create and send a batch", "the insurer gets a protected file and "
       "the password separately")], "SCR-RN-11", "High", 3),
    ("022", 5, "A", "to generate and send the Renewal Advice", "the client is advised on time",
     [("an account RA Ready", "I generate the first notice", "the PDF is named by the convention and the account "
       "locks"), ("CCM is down", "I send", "the failure is listed and can be resent")], "SCR-RN-13", "High", 5),
    ("023", 5, "A", "the client letters decided and sent by the system", "closing letters follow the rules",
     [("a confirmed 30-day hold cover", "the policy expires", "no closing letter is sent before the hold cover ends"),
      ("no submission at the checkpoint", "the job runs", "the account is NRNS and gets one reminder")],
     "SCR-RN-13", "High", 5),
    ("024", 5, "A", "every outbound file protected", "client data stays private",
     [("the individual client Juan Dela Cruz", "his file is sent", "the password is JUADEL")], "SCR-RN-33", "High",
     2),
    ("025", 5, "A", "to record the client's acceptance, one or many", "the renewal goes on to placement",
     [("acceptance by payment", "I record it", "the account is For Placement and Booking"),
      ("20 rows with one unknown reference", "I upload acceptances", "19 are recorded")], "SCR-RN-14", "High", 3),
    ("026", 6, "T", "to approve accepted accounts for placement", "placement starts on approved accounts",
     [("a Non-CBG account without payment confirmation", "I approve", "I am asked for the payment confirmation")],
     "SCR-RN-25", "Medium", 2),
    ("027", 6, "O", "CLPC billing files generated and sent", "CBG accounts are billed",
     [("a period of 40 days", "I generate", "it is refused"),
      ("CBG Home accounts 10 days before expiry", "the job runs", "they are in the billing file of their kind")],
     "SCR-RN-30", "Medium", 5),
    ("028", 6, "O", "the payment status from Cashiering", "paid CBG accounts move on",
     [("a payment leaves a zero balance", "it is applied", "the account is Paid and Submitted for Placement")],
     "SCR-RN-26", "High", 2),
    ("029", 6, "O", "to generate and send the placement", "the insurer issues the policy",
     [("an account with two co-insurers", "I generate", "two placement slips are made"),
      ("an insurer enrolled in MFT", "I send", "the files go to its MFT folder")], "SCR-RN-26", "High", 8),
    ("030", 6, "O", "to process placement responses", "accounts move to booking",
     [("an Approved response", "it is uploaded", "the account is For Booking"),
      ("a Rejected response", "it is uploaded", "the account is Rejected Placement with the reason")], "SCR-RN-26",
     "High", 3),
    ("031", 6, "O", "to return an account to Marketing", "errors are corrected",
     [("I return without reason", "I confirm", "it is refused")], "SCR-RN-26", "Medium", 2),
    ("032", 6, "O", "Insurance Advices for mortgaged accounts", "the bank is informed",
     [("a mortgaged account's placement is sent", "the system generates", "the Insurance Advice is attached")],
     "SCR-RN-26", "Medium", 2),
    ("033", 6, "O", "to receive and send e-policies", "clients get their policy",
     [("a ZIP document not in the summary", "the files are received", "that document is refused"),
      ("an account without valid e-mail", "I send", "it is excluded and listed")], "SCR-RN-31", "Medium", 5),
    ("034", 6, "O", "the incentive code set by the system", "incentives are fair",
     [("a booked, fully paid, accepted renewal", "the criteria match", "the incentive code shows; no user can set "
       "it")], "SCR-RN-36", "Low", 2),
    ("035", 6, "O", "booking through New Business", "accounting is consistent",
     [("a renewal is booked", "the booking completes", "it has an invoice number and business type Renewal")],
     "SCR-RN-26", "High", 1),
    ("036", 7, "O", "hold cover requests and extensions", "clients stay covered",
     [("a Non-CBG client confirms", "the request is generated", "the default is 30 days"),
      ("a confirmed hold cover", "I request another", "it is refused")], "SCR-RN-36", "High", 5),
    ("037", 7, "O", "hold cover responses by file or by hand", "the effective expiry is right",
     [("an approved response", "it is recorded", "the account shows Hold Cover confirmed and its end date"),
      ("Rejected without remarks", "I save", "it is refused")], "SCR-RN-36", "High", 3),
    ("038", 8, "B", "to maintain risk codes", "non-renewable risks stop early",
     [("a risk code in the list", "I add it again", "it is refused"),
      ("a change", "another user approves", "it applies from its effective date")], "SCR-RN-29", "High", 3),
    ("039", 1, "A", "KYC due shown without blocking", "compliance follow-up is visible",
     [("a client due for KYC in 20 days", "I open the renewal", "the KYC due flag shows and the classification is "
       "unchanged")], "SCR-RN-28", "Medium", 3),
    ("040", 8, "B", "to maintain checks, rules, the decision matrix and settings", "rules change without a release",
     [("I made a rule version", "I activate it", "it is refused"),
      ("a Clean account matching an Automatic rule", "it is initiated", "it gets For Renewal without a user action")],
     "SCR-RN-18", "High", 5),
    ("041", 8, "B", "to maintain RMU codes", "RMU loans are recognised",
     [("an active code", "I add it again", "it is refused")], "SCR-RN-32", "Low", 1),
    ("042", 8, "B", "to maintain the annual budget", "the dashboard compares with budget",
     [("a negative amount", "I save", "it is refused")], "SCR-RN-35", "Low", 3),
    ("043", 8, "X", "an audit log of every Renewal action", "I can trace every step",
     [("a renewed account", "I open Audit Logs", "every step shows with user, time, old and new values")],
     "SCR-RN-34", "High", 2),
    ("044", 8, "T", "the Renewal reports", "I report on the portfolio",
     [("Risk Code all except MC01", "I run the Renewal Status Report", "MC01 is left out and the counters show"),
      ("an IBG account not disposed 59 days before expiry", "I run the listing", "it carries the ageing flag")],
     "SCR-RN-21", "High", 5),
    ("045", 8, "B", "the go-live take-over of expiring renewals", "nothing is lost at cut-over",
     [("migrated policies expiring to 31-May-2028", "the take-over runs", "each is listed once; January 2028 is "
       "Urgent"), ("a tracker row of a sent RA", "a second member approves", "it is recorded as sent")],
     "SCR-RN-20", "High", 5),
    ("046", 8, "O", "to choose the package of a migrated policy", "migrated renewals are priced right",
     [("an unmapped package", "I choose a version and approve it myself", "it is refused")], "SCR-RN-19", "Medium",
     3),
    ("047", 5, "C", "to record follow-ups without premium data", "clients are followed up",
     [("a call outcome", "I save it", "the Account Officer sees it"), ("I open the list", "it loads",
                                                                        "no premium column shows")],
     "SCR-RN-15", "Medium", 2),
    ("048", 2, "O", "submitted policies renewed from their hand-off", "submitted policies renew",
     [("a submitted CBG Motor policy is handed over", "it arrives", "one renewal, one hold cover request and one "
       "Renewal Advice are made")], "SCR-RN-02", "Medium", 3),
    ("049", 8, "B", "to maintain the lists of values and letter templates", "wording follows BDOI",
     [("a new reason effective next month", "I choose it today", "it is not offered"),
      ("a new template version", "an RA is generated", "it uses the new version")], "SCR-RN-17", "Medium", 3),
]


def _items(entry: str) -> str:
    return ", ".join(r[0] for f in C.FRUM if f["id"] == entry for r in f["reqs"])


_RULES = {}
for _f in C.FRUM:
    _rs = sorted({x for r in _f["reqs"] for x in r[4].replace(";", ",").split(",") if x.strip().startswith("BR-")})
    _RULES[_f["id"]] = ", ".join(x.strip() for x in _rs) or "-"

STORIES = [(f"US-RN-{n}", f"EP-RN-{e:02d}", _P[p], want, so, acs, _items(f"FRRN.{n}"), _RULES[f"FRRN.{n}"], scr,
            prio, pts) for n, e, p, want, so, acs, scr, prio, pts in _S]
REPORT_AC = []

README = {
    "intro": ("This pack holds every document of Renewal (BRD-06). Each document has one job. A requirement or rule "
              "is stated once, in the FRS, with its ID; the other documents refer to that ID."),
    "order": ["FRS", "BPS", "UXD", "RNO", "ICD", "DSP", "DDI", "USR", "RTM"],
    "programme": [
        ("Non-Functional and Security Requirements", "Performance, availability, security, audit, retention, backup, "
         "accessibility and browsers for every BRD."),
        ("Glossary", "Terms and abbreviations used in every pack."),
        ("Decision and Observation Log", "Decisions taken and observations raised across the BRDs."),
        ("Document Register", "Every current document of the BRD packs with version, date, owner and status."),
    ],
    "ids": [("FRRN.nnn.nn", "Functional requirement (FRS)"), ("BR-RN-nn", "Business rule (FRS)"),
            ("SCR-RN-nn", "Screen (UX Design)"), ("SB-nn, P-RN-nn", "Storyboard, process (Process and Storyboards)"),
            ("RPT-RN-nn, DO-nn, NT-nn", "Report, document, notice (Reports, Notifications and Outputs)"),
            ("IF-RN-nn", "Interface (Interface Specification)"), ("US-RN-nnn", "User story (User Stories)"),
            ("TC-RN-…", "Test case (Test Cases and Traceability)"), ("CR-RN-nnn", "Change request (CR Register)"),
            ("NFR-…", "Non-functional requirement (programme NFR)"), ("D-nnn / O-nnn", "Decision or observation "
             "(programme log)")],
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
                problems.append(f"{s[0]}: unknown FRRN {ref}")
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
        problems.append(f"FRRN items without a user story: {sorted(frum - covered)}")
    return problems

# Texts of the schema reworded for the data dictionary (business words).
DDI_TEXT = {"Built in": "Built-in billing file (amortized accounts)",
            "the CCM interface at the endpoint below": "the CCM interface at the address below"}
