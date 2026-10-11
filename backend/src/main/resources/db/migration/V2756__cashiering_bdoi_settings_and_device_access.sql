-- =====================================================================================
-- Settings of the points the client decides (Appendix R): the base of the PR 2307 (C26) and the
-- use of the system from company-issued devices only (FRS.OPS.001), off by default.
-- =====================================================================================

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CASH_PR2307_BASE', 'PREMIUM', 'STRING', 'OPERATIONS',
     'Base of the PR 2307 of a 2% CWT account: PREMIUM = the premium amount (every premium receivable component), BASIC = the basic premium only (Appendix R, C26)',
     null, null, now(), 'SYSTEM'),
    ('ACCESS_DEVICE_RESTRICTION', 'OFF', 'STRING', 'SECURITY',
     'ON = the system is used only from company-issued devices (company networks or the header of the device-management gateway); OFF = any device (FRS.OPS.001)',
     null, null, now(), 'SYSTEM'),
    ('ACCESS_ALLOWED_NETWORKS', '', 'STRING', 'SECURITY',
     'Company networks accepted when the device restriction is ON: addresses or CIDR ranges separated by commas, e.g. 10.20.0.0/16',
     null, null, now(), 'SYSTEM'),
    ('ACCESS_DEVICE_HEADER', '', 'STRING', 'SECURITY',
     'Header the device-management gateway adds for a company-issued device, e.g. X-Managed-Device; blank = networks only',
     null, null, now(), 'SYSTEM'),
    ('ACCESS_DEVICE_HEADER_VALUE', '', 'STRING', 'SECURITY',
     'Value of the device header; blank = the header alone is enough',
     null, null, now(), 'SYSTEM');

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CASH_UNAPPLIED_MARKETING_SCOPE', 'OWN_UNIT', 'STRING', 'OPERATIONS',
     'What a Marketing user sees in the list of unapplied payments: OWN_UNIT = the records of his or her marketing unit, ALL = every record (Appendix R, C13)',
     null, null, now(), 'SYSTEM');
