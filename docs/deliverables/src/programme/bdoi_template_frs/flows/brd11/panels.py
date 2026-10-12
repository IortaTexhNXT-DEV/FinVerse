"""Spec of the one-page "User Access Maintenance - Processes at a Glance" picture (FRS BRD-11 v1.4, Option (a)).
Rendered by render_panels.sh with panels_gen.py. Untagged text = BRD; {CR:..} orange; {OOTB} green; {INT:..} grey."""

UIDM = "{INT:Integration scope – UIDM-ISC}"


def B(title, lines=(), kind=""):
    return ("box", title, list(lines), kind)


def F(*steps):
    return ("flow", list(steps))


def head(no, title):
    return ("box", f"{no}  {title}", [], "id")


SPEC = {
    "size": (1680, 1240),
    "title": "User Access Maintenance – Processes at a Glance",
    "tag": "Based on FRS v1.4 – Option (a),|BDOI decision of 26-Sep-2026",
    "rows": "290px minmax(0,1fr) 262px",
    "legend": ["BRD requirement (black text)", "{OOTB} OOTB, beyond the BRD",
               "{CR:CR-UA-nnn} change request", "{INT:grey} integration / data-migration scope",
               "Requests for user access are raised and approved in UIDM-ISC; only group-profile requests are raised in BIBS."],
    "panels": [
        {"no": "1", "title": "User lifecycle and access requests (UIDM-ISC → BIBS)",
         "sub": "UIDM-ISC is the source of user lifecycle and user-access requests; BIBS applies the approved events",
         "grid": (1, 1, 6, 1), "colour": "#0277BD", "fill": "#F3FAFE",
         "body": [("cols", [
             B("UIDM-ISC " + UIDM, ["Raise, correct, cancel, view and bulk user-access requests",
                                     "Approve / reject / return; request notices",
                                     "Joiner, mover, leaver, rehire events with the UIDM Request No."], "id"),
             ("arrow", "Approved|event"),
             B("BIBS – Identity synchronisation", ["Validates and applies the event: user, Windows ID and user fields, branch, group profiles",
                                                   "Effective-dated access changes applied on their date",
                                                   "Records the UIDM Request No. in the audit log"], "sys"),
             F(B("Accepted", ["User created / updated / deactivated"], "sys"),
               B("Refused", ["Identity Synchronisation queue with reason",
                             "Alert to System Administrators",
                             "Correct, then Synchronise Now"], "rej")),
         ])]},
        {"no": "2", "title": "Sign-in through EIAM", "sub": "Microsoft Entra ID single sign-on",
         "grid": (7, 1, 3, 1), "colour": "#5E35B1", "fill": "#F7F4FD",
         "body": [F(B("User signs in with Entra ID / EIAM " + "{INT:Integration scope – EIAM}", [], "ret"),
                    B("BIBS checks the user", ["Active BIBS user, not locked or deactivated",
                                               "SSO status revoked → access refused {INT:Integration scope}"], "sys"),
                    B("Break-glass administrators {CR:CR-UA-003}", ["Two named System Administrators; local sign-in with second factor",
                                                                 "Every break-glass sign-in recorded and alerted"], "apr"))]},
        {"no": "3", "title": "Application access and session", "sub": "Menus and data as per group profiles",
         "grid": (10, 1, 3, 1), "colour": "#00695C", "fill": "#F1FAF8",
         "body": [("list", ["Menus, screens and actions from the user's group profiles",
                            "Supervisory and record-ownership access {CR:CR-UA-007}",
                            "One session per device (Collections NFR 10.07): a sign-in on another device ends the earlier one",
                            "One session for all browser tabs",
                            "Inactivity warning at 15 minutes (Stay Logged In / Log Out)",
                            "Session ends after 30 minutes of inactivity",
                            "Sign-in and sign-out recorded in the audit log"])]},
        {"no": "4", "title": "User maintenance processes",
         "sub": "4.1, 4.2, 4.4 arrive from UIDM-ISC; 4.3 is the only request raised and approved in BIBS",
         "grid": (1, 2, 12, 1), "colour": "#1565C0", "fill": "#F5F9FF",
         "body": [("cols", [
             F(head("4.1", "Joiner"), B("", ["Joiner request approved in UIDM-ISC " + UIDM]),
               B("", ["Event with UIDM Request No. sent to BIBS"]),
               B("", ["BIBS creates the user: Windows ID, user fields, branch, group profiles"], "sys"),
               B("", ["Refused → Identity Synchronisation queue, alert"], "rej"),
               B("", ["User signs in through EIAM"]), B("", ["Audit log entry"], "aud")),
             F(head("4.2", "Mover / access change"), B("", ["Access change requested and approved in UIDM-ISC " + UIDM]),
               B("", ["Event sent to BIBS"]),
               B("", ["Effective date reached?"], "dec"),
               B("", ["BIBS updates group profiles, branch and user fields on the effective date"], "sys"),
               B("", ["Rehire: account reactivated with the new access"], "sys"),
               B("", ["Audit log entry"], "aud")),
             F(head("4.3", "Group-profile request (BRD 3.002)"),
               B("", ["Business Administrator creates / changes / deactivates a group profile request in BIBS"]),
               B("", ["Submit: system runs the separation-of-duties check (rules authorised by Information Security)"], "sys"),
               B("", ["Approver(s) in the configured order; requestor cannot approve own request"], "apr"),
               ("cols", [B("", ["Reject: request closed with its reason"], "rej"),
                         B("", ["Return for correction → requestor"], "ret")]),
               B("", ["High / Admin privilege → Second Approver (not the first approver)", "Approval time limits, reminders {CR:CR-UA-011}"], "apr"),
               B("", ["Change applied; notice; audit log"], "aud")),
             F(head("4.4", "Leaver"), B("", ["Leaver event from UIDM-ISC " + UIDM]),
               B("", ["BIBS deactivates the user"], "sys"),
               B("", ["Open sessions ended"], "sys"),
               B("", ["SSO status revoked " + "{INT:Integration scope}"]),
               B("", ["Refused → Identity Synchronisation queue, alert, Synchronise Now"], "rej"),
               B("", ["Audit log entry"], "aud")),
             F(head("4.5", "Temporary access {CR:CR-UA-014}"),
               B("", ["End date entered with the grant {CR:CR-UA-014}"], "apr"),
               B("", ["Access granted from its effective date (BRD)"], "sys"),
               B("", ["End date reached?"], "dec"),
               B("", ["Access removed automatically; notice"], "sys"),
               B("", ["Audit log entry"], "aud")),
             F(head("4.6", "Dormant user deactivation {OOTB}"),
               B("", ["Daily run checks the last sign-in {OOTB}"]),
               B("", ["Warning before deactivation, as configured {OOTB}"], "apr"),
               B("", ["90 days without sign-in?"], "dec"),
               B("", ["User deactivated; notice {OOTB}"], "sys"),
               B("", ["Reactivation only through UIDM-ISC " + UIDM]),
               B("", ["Audit log entry"], "aud")),
         ])]},
        {"no": "5", "title": "Access certification", "sub": "Periodic access review",
         "grid": (1, 3, 3, 1), "colour": "#2E7D32", "fill": "#F3FAF3",
         "body": [F(B("Periodic access review {OOTB}", ["Users, group profiles and permissions listed for review"], "sys"),
                    B("", ["Reviewers confirm or ask for removal {OOTB}"], "apr"),
                    B("", ["Removals raised in UIDM-ISC " + UIDM]),
                    B("", ["Review evidence kept {OOTB}"], "aud"))]},
        {"no": "6", "title": "Separation of duties and privileged access", "sub": "",
         "grid": (4, 3, 5, 1), "colour": "#C62828", "fill": "#FFF6F6",
         "body": [("cols", [
             B("Separation of duties", ["Conflicting-permission rules authorised by Information Security",
                                        "Checked by the system on submit; conflicts blocked or flagged",
                                        "Second Approver for High / Admin privilege",
                                        "Requestor, approver and user concerned are different people"], "rej"),
             B("Privileged access", ["Break-glass administrators {CR:CR-UA-003}",
                                     "Emergency role edit: off by default, switched on with Information Security approval, audited and alerted {CR:CR-UA-016}",
                                     "Group profiles change only through approved and implemented requests"], "apr")])]},
        {"no": "7", "title": "Audit and reporting", "sub": "",
         "grid": (9, 3, 4, 1), "colour": "#7B1FA2", "fill": "#FBF5FD",
         "body": [("cols", [
             ("list", ["User Access Report (Annex AC)", "Access matrix", "Group-profile and membership reports",
                       "Audit log: requests, approvals, sign-ins, sessions, synchronisation"]),
             ("list", ["External access-control list interface (IS-UA-04) {INT:Integration scope}",
                       "Identity Synchronisation queue: refused events, alert, Synchronise Now"])])]},
    ],
}
