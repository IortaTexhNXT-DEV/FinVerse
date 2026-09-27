# Renewal (BRD-6)

Guide of the Renewal module of iNXT BrokerVerse (`renewal`): the expiring accounts of BDOI from extraction to the
renewed invoice. The build design is [`docs/architecture/RENEWAL_DESIGN.md`](../architecture/RENEWAL_DESIGN.md);
section 17 records what was built. The requirements are in
[`docs/requirements/BDOI_RN_BRD_SPEC.md`](../requirements/BDOI_RN_BRD_SPEC.md) and the functional specification in
[`FRS_BRD06_RENEWAL.md`](../deliverables/src/frs/FRS_BRD06_RENEWAL.md) (FR-RN-001 to 112).

Status: built for waves R0 to R2 (extraction and go-live take-over, sanitation with package remapping, Classification
and decision matrix, Marketing disposition and Team Leader review, processing with the renewal account, insurer batches
and responses, LAMD reports, Renewal Advice and closing letters, acceptance, fast track to placement and booking,
Contact Center follow-ups, the Renewal Home, the eleven reports and the seed data). The hand-off of submitted policies
(wave R3) waits for the Submitted Policies module.

## 1. Purpose

- A **renewal** (`RNW-<yyyy>-nnnnnn`) is one expiring account (booked in BIBS, or a migrated policy) for one policy
  year. It carries a snapshot of the policy, the parties and the amounts at extraction, the flags (urgent, returned,
  transferred, endorsed, claims, outstanding, KYC due, NRNS, straight-through, locked, NFR sent) and its `RNW_CASE` work
  case.
- **Extraction** takes the accounts expiring at the business date plus `RNW_EXTRACTION_LEAD_DAYS` every night, or a range
  on demand (Generate Expiry List). An account is extracted once per term.
- **Initiation** runs the **checks** (sanitation, matching, eligibility, the package of migrated policies) and gives the
  **Classification**: Clean, Review or Exception. A failed check never gives Clean. The **decision matrix** proposes the
  disposition (automatic for a straight-through renewal, a proposal to the officer otherwise).
- **Marketing** assigns the renewal to an Account Officer, who opens the Account History and gives the **disposition**:
  For Renewal, For Quotation, For Proposal (New Business path), Not for Renewal (with a reason) or Lost Business, then
  pushes it to the Team Leader, who returns or **posts** it.
- **Processing** creates the **renewal account** (business type Renewal, linked to the expiring account), sends the
  renewals to each insurer in a **batch** and applies the **insurer response**.
- **Letters**: the **Renewal Advice** (first and second notice) is generated and sent protected to the client; renewals
  not renewed get the Not Acceptable (NAL) or Not for Renewal (NFR) letter; renewals with no response are flagged NRNS
  and reminded; past expiry they close with the non-acceptance letter.
- **Acceptance** (e-mail, signed RA or payment) fast-tracks the renewal account to placement; the issued policy is
  queued for booking, and the booked renewal invoice closes the renewal as **Renewed**.

Renewal posts **no accounting entry**: the renewal account is booked by Booking like any account.

## 2. Personas and roles

| Persona | Role | SIT/UAT user(s) | What they do |
|---|---|---|---|
| Marketing Team Leader | `MKT_TL` | `rnwtl` (T-CORP1), `mkttl` | Generate Expiry List, Initiate, Assign Disposition, transfers, TL Review (return, post, override), letters, acceptance |
| Marketing Account Officer | `MKT_AO` | `ao` | My Dispositions: account history, disposition, push, transfer request, remarks, letters, acceptance |
| Renewal processing lead | `PROCESSING_TL` | `proctl` | Assign Processing Officers, renewal accounts, insurer batches and responses, uploads, letters, package choices |
| Processing Officer | `PROCESSOR` | `proc` | Processing work list, renewal account, insurer responses, letters, acceptance, package choices |
| Business Administrator | `BUSINESS_ADMIN` | `badmin` | Renewal Setup: risk codes, checks, Classification rules, decision matrix, go-live take-over, letter templates |
| Loan and Mortgage department | `LAMD` | `lamd` | LAMD Reports upload |
| Contact Center | `CONTACT_CENTER` | `contactc` | Follow-ups |
| Audit | `AUDITOR` | `auditor` | Read-only lists, records and reports |

The permissions are the `RNW_*` rights of V1010 (design section 6). A user sees the renewals of the units in their
scope; the Renewal processing team sees all units.

## 3. Screens (Client & Policy, Renewal)

| Screen | Route | Permission | Main actions |
|---|---|---|---|
| Renewal Home | `/renewal` | `RNW_VIEW` | Due in 30 / 60 / 90 / 140 days, at risk, urgent, returned, NRNS, insurer overdue, letters failed; by status, by Classification, workload |
| Expiry List | `/renewal/expiry` | `RNW_VIEW` | Tabs by stage and disposition; Generate Expiry List, Initiate, Assign Disposition, Push, Transfer, Download; recent extractions |
| My Dispositions | `/renewal/mine` | `RNW_DISPOSE` | Quick filters To disposition, Returned to me, Due in 30 days, NRNS; Push, Transfer; Upload Dispositions with Declare Complete File |
| TL Review | `/renewal/review` | `RNW_REVIEW` | Post, Return, Override, Re-assign |
| Transfers | `/renewal/transfers` | `RNW_ASSIGN` | Accept, Decline, Cancel request |
| Processing Worklist | `/renewal/processing` | `RNW_PROCESS` | Assign PO, Assign to Me, Return to Marketing |
| Insurer Batches | `/renewal/insurer` | `RNW_INSURER` | New Batch, Download, Send to Insurer, Upload Insurer Responses |
| Letters | `/renewal/letters` | `RNW_RA_GENERATE` | Generate RA, Send, Send Letters (NAL / NFR), Upload Acceptances |
| Follow-ups | `/renewal/followups` | `RNW_FOLLOWUP` | Renewals waiting for the client; follow-ups are recorded on the record |
| LAMD Reports | `/renewal/lamd` | `RNW_LAMD_UPLOAD` | Upload LAMD Report (type and month), match results |
| Renewal Setup | `/renewal/setup` | `RNW_SETUP` or `RNW_PACKAGE_REMAP` | Risk codes, checks, Classification rules, decision matrix, package map, package choices, go-live |
| Renewal record | `/renewal/candidates/:ref` | `RNW_VIEW` | Set Disposition, Push, Start Quotation / Proposal, Create Renewal Account, Record Insurer Response, Record Acceptance, Add Follow-up, Add Remark, Propose Package, Re-open, Details PDF; tabs Details, Checks, Account History, Computations, Insurer, Letters, Documents, Remarks & Follow-ups, History |

The client record shows a **Renewal** tab with the client's renewals. The reports are in the Report Centre under
**Renewal**.

## 4. Life of a renewal

1. **Extracted** by the nightly job or Generate Expiry List (stage Extracted).
2. **Initiated** (Initiate on the Expiry List; renewals of the segments in `RNW_BULK_INITIATION_SEGMENTS` and those
   taken over at go-live may be initiated several at a time, the others one at a time): the checks run, the
   Classification and the matrix proposal are set (stage Unassigned Disposition or, for an automatic disposition,
   straight to For Processing when `RNW_STP_SKIP_TL_REVIEW` is on).
3. **Assigned** to an Account Officer (For Disposition). The officer opens the Account History (recorded), sets the
   disposition and pushes it (Review in Progress). A transfer to another unit waits for the receiving Team Leader
   (Transfer Pending).
4. The Team Leader **returns** it with a reason or **posts** it: For Renewal goes to For Processing; For Quotation and
   For Proposal go to the New Business path, where the officer starts a quotation or proposal linked to the renewal;
   Not for Renewal and Lost Business go to Letter Pending.
5. **Processing**: the renewal account is created on reaching For Processing; a Processing Officer is assigned (In
   Processing); the renewal goes to an insurer batch (With Insurer). The insurer response Renew As Is or Revise moves it
   to RA Ready; a decline goes back to Processing or to the NAL letter.
6. **Renewal Advice** generated (RA Generated) and sent (Awaiting Response). A late RA needs the user's confirmation;
   the second notice is offered by the NRNS job.
7. **Acceptance** (Accepted): the renewal account is fast-tracked, the placement slips are generated and the Processing
   Officer is notified (For Placement and Booking). The issued policy is queued for booking; the booked invoice closes
   the renewal as **Renewed**.
8. **Closing letters**: Send Letters sends the NAL or NFR and closes the renewal (Closed). A renewal closed as not renewed
   can be re-opened until its expiry plus `RNW_REOPEN_DAYS`.

## 5. Jobs

| Job | Schedule (PHT) | What it does |
|---|---|---|
| `RNW_EXTRACTION` | 01:00 daily | Extracts the accounts expiring at the lead days and runs their checks |
| `RNW_REEVALUATE` | 01:30 daily | Runs the checks again for renewals changed since the last run or expiring within 30 days |
| `RNW_NRNS_LETTERS` | 06:00 daily | NRNS flag and reminder letters, second-notice offers |
| `RNW_EXPIRY_SWEEP` | 00:15 daily | Closes renewals past expiry plus `RNW_NON_ACCEPTANCE_DAYS` with the non-acceptance letter |
| `RNW_LETTER_BATCH` | hourly | Delivery status of the letters; raises `RNW_LETTER_FAILED` |
| `RNW_GOLIVE_EXTRACTION` | none | Go-live take-over of the migrated policies, run once from the cut-over runbook |

The crons are listed in [`docs/operations/CONFIGURATION.md`](../operations/CONFIGURATION.md).

## 6. Uploads

| Handler | Screen | Content |
|---|---|---|
| `RNW_DISPOSITION_UPLOAD` | My Dispositions | Dispositions of many renewals; then Declare Complete File for an expiry range and unit |
| `RNW_INSURER_RESPONSE` | Insurer Batches | Insurer replies matched by renewal reference and policy |
| `RNW_LAMD_REPORT` | LAMD Reports | Paid-off or RMU loans of a month, matched by PN |
| `RNW_ACCEPTANCE` | Letters | Client acceptances |
| `RNW_PACKAGE_MAP` | Renewal Setup, Package Map | Legacy package to BIBS product version, or Reject |
| `RNW_LEGACY_POLICIES` | Renewal Setup, Go-live | Migrated policies while the migration source is not connected |
| `RNW_RA_ALREADY_SENT` | Renewal Setup, Go-live | Renewal Advices sent by hand before go-live (never sent again) |

## 7. Parameters and lists

Parameters (System Parameters, category Renewal): `RNW_EXTRACTION_LEAD_DAYS` (140), `RNW_EXTRACTION_LEAD_DAYS_BY_SEGMENT`,
`RNW_BULK_INITIATION_SEGMENTS`, `RNW_CBG_SEGMENTS`, `RNW_CBG_STP_LINES`, `RNW_PN_REQUIRED_LINES`, `RNW_STP_SKIP_TL_REVIEW`,
`RNW_OUTSTANDING_THRESHOLD`, `RNW_FIN_IMPACT_TOLERANCE`, `RNW_RA_MIN_NOTICE_DAYS` (30), `RNW_RA_SECOND_NOTICE_DAYS` (15),
`RNW_NRNS_REMINDER_DAYS`, `RNW_NON_ACCEPTANCE_DAYS`, `RNW_REOPEN_DAYS`, `RNW_ESCALATION_DAYS`, `RNW_KYC_SEGMENTS`,
`RNW_RMU_UNIT`, `RNW_AUTO_PLACEMENT`, `RNW_REFERENCE_PREFIX`, `RNW_EXCLUDED_LINES`, `RNW_NAL_REASONS`,
`RNW_INVOICE_NO_REASONS`, `RNW_INSURER_REPLY_DAYS`, `RNW_EXCEPTION_AGEING_DAYS`, `MIG_GOLIVE_RENEWAL_TO` (2028-05-31),
`MIG_RENEWAL_URGENT_TO` (2028-01-31).

Lists of values: `RNW_DISPOSITION`, `RNW_NONRENEWAL_REASON`, `RNW_RETURN_REASON`, `RNW_TRANSFER_REASON`,
`RNW_FOLLOWUP_OUTCOME`, `RNW_FOLLOWUP_CHANNEL`, `RNW_OVERRIDE_REASON`, `RNW_INSURER_RESPONSE`, `RNW_LAMD_STATUS`.

Letter templates (document templates, maintained by `RNW_TEMPLATE_MAINTAIN`): `RNW_RA_FIRST`, `RNW_RA_SECOND`, `RNW_NAL`,
`RNW_NFR`, `RNW_NRNS_REMINDER`, `RNW_NON_ACCEPTANCE`, `RNW_INSURER_COVER`.

## 8. Alerts and notifications

Alerts: `RNW_RENEWAL_AT_RISK`, `RNW_EXTRACTION_FAILED`, `RNW_INSURER_OVERDUE`, `RNW_LETTER_FAILED`,
`RNW_EXCEPTION_AGEING`. Notifications: renewal assigned, transfer requested and decided, returned, posted, insurer
response, accepted, renewed, go-live extraction completed, package choice decided.

## 9. Reports

`RNW-EXPIRY-LIST`, `RNW-STATUS` (Marketing or Processing, detail or summary), `RNW-LISTING` (with escalation),
`RNW-INSURER-EXTRACT`, `RNW-RA-DISPATCH`, `RNW-SANITATION`, `RNW-DECISIONS`, `RNW-LAMD-MATCH`, `RNW-WORKLOAD`,
`RNW-GOLIVE`, `RNW-PACKAGE-REMAP`. Each multi-select criterion accepts several values or "all except"; each report is
limited to the user's scope.

## 10. Seed data

`V1910` adds the users `rnwtl`, `lamd` and `contactc`, the non-renewable risk code `CAR07`, the active Classification
rules and decision matrix and two package map rows. With the seed profile, `RenewalSeedData` extracts seven migrated
policies and walks them through the flows, so the Expiry List, My Dispositions, TL Review, Processing, Letters and
Follow-ups have rows.

## 11. Troubleshooting

| Symptom | Cause and remedy |
|---|---|
| "Open the Account History before you give the disposition" | Open the Account History tab of the renewal and select View Account History, then set the disposition |
| A renewal is not posted | A blocking check fails (shown on the record); fix the data, wait for the re-evaluation, or ask the Team Leader to override it |
| A migrated policy is in Exception with "package" in the message | Its legacy package has no single mapping: propose the package on the record (Propose Package); a second member of the processing team approves it in Renewal Setup, Package Choices |
| The RA is refused as late | The expiry is closer than `RNW_RA_MIN_NOTICE_DAYS`: generate again with the late confirmation ticked |
| An insurer reply does not move the renewal | The reply did not match (reference or policy), was late or conflicting: see the Insurer tab; the Team Leader may override the mismatch |
