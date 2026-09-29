-- Business wording of the Operations set-up texts shown on screens (BRD-2 business sign-off): the
-- payment file layouts are described by the file, its sender and its form, without internal file
-- specification codes or project references.
update csh_payment_file_layout
   set description = 'Bills payment file of the bank: text file, fields separated by a vertical bar, header line first'
 where handler_code = 'PAY_BILLS';
update csh_payment_file_layout
   set description = 'Trade payment file of Corporate and Investment Banking: text file, fields separated by a vertical bar, header line first'
 where handler_code = 'PAY_TRADE';
update csh_payment_file_layout
   set description = 'CLPC payment file: text, Excel or CSV file, header line first'
 where handler_code = 'PAY_CLPC';
update csh_payment_file_layout
   set description = 'Direct credit file of the bank: text file, fields separated by a vertical bar, header line first'
 where handler_code = 'PAY_DIRECT_CREDIT';
update csh_payment_file_layout
   set description = 'Post-dated check list: text, Excel or CSV file, header line first'
 where handler_code = 'PAY_PDC';
update csh_payment_file_layout
   set description = 'Commission payments of the insurers received by Collection'
 where handler_code = 'COMMISSION_PAYMENT';
update csh_payment_file_layout
   set description = 'BIR 2307 certificates tagged by Marketing Collection'
 where handler_code = 'CWT_TAGS';
