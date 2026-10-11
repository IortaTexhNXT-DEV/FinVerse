-- =====================================================================================
-- iNXT BrokerVerse - V1114 Document storage, build step ST1: disbursement and collections.
--   docs/architecture/DOCUMENT_STORAGE_DECISION.md section 7. New files are written through the
--   file store (stored_file, V1100); each row keeps the id of its stored file and no longer the
--   bytes. The bytea columns stay (nullable) until FILE_BYTEA_MIGRATION has copied every row.
--     dsb_eod_output        end-of-day outputs (bank files, printed forms, reports)
--     clx_billing_document  statement of account PDFs
-- =====================================================================================

alter table dsb_eod_output add column stored_file_id bigint references stored_file (id);
alter table dsb_eod_output alter column content drop not null;

alter table clx_billing_document add column stored_file_id bigint references stored_file (id);
alter table clx_billing_document alter column content drop not null;
