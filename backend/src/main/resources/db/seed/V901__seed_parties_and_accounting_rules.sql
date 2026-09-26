-- =====================================================================================
-- SEED DATA (seed profile only): business partners and accounting rules for the seed
-- chart of accounts. In production, rules are configured and authorized in the Accounting
-- Rules screen against the client's own chart of accounts.
-- =====================================================================================
insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, commission_rate, withholding_tax_rate, licence_no,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, p.code, p.name, p.t, p.tin, p.addr, p.email, p.phone, p.ccy, p.days, p.comm, p.wht, p.lic,
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('C-000101','Juan Dela Cruz','INDIVIDUAL_CLIENT','123-456-789-000','Quezon City','juan@example.ph','+63 917 000 0101','PHP',30,null::numeric,null::numeric,null),
  ('C-000102','Maria Clara Santos','INDIVIDUAL_CLIENT','223-456-789-000','Pasig City','maria@example.ph','+63 917 000 0102','PHP',30,null,null,null),
  ('C-000201','Luzon Steel Manufacturing Corp.','CORPORATE_CLIENT','301-222-333-000','Calamba, Laguna','finance@luzonsteel.example','+63 49 555 0201','PHP',60,null,null,null),
  ('C-000202','Visayas Shipping Lines Inc.','CORPORATE_CLIENT','302-222-333-000','Cebu City','ap@vsl.example','+63 32 555 0202','USD',60,null,null,null),
  ('C-000203','Mindanao Agri Ventures Inc.','CORPORATE_CLIENT','303-222-333-000','Davao City','acct@mav.example','+63 82 555 0203','PHP',45,null,null,null),
  ('C-000204','Metro Retail Holdings Corp.','CORPORATE_CLIENT','304-222-333-000','Makati City','treasury@mrh.example','+63 2 555 0204','PHP',60,null,null,null),
  ('A-0001','Rosa Mendoza Insurance Agency','AGENT','401-111-222-000','Makati City','rosa@agency.example','+63 917 100 0001','PHP',15,15.0,10.0,'IC-AG-2020-0001'),
  ('A-0002','Pedro Lim Insurance Agent','AGENT','402-111-222-000','Cebu City','pedro@agent.example','+63 917 100 0002','PHP',15,12.5,10.0,'IC-AG-2019-0452'),
  ('B-0001','Pacific Insurance Brokers Inc.','BROKER','501-111-222-000','BGC, Taguig','ops@pacificbrokers.example','+63 2 777 0001','PHP',30,20.0,10.0,'IC-BR-2015-0031'),
  ('B-0002','Asia Risk Advisory Brokers','BROKER','502-111-222-000','Ortigas, Pasig','placing@arab.example','+63 2 777 0002','USD',30,17.5,10.0,'IC-BR-2017-0077'),
  ('R-0001','National Reinsurance Corp. (Seed)','REINSURER','601-111-222-000','Makati City','ri@natre.example','+63 2 888 0001','PHP',90,null,null,null),
  ('R-0002','Asia Pacific Re (Seed)','REINSURER',null,'Singapore','ri@apre.example','+65 6000 0002','USD',90,null,null,null),
  ('R-0003','Global Specialty Re (Seed)','REINSURER',null,'London','ri@gsre.example','+44 20 0000 0003','USD',90,null,null,null),
  ('CO-0001','Philippine Mutual Assurance (Seed)','COINSURER','701-111-222-000','Manila','coins@pma.example','+63 2 666 0001','PHP',60,null,null,null),
  ('S-0001','Metro Office Supplies Co.','SUPPLIER','801-111-222-000','Mandaluyong City','billing@mos.example','+63 2 444 0001','PHP',30,null,2.0,null),
  ('S-0002','Cloud Systems Philippines Inc.','SUPPLIER','802-111-222-000','Taguig City','ar@cloudsys.example','+63 2 444 0002','PHP',30,null,2.0,null),
  ('S-0003','Ayala Property Leasing (Seed)','SUPPLIER','803-111-222-000','Makati City','leasing@apl.example','+63 2 444 0003','PHP',30,null,5.0,null),
  ('G-0001','AutoFix Service Center','GARAGE','901-111-222-000','Quezon City','claims@autofix.example','+63 2 333 0001','PHP',30,null,2.0,null),
  ('G-0002','Cebu Motor Works','GARAGE','902-111-222-000','Cebu City','service@cmw.example','+63 32 333 0002','PHP',30,null,2.0,null)
) as p(code, name, t, tin, addr, email, phone, ccy, days, comm, wht, lic)
where c.code = 'FVI';

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'POLICY_ISSUE', 'Standard policy issue', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1201', 'TOTAL_DUE', true),
  (2, 'CREDIT', '4100', 'GROSS_PREMIUM', false),
  (3, 'CREDIT', '2503', 'DST', false),
  (4, 'CREDIT', '2504', 'VAT', false),
  (5, 'CREDIT', '2505', 'LGT', false),
  (6, 'CREDIT', '2506', 'FST', false),
  (7, 'CREDIT', '2507', 'PREMIUM_TAX', false),
  (8, 'CREDIT', '4700', 'POLICY_FEE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'POLICY_ISSUE';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'POLICY_ENDORSEMENT', 'Standard policy endorsement', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1201', 'TOTAL_DUE', true),
  (2, 'CREDIT', '4100', 'GROSS_PREMIUM', false),
  (3, 'CREDIT', '2503', 'DST', false),
  (4, 'CREDIT', '2504', 'VAT', false),
  (5, 'CREDIT', '2505', 'LGT', false),
  (6, 'CREDIT', '2506', 'FST', false),
  (7, 'CREDIT', '2507', 'PREMIUM_TAX', false),
  (8, 'CREDIT', '4700', 'POLICY_FEE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'POLICY_ENDORSEMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'POLICY_CANCELLATION', 'Standard policy cancellation', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1201', 'TOTAL_DUE', true),
  (2, 'CREDIT', '4100', 'GROSS_PREMIUM', false),
  (3, 'CREDIT', '2503', 'DST', false),
  (4, 'CREDIT', '2504', 'VAT', false),
  (5, 'CREDIT', '2505', 'LGT', false),
  (6, 'CREDIT', '2506', 'FST', false),
  (7, 'CREDIT', '2507', 'PREMIUM_TAX', false),
  (8, 'CREDIT', '4700', 'POLICY_FEE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'POLICY_CANCELLATION';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'COMMISSION_ACCRUAL', 'Standard commission accrual', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '5400', 'COMMISSION', false),
  (2, 'CREDIT', '2300', 'NET_COMMISSION', true),
  (3, 'CREDIT', '2508', 'WITHHOLDING_TAX', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'COMMISSION_ACCRUAL';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'COINSURANCE_SHARE', 'Standard coinsurance share', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '4100', 'COINSURER_PREMIUM', false),
  (2, 'CREDIT', '2203', 'COINSURER_PREMIUM', true)
) as l(n, side, acc, comp, party)
where r.event_type = 'COINSURANCE_SHARE';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'PREMIUM_RECEIPT', 'Standard premium receipt', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@BANK', 'AMOUNT', false),
  (2, 'CREDIT', '1201', 'AMOUNT', true)
) as l(n, side, acc, comp, party)
where r.event_type = 'PREMIUM_RECEIPT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'PREMIUM_DEPOSIT', 'Standard premium deposit', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@BANK', 'AMOUNT', false),
  (2, 'CREDIT', '2205', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'PREMIUM_DEPOSIT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'PREMIUM_REFUND_PAYMENT', 'Standard premium refund payment', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1201', 'AMOUNT', true),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'PREMIUM_REFUND_PAYMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'COMMISSION_PAYMENT', 'Standard commission payment', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '2300', 'AMOUNT', true),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'COMMISSION_PAYMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'CLAIM_RESERVE', 'Standard claim reserve', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '5200', 'RESERVE_CHANGE', false),
  (2, 'CREDIT', '2102', 'RESERVE_CHANGE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'CLAIM_RESERVE';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'CLAIM_SETTLEMENT', 'Standard claim settlement', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '5100', 'PAID_AMOUNT', false),
  (2, 'CREDIT', '2204', 'PAID_AMOUNT', true),
  (3, 'DEBIT', '2102', 'RESERVE_RELEASE', false),
  (4, 'CREDIT', '5200', 'RESERVE_RELEASE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'CLAIM_SETTLEMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'CLAIM_PAYMENT', 'Standard claim payment', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '2204', 'AMOUNT', true),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'CLAIM_PAYMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'CLAIM_RECOVERY', 'Standard claim recovery', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@BANK', 'AMOUNT', false),
  (2, 'CREDIT', '5100', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'CLAIM_RECOVERY';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'RI_PREMIUM_CEDED', 'Standard ri premium ceded', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '4200', 'CEDED_PREMIUM', false),
  (2, 'CREDIT', '2201', 'NET_DUE', true),
  (3, 'CREDIT', '4400', 'RI_COMMISSION', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'RI_PREMIUM_CEDED';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'RI_CLAIM_RECOVERY', 'Standard ri claim recovery', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1205', 'RECOVERY', true),
  (2, 'CREDIT', '5300', 'RECOVERY', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'RI_CLAIM_RECOVERY';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'RI_RESERVE_SHARE', 'Standard ri reserve share', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1302', 'RESERVE_CHANGE', false),
  (2, 'CREDIT', '5300', 'RESERVE_CHANGE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'RI_RESERVE_SHARE';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'RI_SETTLEMENT_PAYMENT', 'Standard ri settlement payment', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '2201', 'AMOUNT', true),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'RI_SETTLEMENT_PAYMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'RI_SETTLEMENT_RECEIPT', 'Standard ri settlement receipt', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@BANK', 'AMOUNT', false),
  (2, 'CREDIT', '1205', 'AMOUNT', true)
) as l(n, side, acc, comp, party)
where r.event_type = 'RI_SETTLEMENT_RECEIPT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'UPR_PROVISION', 'Standard upr provision', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '4300', 'UPR_CHANGE', false),
  (2, 'CREDIT', '2101', 'UPR_CHANGE', false),
  (3, 'DEBIT', '1301', 'RI_UPR_CHANGE', false),
  (4, 'CREDIT', '4300', 'RI_UPR_CHANGE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'UPR_PROVISION';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'DAC_PROVISION', 'Standard dac provision', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '1400', 'DAC_CHANGE', false),
  (2, 'CREDIT', '5500', 'DAC_CHANGE', false),
  (3, 'DEBIT', '4400', 'DRC_CHANGE', false),
  (4, 'CREDIT', '2400', 'DRC_CHANGE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'DAC_PROVISION';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'IBNR_PROVISION', 'Standard ibnr provision', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '5200', 'IBNR_CHANGE', false),
  (2, 'CREDIT', '2103', 'IBNR_CHANGE', false),
  (3, 'DEBIT', '1303', 'RI_IBNR_CHANGE', false),
  (4, 'CREDIT', '5300', 'RI_IBNR_CHANGE', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'IBNR_PROVISION';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'TAKAFUL_SURPLUS', 'Standard takaful surplus', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '5613', 'SURPLUS', false),
  (2, 'CREDIT', '2502', 'SURPLUS', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'TAKAFUL_SURPLUS';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'SUPPLIER_INVOICE', 'Standard supplier invoice', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@EXPENSE', 'NET_AMOUNT', false),
  (2, 'DEBIT', '1603', 'INPUT_VAT', false),
  (3, 'CREDIT', '2501', 'PAYABLE', true),
  (4, 'CREDIT', '2508', 'WITHHOLDING_TAX', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'SUPPLIER_INVOICE';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'SUPPLIER_PAYMENT', 'Standard supplier payment', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '2501', 'AMOUNT', true),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'SUPPLIER_PAYMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'PETTY_CASH_EXPENSE', 'Standard petty cash expense', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@EXPENSE', 'AMOUNT', false),
  (2, 'CREDIT', '@PETTY_CASH', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'PETTY_CASH_EXPENSE';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'PETTY_CASH_REPLENISHMENT', 'Standard petty cash replenishment', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@PETTY_CASH', 'AMOUNT', false),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'PETTY_CASH_REPLENISHMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'MISC_RECEIPT', 'Standard misc receipt', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@BANK', 'AMOUNT', false),
  (2, 'CREDIT', '@INCOME', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'MISC_RECEIPT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'MISC_PAYMENT', 'Standard misc payment', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@EXPENSE', 'AMOUNT', false),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'MISC_PAYMENT';
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'BANK_TRANSFER', 'Standard bank transfer', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@TO_BANK', 'AMOUNT', false),
  (2, 'CREDIT', '@FROM_BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'BANK_TRANSFER';
