import { describe, expect, it } from 'vitest';
import type { PackageRequest } from '@/api/productmaint';
import {
  daysInStage,
  expiryRows,
  expiryTone,
  formOfRequest,
  homeTiles,
  isRequestTab,
  negotiationOptional,
  newCoverage,
  newRequestForm,
  offered,
  requestCriteria,
  requestErrors,
  slaState,
  submissionGaps,
  tabOfStage,
  toRequestInput,
  toggleTargetInsurer,
  typeLabel,
} from './packageRequest';

describe('package request work list', () => {
  it('maps tabs to stages and back', () => {
    expect(requestCriteria('negotiation', ' PKR ', true, 'NEW')).toEqual({
      text: 'PKR',
      stage: ['NEGOTIATION', 'TERMS_REVIEW', 'FOR_MKT_REVIEW', 'REQUIREMENTS_PREP'],
      type: ['NEW'],
      mine: true,
    });
    expect(requestCriteria('drafts', '', false, '').type).toBeUndefined();
    expect(tabOfStage('FOR_MANCOM')).toBe('mancom');
    expect(tabOfStage('VOIDED')).toBe('closed');
    expect(isRequestTab('mbs')).toBe(true);
    expect(isRequestTab('x')).toBe(false);
    expect(isRequestTab(null)).toBe(false);
  });

  it('builds the home tiles with SLA counts', () => {
    const tiles = homeTiles({
      stages: [
        { stage: 'FOR_TSU_REVIEW', total: 2, overdue: 1, dueSoon: 0 },
        { stage: 'FOR_TSU_APPROVAL', total: 3, overdue: 0, dueSoon: 2 },
      ],
      expiring: {},
      advisoriesPending: 0,
      outputsThisWeek: 0,
    });
    const tsu = tiles.find((t) => t.tab === 'tsu');
    expect(tsu).toMatchObject({ total: 5, overdue: 1, dueSoon: 2 });
    expect(tiles.some((t) => t.tab === 'closed')).toBe(false);
    expect(homeTiles(undefined).every((t) => t.total === 0)).toBe(true);
  });

  it('classifies SLA and stage age', () => {
    const now = new Date('2026-09-25T10:00:00Z');
    expect(slaState(undefined, now)).toBe('ok');
    expect(slaState('2026-09-25T09:00:00Z', now)).toBe('overdue');
    expect(slaState('2026-09-25T12:00:00Z', now)).toBe('soon');
    expect(slaState('2026-09-26T12:00:00Z', now)).toBe('ok');
    expect(daysInStage('2026-09-22T10:00:00Z', now)).toBe(3);
    expect(daysInStage(undefined, now)).toBeUndefined();
  });

  it('splits the expiry list and colours the urgency', () => {
    const rows = [{ renewalRequestId: 1 }, {}];
    expect(expiryRows(rows, 'renewal')).toHaveLength(1);
    expect(expiryRows(rows, 'expiring')).toEqual([{}]);
    expect(expiryRows(rows, 'expired')).toEqual([]);
    expect(expiryTone(5)).toBe('danger');
    expect(expiryTone(20)).toBe('warning');
    expect(expiryTone(80)).toBe('info');
    expect(offered('APPROVED_WITH_CHANGES')).toBe(true);
    expect(offered('DECLINED')).toBe(false);
  });
});

describe('package request form', () => {
  it('validates the mandatory fields', () => {
    const form = newRequestForm();
    expect(Object.keys(requestErrors(form)).sort((a, b) => a.localeCompare(b))).toEqual([
      'coverTypeCode',
      'lineCode',
      'reason',
      'title',
    ]);
    const amend = {
      ...form,
      type: 'AMEND' as const,
      scope: 'CLIENT_SPECIFIC' as const,
      title: 'x',
      lineCode: 'MOTOR',
      reason: 'OTHERS',
      terms: {
        ...form.terms,
        coverages: [newCoverage()],
        dates: { packageStartDate: '2027-01-01', packageEndDate: '2026-01-01' },
      },
    };
    expect(Object.keys(requestErrors(amend)).sort((a, b) => a.localeCompare(b))).toEqual([
      'clientId',
      'coverages',
      'packageEndDate',
      'productCode',
    ]);
  });

  it('lists the gaps of the submission and builds the API body', () => {
    const form = { ...newRequestForm(), title: ' Fleet ', lineCode: 'MOTOR', reason: 'OTHERS' };
    expect(submissionGaps(form)).toHaveLength(3);
    const withInsurer = { ...form, terms: toggleTargetInsurer(form.terms, 'INS-MGIC') };
    expect(withInsurer.terms.insurers).toHaveLength(1);
    expect(toggleTargetInsurer(withInsurer.terms, 'INS-MGIC').insurers).toHaveLength(0);
    const body = toRequestInput(withInsurer, 1);
    expect(body.title).toBe('Fleet');
    expect(body.terms.sections).toEqual([]);
    expect(body.productCode).toBeUndefined();
    expect(toRequestInput({ ...form, type: 'RETIRE' }, 1).negotiationRequired).toBe(false);
    expect(submissionGaps({ ...form, type: 'RETIRE' })).toEqual([]);
    expect(negotiationOptional('RENEW')).toBe(true);
    expect(negotiationOptional('NEW')).toBe(false);
    expect(typeLabel('RETIRE')).toBe('Retire package');
  });

  it('reads a saved request back into the form', () => {
    const saved = {
      id: 3,
      requestNo: 'PKR-2026-000003',
      requestType: 'RENEW',
      scope: 'GENERIC',
      title: 'Renewal',
      lineCode: 'MOTOR',
      productCode: 'MTR10',
      marketSegments: ['CBG'],
      reason: 'PACKAGE_EXPIRY',
      negotiationRequired: false,
      requestedTerms: newRequestForm().terms,
    } as unknown as PackageRequest;
    const form = formOfRequest(saved);
    expect(form).toMatchObject({ id: 3, productCode: 'MTR10', coverTypeCode: '', reasonNote: '' });
  });
});
