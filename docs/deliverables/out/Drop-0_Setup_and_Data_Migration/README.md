# Drop 0 - Setup and Data Migration: deliverables index

BIBS client pack for BDO Insurance and Reinsurance Brokers (BDOI), grouped by BDOI drop (answer A5 of 26-Sep-2026).

| | |
|---|---|
| BDOI dates | Setup with the Drop 1 requirements (Sep - Nov 2026); migration requirements and mapping Sep - Nov 2026, development Nov 2026 - Mar 2027, SIT Apr - Jul 2027, UAT Aug - Oct 2027, full migration and cut-over Nov 2027 - Jan 2028 |
| Scope (BDOI drop plan) | Setup: 0.1 Accessibility and Login, 0.2 Authorization, 0.3 User Maintenance, 0.4 Data Management (GL accounts, reference tables), 0.5 Workflow, 0.6 Product Maintenance; the Data Migration stream; the Drop 0 integrations (EIAM, UIDM-ISC, LMS, HL-LOAS, PMS, CMS / New BOB, OBPCS, AFTS, Old BOB, ECM, CCM, M365, TFS). |
| Status | Status as of 26-Sep-2026; the documents are refreshed before UAT |

## Documents in this drop

One folder per BRD release set (`BRD-nn_<Name>/`): every file of the BRD (Start Here, guide deck, FRS, sign-off
workbook, test plan and summary, migration documents and templates), released and signed off together; in an
issued sign-off set the files carry the reading-order prefix 00_ to 05_ (deliverables README, "Release and
sign-off per BRD"). Each document is kept once, in its latest version.

| Document | BRD | Kind | Version | File |
|---|---|---|---|---|
| Product Maintenance | BRD-03 | Start here guide | 2.0 | [`BRD-03_Product_Maintenance/00_BIBS_StartHere_BRD-03_Product_Maintenance_v2.0.docx`](BRD-03_Product_Maintenance/00_BIBS_StartHere_BRD-03_Product_Maintenance_v2.0.docx) |
| Product Maintenance | BRD-03 | Sign-off pack guide deck | 2.0 | [`BRD-03_Product_Maintenance/01_BIBS_GuideDeck_BRD-03_Product_Maintenance_v2.0.pptx`](BRD-03_Product_Maintenance/01_BIBS_GuideDeck_BRD-03_Product_Maintenance_v2.0.pptx) |
| Product Maintenance | BRD-03 | FRS | 2.0 | [`BRD-03_Product_Maintenance/02_BIBS_FRS_BRD-03_Product_Maintenance_v2.0.docx`](BRD-03_Product_Maintenance/02_BIBS_FRS_BRD-03_Product_Maintenance_v2.0.docx) |
| Product Maintenance | BRD-03 | Sign-off workbook (Excel) | 2.0 | [`BRD-03_Product_Maintenance/03_BIBS_Signoff_BRD-03_Product_Maintenance_v2.0.xlsx`](BRD-03_Product_Maintenance/03_BIBS_Signoff_BRD-03_Product_Maintenance_v2.0.xlsx) |
| Product Maintenance | BRD-03 | Test plan workbook (Excel) | 2.0 | [`BRD-03_Product_Maintenance/04_BIBS_TestPlan_BRD-03_Product_Maintenance_v2.0.xlsx`](BRD-03_Product_Maintenance/04_BIBS_TestPlan_BRD-03_Product_Maintenance_v2.0.xlsx) |
| Product Maintenance | BRD-03 | Test plan summary (Word) | 2.0 | [`BRD-03_Product_Maintenance/05_BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v2.0.docx`](BRD-03_Product_Maintenance/05_BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v2.0.docx) |
| Configuration Inputs | BRD-03 | Templates | 2.0 | [`BRD-03_Product_Maintenance/06_BIBS_Templates_BRD-03_Configuration_Inputs_v2.0.xlsx`](BRD-03_Product_Maintenance/06_BIBS_Templates_BRD-03_Configuration_Inputs_v2.0.xlsx) |
| User Access Maintenance | BRD-11 | FRS | 1.1 | [`BRD-11_User_Access_Maintenance/BIBS_FRS_BRD-11_User_Access_Maintenance_v1.1.docx`](BRD-11_User_Access_Maintenance/BIBS_FRS_BRD-11_User_Access_Maintenance_v1.1.docx) |
| User Access Maintenance | BRD-11 | Test plan summary (Word) | 1.0 | [`BRD-11_User_Access_Maintenance/BIBS_TestPlan_BRD-11_User_Access_Maintenance_Summary_v1.0.docx`](BRD-11_User_Access_Maintenance/BIBS_TestPlan_BRD-11_User_Access_Maintenance_Summary_v1.0.docx) |
| User Access Maintenance | BRD-11 | Test plan workbook (Excel) | 1.0 | [`BRD-11_User_Access_Maintenance/BIBS_TestPlan_BRD-11_User_Access_Maintenance_v1.0.xlsx`](BRD-11_User_Access_Maintenance/BIBS_TestPlan_BRD-11_User_Access_Maintenance_v1.0.xlsx) |
| Data Migration | BRD-13 | FRS | 1.2 | [`BRD-13_Data_Migration/BIBS_FRS_BRD-13_Data_Migration_v1.2.docx`](BRD-13_Data_Migration/BIBS_FRS_BRD-13_Data_Migration_v1.2.docx) |
| Data Migration | BRD-13 | Test plan summary (Word) | 1.2 | [`BRD-13_Data_Migration/BIBS_TestPlan_BRD-13_Data_Migration_Summary_v1.2.docx`](BRD-13_Data_Migration/BIBS_TestPlan_BRD-13_Data_Migration_Summary_v1.2.docx) |
| Data Migration | BRD-13 | Test plan workbook (Excel) | 1.2 | [`BRD-13_Data_Migration/BIBS_TestPlan_BRD-13_Data_Migration_v1.2.xlsx`](BRD-13_Data_Migration/BIBS_TestPlan_BRD-13_Data_Migration_v1.2.xlsx) |
| Cutover Runbook | BRD-13 | Migration pack | 1.2 | [`BRD-13_Data_Migration/BIBS_Migration_BRD-13_Cutover_Runbook_v1.2.docx`](BRD-13_Data_Migration/BIBS_Migration_BRD-13_Cutover_Runbook_v1.2.docx) |
| Cutover Task Plan | BRD-13 | Migration pack | 1.2 | [`BRD-13_Data_Migration/BIBS_Migration_BRD-13_Cutover_Task_Plan_v1.2.xlsx`](BRD-13_Data_Migration/BIBS_Migration_BRD-13_Cutover_Task_Plan_v1.2.xlsx) |
| Data Migration Strategy and Approach | BRD-13 | Migration pack | 1.2 | [`BRD-13_Data_Migration/BIBS_Migration_BRD-13_Data_Migration_Strategy_and_Approach_v1.2.docx`](BRD-13_Data_Migration/BIBS_Migration_BRD-13_Data_Migration_Strategy_and_Approach_v1.2.docx) |
| Data Requirements Workbook | BRD-13 | Migration pack | 1.2 | [`BRD-13_Data_Migration/BIBS_Migration_BRD-13_Data_Requirements_Workbook_v1.2.xlsx`](BRD-13_Data_Migration/BIBS_Migration_BRD-13_Data_Requirements_Workbook_v1.2.xlsx) |
| Reconciliation Approach and Signoff | BRD-13 | Migration pack | 1.2 | [`BRD-13_Data_Migration/BIBS_Migration_BRD-13_Reconciliation_Approach_and_Signoff_v1.2.docx`](BRD-13_Data_Migration/BIBS_Migration_BRD-13_Reconciliation_Approach_and_Signoff_v1.2.docx) |
| Migration extract templates (CSV headers and control file) | BRD-13 | Templates | - | [`BRD-13_Data_Migration/templates/`](BRD-13_Data_Migration/templates/) |

## Also part of this drop (documents kept in their primary drop)

A BRD that spans drops lives in the folder of its primary drop; nothing is copied.

| BRD | Part in this drop | Documents (in the primary drop folder) |
|---|---|---|
| BRD-01 | Client onboarding is also a migration object (clients C01-C03) | [`02_BIBS_FRS_BRD-01_New_Business_v2.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/02_BIBS_FRS_BRD-01_New_Business_v2.0.docx)<br>[`04_BIBS_TestPlan_BRD-01_New_Business_v2.0.xlsx`](../Drop-1_Transactional/BRD-01_New_Business/04_BIBS_TestPlan_BRD-01_New_Business_v2.0.xlsx)<br>[`05_BIBS_TestPlan_BRD-01_New_Business_Summary_v2.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/05_BIBS_TestPlan_BRD-01_New_Business_Summary_v2.0.docx) |
| BRD-05 | GL accounts and reference tables (item 0.4) | [`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Cover_Note_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Cover_Note_v1.0.docx)<br>[`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx)<br>[`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_Summary_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_Summary_v1.0.docx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_Summary_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_Summary_v1.0.docx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx) |

## Still to write

| Document | BRD | Note |
|---|---|---|
| Bill of materials, technical and deployment architecture (items 4, 12) | BRD-00 | From the programme alignment pack (chapter 6), the IER and the architecture option decision |
| Security and data-protection controls mapping (item 26) | BRD-00 | EIAM, UIDM-ISC, S3 encryption, masking |
| Interface specifications of the Drop 0 integrations | - | After BDOI IT answers the IQ questions |
