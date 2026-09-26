import type { Product, SalesOrganisation } from '@/api/catalog';
import { calcProblems, newCalcForm, toRatingInput } from './calculatorForm';
import { splitEmails } from './insurerForm';
import {
  newProductForm,
  productFormOf,
  productProblems,
  toggleSegment,
  toProductInput,
} from './productForm';
import { salesTree, unitsOf } from './salesTree';

describe('product form', () => {
  it('validates and builds the request', () => {
    const form = newProductForm('MOTOR');
    expect(productProblems(form)).toEqual([
      'Code: 1 to 20 capital letters or digits.',
      'Name is required.',
      'Default commission rate is required.',
    ]);
    const ok = {
      ...form,
      code: 'MTR99',
      name: 'Motor',
      defaultCommissionRate: 17.5,
      coverTypeCode: '',
    };
    expect(productProblems(ok)).toEqual([]);
    expect(productProblems({ ...ok, multiYearAllowed: true, maxTermYears: 1 })).toHaveLength(1);
    const input = toProductInput({ ...ok, maxTermYears: 5 });
    expect(input).toMatchObject({ code: 'MTR99', coverTypeCode: undefined, maxTermYears: 1 });
    expect(input).not.toHaveProperty('existing');
    expect(toProductInput({ ...ok, multiYearAllowed: true, maxTermYears: 3 }).maxTermYears).toBe(3);
    expect(productProblems({ ...ok, lineCode: '' })).toEqual(['Choose the product line.']);
  });

  it('edits an existing product', () => {
    const product = {
      ...newProductForm('PROPERTY'),
      id: 1,
      code: 'PAR01',
      name: 'Fire',
      coverTypeCode: null,
      defaultRate: null,
      maxSumInsured: 20_000_000,
      tsuInvolvement: null,
      defaultCommissionRate: 25,
      minimumPremium: 500,
      maxTermYears: 1,
      marketSegments: ['CBG'],
      recordStatus: 'ACTIVE',
    } as unknown as Product;
    const form = productFormOf(product);
    expect(form).toMatchObject({
      existing: true,
      coverTypeCode: undefined,
      tsuInvolvement: 'BY_RULES',
    });
    expect(toggleSegment(form.marketSegments, 'CBG')).toEqual([]);
    expect(toggleSegment(form.marketSegments, 'RETAIL')).toEqual(['CBG', 'RETAIL']);
  });
});

describe('premium calculator form', () => {
  it('requires a product, a sum insured and the period of non-annual rating', () => {
    const form = newCalcForm();
    expect(calcProblems(form)).toHaveLength(2);
    const filled = {
      ...form,
      productCode: 'PAR01',
      basis: 'PRO_RATA' as const,
      items: [{ label: '', sumInsured: '-1,000', ratePercent: '', biLimit: '', pdLimit: '' }],
    };
    expect(calcProblems(filled)).toEqual([
      'Pro-rata and short-period rating need the period (for endorsements: the remaining term).',
      'Only an endorsement may reduce the sum insured.',
    ]);
    const endorsement = {
      ...filled,
      endorsement: true,
      periodFrom: '2026-07-01',
      periodTo: '2027-01-01',
    };
    expect(calcProblems(endorsement)).toEqual([]);
    expect(toRatingInput(endorsement, 1, 'PROPERTY')).toMatchObject({
      companyId: 1,
      insurerCode: undefined,
      endorsement: true,
      items: [{ label: 'Item 1', sumInsured: -1000, ratePercent: undefined, biLimit: undefined }],
    });
  });

  it('sends BI / PD limits for motor and rates otherwise', () => {
    const form = {
      ...newCalcForm(),
      productCode: 'MTR10',
      commissionRate: '15',
      items: [
        { label: 'Car', sumInsured: '900000', ratePercent: '2', biLimit: '100000', pdLimit: 'x' },
        { label: 'Blank', sumInsured: '', ratePercent: '', biLimit: '', pdLimit: '' },
      ],
    };
    const motor = toRatingInput(form, 1, 'MOTOR');
    expect(motor.commissionRate).toBe(15);
    expect(motor.items).toEqual([
      {
        label: 'Car',
        sumInsured: 900000,
        ratePercent: undefined,
        biLimit: 100000,
        pdLimit: undefined,
      },
    ]);
    expect(toRatingInput(form, 1, 'PROPERTY').items[0]?.ratePercent).toBe(2);
  });
});

describe('insurer and sales organisation helpers', () => {
  it('splits placement e-mails', () => {
    expect(splitEmails('a@x.ph, b@x.ph;c@x.ph ')).toEqual(['a@x.ph', 'b@x.ph', 'c@x.ph']);
    expect(splitEmails('')).toEqual([]);
  });

  it('builds the region, department and team tree with inherited cost centers', () => {
    const unit = (
      code: string,
      level: string,
      parentCode: string | null,
      costCenter: string | null,
    ) => ({
      id: code.length,
      code,
      name: code,
      level,
      parentCode,
      costCenter,
      recordStatus: 'ACTIVE',
    });
    const org = {
      units: [
        unit('NCR', 'REGION', null, 'NB-CBG-M'),
        unit('CBG-NCR', 'DEPARTMENT', 'NCR', null),
        unit('T-CBG1', 'TEAM', 'CBG-NCR', null),
        unit('ORPHAN', 'TEAM', 'MISSING', 'X'),
      ],
      officers: [{ id: 1, teamCode: 'T-CBG1', username: 'ao', recordStatus: 'ACTIVE' }],
    } as unknown as SalesOrganisation;
    const tree = salesTree(org);
    expect(tree.map((n) => n.unit.code)).toEqual(['NCR', 'ORPHAN']);
    const team = tree[0]?.children[0]?.children[0];
    expect(team?.costCenter).toBe('NB-CBG-M');
    expect(team?.officers.map((o) => o.username)).toEqual(['ao']);
    expect(unitsOf(org, 'TEAM')).toHaveLength(2);
    expect(unitsOf(undefined, 'TEAM')).toEqual([]);
  });
});
