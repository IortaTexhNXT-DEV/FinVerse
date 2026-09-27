import type { Quotation, QuotationListItem } from '@/api/quotations';
import {
  alignGroups,
  canSaveQuotation,
  formOfQuotation,
  newQuotationForm,
  quotationStepErrors,
  toQuotationInput,
  withGroup,
  yearAfter,
} from './quotationForm';
import { criteriaOf, daysLeft, sendable, TAB_STATUSES, toggle } from './quotationList';
import { isEmail, quotationLinkOf, requestErrors } from './requestForm';
import type { RequestForm } from './requestForm';

describe('quotation form', () => {
  it('starts a year-long draft for the preselected client and product', () => {
    const f = newQuotationForm('2026-03-01', { clientId: 4, productCode: 'MC-PC', requestId: 9 });
    expect(f.periodTo).toBe('2027-03-01');
    expect(f.clientId).toBe(4);
    expect(f.requestId).toBe(9);
    expect(canSaveQuotation(f)).toBe(true);
    expect(canSaveQuotation(newQuotationForm('2026-03-01'))).toBe(false);
    expect(yearAfter('')).toBe('');
  });

  it('sends blanks as undefined and every item with its risk group', () => {
    const f = {
      ...newQuotationForm('2026-03-01', { clientId: 4, productCode: 'MC-PC' }),
      insurerBranch: 'Makati',
      items: [{ sumInsured: 100 }, { sumInsured: 200 }],
      groups: [2],
    };
    const body = toQuotationInput(f, 1);
    expect(body.insurerCode).toBeUndefined();
    expect(body.insurerBranch).toBeUndefined();
    expect(body.remarks).toBeUndefined();
    expect(body.items.map((i) => i.riskGroup)).toEqual([2, 1]);
    expect(toQuotationInput({ ...f, insurerCode: 'INS-1' }, 1).insurerBranch).toBe('Makati');
  });

  it('reads a saved quotation back into the wizard', () => {
    const q = {
      id: 3,
      quotationNo: 'QT-2026-000001',
      arn: 'ARN-2026-000001',
      clientId: 4,
      clientName: 'Acme',
      productCode: 'MC-PC',
      content: {
        insurerCode: 'INS-1',
        periodFrom: '2026-01-01',
        periodTo: '2027-01-01',
        validUntil: '2026-02-01',
        directPayment: true,
        items: [{ riskGroup: 2, data: { sumInsured: 5 } }],
      },
    } as unknown as Quotation;
    const f = formOfQuotation(q);
    expect(f.groups).toEqual([2]);
    expect(f.ratingBasis).toBe('ANNUAL');
    expect(f.marketSegment).toBe('');
    expect(f.directPayment).toBe(true);
  });

  it('blocks each step on its own missing fields', () => {
    const f = newQuotationForm('2026-03-01');
    expect(quotationStepErrors('client', f)).toHaveProperty('clientId');
    expect(quotationStepErrors('product', { ...f, periodTo: '2026-01-01' })).toEqual({
      productCode: 'Select the product',
      periodTo: 'The period end must be after the period start',
    });
    expect(quotationStepErrors('items', f)).toHaveProperty('items');
    expect(quotationStepErrors('items', { ...f, items: [{}] }).items).toContain('sum insured');
    expect(quotationStepErrors('items', { ...f, items: [{ sumInsured: 1 }] })).toEqual({});
    expect(quotationStepErrors('review', f)).toEqual({});
  });

  it('keeps risk groups aligned and at least 1', () => {
    expect(withGroup([1, 1], 1, 0)).toEqual([1, 1]);
    expect(withGroup([1, 1], 1, 2.4)).toEqual([1, 2]);
    expect(alignGroups([3], 3)).toEqual([3, 1, 1]);
    expect(alignGroups([3, 2], 1)).toEqual([3]);
  });
});

describe('quotation work list', () => {
  it('maps tabs and quick filters to criteria', () => {
    expect(criteriaOf('review', undefined, '  ')).toEqual({
      text: undefined,
      status: TAB_STATUSES.review,
      mine: undefined,
      expiring: undefined,
    });
    expect(criteriaOf('drafts', 'myDrafts', ' QT ').mine).toBe(true);
    expect(criteriaOf('drafts', 'myDrafts', ' QT ').text).toBe('QT');
    const expiring = criteriaOf('sent', 'expiring', '');
    expect(expiring.expiring).toBe(true);
    expect(expiring.status).toEqual(['APPROVED', 'SENT_TO_CLIENT']);
  });

  it('sends only the selected approved rows', () => {
    const rows = [
      { id: 1, status: 'APPROVED' },
      { id: 2, status: 'DRAFT' },
      { id: 3, status: 'APPROVED' },
    ] as QuotationListItem[];
    expect(sendable(rows, new Set([1, 2])).map((r) => r.id)).toEqual([1]);
  });

  it('counts days and toggles selections', () => {
    expect(daysLeft('2026-03-08', '2026-03-01')).toBe(7);
    expect(daysLeft('2026-02-27', '2026-03-01')).toBe(-2);
    expect([...toggle(new Set([1]), 2)]).toEqual([1, 2]);
    expect([...toggle(new Set([1, 2]), 1)]).toEqual([2]);
  });
});

describe('quotation requests', () => {
  const blank: RequestForm = {
    channel: 'EMAIL',
    externalRef: '',
    clientCode: '',
    prospectName: '',
    prospectEmail: '',
    prospectMobile: '',
    productCode: '',
    marketSegment: '',
    requestedCover: '',
  };

  it('checks e-mail addresses', () => {
    expect(isEmail('a@b.ph')).toBe(true);
    expect(isEmail('a@b')).toBe(false);
    expect(isEmail('a@@b.ph')).toBe(false);
    expect(isEmail('a b@c.ph')).toBe(false);
    expect(isEmail('@b.ph')).toBe(false);
  });

  it('needs a client or prospect and the cover', () => {
    expect(Object.keys(requestErrors(blank))).toEqual(['client', 'requestedCover']);
    expect(
      requestErrors({
        ...blank,
        prospectName: 'Juan',
        prospectEmail: 'x',
        requestedCover: 'Motor',
      }),
    ).toEqual({
      prospectEmail: 'Enter a valid e-mail address',
    });
    expect(requestErrors({ ...blank, clientCode: 'CL-1', requestedCover: 'Motor' })).toEqual({});
  });

  it('opens the wizard with the request preselections', () => {
    expect(quotationLinkOf({ id: 5, channel: 'EMAIL' })).toBe(
      '/quotations/new?request=5&channel=EMAIL',
    );
    expect(
      quotationLinkOf({
        id: 5,
        channel: 'WEB',
        clientId: 2,
        productCode: 'MC-PC',
        marketSegment: 'SME',
      }),
    ).toBe('/quotations/new?request=5&channel=WEB&client=2&product=MC-PC&segment=SME');
  });
});
