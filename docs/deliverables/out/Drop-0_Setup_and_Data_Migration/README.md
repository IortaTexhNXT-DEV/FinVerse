# Drop 0 - Setup and Data Migration: deliverables index

BIBS client pack for BDO Insurance and Reinsurance Brokers (BDOI), grouped by BDOI drop (answer A5 of 26-Sep-2026).

| | |
|---|---|
| BDOI dates | Setup with the Drop 1 requirements (Sep - Nov 2026); migration requirements and mapping Sep - Nov 2026, development Nov 2026 - Mar 2027, SIT Apr - Jul 2027, UAT Aug - Oct 2027, full migration and cut-over Nov 2027 - Jan 2028 |
| Scope (BDOI drop plan) | Setup: 0.1 Accessibility and Login, 0.2 Authorization, 0.3 User Maintenance, 0.4 Data Management (GL accounts, reference tables), 0.5 Workflow, 0.6 Product Maintenance; the Data Migration stream; the Drop 0 integrations (EIAM, UIDM-ISC, LMS, HL-LOAS, PMS, CMS / New BOB, OBPCS, AFTS, Old BOB, ECM, CCM, M365, TFS). |
| Status | Status as of 26-Sep-2026; the documents are refreshed before UAT |

## Documents in this drop

One folder per BRD release set (`BRD-nn_<Name>/`): every file of the BRD (Start Here, guide deck, FRS, sign-off
workbook, test plan and summary; for BRD-13 the Data Migration Handbook and the Migration Workbook), released
and signed off together; in an
issued sign-off set the files carry the reading-order prefix 00_ to 05_ (deliverables README, "Release and
sign-off per BRD"). Each document is kept once, in its latest version.

The sets of BRD-03, BRD-11, BRD-13 also carry the UX screen documents for the BDOI UX Design team: 07 the UX Screen Deck (every screen persona by persona and flow by flow, with all its states), 08 the UX screen register (one row per screen image with the FRS section, the UXD status and the change flag) and 09 the image package (every screen image at twice the screen resolution, with the register as CSV). A change of the FRS names the screens it affects, so the UX Design team revises only those screens.

The drop-level set `Drop-0_Closure/` (v2.0) covers the whole drop: 01 Configuration inputs workbook (Excel); 02 Closure summary (Word). Its workbook lists every configuration input BDOI provides before go-live with its owner, due date and one route (screen, template or data migration object).

| Document | BRD | Kind | Version | File |
|---|---|---|---|---|
| Configuration Inputs | - | Configuration inputs workbook (Excel) | 2.0 | [`Drop-0_Closure/01_BIBS_Drop-0_Configuration_Inputs_v2.0.xlsx`](Drop-0_Closure/01_BIBS_Drop-0_Configuration_Inputs_v2.0.xlsx) |
| Closure Summary | - | Closure summary (Word) | 2.0 | [`Drop-0_Closure/02_BIBS_Drop-0_Closure_Summary_v2.0.docx`](Drop-0_Closure/02_BIBS_Drop-0_Closure_Summary_v2.0.docx) |
| Product Maintenance | BRD-03 | Start here guide | 2.0 | [`BRD-03_Product_Maintenance/00_BIBS_StartHere_BRD-03_Product_Maintenance_v2.0.docx`](BRD-03_Product_Maintenance/00_BIBS_StartHere_BRD-03_Product_Maintenance_v2.0.docx) |
| Product Maintenance | BRD-03 | Sign-off pack guide deck | 2.0 | [`BRD-03_Product_Maintenance/01_BIBS_GuideDeck_BRD-03_Product_Maintenance_v2.0.pptx`](BRD-03_Product_Maintenance/01_BIBS_GuideDeck_BRD-03_Product_Maintenance_v2.0.pptx) |
| Product Maintenance | BRD-03 | FRS | 2.0 | [`BRD-03_Product_Maintenance/02_BIBS_FRS_BRD-03_Product_Maintenance_v2.0.docx`](BRD-03_Product_Maintenance/02_BIBS_FRS_BRD-03_Product_Maintenance_v2.0.docx) |
| Product Maintenance | BRD-03 | Sign-off workbook (Excel) | 2.0 | [`BRD-03_Product_Maintenance/03_BIBS_Signoff_BRD-03_Product_Maintenance_v2.0.xlsx`](BRD-03_Product_Maintenance/03_BIBS_Signoff_BRD-03_Product_Maintenance_v2.0.xlsx) |
| Product Maintenance | BRD-03 | Test plan workbook (Excel) | 2.0 | [`BRD-03_Product_Maintenance/04_BIBS_TestPlan_BRD-03_Product_Maintenance_v2.0.xlsx`](BRD-03_Product_Maintenance/04_BIBS_TestPlan_BRD-03_Product_Maintenance_v2.0.xlsx) |
| Product Maintenance | BRD-03 | Test plan summary (Word) | 2.0 | [`BRD-03_Product_Maintenance/05_BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v2.0.docx`](BRD-03_Product_Maintenance/05_BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v2.0.docx) |
| Configuration Inputs | BRD-03 | Templates | 2.0 | [`BRD-03_Product_Maintenance/06_BIBS_Templates_BRD-03_Configuration_Inputs_v2.0.xlsx`](BRD-03_Product_Maintenance/06_BIBS_Templates_BRD-03_Configuration_Inputs_v2.0.xlsx) |
| Product Maintenance | BRD-03 | UX screen deck (PowerPoint) | 2.0 | [`BRD-03_Product_Maintenance/07_BIBS_UXDeck_BRD-03_Product_Maintenance_v2.0.pptx`](BRD-03_Product_Maintenance/07_BIBS_UXDeck_BRD-03_Product_Maintenance_v2.0.pptx) |
| Product Maintenance | BRD-03 | UX screen register (Excel) | 2.0 | [`BRD-03_Product_Maintenance/08_BIBS_UXScreens_BRD-03_Product_Maintenance_v2.0.xlsx`](BRD-03_Product_Maintenance/08_BIBS_UXScreens_BRD-03_Product_Maintenance_v2.0.xlsx) |
| Product Maintenance | BRD-03 | UX screen images, 2x PNG with the register as CSV (ZIP) | 2.0 | [`BRD-03_Product_Maintenance/09_BIBS_UXScreens_BRD-03_Product_Maintenance_v2.0.zip`](BRD-03_Product_Maintenance/09_BIBS_UXScreens_BRD-03_Product_Maintenance_v2.0.zip) |
| User Access Maintenance | BRD-11 | Start here guide | 2.0 | [`BRD-11_User_Access_Maintenance/00_BIBS_StartHere_BRD-11_User_Access_Maintenance_v2.0.docx`](BRD-11_User_Access_Maintenance/00_BIBS_StartHere_BRD-11_User_Access_Maintenance_v2.0.docx) |
| User Access Maintenance | BRD-11 | Sign-off pack guide deck | 2.0 | [`BRD-11_User_Access_Maintenance/01_BIBS_GuideDeck_BRD-11_User_Access_Maintenance_v2.0.pptx`](BRD-11_User_Access_Maintenance/01_BIBS_GuideDeck_BRD-11_User_Access_Maintenance_v2.0.pptx) |
| User Access Maintenance | BRD-11 | FRS | 2.0 | [`BRD-11_User_Access_Maintenance/02_BIBS_FRS_BRD-11_User_Access_Maintenance_v2.0.docx`](BRD-11_User_Access_Maintenance/02_BIBS_FRS_BRD-11_User_Access_Maintenance_v2.0.docx) |
| User Access Maintenance | BRD-11 | Sign-off workbook (Excel) | 2.0 | [`BRD-11_User_Access_Maintenance/03_BIBS_Signoff_BRD-11_User_Access_Maintenance_v2.0.xlsx`](BRD-11_User_Access_Maintenance/03_BIBS_Signoff_BRD-11_User_Access_Maintenance_v2.0.xlsx) |
| User Access Maintenance | BRD-11 | Test plan workbook (Excel) | 2.0 | [`BRD-11_User_Access_Maintenance/04_BIBS_TestPlan_BRD-11_User_Access_Maintenance_v2.0.xlsx`](BRD-11_User_Access_Maintenance/04_BIBS_TestPlan_BRD-11_User_Access_Maintenance_v2.0.xlsx) |
| User Access Maintenance | BRD-11 | Test plan summary (Word) | 2.0 | [`BRD-11_User_Access_Maintenance/05_BIBS_TestPlan_BRD-11_User_Access_Maintenance_Summary_v2.0.docx`](BRD-11_User_Access_Maintenance/05_BIBS_TestPlan_BRD-11_User_Access_Maintenance_Summary_v2.0.docx) |
| Configuration Inputs | BRD-11 | Templates | 2.0 | [`BRD-11_User_Access_Maintenance/06_BIBS_Templates_BRD-11_Configuration_Inputs_v2.0.xlsx`](BRD-11_User_Access_Maintenance/06_BIBS_Templates_BRD-11_Configuration_Inputs_v2.0.xlsx) |
| User Access Maintenance | BRD-11 | UX screen deck (PowerPoint) | 2.0 | [`BRD-11_User_Access_Maintenance/07_BIBS_UXDeck_BRD-11_User_Access_Maintenance_v2.0.pptx`](BRD-11_User_Access_Maintenance/07_BIBS_UXDeck_BRD-11_User_Access_Maintenance_v2.0.pptx) |
| User Access Maintenance | BRD-11 | UX screen register (Excel) | 2.0 | [`BRD-11_User_Access_Maintenance/08_BIBS_UXScreens_BRD-11_User_Access_Maintenance_v2.0.xlsx`](BRD-11_User_Access_Maintenance/08_BIBS_UXScreens_BRD-11_User_Access_Maintenance_v2.0.xlsx) |
| User Access Maintenance | BRD-11 | UX screen images, 2x PNG with the register as CSV (ZIP) | 2.0 | [`BRD-11_User_Access_Maintenance/09_BIBS_UXScreens_BRD-11_User_Access_Maintenance_v2.0.zip`](BRD-11_User_Access_Maintenance/09_BIBS_UXScreens_BRD-11_User_Access_Maintenance_v2.0.zip) |
| Data Migration | BRD-13 | Start here guide | 2.0 | [`BRD-13_Data_Migration/00_BIBS_StartHere_BRD-13_Data_Migration_v2.0.docx`](BRD-13_Data_Migration/00_BIBS_StartHere_BRD-13_Data_Migration_v2.0.docx) |
| Data Migration | BRD-13 | Sign-off pack guide deck | 2.0 | [`BRD-13_Data_Migration/01_BIBS_GuideDeck_BRD-13_Data_Migration_v2.0.pptx`](BRD-13_Data_Migration/01_BIBS_GuideDeck_BRD-13_Data_Migration_v2.0.pptx) |
| Data Migration | BRD-13 | Data Migration Handbook | 2.0 | [`BRD-13_Data_Migration/02_BIBS_Handbook_BRD-13_Data_Migration_v2.0.docx`](BRD-13_Data_Migration/02_BIBS_Handbook_BRD-13_Data_Migration_v2.0.docx) |
| Data Migration | BRD-13 | Migration Workbook (Excel) | 2.0 | [`BRD-13_Data_Migration/03_BIBS_Workbook_BRD-13_Data_Migration_v2.0.xlsx`](BRD-13_Data_Migration/03_BIBS_Workbook_BRD-13_Data_Migration_v2.0.xlsx) |
| Data Migration | BRD-13 | Test plan workbook (Excel) | 2.0 | [`BRD-13_Data_Migration/04_BIBS_TestPlan_BRD-13_Data_Migration_v2.0.xlsx`](BRD-13_Data_Migration/04_BIBS_TestPlan_BRD-13_Data_Migration_v2.0.xlsx) |
| Data Migration | BRD-13 | Test plan summary (Word) | 2.0 | [`BRD-13_Data_Migration/05_BIBS_TestPlan_BRD-13_Data_Migration_Summary_v2.0.docx`](BRD-13_Data_Migration/05_BIBS_TestPlan_BRD-13_Data_Migration_Summary_v2.0.docx) |
| Data Migration | BRD-13 | UX screen deck (PowerPoint) | 2.0 | [`BRD-13_Data_Migration/07_BIBS_UXDeck_BRD-13_Data_Migration_v2.0.pptx`](BRD-13_Data_Migration/07_BIBS_UXDeck_BRD-13_Data_Migration_v2.0.pptx) |
| Data Migration | BRD-13 | UX screen register (Excel) | 2.0 | [`BRD-13_Data_Migration/08_BIBS_UXScreens_BRD-13_Data_Migration_v2.0.xlsx`](BRD-13_Data_Migration/08_BIBS_UXScreens_BRD-13_Data_Migration_v2.0.xlsx) |
| Data Migration | BRD-13 | UX screen images, 2x PNG with the register as CSV (ZIP) | 2.0 | [`BRD-13_Data_Migration/09_BIBS_UXScreens_BRD-13_Data_Migration_v2.0.zip`](BRD-13_Data_Migration/09_BIBS_UXScreens_BRD-13_Data_Migration_v2.0.zip) |

## Who signs what

The sign-off sets hold business content only (screens, fields, list and template columns, validations, rules,
messages, notifications, documents, walkthroughs, reports); the technical content is in the Technical
Specification of each set, reviewed by BDOI IT. The signatories are the roles of the BRD approval sheet;
the matrix per part of the set is in the 00 Start Here and the 01 guide deck of each set.

| Set | Prepared by | Input provided by | Reviewed by | Approved by | Approval sheet |
|---|---|---|---|---|---|
| BRD-03 Product Maintenance | iorta TechNXT project team: Project Manager, Business Analysis, test lead<br>Business Analyst, Enterprise Services Group - Business Project Services (BPS) | Technical Support Unit (TSU), BU representatives<br>Marketing Business System (MBS), BU representatives | Program Manager, Enterprise Services Group - Business Project Services (BPS)<br>BDOI Information Technology Group (BDOI IT) | Product Owner, Marketing Business System<br>Unit Head - Claims, Analytics, Risk Management, Reinsurance and Technical Underwriting<br>Unit Head - Combank and Corbank<br>Head - Retail Marketing<br>Head - Corporate and Retail Marketing<br>Head - Comptrollership | BRD-3 approval sheets: main BRD p.34-35; Workshop Addendum p.8-9 |
| BRD-11 User Access Maintenance | iorta TechNXT project team: Project Manager, Business Analysis, test lead<br>Business Analyst, Enterprise Services - Business Process Services (BPS) | BU representative, Marketing Business System (MBS)<br>Business Administration (user access maintenance) | Program Manager, Enterprise Services - Business Process Services (BPS)<br>BDOI Information Security<br>BDOI Information Technology Group (BDOI IT) | Product Owner, Marketing Business System<br>Unit Head - Claims and Risk Management<br>Unit Head - Combank and Corbank Marketing / Corporate Processing<br>Head - Comptrollership<br>Head - Retail Marketing<br>Head - Corporate and Retail Marketing | BRD-11 sign-off sheet p.19 (Owner: "I own this document and approve all of its contents") |
| BRD-13 Data Migration | iorta TechNXT project team: Project Manager, Business Analysis, Migration Lead, test lead<br>Program Manager, Enterprise Services Group - Business Project Services (BPS) | Data Migration Lead<br>Data owners of the objects (department representatives named per object) | Marketing Business System (MBS)<br>BDOI Information Technology Group (BDOI IT) | Product Owner, Marketing Business System<br>Product Owner - Comptrollership - Disbursement<br>Product Owner - Comptrollership - FRBS and ACSL<br>Operations: Financial Transactions and Processing<br>Head - Operations<br>Unit Head - Analytics and Risk Management<br>Unit Head - Claims, Analytics, Risk Management, Reinsurance and Technical Underwriting<br>Unit Head - Combank and Corbank<br>Head - Comptrollership<br>Head - Retail Marketing<br>Head - Corporate and Retail Marketing | BRD-13 approval pages p.15-16 (draft V0.01, not yet signed) |

## Also part of this drop (documents kept in their primary drop)

A BRD that spans drops lives in the folder of its primary drop; nothing is copied.

| BRD | Part in this drop | Documents (in the primary drop folder) |
|---|---|---|
| BRD-01 | Client onboarding is also a migration object (clients C01-C03) | [`02_BIBS_FRS_BRD-01_New_Business_v2.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/02_BIBS_FRS_BRD-01_New_Business_v2.0.docx)<br>[`04_BIBS_TestPlan_BRD-01_New_Business_v2.0.xlsx`](../Drop-1_Transactional/BRD-01_New_Business/04_BIBS_TestPlan_BRD-01_New_Business_v2.0.xlsx)<br>[`05_BIBS_TestPlan_BRD-01_New_Business_Summary_v2.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/05_BIBS_TestPlan_BRD-01_New_Business_Summary_v2.0.docx) |
| BRD-05 | GL accounts and reference tables (item 0.4) | [`02_BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_v2.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/02_BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_v2.0.docx)<br>[`04_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_v2.0.xlsx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/04_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_v2.0.xlsx)<br>[`05_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Summary_v2.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/05_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Summary_v2.0.docx) |

## Still to write

| Document | BRD | Note |
|---|---|---|
| Bill of materials, technical and deployment architecture (items 4, 12) | BRD-00 | From the programme alignment pack (chapter 6), the IER and the architecture option decision |
| Security and data-protection controls mapping (item 26) | BRD-00 | EIAM, UIDM-ISC, S3 encryption, masking |
| Interface specifications of the Drop 0 integrations | - | After BDOI IT answers the IQ questions |
| Technical Specification of each Drop 0 sign-off set (BRD-03, BRD-11, BRD-13) | - | The technical content kept out of the business sets (interfaces, access set-up, data storage, extract transfer); reviewed by BDOI IT |
