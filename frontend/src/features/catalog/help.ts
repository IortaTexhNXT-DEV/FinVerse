import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Product Maintenance screens. */
export const CATALOG_HELP: HelpSection = {
  id: 'catalog',
  module: 'Product Maintenance',
  intro:
    'What BDOI sells and with whom: products and packages with their rules, the insurer panel, the rates and taxes used by rating, and the sales organisation.',
  screens: [
    {
      name: 'Products',
      path: '/catalog/products',
      summary:
        'Product lines, cover types and products with their features, lifecycle and current package version, plus the field, document and TSU routing rules.',
      workflow: [
        'Filter by line, package, market segment or text; open a product to see its versions, features, field matrix and required documents.',
        'Package versions: New Version copies the version in force into a draft that MBS edits (rate scheme, dates, coverages, insurers and insurer terms) and submits for validation.',
        'New product or Edit: set the features, default rate and commission, minimum premium and, for packages, the TSI limit.',
        'Field rules: mandatory data for all products, a line or one product; "a|b" means one of the fields is enough.',
        'Document rules: documents to attach before an account can be submitted.',
        'TSU routing: criteria (non-package, fleet size, number of locations, sum insured) that send an account to Technical Services; the first rule by priority applies.',
      ],
      controls: [
        'Every change waits for authorization by another user (maker-checker) and is audited.',
        'A package whose total sum insured exceeds its TSI limit always goes to TSU.',
        'Deactivated products can no longer be used on new accounts.',
        'New business is always priced on the current released version; an older version or another item rate needs an approved rate exception.',
        'The rate, minimum premium, commission and TSI limit of a package change only through a new version; expired and retired packages stay searchable for PRODUCT_ARCHIVE_VIEW holders.',
      ],
    },
    {
      name: 'Validation Queue',
      path: '/catalog/validation',
      summary:
        'Package versions set up by MBS that wait for the post-set-up validation (PMADD06) before new business can use them, oldest first with their age.',
      workflow: [
        'Open a version to review its rate scheme, dates, coverages, insurers and insurer terms.',
        'Confirm every checklist item, then Validate and Release: the version sells from its effective date and the previous version ends the day before.',
        'Return to MBS with a reason when something is wrong; MBS corrects the draft and submits it again.',
      ],
      controls: [
        'Only PRODUCT_VALIDATE holders (TSU Head, Business Administrator) validate, never the user who set up or submitted the version.',
        'A test premium is computed on release and kept with the checklist; every validation and return is audited.',
        'Package request stages follow the release or return automatically.',
      ],
    },
    {
      name: 'Coverages & Clauses',
      path: '/catalog/coverages',
      summary:
        'The coverage / peril level of the product hierarchy per line (basic coverages, extensions) and the clause library of warranties, clauses, exclusions and deductible wordings (PMADD01/02).',
      workflow: [
        'New Coverage: choose the line, code, kind and whether it is a basic coverage.',
        'New Clause: code, kind, line (or every line), title, wording and effective dates.',
        'Package versions list their coverages and, per insurer and coverage, the clauses that apply.',
      ],
      controls: [
        'Coverages and clauses are authorized by another user (PRODUCT_AUTHORIZE) before a version can use them.',
        'A package version cannot be submitted without an included basic coverage.',
      ],
    },
    {
      name: 'Incentive Criteria',
      path: '/catalog/incentives',
      summary:
        'Incentive criteria such as CPC2 on the maintained products matrix: type, value or rule, products (with an optional segment) and effective dates (PMADD07/08).',
      workflow: [
        'New Criterion: only active products of the matrix can be selected; the value is a rate, an amount or a rule with parameters.',
        'Open an active criterion to amend it: the amendment starts later and ends the current row the day before once authorized.',
        'Deactivate a criterion to stop it; it stays in the History tab.',
      ],
      controls: [
        'Every criterion and amendment is authorized by another user (PRODUCT_AUTHORIZE) and audited.',
        'Overlapping periods of the same code are refused.',
        'Booking stamps the codes of the criteria an invoice matches; criteria of a package that expires or is retired raise an alert for review.',
      ],
    },
    {
      name: 'Insurers',
      path: '/catalog/insurers',
      summary:
        'The insurer panel: accreditation, placement channel and e-mails, credit days, branches with their local government tax (LGT) rate, and commission rates by product.',
      workflow: [
        'New insurer creates the insurer and its business partner (party type Insurer) in one step.',
        'Open an insurer to add branches and commission rates; a rate without a product applies to all its products.',
      ],
      controls: [
        'Insurer, branches and rates are authorized by another user before use.',
        'Commission rates are effective-dated: the rate in force on the period start applies; otherwise the product default.',
        'Only e-mail placement is available; SFTP and API channels are parked.',
      ],
    },
    {
      name: 'Rates & Taxes',
      path: '/catalog/rates',
      summary:
        'Documentary stamp tax, premium tax, VAT, fire service tax, VAT on commission, motor own damage factors, the short-period table and the BI / PD limit premiums.',
      workflow: [
        'A rate without a line applies to every line; a line rate overrides it.',
        'To change a rate, add a new row effective from the change date.',
      ],
      controls: [
        'DST is rounded to centavos, then up to the next 0.50.',
        'Rows are effective-dated and authorized by another user before rating uses them.',
      ],
    },
    {
      name: 'Sales Organisation',
      path: '/catalog/sales-organisation',
      summary:
        'Regions, departments and teams with their cost centers, and the account officers of each team.',
      workflow: [
        'New unit: choose the level and the parent; leave the cost center blank to inherit the parent one.',
        'Assign officer: link a user to a team; new accounts are stamped with the team, department, region and cost center.',
      ],
      controls: ['Units and assignments are authorized by another user.'],
    },
    {
      name: 'Premium Calculator',
      path: '/catalog/calculator',
      summary:
        'Rates a product with the charges in force: annual, pro-rata (days) or short period (table), for a new account or an endorsement over the remaining term.',
      workflow: [
        'Choose the product, the insurer and branch (commission and LGT), the basis and period, then the items.',
        'A package is priced on its current version ("Priced on version n"); pick an earlier version to see its premium for information.',
        'Motor products take the sum insured and BI / PD limits; other products take the sum insured and an optional rate.',
        'Tick Endorsement to rate an additional or reduced sum insured over the remaining term; a reduction gives a return premium.',
      ],
      controls: [
        'The minimum premium does not apply to endorsements or pro-rata periods.',
        'The calculator saves nothing; the account premium is re-rated on the server when the account is saved.',
      ],
    },
  ],
};
