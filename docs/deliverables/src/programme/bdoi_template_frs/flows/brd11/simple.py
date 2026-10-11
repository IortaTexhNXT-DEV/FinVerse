"""Simplified User Access Maintenance flow (FRS v1.4, section 1.8)."""

LEG = ('<div style="display:flex;gap:26px;font-size:12px;align-items:center;color:#374151">'
       '<span><b style="color:#0D47A1">&#8594;</b> Process flow</span>'
       '<span><b style="color:#0277BD">- - &#8594;</b> Exchange with BDOI identity systems</span>'
       '<span><b style="color:#C62828">&#8594;</b> Rejected</span>'
       '<span><b style="color:#5E35B1">- - &#8594;</b> Returned for correction</span>'
       '<span style="margin-left:auto">Source: FRS User Access Maintenance v1.4, FRUM.001&#8211;013, Annexes N, O</span></div>')

SPEC = dict(
    size=(1600, 1000), foot=26,
    title="BDO BIBS &#8211; User Access Maintenance &#8211; Simplified Process Flow",
    tag="Based on FRS v1.4<br>Provisioning option (a), as delivered",
    stages=["1. Identity &amp; Sign-in", "2. Request", "3. Review &amp; Approval", "4. Implementation", "5. Notification", "6. Reporting &amp; Audit"],
    lanes=[("BDOI Identity", "EIAM (Microsoft Entra ID)<br>UIDM-ISC", 1.0),
           ("Requestor", "Business user / HR / Admin", 1.0),
           ("Approver(s)", "Approver, Second Approver,<br>Information Security", 1.25),
           ("System (BIBS)", "", 1.1),
           ("Reports &amp; Audit", "Auditor, Information Security", 0.8)],
    boxes=[
        ("U1", 0, 0, .06, .14, .88, "id", "Joiner / mover / leaver", ["Raised and approved in UIDM-ISC", "Sent to BIBS with the UIDM request no."]),
        ("E1", 0, 1, .06, .14, .88, "id", "Sign-in through EIAM", ["Microsoft Entra ID single sign-on", "Password, second factor, BDO device", "Menus of the user's group profile"]),
        ("C1", 0, 5, .06, .14, .88, "id", "Access certification", ["In UIDM-ISC, from the extract of BIBS accounts, group profiles and permissions"]),
        ("R1", 1, 1, .06, .1, .88, "req", "Raise access request", ["Assign / change a user's group profile", "New / modify / (re)activate group profile", "Bulk request; choose approver; submit"]),
        ("R2", 1, 2, .1, .2, .8, "ret", "Correct and resubmit", ["Returned request edited by the requestor"]),
        ("R3", 1, 4, .08, .18, .84, "ntf", "Receive the outcome", ["Implemented, returned or rejected"]),
        ("A1", 2, 2, .08, .06, .84, "apr", "Review request", ["Assigned to Me", "Permissions added / removed", "Separation-of-duties check"]),
        ("D1", 2, 2, .22, .66, .56, "dec", "Decision?", []),
        ("A2", 2, 3, .03, .06, .55, "apr", "Second approval", ["High / Admin privilege", "Requestor cannot approve own request"]),
        ("RJ", 2, 3, .6, .62, .38, "rej", "Rejected", ["With the reason"]),
        ("B1", 3, 0, .06, .08, .88, "sys", "Identity Synchronisation", ["Create / change / deactivate / reactivate user", "Leaver: deactivate, end sessions", "Refused event: queued and alerted"]),
        ("B2", 3, 3, .03, .12, .55, "sys", "Apply the approved change", ["On the effective date", "Access-change log, before / after"]),
        ("B3", 3, 4, .08, .2, .84, "ntf", "Send notices", ["In BIBS, with e-mail copy", "Requestor and approvers"]),
        ("AU", 4, 5, .06, .1, .88, "aud", "Reports and audit trail", ["User Access Report and Matrix", "User Access Audit Log"]),
    ],
    edges=[
        ["U1", "B1", "i", "", "down"], ["E1", "R1", "i", "", "down"],
        ["R1", "A1", "n", "Submit", "", [588, 458]], ["A1", "D1", "n", "", "down"],
        ["D1", "A2", "n", "Approve", "", [858, 628]], ["D1", "RJ", "r", "Reject", ""],
        ["D1", "R2", "t", "Return", "lu", [612, 560]], ["R2", "A1", "t", "", "down"],
        ["A2", "B2", "n", "", "down"], ["RJ", "B3", "r", "", "hd"],
        ["B2", "B3", "n", "", ""], ["B3", "R3", "o", "", "up"],
        ["B2", "AU", "a", "", "vd"], ["AU", "C1", "i", "Extract", "up"],
    ],
    footer_html=LEG,
)
