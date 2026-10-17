-- =====================================================================================
-- iNXT BrokerVerse - V2752 Cashiering: BDOI's payment files (Operations Cashiering FRS v3.2:
-- FRS.CSH.05.01, 02.02.03 to 02.02.09; Appendix D).
--   csh_channel_profile    one row per payment file type: the file name convention, the format
--                          (separator, header and control records, TOTAL row, date format), the
--                          map of BDOI's fields to the fields of the system, the size limit and the
--                          MFT folder with its switch; changed in Cashiering Setup, no release
--   csh_channel_file       every file received (upload or MFT) with its checks, its raw copy (kept
--                          read-only), the upload reference, the source file, the uploader and the
--                          processing time, the bulk run that processed it and its outcome
--   csh_legacy_reference   legacy numbers of the accounts migrated at go-live (QPS, ISYS, Ebix, PN,
--                          Loan Application Number) used as match keys (FRS.CSH.05.01.08)
-- =====================================================================================

create table csh_channel_profile (
    file_type          varchar(30)   primary key,
    version            bigint        not null default 0,
    name               varchar(80)   not null,
    handler_code       varchar(40)   not null,
    name_pattern       varchar(200)  not null,
    name_example       varchar(80)   not null,
    file_format        varchar(10)   not null check (file_format in ('TEXT', 'EXCEL', 'ANY')),
    delimiter          varchar(5)    not null,
    header_line        boolean       not null,
    header_record      varchar(5),
    detail_record      varchar(5),
    total_row_label    varchar(20),
    date_format        varchar(20)   not null,
    field_map          varchar(1000) not null,
    control_map        varchar(200),
    max_mb             integer       not null default 50 check (max_mb between 1 and 500),
    mft_folder         varchar(300),
    mft_enabled        boolean       not null default false,
    source_system      varchar(30)   not null,
    updated_at         timestamptz,
    updated_by         varchar(50)
);

insert into csh_channel_profile (file_type, name, handler_code, name_pattern, name_example, file_format,
    delimiter, header_line, header_record, detail_record, total_row_label, date_format, field_map,
    control_map, max_mb, mft_folder, mft_enabled, source_system) values
 ('BILLS_PAYMENT', 'Bills Payment', 'PAY_BILLS', '(?i)^BDOI\d{8,9}\.txt$', 'BDOI201506181.TXT', 'TEXT', '|',
  false, '1', '2', null, 'MM/dd/yyyy',
  'Incremental #=2;Invoice #=3;Assured''s name=4;Amount=5;Late deposit (Y/N)=6;Phone #=9;Branch code=14;Date of payment=15;Time of payment=16',
  'count=2;total=3;date=4', 50, 'mft/inbound/obpcs', false, 'OBPCS'),
 ('PDC', 'Post-dated checks (PMS)', 'PAY_PDC', '(?i)^BDOI\d{8,9}PMS\.txt$', 'BDOI201506181PMS.TXT', 'TEXT', '|',
  true, null, null, null, 'MM/dd/yyyy',
  'Client code=Client code;Payor=Payor;Reference=Reference;Check number=Check number;Bank code=Bank code;Check branch=Check branch;Maturity date=Maturity date;Amount=Amount;Market segment=Market segment',
  null, 50, 'mft/inbound/pms', false, 'PMS'),
 ('CLPC', 'CLPC', 'PAY_CLPC', '(?i)^CLPC-[A-Za-z]{3}\d{6}\.xlsx$', 'CLPC-Jun032025.xlsx', 'EXCEL', 'AUTO',
  true, null, null, 'TOTAL', 'MM/dd/yyyy',
  'Count=COUNT;Date credit=DATE CREDIT;Amount=AMOUNT;AR number=AR NUMBER;Invoice=INVOICE;Assured=ASSURED;PN number=PN NUMBER;Corp. dept=CORP.DEPT;Risk code=RISK CODE;Remarks=REMARKS',
  null, 50, null, false, 'CLPC'),
 ('TRADE', 'Trade', 'PAY_TRADE', '(?i)^UploadTrade_\d{2}\.\d{2}\.\d{4}\.txt$', 'UploadTrade_01.01.2025.TXT', 'TEXT', 'AUTO',
  true, null, null, null, 'MM/dd/yyyy',
  'Transaction date=Transaction Date;Branch name=Branch Name;Transaction description=Transaction Description;Debit=Debit;Credit=Credit;Running balance=Running Balance;Check no.=Check No.',
  null, 50, null, false, 'TFS'),
 ('DIRECT_CREDIT', 'Direct Credit', 'PAY_DIRECT_CREDIT', '(?i)^OTC\d{8,9}\s*-\s*[A-Za-z0-9 ]+\.(txt|xlsx)$', 'OTC202506101- AUTOCREDIT.TXT', 'ANY', '|',
  true, null, null, null, 'MM/dd/yyyy',
  'Transaction date=Transaction date;BP filename=BP filename;Transaction no=Transaction no;Paid amount=Paid amount;Payment type=Payment type;Payor=Payor;Account ref no=Account ref no;Assured=Assured;EBIX_RefNo=Ebix reference no.;Logged by=Logged by;Requestor=Requestor',
  null, 50, null, false, 'DIRECT_CREDIT'),
 ('COMMISSION_SCHEDULE', 'Commission Schedule', 'COMMISSION_PAYMENT', '(?i)^.+\.csv$', 'CommissionSchedule_10092026.csv', 'TEXT', '|',
  true, null, null, null, 'MM/dd/yyyy',
  'Insurer code=Insurer code;Payee name=Payee name;Certificate ref=Certificate ref;Payment ref=Payment ref;Invoice no=Invoice no;Basic commission=Basic commission;VAT=VAT;WTAX=Withholding tax;Payment date=Payment date',
  null, 50, null, false, 'INSURER')
on conflict (file_type) do nothing;

create table csh_channel_file (
    id                bigint generated by default as identity primary key,
    version           bigint         not null default 0,
    company_id        bigint         not null references org_company (id),
    upload_ref        varchar(40)    not null,
    file_type         varchar(30)    not null references csh_channel_profile (file_type),
    file_name         varchar(250)   not null,
    file_size         bigint         not null,
    sha256            varchar(64)    not null,
    source            varchar(10)    not null check (source in ('UPLOAD', 'MFT')),
    status            varchar(20)    not null check (status in ('RECEIVED', 'REFUSED', 'PROCESSED', 'PARTIAL')),
    message           varchar(1000),
    stored_file_id    bigint,
    bulk_job_id       bigint,
    bulk_job_no       varchar(40),
    rows_read         integer        not null default 0,
    rows_failed       integer        not null default 0,
    header_count      integer,
    header_total      numeric(19, 2),
    detail_total      numeric(19, 2),
    uploaded_by       varchar(50)    not null,
    received_at       timestamptz    not null,
    processed_at      timestamptz,
    created_at        timestamptz    not null,
    created_by        varchar(50)    not null,
    updated_at        timestamptz,
    updated_by        varchar(50),
    constraint uq_csh_channel_file_ref unique (company_id, upload_ref)
);
create index ix_csh_channel_file_type on csh_channel_file (company_id, file_type, status);
create index ix_csh_channel_file_sha on csh_channel_file (company_id, file_type, sha256);
create index ix_csh_channel_file_name on csh_channel_file (company_id, lower(file_name));

create table csh_legacy_reference (
    id            bigint generated by default as identity primary key,
    version       bigint        not null default 0,
    company_id    bigint        not null references org_company (id),
    ref_type      varchar(20)   not null check (ref_type in ('QPS', 'ISYS', 'EBIX', 'PN', 'LOAN_APPLICATION')),
    ref_no        varchar(60)   not null,
    account_ref   varchar(60)   not null,
    created_at    timestamptz   not null,
    created_by    varchar(50)   not null,
    updated_at    timestamptz,
    updated_by    varchar(50),
    constraint uq_csh_legacy_reference unique (company_id, ref_type, ref_no)
);
create index ix_csh_legacy_reference_no on csh_legacy_reference (company_id, lower(ref_no));

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CASH_DIRECT_CREDIT_MODE', 'IDENTIFY', 'STRING', 'OPERATIONS',
     'Direct Credit file: IDENTIFY = it identifies payments already received (BP filename and transaction number); NEW_PAYMENT = every row is a new payment with its own AR (Appendix R, C8)',
     null, null, now(), 'SYSTEM'),
    ('CASH_COMMISSION_SCHEDULE_MODE', 'LINE_LEVEL', 'STRING', 'OPERATIONS',
     'Commission Schedule: LINE_LEVEL = valid lines are processed (BRQID.006); WHOLE_FILE = a file with a failed or unmatched line, or a paid amount different from the outstanding, is not processed (Appendix R, C10)',
     null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- The alert of a payment file received via MFT and refused (FRS.CSH.10.04.02; Appendix G).
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('CASH_MFT_FILE_REFUSED', 'Payment file refused',
     'A payment file received via MFT failed the file checks (name, size, duplicate, header or TOTAL control) and was not processed (FRS.CSH.05.01.04).',
     'CASHIERING', 'HIGH', null, null, now(), 'SYSTEM')
on conflict (code) do nothing;
