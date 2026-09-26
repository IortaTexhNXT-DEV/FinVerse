import type { HelpScreen, HelpSection } from '@/features/help/helpContent';

/**
 * In-app help of the package request screens (BRD-3, `productmaint`), in sidebar order: Product
 * Maintenance Home, Package Requests, TSU Workbench, Package Expiry.
 */
export const PACKAGE_REQUEST_HELP_SCREENS: readonly HelpScreen[] = [
  {
    name: 'Product Maintenance Home',
    path: '/product-maintenance',
    summary:
      'Package requests by stage with their SLA state, packages ending within 30, 60 and 90 days, versions waiting for validation, advisories pending and the comparative outputs of the week.',
    workflow: [
      'Each tile opens its list: a stage tile opens the Package Requests tab, an expiry tile the Package Expiry list.',
      'New Package Request starts the Package Request Form.',
    ],
    controls: [
      'Tiles flag requests past their SLA (red) or due within eight hours; the SLA hours are the PKG_SLA_* parameters.',
    ],
  },
  {
    name: 'Package Requests',
    path: '/product-maintenance/requests',
    summary:
      'Work list of package requests by stage (Drafts, For Approval, TSU Review, Negotiation, ManCom, With MBS, For Validation, Released, Closed). Search by request number, package, client or product.',
    workflow: [
      'Marketing or TSU drafts the request form: type (new, amend, update, renew, retire, reactivate), scope, client or programme, line, cover type, product, requested terms and target insurers.',
      'Marketing TL / TH / UH approves, the TSU Team Lead recommends and the TSU Head approves; the negotiation then runs in rounds with a quotation slip per round.',
      'After terms final, the TSU Head releases client-specific terms to Marketing (generic programmes go straight to the requirements); TSU submits the requirements pack, ManCom signs off, MBS sets the version up and the catalog validation releases it.',
      'On release an advisory is drafted; send it from the Advisories tab once the signed package slip and the ManCom sign-off are attached.',
    ],
    controls: [
      'Four eyes: an approver is never the maker or submitter, the TSU Head never the recommender, the quotation slip approver never its preparer.',
      'Insurer outcomes include the exception states (approved with changes, counter-proposal, declined, no response); every change is kept in the revision history.',
      'One current comparative master per request; client views are derived from it and never change a value.',
      'The slips are locked at the ManCom sign-off; an advisory cannot be sent without its supporting documents.',
    ],
  },
  {
    name: 'TSU Workbench',
    path: '/product-maintenance/tsu',
    summary:
      'Package request queues of the TSU Officer, Team Lead and Head, one tile per stage, oldest due first. Claim a request to work it; team leads assign requests.',
    workflow: [
      'Open a request from the queue to recommend, approve, prepare and send the quotation slip, key in the insurer terms or compile the requirements.',
    ],
    controls: ['Only users holding the stage permission see and act on its queue.'],
  },
  {
    name: 'Package Expiry',
    path: '/product-maintenance/expiry',
    summary:
      'Released packages by package end date with their renewal status (Expiring, Renewal in Progress, Expired). Generate Expiry List changes the look-ahead.',
    workflow: [
      'Select packages and use Generate Renewal Request: a RENEW request is drafted with the terms of the version in force and the next term dates.',
      'The daily PACKAGE_EXPIRY_MONITOR job alerts TSU and MBS at the notice period and again at 30 and 7 days, and drafts renewal requests itself when PACKAGE_RENEWAL_AUTODRAFT is on.',
    ],
    controls: [
      'A package with an open renewal request is never renewed twice.',
      'Expired packages are never deleted: they stay readable on the Products screen and are reactivated through a REACTIVATE request.',
    ],
  },
];

/**
 * The help section of the Product Maintenance sidebar section: the package request screens first,
 * then the catalog screens (mirrors `withPackageRequests` of module.ts).
 *
 * @param catalog help section of the catalog screens
 * @returns the combined section
 */
export function withPackageRequestHelp(catalog: HelpSection): HelpSection {
  return { ...catalog, screens: [...PACKAGE_REQUEST_HELP_SCREENS, ...catalog.screens] };
}
