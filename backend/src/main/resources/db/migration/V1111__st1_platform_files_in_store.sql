-- =====================================================================================
-- iNXT BrokerVerse - V1111 Document storage, build step ST1: platform tables.
--   docs/architecture/DOCUMENT_STORAGE_DECISION.md section 7. New files are written through the
--   file store (stored_file, V1100); each row keeps the id of its stored file. The bytea columns
--   stay until the job FILE_BYTEA_MIGRATION has copied every row and the reconciliation report is
--   signed off; a later migration drops them (not applied yet:
--   docs/architecture/st1/ST1_DROP_BYTEA_COLUMNS.sql).
--     doc_attachment           attachments (content was in doc_attachment_content)
--     report_run               archived report exports (content was in report_run_file)
--     report_batch             report batch files (ZIP or merged PDF)
--     msg_outbound_attachment  e-mail attachments as sent
-- =====================================================================================

alter table doc_attachment add column stored_file_id bigint references stored_file (id);

alter table report_run add column stored_file_id bigint references stored_file (id);

alter table report_batch add column stored_file_id bigint references stored_file (id);

alter table msg_outbound_attachment add column stored_file_id bigint references stored_file (id);
alter table msg_outbound_attachment alter column content drop not null;
