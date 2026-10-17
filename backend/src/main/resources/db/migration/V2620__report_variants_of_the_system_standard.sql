-- The shared variants that the Claims reports brought with them (owner SYSTEM) are configuration of
-- every environment like the standard variants of the Report Centre: mark them standard so that they
-- are listed with the standard variants, cannot be deleted and travel with Configuration Promotion.
update nbr_report_variant
   set standard = true
 where upper(owner) = 'SYSTEM'
   and standard = false;
