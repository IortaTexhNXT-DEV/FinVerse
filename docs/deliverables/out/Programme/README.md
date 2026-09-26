# Programme (cross-drop): deliverables index

BIBS client pack for BDO Insurance and Reinsurance Brokers (BDOI), grouped by BDOI drop (answer A5 of 26-Sep-2026).
Written by `python tools/deliverables/drop_index.py` from the files of this folder and the drop map in
`tools/deliverables/brand.py`; do not edit by hand.

| | |
|---|---|
| BDOI dates | Performance and penetration test Nov - Dec 2027, ORR / PRR Dec 2027 - Jan 2028, go-live of all modules together in January 2028 (proposed Monday 3 January 2028) |
| Scope (BDOI drop plan) | Documents that cover every drop: the umbrella BRD-00 FRS, the discrepancy and clarification register, the business process deck, the programme alignment pack with the integration inventory and the IER diagrams, and the UAT readiness programme. |
| Status | Status as of 26-Sep-2026; final refresh at build completion (deliverables README, "Document status and the final as-built refresh") |

## Documents in this drop

One folder per BRD release set (`BRD-nn_<Name>/`): its FRS, sign-off workbook, test plan, release note and
any other document of the BRD, released and signed off together (deliverables README, "Release and sign-off
per BRD").

| Document | BRD | Kind | Version | File |
|---|---|---|---|---|
| Core Replacement | BRD-00 | FRS | 1.0 | [`FRS/BIBS_FRS_BRD-00_Core_Replacement_v1.0.docx`](FRS/BIBS_FRS_BRD-00_Core_Replacement_v1.0.docx) |
| BIBS IER Application Architecture | BRD-00 | IER diagram (PNG) | - | [`Alignment/IER/BIBS_IER_Application_Architecture.png`](Alignment/IER/BIBS_IER_Application_Architecture.png) |
| BIBS IER Infrastructure Deployment | BRD-00 | IER diagram (PNG) | - | [`Alignment/IER/BIBS_IER_Infrastructure_Deployment.png`](Alignment/IER/BIBS_IER_Infrastructure_Deployment.png) |
| Business Process AsIs Envisioned BestPractice | BRD-00 | Deck | 1.0 | [`Decks/BIBS_Deck_BRD-00_Business_Process_AsIs_Envisioned_BestPractice_v1.0.pptx`](Decks/BIBS_Deck_BRD-00_Business_Process_AsIs_Envisioned_BestPractice_v1.0.pptx) |
| Change Management | BRD-00 | Change register summary (Word) | 1.0 | [`Change_Management/BIBS_Change_Register_BRD-00_Change_Management_Summary_v1.0.docx`](Change_Management/BIBS_Change_Register_BRD-00_Change_Management_Summary_v1.0.docx) |
| Change Management | BRD-00 | Change register workbook (Excel) | 1.0 | [`Change_Management/BIBS_Change_Register_BRD-00_Change_Management_v1.0.xlsx`](Change_Management/BIBS_Change_Register_BRD-00_Change_Management_v1.0.xlsx) |
| Discrepancies and Clarifications | BRD-00 | Register | 1.2 | [`Registers/BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.2.xlsx`](Registers/BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.2.xlsx) |
| Drops Integrations Infrastructure | BRD-00 | Alignment pack | 1.0 | [`Alignment/BIBS_Alignment_BRD-00_Drops_Integrations_Infrastructure_v1.0.docx`](Alignment/BIBS_Alignment_BRD-00_Drops_Integrations_Infrastructure_v1.0.docx) |
| Integration Inventory | BRD-00 | Alignment pack | 1.0 | [`Alignment/BIBS_Alignment_BRD-00_Integration_Inventory_v1.0.xlsx`](Alignment/BIBS_Alignment_BRD-00_Integration_Inventory_v1.0.xlsx) |

## Still to write

| Document | BRD | Note |
|---|---|---|
| UAT readiness programme and readiness statements (deliverables README) | - | Drop 1 by 30-Jul-2027, Drop 2 by 30-Sep-2027 |
| End-to-end UAT script | - | UAT runs end to end (BDOI answer A1) |
| Performance, penetration test and ORR / PRR evidence (items 28, 37) | - | Nov 2027 - Jan 2028 |
| Requirements traceability matrix (item 21) and the final as-built refresh | - | At build completion |
