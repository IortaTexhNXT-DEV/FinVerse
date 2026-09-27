import type { HelpSection } from '@/features/help/helpContent';

/**
 * In-app help of the Data Migration console (BRD-13), in sidebar order.
 */
export const MIGRATION_HELP: HelpSection = {
  id: 'migration',
  module: 'Data Migration',
  intro:
    'The Data Migration console moves the legacy data of EBIX, QPS and the Excel trackers into BIBS for the single go-live in January 2028. Each data object has a decided class (migrate, carry forward, archive, excluded or conditional); its extracts are checked, mapped with approved code maps, validated, loaded through the BIBS services, reconciled and accepted through the gates G1 to G7. Every migrated record keeps its source system, legacy reference and loading batch, shows a LEGACY badge in its module and raises no notification or outbound event.',
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
      summary:
        'The code maps from legacy codes to BIBS values, with their versions. A version is prepared as a draft (edited or imported from Excel), submitted by the Data Steward and approved by the business owner; the approved version is used by the next batches. Unmapped Codes lists the legacy codes the validation found without an entry.',
      workflow: [
        'New Version copies the approved entries into a draft; add, change or delete entries, then Submit.',
        'Map points a legacy code to an existing BIBS value; Create adds the value in BIBS through the reference-data load; Default uses the default value; Reject refuses the record.',
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
        'The extract layouts in force with their columns, types, code maps and validations - the single source of the load templates. Download the template of a layout, the object workbook, the control file template or the workbook of all objects with its control file and How to fill sheets. The data-quality rules and the masking rules of non-production extracts are listed here.',
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
        'The load runs through the BIBS services as the migration loader, without notifications or outbound events.',
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
      name: 'Sign-off',
      path: '/migration/signoff',
      summary:
        'The gates G1 to G7 of every object and of its latest batch, with the signer and date. The business owner signs the mapping of an object (G2) here; batch gates are signed on the batch page with their evidence.',
      controls: ['One person does not sign two gates of one batch in different roles.'],
    },
  ],
};
