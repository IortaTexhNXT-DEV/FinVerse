"""Process, storyboard and output content of the Operations Cashiering document set v3.4 (BRD-02): the journey, to-be
processes and storyboards (BPS), the reports, documents, exports, notices and jobs (RNO) and the interfaces (ICD).
Read by build_brd02_v34.py, brd02_pack.py and build_brd02_v34_workbooks.py.

The storyboards are performed live on a local seed environment by flows/brd02/shots_v34_steps.cjs, one full-page
screenshot per step (figures/brd02_v34_screens, sbN-nn.png).
"""

from __future__ import annotations

# ================================================================================================ BPS
JOURNEY = [
    ("1", "Client or insurer", "Pays at a Head Office or branch counter, by a bank channel (Bills Payment, CLPC, "
     "Trade, Direct Credit), by post-dated check or through an insurer settlement."),
    ("2", "Cashier", "Creates the AR or OR record with the accounts paid and the bank account to post to, and submits "
     "it for posting."),
    ("3", "Cashiering Team Leader", "Posts the record: BIBS issues the AR or OR number, applies the payment by "
     "component and posts the journal; or returns the record with a reason."),
    ("4", "BIBS", "Matches and applies the payment files and matured checks row by row; each payment gets its AR "
     "and a result (Applied, Excess, Pre-booked, Unbooked/Unmatched)."),
    ("5", "Cashier", "Prints the ARs and ORs in batches; the BDOI Copy is kept with the receipt."),
    ("6", "Cashier and Team Leader", "Work the unapplied payments: apply to another invoice, refund, reclass or "
     "transfer; the Team Leader approves refunds, reclasses and transfers."),
    ("7", "Cashier and Team Leader", "Cancel or reinstate a receipt through a record posted by a second user; BIBS "
     "reverses or re-posts the payment and the journal."),
    ("8", "BIBS", "The paid premium is read by the Remittance extraction; refunds go to Disbursement; journals to "
     "the general ledger."),
    ("9", "Accounting (BRD-05)", "Deposits the day's collections and reconciles the bank accounts on Finance › "
     "Receivables against the bank statements."),
    ("10", "Cashiering Team Leader", "Reviews the Day-End List and the Daily Cash Reconciliation report."),
]

PROCESSES = [
    {"id": "P-CSH-01", "name": "Collection receipt (AR)", "steps": "1 to 3",
     "trigger": "Payment of a client at the counter.", "actors": "Cashier, Cashiering Team Leader",
     "outcome": "AR issued from the branch series; payment applied by component; excess in the Unapplied list; "
                "journal posted.",
     "controls": "BR-CSH-01, BR-CSH-02, BR-CSH-04 to BR-CSH-11, BR-CSH-15."},
    {"id": "P-CSH-02", "name": "OR issuance", "steps": "1 to 3",
     "trigger": "Service fee, profit share, commission, incentive or other income received at Head Office; insurer "
                "settlement approved by Disbursement; Commission Schedule processed.",
     "actors": "Head Office Cashier, Cashiering Team Leader, BIBS",
     "outcome": "OR issued from the Head Office series with its VAT and withholding tax; queued for printing.",
     "controls": "BR-CSH-01 to BR-CSH-04, BR-CSH-15."},
    {"id": "P-CSH-03", "name": "Payment files and automatic matching", "steps": "4",
     "trigger": "File uploaded on Payment Uploads or received through MFT; post-dated check at maturity.",
     "actors": "Cashier, BIBS",
     "outcome": "One AR per payment; each row Applied, Excess, Pre-booked or Unbooked/Unmatched; run report.",
     "controls": "BR-CSH-05, BR-CSH-06, BR-CSH-08, BR-CSH-17."},
    {"id": "P-CSH-04", "name": "Unapplied payments and dispositions", "steps": "6",
     "trigger": "Excess, unmatched, returned or migrated money; collector request.",
     "actors": "Cashier, Cashiering Team Leader, Marketing Collection, Disbursement",
     "outcome": "Balance applied, refunded, reclassified, transferred or taken to income.",
     "controls": "BR-CSH-01, BR-CSH-16, BR-CSH-20."},
    {"id": "P-CSH-05", "name": "Cancellation and reinstatement", "steps": "7",
     "trigger": "Error, double issuance, returned check, mis-application.",
     "actors": "Cashier, Cashiering Team Leader",
     "outcome": "Receipt cancelled or reinstated; payments and journal reversed or re-posted; AR Insurer Refund for a "
                "remitted AR.",
     "controls": "BR-CSH-01, BR-CSH-12 to BR-CSH-15."},
    {"id": "P-CSH-06", "name": "Deposit and bank reconciliation", "steps": "9",
     "trigger": "End of the business day; bank statement received.",
     "actors": "Accounting (BRD-05), Cashiering Team Leader",
     "outcome": "The postings of Cashiering on each bank account (Post to Bank Account) are deposited and "
                "reconciled on Finance › Receivables (Cheques & Deposits, Bank Statements, Bank Reconciliation).",
     "controls": "Day-End List and Daily Cash Reconciliation report; BR-CSH-15."},
    {"id": "P-CSH-07", "name": "Remittance hand-off", "steps": "8",
     "trigger": "Premium applied to a booked invoice.", "actors": "BIBS, Remittance",
     "outcome": "The paid premium is extracted by Remittance per insurer; a cancellation after remittance becomes an "
                "AR Insurer Refund; a remittance return comes back as an unapplied payment.",
     "controls": "BR-CSH-13, BR-CSH-14."},
    {"id": "P-CSH-08", "name": "Set-up under maker-checker", "steps": "-",
     "trigger": "A new receipt series, form change or Cashiering setting.",
     "actors": "Cashiering Team Leader (maker), second authorised user (checker)",
     "outcome": "Approved series, form or setting in force.", "controls": "BR-CSH-01, BR-CSH-18."},
]

# Storyboard steps whose page is exactly a screen state of the UX design (one image kept).
SAME_AS = {"sb3-02": "ux-16-01-files", "sb3-03": "ux-16-02-report", "sb3-04": "ux-08-01-list"}

# Storyboards: id, title, summary, test data, steps (who, screen, does, sees, screenshot).
STORYBOARDS = [
    {"id": "SB-01", "title": "A premium payment from the counter to the issued AR",
     "summary": "A Head Office cashier receives a client's premium in cash, saves the AR record and submits it; the "
                "Cashiering Team Leader posts it; the AR is issued, the payment applied by component and the AR "
                "queued for printing.",
     "data": "Seed account of the ARN series 2026-94 with an outstanding balance; fictitious payor",
     "steps": [
         ("Cashier", "Create AR/OR", "Opens Create AR/OR, finds the account, enters the payor, Cash, the bank account, "
          "the amount and the remarks", "The account with its outstanding balance and the preview of the application",
          "sb1-01"),
         ("Cashier", "Cashiering Record", "Saves the record and clicks Submit for Posting",
          "Record CR-AR-<n> For Posting; the receipt number is given at posting", "sb1-02"),
         ("Cashiering Team Leader", "Posting", "Opens Posting", "The record among the AR/OR records for posting",
          "sb1-03"),
         ("Cashiering Team Leader", "Cashiering Record", "Opens the record and clicks Post",
          "The confirmation: the AR will be issued, payments applied, any excess in the Unapplied list", "sb1-04"),
         ("Cashiering Team Leader", "Cashiering Record", "Confirms", "AR/OR Issued with the AR number", "sb1-05"),
         ("Cashier", "Receipt", "Opens the AR", "The AR Issued with its application by component", "sb1-06"),
         ("Cashier", "Batch Print", "Opens Batch Print", "The AR in the print queue", "sb1-07"),
     ]},
    {"id": "SB-02", "title": "A service fee OR at Head Office",
     "summary": "The Head Office cashier records a service fee paid by an insurer by check, with VAT; the Team Leader "
                "posts it and the OR is issued from the Head Office series.",
     "data": "Seed insurer Mabuhay General Insurance Corp.; check past its holding period",
     "steps": [
         ("Cashier", "Create AR/OR", "Opens Create OR, chooses Service Fee, the insurer, Check with its date, the "
          "amount and the VAT", "The OR record with the payment type Check and the VAT", "sb2-01"),
         ("Cashiering Team Leader", "Cashiering Record", "Opens the submitted record and posts it",
          "AR/OR Issued with the OR number of the Head Office series", "sb2-02"),
         ("Cashier", "Receipt", "Opens the OR", "The OR Issued with its gross amount, VAT and lines", "sb2-03"),
     ]},
    {"id": "SB-03", "title": "A Bills Payment file from upload to matching",
     "summary": "The cashier uploads a Bills Payment file of the bank channel. BIBS checks the file, matches each row "
                "and issues one AR per payment; the run report gives the result per row; the unmatched payment waits "
                "in the Unapplied list.",
     "data": "Bills Payment file of six rows in BDOI's layout made by the channel simulator of the test environment",
     "steps": [
         ("Cashier", "Payment Uploads", "Chooses Bills Payment and the file", "The file chosen with its type, ready "
          "to upload", "sb3-01"),
         ("Cashier", "Payment Files", "Opens Payment Files", "The file Processed with its counts per result",
          "sb3-02"),
         ("Cashier", "Payment Files", "Opens the run report of the file", "Each payment with its AR and result",
          "sb3-03"),
         ("Cashier", "Unapplied Payments", "Opens Unapplied Payments", "The unmatched payment as Unbooked/Unmatched",
          "sb3-04"),
     ]},
    {"id": "SB-04", "title": "An excess payment refunded through Disbursement",
     "summary": "An unapplied payment is refunded: the cashier gives the Refund disposition and submits it, the Team "
                "Leader approves it and the refund request reaches the Disbursement queue.",
     "data": "Seed unapplied payment without a match; fictitious payee",
     "steps": [
         ("Cashier", "Unapplied Payment", "Opens the unapplied payment and fills the Refund disposition",
          "The disposition form with the payee and remarks", "sb4-01"),
         ("Cashier", "Unapplied Payment", "Submits the disposition", "For Approval in the workflow header", "sb4-02"),
         ("Cashiering Team Leader", "Unapplied Payment", "Opens it from For Approval and approves",
          "The disposition approved; the refund request sent to Disbursement", "sb4-03"),
         ("Disbursement", "Disbursement Queue", "Opens the Disbursement Queue", "The refund request to acknowledge",
          "sb4-04"),
     ]},
    {"id": "SB-05", "title": "Cancellation of an issued AR",
     "summary": "An AR issued twice is cancelled: the cashier raises the cancellation record with its reason, the Team "
                "Leader posts it and the AR becomes Cancelled with its payments reversed.",
     "data": "Seed AR issued at Head Office; reason Double issuance",
     "steps": [
         ("Cashier", "Receipt", "Opens the AR, clicks Cancel Receipt and chooses the reason",
          "The cancellation dialog with the reason", "sb5-01"),
         ("Cashiering Team Leader", "Posting", "Opens the cancellation records for posting",
          "The cancellation record CN-AR-<n> For Posting", "sb5-02"),
         ("Cashiering Team Leader", "Cashiering Record", "Opens the record and posts it",
          "AR/OR Cancelled", "sb5-03"),
         ("Cashier", "Receipt", "Opens the AR", "The AR Cancelled with its cancellation record in the history",
          "sb5-04"),
     ]},
]

# ================================================================================================ RNO
_R = "Report Centre › Operations › Cashiering"
REPORTS = [
    # id, report, FRS, parameters, content, output
    ("RPT-CSH-01", "Applied Premium Report", "FRS.CSH.09.02.01", "Period", "Premium applied to invoices by AR: AR "
     "no., date, amount, invoice, payor, bank, branch, check, risk code", "Excel"),
    ("RPT-CSH-02", "Applied Commission Report", "FRS.CSH.09.02.02", "Period", "Commission ORs with invoices, "
     "insurer, basic commission, WTAX, EVAT, net", "Excel"),
    ("RPT-CSH-03", "Minimal Balance of Unapplied Payments (Excess Payments)", "FRS.CSH.09.02.03", "Period",
     "Excess payments moved to AP overages", "Excel"),
    ("RPT-CSH-04", "Cancelled Official Receipts", "FRS.CSH.09.02.04", "Period", "Date issued, OR, assured, gross, "
     "VAT, WTAX, amount, reason", "Excel, PDF"),
    ("RPT-CSH-05", "Cancelled Acknowledgment Receipts", "FRS.CSH.09.02.05", "Period", "Date issued, reason, AR, "
     "assured, amounts", "Excel, PDF"),
    ("RPT-CSH-06", "Unapplied Commission Receivable Extract for Mancom Reports", "FRS.CSH.09.02.06", "End date",
     "Commission ORs with an OR balance, age, encoder", "Excel"),
    ("RPT-CSH-07", "Unapplied Commission Receivable Payments YTD balance per Criteria", "FRS.CSH.09.02.07",
     "Year, criteria", "Year-to-date unapplied commission", "Excel"),
    ("RPT-CSH-08", "Unapplied Premium Minimal Balance", "FRS.CSH.09.02.08", "Period", "ARs with minimal premium "
     "balances reversed", "Excel, PDF"),
    ("RPT-CSH-09", "Unapplied Commission Minimal Balance", "FRS.CSH.09.02.09", "Period", "ORs with minimal "
     "commission balances", "Excel, PDF"),
    ("RPT-CSH-10", "Daily Cash Reconciliation Reports", "FRS.CSH.09.02.10", "Period", "ORs of each day "
     "with encoder, payor and deposit bank; grand total", "Excel, PDF"),
    ("RPT-CSH-11", "Unapplied Premium Payment Transaction", "FRS.CSH.09.02.11", "Period", "Advance and unapplied "
     "premium payments with ageing, application and disposition", "Excel, PDF"),
    ("RPT-CSH-12", "Direct Payment", "FRS.CSH.09.02.12", "Period", "Direct payment accounts with their commission "
     "OR, reversal and OR balance", "Excel"),
    ("RPT-CSH-13", "Reinstatement Monitoring Report", "FRS.CSH.09.02.13", "Period", "Reinstatements with status, "
     "reason and approvers", "Excel, PDF"),
    ("RPT-CSH-14", "Re-Application Report", "FRS.CSH.09.02.14", "Period", "Payments re-applied after a "
     "reinstatement or correction", "Excel, PDF"),
    ("RPT-CSH-15", "Certification of Payment", "FRS.CSH.09.02.15", "Period", "Certificates of Payment issued",
     "Excel, PDF"),
    ("RPT-CSH-16", "Reinstatement", "FRS.CSH.09.02.16", "Period", "Reinstatements posted per account and receipt "
     "with turnaround", "Excel, PDF"),
    ("RPT-CSH-17", "CWT Monitoring Report (Premium)", "FRS.CSH.09.02.17", "Period", "Withholding tax per invoice "
     "with its BIR 2307 tag", "Excel, PDF"),
    ("RPT-CSH-18", "Summary Per Disposition - Remittance", "FRS.CSH.09.02.18", "Period", "Unapplied payments by "
     "disposition type", "Excel, PDF"),
    ("RPT-CSH-19", "Post-dated Checks Warehousing", "FRS.CSH.09.02.19", "End date", "Warehoused checks by maturity "
     "date", "Excel, PDF"),
    ("RPT-CSH-20", "Priority Posted Accounts", "FRS.CSH.09.02.20", "Period", "Payments posted with encoder and "
     "branch", "Excel, PDF"),
    ("RPT-CSH-21", "Payment Reversals", "FRS.CSH.09.02.21", "Period", "Cancellation and reinstatement records "
     "posted", "Excel, PDF"),
    ("RPT-CSH-22", "AR Outstanding with Ageing", "FRS.CSH.09.02.22", "End date", "Premium receivable outstanding "
     "per client and account, aged from booking", "Excel, PDF"),
    ("RPT-CSH-23", "Payment Batch Run Report", "FRS.CSH.09.02.23", "Period", "Payment uploads with applied, excess, "
     "pre-booked, unapplied and failed records", "Excel"),
    ("RPT-CSH-24", "Legacy Unapplied Payments", "FRS.CSH.06.01.02", "End date", "Unapplied payments carried over "
     "from the legacy systems with legacy receipt and age [Data Migration scope]", "Excel"),
    ("RPT-CSH-25", "Unapplied Payments Reclassified to Income", "FRS.CSH.06.01.02", "Period", "Unapplied payments "
     "taken to other income after the two approvals [Data Migration scope]", "Excel"),
    ("RPT-CSH-26", "Check Pick-Up Requests", "-", "Period", "Checks tagged for pick-up and their ARs (CSHID.009 "
     "removed; Q-CSH-07)", "Excel"),
]
REPORT_NOTE = (f"The reports are under {_R}. File name <Report Name>_<date of extraction MMDDYYYY> (BR-CSH-21). Every "
               "report applies the user's data access, is logged with the user, parameters and time, and can be "
               "scheduled with its format and recipients.")

DOCUMENTS = [
    # id, document, format and file name, protection, FRS
    ("DO-01", "Acknowledgement Receipt (AR)", "PDF AR_<AR number>.pdf; Client's Copy and BDOI Copy; REPRINT on a "
     "later print", "BDOI Copy kept with the receipt", "FRS.CSH.02.04, FRS.CSH.02.06.03"),
    ("DO-02", "Official Receipt (OR)", "PDF OR_<OR number>.pdf; Client's Copy and BDOI Copy", "BDOI Copy kept "
     "with the receipt", "FRS.CSH.02.04, FRS.CSH.02.06.02"),
    ("DO-03", "Print batch", "One PDF of the batch for the printer; ZIP of the receipts for download",
     "Kept with the batch", "FRS.CSH.02.04.09, FRS.CSH.02.04.10"),
    ("DO-04", "Certification of Payment", "PDF with its series number", "-", "FRS.CSH.02.06.04"),
    ("DO-05", "Validation report of an upload", "Excel: accepted and refused rows with the reason", "-",
     "FRS.CSH.02.02.04"),
    ("DO-06", "Run report of a payment file", "On screen; Excel report, failed rows and matching list", "-",
     "FRS.CSH.05.01.12"),
    ("DO-07", "Matched and unmatched list of a Commission Schedule", "Excel", "-", "FRS.CSH.02.02.09"),
]
EXPORTS = [
    ("Posting lists, Receipts, Unapplied Payment List", "Excel", "Export of the list", "Columns of the screen"),
    ("Day-End List", "Screen", "Day-End List", "Records of the day and totals per payment type and bank account"),
    ("Search Log", "Screen", "Search Log", "User, time, criteria, results"),
    ("Raw payment file", "Original file", "Payment Files › Download", "The file as received, unchanged"),
]

NOTICES = [
    # id, event, to, text, channel
    ("NT-01", "AR / OR, cancellation or reinstatement record submitted", "Users with the posting permission",
     "<record> for posting: <kind> of <amount> waits for posting", "Bell (e-mail by choice)"),
    ("NT-02", "Record posted", "Creator", "<record> posted: <status>: <AR or OR> <number>", "Bell (e-mail by choice)"),
    ("NT-03", "Record returned", "Creator", "<record> returned: Reason: <reason>", "Bell (e-mail by choice)"),
    ("NT-04", "Disposition for approval", "Team Leaders", "<reference> disposition for approval: <type> <currency> "
     "<amount>", "Bell (e-mail by choice)"),
    ("NT-05", "Receipt cancellation or reinstatement for approval (posting step off)", "Team Leaders",
     "<transaction> for approval: <action> of receipt, reason: <reason>", "Bell (e-mail by choice)"),
    ("NT-06", "Collector request", "Cashiers", "<request>: <action> requested by a collector: <reference> to invoice "
     "<invoice>", "Bell"),
    ("NT-07", "Refund validation requested", "Cashiers", "<task>: refund validation requested: Confirm that the "
     "premium of <invoice> is back in the unapplied list", "Bell"),
    ("NT-08", "Payment reversal for approval", "Team Leaders", "<request>: payment reversal for approval: Receipt "
     "<receipt> on invoice <invoice>", "Bell"),
    ("NT-09", "AR or OR form change", "Form approvers", "<kind> form version <n> for approval: Effective <date>, "
     "changed by <user>", "Bell (e-mail by choice)"),
    ("AL-01", "Receipt series low", "Team Leader (alert)", "<kind> series <prefix> has <n> numbers left", "Alerts"),
    ("AL-02", "Pre-booked payment ageing", "Team Leader (alert)", "Payment for <ARN> waits for the booking since "
     "<date>", "Alerts"),
    ("AL-03", "Payment file received via MFT refused", "Team Leader (alert)", "Payment file <name> received via MFT "
     "was refused: <reason>", "Alerts"),
]

JOBS = [
    # job, when (Manila time), what it does, notices
    ("PREBOOKED_REMATCH", "Every 2 hours", "Applies the pre-booked payments whose account is now booked.", "AL-02"),
    ("PAYMENT_AUTOMATCH", "Hourly at :30", "Matches the unapplied payments again against booked invoices.", "-"),
    ("PDC_MATURITY", "Daily 08:30", "Creates the payment and AR of each post-dated check at maturity.", "-"),
    ("MINIMAL_BALANCE_SWEEP", "Daily 04:00", "Reverses minimal premium balances and moves minimal excess to AP "
     "overages left by earlier postings.", "-"),
    ("CASH_MFT_INTAKE", "On demand (schedule set at go-live)", "Takes the payment files received through MFT and "
     "processes them.", "AL-03"),
]

# ================================================================================================ ICD
INTERFACES = [
    # id, interface, direction, means, frequency, content
    ("IF-CSH-01", "OBPCS Bills Payment", "OBPCS to BIBS", "MFT folder; upload", "Daily", "Bills Payment file "
     "BDOIYYYYMMDD.txt"),
    ("IF-CSH-02", "PMS post-dated checks", "PMS to BIBS", "MFT folder; upload", "Daily", "Matured checks file "
     "BDOIYYYYMMDDPMS.txt"),
    ("IF-CSH-03", "Bank channel files", "Business Unit to BIBS", "Upload", "On receipt", "Trade (CIB), CLPC, Direct "
     "Credit"),
    ("IF-CSH-04", "Commission Schedule", "Insurer to BIBS", "Upload", "On receipt", "Commission paid per account"),
    ("IF-CSH-05", "Collector requests", "Collections to Cashiering", "Within BIBS", "On request", "Apply, refund or "
     "transfer an unapplied payment"),
    ("IF-CSH-06", "Refunds and settlements", "Cashiering and Disbursement", "Within BIBS", "On approval", "Refund "
     "requests and their status; approved insurer settlements"),
    ("IF-CSH-07", "Payment confirmation and paid premium", "Cashiering to New Business and Remittance",
     "Within BIBS", "At posting", "Payments per account"),
    ("IF-CSH-08", "Journals", "Cashiering to the general ledger", "Within BIBS", "At posting", "Journal of every "
     "posting"),
]

for _sb in STORYBOARDS:
    _sb["steps"] = [(w, s, a, r, SAME_AS.get(f, f)) for w, s, a, r, f in _sb["steps"]]
