---
# Source of the release note of the BRD-1 New Business business sign-off pack (release set v2.0).
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/signoff/RELEASE_NOTE_BRD01.md
title: New Business Release Note
subtitle: BRD-1 New Business business sign-off pack, release set v2.0
doc_type: Release Note
doc_code: ReleaseNote
brd: BRD-01
name: New Business
doc_id: BIBS-RN-BRD-01
version: "2.0"
date: 26 September 2026
status: Issued for BDOI business sign-off
header_title: Release Note BRD-1 New Business
output: ReleaseNote/BIBS_ReleaseNote_BRD-01_New_Business_v2.0.docx
h1_page_break: false
control:
  - version: "2.0"
    date: 26 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Release of the BRD-1 New Business business sign-off pack
distribution:
  - {name: "Product Owner, BDOI", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: Marketing Business Services and System Support (MBS), role: Business owner, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Retail, Commercial, Corporate and Institutional Marketing", role: Reviewer, organisation: BDOI, purpose: "Client, quotation, PRF and account screens"}
  - {name: Technical Support Unit (TSU), role: Reviewer, organisation: BDOI, purpose: "PRF, slips and insurer responses"}
  - {name: Processing, role: Reviewer, organisation: BDOI, purpose: "Payment, placement, issuance and booking screens"}
  - {name: Comptrollership, role: Reviewer, organisation: BDOI, purpose: "Booking entries and service invoices"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Review sessions, answers, change requests"}
---

# What this release contains

This release note accompanies the business sign-off pack of BRD-1 New Business, release set v2.0. The set shows the New Business functions of BIBS as built on 26-Sep-2026, screen by screen, so that each business unit can confirm them and sign them off. All documents are in the release-set folder BRD-01_New_Business of Drop 1.

<!-- table: widths=5.2,2,10.4 caption="Documents of the release set" -->
| Document | Version | Content |
|---|---|---|
| BIBS_FRS_BRD-01_New_Business_v2.0.docx | 2.0 | The functional requirements of v1.0 unchanged (chapters 1-11), plus navigation by persona and the screen flow (12), 46 screen specifications with screenshots, field and action tables, rules and outcome (13), three end-to-end walkthroughs (14), the messages catalogue (15), notifications (16), generated documents (17), upload templates (18), the cross-BRD interface contract (19), and sign-off and change control (20) |
| BIBS_Signoff_BRD-01_New_Business_v2.0.xlsx | 2.0 | The same content as rows for review: screen catalogue, field register, actions, business rules, messages, notifications, menu by persona, upload templates, cross-BRD contract and the sign-off sheet |
| BIBS_TestPlan_BRD-01_New_Business_v2.0.xlsx and Summary | 2.0 | The test cases of v1.0 re-traced to the screens, plus one screen case per screen and one message case per screen or dialog |
| This release note | 2.0 | How to review, the review sessions, the dates and change control |

FRS v1.0 and test plan v1.0 are replaced by this set. Every FR, BRD and test ID of v1.0 is kept, so comments already made on v1.0 still point to the same requirement.

# How to review

1. **Read the screen specifications** of your area in FRS chapter 13, or open the same screens on the SIT environment with your SIT user (table below). The numbered markers on each screenshot match the field table under it.
2. **Record your review in the sign-off workbook.** On the sheets Screen catalogue, Field register, Business rules and Messages, set BU review to Accept, Change requested or Comment for each row you review; write the change in BU comment; add your name and the date. Do not change the other columns.
3. **Check the menu of your users** on the sheet Menu by persona, and what New Business takes from and gives to your BRD on the sheet Cross-BRD contract.
4. **Return the workbook** to the iorta TechNXT project manager by the comment deadline below. One workbook per business unit is enough; several reviewers can share it.

<!-- table: widths=5,6.6,3,3 caption="Review areas and SIT users (seed data)" -->
| Business unit | Chapters and screens | Walkthrough | SIT user |
|---|---|---|---|
| Marketing (Retail, Commercial, Corporate, Institutional) | Clients, quotations, proposal requests, accounts (SCR-NB-01 to 17) | WT-A steps 1-9, WT-C | ao, mkttl |
| Technical Support Unit | Proposal requests, TSU Workbench (SCR-NB-09 to 12) | WT-B | tsu |
| Processing | Accounts, payment, placement, issuance, booking (SCR-NB-13 to 34) | WT-A steps 10-17, WT-C | proc, proctl, epol |
| Comptrollership | Booking, booked invoice, service invoices, booking setup (SCR-NB-28 to 34) | WT-A steps 16-17 | proc, badmin |
| MBS and Business Administration | Bulk uploads, reports, dashboard, lists of values, templates, retention (SCR-NB-35 to 46) | - | badmin, admin |

The screenshots and the SIT environment use fictitious seed data only. The SIT passwords are sent separately to the named reviewers.

# Review sessions

The project team walks through the screens on the SIT environment with each business unit. Each session follows the walkthroughs of FRS chapter 14 and answers questions on the spot; the invitation gives the room and the video-call details.

<!-- table: widths=3.4,2.4,7.4,4.4 caption="SIT walkthrough sessions" -->
| Date | Time | Session | Business units |
|---|---|---|---|
| Thu 1-Oct-2026 | 09:30-12:00 | Client onboarding, KYC and package quotation (WT-A steps 1-9) | Marketing, MBS |
| Fri 2-Oct-2026 | 09:30-12:00 | Non-package placement: PRF, quotation slip, insurer responses, proposal slip (WT-B) | TSU, Marketing |
| Mon 5-Oct-2026 | 09:30-12:30 | Account validation, payment, placement, issuance and booking; returns and messages (WT-A steps 10-17, WT-C) | Processing, Comptrollership |
| Tue 6-Oct-2026 | 09:30-11:30 | Bulk uploads, reports and dashboard, administration screens | MBS, Business Administration, all units |

# Timeline for comments and sign-off

<!-- table: widths=3.6,14 caption="Dates of the review" -->
| Date | Step |
|---|---|
| Mon 28-Sep-2026 | Release set issued to the business units |
| 1 to 6-Oct-2026 | SIT walkthrough sessions |
| Fri 16-Oct-2026 | Comment deadline: sign-off workbooks returned with the BU review columns filled in |
| Fri 23-Oct-2026 | Every Change requested row answered in the sign-off tracker: corrected in the set, or raised as a change request |
| Fri 30-Oct-2026 | Sign-off of release set v2.0 (FRS chapter 20 and the Sign-off sheet of the workbook) |

A correction agreed during the review is issued as v2.1 of the set before sign-off, with the changed rows marked.

# Change control after sign-off

Signing freezes the New Business screens, fields, navigation, actions, business rules, messages, notifications, documents, upload templates and the interface contract with the other BRDs as specified in this set. Configuration values marked "default" (SLA hours, thresholds, list entries, templates) are not frozen; the Business Administrator and the System Administrator change them in BIBS.

A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, the reason and the priority. The project team assesses the effect on the other BRDs through the cross-BRD contract of FRS chapter 19; the owners of every BRD concerned approve it; and it is delivered as a new version of the release set with its own release note. The set describes the system as built; the final as-built refresh at the end of the build re-issues it with the system as delivered.
