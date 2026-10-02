-- =====================================================================================
-- iNXT BrokerVerse - V1113 Document storage, build step ST1: broking tables.
--   docs/architecture/DOCUMENT_STORAGE_DECISION.md section 7. New files are written through the
--   file store (stored_file, V1100); each row keeps the id of its stored file and no longer the
--   bytes. The bytea columns stay (nullable) until FILE_BYTEA_MIGRATION has copied every row.
--     plc_slip_file         placement slips (PDF, XLSX)
--     iss_upload_item       files of bulk e-policy uploads
--     iss_insurance_advice  Insurance Advice PDFs
--     bkg_service_invoice   service invoice PDFs (column document)
-- =====================================================================================

alter table plc_slip_file add column stored_file_id bigint references stored_file (id);
alter table plc_slip_file alter column content drop not null;

alter table iss_upload_item add column stored_file_id bigint references stored_file (id);
alter table iss_upload_item alter column content drop not null;

alter table iss_insurance_advice add column stored_file_id bigint references stored_file (id);
alter table iss_insurance_advice alter column content drop not null;

alter table bkg_service_invoice add column stored_file_id bigint references stored_file (id);
alter table bkg_service_invoice alter column document drop not null;
