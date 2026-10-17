-- =====================================================================================
-- iNXT BrokerVerse - V502 Payables changes for Disbursement (BRD-5, wave A1-DSB)
--   DIS 2.24.2  BDOIR bank accounts are tagged active / inactive under maker-checker: the
--               requested status waits for authorisation; inactive accounts are refused for
--               new payments (payables.domain.BankAccount).
--   DIS 2.23.2  The beginning check series of a cheque book may be edited while no leaf is used;
--               the edit is audited on the book (edited_by / edited_at / previous range).
--   DIS 2.16.1  New notification format DCTF (Direct Credit Transaction File, Appendix B p.147)
--               for the credit-to-account payments forwarded to TPD for ACA processing (AQ09).
-- Platform range (V500-V549); no reference to broking tables.
-- =====================================================================================

alter table pay_bank_account
    add column status           varchar(20) not null default 'ACTIVE',
    add column requested_status varchar(20);

alter table pay_bank_account
    add constraint ck_bank_account_status check (status in ('ACTIVE', 'INACTIVE')),
    add constraint ck_bank_account_requested_status
        check (requested_status is null or requested_status in ('ACTIVE', 'INACTIVE'));

alter table pay_bank_account drop constraint if exists pay_bank_account_notification_format_check;
alter table pay_bank_account
    add constraint ck_bank_account_notification_format
        check (notification_format in ('FIXED_WIDTH', 'CSV', 'DCTF'));

alter table pay_cheque_book
    add column edited_by      varchar(50),
    add column edited_at      timestamptz,
    add column previous_range varchar(60);
