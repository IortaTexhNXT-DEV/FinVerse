import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Configuration Promotion screens. */
export const CONFIG_PROMOTION_HELP: HelpSection = {
  id: 'config-promotion',
  module: 'Configuration Promotion',
  intro:
    'Moves the set-up of the platform (masters, configuration, rules and validations) from one environment to another (development, SIT, UAT, production) without moving any transaction.',
  screens: [
    {
      name: 'Export Configuration',
      path: '/admin/config-promotion/export',
      summary:
        'The configuration catalogue of this environment by group, with the number of items of each dataset. Export every dataset, a selection, or only the datasets changed since a baseline, into a signed package.',
      workflow: [
        'Choose whether users are included (never their passwords or second factor) and, for an incremental package, the baseline.',
        'Tick the datasets or keep the default selection, give the purpose and export.',
        'Download the package and upload it in the target environment.',
      ],
      controls: [
        'Transactions, client data, logs, files, secrets and running numbers are never exported.',
        'The package lists every dataset with its number of items and checksum and is signed; a package changed after its export is refused.',
        'Every export is recorded in the audit trail.',
      ],
    },
    {
      name: 'Import Configuration',
      path: '/admin/config-promotion/imports',
      summary:
        'Upload a package from another environment: it is verified, checked against this environment and compared in a dry run (items added, changed, unchanged and only in this environment, field by field).',
      workflow: [
        'Upload the package with the change request number and the reason (mandatory in production).',
        'Review the findings and the differences of each dataset; leave datasets out or deactivate the items only in this environment, then check again.',
        'Submit for approval; a second user with the approval right approves, which applies the package in one step after keeping a snapshot of the configuration, and reconciles every dataset.',
        'Roll Back prepares an import of the snapshot, approved like any import.',
      ],
      controls: [
        'The preparer never approves the import; the two profiles are kept apart by a separation-of-duties rule.',
        'In production an import is applied only in the change window and while no period close or batch job runs.',
        'Items are deactivated, never deleted; an item that transactions use is never deactivated.',
        'A failed apply keeps nothing; every step is in the audit trail with the checksum of the package.',
      ],
    },
    {
      name: 'Packages and History',
      path: '/admin/config-promotion/packages',
      summary:
        'Every package exported here, uploaded for an import or kept as a snapshot before an import, with its source, contents and checksum.',
      workflow: ['View the datasets of a package, download it, or mark it as a baseline.'],
    },
    {
      name: 'Baselines and Drift',
      path: '/admin/config-promotion/baselines',
      summary:
        'Configuration baselines of this environment and the drift of the current configuration from a baseline, dataset by dataset and item by item.',
      controls: ['Only a package exported here or applied here can become a baseline.'],
    },
    {
      name: 'Environment Overrides',
      path: '/admin/config-promotion/overrides',
      summary:
        'This environment (production or not, change window, signing key, versions) and the values it keeps for itself, which an import never changes.',
    },
    {
      name: 'Configuration Uploads',
      summary:
        'Each configuration screen has an Upload action: the template is the tab of the master data and configuration workbook, and the current data downloads in the same layout. The approver opens the upload from My Approvals.',
      workflow: [
        'On the configuration screen, download the template or the current data, fill it and upload it with Upload.',
        'Every row is checked first; the messages name the column, and the rows to correct download in the template layout.',
        'The preview tells for each valid row whether it adds a record or updates the record with the same code; submit the upload for approval.',
        'Another user with the approval right opens the upload here and approves it, which applies the valid rows, or rejects it with a reason.',
      ],
      controls: [
        'Nothing changes before the approval, and the uploader never approves the upload.',
        'A row whose code exists updates that record; uploading the same file again changes nothing.',
        'Each upload type needs its own maintenance permission, and its approval permission to approve; every record written is in the audit trail.',
      ],
    },
  ],
};
