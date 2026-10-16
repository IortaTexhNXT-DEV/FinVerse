"""Process, storyboard, screen-shot and output content of the Product Maintenance document set v1.4 (BRD-03): the
journey, to-be processes and storyboards (BPS), the screenshots of each screen (UXD) and the reports, documents,
exports, notices and jobs (RNO). Read by build_brd03_v14.py, brd03_pack.py and build_brd03_v14_workbooks.py.
The screenshots are in figures/brd03_v14_screens (flows/brd03/shots_v14.sh).
"""

from __future__ import annotations

# ================================================================================================ BPS
JOURNEY = [
    ("1", "Marketing", "The Account Officer raises a package request with the requested terms and target insurers."),
    ("2", "Marketing", "A Marketing approver approves it for TSU."),
    ("3", "TSU", "The TSU Team Lead reviews and recommends; the TSU Head approves it for negotiation."),
    ("4", "TSU", "A TSU officer prepares the quotation slip; the Team Lead approves it; BIBS e-mails it to each "
     "insurer as a protected PDF."),
    ("5", "Insurers", "Insurers reply by e-mail; TSU keys in each response (outcome, rate, minimum premium, terms)."),
    ("6", "TSU", "TSU compiles the comparative table, opens further rounds if needed, and makes the terms final."),
    ("7", "TSU, Marketing", "The TSU Head reviews the terms; for a client-specific package, Marketing reviews them."),
    ("8", "TSU", "TSU completes the product requirements with the signed package slip."),
    ("9", "ManCom", "The selected ManCom approvers approve, the President last."),
    ("10", "MBS", "The request goes to MBS, which sets up the package version and submits it."),
    ("11", "TSU Head", "The TSU Head validates the version against the checklist and releases it."),
    ("12", "BIBS", "Marketing is told; the advisory is drafted; the product master change is queued for transfer."),
    ("13", "TSU", "TSU sends the advisory with the supporting documents."),
    ("14", "Marketing", "New business is priced on the released version; a different rate needs a rate exception."),
    ("15", "TSU, MBS", "Expiry alerts, renewal requests or deactivation close the cycle of the package."),
]

PROCESSES = [
    {"id": "P-PM-01", "name": "Package request and approval", "steps": "1 to 3",
     "trigger": "A Marketing, TSU or insurer need for a new or changed package.",
     "actors": "Marketing Account Officer, Marketing Approver, TSU Team Lead, TSU Head",
     "outcome": "Request approved for negotiation, or returned, voided or not proceeded.",
     "controls": "BR-PM-01; service levels PKG_SLA_MKT_APPROVAL, PKG_SLA_TSU_REVIEW, PKG_SLA_TSU_APPROVAL."},
    {"id": "P-PM-02", "name": "Negotiation, comparative table and terms", "steps": "4 to 7",
     "trigger": "TSU Head approval.", "actors": "TSU Officer, TSU Team Lead, TSU Head, Marketing, insurers",
     "outcome": "Final terms with the insurers of the package, roles and shares.",
     "controls": "BR-PM-01, BR-PM-04, BR-PM-11; PKG_SLA_NEGOTIATION."},
    {"id": "P-PM-03", "name": "Requirements and ManCom sign-off", "steps": "8 to 9",
     "trigger": "Terms reviewed.", "actors": "TSU Officer, ManCom members, President",
     "outcome": "Requirements signed off (MC-<request number>); slips locked.",
     "controls": "BR-PM-01, BR-PM-07, BR-PM-08; PKG_SLA_MANCOM."},
    {"id": "P-PM-04", "name": "Set-up, validation and release", "steps": "10 to 13",
     "trigger": "ManCom approval (automatic or Request for Deployment).",
     "actors": "MBS, TSU Head or Business Administrator, TSU Officer",
     "outcome": "Released version sold from its effective date; advisory sent; product master change transferred.",
     "controls": "BR-PM-01, BR-PM-02, BR-PM-09, BR-PM-14; PKG_SLA_MBS_SETUP."},
    {"id": "P-PM-05", "name": "New business on the rate scheme and rate exceptions", "steps": "14",
     "trigger": "A quotation on a package.", "actors": "Marketing Account Officer, approver",
     "outcome": "Quotation priced on the version in force, or at an approved exception rate.",
     "controls": "BR-PM-03; RATE_EXCEPTION_VALIDITY_DAYS."},
    {"id": "P-PM-06", "name": "Expiry, renewal and deactivation", "steps": "15",
     "trigger": "Daily expiry check, or a deactivation request.", "actors": "TSU Officer, MBS, approver",
     "outcome": "Renewal request raised, or package retired or expired and transferred to other systems.",
     "controls": "BR-PM-05, BR-PM-06, BR-PM-12."},
    {"id": "P-PM-07", "name": "Master maintenance under maker-checker", "steps": "-",
     "trigger": "A new or changed coverage, clause, field rule, insurer, rate or incentive criterion.",
     "actors": "MBS (maker), Business Administrator (checker)", "outcome": "Authorised master record.",
     "controls": "BR-PM-01, BR-PM-05, BR-PM-13."},
]

STORYBOARDS = [
    {"id": "SB-01", "title": "A new package from the request to its release",
     "summary": "Motor Fleet Plus, a new generic programme, goes from Marketing to release in one pass.",
     "steps": [
         ("MBS", "Coverages & Clauses", "Adds the clause 'Accredited repair shops for fleets' and saves it for "
          "authorisation.", "The clause is pending authorisation.", "sb1-01-clause"),
         ("Business Administrator", "Coverages & Clauses", "Authorises the clause.", "The clause is active.",
          "sb1-02-clause-authorised"),
         ("Marketing Account Officer", "New Package Request", "Enters the request, terms and two insurers; submits.",
          "The request is numbered and For Marketing approval.", "sb1-03-request"),
         ("Marketing Approver", "Package Request", "Approves and sends it to TSU.", "For TSU review.",
          "sb1-04-marketing"),
         ("TSU Team Lead", "Package Request", "Recommends it with a comment.", "For TSU Head approval.",
          "sb1-05-recommend"),
         ("TSU Head", "Package Request", "Approves it for negotiation.", "Insurer negotiation, round 1.",
          "sb1-06-tsu-head"),
         ("TSU Officer", "Negotiation tab", "Writes the notes to the insurers and submits the slip.",
          "Slip for approval.", "sb1-07-slip"),
         ("TSU Team Lead", "Negotiation tab", "Approves and sends the slip.", "Slip sent; one e-mail per insurer.",
          "sb1-08-slip-sent"),
         ("TSU Officer", "Negotiation tab", "Keys in the two insurer responses with the reply documents.",
          "Responses with rate and minimum premium.", "sb1-09-responses"),
         ("TSU Officer", "Negotiation tab", "Makes the terms final with the chosen insurer.",
          "Negotiated terms review.", "sb1-10-terms-final"),
         ("TSU Officer", "Comparative tab", "Opens the comparative table.", "Offers side by side, lowest first.",
          "sb1-11-comparative"),
         ("TSU Head", "Package Request", "Sends the generic programme to the requirements.",
          "Requirements preparation.", "sb1-12-requirements"),
         ("TSU Officer", "Requirements, Routing", "Completes the requirements, attaches the signed package slip and "
          "sends to the selected ManCom approvers.", "For ManCom sign-off.", "sb1-13-mancom-sent"),
         ("ManCom Member and President", "Routing & Approvals tab", "Approve, the President last.",
          "Signed off; with MBS for set-up.", "sb1-14-mancom-approved"),
         ("MBS", "Package Request", "Sets up version 1 with the new risk code.", "Draft version linked to the "
          "request.", "sb1-15-setup"),
         ("MBS", "Package Version", "Completes the insurer terms and submits for validation.", "For validation.",
          "sb1-16-version"),
         ("TSU Head", "Package Version", "Ticks the checklist and validates and releases.", "Released; Marketing told.",
          "sb1-17-released"),
         ("Business Administrator", "Insurer", "Adds the commission rate of the insurer for the new product.",
          "Rate pending authorisation.", "sb1-18-commission"),
         ("Approver", "Insurer", "Authorises the commission rate.", "Rate active.", "sb1-19-commission-authorised"),
         ("TSU Officer", "Advisories tab", "Sends the drafted new-package advisory.", "Advisory sent and stored.",
          "sb1-20-advisory"),
     ]},
    {"id": "SB-02", "title": "New business on the released package",
     "summary": "Marketing prices a quotation on Motor Fleet Plus and obtains a rate exception.",
     "steps": [
         ("Marketing Account Officer", "Premium Calculator", "Calculates the premium on the package.",
          "Premium on the version in force.", "sb2-01-calculator"),
         ("Marketing Account Officer", "Quotation", "Requests a rate exception for a lower item rate.",
          "Exception pending approval.", "sb2-02-exception"),
         ("Approver", "Rate Exception", "Approves the exception with a comment.", "Exception approved, valid 30 "
          "days.", "sb2-03-exception-approved"),
         ("Marketing Account Officer", "Quotation", "Opens the quotation.", "The approved exception is shown.",
          "sb2-04-quotation"),
     ]},
    {"id": "SB-03", "title": "Expiry, deactivation and transfer to other systems",
     "summary": "TSU follows the packages that end; MBS deactivates a package; the changes go to other systems.",
     "steps": [
         ("TSU Officer", "Package Expiry", "Reviews the packages ending within 90 days.", "Days left and renewal "
          "requests.", "ux-10-expiry"),
         ("MBS", "Product", "Opens Deactivate Package and enters date, reason, remarks and approver.",
          "The request is ready to submit.", "ux-24-deactivate"),
         ("MBS", "Deactivation Requests", "Follows the requests and decisions.", "Status of each request.",
          "ux-24-deactivations"),
         ("MBS", "Product Master Transfers", "Checks the changes sent to the receiving systems.", "Each change with "
          "its file and status.", "ux-25-transfers"),
     ]},
    {"id": "SB-04", "title": "Returns and refusals",
     "summary": "The checks and returns a user meets, and the messages shown.",
     "steps": [
         ("Marketing Account Officer", "New Package Request", "Saves an empty draft.", "Field messages.",
          "sb4-01-draft"),
         ("Marketing Account Officer", "New Package Request", "Submits a request without terms.",
          "Completeness message.", "sb4-02-incomplete"),
         ("Marketing Approver", "Package Request", "Approves a request of another user.", "For TSU review.",
          "sb4-03-marketing"),
         ("TSU Team Lead", "Package Request", "Returns a request with a reason.", "Back to the requester.",
          "sb4-04-tsu-return"),
         ("TSU Team Lead", "Negotiation tab", "Clicks Approve and Send on the slip he prepared.", "The "
          "confirmation; on confirming, the approval is refused: another officer approves (BR-PM-01).",
          "sb4-05-own-slip"),
         ("TSU Officer", "Negotiation tab", "Makes the terms final while an insurer has no outcome.", "Refused: every "
          "insurer of the round needs an outcome.", "sb4-06-terms-final"),
         ("ManCom Member", "Package Request", "Returns the requirements to TSU.", "Requirements preparation.",
          "sb4-07-mancom-return"),
         ("TSU Officer", "Requirements tab", "Resubmits for ManCom sign-off.", "For ManCom sign-off.",
          "sb4-08-resubmit"),
         ("MBS", "Package Request", "Returns incomplete requirements to TSU.", "Requirements preparation.",
          "sb4-09-mbs-return"),
         ("MBS", "Package Version", "Submits a version with a past date and no minimum premium.", "Field messages.",
          "sb4-10-version-errors"),
         ("TSU Head", "Package Version", "Returns the version to MBS with a reason.", "Draft again.",
          "sb4-11-validation-return"),
         ("MBS", "Package Version", "Opens the returned version.", "The return reason is shown.", "sb4-12-mbs-again"),
     ]},
]

# ================================================================================================ UXD
# Screenshots of each screen state: screen id -> [(shot, caption)]
SHOTS = {
    "SCR-PM-01": [("ux-01-home", "Product Maintenance Home (TSU Team Lead)")],
    "SCR-PM-02": [("ux-02-requests", "Package Requests work list")],
    "SCR-PM-03": [("ux-03-new", "New Package Request, filled"), ("ux-03-errors", "New Package Request, field "
                  "messages")],
    "SCR-PM-04": [("ux-04-record", "Request for Marketing approval (Marketing approver)"),
                  ("ux-04-tsu-approval", "Request for TSU Head approval")],
    "SCR-PM-05": [("ux-05-negotiation", "Negotiation tab")],
    "SCR-PM-06": [("ux-06-comparative", "Comparative tab with the current master")],
    "SCR-PM-07": [("ux-07-requirements", "Requirements & Sign-off tab")],
    "SCR-PM-08": [("ux-08-setup", "Request with MBS (set-up)"), ("ux-08-advisories", "Advisories tab of a released "
                  "request")],
    "SCR-PM-09": [("ux-09-tsu", "TSU Workbench")],
    "SCR-PM-10": [("ux-10-expiry", "Package Expiry")],
    "SCR-PM-11": [("ux-11-products", "Products (Product Matrix)")],
    "SCR-PM-12": [("ux-12-product", "Product record")],
    "SCR-PM-13": [("ux-13-version", "Draft version (MBS)"), ("ux-13-version-validate", "Version for validation "
                  "(TSU Head)")],
    "SCR-PM-14": [("ux-14-validation", "Validation Queue")],
    "SCR-PM-15": [("ux-15-coverages", "Coverages"), ("ux-15-clauses", "Clause Library")],
    "SCR-PM-16": [("ux-16-incentives", "Incentive Criteria")],
    "SCR-PM-17": [("ux-17-insurers", "Insurers")],
    "SCR-PM-18": [("ux-18-insurer", "Insurer record")],
    "SCR-PM-19": [("ux-19-rates", "Rates & Taxes")],
    "SCR-PM-20": [("ux-20-sales", "Sales Organisation")],
    "SCR-PM-21": [("ux-21-calculator", "Premium Calculator with a result")],
    "SCR-PM-22": [("ux-22-exception", "Rate exception for approval")],
    "SCR-PM-23": [("ux-23-dashboard", "Product Maintenance Dashboard")],
    "SCR-PM-24": [("ux-24-deactivations", "Deactivation Requests"), ("ux-24-deactivate", "Deactivate Package "
                  "dialog")],
    "SCR-PM-25": [("ux-25-transfers", "Product Master Transfers")],
    "SCR-PM-26": [("ux-26-placement", "Placement Update Reports")],
    "SCR-PM-27": [("ux-27-audit", "Audit Logs (Auditor)")],
    "SCR-PM-28": [("ux-28-proposal", "Proposal request, Comparative Table tab")],
    "SCR-PM-29": [("ux-29-routing", "Routing & Approvals tab (ManCom member)")],
}

# ================================================================================================ RNO
REPORTS = [
    # id, report, BRD, parameters, columns, output
    ("RPT-PM-01", "Dashboard (KPIs and drill-down)", "BRPM.019", "Period, TSU Officer, Product Line, Package Type",
     "Request No., type, line, requested by, TSU officer, status, submitted, aging", "Screen; Excel, PDF, CSV"),
    ("RPT-PM-02", "Consolidated Placement Update Report", "BRPM.018", "Report date; period (7 days); scope",
     "Item No., insured, segment, team, Account Officer, TSU handler, line, sub-line, PRF received date and time, "
     "aging, status", "Excel, PlacementUpdate_MMDDYYYY.xlsx"),
    ("RPT-PM-03", "Package Status Update Report", "BRPM.018", "Request No.; stage; type; line; insurer; ends within",
     "Request No., type, client or programme, package, product, version, stage, days in stage, package end, rate, "
     "insurers, last action", "PDF, Excel, CSV"),
    ("RPT-PM-04", "Renewal Request Report", "BRPM.017", "Period", "Package, version, expiry date, days left, renewal "
     "request, stage", "PDF, Excel, CSV"),
    ("RPT-PM-05", "Package Expiry Report", "BRPM.017", "Ending within (days), default 90", "Product, package, version, "
     "package end, days left, anniversary, renewal request, stage", "PDF, Excel, CSV"),
    ("RPT-PM-06", "Package Version History", "BRPM.006", "Product (optional)", "Product, version, status, dates, "
     "package end, submitted by, validated by and on, result, source request, change summary", "PDF, Excel, CSV"),
    ("RPT-PM-07", "Product Matrix", "BRPM.003", "Line, status", "Line, sub-line, package, description, insurer, "
     "status, effective and expiry dates, last update", "PDF, Excel, CSV"),
    ("RPT-PM-08", "Product Maintenance audit report", "BRPM.024", "Dates, action, reference, user", "Time, module, "
     "reference, client, action, description, old and new value, user, remarks", "PDF, Excel, CSV"),
    ("RPT-PM-09", "Reports builder", "BRPM.005", "Fields of the data source; dates", "As chosen", "PDF, Excel, CSV"),
]

DOCUMENTS = [
    # id, document, format and name, protection, FRPM
    ("DO-01", "Package Request Form", "PDF", "Kept in BIBS", "FRPM.011.02, FRPM.030.01"),
    ("DO-02", "Package Quotation Slip", "PDF per insurer and round; QS_<reference>_<insurer>_MMDDYYYY.pdf",
     "Password protected when e-mailed", "FRPM.012.01"),
    ("DO-03", "Comparative Table (master and client view)", "PDF and Excel", "Password protected when e-mailed",
     "FRPM.012.04"),
    ("DO-04", "Package slip", "PDF (unsigned, for signature)", "Kept in BIBS", "FRPM.014.01"),
    ("DO-05", "ManCom sign-off record", "PDF, MC-<request number>", "Kept in BIBS", "FRPM.014.02"),
    ("DO-06", "Package advisory", "PDF, stored on the request", "Password protected when e-mailed", "FRPM.025.01"),
    ("DO-07", "Non-package Quotation Slip and Proposal Slip", "PDF; QS_ and ProposalSlip_ names",
     "Password protected", "FRPM.006.01, FRPM.009.02"),
    ("DO-08", "Product master file", "CSV, ProductMaster_yyyyMMddHHmmss.csv", "Folder of the receiving systems",
     "FRPM.029.02"),
]

EXPORTS = [
    ("Product Matrix list", "Excel, PDF, CSV", "Products", "The filtered rows"),
    ("Package Requests list", "Excel", "Package Requests", "The rows of the tab and filters"),
    ("Dashboard drill-down", "Excel, PDF, CSV", "Dashboard", "The rows of the KPI"),
    ("Expiry list", "Excel", "Package Expiry", "Packages ending within the days chosen"),
    ("Audit logs", "CSV, Excel; Audit Logs_MMDDYYYY", "Audit Logs", "The filtered entries"),
    ("Sales organisation", "Excel", "Sales Organisation", "Units and officers"),
]

NOTICES = [
    # id, event, to, title, text, channel
    ("NT-01", "Request enters TSU review", "TSU Team Leads", "<request no.> for TSU review", "<title>", "Bell"),
    ("NT-02", "Request waits for a Marketing approval level", "Marketing approvers of the level (not the maker)",
     "<request no.> for <level> approval", "<title>", "Bell; e-mail"),
    ("NT-03", "Request assigned", "The new assignee", "<request no.> assigned to you", "<title>", "Bell"),
    ("NT-04", "Package quotation slip sent", "Placement e-mails of each insurer", "Package quotation slip <PQS no.> - "
     "<package>", "Please find attached our package quotation slip ... Kindly send your best terms on or before "
     "<reply date>. The password follows separately.", "E-mail"),
    ("NT-05", "Protected document sent", "Same recipients", "Password for: <subject>", "The password of the "
     "attached documents.", "E-mail"),
    ("NT-06", "ManCom approval needed", "Each selected approver; the President when his turn comes",
     "<request no.> for your ManCom approval", "<title>", "Bell; e-mail"),
    ("NT-07", "Deployment request", "MBS", "Package deployment request <PDR no.>", "<title>", "Bell; e-mail"),
    ("NT-08", "Version released", "Marketing users who quote", "Package available: <risk code>", "<package> "
     "(version <n>) sells from <date>.", "Bell"),
    ("NT-09", "Advisory sent", "Users of each recipient group; e-mail addresses given", "<advisory subject>",
     "<advisory text>", "Bell; e-mail (protected PDF)"),
    ("NT-10", "Package ending (90, 30, 7 days)", "TSU Officers and Team Leads (MBS when set); Alerts",
     "Package ending: <risk code>", "Version <n> ends on <date> (<n> days).", "Bell; alert"),
    ("NT-11", "Package anniversary", "As NT-10", "Package anniversary: <risk code>", "<text>", "Bell; alert"),
    ("NT-12", "Package expired", "TSU and MBS users", "Package expired: <risk code>", "Version <n> ended on <date> "
     "without a renewal; the package is no longer sold for new business.", "Bell"),
    ("NT-13", "Incentive criterion on an ended product", "TSU and MBS users; Alerts", "Incentive criterion to "
     "review: <code>", "<text>", "Bell; alert"),
    ("NT-14", "Product Matrix record for approval", "The approver named", "Product Matrix record for approval: "
     "<code>", "<user> sent <product> for your approval.", "Bell; e-mail"),
    ("NT-15", "Deactivation request and decision", "Approver; then the requester", "Package deactivation "
     "<status>: <PKD no.>", "<package> will be deactivated after <date> / stays active.", "Bell; e-mail"),
    ("NT-16", "Rate exception decided", "The requester", "Rate exception <reference> approved / rejected",
     "<details> by <approver>.", "Bell"),
    ("NT-17", "Placement Update Report generated", "Users with the report permission", "<file name> generated",
     "<n> request(s) of <period>", "Bell"),
    ("NT-18", "Non-package proposal decided or terms agreed", "TSU officer; requestor", "<reference>: Proposal "
     "Ready / Terms Agreed", "<text>", "Bell"),
    ("NT-19", "Client response recorded", "TSU officer; requestor", "<reference>: client response <response>",
     "<client> - <remarks>", "Bell"),
    ("NT-20", "Work item overdue; product master transfer failed", "Alerts screen (System Administrator, Auditor)",
     "Work item overdue / PM_MASTER_TRANSFER_FAILED", "<reference>", "Alert"),
]

JOBS = [
    ("PACKAGE_EXPIRY_MONITOR", "Daily 01:00", "Expiry and anniversary alerts; renewal drafts when set.",
     "NT-10, NT-11"),
    ("PACKAGE_VERSION_LIFECYCLE", "Daily 01:00", "Supersedes versions whose selling period ended; expires packages "
     "past their end date; flags incentive criteria.", "NT-12, NT-13"),
    ("PACKAGE_DEACTIVATION", "Daily 01:05", "Retires packages whose approved deactivation is effective.", "NT-15"),
    ("PLACEMENT_UPDATE_REPORT", "Hourly; runs on the set weekday and time", "Generates the Consolidated Placement "
     "Update Report.", "NT-17"),
    ("PRODUCT_MASTER_TRANSFER", "Every 30 minutes; Send Now", "Writes the pending product master changes to the "
     "file for the receiving systems.", "NT-20"),
]

# ================================================================================================ ICD
INTERFACES = [
    ("IF-PM-01", "Product master transfer", "BIBS to the receiving BDOI systems (to be named by BDOI IT)",
     "CSV file in a folder", "Every 30 minutes and Send Now", "Released, retired and expired packages"),
    ("IF-PM-02", "E-mail channel", "BIBS to insurers and recipients of advisories", "SMTP through BDOI's mail "
     "relay", "On each send; queue every 2 minutes", "Quotation slips, advisories, password e-mails"),
]
FILE_RECORDS = [
    # record, field no., field, format, example
    ("H (header)", "1", "Record type", "H", "H"),
    ("H", "2", "File name", "ProductMaster_yyyyMMddHHmmss.csv", "ProductMaster_20261016093000.csv"),
    ("H", "3", "Business date", "yyyy-MM-dd (Manila)", "2026-10-16"),
    ("H", "4", "Source system", "BIBS", "BIBS"),
    ("D (detail)", "1", "Record type", "D", "D"),
    ("D", "2", "Product code (risk code)", "Text, up to 30", "MTR30"),
    ("D", "3", "Product name", "Text; quoted when it holds a comma or quote", "Motor Fleet Plus"),
    ("D", "4", "Line of insurance", "Code", "MOTOR"),
    ("D", "5", "Version", "Whole number; empty for a retirement", "1"),
    ("D", "6", "Change", "RELEASED, RETIRED or EXPIRED", "RELEASED"),
    ("D", "7", "Effective date", "yyyy-MM-dd", "2026-10-16"),
    ("D", "8", "Source package request", "PKR-yyyy-n; empty when none", "PKR-2026-000107"),
    ("T (trailer)", "1", "Record type", "T", "T"),
    ("T", "2", "Number of detail records", "Whole number", "1"),
]
