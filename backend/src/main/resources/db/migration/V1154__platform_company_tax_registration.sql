-- =====================================================================================
-- iNXT BrokerVerse - V1154 Platform: tax registration of the company and its branches
-- (BDOI inputs v1.1, question TX-Q08; template D0-01 Company and D0-02 Branches).
--   org_company  Revenue District Office, VAT registration, the permit of the computerised
--                accounting system (CAS) and the e-invoicing permit or enrolment
--   org_branch   BIR branch code and Revenue District Office of each registered branch
-- The RDO of the company replaces the parameter TAX_RDO_CODE on the BIR exports once it is set.
-- =====================================================================================
alter table org_company add column rdo_code varchar(5)
    constraint org_ck_company_rdo check (rdo_code ~ '^[0-9]{3}[A-Z]?$');
alter table org_company add column vat_registered boolean not null default true;
alter table org_company add column cas_permit_no varchar(40);
alter table org_company add column cas_permit_date date;
alter table org_company add column einvoicing_permit_no varchar(40);
alter table org_company add column einvoicing_permit_date date;

alter table org_branch add column bir_branch_code varchar(5)
    constraint org_ck_branch_bir_code check (bir_branch_code ~ '^[0-9]{3,5}$');
alter table org_branch add column rdo_code varchar(5)
    constraint org_ck_branch_rdo check (rdo_code ~ '^[0-9]{3}[A-Z]?$');
