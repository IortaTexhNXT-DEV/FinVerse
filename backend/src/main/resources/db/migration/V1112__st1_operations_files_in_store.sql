-- =====================================================================================
-- iNXT BrokerVerse - V1112 Document storage, build step ST1: Operations tables.
--   docs/architecture/DOCUMENT_STORAGE_DECISION.md section 7. New files are written through the
--   file store (stored_file, V1100); each row keeps the id of its stored file and no longer the
--   bytes. The bytea columns stay (nullable) until FILE_BYTEA_MIGRATION has copied every row.
--     ops_extract_file    extract repository (remittance extracts, DP lists and billings, registers)
--     csh_print_batch     merged PDFs of receipt print batches
--     rem_batch_document  remittance schedules and payment requests
-- =====================================================================================

alter table ops_extract_file add column stored_file_id bigint references stored_file (id);
alter table ops_extract_file alter column content drop not null;

alter table csh_print_batch add column stored_file_id bigint references stored_file (id);

alter table rem_batch_document add column stored_file_id bigint references stored_file (id);
alter table rem_batch_document alter column content drop not null;
