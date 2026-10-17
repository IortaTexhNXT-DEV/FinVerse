-- =====================================================================================
-- iNXT BrokerVerse - V822 Accounts: business type (New Business / Renewal), the reference of the
-- policy an account renews and the origin of the account. Shared work item BT0 (cross-BRD decision
-- D1, docs/requirements/BDOI_CROSS_BRD_DECISIONS.md section 6.1), used by Renewal (BRD-6),
-- Employee Benefits (BRD-8) and Submitted Policies (BRD-12); built once, in the account range.
--   BRNB.097        NB / Renewal classification required at creation and a report filter
--   BRID-022.01     business type on the policy / transaction and a filter on every report (EB)
--   BRRN.033        renewal accounts created as RENEWAL with the expiring reference (Renewal)
--   BRIDSP-26/27    renewal of a submitted policy: origin SUBMITTED_POLICY (Submitted Policies)
-- business_type    NEW_BUSINESS (default: every account created so far and every existing
--                  NewAccount factory) or RENEWAL (NewAccount.renewal(...)). Booking copies it to
--                  bkg_invoice.business_type and to the InvoiceBooked event.
-- renewal_of_ref   what a RENEWAL account renews: the expiring ARN (BIBS policies, EB programme
--                  lines), the SBM number (submitted policies) or the legacy reference; null for
--                  new business.
-- origin           how the account was created: QUOTATION, PROPOSAL (PRF), DIRECT (screen, bulk
--                  upload), SUBMITTED_POLICY, EMPLOYEE_BENEFITS or RENEWAL. Existing rows are
--                  derived from their quotation and proposal references.
--   Design: EMPLOYEE_BENEFITS_DESIGN section 11, SUBMITTED_POLICIES_DESIGN section 9,
--   RENEWAL_DESIGN section 2.3.
-- =====================================================================================
alter table acc_account add column business_type varchar(20) not null default 'NEW_BUSINESS';
alter table acc_account add column renewal_of_ref varchar(40);
alter table acc_account add column origin varchar(30) not null default 'DIRECT';

update acc_account set origin = 'QUOTATION' where quotation_ref is not null;
update acc_account set origin = 'PROPOSAL' where quotation_ref is null and proposal_ref is not null;

alter table acc_account add constraint ck_acc_business_type
    check (business_type in ('NEW_BUSINESS', 'RENEWAL'));
alter table acc_account add constraint ck_acc_origin
    check (origin in ('QUOTATION', 'PROPOSAL', 'DIRECT', 'SUBMITTED_POLICY', 'EMPLOYEE_BENEFITS',
                      'RENEWAL'));
alter table acc_account add constraint ck_acc_renewal_of
    check (business_type = 'RENEWAL' or renewal_of_ref is null);

create index ix_acc_account_business_type on acc_account (company_id, business_type, status);
create index ix_acc_account_renewal_of on acc_account (renewal_of_ref);
