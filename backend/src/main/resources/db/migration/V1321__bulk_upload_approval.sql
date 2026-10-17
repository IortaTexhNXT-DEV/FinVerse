-- =====================================================================================
-- iNXT BrokerVerse - V1321 Bulk uploads with maker-checker approval (the configuration uploads of
-- the configuration screens, docs/modules/CONFIG_PROMOTION.md section "Uploads of the configuration
-- screens"). A handler that names an approval permission keeps its validated upload until the
-- uploader submits it and a second user with that permission approves it; the valid rows are then
-- applied. Each valid row records whether it adds or updates a record (preview).
-- =====================================================================================

alter table bulk_job drop constraint ck_bulk_job_status;
alter table bulk_job add constraint ck_bulk_job_status
    check (status in ('VALIDATED', 'SUBMITTED', 'COMPLETED', 'CANCELLED', 'REJECTED'));

alter table bulk_job add column submitted_by  varchar(50);
alter table bulk_job add column submitted_at  timestamptz;
alter table bulk_job add column decided_by    varchar(50);
alter table bulk_job add column decided_at    timestamptz;
alter table bulk_job add column decision_note varchar(1000);

create index ix_bulk_job_submitted on bulk_job (status) where status = 'SUBMITTED';

alter table bulk_row add column action varchar(10);
alter table bulk_row add constraint ck_bulk_row_action check (action is null or action in ('ADD', 'UPDATE'));
