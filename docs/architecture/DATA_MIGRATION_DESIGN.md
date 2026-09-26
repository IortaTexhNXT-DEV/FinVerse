# iNXT BrokerVerse - BDOI Data Migration (BRD-13) Build Design

Status: **proposal for review; not built.** It extends `docs/architecture/BROKING_ARCHITECTURE.md`,
`docs/architecture/OPERATIONS_DESIGN.md` and the Developer Guide, and it does not change them. Requirements baseline:
[`BDOI_DM_BRD_SPEC.md`](../requirements/BDOI_DM_BRD_SPEC.md) (23 requirement rows BRID 1.1a-12.1, fit/gap and questions
DMQ01-DMQ35). Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRID 5.2`.

This design is also the input of four later deliverables: the Data Migration Strategy and Approach (deliverables item
29), the BDOI data requirements workbook, the cutover runbook and the migration test plan. Sections 2, 5-13 and 17
give their content; section 27 lists what BDOI must supply per data object.

## 1. Design principles

1. **Selective, not a conversion** (BRD p.3). Each data object gets one class through a decision gate: MIGRATE
   (master and reference data), CARRY_FORWARD (open items only), ARCHIVE (read-only history) or EXCLUDED (BRID 1.1a).
   History is never loaded into business tables.
2. **Business tables are written only through the owning module's service.** A loader calls the same public service
   a screen or a bulk handler calls (`ClientOnboardingService`, `AccountService`, `UnappliedService`,
   `SystemJournalService`, ...), so every invariant, audit row, event and posting of the platform applies. The only
   tables written directly are the `mig_*` tables of this module. Where no service exists, the owner adds one; the
   migration never inserts into another module's table.
3. **Everything is reconciled, and the reconciliation is evidence.** Every extract comes with control totals; every
   object is reconciled by counts, amounts, hash totals and field by field, and the GL by a clearing account that must
   net to zero (BRID 1.1b). A break is either fixed or explained and approved; nothing is signed off with an open
   break.
4. **Idempotent and rerunnable.** Every loaded record has a cross-reference from its legacy key
   (`mig_key_xref`); a rerun skips what is loaded and unchanged, and loads only what failed or changed.
5. **A legacy invoice is a first-class invoice of the Operations ledger.** It is not re-booked. It enters
   `ops_invoice` with origin LEGACY and its opening positions, and every Operations module processes it with its
   normal screens. Only the GL routing differs: its postings carry the **legacy components** (`LG_` prefix) that
   Comptrollership maps to the legacy control accounts (BRID 5-10).
6. **No new posting path.** Legacy sub-ledgers are served by the existing accounting engine: the same events, with
   additional amount components. Rule lines whose component is absent produce no journal line
   (`accounting/service/JournalLineBuilder.java` line 29-31), so one rule per event serves new and legacy invoices.
7. **One processing system per item.** From the freeze, a legacy open item is processed only in BIBS; legacy is
   read-only (BRD p.5). There is no two-way synchronisation of open items.
8. **Protected staging.** Staging data is masked outside production, purged within 5 days of sign-off, encrypted,
   and reachable only by migration roles from the Philippines (hosting appendix).
9. **Parked means seam, not fake** (Operations principle 5). The legacy transport (SFTP), the legacy read-only link
   and archive document transfer get seams; no integration is simulated.

## 2. Scope: the data object catalogue

The BRD lists no objects (DMQ01). The catalogue below is the proposal that the decision gate (BRID 1.1a, section 13)
confirms object by object. Class: M = MIGRATE, CF = CARRY_FORWARD, A = ARCHIVE, X = EXCLUDED, C = conditional.

| Code | Object | Source (proposed) | Class | Target (service called) | BRD | Load order |
|---|---|---|---|---|---|---|
| R01 | Lists of values and MIS values (market segment, business origin, customer segment, department, region, area, account type, ...) | QPS, EBIX | M (map, create missing) | `lov.service.LovService` | 3.1 | 1 |
| R02 | Branches and invoicing branches | EBIX | M (map only; branches configured) | `organization` (check only) | 3.1 | 1 |
| R03 | Sales organisation: units, unit heads, account officers | QPS, EBIX | M | `catalog` `SalesUnit`, `SalesOfficer` services | 3.1 | 2 |
| R04 | Insurers, insurer branches and their parties | QPS, EBIX | M (map, create missing) | `catalog.service.InsurerService`, `party.service.PartyService` | 3.1 | 2 |
| R05 | Product lines, cover types, products / risk codes | QPS, EBIX | M (map, create missing) | `catalog.service.ProductCatalogService` | 3.1 | 3 |
| R06 | Packages (active package versions) | QPS | M | `catalog` product versions (BRD-3) | 3.1 | 3 |
| R07 | Commission rates of active insurer x product | QPS, EBIX | M (DMQ30) | `catalog` commission rates | 3.1 | 3 |
| R08 | Chart of accounts mapping (legacy GL account to BIBS account) | EBIX, ISYS | M (map only) | code map GL_ACCOUNT; chart configured by Comptrollership | 5-8, AQ01 | 3 |
| R09 | Payees | EBIX / Disbursement files | M (DMQ30) | `disbursement` `PayeeMigrationHandler` service | AQ11 | 4 |
| R10 | Users and roles | QPS | X (created through User Access requests) | `nbadmin` (outside migration) | - | - |
| R11 | Receipt series in use (ATP, next number) | EBIX | CF (configuration) | `cashiering` receipt series master | 6.1 | 4 |
| C01 | Client master (individual, corporate) | QPS, EBIX, CMS | M | `crm.service.ClientOnboardingService.registerMigrated` (new) | 2.1 | 5 |
| C02 | Client contacts and addresses | QPS, EBIX | M | same call as C01 | 2.1 | 5 |
| C03 | Client payout accounts (refunds) | EBIX | M | `crm.service.ClientPayoutAccounts` | 5.4 | 6 |
| C04 | KYC documents and IDs | QPS, file shares | A (DMQ07) | archive | 11.1 | 16 |
| C05 | Screening results and risk ratings | Excel / Compliance | X (full screening run after C01) | `screening` periodic run | 2.1 | after 5 |
| P01 | In-force policy headers | QPS, EBIX | C (BRID 4.1; DMQ09) | `account.service.AccountService.importLegacy` (new) | 4.1 | 7 |
| P02 | Expired policies and their history | QPS, EBIX | A | archive | 11.1 | 16 |
| P03 | RMEL cohorts already extracted in legacy (renewal candidates in disposition) | RMEL files, QPS | CF (DMQ26) | `renewal.service.port.LegacyPolicySource` (implemented here) | 12.1 | 13 |
| P04 | Submitted-policy masterlists | Excel | M (DMQ30, SP SQ16) | `submitted` `SBM_MIGRATION` service | BRIDSP-33 | 14 |
| P05 | Employee Benefits programmes | EBIX | M (DMQ30) | `eb` `EB_PROGRAMME_LOAD` service | - | 14 |
| F01 | Open legacy invoices: PR outstanding, paid not remitted, commission receivable, PR2307; with components and insurer shares | EBIX, QPS | CF | `opsledger.service.LegacyInvoiceIntake` (new) + opening event | 5-10 | 8 |
| F02 | Unapplied premium payments (UPP) with disposition in progress | EBIX, CMS | CF | `cashiering.service.UnappliedService.createMigrated` (new) + opening event | 5.1 | 9 |
| F03 | Open collection state: dispositions, promises, installment plans, assignments | CMS | CF (DMQ34, CQ07) | `collections` `CLX_LEGACY_ITEMS` service | CQ07 | 10 |
| F04 | Remittance in flight: batches not yet paid, holds, special remittance | EBIX | CF or X (DMQ21) | `remittance` hold / special services | 8.1 | 11 |
| F05 | DP billing in process and DP commission receivable | EBIX, Operations files | CF | part of F01 + `commission` DP list service | 7.1 | 11 |
| F06 | PDCs in the warehouse, check pick-ups, refunds in process | EBIX | CF or X (DMQ33) | `cashiering` PDC and pick-up services | - | 11 |
| F07 | Open claims | EBIX, ISYS | C (DMQ30, CLQ14) | `brokerclaims` (V1025 held) | - | 14 |
| G01 | GL opening trial balance per account, branch and currency | EBIX / ISYS GL | M | `journal.service.SystemJournalService.post` (OPENING) | 5-8 | 12 |
| G02 | GL history | EBIX / ISYS GL | A | archive | 11.1 | 16 |
| H01 | Closed transactions: paid and remitted invoices, receipts, remittances, endorsements, claims, RAs, letters | EBIX, QPS, ISYS | A | archive (`mig_archive_record`) | 11.1 | 16 (before decommissioning) |
| H02 | Legacy documents (policies, SOAs, ORs, certificates) | file shares, EBIX | A | archive documents (`attachment`) | 11.1 | 16 |

Load order: a lower number loads first; objects with the same number have no dependency on each other. An object
loads only when the objects it depends on are signed off for the same environment (gate G6, section 13).

## 3. Module `migration`

| Item | Value |
|---|---|
| Package | `com.iortatechnxt.brokerverse.migration`, sub-packages `object`, `mapping`, `intake`, `quality`, `load`, `matching`, `recon`, `signoff`, `cutover`, `archive`, `legacy`, `report`, `demo`, each with `domain` / `service` / `api` (the Collections layout) |
| Tables | `mig_*` |
| Depends on | platform (`bulk` parsers, `system` jobs and parameters, `workflow`, `approval`, `audit`, `alert`, `attachment`, `report`, `security`, `organization`, `accounting`, `journal`, `lov`, `party`) and the target modules (`crm`, `catalog`, `account`, `opsledger`, `cashiering`, `collections`, `commission`, `remittance`, `disbursement`, `renewal` port, `csf` port) |
| Depended on by | nobody. Other modules reach migration data only through ports they declare (`renewal.service.port.LegacyPolicySource`, `csf.service.port.LegacyAccountLookup`, `booking.service.port.LegacyInvoiceSource` is implemented by `opsledger`, not here) |
| Flyway | V1080-V1089, demo V1980-V1989 (section 24) |
| Frontend | `features/migration/**` (Migration Console and Legacy Inquiry) |

### 3.1 Dependency graph (arrows = "depends on")

```
                       migration
       ___________________|____________________________________________
      /        |         |          |           |          |           \
    crm     catalog   account   opsledger   cashiering  collections  commission, remittance, disbursement
      \        |         |          |           |          |           /     (services only)
       \_______|_________|__________|___________|__________|__________/
                          platform: bulk (parsers), system, workflow, approval, audit, alert,
                          attachment, report, security, accounting, journal, lov, party, organization
Ports implemented by migration:  renewal.service.port.LegacyPolicySource, csf.service.port.LegacyAccountLookup
Port implemented by opsledger:   booking.service.port.LegacyInvoiceSource  (booking <- opsledger, no cycle)
```

No module depends on `migration`; ArchUnit needs no exception.

### 3.2 Ports and SPIs declared in `migration`

| Interface | Package | Purpose | Implementations |
|---|---|---|---|
| `MigrationLoader` | `migration.load.service` | One per data object: typed row mapping, load through the target service, read-back for field reconciliation, compensation for rollback | One bean per object of section 2 (for example `ClientLoader`, `LegacyInvoiceLoader`, `UppLoader`, `GlOpeningLoader`) |
| `ExtractInbox` | `migration.intake.service.port` | Where extract files arrive | Default `ConsoleUploadInbox` (upload in the console, stored in the intake bucket); `SftpFolderInbox` parked until BDOI IT names the SFTP drop (section 23) |
| `MaskingProvider` | `migration.intake.service.port` | Deterministic masking of personal data outside production | Default `HmacMaskingProvider` (keyed HMAC, key from the secrets store) |
| `ArchiveDocumentStore` | `migration.archive.service.port` | Bulk transfer of legacy documents | Default: documents uploaded with their archive extract through `attachment`; bulk S3 copy parked |

`MigrationLoader` contract:

```java
public interface MigrationLoader {
  String objectCode();                               // e.g. "F01"
  List<String> dependsOn();                          // objects that must be signed off first
  TypedRow map(StagedRow row, MappingContext maps);  // code maps applied; throws MappingException per field
  LoadOutcome load(TypedRow row, LoadContext ctx);   // calls the owning service; returns target entity, id, code
  Map<String, String> readBack(XrefEntry entry);     // target values for the field reconciliation
  Optional<Compensation> compensation(XrefEntry entry); // empty = no per-record rollback (snapshot only)
}
```

## 4. Migration lifecycle

The FRS shows the same lifecycle as a figure.

```
Data object:  PROPOSED --submit decision--> FOR_DECISION --approve (data owner)--> DECIDED(M | CF | A | X | C)
              DECIDED --mapping approved + layout frozen--> READY --batch signed off (per environment)--> ACCEPTED
Extract:      RECEIVED --checks (checksum, layout, control totals)--> CHECKED | REJECTED
              CHECKED --stage (masking outside production)--> STAGED --purge (<= 5 days after sign-off)--> PURGED
Batch:        PLANNED --validate--> VALIDATED --approve load (G4)--> LOADING --> LOADED | LOADED_WITH_REJECTS | FAILED
              LOADED* --reconcile--> RECONCILED --sign off (G5, G6)--> SIGNED_OFF
              LOADED* / RECONCILED --rollback (approved)--> ROLLING_BACK --> ROLLED_BACK
              LOADED_WITH_REJECTS --fix at source / map / waive--> child batch (mode RERUN)
```

Every transition is audited; the batch keeps a step log (`mig_batch_log`).

## 5. Source-extract intake

### 5.1 File contract (published to BDOI in the data requirements workbook)

| Item | Rule |
|---|---|
| One file per | data object, source system, extract (as-of date and sequence) |
| Name | `<OBJECT>_<SOURCE>_<yyyyMMdd>_<nn>.csv` or `.xlsx`, e.g. `F01_EBIX_20270226_01.csv` |
| Format | CSV: UTF-8 without BOM, comma separator, RFC 4180 quoting, one header row with the layout column names. XLSX: first sheet, header in row 1, no merged cells, no formulas |
| Values | Dates `yyyy-MM-dd`; timestamps `yyyy-MM-dd HH:mm:ss` Philippine time; amounts with a dot decimal, 2 decimals, no thousands separator, minus sign for negatives; currency ISO 4217; codes exactly as in legacy (BIBS maps them); blank = no value |
| Control file | `<same name>.ctl.csv` with: object, source system, as-of date and time, extraction time, extracted by, row count, per amount column and currency the sum, hash total of the key column (sum of the numeric part or count of distinct keys, as the layout says), SHA-256 of the data file |
| Delivery | Console upload or the SFTP drop (seam `ExtractInbox`); never e-mail; the file lands in the intake bucket (encrypted, lifecycle 5 days) |
| Layout version | Each object has a layout (`mig_layout`, `mig_layout_column`: name, type, length, mandatory, format, description); a file whose header does not match the current layout version is rejected |

### 5.2 Intake checks (extract status CHECKED or REJECTED)

| Check | Failure |
|---|---|
| SHA-256 of the file = control file value | REJECTED, `MIG_CHECKSUM` |
| Header columns = layout version | REJECTED, `MIG_LAYOUT` (missing / extra columns listed) |
| Parsed rows = control row count | REJECTED, `MIG_ROW_COUNT` |
| Per amount column and currency: sum = control sum | REJECTED, `MIG_CONTROL_TOTAL` (difference shown) |
| Hash total of the key column = control value | REJECTED, `MIG_HASH_TOTAL` |
| Same file (hash) already received for the object | REJECTED, `MIG_DUPLICATE_FILE` |
| As-of date later than the object's previous extract (delta mode) | REJECTED, `MIG_ASOF_ORDER` |

Parsing reuses `bulk/service/CsvParser.java`, `XlsxTableReader.java` and `BulkFileReader.java`; the 5,000-row limit
of the bulk upload does not apply (the intake streams the file into staging in pages of 5,000 rows).

### 5.3 Masking outside production

When the parameter `MIG_ENVIRONMENT_CLASS` is not `PRODUCTION`, the intake masks the columns flagged in
`mig_masking_rule` (object, column, rule) before a row is staged: names and street addresses (FAKE from a seeded list
chosen by keyed HMAC, so the same person gets the same fake name in every file and dedupe still works), TIN / ID /
account / phone numbers (HMAC digits keeping length and check digit), e-mail (`<hmac>@example.invalid`), birth date
(shifted by a keyed offset of up to 30 days). Amounts, codes and dates of transactions are not masked. Unmasked files
never leave production; the masking key is held in the secrets store per environment.

## 6. Staging

| Table | Content |
|---|---|
| `mig_extract` | extract no. `MGX-<yyyy>-nnnnnn`, object, source system, as-of, sequence, file name, file attachment id, SHA-256, layout version, declared and parsed counts, control totals (JSON), status, received by / at, checked at, purged at |
| `mig_stage_row` | extract id, row no., legacy key, raw payload (JSONB, strings as received, masked outside production), mapped payload (JSONB, after code maps), row hash (SHA-256 of the raw payload), status (STAGED / VALID / WARNING / INVALID / LOADED / REJECTED / SKIPPED / ROLLED_BACK / EXCLUDED), batch id, target entity, target id / code, loaded at |
| `mig_issue` | stage row, rule code, severity (ERROR / WARNING), field, value, message, resolution (OPEN / FIXED_AT_SOURCE / MAPPED / WAIVED), resolved by / at, waiver reason |

Staging rows live only until the purge (`MIG_STAGING_PURGE`, section 20): 5 days after the batch that used them is
signed off or rolled back, or after an extract is rejected. The purge deletes the payloads and keeps the counts, hashes
and totals on `mig_extract`, so the reconciliation evidence survives. `mig_stage_row` is indexed on (extract, status)
and (object, legacy key).

## 7. Mapping tables and versioned code maps (BRID 3.1)

| Table | Content |
|---|---|
| `mig_code_map_set` | code (e.g. `INSURER`, `PRODUCT`, `PACKAGE`, `RISK_CODE`, `LINE`, `BRANCH`, `SALES_UNIT`, `AO`, `LOV:<type>`, `MIS:<field>`, `GL_ACCOUNT`, `STATUS:<object>`, `CURRENCY`), target domain, business owner, data steward |
| `mig_code_map_version` | set, version no., status DRAFT / SUBMITTED / APPROVED / SUPERSEDED, submitted by / at, approved by / at, comment. One APPROVED version per set; approval is maker-checker (the approver is the business owner, never the submitter) |
| `mig_code_map_entry` | version, source system, legacy code, legacy description, action MAP / DEFAULT / REJECT / CREATE, target code, remarks |
| `mig_batch_map_version` | batch, set, version used (evidence of which maps loaded which rows) |

Rules:
- A mapped row uses the APPROVED version current when the batch was validated; the version is stored on the batch.
- `CREATE` entries become new reference records through the owning master service (for example `LovService`,
  `InsurerService`) under that master's own maker-checker; the load of dependent objects waits until they are
  authorised ("all required codes exist").
- Unmapped legacy codes found in staging are listed in `MIG-UNMAPPED-CODES` (set, source, legacy code, rows,
  sample keys) and raise `MIG_UNMAPPED` issues; a batch with unmapped codes on an ERROR-level column cannot be
  approved for load (gate G3).
- Map sets are exportable to Excel and importable as a new DRAFT version (the Data Steward edits in Excel).

## 8. Validation and data-quality rules

`mig_rule`: code, object, column(s), kind, parameters, severity, active, description. The rule engine runs in the
batch VALIDATE step on every staged row and records `mig_issue` rows.

| Kind | Example |
|---|---|
| MANDATORY | Client type, name, birth date (individual) or registration name (corporate); invoice no., currency, insurer |
| FORMAT / TYPE | TIN 9-12 digits; dates parse; amounts numeric with 2 decimals; e-mail and PH mobile formats as `crm` validates them |
| LOOKUP | Every coded column has an APPROVED map entry (`MIG_UNMAPPED`) |
| UNIQUE | Legacy key unique in the file; invoice no. unique across source systems (DMQ11) |
| REFERENTIAL | The client of an invoice is loaded (xref) or in the same load; the insurer exists; the parent invoice of an endorsement invoice is in the file or loaded |
| CROSS_FIELD | Expiry after inception; sum of PR components = gross premium; per component open = booked + adjusted - paid - written off (PR) and open = booked + adjusted - remitted (DTIP, commission); shares add to 100 |
| BALANCE | Open balance within 0 and booked (per sign); UPP balance within 0 and amount |
| AGE / PLAUSIBILITY (WARNING) | Invoice older than 5 years still open; UPP older than 2 years; amount above a threshold |
| DUPLICATE_CLIENT | Result of client matching (section 9) |

Object thresholds, parameters per object class: `MIG_MAX_ERROR_RATE_MASTER` (default 0.5 %: master data may be
signed off with waived rows) and `MIG_MAX_ERROR_RATE_FINANCIAL` (default 0: every open item and UPP must load, or be
explicitly excluded by the business owner with a manual entry plan).

## 9. Client matching, dedupe and the client master (BRID 2.1)

Matching runs on the staged, mapped clients of all source systems together and against the clients already in BIBS.

| Key | Rule | Score |
|---|---|---|
| K1 | TIN, digits only (`DuplicateKeys` normalisation) | 100 |
| K2 | ID type and number (`DuplicateKeys.idKey`) | 100 |
| K3 | Last name, first name and birth date, case and space insensitive | 95 |
| K4 | Normalised corporate name (suffixes removed, `DuplicateKeys`) plus registration number when present | 95 (90 without registration no.) |
| K5 | Bank CIF (if BDOI confirms it as a key, DMQ04) | 100 |
| K6 | E-mail, PH mobile (soft keys of `DuplicateCheckService`) | 40 each |
| K7 | Name similarity (trigram) at least 0.85 with the same birth date or address city | 70 |

Decision per pair: score >= `MIG_CLIENT_MATCH_AUTO` (90) merges automatically; `MIG_CLIENT_MATCH_REVIEW` (60) to 89
goes to the Data Steward's review queue; below 60 is a new client. Pairs build clusters (transitive); a cluster
becomes one BIBS client.

Survivorship (`mig_survivorship_rule`: field, rule, source priority): per field the first non-blank value by source
priority (default QPS for contact data, EBIX for billing data), or the most recently updated value where legacy
supplies an update date. Every source value that lost is kept in `mig_client_match` for review.

Load: `ClientLoader` calls the new `crm.service.ClientOnboardingService.registerMigrated(MigratedClient)`, which
creates an ACTIVE client (not a prospect, unlike `ClientBulkHandler`), its party, contacts and addresses, keeps the
KYC status and review date given, skips the onboarding workflow and publishes `ClientRegistered` with `migrated = true`.
`screening/matching/service/ScreeningTriggers.onRegistered` ignores migrated registrations; after the client object
is loaded the cutover plan runs one full screening (`SCR_PERIODIC_SCREENING` with the full-rescreen flag) (DMQ06).
All legacy client keys of a cluster point to the one client code in `mig_key_xref`.

Deltas: until the freeze, new and changed legacy clients arrive as daily delta extracts (umbrella BRD p.43: "EOD
batch - new clients", "client modification reporting"). A changed client that is already loaded is updated through
`ClientService` only for the fields of the delta layout; a new one goes through matching.

## 10. Load through the domain services

- A batch loads one object from one or more checked extracts. The job `MIG_LOAD` processes VALID and WARNING rows in
  chunks of `MIG_CHUNK_SIZE` (500) rows, one transaction per chunk, `MIG_PARTITIONS` (4) partitions in parallel by
  hash of the legacy key. When a chunk fails, its rows are retried one by one so that one bad row fails alone
  (`REJECTED` with the exception code and message).
- Before calling the service the loader looks up `mig_key_xref`. Same legacy key and same row hash: `SKIPPED`.
  Same key, different hash: UPDATE for objects whose loader supports it (clients, headers before freeze), otherwise
  `REJECTED` (`MIG_CHANGED_AFTER_LOAD`).
- The loader runs as the system user `mig-loader`; the batch records the user who approved the load.
- Side effects are controlled at the source: `ClientRegistered.migrated`, `OpsInvoiceBooked.source = MIGRATION`,
  `AccountImported` instead of `AccountStatusChanged`, no notifications for records of origin MIGRATED. Integration
  events published during a batch carry the header `origin=MIGRATION` so outbound consumers can skip them.
- Performance target (planning): 50,000 rows an hour per partition through the services, proven in the dress
  rehearsal (section 17.4).

| Object | Loader | Service called |
|---|---|---|
| R01 | `LovLoader` | `LovService` create + authorise (maker-checker done by the Data Steward in the console) |
| R03-R07 | `SalesOrgLoader`, `InsurerLoader`, `ProductLoader`, `PackageLoader`, `CommissionRateLoader` | the `catalog` services |
| R09 | `PayeeLoader` | the service behind `DISB_PAYEE_MIGRATION` |
| C01-C03 | `ClientLoader` | `ClientOnboardingService.registerMigrated`, `ClientPayoutAccounts` |
| P01 | `PolicyHeaderLoader` | `AccountService.importLegacy` |
| P03 | `RmelCohortLoader` | stores `mig_legacy_candidate` rows; served to Renewal through `LegacyPolicySource` |
| F01 | `LegacyInvoiceLoader` | `opsledger` `LegacyInvoiceIntake.record` + `AccountingEventPublisher` (`MIG_LEGACY_INVOICE_OPENING`) + `OpenItemService.record` |
| F02 | `UppLoader` | `UnappliedService.createMigrated` + `MIG_UPP_OPENING` |
| F03 | `CollectionStateLoader` | `collections` legacy-state service (`CLX_LEGACY_ITEMS`) |
| G01 | `GlOpeningLoader` | `SystemJournalService.post` with `JournalType.OPENING` |
| H01-H02 | `ArchiveLoader` | `mig_archive_record` (owned here) and `attachment` |

## 11. Batches, run log, rerun and rollback

| Table | Content |
|---|---|
| `mig_batch` | batch no. `MGB-<yyyy>-nnnnn`, object, environment class, cutover plan and mock no., mode FULL / DELTA / RERUN, parent batch, extracts, status, counts (staged, valid, warning, invalid, loaded, skipped, rejected, excluded), started / ended, approved to load by / at, signed off at |
| `mig_batch_log` | batch, step (VALIDATE / MATCH / LOAD / RECONCILE / ROLLBACK / PURGE), level, message, counts, time |
| `mig_key_xref` | company, source system, object, legacy key, target entity, target id, target code, batch, row hash, loaded at, rolled back at; unique (company, source system, object, legacy key) |

**Rerun.** Rejected rows are fixed at source (a new extract), fixed by a map change (new approved map version) or
waived. A RERUN batch takes only the rejected and changed rows, loads them, and reconciles as part of the parent.

**Rollback**, in order of preference:
1. Before production: every mock environment is restored from the snapshot taken before the mock.
2. Per batch, before sign-off, for objects whose loader offers a compensation: the loader undoes each xref entry
   whose target has not been changed since the load (version unchanged), newest first, through the owning service
   (e.g. `ClientService.deactivate` of a migrated client with no activity; reversal of an opening event with a negative
   event of the same source reference suffix `:RB`). Rows whose target changed are listed and block the rollback.
   A rollback needs an approver other than the requester (`MIG_ROLLBACK_APPROVE`).
3. During the production cutover, before go / no-go: restore the database snapshot taken at the start of the
   production load (the rollback point, section 17.5).
4. After go-live: no technical rollback; corrections through the normal business functions (fix forward).

## 12. Reconciliation per data object (BRID 1.1b)

`mig_recon_run` (batch or object, as-of, run by, status) and `mig_recon_line` (level, measure, currency, source value,
staged value, target value, difference, tolerance, status MATCHED / BREAK / EXPLAINED, explanation, approved by).

| Level | Measure | Source | Target | Rule |
|---|---|---|---|---|
| L1 Count | rows | control file | received, staged, valid, loaded, skipped, rejected, excluded | received = control; loaded + skipped + rejected + excluded = staged |
| L2 Amount | sum per amount column and currency (open PR by component, DTIP, commission, UPP balance, TB debit and credit) | control totals | staged sum; target sum read from BIBS (ledger components by origin LEGACY and batch, `csh_unapplied`, journals) | difference within `MIG_AMOUNT_TOLERANCE` (0.00) |
| L3 Hash | hash total of the key; SHA-256 per row | control file; staging | xref keys | equal |
| L4 Field | every mapped column of every loaded row | staged mapped payload | `MigrationLoader.readBack` | equal; differences listed per field |
| L5 GL | migration clearing account per branch and currency; legacy control accounts vs legacy sub-ledgers | - | GL balances; ACSL GL-SL recon with context LEGACY | clearing = 0.00; control = sub-ledger |

Reports: `MIG-RECON-SUMMARY` (object x level with status), `MIG-RECON-DETAIL` (lines and breaks), `MIG-REJECTS`
(rejected and invalid rows with issues), `MIG-GL-CLEARING`, all in Excel and PDF. A reconciliation with a BREAK
cannot be signed off until each break is EXPLAINED with an approved explanation.

## 13. Sign-off gates per object (BRID 1.1a, 1.1b)

| Gate | What is signed | By (role) | Evidence |
|---|---|---|---|
| G1 Decision | Class of the object (M / CF / A / X / C) with the four criteria | Data owner (`MIG_DECISION_APPROVE`) after the Data Migration Lead submits | `mig_object_decision` |
| G2 Mapping | Approved map versions used by the object; layout version frozen | Data owner (`MIG_MAPPING_APPROVE`) | `mig_code_map_version` |
| G3 Validation | Error rate within threshold; waivers approved | Data Steward, Data owner for waivers | `MIG-DQ-ISSUES` |
| G4 Load | Approval to load the batch in the environment | Data Migration Lead (`MIG_LOAD_APPROVE`) | batch |
| G5 Reconciliation | L1-L5 matched or explained | Reconciliation approver (`MIG_RECON_SIGNOFF`; Comptrollership for financial objects) | `MIG-RECON-SUMMARY` |
| G6 Object accepted | Business verification of samples on screen | Data owner and Data Migration Lead (`MIG_SIGNOFF`) | `mig_signoff` with attached evidence |
| G7 Go-live | All Day-1 objects at G6; go / no-go criteria met (section 17.5) | Go / no-go board (`MIG_GONOGO_DECIDE`) | `mig_gonogo` |

`mig_signoff`: object, batch, gate, role, user, decision (APPROVED / REJECTED), comment, time, attachment. The same
user cannot sign two gates of the same batch in different roles (segregation of duties), and the maker of a batch
cannot sign G5 or G6 for it.

## 14. Legacy invoice coexistence model (BRID 5-10)

### 14.1 A legacy invoice in the Operations ledger

A legacy invoice is a row of `ops_invoice` with origin LEGACY. It is created by the new
`opsledger.service.LegacyInvoiceIntake.record(LegacyInvoice)`, not by the booking feed, because there is no booked
invoice in BIBS (`InvoiceLedgerWriter.record` reads `BookingQueryService.byNo` and would fail).

**New columns on `ops_invoice`** (migration `V1086`, section 24):

| Column | Value for a legacy invoice | For a BIBS invoice |
|---|---|---|
| `origin` | LEGACY | BIBS (default) |
| `ledger_context` | LEGACY | NEW, or LEGACY when the invoice's root is a legacy invoice (endorsement invoices of legacy policies, section 14.4 H) |
| `source_system` | EBIX / QPS | null |
| `legacy_invoice_no` | the legacy number | null |
| `legacy_ref` | cover no. and version, or the legacy policy reference | null |
| `migration_batch_no` | the loading batch | null |
| `feed_source` | MIGRATION (new value) | EVENT / REPLAY |

Keys: `invoice_no` = the legacy invoice number when it is unique across source systems, else
`<SOURCE>-<legacy no.>` (DMQ11); `legacy_invoice_no` always keeps the original, and both are searchable. `arn` = the
ARN of the migrated header (P01) when there is one, else `LGY-<source>-<cover no.>` (the column is mandatory);
`account_id` = the header account or null; `parent_invoice_no` / `root_invoice_no` = the legacy family when legacy
endorsement invoices are separate invoices.

**Opening positions.** For each component, the intake posts ledger movements in the same transaction:

| Movement | Bucket | Amount |
|---|---|---|
| `BOOKED` (source module MIGRATION) | BOOKED | original booked amount |
| `LEGACY_ADJUSTED` (new) | ADJUSTED | net of legacy adjustments kept on the same invoice (if any) |
| `LEGACY_PAID` (new) | APPLIED | paid before cutover (PR components, PR2307) |
| `LEGACY_REMITTED` (new) | REMITTED | settled with the insurer before cutover (DTIP, commission, VAT on commission, WTAX) |
| `LEGACY_WRITTEN_OFF` (new) | WRITTEN_OFF | written off or DP-reversed before cutover |

The component balance then equals the open balance at cutover, and the check constraint of `ops_invoice_component`
(`balance = booked + adjusted - applied + reversed - remitted - written_off`) holds. Because paid amounts sit in the
APPLIED bucket, remittance treats them as paid AR (`remittance/service/LedgerPositions.position` uses
`netApplied()`), and because remitted amounts sit in REMITTED they are not remitted twice. Payment and remittance
statuses are then computed by the ledger as for any invoice. If legacy can only give open balances (DMQ12), the
intake runs in "open-balance mode": BOOKED = open balance and no LEGACY_* movements; the original values then exist
only in the frozen snapshot.

**Frozen snapshot.** `ops_invoice_origin_snapshot` (invoice, header fields, components with original / paid /
remitted / open, shares, taken at, batch) is written once by the intake and never updated. It is the "original value"
of BRID 10.1 and of the invoice 360 legacy block.

**Events.** The intake publishes `OpsInvoiceBooked` with `source = MIGRATION`. Collections picks the invoice up in its
normal refresh (it reads the ledger); `prodrecon/service/ReconBookingListener` ignores MIGRATION; Renewal ignores it
(legacy renewal candidates come through `LegacyPolicySource`).

**Invoice 360.** `Invoice360Service.view` stops requiring a booked invoice: booking references are empty for origin
LEGACY, and a legacy block shows the source system, legacy number, legacy reference, migration batch and the
snapshot. Lists and the 360 header show a LEGACY badge.

### 14.2 Legacy sub-ledgers and the GL

The BRD asks for legacy Premium Receivable, Commission Receivable, DTIP and UPP sub-ledgers (BRID 5-8). In BIBS a
sub-ledger is the set of party postings on a control account plus the operational ledger behind it (ACSL GL-SL
reconciliation, `acsl_glsl_control`). The design keeps **separate legacy control accounts** so the legacy positions
run off visibly, and routes postings to them by **amount component**, not by a new event:

| Legacy component | Replaces (new) | Proposed legacy control account (demo chart) | Party |
|---|---|---|---|
| `LG_PR_BASIC`, `LG_PR_DST`, `LG_PR_PTX_VAT`, `LG_PR_LGT`, `LG_PR_FST`, `LG_PR_OTHER` | `PR_*` | 1215.01-.06 Premium Receivable - Legacy | client |
| `LG_PR2307` | `PR2307` | 1216 PR 2307 - Legacy | client |
| `LG_DTIP` | `DTIP` | 2212 Due to Insurers - Legacy | insurer |
| `LG_COMMISSION`, `LG_COMMISSION_VAT` | `COMMISSION`, `COMMISSION_VAT` | 1221 Commission Receivable - Legacy | insurer |
| `LG_UNREALIZED` , `LG_DEFERRED_VAT` | `UNREALIZED`, `DEFERRED_VAT` (realisation lines) | 2222 Unrealized Commission - Legacy, 2223 Deferred Output VAT - Legacy | insurer |
| `LG_APPLIED`, `LG_AMOUNT` (UPP side) | `APPLIED`, `AMOUNT` | 2206 Unapplied Collections - Legacy | client |
| `CLEARING` | - | 1999 Migration Clearing | none |

- A shared helper `opsledger.domain.LedgerContext` (NEW with prefix "", LEGACY with prefix "LG_") gives
  `component(String base)`. Every posting helper asks the invoice (or the UPP item) for its context:
  `CashieringPosting.prComponent`, `ApplicationService` amounts, `DispositionExecutor`, `CwtPostings`,
  `RemittancePostings`, `DpPostings`, adjustment `LedgerEffects`, booking `BookingEvents`.
- Comptrollership adds the `LG_` lines to the existing rules of each event (maker-checker on `acc_rule`); the engine
  needs no change because absent components produce no lines. The real account codes come from Comptrollership
  (DMQ18); the demo chart gets the accounts above (demo V1982).
- **ACSL GL-SL reconciliation**: `acsl_glsl_control` gets `ledger_context` (ANY / NEW / LEGACY) and `GlSlQueries`
  filters `ops_invoice.ledger_context` in the OPS_LEDGER source, so 1215 reconciles to the legacy invoices and 1210 to
  the new ones (V1087).
- **Sub-ledger open items**: for parity with booking (`booking/service/BookingPosting.java`, which records client PR,
  insurer DTIP and commission open items), the loader records open items of the open balances with document types
  `LEGACY_PREMIUM`, `LEGACY_DTIP`, `LEGACY_COMMISSION`, document date = legacy invoice date, due date = legacy due
  date, so FRBS reports on `sl_open_item` (e.g. `FRBS-GAP`) include them. Observation for the Operations owner:
  Operations postings do not settle booking's open items today, for new or legacy invoices.
- **Ageing**: GL schedules age FIFO on the posting value date (`finreport/service/ScheduleQueries.java`), so legacy PR
  on 1215 ages from the cutover date. The operational ageing (Collections, `CSH-AR-OUTSTANDING`) uses the invoice
  date and is correct. Whether the GL schedule must age legacy PR by invoice date is DMQ18.

**Opening entries** (value date = cutover date, published by the loaders):

| Event | When | Entry (demo rules) |
|---|---|---|
| `MIG_LEGACY_INVOICE_OPENING` (source ref `MIG:INV:<invoice>`) | F01, per invoice | Dr 1215.x open PR by component (client); Dr 1216 open PR2307 (client); Cr 2212 open DTIP (insurer); Dr 1221 open commission and VAT (insurer); Cr 2222 unrealised commission and Cr 2223 deferred VAT still open (DMQ13); balancing line to 1999 Migration Clearing |
| `MIG_UPP_OPENING` (`MIG:UPP:<ref>`) | F02, per item | Dr 1999 / Cr 2206 (client) |
| GL opening trial balance (system journal type OPENING, source `MIGRATION`, reference `MIG-TB-<asof>`) | G01, per branch and currency | Every balance-sheet account of the legacy TB mapped through `GL_ACCOUNT`; the lines of the legacy control accounts that F01 and F02 build in detail are mapped to 1999 instead; P&L balances follow DMQ18 (year-start cutover recommended) |

**Control:** after F01, F02 and G01, 1999 Migration Clearing is 0.00 per branch and currency (L5, alert
`MIG_CLEARING_NOT_ZERO`). A non-zero balance means the detail and the trial balance disagree; it is a go / no-go
criterion.

### 14.3 Legacy UPP

`csh_unapplied` (V766) gets `ledger_context`, `source_system`, `legacy_ar_no`, `legacy_ar_date`, `match_refs`
(comma-separated invoice / cover / PN / bank references) and origin MIGRATED. `UnappliedService.createMigrated`
creates the item in the stage mapped from the legacy status (UNAPPLIED, MONITORING, FOR_APPROVAL; a legacy disposition
in progress keeps its type and details). No BIBS AR is issued for it (DMQ14); the legacy AR number is shown. The
item's money is on 2206 through `MIG_UPP_OPENING`.

### 14.4 Flows on legacy invoices, per built module

**A. Cashiering: OTC and autopay (BRID 6.1, 6.2).** The payment matcher reads `ops_invoice` by invoice number, ARN,
policy or PN (`PaymentMatcher.invoices`), so a legacy invoice number keyed at the counter or carried in a payment file
(including the Direct Credit `EBIX_RefNo`) finds the legacy invoice; the matcher also tries `legacy_invoice_no`. The
AR is issued as today (`OPS_AR_RECEIPT`, Dr bank / Cr 2205 unapplied collections new), and the application posts
`OPS_PAYMENT_APPLY` with `APPLIED` (new cash) and `LG_PR_*` (legacy invoice): Dr 2205 / Cr 1215.x. The Cash Receipts
Book (`TAX-BOOK-CRB`) lists the receipt as for any other.

**B. UPP automatch rerun across legacy and new (BRID 5.2, 6.3).** `AutomatchService.run` selects origins NO_MATCH,
PREBOOKED **and MIGRATED**; for an item without a payment row it matches on `match_refs`. `applyOldestFirst` spreads
the balance over the matched invoices, legacy or new; each application posts with the UPP side of the item's context
and the invoice side of the invoice's context:

| UPP | Invoice | `OPS_PAYMENT_APPLY` components | Entry |
|---|---|---|---|
| legacy | legacy | `LG_APPLIED`, `LG_PR_*` | Dr 2206 / Cr 1215.x |
| legacy | new | `LG_APPLIED`, `PR_*` | Dr 2206 / Cr 1210.x |
| new | legacy | `APPLIED`, `LG_PR_*` | Dr 2205 / Cr 1215.x |
| new | new | `APPLIED`, `PR_*` | Dr 2205 / Cr 1210.x (today) |

Commission realisation lines on collection follow the invoice context (`LG_UNREALIZED` / `LG_DEFERRED_VAT` for a
legacy invoice). Rerun schedule: after every payment upload and hourly (`PAYMENT_AUTOMATCH`, existing).

**C. UPP dispositions (BRID 5.3, 5.4, 6.4).** APPLY_OTHER_INVOICE and DST_APPLICATION as B. REFUND:
`OPS_UNAPPLIED_REFUND` with `LG_AMOUNT` (Dr 2206 / Cr 2216 refund payable) and the payment request to Disbursement as
today; the disbursement pays through the Cash Disbursements Book.

**D. Reclassification to other income with top-management approval (BRID 5.5).** New disposition type
`RECLASS_OTHER_INCOME` (action OTHER_INCOME, requires approval). The Cashiering User selects items (filters: age,
amount, origin) into a reclassification batch `csh_income_reclass_batch` (`UIR-<yyyy>-nnnn`, lines with item, balance,
age, reason). Workflow `OPS_UPP_INCOME_RECLASS`: DRAFT (`CASH_UPP_INCOME_REQUEST`) -> FOR_TL_APPROVAL
(`CASH_DISPOSITION_APPROVE`) -> FOR_TOP_MANAGEMENT (`CASH_UPP_INCOME_APPROVE`, role `TOP_MANAGEMENT_APPROVER`, through
My Approvals) -> EXECUTED; return with reason at each approval. On execution, per item `OPS_UNAPPLIED_TO_INCOME`
(`UIR:<batch>:<item>`): Dr 2206 (legacy) or 2205 (new) / Cr 4190 Other Income - Unclaimed Collections (account per
DMQ16); the item closes; reversal only by a new approved batch of type REVERSAL.

**E. DPPR batch reversal (BRID 7.1).** `commission` gets `cmr_dppr_batch` / `cmr_dppr_batch_line` (V786), workflow
`OPS_DPPR_REVERSAL` (request `LEGACY_REVERSAL_REQUEST`, approve `LEGACY_REVERSAL_APPROVE`, maker never approves).
Lines come from an upload (template: invoice no., amount, reason) or from the Collections "DP PR for Reversal" tags of
legacy invoices. Each line in its own transaction: DP_REVERSAL movements on PR and DTIP as today, and for legacy
invoices `OPS_DP_PR_REVERSAL` **always** posts, whatever `DP_PR_REVERSAL_POSTING` says (legacy PR is in the GL
through the opening entry): Dr 2212 / Cr 1215.x; the commission receivable treatment (DP commission billing or
reversal against 1221) follows DMQ19. Run report with posted and failed lines.

**F. PR2307 batch reversal (BRID 7.2).** `cashiering` gets `csh_pr2307_reversal_batch` / lines (V766), same workflow
pattern (`OPS_PR2307_REVERSAL`). Legacy PR2307 balances were loaded on the PR2307 component; the run posts
`OPS_CWT_DTIP_OFFSET` with `LG_DTIP`, `LG_PR2307` (Dr 2212 / Cr 1216), or first `OPS_CWT_RECLASS` when the balance
is still on PR (Dr 1216 / Cr 1215.x); commission receivable effect per DMQ20.

**G. Remittance (BRID 8.1).** No extraction change: legacy invoices with paid AR (from `LEGACY_PAID` or later
applications) and open DTIP are extracted with the new ones. `RemittancePostings` builds `OPS_REMITTANCE` per line with
the invoice's context: Dr 2212 (paid AR part) + Dr 1602 CWT / Cr 1221 (commission and VAT) / Cr 2211 due to insurer
for disbursement. The commission OR per settlement batch is issued through `ReceiptIssuer` as today; the schedule
shows the legacy invoice number and the source system; reports get an "invoice origin" filter.

**H. Endorsements on legacy invoices (BRID 9.1-9.3).** Precondition: the policy header is an account (P01).
- `booking` declares `booking.service.port.LegacyInvoiceSource`
  (`Optional<LegacyOriginal> original(long companyId, String arn, LocalDate effectiveDate)`), implemented by
  `opsledger` (`OpsLegacyInvoiceSource`) from the legacy invoice and its snapshot: invoice no., policy year dates,
  components, commission terms, WTAX rate, shares, currency, DP and CWT flags.
- `EndorsementPostingService.originalOf` falls back to the port when no booked original exists for the policy year.
  The endorsement invoice is a BIBS invoice (BI- number, ENDORSEMENT_PLUS / ENDORSEMENT_MINUS / CANCELLATION) with
  `parent_invoice_no` = the legacy invoice and `ledger_context` = LEGACY (V873 adds the column to `bkg_invoice`);
  `BookingEvents` builds `BROKER_BOOKING` with `LG_` components, so the endorsement lands on the legacy control
  accounts. `InvoiceBooked` carries the context and the opsledger writer inherits it from the root.
- The service invoice (ADJID.014) is issued as today; for a decrease the credit refers to the legacy service invoice
  number held on the snapshot (`ServiceInvoiceService` accepts an external original reference; DMQ22).
- Adjustment (`EndorsementRequestService`, `RecomputeService`, `PremiumDeltas`) accepts a legacy invoice whose ARN is
  a migrated account; recompute starts from the legacy original. Negative paths (re-application through
  `PaymentReapplier`, AR Insurer set-up `OPS_AR_INSURER_SETUP`) use the context components.
- Non-financial endorsements (9.3) use the existing workflow and change the account; no posting.
- Renewal basis (9.2, 9.3): the migrated account's premium, sum insured and data are updated by the posted endorsement
  as for any BIBS account, and Renewal reads the account.

**I. Production reconciliation (BRID 10.1).** `ProductionExtractService` excludes origin LEGACY (legacy production was
reconciled in legacy). New report `PRC-LEGACY-CHANGES`: for each legacy invoice changed in the period, one line per
change (endorsement invoice of the family, correction, DP or PR2307 reversal, write-off, minimal balance, non-financial
change from the audit trail; payments and remittances optional, DMQ23) with field / component, original (snapshot),
updated (current), delta, change reference, date and user; filters period, insurer, source system, change type; Excel
and PDF; permission `RECON_PROCESS`.

**J. Collections.** Legacy invoices above the threshold become worklist items through the normal refresh
(`WorklistRefreshService` reads the ledger); `CLX_INVOICE_NO_PATTERN` already accepts `I########`. The item and the
account view show the LEGACY badge. `CollectionStateLoader` (F03) loads open dispositions, promises and assignments
through a new collections service `LegacyItemStateService` (the designed `CLX_LEGACY_ITEMS`, V1007). DP PR and PR2307
"for reversal" tags of legacy invoices feed E and F.

**K. Commission.** DP billing and DP commission collection on legacy invoices post with the context
(`OPS_DP_COMMISSION_COLLECT`: Cr 1221). Incentive runs (No Touch, Top Up, Motor Mania) exclude legacy invoices unless
BDOI decides otherwise (parameter `CMR_INCENTIVE_INCLUDE_LEGACY`, default false).

**L. Minimal balances and write-offs.** The minimal balance sweep (CSHID.016) and the 10-100 write-off file (ADJID.026)
apply to legacy invoices with `LG_` components.

### 14.5 Change list per built module

| Module | Contract change | Files (main) | Flyway | Size | BRID |
|---|---|---|---|---|---|
| `opsledger` | `InvoiceOrigin` (BIBS / LEGACY), `LedgerContext` (NEW / LEGACY, `component()`), `FeedSource.MIGRATION`, `MovementType` `LEGACY_PAID` / `LEGACY_REMITTED` / `LEGACY_ADJUSTED` / `LEGACY_WRITTEN_OFF`; `OpsInvoice.legacy(...)` factory and getters; new `LegacyInvoiceIntake` with record `LegacyInvoice`; `ops_invoice_origin_snapshot`; context inherited from the root in `InvoiceLedgerWriter`; `Invoice360Service` without booking for LEGACY; `InvoiceLedgerQueryService.findByLegacyNo`; `OpsLegacyInvoiceSource` implements the booking port; DTOs with origin, context, source system, legacy no.; LEGACY badge in `features/operations` | `opsledger/domain/*`, `opsledger/service/LegacyInvoiceIntake.java` (new), `InvoiceLedgerWriter.java`, `Invoice360Service.java`, `opsledger/api/dto/*` | V1086 (opsledger range V760-V763 is full) | M | 5-10 |
| `cashiering` | Origin MIGRATED; `UnappliedService.createMigrated`; `csh_unapplied` legacy columns; automatch on `match_refs`; context components in `CashieringPosting`, `ApplicationService`, `DispositionExecutor`, `CwtPostings`, `MinimalBalanceService`; disposition `RECLASS_OTHER_INCOME` with `IncomeReclassService`, batch tables, workflow and approval source; PR2307 legacy reversal batch; permissions `CASH_UPP_INCOME_REQUEST`, `CASH_UPP_INCOME_APPROVE`; reports `CSH-UPP-LEGACY`, `CSH-UPP-INCOME-RECLASS`, origin filter on CSH reports | `cashiering/domain/Unapplied.java`, `CashCodes.java`, `cashiering/service/*` | V766 | M | 5.1-5.5, 6.1-6.4, 7.2 |
| `remittance` | Context components in `RemittancePostings`; legacy invoice no. and source on schedules; origin filter on REM reports | `remittance/service/RemittancePostings.java`, `remittance/report/*` | none | S | 8.1 |
| `commission` | DPPR legacy batch (`cmr_dppr_batch`, lines, workflow `OPS_DPPR_REVERSAL`, handler `DPPR_LEGACY_REVERSAL`); `DpPostings` always posts for LEGACY and uses context components; incentive exclusion parameter | `commission/service/DpPostings.java`, new `DpprReversalService.java` | V786 | M | 7.1 |
| `adjustment` | Accept LEGACY invoices with a migrated account; recompute from the legacy original; context components in `LedgerEffects`; origin column on ADJ reports | `adjustment/service/EndorsementRequestService.java`, `RecomputeService.java`, `PremiumDeltas.java`, `LedgerEffects.java` | none | M | 9.1-9.3 |
| `booking` | Port `LegacyInvoiceSource`; `EndorsementPostingService.originalOf` fallback; `bkg_invoice.ledger_context`; `BookingEvents` `LG_` components; `InvoiceBooked.ledgerContext`; `ServiceInvoiceService` external original reference | `booking/service/port/LegacyInvoiceSource.java` (new), `EndorsementPostingService.java`, `BookingEvents.java`, `ServiceInvoiceService.java` | V873 | M | 9.1-9.3 |
| `account` | `AccountService.importLegacy(ImportedAccount)`; `acc_account.origin` (MIGRATED), `legacy_ref`, `source_system`; event `AccountImported`; `AccountSearch` by legacy reference | `account/service/AccountService.java`, `AccountSearch.java` | V823 (after BT0 V822) | M | 4.1 |
| `crm` | `ClientOnboardingService.registerMigrated(MigratedClient)`; `ClientRegistered.migrated`; `ClientService.updateMigrated` for deltas | `crm/service/ClientOnboardingService.java`, `ClientRegistered.java` | V803 (origin column) | M | 2.1 |
| `screening` | `ScreeningTriggers.onRegistered` ignores migrated registrations | `screening/matching/service/ScreeningTriggers.java` | none | S | 2.1 |
| `prodrecon` | Exclude LEGACY from the production extract; ignore MIGRATION in `ReconBookingListener`; report `PRC-LEGACY-CHANGES` | `prodrecon/service/ProductionExtractService.java`, `ReconBookingListener.java`, new `prodrecon/report/LegacyChangesReport.java` | none | M | 10.1 |
| `collections` | `LegacyItemStateService` (designed `CLX_LEGACY_ITEMS`); LEGACY badge; origin filter on CLX reports | `collections/worklist/**`, new `collections/legacy/service/LegacyItemStateService.java` | V1007 | S | CQ07 |
| `acsl` | `acsl_glsl_control.ledger_context`; filter in `GlSlQueries.OPS_LEDGER_SQL` | `acsl/service/GlSlQueries.java`, `GlSlReconciliationService.java` | V1087 (range V890-V899 is full) | S | 1.1b |
| `finreport` | Optional: age legacy PR by invoice date (DMQ18) | `finreport/service/ScheduleQueries.java` | - | S | - |
| `renewal` (designed) | `LegacyPolicySource` implemented by `migration` (`MigratedPolicySource`) from P01 / P03; the bulk handler stays as fallback | RENEWAL_DESIGN 2.2 | none | S | 12.1 |
| `csf` (designed) | `LegacyAccountLookup` implemented by `migration` (`XrefLegacyAccountLookup`) | CUSTOMER_SERVICING_DESIGN 2 | none | S | 11.1 |
| `brokerclaims` | None now; V1025 stays held for CLQ14 / DMQ30 | - | V1025 (Claims) | - | - |

## 15. In-force policy headers and the renewal-driven transition (BRID 4.1, 12.1)

**Header.** `AccountService.importLegacy(ImportedAccount)` creates an account with origin MIGRATED, status BOOKED,
`legacy_ref` and `source_system`, client from the xref, product / line / risk code, insurer and shares, policy no.,
inception and expiry, sum insured, currency, payment arrangement (DP), PN numbers, AO, unit, branch, business type
(NEW_BUSINESS / RENEWAL, BT0). It creates no quotation, placement, issuance or invoice, and publishes `AccountImported`.
Account search finds it by ARN, policy no., legacy reference and client. Legacy invoices of the policy link to it
(`ops_invoice.arn`, `account_id`).

**RMEL cohorts.** A cohort is an expiry month. At go-live (T), the cohorts with expiry up to T + 140 days (the Renewal
lead time, `RNW_EXTRACTION_LEAD_DAYS`) have already been extracted in legacy. Proposed transition model (DMQ26):

| Cohort | Treatment |
|---|---|
| Expiry before T | Renewed or lapsed in legacy; renewals booked in legacy before the freeze; nothing carried except open items |
| Expiry T to T + 140 days (RMEL already extracted in legacy) | Carried forward (P03): each candidate with its disposition, handler and status loaded as a `mig_legacy_candidate` and served to Renewal through `LegacyPolicySource`; Renewal creates candidates with source LEGACY and continues the process in BIBS |
| Expiry after T + 140 days | Extracted by BIBS: `RNW_EXTRACTION` asks `LegacyPolicySource` for migrated headers expiring at business date + lead days |

A legacy candidate renews on the new-business path pre-filled from the header (Renewal risk 7), because the header
carries no BIBS rating data; the renewal account has `renewal_of_ref` = the legacy reference, so the run-off tracker
can link it.

**Run-off tracker.** `mig_runoff_cohort` (company, expiry month, source system, headers in force at T and premium,
renewed in BIBS, not renewed, lapsed, still open) refreshed monthly by `MIG_RUNOFF_SNAPSHOT` from the renewal
candidates and accounts; report `MIG-RUNOFF`; the console shows the curve of legacy in-force by month.

## 16. Legacy read-only and archive inquiry (BRID 11.1)

| Mechanism | When | What BIBS does |
|---|---|---|
| Read-only legacy | From the freeze until decommissioning (DMQ24) | Stores the legacy link per system (parameter `MIG_LEGACY_LINK_<SYSTEM>`); the legacy system logs access itself (BDOI IT); BIBS's cross-reference lets users find the legacy key |
| BIBS archive | Loaded before a legacy system is decommissioned (objects C04, P02, G02, H01, H02) | `mig_archive_record` and documents, the Legacy Inquiry screen, `mig_access_log` |

`mig_archive_record`: source system, record type (CLIENT, POLICY, INVOICE, RECEIPT, REMITTANCE, ENDORSEMENT, CLAIM,
GL_JOURNAL, RENEWAL_ADVICE, LETTER, OTHER), legacy key, client key and name, policy / cover no., invoice no., dates
(document, from, to), currency and amount, status, summary (JSONB of the legacy columns with their labels), batch,
row hash. Documents: `attachment` records of type `LEGACY_DOCUMENT` linked to the archive record, SHA-256 kept.
Indexes on client key, policy no., invoice no., legacy key and dates.

**Legacy Inquiry** (`/legacy-inquiry`, permission `LEGACY_INQUIRY_VIEW`): search by client name or key, policy / cover
no., invoice no., receipt no., claim no., date range and record type; read-only detail with the labelled legacy
columns and documents; export to Excel up to `MIG_ARCHIVE_EXPORT_MAX_ROWS` (1,000) with `LEGACY_INQUIRY_EXPORT`; a
reason is required per session when `MIG_LEGACY_ACCESS_REASON_REQUIRED` is true.

**Access log** `mig_access_log`: user, time, source address, action (SEARCH / VIEW / DOWNLOAD / EXPORT), criteria,
record keys, result count, reason. Append-only (no update or delete grant). Report `MIG-ACCESS-LOG` for Compliance
(`LEGACY_ACCESS_LOG_VIEW`); alert `MIG_LEGACY_ACCESS_UNUSUAL` when a user exports more than
`MIG_ACCESS_EXPORT_ALERT_ROWS` in a day. Archive records and access logs follow the retention rules
(`nba_retention_rule` record types `LEGACY_ARCHIVE`, `LEGACY_ACCESS_LOG`; umbrella BRD p.45: 5 years online, 15 years
archive).

## 17. Cutover and coexistence (BRID 12.1)

### 17.1 Cutover plan in the console

`mig_cutover_plan` (name, kind MOCK / DRESS_REHEARSAL / PRODUCTION, mock no., environment, go-live date, freeze
start / end, status), `mig_cutover_task` (plan, sequence, phase, task, owner role, depends on, planned start / end,
actual start / end, status, evidence attachment), `mig_gonogo_criterion` and `mig_gonogo_decision`. The runbook is a
generated export of the plan (Excel and Word).

### 17.2 Calendar (T = go-live, first business day of a month after a legacy month-end close, DMQ25)

| When | Step | Objects / checks |
|---|---|---|
| T-16 weeks | Object register and decisions (G1); layouts and the data requirements workbook issued to BDOI | all |
| T-14 weeks | First full extracts; profiling; draft code maps | all M and CF |
| T-12 weeks | **Mock 1** in SIT (masked): reference data, clients, headers | R, C, P01 |
| T-9 weeks | **Mock 2** in SIT (masked): all objects end to end, reconciliation L1-L5 | all |
| T-6 weeks | **Mock 3** = UAT load (masked); business verification on screens; UAT runs on migrated data | all |
| T-3 weeks | **Dress rehearsal** on the production-sized environment, full-volume extract, timed against the window, rollback rehearsed | all |
| T-2 weeks | Production pre-load of reference data and clients; daily client deltas from then on | R, C |
| T-1 week | Code map freeze; legacy reference-data change freeze (new codes only through change control and a map version) | R |
| T-3 days (Fri) | Legacy business freeze after the legacy EOD (22:00); legacy switched to read-only; last client delta | - |
| T-2 days (Sat) | Final extracts after EOD: headers, open invoices, UPP, collection state, RMEL cohorts, GL TB; intake checks; database snapshot (rollback point); loads in load order | P, F, G |
| T-1 day (Sun) | Reconciliation, sign-offs G5 / G6, screening run, business smoke test; go / no-go at 18:00 | all |
| T (Mon) | BIBS open 08:00; hypercare starts | - |
| T to T+4 weeks | Hypercare: daily legacy sub-ledger vs GL, clearing 0.00, automatch results, remittance extracts with legacy invoices, exception queues; defect triage twice a day | - |
| First month-end | First close with legacy control accounts; ACSL recon LEGACY | - |
| Run-off | Monthly run-off snapshot; archive loads; decommissioning review per legacy system | H |

### 17.3 Freeze windows and delta loads

- Reference data: pre-loaded at T-2 weeks; legacy code changes after the map freeze need a change request that adds a
  map version and a delta.
- Clients: pre-loaded at T-2 weeks, daily DELTA batches (new and changed clients) until the freeze.
- Open items, UPP, headers, collection state, GL TB: loaded only after the freeze (a single FULL load), because they
  change every day.
- No delta after go-live: legacy is read-only.

### 17.4 Mock runs and dress rehearsal

Each mock is a cutover plan of kind MOCK with the full task list. Exit criteria of a mock: all objects loaded, L1-L5
reconciled, defects logged, timings recorded per object. The dress rehearsal must complete within the window with at
least 20 % margin and must rehearse the rollback (restore of the snapshot).

### 17.5 Go / no-go criteria and rollback point

| # | Criterion | Threshold |
|---|---|---|
| 1 | Day-1 objects at G6 | 100 % |
| 2 | Count reconciliation per object | loaded + skipped + rejected + excluded = staged; received = control |
| 3 | Financial rejects (F01, F02, G01) | 0, or each excluded item approved by the owner with a manual-entry plan |
| 4 | Amount reconciliation of open items, UPP and TB | 0.00 per currency |
| 5 | Migration clearing account | 0.00 per branch and currency |
| 6 | Legacy control accounts vs legacy sub-ledgers (ACSL, context LEGACY) | 0.00 |
| 7 | Client review queue | empty |
| 8 | Business smoke test (sample of legacy invoices: OTC payment, automatch, remittance extract preview, invoice 360) | passed |
| 9 | Rollback point (database snapshot) | taken and verified |
| 10 | Hypercare roster and support channels | in place |

**Rollback point:** a database snapshot at the start of the production load. **Fallback:** until the go / no-go
decision, restore the snapshot and reopen legacy for update. After go-live, the point of no return is the end of the
first business day (DMQ32); fallback after that is not offered, and issues are fixed forward.

### 17.6 Legacy decommissioning

Two milestones, each with a checklist in the console (`mig_decommission_item`: system, criterion, evidence, status,
signed by):

| Milestone | Criteria |
|---|---|
| Legacy system decommissioned | Archive objects of the system loaded and reconciled (counts and hash totals against legacy); Legacy Inquiry verified by Audit / Compliance; no open inquiry or claim that needs the legacy system; the last legacy-booked policy expired plus the claims reporting tail (DMQ27); legacy access logs exported to the archive; retention obligations covered; sign-off by the system owner, Compliance and Comptrollership |
| Legacy context closed in BIBS | No open legacy invoice or legacy UPP (paid, reversed, written off or reclassified); legacy control accounts at 0.00; migration clearing at 0.00; Comptrollership decides whether the legacy accounts are closed (`coa` freeze) |

## 18. Security

### 18.1 Permissions (added to `security.domain.Permission` in DM0; granted in V1080 unless noted)

| Permission | Used for |
|---|---|
| `MIG_VIEW` | Migration Console, read-only |
| `MIG_OBJECT_MANAGE`, `MIG_DECISION_APPROVE` | Data object register and decisions (G1) |
| `MIG_MAPPING_EDIT`, `MIG_MAPPING_APPROVE` | Code maps, layouts, rules, masking rules (G2) |
| `MIG_INTAKE` | Upload extracts, run intake checks |
| `MIG_DQ_RESOLVE`, `MIG_DQ_WAIVE` | Resolve issues; waive rows (G3) |
| `MIG_MATCH_DECIDE` | Client review queue |
| `MIG_LOAD_RUN`, `MIG_LOAD_APPROVE` | Run validation and loads; approve a load (G4) |
| `MIG_ROLLBACK_REQUEST`, `MIG_ROLLBACK_APPROVE` | Batch rollback |
| `MIG_RECON_SIGNOFF` | Reconciliation sign-off (G5), break explanations approval |
| `MIG_SIGNOFF` | Object acceptance (G6) |
| `MIG_CUTOVER_MANAGE`, `MIG_GONOGO_DECIDE` | Cutover plan and tasks; go / no-go (G7) |
| `LEGACY_INQUIRY_VIEW`, `LEGACY_INQUIRY_EXPORT`, `LEGACY_ACCESS_LOG_VIEW` | Legacy Inquiry and its access log |
| `CASH_UPP_INCOME_REQUEST`, `CASH_UPP_INCOME_APPROVE` (V766) | Reclassification of UPP to income |
| `LEGACY_REVERSAL_REQUEST`, `LEGACY_REVERSAL_APPROVE` (V766, V786) | DPPR and PR2307 legacy batches |

### 18.2 Roles and demo users (V1080; demo users in V1980, password `Brokerverse@2026`)

| Role | Persona | Key permissions | Demo user |
|---|---|---|---|
| `DATA_MIGRATION_LEAD` | Data Migration Lead (BRID 1.1) | MIG_VIEW, MIG_OBJECT_MANAGE, MIG_LOAD_APPROVE, MIG_SIGNOFF, MIG_CUTOVER_MANAGE, MIG_ROLLBACK_REQUEST | `miglead` |
| `DATA_STEWARD` | Data Steward (BRID 3.1) | MIG_VIEW, MIG_MAPPING_EDIT, MIG_DQ_RESOLVE, MIG_MATCH_DECIDE | `migsteward` |
| `DATA_OWNER` | Business owner of an object (approvers of the BRD) | MIG_VIEW, MIG_DECISION_APPROVE, MIG_MAPPING_APPROVE, MIG_DQ_WAIVE, MIG_SIGNOFF | `migowner` |
| `MIGRATION_OPERATOR` | IT operator running the loads | MIG_VIEW, MIG_INTAKE, MIG_LOAD_RUN | `migops` |
| `MIGRATION_RECON_APPROVER` | Reconciliation approver (Comptrollership for financial objects) | MIG_VIEW, MIG_RECON_SIGNOFF, MIG_ROLLBACK_APPROVE | `migrecon` |
| `MIGRATION_GONOGO` | Go / no-go board (Program Manager and heads) | MIG_VIEW, MIG_GONOGO_DECIDE | `miggonogo` |
| `LEGACY_INQUIRY` | Audit / Compliance User (BRID 11.1) | LEGACY_INQUIRY_VIEW, LEGACY_INQUIRY_EXPORT | `legacyaudit` |
| `LEGACY_ACCESS_REVIEWER` | Compliance reviewer | LEGACY_ACCESS_LOG_VIEW | `legacyrev` |
| `TOP_MANAGEMENT_APPROVER` | Finance Approver / top management (BRID 5.5) | CASH_UPP_INCOME_APPROVE | `topmgmt` |

Existing roles gain: `CASHIER` + CASH_UPP_INCOME_REQUEST, LEGACY_REVERSAL_REQUEST; `CASHIER_TL` + LEGACY_REVERSAL_APPROVE;
`COMMREC_TL` + LEGACY_REVERSAL_APPROVE; `COMPTROLLERSHIP` + MIG_VIEW.

Segregation of duties (fixed): the maker of a decision, map version, load, rollback or reclassification batch never
approves it; the operator who ran a batch cannot sign G5 or G6 for it; one person cannot sign two gates of one batch.

### 18.3 Data protection (hosting appendix)

- Non-production: masked data only (section 5.3); unmasked extracts are never copied out of production.
- Staging and files purged within 5 days of batch sign-off (`MIG_STAGING_RETENTION_DAYS` = 5, job
  `MIG_STAGING_PURGE`, S3 lifecycle on the intake bucket); alert `MIG_STAGING_PURGE_OVERDUE` if a signed-off batch
  still has payloads after the limit.
- Access to the console, staging and intake bucket only for migration roles, and only from the Philippines (network
  allow-list / VPN at the infrastructure layer; AWS ap-southeast-1).
- Encryption at rest (KMS) and in transit (TLS, SFTP); every console action audited.

## 19. Accounting events and GL entries

Event types are seeded in V1080 (`acc_event_type`); demo rules in V1982. Real accounts come from Comptrollership
(DMQ18). All events carry party, cost centre and business line, and the BOOK rate for foreign currency (DMQ35).

| # | Transaction | Event (source ref) | Default entry (demo chart) |
|---|---|---|---|
| M1 | Legacy invoice opening | `MIG_LEGACY_INVOICE_OPENING` (`MIG:INV:<invoice>`) | Dr 1215.x (client) open PR; Dr 1216 open PR2307; Cr 2212 (insurer) open DTIP; Dr 1221 (insurer) open commission and VAT; Cr 2222 / 2223 unrealised commission and deferred VAT still open; balance to 1999 |
| M2 | Legacy UPP opening | `MIG_UPP_OPENING` (`MIG:UPP:<ref>`) | Dr 1999 / Cr 2206 (client) |
| M3 | GL opening TB | System journal OPENING (`MIG-TB-<asof>-<branch>-<ccy>`) | Mapped balances; legacy control lines to 1999 |
| M4 | Rollback of an opening (pre-sign-off only) | same event, negative, `...:RB` | Reverses M1 / M2 |
| O2 | Payment applied (legacy invoice or legacy UPP) | `OPS_PAYMENT_APPLY` with `LG_` components | Section 14.4 B |
| O8 | UPP refund (legacy) | `OPS_UNAPPLIED_REFUND` with `LG_AMOUNT` | Dr 2206 / Cr 2216 |
| O8b | UPP reclass to other income | `OPS_UNAPPLIED_TO_INCOME` (new) (`UIR:<batch>:<item>`) | Dr 2206 or 2205 / Cr 4190 |
| O4 | PR2307 legacy reversal | `OPS_CWT_DTIP_OFFSET` with `LG_DTIP`, `LG_PR2307` | Dr 2212 / Cr 1216 |
| O22 | DPPR legacy reversal | `OPS_DP_PR_REVERSAL` with `LG_` (always posted for LEGACY) | Dr 2212 / Cr 1215.x; commission per DMQ19 |
| O12 | Remittance of a legacy invoice | `OPS_REMITTANCE` with `LG_DTIP`, `LG_COMMISSION`, `LG_COMMISSION_VAT` | Dr 2212 + Dr 1602 / Cr 1221 / Cr 2211 |
| O19 | Endorsement of a legacy invoice | `BROKER_BOOKING` with `LG_` components | As booking row 0 on the legacy accounts |
| O18 | Negative endorsement after remittance | `OPS_AR_INSURER_SETUP` with `LG_DTIP` | Dr 1225 / Cr 2212 |
| O21 | Write-off / minimal balance on a legacy invoice | `OPS_WRITE_OFF`, `OPS_MINIMAL_BALANCE_REVERSAL` with `LG_` | Dr 6510 / Cr 1215.x |

## 20. Jobs, parameters and alerts

| ManagedJob | Default cron (UTC) | Purpose |
|---|---|---|
| `MIG_INTAKE_SCAN` | manual (every 15 min when `SftpFolderInbox` is configured) | Pick up files from the inbox, run the intake checks |
| `MIG_VALIDATE` | on demand per batch | Map, validate, match |
| `MIG_LOAD` | on demand per batch (lock per object) | Load |
| `MIG_RECONCILE` | after each load, and on demand | L1-L5 |
| `MIG_STAGING_PURGE` | `0 0 18 * * *` (02:00 PHT) | Purge staging payloads and intake files beyond the retention |
| `MIG_CLIENT_DELTA` | manual; daily 16:30 UTC (00:30 PHT) during the pre-load window | Load the day's client delta extract |
| `MIG_RUNOFF_SNAPSHOT` | `0 30 17 1 * *` (01:30 PHT on the 1st) | Run-off tracker |
| `MIG_ACCESS_LOG_DIGEST` | `0 0 0 1 * *` (08:00 PHT on the 1st) | Monthly access-log report to the reviewers |

Parameters (`sys_parameter`, category DATA_MIGRATION): `MIG_ENVIRONMENT_CLASS` (NON_PRODUCTION), `MIG_CUTOVER_DATE`,
`MIG_STAGING_RETENTION_DAYS` (5), `MIG_CHUNK_SIZE` (500), `MIG_PARTITIONS` (4), `MIG_AMOUNT_TOLERANCE` (0.00),
`MIG_MAX_ERROR_RATE_MASTER` (0.5), `MIG_MAX_ERROR_RATE_FINANCIAL` (0), `MIG_CLIENT_MATCH_AUTO` (90),
`MIG_CLIENT_MATCH_REVIEW` (60), `MIG_INVOICE_NO_COLLISION_PREFIX` (true), `MIG_ARCHIVE_EXPORT_MAX_ROWS` (1000),
`MIG_ACCESS_EXPORT_ALERT_ROWS` (5000), `MIG_LEGACY_ACCESS_REASON_REQUIRED` (true), `MIG_LEGACY_LINK_EBIX`,
`MIG_LEGACY_LINK_QPS` (empty), `CMR_INCENTIVE_INCLUDE_LEGACY` (false, V786).

Alerts (`alt_exception_code`): `MIG_EXTRACT_REJECTED`, `MIG_LOAD_FAILED`, `MIG_RECON_BREAK`, `MIG_UNMAPPED`,
`MIG_CLEARING_NOT_ZERO`, `MIG_STAGING_PURGE_OVERDUE`, `MIG_LEGACY_ACCESS_UNUSUAL`.

LOV types (V1080): `MIG_SOURCE_SYSTEM` (EBIX, QPS, ISYS, EXCEL, CMS), `MIG_OBJECT_CATEGORY` (REFERENCE, CLIENT, POLICY,
OPEN_ITEM, GL, HISTORY), `MIG_BREAK_REASON`, `MIG_WAIVER_REASON`, `MIG_ACCESS_REASON`, `LEGACY_RECORD_TYPE`,
`UPP_INCOME_REASON` (V766).

Workflows (V1080 unless noted): `MIG_OBJECT_DECISION`, `MIG_MAP_VERSION`, `MIG_BATCH_ROLLBACK`;
`OPS_UPP_INCOME_RECLASS`, `OPS_PR2307_REVERSAL` (V766); `OPS_DPPR_REVERSAL` (V786).

## 21. Reports (`ReportDefinition`; category "Data Migration" unless noted)

| Code | Content | BRID |
|---|---|---|
| `MIG-OBJECT-REGISTER` | Objects with class, criteria, owners, status per environment | 1.1a |
| `MIG-DECISIONS` | Decision history with approvals | 1.1a |
| `MIG-UNMAPPED-CODES` | Unmapped legacy codes per set and source | 3.1 |
| `MIG-MAP-VERSIONS` | Map versions, entries, approvals, batches that used them | 3.1 |
| `MIG-DQ-ISSUES` | Issues per rule, severity, resolution | 1.1b |
| `MIG-REJECTS` | Rejected and invalid rows with messages | 1.1b |
| `MIG-BATCH-LOG` | Batches with timings and counts | 1.1b |
| `MIG-RECON-SUMMARY`, `MIG-RECON-DETAIL` | Reconciliation L1-L5, breaks and explanations | 1.1b |
| `MIG-GL-CLEARING` | 1999 balance per branch and currency; legacy control vs sub-ledger | 1.1b |
| `MIG-CLIENT-MATCH` | Clusters, scores, decisions, survivors | 2.1 |
| `MIG-SIGNOFF-STATUS` | Gates per object and batch | 1.1a |
| `MIG-CUTOVER-STATUS`, `MIG-GONOGO` | Tasks and criteria | 12.1 |
| `MIG-RUNOFF` | Legacy in-force by cohort and outcome | 12.1 |
| `MIG-LEGACY-POSITIONS` | Open legacy invoices and UPP by component, insurer, client, age | 5-8 |
| `MIG-ACCESS-LOG` | Legacy inquiry access log | 11.1 |
| `PRC-LEGACY-CHANGES` (Operations) | Original, updated and delta per legacy invoice change | 10.1 |
| `CSH-UPP-LEGACY`, `CSH-UPP-INCOME-RECLASS` (Operations) | Legacy UPP ageing; reclassification batches | 5.1, 5.5 |

## 22. Screens (sidebar section **Data Migration**, and Legacy Inquiry under **Inquiry**)

| Screen | Path | Permission | Content |
|---|---|---|---|
| Migration Home | `/migration` | MIG_VIEW | Tiles: objects by status and gate, batches running or failed, open breaks, unmapped codes, clearing balance, next cutover tasks, run-off curve |
| Data Objects | `/migration/objects` | MIG_VIEW | Register, decision form with the four criteria, decision history, dependencies, gate matrix per environment |
| Code Maps | `/migration/maps` | MIG_VIEW | Sets, versions (diff between versions), entries grid with inline edit in DRAFT, Excel import / export, unmapped codes |
| Layouts and Rules | `/migration/layouts` | MIG_VIEW | Layout versions and columns (drives the data requirements workbook), rules, masking rules |
| Extracts | `/migration/extracts` | MIG_INTAKE | Upload (file + control file), intake check results, status, purge date |
| Batches | `/migration/batches` | MIG_VIEW | Batch page: steps timeline, counts tiles, issues grid, rejects download, rerun, rollback request, run log |
| Client Matching | `/migration/matching` | MIG_MATCH_DECIDE | Review queue: side-by-side legacy records, keys that matched, score, decision (merge / new), survivorship preview |
| Reconciliation | `/migration/reconciliation` | MIG_VIEW | Object x level matrix; break list with explanation and approval; export |
| Sign-off | `/migration/signoff` | MIG_VIEW | Gates matrix per object; sign with comment and evidence |
| Cutover | `/migration/cutover` | MIG_VIEW | Plans (mocks, rehearsal, production), task board with owners and timings, go / no-go criteria with measured values, decision |
| Run-off and Decommissioning | `/migration/runoff` | MIG_VIEW | Cohort table and curve; decommissioning checklists |
| Legacy Inquiry | `/legacy-inquiry` | LEGACY_INQUIRY_VIEW | Search, record detail, documents, export |
| Legacy Access Log | `/legacy-inquiry/access-log` | LEGACY_ACCESS_LOG_VIEW | Log with filters, export |

Operations screens: LEGACY badge and source system on Invoice 360, invoice lists, Unapplied workbench and Collections
items; new Cashiering screens "UPP Income Reclassification" and "PR2307 Legacy Reversal"; Commission screen "DPPR
Legacy Reversal".

## 23. Integrations to park (seam only)

| Item | Seam | Question |
|---|---|---|
| SFTP drop of legacy extracts | `ExtractInbox` (default console upload) | DMQ28 |
| Automated legacy delta extracts | Same inbox; job `MIG_CLIENT_DELTA` manual | DMQ28 |
| Legacy read-only application link | Parameter per system | DMQ24 |
| Bulk transfer of legacy documents | `ArchiveDocumentStore` (default: documents uploaded with their archive extract) | DMQ24 |
| Masking key management | `MaskingProvider`, key from the secrets store | DMQ31 |
| Write-back of contact changes to legacy | Not needed after the freeze (legacy read-only); CSF `ContactSyncGateway` stays NOT_CONFIGURED | CSQ01 |

## 24. Flyway plan

Range **V1080-V1089** (schema) and **V1980-V1989** (demo). A second block is **not** needed: owner changes go into
the owners' free versions; only the two owners whose ranges are full (opsledger V760-V763, Accounting V890-V899) use
versions of this range, as Collections did with V1006 for cashiering.

| Version | Owner | Content |
|---|---|---|
| V1080 `migration_foundation` | migration | Permissions grants, roles, LOV types, parameters, workflows `MIG_*`, alert codes, event types `MIG_*`, report registrations |
| V1081 `migration_objects_maps` | migration | `mig_data_object`, `mig_object_decision`, `mig_code_map_set` / `_version` / `_entry`, `mig_layout`, `mig_layout_column`, `mig_rule`, `mig_masking_rule`, `mig_survivorship_rule` |
| V1082 `migration_intake_staging` | migration | `mig_extract`, `mig_stage_row`, `mig_issue` |
| V1083 `migration_batches` | migration | `mig_batch`, `mig_batch_log`, `mig_batch_map_version`, `mig_key_xref`, `mig_client_match`, `mig_legacy_candidate` |
| V1084 `migration_recon_cutover` | migration | `mig_recon_run`, `mig_recon_line`, `mig_signoff`, `mig_cutover_plan`, `mig_cutover_task`, `mig_gonogo_criterion`, `mig_gonogo_decision`, `mig_runoff_cohort`, `mig_decommission_item` |
| V1085 `migration_archive` | migration | `mig_archive_record`, `mig_access_log` (append-only), document type `LEGACY_DOCUMENT`, retention record types |
| V1086 `opsledger_legacy_invoice` | opsledger (range full) | `ops_invoice` columns of 14.1, `feed_source` MIGRATION, movement types `LEGACY_*`, `ops_invoice_origin_snapshot`, index on `legacy_invoice_no` |
| V1087 `acsl_glsl_ledger_context` | acsl (range full) | `acsl_glsl_control.ledger_context` |
| V1088-V1089 | - | Reserve for a further owner whose range is full |
| V766 `cashiering_legacy` | cashiering | `csh_unapplied` legacy columns, origin MIGRATED, disposition `RECLASS_OTHER_INCOME`, reclass and PR2307 batch tables, workflows, event `OPS_UNAPPLIED_TO_INCOME`, permissions |
| V786 `commission_dppr_legacy` | commission | DPPR batch tables, workflow, parameter |
| V803 `crm_client_origin` | crm | `crm_client.origin` |
| V823 `account_legacy_header` | account | `acc_account.origin`, `legacy_ref`, `source_system` (after BT0 V822) |
| V873 `booking_ledger_context` | booking | `bkg_invoice.ledger_context` |
| V1007 `collections_legacy_items` | collections | legacy state tables of `CLX_LEGACY_ITEMS` |
| V1980 `demo_migration_users` | migration demo | Demo users and roles of 18.2 |
| V1981 `demo_migration_setup` | migration demo | Demo object register, map sets with approved versions, layouts, rules |
| V1982 `demo_migration_gl` | migration demo | Demo accounts 1215.x, 1216, 1221, 2206, 2212, 2222, 2223, 1999, 4190 and the `LG_` lines on the demo rules of the Operations and booking events |

No foreign keys from `mig_*` to V8xx tables (targets are held as plain values), so V1080-V1085 are safe on a fresh
database. V1086 alters V761 tables and V1087 V896 tables, which run earlier. The demo storyline (legacy invoices, UPP,
a mock batch) runs as a Java demo runner (`migration.demo.LegacyMigrationDemo`) that loads sample extracts from
`backend/src/main/resources/demo/migration/` through the real pipeline, never by SQL into business tables.

## 25. Build-wave plan

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **DM0** (1 agent) | Foundation | Module skeleton; V1080-V1084; object register and decisions; layouts; intake with checks and masking; staging and purge; code maps and unmapped report; rule engine; batch framework (loader SPI, chunking, run log, rerun, rollback framework); reconciliation L1-L4; sign-off gates; console shell and screens of sections 22 (except archive, cutover, run-off); shared files (`Permission.java`, `navigation/modules.ts`, help registry, `application.yml`, CONFIGURATION.md); demo V1980-V1981; a reference-data loader (R01) as the first end-to-end object | `migration/**` (except `archive`, `cutover`), V1080-V1084, V1980-V1981, `features/migration/**` | R01 extract -> load -> recon -> sign-off in the console; ITs green |
| **DM1-A** | Legacy invoice contract | V1086 and all `opsledger` changes of 14.5; `LegacyInvoiceLoader` (F01) with M1 and open items; `GlOpeningLoader` (G01, M3); L5 reconciliation; V1087 and the ACSL filter; V1982 demo GL | `opsledger/**` (legacy parts), `acsl/service/GlSlQueries.java`, `migration/load/**/LegacyInvoiceLoader*`, `GlOpeningLoader*` | Legacy invoice visible in Invoice 360; clearing 0.00 after F01 + G01 |
| **DM1-B** | Cashiering, commission, remittance | V766, V786; context components in all Operations postings; migrated UPP (F02, M2); automatch on `match_refs`; income reclass batch with top-management approval; PR2307 and DPPR legacy batches; remittance postings and schedules; CSH reports | `cashiering/**`, `commission/**`, `remittance/**` (legacy parts), `migration/load/**/UppLoader*` | OTC and autopay on a legacy invoice; mixed UPP automatch; remittance batch with legacy lines; reclass executed after two approvals |
| **DM1-C** | Clients, reference, headers | V803, V823; `registerMigrated`, screening skip, `importLegacy`; loaders R03-R09, C01-C03, P01; client matching and review queue; xref search; `LegacyAccountLookup` | `crm/**`, `account/**`, `screening/matching/service/ScreeningTriggers.java` (legacy parts), `migration/matching/**`, loaders | 10,000 synthetic clients with duplicates matched, reviewed and loaded; headers searchable |
| **DM2-A** | Endorsements, prod recon, collections | V873, V1007; booking port and fallback; adjustment on legacy invoices; `PRC-LEGACY-CHANGES`; production extract exclusion; `LegacyItemStateService` and loader F03 | `booking/**`, `adjustment/**`, `prodrecon/**`, `collections/legacy/**` | Positive, negative and non-financial endorsement on a legacy invoice with legacy postings; change report shows original, updated, delta |
| **DM2-B** | Cutover, run-off, archive | V1085; cutover plan, tasks, go / no-go, runbook export; run-off tracker and `LegacyPolicySource` implementation (stub until Renewal R0 exists); archive loader, Legacy Inquiry, access log, retention providers | `migration/cutover/**`, `migration/archive/**`, `features/migration/cutover/**`, `features/migration/inquiry/**` | Mock plan executed in the console; archive search logged |
| **DM3** (1 agent) | Integration and rehearsal tooling | E2E test (extract -> load -> OTC -> automatch -> remittance -> endorsement -> recon -> sign-off); performance harness with 1,000,000 synthetic client rows and 500,000 invoice rows; demo storyline; module guide `docs/modules/MIGRATION.md` | tests, demo, docs | Full `mvn verify` / `npm run verify`; timings recorded |

Rules for parallel work:
- One Flyway range per agent: DM0 V1080-V1084 / V1980-V1981; DM1-A V1086-V1087 / V1982; DM1-B V766, V786; DM1-C V803,
  V823; DM2-A V873, V1007; DM2-B V1085.
- Shared files are edited only in DM0. `LedgerContext` and the `LG_` naming are committed first by DM1-A (first commit)
  so DM1-B and DM2-A build on them; DM1-B starts after that commit.
- Changes in built modules follow the owners' conventions and are reviewed by the module owner; no edits to another
  wave's packages.
- Each wave has its own `*ApiIT` class; port stubs are `@ConditionalOnMissingBean`.
- Not before: Renewal R0 for the real `LegacyPolicySource` wiring; BT0 (V822) before V823.

## 26. Risks

1. **Legacy data cannot give components or paid / remitted splits** (DMQ12). Mitigation: open-balance mode; split rules
   as configuration; early profiling at T-14 weeks.
2. **Clearing account not zero at cutover** (detail and TB disagree). Mitigation: reconciliation in every mock; GL TB
   extracted after the same EOD as the detail; break explanations with Comptrollership before go / no-go.
3. **Duplicate clients merged wrongly.** Mitigation: auto-merge only on hard keys; review queue for the rest; merge
   evidence kept; rollback of client batches before sign-off.
4. **Load time over the window.** Mitigation: pre-load reference data and clients at T-2 weeks; four partitions;
   dress rehearsal with 20 % margin.
5. **Endorsements without headers** (BRID 9 depends on 4.1). Mitigation: decide DMQ09 / DMQ22 before Mock 2.
6. **Real data in test.** Mitigation: masking at intake; unmasked files never leave production; 5-day purge.
7. **Parallel processing in legacy after the freeze.** Mitigation: legacy read-only at the freeze; any late legacy
   transaction is a reconciliation break.

## 27. Inputs required from BDOI

Per data object: the fields are the layout columns (published in the data requirements workbook, generated from
`mig_layout`); "volume" is to be supplied (DMQ28); the sign-off owner signs G1, G2 and G6.

| Object | Source system | BDOI owner (sign-off) | Key fields | Format | Volume | Quality rules (main) | Historic depth | Delta frequency |
|---|---|---|---|---|---|---|---|---|
| R01 LOV / MIS values | QPS, EBIX | Product Owner, MBS | list type, code, description, active | CSV per list | to supply | unique code per list; every value used by a migrated record mapped | current values | none after map freeze |
| R02 Branches | EBIX | Head of Comptrollership | branch code, name, invoicing branch flag | CSV | to supply | all mapped to `org_branch` | current | none |
| R03 Sales organisation | QPS, EBIX | Heads of Retail / Corporate Marketing | unit code, name, unit head, AO code, AO name, user ID, branch | CSV | to supply | AO linked to an active unit; unit head exists | current | weekly until freeze |
| R04 Insurers | QPS, EBIX | Head of Operations | insurer code, name, TIN, address, branches, contact e-mail, status | CSV | to supply | TIN format; mapped or CREATE | current and inactive with open items | none |
| R05 Products / risk codes | QPS, EBIX | Product Owner, MBS | line, cover type, risk code, name, packaged flag, status | CSV | to supply | code fits the line pattern; every code of an open item mapped | current and codes used by in-force policies | none |
| R06 Packages | QPS | Product Owner, MBS; TSU | package code, version, insurers, rates, effective dates | XLSX | to supply | active version per package | active | none |
| R07 Commission rates | QPS, EBIX | Head of Operations | insurer, product, rate, effective dates | CSV | to supply | rate 0-100; no overlap | current | none |
| R08 GL account map | EBIX, ISYS GL | Head of Comptrollership; FRBS / ACSL PO | legacy account, description, BIBS account, legacy control flag | XLSX | to supply | every TB account mapped; legacy controls to 1999 | current chart | none |
| R09 Payees | EBIX / Disbursement | Comptrollership - Disbursement PO | payee code, name, TIN, address, bank, account no., mode | CSV | to supply | as `DISB_PAYEE_MIGRATION` | active payees | none |
| R11 Receipt series | EBIX | Operations - Financial Transactions | branch, kind AR / OR, ATP no., from, to, next no. | CSV | to supply | next no. within range | current | at freeze |
| C01-C02 Clients | QPS, EBIX, CMS | Product Owner, MBS; Heads of Marketing | legacy client no., type, names, birth / registration date, TIN, ID type and no., CIF, e-mail, mobile, addresses, segment, AO, KYC status and dates, last update | CSV | to supply | mandatory identity fields; formats; duplicates resolved | 2020 to present (umbrella p.43), scope per DMQ05 | daily until freeze |
| C03 Payout accounts | EBIX | Comptrollership - Disbursement PO | client no., bank, account no., account name, mode | CSV | to supply | bank code mapped | active | none |
| C04 KYC documents | QPS, shares | Compliance | client no., document type, file, date | files + CSV index | to supply | file readable; checksum | per retention | none |
| P01 Policy headers | QPS, EBIX | Head of Operations | cover / policy no., version, client no., product / risk code, line, insurer(s) and shares, inception, expiry, sum insured, premium, currency, AO, unit, branch, PN nos., DP flag, business type, status | CSV | to supply | expiry after inception; client and product mapped; shares 100 % | in force at T (DMQ09) | at freeze |
| P03 RMEL cohorts | RMEL files, QPS | Heads of Marketing (Renewal) | cover no., expiry, disposition, handler, status, remarks | XLSX | to supply | header exists | cohorts T to T+140 days | at freeze |
| P04 Submitted masterlists | Excel | CBG / Non-CBG Marketing | as `SBM_MIGRATION` (SUBMITTED_POLICIES_DESIGN) | XLSX | to supply | as SP design | all active (SP SQ16) | none |
| P05 EB programmes | EBIX | EB Head | as `EB_PROGRAMME_LOAD` | XLSX | to supply | as EB design | active programmes | none |
| F01 Open legacy invoices | EBIX, QPS | Operations - Financial Transactions; Head of Comptrollership | invoice no., source, kind, parent invoice, cover no. / version, policy no., client no., assured, payor, insurer(s) and shares, currency, booking / inception / expiry / due dates, risk code, line, segment, AO, unit, branch, DP / CWT / incentive flags; per component (basic, DST, PT / VAT, LGT, FST, other, DTIP, commission, VAT, WTAX, PR2307): booked, adjusted, paid, remitted, written off, open; commission realised; last payment date; legacy service invoice no. | CSV + control file | to supply | components add up; open = booked + adjusted - paid / remitted - written off; client, insurer and product mapped; unique invoice no. | every invoice open at T (DMQ10) | at freeze (single load) |
| F02 UPP | EBIX, CMS | Operations - Financial Transactions | UPP ref., AR no. and date, channel, payor, client no., amount, balance, currency, value date, references (invoice, cover, PN, bank), sales unit, legacy status and disposition in progress, remarks | CSV + control file | to supply | balance within 0 and amount; references present | every open UPP at T | at freeze |
| F03 Collection state | CMS | Head of Operations (Collections) | invoice no., disposition, promise date and amount, installment plan, collector, remarks | CSV | to supply | invoice in F01 | open items only (DMQ34) | at freeze |
| F04 Remittance in flight | EBIX | Operations - Remittance | batch no., insurer, invoices, amounts, status, holds with dates | CSV | to supply | invoices in F01 | open at T (DMQ21) | at freeze |
| F06 PDC / pick-ups / refunds | EBIX | Operations - Cashiering | check no., bank, date, amount, client, invoice, status | CSV | to supply | as cashiering rules | open at T (DMQ33) | at freeze |
| F07 Open claims | EBIX, ISYS | Unit Head, Claims | as the Claims migration layout (CLQ14) | CSV | to supply | as Claims design | open at T (DMQ30) | at freeze |
| G01 GL trial balance | EBIX / ISYS GL | Head of Comptrollership | legacy account, branch, currency, debit, credit, base amounts, as-of | XLSX + control file | to supply | balanced per branch and currency; every account mapped | balances at T-1 day EOD | at freeze |
| H01-H02 Archive | EBIX, QPS, ISYS, shares | Audit / Compliance; system owners | record type, keys, dates, amounts, labelled columns, documents | CSV + files | to supply | counts and hash totals per record type against legacy | per retention (DMQ24) | before decommissioning |
