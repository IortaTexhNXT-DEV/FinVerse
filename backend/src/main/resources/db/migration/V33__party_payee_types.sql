-- =====================================================================================
-- iNXT BrokerVerse - V33 Party types of payees (BRD-5 DIS 2.2.2, wave A1-GL).
-- Employees, government agencies and other payees become party types (sub-ledger VENDOR), so
-- a Disbursement payee is always a maintained party. The party type is now checked.
-- =====================================================================================
alter table pty_party add constraint ck_party_type check (party_type in (
    'INDIVIDUAL_CLIENT', 'CORPORATE_CLIENT', 'AGENT', 'BROKER', 'REINSURER', 'RI_BROKER',
    'COINSURER', 'SUPPLIER', 'GARAGE', 'SURVEYOR', 'BANK', 'INSURER',
    'EMPLOYEE', 'GOVERNMENT', 'OTHER_PAYEE'));
