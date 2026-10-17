import { describe, expect, it } from 'vitest';
import { environmentLabel } from './environmentLabel';

describe('environmentLabel', () => {
  it('names the test environments', () => {
    expect(environmentLabel('uat')).toBe('UAT');
    expect(environmentLabel('SIT')).toBe('SIT');
    expect(environmentLabel('preprod')).toBe('Pre-production');
    expect(environmentLabel('dr-drill')).toBe('DR-DRILL');
  });

  it('names the environment a test stack stands in for', () => {
    // the sign-in options carry the environment name (BROKERVERSE_ENVIRONMENT_NAME), so a stack
    // that runs with local safeguards still shows the name of the environment it represents
    expect(environmentLabel('UAT')).toBe('UAT');
    expect(environmentLabel('training')).toBe('Training');
    expect(environmentLabel('local')).toBe('Local');
  });

  it('shows no label in production or when unknown', () => {
    expect(environmentLabel('production')).toBeNull();
    expect(environmentLabel('prod')).toBeNull();
    expect(environmentLabel(undefined)).toBeNull();
    expect(environmentLabel('')).toBeNull();
  });
});
