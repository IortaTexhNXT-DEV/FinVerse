-- File names of the exports in BDOI's convention <name>_MMDDYYYY (User Access Maintenance conflict
-- C18; Product Maintenance conflict C23). Default: BDOI's FRS.
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('EXPORT_FILE_NAMES_BDOI', 'true', 'BOOLEAN', 'REPORTING',
     'true: the exported audit logs, user access reports and Product Maintenance files are named <name>_MMDDYYYY with the extraction date, for example Audit Logs_10092026.xlsx (BDOI''s FRS); false: they are named by the report code',
     null, null, now(), 'SYSTEM');
