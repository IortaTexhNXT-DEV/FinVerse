"""Content of the User Access Maintenance document set v1.5 (BRD-11) in BDOI's template: the FRS, the process and
storyboards (BPS), the UX design (UXD) and the reports, notifications and outputs (RNO). Read by build_brd11_v15.py
and build_brd11_v15_workbooks.py. Business wording only.

Colour of a point: "k" black (in the BRD, BRD-00 or a programme NFR), "g" green (not in the BRD, offered by the
iNXT BrokerVerse OOTB platform), "o" orange (change request), "b" blue (best practice, separate table). A label
"int" or "dm" adds the grey [Integration scope] or [Data Migration scope] label.
"""

from __future__ import annotations

VERSION = "1.5"
WB_VERSION = "1.2"
DATE = "16 Oct 2026"
DATE_US = "10/16/2026"
AUTHOR = "iorta TechNXT Project Team (drafting support); for review by John Barbachano"
OWNER = "John Barbachano"
ICD = "BIBS_ICD-BDOI_BRD-11_User_Access_Interface_Specification_v1.0"

DOCS = {
    "FRS": {"file": "BIBS_FRS-BDOI_BRD-11_User_Access_Maintenance_v1.5.docx",
            "title": "Functional Requirements Specification: User Access Maintenance",
            "cover": "Functional Requirements Specification: User Access Maintenance",
            "header_left": "Functional Requirements Specification", "doc_no": "PRJ0012475"},
    "BPS": {"file": "BIBS_BPS-BDOI_BRD-11_User_Access_Process_and_Storyboards_v1.5.docx",
            "title": "User Access Process and Storyboards",
            "cover": "User Access Maintenance: To-Be Process and Storyboards",
            "header_left": "To-Be Process and Storyboards", "doc_no": "PRJ0012475-BPS"},
    "UXD": {"file": "BIBS_UXD-BDOI_BRD-11_User_Access_UX_Design_v1.5.docx",
            "title": "User Access UX Design",
            "cover": "User Access Maintenance: Screens and UX Design",
            "header_left": "Screens and UX Design", "doc_no": "PRJ0012475-UXD"},
    "RNO": {"file": "BIBS_RNO-BDOI_BRD-11_User_Access_Reports_Notifications_Outputs_v1.5.docx",
            "title": "User Access Reports, Notifications and Outputs",
            "cover": "User Access Maintenance: Reports, Notifications and Outputs",
            "header_left": "Reports, Notifications and Outputs", "doc_no": "PRJ0012475-RNO"},
}
HEADER_RIGHT = "BDO Insure Brokering System (BIBS) - User Access Maintenance"

# Revision log of the FRS (BDOI's v1.1 and the versions since). The companion documents start at 1.5 with the split.
REVISIONS = [
    ("26 Aug 2026", "1.1", "BDOI's FRS for User Access Maintenance (FRUM.001 to FRUM-008).", "John Barbachano"),
    ("09 Oct 2026", "1.2", "Draft for BDOI review: full coverage of the BRD, new items FRUM.009 to FRUM.013.", AUTHOR),
    ("10 Oct 2026", "1.3", "Business Unit review edition: process flows, screens, notices, reports, open questions.", AUTHOR),
    ("11 Oct 2026", "1.4", "Product owner review: three sources only, colour code, change requests with effort, BDOI's "
     "review comments answered (user access requests in UIDM-ISC).", AUTHOR),
    (DATE, "1.5", "Rewritten for the agreed access model: UIDM-ISC maintains the users and their group-profile "
     "membership over SCIM 2.0, Microsoft Entra ID signs every user in, no break-glass account, local passwords only "
     "for test accounts outside production. FRUM.009 and FRUM.011 withdrawn (in UIDM-ISC); FRUM.002, FRUM.003, "
     "FRUM-004, FRUM.010 and FRUM.013 changed; FRUM.014 added (identity synchronisation and reconciliation). The "
     "document is split: this FRS, the Process and Storyboards, the UX Design, and the Reports, Notifications and "
     "Outputs. Change requests CR-UA-003, CR-UA-011 and CR-UA-014 withdrawn; CR-UA-007 rescoped.", AUTHOR),
]
REVISION_COMPANION = [
    (DATE, "1.5", "First issue as a separate document of the User Access Maintenance set (taken out of the FRS v1.4 "
     "annexes and rewritten for the agreed access model). Numbered 1.5 to match the FRS.", AUTHOR),
]

# ================================================================================================ shared facts
PERSONAS = [
    # name, what the person does, where, test account of the walkthroughs
    ("User Access Administrator", "Looks after the users received from UIDM-ISC: data access, unlock, end of sessions; "
     "watches Identity Sync and resolves refused events and reconciliation mismatches; proposes UAM settings.",
     "Users, Group Profiles (read), Identity Sync, Separation of Duties (read), Access Matrix, Reports & Certification, "
     "Audit Log, UAM Settings", "Ursula Access Administrator"),
    ("Group Profile Maker", "Proposes new group profiles and changes, deactivations and reactivations of group "
     "profiles; corrects a returned change; withdraws an own change.",
     "Group Profiles, Access Matrix, Reports & Certification", "Rhea Access Requestor"),
    ("Group Profile Checker", "Approves, returns or rejects a group-profile change made by someone else. A second "
     "checker gives the second approval of a High or Admin profile or of a change outside the working hours.",
     "Group Profiles, Access Matrix, Reports & Certification", "Ulysses Access Approver"),
    ("Information Security", "Authorises separation-of-duties rules, follows the violations, approves UAM settings, "
     "receives the alerts and oversees certification.",
     "Group Profiles (read), Separation of Duties, Access Matrix, Reports & Certification, Audit Log, UAM Settings",
     "Ingrid Security Officer"),
    ("Auditor", "Reads every User Access screen, runs the reports and takes the certification extract. Changes "
     "nothing.", "Users, Group Profiles, Separation of Duties, Access Matrix, Reports & Certification, Audit Log",
     "Audrey Access Auditor"),
]
SUPPORT_ROLE = ("System Administrator", "Implements an approved group-profile change when the setting asks for an "
                "implementation step. Not a User Access persona; listed for the hand-over only.")

MENU = [
    # screen, purpose, personas (A administrator, M maker, C checker, I infosec, U auditor)
    ("Users", "Users received from UIDM-ISC; detail, data access, unlock, end sessions", "A U"),
    ("Group Profiles", "Definitions, permissions, members, pending change, history", "A M C I U"),
    ("Identity Sync", "Connection status, events, refused events, reconciliation", "A"),
    ("Separation of Duties", "Rules and violations", "A I U"),
    ("Access Matrix", "Group profiles against permissions; by action", "A M C I U"),
    ("Reports & Certification", "Five reports and the certification extract", "A M C I U"),
    ("Audit Log", "Audit trail of the user access records", "A I U"),
    ("UAM Settings", "Session, dormancy, test-account and synchronisation settings", "A I"),
]

LEGEND_NOTE = (
    "Each requirement and rule carries a colour. Black: asked by the BRD, BRD-00 or a programme-wide non-functional "
    "requirement. The points not in the BRD form one group with three colours: green when the iNXT BrokerVerse OOTB "
    "platform offers them at no cost, orange when they need a change request, and blue for best-practice "
    "recommendations, kept in a separate table. A grey label marks a point delivered by the Integration workstream "
    "(UIDM-ISC, Microsoft Entra ID) or by Data Migration (BRD-13).")

# ================================================================================================ FRS
FRS_INTRO = {
    "purpose": (
        "This document states the functional requirements of User Access Maintenance in BIBS, the BDO Insure "
        "Brokering System. It replaces version 1.4 and follows the access model agreed with BDOI IT: UIDM-ISC "
        "manages identities and group-profile membership, Microsoft Entra ID signs users in, and BIBS keeps the "
        "group-profile definitions and the controls around them."),
    "audience": (
        "The BIBS project team, the BDOI Business Unit owners of User Access, BDOI Information Security, BDOI IT "
        "(UIDM-ISC and Microsoft Entra ID teams) and Internal Audit."),
    "background": [
        "Before this version, users were enrolled, changed and removed through access requests raised and approved "
        "inside BIBS. BDOI's review of version 1.4 placed those requests in UIDM-ISC, BDOI's identity governance "
        "platform.",
        "UIDM-ISC now handles joiners, movers and leavers and decides who holds which group profile. It sends each "
        "change to BIBS over SCIM 2.0 with its UIDM request number.",
        "Microsoft Entra ID signs every user in with OpenID Connect. The second factor is asked by Entra ID. BIBS "
        "holds no password of a bank user.",
        "BIBS keeps the definition of each group profile (name, level, permissions, status). Every change of a "
        "definition is proposed by a maker and approved by another user.",
        "There is no break-glass account. When Entra ID is not available, nobody signs in to BIBS until it is "
        "restored; this is BDOI IT's decision of 15 Oct 2026.",
    ],
    "in_scope": [
        "Sign-in through Microsoft Entra ID, session warnings and time-out.",
        "Users, their status and group profiles as received from UIDM-ISC; refusal of local user maintenance.",
        "Data access by company and branch, unlock of a test account, end of sessions.",
        "Group-profile definitions under maker-checker, with a second approval for privileged or out-of-hours changes.",
        "Identity synchronisation monitoring, refused events and daily reconciliation with UIDM-ISC.",
        "Separation-of-duties rules and the daily violation check.",
        "Reports, access matrix, certification extract, audit log, notices and e-mails.",
        "UAM settings under maker-checker; dormant-user deactivation.",
    ],
    "out_scope": [
        "Raising, approving, correcting, cancelling and bulk-loading user access requests: done in UIDM-ISC.",
        "Creating identities, passwords and second-factor methods of bank users: done in Microsoft Entra ID.",
        "Certification campaigns and their decisions: run in UIDM-ISC, using the BIBS certification extract.",
        "Emergency sign-in without Entra ID (break-glass): not provided.",
    ],
}

DOC_SET = [
    ("FRS (this document)", "Requirements, rules, validations, personas, parameters, statuses, change control."),
    ("Process and Storyboards (BPS)", "To-be process flows, the 15-step journey and five storyboards with screenshots."),
    ("UX Design (UXD)", "Menu by persona and every screen: fields, validations, actions, states."),
    ("Reports, Notifications and Outputs (RNO)", "Reports, certification extract, exports, notices, e-mails, jobs."),
    ("Interface Specification (ICD) v1.0", "SCIM 2.0 with UIDM-ISC and OpenID Connect with Entra ID. Referenced, "
     "not repeated."),
    ("Workbooks v1.2", "RTM and test cases, Fit-Gap, CR Register and Effort Estimate, Requirements Collection, "
     "Walkthrough Users."),
]

REFERENCES = [
    "Business Requirements Document - QPS User Access Maintenance Module, v1 of 15-Apr-2025 (BRD-11).",
    "BRD-00 and the programme-wide non-functional requirements of the Collections, Claims, ReInsurance and "
    "Operations addenda.",
    "FRS - BDOI BROKERSYS User Access Maintenance v1.1 (BDOI), template D051.",
    "BIBS Interface Specification: User Access (SCIM 2.0 with UIDM-ISC, OpenID Connect with Microsoft Entra ID) v1.0.",
    "BIBS BDOI Feature List vs OOTB and Best Practice v1.0 (baseline of the colour code).",
    "User Access Matrix - Target State (BDOI, Annex C of BDOI's FRS).",
]

# Business Requirements Mapping: BRD group -> FRUM items, where it is done, change from v1.4
MAPPING = [
    ("BRD 1.001, 2.001, 3.001, 4.001", "Log in (all roles), inactivity warning, warning before sign-out",
     "FRUM.001", "BIBS with Entra ID", "Changed: sign-in through Entra ID only"),
    ("BRD 1.002", "Enrol new user", "FRUM.002", "UIDM-ISC; applied by BIBS", "Changed: request in UIDM-ISC"),
    ("BRD 1.003", "Modify existing user", "FRUM.003", "UIDM-ISC; applied by BIBS", "Changed: request in UIDM-ISC"),
    ("BRD 1.004", "Deactivate user", "FRUM.003", "UIDM-ISC; applied by BIBS", "Changed: request in UIDM-ISC"),
    ("BRD 1.005", "Reactivate user", "FRUM.003", "UIDM-ISC; applied by BIBS", "Changed: request in UIDM-ISC"),
    ("BRD 1.006", "Apply correction on a returned request", "FRUM.009 (withdrawn)", "UIDM-ISC", "Withdrawn from BIBS"),
    ("BRD 1.007", "Cancel a submitted request", "FRUM.009 (withdrawn)", "UIDM-ISC", "Withdrawn from BIBS"),
    ("BRD 1.008", "View requests and their status", "FRUM.009 (withdrawn); FRUM.007.01 for group-profile changes",
     "UIDM-ISC; BIBS for group profiles", "Changed"),
    ("BRD 1.009", "Bulk request", "FRUM.011 (withdrawn)", "UIDM-ISC", "Withdrawn from BIBS"),
    ("BRD 2.002", "Review and approve requests", "FRUM.010 (group-profile changes only)",
     "UIDM-ISC for users; BIBS for group profiles", "Changed"),
    ("BRD 3.002.1, 3.002.2", "New group profile; change of a group profile", "FRUM.005, FRUM.006, FRUM.010", "BIBS",
     "Changed: no approver list, any other checker decides"),
    ("BRD 3.002.3, 3.002.4", "Deactivate and reactivate a group profile", "FRUM.006, FRUM.010", "BIBS", "Kept"),
    ("BRD 3.003", "User Access, User Group Profile and Group Profile Membership reports", "FRUM.007", "BIBS",
     "Kept; certification extract added"),
    ("BRD 4.002.1, 4.002.2", "Define roles and their functions", "FRUM.005, FRUM.006", "BIBS", "Kept"),
    ("BRD 4.003", "Generate audit logs", "FRUM-008", "BIBS", "Kept"),
    ("BRD NFR 4, 11, 17, 33", "SSO integration; configurable workflow with escalation; directory sign-in",
     "FRUM.001, FRUM.010, FRUM.014", "BIBS with Entra ID and UIDM-ISC", "Changed"),
    ("BRD NFR 10", "Notification for access changes", "FRUM.013", "BIBS; UIDM-ISC for user requests", "Changed"),
    ("Operations addendum", "Segregation of duties across Marketing, Cashiering and Disbursement", "FRUM.012", "BIBS",
     "Kept"),
]

# Functional requirements. status: Kept / Changed / Withdrawn / New (against v1.4)
FRUM = [
    {"id": "FRUM.001", "component": "Login", "capability": "Sign-in, inactivity warning and time-out",
     "status": "Changed", "brd": "BRD 1.001, 2.001, 3.001, 4.001; NFR 4, 17", "was": "Login, inactivity warning and "
     "time-out (local passwords, break-glass)",
     "reqs": [
         ("FRUM.001.01", "Every user signs in through Microsoft Entra ID (OpenID Connect). The second factor is "
          "asked by Entra ID.", "k", "int",
          "BIBS links the Entra ID identity to an active BIBS user with the same Windows ID; an unknown or inactive "
          "identity is refused.",
          "Isabel Navarro clicks Sign in with Microsoft Entra ID, approves the prompt on her phone and lands on her "
          "home page."),
         ("FRUM.001.02", "After sign-in the user sees only the menus and actions of the group profiles held.", "k", "",
          "Permissions are the union of the active group profiles; data is limited to the user's data access.",
          "A Marketing Account Officer sees New Business and Client & Policy, not Setup & Administration."),
         ("FRUM.001.03", "The user is warned after 15 minutes without activity and signed out after 30 minutes.",
          "k", "", "Stay Logged In resets the timer; Log Out ends the session. Both values are UAM settings.",
          "At 15 minutes the dialog asks: stay logged in or log out?"),
         ("FRUM.001.04", "The user is warned before the system-triggered sign-out at the end of the session.", "k",
          "", "The warning time is a UAM setting (30 minutes by default).",
          "A message shows 30 minutes before the session ends."),
         ("FRUM.001.05", "Production offers no password sign-in. Test accounts sign in with a local password only in "
          "DEV, SIT and UAT.", "k", "",
          "Production refuses local sign-in, password change and password reset; the Login page shows only the Entra "
          "ID button.", "In UAT the Login page also shows Test account sign-in; in production it does not."),
         ("FRUM.001.06", "Break-glass administrator accounts (v1.4) are withdrawn.", "", "",
          "No account bypasses Entra ID. CR-UA-003 is withdrawn.", "-"),
     ]},
    {"id": "FRUM.002", "component": "User Management", "capability": "User creation (joiner)", "status": "Changed",
     "brd": "BRD 1.002", "was": "User creation through an enrolment request in BIBS",
     "reqs": [
         ("FRUM.002.01", "UIDM-ISC creates the user in BIBS with a SCIM 2.0 joiner event, after the request is "
          "approved in UIDM-ISC.", "k", "int",
          "The event carries the User ID, Windows ID, name, e-mail, department, unit, location and UIDM request "
          "number. The user starts active without a group profile.",
          "Joiner UIDM-2026-005196 creates a013000196 Isabel Navarro, Marketing, Makati."),
         ("FRUM.002.02", "BIBS refuses to create or change a user from its own screens.", "k", "",
          "The Users screen is read-only for the user record; any attempt is refused with \"Users are maintained "
          "in UIDM-ISC\".", "-"),
         ("FRUM.002.03", "A joiner whose User ID or Windows ID belongs to another user is refused and listed as a "
          "refused event.", "k", "int", "The User Access Administrator is alerted (see FRUM.014.02).",
          "Windows ID BDO\\sit.transferee belongs to another user: refused."),
     ]},
    {"id": "FRUM.003", "component": "User Management", "capability": "User details update (mover, leaver, rehire); "
     "data access; unlock; sessions", "status": "Changed", "brd": "BRD 1.003, 1.004, 1.005",
     "was": "User update through modify, deactivate and reactivate requests in BIBS",
     "reqs": [
         ("FRUM.003.01", "UIDM-ISC changes a user's details with a mover event and the group profiles with a group "
          "membership event.", "k", "int",
          "Each change is kept attribute by attribute in the access history with the UIDM request number.",
          "UIDM-2026-005240 moves Isabel Navarro to Corporate Marketing and from Marketing Account Officer to "
          "Marketing Team Leader."),
         ("FRUM.003.02", "A leaver event deactivates the user and ends every open session at once.", "k", "int",
          "A rehire event reactivates the user; group profiles come again from UIDM-ISC.",
          "UIDM-2026-005251 deactivates Carlo Mendoza; his session ends."),
         ("FRUM.003.03", "The User Access Administrator sets the companies and branches a user may act for.", "o", "",
          "The change applies at once and is logged. Change request CR-UA-007 (data access by company and branch).",
          "Isabel Navarro: FVI, branches Head Office and Makati."),
         ("FRUM.003.04", "The User Access Administrator ends all sessions of a user.", "g", "",
          "Asks for confirmation; logged with the reason.", "-"),
         ("FRUM.003.05", "The User Access Administrator unlocks a test account locked after failed sign-ins.", "k", "",
          "Applies to local test accounts outside production only.", "-"),
         ("FRUM.003.06", "A user without a sign-in for the dormancy period is deactivated by the daily run, after a "
          "notice.", "g", "",
          "Default 90 days, notice 7 days before. The User Access Administrator is told to disable the user in "
          "UIDM-ISC too.", "-"),
     ]},
    {"id": "FRUM-004", "component": "User Management", "capability": "Password maintenance", "status": "Changed",
     "brd": "BRD NFR 4; Annex I of BDOI's FRS", "was": "Password maintenance of local accounts and break-glass accounts",
     "reqs": [
         ("FRUM-004.01", "Passwords and second-factor methods of bank users are kept by Microsoft Entra ID.", "k",
          "int", "BIBS stores no password of a bank user and offers no password change for them.", "-"),
         ("FRUM-004.02", "Local test accounts follow BDOI's password guidelines in DEV, SIT and UAT.", "k", "",
          "Lock-out after 3 failed sign-ins; expiry 90 days; minimum age 1 day; last 10 passwords not reused.", "-"),
         ("FRUM-004.03", "A test account's password is reset by another System Administrator, never by its owner.",
          "k", "", "Refused in production and wherever local passwords are off.", "-"),
     ]},
    {"id": "FRUM.005", "component": "Roles Management", "capability": "Group-profile creation", "status": "Changed",
     "brd": "BRD 3.002.1, 4.002.1", "was": "Group-profile request with approvers in order",
     "reqs": [
         ("FRUM.005.01", "The Group Profile Maker proposes a new group profile: code, name, description, privilege "
          "level, permissions and reason.", "k", "",
          "Save Draft keeps it; Submit for Approval sends it to the checkers. No approver is chosen.",
          "CLIENT_ENQUIRY - Client enquiry, Standard, permissions Client view and Report view."),
         ("FRUM.005.02", "The new group profile exists only once approved (and implemented when the setting asks "
          "for it).", "k", "", "Until then it is listed under Proposed New Group Profiles.", "-"),
     ]},
    {"id": "FRUM.006", "component": "Roles Management", "capability": "Group-profile change, deactivation and "
     "reactivation", "status": "Kept", "brd": "BRD 3.002.2 to 3.002.4, 4.002.2", "was": "Same",
     "reqs": [
         ("FRUM.006.01", "The Group Profile Maker proposes a change of name, description, level or permissions of an "
          "active group profile.", "k", "",
          "Blank fields keep the current value; the change must alter at least one item.",
          "Marketing Account Officer: add Client approve."),
         ("FRUM.006.02", "The Group Profile Maker proposes the deactivation or reactivation of a group profile.", "k",
          "", "On deactivation the members lose the profile's access; they are told by notice.", "-"),
         ("FRUM.006.03", "A group profile has at most one open change.", "k", "",
          "A second proposal is refused until the open one is decided or withdrawn.", "-"),
     ]},
    {"id": "FRUM.007", "component": "Reports Management", "capability": "Reports, access matrix and certification "
     "extract", "status": "Changed", "brd": "BRD 3.003; NFR 1.g", "was": "Reports generation and exports",
     "reqs": [
         ("FRUM.007.01", "Five reports: User Access, User Group Profile, Group Profile Membership, User Access Audit "
          "Log and Group Profile Changes, each exported to PDF, Excel and CSV.", "k", "",
          "Fields and parameters are in the RNO document.", "-"),
         ("FRUM.007.02", "The Access Matrix shows the group profiles against the permissions, and by action, with an "
          "Excel export.", "k", "", "Read-only for every persona.", "-"),
         ("FRUM.007.03", "The certification extract lists every account, group profile and permission in CSV or "
          "XLSX.", "g", "",
          "Each extract is logged with who took it, when, the rows and the SHA-256 of the file; Information Security "
          "is told.", "Audrey Access Auditor takes the XLSX extract for the quarterly certification."),
     ]},
    {"id": "FRUM-008", "component": "Generate Logs", "capability": "Audit trail", "status": "Kept",
     "brd": "BRD 4.003; NFR 6; addenda NFR 4.03", "was": "Same",
     "reqs": [
         ("FRUM-008.01", "Every user access event is in the audit trail with the from and to values, who, when, "
          "the source address and the request or change number.", "k", "",
          "Users, sessions, group profiles and their changes, SoD rules, identity events, mismatches, certification "
          "extracts and settings.", "-"),
         ("FRUM-008.02", "Sign-ins, failed sign-ins and sign-outs are logged and can be included in the audit log "
          "report.", "k", "", "-", "-"),
     ]},
    {"id": "FRUM.009", "component": "User Access Requests", "capability": "Access request lifecycle (Requestor)",
     "status": "Withdrawn", "brd": "BRD 1.002 to 1.009", "was": "Raise, edit, submit, correct, cancel and view user "
     "access requests in BIBS",
     "reqs": [
         ("FRUM.009.01", "Withdrawn. User access requests are raised, corrected, cancelled and followed in UIDM-ISC; "
          "BIBS receives only the approved result.", "", "int", "BDOI's review comment \"c/o UIDM\".", "-"),
     ]},
    {"id": "FRUM.010", "component": "Roles Management", "capability": "Decision on group-profile changes (Checker)",
     "status": "Changed", "brd": "BRD 2.002, 3.002; NFR 11", "was": "Review and approval of user access requests",
     "reqs": [
         ("FRUM.010.01", "Any Group Profile Checker other than the maker approves, returns for correction or "
          "rejects a submitted change.", "k", "",
          "Return and Reject need a remark. The maker corrects a returned change and submits it again.",
          "Ulysses Access Approver approves AR-2026-000001 with \"Checked against the access matrix\"."),
         ("FRUM.010.02", "A change of a High or Admin group profile, or submitted outside the working hours, needs a "
          "second approval by another checker.", "k", "",
          "The second approver differs from the maker and the first approver.", "-"),
         ("FRUM.010.03", "An approved change applies at once, or waits for the System Administrator to implement "
          "it when the setting asks for it.", "k", "",
          "The implementer differs from the maker.", "AR-2026-000001 is For Implementation, then Implemented."),
         ("FRUM.010.04", "A change that would break an active separation-of-duties rule is refused.", "k", "", "-",
          "-"),
         ("FRUM.010.05", "The approver list and effective date of v1.4 are withdrawn.", "", "",
          "No approver is chosen; a change applies on its decision date.", "-"),
     ]},
    {"id": "FRUM.011", "component": "User Access Requests", "capability": "Bulk request", "status": "Withdrawn",
     "brd": "BRD 1.009", "was": "Bulk upload of user access requests",
     "reqs": [
         ("FRUM.011.01", "Withdrawn. Bulk changes are made in UIDM-ISC and reach BIBS as individual SCIM events.",
          "", "int", "-", "-"),
     ]},
    {"id": "FRUM.012", "component": "Security Administration", "capability": "Separation of duties and UAM settings",
     "status": "Changed", "brd": "BRD NFR 2-3; Operations addendum", "was": "Separation of duties and security "
     "settings",
     "reqs": [
         ("FRUM.012.01", "Separation-of-duties rules name two group profiles, or two permissions, one user may not "
          "hold together.", "k", "",
          "A rule is proposed and authorised by Information Security; deactivation also needs authorisation.",
          "SOD-900001: Group Profile Maker may not be held with Group Profile Checker."),
         ("FRUM.012.02", "A daily check lists the users who breach an active rule and tells Information Security of "
          "the new ones.", "k", "", "The breach is corrected in UIDM-ISC.", "-"),
         ("FRUM.012.03", "UAM settings change only after another user approves the change.", "k", "",
          "The maker may withdraw a pending change; every change is logged.",
          "Dormancy notice 7 to 10 days: proposed by the User Access Administrator, approved by Information "
          "Security."),
         ("FRUM.012.04", "The System Administrator may edit a group profile directly only while Information Security "
          "has opened the emergency path.", "o", "", "Every edit is logged and alerted. Change request CR-UA-016.",
          "-"),
     ]},
    {"id": "FRUM.013", "component": "Notifications", "capability": "Notices and e-mails", "status": "Changed",
     "brd": "BRD NFR 10", "was": "Notices and e-mails of user access requests",
     "reqs": [
         ("FRUM.013.01", "BIBS sends the notices of group-profile changes, access changes, dormancy, identity "
          "events, reconciliation, separation of duties, settings and certification extracts.", "k", "",
          "The list and texts are in the RNO document. Notices of user access requests come from UIDM-ISC.", "-"),
         ("FRUM.013.02", "A notice is also e-mailed when the user's preference or the setting asks for it.", "k", "",
          "-", "-"),
     ]},
    {"id": "FRUM.014", "component": "Identity Synchronisation", "capability": "Monitoring, refused events and "
     "reconciliation", "status": "New", "brd": "BRD NFR 4, 11; integration scope IS-UA-01, IS-UA-02",
     "was": "Part of FRUM.002 and FRUM.003 (Annex N)",
     "reqs": [
         ("FRUM.014.01", "Identity Sync shows the last call from UIDM-ISC, the last successful event, errors of the "
          "last 24 hours, refused events and open mismatches.", "k", "int", "-", "-"),
         ("FRUM.014.02", "An event BIBS cannot apply is refused, listed with its reason and alerted to the User "
          "Access Administrator.", "k", "int",
          "Retry processes it again after UIDM-ISC corrected the data; Dismiss closes it with a remark.",
          "Group profile MKT_SUPERVISOR does not exist: refused, then dismissed."),
         ("FRUM.014.03", "A daily reconciliation compares every user and its group profiles with UIDM-ISC and lists "
          "the mismatches.", "k", "int",
          "Synchronise Now runs it at once. A mismatch is resolved with a remark once UIDM-ISC corrected the user.",
          "Isabel Navarro: active in BIBS, not active in the directory."),
     ]},
]

BUSINESS_RULES = [
    ("BR-UA-01", "UIDM-ISC is the only source of users and of group-profile membership.", "k"),
    ("BR-UA-02", "Every bank user signs in through Microsoft Entra ID; no account bypasses it.", "k"),
    ("BR-UA-03", "Local passwords exist only for test accounts in DEV, SIT and UAT.", "k"),
    ("BR-UA-04", "A group-profile change is decided by a user other than its maker.", "k"),
    ("BR-UA-05", "A High or Admin profile change, or a change outside the working hours, needs a second approval.", "k"),
    ("BR-UA-06", "The implementer of an approved change is not its maker.", "k"),
    ("BR-UA-07", "A group profile has at most one open change.", "k"),
    ("BR-UA-08", "Nobody decides a change about his or her own access.", "k"),
    ("BR-UA-09", "A change that breaks an active separation-of-duties rule is refused.", "k"),
    ("BR-UA-10", "A leaver loses access and every open session at once.", "k"),
    ("BR-UA-11", "A UAM setting changes only after another user approves it.", "k"),
    ("BR-UA-12", "Dormant users are deactivated after the dormancy period and a notice.", "g"),
    ("BR-UA-13", "Every certification extract is logged with its SHA-256.", "g"),
    ("BR-UA-14", "Data access limits a user to the companies and branches given.", "o"),
]

MESSAGES = [
    # screen / step, condition, message shown
    ("Users", "Any attempt to create or change a user in BIBS", "Users are maintained in UIDM-ISC."),
    ("Login", "Entra ID identity not linked to an active user", "Your account is not active in BIBS. Contact "
     "your User Access Administrator."),
    ("Login (production)", "Local password sign-in attempted", "Password sign-in is not used in this environment."),
    ("Test account reset", "Reset where local passwords are off", "Passwords are reset in Microsoft Entra ID."),
    ("Test account reset", "Own password", "Your own password is reset by another System Administrator; change it "
     "from My Profile."),
    ("Propose change", "Code missing or invalid", "Enter the code: capital letters, digits and _ (up to 40)."),
    ("Propose change", "Name missing on a new profile", "Enter the name of the group profile."),
    ("Propose change", "No permission on a new profile", "Select at least one permission."),
    ("Propose change", "Nothing changed", "Change the permissions, the name, the description or the level."),
    ("Propose change", "Reason missing", "Enter the reason for the change."),
    ("Propose change", "Another change of the profile is open", "A change of this group profile is already open."),
    ("Decision", "Maker tries to decide", "A request cannot be decided by the user who submitted it."),
    ("Decision", "Change of the decider's own access", "You cannot decide a request about your own access."),
    ("Decision", "Reject without reason", "Enter the reason of the rejection."),
    ("Second approval", "Same user as first approver", "The second approval is given by another approver."),
    ("Implement", "Maker tries to implement", "Another System Administrator implements the change."),
    ("Separation of Duties", "Same profile twice", "Choose two different group profiles."),
    ("Separation of Duties", "Reason missing", "Enter why the two profiles are not held together."),
    ("Separation of Duties", "Reject without reason", "Give the reason for the rejection."),
    ("Identity Sync", "Dismiss or resolve without remark", "Enter the remark."),
    ("Identity Sync", "Event already dismissed", "The event has already been dismissed."),
    ("Users", "End sessions of a user with none open", "The session has already ended."),
    ("Reports", "As-of date in the future", "The as-of date cannot be in the future."),
    ("Certification", "Format other than CSV or XLSX", "Choose CSV or XLSX."),
]

PERMISSIONS = [
    # function, A, M, C, I, U
    ("View users and their detail", "Y", "-", "-", "-", "Y"),
    ("Set data access; unlock; end sessions", "Y", "-", "-", "-", "-"),
    ("View group profiles", "Y", "Y", "Y", "Y", "Y"),
    ("Propose a group-profile change", "-", "Y", "-", "-", "-"),
    ("Approve, return or reject a change", "-", "-", "Y", "-", "-"),
    ("Identity Sync: retry, dismiss, resolve, synchronise", "Y", "-", "-", "-", "-"),
    ("Separation of Duties: view rules and violations", "Y", "-", "-", "Y", "Y"),
    ("Separation of Duties: authorise or reject rules", "-", "-", "-", "Y", "-"),
    ("Access Matrix and reports", "Y", "Y", "Y", "Y", "Y"),
    ("Take the certification extract", "Y", "Y", "Y", "Y", "Y"),
    ("Audit Log", "Y", "-", "-", "Y", "Y"),
    ("UAM Settings: propose a change", "Y", "-", "-", "-", "-"),
    ("UAM Settings: approve or reject", "-", "-", "-", "Y", "-"),
]

PARAMETERS = [
    # name on screen, default, range, area, colour
    ("Session timeout", "30 minutes", "5 to 480", "Sessions", "k"),
    ("Inactivity warning before sign-out", "15 minutes", "1 to 480", "Sessions", "k"),
    ("Warning before the session ends", "30 minutes", "1 to 240", "Sessions", "k"),
    ("Bank's inactivity warning wording", "Yes", "Yes / No", "Sessions", "k"),
    ("Session timed-out page", "Yes", "Yes / No", "Sessions", "k"),
    ("Dormant after", "90 days", "0 (never) to 730", "Dormancy", "g"),
    ("Dormancy notice before", "7 days", "0 to 60", "Dormancy", "g"),
    ("Failed sign-ins before lock-out", "3", "1 to 10", "Local test accounts", "k"),
    ("Password expires after", "90 days", "0 to 365", "Local test accounts", "k"),
    ("Minimum password age", "1 day", "0 to 30", "Local test accounts", "k"),
    ("Previous passwords not reused", "10", "0 to 24", "Local test accounts", "k"),
    ("Password expiry notice", "7 days", "1 to 90", "Local test accounts", "k"),
    ("Password reset by another administrator only", "Yes", "Yes / No", "Local test accounts", "k"),
    ("Provisioning from UIDM-ISC", "Yes", "Yes / No", "Synchronisation", "k"),
    ("Group profiles from UIDM-ISC events", "Yes", "Yes / No", "Synchronisation", "k"),
    ("Reconciliation every", "1 day", "0 (never) to 31", "Synchronisation", "k"),
    ("Apply a group-profile change at approval", "No (System Administrator implements)", "Yes / No",
     "Group profiles", "k"),
    ("Working hours", "00:00-24:00, MON-SUN (BDOI to set, Q-UA-01)", "Time range and days", "Group profiles", "k"),
    ("Emergency direct edit of group profiles", "No", "Yes / No", "Group profiles", "o"),
]

LOVS = [
    ("Privilege level", "Low, Standard, High, Admin"),
    ("User status", "Active, Inactive, Locked"),
    ("Change type", "New group profile; Change of definition or permissions; Deactivation; Reactivation"),
    ("Identity event type", "Joiner, Mover, Leaver, Rehire, Status change, Group profiles"),
    ("Identity event outcome", "Applied, No change, Refused, Failed"),
    ("Reconciliation mismatch", "Not in the directory; Status differs; Group profiles differ"),
    ("SoD rule type", "Group profiles; Permissions"),
    ("Certification extract format", "CSV, XLSX"),
]

STATUSES = {
    "Group-profile change": [
        ("Draft", "Saved by the maker, not submitted", "Submitted, Withdrawn"),
        ("Pending", "Waiting for a checker", "Approved, Pending Second, Returned, Rejected, Cancelled"),
        ("Pending Second", "Waiting for the second approval", "Approved, Rejected, Cancelled"),
        ("Returned", "Sent back to the maker for correction", "Pending (resubmitted), Cancelled"),
        ("For Implementation", "Approved; waiting for the System Administrator", "Implemented"),
        ("Approved", "Approved and applied (setting: apply at approval)", "-"),
        ("Implemented", "Applied by the System Administrator", "-"),
        ("Rejected", "Refused by a checker, with the reason", "-"),
        ("Cancelled", "Withdrawn by the maker", "-"),
    ],
    "User": [
        ("Active", "May sign in through Entra ID", "Inactive (leaver, dormancy), Locked (test account)"),
        ("Inactive", "Deactivated by UIDM-ISC or by the dormancy run", "Active (rehire or reactivation)"),
        ("Locked", "Test account after failed sign-ins", "Active (unlock)"),
    ],
    "Identity event": [
        ("Applied", "Change made in BIBS", "-"),
        ("No change", "Event carried nothing new", "-"),
        ("Refused", "Data not acceptable; waits for retry or dismissal", "Applied (retry), Dismissed"),
        ("Failed", "Technical failure; retried", "Applied, Refused"),
    ],
    "Separation-of-duties rule": [
        ("Pending Authorization", "Proposed; waits for Information Security", "Active, Rejected"),
        ("Active", "Enforced and checked daily", "Pending deactivation"),
        ("Inactive", "Deactivated after authorisation", "-"),
    ],
    "UAM setting": [
        ("In Force", "Current value applies", "Pending Approval"),
        ("Pending Approval", "New value proposed; old value still applies", "In Force (approved, rejected or withdrawn)"),
    ],
}

ASSUMPTIONS = [
    ("A-UA-01", "UIDM-ISC sends every joiner, mover, leaver and membership change to BIBS over SCIM 2.0, as set out "
     "in the Interface Specification v1.0."),
    ("A-UA-02", "Microsoft Entra ID enforces the second factor and BDOI's device policy for BIBS."),
    ("A-UA-03", "The group-profile codes used in UIDM-ISC are the codes defined in BIBS."),
    ("A-UA-04", "BDOI accepts that BIBS is unavailable for sign-in while Entra ID is unavailable."),
]
DEPENDENCIES = [
    ("D-UA-01", "BDOI IT registers BIBS in Entra ID for each environment (client, redirect address)."),
    ("D-UA-02", "BDOI IT connects UIDM-ISC to the BIBS SCIM service through the API gateway, with test events in SIT "
     "and UAT."),
    ("D-UA-03", "BDOI provides the target User Access Matrix (group profiles and permissions) before SIT."),
    ("D-UA-04", "The users and group profiles at go-live are loaded by Data Migration (BRD-13)."),
]
OPEN_QUESTIONS = [
    ("Q-UA-01", "Working hours for the out-of-hours second approval (today every hour of every day, so no change is out of hours)?",
     "Information Security", "01 Dec 2026"),
    ("Q-UA-02", "Dormancy period and notice: keep 90 and 7 days?", "Business Administration", "01 Dec 2026"),
    ("Q-UA-03", "Apply an approved group-profile change at approval, or after implementation by the System "
     "Administrator?", "Information Security", "01 Dec 2026"),
    ("Q-UA-04", "Certification frequency and the reviewers who receive the extract?", "Internal Audit",
     "01 Dec 2026"),
    ("Q-UA-05", "Approve change request CR-UA-007 (data access by company and branch)?", "Business Administration",
     "01 Dec 2026"),
    ("Q-UA-06", "Approve change request CR-UA-016 (emergency direct edit of group profiles)?", "Information Security",
     "01 Dec 2026"),
]

# Change control
CRS = [
    # id, title, status v1.2, frs ref, effort [analysis, backend, frontend, testing, docs], note
    ("CR-UA-003", "Break-glass administrators", "Withdrawn", "-", [1, 2, 1, 1.5, 0.5],
     "No break-glass account in the agreed model (BDOI IT, 15 Oct 2026)."),
    ("CR-UA-007", "Data access by company and branch", "Open", "FRUM.003.03", [1.5, 5, 2, 3, 1],
     "Rescoped: company and branch scope per user. The supervisor and record-owner scope of v1.4 is dropped."),
    ("CR-UA-011", "Approval time limits and reminders of access requests", "Withdrawn", "-", [0.5, 2, 0.5, 1.5, 0.75],
     "User access requests are handled in UIDM-ISC."),
    ("CR-UA-014", "End date on the granting request (temporary access)", "Withdrawn", "-", [1, 2.5, 2, 1.5, 0.75],
     "Requests and effective dates are handled in UIDM-ISC."),
    ("CR-UA-016", "Emergency direct edit of group profiles", "Open", "FRUM.012.04", [1, 2.5, 1, 1.5, 0.5],
     "Kept: group-profile path, not a request workflow."),
]
OOTB_POINTS = [
    ("OB-UA-02", "Dormant-user deactivation with notice", "FRUM.003.06"),
    ("OB-UA-04", "Settings applied without a release, under maker-checker", "FRUM.012.03"),
    ("OB-UA-05", "Certification extract with its log (periodic access review)", "FRUM.007.03"),
    ("OB-UA-06", "End all sessions of a user", "FRUM.003.04"),
]
INTEGRATION_POINTS = [
    ("IS-UA-01", "Joiner, mover, leaver and rehire from UIDM-ISC over SCIM 2.0", "FRUM.002, FRUM.003"),
    ("IS-UA-02", "Group-profile membership from UIDM-ISC", "FRUM.003.01"),
    ("IS-UA-03", "Sign-in through Microsoft Entra ID with its second factor", "FRUM.001.01"),
    ("IS-UA-05", "Test accounts and identity events in SIT and UAT", "-"),
    ("IS-UA-06", "User access requests raised, approved, corrected, cancelled and bulk-loaded in UIDM-ISC",
     "FRUM.009, FRUM.011"),
    ("IS-UA-10", "Refused events and reconciliation with UIDM-ISC", "FRUM.014"),
    ("DM-UA-01", "Users and group profiles at go-live", "BRD-13"),
]
BEST_PRACTICE = [
    ("BP-UA-01", "Monitoring of privileged sessions", "Medium", 6),
    ("BP-UA-02", "Alert on unusual sign-in", "Medium", 5),
    ("BP-UA-03", "Log of views of sensitive records", "Medium", 6),
    ("BP-UA-04", "Joiner-mover-leaver timeliness report", "Medium", 4),
    ("BP-UA-06", "Generic and shared account register", "Low", 3),
]

CHANGES_V14 = {
    "removed_screens": ["Access Requests (list, new, edit, record)", "Bulk Request and its batch", "Group Profile "
                        "Requests with approvers in order", "Roles & Permissions implement screen", "Forgot Password "
                        "and Change Your Password for bank users", "System Parameters (sign-in and password)"],
    "added_screens": ["Users with detail tabs (Profile, Group Profiles, Effective Permissions, Data Access, Sessions, "
                      "Access History)", "Group Profiles with detail tabs (Definition, Permissions, Members, Pending "
                      "Change, History)", "Identity Sync (Status, Events, Refused, Reconciliation)", "Reports & "
                      "Certification with the certification extract", "Audit Log preset to user access", "UAM "
                      "Settings under maker-checker"],
    "removed_rules": ["Requester chooses the approver; approvers in order", "Effective date and end date of access",
                      "Break-glass sign-in and its alert", "Temporary password shown once", "Approval time limits "
                      "and reminders"],
    "added_rules": ["UIDM-ISC is the only source of users and membership (BR-UA-01)", "Any other checker decides "
                    "(BR-UA-04)", "Local passwords only for test accounts (BR-UA-03)", "Refused events and daily "
                    "reconciliation (FRUM.014)", "Certification extract logged with SHA-256 (BR-UA-13)"],
    "removed_notices": ["Request returned, request cancelled and decision notices of user access requests",
                        "Reset your password and password expiry (bank users)", "Break-glass sign-in alert",
                        "Scheduled access change failed", "Second-factor reset to approve"],
    "added_notices": ["Identity event to review", "Identity reconciliation mismatches", "Certification extract "
                      "taken", "Separation-of-duties breaches"],
}
