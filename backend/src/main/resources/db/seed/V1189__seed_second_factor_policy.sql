-- =====================================================================================
-- iNXT BrokerVerse - V1189 seed (SIT, UAT and training stacks only): the second factor is not
-- asked of the SIT/UAT users by default, so the test scripts and the screen captures sign in with
-- the password alone. Testers switch MFA_POLICY to ALL or PRIVILEGED on the System Parameters
-- screen (second approval) to test the second factor. Production keeps the delivered PRIVILEGED.
-- =====================================================================================

update sys_parameter
   set param_value = 'OFF', updated_at = now(), updated_by = 'SYSTEM'
 where param_key = 'MFA_POLICY' and param_value = 'PRIVILEGED';
