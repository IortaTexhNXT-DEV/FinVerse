import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Renewal screens (BRD-6). */
export const RENEWAL_HELP: HelpSection = {
  id: 'renewal',
  module: 'Renewal',
  intro:
    'Renewal takes every expiring account from extraction to the renewed invoice: the checks and Classification, the disposition by the Account Officer, the Team Leader review, processing with the insurer, the Renewal Advice to the client, the acceptance and the booking of the renewal. Renewal reports are in the Report Centre under Renewal.',
  screens: [
    {
      name: 'Renewal Home',
      path: '/renewal',
      summary:
        'Renewals due in 30, 60, 90 and 140 days, those that need attention, urgent, returned, with no response, insurer replies overdue and failed letters; the renewals by status and Classification and the workload of the officers.',
      workflow: [
        'Select a tile or a status to open the list of those renewals.',
        'The counts cover the renewals of your scope: your unit, or all units for the Renewal team.',
      ],
      controls: [
        'A renewal needs attention when it is not accepted within the escalation days before its expiry (ageing), past its effective expiry (overdue) or in the Exception bucket or with claims (high risk); the rule is shown with the flag.',
        'No alert is sent to the unit head: the Account Officer escalates outside the system.',
      ],
    },
    {
      name: 'Renewal Dashboard',
      path: '/renewal/dashboard',
      summary:
        'Production against the Annual Renewal Budget, the pipeline by business type and stage, the outstanding renewal accounts by ageing, the closing ratio, the biggest open deals, the renewal persistency, the product mix, the insurer approvals and the KPI cards of your scope.',
      workflow: [
        'Choose the period, the market segment and the Account Officer: every figure follows the filters.',
        'Click a figure or a card to open the accounts it counts; Export and Print opens the same list in the Report Centre.',
      ],
      controls: [
        'The figures and the lists use the same accounts, so a list holds exactly the accounts of its figure.',
        'Percentages are shown with two decimals; a percentage of a zero base is shown as a dash.',
      ],
    },
    {
      name: 'Renewal Accounts',
      path: '/renewal/accounts',
      summary:
        'The renewal accounts of the current and previous years in four panels: Clean, Review, Non-Renewable (or Exception, by the setting) and All, with the same columns in every panel.',
      workflow: [
        'Scroll the list: further rows load as you scroll; the number of renewal accounts is shown above.',
        'Sort by a column header and search across the reference, invoice, policy, PN and client.',
        'Select a row to open the renewal account.',
      ],
      controls: [
        'A renewal account with an open claim is never in the Clean panel.',
        'An account locked after posting and placement shows Locked after placement.',
      ],
    },
    {
      name: 'Processing Dashboard',
      path: '/renewal/processing-dashboard',
      summary:
        'One dashboard of New Business and renewal processing: For Placement, For Booking, With and Without Policy, For Releasing, Released, Unreleased, Directly Booked and Returned, in the Combined, New Business or Renewal view.',
      workflow: [
        'Click a card to list its accounts; For Placement has the Unassigned and Assigned tabs and the assignment of the Placement Processor.',
        'Export and Print opens the list in the Report Centre.',
      ],
      controls: [
        'Ageing counts business days; a placement sent after the cut-off hour counts from the next business day.',
      ],
    },
    {
      name: 'Audit Logs',
      path: '/renewal/audit-logs',
      summary:
        'Every recorded activity on the renewal accounts: the actions, the status changes, the dispositions and the classifications, with the old and new values, the user and the time.',
      workflow: [
        'Filter by date range, action type, reference number and user.',
        'Export the entries of the filters as CSV or Excel.',
      ],
      controls: ['The entries cannot be changed or deleted.'],
    },
    {
      name: 'Renewal Annual Budget',
      path: '/renewal/budget',
      summary:
        'The new, renewal and organic budgets of a fiscal year by market segment and hierarchy, with the annual totals, the history of every change and the budget upload.',
      workflow: [
        'New Budget creates a record; Update Budget changes its monthly amounts; View Budget History lists the previous and updated values.',
        'Upload Budget loads a file of the template, one row per record and month.',
      ],
      controls: [
        'Fiscal year and market segment are required; the region for the segments other than Corporate, the team and sub-team for Corporate; amounts cannot be negative.',
      ],
    },
    {
      name: 'Expiry List',
      path: '/renewal/expiry',
      summary:
        'Expiring accounts by tab: extracted, unassigned, by disposition, exceptions, transfers pending and all, with search, the Filters panel and the Download of the list.',
      workflow: [
        'Generate Expiry List reads the accounts expiring in a range; the daily run does the same within the renewal horizon.',
        'Select extracted renewals and Initiate them: the checks run and each renewal gets its Classification (Clean, Review or Exception).',
        'Select renewals and Assign Disposition to an Account Officer; Transfer moves one to another unit.',
        'In the Filters panel, each criterion accepts several values or "all except" the values chosen.',
      ],
      controls: [
        'An account is extracted once per term: a second extraction does not duplicate it.',
        'A failed check never gives the Clean Classification.',
        'Accounts expiring within the urgent window are flagged Urgent.',
      ],
    },
    {
      name: 'My Dispositions',
      path: '/renewal/mine',
      summary:
        'The renewals assigned to you, with quick filters for the renewals to disposition, returned to you, due in 30 days and with no response.',
      workflow: [
        'Open a renewal, view its Account History, then Set Disposition: For Renewal, For Quotation, For Proposal, Not for Renewal (with a reason) or Lost Business.',
        'Select the dispositioned renewals and Push them to your Team Leader.',
      ],
      controls: [
        'The disposition is refused until you have opened the Account History of the renewal.',
        'Every disposition is kept in the History tab; a new one supersedes the previous one.',
      ],
    },
    {
      name: 'TL Review',
      path: '/renewal/review',
      summary:
        'Dispositions of your unit waiting for review, the exceptions, unassigned renewals and renewals on the quotation or proposal path.',
      workflow: [
        'Post the reviewed renewals to Processing; renewals not for renewal go to the closing letters.',
        'Return a renewal to the officer with a reason and remarks.',
        'Override a failed check, the Classification, the disposition or the outstanding balance with a reason and remarks.',
      ],
      controls: [
        'A renewal for renewal with a blocking check is not posted until the check passes or is overridden.',
        'Straight-through renewals (automatic disposition of a Clean renewal) move on without review.',
      ],
    },
    {
      name: 'Transfers',
      path: '/renewal/transfers',
      summary: 'Requests to move a renewal to another Marketing unit, incoming and outgoing.',
      workflow: [
        'Accept or decline an incoming request with remarks; the requesting unit may cancel an open request.',
      ],
      controls: ['A renewal has one open transfer request at a time.'],
    },
    {
      name: 'Channel Monitor',
      path: '/renewal/channels',
      summary:
        'Documents sent to clients through CCM and to insurers through MFT, with their delivery status and the connection of each channel.',
      workflow: [
        'Filter by channel, delivery status or reference number; open the delivery history of a message.',
        'Resend a failed or delivered document: a new transmission of the same stored document. Cancel a message not yet transmitted.',
        'Download the error report of the failed messages to correct the recipients and resend.',
      ],
      controls: [
        'Pending Transmission, Submitted to CCM, Sent, Delivered, Failed and Cancelled are kept with their time.',
        'Each channel runs on its live interface or on the simulator of the test environments, by its setting.',
      ],
    },
    {
      name: 'Submitted for Approval',
      path: '/renewal/approvals',
      summary:
        'Accepted renewal accounts reviewed before their placement, with the For Booking Only and Direct-to-Insurer Payment tags.',
      workflow: [
        'Approve the selected accounts: a CBG Motor account proceeds to placement once its payment is posted; a Non-CBG account needs the client payment confirmation.',
        'Return an account for correction with the reason; the Account Officer corrects it and resubmits it from the Acceptance & Approval tab.',
        'Submit for Placement sends an account For Booking Only once its policy number, OR number, policy document and full payment are in, or its override is approved.',
      ],
      controls: [
        'Whether an accepted account waits for this approval is a setting; without it the account goes to the payment gate at once.',
        'A Direct-to-Insurer Payment routed to the Unit Head must be approved first; a rejected one must be removed before the account proceeds.',
      ],
    },
    {
      name: 'TSU Requests',
      path: '/renewal/tsu-requests',
      summary:
        'Quotation requests to TSU of the renewal accounts For Quotation, from the Team Lead approval to the proposal completed by TSU.',
      workflow: [
        'The Account Officer creates the request from the Proposal & TSU tab of the account; it is Pending Team Lead Approval and the Team Lead is notified.',
        'The Team Lead approves, rejects or returns it for revision, then assigns the TSU Officer (For TSU Processing).',
        'The TSU Officer records the insurer quotations of the comparative table and completes the request with the proposal; Marketing selects the insurers and sends the proposal.',
      ],
      controls: [
        'An account has one open TSU request at a time; every change is notified to the requester.',
      ],
    },
    {
      name: 'KYC Monitoring',
      path: '/renewal/kyc',
      summary:
        'Renewal accounts whose client is due for KYC review, with upcoming reviews, completed reviews and pending follow-ups.',
      workflow: [
        'Open a count to list its accounts; open an account to record the KYC review activity, remarks, follow-up notes or completion on its KYC tab.',
        'Refresh KYC Status applies the KYC criteria now; the daily job does it every night.',
      ],
      controls: [
        'The criteria are settings: review date window, upcoming days, market segments, bank or non-bank clients and products.',
        'Every status change is kept in the history with the user and the time; the classification of the account does not change.',
      ],
    },
    {
      name: 'Risk Code Maintenance',
      path: '/renewal/risk-codes',
      summary:
        'Risk codes with their Renewable or Non-Renewable indicator used at the generation of the renewal accounts.',
      workflow: [
        'Search a risk code or filter by indicator; the list shows who created and last updated each entry.',
        'Add or update a risk code with its description, product line, indicator, effective date and remarks; another user authorizes the change in Renewal Setup.',
      ],
      controls: [
        'A risk code already in the list is refused; the effective date is today or later.',
        'Accounts on a Non-Renewable risk code are Not for Renewal at their generation, with the reason recorded.',
      ],
    },
    {
      name: 'CLPC Billing Files',
      path: '/renewal/billing',
      summary:
        'Billing files of the CBG Home and FFY Motor accounts For Billing Generation, sent to the nominated recipients and to the shared LMS directory.',
      workflow: [
        'The CBG Home job runs daily for the accounts the set days before expiry; the FFY Motor job runs on the set days of the month.',
        'Generate Billing File for a period of at most 31 days ending today or earlier.',
        'Download a file, or download it under another name; the attached file keeps its name.',
      ],
      controls: [
        'Amortized accounts go in the Built-in file, the others in the Non-Built-in file, one pair per product line.',
        'An account is billed once; the delivery status shows Delivered or Failed with the reason.',
      ],
    },
    {
      name: 'Submitted for Placement',
      path: '/renewal/placement',
      summary:
        'Renewal accounts posted and submitted for placement and booking: placement slips and placement files, their sending to the insurers, the Insurance Advice and the return to Marketing.',
      workflow: [
        'Select one or more accounts and Generate Placement: one placement slip per insurer of each account and, for packaged accounts, one placement file per insurer; accounts tagged For Booking Only are left out.',
        'Send Placement: insurers enrolled in MFT receive the files in their MFT location; the others through CCM to the recipients of Insurer Maintenance, which can be changed, with copy recipients. A sent account is For Booking.',
        'On the Placement tab of the account, follow the turnaround time and SLA, tag the placement With Issue with its resolution date, record the insurer response or cancel a rejected placement.',
        'Send Insurance Advice sends the latest advice of the selected mortgaged accounts through CCM; Return sends an account Submitted for Placement or Rejected Placement back to Marketing with a reason.',
      ],
      controls: [
        'Turnaround time in business days from the placement submission (after the cut-off hour, from the next business day): 3 days for packaged accounts and 10 for non-package accounts by default.',
        'Whether the insurer approval books the account is a setting; the Insurance Advice gets a new version when the placement changes.',
      ],
    },
    {
      name: 'E-Policies',
      path: '/renewal/epolicies',
      summary:
        'E-policy files from the insurers (summary file and ZIP file, by upload or MFT), the sending of the e-policies to the clients through CCM and the upload of e-policy numbers.',
      workflow: [
        'Upload the E-Policy Summary File and the E-Policy ZIP File; open a receipt to see each record matched or unmatched.',
        'On For E-Policy Sending, select one, several or all eligible accounts, add copy recipients and send; the summary shows the accounts submitted, failed and pending.',
        'Upload E-Policy Numbers with the template: reference number and policy number.',
      ],
      controls: [
        'Both files are required and every document of the ZIP file must be in the summary file.',
        'An e-policy number already held by another active account is refused into the exception report.',
      ],
    },
    {
      name: 'RMU Account Maintenance',
      path: '/renewal/rmu-officers',
      summary: 'Account Officer codes of the Remedial Management Unit used by the LAMD upload.',
      workflow: [
        'Add an AO code with the officer name; update the name or remarks, or deactivate a code that no longer applies.',
        'A loan of the CBG loans list whose Account Officer is in the list has the loan status RMU and routes its renewal account to RMU.',
      ],
      controls: ['An active AO code is listed once; every change is in the audit log.'],
    },
    {
      name: 'Transfer Requests',
      path: '/renewal/referrals',
      summary:
        'Transfer Request Monitoring: requests to another Marketing unit for a New Business opportunity, with their status and the New Business account.',
      workflow: [
        'Transfer to Other Unit on a renewal account sends the request with its justification (Pending Acceptance) or saves it as a Draft.',
        'The receiving unit accepts, rejects with remarks or returns the request for clarification; the requester clarifies and submits it again.',
        'Once accepted, Create New Business Account copies the data of the renewal account for the receiving unit to review and save.',
      ],
      controls: [
        'The renewal account keeps its unit and Account Officer.',
        'The receiving unit must be another unit; every request, decision and New Business account is in the audit log.',
      ],
    },
    {
      name: 'Processing Worklist',
      path: '/renewal/processing',
      summary:
        'Posted renewals by tab: for processing, in processing, with the insurer, insurer responded and returned.',
      workflow: [
        'Assign a Processing Officer, or Assign to Me.',
        'Open a renewal to create the renewal account; it carries the terms of the expiring account and the documents of the term.',
        'Return to Marketing sends a renewal back to the officer or the Team Leader with a reason.',
        'Upload Dispositions sets the dispositions of many renewals from the file of Marketing; declare the file complete to tag the renewals missing from it.',
      ],
      controls: ['The renewal account is created once and linked to the expiring account.'],
    },
    {
      name: 'Insurer Batches',
      path: '/renewal/insurer',
      summary: 'The renewals sent to each insurer for its renewal terms, and the replies.',
      workflow: [
        'New Batch takes the renewals of an insurer for an expiry range with the 28 columns of the extract; Download it or Send to Insurer.',
        'Upload Insurer Responses records the replies; one reply can also be recorded on the renewal.',
      ],
      controls: [
        'The extract is sent protected.',
        'Replies are kept; only the latest valid reply drives the renewal. Late, mismatched or conflicting replies do not.',
      ],
    },
    {
      name: 'Letters',
      path: '/renewal/letters',
      summary:
        'Renewal Advices ready, generated and sent, the No Advice and Not for Renewal letters and the renewals with no response.',
      workflow: [
        'Generate RA for the renewals with their terms (first or second notice), then Send.',
        'Send Letters generates and sends the No Advice and Not for Renewal letters and closes the renewals.',
        'Unrenewed – NAL and Unrenewed – NRL list the renewals that reached their effective expiry date without renewal: Operations sends the No Advice Letter where a Renewal Advice was sent, the Account Officer the Non-Renewal Letter otherwise.',
        'Upload Acceptances records client acceptances from a file.',
      ],
      controls: [
        'The effective expiry date is the end of a confirmed hold cover, otherwise the policy expiry date; no closing letter is sent before it.',
        'A renewal never receives both a No Advice Letter and a Non-Renewal Letter.',
        'An RA generated after the minimum notice before expiry needs your confirmation, which is recorded.',
        'Letters are generated from versioned templates and kept in the documents of the renewal.',
        'Renewal Advices sent by hand before go-live are never sent again.',
      ],
    },
    {
      name: 'Follow-ups',
      path: '/renewal/followups',
      summary: 'Renewals waiting for the reply of the client after the Renewal Advice.',
      workflow: [
        'Open a renewal to record a call or e-mail with its outcome and next action date, or the acceptance of the client.',
      ],
      controls: ['Renewals with no response are flagged NRNS and a reminder letter is sent.'],
    },
    {
      name: 'LAMD Reports',
      path: '/renewal/lamd',
      summary:
        'The monthly paid-off and past-due loan reports of the Loan and Mortgage department.',
      workflow: ['Upload the report of a month; each line is matched to the renewals by PN.'],
      controls: [
        'A paid-off loan proposes Not for Renewal; a past-due loan routes the renewal to the Team Leader.',
        'A report of the same type and month is not loaded twice.',
      ],
    },
    {
      name: 'Renewal Setup',
      path: '/renewal/setup',
      summary:
        'Non-renewable risk codes, the checks, the insurer renewable lists, the Classification rules, the decision matrix, the package map of migrated policies, package choices to approve and the go-live take-over.',
      workflow: [
        'Risk codes, checks, insurer renewable lists and package mappings are saved for authorization by a second user.',
        'A renewal above the total sum insured threshold of the checks (250 million in the base currency by default) is held for review and proposed For Proposal; a risk code outside the catalogue or outside the renewable list of its insurer is held for review.',
        'Classification rules and the decision matrix are versioned: a new draft is submitted, then activated or rejected by a second user; activation retires the active version.',
        'Approve or reject the package proposed for a migrated policy.',
      ],
      controls: [
        'You cannot authorize your own change.',
        'Risk codes are end-dated, never deleted.',
        'A new version of the classification rules applies to the renewals not yet initiated; renewals in progress keep the version that classified them.',
      ],
    },
  ],
};
