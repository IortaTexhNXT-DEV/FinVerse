# iNXT BrokerVerse - BDOI Customer Servicing Facility (BRD-9) Build Design

Status: **proposal for review**. This design extends `docs/architecture/BROKING_ARCHITECTURE.md`, `docs/architecture/OPERATIONS_DESIGN.md` and the Developer Guide, which stay binding. It does not change them, except for the contract changes listed in section 11.

Requirements baseline: [`BDOI_CSF_BRD_SPEC.md`](../requirements/BDOI_CSF_BRD_SPEC.md): 23 requirement rows (BRCSF-001 to 011 with their process steps, e-mail items 7 and 9; case management OUT) and questions CSQ01-CSQ16. Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRCSF-004`.

## 1. Design principles

1. **CSF is a servicing workspace, not a data store.** Client, account, invoice, payment, e-policy, RA and document data stay in their owning modules. CSF composes them through public query services and keeps only what is its own: the contact change requests with their verification, the legacy sync outbox and the agent activity log.
2. **Agents get narrow write rights.** Agents do not receive `CLIENT_MAINTAIN` or `EPOLICY_SEND`. CSF endpoints under CSF permissions call a contact-only crm contract and the issuance dispatch service. Anything else (civil status, name, ID) is refused and routed to the fulfilment unit (e-mail topic 3).
3. **Verify, then change.** A contact change needs a recorded verification of the caller (e-mail topic 5); the rule (which checks, how many) is a parameter until CSQ03 is answered.
4. **One place for a client's contact.** The update is made once, in the BIBS client master. Writing it back to QPS / EBIX during coexistence is a **port with an outbox**; nothing is sent until the interface is specified (CSQ01).
5. **Everything an agent does is logged.** Searches, servicing views, downloads, resends, uploads and changes, per agent (BRCSF-010; Leads / Heads reporting).

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (seed) |
|---|---|---|---|---|
| `csf` (new, package `com.iortatechnxt.brokerverse.csf`, tables `csf_*`) | Customer search across keys, servicing view (client, accounts with CSF status, payment history, RAs, e-policies, documents), identity verification, contact change requests, RA and e-policy resend, document upload, activity log, legacy contact sync outbox, CSF reports | BRCSF-002-009, CSF-EM07, CSF-EM09 | crm, account, placement (read), issuance, booking (read), opsledger (read), cashiering (read), attachment, messaging, audit, report, lov, system, alert | V1040-V1041 (V1940) |
| `crm` (built) | + contact-only update contract; + additional contacts (if CSQ15 confirms) | BRCSF-004 | unchanged | V1042 |
| `attachment` (platform) | + seven file types | BRCSF-007 | unchanged | none |
| `placement`, `opsledger` (built) | + two read methods | BRCSF-003, 005 | unchanged | none |

### 2.1 Dependency graph (arrows = "depends on")

```
   crm   account   placement(read)   issuance   booking(read)   opsledger(read)   cashiering(read)
     \      |            |              |            |               |                 |
      +-----+------------+------------- csf ---------+---------------+-----------------+
                                         |
                    attachment, messaging, audit, report, lov, system, alert
```

- No module depends on `csf`.
- `cashiering` is read through `CashReceiptService` query methods only (receipts of a payer); if the Operations owner prefers, `opsledger` exposes the receipt facts through `paymentsOfClient` and `csf` drops the cashiering dependency.

### 2.2 Ports

| Port | Declared in | Default | Purpose |
|---|---|---|---|
| `ContactSyncGateway` | `csf.service.port` | `OutboxContactSyncGateway`: writes `csf_sync_outbox` rows with status `NOT_CONFIGURED`; no transport | Push accepted contact changes to QPS / EBIX while they coexist (CSQ01) |
| `LegacyAccountLookup` | `csf.service.port` | none (empty result); implemented by `migration` (`XrefLegacyAccountLookup`, BRD-13) from the key cross-reference `mig_key_xref` and the legacy archive ([`DATA_MIGRATION_DESIGN.md`](DATA_MIGRATION_DESIGN.md) §14.5, §16) | Look up accounts that exist only in QPS / EBIX / LOS by PN, application number or legacy reference, and legacy RAs in the archive (CSQ01, CSQ02, CSQ06: partially answered by BRD-13, which makes legacy read-only after cutover) |

## 3. Flyway allocation

Allocated to Customer Servicing Facility in the Developer Guide: **schema V1040-V1049, seed V1940-V1949.** Three schema versions are used.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1040__csf_foundation.sql` | S0 | Grants of the CSF permissions; roles `CSF_AGENT`, `CSF_SUPERVISOR`, `CSF_MANAGEMENT`; LOV types `CSF_STATUS_MAP`, `CSF_DOCUMENT_TYPE`, `CSF_VERIFY_CHECK`, `CSF_CHANGE_REASON`; `sys_parameter` rows; exception codes; retention rule `CSF_CONTACT_CHANGE` |
| `V1041__csf_tables.sql` | S1 | `csf_verification`, `csf_contact_change`, `csf_sync_outbox`, `csf_activity`; indexes for search: `placement` billing item loan application no. (`plc_billing_item(loan_application_no)`), `acc_account_pn(pn_no)` if missing |
| `V1042__crm_client_contacts.sql` | S1 | `crm_client_contact` (client, type EMAIL / MOBILE / PHONE / ADDRESS, value, primary flag, active, source); built only if CSQ15 confirms "add" means several contacts, otherwise the version stays free |
| `V1043`-`V1049` | - | Kept free |
| `db/seed/V1940__seed_csf.sql` | S2 | SIT/UAT users `csfagent`, `csfagent2`, `csfsup`, `csfmgmt`; status map; a verified contact change and one refused; activity rows |

Rules:
- `csf_*` tables store client code, ARN and invoice no. as plain values; foreign keys only to users and attachments.
- V1041 adds indexes to `placement` and `account` tables (V850 / V820), which exist before it on a fresh database. No column changes to other modules' tables, so their entities are unaffected.

## 4. Entities (key fields)

| Table | Key fields |
|---|---|
| `csf_verification` | client id / code, agent, channel HOTLINE / EMAIL / WEBSITE, checks performed (LOV `CSF_VERIFY_CHECK`: ADDRESS, CONTACT_NUMBER, EMAIL, INSURED_PROPERTY) with matched yes / no, result PASSED / FAILED, time; valid for `CSF_VERIFICATION_VALID_MINUTES` |
| `csf_contact_change` | change no. `CSF-<yyyy>-nnnnnn`, client id / code, verification id, field changes (field, old, new) as rows or JSON, reason (LOV `CSF_CHANGE_REASON`), status APPLIED / REFUSED, applied at, agent, sync status NOT_REQUIRED / QUEUED / NOT_CONFIGURED / SENT / FAILED |
| `csf_sync_outbox` | change id, target system QPS / EBIX, payload, status, attempts, last error |
| `csf_activity` | agent, action SEARCH / VIEW / DOWNLOAD / RESEND_RA / RESEND_EPOLICY / UPLOAD / CONTACT_CHANGE, client code, reference (ARN, attachment id, message id), criteria (for searches), time |

CSF has **no accounting events**, no GL entries and no workflow: every action is immediate and audited.

## 5. Services and behaviour

| Service | Behaviour | BRD |
|---|---|---|
| `CustomerSearchService` | Key type NAME (crm `ClientSearchService`), CLIENT_ID (client code, prospect code, ID number), ACCOUNT_NO (ARN incl. `-nn` suffix, policy number, legacy account number when migrated), PN_NO (`AccountQueryService.search` with `pnNumber`, and `preBooked`), APPLICATION_NO (`PlacementQueryService.arnsByLoanApplication`, then `LegacyAccountLookup`); results grouped by client with matching accounts; minimum length and wildcard rules from parameters; each search logged | BRCSF-003, 3.001 |
| `ServicingViewService` | Client summary (crm), accounts (`AccountQueryService.byClient`) with `CsfStatusMapper` status, policy numbers (`IssuanceQueryService.policyFor`), invoices and balances (`InvoiceLedgerQueryService.forArn`), payment history (`InvoiceLedgerQueryService.paymentsOfClient`, `CSF_PAYMENT_HISTORY_MONTHS`), RAs and documents (`DocumentService`), e-policies; each tab loads separately (< 3 s per tab); each view logged | BRCSF-002, 005, 008, 009 |
| `CsfStatusMapper` | Maps (account stage, invoice payment status, expiry) to the CSF status through LOV `CSF_STATUS_MAP` (rows: account stage pattern, payment status pattern, CSF status, order); seeds: PENDING = DRAFT / SUBMITTED / RETURNED_TO_MARKETING; AWAITING = AWAITING_PAYMENT / READY_FOR_PLACEMENT / PLACED / RETURNED_BY_INSURER; BOOKED = BOOKED with PR outstanding; OPEN = BOOKED and in force; CLOSED = CANCELLED / VOIDED / expired; to be confirmed (CSQ04) | BRCSF-005.001, CSF-EM07 |
| `VerificationService` | Records the checklist; PASSED when at least `CSF_VERIFY_MIN_MATCHES` checks match; a failed verification raises alert `CSF_VERIFICATION_FAILED_REPEAT` after `CSF_VERIFY_MAX_FAILS` per client and day | E-mail topic 5 |
| `ContactChangeService` | Needs a PASSED verification younger than `CSF_VERIFICATION_VALID_MINUTES` (`CSF_VERIFICATION_REQUIRED`); calls `ClientService.updateContact`; refuses other fields (`CSF_FIELD_NOT_UPDATABLE`); validates with the crm rules (e-mail, mobile formats); writes `csf_contact_change` and queues the sync through `ContactSyncGateway` when `CSF_LEGACY_SYNC_ENABLED` | BRCSF-002, 004, 010 |
| `ResendService` | RA: attachment of type `RENEWAL_ADVICE` linked to the client or account, sent by `MessageService.queueEmail` with protection and separate password; e-policy: `EpolicyDispatchService` for the account's confirmed e-policy; recipient = registered e-mail, another address only with `CSF_RESEND_OTHER` and a reason | BRCSF-006, 6.001, CSF-EM09 |
| `CsfDocumentService` | Upload to the client or an account (`AttachmentService`, document type from `CSF_DOCUMENT_TYPE`); list of documents across the client's records grouped by type; download and ZIP through `attachment` (access classes apply); each download logged | BRCSF-007, 009 |

## 6. Security

### 6.1 Permissions (added to `security.domain.Permission` in S0; granted in V1040)

| Permission | Used for |
|---|---|
| `CSF_VIEW` | Search and servicing view |
| `CSF_CONTACT_UPDATE` | Verification and contact changes |
| `CSF_RESEND` | Resend RA and e-policy to the registered e-mail |
| `CSF_RESEND_OTHER` | Resend to another address (supervisor) |
| `CSF_DOCUMENT_UPLOAD` | Upload documents from the CSF view |
| `CSF_REPORT_VIEW` | CSF reports |

### 6.2 Roles (V1040) and SIT/UAT users (V1940)

| Role | Persona | Permissions | SIT/UAT user |
|---|---|---|---|
| `CSF_AGENT` | BDO Insure Contact Center Agent | CSF_VIEW, CSF_CONTACT_UPDATE, CSF_RESEND, CSF_DOCUMENT_UPLOAD, ATTACHMENT_VIEW | `csfagent`, `csfagent2` |
| `CSF_SUPERVISOR` | Contact Center Supervisor / personnel | CSF_AGENT permissions + CSF_RESEND_OTHER, CSF_REPORT_VIEW | `csfsup` |
| `CSF_MANAGEMENT` | Contact Center Management, Leads / Heads | CSF_VIEW, CSF_REPORT_VIEW, AUDIT_VIEW, REPORT_VIEW | `csfmgmt` |
| `SYSADMIN` (existing) | System Admin | user and role management (BRCSF-001) | existing administrator |

The final matrix waits for CSQ10.

## 7. Jobs, alerts, parameters, LOVs

- Job `CSF_LEGACY_SYNC` (every 15 minutes, manual only until `CSF_LEGACY_SYNC_ENABLED`): sends QUEUED outbox rows through the gateway; failures raise `CSF_SYNC_FAILED`.
- Alerts: `CSF_SYNC_FAILED`, `CSF_VERIFICATION_FAILED_REPEAT`.
- Parameters (category CUSTOMER_SERVICE): `CSF_PAYMENT_HISTORY_MONTHS` (12), `CSF_VERIFY_MIN_MATCHES` (2), `CSF_VERIFY_MAX_FAILS` (3), `CSF_VERIFICATION_VALID_MINUTES` (30), `CSF_LEGACY_SYNC_ENABLED` (false), `CSF_SEARCH_MIN_CHARS` (3), `CSF_SEARCH_MAX_RESULTS` (50).
- LOVs: `CSF_STATUS_MAP`, `CSF_DOCUMENT_TYPE`, `CSF_VERIFY_CHECK`, `CSF_CHANGE_REASON`.
- Number: `CSF-<yyyy>` for contact changes.

## 8. Platform and infrastructure items

| Item | Treatment | BRD |
|---|---|---|
| File types | `AllowedFileType` gains TXT (`text/plain`, text probe), RTF (`{\rtf`), HEIC / HEIF (`ftyp` brand heic / heix / mif1 at offset 4), GIF (`GIF8`), BMP (`BM`), TIFF (`II*\0` / `MM\0*`), WEBP (`RIFF....WEBP`); the frontend accept list follows | BRCSF-007 |
| Backup every 15 minutes | PostgreSQL continuous WAL archiving (`archive_mode = on`, `archive_timeout = 900`) to the designated secure storage, daily base backups, monthly restore test; documented in `docs/operations/` by the infrastructure owner; BIBS-wide (CSQ11) | BRCSF-011.001 |
| Response times | Search on indexed keys; the servicing view loads per tab; no report runs on the agent's request path | NFR |

## 9. Reports (category `CUSTOMER_SERVICE`, package `csf.report`)

| Code | Report | Parameters | Permission |
|---|---|---|---|
| `CSF-CONTACT-CHANGES` | Contact changes with old / new values, verification result, agent, reason, sync status | date range, agent, client | CSF_REPORT_VIEW |
| `CSF-ACTIVITY` | Agent activity counts and detail (searches, views, downloads, resends, uploads, changes) | date range, agent, action | CSF_REPORT_VIEW |
| `CTL-AUDIT` (existing) | Audit trail, entity type Client | as today | AUDIT_VIEW |

PDF and Excel export come from the report framework (BRCSF-011.002).

## 10. Screens (group **Client & Policy**, section **Customer Service Facility**; UX-3)

| Screen | Route | Content | Permission |
|---|---|---|---|
| Customer Search | `/csf` | Landing page of the CSF roles: key type selector (Name, Client ID, Account No., PN No., Application No.), search box, results grouped by client with matching accounts; "No client found" empty state | CSF_VIEW |
| Servicing View | `/csf/clients/:clientId` | Record details pattern: back arrow, summary card (code, name, status pill, flags, contact facts with **Update Contact** link), tabs Accounts (CSF status pill, BIBS stage, policy no., period, balance) / Payments / Renewal Advice (view, download, Resend) / E-policies (Resend) / Documents (upload, download, ZIP) / Contact History | CSF_VIEW |
| Update Contact dialog | modal | Step 1 verification checklist; step 2 contact fields only, reason; confirm | CSF_CONTACT_UPDATE |
| Resend dialog | modal | Document, recipient (registered e-mail; other address with reason for supervisors), preview, confirm | CSF_RESEND |
| CSF Reports | Report Centre, category Customer Service | `CSF-CONTACT-CHANGES`, `CSF-ACTIVITY`; Administration > Audit Trail for `CTL-AUDIT` | CSF_REPORT_VIEW / AUDIT_VIEW |

- `navigation/access.landingPath` sends CSF roles to `/csf`.
- Help entries in `frontend/src/features/csf/help.ts`, registered in `HELP_SECTIONS`.
- The feature follows `BDO_UX_GUIDELINES.md` (RecordSummary, Tabs in a flush Card, StatusBadge colours, EmptyState).

## 11. Impact on modules already built or being built (contract changes)

| Module | Change | Owner (wave) | BRD |
|---|---|---|---|
| `crm` | `ClientService.updateContact(Long id, ContactChange change)`: e-mail, mobile, phone, address lines only; validates with `ClientRules`; recomputes the normalised soft keys (`ClientKeys`); audits before / after with source and reason; publishes `ClientContactChanged(clientId, code, fields)` after commit. Optional `crm_client_contact` (V1042) and `ClientService.addContact / endContact` if CSQ15 confirms | S1 | BRCSF-004 |
| `placement` | `PlacementQueryService.arnsByLoanApplication(companyId, loanApplicationNo)` (reads `plc_billing_item`) | S1 | BRCSF-003 |
| `opsledger` | `InvoiceLedgerQueryService.paymentsOfClient(companyId, clientCode, from)`: application movements with receipt no., date, mode, amount and invoice, newest first | S1 (ask to Operations owner) | BRCSF-005 |
| `issuance` | None: CSF calls `EpolicyDispatchService` inside its own transaction under `CSF_RESEND` | - | CSF-EM09 |
| `attachment` | Seven file types (section 8) | S0 | BRCSF-007 |
| `security` | CSF permissions (6.1) | S0 | BRCSF-001 |
| `renewal` (Renewal BRD, designed, V1010-V1019) | **Decided (cross-BRD decision D3):** every generated RA, including those of submitted policies, is an attachment of document type `RENEWAL_ADVICE` linked to the account (ARN) and the client, with its protection password handled by `DocumentPasswordPolicy` (RENEWAL_DESIGN section 2.3) | coordination | BRCSF-006 |
| `eb` (Employee Benefits, designed) | **Decided (D3):** same RA convention (job `EB_RENEWAL_ADVICE`) | coordination | BRCSF-006 |
| `brokerclaims` (Claims, designed, V1020-V1029) | **Decided (D3):** claims reports are attachments of document type `CLAIM_REPORT` linked to the claim, the account and the client (CLAIMS_BROKING_DESIGN section 3.1); which claim documents count is cross-BRD question XQ05 | coordination | BRCSF-009 |
| `attachment` access classes (EB V1031) | The access rows of `RENEWAL_ADVICE` and `CLAIM_REPORT` must let CSF roles list and download them (cross-BRD question XQ04 for the final confidentiality matrix, CSQ07) | coordination | BRCSF-006, 009 |
| `navigation` | Section Customer Service Facility in Client & Policy (`frontend/src/navigation/modules.ts`), after Renewal | S1 | UX-3 |

## 12. Integrations to park (seam only)

| Item | Seam | Question |
|---|---|---|
| QPS / EBIX contact write-back | `ContactSyncGateway` with outbox, status `NOT_CONFIGURED` | CSQ01 |
| QPS / EBIX / LOS account lookup for non-migrated accounts | `LegacyAccountLookup` (empty default; implemented by `migration` when BRD-13 is built). BRD-13 keeps legacy read-only after cutover and asks for no write-back, so `ContactSyncGateway` stays NOT_CONFIGURED unless BDOI confirms coexistence write-back | CSQ01, CSQ02 (partial) |
| Legacy RA files (IT text-file RAs) | Upload as `RENEWAL_ADVICE` attachments by a one-time bulk load if needed | CSQ06 |
| Case management / inquiry logging (SharePoint today) | Out of scope; `csf_activity` is the only log | CSQ09 |
| BDO SSO / Windows ID | Q42 answered by BRD-11: directory sign-in is required and is built as the parked `security` port `DirectoryAuthenticator` (USER_ACCESS_DESIGN section 10); local user ID and password sign-in (BRCSF-001) stays until BDO supplies the interface | Q42, UQ04 |

## 13. Build-wave plan

| Wave | Team | Owns (files) | Delivers | Depends on |
|---|---|---|---|---|
| S0 | Foundation | `db/migration/V1040__*`; `security/domain/Permission.java` (CSF entries); `attachment/domain/AllowedFileType.java` and its tests; `csf/package-info.java` | Permissions, roles, LOVs, parameters, file types | - |
| S1 | CSF core | `csf/**`; `db/migration/V1041__*`, `V1042__*`; `crm/service/ClientService.java` (`updateContact` only) and `crm/domain/ContactChange.java`, `ClientContactChanged.java`; `placement/service/PlacementQueryService.java` (one method); `opsledger/service/InvoiceLedgerQueryService.java` (one method, agreed with the Operations owner); `frontend/src/features/csf/**`, `frontend/src/api/csf.ts`, `navigation/modules.ts` (one entry) | Search, servicing view, verification, contact change, resend, documents, reports | S0 |
| S2 | Integration | `db/seed/V1940__*`, integration tests (`@IntegrationTest` as `csfagent`, `csfmgmt`), `ApiSmokeIT` entries, report export tests, help entries, `docs/modules/CUSTOMER_SERVICING.md` | Seed storyline and definition of done | S1; RA documents appear once the Renewal or EB module stores them (the tab shows "No items to display" until then) |

Parallel-work rules:
- CSF can be built in parallel with EB. The two touch `Permission.java` (different entries), `attachment` (EB: access classes; CSF: file types, different files) and `navigation/modules.ts` (one line each); merge order E0 then S0 avoids conflicts, and neither edits the other's lines.
- The one-method changes to `crm`, `placement` and `opsledger` are additive; they are agreed with the module owners before S1 starts.
- Migrations use only V1040-V1042 and V1940.

## 14. What depends on information BDOI has not given (build the seam, park the content)

| Item | Question | What is built |
|---|---|---|
| Legacy write-back and legacy accounts | CSQ01, CSQ02 | Ports with outbox / empty default |
| Verification rule | CSQ03 | Checklist with parameters (2 of 4 checks) |
| Status definitions | CSQ04 | Maintainable LOV mapping with seeds |
| Payment history content | CSQ05 | Receipts and applications, 12 months |
| Resend to other addresses | CSQ06 | Supervisor permission with reason |
| Document confidentiality | CSQ07 | Attachment access classes (EB design) |
| Roles | CSQ10 | Three roles |
| Additional contacts | CSQ15 | V1042 reserved; built on confirmation |

## 15. Risks

1. **Legacy coexistence.** If QPS / EBIX stay systems of record after BIBS go-live, contact changes diverge until the write-back exists. Mitigation: the outbox keeps every change with its payload; a replay is possible once the interface is specified.
2. **RA availability.** The RA tab depends on the Renewal and EB modules storing RAs with the agreed document type. Mitigation: the convention is in both designs; the tab is empty, not broken, until then.
3. **Over-broad agent access.** Agents see every segment and every document type unless access classes restrict them. Mitigation: the attachment access classes of the EB design apply to CSF lists and downloads, and every view and download is logged.
