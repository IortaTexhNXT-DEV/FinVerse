# iNXT BrokerVerse - BDOI Product Maintenance (BRD-3) Build Design

Status: **proposal for review**. It extends `docs/architecture/BROKING_ARCHITECTURE.md` (binding) and the Developer
Guide, and it does not change them. Requirements baseline: [`BDOI_PM_BRD_SPEC.md`](../requirements/BDOI_PM_BRD_SPEC.md)
(31 requirement IDs, fit/gap and questions PQ01-PQ21). Every class, migration and screen cites its BRD ID in Javadoc
or in a comment, for example `BRPM.007` or `PMADD02`.

Java paths are relative to `backend/src/main/java/com/iortatechnxt/brokerverse/`.

## 1. Design principles

1. **Master data stays in `catalog`; the request process does not.** Everything that quotations, accounts, rating,
   booking and Operations read at transaction time (hierarchy, coverages, insurer terms, versions and rate schemes,
   validation state, expiry, incentive criteria) is catalog data. The package request lifecycle (request form,
   approvals, negotiation, ManCom, hand-off to MBS, advisory, renewal requests) is a transaction process with its
   own personas, documents, e-mails and SLAs. It lives in a new module, `productmaint`, which depends on `catalog`.
2. **A package is versioned, never edited in place.** A packaged product has one or more `cat_product_version`
   rows. Commercial terms are changed only by a new DRAFT version, which is validated (PMADD06) and released with
   an effective date. The released version keeps selling while the next one is drafted. This removes today's gap,
   in which an edited product is unusable until someone re-authorises it (`AuthorizableEntity.markModified`).
3. **New business uses the current scheme; history is replayable.** For new business the rating service always
   picks the current RELEASED version, whatever the period start (BRPM.007). Every quotation, account and invoice
   stores the `product_version_no` that priced it. Endorsements re-rate on the version of the original account.
   Renewals may use a superseded version only through the RENEWAL purpose (seam until the Renewal BRD).
4. **Two independent checks.** Record-level maker-checker (`AuthorizableEntity`) stays for identity masters (lines,
   cover types, coverages, clauses, products, incentive criteria). Package content is released by a **version-level
   validation checkpoint** with a named permission (PRODUCT_VALIDATE), and the maker can never validate their
   own version.
5. **Re-use the BRD-1 platform, do not fork it.** `workflow`, `messaging` (protection, send log), `docgen`
   (versioned templates), `attachment`, `report`, `alert` and `lov` are used as they are. The negotiation part
   follows the `nonpackage` implementation (QS, responses with history, comparative table) and adds rounds,
   exception outcomes and configurable outputs (PMADD03/04). `nonpackage` itself is not changed in this build (PQ05).
6. **Parked means seam, not fake.** Product master synchronisation to other BDOI systems (BRPM.022, PQ16), package
   formulas beyond Appendix A (PQ12) and renewal of accounts (Renewal BRD) each get a port or a documented
   parameter. Nothing is simulated.

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `catalog` (extended) | Hierarchy (subtype, coverage / peril, naming pattern), clause library, product versions with rate scheme, coverages and insurer terms, validation checkpoint and release, expiry / archive, rate-scheme exceptions, field-rule types, incentive criteria; contracts for rating, set-up and incentives | PMADD01/02/06/07/08, BRPM.003/004/006/007 | party, lov, dimension, audit, approval (unchanged), plus `alert` for the expiry and incentive alerts. Still no dependency on `workflow`, `messaging` or `docgen` | V813-V815 (V996) |
| `productmaint` (new) | Package request lifecycle: request form, Marketing and TSU approvals, negotiation rounds and outcomes, QS, comparative outputs (master / client), requirements pack, ManCom sign-off, MBS hand-off, release follow-up, advisories, expiry monitor and renewal requests, package status report, Product Maintenance home | BRPM.005/008-019/021/022, PMADD03/04 | catalog, crm (client look-up), workflow, messaging, docgen, attachment, report, alert, lov, system, audit | V816-V819 (V997) |
| platform | Product Maintenance permissions and roles, permission action classes, access-matrix view by action, role-permission change request | BRPM.001/002/020/024, PMADD05 | - | V755 |

### 2.1 Why `catalog` is extended and a new module is added

- **Extending `catalog` fits** for all master data. The tables are already maker-checker masters with effective
  dates (`EffectiveDatedRecord`), an approval source (`CatalogApprovalSource`, `CatalogKind`) and screens. The
  consumers (`quotation`, `account`, `nonpackage`, `placement`, `booking`, Operations `adjustment` / `commission`)
  already depend on `catalog`, so versions and incentive criteria reach them with no new dependency edge.
- **The request lifecycle does not fit in `catalog`.** `catalog` sits on the hot path of every NB transaction and
  depends only on platform masters today. The lifecycle would pull `workflow`, `messaging`, `docgen`, `crm` and
  `attachment` into it and mix transactional SLAs with reference data. As a separate module it can be built in
  parallel with the catalog changes (section 12).
- **Alternatives rejected:**
  - *Package requests as a `purpose` of the non-package PRF (`nonpackage`)*: the PRF has a client risk and ends in
    accounts (`create_accounts`), while a package request ends in a catalog version. The NB_PROPOSAL workflow would
    gain a second branch (ManCom, MBS, validation, advisory) and every BRD-1 PRF test would be at risk.
  - *Extracting a shared "negotiation" module from `nonpackage` now*: this is a refactor of a finished module for no
    BRD-1 benefit, unless BDOI answers PQ05 "yes" (PMADD03/04 also apply to PRFs). It is then a follow-up
    (section 13).

### 2.2 Dependency graph (arrows = "depends on")

```
  productmaint ──> catalog ──> party, lov, dimension, audit, approval, alert
       │              ^
       │              │ (unchanged edges)
       ├──> crm       quotation, account, nonpackage, placement, booking, adjustment, commission
       └──> workflow, messaging, docgen, attachment, report, alert, lov, system
```

- `catalog` never calls `productmaint`. It publishes Spring events after commit: `ProductVersionReleased`,
  `ProductVersionReturned`, `ProductExpired` and `IncentiveCriteriaChanged`.
- `productmaint` calls `catalog` through `PackageSetupService` (create or update a DRAFT version from the negotiated
  terms) and `ProductVersionQueryService`, and it listens to the catalog events.

## 3. Flyway allocation

Constraints: V750-V759 broking foundation (V750-V754 used), V760-V789 Operations, V810-V819 catalog (V810-V812
used), V9xx demo (V980-V989 NB, V990-V995 Operations). Flyway runs with `out-of-order: true`. None of the versions
below is used on any branch.

| Version | Owner (wave) | Content |
|---|---|---|
| `V755__product_maintenance_foundation.sql` | P0 | Permissions granted to roles (new roles MBS, TSU_TL, TSU_HEAD, MANCOM), `sec_permission_action` (permission -> area, action VIEW / CREATE / AMEND / APPROVE) for every existing and new permission of the broking areas, LOV types (`PKG_REQUEST_TYPE`, `PKG_REQUEST_REASON`, `PKG_RESPONSE_OUTCOME`, `PKG_ADVISORY_GROUP`, `INCENTIVE_TYPE`, `COVERAGE_KIND`, `CLAUSE_KIND`), workflow PM_PACKAGE_REQUEST (`wf_stage`, `wf_transition`), business parameters. It references only V1-V754 tables, so it is safe before V810 on a fresh database |
| `V813__catalog_hierarchy_and_clauses.sql` | P1-A | `cat_cover_type.parent_code`, `cat_product_line.code_pattern`, `cat_coverage`, `cat_clause`, `cat_field_rule` rule types; check "packaged -> cover type required" (seeds verified first) |
| `V814__catalog_product_versions.sql` | P1-A | `cat_product_version`, `cat_package_coverage`, `cat_package_insurer`, `cat_package_insurer_term`, `cat_rate_scheme_exception`, `cat_product.lifecycle_status`; **backfill**: version 1 RELEASED for every packaged product from its current columns (effective 2020-01-01, validated by SYSTEM) |
| `V815__catalog_incentive_criteria.sql` | P1-A | `cat_incentive_criteria`, `cat_incentive_criteria_product` |
| `V816__productmaint.sql` | P1-B | `pm_request` and children, negotiation, comparative outputs, sign-off, advisories; `DOCUMENT_TYPE` values; document templates |
| `V817__productmaint_reports_jobs.sql` | P1-B | Report definition PM-PKG-STATUS, jobs PACKAGE_EXPIRY_MONITOR, alert types, parameters |
| V818-V819 | - | Kept free (follow-ups) |
| `V821__account_product_version.sql` | P1-A | `acc_account.product_version_no`, `rate_override_ref` (account's own range) |
| `V831__quotation_product_version.sql` | P1-A | `quo_quotation.product_version_no`, `rate_override_ref` (quotation's own range) |
| `V871__booking_incentive_criteria.sql` | P1-A | `bkg_invoice.incentive_criteria` (comma-separated codes) and `product_version_no`; one-time copy of `bkg_incentive_rule` rows into `cat_incentive_criteria` (type MIGRATED, status PENDING_AUTHORIZATION); `bkg_incentive_rule` frozen (read-only in the API) |
| `V996__demo_product_versions.sql` | P1-A | Demo: version 2 DRAFT of one MTR package, coverages and insurer terms for two panel insurers, one incentive criterion "CPC2 (demo)" on MTR / PAR packages |
| `V997__demo_package_requests.sql` | P1-B | Demo: package requests at each stage (draft, TSU review, negotiation with two rounds and an "approved with changes" response, ManCom, with MBS, released with advisory), one package expiring in 45 days |

Why V821, V831 and V871: the new columns belong to tables created in V820 / V830 / V870. On a fresh database any
version below V820 would run before those tables exist, and the demo range must stay for demo data. Each change is
therefore made in the owning module's own free range, by the P1-A agent, as a small additive migration.

Rules:
- **No foreign keys from V755 to catalog tables** (V755 runs before V810 on a fresh database).
- `productmaint` tables (V816+) may reference catalog tables (V810-V815) by foreign key; they store client codes,
  insurer codes and ARNs as plain values (crm V800 exists, but `pm_request.client_code` stays a plain value like
  the other broking modules).
- The Developer Guide range table gets: "V755 Product Maintenance foundation; V813-V819 catalog extensions and
  Product Maintenance (`productmaint` V816-V819); demo V996-V997".

## 4. Entities (key fields)

All catalog tables carry the usual `version`, audit columns and, for masters, `record_status / authorized_by /
authorized_at`. Rates are percentages (12.5 = 12.5 %).

### 4.1 `catalog`: hierarchy and clauses (PMADD01/02)

- `cat_cover_type`: + `parent_code` (nullable; a Subtype points to its Type in the same line; depth at most 2).
- `cat_product_line`: + `code_pattern` (regex for new risk codes, nullable; checked by `ProductCatalogService.create`,
  error `PRODUCT_CODE_PATTERN`). The values wait for PQ02.
- `cat_coverage` (maker-checker): `line_code`, `code`, `name`, `kind` (LOV COVERAGE_KIND: PERIL / COVERAGE /
  EXTENSION / SECTION), `basic` flag, `sort_order`. Unique (line, code).
- `cat_clause` (maker-checker): `code`, `kind` (LOV CLAUSE_KIND: WARRANTY / CLAUSE / EXCLUSION / DEDUCTIBLE_WORDING),
  `line_code` (nullable = all lines), `title`, `wording` (text), `effective_from / to`.
- `cat_field_rule`: + `rule_type` (REQUIRED default / LOV / RANGE / PATTERN), `lov_type`, `min_value`,
  `max_value`, `pattern` (BRPM.004).

### 4.2 `catalog`: product versions and rate schemes (BRPM.006/007, PMADD02/06)

- `cat_product`: + `lifecycle_status` (ACTIVE / EXPIRED / RETIRED, default ACTIVE). For packaged products the
  commercial columns (`default_rate`, `minimum_premium`, `default_commission_rate`, `max_sum_insured`) become a
  **projection of the current released version**, written by the release step; they stay readable by the existing
  consumers.
- `cat_product_version`:
  - `product_code` (FK), `version_no` (1..n, unique per product);
  - `status` DRAFT / FOR_VALIDATION / RELEASED / SUPERSEDED / EXPIRED;
  - `effective_from`, `effective_to` (set when superseded), `package_start_date`, `package_end_date`,
    `anniversary_date` (BRPM.017);
  - rate scheme: `default_rate`, `minimum_premium`, `default_commission_rate`, `max_sum_insured`, `rating_basis_note`;
  - origin: `source_request_no` (PKR-...), `mancom_signoff_ref`, `change_summary`;
  - checkpoint: `submitted_by / at`, `validated_by / at`, `validation_checklist` (JSON of checked items),
    `returned_reason`;
  - unique partial index: one RELEASED version per product with `effective_to is null`; one DRAFT or
    FOR_VALIDATION version per product.
- `cat_package_coverage`: `version_id`, `coverage_code`, `included` / `optional`, `limit_amount`, `sub_limit`,
  `deductible_amount`, `deductible_percent`, `deductible_text`, `sort_order`.
- `cat_package_insurer`: `version_id`, `company_id`, `insurer_code` (FK `cat_insurer`), `role` LEAD / PARTICIPANT /
  PANEL, `share_percent` (co-insurance, nullable; sum 100 when used), `rate`, `minimum_premium`,
  `default_branch_code`.
- `cat_package_insurer_term`: `version_id`, `insurer_code`, `coverage_code`, `included`, `limit_amount`,
  `sub_limit`, deductible fields, `clause_codes` (comma-separated `cat_clause` codes), `remarks`.
- `cat_rate_scheme_exception` (maker-checker, approval source): `product_code`, `requested_version_no` or
  `requested_rate`, `purpose`, `reference` (quotation or account ARN), `reason`, `requested_by`, decided by
  PRODUCT_AUTHORIZE, `valid_until`. An approved row is the `rateOverrideRef` of BRPM.007.

### 4.3 `catalog`: incentive criteria (PMADD07/08)

- `cat_incentive_criteria` (`EffectiveDatedRecord`, maker-checker): `company_id`, `code` (for example CPC2), `name`,
  `incentive_type` (LOV INCENTIVE_TYPE), `value_basis` RATE / FIXED_AMOUNT / RULE, `value`, `rule_params` (JSON:
  minimum premium, period type, and similar), `successor_of` (id of the row it replaces), `description`. Unique
  (company, code, effective_from).
- `cat_incentive_criteria_product`: `criteria_id`, `product_code` (FK `cat_product`), optional `cover_type_code`,
  `market_segment`, `source_channel`, `insurer_code`.
- Validation (`IncentiveCriteriaService`): at least one product line; each product ACTIVE (record status and
  lifecycle) on `effective_from`; insurer ACTIVE; `effective_to >= effective_from`; no overlapping period for the
  same code. An ACTIVE row is never edited: `amend` end-dates it and creates a successor (PENDING_AUTHORIZATION).
  `deactivate` sets `effective_to` and INACTIVE.

### 4.4 `productmaint`

- `pm_request`: `request_no` (`PKR-<yyyy>`, parameter `PKG_REQUEST_PREFIX`), `company_id`, `request_type` (NEW /
  AMEND / UPDATE / RETIRE / RENEW / REACTIVATE), `scope` (GENERIC / CLIENT_SPECIFIC), `client_code` (nullable, PQ14),
  `line_code`, `cover_type_code`, `target_product_code` (null for NEW until set-up), `base_version_no`,
  `market_segments`, `reason` (LOV PKG_REQUEST_REASON), `requested_terms` (JSON: sections, coverages, limits,
  deductibles, requested rate, target insurers, period), `recommendation` (TSU TL text), `status` (mirrors the work
  case), `negotiation_required` (false for RETIRE, optional for RENEW), `chosen_insurers`, `proposed_terms` (JSON
  after negotiation), `resulting_version_no`, `released_at`.
- `pm_negotiation_round`: `request_id`, `round_no`, `qs_no` (`PQS-<yyyy>`), `qs_template_version`, `prepared_by`,
  `approved_by`, `sent_at`, `reply_due`, `locked` (true after FOR_MANCOM).
- `pm_insurer_response`: `round_id`, `insurer_code`, `outcome` (LOV PKG_RESPONSE_OUTCOME), `terms` (JSON per
  coverage: included, rate, limit, deductible, clause codes), `rate`, `minimum_premium`, `conditions`,
  `valid_until`, `remarks`, `response_document_id`, `revision`; history in `pm_insurer_response_history`
  (every change, PMADD04).
- `pm_comparative_output`: `request_id`, `round_no`, `kind` MASTER / CLIENT, `parent_output_id`, `current` flag
  (one current MASTER per request, BRPM.014), `fields` / `insurers` (JSON selections), `template_version`,
  `attachment_id`, `sha256`, `generated_by / at` (PMADD03).
- `pm_signoff`: `request_id`, `kind` MANCOM, `decision` SIGNED / RETURNED, `signed_by`, `signed_at`, `comment`,
  `signed_sheet_attachment_id` (PQ07).
- `pm_advisory`: `request_id` or `product_code`, `type` PACKAGE_READY / PACKAGE_UPDATED / RENEWAL / RETIREMENT,
  `recipient_groups` (LOV PKG_ADVISORY_GROUP), `subject`, `body` (merged PKG_ADVISORY template), linked document
  ids, `status` DRAFT / SENT, `message_ids` (messaging outbox), `sent_by / at`.

## 5. Versioning, effectivity and maker-checker

### 5.1 Version states

```
            submitForValidation            validate (PRODUCT_VALIDATE, not the maker)
  DRAFT ─────────────────────> FOR_VALIDATION ─────────────────────────────> RELEASED ──(next version effective)──> SUPERSEDED
    ^                               │ return(reason)                           │
    └───────────────────────────────┘                                          └──(package_end_date passed, no renewal)──> EXPIRED
```

- **DRAFT**: created by `PackageSetupService.createDraftVersion(PackageSpec)` (from a package request) or by
  "New Version" on the product page (PRODUCT_MAINTAIN, MBS). It is editable (coverages, insurer terms, rate scheme,
  dates) and invisible to rating.
- **Submit** checks completeness (PMADD01/06): cover type set; at least one basic coverage; every panel insurer has
  a term per included coverage; rate or insurer rate present when the rating method needs it; `package_end_date`
  after `effective_from`; required templates exist. Error codes: `PACKAGE_HIERARCHY_INCOMPLETE`,
  `PACKAGE_INSURER_TERMS_MISSING`, `PACKAGE_RATE_MISSING`, `PACKAGE_DATES_INVALID`.
- **Validate** (PMADD06): PRODUCT_VALIDATE; refused to the maker and to the submitter (`MAKER_CHECKER_VIOLATION`).
  The validator confirms the checklist, and a test premium is computed with `RatingService` on a sample item. On
  release:
  1. the previous RELEASED version gets `effective_to = new.effective_from - 1` and becomes SUPERSEDED once that
     date passes (daily job; immediately when effective today);
  2. the product's projection columns are updated and `cat_product` stays ACTIVE (no re-authorisation);
  3. audit row and `ProductVersionReleased(productCode, versionNo, effectiveFrom, sourceRequestNo)` after commit.
- **Return** sends it back to DRAFT with a reason (`ProductVersionReturned`).
- **EXPIRED**: set by the expiry monitor when `package_end_date` has passed and no newer version is RELEASED; the
  product's `lifecycle_status` becomes EXPIRED (BRPM.006). Expired versions stay readable and searchable, and are
  never deleted.
- **RETIRED** (product lifecycle): set by a RETIRE request (BRPM.011 "deletion"); the product and its versions
  stay readable.

### 5.2 Which version prices a transaction (BRPM.007)

`RatingQuery` gains three fields: `purpose` (NEW_BUSINESS default / RENEWAL / ENDORSEMENT), `schemeVersion`
(Integer, nullable) and `rateOverrideRef` (String, nullable). Resolution for a **packaged** product:

| Purpose | Version used | Refused when |
|---|---|---|
| NEW_BUSINESS | The current RELEASED version on the **transaction date** (clock), not the period start | Product EXPIRED or RETIRED (`PRODUCT_NOT_SELLABLE`); `schemeVersion` given and not current, or an item rate different from the scheme / insurer rate, without an approved `cat_rate_scheme_exception` (`RATE_SCHEME_NOT_CURRENT`) |
| RENEWAL | `schemeVersion` if it is RELEASED or SUPERSEDED (not EXPIRED), otherwise the current one | Version EXPIRED and no current version (`PRODUCT_NOT_SELLABLE`) |
| ENDORSEMENT | `schemeVersion` of the original account (mandatory) | Unknown version |

- Non-packaged products have no versions: rating is unchanged (the `cat_product` columns, rates on the period start).
- Statutory rates (`cat_rate`: DST, VAT, premium tax, FST, LGT via branch) keep their effective-date lookup; whether
  they also follow "latest approved" is PQ10.
- `Rating` returns `schemeVersion`, `schemeRate` and `overrideRef`. Quotation, account and booking store the version
  (section 9).
- Until the Renewal BRD adds a business type to accounts, every caller passes NEW_BUSINESS (quotation, account) or
  ENDORSEMENT (booking endorsements, Operations adjustment); RENEWAL is the seam.

### 5.3 What stays record-level maker-checker

- Lines, cover types, coverages, clauses, field / document / TSU rules, insurers, branches, commission rates, taxes,
  short-period and motor tables, sales organisation, incentive criteria and rate-scheme exceptions stay
  `AuthorizableEntity` with authorisation by PRODUCT_AUTHORIZE (catalog product areas) or MASTER_AUTHORIZE
  (unchanged areas).
- `cat_product` identity and classification changes (name, line, cover type, packaged flag, segments) remain in
  place with re-authorisation. For a packaged product, changing a versioned column directly is refused
  (`PRODUCT_FIELD_VERSIONED`); the user is sent to "New Version". This keeps a released package sellable while it is
  being changed.

## 6. Security (PMADD05, BRPM.002)

### 6.1 Permissions (added to `security.domain.Permission` in P0; granted in V755)

| Permission | Action class | Used for |
|---|---|---|
| `PRODUCT_VIEW` | VIEW | Product Maintenance screens (read), catalog read (in addition to MASTER_VIEW, kept for compatibility) |
| `PRODUCT_ARCHIVE_VIEW` | VIEW | Expired / retired products and their history (BRPM.006 "restricted to authorised personnel") |
| `PRODUCT_MAINTAIN` | CREATE / AMEND | Catalog product-area masters and draft versions (MBS) |
| `PRODUCT_AUTHORIZE` | APPROVE | Authorise product-area masters, incentive criteria and rate-scheme exceptions |
| `PRODUCT_VALIDATE` | APPROVE | Post-set-up validation and release of versions (TSU Head / Business Administrator, PMADD06) |
| `INCENTIVE_CRITERIA_MAINTAIN` | CREATE / AMEND | Incentive criteria (PMADD07 "Incentive Maintenance user") |
| `PKG_REQUEST` | CREATE / AMEND | Create and submit package requests (Marketing AO, TSU) |
| `PKG_REQUEST_APPROVE` | APPROVE | Marketing TL / TH / UH approval |
| `PKG_TSU_RECOMMEND` | APPROVE | TSU TL review and recommendation |
| `PKG_TSU_APPROVE` | APPROVE | TSU TH approval; release of terms to Marketing |
| `PKG_NEGOTIATE` | CREATE / AMEND | QS preparation, insurer responses, rounds, comparative outputs, requirements pack (TSU Officer / TL / TH) |
| `PKG_QS_APPROVE` | APPROVE | QS approval (TL or co-officer), four eyes |
| `PKG_MANCOM_SIGNOFF` | APPROVE | ManCom sign-off |
| `PKG_ADVISORY` | CREATE | Draft and send advisories (TSU TL / Officer / Operations) |
| `PKG_REPORT_VIEW` | VIEW | Package Status Update Report, Product Maintenance home |

MBS set-up uses PRODUCT_MAINTAIN (the `setup` action of the request and the version editor).

### 6.2 Roles (V755) and demo users (V996 / V997, password `Brokerverse@2026`)

| Role | Persona | Main permissions | Demo user |
|---|---|---|---|
| `MKT_AO` (existing) | Marketing AO | PKG_REQUEST, PRODUCT_VIEW | `ao` |
| `MKT_TL` (existing) | Marketing TL / TH / UH | PKG_REQUEST_APPROVE, PRODUCT_VIEW, PKG_REPORT_VIEW | `mkttl` |
| `TSU` (existing) | TSU Officer | PKG_REQUEST, PKG_NEGOTIATE, PKG_ADVISORY, PRODUCT_VIEW | `tsu` |
| `TSU_TL` (new) | TSU Team Lead | PKG_TSU_RECOMMEND, PKG_QS_APPROVE, PKG_NEGOTIATE, PKG_ADVISORY | `tsulead` (re-granted) |
| `TSU_HEAD` (new) | TSU Team Head / Head | PKG_TSU_APPROVE, PRODUCT_VALIDATE, PKG_REPORT_VIEW | `tsuhead` |
| `MBS` (new) | Marketing Business Services | PRODUCT_MAINTAIN, INCENTIVE_CRITERIA_MAINTAIN, PKG_REPORT_VIEW, PRODUCT_ARCHIVE_VIEW | `mbs`, `mbs2` |
| `MANCOM` (new) | Management Committee | PKG_MANCOM_SIGNOFF, PRODUCT_VIEW | `mancom` |
| `BUSINESS_ADMIN` (existing) | Business Administrator | PRODUCT_VALIDATE, PRODUCT_AUTHORIZE, PRODUCT_ARCHIVE_VIEW | `badmin` |

The matrix is a proposal until PQ17 / OQ48 are answered.

### 6.3 Role-to-action matrix (PMADD05)

- `sec_permission_action` (V755): `permission`, `area` (for example PRODUCT_MAINTENANCE, PACKAGE_REQUEST,
  INCENTIVES, and one area per existing broking module), `action` (VIEW / CREATE / AMEND / APPROVE). A permission can
  have two rows (CREATE and AMEND).
- `nbadmin` `AccessMatrixService` gains `byAction(area)`: roles x areas x actions with the permissions behind each
  cell; the User Access Matrix screen gets a view switch "By permission | By action", and the Excel export includes
  both (audited, as today).
- Role-permission maintenance (PMADD05 AC3): a new access-request type `MODIFY_ROLE_PERMISSIONS` (role, permissions
  added / removed, justification) decided by ACCESS_APPROVE, never the requester, applied by `AccessChangeApplier`
  through `UserAdminService.updateRole`. The direct `PUT /api/v1/admin/roles/{id}` stays for SYSADMIN (audited) and
  can be restricted by the parameter `ROLE_CHANGE_REQUIRES_APPROVAL` (default true for the BDOI tenant), subject to
  PQ17.

## 7. Workflow `PM_PACKAGE_REQUEST` (seeded in V755)

```
DRAFT --submit--> FOR_MKT_APPROVAL --approve--> FOR_TSU_REVIEW --recommend--> FOR_TSU_APPROVAL --approve--> NEGOTIATION
NEGOTIATION --terms_final--> TERMS_REVIEW --release_to_marketing--> FOR_MKT_REVIEW --accept_terms--> REQUIREMENTS_PREP
TERMS_REVIEW --skip_marketing_review (GENERIC scope)--> REQUIREMENTS_PREP
REQUIREMENTS_PREP --submit_requirements--> FOR_MANCOM --signoff--> WITH_MBS --setup--> FOR_VALIDATION
FOR_VALIDATION --(catalog ProductVersionReleased)--> RELEASED
FOR_VALIDATION --(catalog ProductVersionReturned)--> WITH_MBS
returns (reason): FOR_MKT_APPROVAL/FOR_TSU_REVIEW/FOR_TSU_APPROVAL --return--> DRAFT;
                  FOR_MKT_REVIEW --request_changes--> NEGOTIATION; FOR_MANCOM --return--> REQUIREMENTS_PREP;
                  WITH_MBS --return_incomplete--> REQUIREMENTS_PREP
NEGOTIATION --revise_qs--> NEGOTIATION (round n+1, PMADD04)
FOR_TSU_APPROVAL (RETIRE / no negotiation) --approve--> FOR_MANCOM
FOR_MKT_REVIEW/NEGOTIATION --not_proceeded(reason)--> NOT_PROCEEDED;  DRAFT..FOR_TSU_APPROVAL --void(reason)--> VOIDED
```

| Stage | Owner permission | SLA parameter (hours, default) | Business actions |
|---|---|---|---|
| DRAFT | PKG_REQUEST | - | `submit`: mandatory form fields, type-specific checks (`PKG_REQUEST_INCOMPLETE`) |
| FOR_MKT_APPROVAL | PKG_REQUEST_APPROVE | `PKG_SLA_MKT_APPROVAL` (24) | `approve` (four eyes), `return` |
| FOR_TSU_REVIEW | PKG_TSU_RECOMMEND | `PKG_SLA_TSU_REVIEW` (24) | `recommend` (text mandatory), `return` |
| FOR_TSU_APPROVAL | PKG_TSU_APPROVE | `PKG_SLA_TSU_APPROVAL` (24) | `approve` (not the recommender), `return` |
| NEGOTIATION | PKG_NEGOTIATE | `PKG_SLA_NEGOTIATION` (120) | QS submit / approve (PKG_QS_APPROVE) / send / resend, responses, `revise_qs`, `terms_final` (every insurer of the round has a final outcome and the chosen terms exist) |
| TERMS_REVIEW | PKG_TSU_APPROVE | 24 | `release_to_marketing` (CLIENT_SPECIFIC) or `skip_marketing_review` (GENERIC) |
| FOR_MKT_REVIEW | PKG_REQUEST | 48 | `accept_terms`, `request_changes`, `not_proceeded` |
| REQUIREMENTS_PREP | PKG_NEGOTIATE | 48 | `submit_requirements` (requirements pack complete: proposed terms, rates, computation basis, signed package slip) |
| FOR_MANCOM | PKG_MANCOM_SIGNOFF | `PKG_SLA_MANCOM` (72) | `signoff` (QS rounds locked), `return` |
| WITH_MBS | PRODUCT_MAINTAIN | `PKG_SLA_MBS_SETUP` (48) | `setup` (calls `PackageSetupService`), `return_incomplete` |
| FOR_VALIDATION | PRODUCT_VALIDATE | 24 | performed on the catalog version (section 5.1) |
| RELEASED, NOT_PROCEEDED, VOIDED | - | terminal | RELEASED drafts the PACKAGE_READY / UPDATED advisory and notifies Marketing |

The SLA defaults are placeholders until PQ08. Stage-entry notifications, assignment, claim and SLA alerts come
from the `workflow` module unchanged.

## 8. Jobs, alerts, parameters, numbers, templates, reports

| Item | Kind | Detail |
|---|---|---|
| `PACKAGE_EXPIRY_MONITOR` | ManagedJob, daily (`brokerverse.jobs.package-expiry-cron`, default 01:00 PHT) | Moves versions to SUPERSEDED / EXPIRED (catalog), raises PACKAGE_EXPIRING, drafts RENEW requests when `PACKAGE_RENEWAL_AUTODRAFT` is true (productmaint) |
| `PACKAGE_EXPIRING` | Alert type | To PKG_NEGOTIATE and PRODUCT_MAINTAIN holders at `PACKAGE_EXPIRY_NOTICE_DAYS` (60) and at 30 / 7 days |
| `INCENTIVE_PRODUCT_INACTIVE` | Alert type | Active incentive criteria on a product that expired or was retired (PMADD08) |
| Parameters | `system_parameter` | `PKG_REQUEST_PREFIX` (PKR-<yyyy>), `PKG_QS_PREFIX` (PQS-<yyyy>), `PKG_QS_REPLY_DAYS` (5), `PACKAGE_EXPIRY_NOTICE_DAYS` (60), `PACKAGE_RENEWAL_AUTODRAFT` (false), `ROLE_CHANGE_REQUIRES_APPROVAL` (true), SLA hours of section 7 |
| Templates (docgen) | `doc_template` | PKG_REQUEST_FORM, PKG_QUOTATION_SLIP, PKG_COMPARATIVE (MASTER / CLIENT variants), PKG_SLIP, PKG_ADVISORY, PKG_RENEWAL_ADVISORY (layouts Q03) |
| `DOCUMENT_TYPE` values | LOV | PKG_REQUEST_FORM, PKG_QUOTATION_SLIP, PKG_INSURER_RESPONSE, PKG_COMPARATIVE, PKG_SLIP_SIGNED, MANCOM_SIGNOFF, PKG_ADVISORY |
| Report `PM-PKG-STATUS` | ReportDefinition, category NEW_BUSINESS | Package Status Update Report (BRPM.018), PDF / XLSX / ODS / CSV, saved variants |
| Report `PM-PKG-EXPIRY` | ReportDefinition | Packages by end date with renewal status (BRPM.017) |
| Report `PM-VERSION-HISTORY` | ReportDefinition | Versions per product with validator, dates and change summary (BRPM.006/007 audit) |

## 9. Impact on the modules already built (contract changes)

### 9.1 `catalog` (owned by P1-A)

| Contract | Change |
|---|---|
| `RatingQuery` (record) | + `Purpose purpose` (default NEW_BUSINESS when null), `Integer schemeVersion`, `String rateOverrideRef`. The existing constructor stays as an overload that passes nulls, so current callers compile |
| `RatingService.rate(RatingQuery)` | Resolves the version (section 5.2) for packaged products; uses the version's rate / minimum premium, and the `cat_package_insurer` rate and minimum when the insurer is on the panel; refuses a non-panel insurer for a package with a panel (`INSURER_NOT_ON_PACKAGE`); returns `Rating` + `schemeVersion`, `schemeRate`, `overrideRef` |
| `POST /api/v1/catalog/rating/quote` (Premium Calculator) | Request + `purpose`, `schemeVersion`; response + `schemeVersion`, `nonCurrent` flag. The calculator screen shows "Priced on version n (effective ...)" and a version picker read-only for history |
| `ProductCatalogService` | + `requireSellable(code, Purpose, LocalDate)` (lifecycle and version checks; `PRODUCT_NOT_SELLABLE`); `ProductFilter` + `lifecycleStatus`, `includeExpired`; `requireUsableProduct` unchanged |
| `ProductVersionQueryService` (new) | `current(code)`, `inForce(code, date)`, `version(code, n)`, `versions(code)`, `packagesExpiring(companyId, within)` |
| `PackageSetupService` (new) | `createDraftVersion(PackageSpec)` / `updateDraftVersion(...)`: product code (new or existing), base version, coverages, insurer terms, rate scheme, dates, source request no. and ManCom reference; returns `VersionRef`. Refuses when a DRAFT / FOR_VALIDATION version exists (`VERSION_IN_PROGRESS`) |
| `ProductVersionService` (new) | `submitForValidation`, `validate`, `returnToDraft`, `expireDue(date)` |
| `RateSchemeExceptionService` (new) | `request(...)`, `requireApproved(ref, product, purpose)` |
| `IncentiveCriteriaService` (new) | `matching(companyId, IncentiveFacts{productCode, coverType, segment, channel, insurerCode, date})` -> list of criteria codes; maintenance methods |
| `ProductRuleService` | + `violations(product, FieldValues)` for the rule types of BRPM.004 |
| `TsuRoutingService.evaluate` | The package TSI limit is read from the current version (the projection column keeps it compatible) |
| Events | `ProductVersionReleased`, `ProductVersionReturned`, `ProductExpired`, `IncentiveCriteriaChanged` (after commit) |
| `CatalogKind` / `CatalogApprovalSource` | + COVERAGE, CLAUSE, INCENTIVE_CRITERIA, RATE_SCHEME_EXCEPTION |

### 9.2 `quotation` (P1-A, V831)

- `QuotationPricing.rate(...)` passes `purpose = NEW_BUSINESS` and the quotation's `rateOverrideRef`, and stores
  `Rating.schemeVersion` in `quo_quotation.product_version_no` and in the version JSON (`QuotationContent` + 
  `schemeVersion`). The content codec keeps old JSON readable.
- `QuotationService.create` uses `requireSellable(code, NEW_BUSINESS, today)` instead of `requireUsableProduct`.
- On `submit`, an item rate different from the scheme rate needs an approved exception (`RATE_SCHEME_NOT_CURRENT`);
  the quotation page offers "Request rate exception" (creates `cat_rate_scheme_exception`).
- `QuotationSummary` + `productVersionNo`. The version diff (`QuotationDiff`) shows a scheme version change.
- Accounts created by `create_accounts` receive the quotation's `product_version_no` through `NewAccount`.

### 9.3 `account` (P1-A, V821)

- `NewAccount` + `productVersionNo`, `rateOverrideRef` (nullable; filled from the quotation / PRF). `AccountPricing`
  passes them; direct creation prices on the current version.
- `AccountService.createDraft` uses `requireSellable(..., NEW_BUSINESS, today)`.
- `Account.getProductVersionNo()`, exposed in `AccountQueryService` views and the Premium tab.

### 9.4 `booking` (P1-A, V871) and Operations

- `BookingRuleService.incentiveEligible(...)` delegates to `IncentiveCriteriaService.matching(...)`; the invoice
  stores the matched codes (`bkg_invoice.incentive_criteria`), and the existing incentive flag is `!codes.isEmpty()`.
  The `/booking/setup/incentive-rules` API becomes read-only and the setup tab links to Product Maintenance >
  Incentive Criteria.
- `InvoiceBuilder` copies `product_version_no` from the account. `InvoiceBooked` + `productVersionNo` and
  `incentiveCriteria` (additive fields; `opsledger` ignores unknown fields until it uses them).
- `EndorsementPostingService` / `EndorsementCalculator` pass `purpose = ENDORSEMENT` and the original account's
  version to rating.
- **Operations contract asks** (for the O1 agents, no code in this build):
  - `adjustment` (ADJID.008 / 014): re-rate with `purpose = ENDORSEMENT, schemeVersion = account.productVersionNo`.
  - `commission` (O1-D, CMRID.003 / 005 / 006): `cmr_incentive_scheme.criteria_code` references
    `cat_incentive_criteria.code` for product eligibility, instead of product lists of its own (OQ39).
  - `remittance` (RMTID.023 early-remittance incentive): can use a criteria code for product eligibility (OQ23).

### 9.5 `nonpackage`, `placement`, `issuance`, `nbreport`

- `nonpackage`: no change. `ProposalService` uses `requireUsableProduct` for non-package products, which are not
  versioned. Adopting PMADD03/04 is a follow-up if PQ05 says so.
- `placement` / `issuance`: no change (they read the account).
- `nbreport`: `NB-BOOKED-REG` and `NB-PLC-UPDATE` gain an optional "Product version" column; the NB Dashboard gets a
  Product Maintenance tile (counts from `GET /api/v1/product-maintenance/counts`), read through the existing SQL
  pattern, with no new compile-time dependency.

## 10. API

- `/api/v1/catalog` (extended; read PRODUCT_VIEW or the existing read permissions; maintain PRODUCT_MAINTAIN;
  authorise PRODUCT_AUTHORIZE):
  - `coverages`, `clauses` (CRUD + authorise / deactivate through `records/{kind}/{id}`);
  - `products/{code}/versions` (GET list, POST new draft), `products/{code}/versions/{n}` (GET, PUT draft),
    `.../coverages`, `.../insurers`, `.../insurer-terms` (PUT, draft only), `.../submit`, `.../validate`
    (PRODUCT_VALIDATE), `.../return`;
  - `rate-scheme-exceptions` (GET, POST; decide via My Approvals);
  - `incentive-criteria` (GET, POST, PUT = amend, `/{id}/deactivate`; INCENTIVE_CRITERIA_MAINTAIN);
  - `rating/quote` (request / response additions of section 9.1).
- `/api/v1/product-maintenance` (`productmaint`):
  - `requests` (GET list with stage / type / scope / mine / expiring filters; POST), `requests/{id}` (GET, PUT),
    `requests/{id}/{action}` for `submit`, `approve`, `recommend`, `terms-final`, `release-to-marketing`,
    `accept-terms`, `submit-requirements`, `signoff`, `setup`, `return-incomplete`;
  - `requests/{id}/rounds` (GET, POST = revise QS), `rounds/{r}/quotation-slip/submit | approve | send`,
    `rounds/{r}/insurers/{code}/resend`, `rounds/{r}/responses/{rid}` (PUT, POST document), `responses/history`;
  - `requests/{id}/comparatives` (GET, POST client output), `comparatives/{oid}.pdf | .xlsx`;
  - `requests/{id}/advisories`, `advisories/{aid}/send`;
  - `expiry` (GET packages by end date), `expiry/renewal-requests` (POST bulk "Generate Renewal Request");
  - `counts` (home tiles).
- Generic workflow actions (return, void) run through `POST /api/v1/workflow/cases/{id}/actions/{action}` as in
  BRD-1.

## 11. Screens: **Product Maintenance** (group Client & Policy)

The catalog section "Products & Insurers" is renamed **Product Maintenance**, matching the BDOI prototype
(`docs/design/BDO_UX_GUIDELINES.md` section 3). It keeps its position in `navigation/modules.ts` (Client & Policy,
after Adjustment). Screens follow the broking patterns: `WorklistToolbar`, status tabs in a flush card,
`RecordSummary`, `WorkflowPanel`, `StatusBadge`, gold flag chips (`.tag`) and `EmptyState`.

| Screen | Route | Permission | Content |
|---|---|---|---|
| Product Maintenance home | `/product-maintenance` | PKG_REPORT_VIEW or PRODUCT_VIEW | Tiles by stage with SLA red / amber, expiring 30 / 60 / 90, versions for validation, advisories pending; each tile opens its list (BRPM.019) |
| Package Requests | `/product-maintenance/requests` | PRODUCT_VIEW | Work list with tabs Drafts / For Approval / TSU Review / Negotiation / ManCom / With MBS / For Validation / Released / Closed; search "Search Request No."; columns request no., type, scope flag chip, client / programme, line, product, stage pill, stage age, assignee; bulk Assign |
| New Package Request | `/product-maintenance/requests/new` | PKG_REQUEST | Wizard: type and scope, client (CRM look-up) or programme, line / cover type / product, requested terms by section and coverage, target insurers, documents, review |
| Package Request page | `/product-maintenance/requests/:id` | PRODUCT_VIEW | Header (request no. chip, stage pill, flags), `WorkflowPanel`; tabs Details, Requested Terms, Negotiation (rounds, QS, insurer responses grid with the outcome pill), Comparative (current master, client outputs, "Generate Client View" dialog with field / insurer checkboxes), Requirements & Sign-off, Set-up (link to the catalog version), Advisories, Documents, E-mails, History |
| TSU Workbench (packages) | `/product-maintenance/tsu` | PKG_NEGOTIATE | Queue tiles per stage for TSU TL / Officer / TH |
| Products | `/catalog/products` (kept) | PRODUCT_VIEW | + lifecycle and current version columns, "Expired" and "Retired" filters (PRODUCT_ARCHIVE_VIEW), version status chip |
| Product page | `/catalog/products/:code` (kept) | PRODUCT_VIEW | `RecordSummary` (code, name, line > type > subtype, lifecycle pill, flags Package / Client-specific), tabs Versions (timeline, compare two versions), Coverages, Insurer Terms (insurer x coverage matrix), Field Rules, Documents, Incentives, History |
| Version editor | `/catalog/products/:code/versions/:n` | PRODUCT_MAINTAIN (edit) / PRODUCT_VALIDATE (validate) | Draft form with sections Rate Scheme, Dates, Coverages, Insurers, Insurer Terms; "Submit for Validation"; for the validator a checklist panel with the test premium and Validate / Return |
| Validation Queue | `/catalog/validation` | PRODUCT_VALIDATE | Versions FOR_VALIDATION with age |
| Package Expiry | `/product-maintenance/expiry` | PKG_NEGOTIATE or PRODUCT_MAINTAIN | Tabs Expiring / Renewal in Progress / Expired; bulk **Generate Renewal Request**; dialog "Generate Expiry List" with a date range (prototype pattern) |
| Coverages & Clauses | `/catalog/coverages` | PRODUCT_VIEW | Coverage / peril list per line, clause library with wording |
| Incentive Criteria | `/catalog/incentives` | PRODUCT_VIEW / INCENTIVE_CRITERIA_MAINTAIN | List with effective dates and status pill; editor with product-matrix picker (active products only) and history of successors |
| Insurers, Rates & Taxes, Sales Organisation, Premium Calculator | existing | existing | Premium Calculator shows the scheme version; the rest is unchanged |

Frontend folders: `features/catalog/**` (extended by P1-A) and `features/productmaint/**` (new, P1-B, with its
`help.ts`). The rate-exception request dialog is in `features/quotations/**` (P1-A).

## 12. Build-wave plan

Prerequisite: none blocking. BRD-1 is complete. Operations agents work in V760-V789 and in their own packages; the
only shared edits are the Permission enum, navigation and help registry, which P0 does in one short pass.

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **P0** (1 agent, short) | PM foundation | All PM permissions (Permission enum), V755 (roles, grants, `sec_permission_action`, LOV types, workflow PM_PACKAGE_REQUEST, parameters); `nbadmin` action-class matrix view and MODIFY_ROLE_PERMISSIONS access request (PMADD05); catalog **contract stubs** published as interfaces and records (`PackageSetupService`, `PackageSpec`, `VersionRef`, `ProductVersionQueryService`, the four events, `RatingQuery.Purpose`) so P1-B compiles; navigation rename and PM route placeholders; help sections pre-registered; the job cron in `application.yml`; Developer Guide range row | `security/domain/Permission.java`, `V755__*`, `nbadmin/**` (small), `catalog/service/version/*` (interfaces and records only), `frontend/src/navigation/modules.ts`, `frontend/src/help/helpContent.ts`, `features/catalog/module.ts` (section rename), `application.yml`, `docs/development/DEVELOPER_GUIDE.md` | `mvn verify` green; the matrix shows actions; interfaces compile |
| **P1-A** (parallel) | Catalog and consumers | Everything in sections 4.1-4.3, 5 and 9.1-9.4: hierarchy, clauses, field-rule types, versions and release, rate-scheme exceptions, expiry state changes, incentive criteria; the rating contract; quotation / account / booking changes; catalog screens (Products, Product page, Version editor, Validation Queue, Coverages & Clauses, Incentive Criteria, calculator version) and the quotation rate-exception dialog; demo V996 | `catalog/**` (implementing the P0 interfaces), `V813-V815`, `V821`, `V831`, `V871`, `V996`, `quotation/service/QuotationPricing.java`, `QuotationService.java`, `QuotationContent*`, `account/service/AccountPricing.java`, `AccountService.java`, `NewAccount.java`, `booking/service/BookingRuleService.java`, `InvoiceBuilder.java`, `Endorsement*.java`, `features/catalog/**`, `features/quotations/**` (dialog only), their tests (`CatalogVersionIT`, `IncentiveCriteriaIT`, `RatingSchemeIT`) | A package version drafted -> validated -> released; NB refuses an old version without an exception; quotation / account / invoice carry the version; booking stamps criteria codes; existing IT suites green |
| **P1-B** (parallel) | Product Maintenance process | `productmaint` complete: request, approvals, negotiation rounds and outcomes, QS send / resend, comparative master / client outputs, requirements and ManCom sign-off, MBS set-up call, release listener, advisories, expiry monitor and renewal requests, reports PM-PKG-STATUS / PM-PKG-EXPIRY / PM-VERSION-HISTORY, home and counts; screens; demo V997. Uses a stub `PackageSetupService` (`@ConditionalOnMissingBean`) until P1-A merges | `productmaint/**`, `V816-V817`, `V997`, `features/productmaint/**`, `ProductMaintenanceApiIT` | A request runs from draft to RELEASED with the stub; advisory blocked without documents; expiry job drafts renewal requests |
| **P2** (1 agent, short) | Integration and documentation | Remove the stub; E2E test (request -> negotiation (2 rounds) -> ManCom -> MBS set-up -> validation -> release -> quotation on version 2 -> account -> booking with criteria codes -> endorsement on version 1 for an old account); NB Dashboard tile; `nbreport` version columns; module guide `docs/modules/PRODUCT_MAINTENANCE.md`; new section 17 in BROKING_ARCHITECTURE.md; traceability rows for BRPM / PMADD; fit/gap workbook data | tests, `nbreport/**` (small), docs | Full `mvn verify` / `npm run verify`; walkthrough with the demo users |

**Three agents in total (P0, then P1-A and P1-B in parallel), plus one short P2 pass.** Parallel-work rules (as for
Operations):
- One Flyway range per agent.
- No edits to another agent's package.
- Shared files (Permission enum, navigation, help registry, `application.yml`) are edited only in P0.
- Each module has its own `*ApiIT` class.
- Stubs are `@ConditionalOnMissingBean`.

## 13. What depends on information BDOI has not given (build the seam, park the content)

| Topic | Requirements | Built now | Parked content | Q |
|---|---|---|---|---|
| Product matrix content, subtypes, coverages, naming convention | PMADD01, BRPM.003 | Tables, validation, pattern column | Values (seeds are defaults) | Q01, Q02, PQ02 |
| Insurer-specific terms model (panel / co-insurance) | PMADD02 | Panel with role and share | Which attributes vary; shares | PQ03, OQ33 |
| Rate scheme scope and override approver | BRPM.007 | Versions, exceptions, purpose | Whether statutory rates are included; approver | PQ10, Q43 |
| Renewal use of superseded versions | BRPM.007 AC5, BRPM.017 | RENEWAL purpose | Account business type (Renewal BRD) | PQ11 |
| Validation role and checklist | PMADD06 | PRODUCT_VALIDATE and checklist JSON | Final role and items | PQ09 |
| ManCom mechanics | BRPM.015 | Single sign-off with optional signed sheet | Quorum / members | PQ07 |
| Approval chain and SLA values | BRPM.008/009/012/021 | Stages with SLA parameters | Values; TL / TH / UH chain | PQ08, Q05 |
| Exception outcomes; PRF adoption | PMADD04 | LOV with a proposed list | Final list; `nonpackage` follow-up | PQ05 |
| Comparative fields and client layout | PMADD03 | Field catalogue from QS fields | Layout | PQ06, Q03 |
| Package formulas beyond Appendix A | BRPM.015 | Three rating methods | New formulas need development | PQ12 |
| CPC2 and incentive attributes | PMADD07/08 | Criteria master and matching | Definitions and values | PQ04, Q33, OQ39 |
| Product master synchronisation | BRPM.022 | Events + `ProductMasterFeed` port (logged, no transport) | Target systems | PQ16, Q08 |
| Document layouts and password convention | BRPM.005/020 | Templates (draft layouts), protection | Layouts, convention | Q03, Q07, PQ21 |
| Access matrix | PMADD05 | Proposed roles, action classes | Final matrix | PQ17, OQ48 |

## 14. Risks

1. **Behaviour change for new business.** Pricing on the transaction date and blocking non-scheme item rates may
   refuse quotations that pass today (manual item rates on packages). Mitigation: the check applies only to
   products with released versions; V814 backfills version 1 from today's values, so nothing changes until a
   version 2 is released; PQ10 confirms whether manual rates on packages are allowed at all.
2. **Projection drift.** The `cat_product` commercial columns mirror the current version. Mitigation: they are
   written only by the release step, and a guard rejects direct edits on packaged products (`PRODUCT_FIELD_VERSIONED`);
   an IT asserts projection = current version after each release.
3. **Two incentive stores during the transition** (`bkg_incentive_rule`, `cat_incentive_criteria`). Mitigation: V871
   copies once and freezes the booking table; booking reads only the catalog service.
4. **Parallel edits to built modules** (quotation, account, booking) by P1-A while Operations agents extend booking
   contracts. Mitigation: P1-A's edits are additive (new nullable columns and fields, overloads); announce the
   `InvoiceBooked` additions to the Operations agents before merging.
5. **Scope creep into renewals.** BRPM.017 is the renewal of the *package*, not of client policies (Renewal BRD).
   Mitigation: RENEW requests only produce a new catalog version; account renewal stays parked.

## 15. P0 foundation: as built

What the P0 wave built, and where it differs from or details the sections above. P1-A and P1-B build on this.

- **Migrations.** `V755__product_maintenance_foundation.sql` (roles, grants, `sec_permission_action`, LOV types, workflow,
  parameters). The access-request change of PMADD05 needs `nba_access_request` (V790), which does not exist when V755
  runs on a fresh database, so it is in the broking-administration range: `V791__nbadmin_role_permission_requests.sql`.
  The demo users of section 6.2 are in `db/demo/V998__demo_product_maintenance_users.sql` (`tsuhead`, `mbs`, `mbs2`,
  `mancom`; `tsulead` also receives TSU_TL). V996 / V997 stay with P1-A / P1-B.
- **Additional grants** beyond the table of section 6.2: every new role has WORK_VIEW, ATTACHMENT_VIEW, REPORT_VIEW,
  CLIENT_VIEW and PRODUCT_VIEW; MKT_TL also has PKG_REQUEST (FOR_MKT_REVIEW is a Marketing stage); TSU_TL and
  TSU_HEAD have WORK_ASSIGN; TSU_TL and MBS have ATTACHMENT_MANAGE; MBS has MASTER_VIEW (the catalog screens read
  with MASTER_VIEW until P1-A switches them to PRODUCT_VIEW); NB_APPROVER has PRODUCT_VIEW and PRODUCT_AUTHORIZE (it
  authorises catalog records today); SYSADMIN and AUDITOR have read access.
- **Workflow additions** to section 7: stage `RETIRED` (terminal) with `WITH_MBS --retire--> RETIRED` for RETIRE
  requests (`PackageSetupService.retireProduct`); `FOR_TSU_APPROVAL --approve_no_negotiation--> FOR_MANCOM` (RETIRE / no
  negotiation); the catalog outcomes are the system actions `version_released` (FOR_VALIDATION -> RELEASED) and
  `version_returned` (FOR_VALIDATION -> WITH_MBS); `not_proceeded` requires a reason from the new LOV
  `PKG_NOT_PROCEEDED_REASON`; `return_incomplete` requires a RETURN_REASON; `void` is allowed from DRAFT to
  FOR_TSU_APPROVAL. `wf_stage.sla_hours` holds the SLA defaults (the engine reads it); the `PKG_SLA_*` parameters carry
  the same values for BDOI to confirm (PQ08), and P1-B copies a changed parameter into the stage when it wires them.
- **Action classes.** `sec_permission_action` classifies every permission of the broking and Operations areas;
  finance and platform permissions stay unclassified and show only in the permission view. The User Access Matrix
  has the views "By Permission" and "By Action" (`GET /api/v1/nbadmin/access-matrix/by-action?area=`), and the Excel
  export has one sheet per view.
- **Role-permission changes.** Access request type `MODIFY_ROLE_PERMISSIONS` (role, permissions added, permissions
  removed; stored as the effective change), four eyes, applied through `UserAdminService.updateRole` on the role as it
  is at approval. **Parked (PQ17):** restricting the direct `PUT /api/v1/admin/roles/{id}` with a parameter
  `ROLE_CHANGE_REQUIRES_APPROVAL` is not built and the parameter is not seeded: `security` cannot read `system`
  parameters without a module cycle, and the restriction waits for BDOI's answer.
- **Catalog contracts** (`catalog/service/version`): `PackageSetupService`, `ProductVersionQueryService`,
  `PackageSpec`, `VersionRef`, `ProductVersionView`, the events `ProductVersionReleased`, `ProductVersionReturned`,
  `ProductExpired`, `IncentiveCriteriaChanged`; the enums `catalog.domain.ProductVersionStatus` and
  `PackageInsurerRole`; `RatingQuery.Purpose`. `PackageSetupService` also has `retireProduct` for RETIRE requests.
  `PackageVersionStubDefaults` registers in-memory stubs (`@ConditionalOnMissingBean`) of both services until the
  catalog implements them; P2 deletes it.
- **Frontend.** The catalog section is renamed "Product Maintenance". The package request screens are registered in
  `features/productmaint/module.ts` (`withPackageRequests(catalogModule)`, one sidebar section) and the new catalog
  screens in `features/catalog/module.ts`, all hidden with a placeholder component. Help:
  `withPackageRequestHelp(CATALOG_HELP)` with the empty `PACKAGE_REQUEST_HELP_SCREENS` list for P1-B.

## 16. P1-A catalog and consumers: as built

Built note of the catalog wave (sections 4.1-4.3, 5 and 9.1-9.4). Where it differs from or details the sections above,
this note is what the code does.

### 16.1 Migrations

| Version | Content |
|---|---|
| `V813__catalog_hierarchy_and_clauses.sql` | `cat_cover_type.parent_code` (subtype, depth 2), `cat_product_line.code_pattern` (null until PQ02), check "packaged -> cover type", `cat_coverage` (Motor and Property defaults), `cat_clause` (six defaults), `cat_field_rule.rule_type / lov_type / min_value / max_value / pattern` |
| `V814__catalog_product_versions.sql` | `cat_product.lifecycle_status`, `cat_product_version` with `cat_package_coverage`, `cat_package_insurer`, `cat_package_insurer_term`; `cat_rate_scheme_exception`; backfill of version 1 RELEASED (effective 2020-01-01, validated by SYSTEM) for every packaged product |
| `V815__catalog_incentive_criteria.sql` | `cat_incentive_criteria`, `cat_incentive_criteria_product`, INCENTIVE_TYPE `MIGRATED`, alert code `INCENTIVE_PRODUCT_INACTIVE` |
| `V821` / `V831` | `acc_account` and `quo_quotation`: `product_version_no`, `rate_override_ref` |
| `V871__booking_incentive_criteria.sql` | `bkg_invoice.product_version_no`, `incentive_criteria`; one-time copy of `bkg_incentive_rule` into the catalog (code `MIG-<id>`, PENDING_AUTHORIZATION) |
| `db/demo/V996__demo_product_versions.sql` | MTR12 version 2 DRAFT set up by `mbs` (two panel insurers, terms, clauses), criterion `CPC2 (demo)` ACTIVE on MTR10 (CBG), MTR12, PAR19, PAR25; the demo booking rule copied like V871; PAR25 version 1 ends 45 days after the load date (Package Expiry list) |

### 16.2 Behaviour (differences and details)

- **Manual item rates (PQ10).** `cat_product_version.manual_rate_allowed` is true only on the backfilled versions 1, so
  today's quotations and accounts behave as before. Versions set up from now on lock the item rate to the scheme
  (or panel insurer) rate: another rate needs an approved rate exception.
- **Draft versus submission.** Rating has two entry points: `RatingService.rate` (strict: a deviating item rate is
  refused with `RATE_SCHEME_NOT_CURRENT`) and `rateDraft` (the deviation is priced and reported in
  `Rating.schemeDeviation`). Quotation and account drafts are priced with `rateDraft`; `submit` of a quotation and
  `submit` / `resubmit` of an account run the strict check, and refuse a content priced on a version that is no longer
  current. The approved exception is found by the transaction reference (quotation number or ARN) and stored on the
  record (`rate_override_ref`).
- **Version resolution** (`SchemeResolver`): NEW_BUSINESS takes the version in force on the clock date by selling
  period (so rating does not depend on the daily job); a version other than the current one only with an approved
  exception that names it. ENDORSEMENT without a version (accounts created before versions) takes the version in force
  on the rating date. The 12-argument `RatingQuery` constructor keeps compiling: its purpose is ENDORSEMENT when the
  `endorsement` flag is set, else NEW_BUSINESS. Packaged products without any version (created in the old product
  editor) are rated from the product columns until a version exists.
- **Checkpoint.** Submission and validation check completeness (section 5.1) and that the effective date is today or
  later and after the latest released version. The "required templates exist" item is a checklist item of the
  validator (the catalog does not depend on `docgen`). Release ends the previous version the day before (SUPERSEDED at
  once when that day has passed), projects the scheme on the product when effective today (else in the daily step),
  and authorises a new product created by the set-up (the validator is the checker). The validator may not be the
  version's creator or submitter (`MAKER_CHECKER_VIOLATION`).
- **Daily step.** `ProductVersionService.expireDue(date)` supersedes ended versions, projects versions that took effect
  and expires packages whose end date passed (product EXPIRED, `ProductExpired`, `INCENTIVE_PRODUCT_INACTIVE` alerts).
  The catalog runs it with its own ManagedJob `PACKAGE_VERSION_LIFECYCLE` on the same schedule as
  `PACKAGE_EXPIRY_MONITOR` (`brokerverse.jobs.package-expiry-cron`); `productmaint` reacts to `ProductExpired`.
- **Retirement** (`retireProduct`) sets RETIRED and flags the product's incentive criteria.
- **Product list.** `GET /products` lists sellable (lifecycle ACTIVE) products; `lifecycle=EXPIRED|RETIRED` needs
  PRODUCT_ARCHIVE_VIEW. Each row carries `lifecycleStatus`, `currentVersionNo`, `openVersionNo / Status` and the package
  end date.
- **Draft content API.** One `PUT /products/{code}/versions/{n}` replaces the whole draft (rate scheme, dates,
  coverages, insurers, insurer terms) instead of three sub-resources; "New Version" (`POST .../versions`) copies the
  version in force, effective tomorrow.
- **Incentive amendments.** `PUT /incentive-criteria/{id}` changes a pending row or amends an active one (successor);
  the active row is end-dated when the successor is authorised, not when it is requested, so an unauthorised
  amendment never leaves a gap.
- **Maker-checker by kind.** `CatalogKind` names who maintains and authorises each kind: coverages, clauses and rate
  exceptions PRODUCT_MAINTAIN / PRODUCT_AUTHORIZE, incentive criteria INCENTIVE_CRITERIA_MAINTAIN / PRODUCT_AUTHORIZE,
  the older product-area kinds accept both the MASTER_* and PRODUCT_* permissions. A rate exception is rejected by
  deactivating it (PRODUCT_AUTHORIZE).
- **Typed field rules (BRPM.004).** `ProductRuleService.violations` checks LOV, RANGE and PATTERN rules; the account
  checks (`AccountChecks`) add them to the minimum-field errors.
- **Booking.** The incentive flag is "at least one catalog criterion matches" (product, cover type, segment, channel,
  lead insurer, booking date); the invoice stores the codes and the account's version, and endorsements inherit both.
  `POST / PUT /booking/setup/incentive-rules` answer 422 `INCENTIVE_RULES_FROZEN`; the GET stays.

### 16.3 Contracts for P1-B and the integration wave

- `PackageSetupService` is implemented by `catalog.service.version.CatalogPackageSetupService` and
  `ProductVersionQueryService` by `ProductVersionQueries` (both `@Primary`, so the P0 stubs are inactive; P2 deletes
  `PackageVersionStubDefaults`). `createDraftVersion` needs PRODUCT_MAINTAIN; with `newProduct` it creates the package
  (pending authorisation until its first version is validated).
- `ProductVersionService`: `submitForValidation(code, n)`, `validate(code, n, checklist)`, `returnToDraft(code, n,
  reason)`, `expireDue(date)` returning `ExpiryOutcome(superseded, projected, expired)`.
- Events `ProductVersionReleased`, `ProductVersionReturned`, `ProductExpired`, `IncentiveCriteriaChanged` are published
  with Spring's publisher inside the catalog transaction: listeners use `@TransactionalEventListener(phase =
  AFTER_COMMIT)` (and their own transaction for writes), as for `InvoiceBooked`.
- Rating: `RatingQuery(... , Purpose purpose, Integer schemeVersion, String rateOverrideRef)` and `withScheme(...)`;
  `Rating` + `schemeVersion`, `schemeRate`, `overrideRef`, `schemeDeviation`; `RatingService.rateDraft`,
  `testPremium`. Operations `adjustment` should pass `Purpose.ENDORSEMENT` and `account.getProductVersionNo()`.
- `RateSchemeExceptionService`: `request`, `requireApproved`, `latestApproved(transactionRef, product)`, `list`.
- `IncentiveCriteriaService.matching(companyId, IncentiveFacts)` returns the matched codes (Operations `commission`
  can reference `cat_incentive_criteria.code`, OQ39).
- `ProductCatalogService.requireSellable(code, Purpose, date)` (`PRODUCT_NOT_SELLABLE`).
- Consumers: `NewAccount` + `productVersionNo`, `rateOverrideRef` (six-argument constructor kept);
  `Account.getProductVersionNo()` / `getRateOverrideRef()`; `QuotationContent.schemeVersion` / `schemeDeviation`;
  `Quotation.getProductVersionNo()` / `getRateOverrideRef()`; `QuotationSummary.productVersionNo`;
  `InvoiceFacts.productVersionNo`; `InvoiceFlags.incentiveCriteria` (`incentiveCriteriaCodes()`); `InvoiceBooked` +
  `productVersionNo`, `incentiveCriteria` (additive, last components).

### 16.4 Screens

Products (lifecycle filter, version column), Product page (summary, Versions / Features / Field Rules & Documents
tabs, New Version), Version editor (`/catalog/products/:code/versions/:n`: rate scheme and dates, coverages,
insurers, insurer terms, validation checklist), Validation Queue, Coverages & Clauses, Incentive Criteria (current /
pending / history, amendment, deactivation), the calculator's version line and picker, and the quotation's rate
scheme panel with "Request Rate Exception". Field rules gained the check type (presence, list, range, pattern).

### 16.5 Tests

`CatalogVersionIT`, `RatingSchemeIT`, `IncentiveCriteriaIT` (with the booking stamp of CPC2), `CatalogProductMaintenanceApiIT`
and the Vitest suite `productMaintenanceForms.test.ts`. `BookingApiIT` was changed for the frozen incentive rules.

### 16.6 Parked

PQ02 (risk-code patterns: column empty), PQ04 (CPC2 content: demo only), PQ09 (final checklist and validator role),
PQ10 (manual item rates and statutory rates in the scheme), PQ11 (renewal of accounts on superseded versions: the
RENEWAL purpose is the seam), BRPM.022 (`ProductMasterFeed` port: not part of this wave).
