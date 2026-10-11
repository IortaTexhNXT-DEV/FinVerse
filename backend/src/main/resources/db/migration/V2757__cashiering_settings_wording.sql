-- =====================================================================================
-- Wording of the Cashiering settings, accounting events and alerts as the users read them, and
-- the words of the amount printed on a receipt per currency and the company of the MFT files.
-- =====================================================================================

update sys_parameter
   set description = regexp_replace(description, '\s*\((FRS\.|Appendix R|BRQID\.)[^()]*\)', '', 'g')
 where param_key ~ '^(CASH_|ACCESS_)'
   and description ~ '\((FRS\.|Appendix R|BRQID\.)[^()]*\)';

update acc_event_type
   set description = regexp_replace(description, '\s*\(FRS\.[^()]*\)', '', 'g')
 where code in ('OPS_AR_INSURER_REFUND', 'OPS_COMMISSION_PAYMENT_HELD', 'OPS_AP_UNAPPLIED_COMMISSION')
   and description ~ '\(FRS\.[^()]*\)';

update alt_exception_code
   set description = regexp_replace(description, '\s*\(FRS\.[^()]*\)', '', 'g')
 where code = 'CASH_MFT_FILE_REFUSED'
   and description ~ '\(FRS\.[^()]*\)';

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CASH_AMOUNT_WORDS_UNITS', 'PHP=Pesos/Centavos;USD=US Dollars/Cents', 'STRING', 'OPERATIONS',
     'Words of the amount printed on a receipt per currency: CURRENCY=units/hundredths, separated by semicolons',
     null, null, now(), 'SYSTEM'),
    ('CASH_MFT_COMPANY_CODE', '', 'STRING', 'OPERATIONS',
     'Company that receives the payment files of the MFT folders (company code); blank = the first company',
     null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;
