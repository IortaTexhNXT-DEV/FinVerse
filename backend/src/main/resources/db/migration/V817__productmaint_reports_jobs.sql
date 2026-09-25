-- =====================================================================================
-- iNXT BrokerVerse - V817 Product Maintenance (BRD-3) monitoring: the PACKAGE_EXPIRING exception
-- code of the daily PACKAGE_EXPIRY_MONITOR job and the parameters of the monitor and advisories.
--   BRPM.017  expiry / anniversary monitoring, notice at PACKAGE_EXPIRY_NOTICE_DAYS (V755) and
--             again at 30 and 7 days; renewal requests drafted when PACKAGE_RENEWAL_AUTODRAFT
--   BRPM.016  advisory recipient groups by default (PQ12)
--   BRPM.018/019  the reports PM-PKG-STATUS, PM-PKG-EXPIRY and PM-VERSION-HISTORY and the job are
--             code (ReportDefinition / ManagedJob beans); the job cron is
--             brokerverse.jobs.package-expiry-cron (application.yml).
--   Design: docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md section 8.
--   INCENTIVE_PRODUCT_INACTIVE (PMADD08) is raised by the catalog and seeded with it.
-- =====================================================================================

insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by)
select 'PACKAGE_EXPIRING', 'Package expiring',
       'A released package reaches its package end date within the notice period (PACKAGE_EXPIRY_NOTICE_DAYS, then 30 and 7 days) and has no renewal released yet (BRPM.017).',
       'PRODUCT_MAINTENANCE', 'MEDIUM', null, 60, now(), 'SYSTEM'
where not exists (select 1 from alt_exception_code x where x.code = 'PACKAGE_EXPIRING');

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('PACKAGE_EXPIRY_REMINDER_DAYS', '30,7', 'INTEGER_LIST', 'PRODUCT_MAINTENANCE',
     'Days before the package end date at which PACKAGE_EXPIRING is raised again after the first notice (BRPM.017, PQ11)', null, null, now(), 'SYSTEM'),
    ('PKG_ADVISORY_GROUPS', 'MARKETING,TSU,MBS,OPERATIONS', 'CODE_LIST', 'PRODUCT_MAINTENANCE',
     'Recipient groups (list PKG_ADVISORY_GROUP) proposed on a new package advisory (BRPM.016, PQ12)', null, null, now(), 'SYSTEM');
