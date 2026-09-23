-- =====================================================================================
-- iNXT FinVerse - V25 AGEING_BUCKETS becomes the effective default of the debtors and
-- creditors ageing reports (AgeingService.defaultSlots). Align the seeded value with the
-- slots those reports used so far (30/60/90/120 = 0-30, 31-60, 61-90, 91-120, Over 120), so
-- report output does not change. A value an administrator already changed is left alone.
-- =====================================================================================

update sys_parameter
set param_value = '30,60,90,120',
    description = 'Default ageing slots in days (up to 5 ascending limits) of the debtors and creditors ageing reports; users may override them per run'
where param_key = 'AGEING_BUCKETS'
  and param_value = '30,60,90,180'
  and updated_by is null;
