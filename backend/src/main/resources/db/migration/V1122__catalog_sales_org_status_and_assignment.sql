-- =====================================================================================
-- iNXT BrokerVerse - V1122 Catalog: sales organisation status reasons and officer assignment
-- dates (Product Maintenance extension range V1120-V1129).
--   A unit is deactivated or reactivated, and an account officer removed from a team, with a
--   mandatory reason kept on the record. An officer carries the date of the current team
--   assignment (set on assignment and on every move); existing officers take their creation
--   date.
-- =====================================================================================

alter table cat_sales_unit add column status_reason varchar(500);
alter table cat_sales_officer add column status_reason varchar(500);
alter table cat_sales_officer add column assigned_since date;

update cat_sales_officer set assigned_since = cast(created_at as date) where assigned_since is null;
