-- Seed data correction found by the Product Maintenance sign-off pack: a seeded workflow history
-- remark named the validator by login ("Version 2 validated by badmin"). Remarks written by the
-- system use the user's display name, as the running system writes them. Seed profile only; the
-- change is limited to seeded release remarks that end with a known login.
update wf_case_history h
set comment = left(h.comment, length(h.comment) - length(u.username)) || u.full_name
from sec_user u
where h.action = 'version_released'
  and h.comment like 'Version % validated by %'
  and h.comment = 'Version ' || split_part(substr(h.comment, 9), ' ', 1) || ' validated by ' || u.username
  and coalesce(u.full_name, '') <> '';
