# Employee Benefits (BRD-8)

Guide of the Employee Benefits module of iNXT BrokerVerse (`eb`): BDOI's group health (HMO), group life (GLI) and
group personal accident (GPA) programmes for corporate clients. The build design is
[`docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md`](../architecture/EMPLOYEE_BENEFITS_DESIGN.md); section 16 records what
each wave built. The requirements are in [`docs/requirements/BDOI_EB_BRD_SPEC.md`](../requirements/BDOI_EB_BRD_SPEC.md)
(BRID-001 to 030) and the functional specification in
[`FRS_BRD08_EMPLOYEE_BENEFITS.md`](../deliverables/src/frs/FRS_BRD08_EMPLOYEE_BENEFITS.md) (FR-EB-001 to 062).

Employee Benefits is in BDOI **Drop 2** as "Employee Benefits (no portal feature)": insurers and client HR send their
files by e-mail or an agreed channel, and the EB users upload them with the source INSURER or CLIENT. There is no partner
portal and no external user.

Status: built without the portal (design sections 16.4 to 16.8): programmes and cycles, the renewal advice with its
job and reminders, client feedback, the document register, the Broker on Record, franchise requests, terms of reference
and insurer requests, proposals and revisions, the comparative with its sign-off and value-threshold approval, the
client's confirmation with Trigger Placement, submissions to insurers, the member roster, member changes, the SOA
register, the tracked items, the EB set-up and the seven EB reports.

## 1. Purpose

- A **programme** (`EBP-<yyyy>-nnnnnn`) groups a client's benefit lines (list `EB_BENEFIT_LINE`), each with the
  incumbent insurer, the current policy and ARN, the period and the headcount; the team (list `EB_TEAM`), the funding
  (employer or voluntary), the account officer, the renewal flag and the HR contacts who receive the renewal advice and
  the SOAs.
- A **cycle** (`EBC-<yyyy>-nnnnnn`) is one policy year of a programme, typed **New Business** or **Renewal**, with its
  `EB_CYCLE` work case. At most one cycle is open per programme and policy year.
- The **renewal advice** is sent automatically before expiry (job `EB_RENEWAL_ADVICE`) or by the AO, with reminders until
  the client's **feedback** arrives.
- Every EB file is registered in the **document register** of the cycle with its type, process tag, version and source,
  and each department sees only the document types it may see.
- The client's signed **Broker on Record** is uploaded and validated before the cycle goes to market.
- When the client confirms, **one account per benefit line** is created with the cycle's business type; from there the
  BRD-1 placement, issuance and booking run unchanged. The cycle is placed when every account is booked.
- Going to market: **franchise** requests to the chosen insurers, the **terms of reference** (TOR) released to the
  insurers that approved, their **proposals** (and revised proposals after a client revision), the **comparative** of
  the validated proposals, signed off by the team lead and, above a **value threshold**, approved by BDOI Management,
  then presented to the client.
- The client's **confirmation** of the chosen proposal per line is recorded with its evidence; **Trigger Placement**
  creates the accounts and submits them to Processing. **Submissions** send the documents of a process to an insurer,
  checked against the **required documents** of EB Setup.
- The **member roster** of each policy year comes from the client's master list; **member changes** (additions,
  deletions, plan and data changes) are relayed to the insurer, billed, validated by Processing and applied to the roster.
- Insurer **SOAs** are registered, validated by Processing and released to the client and Collection.
- **Tracked items** (contract, HMO card, card replacement, billing) are followed up automatically (job
  `EB_ITEM_FOLLOWUP`) and escalated to the AO.

Employee Benefits posts **no accounting entry** (design section 5): booking, commission and collections belong to the
BRD-1 and Operations modules.

## 2. Personas and roles

| Persona | Role | SIT/UAT user(s) | What they do |
|---|---|---|---|
| Marketing AO (EB) | `EB_AO` | `ebao`, `ebao2` | Programmes, cycles, renewal advice, feedback, documents, BOR, franchise, TOR and requests, proposals, comparative, confirmation and Trigger Placement, submissions, roster, member changes, pending items |
| Marketing TL / UH (EB) | `EB_TL` | `ebtl` | Views programmes; signs off or returns the comparative (not one they prepared); assigns work |
| BDOI Management | `EB_MANAGEMENT` | `ebmgmt` | Views programmes; approves or returns a comparative above the value threshold |
| Processing (EB) | `EB_PROCESSOR` | `ebproc` | Documents, BOR validation, submissions, member change billing and validation, SOA register, pending items, the placement of the accounts (BRD-1 Processing rights) |
| Processing Supervisor | `EB_PROC_SUPERVISOR` | `ebprocsup` | As Processing, plus work assignment |
| Collection (EB) | `EB_COLLECTION` | `ebcoll` | Views programmes, pending items and the SOA register; billing documents |
| Business Administrator | `BUSINESS_ADMIN` | `badmin` | EB Setup: threshold rules and required documents (maker-checker) |

Permissions (enum `security.domain.Permission`, grants in V1030): `EB_VIEW`, `EB_MARKET`, `EB_COMPARATIVE_APPROVE`,
`EB_THRESHOLD_APPROVE`, `EB_PROCESS`, `EB_COLLECT`, `EB_SETUP`, `EB_REPORT_VIEW`. The SIT/UAT password is held in the
seed configuration.

## 3. Screens

Group **Client & Policy**, section **Employee Benefits**, after Non-Package Management (`features/eb/module.ts`):

| Screen | Route | Permission | What it shows |
|---|---|---|---|
| EB Home | `/eb` | EB_VIEW | Tile counts: RA due, awaiting feedback, franchise pending, proposals outstanding, comparatives to sign off, threshold approvals, with client, member changes open, SOAs to validate, pending items overdue; each opens its list |
| Programmes | `/eb/programmes` | EB_VIEW | Tabs Renewal Due, In Progress, With Client, In Placement, Placed, Lost, All; search by programme number, client or name; filters cycle stage and team; selection and **Send RA** (EB_MARKET) with a result list |
| New Programme | `/eb/programmes/new` | EB_MARKET | Client (prospect or confirmed), profile, benefit lines and HR contacts |
| Programme | `/eb/programmes/:id` | EB_VIEW | Record header (programme, client and cycle chips, status, cycle stage, flags, key facts), the workflow panel of the current cycle (Send RA, Record Feedback, Start Requirements, Stay with Incumbent, Go to Market; Close as Lost and Not Renewed with a reason), actions Edit Programme, Add Pending Item, Open Cycle, and the tabs Cycle, Lines, Contacts, Documents, BOR, Franchise, TOR & Requests, Proposals, Comparative, Confirmation (with **Trigger Placement**), Submissions, Accounts, Members, Member Changes, Billing & SOA, Pending Items and History |
| Pending Items | `/eb/pending-items` | EB_VIEW | Tracked items by member, type, party, status and past due; Add Pending Item, Mark Received, Mark Released, Close Item (EB_MARKET or EB_PROCESS) |
| Comparative | `/eb/comparatives/:id` | EB_VIEW | Premium matrix per benefit line with the lowest premium and the recommendation, terms and capability with the deviations, approvals, comments; Submit for Sign-off, Sign Off, Approve Above Threshold, Return to AO, Present to Client; PDF and Excel |
| Member Changes | `/eb/member-changes` | EB_VIEW | Open changes (or by status) across programmes; the change dialog with Relay to Insurer, Record Billing, Validate (EB_PROCESS), Close and Apply, Submit to Insurer |
| SOA Register | `/eb/soa` | EB_PROCESS or EB_COLLECT | SOAs by status and insurer; the SOA dialog with Validate, Reject, Release (EB_PROCESS) |
| EB Setup | `/eb/setup` | EB_SETUP | Threshold rules and required documents; authorize (another user), deactivate |

The client 360 page lists the client's programmes (`EbClientRecords`). Help entries are in `features/eb/help.ts`.

## 4. Lifecycle

### 4.1 Cycle (`EB_CYCLE`)

```
OPEN --send_ra (job or Send RA)--> RA_SENT --record_feedback--> REQUIREMENTS     renewal
OPEN --start--> REQUIREMENTS                                                     new business
REQUIREMENTS --stay_with_incumbent--> INCUMBENT_TERMS                            renewal
REQUIREMENTS --remarket (validated BOR)--> FRANCHISE                             both ("Go to Market")
FRANCHISE --release_tor (first requests)--> PROPOSALS
INCUMBENT_TERMS / PROPOSALS / REVISION --build_comparative--> COMPARATIVE --submit--> FOR_SIGNOFF
FOR_SIGNOFF --approve--> READY_TO_PRESENT      (or approve_to_threshold --> THRESHOLD_APPROVAL --approve-->)
FOR_SIGNOFF / THRESHOLD_APPROVAL --return (reason)--> COMPARATIVE
READY_TO_PRESENT --present--> WITH_CLIENT --request_revision--> REVISION
WITH_CLIENT --confirm--> CONFIRMED      (or reconfirm_threshold --> THRESHOLD_APPROVAL when the chosen proposals meet a rule)
CONFIRMED --trigger_placement--> IN_PLACEMENT --placed (every account booked)--> PLACED
any open stage --close_lost / not_renewed (reason EB_LOST_REASON)--> CLOSED_LOST / NOT_RENEWED
```

The EB services check their rules and then move the work case (`systemTransition`); `EbCycleMirror` keeps the stage on the
cycle. Closing as lost records the outcome LOST (the programme becomes LOST when it was a prospect or a renewal); not
renewed records NOT_RENEWED and the programme becomes LAPSED.

### 4.2 Programme status

PROSPECT (no current policy) or ACTIVE (a line has a current policy or ARN) at creation; ACTIVE after a placement; LOST
or LAPSED after a lost or not renewed cycle; INACTIVE refuses new cycles.

## 5. Business rules

| Rule | Where |
|---|---|
| A programme needs a client (prospect or confirmed, not inactive), a name, a team, a funding, an account officer holding EB_MARKET, at least one benefit line and one HR contact with a valid e-mail | `ProgrammeRules` |
| One open cycle per programme and policy year; the business type is required; a renewal cycle only for a programme flagged for renewal | `CycleService` |
| The renewal advice announces the earliest expiry of the active lines; the job sends it when the expiry is within `EB_RA_LEAD_DAYS` (135) days, to ACTIVE programmes flagged for renewal with an HR contact receiving it; otherwise it raises `EB_RA_NOT_SENT` to the AO | `RenewalAdviceService`, `RenewalAdviceJob` |
| The advice is a PDF from template `EB_RENEWAL_ADVICE`, stored as `RENEWAL_ADVICE` (linked to the programme, the client and the expiring accounts) and e-mailed password-protected with the password in a second e-mail | `RenewalAdviceService`, `RenewalAdviceLetter` |
| Reminders from template `EB_RA_REMINDER` at `EB_RA_REMINDER_DAYS` (120, 105, 90) before expiry, one per run, while the cycle waits for feedback | `RenewalAdviceService.remindIfDue` |
| Feedback needs its channel, a date received not after today, and text or at least one file; the first feedback stops the reminders and moves the cycle to Client Requirements | `FeedbackService` |
| Every uploaded document needs its cycle, an EB document type and its process tag; a new upload of a type supersedes the active version on the cycle; the BOR is uploaded on its own tab | `EbDocumentService` |
| The BOR is PDF or Word, one version waits for validation at a time; validation needs the three checks and the validity dates; a rejection needs a reason; the uploader is notified | `BorService` |
| Going to market needs a validated BOR of the programme in force on the business date | `BorService` (port `BorGate`) |
| Placement creates one account per line: RENEWAL accounts refer to the line's expiring ARN, NEW_BUSINESS accounts otherwise, origin EMPLOYEE_BENEFITS; the contract of each account becomes a tracked item | `EbPlacementService` |
| A tracked item needs its type, subject, responsible party and due date; receiving needs the date received, closing a pending item too; only pending items change | `TrackedItemService` |
| Follow-ups every `EB_FOLLOWUP_DAYS` (5) working days past due, escalation after `EB_FOLLOWUP_MAX` (3): alert `EB_ITEM_OVERDUE` and notice `EB_ITEM_ESCALATED` | `ItemFollowUpService`, `FollowUpSchedule` |
| A franchise request needs a validated BOR and goes once per insurer; the decision needs the insurer's reply; a request without a decision expires `EB_FRANCHISE_GRACE_DAYS` (2) working days after its due date | `FranchiseService`, `FranchiseExpiryJob` |
| Only insurers that approved the franchise receive the released TOR; a TOR changed after release is a new version sent again to the open requests | `InsurerRequestService`, `TorService` |
| A proposal needs its document and the premium of at least one plan; answers only to items of the released TOR; capability ratings 1 to 5; a revised proposal answers every changed item | `EbProposalRules`, `RevisionService` |
| The comparative compares the validated proposals once no request is open; the lowest premium per line is recommended by default | `EbComparativeService` |
| The sign-off is by an `EB_COMPARATIVE_APPROVE` holder who did not prepare it; when the recommended TSI or annual premium of a line meets a threshold rule, an approver holding the rule's permission, neither the maker nor the AO, approves as well | `ComparativeApproval`, `ThresholdEvaluator` |
| The confirmation needs the presented comparative, the channel, the evidence and a validated proposal per active line; the rules are evaluated again on the chosen proposals | `ConfirmationService` |
| Trigger Placement waits for the threshold approval, the mandatory placement documents and, for an insurer not accredited on the business date, its accreditation approval; each account gets the documents and is submitted to Processing | `PlacementTrigger` |
| A submission needs the mandatory documents of its process and only documents of the record | `SubmissionService` |
| Set-up changes wait for another EB_SETUP user; records are deactivated, never deleted | `EbSetupService` |
| A roster upload is a staged version accepted or rejected by the AO; one employee number per version | `RosterService`, `MasterListBulkHandler` |
| A member change is checked against the accepted roster (known or new employee, data of an addition, new plan, effective date in the policy period); a financial change needs its billed amount; validation raises the endorsement request; closing applies the lines to the roster | `MemberChangeRules`, `MemberChangeService`, `MemberChangeEffects` |
| An SOA is refused a second time (same insurer SOA number or same file); Processing validates it with the invoices it bills, then releases it | `EbSoaService`, `SoaRelease` |

Messages of the rules are listed in design sections 16.7 and 16.8. All values are parameters (category EMPLOYEE_BENEFITS) or lists
of values maintained by BDOI; the defaults are placeholders until the open questions EBQ02, EBQ07, EBQ10, EBQ16 and
EBQ20 are answered.

## 6. Jobs

| Job | Cron (UTC) | Does |
|---|---|---|
| `EB_RENEWAL_ADVICE` | `brokerverse.jobs.eb-renewal-advice-cron` (06:00 PHT) | Renewal advices inside the lead time, `EB_RA_NOT_SENT` alerts, then the reminders due. One transaction per programme and per reminder |
| `EB_ITEM_FOLLOWUP` | `brokerverse.jobs.eb-item-followup-cron` (07:00 PHT) | Follow-ups and escalations of the pending items past due. One transaction per item |
| `EB_FRANCHISE_EXPIRY` | `brokerverse.jobs.eb-franchise-expiry-cron` (06:30 PHT) | Franchise requests without a decision past their due date and the grace days are expired. One transaction per request |

All run on the `jobs` deployment and can be started with "Run now" on Administration, Scheduled Jobs. Configuration:
[`docs/operations/CONFIGURATION.md`](../operations/CONFIGURATION.md).

## 7. Notifications, alerts and templates

- Notification events: `EB_FEEDBACK_RECEIVED` (to the AO), `EB_BOR_DECIDED` (to the uploader), `EB_PLACEMENT_TRIGGERED`
  (to Processing), `EB_ITEM_ESCALATED` (to the AO), `EB_COMPARATIVE_DECIDED`, `EB_ROSTER_STAGED`, `EB_SUBMISSION_SENT`,
  `EB_INVOICE_PAID` and the other events of V1030.
- Alerts (daily check): `EB_RA_NOT_SENT`, `EB_ITEM_OVERDUE`, `EB_FRANCHISE_OVERDUE`, `EB_FRANCHISE_ADVICE_LATE`,
  `EB_PROPOSAL_OVERDUE`, `EB_COMPARATIVE_LATE`, `EB_SOA_VALIDATION_LATE`, each once per record.
- Templates: `EB_RENEWAL_ADVICE`, `EB_RA_REMINDER`, `EB_ITEM_FOLLOWUP`, the franchise, TOR, request and comparative
  templates of V1030, `EB_MEMBER_CHANGE_RELAY`, `EB_SUBMISSION_COVER`, `EB_SOA_RELEASE` (Document Templates; wording by
  BDOI, EBQ21). Files to insurers and clients go password-protected.
- Approvals: comparatives to sign off or approve above the threshold, and set-up changes to authorize, are in My
  Approvals.

## 8. Accounts and the shared business type

`EbPlacementService.trigger(companyId, cycleId, lines)` is the contract of the client-confirmation step: each
`LinePlacement` carries the line number, the account draft of the chosen proposal and its premium. The accounts are
created through `AccountService.createDraft` with `AccountClassification` RENEWAL (with the expiring ARN) or
NEW_BUSINESS, origin `EMPLOYEE_BENEFITS`, so the booked register, production and placement reports can be filtered by
business type (shared work item BT0). Trigger Placement (`PlacementTrigger`) builds the lines from the confirmed
proposals, links the confirmation, proposals, TOR, BOR and required documents to each account and submits it to
Processing; the seed products `EBHMO01`, `EBGLI01`, `EBGPA01` pass the submission checks. `PlacementProgressListener`
follows `AccountStatusChanged`. The Accounts tab of the programme lists them.

## 9. Documents and files

Every file is an attachment in the file store (`DocumentService`, owner type `Attachment`); no EB table holds file
content. Document types are the 16 `EB_*` values and `RENEWAL_ADVICE` of list `DOCUMENT_TYPE` with their access classes
(V1031): for example the utilization report is not listed to Collection, the direct billing is. Downloads go through
the attachment API, which applies the access classes and audits the download.

## 10. Reports and data

The EB reports are in the Report Centre under Employee Benefits (`EB_REPORT_VIEW`), with the common parameters period, team, AO, client, benefit line, insurer and business type, in PDF, Excel and Word: `EB-PRODUCTION`, `EB-RENEWAL`, `EB-PLACEMENT`, `EB-NEW-BUSINESS`, `EB-TAT` (working days per activity with the breach flag), `EB-PENDING-ITEMS`, `EB-FRANCHISE`. The read view `eb_tat_v` (V1036) gives the received and
released stamps of the TAT activities with the programme's team and AO; the activity log is the History tab of the
programme. Retention: rule `EB_PROGRAMME` (lost or inactive programmes) through `EbProgrammeRetentionProvider`.

## 11. Seed data (seed profile only)

- V1930: the SIT/UAT users of section 2.
- V1931: six programmes of the seed corporate clients and prospects, dated from the load date: Pacific Harbor Logistics
  (renewal advice sent, waiting for feedback; and a voluntary GPA programme with feedback received), Luzon Agri-Industrial
  (renewal due; and a programme not flagged for renewal, which raises `EB_RA_NOT_SENT`), Bayside Builders (new business
  in requirements) and Metro Dental Clinic Partners (new business lost on price).
- V1932: an HMO card 12 days past due with one follow-up, a billing due in 5 days, a received card replacement.
- V1933: the EB product lines and products `EBHMO01`, `EBGLI01`, `EBGPA01`, the HMO providers `HMO-MHC` (accredited) and
  `HMO-KHP` (accreditation lapsed) with commission rates, the threshold rules (TSI 500M, annual premium 20M) and the
  required documents per process.

## 12. Tests

Backend: `eb.EbProgrammeIT`, `EbRenewalAdviceIT`, `EbDocumentsBorIT`, `EbPlacementIT`, `EbTrackedItemsIT`,
`EbFoundationIT`, `api.EbProgrammesApiIT`, `api.EbFoundationApiIT`, the EB block of `api.ApiSmokeIT`, and the unit tests
`EbServicingDomainTest`, `RenewalTargetTest`, `FollowUpScheduleTest`; `EbMarketingIT`, `EbServicingIT`, `EbReportsIT`,
`EbMonitoringIT`, `api.EbMarketApiIT`, `security.PersonaMenusIT` (suite BRD-8), and the unit tests
`EbMarketingDomainTest`, `ThresholdEvaluatorTest`, `SoaPaymentListenerTest`, `MemberChangeEffectsTest`. Frontend:
`features/eb/module.test.tsx`, `programmes/programmePages.test.tsx`, `programmes/programmeLogic.test.ts`,
`pending/pendingItems.test.tsx`, `market/marketTabs.test.tsx`, `confirmation/confirmation.test.tsx`,
`members/members.test.tsx`, `soa/soaSetup.test.tsx`, `navigation/personaMenus.test.ts`.

## 13. Open points

- EBQ01: the full list of benefit lines and HMO provider panels; the seed products and providers of V1933 stand in.
- EBQ02: renewal advice lead time and reminder days (135; 120, 105, 90 by default).
- EBQ05 / EBQ06: whether a renewal without remarketing needs a BOR; e-signature verification (the validator attests).
- EBQ20: tracked item statuses, thresholds and recipients (5 working days, 3 follow-ups by default).
- EBQ21: template layouts and TAT start and stop events.
- The seed has no storyline past the requirements (no cycle in proposals, roster, member change or SOA): those screens
  fill as the users run the flow. Only `badmin` holds EB_SETUP in the seed, so a set-up change waits for a second
  EB_SETUP user.
