import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Assets & Investments screens (listed in the help centre). */
export const ASSETS_HELP: HelpSection = {
  id: 'assets',
  module: 'Assets & Investments',
  intro:
    'Property and equipment with monthly depreciation, and the investment portfolio under PFRS 9 with interest accrual, amortization, coupons, maturities, sales and fair value. Every posting goes through the accounting engine.',
  screens: [
    {
      name: 'Asset Categories',
      path: '/assets/categories',
      summary:
        'Classes of property and equipment with their GL accounts (asset, accumulated depreciation, depreciation expense) and default depreciation policy: method, useful life and residual value.',
      controls: [
        'Maker-checker: a new or changed category must be authorized by another user (MASTER_AUTHORIZE) before assets use it.',
      ],
    },
    {
      name: 'Asset Register',
      path: '/assets/register',
      summary:
        'Property and equipment with cost, accumulated depreciation and net book value. Register, capitalize, dispose of or transfer assets.',
      workflow: [
        'The maker registers the asset; a different user capitalizes it, which posts ASSET_ACQUISITION on the capitalization date.',
        'Assets acquired before go-live are registered as take-on: their opening accumulated depreciation is posted against retained earnings (ASSET_TAKE_ON).',
        'Dispose of an asset: gain or loss = proceeds − net book value (ASSET_DISPOSAL).',
        'Transfer to another branch: two balanced journals through inter-branch clearing; the asset keeps depreciating at the receiving branch.',
      ],
      controls: [
        'Depreciation must be run up to the month before the disposal month.',
        'Disposals and transfers need ASSET_MANAGE.',
      ],
    },
    {
      name: 'Depreciation Run',
      path: '/assets/depreciation',
      summary:
        'Monthly depreciation of every capitalized asset up to the period end, with one journal per branch, category and cost centre dated at the period end.',
      workflow: [
        'Preview the period, check the charges, then post.',
        'Months missed because of late capitalization are caught up automatically.',
      ],
      controls: [
        'A period is posted only once (posting again returns the existing run); needs PERIOD_END_RUN.',
        'Full-month convention: a full month in the month of acquisition, none in the month of disposal; never below the residual value.',
      ],
    },
    {
      name: 'Investment Portfolios',
      path: '/investments/portfolios',
      summary:
        'Each portfolio fixes the PFRS 9 classification (amortized cost, FVOCI or FVPL) and the GL accounts its holdings post to.',
      controls: [
        'FVOCI changes go to the equity reserve, FVPL changes to profit or loss.',
        'Maker-checker: portfolios are authorized by another user before use.',
      ],
    },
    {
      name: 'Investments',
      path: '/investments/holdings',
      summary:
        'Time deposits, treasury bills, bonds and equities with face value, cost, coupon terms, day count and maturity.',
      workflow: [
        'The maker records the holding; a checker approves it, which posts the purchase (clean price plus purchased accrued interest, INVESTMENT_PURCHASE).',
        'Holdings bought before go-live are taken on at their carrying amount against retained earnings (INVESTMENT_TAKE_ON).',
      ],
      controls: [
        'The approver must differ from the maker (MASTER_AUTHORIZE); the approval appears in My Approvals.',
      ],
    },
    {
      name: 'Accrual & Amortization',
      path: '/investments/runs',
      summary:
        'Month-end investment income: interest accrual (face × coupon rate × days / year days) and premium or discount amortization (effective interest or straight line), one journal per holding.',
      workflow: ['Choose the run type and period, preview, then post.'],
      controls: [
        'Each run type is posted once per period; needs PERIOD_END_RUN.',
        'Day count ACT/365 or 30E/360; accruing month by month equals the coupon of the whole period.',
      ],
    },
    {
      name: 'Maturities & Sales',
      path: '/investments/maturities',
      summary:
        'Held investments ordered by maturity. Record coupons received (with final tax), redemptions at maturity, sales with the realized gain or loss, and fair value updates of FVOCI and FVPL holdings.',
      controls: [
        'Interest is first accrued up to the event date, then the accrued interest is relieved.',
        'Realized gain = proceeds + final tax + FVOCI reserve recycled − carrying amount − accrued interest.',
        'Coupons, maturities, sales and fair value need INVESTMENT_MANAGE.',
      ],
    },
  ],
};
