# Receivables & Banking (collections, PDC received, bank reconciliation)

Package `com.iortatechnxt.finverse.receivables`, frontend `src/features/receivables`, migrations
`V550` (schema) and `V955` (demo rule), demo loader `receivables.demo.ReceivablesDemoData`.

Banks are identified by their **GL bank account code** (postable, active accounts of a bank / cash
category, e.g. `1111`, `1112`, `1113`): the module keeps no bank master of its own.

## 1. Official receipts

Life cycle: `PENDING_APPROVAL` (maker, `RECEIPT_PAYMENT_MAINTAIN`) -> `APPROVED` or `REJECTED`
(checker, `RECEIPT_PAYMENT_AUTHORIZE`, never the maker) -> `CANCELLED` / `BOUNCED`.
Receipt numbers `OR-<branch>-<year>-nnnnnn`. The exchange rate is the SPOT rate of the receipt date
(the same rate the accounting engine uses for the journal).

Allocation to the payer's open DEBIT items (debit notes): `MANUAL` (items and amounts validated at
entry, matched at approval), `FIFO` (oldest due first, computed at approval) or `NONE` (on account).
Money not allocated stays **on account**; it can be applied later (`/receipts/{id}/apply`).

### Accounting (through the accounting engine; the module never picks GL accounts)

| Situation | Event (source reference) | Demo rule |
|---|---|---|
| Approval, policyholder / intermediary, applied part | `PREMIUM_RECEIPT` (`RCPT:<id>:APPLIED`) | Dr bank (`@BANK`) / Cr 1201 premiums receivable (party) |
| Approval, policyholder / intermediary, unapplied part | `PREMIUM_DEPOSIT` (`RCPT:<id>:UNAPPLIED`) | Dr bank / Cr 2205 premium deposits |
| Later application of money on account | `UNAPPLIED_APPLICATION` (`RCPT:<id>:APPLY:<n>`, new in V550) | Dr 2205 / Cr 1201 (party) |
| Approval, reinsurer | `RI_SETTLEMENT_RECEIPT` (`RCPT:<id>`) | Dr bank / Cr 1205 (party) |
| Approval, other payer | `MISC_RECEIPT` with `@INCOME` = chosen income account | Dr bank / Cr income |
| Cancellation or bounced cheque | the same events with **negative** amounts, current applied / unapplied split (`...:REV`) | reverse of the above |

Sub-ledger: approval records one CREDIT open item (`RECEIPT`, full amount) and matches it against
the allocated debit notes; its unmatched balance is the money on account (On A/c in the ageing).
Cancellation / bounce: every match of the receipt's credit item is undone (new
`OpenItemService.unmatch` / `unmatchAll`), the debit notes re-open, and a DEBIT `RECEIPT_REVERSAL`
item neutralises the credit item.

## 2. Cheques received and deposits

Cash and cheque receipts are `UNDEPOSITED` after approval (the receipt journal already debits the
bank account; until the bank credits it the amount is a *deposit in transit* in the BRS). Deposit
slips (`DS-<branch>-<year>-nnnnnn`) group receipts of one bank account and currency:
`PREPARED -> DEPOSITED` (receipts `DEPOSITED` with the deposit date) or `CANCELLED` (receipts back to
undeposited). A bounced cheque is recorded on the receipt (`/receipts/{id}/bounce`).

## 3. Post-dated cheques received (PDC register)

Accounting treatment: **memorandum until banked**. A PDC on hand (`ON_HAND`, `DUE`) is not posted to
the GL; it is shown in the PDC reports and as "PDC Cheques / Balance Net of PDC" on the Statement of
Outstanding. Life cycle:

```
ON_HAND --mark due (cheque date reached)--> DUE
ON_HAND / DUE --bank--> DEPOSITED   (raises an official receipt, mode PDC, pending approval;
                                     allocated to the linked debit note if still open, else FIFO)
DEPOSITED --clear (receipt approved)--> CLEARED
DEPOSITED --bounce--> BOUNCED        (the receipt is reversed as a bounced cheque)
DEPOSITED --receipt rejected--> DUE
ON_HAND / DUE --> RETURNED | REPLACED (replacement registered as a new PDC)
```

Every change is kept in `rcv_pdc_event` (confirmation audit trail, `GET /pdcs/{id}`); report
statuses "as of" a date are taken from this history.

## 4. Bank reconciliation

* Statement import: CSV, see [`docs/samples/BANK_STATEMENT_FORMAT.md`](../samples/BANK_STATEMENT_FORMAT.md).
* Book side: posted ledger entries of the bank GL account (amounts in the account currency).
* Automatic matching (`/bank-rec/auto-match`): one-to-one on equal signed amount within a date
  window (default 7 days), preferring a reference / cheque number found in the other side's text,
  then the closest date; then group matching of a bank line with the book entries of the deposit
  slip, or of the receipt, named in its reference. Manual matching of any balanced selection;
  unmatching.
* A match is dated with its latest item date (never on or before the last finalized reconciliation),
  so an "as of" BRS never changes once finalized; matches covered by a finalized reconciliation
  cannot be undone.
* BRS: `Bank balance = Book balance - (1) book debits not in bank + (2) book credits not in bank
  - (3) bank debits not in book + (4) bank credits not in book`; the difference to the imported
  statement balance must be zero to finalize.

## 5. Reports (FINANCE_REPORTS_SPEC.md)

FIN-AR-AGE-DET, FIN-AR-AGE-SUM, FIN-AR-AGE-DIV (division = branch of the document), FIN-AR-SOO,
FIN-AR-SOO-FC, FIN-AR-CHQ-RCPT, FIN-AR-CHQ-UNDEP, FIN-ARAP-SOA-MATCH, FIN-BRS-UNREC-BOOK,
FIN-BRS-UNREC-BANK, FIN-BRS-STMT, FIN-PDC-RCV-ONHAND, FIN-PDC-RCV-PERIOD, FIN-PDC-RCV-DUEBANK and the
`-DDB` variants (division / department / bank). Ageing slots are configurable per run (up to five
ascending day limits, default `30,60,90,120`); the basis is the due date or the document date.
Balances "as of" ignore matches dated after the date.

## 6. Known limitations

* Unmatching deletes the sub-ledger match (the audit trail keeps it), so an ageing "as of" a date
  before a cancellation shows the re-opened debit note as open on that date.
* Receipts use the SPOT rate of the receipt date; a negotiated rate cannot be entered yet.
* Bank reconciliation matches whole items (no partial matching of one entry against two lines other
  than by grouping).
