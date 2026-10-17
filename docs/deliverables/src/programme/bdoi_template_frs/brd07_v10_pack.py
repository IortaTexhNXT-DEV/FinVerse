"""BRD-07 Claims v1.0: screens of the UX Design, user stories, the facts of the developer specification and data
dictionary, and the "How to read this pack" note. Read by build_bdoi_v10_packs.py (FRS, BPS, UXD, RNO, ICD,
workbooks) and by the shared writers pack_dsp, pack_ddi, pack_usr and pack_readme.

The services, screens and tables are read from the sources by tools/deliverables/api_facts.py and schema_facts.py;
this module says which belong to Claims Handling and gives them their business wording.
"""

from __future__ import annotations

import api_facts as af
import bdoi_v10_common as common
import brd07_v10_content as C
from pack_common import Column

BRD = C.BRD
NAME = C.NAME
SHORT = C.SHORT
FOLDER = "BRD-07_Claims"
FIX_VERSION = "Drop 2"
DATE = C.DATE
DATE_US = C.DATE_US
PREPARED = "iorta TechNXT Project Team"
HEADER_RIGHT = C.HEADER_RIGHT
DOC_NO = C.DOC_NO
CLASS_FIRST_CONSTANTS = True

SET = common.make_set(C, "Claims", {
    "FRS": "BDOI Business Unit owners (Claims, Marketing, Risk)",
    "ICD": "Formal loss advice to the insurer, insurer updates by upload, documents, module hand-offs, legacy claims.",
    "DDI": ", and the lists of BDOI's annexes."})


def file_name(key: str) -> str:
    return common.file_name(SET, BRD, key)


def version(key: str) -> str:
    return next(v for k, _, _, v, *_ in SET if k == key)


for _k in C.DOCS:
    assert C.DOCS[_k]["file"] == file_name(_k), (C.DOCS[_k]["file"], file_name(_k))

DOCS = {
    "DSP": {"cover": "Claims: Developer Specification", "header_left": "Developer Specification",
            "doc_no": f"{DOC_NO}-DSP"},
    "README": {"cover": "Claims: How to Read this Pack", "header_left": "How to Read this Pack",
               "doc_no": f"{DOC_NO}-README"},
}

# ================================================================================================ UX screens
AV = "Available"
CH = "Available; changes of the FRS v1.0 to deliver ({})"
NEW = "Proposed screen ({})"
UX_SCREENS = [
    {"id": "SCR-CLM-01", "name": "Claims Home", "route": "claims-handling", "page": "home/ClaimsHomePage.tsx",
     "purpose": "Dashboard of the user's role: my open claims, follow-ups due today and overdue, diary due, "
                "temporarily closed, unpaid premium, awaiting premium remittance, claims by status and ageing "
                "brackets.", "access": "Claims personas", "frrn": "FRCLM.001", "outcome": "The user opens a list.",
     "state": CH.format("FRCLM.001.01 trend; FRCLM.001.02 to .04 with CR-CLM-103"),
     "rules": ["Figures follow the user's claims (handler), team (Team Leader) or all (Team Head) with CR-CLM-104.",
               "Each tile opens the worklist filtered on it.",
               "Pending approvals show for the Team Leader and Team Head with CR-CLM-102."],
     "fields": [], "actions": [["Open a tile", "All", "Always", "Opens the filtered worklist", "Worklist"]]},
    {"id": "SCR-CLM-02", "name": "Claims Worklist", "route": "claims-handling/worklist",
     "page": "worklist/WorklistPage.tsx", "purpose": "Follow-up list with tabs My Claims, Open, Temporarily Closed, "
     "Closed, Follow-ups Due, All; search; quick view.", "access": "Claims personas", "frrn": "FRCLM.009",
     "outcome": "A claim opened or reassigned.", "state": CH.format("FRCLM.009.01 columns, 009.02, 009.08"),
     "rules": ["Search by claim number, insurer claim number, ARN, policy number or assured, at least 3 characters.",
               "Columns: claim, claimant, follow-up count, last follow-up, aging since last follow-up, in status and "
               "overall, plate number, client, handler."],
     "fields": [["Search", "Text", "N", "-", "At least 3 characters", "Enter at least 3 characters"],
                ["Tab", "Choice", "Y", "My Claims", "-", "-"]],
     "actions": [["Reassign", "Team Leader", "Claims selected", "Gives the claims to another handler", "Reassigned"],
                 ["Export", "Claims personas", "List shown", "Excel or PDF of the list (G-CLM-04)", "File"]]},
    {"id": "SCR-CLM-03", "name": "Record Claim", "route": "claims-handling/new", "page": "record/RecordClaimPage.tsx",
     "purpose": "Cover search, cover card with the premium check, locations, loss, insurers, confirmation.",
     "access": "Claims Handler, Team Leader, Team Head", "frrn": "FRCLM.008, 021",
     "outcome": "Claim recorded with its number.", "state": CH.format("FRCLM.008.02 fields, 008.10 with CR-CLM-108"),
     "rules": ["Only the locations of the cover can be linked.", "A loss date outside the policy year needs a "
               "confirmation.", "The claim of an unpaid cover is saved, flagged and alerted."],
     "fields": [["Cover search", "Text", "Y", "-", "ARN, policy number or assured, at least 3 characters",
                 "Enter at least 3 characters"],
                ["Date of loss", "Date", "Y", "-", "Not in the future", "The date of loss cannot be in the future"],
                ["Reported date", "Date", "Y", "Today", "Not before the date of loss", "The reported date cannot be "
                 "before the date of loss"],
                ["Claimant", "Text", "Y", "Assured", "Override with the permission", "-"],
                ["Nature of claim", "List", "Y", "-", "Annex O", "-"], ["Claim type", "List", "Y", "-", "Annex M", "-"],
                ["Catastrophe", "List", "N", "-", "BRCLM.036", "-"],
                ["Locations", "Multiple", "Y", "Policy locations", "Cover locations only", "Location <item> is not on "
                 "the cover"],
                ["Insurers", "Lines", "Y", "From the invoice shares", "Shares total 100%", "-"]],
     "actions": [["Record", "Handler", "Fields valid", "Creates the claim", "Claim"]]},
    {"id": "SCR-CLM-04", "name": "Claim", "route": "claims-handling/:id", "page": "record/ClaimPage.tsx",
     "purpose": "One claim: summary card and flags, status panel, tabs Details, Locations, Insurers and Updates, "
                "Reserve and Settlement, Documents, Diary, History.", "access": "Claims personas; Marketing read "
     "(FRCLM.002.02)", "frrn": "FRCLM.003, 006, 008, 009, 021 to 024",
     "outcome": "Claim followed up, settled, closed or reopened.",
     "state": CH.format("FRCLM.006 checklist, 008.08 preview, 009.04 fields, 010 approval"),
     "rules": ["Change Status lists only the statuses of the user's role and unit.",
               "A settlement type that closes needs the closing permission.",
               "Flags: Unpaid premium, Awaiting premium remittance, Newer cover version, Multi-location, "
               "Multi-insurer, CAT."],
     "fields": [["Status", "List", "Y", "-", "Status access matrix", "Your role and unit may not set the status "
                 "<status>"],
                ["Next follow-up date", "Date", "Y", "From the status", "Today or later; override with reason",
                 "The next follow-up date must be today or later"],
                ["Next action plan", "Text 2,000", "N", "-", "Versions kept", "The action plan can have at most 2,000 "
                 "characters"],
                ["Insurer claim number", "Text", "Cond.", "-", "Unique per insurer", "Claim number <number> is "
                 "already recorded for <insurer>"],
                ["Reserve", "Amount", "N", "-", "Not negative; permission", "The reserve cannot be negative"],
                ["Settlement type", "List", "Cond.", "-", "Amount and date for a settled type", "Enter the settlement "
                 "amount and date"]],
     "actions": [["Generate Authorization Code", "Handler", "Premium paid", "Issues the code", "Code"],
                 ["Send Loss Advice", "Handler", "Insurer line", "E-mails the letter", "Sent"],
                 ["Change Status", "Handler, Team Leader, Team Head", "Open claim", "Sets the status", "Status"],
                 ["Set Settlement", "Team Leader, Team Head", "Open claim", "Records the outcome", "Closed or open"],
                 ["Assign Adjuster", "Team Leader, Team Head", "Insurer line", "Sets the adjuster", "Adjuster"],
                 ["Override Follow-up Date", "Team Leader, Team Head", "Open claim", "With reason", "Date"],
                 ["Record Insurer Update", "Handler", "Always", "Date, source, remarks", "History line"],
                 ["Reopen", "Team Head, Unit Head", "Closed", "With reason", "In Progress"],
                 ["Request Special Remittance", "Handler", "Awaiting premium remittance", "Opens the request",
                  "Request"]]},
    {"id": "SCR-CLM-05", "name": "Cover Lookup", "route": "claims-handling/covers", "page": "cover/CoverLookupPage.tsx",
     "purpose": "Read-only cover: account, policy years, items and locations, endorsements, invoices with payment and "
                "remittance status, claims of the cover, insurer location references.", "access": "Claims "
     "personas", "frrn": "FRCLM.002, 008.03", "outcome": "Cover read.", "state": AV,
     "rules": ["Read-only; no account maintenance from Claims."], "fields": [],
     "actions": [["Record claim", "Handler", "Cover chosen", "Opens Record Claim on the cover", "Record Claim"]]},
    {"id": "SCR-CLM-06", "name": "My Diary", "route": "claims-handling/diary", "page": "diary/DiaryPage.tsx",
     "purpose": "The user's diary entries across claims, due today and overdue.", "access": "Claims personas",
     "frrn": "FRCLM.009.06", "outcome": "Entry done.", "state": AV, "rules": ["The assignee or the author marks an "
                                                                            "entry done."], "fields": [],
     "actions": [["Done", "Assignee, author", "Open entry", "Marks it done", "Done"]]},
    {"id": "SCR-CLM-07", "name": "Insurer Location References", "route": "claims-handling/location-refs",
     "page": "location/LocationRefsPage.tsx", "purpose": "References of the insured locations per insurer.",
     "access": "Claims Handler, Team Leader, Team Head", "frrn": "FRCLM.022.02", "outcome": "Reference saved.",
     "state": AV, "rules": ["A new reference end-dates the open one."],
     "fields": [["ARN", "Text", "Y", "-", "Cover", "-"], ["Item No.", "Number", "Y", "-", "Location of the cover", "-"],
                ["Insurer", "List", "Y", "-", "Insurer of the cover", "-"],
                ["Insurer Location Ref", "Text", "Y", "-", "-", "-"], ["Effective From", "Date", "Y", "Today", "-",
                                                                      "-"]],
     "actions": [["Add", "Handler", "Always", "Saves the reference", "Reference"],
                 ["Upload", "Handler", "Always", "Loads references (IF-CLM-02)", "Run"]]},
    {"id": "SCR-CLM-08", "name": "Claims Reports", "route": "claims-handling/reports",
     "page": "reports/ClaimsReportsPage.tsx", "purpose": "The claims reports.", "access": "Report permission; "
     "Marketing on screen, Marketing Team Leader export", "frrn": "FRCLM.011 to 019", "outcome": "Report run.",
     "state": CH.format("BDOI heading, filters and grouping; RPT-CLM-08 to 10 with their change requests"),
     "rules": ["Heading of BR-CLM-10; each run archived."],
     "fields": [["Report", "List", "Y", "-", "Claims category", "-"],
                ["As of / period", "Date", "Y", "Today", "-", "-"],
                ["Presentation", "Choice", "Y", "Detailed", "Detailed or Summary", "-"],
                ["Grouping", "Choice", "Y", "Insurer", "Insurer, claim type or risk code", "-"]],
     "actions": [["Run", "Report users", "Parameters given", "Runs on screen", "Report"],
                 ["Export", "Report export permission", "Report run", "Excel or PDF", "File"]]},
    {"id": "SCR-CLM-09", "name": "Claims Setup", "route": "claims-handling/setup", "page": "setup/ClaimsSetupPage.tsx",
     "purpose": "Status attributes, settlement types, status access matrix, claims handler register, claims lists.",
     "access": "Claims Unit Head", "frrn": "FRCLM.002.06, 003, 004, 007", "outcome": "Setup changed under "
     "maker-checker.", "state": CH.format("FRCLM.002.06 reporting lines; FRCLM.004.02 with CR-CLM-106"),
     "rules": ["Every change applies after another user approves it."],
     "fields": [["Role", "List", "Y", "-", "Claims roles", "-"], ["Unit", "List", "N", "Any", "Claims units", "-"],
                ["Status", "List", "Y", "-", "-", "The role and unit already have this status"]],
     "actions": [["Save", "Unit Head", "Always", "Saves for approval", "For approval"],
                 ["Authorise", "Another administrator", "For approval", "Applies the change", "Applied"]]},
    {"id": "SCR-CLM-10", "name": "Claims Reporting Forms", "route": None, "page": None,
     "purpose": "Claims reporting forms of Marketing: draft, submit, accept or return.", "access": "Marketing Account "
     "Officer (own forms); Claims Handler, Team Leader", "frrn": "FRCLM.008.01, 002.03",
     "outcome": "Form accepted into a claim.", "state": NEW.format("CR-CLM-101"),
     "rules": ["The CRF reference differs from the claim number.", "A returned form goes back to the Account Officer "
                                                                   "with the reason."],
     "fields": [["Policy", "Search", "Y", "-", "Policy of the Account Officer's client", "-"],
                ["Fields of claim logging", "Form", "Cond.", "-", "As Record Claim", "<Field> is required"]],
     "actions": [["Save draft", "Account Officer", "Always", "Saves", "Draft"],
                 ["Submit", "Account Officer", "Draft", "Sends to the handlers", "Submitted"],
                 ["Accept", "Handler", "Submitted", "Creates the claim", "Accepted"],
                 ["Return", "Handler", "Submitted", "With reason", "Returned"]]},
    {"id": "SCR-CLM-11", "name": "Approvals", "route": None, "page": None,
     "purpose": "Follow-ups and record changes waiting for the Team Leader or the Team Head.",
     "access": "Claims Team Leader, Team Head", "frrn": "FRCLM.010, 003.04", "outcome": "Approved or returned.",
     "state": NEW.format("CR-CLM-102"), "rules": ["The approver is not the submitter.", "A denial needs a comment."],
     "fields": [["Comment", "Text 1,000", "Cond.", "-", "Required to deny", "Enter the comment for the handler"]],
     "actions": [["Approve", "Team Leader, Team Head", "Pending", "Approves or forwards", "Approved or pending Team "
                  "Head"], ["Deny", "Team Leader, Team Head", "Pending", "Returns with comment", "Returned"]]},
    {"id": "SCR-CLM-12", "name": "Claims Masters", "route": None, "page": None,
     "purpose": "Claim types by risk code, claim categories, severity, adjuster contacts, repair shops, document "
                "checklist per risk code category, loss advice templates with versions.",
     "access": "Claims Unit Head", "frrn": "FRCLM.004.02, 005, 006, 007", "outcome": "Masters maintained under "
     "maker-checker.", "state": NEW.format("CR-CLM-105, 106, 107; G-CLM-01 to 03"),
     "rules": ["No duplicate codes; inactive values not offered on new claims; alphabetical order."],
     "fields": [["Code", "Text", "Y", "-", "Unique", "-"], ["Name", "Text", "Y", "-", "-", "-"],
                ["Risk code category", "List", "Cond.", "-", "Categories and documents", "-"],
                ["Effective from / to", "Date", "Cond.", "-", "Templates", "-"]],
     "actions": [["Save", "Unit Head", "Always", "Saves for approval", "For approval"]]},
    {"id": "SCR-CLM-13", "name": "Insurers' Scorecard", "route": None, "page": None,
     "purpose": "Ratings A to F per insurer on the evaluation and check issuance TAT, acceptance rate and average "
                "TAT.", "access": "Claims Team Head, Unit Head", "frrn": "FRCLM.020", "outcome": "Scorecard read.",
     "state": NEW.format("CR-CLM-109"), "rules": ["Working days on the holiday calendar; targets of Annex AB."],
     "fields": [["Period", "Dates", "Y", "Last quarter", "-", "-"], ["Insurer", "List", "N", "All", "-", "-"]],
     "actions": [["Run", "Team Head, Unit Head", "Period given", "Computes the ratings", "Scorecard"]]},
]
SCREENS_BY_ID = {s["id"]: s for s in UX_SCREENS}


def screens_by_id() -> dict[str, dict]:
    return SCREENS_BY_ID


# ================================================================================================ developer spec
PACKAGES = ["brokerclaims"]
MESSAGE_PACKAGES = ["brokerclaims"]
SETTING_CLASSES = ("SystemParameter",)
CONTROLLERS = ["brokerclaims/" + c for c in (
    "claim/api/BrokerClaimController.java", "cover/api/CoverController.java", "diary/api/DiaryController.java",
    "home/api/ClaimsHomeController.java", "insurer/api/InsurerClaimController.java",
    "insurer/api/InsurerUpdateController.java", "location/api/ClaimLocationController.java",
    "location/api/LocationRefController.java", "setup/api/ClaimsSetupController.java",
    "status/api/ClaimStatusController.java")]
SHARED_CONTROLLERS = ["report/api/ReportController.java"]  # read only when a screen calls it
OMIT_OPS: set[str] = set()
DSP_TEXT = {
    "requirement_ids": "FRCLM, BR-CLM",
    "screen_ids": "SCR-CLM",
    "interfaces": "formal loss advice, insurer updates by upload, documents, module hand-offs, legacy claims",
    "integration_prefix": "",
    "service_ops": "The upload layouts are detailed in the Interface Specification.",
    "status_note": "Claim phases follow the status model of the data dictionary (sheet Status models); statuses and "
                   "settlement types are lists with their attributes.",
    "jobs_note": "The jobs can also be started from the Job Monitor by the System Administrator.",
    "audit_note": "Each claim also keeps its own timeline (status, fields, diary, insurer updates, reserve).",
    "security_note": "Every claim call names the company; a claims user acts only within it.",
    "omit_classes": ("Seed",),
    "replace": {},
}
AREAS = [
    ("Claims and covers", ("broker-claims/covers", "broker-claims/search", "broker-claims/{"),
     "Cover lookup, claim record, claimant, premium check, authorization code."),
    ("Status, follow-up and settlement", ("broker-claims/{broker-claim-id}/status", "broker-claims/{broker-claim-id}/"
                                          "settlement", "broker-claims/{broker-claim-id}/reopen",
                                          "broker-claims/{broker-claim-id}/follow-up"),
     "Status, settlement, reopen, follow-up date, action plan, adjuster."),
    ("Insurers and locations", ("broker-claims/{claim-id}", "broker-claims/location-refs"),
     "Insurer lines, claim numbers, reserve, updates, loss advice, locations, references."),
    ("Diary, home and worklist", ("broker-claims/diary", "broker-claims/home", "broker-claims/worklist",
                                  "broker-claims/assignees", "broker-claims/reassign", "broker-claims/experience"),
     "Diary, Claims Home, worklist, reassignment, claim experience."),
    ("Setup", ("broker-claims/setup",), "Attributes, matrix, handlers, lists."),
]
COMPONENTS = [
    ("Claims screens", f"{sum(1 for s in UX_SCREENS if s['page'])} screens under Claims & Insurance › Claims "
     "Handling.", "Section 3"),
    ("Claims service", "Covers, claims, premium check, status, settlement, insurers, locations, diary, setup.",
     "Section 4"),
    ("Insurer exchanges", "Loss advice e-mail, updates by upload.", "Interface Specification v1.0"),
    ("Audit trail", "Insert-only record of every action.", "Section 8"),
    ("Scheduled jobs", "Premium re-check, follow-up due, ageing alerts.", "Section 7"),
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
                    "page": ("brokerclaims", s["page"]), "loads": s["purpose"], "states": s["rules"][:4] or ["-"],
                    "fields": [], "codes": [], "calls": common.screen_calls("brokerclaims", s["page"], _OPS)})
        if s["route"] not in menu:
            out[-1]["permission"] = "BCL_VIEW"
    return out


SCREENS = _screens()
UX_INPUTS: dict[str, str] = {}
ACTION_LABELS = {"record": "Record", "authorize": "Generate Authorization Code", "changeStatus": "Change Status",
                 "setSettlement": "Set Settlement", "reopen": "Reopen", "overrideFollowUp": "Override Follow-up Date",
                 "reassign": "Reassign", "sendLossAdvice": "Send Loss Advice", "addUpdate": "Record Insurer Update",
                 "done": "Done"}
DATA_LABELS = {"home": "Dashboard figures", "worklist": "The worklist", "claim": "The claim", "covers": "Covers",
               "cover": "The cover"}
PERMISSION_PREFIXES = ("BCL_",)
PERSONA_ROLES = [("Claims Handler", "CLM_OFFICER"), ("Claims Team Leader", "CLM_TL"), ("Claims Team Head", "CLM_TH"),
                 ("Claims Unit Head", "CLM_UH"), ("Claims / Risk user", "CLM_RISK"),
                 ("Marketing Account Officer", "MKT_AO"), ("Marketing Team Leader", "MKT_TL"), ("Auditor", "AUDITOR")]
RULE_ENFORCEMENT = [
    ("BR-CLM-01", "Menu and actions by permission.", "-", "Claims permissions of the group profiles."),
    ("BR-CLM-02", "Field messages on Record Claim.", "BCL_LOSS_DATE_FUTURE; BCL_LOSS_OUTSIDE_COVER; "
     "BCL_REPORTED_DATE_RANGE; BCL_LOCATION_NOT_ON_COVER.", "Cover snapshot on the claim."),
    ("BR-CLM-03", "Unpaid premium flag.", "BCL_PREMIUM_UNPAID; BCL_DP_EVIDENCE_REQUIRED.", "Job BCL_PREMIUM_RECHECK; "
     "setting BCL_AUTH_DP_POLICY."),
    ("BR-CLM-04", "Unit shown on the claim.", "BCL_UNIT_NOT_SET.", "Claims handler register."),
    ("BR-CLM-05", "Statuses of the user's role and unit.", "BCL_STATUS_NOT_ALLOWED; BCL_MATRIX_ROW_EXISTS.",
     "Status access matrix; list attributes under maker-checker."),
    ("BR-CLM-06", "History tab.", "BCL_INSURER_CLAIM_NO_DUPLICATE.", "Claim events insert-only."),
    ("BR-CLM-07", "Next follow-up date on the status panel.", "BCL_FOLLOW_UP_PAST; BCL_REASON_REQUIRED.",
     "Setting BCL_FOLLOW_UP_DAYS; status attribute."),
    ("BR-CLM-08", "Ages on the worklist.", "-", "Setting BCL_AGEING_BUCKETS."),
    ("BR-CLM-09", "Documents tab.", "-", "Document checklist (to enable, G-CLM-01)."),
    ("BR-CLM-10", "Report Centre.", "-", "Report heading (to deliver)."),
    ("BR-CLM-11", "-", "-", "No accounting event in Claims."),
    ("BR-CLM-12", "-", "-", "Approval workflow (CR-CLM-102)."),
]
WORKFLOWS = {
    "Claim phase": ("brokerclaims/domain/ClaimPhase", [
        ("NEW", "New", "Recording", "IN_PROGRESS"),
        ("IN_PROGRESS", "In Progress", "Status of an in-progress phase", "TEMP_CLOSED, CLOSED"),
        ("TEMP_CLOSED", "Temporarily Closed", "Temporary closure status", "IN_PROGRESS, CLOSED"),
        ("CLOSED", "Closed", "Settlement type that closes", "IN_PROGRESS (reopen)")]),
}
STATUSES = C.STATUSES
AUDIT_LABELS = {"BrokerClaimAttribute": "Status or settlement type attribute", "BrokerClaimDiary": "Diary entry",
                "BrokerClaimHandler": "Claims handler register", "BrokerClaimLocationRef": "Insurer location reference",
                "BrokerClaimStatusAccess": "Status access matrix row"}
AUDIT_OMIT = {"Entity Type", "currentUser.username()"}
ACTION_WORDS = {"CREATE": "Created", "UPDATE": "Changed", "DEACTIVATE": "Deactivated", "SUBMIT": "Submitted",
                "AUTHORIZE": "Approved", "REJECT": "Rejected", "OPEN": "Opened"}
JOBS = {j[0]: j[2] for j in C.JOBS}
JOB_OMIT: set[str] = set()

# ================================================================================================ data dictionary
TABLE_PATTERN = r"^bcl_"
TABLES = {t: ("Claims", p, "-", "-") for t, p in {
    "bcl_attribute_change": "Changes of the status and settlement type attributes, under maker-checker.",
    "bcl_claim": "Claims: cover, loss, claimant, status, phase, settlement, follow-up, ages, flags.",
    "bcl_claim_event": "Timeline of each claim: status, field changes, notes.",
    "bcl_claim_location": "Insured locations linked to a claim.",
    "bcl_diary_entry": "Diary entries of the claims handlers.",
    "bcl_handler": "Claims handler register: user, unit, team.",
    "bcl_insurer_claim": "Insurer lines of a claim: share, insurer claim number, reserve, settlement, adjuster.",
    "bcl_insurer_update": "Updates received from the insurers.",
    "bcl_location_ref": "Insurer references of the insured locations with effective dates.",
    "bcl_lov_attribute": "Attributes of the claim statuses and settlement types.",
    "bcl_reserve_change": "History of the insurer reserve.",
    "bcl_status_access": "Status access matrix: status, role, unit.",
    "bcl_status_history": "Status changes with the days spent in the previous status.",
}.items()}
VOLUME_NOTE = "Every table of the module; volumes follow the claims (about 1,200 activity entries a day)."
PARAMETER_FILTER = "param_key like 'BCL%'"
PARAMETER_LABELS: dict[str, str] = {}
SETTINGS_SCREEN_KEYS: list[str] = []
PARAMETER_MAKER_CHECKER = ("-", "Yes: System Parameters, approved by another user")
LOV_TYPES = ("BCL_CLAIM_STATUS", "BCL_SETTLEMENT_TYPE", "BCL_ADJUSTER", "BCL_CATASTROPHE", "BCL_CLAIM_TYPE",
             "BCL_LOSS_NATURE", "BCL_UNIT", "BCL_UPDATE_SOURCE", "BCL_DIARY_TYPE", "BCL_DOCUMENT_TYPE",
             "BCL_REOPEN_REASON", "BCL_OVERRIDE_REASON")
ENUM_LISTS = [("Claim phase", "brokerclaims/domain/ClaimPhase"), ("Claim source", "brokerclaims/domain/ClaimSource"),
              ("Closure kind", "brokerclaims/domain/ClaimClosureKind"),
              ("Premium status", "brokerclaims/domain/ClaimPremiumStatus")]
ENUM_LABELS = {"TEMP_CLOSED": "Temporarily Closed", "IN_PROGRESS": "In Progress", "BROKER_NOTICE": "Notice to BDOI",
               "INSURER_REPORTED": "Reported by the insurer", "NO_INVOICE": "No invoice"}
INACTIVE_CODES: set[str] = set()
DDI_TEXT = {"follow_up_days": "FOLLOW_UP_DAYS"}


def extra_ddi_sheets(wb) -> None:
    """The lists of BDOI's annexes and the upload layouts of the Interface Specification."""
    common.layout_sheet(wb, Column, "BDOI Lists", "BDOI lists", C.ANNEX_LISTS, ("Annex", "List", "Values"),
                        "Lists of BDOI's FRS annexes to load in the claims masters.")
    common.layout_sheet(wb, Column, "Upload Layouts", "Uploads", [
        ("IF-CLM-02", "Insurer claim numbers", "Claim No.*, Insurer*, Insurer Claim No.*, Reported To Insurer On"),
        ("IF-CLM-02", "Insurer updates", "Claim No. or Insurer + Insurer Claim No., Update Date*, Source*, Reference, "
         "Remarks*"),
        ("IF-CLM-02", "Insurer location references", "ARN*, Item No.*, Insurer*, Insurer Location Ref*, Effective "
         "From*")], ("Interface", "File", "Columns (* required)"),
        "Columns of the upload files of the Interface Specification.")


# ================================================================================================ user stories
EPICS = [("EP-CLM-01", "Dashboards and access"), ("EP-CLM-02", "Claims masters"),
         ("EP-CLM-03", "Claim record and insurers"), ("EP-CLM-04", "Follow-up, approval and settlement"),
         ("EP-CLM-05", "Claims reports and scorecard")]
_EPIC_OF = {"FRCLM.001": 1, "FRCLM.002": 1, "FRCLM.003": 2, "FRCLM.004": 2, "FRCLM.005": 2, "FRCLM.006": 2,
            "FRCLM.007": 2, "FRCLM.008": 3, "FRCLM.009": 4, "FRCLM.010": 4, "FRCLM.011": 5, "FRCLM.012": 5,
            "FRCLM.013": 5, "FRCLM.014": 5, "FRCLM.015": 5, "FRCLM.016": 5, "FRCLM.017": 5, "FRCLM.018": 5,
            "FRCLM.019": 5, "FRCLM.020": 5, "FRCLM.021": 3, "FRCLM.022": 3, "FRCLM.023": 4, "FRCLM.024": 4}
_P = {"H": "Claims Handler", "T": "Claims Team Leader", "E": "Claims Team Head", "U": "Claims Unit Head",
      "R": "Claims / Risk user"}
STORIES = common.stories(C, _P, _EPIC_OF)
REPORT_AC: list = []

README = {
    "intro": ("This pack holds every document of Claims (BRD-07), from BDOI's FRS for Claims v1.0. Each document has "
              "one job. A requirement or rule is stated once, in the FRS, with its ID; the other documents refer to "
              "that ID."),
    "order": ["FRS", "BPS", "UXD", "RNO", "ICD", "DSP", "DDI", "USR", "RTM"],
    "programme": [
        ("Non-Functional and Security Requirements", "Performance, availability, security, audit, retention, backup, "
         "accessibility and browsers for every BRD."),
        ("Glossary", "Terms and abbreviations used in every pack."),
        ("Decision and Observation Log", "Decisions taken and observations raised across the BRDs."),
        ("Document Register", "Every current document of the BRD packs with version, date, owner and status."),
    ],
    "ids": [("FRCLM.nnn.nn", "Functional requirement (FRS; BDOI's numbering)"), ("BR-CLM-nn", "Business rule (FRS)"),
            ("Q-CL-nn", "Open question to BDOI (FRS, Requirements Collection)"),
            ("SCR-CLM-nn", "Screen (UX Design)"), ("SB-CLM-nn, P-CLM-nn", "Storyboard, process (Process and "
                                                                          "Storyboards)"),
            ("RPT-CLM-nn, DO-nn, NT-nn, AL-nn", "Report, document, notice, alert (Reports, Notifications and "
             "Outputs)"), ("IF-CLM-nn", "Interface (Interface Specification)"),
            ("US-CLM-nnn", "User story (User Stories)"), ("TC-FRCLM-…", "Test case (Test Cases and Traceability)"),
            ("CR-CLM-nnn, DW-CLM-nn", "Change request, delivery work (CR Register, Effort Estimate)"),
            ("NFR-…", "Non-functional requirement (programme NFR)")],
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
