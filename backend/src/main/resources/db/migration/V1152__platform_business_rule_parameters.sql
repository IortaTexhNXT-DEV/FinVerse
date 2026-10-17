-- =====================================================================================
-- iNXT BrokerVerse - V1152 Business rules as parameters (range V1150-V1159). Terms and tolerances
-- that were written into the platform become business parameters, seeded with the values the
-- platform used until now, so each client sets its own on the Parameters screen. The input VAT
-- rate of supplier invoices is read from the tax code master (tax type VAT_INPUT), not from here.
-- =====================================================================================

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by)
select p.param_key, p.param_value, p.value_type, p.category, p.description, p.min_value,
       p.max_value, now(), 'SYSTEM'
from (values
    ('CLIENT_CREDIT_DAYS', '30', 'INTEGER', 'BROKING',
     'Credit days of the accounting party created for a client', 0, 365),
    ('SERVICE_INVOICE_CREDIT_DAYS', '30', 'INTEGER', 'BOOKING',
     'Credit days printed on a service invoice (payment terms merged into its template)', 0, 365),
    ('INSURER_DEFAULT_CREDIT_DAYS', '30', 'INTEGER', 'DATA_MIGRATION',
     'Credit days of an insurer loaded by data migration without credit days', 0, 365),
    ('PASSWORD_EXPIRY_NOTICE_DAYS', '7', 'INTEGER', 'SECURITY',
     'Days before a password expires from which the user is told to change it (LOCAL mode)', 1, 90),
    ('RATE_EXCEPTION_VALIDITY_DAYS', '30', 'INTEGER', 'BROKING',
     'Days a rate-scheme exception stays valid when the request gives no end date', 1, 3650),
    ('DP_PREMIUM_TOLERANCE', '1.00', 'DECIMAL', 'OPERATIONS',
     'Largest difference between the premium of a direct payment list and the booked gross premium that still passes the check',
     null, null),
    ('EWT_RATE_TOLERANCE', '0.05', 'DECIMAL', 'ACCOUNTING',
     'Largest difference in percentage points between the withholding rate of a document and the rate of the ATC before the EWT worksheet flags it',
     null, null),
    ('OPEN_COVER_TRANSIT_DAYS', '60', 'INTEGER', 'UNDERWRITING',
     'Days of transit of a shipment declared under an open cover when the declaration gives none', 1, 365)
) as p(param_key, param_value, value_type, category, description, min_value, max_value)
where not exists (select 1 from sys_parameter s where s.param_key = p.param_key);
