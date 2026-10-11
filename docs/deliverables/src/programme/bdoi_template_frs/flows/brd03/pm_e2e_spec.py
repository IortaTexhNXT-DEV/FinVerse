"""Product Maintenance - end-to-end process flow (FRS BRD-03 v1.3, section 4). Drawn from the FRS only (FRPM.001 to
FRPM.030, Annexes O, P, R, S, T, Y) and the BRD; CR tags from Annex Y and the CR effort workbook."""

CR_NEW = [("CR-PM-101", "Accessibility (WCAG 2.2 AA) of the module screens")]
CR_EXT = []
BRD = [("NFR 10.07", "One session per device (Collections addendum)"),
       ("NFR 4.03", "Source / IP address in the audit log (CLM, CLXN, RI addenda)")]


def _tag(kind, n):
    cls = {"BRD": "brd", "EXT": "ext"}.get(kind, "")
    return f'<span class="cr {cls}">{n}</span>'


def _grp(title, rows, kind, cols):
    cells = "".join(f'<div>{_tag(kind, n)} {t}</div>' for n, t in rows)
    return (f'<div style="margin-bottom:4px"><div style="font-weight:700;color:#0D2F5E;margin:2px 0 3px">{title}</div>'
            f'<div style="display:grid;grid-template-columns:repeat({cols},1fr);gap:2px 12px">{cells}</div></div>')


FOOT = f'''<div style="display:flex;gap:10px;font-size:11.5px;color:#1f2937">
<div style="flex:1;border:1.5px solid #F9A825;border-radius:7px;padding:5px 9px;background:#FFFDF3">
<div style="display:flex;gap:14px"><div style="flex:1.5">{_grp("Change requests &#8211; not in any BRD", CR_NEW, "", 1)}</div>
<div style="flex:1">{_grp("Already BRD requirements &#8211; not change requests", BRD, "BRD", 1)}</div></div></div>
<div style="width:300px;border:1.5px solid #c5ccd6;border-radius:7px;padding:6px 9px;line-height:1.55">
<div><b style="color:#0D47A1">&#8594;</b> Process flow &nbsp; <b style="color:#C62828">&#8594;</b> Rejected</div>
<div><b style="color:#0277BD">- - &#8594;</b> Exchange with insurers / systems</div>
<div><b style="color:#5E35B1">- - &#8594;</b> Returned for correction</div>
<div><b style="color:#EF6C00">&#8594;</b> Notice &nbsp; <b style="color:#7B1FA2">&#8594;</b> Audit and reports</div>
<div style="color:#4b5563">Source: FRS Product Maintenance v1.3; CR effort workbook v1.0 checked against all BDOI BRDs and addenda</div></div></div>'''

SPEC = dict(
    size=(1800, 1350), foot=138,
    title="BDO BIBS &#8211; Product Maintenance &#8211; End-to-End Process Flow",
    tag="Based on FRS v1.3<br>Package products (BRD-03)",
    stages=["1. Request", "2. Review &amp; Approval", "3. Negotiation", "4. ManCom Sign-off",
            "5. Set-up &amp; Validation", "6. Release &amp; Monitoring"],
    lanes=[("Marketing", "Account Officer,<br>TL, TH, Unit Head", 1.0),
           ("TSU", "Officer, Team Lead,<br>TSU Head", 1.3),
           ("Insurers", "By protected e-mail", 0.62),
           ("ManCom", "Selected approvers,<br>then the President", 0.7),
           ("MBS", "Set-up, validator,<br>incentive maintainers", 1.15),
           ("System (BIBS)", "", 1.3),
           ("Other modules<br>and systems", "New Business, Renewal,<br>Operations, BDOI systems", 0.62),
           ("Reports &amp; Audit", "MBS, Auditor", 0.62)],
    boxes=[
        ("M1", 0, 0, .05, .07, .9, "req", "Raise package request", ["Generic or client-specific", "Request type: new, update, renew, reactivate", "Draft, attachments, submit"]),
        ("M2", 0, 1, .05, .07, .9, "apr", "Marketing approval", ["TL, TH, then Unit Head in turn", "Approve, return or reject with remarks"]),
        ("M3", 0, 3, .05, .1, .9, "apr", "Review negotiated terms", ["Client-specific: accept or ask for changes"]),
        ("M4", 0, 5, .05, .07, .9, "ntf", "Advisory and renewal", ["Package advisory (in system, e-mail)", "Renewal request from the expiry list"]),
        ("T1", 1, 1, .05, .06, .9, "apr", "TSU review", ["Team Lead recommends", "TSU Head approves, not the recommender", "Return or reject"]),
        ("T2", 1, 2, .05, .06, .9, "req", "Quotation Slip per insurer", ["Approved before sending (not by preparer)", "Protected PDF, password sent apart"]),
        ("T3", 1, 2, .05, .7, .9, "req", "Comparative Table", ["Master and client views per round"]),
        ("T4", 1, 3, .05, .06, .9, "req", "Product requirements", ["Approvers selected for ManCom"]),
        ("T5", 1, 4, .05, .1, .9, "req", "Request for deployment", ["To the MBS work queue"]),
        ("I1", 2, 2, .05, .1, .9, "id", "Insurer responses", ["Approved, Not Covered, Other; rounds"]),
        ("C1", 3, 3, .05, .1, .9, "apr", "ManCom approval", ["In parallel, then the President", "Approve, return or reject"]),
        ("B1", 4, 4, .05, .06, .9, "sys", "Package set-up", ["New version (copy of the one in force)", "Rates, coverages, clauses, field rules", "Incentive rules (maker-checker)"]),
        ("B2", 4, 4, .05, .72, .9, "apr", "Post-setup validation", ["Checklist by another user; release"]),
        ("S0", 5, 0, .3, .05, .66, "sys", "Request register", ["Reference number, stage stepper", "One session per device"], ["BRD:NFR 10.07"]),
        ("S1", 5, 1, .05, .05, .9, "sys", "Workflow rules", ["Mandatory fields and checks", "Stage service levels, reminders", "Reassignment of work"]),
        ("S2", 5, 2, .05, .05, .9, "ntf", "Notices", ["Stage moves, assignments, decisions", "In system with e-mail copy"]),
        ("SA", 5, 0, .05, .62, .9, "sys", "Screens of the module", ["Role-based menus and actions"], ["CR-PM-101"]),
        ("S3", 5, 4, .05, .05, .9, "sys", "Product master records", ["Approved changes and deactivations", "Transfer file every 30 minutes"]),
        ("S4", 5, 5, .05, .05, .9, "sys", "Daily expiry check", ["Alerts from 90 days; anniversary", "Renewal request drafted; deactivation"]),
        ("X1", 6, 5, .05, .1, .9, "id", "Consumers of the package", ["New Business, Renewal, Operations", "Other BDOI systems (product master)"]),
        ("A1", 7, 4, .05, .1, .9, "aud", "Audit logs", ["Every action, user, time, IP address", "Old and new values of masters"], ["BRD:NFR 4.03"]),
        ("A2", 7, 5, .05, .1, .9, "aud", "Reports and dashboard", ["Placement Update, expiry, versions", "Export to PDF, Excel, CSV"]),
    ],
    edges=[
        ["M1", "M2", "n", "", ""], ["M1", "S0", "n", "", "down"],
        ["M2", "T1", "n", "Approve", "down"], ["T1", "M1", "t", "Return", "hu", [345, 333]],
        ["T1", "T2", "n", "", ""], ["T3", "I1", "i", "", "down"], ["T2", "T3", "n", "", "down"],
        ["T3", "T4", "n", "", ""], ["T4", "M3", "n", "", "up"], ["T4", "C1", "n", "Submit", "down"],
        ["C1", "T5", "n", "", ""], ["T5", "B1", "n", "", "down"], ["B1", "B2", "n", "", "down"],
        ["B2", "S3", "n", "", "down"], ["S3", "S4", "n", "", ""], ["S4", "X1", "i", "", "down"],
        ["S4", "M4", "o", "Alert", "up"], ["S3", "A1", "a", "", "down"], ["A1", "A2", "a", "", ""],
        ["S1", "S2", "o", "", ""],
    ],
    footer_html=FOOT,
)
