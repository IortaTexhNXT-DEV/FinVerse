# iNXT BrokerVerse - BDOI Employee Benefits (BRD-8) Build Design

Status: **proposal for review**, built in waves (section 16). This design extends `docs/architecture/BROKING_ARCHITECTURE.md`, `docs/architecture/OPERATIONS_DESIGN.md`, `docs/architecture/COLLECTIONS_DESIGN.md` and the Developer Guide, which stay binding. It does not change them, except for the contract changes listed in section 11.

Drop: BDOI places "Employee Benefits (no portal feature)" in **Drop 2** (requirements Dec 2026 - Feb 2027, build Mar - Apr 2027, SIT Jul - Sep 2027, UAT Oct - Nov 2027). EB placement and the EB upstream reports are in Drop 1 items 1.U6 and 1.U9 (DCR-235). The partner portal of the first proposal is removed from this design (section 16.1): insurers and client HR send their files by e-mail or an agreed channel and the EB users upload them.

Requirements baseline: [`BDOI_EB_BRD_SPEC.md`](../requirements/BDOI_EB_BRD_SPEC.md): 34 requirement rows (BRID-001 to 030, 005.01-005.03, 022.01; BRID-028 OUT) and questions EBQ01-EBQ28. Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRID-016`.

## 1. Design principles

1. **The EB cycle ends where the BRD-1 spine starts.** Everything before the client's decision is new: renewal advice, BOR, franchise, TOR, insurer requests and proposals, comparative, revisions, threshold approval. The client's confirmation turns the chosen proposal into **accounts** through `AccountService.createDraft`, one per benefit line. Placement, issuance, booking, the invoice ledger, cashiering, collections and commission then work unchanged. EB never posts a journal itself.
2. **External parties never touch BIBS.** Insurers and client HR users have no BIBS access. They send their files by e-mail or an agreed channel, and internal EB users upload them on the programme, cycle or member change with the source INSURER or CLIENT (BRID-005.01, 014 through internal upload). Nothing takes effect before an internal user validates it (principle 3).
3. **Nothing external takes effect before an internal user validates it (BRID-005.01 AC7, 014 AC7).** Files received from insurers and clients are uploaded by the EB users and become business data only through an EB action. Insurer proposals entered by the AO are SUBMITTED until validated; client master lists are staged roster versions until the AO accepts them.
4. **Versions, not updates.** TOR, BOR, proposals, comparatives and member rosters are versioned rows that are never changed after submission (BRID-008, 015, 024). The history tab and the diff come from the rows.
5. **Documents carry their context.** Every EB document is registered with its type, process (placement, renewal, endorsement, adjustment, franchise) and access class (Marketing, Processing, Collection) (BRID-025). The platform attachment module enforces the class for every module, not only EB.
6. **Parked means seam, not simulation.** The insurer API, e-signature verification of the BOR, HRIS master list feeds, reading insurer mailboxes and the EBIX booking each get a seam. No integration is simulated.
7. **Personal data is sensitive.** Master lists and utilization reports hold employees' personal and health-related data. Access classes, download logging and retention rules apply from day one (spec section 8).

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (seed) |
|---|---|---|---|---|
| `eb` (new, package `com.iortatechnxt.brokerverse.eb`, tables `eb_*`) | EB programmes and cycles, RA and reminders, feedback, document register, BOR, franchise, TOR, insurer requests, proposals and revisions, comparative and approvals, value thresholds, client confirmation and placement trigger, submissions, member roster and member changes, tracked items, SOA intake, EB reports and jobs | BRID-001-004, 007-010, 012, 013, 016, 017, 019, 021, 022, 024, 026, 027, 029, 030 | crm, catalog, account, booking (read), opsledger (read, events), adjustment (endorsement intake), issuance (e-policy receipt), workflow, approval, messaging, docgen, attachment, bulk, report, lov, alert, system, nbadmin (retention port) | V1030, V1031, V1033-V1036 (V1930-V1932) |
| `account` (built) | Business type on the account: the shared work item **BT0** (`V822__account_business_type.sql`, account range), also used by Renewal and Submitted Policies; EB creates no column of its own | BRID-022.01 | unchanged | V822 (BT0) |
| `booking` (built) | Business type from the account (part of BT0); + insurer billing number with duplicate block (EB) | BRID-020, 022.01 | unchanged | V822 (BT0, code only); V1031 (billing number) |
| `attachment` (platform) | + document access classes; + process tag on links | BRID-025 | unchanged | V1031 |
| `messaging`, `report`, `nbreport` (built) | + DOCX protection; + DOCX export (EB); + Business Type filter (part of BT0) | BRID-007, 022.01 | unchanged | none |

### 2.1 Dependency graph (arrows = "depends on")

```
                     crm  catalog  account  booking(read)  opsledger(read)  adjustment  issuance
                       \      |       |         |               |              |          |
                        +-----+-------+---------+------ eb -----+--------------+----------+
```

- `eb` calls BRD-1 and Operations services only through their public services (`AccountService`, `AccountLifecycleService` is **not** used by EB; `EndorsementRequestService.create`, `EpolicyService.receive`, `InvoiceLedgerQueryService`).
- No BRD-1 or Operations module depends on `eb`. EB learns about account progress from `AccountStatusChanged` and about payments from `InvoiceMovementPosted` (Spring events after commit).

### 2.2 Ports

EB declares none. It implements the platform ports `crm.service.ClientRecordsProvider` (`EbClientRecords`: the programmes on the client 360 view) and `nbadmin.service.RetentionCandidateProvider` (`EbProgrammeRetentionProvider`), and inside the module the port `eb.cycle.service.BorGate` (implemented by the BOR service; section 16.6). The portal ports of the first proposal are removed with the portal (section 16.1).

## 3. Flyway allocation

Allocated to Employee Benefits in the Developer Guide: **schema V1030-V1039, seed V1930-V1939.** Seven schema versions are used; V1037-V1039 stay free for follow-ups. One block is enough.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1030__eb_foundation.sql` | E0 | Grants of the EB permissions to new and existing roles; roles `EB_AO`, `EB_TL`, `EB_MANAGEMENT`, `EB_PROCESSOR`, `EB_PROC_SUPERVISOR`, `EB_COLLECTION`; LOV types and values (section 8.4); `DOCUMENT_TYPE` values (spec 6.1); `sys_parameter` rows; exception codes; workflows `EB_CYCLE`, `EB_FRANCHISE`, `EB_MEMBER_CHANGE`, `EB_SOA` (`wf_stage`, `wf_transition`); `doc_template` rows; notification events |
| `V1031__eb_platform_extensions.sql` | E0 | `att_document_access` (document type, permission) and `att_attachment_link.process_tag`; the cross-BRD document types `RENEWAL_ADVICE` (Renewal, EB) and `CLAIM_REPORT` (Claims) with their access rows, including the CSF view permission (insert ... on conflict do nothing, so the order against V1010 / V1020 does not matter; final matrix: cross-BRD question XQ04); `bkg_invoice.insurer_billing_no varchar(60)` and a unique partial index on (company_id, insurer_code, insurer_billing_no) where not null. **Not** the account business type: `acc_account.business_type` is the shared `V822__account_business_type.sql` (BT0, cross-BRD decision D1), which must be merged before E1-B creates accounts |
| `V1032` | - | Reserved: the partner portal is not built (Drop 2 has no portal; section 16.1) |
| `V1033__eb_programmes_cycles.sql` | E0 (moved from E1-B, section 16.4) | `eb_programme`, `eb_programme_line`, `eb_programme_contact`, `eb_cycle`, `eb_cycle_account`, `eb_document`, `eb_activity_log` |
| `V1034__eb_renewal_advice_feedback_bor.sql` | E1-B | `eb_renewal_advice`, `eb_feedback`, `eb_bor` (as built, section 16.6). The marketing tables of the proposal (`eb_franchise_request`, `eb_tor`, `eb_tor_item`, `eb_insurer_request`, `eb_proposal` and its lines, items and factors, `eb_revision_*`, `eb_comparative`, `eb_comparative_signoff`, `eb_comment`, `eb_threshold_rule`, `eb_client_confirmation`, `eb_confirmation_line`, `eb_submission`, `eb_submission_document`, `eb_required_document`) take a free version (V1037-V1039) with their wave |
| `V1035__eb_tracked_items.sql` | E1-C | `eb_tracked_item` (as built). The roster, member, member change and SOA tables of the proposal (`eb_roster_version`, `eb_member`, `eb_member_change`, `eb_member_change_line`, `eb_soa`) take a free version with their wave |
| `V1036__eb_reports_support.sql` | E1-C | Indexes and the read view `eb_tat_v` over `eb_activity_log` for `EB-TAT`; retention rule `EB_PROGRAMME` (`EB_MEMBER` follows with the roster); notification event `EB_BOR_DECIDED` |
| `V1037`-`V1039` | - | Kept free for the marketing and servicing tables still to build |
| `db/seed/V1930__seed_eb_reference.sql` | E1-B | SIT/UAT users of the EB roles (section 6.4). EB product lines and products (HMO, GLI, GPA), HMO providers as panel insurers, threshold rules and required documents follow with the product set-up (EBQ01) |
| `db/seed/V1931__seed_eb_cycles.sql` | E1-B | Six programmes of the V981 corporate clients and prospects: renewal advice sent and awaiting feedback, renewal due, new business in requirements, new business lost, feedback received, a programme not flagged for renewal (section 16.6) |
| `db/seed/V1932__seed_eb_servicing.sql` | E1-C | Tracked items: an HMO card past due with one follow-up, a pending billing, a received card replacement. Rosters, member changes and SOAs follow with their wave |

Rules:
- V1031 alters `bkg_invoice` (V870) and the attachment tables, which always exist before V1031 on a fresh database. The `booking` entity changes in the same wave (E0), because Hibernate validates the schema. The account business type comes from V822 (BT0), which runs before V1031.
- EB tables store ARN, invoice no., client code and insurer code as plain values, like Operations. Foreign keys only to platform tables (users, attachments, workflow, LOV) and to EB's own tables.
- The seed runs after every V9xx seed (V981 clients, V982 catalog, V988 booking), as the range table requires.

## 4. Entities (key fields)

Money is `numeric(19,2)`; every table has `company_id`, the audit columns and `version`.

### 4.1 Portal

Removed: BDOI Drop 2 has no partner portal (section 16.1). No `ptl_*` table is built.

### 4.2 `eb`: programme, cycle, documents

| Table | Key fields |
|---|---|
| `eb_programme` | programme no. `EBP-<yyyy>-nnnnnn`, client id and code, name, team (LOV `EB_TEAM`), funding EMPLOYER / VOLUNTARY, AO username and sales unit, renewal eligible flag, status PROSPECT / ACTIVE / LAPSED / LOST / INACTIVE |
| `eb_programme_line` | programme, benefit line (LOV `EB_BENEFIT_LINE`), product code, incumbent insurer code, current policy no., current ARN, period from / to, headcount |
| `eb_programme_contact` | programme, name, e-mail, mobile, role (HR_HEAD, HR_OFFICER, FINANCE), receives RA / SOA flags |
| `eb_cycle` | cycle no. `EBC-<yyyy>-nnnnnn`, programme, business type NEW_BUSINESS / RENEWAL, policy year, target inception, remarketing flag, stage (mirror of the `EB_CYCLE` case), outcome RENEWED_INCUMBENT / MOVED / NEW_PLACED / NOT_RENEWED / LOST with reason, account ARNs |
| `eb_renewal_advice` | cycle, sent at, recipients, message id, attachment id, reminders sent, last reminder at |
| `eb_feedback` | cycle, channel AO / EMAIL / PHONE / MEETING / LETTER, text, date received, number of files (stored as `EB_CLIENT_FEEDBACK` documents) |
| `eb_document` | cycle or programme, document type, process type (LOV `EB_PROCESS_TYPE`), version, attachment id, source AO / PROCESSING / CLIENT / INSURER / SYSTEM, status ACTIVE / SUPERSEDED / REJECTED |
| `eb_bor` | programme, cycle, version, attachment id, status PENDING / UPLOADED / VALIDATED / REJECTED, checklist answers (signed by authorised signatory, not blank, client name matches), validated by / at, valid from / to |
| `eb_activity_log` | cycle or programme, activity code (TAT annex, spec 6.3), received at, released at, actor; written by the services at each step (source of `EB-TAT`) |

### 4.3 `eb`: marketing

| Table | Key fields |
|---|---|
| `eb_franchise_request` | franchise no. `EBF-<yyyy>-nnnnnn`, cycle, insurer code, documents, submitted at, due at, status (mirror of `EB_FRANCHISE`), decided at, decided by (the AO, with the insurer's evidence), reason, client advised at |
| `eb_tor`, `eb_tor_item` | cycle, version, status DRAFT / RELEASED / SUPERSEDED; items: benefit line, plan code, item code, description, requirement, sort order |
| `eb_insurer_request` | request no. `EBR-<yyyy>-nnnnnn`, cycle, insurer code, TOR version, documents sent, sent at, due at, channel EMAIL, status OPEN / RESPONDED / DECLINED / CLOSED |
| `eb_proposal` | proposal no., cycle, request (null for the incumbent's indicative), insurer code, kind INCUMBENT_INDICATIVE / PROPOSAL / REVISED, version, revision request id, status SUBMITTED / VALIDATED / REJECTED / SUPERSEDED, validity, source AO (the insurer's e-mail attached) |
| `eb_proposal_line`, `eb_proposal_item`, `eb_proposal_factor` | per benefit line: annual premium, TSI (GLI / GPA), premium per plan; per TOR item: offered value, deviation flag, remark; per capability factor (LOV `EB_CAPABILITY_FACTOR`): value and rating |
| `eb_revision_request`, `_item`, `_target` | cycle, requested changes (TOR item or free text), target insurers with status OPEN / ANSWERED, answered by proposal version |
| `eb_comparative` | comparative no., cycle, version, snapshot (JSON of the rows), recommended proposal per line, status DRAFT / FOR_APPROVAL / THRESHOLD_APPROVAL / APPROVED / PRESENTED / SUPERSEDED, due at |
| `eb_comparative_signoff` | comparative, signatory, role, decision, time |
| `eb_comment` | comparative, author (internal user; client comments received by e-mail are entered by the AO), text, time, reply to |
| `eb_threshold_rule` | benefit line (blank = all), measure TSI / ANNUAL_PREMIUM, amount, currency, approver permission, level, effective from / to; `AuthorizableEntity` (maker-checker) |
| `eb_client_confirmation`, `eb_confirmation_line` | cycle, channel EMAIL / SIGNED_DOCUMENT, evidence attachment, recorded by the AO, per line: chosen proposal and insurer, created ARN |
| `eb_submission`, `eb_submission_document` | process type, insurer, cycle / member change, documents, sent at, acknowledged at |
| `eb_required_document` | process type x benefit line x document type, mandatory flag; maker-checker |

### 4.4 `eb`: servicing

| Table | Key fields |
|---|---|
| `eb_roster_version` | programme, policy year, version, source upload (bulk job), status STAGED / ACCEPTED / REJECTED, headcount, accepted by |
| `eb_member` | roster version, employee no., last / first name, birth date, gender, civil status, plan code, dependants count, effective from / to, status ACTIVE / DELETED; unique (programme, policy year, employee no.) in the accepted version |
| `eb_member_change`, `eb_member_change_line` | change no. `EBM-<yyyy>-nnnnnn`, programme, line, source AO / CLIENT, financial flag, direct-billed flag, status (mirror of `EB_MEMBER_CHANGE`), adjustment request id; lines: action ADD / DELETE / CHANGE_PLAN / CHANGE_DATA, member data, effective date |
| `eb_tracked_item` | type CONTRACT / HMO_CARD / CARD_REPLACEMENT / BILLING_INVOICE, programme, member or member change, responsible INSURER / CLIENT / BDOI, status PENDING / RECEIVED / RELEASED / CLOSED, due date, follow-ups sent, last follow-up at |
| `eb_soa` | SOA intake no. `EBS-<yyyy>-nnnnnn`, insurer, programme, insurer SOA no., period, amount, currency, attachment, file hash, invoice no(s)., status (mirror of `EB_SOA`), released at |

## 5. Accounting events and GL entries

EB adds **no event type and no rule**.

| EB business moment | Posting | Owner |
|---|---|---|
| Placement booked (after client confirmation) | `BROKER_BOOKING` per insurer share: Dr premium receivable (client) by component / Cr due to insurer; Dr commission receivable / Cr unrealised commission and deferred output VAT (per `OPS_COMMISSION_REALIZATION`); business type RENEWAL or NEW_BUSINESS carried on the event | `booking` (BRD-1), rules configured by Comptrollership |
| Member change with premium effect | Endorsement request raised by EB (`EndorsementRequestService.create`, source `EB`, reference `EBM-...`), recomputed and posted by Adjustment through `EndorsementPostingService` | `adjustment` (BRD-2) |
| Direct billing by the insurer | Account with the direct-payment arrangement (BRNB.114): no client PR; commission receivable from the insurer | `booking`, `commission` |
| Payment | Cashiering application; EB only reads the invoice ledger payment status | `cashiering` |

The guideline "no payment, no booking on adjustment" (p.3) is handled by the parameter `EB_ADJ_BOOKING_REQUIRES_PAYMENT` (default false until EBQ16 is answered). When true, EB raises the endorsement request only after the member change's billing is marked paid.

## 6. Security

### 6.1 Internal permissions (added to `security.domain.Permission` in E0; granted in V1030)

| Permission | Used for |
|---|---|
| `EB_VIEW` | EB screens, programmes, cycles, documents (subject to access classes) |
| `EB_MARKET` | AO maker actions: programmes, RA, feedback, BOR, franchise, TOR, requests, proposals entry and validation, comparative, revisions, confirmation, placement trigger, member changes |
| `EB_COMPARATIVE_APPROVE` | Authorised signatory of the comparative (four eyes) |
| `EB_THRESHOLD_APPROVE` | BDOI Management approval above the value threshold |
| `EB_PROCESS` | Processing: validate member changes, policy forms and SOA; completeness checks |
| `EB_COLLECT` | Collection: SOA and billing view, release acknowledgement |
| `EB_SETUP` | Threshold rules, required documents, EB parameters and templates (authorised with `MASTER_AUTHORIZE`) |
| `EB_REPORT_VIEW` | EB reports |

### 6.2 Roles (V1030) and SIT/UAT users (V1930)

| Role | Persona | Main permissions | SIT/UAT user |
|---|---|---|---|
| `EB_AO` | Marketing Account Officer (EB) | EB_VIEW, EB_MARKET, EB_REPORT_VIEW, CLIENT_VIEW, CLIENT_MAINTAIN, ACCOUNT_VIEW, ACCOUNT_MAINTAIN, ATTACHMENT_VIEW, ATTACHMENT_MANAGE, WORK_VIEW | `ebao`, `ebao2` |
| `EB_TL` | Marketing TL / UH (EB) | EB_VIEW, EB_COMPARATIVE_APPROVE, EB_REPORT_VIEW, WORK_VIEW, WORK_ASSIGN | `ebtl` |
| `EB_MANAGEMENT` | BDOI Management (threshold approver) | EB_VIEW, EB_THRESHOLD_APPROVE, EB_REPORT_VIEW | `ebmgmt` |
| `EB_PROCESSOR` | Processing (EB) | EB_VIEW, EB_PROCESS + the BRD-1 `PROCESSOR` permissions | `ebproc` |
| `EB_PROC_SUPERVISOR` | Processing Supervisor | EB_PROCESSOR permissions + WORK_ASSIGN | `ebprocsup` |
| `EB_COLLECTION` | Collection (EB) | EB_VIEW, EB_COLLECT + `CLX_VIEW`, `CLX_WORK` | `ebcoll` |
| `BUSINESS_ADMIN` (existing) | Business Administrator | + EB_SETUP | `badmin` |

### 6.3 Portal realm

Removed with the portal (section 16.1). No second filter chain or token audience is built. User Access requests of user type EXTERNAL (decision D7, dormant) stay refused by the default `ExternalUserProvisioner` of `nbadmin`.

### 6.4 SIT/UAT users (V1930)

`ebao` and `ebao2` (EB_AO), `ebtl` (EB_TL), `ebmgmt` (EB_MANAGEMENT), `ebproc` (EB_PROCESSOR), `ebprocsup` (EB_PROC_SUPERVISOR), `ebcoll` (EB_COLLECTION); the Business Administrator `badmin` holds EB_SETUP. No portal user.

## 7. Workflows (`wf_stage` / `wf_transition`, seeded in V1030)

### 7.1 `EB_CYCLE` (BRID-001-004, 008-012, 016, 017)

```
OPEN --send_ra (job / AO)--> RA_SENT --record_feedback--> REQUIREMENTS          (RENEWAL)
OPEN --start--> REQUIREMENTS                                                   (NEW_BUSINESS)
REQUIREMENTS --stay_with_incumbent--> INCUMBENT_TERMS                          (RENEWAL, no remarketing)
REQUIREMENTS --remarket--> FRANCHISE         (needs a VALIDATED BOR: EB_BOR_REQUIRED)
FRANCHISE --release_tor--> PROPOSALS         (at least one franchise APPROVED; TOR released)
INCUMBENT_TERMS | PROPOSALS --build_comparative--> COMPARATIVE
COMPARATIVE --submit--> FOR_SIGNOFF --approve--> THRESHOLD_APPROVAL (rule matched) | READY_TO_PRESENT
THRESHOLD_APPROVAL --approve--> READY_TO_PRESENT
READY_TO_PRESENT --present--> WITH_CLIENT
WITH_CLIENT --request_revision--> REVISION --build_comparative--> COMPARATIVE
WITH_CLIENT --confirm--> CONFIRMED --trigger_placement--> IN_PLACEMENT --(system: all accounts BOOKED)--> PLACED
FOR_SIGNOFF | THRESHOLD_APPROVAL --return(reason)--> COMPARATIVE
any open stage --close_lost(reason)--> CLOSED_LOST ; RENEWAL stages --not_renewed(reason)--> NOT_RENEWED
```

- `approve` on FOR_SIGNOFF needs `EB_COMPARATIVE_APPROVE` and is refused to the maker (four eyes); on THRESHOLD_APPROVAL it needs the rule's approver permission (default `EB_THRESHOLD_APPROVE`).
- `confirm` re-evaluates the threshold rules on the chosen proposals; a newly matched rule sends the cycle back to THRESHOLD_APPROVAL.
- `trigger_placement` needs the placement documents of `eb_required_document` for RENEWAL_PLACEMENT / NB_PLACEMENT and, where the chosen provider is not accredited, the `EB_ISACOM_APPROVAL` document (EBQ24).

### 7.2 Others

| Workflow | Stages | BRD |
|---|---|---|
| `EB_FRANCHISE` | DRAFT -> SUBMITTED -> APPROVED / REJECTED; SUBMITTED -> EXPIRED (job, after `EB_FRANCHISE_TAT_DAYS` + grace); APPROVED / REJECTED -> ADVISED | BRID-026, 027, 029 |
| `EB_MEMBER_CHANGE` | CAPTURED -> RELAYED -> BILLED (insurer billing / direct billing uploaded) -> VALIDATED (Processing) -> CLOSED; returns to CAPTURED with a reason; CANCELLED | BRID-013, 025 |
| `EB_SOA` | RECEIVED -> VALIDATED (Processing) -> RELEASED (client and Collection); RECEIVED -> REJECTED (reason) | BRID-021 |

## 8. Jobs, alerts, parameters, LOVs, numbers, templates, bulk handlers

### 8.1 Managed jobs

| Job | Default cron (PHT) | Does |
|---|---|---|
| `EB_RENEWAL_ADVICE` | daily 06:00 | Opens RENEWAL cycles for eligible programmes whose expiry is `EB_RA_LEAD_DAYS` away, sends the RA, stores it as `RENEWAL_ADVICE` (linked to programme, current ARN and client), and sends reminders at `EB_RA_REMINDER_DAYS` while no feedback is recorded |
| `EB_ITEM_FOLLOWUP` | daily 07:00 | Follow-up e-mails for tracked items past due (`EB_FOLLOWUP_DAYS`), escalation after `EB_FOLLOWUP_MAX` |

Crons are configurable (`brokerverse.jobs.eb-renewal-advice-cron`, ...), documented in `docs/operations/CONFIGURATION.md` by the build team.

### 8.2 Alerts (`alt_exception_code`, V1030) and daily checks (`AlertCheck`)

`EB_RA_NOT_SENT` (inside the lead time, no RA: no contact, no flag), `EB_FRANCHISE_OVERDUE`, `EB_PROPOSAL_OVERDUE` (request past due), `EB_COMPARATIVE_LATE` (`EB_COMPARATIVE_DAYS` after the last proposal), `EB_SOA_VALIDATION_LATE` (`EB_TAT_SOA_VALIDATION`), `EB_ITEM_OVERDUE`.

### 8.3 Parameters (`sys_parameter`, category EMPLOYEE_BENEFITS)

`EB_RA_LEAD_DAYS` (135), `EB_RA_REMINDER_DAYS` (INTEGER_LIST 120, 105, 90), `EB_PROPOSAL_REPLY_DAYS` (5), `EB_FRANCHISE_TAT_DAYS` (5), `EB_FRANCHISE_ADVICE_DAYS` (2), `EB_COMPARATIVE_DAYS` (3), `EB_FOLLOWUP_DAYS` (5), `EB_FOLLOWUP_MAX` (3), the TAT parameters of spec 6.3, `EB_ADJ_BOOKING_REQUIRES_PAYMENT` (false), `BOOKING_BILLING_NO_LINES` (CODE_LIST of product lines that require the insurer billing number; EB lines). All defaults are placeholders until EBQ02, EBQ07, EBQ10, EBQ16 and EBQ20 are answered.

### 8.4 LOVs, numbers, templates, bulk handlers

- LOVs: `EB_BENEFIT_LINE` (HMO, GLI, GPA), `EB_TEAM` (BDO, SM, VOLUNTARY, SOLICITED, NEW_BUSINESS), `EB_PROCESS_TYPE` (NB_PLACEMENT, RENEWAL_PLACEMENT, ENDORSEMENT, ADJUSTMENT, FRANCHISE, PROPOSAL), `EB_CAPABILITY_FACTOR` (COMPANY_STABILITY, CLINIC_PROVIDERS, HOSPITAL_NETWORK, TECHNOLOGY), `EB_LOST_REASON`, `EB_MEMBER_CHANGE_TYPE`, `EB_TRACKED_ITEM_TYPE`, `EB_FRANCHISE_REJECT_REASON`, `EB_SOA_REJECT_REASON`; `DOCUMENT_TYPE` values of spec 6.1.
- Numbers (`DocumentNumberService`): `EBP-<yyyy>`, `EBC-<yyyy>`, `EBF-<yyyy>`, `EBR-<yyyy>`, `EBPR-<yyyy>` (proposal), `EBCA-<yyyy>` (comparative), `EBM-<yyyy>`, `EBS-<yyyy>`.
- Templates (`doc_template`): `EB_RENEWAL_ADVICE`, `EB_RA_REMINDER`, `EB_INDICATIVE_PROPOSAL`, `EB_TOR`, `EB_RFP_COVER`, `EB_FRANCHISE_REQUEST`, `EB_FRANCHISE_ADVICE`, `EB_COMPARATIVE`, `EB_REVISION_RELAY`, `EB_ITEM_FOLLOWUP`. Contents are placeholders until BDOI supplies the layouts (EBQ21).
- Bulk handlers: `EB_MASTERLIST` (template with the roster columns; creates a STAGED roster version; permission EB_MARKET; the AO uploads the list the client sends), `EB_MEMBER_CHANGE` (lines of one member change), `EB_PROGRAMME_LOAD` (go-live migration of existing EB programmes and current policies from EBIX).

## 9. Reports (category `EMPLOYEE_BENEFITS`, package `eb.report`)

`EB-PRODUCTION`, `EB-RENEWAL`, `EB-PLACEMENT`, `EB-NEW-BUSINESS`, `EB-TAT`, `EB-PENDING-ITEMS`, `EB-FRANCHISE` (spec section 7). Common parameters: company, date range, team, AO, client, benefit line, insurer, business type. They read the EB tables and `bkg_invoice` with constant SQL aggregates (the `nbreport` pattern). Export PDF / XLSX / CSV / ODS / XML and, after E0, DOCX.

## 10. Screens (BDOI navigation)

### 10.1 Internal (group **Client & Policy**, section **Employee Benefits**, after Non-Package Management; UX-3)

| Screen | Route | Content | Permission |
|---|---|---|---|
| EB Home | `/eb` | Tiles: RA due, awaiting feedback, franchise pending, proposals outstanding, comparatives to sign off, threshold approvals, with client, member changes open, pending items overdue | EB_VIEW |
| Programmes | `/eb/programmes` | Work list with status tabs Renewal Due / In Progress / With Client / In Placement / Placed / Lost; toolbar and bulk "Send RA" | EB_VIEW |
| New Programme | `/eb/programmes/new` | Client picker (or "New Client" to crm), lines, team, contacts | EB_MARKET |
| Programme page | `/eb/programmes/:id` | Record summary card; `WorkflowPanel` of the current cycle; tabs Cycle / Documents / BOR / Franchise / Insurer Requests / Proposals / Comparative / Members / Member Changes / Billing & SOA / Pending Items / History | EB_VIEW |
| Comparative | `/eb/comparatives/:id` | Matrix, recommendation, sign-off, threshold approval, client comments, PDF / XLSX | EB_VIEW |
| Member Changes | `/eb/member-changes` | Work list, entry and upload (`/bulk/EB_MEMBER_CHANGE`) | EB_VIEW |
| Pending Items | `/eb/pending-items` | Tracked items by programme / member, filters, manual update | EB_VIEW |
| SOA Register | `/eb/soa` | Intake, validation, release | EB_PROCESS or EB_COLLECT |
| EB Setup | `/eb/setup` | Threshold rules, required documents, EB parameters | EB_SETUP |

- No Portal Users screen and no Portal Uploads queue: the portal is removed (section 16.1).
- **Reports** group: the EB reports appear in the Report Centre under "Employee Benefits".
- The crm client page gains the tab **Employee Benefits** through `ClientRecordsProvider` (`EbClientRecords`).
- Help entries in `frontend/src/features/eb/help.ts`, registered in `HELP_SECTIONS`.

### 10.2 Portal

Removed (section 16.1): no portal SPA is built. The insurer and client HR steps of the first proposal are done by the EB users on the internal screens (upload with the source INSURER or CLIENT, decisions recorded with the evidence).

## 11. Impact on modules already built or being built (contract changes)

| Module | Change | Owner (wave) | BRD |
|---|---|---|---|
| `account` | **Shared work item BT0** (cross-BRD decision D1; SUBMITTED_POLICIES_DESIGN section 9): `acc_account.business_type` (required, default NEW_BUSINESS) and `renewal_of_ref` in `V822__account_business_type.sql`; `Account.getBusinessType()`; the existing `NewAccount` factories keep NEW_BUSINESS, and EB creates the accounts of a RENEWAL cycle with `NewAccount.renewal(...)` (`renewal_of_ref` = the line's current ARN); `AccountResponse`, `AccountSearch` filter; bulk `ACCOUNT_CREATE` optional column. EB's earlier variant (column in V1031, a required `businessType` on every `NewAccount`) is dropped | BT0 (first of S0 / R0 / E0 to start) | BRID-022.01 |
| `booking` | Part of BT0: `InvoiceBuilder` takes `account.getBusinessType()` instead of the constant `BusinessType.NEW_BUSINESS` (`booking/service/InvoiceBuilder.java` line 129) and `InvoiceBooked` carries `businessType`. EB only: `BookingOptions.insurerBillingNo`, `bkg_invoice.insurer_billing_no` (V1031), duplicate block `BILLING_NO_DUPLICATE`, mandatory for the lines in `BOOKING_BILLING_NO_LINES`; `BOOKING_UPLOAD` column; booking notification on success / duplicate | BT0; E0 (billing number) | BRID-020, 022.01 |
| `nbreport` | Part of BT0: parameter Business Type on `NB-BOOKED-REG`, `NB-PRODUCTION`, `NB-PLC-UPDATE` (read `bkg_invoice.business_type`) | BT0 | BRID-022.01 |
| `report` | `ExportFormat.DOCX` and `DocxReportRenderer` (Apache POI XWPF, already a dependency through POI) | E0 | BRID-022.01 |
| `messaging` | `DocumentProtector.canProtect / protect` for DOCX (POI agile encryption, as XLSX); `DOCUMENT_NOT_PROTECTABLE` for other types when protection is requested | E0 | BRID-007 |
| `attachment` | `att_document_access` read by `DocumentService` for list, download and ZIP (document types without a row keep today's behaviour); `AttachmentLink.processTag`; `Attachments` component shows only allowed documents | E0 | BRID-025 |
| `security` | Permissions of 6.1 | E0 | BRID-023 |
| `adjustment` | None required: EB calls `EndorsementRequestService.create(RequestDraft)` with source reference `EBM-...`. Ask: a `source` / `sourceRef` field on the request so Adjustment lists show "from EB member change" | E1-C (ask to Operations owner) | BRID-013 |
| `issuance` | None required: validated insurer policy forms go to `EpolicyService.receive(...)` for the placed account | E2 | BRID-019, 005.01 |
| `catalog` / Product Maintenance | Configuration only: EB product lines (HMO, GLI, GPA, risk item kind PERSON / GENERIC), EB products, HMO providers as panel insurers with commission rates; seeds in V1930, production through Product Maintenance | E2 / BDOI | EBQ01 |
| `crm` | None: `EbClientRecords` implements `ClientRecordsProvider` | E1-B | BRID-006 |
| `nbadmin` | None: `EbRetentionProvider` implements `RetentionCandidateProvider` (EB_PROGRAMME, EB_MEMBER) | E1-C | NFR |
| `collections` (being built) | None: EB invoices enter the worklist like any invoice; Collection users see EB SOA and billing documents through the COLLECTION access class | - | BRID-021, 025 |
| `commission` | None: EB accounts billed directly by the insurer use the direct-payment arrangement | - | BRID-025 |
| `renewal` (Renewal BRD, designed, V1010-V1019) | **Decided (decision D3, EBQ28 closed for the boundary):** EB programmes are excluded from the general renewal candidate lists (`RNW_EXCLUDED_LINES` = the `EB_BENEFIT_LINE` values). Every renewal advice, EB's (job `EB_RENEWAL_ADVICE`) and Renewal's, is stored as document type `RENEWAL_ADVICE` linked to the account and the client (EB also links the programme), so CSF resends both. `RENEWAL_ADVICE` is seeded with `on conflict do nothing` by whichever of V1010 / V1030 runs first. Optional: a read method listing the current ARNs of EB programme lines would let Renewal exclude by ARN as well as by line | coordination | EBQ28 |
| User Access Maintenance (`security` / `nbadmin`, V1060-V1069) | Decision D7 is dormant: with no portal there are no external users; requests of user type EXTERNAL stay refused by the default `ExternalUserProvisioner` | - | EBQ13 (closed as not applicable) |

## 12. Integrations to park (seam only)

| Item | Seam | Question |
|---|---|---|
| Insurer system-to-system API | Not built; insurers send files by e-mail and the EB users upload them with the source INSURER | EBQ13 |
| E-signature verification of the BOR | `BorValidator` port; default = validator checklist attestation | EBQ06 |
| HRIS / client payroll feed of master lists | `EB_MASTERLIST` bulk handler (the AO uploads the list the client sends) | EBQ15 |
| Insurer mailbox reading (proposals, SOAs by e-mail) | AO uploads with the e-mail attached (`EML` / `MSG`) | - |
| EBIX booking (TAT annex) | Replaced by BIBS booking; `EB_PROGRAMME_LOAD` for migration | EBQ18 |
| Virus scanning product | `attachment.service.VirusScanner` adapter for the files uploaded on behalf of insurers and clients | - |

## 13. Build-wave plan

| Wave | Team | Owns (files) | Delivers | Depends on |
|---|---|---|---|---|
| E0 | Foundation | `db/migration/V1030__*`, `V1031__*`; `security/domain/Permission.java` (EB entries); the BT0 files (`account/**` business type only, `booking/service/InvoiceBuilder.java`, `InvoiceBooked.java`, the three `nbreport` reports, `V822`) **only if** no Submitted Policies S0 or Renewal R0 has merged BT0 first; `booking/service/BookingService.java`, booking DTOs and `BOOKING_UPLOAD` handler (billing number); `report/render/**`; `messaging/service/DocumentProtector.java`; `attachment/**`; `eb/package-info.java` and `eb/domain/EbDocumentTypes.java` (constants) | Section 11 platform changes, roles, workflows, LOVs, parameters | BT0 merged (or built here) |
| E1-B | EB marketing | `eb/domain` and `eb/service` classes for programme, cycle, RA, feedback, document, BOR, franchise, TOR, insurer request, proposal, revision, comparative, threshold, confirmation, submission, required document; `eb/api` for the same; `db/migration/V1033__*`, `V1034__*`; `frontend/src/features/eb/` except members, pending items, SOA and reports | BRID-001-004, 007-012, 015-017, 024, 026, 027, 029 | E0 |
| E1-C | EB servicing and reports | `eb/domain` and `eb/service` classes for roster, member, member change, tracked item, SOA; `eb/report/**`; EB jobs; `db/migration/V1035__*`, `V1036__*`; `frontend/src/features/eb/{members,pending,soa}*` | BRID-013, 019, 021, 022, 025 (EB side), 030 | E0 |
| E2 | Integration | End-to-end tests of the internal new-business and renewal flows, `ApiSmokeIT` entries, help entries, `docs/modules/EMPLOYEE_BENEFITS.md` (seed data V1930-V1932 delivered with E1-B / E1-C) | End-to-end NB and renewal cycles; seed storyline | E1-B, E1-C |

Parallel-work rules:
- E1-B and E1-C start together after E0 is delivered. Nobody but E0 edits `Permission.java`, `account/**`, `booking/**`, `attachment/**`; later needs go to E0's owner as a follow-up commit.
- E1-B and E1-C share `eb/` by class ownership as listed; shared constants live in `eb/domain/EbDocumentTypes.java` and `eb/service/EbParameters.java`, created by E0 and changed only by additions.
- Migrations use only the listed versions; a missing column in another wave's table is a new migration in V1037-V1039, agreed first.
- Every wave passes `mvn verify` and `npm run verify`; help entries are added with each screen.

## 14. What depends on information BDOI has not given (build the seam, park the content)

| Item | Question | What is built |
|---|---|---|
| RA lead time and reminders | EBQ02 | Parameters with defaults 135 days and three reminders |
| BOR on renewal, signature validation | EBQ05, EBQ06 | Gate for NB and remarketing only; checklist attestation |
| Franchise meaning and TAT | EBQ07 | Workflow and SLA parameter |
| TOR template | EBQ08 | Structured items maintained per cycle; template placeholder |
| Password convention | EBQ09 | Existing `DocumentPasswordPolicy` (generated) |
| Comparative factors and signatories | EBQ10 | LOV of factors; one sign-off stage |
| Threshold values and approvers | EBQ11 | Rule table with seed values TSI 500M, premium 20M |
| Member data and privacy rules | EBQ15 | Roster with access classes and download logs; retention rule placeholders |
| Movement types, "no payment no booking" | EBQ16 | LOV and parameter |
| Report layouts, TAT events | EBQ21 | Reports with the BRD columns; activity log stamps |

## 15. Risks

1. **Files from insurers and clients by e-mail.** Without a portal (Drop 2) the EB users upload what insurers and clients send. Mitigation: every upload carries its source, process and cycle; the access classes restrict who sees each document type; nothing takes effect before an internal user acts on it.
2. **Sensitive personal data.** Rosters and utilization reports. Mitigation: access classes, download logs, minimal roster fields until EBQ15 is answered, no health data in the roster.
3. **Boundary with the Renewal BRD.** Two renewal processes could send two RAs to the same client. Mitigation (decided, D3): EB lines excluded from the Renewal lists (EBQ28), one document type `RENEWAL_ADVICE` for every RA.
4. **Shared platform changes** (attachment access classes, DOCX export, business type on the account) touch every module. Mitigation: they are cross-BRD prerequisite work items (`BDOI_CROSS_BRD_DECISIONS.md` section 6); the business type is BT0 (V822), built once; E0 lands the access classes and DOCX export with defaults that keep today's behaviour.
5. **Undefined thresholds and TATs.** Mitigation: every value is a parameter or a rule row.

## 16. As built (BDOI Drop 2, no portal)

What the waves built, and where it details or differs from the sections above: the foundation E0 (16.1 to 16.5) and the
first business slice of E1-B and E1-C (16.6, 16.7).

### 16.1 Scope change: "Employee Benefits (No Portal Feature)"

BDOI's drop plan puts Employee Benefits in Drop 2 **without the partner portal**. Parked with the portal (no seam built;
the E1-A wave is deferred): the `portal` module and `V1032` (version kept reserved), the portal realm and second filter
chain (6.3), the portal permissions `PORTAL_USER_REQUEST`, `PORTAL_USER_APPROVE`, `PORTAL_ADMIN` (not added to
`Permission`: adding `PORTAL_USER_APPROVE` would switch the approver of EXTERNAL User Access requests in
`nbadmin.service.AccessApprovers`), the ports of 2.2, the workflow `PORTAL_UPLOAD_REVIEW`, the `PORTAL_*` parameters,
alert codes, reject reasons, notification event and invitation template, the job `PORTAL_INVITATION_EXPIRY`, the portal
SPA (10.2), the Portal Uploads queue and the Portal Users screen. The `nbadmin` port `ExternalUserProvisioner` keeps its
refusing default. What changes for the business waves:

| Feature | With the portal (design) | Drop 2 (as-built contract) | Wave |
|---|---|---|---|
| Client feedback, master list, utilization, member change requests from client HR | Portal uploads, staged and validated | Received by e-mail; the AO uploads them on the programme or member change with source `CLIENT` (`EbDocumentSource`); the master list through the bulk handler `EB_MASTERLIST` | E1-B, E1-C |
| Insurer proposals and revisions | Structured portal form, SUBMITTED until validated | The AO enters the proposal (source `AO`, the insurer's e-mail attached as `EML` / `MSG` or PDF) and validates it | E1-B |
| Franchise decision | Insurer approves or rejects in the portal | The AO records the insurer's decision with the evidence (`EB_FRANCHISE` actions `approve` / `reject`, reason list `EB_FRANCHISE_REJECT_REASON`) | E1-B |
| Comparative comments and confirmation | Client comments and confirms in the portal (channel SYSTEM) | Channels EMAIL and SIGNED_DOCUMENT only; the AO records the confirmation with its evidence | E1-B |
| Member change billing, policy forms, SOA | Insurer uploads | Processing or the AO upload them (`EB_DIRECT_BILLING`, `EB_POLICY_FORM`, `EB_SOA`, source `INSURER`) | E1-C |
| Notices to insurers and clients | Portal inbox and e-mail | E-mail only (the templates of 8.4 say "by e-mail") | E1-B, E1-C |
| EB Home tile "Portal uploads to review", alert `PORTAL_UPLOAD_WAITING` | Built | Not built | - |
| `EbUploadTargets`, `EbPortalTasks`, `eb/api/portal/**`, portal SIT/UAT users | E2 | Not built; E2 keeps the seed-data storyline and the end-to-end tests of the internal flow | E2 |

### 16.2 Shared work item BT0 (built here)

EB E0 was the first of S0 / R0 / E0 to start, so it built BT0 as section 6.1 of the cross-BRD decisions, in its own
commit:

- `V822__account_business_type.sql`: `acc_account.business_type` varchar(20) NOT NULL default `NEW_BUSINESS` (check
  NEW_BUSINESS / RENEWAL), `renewal_of_ref` varchar(40) (check: only a RENEWAL names it), `origin` varchar(30) NOT NULL
  default `DIRECT` (check QUOTATION, PROPOSAL, DIRECT, SUBMITTED_POLICY, EMPLOYEE_BENEFITS, RENEWAL; existing rows
  derived from `quotation_ref` / `proposal_ref`); indexes (company, business_type, status) and `renewal_of_ref`.
- `account.domain`: `BusinessType` (NEW_BUSINESS, RENEWAL), `AccountOrigin` (+ `AccountOrigin.of(Account.Origin)`),
  embeddable `AccountClassification(businessType, renewalOfRef, origin)` with `newBusiness(origin)` and `renewal()`;
  `Account.getBusinessType()`, `Account.getClassification()`; `Account.create(..., classification)`.
- `NewAccount` gains the component `classification` (null = new business of the references' kind). The 6- and
  8-argument constructors and `NewAccount.direct` are unchanged and create NEW_BUSINESS. New factories:
  `NewAccount.newBusiness(companyId, AccountOrigin origin, draft, premium, accountOfficer)` (EB new-business cycles use
  origin `EMPLOYEE_BENEFITS`), `NewAccount.renewal(companyId, AccountOrigin origin, draft, renewalOfRef,
  accountOfficer)` and the overload `NewAccount.renewal(..., Integer productVersionNo)`; `NewAccount.businessType()`.
- `AccountService.createDraft`: a RENEWAL request stamps the kept version before rating; `AccountPricing` rates a
  RENEWAL account with `RatingQuery.Purpose.RENEWAL` and the account's `productVersionNo` (SchemeResolver: that version
  while RELEASED or SUPERSEDED, else the current one); `requireScheme` uses the same purpose.
- `AccountResponse` + `businessType`, `renewalOfRef`, `origin`; `AccountSummaryResponse` + `businessType`;
  `AccountSearch` 16th component `businessType` (the 15-argument constructor stays); `GET /api/v1/accounts?businessType=`.
- Bulk `ACCOUNT_CREATE`: optional columns `Business Type` (NEW_BUSINESS default or RENEWAL, case-insensitive; error
  "Business Type must be NEW_BUSINESS or RENEWAL") and `Renewal Of`.
- Booking: `InvoiceBuilder` uses `booking.domain.BusinessType.of(account.getBusinessType())` for every policy year;
  `InvoiceBooked` gains the last component `businessType` (NEW_BUSINESS when null; the 27- and 28-argument constructors
  stay). The integration payload of `BookingEventAdapter` is unchanged (to add when ACSL needs it).
- `nbreport`: parameter `businessType` (select ALL / NEW_BUSINESS / RENEWAL, label "Business Type") on `NB-BOOKED-REG`
  (`bkg_invoice.business_type`), `NB-PRODUCTION` (`ProductionService.production(..., String businessType)`; targets
  unchanged) and `NB-PLC-UPDATE` (`acc_account.business_type`).
- Frontend: `api/accounts.ts` types `BusinessType` and `AccountOrigin`; filter "Business Type" on the account work
  list; a "Renewal" tag on the account record.

Renewal R0 and Submitted Policies S0 only check that BT0 is delivered. Renewal still adds its fast track,
`QueueSource.RENEWAL` and the quotation / proposal `renewalRef` (RENEWAL_DESIGN section 13). Submitted Policies uses
origin `SUBMITTED_POLICY` with `renewal_of_ref` = SBM number.

### 16.3 Platform items P2 / P3 and the booking billing number

- **P2 (DOCX).** Report export in Word was already built (Developer Guide 6.1-6.2). E0 adds DOCX to
  `messaging.service.DocumentProtector` (Office agile encryption, as XLSX); the refusal message is now
  "`<file>` cannot be password protected. Send it as PDF, Excel or Word" (FRS FR-EB-004).
- **P3 (access classes), V1031.** `att_document_access` (document_type, permission, access_class MARKETING, PROCESSING,
  COLLECTION, CLAIMS, SERVICING or AUDIT; unique type + permission). Entity `DocumentAccess`, component
  `attachment.service.DocumentAccessPolicy` (`visible`, `mayView`, `requireView`, `rows`). `DocumentService.list`
  filters; `DocumentService.download(id)` (new; the API download uses it) and `zip` refuse with `AccessDeniedException`
  (403) and an audit entry (action REJECT, recorded in its own transaction); `DocumentService.all(target)` and
  `documentTypesOf` stay unfiltered for mandatory-document checks. No authenticated user (jobs) means unrestricted.
  Process tag: `doc_attachment.process_tag` and `doc_attachment_link.process_tag` (the design's `att_attachment_link` is
  the existing `doc_attachment_link`); `UploadOptions.processTag` (5th component, 4-argument constructor kept), API
  parameter `processTag` on upload and batch upload, `LinkRequest.processTag`, `DocumentService.link(id, targets,
  processTag)`, `AttachmentResponse.processTag`. Rows: MARKETING = EB_MARKET, EB_COMPARATIVE_APPROVE,
  EB_THRESHOLD_APPROVE; PROCESSING = EB_PROCESS; COLLECTION = EB_COLLECT; AUDIT_VIEW on every EB type; the 16 EB types
  as spec 6.1; `RENEWAL_ADVICE`: the EB Marketing permissions, `RNW_VIEW`, `ACCOUNT_MAINTAIN`, EB_PROCESS,
  `ACCOUNT_PROCESS`, `CSF_VIEW`, AUDIT_VIEW; `CLAIM_REPORT`: `BCL_VIEW`, `BCL_REPORT_VIEW`, `CSF_VIEW`, AUDIT_VIEW (final
  matrix XQ04). `RNW_VIEW` and `CSF_VIEW` take effect when Renewal and CSF grant them.
- **Billing number (BRID-020).** `bkg_invoice.insurer_billing_no` varchar(60) + unique partial index
  `uq_bkg_invoice_billing_no` (company_id, insurer_code, insurer_billing_no) where not null. `BookingOptions` 5th
  component `insurerBillingNo` (+ `withBillingNo`), `BookRequest.insurerBillingNo`, `InvoiceResponse.insurerBillingNo`,
  `BookedInvoice.recordInsurerBillingNo` / `getInsurerBillingNo` (first-year invoice), component
  `booking.service.InsurerBillingNumbers`: `BILLING_NO_REQUIRED` "Enter the insurer billing number" for the lines of
  parameter `BOOKING_BILLING_NO_LINES` (CODE_LIST, default `HMO,GLI,GPA`, category BOOKING), `BILLING_NO_TOO_LONG`,
  `BILLING_NO_DUPLICATE` "Billing number `<no>` of `<insurer>` is already on invoice `<invoice>`"; notifications to the
  booker and the account officer (`BOOKING_BILLING_BOOKED`; `BOOKING_BILLING_DUPLICATE` in its own transaction); audit
  summary ", insurer billing no. `<no>`". `BOOKING_UPLOAD` optional column `Insurer Billing No`. Book Account screen
  field "Insurer billing no."; the invoice record shows it.

### 16.4 Employee Benefits foundation

- **Permissions** (`security.domain.Permission`): `EB_VIEW`, `EB_MARKET`, `EB_COMPARATIVE_APPROVE`,
  `EB_THRESHOLD_APPROVE`, `EB_PROCESS`, `EB_COLLECT`, `EB_SETUP`, `EB_REPORT_VIEW` (6.1 without the portal ones).
- **`V1030__eb_foundation.sql`.** Roles `EB_AO`, `EB_TL`, `EB_MANAGEMENT`, `EB_PROCESSOR`, `EB_PROC_SUPERVISOR`,
  `EB_COLLECTION`. Every EB role: EB_VIEW, EB_REPORT_VIEW, WORK_VIEW, ATTACHMENT_VIEW, REPORT_VIEW, CLIENT_VIEW,
  ACCOUNT_VIEW; EB_AO + EB_MARKET, CLIENT_MAINTAIN, ACCOUNT_MAINTAIN, ATTACHMENT_MANAGE, BULK_PROCESS; EB_TL +
  EB_COMPARATIVE_APPROVE, WORK_ASSIGN; EB_MANAGEMENT + EB_THRESHOLD_APPROVE; EB_PROCESSOR + every `PROCESSOR` grant,
  EB_PROCESS, ATTACHMENT_MANAGE, BULK_PROCESS; EB_PROC_SUPERVISOR the same + WORK_ASSIGN; EB_COLLECTION + EB_COLLECT,
  CLX_VIEW, CLX_WORK; BUSINESS_ADMIN + EB_VIEW, EB_REPORT_VIEW, EB_SETUP; SYSADMIN and AUDITOR + EB_VIEW,
  EB_REPORT_VIEW. Action classes area `EMPLOYEE_BENEFITS`. LOV types (owner permission EB_SETUP): `EB_BENEFIT_LINE`
  (HMO, GLI, GPA), `EB_TEAM` (BDO, SM, VOLUNTARY, SOLICITED, NEW_BUSINESS), `EB_PROCESS_TYPE` (NB_PLACEMENT,
  RENEWAL_PLACEMENT, ENDORSEMENT, ADJUSTMENT, FRANCHISE, PROPOSAL), `EB_CAPABILITY_FACTOR` (4), `EB_LOST_REASON` (7),
  `EB_MEMBER_CHANGE_TYPE` (ADD, DELETE, CHANGE_PLAN, CHANGE_DATA), `EB_TRACKED_ITEM_TYPE` (4),
  `EB_FRANCHISE_REJECT_REASON` (4), `EB_SOA_REJECT_REASON` (4); the 16 `DOCUMENT_TYPE` values of spec 6.1 (`EB_*`, sort
  101-116). Parameters (category EMPLOYEE_BENEFITS): those of 8.3 without `PORTAL_*`, with `EB_RA_REMINDER_DAYS` stored
  ascending `90,105,120` (INTEGER_LIST rule), and the 15 `EB_TAT_*` of spec 6.3 (`EB_TAT_OR` = 5 working days for
  "weekly"). Alert codes: the six `EB_*` of 8.2. Notification events (module EMPLOYEE_BENEFITS, sort 500-600; module
  BOOKING 620-630): `EB_FEEDBACK_RECEIVED`, `EB_FRANCHISE_DECIDED`, `EB_PROPOSAL_RECEIVED`, `EB_COMPARATIVE_SIGNOFF`,
  `EB_THRESHOLD_APPROVAL`, `EB_CLIENT_CONFIRMED`, `EB_PLACEMENT_TRIGGERED`, `EB_MEMBER_CHANGE_BILLED`, `EB_SOA_RELEASED`,
  `EB_INVOICE_PAID`, `EB_ITEM_ESCALATED`, `BOOKING_BILLING_BOOKED`, `BOOKING_BILLING_DUPLICATE`. Templates v1: the ten
  `EB_*` of 8.4 (placeholders in `{{...}}`, e-mail wording).
- **Workflows** (stage.action -> stage; the permission documents the right; EB services call `systemTransition` after
  their own checks):
  - `EB_CYCLE`: the 17 stages of `EbCycleStage` (PLACED, CLOSED_LOST, NOT_RENEWED terminal). `OPEN.send_ra` -> RA_SENT,
    `OPEN.start` -> REQUIREMENTS, `RA_SENT.record_feedback` -> REQUIREMENTS, `REQUIREMENTS.stay_with_incumbent` ->
    INCUMBENT_TERMS, `REQUIREMENTS.remarket` -> FRANCHISE, `FRANCHISE.release_tor` -> PROPOSALS,
    `INCUMBENT_TERMS|PROPOSALS|REVISION.build_comparative` -> COMPARATIVE, `COMPARATIVE.submit` -> FOR_SIGNOFF,
    `FOR_SIGNOFF.approve` -> READY_TO_PRESENT and `FOR_SIGNOFF.approve_to_threshold` -> THRESHOLD_APPROVAL (one action
    per target stage: the service picks it after evaluating the threshold rules), `FOR_SIGNOFF|THRESHOLD_APPROVAL.return`
    -> COMPARATIVE, `THRESHOLD_APPROVAL.approve` -> READY_TO_PRESENT, `READY_TO_PRESENT.present` -> WITH_CLIENT,
    `WITH_CLIENT.request_revision` -> REVISION, `WITH_CLIENT.confirm` -> CONFIRMED, `WITH_CLIENT.reconfirm_threshold`
    -> THRESHOLD_APPROVAL (7.1: confirm re-evaluates the threshold rules), `CONFIRMED.trigger_placement` ->
    IN_PLACEMENT, `IN_PLACEMENT.placed` -> PLACED. Generic, with reason list `EB_LOST_REASON`: `close_lost` from every
    open stage up to CONFIRMED except FOR_SIGNOFF and THRESHOLD_APPROVAL, `not_renewed` from the renewal stages.
  - `EB_FRANCHISE`: DRAFT, SUBMITTED, APPROVED, REJECTED, EXPIRED (terminal), ADVISED (terminal); `submit`, `approve`,
    `reject` (reason `EB_FRANCHISE_REJECT_REASON`), `expire`, `advise`.
  - `EB_MEMBER_CHANGE`: CAPTURED, RELAYED, BILLED, VALIDATED, CLOSED, CANCELLED; `relay`, `bill`, `validate`
    (EB_PROCESS), `close`; generic `return` (RETURN_REASON) from RELAYED or BILLED to CAPTURED, `cancel` (VOID_REASON).
  - `EB_SOA`: RECEIVED (SLA 72 h), VALIDATED, RELEASED, REJECTED; `validate`, `reject` (`EB_SOA_REJECT_REASON`),
    `release`.
- **`V1033__eb_programmes_cycles.sql` (moved into E0).** So that E1-B and E1-C start in parallel on the same core
  records, E0 creates `eb_programme`, `eb_programme_line`, `eb_programme_contact`, `eb_cycle` (+ `eb_cycle_account`,
  the ARNs created at placement; one open cycle per programme and policy year), `eb_document` (source AO, PROCESSING,
  CLIENT, INSURER, SYSTEM) and `eb_activity_log`. **E1-B puts `eb_renewal_advice`, `eb_feedback` and `eb_bor` into its
  V1034** with the marketing tables (section 3 listed them in V1033).
- **Domain `eb.domain`** (contract): entities `EbProgramme` (+ `ClientRef`, `Profile`, `addLine`, `addContact`, `line`,
  `markStatus`), `EbProgrammeLine` (+ `Data`), `EbProgrammeContact` (+ `Data`), `EbCycle` (business type required,
  `mirror`, `recordOutcome`, `markRemarketing`, `recordAccount`, `isOpen`), `EbDocument` (+ `Place`, `supersede`,
  `reject`), `EbActivity` (`release`); enums `EbCycleStage`, `EbCycleOutcome`, `EbProgrammeStatus`, `EbFunding`,
  `EbContactRole`, `EbDocumentSource`, `EbDocumentStatus`, `TatActivity` (each with its TAT parameter); repositories
  `EbProgrammeRepository`, `EbCycleRepository` (`findOpen`, `findByAccountArn`), `EbDocumentRepository`,
  `EbActivityRepository`; constants `EbDocumentTypes` (types, process tags) and `EbCodes` (workflows, entity types
  `EbProgramme`, `EbCycle`, `EbFranchise`, `EbMemberChange`, `EbSoa`, number prefixes, `series(prefix, year)`); event
  `EbCycleStageChanged(cycleId, cycleNo, programmeId, from, to, action, reasonCode)`.
- **Services `eb.service`**: `EbCycleMirror` (mirrors EB_CYCLE on `EbCycle`, publishes `EbCycleStageChanged`) and
  `EbParameters` (typed readers of every EB parameter, `tatDays(TatActivity)`).
- **Reports**: `ReportCategory.EMPLOYEE_BENEFITS` ("Employee Benefits") and `ReportMetadata.employeeBenefits(code,
  title, description, parameters)` (view and export `EB_REPORT_VIEW`, archived; add `.asDocument()` for Word).
- **Frontend** `features/eb`: section "Employee Benefits" in Client & Policy after Non-Package Management; routes `/eb`
  (EB Home, landing with the tiles of `home/ebHomeTiles.ts`), `/eb/programmes`, `/eb/programmes/new`,
  `/eb/programmes/:id` (hidden), `/eb/comparatives/:id` (hidden), `/eb/setup` (placeholders of E1-B),
  `/eb/member-changes`, `/eb/pending-items`, `/eb/soa` (EB_PROCESS or EB_COLLECT; placeholders of E1-C);
  `EbPlaceholder` / `EB_SECTION`; help `EB_HELP` registered after Non-Package Management.
- **Crons**: `brokerverse.jobs.eb-renewal-advice-cron` (06:00 PHT) and `eb-item-followup-cron` (07:00 PHT); the jobs
  are built by E1-C.

### 16.5 Notes for FRS v1.1

- FR-EB-002: the process tag refusal ("Select the process of the document") is EB's own check (E1-B / E1-C); the
  platform stores any tag. A refused download is HTTP 403 "You are not permitted to open documents of type `<type>`".
- FR-EB-004: code `DOCUMENT_NOT_PROTECTABLE`, message as in the FRS.
- FR-EB-021: code `EB_BUSINESS_TYPE_REQUIRED` ("Select the business type of the cycle"); a second open cycle is refused
  by the unique index (E1-B maps it to its message).
- FR-EB-052: codes `BILLING_NO_REQUIRED`, `BILLING_NO_DUPLICATE`, `BILLING_NO_TOO_LONG`; the unique key uses the lead
  insurer of the invoice (`bkg_invoice.insurer_code`).
- Screen paths: the portal screens and "Portal Uploads" of the test plan are out of Drop 2; client and insurer
  documents are uploaded by the EB users.

### 16.6 E1-B and E1-C: programmes, renewal advice, BOR, placement accounts and tracked items

The first business slice of the two waves: programmes and cycles with their requirement steps, the renewal advice with
its job and reminders, client feedback, the document register, the Broker on Record, the accounts of a confirmed cycle
with the shared business type (BT0), and the tracked items with their follow-up job. The marketing steps after the
requirements (franchise, TOR, insurer requests, proposals, revisions, comparative, threshold approval, client
confirmation, submissions and required documents) and the servicing records (roster, members, member changes, SOA) and
the EB reports are not in this slice; their tables take V1037-V1039 (section 3).

**Migrations.** `V1034__eb_renewal_advice_feedback_bor.sql`: `eb_renewal_advice` (one per cycle: expiry announced, sent
at / by, JOB or MANUAL, recipients, outbox message, stored document, reminders sent, last reminder, feedback time),
`eb_feedback` (channel AO, EMAIL, PHONE, MEETING or LETTER; date received; text up to 4,000 characters and / or files;
check "text or files"), `eb_bor` (version per cycle, stored file, status UPLOADED, VALIDATED, REJECTED or SUPERSEDED, the
three checklist answers, validity, decision). `V1035__eb_tracked_items.sql`: `eb_tracked_item` (type of list
`EB_TRACKED_ITEM_TYPE`, subject, member and member change as plain references until those tables exist, account ARN,
responsible INSURER / CLIENT / BDOI with the insurer party code, follow-up recipients, status PENDING, RECEIVED,
RELEASED or CLOSED, due date, follow-ups sent, last follow-up, escalation, dates received / released / closed).
`V1036__eb_reports_support.sql`: indexes, read view `eb_tat_v` (activity stamps with the programme's team and AO and
the elapsed days), retention rule `EB_PROGRAMME` (LOST, INACTIVE; 5 years online, 10 more in the archive), notification
event `EB_BOR_DECIDED`. No file content in any table: every file is a `doc_attachment` in the file store.

**Domain** (`eb.domain`, additions): `EbRenewalAdvice` (+ `Sending`), `EbFeedback`, `EbFeedbackChannel`, `EbBor`
(+ `Checklist`), `EbBorStatus`, `EbTrackedItem` (+ `Details`), `EbItemStatus`, `EbResponsibleParty` and their
repositories; `EbCodes` gains the entity type `EbTrackedItem`, the template, alert, event, outbox purpose and retention
codes and `PROGRAMME_LINK`; `EbProgrammeRepository` (`findByIdAndCompanyId`, `findWithLinesExpiring`, specifications),
`EbCycleRepository` (`findByIdAndCompanyId`, `findOpenOfProgramme`, `countOpenByStage`), `EbDocumentRepository`
(`findByAttachmentId`, `findByCycleIdOrderByIdAsc`).

**Services** (by sub-package):

| Package | Class | Does |
|---|---|---|
| `eb.service` | `EbRecords` | Programme and cycle of a company (another company's record is "not found"), open-cycle guard `EB_CYCLE_CLOSED` |
| | `EbWorkingDays` | Working days on the head office calendar (weekends and holidays), Monday to Friday without a head office |
| | `EbActivityLog` | Received / released stamps of the TAT activities (`received`, `done`, `released`) |
| `eb.programme` | `ProgrammeService`, `ProgrammeRules` | Create (client prospect or confirmed; EBP number; ACTIVE when a line has a current policy or ARN, else PROSPECT), profile, lines and contacts (at least one active line and one active contact; LOV team and benefit line; incumbent insurer known; period; e-mail; AO must hold EB_MARKET) |
| | `ProgrammeQuery` | Work list tabs RENEWAL_DUE (flagged, ACTIVE, a line expiring within `EB_RA_LEAD_DAYS`, no advice yet), IN_PROGRESS, WITH_CLIENT (with CONFIRMED), IN_PLACEMENT, PLACED, LOST (with LAPSED and not renewed), ALL; filters stage, AO, team, text |
| | `ProgrammeViewService` | Programme page (lines, contacts, cycles with renewal advice and BOR status, current cycle) and activity log |
| | `EbClientRecords`, `EbProgrammeRetentionProvider` | Client 360 records; retention candidates of `EB_PROGRAMME` |
| `eb.cycle` | `CycleService` | Open a cycle (business type required, `EB_BUSINESS_TYPE_REQUIRED`; renewal only for a flagged programme; one open cycle per policy year `EB_CYCLE_OPEN_EXISTS`; EBC number; `EB_CYCLE` work case), `start` (new business only), `stay_with_incumbent` (renewal only), `remarket` (needs the BOR gate; marks remarketing) |
| | `CycleOutcomeListener` | `close_lost` / `not_renewed` (generic, reason `EB_LOST_REASON`): outcome and reason on the cycle; programme LAPSED (renewal not renewed) or LOST |
| | `BorGate` (port) | "No validated BOR in force": `EB_BOR_REQUIRED` "Cycle <no.> has no validated Broker on Record" |
| `eb.renewal` | `RenewalAdviceService`, `RenewalAdviceLetter`, `RenewalTarget` | Send the advice (below); reminders |
| | `RenewalAdviceJob` | `EB_RENEWAL_ADVICE` |
| | `RenewalAdviceBatch` | Send RA from the work list, one transaction per programme, a result per programme |
| | `FeedbackService` | Record feedback (below) |
| `eb.document` | `EbDocumentService`, `EbDocumentQuery` | Register (below); Documents tab filtered by the access classes |
| `eb.bor` | `BorService` | Upload (PDF or Word, `ATTACHMENT_TYPE_NOT_ALLOWED` "Upload the BOR as PDF or Word"; one version waiting at a time, `EB_BOR_PENDING`), validate (checklist complete `EB_BOR_CHECKLIST_INCOMPLETE`, validity required and ordered), reject with a reason (register entry REJECTED); an earlier validated version is superseded; the uploader is notified (`EB_BOR_DECIDED`); implements `BorGate` over the programme's versions in force on the business date |
| `eb.placement` | `EbPlacementService`, `LinePlacement`, `PlacementProgressListener` | Accounts of a confirmed cycle (below) |
| `eb.tracked` | `TrackedItemService`, `TrackedItemQuery`, `ItemFollowUpService`, `FollowUpSchedule`, `ItemFollowUpJob` | Tracked items and `EB_ITEM_FOLLOWUP` (below) |
| `eb.home` | `EbHomeService` | EB Home counts |

**Renewal advice (FR-EB-022).** The advice announces the earliest expiry of the programme's active lines (on or after
the business date; for the job, not after the lead time); its policy year is the expiry's year. The RENEWAL cycle of
that year is opened (or an OPEN renewal cycle without advice is reused). The text is template `EB_RENEWAL_ADVICE`
(contact names, programme, lines, expiry dd-MMM-yyyy, incumbent insurers, AO); the PDF (`DocumentComposer`) is stored
as `RENEWAL_ADVICE`, process `RENEWAL_PLACEMENT`, source SYSTEM, on the cycle and linked to the programme, the client and
every current ARN of the lines (decision D3: Customer Servicing finds it). The e-mail goes to the active contacts that
receive the advice, the AO in copy, with the PDF password-protected and the password in a second e-mail
(`OutboundEmail.Protection`). The cycle moves `send_ra`; the activity `RENEWAL_ADVICE` is stamped. Refused: not flagged
("Programme <no.> is not flagged for renewal"), not ACTIVE ("has no current business to renew"), no contact for the
advice, no line ending on or after today (`EB_RA_NO_EXPIRY`), cycle past the advice (`EB_RA_ALREADY_SENT`). The job
raises `EB_RA_NOT_SENT` (de-duplicated per programme and expiry) instead of refusing, and skips a policy year that
already has a cycle past OPEN or a closed cycle. Reminders: template `EB_RA_REMINDER`, one per run for each day of
`EB_RA_REMINDER_DAYS` reached while the cycle is RA_SENT; a cycle that left RA_SENT stops them.

**Feedback (FR-EB-023).** Channel and date received (not after the business date) are required; text or at least one
file (`EB_FEEDBACK_EMPTY` "Enter the feedback or attach a file"). Files are `EB_CLIENT_FEEDBACK` documents, source
CLIENT. The first feedback stops the reminders and moves RA_SENT to REQUIREMENTS (`record_feedback`). The AO is notified
(`EB_FEEDBACK_RECEIVED`) when someone else records it.

**Document register (FR-EB-002, 030).** Upload on a cycle (the transaction; `EB_DOCUMENT_TRANSACTION_REQUIRED` without
it), with the document type (an EB type or `RENEWAL_ADVICE`; the BOR only on its tab, `EB_BOR_ON_BOR_TAB`), the process
(list `EB_PROCESS_TYPE`, `EB_PROCESS_REQUIRED` "Select the process of the document") and the source (AO, PROCESSING,
CLIENT, INSURER). Files go through `DocumentService.upload` (type, signature, size, file store) on the cycle and are
linked to the programme and the client with the process tag. All files of one upload share a version number; the ACTIVE
entries of the type on the cycle become SUPERSEDED (feedback and BOR files do not supersede). The Documents tab lists only
the documents whose type the user may see (`DocumentService.list`, access classes of V1031).

**Accounts with the shared business type (FR-EB-021 R2, FR-EB-046).** Contract for the client-confirmation step:
`EbPlacementService.trigger(companyId, cycleId, List<LinePlacement(lineNo, AccountDraft draft, PremiumBreakdown
premium)>)` on a CONFIRMED cycle (`EB_CYCLE_NOT_CONFIRMED`). One `AccountService.createDraft` per line (a line only once,
`EB_LINE_TWICE`): the programme's client and AO, the line's product and incumbent insurer where the draft has none;
classification RENEWAL with `renewal_of_ref` = the line's current ARN (else its current policy number, else
programme/line) for a RENEWAL cycle, NEW_BUSINESS for a new-business cycle, origin `EMPLOYEE_BENEFITS` for both. The ARNs
are kept on the cycle, a line without a product takes the account's product, the account's contract becomes a tracked
item (CONTRACT, owed by the insurer, due `EB_TAT_POLICY_SOA` working days ahead), the cycle moves `trigger_placement`,
the activity `PLACEMENT_REQUEST` is stamped and Processing (`EB_PROCESS`) is notified (`EB_PLACEMENT_TRIGGERED`).
`PlacementProgressListener` listens to `AccountStatusChanged`: when the last account of an IN_PLACEMENT cycle is BOOKED
the cycle records its outcome (NEW_PLACED; RENEWED_INCUMBENT when every account keeps the line's product and incumbent
insurer, else MOVED), each line takes its account as the current policy (ARN, insurer, period, first policy number), the
programme becomes ACTIVE and the cycle moves `placed`. The accounts stay drafts for the AO to complete and submit on the
account screens (the EB product catalogue and the proposal data are not yet in BIBS).

**Tracked items (FR-EB-057).** Opened by EB_MARKET or EB_PROCESS users on a programme (type, subject, due date,
responsible party required; recipients e-mail checked) and for each placed account. Moved on: RECEIVE (date required,
`EB_ITEM_RECEIVED_DATE` "Enter the date received"), RELEASE (after RECEIVED), CLOSE (a pending item needs its date
received); dates not after the business date; only a pending item changes. `EB_ITEM_FOLLOWUP`: for each PENDING item
past due, `FollowUpSchedule` gives the n-th follow-up `n x EB_FOLLOWUP_DAYS` working days after the due date; while fewer
than `EB_FOLLOWUP_MAX` were sent, template `EB_ITEM_FOLLOWUP` goes to the item's recipients, else the insurer's
placement mailboxes, the client's active contacts or the AO; then (or when nobody can be written to) the alert
`EB_ITEM_OVERDUE` (de-duplicated per item) and the notice `EB_ITEM_ESCALATED` to the AO, once.

**API** (`/api/v1/eb`, company in `companyId`):

| Method and path | Permission | Does |
|---|---|---|
| `GET /home` | EB_VIEW | Tile counts: `raDue`, `awaitingFeedback`, `franchisePending`, `proposalsOutstanding`, `comparativesToSignOff`, `thresholdApprovals`, `withClient`, `pendingItemsOverdue` |
| `GET /account-officers` | EB_VIEW | Users holding EB_MARKET |
| `GET /programmes?tab=&stage=&ao=&team=&q=&page=&size=` | EB_VIEW | Work list |
| `POST /programmes`, `GET`, `PUT /programmes/{id}` | EB_MARKET / EB_VIEW | Create, read, change the profile |
| `POST /programmes/{id}/lines`, `PUT`, `DELETE /programmes/{id}/lines/{lineNo}` | EB_MARKET | Lines |
| `POST /programmes/{id}/contacts`, `PUT`, `DELETE /programmes/{id}/contacts/{contactId}` | EB_MARKET | Contacts |
| `POST /programmes/send-ra` | EB_MARKET | Send RA of the selected programmes |
| `GET /programmes/{id}/activity`, `/feedback`, `/documents`, `/bor`, `/accounts` | EB_VIEW | Tabs of the programme page |
| `POST /programmes/{id}/cycles` | EB_MARKET | Open a cycle |
| `POST /cycles/{id}/start`, `/stay-with-incumbent`, `/remarket` | EB_MARKET | Requirement steps |
| `POST /cycles/{id}/feedback` (multipart) | EB_MARKET | Record feedback |
| `POST /cycles/{id}/documents` (multipart) | EB_MARKET or EB_PROCESS | Upload documents |
| `POST /cycles/{id}/bor` (multipart), `POST /bor/{id}/validate`, `POST /bor/{id}/reject` | EB_MARKET; validate / reject EB_MARKET or EB_PROCESS | BOR |
| `GET /pending-items?programmeId=&member=&type=&responsible=&status=&overdue=&q=`, `POST /pending-items`, `PUT /pending-items/{id}`, `POST /pending-items/{id}/status` | EB_VIEW; changes EB_MARKET or EB_PROCESS | Pending Items |

Documents are downloaded through the attachment API (`/api/v1/attachments/{id}/content`, access classes applied).

**Screens** (`features/eb`, standard record and work list pattern): EB Home (tile counts, "Needs attention" on RA due and
pending items overdue), Programmes (tabs, search, stage and team filters, selection column and Send RA with a result
list), New Programme (client picker with prospects, profile, repeated benefit line and HR contact groups, validation,
unsaved-changes guard), the programme page (`RecordHeader` with the programme, client and cycle chips, status and cycle
stage, flags; `WorkflowPanel` of the current cycle with Send RA, Record Feedback, Start Requirements, Stay with Incumbent
and Go to Market; page actions Edit Programme, Add Pending Item, Open Cycle; tabs Cycle (cycles, renewal advice,
feedback), Lines, Contacts, Documents (upload dialog), BOR (upload, validate with the checklist, reject), Accounts,
Pending Items and History (activity log)), Pending Items (filters member, type, party, status, past due; Add Pending
Item; Mark Received / Released, Close Item). The comparative, member change, SOA and set-up routes keep the E0
placeholder. Status tones for the EB stages were added to `statusTones`.

**Seed data.** V1930 SIT/UAT users; V1931 six programmes and four cycles with their work cases, advices, feedback and
activity (dates relative to the load date; series `EBP-2026` and `EBC-2026` continue at 101); V1932 three tracked items.
Screenshots: `eb-home`, `eb-programmes`, `eb-programme-record`, `eb-new-programme`, `eb-pending-items` in
`tools/screenshots/screens.cjs`.

**Tests.** `EbProgrammeIT`, `EbRenewalAdviceIT`, `EbDocumentsBorIT`, `EbPlacementIT`, `EbTrackedItemsIT`,
`api/EbProgrammesApiIT`, the EB block of `ApiSmokeIT`; unit tests `EbServicingDomainTest`, `RenewalTargetTest`,
`FollowUpScheduleTest`; frontend `module.test.tsx`, `programmes/programmePages.test.tsx`,
`programmes/programmeLogic.test.ts`, `pending/pendingItems.test.tsx`.

### 16.7 Notes for FRS v1.1 (E1-B, E1-C)

- FR-EB-021: "Programme <no.> already has an open cycle for <year>" is `EB_CYCLE_OPEN_EXISTS`; a renewal cycle of a
  programme not flagged for renewal is refused with `EB_NOT_RENEWAL_ELIGIBLE`. Validation codes `EB_LINE_REQUIRED`,
  `EB_CONTACT_REQUIRED`, `EB_CLIENT_REQUIRED`, `EB_AO_INVALID`, `EB_LINE_PERIOD_INVALID`, `EMAIL_ADDRESS_INVALID`. The
  account officer list is the users holding EB_MARKET (`GET /api/v1/eb/account-officers`).
- FR-EB-022: manual send refusal "Programme <no.> is not flagged for renewal" is `EB_RA_NOT_ALLOWED` (also "has no
  current business to renew", "has no HR contact receiving the renewal advice"); `EB_RA_NO_EXPIRY`,
  `EB_RA_ALREADY_SENT`, `EB_RA_SELECTION`. The reminder carries no portal link (no portal); the client replies by e-mail.
  New or prospect programmes (no current business) receive no advice.
- FR-EB-023: channel list AO / E-mail / Telephone / Meeting / Letter (no Portal); codes `EB_FEEDBACK_EMPTY`,
  `EB_FEEDBACK_DATE_FUTURE`, `EB_FEEDBACK_DATE_REQUIRED`, `EB_FEEDBACK_CHANNEL_REQUIRED`.
- FR-EB-002 / 030: `EB_PROCESS_REQUIRED` (the FRS "-"), `EB_DOCUMENT_TRANSACTION_REQUIRED`, `EB_DOCUMENT_TYPE_REQUIRED`,
  `EB_DOCUMENT_TYPE_INVALID`, `EB_BOR_ON_BOR_TAB`. "Request Unnamed Master List" (portal task) is not built; the AO
  asks the incumbent by e-mail and uploads the census with the source INSURER.
- FR-EB-031: `EB_BOR_CHECKLIST_INCOMPLETE`, `EB_BOR_REQUIRED` ("Cycle <no.> has no validated Broker on Record"),
  `EB_BOR_VALIDITY_REQUIRED`, `EB_BOR_VALIDITY_INVALID`, `EB_BOR_REASON_REQUIRED`, `EB_BOR_PENDING`,
  `EB_BOR_ALREADY_DECIDED`; the validator is an EB_MARKET or EB_PROCESS user; a validated BOR in force for the
  programme (any cycle) opens the market.
- FR-EB-046: `EB_CYCLE_NOT_CONFIRMED`, `EB_LINE_TWICE`. The trigger creates draft accounts; submitting them to
  Processing waits for the EB products and the proposal data (the minimum-field matrix of BRD-1 applies on submit).
- FR-EB-057: `EB_ITEM_RECEIVED_DATE`, `EB_ITEM_NOT_RECEIVED`, `EB_ITEM_NOT_PENDING`, `EB_ITEM_CLOSED`,
  `EB_ITEM_DUE_REQUIRED`, `EB_ITEM_SUBJECT_REQUIRED`, `EB_ITEM_RESPONSIBLE_REQUIRED`, `EB_ITEM_DATE_FUTURE`. HMO card
  and billing items are opened by the users until the member and member change steps create them.
