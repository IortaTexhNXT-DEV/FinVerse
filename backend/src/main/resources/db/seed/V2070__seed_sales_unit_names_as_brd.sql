-- Seed data correction found by the list presentation review (02-Oct-2026): the seeded sales units are named the way
-- the BRDs and the FRS name the marketing units. The New Business FRS (BRD-01, Definitions: "CBG: Consumer Banking
-- Group") and the Renewal FRS (BRD-06, business users "Retail and Corporate Marketing") name the units Consumer Banking
-- Group and Corporate Marketing; the seed called them "Consumer Banking" and "Corporate Accounts".
-- Seed profile only; the codes (CBG-NCR, CBG-VIS, CORP-NCR, T-CORP1) are unchanged and a unit already renamed by a
-- user is left as it is.
update cat_sales_unit u
set name = n.name, updated_at = now(), updated_by = 'SYSTEM'
from (values
  ('CBG-NCR', 'Consumer Banking - NCR', 'Consumer Banking Group - NCR'),
  ('CBG-VIS', 'Consumer Banking - Visayas', 'Consumer Banking Group - Visayas'),
  ('CORP-NCR', 'Corporate Accounts - NCR', 'Corporate Marketing - NCR'),
  ('T-CORP1', 'Corporate Team 1', 'Corporate Marketing Team 1')
) as n(code, seeded, name)
where u.code = n.code
  and u.name = n.seeded
  and u.company_id = (select id from org_company where code = 'FVI');
