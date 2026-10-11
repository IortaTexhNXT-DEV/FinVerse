-- =====================================================================================
-- iNXT BrokerVerse - V853 Placement: tax details of the clients on the placement slip
-- (Ease of Paying Taxes, FR-NB-087). The slip records a fingerprint of the taxpayer name, TIN
-- and registered address of its clients when it is generated; a slip whose clients' details
-- changed before the first send is generated again as its next version before it is sent.
-- =====================================================================================
alter table plc_slip add column eopt_fingerprint varchar(64);
