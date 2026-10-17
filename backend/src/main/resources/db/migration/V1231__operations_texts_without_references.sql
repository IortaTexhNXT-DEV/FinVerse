-- Texts of the Operations screens in business terms only: the notification event of refund,
-- cash-advance and check-cancellation requests, the excess minimal balance rule and its accounting
-- event carried requirement references. Each text is replaced only where it still holds the
-- delivered value: a text an administrator has since changed is kept.
update msg_notification_event
   set description = 'A refund, cash-advance or check-cancellation request was validated, returned, approved or disbursed'
 where code = 'PRQ_REQUEST_STATUS'
   and description = 'A refund, cash-advance or check-cancellation request was validated, returned, approved or disbursed (MKT 1.20.0, 2.26.0)';

update csh_minimal_balance_rule
   set description = 'Unapplied and excess payments up to the amount are moved to AP overages'
 where kind = 'EXCESS'
   and description = 'Unapplied and excess payments up to the amount are moved to AP overages (Cashiering summary 5.f)';

update acc_event_type
   set description = 'Unapplied or excess payments at or below the minimal amount moved to AP overages.'
 where code = 'OPS_EXCESS_TO_OVERAGES'
   and description = 'Unapplied or excess payments at or below the minimal amount moved to AP overages (Cashiering summary 5.f).';
