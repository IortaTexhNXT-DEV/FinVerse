-- =====================================================================================
-- iNXT BrokerVerse - V2 Reference data required in every environment:
-- standard roles with their permissions, and ISO currencies.
-- The first administrator is created at start-up by AdminBootstrap from
-- BROKERVERSE_ADMIN_USERNAME / BROKERVERSE_ADMIN_INITIAL_PASSWORD (never hard-coded).
-- =====================================================================================

insert into sec_role (code, name, created_at, created_by) values
    ('SYSADMIN',        'System Administrator',            now(), 'SYSTEM'),
    ('FIN_ADMIN',       'Finance Administrator',           now(), 'SYSTEM'),
    ('FIN_MANAGER',     'Finance Manager',                 now(), 'SYSTEM'),
    ('ACCOUNTANT',      'Accountant (Maker)',              now(), 'SYSTEM'),
    ('AUTHORIZER',      'Accounts Authorizer (Checker)',   now(), 'SYSTEM'),
    ('BRANCH_FINANCE',  'Branch Finance User',             now(), 'SYSTEM'),
    ('UNDERWRITER',     'Underwriter',                     now(), 'SYSTEM'),
    ('CLAIMS_OFFICER',  'Claims Officer',                  now(), 'SYSTEM'),
    ('RI_OFFICER',      'Reinsurance Officer',             now(), 'SYSTEM'),
    ('AUDITOR',         'Auditor',                         now(), 'SYSTEM'),
    ('READ_ONLY',       'Read-Only User',                  now(), 'SYSTEM');

-- System administrator: security and system parameters only (segregation of duties:
-- administrators do not post or authorize financial transactions).
insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'USER_MANAGE','ROLE_MANAGE','SYSTEM_PARAMETER_MANAGE','AUDIT_VIEW','MASTER_VIEW',
    'REPORT_VIEW','DASHBOARD_VIEW']) as p
where code = 'SYSADMIN';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','MASTER_MAINTAIN','ACCOUNTING_RULE_MANAGE','PERIOD_MANAGE','JOURNAL_VIEW',
    'REPORT_VIEW','REPORT_FINANCIAL','DASHBOARD_VIEW','BUDGET_MANAGE']) as p
where code = 'FIN_ADMIN';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','MASTER_AUTHORIZE','JOURNAL_VIEW','JOURNAL_AUTHORIZE','JOURNAL_REVERSE',
    'PERIOD_MANAGE','YEAR_END_CLOSE','PERIOD_END_RUN','RECONCILIATION_MANAGE','BUDGET_MANAGE',
    'CONSOLIDATION_RUN','RECEIPT_PAYMENT_AUTHORIZE','POLICY_VIEW','CLAIM_VIEW','REINSURANCE_VIEW',
    'REPORT_VIEW','REPORT_FINANCIAL','DASHBOARD_VIEW','AUDIT_VIEW']) as p
where code = 'FIN_MANAGER';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','MASTER_MAINTAIN','JOURNAL_VIEW','JOURNAL_CREATE','RECEIPT_PAYMENT_MAINTAIN',
    'RECONCILIATION_MANAGE','POLICY_VIEW','CLAIM_VIEW','REINSURANCE_VIEW','REPORT_VIEW',
    'REPORT_FINANCIAL','DASHBOARD_VIEW']) as p
where code = 'ACCOUNTANT';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','MASTER_AUTHORIZE','JOURNAL_VIEW','JOURNAL_AUTHORIZE','JOURNAL_REVERSE',
    'RECEIPT_PAYMENT_AUTHORIZE','POLICY_VIEW','CLAIM_VIEW','REINSURANCE_VIEW','REPORT_VIEW',
    'REPORT_FINANCIAL','DASHBOARD_VIEW']) as p
where code = 'AUTHORIZER';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','JOURNAL_VIEW','JOURNAL_CREATE','RECEIPT_PAYMENT_MAINTAIN','POLICY_VIEW',
    'CLAIM_VIEW','REPORT_VIEW','DASHBOARD_VIEW']) as p
where code = 'BRANCH_FINANCE';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','POLICY_VIEW','POLICY_MAINTAIN','POLICY_AUTHORIZE','CLAIM_VIEW',
    'REINSURANCE_VIEW','REPORT_VIEW','DASHBOARD_VIEW']) as p
where code = 'UNDERWRITER';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','POLICY_VIEW','CLAIM_VIEW','CLAIM_MAINTAIN','CLAIM_AUTHORIZE',
    'REINSURANCE_VIEW','REPORT_VIEW','DASHBOARD_VIEW']) as p
where code = 'CLAIMS_OFFICER';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','POLICY_VIEW','CLAIM_VIEW','REINSURANCE_VIEW','REINSURANCE_MAINTAIN',
    'REPORT_VIEW','DASHBOARD_VIEW']) as p
where code = 'RI_OFFICER';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','JOURNAL_VIEW','POLICY_VIEW','CLAIM_VIEW','REINSURANCE_VIEW','AUDIT_VIEW',
    'REPORT_VIEW','REPORT_FINANCIAL','DASHBOARD_VIEW']) as p
where code = 'AUDITOR';

insert into sec_role_permission (role_id, permission)
select id, p from sec_role, unnest(array[
    'MASTER_VIEW','JOURNAL_VIEW','POLICY_VIEW','CLAIM_VIEW','REPORT_VIEW','DASHBOARD_VIEW']) as p
where code = 'READ_ONLY';

insert into cur_currency (code, name, symbol, decimal_places) values
    ('PHP', 'Philippine Peso',     '₱',   2),
    ('USD', 'US Dollar',           '$',   2),
    ('EUR', 'Euro',                '€',   2),
    ('GBP', 'Pound Sterling',      '£',   2),
    ('JPY', 'Japanese Yen',        '¥',   0),
    ('SGD', 'Singapore Dollar',    'S$',  2),
    ('HKD', 'Hong Kong Dollar',    'HK$', 2),
    ('AUD', 'Australian Dollar',   'A$',  2),
    ('CNY', 'Chinese Yuan',        '¥',   2),
    ('INR', 'Indian Rupee',        '₹',   2);
