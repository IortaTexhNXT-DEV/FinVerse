import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Actuarial Reserves screens (listed in the help centre). */
export const RESERVES_HELP: HelpSection = {
  id: 'reserves',
  module: 'Actuarial Reserves',
  intro:
    'Monthly technical provisions: UPR, DAC / UCR, OSLR, IBNR, ULAE, margin for adverse deviation, premium deficiency and the takaful surplus, posted as movement journals.',
  screens: [
    {
      name: 'Reserve Summary',
      path: '/reserves/summary',
      summary:
        'Gross, reinsurers share and net of every reserve per line of business for the valuation month, compared with the previous posted valuation.',
    },
    {
      name: 'Valuation Runs',
      path: '/reserves/runs',
      summary:
        'Prepare the valuation of a month (preview), drill down to policy-level UPR and the journal movements, then submit, approve and post it.',
      workflow: [
        'Calculate a preview for the month; recalculate after late transactions or parameter changes.',
        'Submit for approval; a checker approves (or rejects back to preview) and posts.',
        'Posting books closing balance minus the previous posted run per branch and line of business.',
        'Cancel a posted run to reverse its journals (latest posted run only).',
      ],
      controls: [
        'The preparer cannot approve the run (maker-checker).',
        'One live run per month; posting is idempotent and runs are posted in date order.',
        'The period-end checklist requires the month to be posted.',
      ],
    },
    {
      name: 'IBNR Triangles',
      path: '/reserves/triangles',
      summary:
        'Paid or incurred claims development by accident year or quarter with chain-ladder factors, ultimates and IBNR = ultimate − incurred.',
    },
    {
      name: 'Reserve Parameters',
      path: '/reserves/parameters',
      summary:
        'IBNR method and rate, triangle options, MfAD %, ULAE %, expected loss ratio and reinsurance commission per line of business, effective dated; takaful settings.',
      controls: [
        'Every change is authorized by a second user before valuation runs use it.',
        'An authorized record is never changed: add a record with a later effective date.',
      ],
    },
  ],
};
