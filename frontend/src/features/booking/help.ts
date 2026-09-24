import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Booking screens. */
export const BOOKING_HELP: HelpSection = {
  id: 'booking',
  module: 'Booking',
  intro:
    'Booking turns an issued policy into a booked invoice: the GL entry, the client and insurer open items, the commission service invoice to the insurer, the cost center and the incentive flag, all in one step. Endorsements and cancellations follow on booked accounts.',
  screens: [
    {
      name: 'Booking Workbench',
      path: '/booking',
      summary:
        'Accounts ready to book (policy issued, or booked directly when the insurer had already issued it), the batch queue, booked accounts and failures, with tiles for each.',
      workflow: [
        'Open an account to see the pre-booking confirmation: the invoice (premium by component, commission, VAT and withholding tax, flags, policy years of a multi-year account) and the journal the rules will post; then Book Account.',
        'Select accounts and use Book Now to book them at once, or Add to Batch to queue them for the end-of-day batch.',
        'In Queued for Batch, edit the booking date or cost center of an account, remove it, confirm the selected accounts or cancel the whole batch.',
        'Accounts matching an auto-book rule are queued when their policy is issued; Upload Bookings books a list of ARNs.',
      ],
      controls: [
        'An account is booked once: a second booking of the same ARN and transaction is refused.',
        'The booking date cannot be in the future and its accounting period must be open; a failure leaves nothing booked.',
        'Each account of a batch is booked on its own: failures keep their reason in the Failed tab and in the batch run.',
        "The cost center is mandatory: the account officer's sales team gives the default.",
      ],
    },
    {
      name: 'Endorsements',
      path: '/booking/endorsements',
      summary:
        'Positive, negative and non-financial endorsements and cancellations of booked accounts, newest first.',
      workflow: [
        'Open a booked invoice and choose New Endorsement: enter the effective date and the change of the sum insured; the premium for the remaining term, the commission and the journal are computed as you type.',
        'Cancel Booking on the booked invoice cancels the policy: flat (from inception), flat retaining DST, or partial on the unexpired term (pro-rata or short period).',
      ],
      controls: [
        'Processing posts positive endorsements; Adjustment posts negative endorsements and cancellations.',
        'Return invoices reverse the booking entry and credit the commission service invoice; a non-financial endorsement posts nothing.',
      ],
    },
    {
      name: 'Service Invoices',
      path: '/booking/service-invoices',
      summary:
        'Commission service invoices to insurers and internal service invoices, with their credits and e-mail outcome.',
      workflow: [
        "Open a service invoice to download the PDF as issued, send it again to the insurer's billing e-mail, or credit it.",
      ],
      controls: [
        'Numbers follow a gap-free series per branch and year (SI-branch-year).',
        'The owner of the type is notified when the e-mail is sent or fails, with the reason.',
      ],
    },
    {
      name: 'Batch Runs',
      path: '/booking/batch-runs',
      summary:
        'Every booking batch - confirmed by a user, Book Now, the upload or the end-of-day job - with the result of each account.',
      workflow: ['Open a run to see which accounts were booked and why others failed.'],
      controls: [
        'The end-of-day BOOKING_BATCH job also books the later policy years of multi-year accounts when they start.',
      ],
    },
    {
      name: 'Booking Setup',
      path: '/booking/setup',
      summary:
        'Auto-book rules, incentive eligibility rules and service invoice types (recipient, trigger, owner and template).',
      workflow: ['Add or change a rule or type; click a row to edit it.'],
      controls: [
        'Only Business Administrators maintain the setup; every change is audited.',
        "Incentive rules are pending BDOI's qualification criteria (Q33).",
      ],
    },
  ],
};
