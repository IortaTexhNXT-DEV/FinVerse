---
# BIBS Database Design (BRD-00). Build: build_database_design.py (expands the <!-- dd:... --> and <!-- sd:... --> lines
# from data/catalog.json, data/descriptions.json and ../security_data/domains.yaml; writes figures/erd/*.dot).
# Chapters mirror the iNXT BrokerVerse OOTB document 02 Database Design.
title: Database Design
subtitle: Default database design of BIBS on iNXT BrokerVerse, with the notes for data migration
doc_type: Database Design
doc_code: Database_Design
brd: BRD-00
name: Database Design
doc_id: BIBS-DBD-BRD-00
version: "1.0"
date: 13 October 2026
status: Issued for BDOI review
header_title: Database Design
output: Database_Design/BIBS_DB_BIBS_Database_Design_v1.0.docx
control:
  - version: "1.0"
    date: 13 Oct 2026
    author: iorta TechNXT Data Architect
    reviewer: iorta TechNXT Solution Architect; Migration Lead
    approver: BDOI IT (pending)
    change: First issue, read from the system catalogue of a PostgreSQL 16 database holding the BIBS schema of 13 October 2026; supersedes the Data Dictionary v1.0 and the Entity Relationship Diagrams v1.0 of 08 October 2026 for the physical design
distribution:
  - {name: "BDOI Information Technology Group (BDOI IT)", role: Approver, organisation: BDOI, purpose: "Database design, administration, reporting and extracts"}
  - {name: "Data Migration Lead and data owners", role: Reviewer, organisation: BDOI, purpose: "Target tables, keys, code lists and load order of the migration objects (BRD-13)"}
  - {name: "BIBS Product Owner", role: Reviewer, organisation: BDOI, purpose: "Data migration preparation"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Data architecture, migration, reports, interfaces"}
---

# Introduction

## Purpose and scope

This document describes the default database design of BIBS (BDOI Broker System, on iNXT BrokerVerse): the principles and conventions, the schema by data domain and module, the entity relationship diagrams of each module, the table catalogue with the purpose, keys and relationships of every table, the reference data and code lists, audit and history, document numbering, data retention, and the facts the data migration team needs to map and load the legacy data (master and reference tables against transactional tables, natural keys and code columns, mandatory columns, allowed values, number series and the load order).

Its companion workbook, BIBS Database Inventory v1.0, lists every table and column with data type, nullability, default, keys, indexes and allowed values, every foreign key, the load order and the enumerations. The two follow the structure of the iNXT BrokerVerse OOTB documents 02 Database Design and 03 Database Inventory, which describe the out-of-the-box platform; this document describes the BIBS database.

## Database platform

<!-- table: widths=3.5,10 caption="Database platform" bold=first -->
| Item | Design |
|---|---|
| Engine | PostgreSQL 16 (Amazon RDS for PostgreSQL in the BDOI AWS accounts; Multi-AZ in production) |
| Schemas | One application schema (PUBLIC) holds every table; the schema version history is kept in its own table |
| Logins | The schema owner applies the versioned schema changes; the application connects with a least-privilege runtime role that has row access only (no DDL, no TRUNCATE, cannot disable a trigger) |
| Time | Time stamps are TIMESTAMPTZ and stored in UTC; business dates are DATE in the Asia/Manila calendar |
| Character set | UTF-8 |
| Schema changes | Versioned, forward-only change scripts applied in order at deployment, one transaction per version; a version is never edited once applied |

## How the content was obtained

The content is read from the database, not written by hand, so it matches the schema exactly:

1. An empty PostgreSQL 16 database was created.
2. All schema versions were applied in order, one transaction per version, without starting the application; reference rows for test environments were not applied.
3. Tables, columns, types, nullability, defaults, keys, foreign keys, check constraints, indexes, triggers, sequences and views were read from the PostgreSQL system catalogue.
4. The purpose of each table is the first sentence of the documentation of its entity; shared columns carry a standard meaning, other columns are described from their name and foreign key.
5. The class of a table (reference, master, transactional, audit and log, migration control, system) and the load order are derived from the catalogue (sections 6.1 and 6.6).
6. The database was dropped after the extraction.

<!-- dd:stats -->

# Principles and conventions

## Design principles

- **One modular database.** Each module (bounded context) owns its tables, recognisable by their prefix (for example RNW for renewal, CSH for cashiering, GL for the general ledger). A module reads another module's tables only through foreign keys to masters and through the platform services.
- **Surrogate keys, business keys unique.** Every table has a surrogate primary key; business numbers and codes are unique keys of their own (section 2.2).
- **Data scope on every business row.** Business tables carry COMPANY_ID and, where the record belongs to a branch, BRANCH_ID; the data scope of a user restricts the rows the user sees.
- **Maker-checker on masters.** Masters carry RECORD_STATUS, AUTHORIZED_BY and AUTHORIZED_AT; the checker is never the maker.
- **Optimistic locking.** VERSION is incremented on every update; a stale update is refused.
- **Who and when.** CREATED_AT, CREATED_BY, UPDATED_AT and UPDATED_BY on every table that is changed by users.
- **Insert-only evidence.** The audit trail, the ledger entries and the histories listed in section 2.6 refuse UPDATE, DELETE and TRUNCATE at database level; corrections are made by reversal.
- **Allowed values in the database.** Status, type and kind columns are restricted by check constraints to their list of codes (section 5).
- **Origin of a row.** The tables that receive migrated data record the origin of each row (BIBS or MIGRATED), the legacy system, the legacy reference and the migration batch (section 6.4).

## Keys

- Primary key: ID BIGINT, an identity column (BY DEFAULT) (a load may supply the value; the identity is then advanced past the highest loaded value). Link tables and counters use their natural composite key.
- Business keys: numbers (policy, invoice, receipt, claim, request numbers) and codes (clients, insurers, products, accounts, list values) are unique, per company where the number series is per company. Case-insensitive uniqueness uses an index on LOWER(column).
- Foreign keys reference the surrogate ID of the parent. Some modules also hold a business code of a master (for example INSURER_CODE, CLIENT_CODE) for reporting and for matching legacy data; the code is checked by the application.
- Partial unique indexes enforce one active row where the business requires it (for example one open renewal candidate per policy).

## Column types

<!-- dd:types -->

Amounts are NUMERIC with two decimals in the currency of the row; the currency is the ISO 4217 code in CURRENCY or a column named for its role (for example PREMIUM_CURRENCY). Rates and percentages use a larger scale. Free text is VARCHAR with a length set per column, so a migrated value longer than the column is refused, not cut.

## Data domains and modules

The tables fall into the data domains below. Each domain has one data owner at BDOI who approves changes to its master data, its retention and its quality rules.

<!-- sd:domain_summary -->

<!-- dd:domain_classes -->

<!-- sd:domain_owners -->

## Document numbering

Document numbers come from number series kept in the database, so two users never receive the same number and a number is not reused after a rollback where the series requires gapless numbering:

<!-- dd:numbering -->

The format of each series (prefix, branch, year, running number) is configuration. For data migration, documents keep their legacy number; the running number of each BIBS series is set above the highest migrated number of the same series before go-live, and BIR-registered receipt series continue from the next unused number of the authority to print.

## Audit, history and soft delete

Every create, update, authorisation, posting, reversal, sign-in and export is written to the audit trail in the same transaction as the change. Masters are not deleted: they are deactivated (ACTIVE false, or an inactive record status) or end-dated (EFFECTIVE_TO). Documents are cancelled or reversed, never deleted.

<!-- dd:audit -->

## Constraints and indexes

Foreign keys are declared for every link between tables of BIBS; most use the default rule (a parent row cannot be deleted while children exist), and the lines of a document cascade with their header. Check constraints hold the lists of allowed values, the ranges (for example a percentage between 0 and 100) and the cross-column rules (for example an end date not before the start date). Secondary indexes support the foreign keys of growing tables, the search screens and the jobs.

<!-- dd:self_refs -->

<!-- landscape -->

# Entity relationship diagrams

Each figure shows the tables of one module with their primary key (PK) and the foreign key columns (FK) drawn in the figure; "+ n columns" counts the other columns, which are in the inventory. A line runs from the foreign key column of the child table to the parent table: the crow's foot marks the "many" end; two bars at the parent end mean a mandatory link, a bar and a circle an optional link. A grey rounded box is a table of another module. The links to ORG_COMPANY and ORG_BRANCH are drawn only in the Organisation figures.

<!-- dd:erd_count -->

<!-- sd:erd_domains -->

<!-- portrait -->

# Table catalogue

One row per table: its purpose, class for data migration, primary key, natural or unique keys, the tables it references and its load level (Lvl, section 6.6). Columns, types and indexes are in the inventory workbook.

<!-- dd:catalogue -->

# Reference data and lists of values

## Lists of values

The code lists that users maintain are held in two tables: a list type and its values, each value with a code, a label, an order, an optional parent code (for dependent lists) and effective dates. System parameters hold the configurable values of the rules (limits, days, switches) with their type and owner.

<!-- dd:reference -->

## Allowed values (enumerations)

Codes that drive the processing (statuses, kinds, types, sources) are fixed lists held as check constraints of their column. A migrated row must carry one of the listed codes; the legacy value is translated through the code maps of the migration. The inventory sheet Enumerations lists every check constraint with its values.

<!-- dd:enum_summary -->

## Masters

The masters (organisation, currency, periods, product catalogue, chart of accounts, users and roles, insurers, clients and the other maker-checker masters) are listed with class Master in the table catalogue. They are loaded or configured before the transactions that reference them, and each is approved by its data owner.

# Data migration notes

## Master and reference tables against transactional tables

<!-- dd:classes -->

The class is derived from the catalogue: Reference for the code list and parameter tables; Migration control for the tables of the migration console; Audit and log for the trail, histories, logs and insert-only tables; System for the technical state; Master for tables with maker-checker approval or owned by a master module; Transactional for the rest. The data migration team confirms the class of each table in the Migration Workbook (BRD-13).

## Natural keys and code columns for legacy mapping

Legacy records are matched to BIBS rows by the natural or unique keys of the table catalogue (for example the client code, the policy number with its insurer, the invoice number) and not by the surrogate ID. The column "Code and number columns" of the inventory lists, per table, the columns that hold codes, numbers and references usable for mapping. Foreign keys are resolved at load time from the business code of the parent to its ID.

## Mandatory columns

A column that is NOT NULL and has no default must be provided by every loaded row; the inventory lists these columns per table (All tables sheet) and marks them in the column sheets (Mandatory). Columns with a default (for example VERSION, CREATED_AT, ACTIVE, RECORD_STATUS) take it when the load leaves them out.

## Origin of migrated rows

<!-- dd:legacy -->

ORIGIN is BIBS or MIGRATED; SOURCE_SYSTEM names the legacy system; LEGACY_REF holds the legacy key; MIGRATION_BATCH the batch of the migration console that loaded the row, so a batch can be reconciled and rolled back.

## Number series and sequences

Identity values come from one sequence per table. When a load supplies identifiers, each sequence is advanced past the highest loaded value before the system is opened to users. The document number series (section 2.5) are set above the highest migrated number of each series.

## Load order

The load level of a table is one more than the highest level of the tables it references; a table of level 1 references no other table. Loading the levels in order satisfies every foreign key. Tables in the same level can be loaded in parallel.

<!-- dd:load_levels -->

<!-- dd:cycles -->

Hierarchies within one table (section 2.7) are loaded parents first, or with the parent link empty and updated after the load.

# Data retention

Retention is configuration: retention rules per record type (years online, years in archive, statuses, action REVIEW or ARCHIVE) and record classes of stored files (retention period, legal hold, archive). A monthly job counts the records past their online period for the data owner's review; archive or disposal needs the owner's approval, and legal hold blocks both. The proposed schedule (BIR 10 years after the taxable year for books and tax records, AMLA 5 years after the end of the relationship for clients and screening, 15 years for the access change log and the legacy archive) is in the Data Architecture v1.0, chapter 7, and remains to be confirmed by BDOI.

Migrated history keeps the retention of its record type from its original date. Legacy records that are not migrated as BIBS transactions are kept in the legacy archive tables (D17), with every inquiry logged.

# Time zone, date and amount handling

- Time stamps are stored in UTC (TIMESTAMPTZ) and shown in Asia/Manila time; a migrated local time is converted to UTC on load.
- Business dates (effective, expiry, due, posting, value dates) are DATE without time.
- Amounts are NUMERIC with the currency of the row; amounts in a foreign currency carry the exchange rate and the amount in pesos where the ledger needs it. No amount is stored as a floating-point number.
- Rounding to two decimals follows the rule of the document (premium, taxes, commission) at the time the amount is computed; migrated amounts are loaded as recorded in the legacy system, not recomputed.

# Decisions and open points for BDOI

<!-- table: widths=1.2,5,6,2.4,2 caption="Decisions and open points" bold=first -->
| Ref | Point | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| DBD-01 | Class of each table for the migration | Confirm the class (Reference, Master, Transactional) and the in-scope history per table in the Migration Workbook | Data Migration Lead | 30 Nov 2026 |
| DBD-02 | Cut-over of the number series | Set each series above the highest migrated number; BIR receipt series continue from the next unused number of the authority to print | BDOI Comptrollership; iorta TechNXT | Mock migration 1 |
| DBD-03 | Retention values | Confirm the retention schedule of the Data Architecture v1.0 (RET-01 to RET-04) | BDOI Compliance; BDOI IT | 31 Jan 2027 |
| DBD-04 | Refresh of this document | Refreshed from the catalogue at every release that changes the schema | iorta TechNXT | Each release |

# References {-}

<!-- table: widths=4.5,9 caption="References" bold=first -->
| Document | Use |
|---|---|
| BIBS Database Inventory v1.0 (Excel) | Every table and column, foreign keys, load order, enumerations, number series |
| iNXT BrokerVerse OOTB 02 Database Design and 03 Database Inventory v1.2 | Design of the out-of-the-box platform; the structure this document follows |
| BIBS Data Architecture v1.0 | Conceptual model, master data, lineage, audit, retention, data quality |
| BIBS Data Dictionary v1.0 and Entity Relationship Diagrams v1.0 (08 Oct 2026) | Superseded by this document and the inventory for the physical design |
| BRD-13 Data Migration Handbook and Migration Workbook | Migration objects, templates and code maps |
