# BRD-13 Data Migration - extract templates

Version 1.0, 26 September 2026. BDO Insurance and Reinsurance Brokers, Inc. (BDOI) - BIBS. Prepared by iorta TechNXT. Confidential - BDOI.

One CSV template per extract layout. Each file holds only the header row: the field names exactly as in the layout sheet of the BDOI Data Requirements Workbook (`../BIBS_Migration_BRD-13_Data_Requirements_Workbook_v1.0.xlsx`), which gives the type, length, mandatory flag, allowed values, format, example, BIBS target and validation rule of every field.

How to use a template:

1. Copy the template and name the copy `<LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv`, for example `F01C_EBIX_20270226_01.csv` (as-of date, sequence of the day).
2. Write one row per record under the header: UTF-8 without BOM, comma separator, RFC 4180 quoting, dates yyyy-MM-dd, amounts with a dot decimal and 2 decimals, codes exactly as stored in legacy.
3. Produce the control file `<data file name>.ctl.csv` from `CONTROL_template.ctl.csv`: row count, hash total of the key, amount totals per column and currency, SHA-256 of the data file.
4. Send both files through the Migration Console upload or the agreed SFTP drop. Never by e-mail.

| Template | Layout | Object | Decision (proposed) | Source system | Fields | Key |
|---|---|---|---|---|---|---|
| `R01_template.csv` | Lists of values and MIS values | R01 | Migrate | QPS, EBIX | 8 | list_type, code |
| `R02_template.csv` | Branches and invoicing branches | R02 | Migrate | EBIX | 5 | branch_code |
| `R03_template.csv` | Sales organisation (units and account officers) | R03 | Migrate | QPS, EBIX | 11 | record_type, unit_code, ao_user_id |
| `R04_template.csv` | Insurers | R04 | Migrate | QPS, EBIX | 10 | insurer_code |
| `R04B_template.csv` | Insurer branches | R04 | Migrate | QPS, EBIX | 7 | insurer_code, branch_code |
| `R05_template.csv` | Product lines, cover types, products and risk codes | R05 | Migrate | QPS, EBIX | 9 | risk_code |
| `R06_template.csv` | Packages (active versions) | R06 | Migrate | QPS | 10 | package_code, package_version, insurer_code |
| `R07_template.csv` | Commission rates by insurer and product | R07 | Migrate | QPS, EBIX | 5 | insurer_code, risk_code, effective_from |
| `R08_template.csv` | Chart of accounts mapping | R08 | Migrate | EBIX, ISYS GL | 8 | legacy_account_code |
| `R09_template.csv` | Payees | R09 | Conditional | EBIX, Disbursement files | 11 | payee_code |
| `R11_template.csv` | Receipt series in use | R11 | Carry forward | EBIX | 8 | branch_code, kind, prefix |
| `C01_template.csv` | Client master | C01 | Migrate | QPS, EBIX, CMS | 36 | legacy_client_no |
| `C02_template.csv` | Client addresses and contacts | C01 | Migrate | QPS, EBIX, CMS | 13 | legacy_client_no, record_type, seq_no |
| `C03_template.csv` | Client payout accounts | C03 | Migrate | EBIX | 6 | legacy_client_no, account_no |
| `P01_template.csv` | In-force policy headers | P01 | Conditional | QPS, EBIX | 29 | legacy_policy_ref |
| `P01S_template.csv` | Policy insurer shares | P01 | Conditional | QPS, EBIX | 6 | legacy_policy_ref, share_seq |
| `P03_template.csv` | RMEL cohorts already extracted in legacy | P03 | Carry forward | RMEL files, QPS | 12 | legacy_policy_ref |
| `F01_template.csv` | Open legacy invoices - header | F01 | Carry forward | EBIX, QPS | 38 | legacy_invoice_no |
| `F01S_template.csv` | Open legacy invoices - insurer shares | F01 | Carry forward | EBIX, QPS | 5 | legacy_invoice_no, share_seq |
| `F01C_template.csv` | Open legacy invoices - components and positions | F01 | Carry forward | EBIX, QPS | 8 | legacy_invoice_no, component |
| `F02_template.csv` | Unapplied premium payments (UPP) | F02 | Carry forward | EBIX, CMS | 21 | legacy_upp_ref |
| `F03_template.csv` | Open collection state | F03 | Carry forward | CMS | 12 | legacy_invoice_no, record_type, seq_no |
| `F04_template.csv` | Remittance in flight | F04 | Conditional | EBIX | 10 | legacy_batch_no, legacy_invoice_no |
| `F06_template.csv` | PDCs, check pick-ups and refunds in process | F06 | Conditional | EBIX | 14 | record_type, reference |
| `G01_template.csv` | GL opening trial balance | G01 | Migrate | EBIX GL, ISYS GL | 10 | branch_code, currency, legacy_account_code, cost_center |
| `H01_template.csv` | Archive records (closed transactions and history) | H01 | Archive | EBIX, QPS, ISYS | 15 | record_type, legacy_key |
| `H02_template.csv` | Archive documents index | H02 | Archive | File shares, EBIX | 8 | record_type, legacy_key, file_name |
| `CONTROL_template.ctl.csv` | Control file | all | - | - | 12 | data_file, measure, column_name, currency, filter |

Objects without a template:

- R10 Users and roles: Excluded. Users are created through User Access requests and approvals (BRD-11); legacy user IDs are only mapped (map set USER) so AO, handler and collector fields resolve.
- C05 Screening results and risk ratings: Excluded. One full screening run after the client load gives current results (BRD-10); legacy results are archived with the client history.
- P04 Submitted-policy masterlists: Conditional. Loaded through the Submitted Policies migration handler (SBM_MIGRATION) if BDOI confirms (SP SQ16, DMQ30); layout issued with that module.
- P05 Employee Benefits programmes: Conditional. Loaded through EB_PROGRAMME_LOAD if BDOI confirms (DMQ30); layout issued with the EB module.
- F05 DP billing in process and DP commission receivable: Carry forward. Carried inside F01 (dp_flag and the COMMISSION components of direct-payment invoices); no separate file.
- F07 Open claims: Conditional. Loaded through the Claims migration (BCL_CLAIM_MIGRATION, V1025 held) if BDOI decides (CLQ14, DMQ30); closed claims are archived.
