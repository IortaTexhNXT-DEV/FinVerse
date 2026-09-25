import type { HelpScreen } from '@/features/help/helpContent';

/**
 * Help entries of the Collections plans screens (BRCLXN.053/054/055), in sidebar order; added to
 * `COLLECTIONS_HELP.screens` by the Collections owner.
 */
export const PLAN_HELP: HelpScreen[] = [
  {
    name: 'Installment Plans',
    path: '/collections/plans',
    summary:
      'The billing cycles of multi-year and installment accounts. A policy-year plan bills every policy year of a multi-year account (booked or still scheduled) per cycle of the billing frequency; a generated plan splits the outstanding premium of one invoice in equal installments.',
    workflow: [
      'New Installment Plan: choose the basis, the account or invoice and the billing frequency.',
      'Open a plan to see each installment, generate the statement of account of a cycle and record a promise on an installment.',
      'Refresh Allocation applies the latest ledger payments at once; the nightly promise check does it for every live plan.',
    ],
    controls: [
      'Payments are allocated from the invoice ledger, oldest due installment first (BRCLXN.054).',
      'An installment past its due date and not fully paid is flagged Overdue and can escalate the account.',
      'One live plan per invoice or account; a plan changes neither the booking nor the GL (BRCLXN.060).',
      'Creating and cancelling plans needs CLX_BILLING.',
    ],
  },
  {
    name: 'Installments Due',
    path: '/collections/installments-due',
    summary:
      'Unpaid installments of the live plans falling due up to today, oldest first, with the overdue ones on their own tab.',
    controls: ['Open a row to go to its plan.'],
  },
  {
    name: 'Promises to Pay',
    path: '/collections/promises',
    summary:
      'Promises made by clients to pay an amount by a date. Each night the promises whose date (plus CLX_PROMISE_GRACE_DAYS) has passed are checked against the payments applied between the day of the promise and that date.',
    workflow: [
      'Record Promise: the invoice, the day of the promise, the promised date and the amount (empty = the whole outstanding).',
      'With the bulk update permission, enter several invoices to record the same promise on each.',
      'Select open promises and Withdraw Promise when the client withdraws them.',
    ],
    controls: [
      'Kept when the promised amount was paid in time or nothing is left to collect; partially kept when some of it was paid; otherwise broken (BRCLXN.055).',
      'A new promise on the same invoice replaces the running one.',
      'A broken promise notifies the collector and escalates the account through the broken-promise rules (BRCLXN.049).',
    ],
  },
];
