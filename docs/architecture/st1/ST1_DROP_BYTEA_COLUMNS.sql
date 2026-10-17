-- =====================================================================================
-- iNXT BrokerVerse - Document storage, build step ST1, step 3: drop the bytea columns.
--
-- NOT APPLIED. This file is kept under docs on purpose and is not in db/migration.
-- It is applied only after, in EVERY environment (DEV, SIT, UAT, pre-production, production, DR):
--   1. the job FILE_BYTEA_MIGRATION has run until nothing is left, and
--   2. the reconciliation report GET /api/v1/files/content-migration shows remaining = 0 for
--      every table and has been signed off (DOCUMENT_STORAGE_DECISION.md section 7).
-- To apply it: copy it to backend/src/main/resources/db/migration with the next free version
-- of the document storage range (V1115 to V1119, see DEVELOPER_GUIDE section 4), and in the
-- same change remove the bytea fields and the "kept before ST1" read paths from the entities
-- and services listed in section 7 (content, document, AttachmentContent, ReportRunFile).
--
-- The guard below stops the migration while any row still holds content without a stored file.
-- stored_file_id becomes NOT NULL only on the tables whose row is written after its file; the
-- other tables write the row first (its id is the owner key of the file) and set the column
-- in the same transaction.
-- =====================================================================================

do $$
declare
    left_over bigint;
begin
    select (select count(*) from doc_attachment_content c join doc_attachment a on a.id = c.attachment_id
             where a.stored_file_id is null)
         + (select count(*) from report_run_file f join report_run r on r.id = f.run_id
             where r.stored_file_id is null)
         + (select count(*) from report_batch where content is not null and stored_file_id is null)
         + (select count(*) from msg_outbound_attachment where content is not null and stored_file_id is null)
         + (select count(*) from ops_extract_file where content is not null and stored_file_id is null)
         + (select count(*) from csh_print_batch where content is not null and stored_file_id is null)
         + (select count(*) from rem_batch_document where content is not null and stored_file_id is null)
         + (select count(*) from plc_slip_file where content is not null and stored_file_id is null)
         + (select count(*) from iss_upload_item where content is not null and stored_file_id is null)
         + (select count(*) from iss_insurance_advice where content is not null and stored_file_id is null)
         + (select count(*) from bkg_service_invoice where document is not null and stored_file_id is null)
         + (select count(*) from dsb_eod_output where content is not null and stored_file_id is null)
         + (select count(*) from clx_billing_document where content is not null and stored_file_id is null)
      into left_over;
    if left_over > 0 then
        raise exception 'ST1: % rows still hold file content without a stored file; run FILE_BYTEA_MIGRATION first', left_over;
    end if;
end $$;

-- Platform
drop table doc_attachment_content;
drop table report_run_file;
alter table report_batch drop column content;
alter table msg_outbound_attachment drop column content;
alter table msg_outbound_attachment alter column stored_file_id set not null;

-- Operations
alter table ops_extract_file drop column content;
alter table csh_print_batch drop column content;
alter table rem_batch_document drop column content;

-- Broking
alter table plc_slip_file drop column content;
alter table plc_slip_file alter column stored_file_id set not null;
alter table iss_upload_item drop column content;
alter table iss_insurance_advice drop column content;
alter table bkg_service_invoice drop column document;
alter table bkg_service_invoice alter column stored_file_id set not null;

-- Disbursement and collections
alter table dsb_eod_output drop column content;
alter table clx_billing_document drop column content;
alter table clx_billing_document alter column stored_file_id set not null;
