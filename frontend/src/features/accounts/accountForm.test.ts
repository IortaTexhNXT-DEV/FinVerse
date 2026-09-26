import type { Account } from '@/api/accounts';
import { ApiError } from '@/api/client';
import {
  canSave,
  criteriaOf,
  draftOfAccount,
  duplicateArns,
  EMPTY_PANEL,
  emptyItem,
  newDraft,
  oneYearAfter,
  splitRefs,
  stepProblems,
  toAccountInput,
  withClient,
} from './accountForm';
import { checkLines } from './accountChecks';

const account = {
  id: 7,
  arn: 'ARN-2026-900003',
  clientId: 3,
  clientCode: 'CL-2026-900003',
  clientName: 'SIT Client',
  productCode: 'PAR01',
  lineCode: 'PROPERTY',
  marketSegment: null,
  sourceChannel: 'EMAIL',
  insurerCode: 'INS-MGIC',
  insurerBranch: 'MKT',
  periodFrom: '2026-10-01',
  periodTo: '2027-10-01',
  multiYear: false,
  termYears: 1,
  currency: 'PHP',
  premium: { ratingBasis: null, commissionRate: 25, minimumApplied: false },
  paymentArrangement: 'DIRECT_TO_INSURER',
  pnNumbers: ['PN-1', 'PN-2'],
  contact: { name: 'Ana', email: null, mobile: null, address: null },
  freeFirstYear: { active: true, start: '2026-10-01' },
  items: [
    {
      id: 1,
      itemNo: 1,
      kind: 'PROPERTY_LOCATION',
      label: 'Makati',
      description: 'Office',
      sumInsured: 1_000_000,
      rate: 0.25,
      premium: 2500,
      location: { address: 'Makati', insuredItems: [{ description: 'Stock', sumInsured: 1 }] },
    },
  ],
} as unknown as Account;

describe('account wizard draft', () => {
  it('starts an annual draft through BDOI in pesos', () => {
    const d = newDraft('2026-03-15');
    expect(d.periodTo).toBe('2027-03-15');
    expect(d.paymentArrangement).toBe('VIA_BDOI');
    expect(canSave(d)).toBe(false);
    expect(canSave({ ...d, clientId: 1, productCode: 'MTR10' })).toBe(true);
    expect(oneYearAfter('')).toBe('');
  });

  it('continues a saved account and sends it back unchanged', () => {
    const d = draftOfAccount(account);
    expect(d).toMatchObject({
      id: 7,
      marketSegment: '',
      pnNumbers: 'PN-1, PN-2',
      ratingBasis: 'ANNUAL',
      commissionRate: 25,
      ffyStart: '2026-10-01',
      contactName: 'Ana',
      contactEmail: '',
    });
    const input = toAccountInput(d, 1);
    expect(input).toMatchObject({
      companyId: 1,
      clientId: 3,
      marketSegment: undefined,
      pnNumbers: ['PN-1', 'PN-2'],
      paymentArrangement: 'DIRECT_TO_INSURER',
      termYears: 1,
      ffyStart: '2026-10-01',
    });
    expect(input.items[0]?.location?.insuredItems).toHaveLength(1);
    expect(toAccountInput({ ...d, multiYear: true, termYears: 3 }, 1).termYears).toBe(3);
  });

  it('defaults segment and contact from the chosen client without overwriting', () => {
    const d = newDraft('2026-01-01');
    const client = {
      id: 9,
      displayName: 'Cruz, Ana',
      marketSegment: 'CBG',
      email: 'ana@example.com',
      mobile: '0917',
    } as Parameters<typeof withClient>[1];
    expect(withClient(d, client)).toMatchObject({
      clientId: 9,
      marketSegment: 'CBG',
      contactMobile: '0917',
    });
    expect(withClient({ ...d, marketSegment: 'RETAIL' }, client).marketSegment).toBe('RETAIL');
    expect(withClient(d, undefined)).toEqual({ clientId: undefined, clientName: '' });
  });

  it('blocks steps until their essentials are entered', () => {
    const d = newDraft('2026-01-01');
    expect(stepProblems('client', d)).toEqual(['Choose the client.']);
    expect(stepProblems('product', d)).toEqual(['Choose the product.']);
    expect(stepProblems('period', { ...d, periodTo: '2025-12-31' })).toHaveLength(1);
    expect(stepProblems('period', d)).toEqual([]);
    expect(stepProblems('items', d)).toEqual(['Add at least one risk item.']);
    const items = [emptyItem('VEHICLE'), emptyItem('PROPERTY_LOCATION'), emptyItem('PERSON')];
    expect(stepProblems('items', { ...d, items })).toEqual([
      'Vehicle 1: enter the plate number or conduction sticker.',
      'Location 2: enter the address.',
      'Person 3: enter the name.',
    ]);
    expect(
      stepProblems('items', { ...d, items: [{ vehicle: { conductionSticker: 'CS1' } }] }),
    ).toEqual([]);
    expect(emptyItem('GENERIC')).toEqual({ description: '' });
    expect(stepProblems('review', d)).toEqual([]);
  });

  it('names the existing accounts of a duplicate rejection', () => {
    const error = new ApiError(422, {
      code: 'DUPLICATE_ACCOUNT',
      detail:
        'Duplicate of existing account ARN-2026-900002, ARN-2026-900010: plate ABC ARN-2026-900002',
    });
    expect(duplicateArns(error)).toEqual(['ARN-2026-900002', 'ARN-2026-900010']);
    expect(duplicateArns(new ApiError(422, { code: 'OTHER', detail: 'ARN-2026-1' }))).toEqual([]);
    expect(duplicateArns(new Error('x'))).toEqual([]);
  });

  it('splits reference numbers', () => {
    expect(splitRefs(' PN-1; PN-2,PN-3  PN-4 ')).toEqual(['PN-1', 'PN-2', 'PN-3', 'PN-4']);
  });
});

describe('account search', () => {
  it('combines the panel with a quick filter', () => {
    expect(criteriaOf(EMPTY_PANEL, 'all')).toEqual({ status: undefined });
    expect(criteriaOf({ ...EMPTY_PANEL, text: 'Cruz', status: 'PLACED' }, 'all')).toEqual({
      text: 'Cruz',
      status: ['PLACED'],
    });
    expect(criteriaOf({ ...EMPTY_PANEL, status: 'PLACED' }, 'drafts')).toEqual({
      mine: true,
      status: ['DRAFT'],
    });
    expect(criteriaOf({ ...EMPTY_PANEL, includeVoided: true }, 'direct')).toMatchObject({
      includeVoided: true,
      directPayment: true,
    });
  });
});

describe('account check', () => {
  it('lists what blocks the submission', () => {
    expect(
      checkLines({
        fieldErrors: { 'items[0].plateNo': 'is required' },
        missingDocuments: ['IDF'],
        duplicates: [],
        premiumRated: false,
        tsuRequired: true,
        tsuCleared: false,
        readyToSubmit: false,
      }),
    ).toEqual([
      'Item 1 plate no.: is required',
      'Attach the Idf.',
      'The premium could not be rated yet (sum insured, period and rates).',
    ]);
  });
});
