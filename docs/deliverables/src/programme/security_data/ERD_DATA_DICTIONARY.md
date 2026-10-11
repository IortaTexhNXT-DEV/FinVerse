---
# Entity relationship diagrams of the production schema (BRD-00). Build: build_secdata_pack.py (expands the
# <!-- sd:... --> lines from data/catalog.json, data/descriptions.json and domains.yaml; writes figures/erd/*.dot).
title: Entity Relationship Diagrams
subtitle: Production schema of BIBS by data domain and module, with the Data Dictionary workbook
doc_type: Entity Relationship Diagrams
doc_code: Data
brd: BRD-00
name: Entity Relationship Diagrams
doc_id: BIBS-ERD-BRD-00
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Entity Relationship Diagrams
output: Data/BIBS_Data_BRD-00_Entity_Relationship_Diagrams_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Data Architect
    reviewer: iorta TechNXT Solution Architect
    approver: BDOI IT (pending)
    change: First issue, read from the system catalogue of a PostgreSQL 16 database holding the production schema of 08 October 2026
distribution:
  - {name: "BDOI Information Technology Group (BDOI IT)", role: Approver, organisation: BDOI, purpose: "Data model, reporting and extract design, database administration"}
  - {name: "Data Migration Lead and data owners", role: Reviewer, organisation: BDOI, purpose: "Target tables of the migration objects (BRD-13)"}
  - {name: "BDOI Information Security", role: Reviewer, organisation: BDOI, purpose: "Personal data, insert-only and audit tables"}
  - {name: "Enterprise Data Platform team (EDP)", role: Reviewer, organisation: BDO Unibank IT, purpose: "Source tables of the EDP extracts"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Data architecture, migration, reports, interfaces"}
---

# Introduction

## Purpose and audience

This document shows the tables of the BIBS (BDOI Broker System, on iNXT BrokerVerse) production schema and the foreign keys between them, one data domain at a time. The companion workbook, BIBS Data Dictionary v1.0, lists every column with its data type, nullability, default, keys, allowed values, indexes and meaning. Together they are the reference for BDOI IT, the Data Migration team (BRD-13), the report and extract designers (EGL, EDP) and the database administrators.

The audience is technical. The Data Architecture v1.0 explains the conceptual and logical model, master data, lineage, audit, retention and data quality; this document is its physical counterpart.

## Scope

<!-- table: widths=3,10 caption="Scope of the diagrams and the dictionary" bold=first -->
| Item | In scope |
|---|---|
| Schema | The production schema of BIBS on PostgreSQL 16: every table, column, primary key, foreign key, unique and check constraint, secondary index and trigger created by the schema migrations |
| Not in scope | Seed data for SIT, UAT and training (it is loaded only by the seed profile and never in production); reference rows; table sizes and volumes (Data Architecture, chapter 2) |
| Not included | The tables of the insurer-company modules being removed from BIBS (section 1.5) |
| Date | Schema of 08 October 2026; the documents are refreshed with each release that changes the schema |

## How the content was obtained

The content is read from the database, not written by hand, so it matches the system exactly:

1. An empty PostgreSQL 16 database was created on the project cluster.
2. The production schema migrations were applied in version order, one transaction per migration, without starting the application. The seed migrations were not applied.
3. Tables, columns, types, nullability, defaults, keys, foreign keys, check constraints, indexes, triggers and comments were read from the PostgreSQL system catalogue.
4. The meaning of each table is the first sentence of the documentation of its persistent entity; columns shared by most tables (identifier, version, created and updated stamps, company, branch, maker-checker fields) carry a standard meaning, and every other column is described from its name and its foreign key.
5. The database was dropped after the extraction.

<!-- sd:erd_stats -->

## Conventions

- Identifiers are written in upper case. PostgreSQL folds unquoted names to lower case, so CLIENT_CODE in this document is the same column as its lower-case name in the catalogue.
- Each figure shows the tables of one module with their primary key (PK) and the foreign key columns (FK) drawn in the figure; "+ n columns" counts the other columns, which are in the dictionary.
- A line runs from the foreign key column of the child table to the parent table. The crow's foot marks the "many" end; two bars at the parent end mean a mandatory link (FK column NOT NULL), a bar and a circle mean an optional link.
- A grey rounded box is a table of another module (its module name under it).
- Every business table carries COMPANY_ID (and many BRANCH_ID) with a foreign key to ORG_COMPANY (ORG_BRANCH). These links are drawn only in the Organisation figures; elsewhere they would join every table to one box. They are listed in the dictionary.
- A module with many tables is drawn in several figures, each holding connected tables; a table referenced by many tables of the module (for example RNW_CANDIDATE) is then repeated as a grey box. Tables without any foreign key inside their module are listed in the module's table with "Listed only (no link)".

## Tables not included: insurer-company modules

BIBS is an insurance broking system. The insurer-company modules of the platform (insurer underwriting, insurer claims, reinsurance treaty and cession accounting, actuarial reserves, group consolidation and inter-company transactions) are being removed from BIBS and are not BIBS capabilities. Their tables still exist in the schema of 08 October 2026 until the removal migrations run; they are not included in the diagrams or the dictionary. The reinsurance broking of phase 2 is a separate BRD and will have its own tables.

<!-- sd:excluded -->

# Data domains

The domains follow the bounded contexts of the modular monolith and the BRDs. Each domain has one data owner at BDOI who approves changes to its master data, its retention and its quality rules (Data Architecture, chapter 4).

<!-- sd:domain_summary -->

<!-- sd:domain_owners -->

## Reading the dictionary workbook

<!-- table: widths=3,10 caption="Sheets of the Data Dictionary workbook" bold=first -->
| Sheet | Content |
|---|---|
| Cover, README | Title, sources, legend, the meaning of every column of the sheets |
| Index | One row per table: domain, module, description, number of columns, primary key, tables referenced, number of incoming foreign keys, number of indexes, insert-only flag and the figure of this document that shows the table |
| D01 to D17 | One sheet per data domain, one row per column: table, position, column, data type, NULL allowed, default, key (PK, FK, UQ), referenced table and column (with "delete cascades" where the foreign key cascades), allowed values and check constraints, secondary indexes led by the column, description |

The sheets have filters and frozen headers. To follow a foreign key, filter the References column of the domain sheet, or look the parent table up in the Index sheet.

<!-- landscape -->

# Entity relationship diagrams

<!-- sd:erd_domains -->

<!-- portrait -->

# Decisions and open points for BDOI

<!-- table: widths=1,5,6,2.2,2 caption="Decisions and open points" bold=first -->
| Ref | Point | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| ERD-01 | Removal of the insurer-company tables listed in section 1.5 | Drop the tables with the removal of the insurer modules, before the first SIT cycle of Drop 1; the diagrams and the dictionary are then reissued with the same content | iorta TechNXT; BDOI IT to note | 30 Nov 2026 |
| ERD-02 | Column comments in the database | Write the meaning of each table and of each business column as a database comment, so that BDOI's data catalogue reads it with the schema (Data Architecture, chapter 8) | iorta TechNXT; BDOI IT to confirm the catalogue tool | 31 Jan 2027 |
| ERD-03 | Identifier style in BDOI documents | Upper case, as here; BDOI IT confirms that its standards accept it | BDOI IT | 31 Oct 2026 |
| ERD-04 | Read access for BDOI report and extract designers | A read-only database role on a reporting replica, limited to the tables the EGL and EDP extracts read; no access to the production primary | BDOI IT, BDOI Information Security | 31 Mar 2027 |
| ERD-05 | Refresh of this document | Regenerated from the catalogue at every release that changes the schema; version 1.x for each Drop 1 and Drop 2 release | iorta TechNXT | Each release |

# References {-}

<!-- table: widths=4,9 caption="References" bold=first -->
| Document | Use |
|---|---|
| BIBS Data Dictionary v1.0 (Excel) | Column-level dictionary of every table of this document |
| BIBS Data Architecture v1.0 | Conceptual and logical model, master data, lineage, audit, retention, data quality |
| BIBS Security Architecture v1.0 | Encryption, access control, audit trail and the database roles |
| BDOI Drop Plan and Programme Alignment v1.0 | Drops and the BRD of each domain |
| BRD-13 Data Migration Handbook and Migration Workbook | Migration objects and their target tables |
