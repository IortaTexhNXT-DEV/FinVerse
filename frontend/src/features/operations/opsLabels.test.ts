import type { InvoiceComponentRow, InvoiceFlags } from '@/api/operations';
import {
  SECTION_LABELS,
  SECTION_ROUTES,
  componentLabel,
  flagChips,
  invoiceTabs,
  premiumTotals,
  safeUrl,
  tileTone,
  visibleComponents,
} from './opsLabels';

const row = (
  component: InvoiceComponentRow['component'],
  booked: number,
  applied = 0,
  premiumReceivable = true,
): InvoiceComponentRow => ({
  component,
  premiumReceivable,
  booked,
  applied,
  reversed: 0,
  remitted: 0,
  adjusted: 0,
  writtenOff: 0,
  balance: booked - applied,
});

const noFlags: InvoiceFlags = {
  directPayment: false,
  cwt2Percent: false,
  incentiveEligible: false,
  hold: false,
  pendingNegativeAdjustment: false,
  writtenOff: false,
  cancelled: false,
  estimated: false,
};

describe('operations labels', () => {
  it('orders components by the application hierarchy and hides untouched ones', () => {
    const rows = [row('BASIC', 1000), row('DTIP', 1150, 0, false), row('DST', 150), row('LGT', 0)];
    expect(visibleComponents(rows).map((r) => r.component)).toEqual(['DST', 'BASIC', 'DTIP']);
    expect(componentLabel('PREMIUM_TAX_VAT')).toBe('PR - Premium Tax / VAT');
  });

  it('totals only the premium receivable', () => {
    expect(premiumTotals([row('BASIC', 1000, 400), row('DTIP', 1150, 0, false)])).toEqual({
      booked: 1000,
      applied: 400,
      balance: 600,
    });
  });

  it('lists flag chips including the lock', () => {
    expect(flagChips(noFlags)).toEqual([]);
    expect(
      flagChips({ ...noFlags, directPayment: true, hold: true, lockOwner: 'REMITTANCE' }),
    ).toEqual(['Direct Payment', 'On Hold', 'Locked by REMITTANCE']);
  });

  it('colours tiles only when they count something', () => {
    expect(tileTone(0, 'ALERT')).toBe('');
    expect(tileTone(2, 'ALERT')).toBe('ops-tile-alert');
    expect(tileTone(2, 'WARNING')).toBe('ops-tile-warning');
    expect(tileTone(2, 'INFO')).toBe('');
  });

  it('accepts only web addresses for external links', () => {
    expect(safeUrl('https://collection.bdo')).toBe('https://collection.bdo');
    expect(safeUrl('javascript:alert(1)')).toBeUndefined();
    expect(safeUrl(undefined)).toBeUndefined();
  });

  it('has a label and a route for every section', () => {
    expect(Object.keys(SECTION_LABELS)).toEqual(Object.keys(SECTION_ROUTES));
  });

  it('gives Invoice 360 one tab per Operations module, counting its records', () => {
    const item = { type: 'RECEIPT', reference: 'AR-1', status: 'ACTIVE' };
    const tabs = invoiceTabs({ RECEIPTS: [item, { ...item, reference: 'AR-2' }], COMMISSION: [] });
    expect(tabs.map((t) => t.id)).toEqual([
      'components',
      'movements',
      'receipts',
      'remittances',
      'adjustments',
      'reconciliation',
      'commission',
      'history',
      'documents',
    ]);
    expect(tabs.find((t) => t.id === 'receipts')?.label).toBe('Receipts (2)');
    expect(tabs.find((t) => t.id === 'commission')?.label).toBe('Commission');
    expect(invoiceTabs({ DOCUMENTS: [item] }).at(-1)?.label).toBe('Documents (1)');
  });
});
