-- =====================================================================================
-- SEED DATA (seed profile only): accounting rule for applying unapplied collections.
-- Premium deposits (2205) are released against the policyholder / intermediary
-- receivable (1201, sub-ledger party) when money held on account is applied to a debit note.
-- Receipts, deposits, PDCs and bank statements are created by ReceivablesSeedData.
-- =====================================================================================
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'UNAPPLIED_APPLICATION', 'Standard application of unapplied collection', 100,
       date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '2205', 'AMOUNT', false),
  (2, 'CREDIT', '1201', 'AMOUNT', true)
) as l(n, side, acc, comp, party)
where r.event_type = 'UNAPPLIED_APPLICATION';
