"""BRD-11 User Access Maintenance: content of the standard document set that is not already in brd11_v15_content.py
and brd11_v15_pages.py. Read by build_pack.py (developer specification DSP, data dictionary DDI, user stories USR, the
"How to read this pack" note) and by build_brd11_v15_workbooks.py (traceability).

The facts of the services, screens and tables are read from the sources by tools/deliverables/api_facts.py and
schema_facts.py; this module only says which of them belong to User Access and gives them their business wording.
Every rule, message code, field and screen named here is checked against what those readers find.
"""

from __future__ import annotations

import brd11_v15_content as C
import brd11_v15_pages as P

BRD = "BRD-11"
NAME = "User Access Maintenance"
SHORT = "User Access"
FOLDER = "BRD-11_User_Access_Maintenance"
FILE_STEM = "BRD-11_User_Access"
DATE = C.DATE
DATE_US = C.DATE_US
PREPARED = "iorta TechNXT Project Team"
HEADER_RIGHT = C.HEADER_RIGHT
DOC_NO = "PRJ0012475"

# Current document set of the BRD: key -> (type, title in the file name, version, purpose, reviewer)
SET = [
    ("README", "Inputs", "How_to_Read_this_Pack", "1.0", "This note: the documents of the pack and who reviews them.",
     "All reviewers"),
    ("FRS", "FRS-BDOI", "User_Access_Maintenance", "1.6", "Requirements, business rules, validations, personas and "
     "permissions, parameters and lists of values.", "BDOI Business Unit owner; Information Security"),
    ("BPS", "BPS-BDOI", "User_Access_Process_and_Storyboards", "1.6", "To-be process flows and storyboards.",
     "BDOI Business Unit owner"),
    ("UXD", "UXD-BDOI", "User_Access_UX_Design", "1.6", "Every screen: purpose, users, fields, actions, states, "
     "messages, one screenshot per page.", "BDOI Business Unit owner; UAT lead"),
    ("RNO", "RNO-BDOI", "User_Access_Reports_Notifications_Outputs", "1.6", "Reports, certification extract, "
     "exports, notices, e-mails and jobs.", "BDOI Business Unit owner; Internal Audit"),
    ("ICD", "ICD-BDOI", "User_Access_Interface_Specification", "1.1", "SCIM 2.0 with UIDM-ISC and OpenID Connect "
     "with Microsoft Entra ID, with the OpenAPI description and the interface test pack.", "BDOI IT (UIDM-ISC, Entra "
     "ID, API gateway)"),
    ("DSP", "DSP-BDOI", "User_Access_Developer_Specification", "1.0", "Screens, service operations, field "
     "validations, rules, states, jobs, audit events and permissions, as the system works.", "BDOI IT; iorta "
     "TechNXT developers and testers"),
    ("DDI", "DDI-BDOI", "User_Access_Data_Dictionary", "1.0", "Tables, columns, lists of values, parameters, "
     "status models and indexes.", "BDOI IT (database administration); Data Migration"),
    ("USR", "USR-BDOI", "User_Access_User_Stories", "1.0", "User stories with acceptance criteria, ready to import "
     "into the backlog tool.", "BDOI Product Owner; UAT lead"),
    ("RTM", "RTM", "Test_Cases_and_Traceability", "1.3", "Test cases and the trace BRD, FRUM, user story, screen, "
     "test case.", "UAT lead; Internal Audit"),
    ("FIT", "FitGap", "BRD_vs_OOTB_vs_Best_Practice", "1.3", "Each requirement against the BRD and the OOTB "
     "platform.", "BDOI Product Owner"),
    ("CRR", "CR", "Change_Request_Register", "1.3", "Change requests and their status.", "BDOI Product Owner"),
    ("CRE", "CR", "Effort_Estimate", "1.3", "Effort of the open change requests.", "BDOI Product Owner"),
    ("INP", "Inputs", "Business_Unit_Requirements_Collection", "1.3", "Values the Business Unit is asked to give.",
     "BDOI Business Unit owner"),
    ("WTU", "UAT", "Business_Unit_Walkthrough_Users", "1.3", "Accounts for the walkthroughs.", "UAT lead"),
]
EXT = {"README": "docx", "FRS": "docx", "BPS": "docx", "UXD": "docx", "RNO": "docx", "ICD": "docx", "DSP": "docx",
       "DDI": "xlsx", "USR": "xlsx", "RTM": "xlsx", "FIT": "xlsx", "CRR": "xlsx", "CRE": "xlsx", "INP": "xlsx",
       "WTU": "xlsx"}
SET.append(("ACX", "Access", "Access_Matrix", "1.0", "Group profile x screen x action, from the permissions, group profiles, menus and server checks of the release.", "BDOI Information Security; UAT lead"))
EXT["ACX"] = "xlsx"
# BRD-11 kept the shorter file names of its workbooks (no "User_Access" in them).
_WB_PLAIN = {"RTM", "FIT", "CRR", "CRE", "INP", "WTU"}


def file_name(key: str) -> str:
    kind, title, ver = next((k2, t, v) for k, k2, t, v, *_ in SET if k == key)
    stem = f"BIBS_{kind}_BRD-11_{title}" if key in _WB_PLAIN or key == "README" else f"BIBS_{kind}_BRD-11_{title}"
    return f"{stem}_v{ver}.{EXT[key]}"


def version(key: str) -> str:
    return next(v for k, _, _, v, *_ in SET if k == key)


DOCS = {
    "DSP": {"cover": "User Access Maintenance: Developer Specification", "header_left": "Developer Specification",
            "doc_no": f"{DOC_NO}-DSP"},
    "README": {"cover": "User Access Maintenance: How to Read this Pack", "header_left": "How to Read this Pack",
               "doc_no": f"{DOC_NO}-README"},
}

# ================================================================================================ developer spec
# Backend packages whose messages and audit records belong to User Access; the system package adds the settings.
PACKAGES = ["nbadmin", "security", "identity"]
MESSAGE_PACKAGES = ["nbadmin", "security", "identity", "system"]
SETTING_CLASSES = ("SystemParameter", "SecurityParameterApprovals")
# Controllers of the module (paths relative to the backend source root of the product).
CONTROLLERS = [
    "security/api/AuthController.java", "security/api/SsoController.java", "security/api/UserAdminController.java",
    "security/api/UserAccessDetailController.java", "security/api/DataScopeController.java",
    "security/api/UserSessionController.java", "identity/api/IdentityAdminController.java",
    "identity/integration/api/ScimProvisioningController.java", "identity/integration/api/ScimGroupsController.java",
    "nbadmin/api/AccessRequestController.java", "nbadmin/api/AccessDecisionController.java",
    "nbadmin/api/AccessMatrixController.java", "nbadmin/api/CertificationController.java",
    "nbadmin/api/SodRuleController.java", "nbadmin/api/UamSettingsController.java", "audit/api/AuditController.java",
]
# Controllers shared with other modules: only the operations a User Access screen calls are described.
SHARED_CONTROLLERS = ["report/api/ReportController.java"]
# Operations left out of the specification: password and second-factor self-service of local test accounts (kept in
# the Interface Specification and the FRS by reference), single sign-on protocol steps (Interface Specification).
OMIT_OPS = {"auth/password-reset/request", "auth/password-reset/check", "auth/password-reset/confirm",
            "auth/sso/saml/acs", "auth/sso/saml/metadata"}

AREAS = [
    # area, path prefix, what it covers
    ("Sign-in and sessions", ("auth/",), "Sign-in through Entra ID, test-account sign-in, session refresh, sign-out."),
    ("Users", ("admin/users", "admin/sessions", "admin/data-scope", "admin/roles", "admin/permissions"),
     "Users received from UIDM-ISC, their group profiles, permissions, data access, sessions, unlock."),
    ("Identity synchronisation", ("admin/identity", "integration/"),
     "SCIM 2.0 provisioning from UIDM-ISC, monitoring, refused events, reconciliation."),
    ("Group profiles", ("nbadmin/access-requests", "nbadmin/access-settings", "nbadmin/users", "nbadmin/roles"),
     "Group-profile changes under maker-checker and their decisions."),
    ("Separation of duties", ("nbadmin/sod-rules",), "Rules under maker-checker and the daily violation list."),
    ("Reports and certification", ("nbadmin/access-matrix", "nbadmin/certification", "audit-logs", "reports/"),
     "Access matrix, certification extract and audit log."),
    ("UAM settings", ("nbadmin/uam-settings",), "Settings under maker-checker."),
]

COMPONENTS = [
    ("User Access screens", "Eleven screens under Setup & Administration › User Access, and the sign-in page.",
     "Section 3"),
    ("Security service", "Sign-in, sessions, users, group profiles as stored, data access, unlock, session end.",
     "Section 4"),
    ("Identity service", "SCIM 2.0 service for UIDM-ISC, identity events, monitoring and reconciliation.",
     "Section 4; Interface Specification v1.1"),
    ("Access administration service", "Group-profile changes, decisions, separation of duties, access matrix, "
     "certification extract, UAM settings.", "Section 4"),
    ("Audit trail", "Insert-only record of every user access action.", "Section 8"),
    ("Scheduled jobs", "Dormant users, reconciliation, separation-of-duties check, session sweep, password notices.",
     "Section 7"),
]

# Screens of the specification. route/permission come from the screen register of the product (checked); page and
# extra are the screen sources read for the service calls; uxd: the screens of the UX Design they cover.
SCREENS = [
    {"id": "SCR-UA-01", "name": "Sign in", "route": "login", "uxd": "SCR-UA-01", "frum": "FRUM.001, FRUM-004",
     "calls": [("GET", "/auth/sign-in-options", "signInOptions"), ("GET", "/auth/sso/start", "startSingleSignOn"),
               ("POST", "/auth/sso/complete", "completeSingleSignOn"), ("POST", "/auth/login", "login"),
               ("GET", "/auth/me", "me")],
     "permission": "None (public page)",
     "loads": "The sign-in options of the environment: Entra ID, and the test-account form outside production.",
     "states": ["Production: only the Sign in with Microsoft Entra ID button.",
                "DEV, SIT, UAT: the test-account form (User ID, Password) is shown under the button.",
                "A refused sign-in shows the message under the button; the form keeps the User ID."],
     "fields": [
         {"label": "User ID", "dto": ("LoginRequest", "username"), "lov": "-",
          "client": "Required by the browser.", "msg": "", "codes": ["AUTHENTICATION_FAILED", "LOGIN_RATE_LIMITED"]},
         {"label": "Password", "dto": ("LoginRequest", "password"), "lov": "-", "client": "Required by the browser; "
          "masked, with Show/Hide.", "msg": "", "codes": ["AUTHENTICATION_FAILED"]},
     ],
     "codes": ["AUTHENTICATION_FAILED", "LOGIN_RATE_LIMITED", "SIGN_IN_UNAVAILABLE", "SIGN_IN_CHECK_UNAVAILABLE"]},
    {"id": "SCR-UA-02", "name": "Users", "route": "access/users", "uxd": "SCR-UA-02", "frum": "FRUM.002, FRUM.003",
     "page": ("useraccess", "UsersPage.tsx"),
     "loads": "Every user with status, group profiles and last sign-in; the group profiles for the filter.",
     "states": ["Filters (search, status, group profile, business unit) apply in the browser; Export (CSV) writes "
                "the filtered rows.", "No New User or Edit action: users come from UIDM-ISC (BR-UA-01)."],
     "fields": [], "codes": []},
    {"id": "SCR-UA-03", "name": "User", "route": "access/users/:id", "uxd": "SCR-UA-03 to SCR-UA-08",
     "frum": "FRUM.003, FRUM.007", "page": ("useraccess", "UserDetailPage.tsx"),
     "loads": "The user, the directory data from UIDM-ISC, group profiles held, effective permissions, data access, "
              "sessions and access history (one call per tab, on opening the tab).",
     "states": ["Action buttons show only with USER_MANAGE; the Auditor sees the same tabs read-only.",
                "Unlock shows only for a locked test account; End Sessions only when a session is open.",
                "Save Data Access is enabled after a change and while no check fails."],
     "fields": [
         {"label": "All companies", "dto": ("DataScopeRequest", "allCompanies"), "lov": "Yes / No", "default": "Yes",
          "rule": "BR-UA-14",
          "client": "-", "msg": "", "codes": []},
         {"label": "Companies and branches", "dto": ("DataScopeRequest", "companies"), "lov": "Companies and "
          "branches within the administrator's own data access", "client": "At least one company, and one branch per "
          "company unless all its branches are given.", "msg": "",
          "codes": ["DATA_SCOPE_EMPTY", "DATA_SCOPE_NO_BRANCH", "DATA_SCOPE_BEYOND_OWN", "DATA_SCOPE_SELF_CHANGE",
                    "DATA_SCOPE_UNKNOWN_COMPANY", "DATA_SCOPE_UNKNOWN_BRANCH"]},
     ],
     "codes": ["SESSION_ALREADY_ENDED", "DATA_SCOPE_SELF_CHANGE", "NOT_FOUND"]},
    {"id": "SCR-UA-09", "name": "Group Profiles", "route": "access/group-profiles", "uxd": "SCR-UA-09",
     "frum": "FRUM.005, FRUM.006", "page": ("useraccess", "GroupProfilesPage.tsx"),
     "loads": "Every group profile with level, status, members and open change; the access matrix for the "
              "permission picker.",
     "states": ["New Group Profile shows only with UAM_GROUP_REQUEST or ACCESS_REQUEST.",
                "A profile with an open change shows its status; Propose Change is then disabled (BR-UA-07)."],
     "fields": [], "codes": []},
    {"id": "SCR-UA-10", "name": "Group Profile", "route": "access/group-profiles/:code",
     "uxd": "SCR-UA-10, SCR-UA-12", "frum": "FRUM.006, FRUM.010", "page": ("useraccess", "GroupProfileDetailPage.tsx"),
     "loads": "The profile, its permissions and members, its open change and the history of its changes.",
     "states": ["The actions offered depend on the status of the open change and on the user (table below).",
                "The maker never sees Approve, Return or Reject on an own change (BR-UA-04).",
                "Give Second Approval shows only on Pending Second and never to the first approver."],
     "fields": [
         {"label": "Reason (Reject, Return for Correction, Cancel Change)", "dto": ("DecisionRequest", "comment"),
          "lov": "-", "client": "Required for Reject, Return and Cancel; up to 500 characters.",
          "msg": "Enter the reason.", "codes": ["ACCESS_REJECT_REASON", "ACCESS_RETURN_REASON",
                                                 "ACCESS_CANCEL_REASON"]},
         {"label": "Comment (Approve, Give Second Approval)", "dto": ("DecisionRequest", "comment"), "lov": "-",
          "client": "Optional; up to 500 characters.", "msg": "", "codes": []},
     ],
     "codes": ["ACCESS_FOUR_EYES", "ACCESS_SUBJECT_DECIDES", "ACCESS_SECOND_SAME_APPROVER",
               "ACCESS_NOT_SECOND_APPROVAL", "ACCESS_IMPLEMENTER_IS_REQUESTER", "ACCESS_REQUEST_DECIDED",
               "ACCESS_REQUEST_NOT_FOR_IMPLEMENTATION", "ACCESS_PERMISSION_COMBINATION"]},
    {"id": "SCR-UA-11", "name": "Propose a change (dialog)", "route": "access/group-profiles (dialog)",
     "uxd": "SCR-UA-11", "frum": "FRUM.005, FRUM.006", "page": ("useraccess", "ProposeChangeDialog.tsx"),
     "permission_override": ("UAM_GROUP_REQUEST", ["ACCESS_REQUEST"]),
     "loads": "Opened from Group Profiles or a group profile; uses the data already loaded there.",
     "states": ["Save Draft needs the type and the profile only; Submit for Approval runs every check.",
                "A returned change opens with its content and asks what was corrected.",
                "New Name, Description and Level left blank keep the current value."],
     "fields": [
         {"label": "Change type", "dto": ("AccessRequestRequest", "type"), "lov": "New group profile; Change of "
          "definition or permissions; Deactivation; Reactivation", "client": "Set by the button that opened the "
          "dialog.", "msg": "", "codes": ["ACCESS_TYPE", "ACCESS_REQUEST_TYPE_FIXED"]},
         {"label": "Code", "dto": ("AccessRequestRequest", "roleCode"), "lov": "New profile: free; change: the "
          "chosen profile", "client": "Capital letters, digits and _, 1 to 40.",
          "msg": "Enter the code: capital letters, digits and _ (up to 40)",
          "codes": ["ACCESS_ROLE_CODE", "DUPLICATE", "ACCESS_ROLE", "ACCESS_UNKNOWN_ROLE"]},
         {"label": "Name / New Name", "dto": ("AccessRequestRequest", "roleName"), "lov": "-",
          "client": "Mandatory for a new profile on submission.", "msg": "Enter the name of the group profile",
          "codes": ["ACCESS_ROLE_NAME"]},
         {"label": "Description", "dto": ("AccessRequestRequest", "roleDescription"), "lov": "-",
          "client": "Optional.", "msg": "", "codes": []},
         {"label": "Privilege Level", "dto": ("AccessRequestRequest", "privilegeLevel"),
          "lov": "Low, Standard, High, Admin", "client": "Optional on a change.", "msg": "", "codes": []},
         {"label": "Permissions", "dto": ("AccessRequestRequest", "permissionsAdded"), "lov": "Permissions of the "
          "access matrix", "client": "New profile: at least one. Change: something must change.",
          "msg": "Select at least one permission",
          "codes": ["ACCESS_NO_PERMISSION", "ACCESS_NO_PERMISSION_CHANGE", "ACCESS_UNKNOWN_PERMISSION",
                    "ACCESS_PERMISSION_CONFLICT", "ACCESS_PERMISSION_LIST_TOO_LONG",
                    "ACCESS_PERMISSION_COMBINATION"]},
         {"label": "Reason for the change", "dto": ("AccessRequestRequest", "justification"), "lov": "-",
          "client": "Mandatory on submission.", "msg": "Enter the reason for the change",
          "codes": ["ACCESS_JUSTIFICATION"]},
         {"label": "What was corrected (returned change)", "dto": ("AccessSubmitRequest", "remarks"), "lov": "-",
          "client": "Mandatory when resubmitting a returned change.", "msg": "Enter what was corrected",
          "codes": ["ACCESS_CORRECTION_REMARKS"]},
     ],
     "codes": ["ACCESS_REQUEST_PENDING", "ACCESS_USERS_BY_UIDM", "ACCESS_ROLE_ALREADY_ACTIVE",
               "ACCESS_ROLE_ALREADY_INACTIVE", "ACCESS_ROLE_PROTECTED"]},
    {"id": "SCR-UA-13", "name": "Identity Sync", "route": "access/identity-sync", "uxd": "SCR-UA-13",
     "frum": "FRUM.014", "page": ("useraccess", "IdentitySyncPage.tsx"),
     "loads": "Connection status, provisioning settings, events (filtered, 25 a page), refused events and open "
              "reconciliation mismatches.",
     "states": ["Retry and Dismiss show only on a Refused or Failed event.",
                "Resolve shows only on an open mismatch.", "Synchronise Now runs the reconciliation job at once."],
     "fields": [
         {"label": "Remark (Dismiss, Resolve)", "dto": ("Remark", "remark"), "lov": "-",
          "client": "Required; up to 500 characters.", "msg": "Enter the reason.",
          "codes": ["IDENTITY_REMARK"]},
     ],
     "codes": ["IDENTITY_ALREADY_DISMISSED", "IDENTITY_NOT_REFUSED", "IDENTITY_MISMATCH_RESOLVED"]},
    {"id": "SCR-UA-14", "name": "Separation of Duties", "route": "access/separation-of-duties", "uxd": "SCR-UA-14",
     "frum": "FRUM.012", "page": ("nbadmin", "SodRulesPage.tsx"), "extra": [("useraccess", "SodViolationsTab.tsx")],
     "loads": "Every rule with its status; the group profiles and permissions for the pickers; the violations.",
     "states": ["Authorise and Reject show only on a pending rule and never to its maker (BR-UA-04).",
                "Deactivate shows only on an active rule."],
     "fields": [
         {"label": "Rule Type", "dto": ("SodRuleRequest", "kind"), "lov": "Group profiles; Permissions",
          "default": "Group profiles",
          "client": "Required; Group profiles by default.", "msg": "", "codes": []},
         {"label": "Group Profile / Permission (first)", "dto": ("SodRuleRequest", "profileA"),
          "lov": "Active group profiles, or permissions", "client": "Required.", "msg": "",
          "codes": ["SOD_UNKNOWN_PROFILE"]},
         {"label": "Group Profile / Permission (second)", "dto": ("SodRuleRequest", "profileB"),
          "lov": "Active group profiles, or permissions", "client": "Required; different from the first.",
          "msg": "Choose two different group profiles", "codes": ["SOD_SAME_PROFILE", "SOD_RULE_EXISTS"]},
         {"label": "Reason", "dto": ("SodRuleRequest", "description"), "lov": "-", "client": "Required.",
          "msg": "Enter why the two profiles are not held together", "codes": ["SOD_DESCRIPTION"]},
         {"label": "Reason of a rejection", "dto": ("ReasonRequest", "reason"), "lov": "-", "client": "Required.",
          "msg": "Enter the reason.", "codes": ["REASON_REQUIRED"]},
     ],
     "codes": ["MAKER_CHECKER_VIOLATION", "SOD_RULE_NOT_PENDING"]},
    {"id": "SCR-UA-15", "name": "Access Matrix", "route": "access/matrix", "uxd": "SCR-UA-15", "frum": "FRUM.007",
     "page": ("nbadmin", "AccessMatrixPage.tsx"),
     "loads": "Group profiles against permissions, and by action for the area chosen.",
     "states": ["Two views: by permission and by action; the search filters rows in the browser.",
                "Export writes both views to one Excel file and is recorded in the audit trail."],
     "fields": [], "codes": []},
    {"id": "SCR-UA-16", "name": "Reports & Certification", "route": "access/reports", "uxd": "SCR-UA-16",
     "frum": "FRUM.007", "page": ("useraccess", "ReportsPage.tsx"),
     "loads": "The five reports (opened in the report runner with their parameters) and the log of extracts.",
     "states": ["Take Extract asks for the format, then downloads the file and adds a line to the log."],
     "fields": [
         {"label": "Format", "dto": ("ExtractRequest", "format"), "lov": "CSV, XLSX", "client": "Chosen with the "
          "button.", "msg": "", "codes": ["CERTIFICATION_FORMAT"]},
         {"label": "As-of date (reports)", "dto": None, "type": "Date", "lov": "-", "client": "Not after today.",
          "msg": "", "codes": ["AS_OF_IN_FUTURE", "DATE_RANGE_REVERSED"]},
     ],
     "codes": ["AS_OF_IN_FUTURE", "DATE_RANGE_REVERSED", "CERTIFICATION_FORMAT"]},
    {"id": "SCR-UA-17", "name": "Audit Log", "route": "access/audit", "uxd": "SCR-UA-17", "frum": "FRUM-008",
     "page": ("useraccess", "AuditLogPage.tsx"), "extra": [("admin", "AuditTrailView.tsx")],
     "loads": "The audit trail limited to the user access records, newest first, with filters and export.",
     "states": ["Read-only. Export writes the filtered rows."], "fields": [], "codes": []},
    {"id": "SCR-UA-18", "name": "UAM Settings", "route": "access/settings", "uxd": "SCR-UA-18", "frum": "FRUM.012",
     "page": ("useraccess", "UamSettingsPage.tsx"),
     "loads": "The 21 settings of User Access with any change waiting for approval.",
     "states": ["Propose Change shows to USER_MANAGE or SYSTEM_PARAMETER_MANAGE; Approve and Reject to "
                "SECURITY_PARAMETER_APPROVE.", "A pending change shows the old and the new value; the old value "
                "applies until approval (BR-UA-11).", "The maker may withdraw an own pending change."],
     "fields": [
         {"label": "New value", "dto": ("ParameterUpdateRequest", "value"), "lov": "Per setting (type and range "
          "in the data dictionary)", "client": "Required; whole number within the range for a number; Yes/No for "
          "a switch.", "msg": "Enter a value", "codes": ["INVALID_PARAMETER_VALUE", "PARAMETER_CHANGE_PENDING",
                                                         "UAM_SETTING_UNKNOWN"]},
         {"label": "Reason (reject)", "dto": ("RejectRequest", "reason"), "lov": "-",
          "client": "Required to reject; up to 200 characters.", "msg": "Enter the reason for the rejection",
          "codes": ["REASON_REQUIRED"]},
     ],
     "codes": ["MAKER_CHECKER_VIOLATION", "PARAMETER_NO_PENDING_CHANGE", "PARAMETER_CHANGE_PENDING"]},
]

# UX Design screen that shows the input fields of each screen of this specification.
UX_INPUTS = {"SCR-UA-01": "SCR-UA-01", "SCR-UA-03": "SCR-UA-06", "SCR-UA-10": "SCR-UA-12", "SCR-UA-11": "SCR-UA-11",
             "SCR-UA-13": "SCR-UA-13", "SCR-UA-14": "SCR-UA-14", "SCR-UA-16": "SCR-UA-16", "SCR-UA-18": "SCR-UA-18"}

# Business names of the client calls (screen actions); a call not named here keeps its function name humanised.
ACTION_LABELS = {
    "unlockUser": "Unlock", "endSessions": "End Sessions", "replace": "Save Data Access",
    "create": "Save Draft / Submit for Approval (new)", "edit": "Save Draft (edit)",
    "submitSaved": "Submit for Approval / Resubmit", "approve": "Approve", "secondApprove": "Give Second Approval",
    "reject": "Reject", "returnRequest": "Return for Correction", "cancel": "Cancel Change", "implement": "Implement",
    "synchroniseNow": "Synchronise Now", "resolveMismatch": "Resolve", "reprocess": "Retry",
    "dismissEvent": "Dismiss", "createSodRule": "New Rule", "deactivateSodRule": "Deactivate",
    "authorizeSodRule": "Authorise", "rejectSodRule": "Reject", "exportMatrix": "Export",
    "takeExtract": "Take Extract", "changeSetting": "Propose Change", "approveSetting": "Approve",
    "rejectSetting": "Reject / Withdraw", "export": "Export", "login": "Sign in (test account)",
    "startSingleSignOn": "Sign in with Microsoft Entra ID", "completeSingleSignOn": "Return from Entra ID",
}

# Business names of the data a screen loads (client calls that read).
DATA_LABELS = {
    "signInOptions": "Sign-in options of the environment", "me": "Signed-in user and permissions",
    "users": "Users", "roles": "Group profiles", "units": "Companies and branches the administrator may grant",
    "ofUser": "Data access of the user", "profile": "Directory data from UIDM-ISC",
    "groupProfilesOf": "Group profiles held", "permissionsOf": "Effective permissions", "sessions": "Sessions",
    "accessHistory": "Access history", "requests": "Group-profile changes", "matrix": "Access matrix",
    "matrixByAction": "Access matrix by action", "identityStatus": "Connection status with UIDM-ISC",
    "settings": "Settings", "mismatches": "Reconciliation mismatches", "events": "Identity events",
    "sodRules": "Separation-of-duties rules", "sodViolations": "Violations", "extracts": "Log of extracts",
    "audit": "Audit records",
}
# Permissions shown in full; a shared operation lists these and says that other modules' permissions also open it.
PERMISSION_PREFIXES = ("UAM_", "USER_", "ROLE_", "ACCESS_", "AUDIT_", "SECURITY_", "SYSTEM_PARAMETER")

# Which personas hold which group profile in the walkthroughs (role codes of the product).
PERSONA_ROLES = [
    ("User Access Administrator", "UAM_ADMINISTRATOR"), ("Group Profile Maker", "UAM_REQUESTOR"),
    ("Group Profile Checker", "UAM_APPROVER"), ("Second checker", "UAM_SECOND_APPROVER"),
    ("Information Security", "INFOSEC_OFFICER"), ("Auditor", "UAM_AUDITOR"), ("System Administrator", "SYSADMIN"),
]

# Where each business rule of the FRS is enforced. (rule, screen check, service check with its message code,
# data or job control)
RULE_ENFORCEMENT = [
    ("BR-UA-01", "No create or edit of users on Users.", "ACCESS_USERS_BY_UIDM on any request about a user.",
     "Users are written only by the SCIM service (UIDM-ISC client credential)."),
    ("BR-UA-02", "Production sign-in page offers Entra ID only.", "AUTHENTICATION_FAILED when the identity is not "
     "linked to an active user.", "Setting AUTH_MODE; session records the sign-in method."),
    ("BR-UA-03", "Test-account form outside production only.", "PASSWORD_MANAGED_BY_ORGANISATION where local "
     "passwords are off.", "Password history and expiry kept for test accounts only."),
    ("BR-UA-04", "Decision buttons hidden from the maker.", "ACCESS_FOUR_EYES; MAKER_CHECKER_VIOLATION (rules, "
     "settings).", "Maker and decider recorded on the change."),
    ("BR-UA-05", "Second approval step shown on Pending Second.", "ACCESS_SECOND_APPROVAL_PENDING; "
     "ACCESS_SECOND_SAME_APPROVER.", "Settings UAM_WORKING_HOURS; privilege level of the profile."),
    ("BR-UA-06", "Implement hidden from the maker.", "ACCESS_IMPLEMENTER_IS_REQUESTER.",
     "Setting UAM_ROLE_APPLY_ON_APPROVAL."),
    ("BR-UA-07", "Propose Change disabled while a change is open.", "ACCESS_REQUEST_PENDING.", "-"),
    ("BR-UA-08", "-", "ACCESS_SUBJECT_DECIDES; DATA_SCOPE_SELF_CHANGE.", "-"),
    ("BR-UA-09", "-", "ACCESS_PERMISSION_COMBINATION on submission and on approval.",
     "Daily job UAM_SOD_VIOLATIONS lists the users in breach."),
    ("BR-UA-10", "-", "SCIM leaver deactivates the user and ends every session.", "Session sweep every 15 minutes."),
    ("BR-UA-11", "Old value shown until approval.", "MAKER_CHECKER_VIOLATION; PARAMETER_CHANGE_PENDING.",
     "Pending value kept beside the value in force."),
    ("BR-UA-12", "-", "-", "Daily job UAM_DORMANT_USERS with UAM_DORMANT_DAYS and UAM_DORMANT_NOTICE_DAYS."),
    ("BR-UA-13", "Extract log on Reports & Certification.", "CERTIFICATION_FORMAT.",
     "Each extract recorded with rows and SHA-256; audit record."),
    ("BR-UA-14", "Data Access tab.", "DATA_SCOPE_BEYOND_OWN; DATA_SCOPE_EMPTY; DATA_SCOPE_NO_BRANCH.",
     "Every list and record filtered by the companies and branches of the user."),
]

# Status models: entity, enumeration in the product, label per code, the action that moves it on, next codes.
WORKFLOWS = {
    "Group-profile change": ("AccessRequestStatus", [
        ("DRAFT", "Draft", "Save Draft", "PENDING, CANCELLED"),
        ("PENDING", "Pending", "Submit for Approval / Resubmit", "PENDING_SECOND, FOR_IMPLEMENTATION, APPROVED, "
         "RETURNED, REJECTED, CANCELLED"),
        ("PENDING_SECOND", "Pending Second", "Approve (High or Admin profile, or out of hours)",
         "FOR_IMPLEMENTATION, APPROVED, REJECTED, CANCELLED"),
        ("RETURNED", "Returned", "Return for Correction", "PENDING, CANCELLED"),
        ("FOR_IMPLEMENTATION", "For Implementation", "Approve or Give Second Approval (implementation step on)",
         "IMPLEMENTED"),
        ("APPROVED", "Approved", "Approve or Give Second Approval (apply at approval)", "-"),
        ("IMPLEMENTED", "Implemented", "Implement", "-"),
        ("REJECTED", "Rejected", "Reject", "-"),
        ("CANCELLED", "Cancelled", "Cancel Change", "-"),
        ("SCHEDULED", "Scheduled", "Requests about users raised before UIDM-ISC (history only)", "-"),
    ]),
    "Identity event": ("IdentityEventStatus", [
        ("APPLIED", "Applied", "Event received and applied", "-"),
        ("NO_CHANGE", "No change", "Event received, nothing new", "-"),
        ("REFUSED", "Refused", "Event data not acceptable", "APPLIED (Retry); dismissed (Dismiss)"),
        ("FAILED", "Failed", "Technical failure", "APPLIED, REFUSED (Retry); dismissed (Dismiss)"),
    ]),
}

# Business names of the audit records (entity of the audit trail -> label). Records whose entity is a user name are
# sign-in records.
AUDIT_LABELS = {
    "AppUser": "User", "Role": "Group profile", "SodRule": "Separation-of-duties rule",
    "CertificationExtract": "Certification extract", "AccessMatrix": "Access matrix export",
    "IdentityMismatch": "Reconciliation mismatch", "Package version": "Group profile (change applied)",
    "username": "Sign-in", "user.getUsername()": "Sign-in or password", "replaced.getUsername()": "Session refresh",
    "event.getSource().label()": "Identity event", "source.actor()": "Identity event (applied)",
}
AUDIT_OMIT = {"RetentionRule"}
ACTION_WORDS = {"CREATE": "Created", "UPDATE": "Changed", "DEACTIVATE": "Deactivated", "SUBMIT": "Submitted",
                "AUTHORIZE": "Authorised", "REJECT": "Rejected or refused", "EXPORT": "Exported", "RUN": "Taken",
                "LOGIN": "Signed in", "LOGIN_FAILED": "Sign-in refused", "LOGOUT": "Signed out",
                "TIMEOUT": "Timed out", "INACTIVITY": "Inactivity warning shown"}

JOBS = {
    "UAM_DORMANT_USERS": "Deactivates users without a sign-in for UAM_DORMANT_DAYS; sends the notice "
                         "UAM_DORMANT_NOTICE_DAYS before.",
    "UAM_IDENTITY_RECONCILIATION": "Compares every user and its group profiles with UIDM-ISC; records mismatches.",
    "UAM_SOD_VIOLATIONS": "Lists users in breach of an active rule; tells Information Security of new ones.",
    "USER_SESSION_SWEEP": "Ends sessions past the time-out or the session end.",
    "PASSWORD_EXPIRY_NOTICE": "Tells test-account users their password expires within PASSWORD_EXPIRY_NOTICE_DAYS.",
}
JOB_OMIT = {"RETENTION_REVIEW"}

# ================================================================================================ data dictionary
TABLE_PATTERN = (r"^(sec_|idn_(?!sim_)|nba_access_request|nba_sod_|nba_certification_extract$|sys_role_home$|"
                 r"audit_log$|sys_parameter$)")
TABLES = {
    # table: (module, purpose, rows at go-live, rows after one year)
    "sec_user": ("Security", "One row per user received from UIDM-ISC, and per test account outside production.",
                 "600", "700"),
    "sec_role": ("Security", "Group profiles: code, name, level, status.", "40", "60"),
    "sec_role_permission": ("Security", "Permissions granted by each group profile.", "2,500", "3,000"),
    "sec_user_role": ("Security", "Group profiles held by each user (from UIDM-ISC).", "1,500", "1,800"),
    "sec_permission_action": ("Security", "Actions of the access matrix by permission.", "1,000", "1,000"),
    "sec_user_data_scope": ("Security", "Companies and branches a user may act for.", "100", "300"),
    "sec_user_session": ("Security", "Sign-in sessions: issued, last seen, ended and why.", "0", "300,000"),
    "sec_revoked_token": ("Security", "Tokens refused before their expiry (sign-out, session end).", "0", "5,000"),
    "sec_access_change_log": ("Security", "Every change of a user's access, with the UIDM request number.", "0",
                              "20,000"),
    "sec_sso_request": ("Security", "Open sign-in requests at Entra ID (state, nonce); short-lived.", "0", "500"),
    "sec_sso_ticket": ("Security", "One-time tickets that hand the Entra ID sign-in to the browser.", "0", "500"),
    "sec_password_history": ("Security", "Previous password hashes of test accounts.", "0", "500"),
    "sec_password_reset_token": ("Security", "Password reset links of test accounts.", "0", "200"),
    "sec_user_mfa": ("Security", "Authenticator enrolment of test accounts (second factor outside Entra ID).", "0",
                     "50"),
    "sec_mfa_recovery_code": ("Security", "Recovery codes of the authenticator enrolment.", "0", "500"),
    "sec_mfa_reset_request": ("Security", "Requests to reset an authenticator enrolment.", "0", "50"),
    "sec_mfa_trusted_device": ("Security", "Devices remembered for the second factor.", "0", "100"),
    "idn_directory_profile": ("Identity", "Directory data of each user as last sent by UIDM-ISC.", "600", "700"),
    "idn_identity_event": ("Identity", "Every SCIM event received from UIDM-ISC with its outcome.", "600", "10,000"),
    "idn_event_dismissal": ("Identity", "Dismissals of refused or failed events, with the remark.", "0", "200"),
    "idn_reconciliation_mismatch": ("Identity", "Differences found by the daily reconciliation.", "0", "1,000"),
    "nba_access_request": ("Access administration", "Group-profile changes (and the user requests raised before "
                           "UIDM-ISC, read-only).", "0", "1,000"),
    "nba_access_request_event": ("Access administration", "History of each change: who did what, when.", "0",
                                 "6,000"),
    "nba_access_request_approver": ("Access administration", "Approvers of the user requests raised before "
                                    "UIDM-ISC (history only).", "0", "0"),
    "nba_access_request_batch": ("Access administration", "Bulk user requests raised before UIDM-ISC (history "
                                 "only).", "0", "0"),
    "nba_sod_rule": ("Access administration", "Separation-of-duties rules under maker-checker.", "20", "40"),
    "nba_sod_violation": ("Access administration", "Users in breach of an active rule, found by the daily check.",
                          "0", "200"),
    "nba_certification_extract": ("Access administration", "Log of certification extracts with rows and SHA-256.",
                                  "0", "50"),
    "sys_role_home": ("Platform", "Home page widgets of each group profile under maker-checker.", "40", "60"),
    "audit_log": ("Platform (shared)", "Insert-only audit trail; User Access writes the records listed in the "
                  "developer specification, section 8.", "0", "500,000 (User Access share)"),
    "sys_parameter": ("Platform (shared)", "Parameters; the UAM settings are the rows of category SECURITY.", "500",
                      "500"),
}
PARAMETER_FILTER = "category = 'SECURITY' or param_key like 'MIG_ACCESS%' or param_key like 'MIG_LEGACY_ACCESS%'"
LOV_TYPES = ("UAM_DEACTIVATION_REASON", "UAM_USER_LEVEL")
# Enumerations of the services that are code lists of the screens: (list name, enumeration, label source)
ENUM_LISTS = [("Privilege level", "PrivilegeLevel"), ("Change type", "AccessRequestType"),
              ("Change status", "AccessRequestStatus"), ("Identity event outcome", "IdentityEventStatus"),
              ("Separation-of-duties rule type", "SodRuleKind")]
ENUM_LABELS = {
    "LOW": "Low", "STANDARD": "Standard", "HIGH": "High", "ADMIN": "Admin",
    "CREATE_ROLE": "New group profile", "MODIFY_ROLE_PERMISSIONS": "Change of definition or permissions",
    "DEACTIVATE_ROLE": "Deactivation", "REACTIVATE_ROLE": "Reactivation",
    "CREATE_USER": "User enrolment (before UIDM-ISC)", "MODIFY_USER": "User change (before UIDM-ISC)",
    "MODIFY_ROLES": "User group profiles (before UIDM-ISC)", "DISABLE_USER": "User deactivation (before UIDM-ISC)",
    "ENABLE_USER": "User reactivation (before UIDM-ISC)", "PROFILES": "Group profiles", "PERMISSIONS": "Permissions",
    "APPLIED": "Applied", "NO_CHANGE": "No change", "REFUSED": "Refused", "FAILED": "Failed",
}
INACTIVE_CODES = {"CREATE_USER", "MODIFY_USER", "MODIFY_ROLES", "DISABLE_USER", "ENABLE_USER", "SCHEDULED"}

# ================================================================================================ user stories
EPICS = [
    ("EP-UA-01", "Sign-in and sessions"), ("EP-UA-02", "Users from UIDM-ISC"),
    ("EP-UA-03", "User administration"), ("EP-UA-04", "Group-profile changes"),
    ("EP-UA-05", "Decisions on group-profile changes"), ("EP-UA-06", "Identity synchronisation"),
    ("EP-UA-07", "Separation of duties and settings"), ("EP-UA-08", "Reports, certification and audit"),
]
# story: id, epic, persona, I want, so that, [(given, when, then)], FRUM refs, rules, screen, priority, points
STORIES = [
    ("US-UA-001", "EP-UA-01", "Any user", "to sign in with my Microsoft Entra ID account",
     "I use one bank identity and second factor for BIBS",
     [("my Entra ID account is linked to an active BIBS user", "I click Sign in with Microsoft Entra ID and pass "
       "Entra ID", "I land on my home page with the menus of my group profiles"),
      ("my identity is not linked to an active user", "Entra ID returns me to BIBS",
       "I see \"Your account is not active in BIBS\" and no session opens")],
     "FRUM.001.01, FRUM.001.02", "BR-UA-02", "SCR-UA-01", "High", 3),
    ("US-UA-002", "EP-UA-01", "Any user", "to be warned before I am signed out for inactivity",
     "I do not lose my work without notice",
     [("I am signed in", "I do nothing for the inactivity warning period", "a warning offers to stay signed in"),
      ("the warning is shown", "I do nothing until the time-out", "I am signed out and see the timed-out page")],
     "FRUM.001.03, FRUM.001.04", "-", "SCR-UA-01", "High", 2),
    ("US-UA-003", "EP-UA-01", "Tester", "to sign in with a local test account in DEV, SIT and UAT only",
     "tests run without bank identities",
     [("the environment is production", "I open the sign-in page", "no password form is shown"),
      ("the environment is UAT", "I enter a wrong password the allowed number of times",
       "the test account is locked")],
     "FRUM.001.05, FRUM-004.02", "BR-UA-03", "SCR-UA-01", "Medium", 2),
    ("US-UA-030", "EP-UA-01", "Information Security", "bank passwords and second factors to stay in Entra ID, "
     "and test-account passwords to be reset only by another administrator", "BIBS holds no bank password",
     [("a bank user opens My Profile", "the page loads", "no password change is offered"),
      ("I am a System Administrator", "I try to reset my own test-account password",
       "it is refused: another System Administrator resets it")],
     "FRUM-004.01, FRUM-004.03", "BR-UA-02, BR-UA-03", "SCR-UA-01", "Medium", 1),
    ("US-UA-004", "EP-UA-02", "User Access Administrator", "joiners to be created in BIBS from UIDM-ISC",
     "access is given only through the approved bank process",
     [("UIDM-ISC sends an approved joiner", "BIBS receives it", "the user exists with the group profiles sent and "
       "the UIDM request number"),
      ("the User ID or Windows ID belongs to another user", "the joiner arrives",
       "the event is refused and listed on Identity Sync")],
     "FRUM.002.01, FRUM.002.03", "BR-UA-01", "SCR-UA-13", "High", 3),
    ("US-UA-005", "EP-UA-02", "User Access Administrator", "movers and group-profile changes from UIDM-ISC to "
     "apply at once", "a user's access matches the approved request",
     [("UIDM-ISC sends a mover", "BIBS applies it", "the details change and the change is in the access history"),
      ("UIDM-ISC changes a group-profile membership", "BIBS applies it", "the user's permissions change at the "
       "next request")],
     "FRUM.003.01", "BR-UA-01", "SCR-UA-03", "High", 3),
    ("US-UA-006", "EP-UA-02", "Information Security", "a leaver to lose access and every session at once",
     "no one keeps access after leaving",
     [("a user has open sessions", "UIDM-ISC sends the leaver", "the user is Inactive and every session ends")],
     "FRUM.003.02", "BR-UA-10", "SCR-UA-03", "High", 2),
    ("US-UA-007", "EP-UA-02", "User Access Administrator", "BIBS to refuse any local creation or change of a user",
     "UIDM-ISC stays the only source",
     [("I am on Users", "I look for New User or Edit", "no such action exists"),
      ("a request about a user reaches the service", "it is checked", "it is refused with \"Users are maintained in "
       "UIDM-ISC\"")],
     "FRUM.002.02", "BR-UA-01", "SCR-UA-02", "High", 1),
    ("US-UA-008", "EP-UA-03", "User Access Administrator", "to set the companies and branches a user may act for",
     "users see only the records of their units",
     [("I hold a wider data access than the user", "I select companies and branches and save",
       "the user's lists show only those units"),
      ("I select a company without any branch", "I save", "I see \"Select at least one branch\"")],
     "FRUM.003.03", "BR-UA-14", "SCR-UA-03", "Medium", 5),
    ("US-UA-009", "EP-UA-03", "User Access Administrator", "to end every session of a user",
     "I can stop access at once during an incident",
     [("the user has open sessions", "I click End Sessions and confirm", "every session ends and the action is "
       "audited")],
     "FRUM.003.04", "BR-UA-10", "SCR-UA-03", "Medium", 1),
    ("US-UA-010", "EP-UA-03", "User Access Administrator", "to unlock a locked test account",
     "testers can resume without a new account",
     [("a test account is locked", "I click Unlock", "the account is Active and the failed count is reset")],
     "FRUM.003.05", "BR-UA-03", "SCR-UA-03", "Low", 1),
    ("US-UA-011", "EP-UA-03", "Information Security", "dormant users to be deactivated after a notice",
     "unused accounts do not stay open",
     [("a user has not signed in for the dormancy period", "the daily run starts", "the user is Inactive"),
      ("the dormancy date is within the notice period", "the daily run starts", "the user receives the notice")],
     "FRUM.003.06", "BR-UA-12", "SCR-UA-18", "Medium", 2),
    ("US-UA-012", "EP-UA-04", "Group Profile Maker", "to propose a new group profile with its permissions",
     "a new job gets exactly the access it needs",
     [("I enter code, name, level, permissions and reason", "I submit", "the change is Pending and the checkers are "
       "told"),
      ("the code holds lower-case letters", "I submit", "I see \"Enter the code: capital letters, digits and _\""),
      ("no permission is selected", "I submit", "I see \"Select at least one permission\"")],
     "FRUM.005.01, FRUM.005.02", "BR-UA-04", "SCR-UA-11", "High", 3),
    ("US-UA-013", "EP-UA-04", "Group Profile Maker", "to propose a change, deactivation or reactivation of a "
     "group profile", "profiles follow the organisation",
     [("the profile has no open change", "I propose a change and submit", "the change is Pending"),
      ("the profile has an open change", "I try to propose another", "I see \"A change of this group profile is "
       "already open\"")],
     "FRUM.006.01, FRUM.006.02, FRUM.006.03", "BR-UA-07", "SCR-UA-11", "High", 3),
    ("US-UA-014", "EP-UA-04", "Group Profile Maker", "to save a draft and correct a returned change",
     "I can finish a proposal later or fix it",
     [("I saved a draft", "I open it and submit", "it is Pending"),
      ("my change was returned", "I correct it, say what was corrected and resubmit", "it is Pending again")],
     "FRUM.005.01, FRUM.010.01", "BR-UA-04", "SCR-UA-11", "Medium", 2),
    ("US-UA-015", "EP-UA-05", "Group Profile Checker", "to approve, return or reject a change made by someone "
     "else", "no one changes access alone",
     [("a change is Pending and I am not its maker", "I approve it", "it applies, or waits for the second approval "
       "or the implementation"),
      ("I am the maker", "I open the change", "no decision button is shown"),
      ("I reject without a reason", "I confirm", "I see \"Enter the reason.\"")],
     "FRUM.010.01, FRUM.010.03", "BR-UA-04, BR-UA-08", "SCR-UA-10", "High", 3),
    ("US-UA-016", "EP-UA-05", "Second checker", "to give the second approval of a privileged or out-of-hours "
     "change", "high-risk changes have two approvers",
     [("a High or Admin profile change is approved once", "another checker gives the second approval",
       "the change applies or goes to implementation"),
      ("I gave the first approval", "I open the change", "Give Second Approval is not offered")],
     "FRUM.010.02", "BR-UA-05", "SCR-UA-10", "High", 2),
    ("US-UA-017", "EP-UA-05", "System Administrator", "to implement an approved change when the setting asks for "
     "it", "changes apply at a controlled moment",
     [("the change is For Implementation and I am not its maker", "I click Implement", "the profile changes and "
       "the change is Implemented")],
     "FRUM.010.03", "BR-UA-06", "SCR-UA-10", "Medium", 1),
    ("US-UA-018", "EP-UA-05", "Information Security", "a change that breaks a separation-of-duties rule to be "
     "refused", "toxic combinations never reach a user",
     [("an active rule forbids two permissions together", "a change would give both to one user",
       "the change is refused with the rule code")],
     "FRUM.010.04", "BR-UA-09", "SCR-UA-10", "High", 2),
    ("US-UA-019", "EP-UA-06", "User Access Administrator", "to see the state of the connection with UIDM-ISC",
     "I spot a broken feed the same day",
     [("I open Identity Sync", "the page loads", "I see the last call, the last success, errors of the last 24 "
       "hours and refused events")],
     "FRUM.014.01", "BR-UA-01", "SCR-UA-13", "High", 2),
    ("US-UA-020", "EP-UA-06", "User Access Administrator", "to retry or dismiss a refused event",
     "every event ends applied or explained",
     [("an event is Refused", "I click Retry after the data is fixed", "the event is Applied"),
      ("an event is Refused", "I dismiss it without a remark", "I see \"Enter the reason.\"")],
     "FRUM.014.02", "-", "SCR-UA-13", "High", 2),
    ("US-UA-021", "EP-UA-06", "User Access Administrator", "a daily reconciliation with UIDM-ISC",
     "differences are found and resolved",
     [("a user differs from UIDM-ISC", "the reconciliation runs", "a mismatch is listed"),
      ("a mismatch is open", "I resolve it with a remark", "it leaves the open list")],
     "FRUM.014.03", "BR-UA-01", "SCR-UA-13", "High", 3),
    ("US-UA-022", "EP-UA-07", "Information Security", "to authorise separation-of-duties rules proposed by "
     "others", "rules are agreed before they apply",
     [("a rule is Pending Authorization and I am not its maker", "I authorise it", "it is Active and checked "
       "daily"),
      ("I choose the same profile twice", "I save the rule", "I see \"Choose two different group profiles\"")],
     "FRUM.012.01", "BR-UA-04", "SCR-UA-14", "High", 3),
    ("US-UA-023", "EP-UA-07", "Information Security", "a daily list of users who breach an active rule",
     "I act on new breaches",
     [("a user holds both profiles of an active rule", "the daily check runs", "the user is listed and I am told")],
     "FRUM.012.02", "BR-UA-09", "SCR-UA-14", "High", 2),
    ("US-UA-024", "EP-UA-07", "User Access Administrator", "to propose a UAM setting that applies only after "
     "approval", "no one changes a control alone",
     [("I propose a new value", "I save", "the old value applies and the change waits"),
      ("I proposed the change", "I try to approve it", "it is refused: another user approves")],
     "FRUM.012.03", "BR-UA-11", "SCR-UA-18", "High", 2),
    ("US-UA-031", "EP-UA-07", "System Administrator", "to edit a group profile directly only while Information "
     "Security has opened the emergency switch", "an outage can be fixed without bypassing control",
     [("the emergency switch is off", "I try a direct edit", "it is refused: changes go through a request"),
      ("Information Security opened the switch", "I edit the profile", "the edit is applied and flagged in the "
       "audit log")],
     "FRUM.012.04", "BR-UA-11", "SCR-UA-18", "Low", 3),
    ("US-UA-025", "EP-UA-08", "Auditor", "the five user access reports", "I can evidence who has which access",
     None, "FRUM.007.01", "-", "SCR-UA-16", "High", 3),
    ("US-UA-026", "EP-UA-08", "Auditor", "the access matrix with an Excel export",
     "I compare profiles with the target matrix",
     [("I open Access Matrix", "I switch to By Action and export", "the file holds both views and the export is "
       "audited")],
     "FRUM.007.02", "-", "SCR-UA-15", "Medium", 2),
    ("US-UA-027", "EP-UA-08", "Auditor", "to take the certification extract in CSV or XLSX",
     "UIDM-ISC certification campaigns use current data",
     [("I click Take Extract and choose XLSX", "the file downloads", "the log shows rows and SHA-256 and "
       "Information Security is told")],
     "FRUM.007.03", "BR-UA-13", "SCR-UA-16", "Medium", 2),
    ("US-UA-028", "EP-UA-08", "Auditor", "every user access action in the audit log",
     "I can trace who did what, when and from where",
     [("a group-profile change is approved", "I open the Audit Log", "I see the action, before and after values, "
       "user, time and source address")],
     "FRUM-008.01, FRUM-008.02", "-", "SCR-UA-17", "High", 2),
    ("US-UA-029", "EP-UA-08", "Any user", "notices and e-mails of the access events that concern me",
     "I act without checking every screen",
     [("a change I made is returned", "the checker confirms", "I get a notice, and an e-mail when set")],
     "FRUM.013.01, FRUM.013.02", "-", "SCR-UA-10", "Medium", 2),
]
# The reports story keeps its criteria here (one per report).
REPORT_AC = [("I open Reports & Certification", f"I run {r}", "the report shows the rows as of the date chosen and "
              "exports to Excel and PDF") for r in ("User Access", "User Group Profile", "Group Profile Membership")]

README = {
    "intro": ("This pack holds every document of User Access Maintenance (BRD-11). Each document has one job. A "
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
    "ids": [("FRUM.nnn.nn", "Functional requirement (FRS)"), ("BR-UA-nn", "Business rule (FRS)"),
            ("SCR-UA-nn", "Screen (UX Design)"), ("US-UA-nnn", "User story (User Stories)"),
            ("TC-UA-…", "Test case (Test Cases and Traceability)"), ("CR-UA-nnn", "Change request (CR Register)"),
            ("NFR-nn", "Non-functional requirement (programme NFR)"), ("D-nnn / O-nnn", "Decision or observation "
             "(programme log)")],
    "colour": "The colour code of the FRS applies to every document: black in the BRD, green OOTB, orange change "
              "request, blue best practice, grey label for Integration or Data Migration scope.",
}


def screens_by_id() -> dict[str, dict]:
    return {s["id"]: s for s in P.SCREENS}


def check() -> list[str]:
    """Consistency of the module with the FRS content: rules, FRUM items and screens named here exist."""
    problems = []
    rules = {r[0] for r in C.BUSINESS_RULES}
    frum = {r[0] for f in C.FRUM for r in f["reqs"]}
    scr = set(screens_by_id())
    for s in STORIES:
        for ref in [x.strip() for x in s[6].split(",")]:
            if ref not in frum:
                problems.append(f"{s[0]}: unknown FRUM {ref}")
        for ref in [x.strip() for x in s[7].split(",") if x.strip() != "-"]:
            if ref not in rules:
                problems.append(f"{s[0]}: unknown rule {ref}")
        if s[8] not in scr:
            problems.append(f"{s[0]}: unknown screen {s[8]}")
    for r in RULE_ENFORCEMENT:
        if r[0] not in rules:
            problems.append(f"unknown rule {r[0]}")
    missing = rules - {r[0] for r in RULE_ENFORCEMENT}
    if missing:
        problems.append(f"rules without enforcement: {sorted(missing)}")
    in_force = {r[0] for f in C.FRUM if f["status"] != "Withdrawn" for r in f["reqs"]
                if not r[1].endswith("withdrawn.") and "are withdrawn" not in r[1]}
    covered = {x.strip() for s in STORIES for x in s[6].split(",")}
    if in_force - covered:
        problems.append(f"FRUM items without a user story: {sorted(in_force - covered)}")
    return problems
