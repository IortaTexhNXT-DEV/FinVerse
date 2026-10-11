-- Every permission has its functional area and action class (User Access Matrix, PMADD05): the
-- accounting and finance permissions, the service fee permissions and the employee master were not
-- classified, so the matrix showed them without area and action class.
insert into sec_permission_action (permission, area, action)
select v.permission, v.area, v.action
from (values
    ('JOURNAL_VIEW', 'ACCOUNTING', 'VIEW'),
    ('JOURNAL_CREATE', 'ACCOUNTING', 'CREATE'),
    ('JOURNAL_ASSIGN', 'ACCOUNTING', 'AMEND'),
    ('JOURNAL_REVERSE', 'ACCOUNTING', 'AMEND'),
    ('JOURNAL_AUTHORIZE', 'ACCOUNTING', 'APPROVE'),
    ('ACCOUNTING_RULE_MANAGE', 'ACCOUNTING', 'CREATE'),
    ('ACCOUNTING_RULE_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('COA_UPLOAD', 'ACCOUNTING', 'CREATE'),
    ('PERIOD_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('PERIOD_END_RUN', 'ACCOUNTING', 'AMEND'),
    ('GL_CLOSE_SCHEDULE', 'ACCOUNTING', 'AMEND'),
    ('YEAR_END_CLOSE', 'ACCOUNTING', 'AMEND'),
    ('RECONCILIATION_MANAGE', 'ACCOUNTING', 'CREATE'),
    ('RECONCILIATION_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('RECEIPT_PAYMENT_MAINTAIN', 'ACCOUNTING', 'CREATE'),
    ('RECEIPT_PAYMENT_MAINTAIN', 'ACCOUNTING', 'AMEND'),
    ('RECEIPT_PAYMENT_AUTHORIZE', 'ACCOUNTING', 'APPROVE'),
    ('REVALUATION_RATE_MAINTAIN', 'ACCOUNTING', 'CREATE'),
    ('REVALUATION_RATE_MAINTAIN', 'ACCOUNTING', 'AMEND'),
    ('ASSET_MANAGE', 'ACCOUNTING', 'CREATE'),
    ('ASSET_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('BUDGET_MANAGE', 'ACCOUNTING', 'CREATE'),
    ('BUDGET_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('INVESTMENT_MANAGE', 'ACCOUNTING', 'CREATE'),
    ('INVESTMENT_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('TAX_VIEW', 'ACCOUNTING', 'VIEW'),
    ('TAX_MANAGE', 'ACCOUNTING', 'CREATE'),
    ('TAX_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('REPORT_FINANCIAL', 'ACCOUNTING', 'VIEW'),
    ('FRBS_REPORT_VIEW', 'ACCOUNTING', 'VIEW'),
    ('FRBS_REPORT_EXPORT', 'ACCOUNTING', 'VIEW'),
    ('SERVICE_FEE_MANAGE', 'ACCOUNTING', 'CREATE'),
    ('SERVICE_FEE_MANAGE', 'ACCOUNTING', 'AMEND'),
    ('SERVICE_FEE_TAG', 'ACCOUNTING', 'AMEND'),
    ('SERVICE_FEE_APPROVE', 'ACCOUNTING', 'APPROVE'),
    ('EMPLOYEE_MAINTAIN', 'ADMINISTRATION', 'CREATE'),
    ('EMPLOYEE_MAINTAIN', 'ADMINISTRATION', 'AMEND')
) as v (permission, area, action)
where not exists (select 1 from sec_permission_action x
                  where x.permission = v.permission and x.action = v.action);
