-- Renewal lists, buckets, data scope and audit logs (BDOI Renewal FRS FRRN.001.02, FRRN.002.05,
-- FRRN.003.05, FRRN.043): the settings of the conflicts between BDOI's FRS and the delivered
-- behaviour, BDOI's behaviour by default.

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('RNW_PROCESSING_SCOPE', 'ALL', 'STRING', 'RENEWAL',
     'Renewals a Processing Officer may open: ALL (every renewal of the company; his worklist shows the renewals assigned to him) or ASSIGNED (only the renewals assigned to him); the Processing Team Lead always sees all',
     null, null, now(), 'SYSTEM'),
    ('RNW_THIRD_BUCKET', 'NON_RENEWABLE', 'STRING', 'RENEWAL',
     'Third panel of the renewal accounts: NON_RENEWABLE (Clean, Review, Non-Renewable and All; Exception accounts under Review) or EXCEPTION (Clean, Review, Exception and All)',
     null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;
