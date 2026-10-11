-- Seed data correction found by the New Business sign-off pack: the onboarding history of the seeded clients
-- carried the remark "Seed onboarding history" on every step. Each step now carries the remark a user would write
-- for it. Seed profile only; only the seeded remark is replaced.
update wf_case_history h
set comment = case h.action
                when 'submit_kyc' then 'KYC form and valid ID uploaded'
                when 'verify_kyc' then 'KYC documents checked against the valid ID'
                when 'confirm' then 'Client details complete'
                when 'deactivate' then 'Duplicate of an existing client record'
                else h.comment
              end
from wf_case w
where w.id = h.case_id
  and w.workflow_code = 'NB_CLIENT'
  and h.comment = 'Seed onboarding history';
