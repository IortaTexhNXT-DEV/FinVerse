# Document and file storage: decision record

Status: **Approved by BDOI, 26-Sep-2026** (option C, GuardDuty, governance and legal hold per the DOA, BDOI-owned keys,
ECM for records management). BDOI instruction: "documents and attachments live in an S3 bucket only".
Build step ST0 is built (section 6). Build step ST1 is built (section 7): every module writes its files through the
file store; the copy of the existing bytes and the later drop of the `bytea` columns are run per environment.

## 1. What is stored today

File bytes sit in PostgreSQL `bytea` columns. There are at least 13 tables, among them:
- attachments (`att_*`, V21);
- messaging e-mail files (V753);
- ledger and Operations documents (V761, V762);
- cashiering, remittance, placement, issuance and booking documents (V763, V770, V850, V860, V870);
- disbursement vouchers (V892);
- collections (V1002);
- report runs and batches (V32, `ReportRunFile`); the Word rendition table `doc_rendition` holds only the specification and the SHA-256, not bytes;
- screening case documents, STR files and watchlist files;
- KYC documents, DP lists, payment-request documents and journal uploads.

Each module reads and writes the bytes itself.

## 2. Options considered

| Option | Description | For | Against |
|---|---|---|---|
| A. Keep in the database | `bytea`, as today | One transaction for record and file; simple backup | The database grows with documents: the IER sizes production RDS at 2 TB, and a large share would be PDFs. Backups, restores, cross-region replication (RPO 15 min) and DR tests get slower and dearer. PostgreSQL is not a file store. Does not meet BDOI's instruction |
| B. S3, every download streamed through BIBS | Bytes in S3; BIBS reads the object and sends it to the browser | Every download passes the permission check and the audit log in one place | Every file travels through the application pods, which uses bandwidth and memory and slows large reports and bulk files |
| **C. S3 for bytes, PostgreSQL for metadata, short-lived presigned links (recommended)** | The database keeps the record: owner, type, name, size, SHA-256, S3 key, retention class and legal hold. S3 keeps the bytes. BIBS checks permission and writes the audit entry, then returns a presigned URL valid for a few minutes | Standard AWS practice; the database stays small and fast; S3 handles durability, versioning, lifecycle, replication and legal hold; the permission check and audit trail stay in BIBS | Needs a storage port, a migration of existing bytes, and care with key naming and URL expiry (handled below) |
| D. ECM | Store in BDO's Enterprise Content Management | Enterprise records management | BDOI chose S3; ECM can still receive copies of selected records through its integration if BDOI later asks |

**Recommendation: option C.** Option B is kept as the path for downloads that must be streamed: password-protected
e-mail attachments, and ZIP bundles that are built on the fly.

## 3. Design (option C)

**Storage port.**
- One platform component, `common/storage` → `FileStore`, with the operations `put`, `get`, `presignedGet`, `presignedPut`, `delete`, `copy`, `exists`, `metadata`.
- Every module calls the port instead of holding bytes.
- Adapters:
  - `S3FileStore` (AWS SDK v2), used in DEV, SIT, UAT, pre-production, production and DR;
  - `LocalFileStore` (filesystem), for developer machines and automated tests;
  - an S3-compatible container (MinIO), for the integration test of the S3 adapter itself.

**Metadata.**
- A shared table `stored_file` holds id, company, owner entity type and id, document type, file name, content type, size, SHA-256, bucket, key, version id, retention class, legal-hold flag, created by / at and deleted at.
- Module tables replace their `bytea` column with a `stored_file_id`.
- The SHA-256 is checked on upload and can be re-checked on read.

**Buckets.** One set per environment and account; public access blocked at account and bucket level.

| Bucket | Content | Lifecycle |
|---|---|---|
| `bibs-<env>-documents` | Attachments, generated documents (slips, invoices, ORs, SOAs, vouchers, letters, STRs), Word and PDF renditions | Versioning on; Standard, then Standard-IA after 90 days, then Glacier Instant Retrieval after 1 year. Retention and deletion follow the BIBS retention rules per record type (5 / 15 years by default, 10 / 15 years for claims, AMLA periods for screening) |
| `bibs-<env>-reports` | Report runs, scheduled files, batch ZIPs | Expiry per report archive setting (default 400 days) |
| `bibs-<env>-inbound` | Bulk upload files, bank and insurer files, watchlist feeds | Quarantine prefix until the malware scan passes; kept 90 days |
| `bibs-<env>-migration` | Migration extracts and staging files | **Expire after 5 days** (hosting appendix); access restricted to the migration role |

**Security.**
- Encryption: SSE-KMS with a customer-managed key per environment. Bucket policy denies unencrypted puts and non-TLS access.
- Network: access only from the BIBS pods, through IAM Roles for Service Accounts on EKS and an S3 VPC gateway endpoint; no public route.
- Object keys carry no personal data: `<company>/<entity-type>/<yyyy>/<mm>/<uuid>`. The file name lives only in the metadata and in the `Content-Disposition` of the presigned link.
- Presigned GET links:
  - valid for 5 minutes (parameter `FILE_LINK_TTL_SECONDS`) and issued only after the BIBS permission check;
  - each issue writes an audit entry (who, what, when, from where), which also covers the archive-inquiry logging of BRD-13;
  - `Content-Disposition: attachment` and a no-store cache header are forced.
- Uploads:
  - Small files (up to 25 MB) go through BIBS, which validates the type and size.
  - Large bulk files go by presigned PUT into the `inbound` quarantine prefix.
  - The malware scan (Amazon GuardDuty Malware Protection for S3, or the scanner BDOI IT prescribes) tags each object; BIBS accepts only clean objects.
- Legal hold and immutability: S3 Object Lock in governance mode on the documents bucket. Records under audit or AMLA hold (STRs, filed BIR forms, official receipts) get a legal hold that the retention job cannot remove.

**DR and backup.**
- Cross-region replication of the documents and reports buckets to the DR region, with replication time control, which meets the IER RPO of 15 minutes.
- Versioning replaces file backups; database backups stay small.
- EFS is not used for documents; it keeps only transient pod scratch space if a component needs it.

**Transactions.** The object is written first, then the metadata row is saved.
- If the database transaction fails, the object is an orphan: a nightly reconciliation job deletes objects that have no row after 24 hours.
- A delete is soft in the database, then a lifecycle or retention job removes the object.

**Performance.** Downloads no longer pass through the application pods. Report generation writes straight to S3 and the user gets a link, which fits the "reports as jobs" direction for the volume and NFR targets.

## 4. Changes to BIBS (build step ST0, then ST1)

| Step | Scope |
|---|---|
| **ST0 foundation** | `FileStore` port and adapters; `stored_file` table; retention classes mapped to the retention rules; presigned-link endpoint with permission and audit; orphan and retention jobs; the S3 settings in `application.yml` and CONFIGURATION.md; MinIO integration test |
| **ST1 module move** | Every module writes through the port. For each `bytea` table: add `stored_file_id`, copy existing bytes to S3 with a one-off job (Flyway Java migration or a system job) that verifies the SHA-256, switch reads, then drop the `bytea` column in a later migration once verified. E-mail attachments and ZIP bundles are read from S3 and streamed |
| **Tests** | Upload, download link, permission refused, expiry, checksum mismatch, quarantine, retention delete, legal hold, orphan clean-up |

**Deployment (infrastructure, BDOI IT):**
- the buckets and KMS keys per environment, the IRSA role, the VPC endpoint and the replication rule;
- the malware scanning service;
- Object Lock, which must be enabled when a bucket is created.

## 5. BDOI decisions (26-Sep-2026) and what they mean for the design

| # | Decision | Design consequence |
|---|---|---|
| 1 | **Option C approved**: S3 for file content, PostgreSQL metadata, short-lived presigned links | Build ST0 and ST1 as in section 4 |
| 2 | **Malware scanning: Amazon GuardDuty Malware Protection for S3** | GuardDuty is enabled on the `inbound` bucket and on the upload prefix of `documents`. BIBS reads the scan result tag (`GuardDutyMalwareScanStatus`); only `NO_THREATS_FOUND` objects are accepted. Other results are moved to a quarantine prefix, and an alert goes to the uploader and to the security role. A file whose scan has not completed cannot be downloaded |
| 3 | **Object Lock and legal hold follow the BDOI Delegation of Authority (DOA).** Philippine data is hosted in Singapore (ap-southeast-1), so governance and data protection controls apply | Object Lock in **governance mode**. Placing or releasing a legal hold, and any governance bypass, is a BIBS action limited to the roles the DOA names; each action is recorded with approver and reason. Record classes under hold are listed in a parameterised table (for example STRs, filed BIR forms, official receipts, claim settlements), with retention from the retention rules. Cross-border controls: data stays in the approved AWS regions only; a bucket policy denies any other region; CloudTrail data events and S3 server access logs are kept for audit; encryption with BDOI-owned keys (decision 4); no personal data in object keys. The DR region must also be approved under the same cross-border basis (question DSQ01) |
| 4 | **BDOI owns the encryption keys and the rotation policy** | Customer-managed KMS keys created and administered in BDOI's AWS account, one per environment and bucket class. Key administrators are BDOI security staff; the BIBS service role only has encrypt / decrypt / generate-data-key through a key policy. Automatic rotation follows the BDOI policy (AWS default annual rotation unless BDOI sets otherwise). If BDOI requires its own key material, KMS imported key material or the external key store can be used without any change to BIBS, because BIBS only names the key ARN in configuration. The DR region uses a multi-Region key or its own BDOI key |
| 5 | **ECM: yes, following best practice** | S3 stays the operational store that BIBS reads and writes. ECM becomes the **records-management archive for final, issued records**. When a record class marked "archive to ECM" reaches its final state (for example a signed or issued policy document, official receipt, BIR form, statement of account, claim settlement letter or filed STR), BIBS publishes it to ECM with its metadata through the integration outbox, without waiting in the user's transaction. The ECM reference is stored on the `stored_file` row. Retrieval inside BIBS stays on S3; ECM serves enterprise search, records retention and legal discovery. Drafts, working files, reports and bulk files are not sent to ECM. The ECM interface (API, metadata schema, record classes) is owned by BDOI IT (question DSQ02) |

### Open points for BDOI IT

| Ref | Question |
|---|---|
| DSQ01 | Which DR region is approved for the cross-border basis (the IER workbook plans a cross-region replica)? |
| DSQ02 | ECM interface specification: API, authentication, metadata schema, record classes, acknowledgement and error handling |
| DSQ03 | The DOA roles allowed to place and release legal holds and to authorise a governance bypass |
| DSQ04 | Key policy details: key administrators, rotation period, and whether imported key material is required |

## 6. As built: ST0 foundation (26-Sep-2026)

ST0 is in the code. The module tables moved in ST1 (section 7).

**Components.**

| Part | Where | What it does |
|---|---|---|
| Port | `common.storage.FileStore` | `put`, `get`, `presignedGet`, `presignedPut`, `delete`, `copy`, `exists`, `metadata`, plus `list` (orphan reconciliation) and `legalHold` (Object Lock) |
| S3 adapter | `common.storage.S3FileStore` | AWS SDK v2 on the JDK HTTP client. Every put and copy uses SSE-KMS with the key ARN of its bucket class (`brokerverse.storage.kms-keys.*`, bucket keys on) and carries its SHA-256 checksum, which S3 verifies. Download links force `Content-Disposition: attachment` and `Cache-Control: no-store`; upload links sign the encryption and checksum headers. Credentials come from the default chain (IRSA on EKS) |
| Local adapter | `common.storage.LocalFileStore` | File system, for developer machines, automated tests and seed stacks. Marks every object clean (`NO_THREATS_FOUND`). Its links are application URLs (`/api/v1/files/local-content`) with an HMAC-signed token (object, method, expiry). Refused when `brokerverse.environment=production` |
| Selection | `common.storage.StorageConfiguration` | `brokerverse.storage.provider` = `s3` or `local`. With `s3` the start is refused without the bucket and KMS key of the documents, reports and inbound classes |
| Object keys | `common.storage.ObjectKeys` | `<company>/<entity-type>/<yyyy>/<mm>/<uuid>`; `incoming/` (presigned uploads) and `quarantine/` prefixes. The file name is never part of a key |
| Metadata | `storage` module, table `stored_file` (V1100) | Section 3 columns plus scan status and result, legal hold (reason, placed by, approved by, time), final time, ECM status and reference, soft delete and purge time |
| Record classes | `sto_record_class` (V1100, V1101) | Parameterised: bucket class, retention record type (mapped to `nba_retention_rule`: the longest active rule, years online plus years in archive), fallback period, "under legal hold", "archive to ECM", active. Changed by the DOA approvers only (`PUT /api/v1/files/record-classes/{code}`, audited) |
| Storing | `storage.service.StoredFileService` | Size (25 MB through the application), type by extension confirmed by the file signature, SHA-256 (compared with a checksum the sender declares), object first, then the row, scan result, audit entry. `read` re-checks the SHA-256 for streaming flows |
| Owner permission SPI | `storage.service.FileOwnerAccess` | The owning module decides `mayRead` / `mayStore` for its owner types. Owner types without a resolver are refused |
| Download link | `GET /api/v1/files/{id}/link` | Owner permission, then scan state (only clean files), then a link valid for `FILE_LINK_TTL_SECONDS` (default 300). Each issue and each refusal is audited with user, file, time and client address |
| Inbound upload | `POST /api/v1/files/inbound-uploads`, `POST /api/v1/files/{id}/upload-complete` | Presigned PUT into `incoming/` of the inbound bucket for large bulk files. The SHA-256 is declared up front and signed into the link |
| Malware scan | `storage.service.FileScanService` | Reads the tag `GuardDutyMalwareScanStatus`. `NO_THREATS_FOUND` makes the file downloadable. Any other result moves the object to `quarantine/`, marks the row `QUARANTINED` and calls `FileQuarantineListener`: an in-app notification (event `FILE_QUARANTINED`) to the uploader and to the holders of `FILE_QUARANTINE_VIEW`, plus the alert `FILE_QUARANTINED`. A file without a result stays `PENDING` and cannot be downloaded |
| Legal hold | `storage.service.LegalHoldService` | Controlled action: `FILE_LEGAL_HOLD_REQUEST` requests PLACE or RELEASE with a reason; another user with `FILE_LEGAL_HOLD_APPROVE` approves or rejects it (approval inbox item). The approval sets or clears the Object Lock legal hold and records requester, approver and reason. Files of a class "under legal hold" get their hold when their scan is clean. Held files cannot be deleted and are never removed by the retention job |
| Jobs | `FILE_SCAN_RESULTS` (every 5 minutes), `FILE_ORPHAN_RECONCILIATION` (daily), `FILE_RETENTION` (daily), `FILE_ECM_ARCHIVE` (every 15 minutes) | Read pending scan results; delete objects older than 24 hours that have no row (held objects are kept); remove the objects of files past retention or soft-deleted for longer than 30 days, and close announced uploads never confirmed (held files are counted, not removed); publish final records of the "archive to ECM" classes |
| ECM publisher | topic `bibs.storage.ecm-archive-requested.v1` | Through the integration outbox, outside the user's transaction. The payload carries identifiers and object facts only; the file name stays in the row. The ECM adapter that consumes it waits for DSQ02 and then calls `StoredFileService.recordEcmReference` |

**Roles and permissions (V1101).** `RECORDS_HOLD_OFFICER` (`FILE_LEGAL_HOLD_REQUEST`), `RECORDS_HOLD_APPROVER`
(`FILE_LEGAL_HOLD_APPROVE`) and `INFOSEC_OFFICER` (`FILE_QUARANTINE_VIEW`, `AUDIT_VIEW`). The auditor also holds
`FILE_QUARANTINE_VIEW`. When BDOI answers DSQ03, administrators grant the legal hold permissions to the DOA roles it
names. Seed users: `holdofficer`, `holdapprover` and `infosec` (V1109, seed data only).

**Tests.**
- `LocalFileStoreTest` covers the local adapter and its links: expiry, tampering, wrong method and checksum.
- `S3FileStoreTest` runs the S3 adapter through the real AWS SDK against an in-process S3 REST service. No container
  runtime is available on the build hosts, so the MinIO container test is replaced by this service. Object Lock,
  bucket policies and GuardDuty itself are verified in the DEV account.
- `StoredFileIT` covers upload, link and download, permission refused, the checksum, type and integrity checks,
  inbound upload with a pending and a quarantined scan, and the record classes with the retention mapping.
- `StorageJobsIT` covers legal hold, class holds, retention delete, deleted and abandoned files, orphan clean-up and
  the ECM publisher.

**Still to do.**
- The ECM adapter (DSQ02).
- The DOA role names (DSQ03).
- The buckets, keys, IRSA role, VPC endpoint, replication, Object Lock and GuardDuty per environment (BDOI IT,
  section 4).

## 7. As built: ST1 module move (26-Sep-2026)

Every module that kept file bytes in PostgreSQL writes new files through `StoredFileService`. The existing bytes are
copied by a one-off job; the `bytea` columns stay until the copy is signed off in each environment, then a later
migration drops them.

**The 13 tables.** The migrations were checked: these are all the `bytea` columns of BIBS. Screening case documents,
STR supporting documents, watchlist files, KYC documents, payment-request documents, EB and claim documents are
attachments (`doc_attachment_content`); DP lists and billings, remittance extracts and production registers are in
the extract repository (`ops_extract_file`); STR files as filed are report runs (`report_run_file`). Journal uploads
are read and posted, not kept. `doc_rendition` (V756) keeps only the specification and the SHA-256.

| Table (migration) | Module | Owner type (key) | Record class | Origin | Who may open it (`FileOwnerAccess`) | `stored_file_id` on |
|---|---|---|---|---|---|---|
| `doc_attachment_content` (V21) | attachment | `Attachment` (attachment id) | by document type: `POLICY_DOCUMENT` (e-policy, policy copy, Insurance Advice, EB policy form), `OFFICIAL_RECEIPT`, `CLAIM_SETTLEMENT` (settlement and release papers), else `GENERAL_DOCUMENT` | uploaded | `ATTACHMENT_VIEW` and the document access classes (V1031); adding `ATTACHMENT_MANAGE` | `doc_attachment` |
| `report_run_file` (V762) | report | `ReportRun` (run id) | `REPORT_OUTPUT`; STR files `STR` | generated | the export permission of the report and its availability time | `report_run` |
| `report_batch` (V32) | report | `ReportBatch` (batch id) | `REPORT_OUTPUT` | generated | `REPORT_VIEW`, creator only | `report_batch` |
| `msg_outbound_attachment` (V753) | messaging | `OutboundMessage` (message id) | `GENERAL_DOCUMENT` | generated | `MESSAGE_VIEW` | same table |
| `ops_extract_file` (V761) | opsledger | `ExtractFile` (file id) | `WORKING_FILE` | generated | `OPS_VIEW`; DP billings also `COMMREC_*`, production registers `RECON_PROCESS` / `RECON_SEND` | same table |
| `csh_print_batch` (V763) | cashiering | `PrintBatch` (batch id) | `OFFICIAL_RECEIPT` | generated | `CASH_PRINT` | same table |
| `rem_batch_document` (V770) | remittance | `RemittanceBatchDocument` (document id) | `GENERAL_DOCUMENT` | generated | `REMIT_PROCESS`, `REMIT_EXTRACT`, `REMIT_APPROVE`, `REMIT_OR_UPLOAD` | same table |
| `plc_slip_file` (V850) | placement | `PlacementSlip` (slip id) | `POLICY_DOCUMENT` | generated | `ACCOUNT_VIEW`, `PLACEMENT_MANAGE`, `BILLING_MANAGE` | same table |
| `iss_upload_item` (V860) | issuance | `EpolicyUpload` (upload id) | `INBOUND_FILE` | uploaded | `EPOLICY_MANAGE` (also adds them) | same table |
| `iss_insurance_advice` (V860) | issuance | `InsuranceAdvice` (advice id) | `POLICY_DOCUMENT` | generated | `ACCOUNT_VIEW`, `EPOLICY_MANAGE`, `EPOLICY_SEND` | same table |
| `bkg_service_invoice` (V870, column `document`) | booking | `ServiceInvoice` (SI number) | `STATEMENT_OF_ACCOUNT` | generated | `BOOKING_PROCESS`, `BOOKING_ADJUST` | same table |
| `dsb_eod_output` (V892) | disbursement | `EodOutput` (output id) | `WORKING_FILE` | generated | `DISB_EOD`, `DISB_VIEW` | same table |
| `clx_billing_document` (V1002) | collections | `BillingStatement` (statement id) | `STATEMENT_OF_ACCOUNT` | generated | `CLX_VIEW` | same table |

Only the attachment and e-policy upload owners accept files through `POST /api/v1/files`; the other resolvers answer
`mayStore = false` (their files are produced by the application). Owner types opened by a list of permissions use
`storage.service.PermissionFileAccess`; attachments, extract files and reports have resolvers of their own.

**Migrations.** V1111 (platform: attachments, report runs and batches, e-mail attachments), V1112 (Operations:
extract repository, print batches, remittance documents), V1113 (broking: slips, e-policy uploads, Insurance Advices,
service invoices) and V1114 (disbursement outputs, statements of account) add the nullable `stored_file_id` and make
the `bytea` columns nullable. V1115 to V1119 stay free for step 3.

**Writing.** `StoredFileService.storeChecked(request, contentType, origin)` is used by the modules: the module has
produced the content or validated it with its own rules (the attachment module keeps its type, signature, size and
malware checks), so the upload allow-list and the 25 MB limit of `store` do not apply; the SHA-256, the SSE-KMS
write, the metadata row and the audit entry do. The origin decides the malware scan:
- `UPLOADED` (attachments, e-policy uploads): the file waits for its scan result like any upload and can be opened
  only when clean;
- `GENERATED` (everything the application renders or sends, including password-protected e-mail attachments that no
  scanner can read): recorded clean with the scan result `APPLICATION_GENERATED`. GuardDuty covers the inbound bucket
  and uploads; generated files in the documents and reports buckets would otherwise never become downloadable.

**Reading.** A row with `stored_file_id` is read from the file store; a row still holding bytes (not yet copied) is
read from its column as before, so nothing breaks between the deployment and the copy.
- Download endpoints answer through `storage.api.FileDownloads`. With `brokerverse.storage.downloads.mode=redirect`
  a stored file is answered with `302 Found` to its presigned link (owner permission, scan state, audit entry by
  `FileLinkService`; attachment disposition, no-store). With `stream` (the default, until the browser settings below
  are deployed) the same owner check applies and the file is read by `StoredFileService.read` (SHA-256 re-checked,
  audited). Bytes still in the database are sent as before.
- Streamed flows read with `StoredFileService.read`: e-mail attachments at dispatch (`MailDispatcher`), ZIP bundles of
  attachments, attachments read by other modules (watchlist feeds, e-policy extraction and dispatch), slips,
  Insurance Advices, service invoices and statements of account attached to e-mails, production registers sent to
  insurers, report files copied into the end-of-day outputs.

| Endpoint | Answer for a stored file |
|---|---|
| `GET /api/v1/attachments/{id}/content` | link or stream (ZIP `GET /api/v1/attachments/zip` is always built and streamed) |
| `GET /api/v1/reports/runs/{id}/file`, `GET /api/v1/reports/batches/{id}/file` | link or stream |
| `GET /api/v1/screening/str/extractions/{id}/file` | link or stream |
| `GET /api/v1/ops/extracts/{id}/file`, `GET /api/v1/commission/dp/billings/{id}/file`, `GET /api/v1/prodrecon/extracts/{id}/file` | link or stream |
| `GET /api/v1/cashiering/print-batches/{id}/file` | link or stream |
| `GET /api/v1/remittance/batches/{id}/documents/{kind}` | link or stream after submission; before it the document is rendered and streamed as before |
| `GET /api/v1/placement/slips/{id}/files/{format}` | link or stream |
| `GET /api/v1/issuance/insurance-advice/{id}/file` | link or stream |
| `GET /api/v1/booking/service-invoices/{id}/pdf` | link or stream |
| `GET /api/v1/disbursement/eod/outputs/{outputId}` | link or stream |
| `GET /api/v1/collections/billing/statements/{id}/document` | link or stream |

The response body of these endpoints does not change: the file itself, with its `Content-Type` and
`Content-Disposition`. Documents rendered on request (receipts, certificates of payment, vouchers, instruments,
quotation and proposal slips, BIR forms, billing workbooks) are not stored and stay as they are.

**Web client (frontend team).** The client downloads with `fetch` and the bearer token, then saves the blob; `fetch`
follows the redirect. To switch an S3 environment to `redirect`:
- `frontend/nginx.conf`: add the bucket hosts of the environment (for example
  `https://bibs-<env>-documents.s3.ap-southeast-1.amazonaws.com` and the reports and inbound buckets) to
  `connect-src`; today `connect-src 'self'` would block the redirected request;
- the file name is read from `Content-Disposition`, which S3 returns only if the bucket CORS rule exposes it;
- optionally, the client may call `GET /api/v1/files/{id}/link` itself and navigate to the URL, which needs no CORS
  (the module responses do not carry the stored file id today).
With the local store (developer machines and seed stacks) the link is an application URL and `redirect` works as is.

**Copying the existing bytes: `FILE_BYTEA_MIGRATION`** (`storage.service.FileContentMigrationJob`, workload `BATCH`,
manual by default: `brokerverse.storage.content-migration.cron`, batch size `...batch-size`, default 20).
- Each module declares its table as a `LegacyFileTable` bean (select of the rows still to copy, update of
  `stored_file_id`, counts); the storage module does not depend on the modules. The STR files are declared by
  screening (`@Order(0)`) so that they are copied under the class `STR` before the general copy of `report_run_file`.
- Per row, in its own transaction: the content read from the table is compared with the SHA-256 the module recorded
  (attachments, extract files, e-mail attachments, slips, uploads, Insurance Advices); the file is stored; the object
  is read back and its SHA-256 compared with the content; then `stored_file_id` is set, only if still empty. A failing
  row is logged and counted and stays as it is; its object, if written, is an orphan for
  `FILE_ORPHAN_RECONCILIATION`.
- Restartable and idempotent: copied rows are never read again; a second run copies nothing. The run message lists,
  per table, the rows copied, failed and left.
- Reconciliation report: `GET /api/v1/files/content-migration` (`SYSTEM_MONITOR`, `SYSTEM_PARAMETER_MANAGE` or
  `AUDIT_VIEW`) gives per table the rows with content, the rows copied and the rows left
  (`report_run_file (STR files)` is a subset of `report_run_file`).
- Retention of the copies starts at the copy date (the metadata row's creation), not at the original date.

**Step 3, not applied.** `docs/architecture/st1/ST1_DROP_BYTEA_COLUMNS.sql` drops the `bytea` columns (and the tables
`doc_attachment_content` and `report_run_file`) and refuses to run while any row still holds content without a stored
file. It is copied into `db/migration` with the next free version of V1115 to V1119 once the report of every
environment is signed off, together with the removal of the old read paths in the entities and services above.

**Tests.** `ModuleFileAccessIT` (every owner type: link issued with the module's permission, refused without it;
attachments with their class, content read back and no database copy; extract files by producing module; report runs
by export permission, STR class, batches by creator), `FileContentMigrationIT` (legacy rows of four tables served from
the database, copied with the checksum checked, a row with a wrong recorded checksum left and counted, second run
copies nothing, report counts and permission), `FileDownloadsTest` (redirect, stream with the owner check, bytes still
in the database), `ContentMigrationSettingsTest` (job settings, owner keys, attachment classes), and the module tests, which now check that their files are stored and download through the link.

**For BDOI IT.**
- Run `FILE_BYTEA_MIGRATION` in each environment after the deployment, until the report shows nothing left; sign off
  the report; then schedule step 3.
- Bucket CORS on the documents, reports and inbound buckets for the web origin (method `GET`, exposed headers
  `Content-Disposition`, `Content-Type`) before `BROKERVERSE_STORAGE_DOWNLOAD_MODE=redirect` is set.
- GuardDuty stays on the inbound bucket and the uploads; generated files are not scanned (see "Writing").
