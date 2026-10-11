-- =====================================================================================
-- iNXT BrokerVerse - V895 Request forms of payrequest (Appendix D, MKT 1.10.0): Refund Request
-- Form (RRF), Request for Payment (RFP) and Cash Advance Liquidation Form. Draft wording; the
-- mandatory fields, signatories and layouts are to be confirmed by BDOI (AQ18).
-- =====================================================================================

insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('PRQ_RRF', 1, 'Refund Request Form',
 'Refund request {{requestNo}} ({{reference}}) of {{requestDate}} for segment {{segment}}: please refund {{currency}} {{amount}} to {{payeeName}} by {{mode}} for the accounts listed below.', date '2020-01-01', now(), 'SYSTEM'),
('PRQ_RFP', 1, 'Request for Payment',
 'Request for payment {{requestNo}} of {{requestDate}}: {{rfpType}} of {{currency}} {{amount}} to {{payeeName}} by {{mode}}. Purpose: {{purpose}}', date '2020-01-01', now(), 'SYSTEM'),
('PRQ_LIQUIDATION', 1, 'Cash Advance Liquidation Form',
 'Liquidation {{liquidationNo}} of cash advance {{requestNo}} of {{payeeName}}: cash advanced {{currency}} {{cashAdvanced}}, expenses {{totalExpenses}}, over / (short) {{overShort}}.', date '2020-01-01', now(), 'SYSTEM');
