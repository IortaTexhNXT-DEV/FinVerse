# Submitted Policies (BRD-12)

Module guide of `submitted` (package `com.iortatechnxt.brokerverse.submitted`, screens
`frontend/src/features/submitted`). Design: [`SUBMITTED_POLICIES_DESIGN.md`](../architecture/SUBMITTED_POLICIES_DESIGN.md)
(section 17 as built). The renewal of a handed-over record is Renewal's (wave R3, [`RENEWAL.md`](RENEWAL.md)).

## 1. Purpose

- A **submitted policy** is a policy the bank's borrower bought elsewhere and submitted to the bank. It is kept in the
  **masterlist** (`SBM-<yyyy>-nnnnnn`) with its loan, assured, policy, risk and marks (FFY, group employee, No Touch). It
  is not an account and posts nothing until it is renewed with BDOI.
- **Intake**: the source uploads of the bank (LFS, HLS, CIU, SPI, Loan Booking, IA masterlist), the documents read by
  **extraction** and confirmed by the handler, entry by hand and the **migration** of the Excel masterlists. A policy
  already in the masterlist (same PN, or policy number) is updated, never duplicated.
- **Processing runs** apply the active rule sets of four steps: **sanitation** (completeness, duplicates), **matching**
  with the latest LAMD loan snapshot, **classification** (in force or submitted) and **disposition** (the bucket and
  the renewal tag), then the **insurer limits**. A record no rule places falls out with its reason; the result of every
  step is kept.
- **Reviews and IAAF**: the Policy Review Officer records the reviews (findings are e-mailed to the bank counterpart);
  once a review is adequate, the IAAF is generated, approved by the levels of the approval matrix and sent.
- **Terms of Reference**: a record above the insurer limits gets a TOR with the proposed terms, approved by TSU and
  released to the Account Officer.
- **Renewal**: the expiry scan hands the records For Renewal near their expiry to Renewal with the insurer of the insurer
  rules (Renew with BDOI does it by hand). The masterlist follows the renewal account (placed, booked) and closes the
  records Renewal closes without booking. The insurer can be re-assigned while its hold cover is not accepted.
- **Letters** of the letter rules (reminders, renewal notices, proposals) go by e-mail, to the bank counterpart or to the
  mail house in print batches.
- **Handling fees** billed to the borrowers are tagged to their payments in the unapplied collections (by PN or
  location); Cashiering recognises them as income and issues the official receipt. **No Touch** accounts are exported
  monthly to their insurer, whose return is billed with a service invoice.

## 2. Personas and roles

| Persona | Role | SIT/UAT user | What they do |
|---|---|---|---|
| Submitted Handler | `SBM_HANDLER` | `sbmhandler` (CBG Motor), `firehandler` (CBG Fire) | Intake, extraction review, records of their segment, TOR preparation |
| Sanitation Handler | `SBM_SANITATION` | `sanitation` | Uploads, processing runs, handler actions, letters |
| Team Lead | `SBM_TL` | `sbmtl` | Everything of the handlers, assignment, rules, handling fees, migration |
| Submitted Checker | `SBM_CHECKER` | `sbmchecker` | IAAF approval |
| Policy Review Officer | `SBM_POLICY_REVIEWER` | `polreview` | Reviews and IAAF |
| Handling Fee Handler | `SBM_UPP_HANDLER` | `sbmfee` | Handling fees and No Touch billing |
| Account Officer | `MKT_AO` | `ao`, `ao2` | Own records, TOR preparation, Renew with BDOI |
| Marketing Team Leader | `MKT_TL` | `mkttl` | IAAF approval (second level), rule approval |
| TSU | `TSU` | `tsu` | TOR approval |
| Rule administrator | `SBM_RULE_ADMIN` | (no seed user) | Rule maintenance |

A user sees the records of the segments of their scope (Setup, User Scopes); the roles of `SBM_OWN_RECORDS_ROLES`
(Account Officers) see only their own records.

## 3. Screens (Client & Policy, Submitted Policies)

| Screen | Path | Permission |
|---|---|---|
| Submitted Policies Home | `/submitted` | `SBM_VIEW` |
| Masterlist | `/submitted/masterlist` | `SBM_VIEW` (Assign Handler: `WORK_ASSIGN`; Run Processing: `SBM_PROCESS`) |
| Submitted policy | `/submitted/policies/:id` | `SBM_VIEW` |
| Upload & Intake | `/submitted/intake` | `SBM_INTAKE` or `SBM_MIGRATE` |
| Extraction Review | `/submitted/extractions` | `SBM_MAINTAIN` |
| Processing Runs | `/submitted/runs` | `SBM_VIEW` |
| Reviews & IAAF | `/submitted/reviews` | `SBM_VIEW` (`IAAF_PREPARE`, `IAAF_APPROVE` for the actions) |
| Terms of Reference | `/submitted/tors` | `SBM_VIEW` or `TOR_APPROVE` |
| Renewal Work List | `/submitted/renewals` | `SBM_VIEW` (Scan Now, Re-assign: `SBM_PROCESS`) |
| Letters & Print Batches | `/submitted/letters` | `SBM_LETTER_SEND` |
| Handling Fees | `/submitted/fees` | `SBM_HANDLING_FEE` |
| No Touch Billing | `/submitted/no-touch` | `SBM_HANDLING_FEE` |
| Submitted Policies Setup | `/submitted/setup` | `SBM_RULE_MAINTAIN` or `SBM_RULE_APPROVE` (Rule Sets tab: the rule editor with maker and checker) |

## 4. Life of a record

`RECEIVED` (intake) → `VALIDATED` (entered by hand or confirmed) → processing: `CLASSIFIED`, `IN_REVIEW`, `FOR_RENEWAL`,
`FOR_MANUAL_DISPOSITION` or `EXCLUDED` (a fallout stays where it is, flagged) → `RENEWAL_IN_PROGRESS` (handed to Renewal)
→ `PLACED` → `BOOKED`, or `NOT_RENEWED` / `CLOSED`. The handler disposes a record for renewal, excludes it with a
non-renewal reason, reinstates it or closes it; every change of a field is in the History tab.

## 5. Jobs

| Job | When (PHT) | What |
|---|---|---|
| `SBM_PROCESSING` | 21:30 | Processing run of the open records of every company |
| `SBM_EXPIRY_SCAN` | 22:00 | Hand-off of the records For Renewal within the lead days of their segment; pending hand-offs offered again |
| `SBM_LETTER_DISPATCH` | 06:30 | Letters due by the letter rules; print batches to the mail house |
| `SBM_HOLD_COVER_WATCH` | 07:00 | Insurer not accepted, hold cover of an unbooked renewal, records closed by Renewal |
| `SBM_HANDLING_FEE_TAGGER` | every 30 minutes | Tagging of the payments to the billed handling fees |
| `SBM_INTAKE_PULL` | not scheduled | Pull of the source files once a bank transport is connected |

## 6. Uploads

`SBM_LFS_INSURANCE`, `SBM_HLS_INSURANCE`, `SBM_CIU`, `SBM_SPI`, `SBM_LOAN_BOOKING`, `SBM_IA_MASTERLIST` (sources),
`SBM_LAMD` (loan snapshot), `SBM_MIGRATION` (Excel masterlists, legacy statuses through the status map),
`SBM_HANDLING_FEE_BILLING` (fees billed) and `SBM_NO_TOUCH_RETURN` (the insurer's return of a No Touch batch). Each has
its template on the upload screen; a committed source upload starts a processing run of its records.

## 7. Parameters and lists

Parameters of category SUBMITTED: `SBM_RENEWAL_LEAD_DAYS` (per segment: CBG Fire 150, CBG Motor 120),
`SBM_INSURER_ACCEPT_DAYS`, `SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS`, `SBM_REVIEW_SLA_DAYS`, `SBM_MANUAL_RENEWAL_SEGMENTS`,
`SBM_OWN_RECORDS_ROLES`, `SBM_NUMBER_PREFIX`. Lists: `SBM_SEGMENT`, `SBM_BUCKET` (the group of each bucket says whether it
is renewed, disposed by hand or excluded), `SBM_REASON`, `SBM_NON_RENEWAL_REASON`, `SBM_CONVERSION_STATUS`,
`SBM_LOAN_STATUS`, `SBM_LETTER_TYPE`, `SBM_DECLINE_REASON`, `SBM_IAAF_FINDING`, `SBM_RETURN_REASON`.

## 8. Alerts and notifications

Alerts `SBM_IAAF_SLA`, `SBM_TOR_SLA`, `SBM_INSURER_NOT_ACCEPTED`, `SBM_HOLD_COVER_UNBOOKED`, `SBM_LETTER_FAILED`. The
handler is told of an assignment; the approvers of each level of an IAAF or TOR are told when it reaches them; the
handler and the Account Officer are told when a renewal starts. IAAF and TOR waiting for approval are in My Approvals.

## 9. Reports

Report Centre, category Submitted Policies: `SBM-MASTERLIST`, `SBM-DOC-FALLOUT`, `SBM-PROCESS-FALLOUT`,
`SBM-NON-RENEWAL`, `SBM-MIGRATION-ERRORS`, `SBM-SANITATION`, `SBM-DISPOSITION`, `SBM-CLASSIFICATION`, `SBM-RENEWABLE`,
`SBM-IAAF`, `SBM-TOR`, `SBM-HANDLING-FEE`, `SBM-CONVERSION`, `SBM-NO-TOUCH`, `SBM-PR-CONVERSION`, `SBM-PR-MONITORING`,
`SBM-PERSISTENCY`, `SBM-PENETRATION`, `SBM-HOLD-COVER-GAP`, `SBM-LETTERS`. Each takes the company and a date range; the
reports of the masterlist are limited to the user's scope. View: `SBM_REPORT_VIEW`; export: `SBM_REPORT_EXPORT`.

## 10. Accounting

Two events: `SBM_HANDLING_FEE` (the payment leaves the unapplied collections as handling fee income with its output VAT;
posted by Cashiering's income disposition) and `SBM_NO_TOUCH_FEE` (the service fee receivable from the insurer, the income
and the output VAT; posted when a No Touch batch is billed). The seed rules use the accounts 2205, 4115 (Handling Fee
Income), 1236 (Service Fee Receivable - Insurers) and 2504.

## 11. Seed data

V1970 loads the users, scopes, GL accounts and rules, the active rule sets, insurer limits, insurer assignment and letter
rules, the approval matrix and a LAMD snapshot. `SubmittedSeedData` (seed profile, once) then takes 21 motor, fire,
corporate and retail policies through intake, assignment and a processing run (with fallout, FFY, employee and No Touch
records), records reviews with an IAAF approved and one waiting, a TOR of a policy above the motor limit, a renewal
started with Renew with BDOI, three handling fees and a No Touch export.

## 12. Troubleshooting

| Symptom | Check |
|---|---|
| A rule set cannot be changed | Only a draft is changed: open the active set and create a New Version |
| A record falls out with "No rule applies" | An active rule set of the step and segment exists (Setup, Rule Sets) and ends with a rule that always applies |
| A CBG record falls out with "No record in the LAMD loan snapshot" | Upload the latest LAMD snapshot, then run the processing again |
| Generate IAAF is disabled | The last review of the record is not adequate |
| "No approval level is defined" | The approval matrix has no level for the segment and sum insured |
| Hand-off stays Pending | The Renewal module refused it (message on the Renewal tab); it is offered again by the next scan |
| A handling fee stays Billed | The payment reference does not carry the PN or location, or several fees match (tab Several Matches) |
