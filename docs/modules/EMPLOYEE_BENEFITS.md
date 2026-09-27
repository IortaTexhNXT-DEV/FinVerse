# Employee Benefits (BRD-8)

Guide of the Employee Benefits module of iNXT BrokerVerse (`eb`): BDOI's group health (HMO), group life (GLI) and
group personal accident (GPA) programmes for corporate clients. The build design is
[`docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md`](../architecture/EMPLOYEE_BENEFITS_DESIGN.md); section 16 records what
each wave built. The requirements are in [`docs/requirements/BDOI_EB_BRD_SPEC.md`](../requirements/BDOI_EB_BRD_SPEC.md)
(BRID-001 to 030) and the functional specification in
[`FRS_BRD08_EMPLOYEE_BENEFITS.md`](../deliverables/src/BRD-08_Employee_Benefits/FRS_BRD08_EMPLOYEE_BENEFITS.md) (FR-EB-001 to 062).

Employee Benefits is in BDOI **Drop 2** as "Employee Benefits (no portal feature)": insurers and client HR send their
files by e-mail or an agreed channel, and the EB users upload them with the source INSURER or CLIENT. There is no partner
portal and no external user.

Status: the foundation (E0) and the first slice of the business waves E1-B and E1-C are built: programmes and cycles,
the renewal advice with its job and reminders, client feedback, the document register, the Broker on Record, the accounts
of a confirmed cycle with the shared business type, and the tracked items with their follow-up job. The marketing steps
after the requirements (franchise, TOR, insurer requests, proposals, comparative, threshold approval, client
confirmation), the member roster and member changes, the SOA register and the EB reports are designed and follow.

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
- **Tracked items** (contract, HMO card, card replacement, billing) are followed up automatically (job
  `EB_ITEM_FOLLOWUP`) and escalated to the AO.

Employee Benefits posts **no accounting entry** (design section 5): booking, commission and collections belong to the
BRD-1 and Operations modules.

## 2. Personas and roles

| Persona | Role | SIT/UAT user(s) | What they do |
|---|---|---|---|
| Marketing AO (EB) | `EB_AO` | `ebao`, `ebao2` | Programmes, cycles, renewal advice, feedback, documents, BOR upload and validation, pending items |
| Marketing TL / UH (EB) | `EB_TL` | `ebtl` | Views programmes; comparative sign-off (later wave); assigns work |
| BDOI Management | `EB_MANAGEMENT` | `ebmgmt` | Views programmes; threshold approval (later wave) |
| Processing (EB) | `EB_PROCESSOR` | `ebproc` | Documents, BOR validation, pending items, the placement of the accounts (BRD-1 Processing rights) |
| Processing Supervisor | `EB_PROC_SUPERVISOR` | `ebprocsup` | As Processing, plus work assignment |
| Collection (EB) | `EB_COLLECTION` | `ebcoll` | Views programmes and pending items; billing documents |
| Business Administrator | `BUSINESS_ADMIN` | `badmin` | EB set-up (later wave) |

Permissions (enum `security.domain.Permission`, grants in V1030): `EB_VIEW`, `EB_MARKET`, `EB_COMPARATIVE_APPROVE`,
`EB_THRESHOLD_APPROVE`, `EB_PROCESS`, `EB_COLLECT`, `EB_SETUP`, `EB_REPORT_VIEW`. The SIT/UAT password is held in the
seed configuration.

## 3. Screens

Group **Client & Policy**, section **Employee Benefits**, after Non-Package Management (`features/eb/module.ts`):

| Screen | Route | Permission | What it shows |
|---|---|---|---|
| EB Home | `/eb` | EB_VIEW | Tile counts: RA due, awaiting feedback, franchise pending, proposals outstanding, comparatives to sign off, threshold approvals, with client, pending items overdue (member changes open has no count yet); each opens its list |
| Programmes | `/eb/programmes` | EB_VIEW | Tabs Renewal Due, In Progress, With Client, In Placement, Placed, Lost, All; search by programme number, client or name; filters cycle stage and team; selection and **Send RA** (EB_MARKET) with a result list |
| New Programme | `/eb/programmes/new` | EB_MARKET | Client (prospect or confirmed), profile, benefit lines and HR contacts |
| Programme | `/eb/programmes/:id` | EB_VIEW | Record header (programme, client and cycle chips, status, cycle stage, flags, key facts), the workflow panel of the current cycle (Send RA, Record Feedback, Start Requirements, Stay with Incumbent, Go to Market; Close as Lost and Not Renewed with a reason), actions Edit Programme, Add Pending Item, Open Cycle, and the tabs Cycle, Lines, Contacts, Documents, BOR, Accounts, Pending Items and History |
| Pending Items | `/eb/pending-items` | EB_VIEW | Tracked items by member, type, party, status and past due; Add Pending Item, Mark Received, Mark Released, Close Item (EB_MARKET or EB_PROCESS) |
| Comparative, Member Changes, SOA Register, EB Setup | `/eb/comparatives/:id`, `/eb/member-changes`, `/eb/soa`, `/eb/setup` | as design 10.1 | Placeholders until their waves |

The client 360 page lists the client's programmes (`EbClientRecords`). Help entries are in `features/eb/help.ts`.

## 4. Lifecycle

### 4.1 Cycle (`EB_CYCLE`)

```
OPEN --send_ra (job or Send RA)--> RA_SENT --record_feedback--> REQUIREMENTS     renewal
OPEN --start--> REQUIREMENTS                                                     new business
REQUIREMENTS --stay_with_incumbent--> INCUMBENT_TERMS                            renewal
REQUIREMENTS --remarket (validated BOR)--> FRANCHISE                             both ("Go to Market")
... FRANCHISE, PROPOSALS, COMPARATIVE, FOR_SIGNOFF, THRESHOLD_APPROVAL, READY_TO_PRESENT,
    WITH_CLIENT, REVISION, CONFIRMED                                             later waves
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

Messages of the rules are listed in design section 16.7. All values are parameters (category EMPLOYEE_BENEFITS) or lists
of values maintained by BDOI; the defaults are placeholders until the open questions EBQ02, EBQ07, EBQ10, EBQ16 and
EBQ20 are answered.

## 6. Jobs

| Job | Cron (UTC) | Does |
|---|---|---|
| `EB_RENEWAL_ADVICE` | `brokerverse.jobs.eb-renewal-advice-cron` (06:00 PHT) | Renewal advices inside the lead time, `EB_RA_NOT_SENT` alerts, then the reminders due. One transaction per programme and per reminder |
| `EB_ITEM_FOLLOWUP` | `brokerverse.jobs.eb-item-followup-cron` (07:00 PHT) | Follow-ups and escalations of the pending items past due. One transaction per item |

Both run on the `jobs` deployment and can be started with "Run now" on Administration, Scheduled Jobs. Configuration:
[`docs/operations/CONFIGURATION.md`](../operations/CONFIGURATION.md).

## 7. Notifications, alerts and templates

- Notification events: `EB_FEEDBACK_RECEIVED` (to the AO), `EB_BOR_DECIDED` (to the uploader), `EB_PLACEMENT_TRIGGERED`
  (to Processing), `EB_ITEM_ESCALATED` (to the AO). The other events of V1030 belong to the later waves.
- Alerts: `EB_RA_NOT_SENT` (per programme and expiry), `EB_ITEM_OVERDUE` (per item).
- Templates: `EB_RENEWAL_ADVICE`, `EB_RA_REMINDER`, `EB_ITEM_FOLLOWUP` (Document Templates; wording by BDOI, EBQ21).

## 8. Accounts and the shared business type

`EbPlacementService.trigger(companyId, cycleId, lines)` is the contract of the client-confirmation step: each
`LinePlacement` carries the line number, the account draft of the chosen proposal and its premium. The accounts are
created through `AccountService.createDraft` with `AccountClassification` RENEWAL (with the expiring ARN) or
NEW_BUSINESS, origin `EMPLOYEE_BENEFITS`, so the booked register, production and placement reports can be filtered by
business type (shared work item BT0). `PlacementProgressListener` follows `AccountStatusChanged`. The Accounts tab of the
programme lists them.

## 9. Documents and files

Every file is an attachment in the file store (`DocumentService`, owner type `Attachment`); no EB table holds file
content. Document types are the 16 `EB_*` values and `RENEWAL_ADVICE` of list `DOCUMENT_TYPE` with their access classes
(V1031): for example the utilization report is not listed to Collection, the direct billing is. Downloads go through
the attachment API, which applies the access classes and audits the download.

## 10. Reports and data

The EB reports (design section 9) follow with their wave. The read view `eb_tat_v` (V1036) gives the received and
released stamps of the TAT activities with the programme's team and AO; the activity log is the History tab of the
programme. Retention: rule `EB_PROGRAMME` (lost or inactive programmes) through `EbProgrammeRetentionProvider`.

## 11. Seed data (seed profile only)

- V1930: the SIT/UAT users of section 2.
- V1931: six programmes of the seed corporate clients and prospects, dated from the load date: Pacific Harbor Logistics
  (renewal advice sent, waiting for feedback; and a voluntary GPA programme with feedback received), Luzon Agri-Industrial
  (renewal due; and a programme not flagged for renewal, which raises `EB_RA_NOT_SENT`), Bayside Builders (new business
  in requirements) and Metro Dental Clinic Partners (new business lost on price).
- V1932: an HMO card 12 days past due with one follow-up, a billing due in 5 days, a received card replacement.

## 12. Tests

Backend: `eb.EbProgrammeIT`, `EbRenewalAdviceIT`, `EbDocumentsBorIT`, `EbPlacementIT`, `EbTrackedItemsIT`,
`EbFoundationIT`, `api.EbProgrammesApiIT`, `api.EbFoundationApiIT`, the EB block of `api.ApiSmokeIT`, and the unit tests
`EbServicingDomainTest`, `RenewalTargetTest`, `FollowUpScheduleTest`. Frontend: `features/eb/module.test.tsx`,
`programmes/programmePages.test.tsx`, `programmes/programmeLogic.test.ts`, `pending/pendingItems.test.tsx`.

## 13. Open points

- EBQ01: EB products (HMO, GLI, GPA) and HMO provider panels in the catalogue; until then the placement accounts use the
  product of the proposal and are completed on the account screens.
- EBQ02: renewal advice lead time and reminder days (135; 120, 105, 90 by default).
- EBQ05 / EBQ06: whether a renewal without remarketing needs a BOR; e-signature verification (the validator attests).
- EBQ20: tracked item statuses, thresholds and recipients (5 working days, 3 follow-ups by default).
- EBQ21: template layouts and TAT start and stop events.
- Later waves: franchise, TOR and insurer requests, proposals and revisions, comparative with sign-off and threshold
  approval, client confirmation (which calls the placement contract), submissions and required documents, member roster
  and member changes, SOA register, EB reports and EB set-up.
