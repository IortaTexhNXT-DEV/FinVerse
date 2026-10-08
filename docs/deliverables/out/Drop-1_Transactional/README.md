# Drop 1 - Transactional (upstream and downstream): deliverables index

BIBS client pack for BDO Insurance and Reinsurance Brokers (BDOI), grouped by BDOI drop (answer A5 of 26-Sep-2026).

| | |
|---|---|
| BDOI dates | Requirements Sep - Nov 2026, development Nov 2026 - Feb 2027, SIT Jan - Jul 2027, UAT (end to end) Aug - Dec 2027 |
| Scope (BDOI drop plan) | Upstream (product inherent): 1.U1 Client Onboarding, 1.U2 Quotation or Proposal, 1.U3 Account Creation and Maintenance, 1.U4 Submitted Policy, 1.U5 Renewal, 1.U6 Placement and ePolicy, 1.U7 Booking, 1.U8 Accounting / GL, 1.U9 Reports. Downstream (product agnostic): 1.D1 Disbursement, 1.D2 Cashiering, 1.D3 Remittance (with the reinsurance transactions), 1.D4 Adjustment / Cancellation, 1.D5 Accounting / GL, 1.D6 Reports (BIR / regulatory), 1.D7 Collection of Commission Receivables (Direct Payment). |
| Status | Status as of 26-Sep-2026; the documents are refreshed before UAT |

## Documents in this drop

One folder per BRD release set (`BRD-nn_<Name>/`): every file of the BRD (Start Here, guide deck, FRS, sign-off
workbook, test plan and summary; for BRD-13 the Data Migration Handbook and the Migration Workbook), released
and signed off together; in an
issued sign-off set the files carry the reading-order prefix 00_ to 05_ (deliverables README, "Release and
sign-off per BRD"). Each document is kept once, in its latest version.

The sets of BRD-01, BRD-02, BRD-04, BRD-05 also carry the UX screen documents for the BDOI UX Design team: 07 the UX Screen Deck (every screen persona by persona and flow by flow, with all its states), 08 the UX screen register (one row per screen image with the FRS section, the UXD status and the change flag) and 09 the image package (every screen image at twice the screen resolution, with the register as CSV). A change of the FRS names the screens it affects, so the UX Design team revises only those screens.

| Document | BRD | Kind | Version | File |
|---|---|---|---|---|
| New Business | BRD-01 | Start here guide | 2.1 | [`BRD-01_New_Business/00_BIBS_StartHere_BRD-01_New_Business_v2.1.docx`](BRD-01_New_Business/00_BIBS_StartHere_BRD-01_New_Business_v2.1.docx) |
| New Business | BRD-01 | Sign-off pack guide deck | 2.1 | [`BRD-01_New_Business/01_BIBS_GuideDeck_BRD-01_New_Business_v2.1.pptx`](BRD-01_New_Business/01_BIBS_GuideDeck_BRD-01_New_Business_v2.1.pptx) |
| New Business | BRD-01 | FRS | 2.1 | [`BRD-01_New_Business/02_BIBS_FRS_BRD-01_New_Business_v2.1.docx`](BRD-01_New_Business/02_BIBS_FRS_BRD-01_New_Business_v2.1.docx) |
| New Business | BRD-01 | Sign-off workbook (Excel) | 2.1 | [`BRD-01_New_Business/03_BIBS_Signoff_BRD-01_New_Business_v2.1.xlsx`](BRD-01_New_Business/03_BIBS_Signoff_BRD-01_New_Business_v2.1.xlsx) |
| New Business | BRD-01 | Test plan workbook (Excel) | 2.1 | [`BRD-01_New_Business/04_BIBS_TestPlan_BRD-01_New_Business_v2.1.xlsx`](BRD-01_New_Business/04_BIBS_TestPlan_BRD-01_New_Business_v2.1.xlsx) |
| New Business | BRD-01 | Test plan summary (Word) | 2.1 | [`BRD-01_New_Business/05_BIBS_TestPlan_BRD-01_New_Business_Summary_v2.1.docx`](BRD-01_New_Business/05_BIBS_TestPlan_BRD-01_New_Business_Summary_v2.1.docx) |
| New Business | BRD-01 | UX screen deck (PowerPoint) | 2.1 | [`BRD-01_New_Business/07_BIBS_UXDeck_BRD-01_New_Business_v2.1.pptx`](BRD-01_New_Business/07_BIBS_UXDeck_BRD-01_New_Business_v2.1.pptx) |
| New Business | BRD-01 | UX screen register (Excel) | 2.1 | [`BRD-01_New_Business/08_BIBS_UXScreens_BRD-01_New_Business_v2.1.xlsx`](BRD-01_New_Business/08_BIBS_UXScreens_BRD-01_New_Business_v2.1.xlsx) |
| New Business | BRD-01 | UX screen images, 2x PNG with the register as CSV (ZIP) | 2.1 | [`BRD-01_New_Business/09_BIBS_UXScreens_BRD-01_New_Business_v2.1.zip`](BRD-01_New_Business/09_BIBS_UXScreens_BRD-01_New_Business_v2.1.zip) |
| Operations | BRD-02 | Start here guide | 2.1 | [`BRD-02_Operations/00_BIBS_StartHere_BRD-02_Operations_v2.1.docx`](BRD-02_Operations/00_BIBS_StartHere_BRD-02_Operations_v2.1.docx) |
| Operations | BRD-02 | Sign-off pack guide deck | 2.1 | [`BRD-02_Operations/01_BIBS_GuideDeck_BRD-02_Operations_v2.1.pptx`](BRD-02_Operations/01_BIBS_GuideDeck_BRD-02_Operations_v2.1.pptx) |
| Operations | BRD-02 | FRS | 2.1 | [`BRD-02_Operations/02_BIBS_FRS_BRD-02_Operations_v2.1.docx`](BRD-02_Operations/02_BIBS_FRS_BRD-02_Operations_v2.1.docx) |
| Operations | BRD-02 | Sign-off workbook (Excel) | 2.1 | [`BRD-02_Operations/03_BIBS_Signoff_BRD-02_Operations_v2.1.xlsx`](BRD-02_Operations/03_BIBS_Signoff_BRD-02_Operations_v2.1.xlsx) |
| Operations | BRD-02 | Test plan workbook (Excel) | 2.1 | [`BRD-02_Operations/04_BIBS_TestPlan_BRD-02_Operations_v2.1.xlsx`](BRD-02_Operations/04_BIBS_TestPlan_BRD-02_Operations_v2.1.xlsx) |
| Operations | BRD-02 | Test plan summary (Word) | 2.1 | [`BRD-02_Operations/05_BIBS_TestPlan_BRD-02_Operations_Summary_v2.1.docx`](BRD-02_Operations/05_BIBS_TestPlan_BRD-02_Operations_Summary_v2.1.docx) |
| Operations | BRD-02 | UX screen deck (PowerPoint) | 2.1 | [`BRD-02_Operations/07_BIBS_UXDeck_BRD-02_Operations_v2.1.pptx`](BRD-02_Operations/07_BIBS_UXDeck_BRD-02_Operations_v2.1.pptx) |
| Operations | BRD-02 | UX screen register (Excel) | 2.1 | [`BRD-02_Operations/08_BIBS_UXScreens_BRD-02_Operations_v2.1.xlsx`](BRD-02_Operations/08_BIBS_UXScreens_BRD-02_Operations_v2.1.xlsx) |
| Operations | BRD-02 | UX screen images, 2x PNG with the register as CSV (ZIP) | 2.1 | [`BRD-02_Operations/09_BIBS_UXScreens_BRD-02_Operations_v2.1.zip`](BRD-02_Operations/09_BIBS_UXScreens_BRD-02_Operations_v2.1.zip) |
| Collections | BRD-04 | Start here guide | 2.1 | [`BRD-04_Collections/00_BIBS_StartHere_BRD-04_Collections_v2.1.docx`](BRD-04_Collections/00_BIBS_StartHere_BRD-04_Collections_v2.1.docx) |
| Collections | BRD-04 | Sign-off pack guide deck | 2.1 | [`BRD-04_Collections/01_BIBS_GuideDeck_BRD-04_Collections_v2.1.pptx`](BRD-04_Collections/01_BIBS_GuideDeck_BRD-04_Collections_v2.1.pptx) |
| Collections | BRD-04 | FRS | 2.1 | [`BRD-04_Collections/02_BIBS_FRS_BRD-04_Collections_v2.1.docx`](BRD-04_Collections/02_BIBS_FRS_BRD-04_Collections_v2.1.docx) |
| Collections | BRD-04 | Sign-off workbook (Excel) | 2.1 | [`BRD-04_Collections/03_BIBS_Signoff_BRD-04_Collections_v2.1.xlsx`](BRD-04_Collections/03_BIBS_Signoff_BRD-04_Collections_v2.1.xlsx) |
| Collections | BRD-04 | Test plan workbook (Excel) | 2.1 | [`BRD-04_Collections/04_BIBS_TestPlan_BRD-04_Collections_v2.1.xlsx`](BRD-04_Collections/04_BIBS_TestPlan_BRD-04_Collections_v2.1.xlsx) |
| Collections | BRD-04 | Test plan summary (Word) | 2.1 | [`BRD-04_Collections/05_BIBS_TestPlan_BRD-04_Collections_Summary_v2.1.docx`](BRD-04_Collections/05_BIBS_TestPlan_BRD-04_Collections_Summary_v2.1.docx) |
| Collections | BRD-04 | UX screen deck (PowerPoint) | 2.1 | [`BRD-04_Collections/07_BIBS_UXDeck_BRD-04_Collections_v2.1.pptx`](BRD-04_Collections/07_BIBS_UXDeck_BRD-04_Collections_v2.1.pptx) |
| Collections | BRD-04 | UX screen register (Excel) | 2.1 | [`BRD-04_Collections/08_BIBS_UXScreens_BRD-04_Collections_v2.1.xlsx`](BRD-04_Collections/08_BIBS_UXScreens_BRD-04_Collections_v2.1.xlsx) |
| Collections | BRD-04 | UX screen images, 2x PNG with the register as CSV (ZIP) | 2.1 | [`BRD-04_Collections/09_BIBS_UXScreens_BRD-04_Collections_v2.1.zip`](BRD-04_Collections/09_BIBS_UXScreens_BRD-04_Collections_v2.1.zip) |
| Accounting Disbursement ACSL | BRD-05 | Start here guide | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/00_BIBS_StartHere_BRD-05_Accounting_Disbursement_ACSL_v2.1.docx`](BRD-05_Accounting_Disbursement_ACSL/00_BIBS_StartHere_BRD-05_Accounting_Disbursement_ACSL_v2.1.docx) |
| Accounting Disbursement ACSL | BRD-05 | Sign-off pack guide deck | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/01_BIBS_GuideDeck_BRD-05_Accounting_Disbursement_ACSL_v2.1.pptx`](BRD-05_Accounting_Disbursement_ACSL/01_BIBS_GuideDeck_BRD-05_Accounting_Disbursement_ACSL_v2.1.pptx) |
| Accounting Disbursement ACSL | BRD-05 | FRS | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/02_BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_v2.1.docx`](BRD-05_Accounting_Disbursement_ACSL/02_BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_v2.1.docx) |
| Accounting Disbursement ACSL | BRD-05 | Sign-off workbook (Excel) | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/03_BIBS_Signoff_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx`](BRD-05_Accounting_Disbursement_ACSL/03_BIBS_Signoff_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx) |
| Accounting Disbursement ACSL | BRD-05 | Test plan workbook (Excel) | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/04_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx`](BRD-05_Accounting_Disbursement_ACSL/04_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx) |
| Accounting Disbursement ACSL | BRD-05 | Test plan summary (Word) | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/05_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Summary_v2.1.docx`](BRD-05_Accounting_Disbursement_ACSL/05_BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Summary_v2.1.docx) |
| Accounting Disbursement ACSL | BRD-05 | UX screen deck (PowerPoint) | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/07_BIBS_UXDeck_BRD-05_Accounting_Disbursement_ACSL_v2.1.pptx`](BRD-05_Accounting_Disbursement_ACSL/07_BIBS_UXDeck_BRD-05_Accounting_Disbursement_ACSL_v2.1.pptx) |
| Accounting Disbursement ACSL | BRD-05 | UX screen register (Excel) | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/08_BIBS_UXScreens_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx`](BRD-05_Accounting_Disbursement_ACSL/08_BIBS_UXScreens_BRD-05_Accounting_Disbursement_ACSL_v2.1.xlsx) |
| Accounting Disbursement ACSL | BRD-05 | UX screen images, 2x PNG with the register as CSV (ZIP) | 2.1 | [`BRD-05_Accounting_Disbursement_ACSL/09_BIBS_UXScreens_BRD-05_Accounting_Disbursement_ACSL_v2.1.zip`](BRD-05_Accounting_Disbursement_ACSL/09_BIBS_UXScreens_BRD-05_Accounting_Disbursement_ACSL_v2.1.zip) |
| Renewal | BRD-06 | FRS | 1.1 | [`BRD-06_Renewal/BIBS_FRS_BRD-06_Renewal_v1.1.docx`](BRD-06_Renewal/BIBS_FRS_BRD-06_Renewal_v1.1.docx) |
| Renewal | BRD-06 | Test plan summary (Word) | 1.1 | [`BRD-06_Renewal/BIBS_TestPlan_BRD-06_Renewal_Summary_v1.1.docx`](BRD-06_Renewal/BIBS_TestPlan_BRD-06_Renewal_Summary_v1.1.docx) |
| Renewal | BRD-06 | Test plan workbook (Excel) | 1.1 | [`BRD-06_Renewal/BIBS_TestPlan_BRD-06_Renewal_v1.1.xlsx`](BRD-06_Renewal/BIBS_TestPlan_BRD-06_Renewal_v1.1.xlsx) |
| Customer Servicing Facility | BRD-09 | FRS | 1.1 | [`BRD-09_Customer_Servicing_Facility/BIBS_FRS_BRD-09_Customer_Servicing_Facility_v1.1.docx`](BRD-09_Customer_Servicing_Facility/BIBS_FRS_BRD-09_Customer_Servicing_Facility_v1.1.docx) |
| Customer Servicing Facility | BRD-09 | Test plan summary (Word) | 1.1 | [`BRD-09_Customer_Servicing_Facility/BIBS_TestPlan_BRD-09_Customer_Servicing_Facility_Summary_v1.1.docx`](BRD-09_Customer_Servicing_Facility/BIBS_TestPlan_BRD-09_Customer_Servicing_Facility_Summary_v1.1.docx) |
| Customer Servicing Facility | BRD-09 | Test plan workbook (Excel) | 1.1 | [`BRD-09_Customer_Servicing_Facility/BIBS_TestPlan_BRD-09_Customer_Servicing_Facility_v1.1.xlsx`](BRD-09_Customer_Servicing_Facility/BIBS_TestPlan_BRD-09_Customer_Servicing_Facility_v1.1.xlsx) |
| Sanction Screening | BRD-10 | FRS | 1.1 | [`BRD-10_Sanction_Screening/BIBS_FRS_BRD-10_Sanction_Screening_v1.1.docx`](BRD-10_Sanction_Screening/BIBS_FRS_BRD-10_Sanction_Screening_v1.1.docx) |
| Sanction Screening | BRD-10 | Test plan summary (Word) | 1.1 | [`BRD-10_Sanction_Screening/BIBS_TestPlan_BRD-10_Sanction_Screening_Summary_v1.1.docx`](BRD-10_Sanction_Screening/BIBS_TestPlan_BRD-10_Sanction_Screening_Summary_v1.1.docx) |
| Sanction Screening | BRD-10 | Test plan workbook (Excel) | 1.1 | [`BRD-10_Sanction_Screening/BIBS_TestPlan_BRD-10_Sanction_Screening_v1.1.xlsx`](BRD-10_Sanction_Screening/BIBS_TestPlan_BRD-10_Sanction_Screening_v1.1.xlsx) |
| Submitted Policies | BRD-12 | FRS | 1.1 | [`BRD-12_Submitted_Policies/BIBS_FRS_BRD-12_Submitted_Policies_v1.1.docx`](BRD-12_Submitted_Policies/BIBS_FRS_BRD-12_Submitted_Policies_v1.1.docx) |
| Submitted Policies | BRD-12 | Test plan summary (Word) | 1.1 | [`BRD-12_Submitted_Policies/BIBS_TestPlan_BRD-12_Submitted_Policies_Summary_v1.1.docx`](BRD-12_Submitted_Policies/BIBS_TestPlan_BRD-12_Submitted_Policies_Summary_v1.1.docx) |
| Submitted Policies | BRD-12 | Test plan workbook (Excel) | 1.1 | [`BRD-12_Submitted_Policies/BIBS_TestPlan_BRD-12_Submitted_Policies_v1.1.xlsx`](BRD-12_Submitted_Policies/BIBS_TestPlan_BRD-12_Submitted_Policies_v1.1.xlsx) |

## Who signs what

The sign-off sets hold business content only (screens, fields, list and template columns, validations, rules,
messages, notifications, documents, walkthroughs, reports); the technical content is in the Technical
Specification of each set, reviewed by BDOI IT. The signatories are the roles of the BRD approval sheet;
the matrix per part of the set is in the 00 Start Here and the 01 guide deck of each set.

| Set | Prepared by | Input provided by | Reviewed by | Approved by | Approval sheet |
|---|---|---|---|---|---|
| BRD-01 New Business | iorta TechNXT project team: Project Manager, Business Analysis, test lead<br>Business Analyst, Enterprise Services Group - Business Project Services (BPS) | Marketing Business Services and System Support (MBS), BU representatives<br>Technical Support Unit (TSU), BU representatives | Program Manager, Enterprise Services Group - Business Project Services (BPS)<br>BA Unit Head, Enterprise Services Group - Business Project Services (BPS)<br>BDOI Information Technology Group (BDOI IT)<br>Compliance Officer | Product Owner, Marketing Business System<br>Unit Head - Processing<br>Unit Head - Combank and Corbank<br>Head - Retail Marketing<br>Head of Institutional Banking, SM and BDO Accounts<br>Head - Comptrollership<br>Officer-in-Charge (OIC), BDO Insurance and Reinsurance Brokers | BRD-1 approval sheets (New Business BRD 05132026): IT Walkthrough Addendum pp.8-10; Fire and Motor BRD ID consolidation p.138-139; Workshop Addendum p.32 (copy p.54) |
| BRD-02 Operations | iorta TechNXT project team: Project Manager, Business Analysis, test lead<br>Business Analyst, Enterprise Services Group - Business Project Services (BPS) (Zean C. Ibay) | Operations: Financial Transactions and Processing, BU representatives (Shirley Catapang, Perjelyn Joy Gutierrez) | Marketing Head Office, Admin and Collections; BBG (Angel Lou R. Kabigting)<br>Comptrollership (Rodrigo R. Dela Cruz)<br>BDOI Information Technology Group (BDOI IT) | Collections and Marketing Support (Pia Grace M. Pinili)<br>Program Manager, Enterprise Services Group - Business Project Services (BPS) (Dan Ace Cauton)<br>Operations: Financial Transactions and Processing (Jose Melvin M. Jarin)<br>Product Owner, AVP (Shellah Marie C. Miranda) | Operations BRD v1.01 approval sheet p.184-185 of the file Operations_WS Addendum (Prepared by, Reviewed by, Approved by), with the input providers of the original sheet |
| BRD-04 Collections | iorta TechNXT project team: Project Manager, Business Analysis, test lead<br>Business Analyst, Enterprise Services Group - Business Project Services (BPS) (Zean C. Ibay) | Collections and Marketing Support, HO Marketing Support BBG, Operations, Marketing and Business Services, Marketing - Head Office, Marketing Business Services and System Support (Pia Grace M. Pinili, Maria Victoria Evangelista, Grace Tordesillas, Ma. Fides E. Rivera, Arra R. Rivera, Rhommel Mark Galler)<br>Operations: Financial Transactions and Processing (Perjelyn Joy Gutierrez) | VP and Head, BDOI Operations (John Benedict S. Santos)<br>BDOI Information Technology Group: IT Core Business Delivery (Reynaldo Valera Jr.) and IT AIO Core Business Delivery - Insurance (Wynonah Allyssa D. Agojo) | Program Manager, Enterprise Services Group - Business Project Services (BPS) (Dan Ace Cauton)<br>Product Owner, AVP (Shellah Marie C. Miranda)<br>Product Owner, AVP (Jose Melvin Jarin)<br>Unit Head - Combank and Corbank (Mark Joseph C. Makalintal) and Heads of Retail Marketing (Edmundante F. Ramirez, Roderick Lim)<br>Head - Comptrollership, VP (Ronald Allan E. De Leon) | Collections BRD approval sheets: CMS BRD p.95-96 (Prepared, Reviewed, Validated, Approved by), renumbering addendum p.36 and Collections Addendum p.24 |
| BRD-05 Accounting Disbursement ACSL | iorta TechNXT project team: Project Manager, Business Analysis, test lead<br>Business Analysts, Enterprise Services Group - Business Project Services (BPS) (Roda Lyn D. Gallardo; Zean C. Ibay and Jane Tagle for the Workshop Addendum) | Marketing Business Services and System Support (MBS Team) (Ma. Fides Rivera; Ruby Evangelista and Darryl Castillo for the Workshop Addendum) | Program Manager, Enterprise Services Group - Business Project Services (BPS) (Dan Ace R. Cauton)<br>Product Owners - Comptrollership - ACSL (Freddie A. Atanque, Jennifer T. Lugtu)<br>BDOI Information Technology Group (Carmela A. Morfe, Yonille D. Guy, Abegail R. Panghulan, Nelda R. Ranchez)<br>Marketing Business Services and System Support (Rhommel Mark Galler) | Product Owner - Comptrollership - Disbursement (Rodrigo R. Dela Cruz)<br>Product Owner - Comptrollership - FRBS and ACSL (Iris S. Marquez)<br>Product Owner - Comptrollership and Operations (Perjelyn Joy R. Gutierrez)<br>Product Owner - Marketing, Processing and Collections, AVP (Shellah Marie C. Miranda)<br>Head - Comptrollership, VP (Ronald Allan E. De Leon)<br>Head - Institutional Banking, SM and BDO Accounts, FVP (Roderick L. Lim; Head - Retail Marketing on the Workshop Addendum) | BRD-5 approval sheets: main BRD p.141-142 (Prepared, Input Provided, Reviewed, Approved by), Addendum 1 p.39-40 and the Workshop Addendum p.15-16 |

## Also part of this drop (documents kept in their primary drop)

A BRD that spans drops lives in the folder of its primary drop; nothing is copied.

| BRD | Part in this drop | Documents (in the primary drop folder) |
|---|---|---|
| BRD-03 | Quotation or proposal with packages (item 1.U2) | [`02_BIBS_FRS_BRD-03_Product_Maintenance_v2.1.docx`](../Drop-0_Setup_and_Data_Migration/BRD-03_Product_Maintenance/02_BIBS_FRS_BRD-03_Product_Maintenance_v2.1.docx)<br>[`04_BIBS_TestPlan_BRD-03_Product_Maintenance_v2.1.xlsx`](../Drop-0_Setup_and_Data_Migration/BRD-03_Product_Maintenance/04_BIBS_TestPlan_BRD-03_Product_Maintenance_v2.1.xlsx)<br>[`05_BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v2.1.docx`](../Drop-0_Setup_and_Data_Migration/BRD-03_Product_Maintenance/05_BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v2.1.docx) |
| BRD-08 | EB placement and ePolicy, EB upstream reports (items 1.U6, 1.U9) | [`BIBS_FRS_BRD-08_Employee_Benefits_v1.1.docx`](../Drop-2_Independent/BRD-08_Employee_Benefits/BIBS_FRS_BRD-08_Employee_Benefits_v1.1.docx)<br>[`BIBS_TestPlan_BRD-08_Employee_Benefits_Summary_v1.1.docx`](../Drop-2_Independent/BRD-08_Employee_Benefits/BIBS_TestPlan_BRD-08_Employee_Benefits_Summary_v1.1.docx)<br>[`BIBS_TestPlan_BRD-08_Employee_Benefits_v1.1.xlsx`](../Drop-2_Independent/BRD-08_Employee_Benefits/BIBS_TestPlan_BRD-08_Employee_Benefits_v1.1.xlsx) |
| BRD-13 | Legacy invoices in cashiering, commission and BIR reports (items 1.U1, 1.D2, 1.D6) | [`02_BIBS_Handbook_BRD-13_Data_Migration_v2.1.docx`](../Drop-0_Setup_and_Data_Migration/BRD-13_Data_Migration/02_BIBS_Handbook_BRD-13_Data_Migration_v2.1.docx)<br>[`04_BIBS_TestPlan_BRD-13_Data_Migration_v2.1.xlsx`](../Drop-0_Setup_and_Data_Migration/BRD-13_Data_Migration/04_BIBS_TestPlan_BRD-13_Data_Migration_v2.1.xlsx)<br>[`05_BIBS_TestPlan_BRD-13_Data_Migration_Summary_v2.1.docx`](../Drop-0_Setup_and_Data_Migration/BRD-13_Data_Migration/05_BIBS_TestPlan_BRD-13_Data_Migration_Summary_v2.1.docx) |

## Still to write

| Document | BRD | Note |
|---|---|---|
| UAT plan and sign-off forms, Drop 1 (item 30) | - | By 16-Jul-2027; readiness statement by 30-Jul-2027 |
| Interface specifications of the Drop 1 channels | - | After BDOI IT answers the IQ questions |
| Technical Specification of each Drop 1 sign-off set | - | The technical content kept out of the business sets; reviewed by BDOI IT |
