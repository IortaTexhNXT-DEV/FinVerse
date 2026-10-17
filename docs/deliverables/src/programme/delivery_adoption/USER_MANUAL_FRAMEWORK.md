---
# User Manual Framework and index of the persona quick-reference guides (BRD-00). Built by build_delivery_adoption.py.
title: User Manual Framework
subtitle: Structure, standards and upkeep of the BIBS user documentation, with the index of the persona quick-reference guides
doc_type: User Manual Framework
doc_code: Adoption
brd: BRD-00
name: User Manual Framework
doc_id: BIBS-UMF-BRD-00
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: BIBS User Manual Framework
h1_page_break: false
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT change and training lead
    reviewer: iorta TechNXT Business Analyst lead
    approver: BIBS Product Owner (pending)
    change: First issue, with 13 persona quick-reference guides
distribution:
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Approval of the documentation set"}
  - {name: "Business owners and key users", role: Reviewers, organisation: BDOI, purpose: "Review of the parts of their personas"}
  - {name: "BDOI trainers and application support", role: Users, organisation: BDOI, purpose: "Training and first-line help"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Writes and maintains the documentation"}
---

# Purpose, audience and scope

## Purpose

This framework fixes how the BIBS user documentation is organised, written, checked and kept current: the BIBS User Manual with one part per persona, the persona quick-reference guides, the release notes per persona and their link with the in-app Help Center and the training material. It also indexes the 13 quick-reference guides issued with it.

## Audience

The BIBS Product Owner (approval), the business owners and key users (review of their parts), the BDOI trainers and application support (use), and the iorta TechNXT writers.

## Scope

The documentation of the business users of BIBS. The operations and support documentation (runbook, deployment and configuration guides) is covered by the Knowledge Transfer Plan; the FRS and sign-off sets remain the specification.

# The documentation set

![The BIBS user documentation set](figures/um_structure.dot){width=15}

<!-- table: widths=3.6,6.4,3.2,3.4 caption="Documents of the set" size=8.5 -->
| Document | Content | Length | Ready |
|---|---|---|---|
| BIBS User Manual | Getting started; the broking cycle end to end; one part per persona; module reference; reports; messages and what to do; glossary | One volume per drop group | Drop 0 and Drop 1 parts draft 30-Jun-2027; all parts 30-Sep-2027; final 17-Dec-2027 |
| Quick-reference guides | One per persona: role, menus, main tasks step by step with screens and FRS references, tips, help | 2 to 6 pages | Version 1.0 with this framework; updated for UAT (30-Jul-2027) and go-live (17-Dec-2027) |
| Help Center | In-app help per screen: summary, How it works, Controls | Per screen | With each release |
| Release notes per persona | What changed for the persona in a release, with the screens | 1 page per persona | With each release from UAT |
| Training material | Learning paths, exercises and assessments (Change Management and Training Framework) | Per learning path | 27-Aug-2027 |

# Structure of the User Manual

<!-- table: widths=1.4,5,10.2 caption="Chapters of the User Manual" size=8.5 -->
| No. | Chapter | Content |
|---|---|---|
| 1 | About this manual | Purpose, how it is organised, conventions, versions |
| 2 | Getting started | Sign-in with BDO single sign-on and MFA; session warnings and automatic sign-out; locked account (the message "Invalid user name or password" and the unlock by the System Administrator); the screen layout, sidebar and home page; My Profile; My Work and My Approvals; notices; lists, filters, row action menus and exports; record pages, tabs and history; maker-checker; uploads with the Excel template or CSV or TXT files |
| 3 | The broking cycle end to end | Client to booked invoice, payment to insurer receipt, collection, renewal, claim, period end; who does each step (the walkthroughs of the FRS across BRDs) |
| 4 to 16 | One part per persona | The parts below, in the order of the quick-reference guides |
| 17 | Module reference | Every screen by menu group: purpose, who can open it, fields and actions in short; points to the screen specification of the FRS |
| 18 | Reports | Report Centre, report catalogue per module, parameters, exports, report archive |
| 19 | Messages and what to do | The messages of the screens, word for word, with the cause and the action |
| 20 | Glossary | Terms and abbreviations (ARN, PRF, KYC, RA, NRNS, DV, ACSL, FRBS, UPP and the others of the FRS glossaries) |

## Template of a persona part

Every persona part has the same sections:

1. **Role summary** - what the persona does in BIBS, the BRDs that govern it, the home page.
2. **Menus available** - the sidebar sections of the persona's roles.
3. **Daily and periodic tasks** - a table of tasks by frequency (daily, weekly, month end, year end) with the procedure that covers each.
4. **Procedures** - one procedure per task: when to use it, before you start, numbered steps (what you do, what you see), the result, and the screen ID and FRS walkthrough step or FR it follows.
5. **Approvals** - what the persona approves or returns, and the rule of four eyes.
6. **Reports** - the reports of the persona with their parameters.
7. **Messages and tips** - the messages the persona meets most, with what to do.

# Writing standards

<!-- table: widths=3.6,13 caption="Writing standards" size=8.5 -->
| Topic | Standard |
|---|---|
| Source | Only what the FRS walkthroughs, screen specifications and Help Center say; no screen, field, button or message that is not in them |
| Voice | Second person and active ("Click Submit for Review"); one action per step; the result after the step |
| Names | Screen names and button labels exactly as on the screen, in bold in the manual; screen IDs of the screen register (for example SCR-NB-06) |
| Data | Dates dd-MMM-yyyy; amounts formatted; record numbers in their pattern (QT-yyyy-nnnnnn); persons by name, never by login |
| Messages | Quoted word for word; a part in angle brackets (<ARN>) is filled by BIBS |
| Screenshots | Taken on the training environment with seed data; one per procedure where it helps; callouts numbered as in the FRS screen specification |
| Language | Plain English, short sentences; BRD and FRS terms of the glossary |
| Content rules | No internal references on what a user reads (no requirement IDs inside steps, no design notes); seed data only |

# Upkeep

- **Change trigger.** Every release lists the changed screens in the screen register (change flag). The writer updates the procedures and screenshots of those screens, the quick-reference guides and the release notes per persona in the same release.
- **Versions.** The User Manual and each quick-reference guide carry their version and date; x.0 at UAT and at go-live, x.1 for corrections. Only the current version is published.
- **Review.** The key users of the persona check the changed procedures on the UAT environment; the BIBS Product Owner approves the go-live version.
- **Content freeze.** For training, the content is frozen on 29-Oct-2027; later changes are issued as a delta sheet per persona until go-live.
- **Publication.** The Word masters are kept in the programme document store; PDFs are published on the BDOI intranet and linked from the Help Center.

# Personas and quick-reference guides

<!-- da:personas -->

Users per persona are the user tables of the BRDs where they exist; the others are our proposal, confirmed in the training needs analysis.

<!-- da:qrg_index -->

Each quick-reference guide names, for every step, the screen and the FRS walkthrough step or functional requirement it follows, so that the trainer and the key user can open the full walkthrough with its screenshots. The guides of Renewal, Claims and the other BRDs without a screen register yet name the screens as the FR does; they receive screen IDs when the sign-off sets of those BRDs are issued.

# References

- FRS of BRD-01 to BRD-13: personas, walkthroughs, screen specifications, messages; UX screen decks and registers.
- Umbrella FRS (BRD-00): personas and menu groups.
- BDO UX guidelines; Help Center content of BIBS.
- Change Management and Training Framework and Knowledge Transfer Plan, version 1.0.

# Decisions and open points for BDOI

<!-- table: widths=1.4,8.4,3.2,2,1.6 caption="Decisions and open points" size=8.5 -->
| No. | Decision or open point (our proposal) | Owner | Needed by | Ref. |
|---|---|---|---|---|
| UM-01 | Approve the documentation set and the User Manual structure | BIBS Product Owner | 30-Nov-2026 | - |
| UM-02 | Confirm the 13 main personas and add any persona that needs its own guide (for example Remittance Processor, Adjustment Processor, Contact Center staff, Compliance Officer) | Business owners | 31-Jan-2027 | - |
| UM-03 | Name the key users who check each persona part on UAT | Business owners | 30-Jun-2027 | - |
| UM-04 | Confirm the BDOI intranet location for the published PDFs and the link from the Help Center | BDOI IT | 30-Sep-2027 | - |
