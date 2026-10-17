-- =====================================================================================
-- iNXT BrokerVerse - V1120 Catalog: decision of a rate-scheme exception (BRPM.007, FR-PM-051).
--   Product Maintenance extensions after its range V813-V819 is full (V1120-V1129).
--   The approver decides the exception on its record, opened from My Approvals: Approve with an
--   optional comment, or Reject with a reason. The decision is kept on the exception, next to
--   the requester (created_by) and the approver (authorized_by), so the quotation and the
--   requester's notice show who decided, when and why.
-- =====================================================================================

alter table cat_rate_scheme_exception add column decided_by varchar(50);
alter table cat_rate_scheme_exception add column decided_at timestamptz;
alter table cat_rate_scheme_exception add column decision_comment varchar(1000);
