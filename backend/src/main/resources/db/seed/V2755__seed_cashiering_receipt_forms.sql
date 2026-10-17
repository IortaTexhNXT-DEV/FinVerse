-- Seed data: the AR and OR forms of the Philippine company with the header and footer lines of the
-- client's templates (FRS.CSH.02.06.02 / 02.06.03; Appendix A sample AR).
update csh_receipt_form f
   set company_description = 'A subsidiary of BDO Unibank Inc.',
       footer_1 = 'BDO Insure is the Trademark of BDO Insurance and Reinsurance Brokers, Inc. | BDO Insurance and Reinsurance Brokers, Inc. is a licensed broker of the Insurance Commission of the Philippines.'
  from org_company c
 where c.id = f.company_id and c.code = 'FVI' and f.form_kind = 'OR' and f.version_no = 1;

update csh_receipt_form f
   set company_description = 'A subsidiary of BDO Unibank Inc.',
       footer_2 = 'BDO Insure is the Trademark of BDO Insurance and Reinsurance Brokers, Inc. | BDO Insurance and Reinsurance Brokers, Inc. is a licensed broker of the Insurance Commission of the Philippines.'
  from org_company c
 where c.id = f.company_id and c.code = 'FVI' and f.form_kind = 'AR' and f.version_no = 1;
