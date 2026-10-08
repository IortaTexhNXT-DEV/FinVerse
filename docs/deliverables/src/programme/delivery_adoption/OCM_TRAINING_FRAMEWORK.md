---
# Organisational Change Management and Training Framework of the BIBS programme (BRD-00).
# Built by build_delivery_adoption.py; lines <!-- da:... --> are replaced by tables built from the YAML files.
title: Organisational Change Management and Training Framework
subtitle: Stakeholders and impact, persona journeys, communications, role-based learning paths, train-the-trainer, contextual help, usability tests and adoption KPIs
doc_type: Change Management and Training Framework
doc_code: Adoption
brd: BRD-00
name: Change Management and Training Framework
doc_id: BIBS-OCM-BRD-00
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: BIBS OCM and Training Framework
h1_page_break: false
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT change and training lead
    reviewer: iorta TechNXT Project Manager; iorta TechNXT Business Analyst lead
    approver: BIBS Product Owner (pending)
    change: First issue
distribution:
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Sponsor of the change and of the training"}
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Plan and resources"}
  - {name: "Business owners of BRD-01 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "Impact, journeys, learning paths and KPIs of their units"}
  - {name: "Head, Human Resources", role: Reviewer, organisation: BDOI, purpose: "Training calendar, attendance and learning records"}
  - {name: "BDOI IT", role: Reviewer, organisation: BDOI, purpose: "Training environment, sign-in, service desk"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivers the framework with the BDOI trainers"}
---

# Purpose, audience and scope

## Purpose

BIBS replaces e-mail, spreadsheets, paper hand-offs and the legacy systems (QPS, EBIX, ISYS) with one workflow for the whole broking cycle. This framework states how BDOI people are brought from today's way of working to BIBS: who is affected and how much, what each persona's work looks like before and after, how and when we communicate, how each persona is trained, how the application helps users at the moment of need, how usability is tested, and which adoption KPIs show that BIBS is used as intended.

## Audience

The BIBS Product Owner (sponsor), the business owners and change champions of each unit, the key users and BDOI trainers, Human Resources (training records), BDOI IT (training environment, service desk) and the iorta TechNXT team.

## Scope

All BDOI users of BIBS at go-live: about 1,344 named users and 429 concurrent users (umbrella BRD p.42), in Marketing, TSU and MBS, Processing, Operations (Cashiering, Remittance, Adjustment, Production Reconciliation, Commission Receivables), Collections, Comptrollership (FRBS, Disbursement, ACSL), Renewal, Claims, Employee Benefits, Customer Servicing, Compliance, Business and System Administration and Audit. External parties (clients, insurers, bank channels) are covered by the communication plan. Knowledge transfer to the support and operations teams is in the Knowledge Transfer Plan.

# Framework

![From impact to reinforcement](figures/ocm_framework.dot){width=14}

<!-- table: widths=3.6,4.4,8.6 caption="Change roles" size=8.5 -->
| Role | Who | What they do |
|---|---|---|
| Sponsor | BIBS Product Owner, with the Program Manager | Owns the case for change; opens the major communications; removes obstacles; chairs the change and training working group |
| Business owners | Product owner or head of each BRD | Own the impact, the learning paths and the KPIs of their units; release people for training and UAT |
| Change champions | One per unit and branch cluster (about 25) | Carry the messages; collect concerns; report readiness every month |
| Key users | Two per unit and persona group (about 40) | Test in UAT; coach colleagues; first help after go-live |
| BDOI trainers | One per unit and persona group (about 10 to 12) | Deliver end-user training after train-the-trainer |
| iorta change and training lead | iorta TechNXT | Designs the framework, material and assessments; trains key users and trainers; measures adoption |

# Stakeholder analysis

<!-- table: widths=3.8,1.8,1.6,1.6,7.8 caption="Stakeholders, influence and impact" size=8 -->
| Stakeholder group | Users | Influence | Impact | Engagement |
|---|---|---|---|---|
| Steering committee and heads of units | - | High | Medium | Monthly steering committee; readiness dashboard; go / no-go |
| Marketing (AO, Account Broker, TL, TH, UH) | 485 | High | High | Champions per segment; iteration reviews; learning path; dashboard briefings for TLs |
| TSU and MBS | 18 | Medium | High | Working group of BRD-03; key users in every review |
| Processing (Team Leaders, officers) | 123 | Medium | High | Key users in UAT; Renewal processing team readiness plan for January 2028 |
| Operations (Cashiering 16, Remittance 4, Production Reconciliation 4, Adjustment 6, Commission) | 35 | Medium | High | Working group of BRD-02; walkthroughs per team |
| Collections | 130 | Medium | High | Champions per section; worklist demonstrations in reviews |
| Comptrollership (FRBS 5, Disbursement 8, ACSL 6) | 19 | High | High | Head of Comptrollership in the CCB; month-end rehearsals in UAT |
| Claims | 37 | Medium | High | Working group of BRD-07; Motor and Non-Motor key users |
| Employee Benefits | 20 | Low | Medium | Working group of BRD-08 |
| Customer Servicing (Contact Center) | 24 concurrent | Medium | High | Case Management scope decision first; then the learning path |
| Compliance and investigators | 287 investigators; 8 approvers | High | Medium | Chief Compliance Officer; screening learning module for investigators |
| Business and System Administrators, Information Security | about 15 | High | High | Design authority; Knowledge Transfer Plan |
| BDOI IT and application support | about 12 | High | High | Knowledge Transfer Plan |
| Clients, insurers, bank channels | External | Medium | Medium | Letters and e-mails before go-live (T-21); new document formats and protected files |

User numbers are the user tables of the BRDs (BRD-01 p.210, BRD-02 annex, BRD-03 p.33, BRD-04 NFR, BRD-05 p.133-137, BRD-06 p.186-188, BRD-07, BRD-08, BRD-09 p.13-14, BRD-10); a person in two groups is counted in both.

# Change impact analysis

<!-- table: widths=3.4,8.4,1.6,3.2 caption="What changes per persona" size=8 -->
| Persona | What changes | Impact | Main readiness action |
|---|---|---|---|
| Marketing Account Officer | Clients onboarded with KYC and four-eyes verification; quotations rated and approved before sending; PRFs in a workflow; accounts with one ARN; renewal dispositions online instead of Excel; refund requests online | High | Learning path; practice on own clients in UAT data |
| Marketing Team Lead | Approvals in My Approvals instead of e-mail; renewal assignment and TL review in BIBS; dashboards for the team | High | Approver session; dashboard briefing |
| Processing Officer | Validation, CLPC matching, placement slips, e-policy extraction and booking in one workflow; booking posts invoice, GL entry and service invoice in one step | High | Learning path; booking practice with the journal preview |
| TSU Officer | Slips per insurer generated, protected and logged; insurer terms keyed once; comparative compiled by BIBS; package life cycle with ManCom sign-off | High | Negotiation walkthrough; maker-checker practice |
| Cashier | Receipts from controlled series; payment uploads matched; unapplied money in a workbench; requests from collectors in a queue | High | Learning path; payment uploads with the bank layouts |
| Collection Handler | One worklist from the invoice ledger; efforts, promises and escalations recorded; check pick-ups and application requests to Cashiering in-app | High | Learning path; worklist practice |
| Accountant and FRBS Officer | Unbalanced entries refused; accruals reverse automatically; broking books close by a scheduled job; bank files matched; ACSL corrections post on approval | High | Month-end rehearsal in UAT |
| Disbursement Processor | One intake of payment requests; vouchers and proforma entries created by BIBS; payees for every mode; end-of-day files | Medium | Learning path |
| Renewal Officer | Daily extraction and sanitation checks replace IT lists and pre-cleaning; insurer extracts protected; RA for every account in batch; acceptance flows to placement and booking without re-keying; January 2028 expiries from day 1 | High | Day-1 queue practice on Trial migration 4 and dress-rehearsal data |
| Claims Officer | Claims recorded on the cover without re-keying; premium check before the authorization code; loss advice e-mailed and logged; status, reserve and diary on the claim | High | Learning path |
| Business Administrator | Set-up through maker-checker screens instead of IT requests; group-profile requests with approvals | Medium | Configuration labs |
| System and Security Administrator | Implement approved changes; sign-in through EIAM; MFA and break-glass; parameters and separation of duties | Medium | Knowledge Transfer Plan |
| Auditor | Evidence from user access reports, audit logs and history tabs instead of manual lists | Low | Short session |

# Persona journey maps

The maps below compare today's way of working, taken from the current-process chapters of the BRDs, with the BIBS way of working of the FRS walkthroughs. The last column is what the person must learn; it drives the learning paths.

## Marketing Account Officer

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: Marketing Account Officer (BRD-01 p.133-134; FRS BRD-01 WT-A, WT-B)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Request | Requests by e-mail; HLS requests keyed in | Request inbox; HLS extract and bulk files uploaded with row checks | Quotation Requests; bulk upload |
| Onboarding | Client created without a standard KYC step | Prospect, KYC documents, verification by the Team Lead, confirmed client | New Client; KYC & Documents tab |
| Quotation | Prepared in QPS or Excel; approval by e-mail | Rated quotation, approved before it is sent, sent with a protected file | New Quotation; Send via Email; Record acceptance |
| Non-package | Risk sent to TSU by e-mail; comparative in Excel | PRF with TSU slips per insurer and a comparative compiled by BIBS | New Proposal Request; proposal slip |
| Account | Accounts in spreadsheets; duplicates found late | One ARN; completeness check; duplicate check at creation | Edit Account; Submit to Processing; Returned to Me |
| Renewal | Dispositions in Excel; hand-offs by e-mail | Dispositions online with a TL review and a notice at each step | My Dispositions; Disposition panel |

## Processing Officer

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: Processing Officer (BRD-01 p.133-134; FRS BRD-01 WT-A)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Validation | Accounts received by e-mail and tracked in spreadsheets | Account in My Work; Validate or Return To Marketing with a reason | Account record |
| Payment | CBG Fire billing and payment matching by hand | CLPC billing file produced and payment report matched | CLPC Billing; Payment Report |
| Placement | Placement files and hold covers by e-mail, no log | Slips generated, password protected and logged | Placement Workbench |
| Issuance | E-policies received by e-mail | E-policy uploaded, policy number extracted and confirmed; Insurance Advice generated | E-policy Upload; Extraction Review |
| Booking | Booking, GL entry and invoice are separate steps | One booking posts the invoice, the GL entry and the service invoice; batch at 20:00 | Pre-booking Confirmation; Booked Invoice |

## Cashier

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: Cashier (BRD-02 p.11; FRS BRD-02 WT-A, WT-B, WT-D)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Payment | Receipts issued by hand; payment details not visible | AR and OR from controlled series; payment matched at acceptance | Receive Payment; Receipt |
| Application | Application fragmented | Applied by component; unapplied money kept in a workbench | Unapplied Payments |
| Channels | Bank files handled outside the system | Payment uploads of the bank channels matched automatically | Payment Uploads |
| Requests | Pick-ups and 2307 by e-mail | Requests from collectors in Incoming Requests; Check Pick-up queue | Incoming Requests |
| Corrections | Cancellation by hand | Cancel Receipt with a reason, approved and posted by the Team Leader | Receipt actions |

## Collection Handler

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: Collection Handler (BRD-04 current process; FRS BRD-04 WT-A to WT-D)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Worklist | EBIX PR list and ISYS diary in two systems | One worklist from the invoice ledger, refreshed nightly, diary in view | PR Worklist |
| Follow-up | Efforts and commitments tracked by the handler | Efforts, promises and reminders on the collection item | Log Effort; Record Promise |
| Escalation | Credit term extension and escalation by e-mail | Escalation rules by ageing and broken promises; TL queue | Escalations |
| Disposition | Day-60 disposition and A / B / C tag by hand | Disposition from a list with category; history kept | Record Disposition |
| Hand-off | Pick-up requests and 2307 by e-mail | Requests to Cashiering placed from the item and tracked | Requests to Cashiering |

## Renewal Officer

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: Renewal Officer (BRD-06 current process; FRS BRD-06)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Extraction | IT extracts the expiring list | Daily extraction at the lead days; list for any range on demand | Expiry List |
| Sanitation | List pre-cleaned by hand | Checks classify each record Clean, Review or Exception | Checks & Bucket tab |
| Processing | Hand-offs by e-mail; no assignment | Staged workflow with assignment, review, return and post | Processing Worklist |
| Insurer | Accounts sent from Excel by e-mail | Extract per insurer sent protected; response file applied | Insurer Batches |
| Renewal Advice | IT text file, packaged accounts only | First and second notice for any account, in batch, protected | Letters |
| Booking | Accepted renewals re-keyed | Acceptance moves the account to placement and booking | Record Acceptance |

## Claims Officer

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: Claims Officer (BRD-07 current process; FRS BRD-07)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Notice | PLA or CRF prepared by hand from an e-mail or call | Claim recorded on the cover with the notice data | Record Claim |
| Record | Policy details looked up in EBIX and re-keyed | Cover, policy, version, locations, unit and branch read from the account | Cover Lookup |
| Premium check | Checked by hand; nothing stops an unpaid claim | Authorization code disabled while premium is unpaid | Generate Authorization Code |
| Loss advice | From a mailbox, no send log | Generated and e-mailed per insurer, logged; insurer claim numbers recorded | Send Loss Advice; Insurers & Updates |
| Handling | Status and follow-up in spreadsheets | Status, adjuster, reserve, follow-up and diary on the claim | Change Status; Diary |
| Closure | Informal closure | Temporary and permanent closure with a settlement type | Closure rules |

## Accountant and FRBS Officer

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: Accountant and FRBS Officer (BRD-05 p.44-45; FRS BRD-05 WT-A, WT-C, WT-D)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Entries | Some entries keyed by hand; unbalanced entries not prevented | Journal refused when debit and credit differ; accruals reverse automatically | New Journal; Journals |
| Inputs | Input reports depend on IT | Users run input reports themselves | Report Centre |
| Corrections | Corrections posted by a separate unit; cancellation gets a new invoice number | ACSL corrections post on approval; one invoice family | ACSL Case; Correction Entry |
| Reconciliation | Bank reconciliations by hand | Bank file matched on check number and amount | Bank Statements; Bank Reconciliation |
| Close | Broking books close with IT | Scheduled close; revaluation and service fee in BIBS | GL Close & Cut-Off; FX Revaluation |

## TSU Officer

<!-- table: widths=2.6,5,5.4,3.6 caption="Journey: TSU Officer (BRD-03 current process; FRS BRD-03 WT-A; FRS BRD-01 WT-B)" size=8 -->
| Stage | Today | In BIBS | What to learn |
|---|---|---|---|
| Request | Requests by e-mail with a TSU form | Package request or PRF logged, numbered and tracked by stage | TSU Workbench |
| Slips | Slips e-mailed, no log | Slips approved by a second officer, e-mailed per insurer, protected and logged | Quotation Slip; Negotiation |
| Terms | Replies in mailboxes | Terms keyed once per insurer; rounds recorded | Insurer Responses; Key In |
| Comparative | Compiled in Excel | One comparative master per package | Comparative tab |
| Sign-off | Manual sign-off and advisory | ManCom sign-off in BIBS; advisory sent on release | Requirements & Sign-off; Advisories |

# Communication plan

<!-- table: widths=2.4,5.2,3.2,2.6,3.2 caption="Communication plan" size=8 -->
| When | Message | Audience | Channel | Sender |
|---|---|---|---|---|
| Oct 2026 | Why BIBS, what changes, the drops and the January 2028 go-live | All BDOI staff | Town hall; e-mail; intranet page | BIBS Product Owner |
| Monthly from Nov 2026 | Programme news: what was signed, what is next, who is involved | All BDOI staff | Newsletter; champions' briefing | Change and training working group |
| Each iteration review | What the unit will see in BIBS | Key users and champions of the unit | Review session on SIT | Business owner |
| Jun 2027 | Training approach, calendar and nomination | Unit heads | E-mail; meeting | Business owners; Human Resources |
| Jul 2027 | UAT starts; who tests what | UAT testers and their heads | Kick-off session | BIBS Product Owner |
| Sep 2027 | Your learning path and your training dates | All users | E-mail from the unit head; calendar invite | Unit heads |
| Oct - Dec 2027 | "What changes for you" per persona; quick-reference guide | Users of each persona | Training session; intranet | Trainers |
| 13-Dec-2027 (T-21) | New documents, protected files and channels from January 2028 | Branches, insurers, bank channels | Letters and e-mails | Business owners; Head of Operations |
| 20-Dec-2027 | Cut-over calendar, legacy freeze on 31-Dec-2027, first-day instructions | All users | E-mail; champions | Program Manager |
| 3-Jan-2028 | BIBS is live: where to get help, floor support, hypercare contacts | All users | E-mail; banner; floor walkers | BIBS Product Owner |
| Weekly to Feb 2028, then monthly | Adoption KPIs, tips, fixes released | All users; heads | Newsletter; dashboard | Change and training working group |
| Mar 2028 | Legacy trackers retired; refresher sessions | All users | E-mail; sessions | Business owners |

# Training curriculum by persona

## Learning paths

Every user follows the common module, then the modules of the persona's learning path. A user with two personas follows both paths; modules shared by two paths are taken once. Sessions are hands-on on the training environment with seed data, at most 12 participants per trainer, in blocks of 3 to 4 hours.

<!-- table: widths=3.4,7.8,1.4,4 caption="Learning paths" size=8 -->
| Persona | Modules (after the common module C0: sign-in with BDO single sign-on and MFA, navigation, My Work, My Approvals, lists and filters, record pages and history, messages, Help Center; 2 hours) | Hours | Assessment |
|---|---|---|---|
| Marketing Account Officer | M1 Clients and KYC; M2 Quotations and acceptance; M3 Proposal requests; M4 Accounts and returns; M5 Renewal dispositions; M6 Packages and premium calculator; M7 Refund requests; M8 Collections view and reports | 12 | Scenario: client to submitted account; renewal disposition |
| Marketing Team Lead | Path of the Account Officer, then L1 Approvals and returns; L2 Renewal assignment and TL review; L3 Remittance holds and escalations; L4 Dashboards and production targets | +6 | Scenario: approvals and TL review |
| Processing Officer | P1 Validation and returns; P2 CLPC billing and payment report; P3 Placement and slips; P4 E-policy and Insurance Advice; P5 Booking, endorsements and service invoices; P6 Submitted policies | 12 | Scenario: account from validation to booked invoice |
| TSU Officer | T1 TSU Workbench and PRFs; T2 Quotation slips and insurer responses; T3 Package negotiation and comparative; T4 Requirements, ManCom and advisories; T5 Validation of versions (TL and Head) | 8 | Scenario: PRF to proposal slip |
| Cashier | K1 Invoice search and receipts; K2 Payment uploads and automatch; K3 Unapplied payments and refunds; K4 Incoming requests, pick-ups, PDCs; K5 Receipt cancellation and series; K6 Legacy invoices after go-live | 8 | Scenario: payment to applied receipt |
| Collection Handler | H1 Worklist and efforts; H2 Promises and escalations; H3 Installment plans and statements; H4 Unapplied payments and requests to Cashiering; H5 Reports and files | 6 | Scenario: account from worklist to pick-up |
| Accountant and FRBS Officer | F1 Journals and inquiry; F2 Month end: revaluation, close, service fee; F3 Bank reconciliation; F4 Reports and schedules; F5 ACSL reconciliation and corrections; F6 BIR forms and books | 12 | Month-end exercise |
| Disbursement Processor | D1 Workbench and vouchers; D2 Payees; D3 Encoding and uploads; D4 Instruments and end of day; D5 Reports and BIR 2307 | 8 | Scenario: refund request to posted voucher |
| Renewal Officer | Path of the Processing Officer (P1, P3 to P5), then R1 Expiry list and checks; R2 Uploads and assignment; R3 Processing and computations; R4 Insurer batches; R5 Letters and acceptance; R6 Go-live takeover and package remapping | 10 | Day-1 queue exercise |
| Claims Officer | CL1 Cover lookup and recording; CL2 Insurers, loss advice and authorization code; CL3 Status, reserve, settlement, closure; CL4 Diary, worklist and reports | 6 | Scenario: notice to temporary closure |
| Business Administrator | B1 Lists of values and templates; B2 Products, clauses, insurers and rates; B3 Renewal and collection rules; B4 Group-profile requests; B5 Maker-checker and audit trail | 8 | Configuration lab |
| System and Security Administrator | S1 Users, roles and requests to implement; S2 Parameters and separation of duties; S3 MFA, EIAM sign-in and break-glass; S4 Jobs, alerts and audit trail; then the Knowledge Transfer Plan | 8 | Administration lab |
| Auditor | A1 Read-only navigation and history tabs; A2 User access reports and audit logs; A3 Report Centre and exports | 3 | Evidence exercise |

The learning paths of Remittance, Adjustment, Production Reconciliation, Commission Receivables, Employee Benefits, Customer Servicing and Compliance follow the same structure; they are completed in the training needs analysis by 30-Apr-2027 from their FRS walkthroughs.

## Training needs analysis and material

- Training needs analysis per unit by 30-Apr-2027: named users per persona, second personas, branch location, availability windows.
- Material per learning path: trainer guide, participant exercises, data sheet of the seed records used, assessment, the quick-reference guide of the persona and the User Manual parts; ready for review by 27-Aug-2027 and final after the train-the-trainer.
- Material is written from the FRS walkthroughs and screen register; changed screens are flagged by the screen register and the material is updated with each release until the content freeze of 29-Oct-2027, after which changes go into a delta sheet per persona.

# Train-the-trainer

<!-- table: widths=3.4,13.2 caption="Train-the-trainer programme" size=8.5 -->
| Item | Content |
|---|---|
| Participants | BDOI trainers, one per unit and persona group (about 10 to 12), named by 30-Jul-2027; key users may join |
| Dates | 6 to 17-Sep-2027, on the training environment |
| Programme | Week 1: the learning paths of the trainer's personas at trainer depth; the end-to-end walkthroughs across BRDs; seed data. Week 2: adult-learning basics, session plan, delivery of two modules to peers with feedback, handling questions and the Help Center, logging feedback and incidents |
| Certification | Delivers one module observed by the iorta change and training lead and scores 80 percent or more on the trainer assessment of the learning path |
| Trainer kit | Trainer guide, slides, exercises, data sheets, assessment and answer keys, room and environment checklist, attendance form |
| Support | iorta trainers co-deliver the first session of each trainer; weekly trainer call during end-user training |

# Training delivery

<!-- table: widths=3.4,7.4,5.8 caption="Training calendar" size=8.5 -->
| Window | Training | Who |
|---|---|---|
| 19 to 30-Jul-2027 | Key user training (UAT testers and power users) | iorta change and training lead |
| 6 to 17-Sep-2027 | Train-the-trainer | iorta change and training lead |
| 18-Oct to 10-Dec-2027 | End-user training by persona, in waves by unit; the Renewal processing team first (complete by 3-Dec-2027 for the day-1 queue practice) | BDOI trainers with iorta support |
| 13 to 17-Dec-2027 | Catch-up sessions and assessment retakes | BDOI trainers |
| 3-Jan to 2-Feb-2028 | Floor walkers (key users and iorta staff) in head office and the main branches; daily drop-in clinic | Key users; iorta team |
| Feb to Jun 2028 | Refresher sessions on the topics with the most tickets or errors; new joiners monthly | BDOI trainers |

**Capacity.** About 1,344 users with an average path of 8 hours (two half-days) at 12 per class need about 224 half-day sessions. Ten trainers delivering three half-days a week over the 8 weeks of end-user training give 240 sessions.

**Completion target.** 95 percent of named users trained and assessed by 17-Dec-2027; access to production is granted with the training record (proposed rule, open point OCM-04).

# Contextual help and guided actions in the application

BIBS helps users at the moment of need:

<!-- table: widths=3.6,13 caption="Help inside BIBS" size=8.5 -->
| Help | What the user gets |
|---|---|
| Help Center | The help of every screen the user may open (screens refused to the user are not listed), with a summary, How it works and Controls; searchable; each topic opens its screen |
| Field help | Short format hints under fields (dd-MMM-yyyy, file limits) and info icons on labels for the rule of the field |
| Messages | Refused actions explained in plain words, with what to correct (for example the missing KYC documents) |
| Work queues | My Work and My Approvals list what is waiting for the user, with the next action; notices in the bell icon link to the record |
| Completeness checks | Records show what is missing before submission (for example the account completeness check) |
| Workflow panel and history | The stage of the record, the next step and who acted, with reasons and comments |
| Empty states | An empty list says so and offers the next action |

Proposed additions, through change control (open point OCM-05): a first sign-in tour per persona showing the home page, the sidebar, My Work and the Help Center; a "What's new" panel fed from the release notes per persona; and counters of Help Center use per screen to measure help usage.

# Usability testing plan

<!-- table: widths=3.4,13.2 caption="Usability tests" size=8.5 -->
| Item | Plan |
|---|---|
| Objective | Find what slows or confuses users before training and go-live; check that the main tasks of each persona can be done without help |
| Participants | Five users per persona group (eight groups: Marketing, Processing, TSU, Operations, Collections, Comptrollership, Renewal, Claims), not the key users who reviewed the screens |
| Tasks | The main tasks of the persona's quick-reference guide, with seed data prepared for each participant |
| Rounds | Round 1: 17 to 28-May-2027 on SIT (Drop 0 and Drop 1 screens). Round 2: 13 to 24-Sep-2027 on UAT (Drop 2 screens and the fixes of round 1). Round 3: 25-Oct to 5-Nov-2027 on the training environment (spot checks of changed screens) |
| Method | Moderated sessions of 60 minutes, think-aloud, screen recording with consent; one moderator and one note taker |
| Measures | Task success without help; time on task; errors and refused actions; single-question ease score per task (1 to 7); System Usability Scale (SUS) at the end |
| Targets | Task success 90 percent; SUS 75 or more; no task with an ease score below 5 |
| Findings | Rated critical, serious or minor; critical and serious findings become UX points in the screen register or change requests; report within 5 working days of each round |

# Adoption KPIs

The KPIs show whether BIBS is used as intended. They are measured from the BIBS audit trail and workflow history, the user access audit log (sign-ins), the service desk, and surveys. Baselines are taken in UAT and, where the legacy allows, from today's volumes in the BRDs.

<!-- table: widths=2.6,4.6,4,3,2.4 caption="Adoption KPIs" size=8 -->
| KPI | Definition | Target | Measurement | Owner |
|---|---|---|---|---|
| Active users | Named users who signed in during the week / named users with access | 90 percent by T+30; 95 percent by T+90 | User access audit log, weekly | BIBS Product Owner |
| Task completion in BIBS | Transactions of the process done in BIBS / all transactions of the process (for example renewal dispositions, receipts, claims recorded) | 100 percent of new business, receipts and renewals from T; no parallel tracker by T+60 | Workflow history; unit heads' confirmation of retired trackers | Business owners |
| Time to complete | Elapsed time of key flows: quotation request to quotation sent; account submitted to booked; payment received to receipt applied; claim notice to loss advice sent; renewal candidate to RA sent | Not above the UAT baseline by T+30; 20 percent below by T+90 | Workflow timestamps | Business owners |
| Error rate | Refused actions and returns for correction per 100 transactions (validation messages, returns to maker, insurer returns) | Below 8 by T+30; below 4 by T+90 | Message and return counts from the audit trail | Business owners |
| Help usage | Help Center opens per active user per week; how-to tickets per 100 users | Help opens fall by half from week 1 to week 6; how-to tickets below 5 per 100 users a week by T+30 | Help Center counters (proposed); service desk | iorta change and training lead |
| Support load | Tickets per 100 users a week; share solved by L1 and L2 | Below 10 by T+30; 80 percent solved by BDOI | Service desk | BDOI application support |
| Satisfaction | Survey score (1 to 5) on ease, speed and help; SUS | 3.8 at T+30; 4.0 at T+90; SUS 75 | Survey at T+30 and T+90 | BIBS Product Owner |
| Training completion | Users trained and assessed / named users | 95 percent by 17-Dec-2027 | Training records | Human Resources |

KPIs are reported daily in hypercare, weekly until T+90 and monthly after it, by unit and persona. A unit below target gets a coaching plan from its key users and, where the cause is the application, a UX point or a change request.

# Readiness and go-live support

- Change readiness survey of the champions monthly from June 2027 (awareness, understanding, ability, concerns), with a go-live readiness score per unit at the go / no-go: green when training completion is 95 percent or more, the key users are certified and the unit's open concerns have owners.
- Floor walkers and a drop-in clinic during the first four weeks; a daily adoption review in the hypercare triage.
- Legacy trackers (renewal Excel files, collection lists, SharePoint tracking) are retired by unit on go-live; the RA-sent tracker is loaded once into BIBS.

# Responsibilities

<!-- da:raci Training -->

# References

- BDOI BRDs and their current-process chapters (BRD-01 p.133-134; BRD-02 p.11; BRD-03; BRD-04; BRD-05 p.44-45; BRD-06; BRD-07) and user tables; umbrella BRD p.42 (1,344 named and 429 concurrent users).
- FRS of each BRD: personas, walkthroughs (WT-A onwards) and screen specifications; UX screen decks and screen registers.
- Business process deck: As-Is, envisioned and best practice, version 1.0.
- BDO UX guidelines and UX readiness checklist.
- Project Plan, Delivery Methodology, Knowledge Transfer Plan, User Manual Framework and quick-reference guides, version 1.0.

# Decisions and open points for BDOI

<!-- table: widths=1.4,8.4,3.2,2,1.6 caption="Decisions and open points" size=8.5 -->
| No. | Decision or open point (our proposal) | Owner | Needed by | Ref. |
|---|---|---|---|---|
| OCM-01 | Name the change champions (about 25) and key users (about 40) | Business owners | 31-Jan-2027 and 30-Jun-2027 | - |
| OCM-02 | Name the BDOI trainers (10 to 12) and release them for 6 to 17-Sep-2027 and the end-user weeks | Business owners; Human Resources | 30-Jul-2027 | DEP-22 |
| OCM-03 | Provide the training environment with training users from 19-Jul-2027 | BDOI IT | 19-Jul-2027 | DEP-21 |
| OCM-04 | Grant production access only with a completed training record | BIBS Product Owner; Information Security | 1-Oct-2027 | - |
| OCM-05 | Approve the proposed in-app additions (first sign-in tour, What's new panel, Help Center counters) through a change request | BIBS Product Owner; CCB | 31-Mar-2027 | - |
| OCM-06 | Confirm the adoption KPIs, their targets and the survey | BIBS Product Owner; business owners | 30-Jun-2027 | - |
| OCM-07 | Confirm the user counts per persona in the training needs analysis (the BRD tables give 485 Marketing, 123 Processing, 130 Collections, 37 Claims; Business Administrators, System Administrators and Auditors are our proposal) | Business owners | 30-Apr-2027 | - |
| OCM-08 | Decide the Customer Servicing Case Management scope so that the Contact Center learning path can be planned | Product Owner, CSF | 30-Oct-2026 | R-02 |
