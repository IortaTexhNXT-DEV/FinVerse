import { describe, expect, it } from 'vitest';
import { environmentLabel } from './environmentLabel';

describe('environmentLabel', () => {
  it('names the test environments', () => {
    expect(environmentLabel('uat')).toBe('UAT');
    expect(environmentLabel('SIT')).toBe('SIT');
    expect(environmentLabel('preprod')).toBe('Pre-production');
    expect(environmentLabel('dr-drill')).toBe('DR-DRILL');
  });

  it('shows no label in production or when unknown', () => {
    expect(environmentLabel('production')).toBeNull();
    expect(environmentLabel('prod')).toBeNull();
    expect(environmentLabel(undefined)).toBeNull();
    expect(environmentLabel('')).toBeNull();
  });
});
