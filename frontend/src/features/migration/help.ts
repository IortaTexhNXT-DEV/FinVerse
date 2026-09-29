import type { HelpSection } from '@/features/help/helpContent';
import { BRAND } from '@/branding';

/**
 * In-app help of the Data Migration console (BRD-13), in sidebar order.
 */
export const MIGRATION_HELP: HelpSection = {
  id: 'migration',
  module: 'Data Migration',
  intro: `The Data Migration console moves the legacy data of EBIX, QPS and the Excel trackers into ${BRAND.product} for the single go-live in January 2028. Each data object has a decided class (migrate, carry forward, archive, excluded or conditional); its extracts are checked, mapped with approved code maps, validated, loaded through the ${BRAND.product} services, reconciled and accepted through the gates G1 to G7. Every migrated record keeps its source system, legacy reference and loading batch, shows a LEGACY badge in its module and raises no notification or outbound event.`,
  screens: [
    {
      name: 'Migration Home',
      path: '/migration',
      summary:
        'Tiles with the objects decided and waiting for a decision, the batches in progress or failed, open reconciliation breaks, unmapped legacy codes, open validation errors and client pairs to review; the next cut-over tasks and the run-off of the legacy policies.',
      controls: ['Every console user sees the tiles; each tile opens its list.'],
    },
    {
      name: 'Data Objects',
      path: '/migration/objects',
      summary:
        'The register of the legacy data objects with the four criteria (Day-1 need, compliance need, archive option, data trust), the proposed and decided class, owners, dependencies and load order. Download the load template of an object or of all objects.',
      workflow: [
        'The Data Migration Lead submits the proposed class of an object for decision.',
        'The business owner of the object approves it (gate G1) or returns it with a reason.',
      ],
      controls: [
        'Only the business owner of an object decides its class; the submitter never approves.',
        'The class of an object with loaded data cannot change from a loaded class to an unloaded one.',
      ],
    },
    {
      name: 'Code Maps',
      path: '/migration/maps',
      summary: `The code maps from legacy codes to ${BRAND.product} values, with their versions. A version is prepared as a draft (edited or imported from Excel), submitted by the Data Steward and approved by the business owner; the approved version is used by the next batches. Unmapped Codes lists the legacy codes the validation found without an entry.`,
      workflow: [
        'New Version copies the approved entries into a draft; add, change or delete entries, then Submit.',
        `Map points a legacy code to an existing ${BRAND.product} value; Create adds the value in ${BRAND.product} through the reference-data load; Default uses the default value; Reject refuses the record.`,
        'A conditional entry applies only when another column has the given value (for example the insurer of a package).',
      ],
      controls: [
        'The maker of a version never approves it; approving supersedes the previous version.',
        'A batch records the code map versions it used.',
      ],
    },
    {
      name: 'Layouts and Rules',
      path: '/migration/layouts',
      summary:
        'The extract layouts in force with their columns, types, code maps and validations - the single source of the load templates. Download the Excel load template of a layout (one guided sheet: what it is for, the rules of a delivery and, above each column, whether it is mandatory, its format, allowed values and what to enter), the object workbook or the workbook of all objects (a Start here sheet and one guided sheet per layout), and the control file template. A filled Excel template is uploaded as it is on Extracts; large extracts use the CSV layout of the same columns. The data-quality rules and the masking rules of non-production extracts are listed here.',
      controls: [
        'A layout is frozen before its extracts are accepted; a new version retires the previous one.',
        'Outside production the personal data of the extracts is masked on intake.',
      ],
    },
    {
      name: 'Extracts',
      path: '/migration/extracts',
      summary:
        'Upload a legacy extract (LAYOUT_SOURCE_yyyyMMdd_nn.csv or .xlsx) with its control file. The intake checks the checksum, the header against the layout in force, the row count, the amount totals and the hash total; the extract is staged or rejected with the reason.',
      controls: [
        'The same file cannot be received twice; delta extracts must follow the as-of order.',
        'Extract files and staged rows are purged after the retention days (5 by default).',
      ],
    },
    {
      name: 'Batches',
      path: '/migration/batches',
      summary:
        'The load batches with their steps, counts, issues, rows, reconciliation, sign-offs and run log. Plan a batch from the staged extracts of a decided object, validate it, sign the validation (G3), approve the load (G4), load, reconcile, sign the reconciliation (G5) and accept the object (G6).',
      workflow: [
        'The data owner waives failing rows (they load) or excludes them with a manual-entry plan.',
        'Rerun Rejects plans a new batch with the rejected rows; Resubmit Corrected Rows prepares a resubmission of a corrected rejection file for a checker.',
        'Request Rollback undoes the records of a batch after a second person approves it.',
      ],
      controls: [
        'The operator who validated a batch cannot approve its load; the approver cannot run it; the operator cannot sign its reconciliation or acceptance.',
        `The load runs through the ${BRAND.product} services as the migration loader, without notifications or outbound events.`,
      ],
    },
    {
      name: 'Client Matching',
      path: '/migration/matching',
      summary:
        'Pairs of client records whose score is between the review and the automatic thresholds, side by side with the keys that matched. Merge them or keep them separate; the survivorship rules decide the values kept.',
      controls: ['A client batch cannot be approved while pairs wait for review.'],
    },
    {
      name: 'Reconciliation',
      path: '/migration/reconciliation',
      summary:
        'The loaded batches and, for each, the reconciliation lines by level: counts, amounts, hash totals, fields and the general ledger. Explain a break with a reason; a second person approves the explanation.',
      controls: ['The reconciliation is signed (G5) only when no break is open.'],
    },
    {
      name: 'Opening-Balance Adjustments',
      path: '/migration/trueups',
      summary:
        'The year-end adjustments of the legacy books after go-live. The Comptrollership GL lead prepares an adjustment on a validated batch of adjustment journals and the legacy trial balance of the same version; the Head of Comptrollership approves it; loading the batch posts the adjustment journals into the opening period and adjusts the open legacy invoices; the adjustment is then reconciled and signed.',
      controls: [
        'The preparer never approves or signs the same adjustment.',
        'Only an approved adjustment is posted; a posted adjustment is corrected by the next one, never edited.',
        'Income and expense lines go to retained earnings; legacy control accounts go to Migration Clearing, which must stay at zero.',
      ],
    },
    {
      name: 'Cutover',
      path: '/migration/cutover',
      summary:
        'The mock runs, the dress rehearsal and the production cut-over. Each plan gets the runbook tasks planned from its go-live date, with their owners and dependencies, and the twelve go / no-go criteria: seven measured by the system (objects accepted, count and amount reconciliation, financial rejects, Migration Clearing, legacy control accounts, client review queue) and five recorded with evidence. Download the runbook as a workbook.',
      workflow: [
        'The Data Migration Lead creates the plan and records the start and end of each task during the run.',
        'Measure Criteria refreshes the measured criteria; the recorded ones are marked met or not met with their evidence.',
        'The go / no-go board decides GO or NO-GO; on the production plan the decision signs the go-live gate.',
      ],
      controls: [
        'A task starts only when the tasks it depends on are finished; a blocked task needs its reason.',
        'A GO with criteria not met needs its justification; a NO-GO always needs its reason.',
      ],
    },
    {
      name: 'Run-off and Decommissioning',
      path: '/migration/runoff',
      summary: `The monthly run-off of the legacy in-force headers by expiry month and legacy system (renewed, not renewed, lapsed, still open), and the decommissioning checklists: one per legacy system, and one for the legacy context of ${BRAND.product} whose criteria (no open legacy invoice, no legacy unapplied balance, legacy accounts at zero) are measured.`,
      workflow: [
        'The snapshot runs on the first of each month; Take Snapshot adds one now.',
        'Open the checklist of a legacy system when its decommissioning starts; record each criterion as met with its evidence, then sign it.',
      ],
      controls: [
        'Every criterion needs its evidence; the measured criteria are refreshed each time the page opens.',
      ],
    },
    {
      name: 'Sign-off',
      path: '/migration/signoff',
      summary:
        'The gates G1 to G7 of every object and of its latest batch, with the signer and date. The business owner signs the mapping of an object (G2) here; batch gates are signed on the batch page with their evidence.',
      controls: ['One person does not sign two gates of one batch in different roles.'],
    },
  ],
};
