"""Process, storyboards, screens and outputs of the User Access Maintenance document set v1.5 (BRD-11).

Screens and storyboard steps name their full-page screenshot (figures/brd11_v15_screens/<shot>.png), taken by
flows/brd11/shots_v15.sh on a test environment with fictitious people and records.
"""

from __future__ import annotations

# ================================================================================================ BPS
JOURNEY = [
    # no., lane (UIDM-ISC / Entra ID / BIBS / User), step
    (1, "UIDM-ISC", "The line manager raises an access request for the new joiner in UIDM-ISC."),
    (2, "UIDM-ISC", "The UIDM-ISC approvers approve the request."),
    (3, "Entra ID", "UIDM-ISC creates the identity in Entra ID: Windows ID, e-mail, second-factor registration."),
    (4, "UIDM-ISC", "UIDM-ISC sends the joiner to BIBS over SCIM 2.0 with the UIDM request number."),
    (5, "BIBS", "BIBS checks the event and creates the user, active and without a group profile."),
    (6, "UIDM-ISC", "UIDM-ISC adds the user to the approved group profile (SCIM group membership)."),
    (7, "BIBS", "BIBS checks that the group profile exists and is active, applies it and records the history."),
    (8, "User", "The user opens BIBS and is sent to Microsoft Entra ID."),
    (9, "Entra ID", "Entra ID checks the password, the second factor and the device, and returns the identity."),
    (10, "BIBS", "BIBS links the identity to the active user and opens a session with the menus of the profiles."),
    (11, "UIDM-ISC", "Mover: UIDM-ISC sends the new details and group profiles; BIBS applies them."),
    (12, "UIDM-ISC", "Leaver: UIDM-ISC deactivates the user; BIBS deactivates the user and ends the sessions."),
    (13, "BIBS", "An event BIBS cannot apply is refused and alerted; UIDM-ISC corrects it, the administrator retries "
     "or dismisses it."),
    (14, "BIBS", "Every day BIBS reconciles its users with UIDM-ISC; mismatches are resolved after UIDM-ISC corrects "
     "them."),
    (15, "BIBS", "The Auditor and Information Security take reports, the certification extract and the audit log; "
     "certification decisions are actioned in UIDM-ISC."),
]

PROCESSES = [
    {"id": "P-01", "name": "Joiner to first sign-in", "steps": "1 to 10",
     "trigger": "A new employee needs BIBS access.", "actors": "Line manager, UIDM-ISC approvers, user, User Access "
     "Administrator", "outcome": "Active user with the approved group profiles, signed in through Entra ID.",
     "controls": "Request approved in UIDM-ISC; second factor in Entra ID; history with the UIDM request number."},
    {"id": "P-02", "name": "Mover", "steps": "11", "trigger": "A transfer or a change of job.",
     "actors": "UIDM-ISC, BIBS", "outcome": "New details and group profiles in force; old profiles removed.",
     "controls": "Attribute-level history; SoD rules checked daily."},
    {"id": "P-03", "name": "Leaver", "steps": "12", "trigger": "Resignation, end of contract, long leave.",
     "actors": "UIDM-ISC, BIBS", "outcome": "User inactive; every session ended at once.",
     "controls": "Reconciliation finds a leaver still active in BIBS."},
    {"id": "P-04", "name": "Group-profile change under maker-checker", "steps": "-",
     "trigger": "A business need to add or change a group profile.",
     "actors": "Group Profile Maker, Group Profile Checker, second checker, System Administrator",
     "outcome": "Group profile created, changed, deactivated or reactivated.",
     "controls": "Maker never decides; second approval for High or Admin and out-of-hours; SoD rules; notices."},
    {"id": "P-05", "name": "Refused event and reconciliation", "steps": "13, 14",
     "trigger": "An event BIBS cannot apply, or a difference found by the daily reconciliation.",
     "actors": "User Access Administrator, UIDM-ISC team", "outcome": "BIBS and UIDM-ISC agree again.",
     "controls": "Nothing is corrected in BIBS; remark on every dismissal and resolution."},
    {"id": "P-06", "name": "Evidence for the auditor and certification", "steps": "15",
     "trigger": "Audit request or periodic certification.", "actors": "Auditor, Information Security",
     "outcome": "Reports, extract with its SHA-256, audit log.", "controls": "Every extract is logged and notified."},
]

STORYBOARDS = [
    {"id": "SB-1", "title": "Joiner to first sign-in",
     "summary": "Marketing hires Isabel Navarro as account officer. UIDM-ISC sends the joiner and the group profile "
     "Marketing Account Officer. She signs in through Entra ID and sees the menus of her profile.",
     "steps": [
         ("UIDM-ISC", "Identity Sync › Events", "Sends the joiner and the group-profile event of UIDM-2026-005196.",
          "Two events for BDO\\inavarro, both Applied.", "wt1-01-events"),
         ("User Access Administrator", "Users", "Searches a01300019.", "Isabel Navarro is Active with Marketing "
          "Account Officer.", "wt1-02-users"),
         ("User Access Administrator", "User › Profile", "Opens the user.", "Account data and the data sent by "
          "UIDM-ISC, with the request number.", "wt1-03-user-profile"),
         ("User Access Administrator", "User › Group Profiles", "Opens Group Profiles.", "Marketing Account Officer, "
          "source UIDM-ISC, with the date and request number.", "wt1-04-user-groups"),
         ("Isabel Navarro", "Sign in", "Opens BIBS and clicks Sign in with Microsoft Entra ID.", "Entra ID asks for "
          "her password and second factor.", "ux-01-signin"),
         ("Isabel Navarro", "Home", "Is signed in.", "The New Business Dashboard and only the menus of a Marketing "
          "Account Officer.", "wt1-06-first-home"),
         ("User Access Administrator", "User › Sessions", "Checks the first session.", "One open session with the "
          "sign-in time.", "ux-07-user-sessions"),
     ]},
    {"id": "SB-2", "title": "Mover and leaver",
     "summary": "Isabel Navarro moves to Corporate Marketing as team leader. Carlo Mendoza leaves BDOI.",
     "steps": [
         ("UIDM-ISC", "Identity Sync › Events", "Sends the mover and group-profile events of UIDM-2026-005240.",
          "Mover and Group profiles events Applied.", "wt2-01-mover-event"),
         ("User Access Administrator", "User › Group Profiles", "Opens Isabel's group profiles.", "Marketing Team "
          "Leader active; Marketing Account Officer removed.", "ux-04-user-groups"),
         ("User Access Administrator", "User › Access History", "Opens the history.", "Each attribute with its from "
          "and to value and the UIDM request number.", "ux-08-user-history"),
         ("UIDM-ISC", "User › Profile", "Sends the leaver of UIDM-2026-005251 for Carlo Mendoza.", "Carlo is "
          "Inactive; his sessions are ended.", "wt2-04-leaver-user"),
         ("Auditor", "User › Access History", "Reads Carlo's history.", "Deactivation with the UIDM request number.",
          "wt2-05-leaver-history"),
     ]},
    {"id": "SB-3", "title": "Group-profile change under maker-checker",
     "summary": "Rhea Access Requestor (maker) adds Client approve to Marketing Account Officer. Ulysses Access "
     "Approver (checker) approves. The System Administrator implements it.",
     "steps": [
         ("Group Profile Maker", "Group Profile › Definition", "Opens Marketing Account Officer.", "Definition, "
          "members and the Propose Change button.", "wt3-01-profile"),
         ("Group Profile Maker", "Propose Change", "Adds the permission, enters the reason, clicks Submit for "
          "Approval.", "The permission picker by area and the reason field.", "wt3-02-propose"),
         ("Group Profile Maker", "Pending Change", "Reads the submitted change.", "AR-2026-000001 Pending, maker "
          "Rhea, permission added.", "wt3-03-pending-maker"),
         ("Group Profile Checker", "Pending Change", "Opens the change from the notice.", "Approve, Return for "
          "Correction and Reject.", "wt3-04-checker"),
         ("Group Profile Checker", "Approve", "Enters a remark and confirms.", "The change goes to implementation.",
          "wt3-05-approve-dialog"),
         ("System Administrator", "Pending Change", "Implements the approved change.", "For Implementation with the "
          "Implement button; then Implemented.", "wt3-06-implement"),
         ("Auditor", "Group Profile › History", "Reads the history.", "Change number, maker, checker, date and "
          "status.", "wt3-07-history"),
     ]},
    {"id": "SB-4", "title": "Refused event and reconciliation",
     "summary": "UIDM-ISC sends a group profile that does not exist in BIBS. Later the daily reconciliation finds a "
     "user active in BIBS but not in the directory.",
     "steps": [
         ("User Access Administrator", "Identity Sync › Refused", "Opens the alert.", "Event refused: no group "
          "profile MKT_SUPERVISOR.", "wt4-01-refused"),
         ("User Access Administrator", "Dismiss", "Enters the remark after UIDM-ISC corrected the request.",
          "The event is closed with the remark.", "wt4-02-dismiss"),
         ("User Access Administrator", "Identity Sync › Reconciliation", "Clicks Synchronise Now.", "Mismatch: "
          "status differs for a013000196.", "ux-20-idsync-recon"),
         ("User Access Administrator", "Identity Sync › Status", "Checks the connection.", "Last call, last success, "
          "errors, refused events and open mismatches.", "ux-17-idsync-status"),
     ]},
    {"id": "SB-5", "title": "Evidence for the auditor and certification",
     "summary": "Audrey Access Auditor prepares the quarterly access certification.",
     "steps": [
         ("Auditor", "Reports & Certification", "Opens the reports.", "Five reports and the certification extract.",
          "wt5-01-reports"),
         ("Auditor", "Take Extract", "Chooses XLSX and confirms.", "The extract is logged with rows and SHA-256; "
          "Information Security is told.", "wt5-02-extract"),
         ("Auditor", "Audit Log", "Reads the user access events.", "Every event with from and to values, who and "
          "when.", "wt5-03-audit"),
         ("Auditor", "Access Matrix", "Checks the group profiles against the permissions.", "The matrix, exportable "
          "to Excel.", "ux-24-matrix-permission"),
     ]},
]

# ================================================================================================ UXD
# Every screen: title, path in the menu, personas, purpose, fields [(name, description)], validations, actions,
# states, empty and error states, shots [(shot, caption)]
SCREENS = [
    {"id": "SCR-UA-01", "name": "Sign in", "menu": "Login page", "personas": "All users",
     "purpose": "Starts every session through Microsoft Entra ID.",
     "fields": [("Sign in with Microsoft Entra ID", "Opens Entra ID; returns signed in."),
                ("Test account sign-in", "DEV, SIT and UAT only: User ID and password of a test account."),
                ("Environment badge", "Name of a non-production environment.")],
     "validations": ["An identity not linked to an active user is refused.", "Production offers no password form."],
     "actions": ["Sign in with Microsoft Entra ID"], "states": "Default; refused (message under the button).",
     "empty": "-", "shots": [("ux-01-signin", "Sign in with the Entra ID button")]},
    {"id": "SCR-UA-02", "name": "Users", "menu": "Setup & Administration › User Access › Users",
     "personas": "User Access Administrator, Auditor",
     "purpose": "The users received from UIDM-ISC. Read-only for the user record.",
     "fields": [("Search", "User ID, name or Windows ID."), ("Status", "All, Active, Inactive, Locked."),
                ("Group profile", "Filter by profile."), ("Business unit", "Filter by unit."),
                ("Columns", "User ID, Windows ID, Name, Business Unit, Status, Group Profiles, Last Login, Locked.")],
     "validations": ["-"], "actions": ["Search", "Export (CSV)", "Open a user"],
     "states": "List; filtered.", "empty": "No user matches the filters.",
     "shots": [("ux-02-users", "Users of the User Access Administrator")]},
    {"id": "SCR-UA-03", "name": "User › Profile", "menu": "Users › a user", "personas": "User Access Administrator, "
     "Auditor", "purpose": "The account and the data sent by UIDM-ISC.",
     "fields": [("Account", "User ID, Windows ID, Name, E-mail, Business Unit, User Level, Status, Last Login."),
                ("From UIDM-ISC (SCIM)", "Display name, directory status, department, unit, location, team leader, "
                 "UIDM request no., last synchronised.")],
     "validations": ["-"], "actions": ["End Sessions", "Unlock (locked test account)"],
     "states": "Active, Inactive, Locked.", "empty": "Not connected: the directory data is not shown.",
     "shots": [("ux-03-user-profile", "User profile with the data from UIDM-ISC"),
               ("ux-10-user-auditor", "The same user seen by the Auditor: no action buttons")]},
    {"id": "SCR-UA-04", "name": "User › Group Profiles", "menu": "Users › a user › Group Profiles",
     "personas": "User Access Administrator, Auditor", "purpose": "The group profiles held and where they came from.",
     "fields": [("Columns", "Code, Group Profile, Status, Source, Since, Request No.")],
     "validations": ["-"], "actions": ["-"], "states": "List.", "empty": "No group profile.",
     "shots": [("ux-04-user-groups", "Group profiles of a user with source and request number")]},
    {"id": "SCR-UA-05", "name": "User › Effective Permissions", "menu": "Users › a user › Effective Permissions",
     "personas": "User Access Administrator, Auditor", "purpose": "Every permission the user holds and through which "
     "profile.", "fields": [("Columns", "Permission, area, granted by (group profiles).")],
     "validations": ["-"], "actions": ["-"], "states": "List.", "empty": "No permission.",
     "shots": [("ux-05-user-permissions", "Effective permissions through the active group profiles")]},
    {"id": "SCR-UA-06", "name": "User › Data Access", "menu": "Users › a user › Data Access",
     "personas": "User Access Administrator (change), Auditor (read)",
     "purpose": "The companies and branches the user may act for.",
     "fields": [("Companies", "Ticked companies."), ("Branches", "All branches, or the ticked ones.")],
     "validations": ["At least one company."], "actions": ["Save"], "states": "Read; editing.",
     "empty": "No data access: the user sees no records.",
     "shots": [("ux-06-user-data", "Data access by company and branch")]},
    {"id": "SCR-UA-07", "name": "User › Sessions", "menu": "Users › a user › Sessions",
     "personas": "User Access Administrator, Auditor", "purpose": "Open and recent sessions.",
     "fields": [("Columns", "Started, last activity, address, device, status, ended by.")],
     "validations": ["-"], "actions": ["End Sessions (header)"], "states": "List.", "empty": "No session.",
     "shots": [("ux-07-user-sessions", "Sessions of a user"),
               ("ux-09-end-sessions", "End Sessions asks for confirmation")]},
    {"id": "SCR-UA-08", "name": "User › Access History", "menu": "Users › a user › Access History",
     "personas": "User Access Administrator, Auditor", "purpose": "Every change of the user, attribute by attribute.",
     "fields": [("Columns", "Date / Time, Activity, Attribute, From, To, Request No., Done By, Approved By.")],
     "validations": ["-"], "actions": ["-"], "states": "List.", "empty": "No change recorded.",
     "shots": [("ux-08-user-history", "Access history with the UIDM request numbers")]},
    {"id": "SCR-UA-09", "name": "Group Profiles", "menu": "Setup & Administration › User Access › Group Profiles",
     "personas": "All User Access personas", "purpose": "Every group profile and the change waiting for approval.",
     "fields": [("Search", "Code or name."), ("Columns", "Code, Name, Privilege Level, Members, Status, Pending "
                "Change."), ("Proposed New Group Profiles", "New profiles not yet approved.")],
     "validations": ["-"], "actions": ["New Group Profile (maker)", "Open a profile"], "states": "List.",
     "empty": "No group profile matches.", "shots": [("ux-11-group-profiles", "Group Profiles of the maker")]},
    {"id": "SCR-UA-10", "name": "Group Profile › Definition, Permissions, Members",
     "menu": "Group Profiles › a profile", "personas": "All User Access personas",
     "purpose": "The definition, the permissions by area and the members (from UIDM-ISC, read-only).",
     "fields": [("Definition", "Code, Name, Description, Privilege Level, Status, Members, Permissions."),
                ("Permissions", "By area, with what each permission allows."),
                ("Members", "User ID, Windows ID, Name, Status.")],
     "validations": ["-"], "actions": ["Propose Change", "Propose Deactivation / Reactivation (maker)"],
     "states": "Active; Inactive; change pending (banner).", "empty": "No member.",
     "shots": [("ux-12-gp-definition", "Definition of a group profile"),
               ("ux-13-gp-permissions", "Permissions of a group profile by area"),
               ("ux-14-gp-members", "Members of a group profile")]},
    {"id": "SCR-UA-11", "name": "Propose a change", "menu": "Group Profiles › New Group Profile / Propose Change",
     "personas": "Group Profile Maker", "purpose": "Proposes a new profile or a change, deactivation or reactivation.",
     "fields": [("Code", "New profile: capital letters, digits and _ (up to 40). Mandatory."),
                ("Name / New Name", "Mandatory for a new profile; blank keeps the current name."),
                ("Description", "Optional; blank keeps the current one."),
                ("Privilege Level", "Low, Standard, High, Admin; High and Admin need a second approval."),
                ("Permissions", "Picker by area with search, Select all and Clear."),
                ("Reason", "Mandatory on submission, up to 500 characters.")],
     "validations": ["Enter the code: capital letters, digits and _ (up to 40).", "Enter the name of the group "
                     "profile.", "Select at least one permission.", "Change the permissions, the name, the "
                     "description or the level.", "Enter the reason for the change."],
     "actions": ["Save Draft", "Submit for Approval", "Cancel"], "states": "New; correcting a returned change.",
     "empty": "-", "shots": [("ux-15-gp-new", "New group profile with the messages of missing fields")]},
    {"id": "SCR-UA-12", "name": "Group Profile › Pending Change and History", "menu": "Group Profiles › a profile",
     "personas": "Maker, Checker, System Administrator, Auditor", "purpose": "The open change and its decisions.",
     "fields": [("Pending Change", "Status, Maker, Proposed on, Change, Permissions added and removed, Reason, "
                 "Checker remarks, Second approval."), ("History", "Change No., Change, Details, Maker, Checker, "
                 "Date, Status.")],
     "validations": ["Return and Reject need a remark.", "The maker cannot decide."],
     "actions": ["Approve", "Give Second Approval", "Return for Correction", "Reject", "Cancel Change (maker)",
                 "Implement (System Administrator)"],
     "states": "Pending, Pending Second, Returned, For Implementation.",
     "empty": "No change of this group profile is waiting.",
     "shots": [("ux-16-gp-pending-empty", "Pending Change with nothing waiting")]},
    {"id": "SCR-UA-13", "name": "Identity Sync", "menu": "Setup & Administration › User Access › Identity Sync",
     "personas": "User Access Administrator", "purpose": "The link with UIDM-ISC: status, events, refused events, "
     "reconciliation.",
     "fields": [("Status", "Directory, last call, last successful event, errors in 24 hours, refused events to "
                 "review, mismatches to resolve, last synchronisation."),
                ("Events", "Event No., Received, Source, Event, Windows ID, User ID, UIDM Request No., Outcome, "
                 "Details, Processed By, Attempts."),
                ("Refused", "Event No., Received, Event, Windows ID, User ID, UIDM Request No., Reason, Attempts."),
                ("Reconciliation", "Run Date, User ID, Windows ID, Mismatch, Details, Resolved.")],
     "validations": ["Dismiss and Resolve need a remark."],
     "actions": ["Synchronise Now", "Retry", "Dismiss", "Resolve", "Run Now", "Show resolved"],
     "states": "Connected; errors.", "empty": "None yet / no refused event / no mismatch.",
     "shots": [("ux-17-idsync-status", "Identity Sync status"), ("ux-18-idsync-events", "Events received from "
               "UIDM-ISC"), ("ux-19-idsync-refused", "Refused events"), ("ux-20-idsync-recon",
               "Reconciliation mismatches")]},
    {"id": "SCR-UA-14", "name": "Separation of Duties", "menu": "Setup & Administration › User Access › "
     "Separation of Duties", "personas": "Information Security; User Access Administrator and Auditor read",
     "purpose": "Rules and the users who breach them.",
     "fields": [("Rules", "Rule, Type, Group Profile / Permission, May Not Be Held With, Reason, Status, Maker, "
                 "Authorised."), ("Violations", "Rule, Kept Apart, User ID, Name, Group Profiles, First Found.")],
     "validations": ["Choose two different group profiles.", "Enter why the two profiles are not held together."],
     "actions": ["New Rule (maintainer)", "Authorise", "Reject", "Deactivate", "Export"],
     "states": "Pending Authorization, Active, Inactive.", "empty": "No violation.",
     "shots": [("ux-21-sod-rules", "Rules, one pending authorisation"), ("ux-23-sod-violations", "Violations")]},
    {"id": "SCR-UA-15", "name": "Access Matrix", "menu": "Setup & Administration › User Access › Access Matrix",
     "personas": "All User Access personas", "purpose": "Group profiles against permissions.",
     "fields": [("By Permission", "Profiles as columns, permissions as rows."), ("By Action", "Area and action "
                "class.")], "validations": ["-"], "actions": ["Export to Excel"], "states": "Two tabs.",
     "empty": "-", "shots": [("ux-24-matrix-permission", "Access matrix by permission"),
                             ("ux-25-matrix-action", "Access matrix by action")]},
    {"id": "SCR-UA-16", "name": "Reports & Certification", "menu": "Setup & Administration › User Access › Reports & "
     "Certification", "personas": "All User Access personas", "purpose": "Reports and the certification extract.",
     "fields": [("Reports", "Five reports with their parameters."), ("Extracts taken", "Taken, Taken By, File, Rows, "
                "SHA-256.")], "validations": ["Choose CSV or XLSX."],
     "actions": ["Open a report", "Take Extract (CSV / XLSX)"], "states": "-", "empty": "No extract has been taken.",
     "shots": [("ux-26-reports", "Reports and certification extract log")]},
    {"id": "SCR-UA-17", "name": "Audit Log", "menu": "Setup & Administration › User Access › Audit Log",
     "personas": "User Access Administrator, Information Security, Auditor",
     "purpose": "The audit trail preset to the user access records.",
     "fields": [("Filters", "Date from / to, record type, action, user."), ("Columns", "Date / time, user, action, "
                "record, from and to values, address.")], "validations": ["-"], "actions": ["Search", "Export"],
     "states": "-", "empty": "No event for the filters.", "shots": [("ux-27-audit", "Audit log of user access")]},
    {"id": "SCR-UA-18", "name": "UAM Settings", "menu": "Setup & Administration › User Access › UAM Settings",
     "personas": "User Access Administrator (propose), Information Security (approve)",
     "purpose": "Settings of sessions, dormancy, test accounts, synchronisation and group profiles.",
     "fields": [("Columns", "Area, Setting, Value, Status, Last Changed, Action.")],
     "validations": ["Whole number within the range; true or false; text."],
     "actions": ["Change", "Approve", "Reject", "Withdraw"], "states": "In Force; Pending Approval.",
     "empty": "-", "shots": [("ux-28-settings", "UAM Settings"), ("ux-29-settings-change", "Change of a setting"),
                              ("ux-30-settings-approve", "Setting waiting for Information Security")]},
]

# ================================================================================================ RNO
REPORTS = [
    ("RPT-UA-01", "User Access Report", "BRD 3.003.1", "As of date, business unit, status, group profile",
     "User ID, name, Windows ID, business unit, user level, status, group profiles, date created, created by "
     "(UIDM-ISC with request number), date modified, modified by, last action"),
    ("RPT-UA-02", "User Group Profile Report", "BRD 3.003.2", "Group profile, module, active",
     "Group profile name, module, task, With Access / No Access, date created, created by, date modified, modified by"),
    ("RPT-UA-03", "Group Profile Membership", "BRD 3.003.3", "Group profile, as of date",
     "Group profile, user ID, user name, added on, added by (UIDM request number)"),
    ("RPT-UA-04", "User Access Audit Log", "BRD 4.003.1", "Date from / to, user, activity, log-ins and log-outs",
     "Date and time, activity, record, from value, to value, done by, approved by, request or change number, "
     "source address"),
    ("RPT-UA-05", "Group Profile Changes", "BRD 1.008 (group profiles)", "Date from / to, status, change",
     "Change number, group profile, change, maker, checker, second approver, status, age in days"),
]
EXTRACT_FIELDS = ["User ID", "Windows ID", "Name", "E-mail", "Business Unit", "User Status", "Last Login",
                  "Group Profile Code", "Group Profile Name", "Group Profile Active", "Privilege Level",
                  "Permission", "Permission Area", "Source", "UIDM Request No."]
EXPORTS = [
    ("Users list", "CSV", "Users screen, Export", "Columns of the list with the filters applied"),
    ("Reports", "PDF (landscape), Excel, CSV", "Each report", "File name <Report name>_MMDDYYYY"),
    ("Access matrix", "Excel", "Access Matrix, Export", "Profiles against permissions"),
    ("Certification extract", "CSV or XLSX", "Reports & Certification, Take Extract", "Logged with SHA-256"),
    ("SoD violations", "CSV", "Separation of Duties › Violations, Export", "Users who breach a rule"),
    ("Audit log", "CSV", "Audit Log, Export", "Events with the filters applied"),
]
NOTICES = [
    # id, event, recipients, title, text, channel
    ("NT-01", "Group-profile change submitted", "Group Profile Checkers", "Access request <no.> to approve",
     "<Change description>.", "In-app; e-mail by preference"),
    ("NT-02", "Second approval needed", "Second checkers", "Access request <no.> for second approval",
     "<Change description>.", "In-app; e-mail by preference"),
    ("NT-03", "Change approved, waiting for implementation", "System Administrators", "Access request <no.> to "
     "implement", "<Change description>.", "In-app; e-mail by preference"),
    ("NT-04", "Change decided", "Maker", "Access request <no.> <approved / rejected / returned / implemented>",
     "<Change description> (<checker remark>).", "In-app; e-mail by preference"),
    ("NT-05", "Group profile deactivated or reactivated, or permissions changed", "Members of the profile",
     "Your access changed", "Your access changed: <change description> (request <no.>).", "In-app and e-mail"),
    ("NT-06", "Dormancy notice", "The dormant user", "Your account will be deactivated", "You have not signed in to "
     "BIBS since <date>. Sign in before <date> to keep your account.", "In-app and e-mail"),
    ("NT-07", "Dormant users deactivated", "User Access Administrators", "Dormant users deactivated",
     "<n> users without a sign-in for <days> days were deactivated. Disable them in UIDM-ISC too.", "In-app and "
     "e-mail"),
    ("NT-08", "Identity event refused", "User Access Administrators", "Identity event <no.> needs review",
     "UIDM-ISC event <type> of Windows ID <id> was refused: <reason>.", "In-app"),
    ("NT-09", "Reconciliation mismatches", "User Access Administrators", "Identity reconciliation: <n> mismatch(es)",
     "<n> users differ from the UIDM-ISC directory on <date>. Review them on the Identity Sync screen and ask "
     "UIDM-ISC to correct them.", "In-app"),
    ("NT-10", "New separation-of-duties breaches", "Information Security", "Separation of duties: <n> new breach(es)",
     "Users who hold group profiles kept apart by an active rule; see Separation of Duties › Violations.", "In-app"),
    ("NT-11", "SoD rule proposed", "Information Security", "Separation-of-duties rule <no.> to authorise",
     "<Profiles kept apart>: <reason>.", "In-app"),
    ("NT-12", "UAM setting proposed", "Information Security", "Security setting to approve: <setting>",
     "<Maker> requests a change from '<old>' to '<new>'.", "In-app"),
    ("NT-13", "Certification extract taken", "Information Security", "Certification extract taken",
     "Certification extract <format>, <rows> row(s), SHA-256 <hash>, taken by <user>.", "In-app"),
    ("NT-14", "Privileged group-profile change approved", "Information Security", "Privileged access change",
     "<Change description> of a High or Admin group profile.", "In-app"),
]
JOBS = [
    ("UAM_DORMANT_USERS", "Daily, 00:15 Philippine time", "Notices users near the dormancy limit and deactivates "
     "those past it; tells the User Access Administrators.", "NT-06, NT-07"),
    ("UAM_IDENTITY_RECONCILIATION", "Daily (every setting: Reconciliation every)", "Compares users and group "
     "profiles with UIDM-ISC; lists mismatches.", "NT-09"),
    ("UAM_SOD_VIOLATIONS", "Daily", "Records new breaches of active rules.", "NT-10"),
    ("USER_SESSION_SWEEP", "Every few minutes", "Ends idle, expired, locked and disabled sessions.", "-"),
    ("PASSWORD_EXPIRY_NOTICE", "Daily (DEV, SIT and UAT)", "Warns local test accounts before their password expires.",
     "-"),
]
REMOVED_NOTICES = [
    ("NT-05, NT-06 (v1.4)", "Request returned; request cancelled", "User access requests are in UIDM-ISC."),
    ("NT-08, NT-09 (v1.4)", "Reset your password; password expires soon", "Passwords of bank users are in Entra ID."),
    ("NT-12 (v1.4)", "Scheduled access change failed", "No effective dates."),
    ("NT-19, NT-20 (v1.4)", "Second-factor reset to approve; authenticator app reset", "Second factor is in Entra ID."),
    ("NT-22 (v1.4)", "Break-glass sign-in", "No break-glass account."),
    ("NT-13 (v1.4)", "Role edited outside a request", "Kept only with CR-UA-016 (emergency edit)."),
]
