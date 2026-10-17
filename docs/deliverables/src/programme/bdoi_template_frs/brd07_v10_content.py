"""Content of the Claims document set v1.0 (BRD-07, Claims (CLM) BRD with the workshop addendum) in BDOI's template:
the FRS, the process and storyboards (BPS), the UX design (UXD, screens in brd07_v10_pack.py), the reports,
notifications and outputs (RNO) and the interface specification (ICD). Read by build_bdoi_v10_packs.py and
brd07_v10_pack.py. Business wording only.

Source: BDOI's "FRS - BDOI BROKERSYS Claims v1.0" (template D051, 13-Aug-2026, received 17-Oct-2026): business
requirements mapping of BRCLM.001 to 043, items FRCLM.001 to 020 and annexes A to AC. This version keeps BDOI's
numbers, states each sub-item as a requirement row, adds FRCLM.021 to 024 for the BRD lines BDOI's items do not
state, and applies the colour code: BDOI's items beyond the BRD are green (OOTB) or orange (change request).

Colour of a point: "k" black (in the BRD, BRD-00 or a programme document), "g" green (not in the BRD, offered by the
iNXT BrokerVerse OOTB platform), "o" orange (change request), "b" blue (best practice, separate table). A label "int"
or "dm" adds the grey [Integration scope] or [Data Migration scope] label. Fit against the BIBS platform: Fit,
Partial, Gap, Change request, Integration, Data Migration, In another FRS.
"""

from __future__ import annotations

VERSION = "1.0"
DROP = "Drop 2"
WB_VERSION = "1.0"
ICD_VERSION = "1.0"
# Documents issued again after the release of 17 Oct 2026 changed them; the others stay at VERSION.
DOC_VERSIONS = {"UXD": "1.1", "RNO": "1.1", "DSP": "1.1", "DDI": "1.1"}
SHOTS_DIR = "brd07_v11_screens"
DATE = "17 Oct 2026"
DATE_LONG = "17 October 2026"
DATE_US = "10/17/2026"
CREATED_US = {"FRS": "08/13/2026"}
AUTHOR = "iorta TechNXT Project Team (drafting support); for review by the BDOI Product Owner"
OWNER = "BDOI Product Owner, Claims"
ANSWER_BY = "15 Dec 2026"
BRD = "BRD-07"
NAME = "Claims"
SHORT = "Claims"
CODE = "CLM"
DOC_NO = "PRJ0012475-CLM"
HEADER_RIGHT = "BDO Insure Brokering System (BIBS) - Claims"

DOCS = {
    "FRS": {"file": f"BIBS_FRS-BDOI_BRD-07_Claims_v{VERSION}.docx",
            "cover": "Functional Requirements Specification: Claims",
            "header_left": "Functional Requirements Specification", "doc_no": DOC_NO},
    "BPS": {"file": f"BIBS_BPS-BDOI_BRD-07_Claims_Process_and_Storyboards_v{VERSION}.docx",
            "cover": "Claims: To-Be Process and Storyboards", "header_left": "To-Be Process and Storyboards",
            "doc_no": f"{DOC_NO}-BPS"},
    "UXD": {"file": f"BIBS_UXD-BDOI_BRD-07_Claims_UX_Design_v{DOC_VERSIONS['UXD']}.docx",
            "cover": "Claims: Screens and UX Design", "header_left": "Screens and UX Design",
            "doc_no": f"{DOC_NO}-UXD"},
    "RNO": {"file": f"BIBS_RNO-BDOI_BRD-07_Claims_Reports_Notifications_Outputs_v{DOC_VERSIONS['RNO']}.docx",
            "cover": "Claims: Reports, Notifications and Outputs", "header_left": "Reports, Notifications and Outputs",
            "doc_no": f"{DOC_NO}-RNO"},
    "ICD": {"file": f"BIBS_ICD-BDOI_BRD-07_Claims_Interface_Specification_v{ICD_VERSION}.docx",
            "cover": "Claims: Interface Specification", "header_left": "Interface Specification",
            "doc_no": f"{DOC_NO}-ICD"},
}

REVISIONS = [
    ("13 Aug 2026", "0.1", "BDOI's FRS for Claims (BROKERSYS Claims v1.0): mapping of BRCLM.001 to 043, items FRCLM.001 "
     "to FRCLM.020 and annexes A to AC.", "BDOI System Analyst"),
    (DATE, VERSION, "First issue in the standard document set: BDOI's items FRCLM.001 to 020 kept with their numbers, "
     "each sub-item stated as a requirement row with acceptance criteria; FRCLM.021 to 024 added for the BRD lines "
     "BDOI's items do not state (premium check, insurer communication, settlement and closure, special remittance); "
     "colour code applied (BDOI's items beyond the BRD in green or orange with change requests and effort); review "
     "of BDOI's FRS with the points to align. Names of persons replaced by roles.", AUTHOR),
]
REVISION_COMPANION = [(DATE, VERSION, "First issue, with the FRS Claims v1.0.", AUTHOR)]
REVISIONS_UXD = REVISION_COMPANION + [(DATE, "1.1", "Second issue with the release of 17 Oct 2026: SCR-CLM-01, 02, 04, 08 and 09 updated for the delivered follow-ups, document checklist, motor details, return to account officer, worklist sort and export, home trend, report heading and grouping, and team lead and team head on the handlers; screenshots captured from the running application on seed data. The screens of the change requests CR-CLM-101 to 112 stay proposed.", AUTHOR)]
REVISIONS_RNO = REVISION_COMPANION + [(DATE, "1.1", "Second issue with the release of 17 Oct 2026: worklist export, loss advice preview, follow-up and return-to-account-officer notices, and the BDOI heading, presentation and grouping of the claims reports.", AUTHOR)]
ICD_REVISIONS = [(DATE, ICD_VERSION, "First issue: loss advice to the insurer, insurer claim updates and references "
                  "by upload, claim documents, module hand-offs and the legacy claims load.", AUTHOR)]

# ================================================================================================ shared facts
PERSONAS = [
    ("Claims Handler (Claims Officer)", "Reviews claims reporting forms, records and completes claims, sends the "
     "formal loss advice, records insurer claim numbers and updates, follows up, keeps the diary.",
     "Claims Home, Claims Worklist, Record Claim, Claim, Cover Lookup, My Diary", "Clara Claims Officer"),
    ("Claims Team Leader", "Approves or returns follow-ups and record changes of the team, sets statuses, settlement, "
     "adjuster, reserve and follow-up date, closes claims, reassigns.", "Claims Home, Claims Worklist, Claim, "
     "Approvals (proposed)", "Tomas Claims Lead"),
    ("Claims Team Head", "Gives the final approval, follows every team, reopens claims.", "Claims Home, Claims "
     "Worklist, Claim, Approvals (proposed)", "Teodora Claims Head"),
    ("Claims Unit Head", "Keeps the claims lists, status access matrix and the claims handler groups; runs the data "
     "extract.", "Claims Setup, Claims Reports", "Ulysses Claims Unit Head"),
    ("Claims / Risk user", "Reads claims, claims-prone locations and loss reports.", "Claims Reports, Cover Lookup",
     "Rina Risk Analyst"),
    ("Marketing Account Officer", "Fills the claims reporting form of a client (proposed) and reads the loss reports "
     "of the accounts.", "Claims Reporting Form (proposed), Claims Reports", "Andres Account Officer"),
    ("Marketing Team Leader", "As the Account Officer, and exports the reports.", "Claims Reports",
     "Marta Marketing Lead"),
    ("MBS Administrative Unit", "Keeps the permissions of the claims group profiles in BIBS under maker-checker; "
     "users and memberships come from UIDM-ISC.", "User Access screens (BRD-11)", "SIT User Access Administrator"),
    ("Auditor", "Reads claims, their history and the reports. Changes nothing.", "Claim, Claims Reports",
     "Audrey Auditor"),
]

MENU = [
    ("Claims Home", "Dashboard of the user's role: open claims, follow-ups due, ageing, pending approvals", "H T E U"),
    ("Claims Worklist", "Claims follow-up list with tabs, filters and quick view", "H T E U R"),
    ("Claims Reporting Forms", "Forms filled by Marketing for the handlers (proposed)", "H T A"),
    ("Record Claim", "Cover search and new claim", "H T E"),
    ("Claim", "One claim: details, locations, insurers, reserve, documents, diary, history", "H T E U R X"),
    ("Approvals", "Follow-ups and record changes waiting for approval (proposed)", "T E"),
    ("Cover Lookup", "Read-only cover with premium and claims", "H T E U R"),
    ("My Diary", "The user's diary entries", "H T E"),
    ("Insurer Location References", "References of the insured locations per insurer", "H T E"),
    ("Claims Reports", "The claims reports", "H T E U R A M X"),
    ("Claims Setup", "Claims lists, status access matrix, handler groups, masters", "U"),
]
PERSONA_CODES = {"H": "Handler", "T": "Team Leader", "E": "Team Head", "U": "Unit Head", "R": "Risk",
                 "A": "Mkt AO", "M": "Mkt TL", "X": "Auditor"}
MENU_NOTE = "The screens are under Claims & Insurance › Claims Handling. A persona sees only its entries."

LEGEND_NOTE = (
    "Each requirement and rule carries a colour. Black: asked by the Claims BRD, BRD-00 or a programme document. "
    "BDOI's FRS adds points that are not in the BRD; with the other points not in the BRD they form one group with "
    "three colours: green when the iNXT BrokerVerse OOTB platform offers them at no cost, orange when they need a "
    "change request, and blue for best-practice recommendations, kept in a separate table. A grey label marks a "
    "point delivered by the Integration workstream or by Data Migration (BRD-13).")

# ================================================================================================ FRS
FRS_INTRO = {
    "purpose": (
        "This document states the functional requirements of Claims in BIBS, the BDO Insure Brokering System. It "
        "takes BDOI's FRS for Claims v1.0 (BROKERSYS Claims) into the standard document set: BDOI's items FRCLM.001 "
        "to FRCLM.020 keep their numbers, each sub-item is a requirement row with acceptance criteria, and items "
        "FRCLM.021 to FRCLM.024 state the BRD lines that BDOI's items do not cover."),
    "audience": (
        "The BIBS project team; the BDOI Business Unit owners of Claims (Motor and Non-Motor), Marketing and Risk; "
        "the MBS Administrative Unit; BDO ITG; Internal Audit."),
    "background": [
        "BDOI is a broker: the insurer decides and pays a claim, BDOI records it, coordinates it and follows it up for "
        "the client. Claims of Motor and Non-Motor lines are kept today in the core system, spreadsheets and e-mail.",
        "In BIBS a claim is recorded on the cover (account and policy year) with a snapshot of the policy, the "
        "premium check, the locations and the insurers with their claim numbers. The handler sends the formal loss "
        "advice, records the insurer's updates, reserve and settlement, and follows the claim to its closure.",
        "BDOI's FRS adds a claims reporting form filled by Marketing, an approval of follow-ups by the Team Leader "
        "and Team Head, dashboards per role, masters per risk code, document checklists, report layouts and an "
        "insurer scorecard.",
        "BIBS posts no accounting entry for a claim: the reserve and settlement are the insurer's figures, kept for "
        "monitoring and loss experience.",
    ],
    "in_scope": [
        "Dashboards, follow-up list and quick view; access by role and hierarchy.",
        "Claims masters: statuses, settlement types, adjusters, claim types, categories, severity, documents, "
        "letters, repair shops, follow-up types, nature of claim, catastrophe codes.",
        "Claims reporting form, claim record, premium check, locations, insurers and claim numbers, loss advice.",
        "Follow-up, approval, diary, reserve, settlement, closure and reopen.",
        "Claims reports, KRA and insurer scorecard; notices and jobs.",
    ],
    "out_scope": [
        "Users, sign-in and group-profile membership: User Access Maintenance (BRD-11) with UIDM-ISC and Entra ID.",
        "Claim money received or paid by BDOI and its accounting: not in the BRD (CLQ10).",
        "Reinsurance claims: Phase 2 with the reinsurance module.",
        "A client or insurer portal: not in the BRD; insurer communications by e-mail and upload.",
        "Load of open and historical claims at go-live: Data Migration (BRD-13).",
    ],
}

DOC_SET = [
    ("FRS (this document)", "Requirements, rules, validations, personas, permissions, settings, statuses, change "
     "control, review of BDOI's FRS."),
    (f"Process and Storyboards (BPS) v{VERSION}", "End-to-end journey, to-be processes and storyboards."),
    (f"UX Design (UXD) v{VERSION}", "Menu by persona and every Claims screen: fields, validations, actions, states."),
    (f"Reports, Notifications and Outputs (RNO) v{VERSION}", "Reports, letters, exports, notices, alerts, jobs."),
    (f"Interface Specification (ICD) v{ICD_VERSION}", "Loss advice to the insurer, insurer updates by upload, "
     "documents, module hand-offs, legacy claims load."),
    ("Developer Specification (DSP) v1.0", "Screens, service operations, field checks, rule enforcement, states, "
     "jobs, audit records, permissions."),
    ("Data Dictionary (DDI) v1.0", "Tables, columns, lists of values, parameters, status models, indexes and the "
     "lists of BDOI's annexes."),
    ("User Stories (USR) v1.0", "Epics and stories with acceptance criteria, ready for the backlog."),
    (f"Workbooks v{WB_VERSION}", "RTM and test cases, Fit-Gap, CR Register and Effort Estimate, Requirements "
     "Collection, Walkthrough Users."),
    ("Programme documents", "Non-Functional and Security Requirements, Glossary, Decision and Observation Log, "
     "Document Register (Programme/Common)."),
]

REFERENCES = [
    "Business Requirements Document - Claims (CLM), with the renumbering addendum of 17-Dec-2025 and the workshop "
    "addendum of 8-Apr-2026 (BRCLM.001 to 043) (version of 8-Oct-2026).",
    "FRS - BDOI BROKERSYS Claims v1.0 (BDOI), template D051, 13-Aug-2026, with annexes A to AC.",
    "Annex references named by BDOI's FRS and not received: Claims Core FRS Data Mapping, Data Migration - Data "
    "Mapping v1.0, Motor Claims Process Flow, Insurers Directory, Claims KRA.",
    "BDOI drop plan of 26-Sep-2026: Claims in Drop 2.",
    "BIBS FRS User Access Maintenance v1.6 (BRD-11) and Operations Remittance v1.0 (BRD-02).",
    f"BIBS Interface Specification: Claims v{ICD_VERSION}.",
    "BIBS BDOI Feature List vs OOTB and Best Practice v1.0 (baseline of the colour code).",
]


def R(rid, text, colour, label, rule, fit, today, acs):
    return {"id": rid, "text": " ".join(text.split()), "colour": colour, "label": label, "rule": rule, "fit": fit,
            "today": today, "ac": acs}


def E(eid, component, capability, brd, status, story, reqs):
    return {"id": eid, "component": component, "capability": capability, "brd_ids": brd,
            "brd": ", ".join(brd) or "Not in the BRD", "status": status, "story": story, "reqs": reqs}


BDOI = "BDOI FRS"
NEW = "Completed v1.0"
CR = "Change request"

FRUM = [
    E("FRCLM.001", "Dashboard", "Dashboard per role", ["BRCLM.034", "BRCLM.025"], BDOI,
      ("H", "a dashboard of my claims, follow-ups and ageing", "I see what to do first", "SCR-CLM-01", "High", 8,
       [("three of my claims due for follow-up today", "I open Claims Home", "the tile Due today shows 3"),
        ("a Team Leader", "I open Claims Home", "I see the totals of my team and the approvals waiting")]), [
        R("FRCLM.001.01", "The Claims Handler dashboard shows the outstanding claims, claims due for follow-up today, "
          "claims past due for follow-up, the claims ageing trend, claims aged more than 30, 60 and 90 days and the "
          "upcoming action items.", "k", "", "BR-CLM-08", "Partial",
          "Claims Home shows open claims, follow-ups due and overdue, diary due, ageing buckets and claims by status; "
          "the ageing trend is delivery work.", ["A handler with 2 claims aged 45 days sees 2 in the bracket more "
                                                 "than 30 days."]),
        R("FRCLM.001.02", "The handler dashboard also shows the claims past their SLA or TAT (Annex AB) and the claims "
          "per grouping: assured, insurer, Marketing, Motor and Non-Motor.", "o", "", "CR-CLM-103", CR,
          "Not offered; change request CR-CLM-103.", ["A claim past the insurer's approval TAT is counted in Past due "
                                                     "SLA/TAT."]),
        R("FRCLM.001.03", "The Claims Team Leader dashboard shows the team: outstanding claims, claims per handler, "
          "ageing, settlements, lead measures LM1 to LM3, past due SLA/TAT, claims by product line, grouping and "
          "status, workload distribution and the items pending approval (CRF and FLA, follow-ups, settlements, "
          "record corrections, temporary closures, settlement checks, reopen, denial).", "o", "", "CR-CLM-103", CR,
          "Not offered; change request CR-CLM-103 (approvals with CR-CLM-102).",
          ["A Team Leader of 4 handlers sees the claims of each handler and the approvals waiting."]),
        R("FRCLM.001.04", "The Claims Team Head dashboard shows the whole portfolio (Head Office and branches), per "
          "group and handler, with the same measures and the items escalated for final approval.", "o", "",
          "CR-CLM-103", CR, "Not offered; change request CR-CLM-103.",
          ["The Team Head sees the claims of every group and the Head Office and branch totals."]),
    ]),
    E("FRCLM.002", "User access profiles", "Access of each profile",
      ["BRCLM.002", "BRCLM.003", "BRCLM.016", "BRCLM.033", "BRCLM.040"], BDOI,
      ("U", "each claims profile to have its own access", "claims data is seen and changed only by the right people",
       "SCR-CLM-09", "High", 5,
       [("a Marketing Account Officer", "I open the Claims Reports", "I see the loss reports of the accounts"),
        ("a handler group with its Team Lead", "a follow-up needs approval", "it goes to that Team Lead")]), [
        R("FRCLM.002.01", "The Claims Handler, Claims Team Lead and Claims Team Head each have a group profile whose "
          "permissions are those of section 6; access follows the User Access Maintenance FRS (Annex E).", "k", "",
          "BR-CLM-01", "Fit", "Claims Officer, Team Lead, Team Head, Unit Head and Risk profiles with their claims "
          "permissions.", ["A Claims Handler can record a claim and cannot close it permanently."]),
        R("FRCLM.002.02", "The Marketing unit views the claim record and its status, and the loss information of its "
          "accounts.", "k", "", "BR-CLM-01", "Partial",
          "Marketing reads the claims reports and the Claims tab of the account; read access to the claim record is "
          "delivery work.", ["A Marketing Account Officer opens the claim of his account read-only."]),
        R("FRCLM.002.03", "The Marketing unit creates and updates a Claims Reporting Form for the handler's review "
          "(FRCLM.008.01).", "o", "", "CR-CLM-101", CR, "Not offered; change request CR-CLM-101.",
          ["A Marketing Account Officer saves a CRF as draft and submits it."]),
        R("FRCLM.002.04", "The Marketing unit extracts the records tagged Total Loss and runs the Loss Ratio Report.",
          "k", "", "-", "Partial", "The Loss Ratio Report is offered to Marketing; the Total Loss tag is part of "
          "CR-CLM-108.", ["A Marketing Team Leader exports the Loss Ratio Report of his accounts."]),
        R("FRCLM.002.05", "Users and their group-profile membership come from UIDM-ISC and every user signs in with "
          "Microsoft Entra ID; the MBS Administrative Unit keeps the permissions of the claims group profiles in BIBS "
          "under maker-checker (BRD-11).", "k", "", "BR-CLM-01", "Fit",
          "Group-profile permissions under maker-checker; users received from UIDM-ISC.",
          ["A permission added to the Claims Team Lead profile applies after another administrator approves it."]),
        R("FRCLM.002.06", "The Claims Handler Group follows the hierarchy: each handler has a supervising Team Lead "
          "and Team Head, used for the visibility of records, the approvals and the escalations (Annex G).", "k", "",
          "BR-CLM-04", "Partial", "Claims handler register with unit and team kept by the Unit Head; the "
          "supervising Team Lead and Team Head and their use for approvals are delivery work with CR-CLM-102 and "
          "CR-CLM-104.", ["The handler register shows each handler with his Team Lead and Team Head."]),
    ]),
    E("FRCLM.003", "Maintenance", "Claim status list and workflow",
      ["BRCLM.010", "BRCLM.011", "BRCLM.012", "BRCLM.013", "BRCLM.014", "BRCLM.015", "BRCLM.035"], BDOI,
      ("U", "to keep the claim statuses with the roles allowed to set them", "statuses mean the same for everybody",
       "SCR-CLM-09", "High", 5,
       [("the status list", "a handler opens Change Status", "only the statuses of his role and unit show, in "
         "alphabetical order"), ("a status that needs approval", "a handler sets it", "the change waits for the Team "
         "Leader")]), [
        R("FRCLM.003.01", "Claim status maintenance is restricted to the users authorised in User Access "
          "Maintenance.", "k", "", "BR-CLM-01", "Fit", "Claims Setup with the setup permission, maker-checker.",
          ["A Claims Handler does not see Claims Setup."]),
        R("FRCLM.003.02", "The claim statuses of Annex F are pre-configured with their status type (New PLA, Follow "
          "Up, Settlement, Settlement - Paid, Settlement - LOA Issued) and shown in alphabetical order on every "
          "screen.", "k", "", "BR-CLM-05", "Partial",
          "18 statuses and 11 settlement types with their phases are kept as lists; Annex F mixes them with Open and "
          "Closed and adds status types, and the alphabetical order is delivery work (Q-CL-04).",
          ["The status list of a claim shows the statuses of Annex F in alphabetical order."]),
        R("FRCLM.003.03", "A status can be selected only by the roles and units allowed for it.", "k", "",
          "BR-CLM-05", "Fit", "Status access matrix per role and unit.",
          ["A handler of the Motor unit cannot set Closed - Denied."]),
        R("FRCLM.003.04", "A change to the statuses marked in Annex F needs the approval of the Team Leader and, where "
          "Annex F says so, of the Team Head (FRCLM.010).", "o", "", "CR-CLM-102", CR,
          "Not offered: the status is set at once by the allowed roles; change request CR-CLM-102.",
          ["A handler setting Closed - within Deductible sends it to the Team Leader, then the Team Head."]),
    ]),
    E("FRCLM.004", "Maintenance", "Adjusters and appraisers", ["BRCLM.017", "BRCLM.018"], BDOI,
      ("U", "to keep the adjusters and appraisers with their contacts", "handlers reach them quickly", "SCR-CLM-09",
       "Medium", 3, [("a new adjuster", "I add it with its contact", "it is selectable on the claims")]), [
        R("FRCLM.004.01", "Authorised users create, view, update, activate and deactivate Adjuster and Appraiser "
          "records; the 25 companies of the BRD are pre-configured and shown in alphabetical order.", "k", "",
          "BR-CLM-05", "Fit", "List of 25 adjusters, maker-checker.",
          ["A deactivated adjuster is no longer offered on a claim."]),
        R("FRCLM.004.02", "Each adjuster record keeps the contact name, phone number, e-mail address and remarks "
          "(Annex H).", "o", "", "CR-CLM-106", CR, "The list holds the name only; change request CR-CLM-106.",
          ["The adjuster record shows its contact person and e-mail."]),
        R("FRCLM.004.03", "Only users with the permission set or change the adjuster of a claim.", "k", "",
          "BR-CLM-01", "Fit", "Assign adjuster with its permission.",
          ["A handler without the permission does not see Assign Adjuster."]),
    ]),
    E("FRCLM.005", "Maintenance", "Claim types, claim categories and claim severity", [], BDOI,
      ("U", "claim types, categories and severities by risk code", "claims are classified the same way",
       "SCR-CLM-12", "Medium", 5,
       [("a Motor policy", "the handler records a claim", "only the Motor claim types and categories are offered")]), [
        R("FRCLM.005.01", "Claim Type is a maintained list with the values of Annex M (codes of the core system and "
          "types of claim), created, updated, activated and deactivated by authorised users.", "k", "", "BR-CLM-05",
          "Partial", "Claim type list with 10 provisional values; the values of Annex M are to load (CLQ12).",
          ["The claim type list holds the values of Annex M."]),
        R("FRCLM.005.02", "During claim creation only the claim types that apply to the risk code of the policy are "
          "offered.", "o", "", "CR-CLM-105", CR, "Not offered; change request CR-CLM-105.",
          ["A Fire policy does not offer the Motor claim types."]),
        R("FRCLM.005.03", "Claim Category is a maintained list (code, type of claim, nature of claim category, risk "
          "code category, status) without duplicate codes; only the categories of the selected type and risk code "
          "category are offered, in alphabetical order; inactive categories are not offered (Annex L).", "o", "",
          "CR-CLM-105", CR, "Not offered; change request CR-CLM-105.",
          ["Saving a second category with the same code is refused."]),
        R("FRCLM.005.04", "Claim Severity is a maintained list with the values of Annex K.", "o", "", "CR-CLM-105", CR,
          "Not offered; change request CR-CLM-105.", ["The severity list offers Total Loss and Minor Damage."]),
    ]),
    E("FRCLM.006", "Maintenance", "Standard claim documents per risk code category", [], BDOI,
      ("H", "the documents required for each claim type to be listed and checked", "the insurer receives a complete "
       "file", "SCR-CLM-04", "Medium", 5,
       [("a Motor claim", "I open Documents", "the required documents of Annex J for Motor are listed"),
        ("a required document missing", "I submit the claim to the insurer", "I am told which document is missing")]), [
        R("FRCLM.006.01", "Authorised users keep the standard document master list and the documents required per "
          "risk code category (Annex J).", "g", "", "-", "Gap",
          "Document types on the claim; the checklist per risk code is offered by the OOTB platform and is to "
          "enable for BIBS (G-CLM-01).", ["The Motor category requires the Police Report and the Driver's "
                                          "Affidavit."]),
        R("FRCLM.006.02", "The claim proceeds only when the required documents are uploaded.", "g", "", "BR-CLM-09",
          "Gap", "To enable with the checklist (G-CLM-01).",
          ["A claim without the Police Report cannot move to Newly Filed Claim - with complete documents."]),
        R("FRCLM.006.03", "Documents are uploaded on the claim as images (.jpeg, .png, .heic) or PDF (.pdf).", "k", "",
          "-", "Fit", "Attachments accept JPEG, PNG, HEIC and PDF.", ["A .heic photo is accepted; a .exe file is "
                                                                    "refused."]),
    ]),
    E("FRCLM.007", "Maintenance", "Letters, repair shops and other lists", ["BRCLM.036"], BDOI,
      ("U", "to keep the loss advice letter and the claims lists", "letters and lists follow BDOI's standards",
       "SCR-CLM-12", "Medium", 8,
       [("a new letter version effective 1-Jan", "a letter is generated on 2-Jan", "the new version is used")]), [
        R("FRCLM.007.01", "Authorised users keep the Formal Loss Advice letter templates (header, logo, body, "
          "recipient, signatories, footer) with versions and effectivity dates; the letter is generated separately "
          "or consolidated for the insurers of a claim with the version effective on its date.", "o", "",
          "CR-CLM-107", CR, "One loss advice template per insurer line; template maintenance with versions, "
          "effectivity and the consolidated letter is change request CR-CLM-107.",
          ["A letter generated after the effective date of version 2 uses version 2."]),
        R("FRCLM.007.02", "Authorised users keep the Accredited Repair Shops (code, name, contact, phone, fax), "
          "offered as Preferred Shop on Motor claims (Annex I).", "g", "", "-", "Gap",
          "Offered by the OOTB platform (motor repairs), to enable for BIBS (G-CLM-02).",
          ["A Motor claim offers the active repair shops as Preferred Shop."]),
        R("FRCLM.007.03", "Follow-up Type is a maintained list with the values of Annex N.", "g", "", "-", "Fit",
          "A maintained list; the values of Annex N are to load (G-CLM-03).",
          ["The follow-up types include Appoint Assessor."]),
        R("FRCLM.007.04", "Nature of Claim is a maintained list with the values of Annex O.", "k", "", "-", "Fit",
          "Nature of loss list; the values of Annex O are to load.", ["The list includes Own Loss and Damage."]),
        R("FRCLM.007.05", "Catastrophe codes are a maintained list: Typhoon, Earthquake, Flood, Volcanic Eruption, "
          "Landslide, Fire, Others.", "k", "", "-", "Fit", "Catastrophe list of 7 values.",
          ["A claim tagged Typhoon appears under Typhoon in the claims-prone report."]),
    ]),
    E("FRCLM.008", "Claim record", "Claims reporting form and claim record",
      ["BRCLM.004", "BRCLM.006", "BRCLM.007", "BRCLM.008", "BRCLM.009", "BRCLM.037", "BRCLM.039", "BRCLM.043"], BDOI,
      ("H", "to complete the claim from the reporting form and send the loss advice", "the insurer is told of the loss "
       "with complete data", "SCR-CLM-03", "Highest", 13,
       [("a fully paid policy", "I record the claim", "it gets its claim number and the loss advice can be sent"),
        ("an unpaid policy", "I record the claim", "it is flagged unpaid and the authorization code stays "
         "disabled")]), [
        R("FRCLM.008.01", "A Marketing Account Officer creates a Claims Reporting Form with the fields of claim "
          "logging, saves it as draft with a CRF reference number and submits it; the handler accepts it, which "
          "creates the claim with its own claim number, or returns it.", "o", "", "CR-CLM-101", CR,
          "Not offered: claims are recorded by the claims users; change request CR-CLM-101.",
          ["An accepted CRF becomes a claim with a claim number different from the CRF reference."]),
        R("FRCLM.008.02", "The handler completes the mandatory claim fields of Annex P for the risk code of the "
          "policy and initiates the formal loss advice to the insurers.", "k", "", "BR-CLM-02", "Partial",
          "Claim recording with cover, loss, claimant, locations, insurers and loss advice; the field set of Annex "
          "P is delivery work, its risk-code blocks change request CR-CLM-108.",
          ["Saving a claim without the claimant's name is refused."]),
        R("FRCLM.008.03", "On retrieval of the policy the claim shows the payment status, the insurer and co-insurers, "
          "the account officer, the policy currency and the policy location; the latest cover version applies.",
          "k", "", "BR-CLM-03", "Fit", "Cover card with premium check, insurers, officer, currency, locations and "
          "newer version flag.", ["A policy with an endorsement after the claim shows Newer cover version."]),
        R("FRCLM.008.04", "A fully paid policy proceeds; a partially paid or unpaid policy is flagged, the "
          "authorization code stays disabled and the form is returned to the Marketing Account Officer for "
          "collection.", "k", "", "BR-CLM-03", "Partial",
          "The claim is saved, flagged unpaid and alerted, and the code is disabled; the return to the Account "
          "Officer is delivery work (Q-CL-02).", ["A claim on an unpaid policy shows Unpaid premium and an alert."]),
        R("FRCLM.008.05", "The claim currency defaults to the policy currency, else Philippine Peso.", "k", "",
          "BCL_DEFAULT_CURRENCY", "Fit", "Currency of the cover, default PHP.",
          ["A USD policy gives a USD claim."]),
        R("FRCLM.008.06", "The claim location inherits the policy locations; the handler may add more locations of "
          "the policy.", "k", "", "-", "Fit", "Locations picked from the cover.",
          ["A location that is not on the policy cannot be added."]),
        R("FRCLM.008.07", "One claim incident (mother record) holds the claim numbers of several insurers, each linked "
          "to its insurer, without duplicates for an insurer; the incident is found by any of its insurer claim "
          "numbers; every change is audited and every number appears in reports.", "k", "", "BR-CLM-06", "Fit",
          "Insurer lines with their claim numbers, unique per insurer, searchable and reported.",
          ["Searching an insurer claim number opens its incident."]),
        R("FRCLM.008.08", "The Formal Loss Advice is generated in PDF from the template version in force, filled with "
          "the claim, policy and insurer data, and can be previewed, downloaded, saved and sent to the insurer's "
          "point of contact.", "k", "", "-", "Partial", "Loss advice PDF per insurer, e-mailed and stored on the "
          "claim; preview before sending and the template versions (CR-CLM-107) are delivery work.",
          ["The handler previews the letter, sends it and finds it under Documents."]),
        R("FRCLM.008.09", "The reported date is kept and may be corrected only by permitted users; the claimant "
          "defaults to the assured and may be overridden with the permission; the policy number comes from the "
          "policy record.", "k", "", "BR-CLM-01", "Fit", "Reported date, claimant override and policy number.",
          ["A handler without the permission cannot change the claimant."]),
        R("FRCLM.008.10", "The risk-code blocks of Annex P are captured: Motor details (assignee, driver, vehicle, "
          "plate, coverages, deductible, preferred shop, damage assignments), Health (hospital from a maintained "
          "list, confinement, physician), Mortgage Redemption (cause of death, certifier), and the Total Loss tag.",
          "o", "", "CR-CLM-108", CR, "Not offered beyond the loss details; change request CR-CLM-108.",
          ["A Motor claim requires the plate number; a Health claim offers the hospitals of the list."]),
    ]),
    E("FRCLM.009", "Claim follow-up", "Follow-up list, quick view and follow-up processing",
      ["BRCLM.019", "BRCLM.020", "BRCLM.021", "BRCLM.022", "BRCLM.023", "BRCLM.024", "BRCLM.027", "BRCLM.034"], BDOI,
      ("H", "a follow-up list with the ages and a quick view of each claim", "I follow up every claim on time",
       "SCR-CLM-02", "Highest", 13,
       [("my claims", "I open the worklist", "each shows its last follow-up, its ages and the next follow-up date"),
        ("a follow-up recorded", "I save it", "the next follow-up date is set and the action plan kept")]), [
        R("FRCLM.009.01", "The follow-up list shows claim number, claimant, follow-up count, last follow-up date, "
          "aging since last follow-up, aging in current status, overall aging, plate number, client and handler, "
          "with filtering, sorting and searching.", "k", "", "BR-CLM-08", "Partial",
          "Worklist with claim, claimant, status, ages, next follow-up and search; follow-up count, plate number and "
          "aging since last follow-up are delivery work.",
          ["Sorting the list on overall aging puts the oldest claim first."]),
        R("FRCLM.009.02", "A handler sees only the claims assigned to him, a Team Leader those of his team, a Team "
          "Head every claim.", "o", "", "CR-CLM-104", CR,
          "Claims users see every claim of the company; change request CR-CLM-104 (Q-CL-07).",
          ["A handler does not find the claim of another team in the list."]),
        R("FRCLM.009.03", "Selecting a claim shows a quick view: claim type, assured, policy and invoice, insurer, "
          "account officer, risk, inception and expiry, claim amount, handler, loss and reported dates, the last "
          "approved activity and the activity pending approval.", "k", "", "-", "Partial",
          "Claim summary card and timeline; the activity pending approval comes with CR-CLM-102.",
          ["The quick view shows the last approved follow-up with its date."]),
        R("FRCLM.009.04", "The handler records a follow-up with the fields of Annex Q: action taken today, next action "
          "plan summary and detail, settlement amount, adjuster's evaluation, insurer's deductible, status as of the "
          "date and the turnaround dates.", "k", "", "BR-CLM-07", "Partial",
          "Status change, action plan, diary, reserve and settlement; the evaluation and deductible amounts and the "
          "turnaround dates (CR-CLM-109) are delivery work.",
          ["A follow-up saved with status For Insurer's Issuance of Check keeps its action taken and next plan."]),
        R("FRCLM.009.05", "The next follow-up date is set from the status and may be overridden, with a reason, only by "
          "permitted users.", "k", "", "BR-CLM-07", "Fit", "Follow-up days per status; override with reason.",
          ["A Team Leader overrides the next follow-up date to Friday with a reason."]),
        R("FRCLM.009.06", "Each handler keeps a diary of calls, e-mails, meetings and follow-ups per claim, with due "
          "dates and reminders.", "k", "", "-", "Fit", "Diary per claim and My Diary, daily reminders.",
          ["A diary entry due today appears in My Diary and in the morning notice."]),
        R("FRCLM.009.07", "The insurer reserve is recorded per insurer and amended only by permitted users, with its "
          "history.", "k", "", "BR-CLM-01", "Fit", "Reserve per insurer line with history.",
          ["A reserve amendment shows the old and new amounts with user and date."]),
        R("FRCLM.009.08", "The filtered and grouped list is exported to Excel and PDF as shown.", "g", "", "-", "Gap",
          "Exports are offered on the reports; the export of the worklist is offered by the OOTB platform "
          "(G-CLM-04).", ["The exported file holds the rows and order of the screen."]),
    ]),
    E("FRCLM.010", "Claim follow-up", "Follow-up approval and validation", [], BDOI,
      ("T", "to approve or return the follow-ups that need my approval", "status changes are validated", "SCR-CLM-11",
       "High", 8,
       [("a follow-up with status Closed - Denied", "I approve it", "it goes to the Team Head"),
        ("a follow-up I deny with a comment", "I save", "it returns to the handler with my comment")]), [
        R("FRCLM.010.01", "Follow-ups whose status needs approval (Annex F) go to the Team Leader, who approves and "
          "forwards them or denies them with comments; the Team Head gives the final approval or returns them to the "
          "Team Leader; status, approver, comments and audit trail are kept.", "o", "", "CR-CLM-102", CR,
          "Not offered; change request CR-CLM-102.", ["A denied follow-up shows the comment to the handler."]),
        R("FRCLM.010.02", "Pending approvals show on the dashboards of the approvers with direct access to the "
          "follow-up.", "o", "", "CR-CLM-102", CR, "Not offered; change request CR-CLM-102.",
          ["The Team Leader opens the pending follow-up from the dashboard tile."]),
    ]),
    E("FRCLM.011", "Reports", "Outstanding Claims report", ["BRCLM.031", "BRCLM.026"], BDOI,
      ("R", "the Outstanding Claims report in BDOI's layout", "management sees the open claims by insurer or type",
       "SCR-CLM-08", "High", 3,
       [("the as-of date 30-Sep", "I run it grouped by insurer, detailed", "the claims are listed per insurer with "
         "the BDOI heading")]), [
        R("FRCLM.011.01", "On demand, in Excel and PDF, with the BDO Insure heading: company name, title 'Aging of "
          "Outstanding Claims by <grouping>', as-of date, run date, run time and page N of N.", "k", "", "BR-CLM-10",
          "Partial", "Outstanding claims report in Excel, PDF and CSV; the BDOI heading is delivery work.",
          ["The PDF shows 'Aging of Outstanding Claims by Insurer' and Page 1 of 2."]),
        R("FRCLM.011.02", "Filters (cut-off date, account officer, claim type, insurer, status, handler, department, "
          "claim amount range, assured, origin, market category, branch, account type, risk code, segment, policy, "
          "age, catastrophe), detailed or summary presentation and grouping by insurer or claim type, with the "
          "columns of Annex U.", "k", "", "BR-CLM-10", "Partial", "Filters and columns of the BRD; the full filter "
          "set and the summary presentation are delivery work.",
          ["The summary by claim type shows one line per claim type with counts and amounts."]),
    ]),
    E("FRCLM.012", "Reports", "Settled Claims report", ["BRCLM.029"], BDOI,
      ("R", "the Settled Claims report in BDOI's layout", "management sees the settlements of a period",
       "SCR-CLM-08", "Medium", 2,
       [("July", "I run it grouped by risk code", "the claims settled in July are listed per risk code")]), [
        R("FRCLM.012.01", "On demand, Excel and PDF, BDO Insure heading with title 'List of Various Settled Claims by "
          "<grouping>' and the date range.", "k", "", "BR-CLM-10", "Partial", "Settled claims report; heading is "
          "delivery work.", ["The heading shows the date range From - To."]),
        R("FRCLM.012.02", "Filters of FRCLM.011.02 on the settled date range, detailed or summary, grouping by insurer "
          "or risk type, columns of Annex V.", "k", "", "BR-CLM-10", "Partial", "Date range filter; grouping and "
          "summary are delivery work.", ["Grouping by insurer gives a subtotal per insurer."]),
    ]),
    E("FRCLM.013", "Reports", "Outstanding Claims N days past due", ["BRCLM.031"], BDOI,
      ("R", "the claims past due report", "old claims are pursued", "SCR-CLM-08", "Medium", 2,
       [("N = 90", "I run the report", "only the claims aged more than 90 days are listed")]), [
        R("FRCLM.013.01", "On demand, Excel and PDF, BDO Insure heading, listing the outstanding claims older than N "
          "days from the reported date.", "k", "", "BCL_PAST_DUE_DAYS", "Partial", "Past due report with N "
          "(default 90); heading is delivery work.", ["N = 60 lists the claims aged more than 60 days."]),
        R("FRCLM.013.02", "Filters of FRCLM.011.02, detailed or summary, grouping by insurer or claim type, columns of "
          "Annex W.", "k", "", "BR-CLM-10", "Partial", "Grouping and summary are delivery work.",
          ["The detailed list is grouped by insurer."]),
    ]),
    E("FRCLM.014", "Reports", "Claims Aging per status", ["BRCLM.026", "BRCLM.027", "BRCLM.028"], BDOI,
      ("R", "the ageing report per status", "bottlenecks show", "SCR-CLM-08", "Medium", 2,
       [("the as-of date", "I run it", "each status shows its claims with the age in the status and overall")]), [
        R("FRCLM.014.01", "On demand, Excel and PDF, heading 'Aging of Outstanding Claims (Current Status) by "
          "<grouping>', with the age in the current status and the overall age.", "k", "", "BR-CLM-08", "Partial",
          "Ageing per status report; heading is delivery work.", ["A claim 12 days in its status shows 12."]),
        R("FRCLM.014.02", "Filters of FRCLM.011.02, detailed or summary, grouping by insurer or claim type, columns of "
          "Annex X.", "k", "", "BR-CLM-10", "Partial", "Grouping and summary are delivery work.",
          ["The summary shows one line per status and insurer."]),
    ]),
    E("FRCLM.015", "Reports", "Loss Experience report", ["BRCLM.030", "BRCLM.040"], BDOI,
      ("R", "the loss experience report with my own title", "accounts are discussed on facts", "SCR-CLM-08", "Medium",
       3, [("a title and a loss date range", "I run it", "the report carries my title and the paid, outstanding and "
            "total per claim")]), [
        R("FRCLM.015.01", "On demand, Excel and PDF, with a title entered by the user and the period of the date of "
          "loss.", "k", "", "BR-CLM-10", "Partial", "Loss experience report; the user's title is delivery work.",
          ["The heading shows the title entered."]),
        R("FRCLM.015.02", "Filters (account officer, insurer, handler, branch, policy, age, claim type, invoice, "
          "status, amount, assured, claimant, loss ratio), detailed or summary, and the columns of Annex Y (insured, "
          "claimant, policy, basic premium, loss date, nature and type of loss, gross loss, deductible, amount of "
          "loss, paid, outstanding, total, insurer, status).", "k", "", "BR-CLM-10", "Partial",
          "Paid, outstanding and total per claim and insurer line; the full filter set is delivery work.",
          ["Outstanding = reserve - paid while the claim is open."]),
    ]),
    E("FRCLM.016", "Reports", "Loss Ratio report", ["BRCLM.032"], BDOI,
      ("R", "the loss ratio report", "renewal and pricing use the losses", "SCR-CLM-08", "Medium", 3,
       [("a client with premium 100,000.00 and losses 40,000.00", "I run it", "the loss ratio is 40%")]), [
        R("FRCLM.016.01", "On demand, Excel and PDF, user title and period, with the loss ratio of losses over "
          "premium.", "k", "", "BR-CLM-11", "Partial", "Loss ratio report on the signed gross premium; the user's "
          "title and the premium basis (CLQ20) are to confirm.", ["The ratio shows with two decimals and %."]),
        R("FRCLM.016.02", "Filters and columns as FRCLM.015.02 with the premium and the ratio (Annex Z).", "k", "",
          "BR-CLM-10", "Partial", "Grouping by client, line or insurer; the full filter set is delivery work.",
          ["Grouping by insurer gives the ratio per insurer."]),
    ]),
    E("FRCLM.017", "Reports", "Claims-prone locations and Total Loss", ["BRCLM.038"], BDOI,
      ("R", "to see the locations with frequent claims and the total losses", "risks are analysed", "SCR-CLM-08",
       "Medium", 3, [("three claims at one barangay in three years", "I run the report", "the location is flagged "
                      "claims-prone")]), [
        R("FRCLM.017.01", "On demand, Excel and PDF, title 'Claims Prone Locations and Total Loss tagging', with "
          "location filters down to province, city or municipality and barangay, several locations at once.", "k",
          "", "BCL_PRONE_MIN_CLAIMS", "Partial", "Claims-prone report per location key, city and province with the "
          "claims-prone flag; the barangay level and several locations at once are delivery work.",
          ["Filtering two cities lists the claims of both."]),
        R("FRCLM.017.02", "The report shows the claims tagged Total Loss.", "o", "", "CR-CLM-108", CR,
          "The Total Loss tag comes with change request CR-CLM-108.", ["A claim tagged Total Loss is marked."]),
    ]),
    E("FRCLM.018", "Reports", "Sustainability report, claims input", [], BDOI,
      ("R", "the claims input of the sustainability report", "BDOI reports on climate-related losses", "SCR-CLM-08",
       "Low", 2, [("the as-of date", "I run it", "the claims with their location and catastrophe code are listed")]), [
        R("FRCLM.018.01", "On demand, Excel and PDF, title 'Sustainability Report', with the filters of FRCLM.017.01 "
          "and the columns BDOI gives (content to confirm, Q-CL-12).", "o", "", "CR-CLM-111", CR,
          "Not offered; change request CR-CLM-111.", ["The report lists the claims by location and catastrophe."]),
    ]),
    E("FRCLM.019", "Reports", "Claims KRA report", [], BDOI,
      ("E", "the KRA report of the claims staff", "performance is measured the same way", "SCR-CLM-08", "Low", 3,
       [("a period and a Claims Officer", "I run the KRA", "the KRA criteria of Annex AC are scored")]), [
        R("FRCLM.019.01", "The KRA report scores the Claims Officer, Team Leader and Team Head, and Unit Head on the "
          "criteria of Annex AC for a period and a person.", "o", "", "CR-CLM-110", CR,
          "Not offered; change request CR-CLM-110 (Annex AC not received, Q-CL-13).",
          ["The KRA of an officer for Q3 shows each criterion with its score."]),
    ]),
    E("FRCLM.020", "Reports", "Insurers' Scorecard", [], BDOI,
      ("E", "an insurer scorecard on turnaround times", "insurers are compared on service", "SCR-CLM-13", "Medium",
       5, [("an insurer approving 96% of claims within TAT", "the scorecard runs", "its approval TAT rating is A")]), [
        R("FRCLM.020.01", "The scorecard rates each insurer from A to F on two turnaround times: claim evaluation (working "
          "days from the adjuster's report or complete documents to the insurer's approval) and check issuance "
          "(working days from the accepted offer to BDOI's receipt of the check), against the targets of Annex AB; "
          "A 95% and more, B 90 to 94%, C 85 to 89%, D 80 to 84%, F below 80%.", "o", "", "CR-CLM-109", CR,
          "Not offered; change request CR-CLM-109.", ["An insurer at 83% within TAT is rated D."]),
        R("FRCLM.020.02", "The scorecard also shows the acceptance rate (accepted claims over all claims) and the "
          "average evaluation and check issuance TAT per insurer.", "o", "", "CR-CLM-109", CR,
          "Not offered; change request CR-CLM-109.", ["8 accepted of 10 claims gives an acceptance rate of 80%."]),
    ]),
    E("FRCLM.021", "Claim record", "Premium check and claims authorization code", ["BRCLM.001"], NEW,
      ("H", "the premium of the cover checked on every claim", "no claim proceeds on unpaid premium", "SCR-CLM-04",
       "Highest", 5, [("an unpaid invoice", "the premium is paid", "the claim flag clears and the code can be "
                       "generated")]), [
        R("FRCLM.021.01", "The unpaid or partly paid invoices of the policy are identified and shown on the claim; a "
          "payment clears the flag.", "k", "", "BR-CLM-03", "Fit", "Premium check on recording, on each payment and "
          "daily.", ["The claim of an unpaid cover shows the unpaid invoices."]),
        R("FRCLM.021.02", "The Claims Authorization Code is disabled for unpaid invoices or policies.", "k", "",
          "BR-CLM-03", "Fit", "Code generated only on a paid cover (Q-CL-01: BDOI's FRS calls it an EBIX function).",
          ["Generate Authorization Code is refused on an unpaid cover."]),
    ]),
    E("FRCLM.022", "Claim record", "Insurer communications and location references", ["BRCLM.041", "BRCLM.042"], NEW,
      ("H", "to record what the insurers report and their location references", "insurer communications are "
       "auditable", "SCR-CLM-04", "High", 5,
       [("an e-mail from the insurer", "I record the update", "it is in the claim history with date, source and "
         "remarks")]), [
        R("FRCLM.022.01", "Claims reported or updated by insurers are recorded with date, source, reference, remarks "
          "and attachments, one by one or by upload.", "k", "", "BR-CLM-06", "Fit",
          "Insurer updates on the claim and by upload.", ["An uploaded file of 20 updates adds 20 history lines."]),
        R("FRCLM.022.02", "Each insured location keeps the reference the insurer uses, with its effective date, shown "
          "on the claim.", "k", "", "-", "Fit", "Insurer location references, one open at a time.",
          ["A new reference end-dates the previous one."]),
    ]),
    E("FRCLM.023", "Settlement", "Settlement, closure and reopen",
      ["BRCLM.005", "BRCLM.014", "BRCLM.015", "BRCLM.035"], NEW,
      ("T", "to record the settlement and close or reopen claims", "the claim ends with the right outcome",
       "SCR-CLM-04", "High", 5,
       [("a settlement type that closes the claim", "I set it with the amount and date", "the claim is closed "
         "permanently"), ("a closed claim", "the Team Head reopens it with a reason", "it is In Progress again")]), [
        R("FRCLM.023.01", "The requested type of settlement is set with the permission; a settled type needs the "
          "amount and date settled.", "k", "", "BR-CLM-05", "Fit", "Set Settlement with its rules.",
          ["Settled - LOA Issued without an amount is refused."]),
        R("FRCLM.023.02", "Closure is tagged temporary or permanent; permanent closure needs the closing permission; a "
          "closed claim is reopened with a reason by a permitted user.", "k", "", "BR-CLM-05", "Fit",
          "Temporary closure statuses, permanent closure, reopen.",
          ["A temporarily closed claim keeps ageing."]),
        R("FRCLM.023.03", "The settlement fields (Annex R), the settlement check registry (Annex S) and the "
          "Certificate of No Claim (Annex T) are kept as BDOI defines them.", "o", "", "CR-CLM-112", CR,
          "Annexes R, S and T have no content; change request CR-CLM-112 once defined (Q-CL-05).",
          ["A settlement check received for safekeeping is recorded with its number and release."]),
    ]),
    E("FRCLM.024", "Settlement", "Claims special remittance", ["BRCLM.010"], NEW,
      ("H", "to ask for the premium remittance a claim waits for", "the insurer releases the claim", "SCR-CLM-04",
       "High", 3, [("a claim in With BDOI - For Premium Remittance", "the batch is fully remitted", "I am told and the "
                    "claim flag clears")]), [
        R("FRCLM.024.01", "A claim in status With BDOI - For Premium Remittance shows the unremitted invoices of the "
          "cover and links to the special remittance request with condition Claims; the handler is told when the "
          "premium is fully remitted.", "k", "", "-", "Fit", "Claims special remittance through Remittance "
          "(FRID.RMT.011).", ["The special remittance request is confirmed by the Claims module."]),
    ]),
]
for _f in FRUM:
    for _r in _f["reqs"]:
        assert _r["id"].startswith(_f["id"] + "."), _r["id"]
ACCEPTANCE = {r["id"]: r["ac"] for f in FRUM for r in f["reqs"]}

BRD_ROWS = [
    ("BRCLM.001", "Identify unpaid invoices / policies; Claims Authorization Code disabled for them."),
    ("BRCLM.002", "Permissions to access the Cover Numbers."),
    ("BRCLM.003", "View access to all Cover Numbers; cover number and version required on a claim."),
    ("BRCLM.004", "Maintain the Reported Date only."),
    ("BRCLM.005", "Permission to update the status to Closed."),
    ("BRCLM.006", "Permission to override or input the Claimant's Name."),
    ("BRCLM.007", "Get the Policy Number from the source and upload it to Claims."),
    ("BRCLM.008", "Display the Policy Number upon invoicing."),
    ("BRCLM.009", "Philippine Peso as the default currency."),
    ("BRCLM.010", "Maintain the values of Claim Status (18)."),
    ("BRCLM.011", "Permission to update the Claim Status."),
    ("BRCLM.012", "Role / unit-based permissions for selecting a Claim Status."),
    ("BRCLM.013", "Selection of a Claim Status only for specific roles / units."),
    ("BRCLM.014", "Maintain the values of Requested Type of Settlement (10)."),
    ("BRCLM.015", "Permission to update the Requested Type of Settlement."),
    ("BRCLM.016", "View the Marketing Team / Unit and the Location / BDOI Branch of the policy."),
    ("BRCLM.017", "Maintain the values of Adjuster / Appraiser (25)."),
    ("BRCLM.018", "Permission to update the Adjuster / Appraiser."),
    ("BRCLM.019", "Permission to override the Next Follow-up Date."),
    ("BRCLM.020", "A field for the Next Action Plan Summary."),
    ("BRCLM.021", "Permission to encode the Next Action Plan Summary."),
    ("BRCLM.022", "Diary function for claims handlers."),
    ("BRCLM.023", "A field to amend the Insurer Reserve."),
    ("BRCLM.024", "Permission to amend the Insurer Reserve."),
    ("BRCLM.025", "Overall age of claims from the date reported."),
    ("BRCLM.026", "Claims Aging Report with the overall age."),
    ("BRCLM.027", "Age of claims per status."),
    ("BRCLM.028", "Claims Aging Report per status."),
    ("BRCLM.029", "Settled Claims report."),
    ("BRCLM.030", "Loss Experience report."),
    ("BRCLM.031", "Outstanding Claims Report."),
    ("BRCLM.032", "Loss Ratio Report."),
    ("BRCLM.033", "Access to the data needed for reports or analytics."),
    ("BRCLM.034", "Track and report pending actions for claims."),
    ("BRCLM.035", "Tag closures as temporary or permanent."),
    ("BRCLM.036", "Maintain catastrophe codes."),
    ("BRCLM.037", "Several insured locations on one claim."),
    ("BRCLM.038", "Identify claims-prone locations or areas."),
    ("BRCLM.039", "View the latest endorsement or cover version of a claim."),
    ("BRCLM.040", "View loss information of accounts and policies (Marketing)."),
    ("BRCLM.041", "Capture claims reported or updated by insurers."),
    ("BRCLM.042", "Align insured locations with insurer location references."),
    ("BRCLM.043", "Several insurer claim numbers under one claim incident."),
]
MAPPING_NOTE = ("Every line of the Claims BRD maps to entries of section 3. BDOI's mapping is kept where it holds; "
                "where BDOI's item does not state the line, the entry FRCLM.021 to 024 of this version is added.")


def _mapping():
    rows = []
    for bid, text in BRD_ROWS:
        entries = [f["id"] for f in FRUM if bid in f["brd_ids"]]
        if not entries:
            raise SystemExit(f"BRD {bid} maps to no entry")
        rows.append((bid, text, ", ".join(entries), "-"))
    return rows


MAPPING = _mapping()
BRD_NFR = [
    ("Security", "Role-based access, permissions per action (BRCLM.002, 005, 011).", "NFR-SEC-02; BRD-11"),
    ("Audit", "Every change of a claim recorded (BRCLM.043, FRCLM.010).", "NFR-AUD-01"),
    ("Retention", "Claims kept 10 years online and 5 years archived.", "NFR-DATA (retention)"),
    ("Activity log", "Activity log of about 1,200 entries a day (NFR 15.08).", "NFR-PER (volumes)"),
    ("Reports", "Reports in Excel and PDF, on demand.", "NFR-REP"),
]
FRUM_NOTE = ("Each entry keeps BDOI's FRCLM number and each sub-item is a requirement row; FRCLM.021 to 024 are added "
             "by this version. Source gives which. BDOI's items beyond the BRD are green or orange. The Example column "
             "holds the first acceptance criterion; all the criteria are in the User Stories v1.0 and the RTM.")

BUSINESS_RULES = [
    ("BR-CLM-01", "Each claims action needs its permission; users and memberships come from UIDM-ISC, sign-in "
     "through Entra ID, no break-glass account.", "k"),
    ("BR-CLM-02", "A claim is recorded on a cover (account and policy year) of the claims users' company, with a loss "
     "date not in the future and inside the policy period unless confirmed, and a reported date not before the loss "
     "date.", "k"),
    ("BR-CLM-03", "The premium of the policy year is checked on recording, on each payment and daily; the claims "
     "authorization code is generated only on a paid cover (direct payment: setting BCL_AUTH_DP_POLICY).", "k"),
    ("BR-CLM-04", "The claims handler group gives each handler his unit and team; visibility, approvals and "
     "escalations follow it.", "k"),
    ("BR-CLM-05", "Statuses, settlement types and lists are maintained by the Claims Unit Head under maker-checker; a "
     "status is selected only by the roles and units of the status access matrix.", "k"),
    ("BR-CLM-06", "An insurer claim number is unique for its insurer; every change of a claim is recorded with user "
     "and time and the history is insert-only.", "k"),
    ("BR-CLM-07", "The next follow-up date is set at each status change from the days of the status (else setting "
     "BCL_FOLLOW_UP_DAYS) unless overridden with a reason.", "k"),
    ("BR-CLM-08", "Age overall = closure or as-of date - reported date; age in status = as-of date - date of the last "
     "status change; calendar days; brackets of setting BCL_AGEING_BUCKETS.", "k"),
    ("BR-CLM-09", "A claim moves to Newly Filed Claim - with complete documents only when the documents required for "
     "its risk code category are uploaded.", "g"),
    ("BR-CLM-10", "Claims reports carry the BDO Insure heading (company, title, as-of or period, run date, run time "
     "in 12-hour format, page N of N), are on demand and export to Excel and PDF.", "k"),
    ("BR-CLM-11", "Claims post no accounting entry; reserve and settlement are the insurer's figures.", "k"),
    ("BR-CLM-12", "A follow-up whose status needs approval is final only after the Team Leader and, where required, "
     "the Team Head approve it.", "o"),
]
MESSAGES = [
    ("Record Claim", "Search too short", "Enter at least 3 characters"),
    ("Record Claim", "Loss date in the future", "The date of loss cannot be in the future"),
    ("Record Claim", "Loss date outside the policy year", "The date of loss is outside the policy period: confirm"),
    ("Record Claim", "Reported date before the loss date", "The reported date cannot be before the date of loss"),
    ("Record Claim", "Location not on the cover", "Location <item> is not on the cover"),
    ("Claim", "Authorization code on an unpaid cover", "The premium of the policy year is not paid"),
    ("Claim", "Insurer claim number already used", "Claim number <number> is already recorded for <insurer>"),
    ("Claim", "Status not allowed", "Your role and unit may not set the status <status>"),
    ("Claim", "Back to a newly filed status", "A claim in progress cannot return to a newly filed status"),
    ("Claim", "Settlement amount missing", "Enter the settlement amount and date"),
    ("Claim", "Reopen without reason", "Select the reason for reopening"),
    ("Claim", "Follow-up date in the past", "The next follow-up date must be today or later"),
    ("Claim", "Action plan too long", "The action plan can have at most 2,000 characters"),
    ("Claim", "Reserve negative", "The reserve cannot be negative"),
    ("Claims Setup", "Matrix row exists", "The role and unit already have this status"),
    ("Claims Reporting Form (proposed)", "Mandatory field missing", "<Field> is required"),
    ("Approvals (proposed)", "Deny without comment", "Enter the comment for the handler"),
]
PERSONA_HEADS = ["Function", "Officer", "Team Lead", "Team Head", "Unit Head", "Risk", "Mkt TL"]
_ROLES = ["CLM_OFFICER", "CLM_TL", "CLM_TH", "CLM_UH", "CLM_RISK", "MKT_TL"]
_GRANTS = {
    "BCL_VIEW": "CLM_OFFICER CLM_TL CLM_TH CLM_UH CLM_RISK", "BCL_COVER_VIEW": "CLM_OFFICER CLM_TL CLM_TH CLM_UH CLM_RISK",
    "BCL_RECORD": "CLM_OFFICER CLM_TL CLM_TH", "BCL_AUTHORIZE": "CLM_OFFICER CLM_TL CLM_TH",
    "BCL_STATUS_UPDATE": "CLM_OFFICER CLM_TL CLM_TH", "BCL_ACTION_PLAN": "CLM_OFFICER CLM_TL CLM_TH",
    "BCL_CLAIMANT_OVERRIDE": "CLM_TL CLM_TH", "BCL_SETTLEMENT_UPDATE": "CLM_TL CLM_TH", "BCL_CLOSE": "CLM_TL CLM_TH",
    "BCL_ADJUSTER_ASSIGN": "CLM_TL CLM_TH", "BCL_RESERVE_AMEND": "CLM_TL CLM_TH",
    "BCL_FOLLOW_UP_OVERRIDE": "CLM_TL CLM_TH", "BCL_REOPEN": "CLM_TH CLM_UH", "BCL_SETUP": "CLM_UH",
    "BCL_REPORT_VIEW": "CLM_OFFICER CLM_TL CLM_TH CLM_UH CLM_RISK MKT_TL",
    "BCL_REPORT_EXPORT": "CLM_OFFICER CLM_TL CLM_TH CLM_UH CLM_RISK MKT_TL", "BCL_DATA_EXTRACT": "CLM_UH CLM_RISK",
}
_FUNCTIONS = [
    ("View claims", "BCL_VIEW"), ("Look up covers", "BCL_COVER_VIEW"), ("Record claims, insurers, locations, diary",
                                                                      "BCL_RECORD"),
    ("Generate the authorization code", "BCL_AUTHORIZE"), ("Change status", "BCL_STATUS_UPDATE"),
    ("Encode the action plan", "BCL_ACTION_PLAN"), ("Override the claimant", "BCL_CLAIMANT_OVERRIDE"),
    ("Set settlement", "BCL_SETTLEMENT_UPDATE"), ("Close permanently", "BCL_CLOSE"),
    ("Assign adjuster", "BCL_ADJUSTER_ASSIGN"), ("Amend reserve", "BCL_RESERVE_AMEND"),
    ("Override follow-up date", "BCL_FOLLOW_UP_OVERRIDE"), ("Reopen", "BCL_REOPEN"), ("Claims Setup", "BCL_SETUP"),
    ("Run reports", "BCL_REPORT_VIEW"), ("Export reports", "BCL_REPORT_EXPORT"), ("Data extract", "BCL_DATA_EXTRACT"),
]
PERMISSIONS = [(f"{name} ({perm})", *("Y" if r in _GRANTS[perm].split() else "-" for r in _ROLES))
               for name, perm in _FUNCTIONS]
ACCESS_NOTE = ("The Marketing Account Officer runs the claims reports on screen without export. The CRF and approval "
               "permissions come with CR-CLM-101 and CR-CLM-102. Users and group-profile membership come from UIDM-ISC "
               "and sign-in is through Microsoft Entra ID; there is no break-glass account. Reinsurance claims are "
               "Phase 2 and hidden from the BDOI profiles.")
PARAMETER_NOTE = ("Settings are changed on System Parameters and applied after another user approves the change; the "
                  "lists on Claims Setup. Keys, types and ranges are in the Data Dictionary, sheet Parameters.")
PARAMETERS = [
    ("BCL_DEFAULT_CURRENCY: claim currency when the cover has none", "PHP", "Currency", "Claim", "k"),
    ("BCL_FOLLOW_UP_DAYS: days to the next follow-up", "7", "1 to 90", "Follow-up", "k"),
    ("BCL_AGEING_BUCKETS: ageing brackets", "30, 60, 90, 180", "Days", "Ageing", "k"),
    ("BCL_PAST_DUE_DAYS: past due threshold", "90", "Days", "Ageing", "k"),
    ("BCL_PRONE_MIN_CLAIMS / BCL_PRONE_YEARS: claims-prone location", "3 / 3", "Number / years", "Reports", "k"),
    ("BCL_AUTH_DP_POLICY: authorization on a direct-payment cover", "CONFIRM", "ALLOW, CONFIRM, BLOCK", "Premium",
     "k"),
    ("Scorecard rating bands", "A 95, B 90, C 85, D 80, F below 80", "Percent", "Scorecard", "o"),
    ("TAT targets per insurer (Annex AB)", "Motor 2, 3 or 7 working days; Non-Motor 10 or 15; check 10", "Working days",
     "Scorecard", "o"),
]
LOVS = [
    ("Claim status (BCL_CLAIM_STATUS)", "The 18 statuses of the BRD (Annex F adds Open, Closed and the settled and "
     "closed values, Q-CL-04)"),
    ("Requested type of settlement (BCL_SETTLEMENT_TYPE)", "Closed - Cancelled; Closed - Denied; Closed - within "
     "Deductible; Closed - without Payment; Settled; Settled - Directly Filed; Settled - LOA Issued; Settled - LOA "
     "Issued - For Schedule of Repair; Settled - LOA Issued - Vehicle Under Repair; Settled - Signed Release Papers "
     "Returned; Settled - Total Loss"),
    ("Adjuster / Appraiser (BCL_ADJUSTER)", "The 25 companies of the BRD (Annex H)"),
    ("Catastrophe (BCL_CATASTROPHE)", "Typhoon; Earthquake; Flood; Volcanic Eruption; Landslide; Fire; Others"),
    ("Claim type (BCL_CLAIM_TYPE)", "Annex M: 8 codes of the core system and 98 types of claim (to load)"),
    ("Nature of claim (BCL_LOSS_NATURE)", "Annex O: 37 values (to load)"),
    ("Claim severity (proposed)", "Annex K: 18 values"),
    ("Follow-up type (proposed list)", "Annex N: 8 values"),
    ("Claims unit (BCL_UNIT)", "Motor - Head Office; Non-Motor - Head Office; branches Angeles, Cebu, Cagayan de Oro, "
     "Davao, General Santos"),
    ("Insurer update source (BCL_UPDATE_SOURCE)", "E-mail; Letter; Insurer portal; Call; File from the insurer"),
]
LIFECYCLES = {
    "claim": ([("N", "New"), ("I", "In Progress"), ("T", "Temporarily\\nClosed"), ("C", "Closed")],
              [("N", "I", "Status in progress"), ("I", "T", "Temporary closure"), ("T", "I", "Status in progress"),
               ("I", "C", "Settlement that closes"), ("T", "C", "Settlement that closes"),
               ("C", "I", "Reopen with reason")], "Phases of a claim"),
    "followup": ([("D", "Saved"), ("P", "Pending Team\\nLeader"), ("H", "Pending Team\\nHead"), ("A", "Approved"),
                  ("R", "Returned")],
                 [("D", "P", "Submit (status needs approval)"), ("P", "H", "Approve and forward"),
                  ("P", "A", "Approve (no Team Head)"), ("H", "A", "Approve"), ("P", "R", "Deny with comment"),
                  ("H", "P", "Deny with comment"), ("R", "P", "Correct and submit")],
                 "Approval of a follow-up (proposed, CR-CLM-102)"),
}
STATUSES = {
    "Claim phase": [("New", "Newly filed claim.", "In Progress"),
                    ("In Progress", "Being followed up.", "Temporarily Closed; Closed"),
                    ("Temporarily Closed", "Non-submission of documents or with offer; keeps ageing.",
                     "In Progress; Closed"),
                    ("Closed", "Settled or closed by a settlement type.", "In Progress (reopen)")],
    "Follow-up approval (proposed)": [("Saved", "Recorded by the handler.", "Pending Team Leader"),
                                      ("Pending Team Leader", "Waiting for the Team Leader.",
                                       "Pending Team Head; Approved; Returned"),
                                      ("Pending Team Head", "Escalated for final approval.", "Approved; Pending Team "
                                                                                             "Leader"),
                                      ("Approved", "Final.", "-"), ("Returned", "Back with comments.", "Pending Team "
                                                                                                     "Leader")],
    "Claims reporting form (proposed)": [("Draft", "Saved by the Account Officer.", "Submitted"),
                                         ("Submitted", "With the handlers.", "Accepted; Returned"),
                                         ("Returned", "Back to the Account Officer.", "Submitted"),
                                         ("Accepted", "Claim created with its number.", "-")],
}
ASSUMPTIONS = [
    ("A-CLM-01", "The Claims BRD with its addenda (version of 8-Oct-2026) is the baseline; BDOI's FRS v1.0 adds the "
     "points marked green or orange."),
    ("A-CLM-02", "All modules go live together in January 2028; Claims is specified with Drop 2."),
    ("A-CLM-03", "BDOI receives no claim proceeds and pays no claimant: BIBS records the insurer's figures only."),
    ("A-CLM-04", "Reinsurance claims start with the reinsurance module in Phase 2."),
]
DEPENDENCIES = [
    ("D-CLM-01", "BDOI gives the annexes referred to and not received: Claims Core FRS Data Mapping, Data Migration "
     "data mapping, Motor and Non-Motor process flows, Insurers Directory, Claims KRA."),
    ("D-CLM-02", "BDOI completes Annexes L, R, S and T and the columns of the report annexes U to Z."),
    ("D-CLM-03", "BDO IT confirms the e-mail service (CCM) and the insurers' claims mailboxes (IS-CLM-01)."),
    ("D-CLM-04", "Data Migration loads the open and historical claims and the masters (DM-CLM-01, DM-CLM-02)."),
    ("D-CLM-05", "UIDM-ISC carries the claims group profiles and the reporting lines of the handler groups."),
]
OPEN_QUESTIONS = [
    ("Q-CL-01", "BDOI's mapping calls the Claims Authorization Code an EBIX function, out of scope, while BRCLM.001 "
     "asks to disable it for unpaid covers. Keep the code in BIBS (as stated in FRCLM.021.02) or remove it?",
     "Claims Unit Head", ANSWER_BY),
    ("Q-CL-02", "Unpaid or partly paid policy: return the form to the Account Officer (BDOI's FRS) or record the claim "
     "flagged unpaid with the code disabled (BRD)? Both are proposed: record, flag and return to the AO.",
     "Claims Unit Head", ANSWER_BY),
    ("Q-CL-03", "Claims Reporting Form: which fields are mandatory for Marketing, and may a handler create a claim "
     "without a form?", "Claims Unit Head; Marketing", ANSWER_BY),
    ("Q-CL-04", "Annex F lists 29 values mixing statuses (Follow Up, New PLA) and settlement outcomes, plus Open and "
     "Closed; the BRD has 18 statuses and 10 settlement types. Confirm the lists and which values need Team Leader "
     "or Team Head approval.", "Claims Unit Head", ANSWER_BY),
    ("Q-CL-05", "Annexes R, S and T are empty: settlement fields, settlement check registry, certificate of no claim. "
     "Does BDOI receive settlement checks for safekeeping (the dashboard lists 'Settlement Check for Safekeeping')? "
     "This changes CLQ10 (no claim money through BDOI).", "Claims Unit Head; Comptrollership", ANSWER_BY),
    ("Q-CL-06", "Approval workflow: which actions need approval (follow-up, record corrections, temporary closure, "
     "reopen, settlement, denial), with which levels, and the time limit before escalation.", "Claims Unit Head",
     ANSWER_BY),
    ("Q-CL-07", "Visibility: handler sees only his claims (BDOI's FRS) while BRCLM.003 gives view access to all "
     "covers. Confirm for claims, covers and reports, including branch users.", "Claims Unit Head", ANSWER_BY),
    ("Q-CL-08", "Lead measures LM1, LM2, LM3 and the claims grouping 'Motor vs Non-Motor': definitions.",
     "Claims Unit Head", ANSWER_BY),
    ("Q-CL-09", "TAT: working days on which calendar; start events (adjuster's report submitted, documents complete); "
     "TAT per insurer from the SLA or one table (Annex AB).", "Claims Unit Head", ANSWER_BY),
    ("Q-CL-10", "Annex Q: default of the next follow-up date ('Today's Date + ???') per status.", "Claims Unit Head",
     ANSWER_BY),
    ("Q-CL-11", "Annex P: 'Name' field (difference with the claimant); Completed date versus Date of Settlement; "
     "Claim CorpDept, Claim Origin values.", "Claims Unit Head", ANSWER_BY),
    ("Q-CL-12", "Sustainability report: purpose, columns and recipients (title only in FRCLM.018).", "Risk; "
     "Sustainability Office", ANSWER_BY),
    ("Q-CL-13", "Claims KRA: send Annex AC (Claims KRA workbook) with the criteria and weights.", "Claims Unit Head",
     ANSWER_BY),
    ("Q-CL-14", "Claim types: Annex M mixes the 8 codes of the core system with 98 types of claim of the category "
     "list, with duplicates (Fire / Fire/Lightning, Theft / Robbery). Confirm the two lists and Annex L (empty).",
     "Claims Unit Head", ANSWER_BY),
    ("Q-CL-15", "Report annexes U to Z have headings only: send the column lists of each layout.", "Claims Unit Head",
     ANSWER_BY),
    ("Q-CL-16", "Total Loss: 75% of the loss for Motor, the whole sum insured for Non-Motor; set by the handler or "
     "computed? Used by Renewal (CLQ28).", "Claims Unit Head", ANSWER_BY),
    ("Q-CL-17", "Legacy claims: years and statuses to migrate (Annex C reference not received).", "Claims Unit Head; "
     "Data Migration", ANSWER_BY),
    ("Q-CL-18", "Annex B copies the definitions of the Collections FRS (CMS, DP, PTP, SOA); send the claims terms.",
     "BDOI System Analyst", ANSWER_BY),
]
CRS = [
    ("CR-CLM-101", "Claims Reporting Form by the Marketing Account Officer: draft, CRF reference, submit, accept or "
     "return, conversion to a claim", "Open", "FRCLM.008.01, 002.03", (1.5, 4.0, 4.0, 2.5, 1.0),
     "BDOI's FRS; not in the BRD; not offered."),
    ("CR-CLM-102", "Approval of follow-ups and record changes by the Team Leader and Team Head, with return comments "
     "and approval queues", "Open", "FRCLM.010, 003.04", (1.5, 5.0, 4.0, 3.0, 1.0), "BDOI's FRS; not in the BRD."),
    ("CR-CLM-103", "Dashboards of the Claims Handler, Team Leader and Team Head: team measures, lead measures, SLA/TAT, "
     "workload, groupings", "Open", "FRCLM.001.02 to .04", (2.0, 5.0, 6.0, 3.0, 1.0), "BDOI's FRS; not in the BRD."),
    ("CR-CLM-104", "Visibility of claims by hierarchy: handler own, Team Leader team, Team Head all", "Open",
     "FRCLM.009.02", (1.0, 3.0, 2.0, 2.0, 0.5), "BDOI's FRS; BRD gives view access to all covers (Q-CL-07)."),
    ("CR-CLM-105", "Claim types by risk code, claim categories and claim severity masters with filtering", "Open",
     "FRCLM.005.02 to .04", (1.0, 3.0, 3.0, 2.0, 0.5), "BDOI's FRS; not in the BRD."),
    ("CR-CLM-106", "Adjuster and appraiser master with contact details", "Open", "FRCLM.004.02",
     (0.5, 1.5, 1.5, 1.0, 0.5), "BDOI's FRS; the BRD asks for the list only."),
    ("CR-CLM-107", "Formal Loss Advice templates with versions, effectivity dates and the consolidated letter", "Open",
     "FRCLM.007.01", (1.0, 3.0, 3.0, 2.0, 0.5), "BDOI's FRS; the BRD asks for the loss advice only."),
    ("CR-CLM-108", "Risk-code blocks of the claim (Motor, Health with hospital list, Mortgage Redemption) and the Total "
     "Loss tag", "Open", "FRCLM.008.10, 017.02", (1.5, 4.0, 4.0, 2.0, 0.5), "BDOI's FRS (Annex P)."),
    ("CR-CLM-109", "Turnaround dates on the follow-up and the Insurers' Scorecard (A to F, acceptance rate, average "
     "TAT)", "Open", "FRCLM.020, 009.04", (1.5, 4.0, 3.0, 2.5, 0.5), "BDOI's FRS (Annex AB)."),
    ("CR-CLM-110", "Claims KRA report (Annex AC)", "Open", "FRCLM.019", (1.0, 3.0, 1.0, 1.5, 0.5),
     "BDOI's FRS; Annex AC not received."),
    ("CR-CLM-111", "Sustainability report, claims input", "Open", "FRCLM.018", (1.0, 2.0, 1.0, 1.0, 0.5),
     "BDOI's FRS; content to confirm."),
    ("CR-CLM-112", "Settlement fields, settlement check registry and certificate of no claim (Annexes R, S, T)",
     "Open", "FRCLM.023.03", (1.5, 3.0, 3.0, 2.0, 0.5), "BDOI's FRS; annexes empty; effort indicative (Q-CL-05)."),
]
CR_DECISION = ("Register v1.0: twelve change requests from BDOI's FRS v1.0 for points not in the Claims BRD and not "
               "offered by the OOTB platform; four green points offered by the OOTB platform at no cost. BDOI "
               "approves or rejects each change request.")
OOTB_POINTS = [
    ("G-CLM-01", "Standard claim document checklist per risk code category, with the claim held until the required "
     "documents are uploaded", "FRCLM.006.01, 006.02"),
    ("G-CLM-02", "Accredited repair shops and the Preferred Shop of Motor claims", "FRCLM.007.02"),
    ("G-CLM-03", "Follow-up type list", "FRCLM.007.03"),
    ("G-CLM-04", "Export of the follow-up list to Excel and PDF", "FRCLM.009.08"),
]
INTEGRATION_POINTS = [
    ("IS-CLM-01", "Formal loss advice e-mailed to the insurer's claims point of contact through BDO's e-mail service "
     "(CCM)", "FRCLM.008.08; ICD IF-CLM-01"),
    ("IS-CLM-02", "Claim documents kept in the document store (S3)", "FRCLM.006.03; ICD IF-CLM-03"),
    ("IS-CLM-03", "Users, group profiles and reporting lines from UIDM-ISC", "FRCLM.002.05, 002.06; BRD-11"),
    ("DM-CLM-01", "Open and historical claims of the core system (Annex C)", "Data Migration (BRD-13); ICD IF-CLM-05"),
    ("DM-CLM-02", "Masters: adjusters, claim types, nature of claim, documents per risk code", "FRCLM.004 to 007; "
     "Data Migration (BRD-13)"),
]
BEST_PRACTICE = [
    ("BP-CLM-01", "Escalation of disputed and denied claims within BDOI and to the insurer's management, with the "
     "advocacy steps logged", "Medium", 5),
    ("BP-CLM-02", "Alert to the handler before an insurer's TAT target is reached, from the turnaround dates",
     "Medium", 3),
    ("BP-CLM-03", "Written acknowledgement of the claim to the client, from a letter template", "Low", 2),
]
DELIVERY_WORK = [
    ("DW-CLM-01", "Handler dashboard: ageing trend", "FRCLM.001.01", 2),
    ("DW-CLM-02", "Read access of Marketing to the claim record", "FRCLM.002.02", 2),
    ("DW-CLM-03", "Supervising Team Lead and Team Head in the handler register", "FRCLM.002.06", 2),
    ("DW-CLM-04", "Annex F status list, status types and alphabetical order", "FRCLM.003.02", 2),
    ("DW-CLM-05", "Claim type values of Annex M", "FRCLM.005.01", 1),
    ("DW-CLM-06", "Claim logging fields of Annex P in the BRD scope", "FRCLM.008.02", 4),
    ("DW-CLM-07", "Return of the claim of an unpaid policy to the Account Officer", "FRCLM.008.04", 2),
    ("DW-CLM-08", "Preview of the loss advice before sending", "FRCLM.008.08", 1),
    ("DW-CLM-09", "Follow-up list: follow-up count, plate number, aging since last follow-up", "FRCLM.009.01", 3),
    ("DW-CLM-10", "Quick view and Annex Q follow-up fields in the BRD scope", "FRCLM.009.03, 009.04", 3),
    ("DW-CLM-11", "BDOI report heading, filter set, detailed and summary presentation of the claims reports",
     "FRCLM.011 to 017", 10),
    ("DW-CLM-12", "Barangay level and several locations in the claims-prone report", "FRCLM.017.01", 2),
    ("DW-CLM-13", "Enable the OOTB document checklist, repair shops and list export", "FRCLM.006, 007.02, 009.08", 4),
]

REVIEW_NOTE = ("BDOI's FRS for Claims v1.0 (13-Aug-2026) was received on 17-Oct-2026 with its signoff sheet. It maps "
               "the 43 BRD lines to its items FRCLM.001 to 020 and has annexes A to AC. The review below lists what "
               "this version keeps, completes or asks BDOI to confirm.")
REVIEW = [
    ("Mapping", "Every BRD line names FRCLM items, but several items do not state the line (BRCLM.001 authorization "
     "code, 022 diary, 023/024 reserve, 041/042 insurer updates and references).", "FRCLM.021 to 024 added; "
     "FRCLM.009 states the diary and the reserve."),
    ("Items beyond the BRD", "CRF by Marketing, approval workflow, role dashboards, hierarchy visibility, claim "
     "categories and severity, templates with versions, risk-code fields, KRA, scorecard, sustainability report.",
     "Kept, coloured orange with change requests CR-CLM-101 to 112, or green when offered by the OOTB platform."),
    ("FRCLM.002.01 to .03", "Access of the Claims Handler, Team Lead and Team Head left blank.", "Stated by the "
     "permissions of section 6."),
    ("FRCLM.013 and FRCLM.014", "Both titled 'Aging of Outstanding Claims (Current Status)'.", "FRCLM.013 is the past "
     "due report (N days); FRCLM.014 keeps the title."),
    ("FRCLM.017 and FRCLM.018", "Lead text 'Outstanding Claims Report' copied from FRCLM.011.", "Titles of the items "
     "used."),
    ("Annexes", "L, R, S, T empty; U to Z headings only; C, D, AA, AC refer to files not received.",
     "Dependencies D-CLM-01, D-CLM-02; questions Q-CL-05, Q-CL-13 to Q-CL-15."),
    ("Annex B", "Definitions of the Collections FRS (CMS, DP, PTP, SOA, incentives).", "Q-CL-18."),
    ("Cover, revision log and signoff", "Names of persons.", "Replaced by roles."),
]
CONFLICTS = [
    ("MBS maintains user profiles (FRCLM.002.05)", "UIDM-ISC maintains the users and their group-profile membership; "
     "Microsoft Entra ID signs every user in; no break-glass account; BIBS keeps the permissions of each group profile "
     "under maker-checker (FRS User Access Maintenance v1.6).", "FRCLM.002.05 restated: MBS keeps the claims profile "
     "permissions in BIBS; users and memberships are requested in UIDM-ISC."),
    ("Claims Authorization Code out of scope", "BRCLM.001 asks to disable the code for unpaid covers; BIBS issues it "
     "on a paid cover.", "Kept in FRCLM.021.02 pending Q-CL-01."),
    ("Unpaid policy routed back to Marketing", "The BRD asks to identify unpaid invoices and disable the code; the "
     "claim is still recorded.", "FRCLM.008.04 records, flags and returns (Q-CL-02)."),
    ("Settlement checks for safekeeping", "Claims post no accounting entry and BDOI handles no claim money (CLQ10).",
     "CR-CLM-112 waits for Q-CL-05."),
    ("Reinsurance and insurer or client portals", "Reinsurance is Phase 2, hidden except for the Brokerverse "
     "administrator; no partner portal in phase 1.", "Insurer communications by e-mail and upload (FRCLM.022)."),
    ("Handler sees only his claims", "BRCLM.003 gives view access to all covers.", "CR-CLM-104 pending Q-CL-07."),
]
RENUMBERED: list = []

UXD_NOTE = ("The screens marked Available are offered by the BIBS platform today; the Availability row names the "
            "changes of this FRS still to deliver, and the proposed screens come with their change requests. "
            "Screenshots are added in the next version, after the walkthrough with the Business Unit on the test "
            "environment.")

# ================================================================================================ BPS
BPS_NOTE = ("The storyboards use the walkthrough accounts and fictitious clients, insurers and amounts; the screens "
            "they name are described in the UX Design. Storyboard SB-CLM-01 and the approval steps of SB-CLM-02 "
            "depend on change requests CR-CLM-101 and CR-CLM-102.")
JOURNEY_SPAN = "the notice of loss to the closure of the claim"
JOURNEY = [
    ("1", "Marketing Account Officer", "Claims reporting form filled from the client's notice (proposed)"),
    ("2", "Claims Handler", "Form reviewed; claim recorded on the cover with the premium check"),
    ("3", "Claims Handler", "Formal loss advice sent to each insurer; insurer claim numbers recorded"),
    ("4", "Claims Handler", "Follow-up recorded with status, action and next follow-up date"),
    ("5", "Claims Team Leader", "Follow-up approved, forwarded or returned (proposed)"),
    ("6", "Claims Team Head", "Final approval of escalated follow-ups (proposed)"),
    ("7", "Insurer", "Updates, reserve, offer and settlement communicated"),
    ("8", "Claims Handler", "Insurer updates and reserve recorded; diary kept"),
    ("9", "Claims Team Leader", "Settlement type set; claim closed temporarily or permanently"),
    ("10", "Management, Marketing, Risk", "Dashboards, reports and scorecard read"),
]
PROCESSES = [
    {"id": "P-CLM-01", "name": "Notice of loss and claim recording", "steps": "1 to 3", "trigger": "Client notice "
     "through Marketing or the insurer", "actors": "Marketing Account Officer, Claims Handler",
     "outcome": "Claim with its number, insurers and loss advice sent", "controls": "BR-CLM-02, BR-CLM-03, BR-CLM-06",
     "frs": "FRCLM.008, 021, 022"},
    {"id": "P-CLM-02", "name": "Follow-up and approval", "steps": "4 to 8", "trigger": "Next follow-up date; insurer "
     "update", "actors": "Claims Handler, Team Leader, Team Head, Insurer", "outcome": "Claim status current and "
     "approved", "controls": "BR-CLM-05, BR-CLM-07, BR-CLM-12", "frs": "FRCLM.003, 009, 010"},
    {"id": "P-CLM-03", "name": "Settlement and closure", "steps": "9", "trigger": "Insurer settlement or denial",
     "actors": "Claims Team Leader, Team Head", "outcome": "Claim closed or reopened", "controls": "BR-CLM-05",
     "frs": "FRCLM.023, 024"},
    {"id": "P-CLM-04", "name": "Monitoring and reporting", "steps": "10", "trigger": "On demand; daily jobs",
     "actors": "Claims, Marketing, Risk", "outcome": "Reports, dashboards, scorecard",
     "controls": "BR-CLM-08, BR-CLM-10", "frs": "FRCLM.001, 011 to 020"},
]
STORYBOARDS = [
    {"id": "SB-CLM-01", "title": "Claims reporting form to loss advice", "data": "client Alpha Trading Corporation, "
     "Motor policy, own damage on 10-Oct-2026, insurer Alpha Insurance", "summary": "Marketing fills the form, the "
     "handler accepts it, records the claim and sends the loss advice.",
     "steps": [("Marketing Account Officer", "Claims Reporting Form", "Fills the form and submits it",
                "CRF reference given; the handlers are told"),
               ("Claims Handler", "Claims Reporting Form", "Reviews and accepts the form", "Claim created with its "
                "claim number"),
               ("Claims Handler", "Claim", "Completes the Motor fields and the insurer line", "Premium paid; "
                "authorization code generated"),
               ("Claims Handler", "Claim", "Previews and sends the formal loss advice", "Letter sent and kept under "
                "Documents")]},
    {"id": "SB-CLM-02", "title": "Follow-up with approval", "data": "the claim of SB-CLM-01",
     "summary": "The handler records a follow-up whose status needs approval; the Team Leader approves and the Team "
                "Head gives the final approval.",
     "steps": [("Claims Handler", "Claims Worklist", "Opens the claims due today", "The claim with its ages"),
               ("Claims Handler", "Claim", "Records the follow-up with status Closed - within Deductible", "Pending "
                "Team Leader"),
               ("Claims Team Leader", "Approvals", "Approves and forwards", "Pending Team Head"),
               ("Claims Team Head", "Approvals", "Approves", "Approved; claim closed")]},
    {"id": "SB-CLM-03", "title": "Claim waiting for premium remittance", "data": "a Fire claim whose premium is paid "
     "but not yet remitted", "summary": "The insurer waits for the premium; the handler asks for the special "
     "remittance and is told when it is remitted.",
     "steps": [("Claims Handler", "Claim", "Sets With BDOI - For Premium Remittance", "Unremitted invoices shown with "
                "the link"),
               ("Marketing Collection", "Special Remittance", "Requests a special remittance with condition Claims",
                "Confirmed by the Claims module"),
               ("Remittance Team Leader", "Special Remittance Request", "Approves", "Batch created"),
               ("Claims Handler", "Claims Home", "Opens the notice", "Premium remitted; flag cleared")]},
]

# ================================================================================================ RNO
REPORTS = [
    ("RPT-CLM-01", "Outstanding Claims", "FRCLM.011", "As of, filters, grouping", "Open claims with ages (Annex U)",
     "Excel, PDF"),
    ("RPT-CLM-02", "Settled Claims", "FRCLM.012", "Date range, grouping", "Settled claims (Annex V)", "Excel, PDF"),
    ("RPT-CLM-03", "Outstanding Claims N Days Past Due", "FRCLM.013", "As of, N", "Claims older than N days "
     "(Annex W)", "Excel, PDF"),
    ("RPT-CLM-04", "Claims Aging per Status", "FRCLM.014", "As of, grouping", "Age in status and overall (Annex X)",
     "Excel, PDF"),
    ("RPT-CLM-05", "Loss Experience", "FRCLM.015", "Title, loss date range", "Paid, outstanding, total (Annex Y)",
     "Excel, PDF"),
    ("RPT-CLM-06", "Loss Ratio", "FRCLM.016", "Title, period", "Losses over premium (Annex Z)", "Excel, PDF"),
    ("RPT-CLM-07", "Claims-prone Locations and Total Loss", "FRCLM.017", "As of, locations", "Counts and amounts per "
     "location, claims-prone flag, total loss", "Excel, PDF"),
    ("RPT-CLM-08", "Sustainability Report (claims input)", "FRCLM.018", "As of, locations", "To confirm (Q-CL-12)",
     "Excel, PDF"),
    ("RPT-CLM-09", "Claims KRA", "FRCLM.019", "Period, person", "Criteria of Annex AC", "Excel, PDF"),
    ("RPT-CLM-10", "Insurers' Scorecard", "FRCLM.020", "Period, insurer", "TAT ratings A to F, acceptance rate, average "
     "TAT", "Excel, PDF"),
    ("RPT-CLM-11", "Pending Actions", "FRCLM.001", "As of, handler", "Follow-ups and diary due or overdue", "Excel, "
     "PDF"),
    ("RPT-CLM-12", "Insurer Claim Numbers", "FRCLM.008.07", "Period, insurer", "Insurer claim numbers with reserve and "
     "settlement", "Excel, PDF"),
    ("RPT-CLM-13", "Claims Activity Log", "FRCLM.009", "Period", "Status, field, diary, insurer and reserve changes",
     "Excel"),
    ("RPT-CLM-14", "Claims Data Extract", "FRCLM.002", "Period", "One row per claim, insurer line and location",
     "Excel, CSV"),
]
REPORT_NOTE = ("Reports run on screen from Claims Reports and export to Excel and PDF with the heading of BR-CLM-10; "
               "each run is archived. RPT-CLM-08 to 10 come with their change requests.")
DOCUMENTS = [
    ("DO-01", "Formal Loss Advice", "PDF on the letterhead, per insurer or consolidated; preview before sending",
     "Claim, account, client", "FRCLM.008.08"),
    ("DO-02", "Claims Reporting Form", "PDF of the form (proposed)", "Claim", "FRCLM.008.01"),
    ("DO-03", "Certificate of No Claim", "PDF (Annex T, to define)", "Account", "FRCLM.023.03"),
]
EXPORTS = [
    ("Follow-up list", "Excel, PDF", "Claims Worklist", "Rows, filters and sort of the screen, with the follow-up "
     "count, last follow-up and days since"),
    ("Claims reports", "Excel, PDF", "Claims Reports", "BDOI heading, detailed or summary presentation, grouping"),
    ("Claims data extract", "Excel, CSV", "Claims Reports", "Every claim field"),
]
NOTICES = [
    ("NT-01", "Claim recorded or assigned", "Handler, account officer", "Claim <claim> is assigned to you", "Bell"),
    ("NT-02", "Status changed", "Account officer", "Claim <claim> is now <status>", "Bell"),
    ("NT-03", "Follow-up due", "Handler", "<n> follow-ups and diary entries are due today", "Bell"),
    ("NT-04", "Premium paid or remitted", "Handler", "Premium of claim <claim> is <paid / remitted>", "Bell"),
    ("NT-05", "Newer cover version", "Handler", "Cover of claim <claim> has a newer version", "Bell"),
    ("NT-06", "Follow-up to approve (proposed)", "Team Leader, Team Head", "Follow-up of claim <claim> waits for your "
     "approval", "Bell"),
    ("NT-07", "CRF submitted (proposed)", "Handlers of the unit", "Claims reporting form <ref> is submitted", "Bell"),
    ("NT-08", "Claim returned to the account officer", "Account officer", "Claim <claim> is returned to you: "
     "<reason>", "Bell"),
    ("AL-01", "Claim on an unpaid cover", "Handler, Team Leader", "Claim <claim> is on an unpaid cover", "Alert"),
    ("AL-02", "Follow-up overdue", "Handler, Team Leader", "Follow-up of claim <claim> is overdue", "Alert"),
    ("AL-03", "Claim past due", "Handler, Team Leader", "Claim <claim> is older than <n> days", "Alert"),
]
JOBS = [
    ("BCL_PREMIUM_RECHECK", "Daily 05:30", "Re-checks the premium of the open claims without an authorization code.",
     "AL-01"),
    ("BCL_FOLLOW_UP_DUE", "Daily 06:00", "Notices of the follow-ups and diary entries due; alert on overdue ones.",
     "AL-02"),
    ("BCL_AGEING_ALERTS", "Daily 06:00", "Alert for each outstanding claim older than the past due threshold.",
     "AL-03"),
]

# Lists of BDOI's annexes kept in the data dictionary (values are business lists, no personal data).
ANNEX_LISTS = [
    ("Annex K", "Claim severity", "WIND Windshield; MIN Minor damage; MAJ Major damage; CAT Catastrophe; FEDM Front end "
     "damage; CVDM Claimant vehicle damage; UND Undetermined; MINL Minor loss; PDBI Auto PD with BI; N/A Not "
     "applicable; PEIN Pedestrian injury; FAT Fatality; REDM Rear end damage; MVDM Multi vehicle damage; TOT Total "
     "loss; MATL Material loss; OTHR Other; NONE None"),
    ("Annex M", "Claim type codes of the core system", "CG Comprehensive General Liability; FL Fire & Lightning; HP "
     "Hospitalization; LC Credit Card Loss/Fraud; LF Life/Accident/Health; MC Motor; MR Marine Cover; OT "
     "Bonds/BBB/Others/Miscellaneous (98 further types of claim from the category list)"),
    ("Annex N", "Follow-up type", "Advise Insurer of Claim; Appoint Assessor; Chase Insurer for Settlement Offer; Claim "
     "Follow-Up - Claims; Claim Report from Client; Claims - Follow up Recovery; Claims Follow-Up - AE; Obtain Police "
     "Report"),
    ("Annex O", "Nature of claim", "37 values from Accidental Death and Disablement to Voluntary Third Party Liability"),
    ("Annex J", "Standard claim documents", "About 180 document titles against 8 claim types (Y, N or Applicable)"),
    ("Annex F", "Claim status with status type and approval", "29 values; see the FRS, section 3"),
    ("Annex AB", "Scorecard bands", "A 95% and more; B 90-94%; C 85-89%; D 80-84%; F below 80%"),
]


def write_icd(d, tmp) -> None:
    """Body of the Interface Specification v1.0 of Claims."""
    d.h1("Introduction")
    d.para("This document specifies how Claims exchanges data with the insurers, the document store and the other "
           "BIBS modules. Exchanges with systems outside BIBS are Integration scope (grey label in the FRS); the "
           "hand-offs between BIBS modules are listed for completeness. BDOI's FRS names no file exchange with the "
           "insurers: the loss advice goes by e-mail and the insurers' replies are recorded on the claim or uploaded.")
    d.table(["ID", "Interface", "Direction", "Means", "Frequency", "Content"], [
        ["IF-CLM-01", "Formal loss advice", "BIBS to insurer", "E-mail (CCM)", "Per claim and insurer",
         "Letter PDF with claim, policy and insurer data"],
        ["IF-CLM-02", "Insurer updates and references", "Insurer to BIBS", "Upload of a file", "As received",
         "Claim numbers, updates, location references"],
        ["IF-CLM-03", "Claim documents", "BIBS to document store", "In the system (S3)", "Per upload",
         "Images and PDF of the claim"],
        ["IF-CLM-04", "Module hand-offs", "BIBS modules", "In the system", "Continuous",
         "Premium status, special remittance, claim experience"],
        ["IF-CLM-05", "Legacy claims load", "Core system to BIBS", "Migration files", "Once at go-live",
         "Open and historical claims"]], [1.1, 2.0, 1.8, 1.5, 1.4, 1.4], caption="Interfaces")
    d.h1("IF-CLM-01 Formal Loss Advice")
    d.table(["Item", "Detail"], [
        ["Trigger", "Send Loss Advice on the claim, per insurer line (consolidated letter with CR-CLM-107)."],
        ["Recipient", "The insurer's claims point of contact (Annex AA, Insurers Directory) kept on the insurer record."],
        ["Content", "Letter on the letterhead with the claim number, insurer claim number when known, policy number, "
                    "assured, date and place of loss, nature of loss, estimated amount and the documents attached."],
        ["Channel", "BDO e-mail service (CCM) [Integration scope]; the send log and the PDF are kept on the claim."],
        ["Failure", "A failed sending is shown on the claim and can be sent again."]], [2.2, 7.0],
        caption="Formal loss advice")
    d.h1("IF-CLM-02 Insurer Updates and References by Upload")
    d.para("Files of insurer updates are uploaded on the Bulk Upload screen; each row is processed on its own and the "
           "refused rows are listed with their reason. The values below are invented.")
    for title, rows in [
        ("Insurer claim numbers", [["Claim No.", "Y", "Broker claim number", "BCL-2026-000001"],
                                   ["Insurer", "Y", "Insurer code", "INS-ALPHA"],
                                   ["Insurer Claim No.", "Y", "Insurer's claim number", "ALPHA-CL-0001"],
                                   ["Reported To Insurer On", "N", "Date the loss was reported", "2026-10-11"]]),
        ("Insurer updates", [["Claim No.", "Cond.", "Broker claim number, else insurer and insurer claim number",
                              "BCL-2026-000001"],
                             ["Insurer", "Cond.", "With the insurer claim number", "INS-ALPHA"],
                             ["Insurer Claim No.", "Cond.", "Insurer's claim number", "ALPHA-CL-0001"],
                             ["Update Date", "Y", "Not in the future", "2026-10-15"],
                             ["Source", "Y", "E-mail, Letter, Insurer portal, Call, File", "EMAIL"],
                             ["Reference", "N", "Insurer's reference", "LTR-0001"],
                             ["Remarks", "Y", "What the insurer communicated", "Adjuster appointed"]]),
        ("Insurer location references", [["ARN", "Y", "Account of the cover", "ARN-2026-000001"],
                                          ["Item No.", "Y", "Location item of the cover", "1"],
                                          ["Insurer", "Y", "Insurer of the cover", "INS-ALPHA"],
                                          ["Insurer Location Ref", "Y", "Insurer's reference", "ALPHA-LOC-0001"],
                                          ["Effective From", "Y", "First day of the reference", "2026-10-01"]])]:
        d.table(["Column", "Req.", "Content", "Example"], rows, [2.4, 0.7, 4.0, 2.1], caption=title)
    d.h1("IF-CLM-03 Claim Documents")
    d.bullets(["Documents are uploaded on the claim as JPEG, PNG, HEIC or PDF, at most 10 MB each, and stored in the "
               "document store (S3) with the claim, account and client as owners [Integration scope].",
               "Each document has a type from the list of claim document types; the checklist per risk code category "
               "comes with G-CLM-01."])
    d.h1("IF-CLM-04 Module Hand-offs")
    d.table(["Hand-off", "What passes", "When"], [
        ["Operations invoice ledger", "Premium status of the invoices of the cover; payment and remittance events",
         "Recording; each payment; daily"],
        ["Remittance", "Confirmation of an open claim for a special remittance with condition Claims; notice when "
                       "fully remitted", "Request; remittance"],
        ["New Business, Renewal, Account", "Claim experience of a cover: number, status, paid and outstanding",
         "On request"],
        ["User Access", "Users, group profiles and reporting lines from UIDM-ISC", "Continuous"]], [2.4, 5.2, 1.6],
        caption="Hand-offs")
    d.h1("IF-CLM-05 Legacy Claims Load")
    d.para("Open and historical claims of the core system are loaded by Data Migration (BRD-13) with their insurer "
           "claim numbers, statuses and dates, as source Migrated; the data mapping of Annex C is to be received "
           "(D-CLM-01, Q-CL-17) [Data Migration scope].")
    d.h1("Open Points")
    d.table(["ID", "Point", "Owner"], [[a, b, c] for a, b, c, _ in OPEN_QUESTIONS if a in ("Q-CL-05", "Q-CL-17")]
            + [["D-CLM-03", DEPENDENCIES[2][1], "BDO IT"]], [1.4, 5.8, 2.0], caption="Open points")


FRS_ANNEXES = [
    ("Annex A: BDOI Lists Kept in the Masters", [
        "The lists of BDOI's annexes are loaded in the claims masters. They are summarised here and given in full in "
        "the Data Dictionary, sheet BDOI Lists."], (["Annex", "List", "Values"],
                                                   [list(r) for r in ANNEX_LISTS], [1.2, 2.4, 5.6], "BDOI lists")),
]
