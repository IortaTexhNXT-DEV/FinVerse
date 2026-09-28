-- =====================================================================================
-- iNXT BrokerVerse - V1121 Catalog: structured validation checks of a package version
-- (PMADD06; Product Maintenance extension range V1120-V1129).
--   The validation checkpoint keeps its result (PASSED when released, RETURNED when sent back
--   to MBS), who returned it and when, and the checks run at that moment: code, label, result
--   (PASSED / FAILED / NOT_APPLICABLE) and the figures compared ("3 of 3 panel insurers have
--   terms"). Versions validated before keep their confirmed checklist, carried over as passed
--   checks so the page reads them the same way.
-- =====================================================================================

alter table cat_product_version add column validation_result varchar(20);
alter table cat_product_version add column returned_by varchar(50);
alter table cat_product_version add column returned_at timestamptz;
alter table cat_product_version add constraint ck_cat_version_validation_result
    check (validation_result is null or validation_result in ('PASSED', 'RETURNED'));

create table cat_version_validation_check (
    version_id  bigint       not null references cat_product_version (id) on delete cascade,
    seq         int          not null,
    code        varchar(30)  not null,
    label       varchar(200) not null,
    result      varchar(20)  not null,
    detail      varchar(500),
    constraint pk_cat_version_validation_check primary key (version_id, seq),
    constraint ck_cat_version_check_result check (result in ('PASSED', 'FAILED', 'NOT_APPLICABLE'))
);

update cat_product_version set validation_result = 'PASSED' where validated_by is not null;
update cat_product_version set validation_result = 'RETURNED'
where validated_by is null and returned_reason is not null;

insert into cat_version_validation_check (version_id, seq, code, label, result, detail)
select v.id,
       cast(t.ord as int),
       case t.item
           when 'Hierarchy complete: cover type and basic coverage' then 'HIERARCHY'
           when 'Every panel insurer has terms for each included coverage' then 'INSURER_TERMS'
           when 'Rates and minimum premiums match the signed-off terms' then 'RATES'
           when 'Dates and package term are correct' then 'DATES'
           when 'Test premium reviewed' then 'TEST_PREMIUM'
           else 'CONFIRMED'
       end,
       left(t.item, 200),
       'PASSED',
       'Confirmed by the validator'
from cat_product_version v
cross join lateral json_array_elements_text(cast(v.validation_checklist as json))
    with ordinality as t(item, ord)
where v.validated_by is not null
  and v.validation_checklist like '[%';
