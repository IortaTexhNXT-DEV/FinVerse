-- =====================================================================================
-- iNXT BrokerVerse - V2020 Seed data of the product module switches (V1160).
-- SEED DATA ONLY - never load in production.
-- The insurer suite modules stay on in the SIT/UAT databases until they are removed, so the insurer
-- seed stories and their automated tests keep running as the SIT/UAT users (as V1961 does for their
-- permissions). A production database keeps the insurance broker profile of V1160.
-- =====================================================================================

update sys_product_module
set enabled = true,
    changed_reason = 'SIT/UAT: insurer seed stories and their tests run until the insurer modules are removed',
    changed_by = 'SYSTEM',
    changed_at = now()
where code in ('UNDERWRITING', 'INSURER_CLAIMS', 'REINSURANCE', 'ACTUARIAL_RESERVES', 'CONSOLIDATION',
               'INSURER_TAX')
  and enabled = false;
