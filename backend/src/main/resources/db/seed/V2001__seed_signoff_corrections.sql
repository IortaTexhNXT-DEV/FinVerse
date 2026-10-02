-- Seed data corrections found by the business sign-off packs (BRD-1 New Business, BRD-3 Product
-- Maintenance). Seed profile only; runs after all seed data (V2000). Every change is limited to the
-- seeded rows it corrects.

-- ---------- Package products: active for testing -------------------------------------------------
-- V818 makes the Fire and Motor packages inactive until their matrix is loaded; the seed profile
-- sells them (walkthroughs, SIT and UAT cases).
update cat_product
set record_status = 'ACTIVE', updated_at = now(), updated_by = 'SYSTEM'
where code in ('MTR08', 'MTR10', 'MTR12', 'MTR15', 'MTR22', 'MTR23', 'MTR24', 'MTR25', 'MTR26',
               'MTR27', 'MTR28', 'MTR29', 'MTR31', 'MTR32', 'MTR34', 'MTR35', 'MTR38',
               'PAR01', 'PAR08', 'PAR09', 'PAR13', 'PAR19', 'PAR25')
  and record_status = 'INACTIVE';

-- ---------- Two seed clients had the same name ------------------------------------------------------
-- CL-2026-000001 and CL-2026-900001 were both "Santos, Maria Clara Reyes". No test needs the pair
-- (the duplicate check is shown with the TIN of CL-2026-000001), so CL-2026-900001 becomes another
-- person, on the client and on every seeded record that carries its name.
do $$
declare
    old_name  constant text := 'Santos, Maria Clara Reyes';
    new_name  constant text := 'Villareal, Teresa Mae Gonzales';
    old_email constant text := 'mc.santos@example.ph';
    new_email constant text := 'tm.villareal@example.ph';
    client    bigint;
    t         record;
begin
    select id into client from crm_client where client_code = 'CL-2026-900001' and display_name = old_name;
    if client is null then
        return;
    end if;
    update crm_client
    set last_name = 'Villareal', first_name = 'Teresa Mae', middle_name = 'Gonzales',
        display_name = new_name, email = new_email, updated_at = now(), updated_by = 'SYSTEM'
    where id = client;
    for t in
        select c.table_name
        from information_schema.columns c
        join information_schema.columns n
          on n.table_schema = c.table_schema and n.table_name = c.table_name and n.column_name = 'client_name'
        join information_schema.tables b
          on b.table_schema = c.table_schema and b.table_name = c.table_name and b.table_type = 'BASE TABLE'
        where c.table_schema = current_schema() and c.column_name = 'client_code'
    loop
        execute format('update %I set client_name = %L where client_code = %L and client_name = %L',
                       t.table_name, new_name, 'CL-2026-900001', old_name);
    end loop;
    for t in
        select c.table_name
        from information_schema.columns c
        join information_schema.columns n
          on n.table_schema = c.table_schema and n.table_name = c.table_name and n.column_name = 'client_name'
        join information_schema.tables b
          on b.table_schema = c.table_schema and b.table_name = c.table_name and b.table_type = 'BASE TABLE'
        where c.table_schema = current_schema() and c.column_name = 'client_id'
    loop
        execute format('update %I set client_name = %L where client_id = %s and client_name = %L',
                       t.table_name, new_name, client, old_name);
    end loop;
    update acc_account
    set contact_name = new_name, contact_email = new_email
    where client_code = 'CL-2026-900001' and contact_name = old_name;
    update acc_account
    set contact_email = new_email
    where client_code = 'CL-2026-900001' and contact_email = old_email;
end $$;

-- ---------- Test-like addresses of the booking seed --------------------------------------------------
update acc_risk_item set address = '9 San Miguel Avenue, Ortigas Center'
where address = '9 Booking Street, Ortigas Center';
update acc_risk_item set address = '12 East Capitol Drive, Barangay Kapitolyo'
where address = '12 Booking Road, Lahug';
update acc_risk_item set address = '27 United Street, Barangay Kapitolyo'
where address = '27 Multi-Year Avenue, Kapitolyo';

-- ---------- Location keys as the application writes them ---------------------------------------------
-- RiskIdentifiers.locationKey: address and city in lower case, every run of other characters than
-- letters and digits replaced by one space, trimmed, joined by " | ". Seeded keys kept commas, so the
-- duplicate check did not find the seeded risks.
update acc_risk_item i
set location_key = k.key
from (
    select id,
           case when a = '' then null when c = '' then a else a || ' | ' || c end as key
    from (
        select id,
               btrim(regexp_replace(lower(coalesce(address, '')), '[^a-z0-9]+', ' ', 'g')) as a,
               btrim(regexp_replace(lower(coalesce(city, '')), '[^a-z0-9]+', ' ', 'g')) as c
        from acc_risk_item
        where address is not null
    ) raw
) k
where i.id = k.id and i.location_key is distinct from k.key;

update bcl_location_ref r
set location_key = i.location_key
from acc_risk_item i
where i.account_id = r.account_id and i.item_no = r.account_item_no
  and i.location_key is not null and r.location_key is distinct from i.location_key;

-- ---------- Unapplied Payment Handler signs in like the other seed users -------------------------------
update sec_user set must_change_password = false where username = 'upphandler';

-- ---------- Seed texts written as placeholders ---------------------------------------------------------
update bkg_incentive_rule
set description = 'CBG motor MTR10 campaign 2026'
where description = 'Placeholder: CBG motor MTR10 campaign 2026';
update cat_incentive_criteria
set description = 'CBG motor MTR10 campaign 2026'
where description = 'Placeholder: CBG motor MTR10 campaign 2026';
update bkg_auto_book_rule
set description = 'CBG fire PAR08 accounts are booked by the end-of-day batch'
where description = 'Placeholder: CBG fire PAR08 is booked by the end-of-day batch';
update rem_incentive_rule
set description = '2% early remittance incentive, CBG fire, within 30 days of inception'
where description = 'Placeholder: 2% early remittance incentive, CBG fire, within 30 days of inception';
update cat_product_version
set change_summary = 'Yearly review of the MTR12 package with a panel of two insurers'
where product_code = 'MTR12' and version_no = 2
  and change_summary = 'Placeholder: yearly review of the MTR12 package with a panel of two insurers';
update lov_value set label = 'Transaction with a listed person'
where type_code = 'SCR_STR_REASON' and label = 'Placeholder: transaction with a listed person';
update lov_value set label = 'No apparent economic purpose'
where type_code = 'SCR_STR_REASON' and label = 'Placeholder: no apparent economic purpose';
update lov_value set label = 'Amount not commensurate with the profile'
where type_code = 'SCR_STR_REASON' and label = 'Placeholder: amount not commensurate with the profile';

-- ---------- PAR08 version 2, released by package request PKR-2026-900006 ------------------------------
-- The request is released as version 2 of PAR08, which was never seeded (Open Version Editor failed).
-- Version 2 carries the request's terms from 01-Sep-2026; version 1 ends the day before.
update cat_product_version
set status = 'SUPERSEDED', effective_to = date '2026-08-31', updated_at = now(), updated_by = 'mbs'
where product_code = 'PAR08' and version_no = 1 and status = 'RELEASED' and effective_to is null
  and not exists (select 1 from cat_product_version where product_code = 'PAR08' and version_no = 2);

insert into cat_product_version (product_code, version_no, status, effective_from, package_start_date,
    package_end_date, anniversary_date, default_rate, minimum_premium, default_commission_rate,
    max_sum_insured, rating_basis_note, manual_rate_allowed, source_request_no, change_summary,
    submitted_by, submitted_at, validated_by, validated_at, created_at, created_by)
select 'PAR08', 2, 'RELEASED', date '2026-09-01', date '2026-09-01', date '2027-08-31', date '2027-09-01',
       0.25, 1000, 20, 20000000, 'Sum insured x 0.25%, minimum PHP 1,000', false, 'PKR-2026-900006',
       'Allied perils at the current rate', 'mbs', timestamptz '2026-08-05T10:00:00+08', 'tsu',
       timestamptz '2026-08-06T10:00:00+08', timestamptz '2026-08-05T10:00:00+08', 'mbs'
where not exists (select 1 from cat_product_version where product_code = 'PAR08' and version_no = 2)
  and exists (select 1 from pm_request where request_no = 'PKR-2026-900006');

insert into cat_package_coverage (version_id, coverage_code, included, optional, deductible_text, sort_order)
select v.id, c.code, true, false, c.ded_text, c.sort_order
from cat_product_version v
cross join (values
    ('FIRE_LIGHTNING', null, 10),
    ('EARTHQUAKE', '2% of value at risk', 20),
    ('TYPHOON_FLOOD', '1% of loss, minimum PHP 20,000', 30),
    ('RSMD', null, 40)
) as c(code, ded_text, sort_order)
where v.product_code = 'PAR08' and v.version_no = 2
  and not exists (select 1 from cat_package_coverage x where x.version_id = v.id);

insert into cat_package_insurer (version_id, company_id, insurer_code, role, share_percent, rate,
    minimum_premium, default_branch_code)
select v.id, co.id, 'INS-MGIC', 'LEAD', 100, 0.25, 1000, 'MKT'
from cat_product_version v
cross join org_company co
where v.product_code = 'PAR08' and v.version_no = 2 and co.code = 'FVI'
  and not exists (select 1 from cat_package_insurer x where x.version_id = v.id);

-- Incentive criteria copied from the booking rules took the placeholder text as their name.
update cat_incentive_criteria
set name = regexp_replace(name, '^Placeholder: ', '')
where name like 'Placeholder: %';
