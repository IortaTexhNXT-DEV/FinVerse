-- =====================================================================================
-- iNXT BrokerVerse - V1900 Demo Collections (BRD-4) core (demo profile only; password for all
-- users: Brokerverse@2026). DEMO ONLY.
--   * Users of the Collections personas (COLLECTIONS_DESIGN 6.2): clxhandler (Collection
--     Handler, MKT_COLLECTION), clxtl (Collection Team Lead, CLX_TL), clxuh (Section Head,
--     MKT_SECTION_HEAD) and mkthandler (Marketing Handler, MKT_HANDLER). mktcoll and mkttl
--     exist since V990 / V980.
--   * Unit Heads of the demo sales organisation (BRCLXN.011/012, CQ05; V819).
--   * Default assignment rules (BRCLXN.052): CBG accounts to clxhandler, corporate team accounts
--     to mkthandler, everything else to mktcoll.
--   The collection items themselves are not seeded: the demo invoices reach the ledger at
--   start-up (booking and Operations demo runners), and collections.demo.CollectionsDemoData
--   refreshes the worklist from them and records the dispositions, efforts, hand-offs,
--   reassignment and files of the storyline as the demo users.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('clxhandler', 'Clara Collection Handler', 'clxhandler@brokerverse-demo.ph'),
             ('clxtl',      'Carlo Collection Lead',    'clxtl@brokerverse-demo.ph'),
             ('clxuh',      'Ursula Section Head',      'clxuh@brokerverse-demo.ph'),
             ('mkthandler', 'Marco Marketing Handler',  'mkthandler@brokerverse-demo.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where x.username = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u join sec_role r on r.code = case u.username
    when 'clxhandler' then 'MKT_COLLECTION' when 'clxtl' then 'CLX_TL'
    when 'clxuh' then 'MKT_SECTION_HEAD' when 'mkthandler' then 'MKT_HANDLER' end
where u.username in ('clxhandler', 'clxtl', 'clxuh', 'mkthandler')
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

-- ---------- Unit Heads (BRCLXN.011/012) --------------------------------------------------------
update cat_sales_unit u
set head_username = h.head, updated_at = now(), updated_by = 'SYSTEM'
from (values ('T-CBG1', 'mkttl'), ('CBG-NCR', 'clxuh'), ('CORP-NCR', 'clxuh'), ('VIS', 'clxuh'))
     as h(code, head)
where u.code = h.code
  and u.company_id = (select id from org_company where code = 'FVI');

-- ---------- Default assignment rules (BRCLXN.052) ----------------------------------------------
insert into clx_assignment_rule (company_id, priority, name, segment, sales_unit, client_code,
    amount_from, amount_to, aging_from, aging_to, handler_username, active, created_at, created_by)
select c.id, r.priority, r.name, r.segment, r.unit, null, null, null, null, null, r.handler, true,
       now(), 'SYSTEM'
from org_company c
cross join (values (10, 'Consumer banking accounts', 'CBG', null, 'clxhandler'),
                   (20, 'Corporate team accounts', null, 'T-CORP1', 'mkthandler'),
                   (90, 'All other accounts', null, null, 'mktcoll'))
     as r(priority, name, segment, unit, handler)
where c.code = 'FVI';
