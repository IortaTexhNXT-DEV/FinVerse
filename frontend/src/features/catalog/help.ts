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
        'Product lines, cover types and products with their features (package, fleet, mortgage, direct payment, multi-year, Free First Year, market segments), plus the field, document and TSU routing rules.',
      workflow: [
        'Filter by line, package, market segment or text; open a product to see its field matrix and required documents.',
        'New product or Edit: set the features, default rate and commission, minimum premium and, for packages, the TSI limit.',
        'Field rules: mandatory data for all products, a line or one product; "a|b" means one of the fields is enough.',
        'Document rules: documents to attach before an account can be submitted.',
        'TSU routing: criteria (non-package, fleet size, number of locations, sum insured) that send an account to Technical Services; the first rule by priority applies.',
      ],
      controls: [
        'Every change waits for authorization by another user (maker-checker) and is audited.',
        'A package whose total sum insured exceeds its TSI limit always goes to TSU.',
        'Deactivated products can no longer be used on new accounts.',
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
