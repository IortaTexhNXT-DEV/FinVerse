import type { HelpSection } from '@/features/help/helpContent';

/**
 * In-app help of the Claims Handling screens (BRD-7, BRCLM.001-043), in sidebar order. Registered
 * by the foundation (CL0); owned by wave CL1-B, which completes the entries (CL1-A sends its own).
 */
export const BROKER_CLAIMS_HELP: HelpSection = {
  id: 'brokerclaims',
  module: 'Claims Handling',
  intro:
    "Claims Handling is BDOI's case file for a client's claim against its insurers: the cover it is made under, the loss, the locations, the insurers' claim numbers, reserve and settlement as the insurers report them, the status, the follow-up, the diary and the documents. BDOI records and coordinates; the insurer decides and pays. Claims posts no accounting entry and keeps no reserve of its own.",
  screens: [
    {
      name: 'Claims Home',
      path: '/claims-handling',
      summary:
        'Your claims at a glance: open claims, follow-ups due today and overdue, claims by status and phase, ageing buckets, claims on unpaid premium and claims waiting for premium remittance.',
      workflow: [
        'A claim moves through the phases New, In progress, Temporarily closed and Closed (workflow BCL_CLAIM). The 18 BDOI statuses each belong to one phase; a claim that has left New does not go back to a newly filed status.',
        'Each tile opens the worklist filtered: My Open Claims, Follow-ups Due Today, Follow-ups Overdue, My Diary Due, Temporarily Closed, Unpaid Premium and Awaiting Premium Remittance.',
        'Open Claims by Status and Outstanding Claims by Age (brackets of parameter BCL_AGEING_BUCKETS, default 0-30 / 31-60 / 61-90 / 91-180 / 181+ days) count the outstanding claims.',
        'Record Claim starts a new claim; Open Worklist lists every claim you may see.',
      ],
      controls: [
        'You see Claims Handling with BCL_VIEW; each action needs its own permission (BCL_RECORD, BCL_STATUS_UPDATE, BCL_CLOSE, BCL_REOPEN and the Team Lead rights).',
      ],
    },
    {
      name: 'Claims Worklist',
      path: '/claims-handling/worklist',
      summary:
        'Every claim you may see in the tabs My Claims, Open, Temporarily Closed, Closed and Follow-ups Due, searchable by claim number, insurer claim number, ARN, policy number or assured.',
      workflow: [
        'Ages are in calendar days: age overall from the reported date (to the closure date once closed), age this stage from the day the current status was set. Temporarily closed claims keep ageing.',
        'On the claim, Change Status lists only the statuses the status access matrix gives your role and claims unit; the change is kept in the History tab with the days spent in the previous status, and the next follow-up date is recomputed unless a Team Lead overrode it.',
        'Statuses 17 and 18 close a claim temporarily; any in-progress status resumes it. Set Settlement with a closing type closes it permanently (BCL_CLOSE); Reopen needs BCL_REOPEN and a reason.',
      ],
      controls: [
        'Reassigning claims to another handler needs WORK_ASSIGN (Team Lead, Team Head, Unit Head) and notifies the new handler (BCL_CLAIM_ASSIGNED); the History tab shows the old and new handler.',
        'Changing a status needs BCL_STATUS_UPDATE and a unit in the claims handler register; the settlement needs BCL_SETTLEMENT_UPDATE; the follow-up override and adjuster need the Team Lead rights; the action plan needs BCL_ACTION_PLAN.',
      ],
    },
    // Wave CL1-A entry (Record Claim and the claim record).
    {
      name: 'Record Claim',
      path: '/claims-handling/new',
      summary:
        'Find the cover by ARN, policy number or assured; the cover card shows the policy number, cover version at the loss date, period, sum insured, Marketing team, AO and branch, and the premium check with any unpaid invoices. Then pick the locations, record the loss and confirm the insurers proposed from the invoice shares.',
      workflow: [
        'Save gives the claim its number BCL-<yyyy>-nnnnnn, phase New and the first status, and puts it in your queue. The account officer is told in the app.',
        'A loss date outside the policy year asks for confirmation; an insurer claim number already on another claim asks for confirmation and raises BCL_INSURER_CLAIM_NO_REUSED.',
        'An insurer-reported claim needs the insurer claim number at recording.',
        'On the claim record: Details (loss, claimant), Locations (with the insurer location references), Insurers & Updates, Reserve & Settlement and Documents (loss advice and claims reports). Generate Authorization Code, Send Loss Advice, Refresh Cover Data and Use Latest Version are the page actions.',
      ],
      controls: [
        'Needs BCL_RECORD. Policy data comes from the account and is never re-keyed (BRCLM.007, 039).',
        'A claim on unpaid or partly paid premium is recorded, flagged Unpaid premium and raises BCL_UNPAID_PREMIUM_CLAIM; the authorization code (BCL_AUTHORIZE) is issued only when the premium is paid, or on a direct-payment cover under BCL_AUTH_DP_POLICY (CONFIRM: attach the insurer payment evidence first) (BRCLM.001).',
        'The reported date is corrected with BCL_STATUS_UPDATE and a reason, the claimant overridden with BCL_CLAIMANT_OVERRIDE, the reserve amended with BCL_RESERVE_AMEND and the adjuster of an insurer line set with BCL_ADJUSTER_ASSIGN; every change is kept in the claim history.',
        'The claim currency is the cover currency, else BCL_DEFAULT_CURRENCY.',
      ],
    },
    // Wave CL1-A entry.
    {
      name: 'Cover Lookup',
      path: '/claims-handling/covers',
      summary:
        'Read-only view of any cover of the company: account, policy years and policy numbers, items and locations, endorsements (cover versions), invoices with their payment and remittance status, the claims of the cover and the insurer location references (BRCLM.002, 003, 042).',
      workflow: ['Record Claim on the cover opens Record Claim with the cover selected.'],
      controls: [
        'Needs BCL_COVER_VIEW; nothing on the cover can be changed here. Search texts need at least 3 characters; there is no branch or portfolio restriction (CLQ27).',
        'Opening a cover is logged in the audit trail of the account.',
      ],
    },
    {
      name: 'My Diary',
      path: '/claims-handling/diary',
      summary:
        'Your calls, e-mails, meetings, notes and follow-ups across claims, with the entries due today and overdue (BRCLM.022).',
      workflow: [
        'Add Diary Entry on a claim records a call, e-mail, meeting, note or follow-up with an optional due date and assignee; an entry assigned to someone else notifies that user.',
        'The assignee or the author marks an entry done; entries are never deleted, and closed claims accept entries.',
      ],
      controls: [
        'The daily job BCL_FOLLOW_UP_DUE (06:00) reminds you of follow-ups and diary entries due today; past dates raise BCL_FOLLOW_UP_OVERDUE once per claim.',
      ],
    },
    // Wave CL1-A entry.
    {
      name: 'Insurer Location References',
      path: '/claims-handling/location-refs',
      summary:
        'The reference each insurer uses for a location of a cover, with its effective dates; maintained here or by the bulk upload BCL_LOCATION_REF (BRCLM.042). Every claim location shows the references valid today.',
      controls: [
        'Needs BCL_LOCATION_REF_MAINTAIN; a new reference ends the current one of the location and insurer the day before it starts, and both stay in the history. Insurer claim numbers and insurer updates also load by bulk upload (BCL_INSURER_CLAIM_NO, BCL_INSURER_UPDATE, BCL_RECORD).',
      ],
    },
    {
      name: 'Claims Reports',
      path: '/claims-handling/reports',
      summary:
        'The Claims Handling reports: outstanding and past due claims, settled claims, ageing overall and per status, loss experience, loss ratio, pending actions, claims-prone locations, insurer claim numbers, activity log and the data extract.',
      workflow: [
        'Outstanding lists cover the phases New, In progress and Temporarily closed on the as-of date (not in the future); the past due variant keeps the claims older than BCL_PAST_DUE_DAYS (default 90), which the daily job BCL_AGEING_ALERTS also flags.',
        'Loss experience: paid = settled amount, O/S = insurer reserve less paid while open (never below zero); loss ratio = losses over the signed gross premium of the cover and policy year x 100.',
        'Claims-prone locations: at least BCL_PRONE_MIN_CLAIMS claims in BCL_PRONE_YEARS years; enter a location key to list its claims.',
        'Files are named <Report>_<date of extraction>; save your parameters as a report variant.',
      ],
      controls: [
        'Running a report on screen needs BCL_REPORT_VIEW, downloading it (Excel, PDF, CSV) BCL_REPORT_EXPORT; the data extract needs BCL_DATA_EXTRACT.',
        'Marketing users see the reports only, never the claims themselves (BRCLM.040).',
      ],
    },
    {
      name: 'Claims Setup',
      path: '/claims-handling/setup',
      summary:
        'Status attributes (phase, waiting party, follow-up days, awaiting premium remittance), settlement type attributes (outcome, closes the claim, amount required), the status access matrix per role and unit, and the claims handler register.',
      workflow: [
        'Status Attributes: phase (New, In progress, Temporarily closed), the party the claim waits on, follow-up days (1-365; blank = BCL_FOLLOW_UP_DAYS) and Awaiting premium remittance. A new status is usable once it is authorized, has a phase and has a matrix row.',
        'Settlement Types: outcome (Settled or Closed without payment), whether the type closes the claim and whether amount and date are required.',
        'Status Access Matrix: the roles and claims units that may select each status (blank unit = any unit). Claims Handler Register: the unit and team of each claims user.',
        'Claims Lists: the values of every Claims list (statuses, settlement types, adjusters, catastrophe codes, units, diary types, reasons).',
      ],
      controls: [
        'Needs BCL_SETUP (Unit Head). Attribute changes, matrix rows and list values take effect only after another BCL_SETUP user (or a master data authorizer) authorizes them in My Approvals or on this screen.',
      ],
    },
  ],
};
