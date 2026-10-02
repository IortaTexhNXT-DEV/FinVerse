-- =====================================================================================
-- iNXT BrokerVerse - V781 Adjustment: link every endorsement request to its policy and
-- placement, and business labels of the endorsement types.
--   Requirements: ADJID.001/020 (ARN and invoice kept on the request), ADJID.022/024 (history
--   and look-up), client feedback of 27-Sep-2026 on the Adjustment Workbench.
--   Design: docs/architecture/OPERATIONS_DESIGN.md section 4.5.
--   Keys of other modules stay plain values: no foreign keys to the V8xx tables. The account
--   look-up runs only where the account table already exists (an upgraded database).
-- =====================================================================================

-- Account and product of the request, copied from the ledger and the account when it is raised.
alter table adj_request
    add column account_id   bigint,
    add column product_code varchar(30);

-- Policy number and account from the Operations ledger (the policy number may have been issued
-- after the request was raised).
update adj_request r
set account_id = i.account_id,
    policy_no  = coalesce(i.policy_no, r.policy_no)
from ops_invoice i
where i.invoice_no = r.invoice_no;

do $$
begin
    if to_regclass('acc_account') is not null then
        execute 'update adj_request r set product_code = a.product_code, account_id = coalesce(r.account_id, a.id)'
             || ' from acc_account a where a.arn = r.arn and r.product_code is null';
    end if;
end
$$;

-- Look-up paths of the workbench search (policy, client, product) and of the account history.
create index ix_adj_request_policy on adj_request (policy_no);
create index ix_adj_request_client on adj_request (client_code);
create index ix_adj_request_account on adj_request (account_id);

-- Endorsement types as the business reads them: class, then the change. Only labels that still
-- hold the delivered text are replaced (a label maintained by the administrator is kept).
update lov_value v
set label = n.label,
    updated_at = now(),
    updated_by = 'SYSTEM'
from (values
    ('FIN_TSI', 'Change of Total Sum Insured (increase or decrease)', 'Financial – Change of Sum Insured'),
    ('FIN_ITEMS', 'Change in insured items, persons / members, units, locations', 'Financial – Change of Insured Items'),
    ('FIN_COMMISSION_RATE', 'Change in commission rate (increase or decrease)', 'Financial – Change of Commission Rate'),
    ('FIN_PREMIUM_RATE', 'Change in premium rate (increase or decrease) or premium amount', 'Financial – Change of Premium Rate'),
    ('FIN_EXTENSION', 'Extension of cover (period of cover)', 'Financial – Extension of Cover'),
    ('FIN_CHANGE_COVER', 'Change of cover', 'Financial – Change of Cover'),
    ('FIN_CHARGES', 'Adjustment in charges (DST, premium tax / VAT, LGT, others) - increase or decrease', 'Financial – Adjustment in Charges'),
    ('FIN_MINIMAL_BALANCE', 'Minimal balance', 'Financial – Minimal Balance'),
    ('NF_DESCRIPTIVE', 'Descriptive changes (assured, person, risk / item, perils, coverage, extensions, warranties, clauses, deductibles)', 'Non-financial – Descriptive Change'),
    ('NF_PERIOD_CHANGE', 'Change of period cover (inception moved earlier / later, term unchanged)', 'Non-financial – Change of Period'),
    ('NF_PERIOD_EXTENSION', 'Extension of period covered (inception and expiry moved, no financial impact)', 'Non-financial – Extension of Period'),
    ('NF_COVER_EXTENSION', 'Extension of cover (extensions / clauses / warranties / deductibles added, no premium)', 'Non-financial – Cover Extension'),
    ('NF_ASSURED_INFO', 'Change of assured name or information (address, birthdate, occupation)', 'Non-financial – Assured Information'),
    ('INT_ADJUSTMENT', 'Internal adjustment (no insurer endorsement needed)', 'Internal Adjustment')
) as n(code, delivered, label)
where v.type_code = 'ENDORSEMENT_TYPE'
  and v.code = n.code
  and v.label = n.delivered;
