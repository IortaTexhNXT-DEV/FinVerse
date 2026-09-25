/**
 * The new BIR outputs of the report pack (FRBS 3.2.0, Appendix A VII): monthly and annual
 * alphalists, SAWT, the 0619-F, 1603, 1702-Q and 1702 worksheets, the books of accounts and the IC
 * Broker's Annual Statement of Business Operations, with the period each one takes.
 */

export type OutputPeriod = 'MONTH' | 'QUARTER' | 'YEAR' | 'RANGE' | 'LEDGER';

export interface BirOutput {
  code: string;
  title: string;
  text: string;
  period: OutputPeriod;
}

export const BIR_OUTPUT_GROUPS: readonly { title: string; outputs: readonly BirOutput[] }[] = [
  {
    title: 'Returns and Worksheets',
    outputs: [
      {
        code: 'TAX-0619F',
        title: '0619-F Final Withholding Tax',
        text: 'Final income taxes withheld in the month.',
        period: 'MONTH',
      },
      {
        code: 'TAX-1603',
        title: '1603 Fringe Benefit Tax',
        text: 'Fringe benefits of the quarter, grossed up, and the tax due.',
        period: 'QUARTER',
      },
      {
        code: 'TAX-1702Q',
        title: '1702-Q Quarterly Income Tax',
        text: 'Year to date to the end of the quarter, less the creditable taxes received.',
        period: 'QUARTER',
      },
      {
        code: 'TAX-1702',
        title: '1702 Annual Income Tax',
        text: 'Income, expenses, tax due and creditable taxes of the year.',
        period: 'YEAR',
      },
    ],
  },
  {
    title: 'Alphalists',
    outputs: [
      {
        code: 'TAX-MAP',
        title: 'Monthly Alphalist of Payees (MAP)',
        text: 'Payees subject to expanded withholding in the month.',
        period: 'MONTH',
      },
      {
        code: 'TAX-1604E',
        title: '1604-E Annual Alphalist of Payees',
        text: 'Payees and taxes withheld of the year.',
        period: 'YEAR',
      },
      {
        code: 'TAX-SAWT',
        title: 'Summary Alphalist of Withholding Taxes (SAWT)',
        text: 'Taxes withheld from BDOI per agent, from the certificates received.',
        period: 'QUARTER',
      },
    ],
  },
  {
    title: 'Books of Accounts',
    outputs: [
      {
        code: 'TAX-BOOK-GJ',
        title: 'General Journal',
        text: 'Manual, adjustment, accrual and period-end entries.',
        period: 'RANGE',
      },
      {
        code: 'TAX-BOOK-PJ',
        title: 'Purchase Journal',
        text: 'Supplier invoices.',
        period: 'RANGE',
      },
      {
        code: 'TAX-BOOK-SJ',
        title: 'Sales Revenue Journal',
        text: 'Bookings, endorsements and commission.',
        period: 'RANGE',
      },
      {
        code: 'TAX-BOOK-CRB',
        title: 'Cash Receipts Book',
        text: 'Receipts and collections.',
        period: 'RANGE',
      },
      {
        code: 'TAX-BOOK-CDB',
        title: 'Cash Disbursements Book',
        text: 'Disbursement vouchers and payments.',
        period: 'RANGE',
      },
      {
        code: 'TAX-BOOK-SL',
        title: 'General Ledger by Account Class',
        text: 'Balance forward and entries per account.',
        period: 'LEDGER',
      },
    ],
  },
  {
    title: 'Insurance Commission',
    outputs: [
      {
        code: 'IC-BROKER-ASBO',
        title: "Broker's Annual Statement of Business Operations",
        text: 'Business placed per line of business.',
        period: 'YEAR',
      },
    ],
  },
];

export interface PeriodChoice {
  year: string;
  quarter: string;
  month: string;
  from: string;
  to: string;
  accountClass: string;
}

/** The period of today: year, quarter, month and the year to date. */
export function initialChoice(today: string): PeriodChoice {
  const month = Number(today.slice(5, 7));
  return {
    year: today.slice(0, 4),
    quarter: String(Math.ceil(month / 3)),
    month: String(month),
    from: `${today.slice(0, 4)}-01-01`,
    to: today,
    accountClass: 'ASSET',
  };
}

/** The report parameters of an output for the chosen period. */
export function outputParams(
  output: BirOutput,
  companyId: number,
  c: PeriodChoice,
): Record<string, string> {
  const params: Record<string, string> = { companyId: String(companyId), year: c.year };
  if (output.period === 'MONTH') {
    params.month = c.month;
  }
  if (output.period === 'QUARTER') {
    params.quarter = c.quarter;
  }
  if (output.period === 'RANGE' || output.period === 'LEDGER') {
    params.fromDate = c.from;
    params.toDate = c.to;
  }
  if (output.period === 'LEDGER') {
    params.accountClass = c.accountClass;
  }
  return params;
}

/** A label of the period an output is exported for. */
export function periodLabel(output: BirOutput, c: PeriodChoice): string {
  switch (output.period) {
    case 'MONTH':
      return `${c.year}-${c.month.padStart(2, '0')}`;
    case 'QUARTER':
      return `Q${c.quarter} ${c.year}`;
    case 'YEAR':
      return c.year;
    case 'LEDGER':
      return `${c.from} to ${c.to} · ${c.accountClass}`;
    default:
      return `${c.from} to ${c.to}`;
  }
}
