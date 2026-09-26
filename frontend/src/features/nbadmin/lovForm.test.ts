import type { LovValue } from '@/api/lov';
import { effectivity, lovForm, nextSortOrder, toLovRequest, validateLov } from './lovForm';

const VALUE: LovValue = {
  id: 1,
  typeCode: 'CLIENT_TAG',
  code: 'VIP',
  label: 'VIP client',
  sortOrder: 10,
  effectiveFrom: '2020-01-01',
  status: 'ACTIVE',
};

describe('list value form', () => {
  it('starts a new value today after the last sort order', () => {
    const form = lovForm(
      undefined,
      '2026-09-24',
      nextSortOrder([VALUE, { ...VALUE, sortOrder: 35 }]),
    );
    expect(form).toEqual({
      code: '',
      label: '',
      sortOrder: '40',
      parentCode: '',
      effectiveFrom: '2026-09-24',
      effectiveTo: '',
    });
    expect(nextSortOrder([])).toBe(10);
    expect(lovForm(VALUE, '2026-09-24', 99).sortOrder).toBe('10');
  });

  it('validates code, label, order and dates', () => {
    expect(
      validateLov({
        code: 'bad code',
        label: ' ',
        sortOrder: 'x',
        parentCode: '',
        effectiveFrom: '',
        effectiveTo: '',
      }),
    ).toEqual({
      code: 'Use capital letters, digits and _ (up to 40)',
      label: 'Enter the label',
      sortOrder: 'Enter a whole number',
      effectiveFrom: 'Enter the first valid date',
    });
    expect(
      validateLov({ ...lovForm(VALUE, '2026-01-01', 0), effectiveTo: '2019-12-31' }).effectiveTo,
    ).toBe('Must not be before the first valid date');
  });

  it('maps the form and tells the effectivity', () => {
    expect(toLovRequest({ ...lovForm(VALUE, '', 0), label: ' VIP ', parentCode: ' ' })).toEqual({
      code: 'VIP',
      label: 'VIP',
      sortOrder: 10,
      parentCode: undefined,
      effectiveFrom: '2020-01-01',
      effectiveTo: undefined,
    });
    expect(effectivity(VALUE, '2026-01-01')).toBe('ACTIVE');
    expect(effectivity({ ...VALUE, effectiveFrom: '2027-01-01' }, '2026-01-01')).toBe('FUTURE');
    expect(effectivity({ ...VALUE, effectiveTo: '2025-12-31' }, '2026-01-01')).toBe('EXPIRED');
  });
});
