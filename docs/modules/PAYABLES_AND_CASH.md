# Payables & Cash (supplier invoices, payments, PDC issued, petty cash)

Package `com.iortatechnxt.finverse.payables`, frontend `src/features/payables`, migrations `V500`
(schema) and `V950` (demo masters).

## 1. Documents and approval

| Document | Maker | Checker | Inbox link |
|---|---|---|---|
| Supplier invoice `SI-…` | `RECEIPT_PAYMENT_MAINTAIN`, DRAFT → PENDING_APPROVAL | `RECEIPT_PAYMENT_AUTHORIZE` | `/payables/invoices` |
| Payment voucher `PV-…` | same | same | `/payables/vouchers` |
| Petty cash voucher / reimbursement claim | same | same | `/payables/petty-cash` |
| Bank account, petty cash fund (master data) | `MASTER_MAINTAIN` | `MASTER_AUTHORIZE` | `/payables/bank-accounts`, `/payables/petty-cash` |

The checker must not be the creator or the submitter, and the document's base-currency amount must
be within the checker's authorization limit (`PayablesSupport.checker`). The universal approval
inbox (`PayablesApprovalSource`) applies the same two rules, so a checker only sees the documents
they can actually approve; the system-wide view used by the `PENDING_APPROVAL_AGEING` alert sees all
of them.

## 2. Payments and their reversal

Approval of a payment voucher posts the event of its category (`SUPPLIER_PAYMENT`,
`COMMISSION_PAYMENT`, `CLAIM_PAYMENT`, …; Dr party payable / Cr bank, or Cr the PDC clearing account
for post-dated cheques), records a DEBIT `PAYMENT` open item and matches it against the paid CREDIT
items (invoices, commission and claim payables).

A payment is reversed when an unpresented cheque is **voided** (`/vouchers/{id}/void`) or an issued
PDC is **cancelled** (`/pdc-issued/{id}/cancel`):

1. the same event is posted with a negative amount (`PV:<id>:VOID`: Dr bank or PDC clearing / Cr
   party payable);
2. every match of the payment item is undone with `OpenItemService.unmatchAll`, so the paid invoices
   are open again under their **own** document numbers, dates and due dates (ageing and statements
   show them exactly as before the payment);
3. a CREDIT `PAYMENT_REVERSAL` item (`<voucher no>-VOID`, the reversal journal) is recorded and
   matched against the payment item, which is therefore settled and drops out of the outstanding
   list.

This is the same pattern as the cancellation of a receipt in receivables. Earlier versions
re-created the paid items as new CREDIT items because the sub-ledger had no unmatch; that
workaround is gone.

## 3. Post-dated cheques issued

`ISSUED → DUE → PRESENTED → CLEARED`, or `CANCELLED` (reversal as above, voucher VOIDED) or
`REPLACED` (new cheque leaf, no posting). See `IssuedPdcService` for the postings of each step.

## 4. Ageing reports

`FIN-AP-AGE-SUM`, `FIN-AP-AGE-DET`, `FIN-AP-SUPOS` and `FIN-AP-SOP` age the vendor open items with the
shared `subledger.service.AgeingSlots`. Users may enter up to five slots per run (`slot1..slot5`);
when none is entered the company default applies: the `AGEING_BUCKETS` system parameter
(`AgeingService.defaultSlots()`, 30/60/90/120 as delivered). The Statement of Payables keeps its own
default layout (30/60/90/180/365) prescribed by the Reports Book.
