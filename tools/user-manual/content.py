"""Text of the iNXT BrokerVerse User Manual.

Each chapter is a list of blocks read by build_user_manual.py:
  ("h1", title) ("h2", title) ("h3", title) ("p", text) ("bullets", [text, ...])
  ("steps", [text, ...]) ("table", [headers], [[cells], ...], [widths in cm])
  ("tip", text) ("note", text) ("figure", key, caption)
Text may use **bold**. Headings are numbered by the builder.
"""

INTRODUCTION = [
    ("h1", "Introduction"),
    ("h2", "About iNXT BrokerVerse"),
    ("p", "iNXT BrokerVerse is the insurance broking platform from iorta TechNXT. It brings the whole broking "
          "cycle into one system: client onboarding and KYC, quotations and proposals, placement with insurers, "
          "policy issuance and booking, collections and cashiering, remittance to insurers, commissions, "
          "disbursement, accounting, claims follow-up and reporting."),
    ("p", "Every team works from the same client, account and invoice records, so a change made in one area is "
          "visible to the next team straight away, and every step is recorded with the user, date and time."),
    ("h2", "Who this manual is for"),
    ("p", "This manual is for business users of BrokerVerse: account officers, team leaders, technical services, "
          "processing, finance and claims staff, and system administrators. It explains how to sign in, how "
          "the screens work and what each module is used for. It is not a procedures manual; your "
          "organisation's own policies still decide who does what and when."),
    ("h2", "How the manual is organised"),
    ("table", ["Chapter", "What it covers"], [
        ["2  Getting started", "Signing in, passwords, sessions and signing out"],
        ["3  Finding your way around", "The screen layout, My Work, approvals, notifications and common screen controls"],
        ["4  The broking cycle at a glance", "How a client request moves through BrokerVerse from quotation to remittance"],
        ["5  Client and policy", "Clients, screening, quotations, proposals, accounts, placement, issuance, booking and endorsements"],
        ["6  Operations and finance", "Collections, cashiering, remittance, commission, disbursement and accounting"],
        ["7  Claims handling", "Recording and following up client claims with insurers"],
        ["8  Reports", "Running, saving and exporting reports"],
        ["9  Setup and administration", "Reference data, templates and user access"],
        ["10  Help and support", "Common questions and how to get help"],
    ], [5.2, 11.4]),
    ("h2", "Your role decides what you see"),
    ("p", "BrokerVerse shows each user only the menus and actions their role allows. An account officer will not "
          "see the disbursement screens, and a cashier will not see product set-up. Buttons such as Approve, "
          "Post or Release appear only for users who hold that authority."),
    ("tip", "If this manual describes a screen you cannot find in your menu, your role probably does not include "
            "it. Ask your system administrator to check your access."),
]

GETTING_STARTED = [
    ("h1", "Getting started"),
    ("h2", "What you need"),
    ("bullets", [
        "A desktop or laptop with an up-to-date Google Chrome or Microsoft Edge browser.",
        "The BrokerVerse web address for your organisation.",
        "A user ID and a password, issued by your system administrator.",
    ]),
    ("p", "A screen resolution of at least 1366 x 768 is recommended. BrokerVerse does not need any software to "
          "be installed on your computer."),
    ("h2", "Signing in"),
    ("steps", [
        "Open your browser and go to the BrokerVerse web address.",
        "Enter your **User ID** and **Password**.",
        "Click **Login**. Your home page opens.",
    ]),
    ("p", "After several failed attempts in a row your user ID is locked for security. Your system administrator "
          "can unlock it."),
    ("h2", "Your first sign-in and changing your password"),
    ("p", "The first time you sign in, and whenever an administrator has reset your password, BrokerVerse asks "
          "you to choose your own password before the home page opens. The same happens when your password "
          "has expired."),
    ("p", "A password must meet these rules:"),
    ("table", ["Rule", "Standard setting"], [
        ["Length", "At least 10 characters"],
        ["Characters", "At least one upper-case letter, one lower-case letter, one digit and one symbol"],
        ["Re-use", "Must differ from your last 8 passwords"],
        ["Changes", "Can be changed once a day"],
        ["Expiry", "Expires after 90 days; you are reminded 7 days before"],
    ], [4.0, 12.6]),
    ("p", "These are the standard settings; your organisation may have adjusted them. To change your password "
          "at any time, open **My Profile** (click your name at the top right) and use **Change password**."),
    ("h2", "If you forget your password"),
    ("steps", [
        "On the sign-in page, click **Forgot password?**",
        "Enter your user ID and submit.",
        "Open the e-mail you receive and follow the link. The link works once and is valid for 30 minutes.",
        "Choose a new password and sign in with it.",
    ]),
    ("note", "If your organisation signs you in with your corporate network account, your password is managed "
             "by your IT help desk and not by BrokerVerse."),
    ("h2", "Staying signed in and signing out"),
    ("bullets", [
        "If you are inactive for a while, a warning appears. Click **Stay Signed In** to carry on working; "
        "if you do not, you are signed out automatically (after 30 minutes of inactivity by default).",
        "A session also ends at a fixed time after you sign in. You are warned 30 minutes before.",
        "You can work in several browser tabs at once. They share one session, and signing out in one tab "
        "signs you out of all of them.",
        "To sign out, click **Sign out** at the top right of the screen. Always sign out on a shared computer.",
    ]),
    ("p", "**My Profile** lists your recent sessions and how each one ended, so you can spot a sign-in you do "
          "not recognise."),
]

FINDING_YOUR_WAY = [
    ("h1", "Finding your way around"),
    ("h2", "The screen layout"),
    ("p", "Every screen has the same frame: the menu on the left, a header across the top, and the page you are "
          "working on in the middle."),
    ("table", ["Area", "What it does"], [
        ["Menu (left)", "Your home page, My Approvals and My Work at the top, then the menu groups: Client & Policy, "
                        "Operations, Finance, Claims & Insurance, Reports and Setup & Administration. Click a group "
                        "to open it. The screen you are on is highlighted."],
        ["Company and branch", "The two selectors at the top left of the header. Lists, dashboards and reports follow "
                               "the company and branch you choose here. Choose All branches to see everything you "
                               "are allowed to see."],
        ["Notifications", "The bell shows new notifications for you, such as an item assigned to you or a record "
                          "returned. Click it to open the list."],
        ["My Approvals", "The inbox icon, with the number of items waiting for your authorisation."],
        ["Alerts", "Shown to users who follow control alerts, with the number of open alerts."],
        ["Help", "The question mark opens the Help Center."],
        ["About", "The i icon shows the product name and version."],
        ["Your name", "Opens My Profile: your details, roles, password and recent sessions."],
        ["Sign out", "Ends your session."],
    ], [4.0, 12.6]),
    ("h2", "Your home page"),
    ("p", "Your home page depends on your role. Broking users land on the **New Business Dashboard**: new "
          "requests, quotations sent this month, service level breaches, bookings of the month and production "
          "against target. Finance users land on a dashboard with premium, collections, receivables ageing, "
          "payables due, cash position and pending approvals. Click a figure to open the records behind it."),
    ("h2", "My Work"),
    ("p", "**My Work** lists everything waiting for your team: quotations, proposal requests, accounts and other "
          "work items, oldest due first. The tiles at the top show what is assigned to you, what is open in your "
          "team's queues and what is overdue."),
    ("steps", [
        "Click a tile to see only that stage.",
        "Use the tabs to switch between your own items, the unassigned team queue and everything.",
        "Click **Claim** on an unassigned item to take it over, then open it to work on it.",
        "Team leaders can assign an item to a team member or return it to the team queue.",
    ]),
    ("p", "Items that pass the service level for their stage are marked overdue."),
    ("h2", "My Approvals"),
    ("p", "**My Approvals** collects everything waiting for your authorisation across modules. Open an item to "
          "review it on its own screen, then approve, reject or return it. Once you act on it, it leaves your "
          "inbox."),
    ("p", "BrokerVerse applies the maker-checker principle throughout: you never see your own submissions in "
          "your approval inbox, and items above your authority limit are routed to someone with a higher "
          "limit."),
    ("h2", "Working with screens"),
    ("p", "Most screens follow a small number of patterns. Once you know them, you will find your way around new "
          "modules quickly."),
    ("h3", "Work lists"),
    ("bullets", [
        "Tabs across the top split the list by status, for example Drafts, For Review and Sent to Client.",
        "Quick filters pick out common cases, such as your own drafts or items expiring soon.",
        "The search box finds records by their number, client name or other key details.",
        "Click a column heading to sort; click a row to open the record.",
    ]),
    ("h3", "Record pages"),
    ("p", "A record page shows the record's number and status at the top, its details in tabs below, and the "
          "actions you can take on it (for example Submit, Approve or Return) in the action area. The "
          "**History** tab shows every status change with the user, time and any comment or reason."),
    ("h3", "Step-by-step forms"),
    ("p", "New quotations, accounts and similar records are created in guided steps. Use **Next** and **Back** "
          "to move between steps. **Save Draft** keeps your work so you can finish it later; **Submit** sends it "
          "on to the next team. Required fields are marked with an asterisk (*), and any problem is shown next "
          "to the field before you can continue."),
    ("h3", "Documents and attachments"),
    ("p", "Records that need supporting documents have a **Documents** tab or section. Choose the document type, "
          "then add one or more files. Documents generated by BrokerVerse, such as slips, letters and "
          "statements, can be downloaded as PDF and, where useful, as Word."),
    ("h3", "Exporting and printing"),
    ("p", "Lists and reports can be exported to Excel or PDF, and printed. The export uses the filters you have "
          "applied on screen."),
    ("h3", "Bulk uploads"),
    ("p", "Where many records need to be created or updated at once, **Bulk Processing** lets you download a "
          "template, fill it in and upload it. Each row is checked, and a result report shows which rows were "
          "accepted and why any were rejected."),
    ("h2", "Help Center"),
    ("p", "The Help Center (the question mark in the header) describes every screen: what it is for, how its "
          "workflow runs and which controls apply. Search by screen name, module or keyword, and click **Open** "
          "next to a screen to go straight to it."),
]

CYCLE = [
    ("h1", "The broking cycle at a glance"),
    ("p", "The diagram below shows the main path of a new piece of business through BrokerVerse. Each stage is "
          "handled in its own module and handed to the next team through the work queues."),
    ("figure", "cycle", "Figure 1. The main broking cycle in BrokerVerse"),
    ("table", ["Stage", "What happens", "Usually done by"], [
        ["Client", "A prospect is registered, KYC documents are verified and the client is confirmed. Sanction "
                   "screening runs in the background.", "Account officer, team leader"],
        ["Quotation", "Package products are quoted with the premium calculated on screen and sent to the client.",
         "Account officer, approver"],
        ["Proposal", "Risks outside the standard packages go to the Technical Services Unit (TSU), which obtains "
                     "terms from insurers.", "Account officer, TSU"],
        ["Account", "The accepted cover is captured as an account with its Account Reference Number (ARN) and "
                    "submitted to Processing.", "Account officer, Processing"],
        ["Placement", "Once payment or client confirmation is in, the account is placed with the insurer on a "
                      "placement slip.", "Processing"],
        ["Issuance", "The insurer's e-policy is uploaded, reviewed and sent to the client.", "Processing, e-policy team"],
        ["Booking", "The issued policy is booked as an invoice, with its accounting entries and the commission "
                    "service invoice to the insurer.", "Processing"],
        ["Collection", "Premium is followed up, received and applied to the invoice.", "Collections, Cashiering"],
        ["Remittance", "Collected premium, net of commission, is remitted to the insurer.", "Remittance, Disbursement"],
    ], [2.8, 9.8, 4.0]),
    ("p", "After booking, the account can still change: endorsements and cancellations go through Adjustment, "
          "production is reconciled with each insurer every month, and claims are followed up in Claims "
          "Handling."),
]

CLIENT_POLICY = [
    ("h1", "Client and policy"),
    ("p", "This chapter covers the modules in the **Client & Policy** menu group, in the order a new piece of "
          "business normally uses them."),
    ("h2", "Clients"),
    ("p", "The client record holds everything known about a client: identity, contacts and addresses, segment, "
          "KYC documents, tags, special instructions and all linked quotations, accounts and claims. A client "
          "moves through the stages Prospect, KYC for verification, KYC verified and Confirmed."),
    ("h3", "To register a client and complete KYC"),
    ("steps", [
        "Go to **Client & Policy > Clients > New Client**.",
        "Choose Individual or Corporate and enter the name. Enter the TIN, ID, e-mail and mobile number if you "
        "have them. Possible duplicates are shown as you type; check them before carrying on.",
        "Click **Save as Prospect**. The client is saved as a prospect and gets a prospect code.",
        "On the client page, upload the mandatory KYC documents and click **Submit KYC**.",
        "Your team leader checks the documents and clicks **Verify KYC**. The client can then be confirmed.",
    ]),
    ("p", "A prospect is enough to prepare a quotation, but the client must be confirmed before accounts can be "
          "created. **KYC Reviews Due** lists clients whose periodic KYC review is overdue or coming up."),
    ("h2", "Sanction screening"),
    ("p", "BrokerVerse checks clients against the sanctions, PEP and internal watchlists when they are "
          "registered, when an account is submitted, when a list changes and in a nightly run. A possible match "
          "becomes a screening case, which Compliance investigates and decides. Most users only notice "
          "screening when a client is put on hold pending a decision. Compliance staff work from "
          "**Screening Home**, **Cases** and **Matches**."),
    ("h2", "Quotations"),
    ("p", "Quotations are for package products. Each quotation has a Quotation No. and an ARN, which every "
          "account created from it carries."),
    ("h3", "To create a quotation"),
    ("steps", [
        "Go to **Quotation / Proposal > New Quotation**.",
        "Choose the client or prospect.",
        "Choose the product and enter the terms: insurer, period and validity.",
        "Add the risk items (vehicles, locations or persons). Items that share a risk group become one account "
        "when the client accepts them.",
        "Check the premium, which is calculated as you enter the details.",
        "Review, then click **Save Draft** or **Submit for Review**.",
    ]),
    ("p", "Once approved, quotations can be sent to clients from the **For Review** tab with **Send via Email**. "
          "Each client receives one e-mail with its quotations, and the password to open them follows "
          "separately. **Quotation Requests** holds requests received by e-mail or upload that are still waiting "
          "to be quoted."),
    ("h2", "Proposal requests (non-package risks)"),
    ("p", "Risks that do not fit a standard package, or that a package rule sends for technical review, go "
          "through a Proposal Request Form (PRF). The account officer raises the PRF with the risk details, the "
          "insurers to approach and the required documents. The Technical Services Unit (TSU) works it from the "
          "**TSU Workbench**, obtains terms from the insurers and prepares the proposal slip, which the account "
          "officer then sends to the client."),
    ("h2", "Accounts"),
    ("p", "An account is the cover the client has accepted. It carries the ARN from the quotation and moves from "
          "Marketing to Processing, then on to payment, placement, policy issuance and booking."),
    ("h3", "To create and submit an account"),
    ("steps", [
        "Go to **Accounts & Placement > New Account**.",
        "Choose the client and the product. From this point the draft is saved automatically every 30 seconds "
        "and receives its ARN.",
        "Enter the period and payment details, then the risk items.",
        "Enter the contact details and check the premium.",
        "In the review step, attach the required documents. The completeness check tells you if anything is "
        "missing.",
        "Click **Submit to Processing**. The account goes to Processing for validation.",
    ]),
    ("p", "If Processing returns the account, it comes back to the account officer who created it with the "
          "reason. Make the corrections and click **Resubmit to Processing**. Accounts where the client pays the insurer "
          "directly are tagged **Direct Payment**; accounts with a free first year are tagged **FFY**. Both "
          "tags are set on the account's Details tab."),
    ("h2", "Employee benefits"),
    ("p", "The Employee Benefits module runs group health, group life and group personal accident programmes. "
          "A programme holds its benefit lines, HR contacts, documents and yearly cycle: renewal advice, insurer "
          "proposals, the comparative and its sign-off, the client's decision and placement. **Member Changes** "
          "tracks additions, deletions and plan changes; **Pending Items** follows contracts, cards and billings "
          "still to be received."),
    ("h2", "Placement"),
    ("p", "Processing places validated accounts with the insurers from the **Placement Workbench**. Its tiles "
          "and tabs show accounts awaiting payment, ready for placement, placed, returned by the insurer and "
          "with hold cover about to expire. Placement slips are produced per insurer branch and can be "
          "downloaded as PDF or Excel from **Placement Slips**, together with their send history."),
    ("h2", "Policy issuance"),
    ("p", "The **Issuance Workbench** follows placed accounts until the policy is issued."),
    ("bullets", [
        "**E-policy Upload**: upload one e-policy, or many at once. Each file is matched to its account by ARN or "
        "policy number, then reviewed before the policy number is updated.",
        "**Insurance Advice**: produce and send the Insurance Advice for mortgaged accounts to the mortgagee bank.",
        "**E-policy Dispatch**: send confirmed e-policies to clients one by one or in a batch, and follow each "
        "delivery.",
    ]),
    ("h2", "Booking"),
    ("p", "Booking turns an issued policy into a booked invoice in one step: the accounting entries, the client "
          "and insurer balances, and the commission service invoice to the insurer. Accounts ready to book are "
          "listed in the **Booking Workbench**. They can be booked individually or in batches, and **Batch "
          "Runs** shows the result of each account in a batch."),
    ("h2", "Endorsements and adjustments"),
    ("p", "Changes to a booked account (positive, negative and non-financial endorsements, and cancellations) "
          "are raised in **Adjustment > New Request**. The wizard takes you through choosing the invoices, "
          "describing the change, recalculating the premium and submitting. The request is then validated, "
          "approved and posted, and the **Adjustment Workbench** shows each request by stage."),
    ("h2", "Production reconciliation"),
    ("p", "Every month BrokerVerse extracts the register of booked accounts for each insurer. The insurer's "
          "feedback is uploaded and matched automatically within the agreed tolerance, and anything left "
          "unmatched is followed up to closure. Accounts an insurer reports that have not been booked appear in "
          "**Unbooked Accounts**."),
    ("h2", "Product maintenance"),
    ("p", "Product maintenance holds what your organisation sells and with whom: products and packages with "
          "their rules, the insurer panel with commission rates, rates and taxes, and the sales organisation. "
          "New or changed packages are requested through **Package Requests**, reviewed and negotiated by TSU, "
          "set up, and validated in the **Validation Queue** before new business can use them. The **Premium "
          "Calculator** lets you rate a product without creating a quotation."),
]

OPS_FINANCE = [
    ("h1", "Operations and finance"),
    ("p", "The **Operations** and **Finance** menu groups handle the money side of the business. They all work "
          "from one invoice ledger, so the premium, payments, remittance and commission of every booked invoice "
          "can be seen in one place."),
    ("h2", "Operations home and Invoice 360"),
    ("p", "**Operations Home** shows one card per team you belong to, with live counts of the work waiting. "
          "**Invoice Search** finds any booked invoice; opening it shows the **Invoice 360** view: its premium "
          "components and balances, insurer shares, every movement, related endorsements and one tab for each "
          "module that has touched it."),
    ("h2", "Collections"),
    ("p", "Collections follows up outstanding premium. The **PR Worklist** has one row per invoice with an "
          "outstanding balance, refreshed nightly and after every payment. Collectors record their follow-up "
          "and a disposition, and can capture promises to pay, installment plans and escalations. Team leaders "
          "assign accounts to collectors in **Assignments**."),
    ("h2", "Cashiering"),
    ("p", "Cashiering receives payments, issues acknowledgement and official receipts, and applies payments to "
          "invoices."),
    ("h3", "To receive a payment at the counter"),
    ("steps", [
        "Go to **Cashiering > Receive Payment**.",
        "Enter the ARN, invoice, policy or PN numbers, the payor and the amount.",
        "Check the preview. It shows which invoices are matched and how the payment is applied to each "
        "component, and any excess.",
        "Click **Issue AR and Apply**. The acknowledgement receipt (AR) is issued and the payment is applied.",
    ]),
    ("p", "Payment files from banks and other channels are loaded in **Payment Uploads**. Payments that cannot "
          "be applied go to **Unapplied Payments**, where they are applied, refunded or transferred. Payments "
          "received before an account is booked wait in **Pre-booked Payments** and are applied automatically "
          "once it is booked."),
    ("h2", "Remittance"),
    ("p", "Remittance extracts the collected premium due to each insurer, builds remittance batches, sends them "
          "for approval and on to Disbursement for payment, and records the insurer's official receipt when it "
          "comes back. Marketing can ask to hold an invoice out of remittance until a date through "
          "**Remittance Holds**."),
    ("h2", "Commission receivables"),
    ("p", "For accounts the client paid directly to the insurer, the commission is billed to the insurer. "
          "Commission Receivables confirms these direct payment accounts, bills the insurers, records their "
          "answers and the collection, and runs the insurer incentive schemes. It also tracks the withholding "
          "tax certificates the insurers issue on commission."),
    ("h2", "Disbursement"),
    ("p", "Disbursement pays what other teams request: remittances to insurers, client refunds, suppliers and "
          "others. Each payment is a disbursement voucher (DV), reviewed and approved before it posts. The "
          "**Disbursement End of Day** closes the approved vouchers of the day and produces the checks, bank "
          "credit files and reports."),
    ("h2", "Refund and cash advance requests"),
    ("p", "Client refund requests and employee cash advance requests are raised in **Refund & Cash Advance "
          "Requests**, reviewed and approved there, and then passed to Disbursement for payment. The request "
          "page shows the status of the payment, including the DV and check."),
    ("h2", "Accounting control and general ledger"),
    ("p", "The remaining Finance screens are used by the accounting team:"),
    ("table", ["Area", "Used for"], [
        ["ACSL", "Reconciling insurer statements of account and sub-ledgers with the general ledger, investigating "
                 "accounts and posting correction entries"],
        ["Accounting Reports", "The accounting report pack, account schedules and service fee runs"],
        ["General Ledger", "Journals, the chart of accounts, recurring journals and journal uploads"],
        ["Receivables & Banking, Payables & Cash", "Bank reconciliation, bank accounts and check books"],
        ["Assets & Investments", "Fixed asset and investment schedules"],
        ["Planning & Closing", "Budgets, month-end and year-end close"],
        ["Tax & Statutory", "Tax returns, certificates and statutory reports"],
    ], [5.2, 11.4]),
]

CLAIMS = [
    ("h1", "Claims handling"),
    ("p", "Claims Handling is the broker's case file for a client's claim against its insurers. The insurer "
          "decides and pays the claim; BrokerVerse records the loss, the insurer's claim numbers, the reserve "
          "and settlement as the insurer reports them, and keeps the follow-up on track."),
    ("h3", "To record a claim"),
    ("steps", [
        "Go to **Claims & Insurance > Claims Handling > Record Claim**.",
        "Find the cover by ARN, policy number or assured name. The cover card shows the policy, period, sum "
        "insured and any unpaid premium.",
        "Choose the affected locations or items and enter the details of the loss.",
        "Click **Save Claim**. The claim is given a claim number and appears in the **Claims Worklist**.",
    ]),
    ("p", "From the claim page you record the insurer's claim number, reserve and settlement, update the status, "
          "attach documents and keep the diary. **My Diary** gathers your calls, e-mails, meetings and "
          "follow-ups across all claims, with the ones due today and overdue. **Claims Home** gives an overview "
          "of open claims, follow-ups due and claims by status."),
]

REPORTS = [
    ("h1", "Reports"),
    ("p", "Reports are found in the **Reports** menu group. **New Business Reports** holds the operational "
          "reports of the broking teams (account status, placement, production and dispatch). The **Report "
          "Centre** holds the financial statements, registers and control reports. You only see the reports "
          "your role may run."),
    ("h3", "To run a report"),
    ("steps", [
        "Open the **Report Centre** or **New Business Reports** and choose the report.",
        "Fill in the parameters, such as the period, branch or insurer.",
        "Click **Run Report** to see the report on screen.",
        "Use **Print**, or download the report as Excel or PDF. Reports that are documents or schedules can also "
        "be downloaded as Word.",
    ]),
    ("tip", "If you run the same report often, save its parameters with **Save Variant**. Shared variants are "
            "offered to everyone who can run the report."),
    ("p", "Printed and PDF reports show the report name, the user, the run time and the filters used in the "
          "header, so a printout can always be traced back to how it was produced."),
]

ADMIN = [
    ("h1", "Setup and administration"),
    ("p", "The **Setup & Administration** menu group is used by business and system administrators. Changes to "
          "reference data and access follow maker-checker: one user makes the change and another approves it."),
    ("table", ["Screen", "Used for"], [
        ["Lists of Values", "The coded values offered in drop-down lists, such as segments, reasons, document types "
                            "and client tags, each with its order and effective period"],
        ["Document Templates", "The wording merged into quotations, slips, advices, e-mails and service invoices"],
        ["Outbound Messages", "A log of every e-mail sent or attempted, with its recipients, attachments and outcome"],
        ["Data Retention", "Retention rules by record type and the records due for review"],
        ["Compliance Setup", "Screening configuration, watchlists and review templates"],
        ["Access Requests", "Requests to enrol, change, deactivate or reactivate users"],
        ["Group Profile Requests", "Requests to create or change roles and their permissions"],
        ["User Access Matrix", "Every role against every permission, for review and sign-off"],
    ], [4.6, 12.0]),
    ("h2", "Requesting access for a user"),
    ("steps", [
        "Go to **User Access > Access Requests** and click **New Access Request**.",
        "Choose the action (enrol, modify, deactivate or reactivate), the user and the roles.",
        "Submit. The request goes to the approvers in turn and is then implemented by the system administrator.",
    ]),
    ("p", "Many users can be requested at once with **Bulk Request**, using the upload template. Every change "
          "to a user or role is recorded in the access audit log."),
]

SUPPORT = [
    ("h1", "Help and support"),
    ("h2", "Common questions"),
    ("table", ["Question", "Answer"], [
        ["I cannot sign in.", "Check your user ID and that Caps Lock is off. If your password has expired or was "
                              "reset, you will be asked to change it. If your user ID is locked, ask your system "
                              "administrator to unlock it."],
        ["A screen in this manual is not in my menu.", "Your role does not include it. Ask your system administrator "
                                                      "if you need it."],
        ["I cannot find a record I know exists.", "Check the company and branch selected in the header, and clear "
                                                  "any filters or tabs on the list."],
        ["The Submit button is not available.", "A required field or document is missing. The form shows what is "
                                                "missing; on accounts, the completeness check lists it."],
        ["I was signed out while working.", "Sessions end after a period of inactivity. Anything saved as a draft "
                                            "is kept; sign in again and carry on."],
        ["A record was returned to me.", "Open it from My Work or the notification. The reason is shown on the "
                                         "record and in its History tab."],
    ], [5.2, 11.4]),
    ("h2", "Getting help"),
    ("bullets", [
        "**In the system**: open the Help Center from the question mark in the header.",
        "**In your organisation**: contact your team leader or BrokerVerse system administrator for questions "
        "about access, procedures and data.",
        "**From iorta TechNXT**: raise product issues through your agreed support channel. When you report a "
        "problem, include the screen name, the record number, what you did and any message shown.",
    ]),
    ("p", "More about iorta TechNXT is available at www.iortatechnxt.com."),
]

GLOSSARY = [
    ("h1", "Glossary"),
    ("table", ["Term", "Meaning"], [
        ["AR / OR", "Acknowledgement receipt / official receipt issued for a payment"],
        ["ARN", "Account Reference Number, the reference an account carries from quotation to booking"],
        ["Booking", "Recording an issued policy as an invoice, with its accounting entries"],
        ["Direct payment (DP)", "An account where the client pays the premium straight to the insurer"],
        ["DV", "Disbursement voucher, the record of a payment made"],
        ["Endorsement", "A change to a booked policy; it can increase, reduce or not affect the premium"],
        ["E-policy", "The electronic policy document issued by the insurer"],
        ["FFY", "Free First Year, an account arrangement tracked with its FFY start and end dates"],
        ["KYC", "Know your customer: the identity checks and documents needed before a client is confirmed"],
        ["Maker-checker", "The control where one user makes a change and a different user approves it"],
        ["PEP", "Politically exposed person"],
        ["PRF", "Proposal Request Form, used for risks outside the standard packages"],
        ["Remittance", "Paying the collected premium, net of commission, to the insurer"],
        ["SLA", "Service level: the time allowed for a stage before an item is overdue"],
        ["SOA", "Statement of account"],
        ["TSU", "Technical Services Unit, which obtains terms from insurers for non-package risks"],
    ], [4.0, 12.6]),
]

CHAPTERS = [INTRODUCTION, GETTING_STARTED, FINDING_YOUR_WAY, CYCLE, CLIENT_POLICY, OPS_FINANCE, CLAIMS,
            REPORTS, ADMIN, SUPPORT, GLOSSARY]
