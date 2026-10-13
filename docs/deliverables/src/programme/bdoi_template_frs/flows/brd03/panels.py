"""Spec of the one-page "Product Maintenance - Processes at a Glance" picture (FRS BRD-03 v1.3, BDOI template).
Rendered by render_panels.sh with panels_gen.py. Content strictly from the FRS v1.3 (sections 3 and 4, Annexes J, O,
S, U and Y). Untagged text = BRD (black); {CR:..} orange (Annex Y.2); {OOTB} green (Annex Y.3); {INT:..} grey
(Annex Y.5)."""

INT = "{INT:Integration scope}"
DM = "{INT:Data Migration scope}"


def B(title, lines=(), kind=""):
    return ("box", title, list(lines), kind)


def F(*steps):
    return ("flow", list(steps))


SPEC = {
    "size": (1680, 1010),
    "title": "Product Maintenance – Processes at a Glance",
    "tag": "Based on FRS v1.3|BRD-03, in BDOI's FRS template",
    "rows": "minmax(0,0.97fr) minmax(0,1.18fr) minmax(0,0.85fr)",
    "legend": ["BRD requirement (black text)", "{OOTB} OOTB, beyond the BRD",
               "{CR:CR-PM-nnn} change request", "{INT:grey} integration / data-migration scope",
               "Every approver differs from the maker; every step is in the audit trail."],
    "panels": [
        {"no": "1", "title": "Request intake", "sub": "Package requests and non-package quotation requests",
         "grid": (1, 1, 4, 1), "colour": "#0277BD", "fill": "#F3FAFE",
         "body": [F(B("Package request (PKR-yyyy-nnnnnn)", [
                        "New, amend, renew or deactivate a package",
                        "Generic programme or client-specific package",
                        "Source: Marketing, TSU or insurer"], "id"),
                    B("", ["Requested terms, coverages, rate scheme, package dates and target insurers",
                           "Mandatory fields and documents checked on submission",
                           "Minimums: 20 policies, PHP 5,000,000.00 basic premium"]),
                    B("", ["Non-package quotation requests, slips and proposals with New Business (specified in the New Business FRS)"], "ret"))]},
        {"no": "2", "title": "Approval: Marketing and TSU", "sub": "Service levels per stage (Annex J.4)",
         "grid": (5, 1, 4, 1), "colour": "#5E35B1", "fill": "#F7F4FD",
         "body": [F(B("Marketing approval", ["One approval by the TL, TH or UH, or the three in order (setting)",
                                             "The requester is never the approver; 24 hours per level"], "apr"),
                    B("TSU review", ["TSU Team Lead recommends and assigns the TSU Officer (24 hours)"], "apr"),
                    B("TSU Head approval", ["Approves for negotiation, without negotiation, returns or rejects"], "apr"),
                    ("cols", [B("", ["Return with remarks → requester"], "ret"),
                              B("", ["Reject with remarks"], "rej")]))]},
        {"no": "3", "title": "Negotiation with insurers", "sub": "Rounds until the terms are final",
         "grid": (9, 1, 4, 1), "colour": "#00695C", "fill": "#F1FAF8",
         "body": [F(B("Quotation Slip (PQS-yyyy-nnnnnn)", ["Prepared by the TSU Officer, approved by the TSU Team Lead (not the preparer)"], "apr"),
                    B("", ["Sent to the insurers as a protected document; the password in a separate e-mail",
                           "Insurer reply days: 5 (configurable)"], "sys"),
                    B("", ["Replies keyed in; every earlier value kept; further rounds",
                           "Comparative Table: master and client view stored"]),
                    B("", ["Terms final: insurers with terms only, shares total 100%"], "sys"))]},
        {"no": "4", "title": "Requirements and ManCom sign-off", "sub": "",
         "grid": (1, 2, 3, 1), "colour": "#C62828", "fill": "#FFF6F6",
         "body": [F(B("", ["TSU Head reviews the negotiated terms; Marketing reviews a client-specific package"], "apr"),
                    B("", ["Product requirements, computation basis and the signed package slip (48 hours)"]),
                    B("ManCom approval", ["Selected ManCom approvers in parallel, then the President (setting)",
                                          "The submitter cannot approve; 72 hours",
                                          "Quotation Slips locked on approval"], "apr"),
                    B("", ["Request for deployment to MBS: automatic or by the button (setting)"], "sys"))]},
        {"no": "5", "title": "Set-up, validation and release", "sub": "Marketing Business Services (MBS)",
         "grid": (4, 2, 3, 1), "colour": "#1565C0", "fill": "#F5F9FF",
         "body": [F(B("", ["MBS sets up the package version: risk code, rate scheme, coverages, panel insurers, clauses (48 hours)",
                           "Terms equal the ManCom-approved terms"], "sys"),
                    B("", ["Return incomplete requirements to TSU with reason"], "ret"),
                    B("Post-setup validation", ["TSU Head: checklist and test premium; not the MBS user who set it up (24 hours)"], "apr"),
                    B("", ["Release: the previous version ends the day before the new one takes effect"], "sys"))]},
        {"no": "6", "title": "Publishing to New Business", "sub": "",
         "grid": (7, 2, 3, 1), "colour": "#2E7D32", "fill": "#F3FAF3",
         "body": [F(B("", ["Advisory checked and sent by TSU with the signed package slip and ManCom sign-off attached"], "sys"),
                    B("", ["New Business offers the package on its latest approved rate scheme; quotations, accounts and invoices keep the version"]),
                    B("", ["Premium Calculator on the package rate scheme {OOTB}"]),
                    B("Rate exception", ["Requested by the Marketing AO with its reason",
                                         "Decided by the New Business approver or the Business Administrator (not the requester); valid 30 days"], "apr"))]},
        {"no": "7", "title": "Insurer, rating and product masters", "sub": "Maker-checker on every master",
         "grid": (10, 2, 3, 1), "colour": "#00838F", "fill": "#F0FAFB",
         "body": [("list", ["Product lines, cover types, products and the Product Matrix",
                            "Coverages, perils, field rules and the clause library",
                            "Insurers, branches, placement e-mails and commission rates",
                            "Rate tables and statutory charges",
                            "Incentive rules: active products only, no overlap in time",
                            "Sales organisation",
                            "Each change made by one user and authorised by another (maker-checker)"])]},
        {"no": "8", "title": "Expiry, renewal and deactivation", "sub": "",
         "grid": (1, 3, 4, 1), "colour": "#EF6C00", "fill": "#FFF8F0",
         "body": [("cols", [
             F(B("", ["Daily check: alerts from 90 days before expiry, reminders at the configured days"], "sys"),
               B("", ["Renewal request: one open per package, through the same approvals"])),
             F(B("", ["Deactivation request (PKD-yyyy-nnnnnn) with its effective date"]),
               B("", ["Selected approver; not the requester; reject with remarks"], "apr"),
               B("", ["Automatic expiry; advisory drafted"], "sys"))])]},
        {"no": "9", "title": "Reporting and audit", "sub": "",
         "grid": (5, 3, 4, 1), "colour": "#7B1FA2", "fill": "#FBF5FD",
         "body": [("list", ["Dashboard: request status and key figures in real time",
                            "Placement update reports on schedule; exports to PDF, Excel and CSV, every run logged",
                            "Audit trail of every action, with old and new values of changed masters",
                            "E-mail log: recipient, time, subject and attachment",
                            "Screens to WCAG 2.2 level AA {CR:CR-PM-101}"])]},
        {"no": "10", "title": "Integration and data migration", "sub": "Annexes S and U",
         "grid": (9, 3, 4, 1), "colour": "#616161", "fill": "#F7F7F7",
         "body": [("list", ["Product master changes to the other BDOI systems " + INT,
                            "E-mails through BDOI's channel; documents in the ECM store " + INT,
                            "Sign-in and roles from User Access Maintenance (Entra ID) " + INT,
                            "Go-live masters, insurers, commission rates and the package map of legacy packages " + DM])]},
    ],
}
