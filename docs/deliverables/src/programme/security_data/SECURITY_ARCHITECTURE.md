---
# Security Architecture of BIBS (BRD-00). Build: build_secdata_pack.py --only security (expands the <!-- sd:... -->
# lines from asvs_mapping.yaml and also writes the ASVS Level 2 control mapping workbook).
title: Security Architecture
subtitle: Zero Trust, identity and access, cryptography, secure coding, threat model, security testing and privacy of BIBS
doc_type: Security Architecture
doc_code: Security
brd: BRD-00
name: Security Architecture
doc_id: BIBS-SEC-BRD-00
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Security Architecture
output: Security/BIBS_Architecture_BRD-00_Security_Architecture_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Security Architect
    reviewer: iorta TechNXT Solution Architect; Tech Lead
    approver: BDOI Information Security (pending)
    change: First issue, from the platform security controls, the deployment and configuration references, the CI pipeline, the BRD-11 non-functional requirements, the IER workbook v20 and the client decisions of 26 September and 8 October 2026
distribution:
  - {name: "BDOI Information Security", role: Approver, organisation: BDOI, purpose: "Security architecture, ASVS mapping, threat model, exception policy"}
  - {name: "BDOI Information Technology Group (BDOI IT)", role: Approver, organisation: BDOI, purpose: "Identity, network, keys, logging and SIEM integration"}
  - {name: "BDOI Data Protection Officer", role: Reviewer, organisation: BDOI, purpose: "Privacy by design (Data Privacy Act 2012)"}
  - {name: "Cloud and Digital Operations Engineering (IER owner)", role: Reviewer, organisation: BDO Unibank IT, purpose: "Cloud controls, KMS, WAF, Kubernetes"}
  - {name: "BDO API team (Apigee X)", role: Reviewer, organisation: BDO Unibank IT, purpose: "System-to-system tokens and scopes"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Security controls, pipeline, remediation"}
---

# Introduction

## Purpose and audience

This document describes how BIBS (BDOI Broker System, on iNXT BrokerVerse) protects the data and the transactions of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It states the security principles, the controls of every layer, the threat model, the security testing in the delivery pipeline, the vulnerability management and the privacy controls, and it maps the controls to the OWASP Top 10 (2021) and to OWASP ASVS 4.0.3 Level 2.

The audience is BDOI Information Security, BDOI IT, the BDOI Data Protection Officer, the BDO cloud, network and API teams and the iorta TechNXT project team. Technical terms are used.

## Scope

<!-- table: widths=3,10 caption="Scope" bold=first -->
| Item | Scope |
|---|---|
| Application | The BIBS web client (React and TypeScript, served by nginx), the backend (OpenJDK 21 LTS, Spring Boot 3.5 modular monolith) in its three runtime roles (web, jobs, integration), and its data stores |
| Platform | Managed Kubernetes on AWS (Amazon EKS, ap-southeast-1, as in the IER workbook v20), Kubernetes Gateway API, PostgreSQL 16 (Amazon RDS), Valkey 8, Apache Kafka 3.9 (Amazon MSK), Amazon S3, AWS KMS and Secrets Manager |
| Delivery | The CI pipeline (static analysis, dependency, secret and image scanning, SBOM) and the release to the environments DEV, SIT, UAT, Pre-Prod, PROD and DR |
| Interfaces | EIAM (Microsoft Entra ID) sign-in, UIDM-ISC provisioning, Apigee X for every system-to-system interface, CCM mail, bank, insurer and watchlist files |
| Out of scope | The security of the BDO systems themselves (EIAM, Apigee X, CMS, EGL, EDP), end-user devices and VDI, and the physical security of the cloud provider |

BIBS is an insurance broking system. The insurer-company modules of the platform (insurer underwriting, insurer claims, reinsurance treaty accounting, actuarial reserves, group consolidation) are being removed and are not part of this document; their permissions are already withdrawn from every role.

## Sources

<!-- table: widths=1,6,6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | BRD-11 User Access Maintenance and its FRS v2.0 (UAM-NFR-01 to 41) | Password, lockout, session, audit and four-eyes rules |
| S2 | Umbrella BRD (00 BRD BDOI Core Replacement) and the BRD non-functional sections | Regulatory requirements (BSP MORB, Circular 808, AMLA, Data Privacy Act 2012) |
| S3 | IER workbook v20 (FR-ITC-ENG007) | Environments, RPO 15 minutes, RTO 4 hours, AWS services |
| S4 | Programme Alignment v1.0 and Integration Inventory v1.0 | EIAM, UIDM-ISC, Apigee X and the other interfaces |
| S5 | Document storage decision (BDOI answers of 26 September 2026) | S3, SSE-KMS with BDOI keys, Object Lock, malware scan, ECM archive |
| S6 | Client decisions of 8 October 2026 | Component baseline: PostgreSQL 16, Valkey 8 (replaces Redis), Apache Kafka 3.9, OpenJDK 21 LTS, Spring Boot 3.5, React, the Node runtime 22 LTS (packaging tool of the web client only), nginx, managed Kubernetes, Kubernetes Gateway API (replaces the retired ingress-nginx) |
| S7 | Platform security controls, configuration reference, deployment guide, runbook and quality gates of BIBS | The controls described here |
| S8 | OWASP Top 10 (2021); OWASP ASVS 4.0.3; NIST SP 800-207 (Zero Trust Architecture) | Reference standards |

## Summary

- **Identity.** BDO users sign in with single sign-on at EIAM (OpenID Connect or SAML 2.0). BIBS links the identity to an existing user and issues its own short-lived token bound to a server-side session. Local passwords remain only for named break-glass administrators, who always need the second factor (TOTP).
- **Access.** Every request is checked for a live session, a permission (role-based) and the company and branch data scope (attribute-based), then for the record rules (maker-checker, authorisation limits, separation of duties). Privileged access changes and security parameters need a second approver.
- **Data protection.** TLS 1.2 or 1.3 on every connection; encryption at rest with KMS keys that BDOI owns; secrets in AWS Secrets Manager; insert-only audit trail protected by database triggers that the application login cannot disable.
- **Assurance.** 197 of the 259 ASVS Level 2 requirements are met, 31 are partly met with a dated action, 1 is planned, 5 are met by BDO services and 25 do not apply (chapter 8). The pipeline stops a change on a high vulnerability, a secret or a static-analysis finding. A scan of every database call found no SQL text assembled from request values (section 7.2).

# Security architecture overview

BIBS runs in six trust zones. Each arrow that crosses a zone is a trust boundary (TB1 to TB7) of the threat model in chapter 9.

<!-- landscape -->

![Trust zones and trust boundaries of BIBS](figures/sa_trust_zones.dot)

<!-- portrait -->

<!-- table: widths=1.4,4,8 caption="Trust boundaries" bold=first -->
| ID | Boundary | Controls at the boundary |
|---|---|---|
| TB1 | User - EIAM | BDO password, MFA and conditional access at EIAM; OIDC authorisation code with PKCE or signed SAML assertion to BIBS |
| TB2 | User browser - BIBS edge | BDO network or VDI only; Route 53 private zone; AWS WAF (managed rule groups, SQL injection rules, rate rule on sign-in); internal load balancer with TLS 1.3/1.2; HSTS |
| TB3 | BDO systems - BIBS (both directions) | Apigee X only; OAuth 2.0 access tokens verified by BIBS (signature against the Apigee key set, issuer, audience, scopes per API); a separate security chain that refuses user tokens; WAF admits the integration path only from Apigee source ranges |
| TB4 | Edge - application pods | Gateway API routes (users to bibs-web and bibs-frontend, systems to bibs-integration); TLS to the pods with certificates from AWS Private CA; NetworkPolicies deny by default |
| TB5 | Application - data stores | Least-privilege database login, TLS verify-full; Valkey ACL user over TLS; Kafka SASL_SSL; private subnets |
| TB6 | Application - AWS services | IAM roles of the service accounts (no stored AWS keys); private VPC connections; bucket policies deny non-TLS and unencrypted writes |
| TB7 | Delivery - runtime | Reviewed changes; pipeline gates; images pinned by digest and scanned; release by the release team from the overlays |

<!-- table: widths=3,9 caption="Security components" bold=first -->
| Component | Security function |
|---|---|
| bibs-frontend (nginx) | Serves the compiled client only; Content Security Policy, HSTS, frame denial, no server version; runs as an unprivileged user on 8443 |
| bibs-web | User APIs: authentication, permission and data scope checks, business validation, audit; no schedulers and no integration path |
| bibs-jobs | Batch jobs under a job lock; no user API (actuator only) |
| bibs-integration | Apigee-authenticated system APIs, outbox relay to Kafka, inbound files; no user API |
| Security filter chains | One chain for users (bearer token and session), one for systems (Apigee tokens); stateless, CSRF-safe |
| Data scope guard | Central company and branch check before the controller, fail closed |
| Audit service | Insert-only audit trail written in the same transaction as the change |
| File store | S3 with SSE-KMS, generated object keys, malware scan, presigned links of 5 minutes, legal hold |
| Production safeguards | Refuse to start with a missing or development secret, a plaintext connection, seed data or API documentation in production |

# Zero Trust principles applied

BIBS follows the tenets of NIST SP 800-207: no request is trusted because of where it comes from; every request is authenticated, authorised and encrypted, with the least privilege, and every decision is recorded.

<!-- table: widths=3.4,9 caption="Zero Trust principles in BIBS" bold=first -->
| Principle | How BIBS applies it |
|---|---|
| Verify explicitly | Every user request carries a token whose session is checked in the session log on each call; a token without a live session is refused. System calls carry an Apigee token checked for signature, issuer, audience and scope on each call. The internal network grants nothing |
| Least privilege | About 300 fine-grained permissions bundled into 94 active roles; company and branch data scope per user; database runtime login with row access only (no DDL, no TRUNCATE, cannot disable triggers); containers without Linux capabilities; IAM role per workload with the actions it needs |
| Assume breach | NetworkPolicies deny all traffic by default and pods never call each other; secrets only in memory; keys in KMS; insert-only audit and ledger tables; malware scan and quarantine of every file; break-glass accounts named and audited |
| Fail closed | When the token denylist (Valkey) cannot be read the session log decides; when the session log cannot be read the request is refused (status 503) and an alert is raised; rate limits fall back to the database and refuse when neither store answers; an unknown user or company is refused |
| Encrypt everywhere | TLS on every hop, including inside the cluster; encryption at rest with BDOI-owned KMS keys |
| Continuous monitoring | Audit trail, sign-in audit, access change log, security store alarms, metrics; forwarding to the BDO SIEM (open point SEC-07) |
| Device and context | Conditional access (device compliance, location, risk) is applied by EIAM at sign-in; BIBS accepts only users from the BDO network and VDI ranges at the WAF |

# Identity and access management

## Single sign-on with EIAM

<!-- table: widths=3,9 caption="Sign-in modes (security parameter AUTH_MODE)" bold=first -->
| Mode | Use in BIBS |
|---|---|
| OIDC (proposed for production) | Microsoft Entra ID as OpenID Connect provider: authorisation code with PKCE (S256), confidential client, ID token signature from the provider's key set (RS256 only), issuer, audience, expiry and nonce checked; clock tolerance 2 minutes |
| SAML | Signed Response or Assertion validated with the configured provider certificate only (never a key inside the message); one Assertion as a child of the Response; reference to the signed ID and unique IDs against signature wrapping; audience, recipient, validity and In-Response-To checked; no DTDs |
| LOCAL | Only for the break-glass administrators named in the configuration, with the second factor always asked |

The identity returned by EIAM is linked to an existing, active BIBS user only; BIBS never creates a user from a sign-in. Roles are never granted from EIAM groups; an optional group gate accepts a sign-in only when EIAM asserts a group mapped to a role the user holds. The provider's answer is exchanged for a one-time ticket of 2 minutes that the web client redeems, so no token appears in a URL.

![Single sign-on with EIAM and the BIBS session](figures/sa_sso_flow.dot){width=11}

## Provisioning with UIDM-ISC (SCIM)

UIDM-ISC, BDO's identity governance tool, provisions joiners, movers and leavers and certifies access. We propose option (a) of the Programme Alignment (open point IQ05): UIDM-ISC creates, deactivates and reactivates user accounts through a SCIM 2.0 Users interface published through Apigee X, and role (group profile) and data scope changes stay as BIBS access requests under four eyes. Every change applied from UIDM-ISC writes the access change log with source IGA and the UIDM-ISC request number, so the BRD-11 audit reports stay complete. BIBS also offers an aggregation export of accounts, roles and permissions for access certification. The SCIM interface is planned with the EIAM integration of Drop 0 and is listed under open point SEC-12.

## Second factor

- TOTP (RFC 6238, 6 digits, 30 seconds, one step of drift) with an authenticator app; a code is never accepted twice; a wrong code counts towards the lockout.
- Policy parameter MFA_POLICY: ALL, PRIVILEGED (roles of privilege level HIGH or ADMIN; delivered value) or OFF. A user who enrolled is always asked. With OIDC or SAML, EIAM's own MFA applies; BIBS can ask its own second factor as well.
- Enrolment at the first sign-in or on My Profile; ten single-use recovery codes stored as SHA-256 hashes; secrets encrypted with AES-256-GCM under a key from Secrets Manager, bound to the user.
- Remembered devices are off (MFA_REMEMBER_DEVICE_DAYS = 0).
- A lost authenticator is reset by a System Administrator and approved by a second administrator (never the requester); the reset removes the app, the codes and the devices and ends the user's sessions.

Today only the System Administrator role carries privilege level ADMIN, so PRIVILEGED covers one role. We propose privilege level HIGH for the roles that change access, money or security (open point SEC-01).

## Break-glass access

<!-- table: widths=3,9 caption="Break-glass administrators" bold=first -->
| Rule | Value |
|---|---|
| Who | Two named System Administrators of BDOI IT (proposal), listed in the deployment configuration; no shared account |
| When | EIAM or the network path to EIAM is unavailable, or a single sign-on configuration must be repaired |
| How | Local password (at least 10 characters, complexity, history of 8, maximum age 90 days) plus the BIBS second factor, always |
| Control | Every sign-in audited and alerted to BDOI Information Security; credentials kept in the BDOI privileged access vault; use reviewed within one working day; password changed after each use |

## Role-based access control

- A **permission** is one function (for example approve a disbursement voucher, maintain the chart of accounts). The catalogue holds about 300 permissions, each with its area and action class (view, create, amend, approve).
- A **role** (group profile) is a bundle of permissions; a user can hold several roles. 94 roles are active in the schema of 8 October 2026, one per persona of the BRDs (for example MKT_AO, PROCESSING_TL, CASHIER, DISB_APPROVER, FRBS_HEAD, SCR_INVESTIGATOR, UAM_APPROVER).
- Roles have a privilege level (STANDARD, HIGH, ADMIN) that drives the second factor and the second approval of changes.
- Every controller method names its permission; the web client menus follow the same permissions, so a user never sees a function the server refuses.
- The insurer-only permissions (insurer policies, claims, reinsurance, reserves, consolidation, insurer taxes) are withdrawn from every role and refused by the User Access screens.

## Data scope (attribute-based access control)

Each user has a data scope: "All companies" or a list of companies, each with all or some of its branches. The scope is checked centrally before the controller for every request that names a company or branch (query parameter, path variable or request body); lists, inboxes and pickers show only allowed companies. On 8 October 2026 all 747 request mappings that take a company are guarded, and the pipeline fails when a new one is not. A request outside the scope is refused with the code DATA_SCOPE_DENIED. A scope changes only through an approved access request or the audited emergency edit, only within the administrator's own scope, and never one's own.

Records loaded by their identifier alone carry their company but the request does not name it; adding the check on load for those requests, module by module, is open point SEC-05.

![How BIBS decides an access](figures/sa_access_model.dot){width=12}

## Second approval and segregation of duties

<!-- table: widths=4,8 caption="Four-eyes controls" bold=first -->
| Control | Rule |
|---|---|
| User access requests | The requester and the subject user never decide; the approver is chosen by the requester; a change to a high-privilege profile or submitted outside UAM_WORKING_HOURS (08:00-18:00, Monday to Friday) needs a second approver who differs from the first |
| Group profile requests | Implemented by the System Administrator, who is not the requester |
| Security parameters | A change (for example AUTH_MODE, MFA_POLICY, LOGIN_MAX_FAILED_ATTEMPTS) waits as a pending value until a holder of the approval permission other than the requester approves it |
| Second-factor reset | Requested by one administrator, approved by another |
| Master data and journals | Maker-checker on every master record; the maker never authorises his own journal or master record; administrators cannot post |
| Authorisation limits | Per user on journals, payables, disbursement vouchers and claims payments; the approval inbox hides items above the viewer's limit |
| Separation-of-duties rules | Pairs of group profiles that one user may not hold, maintained under maker-checker and checked on submission, on each bulk line and again at approval |
| Legal hold | Placing or releasing a hold on a record, and any governance bypass, is limited to the roles of the BDOI Delegation of Authority with approver and reason recorded |

## Account life cycle

- Lockout after LOGIN_MAX_FAILED_ATTEMPTS (3) consecutive failures of password or code; unlock by an administrator.
- One message for a wrong password, a locked and a deactivated account ("Invalid user name or password"); the real reason is audited.
- Dormant accounts: a system deactivation request after UAM_DORMANT_DAYS (90) without sign-in, with notice 7 days before; System Administrators are exempt and reviewed quarterly.
- Deactivation ends every open session of the user at once.
- The initial administrator exists only on an empty installation, from a secret, and must change the password at the first sign-in.

# Session management

<!-- table: widths=3.6,8.4 caption="Session controls" bold=first -->
| Control | Value |
|---|---|
| Access token | Signed (HMAC-SHA256) with issuer, expiry, token id and session id; life ACCESS_TOKEN_MINUTES = 15; kept in the browser's session storage only |
| Refresh token | 256-bit random value in an HttpOnly, Secure, SameSite=Strict cookie limited to the sign-in path; replaced on every renewal; only its SHA-256 is stored; a replaced token presented after 30 seconds ends the session (token reuse) |
| Idle time-out | Warning after SESSION_IDLE_WARNING_MINUTES = 15; sign-out after SESSION_TIMEOUT_MINUTES = 30 without activity; renewal is not activity |
| Absolute end | 8 hours after sign-in, with a warning 30 minutes before |
| Sign-out | Ends the session in the session log and the token denylist at once; clears the browser storage |
| Server-side check | Every request checks the session in the session log (cached in Valkey); fail closed when the store cannot be read |
| Concurrent sessions | Visible to administrators (users online, session log); administrators can end them |
| CSRF | User APIs authenticate by the Authorization header, which browsers never send on their own; the renewal call needs the SameSite=Strict cookie and a custom request header |

Open point SEC-03 adds a user notice after a password change or a new device, the option to sign out all other sessions after a password change, and the user's own session list on My Profile.

# Cryptography, secrets and keys

## Encryption in transit

<!-- table: widths=4,8 caption="Encryption in transit" bold=first -->
| Hop | Control |
|---|---|
| Browser - load balancer | HTTPS 443 only, certificate of the BDO domain (BDO PKI or ACM), policy TLS 1.3 / 1.2 (ELBSecurityPolicy-TLS13-1-2-2021-06); HSTS one year with sub-domains |
| Load balancer and Gateway - pods | HTTPS on 8443 with certificates from AWS Private CA through cert-manager (90 days, renewed 15 days before expiry, new key each time) |
| Backend - PostgreSQL | sslmode verify-full with the RDS CA bundle; the database forces TLS |
| Backend - Valkey 8 | TLS with an ACL user |
| Backend - Kafka 3.9 (MSK) | SASL_SSL with SCRAM-SHA-512 or IAM; plaintext listeners disabled |
| Backend - S3, KMS, STS, Secrets Manager | private VPC connections over TLS; bucket policies deny non-TLS requests |
| Backend - BDO systems | Through Apigee X over TLS; SMTP to CCM with STARTTLS |

A production start is refused when any of these connections could run in plaintext.

## Encryption at rest

<!-- table: widths=4,8 caption="Encryption at rest" bold=first -->
| Store | Control |
|---|---|
| PostgreSQL (RDS Multi-AZ) | Storage, snapshots, automated backups and the cross-region replica encrypted with a BDOI customer-managed KMS key |
| Amazon S3 (documents, reports, inbound, migration) | SSE-KMS with one BDOI customer-managed key per environment and bucket class, bucket keys on; unencrypted writes denied; Object Lock (governance) on documents |
| Valkey 8 and Kafka (MSK) | Encryption at rest with KMS; no personal data in the cache beyond the session and scope entries |
| Second-factor secrets | AES-256-GCM in the application, key from Secrets Manager, user name as additional data |
| Passwords and recovery codes | BCrypt (cost 12); recovery codes, refresh tokens and reset links as SHA-256 |
| Non-production migration data | Personal data masked with a keyed HMAC before staging; unmasked extracts never leave production |

## Secret and key management

<!-- table: widths=3.3,3.2,3.2,3.4 caption="Secrets and keys with their rotation" bold=first -->
| Secret or key | Held in | Owner | Rotation (proposal) |
|---|---|---|---|
| KMS keys (RDS, S3, Valkey, MSK) | AWS KMS, BDOI account | BDOI Information Security (key administrators) | Automatic yearly, or the BDOI policy |
| Token signing key | Secrets Manager | BDOI IT | Every 90 days; rotation re-issues tokens through the refresh cookie without signing users out |
| Second-factor encryption key | Secrets Manager | BDOI IT | Yearly; the previous key stays readable and secrets are re-encrypted at their next use |
| Migration masking key | Secrets Manager | Data Migration Lead | Per environment, at each refresh of SIT and UAT |
| Database logins (owner, application) | Secrets Manager | DBA | 90 days, automatic rotation of RDS |
| Valkey and Kafka credentials | Secrets Manager | BDOI IT | 90 days |
| OIDC client secret | Secrets Manager | BDOI IT and the EIAM team | Before expiry of the app registration secret (at most 12 months) |
| TLS certificates | ACM, AWS Private CA | BDO PKI team | 90 days in the cluster (automatic); per BDO PKI at the edge |
| Apigee signing keys | Apigee X | BDO API team | Per BDO API policy; BIBS reads the key set |

Secrets reach the pods through the namespace secret created from Secrets Manager (External Secrets Operator or the release pipeline); they are never in images, configuration files or version control. The workloads access AWS with the IAM role of their service account; no AWS key is stored anywhere. Every environment except a developer's machine refuses to start with a missing secret or a development value.

# Secure coding

## Standards

- Coding rules of the Developer Guide; static analysis on every change: SpotBugs with its security plug-in, PMD, Checkstyle, CodeQL for Java and TypeScript, SonarJS rules for the web client; compiler warnings are errors.
- Module boundaries enforced by architecture rules; security controls are shared platform services, never copied into a module.
- Every change is reviewed by a second developer before it is merged.

## Parameterised queries: scan result

All database access goes through JPA (entity queries, criteria queries) or the Spring JDBC templates with bound parameters. On 8 October 2026 we scanned every database call of the backend for SQL text assembled from values:

<!-- table: widths=5,7 caption="Database call scan of 8 October 2026" bold=first -->
| Check | Result |
|---|---|
| Database calls in the classes that use JDBC, JPA query APIs or the Java SQL package | 249 calls: 22 with a literal SQL text, 196 with a constant, 31 with a text assembled in the method |
| The 31 assembled texts | Each traced to its source: report definitions and helpers that join constant fragments (columns, joins, filters), JPA criteria queries, a fixed list of disbursement types written in the program, and the business time zone (an identifier validated as a time zone, which cannot hold a quote) |
| SQL text concatenated with any other value (scan of every string that contains SQL keywords and is joined with a non-constant) | 37 candidates; all are messages or the fragments above; none joins a value from a request, a file or the database into SQL text |
| Values of filters, codes, dates and amounts | Always bound parameters (positional or named) |
| Result | **No SQL text is assembled from external values.** The rules of the SpotBugs security plug-in and CodeQL for SQL injection stay active on every change |

## Input validation

- Every request body binds to a dedicated request object with Bean Validation (types, lengths, patterns, lists of values); server-owned fields (status, approver, time stamps) cannot be bound.
- Business rules are checked in the services and answer with a business code (status 422).
- Request size: JSON bodies at most 2 MB; uploads at most 25 MB through BIBS, larger bulk files by presigned upload to the quarantine prefix of S3.
- Uploaded Office and OpenDocument files are read within fixed limits (1,000 parts, 64 MB per part, expansion at most 100 times); CSV and TXT must be text in UTF-8.
- XML (SAML responses, OpenDocument) is parsed with DTDs and external entities disabled.

## Output encoding

- React escapes all rendered text; the web client never inserts raw HTML and loads no script from another origin (Content Security Policy).
- Error answers follow RFC 7807 with a business message; unexpected errors return a generic message and a reference for support, never a stack trace.
- CSV exports neutralise formulas; downloads carry Content-Disposition attachment and no-store.
- Log messages have their line breaks neutralised.

# OWASP Top 10 and ASVS Level 2

## OWASP Top 10 (2021)

<!-- sd:owasp_top10 -->

## ASVS 4.0.3 Level 2 control mapping

The workbook BIBS ASVS L2 Control Mapping v1.0 maps each of the 259 Level 2 requirements to the control, the evidence in code, configuration or pipeline, and the gap with its action, owner and date.

<!-- sd:asvs_summary -->

The rows with an action:

<!-- sd:asvs_gaps -->

# Threat model

## Method

STRIDE per trust boundary (chapter 2) and per main data flow. Each threat is rated by likelihood and impact (High, Medium, Low) after the mitigations in place; residual High risks are not accepted. The model is reviewed at each drop and whenever an integration or a trust boundary is added.

## Data flows

<!-- table: widths=1,4,4,3 caption="Main data flows" bold=first -->
| ID | Flow | Data | Boundaries |
|---|---|---|---|
| DF1 | Sign-in and session | Identity, tokens | TB1, TB2, TB4, TB5 |
| DF2 | Business transaction (quotation to booking, receipt, voucher) | Client personal data, premiums, bank accounts | TB2, TB4, TB5 |
| DF3 | Document upload and download | KYC documents, policies, receipts, STRs | TB2, TB4, TB6 |
| DF4 | Outbound payment and accounting files (CMS / New BOB, EGL) | Payee bank accounts, amounts, journals | TB3, TB5 |
| DF5 | Inbound files (bank, insurer, watchlist, submitted policies) | Payments, policies, sanctions lists | TB3, TB6 |
| DF6 | Integration events (Kafka outbox) and extracts (EDP) | Business events, reporting data | TB3, TB5 |
| DF7 | Release and configuration | Images, manifests, secrets | TB7, TB6 |
| DF8 | Data migration loads | Legacy records, masked outside production | TB3, TB6, TB5 |

## STRIDE analysis

<!-- table: widths=1.3,1.4,4.4,5.8,1.6 caption="STRIDE threats and mitigations" bold=first size=8.5 -->
| TB | STRIDE | Threat | Mitigations | Residual risk |
|---|---|---|---|---|
| TB1 | S | Credential theft or phishing of a BDO user | EIAM MFA and conditional access; BIBS links only active users; lockout and rate limits for break-glass accounts | Medium |
| TB1 | T | Forged or replayed ID token or SAML assertion | Signature with the provider key only, nonce, PKCE, audience, issuer, expiry, In-Response-To, signature-wrapping checks; one-time ticket of 2 minutes | Low |
| TB2 | S | Stolen access token reused | 15-minute life; session checked on each request; sign-out and deactivation end it at once; token in session storage only | Low |
| TB2 | T | Request tampering (other company, other record) | Server-side permission and data scope checks; request objects; optimistic locking | Medium (SEC-05) |
| TB2 | R | User denies an approval or a change | Audit trail in the same transaction, insert-only with triggers; access change log; maker and checker named on every record | Low |
| TB2 | I | Cross-site scripting or data exposure in the browser | React escaping, strict CSP, no raw HTML, no-store downloads, no personal data in URLs | Low |
| TB2 | D | Flooding of sign-in or exports | WAF rate rules; sign-in rate limits; background report jobs; size limits | Medium (SEC-06) |
| TB2 | E | Privilege escalation through access requests | Four eyes, second approval for high-privilege changes, nobody changes his own scope, SoD rules | Low |
| TB3 | S | A system impersonates another | Apigee OAuth tokens with scopes per API; user tokens refused; WAF admits Apigee ranges only | Low |
| TB3 | T | Altered payment file to CMS / New BOB | TLS through Apigee; voucher approved under four eyes before the file; file totals and control records; reconciliation of bank confirmations | Medium |
| TB3 | I | Interception of data in transit | TLS on every hop; private route between Apigee and the load balancer | Low |
| TB4 | E | Compromised pod reaches other services | NetworkPolicies deny by default; pods never call each other; non-root, read-only, no capabilities; IAM role per workload | Low |
| TB5 | T | Tampering with the audit trail or the ledger | Triggers refuse UPDATE, DELETE and TRUNCATE; runtime login cannot disable triggers or change the schema | Low |
| TB5 | I | SQL injection | Bound parameters only (section 7.2); WAF SQL injection rules | Low |
| TB5 | I | Database snapshot or backup leak | KMS encryption with BDOI keys; snapshot sharing restricted by IAM | Low |
| TB6 | T | Malicious file uploaded | GuardDuty Malware Protection; no download before a clean result; quarantine and alert; type and size checks | Low |
| TB6 | I | Document link shared or guessed | Random object keys; presigned links of 5 minutes after a permission check, audited; buckets block public access | Low |
| TB6 | D | Deletion of documents | Versioning, Object Lock (governance), legal hold, cross-region replication | Low |
| TB7 | T | Compromised dependency or base image | SCA and Trivy gates, digest-pinned images, pinned pipeline actions, SBOM; image signing (SEC-11) | Medium |
| TB7 | I | Secret written into version control | gitleaks over the full history on every pipeline; secrets only in Secrets Manager | Low |
| DF8 | I | Personal data exposed in non-production migration | Keyed masking before staging outside production; staging purged within 5 days of sign-off; migration bucket restricted to the migration role | Low |

# Security testing in the pipeline

![Security gates of the CI pipeline](figures/sa_pipeline.dot){width=12}

<!-- table: widths=2.4,3.8,3.6,3.2 caption="Security testing" bold=first -->
| Test | Tool and scope | Fails the pipeline on | Exceptions |
|---|---|---|---|
| SAST | CodeQL (Java and TypeScript) on every push and merge request and weekly; SpotBugs security plug-in, PMD, SonarJS in every compile | Findings of the configured rule sets | Written justification next to the rule exclusion |
| SCA | OWASP dependency-check (NVD data) for Java; npm audit for the web client; weekly update proposals | CVSS 7.0 or higher; high or critical advisory | Suppression file entries with reason and an expiry (at most 6 months) |
| Secrets | gitleaks over the full history, release archive checked against its SHA-256 | Any finding | Reviewed fingerprints with reason, reviewer and review date |
| Container | Trivy on both images (OS packages, application libraries, secrets) | High or critical vulnerability with a fix available | Trivy ignore file (YAML) entries with statement and expiry date |
| SBOM | CycloneDX for the backend and the web client runtime dependencies | Missing SBOM | - |
| DAST (plan) | OWASP ZAP baseline scan nightly against SIT from June 2027; authenticated full scan with the persona users against each UAT release from August 2027 | High findings block the UAT release | Register as for SCA |
| Penetration test | Independent test of Pre-Prod, November to December 2027, scope agreed with BDOI Information Security | Critical or high findings block go-live | Risk acceptance by BDOI Information Security only |

The dependency-check job needs the NVD access key in the pipeline secrets of each pipeline that runs it; without it the job reports that it was skipped. BDOI IT holds the key for its own pipeline (open point SEC-13).

## Penetration-test readiness checklist

<!-- table: widths=0.8,7,2.6,2 caption="Penetration-test readiness" bold=first status=Status -->
| # | Item | Owner | Status |
|---|---|---|---|
| 1 | Scope, rules of engagement, test window (November - December 2027) and contacts agreed with BDOI Information Security | BDOI Information Security | Open |
| 2 | Pre-Prod ready by 1 October 2027 with production-like configuration (TLS, WAF, Gateway, NetworkPolicies, single sign-on) | BDOI IT, iorta TechNXT | Open |
| 3 | Test users per persona (including break-glass and a user with a narrowed data scope) and test Apigee clients with scopes | iorta TechNXT | Open |
| 4 | Masked data set loaded (migration Mock 4) | Data Migration Lead | Open |
| 5 | All Partly met ASVS rows due before October 2027 closed or accepted | iorta TechNXT | Open |
| 6 | No open high or critical finding of SAST, SCA, Trivy or DAST; exception register reviewed | iorta TechNXT | Open |
| 7 | API list and the interface catalogue given to the testers | iorta TechNXT | Open |
| 8 | Monitoring and SIEM alerts active so the test also checks detection | BDOI IT | Open |
| 9 | Rollback and data restore plan for the test window | BDOI IT | Open |
| 10 | Retest window of two weeks booked before the go-live readiness review | BDOI Information Security | Open |

# Vulnerability management and exceptions

<!-- table: widths=2.4,3,3,3 caption="Remediation targets (proposal)" bold=first -->
| Severity (CVSS 3.1) | Production | Before release | Source |
|---|---|---|---|
| Critical (9.0 - 10) | Fix or mitigate within 7 days | Blocks the release | Pipeline, DAST, pen test, BDO advisories |
| High (7.0 - 8.9) | 30 days | Blocks the release | As above |
| Medium (4.0 - 6.9) | 90 days | Release allowed, ticket raised | As above |
| Low (0.1 - 3.9) | Next planned upgrade | Release allowed | As above |

**Exception policy.** A finding is fixed by upgrading the library, the base image or the package. An exception is allowed only when the vulnerable code path is not reachable or a mitigation is in place, and only with:

1. the reason why it does not apply to BIBS, in the exception file of the scanner (Trivy ignore file, dependency-check suppression file or gitleaks fingerprint list);
2. the reviewer (Tech Lead or above) and the review date;
3. an expiry of at most 6 months (the expiry date field of Trivy, the until date of dependency-check), after which the gate fails again;
4. for High and Critical in production, the approval of BDOI Information Security within 5 working days.

On 8 October 2026 the Trivy file holds two exceptions (Spring web MVC findings whose code paths BIBS does not use, no fixed 6.2 release yet; reviewed by the Tech Lead on 8 October 2026, expiry 30 November 2026), the dependency-check file holds two suppressions and the gitleaks list holds test values of the history. The exception register is reviewed monthly by the Tech Lead and quarterly with BDOI Information Security.

The 8 October 2026 baseline (Valkey 8 in place of Redis, Kafka 3.9, Kubernetes Gateway API in place of ingress-nginx) removes components that reach end of support or change licence; each component is kept within its vendor support window, and the SBOM of each release shows the versions.

# Privacy by design (Data Privacy Act 2012)

BIBS processes personal information and sensitive personal information of clients, insured persons, employee-benefit members, payees and BDOI staff. BDOI is the personal information controller; iorta TechNXT acts as personal information processor during delivery and support.

<!-- table: widths=3,9 caption="Data Privacy Act 2012 principles in BIBS" bold=first -->
| Principle (RA 10173, IRR, NPC circulars) | How BIBS supports it |
|---|---|
| Transparency | Consent and privacy notices are captured in BDOI's channels (KYC forms, portal); where BIBS must record the consent, a field and its date are kept on the client (open point PRV-01) |
| Legitimate purpose | Data collected per BRD function only; screens and uploads ask only for the fields the FRS defines |
| Proportionality and data minimisation | No health data in the employee-benefits roster; masked data outside production; reports and exports limited by permission and data scope; object keys carry no personal data |
| Security of personal data (NPC Circular 16-01) | Encryption in transit and at rest, access control, audit, malware scan, backup and DR (RPO 15 minutes, RTO 4 hours) |
| Retention and disposal | Retention rules per record type and record class with review, archive or purge; legal hold for audit and AMLA records (Data Architecture, chapter 7) |
| Rights of the data subject | Access and correction through the client record by authorised staff; erasure or blocking through the retention process with a reason; requests logged by the Compliance Officer (open point PRV-02) |
| Breach management (NPC Circular 16-03) | Security monitoring and alerts; incident runbook; notification to the NPC and data subjects within 72 hours by BDOI's Data Protection Officer, with BIBS logs and audit as evidence |
| Cross-border transfer | Data hosted in the approved AWS region ap-southeast-1 (Singapore) under BDO's cross-border basis; DR region to be approved (DSQ01) |
| Privacy impact assessment | iorta TechNXT provides the data flows, the classification and the controls of this document for BDOI's PIA (open point PRV-03) |

<!-- table: widths=2.4,4.4,5.2 caption="Data classification and controls (proposal)" bold=first -->
| Class | Examples | Controls |
|---|---|---|
| Restricted | Sensitive personal information (government IDs, health-related EB reports), STRs, payee bank accounts, credentials | Need-to-know permissions, data scope, encryption, download audit, legal hold for STRs, masked outside production |
| Confidential | Client names and contacts, policies, premiums, commissions, journals | Permissions and data scope, encryption, audit |
| Internal | Reference data, products, insurers, branches | Permissions for change; read by staff |
| Public | None held by BIBS | - |

# Logging and security monitoring

<!-- table: widths=3.2,4.6,4.2 caption="Security-relevant records" bold=first -->
| Record | Content | Protection and retention (proposal) |
|---|---|---|
| Audit trail | Every create, update, authorise, post, reverse, sign-in, report run and export with user, time, entity and summary, in the same transaction | Insert-only (database triggers); online 5 years, archive to 10 years |
| Sign-in audit | Successful and failed sign-in, lockout, sign-out, real reason of a refusal | As above |
| Access change log | Every attribute changed on a user or role: from, to, request number, approver, source (BIBS or IGA) | Insert-only; 15 years |
| Session log | Sessions with method, second factor, start, last activity, end reason | 1 year online |
| File link audit | Every presigned link issued: who, what, when, from where | 5 years |
| Legacy archive access log | Inquiries of migrated legacy records | Insert-only; 15 years |
| Application logs | Container output: time with offset, level, logger, message; line breaks neutralised; no secrets or personal data | Platform log store 1 year (BDOI policy) |
| Metrics and alerts | Prometheus metrics on the management port; alerts such as SECURITY_STORE_UNAVAILABLE, FILE_QUARANTINED, job failures | Monitoring platform |

**Monitoring use cases (proposal for the BDO SIEM, open point SEC-07):** repeated sign-in failures and lockouts; any break-glass sign-in; sign-in outside working hours by a privileged role; second approval bypass attempts; access-control refusals above a threshold per user (after SEC-06); security store outages; quarantined files; mass exports; changes of security parameters; creation of a role with privilege level ADMIN; Trivy or SCA exception expiry.

# Roles and responsibilities

<!-- table: widths=4.6,1.6,1.6,1.6,1.6,1.6 caption="RACI for security (R responsible, A accountable, C consulted, I informed)" bold=first -->
| Activity | iorta TechNXT | BDOI InfoSec | BDOI IT | BDO cloud and API teams | BDOI DPO |
|---|---|---|---|---|---|
| Security architecture and threat model | R | A | C | C | I |
| Secure coding, pipeline gates, remediation | R, A | I | I | - | - |
| EIAM, conditional access, UIDM-ISC | C | C | R, A | C | - |
| Keys (KMS) and key policies | C | A | R | R | - |
| Secrets in Secrets Manager and rotation | C | I | R, A | C | - |
| WAF, network, Gateway, Apigee X | C | C | A | R | - |
| Vulnerability exceptions (High and Critical) | R | A | C | - | - |
| Penetration test | C | R, A | C | C | I |
| SIEM use cases and monitoring | C | A | R | C | - |
| Privacy impact assessment and data subject requests | C | C | I | - | R, A |
| Security incident response | C | A | R | C | R (breach notification) |

# Decisions and open points for BDOI

<!-- table: widths=1.1,3.4,5.6,2.2,1.7 caption="Open points" bold=first size=8.5 -->
| Ref | Point | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| SEC-01 | Second factor in production and EIAM conditional access for BIBS | AUTH_MODE = OIDC with EIAM MFA for every user; MFA_POLICY = PRIVILEGED with privilege level HIGH for the roles that change access, money or security (UAM_APPROVER, UAM_SECOND_APPROVER, INFOSEC_OFFICER, BUSINESS_ADMIN, FIN_ADMIN, DISB_APPROVER, RECORDS_HOLD_APPROVER); remembered devices off | BDOI Information Security, BDOI IT | 30 Nov 2026 |
| SEC-02 | Local password rules of the break-glass accounts | Minimum 12 characters (BRD-11 asks 10); keep composition, history 8 and maximum age 90 days as BRD-11 requires (accepted deviation from ASVS 2.1.9 and 2.1.10); add a breached-password list and a secret pepper | BDOI Information Security | 31 Jan 2027 |
| SEC-03 | Account notices and session self-service | E-mail to the user after a password change or reset and after a sign-in from a new device; option to sign out other sessions after a password change; own session list on My Profile; cookie name with the __Host- prefix | iorta TechNXT | 30 Apr 2027 |
| SEC-04 | Key management standard and token signing | BDOI policy (NIST SP 800-57 based) with the rotation periods of section 6.3; token signing with a KMS asymmetric key if BDOI requires keys never in application memory | BDOI Information Security | 31 Jan 2027 |
| SEC-05 | Data scope for records opened by identifier | Company check on load in every module, delivered with Drop 1 | iorta TechNXT | 31 Mar 2027 |
| SEC-06 | Security events, rate limits and quotas | Log every access refusal with user, path and permission or company; per-user limits on exports and report runs; per-user storage quota; alerts on thresholds | iorta TechNXT | 31 Mar 2027 |
| SEC-07 | Log platform and SIEM | Forward container logs and the audit trail to the BDO SIEM; agree the use cases of chapter 13 | BDOI IT, BDOI Information Security | 31 Mar 2027 |
| SEC-08 | Certificate revocation and DNS | OCSP stapling at the edge and Apigee; BDO-owned private DNS records | BDOI IT | 31 Mar 2027 |
| SEC-09 | Data classification | Approve the four classes and controls of chapter 12 | BDOI Information Security | 31 Jan 2027 |
| SEC-10 | Content-Disposition on JSON answers | Accept: API answers are read by the BIBS client only | BDOI Information Security | 31 Jan 2027 |
| SEC-11 | Image signing | Sign images in the pipeline and verify the signature at admission to the cluster | iorta TechNXT, BDOI IT | 30 Jun 2027 |
| SEC-12 | UIDM-ISC provisioning | Option (a): SCIM 2.0 Users through Apigee X for accounts, BIBS requests for roles and data scope (IQ05) | BDOI IT | 30 Nov 2026 |
| SEC-13 | Pipeline secrets | NVD access key and the scanner settings in BDOI's pipeline, so every gate runs there as in ours | BDOI IT | 31 Dec 2026 |
| SEC-14 | Remediation targets and exception approval | Accept chapter 11 | BDOI Information Security | 31 Jan 2027 |
| PRV-01 | Consent record | BDOI states where consent is captured; BIBS stores the consent flag and date on the client when needed | BDOI Data Protection Officer | 31 Jan 2027 |
| PRV-02 | Data subject requests | Procedure for access, correction, erasure and blocking, with a request log kept by the Compliance Officer | BDOI Data Protection Officer | 31 Mar 2027 |
| PRV-03 | Privacy impact assessment | BDOI runs the PIA on this document and the Data Architecture before UAT | BDOI Data Protection Officer | 30 Jun 2027 |

# References {-}

<!-- table: widths=5,8 caption="References" bold=first -->
| Document | Use |
|---|---|
| BIBS ASVS L2 Control Mapping v1.0 (Excel) | Requirement-level mapping with evidence and gaps |
| BIBS Data Architecture v1.0 | Data classification, retention, audit trail, lineage |
| BIBS Entity Relationship Diagrams v1.0 and Data Dictionary v1.0 | Tables of the security, audit and retention records |
| Programme Alignment - Drops, Integrations and Infrastructure v1.0; Integration Inventory v1.0 | Integrations and infrastructure |
| FRS BRD-11 User Access Maintenance v2.0 | Access requests, roles, sessions, passwords |
| OWASP Top 10 (2021); OWASP ASVS 4.0.3; NIST SP 800-207; NIST SP 800-57 | Standards |
| Republic Act 10173 (Data Privacy Act of 2012), its IRR and NPC Circulars 16-01 and 16-03 | Privacy |
