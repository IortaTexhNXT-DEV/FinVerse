import type { TaxDocument, TaxForm, Worksheet } from '@/api/tax';
import {
  dueBadge,
  formsFor,
  isQuarterly,
  lineAmount,
  sourceLink,
  unreconciled,
  worksheetKpis,
} from './taxDisplay';
import { choiceLabel, defaultChoice, indexOptions, periodOf, quarterOf } from './taxPeriods';

const worksheet = (kind: Worksheet['kind']): Worksheet => ({
  kind,
  from: '2026-07-01',
  to: '2026-09-30',
  periodLabel: '2026-Q3',
  lines: [{ code: 'OUTPUT_VAT', description: 'Output VAT', amount: 1200 }],
  figures: { taxBase: 10000, taxDue: 1200, taxCredits: 200, amountPayable: 1000, excessCredit: 0 },
  documents: [],
  controls: [
    {
      accountCode: '2504',
      description: 'Output VAT',
      perDocuments: 1200,
      perLedger: 1150,
      difference: -50,
    },
    {
      accountCode: '1603',
      description: 'Input VAT',
      perDocuments: 200,
      perLedger: 210,
      difference: 10,
    },
  ],
  notes: [],
});

const form = (code: string, overrides: Partial<TaxForm>): TaxForm => ({
  id: 1,
  companyId: 1,
  code,
  name: code,
  authority: 'BIR',
  frequency: 'QUARTERLY',
  worksheet: 'VAT',
  dueMonthsAfter: 1,
  dueDay: 25,
  trackFiling: true,
  effectiveFrom: '2026-01-01',
  recordStatus: 'ACTIVE',
  maker: 'accountant',
  ...overrides,
});

describe('tax periods', () => {
  it('computes month and quarter bounds', () => {
    expect(periodOf({ year: 2026, granularity: 'QUARTER', index: 1 })).toEqual({
      from: '2026-01-01',
      to: '2026-03-31',
    });
    expect(periodOf({ year: 2028, granularity: 'MONTH', index: 2 })).toEqual({
      from: '2028-02-01',
      to: '2028-02-29',
    });
    expect(periodOf({ year: 2026, granularity: 'QUARTER', index: 4 }).to).toBe('2026-12-31');
  });

  it('defaults to the last completed period', () => {
    expect(defaultChoice('2026-09-23', 'QUARTER')).toEqual({
      year: 2026,
      granularity: 'QUARTER',
      index: 2,
    });
    expect(defaultChoice('2026-02-10', 'QUARTER')).toEqual({
      year: 2025,
      granularity: 'QUARTER',
      index: 4,
    });
    expect(defaultChoice('2026-09-23', 'MONTH')).toEqual({
      year: 2026,
      granularity: 'MONTH',
      index: 8,
    });
    expect(defaultChoice('2026-01-05', 'MONTH')).toEqual({
      year: 2025,
      granularity: 'MONTH',
      index: 12,
    });
  });

  it('labels selections and lists options', () => {
    expect(choiceLabel({ year: 2026, granularity: 'QUARTER', index: 3 })).toBe('2026-Q3');
    expect(choiceLabel({ year: 2026, granularity: 'MONTH', index: 3 })).toBe('2026-03');
    expect(indexOptions('QUARTER')).toHaveLength(4);
    expect(indexOptions('MONTH')[11]).toEqual({ value: 12, label: 'Dec' });
    expect(quarterOf('2026-12-31')).toBe(4);
    expect(quarterOf('2026-04-01')).toBe(2);
  });
});

describe('tax display', () => {
  it('maps due states to badges', () => {
    expect(dueBadge('OVERDUE', -5)).toEqual({ tone: 'danger', text: 'Overdue 5 d' });
    expect(dueBadge('DUE_SOON', 3).tone).toBe('warning');
    expect(dueBadge('PAID', 0).text).toBe('Paid');
    expect(dueBadge('REMINDER', 10).text).toBe('Outside FinVerse');
    expect(dueBadge('UPCOMING', 40)).toEqual({ tone: 'neutral', text: 'Due in 40 d' });
  });

  it('links documents to their source screens', () => {
    const doc = { sourceType: 'COMMISSION', sourceId: 42 } as TaxDocument;
    expect(sourceLink(doc)).toBe('/underwriting/policies/42');
    expect(sourceLink({ ...doc, sourceType: 'SUPPLIER_INVOICE' })).toBe('/payables/invoices');
  });

  it('summarises worksheets', () => {
    const vat = worksheet('VAT');
    expect(lineAmount(vat, 'OUTPUT_VAT')).toBe(1200);
    expect(lineAmount(vat, 'MISSING')).toBe(0);
    expect(lineAmount(undefined, 'OUTPUT_VAT')).toBe(0);
    expect(worksheetKpis(vat).map((k) => k.value)).toEqual([1200, 200, 1000, 0]);
    expect(worksheetKpis(worksheet('EWT'))[3]).toEqual({ label: 'Tax still due', value: 1000 });
    expect(worksheetKpis(worksheet('DST'))).toHaveLength(3);
    expect(unreconciled(vat)).toBe(60);
  });

  it('offers the tracked forms of a worksheet', () => {
    const forms = [
      form('2550Q', {}),
      form('0619-E', { worksheet: 'EWT', frequency: 'MONTHLY_EXCEPT_QUARTER_END' }),
      form('X', { recordStatus: 'PENDING_AUTHORIZATION' }),
      form('1601-C', { worksheet: 'NONE', trackFiling: false }),
    ];
    expect(formsFor(forms, 'VAT').map((f) => f.code)).toEqual(['2550Q']);
    expect(formsFor(forms, 'EWT').map((f) => f.code)).toEqual(['0619-E']);
    expect(isQuarterly(forms[0]!)).toBe(true);
    expect(isQuarterly(forms[1]!)).toBe(false);
  });
});
