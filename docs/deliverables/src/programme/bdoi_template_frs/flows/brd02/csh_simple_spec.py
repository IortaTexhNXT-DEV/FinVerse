"""Operations Cashiering - simplified process flow (FRS BRD-02 v3.4; Process and Storyboards v3.4). Drawn from the FRS only:
FRS.CSH.01 to FRS.CSH.10, and the Operations BRD (BRQID, CSHID)."""

LEG = ('<div style="display:flex;gap:26px;font-size:12px;align-items:center;color:#374151">'
       '<span><b style="color:#0D47A1">&#8594;</b> Process flow</span>'
       '<span><b style="color:#0277BD">- - &#8594;</b> Files and exchanges with other systems</span>'
       '<span><b style="color:#5E35B1">- - &#8594;</b> Returned for correction</span>'
       '<span><b style="color:#7B1FA2">&#8594;</b> Accounting, reports and audit</span>'
       '<span style="margin-left:auto">Source: FRS v3.4, FRS.CSH.01&#8211;10</span></div>')

SPEC = dict(
    size=(1600, 1010), foot=26,
    title="BDO BIBS &#8211; Operations Cashiering &#8211; Simplified Process Flow",
    tag="Based on FRS v3.4<br>Cashiering (BRD-02)",
    stages=["1. Payment Received", "2. Receipt Record", "3. Posting", "4. Application &amp; GL",
            "5. Exceptions", "6. Reports &amp; Audit"],
    lanes=[("Payors and<br>channels", "Clients, insurers, banks,<br>payment files (MFT)", 0.85),
           ("Cashier", "Head Office<br>and branches", 1.0),
           ("Approver / Poster", "Cashiering TL / TH", 0.95),
           ("System (BIBS)", "", 1.2),
           ("Other units", "Marketing, Accounting,<br>Disbursement", 0.9)],
    boxes=[
        ("P1", 0, 0, .06, .12, .88, "id", "Payment received", ["Cash, check, bank credit", "Bills Payment, CLPC, Trade, Direct Credit, PDC files"]),
        ("C1", 1, 1, .06, .1, .88, "req", "Create AR or OR record", ["Payor, accounts, amounts, bank", "Save, then submit"]),
        ("C2", 1, 4, .06, .1, .88, "req", "Cancel or reinstate", ["Record with reason, submitted"]),
        ("A1", 2, 2, .06, .1, .88, "apr", "Post or return", ["Not the creator", "Number issued at posting"]),
        ("S0", 3, 0, .06, .1, .88, "sys", "Automatic processing", ["Files matched to accounts", "Run report; unknown to Unapplied"]),
        ("S1", 3, 3, .06, .08, .88, "sys", "Receipt issued and applied", ["AR / OR printed", "Accounts paid; minimal balance, excess", "Accounting entries"]),
        ("S2", 3, 4, .06, .1, .88, "sys", "Unapplied payments", ["Disposition, approval", "Refund or reversal"]),
        ("R1", 3, 5, .06, .1, .88, "aud", "Reports and audit log", ["Daily cash reconciliation", "Unapplied and receipts reports"]),
        ("O1", 4, 4, .06, .12, .88, "id", "Marketing, Accounting, Disbursement", ["Work the own unit's unapplied", "Refund paid by Disbursement"]),
    ],
    edges=[
        ["P1", "C1", "n", "", ""], ["P1", "S0", "i", "File", "down"],
        ["C1", "A1", "n", "Submit", "vd"], ["A1", "C1", "t", "Return", "vu"],
        ["A1", "S1", "n", "Post", "hd"], ["S0", "S1", "n", "", ""],
        ["S1", "S2", "n", "", ""], ["C2", "S2", "n", "", "down"],
        ["S2", "O1", "i", "", "down"], ["S1", "R1", "a", "", "bot800"],
    ],
    footer_html=LEG,
)
