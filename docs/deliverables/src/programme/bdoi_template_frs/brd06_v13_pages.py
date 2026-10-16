"""Process, storyboard and output content of the Renewal document set v1.3 (BRD-06): the journey, to-be processes and
storyboards (BPS), the reports, documents, exports, notices and jobs (RNO) and the interfaces (ICD). Read by
build_brd06_v13.py, brd06_pack.py and build_brd06_v13_workbooks.py.

The storyboards are the walkthroughs of the Renewal sign-off pack (docs/deliverables/src/BRD-06_Renewal/pack/
walkthroughs.yaml) and the notices its catalogue (notifications.yaml); the screenshots are in
figures/brd06_v13_screens (flows/brd06/shots_v13.sh).
"""

from __future__ import annotations

import re
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
PACK = HERE.parents[1] / "BRD-06_Renewal" / "pack"

# ================================================================================================ BPS
JOURNEY = [
    ("1", "BIBS", "The daily run extracts the policies at the lead days before expiry; a Team Lead may generate any "
     "range. One renewal account per expiring policy."),
    ("2", "Marketing TL", "Initiates the accounts; BIBS runs the checks, classifies them Clean, Review or Exception "
     "and the decision matrix proposes or gives the disposition."),
    ("3", "Marketing TL", "Assigns the accounts to Account Officers (or BIBS assigns by round robin); transfers an "
     "account to another unit when needed."),
    ("4", "Account Officer", "Reads the account history, gives the disposition and submits the account for posting."),
    ("5", "Marketing TL", "Reviews, returns or overrides, and posts. For Renewal goes to Processing; For Quotation or "
     "For Proposal to New Business; Not for Renewal to the closing letter."),
    ("6", "Processing", "Assigns the Processing Officer, sends the insurer request, records the insurer's answer."),
    ("7", "Insurers", "Answer Renew As Is, Revise or Reject by file, MFT or e-mail."),
    ("8", "Marketing", "Generates and sends the Renewal Advice (first, then last notice), protected through CCM."),
    ("9", "Contact Center", "Follows up the client who has not answered."),
    ("10", "Marketing", "Records the client's acceptance; BIBS requests the hold cover where needed."),
    ("11", "Processing", "Places the renewal with the insurer (MFT or CCM), records the placement response."),
    ("12", "Processing", "Books the renewal through New Business; the e-policy is received and sent to the client."),
    ("13", "BIBS", "Closing letters, NRNS reminders, billing files and the closure of unrenewed accounts by the "
     "scheduled jobs."),
]

PROCESSES = [
    {"id": "P-RN-01", "name": "Extraction, sanitation and classification", "steps": "1 to 2",
     "trigger": "Daily run at the lead days, or Generate Expiry List.", "actors": "BIBS, Marketing and Processing "
     "Team Leads", "outcome": "Initiated renewal accounts with checks, classification and proposed disposition.",
     "controls": "BR-RN-02, BR-RN-03, BR-RN-07; RNW_EXTRACTION_LEAD_DAYS."},
    {"id": "P-RN-02", "name": "Assignment, disposition and posting", "steps": "3 to 5",
     "trigger": "Initiated account.", "actors": "Marketing Team Lead, Account Officer",
     "outcome": "Posted renewal routed by its disposition.", "controls": "BR-RN-04, BR-RN-06, BR-RN-15, BR-RN-16."},
    {"id": "P-RN-03", "name": "Processing and the insurer's answer", "steps": "6 to 7",
     "trigger": "Renewal posted For Renewal.", "actors": "Processing Team Lead, Processing Officer, insurers",
     "outcome": "Insurer disposition recorded; RA Ready, or returned to Marketing.",
     "controls": "BR-RN-09, BR-RN-15; RNW_INSURER_REPLY_DAYS."},
    {"id": "P-RN-04", "name": "Renewal Advice, follow-up and acceptance", "steps": "8 to 10",
     "trigger": "RA Ready.", "actors": "Account Officer, Contact Center, client",
     "outcome": "Client acceptance recorded, or NRNS and closing letters.", "controls": "BR-RN-05, BR-RN-08, "
     "BR-RN-09; RNW_RA_FIRST_NOTICE_DAYS, RNW_NRNS_REMINDER_DAYS."},
    {"id": "P-RN-05", "name": "Placement, booking and e-policy", "steps": "11 to 12",
     "trigger": "Accepted renewal; paid CBG account.", "actors": "Processing Officer, insurers",
     "outcome": "Renewal booked as business type Renewal; e-policy sent.", "controls": "BR-RN-12, BR-RN-13."},
    {"id": "P-RN-06", "name": "Straight-through CBG renewals", "steps": "1, 10 to 13",
     "trigger": "Clean CBG account.", "actors": "BIBS, LAMD, Processing",
     "outcome": "Hold cover at 75 days, billing file to LMS, placement on payment.",
     "controls": "BR-RN-07, BR-RN-12; RNW_HOLD_COVER_CBG_DAYS_BEFORE, RNW_CLPC_BILLING_DAYS_BEFORE."},
    {"id": "P-RN-07", "name": "Set-up under maker-checker", "steps": "-",
     "trigger": "A change of risk codes, checks, rules, decision matrix, lists, templates or settings.",
     "actors": "Business Administrator (maker), second user (checker)", "outcome": "Approved set-up in force.",
     "controls": "BR-RN-01, BR-RN-14."},
]


def _clean(t: str) -> str:
    t = re.sub(r"\s*\((?:FR-RN-\d+(?:, )?)+\)", "", str(t))
    return " ".join(t.split())


_ROLE = {"MKT_TL": "Marketing Team Lead", "MKT_AO": "Account Officer", "PROCESSING_TL": "Processing Team Lead",
         "PROCESSOR": "Processing Officer", "CONTACT_CENTER": "Contact Center"}


# Storyboard steps whose page is exactly a screen state of the UX design (one image kept, see shots_v13.sh).
SAME_AS = {"sb3-01": "ux-15-01-list"}


def _storyboards() -> list[dict]:
    import brd06_v13_screens as S  # noqa: PLC0415
    names = {s["id"]: s["name"].split(":")[0] for s in S.SCREENS}
    out = []
    data = yaml.safe_load((PACK / "walkthroughs.yaml").read_text(encoding="utf-8"))["walkthroughs"]
    for k, w in enumerate(data, 1):
        steps = [(_ROLE[r], names[scr], _clean(does), _clean(sees),
                  SAME_AS.get(f"sb{k}-{n:02d}", f"sb{k}-{n:02d}"))
                 for n, (r, scr, does, sees, _res, _shot) in enumerate(w["steps"], 1)]
        out.append({"id": f"SB-{k:02d}", "title": w["title"], "summary": _clean(w["summary"]),
                    "data": _clean(w["data"]), "steps": steps})
    return out


STORYBOARDS = _storyboards()

# ================================================================================================ RNO
REPORTS = [
    # id, report, BRD, parameters, content, output
    ("RPT-RN-01", "Renewal Status Report", "BRRN.019; BRD 1.009", "Expiry range; the 20 criteria of the BRD",
     "Status of every renewal per invoice; summary counters", "Excel, PDF; saved variant"),
    ("RPT-RN-02", "Renewal Listing with attention flags", "BRRN.036", "Unit, segment, window, status",
     "Days to expiry; ageing, overdue, high-risk flags", "Excel, PDF, CSV"),
    ("RPT-RN-03", "Expiry List Report", "BRRN.002-008", "Expiry range, filters", "The RMEL columns", "Excel"),
    ("RPT-RN-04", "Sanitation Report", "BRRN.020", "Date range", "Check results and classification changes",
     "Excel, PDF"),
    ("RPT-RN-05", "Decision Log", "BRRN.031, 034", "Date range", "System dispositions with matrix version and rule; "
     "overrides with reason", "Excel, PDF"),
    ("RPT-RN-06", "LAMD Match Report", "BRRN.029", "Upload, month", "Matched and unmatched loans", "Excel"),
    ("RPT-RN-07", "Insurer Extract", "BRD 3.009", "Insurer, expiry range", "The 28 fields of the BRD", "Excel"),
    ("RPT-RN-08", "Renewal Advice Dispatch (letter log)", "BRRN.010", "Date range", "Letters with recipient and "
     "delivery status", "Excel, PDF"),
    ("RPT-RN-09", "Workload Report", "BRD 1.009.7", "Unit, officer", "Open renewals by officer and status", "Excel"),
    ("RPT-RN-10", "Go-live Take-over Report", "FRRN.045", "-", "Taken-over accounts by expiry month; urgent",
     "Excel"),
    ("RPT-RN-11", "Package Remap Report", "FRRN.046", "-", "Package choices of migrated policies", "Excel"),
    ("RPT-RN-12", "Pipeline, Full Production, Audit Log Reports", "FRRN.044.01", "Period, unit, segment",
     "Pipeline by stage; booked production; audit entries", "Excel, PDF"),
    ("RPT-RN-13", "CLPC Billing Reports (three)", "FRRN.044.01", "Period", "Billed accounts, built-in and "
     "non-built-in", "Excel, PDF"),
    ("RPT-RN-14", "Outcome per Stage, Placement Summary, ePolicy Sent, RA Generated", "FRRN.044.01", "Period",
     "Successful and fall-out accounts; placements; e-policies; Renewal Advices", "Excel, PDF"),
    ("RPT-RN-15", "Penetration, Hold Cover, Booked Non-Bank, Booked PEP, No Policy Number", "FRRN.044.01", "Period",
     "As named", "Excel, PDF"),
    ("RPT-RN-16", "Insurer Scorecard, Unapplied Payment with Disposition", "FRRN.044.01", "Period, insurer",
     "Insurer response and placement figures; unapplied payments", "Excel, PDF"),
    ("RPT-RN-17", "LAMD reconciliation reports (five)", "FRRN.044.01", "Month", "RMU and submitted accounts against "
     "the bookings", "Excel, PDF"),
    ("RPT-RN-18", "Renewal and Processing Dashboard drill-downs", "FRRN.002.02, 003.02", "Dashboard filters",
     "Accounts behind a figure", "Excel, PDF, CSV"),
]

DOCUMENTS = [
    # id, document, format and file name, protection, FRRN
    ("DO-01", "Renewal Advice (first, last notice; FFY Reminder)", "PDF <Product>_RA_<Notice>_<Reference>_MMDDYYYY.pdf",
     "Password protected", "FRRN.022.01"),
    ("DO-02", "No Advice, Non-Renewal, SFU, NRNS reminder, non-acceptance letters",
     "PDF <Product>_<Letter>_<Reference>_MMDDYYYY.pdf", "Password protected", "FRRN.023.02"),
    ("DO-03", "Insurer request (extract)", "Excel, 28 columns, one per insurer batch", "Password protected",
     "FRRN.021.01"),
    ("DO-04", "Quick and Full Proposal", "PDF Renewal_QuickProposal_ / Renewal_FullProposal_<ref>_MMDDYYYY.pdf",
     "Password protected", "FRRN.017.01"),
    ("DO-05", "Placement slip and placement file", "PDF <Insurer>_<Line>_PlacementSlip_<ref>_MMDDYYYY.pdf; Excel "
     "<Insurer>_<Line>_PlacementFile_MMDDYYYY.xlsx", "Password protected by e-mail", "FRRN.029.02"),
    ("DO-06", "Insurance Advice", "PDF IA_<Reference>_MMDDYYYY.pdf", "Password protected", "FRRN.032.01"),
    ("DO-07", "Hold cover request", "PDF per account; Excel per insurer for CBG", "Password protected by e-mail",
     "FRRN.036.02, 036.04"),
    ("DO-08", "CLPC billing file", "CSV per product line and kind (built-in, non-built-in) with the date MMDDYYYY", "LMS directory; "
     "protected by e-mail", "FRRN.027.01"),
    ("DO-09", "Renewal account details", "PDF from the record", "-", "FRRN.014.06"),
]

EXPORTS = [
    ("Renewal lists", "Excel, PDF, CSV", "Expiry List, My Dispositions, TL Review, Processing Worklist, Letters",
     "Rows and columns shown with the filters; RMEL_<MMM>_<YYYY>_<MMDDYYYY>.xlsx"),
    ("Upload results", "Excel", "Every upload", "Each row with its outcome and message"),
    ("Audit logs", "Excel, CSV", "Audit Logs", "Filtered rows; Audit Logs_MMDDYYYY"),
    ("Dashboard drill-downs", "Excel, PDF, CSV", "Renewal and Processing Dashboards", "Accounts behind a figure"),
]


def _notices():
    data = yaml.safe_load((PACK / "notifications.yaml").read_text(encoding="utf-8"))["notifications"]
    rows = []
    for n in data:
        trig = _clean(n["trigger"]).replace("7 by default", "5 working days by default")
        tmpl = _clean(n["template"])
        rows.append((n["id"], trig, _clean(n["recipient"]), tmpl, n["channel"]))
    return rows


NOTICES = _notices()

JOBS = [
    # job, when (Manila time), what it does, notices
    ("RNW_EXTRACTION", "Daily 01:00", "Extracts the policies at the lead days before expiry, one renewal account each.",
     "NT-12"),
    ("RNW_REEVALUATE", "Daily 01:30", "Runs the checks of every open renewal again; reclassifies.", "NT-01, NT-15"),
    ("RNW_EXPIRY_SWEEP", "Daily 00:15", "Closes renewals past expiry plus the non-acceptance days as expired "
     "without renewal.", "NT-08"),
    ("RNW_NRNS_LETTERS", "Daily 06:00", "Tags NRNS at the checkpoint; sends reminders and non-acceptance letters.",
     "NT-19"),
    ("RNW_LETTER_BATCH", "Hourly at :05", "Updates the delivery status of queued letters.", "NT-14"),
    ("RNW_HOLD_COVER", "Daily 05:30", "Requests hold cover of Clean CBG accounts; CBG Home extensions on the 15th.",
     "-"),
    ("RNW_CLPC_BILLING", "Daily 14:15", "CBG Home billing files daily; FFY Motor on the 10th and 25th.", "-"),
    ("RNW_KYC_MONITOR", "Daily 13:40", "Refreshes the KYC status of open renewals.", "-"),
    ("RNW_MFT_INBOX", "Every 15 minutes", "Takes placement responses, hold cover responses and e-policies from MFT.",
     "-"),
    ("RNW_CHANNEL_STATUS", "Every 15 minutes", "Reads CCM and MFT delivery status; retries pending messages.",
     "NT-14"),
    ("RNW_GOLIVE_EXTRACTION", "Once, at cut-over", "Takes over the migrated policies expiring to 31-May-2028.",
     "NT-09"),
]

# ================================================================================================ ICD
INTERFACES = [
    # id, interface, direction, means, frequency, content
    ("IF-RN-01", "CCM (client communications)", "BIBS to CCM; status back", "HTTPS JSON", "On send; status every "
     "15 min", "Renewal Advice, letters, proposals, Insurance Advice, e-policies"),
    ("IF-RN-02", "MFT outbound", "BIBS to insurer folders", "HTTPS file put", "On send", "Placement files, hold cover "
     "requests"),
    ("IF-RN-03", "MFT inbound", "Insurer folders to BIBS", "Folder poll", "Every 15 min", "Placement and hold cover "
     "responses, e-policies"),
    ("IF-RN-04", "Insurer files by upload or e-mail", "Both", "Excel / CSV", "Per batch", "Insurer request and "
     "disposition, placement and hold cover responses, e-policies"),
    ("IF-RN-05", "CLPC billing to LMS", "BIBS to LMS directory and recipients", "CSV file", "Daily; 10th, 25th",
     "CBG Home and FFY Motor billing"),
    ("IF-RN-06", "LAMD and BDOFC / BDOSOLD reports", "Bank units to BIBS", "Excel / CSV upload", "Monthly",
     "Loan status, paid-off accounts, BDOFC and BDOSOLD lists"),
]
# Upload handlers whose templates define the inbound files (read from the system for the ICD).
INBOUND_HANDLERS = [
    ("IF-RN-03, IF-RN-04", "Placement response", "PlacementResponseHandler"),
    ("IF-RN-03, IF-RN-04", "Hold cover response", "HoldCoverResponseHandler"),
    ("IF-RN-04", "Insurer disposition", "InsurerDispositionHandler"),
    ("IF-RN-04", "Insurer response (batch)", "InsurerResponseBulkHandler"),
    ("IF-RN-04", "E-policy numbers", "EpolicyNumbersHandler"),
    ("IF-RN-06", "LAMD loan report", "LamdReportHandler"),
    ("IF-RN-06", "LAMD CBG loans", "LamdCbgLoansHandler"),
    ("IF-RN-06", "LAMD paid-off accounts", "LamdPaidOffHandler"),
    ("IF-RN-06", "BDOFC and BDOSOLD report", "BdofcSoldHandler"),
]
BILLING_COLUMNS = ["No.", "PN #", "Account No.", "Assured's Name", "Loan Booking Date", "Loan Maturity Date",
                   "Inception Date", "Expiry Date", "Loan Type", "Total Premium (1 Year)",
                   "Logical Branch Description PN", "Insure Branch", "Enrolled Account Number"]
HOLD_COVER_COLUMNS = ["Reference Number", "Renewal Reference Number", "Assured's Name", "Expiring Policy No.",
                      "Expiry Date", "Hold Cover Start", "Hold Cover End", "Days", "Share (%)", "Request"]
CCM_FIELDS = [
    ("clientId", "Client identifier of BIBS at CCM (setting RNW_CCM_CLIENT_ID)"),
    ("messageNo", "Message number of BIBS"), ("to, cc", "Recipient and copy e-mail addresses"),
    ("subject, body", "Subject and text of the e-mail"),
    ("attachment.fileName, content, password", "File name, file (Base64) and its password"),
]
CCM_STATUS = [("SENT", "Sent"), ("DELIVERED", "Delivered"), ("FAILED, BOUNCED", "Failed"),
              ("CANCELLED", "Cancelled")]
