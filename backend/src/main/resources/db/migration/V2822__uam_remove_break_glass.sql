-- Microsoft Entra ID signs every user in; there is no break-glass account. The break-glass sign-in
-- alert (notification event, alert code without raised alerts, recipients setting) is withdrawn.
-- Alerts already raised keep their code so the history stays readable.
delete from msg_notification_preference where event_code = 'UAM_BREAK_GLASS_SIGN_IN';
delete from sbm_notification_recipient where event_code = 'UAM_BREAK_GLASS_SIGN_IN';
delete from msg_notification_event where code = 'UAM_BREAK_GLASS_SIGN_IN';

delete from alt_exception_code c
where c.code = 'UAM_BREAK_GLASS_SIGN_IN'
  and not exists (select 1 from alt_alert a where a.exception_code = c.code);

delete from sys_parameter where param_key = 'BREAK_GLASS_ALERT_RECIPIENTS';

update sys_parameter set description =
    'true: no forgotten-password link on the Login page, and a local test account''s password is reset only by another System Administrator; false: the self-service reset link for local test accounts and the administrator reset of any local test account. Production never uses local passwords.'
where param_key = 'PASSWORD_RESET_BDOI_RULES';
