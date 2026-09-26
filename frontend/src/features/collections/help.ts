import type { HelpSection } from '@/features/help/helpContent';
import { BILLING_HELP } from './billing/helpEntries';
import { ESCALATION_HELP } from './escalations/helpEntries';
import { PLAN_HELP } from './plans/helpEntries';
import { UNAPPLIED_HELP } from './unapplied/helpEntries';

/** In-app help of the Collections screens (BRCLXN.001-060), in sidebar order. */
export const COLLECTIONS_HELP: HelpSection = {
  id: 'collections',
  module: 'Collections',
  intro:
    'Collections follows up the premium receivables of the booked invoices: the worklist per invoice, assignment, collection efforts, dispositions, promises to pay, installments, escalations, billing statements and the collector side of unapplied payments. It reads the Operations invoice ledger and posts no accounting entry: payments, 2307 and direct payment reversals are executed by Cashiering and Commission.',
  screens: [
    {
      name: 'Collections Home',
      path: '/collections',
      summary:
        'Your collection work as tiles - your open accounts, unassigned accounts, direct payments returned by insurers, credit balances, files ready and the tiles of the other Collections screens - and the open amounts per aging bracket, for all segments or one.',
      controls: [
        'An invoice enters the worklist when its net outstanding premium is above CLX_MIN_BALANCE_THRESHOLD (BRCLXN.005).',
        'Aging counts from the booking or inception date (CLX_AGING_BASIS) in the brackets of CLX_AGING_BRACKETS.',
      ],
    },
    {
      name: 'PR Worklist',
      path: '/collections/worklist',
      summary:
        'One row per invoice with outstanding premium receivable, refreshed nightly from the invoice ledger (CLX_DAILY_REFRESH) and after each payment. Filter by segment, unit, Unit Head, handler, AO, aging bracket, category, disposition, amount, promise and escalation; see the totals by client or account; select accounts to log an effort, record a disposition or reassign them. Open an account for its details.',
      workflow: [
        'The nightly refresh lists new invoices, updates balances and aging, completes paid accounts and reopens them when the balance comes back (BRCLXN.001-015, 022).',
        'New accounts are assigned by the assignment rules, else to their account officer (BRCLXN.052).',
        'A disposition with an Operations action is handed over in the application: check pick-up and BIR 2307 tags to Cashiering, direct payment accounts to Commission.',
      ],
      controls: [
        'Several accounts at once need CLX_BULK_UPDATE and share one bulk reference (BRCLXN.051).',
        'Only active dispositions can be chosen; some are reserved to roles (e.g. "no policy number" to the Processing Unit).',
        'Negative balances of cancellations are never listed; other credits show on the Credit Balances tab (BRCLXN.010, CQ04).',
        'Export to Files produces the Outstanding PR List in the background, capped at CLX_EXPORT_MAX_ROWS, under CLX_EXPORT.',
      ],
    },
    ...PLAN_HELP,
    ...ESCALATION_HELP,
    ...BILLING_HELP,
    ...UNAPPLIED_HELP,
    {
      name: 'Assignments',
      path: '/collections/assignments',
      summary:
        'The default assignment rules (segment, sales unit, client, amount, aging) tried in priority order for new accounts, and the reassignment of accounts by criteria after a preview - permanent, or temporary until an end date after which the account returns to its handler.',
      controls: [
        'Needs CLX_ASSIGN; the new handler must hold CLX_WORK.',
        'Every reassignment keeps its history on the account and notifies the handlers (CLX_REASSIGNED).',
      ],
    },
    {
      name: 'Collections Files',
      path: '/collections/files',
      summary:
        'The daily Outstanding PR List and Full Production Report, the weekly DP PR / PR 2307 for reversal files per unit and branch (Saturday to Friday, available Monday 08:00), the monthly ones of the previous month (first working day) and your exports.',
      controls: [
        'A file cannot be downloaded before its availability time; downloads need CLX_EXPORT.',
        'A file that fails raises CLX_FILE_NOT_PUBLISHED; set-up users can generate the files of a date again.',
      ],
    },
    {
      name: 'Collections Setup',
      path: '/collections/setup',
      summary:
        'The Collections parameters (threshold, aging basis and brackets, export cap, edit-lock time), the rules of each disposition (category, tagging owner, Operations action, allowed roles, unapplied-payment attributes), the Unit Head of each sales unit and a refresh of the worklist on demand.',
      controls: [
        'Needs CLX_SETUP; every change is kept in the Collections audit log with the old and new value (BRCLXN.043).',
        'Disposition values themselves are added and deactivated on the LOV screen with maker-checker (BRCLXN.017).',
      ],
    },
  ],
};
