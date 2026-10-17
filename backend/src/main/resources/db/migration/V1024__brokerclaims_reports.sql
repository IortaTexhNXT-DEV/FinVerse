-- =====================================================================================
-- iNXT BrokerVerse - V1024 Claims Handling (BRD-7) report support: indexes for the ageing,
-- settled, location and loss reports, and the shared report variants of the Claims Unit.
--   Requirements: docs/requirements/BDOI_CLM_BRD_SPEC.md
--     BRCLM.025-028/031  outstanding and ageing lists by reported date and status (p.42-43)
--     BRCLM.029          settled claims by date settled (p.42)
--     BRCLM.030/032      loss experience and loss ratio per cover and policy year (p.43-44)
--     BRCLM.033          saved report variants for every Claims report (Q40)
--     BRCLM.038          claims per location key, city and province (CLQ16)
--   Design: docs/architecture/CLAIMS_BROKING_DESIGN.md sections 4 and 10 (wave CL1-B).
-- =====================================================================================

create index ix_bcl_claim_reported on bcl_claim (company_id, reported_date);
create index ix_bcl_claim_settled on bcl_claim (company_id, date_settled) where phase = 'CLOSED';
create index ix_bcl_claim_loss on bcl_claim (company_id, loss_date, catastrophe_code);
create index ix_bcl_claim_client on bcl_claim (company_id, client_code, arn, policy_year);
create index ix_bcl_claim_location_claim on bcl_claim_location (claim_id);
create index ix_bcl_insurer_claim_claim on bcl_insurer_claim (claim_id);
create index ix_bcl_status_history_time on bcl_status_history (changed_at);
-- The loss ratio reads the ledger premium through the existing ix_ops_invoice_arn (V761).

-- ---------- Shared report variants (design section 10; nbreport ReportVariantService) --------
-- Visible to every user who may run the report; a user saves his own variants on the report.
insert into nbr_report_variant (owner, report_code, name, parameters, shared, created_at, created_by)
values ('SYSTEM', 'BCL-AGEING', 'Outstanding by insurer', '{}', true, now(), 'SYSTEM'),
       ('SYSTEM', 'BCL-OUTSTANDING-PAST-DUE', 'Past due 90 days', '{"days":"90"}', true, now(), 'SYSTEM');
