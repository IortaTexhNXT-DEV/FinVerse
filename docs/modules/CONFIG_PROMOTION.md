# Configuration Promotion

Guide of the Configuration Promotion module of iNXT BrokerVerse (`configpromo`): it moves the set-up of the platform
(masters, configuration, rules and validations) from one environment to another - development, SIT, UAT, production -
without moving any transaction. It also gives every configuration screen an **Upload** action, so the master data and
configuration workbook is loaded tab by tab under maker-checker.

Screens: System Administration > Configuration Promotion (Export Configuration, Import Configuration, Packages and
History, Baselines and Drift, Environment Overrides), and the Upload action of the 27 configuration screens with the
approval screen Configuration Uploads (opened from My Approvals). API: `/api/v1/config-promotion/**` and
`/api/v1/config-uploads/**`. Flyway V1320 (promotion), V1321 (approval of uploads), seed V2560.

## 1. Purpose

- The configuration of an environment is described by a **configuration catalogue**
  (`backend/src/main/resources/configpromo/catalogue.yml`): 153 datasets in 16 groups, each with its table, its
  natural key, the datasets it refers to and what each environment keeps for itself. Every other table of the schema
  is classified as never promoted, with its reason (421 tables: transactions, client data, logs, secrets, running
  numbers, files, environment state, planning figures, watchlist contents and the module's own records). A build test
  (`ConfigCatalogueSchemaIT`) fails when a table is neither a dataset nor classified, so a new table is always placed.
- One table may hold two datasets when each names its rows (`rows`): the **standard report variants** of the Report
  Centre (V2610, and the shared Claims variants marked standard by V2620) are configuration in the Reports group and
  are promoted by default; the variants the users saved for themselves stay user records, promoted only with users.
- An **export** writes the datasets into a signed, versioned **package**. An **import** verifies the package, checks
  it against the target environment, compares it in a **dry run**, waits for the **approval of a second user** and
  applies it in one database transaction, after keeping a **snapshot** of the configuration it changes.
- A **baseline** marks the configuration at a point in time; the **drift report** compares the environment with it,
  and an incremental export takes only the datasets changed since it.

## 2. Personas and roles

| Persona | Group profile | SIT/UAT user | What they do |
|---|---|---|---|
| System Administrator | `SYSADMIN` | `admin` | Exports packages, uploads and checks imports, submits them, rolls back, marks baselines, reads the drift report |
| Configuration Release Approver | `CONFIG_APPROVER` | `cfgapprover` (Consolacion Configuration Approver) | Approves and applies, or rejects, the imports prepared by another user |
| Owners of the configuration screens | the profiles holding the maintenance permission of the screen | per module | Upload the tab of the workbook on their screen and submit it |
| Approvers of the configuration screens | the profiles holding the approval permission of the screen | per module | Approve or reject the uploads of their screens |

Permissions: `CONFIG_EXPORT`, `CONFIG_IMPORT_PREPARE`, `CONFIG_BASELINE_MANAGE` (System Administrator) and
`CONFIG_IMPORT_APPROVE` (Configuration Release Approver). The separation-of-duties rule SOD-CFG-01 keeps the two
profiles apart, and an import is never approved by the user who prepared it. The seed grants these profiles in the SIT
and UAT databases only (V2560 in `db/seed`).

## 3. The configuration catalogue

| Group | Datasets (examples) |
|---|---|
| Organisation | Companies, branches, employees, holiday calendar, cost centres and business lines, sales units |
| Lists of values, parameters and switches | Lists and their values, system parameters and numbering prefixes, product module switches, currencies |
| Product catalogue | Product lines, products, versions and packages, coverages, clauses, field and document rules, rates, incentive criteria |
| Insurers and business partners | Insurers, insurer branches, commission rates, business partners |
| Accounting | Chart of accounts and categories, accounting events and rules, journal types, legacy account maps |
| Tax | Tax codes, tax forms and their lines |
| Cash and banking | Bank accounts, check books, petty cash funds, statement layouts, payment file layouts |
| Operations, collections and commission rules | Operations rules, collections rules and escalations, commission and incentive rules |
| Renewal and submitted policies rules | Renewal rule sets and matrices, submitted-policy rule sets, limits, approval matrix |
| Claims and employee benefits rules | Claim handlers set-up, EB threshold rules and document rules |
| Sanction screening configuration | Configuration versions with their rules, routes and layouts |
| Workflows, notifications and document templates | Workflow definitions, notification events, document templates |
| Report definitions and layouts | Report definitions, variants and layouts |
| Group profiles, permissions and separation of duties | Roles, role permissions, permission action classes, SoD rules |
| Migration console layouts and code maps | Layouts, rules, code map sets and approved versions |
| Users and their assignments (optional) | Users (never passwords, second factor or sessions), their profiles and data scope |

Rules of the catalogue:

- **Natural keys, never ids.** A reference to another record is written as that record's natural key (for example the
  branch code and company code, not `branch_id`); the import resolves it in the target and remaps the ids. A reference
  to a record that is neither in the package nor in the target is a blocker of the dry run.
- **Collections** (lines of a rule, permissions of a profile, items of a version) travel with their parent and replace
  the parent's lines as a whole.
- **Environment values** stay where they are: columns such as e-mail addresses, file patterns, legacy links, the
  sign-in mode or the change window are never overwritten, and the parameters of the environment
  (`environmentRows`) are neither exported nor imported. Environment Overrides lists them.
- **Users** are optional: they are exported only when asked, never with a password, second factor or session; a new
  user cannot sign in until a password is set in the target.
- **Never deleted.** An item only in the target is listed; on request it is deactivated (record status INACTIVE or
  active off), never deleted, and the import refuses to deactivate an item that transactions use.

## 4. Packages

A package is a zip file:

| Entry | Content |
|---|---|
| `manifest.json` | Format and format version, package number, source environment, platform version, schema fingerprint, mode (FULL or INCREMENTAL with its baseline), creator and time, and per dataset its file, row count and SHA-256 |
| `datasets/<CODE>.json` | The rows of the dataset in canonical JSON (sorted keys, natural keys, booleans as booleans, other values as text) |
| `signature.json` | HMAC-SHA256 of the manifest with the signing key of the environments and its key identifier |

The import refuses a package whose signature does not match, whose files do not match their checksums, that holds
files the manifest does not list, or that is larger than `CONFIG_PROMOTION_MAX_PACKAGE_MB`. The compatibility check
refuses a package with a dataset the target does not know or whose fields differ (fingerprint of the columns of each
dataset); another schema or platform version with the same fields is a warning.

## 5. Import lifecycle

| Status | Meaning | Next |
|---|---|---|
| Checked | Verified and compared in a dry run | Submit (preparer), Check Again with other choices, Withdraw |
| Waiting for Approval | Submitted | Approve and Apply or Reject (another user with `CONFIG_IMPORT_APPROVE`), Withdraw |
| Applied | Applied with its reconciliation; the snapshot taken before is kept | Roll Back (prepares an import of the snapshot) |
| Rejected / Withdrawn | Nothing applied | - |
| Failed | The apply failed; nothing of it was kept (one transaction) | Check Again, Withdraw |

The **dry run** lists, per dataset, the items added, changed (field by field), unchanged and only in this environment,
and the findings: blockers (missing references, schema differences, items in use that would be deactivated) and
warnings. It also runs the whole apply in a transaction that is rolled back, so a constraint of the database is found
before the approval.

The **apply** runs in dependency order (a dataset after the datasets it refers to; self-references in a second pass),
in one transaction with the ids remapped, then **reconciles** each dataset: the content checksum of the package must
equal the checksum of the target after the apply. The reference-data caches are cleared and the notified groups
(`CONFIG_PROMOTION_NOTIFY`) are told. Every step is in the audit trail.

**Rollback**: Roll Back on an applied import prepares a new import of its snapshot, checked and approved like any
import.

## 6. Safety

- **Production** (`brokerverse.environment` is `production`, or the `prod` profile is active): a change request number and a reason are mandatory;
  an import is applied only inside the change window `CONFIG_PROMOTION_PRODUCTION_WINDOW` (for example
  `SAT-SUN 20:00-06:00`; empty = no window, production imports are refused).
- **No apply during a close or a batch run**: the apply takes the job lock `CONFIG_PROMOTION_APPLY` and is refused
  while a period close (`GL_PERIOD_CLOSE`, `BROKING_BOOKS_CLOSE`) or a batch job holds its lock; the approver tries
  again later (`CONFIG_IMPORT_JOB_RUNNING`).
- **Signing key**: `BROKERVERSE_CONFIG_PROMOTION_SIGNING_KEY`, the same in every environment of one promotion path
  (at least 32 characters); without it exports and imports are refused and the screens say so.
- Notification events: `CONFIG_IMPORT_TO_APPROVE`, `CONFIG_IMPORT_APPLIED`, `CONFIG_IMPORT_REJECTED`,
  `CONFIG_IMPORT_FAILED`.

## 7. Deployment pipeline

A deployment pipeline promotes with a service user holding `CONFIG_IMPORT_PREPARE`:

1. `POST /api/v1/config-promotion/exports` in the source (or download a package kept under Packages and History).
2. `POST /api/v1/config-promotion/imports` in the target with the package, `pipeline=true`, the change request
   number and the reason: the package is verified and compared; the response holds the findings.
3. Outside production, when `CONFIG_PROMOTION_PIPELINE_APPLY` is true, `apply=true` (or
   `POST /imports/{id}/pipeline-apply`) applies a clean dry run at once. Otherwise, and always in production,
   `POST /imports/{id}/submit` sends it to a Configuration Release Approver.

## 8. Uploads of the configuration screens

Each configuration screen has an **Upload** action (shown to the holders of its maintenance permission). The
template is the tab of the master data and configuration workbook (same headers, same order, same guide rows), and
**Current Data** downloads what the screen holds in the same layout, so a download uploads again unchanged. Every row
is validated before anything changes, with business messages naming the row and the column; the rows to correct
download in the template layout. The preview tells for each valid row whether it adds a record or updates the record
with the same natural key, so uploading the same file twice changes nothing. The uploader submits the upload; a
second user holding the approval permission of the screen approves it (the valid rows are then applied, each audited)
or rejects it.

| Tab | Screen | Maintain | Approve |
|---|---|---|---|
| D0-01 Company | Setup > Companies | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-02 Branches | Setup > Branches | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-03 Holiday calendar | Setup > Holiday Calendar | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-04 Dimensions | Setup > Financial Dimensions | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-05 Employees | Setup > Employees | `EMPLOYEE_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-07 Categories and numbering | General Ledger > Chart Upload & Numbering | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-08 Accounting rules | Accounting Engine > Accounting Rules | `ACCOUNTING_RULE_MANAGE` | `ACCOUNTING_RULE_MANAGE` |
| D0-09 Legacy accounts | General Ledger > Chart of Accounts | `MIG_MAPPING_EDIT` | `MASTER_AUTHORIZE` |
| D0-10 Tax codes | Tax & Statutory > Tax Codes & Forms | `TAX_MANAGE` | `MASTER_AUTHORIZE` |
| D0-11 Bank accounts | Disbursement > Bank Accounts and Checks | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-12 Check books | Disbursement > Bank Accounts and Checks | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-13 Petty cash funds | Payables & Cash > Petty Cash | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| D0-14 Statement layouts | Setup > Bank Statement Layouts | `RECONCILIATION_MANAGE` | `MASTER_AUTHORIZE` |
| D0-15 Approval limits | Submitted Policies Setup; EB Setup | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| PM-02 Coverages and rules | Product Maintenance > Coverages & Clauses; Products | `PRODUCT_MAINTAIN` | `PRODUCT_AUTHORIZE` |
| PM-03 Clause library | Product Maintenance > Coverages & Clauses | `PRODUCT_MAINTAIN` | `PRODUCT_AUTHORIZE` |
| PM-04 Rate tables | Product Maintenance > Rates & Taxes | `PRODUCT_MAINTAIN` | `PRODUCT_AUTHORIZE` |
| PM-04C Other charges (platform layout; charges of the checklist PM-04) | Product Maintenance > Rates & Taxes > Other Charges | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| PM-06 Packages | Product Maintenance > Products | `PRODUCT_MAINTAIN` | `PRODUCT_MAINTAIN` (second package maintainer; the draft is then validated and released on Products) |
| PM-08 Document templates | Broking Setup > Document Templates | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |
| PM-09 Lists and parameters | Broking Setup > Lists of Values; System Parameters | `LOV_MANAGE` | `MASTER_AUTHORIZE` |
| PM-10 Incentive criteria | Product Maintenance > Incentive Criteria | `INCENTIVE_CRITERIA_MAINTAIN` | `PRODUCT_AUTHORIZE` |
| UA-02 Role matrix | Administration > Roles & Permissions | `ROLE_MANAGE` | `ACCESS_APPROVE` |
| UA-03 Approval rules | Administration > Users | `USER_MANAGE` | `ACCESS_APPROVE` |
| UA-04 SoD rules | User Access > Separation of Duties | `UAM_SOD_MAINTAIN` | `UAM_SOD_AUTHORIZE` |
| UA-05 Parameters | Administration > System Parameters | `SYSTEM_PARAMETER_MANAGE` | `SECURITY_PARAMETER_APPROVE` |
| UA-06 Lists of values | Broking Setup > Lists of Values | `LOV_MANAGE` | `MASTER_AUTHORIZE` |
| MD-01 Business partners | Setup > Business Partners | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` |

Rules of particular tabs:

- Columns the platform added after the workbook tab (the BIR registration of D0-01 and D0-02: RDO code, VAT
  registration, CAS and e-invoicing permits, BIR branch code) are at the end of the template and may be missing from
  a file filled on the workbook; the download of the current data holds them. D0-10 takes the final tax types (FWT,
  FINAL_VAT, PERCENTAGE_TAX); FWT needs its ATC like EWT.

- Cells holding several values use semicolons (`50; Y`, `01-Jan-2028; 31-Dec-2028`); the cells of PM-06 and PM-10
  that hold several entries (coverages, insurers, insurer terms, products) take one entry per line of the cell.
- D0-09 adds the legacy accounts to the draft of the code map `GL_ACCOUNT` of the Migration Console, approved there.
- D0-13 starts a new fund with its imprest as cash on hand; the cash of an existing fund is never changed by an upload.
- D0-15 loads the Submitted Policies approval matrix and the EB thresholds; the routes of Sanction Screening belong to
  a draft configuration version of Compliance Setup and disbursements follow the authorisation limit of the user
  (UA-03), so those rows are refused with where to set them. The approver column takes the permission the approvers
  hold, which is what both matrices store.
- PM-06 makes a draft version of a packaged product, identified by its ManCom sign-off reference; a reference already
  released changes nothing.
- PM-08 adds a new version of the template when the title, text or date differ from the latest version; the merge
  fields of each template are checked.
- UA-03 refuses a profile that a separation-of-duties rule forbids next to a profile the user holds;
  `UAM_ANY_APPROVER` is a security parameter changed with UA-05.
- UA-04 loads the Prevent rules between two group profiles; rules followed by review or by a second approval are
  refused with that reason.
- UA-05 refuses a row whose value as delivered no longer matches the parameter (changed since the workbook was
  filled) and a parameter whose change already waits for approval on System Parameters.

## 9. Adding a table

A new table must be placed in the catalogue in the same change: either as a dataset (natural key, references,
environment columns) or under `excluded` with its reason. `ConfigCatalogueSchemaIT` and `ConfigPromotionFreshDatabaseIT`
(export of the seeded database, import into a database built by the migrations only, equal checksums, no transaction
copied) fail otherwise.

## 10. Open points

- Workbook values that differ from the platform codes: D0-10 tax types (the workbook lists EWT, FWT, VAT, PT; the
  platform codes are VAT_OUTPUT, VAT_INPUT, VAT_ZERO_RATED, VAT_EXEMPT, PREMIUM_TAX, DST, LGT, FST, EWT); D0-11
  payment notification format (the example says EMAIL; the platform formats are FIXED_WIDTH, CSV and DCTF); D0-15
  approver column (a group profile in the workbook, a permission in the platform). BDOI to confirm the workbook
  wording.
- The signing key is shared by the environments of one promotion path; its custody and rotation are with BDOI IT.
- Whether the pipeline may apply its own dry run in SIT and UAT (`CONFIG_PROMOTION_PIPELINE_APPLY`, off by default).
