-- =====================================================================================
-- iNXT BrokerVerse - V703 BIR books of accounts and new form worksheets (BRD-5 A1-FRBS)
--   FRBS 3.2.0, Appendix A VII (government returns and books), BDOI report list #18-#31;
--   docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 10 and 12.1. Formats and channels
--   of the returns and books are open (AQ07): these are worksheets and loose-leaf exports, not
--   eFPS / eBIRForms / CAS files.
--
--   * tax_book_def          the BIR books of accounts: general journal, purchase journal, sales
--                           journal, cash receipts and cash disbursements books (journal layout,
--                           selected by journal type and accounting event type) and the general
--                           ledger per account class (ledger layout)
--   * tax_form_output_line  the lines of the new form worksheets 0619-F, 1603, 1702-Q and 1702:
--                           ledger movements of account prefixes, sums of lines, a rate (system
--                           parameter) on a line, or the tax of the received certificates (SAWT)
--   * parameters TAX_RCIT_RATE, TAX_FBT_RATE, TAX_FBT_GROSSUP_RATE
--
-- The monthly and annual alphalists (MAP, 1604-E), the SAWT and the IC Broker's Annual Statement
-- of Business Operations are built from the EWT worksheet, the received-certificate register (V702)
-- and the Operations production register, so they need no configuration here.
-- =====================================================================================

create table tax_book_def (
    code                varchar(20)   primary key,
    name                varchar(120)  not null,
    layout              varchar(10)   not null
        constraint tax_ck_book_layout check (layout in ('JOURNAL', 'LEDGER')),
    journal_types       varchar(300),
    event_types         varchar(300),
    exclude_event_types varchar(300),
    description         varchar(300)  not null
);

insert into tax_book_def (code, name, layout, journal_types, event_types, exclude_event_types, description) values
 ('GJ', 'General Journal', 'JOURNAL', 'MANUAL,ADJUSTMENT,ACCRUAL,REVERSAL,PROVISION,REVALUATION,CLOSING,OPENING,CONSOLIDATION',
  null, null, 'Manual, adjustment, accrual, reversal and period-end entries (list #18)'),
 ('PJ', 'Purchase Journal', 'JOURNAL', null, 'SUPPLIER_INVOICE', null,
  'Supplier invoices booked in Payables (list #19)'),
 ('SJ', 'Sales Revenue Journal', 'JOURNAL', 'PREMIUM,ENDORSEMENT,COMMISSION', null, null,
  'Bookings, endorsements and commission entries (list #20)'),
 ('CRB', 'Cash Receipts Book', 'JOURNAL', 'RECEIPT', null, null,
  'Receipts: official and acknowledgement receipts, collections (list #21)'),
 ('CDB', 'Cash Disbursements Book', 'JOURNAL', 'PAYMENT', null, 'SUPPLIER_INVOICE',
  'Disbursement vouchers and other payments, without supplier invoices (list #22)'),
 ('SL', 'General Ledger', 'LEDGER', null, null, null,
  'General ledger of one account class: assets, liabilities, capital, income, expenses, contingent (list #23-#27)');

create table tax_form_output_line (
    form_code  varchar(20)   not null,
    line_no    integer       not null,
    label      varchar(200)  not null,
    kind       varchar(20)   not null
        constraint tax_ck_form_line_kind check (kind in ('ACCOUNT_CREDITS', 'ACCOUNT_DEBITS',
            'LINES', 'RATE', 'CERTIFICATES')),
    selector   varchar(300),
    operand    varchar(100),
    primary key (form_code, line_no)
);

insert into tax_form_output_line (form_code, line_no, label, kind, selector, operand) values
 -- 0619-F Monthly remittance of final income taxes withheld (final withholding tax account, AQ07)
 ('0619F', 10, 'Final income taxes withheld in the month', 'ACCOUNT_CREDITS', null, null),
 ('0619F', 20, 'Less: adjustments and remittances of the month', 'ACCOUNT_DEBITS', null, null),
 ('0619F', 30, 'Tax to remit', 'LINES', null, '10,-20'),
 -- 1603 Quarterly remittance of fringe benefit tax
 ('1603', 10, 'Monetary value of fringe benefits (employee benefits)', 'ACCOUNT_DEBITS', '5602', null),
 ('1603', 20, 'Grossed-up monetary value', 'RATE', null, '10:TAX_FBT_GROSSUP_RATE'),
 ('1603', 30, 'Fringe benefit tax due', 'RATE', null, '20:TAX_FBT_RATE'),
 -- 1702-Q Quarterly income tax return (year to date to the end of the quarter)
 ('1702Q', 10, 'Commission and service income', 'ACCOUNT_CREDITS', '4101,4110,4120,4130,4131,44', null),
 ('1702Q', 20, 'Other income', 'ACCOUNT_CREDITS', '4190,45,46,47', null),
 ('1702Q', 30, 'Total gross income', 'LINES', null, '10,20'),
 ('1702Q', 40, 'Less: operating expenses', 'ACCOUNT_DEBITS', '5,6', null),
 ('1702Q', 50, 'Taxable income', 'LINES', null, '30,-40'),
 ('1702Q', 60, 'Income tax due (regular corporate income tax)', 'RATE', null, '50:TAX_RCIT_RATE'),
 ('1702Q', 70, 'Less: creditable withholding tax per certificates received (SAWT)', 'CERTIFICATES', null, null),
 ('1702Q', 80, 'Tax payable / (overpayment)', 'LINES', null, '60,-70'),
 -- 1702 Annual income tax return
 ('1702', 10, 'Commission and service income', 'ACCOUNT_CREDITS', '4101,4110,4120,4130,4131,44', null),
 ('1702', 20, 'Other income', 'ACCOUNT_CREDITS', '4190,45,46,47', null),
 ('1702', 30, 'Total gross income', 'LINES', null, '10,20'),
 ('1702', 40, 'Less: operating expenses', 'ACCOUNT_DEBITS', '5,6', null),
 ('1702', 50, 'Taxable income', 'LINES', null, '30,-40'),
 ('1702', 60, 'Income tax due (regular corporate income tax)', 'RATE', null, '50:TAX_RCIT_RATE'),
 ('1702', 70, 'Less: creditable withholding tax per certificates received (SAWT)', 'CERTIFICATES', null, null),
 ('1702', 80, 'Tax payable / (overpayment)', 'LINES', null, '60,-70');

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('TAX_RCIT_RATE', '25', 'DECIMAL', 'TAX',
     'Regular corporate income tax rate in percent used by the 1702-Q / 1702 worksheets (AQ07)',
     null, null, now(), 'SYSTEM'),
    ('TAX_FBT_RATE', '35', 'DECIMAL', 'TAX',
     'Fringe benefit tax rate in percent used by the 1603 worksheet', null, null, now(), 'SYSTEM'),
    ('TAX_FBT_GROSSUP_RATE', '153.8462', 'DECIMAL', 'TAX',
     'Gross-up of the monetary value of fringe benefits in percent (100 / 65) used by the 1603 worksheet',
     null, null, now(), 'SYSTEM');
