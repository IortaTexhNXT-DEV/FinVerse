# Drop 0 - Setup and Data Migration: deliverables index

BIBS client pack for BDO Insurance and Reinsurance Brokers (BDOI), grouped by BDOI drop (answer A5 of 26-Sep-2026).
Written by `python tools/deliverables/drop_index.py` from the files of this folder and the drop map in
`tools/deliverables/brand.py`; do not edit by hand.

| | |
|---|---|
| BDOI dates | Setup with the Drop 1 requirements (Sep - Nov 2026) and build wave 1; migration requirements and mapping Sep - Nov 2026, build Nov 2026 - Mar 2027, SIT Apr - Jul 2027, UAT Aug - Oct 2027, full migration and cut-over Nov 2027 - Jan 2028 |
| Scope (BDOI drop plan) | Setup: 0.1 Accessibility and Login, 0.2 Authorization, 0.3 User Maintenance, 0.4 Data Management (GL accounts, reference tables), 0.5 Workflow, 0.6 Product Maintenance; the Data Migration stream; the Drop 0 integrations (EIAM, UIDM-ISC, LMS, HL-LOAS, PMS, CMS / New BOB, OBPCS, AFTS, Old BOB, ECM, CCM, M365, TFS). |
| Status | Status as of 26-Sep-2026; final refresh at build completion (deliverables README, "Document status and the final as-built refresh") |

## Documents in this drop

One folder per BRD release set (`BRD-nn_<Name>/`): its FRS, sign-off workbook, test plan, release note and
any other document of the BRD, released and signed off together (deliverables README, "Release and sign-off
per BRD").

| Document | BRD | Kind | Version | File |
|---|---|---|---|---|
| Product Maintenance | BRD-03 | FRS | 1.0 | [`BRD-03_Product_Maintenance/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx`](BRD-03_Product_Maintenance/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx) |
| Product Maintenance | BRD-03 | Test plan summary (Word) | 1.0 | [`BRD-03_Product_Maintenance/BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v1.0.docx`](BRD-03_Product_Maintenance/BIBS_TestPlan_BRD-03_Product_Maintenance_Summary_v1.0.docx) |
| Product Maintenance | BRD-03 | Test plan workbook (Excel) | 1.0 | [`BRD-03_Product_Maintenance/BIBS_TestPlan_BRD-03_Product_Maintenance_v1.0.xlsx`](BRD-03_Product_Maintenance/BIBS_TestPlan_BRD-03_Product_Maintenance_v1.0.xlsx) |
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
| BRD-01 | Client onboarding is also a migration object (clients C01-C03) | [`BIBS_FRS_BRD-01_New_Business_v1.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/BIBS_FRS_BRD-01_New_Business_v1.0.docx)<br>[`BIBS_FRS_BRD-01_New_Business_v2.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/BIBS_FRS_BRD-01_New_Business_v2.0.docx)<br>[`BIBS_TestPlan_BRD-01_New_Business_Summary_v1.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/BIBS_TestPlan_BRD-01_New_Business_Summary_v1.0.docx)<br>[`BIBS_TestPlan_BRD-01_New_Business_Summary_v2.0.docx`](../Drop-1_Transactional/BRD-01_New_Business/BIBS_TestPlan_BRD-01_New_Business_Summary_v2.0.docx)<br>[`BIBS_TestPlan_BRD-01_New_Business_v1.0.xlsx`](../Drop-1_Transactional/BRD-01_New_Business/BIBS_TestPlan_BRD-01_New_Business_v1.0.xlsx)<br>[`BIBS_TestPlan_BRD-01_New_Business_v2.0.xlsx`](../Drop-1_Transactional/BRD-01_New_Business/BIBS_TestPlan_BRD-01_New_Business_v2.0.xlsx) |
| BRD-05 | GL accounts and reference tables (item 0.4) | [`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Cover_Note_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Cover_Note_v1.0.docx)<br>[`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx)<br>[`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_Summary_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_Summary_v1.0.docx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.xlsx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_Summary_v1.0.docx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_Summary_v1.0.docx)<br>[`BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx`](../Drop-1_Transactional/BRD-05_Accounting_Disbursement_ACSL/BIBS_TestPlan_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.xlsx) |

## Still to write

| Document | BRD | Note |
|---|---|---|
| Bill of materials, technical and deployment architecture (items 4, 12) | BRD-00 | From PROGRAMME_ALIGNMENT section 6, the IER and ARCHITECTURE_OPTION_DECISION.md |
| Security and data-protection controls mapping (item 26) | BRD-00 | EIAM, UIDM-ISC, S3 encryption, masking |
| Interface specifications of the Drop 0 integrations | - | After BDOI IT answers the IQ questions |
