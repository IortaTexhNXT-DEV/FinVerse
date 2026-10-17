-- Supplementary service-invoice batches (Sprint 3, G09): a service invoice issued after the day's
-- batch of its insurer goes into the next daily run, or into a supplementary batch that a booking
-- user runs for the date; batches of one company, insurer and date are numbered (1 = the daily
-- batch, 2 and up = supplementary batches).
alter table bkg_si_batch add column sequence_no integer not null default 1;
alter table bkg_si_batch drop constraint uq_bkg_si_batch;
alter table bkg_si_batch add constraint uq_bkg_si_batch
    unique (company_id, insurer_code, batch_date, sequence_no);
alter table bkg_si_batch add constraint ck_bkg_si_batch_sequence check (sequence_no >= 1);
