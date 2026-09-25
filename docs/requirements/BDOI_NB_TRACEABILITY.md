# BDOI New Business (BRD-1) - Requirements Traceability

One row per requirement of [`BDOI_NB_BRD_SPEC.md`](BDOI_NB_BRD_SPEC.md) (119 rows). Each claim
was checked against the code: the module, screen route, API path and test class named in the row
exist and cite the BR ID or implement the rule described in
[`BROKING_ARCHITECTURE.md`](../architecture/BROKING_ARCHITECTURE.md) sections 7 to 16.

Status:

- **Built**: implemented and tested.
- **Configured**: delivered by configuration of a built capability (tables, parameters, roles);
  the business content is set up by BDOI.
- **Parked (Qxx)**: a seam (port, table, manual upload) is built; the rest waits for the BDOI
  answer to the question.
- **Out of scope**: excluded by the BRD.

Where a row is Built but an external part is parked, the parked part is noted in the requirement
column and listed in the summary.

API paths are relative to `/api/v1`. Screens are frontend routes. Test classes are in
`backend/src/test/java/com/iortatechnxt/brokerverse` (`*IT`, `*Test`) or next to the frontend
feature (`*.test.ts`).

## A. Product lines and product rules

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.001 | Other Lines follow the Fire / Motor workflow; product master by risk code | Configured (matrix files Q01) | catalog, workflow | /catalog/products | catalog/products, catalog/lines | CatalogIT, CatalogAccountApiIT |
| BRNB.002 | Product-specific critical fields | Configured (Q02) | catalog, account | /catalog/products/:code, /accounts/new | catalog/field-rules | CatalogIT, AccountRulesIT |
| BRNB.003 | Validation rules by product type | Configured (Q02) | catalog, account | /catalog/products/:code | catalog/field-rules, accounts/{id}/check | AccountRulesIT, CatalogIT |
| BRNB.004 | Shared, versioned templates; intake template version on each quotation | Built (layouts Q03) | docgen, quotation, nonpackage | /broking-setup/templates | doc-templates | DocTemplateServiceIT, QuotationIT |
| BRNB.093 | Minimum-field matrix per product line | Configured (Q02) | catalog, account | /catalog/products | catalog/field-rules | CatalogIT, AccountRulesIT |
| BRNB.098 | Rule-based TSU involvement | Configured (thresholds Q04) | catalog, quotation, nonpackage | /catalog/products (TSU rules tab) | catalog/tsu-rules | CatalogIT, ProposalIT |

## B. Non-package placement (PRF / QS / TSU)

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.005 | PRF with risk details, attachments, approval, document checklist (multi-level chain Q05) | Built | nonpackage | /proposals/new, /proposals/:id | proposals, proposals/{id}/checklist | ProposalIT, QuotationProposalApiIT |
| BRNB.006 | Sequential marketing reference per PRF | Built | nonpackage | /proposals | proposals | ProposalIT |
| BRNB.007 | TSU receives, amends, returns or deletes PRFs | Built | nonpackage, workflow | /proposals/tsu | proposals/{id}, workflow/cases/{id}/actions/{action} | ProposalIT, WorkflowServiceIT |
| BRNB.008 | Quotation Slip to selected insurers with delivery log (insurer channels Q06) | Built | nonpackage, messaging | /proposals/:id (Quotation Slip tab) | proposals/{id}/quotation-slip/submit, approve | ProposalIT, MessagingIT |
| BRNB.009 | Insurer terms keyed in, editable, version history | Built | nonpackage | /proposals/:id (Insurer Responses tab) | proposals/{id}/responses, responses/history | ProposalIT |
| BRNB.010 | Comparative table, exportable, recommended insurer | Built | nonpackage | /proposals/:id (Comparative Table tab) | proposals/{id}/comparative(.pdf/.xlsx) | ComparativeTableTest, ProposalIT |
| BRNB.011 | Placement Update Reports, individual and collective | Built | nbreport | /nb/reports, /reports/NB-PLC-UPDATE | reports/NB-PLC-UPDATE/run, export | NbReportsIT |
| BRNB.012 | Dashboard: real-time tracking of requests, reports and comparative tables | Built | nbreport | /nb/dashboard | nb/dashboard | NbReportsIT, NbReportApiIT, nbreports.test.ts |
| BRNB.013 | Password protection of outbound documents, password in a separate e-mail (BDOI convention Q07) | Built | messaging, quotation, nonpackage, placement, issuance | SendEmailDialog on the record pages | messages | MessagingIT, QuotationIT |
| BRNB.014 | Validation and approval before sending (multi-level Q05) | Built | quotation, nonpackage, workflow | /quotations/:id, /proposals/:id | quotations/{id}/approve, proposals/{id}/approve | QuotationIT, ProposalIT |
| BRNB.015 | Notifications on status changes (feed to BDOI systems Q08) | Built | workflow, messaging | header bell, /my-work | notifications | WorkflowServiceIT, MessagingIT |
| BRNB.016 | Audit trail of all actions and changes | Built | audit (every module) | /admin/audit, History tabs | audit-logs | AuditTrailImmutabilityIT |
| BRNB.017 | Proposal Slip after approvals, archived and retrievable | Built | nonpackage | /proposals/:id (Proposal Slip tab) | proposals/{id}/proposal-slip/submit, approve, .pdf | ProposalIT |
| BRNB.018 | Late renewal requests report | Parked (Q09) | - | - | - | - |

## C. Quotation / proposal

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.020 | Edit before approval, version history with diff | Built | quotation | /quotations/:id (Versions tab) | quotations/{id}/versions, diff, revise | QuotationIT, QuotationDiffTest |
| BRNB.021 | Approve / reject before sending or account creation | Built | quotation | /quotations/:id | quotations/{id}/approve, workflow actions | QuotationIT |
| BRNB.041 | Requests by e-mail or source system (shared mailbox reading Q12) | Built | quotation | /quotations/requests | quotation-requests | QuotationIntakeIT, QuotationProposalApiIT |
| BRNB.024 | Accepted quotations updated by upload or manually | Built | quotation, bulk | /bulk/QUOTATION_ACCEPTANCE | bulk | QuotationIntakeIT, BulkServiceIT |
| BRNB.029 | Client code required; incomplete information flagged and gated | Built | crm, quotation, account | /crm/clients/:id, /quotations/new | crm/clients, quotations | ClientServiceIT, QuotationIT, AccountIT |
| BRNB.043 | Individual quotation with premium, PDF / XLSX, e-mail, acceptance | Built | quotation | /quotations/new, /quotations/:id | quotations, document.pdf/.xlsx, send, accept | QuotationIT, QuotationProposalApiIT |
| BRNB.045 | Client acceptance creates the accounts | Built | quotation, account | /quotations/:id | quotations/{id}/accept, create-accounts | QuotationIT |
| BRNB.102 | One ARN from quotation to invoice | Built | account, quotation, nonpackage, placement, issuance, booking | ARN chip on every record | accounts/by-arn/{arn}, quotations/by-arn/{arn} | QuotationIT, BookingServiceIT |

## D. Client (CRM, onboarding, KYC)

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.030 | Create / update client to bank standards, dedupe, KYC documents, client code (BDO KYC fields Q16) | Built | crm | /crm/clients/new, /crm/clients/:id | crm/clients, kyc-documents | ClientServiceIT, ClientOnboardingIT |
| BRNB.032 | Prevent duplicate clients and accounts (keys Q17) | Built | crm, account | /crm/clients/new, /accounts/new | crm/clients/duplicates | ClientServiceIT, AccountRulesIT |
| BRNB.046 | Search clients by several criteria | Built | crm | /crm/clients | crm/clients | ClientServiceIT, CrmAdminApiIT |
| BRNB.047 | Bulk client update | Built | crm, bulk | /bulk/CLIENT_CREATE | bulk | ClientBulkIT |
| BRNB.048 | Create individual client | Built | crm | /crm/clients/new | crm/clients | ClientServiceIT |
| BRNB.049 | Update individual client with documents | Built | crm | /crm/clients/:id/edit | crm/clients/{id} | ClientServiceIT |
| BRNB.065 | Auto-create / update clients during bulk | Built | crm, account, quotation | /bulk/CLIENT_CREATE, /bulk/ACCOUNT_CREATE | bulk | ClientBulkIT, AccountBulkIT |
| BRNB.090 | Onboarding: prospect, KYC verified, confirmed | Built | crm, workflow | /crm/clients/:id | crm/clients/{id}/submit-kyc, verify-kyc, confirm | ClientOnboardingIT |
| BRNB.091 | Client and account tags and special instructions (enforcement Q18) | Built | crm | /crm/clients/:id (Tags & Instructions) | crm/clients/{id}/notes, tags, instructions | ClientNotesIT |
| BRNB.099 | Client 360 with linkage warnings (leads Q19) | Built | crm (+ providers in account, quotation, nonpackage, issuance, booking) | /crm/clients/:id (Linked Records) | crm/clients/{id}/records | ClientServiceIT |
| BRNB.101 | Prospect vs confirmed identifiers, traceable conversion | Built | crm | /crm/clients | crm/clients/{id}/confirm | ClientOnboardingIT |
| BRNB.110 | Monthly list of non-bank clients due for KYC review | Built | crm | /crm/kyc-reviews | crm/kyc-reviews, reports/NB-KYC-DUE | KycReviewIT |

## E. Account (risk record)

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.025 | Update accounts in bulk and individually | Built | account, bulk | /accounts/:id/edit, /bulk/ACCOUNT_UPDATE | accounts/{id}, bulk | AccountIT, AccountBulkIT |
| BRNB.050 | Search accounts by several criteria | Built | account | /accounts | accounts | AccountIT, CatalogAccountApiIT |
| BRNB.051 | Create account: multi-value items, calculations, duplicate fall-out, draft | Built | account, catalog | /accounts/new | accounts, catalog/rating/quote | AccountIT, AccountRulesIT, CatalogRatingIT |
| BRNB.052 | Bulk account update | Built | account, bulk | /bulk/ACCOUNT_UPDATE | bulk | AccountBulkIT |
| BRNB.053 | Update account (Marketing) with documents | Built | account, attachment | /accounts/:id/edit | accounts/{id}, attachments | AccountIT |
| BRNB.054 | Update account (Processing) | Built | account | /accounts/:id | accounts/{id}, validate | AccountIT |
| BRNB.066 | Auto-create accounts in bulk with dedupe and naming | Built | account, bulk | /bulk/ACCOUNT_CREATE | bulk | AccountBulkIT |
| BRNB.109 | Account-level contact defaulted from the client | Built | account | /accounts/new | accounts | AccountIT |

## F. Bulk intake and source systems

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.023 | Home insurance requests from HLS | Parked (Q11) | quotation (QuotationRequestSource port, intake job) | /quotations/requests | quotation-requests | QuotationIntakeIT |
| BRNB.028 | Bulk quotations from source-system requests (interface Q11) | Built | quotation, bulk | /bulk/QUOTATION_CREATE | bulk | QuotationIntakeIT |
| BRNB.063 | Bulk quotation without a client code (creates a prospect, Q13) | Built | quotation, crm | /bulk/QUOTATION_CREATE | bulk | QuotationIntakeIT |
| BRNB.064 | Accounts for bulk from source or upload, sanitised (criteria Q14) | Built | bulk, account | /bulk | bulk | BulkServiceIT, AccountBulkIT |
| BRNB.039 | Manually uploaded accounts with the standard template | Built | bulk, account | /bulk/ACCOUNT_CREATE | bulk | BulkServiceIT |
| BRNB.042 | Bulk quotations, reference numbers, single or batch send | Built | quotation | /quotations (Send via Email) | quotations/batch-send | QuotationIT |
| BRNB.044 | Build accounts for bulk processing from accepted quotations | Built | quotation, bulk | /bulk/QUOTATION_ACCEPTANCE | bulk | QuotationIntakeIT |

## G. Documents

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.026 | Multi-file upload, naming syntax, link to accounts (syntax Q23) | Built | attachment | Documents tabs | attachments/batch, attachments/{id}/links | AttachmentIT, DocumentServiceIT |
| BRNB.055 | Upload files / documents | Built | attachment | Documents tabs | attachments | AttachmentIT |
| BRNB.056 | View and download client and account documents, ZIP | Built | attachment | Documents tabs | attachments/zip | AttachmentIT |
| BRNB.104 | Extract details from uploaded documents after confirmation (OCR Q24) | Built | issuance | /issuance/epolicies/:id | issuance/epolicies/{id}/extract, confirm | EpolicyIT, PolicyTextParserTest |
| BRNB.105 | Uploaded documents trigger business processes | Built | issuance | /issuance/upload | issuance/triggers | EpolicyIT |

## H. Placement with insurers

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.033 | Marketing handles accounts returned by Processing | Built | workflow, account | /my-work, /accounts/:id | workflow actions, accounts/{id}/resubmit | AccountLifecycleIT, WorkflowServiceIT |
| BRNB.034 | Processing handles placements returned by the insurer | Built | placement | /placement, /placement/accounts/:arn | placement/accounts/{arn}/insurer-return, resubmit | PlacementSlipIT, PlacementIssuanceActionsApiIT |
| BRNB.058 | Returned placement (as BRNB.033) | Built | placement, workflow | /placement | placement/accounts/{arn}/resubmit | PlacementSlipIT |
| BRNB.059 | Insurer return (as BRNB.034) | Built | placement | /placement | placement/accounts/{arn}/insurer-return | PlacementSlipIT |
| BRNB.062 | Cancel placement, single or multiple | Built | placement, account | /placement (Cancel Placement) | placement/accounts/cancel | PlacementIssuanceActionsApiIT |
| BRD 2.1.16 | Reactivate cancelled placements | Built | placement, account | /placement (Reactivate) | placement/accounts/reactivate | PlacementIssuanceActionsApiIT |
| BRNB.069 | Placement slip when prerequisites are met; regenerate after returns | Built | placement | /placement, /placement/slips | placement/slips/generate, regenerate | PlacementSlipIT, PlacementFilesTest |
| BRNB.071 | Send the slip to the insurer, resend (SFTP / API Q06) | Built | placement, messaging | /placement/slips | placement/slips/{id}/send, slips/send | PlacementSlipIT |
| BRNB.072 | 30-day hold-cover request (rules Q27) | Built | placement | /placement/accounts/:arn | placement/accounts/{arn}/hold-cover | HoldCoverIT |
| BRNB.103 | Record the insurer's hold-cover confirmation | Built | placement | /placement/accounts/:arn | placement/hold-cover/confirm, decline | HoldCoverIT |

## I. Billing and payment confirmation

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.067 | CLPC billing file and payment report matching (SFTP Q28) | Built | placement | /placement/billing | placement/billing/batches, reports | BillingIT, NbReportsIT (NB-CLPC-BILLING) |
| BRNB.068 | Payment validation by reference for other segments | Built | placement | /placement/billing/reports/:id | placement/billing/reports, gate | PaymentGateIT, BillingIT |
| BRNB.114 | Direct-payment accounts tagged (accounting Q29) | Built | account, placement, booking | /accounts/direct-payment | accounts/{id}/payment-arrangement, placement/gate/{arn}/direct-payment | AccountIT, PaymentGateIT |

## J. E-policy and Insurance Advice

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.035 | Encrypted IA and e-policies, password separately (convention Q07) | Built | messaging, issuance | /issuance/dispatch, /issuance/insurance-advice | issuance/dispatch, insurance-advice/send | AdviceAndDispatchIT |
| BRNB.060 | Insurance Advice register: search, view, download, send | Built | issuance | /issuance/insurance-advice | issuance/insurance-advice | AdviceAndDispatchIT |
| BRNB.070 | Insurance Advice for mortgaged accounts (recipient Q30) | Built | issuance | /issuance/insurance-advice | issuance/insurance-advice/generate | AdviceAndDispatchIT |
| BRNB.095 | IA generation trigger rules | Configured (Q30) | issuance | /admin/parameters (IA_TRIGGER) | issuance/insurance-advice/generate | AdviceAndDispatchIT |
| BRNB.073 | Receive e-policies and store them (mailbox / SFTP Q31) | Built | issuance | /issuance/upload | issuance/epolicies, epolicy-uploads | EpolicyIT |
| BRNB.074 | Update the policy number and link the e-policy | Built | issuance, account | /issuance/epolicies/:id | issuance/epolicies/{id}/confirm | EpolicyIT |
| BRNB.077 | E-mail e-policies to clients, encrypted, single or batch | Built | issuance, messaging | /issuance/dispatch | issuance/dispatch/{id}, batch | AdviceAndDispatchIT |
| BRNB.078 | Report of sent and failed e-policies by date range | Built | nbreport, issuance | /reports/NB-DISPATCH, /issuance/dispatch | reports/NB-DISPATCH/run, issuance/dispatch/log | NbReportsIT, AdviceAndDispatchIT |

## K. Booking, endorsements and cancellation

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.027 | Book accounts with GL entry and service invoice, atomic (GL accounts OQ07) | Built | booking, accounting | /booking, /booking/book/:arn | booking/preview, book | BookingServiceIT, BookingApiIT |
| BRNB.036 | Individual and batch booking, scheduled batch, edit before booking | Built | booking | /booking, /booking/batch-runs | booking/queue, batch/confirm, book-now | BookingBatchIT |
| BRNB.038 | Direct booking without placement | Built | booking, account | /accounts/:id (Direct Booking) | accounts/{id}/direct-booking | BookingServiceIT, AccountLifecycleIT |
| BRNB.061 | Book manually or in bulk; positive endorsements | Built | booking | /booking, /booking/endorsements/new | booking/book, endorsements | BookingServiceIT, EndorsementIT |
| BRNB.076 | Auto-book on criteria, no duplicate booking | Built | booking | /booking/setup | booking/setup/auto-book-rules | BookingBatchIT, BookingServiceIT |
| BRNB.081 | Cancel booking; negative endorsements | Built | booking | /booking/invoices/:id | booking/endorsements | EndorsementIT |
| BRNB.094 | Policy cancellation vs internal reversal | Built | account, booking | /accounts/:id, /booking/invoices/:id | workflow void, booking/endorsements | AccountLifecycleIT, EndorsementIT |
| BRNB.100 | Service invoice from template and triggers, with owner (BIR CAS seam) | Built | booking | /booking/service-invoices | booking/service-invoices | ServiceInvoiceIT |
| BRNB.100b | Send the service invoice, notify success or failure | Built | booking, messaging | /booking/service-invoices/:id | booking/service-invoices/{id}/resend | ServiceInvoiceIT, NbReportsIT (NB-SI-REG) |
| BRNB.107 | Incentive eligibility indicator (rules Q33) | Configured (Q33) | booking | /booking/setup | booking/setup/incentive-rules | BookingServiceIT |
| BRNB.108 | Cost center on every booked transaction (source Q34) | Built | booking, catalog | /booking/book/:arn | booking/book | BookingServiceIT |
| BRNB.111 | Direct booking with the issued policy mandatory | Built | account, booking | /accounts/:id | accounts/{id}/direct-booking | AccountLifecycleIT |
| BRNB.112 | Multi-year booking, one policy number per year (Q35) | Built | booking, issuance | /booking/invoices/:id | booking/book | BookingBatchIT |
| BRNB.113 | Free First-Year accounts list (Q36) | Built | account | /accounts/ffy | accounts/{id}/ffy, bulk FFY_TAGGING | AccountIT, AccountBulkIT |

## L. Workflow, status and governance

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.019 | Delete (void) in-process records with a reason | Built | workflow, account, quotation, nonpackage | WorkflowPanel (Void) | workflow/cases/{id}/actions/void | AccountLifecycleIT, WorkflowServiceIT |
| BRNB.022 | Status tracking with defined stages and transitions (master list Q10) | Built | workflow | WorkflowPanel, StageTimeline | workflow/cases | WorkflowServiceIT, BrokingFoundationApiIT |
| BRNB.092 | Source of truth per data element | Parked (Q37) | - | - | - | - |
| BRNB.096 | Marketing submissions trigger Processing (GRF / ARF Q38) | Built | workflow | /my-work | workflow/queue | WorkflowServiceIT |
| BRNB.097 | New Business vs Renewal classified and routed | Built | workflow, booking | /booking | booking/invoices | BookingServiceIT |
| BRNB.106 | Retention policy (archive / purge Q39) | Built | nbadmin (+ providers) | /broking-setup/retention | nbadmin/retention | RetentionIT |
| BRNB.115 | Track status from quotation to booking; stalled accounts; status reports | Built | workflow, nbreport | /my-work, /nb/dashboard, /reports/NB-ACC-STATUS | workflow/cases, reports/NB-ACC-STATUS/run | WorkflowServiceIT, NbReportsIT |
| BRNB.080 | Assign and re-assign workload | Built | workflow | /my-work | workflow/cases/{id}/assign, claim | WorkflowServiceIT |
| BRNB.079 | Approve requests (LOV, profile ...) | Built | approval, lov, nbadmin | /approvals | approvals/inbox | LovServiceIT, AccessRequestIT |
| OOS-1 | Override requests (Renewals only) | Out of scope | - | - | - | - |

## M. Reports and dashboards

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.031 | Print reports with preview and metadata (report, user, time, filters) | Built | report | /reports/:code (Print) | reports/{code}/export?format=PDF | OdsXmlReportRendererTest, ReportRenderingIT |
| BRNB.037 | Download reports as xlsx, xml, ods, csv, logged | Built | report | /reports/:code | reports/{code}/export?format=XLSX/XML/ODS/CSV | OdsXmlReportRendererTest, ReportRenderingIT, NbReportApiIT |
| BRNB.057 | Role-based reports and dynamic reports (builder Q40) | Built (saved variants) | report, nbreport | /nb/reports, /reports/:code | nb/report-variants | NbReportsIT, NbReportApiIT |
| BRNB.075 | Successful / fall-out per stage, status per account, placement summary, CLPC billing, matched payments, production vs target (targets Q41) | Built | nbreport | /nb/reports, /nb/targets, /nb/dashboard | reports/NB-*/run, nb/targets | NbReportsIT, NbReportApiIT |
| OOS-2 | QPS-specific references and reports | Out of scope | - | - | - | - |

## N. Access, security and administration

| ID | Requirement | Status | Module(s) | Screen(s) | API | Tests |
|---|---|---|---|---|---|---|
| BRNB.040 | Role login, inactivity and expiry warnings (BDO SSO Q42) | Configured | security, system | session warning dialog | system/session-policy | SystemAdminIT, CrmAdminApiIT |
| BRNB.082 | Application open in several tabs | Built | frontend session | every screen | - | tabSync.test.ts |
| BRNB.083 | Maintain LOVs with effectivity and approval | Built | lov | /broking-setup/lists | lov | LovServiceIT |
| BRNB.084 | Assign role profiles per User Access Matrix | Configured | security | /admin/users, /broking-setup/access-matrix | admin/users, nbadmin/access-matrix | UserAdminApiIT, AccessRequestIT |
| BRNB.085 | Profile creation / modification requests for approval | Built | nbadmin | /broking-setup/access-requests | nbadmin/access-requests | AccessRequestIT |
| BRNB.086 | View and download the audit log report | Built | audit, report | /admin/audit | audit-logs, reports/CTL-AUDIT/export | AuditTrailImmutabilityIT, ReportRenderingIT |
| BRNB.087 | System Administrator access | Configured | security | /admin/* | admin | UserAdminApiIT |
| BRNB.088 | Profiles and their functions | Configured | security | /admin/roles, /broking-setup/access-matrix | admin/roles | UserAdminApiIT |
| BRNB.089 | Generate and export audit log reports | Built | audit, report | /admin/audit | reports/CTL-AUDIT/export | ReportRenderingIT |

## Summary

| Status | Rows |
|---|---|
| Built | 103 |
| Configured | 11 |
| Parked | 3 |
| Out of scope | 2 |
| **Total** | **119** |

Built rows may still have a parked external part (listed below); Configured rows need BDOI's
content (product matrix, thresholds, roles) in tables that are built and seeded with defaults.

### Parked items and their questions

Fully parked requirements:

| ID | Question | Seam |
|---|---|---|
| BRNB.018 | Q09 | none yet: waits for the Renewal BRD decision |
| BRNB.023 | Q11 | `QuotationRequestSource` port, `quo_request` staging, `QUOTATION_REQUEST_INTAKE` job, `QUOTATION_REQUEST` upload |
| BRNB.092 | Q37 | data ownership register not started |

Parked parts of built or configured requirements:

| Question | Parked part | Requirements |
|---|---|---|
| Q01, Q02 | Product matrix files and minimum fields per line (tables seeded with defaults) | BRNB.001-003, 093 |
| Q03 | BDOI document layouts (templates are versioned and editable) | BRNB.004 |
| Q04 | TSU routing thresholds | BRNB.098 |
| Q05 | Multi-level approval chains | BRNB.005, 014 |
| Q06 | Insurer SFTP / API channels (e-mail only) | BRNB.008, 071 |
| Q07 | BDOI password convention | BRNB.013, 035 |
| Q08 | Feed of NB data to other BDOI systems | BRNB.015 |
| Q12, Q31 | Reading shared mailboxes (manual capture and upload) | BRNB.041, 073 |
| Q14 | Bulk sanitisation criteria | BRNB.064 |
| Q16 | BDO KYC standard fields and CIF integration | BRNB.030 |
| Q17 | Duplicate keys and precedence | BRNB.032 |
| Q18 | Enforcement of tags and instructions (warn only) | BRNB.091 |
| Q19 | Lead management | BRNB.099 |
| Q23 | Document naming syntax | BRNB.026 |
| Q24 | OCR (text PDFs only) | BRNB.104 |
| Q27 | Hold-cover rules at expiry | BRNB.072 |
| Q28 | CLPC SFTP transport | BRNB.067 |
| Q29 | Direct-payment accounting variant | BRNB.114 |
| Q30 | Insurance Advice trigger and recipient | BRNB.070, 095 |
| Q33 | Incentive rules content | BRNB.107 |
| Q34 | Cost center master source | BRNB.108 |
| Q35 | Multi-year premium and commission basis | BRNB.112 |
| Q36 | FFY payer and renewal hand-off | BRNB.113 |
| Q38 | GRF / ARF form behaviour | BRNB.096 |
| Q39 | Archive and purge of retained records | BRNB.106 |
| Q40 | Dynamic report builder (saved variants delivered) | BRNB.057 |
| Q41 | Sales hierarchy and target values (demo targets) | BRNB.075 |
| Q42 | BDO SSO / Active Directory | BRNB.040 |
| OQ07 | Real GL accounts of the booking event | BRNB.027 |

### Answered by later BRDs

The rows above are unchanged: they describe what is built today. Later BRDs (BRD-6 to BRD-12) answer some of the
parked items; nothing below is built yet. The answers are in
[`BDOI_NB_BRD_SPEC.md`](BDOI_NB_BRD_SPEC.md) section 9.1 and, with the design impact, in
[`BDOI_CROSS_BRD_DECISIONS.md`](BDOI_CROSS_BRD_DECISIONS.md).

| ID | Question | Status | Answering BRD and ID | Effect on this row (planned, not built) |
|---|---|---|---|---|
| BRNB.040 | Q42 | answered | BRD-11 UAM (UAM-NFR-11, 17, 33) | Directory sign-in required; `DirectoryAuthenticator` port with LOCAL default (USER_ACCESS_DESIGN); the EUA adapter stays parked (UQ04) |
| OOS-1 | - | answered | BRD-6 RN (1.011.1, BRRN.023/031/035) | Stays out of BRD-1; delivered by the Renewal module (`RNW_OVERRIDE`, `rnw_override`) |
| BRNB.097 | - | answered | BRD-6 RN (BRRN.033), BRD-8 EB (BRID-022.01), BRD-12 SP (BRIDSP-27) | Work item BT0: the booked invoice takes the account's business type instead of the constant NEW_BUSINESS (V822) |
| BRNB.018 | Q09 | open | BRD-6 RN (RQ29) | Still parked; candidate variant of `RNW-LISTING` |
| BRNB.023 | Q11 | partial | BRD-12 SP (p.4-5) | Source list named; layouts and transport still open |
| BRNB.092 | Q37 | partial | BRD-7 CLM (BRCLM.007), BRD-9 CSF (p.3) | Policy number owned by the account / issuance; client contacts owned by QPS / EBIX during coexistence |
| BRNB.064 | Q14 | partial | BRD-6 RN (BRRN.020), BRD-12 SP (Report List #151) | Criteria for renewal and submitted-policy lists; the NB bulk sanitiser is unchanged |
| BRNB.091 | Q18 | partial | BRD-10 SANC (SNSRP-302/303) | Screening sets `PEP` / `WATCHLIST_REVIEW`; warn only stays |
| BRNB.110 | Q21 | partial | BRD-6 RN (BRRN.028), BRD-10 SANC (SNSRP-303) | KYC due is information only in Renewal; high-risk / PEP clients get a screening case |
| BRNB.026 | Q23 | partial | BRD-8 EB, BRD-9 CSF (BRCSF-007), BRD-10 SANC (SNSRP-601) | More file types (CSF); a second, named naming pattern for screening documents |
| BRNB.104 | Q24 | partial | BRD-12 SP (BRIDSP-02) | Second extraction document kind; OCR still a parked port |
| BRNB.072 | Q27 | partial | BRD-6 RN, BRD-12 SP (BRIDSP-24/32) | Hold-cover re-assignment and unbooked alert (Submitted Policies); expiry handling still open |
| BRNB.067 | Q28 | partial | BRD-12 SP (Report List #65-67) | Three CLPC billing variants; transport still open |
| BRNB.113 | Q36 | partial | BRD-12 SP (BRIDSP-13) | FFY-specific RA template at renewal; payer still open |
| BRNB.106 | Q39 | partial | BRD-7, BRD-8, BRD-9, BRD-10 | Retention values for claims, EB, CSF and screening records |
| BRNB.057 | Q40 | partial | BRD-8 EB (BRID-022 AC9), BRD-12 SP (Report List #74) | Saved variants and column filters confirmed as enough |
| BRNB.008, 071 | Q06 | partial | BRD-8 EB (BRID-005) | Insurer portal for EB |
| BRNB.013, 035 | Q07 | partial | BRD-8 EB (BRID-007) | Both password modes confirmed; convention still missing |
| BRNB.015 | Q08 | partial | BRD-9 CSF (p.3) | Contact updates to QPS / EBIX through an outbox port |
| BRNB.073 | Q31 | partial | BRD-8 EB (BRID-005.01) | EB policy forms uploaded by insurers through the portal |
| BRNB.085 | - | extended | BRD-11 UAM (sections A-B) | Request lifecycle with drafts, return, cancel, chosen approver and bulk (`nbadmin`) |
