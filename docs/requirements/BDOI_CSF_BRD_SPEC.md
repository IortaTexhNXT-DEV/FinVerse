# BDOI Customer Servicing Facility (BRD-9) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse (BIBS - BDOI Broker System).

Status: **for BDOI concurrence.** Build design: [`CUSTOMER_SERVICING_DESIGN.md`](../architecture/CUSTOMER_SERVICING_DESIGN.md). The review workbook will be consolidated with the other BRDs.

File references in the "Current capability" column are relative to `backend/src/main/java/com/iortatechnxt/brokerverse/` unless they start with `frontend/`.

## 1. Source documents

Source: `docs/source-documents/Customer Servicing Facility.PDF` (18 PDF pages). The BRD is numbered "n of 16"; pages 17-18 are an e-mail thread attached to it.

| PDF pages | Document | Content |
|---|---|---|
| 1-2 | Cover and revision log | Prepared by Zean C. Ibay (ESG-BPS); v1.0, 04-Jun-2025 (initial) and 09-Jun-2025 (key capabilities, business requirements, usage requirements) |
| 3-5 | Executive summary, objectives, benefits, current process, envisioned process | Replace the "BDO-Insure Front-End System" used by the BDO Insure Contact Center; data sources QPS and EBIX (footnotes 2 and 3) |
| 6 | 11 business key capabilities | Authentication, client and account information, search, contact details, payment history, RA resend, uploads, current client information, policy documents, change recording, audit trail report |
| 6-12 | Business requirements | **BRCSF-001 to BRCSF-011** with process steps (1.001, 1.002, 3.001, 5.001, 6.001, 7.001, 9.001, 11.001, 11.002) |
| 13-14 | Usage requirements | Volumes, response times, peak, availability, RTO / RPO, retention |
| 15-16 (scanned) | Approval sheet | Prepared 18-Jun-2025; inputs from Contact Center Management and Marketing Business Services; approved by the Alternative Distribution Head (24-Jun-2025), the Product Owner (24-Jun-2025), Unit Head Combank / Corbank (e-mail sign-off 27-Jun-2025), Head Retail Marketing, Head Corporate and Retail Marketing (02-Jul-2025); Head Comptrollership annotated "signature not required" |
| 17-18 | E-mail thread "Core Modernization: CSF", 22-Jan-2026 to 13-Feb-2026 | Minutes of the 23-Jan-2026 session (12 topics). The BA confirms on 13-Feb-2026 that the e-mail **supports the inclusion of item 7 (two additional statuses) and item 9 (e-policy sending)**; items 10 (case management) and 11 (open items) are **future enhancements** |

Pages 15-16 were rendered and read as images. All other pages have a text layer.

**Precedence.** The signed BRD is the baseline. The e-mail of 13-Feb-2026 adds two requirements (rows CSF-EM07 and CSF-EM09) and defers case management (row CSF-EM10, OUT). Other e-mail topics (verification before update, search keys, payment history depth, RA passwords) clarify existing rows and are recorded there; the open items of topic 12 become questions.

## 2. Business summary

**Who uses it.** The **BDO Insure Contact Center**: agents answer clients who call the hotline, write e-mails or use the website (e-mail topic 2). Supervisors and Contact Center Management personnel print audit reports; Leads / Heads run reports (p.13).

**What they do today.** The BDO-Insure Front-End System (the e-mail calls it IBS) was built by BDO IT. It shows full insurance account details only for Consumer Banking Group (CBG) accounts; for non-CBG accounts it shows only contact details, account number and expiry date. Agents can update only client contact details. The infrastructure is end-of-life and there is no role-based access control (p.4).

**What they need.** One servicing screen where an agent:
- signs in with a user ID and password (BRCSF-001);
- searches a client by name, client ID, account number, promissory note (PN) number or application number (BRCSF-003);
- sees the current client information and every insurance account with its status (Pending, Awaiting, Booked, Open / Closed) (BRCSF-002, 005.001, 008, e-mail item 7);
- sees the payment history, typically the last 11-12 months (BRCSF-005, e-mail topic 6);
- after verifying the caller's identity (e-mail topic 5), adds or updates the **contact details only** (BRCSF-004; other changes such as civil status go to the fulfilment unit);
- views, downloads and resends the Renewal Advice (RA) and the e-policy (BRCSF-006, e-mail item 9);
- retrieves policy-related documents (quotations, RAs, claims reports) and uploads documents (BRCSF-007, 009);
- relies on the system to record every change and print or save the audit trail report (BRCSF-010, 011).

**Position in BIBS.** The BRD was written before BIBS, for a front end reading QPS and EBIX and writing contact updates back to them. In BIBS, the client, account, invoice, payment and document data already live in BrokerVerse modules (crm, account, placement, issuance, booking, opsledger, cashiering), so CSF is a **read-mostly servicing workspace over the BIBS modules**, not an integration layer. Contact updates are made once, in the BIBS client master. Writing them back to QPS / EBIX matters only while the legacy systems coexist, and is parked behind a port (CSQ01). CSF is small: one new module (`csf`), a contact-update contract on `crm`, a few read contracts, and platform tweaks (file types).

## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 8 |
| CONFIGURE | Set-up only | 1 |
| CHANGE | Extends an existing capability | 11 |
| NEW | New build | 2 |
| OUT | Out of scope per the BRD pack | 1 |
| **Total** | | **23** |

### Rows per section and fit

| Section | Name | Rows | FIT | CONFIGURE | CHANGE | NEW | OUT | Size S/M/L |
|---|---|---|---|---|---|---|---|---|
| A | Access and authentication | 3 | 3 | 0 | 0 | 0 | 0 | 3/0/0 |
| B | Client and account information, search, status, payment history | 7 | 1 | 0 | 6 | 0 | 0 | 4/3/0 |
| C | Contact details maintenance | 1 | 0 | 0 | 1 | 0 | 0 | 0/1/0 |
| D | Renewal advice, e-policy and documents | 7 | 1 | 0 | 4 | 2 | 0 | 5/2/0 |
| E | Audit trail, reports and backup | 4 | 3 | 1 | 0 | 0 | 0 | 4/0/0 |
| F | Deferred by the e-mail of 13-Feb-2026 | 1 | 0 | 0 | 0 | 0 | 1 | - |
| | **Total** | **23** | **8** | **1** | **11** | **2** | **1** | **16/6/0** |

### Target modules

| Module | Rows |
|---|---|
| `csf` (new) | 11 |
| `crm` | 2 |
| platform (`security`, `audit`, `report`, `attachment`) | 8 |
| infrastructure (database backup) | 1 |
| OUT | 1 |

### Platform impact

| Area | Treatment | Impact |
|---|---|---|
| Security | Re-use | CSF permissions and roles (agent, supervisor, management) |
| crm | Extend | Contact-only update contract with reason, verification reference and source; optional additional contacts |
| Attachments | Extend | More file types (TXT, RTF, HEIC / HEIF, GIF, BMP, TIFF, WEBP) for BRCSF-007 |
| Read models | Re-use | Accounts, invoices, payment movements, receipts, e-policies, RAs and documents are read through the owning modules' query services |
| Messaging | Re-use | RA and e-policy resend with password protection and separate password e-mail |
| Audit and reports | Re-use | `CTL-AUDIT` plus one CSF report of contact changes |
| Legacy (QPS / EBIX) | Park | `ContactSyncGateway` port with an outbox; no transport until CSQ01 is answered |

## 4. Servicing flow in BIBS (for concurrence)

| Step | Who | What happens | BRD |
|---|---|---|---|
| Sign in | Agent | BIBS login; the CSF workspace is the landing page of the agent role | BRCSF-001 |
| Find the client | Agent | One search box: name, client code or ID number, ARN / policy no. / legacy account no., PN no., loan application no. | BRCSF-003 |
| Servicing view | Agent | Client summary; accounts with a CSF status; payments of the last 12 months; RAs; e-policies; documents | BRCSF-002, 005, 008, 009 |
| Verify the caller | Agent | Checklist (address, contact number, e-mail, insured property) before any change | E-mail topic 5 |
| Update contact | Agent | Contact fields only, with a reason; saved to the client master; audit and change log; queued for legacy sync while QPS / EBIX coexist | BRCSF-004, 010 |
| Resend | Agent | RA or e-policy to the registered e-mail, protected, password in a separate e-mail | BRCSF-006, e-mail item 9 |
| Attach | Agent | Upload a document to the client or an account | BRCSF-007 |
| Audit | Management | Audit trail and contact change reports, PDF / Excel | BRCSF-011 |

## 5. Requirements and fit/gap

### A. Access and authentication

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCSF-001<br><sub>p.6</sub> | System Admin | Restrict access to authorised users | Only authorised users log in with a valid user ID and password; access denied on wrong credentials; unauthorised attempts refused | **FIT** | JWT login, roles and permissions, lockout, `@PreAuthorize` on every endpoint (`security/service/AuthService.java`, `config/SecurityConfig.java`) | Re-use; CSF roles and permissions seeded (design section 6). BDO SSO stays parked (BRD-1 Q42) | platform (`security`) | S | CSQ10 |
| BRCSF-001 / 1.001<br><sub>p.6-7</sub> | Contact Center Agent | Log in with user ID and password | User accesses the system. Negative: wrong credentials, access denied | **FIT** | Sign-in page and `/api/v1/auth/login` | Re-use; landing page of the CSF roles = CSF workspace (`navigation/access.landingPath`) | platform (`security`) | S | - |
| BRCSF-001 / 1.002<br><sub>p.7</sub> | System | Validate credentials and grant access | Valid credentials accepted and access granted | **FIT** | `security/service/AuthService.java` | Re-use | platform (`security`) | S | - |

### B. Client and account information, search, status, payment history

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCSF-002<br><sub>p.7</sub> | Contact Center Agent | Access client and account information from the data source systems to view and update customer details | Client information retrieved and viewed; retrieved, updated and saved; retrieved from the data source systems; contact updates reflected automatically in the client or account database. Negative: failure to retrieve or update | **CHANGE** | Client master and 360 view (`crm/service/Client360Service.java`), accounts by client (`account/service/AccountQueryService.java` `byClient`), invoices per ARN (`opsledger/service/InvoiceLedgerQueryService.java` `forArn`) | CSF servicing view composed from the owning modules (no copy of the data); contact updates go to the client master through the new crm contract (BRCSF-004), so every BIBS module sees them at once; QPS / EBIX write-back through the parked `ContactSyncGateway` (CSQ01); every segment is visible, CBG and non-CBG (the key limitation of today's system, p.4), with no segment scoping unless BDOI asks (CSQ10) | `csf` | M | CSQ01, CSQ10 |
| BRCSF-003<br><sub>p.7-8</sub> | Contact Center Agent | Search client details by name, client ID number, account number, promissory note number or application number | Accurate and relevant results for the criteria. Negative: wrong or no results | **CHANGE** | Client search by text and code (`crm/service/ClientSearchService.java`); account search by ARN, PN, vehicle, location (`account/service/AccountSearch.java` `pnNumber`); pre-booked lookup by ARN, policy no. or PN (`AccountQueryService.preBooked`); loan application no. only on CLPC billing items (`placement/domain/BillingItem.java`) | `CustomerSearchService`: one query dispatched by key type (NAME, CLIENT_ID = client code / prospect code / government ID no., ACCOUNT_NO = ARN / policy no. / legacy account no., PN_NO, APPLICATION_NO); new read contract `PlacementQueryService.arnsByLoanApplication(companyId, no)`; results grouped by client with the matching accounts; the search itself is logged (who searched what) | `csf` | M | CSQ02 |
| BRCSF-003 / 3.001<br><sub>p.8</sub> | Contact Center Agent | Search for a client | Client found; accurate display of client details. Negative: not found, inaccurate display | **CHANGE** | As BRCSF-003 | Result list with an explicit "No client found for <criteria>" state (e-mail topic 4: negative scenarios clearly indicated) and a direct open of the servicing view when exactly one client matches | `csf` | S | CSQ02 |
| BRCSF-005<br><sub>p.8</sub> | Contact Center Agent | View payment history | Payment history correctly displayed. Negative: incorrect or missing | **CHANGE** | Invoice ledger movements with payment status (`opsledger/service/InvoiceLedgerQueryService.java` `movements`, `opsledger/domain/PaymentStatus.java`); cashiering receipts (`cashiering/service/CashReceiptService.java`) | Payment history per client and per account: receipts (AR / OR no., date, mode, amount), applications to invoices, current balance and payment status of each invoice; default window `CSF_PAYMENT_HISTORY_MONTHS` (12, e-mail topic 6) with "show older"; new read contract `InvoiceLedgerQueryService.paymentsOfClient(companyId, clientCode, from)` | `csf` | M | CSQ05 |
| BRCSF-005 / 5.001<br><sub>p.8</sub> | Contact Center Agent | View the status (Pending, Awaiting) | Latest status shown correctly | **CHANGE** | Account status mirrors the `NB_ACCOUNT` case (DRAFT, SUBMITTED, AWAITING_PAYMENT, READY_FOR_PLACEMENT, PLACED, POLICY_ISSUED, BOOKED, CANCELLED ...); invoice payment status in the ledger | CSF status per account computed by `CsfStatusMapper` from the account stage and the invoice payment status, with the mapping in LOV `CSF_STATUS_MAP` (maintainable, so the status definitions of Marketing, Processing and Operations can be aligned without code, e-mail open item 3); the BIBS stage is shown next to it | `csf` | S | CSQ04 |
| CSF-EM07<br><sub>p.17 (e-mail item 7, included 13-Feb-2026)</sub> | Contact Center Agent | Two additional statuses: "Booked" and "Open / Closed" | Statuses follow the definitions of Marketing, Processing and Operations | **CHANGE** | Account BOOKED stage; invoice fully paid / cancelled | Values BOOKED, OPEN, CLOSED in `CSF_STATUS_MAP` (seeded: BOOKED = account BOOKED; OPEN = in force and not cancelled; CLOSED = expired, cancelled or not renewed); definitions to confirm | `csf` | S | CSQ04 |
| BRCSF-008<br><sub>p.10</sub> | Contact Center Agent | View current client information | Client information correctly displayed | **FIT** | Client page and 360 view (`crm/service/Client360Service.java`, `frontend/src/features/crm`) | Re-use the client summary card (`components/broking/RecordSummary`) in the CSF view; the full client page is one click away for users with `CLIENT_VIEW` | `crm` | S | - |

### C. Contact details maintenance

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCSF-004<br><sub>p.8</sub> | Contact Center Agent | View, add or update client contact details | Users view, add or update contact details; changes saved and reflected. Negative: failure to save or reflect | **CHANGE** | `crm/service/ClientService.java` `update(id, ClientDetails)` updates every client field under `CLIENT_MAINTAIN` (too wide for agents); one e-mail, one mobile, one phone and one address per client (`crm/domain/Client.java`) | New crm contract `ClientService.updateContact(id, ContactChange)` limited to e-mail, mobile, phone and address, with reason, source (`CSF`) and verification reference, audited with before / after; CSF endpoint under `CSF_CONTACT_UPDATE`; verification checklist first (e-mail topic 5: address, contact number, e-mail, insured property; at least `CSF_VERIFY_MIN_MATCHES` must match, CSQ03); `csf_contact_change` keeps the change and the sync status; "add" = additional contacts in `crm_client_contact` (type, value, primary flag) if CSQ15 confirms; other fields (civil status ...) are refused with `CSF_FIELD_NOT_UPDATABLE` and routed to the fulfilment unit | `crm` + `csf` | M | CSQ03, CSQ14, CSQ15 |

### D. Renewal advice, e-policy and documents

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCSF-006<br><sub>p.9</sub> | Contact Center Agent | View and download to resend the RA to clients | Faster RA resend; RA correctly viewed and resent. Negative: failure to view or resend | **NEW** | No RA in BIBS yet: RAs will come from the Renewal module (Renewal BRD, V1010-V1019, being analysed) and the EB module (BRID-001); protected e-mail exists (`messaging/service/MessageService.java`) | RA list per client and account from the attachments of document type `RENEWAL_ADVICE` (the convention agreed with Renewal and EB, design section 11); view, download and **Resend** to the registered e-mail with protection (RA files are password protected, e-mail topic 8) and the password in a separate e-mail; resend log from the messaging outbox | `csf` | M | CSQ06 |
| BRCSF-006 / 6.001<br><sub>p.9</sub> | Contact Center Agent | Access the RA, select resend, confirm | RA sent successfully. Negative: RA not accessed, option not selected | **NEW** | As above | Resend dialog: recipient (registered e-mail by default; another address only with `CSF_RESEND_OTHER` and a reason, CSQ06), preview, confirm; success toast and log entry | `csf` | S | CSQ06 |
| CSF-EM09<br><sub>p.17 (e-mail item 9, included 13-Feb-2026)</sub> | Contact Center Agent | E-policy resending | (No criteria given) | **CHANGE** | E-policy dispatch to the client with encryption and separate password (`issuance/service/EpolicyDispatchService.java`, `issuance/api/DispatchController.java`, permission `EPOLICY_SEND`) | CSF "Resend e-policy" calls the issuance dispatch service for the account's confirmed e-policy under the CSF permission `CSF_RESEND` (no `EPOLICY_SEND` grant to agents); logged in the dispatch report | `csf` | S | CSQ13 |
| BRCSF-007<br><sub>p.9</sub> | Contact Center Agent | Attach or upload documents: text files (doc, docx, txt ...), spreadsheets (xls, xlsx, ods, csv ...), images (PNG, JPG, HEIF ...) and PDF | System prompts for the file, accepts, stores, document accessible. Negative: upload not selected, file not chosen, upload failed, not confirmed | **CHANGE** | Attachments on any entity with type and signature checks; allowed today: PDF, PNG, JPEG, XLSX, DOCX, CSV, ODS, ODT, XLS, DOC, MSG, EML (`attachment/domain/AllowedFileType.java`); no TXT, RTF, HEIC / HEIF, GIF, BMP, TIFF, WEBP | Add TXT, RTF, HEIC / HEIF, GIF, BMP, TIFF and WEBP to `AllowedFileType` with signatures; CSF uploads to the client or an account with document type (LOV `CSF_DOCUMENT_TYPE` subset of `DOCUMENT_TYPE`); confidentiality through the attachment access classes (EB design, BRID-025) | platform (`attachment`) | S | CSQ07, CSQ08 |
| BRCSF-007 / 7.001<br><sub>p.9-10</sub> | Contact Center Agent | Select upload, choose file, upload, confirm | Documents correctly stored and accessible | **FIT** | `frontend/src/components/attachments/Attachments.tsx`, `AttachmentUploadBar.tsx`; `/api/v1/attachments` | Re-use the component on the CSF view | platform (`attachment`) | S | - |
| BRCSF-009<br><sub>p.10</sub> | Contact Center Agent | View, retrieve or download all uploaded policy-related documents, including quotations, renewal advices and claims reports | Documents correctly retrieved, displayed and accessible. Negative: failure to retrieve | **CHANGE** | Attachments per entity (`attachment/service/DocumentService.java`), ZIP download; quotation PDFs (`quotation`), e-policies (`issuance`); claims reports will come from the broking Claims BRD (V1020-V1029) | Documents tab of the CSF view: every attachment linked to the client, its accounts, quotations / PRFs and invoices, grouped by type (quotation, RA, e-policy, claims report, other), with preview, download and ZIP; access classes apply | `csf` | M | CSQ07 |
| BRCSF-009 / 9.001<br><sub>p.10</sub> | Contact Center Agent | Search the client, access the uploaded documents, retrieve a document | Client found, documents accessed and retrieved | **CHANGE** | As above | Same view; each download is logged | `csf` | S | - |

### E. Audit trail, reports and backup

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRCSF-010<br><sub>p.11</sub> | System | Record changes to client information as an audit trail | Changes correctly recorded | **FIT** | `audit/service/AuditTrailService.java` on every change; crm note history (`crm/domain/NoteHistory.java`) | Re-use; the contact update writes the audit entry with before / after values (BRCSF-004) | platform (`audit`) | S | - |
| BRCSF-011<br><sub>p.11</sub> | System | Print or save the audit trail report | Print or save an electronic copy; correctly printed / saved | **FIT** | `CTL-AUDIT` (`report/gl/AuditTrailReport.java`) with PDF / XLSX / CSV export; Administration > Audit Trail download buttons | Re-use; plus the CSF report `CSF-CONTACT-CHANGES` (design section 9) for the contact-change view Management asks for | platform (`report`) | S | - |
| BRCSF-011 / 11.001<br><sub>p.11</sub> | System | Back up every 15 minutes to a designated secure location, complete and restorable, without degrading performance | Backups every 15 minutes, timestamped, complete, restorable; no performance impact. Negative: failure, corruption, degradation | **CONFIGURE** | Application-level data is in PostgreSQL; backups are an infrastructure service | Continuous WAL archiving with point-in-time recovery (archive at most every 15 minutes, `archive_timeout = 900`) plus scheduled base backups, restore tests; applies to the whole BIBS database, not CSF only. Recorded for the infrastructure team | infrastructure | S | CSQ11 |
| BRCSF-011 / 11.002<br><sub>p.12</sub> | Contact Center Management | Access the audit trail, choose print / save in Excel or PDF, confirm | Audit trail retrieved; print / save prompted; report printed / saved | **FIT** | Report runner export PDF / XLSX (`report/core`) | Re-use; `AUDIT_VIEW` and `CSF_REPORT_VIEW` granted to the management role | platform (`report`) | S | - |

### F. Deferred by the e-mail of 13-Feb-2026

| BR ID<br><sub>page</sub> | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|
| CSF-EM10<br><sub>p.17 (e-mail item 10)</sub> | Contact Center | Case management: case logging (today in SharePoint as an interim solution), case tracking and performance monitoring | - | **OUT** | - | Future enhancement per the e-mail of 13-Feb-2026; a separate requirement document will follow. The workflow engine and queues can host it later | - | - | CSQ09 |


## 6. Personas, volumes and statuses from the BRD

| Persona | Source | Use |
|---|---|---|
| System Admin | BRCSF-001 | Access control |
| BDO Insure Contact Center Agents | BRCSF-001-009 | Servicing |
| BDO-Insure Contact Center Supervisors and BDO Contact Center Personnel | p.13 (concurrent access) | Servicing and supervision |
| Contact Center Management Personnel | BRCSF-011.002 | Audit trail print / save |
| BDO Insure Leads / Heads | p.13 (report generation) | Reports |

| Business process | Users | Max concurrent | Transactions a year | Growth | Response time |
|---|---|---|---|---|---|
| Concurrent user access (agents, supervisors, contact center personnel) | 24 | 24 | 144,400 | 1% | < 3 s |
| Data retrieval and updates | 24 | 24 | 144,400 | 1% | < 3 s |
| Search | 24 | 24 | 144,400 | 1% | < 3 s |
| Document management | 16 | 16 | 113,800 | 1% | 5 s |
| Report generation (Leads / Heads) | 8 | 8 | as needed | - | 5 s |

Statuses: Pending, Awaiting (BRCSF-005.001); Booked, Open / Closed (e-mail item 7).

Search keys (BRCSF-003, e-mail topic 4): name, client ID number, account number, PN (promissory note) number, application number.

Verification data (e-mail topic 5, not baselined as a requirement): address, contact number, e-mail, insured property details.

## 7. Reports

| Code | Report | BRD | Permission |
|---|---|---|---|
| `CTL-AUDIT` (existing) | Audit trail, filtered by entity type Client or user | BRCSF-011 | AUDIT_VIEW |
| `CSF-CONTACT-CHANGES` | Contact changes: client, field, old / new value, verification checks passed, agent, time, reason, legacy sync status | BRCSF-010, 011 | CSF_REPORT_VIEW |
| `CSF-ACTIVITY` | Agent activity: searches, views of servicing records, resends (RA, e-policy), uploads, contact changes per agent and day | p.13 (Leads / Heads report generation) | CSF_REPORT_VIEW |

## 8. Non-functional requirements

| Topic | BRD (p.13-14) | Approach | Fit |
|---|---|---|---|
| Users | 24 concurrent (agents, supervisors, personnel); 16 document users; 8 report users | Small load | FIT |
| Volumes | 144,400 transactions a year (about 600 per working day), 1% growth; 113,800 document transactions | Indexed search keys; paged results | FIT |
| Response time | < 3 s retrieval, update and search; 5 s documents and reports | Composite view loads per tab; search on indexed normalised keys (`crm_client` keys, `acc_account_pn`, billing item loan application no.) | CONFIGURE |
| Peak | First quarter of the year; 10:00-12:00 and 14:00-16:00 | No batch work in these windows | FIT |
| Availability | 99.9%; used 06:00-22:00; maintenance weekdays and Saturdays 21:00-05:00; RTO 4 h, RPO 4 h | The maintenance window overlaps usage 21:00-22:00 (CSQ12); RPO 4 h is met by WAL archiving | CONFIGURE |
| Backup | Every 15 minutes (BRCSF-011.001); every 4 hours in the retention table | WAL archiving (15 minutes) plus base backups (CSQ11) | CONFIGURE |
| Retention | Client and account transactional information, contact updates, audit trail, document attachments: 5 years online, 15 years archive, backup every 4 hours, backup retention 5 years | nbadmin retention framework (record types CSF_CONTACT_CHANGE) | CHANGE |
| Anonymisation | No | None | FIT |
| Devices | Same performance on mobile and desktop | Responsive UI | FIT |
| Usability | User-friendly, intuitive, easy navigation | CSF workspace follows `BDO_UX_GUIDELINES.md` (record details pattern) | FIT |

## 9. Questions answered by this BRD

The BRD answers **none of the parked Operations items (OQ01, OQ02, OQ07, OQ45)**. It gives partial answers to the questions below.

| Q# | Source | Question (short) | Answer from the CSF BRD | Design impact |
|---|---|---|---|---|
| Q37 | NB spec | Authoritative system per data element | For client contact data, **QPS and EBIX are today's systems of record**; updates made in CSF must flow back to them (p.3, BRCSF-002) | During coexistence the contact change is queued for the legacy systems (`ContactSyncGateway`); after cut-over BIBS is the only source (CSQ01) |
| Q08 | NB spec | Which BDOI systems must receive data | QPS and EBIX must receive client contact updates (p.3 footnote 3) | Outbound port parked with an outbox |
| Q16 | NB spec | BDO bank-standard client data, CIF | Not answered; CSF limits agent updates to contact details (e-mail topic 3) | Contact-only update contract |
| Q23 | NB spec | Allowed file types | Text (doc, docx, txt), spreadsheets (xls, xlsx, ods, csv), images (PNG, JPG, HEIF) and PDF (BRCSF-007) | Seven types added to `AllowedFileType` |
| Q39 | NB spec | Retention per record type | Client / account data, contact updates, audit trail and attachments: 5 years online, 15 years archive, backup retention 5 years | Retention rule rows |
| Q42 | NB spec | Login: SSO or user ID / password | CSF asks for **user ID and password** (BRCSF-001) | Existing JWT login is sufficient for CSF; SSO remains a BDO-wide question |
| OQ44 / CQ25 / AQ27 | OPS / CLXN / ACCT | NFR alignment | Adds 99.9%, 06:00-22:00, RTO 4 h / RPO 4 h, maintenance 21:00-05:00, backup every 15 minutes | Not resolved; the infrastructure decision on 15-minute WAL archiving covers the strictest RPO (CSQ11) |
| CQ06 | CLXN spec | Data scope by segment | CSF agents must see **all** accounts, CBG and non-CBG (p.4) | No segment scoping for CSF roles |
| UX-3 | UX guidelines | Customer Service Facility menu entry waits for its BRD | This BRD | Section **Customer Service Facility** in the Client & Policy group (design section 10) |

## 10. Open questions for BDOI

| Q# | Topic | Question | Related |
|---|---|---|---|
| CSQ01 | Data sources | Until when do QPS and EBIX remain systems of record? Must contact updates made in BIBS be written back to them (direction, interface, frequency, owner), and does CSF need to show accounts that exist only in QPS / EBIX (not migrated)? | BRCSF-002, p.3 |
| CSQ02 | Search keys | "Client ID Number" = BIBS client code or a government ID number? "Account Number" = ARN, policy number or the legacy account number? PN and application numbers come from the loan system (LOS): is an LOS lookup needed for accounts not in BIBS (e-mail open item 2)? | BRCSF-003 |
| CSQ03 | Verification | Which verification checks are mandatory before an update, how many must match, and what evidence is kept (e-mail open item 5)? Is verification also required before resending an RA or e-policy? | BRCSF-004, e-mail topic 5 |
| CSQ04 | Statuses | Definitions of Pending, Awaiting, Booked, Open and Closed agreed by Marketing, Processing and Operations (e-mail open item 3), and their mapping to BIBS account and invoice states | BRCSF-005.001, e-mail item 7 |
| CSQ05 | Payment history | Content (receipts, applications, remittances to insurers?) and depth (11-12 months online, older on request?) | BRCSF-005, e-mail topic 6 |
| CSQ06 | RA resend | Are legacy RAs (IT text files) to be viewable in CSF? May an agent resend to an address other than the registered e-mail? Password convention of RA files | BRCSF-006, e-mail topic 8 |
| CSQ07 | Documents | Answers to the e-mail of 22-Jan-2026: which document types, linked to a client or an account, retention, minimum requirements, may agents download / upload / replace, confidentiality matrix | BRCSF-007, 009 |
| CSQ08 | File limits | Maximum file size; are HEIF photos from phones expected; virus scanning | BRCSF-007 |
| CSQ09 | Case management | Confirm that inquiry logging and case management (SharePoint today) are out of this phase, and whether a minimal interaction note per call is wanted now (e-mail topic 1 says CSF "will log client inquiries") | E-mail items 1, 10 |
| CSQ10 | Roles | Role matrix for agents, supervisors, contact center personnel, management and leads / heads; who may upload documents and who may update contacts (the BRD asks for differentiated roles, p.4) | BRCSF-001, p.4 |
| CSQ11 | Backup | Is the 15-minute backup a BIBS-wide infrastructure requirement (RPO 15 minutes) or specific to CSF data? The retention table says every 4 hours and the RPO is 4 hours | BRCSF-011.001 |
| CSQ12 | Service hours | Usage 06:00-22:00 overlaps the maintenance window 21:00-05:00 on weekdays and Saturdays; Sunday usage? | NFR |
| CSQ13 | E-policy resend | Only the confirmed e-policy of the account to the registered e-mail? Password as for the original dispatch? | E-mail item 9 |
| CSQ14 | Account contacts | Accounts have their own contact data in BIBS: should a CSF contact update also change the contacts of open accounts, or only the client master? | BRCSF-004 |
| CSQ15 | Additional contacts | Does "add contact details" mean more than one e-mail / mobile per client (additional contacts with a primary flag)? | BRCSF-004 |
| CSQ16 | Priorities | "Priority order for servicing (payments vs claims)" and "parameter alignment between the loan system and the insurance system" (e-mail open items): what is expected from CSF? | E-mail topic 11 |

**Answered in part by later BRDs** (single record with the design impact: [`BDOI_CROSS_BRD_DECISIONS.md`](BDOI_CROSS_BRD_DECISIONS.md) section 3):
- **CSQ01 partial** (BRD-13 Data Migration, p.5, BRID 2.1): after cutover QPS and EBIX are read-only and the new Core is
  the system of record for the migrated client master; no write-back is required by BRD-13. `LegacyAccountLookup` is
  implemented by `migration`. The conflict with footnote 3 of this BRD (updates sent to QPS / EBIX) stays open.
- **CSQ02 partial** (BRD-13 BRID 4.1, 11.1): legacy references are retained and searchable.
- **CSQ06 partial** (BRD-13 BRID 11.1): legacy RAs stay in read-only legacy or the archive (record type RENEWAL_ADVICE).
- **CSQ09 / CSF-EM10.** The Core Replacement umbrella BRD asks for case resolution (capability 16, BR-165), while this
  BRD defers case management: **CRQ04**, cross-BRD conflict **XQ12**. The umbrella also asks to resend e-policies
  (BR-164) where BRCSF-006 names RAs only (**CRQ16**).

## 11. Observations on the BRD pack

- The BRD predates BIBS. It describes a front end over QPS and EBIX that writes contact updates back to them. In BIBS the data sources are BrokerVerse modules, so most "integration" becomes reading BIBS data (CSQ01).
- The current system is called the "BDO-Insure Front-End System" in the BRD and "IBS" in the e-mail ("The CSF BRD aims to replace IBS").
- BRCSF-001 is used twice, for the System Admin requirement and for the agent log-in step; process steps (1.001, 3.001 ...) reuse the parent ID.
- The e-mail thread (p.17-18) is part of the signed pack only by attachment. It adds two statuses (item 7) and e-policy resending (item 9), and defers case management (item 10) and the open items (item 11). The other topics (verification, search keys, payment history depth, RA passwords) are clarifications without a BR ID.
- The printed e-mail includes a Microsoft Teams meeting link with meeting ID and passcode; the pack should not be circulated outside the project.
- Statuses "Pending" and "Awaiting" are not defined anywhere in the BRD.
- BRCSF-011.001 (backup every 15 minutes) is an infrastructure requirement placed under the audit trail report ID; it conflicts with the "every 4 hours" backup frequency of the retention table and the 4-hour RPO.
- The maintenance window (21:00-05:00) overlaps the usage hours (06:00-22:00) for one hour.
- The backup retention period is 5 years here, 7 years in the Operations and EB BRDs.
- The approval sheet shows the Head of Comptrollership annotated "signature not required"; the Unit Head Combank / Corbank signed by e-mail; some input providers' signature dates are blank.
- The revision log shows two v1.0 entries (04 and 09-Jun-2025) and no later revision, although the e-mail of 13-Feb-2026 changes the scope.
