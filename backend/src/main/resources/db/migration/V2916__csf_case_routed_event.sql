-- Notification event of the case routing notice (FRS BRD-9 v1.2, FR-CSF-050 and FR-CSF-060, BRCSFCM.012): the
-- members of the fulfilment unit are told in the system and by e-mail, with a copy to the account officer and to the
-- unit of the account (the account officers and the head of the account's sales unit). Each user may switch either
-- channel off in the notification preferences.
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order)
values ('CSF_CASE_ROUTED', 'Case routed to a fulfilment unit', 'CUSTOMER_SERVICE',
        'A case was routed to your fulfilment unit, or a copy for the account officer or the unit of the account',
        true, true, 200)
on conflict (code) do nothing;
