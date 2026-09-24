import type { WorkbenchRow } from '@/api/booking';
import {
  actionsOf,
  cancellationErrors,
  cancellationRequest,
  endorsementErrors,
  endorsementRequest,
  handedArns,
  isValid,
  journalTotals,
  labelOf,
  rowLink,
  selectedArns,
  toggle,
  toggleAll,
  withHanded,
} from './bookingForm';
import type { CancellationForm, EndorsementForm } from './bookingForm';

const FORM: EndorsementForm = {
  type: 'POSITIVE',
  effectiveDate: '2027-04-01',
  basis: 'PRO_RATA',
  sumInsuredChange: '200000',
  ratePercent: '',
  description: 'Accessories',
  bookingDate: '',
};

const ROWS: WorkbenchRow[] = [
  { id: 1, arn: 'ARN-1', status: 'POLICY_ISSUED' },
  { id: 2, arn: 'ARN-2', status: 'QUEUED' },
  { id: 3, arn: 'ARN-3', status: 'BOOKED', invoiceId: 30 },
];

describe('workbench selection', () => {
  it('toggles rows and the whole page', () => {
    const one = toggle(new Set<number>(), 1);
    expect([...one]).toEqual([1]);
    expect([...toggle(one, 1)]).toEqual([]);
    expect([...toggleAll(one, [1, 2])]).toEqual([1, 2]);
    expect([...toggleAll(new Set([1, 2]), [1, 2])]).toEqual([]);
    expect(selectedArns(ROWS, new Set([1, 3]))).toEqual(['ARN-1', 'ARN-3']);
  });

  it('reads the ARNs handed over by placement and pre-selects their rows', () => {
    expect(handedArns(null)).toEqual([]);
    expect(handedArns(' ARN-1, ,ARN-3,ARN-1')).toEqual(['ARN-1', 'ARN-3']);
    expect([...withHanded(ROWS, new Set([2]), ['ARN-3', 'ARN-9'])]).toEqual([2, 3]);
  });

  it('offers the bulk actions of each tab', () => {
    expect(actionsOf('READY')).toEqual({ bookNow: true, addToBatch: true, confirmBatch: false });
    expect(actionsOf('QUEUED')).toEqual({ bookNow: true, addToBatch: false, confirmBatch: true });
    expect(actionsOf('BOOKED').bookNow).toBe(false);
    expect(actionsOf('FAILED').addToBatch).toBe(true);
  });

  it('opens the invoice once booked, else the confirmation', () => {
    expect(rowLink(ROWS[0]!)).toBe('/booking/book/ARN-1');
    expect(rowLink(ROWS[2]!)).toBe('/booking/invoices/30');
  });
});

describe('journal totals', () => {
  it('adds debits and credits in cents', () => {
    const totals = journalTotals([
      { side: 'DEBIT', amount: 0.1 },
      { side: 'DEBIT', amount: 0.2 },
      { side: 'CREDIT', amount: 0.3 },
    ]);
    expect(totals).toEqual({ debit: 0.3, credit: 0.3, balanced: true });
    expect(journalTotals([]).balanced).toBe(false);
  });
});

describe('endorsement form', () => {
  it('accepts a complete positive endorsement', () => {
    expect(isValid(endorsementErrors(FORM))).toBe(true);
    expect(endorsementRequest('ARN-1', FORM)).toEqual({
      arn: 'ARN-1',
      type: 'POSITIVE',
      effectiveDate: '2027-04-01',
      basis: 'PRO_RATA',
      sumInsuredChange: 200000,
      ratePercent: undefined,
      description: 'Accessories',
      bookingDate: undefined,
    });
  });

  it('checks the sign, the change, the rate and the text', () => {
    expect(endorsementErrors({ ...FORM, sumInsuredChange: '-5' }).sumInsuredChange).toBeDefined();
    expect(
      endorsementErrors({ ...FORM, type: 'NEGATIVE', sumInsuredChange: '5' }).sumInsuredChange,
    ).toBeDefined();
    expect(endorsementErrors({ ...FORM, sumInsuredChange: '' }).sumInsuredChange).toBeDefined();
    expect(endorsementErrors({ ...FORM, ratePercent: '-1' }).ratePercent).toBeDefined();
    const empty = endorsementErrors({ ...FORM, effectiveDate: '', description: ' ' });
    expect(empty.effectiveDate).toBeDefined();
    expect(empty.description).toBeDefined();
  });

  it('needs no amounts for a non-financial endorsement', () => {
    const form: EndorsementForm = { ...FORM, type: 'NON_FINANCIAL', sumInsuredChange: '' };
    expect(isValid(endorsementErrors(form))).toBe(true);
    expect(endorsementRequest('ARN-1', form).sumInsuredChange).toBeUndefined();
  });
});

describe('cancellation form', () => {
  const CANCEL: CancellationForm = {
    kind: 'PARTIAL',
    basis: 'SHORT_PERIOD',
    effectiveDate: '2027-04-01',
    reasonCode: 'CLIENT_REQUEST',
    description: 'Sold',
    bookingDate: '2026-09-20',
  };

  it('requires the date, reason and description', () => {
    expect(isValid(cancellationErrors(CANCEL))).toBe(true);
    const errors = cancellationErrors({
      ...CANCEL,
      effectiveDate: '',
      reasonCode: '',
      description: '',
    });
    expect(Object.keys(errors)).toEqual(['effectiveDate', 'reasonCode', 'description']);
  });

  it('sends the basis only for a partial cancellation', () => {
    expect(cancellationRequest('ARN-1', CANCEL).basis).toBe('SHORT_PERIOD');
    expect(cancellationRequest('ARN-1', { ...CANCEL, kind: 'FLAT' }).basis).toBeUndefined();
    expect(labelOf('FLAT_RETAIN_DST')).toBe('Flat, retaining DST');
    expect(labelOf('OTHER')).toBe('OTHER');
    expect(labelOf(undefined)).toBe('');
  });
});
