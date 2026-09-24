# Finance Reports – Implementation Specification

Source: *Annexure 2(b) Reports Book – Finance* (PREMIA 11, v1.0, 125 pages). This is the
traceability baseline for building the Finance reports in iNXT BrokerVerse. Companion documents:
`GL_FUNCTIONAL_SPEC.md` (core GL) and `REPORTS_BOOK_SPEC.md` (GI reports).

How to read this file:
- **FIN code**: the BrokerVerse report code (stable and short). **Src ID**: the PREMIA report ID printed
  on the sample page in the book, so business users can recognise the report.
- Columns are listed in print order: left to right, then top to bottom when a header wraps onto
  several lines. `A / B` means B is printed under A in the same column.
- Anything the book does not state (for example a formula, a filter or a default) is marked **(inferred)**.
- Parameter notation: `Name (type, M|O)`. M = mandatory, O = optional. Types: `range<code>`
  (From/To pair), `dateRange`, `date`, `month`, `year`, `option{…}` (radio), `flag` (checkbox),
  `lov` (a single value from a List of Values), `int`, `amount`, `text`.

---

## 1. Generic report interface and layout

### 1.1 Interface types
| Type | Rule |
|---|---|
| Standard Report Interface | A fixed set of parameters for each report. Tab moves to the next field. Pressing Tab after the last field opens the **Report popup** (output options, section 1.3). |
| Dynamic Report Interface | A grid of *Parameter* and *Value* rows. The rows come from the report definition built with the **Report Generator** tool. The user types each value. This is needed for user-defined reports (inferred: BrokerVerse report-definition metadata). |

### 1.2 Parameter field kinds
| Kind | Rule |
|---|---|
| Data-entry | Free text. In PREMIA a full range is From = `0` and To = `zzzzzzzzzzzz` (alphanumeric), or `0` / `9999999999` (numeric). Tabbing out of an empty field fills these values automatically. **BrokerVerse:** a blank From/To means "all". The echo prints `0` / `zzzz…` for compatibility (inferred). |
| LOV Help | Picks a value from a list. The full range is still allowed. |
| Check box | A Y/N criterion. |
| Radio (option) | Picks one variant of the report from the same parameters (for example Order By, Base/Foreign currency, Posted/Unposted/Both). |
| Date | Usually a From/To pair. Some reports use a single As-of date. |
| Range vs Selection | Some reports offer both. *Range* shows From/To fields. *Selection* shows a single LOV field. The book notes this is "not functional" on some reports. BrokerVerse supports both on every range parameter (inferred). |

Validation (inferred): From ≤ To. Date ranges must fall within the open or closed financial periods
of the company. Month is 1–12, plus period 13 (adjustment period) where the report shows "13th Period Included".

### 1.3 Output options (Report popup)
| Option | Behaviour |
|---|---|
| View | Renders on screen. The popup offers **PDF / Excel / RTF** sub-formats. |
| Mail | Puts the report in the body or an attachment of a mail message (Outlook in PREMIA). BrokerVerse sends it by SMTP or the mail integration (inferred). |
| Print | Produces a PDF sent to the chosen printer. Fields: **Printer Name** (LOV of printers) and **Number of Copies** (int ≥ 1). |
| File | Saves the report to a physical path. Sub-formats: PDF / Excel / RTF. |
| HTML / PDF / XML | Writes the file to the report path. |
| Excel | Produces an Excel file. |
| Publish & Subscribe | Schedules generation for a date and time. Users can subscribe to receive the report (see the scheduler in BrokerVerse, inferred). |

Other popup fields:
- **Logo Y/N**: prints the company logo on the report.
- **File Name**: defaults to the report ID plus a timestamp (inferred). The user can edit it. Used by the HTML, PDF and XML options.
- **Report path**: set by the company parameter `REP_PATH` (Core → Installation Attributes → Company Parameter).

### 1.4 Layout (common to every report)
The layout has three sections, in this order:
1. **Report Header**. Title (centred). Company Name. User ID. **Report ID + version** (for example `FGL012 / v1.10`). Run Date (and time). **Page No "n of m"**. Ledger reports also print "13th Period Included" when the adjustment period is included.
2. **Input Parameters**. Echoes every parameter from the interface, including range defaults.
3. **Report Data**. Group headers, detail lines, sub-totals and a grand total, then the `***End Of Report***` footer.

Other conventions seen in the samples:
- Amounts use 2 decimals. Some samples show 3; BrokerVerse uses the currency's precision (inferred).
- Balances print as an absolute value with a `Dr`/`Cr` suffix.
- MIS statements print negatives as `< n >`.
- Documents print as `TC-DocNo`, for example `CN100-2013120004`. The transaction code is the voucher-type prefix.
- Reports on documents that need approval carry approver signature blocks (Entered / Authorised / Approved by, with dates). Some also carry a Remarks field and the document **Status** (Submitted / Posted / Approved).
- A report can repeat the page header on every page. Group headers repeat on continuation pages (inferred).

### 1.5 Shared calculation rules (referenced below)
| Rule | Definition |
|---|---|
| R-BAL | Balance = Σ Debit − Σ Credit. Print \|x\| with the suffix Dr when x ≥ 0, else Cr. |
| R-RUN | Running balance: the opening balance plus the cumulative (Dr − Cr), document by document, in sort order. |
| R-OPEN | Opening balance for a period = all posted entries before the From date. The balance brought forward from the year-end close counts as opening (inferred). |
| R-TB | TB closing = opening + period Dr − period Cr. It goes in the Closing Debit column when positive, else in the Closing Credit column. Σ Closing Dr must equal Σ Closing Cr. The book says any difference goes to suspense. BrokerVerse flags the imbalance in the report footer (inferred). |
| R-AGE | Age days = As-of date − basis date. The basis is **Document Date** or **Due Date** (the Order By / basis option). Each open item's *balance* (original amount − matched amount as of the as-of date) goes into the slot whose bounds contain its age. Slots are configurable: the book mentions 10/20/30/40+ for creditors and 0/30/45/60/75+ for supplier outstanding. The samples show ≤30/31-60/61-90/91-120/≥121, ≤90/91-180/…/≥181, and 0-30/31-60/61-90/91-180/181-365/>365. **BrokerVerse: up to 5 user slot boundaries plus an "above" bucket, default 30/60/90/120** (inferred). |
| R-ONAC | **On A/c** = unallocated items on the opposite side of the balance: unmatched receipts or payments, advances and credit notes. It is printed separately and not aged. **Net Value = Σ age buckets + On A/c.** On A/c is printed with its sign. |
| R-FX | LC value = FC amount × the rate stored on the document (historical rate, not revalued, inferred). With the "Base currency" option, reports show LC. With "Foreign currency", they show FC amounts grouped by currency. |
| R-OPENITEM | Open item = an AR/AP document line posted to a sub-ledger (control) account whose balance, after knock-off/matching, is not zero as of the as-of date (inferred: matching dated after the as-of date is ignored). |

---

## 2. Report catalogue

Count: **52 reports** (4 MIS, 5 Trial Balance, 2 Petty Cash, 8 GL operational, 4 Cashier, 2 Activity,
4 AP, 8 AR, 4 BRS, 11 PDC). The PDC section of the book contains 11 reports, not 12 (see section 4).

Several source reports share one program: FAP008 is used for Creditors, Debtors and Division-wise ageing summaries; FAP009 for the Creditors and Debtors detailed ageing; FAP007A for the Statement of Payables and the Statement of Outstanding. **Build them as one engine each, parameterised by ledger type (AR/AP)** (inferred).

### 2.1 MIS / Budgeting (GM Finance – strategic)

These reports need a **Financial Statement Format** master (Format ID, description, ordered line items, account mapping, total lines, sign) (inferred from "Format ID").

#### FIN-MIS-BS – Balance Sheet (Src FMI09)
- Purpose: summary financial position (assets, liabilities) as of a date, this year against last year.
- Params: Format ID (lov, M). As Of Date (date, M). Rounding Option (option{None, Thousands, Lakhs, Millions} (inferred values), M, default None).
- Header echo: Format ID, Format Description, As On Date.
- Columns: Particulars | Balance Last Year | Balance This Year.
- Structure: format captions and groups (Fixed Asset, Investments, Reinsurance Debtors, Insurance & Other Receivables, Unearned Premium Reserve, Outstanding Losses, IBNR Reserves, Creditors, …). Group lines show format-group values (not individual accounts), followed by TOTAL lines, TOTAL ASSETS, TOTAL LIABILITIES & PROVISIONS, NET ASSETS, and REPRESENTED BY / Shareholders' Funds.
- Calc: This Year = R-BAL of the mapped accounts as of the As-of date. Last Year = R-BAL as of the same day and month one year earlier (inferred). The alternative is the previous FY end; make it configurable. Line sign comes from the format (assets Dr+, liabilities Cr+) (inferred). Negatives print as `< >`. Rounding applies per line and the totals are recomputed from the rounded lines (inferred).

#### FIN-MIS-BS-SCH – Schedule to Balance Sheet (Src FMI10)
- Purpose: account-level detail behind each Balance Sheet line.
- Params: Format ID (lov, M). As Of Date (date, M).
- Columns: Particulars | Balance Last Year | Balance This Year.
- Structure: same format as FIN-MIS-BS. Under each format line, every mapped GL account is listed (account name). Group totals follow (for example "TOTAL Cash and bank balances").
- Calc: as FIN-MIS-BS, but per account.

#### FIN-MIS-IE-SCH – Schedule to Income and Expense Statement (Src FMI12)
- Purpose: compare this period's income and expense with YTD and with last year.
- Params: Format ID (lov, M). From Date (date, M). To Date (date, M).
- Columns (sample): Particulars | This Period | This Year | Last Year. The text lists only Particulars, This Period and This Year.
- Structure: account-level lines under format groups. Computed lines follow: Absolute Gross Balance, True Gross Balance, Net U/W Profits Total, Net Investment Income, Profit before Except Items, Profit before Tax, TOTAL, taxes, Net Retained.
- Calc: This Period = Σ(Cr − Dr) for income and Σ(Dr − Cr) for expense between From and To (inferred sign). This Year = FY start → To Date. Last Year = the same From→To window one year earlier (inferred).

#### FIN-MIS-IE – Income and Expense Statement (Src FMI11)
- Purpose: surplus or deficit for the period, with YTD, against last year.
- Params: Format ID (lov, M). From Date, To Date (date, M). Rounding Option (option, M).
- Columns: Last Year This Period | Last Year Year-To-Date | Particulars | This Year This Period | This Year Year-To-Date.
- Structure: format-group lines (not accounts). Totals as in FIN-MIS-IE-SCH.
- Calc: TY This Period = From→To. TY YTD = FY start→To. LY columns = the same windows shifted back one year (inferred). Surplus = income − expense.

### 2.2 General Ledger – Trial Balances (Accounts-in-Charge – strategic)

#### FIN-TB-MAIN – Main Accounts Trial Balance (Src FGL012)
- Purpose: monthly closing balances of main accounts. Proves arithmetical accuracy (double entry).
- Params: Calendar Month (month, M). Calendar Year (year, M).
- Columns: Account | Account Name | Opening Balance (Dr/Cr) | This Month Transactions: Debits, Credits | Closing Balance: Debits, Credits.
- Sort: main account code. Grand **Total** row (Opening net, Σ Debits, Σ Credits, Σ Closing Dr, Σ Closing Cr).
- Calc: R-OPEN (as of the first day of the month), R-TB. Period 13 is included when month = 12 or 13 (inferred).

#### FIN-TB-DIVDEPT – Main Accounts Trial Balance by Division/Department (Src FGL013)
- Purpose: monthly closing balances by Division, Department and Main A/c.
- Params: Calendar Month (month, M). Calendar Year (year, M). Division Code (range, O). Department Code (range, O). Main A/c Code (range, O). Order By (option{Division, Main A/c}, M).
- Columns: Division | Department | Account | Account Name | Opening Balance Dr/Cr | This Month Debits | Credits | Closing Debits | Closing Credits.
- Grouping:
  - Order By = Division: Division → Department → Account, with a **Department Total**, a **Division Total** and a **Grand Total** (the sum of the division totals).
  - Order By = Main A/c: Main A/c → Division, with a sub-total per main account and a grand total.
- Calc: R-TB per (division, department, account) combination.

#### FIN-TB-SUB – Sub Account Trial Balance (Src FGL014)
- Purpose: monthly closing balances by sub-account. Only main accounts that have sub-accounts are included (for example Sundry Creditors and Debtors).
- Params: Calendar Month (M). Calendar Year (M). Division Code (lov/range, O). Department Code (O). Main A/c Code (range, O). Sub A/c Code (range, O). Order By (option, O).
- Columns: Account Code (sub) | Account Name | Opening Balance Dr/Cr | This Month Debits | Credits | Closing Debits | Closing Credits.
- Grouping: header "Main A/c Code nnn – A/c Name", then the sub-account rows, then **Total**, then **Sub Account Closing Bal** (net, Dr/Cr).
- Calc: R-TB per sub-account. Σ sub-accounts must equal the control account balance. A mismatch is flagged (inferred).

#### FIN-TB-POSTUNP – Main A/C Trial Balance (Posted and Unposted) (Src FR2174)
- Purpose: TB that includes unposted (saved but not posted) vouchers, to check balances before posting.
- Params: Calendar Month (M). Calendar Year (M). Show A/c Not in Use (flag, O). The echo prints "Selection: A/c With Transaction During Period" or "All Accounts". Transaction Status (option{Posted, Unposted, Both}, default Both) (inferred from "posted and unposted summary separately").
- Columns: as FIN-TB-MAIN.
- Calc: R-TB over the chosen statuses. When the flag is off, accounts with no transactions in the period are hidden.

#### FIN-TB-YTD – Main A/C Trial Balance with Annual Figure (YTD) (Src FR2393)
- Purpose: month and year-to-date movement per main account.
- Params: Calendar Year YYYY (M). Calendar Month MM (M).
- Columns: Main Account | Account Name | Year Opening Dr/Cr | MTD-Dr | MTD-Cr | YTD-Dr | YTD-Cr | YTD-Closing Dr/Cr.
- Calc: Year Opening = balance at FY start. MTD = selected month. YTD = FY start → month end. YTD Closing = Year Opening + YTD-Dr − YTD-Cr (R-BAL). A grand total is printed (inferred).

### 2.3 Petty Cash (Accounts-in-Charge – strategic)

#### FIN-PC-PENDING – Petty Cash Disbursements Pending Reimbursement (Src FGL018)
- Purpose: list disbursement vouchers per petty-cash box that are not yet reimbursed, to follow up.
- Params: Petty Cash Number (range/lov, M). As-of Date (date, O, default today) (inferred).
- Columns (3-line header):
  - L1 (box): Petty Cash Number | Name | Cash A/c and Name | Division | Department | Box Limit | Disbursement Amt | Closing Balance.
  - L2 (analysis): Anly-1 | Anly-2.
  - L3 (detail): Date | Reference | Account Code | Sub A/c Code | Account Name | Division | Department | Anly-1 | Anly-2 | Disb. Amount | User ID | Date (entry).
  - L4: Transaction Code | Document No | Narration.
- Grouping: box → expense account caption → vouchers. Grand Total of Disb. Amount.
- Calc: Disbursement Amt = Σ pending disbursements. Closing Balance = Box Limit − pending disbursements (inferred). Pending days = As-of − disbursement date (inferred; the book says the report shows how long items have been pending).

#### FIN-PC-REIMB – Petty Cash Reimbursements during Period (Src FGL019)
- Purpose: reimbursements received during a period (basis: document date), with the delay after disbursement.
- Params: Document Date From/To (dateRange, M).
- Columns:
  - Box line: Petty Cash Number | Name | Cash Main A/c Code | Cash Main A/c Name.
  - Reimbursement line: Reimbursement Bank Code | Reimbursement Bank Name | Transaction Code | Document Number | Document Date.
  - Disbursement detail: Date | Reference | Main A/c | Sub A/c | Account Name | Division | Department | Reimbursement Amount | User Id | Date.
- Grouping: box → reimbursement document (Total per document). Grand total (inferred).
- Calc: Delay days = reimbursement document date − disbursement date. Print it per detail line (inferred placement).

### 2.4 General Ledger – operational (Accounts-in-Charge)

#### FIN-GL-TXNLIST – List of Transactions Detailed (Src FR2183)
- Purpose: account-wise transaction detail by status and due date, in LC and FC.
- Params: Transaction Code (range, O). Document Number (range, O). Document Date (dateRange, M). Main A/c Code (range, O). Sub A/c Code (range, O). Order By (option{Document Date, Document Number}, M). User ID (lov, O). Transaction Status (option{Posted, Unposted, Both}, M). Combine Transactions (flag, O).
- Columns:
  - Voucher line: Document Date | Transaction Code | Document No. | Narration | Document Due Date | Document Ref Date | Document Ref.
  - Entry line: Main Account Code | Sub Account Code | Account Name | Divn Code | Dept Code | Activity Code 1 | Activity Code 2 | Anly Code 1 | Anly Code 2 | Currency Code | FC Val | LC Val | Dr/Cr | Status (P/U).
  - Entry sub-line: Description (line narration) | Doc Reference.
- Grouping: Transaction Code → voucher. **Transaction-wise Summary** per TC: Total No. of Vouchers, Total No. of Entries, Total Amt Credit, Total Amt Debit. **Report-wise Summary** with the same four figures.
- Calc: Combine Transactions = Y merges the lines of one voucher that share account, sub-account, division, department, analysis and activity codes and Dr/Cr (inferred).

#### FIN-GL-LEDGER-LC – General Ledger (LC) (Src FGL010B)
- Purpose: local-currency ledger per main account for a period.
- Params: Level (option{Company, Division, Department}, M). Document Date (dateRange, M). Main A/c Code (range, O). Division Code (range, O). Department Code (range, O). Include 13th period (flag) (inferred from the header).
- Columns: Main A/c Code | Main A/c Name. Then Document Date | Document Number | Reference Number | Division | Department | Narration | LC Debits | LC Credits | Balance Amount Dr/Cr.
- Grouping: per main account (per division or department when Level ≠ Company). Rows in order: **Opening Balance** (Dr, Cr, net), transaction lines, **Control account summary** (Dr, Cr), **Total transactions**, **Closing balance**.
- Calc: R-OPEN, R-RUN. For control accounts, sub-ledger postings are collapsed into one "Control account summary" Dr/Cr line instead of detail (inferred). Closing = opening + totals.

#### FIN-GL-SUBLEDGER-LC – Sub Ledger (LC) (Src FGL011B)
- Purpose: local-currency ledger per sub-account (for example Sundry Creditors / Supplier D).
- Params: Level (option, M). Document Date (dateRange, M). Main A/c Code (range, O). Sub A/c Code (range, O). Division Code (range, O). Department Code (range, O).
- Columns: Sub A/c | Sub A/c Name. Then Document Date | Document Number | Reference | Division Code | Department Code | Narration | Debits | Credits | Balance Amount Dr/Cr.
- Grouping: **Control A/c** (main) → Sub A/c. Per sub-account: Opening Balance, lines, **Sub A/C Total**, Closing Balance. Per control account: **\*\* GROUP TOTAL \*\***. At the end: **\*\* GRAND TOTAL \*\***.
- Calc: R-OPEN, R-RUN.

#### FIN-GL-CONSSUM – Consolidated Account-wise Summary (Src FGL009)
- Purpose: main and sub-account summaries by day and transaction status.
- Params: Transaction Code (range, O). Date (dateRange, M). Main A/c Code (range, O). Sub A/c Code (range, O). Amount Over Limit (amount, O, 0 = all). Summary basis flags: Daily Summary (flag), All Transaction (flag). Posted/Unposted (option{Posted, Unposted, Both}, M).
- Columns: Main A/c | Sub A/c | Account Description | Date | Debit Amount | Credit Amount | Net Amount | Dr/Cr.
- Grouping: Main A/c → Sub A/c (→ Date when Daily Summary = Y). **Main Account Total**. Total. **REPORT TOTAL**.
- Calc: Net = R-BAL of Dr − Cr. Only transactions with amount > Amount Over Limit are included. Daily Summary = N gives one row per sub-account for the whole range (inferred).

#### FIN-GL-MISSVCH – Missing Voucher Number List (Src FGL015)
- Purpose: find gaps in document numbering.
- Params: Transaction Code (range, O). Document Date (dateRange, M).
- Columns: Transaction Code and Description (group line) | Missing From Document Number | Missing To Document Number | No. of Documents.
- Grouping: per transaction code, gaps in ascending order.
- Calc: within each numbering series, sort the existing numbers. For each consecutive pair (a, b) with b > a+1, emit From = a+1, To = b−1, Count = b−a−1. The book's example: 1 and 3 exist, so 2 is missing.
  - **The series key must include the number prefix (TC + year/period).** Numbers look like `YYYYMM`+seq, and the sample prints a bogus count of 181,180,807 when it jumps across series (inferred).
  - Cancelled or voided vouchers count as present, not missing (inferred).

#### FIN-GL-ALLOCJV – Allocation JV Details (Src FR2189)
- Purpose: expense JVs allocated from prepaid accounts over allocation periods.
- Params: Prepaid Main A/c (range, O). Date (dateRange, M) (allocated date).
- Columns: Allocated JV (TC-DocNo) | Expense Main A/c | Expense Main A/c Name | Expense Sub A/c | Expense Division | Expense Department | Allocated From Date | Allocated To Date | Allocated Date | LC Value.
- Grouping: "Prepaid A/c Name: code – name", then lines, then **Prepaid A/c Total**. **Grand Total**.
- Calc: include allocation JVs whose allocated date is within the range. Each JV is the periodic amortisation: Dr expense, Cr prepaid (inferred).

#### FIN-GL-LEDGER-FC – General Ledger (FC) (Src FGL010A)
- Purpose: foreign-currency ledger per main account.
- Params: as FIN-GL-LEDGER-LC.
- Columns: Main A/c Code and Name | Currency code. Then Document date | Txn Code | Document Number | Reference | Division | Dept | Narration | Debits | Credits | Balance amount Dr/Cr (FC).
- Grouping and calc: as the LC ledger, but per (main a/c, currency). FC amounts in different currencies are never summed together (inferred).

#### FIN-GL-SUBLEDGER-FC – Sub Ledger (FC) (Src FGL011A)
- Purpose: foreign-currency ledger per sub-account (for example Sundry Debtors / Customer A).
- Params: as FIN-GL-SUBLEDGER-LC.
- Columns: Sub A/c Code | Sub A/c Name | Currency Code. Then Document Date | TC | Document Number | Reference | Division | Dept | Narration | Debits | Credits | Balance Amount Dr/Cr.
- Grouping: Control A/c → Sub A/c (+ currency). Each has Opening Balance, Total transactions and Closing balance.

### 2.5 Cashier and Accountant – operational

#### FIN-CB-POSITION – Bank/Cash Position Report (LC) (Src FGL007A)
- Purpose: account-wise opening, deposits, withdrawals and closing for bank and cash accounts in a period.
- Params: Document Date (dateRange, M). Division Code (lov, O). Department Code (lov, O). Main A/c Code (range, O). Sub A/c Code (range, O).
- Columns: Bank Code (main a/c) | Bank Name | Sub A/c code / name | Opening Balance Dr/Cr | Deposits | Withdrawals | Closing Balance Dr/Cr | Currency | FC Value Dr/Cr.
- Grouping: bank or cash main account → sub-account/currency rows, then **Totals** per main account and **Grand Totals**.
- Calc: Deposits = Σ Dr, Withdrawals = Σ Cr in the range. Closing = Opening + Deposits − Withdrawals. Only accounts flagged as Bank or Cash type are included (inferred).

#### FIN-GL-VOUCHER – Voucher (Voucher Listing) (Src FGL001)
- Purpose: print posted or unposted vouchers by transaction code and document number, to check them against source registers.
- Params: Transaction Code (range, O). Document Number (range, O). Document Date (dateRange, M). Voucher Status (flags Posted / Unposted, M ≥ 1). User ID (lov, O).
- Layout (one voucher per block or page, inferred):
  - Header: Document Number (TC + No) | Document Date | Document Ref | Status | Narration.
  - Lines: Main A/c | Main A/c Description | Sub A/c | Sub A/c Description | Divn | Dept | Anly-1 | Anly-2 | Acty-1 | Acty-2 | Currency | FC Amt | Amount DR/CR. Then the line Narration.
  - Footer: Amount in Words (currency name + words) | Total Amount | Entered by | Date Entered | Authorised by | Date | Approved by | Date (signature blocks).
- Calc: Total Amount = Σ Dr (= Σ Cr). Amount in words uses the voucher currency.

#### FIN-GL-PROCLIST – List of Processed/Unprocessed Transactions (Src FGL002 "Transaction Listing-Posted/Unposted")
- Purpose: list posted and/or unposted documents.
- Params: Transaction Code (range, O). Document Number (range, O). Document Date (dateRange, M). Main Account Code (range, O). Sub Account Code (range, O). Order By (option{Document Date, Document Number}, M). Combine Transactions (flag, O). Status (option{Posted, Unposted, Both}, M).
- Columns:
  - Voucher line: Document Date | Transaction Code | Document Number | Narration | Reference Date | Reference.
  - Entry line: Main A/C | Sub A/C | Division | Department | Acty 1 | Acty 2 | Anly 1 | Anly 2 | Due Date | Currency | FC Value | LC Value | Dr/Cr | Status.
  - Sub-line: Account Name | Order By value.
- Grouping and summary: as FIN-GL-TXNLIST (Transaction-wise Summary and Report-wise Summary: vouchers, entries, total Cr, total Dr).
- Note: functionally the same as FIN-GL-TXNLIST. **Implement one engine with two layouts** (inferred).

#### FIN-GL-DAYBOOK – Journal and Other Day Books (Src FGL008)
- Purpose: posted transactions only, as journals and day books.
- Params: Transaction Code (range, O). Document Date (dateRange, M). Document Number (range, O). Combine Type (flag, O). User ID (lov, O).
- Columns:
  - Line 1: Document Date | Transaction Code | Document Number | Main A/c / Account Name | Sub A/c / name | Division Code | Department Code | Debits | Credits.
  - Line 2: Document Reference | Narration | Analysis Code 1 | Analysis Code 2 | Activity Code 1 | Activity Code 2.
- Grouping: per transaction code, which is the book (for example the Cash Book or Journal). Combine Type = Y prints all TCs as one book (inferred). Footer: TOTAL NO. OF VOUCHERS, TOTAL NO. OF ENTRIES. Σ Dr = Σ Cr per voucher and per book (inferred).
- Filter: status = Posted.

### 2.6 Activity analysis (Accounts-in-Charge – strategic)

Activity codes are an analysis dimension with a hierarchy: Main Head 1 / Main Head 2 → Main Activity → Sub Activity (ACT1/ACT2 on voucher lines).

#### FIN-ACT-SUM2 – Activity Analysis Report Summary II (Src FMI023B)
- Purpose: main-account totals by main activity head for divisions and departments in a period.
- Params: Document Date (dateRange, M). Main A/c Code (range, O). Division Code (range, O). Department Code (range, O). Main Activity Head (lov, M). Combine All Main A/cs (flag). Main Head Number (option{Main Head 1, Main Head 2}, M).
- Columns: Main Acty | Main Acty Name | Currency | FC Amount | LC Amount Debits | LC Amount Credits.
- Grouping: Division → Department → Main A/c Code (omitted when Combine = Y) → activity rows. Subtotals: **Total** per main a/c, **Department Total**, **Divisional Total**.
- Calc: Σ over lines whose activity code (ACT1 or ACT2, chosen by the Main Head Number) is under the selected head.

#### FIN-ACT-DET – Activity Analysis Report Detailed (Src FMI024)
- Purpose: transaction detail for each main account by main head and sub head.
- Params: Document Date (dateRange, M). Main A/c Code (range, O). Main Head (lov, M). Sub Head (range/lov, O). Division Code (range, O). Department Code (range, O). Combine All Main A/cs (flag). Main Head Number (option{1, 2}, M).
- Columns: Main A/C | Main Account Name. Main Activity Name block: Doc Date | Type (voucher type, for example Dnote or Payment) | TC | Doc No. | Narration. Then Sub Activity Name block: Cur | FC Amount. Then LC Amount Debits | Credits.
- Grouping: Division → Department → Main A/c → Main Activity → Sub Activity, with "Totals for <x>" at every level.

### 2.7 Accounts Payable (AP-in-Charge – operational)

#### FIN-AP-AGE-SUM – Creditors Aged Analysis – Summary (Src FAP008)
- Purpose: creditor ageing by supplier, main a/c or sub a/c as of a date, with On A/c and Net.
- Params:
  - Selection (option{By Parent, All Account-wise, Both}, M).
  - Parent Supplier Code (range, O). Main A/c Code (range, O). Sub A/c Code (range, O).
  - As Of Date (date, M).
  - Order By (option{Document Date, Due Date}, M). This is the ageing basis.
  - Currency (option{Base, Foreign}, M).
  - Ageing slots (int×5, O, default 30/60/90/120) (inferred).
- Columns: Main A/c | Sub A/c / Account Name | Currency | ≤30 Days | 31-60 | 61-90 | 91-120 | ≥121 Days | On a/c | Net Val.
- Grouping: Parent supplier (when By Parent) → main a/c → sub a/c. **Total** row.
- Calc: R-OPENITEM, R-AGE, R-ONAC, R-FX. Payables show credit balances as positive (inferred from the sample).

#### FIN-AP-AGE-DET – Creditors Aged Analysis – Detailed (Src FAP009)
- Purpose: transaction-level creditor ageing with due dates and days pending.
- Params: as FIN-AP-AGE-SUM. The echo prints From/To Main, Sub, As Of Date, Order By and "Report Based On: Base Currency".
- Columns:
  - Group line: Main A/c | Sub A/c | Name | Currency.
  - Detail: Document (TC-DocNo) | Doc Date | Due Date | Reference (for example the policy or claim no.) | O/S Amount | On Account | ≤30 | 31-60 | 61-90 | 91-120 | ≥121 | Days pending (inferred placement).
- Grouping: sub-account, then **Sub Account Total**, then report **Total**.
- Calc: per document, O/S = balance. A debit or unallocated item goes to On Account, otherwise to the bucket by age. O/S = On Account + Σ buckets.

#### FIN-AP-SOP – Statement of Payables (Base Currency) (Src FAP007A "Statement of Accounts")
- Purpose: supplier statement of invoices, payments and balance, with PDCs and ageing (LC transactions).
- Params: Parent Supplier Code (range, O). Main Account (range, O). Sub Account (range, O). Currency (lov, M). As Of Date (date, M). Ageing Slots 1-5 (int, O). Order By (option{Due Date, Document Date}, M).
- Layout (one statement per supplier account):
  - Title: "Statement of Accounts", currency name, "As Of <date>".
  - Address block: To | Contact | Address 1–5 | City | State | Internal Reference Number (main a/c + sub a/c).
  - Columns: Document Date | Due Date | Document Reference | Document Number | Debit | Credit | Original Amount Dr/Cr | Balance Amount | PDC Date | PDC Number | PDC Amount.
  - After the lines: **Net Balance** (Dr/Cr).
  - **Ageing of Outstandings**: 0-30 | 31-60 | 61-90 | 91-180 | 181-365 | Over 365 Days | On Account | Net Balance.
  - **PDC Cheques** | **Balance Net of PDC**.
- Group: Parent supplier → supplier account.
- Calc: Balance per open item (R-OPENITEM). Net Balance = Σ balances. PDC Amount = PDCs issued against the item and not yet confirmed. Balance Net of PDC = Net Balance − Σ PDC. Ageing: R-AGE, R-ONAC.

#### FIN-AP-SUPOS – Supplier Outstanding Summary (Src FR2526)
- Purpose: outstanding by main account per company and supplier, with ageing. Compares companies.
- Params: Company (range, O). Division (range, O). Department (range, O). Main A/c Code (range, O). Sub A/c Code (range, O). Analysis Code 1 (range, O). Analysis Code 2 (range, O). Date (date, M, as-of).
- Columns: Main Account Code | Main Account Name | Supplier Code | Supplier Name | Phone No. | Amount | On A/c | Age Period in Days <30 | 31-60 | 61-90 | 91-120 | >120.
- Grouping: Company → main a/c → supplier. Totals per company (inferred) and a grand total.
- Calc: R-AGE (text limits 0/30/45/60/75+, sample 30/60/90/120: configurable), R-ONAC. Amount = net outstanding.

### 2.8 Accounts Receivable (AR-in-Charge – operational)

#### FIN-AR-AGE-DET – Debtors Aged Analysis – Detailed (Src FAP009)
- Purpose: debtors in a parent customer group, per main account, with transaction-level ageing.
- Params: Selection (option{By Parent, All, Both}, M). Parent Customer Code (range, O). Main Account Code (range, O). Sub Account Code (range, O). As Of Date (date, M). Order By (option{Document Date, Due Date}, M). Currency (option{Base, Foreign}, M).
- Columns:
  - Group line: Main A/c Code | Sub A/c Code | Account Name | Currency.
  - Detail: Tran Code (TC-DocNo) | Document Date | Due Date | Reference | O/S Amount | On Account | ≤30 | 31-60 | 61-90 | 91-120 | >120 Days.
- Grouping: Parent customer → sub-account (**Sub Account Total**) → **Total**.
- Calc: as FIN-AP-AGE-DET, with the debit side positive.

#### FIN-AR-SOO – Statement of Outstanding (Src FAP007A)
- Purpose: customer statement of account in LC (FC items converted), with ageing.
- Params: Parent Customer Code (range, O). Main Account Code (range, O). Sub Account Code (range, O). As Of Date (date, M). Ageing Slot 1-5 (int, O). Order By (option{Due Date, Document Date}, M). Currency basis (option{Base, Foreign}, O).
- Layout: identical to FIN-AP-SOP (same program). It has the address block, Internal Reference Number and these columns: Document Date | Due Date | Document Reference | Document Number | Debit | Credit | Original Amount Dr/Cr | Balance Amount | PDC Date | PDC Number | PDC Amount. Then Net Balance, the ageing block and PDC Cheques / Balance Net of PDC.
- Calc: R-FX (FC to INR/LC per item). PDCs = PDCs *received* against the customer and not yet deposited or realised.

#### FIN-AR-SOO-FC – Statement of Outstanding (Foreign Currency) (Src FAP007)
- Purpose: statement for a parent customer or supplier in one foreign currency (for example USD), by account or internal reference.
- Params: Parent Customer Code (range, O). Main Account Code (range, O). Sub Account Code (range, O). As Of Date (date, M). Currency Code (lov, M). Ageing Slot 1-5 (int, O). Order By (option, M).
- Layout:
  - Header: currency name, then To | Contact Person | Address | Internal Reference No. (main + sub).
  - Columns: Document Date | Due Date | Document Reference | Transaction Code (TC-DocNo) | Debit | Credit | Original Amount Dr/Cr | Balance Amount | PDC Date | PDC Cheque No. | PDC Amount.
  - **Ageing of Outstandings (in days)**: 0-30 | 31-60 | 61-90 | 91-180 | 181-365 | Over 365 | On Account | Net Balance.
  - **Group Net Balance** (parent group).
- Calc: amounts in FC, no conversion. R-AGE, R-ONAC. Group Net = Σ account net balances under the parent.

#### FIN-AR-AGE-SUM – Debtors Aged Analysis – Summary (Src FAP008)
- Purpose: net balance per customer account or group on a date, with ageing buckets.
- Params: Parent Customer Code (range, O). Main Account Code (range, O). Sub Account Code (range, O). As Of Date (date, M). Order By (option{Document Date, Due Date}, M). Currency (option{Base, Foreign}, M).
- Columns: Main A/c | Sub A/c / Account Name | Currency | bucket1 … bucket5 (sample ≤30/31-60/61-90/91-120/≥121, or ≤90/91-180/…/≥181) | On a/c | Net Val.
- Grouping: Parent → main → sub. Total.
- Calc: R-AGE, R-ONAC, R-FX.

#### FIN-AR-CHQ-RCPT – Payment Received for Invoices through a Cheque Number (Src FR2390)
- Purpose: register of non-PDC cheques received from debtors and the invoices they settled.
- Params: Document Number (range, O). Document Date (dateRange, M). Bank Code (range, O). Customer Code (range, O).
- Columns: Sl.No. | Customer Code / Customer Name | Cheque No / Cheque Date | Cheque Amount | Cust. Bank Name | Bank Name / Account No (company bank) | Document No / Document Date (receipt) | Invoice No / Invoice Date | Currency Code | Invoice Amount | Adj. Amount.
- Grouping: one numbered entry per cheque (receipt), with one row per invoice matched to it. Sorted by customer, then cheque (inferred).
- Calc: Adj. Amount = the amount of this receipt knocked off against the invoice. Invoice Amount − Σ settlements = the difference, which includes credit-note and cash-discount adjustments (the book's wording is ambiguous; this reading is inferred). PDC receipts are excluded.

#### FIN-AR-CHQ-UNDEP – Cheques Received but Not Deposited (Src FR2392)
- Purpose: non-PDC cheques received and not yet deposited, as of a date.
- Params: Document Number (range, O). As of Date (date, M).
- Columns: Cheque Number | Cheque Date | Document Number | Document Date | Bank Code / Bank Name (company bank a/c) | Customer Code / Customer Name | Currency Code | Cheque Amount FC | Cheque Amount LC.
- Sort: by document number.
- Filter: receipt instrument = cheque (not PDC), cheque date ≤ As-of, no deposit (bank pay-in) recorded as of the As-of date.
- Grand totals FC (per currency) and LC (inferred).

#### FIN-ARAP-SOA-MATCH – Statement of Account with Matched/Un-Matched Details (Debtors/Creditors) (Src FR2545)
- Users: AR and AP departments.
- Purpose: customer or supplier transactions in a period, split into matched and unmatched. Used for next month's SOA reconciliation.
- Params: Main A/c Code (range, O). Sub A/c Code (range, O). Date From/To (dateRange, M). Currency (option{Foreign as-is, Local converted}, M).
- Layout:
  - Header: To | Contact | Address 1-3 | Country | Phone No. | Fax | From/To Date | Internal Reference (main + sub). Currency Code per the text.
  - Columns: Document Date | Document Reference | Transaction Code | Cheque Number | Cheque Date | Debit Amount | Credit Amount | Original Amount | Balance Amount.
  - Sections: **Matched Details**, then **Net Balance**. **Unmatched Details**, then **Net Balance**. Finally **Grand Total** (Dr/Cr).
- Calc: Matched = items fully knocked off (balance 0) within the period. Unmatched = balance ≠ 0. Section Net Balance = Σ Dr − Σ Cr (Balance column). Grand = unmatched net (inferred).

#### FIN-AR-AGE-DIV – Debtors Aged Analysis – Summary – Division-wise (Src FAP008 layout)
- Purpose: FIN-AR-AGE-SUM grouped by division.
- Params: Parent Customer Code (range, O). Main Account Code (range, O). Sub Account Code (range, O). As of Date (date, M). Order By (option{Doc Date, Due Date}, M). Currency (option{Base, Foreign}, M). Division (range, O) (inferred).
- Columns: Main A/c | Sub A/c / Account Name | Currency | age buckets ("Number of Days") | On A/c | Net Value.
- Grouping: **Division** → main → sub, with a Division total and a Grand total. The division comes from the document line's division code (inferred).
- Note: the book's sample page is the plain FAP008 page with no division break.

### 2.9 Bank Reconciliation (Accounts-in-Charge – operational)

#### FIN-BRS-UNREC-BOOK – Un-reconciled Book Entries (Src BR001)
- Purpose: book (ledger) entries on bank accounts that are not matched to the bank statement, with days outstanding. Used to chase long-pending items (for example a policy of no more than 60 days).
- Params: Bank Code (range, O). Bank Account Number (range, O). As of Date (date, M). Detail Required (flag, O).
- Columns: Main A/C | Sub A/C | Doc Date | Doc. Narration | Doc No. | Doc. Reference | Debit | Credit | Original Amount Dr/Cr | Balance Dr/Cr | Currency | Days.
- Grouping: Bank Code, Bank Name, Bank A/c No., then the lines. Totals per bank (inferred).
- Calc: include book lines on the bank GL account with doc date ≤ As-of and not reconciled as of the As-of date. Balance = original − partially reconciled amount. **Days = As-of − Doc Date** (sample: 01/05 − 01/04 = 30).

#### FIN-BRS-UNREC-BANK – Un-reconciled Bank Entries (Src BR002)
- Purpose: bank statement entries not matched to the books, with days outstanding.
- Params: Bank Code (range, O). Bank Account Number (range, O). As of Date (date, M). Detail Required (flag, O).
- Columns: Main A/C | Sub A/C | Doc Date | Doc Narration | Doc No. | Doc Ref | Debit | Credit | Org. Amount Dr/Cr | Balance Dr/Cr | Days.
- Detail sub-rows (when Detail Required = Y): Sno | Bank A/c No. / date | Bank Name / reference | Dr/Cr | Tran Code | Bank Code. These are the partial-match candidates or split statement lines (inferred).
- Grouping: Bank Code / Bank Name / Bank A/c No.
- Calc: Days = As-of − statement value date.

#### FIN-BRS-STMT – Bank Reconciliation Statement (Src BR003)
- Purpose: reconcile the book balance with the bank balance as of a date, listing the reconciling items.
- Params: Bank Code (lov, M). Bank Account Number (lov, M). As of Date (date, M).
- Header: Bank | Bank A/c No.
- Columns:
  - Line 1: Doc. Date | Tran. Code | Doc. No. | Description | Amount.
  - Line 2: Chq. Date | Chq. No. | Reference | Narration.
- Sections, in order:
  - **Book Balance** (Dr/Cr).
  - 1. Book Debit Entries not accounted by Bank (subtotal).
  - 2. Book Credit Entries not accounted by Bank (subtotal).
  - 3. Bank Debit Entries not accounted in Book (inferred).
  - 4. Bank Credit Entries not accounted in Book (inferred).
  - **Balance as per Bank Statement** (inferred).
- Formula (book-side sign, Dr = money in bank) (inferred):
  `Bank balance = Book balance − (1) + (2) − (3) + (4)`.
  Here (3) is bank charges or debits missing from the book, and (4) is bank receipts or credits missing from the book.
  The computed bank balance must equal the closing balance of the imported statement as of the date. Any difference is printed as "Unexplained difference".

#### FIN-BRS-PAYNOTIFY – Payment Notification to the Bank (Src –, text file)
- Purpose: a text-format file advising the bank of supplier payments, so the bank can clear issued cheques (positive pay).
- Params: Bank Code (lov, M). Transaction Code (range, O). Date From/To (dateRange, M). File Name (text, M).
- Output fields: Vendor Code | Bank Account Number | Amount. The sample also shows the cheque/document number, the date (DDMMYYYY) and the supplier name.
- Format: fixed-width, no delimiters. Record type `01` = payment detail and `02` = trailer (count + total amount) (inferred from the sample lines). Numbers are zero-padded. The book does not document the field widths; make the layout configurable per bank (inferred).

### 2.10 Post-Dated Cheques (AR-in-Charge – operational)

PDC lifecycle for BrokerVerse (inferred from the report semantics):
- Received PDC: `Received (on hand)` → `Deposited/Banked` → `Realised` | `Returned/Bounced` | `Cancelled/Replaced`.
- Issued PDC: `Issued` → `Presented/Confirmed` (the confirmation voucher posts Dr PDC-issued clearing, Cr Bank) | `Cancelled/Stopped`.
- Status tests use the status history as of the As-of date.

| FIN code | Title (Src) | Params (all M unless noted) | Columns (order) | Filter / calc | Grouping and totals |
|---|---|---|---|---|---|
| FIN-PDC-RCV-ONHAND | PDC (Received) on Hand (FPD001) | As Of Date | Due Date / Narration · Cheque No. · Customer Account Main · Sub · Customer Name / Bank Name (drawee) · Currency · Cheque Amount · LC Value · Receipt No. · Date | Received PDCs with receipt date ≤ As-of and status on hand as of that date. Includes items pending deposit. | Sort by due date. **Total LC Amount** |
| FIN-PDC-RCV-PERIOD | PDC Received during a Period (FPD002) | From Date, To Date (receipt date) | as above (LC Amount) | Receipt date within the range, any status. Shows cheques still to be realised. | Sort by due date. Total (inferred) |
| FIN-PDC-RCV-DUEBANK | PDC Due to be Banked (FPD003) | As Of Date | as FPD001 | On hand and due date ≤ As-of (inferred: "due to be banked as of date") | **Grand Total** |
| FIN-PDC-ISS-PERIOD | PDC Issued during Period (FPD004) | From Date, To Date | Due Date · Cheque No. · Issued to Account Main · Sub · Account Name · Currency · Cheque Amount · LC Amount · Issue No. · Date | Issue date within the range. FC is converted to INR/LC (R-FX). | By **Bank Code / Bank Name** (paying bank), with a Total per bank. Grand total (inferred) |
| FIN-PDC-ISS-DUEPAY | PDC Issued Due for Payment (FPD005) | As of Date | Bank group line: Bank Code · Bank Name. Detail: Due date · Cheque No. · Issued to Account Main · Sub · Account Name · Currency · Cheque Amount · LC Amount · Receipt(Issue) No. · Date | Issued, not yet presented or confirmed as of the As-of date. Used to keep enough bank balance and avoid bounced cheques. | By bank. **Total** |
| FIN-PDC-RCV-ONHAND-DDB | PDC (Received) on Hand by Division/Department and Bank (FR2581) | As-of Date. Division, Department, Main A/c, Sub A/c, Bank Main A/c, Bank Sub A/c (range, O) | Due Date / Narration · Cheque No. · Customer Account Main · Sub · Customer Name / Bank Name · Bank Sub Account · Currency · Cheque Amount (FC) · LC Value · Receipt No. / Date | as FPD001. Bank Main/Sub = the bank account the cheque will be deposited in (the customer's bank name and a/c no. per the text) | Division → Department → Bank, with Bank, Department, Division and Grand totals (inferred, same as FR2582) |
| FIN-PDC-RCV-PERIOD-DDB | PDC Received during the Period by Division/Department and Bank (FR2582) | From/To Date + the same 6 ranges | as FR2581 (LC Amount) | as FPD002 | **Bank Total**, **Department Total**, **Division Total**, Grand Total |
| FIN-PDC-RCV-DUEBANK-DDB | PDC Due to be Banked by Division/Department and Bank (FR2583) | As Of Date + 6 ranges | as FR2581 | as FPD003. Select by customer a/c range or by bank a/c range | Div → Dept → Bank totals |
| FIN-PDC-ISS-PERIOD-DDB | PDC Issued during a Period by Division/Department and Bank (FR2584) | From/To Date + 6 ranges | Due Date · Cheque No. · Issued to Account Main · Sub · Account Name · Bank Sub Account · Currency · Cheque Amount (FC) · LC Amount · Issue No. · Date | as FPD004. Includes both still due and confirmed PDCs | **Bank Total**, **Department Total**, **Division Total**, **Grand Total** |
| FIN-PDC-ISS-DUEPAY-DDB | PDC Due for Payment by Division/Department and Bank (FR2585, inferred ID) | As-of Date + 6 ranges | Division · Department · Due Date · Cheque No. · Account Name · Currency · Cheque Amount · LC Amount · Receipt(Issue) Number · Bank Code · Bank Name | as FPD005. Also serves as the issued-cheque register per department | Div → Dept → Bank → due date. Totals at each level (inferred) |
| FIN-PDC-CONF-AUDIT-DDB | PDC Confirmation Audit Trail by Division/Department and Bank (FR2586) | From/To Date + 6 ranges | Voucher line: Doc Date · Tran (TC-No) · Bank Date · Due Date · Reference (Cheque No.) · Ref Date · Document Narration. Entry line: Main · Sub · Account Name / Voucher Line Narration · Currency · Anly-2 · Anly-1 · FC Amount · LC Amount Dr/Cr · Acty-2 · Acty-1 · Date | Confirmation vouchers (issued PDC presented and accounted) dated within the range. Used to reconcile bank debits with PDCs issued | Div → Dept → Bank → voucher. Per voucher, a **Created Date Summary**: Total No. of Vouchers, Total No. of Entries, Total Credit in Base Currency, Total Debit in Base Currency |

---

## 3. Data requirements (beyond the core GL)

The core GL already provides the chart of accounts, vouchers and lines, periods and posting. The
reports imply the entities and fields below.
Legend: ☐ = to build. Every entity carries company, division and department keys (inferred).

**Voucher / GL extensions**
- ☐ Voucher header: `transaction_code` (voucher type, for example JV, CN100, DN100, RVB100, PVB100, PRC100, PRE100, PIE100), `document_no`, `document_date`, `document_ref`, `ref_date`, `due_date`, `narration`, `status` (Unposted/Submitted/Approved/Posted), `entered_by`, `entered_at`, `authorised_by/at`, `approved_by/at`, `posted_at`, `user_id`, `period_no` (1–13, **13th adjustment period**), `voucher_kind` (for example Dnote, Payment, Receipt).
- ☐ Voucher line: `main_ac`, `sub_ac`, `division`, `department`, `analysis_1`, `analysis_2`, `activity_1`, `activity_2`, `currency`, `fc_amount`, `exchange_rate`, `lc_amount`, `dr_cr`, `line_narration`, `line_reference` (policy, claim or endorsement no.), `cheque_no`, `cheque_date`.
- ☐ **Document numbering series**: per TC plus prefix (YYYY or YYYYMM) with a last number. Cancelled numbers are retained, which FIN-GL-MISSVCH needs.
- ☐ Account master flags: `has_sub_accounts`/control, `account_type` (Bank, Cash, Petty cash, Prepaid, Debtor, Creditor), `in_use`, `is_bank` + bank link.
- ☐ Sub-account master: code, name, parent main a/c, contact, address 1–5, city, state, country, phone, fax, `parent_group` (parent customer/supplier), credit days (for the due date, inferred).
- ☐ Activity code master: code, name, `main_head_1`, `main_head_2`, sub head (hierarchy).
- ☐ Analysis code masters 1 and 2.
- ☐ Financial Statement Format: `format_id`, description, ordered lines (caption, level, type: header/account-map/total/formula, account ranges, sign, bold). Rounding options.
- ☐ Balances snapshot per (account, sub, div, dept, currency, period), both LC and FC, for the TB and YTD reports (or computed on the fly).

**Accounts Receivable / Payable**
- ☐ Customers and Suppliers as sub-accounts under control accounts, with parent group, contact and address details, phone, and bank details (vendor bank a/c no., which the payment notification needs).
- ☐ Open-item ledger: per AR/AP document line `original_amount` (FC/LC), `balance_amount`, `due_date`, `internal_reference_no`.
- ☐ **Matching / knock-off** records: debit item ↔ credit item, `matched_amount`, `matched_date`, source (receipt, payment, credit note, cash discount, write-off). This drives On A/c, Matched/Unmatched and Adj. Amount.
- ☐ Receipts: receipt no./date, customer, instrument type (cash, cheque, PDC, transfer), cheque no./date, drawee bank name, company bank a/c, amount FC/LC, **deposit status and date** (pay-in slip).
- ☐ Payments: payment no./date, supplier, paying bank a/c, cheque no./date, amount, instrument (cheque, PDC, transfer).
- ☐ Ageing slot configuration: up to 5 boundaries, per report or user default.

**PDC register**
- ☐ PDC record: direction (Received/Issued), receipt/issue no. and date, cheque no., cheque (due) date, party main/sub a/c, drawee bank name (received), company bank main/sub a/c, currency, FC amount, LC amount, rate, division, department, narration, linked invoices (for PDC Amount on statements).
- ☐ PDC status history: Received/Issued → Deposited → Realised/Confirmed (bank date, confirmation voucher ref) | Returned (reason) | Cancelled/Replaced, each with a date and the voucher that posted it.

**Petty cash**
- ☐ Petty cash box: number, name, cash main a/c, custodian, division, department, **box limit** (imprest), current balance.
- ☐ Disbursement voucher: box, date, reference, TC/doc no., expense main/sub a/c, division, department, anly 1-2, amount, narration, user, `reimbursed` flag.
- ☐ Reimbursement: box, TC/doc no./date, reimbursement bank code, linked disbursements, amount, user.

**Bank reconciliation**
- ☐ Bank account master: bank code, bank name, bank a/c no., GL main/sub a/c, currency, branch.
- ☐ Bank statement lines (imported or keyed): bank a/c, value date, reference/cheque no., narration, Dr/Cr, amount, running balance, statement closing balance.
- ☐ Reconciliation match: book line(s) ↔ statement line(s), matched amount, reconciled date, user. Partial matches are allowed (the Balance column).
- ☐ Bank notification file layout configuration per bank (fixed-width field map) and a generation log.

**Allocations / prepaid**
- ☐ Allocation (prepaid amortisation) schedule: source prepaid a/c, source voucher, expense main/sub/div/dept, allocated from/to dates, frequency, per-period amount, generated allocation JV (TC-No), allocated date, LC value.

**Reporting infrastructure**
- ☐ Report registry: FIN code, source ID, version, title, parameter definitions (type, mandatory, default, LOV source), output formats.
- ☐ Run log (user, run time, parameters, pages), printer list, `REP_PATH`, logo per company, schedules and subscriptions (Publish & Subscribe), mail delivery.

---

## 4. Gaps and ambiguities in the source
1. **PDC count**: the book lists 11 PDC reports, not 12.
2. Several text pages have an unfinished "Purpose" ("To provide", "To view the details of the c"): FIN-TB-POSTUNP, FIN-TB-YTD, FIN-GL-MISSVCH, FIN-GL-PROCLIST and FIN-AR-AGE-DIV. Their purposes above come from the analysis text and the samples.
3. The ageing bucket boundaries in the text (10/20/30/40 and 0/30/45/60/75) disagree with the samples, so the slots are configurable (R-AGE).
4. The **FIN-PDC-ISS-DUEPAY-DDB** sample page duplicates FPD005. The FR2585 ID and the division/department grouping are inferred.
5. The **FIN-AR-AGE-DIV** sample page shows no division grouping.
6. **FIN-BRS-STMT** sections 3 and 4 and the closing bank balance are not visible in the sample (page 1 of 2 only).
7. **FIN-BRS-PAYNOTIFY**: the fixed-width layout is not specified.
8. **FIN-AR-CHQ-RCPT**: the definition of "Adjusted amount" conflicts with the sample, where Adj. = Invoice amount.
9. Statement of Outstanding and Statement of Payables mix "Parent Supplier" and "Parent Customer" labels. Both use the FAP007A program, so they are implemented as one engine with a ledger type.
10. The "Last Year" basis for the MIS statements (same date last year, or previous FY end) is not stated.
