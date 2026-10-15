-- Microsoft Entra ID keeps the second factor of the bank's users, so the administrators' reset of
-- the BrokerVerse second factor (V1181) goes: the permissions MFA_RESET and MFA_RESET_APPROVE are
-- revoked from every group profile and withdrawn, and the reset notices go. The rows of
-- sec_mfa_reset_request and the audit trail of past resets stay as history.
delete from sec_role_permission where permission in ('MFA_RESET', 'MFA_RESET_APPROVE');
delete from sec_permission_action where permission in ('MFA_RESET', 'MFA_RESET_APPROVE');

delete from msg_notification_preference where event_code in ('MFA_RESET_TO_APPROVE', 'MFA_RESET_DONE');
delete from sbm_notification_recipient where event_code in ('MFA_RESET_TO_APPROVE', 'MFA_RESET_DONE');
delete from msg_notification_event where code in ('MFA_RESET_TO_APPROVE', 'MFA_RESET_DONE');
