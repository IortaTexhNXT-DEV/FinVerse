-- =====================================================================================
-- iNXT BrokerVerse - V704 Tax & Statutory: final taxes, percentage tax and the tax status
-- of the parties (BDOI inputs v1.1, questions TX-Q06 and TX-Q09; templates D0-10 and TX-01).
--   tax_code           three more kinds of tax: FWT (final withholding tax, one code per ATC:
--                      government withholding on payments to the company, final tax on bank
--                      interest), FINAL_VAT (final VAT withheld by government payors) and
--                      PERCENTAGE_TAX (percentage tax on the company's own receipts)
--   tax_party_profile  withholding agent, top withholding agent and government payor flags, and
--                      the tax exemption certificate with its validity
-- =====================================================================================
alter table tax_code drop constraint tax_ck_code_type;
alter table tax_code add constraint tax_ck_code_type check (tax_type in ('VAT_OUTPUT', 'VAT_INPUT',
    'VAT_ZERO_RATED', 'VAT_EXEMPT', 'PREMIUM_TAX', 'DST', 'LGT', 'FST', 'EWT', 'FWT', 'FINAL_VAT',
    'PERCENTAGE_TAX'));
alter table tax_code drop constraint tax_ck_code_atc;
alter table tax_code add constraint tax_ck_code_atc
    check (tax_type not in ('EWT', 'FWT') or atc is not null);

alter table tax_party_profile add column withholding_agent boolean not null default false;
alter table tax_party_profile add column top_withholding_agent boolean not null default false;
alter table tax_party_profile add column government_payor boolean not null default false;
alter table tax_party_profile add column exemption_certificate_no varchar(40);
alter table tax_party_profile add column exemption_valid_from date;
alter table tax_party_profile add column exemption_valid_to date;
alter table tax_party_profile add constraint tax_ck_profile_agent
    check (withholding_agent or not (top_withholding_agent or government_payor));
alter table tax_party_profile add constraint tax_ck_profile_exemption
    check (exemption_certificate_no is null
        or (exemption_valid_from is not null and exemption_valid_to is not null
            and exemption_valid_to >= exemption_valid_from));
