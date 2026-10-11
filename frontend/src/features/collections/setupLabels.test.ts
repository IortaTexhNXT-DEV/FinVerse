import { describe, expect, it } from 'vitest';
import {
  attributeChoices,
  attributeText,
  parameterChoices,
  parameterDescription,
  parameterLabel,
  parameterValueText,
} from './setupLabels';

/** The Collections Setup screen by labels and business descriptions, never codes or references. */

const KEYS = [
  'CLX_AGING_BASIS',
  'CLX_AGING_BRACKETS',
  'CLX_EDIT_LOCK_MINUTES',
  'CLX_EXPORT_MAX_ROWS',
  'CLX_INVOICE_NO_PATTERN',
  'CLX_MIN_BALANCE_THRESHOLD',
  'CLX_PROMISE_GRACE_DAYS',
];

describe('parameters', () => {
  it('names each parameter in words and describes it without requirement references', () => {
    for (const key of KEYS) {
      expect(parameterLabel(key)).not.toMatch(/CLX|_/);
      const text = parameterDescription(key, 'stored (NFR record lock) (caveat p.93) BrokerVerse');
      expect(text).not.toMatch(/NFR|caveat|p\.\d+|BrokerVerse|BOOKING|INCEPTION/);
    }
    expect(parameterLabel('CLX_MIN_BALANCE_THRESHOLD')).toBe('Minimal Balance Threshold');
  });

  it('shows a coded value by its label and offers it as a drop-down', () => {
    expect(parameterValueText('CLX_AGING_BASIS', 'BOOKING')).toBe('Booking date');
    expect(parameterValueText('CLX_MIN_BALANCE_THRESHOLD', '10.00')).toBe('10.00');
    expect(parameterChoices('CLX_AGING_BASIS')).toEqual([
      { code: 'BOOKING', label: 'Booking date' },
      { code: 'INCEPTION', label: 'Inception date' },
    ]);
    expect(parameterChoices('CLX_EXPORT_MAX_ROWS')).toBeUndefined();
  });
});

describe('disposition attributes', () => {
  it('shows the attributes of a disposition by label', () => {
    expect(attributeText('tagging_owner', 'OPERATIONS')).toBe('Operations');
    expect(attributeText('ops_action', 'CWT2307_REVERSAL')).toBe('BIR 2307 reversal');
    expect(attributeText('ops_action', 'NONE')).toBe('None');
    expect(attributeText('cashiering_action', 'APPLY_TO_INVOICE')).toBe('Apply to invoice');
    expect(attributeText('requires_invoice', 'true')).toBe('Yes');
    expect(attributeText('allowed_roles', 'PROCESSOR,MKT_COLLECTION,CLX_TL,MKT_SECTION_HEAD')).toBe(
      'Processing, Collection Handler, Collection Team Lead, Section Head',
    );
  });

  it('reads a blank attribute in words', () => {
    expect(attributeText('allowed_roles', '')).toBe('Every collector');
    expect(attributeText('category', '')).toBe('Not set');
  });

  it('offers the values of an attribute as labelled choices', () => {
    expect(attributeChoices('tagging_owner').map((c) => c.label)).toEqual([
      'Marketing',
      'Operations',
    ]);
    expect(attributeChoices('allowed_roles').map((c) => c.code)).toContain('CLX_TL');
    expect(attributeChoices('allowed_roles').every((c) => !c.label.includes('_'))).toBe(true);
  });
});
