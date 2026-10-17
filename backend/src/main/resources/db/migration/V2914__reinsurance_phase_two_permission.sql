-- Reinsurance deferred to Phase 2 (client decision of 17-Oct-2026; Decision Log O-018). The reinsurance functions
-- the platform keeps - reinsurer and reinsurance broker parties, receipts from a reinsurer and reinsurance settlement
-- payments - need RI_BROKING_TRANSACT (party.service.ReinsuranceFunctions). No delivered role holds it: it is granted
-- through a User Access request when Phase 2 starts. The super user of DEV, SIT and UAT holds it (seed V2915).
insert into sec_permission_action (permission, area, action)
values ('RI_BROKING_TRANSACT', 'MASTER_DATA', 'CREATE'),
       ('RI_BROKING_TRANSACT', 'MASTER_DATA', 'AMEND')
on conflict (permission, action) do nothing;
