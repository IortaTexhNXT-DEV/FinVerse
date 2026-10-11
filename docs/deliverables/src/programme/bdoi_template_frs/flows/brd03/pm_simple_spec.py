"""Product Maintenance - simplified process flow (FRS BRD-03 v1.3, section 1.4.2). Drawn from the FRS only:
FRPM.001 to FRPM.030, Annex O (workflow and approvals), Annex P (notices) and the BRD (BRPM.001 to BRPM.024)."""

LEG = ('<div style="display:flex;gap:26px;font-size:12px;align-items:center;color:#374151">'
       '<span><b style="color:#0D47A1">&#8594;</b> Process flow</span>'
       '<span><b style="color:#0277BD">- - &#8594;</b> Exchange with insurers / other systems</span>'
       '<span><b style="color:#5E35B1">- - &#8594;</b> Returned for correction</span>'
       '<span><b style="color:#EF6C00">&#8594;</b> Notice / advisory</span>'
       '<span style="margin-left:auto">Source: FRS v1.3, FRPM.001&#8211;030, Annexes O and P</span></div>')

SPEC = dict(
    size=(1600, 1010), foot=26,
    title="BDO BIBS &#8211; Product Maintenance &#8211; Simplified Process Flow",
    tag="Based on FRS v1.3<br>Package products (BRD-03)",
    stages=["1. Request", "2. Review &amp; Approval", "3. Negotiation", "4. ManCom Sign-off",
            "5. Set-up &amp; Validation", "6. Release &amp; Monitoring"],
    lanes=[("Marketing", "Account Officer,<br>TL, TH, Unit Head", 1.0),
           ("TSU", "Officer, Team Lead,<br>TSU Head", 1.05),
           ("Insurers", "By e-mail", 0.7),
           ("ManCom", "Selected approvers,<br>then the President", 0.75),
           ("MBS", "Set-up and validator", 1.0),
           ("System (BIBS)", "", 1.0)],
    boxes=[
        ("M1", 0, 0, .06, .1, .88, "req", "Raise package request", ["Generic or client-specific", "Attachments; draft or submit"]),
        ("M2", 0, 1, .06, .1, .88, "apr", "Marketing approval", ["TL, TH, then Unit Head in turn", "Approve, return or reject"]),
        ("M3", 0, 5, .06, .1, .88, "ntf", "Advisory and renewal", ["Package advisory received", "Renewal request from expiry list"]),
        ("T1", 1, 1, .06, .1, .88, "apr", "TSU review", ["Team Lead recommends", "TSU Head approves (not the recommender)"]),
        ("T2", 1, 2, .06, .1, .88, "req", "Quotation Slip and Comparative Table", ["One slip per insurer per round", "Master and client views"]),
        ("T3", 1, 3, .06, .1, .88, "req", "Product requirements", ["Negotiated terms reviewed by TSU Head and Marketing"]),
        ("I1", 2, 2, .06, .14, .88, "id", "Insurer responses", ["Approved, Not Covered, Other", "Further rounds if needed"]),
        ("C1", 3, 3, .06, .12, .88, "apr", "ManCom approval", ["Approvers in parallel, then President"]),
        ("B1", 4, 4, .06, .08, .88, "sys", "Package set-up", ["New version: rates, coverages, clauses, incentive rules"]),
        ("B2", 4, 4, .06, .58, .88, "apr", "Post-setup validation", ["Checklist by another user"]),
        ("S0", 5, 0, .3, .12, .66, "sys", "Request registered", ["Reference no., stage, service level", "Audit trail"]),
        ("S1", 5, 5, .06, .06, .88, "sys", "Package released", ["Used by New Business, Renewal, Operations", "Product master transfer to other systems"]),
        ("S2", 5, 3, .06, .3, .88, "aud", "Monitoring and reports", ["Expiry alerts from 90 days", "Dashboard, reports, audit logs"]),
    ],
    edges=[
        ["M1", "M2", "n", "", ""], ["M1", "S0", "n", "", "down"],
        ["M2", "T1", "n", "Approve", "down"], ["T1", "M1", "t", "Return", "hu", [318, 360]],
        ["T1", "T2", "n", "", ""], ["T2", "I1", "i", "Slip", "down"],
        ["T2", "T3", "n", "", ""], ["T3", "C1", "n", "Submit", "down"],
        ["C1", "B1", "n", "", ""], ["B1", "B2", "n", "", "down"],
        ["B2", "S1", "n", "", ""], ["S1", "M3", "o", "Advisory", "up"],
        ["S1", "S2", "a", "", "bot968"],
    ],
    footer_html=LEG,
)
