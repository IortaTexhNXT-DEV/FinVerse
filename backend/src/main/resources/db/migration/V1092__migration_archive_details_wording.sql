-- =====================================================================================
-- iNXT BrokerVerse - V1092 Data Migration: business wording of the "other legacy fields" column of the
-- archive layout H01 (Layouts and Rules, the load templates of the console). The column held its texts in
-- technical terms; it now reads as the business user fills it in: label and value pairs separated by ;
-- (the load still accepts the object notation that system extracts produce). The column name stays: it is
-- the name of the file column.
-- =====================================================================================

update mig_layout_column set description = 'Other legacy fields of the record, each as a label and its value', format = 'Label: value pairs separated by ;', example = 'Insurer: MAL; Remitted on: 01-Jul-2022', target = 'Details of the archive record (Legacy Inquiry)', validation = 'Every pair has a label and a value' where layout_id = (select id from mig_layout where code = 'H01' and version_no = 1) and seq = 15;
