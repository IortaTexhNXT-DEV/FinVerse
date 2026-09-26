# iNXT BrokerVerse - BDOI Employee Benefits (BRD-8) Build Design

Status: **proposal for review**. This design extends `docs/architecture/BROKING_ARCHITECTURE.md`, `docs/architecture/OPERATIONS_DESIGN.md`, `docs/architecture/COLLECTIONS_DESIGN.md` and the Developer Guide, which stay binding. It does not change them, except for the contract changes listed in section 11.

Requirements baseline: [`BDOI_EB_BRD_SPEC.md`](../requirements/BDOI_EB_BRD_SPEC.md): 34 requirement rows (BRID-001 to 030, 005.01-005.03, 022.01; BRID-028 OUT) and questions EBQ01-EBQ28. Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRID-016`.

## 1. Design principles

1. **The EB cycle ends where the BRD-1 spine starts.** Everything before the client's decision is new: renewal advice, BOR, franchise, TOR, insurer requests and proposals, comparative, revisions, threshold approval. The client's confirmation turns the chosen proposal into **accounts** through `AccountService.createDraft`, one per benefit line. Placement, issuance, booking, the invoice ledger, cashiering, collections and commission then work unchanged. EB never posts a journal itself.
2. **External parties never touch the core.** Insurers and client HR users are not `AppUser`s. They sign in to a separate realm (`/api/portal/**`, token audience `portal`) that the core API refuses. Every portal query is scoped by the party the user is bound to. The portal is a platform module that later BRDs can reuse (insurer files for Prod Recon or Remittance, client self-service).
3. **Nothing external takes effect before an internal user validates it (BRID-005.01 AC7, 014 AC7).** Portal uploads land in a staging table and become attachments or business data only on validation. Insurer proposals are SUBMITTED until the AO validates them; client master lists are staged roster versions until the AO accepts them.
4. **Versions, not updates.** TOR, BOR, proposals, comparatives and member rosters are versioned rows that are never changed after submission (BRID-008, 015, 024). The history tab and the diff come from the rows.
5. **Documents carry their context.** Every EB document is registered with its type, process (placement, renewal, endorsement, adjustment, franchise) and access class (Marketing, Processing, Collection) (BRID-025). The platform attachment module enforces the class for every module, not only EB.
6. **Parked means seam, not fake.** The insurer API, e-signature verification of the BOR, HRIS master list feeds, reading insurer mailboxes and the EBIX booking each get a seam. No integration is simulated.
7. **Personal data is sensitive.** Master lists and utilization reports hold employees' personal and health-related data. Access classes, download logging and retention rules apply from day one (spec section 8).

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `portal` (new, platform, package `com.iortatechnxt.brokerverse.portal`, tables `ptl_*`) | External identities bound to a party (insurer or client), invitation and approval of portal users, separate security realm and tokens, party context, staged uploads with review, portal notices, portal audit, ports for business modules | BRID-005, 005.01-005.03, 014 (realm and staging), 011 / 015 (channel) | security, attachment, audit, messaging, workflow, system, party | V1032 (V1930) |
| `eb` (new, package `com.iortatechnxt.brokerverse.eb`, tables `eb_*`) | EB programmes and cycles, RA and reminders, feedback, document register, BOR, franchise, TOR, insurer requests, proposals and revisions, comparative and approvals, value thresholds, client confirmation and placement trigger, submissions, member roster and member changes, tracked items, SOA intake, EB reports and jobs, EB portal endpoints | BRID-001-004, 007-010, 012, 013, 016, 017, 019, 021, 022, 024, 026, 027, 029, 030 | portal, crm, catalog, account, booking (read), opsledger (read, events), adjustment (endorsement intake), issuance (e-policy receipt), workflow, approval, messaging, docgen, attachment, bulk, report, lov, alert, system, nbadmin (retention port) | V1033-V1035 (V1931-V1932) |
| `account` (built) | Business type on the account: the shared work item **BT0** (`V822__account_business_type.sql`, account range), also used by Renewal and Submitted Policies; EB creates no column of its own | BRID-022.01 | unchanged | V822 (BT0) |
| `booking` (built) | Business type from the account (part of BT0); + insurer billing number with duplicate block (EB) | BRID-020, 022.01 | unchanged | V822 (BT0, code only); V1031 (billing number) |
| `attachment` (platform) | + document access classes; + process tag on links | BRID-025 | unchanged | V1031 |
| `messaging`, `report`, `nbreport` (built) | + DOCX protection; + DOCX export (EB); + Business Type filter (part of BT0) | BRID-007, 022.01 | unchanged | none |

### 2.1 Dependency graph (arrows = "depends on")

```
                     crm  catalog  account  booking(read)  opsledger(read)  adjustment  issuance
                       \      |       |         |               |              |          |
                        +-----+-------+---------+------ eb -----+--------------+----------+
                                                         |
                                                       portal ---> security, attachment, audit, messaging, workflow, party
```

- `portal` knows nothing about EB. It declares the ports below; `eb` implements them.
- `eb` calls BRD-1 and Operations services only through their public services (`AccountService`, `AccountLifecycleService` is **not** used by EB; `EndorsementRequestService.create`, `EpolicyService.receive`, `InvoiceLedgerQueryService`).
- No BRD-1 or Operations module depends on `eb`. EB learns about account progress from `AccountStatusChanged` and about payments from `InvoiceMovementPosted` (Spring events after commit).

### 2.2 Ports declared in `portal.service.port`

| Port | Implemented by | Purpose |
|---|---|---|
| `PortalUploadTarget` | eb (`EbUploadTargets`) | `targetTypes()`, `authorize(PortalParty, targetType, targetId)` (the party may upload to this target), `reviewPermission(targetType)`, `onValidated(StagedUpload, attachmentId)`, `onRejected(StagedUpload, reason)` |
| `PortalTaskSource` | eb (`EbPortalTasks`) | Open tasks of a party for the portal home: request to answer, franchise to decide, member change to bill, SOA to send, master list to upload, comparative to review |
| `PortalHomeCounts` | eb | Counts per tile for the internal "Portal uploads to review" queue per permission |

## 3. Flyway allocation

Allocated to Employee Benefits in the Developer Guide: **schema V1030-V1039, demo V1930-V1939.** Seven schema versions are used; V1037-V1039 stay free for follow-ups. One block is enough.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1030__eb_foundation.sql` | E0 | Grants of the EB and portal permissions to new and existing roles; roles `EB_AO`, `EB_TL`, `EB_MANAGEMENT`, `EB_PROCESSOR`, `EB_PROC_SUPERVISOR`, `EB_COLLECTION`; LOV types and values (section 8.4); `DOCUMENT_TYPE` values (spec 6.1); `sys_parameter` rows; exception codes; workflows `EB_CYCLE`, `EB_FRANCHISE`, `EB_MEMBER_CHANGE`, `EB_SOA`, `PORTAL_UPLOAD_REVIEW` (`wf_stage`, `wf_transition`); `doc_template` rows; notification events |
| `V1031__eb_platform_extensions.sql` | E0 | `att_document_access` (document type, permission) and `att_attachment_link.process_tag`; the cross-BRD document types `RENEWAL_ADVICE` (Renewal, EB) and `CLAIM_REPORT` (Claims) with their access rows, including the CSF view permission (insert ... on conflict do nothing, so the order against V1010 / V1020 does not matter; final matrix: cross-BRD question XQ04); `bkg_invoice.insurer_billing_no varchar(60)` and a unique partial index on (company_id, insurer_code, insurer_billing_no) where not null. **Not** the account business type: `acc_account.business_type` is the shared `V822__account_business_type.sql` (BT0, cross-BRD decision D1), which must be merged before E1-B creates accounts |
| `V1032__portal.sql` | E1-A | `ptl_user`, `ptl_invitation`, `ptl_login_event`, `ptl_upload`, `ptl_notice`, `ptl_download_log`. No request table: portal users are provisioned through User Access Maintenance requests (`nba_access_request`, user type EXTERNAL, V1062; decision D7) |
| `V1033__eb_programmes_cycles.sql` | E1-B | `eb_programme`, `eb_programme_line`, `eb_programme_contact`, `eb_cycle`, `eb_renewal_advice`, `eb_feedback`, `eb_document`, `eb_bor`, `eb_activity_log` |
| `V1034__eb_marketing.sql` | E1-B | `eb_franchise_request`, `eb_tor`, `eb_tor_item`, `eb_insurer_request`, `eb_proposal`, `eb_proposal_line`, `eb_proposal_item`, `eb_proposal_factor`, `eb_revision_request`, `eb_revision_item`, `eb_revision_target`, `eb_comparative`, `eb_comparative_signoff`, `eb_comment`, `eb_threshold_rule`, `eb_client_confirmation`, `eb_confirmation_line`, `eb_submission`, `eb_submission_document`, `eb_required_document` |
| `V1035__eb_servicing.sql` | E1-C | `eb_roster_version`, `eb_member`, `eb_member_change`, `eb_member_change_line`, `eb_tracked_item`, `eb_soa` |
| `V1036__eb_reports_support.sql` | E1-C | Indexes and the read view `eb_tat_v` over `eb_activity_log` for `EB-TAT`; retention rules (`nba_retention_rule`: EB_PROGRAMME, EB_MEMBER) |
| `V1037`-`V1039` | - | Kept free |
| `db/demo/V1930__demo_eb_reference.sql` | E2 | EB product lines and products (HMO, GLI, GPA) in the demo catalog, HMO providers as panel insurers with commission rates, threshold rules, required documents, demo internal users and portal users |
| `db/demo/V1931__demo_eb_cycles.sql` | E2 | Six programmes of the V981 corporate clients, with cycles in every stage: RA sent, franchise pending, proposals received, comparative for approval above the threshold, with client, confirmed and placed |
| `db/demo/V1932__demo_eb_servicing.sql` | E2 | Rosters, member changes (one relayed, one billed, one closed), tracked items (an overdue HMO card), SOA received and released, portal uploads waiting for review |

Rules:
- V1031 alters `bkg_invoice` (V870) and the attachment tables, which always exist before V1031 on a fresh database. The `booking` entity changes in the same wave (E0), because Hibernate validates the schema. The account business type comes from V822 (BT0), which runs before V1031.
- EB tables store ARN, invoice no., client code and insurer code as plain values, like Operations. Foreign keys only to platform tables (users, attachments, workflow, LOV) and to EB's own tables.
- The demo runs after every V9xx demo (V981 clients, V982 catalog, V988 booking), as the range table requires.

## 4. Entities (key fields)

Money is `numeric(19,2)`; every table has `company_id`, the audit columns and `version`.

### 4.1 `portal`

| Table | Key fields |
|---|---|
| `ptl_user` | username (e-mail, unique), full name, party kind INSURER / CLIENT, party code (insurer party code or client code), company, portal role (INSURER_USER, CLIENT_HR), status INVITED / ACTIVE / LOCKED / DISABLED, password hash, failed attempts, MFA required, last login |
| `ptl_invitation` | user, one-time token hash, expires at (`PORTAL_INVITE_VALID_HOURS`), used at |
| `ptl_login_event` | user, time, outcome, IP, user agent |
| `ptl_upload` | portal user, party, target type (EB_INSURER_REQUEST, EB_FRANCHISE, EB_MEMBER_CHANGE, EB_SOA_REQUEST, EB_POLICY_FORM, EB_PROGRAMME_DOCS ...), target id, document type, file name, content, SHA-256, size, virus-scan result, status RECEIVED / VALIDATED / REJECTED, reviewer, reason, attachment id (after validation) |
| `ptl_notice` | portal user or party, subject, body, link, read at |
| `ptl_download_log` | portal user, attachment id, time (BRID-014 AC6: downloads audited) |

### 4.2 `eb`: programme, cycle, documents

| Table | Key fields |
|---|---|
| `eb_programme` | programme no. `EBP-<yyyy>-nnnnnn`, client id and code, name, team (LOV `EB_TEAM`), funding EMPLOYER / VOLUNTARY, AO username and sales unit, renewal eligible flag, status PROSPECT / ACTIVE / LAPSED / LOST / INACTIVE |
| `eb_programme_line` | programme, benefit line (LOV `EB_BENEFIT_LINE`), product code, incumbent insurer code, current policy no., current ARN, period from / to, headcount |
| `eb_programme_contact` | programme, name, e-mail, mobile, role (HR_HEAD, HR_OFFICER, FINANCE), receives RA / SOA flags, portal user id |
| `eb_cycle` | cycle no. `EBC-<yyyy>-nnnnnn`, programme, business type NEW_BUSINESS / RENEWAL, policy year, target inception, remarketing flag, stage (mirror of the `EB_CYCLE` case), outcome RENEWED_INCUMBENT / MOVED / NEW_PLACED / NOT_RENEWED / LOST with reason, account ARNs |
| `eb_renewal_advice` | cycle, sent at, recipients, message id, attachment id, reminders sent, last reminder at |
| `eb_feedback` | cycle, channel AO / PORTAL / EMAIL, text, received at, attachment links |
| `eb_document` | cycle or programme, document type, process type (LOV `EB_PROCESS_TYPE`), version, attachment id, source AO / CLIENT_PORTAL / INSURER_PORTAL / SYSTEM, status ACTIVE / SUPERSEDED / REJECTED |
| `eb_bor` | programme, cycle, version, attachment id, status PENDING / UPLOADED / VALIDATED / REJECTED, checklist answers (signed by authorised signatory, not blank, client name matches), validated by / at, valid from / to |
| `eb_activity_log` | cycle or programme, activity code (TAT annex, spec 6.3), received at, released at, actor; written by the services at each step (source of `EB-TAT`) |

### 4.3 `eb`: marketing

| Table | Key fields |
|---|---|
| `eb_franchise_request` | franchise no. `EBF-<yyyy>-nnnnnn`, cycle, insurer code, documents, submitted at, due at, status (mirror of `EB_FRANCHISE`), decided at, decided by (portal user or AO with evidence), reason, client advised at |
| `eb_tor`, `eb_tor_item` | cycle, version, status DRAFT / RELEASED / SUPERSEDED; items: benefit line, plan code, item code, description, requirement, sort order |
| `eb_insurer_request` | request no. `EBR-<yyyy>-nnnnnn`, cycle, insurer code, TOR version, documents sent, sent at, due at, channel PORTAL_AND_EMAIL, status OPEN / RESPONDED / DECLINED / CLOSED |
| `eb_proposal` | proposal no., cycle, request (null for the incumbent's indicative), insurer code, kind INCUMBENT_INDICATIVE / PROPOSAL / REVISED, version, revision request id, status SUBMITTED / VALIDATED / REJECTED / SUPERSEDED, validity, source PORTAL / AO |
| `eb_proposal_line`, `eb_proposal_item`, `eb_proposal_factor` | per benefit line: annual premium, TSI (GLI / GPA), premium per plan; per TOR item: offered value, deviation flag, remark; per capability factor (LOV `EB_CAPABILITY_FACTOR`): value and rating |
| `eb_revision_request`, `_item`, `_target` | cycle, requested changes (TOR item or free text), target insurers with status OPEN / ANSWERED, answered by proposal version |
| `eb_comparative` | comparative no., cycle, version, snapshot (JSON of the rows), recommended proposal per line, status DRAFT / FOR_APPROVAL / THRESHOLD_APPROVAL / APPROVED / PRESENTED / SUPERSEDED, due at |
| `eb_comparative_signoff` | comparative, signatory, role, decision, time |
| `eb_comment` | comparative, author (internal or portal user), text, time, reply to |
| `eb_threshold_rule` | benefit line (blank = all), measure TSI / ANNUAL_PREMIUM, amount, currency, approver permission, level, effective from / to; `AuthorizableEntity` (maker-checker) |
| `eb_client_confirmation`, `eb_confirmation_line` | cycle, channel SYSTEM / EMAIL / SIGNED_DOCUMENT, evidence attachment, confirmed by (portal user or AO), per line: chosen proposal and insurer, created ARN |
| `eb_submission`, `eb_submission_document` | process type, insurer, cycle / member change, documents, sent at, acknowledged at |
| `eb_required_document` | process type x benefit line x document type, mandatory flag; maker-checker |

### 4.4 `eb`: servicing

| Table | Key fields |
|---|---|
| `eb_roster_version` | programme, policy year, version, source upload (bulk job), status STAGED / ACCEPTED / REJECTED, headcount, accepted by |
| `eb_member` | roster version, employee no., last / first name, birth date, gender, civil status, plan code, dependants count, effective from / to, status ACTIVE / DELETED; unique (programme, policy year, employee no.) in the accepted version |
| `eb_member_change`, `eb_member_change_line` | change no. `EBM-<yyyy>-nnnnnn`, programme, line, source AO / CLIENT_PORTAL, financial flag, direct-billed flag, status (mirror of `EB_MEMBER_CHANGE`), adjustment request id; lines: action ADD / DELETE / CHANGE_PLAN / CHANGE_DATA, member data, effective date |
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
| `PORTAL_USER_REQUEST`, `PORTAL_USER_APPROVE` | Raise and decide User Access Maintenance requests of user type EXTERNAL (portal users; four eyes by the UAM rules, decision D7) |
| `PORTAL_ADMIN` | Lock / unlock portal users, portal login and download logs |

### 6.2 Roles (V1030) and demo users (V1930, password `Brokerverse@2026`)

| Role | Persona | Main permissions | Demo user |
|---|---|---|---|
| `EB_AO` | Marketing Account Officer (EB) | EB_VIEW, EB_MARKET, EB_REPORT_VIEW, PORTAL_USER_REQUEST, CLIENT_VIEW, CLIENT_MAINTAIN, ACCOUNT_VIEW, ACCOUNT_MAINTAIN, ATTACHMENT_VIEW, ATTACHMENT_MANAGE, WORK_VIEW | `ebao`, `ebao2` |
| `EB_TL` | Marketing TL / UH (EB) | EB_VIEW, EB_COMPARATIVE_APPROVE, EB_REPORT_VIEW, WORK_VIEW, WORK_ASSIGN | `ebtl` |
| `EB_MANAGEMENT` | BDOI Management (threshold approver) | EB_VIEW, EB_THRESHOLD_APPROVE, EB_REPORT_VIEW | `ebmgmt` |
| `EB_PROCESSOR` | Processing (EB) | EB_VIEW, EB_PROCESS + the BRD-1 `PROCESSOR` permissions | `ebproc` |
| `EB_PROC_SUPERVISOR` | Processing Supervisor | EB_PROCESSOR permissions + WORK_ASSIGN | `ebprocsup` |
| `EB_COLLECTION` | Collection (EB) | EB_VIEW, EB_COLLECT + `CLX_VIEW`, `CLX_WORK` | `ebcoll` |
| `BUSINESS_ADMIN` (existing) | Business Administrator | + EB_SETUP, PORTAL_USER_APPROVE, PORTAL_ADMIN | `badmin` |

### 6.3 Portal realm

- Portal authorities are an enum of the `portal` module (`PortalRole.INSURER_USER`, `CLIENT_HR`), not core permissions. Portal controllers use `@PreAuthorize("hasAuthority('PORTAL_INSURER_USER')")` or `...CLIENT_HR`.
- `config/SecurityConfig.java` gets a second `SecurityFilterChain` ordered first with `securityMatcher("/api/portal/**")`: portal login, invitation acceptance and portal endpoints. Tokens from `JwtTokenService` carry `aud` = `portal` and the claims `partyKind`, `partyCode`. The core chain rejects any token whose audience is not `core`; the portal chain rejects core tokens.
- `PortalContext.current()` returns the bound party; every EB portal query filters on it (insurer code or client code). A mismatch is `404`, not `403`, so a portal user cannot probe other records.
- Lockout after `PORTAL_MAX_FAILED_LOGINS`; session timeout `PORTAL_SESSION_MINUTES`; e-mail one-time code when `PORTAL_MFA_REQUIRED` (default true; the final method is EBQ13).
- Portal users are provisioned only through **User Access Maintenance requests** as an **external user type** (cross-BRD decision D7; USER_ACCESS_DESIGN section 4.4): the AO raises a CREATE / DISABLE / ENABLE request of user type EXTERNAL (party kind, party code, portal role) with `PORTAL_USER_REQUEST`; a chosen approver holding `PORTAL_USER_APPROVE` decides it (never the requester), with the UAM draft, return, history and change log. On approval `nbadmin` calls the port `ExternalUserProvisioner`, which `portal` implements: it creates the `ptl_user` (INVITED) and sends the invitation link, and the user sets the password. No password is ever sent by e-mail. Lock / unlock and the login and download logs stay in `portal` (`PORTAL_ADMIN`).
- Every portal action writes `AuditTrailService` entries under the portal username prefixed `portal:`; downloads go to `ptl_download_log`.
- Uploads: allowed types from `AllowedFileType`, `PORTAL_MAX_UPLOAD_MB`, signature check, and the `VirusScanner` port. A production adapter is required before go-live (the default is a no-op); the staging status stays RECEIVED until the scan passes.
- Deployment: the same image can run with profile `portal`, which registers only the portal chain and controllers, in the DMZ; the core runs internally. Details follow BDO Information Security (EBQ13).

### 6.4 Demo portal users (V1930)

| User | Party | Role |
|---|---|---|
| `hmo1@demo.portal` | first demo HMO provider | INSURER_USER |
| `life1@demo.portal` | first demo life insurer | INSURER_USER |
| `hr@cl2026000001.portal` | client `CL-2026-000001` | CLIENT_HR |

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
| `PORTAL_UPLOAD_REVIEW` | RECEIVED -> VALIDATED / REJECTED (reason); re-map target while RECEIVED | BRID-005.03 |

## 8. Jobs, alerts, parameters, LOVs, numbers, templates, bulk handlers

### 8.1 Managed jobs

| Job | Default cron (PHT) | Does |
|---|---|---|
| `EB_RENEWAL_ADVICE` | daily 06:00 | Opens RENEWAL cycles for eligible programmes whose expiry is `EB_RA_LEAD_DAYS` away, sends the RA, stores it as `RENEWAL_ADVICE` (linked to programme, current ARN and client), and sends reminders at `EB_RA_REMINDER_DAYS` while no feedback is recorded |
| `EB_ITEM_FOLLOWUP` | daily 07:00 | Follow-up e-mails for tracked items past due (`EB_FOLLOWUP_DAYS`), escalation after `EB_FOLLOWUP_MAX` |
| `PORTAL_INVITATION_EXPIRY` | daily | Expires unused invitations |

Crons are configurable (`brokerverse.jobs.eb-renewal-advice-cron`, ...), documented in `docs/operations/CONFIGURATION.md` by the build agent.

### 8.2 Alerts (`alt_exception_code`, V1030) and daily checks (`AlertCheck`)

`EB_RA_NOT_SENT` (inside the lead time, no RA: no contact, no flag), `EB_FRANCHISE_OVERDUE`, `EB_PROPOSAL_OVERDUE` (request past due), `EB_COMPARATIVE_LATE` (`EB_COMPARATIVE_DAYS` after the last proposal), `EB_SOA_VALIDATION_LATE` (`EB_TAT_SOA_VALIDATION`), `EB_ITEM_OVERDUE`, `PORTAL_UPLOAD_WAITING` (staged upload older than one working day), `PORTAL_LOGIN_LOCKED`.

### 8.3 Parameters (`sys_parameter`, category EMPLOYEE_BENEFITS / PORTAL)

`EB_RA_LEAD_DAYS` (135), `EB_RA_REMINDER_DAYS` (INTEGER_LIST 120, 105, 90), `EB_PROPOSAL_REPLY_DAYS` (5), `EB_FRANCHISE_TAT_DAYS` (5), `EB_FRANCHISE_ADVICE_DAYS` (2), `EB_COMPARATIVE_DAYS` (3), `EB_FOLLOWUP_DAYS` (5), `EB_FOLLOWUP_MAX` (3), the TAT parameters of spec 6.3, `EB_ADJ_BOOKING_REQUIRES_PAYMENT` (false), `BOOKING_BILLING_NO_LINES` (CODE_LIST of product lines that require the insurer billing number; EB lines), `PORTAL_SESSION_MINUTES` (15), `PORTAL_MAX_FAILED_LOGINS` (3), `PORTAL_MFA_REQUIRED` (true), `PORTAL_INVITE_VALID_HOURS` (72), `PORTAL_MAX_UPLOAD_MB` (10). All defaults are placeholders until EBQ02, EBQ07, EBQ10, EBQ13, EBQ16 and EBQ20 are answered.

### 8.4 LOVs, numbers, templates, bulk handlers

- LOVs: `EB_BENEFIT_LINE` (HMO, GLI, GPA), `EB_TEAM` (BDO, SM, VOLUNTARY, SOLICITED, NEW_BUSINESS), `EB_PROCESS_TYPE` (NB_PLACEMENT, RENEWAL_PLACEMENT, ENDORSEMENT, ADJUSTMENT, FRANCHISE, PROPOSAL), `EB_CAPABILITY_FACTOR` (COMPANY_STABILITY, CLINIC_PROVIDERS, HOSPITAL_NETWORK, TECHNOLOGY), `EB_LOST_REASON`, `EB_MEMBER_CHANGE_TYPE`, `EB_TRACKED_ITEM_TYPE`, `EB_FRANCHISE_REJECT_REASON`, `EB_SOA_REJECT_REASON`, `PORTAL_REJECT_REASON`; `DOCUMENT_TYPE` values of spec 6.1.
- Numbers (`DocumentNumberService`): `EBP-<yyyy>`, `EBC-<yyyy>`, `EBF-<yyyy>`, `EBR-<yyyy>`, `EBPR-<yyyy>` (proposal), `EBCA-<yyyy>` (comparative), `EBM-<yyyy>`, `EBS-<yyyy>`.
- Templates (`doc_template`): `EB_RENEWAL_ADVICE`, `EB_RA_REMINDER`, `EB_INDICATIVE_PROPOSAL`, `EB_TOR`, `EB_RFP_COVER`, `EB_FRANCHISE_REQUEST`, `EB_FRANCHISE_ADVICE`, `EB_COMPARATIVE`, `EB_REVISION_RELAY`, `EB_ITEM_FOLLOWUP`, `PORTAL_INVITATION`. Contents are placeholders until BDOI supplies the layouts (EBQ21).
- Bulk handlers: `EB_MASTERLIST` (template with the roster columns; creates a STAGED roster version; permission EB_MARKET, and the client portal upload uses it through the staging path), `EB_MEMBER_CHANGE` (lines of one member change), `EB_PROGRAMME_LOAD` (go-live migration of existing EB programmes and current policies from EBIX).

## 9. Reports (category `EMPLOYEE_BENEFITS`, package `eb.report`)

`EB-PRODUCTION`, `EB-RENEWAL`, `EB-PLACEMENT`, `EB-NEW-BUSINESS`, `EB-TAT`, `EB-PENDING-ITEMS`, `EB-FRANCHISE` (spec section 7). Common parameters: company, date range, team, AO, client, benefit line, insurer, business type. They read the EB tables and `bkg_invoice` with constant SQL aggregates (the `nbreport` pattern). Export PDF / XLSX / CSV / ODS / XML and, after E0, DOCX.

## 10. Screens (BDOI navigation)

### 10.1 Internal (group **Client & Policy**, section **Employee Benefits**, after Non-Package Management; UX-3)

| Screen | Route | Content | Permission |
|---|---|---|---|
| EB Home | `/eb` | Tiles: RA due, awaiting feedback, franchise pending, proposals outstanding, comparatives to sign off, threshold approvals, with client, member changes open, pending items overdue, portal uploads to review | EB_VIEW |
| Programmes | `/eb/programmes` | Work list with status tabs Renewal Due / In Progress / With Client / In Placement / Placed / Lost; toolbar and bulk "Send RA" | EB_VIEW |
| New Programme | `/eb/programmes/new` | Client picker (or "New Client" to crm), lines, team, contacts | EB_MARKET |
| Programme page | `/eb/programmes/:id` | Record summary card; `WorkflowPanel` of the current cycle; tabs Cycle / Documents / BOR / Franchise / Insurer Requests / Proposals / Comparative / Members / Member Changes / Billing & SOA / Pending Items / History | EB_VIEW |
| Comparative | `/eb/comparatives/:id` | Matrix, recommendation, sign-off, threshold approval, client comments, PDF / XLSX | EB_VIEW |
| Member Changes | `/eb/member-changes` | Work list, entry and upload (`/bulk/EB_MEMBER_CHANGE`) | EB_VIEW |
| Pending Items | `/eb/pending-items` | Tracked items by programme / member, filters, manual update | EB_VIEW |
| SOA Register | `/eb/soa` | Intake, validation, release | EB_PROCESS or EB_COLLECT |
| Portal Uploads | `/eb/portal-uploads` | Review queue (filtered by the reviewer's permission per target type) | EB_MARKET, EB_PROCESS or EB_COLLECT |
| EB Setup | `/eb/setup` | Threshold rules, required documents, EB parameters | EB_SETUP |

- **Setup & Administration** group: **Portal Users** (`/admin/portal-users`: portal user list, lock / unlock, login and download logs; PORTAL_ADMIN). Requests for portal users are raised and approved on the User Access screens (`/user-access/requests`, user type External; PORTAL_USER_REQUEST / PORTAL_USER_APPROVE).
- **Reports** group: the EB reports appear in the Report Centre under "Employee Benefits".
- The crm client page gains the tab **Employee Benefits** through `ClientRecordsProvider` (`EbClientRecords`).
- Help entries in `frontend/src/features/eb/help.ts`, registered in `HELP_SECTIONS`.

### 10.2 Portal (separate SPA entry `frontend/src/portal/`, served at `/portal`)

Sign-in (BDO Insure branding, "BIBS Partner Portal"), invitation acceptance, Home (tasks and notices), and:
- Insurer: Requests for Proposal (TOR and documents download, structured proposal form, attachments, revisions), Franchise Requests (approve / reject with reason), Member Changes (billing upload, confirmation), SOA and Policy Forms upload, Documents.
- Client HR: Programmes (summary, policy details, contacts), Members (read), Uploads (master list, utilization, member changes, feedback), Comparative (view, comment, confirm when channel SYSTEM is allowed, EBQ12), Renewal Advice and Documents.

The portal SPA reuses the UI kit and tokens of `frontend/src/components/ui`, and has no route into the internal application.

## 11. Impact on modules already built or being built (contract changes)

| Module | Change | Owner (wave) | BRD |
|---|---|---|---|
| `account` | **Shared work item BT0** (cross-BRD decision D1; SUBMITTED_POLICIES_DESIGN section 9): `acc_account.business_type` (required, default NEW_BUSINESS) and `renewal_of_ref` in `V822__account_business_type.sql`; `Account.getBusinessType()`; the existing `NewAccount` factories keep NEW_BUSINESS, and EB creates the accounts of a RENEWAL cycle with `NewAccount.renewal(...)` (`renewal_of_ref` = the line's current ARN); `AccountResponse`, `AccountSearch` filter; bulk `ACCOUNT_CREATE` optional column. EB's earlier variant (column in V1031, a required `businessType` on every `NewAccount`) is dropped | BT0 (first of S0 / R0 / E0 to start) | BRID-022.01 |
| `booking` | Part of BT0: `InvoiceBuilder` takes `account.getBusinessType()` instead of the constant `BusinessType.NEW_BUSINESS` (`booking/service/InvoiceBuilder.java` line 129) and `InvoiceBooked` carries `businessType`. EB only: `BookingOptions.insurerBillingNo`, `bkg_invoice.insurer_billing_no` (V1031), duplicate block `BILLING_NO_DUPLICATE`, mandatory for the lines in `BOOKING_BILLING_NO_LINES`; `BOOKING_UPLOAD` column; booking notification on success / duplicate | BT0; E0 (billing number) | BRID-020, 022.01 |
| `nbreport` | Part of BT0: parameter Business Type on `NB-BOOKED-REG`, `NB-PRODUCTION`, `NB-PLC-UPDATE` (read `bkg_invoice.business_type`) | BT0 | BRID-022.01 |
| `report` | `ExportFormat.DOCX` and `DocxReportRenderer` (Apache POI XWPF, already a dependency through POI) | E0 | BRID-022.01 |
| `messaging` | `DocumentProtector.canProtect / protect` for DOCX (POI agile encryption, as XLSX); `DOCUMENT_NOT_PROTECTABLE` for other types when protection is requested | E0 | BRID-007 |
| `attachment` | `att_document_access` read by `DocumentService` for list, download and ZIP (document types without a row keep today's behaviour); `AttachmentLink.processTag`; `Attachments` component shows only allowed documents | E0 | BRID-025 |
| `security` / `config` | Permissions of 6.1; second filter chain and token audience (6.3); core chain rejects portal tokens | E0 (permissions), E1-A (chain) | BRID-005 |
| `adjustment` | None required: EB calls `EndorsementRequestService.create(RequestDraft)` with source reference `EBM-...`. Ask: a `source` / `sourceRef` field on the request so Adjustment lists show "from EB member change" | E1-C (ask to Operations owner) | BRID-013 |
| `issuance` | None required: validated insurer policy forms go to `EpolicyService.receive(...)` for the placed account | E2 | BRID-019, 005.01 |
| `catalog` / Product Maintenance | Configuration only: EB product lines (HMO, GLI, GPA, risk item kind PERSON / GENERIC), EB products, HMO providers as panel insurers with commission rates; demo seeds in V1930, production through Product Maintenance | E2 / BDOI | EBQ01 |
| `crm` | None: `EbClientRecords` implements `ClientRecordsProvider` | E1-B | BRID-006 |
| `nbadmin` | None: `EbRetentionProvider` implements `RetentionCandidateProvider` (EB_PROGRAMME, EB_MEMBER) | E1-C | NFR |
| `collections` (being built) | None: EB invoices enter the worklist like any invoice; Collection users see EB SOA and billing documents through the COLLECTION access class | - | BRID-021, 025 |
| `commission` | None: EB accounts billed directly by the insurer use the direct-payment arrangement | - | BRID-025 |
| `renewal` (Renewal BRD, designed, V1010-V1019) | **Decided (decision D3, EBQ28 closed for the boundary):** EB programmes are excluded from the general renewal candidate lists (`RNW_EXCLUDED_LINES` = the `EB_BENEFIT_LINE` values). Every renewal advice, EB's (job `EB_RENEWAL_ADVICE`) and Renewal's, is stored as document type `RENEWAL_ADVICE` linked to the account and the client (EB also links the programme), so CSF resends both. `RENEWAL_ADVICE` is seeded with `on conflict do nothing` by whichever of V1010 / V1030 runs first. Optional: a read method listing the current ARNs of EB programme lines would let Renewal exclude by ARN as well as by line | coordination | EBQ28 |
| User Access Maintenance (`security` / `nbadmin`, V1060-V1069) | **Decided (decision D7):** portal users are provisioned through UAM requests as an external user type; `ptl_user_request` is dropped. `portal` implements the `nbadmin` port `ExternalUserProvisioner` (E1-A, after UAM U1-A has committed the port) | E1-A | EBQ13 |

## 12. Integrations to park (seam only)

| Item | Seam | Question |
|---|---|---|
| Insurer system-to-system API | The portal endpoints are the API; OAuth2 client credentials for insurer systems are not enabled | EBQ13 |
| E-signature verification of the BOR | `BorValidator` port; default = validator checklist attestation | EBQ06 |
| HRIS / client payroll feed of master lists | `EB_MASTERLIST` bulk handler and portal upload | EBQ15 |
| Insurer mailbox reading (proposals, SOAs by e-mail) | AO uploads with the e-mail attached (`EML` / `MSG`) | - |
| EBIX booking (TAT annex) | Replaced by BIBS booking; `EB_PROGRAMME_LOAD` for migration | EBQ18 |
| Virus scanning product | `attachment.service.VirusScanner` adapter (required before the portal goes live) | EBQ13 |
| SMS / push notifications to portal users | `ptl_notice` and e-mail only | - |

## 13. Build-wave plan

| Wave | Agent | Owns (files) | Delivers | Depends on |
|---|---|---|---|---|
| E0 | Foundation | `db/migration/V1030__*`, `V1031__*`; `security/domain/Permission.java` (EB and portal entries); the BT0 files (`account/**` business type only, `booking/service/InvoiceBuilder.java`, `InvoiceBooked.java`, the three `nbreport` reports, `V822`) **only if** no Submitted Policies S0 or Renewal R0 has merged BT0 first; `booking/service/BookingService.java`, booking DTOs and `BOOKING_UPLOAD` handler (billing number); `report/render/**`; `messaging/service/DocumentProtector.java`; `attachment/**`; `eb/package-info.java` and `eb/domain/EbDocumentTypes.java` (constants) | Section 11 platform changes, roles, workflows, LOVs, parameters | BT0 merged (or built here) |
| E1-A | Portal | `portal/**` (including `PortalUserProvisioner` implementing `nbadmin`'s `ExternalUserProvisioner`), `config/SecurityConfig.java`, `security/service/JwtTokenService.java` (audience), `db/migration/V1032__*`, `frontend/src/portal/**` (shell, sign-in, home, uploads), `frontend/src/features/admin/PortalUsers*` | Realm, users, invitations, staging and review, notices; **the ports of 2.2 are committed on day one** | E0; UAM U1-A (the provisioner port and the EXTERNAL request type) |
| E1-B | EB marketing | `eb/domain` and `eb/service` classes for programme, cycle, RA, feedback, document, BOR, franchise, TOR, insurer request, proposal, revision, comparative, threshold, confirmation, submission, required document; `eb/api` for the same; `db/migration/V1033__*`, `V1034__*`; `frontend/src/features/eb/` except members, pending items, SOA and reports | BRID-001-004, 007-012, 015-017, 024, 026, 027, 029 | E0; portal ports (interfaces only) |
| E1-C | EB servicing and reports | `eb/domain` and `eb/service` classes for roster, member, member change, tracked item, SOA; `eb/report/**`; EB jobs; `db/migration/V1035__*`, `V1036__*`; `frontend/src/features/eb/{members,pending,soa}*` | BRID-013, 019, 021, 022, 025 (EB side), 030 | E0 |
| E2 | Integration | `eb/api/portal/**` (EB portal endpoints on `PortalContext`), `eb/service/EbUploadTargets.java`, `EbPortalTasks.java`, portal EB screens in `frontend/src/portal/eb/**`, `db/demo/V1930-V1932`, integration tests, `ApiSmokeIT` entries, help entries, `docs/modules/EMPLOYEE_BENEFITS.md` | End-to-end NB and renewal cycles through the portal; demo storyline | E1-A, E1-B, E1-C |

Parallel-work rules:
- E1-A, E1-B and E1-C start together after E0 is merged. Nobody but E0 edits `Permission.java`, `account/**`, `booking/**`, `attachment/**`; later needs go to E0's owner as a follow-up commit.
- E1-B and E1-C share `eb/` by class ownership as listed; shared constants live in `eb/domain/EbDocumentTypes.java` and `eb/service/EbParameters.java`, created by E0 and changed only by additions.
- E1-B and E1-C code against the portal ports but do not implement them; E2 wires them.
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
| Threshold values and approvers | EBQ11 | Rule table with demo values TSI 500M, premium 20M |
| Portal security model | EBQ13 | Realm, invitation, lockout, e-mail OTP; production IdP / MFA decision open |
| Member data and privacy rules | EBQ15 | Roster with access classes and download logs; retention rule placeholders |
| Movement types, "no payment no booking" | EBQ16 | LOV and parameter |
| Report layouts, TAT events | EBQ21 | Reports with the BRD columns; activity log stamps |

## 15. Risks

1. **Internet-facing portal.** The first external surface of BIBS. BDO Information Security approval, penetration testing, a production virus scanner and the MFA method can delay go-live. Mitigation: build the portal first (E1-A) and run the internal flow without it (AO uploads on behalf of insurers and clients) until it is approved.
2. **Sensitive personal data.** Rosters and utilization reports. Mitigation: access classes, download logs, minimal roster fields until EBQ15 is answered, no health data in the roster.
3. **Boundary with the Renewal BRD.** Two renewal processes could send two RAs to the same client. Mitigation (decided, D3): EB lines excluded from the Renewal lists (EBQ28), one document type `RENEWAL_ADVICE` for every RA.
4. **Shared platform changes** (attachment access classes, DOCX export, business type on the account) touch every module. Mitigation: they are cross-BRD prerequisite work items (`BDOI_CROSS_BRD_DECISIONS.md` section 6); the business type is BT0 (V822), built once; E0 lands the access classes and DOCX export with defaults that keep today's behaviour.
5. **Undefined thresholds and TATs.** Mitigation: every value is a parameter or a rule row.

## 16. E0 foundation: as built (BDOI Drop 2, no portal)

What the E0 wave built, and where it details or differs from the sections above. E1-B and E1-C build on this and
compile only against it.

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
| `EbUploadTargets`, `EbPortalTasks`, `eb/api/portal/**`, portal demo users | E2 | Not built; E2 keeps the demo storyline and the end-to-end tests of the internal flow | E2 |

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

Renewal R0 and Submitted Policies S0 only check that BT0 is merged. Renewal still adds its fast track,
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
