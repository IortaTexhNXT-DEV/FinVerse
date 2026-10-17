---
# Data Architecture of BIBS (BRD-00). Build: build_secdata_pack.py --only data (expands the <!-- sd:... --> lines from
# domains.yaml and the catalogue snapshot data/catalog.json).
title: Data Architecture
subtitle: Data model, master data, lineage, audit, retention, reconciliation, data quality and model governance of BIBS
doc_type: Data Architecture
doc_code: Data
brd: BRD-00
name: Data Architecture
doc_id: BIBS-DAT-BRD-00
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Data Architecture
output: Data/BIBS_Architecture_BRD-00_Data_Architecture_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Data Architect
    reviewer: iorta TechNXT Solution Architect; Migration Lead
    approver: BDOI IT (pending)
    change: First issue, from the production schema of 08 October 2026, the module documentation, the BRDs and FRS, the Data Migration Handbook, the IER workbook v20 and the client decisions of 26 September and 8 October 2026
distribution:
  - {name: "BDOI Information Technology Group (BDOI IT)", role: Approver, organisation: BDOI, purpose: "Data architecture, interfaces, retention implementation"}
  - {name: "Head - Comptrollership; Product Owners - Comptrollership", role: Approver, organisation: BDOI, purpose: "Financial lineage, reconciliation controls, BIR retention"}
  - {name: "Compliance Officer; BDOI Data Protection Officer", role: Approver, organisation: BDOI, purpose: "AMLA and privacy retention, classification, model governance of screening"}
  - {name: "Product Owner, Marketing Business System", role: Reviewer, organisation: BDOI, purpose: "Master data of clients, products, packages, insurers"}
  - {name: "Data Migration Lead and data owners", role: Reviewer, organisation: BDOI, purpose: "Golden records and data quality rules"}
  - {name: "Enterprise Data Platform team (EDP); EGL team", role: Reviewer, organisation: BDO Unibank IT, purpose: "Extracts and lineage to BDO platforms"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Data model, migration, reports, interfaces"}
---

# Introduction

## Purpose and audience

This document describes the data architecture of BIBS (BDOI Broker System, on iNXT BrokerVerse) for BDO Insurance and Reinsurance Brokers, Inc. (BDOI): the conceptual and logical model per domain, who owns which data and how the golden record of each master is kept, how data flows from its sources through BIBS to the general ledger, the reports and the regulators, what is audited and for how long it is kept, the reconciliation and data quality controls, the metadata and data catalogue, and the governance of automated decisions.

The audience is BDOI IT, Comptrollership, Compliance, the Data Protection Officer, the data owners and the Data Migration team. The physical model (tables, columns, keys) is in the companion documents BIBS Entity Relationship Diagrams v1.0 and BIBS Data Dictionary v1.0.

## Scope

<!-- table: widths=3,10 caption="Scope" bold=first -->
| Item | Scope |
|---|---|
| In scope | All data held by BIBS for the BRDs of Drop 0 (BRD-03, 11, 13), Drop 1 (BRD-01, 02, 04, 05, 06, 09, 10, 12) and Drop 2 (BRD-07, 08); its documents in Amazon S3; its interfaces to BDO systems and regulators |
| Out of scope | Reinsurance broking (phase 2); data held only in BDO systems (EIAM, EGL, EDP, CMS); the insurer-company modules being removed from BIBS (insurer underwriting, insurer claims, reinsurance treaty accounting, actuarial reserves, group consolidation), whose tables are not part of the model |
| Date | Production schema and configuration of 08 October 2026; go-live January 2028 |

## Sources

<!-- table: widths=1,6,6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | BRDs of 8 October 2026 (umbrella BRD and BRD-01 to BRD-13) and their FRS | Business entities, retention and audit requirements |
| S2 | BDOI Drop Plan; Programme Alignment v1.0; Integration Inventory v1.0 | Drops, interfaces (EGL, EDP, CMS, ECM and others) |
| S3 | BRD-13 Data Migration Handbook and Migration Workbook | Migration objects, reconciliation levels L1 to L5, quality gates |
| S4 | IER workbook v20 | PostgreSQL 16 on Amazon RDS (2 TB, Multi-AZ), RPO 15 minutes, RTO 4 hours |
| S5 | Document storage decision (BDOI answers of 26 September 2026) | S3, record classes, legal hold, ECM archive |
| S6 | Client decisions of 8 October 2026 | PostgreSQL 16, Valkey 8, Apache Kafka 3.9 |
| S7 | Production schema of 8 October 2026 (Entity Relationship Diagrams v1.0, Data Dictionary v1.0) | Physical model, audit tables, retention configuration |

## Summary

- **One operational database.** BIBS keeps all business data in one PostgreSQL 16 database, organised in 17 data domains (561 tables). A business record, its journal, ledger entries, open items and audit rows commit in one transaction, so the operational data and the books never disagree.
- **Owned master data.** Every master (client, insurer, product and package, organisation, users) has one BDOI owner, one system of record and maker-checker control. Client duplicates are blocked on hard keys (TIN, identity document, name and birth date).
- **Traceable numbers.** Every journal comes from a configured accounting event of a business record; every report and extract reads the ledger or the operational records; the general ledger reaches EGL as journals or trial balance by account, branch, cost centre and currency.
- **Evidence by design.** The audit trail and 13 other history tables are insert-only at database level. Retention runs per record type and record class with review, archive, legal hold and disposal steps; the values proposed here (BIR 10 years, AMLA 5 years after closure) are open points for BDOI.
- **Controlled automated decisions.** Name matching and risk profiling of sanction screening, payment matching and collection rules are versioned, approved under maker-checker and monitored; any generative tool BDOI introduces later follows the governance of chapter 12.

# Principles and overview

<!-- table: widths=3.4,9 caption="Data architecture principles" bold=first -->
| Principle | What it means in BIBS |
|---|---|
| One system of record per data object | Each entity has one owning module and one BDOI owner; other modules read it through the owner's services, never copy it |
| Transactional integrity | The business record and its accounting, open items and audit commit in one database transaction; corrections are reversals, never updates of posted data |
| Accounting by configuration | Journals come from accounting events and rules maintained by Comptrollership; GL accounts are never chosen in program code |
| Maker-checker on master data | Every master record has a record status and an authoriser who is not its maker |
| Evidence is immutable | Audit trail, ledger entries and history tables refuse UPDATE, DELETE and TRUNCATE at database level |
| Data scope by company and branch | Every business record carries its company (and branch where relevant); access follows the user's data scope |
| Documents outside the database | File content in Amazon S3 (SSE-KMS, Object Lock); the database keeps metadata, checksum and record class |
| Privacy by design | Minimum data per function, masking outside production, retention with disposal (Security Architecture, chapter 12) |

<!-- table: widths=3,4.2,5 caption="Data stores" bold=first -->
| Store | Holds | Notes |
|---|---|---|
| PostgreSQL 16 (Amazon RDS Multi-AZ, 2 TB in the IER) | All business, accounting, audit and configuration data; file metadata | System of record; point-in-time recovery; cross-region replica for DR (RPO 15 minutes, RTO 4 hours) |
| Amazon S3 (documents, reports, inbound, migration buckets) | Attachments, generated documents, report outputs, inbound files, migration extracts | Record class and retention per object; versioning; replication to the DR region |
| Valkey 8 | Caches (reference data, data scope), session and token denylist entries, job locks, counters | Not a system of record; refilled from PostgreSQL |
| Apache Kafka 3.9 (Amazon MSK) | Integration events published from the transactional outbox | Not a system of record; events can be replayed from the outbox archive |
| ECM (BDO) | Final records of the "archive to ECM" classes | Records-management archive; retrieval in BIBS stays on S3 |

**Volumes (sizing basis).** 1,344 named and 429 concurrent users (umbrella BRD; the IER diagram states about 1,200 internal users, open point IQ32 of the Programme Alignment); about 21,200 new-business bookings and 25,800 renewal accounts a month; 15 % yearly growth over 60 months (IER).

# Conceptual and logical data model

## Conceptual model

![Conceptual model: main business entities and their relationships](figures/da_conceptual.dot){width=15}

The client is the centre of the broking model. A quotation (package) or a proposal request (non-package) becomes an account with its risk items; the account is placed with one or more insurers and issued as a policy; booking creates the invoice with the premium by component, the insurer shares and the broker commission. Collections, cashiering, remittance to insurers, commission receivables, endorsements, renewals and claims all work on the invoice and the account. Every money movement produces accounting events that the rules turn into journals of the immutable ledger and open items of the sub-ledger.

## Data domains

<!-- sd:domain_summary -->

## Logical model per domain

<!-- table: widths=2.4,5.4,4.6 caption="Main logical entities and business keys per domain" bold=first size=8.5 -->
| Domain | Main entities (logical) | Business keys and rules |
|---|---|---|
| D01 Security and User Access | User, Role (group profile), Permission, Data scope grant, Sign-in session, Access request with approvers and events, SoD rule, Access change log, Retention rule | User name (unique), Windows ID (unique); a request is the only way to change access |
| D02 Platform Services | Parameter, Scheduled job and run, Audit entry, List of values, Workflow case and history, Notification, Alert, Bulk job, Attachment, Stored file, Document template, Report run, Outbox event, Document sequence | Document numbers gapless per sequence; stored files identified by a random key and SHA-256 |
| D03 Organisation and Reference Data | Company, Branch (Office Master), Employee, Holiday calendar, Currency, Exchange rate, Cost centre and dimensions, Fiscal year, Period | Company and branch codes; one open period sequence per company |
| D04 Client and Party | Client (individual or corporate) with KYC profile, KYC document, Contact, Tag, Special instruction, Business partner | Prospect PR-yyyy-nnnnnn, client CL-yyyy-nnnnnn; hard duplicate keys block creation |
| D05 Product Catalogue and Maintenance | Insurer and insurer branch, Product line, Product, Package and package version, Coverage, Rate, Commission and incentive set-up, Package request | Package version effective dates; one active version per date |
| D06 New Business | Quotation, Proposal request (PRF), Account (ARN) with risk items, Placement slip, Hold cover, Payment gate, Policy issuance, Booking batch, Invoice with components, shares and journals | ARN per account; invoice number BIR-sequential per company; root invoice number links the invoice family |
| D07 Operations | Invoice ledger and movements, Receipt series, AR and OR, Payment intake, Application, Unapplied payment, Remittance batch, Production reconciliation, Endorsement request, Commission receivable, Incentive scheme | OR and AR numbers from receipt series; an invoice carries remittance, hold and payment status |
| D08 Collections | Collection item and account, Disposition, Promise to pay, Instalment, Escalation, Billing statement | One collection item per ledger invoice ever listed |
| D09 General Ledger and Finance | GL account, Journal batch and lines, Ledger entry, Daily balance, Open item, Accounting event and rule, Period close, Budget, Statement format and schedule, Report pack, Bank reconciliation, Fixed asset, Investment, Tax return, 2307 certificate | Balanced journals; ledger entries immutable; corrections by reversal |
| D10 Disbursement and ACSL | Payee, Disbursement request and voucher (DV) with payment instruments, Payables masters, Refund and cash-advance requests, Insurer SOA upload, GL-SL reconciliation, Investigation case, Correction entry | DV numbers; editable proforma only before approval |
| D11 Renewal | Renewal candidate, Extraction run, Check, Disposition, Letter, Insurer quotation, Acceptance, Transfer | One candidate per expiring account and term |
| D12 Claims Broking | Claim, Insurer line, Insurer update, Reserve change as advised, Status history, Diary entry | Claim number; no claim journal in BIBS |
| D13 Employee Benefits | Programme, Proposal, Benefit line and plan, Census and member movement, Billing cycle, Statement of account, Tracked item | Programme number; member data limited to the roster fields |
| D14 Customer Servicing Facility | Service request, Contact change with verification, Activity, Referral | Request number; contact changes verified before synchronisation |
| D15 Sanction Screening | Watchlist and entry, Watchlist change, Screening run and match, Case with reviews and votes, Client risk profile, Configuration version and rules, STR | Configuration versions approved before use; decisions insert-only |
| D16 Submitted Policies | Masterlist policy, Intake run, Review, IAAF and TOR records, Letter, Fee, Renewal hand-off | Policy number per insurer |
| D17 Data Migration | Migration object, Batch, Extract, Staging row, Code map, Quality rule and result, Reconciliation, Gate sign-off, Legacy archive record and access log, Decommission item | Legacy key cross-reference per loaded row |

# Master data management

## Ownership

<!-- sd:domain_owners -->

## Master data and golden-record rules

<!-- table: widths=2.2,2.6,3.6,4,2.6 caption="Master data management" bold=first size=8.5 -->
| Master | System of record and owner | Golden-record rules | Change control | Consumers |
|---|---|---|---|---|
| Client (party) | BIBS client master (D04); Product Owner, Marketing Business System | One client per person or entity. Hard keys block a duplicate: TIN; identity document type and number; last name, first name and birth date. E-mail, mobile and corporate name warn. TIN format 000-000-000-000; minimum fields per client type; KYC review date from the risk rating. The confirmed client opens its business partner in the same transaction | Prospect by the Account Officer; KYC verification and confirmation under the NB_CLIENT workflow; changes audited with from / to values; deactivation with a reason; merges of legacy duplicates during migration only, approved by the data owner | All broking modules, screening, collections, EDP |
| Insurer | BIBS catalogue (D05); Head - Operations | One insurer per insurer code; insurer branches and bank accounts under it; accreditation and status dates | Maker-checker; deactivation only without open placements | Placement, remittance, production reconciliation, ACSL, disbursement |
| Product and package | BIBS catalogue and product maintenance (D05); Product Owner, Marketing Business System with the TSU | One product per product line and code; packages in versions with effective dates, one active version per date; rates, commission and incentive criteria per version | Package request, negotiation and approval workflow; maker-checker on the version | Quotation, account, booking, renewal, commission |
| Organisation (company, branch, cost centre) | BIBS organisation (D03); Head - Comptrollership | One code per company and branch (Office Master); cost centres and dimensions linked to GL posting controls | Maker-checker; a branch with open records cannot be closed | Every module (data scope), GL, EGL mapping |
| Users and roles | BIBS security (D01), fed by UIDM-ISC for accounts (proposal); Business Administration and the System Administrator | One user per person (user name and Windows ID unique); roles are group profiles approved under BRD-11 | Access requests under four eyes; second approval for privileged or out-of-hours changes; access change log | Every module; audit reports |
| Chart of accounts and accounting rules | BIBS finance (D09); Head - Comptrollership | One GL account per code with category, posting controls and branch / currency rules; one active rule per event and condition | Maker-checker; rule simulation before authorisation | Posting engine, reports, EGL |
| Lists of values and parameters | BIBS platform (D02); owner per list as named in the configuration inputs | One code per list; labels shown, codes stored | Maker-checker on values; security parameters need a second approval | All screens and rules |

## Master data quality targets (proposal)

<!-- table: widths=4,2.4,2.6,3.6 caption="Master data quality targets" bold=first -->
| Measure | Target at go-live | Then | Monitored by |
|---|---|---|---|
| Confirmed clients with complete minimum fields | 98 % | 99 % monthly | Client 360 warnings; incomplete-client report |
| Confirmed clients with valid TIN format | 100 % | 100 % | Validation at entry; migration quality rules |
| Clients with KYC review overdue | below 2 % | below 2 % | KYC reviews due job and report (monthly) |
| Duplicate clients (hard keys) | 0 | 0 | Duplicate check; migration cross-reference |
| Active insurers with a bank account and remittance terms | 100 % | 100 % | Catalogue completeness report |
| Package versions with rates and commission set for every product | 100 % | 100 % | Version validation checks |

# Data lineage

![Lineage from the sources through BIBS to the ledger, reports and regulators](figures/da_lineage.dot)

<!-- table: widths=2.6,3,4,3.8 caption="Lineage of the main figures" bold=first size=8.5 -->
| Figure | Source | Inside BIBS | Destination |
|---|---|---|---|
| Gross premium and its components (premium, DST, VAT, LGT, other charges) | Insurer terms on the placement slip; rates of the package version | Booked invoice components; accounting event at booking; journal lines per rule | GL (premium receivable, due to insurer); EGL; IC broker reports; Mancom |
| Broker commission and VAT on commission | Commission rate of the package version or the placement | Invoice commission with VAT and withholding; commission receivable | GL income and output VAT; BIR VAT return (2550Q); EGL |
| Expanded withholding tax | Payee profile and tax code | Disbursement voucher and receipt lines; 2307 certificates issued and received | BIR returns (1601-EQ, QAP, SAWT); 2307 to payees |
| Collections | Bank channels (OBPCS, Old BOB, PMS, direct deposit); cashier entries | AR / OR, application to invoice components, unapplied payments; accounting events | GL cash and receivable; collection worklist; remittance eligibility |
| Remittance to insurers | Paid invoice components | Remittance batch, payment request, DV | CMS / New BOB payment; GL due to insurer; insurer SOA reconciliation (ACSL) |
| Disbursements | Requests from Operations, Marketing, HR | DV with editable proforma, approval, payment instrument | CMS / New BOB; GL; Disbursement reports |
| Financial statements and schedules | Ledger and daily balances | Statement formats, schedules, FRBS report pack | BDOI management; EGL (trial balance); BIR annual return (1702) |
| Suspicious transaction reports | Screening cases and transactions | Case decision by the AML committee; STR record (insert-only) | AMLC; ECM archive |
| Opening balances | Legacy trial balance and open items (BRD-13) | Migration batches; opening journals through the migration clearing account | GL; reconciliation L5 (clearing account at zero) |
| Analytical data | All domains | Outbox events and extracts (daily changes) | EDP (data ingestion, SD 11) |

Every journal line keeps the event, the source record and its company and branch, so any GL balance can be drilled down to the business records behind it (ledger entry, journal batch, accounting event, business record). Report definitions name their source tables and filters in the report catalogue (chapter 8).

# Audit trails

<!-- table: widths=3.4,5.4,2.4,2.6 caption="What is audited" bold=first size=8.5 -->
| Record | Content | Immutability | Retention (proposal) |
|---|---|---|---|
| Audit trail (D02) | Every create, update, authorise, post, reverse, sign-in, sign-out, report run and export: time (UTC), user, entity type and id, action, summary; written in the same transaction as the change | Database triggers refuse UPDATE, DELETE and TRUNCATE | 10 years (5 online) |
| Access change log (D01) | Each changed attribute of a user or role: from, to, request number, done by, approver, source | Insert-only triggers | 15 years |
| Access request events (D01) | Every step of a user access request | Insert-only triggers | 15 years |
| Ledger entries (D09) | Every posted amount with account, branch, currency, event and source | Insert-only triggers; corrections by reversal | 10 years after the year (BIR) |
| Workflow case history (D02) | Every stage change with action, reason, comment, user | TRUNCATE refused | Life of the record |
| Client note history (D04) | Tags and special instructions: who, when, from / to | Insert-only triggers | Life of the client |
| Claim status history, insurer updates, reserve changes as advised (D12) | Claim events | Insert-only triggers | Life of the claim |
| Screening case events, committee votes, risk profiles, decided watchlist changes (D15) | Screening decisions | Insert-only triggers | 5 years after closure (AMLA) |
| Customer servicing activities (D14) | Service actions | Insert-only triggers | Life of the request |
| Invoice origin snapshot (D07) | Invoice values at origin | Insert-only triggers | Life of the invoice |
| Legacy archive access log (D17) | Every inquiry of migrated legacy records | Append-only triggers | 15 years |
| File link audit (D02) | Every presigned link: who, what, when, from where | Audit trail | 5 years |
| Sign-in sessions (D01) | Session start, method, second factor, last activity, end reason | Updated only by the platform | 1 year online |

**Who can change audited data.** The application connects with a least-privilege login that has row access only: it cannot change the schema, truncate a table or disable a trigger. Archiving or correcting an insert-only table is a database administrator task under change control, done as the schema owner and recorded.

# Archival and retention schedule

![Life cycle of a record](figures/da_retention.dot){width=9}

Retention runs in two places, both held as configuration:

- **Retention rules** per record type (online years, archive years, statuses, action REVIEW or ARCHIVE). A monthly job counts the records past their online period; the data owner reviews them; archive or disposal needs the owner's approval.
- **Record classes** of stored files (retention period, legal hold, archive to ECM). A daily job removes the objects of files past retention unless they are under legal hold.

## Proposed schedule

<!-- table: widths=3,3,1.5,1.5,1.6,4.6 caption="Retention schedule (proposal; values to be confirmed by BDOI)" bold=first size=8.5 -->
| Records | Legal or policy basis | Online | Archive | Total | Configured on 8 Oct 2026 and proposal |
|---|---|---|---|---|---|
| Books of accounts, journals, ledger, invoices, ORs, DVs, tax returns and 2307 certificates | BIR: 10 years (NIRC Section 235; RR 17-2013 as amended by RR 5-2014) | 5 years | 5 years | 10 years after the taxable year | Record classes Official receipt and Filed BIR form: 20 years with legal hold; propose 10 years after the taxable year, then disposal after BIR clearance (RET-01) |
| Policies, placement slips, endorsements, statements of account | Insurance Commission requirements for brokers (to confirm); BIR for the billing | 5 years | 5 years | 10 years after expiry | Record classes Issued policy document and SOA: 20 years; propose 10 years after the policy expiry (RET-02) |
| Clients and KYC documents | AMLA (RA 9160 as amended): 5 years after the end of the relationship; Data Privacy Act: no longer than necessary | 5 years after inactivity | 10 years | 15 years | Rules CLIENT (5 online, 15 total, ARCHIVE) and prospects (REVIEW); keep; prospects never confirmed disposed after 5 years (RET-03) |
| Screening cases, STRs, watchlist decisions | AMLA: 5 years after the transaction or closure; longer while a case is under investigation | 5 years | 5 years | 10 years | Rules SCREENING_CASE and WATCHLIST_ENTRY (5 + 5); STR record class 10 years with legal hold; keep (RET-03) |
| Claims | BIR for settlements; litigation period | 10 years | 5 years | 15 years | Rule BROKER_CLAIM (10 + 5); Claim settlement letter 15 years with legal hold; keep |
| Renewals, submitted policies, EB programmes, customer service requests | BDO policy | 5 years | 10 years | 15 years | Rules of 5 online and 15 in total; keep (RET-04) |
| Quotations, proposals and accounts not proceeded, voided or cancelled | BDO policy; Data Privacy Act | 5 years | 10 years | 15 years | Rules (5 + 15, ARCHIVE); propose 5 + 5 (RET-04) |
| User access requests and access change log | BSP MORB and Circular 808 (IT risk); BDO policy | 5 years | 10 years | 15 years | Rule ACCESS_REQUEST (5 + 15); keep |
| Audit trail and sign-in audit | BSP Circular 808; BDO policy | 5 years | 5 years | 10 years | No purge today; propose archive after 5 years (RET-04) |
| Legacy archive and its access log (BRD-13) | BIR and AMLA periods of the legacy records | 5 years | 10 years | 15 years after decommissioning | Rules LEGACY_ARCHIVE and LEGACY_ACCESS_LOG; keep |
| Report outputs | Operational | 400 days | - | 400 days | Record class Report output; keep |
| Inbound files (bank, insurer, watchlist, bulk) | Operational; the loaded data is the record | 90 days | - | 90 days | Record class Inbound file; keep |
| Migration extracts and staging | Hosting appendix | 5 days after sign-off | - | 5 days | Record class Migration extract; staging purged within 5 days of sign-off; keep |
| Working files and drafts | BDO policy | 5 years | - | 5 years | Record class Working file; keep |

**Legal hold** blocks archive and disposal for records under BIR audit, AMLA investigation or litigation; placing and releasing a hold follows the BDOI Delegation of Authority with approver and reason recorded. **Disposal** is a purge or an anonymisation of personal data that keeps the financial figures, approved by the data owner, with a record of disposal kept for 10 years.

# Metadata management and data catalogue

<!-- table: widths=3.4,5,4 caption="Metadata of BIBS" bold=first -->
| Metadata | Where it is kept | Use |
|---|---|---|
| Physical schema (tables, columns, types, keys, indexes, triggers) | PostgreSQL catalogue; Data Dictionary and Entity Relationship Diagrams regenerated from it at each release | DBA, extracts, migration targets |
| Business meaning of tables | Entity documentation, published in the Data Dictionary | Report and extract designers |
| Schema versions | Versioned schema migrations applied at start-up with their history table | Change control, environment comparison |
| Lists of values, parameters, screen labels | Configuration tables with maker-checker; configuration input workbooks | Business configuration |
| Accounting event catalogue and rules | Event register and rules (D09) | Lineage from business records to the GL |
| Report catalogue | Report definitions with category, permission, parameters, columns and source | Report Centre; lineage of reports |
| Record classes and retention rules | Configuration tables (D01, D02) | Retention |
| Integration event catalogue | Topic list and event envelope (schema version 1) | EDP and other consumers |
| Data classification | Data Architecture chapter 10, then column tags in the catalogue (proposal) | Access, masking, privacy |

**Data catalogue (proposal).** BIBS publishes its metadata to BDO's enterprise data catalogue (tool to be named by BDOI IT, open point DAT-03): the schema and its comments, the classification tags, the owners of chapter 4, the report catalogue and the event catalogue, refreshed at each release. Until the catalogue is named, the Data Dictionary workbook is the catalogue of record. Table and column comments will be written into the database so that any catalogue tool reads them with the schema.

# Reconciliation controls

<!-- table: widths=3.4,5.2,2.2,2.6 caption="Reconciliation controls" bold=first size=8.5 -->
| Control | What is compared | Frequency | Owner |
|---|---|---|---|
| Balanced journals | Debits equal credits per journal batch and currency; a batch that does not balance cannot post | Every posting | System (Comptrollership monitors) |
| Ledger to daily balances | Daily balances equal the sum of ledger entries per account, branch and currency | Daily job; period close | FRBS |
| GL to sub-ledger (ACSL) | GL control accounts against the open items of the sub-ledger; differences into investigation cases | Monthly and on demand | ACSL |
| Insurer statement of account (ACSL) | Insurer SOA lines against BIBS invoices: outstanding, for remittance, remitted, cancelled, direct billed, not found | Per SOA received | ACSL |
| Production reconciliation (Operations) | Production register per insurer against insurer feedback, with tolerance; unbooked repository | Monthly per insurer | Operations: Financial Transactions and Processing |
| Bank reconciliation | Bank statements against receipts and payments | Monthly per bank account | FRBS |
| Collections to remittance | Paid components of invoices against remittance batches; hold and exclusion lists | Per remittance batch | Operations |
| Payments to bank confirmations | DVs sent to CMS / New BOB against debit confirmations and status | Daily | Disbursement |
| Outbox to consumers | Outbox events published, failed and dead-lettered; replay | Continuous; daily review | BDOI IT |
| File store | Stored file rows against S3 objects (orphans, missing objects, checksum) | Daily job | BDOI IT |
| Data migration L1 to L5 | Counts, amounts, hash totals, field values read back, and the migration clearing account per branch and currency at zero | After each load; gate G5 | Data Migration Lead; Comptrollership for financial objects |
| EGL extract | Trial balance sent against the BIBS trial balance per account, branch, cost centre and currency | Per extract (daily or monthly, to agree) | FRBS; EGL team |

# Data classification and privacy

<!-- table: widths=2.4,4.4,5.2 caption="Data classes (proposal)" bold=first -->
| Class | Data in BIBS | Controls |
|---|---|---|
| Restricted | Government identity numbers and documents, TIN, birth dates, health-related employee-benefit reports, STRs and screening cases, payee bank accounts, credentials and keys | Need-to-know permissions and data scope; encryption at rest and in transit; download audit; masked outside production; legal hold for STRs |
| Confidential | Client names, addresses and contacts, policies, premiums, commissions, claims, journals and balances | Permissions and data scope; encryption; audit |
| Internal | Products, packages, insurers, branches, lists of values, parameters | Maker-checker for change; read by staff |
| Public | None | - |

Personal data appears in D04, D06, D07, D10, D12, D13, D14, D15, D16 and D17. Non-production environments receive masked data only: names and addresses are replaced, identity numbers and contacts are keyed hashes and birth dates are shifted by up to 30 days; amounts, codes and transaction dates are kept. The privacy controls (Data Privacy Act 2012) are in chapter 12 of the Security Architecture; the classification is open point SEC-09 there.

# Data quality rules and monitoring

<!-- table: widths=2.4,6,4 caption="Data quality rules" bold=first size=8.5 -->
| Dimension | Rules in BIBS | Monitoring |
|---|---|---|
| Validity | Types, lengths, formats (TIN, mobile, e-mail, dates), lists of values on every screen and upload; database check constraints (887 in the schema) | Rejected uploads with reasons; migration quality results |
| Completeness | Minimum fields per client type; mandatory fields per FRS; KYC documents per client type; package version checks before activation | Client 360 warnings; incomplete-client and KYC-due reports |
| Uniqueness | Hard duplicate keys of clients; unique constraints and unique indexes on codes and numbers | Duplicate check log; migration cross-reference |
| Consistency | Foreign keys (843 in the schema); invoice family (root invoice number); balanced journals; one active package version per date | Reconciliation controls (chapter 9) |
| Accuracy | Maker-checker on masters and money; insurer feedback in production reconciliation; bank confirmations | Reconciliation exceptions; ACSL cases |
| Timeliness | SLA of workflows; KYC review dates; renewal extraction windows; ageing of collections | Dashboards and alerts (SLA breaches, ageing) |

**Monitoring (proposal).** A monthly data quality report per domain gives each data owner the measures of section 4.3 and the open exceptions of chapter 9, with a target and a trend; exceptions above target are raised to the data owner and tracked to closure. During migration the quality rules of BRD-13 run on every load, and gate G3 (quality) must pass before a load is signed off.

# Automated decision and model governance

## Automated decisions in BIBS

BIBS takes no decision with a statistical or learning model. It does use rules and scores that decide or prioritise work automatically; each is governed as below.

<!-- table: widths=3,4.6,4.6 caption="Automated decisions and their controls" bold=first size=8.5 -->
| Decision | Logic | Governance |
|---|---|---|
| Sanction screening name match (BRD-10) | Exact, phonetic (Double Metaphone) and similarity (Jaro-Winkler) scores against watchlists with thresholds per list and subject type | Thresholds in a configuration version approved by Compliance before use; every match decided by a person (true match or false positive), insert-only; monthly false-positive and hit-rate review |
| Client risk profiling (BRD-10) | Risk matrices and categories configured by Compliance | Versioned, approved configuration; profiles insert-only with the version used; periodic review dates |
| Payment matching and application (BRD-02) | Matching engine on references and amounts with tolerances | Tolerances as parameters under maker-checker; unmatched items to people; audit of each application |
| Production reconciliation matching (BRD-02) | Matching with tolerance per insurer | Parameters under maker-checker; buckets reviewed by Operations |
| Collection prioritisation and escalation (BRD-04) | Rules on ageing, amounts and dispositions | Rules maintained under maker-checker; escalations worked by people |
| Renewal candidate selection and checks (BRD-06) | Rules on expiry windows, packages and checks | Configured rules; dispositions by people |
| Accounting rules (BRD-05) | Event to journal rules | Rules authorised by Comptrollership after simulation |

Principles for every automated decision: the logic is documented and versioned; a change is approved under maker-checker by the business owner; the version used is recorded with each result; a person decides every outcome that affects a client, a payment or a regulatory report; and the results are monitored with measures that the owner reviews.

## Governance of a generative tool, should BDOI introduce one

BIBS contains no generative text or decision tool. Should BDOI introduce one (for example to draft letters or summarise cases), we propose these controls before any use with BIBS data:

<!-- table: widths=3,9 caption="Controls for a generative tool (proposal)" bold=first -->
| Area | Control |
|---|---|
| Approval | Use case approved by the business owner, BDOI Information Security, the Data Protection Officer and Compliance; privacy impact assessment; vendor and hosting reviewed against BDO's cloud and cross-border rules |
| Data | No Restricted data sent to the tool unless approved; masking of personal data; data scope of the requesting user applied; no use of BIBS data to train a third-party model |
| Prompt controls | Approved prompt templates under version control; user input inserted as data, never as instructions; blocked topics and output limits |
| Output controls | Output is a draft that a person reviews and approves; labelled as generated; never posted, sent or filed without approval; checks against the source record (amounts, names, dates) |
| Access and audit | Same permissions and data scope as BIBS; every prompt and output logged with user, time, template version and record id, kept as the audit trail |
| Monitoring | Monthly review of usage, rejected outputs, errors and complaints by the owner; re-approval at each change of tool, version or template |

# Roles and responsibilities

<!-- table: widths=4.4,1.8,1.8,1.8,1.8,1.8 caption="RACI for data (R responsible, A accountable, C consulted, I informed)" bold=first -->
| Activity | Data owner (BDOI) | BDOI IT | Comptrollership | Compliance and DPO | iorta TechNXT |
|---|---|---|---|---|---|
| Master data content and golden-record rules | A | I | C | C | R (system rules) |
| Data model and dictionary | C | A | C | I | R |
| Accounting events and rules (lineage to GL) | C | I | A, R | I | C |
| Retention schedule and disposal | A | R (execution) | C | C | C |
| Legal hold | C | R | C | A | I |
| Reconciliation controls | C | C | A, R | I | C |
| Data quality monitoring | A, R | C | C | I | C |
| Data classification and privacy | C | C | I | A | R (controls) |
| Automated decision governance | A | C | C | C | R |
| Data catalogue | C | A, R | I | I | C |
| Interfaces to EGL and EDP | C | A | C | I | R |

# Decisions and open points for BDOI

<!-- table: widths=1.1,3.4,5.6,2.2,1.7 caption="Open points" bold=first size=8.5 -->
| Ref | Point | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| RET-01 | Retention of books, accounting records and tax documents | 10 years after the taxable year (5 online, 5 archive), then disposal after BIR clearance; replaces the 20 years configured for receipts and BIR forms | Head - Comptrollership; BDOI Tax | 31 Jan 2027 |
| RET-02 | Insurance Commission record requirements for a broker | 10 years after policy expiry for policies, slips and SOAs, unless the Insurance Commission requires more | Compliance Officer | 31 Jan 2027 |
| RET-03 | AMLA retention | 5 years after the end of the client relationship or the transaction for KYC, screening and STR records; longer while a case is open; legal hold for STRs | Compliance Officer | 31 Jan 2027 |
| RET-04 | BDO records retention policy | Apply the values of chapter 7 for the records without a legal period; archive the audit trail after 5 years; dispose of prospects never confirmed after 5 years | BDOI IT; Compliance Officer | 31 Mar 2027 |
| RET-05 | Archive store | Archive to an S3 archive tier with ECM for final records; BIBS keeps a searchable index of archived records | BDOI IT | 31 Mar 2027 |
| DAT-01 | Data owners | Confirm the owners of chapter 4 by name | BDOI Program Manager | 30 Nov 2026 |
| DAT-02 | Data quality targets | Accept the targets of section 4.3 and the monthly report of chapter 11 | Data owners | 31 Jan 2027 |
| DAT-03 | Enterprise data catalogue | Name the catalogue tool; BIBS publishes schema, comments, classification tags and owners at each release | BDOI IT | 31 Mar 2027 |
| DAT-04 | EGL and EDP extracts | Content, frequency and reconciliation of the EGL extract (journals or trial balance) and the EDP data ingestion (SD 11) | BDOI IT; EGL and EDP teams | 31 Mar 2027 |
| DAT-05 | Client consent record | Where consent is captured and whether BIBS stores it (Security Architecture PRV-01) | BDOI Data Protection Officer | 31 Jan 2027 |
| DAT-06 | Screening model review | Compliance reviews thresholds and false-positive rates each quarter and approves changes as new configuration versions | Compliance Officer | 31 Mar 2027 |
| DAT-07 | Generative tools | Adopt the controls of section 12.2 as BDOI policy before any such tool is used with BIBS data | BDOI Information Security; Data Protection Officer | 30 Jun 2027 |

# References {-}

<!-- table: widths=5,8 caption="References" bold=first -->
| Document | Use |
|---|---|
| BIBS Entity Relationship Diagrams v1.0 and Data Dictionary v1.0 | Physical model of every domain |
| BIBS Security Architecture v1.0 and ASVS L2 Control Mapping v1.0 | Access control, encryption, privacy, audit protection |
| Programme Alignment - Drops, Integrations and Infrastructure v1.0; Integration Inventory v1.0 | Interfaces and infrastructure |
| BRD-13 Data Migration Handbook and Migration Workbook | Migration objects, quality gates, reconciliation L1 to L5 |
| FRS of BRD-01 to BRD-13 | Business rules, retention and audit requirements per domain |
| NIRC Section 235 and RR 17-2013 (as amended by RR 5-2014); RA 9160 (AMLA) as amended and its 2018 IRR; RA 10173 (Data Privacy Act 2012); BSP Circular 808 | Legal bases of the retention schedule |
