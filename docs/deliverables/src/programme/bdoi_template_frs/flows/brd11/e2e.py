"""End-to-end User Access Maintenance flow, with the change requests and the points beyond the BRD tagged
(FRS v1.4, CR Register v1.1, CR Effort Estimate v1.0)."""

from gen import tag_class


def tag(t):
    return f'<span class="cr {tag_class(t)}">{t.split(":", 1)[-1]}</span>'


# Change requests: in no BRD and not (or only partly) in the iNXT BrokerVerse OOTB platform (orange in the FRS)
CR = [("CR-UA-003", "Break-glass administrators"), ("CR-UA-007", "Supervisory / record-ownership access"),
      ("CR-UA-008", "No conflicting permissions in a role"), ("CR-UA-009", "Export file names"),
      ("CR-UA-011", "Approval times, reminders, escalation"), ("CR-UA-013", "E-mail copy of notices"),
      ("CR-UA-014", "Temporary access with an end date"), ("CR-UA-016", "Emergency role edit")]
# Beyond the BRD, offered by the iNXT BrokerVerse OOTB platform (green in the FRS): no change request, no cost
OOTB = [("OB-UA-01", "Separation-of-duties rules (was CR-UA-001)"), ("OB-UA-02", "Dormant users (was CR-UA-002)"),
        ("OB-UA-03", "Second factor of local accounts"), ("OB-UA-04", "Settings without a release"),
        ("OB-UA-05", "Access review, report schedules (was CR-UA-012)")]
# Integration and data migration scope (grey label in the FRS): not change requests
INT = [("IS-UA-01", "UIDM-ISC synchronisation (was CR-UA-005)"), ("IS-UA-02", "Revoke on SSO status (was CR-UA-006)"),
       ("IS-UA-03", "Sign-in through Entra ID / EIAM"), ("IS-UA-05", "EIAM / UIDM-ISC simulator (SIT, UAT)"),
       ("DM-UA-01", "Users and group profiles at go-live (BRD-13)")]
BRD = [("BRD 1.001.1.3", "Session-end warning (30 min), UAM BRD; was CR-UA-004"),
       ("NFR 4.03", "Source / IP address in audit log, CLM, CLXN, RI addenda; was CR-UA-010"),
       ("NFR 10.07", "One session per device, CLXN addendum; was CR-UA-015")]


def grp(title, rows, kind, cols):
    cells = "".join(f'<div>{tag(kind + n)} {t}</div>' for n, t in rows)
    return (f'<div style="margin-bottom:4px"><div style="font-weight:700;color:#0D2F5E;margin:2px 0 3px">{title}</div>'
            f'<div style="display:grid;grid-template-columns:repeat({cols},1fr);gap:2px 12px">{cells}</div></div>')


FOOT = f'''<div style="display:flex;gap:10px;font-size:11.5px;color:#1f2937">
<div style="flex:1;border:1.5px solid #F9A825;border-radius:7px;padding:5px 9px;background:#FFFDF3">
<div style="display:flex;gap:14px"><div style="flex:1">{grp("Change requests &#8211; in no BRD, for decision by BDOI (effort in the CR Effort Estimate)", CR, "", 2)}</div>
<div style="flex:1">{grp("Already BRD requirements &#8211; not change requests", BRD, "BRD:", 1)}</div></div>
<div style="display:flex;gap:14px"><div style="flex:1">{grp("Beyond the BRD, offered by iNXT BrokerVerse OOTB &#8211; no cost", OOTB, "OOTB:", 2)}</div>
<div style="flex:1">{grp("Integration / Data Migration scope &#8211; not change requests", INT, "INT:", 2)}</div></div></div>
<div style="width:300px;border:1.5px solid #c5ccd6;border-radius:7px;padding:6px 9px;line-height:1.55">
<div><b style="color:#0D47A1">&#8594;</b> Process flow &nbsp; <b style="color:#C62828">&#8594;</b> Rejected</div>
<div><b style="color:#0277BD">- - &#8594;</b> Exchange with BDOI identity systems</div>
<div><b style="color:#5E35B1">- - &#8594;</b> Returned for correction</div>
<div style="color:#4b5563">Source: FRS User Access Maintenance v1.4; CR Register v1.1, checked against all BDOI BRDs and addenda</div></div></div>'''


SPEC = dict(
    size=(1800, 1290), foot=200,
    title="BDO BIBS &#8211; User Access Maintenance &#8211; End-to-End Process Flow",
    tag="Based on FRS v1.4<br>Provisioning option (a), as delivered",
    stages=["1. Identity &amp; Sign-in", "2. Request &amp; Validation", "3. Approval", "4. Implementation", "5. Notification", "6. Reporting &amp; Audit"],
    lanes=[("EIAM", "BDO Enterprise Identity<br>Access Management on<br>Microsoft Entra ID", 0.72),
           ("UIDM-ISC", "BDOI identity<br>governance", 0.62),
           ("Requestor", "Business user / HR / Admin", 1.0),
           ("Approver(s)", "Approver(s) in order,<br>Second Approver,<br>Information Security", 1.15),
           ("System (BIBS)", "", 1.45),
           ("Reports &amp; Audit", "Auditor, Information Security", 0.75)],
    boxes=[
        ("E1", 0, 1, .04, .1, .92, "id", "Single sign-on", ["Windows ID / user principal name", "EIAM password, second factor, BDO device"], ["INT:IS-UA-03"]),
        ("U1", 1, 0, .04, .1, .92, "id", "Joiner / mover / leaver / rehire", ["Approved in UIDM-ISC; UIDM request no."], ["INT:IS-UA-01"]),
        ("U2", 1, 5, .04, .1, .92, "id", "Access certification campaign", ["Reads the BIBS extract"]),
        ("R1", 2, 1, .04, .05, .92, "req", "Raise request (Access Requests)", [
            "User request: assign / change group profile, revoke", "Group-profile request: new, modify, deactivate, reactivate",
            "Bulk request: template and error file", "Effective date, justification, attachments; approvers in order; draft"], ["CR-UA-014"]),
        ("R2", 2, 2, .1, .2, .8, "ret", "Correct and resubmit, or void", ["Returned request edited by the requestor"]),
        ("R3", 2, 4, .06, .2, .88, "ntf", "My Requests", ["Status: submitted, pending, returned, approved, rejected, implemented"]),
        ("A1", 3, 2, .08, .05, .84, "apr", "Review (Assigned to Me)", ["Details, attachments", "Permissions added / removed", "Separation-of-duties notice"]),
        ("D1", 3, 2, .22, .7, .56, "dec", "Decision?", []),
        ("A2", 3, 3, .03, .05, .55, "apr", "Second approval", ["High / Admin privilege", "Not the requestor", "Information Security for SoD rules"], ["OOTB:OB-UA-01"]),
        ("RJ", 3, 3, .6, .62, .38, "rej", "Rejected", ["With reason"]),
        ("B0", 4, 0, .04, .04, .92, "sys", "Identity Synchronisation", [
            "Create / change / deactivate / reactivate user with the UIDM no.", "Leaver or SSO inactive: deactivate, end sessions",
            "Refused event queued and alerted; Synchronise Now"], ["INT:IS-UA-01", "INT:IS-UA-02"]),
        ("BG", 4, 0, .04, .66, .92, "sys", "Break-glass sign-in", ["Two named admins, local password + second factor; alert"], ["CR-UA-003"]),
        ("BV", 4, 1, .04, .04, .92, "sys", "Checks on submit", ["Mandatory fields, profile state", "Separation-of-duties rules", "No conflicting permissions in a role"], ["OOTB:OB-UA-01", "CR-UA-008"]),
        ("BS", 4, 1, .04, .55, .92, "sys", "Session and data access", ["Menus of the group profile", "Time-out and session-end warning", "Own / supervised records"], ["BRD:BRD 1.001.1.3", "BRD:NFR 10.07", "CR-UA-007"]),
        ("BT", 4, 2, .06, .55, .88, "sys", "Time rules", ["2 working days per stage", "Reminder, then escalation to the Business Administrator"], ["CR-UA-011"]),
        ("BI", 4, 3, .03, .04, .55, "sys", "Implement", ["On the effective date", "Group profiles, permissions", "Access-change log, before / after"]),
        ("BD", 4, 3, .03, .62, .94, "sys", "Daily runs", ["Dormant 90 days: deactivate (notice 7 days before)", "Temporary access removed at end date"], ["OOTB:OB-UA-02", "CR-UA-014"]),
        ("BN", 4, 4, .06, .1, .88, "ntf", "Notices", ["In BIBS: to approve, returned, decided, implemented", "E-mail copy by preference"], ["CR-UA-013"]),
        ("AL", 5, 4, .06, .1, .88, "aud", "User Access Audit Log", ["Every action, before / after", "IP address and role"], ["BRD:NFR 4.03"]),
        ("AR", 5, 5, .04, .1, .92, "aud", "Reports", ["User Access Report, Matrix, group profiles; export", "Monthly review to Information Security"], ["CR-UA-009", "OOTB:OB-UA-05"]),
    ],
    edges=[
        ["U1", "B0", "i", "", "down"], ["E1", "R1", "i", "", "down"],
        ["R1", "BV", "n", "Submit", "down"], ["BV", "A1", "n", "", ""],
        ["A1", "D1", "n", "", "down"], ["D1", "A2", "n", "Approve", "", [893, 632]], ["D1", "RJ", "r", "Reject", "", [1022, 692]],
        ["D1", "R2", "t", "Return", "lu", [688, 645]], ["R2", "A1", "t", "", "down"],
        ["A2", "BI", "n", "", "down"], ["RJ", "BN", "r", "", "hd"],
        ["BI", "BN", "n", "", ""], ["BN", "R3", "o", "", "up"],
        ["BN", "AL", "a", "", "down"], ["AL", "AR", "a", "", ""], ["AR", "U2", "i", "Extract", "up"],
    ],
    footer_html=FOOT,
)
