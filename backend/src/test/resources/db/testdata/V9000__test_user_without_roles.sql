-- Automated tests only (location db/testdata of application-test.yml): a user without any role, for
-- the tests that check that a missing permission is refused.
insert into sec_user (username, full_name, email, password_hash, home_branch_id, created_at,
    created_by)
select 'norole', 'Nora Norole', 'norole@brokerverse-test.ph',
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2',
       (select id from org_branch where code = 'HO' and company_id = (select id from org_company where code = 'FVI')),
       now(), 'SYSTEM'
where not exists (select 1 from sec_user where username = 'norole');
