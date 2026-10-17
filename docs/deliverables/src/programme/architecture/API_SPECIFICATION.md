---
# API Specification of BIBS (BRD-00, Programme/Architecture); the API Catalogue workbook is generated with it.
# Build: python docs/deliverables/src/programme/architecture/build_architecture_pack.py
title: API Specification
subtitle: API standards, the OpenAPI description and the catalogue of the BIBS backend operations
doc_type: API Specification
doc_code: Architecture
brd: BRD-00
name: API Specification
doc_id: BIBS-ARC-05
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Architecture set, document 5 of 6
h1_page_break: false
output: Architecture/BIBS_Architecture_BRD-00_API_Specification_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Tech Lead; iorta TechNXT Integration Lead
    approver: BDOI IT Application Development and Integration (pending)
    change: First issue, with the API Catalogue v1.0 generated from the request mappings of the backend (broker-only scope of 8 October 2026)
distribution:
  - {name: "Application Development and Integration", role: Approver, organisation: BDOI IT, purpose: "API standards and catalogue"}
  - {name: "BDO API team (Apigee X)", role: Reviewer, organisation: BDO Unibank IT, purpose: "System integration interface"}
  - {name: "Information Security Office", role: Reviewer, organisation: BDOI IT, purpose: "Authentication, authorization, data scope"}
  - {name: "Enterprise Architecture", role: Reviewer, organisation: BDOI IT, purpose: "API-first standards"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Contract for the web client and the interfaces"}
---

# Introduction

## Purpose and audience

This document specifies the application programming interface of the BIBS backend: the standards every operation follows, how the OpenAPI description is obtained, and how to read the API Catalogue (Excel) that lists every operation. It is written for BDOI IT developers and integration analysts, the BDO API team that publishes the system integration operations on Apigee X, and the Information Security Office.

## Scope

All operations of the backend: the user interface operations called by the BIBS web client, the administration operations, and the system integration operations published through Apigee X. Modules of the insurer-company suite are not part of BIBS and are not listed (ADR-11). Business behaviour of each operation is specified in the FRS of its BRD; this document covers the technical contract.

## Sources

<!-- table: widths=1,7,9.6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | Request mappings, access rules and descriptions of the backend operations (generated into the API Catalogue v1.0 on 08-Oct-2026) | Catalogue and figures of this document |
| S2 | BIBS configuration reference and security controls | Tokens, single sign-on, integration tokens, limits, start-up safeguards |
| S3 | Platform cache and events architecture | Correlation identifier, events, support operations |
| S4 | Company and branch data scope architecture | Data scope checks |
| S5 | Application architecture option decision, section 5 | Apigee X path for system calls |

## Conventions

Paths are written relative to the BIBS host name without the leading slash, and path variables as :name - for example api/v1/acsl/cases/:id. Open points are numbered AP-01 to AP-06 (chapter 7).

# API overview

## Three interfaces on one backend

<!-- table: widths=3.6,3.4,4.4,6.2 caption="Interfaces of the backend" bold=first size=8.5 -->
| Interface | Path prefix | Served by | Who calls it |
|---|---|---|---|
| User interface | api/v1 | bibs-web | The BIBS web client for the signed-in user ({{api_user}} operations) |
| Administration | api/v1/admin | bibs-web | Administration screens: users, roles, data scope, parameters, caches, integration events ({{api_admin}} operations) |
| System integration | integration/v1 | bibs-integration only | BDO systems through Apigee X ({{api_integration}} operation today, the connectivity check; the system interfaces of the Integration Architecture are added under this prefix) |

The catalogue holds **{{api_total}} operations** in {{api_modules}} modules and {{api_contexts}} bounded contexts: {{api_get}} GET, {{api_post}} POST, {{api_put}} PUT and {{api_delete}} DELETE. {{api_company}} operations name a company or branch in the path or query (data scope checked); {{api_permissions}} permission codes protect them.

## Operations per bounded context

<!-- arch:context_api -->

## The API Catalogue workbook

The API Catalogue (BIBS_Architecture_BRD-00_API_Catalogue_v1.0.xlsx) is generated from the request mappings of the backend at each release, so it always matches the delivered system. Its sheets:

<!-- table: widths=3.6,14 caption="Sheets of the API Catalogue" bold=first size=8.5 -->
| Sheet | Content |
|---|---|
| API catalogue | One row per operation: bounded context, module, BRD, drop, interface, method, path, purpose (first sentence of the operation's description), permission, and whether the company or branch is named in the path or query |
| Summary by module | Operations per module by method, and those naming a company or branch |
| Standards | The API standards of chapter 5 in short |
| README | The meaning of every column |

# What every request passes through

![What every request passes through](figures/api_pipeline.dot){width=12.5}

1. **Gateway and WAF** - TLS, AWS WAF managed rules, the sign-in rate rule and the upload size rule; the integration prefix only from the Apigee ranges.
2. **Correlation identifier** - the X-Correlation-Id of the caller is kept when valid, otherwise created; it is in every log line, in the answer and in the events the request publishes.
3. **Body size** - JSON bodies up to 2 MB (413 REQUEST_TOO_LARGE above), files up to 25 MB (413 UPLOAD_TOO_LARGE).
4. **Runtime role** - bibs-web answers only the user and administration prefixes, bibs-integration only the integration prefix and its probes.
5. **Authentication** - user operations need the BIBS access token; the session is checked and the user re-loaded on every request, so a lock or deactivation applies at once. Integration operations need an Apigee X token (chapter 5.7).
6. **Permission** - every operation names its permission; 403 ACCESS_DENIED otherwise.
7. **Data scope** - the company and branch named in the path, the query or the body are checked against the user's scope; 403 DATA_SCOPE_DENIED otherwise.
8. **Validation and business rules** - bean validation of the request record (400 VALIDATION_FAILED with the field errors) and the business rules of the service (422 with the business code).
9. **Transaction** - the record, its accounting, the audit entry and any outgoing event commit together.

# OpenAPI description

BIBS publishes an OpenAPI 3 description of its operations through springdoc-openapi 2.8, with the bearer token as the security scheme.

- **Where.** The backend serves the description at the path v3/api-docs (JSON) and an interactive viewer at swagger-ui.html, relative to the backend address.
- **When.** Only on a developer machine (environment local) or with the dev profile. A start-up safeguard switches the description and the viewer off in every other environment - SIT, UAT, Pre-Prod and production - so the production surface does not advertise its operations.
- **How to obtain it.** Run the backend of the release with the dev profile on a developer workstation or the VDI (database, signing key and second-factor key from the local environment, as for any start), then download v3/api-docs from the running backend. The proposal (AP-01) is that the pipeline does the same at each release and stores the description as a file with the release record, next to the API Catalogue, so BDOI IT receives both without starting the application.
- **Title.** The description is titled "iNXT BrokerVerse API"; the delivered description for BDOI is retitled "BIBS API" with the BIBS version (AP-02).
- **System integration.** Each API published on Apigee X gets its own OpenAPI document as part of its interface specification (Integration Architecture, chapter 8); Apigee publishes it to its developer portal for the BDO teams.

# API standards

## Paths and naming

- Paths are nouns in the plural, lower-case with hyphens, under the module: api/v1/<module>/<resources>/:id. Examples: api/v1/collections/plans, api/v1/product-maintenance/advisories/:advisoryId/send.
- Business actions that change state are POST sub-resources named by the verb of the business: api/v1/catalog/records/:kind/:id/authorize, api/v1/migration/decisions/:decisionNo/return.
- Business keys may stand in for identifiers where the business uses them (api/v1/placement/gate/:arn, api/v1/ops/invoices/:invoiceNo).
- JSON field names are camelCase; codes of lists and statuses are upper-case codes; labels are resolved by the lists of values.

## Versioning

- The major version is in the path: api/v1 and integration/v1. Adding an operation, an optional field or a code value keeps the version.
- A breaking change (field removed or renamed, meaning changed, required field added) is a new version served next to the old one until every caller has moved; the old version is announced as deprecated with a date at least one release ahead (AP-04).
- The web client and the backend are released together, so the user interface moves in step; system integration versions are agreed with each counterpart.

## Methods and status codes

<!-- table: widths=2.4,6,9.2 caption="Methods" bold=first size=8.5 -->
| Method | Use | Success |
|---|---|---|
| GET | Read a record, a list, a report or a file link; never changes data | 200 with the body; file content with Content-Disposition attachment |
| POST | Create a record, upload a file, or run a business action (authorize, approve, send, book) | 200 or 201 with the resulting record; 204 when there is nothing to return |
| PUT | Update a record (with its version) | 200 with the record |
| DELETE | Remove a draft or a configuration row that may be removed; business records are cancelled or reversed by an action, never deleted | 204 |

PATCH is not used: updates replace the record with its version.

## Errors

Every error is an RFC 7807 problem document with the fields type, title, status, detail (a business text without internal references), code (stable machine code), correlationId and, where relevant, errors (field errors) or reference (unexpected errors). The code never changes; only its text may.

<!-- table: widths=1.6,4.4,11.6 caption="Error codes of the platform" bold=first size=8.5 -->
| Status | Code | When |
|---|---|---|
| 400 | VALIDATION_FAILED | Bean validation failed; errors lists each field and message |
| 400 | MALFORMED_REQUEST, BAD_REQUEST | Unreadable body or invalid argument (generic text, no internal message) |
| 401 | AUTHENTICATION_FAILED | Missing, expired or invalid token |
| 403 | ACCESS_DENIED | The user lacks the permission of the operation |
| 403 | DATA_SCOPE_DENIED | The company or branch is outside the user's data scope |
| 404 | NOT_FOUND, MODULE_NOT_IN_USE | Record or path not found; module switched off for the deployment |
| 409 | DUPLICATE | A record with the same business key exists (also a repeated source reference) |
| 409 | CONCURRENT_MODIFICATION | The record changed since it was read (optimistic locking); reload and try again |
| 413 | REQUEST_TOO_LARGE, UPLOAD_TOO_LARGE | JSON body over 2 MB; file over 25 MB |
| 422 | Business code of the rule | A business rule refused the action, for example PARAMETER_NOT_SET, NO_ACCOUNTING_RULE, PERIOD_NOT_OPEN, MAKER_CHECKER_VIOLATION |
| 429 | LOGIN_RATE_LIMITED, RESET_RATE_LIMITED | Sign-in or password reset above the limit per address, with Retry-After |
| 500 | INTERNAL_ERROR | Unexpected error; reference is also in the log line for support |
| 503 | SIGN_IN_CHECK_UNAVAILABLE | The session store cannot be read; the client retries |

## Pagination, sorting and filtering

- List operations take page (zero-based) and size, and sort=field,asc or sort=field,desc; the default size is 20 and the maximum 2,000.
- A paged answer is an envelope with content, page, size, totalElements and totalPages, independent of the framework.
- Filters are query parameters named after the fields (status, companyId, branchId, dateFrom, dateTo); companyId and branchId are checked against the data scope.
- Large exports are reports run as jobs; the answer is a link to the file in S3, not a page of thousands of rows.

## Idempotency and concurrency

- GET is safe and can be repeated.
- Creations carry a business reference (receipt number, source reference of an inbound file or request, request number) that BIBS refuses twice with 409 DUPLICATE, so a retry after a time-out never creates a second record.
- Updates carry the version of the record read; a stale version is refused with 409 CONCURRENT_MODIFICATION.
- Business actions check the current stage of the record (workflow), so repeating an action that already took effect is refused with a business code instead of acting twice.
- System integration creations carry an Idempotency-Key header (a UUID of the caller); BIBS keeps the key and its answer for 24 hours and returns the same answer to a repeat (standard for every integration API, AP-03).
- Events are delivered at least once; consumers deduplicate on the event identifier.

## Security

<!-- table: widths=3.4,14.2 caption="Security of the operations" bold=first size=8.5 -->
| Topic | Rule |
|---|---|
| User authentication | Sign-in by EIAM single sign-on (OIDC, or SAML 2.0) or, for break-glass administrators, password and TOTP; the backend then issues an access token (JWT, HS256, 15 minutes by default, 5 to 60 by parameter) sent as a bearer token, and a refresh token in an HttpOnly, SameSite=Strict cookie limited to the sign-in path, rotated on every renewal; inactivity time-out and an absolute end after 8 hours |
| Authorization | Every operation names its permission (the Permission column of the catalogue); roles are permission bundles granted by access requests under four eyes; maker-checker and authorization limits apply in the services |
| Data scope | Company and branch in path, query or body checked centrally; list operations filter on the allowed companies |
| System integration | OAuth 2.0 access token issued by Apigee X; BIBS checks signature against the Apigee key set, issuer, audience, validity (60-second tolerance), asymmetric algorithms only (RS256), and the scopes configured for the path of the API; answers 401 or 403 |
| Transport and headers | TLS 1.2 or higher; HSTS for one year; Content-Security-Policy default-src 'none' on API answers; X-Content-Type-Options nosniff; no-referrer; CORS limited to the BIBS origin |
| Limits | Sign-in 20 attempts a minute per address, password reset 10 per 15 minutes; Apigee quotas and spike arrest per product |
| Output | No stack traces or internal messages in answers; CSV exports protected against formula injection; personal data only where the permission allows it |

## Data formats

- JSON in UTF-8. Dates are ISO-8601 dates (2026-10-08); timestamps are ISO-8601 in UTC with Z; the business date is the date in the business zone (Asia/Manila).
- Amounts are decimal numbers with two decimals, never floating point in the backend; every amount comes with its currency (ISO 4217 code); foreign-currency lines carry the base-currency amount too.
- Files are uploaded as multipart form data (Excel template, CSV or TXT where the upload screen allows it) and downloaded through a link valid 5 minutes, or streamed for protected e-mail attachments and ZIP bundles.

## Tracing and support

Every answer carries X-Correlation-Id; the same identifier is in the log lines of the request and in the envelope of every integration event it published, so support can follow a request from the screen to the event archive. Unexpected errors also carry a reference that is in the log line.

# Governance of the API

<!-- table: widths=5.6,2.4,2.4,2.4,2.4,2.4 caption="API responsibilities (R responsible, A accountable, C consulted, I informed)" bold=first size=8 -->
| Activity | BDOI IT integration | BDO API team | BDOI Information Security | iorta TechNXT Tech Lead | iorta TechNXT Solution Architect |
|---|---|---|---|---|---|
| API standards (this document) | A | C | C | R | R |
| New or changed user interface operation | I | I | I | A, R | C |
| New system integration API | A | R | C | R | C |
| Apigee proxy, product, scopes, quotas | C | A, R | C | C | I |
| API Catalogue and OpenAPI file per release | I | I | I | A, R | C |
| Security review of new operations | I | I | A | R | C |

Rules for a change: a new operation names its permission and is covered by the data scope check (the compile fails otherwise); a breaking change follows the versioning rule; the catalogue and the OpenAPI file are regenerated with the release and listed in the release note.

# Decisions and open points for BDOI

<!-- table: widths=1.2,3,8.4,2.8,2.2 caption="Decisions and open points for BDOI" bold=first size=8 -->
| ID | Topic | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| AP-01 | OpenAPI file per release | The pipeline starts the release on a dev profile, exports the OpenAPI description and stores it with the release record and the API Catalogue; no environment beyond development serves it | iorta TechNXT Tech Lead; BDOI IT informed | 31-Jan-2027 |
| AP-02 | Title of the OpenAPI description | "BIBS API" with the BIBS release version and BDOI contact, instead of the platform title | iorta TechNXT | 31-Jan-2027 |
| AP-03 | Idempotency-Key on system integration | Every creation through integration/v1 carries an Idempotency-Key; BIBS keeps key and answer for 24 hours | BDOI IT integration, BDO API team | 30-Nov-2026 |
| AP-04 | Deprecation period | A deprecated system integration version stays for at least two releases or three months, whichever is longer | BDOI IT integration | 30-Nov-2026 |
| AP-05 | Apigee developer portal | Each published BIBS API with its OpenAPI document on the Apigee developer portal of BDO | BDO API team | 31-Mar-2027 |
| AP-06 | Quotas | Default Apigee quota of 600 calls a minute per client application with spike arrest of 20 a second; adjusted per interface in its specification | BDO API team | 30-Nov-2026 |

# Glossary {-}

```glossary
API: Application programming interface
JWT: JSON Web Token
OIDC: OpenID Connect
OpenAPI: Standard description format of HTTP APIs (version 3)
RFC 7807: Problem details for HTTP APIs
SAML: Security Assertion Markup Language 2.0
TOTP: Time-based one-time password
```
