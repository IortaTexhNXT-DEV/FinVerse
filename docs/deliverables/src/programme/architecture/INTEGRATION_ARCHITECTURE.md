---
# Integration Architecture of BIBS (BRD-00, Programme/Architecture).
# Build: python docs/deliverables/src/programme/architecture/build_architecture_pack.py
title: Integration Architecture
subtitle: Interface catalogue, patterns, security, error handling, reconciliation and ownership of the BIBS interfaces
doc_type: Integration Architecture
doc_code: Architecture
brd: BRD-00
name: Integration Architecture
doc_id: BIBS-ARC-04
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Architecture set, document 4 of 6
h1_page_break: false
output: Architecture/BIBS_Architecture_BRD-00_Integration_Architecture_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Integration Lead; iorta TechNXT Tech Lead
    approver: BDOI IT Application Development and Integration (pending)
    change: First issue, from the integration inventory of the Programme Alignment pack v1.0 and the interfaces named in the BRDs
distribution:
  - {name: "Application Development and Integration (integration owners)", role: Approver, organisation: BDOI IT, purpose: "Interface catalogue, patterns, ownership"}
  - {name: "BDO API team (Apigee X)", role: Reviewer, organisation: BDO Unibank IT, purpose: "API proxies, tokens, private route"}
  - {name: "Information Security Office", role: Reviewer, organisation: BDOI IT, purpose: "Security per interface"}
  - {name: "Owners of BRD-1 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "Reconciliation owners, file layouts"}
  - {name: "Treasury, Operations, Comptrollership", role: Reviewers, organisation: BDOI, purpose: "Bank channels, insurer files, EGL"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Interface delivery and testing"}
---

# Introduction

## Purpose and audience

This document describes every interface between BIBS and other systems: what flows, by which pattern, how it is secured, how errors are handled and reconciled, and who owns each side. It is written for BDOI IT Application Development and Integration (owner of every external touch-point, BDOI answer A2 of 26 September 2026), the BDO API team, the Information Security Office and the business units that reconcile the data.

## Scope

The interfaces INT-01 to INT-20 of the integration inventory of the Programme Alignment pack (same numbers; INT-11 ICBS is struck through on the drop plan and not listed), the interfaces named in the BRDs but not on the drop plan (INT-21 to INT-25), the document store (INT-26), the legacy extracts of data migration (INT-27) and the SMS question (INT-28). Each interface gets an interface specification agreed with its owner before delivery (chapter 8).

## Sources

<!-- table: widths=1,7,9.6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | Programme Alignment pack v1.0 and its Integration Inventory workbook (26-Sep-2026) | INT-01 to INT-20, questions IQ04 to IQ19 |
| S2 | BDOI answers of 26-Sep-2026 (A2 integration names and ownership, A4 documents in S3) | Meanings, ownership |
| S3 | Application architecture option decision, section 5 (edge paths, Apigee X) | Patterns and security of system calls |
| S4 | Platform cache and events architecture | Topics, outbox, consumers, dead letters |
| S5 | Document storage decision record | S3 buckets, malware scan, ECM archive |
| S6 | Design notes of the BRD modules (operations, collections, screening, submitted policies, user access) | Ports and file interfaces of each module |
| S7 | BRDs 01 to 13 | Data exchanged, frequencies, reconciliation needs |

## Conventions

Open points of this document are numbered IN-01 to IN-10 (chapter 10). "To confirm" marks a value BDOI IT has not given yet; the question number says where it is asked.

# Integration principles

<!-- table: widths=1,4.4,12.2 caption="Integration principles" bold=first size=8.5 -->
| # | Principle | Practice |
|---|---|---|
| I1 | One governed entry for system calls | Every synchronous call between BIBS and a BDO system passes through Apigee X proxies in both directions (ADR-16); no system calls BIBS directly and no system reads the BIBS database |
| I2 | The right pattern for the job | Synchronous APIs for requests that need an immediate answer; events for facts that others react to; files for bulk, scheduled or bank and insurer exchanges; manual steps only where the counterpart offers no interface |
| I3 | Business transaction first | Outbound work never runs inside the user's transaction: events through the outbox, files by jobs; a counterpart that is down never blocks a BIBS user |
| I4 | Safe to repeat | Every inbound message or file carries a source reference that BIBS refuses twice; every event carries an identifier that consumers deduplicate on |
| I5 | Controlled and reconciled | Every file has control totals; every interface has a reconciliation report and a business owner who signs it off |
| I6 | Secure by default | TLS on every hop; OAuth 2.0 tokens with scopes per API; files encrypted at rest with BDOI keys and scanned for malware before processing; no personal data in event payloads or object keys |
| I7 | Ports in BIBS | Each interface is a port of the owning module with a connector chosen by configuration, so a file interface can become an API without a change in the module |
| I8 | Specified and versioned | Each interface has a specification (layout, transport, schedule, errors, reconciliation) agreed with its owner; breaking changes are new versions run side by side |

# Integration landscape

![Integration landscape by pattern](figures/in_landscape.dot){width=16.5}

<!-- arch:integrations caption="Interface catalogue" -->

<!-- table: widths=2.6,15 caption="Interfaces per drop" bold=first size=8.5 -->
| Drop | Interfaces |
|---|---|
| Drop 0 | INT-01 EIAM, INT-02 UIDM-ISC, INT-03 LMS, INT-04 HL-LOAS (HLS side), INT-06 PMS, INT-07 CMS / New BOB, INT-08 OBPCS, INT-09 Old BOB, INT-10 AFTS, INT-12 ECM, INT-13 CCM, INT-14 M365, INT-15 TFS, INT-26 document store, INT-27 legacy extracts |
| Drop 1 | INT-05 LFS (proposed), INT-21 watchlist feeds, INT-22 AMLC STR filing, INT-23 BIR e-filing, INT-24 file server, INT-25 mail house |
| Drop 2 | INT-04 HL-LOAS (LOAS side), INT-16 EDP, INT-17 EGL, INT-18 CARMS, INT-19 insurer interfaces, INT-20 Bridger Insight XG |

# Integration patterns

## Synchronous APIs through Apigee X

**Inbound (a BDO system calls BIBS).** The system obtains an OAuth 2.0 access token from Apigee X (client credentials) and calls the Apigee proxy of the BIBS API; Apigee applies quotas, spike arrest and analytics and forwards the call over the private route to the internal load balancer, where only the integration path prefix is admitted from the Apigee source ranges. bibs-integration validates the token in its own security chain - signature against the Apigee key set, issuer, audience, validity, asymmetric algorithms only - and the scopes configured for the path of the API; a path without a rule is refused. A BIBS user token is refused there, and an Apigee token is refused on the user operations.

- Operations are versioned in the path (integration/v1) and follow the API standards of the API Specification (problem details, correlation identifier, Idempotency-Key header on creations).
- Answers: 401 for a missing or invalid token, 403 for a missing scope or an unconfigured path, 409 for a repeated source reference, 422 for a business rule.
- The connectivity check of the integration interface answers the client identifier and scopes of the token, so each Apigee proxy can be tested end to end before its first business call.

**Outbound (BIBS calls a BDO system).** BIBS calls the Apigee proxy of the target with its own client credentials; connect time-out 5 seconds and response time-out 30 seconds (proposed); safe reads are retried twice with back-off; a failed call never fails the user's transaction - it leaves the item in the work queue of the module with the reason, and the job retries it.

## Events (Kafka)

Integration events leave BIBS through the transactional outbox on the ten topics bibs.<domain>.<event>.v1 (Solution Architecture, chapter 6). Consumers inside BIBS deliver e-mail (INT-13), archive events and archive final records to ECM (INT-12). A BDO system that needs a BIBS event subscribes through a consumer group agreed with BDOI IT on MSK, or receives it through Apigee from a BIBS connector; the payloads carry identifiers, status and amounts, never personal data.

<!-- arch:topics caption="Kafka topics available to consumers" -->

## Files

**Inbound files** (bank, loan, PDC, watchlist, insurer, migration) arrive in the incoming prefix of the inbound bucket of the environment - by AWS Transfer Family (SFTP) for host-to-host senders, by a presigned upload link, or by a user upload on the BIBS screen - with a declared SHA-256. The malware scan tags each object; only clean objects are processed (chapter 7).

**Outbound files** (payment instructions, extracts for EGL and EDP, collection and remittance files, print batches) are written by bibs-jobs to the reports bucket with a control file and recorded in the file register (name, size, SHA-256, run). Recipients download them with a presigned link valid 5 minutes after the BIBS permission check, or receive them by managed transfer to their own landing area.

<!-- table: widths=4,13.6 caption="File standards" bold=first size=8.5 -->
| Topic | Standard |
|---|---|
| Naming | <SOURCE>_<FILE TYPE>_<company>_<yyyyMMdd>_<HHmmss>.<ext>, unique per run; the date is the business date |
| Encoding | UTF-8 text without control characters (CSV, TXT, pipe-delimited) or the Excel template of the upload screen; fixed-width only where a bank layout requires it |
| Control | Header and trailer (or a separate control file) with record count and amount totals per currency; a file whose totals do not match is refused as a whole |
| Size | Up to 25 MB through the application; larger files by presigned upload or transfer, up to 5 GB |
| Duplicates | A file with the same name or SHA-256 as an accepted file is refused |
| Retention | Inbound 90 days, reports per the report archive setting (default 400 days), migration extracts 5 days |

## Sign-in redirect, e-mail and manual steps

- **Browser redirect** (INT-01): OpenID Connect with Entra ID; SAML 2.0 supported as the alternative (ADR-09).
- **E-mail** (INT-13): SMTP relay of CCM with STARTTLS, password-protected PDF attachments; the queue and the retry job make delivery independent of the user transaction.
- **Manual** (INT-15, INT-20, INT-22, INT-23): BIBS produces or records the data (Trade client and payment file, Bridger result, STR file, BIR returns); a user carries it to or from the counterpart and records its reference in BIBS.

## Choosing the pattern

<!-- table: widths=5.4,4,8.2 caption="Pattern selection" bold=first size=8.5 -->
| Need | Pattern | Examples |
|---|---|---|
| Immediate answer to a request | Synchronous API through Apigee X | Quotation request from HL-LOAS, account provisioning from UIDM-ISC |
| A fact others react to, no answer needed | Event | Invoice booked, receipt issued, record to archive |
| Bulk or scheduled exchange, bank or insurer layouts | File | Payment instructions, collection files, EGL and EDP extracts |
| User identity | Browser redirect | EIAM sign-in |
| Counterpart without an interface | Manual with a reference recorded | BIR e-filing, AMLC STR filing |

# Transport and data per interface

<!-- arch:integration_transport -->

# Security per interface

General controls that apply to every interface:

- TLS 1.2 or higher on every hop; no plaintext protocol anywhere (the backend refuses to start in production with a plaintext connection).
- Synchronous calls: OAuth 2.0 client credentials through Apigee X with scopes per API; tokens validated by BIBS; source ranges limited at the WAF.
- Files: encrypted at rest with the BDOI KMS key of the bucket class; malware scan before processing; access by IAM role of the workload; presigned links valid 5 minutes, issued after the permission check and audited.
- Events: SASL_SSL; topics created by the application only; payloads without personal data.
- Personal data in extracts for non-production or for EDP is masked unless the recipient is authorised (IN-06).

<!-- arch:integration_security -->

# Error handling and reconciliation

## Handling errors by pattern

<!-- table: widths=3.4,14.2 caption="Error handling by pattern" bold=first size=8.5 -->
| Pattern | Handling |
|---|---|
| Inbound API | Validation and business errors answered at once as problem details with a code; the caller corrects and calls again; a repeated source reference is refused (409) so a retry after a time-out never creates a duplicate |
| Outbound API | Safe reads retried twice with back-off; other failures leave the item in the module's work queue with the reason; a job retries; repeated failures raise an exception code alert to the owner unit |
| Events | Outbox retries with back-off, failed after 10 attempts and visible on the Integration Events screen; consumer retries 3 times, then the dead-letter topic with retry and discard on the same screen |
| Inbound file | Whole-file checks (scan, structure, totals, duplicate) refuse the file with a result; row errors are listed per row while valid rows are processed in their own transaction; the sender corrects and re-sends only the rejected rows when the layout allows it |
| Outbound file | A failed run raises JOB_FAILURE; the job is re-run for the same business date and replaces the file (same name with a new run time); the recipient takes the latest run |

![Inbound file: from arrival to reconciliation](figures/in_fileflow.dot){width=12}

## Error handling and reconciliation per interface

<!-- arch:integration_errors -->

Reconciliation reports are part of the interface: each one shows what was sent or received (count and totals), what was processed, what was rejected and the differences, and is signed off by the business owner of the interface on the frequency of the interface (daily for bank and collection files, per period for EGL).

# Ownership and governance

## Interface ownership

All external touch-points are owned by BDOI IT (answer A2); the business owner of each interface is the unit that reconciles it.

<!-- arch:integration_owners -->

<!-- table: widths=5.6,2.4,2.4,2.4,2.4,2.4 caption="Interface responsibilities (R responsible, A accountable, C consulted, I informed)" bold=first size=8 -->
| Activity | BDOI IT integration owner | Counterpart system owner | BDO API team | BDOI business owner | iorta TechNXT |
|---|---|---|---|---|---|
| Interface specification | A | R | C | C | R |
| Apigee proxy, product and scopes | C | I | A, R | I | C |
| Connector in BIBS | I | I | I | I | A, R |
| Connection test in SIT | A | R | R | I | R |
| Reconciliation sign-off | I | I | I | A, R | C |
| Change of layout or version | A | R | C | C | R |

## Interface specification

Every interface has a specification agreed before delivery, with these sections: purpose and BRD references; counterpart and owners; pattern, direction, transport and schedule; layout or message contract with field rules and code lists; security (identity, scopes, keys, network route); volumes and time windows; error handling and reprocessing; reconciliation report and owner; environments and test data; versioning and change procedure.

## Lifecycle

1. **Specify** - BDOI IT and the counterpart owner give the layout and transport; iorta TechNXT writes the specification; BDOI IT approves it.
2. **Connect** - the BDO API team publishes the Apigee proxy, the network team opens the route, the connector is configured in SIT.
3. **Test** - connection test, then integration test cycles in SIT with the counterpart's test system and reconciliation reports.
4. **Accept** - UAT with the business owner, including a reconciliation cycle.
5. **Operate** - monitoring and alerts per interface; changes through the versioning rule (a breaking change is a new version run side by side).

# Schedule

<!-- table: widths=3.6,8,6 caption="Integration schedule" bold=first size=8.5 -->
| By | What | From whom |
|---|---|---|
| 16-Oct-2026 | Answers to the priority-1 integration questions (IQ04, IQ05, IQ10, IQ15, IQ16) | BDOI IT |
| 30-Nov-2026 | Apigee X onboarding of BIBS (private route, source ranges, token issuer and key set); EIAM app registration | BDO API and network teams, BDOI IT Identity |
| 15-Jan-2027 | Interface specifications of Drop 0 and Drop 1 (layouts of the bank, loan and PDC files, CCM) | BDOI IT and counterpart owners with iorta TechNXT |
| 31-Jan-2027 | Specifications of EGL, EDP, CARMS and the insurer channels (Drop 2) | BDOI IT, Comptrollership, Operations |
| 30-Apr-2027 | Integration test connections for EGL, EDP and the insurer channels in SIT | BDOI IT |
| Jan - Sep 2027 | SIT of the interfaces with each drop; reconciliation reports reviewed | iorta TechNXT, BDOI IT |
| Aug - Nov 2027 | UAT with reconciliation cycles per interface | BDOI business owners |

# Decisions and open points for BDOI

<!-- table: widths=1.2,3,8.4,2.8,2.2 caption="Decisions and open points for BDOI" bold=first size=8 -->
| ID | Topic | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| IN-01 | Bank, loan and PDC file layouts | BDOI IT provides the layouts, schedules and transport of INT-03 to INT-10, and says what AFTS is used for (IQ06 to IQ10) | BDOI IT, Treasury | 15-Jan-2027 |
| IN-02 | File transport | AWS Transfer Family (SFTP) into the inbound bucket for host-to-host senders; presigned upload or the BIBS upload screen otherwise | BDO IT Cloud, network team | 30-Nov-2026 |
| IN-03 | Apigee X onboarding | One Apigee product per counterpart with scopes per API; private route from Apigee X to the BIBS load balancer; BIBS registered as an Apigee client for outbound calls | BDO API team | 30-Nov-2026 |
| IN-04 | EGL feed | Daily journal detail file mapped to EGL accounts and segments, reconciled per period by Comptrollership (IQ16) | BDOI Comptrollership, BDO EGL team | 16-Oct-2026 |
| IN-05 | SMS | No SMS channel at go-live: no BRD requires it; notifications by e-mail and in-app | BIBS Product Owner | 30-Nov-2026 |
| IN-06 | EDP ingestion (SD 11) | Daily extracts with control file to an EDP landing bucket; personal data masked unless EDP is authorised (IQ15) | BDO Data Office | 16-Oct-2026 |
| IN-07 | Insurer channels | Keep e-mail with protected attachments for insurers without an interface; SFTP or API through Apigee for insurers that offer one, per insurer (IQ18) | BDOI Operations | 31-Jan-2027 |
| IN-08 | CCM transport | SMTP relay with STARTTLS on port 587 from the BIBS pods through the private route (IQ11) | BDOI IT (CCM) | 30-Nov-2026 |
| IN-09 | ECM interface | ECM receives final records of the "archive to ECM" classes through a connector consuming the ECM topic; ECM API, metadata and acknowledgement from BDOI IT (DSQ02, IQ13) | BDOI IT (ECM) | 31-Jan-2027 |
| IN-10 | Reconciliation owners | The business owner of each interface in chapter 8 signs off its reconciliation report | BRD owners | 30-Nov-2026 |

# Glossary {-}

```glossary
AFTS: Automatic Fund Transfer System
AMLC: Anti-Money Laundering Council
CARMS: System named on the drop plan, to be explained by BDOI (IQ17)
CCM: Centralized Communications Management (e-mail sending)
CMS / New BOB: Cash Management System, outward payments
ECM: Enterprise Content Management
EDP: Enterprise Data Platform
EGL: Enterprise General Ledger
HL-LOAS: Home Loan System (Loan Origination and Admin)
LFS: Loan Front-End System
LMS: Loans Management System
OBPCS: Online Bills Payment Consolidation System
PMS: PDC Management System
SCIM: System for Cross-domain Identity Management
TFS: Trade Finance System
UIDM-ISC: User ID Maintenance - Identity Security Cloud
```
