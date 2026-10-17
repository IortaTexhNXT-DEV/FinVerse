import { describe, expect, it } from 'vitest';
import { settingsReady } from './settingsApi';
import type { RemittanceSettings } from './settingsApi';

const VALID: RemittanceSettings = {
  scheduleActive: true,
  extractionTime: '20:00',
  workingDaysOnly: true,
  folderNormalPhp: 'Remittance/Normal - Peso',
  folderNormalUsd: 'Remittance/Normal - Dollar',
  folderWithIncentives: 'Remittance/With Incentives',
  filePattern: '{date}{insurer}{settlement}_{type}',
  textPrefix: 'AM01',
};

describe('remittance settings', () => {
  it('accepts a complete change', () => {
    expect(settingsReady(VALID)).toBe(true);
  });

  it('refuses a wrong time, an empty folder or a pattern without the batch keys', () => {
    expect(settingsReady({ ...VALID, extractionTime: '24:00' })).toBe(false);
    expect(settingsReady({ ...VALID, folderNormalUsd: ' ' })).toBe(false);
    expect(settingsReady({ ...VALID, filePattern: '{date}_{type}' })).toBe(false);
    expect(settingsReady({ ...VALID, textPrefix: 'TOO-LONG-PREFIX' })).toBe(false);
  });
});
