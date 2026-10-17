import type {
  IncentiveCriteria,
  IncentiveRuleParameter,
  VersionDetail,
} from '@/api/productCatalog';
import { clauseErrors, coverageErrors } from './coverageForm';
import {
  incentiveErrors,
  incentiveFormOf,
  isCurrent,
  newIncentiveForm,
  paramErrors,
  paramRowsOf,
  storedParams,
  toIncentiveInput,
} from './incentiveForm';
import { ageInDays, formOf, syncTerms, toInput, validateVersionForm } from './versionForm';

const detail: VersionDetail = {
  summary: {
    productCode: 'MTR12',
    productName: 'Motor package',
    versionNo: 2,
    status: 'DRAFT',
    effectiveFrom: '2030-01-01',
    packageEndDate: '2031-01-01',
    minimumPremium: 5500,
    maker: 'mbs',
  },
  lineCode: 'MOTOR',
  scheme: {
    defaultRate: 1.25,
    minimumPremium: 5500,
    defaultCommissionRate: 15,
    manualRateAllowed: false,
  },
  coverages: [
    {
      coverageCode: 'OD_THEFT',
      included: true,
      optional: false,
      deductibleAmount: 2000,
      sortOrder: 10,
    },
    { coverageCode: 'AUTO_PA', included: false, optional: true, sortOrder: 20 },
  ],
  insurers: [{ insurerCode: 'INS-MGIC', role: 'PANEL', rate: 1.25 }],
  insurerTerms: [
    {
      insurerCode: 'INS-MGIC',
      coverageCode: 'OD_THEFT',
      included: true,
      clauseCodes: 'MTR_PARTICIPATION,GEN_SANCTIONS',
    },
  ],
};

describe('package version form', () => {
  it('reads a version and writes the API body with one term per insurer and coverage', () => {
    const form = formOf(detail);
    expect(form.defaultRate).toBe('1.25');
    expect(form.terms[0]?.clauseCodes).toEqual(['MTR_PARTICIPATION', 'GEN_SANCTIONS']);
    const withLac = {
      ...form,
      insurers: [
        ...form.insurers,
        {
          insurerCode: 'INS-LAC',
          role: 'PANEL' as const,
          sharePercent: '',
          rate: '1.3',
          minimumPremium: '',
          defaultBranchCode: '',
        },
      ],
    };
    const terms = syncTerms(withLac);
    expect(terms).toHaveLength(2);
    expect(terms[1]).toMatchObject({
      insurerCode: 'INS-LAC',
      coverageCode: 'OD_THEFT',
      deductibleAmount: '2000',
    });
    const input = toInput(withLac, 7);
    expect(input.companyId).toBe(7);
    expect(input.rateScheme).toMatchObject({ defaultRate: 1.25, minimumPremium: 5500 });
    expect(input.coverages[0]).toMatchObject({ deductible: { amount: 2000 }, sortOrder: 10 });
    expect(input.coverages[1]?.deductible).toBeUndefined();
    expect(input.insurers[1]?.rate).toBe(1.3);
    expect(input.insurerTerms).toHaveLength(2);
  });

  it('validates the rate scheme, dates and coverages', () => {
    const form = formOf(detail);
    expect(validateVersionForm(form, '2029-12-31')).toEqual({});
    const broken = {
      ...form,
      defaultRate: '',
      minimumPremium: '',
      defaultCommissionRate: '120',
      effectiveFrom: '2020-01-01',
      packageEndDate: '2019-01-01',
      coverages: form.coverages.map((c) => ({ ...c, included: false })),
      insurers: form.insurers.map((i) => ({ ...i, rate: '', sharePercent: '150' })),
    };
    const errors = validateVersionForm(broken, '2029-12-31');
    expect(Object.keys(errors)).toEqual(
      expect.arrayContaining([
        'defaultRate',
        'minimumPremium',
        'defaultCommissionRate',
        'effectiveFrom',
        'packageEndDate',
        'coverages',
        'insurers.0.sharePercent',
      ]),
    );
    expect(validateVersionForm({ ...form, effectiveFrom: '' }, '2029-12-31').effectiveFrom).toBe(
      'Enter the effective date',
    );
  });

  it('computes the age of a submission', () => {
    expect(ageInDays(undefined)).toBe(0);
    expect(ageInDays('2030-01-01T00:00:00Z', Date.parse('2030-01-04T12:00:00Z'))).toBe(3);
  });
});

describe('incentive criteria form', () => {
  const row: IncentiveCriteria = {
    id: 1,
    recordStatus: 'ACTIVE',
    companyId: 1,
    code: 'CPC2',
    name: 'CPC2',
    incentiveType: 'CAMPAIGN',
    valueBasis: 'RATE',
    value: 1,
    products: [{ productCode: 'MTR10', marketSegment: 'CBG' }],
    effectiveFrom: '2026-01-01',
  };

  it('requires products, a value and logical dates', () => {
    const errors = incentiveErrors({
      ...newIncentiveForm(),
      code: 'bad code',
      params: [{ key: '', value: '' }],
    });
    expect(Object.keys(errors)).toEqual(
      expect.arrayContaining([
        'code',
        'name',
        'incentiveType',
        'value',
        'params.0',
        'products',
        'effectiveFrom',
      ]),
    );
    const rate = { ...incentiveFormOf(row), value: '150' };
    expect(incentiveErrors(rate).value).toBe('A rate is between 0 and 100');
    const amendment = {
      ...incentiveFormOf(row),
      effectiveFrom: '2026-01-01',
      effectiveTo: '2025-01-01',
    };
    const amended = incentiveErrors(amendment, true, '2026-01-01');
    expect(amended.effectiveFrom).toContain('after 2026-01-01');
    expect(amended.effectiveTo).toBe('The end is before the start');
    expect(incentiveErrors({ ...incentiveFormOf(row), valueBasis: 'RULE', value: '' })).toEqual({});
  });

  it('writes the API body and tells current rows apart', () => {
    const input = toIncentiveInput({ ...incentiveFormOf(row), params: [] }, 3);
    expect(input).toMatchObject({
      companyId: 3,
      value: 1,
      ruleParams: undefined,
      effectiveTo: undefined,
    });
    expect(
      toIncentiveInput({ ...incentiveFormOf(row), valueBasis: 'RULE' }, 3).value,
    ).toBeUndefined();
    expect(isCurrent(row, '2026-06-01')).toBe(true);
    expect(isCurrent({ ...row, effectiveTo: '2026-05-31' }, '2026-06-01')).toBe(false);
    expect(isCurrent({ ...row, recordStatus: 'INACTIVE' }, '2026-06-01')).toBe(false);
  });
});

describe('incentive rule parameters', () => {
  const definitions: IncentiveRuleParameter[] = [
    { key: 'minimumPremium', label: 'Minimum Gross Premium', valueType: 'AMOUNT' },
    { key: 'share', label: 'Share', valueType: 'PERCENT' },
    { key: 'note', label: 'Note', valueType: 'TEXT' },
  ];

  it('reads the stored parameters as rows and writes them back', () => {
    expect(paramRowsOf('{"minimumPremium": 5000, "note": "x"}')).toEqual([
      { key: 'minimumPremium', value: '5000' },
      { key: 'note', value: 'x' },
    ]);
    expect(paramRowsOf('{x')).toEqual([]);
    expect(paramRowsOf('[1]')).toEqual([]);
    expect(paramRowsOf(undefined)).toEqual([]);
    expect(paramRowsOf('{"flag": true}')).toEqual([{ key: 'flag', value: '' }]);
    expect(
      storedParams(
        [
          { key: 'minimumPremium', value: '5,000' },
          { key: 'note', value: ' Seed ' },
          { key: '', value: '' },
        ],
        definitions,
      ),
    ).toBe('{"minimumPremium":5000,"note":"Seed"}');
    expect(storedParams([], definitions)).toBeUndefined();
  });

  it('checks each row in business words', () => {
    expect(
      paramErrors(
        [
          { key: 'minimumPremium', value: 'abc' },
          { key: 'share', value: '120' },
          { key: 'note', value: '' },
          { key: 'minimumPremium', value: '1' },
          { key: '', value: '1' },
          { key: 'share', value: '-1' },
          { key: 'note', value: 'ok' },
        ],
        definitions,
      ),
    ).toEqual({
      'params.0': 'Minimum Gross Premium must be a number',
      'params.1': 'Share must be between 0 and 100',
      'params.2': 'Enter a value for each parameter',
      'params.3': 'Minimum Gross Premium is listed twice',
      'params.4': 'Select the parameter',
      'params.5': 'Share is listed twice',
      'params.6': 'Note is listed twice',
    });
    expect(paramErrors([{ key: 'share', value: '-1' }], definitions)).toEqual({
      'params.0': 'Share cannot be negative',
    });
  });
});

describe('coverage and clause forms', () => {
  it('checks the mandatory fields', () => {
    expect(
      Object.keys(
        coverageErrors({
          lineCode: '',
          code: 'a b',
          name: ' ',
          kind: '',
          basic: false,
          sortOrder: 1,
        }),
      ),
    ).toEqual(['lineCode', 'code', 'name', 'kind']);
    expect(
      coverageErrors({
        lineCode: 'MOTOR',
        code: 'OD',
        name: 'Own damage',
        kind: 'COVERAGE',
        basic: true,
        sortOrder: 1,
      }),
    ).toEqual({});
    expect(
      Object.keys(
        clauseErrors({
          code: 'X',
          kind: '',
          title: '',
          wording: '',
          effectiveFrom: '2026-01-01',
          effectiveTo: '2025-01-01',
        }),
      ),
    ).toEqual(['kind', 'title', 'wording', 'effectiveTo']);
    expect(
      clauseErrors({ code: 'X', kind: 'CLAUSE', title: 't', wording: 'w', effectiveFrom: '' })
        .effectiveFrom,
    ).toBe('Enter the effective date');
  });
});
