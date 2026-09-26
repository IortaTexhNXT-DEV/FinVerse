# BDOI drop plan and programme timeline (received 26-Sep-2026)

Transcribed from the BDOI slides (`BDOI Drop Plan - modules and integrations.webp`, `BDOI Programme Timeline.webp`).

## Drops

| Drop | Module / functionality | BRD file | Integrations |
|---|---|---|---|
| **Drop 0 - Setup & Data Migration** | Setup: 1 Accessibility & Login; 2 Authorization; 3 User Maintenance; 4 Data Management (GL accounts, reference tables); 5 Workflow; 6 Product Maintenance | - | Entra-ID; IGA; LMS; HLS; PMS; CMS (alternate OBPCS), Automatic Fund Transfer System (AFTS), ICBS (struck through on the slide), Business Online Banking (BOB); ECM (content management); CCM (e-mail sending); M365; Trade Finance System (manual) |
| **Drop 1 - Transactional, upstream (product inherent)** | 1 Client Onboarding (Data Migration, NB); 2 Quotation or Proposal (NB, PM); 3 Account Creation and Maintenance (NB, CSF); 4 Submitted Policy (SP); 5 Renewal (RN); 6 Placement & ePolicy (NB, RN, EB); 7 Booking (NB, RN, SP); 8 Accounting / GL (ACCT); 9 Reports upstream (NB, RN, SP, EB) | as listed | (Drop 0 list) |
| **Drop 1 - Transactional, downstream (product agnostic)** | 1 Disbursement (ACCT); 2 Cashiering (Operations Cashiering addendum, Data Migration); 3 Remittance (Operations Remittance addendum, Reinsurance); 4 Adjustment / Cancellation (Operations, ACCT); 5 Accounting / GL (ACCT); 6 Reports BIR / regulatory (ACCT, Data Migration); 7 Collection of Commission Receivables - Direct Payment (Collections addendum, Operations) | as listed | (Drop 0 list) |
| **Drop 2 - Independent** | 1 Marketing Collection (extraction); 2 Claims; 3 Production Reconciliation; 4 Employee Benefits (no portal feature); 5 Other Reports | - | EDP - Data Ingestion (SD 11); Enterprise General Ledger (Ent GL); CARMS; Loan Origination and Admin System (LOAS); Insurer System; Bridger Insight (manual) |

## Timeline (months numbered from Jul 2026 = 4)

| Stream | Drop 1 | Migration | Drop 2 |
|---|---|---|---|
| Requirements / design | Sep - Nov 2026 | Requirements / mapping Sep - Nov 2026 | Dec 2026 - Feb 2027 |
| Build | Nov 2026 - Feb 2027 | Nov 2026 - Mar 2027 | Mar - Apr 2027 |
| SIT | Jan - Jul 2027 | SIT migration Apr - Jul 2027 | Jul - Sep 2027 |
| UAT | Aug - Dec 2027 (end to end) | UAT migration Aug - Oct 2027 | Oct - Nov 2027 |
| ORR / PRR | Full migration and cut-over Nov 2027 - Jan 2028; performance / penetration test Nov - Dec 2027; ORR / PRR Dec 2027 - Jan 2028 | | |
| **Go-live** | **January 2028** | | |

Requirement bars read against the month grid of the slide (corrected 26-Sep-2026; the first transcription gave Sep - Oct and Nov - Feb). Drop 0 has no bar of its own: its setup items are specified with Drop 1 and built in wave 1.

Notes on the slide: requirements and build run in three waves (1 setup and upstream, 2 downstream, 3 independent modules), about two months of build per wave.

Related: `Concept Paper - Advance Implementation of Renewal Processing V1.0 (signed).pdf` (early renewal release by 15-Aug-2027 for January-May 2028 expiries; no placement or booking before the January 2028 cut-over) and `BDOI_IER_Workbook_v20_iorta.xlsx` (infrastructure estimate: architecture, HW / SW, VDI, Kubernetes sizing per environment).

## Integration systems (BDOI, 26-Sep-2026; all external touch-points are owned by BDOI IT)

| System | Code |
|---|---|
| Enterprise Identity Access Management (Entra-ID) | EIAM |
| User ID Maintenance - Identity Security Cloud (IGA) | UIDM-ISC |
| Enterprise Content Management | ECM |
| Centralized Communications Management (e-mail sending) | CCM |
| M365 Exchange | M365 |
| Loans Management System | LMS |
| Home Loan System (Loan Origination and Admin) | HL - LOAS |
| Loan Front-End System | LFS SaaS |
| PDC Management System | PMS |
| Cash Management System (outward payments) | CMS / New BOB |
| Online Bills Payment Consolidation System (bills payment) | OBPCS |
| Old Business Online Banking Collection System (SOA bills payments via funds transfer) | Old BOB |
| Trade Finance System - client onboarding and payment (manual) | TFS |
| Enterprise Data Platform | EDP |
| Enterprise General Ledger | EGL |
| Bridger Insight XG | BridgerInsight |

## BDOI answers (26-Sep-2026)

- **Go-live:** every module goes live together in January 2028. There is no early go-live; the early renewal concept paper does not change the go-live.
- **Drops:** they split requirements, build, test and sign-off workload. The platform is built end to end and goes to UAT complete. BRDs, FRS, test plans and other collaterals are grouped under the drops.
- **Reinsurance:** Remittance handles the reinsurance transactions in Drop 1; the reinsurance module itself is phase 2.
- **Documents and attachments:** stored in an S3 bucket only.
- **Renewals and migration (answers to DMQ36-DMQ39):** renewals expiring January to May 2028 are processed in BIBS after go-live (go-live extraction of every expiry up to 31-May-2028, January expiries flagged urgent; renewal advices already sent by hand recorded, not re-sent); legacy packages are remapped at Renewal sanitation, not at upload; the RMEL and dispositions are kept in Excel and rejected rows are reviewed maker-checker by the Renewal processing team; year-end cut-over option A recommended (go-live Monday 3-Jan-2028, provisional GL opening trial balance, true-ups after the legacy close and the audited financial statements), awaiting Comptrollership confirmation.
