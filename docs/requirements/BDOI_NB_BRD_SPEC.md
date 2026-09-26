# BDOI New Business (BRD-1) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse.

Status: **baseline of the BRD-1 build.** This file was first issued for BDOI concurrence before the build, with a fit/gap review workbook generated from the same data. That workbook is superseded by the FRS ([`FRS_BRD01_NEW_BUSINESS.md`](../deliverables/src/frs/FRS_BRD01_NEW_BUSINESS.md), issued as `docs/deliverables/out/Drop-1_Transactional/FRS/BIBS_FRS_BRD-01_New_Business_v1.0.docx`), the discrepancy and clarification register (`docs/deliverables/out/Programme/Registers/`) and the as-built status per BR ID in [`BDOI_NB_TRACEABILITY.md`](BDOI_NB_TRACEABILITY.md).

## 1. Source documents

Source: `docs/source-documents/New Business (NB) BRD.pdf` (218 pages).

| Pages | Document | Content |
|---|---|---|
| 1-44 | NB Workshop Addendum, signed Apr-2026 (two identical copies) | BRNB.090-115, updates to BRNB.004 / 022 / 069 |
| 45-90 | NB Addendum - Other Lines & Non-Package, Dec-2025 | BRNB.001-039, data retention, out of scope, Annex (risk codes, product lines, definitions) |
| 91-129 | NB Fire & Motor - BRD ID consolidation, Dec-2025 | BRNB.040-089 mapped to legacy BRD 1.x-3.x |
| 130-218 | NB Fire & Motor BRD V06162025 | Legacy BRD 1.1.1-3.4.3, volumes/NFR, Appendix A (calculations), Appendix B (product matrix), Appendix C (TL confirmations) |

## 2. Business context

BDOI is a **broker**. Insurers issue the policies. BDOI does the following:
- prepares quotations (package products) or proposals through the TSU for non-package risks;
- onboards the client and creates the account;
- collects premium or confirms payment (CLPC loan billing for CBG Fire; payment reports for other segments);
- places the risk with the insurer;
- receives the e-policy, sends it to the client in encrypted form, and issues an Insurance Advice for mortgaged accounts;
- books the account: GL entry, service invoice, cost center and incentive flag.

Two processing modes apply to every step: **bulk** (source systems such as HLS, or uploads) and **individual**. Personas: Marketing AO/TL, TSU, Processing, E-policy Sender, Approver, Adjustment, Business Administrator and System Administrator.

## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 8 |
| CONFIGURE | Set-up only | 2 |
| CHANGE | Extend or re-purpose existing capability | 42 |
| NEW | New build | 65 |
| OUT | Out of scope per BRD | 2 |
| **Total** | | **119** |

### Platform impact

| Area | Treatment | Impact |
|---|---|---|
| Underwriting (insurer) | Re-purpose | Quotations/policies/endorsements become broker quotation -> account -> placement -> booking; premium calculator re-used with Appendix A formulas |
| Parties | Extend | Client master: prospect/confirmed, KYC, tags, special instructions, dedupe; insurers become panel parties with channels and branches |
| Attachments | Extend | Document types, ODS, naming syntax, multi-link, extraction, triggers |
| Approvals / maker-checker | Extend | Multi-level chains (PRF, QS, LOV, user access) |
| Accounting engine / GL | Re-use | New broker events: booking, cancellation, commission, VAT on commission, premium receivable/payable |
| Receivables / Payables | Re-use | Premium collected from clients and remitted to insurers; commission receivable from insurers |
| Tax (PH BIR) | Re-use | VAT on commission, EWT on commission, BIR service invoice |
| Dimensions | Re-use | Cost center / region / department / team on every booking |
| Reports & dashboard | Extend | ODS/XML output, print metadata, dynamic reports, NB operational reports |
| Jobs & alerts | Re-use | Batch booking, KYC monthly list, retention, hold-cover monitor, e-policy dispatch |
| Security & audit | Re-use | Roles/permissions for 39 NB functions; immutable audit trail |
| Reinsurance, reserves, claims reserving, actuarial | Not applicable (to confirm Q44) | Insurer-only; hide from BDOI tenant via feature flags |
| New modules | Build | Workflow engine & queues, Non-package placement (PRF/QS/PS), Bulk processing framework, Placement, E-policy & IA, Outbound e-mail & encryption, Integrations (HLS, CLPC, insurers), LOV service, Document extraction |

## 4. Proposed target workflow (for concurrence, Q10)

| Stage | Name | Owner | Meaning | Allowed next |
|---|---|---|---|---|
| S01 | Request received | Marketing AO | Quotation / PRF request logged (e-mail, HLS, upload) | S02, S99 |
| S02 | Quotation in progress | Marketing AO / TSU | Quotation or QS being prepared; insurer terms collected for non-package | S03, S99 |
| S03 | Quotation approved | Approver | Quotation / QS / PS approved and locked | S04 |
| S04 | Sent to client | Marketing AO | Quotation / proposal e-mailed (encrypted) | S05, S06 |
| S05 | Accepted by client | Marketing AO | Acceptance recorded; client onboarding checked | S07 |
| S06 | Declined / not proceeded | Marketing AO | Closed; retention policy applies | - |
| S07 | Account created | Marketing / System | Client confirmed; account(s) with ARN created; documents complete | S08, S20 |
| S08 | Awaiting payment / confirmation | System | CLPC billing / payment matching / client confirmation per segment | S09, S98 |
| S09 | Submitted for placement | Processing | Placement slip generated (prerequisites met) | S10 |
| S10 | Placed with insurer | System | Placement slip sent; hold-cover requested / confirmed | S11, S12 |
| S11 | Returned by insurer | Processing | Returned with remarks; may go back to Marketing | S09, S98 |
| S12 | Policy issued (e-policy received) | System | Policy number updated; e-policy linked; IA generated if mortgaged | S13 |
| S13 | Booked | Processing / System | GL entry + service invoice; incentive and cost center stamped | S14, S15 |
| S14 | E-policy sent to client | E-policy Sender | Encrypted e-policy dispatched; send log | S15 |
| S15 | Endorsed / cancelled | Processing / Adjustment | Post-issuance endorsement or cancellation | - |
| S20 | Direct booking (policy already issued) | Marketing / Processing | Placement bypassed; issued policy upload mandatory | S13 |
| S98 | Placement cancelled (reversal) | Processing | Pre-issuance internal reversal; can be reactivated | S09 |
| S99 | Voided | Authorised user | In-process record deleted (soft) with reason | - |

## 5. Requirements and fit/gap

### A. Product lines & product rules

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.001 | System | Other Lines follow the same workflow as Fire and Motor (quotation -> client/account -> placement -> billing -> issuance -> reporting) | **CHANGE** | Product master with line of business; one underwriting workflow for all products | Product master keyed by BDOI risk code (CAR00..TRP03, MTRxx, PARxx) and product line / cover type; single NB workflow engine shared by all lines | M | Q01 |
| BRNB.002 | System | Capture critical (product-specific) fields | **CHANGE** | Risk details per policy (generic + marine details) | Product-driven risk schema: field definitions per product line (motor vehicle, property location, engineering project, liability, marine, PA insured persons ...) | L | Q02 |
| BRNB.003 | System | Apply validation rules by product type | **CHANGE** | Bean validation + underwriting rules (fixed in code) | Configurable rule set per product (mandatory, LOV, range, cross-field) evaluated server-side and mirrored in UI | M | Q02 |
| BRNB.004 | Marketing | Shared templates with dynamic fields; standard intake template for quotation/proposal (updated in workshop: package + non-package, versioned) | **NEW** | Report/PDF rendering exists; no document template engine | Versioned document & intake template library (HTML->PDF / XLSX) with merge fields per product; intake template version stamped on each quotation | L | Q03 |
| BRNB.093 | Marketing | Data standards for risk information (vehicle, property, address) | **CHANGE** | Fixed mandatory fields | Minimum-field matrix per product line held in product master (loaded from BDOI product matrix MTR/PAR files) | M | Q02 |
| BRNB.098 | TSU | Rule-based TSU involvement (package vs non-package, fleet thresholds, single vs multiple locations, endorsements) | **NEW** | Approval limits by amount only | Routing rules table (product class, fleet count, location count, TSI threshold, endorsement type) -> TSU queue | M | Q04 |

### B. Non-package placement (PRF / QS / TSU)

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.005 | Marketing AO | Create Proposal Request Form (PRF) with complete risk details and attachments for quotation to insurers | **NEW** | Quotation with approval exists (insurer-side) | PRF entity + screen; approval chain Marketing TL -> TH -> UH; mandatory-document checklist per product; submit to TSU queue | L | Q05 |
| BRNB.006 | System | Automatic unique sequential marketing reference number for each PRF | **CHANGE** | Document numbering series exist (quotation/policy numbers) | New numbering series MKT-YYYY-nnnnnn; gap-free DB sequence per year; immutable | S |  |
| BRNB.007 | TSU | Receive / view PRF requests; amend, update, return (with reason) or delete | **NEW** | Approval inbox (approve/reject only) | TSU work queue with actions Accept / Return (reason LOV) / Update / Delete (in-process only) | M |  |
| BRNB.008 | TSU | Prepare and send Quotation Slip (QS) to selected insurers with terms and conditions | **NEW** | No outbound e-mail | QS document from template; insurer panel selection; approval; outbound e-mail service with delivery log (recipient, time, body, attachment hash) | L | Q06 |
| BRNB.009 | TSU | Key-in insurer feedback / terms, editable by authorised users | **NEW** | Quotation iterations (single insurer view) | Insurer response grid per QS (premium, rate, deductibles, conditions, validity) with version history | M |  |
| BRNB.010 | System | Automatically compile insurer feedback into an exportable comparative table | **NEW** | - | Comparative table view generated from insurer responses; XLSX/PDF export; recommended insurer flag | M |  |
| BRNB.011 | Marketing / TSU | Placement Update Reports (individual and collective) | **NEW** | Report framework (role-based, XLSX/CSV/PDF) | New report on report framework | S |  |
| BRNB.012 | Marketing / TSU | Dashboard for real-time tracking of requests, reports and comparative tables | **CHANGE** | Dashboard module (finance KPIs) | NB operations dashboard: requests by status/age/unit, SLA breaches, drill-down | M |  |
| BRNB.013 | System | Password protection / encryption of all outbound documents (QS, PS, advisories, comparative tables, placement update, renewal request reports) | **NEW** | - | PDF AES-256 password protection (and password-protected XLSX); password per convention; password sent in separate e-mail | M | Q07 |
| BRNB.014 | System | Validation and multi-level approval enforced before documents are sent | **CHANGE** | Maker-checker with authorization limits | Configurable multi-level approval chains per document type; send action disabled until approved | M | Q05 |
| BRNB.015 | System | Notifications on status changes and integration with BDOI tracking/reporting | **CHANGE** | In-app alerts | Notification service: in-app + e-mail per event/role; integration via file/API outbox | M | Q08 |
| BRNB.016 | System | Audit trail for all actions and changes | **FIT** | Immutable audit trail (DB-enforced) with before/after values | Extend audit coverage to new entities; audit viewer per record | S |  |
| BRNB.017 | TSU | Generate Proposal Slip for non-package placement after approvals; archive and retrieve | **NEW** | - | Proposal Slip document from accepted insurer terms; approval; stored as versioned document | M |  |
| BRNB.018 | TSU | Late Renewal Requests Report (renewals received after expiry) | **CHANGE** | Renewal due report exists | Scheduled late-renewal report (renewal request date > expiry) | S | Q09 |

### C. Quotation / proposal

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.020 | Authorised user | Edit quotation/proposal before approval/finalisation with version history | **CHANGE** | Quotation iterations + draft edit | Quotation versioning (v1..vn) with diff view | S |  |
| BRNB.021 | Approver | Approve/reject quotation/proposal before sending to client or account creation | **FIT** | Quotation approval with authorization limit and comments | Re-use; add lock + routing to next stage | S |  |
| BRNB.041 | Marketing | Receive quotation/proposal request via e-mail or system-triggered request | **NEW** | - | Request intake: manual capture of e-mailed requests (attach e-mail) + system channel (HLS/API) | M | Q12 |
| BRNB.024 | Marketing | Receive single/multiple accepted quotations; update them via file upload or manually | **NEW** | No bulk update upload | Generic bulk-upload framework (template download, validate, partial commit, error report) - used by all bulk features | L |  |
| BRNB.029 | System | Quotation requires a client code (minimum prospect data); incomplete client info flagged; downstream blocked until complete | **CHANGE** | Quotation requires a full party | Prospect-level party with minimum fields + completeness flag; gate at placement/booking | S | Q13 |
| BRNB.043 | Marketing | Create individual quotation/proposal (IDF, FFYMI), reference number, send via e-mail, receive acceptance, download PDF/XLSX, save | **CHANGE** | Quotation create/approve/print | Broker quotation per product with premium calc (Appendix A), PDF/XLSX output, e-mail to client, acceptance capture | M |  |
| BRNB.045 | Marketing | Receive client's e-mail acceptance and create list of accounts (individual) | **NEW** | Convert quotation to policy | Acceptance capture (upload acceptance e-mail) -> create account(s) from quotation | S |  |
| BRNB.102 | Marketing / Ops | One unique reference number per account from quotation, reused through processing, booking, issuance, invoicing | **CHANGE** | Separate quotation and policy numbers | Account Reference Number (ARN) as golden key carried on every child record, report and interface | M | Q15 |

### D. Client (CRM, onboarding, KYC)

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.030 | User / System | Create client or update existing (bank standards); dedupe; flag invalid; upload KYC docs (internal & external sources); drafts; unique client code; push to next stage | **CHANGE** | Party master with approval, TIN, addresses | Client master: individual/corporate KYC fields per BDO standard, dedupe keys, draft state, client code series, document checklist | M | Q16 |
| BRNB.032 | User | Prevent duplicate clients/accounts for Fire, Motor, Other Lines; allow flagged duplicates for endorsements | **CHANGE** | Party TIN uniqueness | Duplicate-detection service (exact + fuzzy name) returning matches with reference numbers | M | Q17 |
| BRNB.046 | Marketing / Processing | Search clients by single/multiple criteria and view full details | **FIT** | Party search with filters | Add criteria (ID no., mobile, e-mail, client code, status) | S |  |
| BRNB.047 | Marketing | Update client details in bulk with validation, draft, cancel, push | **NEW** | - | Bulk client update upload | S |  |
| BRNB.048 | Marketing | Create client (individual) with validation, documents, draft, cancel, client code, push | **CHANGE** | Party create + approval | See BRNB.030 | S |  |
| BRNB.049 | Marketing | Update client (individual) with documents, draft, cancel, push | **CHANGE** | Party edit + approval | Draft + document upload on client | S |  |
| BRNB.065 | System | Auto-create clients not yet existing / auto-update existing (bank standard) during bulk; flag invalid; drafts; client codes | **NEW** | - | Client upsert step in bulk job using dedupe keys | M | Q16 |
| BRNB.090 | Marketing / Ops | Standard client onboarding step before any new business | **CHANGE** | Party approval (maker-checker) | Onboarding workflow: Prospect -> KYC verified -> Confirmed client; quotation/booking gates | M | Q16 |
| BRNB.091 | All users | Client-level and account-level tags and special instructions applied across servicing, collections, processing | **NEW** | - | Tag LOV + special-instruction notes on client/account; banner on every related screen; history | M | Q18 |
| BRNB.099 | Processing | CRM-like view: leads, prospects, special instructions; linkage client-account-policy visible; missing linkage flagged | **NEW** | - | Client 360 page (accounts, policies, documents, instructions, activity timeline) | M | Q19 |
| BRNB.101 | Marketing | Distinct identifier for prospect vs confirmed clients; traceable conversion | **NEW** | - | Client status PROSPECT/CONFIRMED; prospect code P-nnnn converted to client code C-nnnn (both retained) | S | Q20 |
| BRNB.110 | Marketing | Monthly list of non-bank clients due for KYC review | **NEW** | Monitored job framework | KYC review-date per client + monthly job producing report + notification | S | Q21 |

### E. Account (risk record)

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.025 | User | Update account(s) - bulk and individual upload/entry, validate mandatory fields, draft, submit | **NEW** | - | Account entity + bulk update | M |  |
| BRNB.050 | Marketing / Processing | Search accounts by single/multiple criteria and view full details | **CHANGE** | Policy search | Account search (ARN, PN no., plate, engine, chassis, location, client, status, insurer) | S |  |
| BRNB.051 | Marketing | Create account (individual): multi-value fields, currency, auto calculations (pro-rata, short-term, period), duplicate fall-out (motor vehicle IDs / fire client+location+items), link client, docs, auto status, reference, draft, cancel, push | **CHANGE** | Policy with risks, currency, premium calculator, cover period | Account (placement record) with multi-risk items, FX, calculator per Appendix A, duplicate rules per line | L | Q22 |
| BRNB.052 | Marketing / Processing | Update account (bulk) with validation, multi-values, auto calculation, draft, cancel, push | **NEW** | - | Bulk update | S |  |
| BRNB.053 | Marketing | Update account (individual) incl. document upload | **CHANGE** | Policy draft edit | Account edit | S |  |
| BRNB.054 | Processing | Update account (individual) - processing | **CHANGE** | Policy draft edit | Account edit with role-specific fields | S |  |
| BRNB.066 | System | Auto-create accounts in bulk: validation, multi-values, currency, dedupe fall-out, link client, receive docs with naming, auto status, auto-save draft, reference, associate quotation ref, push | **NEW** | - | Bulk account creation job | M | Q22 |
| BRNB.109 | Marketing | Account-level contact details used for communication; default from client level | **NEW** | Party addresses/contacts | Account contact block defaulted from client | S |  |

### F. Bulk intake & source systems

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.023 | Marketing | Receive quotation requests for home insurance directly from HLS (Home Loan System) | **NEW** | - | HLS inbound interface (SFTP file or REST) with source signature check, staging table, validation and auto quotation creation | L | Q11 |
| BRNB.028 | System | Create bulk quotations from source-system request details (integration or manual upload) | **NEW** | - | Bulk quotation job on bulk-upload framework | M | Q11 |
| BRNB.063 | System | Create bulk quotation/proposal from received requests / uploaded list / source system; allow creation without client code; generate reference | **NEW** | - | Bulk quotation job; note conflict with BRNB.029 (client code required) - see Q13 | M | Q13 |
| BRNB.064 | System | Generate accounts for bulk processing from source system or manual upload (.xlsx/.ods); sanitise list by defined criteria; push to next stage | **NEW** | - | Bulk account job with sanitisation rules (trim, case, format, dedupe, LOV mapping) | M | Q14 |
| BRNB.039 | User | Accept manually uploaded accounts for bulk processing using the standard template | **NEW** | - | Bulk-upload framework | S |  |
| BRNB.042 | Marketing | Create quotation/proposal for bulk processing (IDF, FFYMI ...), generate reference, send single/multiple | **NEW** | - | Bulk quotation + batch send | M |  |
| BRNB.044 | Marketing | Manually build accounts for bulk processing (receive accepted quotes, download PDF/XLSX, create client/account upload list, upload docs, search client) | **NEW** | - | Bulk-upload framework + account list builder | M |  |

### G. Documents

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.026 | User | Upload single/multiple files (.jpg .xlsx .ods .pdf ...), inherit or nominate file name (syntax), size limits, link to account(s), remove | **CHANGE** | Attachments: PDF/PNG/JPEG/XLSX/DOCX/CSV with signature check & virus-scan hook | Add ODS/others, multi-file upload, naming syntax, link one file to many accounts, document type LOV | S | Q23 |
| BRNB.055 | Marketing / Processing / System | Upload files/documents (same as BRNB.026) | **CHANGE** | Attachments | See BRNB.026 | S |  |
| BRNB.056 | Marketing / Processing | View client-level and account-level documents; download single/multiple; save | **CHANGE** | Attachment list/download per record | Multi-select ZIP download; client + account document tabs | S |  |
| BRNB.104 | System | Auto-extract details from uploaded documents (e.g. e-policy) and update account after user confirmation | **NEW** | - | Document extraction service (PDF text + OCR, insurer-specific templates) with review screen | XL | Q24 |
| BRNB.105 | System | Uploaded documents trigger defined business processes | **NEW** | - | Document-type trigger rules (e.g. e-policy uploaded -> policy number update -> booking) | M | Q24 |

### H. Placement with insurers

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.033 | Marketing | Handle accounts returned by Processing: view, update, comment, push | **NEW** | Reject with comment only | Return-to-sender action with reason LOV and return queue | S |  |
| BRNB.034 | Processing | Handle placement requests returned by insurer: update, remarks, return to Marketing AO/TL, receive updates, push | **NEW** | - | Insurer return capture + return loop Processing <-> Marketing | M |  |
| BRNB.058 | Marketing | Handle returned placement request (same as BRNB.033) | **NEW** | - | See BRNB.033 | S |  |
| BRNB.059 | Processing | Handle returned placement request from insurer (same as BRNB.034) | **NEW** | - | See BRNB.034 | S |  |
| BRNB.062 | Processing | Cancel placement (single/multiple) with comments; auto-tag Cancelled Placement and return to previous stage | **NEW** | - | Cancel-placement action (pre-issuance reversal, see BRNB.094) | S |  |
| BRD 2.1.16 | Marketing | Reactivate cancelled placed accounts (single/multiple) and move to next stage (listed in role matrix, no BRNB ID) | **NEW** | - | Reactivate action | S | Q25 |
| BRNB.069 | Processing / System | Generate placement file/slip automatically, individually or in batch; regenerate for returns; only when prerequisites met (workshop update) | **NEW** | - | Placement slip per insurer format (XLSX/PDF) with prerequisite checks (paid/confirmed, docs complete, TSU cleared) | M | Q26 |
| BRNB.071 | System | Automatically send placement file/slip to insurer(s); resend for returns | **NEW** | - | Insurer channel config (e-mail / SFTP / API) + send log | M | Q06 |
| BRNB.072 | System | Automatically send 30-day hold-cover request to insurer(s) | **NEW** | - | Hold-cover request document + send | S | Q27 |
| BRNB.103 | Marketing | Record insurer hold-cover confirmation (insurer, reference, date); status visible | **NEW** | - | Hold-cover confirmation capture + expiry monitor | S | Q27 |

### I. Billing & payment confirmation

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.067 | System | CBG Fire billing file to CLPC (PN no., loan application no., booking date, borrower, originating unit, premium, reference, BDOI location, amortised Y/N); save .xlsx/.ods; forward to CLPC; receive payment report; match paid/unpaid by PN or loan application no.; push paid | **NEW** | Receivables module (open items, receipts) | CLPC outbound billing file + inbound payment report matcher; creates premium receivable/receipt entries | L | Q28 |
| BRNB.068 | System | Validate payment for other market segments by matching payment reports on reference number | **CHANGE** | Receipt matching to open items | Payment-report import + auto-match on ARN; payment gate per segment (CBG Fire & Motor paid; Other Lines client confirmation) | M | Q28 |
| BRNB.114 | Marketing | Identify and tag accounts paid directly to insurers (Direct Payment) | **NEW** | - | Payment-arrangement attribute (via BDOI / direct to insurer) driving billing and commission receivable | M | Q29 |

### J. E-policy & insurance advice

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.035 | System / User | Encrypt and password-protect Insurance Advice and e-policies before sending; password standard or generated; password in separate e-mail; single & bulk | **NEW** | - | See BRNB.013 + outbound e-mail | M | Q07 |
| BRNB.060 | Marketing | Access system-generated Insurance Advice: search, list, view, download, save, send single/multiple, encrypt | **NEW** | - | Insurance Advice register screen | M |  |
| BRNB.070 | System | Generate Insurance Advice (mortgaged accounts only), single or multiple | **NEW** | - | IA document (template) for mortgagee-bank accounts | M | Q30 |
| BRNB.095 | System | Explicit IA generation trigger rules aligned to lifecycle | **NEW** | - | IA trigger rule (e.g. on placement confirmation / booking) | S | Q30 |
| BRNB.073 | System | Receive e-policy (PDF) from insurers and save to designated folder | **NEW** | - | E-policy inbox (monitored mailbox / SFTP / upload) matched to account | M | Q31 |
| BRNB.074 | System | Automatically update policy number and link e-policy artifacts to the account | **NEW** | - | Policy number capture (from extraction or insurer file) -> account | M | Q24 |
| BRNB.077 | E-policy Sender | E-mail e-policy to client, single/multiple, encrypted, attach, advise password syntax | **NEW** | - | E-policy dispatch screen + batch job | M | Q07 |
| BRNB.078 | E-policy Sender | Report on successfully/unsuccessfully sent e-policies with reasons, by date range | **NEW** | - | E-policy dispatch report from send log | S |  |

### K. Booking, endorsements & cancellation

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.027 | System | Book accounts (individual/bulk) and positive financial endorsements; on approval auto-post GL entry and generate service invoice on booking date; all linked in audit; failures roll back booking | **CHANGE** | Accounting engine (event -> rule -> journal), immutable ledger, receivables invoices, VAT | Broker booking event: commission income + VAT on commission, premium receivable from client / payable to insurer (or direct-payment variant); service (commission) invoice to insurer; atomic | L | Q32 |
| BRNB.036 | System / User | Individual vs batch booking (manual & bulk upload), scheduled batch trigger (e.g. end-of-day), edit/delete before booking, confirm/cancel | **CHANGE** | Monitored job scheduler; single policy approval | Booking batch (select many, scheduled EOD job) with pre-booking confirmation | M |  |
| BRNB.038 | User | Direct booking without placement (individual/bulk) with documents, validation, confirmation and reports | **NEW** | - | Direct-booking path (see BRNB.111) | M |  |
| BRNB.061 | Processing | Book accounts manually / bulk; process positive financial endorsements | **CHANGE** | Policy approval posting | See BRNB.027 | S |  |
| BRNB.076 | System | Auto-book on defined criteria (e.g. after e-policy receipt); prevent duplicate booking; financial & non-financial endorsements | **CHANGE** | Endorsements (additional/refund/renewal/cancellation/nil) | Auto-booking rule + idempotent booking key (ARN + transaction no.) | M |  |
| BRNB.081 | Adjustment | Cancel booking (single/multiple); negative financial and non-financial endorsements | **CHANGE** | Cancellation & refund endorsements with reversal journals | Booking cancellation reverses commission/receivable entries; credit note to insurer | M |  |
| BRNB.094 | System | Distinguish true policy cancellation (post-issuance) from internal reversal (pre-issuance / correction) | **CHANGE** | Cancellation endorsement only | Two lifecycle actions: REVERSED (pre-issuance, no insurer impact) vs CANCELLED (post-issuance endorsement) | S |  |
| BRNB.100 | Processing | Service invoice generated from defined template and triggers; owner assigned; per invoice type (insurer / internal) | **CHANGE** | Receivables invoice with BIR fields | Service-invoice types (Insurer commission invoice, internal) with trigger rules and owner; BIR-compliant numbering | M | Q32 |
| BRNB.100b | Processing | Send generated service invoice to recipients; notify success/failure with reason (unnumbered row in addendum) | **NEW** | - | Invoice dispatch via e-mail service + log | S |  |
| BRNB.107 | System | Incentive eligibility indicator on booked transactions (rule-based) | **NEW** | - | Incentive rule table (product, segment, channel, period) -> indicator on booking | M | Q33 |
| BRNB.108 | System | Cost center captured for every booked transaction (MIS) | **CONFIGURE** | Dimensions module (COST_CENTER, PROFIT_CENTER, DEPARTMENT) posted on journals | Make cost center mandatory on booking; default from AO / unit | S | Q34 |
| BRNB.111 | Marketing / Processing | Bypass placement and book directly when insurer already issued the policy; issued-policy upload mandatory | **NEW** | - | Direct-booking path with document gate | S |  |
| BRNB.112 | Marketing / Processing | Book multi-year policies (e.g. 5-year term = 5 policy numbers linked to one booking reference) | **CHANGE** | Single-period policies; renewal creates new period | Multi-year booking with yearly policy-number schedule and per-year commission recognition (Appendix A multi-year formula) | M | Q35 |
| BRNB.113 | Marketing | Maintain Free First-Year (FFY) accounts list: upload, tag, edit dates, cancel, auto end date, list, audit | **NEW** | - | FFY attribute + bulk tagging + FFY register | M | Q36 |

### L. Workflow, status & governance

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.019 | Authorised user | Delete prospect/account records that are still in-process (not finalised/posted/in active workflow) | **CHANGE** | Draft delete for some entities; no soft delete | Soft-delete (VOIDED) with reason for in-process records; audit retained | S |  |
| BRNB.022 | System | Automatic status tracking (Received, In Process, Sent to Client, Accepted, Rejected, Account Created, Submitted for Placement, Booked, Cancelled, Endorsed); workshop update: defined stages and allowed transitions | **NEW** | Per-entity status enums (Draft/Pending/Approved/...) | Workflow engine: configurable stage & status model per transaction type, transition table, status history table, SLA timers | L | Q10 |
| BRNB.092 | Business unit | Defined authoritative source of truth per data element (client, vehicle, property, address, policy, invoice) | **NEW** | - | Data-ownership register + field-level source/lock metadata | M | Q37 |
| BRNB.096 | System | Marketing submissions are the official trigger for Processing (like GRF/ARF requests) | **NEW** | Approval inbox | Processing request queue | M | Q38 |
| BRNB.097 | System | Classify New Business vs Renewal before processing and route separately | **CHANGE** | Business type NEW / RENEWAL on policies | Transaction-type-specific workflows and queues | S |  |
| BRNB.106 | System | Retention policy for quotations, accounts, clients incl. not-proceeded/voided | **NEW** | Monitored job framework | Retention rule table + archive/purge job (see NFR retention 5y online / 15y archive) | M | Q39 |
| BRNB.115 | All users | Track account status from quotation to booking; stalled accounts identified; status reports | **NEW** | - | Status history + ageing/SLA on workflow engine (see BRNB.022) | S | Q10 |
| BRNB.080 | Approver | View accounts for assignment; assign / re-assign workload to users | **NEW** | - | Work allocation (manual + round-robin option) on queues | M |  |
| BRNB.079 | Approver | Approve requests (override, LOV change, profile creation/modification...): view, review, approve, decline, comment, push | **FIT** | Approval inbox with approve/reject + comments | Add LOV-change and profile-request sources | S |  |
| OOS-1 | Marketing / Processing | Override requests (submit, select approver, comment, push) - applicable to Renewals only | **OUT** | - | Deferred to Renewal BRD | - |  |

### M. Reports & dashboards

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.031 | System / User | Print any accessible report with preview, headers/footers/metadata, A4/Letter, summary/detail, standard and dynamic reports | **CHANGE** | PDF export with header/footer | Print-friendly PDF with metadata block (report, user, time, filters) + browser print preview | S |  |
| BRNB.037 | System | Download reports (xlsx, xml, ods, csv) with date range; saved to chosen location; logged | **CHANGE** | XLSX / CSV / PDF export; download logged | Add ODS and XML renderers | S |  |
| BRNB.057 | Marketing / Processing / Approver | Role-based reports: view, date range, download xlsx/ods, save, print, create dynamic reports | **CHANGE** | Role-based report catalogue | Dynamic report builder (choose dataset, columns, filters, grouping; save & share) | L | Q40 |
| BRNB.075 | System | Reports: successful & fall-out accounts per stage; current status per account; placement summary (sent/failed); CLPC billing; matched/unmatched payments; production statistics per region/department/team/user vs target | **NEW** | Report framework | 6 new operational reports + production dashboard with targets | M | Q41 |
| OOS-2 | System | QPS-specific references (QPS reference number, QPS placement report) - unique reference and reports still required | **OUT** | - | Replaced by ARN (BRNB.102) and placement summary report | - |  |

### N. Access, security & administration

| BR ID | Persona | Requirement | Fit | Current | Proposed solution | Effort | Q |
|---|---|---|---|---|---|---|---|
| BRNB.040 | All roles | Log in from any BDO-issued device with role profile; inactivity warning after 15 min; warning 30 min before system-triggered logout | **CONFIGURE** | JWT login, roles, configurable inactivity timeout and warning dialog | Set timeout parameters; add absolute session-end warning; BDO SSO / AD integration if required | S | Q42 |
| BRNB.082 | Business Admin | Open the application in multiple tabs | **CHANGE** | Token kept in sessionStorage (per tab - new tab needs login) | Share session across tabs (localStorage + BroadcastChannel logout sync) | S |  |
| BRNB.083 | Business Admin | Maintain LOVs: view, add, edit, deactivate, effectivity date (changes approved) | **NEW** | Code lists are per-module tables/enums | Generic LOV service (type, code, value, effective from/to, status) with maker-checker | M |  |
| BRNB.084 | Business Admin | Assign role profiles to users per User Access Matrix | **FIT** | User admin with roles | Load BDOI roles | S |  |
| BRNB.085 | Business Admin | Submit profile creation/modification requests for approval | **CHANGE** | User/role changes applied directly by admin | User-access request with approval (maker-checker) | S |  |
| BRNB.086 | Business Admin | View, download, save audit log report | **FIT** | Audit trail report with export | - | S |  |
| BRNB.087 | System Admin | Access application from any BDO workstation with System Administrator profile | **FIT** | Admin role | - | S |  |
| BRNB.088 | System Admin | Define profiles (Marketing, Processing, Approver, Business Admin, Adjustment, E-policy Sender) and assign 39 functions to profiles | **CHANGE** | Role -> permission model | New permission set for the 39 NB functions; seed BDOI profiles | S |  |
| BRNB.089 | System Admin | Generate and export audit log reports | **FIT** | Audit trail report | - | S |  |

## 6. Calculations (Appendix A, as written - to confirm)

| Line | Field | Formula |
|---|---|---|
| All lines | Pro-rata | Premium components x days covered / 365 (or 366) |
| All lines | Short-term period rate | Premium x short-period % by months/days covered (cancellation before expiry) - table needed |
| All lines | Minimum premium | Applied when TSI below package threshold; not for endorsements or pro-rata (per product matrix) |
| Fire | Net premium | TSI x premium rate |
| Fire | DST | 12.5% of net premium; rounding: .01-.49 -> .50, .51-.99 -> next 1.00 |
| Fire | PTX (premium tax) | 12% of net premium  [note: statutory PH premium tax for non-life is generally 2% / VAT 12% - to confirm] |
| Fire | FST (fire service tax) | 2% of net premium |
| Fire | LGT | Rate depends on insurer branch |
| Fire | Total charges / Gross premium | DST + PTX + FST + LGT (+OTH); gross = net + charges |
| Fire | Commission | Commission rate x net premium |
| Fire | VAT on commission | 12% of commission |
| Motor | OD/Theft coverage | Annual: 90% of TSI; multi-year: 81% of TSI  [to confirm which column applies] |
| Motor | OD/PV rate | Premium rate / 100 |
| Motor | OD/Theft premium | OD/Theft coverage x OD/PV rate |
| Motor | BI / PD premium | Excess BI / PD premium amounts per limit table |
| Motor | Basic premium | OD/Theft + BI + PD premium |
| Motor | DST | 12.5% of basic premium with same rounding rule |
| Motor | VAT | 12% of basic premium |
| Motor | LGT | Basic premium x LGT rate (by insurer branch) |
| Motor | Total premium | Basic premium + DST + VAT + LGT |

## 7. Non-functional requirements

| Topic | BRD | Approach | Fit |
|---|---|---|---|
| Users | 485 named users (392 Marketing AO/TL, 82 Processing, 3 System Admin); max concurrent 145 | Platform is stateless; horizontal scaling (k8s HPA). Size for 150 concurrent + 20%/yr growth | CONFIGURE |
| Volume | 21,200 transactions/month across quotation, client, account, upload, booking; endorsements 1,300/month; +20%/yr | ~1,000/day; bulk jobs chunked (500 rows/commit) | FIT |
| Response time | 10 seconds for all listed functions | Target p95 < 3 s online; bulk jobs asynchronous with progress | FIT |
| Peak hours | 08:00-10:00 and 15:00-17:00 | Run batch booking / e-mail dispatch outside peaks | CONFIGURE |
| Availability | 07:00-22:00 Mon-Sat; other items 'follow existing QPS set-up' | Maintenance window outside service hours; HA deployment | CONFIGURE |
| Retention | Application, DB, audit logs and historical data: 5 years online, 15 years archive, backup every 4 hours, backup retention 5 years | Archive tablespace/object storage + retention job (BRNB.106); DB backups every 4h | NEW |
| Security | All outbound documents encrypted / password protected; insurers have no system access | PDF/XLSX encryption; no external user roles | NEW |
| Devices | Any BDO-issued device / workstation | Browser-based; optional IP allow-list / SSO | FIT |

## 8. Products

Annex I lists 67 Other Lines risk codes (CAR, CGL, CTP, EEI, EPF, GIP, INL, MOP/MON, PA, TRP). Appendix B lists Motor product matrices MTR08-MTR38 and Property matrices PAR01-PAR25. The matrix files themselves are not in the BRD (Q01). Product lines (Annex II):

- **Property**: Fire and Lightning only; All-Risk; Broad-Named Perils
- **Motor**: TPL only (no L/D cover); Comprehensive
- **Engineering**: CAR; EAR
- **Liability**: CGL; CPL; Employers; Products; Professional; D&O; E&O; Cyber; Contaminated Products; Pollution Legal; Contractor's Pollution
- **Crime**: MSP; FG; MSP + FG; DDD; BBB
- **Property / Equipment Floater**: -
- **Marine**: Inland; Inter-Island; Import; Export; combinations; Project Cargo; Charterer's Liability; Terminal Operators Liability; Freight Forwarder's Liability; Stock Throughput; Trust Receipt
- **Marine Hull**: Hull and Machinery; Ship Repairer's Liability; Builder's Risk; Protection & Indemnity
- **Aviation**: General Aviation; Drone; Airport Contractor's Liability; Airport Owner's & Operator's Liability; Aviation Products Liability; Aviation Refuellers Liability; Hangar Keepers Liability; Pilot Loss of License
- **Personal Accident**: Individual PA; Group PA; Marine PA; Travel PA
- **Electronic Equipment**: -
- **Others / Miscellaneous**: S&T; PV; Trade Credit; Parametric; Golfer's / Hole-in-One; Fine Arts; Jewelers Block

## 9. Open questions for BDOI

| Q# | Topic | Question | Related |
|---|---|---|---|
| Q01 | Product master | Please share the product matrix files (MTR08..MTR38, PAR01..PAR25, other risk codes) referenced in Appendix B; the SharePoint link is internal to BDO. | BRNB.001-003, 093 |
| Q02 | Product master | Minimum mandatory fields per product line / cover type (Annex II: Property, Motor, Engineering, Liability, Crime, Floater, Marine, Hull, Aviation, PA, EEI, Misc.). | BRNB.002, 003, 093 |
| Q03 | Templates | Sample layouts for IDF, FFYMI quotation, Quotation Slip, Proposal Slip, Comparative Table, Placement Slip, Hold Cover request, Insurance Advice, Service Invoice. | BRNB.004, 008, 017, 069, 070, 100 |
| Q04 | TSU routing | Exact TSU involvement criteria: package vs non-package list, fleet threshold (no. of units), multi-location threshold, TSI threshold, endorsement types. | BRNB.098 |
| Q05 | Approvals | Approval chain and limits for PRF (Marketing TL / TH / UH) and QS (TSU) - by amount, product or segment? | BRNB.005, 014 |
| Q06 | Insurer channel | How are insurers reached per insurer: e-mail only, SFTP, or insurer API? List of panel insurers and branch codes (LGT depends on insurer branch). | BRNB.008, 071 |
| Q07 | Encryption | Password convention for outbound documents (standard vs generated syntax, e.g. surname + birth date); which documents to clients vs insurers. | BRNB.013, 035, 077 |
| Q08 | Integration | Which 'BDOI systems' must receive NB data (core/accounting, CRM, GRF/ARF, data warehouse) and preferred interface (file vs API). | BRNB.015, 096 |
| Q09 | Renewals | Late Renewal Requests Report is renewal-related - confirm it stays in NB scope or moves to the Renewal BRD. | BRNB.018 |
| Q10 | Workflow | Confirm the master list of stages and statuses and allowed transitions per transaction type (NB individual, NB bulk, non-package, direct booking, endorsement). | BRNB.022, 115 |
| Q11 | HLS | HLS interface specification (format, fields, frequency, transport, source authentication) and other source systems for bulk (e.g. auto loans for FFYMI). | BRNB.023, 028 |
| Q12 | E-mail intake | Should the system read a shared mailbox for quotation requests automatically, or is manual capture with the e-mail attached sufficient? | BRNB.041 |
| Q13 | Client code | BRNB.029 requires a client code for every quotation; BRNB.063 allows quotation without client code. Confirm: auto-create prospect code at quotation? | BRNB.029, 063 |
| Q14 | Sanitation | Defined sanitisation criteria for bulk lists. | BRNB.064 |
| Q15 | Reference no. | Format of the single Account Reference Number (prefix, segment, year, sequence) and whether PRF marketing reference is the same number. | BRNB.006, 102 |
| Q16 | Client / KYC | BDO 'bank standard' client data and KYC fields; is there a BDO CIF to integrate with (source of truth for bank clients)? | BRNB.030, 065, 090, 092 |
| Q17 | Dedupe | Duplicate-client keys and match precedence (name + DOB, ID number, e-mail, mobile, TIN). | BRNB.032 |
| Q18 | Tags | List of client/account tags and special-instruction types, and which processes must enforce them (block vs warn). | BRNB.091 |
| Q19 | CRM | Lead management scope: do leads come from bank channels (referrals) and is lead-to-prospect conversion required in NB phase? | BRNB.099 |
| Q20 | Prospect | Prospect vs confirmed client: which event confirms (KYC complete, first booking)? | BRNB.101 |
| Q21 | KYC review | KYC review frequency by risk rating for non-bank clients; 'shared path' - is an in-system report acceptable? | BRNB.110 |
| Q22 | Duplicates | Fire duplicate rule 'same client ID + location + items' - exact or normalised address match? Motor: CTPL vs MTR exception confirmed. | BRNB.051, 066 |
| Q23 | Files | Maximum file size and full list of allowed file types; file-naming syntax. | BRNB.026 |
| Q24 | Extraction | Which documents need auto-extraction (e-policy per insurer?) and which fields; are insurer e-policy layouts stable? | BRNB.074, 104, 105 |
| Q25 | Reactivation | Reactivate cancelled placement (BRD 2.1.16) has no BRNB ID - confirm in scope. | BRD 2.1.16 |
| Q26 | Placement | Prerequisites before placement per segment (payment received, client confirmation, docs complete, TSU clearance). | BRNB.069 |
| Q27 | Hold cover | Which accounts need an automatic 30-day hold cover request, and what happens at expiry without confirmation? | BRNB.072, 103 |
| Q28 | CLPC | CLPC billing file and payment report layouts, transport and schedule; payment report for other segments (bank debit, OTC, online). | BRNB.067, 068 |
| Q29 | Direct payment | Direct-payment criteria and accounting: does BDOI book only commission receivable from insurer for these? | BRNB.114 |
| Q30 | Insurance Advice | IA trigger point and recipient (mortgagee bank unit) for mortgaged accounts. | BRNB.070, 095 |
| Q31 | E-policy receipt | How are e-policies received (insurer e-mail to shared mailbox, SFTP, portal download)? | BRNB.073 |
| Q32 | Accounting | Booking accounting entries (premium receivable/payable to insurer, commission income, VAT, EWT) and service invoice rules - is commission invoiced per booking or monthly per insurer? | BRNB.027, 100 |
| Q33 | Incentives | Incentive qualification rules and whether incentive computation/payout is in scope. | BRNB.107 |
| Q34 | Cost center | Cost center master data source and default rule (by AO, branch, segment). | BRNB.108 |
| Q35 | Multi-year | Multi-year: premium paid upfront or yearly? Commission recognition per year or upfront? 81% vs 90% OD/Theft basis. | BRNB.112 |
| Q36 | FFY | Free First-Year: who pays the first-year premium (dealer / bank), FFY period length and renewal hand-off. | BRNB.113 |
| Q37 | Data ownership | Authoritative system per data element (client, vehicle, property, address, policy, invoice). | BRNB.092 |
| Q38 | GRF/ARF | Describe the existing GRF and ARF request forms to replicate their behaviour. | BRNB.096 |
| Q39 | Retention | Retention periods per record type and status (not proceeded, abandoned, voided) and purge vs archive. | BRNB.106 |
| Q40 | Dynamic reports | Expected scope of 'dynamic reports' (ad-hoc column/filter builder vs saved report variants). | BRNB.057 |
| Q41 | Production stats | Region / department / team hierarchy and target values per stage for production statistics. | BRNB.075 |
| Q42 | Login | Authentication: BDO Active Directory / SSO? Clarify '30-min warning prior to system-triggered logout' (absolute session length?). | BRNB.040 |
| Q43 | Taxes | Appendix A shows PTX 12% of net premium for Fire; please confirm premium tax / VAT basis per line and whether BDOI computes taxes or copies insurer figures. | Appendix A |
| Q44 | Scope boundary | Broker model: confirm insurer-only platform functions (reinsurance, technical reserves, IBNR, claims reserving) are not required for BDOI. | Platform |

### 9.1 Answered by later BRDs

The rows above are kept as asked. This section records what the later BRDs (BRD-6 to BRD-12, then BRD-00 Core
Replacement and BRD-13 Data Migration) answer. The single
record of every cross-BRD answer, with the design impact, is
[`BDOI_CROSS_BRD_DECISIONS.md`](BDOI_CROSS_BRD_DECISIONS.md) section 3. Status: **answered**, **partial** (the question
stays open for the rest) or **open**.

| Q# / ID | Status | Answering BRD and ID | Answer (short) |
|---|---|---|---|
| Q42 | answered | BRD-11 UAM (p.13-14, 17; UAM-NFR-11, 17, 33) | Directory sign-in is required: Windows ID against BDO EUA, LDAP / AD or SSO. Built as the parked port `DirectoryAuthenticator`; local sign-in stays until BDO supplies the interface (UQ04). The 30-minute warning is an inactivity log-out, with a warning after 15 minutes. BRD-7 (NFR 1.01) and BRD-9 (BRCSF-001) restate the question; BRD-11 governs |
| OOS-1 | answered | BRD-6 RN (1.011.1, BRRN.023/031/035) | The renewal overrides are: the Marketing TL overrides accounts with an outstanding balance, and an authorised user overrides a system disposition or bucket with mandatory remarks. No approver selection or push step (`RNW_OVERRIDE`, `rnw_override`) |
| BRNB.097 | answered | BRD-6 RN (BRRN.033), BRD-8 EB (BRID-022.01), BRD-12 SP (BRIDSP-26/27) | Business type NEW_BUSINESS / RENEWAL is required on the account at creation, carried to the invoice, and a filter on the reports. One shared change for the three BRDs: work item BT0 (`V822__account_business_type.sql`, `InvoiceBuilder` reads the account's type) |
| Q09 | open | BRD-6 RN (RQ29); BRD-12 SP (Report List #182) | Not answered. The RN reports have no late-renewal-request report; Report List #182 is a *package* late-renewal report (TSU). BRNB.018 stays parked; proposal: a variant of `RNW-LISTING` once BDOI confirms (RQ29) |
| Q06 | partial | BRD-8 EB (BRID-005, 005.01-005.02, 009) | For EB, insurers use a secure portal or API. Other lines: still e-mail |
| Q07 | partial | BRD-8 EB (BRID-007) | A standard assigned password or a system-generated one following a defined syntax, sent separately; the convention itself is still missing (EBQ09). BRD-6 needs it for RAs too |
| Q08 | partial | BRD-9 CSF (p.3 footnote 3) | QPS and EBIX must receive client contact updates during coexistence (`ContactSyncGateway` outbox, CSQ01). Other targets still open BRD-13 Data Migration (p.5): after cutover legacy is read-only and no feed to QPS / EBIX is required by that BRD (conflicts with the CSF footnote; CSQ01). |
| Q10 | partial | BRD-6 RN (personas, tabs, 34 counters, BRRN.040) | Renewal stages and statuses (workflow `RNW_CASE`); counter mapping to confirm (RQ10) |
| Q11 | partial | BRD-12 SP (p.4-5, Report List #138 / #151) | Sources named: LFS insurance report, HLS daily insurance report, CIU report, SPI list, LAMD report, Loan Booking Report, IA masterlist; all Excel today. Layouts and transports still open (SP SQ01) |
| Q14 | partial | BRD-6 RN (BRRN.020); BRD-12 SP (Report List #151) | Renewal: sanitation is rule-based in the system, criteria "agreed" but not listed (RQ01). Submitted policies: PN vs LAMD match, FFY / employee / No Touch exclusions, loan status, duplicate checks, PN = serial / motor / assured checks |
| Q15 | partial | BRD-6 RN (BRRN.022, 1.003.4.1.1.2) | A mandatory unique renewal reference is the matching key of every renewal record; no format given (`RNW-<yyyy>-nnnnnn` as a parameter) |
| Q18 | partial | BRD-10 SANC (SNSRP-302/303) | `PEP` and high-risk tags are set by screening rules and trigger reviews, not blocks; the block stays a proposal behind a parameter (SANC SQ07) |
| Q21 | partial | BRD-6 RN (BRRN.028); BRD-10 SANC (SNSRP-102, 303) | KYC due is flagged for visibility only and never blocks renewal; Compliance defines risk categories in the system and high-risk / PEP clients with an active policy get a KYC review / EDD case. The frequency per rating is still not given |
| Q23 | partial | BRD-8 EB (BRID-002, 005.01, 014); BRD-9 CSF (BRCSF-007); BRD-10 SANC (SNSRP-601) | File types: PDF, Word, Excel and other agreed formats (EB); doc, docx, txt, xls, xlsx, ods, csv, PNG, JPG, HEIF, PDF (CSF). Naming: `<Form Type>_<Client Name>_<Date Received>_<Document Type>_<n>` for screening documents only. Maximum size and the general naming syntax still open |
| Q24 | partial | BRD-12 SP (BRIDSP-02) | Submitted policy documents (scanned or printed) are extracted and confirmed by a user; scanned input implies OCR (parked port) |
| Q27 | partial | BRD-6 RN (1.009.3.1.33-34); BRD-12 SP (p.5, BRIDSP-24/32) | Hold covers apply to renewals too; submitted CBG Motor / Fire renewals go to the insurer with a 30-day hold cover request, accepted within 3-5 days, else follow up or re-assign; unbooked hold covers are alerted. Expiry handling still open |
| Q28 | partial | BRD-12 SP (Report List #65-67) | Three CLPC billing variants with sample files. Transport and schedule still open |
| Q31 | partial | BRD-8 EB (BRID-005.01) | For EB, insurers upload policy forms and billing through the portal. Other lines still open |
| Q36 | partial | BRD-12 SP (BRIDSP-13/14, Report List #156) | At renewal an FFY account gets an FFY-specific RA template; FFY is both a renewal and a non-renewal bucket (SP SQ05). The payer is not stated |
| Q37 | partial | BRD-7 CLM (BRCLM.007, 039); BRD-9 CSF (p.3, BRCSF-002) | The policy number flows from the policy system to Claims, which must not re-key it; QPS and EBIX are today's systems of record for client contact data during coexistence BRD-13 (p.5): from cutover the new Core holds the trusted client master and the governed reference data. |
| Q39 | partial | BRD-7 CLM (p.41); BRD-8 EB (NFR); BRD-9 CSF (NFR); BRD-10 SANC (p.25) | Claims 10 years online / 15 archive; EB 5 / 15; CSF 5 / 15 (backup retention 5 years); screening records 5 / 5. Other record types, and archive vs purge, still open; BRD-11 says "follow QPS" (UQ12) BRD-00 Core Replacement (p.45): 5 years online and 15 years offline for application, database and audit logs and historical data, the platform default; BRD-7 and the 5 / 5 BRDs remain exceptions (CRQ22). BRD-13 defers to the consolidated NFR. |
| Q40 | partial | BRD-8 EB (BRID-022 AC9); BRD-12 SP (Report List #74) | "Customise needed reports (date range, client, product type)"; "user-defined filters as needed; export to Excel / PDF; no defined template". Saved variants plus column filters meet it; no query builder BRD-00 (BR-053, BR-054, capability 21) asks for more: customised reports with user-chosen fields, charts and summaries, scheduled or on demand (CRQ11; Core wave CR-W2). |
| Q44 | answered (for claims) | BRD-7 CLM (BRCLM.023/024) | BDOI records the insurer's reserve and settlement as information only; no reserving, no journal. The insurer-side `claims` module stays hidden from BDOI roles BRD-00 (goal 1, capability 15): Reinsurance is a BDOI business line whose BRD is phase 2; for phase 1 the insurer-side modules stay hidden. |
| BRNB.085 | extended | BRD-11 UAM (sections A-B) | Access requests gain a Requestor persona, drafts, return, cancel, chosen approver and bulk (`nbadmin` lifecycle) |

Not answered by BRD-6 to BRD-12: Q01-Q05, Q12, Q13, Q16 (BDO CIF; BRD-9 and BRD-10 restate it), Q17, Q19, Q20, Q22,
Q25, Q26, Q29, Q30, Q32-Q35, Q38, Q41, Q43.

## 10. Observations on the BRD pack

- Pages 1-22 and 23-44 are the same signed workshop addendum.
- BRNB.029 (a client code is mandatory for a quotation) conflicts with BRNB.063 ("allow quotation creation without client code"); see Q13.
- "Reactivate cancelled placed accounts" (BRD 2.1.16) is in the role matrix but has no BRNB ID; see Q25.
- The workshop addendum has an unnumbered row, "send generated service invoice", recorded here as BRNB.100b.
- BRNB.089 criterion 1 repeats "assign cancel placement function"; this is read as "generate audit log report".
- Appendix A gives Fire premium tax as 12% of net premium, and gives two OD/Theft bases (81% and 90% of TSI); see Q35 and Q43.
- The data retention NFR differs between the addendum (5 years online / 15 years archive) and the original BRD ("follow QPS"). The addendum takes precedence.
