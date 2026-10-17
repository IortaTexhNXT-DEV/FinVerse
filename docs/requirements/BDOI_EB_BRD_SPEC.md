# BDOI Employee Benefits (BRD-8) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse (BIBS - BDOI Broker System).

Status: **for BDOI concurrence.** Build design: [`EMPLOYEE_BENEFITS_DESIGN.md`](../architecture/EMPLOYEE_BENEFITS_DESIGN.md). The review workbook will be consolidated with the other BRDs.

File references in the "Current capability" column are relative to `backend/src/main/java/com/iortatechnxt/brokerverse/` unless they start with `frontend/`.

## 1. Source documents

Source: `docs/source-documents/Employee Benefits.pdf` (59 PDF pages). The pack holds two documents. The addendum comes first and has its own page numbers (1-17). The main BRD follows; its page numbers are 1-42, so **main page n = PDF page n + 17**. This spec cites "Add. p.n" for the addendum and "p.n" for the main BRD, with the PDF page where it helps.

| PDF pages | Document | Content |
|---|---|---|
| 1 | Employee Benefits Addendum, cover | Footnote: an e-mail sign-off is acceptable because the addendum is post-walkthrough and post-approval, clarifies and refines, and does not reset scope |
| 2 | Addendum revision log | v1.0 "February 04, 2025" (initial), 09-Feb-2026 (end-to-end workflow with insurer, marketing, processing and collection touchpoints; insurer portal / API access controls, validation and audit), 23-Feb-2026 (document management and approval workflows; NB vs Renewal reporting; volumes and growth) |
| 3-5 | Addendum: before / after process; flow chart (Add. p.4, rendered and read as an image) | Renewal lane and New Business lane; franchise approval before TOR release; "Prepare ISACOM approval if the policy will be awarded to a non-accredited provider" |
| 6 | Addendum: 13 business key capabilities | Adds "data management based on Broker on Record", "approval based on TSI / premium threshold and level of authority", "upload in agreed / defined format", "view, download, generate, print as per authorization level" |
| 7-13 | Addendum business requirements | **BRID-005.01, 005.02, 005.03** (new split of BRID-005), **BRID-022.01** (new), **BRID-025** and **BRID-026** (restated and extended); Out-of-scope: **BRID-028** |
| 14-16 | Addendum: volumes per team and target-state user matrix (tick marks read from the rendered pages) | Five EB teams; 27 capabilities x Account Officer / Client-HR / Insurer / Marketing / Processing / Collection; note on makers, conditional approvals and validators |
| 17 | Addendum approval | Prepared 10-Feb-2026; inputs 23-Feb-2026; approved by the Product Owner (Shellah Marie C. Miranda, AVP), dated 26-Feb-2026 |
| 18-19 | Main BRD cover and revision log | v1.0, 21-Oct-2025 to 14-Nov-2025 (BU, Processing and Marketing EB discussions) |
| 20-23 | Main BRD p.3-6: executive summary, objectives, before / after, flow chart (p.5, image), 12 key capabilities | "New and Renewal accounts, captive and open market"; "MIS Credit / no payment no booking on adjustment" |
| 24-52 | Main BRD p.7-35: business requirements | **BRID-001 to BRID-030** |
| 53-54 | Main BRD p.36-37: usage requirements | Users, peak, availability, RTO / RPO, retention |
| 55-57 | Main BRD p.38-40: approval sheet (57 scanned) | Marketing (6), Collections & Marketing Support (2), Processing (2), Program Manager, Product Owner, Unit Head Combank / Corbank, Head Comptrollership, Heads Retail Marketing; signed 12 to 28-Nov-2025; Corporate Processing Team Head "Not applicable for this BRD" |
| 58 | Main BRD p.41 (scanned): Role Matrix (Current) | 28 process steps x System / AO / Client-HR / Insurer / Marketing / Processing / Collection |
| 59 | Main BRD p.42 (scanned): Employee Benefits Turn-Around-Time | 20 activities with trigger, responsible unit and TAT (see section 6.3) |

**Precedence.** The addendum overrides the main BRD. BRID-025 and BRID-026 appear in both documents; they are recorded once, with the addendum text. BRID-005 stays as the umbrella row and its addendum split (005.01-005.03) gets its own rows. BRID-028 is recorded as OUT (Add. p.13).

## 2. Business summary

**What Employee Benefits (EB) is at BDOI.** The EB desk of Marketing places **group benefit programmes** for corporate clients: HMO (health maintenance), Group Life Insurance (GLI) and Group Personal Accident (GPA) are the lines named in the BRD (BRID-013). The client is an employer (its HR department is the "Client / HR" persona). The risk is a **membership roster** (the master list) rather than a vehicle or a property. Business comes from five teams (Add. p.14): BDO (captive bank group accounts), SM (SM group accounts), Voluntary, Solicited and New Business. Renewals dominate: about 4,150 of the 4,200 transactions a year are renewals of existing programmes (Team 5 New Business has 50).

**The cycle the BRD asks for (Add. p.4, p.5).**
- **Renewal lane.** The system sends a renewal advice before expiry (BRID-001). The AO requests the BOR, master list and utilization from the client, and secures the incumbent's indicative renewal proposal (BRID-003). The AO collects the client's feedback (BRID-002). The BOR goes to the insurers for **franchise** approval (BRID-026 / 027). The AO prepares a Terms of Reference (TOR) and releases TOR, master list and utilization to the insurers with approved franchise (BRID-009), or the client stays with the incumbent and no remarketing is needed.
- **New Business lane.** The AO captures and tags the new client (BRID-006), requests the signed BOR, master list and utilization (BRID-007 / 008), sends the BOR for franchise approval, prepares and releases the TOR.
- **Common tail.** Insurers submit proposals (through a portal, BRID-005). The system builds a comparative analysis on premium, benefits and capabilities such as company stability, clinic providers, hospitals and technology (BRID-010), with approval above a value threshold (BRID-016). The AO presents it to the client (BRID-011). Client revisions are relayed to the insurers, who update their proposals (BRID-012 / 015). The client confirms, and placement is triggered (BRID-017). Processing issues the policy (BRID-018 / 019) and books it with a duplicate check on the billing number (BRID-020). Collection manages SOA, billing and payment tracking (BRID-021). Member movements (new hires, deletions) are handled as EB changes with insurer direct billing (BRID-013 / 025). Contracts, HMO cards and billing are monitored per member or endorsement with automatic follow-ups (BRID-030). Reports cover production, renewal, placement, New Business and TAT (BRID-022 / 022.01).

**Who does what (Add. p.16 note).** The Marketing AO is the maker of almost every EB action. Approvals are conditional: the client approves proposals and placement, the insurer approves franchise, proposals and issuance, and BDOI Management approves above the value threshold. Processing and Collections validate and execute; they are not business approvers.

**Position in BIBS.** EB reuses the BRD-1 spine for everything after the client's decision: the chosen proposal becomes an **account** (ARN), then placement, issuance and booking (BRD-1), the invoice ledger, cashiering, collections and commission (BRD-2 / BRD-4). What is new is the **EB marketing cycle before the account** (renewal advice, BOR, franchise, TOR, insurer proposals, comparative, client revisions), the **member roster and member movements**, the **item monitoring** of contracts, cards and billing, and the **external portal** through which insurers and client HR users interact without touching the core system. The portal is the largest single item and is a platform capability that later BRDs can reuse.

## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 3 |
| CONFIGURE | Set-up only | 0 |
| CHANGE | Extends an existing capability | 13 |
| NEW | New build | 17 |
| OUT | Out of scope per the BRD | 1 |
| **Total** | | **34** |

### Rows per section and fit

| Section | Name | Rows | FIT | CONFIGURE | CHANGE | NEW | OUT | Size S/M/L |
|---|---|---|---|---|---|---|---|---|
| A | Renewal initiation, client feedback, incumbent proposal, remarketing | 4 | 0 | 0 | 1 | 3 | 0 | 1/3/0 |
| B | Insurer and client portals | 5 | 0 | 0 | 0 | 5 | 0 | 0/2/3 |
| C | Client capture, documents, BOR, TOR distribution, franchise | 8 | 1 | 0 | 3 | 3 | 1 | 2/5/0 |
| D | Proposals, comparative analysis, approvals and client decision | 6 | 0 | 0 | 3 | 3 | 0 | 0/6/0 |
| E | Processing, booking, billing and member servicing | 7 | 1 | 0 | 4 | 2 | 0 | 2/4/1 |
| F | Reports, access and audit | 4 | 1 | 0 | 2 | 1 | 0 | 2/2/0 |
| | **Total** | **34** | **3** | **0** | **13** | **17** | **1** | **7/22/4** |

### Target modules

| Module | Rows | Note |
|---|---|---|
| `eb` (new) | 20 | EB programmes, cycles, documents register, BOR, franchise, TOR, insurer requests and proposals, comparative, threshold approval, client decision, member roster and movements, tracked items, SOA intake, EB reports and jobs |
| `portal` (new, platform) | 7 | External identities (insurer, client HR), a separate security realm and API, scoped views, staged uploads with validation, structured proposal forms |
| `crm` | 1 | Prospect / client capture (FIT) |
| `booking` | 1 | Insurer billing number with duplicate check |
| `account` + `booking` + `nbreport` | 1 | Business Type (NB / Renewal) on the account, carried to the invoice, filter on reports |
| platform (`workflow`, `attachment`, `security`) | 3 | Assignment and returns (FIT); department-restricted document access; authorised report access (FIT) |
| OUT | 1 | BRID-028 |

### Platform impact

| Area | Treatment | Impact |
|---|---|---|
| Security | Extend | New **external realm** for portal users (separate token audience; portal tokens are refused by `/api/v1/**`), EB permissions and roles |
| Attachments | Extend | Document **access classes** by department (Marketing / Processing / Collection) per document type (BRID-025); process tag on the link (placement, renewal, endorsement, adjustment, franchise) |
| Messaging | Extend | DOCX protection next to PDF / XLSX (BRID-007); insurer and client notifications (e-mail and portal inbox) |
| Reports | Extend | Word (DOCX) export (BRID-022.01); Business Type filter on NB reports |
| Workflow / approval | Re-use | Four workflows (EB cycle, franchise, member change, portal upload review); threshold approvals in My Approvals |
| Bulk | Re-use | Master list and utilization mapping from Excel (BRID-014 "automated mapping") |
| Accounting | Re-use | No new event: EB premium and commission post through `BROKER_BOOKING` and the booking endorsement path |
| BRD-1 spine | Re-use | Account, placement, issuance and booking after the client's decision |

## 4. EB flow in BIBS (for concurrence)

| Step | Owner | What happens | BRD |
|---|---|---|---|
| Programme and cycle | AO | An EB programme per client and benefit line; one cycle per policy year, typed NB or RENEWAL | BRID-006, 022.01 |
| Renewal advice | System | Job sends the RA at the lead time before expiry, with reminders and a feedback request; only programmes flagged for renewal | BRID-001 |
| Requirements and feedback | AO, Client HR | Master list, utilization, client feedback uploaded (portal or AO); incumbent indicative proposal sent to the client | BRID-002, 003, 007, 014 |
| BOR | AO, Client | Signed BOR uploaded and validated; proposal requests to insurers blocked without it (NB and remarketing) | BRID-008 |
| Franchise | AO, Insurer | BOR and documents to each target insurer; insurer approves or rejects in the portal within TAT; client advised | BRID-026, 027, 029 |
| TOR and RFP | AO | TOR prepared (structured items) and released with the unnamed master list and utilization to insurers with approved franchise; password protected | BRID-004, 007, 009 |
| Proposals | Insurer | Proposals entered through the structured portal form or uploaded; validated by the AO before they count | BRID-005.01-005.03, 015 |
| Comparative | System, AO, Management | Comparative analysis; value-threshold approval; authorised signatories | BRID-010, 016 |
| Client decision | AO, Client HR | Comparative presented (portal + e-mail); revisions relayed to insurers; client confirms the chosen proposal | BRID-011, 012, 015, 017 |
| Placement to booking | Processing | Account(s) created from the chosen proposal; placement, issuance and booking of BRD-1, with the billing number duplicate check | BRID-017-020 |
| Billing and payment | Collection | Insurer SOA uploaded and validated, released to the client; payment status from the invoice ledger | BRID-021 |
| Member servicing | AO, Processing, Insurer | Member movements (additions, deletions, changes) relayed to insurer; direct billing documents; contracts, HMO cards and billing monitored with auto follow-ups | BRID-013, 025, 030 |
| Reports | System / users | Production, Renewal, Placement, NB, TAT, pending items; NB / Renewal filter | BRID-022, 022.01 |

## 5. Requirements and fit/gap

### A. Renewal initiation, client feedback, incumbent proposal, remarketing

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRID-001<br><sub>p.7 (PDF 24)</sub> | System, User | Send the renewal advice (RA) automatically before expiry, for existing policies being renewed | RA sent a defined number of days before expiry (e.g. 180); only policies flagged for renewal; none for new, cancelled or not-eligible policies; includes key details; to the correct client contacts. Negative: RA to an ineligible policy, wrong client, late RA | **NEW** | No renewal processing in BIBS (BRD-1 OOS-1; UX-2). Building blocks: `system/service/ManagedJob.java`, `docgen/service/DocumentComposer.java`, `messaging/service/MessageService.java` (protected e-mail, send log) | Job `EB_RENEWAL_ADVICE` (daily) selects cycles of programmes flagged `renewalEligible` whose expiry is `EB_RA_LEAD_DAYS` away (parameter; default 135 per the TAT annex, BRD example 180), composes the RA from template `EB_RENEWAL_ADVICE`, sends it to the programme's HR contacts, stores it as document type `RENEWAL_ADVICE` linked to the programme, account and client (so CSF can resend it), and schedules reminders `EB_RA_REMINDER_DAYS` with a feedback request link to the client portal | `eb` | M | EBQ02, EBQ03, EBQ28 |
| BRID-002<br><sub>p.8 (PDF 25)</sub> | User | Collect client feedback and upload it to the system | Enter or attach feedback (text, PDF, Word, image); linked to the correct client and policy / transaction; confirmation message. Negative: no client, unsupported format, empty feedback | **NEW** | Attachments with document types and multi-file upload (`attachment/service/AttachmentService.java`, `DocumentService.java`); no feedback record | `eb_feedback` (cycle, channel AO / PORTAL / E-MAIL, text, attachment links, received date); non-empty validation (`EB_FEEDBACK_EMPTY`); also captured by client HR in the portal | `eb` | S | EBQ14 |
| BRID-003<br><sub>p.8-9 (PDF 25-26)</sub> | User, Client | Send an initial proposal based on the incumbent's rates (renewal only) | Only for existing policies flagged for renewal; latest incumbent rates and coverage; contains policy no., coverage, incumbent rates, expiry and renewal instructions; to the correct client contacts | **NEW** | Quotation letter and protected send exist for package quotations (`quotation/service`, templates QUOTATION_LETTER / QUOTATION_TERMS); nothing for EB renewals | Proposal of kind `INCUMBENT_INDICATIVE` on the renewal cycle (entered by the AO or submitted by the incumbent in the portal); PDF from template `EB_INDICATIVE_PROPOSAL`; send action refused unless the cycle is RENEWAL and the programme is flagged (`EB_NOT_RENEWAL`) | `eb` | M | EBQ04 |
| BRID-004<br><sub>p.9-10 (PDF 26-27)</sub> | User | Remarket to other insurers for better options | Only for policies flagged for renewal; triggered from the UI; requests with policy and client details to the selected insurers; incoming proposals collected; comparative generated | **CHANGE** | Non-package PRF sends a quotation slip to several panel insurers and records one response per insurer (`nonpackage/service/QuotationSlipService.java`, `InsurerResponseService.java`), insurer panel in `catalog` | EB remarketing = an insurer request (`eb_insurer_request`) per selected panel insurer from the TOR (section C), same pattern as the QS: protected e-mail plus a portal task; blocked without a validated BOR and without an approved franchise for that insurer; eligibility check as BRID-003 | `eb` | M | EBQ05, EBQ07 |

### B. Insurer and client portals

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRID-005<br><sub>p.10-11 (PDF 27-28)</sub> | System, Insurer, User | Insurers interact only through a secure portal or integration (API), never the core system; upload, view and manage records assigned to them, limited by role and scope | Portal or API only; authentication and RBAC; PDF / Word / Excel uploads; linked to client / policy / transaction; confirmation and error notices; audit trail; uploads not effective before validation and approval; no data outside the insurer's scope | **NEW** | Internal JWT login only (`security/service/JwtTokenService.java`, `config/SecurityConfig.java`); all users are internal `AppUser`s with roles; no external party access | New platform module `portal`: external users `ptl_user` bound to one party (insurer party code or client code) and a portal role; a second security filter chain on `/api/portal/**` with its own token audience (`portal`); core `/api/v1/**` refuses portal tokens; every query is scoped by the bound party; every portal action is audited. Deployable as a separate container profile (`portal`) that exposes only portal endpoints (DMZ) | `portal` | L | EBQ13 |
| BRID-005.01<br><sub>Add. p.7-8</sub> | System | Secure portal / API with enforced authentication; insurers upload proposals, policy forms and billing documents; automatic linking to the adjustment, employee, client, policy or transaction; validation and approval before effect; logged; confirmation / error notices | Access only through portal / API; RBAC; PDF / Word / Excel and agreed formats; correct linking; success and error alerts; audit of all insurer actions; nothing takes effect before approval. Negative: direct core access, unsupported format, unlinked document, bypassed approval, missing audit | **NEW** | As BRID-005; attachment type and signature checks (`attachment/domain/AllowedFileType.java`), virus-scan port (`attachment/service/VirusScanner.java`, no-op today) | Portal uploads are **staged** (`ptl_upload`: party, target task, document type, file, status RECEIVED / VALIDATED / REJECTED); a task defines the target (insurer request, franchise request, member change, SOA request), so linking is automatic; the staged file becomes a real attachment only when an internal user validates it (BRID-005.03). Insurer API for system-to-system upload: same endpoints with OAuth2 client credentials, **parked** until an insurer asks for it (the portal satisfies "portal / API") | `portal` | L | EBQ13 |
| BRID-005.02<br><sub>Add. p.8-9</sub> | Insurer | Insurer logs in only through portal / API; uploads proposals, policy forms and billing; submits proposals through structured web forms aligned to TOR items; works within assigned scope | Authentication; supported formats; uploads assigned to the right client / transaction; validation; nothing outside the permission set | **NEW** | Insurer responses are keyed in by TSU (`nonpackage/service/InsurerResponseService.java`) | Insurer task inbox (open requests, franchise requests, member changes, SOA requests); **structured proposal form** generated from the TOR items of the request (`eb_tor_item`: benefit line, plan, item, requirement) with the insurer's offered value per item, premium per plan, exclusions and capabilities; attachments on the same submission | `portal` | M | EBQ08 |
| BRID-005.03<br><sub>Add. p.9-10</sub> | User | Review insurer-uploaded documents, validate and approve | Users review uploads, complete validation, receive notices for uploads and errors, see the audit trail, ensure correct mapping | **NEW** | Workflow queues and notifications (`workflow/service/WorkQueueService.java`, `messaging/service/NotificationService.java`) | Workflow `EB_PORTAL_REVIEW` (RECEIVED -> VALIDATED / REJECTED with reason, re-map to another target before validation); queue tile "Portal uploads to review" for the owner (AO for proposals and franchise, Processing for policy forms, Collection for billing); in-app and e-mail notice on arrival; portal user notified of the outcome | `portal` | M | EBQ13 |
| BRID-014<br><sub>p.20-22 (PDF 37-39)</sub> | System, Client, User | Client (HR) accesses only through a secure portal / integration: uploads master lists and utilization reports, views policy details; access limited to viewing and uploading | Portal only; RBAC; supported formats; linked to the client record / policy / transaction; confirmations; audit of uploads **and downloads**; validation and endorsement before affecting policy status; no data outside scope; **automated mapping of uploaded data (e.g. Excel) to system fields** | **NEW** | Bulk framework parses XLSX / CSV / ODS with template and row validation (`bulk/service/BulkImportHandler.java`); no external access | Client HR portal role scoped to one client code: programme and policy summary, member roster (read), documents, upload of master list / utilization / feedback / member changes; master list uploads go through the bulk handler `EB_MASTERLIST` (template, column mapping, row validation) into a **staged roster version** that the AO validates before it replaces the roster (BRID-013); every download is logged | `portal` | L | EBQ13, EBQ14, EBQ15 |

### C. Client capture, documents, BOR, TOR distribution, franchise

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRID-006<br><sub>p.11-12 (PDF 28-29)</sub> | User | Capture and tag new clients as prospect or account (new clients only) | Client record with required fields; tagged prospect or account by the onboarding workflow; search by tag; no duplicate client created | **FIT** | Prospect creation, KYC onboarding to confirmed client, hard / soft duplicate keys, search by status (`crm/service/ClientOnboardingService.java`, `DuplicateCheckService.java`, `ClientSearchService.java`; workflow `NB_CLIENT`) | Re-use. "Prospect" = crm PROSPECT, "account" = CONFIRMED client. The EB programme is opened from the client page ("New EB Programme") | `crm` | S | - |
| BRID-007<br><sub>p.12-13 (PDF 29-30)</sub> | User | Upload TOR, master list and utilization report; files sent out encrypted and password protected (standard or generated password, sent separately); single or multiple files, individual or bulk; notify the incumbent insurer to upload the (unnamed) master list | Word / Excel / PDF; linked to client, policy and renewal transaction; confirmation; password in a separate message; every selected file protected; documented password convention; notification to the incumbent | **CHANGE** | Multi-file upload, document types, links (`attachment`); protected e-mail with separate password e-mail (`messaging/service/MessageService.java`, `DocumentProtector.java`: **PDF and XLSX only**), pluggable password policy (`DocumentPasswordPolicy.java`, BDOI convention parked Q07) | EB document register `eb_document` (cycle, type TOR / MASTERLIST / UNNAMED_MASTERLIST / UTILIZATION / ..., version, source, status); **DOCX protection** added to `DocumentProtector`; files that cannot be protected (CSV, images) are refused for outbound sending (`DOCUMENT_NOT_PROTECTABLE`); incumbent notified through a portal task "Upload unnamed master list" plus e-mail | `eb` | M | EBQ09, EBQ15 |
| BRID-008<br><sub>p.14-15 (PDF 31-32)</sub> | User | Upload the signed Broker on Record (BOR); validate signed and not empty before proposal requests proceed | Status Pending / Uploaded / Validated; re-upload when invalid; audit (time, user, client); error on invalid format or missing signature; view / download by authorised users; version control (latest active); PDF / Word; linked to the client and NB transaction; proposal requests blocked until a valid BOR exists. Negative: BOR for a renewal, unsigned / empty BOR, proposal without BOR for NB | **NEW** | Attachments are versionless; no BOR concept | `eb_bor` (programme, cycle, version, file, status PENDING / UPLOADED / VALIDATED / REJECTED, validated by, validity dates); the "signed" check is a validator attestation with a checklist (automatic signature detection is not reliable; e-signature verification parked); gate `EB_BOR_REQUIRED` on insurer requests and franchise requests; re-upload creates version n+1 and supersedes the previous one | `eb` | M | EBQ05, EBQ06 |
| BRID-009<br><sub>p.15-16 (PDF 32-33)</sub> | User | Distribute TOR, unnamed master list and utilization report to insurers | Select insurers and distribute via the agreed channel; confirmation of distribution; insurer notified (e-mail, alert) and can download; insurers upload proposals linked to the client and transaction; all actions logged. Negative: wrong insurer, proposal without TOR, wrong utilization report, incomplete audit | **CHANGE** | Quotation-slip e-mail to several insurers with protected attachments and send log (`nonpackage/service/QuotationSlipService.java`) | `eb_insurer_request` per insurer: TOR version, documents, due date (`EB_PROPOSAL_REPLY_DAYS`), channel (PORTAL task + protected e-mail); only insurers with an APPROVED franchise for the cycle (addendum flow); a proposal can only be submitted against an open request (no request, no upload); distribution and downloads logged per insurer | `eb` | M | EBQ07, EBQ08 |
| BRID-026 (Addendum)<br><sub>Add. p.12-13; main p.32</sub> | User | Submit all necessary documents to the insurer for NB placement, renewal placement, adjustment / endorsement (financial and non-financial), franchise request (accreditation), and any insurer-required documentation (BOR, TOR, master list, utilization, indicative proposal, placement and adjustment requirements) | Required documents per process type present before submission (validated); transmitted to the correct insurer or its portal; Processing and Collections notified and can view; only authorised users submit / view; insurers see only their documents. Negative: missing documents, wrong insurer, no notification, unauthorised user, insurer cannot open, unlinked documents | **CHANGE** | Required documents per product for account submission (`catalog/service/ProductRuleService.java`, `cat_document_rule`); protected e-mail to insurer placement addresses | `eb_submission` (process type NB_PLACEMENT / RENEWAL_PLACEMENT / ENDORSEMENT / FRANCHISE / PROPOSAL, insurer, document set, sent at, acknowledgement); checklist per process type from `eb_required_document` (process type x benefit line x document type), `EB_SUBMISSION_INCOMPLETE` when a mandatory type is missing; transmitted as a portal task and protected e-mail; Processing / Collection notified by permission | `eb` | M | EBQ07, EBQ26 |
| BRID-027<br><sub>p.32-33 (PDF 49-50)</sub> | Insurer | Insurer approves or rejects the franchise and BDOI Marketing is notified | Outcome communicated within the agreed working days after receipt. Negative: no response within TAT, approval without evaluation | **NEW** | - | Workflow `EB_FRANCHISE` (DRAFT -> SUBMITTED -> APPROVED / REJECTED / EXPIRED); the insurer decides in the portal (or the AO records an e-mailed outcome with evidence); SLA `EB_FRANCHISE_TAT_DAYS` (5 working days, TAT annex) with alert `EB_FRANCHISE_OVERDUE`; the AO is notified | `eb` | M | EBQ07 |
| BRID-028<br><sub>Add. p.13; main p.33</sub> | Insurer | Present required documents for high-risk accounts (ISO Franchise Form, BOR / SOA, master list, TOR, utilization report) | - | **OUT** | - | Out of scope per the addendum: high-risk accounts do not apply to Employee Benefits. It stays in the main BRD and in the current role matrix (p.41) | - | - | EBQ26 |
| BRID-029<br><sub>p.33-34 (PDF 50-51)</sub> | User | Advise the client of the franchise approval or rejection | Clear communication within the agreed TAT. Negative: client not informed | **NEW** | Protected e-mail and templates exist | Action "Advise client" on the franchise outcome (template `EB_FRANCHISE_ADVICE`, e-mail and portal notice); SLA from the outcome date (`EB_FRANCHISE_ADVICE_DAYS`) with alert | `eb` | S | EBQ07 |

### D. Proposals, comparative analysis, approvals and client decision

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRID-010<br><sub>p.16-17 (PDF 33-34)</sub> | User, System | Generate the comparative analysis of renewal proposals | All proposals of the client and renewal transaction; within the defined timeline (e.g. three days); compares coverage, premium rates, exclusions, terms, additional benefits (flow: capabilities such as company stability, clinic providers, hospitals, technology); structured table; linked to client, transaction and proposals; completeness check; **approval workflow with authorised signatories**; viewable, extractable, printable | **CHANGE** | `nonpackage/service/ComparativeTable.java` (received terms, cheapest first, lowest premium and recommended flags, PDF / XLSX); Product Maintenance comparative outputs | `eb_comparative` (snapshot version of the VALIDATED proposals, factor matrix from TOR items + premium per plan + capability factors from LOV `EB_CAPABILITY_FACTOR`, recommended proposal, due date `EB_COMPARATIVE_DAYS` from the last proposal); completeness check against the open requests (`EB_PROPOSALS_OUTSTANDING` unless closed explicitly); approval stage with signatories (permission `EB_COMPARATIVE_APPROVE`, four eyes) and the value-threshold approval of BRID-016; PDF / XLSX | `eb` | M | EBQ10 |
| BRID-011<br><sub>p.18 (PDF 35)</sub> | User, Client | Present the comparative report to the client | Report available to the client; complete; structured (table, summary, downloadable PDF); client notified; **client can give feedback or ask for clarification from the report interface** | **NEW** | - | "Present to client": publishes the approved comparative to the client portal and sends it protected by e-mail; the client HR user comments or asks for clarification on the portal page (thread `eb_comment`), which notifies the AO | `portal` | M | EBQ12, EBQ13 |
| BRID-012<br><sub>p.19-20 (PDF 36-37)</sub> | User, Client | Capture client changes / additional / amendment requests and relay them to insurers | Entered or uploaded (comment, form, document); linked to client, policy and renewal transaction; relayed to selected insurers; confirmation; not empty and meeting minimum content | **NEW** | - | `eb_revision_request` (cycle, items: TOR item or free text, requested change; documents; target insurers); relay = portal task per insurer plus e-mail; validation `EB_REVISION_EMPTY`; status per insurer (OPEN / ANSWERED) | `eb` | M | - |
| BRID-015<br><sub>p.22-23 (PDF 39-40)</sub> | User, Insurer | Insurers update their proposals based on client changes | Changes relayed reliably; the insurer updates its proposal referencing the specific request; linked to client, policy and request; system checks that the update addresses the request; user notified; version history kept | **NEW** | Response revisions with history in non-package (`npk_insurer_response_history`) | Revised proposal = new version of `eb_proposal` with `revisionRequestId`; the portal form shows the requested items and requires an answer per item (`EB_REVISION_NOT_ADDRESSED`); AO notified; version history and diff on the proposal page | `portal` | M | - |
| BRID-016<br><sub>p.23 (PDF 40)</sub> | System | Trigger approval when a transaction meets or exceeds a defined value threshold (TSI or premium) | Threshold detected (e.g. TSI >= 500M, premium >= 20M); workflow started; approvers notified; only authorised personnel approve; process blocked until approved; **threshold configurable by administrators** | **CHANGE** | Workflow engine, My Approvals (`approval/service/PendingApprovalSource.java`), user authorization limit (`security/domain/AppUser.java` `authorizationLimit`); TSU TSI rules in `catalog` (`cat_tsu_rule`) | `eb_threshold_rule` (benefit line or all, measure TSI / ANNUAL_PREMIUM, operator >=, amount, currency, approver permission, level, effective dates; maker-checker); evaluated on the comparative (recommended proposal) and again on client confirmation; stage `THRESHOLD_APPROVAL` in `EB_CYCLE`; `EbApprovalSource` feeds My Approvals; placement trigger blocked (`EB_THRESHOLD_APPROVAL_PENDING`) | `eb` | M | EBQ11 |
| BRID-017<br><sub>p.24 (PDF 41)</sub> | User | Trigger placement after the client's confirmation | Explicit client confirmation captured (system, e-mail or signed document); placement workflow starts automatically for the chosen insurer and coverage; Processing and insurer notified; required documents linked to the placement. Negative: no valid confirmation, wrong proposal or insurer, missing documents | **CHANGE** | Quotation / PRF acceptance with the client's acceptance e-mail and `create_accounts` through `account/service/AccountService.java` `createDraft`; placement workflow `NB_ACCOUNT` | `eb_client_confirmation` (chosen proposal, channel SYSTEM / EMAIL / SIGNED_DOCUMENT, evidence mandatory unless SYSTEM); "Trigger placement" creates one draft account per benefit line of the chosen proposal (product = EB product of the line, insurer, premium, business type), links the placement documents (confirmation, proposal, TOR, master list, BOR) to the account, submits it and notifies Processing; blocked while a threshold approval or placement requirement is missing | `eb` | M | EBQ12, EBQ24 |

### E. Processing, booking, billing and member servicing

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRID-013<br><sub>p.20 (PDF 37)</sub> | User | Entry and upload of employee benefit changes / additional / amendment requests (e.g. new hires for HMO, GPA, GLI) | Captured, validated, linked to the policy / transaction and relayed to the insurer. Negative: incomplete, unlinked, insurer not reached | **NEW** | Endorsement requests on booked invoices (`adjustment/service/EndorsementRequestService.java`); no member roster | Member roster `eb_member` (per programme and policy year: employee no., name, birth date, gender, civil status, plan, dependants, effective / end dates, status) loaded from the validated master list; `eb_member_change` (ADD / DELETE / CHANGE_PLAN / CHANGE_DATA lines, effective date) entered on screen or uploaded (bulk `EB_MEMBER_CHANGE`) by the AO or client HR; workflow `EB_MEMBER_CHANGE` (captured -> relayed to insurer -> insurer billed / confirmed -> Processing validated -> closed); financial changes raise an Operations endorsement request (source `EB`) for the premium effect | `eb` | L | EBQ15, EBQ16, EBQ17 |
| BRID-018<br><sub>p.25 (PDF 42)</sub> | User | Assign, reassign, return and give remarks | Only authorised users; remarks captured and visible; returns with feedback | **FIT** | `workflow/service/WorkAssignmentService.java` (assign / claim / re-assign with `WORK_ASSIGN`), generic return with reason and comment, case history (`WorkCaseHistory`), `WorkflowPanel` | Re-use on the EB workflows; Processing Supervisor approval "if applicable" is a stage only where BDOI defines one (EBQ23) | platform (`workflow`) | S | EBQ23 |
| BRID-019<br><sub>p.25-26 (PDF 42-43)</sub> | User | Complete policy issuance after a valid placement confirmation, with all documents received and activities tracked until completion | Information and documents flow to the next process; notification with details and documents; restricted access; activities tracked until issuance | **CHANGE** | Placement workbench, slips, insurer returns (`placement`), e-policy receipt and policy number (`issuance`), account status history (`NB_ACCOUNT`) | EB documents linked to the account at trigger (BRID-017) appear in the account's Documents tab; Processing is notified by the account submission; the EB cycle mirrors the account stages (PLACED, POLICY_ISSUED, BOOKED) through `AccountStatusChanged` so the AO sees progress; EB contracts are a tracked item (BRID-030) | `eb` | S | - |
| BRID-020<br><sub>p.26-27 (PDF 43-44)</sub> | User | Submit booking with a duplicate check on billing number, client and policy | Booking submission; required details; duplicate billing number blocks and notifies; audit (user, time, billing number); only authorised users. Negative: billing number already exists, missing fields | **CHANGE** | `booking/service/BookingService.java` (idempotent on ARN + transaction number), booking queue and workbench; no insurer billing number | `bkg_invoice.insurer_billing_no` and a unique index per company + insurer + billing number (`BILLING_NO_DUPLICATE`); mandatory for EB products (`cat_product` flag `billing_no_required`), entered on "Book account" and in `BOOKING_UPLOAD`; notification on success and on duplicate; the booking audit already records user and time | `booking` | M | EBQ18 |
| BRID-021<br><sub>p.27-28 (PDF 44-45)</sub> | User | Manage financial transactions, billing and payment tracking after placement | SOA received from the insurer (PDF, Excel; manual or automated upload); SOA released to the client after billing validation; payment status updated and visible to all teams; notification after payment confirmation with document links; restricted access; tracked until paid. Negative: invalid / corrupted / duplicate SOA, payment not updated, no notification | **CHANGE** | Invoice ledger with payment status per invoice (`opsledger/domain/PaymentStatus.java`, `opsledger/service/Invoice360Service.java`); Cashiering application; Collections worklist and billing statements (BRD-4, being built) | `eb_soa` (insurer, programme, SOA no., period, amount, file; duplicate block on insurer + SOA no. and file hash); workflow RECEIVED -> VALIDATED (Processing) -> RELEASED (to client HR via portal + e-mail, and to Collection); linked to the booked invoice(s); the payment status shown is the invoice ledger's (no second copy); event listener on `InvoiceMovementPosted` notifies AO and Collection when the invoice becomes PAID | `eb` | M | EBQ19 |
| BRID-025 (Addendum)<br><sub>Add. p.11; main p.32</sub> | User | Marketing AO uploads insurer direct billing documents for EB changes; every document uploaded or generated is linked to the correct transaction and endorsement type (placement, renewal, endorsement, adjustment, franchise request) and department (Marketing, Processing, Collection) | Mandatory link to endorsement / transaction type, client and policy; Marketing, Processing and Collection open documents directly; Collection sees billing documents, SOA and endorsement files, collection supporting documents; role-restricted. Negative: unlinked, not accessible to Collection, visible to unauthorised roles, no department or transaction context | **CHANGE** | Attachment links to any entity, document types (`attachment/service/DocumentService.java`); access is a single global permission pair `ATTACHMENT_VIEW` / `ATTACHMENT_MANAGE` (`attachment/api/AttachmentController.java`) | Platform: document **access class** per document type (`att_document_access`: document type -> permissions allowed to view), enforced by `DocumentService` for lists and downloads; EB document types seeded with classes MARKETING / PROCESSING / COLLECTION; the link carries a **process tag** (LOV `EB_PROCESS_TYPE`); EB uploads require the tag and the member change / transaction | platform (`attachment`) | M | EBQ17 |
| BRID-030<br><sub>p.34-35 (PDF 51-52)</sub> | System, User | Automated monitoring and tagging of contracts, HMO cards (loss / replacement) and billing / invoices per endorsement or member, with automatic follow-up e-mails | Status per item per endorsement or member; tags update automatically on state changes; follow-up e-mails beyond thresholds; view and filter pending items by account / member. Negative: untracked items, no follow-ups, wrong statuses | **NEW** | Alert framework and daily checks (`alert/service/AlertService.java`, `AlertCheck`); managed jobs; outbound e-mail | `eb_tracked_item` (type CONTRACT / HMO_CARD / CARD_REPLACEMENT / BILLING_INVOICE, programme, member or member change, responsible party INSURER / CLIENT / BDOI, status PENDING / RECEIVED / RELEASED / CLOSED, due date, follow-up count); created automatically by the triggering event (placement -> CONTRACT; member ADD -> HMO_CARD; member change -> BILLING_INVOICE), closed by the portal upload or manual update; job `EB_ITEM_FOLLOWUP` sends follow-ups at `EB_FOLLOWUP_DAYS` and escalates after `EB_FOLLOWUP_MAX`; Pending Items screen and report | `eb` | M | EBQ20 |

### F. Reports, access and audit

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRID-022<br><sub>p.28-30 (PDF 45-47)</sub> | System, User | Generate reports automatically or manually: Production, Renewal and Placement reports; customisable TAT reports; received / released date stamps; filters by team or AO | Parameters (activity, team, AO, date range); TAT metrics; export; accurate. Renewal report: policies for renewal with expiry, RA sent date, status (pending, confirmed, declined), proposals, comparative results, client changes, BOR / TOR / remarketing actions. Placement report: client confirmation, insurer selected, coverage, commission confirmations, counter-proposals, renegotiations, final terms, document uploads and confirmation dates. View, download, print, share; customisable (date range, client, product type); real-time | **NEW** | Report framework with parameters and PDF / XLSX / CSV / ODS / XML export (`report/core/ReportDefinition.java`, `TabularReportBuilder`); NB reports (`nbreport/report/*`), saved variants (BRNB.057) | EB reports in category `EMPLOYEE_BENEFITS`: `EB-PRODUCTION`, `EB-RENEWAL`, `EB-PLACEMENT`, `EB-TAT` (per activity of the TAT annex: received and released stamps, days, breach), `EB-PENDING-ITEMS`, `EB-FRANCHISE`; filters team, AO, client, benefit line, business type, date range; live SQL over the EB tables (no snapshots); saved variants reused; "share" = e-mail the export from the report runner | `eb` | M | EBQ21, EBQ22 |
| BRID-022.01<br><sub>Add. p.10-11</sub> | System, User | New Business report of new client accounts / policies in a period; NB or Renewal classification on the policy / transaction, required at creation / update, and as a filter | Business Type field required at creation; every report (Production, Renewal, Placement, NB) filterable by Business Type; NB report columns: client, line / product, effective date, insurer selected, premium, commission, AO, business type, date, status; export **PDF / XLSX / CSV / WORD** respecting authorisation | **CHANGE** | `booking/domain/BusinessType.java` exists on the invoice flags but is always `NEW_BUSINESS` (`booking/service/InvoiceBuilder.java`, line 129); no field on the account; no Word export (`report/render/ExportFormat.java`: PDF, XLSX, CSV, ODS, XML) | `acc_account.business_type` (NEW_BUSINESS / RENEWAL), required by `AccountService.createDraft` (defaults from the source: quotation / PRF = NEW_BUSINESS, EB cycle = its type, Renewal module = RENEWAL) and carried by `InvoiceBuilder` to the invoice and `InvoiceBooked`; Business Type filter on `NB-BOOKED-REG`, `NB-PRODUCTION`, `NB-PLC-UPDATE` and the EB reports; report `EB-NEW-BUSINESS`; `ExportFormat.DOCX` renderer (Apache POI XWPF) for every report | `account` + `booking` + `nbreport` | M | EBQ21, EBQ28 |
| BRID-023<br><sub>p.30-31 (PDF 47-48)</sub> | User, System | Only authorised users view, download, print or access documents and reports | Reports complete, readable, viewed or downloaded by authorised users, updated automatically. Negative: unauthorised access, authorised users refused | **FIT** | Every report declares its permission (`report/core/ReportDefinition.java`); endpoint `@PreAuthorize`; attachment permissions | Re-use; EB reports need `EB_REPORT_VIEW`; document restriction by department comes from BRID-025 | platform (`security`, `report`) | S | - |
| BRID-024<br><sub>p.31 (PDF 48)</sub> | System | Log all key actions (RA sent, feedback, proposals, documents, approvals, client confirmation) with time and user; history of changes to proposals and documents | Automatic logging with time, user and reference; reviewable; users see the history of proposals and documents | **CHANGE** | `audit/service/AuditTrailService.java` on every change, `CTL-AUDIT` report (`report/gl/AuditTrailReport.java`), workflow history | Every EB service records audit entries; proposals, TOR, BOR, comparative and roster are **versioned** (new row per version, never updated after submission) with a History tab and version diff; portal actions audited under the portal user and party | `eb` | S | - |

## 6. Lists and values from the BRD

### 6.1 Document types (BRID-007, 008, 021, 025, 026, 028, 030; flow)

| Code (proposed) | Document | Access class (BRID-025) | Source |
|---|---|---|---|
| `RENEWAL_ADVICE` | Renewal advice (RA) | MARKETING, PROCESSING | System |
| `EB_CLIENT_FEEDBACK` | Client feedback | MARKETING | AO / client portal |
| `EB_BOR` | Broker on Record (signed) | MARKETING, PROCESSING | AO / client |
| `EB_TOR` | Terms of Reference | MARKETING, PROCESSING | AO |
| `EB_MASTERLIST` | Master list (named) | MARKETING, PROCESSING | Client HR / AO |
| `EB_MASTERLIST_UNNAMED` | Unnamed master list (census) | MARKETING | Incumbent insurer (BRID-007 note) |
| `EB_UTILIZATION` | Utilization report | MARKETING | Client HR / incumbent |
| `EB_INDICATIVE_PROPOSAL` | Indicative (incumbent) proposal | MARKETING | Incumbent / AO |
| `EB_PROPOSAL` | Insurer proposal | MARKETING | Insurer portal |
| `EB_COMPARATIVE` | Comparative analysis | MARKETING | System |
| `EB_FRANCHISE_FORM` | ISO franchise form / franchise request | MARKETING | AO |
| `EB_CLIENT_CONFIRMATION` | Client confirmation of the chosen proposal | MARKETING, PROCESSING | Client |
| `EB_POLICY_FORM` / `EPOLICY` | Policy form / contract | MARKETING, PROCESSING, COLLECTION | Insurer |
| `EB_DIRECT_BILLING` | Insurer direct billing for EB changes | MARKETING, PROCESSING, COLLECTION | AO (BRID-025) |
| `EB_SOA` | Insurer statement of account | PROCESSING, COLLECTION | Insurer |
| `EB_MEMBER_CHANGE` | Member change request (inclusion / deletion) | MARKETING, PROCESSING | Client HR / AO |
| `EB_ISACOM_APPROVAL` | ISACOM approval (non-accredited provider, Add. p.4 flow) | MARKETING | AO |

### 6.2 Target-state user matrix (Add. p.14-16, read from the rendered pages)

| Capability | AO | Client / HR | Insurer | Marketing | Processing | Collection |
|---|---|---|---|---|---|---|
| Sends renewal advice | x | | | | | |
| Collects / uploads client feedback | x | x | | | | |
| Sends initial proposal (incumbent rates) | x | | | | | |
| Remarkets to other insurers | x | | x | | | |
| Insurer access (view / upload / update), validated by BDOI system controls | | | x | | | |
| Captures / tags new clients (prospect / account) | | x | | x | | |
| Uploads TOR and master list | x | | x | | | |
| Secures / uploads BOR (client signs as confirmation) | x | x | | | | |
| Distributes TOR / master list / utilization | x | | x | | | |
| Generates comparative analysis (system / AO) | x | | x | | | |
| Presents comparative report; client confirms / approves | x | x | | | | |
| Captures / relays client changes | x | x | x | | | |
| Entry / upload of employee benefit changes | x | x | x | x | | |
| Client view / upload / manage policy | | x | | | | |
| Insurers update proposals | | | x | | | |
| System triggers approval workflow (value threshold); BDOI Management approves | x | | | | | |
| Triggers placement after client confirmation; client approves | x | x | x | | x | |
| Processing assign / reassign / return / remark; Processing Supervisor approves if applicable | | | | | x | |
| Processing completes policy issuance | | | | | x | |
| Manages billing / payment tracking | | | | | | x |
| Generates reports | x | | | x | x | |
| Logs all key actions (audit) | x | x | x | x | x | x |
| Uploads direct billing docs for benefit changes; Processing validates completeness | | | x | x | | |
| Franchise request submission | | | x | x | | |
| Franchise approval / rejection by the insurer | | | x | | | |
| Advises client of franchise outcome | | x | | x | | |

The current-state Role Matrix (p.41, scanned) has an extra "System" column ticked on every row and still lists "Present docs for high-risk accounts" (now OUT).

### 6.3 Turn-around times (p.42, scanned)

| Activity | Trigger | Responsible | TAT | Parameter (proposed) |
|---|---|---|---|---|
| Submission of request for franchise | Submit documents to insurer | BDOI Marketing | 1-3 working days upon receipt of complete documents | `EB_TAT_FRANCHISE_SUBMIT` |
| Franchise approval | Approval for franchise; high-value triggers | Insurance Provider | 1-5 working days upon request | `EB_FRANCHISE_TAT_DAYS` |
| Submission of request for quotation (new) | Submit documents / request to insurer | BDOI Marketing | 1-3 working days upon franchise approval | `EB_TAT_RFQ_SUBMIT` |
| Submission of renewal advice | Send renewal notice to client | BDOI Marketing | 135 days prior to inception date | `EB_RA_LEAD_DAYS` |
| Request renewal requirements | E-mail request for renewal docs (active list, loss experience, etc.) | BDOI Marketing | 135 days prior to inception date | `EB_RA_LEAD_DAYS` |
| Issuance of quotation (new / renewal) | Provide quote / proposal to BDOI Marketing | Insurance Provider | 1-5 working days upon complete documents | `EB_PROPOSAL_REPLY_DAYS` |
| Sending of proposal to client | Provide proposal / renewal proposal, comparative analysis | BDOI Marketing | 1-5 working days from receipt of proposal | `EB_COMPARATIVE_DAYS` |
| Sending of client's confirmation | Send confirmation / request for certificate of cover | BDOI Marketing | 1-2 working days upon client confirmation | `EB_TAT_CONFIRMATION` |
| Sending of certificate of cover | Issue certificate of cover | Insurance Provider | Within 1 working day | `EB_TAT_COC` |
| Submission of request for placement and booking | E-mail for placement / booking with documents | BDOI Marketing | 1-5 working days upon client confirmation | `EB_TAT_PLACEMENT_REQUEST` |
| Submission of placement slip | Submit placement to insurer | BDOI Processing | 1-3 days upon request | `EB_TAT_PLACEMENT_SLIP` |
| Submission of policy / contract and billing / SOA | Submit policy / contract, endorsements, billing / SOA | Insurance Provider | 1-10 working days from placement slip | `EB_TAT_POLICY_SOA` |
| Validation of SOA | Validate SOA received from insurer | BDOI Processing | Within 3 working days upon receipt of final SOA | `EB_TAT_SOA_VALIDATION` |
| Booking in EBIX | Book account once billing is validated | BDOI Processing | Within 3 working days upon request | `EB_TAT_BOOKING` |
| Checking of policy / contract | Check policy vs proposal | BDOI Processing | Within 3 working days upon request | `EB_TAT_POLICY_CHECK` |
| Releasing of SOA and policy | Release SOA to Collection and policy to client / COG | BDOI Processing | Within 3 working days upon request | `EB_TAT_RELEASE` |
| Adjustments (inclusion / deletion) | Send / consolidate list, request / follow-up SOA / billing | BDOI Marketing / Processing | 1-3 working days per step | `EB_TAT_MEMBER_CHANGE` |
| Collection of premium | Collect premium due from client | BDOI Collection Team | 1-2 working days upon SOA / billing receipt | `EB_TAT_COLLECTION` |
| Submission / validation / releasing of cards | Submit, validate, release cards to COG / client | Insurance Provider / COG | 5-10 working days depending on volume | `EB_TAT_CARDS` |
| OR submission | Submit original Official Receipt to client | Insurance Provider | Weekly | `EB_TAT_OR` |

"Booking in EBIX" becomes booking in BIBS (BRID-020). "COG" is not defined in the pack (EBQ21).

## 7. Reports

| Code | Report | BRD | Permission |
|---|---|---|---|
| `EB-PRODUCTION` | Production by team, AO, benefit line, insurer, business type; premium and commission | BRID-022 | EB_REPORT_VIEW |
| `EB-RENEWAL` | Programmes due for renewal: expiry, RA sent date, status, proposals received, comparative result, client changes, BOR / TOR / remarketing actions | BRID-022 AC5 | EB_REPORT_VIEW |
| `EB-PLACEMENT` | Placed programmes: client confirmation, insurer selected, coverage, commission, counter-proposals and revisions, final terms, document uploads and confirmation dates | BRID-022 AC6 | EB_REPORT_VIEW |
| `EB-NEW-BUSINESS` | New client programmes in a period: client, line / product, effective date, insurer selected, premium, commission, AO, business type, date, status | BRID-022.01 | EB_REPORT_VIEW |
| `EB-TAT` | Received / released stamps and days per activity of the TAT annex, breaches, by team or AO | BRID-022 AC1-2 | EB_REPORT_VIEW |
| `EB-PENDING-ITEMS` | Contracts, HMO cards and billing pending per programme / member, age, follow-ups sent | BRID-030 | EB_REPORT_VIEW |
| `EB-FRANCHISE` | Franchise requests, outcomes and response times per insurer | BRID-026/027 | EB_REPORT_VIEW |
| existing `NB-BOOKED-REG`, `NB-PRODUCTION`, `NB-PLC-UPDATE` | Gain the Business Type filter | BRID-022.01 AC2 | unchanged |

## 8. Non-functional requirements

| Topic | BRD (p.36-37, Add. p.14) | Approach | Fit |
|---|---|---|---|
| Users | Marketing 13, Processing 4, Collection 3 (max concurrent equal); plus insurer and client HR portal users (not sized) | Well within the BRD-1 sizing; portal sized separately (EBQ13) | FIT |
| Volumes | 2025 / 2026 transactions per team: BDO 400 / 550; SM 2,500 / 3,500; Voluntary 550 / 560; Solicited 700 / 900; New Business 50 / 55 (about 4,200 / 5,565 a year) | Low volume; the roster (members) is the larger dataset: index by programme and policy year | FIT |
| Peak | Month-end; peak hours "08:30 - 07:00pm" (read 08:30-19:00) | Jobs (RA, follow-ups) run off-peak | CONFIGURE |
| Availability | 99.99%; usage 08:30-19:00; maintenance per bank standard; RTO 4 h, RPO 24 h | 99.99% is above every other BRD (99.9%); HA deployment; the portal adds an internet-facing component | CONFIGURE (EBQ25) |
| Retention | Application, database, audit logs and historical data: 5 years online, 15 years offline, backup every 4 hours, backup retention 7 years | nbadmin retention framework (BRD-1 BRNB.106) with record types EB_PROGRAMME, EB_MEMBER | CHANGE |
| Anonymisation | Not required | None; but see data privacy below | FIT |
| Devices | Mobile and desktop users expect the same speed | Responsive UI (portal included) | FIT |
| Security (derived) | Portal / API with authentication, RBAC, audit, validation before effect (BRID-005, 014) | Separate realm, scoped queries, virus scanning of external uploads (VirusScanner adapter), rate limiting, MFA to confirm | NEW (EBQ13) |
| Data privacy (derived) | Master lists hold employees' personal data; utilization reports are health-related | Sensitive personal information under the Data Privacy Act (RA 10173): need-to-know access classes, encryption in transit and at rest, download logging, retention per EBQ15 | NEW (EBQ15) |

## 9. Questions answered by this BRD

The BRD answers no parked Operations item in full (**OQ01, OQ02, OQ07 and OQ45 stay open**). It gives partial answers to the questions below.

| Q# | Source | Question (short) | Answer from the EB BRD | Design impact |
|---|---|---|---|---|
| Q06 | NB spec | How are insurers reached: e-mail, SFTP or API? | For EB, insurers must use a **secure portal or API** (BRID-005, 005.01-005.02); TOR goes "per agreed and defined channel" (BRID-009) | The insurer portal becomes a second insurer channel. `cat_insurer.placement_channel` can later accept `PORTAL` for placement slips (today refused with `PLACEMENT_CHANNEL_PARKED`); not changed in this BRD |
| Q07 | NB spec | Password convention for outbound documents | Either a **standard assigned password or a system-generated one following a defined syntax**, always sent separately, single or bulk; the convention must be documented (BRID-007) | Confirms the existing `DocumentPasswordPolicy` design (both modes); the convention itself is still missing (EBQ09) |
| Q23 | NB spec | Full list of allowed file types | EB: PDF, Word, Excel "and other agreed formats"; feedback also as text and image (BRID-002, 005.01, 014) | Within today's `AllowedFileType`; DOCX protection needed for outbound (BRID-007) |
| Q31 | NB spec | How are e-policies received? | For EB, insurers **upload policy forms and billing through the portal** (BRID-005.01) | Portal tasks deliver EB policy forms into `issuance` (e-policy attach) after validation |
| Q39 | NB spec | Retention periods per record type | 5 years online, 15 years offline, backup every 4 hours, backup retention 7 years; no anonymisation | Record types EB_PROGRAMME and EB_MEMBER in `nba_retention_rule` |
| Q40 | NB spec | Scope of "dynamic reports" | "Customise needed reports (date range, client, product type)" (BRID-022 AC9) | Saved report variants are enough; no ad-hoc builder |
| BRNB.097 (Business Type) | NB build | NB / Renewal classification on the booked invoice (built as a constant) | The classification is **required at creation / update of the policy / transaction and a filter on all reports** (BRID-022.01) | `acc_account.business_type` (required) and the invoice takes it from the account; replaces the constant in `InvoiceBuilder` |
| OQ01 | OPS spec | Is "Collection" an external system? | For EB, billing and payment tracking are done by the **Collection team inside the same system** (BRID-021, user matrix) | Consistent with the Collections design (in-app module); EB adds no Collection interface |
| OQ29 | OPS spec | Channel to send and receive insurer reports | A secure **insurer portal** will exist (BRID-005) | Prod Recon and Remittance may later offer insurer files through portal tasks; not in this build |
| OQ38 | OPS spec | Direct payment list sources, "fully paid to insurer" | EB changes are **billed directly by the insurer** and the AO uploads the direct billing documents (BRID-025) | EB programmes paid directly to the insurer are accounts with the direct-payment arrangement (BRNB.114); Commission handles the commission receivable; EBQ17 confirms |
| OQ44 / CQ25 / AQ27 | OPS / CLXN / ACCT | NFR alignment across BRDs | Adds a new set: 99.99%, 08:30-19:00, RTO 4 h / RPO 24 h, backup retention 7 years | Not resolved; one more variant (EBQ25) |
| OQ48 / AQ28 | OPS / ACCT | Role matrices | EB personas and the target-state matrix (Add. p.14-16) | EB roles in the design (section 6) |
| AQ21 | ACCT spec | Direct-billed indicator source | For EB, the AO's upload of the insurer's direct billing (BRID-025) | `eb_member_change.direct_billed` and the account's direct-payment arrangement |
| PQ21 | PM spec | Which documents leave BDOI protected | TOR, master list and utilization to insurers are password protected with the password sent separately (BRID-007) | EB outbound documents always protected |
| UX-3 | UX guidelines | Employee Benefits menu entry waits for its BRD | This BRD | Section **Employee Benefits** in the Client & Policy group (design section 11) |

## 10. Open questions for BDOI

| Q# | Topic | Question | Related |
|---|---|---|---|
| EBQ01 | Lines and catalog | Full list of EB benefit lines (HMO, GLI, GPA; also dental, rider benefits?). Are HMO providers panel "insurers" in the catalog (party type INSURER) although they are not insurance companies? Commission rates per provider and line | BRID-013, catalog |
| EBQ02 | RA timing | RA lead time: 180 days (BRID-001 example) or 135 days (TAT annex)? Reminder frequency and number; feedback request content; recipients (HR contacts) | BRID-001 |
| EBQ03 | Renewal flag | Who flags a programme "for renewal" and when? Relation to the Renewal BRD dispositions (For Renewal / Not for Renewal, NAL / NFR letters) | BRID-001, 003, 004 |
| EBQ04 | Incumbent proposal | Does the incumbent submit the indicative renewal terms through the portal, or does the AO encode them? Which rates are "latest incumbent rates"? | BRID-003 |
| EBQ05 | BOR on renewal | The renewal lane of the flow requests a BOR, while the BRID-008 negative scenario rejects a BOR for a renewal. Is a BOR needed for renewals when remarketing, never, or always? | BRID-008, flow |
| EBQ06 | BOR validation | "System validates that the BOR is signed": is an AO attestation acceptable, or is an e-signature expected? BOR template and validity period | BRID-008 |
| EBQ07 | Franchise | Definition of "franchise" (the insurer's accreditation of BDOI for the account?); is it needed for the incumbent; per insurer or once per cycle; content of the ISO franchise form; "high-value triggers" in the TAT annex | BRID-026, 027, 029 |
| EBQ08 | TOR structure | Standard TOR template per benefit line (benefit schedule items) so that the portal proposal form can be structured "aligned to TOR items" | BRID-005.02, 009 |
| EBQ09 | Password convention | The documented convention (standard value or syntax) for EB documents to insurers and clients | BRID-007, Q07 |
| EBQ10 | Comparative | Factors and scoring (coverage, premium, exclusions, capabilities: company stability, clinic providers, hospitals, technology); "within three days"; who are the authorised signatories; is this approval separate from the value-threshold approval? | BRID-010, 016 |
| EBQ11 | Threshold | Threshold values (TSI >= 500M, premium >= 20M are examples); measured per benefit line or per programme; annual premium or total; approver levels ("level of authority", BDOI Management) | BRID-016 |
| EBQ12 | Client decision | Accepted evidence of client confirmation (portal click, e-mail, signed document); does the client approve placement separately from the proposal (Add. p.16 note)? | BRID-011, 017 |
| EBQ13 | Portal | Hosting (internet-facing, DMZ) and BDO Information Security approval; authentication (MFA, password policy, SSO for insurers?); who creates and approves insurer and client users (BDOI admin or delegated insurer / HR admin); several users per insurer / client; API for insurers now or later; virus scanning product | BRID-005, 005.01-005.03, 014 |
| EBQ14 | Client portal scope | BRID-014 limits clients to viewing and uploading, while the target matrix says "view / upload / manage policy": what does "manage" include? | BRID-014, Add. p.15 |
| EBQ15 | Member data | Master list fields; is the roster kept in BIBS per member (needed for BRID-013 / 030) or only as files? Health data in utilization reports: access, retention and masking rules under the Data Privacy Act; meaning of "unnamed master list will be submitted by the insurer" | BRID-007, 013, 014, 030 |
| EBQ16 | Member movements | Movement types (inclusion, deletion, plan change, salary change for GLI); pro-rata basis; financial effect booked as endorsement per movement or per monthly billing; meaning of "MIS Credit" and "no payment no booking on adjustment" (p.3) | BRID-013, exec summary |
| EBQ17 | Direct billing | For EB changes billed directly by the insurer, who collects from the client (insurer or BDOI)? Is BDOI's commission then a receivable from the insurer (direct payment)? | BRID-025, OQ38 |
| EBQ18 | Billing number | Which number is the "billing number" (insurer SOA / billing no.)? Duplicate key scope: per insurer or global? Is it mandatory for every EB booking? | BRID-020 |
| EBQ19 | SOA | SOA formats per insurer; validation rules (against placement terms, headcount); recipients of the released SOA (client HR, Collection) | BRID-021 |
| EBQ20 | Monitoring | Statuses of contracts, HMO cards (loss / replacement) and billing; follow-up dates and thresholds; recipients of automatic follow-ups (insurer, client, internal) | BRID-030 |
| EBQ21 | Reports and TAT | Layouts of Production, Renewal, Placement and NB reports; TAT definitions per activity (start and stop events); is Word export required for all reports or for NB only; meaning of "COG" | BRID-022, 022.01, p.42 |
| EBQ22 | Teams | Are the five teams (BDO, SM, Voluntary, Solicited, New Business) a programme attribute or units of the sales organisation? | Add. p.14 |
| EBQ23 | Processing supervisor | When does the Processing Supervisor approve ("if applicable")? | Add. p.15, BRID-018 |
| EBQ24 | ISACOM | What is ISACOM, and when is its approval needed ("if the policy will be awarded to a non-accredited provider", Add. p.4)? Document and approver | Flow |
| EBQ25 | NFR | 99.99% availability vs 99.9% elsewhere; RPO 24 h with 4-hourly backups; hours 08:30-19:00 vs other BRDs; portal availability for insurers and clients | NFR, OQ44 |
| EBQ26 | High-risk accounts | Confirm that no high-risk handling applies to EB (BRID-028 OUT) although the current role matrix still lists it | BRID-028 |
| EBQ27 | Voluntary plans | Team 3 Voluntary: do employees pay individually (billing and collection per member), or does the employer pay? | Add. p.14 |
| EBQ28 | Renewal BRD boundary | Are EB renewals handled only by this BRD (own RA, own cycle), with the Renewal BRD covering the other lines? Shared RA template? | BRID-001, Renewal BRD |

## 11. Observations on the BRD pack

- The pack contains two documents with separate page numbering: the addendum (p.1-17, approved 26-Feb-2026) and the main BRD (p.1-42, signed Nov-2025). The addendum's revision log starts "February 04, 2025", which is probably 2026 (the other entries are Feb-2026).
- BRID-025 and BRID-026 are restated in the addendum with a much wider scope: BRID-026 changes from "franchise request" to "all processes" (NB, renewal, adjustment, franchise, proposals). The addendum text is used.
- BRID-005 is kept as the umbrella row; the addendum splits it into 005.01 (system), 005.02 (insurer) and 005.03 (user review).
- BRID-028 is OUT per the addendum ("high risk accounts not applicable to Employee Benefits"), but it is still in the main BRD and in the current role matrix (p.41).
- The BRID-008 negative scenario says a BOR uploaded "for a renewal transaction" is invalid, while both flow charts request the BOR in the renewal lane (EBQ05).
- The RA lead time differs: 180 days in BRID-001 (example) and 135 days in the TAT annex (EBQ02).
- The addendum flow (Add. p.4) has steps the main flow (p.5) does not: franchise approval before TOR release, and an ISACOM approval note. The main flow shows "Upload TOR and master list" for New Business only.
- The target user matrix ticks "Client / HR" and "Marketing" (not the AO) for "captures / tags new clients", and ticks "Insurer" for "AO uploads TOR and master list" and "generates comparative analysis"; these read as participation, not as makers.
- BRID-022.01 asks for Word export; no other BRD does.
- Volume growth percentages do not all match the figures: Voluntary 550 -> 560 is 1.8%, not 3%; BDO 400 -> 550 is 37.5% (shown 38%); Solicited 700 -> 900 is 28.6% (shown 29%).
- Peak hours are written "08:30 - 07:00pm"; availability 99.99% is higher than every other BRD.
- The TAT annex still says "Booking in EBIX"; in BIBS this is BRID-020 booking.
- The main approval sheet marks the Corporate Processing Team Head as "Not applicable for this BRD"; one input date reads "11/124/2025".
- The executive summary mentions "MIS Credit / no payment no booking on adjustment" as an internal guideline without a requirement row (EBQ16).
- Personal and health data (master lists, utilization reports) are central to EB, but the BRD has no data privacy requirement; the design treats them as sensitive personal information (section 8).
