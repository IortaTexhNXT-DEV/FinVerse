# Customer Servicing Facility (BRD-9)

Module guide of `csf` (package `com.iortatechnxt.brokerverse.csf`, screens `frontend/src/features/csf`). Design:
[`CUSTOMER_SERVICING_DESIGN.md`](../architecture/CUSTOMER_SERVICING_DESIGN.md) (section 16 as built). Functional
requirements: FRS BRD-9 v1.0 (`docs/deliverables/src/BRD-09_Customer_Servicing_Facility`).

## 1. Purpose

- The **Customer Servicing Facility (CSF)** is the workspace of the BDO Insure Contact Center. It replaces the
  BDO-Insure Front-End System: agents find the client of a caller, see every account with its status, the payments, the
  renewal advices, the e-policies and the documents, and serve the caller.
- CSF is a **servicing workspace, not a data store**. Client, account, invoice, payment, e-policy, renewal advice and
  document data stay in their owning modules and are read through their query services when a tab opens. CSF keeps
  only its own records: the caller verifications, the contact changes (applied, refused, referred) with their field
  rows, the legacy contact sync outbox and the agent activity log (`csf_*`, V1041).
- **Customer Search** by one key: name, client ID (client or prospect code, government ID number), account number (ARN
  with or without its suffix, policy number, legacy reference of a migrated account), PN number, loan application number
  (account and CLPC billing items). Results are grouped by client with the matching accounts; one match opens the
  Servicing View; accounts only in the legacy systems come from the port `LegacyAccountLookup` (empty today).
- **Servicing View**: summary card (codes, status, KYC, tags, instructions, contact details, the valid verification) and
  the tabs Accounts, Payments, Renewal Advice, E-policies, Documents and Contact History. Each tab loads on its own.
- **Verify, then change**: a contact change needs a PASSED verification of the same client that is still valid. The
  change calls the contact-only contract of the client master (`ClientService.updateContact`): e-mail, mobile, phone and
  address lines only, validated with the client master rules, audited on the client with the values before and after,
  the source CSF, the change number and the reason, and published as `ClientContactChanged`.
- Any other field (name, civil status, birth date, ID, TIN) is **refused** (`CSF_FIELD_NOT_UPDATABLE`, the refusal is
  kept as a REFUSED record) and **referred to the fulfilment unit**: a REFERRED record and an Operations hand-off
  (port `CSF_FULFILMENT_REFERRAL`, notified to the users with `CLIENT_MAINTAIN`, listed on Operations > Hand-offs and Extracts).
- **Resend** of a renewal advice (document type `RENEWAL_ADVICE` of the client or its accounts, decision D3) through the
  e-mail outbox, password protected with the password in a separate e-mail; of a **confirmed** e-policy through the
  dispatch service of Issuance (appears in the e-policy dispatch report). The recipient is the registered e-mail;
  another address needs `CSF_RESEND_OTHER` and a reason.
- **Documents**: upload to the client or an account with a type of `CSF_DOCUMENT_TYPE` (the platform file types plus TXT,
  RTF, HEIC / HEIF, GIF, BMP, TIFF, WEBP, checked by extension and content), list of every document of the client, its
  accounts and its quotations under the document access classes, download of one file or a ZIP.
- **Everything an agent does is logged** in `csf_activity` (search with criteria, view, download, resends, upload,
  verification, contact change, referral); the log cannot be switched off.
- No workflow, no accounting events, no GL entries: every action is immediate and audited.

## 2. Personas and roles (V1040; matrix proposal until CSQ10)

| Persona | Role | SIT/UAT user | Permissions |
|---|---|---|---|
| Contact Center Agent | `CSF_AGENT` | `csfagent`, `csfagent2` | `CSF_VIEW`, `CSF_CONTACT_UPDATE`, `CSF_RESEND`, `CSF_DOCUMENT_UPLOAD`, `ATTACHMENT_VIEW` |
| Contact Center Supervisor | `CSF_SUPERVISOR` | `csfsup` | the agent's, `CSF_RESEND_OTHER`, `CSF_REPORT_VIEW`, `REPORT_VIEW` |
| Contact Center Management (Leads / Heads) | `CSF_MANAGEMENT` | `csfmgmt` | `CSF_VIEW`, `CSF_REPORT_VIEW`, `AUDIT_VIEW`, `REPORT_VIEW` |
| System Administrator | `SYSADMIN` | existing | users and roles |

Agents hold neither `CLIENT_MAINTAIN` nor `EPOLICY_SEND`. CSF roles see every client and account, CBG and non-CBG
(CQ06). The Customer Search is the landing page of the CSF roles (`navigation/access.landingPath`). The persona menus
are in `frontend/src/navigation/personaMenus.json` (suite BRD-9).

## 3. Screens (Client & Policy, Customer Service Facility)

| Screen | Path | Permission |
|---|---|---|
| Customer Search | `/csf` | `CSF_VIEW` |
| Servicing View | `/csf/clients/:clientId` (tab in `?tab=`) | `CSF_VIEW`; Update Contact and Refer to Fulfilment Unit `CSF_CONTACT_UPDATE`; Resend `CSF_RESEND`; Upload `CSF_DOCUMENT_UPLOAD`; downloads `ATTACHMENT_VIEW` |
| Contact Changes | `/csf/changes` | `CSF_REPORT_VIEW` or `CSF_CONTACT_UPDATE` |
| Customer Service Reports | `/csf/reports` (Report Centre, category Customer Service) | `CSF_REPORT_VIEW` |
| Audit Trail (`CTL-AUDIT`, entity Client) | Administration | `AUDIT_VIEW` |

Dialogs: Update Contact (stepper Verify Caller, Change Contact), Refer to Fulfilment Unit, Resend (preview, recipient,
other address with reason), Upload Document (`FileDropZone`). Help entries: `features/csf/help.ts`.

## 4. API (`/api/v1/csf`, every call with `companyId`)

| Method and path | Use | Permission |
|---|---|---|
| `GET /search?keyType=&q=` | Customer Search | `CSF_VIEW` |
| `GET /clients/{id}` | Summary (logs VIEW) | `CSF_VIEW` |
| `GET /clients/{id}/accounts`, `/payments?months=&arn=`, `/renewal-advices`, `/epolicies`, `/documents`, `/contact-changes` | Tabs | `CSF_VIEW` |
| `POST /clients/{id}/verifications` | Verification checklist | `CSF_CONTACT_UPDATE` |
| `POST /clients/{id}/contact-changes` | Contact change (`values` by field; blank clears) | `CSF_CONTACT_UPDATE` |
| `POST /clients/{id}/referrals` | Referral to the fulfilment unit | `CSF_CONTACT_UPDATE` |
| `GET /clients/{id}/resend-preview?kind=RA\|EPOLICY&documentId=` | Resend dialog | `CSF_RESEND` |
| `POST /clients/{id}/resend-advice`, `/resend-epolicy` | Resend (another address: `CSF_RESEND_OTHER`) | `CSF_RESEND` |
| `POST /clients/{id}/documents` (multipart) | Upload | `CSF_DOCUMENT_UPLOAD` |
| `GET /clients/{id}/documents/{attachmentId}/content`, `/documents/zip?ids=` | Logged download | `CSF_VIEW` and `ATTACHMENT_VIEW` |
| `GET /contact-changes?status=&agent=&from=&to=&q=` | Contact Changes list | `CSF_REPORT_VIEW` or `CSF_CONTACT_UPDATE` |

## 5. Contracts of other modules used by CSF

| Module | Contract | Added by CSF |
|---|---|---|
| `crm` | `ClientService.updateContact(id, ContactChange)`, `ClientContactChanged`, `ClientRules.contactViolations`, `Client.changeContact`; `ClientSearchService`, `ClientNotesService.banner` | yes (contact-only contract) |
| `account` | `AccountQueryService.byAccountNumber`, `byLoanApplication` (new), `search` (PN), `byClient` | yes (two reads) |
| `placement` | `PlacementQueryService.arnsByLoanApplication` | yes |
| `opsledger` | `InvoiceLedgerQueryService.paymentsOfClient` (new), `forArn`, `byNumbers`; `HandoffService.record` (referrals) | yes (one read) |
| `cashiering` | `CashReceiptService.modesOf` (mode of payment of receipts) | yes |
| `issuance` | `IssuanceQueryService.policyFor`, `EpolicyService.get`, `EpolicyDispatchService.draft / dispatch` | no |
| `quotation` | `QuotationQueryService.byClient` (documents of the quotations) | no |
| `attachment` | `DocumentService.list / upload / downloadable / zip / download`, `AllowedFileType` (+7 types) | yes (file types) |
| `catalog` | `CatalogNames` (product and insurer names: agents do not read the catalog) | no |

## 6. CSF status of an account

List `CSF_STATUS_MAP` (maintained with maker and checker): the code is an account stage (`ANY` for every stage),
optionally followed by `_OUTSTANDING` (premium receivable of the account's invoices not fully paid) or `_EXPIRED` (policy
period ended); the group above the row is the CSF status (list `CSF_STATUS`: Pending, Awaiting, Booked, Open, Closed);
rows are read in their order and the first match applies (`CsfStatusRules`). Seed (to confirm, CSQ04): Cancelled,
Voided, Placement Cancelled and Booked past its period Closed; Booked with premium outstanding Booked; Booked Open;
Draft, Submitted, Returned to Marketing Pending; Awaiting Payment, Ready for Placement, Placed, Returned by Insurer,
Policy Issued Awaiting. A changed row applies at once (the list is read on every call; a change made by SQL needs the
cache `lov-values` cleared).

## 7. Legacy contact sync

Every applied change gets one outbox row per legacy system (QPS, EBIX) with its payload (change number, client code,
bank CIF, fields before and after): NOT_CONFIGURED while `CSF_LEGACY_SYNC_ENABLED` is false (today), QUEUED otherwise.
The job `CSF_LEGACY_SYNC` (manual by default; cron `brokerverse.jobs.csf-legacy-sync-cron`, every 15 minutes once the
interface exists, workload INTEGRATION) first queues the rows kept while the sync was off (replay), then sends the
queued and failed rows through the port `ContactSyncGateway`. The default adapter `NotConfiguredContactSync` has no
transport: every attempt fails with a clear reason and raises `CSF_SYNC_FAILED`. The change shows Sent, Failed, Queued or
Not Configured in the Contact History and the Contact Changes report.

## 8. Parameters, lists and alerts (V1040)

| Parameter (category CUSTOMER_SERVICE) | Default | Meaning |
|---|---|---|
| `CSF_PAYMENT_HISTORY_MONTHS` | 12 | Months of the Payments tab before Show Older (1-120) |
| `CSF_VERIFY_MIN_MATCHES` | 2 | Checks that must match |
| `CSF_VERIFY_MAX_FAILS` | 3 | Failed verifications of a client in a day before the alert |
| `CSF_VERIFICATION_VALID_MINUTES` | 30 | Validity of a passed verification |
| `CSF_LEGACY_SYNC_ENABLED` | false | Send contact changes to QPS and EBIX |
| `CSF_SEARCH_MIN_CHARS` | 3 | Minimum characters of a name search |
| `CSF_SEARCH_MAX_RESULTS` | 50 | Maximum clients of a search |

Lists: `CSF_STATUS`, `CSF_STATUS_MAP`, `CSF_DOCUMENT_TYPE` (Client request or letter, Valid ID, Proof of address, Photo
sent by the client, Official receipt, Policy copy, Others; the new platform document types `CLIENT_REQUEST`,
`PROOF_OF_ADDRESS`, `CLIENT_PHOTO`), `CSF_VERIFY_CHECK` (address, contact number, e-mail, insured property),
`CSF_CHANGE_REASON`, `CSF_CHANNEL` (hotline, e-mail, website), `CSF_REFERRAL_FIELD` (name, civil status, birth date,
ID document, TIN, others). Change numbers `CSF-<yyyy>-nnnnnn`.

Alerts (exception codes, module CUSTOMER_SERVICE): `CSF_VERIFICATION_FAILED_REPEAT` (HIGH; de-duplicated per client and
day), `CSF_SYNC_FAILED` (MEDIUM; per outbox row). Retention rule `CSF_CONTACT_CHANGE` (APPLIED, REFUSED, REFERRED; 5
years online, 10 in the archive, review), provider `CsfRetentionProvider`.

## 9. Reports (category Customer Service, `CSF_REPORT_VIEW`, archived)

| Code | Report | Parameters |
|---|---|---|
| `CSF-CONTACT-CHANGES` | One row per changed field: change number, time, client, field, old and new value, status, verification (result and checks), channel, agent (name), reason, sync status | company, from, to, agent (user ID), client code |
| `CSF-ACTIVITY` | Layout Summary: counts per agent and day by action with totals; Detail: every action with client, reference and detail, per agent | company, from, to, agent, layout, action, client code |
| `CTL-AUDIT` (existing) | Audit trail, entity Client (contact changes carry the values before and after) | as today (`AUDIT_VIEW`) |

Exports: PDF, Excel, CSV (and ODS, XML, Word) from the report framework. Days are the business days of Manila.

## 10. Seed data

V1940 loads the users `csfagent`, `csfagent2`, `csfsup`, `csfmgmt` (password as every seed user), a passed
verification of CL-2026-000001 with the applied mobile change CSF-2026-000001 (outbox rows QPS and EBIX, not configured),
a failed verification and the refused civil status change CSF-2026-000002 of CL-2026-000005, and the agents' activity.
`CsfSeedData` (seed profile, once) stores a renewal advice PDF on an account of CL-2026-000001 linked to the client. The
seed search keys: name "Santos", PN `PN-0441101`, application number `AL-2026-004411` (ARN-2026-900002).

## 11. Tests

`CsfContactChangeIT` (verification, alert on the third failure, change and audit, refusals, referral and hand-off,
legacy sync job and alert, retention), `CsfServicingIT` (search by every key, servicing view and status mapping change,
HEIF upload and refused executable, downloads, RA resend with the recipient rules, e-policy resend), `CsfReportsApiIT`
(both reports and their exports, the API under the CSF permissions), `CsfStatusRulesTest`, `AllowedFileTypeTest`,
`ApiSmokeIT` (CSF reads), `PersonaMenusIT` (suite BRD-9). Frontend: `features/csf/pages.test.tsx`,
`csfCodes.test.ts`, `navigation/personaMenus.test.ts`, `navigation/access.test.ts`.

## 12. Troubleshooting

| Symptom | Check |
|---|---|
| "Verify the caller before changing the contact details" | The verification is of another client, or none was recorded; record the checklist again |
| "The verification of the caller has expired" | More than `CSF_VERIFICATION_VALID_MINUTES` passed; verify again |
| An account shows no status | No row of `CSF_STATUS_MAP` applies to its stage; add one |
| The Renewal Advice tab is empty | No `RENEWAL_ADVICE` document is linked to the client or its accounts, or its access class excludes the CSF roles (`att_document_access`) |
| No Resend on an e-policy | The e-policy is not confirmed yet (Issuance extraction review) |
| Contact changes stay Not Configured | Expected while `CSF_LEGACY_SYNC_ENABLED` is false; once a `ContactSyncGateway` exists, switch it on and run `CSF_LEGACY_SYNC` (the kept rows are replayed) |
| A referral stays With Fulfilment | The hand-off is listed on Operations > Hand-offs and Extracts; it is closed there (Cashiering or interface administrators) once the fulfilment unit has changed the client master |
