"""BRD-03 Product Maintenance: content of the standard document set that is not already in brd03_v16_content.py,
brd03_v16_pages.py and brd03_v16_screens.py. Read by build_pack.py (developer specification DSP, data dictionary DDI,
user stories USR, the "How to read this pack" note) and by build_brd03_v16_workbooks.py (traceability).

The facts of the services, screens and tables are read from the sources by tools/deliverables/api_facts.py and
schema_facts.py; this module says which of them belong to Product Maintenance and gives them their business wording.
"""

from __future__ import annotations

import api_facts as af
import brd03_v16_content as C
import brd03_v16_screens as UX

BRD = "BRD-03"
NAME = "Product Maintenance"
SHORT = "Product Maintenance"
FOLDER = "BRD-03_Product_Maintenance"
DATE = C.DATE
DATE_US = C.DATE_US
PREPARED = "iorta TechNXT Project Team"
HEADER_RIGHT = C.HEADER_RIGHT
DOC_NO = "PRJ0012475-PM"

SET = [
    ("README", "Inputs", "How_to_Read_this_Pack", "1.0", "This note: the documents of the pack and who reviews them.",
     "All reviewers"),
    ("FRS", "FRS-BDOI", "Product_Maintenance", C.VERSION, "Requirements, business rules, validations, personas and "
     "permissions, parameters and lists of values, statuses.", "BDOI Business Unit owners (TSU, MBS, Retail "
     "Marketing)"),
    ("BPS", "BPS-BDOI", "Product_Maintenance_Process_and_Storyboards", C.VERSION, "To-be process flows and "
     "storyboards.", "BDOI Business Unit owners"),
    ("UXD", "UXD-BDOI", "Product_Maintenance_UX_Design", C.UXD_VERSION, "Every screen: purpose, users, fields, actions, "
     "states, messages, one screenshot per page.", "BDOI Business Unit owners; UAT lead"),
    ("RNO", "RNO-BDOI", "Product_Maintenance_Reports_Notifications_Outputs", C.VERSION, "Reports, documents, exports, "
     "notices, e-mails and jobs.", "BDOI Business Unit owners; Internal Audit"),
    ("ICD", "ICD-BDOI", "Product_Maintenance_Interface_Specification", C.ICD_VERSION, "Product master transfer file "
     "and e-mail channel.", "BDOI IT (integration, mail)"),
    ("DSP", "DSP-BDOI", "Product_Maintenance_Developer_Specification", "1.4", "Screens, service operations, field "
     "validations, rules, states, jobs, audit events and permissions, as the system works.", "BDOI IT; iorta "
     "TechNXT developers and testers"),
    ("DDI", "DDI-BDOI", "Product_Maintenance_Data_Dictionary", "1.2", "Tables, columns, lists of values, parameters, "
     "status models and indexes.", "BDOI IT (database administration); Data Migration"),
    ("USR", "USR-BDOI", "Product_Maintenance_User_Stories", "1.3", "User stories with acceptance criteria, ready to "
     "import into the backlog tool.", "BDOI Product Owner; UAT lead"),
    ("RTM", "RTM", "Test_Cases_and_Traceability", C.WB_VERSION, "Test cases and the trace BRD, FRPM, user story, "
     "screen, test case.", "UAT lead; Internal Audit"),
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
SET.append(("ACX", "Access", "Access_Matrix", "1.0", "Group profile x screen x action, from the permissions, group profiles, menus and server checks of the release.", "BDOI Information Security; UAT lead"))
EXT["ACX"] = "xlsx"
SET.append(("SRX", "SRS", "Product_Maintenance_System_Requirements_Extract", "1.2", "The chapter of the BIBS System Requirements Specification v1.2 for this module: system requirements traced to the FRS, common requirements, access and acceptance.", "BDOI Product Owner; BDOI IT"))
EXT["SRX"] = "docx"


def file_name(key: str) -> str:
    kind, title, ver = next((k2, t, v) for k, k2, t, v, *_ in SET if k == key)
    return f"BIBS_{kind}_{BRD}_{title}_v{ver}.{EXT[key]}"


def version(key: str) -> str:
    return next(v for k, _, _, v, *_ in SET if k == key)


# Revisions of the Developer Specification (pack_dsp.py).
DSP_REVISIONS = [
    (DATE, "1.0", "First issue. Describes the screens, service operations, validations, rules, states, jobs, audit "
     f"records and permissions of the release of {DATE}.", PREPARED),
    (DATE, "1.1", "Permissions of the service operations corrected: the request routing, quotation rounds, responses "
     "and comparative outputs are checked against PRODUCT_VIEW or PKG_REPORT_VIEW, not JOURNAL_VIEW (Decision Log "
     "O-009).", PREPARED),
    ("16 October 2026", "1.2", "Insurer page completed (Conformance Register gap G08): contact persons, payment "
     "terms and remittance details, bank accounts and the products linked, each change maker-checker. Field-level "
     "change history of the master records (gap G05): old and new value, maker, checker and their times, shown on "
     "the insurer and product pages and readable by the Auditor.", PREPARED),
    ("17 October 2026", "1.3", "Ledger Accounts tab of the insurer page (gap G08): the general ledger accounts "
     "posted for the insurer and its sub-ledger open items, read-only. Master Data Change Log screen (gap G05) with "
     "filters and Excel export; the change history follows the reader's data access.", PREPARED),
    ("17 October 2026", "1.4", "Co-insurance / Risk Participation tab of the insurer page (BRD-00 CRQ08): the "
     "participations of the insurer's default co-insurance arrangement, maker-checker, and their use as the default "
     "co-insurance split at booking.", PREPARED),
]

DOCS = {
    "DSP": {"cover": "Product Maintenance: Developer Specification", "header_left": "Developer Specification",
            "doc_no": f"{DOC_NO}-DSP"},
    "README": {"cover": "Product Maintenance: How to Read this Pack", "header_left": "How to Read this Pack",
               "doc_no": f"{DOC_NO}-README"},
}

# ================================================================================================ developer spec
PACKAGES = ["productmaint", "catalog"]
MESSAGE_PACKAGES = ["productmaint", "catalog"]
SETTING_CLASSES = ("SystemParameter",)
_PM = "productmaint/api/"
_CAT = "catalog/api/"
CONTROLLERS = [_PM + c for c in (
    "PackageRequestController.java", "PackageRoutingController.java", "NegotiationController.java",
    "TermsController.java", "PackageOutputsController.java", "AdvisoryExpiryController.java",
    "DeactivationController.java", "MasterChangeController.java", "PlacementUpdateController.java",
    "PmWorkspaceController.java", "PmAuditController.java")] + [_CAT + c for c in (
        "ProductCatalogController.java", "ProductVersionController.java", "CatalogRecordController.java",
        "CatalogRuleController.java", "CoverageController.java", "IncentiveCriteriaController.java",
        "InsurerController.java", "RateTableController.java", "OtherChargeController.java", "RatingController.java",
        "RateSchemeExceptionController.java", "SalesOrganisationController.java")]
SHARED_CONTROLLERS = ["workflow/api/WorkflowController.java", "report/api/ReportController.java",
                      "audit/api/ChangeHistoryController.java",
                      "attachment/api/AttachmentController.java", "nonpackage/api/ProposalController.java"]
OMIT_OPS: set[str] = set()

DSP_TEXT = {
    "requirement_ids": "FRPM, BR-PM",
    "screen_ids": "SCR-PM",
    "interfaces": "product master transfer file and e-mail channel",
    "integration_prefix": "",
    "service_ops": "The product master transfer file is detailed in the Interface Specification.",
    "status_note": "Masters (coverages, clauses, field rules, insurers, rates, incentive criteria, sales units) follow "
                   "the maker-checker statuses of the data dictionary (sheet Status models).",
    "jobs_note": "Send Now on Product Master Transfers starts PRODUCT_MASTER_TRANSFER.",
    "audit_note": "Package requests also keep their workflow history (stage, action, user, time, comment).",
    "security_note": "Package documents open only for users with access to the package request.",
    "omit_classes": ("Simulator", "Retention", "Seed"),
}

AREAS = [
    ("Package requests", ("product-maintenance/requests", "product-maintenance/prefill", "workflow/"),
     "Requests, approvals, assignment, requirements, ManCom routing, set-up hand-over."),
    ("Negotiation and comparative tables", ("product-maintenance/terms", "product-maintenance/comparatives"),
     "Rounds, slips, responses, comparative tables, final terms, proposal slips."),
    ("Advisories, expiry and deactivation", ("product-maintenance/advisories", "product-maintenance/expiry",
                                             "product-maintenance/deactivations", "product-maintenance/renewals"),
     "Advisories, expiry list, renewal requests, deactivation requests."),
    ("Workspace, reports and audit", ("product-maintenance/matrix", "product-maintenance/quotation-requests",
                                      "product-maintenance/dashboard", "product-maintenance/home",
                                      "product-maintenance/placement", "product-maintenance/audit",
                                      "product-maintenance/master-changes", "reports/", "attachments/",
                                      "change-history"),
     "Landing page, dashboard, Placement Update Reports, audit logs, product master transfers."),
    ("Product catalogue", ("catalog/",), "Lines, cover types, products, versions, coverages, clauses, rules, "
     "insurers, rates, incentive criteria, rate exceptions, sales organisation, rating."),
    ("Proposal requests (New Business)", ("proposals",), "Non-package proposal requests used by SCR-PM-28."),
]

COMPONENTS = [
    ("Product Maintenance screens", "Twenty-nine screens and tabs under Client & Policy › Product Maintenance.",
     "Section 3"),
    ("Package request service", "Requests, workflow, negotiation, comparative tables, requirements, ManCom, "
     "advisories, expiry, deactivation.", "Section 4"),
    ("Product catalogue service", "Hierarchy, products, versions, insurer terms, rules, insurers, rates, incentive "
     "criteria, rate exceptions, premium calculation.", "Section 4"),
    ("Product master transfer", "File of product master changes for other BDOI systems.",
     "Interface Specification v1.0"),
    ("Audit trail", "Insert-only record of every action.", "Section 8"),
    ("Scheduled jobs", "Expiry monitor, version lifecycle, deactivation, Placement Update Report, master transfer.",
     "Section 7"),
]

_ROUTES = {
    "SCR-PM-01": ("product-maintenance", ("productmaint", "ProductMaintenanceHomePage.tsx")),
    "SCR-PM-02": ("product-maintenance/requests", ("productmaint", "PackageRequestsPage.tsx")),
    "SCR-PM-03": ("product-maintenance/requests/new", ("productmaint", "PackageRequestFormPage.tsx")),
    "SCR-PM-04": ("product-maintenance/requests/:id", ("productmaint", "PackageRequestPage.tsx")),
    "SCR-PM-05": ("product-maintenance/requests/:id", ("productmaint", "NegotiationTab.tsx")),
    "SCR-PM-06": ("product-maintenance/requests/:id", ("productmaint", "ComparativeTab.tsx")),
    "SCR-PM-07": ("product-maintenance/requests/:id", ("productmaint", "RequirementsTab.tsx")),
    "SCR-PM-08": ("product-maintenance/requests/:id", ("productmaint", "AdvisoriesTab.tsx")),
    "SCR-PM-09": ("product-maintenance/tsu", ("productmaint", "PackageTsuWorkbenchPage.tsx")),
    "SCR-PM-10": ("product-maintenance/expiry", ("productmaint", "PackageExpiryPage.tsx")),
    "SCR-PM-11": ("catalog/products", ("catalog", "ProductsPage.tsx")),
    "SCR-PM-12": ("catalog/products/:code", ("catalog", "ProductDetailPage.tsx")),
    "SCR-PM-13": ("catalog/products/:code/versions/:versionNo", ("catalog", "VersionEditorPage.tsx")),
    "SCR-PM-14": ("catalog/validation", ("catalog", "ValidationQueuePage.tsx")),
    "SCR-PM-15": ("catalog/coverages", ("catalog", "CoveragesPage.tsx")),
    "SCR-PM-16": ("catalog/incentives", ("catalog", "IncentivesPage.tsx")),
    "SCR-PM-17": ("catalog/insurers", ("catalog", "InsurersPage.tsx")),
    "SCR-PM-18": ("catalog/insurers/:id", ("catalog", "InsurerDetailPage.tsx")),
    "SCR-PM-19": ("catalog/rates", ("catalog", "RatesPage.tsx")),
    "SCR-PM-20": ("catalog/sales-organisation", ("catalog", "SalesOrganisationPage.tsx")),
    "SCR-PM-21": ("catalog/calculator", ("catalog", "PremiumCalculatorPage.tsx")),
    "SCR-PM-22": ("catalog/rate-exceptions/:reference", ("catalog", "RateExceptionPage.tsx")),
    "SCR-PM-23": ("product-maintenance/dashboard", ("productmaint", "workspace/PmDashboardPage.tsx")),
    "SCR-PM-24": ("product-maintenance/deactivations", ("productmaint", "workspace/DeactivationsPage.tsx")),
    "SCR-PM-25": ("product-maintenance/master-changes", ("productmaint", "workspace/MasterChangesPage.tsx")),
    "SCR-PM-26": ("product-maintenance/placement-reports", ("productmaint", "placement/PlacementReportsPage.tsx")),
    "SCR-PM-27": ("product-maintenance/audit-logs", ("productmaint", "workspace/PmAuditLogsPage.tsx")),
    "SCR-PM-28": ("proposals/:id", ("productmaint", "terms/TermsTablePanel.tsx")),
    "SCR-PM-29": ("product-maintenance/requests/:id", ("productmaint", "workspace/RoutingTab.tsx")),
}
_PERMISSION = {"SCR-PM-05": ("PRODUCT_VIEW", ["PKG_REPORT_VIEW"]), "SCR-PM-06": ("PRODUCT_VIEW", ["PKG_REPORT_VIEW"]),
               "SCR-PM-07": ("PRODUCT_VIEW", ["PKG_REPORT_VIEW"]), "SCR-PM-08": ("PRODUCT_VIEW", ["PKG_REPORT_VIEW"]),
               "SCR-PM-29": ("PRODUCT_VIEW", ["PKG_REPORT_VIEW"])}
_CODES = {
    "SCR-PM-03": ["PKG_REQUEST_INCOMPLETE", "PKG_COVER_TYPE_INVALID", "PKG_LINE_MISMATCH", "PKG_PRODUCT_NOT_PACKAGED",
                  "PKG_TYPE_FIXED", "PKG_MIN_POLICIES", "PKG_MIN_PREMIUM", "PKG_INCENTIVE_REQUIRED",
                  "PKG_INCENTIVE_AMOUNT", "PKG_INCENTIVE_RATE", "PKG_SOURCE", "PKG_REQUEST_NOT_EDITABLE"],
    "SCR-PM-04": ["PKG_FOUR_EYES", "PKG_MARKETING_LEVEL", "PKG_RECOMMENDATION_REQUIRED", "PKG_SCOPE_CLIENT",
                  "PKG_SCOPE_GENERIC", "PKG_REQUEST_TYPE_MISMATCH", "PRODUCT_CODE_REQUIRED", "VERSION_IN_PROGRESS"],
    "SCR-PM-05": ["QS_NO_INSURER", "QS_FOUR_EYES", "QS_ALREADY_SENT", "QS_NOT_RESENDABLE", "QS_LOCKED",
                  "INSURER_NO_EMAIL", "PKG_RESPONSE_RATE_REQUIRED", "PKG_AMOUNT_NEGATIVE", "PKG_ROUND_CLOSED",
                  "NEGOTIATION_INCOMPLETE", "PKG_INSURER_NOT_OFFERED", "PKG_SHARES_INVALID"],
    "SCR-PM-06": ["COMPARATIVE_NO_TERMS", "COMPARATIVE_NO_MASTER", "COMPARATIVE_TITLE_REQUIRED",
                  "COMPARATIVE_NO_INSURER", "COMPARATIVE_FIELD_UNKNOWN"],
    "SCR-PM-07": ["REQUIREMENTS_INCOMPLETE", "PKG_DATES_INVALID", "PKG_DEPLOYMENT_NOT_READY"],
    "SCR-PM-08": ["ADVISORY_DOCUMENTS_MISSING", "ADVISORY_NO_RECIPIENT", "ADVISORY_CONTENT_REQUIRED",
                  "ADVISORY_ALREADY_SENT"],
    "SCR-PM-10": ["PKG_RENEWAL_NONE"],
    "SCR-PM-11": ["PM_APPROVER", "PM_DIRECT_MAINTENANCE_OFF", "PACKAGE_HIERARCHY_INCOMPLETE", "PRODUCT_CODE_PATTERN",
                  "PRODUCT_FIELD_VERSIONED"],
    "SCR-PM-13": ["PACKAGE_RATE_MISSING", "PACKAGE_SHARES_INVALID", "PACKAGE_INSURER_TERMS_MISSING",
                  "PACKAGE_DATES_INVALID", "VERSION_NOT_FOR_VALIDATION"],
    "SCR-PM-15": ["COVER_TYPE_DEPTH", "DUPLICATE", "EFFECTIVITY_INVALID", "FIELD_RULE_INCOMPLETE"],
    "SCR-PM-16": ["INCENTIVE_PRODUCTS_REQUIRED", "INCENTIVE_PRODUCT_NOT_ACTIVE", "INCENTIVE_PERIOD_OVERLAP",
                  "INCENTIVE_ACTIVE_LOCKED", "INCENTIVE_VALUE_REQUIRED"],
    "SCR-PM-17": ["PARTY_NOT_INSURER", "CREDIT_DAYS_INVALID", "PLACEMENT_CHANNEL_PARKED"],
    "SCR-PM-19": ["RATE_INVALID", "SHORT_PERIOD_MONTHS_INVALID", "CHARGE_VALUE_INVALID"],
    "SCR-PM-20": ["SALES_PARENT_INVALID", "SALES_UNIT_IN_USE", "REASON_REQUIRED"],
    "SCR-PM-21": ["RATING_NO_ITEMS", "RATING_PERIOD_INVALID", "RATING_RATE_REQUIRED", "INSURER_NOT_ON_PACKAGE"],
    "SCR-PM-22": ["RATE_SCHEME_NOT_CURRENT", "MAKER_CHECKER_VIOLATION", "RATE_EXCEPTION_REASON_REQUIRED",
                  "RECORD_NOT_PENDING"],
    "SCR-PM-24": ["PKG_DEACTIVATION_DATE", "PKG_DEACTIVATION_REASON", "PKG_DEACTIVATION_APPROVER",
                  "PKG_DEACTIVATION_PENDING", "PKG_DEACTIVATION_REMARKS", "PKG_DEACTIVATION_DECIDED"],
    "SCR-PM-25": ["PM_MASTER_NOT_FAILED"],
    "SCR-PM-27": ["DATE_RANGE"],
    "SCR-PM-28": ["PM_TERMS_ANSWER", "PM_TERMS_OTHER", "PM_FINAL_TERMS", "PM_PROPOSAL_INSURER",
                  "PM_PROPOSAL_NO_TERMS", "PM_CLIENT_RESPONSE", "PM_CLIENT_REMARKS"],
    "SCR-PM-29": ["PKG_MANCOM_APPROVERS", "PKG_MANCOM_NOT_YOURS", "PKG_MANCOM_REMARKS", "PKG_MANCOM_STAGE"],
}


def _screens() -> list[dict]:
    out = []
    for s in UX.SCREENS:
        route, page = _ROUTES[s["id"]]
        d = {"id": s["id"], "name": s["name"], "route": route, "uxd": s["id"], "frum": s["frpm"], "page": page,
             "loads": s["purpose"], "states": s["rules"][:4] or ["-"], "fields": [], "codes": _CODES.get(s["id"], [])}
        if s["id"] in _PERMISSION:
            d["permission_override"] = _PERMISSION[s["id"]]
        if s["id"] == "SCR-PM-04":
            d["extra"] = [("productmaint", "RequestTabs.tsx")]
        out.append(d)
    return out


SCREENS = _screens()
UX_INPUTS: dict[str, str] = {}

# Calls of the screen clients that the reader cannot resolve: the deactivation client builds its paths from a
# variable; option lists, quotation search and comparative files are platform calls described elsewhere.
_DEACT = "/product-maintenance/deactivations"
_DROP = {("lovApi", "options"), ("quotationsApi", "search"), ("productMaintApi", "outputFile"),
         ("systemApi", "settings")}


def fix_call(c: af.ClientCall) -> af.ClientCall | None:
    if (c.obj, c.fn) in _DROP:
        return None
    if c.obj == "deactivationApi":
        path = {"settings": "/settings", "submit": "", "list": "", "get": "/{x}", "approve": "/{x}/approve",
                "reject": "/{x}/reject", "cancel": "/{x}/cancel", "reassign": "/{x}/reassign"}.get(c.fn)
        return None if path is None else af.ClientCall(c.obj, c.fn, c.verb, _DEACT + path)
    return c


ACTION_LABELS = {
    "create": "Save Draft (new)", "update": "Save Draft", "submit": "Submit for Approval", "approve": "Approve",
    "recommend": "Recommend for Approval", "approveTsu": "Approve for Negotiation", "prepare": "Save Slip",
    "submitSlip": "Submit Slip for Approval", "approveSlip": "Approve and Send", "resend": "Resend",
    "revise": "Revise Quotation Slip", "recordResponse": "Key In", "termsFinal": "Terms Final",
    "compileMaster": "Compile Master", "clientView": "Client View", "releaseToMarketing": "Release to Marketing",
    "skipMarketingReview": "Proceed to Requirements", "acceptTerms": "Accept Terms",
    "updateRequirements": "Save Requirements", "submitRequirements": "Submit Requirements for ManCom Sign-off",
    "selectManCom": "Send for ManCom Approval", "decideManCom": "Approve / Return / Reject (ManCom)",
    "requestDeployment": "Request for Deployment", "setup": "Set Up Package Version",
    "returnIncomplete": "Return Incomplete Requirements", "retire": "Retire Package", "signoff": "Sign Off",
    "updateAdvisory": "Save Advisory", "sendAdvisory": "Send Advisory", "assign": "Assign", "claim": "Claim",
    "transfer": "Send Now", "reprocess": "Process Again", "export": "Export",
}
DATA_LABELS = {"search": "Requests or records found", "get": "The record", "matrix": "Product Matrix",
               "quotationRequests": "Quotation requests", "lines": "Lines of insurance", "rounds": "Negotiation rounds",
               "requirements": "Product requirements", "advisories": "Advisories", "routing": "Routing and approvals",
               "outputs": "Comparative outputs", "comparative": "Comparative table of a round"}
PERMISSION_PREFIXES = ("PKG_", "PRODUCT_", "MASTER_", "INCENTIVE_", "PROPOSAL_", "AUDIT_")

PERSONA_ROLES = [
    ("Marketing Account Officer", "MKT_AO"), ("Marketing Approver", "MKT_TL"), ("TSU Officer", "TSU"),
    ("TSU Team Lead", "TSU_TL"), ("TSU Head", "TSU_HEAD"), ("ManCom Member", "MANCOM"), ("MBS", "MBS"),
    ("Business Administrator", "BUSINESS_ADMIN"), ("Auditor", "AUDITOR"),
]

RULE_ENFORCEMENT = [
    ("BR-PM-01", "Decision buttons hidden from the maker.", "PKG_FOUR_EYES; QS_FOUR_EYES; MAKER_CHECKER_VIOLATION; "
     "PM_APPROVER.", "Maker, submitter and decider recorded on each record."),
    ("BR-PM-02", "Version history on the product.", "-", "Daily job PACKAGE_VERSION_LIFECYCLE supersedes the "
     "previous version."),
    ("BR-PM-03", "Rate scheme notice on the quotation.", "RATE_SCHEME_NOT_CURRENT; INSURER_NOT_ON_PACKAGE.",
     "Version number stored on quotation, account and invoice."),
    ("BR-PM-04", "-", "PACKAGE_SHARES_INVALID; PKG_SHARES_INVALID; PACKAGE_INSURER_TERMS_MISSING.", "-"),
    ("BR-PM-05", "No delete action.", "-", "Records end-dated or expired; audit trail insert-only."),
    ("BR-PM-06", "Expired tab only for authorised users.", "-", "Lists filtered by permission."),
    ("BR-PM-07", "Slip actions hidden after sign-off.", "QS_LOCKED.", "-"),
    ("BR-PM-08", "Request for Deployment only after approval.", "PKG_DEPLOYMENT_NOT_READY.",
     "Setting PM_DEPLOYMENT_REQUEST."),
    ("BR-PM-09", "Validate and Release needs every checklist item.", "VERSION_NOT_FOR_VALIDATION.",
     "Only RELEASED versions are rated."),
    ("BR-PM-10", "Overdue and due-soon marking.", "-", "Service-level settings PKG_SLA_*; overdue alert."),
    ("BR-PM-11", "-", "INSURER_NO_EMAIL.", "Every e-mail with an attachment is protected; password e-mail queued."),
    ("BR-PM-12", "Renewal and set-up actions hidden when one is open.", "PKG_RENEWAL_NONE; VERSION_IN_PROGRESS.", "-"),
    ("BR-PM-13", "-", "INCENTIVE_PERIOD_OVERLAP; INCENTIVE_PRODUCT_NOT_ACTIVE.", "-"),
    ("BR-PM-14", "Send Advisory disabled until documents are attached.", "ADVISORY_DOCUMENTS_MISSING.", "-"),
]

_STAGE_ROWS = [
    ("DRAFT", "Draft request", "Save Draft", "FOR_MKT_APPROVAL, FOR_TSU_REVIEW, VOIDED"),
    ("FOR_MKT_APPROVAL", "For Marketing approval", "Submit for Approval (Marketing source)",
     "FOR_TSU_REVIEW, DRAFT"),
    ("FOR_TSU_REVIEW", "For TSU review", "Approve (Marketing); Submit (TSU or insurer source)",
     "FOR_TSU_APPROVAL, DRAFT"),
    ("FOR_TSU_APPROVAL", "For TSU Head approval", "Recommend for Approval",
     "NEGOTIATION, REQUIREMENTS_PREP, NOT_PROCEEDED"),
    ("NEGOTIATION", "Insurer negotiation", "Approve for Negotiation", "TERMS_REVIEW, NOT_PROCEEDED"),
    ("TERMS_REVIEW", "Negotiated terms review", "Terms Final", "FOR_MKT_REVIEW, REQUIREMENTS_PREP"),
    ("FOR_MKT_REVIEW", "Marketing review of terms", "Release to Marketing", "REQUIREMENTS_PREP, NEGOTIATION"),
    ("REQUIREMENTS_PREP", "Requirements preparation", "Proceed to Requirements; Accept Terms", "FOR_MANCOM"),
    ("FOR_MANCOM", "For ManCom sign-off", "Submit Requirements for ManCom Sign-off",
     "WITH_MBS, MANCOM_APPROVED, REQUIREMENTS_PREP"),
    ("MANCOM_APPROVED", "ManCom approved, deployment to request", "Last ManCom approval (setting MANUAL)",
     "WITH_MBS"),
    ("WITH_MBS", "With MBS for set-up", "Last ManCom approval (AUTO) or Request for Deployment",
     "FOR_VALIDATION, REQUIREMENTS_PREP, RETIRED"),
    ("FOR_VALIDATION", "Package version for validation", "Submit for Validation (version)", "RELEASED, WITH_MBS"),
    ("RELEASED", "Released", "Validate and Release", "-"),
    ("RETIRED", "Package retired", "Retire Package", "-"),
    ("NOT_PROCEEDED", "Not proceeded", "Close as Not Proceeded", "-"),
    ("VOIDED", "Voided", "Void", "-"),
]
WORKFLOWS = {
    "Package request": ("productmaint/RequestStage", _STAGE_ROWS),
    "Package version": ("catalog/ProductVersionStatus", [
        ("DRAFT", "Draft", "New Version; Set Up Package Version; Return", "FOR_VALIDATION"),
        ("FOR_VALIDATION", "For Validation", "Submit for Validation", "RELEASED, DRAFT"),
        ("RELEASED", "Released", "Validate and Release", "SUPERSEDED, EXPIRED"),
        ("SUPERSEDED", "Superseded", "Release of a newer version (daily lifecycle)", "-"),
        ("EXPIRED", "Expired", "Package end date passed (daily lifecycle)", "-")]),
    "Deactivation request": ("productmaint/DeactivationStatus", [
        ("PENDING", "Pending Approval", "Submit", "APPROVED, REJECTED, CANCELLED"),
        ("APPROVED", "Approved", "Approve", "-"), ("REJECTED", "Rejected", "Reject", "-"),
        ("CANCELLED", "Cancelled", "Withdraw", "-")]),
    "Quotation slip of a round": ("productmaint/RoundStatus", [
        ("PREPARATION", "Preparation", "Revise Quotation Slip; approval of the request", "FOR_APPROVAL"),
        ("FOR_APPROVAL", "For approval", "Submit Slip for Approval", "SENT, PREPARATION"),
        ("SENT", "Sent", "Approve and Send", "CLOSED"),
        ("CLOSED", "Closed", "Next round or Terms Final", "-")]),
}
STATUSES = C.STATUSES

AUDIT_LABELS = {
    "CatalogKind.CLAUSE.label()": "Clause", "CatalogKind.COVERAGE.label()": "Coverage",
    "CatalogKind.DOCUMENT_RULE.label()": "Document rule", "CatalogKind.FIELD_RULE.label()": "Field rule",
    "CatalogKind.INSURER.label()": "Insurer", "CatalogKind.INSURER_BRANCH.label()": "Insurer branch",
    "CatalogKind.INSURER_CONTACT.label()": "Insurer contact",
    "CatalogKind.INSURER_BANK_ACCOUNT.label()": "Insurer bank account",
    "CatalogKind.INSURER_PARTICIPATION.label()": "Co-insurance participation",
    "CatalogKind.OTHER_CHARGE.label()": "Other charge", "CatalogKind.PRODUCT.label()": "Product",
    "CatalogKind.RATE_SCHEME_EXCEPTION.label()": "Rate exception", "CatalogKind.SALES_OFFICER.label()":
    "Sales officer", "CatalogKind.SALES_UNIT.label()": "Sales unit", "CatalogKind.TSU_RULE.label()": "TSU rule",
    "PRODUCT.strip()": "Product", "Package advisory": "Package advisory", "Package version": "Package version",
    "PackageDeactivation": "Package deactivation", "ProductMasterTransfer": "Product master transfer",
    "The commission rate": "Commission rate", "kind.label()": "Catalogue record", "record.type()":
    "Comparative table or proposal",
}
AUDIT_OMIT: set[str] = set()
ACTION_WORDS = {"CREATE": "Created", "UPDATE": "Changed", "DEACTIVATE": "Deactivated", "SUBMIT": "Submitted",
                "AUTHORIZE": "Authorised", "REJECT": "Rejected", "EXPORT": "Exported", "RUN": "Run",
                "APPROVE": "Approved", "DELETE": "Removed", "SEND": "Sent"}
JOBS = {j[0]: j[2] for j in __import__("brd03_v16_pages").JOBS}
JOB_OMIT: set[str] = set()

# ================================================================================================ data dictionary
TABLE_PATTERN = r"^(pm_|cat_)"
TABLES = {
    "cat_clause": ("Product catalogue", "Clause library: warranties, clauses, exclusions, deductible wordings.", "300",
                   "400"),
    "cat_commission_rate": ("Product catalogue", "Commission rate by insurer and product, effective-dated.", "1,500",
                            "1,800"),
    "cat_cover_type": ("Product catalogue", "Cover types and subtypes of each line.", "80", "90"),
    "cat_coverage": ("Product catalogue", "Coverages and perils of each line.", "400", "450"),
    "cat_document_rule": ("Product catalogue", "Mandatory documents by line or product.", "100", "120"),
    "cat_field_rule": ("Product catalogue", "Field rules (presence, list, range, pattern) by line or product.", "200",
                       "250"),
    "cat_incentive_criteria": ("Product catalogue", "Incentive eligibility criteria, effective-dated.", "20", "40"),
    "cat_incentive_criteria_product": ("Product catalogue", "Products of each incentive criterion.", "200", "400"),
    "cat_insurer": ("Product catalogue", "Insurer profiles: placement channel and e-mails, tax status, credit days, "
                    "payment terms and remittance details.", "60", "65"),
    "cat_insurer_bank_account": ("Product catalogue", "Bank accounts of each insurer by purpose (remittance, refunds, "
                                 "commissions), maker-checker.", "120", "130"),
    "cat_insurer_branch": ("Product catalogue", "Branches of each insurer.", "150", "160"),
    "cat_insurer_contact": ("Product catalogue", "Contact persons of each insurer by role, maker-checker.", "300",
                            "330"),
    "cat_insurer_participation": ("Product catalogue", "Default co-insurance participation of each insurer: "
                                  "participating insurer, share %, lead flag, effective dates, maker-checker.",
                                  "60", "80"),
    "cat_motor_limit": ("Product catalogue", "Motor liability limits and premiums.", "40", "40"),
    "cat_other_charge": ("Product catalogue", "Other charges billed with the premium (switched off by default).", "10",
                         "10"),
    "cat_package_coverage": ("Product catalogue", "Coverages of a package version.", "2,000", "2,600"),
    "cat_package_insurer": ("Product catalogue", "Insurers of a package version: role, share, rate, minimum premium.",
                            "600", "800"),
    "cat_package_insurer_term": ("Product catalogue", "Terms per insurer and coverage of a version.", "4,000", "5,500"),
    "cat_product": ("Product catalogue", "Products (risk codes): package or non-package, line, lifecycle.", "400",
                    "450"),
    "cat_product_line": ("Product catalogue", "Lines of insurance with the risk-code pattern.", "15", "15"),
    "cat_product_version": ("Product catalogue", "Package versions: rate scheme, dates, status, origin, validation.",
                            "300", "450"),
    "cat_rate": ("Product catalogue", "Rate tables and statutory charges, effective-dated.", "800", "900"),
    "cat_rate_scheme_exception": ("Product catalogue", "Rate exceptions of new business with their decision.", "0",
                                  "300"),
    "cat_sales_officer": ("Product catalogue", "Account officers of each sales team.", "500", "560"),
    "cat_sales_unit": ("Product catalogue", "Sales units: regions, departments, teams, cost centres.", "120", "130"),
    "cat_short_period_rate": ("Product catalogue", "Short-period percentages by months covered.", "12", "12"),
    "cat_tsu_rule": ("Product catalogue", "TSU involvement rules by line or product.", "30", "40"),
    "cat_version_validation_check": ("Product catalogue", "Checklist results of each validation of a version.", "0",
                                     "1,500"),
    "pm_advisory": ("Package requests", "Package advisories: type, recipients, text, sending.", "0", "200"),
    "pm_client_response": ("Package requests", "Client responses to proposals.", "0", "500"),
    "pm_comparative_output": ("Package requests", "Comparative masters and client views with their files.", "0",
                              "600"),
    "pm_deactivation_request": ("Package requests", "Package deactivation requests and decisions.", "0", "50"),
    "pm_final_term": ("Package requests", "Final Terms for Proposal per field.", "0", "5,000"),
    "pm_final_term_history": ("Package requests", "History of the final terms.", "0", "8,000"),
    "pm_insurer_response": ("Package requests", "Insurer responses of a round.", "0", "1,500"),
    "pm_insurer_response_history": ("Package requests", "Earlier values of insurer responses.", "0", "3,000"),
    "pm_mancom_approval": ("Package requests", "ManCom approval tasks and decisions.", "0", "600"),
    "pm_marketing_approval": ("Package requests", "Marketing approvals by level.", "0", "400"),
    "pm_master_change": ("Package requests", "Product master changes and their transfer status.", "0", "300"),
    "pm_master_sim_inbox": ("Package requests", "Files received by the simulated receiving system (test only).", "0",
                            "0"),
    "pm_negotiation_round": ("Package requests", "Negotiation rounds and their quotation slips.", "0", "400"),
    "pm_placement_report": ("Package requests", "Generated Placement Update Reports.", "0", "60"),
    "pm_proposal_file": ("Package requests", "Proposal slips per insurer and version.", "0", "1,000"),
    "pm_request": ("Package requests", "Package requests: type, scope, terms, stage, milestones.", "0", "250"),
    "pm_round_insurer": ("Package requests", "Insurers approached in each round.", "0", "1,200"),
    "pm_signoff": ("Package requests", "ManCom sign-off records.", "0", "200"),
    "pm_terms_option": ("Package requests", "Options of an insurer in a comparative table.", "0", "2,000"),
    "pm_terms_setting": ("Package requests", "Fields and settings of a comparative table.", "0", "500"),
}
VOLUME_NOTE = "Every table of the module, with the estimated volume (tens of package requests a month)."
PARAMETER_FILTER = ("category = 'PRODUCT_MAINTENANCE' or param_key in ('RATE_EXCEPTION_VALIDITY_DAYS', "
                    "'QUOTATION_NUMBER_PREFIX')")
PARAMETER_LABELS: dict[str, str] = {}
SETTINGS_SCREEN_KEYS: list[str] = []
PARAMETER_MAKER_CHECKER = ("-", "Yes: System Parameters, approved by another user")
LOV_TYPES = ("CLAUSE_KIND", "COVERAGE_KIND", "INCENTIVE_EXCLUSION_RULE", "INCENTIVE_TYPE", "PKG_ADVISORY_GROUP",
             "PKG_BUSINESS_ORIGIN", "PKG_DEACTIVATION_REASON", "PKG_NOT_PROCEEDED_REASON", "PKG_POLICY_TYPE",
             "PKG_REQUEST_REASON", "PKG_REQUEST_SOURCE", "PKG_REQUEST_TYPE", "PKG_RESPONSE_OUTCOME")
ENUM_LISTS = [("Request stage", "productmaint/RequestStage"), ("Request type", "productmaint/RequestType"),
              ("Request scope", "productmaint/RequestScope"),
              ("Version status", "catalog/ProductVersionStatus"), ("Product lifecycle", "catalog/ProductLifecycle"),
              ("Deactivation status", "productmaint/DeactivationStatus"), ("Slip status", "productmaint/RoundStatus")]
ENUM_LABELS = {
    "DRAFT": "Draft", "FOR_MKT_APPROVAL": "For Marketing approval", "FOR_TSU_REVIEW": "For TSU review",
    "FOR_TSU_APPROVAL": "For TSU Head approval", "NEGOTIATION": "Insurer negotiation",
    "TERMS_REVIEW": "Negotiated terms review", "FOR_MKT_REVIEW": "Marketing review of terms",
    "REQUIREMENTS_PREP": "Requirements preparation", "FOR_MANCOM": "For ManCom sign-off",
    "MANCOM_APPROVED": "ManCom approved, deployment to request", "WITH_MBS": "With MBS for set-up",
    "FOR_VALIDATION": "For validation", "RELEASED": "Released", "RETIRED": "Retired",
    "NOT_PROCEEDED": "Not proceeded", "VOIDED": "Voided", "NEW": "New package", "AMEND": "Amendment",
    "UPDATE": "Update", "RENEW": "Renewal", "RETIRE": "Retirement", "REACTIVATE": "Reactivation",
    "GENERIC": "Generic programme", "CLIENT_SPECIFIC": "Client-specific package", "SUPERSEDED": "Superseded",
    "EXPIRED": "Expired", "ACTIVE": "Active", "PENDING": "Pending Approval", "APPROVED": "Approved",
    "REJECTED": "Rejected", "CANCELLED": "Cancelled", "PREPARATION": "Preparation", "FOR_APPROVAL": "For approval",
    "SENT": "Sent", "CLOSED": "Closed",
}
INACTIVE_CODES: set[str] = set()

# ================================================================================================ user stories
EPICS = [
    ("EP-PM-01", "Product Matrix and masters"), ("EP-PM-02", "Package requests and approvals"),
    ("EP-PM-03", "Negotiation and comparative tables"), ("EP-PM-04", "Requirements and ManCom sign-off"),
    ("EP-PM-05", "Set-up, validation and versions"), ("EP-PM-06", "Advisories, expiry, renewal and deactivation"),
    ("EP-PM-07", "Dashboard, reports, audit and transfer"), ("EP-PM-08", "Non-package proposal requests"),
]
STORIES = [
    ("US-PM-001", "EP-PM-01", "MBS", "to maintain the Product Matrix with the fields of each line and an approver",
     "products are consistent and approved before use",
     [("I enter a package without a cover type", "I save", "I see \"A package needs its cover type\""),
      ("I name myself as approver", "I send the record", "it is refused: another user approves")],
     "FRPM.003.01, FRPM.003.02, FRPM.003.03", "BR-PM-01", "SCR-PM-11", "High", 5),
    ("US-PM-002", "EP-PM-01", "MBS", "field rules per line or product", "accounts capture the right data",
     [("a Motor rule makes the plate number required", "a Motor account without plate number is submitted",
       "the submission is refused and the field named")],
     "FRPM.003.08", "BR-PM-01", "SCR-PM-15", "Medium", 3),
    ("US-PM-003", "EP-PM-01", "MBS", "one product hierarchy and a clause library under maker-checker",
     "insurer wordings are kept once",
     [("I add a clause and save it for authorisation", "the Business Administrator authorises it",
       "the clause is selectable in insurer terms"),
      ("I add a subtype under a subtype", "I save", "I see \"A subtype cannot have subtypes\"")],
     "FRPM.022.01, FRPM.022.02", "BR-PM-01, BR-PM-05", "SCR-PM-15", "High", 3),
    ("US-PM-004", "EP-PM-01", "MBS", "incentive criteria on the products of the matrix",
     "incentives apply only to eligible products",
     [("I choose an inactive product", "I save", "I see the product is not active"),
      ("a product of an active criterion expires", "the daily check runs", "TSU and MBS are told to review it")],
     "FRPM.018.01, FRPM.018.02", "BR-PM-13", "SCR-PM-16", "Medium", 3),
    ("US-PM-005", "EP-PM-02", "Marketing Account Officer", "to raise a package request with its terms and insurers",
     "TSU receives a complete request",
     [("I complete the request", "I submit", "it is numbered PKR-yyyy-n and For Marketing approval"),
      ("the name, line and reason are empty", "I submit", "each missing field is named"),
      ("I enter 15 estimated policies", "I save", "I see \"Enter at least 20 policies\"")],
     "FRPM.011.01, FRPM.011.02", "BR-PM-12", "SCR-PM-03", "High", 5),
    ("US-PM-006", "EP-PM-02", "Marketing Approver", "to approve or return requests of others",
     "only agreed requests reach TSU",
     [("a request of another user waits for me", "I approve it", "it is For TSU review"),
      ("I made the request", "I try to approve it", "it is refused: another user approves")],
     "FRPM.011.03", "BR-PM-01", "SCR-PM-04", "High", 2),
    ("US-PM-007", "EP-PM-02", "TSU Head", "the TSU Team Lead's recommendation before I approve for negotiation",
     "TSU work starts on agreed requests",
     [("the Team Lead recommended the request", "I approve it", "it is in Insurer negotiation")],
     "FRPM.011.03", "BR-PM-01", "SCR-PM-04", "High", 2),
    ("US-PM-008", "EP-PM-02", "TSU Team Lead", "to assign requests and let officers claim them",
     "work is shared in the team",
     [("I select requests of one stage", "I assign them to an officer", "the officer is assignee and is told")],
     "FRPM.017.01", "-", "SCR-PM-09", "Medium", 2),
    ("US-PM-009", "EP-PM-02", "TSU Head", "every action checked against the workflow and service levels",
     "no step is skipped and delays are seen",
     [("a request is in negotiation", "someone calls Approve", "it is refused: not allowed in this stage"),
      ("a request passed its stage service level", "I open the home page", "it counts as overdue")],
     "FRPM.028.01, FRPM.001.02", "BR-PM-01, BR-PM-10", "SCR-PM-01", "High", 3),
    ("US-PM-010", "EP-PM-03", "TSU Officer", "to send an approved quotation slip to the insurers from BIBS",
     "insurers receive protected slips and I keep the record",
     [("I submit the slip", "the Team Lead approves and sends", "each insurer gets a protected PDF and the password "
       "separately"),
      ("I prepared the slip", "I try to approve it", "I see \"The quotation slip is approved by a TL or a "
       "co-officer\"")],
     "FRPM.012.01, FRPM.027.01", "BR-PM-01, BR-PM-11", "SCR-PM-05", "High", 5),
    ("US-PM-011", "EP-PM-03", "TSU Officer", "to key in insurer responses and open further rounds",
     "negotiation is recorded completely",
     [("an insurer offers terms", "I key them in without a rate", "I see the rate is required"),
      ("terms need renegotiation", "I revise the slip", "round 2 opens without the insurers that declined")],
     "FRPM.012.02, FRPM.012.03", "-", "SCR-PM-05", "High", 5),
    ("US-PM-012", "EP-PM-03", "TSU Officer", "one current comparative master and client views",
     "the client sees the right subset and the master stays auditable",
     [("responses are in", "I compile the master", "the previous master is superseded and still downloadable")],
     "FRPM.012.04", "-", "SCR-PM-06", "High", 3),
    ("US-PM-013", "EP-PM-03", "TSU Officer", "to make the terms final with roles and shares",
     "the package has its insurers",
     [("shares total 90%", "I make the terms final", "I see the shares must total 100")],
     "FRPM.013.01", "BR-PM-04", "SCR-PM-05", "High", 2),
    ("US-PM-014", "EP-PM-03", "TSU Head", "to review the negotiated terms, with Marketing for a client-specific "
     "package", "the client sees only reviewed terms",
     [("the package is generic", "I review the terms", "it goes to the requirements"),
      ("the package is client-specific", "I release it to Marketing", "Marketing accepts or asks for changes")],
     "FRPM.013.02", "-", "SCR-PM-04", "Medium", 2),
    ("US-PM-015", "EP-PM-04", "TSU Officer", "to submit complete product requirements with the signed package slip",
     "ManCom signs off a full set",
     [("the signed package slip is missing", "I submit", "the missing item is named")],
     "FRPM.014.01", "-", "SCR-PM-07", "High", 3),
    ("US-PM-016", "EP-PM-04", "ManCom Member", "to approve the requirements in BIBS, the President last",
     "the sign-off is recorded",
     [("I am a selected approver", "I approve", "the President is asked once every other approver approved"),
      ("I return without remarks", "I confirm", "I see the remarks are required")],
     "FRPM.014.02", "BR-PM-01, BR-PM-07", "SCR-PM-29", "High", 3),
    ("US-PM-017", "EP-PM-04", "MBS", "the request to reach me only after the ManCom approval",
     "I set up only signed-off packages",
     [("the ManCom approval is complete", "the routing setting is AUTO", "the request is With MBS and I am told")],
     "FRPM.015.01, FRPM.016.01", "BR-PM-08", "SCR-PM-08", "High", 2),
    ("US-PM-018", "EP-PM-05", "MBS", "to set up a package version from the approved terms",
     "nothing is keyed twice",
     [("the request is With MBS", "I set up the version", "a draft version with the approved terms is created"),
      ("a version is already being set up", "I set up another", "it is refused")],
     "FRPM.016.02, FRPM.023.01", "BR-PM-12, BR-PM-04", "SCR-PM-13", "High", 5),
    ("US-PM-019", "EP-PM-05", "TSU Head", "to validate and release a version set up by someone else",
     "no wrong package is sold",
     [("I tick the checklist", "I validate and release", "the version is Released and Marketing is told"),
      ("I set up the version", "I try to validate it", "it is refused")],
     "FRPM.016.02, FRPM.003.03", "BR-PM-01, BR-PM-09", "SCR-PM-14", "High", 3),
    ("US-PM-020", "EP-PM-05", "Marketing Account Officer", "new business priced on the version in force",
     "outdated rates are never used",
     [("I quote a package", "I enter a lower item rate", "the submission needs an approved rate exception"),
      ("the exception is approved", "I submit", "the quotation is accepted with the version number")],
     "FRPM.024.01, FRPM.024.02", "BR-PM-02, BR-PM-03", "SCR-PM-22", "High", 5),
    ("US-PM-021", "EP-PM-05", "Marketing Account Officer", "a premium calculator on the package rate scheme",
     "I can quote the client at once",
     [("I choose the product, insurer and sum insured", "I calculate", "the premium breakdown is shown")],
     "FRPM.024.03", "-", "SCR-PM-21", "Low", 1),
    ("US-PM-022", "EP-PM-06", "TSU Officer", "to send the advisory with its supporting documents",
     "everyone is told of the package",
     [("the signed package slip is missing", "I send", "the missing documents are named"),
      ("the documents are attached", "I send", "groups are told and e-mail addresses get a protected PDF")],
     "FRPM.025.01", "BR-PM-11, BR-PM-14", "SCR-PM-08", "Medium", 3),
    ("US-PM-023", "EP-PM-06", "TSU Officer", "expiry and anniversary alerts and the expiry list",
     "packages are renewed on time",
     [("a package ends in 90 days", "the daily check runs", "I am told once for that day"),
      ("I open Package Expiry", "the page loads", "I see days left and any renewal request")],
     "FRPM.004.01, FRPM.004.02", "-", "SCR-PM-10", "High", 3),
    ("US-PM-024", "EP-PM-06", "TSU Officer", "to generate renewal requests from the expiry list",
     "renewals start from the current terms",
     [("I select expiring packages", "I generate", "one renewal request each, with the next term's dates"),
      ("a renewal request is open", "I generate again", "no second one is created")],
     "FRPM.026.01", "BR-PM-12", "SCR-PM-10", "Medium", 3),
    ("US-PM-025", "EP-PM-06", "MBS", "to request a package deactivation approved by someone else",
     "packages leave sale under control",
     [("I enter a past effective date", "I submit", "I see the date must be today or later"),
      ("the approver approves", "the effective date comes", "the package is retired and stays readable")],
     "FRPM.003.04, FRPM.003.06, FRPM.003.07", "BR-PM-01, BR-PM-05", "SCR-PM-24", "Medium", 3),
    ("US-PM-026", "EP-PM-06", "TSU Officer", "packages past their end date to expire automatically",
     "they are no longer sold",
     [("a package end date passed without a newer version", "the daily step runs", "the package is Expired and "
       "TSU and MBS are told")],
     "FRPM.003.05, FRPM.002.02", "BR-PM-05, BR-PM-06", "SCR-PM-01", "Medium", 2),
    ("US-PM-027", "EP-PM-07", "TSU Head", "a dashboard with KPIs, drill-down and export",
     "I follow the work of the unit",
     [("I filter by period and officer", "I open a KPI", "I see its requests and can export them")],
     "FRPM.001.01, FRPM.001.03, FRPM.002.01", "-", "SCR-PM-23", "High", 3),
    ("US-PM-028", "EP-PM-07", "TSU Officer", "the Placement Update Report weekly and on request",
     "placement status is shared",
     [("it is Thursday 08:00", "the job runs", "the report is stored and users are told")],
     "FRPM.007.01, FRPM.007.02", "-", "SCR-PM-26", "Medium", 3),
    ("US-PM-029", "EP-PM-07", "Auditor", "the package reports and a reports builder",
     "I can evidence every package and version",
     None, "FRPM.019.01, FRPM.019.02, FRPM.019.03, FRPM.020.01, FRPM.030.01", "-", "SCR-PM-23", "Medium", 3),
    ("US-PM-030", "EP-PM-07", "Auditor", "an audit log of every Product Maintenance action",
     "I can trace who did what and when",
     [("a version is released", "I open Audit Logs", "I see each step with user, time, old and new values")],
     "FRPM.021.01, FRPM.021.02", "BR-PM-05", "SCR-PM-27", "High", 2),
    ("US-PM-034", "EP-PM-07", "Auditor", "the field-level change history of a master record",
     "I can see what changed, who made it and who authorized it",
     [("an insurer's credit days changed from 30 to 45 and were authorized", "I open its Change History",
       "I see Credit days, 30, 45, the maker, the checker and both times")],
     "FRPM.021.03", "BR-PM-01, BR-PM-05", "SCR-PM-18", "High", 3),
    ("US-PM-035", "EP-PM-01", "Business Administrator", "the full insurer page: contacts, payment terms and "
     "remittance, bank accounts, products", "the insurer's data is kept in BIBS under maker-checker",
     [("I add a bank account", "I save it", "it is pending until another user authorizes it"),
      ("the account number is already recorded for the insurer", "I save", "it is refused")],
     "FRPM.023.02", "BR-PM-01", "SCR-PM-18", "Medium", 5),
    ("US-PM-036", "EP-PM-01", "MBS", "the ledger accounts of an insurer on its page",
     "I see what is posted and outstanding with the insurer without leaving the insurer record",
     [("premiums payable to the insurer are posted and partly remitted", "I open its Ledger Accounts tab",
       "I see the accounts with debit, credit and balance and the open items with the outstanding amount")],
     "FRPM.023.02", "BR-PM-01", "SCR-PM-18", "Medium", 3),
    ("US-PM-038", "EP-PM-01", "Business Administrator", "the default co-insurance participation of an insurer",
     "an invoice booked without a co-insurance split takes the insurer's arrangement",
     [("I add two participations of 60% (lead) and 40%", "another user authorizes them",
       "the tab shows a complete arrangement of 100%"),
      ("the shares of the period would exceed 100%", "I save", "it is refused"),
      ("the arrangement is complete on the booking date", "an invoice is booked without shares",
       "it is split 60% and 40% with the lead first")],
     "FRPM.023.03", "BR-PM-01", "SCR-PM-18", "Medium", 5),
    ("US-PM-037", "EP-PM-07", "Auditor", "one log of the changes of every master record",
     "I can review the master data changes of a period without opening each record",
     [("insurers and products were changed in October", "I filter the Master Data Change Log on October and a "
       "maker", "I see each field change with its maker and checker and export the rows to Excel"),
      ("a record belongs to a company outside my data access", "I filter the log", "its changes are not listed")],
     "FRPM.021.03", "BR-PM-05", "SCR-PM-27", "Medium", 3),
    ("US-PM-031", "EP-PM-07", "MBS", "product master changes sent to the other BDOI systems",
     "other systems know which packages are on sale",
     [("a version is released", "the transfer runs", "the change is in the file and marked sent"),
      ("a transfer failed", "I process the change again", "it is sent in the next file")],
     "FRPM.029.01, FRPM.029.02", "-", "SCR-PM-25", "Medium", 3),
    ("US-PM-032", "EP-PM-08", "TSU Officer", "non-package quotation requests, slips and comparative tables",
     "non-package risks follow the same control",
     [("a request lacks its mandatory documents", "it is submitted", "it is refused"),
      ("insurer terms are keyed in", "I open the comparative table", "each insurer is a column with its options")],
     "FRPM.005.01, FRPM.005.02, FRPM.006.01, FRPM.006.02", "BR-PM-01", "SCR-PM-28", "Medium", 5),
    ("US-PM-033", "EP-PM-08", "Marketing Account Officer", "to select insurers, receive proposal slips and record "
     "the client's response", "the client decision is recorded",
     [("final terms are set", "proposal slips are approved", "I receive them"),
      ("the client accepts", "I record it without the acceptance e-mail", "it is refused")],
     "FRPM.008.01, FRPM.009.01, FRPM.009.02, FRPM.010.01", "-", "SCR-PM-28", "Medium", 5),
]
REPORT_AC = [("I open the report runner", f"I run the {r}", "the rows match the parameters and export to PDF, Excel "
              "and CSV") for r in ("Package Expiry Report", "Package Version History", "Renewal Request Report")]

README = {
    "intro": ("This pack holds every document of Product Maintenance (BRD-03). Each document has one job. A "
              "requirement or rule is stated once, in the FRS, with its ID; the other documents refer to that ID."),
    "order": ["FRS", "BPS", "UXD", "RNO", "ICD", "DSP", "DDI", "USR", "RTM"],
    "programme": [
        ("Non-Functional and Security Requirements", "Performance, availability, security, audit, retention, backup, "
         "accessibility and browsers for every BRD."),
        ("Glossary", "Terms and abbreviations used in every pack."),
        ("Decision and Observation Log", "Decisions taken and observations raised across the BRDs."),
        ("Document Register", "Every current document of BRD-11, BRD-03, BRD-06 and BRD-02 with version, date, "
         "owner and status."),
    ],
    "ids": [("FRPM.nnn.nn", "Functional requirement (FRS)"), ("BR-PM-nn", "Business rule (FRS)"),
            ("SCR-PM-nn", "Screen (UX Design)"), ("SB-nn, P-PM-nn", "Storyboard, process (Process and Storyboards)"),
            ("RPT-PM-nn, DO-nn, NT-nn", "Report, document, notice (Reports, Notifications and Outputs)"),
            ("IF-PM-nn", "Interface (Interface Specification)"), ("US-PM-nnn", "User story (User Stories)"),
            ("TC-PM-…", "Test case (Test Cases and Traceability)"), ("CR-PM-nnn", "Change request (CR Register)"),
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
                problems.append(f"{s[0]}: unknown FRPM {ref}")
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
        problems.append(f"FRPM items without a user story: {sorted(frum - covered)}")
    return problems
