# iNXT BrokerVerse - BDOI Data Migration (BRD-13) Build Design

Status: **proposal for review; not built.** It extends `docs/architecture/BROKING_ARCHITECTURE.md`,
`docs/architecture/OPERATIONS_DESIGN.md` and the Developer Guide, and it does not change them. Requirements baseline:
[`BDOI_DM_BRD_SPEC.md`](../requirements/BDOI_DM_BRD_SPEC.md) (23 requirement rows BRID 1.1a-12.1, fit/gap and questions
DMQ01-DMQ39). Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRID 5.2`.

**Programme inputs of 26-Sep-2026.** The BDOI drop plan and timeline
(`docs/source-documents/BDOI_DROP_PLAN.md`) set the migration calendar: requirements and mapping Sep-Oct 2026, build
Nov 2026-Mar 2027, SIT migration Apr-Jul 2027, UAT migration Aug-Oct 2027, full migration and cut-over Nov 2027-Jan
2028 (performance and penetration test Nov-Dec 2027, ORR / PRR Dec 2027-Jan 2028), go-live January 2028 (DMQ25,
section 17.2). The signed concept paper *Advance Implementation of Renewal Processing* V1.0 (06-Sep-2026; "CP")
proposed an early renewal release by 15-Aug-2027 with the client master and reference data migrated ahead of it. BDOI
decided on 26-Sep-2026 that everything goes live together in January 2028, so the concept paper is **superseded** and
there is one production cut-over (register DCR-240). Of the concept paper, only the mock-load order still applies: the
client master and the renewal reference data are the first objects of every mock, because renewal testing needs them
(CP section VI; section 2, mock-load order).

**BDOI answers of 26-Sep-2026 (DMQ36-DMQ39).** This version applies them:
- **DMQ36 (answered).** Legacy packages are remapped **at sanitation in Renewal**, not at migration intake. Migration
  loads the PACKAGE code map as reference data only (object R06); the Renewal sanitation check `PACKAGE_REMAP` resolves
  the BIBS package per candidate and sends an unmapped package to the Exception bucket, which the Renewal processing
  team works (section 15.2).
- **DMQ37 (answered).** The renewals of the January-May 2028 expiries are **processed in BIBS after go-live**; no
  renewal candidate is carried from legacy. At go-live BIBS extracts every expiry from go-live to 31-May-2028 from the
  migrated in-force headers (P01), prioritised by expiry date with the January expiries flagged urgent. Renewal advices
  already sent by hand before go-live are loaded as a small reference file (object P03, "RA already sent") so that
  they are not sent again (section 15.1).
- **DMQ38 (answered).** The RMEL and the dispositions are kept in Excel today; rejected rows are reviewed by the
  Renewal processing team with a maker and a checker. The Excel intake of the RA-sent file, its rejection report and
  its maker-checker resubmission are in section 15.1.
- **DMQ39 (answered with a recommendation, awaiting Comptrollership confirmation).** Option A: go-live on Monday
  3-Jan-2028 at the year-end boundary with a provisional GL opening trial balance and controlled true-ups of the
  FY2027 closing and audit adjustments (section 17.7). Options B and C are recorded as considered and not recommended.

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
9. **Parked means seam, not simulation** (Operations principle 5). The legacy transport (SFTP), the legacy read-only link
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
| R06 | PACKAGE code map: legacy package and version to the BIBS package version, loaded as reference data for the Renewal sanitation check `PACKAGE_REMAP` (section 15.2). The BIBS packages themselves are maintained by TSU in Product Maintenance (BRD-3) and are not migrated | QPS | M (map only; DMQ36 answered) | Renewal package map through the Renewal service that maintains it | 3.1 | 3 |
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
| P01 | In-force policy headers, with the legacy package code and version as stored in legacy (not remapped). Whatever DMQ09 decides, the scope includes every policy expiring from T to 31-May-2028 and every renewal term booked in legacy that starts after T (section 15.1) | QPS, EBIX | C (BRID 4.1; DMQ09) | `account.service.AccountService.importLegacy` (new) | 4.1 | 7 |
| P02 | Expired policies and their history | QPS, EBIX | A | archive | 11.1 | 16 |
| P03 | Renewal advices already sent by hand before go-live for the expiries T to 31-May-2028 ("RA already sent" reference file, section 15.1) | Excel RA trackers of the Renewal processing team | M (reference; DMQ37, DMQ38 answered) | `mig_ra_sent`, served to Renewal through `renewal.service.port.LegacyPolicySource` (implemented here) | 12.1 | 13 |
| P04 | Submitted-policy masterlists | Excel | M (DMQ30, SP SQ16) | `submitted` `SBM_MIGRATION` service | BRIDSP-33 | 14 |
| P05 | Employee Benefits programmes | EBIX | M (DMQ30) | `eb` `EB_PROGRAMME_LOAD` service | - | 14 |
| F01 | Open legacy invoices: PR outstanding, paid not remitted, commission receivable, PR2307; with components and insurer shares | EBIX, QPS | CF | `opsledger.service.LegacyInvoiceIntake` (new) + opening event | 5-10 | 8 |
| F02 | Unapplied premium payments (UPP) with disposition in progress | EBIX, CMS | CF | `cashiering.service.UnappliedService.createMigrated` (new) + opening event | 5.1 | 9 |
| F03 | Open collection state: dispositions, promises, installment plans, assignments | CMS | CF (DMQ34, CQ07) | `collections` `CLX_LEGACY_ITEMS` service | CQ07 | 10 |
| F04 | Remittance in flight: batches not yet paid, holds, special remittance | EBIX | CF or X (DMQ21) | `remittance` hold / special services | 8.1 | 11 |
| F05 | DP billing in process and DP commission receivable | EBIX, Operations files | CF | part of F01 + `commission` DP list service | 7.1 | 11 |
| F06 | PDCs in the warehouse, check pick-ups, refunds in process | EBIX | CF or X (DMQ33) | `cashiering` PDC and pick-up services | - | 11 |
| F07 | Open claims | EBIX, ISYS | C (DMQ30, CLQ14) | `brokerclaims` (V1025 held) | - | 14 |
| G01 | GL opening trial balance per account, branch and currency: the **provisional** balance-sheet opening from the preliminary December 2027 TB (FY2027 P&L net result to retained earnings), and the legacy TB at each true-up as reconciliation input (section 17.7) | EBIX / ISYS GL | M | `journal.service.SystemJournalService.post` (OPENING) | 5-8 | 12 |
| G03 | FY2027 opening-balance true-ups: legacy closing and audit adjustment journals posted after the freeze (G03) and the open-item detail of their lines on legacy control accounts (G03D) (section 17.7; DMQ39) | EBIX / ISYS GL; Comptrollership adjustment register | M (opening adjustment) | `SystemJournalService.post` (OPENING); `opsledger` `LegacyInvoiceIntake.adjustOpening`, `cashiering` `UnappliedService.adjustMigratedOpening` (new) | 1.1b, 5-8 | 17 (after go-live) |
| G02 | GL history | EBIX / ISYS GL | A | archive | 11.1 | 16 |
| H01 | Closed transactions: paid and remitted invoices, receipts, remittances, endorsements, claims, RAs, letters | EBIX, QPS, ISYS | A | archive (`mig_archive_record`) | 11.1 | 16 (before decommissioning) |
| H02 | Legacy documents (policies, SOAs, ORs, certificates) | file shares, EBIX | A | archive documents (`attachment`) | 11.1 | 16 |

Load order: a lower number loads first; objects with the same number have no dependency on each other. An object
loads only when the objects it depends on are signed off for the same environment (gate G6, section 13).

**Layouts per object.** An extract file holds one layout. Most objects have one layout with the object's code; four
have sub-layouts that are sent as separate files with the same as-of date and joined on the key by the loader: R04
(insurers) and R04B (insurer branches); P01 (policy header) and P01S (insurer shares of the policy); F01 (invoice
header), F01S (insurer shares of the invoice) and F01C (components with their buckets); G03 (true-up journal lines)
and G03D (open-item detail of the lines on legacy control accounts). The file name starts with the
layout code, not the object code (section 5.1). Layouts, fields and control totals are published in the BDOI Data
Requirements Workbook.

**Mock-load order.** Reference data (R01-R07) and the client master (C01-C03) load first in every mock and are
accepted (G6) before any policy, open item or RA-sent file loads. Besides the dependency rule, this gives the SIT and
UAT of the Drop 1 modules, Renewal first, migrated clients and reference data to test with from the first mock, which
is the prerequisite the concept paper states for renewal (CP section VI, p.2; section 17.4).

## 3. Module `migration`

| Item | Value |
|---|---|
| Package | `com.iortatechnxt.brokerverse.migration`, sub-packages `object`, `mapping`, `intake`, `quality`, `load`, `matching`, `recon`, `signoff`, `cutover`, `archive`, `legacy`, `report`, `seed`, each with `domain` / `service` / `api` (the Collections layout) |
| Tables | `mig_*` |
| Depends on | platform (`bulk` parsers, `system` jobs and parameters, `workflow`, `approval`, `audit`, `alert`, `attachment`, `report`, `security`, `organization`, `accounting`, `journal`, `lov`, `party`) and the target modules (`crm`, `catalog`, `account`, `opsledger`, `cashiering`, `collections`, `commission`, `remittance`, `disbursement`, `renewal` port, `csf` port) |
| Depended on by | nobody. Other modules reach migration data only through ports they declare (`renewal.service.port.LegacyPolicySource`, `csf.service.port.LegacyAccountLookup`, `booking.service.port.LegacyInvoiceSource` is implemented by `opsledger`, not here) |
| Flyway | V1080-V1089; seed data for SIT/UAT V1980-V1989 (section 24) |
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
| One file per | layout, source system, extract (as-of date and sequence); an object with sub-layouts (R04 / R04B, P01 / P01S, F01 / F01S / F01C) sends one file per layout with the same as-of date |
| Name | `<LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv` or `.xlsx`, e.g. `F01C_EBIX_20271231_01.csv` (layout code, not object code; the date is the as-of date, nn the sequence of that day) |
| Format | CSV: UTF-8 without BOM, comma separator, RFC 4180 quoting, one header row with the layout column names. XLSX: first sheet, header in row 1, no merged cells, no formulas |
| Values | Dates `yyyy-MM-dd`; timestamps `yyyy-MM-dd HH:mm:ss` Philippine time; amounts with a dot decimal, 2 decimals, no thousands separator, minus sign for negatives; currency ISO 4217; codes exactly as in legacy (BIBS maps them); blank = no value |
| Control file | `<same name>.ctl.csv` with: object, source system, as-of date and time, extraction time, extracted by, row count, per amount column and currency the sum, hash total of the key column (sum of the numeric part or count of distinct keys, as the layout says), SHA-256 of the data file |
| Delivery | Console upload or the SFTP drop (seam `ExtractInbox`); never e-mail; the file lands in the intake bucket (encrypted, lifecycle 5 days) |
| Layout version | Each layout (`mig_layout` with its object; `mig_layout_column`: name, type, length, mandatory, format, description) has versions; a file whose header does not match the current layout version is rejected. A batch of an object takes one checked extract of each of its layouts with the same as-of date |

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
`mig_masking_rule` (object, column, rule) before a row is staged: names and street addresses (replaced by entries of a
fixed list chosen by keyed HMAC, so the same person gets the same substitute name in every file and dedupe still works), TIN / ID /
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
explicitly excluded by the business owner with a manual entry plan). The error rate is (INVALID rows not waived +
waived rows) / (staged rows - EXCLUDED rows): rows the data owner excluded from the batch are neither errors nor part
of the base, so a financial batch whose failing rows are all excluded has a rate of 0.

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
| R03-R05, R07 (R04 with R04B) | `SalesOrgLoader`, `InsurerLoader` (insurer, then its branches), `ProductLoader`, `CommissionRateLoader` | the `catalog` services |
| R09 | `PayeeLoader` | the service behind `DISB_PAYEE_MIGRATION` |
| C01-C03 | `ClientLoader` | `ClientOnboardingService.registerMigrated`, `ClientPayoutAccounts` |
| P01 (P01, P01S) | `PolicyHeaderLoader` | `AccountService.importLegacy` |
| R06 | `PackageMapLoader` | the Renewal service that maintains the package map read by `PACKAGE_REMAP` (section 15.2) |
| P03 | `RaSentLoader` | stores `mig_ra_sent` rows linked to the migrated header (P01); served to Renewal through `LegacyPolicySource` (section 15.1) |
| F01 (F01, F01S, F01C) | `LegacyInvoiceLoader` | `opsledger` `LegacyInvoiceIntake.record` + `AccountingEventPublisher` (`MIG_LEGACY_INVOICE_OPENING`) + `OpenItemService.record` |
| F02 | `UppLoader` | `UnappliedService.createMigrated` + `MIG_UPP_OPENING` |
| F03 | `CollectionStateLoader` | `collections` legacy-state service (`CLX_LEGACY_ITEMS`) |
| G01 | `GlOpeningLoader` | `SystemJournalService.post` with `JournalType.OPENING` (provisional opening); at a true-up, reconciliation input only |
| G03 (G03, G03D) | `TrueUpLoader` | `SystemJournalService.post` with `JournalType.OPENING`; `LegacyInvoiceIntake.adjustOpening`; `UnappliedService.adjustMigratedOpening` (section 17.7) |
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
| L5 GL | migration clearing account per branch and currency, analysed per legacy control account (trial balance line against the opening detail posted to that account); legacy control accounts vs legacy sub-ledgers | trial balance (G01) | opening entries of F01 / F02; GL balances; ACSL GL-SL recon with context LEGACY | clearing = 0.00; per control account TB line = detail; control = sub-ledger |

Reports: `MIG-RECON-SUMMARY` (object x level with status), `MIG-RECON-DETAIL` (lines and breaks), `MIG-REJECTS`
(rejected and invalid rows with issues), `MIG-GL-CLEARING`, all in Excel and PDF. Each FY2027 true-up (object G03)
has its own reconciliation to the legacy trial balance, with cut-off checks, in `MIG-TRUEUP-RECON` (section 17.7). A reconciliation with a BREAK
cannot be signed off until each break is EXPLAINED with an approved explanation.

**Sign of a clearing difference.** The trial balance puts each legacy control-account line on Migration Clearing with
the sign it has in the TB; each opening entry puts the balancing line of its detail on Migration Clearing with the
opposite sign. A difference therefore has the sign of the trial balance. An invoice missing from F01 leaves a
**debit** on Migration Clearing equal to its net receivable position (open PR + open PR2307 + open commission
receivable - open DTIP - unrealised commission - deferred VAT) when that is positive, and a **credit** when it is
negative: for example a credit of the open DTIP less the commission receivable for an invoice paid in full and not
remitted. A UPP item missing from F02 leaves a credit equal to its balance. An unpaid invoice whose receivable and DTIP
are about equal nets to about zero, so `MIG-GL-CLEARING` also compares, per branch, currency and legacy control
account, the TB line with the opening detail posted to that account: the missing invoice then shows as a debit
difference on Premium Receivable - Legacy and a credit difference on DTIP - Legacy even when the net is zero.

## 13. Sign-off gates per object (BRID 1.1a, 1.1b)

| Gate | What is signed | By (role) | Evidence |
|---|---|---|---|
| G1 Decision | Class of the object (M / CF / A / X / C) with the four criteria | Data owner (`MIG_DECISION_APPROVE`) after the Data Migration Lead submits | `mig_object_decision` |
| G2 Mapping | Approved map versions used by the object; layout version frozen | Data owner (`MIG_MAPPING_APPROVE`) | `mig_code_map_version` |
| G3 Validation | Error rate within threshold; waivers approved | Data Steward, Data owner for waivers | `MIG-DQ-ISSUES` |
| G4 Load | Approval to load the batch in the environment | Data Migration Lead (`MIG_LOAD_APPROVE`); for a true-up (G03), the Head of Comptrollership (`MIG_TRUEUP_APPROVE`) after the Comptrollership GL lead prepares it (`MIG_TRUEUP_PREPARE`) | batch; `mig_trueup` |
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
of BRID 10.1 and of the invoice 360 legacy block. A FY2027 true-up on the invoice (section 17.7) is a later change and
appears in `PRC-LEGACY-CHANGES` with change type FY2027 TRUE-UP.

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

`LGC-DTIP`, `LGC-COMM` and `LGC-CLR` are placeholders: Comptrollership assigns the account codes (DMQ18, DCR-197). The first
draft used 1221 and 2212, which the Accounting design already uses for USD commission receivable and USD payable to insurers.

| Legacy component | Replaces (new) | Proposed legacy control account (SIT/UAT chart) | Party |
|---|---|---|---|
| `LG_PR_BASIC`, `LG_PR_DST`, `LG_PR_PTX_VAT`, `LG_PR_LGT`, `LG_PR_FST`, `LG_PR_OTHER` | `PR_*` | 1215.01-.06 Premium Receivable - Legacy | client |
| `LG_PR2307` | `PR2307` | 1216 PR 2307 - Legacy | client |
| `LG_DTIP` | `DTIP` | LGC-DTIP (Due to Insurers - Legacy; code assigned by Comptrollership, DMQ18) | insurer |
| `LG_COMMISSION`, `LG_COMMISSION_VAT` | `COMMISSION`, `COMMISSION_VAT` | LGC-COMM (Commission Receivable - Legacy; code assigned by Comptrollership, DMQ18) | insurer |
| `LG_UNREALIZED` , `LG_DEFERRED_VAT` | `UNREALIZED`, `DEFERRED_VAT` (realisation lines) | 2222 Unrealized Commission - Legacy, 2223 Deferred Output VAT - Legacy | insurer |
| `LG_APPLIED`, `LG_AMOUNT` (UPP side) | `APPLIED`, `AMOUNT` | 2206 Unapplied Collections - Legacy | client |
| `CLEARING` | - | LGC-CLR Migration Clearing | none |

- A shared helper `opsledger.domain.LedgerContext` (NEW with prefix "", LEGACY with prefix "LG_") gives
  `component(String base)`. Every posting helper asks the invoice (or the UPP item) for its context:
  `CashieringPosting.prComponent`, `ApplicationService` amounts, `DispositionExecutor`, `CwtPostings`,
  `RemittancePostings`, `DpPostings`, adjustment `LedgerEffects`, booking `BookingEvents`.
- Comptrollership adds the `LG_` lines to the existing rules of each event (maker-checker on `acc_rule`); the engine
  needs no change because absent components produce no lines. The real account codes come from Comptrollership
  (DMQ18); the SIT/UAT chart gets the accounts above (seed data V1982).
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

**Opening entries** (value date `MIG_OPENING_VALUE_DATE`: 1-Jan-2028, the first day of FY2028 and of the opening period, for the year-end go-live; published by the loaders):

| Event | When | Entry (SIT/UAT rules) |
|---|---|---|
| `MIG_LEGACY_INVOICE_OPENING` (source ref `MIG:INV:<invoice>`) | F01, per invoice | Dr 1215.x open PR by component (client); Dr 1216 open PR2307 (client); Cr LGC-DTIP open DTIP (insurer); Dr LGC-COMM open commission and VAT (insurer); Cr 2222 unrealised commission and Cr 2223 deferred VAT still open (DMQ13); balancing line to LGC-CLR Migration Clearing |
| `MIG_UPP_OPENING` (`MIG:UPP:<ref>`) | F02, per item | Dr LGC-CLR / Cr 2206 (client) |
| Provisional GL opening trial balance (system journal type OPENING, source `MIGRATION`, reference `MIG-TB-<asof>`) | G01, per branch and currency | Every balance-sheet account of the preliminary December 2027 TB mapped through `GL_ACCOUNT`; the lines of the legacy control accounts that F01 and F02 build in detail are mapped to LGC-CLR instead; the FY2027 P&L accounts are not opened: their net result goes to retained earnings (the company's retained earnings account, as `closing/service/YearEndService.java` uses it), so FY2028 P&L starts at zero (DMQ39, section 17.7) |
| Opening-balance adjustment journal (type OPENING, source `MIGRATION`, reference `MIG-TU-<n>-<branch>-<ccy>`) | G03, per true-up, branch and currency, after approval | The FY2027 adjustment lines mapped through `GL_ACCOUNT`; P&L lines to retained earnings; lines on legacy control accounts to LGC-CLR |
| `MIG_LEGACY_POSITION_TRUEUP` (`MIG:TU:<n>:<item>:<component>`) | G03D, per open item and component | The legacy control account of the component against LGC-CLR, with the ledger movement `LEGACY_ADJUSTED` or `LEGACY_WRITTEN_OFF` on the invoice (or the balance change of the legacy UPP) |

**Control:** after F01, F02 and G01, and again after each true-up (G03, G03D), LGC-CLR Migration Clearing is 0.00 per
branch and currency, and per legacy control account the TB line equals the opening detail (L5, section 12; alert `MIG_CLEARING_NOT_ZERO`). A non-zero balance means
the detail and the trial balance disagree; it is a go / no-go criterion.

### 14.3 Legacy UPP

`csh_unapplied` (V766) gets `ledger_context`, `source_system`, `legacy_ar_no`, `legacy_ar_date`, `match_refs`
(comma-separated invoice / cover / PN / bank references) and origin MIGRATED. `UnappliedService.createMigrated`
creates the item in the stage mapped from the legacy status (UNAPPLIED, MONITORING, FOR_APPROVAL; a legacy disposition
in progress keeps its type and details). No BIBS AR is issued for it (DMQ14); the legacy AR number is shown. If BDOI
answers DMQ14 with an acknowledgment, parameter `MIG_UPP_ISSUE_AR` (default false) makes the loader issue a BIBS AR
through `ReceiptIssuer` without a cash posting, referring to the legacy AR number. The item's money is on 2206 through
`MIG_UPP_OPENING`.

### 14.4 Flows on legacy invoices, per built module

**A. Cashiering: OTC and autopay (BRID 6.1, 6.2).** The payment matcher reads `ops_invoice` by invoice number, ARN,
policy or PN (`PaymentMatcher.invoices`), so a legacy invoice number keyed at the counter or carried in a payment file
(including the Direct Credit `EBIX_RefNo`) finds the legacy invoice; the matcher also tries `legacy_invoice_no`. The
matcher recognises a legacy invoice number by the patterns of parameter `MIG_LEGACY_INVOICE_NO_PATTERN` (default
the EBIX pattern `^I\d{8}$`); the QPS pattern is added to it and to `CLX_INVOICE_NO_PATTERN` when DMQ11 gives the QPS
format. Until then a payment for a QPS invoice is matched by ARN, policy number or PN, or stays unapplied. The
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
through the opening entry): Dr LGC-DTIP / Cr 1215.x; the commission receivable treatment (DP commission billing or
reversal against LGC-COMM) follows DMQ19. Run report with posted and failed lines.

**F. PR2307 batch reversal (BRID 7.2).** `cashiering` gets `csh_pr2307_reversal_batch` / lines (V766), same workflow
pattern (`OPS_PR2307_REVERSAL`). Legacy PR2307 balances were loaded on the PR2307 component; the run posts
`OPS_CWT_DTIP_OFFSET` with `LG_DTIP`, `LG_PR2307` (Dr LGC-DTIP / Cr 1216), or first `OPS_CWT_RECLASS` when the balance
is still on PR (Dr 1216 / Cr 1215.x); commission receivable effect per DMQ20.

**G. Remittance (BRID 8.1).** No extraction change: legacy invoices with paid AR (from `LEGACY_PAID` or later
applications) and open DTIP are extracted with the new ones. `RemittancePostings` builds `OPS_REMITTANCE` per line with
the invoice's context: Dr LGC-DTIP (paid AR part) + Dr 1602 CWT / Cr LGC-COMM (commission and VAT) / Cr 2211 due to insurer
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
(`OPS_DP_COMMISSION_COLLECT`: Cr LGC-COMM). Incentive runs (No Touch, Top Up, Motor Mania) exclude legacy invoices unless
BDOI decides otherwise (parameter `CMR_INCENTIVE_INCLUDE_LEGACY`, default false).

**L. Minimal balances and write-offs.** The minimal balance sweep (CSHID.016) and the 10-100 write-off file (ADJID.026)
apply to legacy invoices with `LG_` components.

### 14.5 Change list per built module

| Module | Contract change | Files (main) | Flyway | Size | BRID |
|---|---|---|---|---|---|
| `opsledger` | `InvoiceOrigin` (BIBS / LEGACY), `LedgerContext` (NEW / LEGACY, `component()`), `FeedSource.MIGRATION`, `MovementType` `LEGACY_PAID` / `LEGACY_REMITTED` / `LEGACY_ADJUSTED` / `LEGACY_WRITTEN_OFF`; `OpsInvoice.legacy(...)` factory and getters; new `LegacyInvoiceIntake` with record `LegacyInvoice`; `ops_invoice_origin_snapshot`; context inherited from the root in `InvoiceLedgerWriter`; `Invoice360Service` without booking for LEGACY; `InvoiceLedgerQueryService.findByLegacyNo`; `LegacyInvoiceIntake.adjustOpening` for the true-ups (movements `LEGACY_ADJUSTED` / `LEGACY_WRITTEN_OFF`, event `MIG_LEGACY_POSITION_TRUEUP`, section 17.7); `OpsLegacyInvoiceSource` implements the booking port; DTOs with origin, context, source system, legacy no.; LEGACY badge in `features/operations` | `opsledger/domain/*`, `opsledger/service/LegacyInvoiceIntake.java` (new), `InvoiceLedgerWriter.java`, `Invoice360Service.java`, `opsledger/api/dto/*` | V1086 (opsledger range V760-V763 is full) | M | 5-10 |
| `cashiering` | Origin MIGRATED; `UnappliedService.createMigrated` and `adjustMigratedOpening` (true-ups, section 17.7); `csh_unapplied` legacy columns; automatch on `match_refs`; context components in `CashieringPosting`, `ApplicationService`, `DispositionExecutor`, `CwtPostings`, `MinimalBalanceService`; disposition `RECLASS_OTHER_INCOME` with `IncomeReclassService`, batch tables, workflow and approval source; PR2307 legacy reversal batch; permissions `CASH_UPP_INCOME_REQUEST`, `CASH_UPP_INCOME_APPROVE`; reports `CSH-UPP-LEGACY`, `CSH-UPP-INCOME-RECLASS`, origin filter on CSH reports | `cashiering/domain/Unapplied.java`, `CashCodes.java`, `cashiering/service/*` | V766 | M | 5.1-5.5, 6.1-6.4, 7.2 |
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
| `journal`, `period` | None. True-ups post as `JournalType.OPENING`, which is privileged and may post into a CLOSING or REOPENED period (`journal/domain/JournalType.java`); a closed opening period is reopened with the existing `PeriodService.reopen` (reason required, audited) (section 17.7) | - | none | - | 1.1b |
| `renewal` (designed) | `LegacyPolicySource` implemented by `migration` (`MigratedPolicySource`) from P01 / P03, with `goLiveCandidates` for the go-live extraction of the expiries T to 31-May-2028 (priority by expiry date, URGENT flag for January expiries, RA already sent recorded from P03 and not sent again); the daily extraction skips headers that already have a candidate; the bulk handler stays as fallback; the package map loaded by R06 and the sanitation check `PACKAGE_REMAP` (unmapped package to the Exception bucket, worked by the Renewal processing team) (sections 15.1, 15.2) | RENEWAL_DESIGN 2.2, 8 | none | S | 12.1 |
| `csf` (designed) | `LegacyAccountLookup` implemented by `migration` (`XrefLegacyAccountLookup`) | CUSTOMER_SERVICING_DESIGN 2 | none | S | 11.1 |
| `brokerclaims` | None now; V1025 stays held for CLQ14 / DMQ30 | - | V1025 (Claims) | - | - |

## 15. In-force policy headers and the renewal-driven transition (BRID 4.1, 12.1)

**Header.** `AccountService.importLegacy(ImportedAccount)` creates an account with origin MIGRATED, status BOOKED,
`legacy_ref` and `source_system`, client from the xref, product / line / risk code, the **legacy package code and
version as stored in legacy** (`legacy_package_code`, `legacy_package_version`; not remapped at intake, DMQ36, section
15.2), insurer and shares, policy no., inception and expiry, sum insured, currency, payment arrangement (DP), PN
numbers, AO, unit, branch, business type (NEW_BUSINESS / RENEWAL, BT0). It creates no quotation, placement, issuance or
invoice, and publishes `AccountImported`. Account search finds it by ARN, policy no., legacy reference and client.
Legacy invoices of the policy link to it (`ops_invoice.arn`, `account_id`).

**P01 scope needed by the renewal transition.** Whatever DMQ09 decides for the other policies, P01 must include:
(a) every policy in force at T whose term expires from T to 31-May-2028, because the go-live extraction (section 15.1)
builds the January-May 2028 renewals from these headers only; and (b) every renewal term booked in legacy before the
freeze that starts on or after T, so that the go-live extraction sees that the expiring term is already renewed. A
header missing from P01 is a renewal that BIBS never extracts; the extraction check of section 15.1 and the P01
reconciliation against the legacy in-force list prove that none is missing.

**Transition by expiry month** (DMQ26; DMQ37 answered on 26-Sep-2026):

| Expiry | Treatment |
|---|---|
| Before T (up to 2-Jan-2028) | Renewed or lapsed in legacy; a renewal whose new term starts before T is placed and booked in legacy before the freeze and migrates as an in-force header (P01) with its open invoice (F01) |
| T to 31-May-2028 | Processed in BIBS after go-live. No renewal candidate is carried from legacy. At T the go-live extraction creates a candidate for every migrated header expiring in the window that was not renewed in legacy, prioritised by expiry date, with the January expiries flagged urgent; renewal advices already sent by hand before go-live are recorded from P03 and not sent again (section 15.1) |
| From 1-Jun-2028 | Extracted by BIBS on the normal lead time: `RNW_EXTRACTION` asks `LegacyPolicySource` for migrated headers expiring at business date + `RNW_EXTRACTION_LEAD_DAYS` (140). The daily runs from T + 1 reach the expiries of 23-31 May 2028 first; those headers already have a candidate from the go-live extraction and are skipped. The first new expiry, 1-Jun-2028, is extracted on 13-Jan-2028 (T + 10) |

A legacy candidate renews on the new-business path pre-filled from the header (Renewal risk 7), because the header
carries no BIBS rating data; a packaged policy whose legacy package the sanitation check `PACKAGE_REMAP` resolves
(section 15.2) can renew as is on the resolved package version. The renewal account has `renewal_of_ref` = the legacy
reference, so the run-off tracker can link it.

### 15.1 Renewals of the January to May 2028 expiries (DMQ37, DMQ38)

**Go-live extraction.** At T, before business opens (Cutover Runbook, T 04:00), Renewal runs one extraction through
`LegacyPolicySource.goLiveCandidates(companyId, T, to)`, which `MigratedPolicySource` serves:
- every migrated header (P01) whose expiry is from T to `MIG_GOLIVE_RENEWAL_TO` (default 31-May-2028), except a header
  whose cover has a later P01 term starting on its expiry (renewed in legacy);
- each candidate is created with source LEGACY at the first stage, like any extracted candidate, and goes through the
  normal Renewal sanitation. Its priority is the expiry date; a candidate expiring on or before `MIG_RENEWAL_URGENT_TO`
  (default 31-Jan-2028) is flagged URGENT, so the day-1 worklist of the Renewal processing team shows the January
  expiries first, earliest expiry first;
- when P03 has a row for the header, the candidate records the RA as already sent (date, reference, channel, sender,
  proposed insurer and premium quoted) and the RA step is marked done with the legacy date; BIBS does not send it
  again. A revised RA is a user action, as for any candidate.

The extraction is idempotent per header (the candidate key is the legacy reference), so a rerun creates no duplicate
and the daily extraction skips a header that already has a candidate.

**Extraction check** (report `MIG-RENEWAL-GOLIVE`, part of the go-live evidence): per expiry month January to May
2028, the P01 headers expiring in the window = candidates created + headers renewed in legacy; the loaded P03 rows =
candidates with the RA recorded as already sent; every January candidate is URGENT. A difference is a break raised to
the Renewal processing team and the Data Migration Lead (alert `MIG_RECON_BREAK`).

The January expiries get a lead time of days to four weeks instead of 140 days. The mitigation is operational (risk 9,
section 26): the day-1 priority queue, a staffing plan for January, and a Renewal team that is trained and has worked
the queue on the Mock 4 and dress-rehearsal data before go-live.

**Content of P03 ("RA already sent").** One row per expiring policy term whose renewal advice was sent by hand before
go-live, keyed by the `legacy_policy_ref` of its P01 header: cover no., expiry date, RA sent date, RA reference,
channel (e-mail, courier, hand, other), recipient, proposed insurer and premium quoted, sender, tracker and sheet the
row comes from, remarks. The file is small: at most the January-May 2028 expiries that received an RA before go-live.

**Excel intake (DMQ38).** The RMEL and the dispositions are kept in Excel trackers today. The migration takes only
the RAs already sent from them; the trackers themselves are not migrated.

| Step | What happens | Who |
|---|---|---|
| Template | The Data Requirements Workbook ships `P03_template.xlsx`: first sheet `RA_SENT` with the layout header in row 1, a list on the channel column, date cells as yyyy-MM-dd text, no merged cells or formulas; a second sheet with the instructions. The CSV template of the same layout is also accepted | iorta TechNXT issues; Renewal processing team uses |
| Compilation | The maker copies the RA rows of the Retail and Corporate trackers into the template, one row per expiring term, and names the tracker and sheet of each row. File name and control file follow the file contract (`P03_EXCEL_<yyyyMMdd>_<nn>.xlsx`; row count, distinct `legacy_policy_ref`, proposed premium per currency) | Renewal processing team - maker |
| Check before upload | The checker compares the file with the trackers (row counts per tracker and 10 sample rows) and releases it for upload | Renewal processing team - checker |
| Intake and validation | Intake checks (section 5.2) and the rules below | Migration Operator; BIBS |
| Rejection report | `MIG-REJECTS` of P03 in Excel: row number, legacy reference, column, value, rule, message, plus blank columns for the maker (correction, corrected by, date) and the checker (checked by) | BIBS; sent to the maker and the checker |
| Correction and resubmission | The maker corrects each rejected row (in the tracker when the tracker is wrong) and puts the corrected rows only in a resubmission file (next sequence, same as-of date), filling the correction columns. The checker reviews each correction against the tracker and approves the resubmission in the console (workflow `MIG_RESUBMISSION`: PREPARED, then APPROVED or RETURNED; the checker is never the maker). The operator loads it as a RERUN batch | Maker; checker; Migration Operator |
| Rows still rejected at the deadline | Not loaded. They are listed for the day-1 queue: before sending an RA for such a policy, the processor checks the tracker | Renewal processing team |

**Owner.** The Head of the Renewal processing team is the data owner of P03 (signs G1, G2 and G6). The team's makers
act as its data stewards (`MIG_DQ_RESOLVE`) and its checkers approve resubmissions (`MIG_RESUBMIT_APPROVE`); the
console limits both to the users named as steward and owner of P03 in the object register. Deadlines: in the mocks,
rejects are corrected before the next mock; in production the final file covers the RAs sent up to the last legacy
business day (T-5), and resubmissions are accepted until T-1 12:00.

**Validation** (workbook rules DQ-018, DQ-019, DQ-047 to DQ-049 and the generic rules):

| Check | Severity | Handling |
|---|---|---|
| The expiring term is in P01 of the same load (DQ-018) | ERROR | Maker corrects the reference |
| Expiry equals the P01 expiry and falls from T to 31-May-2028 (DQ-019) | ERROR | Maker corrects or removes the row |
| RA date not after the last legacy business day (DQ-047) | ERROR | Maker corrects the date |
| RA date more than 140 days before the expiry (DQ-048) | WARNING | Loaded; the checker confirms it against the tracker |
| The cover was not already renewed in legacy: no P01 term of the same cover starts on this expiry (DQ-049) | ERROR | Row removed; no renewal is needed |
| One row per expiring term (DQ-004) | ERROR | Maker keeps the latest RA |

**Load.** `RaSentLoader` stores `mig_ra_sent` (legacy reference, migrated account, expiry, RA date, reference,
channel, sender, proposed insurer and premium, tracker, batch); `MigratedPolicySource` returns it with the header in
the go-live extraction. The object is reconciled L1-L4 like any other and verified on samples per tracker
(Reconciliation Approach).

### 15.2 Package remapping at sanitation (DMQ36 answered)

Legacy packages (QPS package code and version) must map to the package names that TSU maintains in BIBS (BRD-3). BDOI
decided on 26-Sep-2026 that the remapping happens **at sanitation in Renewal**, per candidate, and not in the
migration intake (the concept paper had left "during upload processing or during sanitation" open, CP p.2, Annex B,
Annex C).

| Where | What happens |
|---|---|
| Migration | Loads the PACKAGE code map as reference data (object R06): entries from legacy package and version to BIBS package version, with conditional entries where one legacy package splits (qualifier risk code, insurer or sum-insured band). TSU prepares each version and the Product Owner of Marketing Business System approves it (G2). `PackageMapLoader` hands the approved version to the Renewal service that maintains the package map; the map is reconciled L1-L4 and is maintained in Renewal after go-live. P01 headers and P03 rows keep the legacy package as given: no package is resolved, rejected or warned at intake |
| Renewal sanitation | The check `PACKAGE_REMAP` reads the legacy package of the candidate's header and resolves the BIBS package version through the map. A package without an entry, with a REJECT entry or with no matching qualifier sends the candidate to the Exception bucket |
| Exception bucket | Worked by the Renewal processing team: the processor chooses the BIBS package for the candidate (or the new-business path) and records the reason. Each choice is passed to TSU for the next map version, so the same legacy package does not return to the bucket |

Profiling in each mock lists the legacy packages of the P01 headers expiring by 31-May-2028 that have no entry in the
PACKAGE map (an information report, not a gate). It is the expected Exception bucket volume at go-live, and TSU closes
the gaps before the map freeze (T-7). The check `PACKAGE_REMAP` is a change to the Renewal design (section 14.5).

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

### 17.2 Calendar (BDOI timeline; go-live January 2028, DMQ25, DMQ39)

T is the go-live date. The BDOI timeline answers the month (January 2028). The recommendation of DMQ39 (option A,
section 17.7; awaiting Comptrollership confirmation) is **Monday 3 January 2028**, at the year-end boundary: legacy
processes to 31-Dec-2027 and closes FY2027, and BIBS opens with the open items at 31 December and a provisional GL
opening trial balance. The last legacy business day is Wednesday 29 December 2027 (30 and 31 December are holidays);
the legacy EOD runs on Friday 31 December, followed by the business freeze. Comptrollership completes a December soft
close by about 20-Dec-2027 so that the preliminary December TB is close to final. The holidays around the year end
(24-25 and 30-31 December, 1 January) fall inside the cut-over window and the runbook places no task on them except
the cut-over itself.

| When | Step | Objects / checks |
|---|---|---|
| Sep-Oct 2026 (requirements and mapping) | Build-shaping decisions (16-Oct-2026); owners, stewards, keys, object decisions (G1) and layouts frozen, version 1 (G2 part 1) (30-Oct-2026) | all |
| Nov 2026-Mar 2027 (build) | Build waves in mock order (section 25); first full extracts 29-Jan-2027 for profiling and performance sizing; draft code maps | all M and CF |
| 19-30 Apr 2027 (SIT migration) | **Mock 1** in SIT (masked): reference data and clients first, then headers and the RA-sent file | R, C, P01, P03 |
| 5-16 Jul 2027 (SIT migration) | **Mock 2** in SIT (masked): all objects end to end, reconciliation L1-L5 | all |
| 2-13 Aug 2027 (UAT migration) | **Mock 3** = UAT load (masked); business verification on screens; the Drop 1 end-to-end UAT (Aug-Dec 2027) runs on migrated data | all |
| 4-15 Oct 2027 (UAT migration) | **Mock 4** = UAT refresh (masked): fresh extracts, run as a timed cut-over; go-live renewal extraction of the January-May 2028 expiries and the day-1 queue worked by the Renewal team; a true-up rehearsed on a test adjustment set | all |
| 15-26 Nov 2027 (full migration and cut-over; performance test) | **Dress rehearsal** on the production-sized environment, full-volume extract, timed against the window, rollback rehearsed; reserve slot 6-10 Dec 2027 | all |
| 4 Dec 2027 (T-30) | Production cut-over plan starts (Cutover Runbook); ORR / PRR Dec 2027-Jan 2028 | - |
| 20 Dec 2027 (T-14) | Legacy December soft close complete; production pre-load of reference data and clients; daily client deltas from then on | R, C |
| 27 Dec 2027 (T-7) | Code map freeze; legacy reference-data change freeze (new codes only through change control and a map version) | R |
| 29 Dec 2027 (T-5, Wed) | Last legacy business day; final RA-sent file compiled (maker-checker) | P03 |
| 31 Dec 2027 (T-3, Fri) | Last legacy EOD; business freeze 22:00; legacy business modules read-only; legacy GL restricted to FY2027 adjustments by named Comptrollership users; last client delta; preliminary December TB | - |
| 1 Jan 2028 (T-2, Sat) | Final extracts after EOD: headers, open invoices, UPP, collection state, RA-sent file, preliminary TB; intake checks; database snapshot (rollback point); loads in load order, provisional opening TB | P, F, G01 |
| 2 Jan 2028 (T-1, Sun) | Reconciliation, sign-offs G5 / G6, P03 resubmissions until 12:00, screening run, business smoke test; go / no-go at 18:00 | all |
| 3 Jan 2028 (T, Mon) | Go-live renewal extraction 04:00; BIBS open 08:00; Renewal team on the day-1 priority queue; hypercare starts | - |
| T to T+30 | Hypercare: daily legacy sub-ledger vs GL, clearing 0.00, automatch results, remittance extracts with legacy invoices, urgent January renewals, exception queues; defect triage twice a day | - |
| 18-21 Jan 2028 (T+15 to T+18) | Legacy FY2027 year-end close; true-up 1 extracted, approved, posted and reconciled | G01, G03 |
| First month-end (January 2028) | First close with legacy control accounts; ACSL recon LEGACY | - |
| About March-April 2028 (T+85 to T+106) | Audited FS; final true-up posted and reconciled; legacy GL locked | G01, G03 |
| 2 May 2028 (T+120) | True-ups closed | - |
| Run-off | Monthly run-off snapshot; archive loads; decommissioning review per legacy system | H |

### 17.3 Freeze windows and delta loads

- Reference data: pre-loaded at T-2 weeks; legacy code changes after the map freeze need a change request that adds a
  map version and a delta.
- Clients: pre-loaded at T-2 weeks, daily DELTA batches (new and changed clients) until the freeze.
- Open items, UPP, headers, collection state, RA-sent file, preliminary TB: loaded only after the freeze (a single FULL
  load), because they change every day.
- No business delta after go-live: legacy business modules are read-only. The only legacy changes after the freeze
  are the FY2027 closing and audit adjustments in the legacy GL, which reach BIBS as true-ups (section 17.7).

### 17.4 Mock runs and dress rehearsal

Each mock is a cutover plan of kind MOCK with the full task list: two in SIT (Apr and Jul 2027, SIT migration), two in
UAT (Aug and Oct 2027, UAT migration), then the dress rehearsal (Nov 2027). In every run reference data and clients
load and are accepted first (mock-load order, section 2), so Renewal and the other Drop 1 modules test on migrated
clients and reference data as early as possible. Exit criteria of a mock: all objects loaded, L1-L5
reconciled, defects logged, timings recorded per object. The dress rehearsal must complete within the window with at
least 20 % margin and must rehearse the rollback (restore of the snapshot). Mock 4 and the dress rehearsal also run the
go-live renewal extraction and let the Renewal processing team work the day-1 queue, and Mock 4 rehearses a true-up
with a test set of adjustment journals.

### 17.5 Go / no-go criteria and rollback point

| # | Criterion | Threshold |
|---|---|---|
| 1 | Day-1 objects at G6 | 100 % |
| 2 | Count reconciliation per object | loaded + skipped + rejected + excluded = staged; received = control |
| 3 | Financial rejects (F01, F02, G01) | 0, or each excluded item approved by the owner with a manual-entry plan |
| 4 | Amount reconciliation of open items, UPP and the provisional TB | 0.00 per currency |
| 5 | Migration clearing account | 0.00 per branch and currency |
| 6 | Legacy control accounts vs legacy sub-ledgers (ACSL, context LEGACY) | 0.00 |
| 7 | Client review queue | empty |
| 8 | Business smoke test (sample of legacy invoices: OTC payment, automatch, remittance extract preview, invoice 360) | passed |
| 9 | Rollback point (database snapshot) | taken and verified |
| 10 | Hypercare roster and support channels | in place |
| 11 | Preliminary December TB signed by Comptrollership as the provisional opening; legacy GL restricted to the named FY2027 adjustment users | signed; access list signed |
| 12 | Renewal: headers of every expiry T to 31-May-2028 loaded (P01 against the legacy in-force list); RA-sent file loaded; Renewal staffing plan for January | complete; confirmed |

**Rollback point:** a database snapshot at the start of the production load. **Fallback:** until the go / no-go
decision, restore the snapshot and reopen legacy for update. After go-live, the point of no return is the end of the
first business day (DMQ32); fallback after that is not offered, and issues are fixed forward. The true-ups come after
the point of no return and have no rollback other than a correcting true-up.

### 17.6 Legacy decommissioning

Two milestones, each with a checklist in the console (`mig_decommission_item`: system, criterion, evidence, status,
signed by). The checklist template gives each criterion a short default name that screens and messages use: Final
extracts reconciled, Final true-up reconciled, Archive reconciled, Legacy Inquiry verified, No open item needs the
system, Claims tail covered, Access logs archived, Retention covered, Owners signed; for the legacy context: No open
legacy invoice, No legacy UPP balance, Legacy accounts at zero, Chart decision recorded.

| Milestone | Criteria |
|---|---|
| Legacy system decommissioned | Archive objects of the system loaded and reconciled (counts and hash totals against legacy); for a system holding the GL (EBIX, ISYS), the final true-up reconciled to the audited FY2027 TB and the legacy GL locked; Legacy Inquiry verified by Audit / Compliance; no open inquiry or claim that needs the legacy system; the last legacy-booked policy expired plus the claims reporting tail (DMQ27); legacy access logs exported to the archive; retention obligations covered; sign-off by the system owner, Compliance and Comptrollership |
| Legacy context closed in BIBS | No open legacy invoice or legacy UPP (paid, reversed, written off or reclassified); legacy control accounts at 0.00; migration clearing at 0.00; Comptrollership decides whether the legacy accounts are closed (`coa` freeze) |

### 17.7 Year-end cut-over and FY2027 true-ups (DMQ39)

**Options** (the recommendation awaits Comptrollership confirmation, needed by M6, 1-Oct-2027):

| Option | What it means | Assessment |
|---|---|---|
| **A. Go-live at the year-end boundary (recommended)** | Go-live Monday 3-Jan-2028. Legacy processes to 31-Dec-2027 and closes FY2027. BIBS opens with the open items at 31 December and a provisional GL opening trial balance (balance sheet only; the FY2027 net result in retained earnings; P&L starts at zero in FY2028) from the preliminary December TB. The legacy GL stays open only for FY2027 closing and audit adjustments, restricted to Comptrollership, with no new business. Each adjustment reaches BIBS as a controlled opening-balance adjustment journal in the opening period: the first true-up after the legacy year-end close (about mid to late January 2028), the final one after the audited FS (about March-April 2028). December soft close by about 20-Dec-2027. FY2027 BIR annual returns and the FY2027 audit come from legacy; FY2028 from BIBS | Recommended: one fiscal year per system, no P&L migration, BIR books and returns never split inside a year, and the late adjustments are few, controlled and reconciled |
| B. Go-live after the Q1 close (April 2028) | Legacy runs January-March 2028; BIBS opens in April with the open items and the TB at 31 March, including the year-to-date P&L by account, branch and month | Not recommended: go-live moves three months off the BDOI timeline; FY2028 is split across two systems (quarterly BIR returns and books of accounts for Q1 from legacy, the rest and the annual returns from BIBS), so the FY2028 annual figures need a combination of both; the YTD P&L must be migrated and reconciled by month; the renewal peak and the FY2027 audit then fall in the same period as the cut-over |
| C. Two books in parallel for Q1 2028 | BIBS goes live in January, and legacy also keeps the books for January-March | Not recommended: every transaction is keyed twice for three months; the two ledgers must be reconciled daily and diverge on every difference in rules; it breaks the rule of one processing system per item (BRD p.5); it is unclear which book is the book of record for BIR; the staff load falls in the January renewal peak |

If BDOI wants extra assurance in January, the recommendation is a **report comparison, not two books**: for January
2028, Comptrollership compares the BIBS opening reports with their legacy counterparts at 31 December (trial balance,
PR / DTIP / commission receivable / UPP by insurer and ageing), and reviews the January month-end reports of BIBS (trial
balance, GL schedules, Cash Receipts and Cash Disbursements Books, remittance schedules) against the opening position
and the January activity. No transaction is keyed twice.

**Provisional opening.** G01 is the preliminary December TB after the soft close and the last EOD. `GlOpeningLoader`
posts the balance-sheet accounts (legacy control accounts to LGC-CLR, as in section 14.2) and puts the FY2027 net result
of the P&L accounts on retained earnings, per branch and currency, value date `MIG_OPENING_VALUE_DATE` (1-Jan-2028).
The journals carry the flag PROVISIONAL and reference `MIG-TB-<asof>`. The TB is signed by Comptrollership as the
provisional opening before GNG-3 (go / no-go criterion 11).

**Cut-off controls on the legacy GL after the freeze.**

| # | Control | Evidence |
|---|---|---|
| C1 | Legacy business modules read-only from the freeze (T-3 22:00); no new business in legacy | Runbook freeze test; any posting from a business module is a break |
| C2 | The legacy GL accepts journals only in FY2027 periods and only from the Comptrollership users named on an access list signed by the Head of Comptrollership; FY2028 periods are never opened in legacy | Access list; legacy period status; reviewed at each true-up |
| C3 | Every legacy journal after the freeze is entered in the FY2027 adjustment register (register reference, legacy journal no., date, kind CLOSING or AUDIT, reason or audit finding, preparer, approver - not the preparer) | Register kept by Comptrollership |
| C4 | At each true-up, the legacy journal listing since the freeze, extracted from the legacy audit trail with its control file, = the register = the G03 file | `MIG-TRUEUP-RECON`, cut-off section |
| C5 | Adjustments on accounts that BIBS holds in detail (legacy control accounts of F01 and F02) are accepted only with their open-item detail (G03D); an adjustment that would make an open item's balance leave the range 0 to booked is refused and handled with Comptrollership (it usually means the item moved in BIBS since go-live) | DQ-055, DQ-056 |
| C6 | A true-up is posted only through the console workflow `MIG_OPENING_TRUEUP` (prepared by the Comptrollership GL lead, approved by the Head of Comptrollership; never the same user). When the opening period (January 2028) is already closed, it is reopened with a reason for the posting only and closed again the same day; the reconciliation lists every journal posted into the period during that window | `mig_trueup`; period audit trail; `MIG-TRUEUP-RECON` |
| C7 | After the final true-up the legacy GL is locked for all users and the register is closed. A FY2027 finding after that is a prior-period adjustment in BIBS through the normal Comptrollership journal, outside the migration | Legacy lock evidence; closure sign-off |

**True-up process.** Each true-up n (1 = after the legacy year-end close, about 18-Jan-2028; F = final after the
audited FS, about March-April 2028; interim true-ups only when Comptrollership posts material adjustments in between)
is a batch of object G03:

1. BDOI IT extracts the legacy TB after the adjustments (G01 with `tb_version` TU<n> or FINAL), the adjustment journal
   lines since the previous true-up (G03), the open-item detail of the lines on legacy control accounts (G03D) and the
   legacy journal listing since the freeze (control input of C4).
2. BIBS validates: every journal dated in FY2027, posted after the freeze, in the register, balanced per branch and
   currency; every account mapped (`GL_ACCOUNT`: P&L accounts to retained earnings, legacy control accounts to LGC-CLR
   with G03D detail); detail lines on loaded items and within their balances.
3. The Comptrollership GL lead prepares the true-up (`MIG_TRUEUP_PREPARE`); the Head of Comptrollership approves it
   (`MIG_TRUEUP_APPROVE`, gate G4 of G03).
4. `TrueUpLoader` posts the **opening-balance adjustment journal** per branch and currency through
   `SystemJournalService.post`: journal type OPENING (privileged, so it may post into a CLOSING or REOPENED period),
   source `MIGRATION`, reference `MIG-TU-<n>-<branch>-<ccy>`, value date `MIG_OPENING_VALUE_DATE` (1-Jan-2028, the
   opening period). FY2027 P&L lines go to retained earnings, never to FY2028 P&L; lines on legacy control accounts go
   to LGC-CLR. For each G03D line it calls `LegacyInvoiceIntake.adjustOpening` (movement `LEGACY_ADJUSTED` or
   `LEGACY_WRITTEN_OFF` on the component, event `MIG_LEGACY_POSITION_TRUEUP`: legacy control account against LGC-CLR)
   or `UnappliedService.adjustMigratedOpening`. The posting is idempotent on the reference.
5. BIBS reconciles the true-up (`MIG-TRUEUP-RECON`) and the Head of Comptrollership signs it (G5):

| Check | Rule |
|---|---|
| Cut-off (C4) | Legacy journal listing since the freeze = register = G03; no journal dated outside FY2027; no posting user outside the access list; no business-module posting |
| Movement | Per BIBS account, branch and currency: G03 lines of true-up n = legacy TB of true-up n - legacy TB of the previous true-up (or of the provisional opening), both mapped through `GL_ACCOUNT` |
| Balance | Per BIBS account, branch and currency: provisional opening + true-ups 1 to n (OPENING journals of source MIGRATION) = legacy TB of true-up n, mapped (P&L netted into retained earnings; legacy control accounts replaced by the open-item detail) |
| Clearing and sub-ledgers | Migration Clearing 0.00 per branch and currency; legacy control accounts = legacy sub-ledgers (ACSL, context LEGACY) |
| Opening period | When the period was reopened: no other journal posted into it during the window |

A break blocks the sign-off; a true-up with an error is corrected by the next true-up, never by editing the posted
journal. After the final true-up, the reconciliation of the balance check against the **audited** FY2027 TB is signed by
the Head of Comptrollership and shared with the external auditor, the legacy GL is locked (C7), and BIBS reports of the
closed periods February and March that the true-up changed (balance sheet only) are re-run and filed. The true-ups close
at T+120 (about 2-May-2028) with a sign-off by the Head of Comptrollership and the Program Manager.

`mig_trueup` (company, true-up no., as-of, status PREPARED / APPROVED / POSTED / RECONCILED / SIGNED, legacy TB extract,
journals posted, prepared by / at, approved by / at, reconciliation run, signed by / at, period reopen log) keeps the
evidence; it is not purged with the staging data.

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
| `MIG_TRUEUP_PREPARE`, `MIG_TRUEUP_APPROVE` | Prepare and approve a FY2027 true-up (section 17.7) |
| `MIG_RESUBMIT_APPROVE` | Approve a resubmission of corrected rejected rows (checker; section 15.1) |
| `LEGACY_INQUIRY_VIEW`, `LEGACY_INQUIRY_EXPORT`, `LEGACY_ACCESS_LOG_VIEW` | Legacy Inquiry and its access log |
| `CASH_UPP_INCOME_REQUEST`, `CASH_UPP_INCOME_APPROVE` (V766) | Reclassification of UPP to income |
| `LEGACY_REVERSAL_REQUEST`, `LEGACY_REVERSAL_APPROVE` (V766, V786) | DPPR and PR2307 legacy batches |

### 18.2 Roles and SIT/UAT users (V1080; SIT/UAT users in the seed data V1980)

| Role | Persona | Key permissions | SIT/UAT user |
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
`COMMREC_TL` + LEGACY_REVERSAL_APPROVE; `COMPTROLLERSHIP` + MIG_VIEW, MIG_TRUEUP_PREPARE; `MIGRATION_RECON_APPROVER` +
MIG_TRUEUP_APPROVE (held by the Head of Comptrollership). The Renewal processing team's makers get MIG_VIEW and
MIG_DQ_RESOLVE and its checkers MIG_VIEW and MIG_RESUBMIT_APPROVE, both limited to object P03 (section 15.1), through
User Access requests.

Segregation of duties (fixed): the maker of a decision, map version, load, rollback, reclassification batch, true-up or
resubmission never approves it; the operator who ran a batch cannot sign G5 or G6 for it; one person cannot sign two gates of one batch.

### 18.3 Data protection (hosting appendix)

- Non-production: masked data only (section 5.3); unmasked extracts are never copied out of production.
- Staging and files purged within 5 days of batch sign-off (`MIG_STAGING_RETENTION_DAYS` = 5, job
  `MIG_STAGING_PURGE`, S3 lifecycle on the intake bucket); alert `MIG_STAGING_PURGE_OVERDUE` if a signed-off batch
  still has payloads after the limit.
- Access to the console, staging and intake bucket only for migration roles, and only from the Philippines (network
  allow-list / VPN at the infrastructure layer; AWS ap-southeast-1).
- Encryption at rest (KMS) and in transit (TLS, SFTP); every console action audited.

## 19. Accounting events and GL entries

Event types are seeded in V1080 (`acc_event_type`); SIT/UAT rules in the seed data V1982. Real accounts come from Comptrollership
(DMQ18). All events carry party, cost centre and business line, and the BOOK rate for foreign currency (DMQ35).

| # | Transaction | Event (source ref) | Default entry (SIT/UAT chart) |
|---|---|---|---|
| M1 | Legacy invoice opening | `MIG_LEGACY_INVOICE_OPENING` (`MIG:INV:<invoice>`) | Dr 1215.x (client) open PR; Dr 1216 open PR2307; Cr LGC-DTIP (insurer) open DTIP; Dr LGC-COMM (insurer) open commission and VAT; Cr 2222 / 2223 unrealised commission and deferred VAT still open; balance to LGC-CLR |
| M2 | Legacy UPP opening | `MIG_UPP_OPENING` (`MIG:UPP:<ref>`) | Dr LGC-CLR / Cr 2206 (client) |
| M3 | GL opening TB | System journal OPENING (`MIG-TB-<asof>-<branch>-<ccy>`) | Mapped balances; legacy control lines to LGC-CLR |
| M4 | Rollback of an opening (pre-sign-off only) | same event, negative, `...:RB` | Reverses M1 / M2 |
| M5 | FY2027 true-up (opening-balance adjustment journal) | System journal OPENING (`MIG-TU-<n>-<branch>-<ccy>`), value date 1-Jan-2028 | Mapped adjustment lines; FY2027 P&L lines to retained earnings; legacy control lines to LGC-CLR |
| M6 | Legacy open-item true-up | `MIG_LEGACY_POSITION_TRUEUP` (`MIG:TU:<n>:<item>:<component>`) | Legacy control account of the component (1215.x, 1216, LGC-DTIP, LGC-COMM, 2222, 2223, 2206) against LGC-CLR |
| O2 | Payment applied (legacy invoice or legacy UPP) | `OPS_PAYMENT_APPLY` with `LG_` components | Section 14.4 B |
| O8 | UPP refund (legacy) | `OPS_UNAPPLIED_REFUND` with `LG_AMOUNT` | Dr 2206 / Cr 2216 |
| O8b | UPP reclass to other income | `OPS_UNAPPLIED_TO_INCOME` (new) (`UIR:<batch>:<item>`) | Dr 2206 or 2205 / Cr 4190 |
| O4 | PR2307 legacy reversal | `OPS_CWT_DTIP_OFFSET` with `LG_DTIP`, `LG_PR2307` | Dr LGC-DTIP / Cr 1216 |
| O22 | DPPR legacy reversal | `OPS_DP_PR_REVERSAL` with `LG_` (always posted for LEGACY) | Dr LGC-DTIP / Cr 1215.x; commission per DMQ19 |
| O12 | Remittance of a legacy invoice | `OPS_REMITTANCE` with `LG_DTIP`, `LG_COMMISSION`, `LG_COMMISSION_VAT` | Dr LGC-DTIP + Dr 1602 / Cr LGC-COMM / Cr 2211 |
| O19 | Endorsement of a legacy invoice | `BROKER_BOOKING` with `LG_` components | As booking row 0 on the legacy accounts |
| O18 | Negative endorsement after remittance | `OPS_AR_INSURER_SETUP` with `LG_DTIP` | Dr 1225 / Cr LGC-DTIP |
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
| `MIG_TRUEUP_POST` | on demand, after the approval of a true-up | Post the M5 journals and the M6 open-item adjustments; run `MIG-TRUEUP-RECON` |

Parameters (`sys_parameter`, category DATA_MIGRATION): `MIG_ENVIRONMENT_CLASS` (NON_PRODUCTION), `MIG_CUTOVER_DATE`,
`MIG_STAGING_RETENTION_DAYS` (5), `MIG_CHUNK_SIZE` (500), `MIG_PARTITIONS` (4), `MIG_AMOUNT_TOLERANCE` (0.00),
`MIG_MAX_ERROR_RATE_MASTER` (0.5), `MIG_MAX_ERROR_RATE_FINANCIAL` (0), `MIG_CLIENT_MATCH_AUTO` (90),
`MIG_CLIENT_MATCH_REVIEW` (60), `MIG_INVOICE_NO_COLLISION_PREFIX` (true), `MIG_ARCHIVE_EXPORT_MAX_ROWS` (1000),
`MIG_ACCESS_EXPORT_ALERT_ROWS` (5000), `MIG_LEGACY_ACCESS_REASON_REQUIRED` (true), `MIG_LEGACY_LINK_EBIX`,
`MIG_LEGACY_LINK_QPS` (empty), `MIG_UPP_ISSUE_AR` (false; DMQ14), `MIG_LEGACY_INVOICE_NO_PATTERN` (`^I\d{8}$`, the QPS
pattern added after DMQ11), `CMR_INCENTIVE_INCLUDE_LEGACY` (false, V786), `MIG_OPENING_VALUE_DATE` (2028-01-01; value
date of the opening entries and the true-ups), `MIG_GOLIVE_RENEWAL_TO` (2028-05-31; last expiry of the go-live renewal
extraction), `MIG_RENEWAL_URGENT_TO` (2028-01-31; candidates expiring up to this date are flagged URGENT),
`MIG_RESUBMIT_DEADLINE` (T-1 12:00 in production; last approval time of a P03 resubmission).

Alerts (`alt_exception_code`): `MIG_EXTRACT_REJECTED`, `MIG_LOAD_FAILED`, `MIG_RECON_BREAK`, `MIG_UNMAPPED`,
`MIG_CLEARING_NOT_ZERO`, `MIG_STAGING_PURGE_OVERDUE`, `MIG_LEGACY_ACCESS_UNUSUAL`, `MIG_TRUEUP_BREAK` (a true-up
reconciliation check fails; to the Head of Comptrollership and the Data Migration Lead).

LOV types (V1080): `MIG_SOURCE_SYSTEM` (EBIX, QPS, ISYS, EXCEL, CMS), `MIG_OBJECT_CATEGORY` (REFERENCE, CLIENT, POLICY,
OPEN_ITEM, GL, HISTORY), `MIG_BREAK_REASON`, `MIG_WAIVER_REASON`, `MIG_ACCESS_REASON`, `LEGACY_RECORD_TYPE`,
`UPP_INCOME_REASON` (V766).

Workflows (V1080 unless noted): `MIG_OBJECT_DECISION`, `MIG_MAP_VERSION`, `MIG_BATCH_ROLLBACK`, `MIG_OPENING_TRUEUP`
(PREPARED, FOR_APPROVAL, APPROVED, POSTED), `MIG_RESUBMISSION` (PREPARED, APPROVED or RETURNED);
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
| `MIG-GL-CLEARING` | LGC-CLR balance per branch and currency; legacy control vs sub-ledger | 1.1b |
| `MIG-CLIENT-MATCH` | Clusters, scores, decisions, survivors | 2.1 |
| `MIG-SIGNOFF-STATUS` | Gates per object and batch | 1.1a |
| `MIG-CUTOVER-STATUS`, `MIG-GONOGO` | Tasks and criteria | 12.1 |
| `MIG-RUNOFF` | Legacy in-force by cohort and outcome | 12.1 |
| `MIG-RENEWAL-GOLIVE` | Go-live renewal extraction check: per expiry month January-May 2028, headers, candidates, renewed in legacy, RA already sent, URGENT | 12.1 |
| `MIG-TRUEUP-RECON` | Per true-up: cut-off, movement, balance, clearing and opening-period checks against the legacy TB | 1.1b |
| `MIG-TRUEUP-REGISTER` | True-ups with their legacy journals, register references, approvals and postings | 1.1b |
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
| True-ups | `/migration/trueups` | MIG_VIEW | True-ups with status; prepare (MIG_TRUEUP_PREPARE) and approve (MIG_TRUEUP_APPROVE); journals posted; reconciliation checks and sign-off |
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

Range **V1080-V1089** (schema) and **V1980-V1989** (seed data for SIT/UAT). A second block is **not** needed: owner changes go into
the owners' free versions; only the two owners whose ranges are full (opsledger V760-V763, Accounting V890-V899) use
versions of this range, as Collections did with V1006 for cashiering.

| Version | Owner | Content |
|---|---|---|
| V1080 `migration_foundation` | migration | Permissions grants, roles, LOV types, parameters, workflows `MIG_*`, alert codes, event types `MIG_*`, report registrations |
| V1081 `migration_objects_maps` | migration | `mig_data_object`, `mig_object_decision`, `mig_code_map_set` / `_version` / `_entry`, `mig_layout`, `mig_layout_column`, `mig_rule`, `mig_masking_rule`, `mig_survivorship_rule` |
| V1082 `migration_intake_staging` | migration | `mig_extract`, `mig_stage_row`, `mig_issue` |
| V1083 `migration_batches` | migration | `mig_batch`, `mig_batch_log`, `mig_batch_map_version`, `mig_key_xref`, `mig_client_match`, `mig_ra_sent`, `mig_resubmission` |
| V1084 `migration_recon_cutover` | migration | `mig_recon_run`, `mig_recon_line`, `mig_signoff`, `mig_cutover_plan`, `mig_cutover_task`, `mig_gonogo_criterion`, `mig_gonogo_decision`, `mig_runoff_cohort`, `mig_decommission_item`, `mig_trueup` |
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
| V1980 `seed_migration_users` | migration seed data | SIT/UAT users and roles of 18.2 |
| V1981 `seed_migration_setup` | migration seed data | Object register, map sets with approved versions, layouts, rules |
| V1982 `seed_migration_gl` | migration seed data | Accounts of the SIT/UAT chart for the legacy controls (LGC-*; codes to be taken from the unused range once Comptrollership answers DMQ18, not 1221 or 2212, which Accounting uses for USD balances) and 1215.x, 1216, 2206, 2222, 2223, 4190 and the `LG_` lines on the SIT/UAT rules of the Operations and booking events |

No foreign keys from `mig_*` to V8xx tables (targets are held as plain values), so V1080-V1085 are safe on a fresh
database. V1086 alters V761 tables and V1087 V896 tables, which run earlier. The SIT/UAT storyline (legacy invoices, UPP,
a mock batch) runs as a Java seed runner (`migration.seed.LegacyMigrationSeedData`, profile `seed`) that loads the seed
extracts from `backend/src/main/resources/db/seed/migration/` through the real pipeline, never by SQL into business tables.

## 25. Build-wave plan

The waves are built in the order the mocks need them (build window Nov 2026-Mar 2027). DM0 and DM1-C, which carry the
objects of Mock 1 (reference data, clients, headers, the RA-sent file and the PACKAGE map), come first and are deployed on
SIT by 9-Apr-2027 for Mock 1 (19-Apr-2027). DM1-A, DM1-B, DM2-A and DM2-B follow and are deployed by 18-Jun-2027 for
Mock 2 (5-Jul-2027); the DM3 performance harness is ready before the dress rehearsal (Nov 2027).

| Wave | When | Needed by |
|---|---|---|
| DM0 | Nov-Dec 2026 | Mock 1 |
| DM1-C | Dec 2026-Feb 2027 | Mock 1 |
| DM1-A, DM1-B | Jan-Mar 2027 | Mock 2 |
| DM2-A, DM2-B | Feb-Mar 2027 | Mock 2 |
| DM3 | Mar 2027 (E2E), performance harness by Oct 2027 | Mock 2; dress rehearsal |

| Wave | Stream | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **DM0** (1 stream) | Foundation | Module skeleton; V1080-V1084; object register and decisions; layouts; intake with checks and masking; staging and purge; code maps and unmapped report; rule engine; batch framework (loader SPI, chunking, run log, rerun, rollback framework); reconciliation L1-L4; sign-off gates; console shell and screens of sections 22 (except archive, cutover, run-off); shared files (`Permission.java`, `navigation/modules.ts`, help registry, `application.yml`, CONFIGURATION.md); seed data V1980-V1981; a reference-data loader (R01) as the first end-to-end object | `migration/**` (except `archive`, `cutover`), V1080-V1084, V1980-V1981, `features/migration/**` | R01 extract -> load -> recon -> sign-off in the console; ITs green |
| **DM1-A** | Legacy invoice contract | V1086 and all `opsledger` changes of 14.5; `LegacyInvoiceLoader` (F01) with M1 and open items; `GlOpeningLoader` (G01, M3; provisional opening with the FY2027 result to retained earnings); `TrueUpLoader` (G03, G03D; M5, M6; `adjustOpening`), `mig_trueup`, workflow `MIG_OPENING_TRUEUP` and `MIG-TRUEUP-RECON` (section 17.7); L5 reconciliation; V1087 and the ACSL filter; V1982 seed data (SIT/UAT chart) | `opsledger/**` (legacy parts), `acsl/service/GlSlQueries.java`, `migration/load/**/LegacyInvoiceLoader*`, `GlOpeningLoader*`, `TrueUpLoader*`, `migration/recon/**/TrueUp*` | Legacy invoice visible in Invoice 360; clearing 0.00 after F01 + G01; a test true-up with a P&L line and a control-account line posted, reconciled and signed |
| **DM1-B** | Cashiering, commission, remittance | V766, V786; context components in all Operations postings; migrated UPP (F02, M2); automatch on `match_refs`; income reclass batch with top-management approval; PR2307 and DPPR legacy batches; remittance postings and schedules; CSH reports | `cashiering/**`, `commission/**`, `remittance/**` (legacy parts), `migration/load/**/UppLoader*` | OTC and autopay on a legacy invoice; mixed UPP automatch; remittance batch with legacy lines; reclass executed after two approvals |
| **DM1-C** | Clients, reference, headers, RA-sent file | V803, V823; `registerMigrated`, screening skip, `importLegacy` (legacy package kept as given); loaders R03-R09, C01-C03, P01, P03 (`RaSentLoader`); `PackageMapLoader` (PACKAGE map with conditional entries, handed to the Renewal service, section 15.2); workflow `MIG_RESUBMISSION` and the P03 rejection report with correction columns; client matching and review queue; xref search; `LegacyAccountLookup`; `MigratedPolicySource` with `goLiveCandidates` (stub until Renewal R0 exists) and `MIG-RENEWAL-GOLIVE` | `crm/**`, `account/**`, `screening/matching/service/ScreeningTriggers.java` (legacy parts), `migration/matching/**`, loaders | 10,000 synthetic clients with duplicates matched, reviewed and loaded; headers searchable; the go-live extraction serves every header expiring T to 31-May with URGENT January candidates and the RAs already sent |
| **DM2-A** | Endorsements, prod recon, collections | V873, V1007; booking port and fallback; adjustment on legacy invoices; `PRC-LEGACY-CHANGES`; production extract exclusion; `LegacyItemStateService` and loader F03 | `booking/**`, `adjustment/**`, `prodrecon/**`, `collections/legacy/**` | Positive, negative and non-financial endorsement on a legacy invoice with legacy postings; change report shows original, updated, delta |
| **DM2-B** | Cutover, run-off, archive | V1085; cutover plan, tasks, go / no-go, runbook export; run-off tracker; archive loader, Legacy Inquiry, access log, retention providers | `migration/cutover/**`, `migration/archive/**`, `features/migration/cutover/**`, `features/migration/inquiry/**` | Mock plan executed in the console; archive search logged |
| **DM3** (1 stream) | Integration and rehearsal tooling | E2E test (extract -> load -> OTC -> automatch -> remittance -> endorsement -> recon -> sign-off); performance harness with 1,000,000 synthetic client rows and 500,000 invoice rows; SIT/UAT seed storyline; module guide `docs/modules/MIGRATION.md` | tests, seed data, docs | Full `mvn verify` / `npm run verify`; timings recorded |

Rules for parallel work:
- One Flyway range per stream: DM0 V1080-V1084 / V1980-V1981; DM1-A V1086-V1087 / V1982; DM1-B V766, V786; DM1-C V803,
  V823; DM2-A V873, V1007; DM2-B V1085.
- Shared files are edited only in DM0. `LedgerContext` and the `LG_` naming are delivered first by DM1-A (first change)
  so DM1-B and DM2-A build on them; DM1-B starts after that change.
- Changes in built modules follow the owners' conventions and are reviewed by the module owner; no edits to another
  wave's packages.
- Each wave has its own `*ApiIT` class; port stubs are `@ConditionalOnMissingBean`.
- Not before: Renewal R0 for the real `LegacyPolicySource` wiring; BT0 (V822) before V823.

## 26. Risks

1. **Legacy data cannot give components or paid / remitted splits** (DMQ12). Mitigation: open-balance mode; split rules
   as configuration; early profiling of the first full extracts (29-Jan-2027).
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
8. **Cut-over over the year-end holidays** (T-10 to T-2 fall on 24-Dec-2027 to 1-Jan-2028). Mitigation: date and last
   business day decided by M6 (DMQ39); roster confirmed at T-29; tasks that fall on a holiday moved in the runbook.
9. **Tight lead time for the January 2028 expiries** (processed in BIBS after go-live, DMQ37): days to four weeks
   instead of 140 days for insurer requests and RAs. Mitigation: day-1 priority queue (go-live extraction at T 04:00,
   January expiries URGENT, earliest expiry first); staffing plan for January (handlers, overtime, back-up) signed by
   the Head of the Renewal processing team at T-20; pre-go-live readiness of the Renewal team (trained, day-1 queue
   worked on the Mock 4 and dress-rehearsal data); RAs already sent loaded from P03 so no time is spent re-sending them.
10. **Package remapping at sanitation fills the Exception bucket in the first weeks** (DMQ36). Mitigation: PACKAGE map
    loaded as reference data and tested in every mock; profiling of the legacy packages of the headers expiring by
    31-May-2028 without a map entry, closed by TSU before the map freeze; each Exception-bucket choice fed into the next
    map version.
11. **P01 incomplete for the January-May 2028 expiries** (a header missing is a renewal never extracted). Mitigation:
    P01 scope includes every expiry to 31-May-2028 and every booked renewal term starting after T; P01 reconciled to the
    legacy in-force list; `MIG-RENEWAL-GOLIVE` extraction check; go / no-go criterion 12.
12. **Late or large FY2027 adjustments after go-live** (provisional opening TB, DMQ39). Mitigation: December soft close
    by about 20-Dec-2027; legacy GL restricted to named Comptrollership users and FY2027 periods; adjustment register;
    cut-off controls C1-C7 at every true-up; Migration Clearing 0.00 after each true-up; true-ups closed at T+120.
13. **Comptrollership does not confirm option A** (DMQ39). Mitigation: decision needed by M6 (1-Oct-2027); option B
    changes the calendar and adds a year-to-date P&L migration, so a later answer moves the go-live.

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
| R06 PACKAGE map | QPS; TSU | Product Owner, MBS (approves); TSU (prepares) | legacy package code and version, name, qualifier (risk code, insurer, sum-insured band), BIBS package version, action | XLSX | to supply | target package version active in Product Maintenance; qualifiers do not overlap | legacy packages of in-force policies | new versions until the map freeze |
| R07 Commission rates | QPS, EBIX | Head of Operations | insurer, product, rate, effective dates | CSV | to supply | rate 0-100; no overlap | current | none |
| R08 GL account map | EBIX, ISYS GL | Head of Comptrollership; FRBS / ACSL PO | legacy account, description, BIBS account, legacy control flag | XLSX | to supply | every TB account mapped; legacy controls to LGC-CLR | current chart | none |
| R09 Payees | EBIX / Disbursement | Comptrollership - Disbursement PO | payee code, name, TIN, address, bank, account no., mode | CSV | to supply | as `DISB_PAYEE_MIGRATION` | active payees | none |
| R11 Receipt series | EBIX | Operations - Financial Transactions | branch, kind AR / OR, ATP no., from, to, next no. | CSV | to supply | next no. within range | current | at freeze |
| C01-C02 Clients | QPS, EBIX, CMS | Product Owner, MBS; Heads of Marketing | legacy client no., type, names, birth / registration date, TIN, ID type and no., CIF, e-mail, mobile, addresses, segment, AO, KYC status and dates, last update | CSV | to supply | mandatory identity fields; formats; duplicates resolved | 2020 to present (umbrella p.43), scope per DMQ05 | daily until freeze |
| C03 Payout accounts | EBIX | Comptrollership - Disbursement PO | client no., bank, account no., account name, mode | CSV | to supply | bank code mapped | active | none |
| C04 KYC documents | QPS, shares | Compliance | client no., document type, file, date | files + CSV index | to supply | file readable; checksum | per retention | none |
| P01 Policy headers (P01, P01S) | QPS, EBIX | Head of Operations | cover / policy no., version, client no., product / risk code, legacy package and version (as stored), line, insurer(s) and shares (P01S), inception, expiry, sum insured, premium, currency, AO, unit, branch, PN nos., DP and FFY flags, business type, status | CSV | to supply | expiry after inception; client and product mapped; shares 100 % | in force at T (DMQ09); at least every expiry T to 31-May-2028 and every booked renewal term starting after T | at freeze |
| P03 RAs already sent | Excel RA trackers | Head of the Renewal processing team (makers and checkers of the team review the rejects) | expiring policy reference, cover no., expiry, RA sent date, RA reference, channel, recipient, proposed insurer and premium, sender, tracker, remarks | XLSX template or CSV | to supply (small) | header in P01; expiry T to 31-May-2028; RA date not after T-5; not renewed in legacy | RAs sent before go-live for expiries 3-Jan to 31-May-2028 | final at T-5; resubmissions until T-1 12:00 |
| P04 Submitted masterlists | Excel | CBG / Non-CBG Marketing | as `SBM_MIGRATION` (SUBMITTED_POLICIES_DESIGN) | XLSX | to supply | as SP design | all active (SP SQ16) | none |
| P05 EB programmes | EBIX | EB Head | as `EB_PROGRAMME_LOAD` | XLSX | to supply | as EB design | active programmes | none |
| F01 Open legacy invoices | EBIX, QPS | Operations - Financial Transactions; Head of Comptrollership | invoice no., source, kind, parent invoice, cover no. / version, policy no., client no., assured, payor, insurer(s) and shares, currency, booking / inception / expiry / due dates, risk code, line, segment, AO, unit, branch, DP / CWT / incentive flags; per component (basic, DST, PT / VAT, LGT, FST, other, DTIP, commission, VAT, WTAX, PR2307): booked, adjusted, paid, remitted, written off, open; commission realised; last payment date; legacy service invoice no. | CSV + control file | to supply | components add up; open = booked + adjusted - paid / remitted - written off; client, insurer and product mapped; unique invoice no. | every invoice open at T (DMQ10) | at freeze (single load) |
| F02 UPP | EBIX, CMS | Operations - Financial Transactions | UPP ref., AR no. and date, channel, payor, client no., amount, balance, currency, value date, references (invoice, cover, PN, bank), sales unit, legacy status and disposition in progress, remarks | CSV + control file | to supply | balance within 0 and amount; references present | every open UPP at T | at freeze |
| F03 Collection state | CMS | Head of Operations (Collections) | invoice no., disposition, promise date and amount, installment plan, collector, remarks | CSV | to supply | invoice in F01 | open items only (DMQ34) | at freeze |
| F04 Remittance in flight | EBIX | Operations - Remittance | batch no., insurer, invoices, amounts, status, holds with dates | CSV | to supply | invoices in F01 | open at T (DMQ21) | at freeze |
| F06 PDC / pick-ups / refunds | EBIX | Operations - Cashiering | check no., bank, date, amount, client, invoice, status | CSV | to supply | as cashiering rules | open at T (DMQ33) | at freeze |
| F07 Open claims | EBIX, ISYS | Unit Head, Claims | as the Claims migration layout (CLQ14) | CSV | to supply | as Claims design | open at T (DMQ30) | at freeze |
| G01 GL trial balance | EBIX / ISYS GL | Head of Comptrollership | TB version (PROVISIONAL, TU1, ..., FINAL), legacy account, branch, currency, debit, credit, base amounts, as-of | XLSX + control file | to supply | balanced per branch and currency; every account mapped (P&L to retained earnings) | preliminary December 2027 TB after the last EOD; the legacy TB at each true-up | at freeze; at each true-up |
| G03 FY2027 true-ups (G03, G03D) | EBIX / ISYS GL; adjustment register | Head of Comptrollership | legacy journal no., date, posted at, kind, register reference, lines (account, branch, currency, debit, credit), preparer, approver; open-item detail of legacy control-account lines | XLSX + control file | small | journals in FY2027, after the freeze, in the register, balanced; control-account lines with detail | adjustments after the freeze | at each true-up (about 18-Jan-2028 and after the audited FS) |
| H01-H02 Archive | EBIX, QPS, ISYS, shares | Audit / Compliance; system owners | record type, keys, dates, amounts, labelled columns, documents | CSV + files | to supply | counts and hash totals per record type against legacy | per retention (DMQ24) | before decommissioning |
