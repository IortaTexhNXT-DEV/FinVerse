-- =====================================================================================
-- iNXT BrokerVerse - V1332 Renewal: placement and booking of an accepted renewal under manual
-- control (Walkthrough addendum p.4, BRRN.035; FR-RN-084 R2, CLR-RN-34). The client's confirmation
-- is recorded through the Account Officer; the placement slip of the renewal account is generated
-- and sent by Processing, not automatically. The parameter keeps the switch for BDOI.
-- =====================================================================================
update sys_parameter
set param_value = 'false',
    description = 'Generate the placement slip automatically once an accepted renewal is ready for placement (off: Processing places the renewal account)'
where param_key = 'RNW_AUTO_PLACEMENT' and param_value = 'true';
