import { describe, expect, it } from 'vitest';
import type { EndorsementRequest } from './api';
import {
  EMPTY_FORM,
  classOfType,
  componentLabel,
  formErrors,
  formOf,
  isValid,
  modeOf,
  stageLabel,
  stageTone,
  tabOf,
  toInput,
} from './requestForm';
import type { RequestForm } from './requestForm';

const cancellation: RequestForm = {
  ...EMPTY_FORM,
  endorsementType: 'FIN_CHANGE_COVER',
  requestType: 'FLAT_CANCELLATION',
  reasonCode: 'UNIT_SOLD',
  effectiveDate: '2026-10-01',
  description: 'Unit sold',
};

describe('adjustment request form', () => {
  it('labels and colours the stages', () => {
    expect(stageLabel('AWAITING_REAPPLICATION')).toBe('Payments to Re-apply');
    expect(stageTone('POSTED')).toBe('success');
    expect(stageTone('FOR_APPROVAL')).toBe('warning');
    expect(tabOf('FOR_POSTING')).toBe('FOR_POSTING');
    expect(tabOf('nonsense')).toBe('ALL');
    expect(tabOf(null)).toBe('ALL');
    expect(componentLabel('DTIP')).toBe('Due to Insurer (Gross)');
    expect(componentLabel('X')).toBe('X');
  });

  it('derives the class and the mode from the codes', () => {
    expect(classOfType('FIN_TSI')).toBe('FINANCIAL');
    expect(classOfType('NF_DESCRIPTIVE')).toBe('NON_FINANCIAL');
    expect(classOfType('INT_ADJUSTMENT')).toBe('INTERNAL');
    expect(classOfType('')).toBeUndefined();
    expect(modeOf('FINANCIAL', 'PARTIAL_CANCELLATION')).toBe('CANCELLATION');
    expect(modeOf('FINANCIAL', 'FLAT_CANCELLATION_RETAIN_DST')).toBe('CANCELLATION');
    expect(modeOf('FINANCIAL', 'TSI_CHANGE')).toBe('TSI');
    expect(modeOf('FINANCIAL', 'WRITE_OFF')).toBe('WRITE_OFF');
    expect(modeOf('FINANCIAL', 'CANCELLATION_REVERSAL')).toBe('AMOUNTS');
    expect(modeOf('NON_FINANCIAL', 'TSI_CHANGE')).toBe('NONE');
    expect(modeOf('INTERNAL', '')).toBe('NONE');
  });

  it('validates the fields the type needs', () => {
    expect(isValid(formErrors(cancellation))).toBe(true);
    const empty = formErrors(EMPTY_FORM);
    expect(empty.endorsementType).toBeDefined();
    expect(empty.effectiveDate).toBeDefined();
    expect(empty.description).toBeDefined();
    expect(formErrors({ ...cancellation, reasonCode: '' }).reasonCode).toBeDefined();
    expect(formErrors({ ...cancellation, requestType: '' }).requestType).toBeDefined();
    const tsi = { ...cancellation, endorsementType: 'FIN_TSI', requestType: 'TSI_CHANGE' };
    expect(formErrors({ ...tsi, sumInsuredChange: '0' }).sumInsuredChange).toBeDefined();
    expect(isValid(formErrors({ ...tsi, sumInsuredChange: '-100000' }))).toBe(true);
    const amounts = { ...cancellation, requestType: 'PREMIUM_RATE_CHANGE' };
    expect(formErrors(amounts).amounts).toBeDefined();
    const period = { ...cancellation, newPeriodFrom: '2027-01-01', newPeriodTo: '2026-01-01' };
    expect(formErrors(period).newPeriodTo).toBeDefined();
  });

  it('builds the API body with only the inputs of the mode', () => {
    const input = toInput(
      {
        ...cancellation,
        requestType: 'PREMIUM_RATE_CHANGE',
        sumInsuredChange: '5',
        amounts: { ...EMPTY_FORM.amounts, basic: '-1000', commission: 'x' },
        duplicateOverride: '  ',
      },
      ['BI-1', 'BI-2'],
    );
    expect(input.invoiceNos).toEqual(['BI-1', 'BI-2']);
    expect(input.amounts).toEqual({ basic: -1000 });
    expect(input.sumInsuredChange).toBeUndefined();
    expect(input.duplicateOverride).toBeUndefined();
    const tsi = toInput(
      {
        ...cancellation,
        endorsementType: 'FIN_TSI',
        requestType: 'TSI_CHANGE',
        sumInsuredChange: '200000',
      },
      ['BI-3'],
    );
    expect(tsi.sumInsuredChange).toBe(200000);
    expect(tsi.amounts).toBeUndefined();
  });

  it('refills the form of an existing request', () => {
    const request = {
      terms: {
        endorsementType: 'FIN_PREMIUM_RATE',
        requestType: 'PREMIUM_RATE_CHANGE',
        effectiveDate: '2026-10-01',
        refundBasis: 'SHORT_PERIOD',
        description: 'Rate change',
      },
      amounts: { basic: -500 },
      control: { duplicateOverride: 'ok' },
    } as unknown as EndorsementRequest;
    const form = formOf(request);
    expect(form.amounts.basic).toBe('-500');
    expect(form.amounts.dst).toBe('');
    expect(form.refundBasis).toBe('SHORT_PERIOD');
    expect(form.duplicateOverride).toBe('ok');
    expect(form.reasonCode).toBe('');
  });
});
