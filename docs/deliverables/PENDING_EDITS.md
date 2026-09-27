# Pending edits to documents being edited by the build teams

Status as of 26-Sep-2026. The programme alignment and the BDOI answers of 26-Sep-2026 were consolidated into the shared
documents on that date (register v1.2; FRS and test plan BRD-6 v1.1; FRS BRD-2 and BRD-11 v1.1; the Renewal, User
Access, Operations, Accounting and Disbursement designs; ARCHITECTURE.md; the cross-BRD decisions). The documents below
are being edited by the Employee Benefits and Claims build teams, so the edits they need are listed here and applied
when those teams hand the documents back. Delete each row when it is applied.

## Employee Benefits: no portal (BDOI drop plan item 2.4, treated as decided)

BDOI places "Employee Benefits (no portal feature)" in Drop 2. Register DCR-211 and DCR-235, alignment question IQ22.
Decision D7 of `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` and USER_ACCESS_DESIGN section 4.4 are already marked
dormant.

| Document | Edit |
|---|---|
| `docs/deliverables/src/BRD-08_Employee_Benefits/FRS_BRD08_EMPLOYEE_BENEFITS.md` (v1.1) | Portal requirements (BRID-005, 005.01, 014 portal parts; the portal user, log-in, upload, download and notice FRs) become OUT with the reason "BDOI drop plan 2.4: no portal feature"; the file exchange they carry is met by internal upload FRs; interfaces table: portal row OUT; open questions EBQ13 (portal users) closed as not applicable; document control row 1.1 with "Status as of" date |
| `docs/deliverables/src/BRD-08_Employee_Benefits/brd08_cases.yaml`, `TP_BRD08_EMPLOYEE_BENEFITS.md` (v1.1) | Remove the portal cases and the portal personas (insurer user, client HR user) and their access rows; add cases for the internal upload that replaces them; rebuild the workbook and summary as v1.1 and delete v1.0 |
| `docs/requirements/BDOI_EB_BRD_SPEC.md` | Mark the portal rows (BRID-005, 005.01, 014 and the portal NFRs) as out of scope by the BDOI drop plan of 26-Sep-2026 |

## Claims (Drop 2)

| Document | Edit |
|---|---|
| `docs/architecture/CLAIMS_BROKING_DESIGN.md` | Record the drop: Claims is Drop 2 item 2.2 (SIT Jul - Sep 2027, UAT Oct - Nov 2027); it is built ahead of the drop. Insurer claim files (notices, settlement advices) are part of the "Insurer System" integration INT-19 of PROGRAMME_ALIGNMENT section 5 (`InsurerFileInbox`, IQ18). Claim documents move to S3 with build step ST1 (DOCUMENT_STORAGE_DECISION section 4). Open-claims migration F07 stays conditional (CLQ14, DMQ30; V1025 held) |
| `docs/requirements/BDOI_CLM_BRD_SPEC.md` | None from the alignment |
| FRS BRD-7 and `brd07_cases.yaml` / `TP_BRD07_CLAIMS.md` | At the next issue: the Drop 2 dates in the introduction; the insurer channel row of the interfaces table names INT-19 (Insurer System, IQ18); the register reference becomes v1.2 |
| `docs/modules/**` (Claims guide) | The drop and the S3 storage of claim documents once ST1 lands |

## Other documents (next issue)

| Document | Edit | When |
|---|---|---|
| `docs/deliverables/src/alignment/PROGRAMME_ALIGNMENT.md`, `alignment_data.yaml` (client document v1.1) | IQ02 and IQ03 answered (DMQ37, DMQ36); the renewal check is named `PACKAGE_REMAP`; the link to `ARCHITECTURE_OPTION_DECISION.md` at IQ25 / DCR-222 / DCR-223; the drop folders as built (`out/Drop-0_Setup_and_Data_Migration/`, `out/Drop-1_Transactional/`, `out/Drop-2_Independent/`, `out/Programme/`); "Status as of" date | Next alignment pack issue |
| `docs/deliverables/src/BRD-13_Data_Migration/migration/build_migration_pack.py` | Example file name `F01C_EBIX_20270226_01.csv` in the workbook README and the templates README: use a December 2027 date for a January 2028 go-live | Next migration pack issue |
| Data Migration documents (DATA_MIGRATION_DESIGN section 15, dm_layouts.yaml object P03, FRS BRD-13, test plan BRD-13, migration pack) | BDOI answers DMQ36-DMQ39: no carried RMEL cohorts (P03), remapping at sanitation (the PACKAGE map is loaded only), RMEL and dispositions in Excel, year-end option A | Data Migration team (in progress) |
| Test plan summaries BRD-2 and BRD-11 | Reference FRS v1.1 and register v1.2 | Next test plan issue |

## Merge policy (BDOI, 26-Sep-2026)

When this work is merged into `main`, it is merged as a single squash commit authored by iorta TechNXT, with a plain
descriptive message. The working branch is deleted afterwards. `main` then carries one clean, reviewed change.

## From Data Migration v1.2 (26-Sep-2026)

- FRS BRD-06 (next issue): apply the notes of RENEWAL_DESIGN section 17.8 (the design and the code carry the
  `PACKAGE_REMAP` check, the go-live window and urgent flag parameters, the advices already sent and the package map).
- `tools/deliverables/brand.py`: add `RECOMMENDED` to `STATUS_COLOURS`. `drop_index.py`: skip the `_previews/` folders.
- Register: DCR-240, 241 and 243 texts to the final answers (no carried cohorts; remapping at sanitation); DCR-242 to
  answered-with-recommendation.
- Deliverables README and PROGRAMME_ALIGNMENT: Data Migration set at v1.2 (44 FRs, 181 test cases, objects G03 / G03D).
