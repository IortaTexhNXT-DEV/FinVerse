# Bank statement import format (CSV)

Used by **Receivables & Banking > Bank Statements** (`POST /api/v1/receivables/bank-rec/statements`).
Sample: [`bank-statement-sample.csv`](bank-statement-sample.csv).

| Rule | Detail |
|---|---|
| Encoding | UTF-8 text, one statement line per row, comma separated. Fields containing commas are enclosed in double quotes (`"1,000.50"`, `"Charges, June"`); a quote inside a quoted field is doubled (`""`). Blank rows are ignored. |
| Header | First row, column names in any order, case insensitive: `date`, `description`, `reference`, `debit`, `credit`, `balance`. `date`, `debit` and `credit` are mandatory columns; the others are optional. |
| `date` | Value date: `yyyy-MM-dd`, `dd/MM/yyyy` or `dd-MM-yyyy`. |
| `description` | Bank narration (max 250 characters). |
| `reference` | Cheque number, deposit slip number, transfer reference... (max 60). Used by automatic matching. |
| `debit` | Money **leaving** the account (cheque paid, charges, returned cheque). |
| `credit` | Money **received** (deposit, transfer in, interest). Exactly one of `debit` / `credit` is filled and positive on each row. Thousands separators are allowed. |
| `balance` | Running balance after the row (optional). When present it must agree with the computed running balance, otherwise the import is rejected with the row number. |
| Opening balance | Entered on the upload screen, or derived from the first row (`balance - credit + debit`), or zero. |
| Statement reference | Entered on the upload screen, or the file name; unique per bank account (re-importing the same reference is rejected). |

The statement is stored per GL bank account (for example `1111 Cash in Bank - BDO Current Account`).
The balance of the last line on or before a date is the "balance as per bank statement" of the Bank
Reconciliation Statement, so consecutive statements must continue the running balance (enter the
closing balance of the previous statement as opening balance when the file has no balance column).
