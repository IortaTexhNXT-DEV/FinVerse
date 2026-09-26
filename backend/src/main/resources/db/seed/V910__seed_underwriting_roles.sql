-- =====================================================================================
-- SEED DATA (seed profile only): underwriting.
-- The seed has a single underwriter ("uw"); to show maker-checker on policies the
-- finance manager ("fmanager") acts as the underwriting approver in the seed company.
-- Products, policies, endorsements, quotations and the marine open cover are created at
-- start-up by UnderwritingSeedData through the services, so journals, open items and the
-- accounting event register are real.
-- =====================================================================================
insert into sec_role_permission (role_id, permission)
select r.id, p from sec_role r, unnest(array['POLICY_AUTHORIZE']) as p
where r.code = 'FIN_MANAGER'
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p);
