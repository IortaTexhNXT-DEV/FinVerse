-- Standard claim document checklist of the SIT/UAT environments (FRS BRD-07 FRCLM.006.01; BDOI loads its own
-- checklist of Annex J with the masters): Motor claims need the police report and the driver's license; the photos
-- of the damage are listed as optional. Fire & Lightning claims need the claim form.
insert into bcl_document_requirement (claim_type, document_type, required, active, created_at, created_by)
select r.claim_type, r.document_type, r.required, true, now(), 'SYSTEM'
from (values ('MC', 'CLAIM_POLICE_REPORT', true),
             ('MC', 'CLAIM_DRIVERS_LICENSE', true),
             ('MC', 'CLAIM_DAMAGE_PHOTOS', false),
             ('FL', 'CLAIM_FORM', true)) as r(claim_type, document_type, required)
where not exists (select 1 from bcl_document_requirement x
                  where x.claim_type = r.claim_type and x.document_type = r.document_type);
