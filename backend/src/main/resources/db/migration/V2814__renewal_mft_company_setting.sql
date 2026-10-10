-- The company that receives the files of the Renewal MFT inbox (placement and hold cover responses,
-- e-policies) is a setting, as for the payment files of Cashiering; blank means the first company.
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('RNW_MFT_COMPANY_CODE', '', 'STRING', 'RENEWAL',
     'Company that receives the files of the Renewal MFT inbox (company code); blank = the first company',
     null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;
