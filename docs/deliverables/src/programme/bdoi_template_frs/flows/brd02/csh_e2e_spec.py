"""Operations Cashiering - end-to-end process flow (FRS BRD-02 v3.3, Appendix C). Drawn from the FRS only
(FRS.CSH.01 to FRS.CSH.10, Appendices C, D, G, J, M, N, O, W) and the Operations BRD; CR tags from Appendix W."""

CR_NEW = [("CR-CSH-101", "Time rules of the Cashiering stages: overdue marker and reminder"),
          ("CR-CSH-102", "Each user chooses an e-mail copy of the notices")]
BRD = [("NFR 10.07", "One session per device (Collections addendum)"),
       ("NFR 4.03", "Source / IP address in the audit log (CLM, CLXN, RI addenda)")]


def _tag(kind, n):
    cls = {"BRD": "brd", "EXT": "ext"}.get(kind, "")
    return f'<span class="cr {cls}">{n}</span>'


def _grp(title, rows, kind):
    cells = "".join(f'<div>{_tag(kind, n)} {t}</div>' for n, t in rows)
    return (f'<div style="margin-bottom:4px"><div style="font-weight:700;color:#0D2F5E;margin:2px 0 3px">{title}</div>'
            f'<div>{cells}</div></div>')


FOOT = f'''<div style="display:flex;gap:10px;font-size:11.5px;color:#1f2937">
<div style="flex:1;border:1.5px solid #F9A825;border-radius:7px;padding:5px 9px;background:#FFFDF3">
<div style="display:flex;gap:14px"><div style="flex:1">{_grp("Change requests &#8211; not in any BRD", CR_NEW, "")}
</div>
<div style="flex:1">{_grp("Already BRD requirements &#8211; not change requests", BRD, "BRD")}</div></div></div>
<div style="width:300px;border:1.5px solid #c5ccd6;border-radius:7px;padding:6px 9px;line-height:1.55">
<div><b style="color:#0D47A1">&#8594;</b> Process flow &nbsp; <b style="color:#EF6C00">&#8594;</b> Notice</div>
<div><b style="color:#0277BD">- - &#8594;</b> Files and other systems</div>
<div><b style="color:#5E35B1">- - &#8594;</b> Returned for correction</div>
<div><b style="color:#7B1FA2">&#8594;</b> Accounting, reports and audit</div>
<div style="color:#4b5563">Source: FRS Operations Cashiering v3.3; CR effort workbook v1.0 checked against all BDOI BRDs and addenda</div></div></div>'''

SPEC = dict(
    size=(1800, 1372), foot=140,
    title="BDO BIBS &#8211; Operations Cashiering &#8211; End-to-End Process Flow",
    tag="Based on FRS v3.3<br>Cashiering (BRD-02)",
    stages=["1. Payment Received", "2. Receipt Record", "3. Posting", "4. Application &amp; GL",
            "5. Exceptions", "6. Reports &amp; Audit"],
    lanes=[("Payors and<br>channels", "Clients, insurers,<br>banks, MFT, PMS", 0.75),
           ("Cashier", "Head Office<br>and branches", 1.15),
           ("Approver / Poster", "Cashiering TL / TH", 0.85),
           ("System (BIBS)", "", 1.45),
           ("Marketing", "Own unit", 0.6),
           ("Accounting units", "Accounting Controls,<br>Comptrollership,<br>Commission Receivable", 0.75),
           ("Disbursement", "Refunds", 0.55),
           ("Reports &amp; Audit", "Cashiering, Auditor", 0.65)],
    boxes=[
        ("P1", 0, 1, .05, .1, .9, "id", "Payment received", ["Cash, check, bank credit, insurer commission"]),
        ("P2", 0, 0, .05, .1, .9, "id", "Payment files", ["Bills Payment, CLPC, Trade, Direct Credit, post-dated checks (MFT)", "Delivered in the Integration scope"]),
        ("C1", 1, 1, .05, .05, .9, "req", "Create AR / OR record", ["Premium or non-premium; OR Head Office", "Accounts, amounts, bank; validations", "Commission Schedule upload"]),
        ("C2", 1, 0, .05, .1, .9, "req", "Upload payment files", ["Or received through MFT", "Post-dated checks to the warehouse"]),
        ("C3", 1, 4, .05, .05, .9, "req", "Cancel or reinstate", ["Record with reason; partial or full"]),
        ("C4", 1, 4, .05, .58, .9, "req", "Unapplied disposition", ["Apply, refund, reverse; submit"]),
        ("A1", 2, 2, .05, .1, .9, "apr", "Post or return", ["Not the creator; series checked", "Number issued at posting"], ["CR-CSH-101"]),
        ("A2", 2, 4, .05, .1, .9, "apr", "Approve disposition / reversal", ["Team approval, not the submitter"]),
        ("S0", 3, 0, .05, .05, .9, "sys", "Automatic payment processing", ["Rows matched to accounts", "Run report; failures alerted"]),
        ("SS", 3, 0, .05, .6, .9, "sys", "Session and access", ["Role menus; one session per device"], ["BRD:NFR 10.07"]),
        ("S1", 3, 3, .05, .05, .9, "sys", "Receipt issued and applied", ["AR / OR printed (batch print)", "Accounts paid; minimal balance, excess", "Accounting entries to the GL"]),
        ("S2", 3, 4, .05, .05, .9, "sys", "Unapplied payments", ["Pre-booked, unknown, excess", "Disposition status; ageing"]),
        ("S3", 3, 2, .05, .62, .9, "ntf", "Notices", ["For posting, returned, decided", "E-mail copy by preference"], ["CR-CSH-102"]),
        ("S4", 3, 1, .05, .62, .9, "sys", "Number series", ["Maintained and authorised", "Warning before depletion"]),
        ("M1", 4, 4, .05, .1, .9, "id", "Own unit's unapplied", ["View and propose disposition"]),
        ("G1", 5, 4, .05, .1, .9, "id", "Payment reversal request", ["Accounting Controls, to the Cashiering TL"]),
        ("D1", 6, 5, .05, .1, .9, "id", "Refund paid", ["Returned to Cashiering for reversal"]),
        ("R1", 7, 5, .05, .08, .9, "aud", "Reports and audit log", ["Daily cash reconciliation, unapplied", "Every action, user, time, IP address"], ["BRD:NFR 4.03"]),
    ],
    edges=[
        ["P1", "C1", "n", "", "down"], ["P2", "C2", "i", "", "down"], ["C2", "S0", "n", "", "down"],
        ["C1", "A1", "n", "Submit", "vd"], ["A1", "C1", "t", "Return", "vu"],
        ["A1", "S1", "n", "Post", "hd"], ["A1", "S3", "o", "", "down"], ["S0", "S1", "n", "", ""],
        ["S1", "S2", "n", "", ""], ["C3", "A2", "n", "", "down"], ["C4", "A2", "n", "", "down"],
        ["A2", "S2", "n", "", "down"], ["S2", "M1", "i", "", "down"],
        ["S2", "D1", "i", "Refund", "hd"], ["S1", "R1", "a", "", "vd"],
    ],
    footer_html=FOOT,
)
