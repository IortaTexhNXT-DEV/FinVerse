"""Renewal - simplified process flow (FRS BRD-06 v1.3). Drawn from the FRS (FRRN.001 to FRRN.049) and the BRD
(BRRN.001 to BRRN.042)."""

LEG = ('<div style="display:flex;gap:26px;font-size:12px;align-items:center;color:#374151">'
       '<span><b style="color:#0D47A1">&#8594;</b> Process flow</span>'
       '<span><b style="color:#0277BD">- - &#8594;</b> Exchange with insurers / other systems</span>'
       '<span><b style="color:#5E35B1">- - &#8594;</b> Returned for correction</span>'
       '<span><b style="color:#EF6C00">&#8594;</b> Letter / notice</span>'
       '<span style="margin-left:auto">Source: FRS Renewal v1.3, FRRN.001&#8211;049</span></div>')

SPEC = dict(
    size=(1600, 1010), foot=26,
    title="BDO BIBS &#8211; Renewal &#8211; Simplified Process Flow",
    tag="Based on FRS v1.3<br>Renewal (BRD-06)",
    stages=["1. Extraction &amp; Checks", "2. Disposition", "3. Review &amp; Posting", "4. Insurer",
            "5. Advice &amp; Acceptance", "6. Placement &amp; Booking"],
    lanes=[("Marketing", "Team Lead,<br>Account Officer", 1.0),
           ("Contact Center", "Follow-up", 0.6),
           ("Processing", "Team Lead,<br>Officer", 1.0),
           ("Insurers / Client", "CCM, MFT, e-mail", 0.75),
           ("System (BIBS)", "", 1.05)],
    boxes=[
        ("M0", 0, 0, .06, .1, .88, "req", "Generate and initiate", ["Expiry list by range", "Explicit initiation"]),
        ("M1", 0, 1, .06, .1, .88, "req", "Assign and dispose", ["Account history first", "Disposition, submit"]),
        ("M2", 0, 2, .06, .1, .88, "apr", "TL review and post", ["Return, override, post", "Transfers decided"]),
        ("M3", 0, 4, .06, .1, .88, "req", "Renewal Advice", ["Generate (locks), send", "Record acceptance"]),
        ("C1", 1, 4, .06, .12, .88, "ntf", "Follow-up", ["Call, e-mail, SMS", "No premium shown"]),
        ("P1", 2, 3, .06, .08, .88, "req", "Insurer request", ["Batch per insurer", "Record the answer"]),
        ("P2", 2, 5, .06, .08, .88, "req", "Place and book", ["Slips, files, responses", "E-policy to client"]),
        ("I1", 3, 3, .06, .12, .88, "id", "Insurer answer", ["Renew As Is, Revise, Reject"]),
        ("I2", 3, 4, .06, .12, .88, "id", "Client", ["Protected RA via CCM", "Accepts or not"]),
        ("S0", 4, 0, .06, .08, .88, "sys", "Daily extraction", ["Lead days; one account per policy", "Checks and "
                                                                "classification"]),
        ("S1", 4, 1, .06, .08, .88, "dec", "Decision matrix", ["Clean: automatic disposition", "Others: proposal"]),
        ("S2", 4, 4, .06, .08, .88, "ntf", "Letters and NRNS", ["NAL, NRL on the effective expiry", "Reminders"]),
        ("S3", 4, 5, .06, .08, .88, "aud", "Hold cover, billing, audit", ["CBG hold cover, CLPC billing", "Reports, "
                                                                          "audit trail"]),
    ],
    edges=[
        ["S0", "M0", "n", "", "up"], ["M0", "S1", "n", "", "down"], ["S1", "M1", "n", "", "up"],
        ["M1", "M2", "n", "", ""], ["M2", "M1", "t", "Return", "bot300", [660, 300]],
        ["M2", "P1", "n", "Post", "down"], ["P1", "I1", "i", "Request", "down"],
        ["I1", "M3", "i", "RA Ready", "up"], ["M3", "I2", "i", "Send", "down"],
        ["C1", "I2", "o", "", "down"], ["I2", "P2", "i", "Accept", "up"],
        ["M3", "S2", "o", "", "down"], ["P2", "S3", "a", "", "down"],
    ],
    footer_html=LEG,
)
