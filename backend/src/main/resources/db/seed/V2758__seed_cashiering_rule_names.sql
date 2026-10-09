-- Seed data: the accounting rules of the cancellation and reinstatement records read as business
-- names.
update acc_rule
   set name = regexp_replace(name, '\s*\([Ss]eed\)$', '')
 where event_type in ('OPS_AR_INSURER_REFUND', 'OPS_COMMISSION_PAYMENT_HELD', 'OPS_AP_UNAPPLIED_COMMISSION')
   and name ~ '\([Ss]eed\)$';
