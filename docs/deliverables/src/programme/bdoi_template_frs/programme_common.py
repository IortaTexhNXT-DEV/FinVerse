"""Content of the programme documents every BRD pack refers to (Programme/Common): Non-Functional and Security
Requirements, Glossary, Decision and Observation Log, Document Register. Read by build_programme_common.py.

The non-functional requirements themselves are in requirements_quality/nfr_catalogue.yaml (one source for this
document, the SRS and the Test Strategy); this module adds the measured results, the security checklist summary and
the business wording of the other three documents.
"""

from __future__ import annotations

DATE = "16 Oct 2026"
DATE_US = "10/16/2026"
NFR_VERSION = "2.0"
VERSION = "1.0"
HEADER_RIGHT = "BDO Insure Brokering System (BIBS) - Programme"
FILES = {
    "NFR": f"BIBS_NFR_BRD-00_Non_Functional_and_Security_Requirements_v{NFR_VERSION}.docx",
    "GLO": f"BIBS_Glossary_BRD-00_Programme_Glossary_v{VERSION}.xlsx",
    "LOG": f"BIBS_Log_BRD-00_Decision_and_Observation_Log_v{VERSION}.xlsx",
    "REG": f"BIBS_Register_BRD-00_Document_Register_v{VERSION}.xlsx",
}
# Programme files these documents replace (removed from out/).
SUPERSEDED = [
    "Requirements/BIBS_NFR_BRD-00_Non_Functional_Requirements_v1.0.docx",
    "Requirements/BIBS_NFR_BRD-00_Non_Functional_Requirements_Register_v1.0.xlsx",
]

NFR_INTRO = (
    "This document states the non-functional and security requirements of BIBS once, for every BRD. Each "
    "requirement has an ID (NFR-<area>-nn), a measurable target, how it is verified, when, and its status. An FRS "
    "or another document of a BRD pack refers to these IDs and does not repeat them.")
NFR_STATUS_NOTE = ("Status: ALIGNED, the sources agree or BDOI has answered; DECISION, the BRDs conflict and BDOI "
                   "decides, the proposal applies until then; PROPOSED, no BDOI value exists, BDOI confirms.")
NFR_CHANGES = [
    ("NFR-SEC-02", "Second factor at Microsoft Entra ID for every bank user; no break-glass account (BDOI IT, 15 Oct "
     "2026; D-008)."),
    ("NFR-SEC-03", "Provisioning from UIDM-ISC over SCIM 2.0 in force; a leaver loses every session on receipt of "
     "the event; daily reconciliation added."),
]
SIZING = [
    ("Named users", "530 (User Access sizing); 1,344 in NFR-CAP-01, to be confirmed by BDOI (O-001)"),
    ("Concurrent users", "150 in year 1 (NFR-CAP-01)"),
    ("UIDM-ISC full re-synchronisation", "600 users and 2,000 group-profile membership changes"),
    ("Data of the measured run", "1,200 users, 157 group profiles, 120,000 sessions, 63,000 access change records"),
    ("Host of the measured run", "4 virtual processors, database on the same host, 1 GB application memory, 20 "
     "database connections; production runs at least 3 application instances (NFR-AVL-04)"),
]
# Measured results of User Access (load test of 16 Oct 2026): run, load, requests, errors, p95, p99, NFR, target met
MEASURED = [
    ("Sign-in and menu", "150 users", "42,054", "0 %", "Reads 14 ms; sign-in 373 ms", "Reads 24 ms; sign-in 389 ms",
     "NFR-PRF-01, NFR-PRF-09", "Yes"),
    ("Users and user detail", "30 users", "6,289", "0 %", "322 ms", "478 ms", "NFR-PRF-01, NFR-PRF-02", "Yes"),
    ("Group profiles", "30 users", "5,651", "0 %", "112 ms", "330 ms", "NFR-PRF-01, NFR-PRF-02", "Yes"),
    ("Certification extract", "5 users, 20 extracts", "20", "0 %", "2.2 s (1,200 users)", "2.2 s", "NFR-PRF-04",
     "Yes"),
    ("UIDM-ISC re-synchronisation", "600 users + 2,000 changes", "3,200", "0 %", "84 ms", "98 ms", "NFR-SEC-03",
     "Yes"),
    ("UIDM-ISC new joiners", "600 joiners", "600", "0 %", "94 ms", "104 ms", "NFR-SEC-03", "Yes"),
    ("Mixed load", "150 users + 10 changes a second", "35,368", "0 %", "Reads 76 ms; sign-in 680 ms", "Reads 163 ms",
     "NFR-PRF-01, NFR-PRF-09", "Yes"),
]
MEASURED_NOTE = ("Measured on 16 Oct 2026 on a single host with the volumes above. The same scenarios run again on "
                 "the production-sized Pre-Prod during the non-functional tests (1 Nov to 15 Dec 2027) are the "
                 "acceptance evidence.")
ASR = {"source": "BDO Application Security Requirement workbook (ITS-SQT-1.2.8-L3, ASR V4.10, Fraud-ASR V4.9)",
       "summary": [("ASR", 79, 12, 1, 41, 133), ("Fraud-ASR", 0, 0, 0, 27, 27)],
       "open": [
           ("1.8.6", "Quantum-resistant algorithms for network communications", "Not met",
            "Depends on the load balancer and runtime support; plan with BDOI IT."),
           ("1.2.5", "Minimum password length 12 for administrator accounts", "Partly",
            "Met by Entra ID in production; test accounts use 10 characters."),
           ("1.2.14", "Password parameters adjustable to the threat level", "Partly",
            "All parameters except the minimum length are UAM settings."),
           ("1.5.2, 1.5.3, 1.5.5", "Audit of start-up, failures, record reads and storage access", "Partly",
            "Application log and cloud storage logging to confirm with BDOI IT."),
           ("1.7.6", "Least privilege on the database", "Partly", "Separate account for schema changes to add."),
           ("1.7.10, 1.9.7", "Recovery after ransomware", "Partly", "Immutable or cross-account backup copies to "
            "confirm with BDOI IT."),
           ("1.9.3, 1.9.5", "Supported libraries; third-party libraries encapsulated", "Partly",
            "Dependency scanning key to set in the delivery pipeline."),
           ("2.3.1", "No authorisation bypass through record references", "Partly",
            "Every read and change is checked by permission and data access; record numbers stay visible."),
           ("3.4.2", "Answers limited to what the caller needs", "Partly", "Two user lists return every user; "
            "paging above a few thousand users."),
       ]}
OWASP = [
    ("Access control and identity", "Met"), ("Authentication and session", "Met; single sign-on settings per "
     "environment"), ("Input, output and errors", "Met"), ("Transport, headers and configuration", "Met; values per "
     "environment"),
]
SECURITY_OPEN = [
    ("A dormant user deactivated in BIBS returns active if UIDM-ISC sends the account as active.",
     "UIDM-ISC disables the account on the notice Dormant users deactivated."),
    ("Two user lists return every user (425 KB for 1,200 users).", "Server paging above a few thousand users."),
    ("Two sign-ins of the same test account at the same instant can be refused once.", "Accept (test accounts "
     "only) or retry once."),
    ("Integration rate limit (300 calls a minute) against a full re-synchronisation (about 2,600 calls).",
     "Agree the value with BDOI IT before the load test."),
]

GLOSSARY = [
    # term, meaning, category
    ("Acceptance criteria", "Conditions a user story meets to be accepted, written Given / When / Then.", "Method"),
    ("Access matrix", "Table of the group profiles against the permissions, and against the actions of each screen.",
     "User Access"),
    ("Audit trail", "Insert-only record of who did what, when and from where.", "Control"),
    ("BDOI", "BDO Insurance and Reinsurance Brokers, the client.", "Organisation"),
    ("BIBS", "BDO Insure Brokering System, the system described by the packs.", "System"),
    ("BPS", "Business Process and Storyboards: to-be process flows and storyboards of a BRD.", "Document"),
    ("BRD", "Business Requirements Document of BDOI; BRD-00 is the umbrella document.", "Document"),
    ("Certification extract", "File of every account, group profile and permission, used for access reviews.",
     "User Access"),
    ("Change request (CR)", "A point not in the BRD and not offered by the OOTB platform; done only if approved.",
     "Method"),
    ("Checker", "User who approves, returns or rejects a change made by another user.", "Control"),
    ("Colour code", "Black in the BRD; green OOTB; orange change request; blue best practice; grey label for "
     "Integration or Data Migration scope.", "Method"),
    ("Data access", "Companies and branches a user may act for; every list and record is limited to them.",
     "User Access"),
    ("DDI", "Data Dictionary: tables, columns, lists of values, parameters, status models, indexes.", "Document"),
    ("DEV, SIT, UAT, Pre-Prod", "Development, system integration test, user acceptance test and pre-production "
     "environments.", "Environment"),
    ("Dormant user", "User without a sign-in for the dormancy period; deactivated after a notice.", "User Access"),
    ("Drop", "Delivery group of BRDs agreed with BDOI (Drop 0, 1, 2).", "Method"),
    ("DSP", "Developer Specification: screens, service operations, field checks, rules, states, jobs, audit, "
     "permissions.", "Document"),
    ("Epic", "Group of user stories about one capability.", "Method"),
    ("FitGap", "Workbook of each requirement against the BRD, the OOTB platform and best practice.", "Document"),
    ("FRS", "Functional Requirements Specification in BDOI's template D051.", "Document"),
    ("FRUM", "Functional requirement of User Access Maintenance (FRUM.nnn.nn).", "User Access"),
    ("Group profile", "Named set of permissions held by users; maintained in BIBS under maker-checker, membership "
     "from UIDM-ISC.", "User Access"),
    ("ICD", "Interface Specification of a BRD.", "Document"),
    ("Identity event", "Change received from UIDM-ISC: joiner, mover, leaver, rehire, status or group profiles.",
     "User Access"),
    ("iNXT BrokerVerse", "OOTB platform of iorta TechNXT on which BIBS is configured.", "System"),
    ("Joiner, mover, leaver", "New user, change of a user's job or unit, user who leaves.", "User Access"),
    ("List of values (LOV)", "Maintained code list shown in a drop-down.", "Data"),
    ("Maker", "User who proposes a change; never decides it.", "Control"),
    ("Maker-checker", "Control by which a change applies only after another user approves it.", "Control"),
    ("Microsoft Entra ID", "BDO identity provider (EIAM) that signs every bank user in with OpenID Connect.",
     "System"),
    ("NFR", "Non-functional requirement (NFR-<area>-nn), stated in the programme NFR document.", "Document"),
    ("OOTB", "Out of the box: offered by the iNXT BrokerVerse platform at no cost.", "Method"),
    ("OpenID Connect", "Standard sign-in protocol used with Microsoft Entra ID.", "Integration"),
    ("p95, p99", "Response time that 95 or 99 percent of requests do not exceed.", "Quality"),
    ("Parameter", "Setting that changes behaviour without a release; changed under maker-checker.", "Data"),
    ("Permission", "Right to open a screen or take an action; held through group profiles.", "User Access"),
    ("Persona", "Typical user of a pack (for example User Access Administrator, Auditor).", "Method"),
    ("Phase 2", "Functions the client deferred beyond the current phase (decisions of 17-Oct-2026): reinsurance and "
     "the partner portal of Employee Benefits. Kept as a baseline, hidden from the BDOI roles and not counted in the "
     "current-phase conformance figures.", "Scope"),
    ("PHT", "Philippine time (UTC+8).", "Quality"),
    ("Reconciliation", "Daily comparison of the users and group profiles of BIBS with UIDM-ISC.", "User Access"),
    ("RNO", "Reports, Notifications and Outputs of a BRD.", "Document"),
    ("RPO, RTO", "Recovery point and recovery time objectives.", "Quality"),
    ("RTM", "Requirements traceability matrix and test cases: BRD, requirement, user story, screen, test case.",
     "Document"),
    ("SCIM 2.0", "Standard for provisioning users and group membership, used by UIDM-ISC.", "Integration"),
    ("Second approval", "Approval by a second checker of a privileged or out-of-hours change.", "Control"),
    ("Separation of duties (SoD)", "Rule that two group profiles or permissions are not held by one user.",
     "Control"),
    ("Story points", "Relative size of a user story (first estimate).", "Method"),
    ("Test account", "Local account for testing in DEV, SIT and UAT only; never in production.", "User Access"),
    ("UIDM-ISC", "BDOI identity governance platform; source of users and group-profile membership.", "System"),
    ("User story", "Need of a persona: As a ..., I want ..., so that ....", "Method"),
    ("USR", "User Stories workbook of a BRD.", "Document"),
    ("UXD", "UX Design Specification: every screen of a BRD with a screenshot per page.", "Document"),
    ("WCAG 2.2 AA", "Web accessibility guidelines level AA (NFR-ACC-01).", "Quality"),
]

LOG = [
    # id, date, type, BRD, subject, text, by (role), impact or action, status, reference
    ("D-001", "26 Sep 2026", "Decision", "All", "Delivery by drops", "BRDs are grouped and delivered by BDOI drop "
     "(Drop 0, 1, 2).", "BDOI (answer A5)", "Folder structure of the packs.", "Closed", "Programme README"),
    ("D-002", "27 Sep 2026", "Decision", "All", "Client documents describe the proposed system",
     "Until the Business Unit signs off each FRS, a client document presents the proposed system only.",
     "BDOI", "Wording rules of every document.", "Closed", "-"),
    ("D-003", "28 Sep 2026", "Decision", "All", "Business and technical content apart", "Business sets hold business "
     "content; technical content is in the technical documents of the set, reviewed by BDOI IT.", "BDOI",
     "DSP, DDI and ICD are the technical documents of a pack.", "Closed", "-"),
    ("D-004", "28 Sep 2026", "Decision", "All", "Workbook templates", "Fill-in workbooks carry a title block, column "
     "guide, mandatory marks, an example row and drop-downs.", "BDOI", "All workbooks.", "Closed", "-"),
    ("D-005", "08 Oct 2026", "Decision", "All", "Documents stored in S3 only", "Documents are kept in the cloud "
     "document store only.", "BDOI (answer A4)", "NFR-RET-04.", "Closed", "NFR"),
    ("D-006", "09 Oct 2026", "Decision", "All", "Backlog tool", "The working backlog is kept in the backlog tool "
     "chosen by BDOI; user stories are delivered import-ready.", "BIBS Product Owner", "USR workbook columns.",
     "Closed", "Programme Backlog"),
    ("D-007", "11 Oct 2026", "Decision", "All", "Walkthrough environment", "Business Unit walkthroughs run on the "
     "BIBS DEV environment with fictitious data.", "Product Owner", "Walkthrough Users workbooks.", "Closed", "-"),
    ("D-008", "15 Oct 2026", "Decision", "BRD-11", "No break-glass account", "When Entra ID is unavailable nobody "
     "signs in until it is restored.", "BDOI IT", "CR-UA-003 withdrawn; NFR-SEC-02.", "Closed", "FRS BRD-11 v1.6"),
    ("D-009", "15 Oct 2026", "Decision", "BRD-11", "Users from UIDM-ISC", "UIDM-ISC maintains users and their group "
     "profiles over SCIM 2.0; BIBS keeps the group-profile definitions.", "BDOI IT", "FRUM.009 and FRUM.011 "
     "withdrawn; CR-UA-011, CR-UA-014 withdrawn.", "Closed", "FRS BRD-11 v1.6; ICD v1.1"),
    ("D-010", "16 Oct 2026", "Decision", "BRD-11", "Data access by company and branch", "CR-UA-007 rescoped to "
     "company and branch scope per user.", "Product Owner", "CR Register v1.3.", "Open: BDOI approval (Q-UA-05)",
     "CR Register"),
    ("D-011", "16 Oct 2026", "Decision", "All", "Standard document set per BRD", "Each BRD pack holds FRS, BPS, UXD, "
     "RNO, ICD (where interfaces exist), DSP, DDI, USR and the review workbooks, plus a How to read this pack note; "
     "NFR, glossary and decisions are kept once at programme level.", "Product Owner", "BRD-11 first; BRD-03, "
     "BRD-06 and BRD-02 follow.", "Closed: BRD-11, BRD-03, BRD-06 and BRD-02 complete", "Document Register"),
    ("D-012", "16 Oct 2026", "Decision", "BRD-06", "Renewal FRS numbering", "BDOI's FRRN numbers are kept; "
     "duplicate and short numbers of BDOI's FRS are corrected (FRRN.004.05, 013.03, 013.04, 038.04, 026.01, 027.0x, "
     "029.0x) and the CBG Motor rules of BDOI's Annexes D and E are stated apart as FRRN.009.02.", "Product Owner",
     "FRS Renewal v1.3, section Changes from Version 1.2.", "Open: BDOI confirmation", "FRS BRD-06 v1.3"),
    ("D-013", "16 Oct 2026", "Decision", "BRD-06", "Renewal change requests", "Four change requests stay open: "
     "CR-RN-005 budget, CR-RN-008 CBG Motor rules, CR-RN-015 placement turnaround (delivered, for approval or "
     "removal) and CR-RN-024 accessibility; the other candidates of register v1.0 are BRD items, OOTB or "
     "Integration scope.", "Product Owner", "CR Register BRD-06 v1.1.", "Open: BDOI approval (Q-RN-11)",
     "CR Register"),
    ("D-014", "16 Oct 2026", "Decision", "BRD-02", "Cashiering FRS numbering", "BDOI's FRS.CSH numbers are kept; "
     "slips of BDOI's FRS v3.1 are corrected: FRS.OPS.001 and FRS.OPS.002.0x become FRS.CSH.01.01.01 and "
     "FRS.CSH.01.02.0x; spaces in numbers removed; FRS.CSH.02.02.09.10 to .19 become FRS.CSH.02.02.10 to .19 (taxes "
     "on ORs move to FRS.CSH.02.02.20); the duplicate FRS.CSH.03.01.05 (selection) becomes FRS.CSH.03.01.04.03 and the "
     "duplicate FRS.CSH.04.01.03 becomes FRS.CSH.04.01.04; the report layout of FRS.CSH.09.02 becomes BR-CSH-21.",
     "Product Owner", "FRS Operations Cashiering v3.4, section Changes from Version 3.3.", "Open: BDOI confirmation",
     "FRS BRD-02 v3.4"),
    ("D-015", "16 Oct 2026", "Decision", "BRD-02", "Cashiering change requests", "Two change requests stay open: "
     "CR-CSH-101 time rules (not delivered) and CR-CSH-102 e-mail copy of the notices (delivered, for approval or "
     "removal); D-CSH-01 and D-CSH-02 are BRD requirements.", "Product Owner", "CR Register BRD-02 v1.1.",
     "Open: BDOI approval (Q-CSH-09)", "CR Register"),
    ("O-001", "16 Oct 2026", "Observation", "All", "Number of named users", "NFR-CAP-01 states 1,344 named users; "
     "the User Access sizing uses 530.", "iorta TechNXT", "BDOI to confirm the figure; capacity targets follow.",
     "Open", "NFR"),
    ("O-002", "16 Oct 2026", "Observation", "BRD-11", "Dormant users and UIDM-ISC", "A dormant user deactivated in "
     "BIBS returns active if UIDM-ISC sends the account as active.", "iorta TechNXT", "UIDM-ISC disables the "
     "account on the notice.", "Open", "NFR section 5"),
    ("O-003", "16 Oct 2026", "Observation", "BRD-11", "Integration rate limit", "A full re-synchronisation (about "
     "2,600 calls) takes about 9 minutes at 300 calls a minute.", "iorta TechNXT", "Agree the limit with BDOI IT "
     "before the load test.", "Open", "NFR section 5"),
    ("O-004", "16 Oct 2026", "Observation", "All", "Quantum-resistant network encryption", "ASR 1.8.6 is not met.",
     "iorta TechNXT", "Plan with BDOI IT.", "Open", "NFR section 5"),
    ("O-005", "16 Oct 2026", "Observation", "All", "Backups after ransomware", "Immutable or cross-account backup "
     "copies are not yet confirmed (ASR 1.7.10).", "iorta TechNXT", "Confirm with BDOI IT.", "Open",
     "NFR-REC-03"),
    ("O-006", "16 Oct 2026", "Observation", "All", "References to the NFR register", "The System Requirements "
     "Specification and the Test Strategy v1.0 name the NFR document v1.0 and its register.", "iorta TechNXT",
     "The SRS, the Test Strategy and the traceability matrix v1.1 refer to this NFR v2.0 and its register.",
     "Closed", "SRS, Test Strategy, RTM v1.1"),
    ("O-007", "16 Oct 2026", "Observation", "BRD-02", "Packs to complete", "The BRD-02 pack is complete: FRS, BPS, "
     "UXD and RNO v3.4, ICD, DSP, DDI, USR and How to read v1.0, workbooks v1.1.", "iorta TechNXT",
     "None.", "Closed", "Document Register"),
    ("O-008", "16 Oct 2026", "Observation", "BRD-02", "Cashiering decisions open", "Minimal balance 10.00 (BRD) "
     "against -1.00 to 1.00 (BDOI's FRS); MBS has no group profile to maintain the AR and OR forms; the Check "
     "Pick-up screen stays in the Cashier profile although CSHID.009 is removed; dashboard links to applications "
     "outside BIBS wait for BDOI's list.", "iorta TechNXT", "BDOI answers Q-CSH-01, Q-CSH-06, Q-CSH-07, Q-CSH-08.",
     "Open", "FRS BRD-02 v3.4"),
    ("O-009", "16 Oct 2026", "Observation", "BRD-03", "Permissions in the Developer Specification", "A few "
     "operations of the BRD-03 Developer Specification v1.0 show the permission JOURNAL_VIEW where the service checks "
     "PRODUCT_VIEW or PKG_REPORT_VIEW (shared constant name read from another class).", "iorta TechNXT",
     "Corrected in the BRD-03 Developer Specification v1.1: every operation shows the permission its service "
     "checks.", "Closed", "DSP BRD-03 v1.1"),
    ("O-010", "16 Oct 2026", "Decision", "BRD-00", "End-to-End Conformance Register v1.1", "Conformance "
     "re-measured on a fresh SIT/UAT environment after the fixes since version 1.0. BRD-02, BRD-03, BRD-06 and BRD-11 are counted on the "
     "requirements of their BDOI-template FRS (v3.4, v1.4, v1.3, v1.6; BRD-11 on the UIDM-ISC and Entra ID model, "
     "FRUM.009 and FRUM.011 withdrawn, FRUM.014 new); the other BRDs on their BIBS FRS. Gaps G03 (except TAX-SLS and "
     "the two FY2027 true-up reports), G07, G10, G12 and G13 closed with evidence.", "iorta TechNXT",
     "Register v1.1 issued; v1.0 superseded.", "Closed", "Conformance Register v1.1"),
    ("O-011", "16 Oct 2026", "Decision", "BRD-04", "Statement of account before the due date of a cycle (gap G16)",
     "BRCLXN.058 asks for an SOA per applicable billing cycle and sets no earliest date; FR-CL-060 bills a single "
     "cycle from its plan and limits only the billing run to the cycles due in its period. Rule recorded as FR-CL-060 "
     "R6: the Operations billing user (CLX_SOA_ISSUE) may generate the SOA of a cycle still Not Due. No platform "
     "change; walkthrough D of BRD-04 now performed by the Cashiering Team Leader.", "iorta TechNXT",
     "FRS BRD-04 rule R6 added; walkthrough D passes.", "Closed", "Conformance Register v1.2"),
    ("O-012", "16 Oct 2026", "Decision", "BRD-00", "End-to-End Conformance Register v1.2", "Walkthroughs of BRD-01, "
     "BRD-04 and BRD-11 and journey step J1.1 run again on a fresh SIT/UAT environment with the updated recipes; the "
     "BRD-11 walkthroughs and J1.1 follow the UIDM-ISC and Entra ID model of FRS v1.6. Gaps G14, G15 and G16 closed "
     "with evidence. UAT Walkthrough Users and Sign-in v1.1 issued with the rewritten BRD-11 script.", "iorta TechNXT",
     "Register v1.2 issued; v1.1 superseded.", "Closed", "Conformance Register v1.2"),
    ("O-013", "16 Oct 2026", "Decision", "BRD-00", "Superseded internal records withdrawn", "The FRS comparison "
     "workbooks of BRD-02, BRD-03, BRD-06 and BRD-11 and the FRS Coverage workbooks of the same BRDs are replaced by "
     "the BDOI-template FRS packs (Fit-Gap, RTM, change register) and the Conformance Register; they are withdrawn "
     "with their builders, and the builders of the earlier BDOI-template FRS versions are removed.", "iorta TechNXT",
     "Files removed from the release folders.", "Closed", "Document Register"),
    ("O-014", "16 Oct 2026", "Decision", "BRD-00", "End-to-End Conformance Register v1.3: gaps G04, G05, "
     "G08 and G09", "Report subscriptions with run-time permission and data-scope checks (G04), field-level master "
     "data change log with maker and checker read per record by the Auditor (G05), the full insurer page with "
     "contacts, payment terms and remittance, bank accounts and linked products under maker-checker (G08) and the "
     "daily service-invoice batch per insurer with its delivery status and re-send (G09) are delivered and closed "
     "on the automated checks of the release. Remaining parts recorded on each gap: report run time and password "
     "protection, the programme-wide change log screen, Risk Participation (CRQ08) and the insurer accounts view, the "
     "merged batch file and file-transfer channel (CRQ18).", "iorta TechNXT",
     "Register v1.3 issued; v1.2 superseded. Product Maintenance FRS, UX Design, Process and Storyboards and Reports "
     "v1.5, Developer Specification v1.2, Data Dictionary v1.1, User Stories v1.1 and review workbooks v1.2 issued.",
     "Closed", "Conformance Register v1.3"),
    ("O-015", "17 Oct 2026", "Decision", "BRD-00", "End-to-End Conformance Register v1.4: case management (G01) and "
     "the remaining parts of G04, G05, G08 and G09", "Case management of the Customer Service Facility is delivered "
     "and closed (G01): logging, routing to the fulfilment units, resolution, return and CSR action, supervision, "
     "notes, documents, follow-up cases, SLA check with breach notices, Case Dashboard, case reports and Case "
     "Routing; the BRD-9 walkthrough steps P2.1 to P2.12 pass on a fresh SIT/UAT environment. Also delivered: "
     "report subscriptions at a time of day, on the last working day and with relative periods (G04); the Master "
     "Data Change Log screen with export, and the change history limited to the reader's data scope (G05); the "
     "insurer's ledger accounts on the insurer page (G08); supplementary service-invoice batches with one merged PDF "
     "and a manifest (G09). Open: password protection of confidential reports, Risk Participation (CRQ08), the "
     "file-transfer channel (CRQ18), the full case lists of the addendum (CSQ19).", "iorta TechNXT",
     "Register v1.4 issued; v1.3 superseded. Product Maintenance FRS, UX Design, Process and Storyboards and Reports "
     "v1.6, Developer Specification v1.3, User Stories v1.2 and review workbooks v1.3 issued; Customer Servicing "
     "Facility FRS and test plan v1.2 issued.", "Closed", "Conformance Register v1.4"),
    ("O-016", "17 Oct 2026", "Decision", "BRD-00", "End-to-End Conformance Register v1.5: data scope of records "
     "addressed by id, G06 and G11 closed, walkthrough recipes corrected", "Every screen action that opens or changes "
     "one record checks that the record belongs to a company and branch of the user's data access, otherwise it is "
     "answered as not found. Delivered and closed: the Layout panel of the reports with the MIS fields catalogue "
     "(G06) and New Policy and Edit of Submitted Policies (G11); the case routing notice is copied to the account "
     "officer. Walkthrough recipes corrected where they did not follow the screens or the release: BRD-00 J1.3, "
     "BRD-08 P1.3, P3.1 and P3.3 (Drop 2 process, assumption A-EB-06), BRD-13 FR-DM-125.1 and .2 (compiled and "
     "checked outside BIBS by design). Run again on a fresh SIT/UAT environment: 351 steps pass, none fail, 2 "
     "blocked. Open and waiting for BDOI: the partner portal (G02: Drop 2 scope, EBQ13, UIDM-ISC provisioning).",
     "iorta TechNXT", "Register v1.5 issued; v1.4 superseded. Product Maintenance UX Design v1.7 issued; v1.6 "
     "superseded.", "Closed", "Conformance Register v1.5"),
    ("O-017", "17 Oct 2026", "Decision", "BRD-08", "Partner portal deferred to Phase 2", "The client decided "
     "that the partner portal of Employee Benefits (insurer and client HR users, portal uploads and their review, "
     "insurer task inbox and proposal form, client HR pages) is deferred to Phase 2. No portal function exists on "
     "the platform; the portal permissions are granted to no BDOI role. Gap G02 is recorded as Deferred to Phase 2; "
     "requirements FR-EB-010 to 015, 045 and 056 and walkthrough steps P1.2 and P1.4 are listed as deferred and "
     "left out of the current-phase conformance figures. EBQ13 closed; assumption A-EB-06 (the AO records what "
     "insurers and clients send by e-mail) is the current-phase process.", "Client (BDOI)", "FRS BRD-8 v1.2 "
     "issued; Conformance Register with the Deferred to Phase 2 result.", "Closed", "FRS BRD-8 v1.2"),
    ("O-018", "17 Oct 2026", "Decision", "BRD-00", "Reinsurance deferred to Phase 2; risk participation (CRQ08) "
     "deferred with it", "The client decided that reinsurance is deferred to Phase 2. The platform keeps its "
     "reinsurance functions (reinsurer and reinsurance broker parties, receipts from a reinsurer, reinsurance "
     "settlement payments) behind one permission that no BDOI group profile holds: the screens do not offer them "
     "and the server refuses them to the BDOI roles; the super user of DEV, SIT and UAT keeps them. The Data "
     "Migration console stays with the migration roles of the FRS and the super user and is not given to the "
     "business personas. Risk participation on the insurer record (CRQ08) is a reinsurance and co-insurance "
     "participation concept and is deferred with reinsurance; the co-insurance split of an invoice stays in "
     "Operations (FR-OP-007).", "Client (BDOI)", "FRS BRD-00 v1.2 issued; gap G17 recorded as Deferred to Phase 2.",
     "Closed", "FRS BRD-00 v1.2"),
    ("O-019", "17 Oct 2026", "Decision", "BRD-02", "Remittance FRS in BDOI's format v1.0: its own set under "
     "BRD-02", "BDOI's FRS of Remittance is issued in BDOI's template as a set of its own under BRD-02 (folder "
     "BRD-02_Operations_Remittance), next to the Operations Cashiering set; the full document set and review "
     "workbooks v1.0. The drop plan places reinsurance transactions in Drop 1 Remittance; by the client decision of "
     "17-Oct-2026 reinsurance is deferred to Phase 2 (O-018), so no reinsurance remittance type is offered at "
     "go-live; Q-RM-17 asks BDOI to confirm.", "iorta TechNXT", "Remittance set v1.0 issued; document register "
     "and drop index updated.", "Closed", "BIBS FRS-BDOI BRD-02 Operations Remittance v1.0"),
    ("O-020", "17 Oct 2026", "Decision", "BRD-07", "Claims FRS in BDOI's format v1.0 is the FRS of BRD-07",
     "BDOI's FRS of Claims is issued in BDOI's template v1.0 with its full document set and review workbooks; it is "
     "the FRS of BRD-07 and the earlier sign-off set of Claims (FRS and test plan v1.1) is withdrawn, as for BRD-02, "
     "03, 06 and 11. FRCLM.002.05 (user profiles maintained by MBS) is restated to the UIDM-ISC and Entra ID model "
     "of FRS User Access Maintenance v1.6: UIDM-ISC maintains the users and their group-profile membership; the "
     "claims profile attributes are kept in BIBS under maker-checker. The Conformance Register lists the "
     "requirements of the Remittance and Claims FRS in BDOI's format as not yet assessed, counted apart.",
     "iorta TechNXT", "Claims set v1.0 issued; old Claims set v1.1 removed; document register and drop indexes "
     "updated.", "Closed", "BIBS FRS-BDOI BRD-07 Claims v1.0"),
    ("O-021", "17 Oct 2026", "Decision", "BRD-00", "End-to-End Conformance Register v1.6: Phase 2 deferrals "
     "counted apart, text-key data scope, eleven requirements shown in full", "The register gains the result "
     "Deferred to Phase 2: the partner portal (G02) and reinsurance (G17) are listed and left out of the current-phase "
     "figures, and v1.5 is restated on the same basis (636 requirements, 60.2% conformant). Delivered: the data scope "
     "of records addressed by a text business key, the e-mail and the copy to the unit of the account of the case "
     "routing notice, and automated checks of every acceptance criterion of eleven partly conformant requirements of "
     "BRD-07, BRD-09 and BRD-10. Walkthrough-script steps run again on a fresh SIT/UAT environment: 351 pass, none "
     "fail, none blocked, 2 deferred. Current phase: 394 of 636 requirements conformant (61.9%), 208 partly. The "
     "Remittance and Claims FRS in BDOI's format are listed apart as not yet assessed (65 and 75 items).",
     "iorta TechNXT", "Register v1.6 issued; v1.5 superseded.", "Closed", "Conformance Register v1.6"),
]

# Document register: BRD folders, the owner on the BDOI side, and the standard documents expected in each pack.
REGISTER_BRDS = [
    ("BRD-11", "BRD-11_User_Access_Maintenance", "User Access Maintenance", "BDOI Product Owner, User Access"),
    ("BRD-03", "BRD-03_Product_Maintenance", "Product Maintenance", "BDOI Product Owner, Product Maintenance"),
    ("BRD-06", "BRD-06_Renewal", "Renewal", "BDOI Product Owner, Renewal"),
    ("BRD-02", "BRD-02_Operations_Cashiering", "Operations Cashiering", "BDOI Product Owner, Operations"),
    ("BRD-02", "BRD-02_Operations_Remittance", "Operations Remittance", "BDOI Product Owner, Operations"),
    ("BRD-07", "BRD-07_Claims", "Claims", "BDOI Product Owner, Claims"),
]
STANDARD_SET = [
    ("README", "How to read this pack"), ("FRS", "Functional Requirements Specification"),
    ("BPS", "Business Process and Storyboards"), ("UXD", "UX Design Specification"),
    ("RNO", "Reports, Notifications and Outputs"), ("ICD", "Interface Specification (where interfaces exist)"),
    ("DSP", "Developer Specification"), ("DDI", "Data Dictionary"), ("USR", "User Stories"),
    ("RTM", "Test Cases and Traceability"), ("FitGap", "BRD vs OOTB vs Best Practice"),
    ("CR", "Change Request Register and Effort Estimate"), ("Inputs", "Business Unit Requirements Collection"),
    ("UAT", "Business Unit Walkthrough Users"),
]
