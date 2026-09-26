# Document and file storage: decision record

Status: **Recommended, for BDOI confirmation.** Date: 26-Sep-2026. BDOI instruction: "documents and attachments live in an S3
bucket only".

## 1. What is stored today

File bytes sit in PostgreSQL `bytea` columns. There are at least 13 tables, among them:
- attachments (`att_*`, V21);
- messaging e-mail files (V753);
- ledger and Operations documents (V761, V762);
- cashiering, remittance, placement, issuance and booking documents (V763, V770, V850, V860, V870);
- disbursement vouchers (V892);
- collections (V1002);
- report runs and batches (V32, `ReportRunFile`);
- Word renditions (`doc_rendition`);
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

## 5. Decisions needed from BDOI

1. Confirm option C, including short-lived presigned links; the alternative is option B, streaming every file through BIBS.
2. Malware scanning service to use (GuardDuty Malware Protection for S3 or BDO's standard scanner).
3. Object Lock mode (governance recommended; compliance mode cannot be shortened by anyone) and which records need legal hold.
4. KMS key ownership (BDO security team) and the key rotation policy.
5. Whether ECM must also receive copies of any record class.
