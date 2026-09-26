-- =====================================================================================
-- iNXT BrokerVerse - V998 Seed Product Maintenance (BRD-3) users (seed profile only; password
-- for all users: Brokerverse@2026). SEED DATA ONLY - never load in production.
--   Personas of docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md section 6.2. The roles and their
--   grants are in V755. ao (MKT_AO), mkttl (MKT_TL), tsu (TSU) and badmin (BUSINESS_ADMIN) exist
--   since V980 and received their Product Maintenance grants through their roles.
--   tsulead keeps TSU and also receives TSU_TL. Two MBS users so that the package set-up and a
--   second maker can be shown. V996 / V997 hold the catalog and package request seed data.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('tsuhead', 'Tomas TSU Head',            'tsuhead@brokerverse-seed.ph'),
             ('mbs',     'Monica Business Services',  'mbs@brokerverse-seed.ph'),
             ('mbs2',    'Marco Business Services',   'mbs2@brokerverse-seed.ph'),
             ('mancom',  'Manuel ManCom Member',      'mancom@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('tsuhead', 'TSU_HEAD'), ('mbs', 'MBS'), ('mbs2', 'MBS'), ('mancom', 'MANCOM'),
             ('tsulead', 'TSU_TL'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
