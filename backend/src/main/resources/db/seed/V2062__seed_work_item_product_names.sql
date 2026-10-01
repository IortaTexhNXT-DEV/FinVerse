-- Seed data correction found by the New Business sign-off pack: the work items of the seeded quotations, proposal
-- requests and accounts were described as "<client> - <product code>". The running system now describes them by the
-- product's name, as the work queues, My Work and the Assign dialog show them; the seeded descriptions follow.
-- Seed profile only; only a description that still ends with the product code is changed.
update wf_case w
set title = left(w.title, length(w.title) - length(p.code)) || p.name
from cat_product p
where w.workflow_code in ('NB_QUOTATION', 'NB_PROPOSAL', 'NB_ACCOUNT')
  and w.title like '% - ' || p.code
  and p.name is not null
  and p.name <> p.code;
