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
      'The landing page of Product Maintenance: the Product Matrix (Active Products, Expiring Products within 90 days, Expired Products), the Quotation Request List and the Package Request List.',
    workflow: [
      'Product Matrix: search, filter by line and product type, sort any column and export to Excel, PDF or CSV; Add New Package, Update Package and Deactivate Package from the row menu.',
      'Quotation Request List: the quotation requests of non-package products with their status and aging; a row opens the request.',
      'Package Request List: the package creation and update requests with the Account Officer, the Assigned TSU User and the Submission Date.',
      'Create Package Request starts the Package Request Form; Dashboard opens the key figures.',
    ],
    controls: [
      'Expired Products need the archive permission.',
      'Deactivate Package asks for the effective date, the reason and the approver when PM_DEACTIVATION_ROUTE is EFFECTIVE_DATED; otherwise it opens a retirement package request.',
    ],
  },
  {
    name: 'Product Maintenance Dashboard',
    path: '/product-maintenance/dashboard',
    summary:
      'Key figures for the period, TSU officer, product line and package type: Incoming Requests, In-Progress Requests, For Approval, Expiring Packages, Issued Proposals and Deactivation Requests; below, the package requests by stage with their service levels.',
    workflow: [
      'Change a filter and the figures refresh; a figure opens its requests with the Request Number, Request Type, Product Line, Requested By, Assigned TSU Officer, Current Status, Submission Date and Aging, oldest first.',
      'A row opens the request; Excel, PDF and CSV export the requests of the figure with the filters on screen.',
    ],
    controls: [
      'TSU, MBS and management see every request; Marketing sees the requests it raised or holds.',
      'Aging counts the days since the request entered its current stage.',
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
      'The daily scheduled job alerts TSU and MBS at the notice period and again at 30 and 7 days, and drafts renewal requests itself when PACKAGE_RENEWAL_AUTODRAFT is on.',
    ],
    controls: [
      'A package with an open renewal request is never renewed twice.',
      'Expired packages are never deleted: they stay readable on the Products screen and are reactivated through a reactivation request.',
    ],
  },
  {
    name: 'Deactivation Requests',
    path: '/product-maintenance/deactivations',
    summary:
      'Package deactivation requests with the Request Number, Package Name, Deactivation Effective Date, Approval Status, Requested By, Request Date and Package Expiry Date.',
    workflow: [
      'Search by request number or package and filter by approval status and request date; a row opens the details and the supporting documents.',
      'The selected approver approves or rejects with remarks; the requestor may withdraw a pending request and a team head may reassign it.',
    ],
    controls: [
      'The effective date is today or later; a package has one pending request at a time.',
      'On approval the package expiry date becomes the later of the effective date and the approval date; the package is deactivated after that date.',
      'Remarks are mandatory for a rejection, and the package then stays active.',
    ],
  },
  {
    name: 'Product Master Transfers',
    path: '/product-maintenance/master-changes',
    summary:
      'Every release, deactivation and expiry of a package is recorded for the other systems of the bank and sent in one file (header, one record per change, trailer with the count) on a schedule.',
    workflow: [
      'Send Now sends the waiting changes at once; a failed change is sent again from its row.',
      'In SIT and UAT the receiving system is simulated: its tab shows the files received.',
    ],
    controls: [
      'A failed transfer raises an alert and keeps the error on each change.',
      'The folder of the receiving systems is set at deployment; until it is set the changes wait.',
    ],
  },
  {
    name: 'Audit Logs',
    path: '/product-maintenance/audit-logs',
    summary:
      "Every activity on packages, products, package requests, quotation requests and deactivation requests: Timestamp, Module, Reference Number, Client/Assured's Name, Action Type, Description, Old Value, New Value, Performed By and Remarks.",
    workflow: [
      'Filter by date range, action type, reference number and user; sort by timestamp, reference, action or user.',
      'CSV and Excel export the filtered entries as Audit Logs_MMDDYYYY.',
    ],
    controls: ['Each record also shows its own audit logs, with the same export.'],
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
