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
