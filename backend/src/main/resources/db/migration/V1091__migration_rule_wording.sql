-- =====================================================================================
-- iNXT BrokerVerse - V1091 Data Migration: business wording of the rule texts and of the remaining catalogue texts
-- that the Migration Console shows and exports. The texts named records, services and parameters of BIBS by their
-- internal names (crm formats, xref, cat_product.line_code, MIG_GOLIVE_RENEWAL_TO, table and service names in the
-- "Target in BIBS" of the object register) or carried raw status codes; they now read as the BRD-13 business
-- sign-off set words them (catalogue.yaml and the client wording of build_dm_pack.py, which applies this script
-- after V1090). The column names of the extract layouts stay as they are: they are the names of the file columns.
-- =====================================================================================

-- Object register: where each object lands in BIBS.
update mig_data_object set target = 'Lists of Values (maker-checker); MIS field catalogue' where code = 'R01';
update mig_data_object set target = 'Branches (check only, code map BRANCH)' where code = 'R02';
update mig_data_object set target = 'Sales organisation: sales units and account officers' where code = 'R03';
update mig_data_object set target = 'Insurers and insurer branches (insurer and party records)' where code = 'R04';
update mig_data_object set target = 'Product catalogue: product lines, cover types and products' where code = 'R05';
update mig_data_object set target = 'Commission rates of the product catalogue' where code = 'R07';
update mig_data_object set target = 'Receipt series of Cashiering' where code = 'R11';
update mig_data_object set target = 'Client master, registered as migrated clients' where code = 'C01';
update mig_data_object set target = 'Client payout accounts' where code = 'C03';
update mig_data_object set target = 'BIBS archive (legacy document attachments)' where code = 'C04';
update mig_data_object set target = 'None (a full periodic screening run after C01)' where code = 'C05';
update mig_data_object set target = 'Accounts, imported as legacy policy headers' where code = 'P01';
update mig_data_object set target = 'Renewal Advices already sent, given to Renewal with the policy header in the go-live extraction' where code = 'P03';
update mig_data_object set target = 'Operations invoice ledger (invoices, insurer shares, components) with an opening accounting entry on the legacy sub-ledgers' where code = 'F01';
update mig_data_object set target = 'Cashiering unapplied payments, as migrated items, with an opening accounting entry' where code = 'F02';
update mig_data_object set target = 'Legacy follow-up of the Collections worklist items' where code = 'F03';
update mig_data_object set target = 'Remittance holds and special remittances' where code = 'F04';
update mig_data_object set target = 'Cashiering PDC warehouse and check pick-up' where code = 'F06';
update mig_data_object set target = 'General ledger journals of type OPENING (value date 1-Jan-2028); at a true-up, reconciliation input only' where code = 'G01';
update mig_data_object set target = 'BIBS archive, record type GL journal' where code = 'G02';
update mig_data_object set target = 'General ledger journals of type OPENING (reference MIG-TU-<n>); opening adjustments of the legacy open items and unapplied payments' where code = 'G03';
update mig_data_object set target = 'BIBS archive (Legacy Inquiry)' where code = 'H01';
update mig_data_object set target = 'Legacy documents attached to the archive records' where code = 'H02';
update mig_data_object set rationale = 'Carried inside F01 (the direct payment flag and the commission components of direct-payment invoices); no separate file.' where code = 'F05';

-- Data-quality rules.
update mig_rule set description = 'Every coded column has an entry in the approved version of its code map set' where code = 'DQ-003';
update mig_rule set description = 'E-mail and PH mobile follow the client formats of BIBS (09xxxxxxxxx or +639xxxxxxxxx)' where code = 'DQ-007';
update mig_rule set description = 'Expiry after inception and on or after the cut-over date' where code = 'DQ-014';
update mig_rule set description = 'The client is loaded (cross-reference of migrated keys) or in the same load' where code = 'DQ-016';
update mig_rule set fixed_by = 'iorta TechNXT (load order) / BDOI (extract)' where code = 'DQ-016';
update mig_rule set description = 'The product belongs to the line (line of the product in the product master)' where code = 'DQ-017';
update mig_rule set description = 'Expiry equals the P01 expiry and falls from the cut-over date T to 31-May-2028 (go-live renewal window)' where code = 'DQ-019';
update mig_rule set fixed_by = 'iorta TechNXT (loader)' where code = 'DQ-020';
update mig_rule set description = 'Premium components and PR2307 - open = booked + adjusted - paid - written off; DTIP, commission, VAT on commission, WTAX - open = booked + adjusted - remitted - written off' where code = 'DQ-022';
update mig_rule set description = 'Per branch and currency, the trial balance lines of each legacy control account equal the F01 / F02 opening detail (Migration Clearing nets to 0.00)' where code = 'DQ-040';
update mig_rule set description = 'The next number is the last used number + 1 and lies within the series (from and to numbers)' where code = 'DQ-041';

-- Layout columns and code map sets.
update mig_layout_column set description = 'Marks the legacy control accounts whose balance F01 or F02 carries in detail' where seq = 5 and layout_id = (select id from mig_layout where code = 'R08' and version_no = 1);
update mig_layout_column set allowed_values = 'legacy_invoice_no of F01 or legacy_upp_ref of F02' where seq = 5 and layout_id = (select id from mig_layout where code = 'G03D' and version_no = 1);
update mig_code_map_set set name = 'Legacy values of the list ID type' where code = 'LOV:ID_TYPE';

-- Legacy status of an archive record: the Legacy Inquiry shows its label, with the value kept from legacy in the
-- tooltip. A status not in the list reads as the legacy value in words; the list is maintained by the Data Steward.
insert into lov_type (code, name, description, maintainable, owner_permission, created_at, created_by)
values ('LEGACY_RECORD_STATUS', 'Legacy record status', 'Status of a legacy archive record as the legacy system stored it',
        true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM')
on conflict (code) do nothing;

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('LEGACY_RECORD_STATUS', 'OPEN', 'Open', 10),
    ('LEGACY_RECORD_STATUS', 'UNPAID', 'Unpaid', 20),
    ('LEGACY_RECORD_STATUS', 'PARTIAL', 'Partly paid', 30),
    ('LEGACY_RECORD_STATUS', 'PAID', 'Fully paid', 40),
    ('LEGACY_RECORD_STATUS', 'CLOSED', 'Closed', 50),
    ('LEGACY_RECORD_STATUS', 'CANCELLED', 'Cancelled', 60),
    ('LEGACY_RECORD_STATUS', 'REVERSED', 'Reversed', 70),
    ('LEGACY_RECORD_STATUS', 'VOID', 'Voided', 80),
    ('LEGACY_RECORD_STATUS', 'WRITTEN_OFF', 'Written off', 90),
    ('LEGACY_RECORD_STATUS', 'IF', 'In force', 100),
    ('LEGACY_RECORD_STATUS', 'EXPIRED', 'Expired', 110),
    ('LEGACY_RECORD_STATUS', 'LAPSED', 'Lapsed', 120),
    ('LEGACY_RECORD_STATUS', 'RENEWED', 'Renewed', 130),
    ('LEGACY_RECORD_STATUS', 'POSTED', 'Posted', 140),
    ('LEGACY_RECORD_STATUS', 'REMITTED', 'Remitted to the insurer', 150),
    ('LEGACY_RECORD_STATUS', 'SETTLED', 'Settled', 160),
    ('LEGACY_RECORD_STATUS', 'ACTIVE', 'Active', 170),
    ('LEGACY_RECORD_STATUS', 'INACTIVE', 'Inactive', 180)
) as v(type_code, code, label, sort_order)
where not exists (select 1 from lov_value x where x.type_code = v.type_code and x.code = v.code);
